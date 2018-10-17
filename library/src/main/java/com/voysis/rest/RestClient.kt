package com.voysis.rest

import com.voysis.api.Client
import com.voysis.events.VoysisException
import com.voysis.model.request.FeedbackData
import com.voysis.model.response.QueryResponse
import com.voysis.recorder.AudioInfo
import com.voysis.sevice.AudioResponseFuture
import com.voysis.sevice.Converter
import com.voysis.sevice.QueryFuture
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException
import java.net.URL
import java.nio.channels.ReadableByteChannel
import java.util.concurrent.Future

/**
 * Rest specific implementation of Service
 */
class RestClient(private val converter: Converter, private val url: URL, private val okhttp: OkHttpClient) : Client {

    private val acceptJson = "application/json; charset=utf-8"
    private val type = MediaType.parse(acceptJson)
    private val builder: Request.Builder = Request.Builder()
            .addHeader("X-Voysis-Audio-Profile-Id", converter.headers.audioProfileId)
            .addHeader("X-Voysis-Ignore-Vad", "true")
            .addHeader("Accept", acceptJson)
            .addHeader("User-Agent", converter.headers.userAgent)

    override fun sendTextQuery(context: Map<String, Any>?, text: String, userId: String?, token: String): Future<String> {
        setAuthorizationHeader(token)
        setAcceptHeader("application/vnd.voysisquery.v1+json")
        val future = QueryFuture()
        execute(future, createRequest(text = text, userId = userId, context = context))
        return future
    }

    override fun createAudioQuery(context: Map<String, Any>?, userId: String?, token: String, audioInfo: AudioInfo): Future<String> {
        setAuthorizationHeader(token)
        setAcceptHeader("application/vnd.voysisquery.v1+json")
        val future = QueryFuture()
        execute(future, createRequest(userId = userId, context = context, audioInfo = audioInfo))
        return future
    }

    override fun refreshSessionToken(refreshToken: String): Future<String> {
        setAuthorizationHeader(refreshToken)
        setAcceptHeader(acceptJson)
        val future = QueryFuture()
        val queriesUrl = URL(url, "tokens")
        val request = buildRequest(RequestBody.create(type, ""), queriesUrl.toString())
        execute(future, request)
        return future
    }

    override fun sendFeedback(queryId: String, feedback: FeedbackData, token: String): Future<String> {
        setAuthorizationHeader(token)
        setAcceptHeader("application/vnd.voysisquery.v1+json")
        val future = QueryFuture()
        val body = mutableMapOf<String, Any>()

        feedback.durations.let {
            body["duration"] = mutableMapOf(
                    "vad" to it.vad,
                    "userStop" to it.userStop,
                    "complete" to it.complete)
        }
        feedback.description.let {
            body["description"] = "description"
        }
        feedback.rating.let {
            body["rating"] = "rating"
        }
        val queriesUrl = URL(url, "/queries/$queryId/feedback")
        val request = buildRequest(RequestBody.create(type, converter.toJson(body)), queriesUrl.toString())
        execute(future, request)
        return future
    }

    override fun streamAudio(channel: ReadableByteChannel, queryResponse: QueryResponse): AudioResponseFuture {
        val future = AudioResponseFuture()
        val request = buildRequest(RestRequestBody(channel), queryResponse.href)
        execute(future, request)
        return future
    }

    private fun createRequest(text: String? = null, userId: String?, context: Map<String, Any>?, audioInfo: AudioInfo = AudioInfo(-1, -1)): Request {
        val body = createBody(text, userId, context, audioInfo)
        val queriesUrl = URL(url, "queries")
        return buildRequest(RequestBody.create(type, converter.toJson(body)), queriesUrl.toString())
    }

    private fun createBody(text: String?, userId: String?, context: Map<String, Any>?, audioInfo: AudioInfo): MutableMap<String, Any> {
        val body = if (text == null) {
            val sampleRate = audioInfo.sampleRate
            val bitsPerSample = audioInfo.bitsPerSample
            mutableMapOf(
                    "locale" to "en-US",
                    "queryType" to "audio",
                    "audioQuery" to mapOf("mimeType" to "audio/pcm;bits=$bitsPerSample;rate=$sampleRate"))
        } else {
            mutableMapOf(
                    "locale" to "en-US",
                    "queryType" to "text",
                    "textQuery" to mapOf("text" to text))
        }
        userId?.let {
            body["userId"] = userId
        }
        context?.let {
            body["context"] = context
        }
        return body
    }

    private fun buildRequest(body: RequestBody, url: String): Request {
        return builder.post(body).url(url).build()
    }

    private fun execute(future: QueryFuture, request: Request) {
        okhttp.newCall(request).enqueue(RestCallback(future))
    }

    private fun setAcceptHeader(accept: String) {
        builder.removeHeader("Accept")
        builder.addHeader("Accept", accept)
    }

    private fun setAuthorizationHeader(token: String) {
        builder.removeHeader("Authorization")
        builder.addHeader("Authorization", "Bearer $token")
    }

    private class RestCallback internal constructor(private val future: QueryFuture) : Callback {

        override fun onFailure(call: Call, e: IOException) {
            setException(VoysisException(e))
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                try {
                    future.setResponse(response.body()!!.string())
                } catch (e: IOException) {
                    setException(VoysisException(e))
                }
            } else {
                setException(VoysisException(response.message()))
            }
        }

        private fun setException(exception: VoysisException) {
            future.setException(exception)
        }
    }
}