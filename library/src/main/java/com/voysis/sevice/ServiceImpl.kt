package com.voysis.sevice

import android.content.Context
import com.voysis.api.Client
import com.voysis.api.Service
import com.voysis.api.State
import com.voysis.api.StreamingStoppedReason
import com.voysis.events.Callback
import com.voysis.events.FinishedReason
import com.voysis.events.VoysisException
import com.voysis.model.request.FeedbackData
import com.voysis.model.request.Token
import com.voysis.model.response.QueryResponse
import com.voysis.model.response.StreamResponse
import com.voysis.recorder.AudioRecorder
import com.voysis.recorder.MimeType
import com.voysis.recorder.MimeType
import com.voysis.recorder.OnDataResponse
import com.voysis.setAudioProfileId
import com.voysis.websocket.WebSocketClient.Companion.CLOSING
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.Pipe
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

internal class ServiceImpl(private val client: Client,
                           private val recorder: AudioRecorder,
                           private val converter: Converter,
                           private val userId: String?,
                           private val tokenManager: TokenManager) : Service {
    override var state = State.IDLE
    private lateinit var mimeType: MimeType
    @Throws(IOException::class)
    override fun startAudioQuery(context: Map<String, Any>?, callback: Callback) {
        if (state == State.IDLE) {
            state = State.BUSY
            val pipe = startRecording(callback)
            executeAudio(callback, pipe, context)
        } else {
            callback.failure(VoysisException("duplicate request"))
        }
    }

    override fun sendTextQuery(context: Map<String, Any>?, text: String, callback: Callback) {
        if (state == State.IDLE) {
            state = State.BUSY
            executeText(callback, text, context)
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

    private fun startRecording(callback: Callback): Pipe {
        val pipe = Pipe.open()
        val sink = pipe.sink()
        recorder.start(object : OnDataResponse {
            override fun onDataResponse(buffer: ByteBuffer) {
                sink.write(buffer)
                callback.audioData(buffer)
            }

            override fun onRecordingStarted(mimeType: MimeType) {
                callback.recordingStarted()
                this@ServiceImpl.mimeType = mimeType
            }

            override fun onComplete() {
                sink.close()
            }
        })
        return pipe
    }

    private fun executeAudio(callback: Callback, pipe: Pipe, context: Map<String, Any>?) {
        try {
            checkToken()
            val audioQueryResponse = executeAudioQueryRequest(callback, context)
            // Don't do anything if cancel was called prior to streaming starting
            if (state != State.IDLE) {
                executeStreamRequest(audioQueryResponse, pipe, callback)
            }
        } catch (e: Exception) {
            handleException(callback, e)
        }
    }

    private fun executeText(callback: Callback, text: String, context: Map<String, Any>?) {
        try {
            checkToken()
            executeTextRequest(context, text, callback)
        } catch (e: Exception) {
            handleException(callback, e)
        }
    }

    private fun executeAudioQueryRequest(callback: Callback, context: Map<String, Any>?): QueryResponse {
        val response = client.createAudioQuery(context, userId, tokenManager.token, mimeType)
        val stringResponse = validateResponse(response.get())
        val audioQuery = converter.convertResponse(stringResponse, QueryResponse::class.java)
        callback.queryResponse(audioQuery)
        return audioQuery
    }

    private fun executeStreamRequest(query: QueryResponse, pipe: Pipe, callback: Callback) {
        val response = client.streamAudio(pipe.source(), query)
        checkStreamStoppedReason(response, callback)
        handleSuccess(response, callback)
    }

    private fun executeTextRequest(context: Map<String, Any>?, text: String, callback: Callback) {
        val response = client.sendTextQuery(context, text, userId, tokenManager.token)
        handleSuccess(response, callback)
    }

    private fun checkStreamStoppedReason(response: QueryFuture, callback: Callback) {
        if ((response as AudioResponseFuture).responseReason === StreamingStoppedReason.VAD_RECEIVED) {
            callback.recordingFinished(FinishedReason.VAD_RECEIVED)
            recorder.stop()
        } else {
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
        when {
            e is VoysisException -> callback.failure(e)
            e.cause is VoysisException -> callback.failure(e.cause as VoysisException)
            else -> callback.failure(VoysisException(e))
        }
        state = State.IDLE
    }
}