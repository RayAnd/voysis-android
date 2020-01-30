package com.voysis.recorder

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.Pipe
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class AudioRecorderImpl(recordParams: AudioRecordParams,
                        override var source: Source = AudioSource(AudioRecordFactory(recordParams)),
                        private val executor: Executor = Executors.newSingleThreadExecutor()) : AudioRecorder {

    private lateinit var readChannel: ReadableByteChannel
    private var listener: ((ByteBuffer) -> Unit)? = null

    @Synchronized
    override fun start(): ReadableByteChannel {
        if (!source.isRecording()) {
            source.startRecording()
        }
        val pipe = Pipe.open()
        readChannel = pipe.source()
        executor.execute { write(pipe.sink()) }
        return readChannel
    }

    @Synchronized
    override fun stop() = source.destroy()

    override fun mimeType(): MimeType? = source.generateMimeType()

    override fun registerWriteListener(listener: (ByteBuffer) -> Unit) {
        this.listener = listener
    }

    override fun removeWriteListener() {
        listener = null
    }

    private fun write(sink: WritableByteChannel) {
        try {
            val buffer = source.generateBuffer()
            while (source.isRecording()) {
                val bytesRead = source.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    val byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead)
                    listener?.invoke(byteBuffer)
                    sink.write(byteBuffer)
                }
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderImpl", e.toString(), e)
        }
        sink.close()
    }

    override fun invokeListener(array: ShortArray) {
        listener?.let {
            val byteArray = ByteArray(array.size * Short.SIZE_BYTES)
            val buf = ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN)
            buf.asShortBuffer().put(array)
            buf.position(0).limit(array.size * Short.SIZE_BYTES)
            it.invoke(buf)
        }
    }
}