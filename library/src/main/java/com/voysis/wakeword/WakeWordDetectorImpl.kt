package com.voysis.wakeword

import com.voysis.events.WakeWordState
import com.voysis.events.WakeWordState.ACTIVE
import com.voysis.events.WakeWordState.DETECTED
import com.voysis.events.WakeWordState.IDLE
import org.apache.commons.collections.buffer.CircularFifoBuffer
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.ReadableByteChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

class WakeWordDetectorImpl(private val interpreter: Interpreter,
                           private val type: DetectorType = DetectorType.SINGLE,
                           private val executor: ExecutorService = Executors.newSingleThreadExecutor()) : WakeWordDetector {

    companion object {
        //size input into the wakeword model in bytes.
        const val byteSampleSize = 48000
        //represents the byte stride of the sliding window scale.
        const val byteWindowSize = 800
        //actual input size for wakeword model generated converting byteArray(byteSampleSize) to float array.
        const val sampleSize = 24000
        //the amount of positive interpreter responses must be reached before wakeword detection is returned
        const val detectionThreshold = 7
    }

    private var state: AtomicReference<WakeWordState> = AtomicReference(IDLE)

    private var callback: ((WakeWordState) -> Unit)? = {}

    override fun isActive(): Boolean = state.get() != IDLE

    override fun listen(source: ReadableByteChannel, callback: (WakeWordState) -> Unit) {
        this.callback = callback
        executor.execute {
            state.set(ACTIVE)
            callback.invoke(state.get())
            processWakeWord(source)
        }
    }

    override fun stop(callback: ((WakeWordState) -> Unit)?) {
        if (callback != null) {
            this.callback = callback
        }
        state.set(IDLE)
    }

    private fun processWakeWord(byteChannel: ReadableByteChannel) {
        val source = ByteBuffer.allocate(byteSampleSize)
        val ringBuffer = CircularFifoBuffer(sampleSize)
        var count = 0
        while (byteChannel.read(source) > -1 && isActive()) {
            source.flip()
            while (source.remaining() > byteWindowSize && isActive()) {
                addBytesToBuffer(source, ringBuffer)
                if (ringBuffer.size >= sampleSize) {
                    val input = ringBuffer.toArray().map { it as Float }.toFloatArray()
                    count = if (processWakeword(input)) count + 1 else 0
                    if (count == detectionThreshold) {
                        state.set(DETECTED)
                        callback?.invoke(state.get())
                        ringBuffer.clear()
                        if (type == DetectorType.SINGLE) {
                            return
                        }
                        break
                    }
                }
            }
            source.compact()
        }
        state.set(IDLE)
        callback?.invoke(state.get())
    }

    private fun processWakeword(input: FloatArray): Boolean {
        val output = IntArray(1)
        interpreter.run(input, output)
        return output[0] != 0
    }

    private fun addBytesToBuffer(source: ByteBuffer, ringBuffer: CircularFifoBuffer) {
        val array = ByteArray(byteWindowSize)
        source.get(array, 0, byteWindowSize)
        val shortBuffer = ByteBuffer.wrap(array).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        while (shortBuffer.hasRemaining()) {
            ringBuffer.add(shortBuffer.get().toFloat())
        }
    }
}