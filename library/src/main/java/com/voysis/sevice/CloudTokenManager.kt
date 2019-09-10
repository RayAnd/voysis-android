package com.voysis.sevice

import com.voysis.api.TokenManager
import com.voysis.generateISODate
import com.voysis.generateRFCDate
import com.voysis.model.request.Token
import java.util.Calendar
import java.util.Date

internal class CloudTokenManager(override val refreshToken: String) : TokenManager {
    override var sessionToken: Token? = null

    override val token: String
        get() {
            return sessionToken!!.token
        }

    override fun tokenIsValid(): Boolean {
        return if (sessionToken == null) {
            false
        } else {
            val cal = Calendar.getInstance()
            val currentTime = cal.time
            cal.time = parseExpiresAtDate(sessionToken?.expiresAt!!)
            cal.add(Calendar.SECOND, -30)
            val expiryDate = cal.time
            expiryDate.after(currentTime)
        }
    }

    /*
     * This uses a try catch to account for the following SimpleDateFormat parse exception
     * that occurs on older api levels: IllegalArgumentException: Unknown pattern character 'X'
     */
    private fun parseExpiresAtDate(expiresAt: String): Date {
        return try {
            generateISODate(expiresAt)
        } catch (e: Exception) {
            generateRFCDate(expiresAt)
        }
    }
}