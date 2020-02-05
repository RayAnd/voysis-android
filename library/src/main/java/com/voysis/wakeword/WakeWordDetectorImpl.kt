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
                           private val executor: ExecutorService = Executors.newSingleThreadExecutor(),
                           private val wakewordConfig: WakewordConfig = WakewordConfig()) : WakeWordDetector {

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
        val ringBuffer = CircularFifoBuffer(wakewordConfig.sampleSize)
        val shortArray = ShortArray(wakewordConfig.sampleWindowSize)
        val source = recorder.source
        source.startRecording()
        var count = 0
        var samplesRead = 0
        while (source.isRecording() && isActive()) {
            val requestedSampleSize = getRequestedSampleSize(ringBuffer, wakewordConfig.sampleWindowSize - samplesRead)
            samplesRead += source.read(shortArray, samplesRead, requestedSampleSize)
            if (samplesRead >= wakewordConfig.sampleWindowSize || ringBuffer.size + samplesRead == wakewordConfig.sampleSize) {
                recorder.invokeListener(shortArray)
                for (i in 0 until samplesRead) {
                    ringBuffer.add(shortArray[i].toFloat())
                }
                if (ringBuffer.size >= wakewordConfig.sampleSize) {
                    val input = ringBuffer.toArray().map { it as Float }.toFloatArray()
                    count = if (processWakeword(input)) count + 1 else 0
                    if (count == wakewordConfig.detectionThreshold) {
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

    private fun processWakeword(input: FloatArray): Boolean {
        val output = IntArray(1)
        interpreter.run(input, output)
        return output[0] != 0
    }

    private fun getRequestedSampleSize(ringBuffer: CircularFifoBuffer, requestedSampleSize: Int) = if (!ringBuffer.isFull && ringBuffer.size + requestedSampleSize > wakewordConfig.sampleSize) {
        wakewordConfig.sampleSize - ringBuffer.size
    } else {
        requestedSampleSize
    }
}