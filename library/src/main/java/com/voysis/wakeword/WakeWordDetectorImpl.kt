package com.voysis.wakeword

import com.voysis.events.WakeWordState
import com.voysis.events.WakeWordState.ACTIVE
import com.voysis.events.WakeWordState.DETECTED
import com.voysis.events.WakeWordState.IDLE
import com.voysis.recorder.SourceManager
import org.apache.commons.collections.buffer.CircularFifoBuffer
import org.tensorflow.lite.Interpreter
import java.nio.ShortBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

class WakeWordDetectorImpl(private val interpreter: Interpreter,
                           private val type: DetectorType = DetectorType.SINGLE,
                           private val executor: ExecutorService = Executors.newSingleThreadExecutor()) : WakeWordDetector {

    companion object {
        /*
          Source buffer reads from byteChannel. byteChannels default size is {@link AudioRecordImpl.DEFAULT_READ_BUFFER_SIZE}
          but may be greater depending on latency. This buffer should be sifficently large enough to
          read all incoming bytes from byteCahnnel
         */
        const val sourceBufferSize = 48000
        //represents the byte stride of the sliding window scale.
        const val byteWindowSize = 800
        //input size for wakeword model.
        const val sampleSize = 24000
        //the amount of positive interpreter responses must be reached before wakeword detection is returned
        const val detectionThreshold = 7
    }

    private var state: AtomicReference<WakeWordState> = AtomicReference(IDLE)

    private var callback: ((WakeWordState) -> Unit)? = {}

    override fun isActive(): Boolean = state.get() != IDLE

    override fun listen(source: SourceManager, callback: (WakeWordState) -> Unit) {
        this.callback = callback
        executor.execute {
            state.set(ACTIVE)
            callback.invoke(state.get())
            processWakeWord(source, callback)
        }
    }

    override fun stop(callback: ((WakeWordState) -> Unit)?) {
        if (callback != null) {
            this.callback = callback
        }
        state.set(IDLE)
    }

    private fun processWakeWord(source: SourceManager, callback: (WakeWordState) -> Unit) {
        val buffer = ShortBuffer.allocate(byteWindowSize * 2)
        val ringBuffer = CircularFifoBuffer(sampleSize)
        val shortArray = ShortArray(byteWindowSize)
        source.startRecording()
        var count = 0
        while (source.isRecording() && isActive()) {
            source.read(shortArray, 0, byteWindowSize)
            buffer.put(shortArray)
            if (buffer.position() >= byteWindowSize) {
                fillRingBuffer(buffer, ringBuffer)
                if (ringBuffer.size >= sampleSize) {
                    val input = ringBuffer.toArray().map { it as Float }.toFloatArray()
                    count = if (processWakeword(input)) count + 1 else 0
                    if (count == detectionThreshold) {
                        state.set(DETECTED)
                        callback.invoke(state.get())
                        ringBuffer.clear()
                        if (type == DetectorType.SINGLE) {
                            return
                        }
                        break
                    }
                }
            }
        }
        state.set(IDLE)
        callback.invoke(state.get())
    }

    private fun fillRingBuffer(buffer: ShortBuffer, ringBuffer: CircularFifoBuffer) {
        val tempArray = ShortArray(byteWindowSize)
        buffer.get(tempArray, 0, byteWindowSize)
        tempArray.forEach { ringBuffer.add(it.toFloat()) }
        buffer.compact()
    }

    private fun processWakeword(input: FloatArray): Boolean {
        val output = IntArray(1)
        interpreter.run(input, output)
        return output[0] != 0
    }
}