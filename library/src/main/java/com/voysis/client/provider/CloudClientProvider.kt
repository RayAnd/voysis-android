package com.voysis.client.provider

import com.voysis.api.Client
import com.voysis.api.Config
import com.voysis.client.rest.RestClient
import com.voysis.client.websocket.WebSocketClient
import com.voysis.sevice.Converter
import okhttp3.OkHttpClient

class CloudClientProvider(private val config: Config, private val okClient: OkHttpClient, private val converter: Converter) : ClientProvider {

    override fun createClient(): Client {
        return if (config.isVadEnabled) {
            WebSocketClient(config, converter, okClient)
        } else {
            RestClient(config, converter, okClient)
        }
    }
}