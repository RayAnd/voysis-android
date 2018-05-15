package com.voysis.sevice

import com.voysis.api.Client
import com.voysis.api.Service
import com.voysis.api.State
import com.voysis.api.StreamingStoppedReason
import com.voysis.events.Callback
import com.voysis.events.Event
import com.voysis.events.EventType
import com.voysis.events.VoysisException
import com.voysis.model.request.Token
import com.voysis.model.response.AudioQueryResponse
import com.voysis.model.response.AudioStreamResponse
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
            callback.onError(VoysisException("duplicate request"))
        }
    }

    @Throws(ExecutionException::class)
    override fun refreshSessionToken(): Token {
        val response = client.refreshSessionToken(refreshToken)
        val stringResponse = validateResponse(response.get())
        val token = converter.convertResponse(stringResponse, Token::class.java)
        sessionToken = token
        return token
    }

    override fun finish() {
        recorder.stop()
    }

    override fun cancel() {
        response?.cancel(true)
        recorder.stop()
        state = State.IDLE
    }

    private fun startRecording(callback: Callback) {
        pipe = Pipe.open()
        val sink = pipe.sink()
        recorder.start(object : OnDataResponse {
            override fun onDataResponse(buffer: ByteBuffer) {
                sink.write(buffer)
            }

            override fun onRecordingStarted() {
                callback.call(Event(null, EventType.RECORDING_STARTED))
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
        callback.onError(VoysisException(e))
        state = State.IDLE
    }

    private fun executeAudioQueryRequest(callback: Callback, context: Map<String, Any>?): AudioQueryResponse {
        response = client.createAudioQuery(context, sessionToken!!.token)
        val stringResponse = validateResponse(response!!.get())
        val audioQuery = converter.convertResponse(stringResponse, AudioQueryResponse::class.java)
        callback.call(Event(audioQuery, EventType.AUDIO_QUERY_CREATED))
        return audioQuery
    }

    private fun executeStreamRequest(audioQuery: AudioQueryResponse, callback: Callback) {
        response = client.streamAudio(pipe.source(), audioQuery)
        checkStreamStoppedReason(callback)
        val stringResponse = validateResponse(response!!.get())
        val streamResponse = converter.convertResponse(stringResponse, AudioStreamResponse::class.java)
        callback.call(Event(streamResponse, EventType.AUDIO_QUERY_COMPLETED))
        state = State.IDLE
    }

    private fun checkStreamStoppedReason(callback: Callback) {
        if ((response as AudioResponseFuture).responseReason === StreamingStoppedReason.VAD_RECEIVED) {
            callback.call(Event(null, EventType.VAD_RECEIVED))
            recorder.stop()
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