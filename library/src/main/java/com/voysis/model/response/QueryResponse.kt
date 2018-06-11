package com.voysis.model.response

data class QueryResponse(var audioQuery: Query? = null, var queryType: String? = null) : ApiResponse() {
    val href: String
        get() = links.audio!!.href!!
}