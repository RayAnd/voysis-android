package com.voysis.model.response

data class QueryResponse(var audioQuery: AudioQuery? = null, var queryType: String? = null) : ApiResponse() {
    val href: String
        get() = links.audio!!.href!!
}