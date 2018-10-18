package com.voysis.sdk

import com.google.gson.Gson
import com.nhaarman.mockito_kotlin.whenever
import com.voysis.recorder.AudioInfo
import com.voysis.rest.RestClient
import com.voysis.sevice.Converter
import org.junit.Assert.assertEquals
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mockito.junit.MockitoJUnitRunner
import java.net.MalformedURLException
import java.net.URL
import java.nio.channels.ReadableByteChannel

@RunWith(MockitoJUnitRunner::class)
class ServiceImplRestTest : ClientTest() {

    @Mock
    private lateinit var call: Call
    @Mock
    private lateinit var channel: ReadableByteChannel
    @Mock
    private lateinit var client: OkHttpClient
    @Mock
    private lateinit var networkResponse: Response
    @Mock
    private lateinit var body: ResponseBody

    private lateinit var restClient: RestClient
    private val voysisEventArgumentCaptor = ArgumentCaptor.forClass(Callback::class.java)

    @Before
    @Throws(MalformedURLException::class)
    fun setup() {
        val converter = Converter(headers, Gson())
        restClient = spy(RestClient(converter, URL("http://test.com"), client))
        doReturn(body).whenever(networkResponse).body()
        doReturn(call).whenever(client).newCall(any(Request::class.java))
        doNothing().whenever(call).enqueue(voysisEventArgumentCaptor.capture())
    }

    @Test
    @Throws(Exception::class)
    fun testSuccessCreateAudioQuery() {
        doReturn("result").whenever(body).string()
        val future = restClient.createAudioQuery(userId = "userId", token = "token", audioInfo = AudioInfo(16000, 16))
        doReturn(true).whenever(networkResponse).isSuccessful
        voysisEventArgumentCaptor.value.onResponse(call, networkResponse)
        assertEquals(future.get(), "result")
    }

    @Test
    @Throws(Exception::class)
    fun testSuccessStreamAudio() {
        doReturn("result").whenever(body).string()
        val future = restClient.streamAudio(channel, createAudioQueryResponse())
        doReturn(true).whenever(networkResponse).isSuccessful
        voysisEventArgumentCaptor.value.onResponse(call, networkResponse)
        assertEquals(future.get(), "result")
    }
}