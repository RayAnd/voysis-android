package com.voysis.sdk

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.whenever
import com.voysis.events.WakeWordState
import com.voysis.generateDefaultAudioWavRecordParams
import com.voysis.recorder.AudioRecorderImpl
import com.voysis.recorder.AudioSource
import com.voysis.wakeword.WakeWordDetector
import com.voysis.wakeword.WakeWordDetectorImpl
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService

@RunWith(MockitoJUnitRunner::class)
class WakewordDetectorTest : ClientTest() {

    private lateinit var wakeWordDetector: WakeWordDetector
    @Mock
    private lateinit var executorService: ExecutorService

    @Mock
    private lateinit var source: AudioSource

    @Mock
    private lateinit var interpereter: Interpreter

    private var params = generateDefaultAudioWavRecordParams()

    @Before
    fun setup() {
        doAnswer { invocation ->
            (invocation.getArgument<Any>(0) as Runnable).run()
            null
        }.whenever(executorService).execute(ArgumentMatchers.any(Runnable::class.java))
        val recorder = AudioRecorderImpl(params, source)
        wakeWordDetector = WakeWordDetectorImpl(recorder, interpereter, executor = executorService)
    }

    @Test
    fun testListenStateStartStop() {
        Assert.assertEquals(wakeWordDetector.isActive(), false)
        val states = mutableListOf<WakeWordState>()
        wakeWordDetector.listen {
            states.add(it)
        }
        Assert.assertEquals(states[0], WakeWordState.ACTIVE)
        Assert.assertEquals(states[1], WakeWordState.IDLE)
    }

    @Test
    fun testListenStateDetected() {
        triggerWakeword()
        doReturn(true).whenever(source).isRecording()
        doReturn(1).whenever(source).read(any<ShortArray>(), any(), any())
        Assert.assertEquals(wakeWordDetector.isActive(), false)
        val states = mutableListOf<WakeWordState>()
        wakeWordDetector.listen {
            states.add(it)
        }
        Assert.assertEquals(states[0], WakeWordState.ACTIVE)
        Assert.assertEquals(states[1], WakeWordState.DETECTED)
    }

    private fun triggerWakeword() {
        doAnswer { invocation ->
            val output = (invocation.getArgument<Any>(1) as IntArray)
            output[0] = 1
            null
        }.whenever(interpereter).run(anyOrNull(), anyOrNull())
    }

    private fun fillBuffer(buffer: ByteBuffer) {
        buffer.clear()
        var i = 0
        while (i < WakeWordDetectorImpl.sourceBufferSize) {
            buffer.put(0)
            i++
        }
    }
}
