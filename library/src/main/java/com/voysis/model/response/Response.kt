package com.voysis.model.response

import com.google.gson.annotations.SerializedName

/**
 * class extended by any client response object
 */
open class ApiResponse {
    @SerializedName("_links")
    lateinit var links: Links
    lateinit var id: String
}

data class SocketResponse<out T>(val notificationType: String? = null,
                                 val responseMessage: String? = null,
                                 val requestId: String? = null,
                                 val responseCode: Int = 0,
                                 val type: String? = null,
                                 val entity: T? = null)

data class Links(val self: Self? = null, val queries: Queries? = null, val audio: Audio? = null)

data class TextQuery(val text: String? = null)

data class AudioQuery(val mimeType: String? = "audio/pcm;bits=16;rate=16000")

data class Reply(val text: String? = null, val audioUri: String? = null)

data class Queries(val href: String? = null)

data class Audio(val href: String? = null)

data class Self(val href: String? = null)
