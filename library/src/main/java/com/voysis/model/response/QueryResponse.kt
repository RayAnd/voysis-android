package com.voysis.model.response

import com.google.gson.annotations.SerializedName

data class QueryResponse(@field:SerializedName("audioQuery") var audioQuery: AudioQuery? = null,
                         @field:SerializedName("queryType") var queryType: String? = null) : ApiResponse() {
    val href: String
        get() = links.audio!!.href!!
}