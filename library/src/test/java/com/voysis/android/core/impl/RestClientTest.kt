package com.voysis.android.core.impl

import com.google.gson.Gson
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.voysis.model.request.Duration
import com.voysis.model.request.FeedbackData
import com.voysis.rest.RestClient
import com.voysis.sdk.ClientTest
import com.voysis.sevice.Converter
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.Buffer
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.io.IOException
import java.net.URL

@RunWith(MockitoJUnitRunner::class)
class RestClientTest : ClientTest() {

    private val textRequest = """"queryType":"text","textQuery":{"text":"test text"}"""
    private val audioRequest = """"queryType":"audio","audioQuery":{"mimeType":"audio/pcm;bits\u003d16;rate\u003d16000"}}"""
    private val feedbackRequest = """{"duration":{"vad":5,"userStop":4,"complete":6},"description":"description","rating":"rating"}"""
    @Mock
    private lateinit var okHttpClient: OkHttpClient
    @Mock
    private lateinit var call: Call
    private lateinit var restClient: RestClient

    @Before
    fun setup() {
        restClient = RestClient(Converter(headers, Gson()), URL("http://test.com"), okHttpClient)
    }

    @Test
    fun testSendTextQuery() {
        doReturn(call).whenever(okHttpClient).newCall(any())
        restClient.sendTextQuery(null, "test text", null, "token")
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
        restClient.createAudioQuery(null, null, "token")
        argumentCaptor<Request>().apply {
            verify(okHttpClient).newCall(capture())
            val request = firstValue
            val body = bodyToString(request.body())
            Assert.assertTrue(body.contains(audioRequest))
        }
    }

    @Test
    fun testSendFeedback() {
        doReturn(call).whenever(okHttpClient).newCall(any())
        val feedbackData = FeedbackData(Duration(userStop = 4L, vad = 5L, complete = 6L))
        feedbackData.rating = "5"
        restClient.sendFeedback("1", feedbackData, "token")
        argumentCaptor<Request>().apply {
            verify(okHttpClient).newCall(capture())
            val request = firstValue
            val body = bodyToString(request.body())
            Assert.assertTrue(body.contains(feedbackRequest))
        }
    }

    @Test
    fun testRefreshSessionToken() {
        doReturn(call).whenever(okHttpClient).newCall(any())
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