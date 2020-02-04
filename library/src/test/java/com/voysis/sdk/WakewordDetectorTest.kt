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
import com.voysis.wakeword.detected
import com.voysis.wakeword.isAboveThreshold
import org.apache.commons.collections.buffer.CircularFifoBuffer
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
        wakeWordDetector = WakeWordDetectorImpl(AudioRecorderImpl(params, source), interpereter, executor = executorService, config = WakewordConfig(sampleWindowSize = 2, sampleSize = 5, probThreshold = 0.5f, thresholdCount = 16))
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
        wakeWordDetector = WakeWordDetectorImpl(AudioRecorderImpl(params, source), interpereter, executor = executorService, config = WakewordConfig(sampleWindowSize = 3, sampleSize = 6, probThreshold = 0.5f, thresholdCount = 10))
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
            actual?.add(invocation.getArgument<FloatArray>(0))
            val output = (invocation.getArgument<Array<FloatArray>>(1))
            output[0][1] = 1f
            null
        }.whenever(interpereter).run(anyOrNull(), anyOrNull())
    }

    private fun mockSampleRead(totalSamplesToRead: Int) {
        var samples: Short = 0
        var samplesRead = 0
        doAnswer {
            if (samples <= totalSamplesToRead) {
                val shortArray = it.getArgument<ShortArray>(0)
                samplesRead = it.getArgument<Int>(1)
                val requestedSamples = it.getArgument<Int>(2)
                for (i in 0 until requestedSamples) {
                    shortArray[i] = samples
                    samples = (samples + 1).toShort()
                    samplesRead++
                }
            }
            samplesRead
        }.whenever(source).read(any<ShortArray>(), any(), any())
    }

    @Test
    fun testWakewordActivation() {
        var probabilities = listOf(0.1f, 0.1f, 0.1f, 0.5f, 0.5f, 0.5f, 0.1f, 0.1f, 0.1f)
        Assert.assertEquals(probabilitiesToActivationsWithContext(probabilities, 0.5f, 3), 3)
        Assert.assertEquals(probabilitiesToActivationsWithContext(probabilities, 0.5f, 4), 2)
        Assert.assertEquals(probabilitiesToActivationsWithContext(probabilities, 0.55f, 3), 0)
        Assert.assertEquals(probabilitiesToActivationsWithContext(probabilities, 0.1f, 3), 6)
        probabilities = listOf(0.0356333f, 0.031588446f, 0.031266436f, 0.02904712f, 0.032489043f, 0.026997248f, 0.029406862f, 0.02489764f, 0.029734029f, 0.024640385f, 0.03177711f, 0.028833939f, 0.036821257f, 0.0391036f, 0.052728474f, 0.03529741f, 0.041453764f, 0.028011154f, 0.034597237f, 0.024858817f, 0.02883305f, 0.02245944f, 0.031966653f, 0.03177742f, 0.04432046f, 0.04002099f, 0.040271714f, 0.0382619f, 0.035585098f, 0.030636195f, 0.023181543f, 0.028756317f, 0.044512182f, 0.061714098f, 0.1539415f, 0.23393238f, 0.34523478f, 0.5102445f, 0.66894656f, 0.75178576f, 0.7967912f, 0.8377506f, 0.8416338f, 0.917356f, 0.9224794f, 0.9535297f, 0.94170445f, 0.96369725f, 0.9570479f, 0.97133696f, 0.9577936f, 0.96862686f, 0.9565326f, 0.96527684f, 0.9494574f, 0.9604444f, 0.94931513f, 0.95819664f, 0.9480984f, 0.9541109f, 0.9446847f, 0.9563497f, 0.94704056f, 0.9477755f, 0.9379421f, 0.9392767f, 0.9221256f, 0.950618f, 0.9142939f, 0.9322532f, 0.8050973f, 0.6294255f, 0.39060998f, 0.18650427f, 0.0545912f, 0.0155734895f, 0.010198834f, 0.0065746745f, 0.006712006f, 0.007182035f, 0.010279924f, 0.008469432f, 0.008866795f, 0.011126339f, 0.012798452f, 0.010853377f, 0.011233264f, 0.009790641f, 0.009224032f, 0.00836302f, 0.007906925f, 0.008777731f, 0.008078391f, 0.007237932f, 0.007217334f, 0.0076291915f, 0.0076369676f, 0.00749962f, 0.0076394803f, 0.00809427f, 0.008183744f, 0.008512603f, 0.008308798f, 0.009079428f, 0.009024824f, 0.009467147f, 0.009468399f, 0.009395005f, 0.00971932f, 0.009348406f)
        Assert.assertEquals(probabilitiesToActivationsWithContext(probabilities, 0.55f, 11), 34)
        Assert.assertEquals(probabilitiesToActivationsWithContext(probabilities, 0.95f, 11), 12)
    }

    private fun probabilitiesToActivationsWithContext(probabilities: List<Float>, probThreshold: Float, leftThreshold: Int): Int {
        var numActivations = 0
        val predictions = mutableListOf<Int>()
        for (item in probabilities) {
            predictions.add(isAboveThreshold(item, probThreshold))
        }

        for (i in leftThreshold until predictions.size step 1) {
            val bufferedWindow = predictions.subList(i - leftThreshold, i)
            val x = CircularFifoBuffer(bufferedWindow)
            if (detected(x, leftThreshold)) {
                numActivations++
            }
        }
        return numActivations
    }
}