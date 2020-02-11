package com.voysis.android.core.impl

import com.google.gson.Gson
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.voysis.api.StreamingStoppedReason
import com.voysis.client.websocket.WebSocketClient
import com.voysis.model.request.FeedbackData
import com.voysis.model.request.InteractionType
import com.voysis.recorder.MimeType
import com.voysis.sdk.ClientTest
import com.voysis.sevice.Converter
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.junit.MockitoJUnitRunner
import java.net.MalformedURLException
import java.nio.channels.ReadableByteChannel
import java.util.concurrent.ExecutionException

@RunWith(MockitoJUnitRunner::class)
class WebSocketClientTest : ClientTest() {

    @Mock
    private lateinit var okHttpClient: OkHttpClient
    @Mock
    private lateinit var webSocket: WebSocket
    @Mock
    private lateinit var channel: ReadableByteChannel

    private val argumentCaptor = ArgumentCaptor.forClass(WebSocketListener::class.java)
    private lateinit var webSocketClient: WebSocketClient
    private val context = hashMapOf("test" to "test")
    private val token = "token"
    private val userId = "userId"
    private val interactionType = InteractionType.QUERY
    private val audioInfo = MimeType(16000, 16, "signed-int", false, 1)

    @Before
    @Throws(MalformedURLException::class)
    fun setup() {
        webSocketClient = WebSocketClient(config, Converter(headers, Gson()), okHttpClient)
        doReturn(webSocket).whenever(okHttpClient).newWebSocket(any(), argumentCaptor.capture())
    }

    @Test
    @Throws(Exception::class)
    fun testSuccessfulCreateAudioQuery() {
        getResponseFromStringSend()
        val future = webSocketClient.createAudioQuery(context, interactionType, userId, token, audioInfo)
        val response = future.get()
        assertTrue(this.response.contains(response))
    }

    @Test
    fun testExecuteTextQuery() {
        webSocketClient.sendTextQuery(null, interactionType, "text", "1", "123")
        val textEntity = """"entity":{"interactionType":"QUERY","userId":"1","queryType":"text","textQuery":{"text":"text","lmOutput":"text"},"locale":"en-US"},"requestId":"2","type":"request","method":"POST"}"""
        argumentCaptor<String>().apply {
            verify(webSocket).send(capture())
            assertTrue(firstValue.contains(textEntity))
        }
    }

    fun testSuccessfulGetToken() {
        getTokenResponseFromStringSend()
        val future = webSocketClient.refreshSessionToken(token)
        val token = future.get()
        assertTrue(this.tokenResponseValid == token)
    }

    @Test
    fun testSuccessfulFeedbackSent() {
        getResponseFromStringSend()
        val future = webSocketClient.sendFeedback("", FeedbackData(), token)
        val response = future.get()
        assertTrue(this.response.contains(response))
    }

    @Test
    @Throws(Exception::class)
    fun testSuccessfulStreamAudio() {
        getResponseFromStringSend()
        getResponseFromByteStringSendWithVad()
        val future = webSocketClient.streamAudio(channel, createAudioQueryResponse())
        val vadResponse = future.responseReason
        val response = future.get()
        assertEquals(vadResponse, StreamingStoppedReason.VAD_RECEIVED)
        assertTrue(notification.contains(response))
    }

    @Test
    fun testCancelStream() {
        val future = webSocketClient.streamAudio(channel, createAudioQueryResponse())
        webSocketClient.cancelStreaming()
        assertTrue(future.isCancelled)
    }

    @Test
    fun testCancelCreateAudioQuery() {
        val future = webSocketClient.createAudioQuery(context, interactionType, userId, token, audioInfo)
        webSocketClient.cancelStreaming()
        assertTrue(future.isCancelled)
    }

    @Test
    @Throws(Exception::class)
    fun testCloseWhileSessionInProgress() {
        getCloseResponseFromStringSend()
        val future = webSocketClient.createAudioQuery(context, interactionType, userId, token, audioInfo)
        val response = future.get()
        assertEquals(response, "closing")
    }

    @Test(expected = ExecutionException::class)
    @Throws(Exception::class)
    fun testFailWhileSessionInProgress() {
        getFailureResponseFromStringSend()
        val future = webSocketClient.createAudioQuery(context, interactionType, userId, token, audioInfo)
        future.get()
    }

    private fun getCloseResponseFromStringSend() {
        doAnswer {
            argumentCaptor.value.onClosing(webSocket, 1, "close")
            argumentCaptor.value.onClosed(webSocket, 1, "close")
            null
        }.whenever(webSocket).send(any(String::class.java))
    }

    private fun getFailureResponseFromStringSend() {
        doAnswer {
            argumentCaptor.value.onFailure(webSocket, Throwable("fail"), null)
            null
        }.whenever(webSocket).send(any(String::class.java))
    }

    private fun getResponseFromByteStringSendWithVad() {
        webSocketClient.createAudioQuery(context, interactionType, userId, token, audioInfo)
        doAnswer {
            argumentCaptor.value.onMessage(webSocket, vad)
            argumentCaptor.value.onMessage(webSocket, notification)
            null
        }.whenever(webSocket).send(any(ByteString::class.java))
    }

    private fun getResponseFromStringSend() {
        doAnswer {
            argumentCaptor.value.onMessage(webSocket, response)
            null
        }.whenever(webSocket).send(any(String::class.java))
    }

    private fun getTokenResponseFromStringSend() {
        doAnswer {
            argumentCaptor.value.onMessage(webSocket, tokenResponseValid)
            null
        }.whenever(webSocket).send(any(String::class.java))
    }
}