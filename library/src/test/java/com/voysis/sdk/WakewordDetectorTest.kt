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
import com.voysis.wakeword.WakewordConfig
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.tensorflow.lite.Interpreter
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
    fun testWindowingWithUnevenSampleSize() {
        wakeWordDetector = WakeWordDetectorImpl(AudioRecorderImpl(params, source), interpereter, executor = executorService, wakewordConfig = WakewordConfig(sampleWindowSize = 2, sampleSize = 5, detectionThreshold = 8))
        val expected = listOf(
                floatArrayOf(0F, 1F, 2F, 3F, 4F),
                floatArrayOf(2F, 3F, 4F, 5F, 6F),
                floatArrayOf(4F, 5F, 6F, 7F, 8F),
                floatArrayOf(6F, 7F, 8F, 9F, 10F),
                floatArrayOf(8F, 9F, 10F, 11F, 12F),
                floatArrayOf(10F, 11F, 12F, 13F, 14F),
                floatArrayOf(12F, 13F, 14F, 15F, 16F),
                floatArrayOf(14F, 15F, 16F, 17F, 18F)
        )
        val actual = mutableListOf<FloatArray>()
        triggerWakeword(actual)
        doReturn(true).whenever(source).isRecording()
        mockSampleRead(18)
        Assert.assertEquals(wakeWordDetector.isActive(), false)
        val states = mutableListOf<WakeWordState>()
        wakeWordDetector.listen {
            states.add(it)
        }
        Assert.assertEquals(states[0], WakeWordState.ACTIVE)
        Assert.assertEquals(states[1], WakeWordState.DETECTED)
        expected.forEachIndexed { index, expectedArray ->
            Assert.assertTrue(actual[index] contentEquals expectedArray)
        }
    }

    @Test
    fun testWindowingWithEvenSampleSize() {
        wakeWordDetector = WakeWordDetectorImpl(AudioRecorderImpl(params, source), interpereter, executor = executorService, wakewordConfig = WakewordConfig(sampleWindowSize = 3, sampleSize = 6, detectionThreshold = 5))
        val expected = listOf(
                floatArrayOf(0F, 1F, 2F, 3F, 4F, 5F),
                floatArrayOf(3F, 4F, 5F, 6F, 7F, 8F),
                floatArrayOf(6F, 7F, 8F, 9F, 10F, 11F),
                floatArrayOf(9F, 10F, 11F, 12F, 13F, 14F),
                floatArrayOf(12F, 13F, 14F, 15F, 16F, 17F)
        )
        val actual = mutableListOf<FloatArray>()
        triggerWakeword(actual)
        doReturn(true).whenever(source).isRecording()
        mockSampleRead(17)
        Assert.assertEquals(wakeWordDetector.isActive(), false)
        val states = mutableListOf<WakeWordState>()
        wakeWordDetector.listen {
            states.add(it)
        }
        Assert.assertEquals(states[0], WakeWordState.ACTIVE)
        Assert.assertEquals(states[1], WakeWordState.DETECTED)
        expected.forEachIndexed { index, expectedArray ->
            Assert.assertTrue(actual[index] contentEquals expectedArray)
        }
    }

    private fun triggerWakeword(actual: MutableList<FloatArray>? = null) {
        doAnswer { invocation ->
            actual?.add(invocation.getArgument<Any>(0) as FloatArray)
            val output = (invocation.getArgument<Any>(1) as IntArray)
            output[0] = 1
            null
        }.whenever(interpereter).run(anyOrNull(), anyOrNull())
    }

    private fun mockSampleRead(totalSamplesToRead: Int) {
        var samples: Short = 0
        var samplesRead = 0
        doAnswer {
            if (samples <= totalSamplesToRead) {
                val shortArray = it.getArgument<Any>(0) as ShortArray
                samplesRead = it.getArgument<Any>(1) as Int
                val requestedSamples = it.getArgument<Any>(2) as Int
                for (i in 0 until requestedSamples) {
                    shortArray[i] = samples
                    samples = (samples + 1).toShort()
                    samplesRead++
                }
            }
            samplesRead
        }.whenever(source).read(any<ShortArray>(), any(), any())
    }
}
