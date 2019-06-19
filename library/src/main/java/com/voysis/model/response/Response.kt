package com.voysis.model.response

import com.google.gson.annotations.SerializedName

/**
 * class extended by any client response object
 */
open class ApiResponse {
    @SerializedName("_links")
    lateinit var links: Links
    @SerializedName("id")
    lateinit var id: String
}

data class SocketResponse<out T>(@field:SerializedName("notificationType") val notificationType: String? = null,
                                 @field:SerializedName("responseMessage") val responseMessage: String? = null,
                                 @field:SerializedName("requestId") val requestId: String? = null,
                                 @field:SerializedName("responseCode") val responseCode: Int = 0,
                                 @field:SerializedName("type") val type: String? = null,
                                 @field:SerializedName("entity") val entity: T? = null)

data class Links(@field:SerializedName("self") val self: Self? = null,
                 @field:SerializedName("queries") val queries: Queries? = null,
                 @field:SerializedName("audio") val audio: Audio? = null)

data class TextQuery(@field:SerializedName("text") val text: String? = null)

data class AudioQuery(@field:SerializedName("mimeType") val mimeType: String? = "audio/pcm;bits=16;rate=16000")

data class Reply(@field:SerializedName("text") val text: String? = null,
                 @field:SerializedName("audioUri") val audioUri: String? = null)

data class DmReply(@field:SerializedName("text") val text: String? = null,
                   @field:SerializedName("audioUri") val audioUri: String? = null)

data class Queries(@field:SerializedName("href") val href: String? = null)

data class Audio(@field:SerializedName("href") val href: String? = null)

data class Self(@field:SerializedName("href") val href: String? = null)
