package com.voysis

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.util.Log
import com.voysis.api.Config
import com.voysis.model.request.Headers
import com.voysis.recorder.AudioRecordParams
import com.voysis.recorder.AudioRecorderImpl
import com.voysis.sdk.BuildConfig
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.*
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

fun generateAudioRecordParams(context: Context, config: Config): AudioRecordParams {
    val readBufferSize = generateReadBufferSize(config)
    val recordBufferSize = config.audioRecordParams?.recordBufferSize
            ?: AudioRecorderImpl.DEFAULT_RECORD_BUFFER_SIZE

    val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val rate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE).toInt()
    return AudioRecordParams(rate, readBufferSize, recordBufferSize)
}

fun generateReadBufferSize(config: Config): Int {
    return config.audioRecordParams?.readBufferSize
            ?: AudioRecorderImpl.DEFAULT_READ_BUFFER_SIZE
}

fun calculateMaxRecordingLength(sampleRate: Int, bitDepth: Int): Int {
    return sampleRate * bitDepth * 10
}

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