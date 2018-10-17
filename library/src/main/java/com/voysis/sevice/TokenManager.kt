package com.voysis.sevice

import com.voysis.model.request.Token
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

internal class TokenManager(val refreshToken: String) {
    internal var sessionToken: Token? = null
    private var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")

    val token: String
        get() {
            return sessionToken!!.token
        }

    fun tokenIsValid(): Boolean {
        return if (sessionToken == null) {
            false
        } else {
            val currentTime = LocalDateTime.now()
            val expiryDate = LocalDateTime.parse(sessionToken?.expiresAt!!, formatter).minusSeconds(30)
            expiryDate.isAfter(currentTime)
        }
    }
}
