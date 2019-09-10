package com.voysis.api

import com.voysis.model.request.Token

internal interface TokenManager {

    val token: String
    var sessionToken: Token?
    val refreshToken: String

    fun tokenIsValid(): Boolean
}