package com.voysis.client.provider

import com.voysis.api.Client

interface ClientProvider {

    fun createClient(): Client
}