package com.voysis.rest

import com.voysis.api.Client
import com.voysis.api.Config
import com.voysis.events.VoysisException
import com.voysis.model.request.FeedbackData
import com.voysis.model.response.QueryResponse
import com.voysis.recorder.MimeType
import com.voysis.sevice.AudioResponseFuture
import com.voysis.sevice.Converter
import com.voysis.sevice.QueryFuture
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import java.net.URL
import java.nio.channels.ReadableByteChannel
import java.util.concurrent.Future

/**
 * Rest specific implementation of Service
 */
class RestClient(private val config: Config,
                 private val converter: Converter,
                 private val okhttp: OkHttpClient) : Client {
    private val acceptJson = "application/json; charset=utf-8"
    private val type = MediaType.parse(acceptJson)
    private val url = config.url
    private val builder: Request.Builder = Request.Builder()
            .addHeader("X-Voysis-Audio-Profile-Id", converter.headers.audioProfileId)
            .addHeader("X-Voysis-Ignore-Vad", "true")
            .addHeader("Accept", acceptJson)
            .addHeader("X-Voysis-Client-Info", converter.headers.clientInfo)

    override fun sendTextQuery(context: Map<String, Any>?, text: String, userId: String?, token: String): Future<String> {
        setAuthorizationHeader(token)
        setAcceptHeader("application/vnd.voysisquery.v1+json")
        val future = QueryFuture()
        execute(future, createTextRequest(text, userId, context))
        return future
    }

    override fun createAudioQuery(context: Map<String, Any>?, userId: String?, token: String, mimeType: MimeType): Future<String> {
        setAuthorizationHeader(token)
        setAcceptHeader("application/vnd.voysisquery.v1+json")
        val future = QueryFuture()
        execute(future, createAudioRequest(userId, context, mimeType))
        return future
    }

    override fun cancelStreaming() {
        okhttp.dispatcher().cancelAll()
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
        val request = buildRequest(RestRequestBody(channel, config), queryResponse.href.replace("http", "https"))
        execute(future, request)
        return future
    }

    private fun createTextRequest(text: String? = null, userId: String?, context: Map<String, Any>?): Request {
        val body = createTextBody(text, userId, context)
        val queriesUrl = URL(url, "queries")
        return buildRequest(RequestBody.create(type, converter.toJson(body)), queriesUrl.toString())
    }

    private fun createAudioRequest(userId: String?, context: Map<String, Any>?, mimeType: MimeType): Request {
        val body = createAudioBody(userId, context, mimeType)
        val queriesUrl = URL(url, "queries")
        return buildRequest(RequestBody.create(type, converter.toJson(body)), queriesUrl.toString())
    }

    private fun createAudioBody(userId: String?, context: Map<String, Any>?, mimeType: MimeType): MutableMap<String, Any> {
        val body = mutableMapOf(
                "locale" to "en-US",
                "queryType" to "audio",
                "audioQuery" to mapOf("mimeType" to mimeType.getDescription()))

        userId?.let {
            body["userId"] = userId
        }
        context?.let {
            body["context"] = context
        }
        return body
    }

    private fun createTextBody(text: String?, userId: String?, context: Map<String, Any>?): MutableMap<String, Any> {
        val body = mutableMapOf(
                "locale" to "en-US",
                "queryType" to "text",
                "textQuery" to mapOf("text" to text))

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
        val response = okhttp.newCall(request).execute()
        if (response.isSuccessful) {
            try {
                future.setResponse(response.body()!!.string())
            } catch (e: IOException) {
                future.setException(e)
            }
        } else {
            future.setException(VoysisException(response.message() ?: "error occured"))
        }
    }

    private fun setAcceptHeader(accept: String) {
        builder.removeHeader("Accept")
        builder.addHeader("Accept", accept)
    }

    private fun setAuthorizationHeader(token: String) {
        builder.removeHeader("Authorization")
        builder.addHeader("Authorization", "Bearer $token")
    }
}