package com.voysis.model.request

import com.google.gson.annotations.SerializedName
import com.voysis.model.response.ApiResponse
import com.voysis.model.response.Query
import com.voysis.model.response.TextQuery

/**
 * interface implemented by any client request object
 */
interface ApiRequest

data class SocketRequest(val restUri: String? = null,
                         val headers: Headers? = null,
                         val entity: ApiRequest? = null,
                         val requestId: String? = null,
                         val type: String? = "request",
                         val method: String? = "POST")

data class RequestEntity(val context: Map<String, Any>? = null,
                         val queryType: String? = "audio",
                         val audioQuery: Query? = null,
                         val textQuery: TextQuery? = null,
                         val userId: String? = null,
                         val locale: String = "en-US") : ApiRequest

data class Headers(@field:SerializedName("X-Voysis-Audio-Profile-Id") val audioProfileId: String,
                   @field:SerializedName("User-Agent") val userAgent: String,
                   @field:SerializedName("Authorization") var authorization: String = "",
                   @field:SerializedName("X-Voysis-Ignore-Vad") val xVoysisIgnoreVad: Boolean? = false,
                   @field:SerializedName("Accept") val accept: String? = "application/vnd.voysisquery.v1+json")

data class Token(var expiresAt: String, var token: String) : ApiResponse()

data class FeedbackData(val durations: Duration = Duration(), var rating: String? = null, var description: String? = null) : ApiRequest

data class Duration(var userStop: Long? = null, var vad: Long? = null, var complete: Long? = null)