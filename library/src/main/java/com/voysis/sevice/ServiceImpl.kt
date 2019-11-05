package com.voysis.sevice

import android.content.Context
import com.voysis.api.Client
import com.voysis.api.Service
import com.voysis.api.State
import com.voysis.api.StreamingStoppedReason
import com.voysis.api.TokenManager
import com.voysis.client.websocket.WebSocketClient.Companion.CLOSING
import com.voysis.events.Callback
import com.voysis.events.FinishedReason
import com.voysis.events.VoysisException
import com.voysis.model.request.FeedbackData
import com.voysis.model.request.InteractionType
import com.voysis.model.request.Token
import com.voysis.model.response.QueryResponse
import com.voysis.model.response.StreamResponse
import com.voysis.recorder.AudioRecorder
import com.voysis.setAudioProfileId
import java.io.IOException
import java.nio.channels.ReadableByteChannel
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

internal class ServiceImpl(private val client: Client,
                           private var recorder: AudioRecorder,
                           private val converter: Converter,
                           private val userId: String?,
                           private val tokenManager: TokenManager) : Service {
    override var state = State.IDLE

    @Throws(IOException::class)
    override fun startAudioQuery(context: Map<String, Any>?, callback: Callback, interactionType: InteractionType?, source: AudioRecorder?) {
        if (state == State.IDLE) {
            state = State.BUSY
            if (source != null) {
                recorder = source
            }
            val pipe = recorder.start()
            callback.recordingStarted()
            executeAudio(callback, pipe, context, interactionType)
        } else {
            callback.failure(VoysisException("duplicate request"))
        }
    }

    override fun sendTextQuery(context: Map<String, Any>?, text: String, callback: Callback, interactionType: InteractionType?) {
        if (state == State.IDLE) {
            state = State.BUSY
            executeText(callback, text, context, interactionType)
        } else {
            callback.failure(VoysisException("duplicate request"))
        }
    }

    override fun finish() {
        recorder.stop()
    }

    override fun cancel() {
        client.cancelStreaming()
        recorder.stop()
        state = State.IDLE
    }

    override fun close() {
        client.close()
    }

    @Throws(ExecutionException::class)
    override fun refreshSessionToken(): Token {
        val response = client.refreshSessionToken(tokenManager.refreshToken)
        val stringResponse = validateResponse(response.get())
        val token = converter.convertResponse(stringResponse, Token::class.java)
        tokenManager.sessionToken = token
        return token
    }

    @Throws(ExecutionException::class)
    override fun sendFeedback(queryId: String, feedback: FeedbackData) {
        checkToken()
        client.sendFeedback(queryId, feedback, tokenManager.token)
    }

    override fun getAudioProfileId(context: Context): String? {
        return context.getSharedPreferences("VOYSIS_PREFERENCE", Context.MODE_PRIVATE).getString("ID", null)
    }

    override fun resetAudioProfileId(context: Context): String {
        return setAudioProfileId(context.getSharedPreferences("VOYSIS_PREFERENCE", Context.MODE_PRIVATE))
    }

    private fun executeAudio(callback: Callback, source: ReadableByteChannel, context: Map<String, Any>?, interactionType: InteractionType?) {
        try {
            checkToken()
            val audioQueryResponse = executeAudioQueryRequest(callback, context, interactionType)
            // Don't do anything if cancel was called prior to streaming starting
            if (state != State.IDLE) {
                executeStreamRequest(audioQueryResponse, source, callback)
            }
        } catch (e: Exception) {
            handleException(callback, e)
        }
    }

    private fun executeText(callback: Callback, text: String, context: Map<String, Any>?, interactionType: InteractionType?) {
        try {
            checkToken()
            executeTextRequest(context, text, callback, interactionType)
        } catch (e: Exception) {
            handleException(callback, e)
        }
    }

    private fun executeAudioQueryRequest(callback: Callback, context: Map<String, Any>?, interactionType: InteractionType?): QueryResponse {
        val response = client.createAudioQuery(context, interactionType, userId, tokenManager.token, recorder.mimeType()!!)
        val stringResponse = validateResponse(response.get())
        val audioQuery = converter.convertResponse(stringResponse, QueryResponse::class.java)
        callback.queryResponse(audioQuery)
        return audioQuery
    }

    private fun executeStreamRequest(query: QueryResponse, source: ReadableByteChannel, callback: Callback) {
        val response = client.streamAudio(source, query)
        checkStreamStoppedReason(response, callback)
        handleSuccess(response, callback)
    }

    private fun executeTextRequest(context: Map<String, Any>?, text: String, callback: Callback, interactionType: InteractionType?) {
        val response = client.sendTextQuery(context, interactionType, text, userId, tokenManager.token)
        handleSuccess(response, callback)
    }

    private fun checkStreamStoppedReason(response: QueryFuture, callback: Callback) {
        recorder.stop()
        val reason = (response as AudioResponseFuture).responseReason
        if (reason === StreamingStoppedReason.VAD_RECEIVED) {
            callback.recordingFinished(FinishedReason.VAD_RECEIVED)
        } else if (reason != StreamingStoppedReason.CANCELLATION) {
            callback.recordingFinished(FinishedReason.MANUAL_STOP)
        }
    }

    @Throws(ExecutionException::class)
    private fun validateResponse(stringResponse: String): String {
        if (stringResponse == CLOSING) {
            throw ExecutionException(Throwable("server disconnected"))
        }
        return stringResponse
    }

    private fun checkToken() {
        if (!tokenManager.tokenIsValid()) {
            refreshSessionToken()
        }
    }

    private fun handleSuccess(response: Future<String>, callback: Callback) {
        val stringResponse = validateResponse(response.get())
        val streamResponse = converter.convertResponse(stringResponse, StreamResponse::class.java)
        callback.success(streamResponse)
        state = State.IDLE
    }

    private fun handleException(callback: Callback, e: Exception) {
        recorder.stop()
        recordingFinishedCheck(callback, e)
        when {
            e is VoysisException -> callback.failure(e)
            e.cause is VoysisException -> callback.failure(e.cause as VoysisException)
            else -> callback.failure(VoysisException(e))
        }
        state = State.IDLE
    }

    //If cancellation exception is thrown at any stage before recording finishes we track the callback here
    private fun recordingFinishedCheck(callback: Callback, e: Exception) {
        if (e is CancellationException || e.cause is CancellationException) {
            callback.recordingFinished(FinishedReason.CANCELLED)
        }
    }
}