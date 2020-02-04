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
                           private val config: WakewordConfig = WakewordConfig()) : WakeWordDetector {

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
        val ringBuffer = CircularFifoBuffer(config.sampleSize)
        val countQueue = CircularFifoBuffer(config.thresholdCount)
        val shortArray = ShortArray(config.sampleWindowSize)
        val source = recorder.source
        source.startRecording()
        var samplesRead = 0
        while (source.isRecording() && isActive()) {
            val requestedSampleSize = getRequestedSampleSize(ringBuffer, config.sampleWindowSize - samplesRead)
            samplesRead += source.read(shortArray, samplesRead, requestedSampleSize)
            if (samplesRead >= config.sampleWindowSize || ringBuffer.size + samplesRead == config.sampleSize) {
                recorder.invokeListener(shortArray)
                for (i in 0 until requestedSampleSize) {
                    ringBuffer.add(shortArray[i].toFloat())
                }
                if (ringBuffer.size >= config.sampleSize) {
                    val result = processWakeword(ringBuffer, interpreter)
                    countQueue.add(isAboveThreshold(result, config.probThreshold))
                    if (detected(countQueue, config.thresholdCount)) {
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

    private fun getRequestedSampleSize(ringBuffer: CircularFifoBuffer, requestedSampleSize: Int): Int {
        return if (!ringBuffer.isFull && ringBuffer.size + requestedSampleSize > config.sampleSize) {
            config.sampleSize - ringBuffer.size
        } else {
            requestedSampleSize
        }
    }
}