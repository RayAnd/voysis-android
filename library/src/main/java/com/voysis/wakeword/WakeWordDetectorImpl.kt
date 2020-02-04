package com.voysis.wakeword

import com.voysis.events.WakeWordState
import com.voysis.events.WakeWordState.ACTIVE
import com.voysis.events.WakeWordState.DETECTED
import com.voysis.events.WakeWordState.IDLE
import com.voysis.recorder.AudioRecorder
import org.apache.commons.collections.buffer.CircularFifoBuffer
import org.tensorflow.lite.Interpreter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

class WakeWordDetectorImpl(private val recorder: AudioRecorder,
                           private val interpreter: Interpreter,
                           private val type: DetectorType = DetectorType.SINGLE,
                           private val executor: ExecutorService = Executors.newSingleThreadExecutor()) : WakeWordDetector {
    companion object {
        /*
          Source buffer reads from byteChannel. byteChannels default size is {@link AudioRecordImpl.DEFAULT_READ_BUFFER_SIZE}
          but may be greater depending on latency. This buffer should be sifficently large enough to
          read all incoming bytes from byteCahnnel
         */
        const val sourceBufferSize = 48000
        //represents the sample size of the sliding window scale.
        const val sampleWindowSize = 800
        //input size for wakeword model.
        const val sampleSize = 24000
        //interpreter threshold that output needs too be above in order to be recognised as activation
        const val probThreshold: Float = 0.5f
        //ammount of activations that need to be registered before detection registered
        const val thresholdCount: Int = 11
    }

    private var state: AtomicReference<WakeWordState> = AtomicReference(IDLE)

    private var callback: ((WakeWordState) -> Unit)? = {}

    override fun isActive(): Boolean = state.get() != IDLE

    override fun listen(callback: (WakeWordState) -> Unit) {
        this.callback = callback
        executor.execute {
            state.set(ACTIVE)
            callback.invoke(state.get())
            processWakeWord()
        }
    }

    override fun stopDetection(callback: ((WakeWordState) -> Unit)?) {
        if (callback != null) {
            this.callback = callback
        }
        state.set(IDLE)
    }

    override fun closeSource() {
        recorder.stop()
    }

    private fun processWakeWord() {
        val ringBuffer = CircularFifoBuffer(sampleSize)
        val count = CircularFifoBuffer(thresholdCount)
        val shortArray = ShortArray(sampleWindowSize)
        val source = recorder.source
        source.startRecording()
        var samplesRead = 0
        while (source.isRecording() && isActive()) {
            samplesRead += source.read(shortArray, samplesRead, sampleWindowSize - samplesRead)
            if (samplesRead >= sampleWindowSize) {
                recorder.invokeListener(shortArray)
                shortArray.forEach { ringBuffer.add(it.toFloat()) }
                if (ringBuffer.size >= sampleSize) {
                    val result = processWakeword(ringBuffer, interpreter)
                    count.add(isAboveThreshold(result, probThreshold))
                    if (detected(count, thresholdCount)) {
                        state.set(DETECTED)
                        callback?.invoke(state.get())
                        ringBuffer.clear()
                        if (type == DetectorType.SINGLE) {
                            return
                        }
                    }
                }
                samplesRead = 0
            }
        }
        state.set(IDLE)
        callback?.invoke(state.get())
    }
}