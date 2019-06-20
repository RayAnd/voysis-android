package com.voysis.android.core.impl

import com.google.gson.Gson
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.doNothing
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.voysis.model.request.InteractionType
import com.voysis.model.request.RequestEntity
import com.voysis.recorder.MimeType
import com.voysis.rest.RestClient
import com.voysis.sdk.ClientTest
import com.voysis.sevice.Converter
import okhttp3.Call
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.io.IOException

@RunWith(MockitoJUnitRunner::class)
class RestClientTest : ClientTest() {

    private val textRequest = """"queryType":"text","interactionType":"QUERY","textQuery":{"text":"test text"}"""

    private val feedbackRequest = """{"duration":{"vad":5,"userStop":4,"complete":6},"description":"description","rating":"rating"}"""
    @Mock
    private lateinit var okHttpClient: OkHttpClient
    @Mock
    private lateinit var call: Call
    @Mock
    private lateinit var dispatcher: Dispatcher
    @Mock
    private lateinit var callResponse: Response
    private lateinit var restClient: RestClient
    private val interactionType = InteractionType.QUERY
    private lateinit var gson: Gson

    @Before
    fun setup() {
        gson = Gson()
        restClient = RestClient(config, Converter(headers, gson), okHttpClient)
    }

    @Test
    fun testSendTextQuery() {
        doReturn(call).whenever(okHttpClient).newCall(any())
        doReturn(callResponse).whenever(call).execute()
        restClient.sendTextQuery(null, interactionType, "test text", null, "token")
        argumentCaptor<Request>().apply {
            verify(okHttpClient).newCall(capture())
            val request = firstValue
            val body = bodyToString(request.body())
            Assert.assertTrue(body.contains(textRequest))
        }
    }

    @Test
    fun testSendAudioQuery() {
        doReturn(call).whenever(okHttpClient).newCall(any())
        doReturn(callResponse).whenever(call).execute()
        val mimeType = MimeType(16000, 16, "signed-int", false, 1)
        restClient.createAudioQuery(null, interactionType, null, "token", mimeType)
        argumentCaptor<Request>().apply {
            verify(okHttpClient).newCall(capture())
            val request = firstValue
            val body = bodyToString(request.body())
            val requestBody = gson.fromJson<RequestEntity>(body, RequestEntity::class.java)
            Assert.assertEquals(requestBody.queryType, "audio")
            Assert.assertEquals(requestBody.audioQuery!!.mimeType, mimeType.getDescription())
        }
    }

    @Test
    fun testCancelSendAudioQuery() {
        doReturn(dispatcher).whenever(okHttpClient).dispatcher()
        doNothing().whenever(dispatcher).cancelAll()
        restClient.cancelStreaming()
        verify(dispatcher).cancelAll()
    }

    @Test
    fun testRefreshSessionToken() {
        doReturn(call).whenever(okHttpClient).newCall(any())
        doReturn(callResponse).whenever(call).execute()
        restClient.refreshSessionToken("test token")
        argumentCaptor<Request>().apply {
            verify(okHttpClient).newCall(capture())
            val request = firstValue
            Assert.assertEquals(request.headers().get("Authorization"), "Bearer test token")
        }
    }

    private fun bodyToString(request: RequestBody?): String {
        try {
            val buffer = Buffer()
            if (request != null) {
                request.writeTo(buffer)
            } else {
                return ""
            }
            return buffer.readUtf8()
        } catch (e: IOException) {
            return "did not work"
        }
    }
}