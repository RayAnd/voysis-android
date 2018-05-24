package com.voysis.rest

import com.voysis.api.Client
import com.voysis.events.VoysisException
import com.voysis.model.request.FeedbackData
import com.voysis.model.response.AudioQueryResponse
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

    override fun createAudioQuery(context: Map<String, Any>?, token: String): Future<String> {
        setAuthorizationHeader(token)
        setAcceptHeader("application/vnd.voysisquery.v1+json")
        val future = QueryFuture()
        val body = getBody()
        context?.let {
            body["context"] = context
        }
        val queriesUrl = URL(url, "queries")
        val request = createRequest(RequestBody.create(type, converter.toJson(body)), queriesUrl.toString())
        execute(future, request)
        return future
    }

    override fun refreshSessionToken(refreshToken: String): Future<String> {
        setAuthorizationHeader(refreshToken)
        setAcceptHeader(acceptJson)
        val future = QueryFuture()
        val queriesUrl = URL(url, "tokens")
        val request = createRequest(RequestBody.create(type, ""), queriesUrl.toString())
        execute(future, request)
        return future
    }

    override fun sendFeedback(path: String, feedback: FeedbackData, token: String): Future<String> {
        setAuthorizationHeader(token)
        setAcceptHeader("application/vnd.voysisquery.v1+json")
        val future = QueryFuture()
        val body = getBody()

        feedback.durations.let {
            body["duration"] = mutableMapOf(
                    "vad" to it.vad,
                    "complete" to it.complete)
        }
        feedback.description.let {
            body["description"] = "description"
        }
        feedback.rating.let {
            body["rating"] = "rating"
        }
        val queriesUrl = URL(url, path)
        val request = createRequest(RequestBody.create(type, converter.toJson(body)), queriesUrl.toString())
        execute(future, request)
        return future
    }

    private fun getBody(): MutableMap<String, Any> {
        return mutableMapOf(
                "locale" to "en-US",
                "queryType" to "audio",
                "audioQuery" to mapOf("mimeType" to "audio/pcm")
        )
    }

    override fun streamAudio(channel: ReadableByteChannel, audioQueryResponse: AudioQueryResponse): AudioResponseFuture {
        val future = AudioResponseFuture()
        val request = createRequest(RestRequestBody(channel), audioQueryResponse.href)
        execute(future, request)
        return future
    }

    private fun createRequest(body: RequestBody, url: String): Request {
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