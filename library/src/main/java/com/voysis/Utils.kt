package com.voysis

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.google.gson.GsonBuilder
import com.voysis.api.BaseConfig
import com.voysis.model.request.Headers
import com.voysis.recorder.AudioRecordParams
import com.voysis.recorder.AudioRecorderImpl
import com.voysis.sdk.BuildConfig
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

fun getHeaders(context: Context): Headers {
    return Headers(getOrCreateAudioProfileId(context), getClientVersionInfo(context))
}

/**
 * Get the stringified form of the client version info to supply when
 * creating a new query.
 * @param context current context
 * @return client version info string including app package/version and sdk package/version.
 */
fun getClientVersionInfo(context: Context): String {
    var versionName = ""
    try {
        val appPackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        versionName = appPackageInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        Log.w("ServiceImpl", "getClientVersionInfo: ", e)
    }
    val clientInfo = mapOf(
            "os" to mapOf(
                "id" to "Android",
                "version" to Build.VERSION.RELEASE
            ),
            "sdk" to mapOf(
                    "id" to "voysis-android",
                    "version" to BuildConfig.VERSION_NAME
            ),
            "app" to mapOf(
                    "id" to context.packageName,
                    "version" to versionName
            ),
            "device" to mapOf(
                    "manufacturer" to Build.MANUFACTURER,
                    "model" to Build.MODEL
            )
    )
    return GsonBuilder().serializeNulls().create().toJson(clientInfo)
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
        setAudioProfileId(preferences)
    } else {
        Log.d("getAudioProfileId", id)
        id
    }
}

fun setAudioProfileId(preferences: SharedPreferences): String {
    val uuid = UUID.randomUUID().toString()
    preferences.edit().putString("ID", uuid).apply()
    return uuid
}

fun generateAudioRecordParams(context: Context, config: BaseConfig): AudioRecordParams {
    val readBufferSize = generateReadBufferSize(config)
    val recordBufferSize = config.audioRecordParams?.recordBufferSize
            ?: AudioRecorderImpl.DEFAULT_RECORD_BUFFER_SIZE

    val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val rate = config.audioRecordParams?.sampleRate
            ?: audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE).toInt()
    return AudioRecordParams(rate, readBufferSize, recordBufferSize)
}

/**
 * @param config app config
 * @return wav specifics AudioRecordParams
 */
fun generateAudioWavRecordParams(config: BaseConfig): AudioRecordParams {
    val readBufferSize = generateReadBufferSize(config)
    val recordBufferSize = config.audioRecordParams?.recordBufferSize
            ?: AudioRecorderImpl.DEFAULT_RECORD_BUFFER_SIZE
    val rate = 16000
    return AudioRecordParams(rate, readBufferSize, recordBufferSize)
}

fun generateReadBufferSize(config: BaseConfig): Int {
    return config.audioRecordParams?.readBufferSize
            ?: AudioRecorderImpl.DEFAULT_READ_BUFFER_SIZE
}

fun calculateMaxRecordingLength(sampleRate: Int): Int {
    //AudioFormat.ENCODING_PCM_16BIT = two bytes per sample
    val bytesPerSample = 2
    val seconds = 10
    return sampleRate * bytesPerSample * seconds
}

fun generateISODate(expiresAt: String): Date {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.ENGLISH)
    return format.parse(expiresAt)!!
}

fun generateRFCDate(expiresAt: String): Date {
    var local = expiresAt
    if (expiresAt.endsWith("Z")) {
        local = expiresAt.replace("Z", "+0000")
    }
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH)
    return format.parse(local)!!
}
