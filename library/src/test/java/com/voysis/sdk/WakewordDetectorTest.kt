package com.voysis.sdk

import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.whenever
import com.voysis.events.WakeWordState
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
import java.nio.channels.ReadableByteChannel
import java.util.concurrent.ExecutorService

@RunWith(MockitoJUnitRunner::class)
class WakewordDetectorTest : ClientTest() {

    private lateinit var wakeWordDetector: WakeWordDetector
    @Mock
    private lateinit var executorService: ExecutorService
    @Mock
    private lateinit var source: ReadableByteChannel
    @Mock
    private lateinit var interpereter: Interpreter

    @Before
    fun setup() {
        doAnswer { invocation ->
            (invocation.getArgument<Any>(0) as Runnable).run()
            null
        }.whenever(executorService).execute(ArgumentMatchers.any(Runnable::class.java))
        wakeWordDetector = WakeWordDetectorImpl(interpereter, executor = executorService)
    }

    @Test
    fun testListenStateStartStop() {
        doReturn(-1).whenever(source).read(anyOrNull())
        Assert.assertEquals(wakeWordDetector.isActive(), false)
        val states = mutableListOf<WakeWordState>()
        wakeWordDetector.listen(source) {
            states.add(it)
        }
        Assert.assertEquals(states[0], WakeWordState.ACTIVE)
        Assert.assertEquals(states[1], WakeWordState.IDLE)
    }

    @Test
    fun testListenStateDetected() {
        triggerWakeword()
        fillBuffer()
        Assert.assertEquals(wakeWordDetector.isActive(), false)
        val states = mutableListOf<WakeWordState>()
        wakeWordDetector.listen(source) {
            states.add(it)
        }
        Assert.assertEquals(states[0], WakeWordState.ACTIVE)
        Assert.assertEquals(states[1], WakeWordState.DETECTED)
    }

    private fun fillBuffer() {
        doAnswer { invocation ->
            val buffer = (invocation.getArgument<Any>(0) as ByteBuffer)
            fillBuffer(buffer)
            null
        }.whenever(source).read(anyOrNull())
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
