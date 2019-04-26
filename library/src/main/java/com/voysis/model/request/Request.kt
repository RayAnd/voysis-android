package com.voysis.model.request

import com.google.gson.annotations.SerializedName
import com.voysis.model.response.ApiResponse
import com.voysis.model.response.AudioQuery
import com.voysis.model.response.TextQuery

/**
 * interface implemented by any client request object
 */
interface ApiRequest

data class SocketRequest(@field:SerializedName("restUri") val restUri: String? = null,
                         @field:SerializedName("headers") val headers: Headers? = null,
                         @field:SerializedName("entity") val entity: ApiRequest? = null,
                         @field:SerializedName("requestId") val requestId: String? = null,
                         @field:SerializedName("type") val type: String? = "request",
                         @field:SerializedName("method") val method: String? = "POST")

data class RequestEntity(@field:SerializedName("context") val context: Map<String, Any>? = null,
                         @field:SerializedName("queryType") val queryType: String? = "audio",
                         @field:SerializedName("audioQuery") val audioQuery: AudioQuery? = null,
                         @field:SerializedName("textQuery") val textQuery: TextQuery? = null,
                         @field:SerializedName("userId") val userId: String? = null,
                         @field:SerializedName("locale") val locale: String = "en-US") : ApiRequest

data class Headers(@field:SerializedName("X-Voysis-Audio-Profile-Id") val audioProfileId: String,
                   @field:SerializedName("X-Voysis-Client-Info") val clientInfo: String,
                   @field:SerializedName("Authorization") var authorization: String = "",
                   @field:SerializedName("X-Voysis-Ignore-Vad") val xVoysisIgnoreVad: Boolean? = false,
                   @field:SerializedName("Accept") val accept: String? = "application/vnd.voysisquery.v1+json")

data class Token(@field:SerializedName("expiresAt") var expiresAt: String,
                 @field:SerializedName("token") var token: String) : ApiResponse()

data class FeedbackData(@field:SerializedName("durations") val durations: Duration = Duration(),
                        @field:SerializedName("rating") var rating: String? = null,
                        @field:SerializedName("description") var description: String? = null) : ApiRequest

data class Duration(@field:SerializedName("userStop") var userStop: Long? = null,
                    @field:SerializedName("vad") var vad: Long? = null,
                    @field:SerializedName("complete") var complete: Long? = null)