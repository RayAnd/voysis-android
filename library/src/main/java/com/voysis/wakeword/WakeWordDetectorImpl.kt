package com.voysis.wakeword

import android.util.Log
import com.voysis.events.WakeWordState
import com.voysis.events.WakeWordState.CANCELLED
import com.voysis.events.WakeWordState.DETECTED
import com.voysis.events.WakeWordState.IDLE
import com.voysis.events.WakeWordState.INPROGRESS
import org.apache.commons.collections.buffer.CircularFifoBuffer
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

internal class WakeWordDetectorImpl(private val interpreter: Interpreter, private val executor: ExecutorService = Executors.newSingleThreadExecutor()) : WakeWordDetector {

    companion object {
        //size input into the wakeword model in bytes.
        const val byteSampleSize = 48000
        //represents the byte stride of the sliding window scale.
        const val byteWindowSize = 800
        //actual input size for wakeword model generated converting byteArray(byteSampleSize) to float array.
        const val sampleSize = 24000
    }

    private var state: AtomicReference<WakeWordState> = AtomicReference(IDLE)
    private var callback: ((WakeWordState) -> Unit)? = {}

    override fun isActive(): Boolean = state.get() == INPROGRESS
    override fun cancel() = state.set(CANCELLED)

    override fun listen(source: ReadableByteChannel, callback: (WakeWordState) -> Unit) {
        executor.execute {
            state.set(INPROGRESS)
            this.callback = callback
            callback(state.get())
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
        while (byteChannel.read(source) > -1 && isActive()) {
            source.flip()
            while (source.remaining() > byteWindowSize && isActive()) {
                addBytesToBuffer(source, ringBuffer)
                if (ringBuffer.size >= sampleSize) {
                    val input = ringBuffer.toArray().map { it as Float }.toFloatArray()
                    if (processWakeword(input)) {
                        state.set(DETECTED)
                        onComplete()
                        return
                    }
                    Log.d("Wakeword", "processing")
                }
            }
            source.compact()
        }
        state.set(IDLE)
        onComplete()
    }

    private fun onComplete() {
        if (state.get() != CANCELLED) {
            callback?.invoke(state.get())
        }
    }

    private fun processWakeword(input: FloatArray): Boolean {
        val output = IntArray(1)
        interpreter.run(input, output)
        return output[0] != 0
    }
}