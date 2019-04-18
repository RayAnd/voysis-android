package com.voysis.websocket

import com.voysis.api.Client
import com.voysis.api.Config
import com.voysis.api.StreamingStoppedReason
import com.voysis.api.StreamingStoppedReason.CANCELLATION
import com.voysis.api.StreamingStoppedReason.END_OF_STREAM
import com.voysis.api.StreamingStoppedReason.NONE
import com.voysis.api.StreamingStoppedReason.VAD_RECEIVED
import com.voysis.events.PermissionDeniedException
import com.voysis.events.VoysisException
import com.voysis.generateReadBufferSize
import com.voysis.model.request.ApiRequest
import com.voysis.model.request.FeedbackData
import com.voysis.model.request.RequestEntity
import com.voysis.model.response.AudioQuery
import com.voysis.model.response.QueryResponse
import com.voysis.model.response.SocketResponse
import com.voysis.model.response.TextQuery
import com.voysis.recorder.MimeType
import com.voysis.sevice.AudioResponseFuture
import com.voysis.sevice.Converter
import com.voysis.sevice.QueryFuture
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.IOException
import java.net.URL
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicLong

internal class WebSocketClient(private val config: Config,
                               private val converter: Converter,
                               private val client: OkHttpClient) : Client {
    private val request: Request = Request.Builder().url(URL(config.url, "/websocketapi")).build()
    private val webSocketListener = InternalWebSocketListener()
    private var webSocket: WebSocket? = null
    private val streamId: Long = 1
    private val id = AtomicLong(2)

    companion object {
        const val CLOSING = "closing"
    }

    override fun createAudioQuery(context: Map<String, Any>?, userId: String?, token: String, mimeType: MimeType): Future<String> {
        return sendString("/queries", RequestEntity(context = context, userId = userId, audioQuery = AudioQuery(mimeType = mimeType.getDescription())), token)
    }

    override fun sendTextQuery(context: Map<String, Any>?, text: String, userId: String?, token: String): Future<String> {
        return sendString("/queries", RequestEntity(queryType = "text", context = context, userId = userId, textQuery = TextQuery(text = text)), token)
    }

    override fun refreshSessionToken(refreshToken: String): Future<String> {
        return sendString("/tokens", null, refreshToken)
    }

    override fun sendFeedback(queryId: String, feedback: FeedbackData, token: String): Future<String> {
        return sendString("/queries/$queryId/feedback", feedback, token)
    }

    @Throws(IOException::class)
    override fun streamAudio(channel: ReadableByteChannel, queryResponse: QueryResponse): AudioResponseFuture {
        val future = AudioResponseFuture()
        webSocketListener.addFuture(streamId, future)
        sendLoop(channel, future)
        return future
    }

    override fun cancelStreaming() {
        webSocketListener.cancelStream()
    }

    @Throws(IOException::class)
    private fun sendLoop(channel: ReadableByteChannel, future: AudioResponseFuture) {
        val buf = ByteBuffer.allocate(generateReadBufferSize(config))
        while ((channel.read(buf) > 0 || buf.position() > 0) && future.responseReason == NONE) {
            buf.flip()
            send(ByteString.of(buf))
            buf.compact()
        }
        checkStreamStopReason(future)
    }

    private fun sendString(path: String, entity: ApiRequest?, token: String): Future<String> {
        val key = id.getAndIncrement()
        val future = webSocketListener.createQueryFuture(key)
        send(converter.encodeRequest(path, entity, token, key))
        return future
    }

    /*
     *If the `responseReason != VAD_RECEIVED` or 'CANCELLATION' we can take it that the user stopped recording
     *manually in which case we need to notify the server by sending `ByteString.of((4))`
     */
    private fun checkStreamStopReason(future: AudioResponseFuture) {
        if (future.responseReason != VAD_RECEIVED && future.responseReason != CANCELLATION) {
            send(ByteString.of((4)))
            webSocketListener.setStreamStoppedReason(END_OF_STREAM)
        }
    }

    private fun init() {
        if (webSocket == null) {
            webSocket = client.newWebSocket(request, webSocketListener)
        }
    }

    private fun send(request: String) {
        init()
        webSocket?.send(request)
    }

    private fun send(read: ByteString) {
        webSocket?.send(read)
    }

    private fun close() {
        if (webSocket != null) {
            webSocket!!.close(1000, "disconnect")
            webSocket = null
        }
    }

    private inner class InternalWebSocketListener : WebSocketListener() {
        private val callbackMap = ConcurrentHashMap<Long, QueryFuture>()

        override fun onMessage(webSocket: WebSocket, stringResponse: String) {
            val response = converter.decodeResponse(stringResponse)
            val responseCode = response.responseCode
            if (responseCode == 401 || responseCode == 403) {
                onFailure(PermissionDeniedException("error code : " + response.responseCode))
            } else if (responseCode >= 400) {
                onFailure(VoysisException(stringResponse))
            } else {
                onSuccess(response)
            }
        }

        override fun onOpen(webSocket: WebSocket, response: Response?) {
            //ignore
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String?) = close()

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String?) = callFuturesOnClose(null)

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = onFailure(VoysisException(t))

        fun setStreamStoppedReason(reason: StreamingStoppedReason) {
            val future = callbackMap[streamId] as? AudioResponseFuture
            future?.responseReason = reason
        }

        fun cancelStream() {
            setStreamStoppedReason(CANCELLATION)
            for (id in HashSet(callbackMap.keys)) {
                val future = callbackMap[id]
                future?.cancel(true)
            }
        }

        fun createQueryFuture(id: Long): QueryFuture = addFuture(id, QueryFuture())

        fun <T : QueryFuture> addFuture(id: Long, future: T): T {
            callbackMap[id] = future
            return future
        }

        private fun onSuccess(response: SocketResponse<*>) {
            val notificationType = response.notificationType
            val requestId = response.requestId
            when {
                notificationType == "vad_stop" -> setStreamStoppedReason(VAD_RECEIVED)
                notificationType == "query_complete" -> callFuture(streamId, response)
                notificationType == "internal_server_error" -> callFuturesOnClose(VoysisException("A Server Error Occurred"))
                requestId != null -> callFuture(java.lang.Long.valueOf(requestId), response)
                else -> callFuturesOnClose(VoysisException("Unknown Response"))
            }
        }

        private fun onFailure(exception: VoysisException) {
            callFuturesOnClose(exception)
            close()
        }

        private fun callFuture(id: Long, response: SocketResponse<*>) {
            val callback = callbackMap.remove(id)
            if (callback != null && !callback.isDone) {
                callback.setResponse(converter.toJson(response.entity!!))
            }
        }

        private fun callFuturesOnClose(exception: VoysisException?) {
            for (id in HashSet(callbackMap.keys)) {
                if (exception != null) {
                    callbackMap.remove(id)?.setException(exception)
                } else {
                    callbackMap.remove(id)?.setResponse(CLOSING)
                }
            }
        }
    }
}