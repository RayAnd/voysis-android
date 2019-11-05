package com.voysis.sdk

import android.content.Context
import com.nhaarman.mockito_kotlin.verify
import com.voysis.events.Callback
import com.voysis.model.request.FeedbackData
import com.voysis.recorder.AudioRecorder
import com.voysis.sevice.ServiceImpl
import com.voysis.wakeword.WakeWordDetector
import com.voysis.wakeword.WakeWordDetectorImpl
import com.voysis.wakeword.WakeWordServiceImpl
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.tensorflow.lite.Interpreter
import java.util.concurrent.ExecutorService

@RunWith(MockitoJUnitRunner::class)
class WakewordServiceTest : ClientTest() {

    @Mock
    private lateinit var executorService: ExecutorService
    @Mock
    private lateinit var context: Context
    @Mock
    private lateinit var service: ServiceImpl
    @Mock
    private lateinit var callback: Callback
    @Mock
    private lateinit var interpereter: Interpreter
    @Mock
    private lateinit var manager: AudioRecorder

    private lateinit var detector: WakeWordDetector

    private lateinit var wakeWordServcie: WakeWordServiceImpl

    @Before
    fun setup() {
        detector = WakeWordDetectorImpl(interpereter, executorService)
        wakeWordServcie = WakeWordServiceImpl(manager, detector, service)
    }

    @Test
    fun testStartAudioQuery() {
        wakeWordServcie.startAudioQuery(callback = callback)
        verify(service).startAudioQuery(callback = callback, source = manager)
    }

    @Test
    fun testSendTextQuery() {
        wakeWordServcie.sendTextQuery(callback = callback, text = "hello")
        verify(service).sendTextQuery(callback = callback, text = "hello")
    }

    @Test
    fun testCancel() {
        wakeWordServcie.cancel()
        verify(service).cancel()
    }

    @Test
    fun testRefreshSessionToken() {
        wakeWordServcie.refreshSessionToken()
        verify(service).refreshSessionToken()
    }

    @Test
    fun testGetAudioProfileId() {
        wakeWordServcie.getAudioProfileId(context)
        verify(service).getAudioProfileId(context)
    }

    @Test
    fun resetAudioProfileId() {
        wakeWordServcie.resetAudioProfileId(context)
        verify(service).resetAudioProfileId(context)
    }

    @Test
    fun sendFeedback() {
        val data = FeedbackData()
        wakeWordServcie.sendFeedback("", data)
        verify(service).sendFeedback("", data)
    }
}