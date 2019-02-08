package com.voysis.sdk

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.doNothing
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.voysis.api.Client
import com.voysis.api.State
import com.voysis.api.StreamingStoppedReason.VAD_RECEIVED
import com.voysis.events.Callback
import com.voysis.events.FinishedReason
import com.voysis.events.PermissionDeniedException
import com.voysis.events.VoysisException
import com.voysis.model.request.FeedbackData
import com.voysis.recorder.AudioRecorder
import com.voysis.recorder.OnDataResponse
import com.voysis.sevice.AudioResponseFuture
import com.voysis.sevice.Converter
import com.voysis.sevice.ServiceImpl
import com.voysis.sevice.TokenManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

@RunWith(MockitoJUnitRunner::class)
class ServiceImplTest : ClientTest() {

    @Mock
    private lateinit var callback: Callback
    @Mock
    private lateinit var client: Client
    @Mock
    private lateinit var manager: AudioRecorder
    @Mock
    private lateinit var audioQueryFuture: Future<String>
    @Mock
    private lateinit var tokenFuture: Future<String>
    @Mock
    private lateinit var queryFuture: AudioResponseFuture

    private var editor: SharedPreferences.Editor = mock { }

    private var preferences: SharedPreferences = mock {
        on { getString("ID", null) } doReturn "audioProfileId"
        on { edit() } doReturn editor
    }

    private var context: Context = mock {
        on { getSharedPreferences("VOYSIS_PREFERENCE", Context.MODE_PRIVATE) } doReturn preferences
    }

    private lateinit var serviceImpl: ServiceImpl
    private val tokenManager = TokenManager("refreshToken")
    private val userId = "userId"

    @Before
    fun setup() {
        val converter = Converter(headers, Gson())
        serviceImpl = ServiceImpl(client, manager, converter, userId, tokenManager)
    }

    @Test
    @Throws(Exception::class)
    fun testSuccessResponse() {
        successfulExecutionResponses()
        answerRecordingStarted()
        serviceImpl.startAudioQuery(callback = callback)
        verify(callback).recordingStarted()
        verify(callback).recordingFinished(eq(FinishedReason.VAD_RECEIVED))
        verify(callback).queryResponse(anyOrNull())
        verify(callback).success(anyOrNull())
        verify(manager).start(anyOrNull())
        verify(manager).stop()
    }

    private fun successfulExecutionResponses() {
        doReturn(tokenResponseExpired).whenever(tokenFuture).get()
        doReturn(queryFutureResponse).whenever(audioQueryFuture).get()
        doReturn(notification).whenever(queryFuture).get()
        doReturn(VAD_RECEIVED).whenever(queryFuture).responseReason
        doReturn(tokenFuture).whenever(client).refreshSessionToken(anyOrNull())
        doReturn(audioQueryFuture).whenever(client).createAudioQuery(any(), anyOrNull(), anyOrNull(), anyOrNull())
        doReturn(queryFuture).whenever(client).streamAudio(anyOrNull(), anyOrNull())
    }

    @Test
    fun testCancelRequest() {
        serviceImpl.cancel()
        verify(manager).stop()
        assertTrue(serviceImpl.state == State.IDLE)
    }

    @Test
    fun testExceptionWithVoysisExceptionCause() {
        doThrow(ExecutionException(PermissionDeniedException(""))).whenever(tokenFuture).get()
        doReturn(tokenFuture).whenever(client).refreshSessionToken(anyOrNull())
        serviceImpl.startAudioQuery(callback = callback)
        val captor = argumentCaptor<VoysisException>()
        verify(callback).failure(captor.capture())
        assertTrue(captor.firstValue is PermissionDeniedException)
        verify(manager).stop()
    }

    @Test
    fun testExpiredTokenRefreshed() {
        doReturn(tokenResponseExpired).whenever(tokenFuture).get()
        doReturn(tokenFuture).whenever(client).refreshSessionToken(anyOrNull())
        serviceImpl.startAudioQuery(callback = callback)
        serviceImpl.startAudioQuery(callback = callback)
        verify(client, times(2)).refreshSessionToken(anyOrNull())
    }

    @Test
    fun testValidToken() {
        doReturn(tokenResponseValid).whenever(tokenFuture).get()
        doReturn(tokenFuture).whenever(client).refreshSessionToken(anyOrNull())
        serviceImpl.startAudioQuery(callback = callback)
        serviceImpl.startAudioQuery(callback = callback)
        verify(client, times(1)).refreshSessionToken(anyOrNull())
    }

    @Test
    fun testValidFeedback() {
        successfulExecutionResponses()
        answerRecordingStarted()
        val feedback = FeedbackData()
        serviceImpl.startAudioQuery(callback = callback)
        serviceImpl.sendFeedback("1", feedback)
        verify(client, times(1)).sendFeedback(eq("1"), eq(feedback), eq("token"))
    }

    @Test
    fun testSuccessfulTextRequest() {
        val exampleRequest = "I'm looking for things."
        doReturn(tokenResponseValid).whenever(tokenFuture).get()
        doReturn(tokenFuture).whenever(client).refreshSessionToken(anyOrNull())
        doReturn(queryFuture).whenever(client).sendTextQuery(anyOrNull(), eq(exampleRequest), anyOrNull(), anyOrNull())
        doReturn(streamResponse).whenever(queryFuture).get()
        serviceImpl.sendTextQuery(context = null, text = exampleRequest, callback = callback)
        verify(client).sendTextQuery(anyOrNull(), eq(exampleRequest), anyOrNull(), anyOrNull())
        verify(callback).success(argThat { id == "5" })
    }

    @Test
    fun testGetAudioProfileId() {
        assertEquals("audioProfileId", serviceImpl.getAudioProfileId(context))
    }

    @Test
    fun testResetAudioProfileId() {
        doReturn(editor).whenever(editor).putString(any(), any())
        doNothing().whenever(editor).apply()
        serviceImpl.resetAudioProfileId(context)
        verify(editor).putString(eq("ID"), anyOrNull())
    }

    private fun answerRecordingStarted() {
        doAnswer { invocation ->
            (invocation.getArgument<Any>(0) as OnDataResponse).onRecordingStarted()
            null
        }.whenever(manager).start(anyOrNull())
    }
}
