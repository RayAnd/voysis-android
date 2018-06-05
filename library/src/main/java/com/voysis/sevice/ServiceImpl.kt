package com.voysis.sevice

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
import com.voysis.recorder.OnDataResponse
import com.voysis.websocket.WebSocketClient.Companion.CLOSING
import java.io.IOException
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.channels.Pipe
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

internal class ServiceImpl(private val client: Client,
                           private val recorder: AudioRecorder,
                           private val converter: Converter,
                           private val userId: String?,
                           private val refreshToken: String) : Service {
    private var response: Future<String>? = null
    private var sessionToken: Token? = null
    private lateinit var pipe: Pipe
    override var state = State.IDLE

    @Throws(IOException::class)
    override fun startAudioQuery(context: Map<String, Any>?, callback: Callback) {
        if (state == State.IDLE) {
            state = State.BUSY
            startRecording(callback)
            execute(callback, context)
        } else {
            callback.failure(VoysisException("duplicate request"))
        }
    }

    override fun finish() {
        recorder.stop()
    }

    override fun cancel() {
        response?.cancel(true)
        recorder.stop()
        state = State.IDLE
    }

    @Throws(ExecutionException::class)
    override fun refreshSessionToken(): Token {
        val response = client.refreshSessionToken(refreshToken)
        val stringResponse = validateResponse(response.get())
        val token = converter.convertResponse(stringResponse, Token::class.java)
        sessionToken = token
        return token
    }

    @Throws(ExecutionException::class)
    override fun sendFeedback(queryId: String, feedback: FeedbackData) {
        if (!tokenIsValid()) {
            refreshSessionToken()
        }
        client.sendFeedback(queryId, feedback, sessionToken!!.token)
    }

    private fun startRecording(callback: Callback) {
        pipe = Pipe.open()
        val sink = pipe.sink()
        recorder.start(object : OnDataResponse {
            override fun onDataResponse(buffer: ByteBuffer) {
                sink.write(buffer)
                callback.audioData(buffer)
            }

            override fun onRecordingStarted() {
                callback.recordingStarted()
            }

            override fun onComplete() {
                sink.close()
            }
        })
    }

    private fun execute(callback: Callback, context: Map<String, Any>?) {
        try {
            if (!tokenIsValid()) {
                refreshSessionToken()
            }
            val audioQueryResponse = executeAudioQueryRequest(callback, context)
            executeStreamRequest(audioQueryResponse, callback)
        } catch (e: Exception) {
            handleException(callback, e)
        }
    }

    private fun handleException(callback: Callback, e: Exception) {
        recorder.stop()
        callback.failure(VoysisException(e))
        state = State.IDLE
    }

    private fun executeAudioQueryRequest(callback: Callback, context: Map<String, Any>?): QueryResponse {
        response = client.createAudioQuery(context, userId, sessionToken!!.token)
        val stringResponse = validateResponse(response!!.get())
        val audioQuery = converter.convertResponse(stringResponse, QueryResponse::class.java)
        callback.queryResponse(audioQuery)
        return audioQuery
    }

    private fun executeStreamRequest(query: QueryResponse, callback: Callback) {
        response = client.streamAudio(pipe.source(), query)
        checkStreamStoppedReason(callback)
        val stringResponse = validateResponse(response!!.get())
        val streamResponse = converter.convertResponse(stringResponse, StreamResponse::class.java)
        callback.success(streamResponse)
        state = State.IDLE
    }

    private fun checkStreamStoppedReason(callback: Callback) {
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

    private fun tokenIsValid(): Boolean {
        return if (sessionToken == null) {
            false
        } else {
            val cal = Calendar.getInstance()
            val currentTime = cal.time
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
            cal.time = format.parse(sessionToken?.expiresAt)
            cal.add(Calendar.SECOND, -30)
            val expiryDate = cal.time
            expiryDate.after(currentTime)
        }
    }
}