package com.voysis

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.voysis.model.request.Headers
import com.voysis.sdk.BuildConfig
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

fun getHeaders(context: Context): Headers {
    return Headers(getOrCreateAudioProfileId(context), getUserAgent(context))
}

/**
 * @param context current context
 * @return user agent string including app package/version and sdk package/version.
 */
fun getUserAgent(context: Context): String {
    var versionName = ""
    try {
        val appPackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        versionName = appPackageInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        Log.w("ServiceImpl", "getUserAgent: ", e)
    }

    val stringBuilder = StringBuilder()
    return stringBuilder
            .append(System.getProperty("http.agent"))
            .append(" ")
            .append(context.packageName)
            .append("/")
            .append(versionName)
            .append(" ")
            .append(BuildConfig.APPLICATION_ID)
            .append("/")
            .append(BuildConfig.VERSION_NAME)
            .toString()
}

/**
 * @param client optional client
 * @return new okhttp client
 */
fun generateOkHttpClient(client: OkHttpClient?): OkHttpClient {
    val builder = getBuilder(client)
    return builder.connectTimeout(20, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
}

private fun getBuilder(client: OkHttpClient?): OkHttpClient.Builder {
    return if (client == null) {
        OkHttpClient.Builder()
    } else {
        client.newBuilder()
    }
}

/**
 * @param context current context
 * @return uuid used in audioProfileId header.
 */
fun getOrCreateAudioProfileId(context: Context): String {
    val preferences = context.getSharedPreferences("VOYSIS_PREFERENCE", Context.MODE_PRIVATE)
    val id = preferences.getString("ID", null)
    return if (id == null) {
        val uuid = UUID.randomUUID().toString()
        preferences.edit().putString("ID", uuid).apply()
        uuid
    } else {
        Log.d("getAudioProfileId", id)
        id
    }
}

/**
 * @return default AudioRecord class
 */

fun generateISODate(expiresAt: String): Date {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.ENGLISH)
    return format.parse(expiresAt)
}

fun generateRFCDate(expiresAt: String): Date {
    var local = expiresAt
    if (expiresAt.endsWith("Z")) {
        local = expiresAt.replace("Z", "+0000")
    }
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH)
    return format.parse(local)
}