package com.voysis.api

import android.content.Context
import com.google.gson.Gson
import com.jakewharton.threetenabp.AndroidThreeTen
import com.voysis.generateOkHttpClient
import com.voysis.getHeaders
import com.voysis.recorder.AudioRecorder
import com.voysis.recorder.AudioRecorderImpl
import com.voysis.rest.RestClient
import com.voysis.sevice.Converter
import com.voysis.sevice.ServiceImpl
import com.voysis.sevice.TokenManager
import com.voysis.websocket.WebSocketClient
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL

/**
 * The Voysis.ServiceProvider is the sdk's primary object for making Voysis.Service instances.
 */
class ServiceProvider {

    /**
     * @param config config
     * @param okClient optional okhttp client
     * @param audioRecorder optional to override default recorder
     * @return new `Service` instance
     */
    @JvmOverloads
    fun make(context: Context,
             config: Config,
             okClient: OkHttpClient? = null,
             audioRecorder: AudioRecorder = AudioRecorderImpl(context)): Service {
        val converter = Converter(getHeaders(context), Gson())
        AndroidThreeTen.init(context);
        val client = createClient(config, generateOkHttpClient(okClient), converter)
        return ServiceImpl(client, audioRecorder, converter, config.userId, TokenManager(config.refreshToken))
    }

    /**
     * @param config config
     * @param audioRecorder optional to override default recorder
     * @return new `Service` instance
     */
    fun make(context: Context,
             config: Config,
             audioRecorder: AudioRecorder = AudioRecorderImpl(context)): Service {
        return make(context, config, null, audioRecorder)
    }

    private fun createClient(config: Config, okClient: OkHttpClient, converter: Converter): Client {
        val url = config.url
        return if (config.isVadEnabled) {
            val request = Request.Builder().url(URL(url, "/websocketapi")).build()
            WebSocketClient(converter, request, okClient)
        } else {
            RestClient(converter, url, okClient)
        }
    }
}
