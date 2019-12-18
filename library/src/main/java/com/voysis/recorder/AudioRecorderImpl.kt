package com.voysis.recorder

import android.util.Log
import java.nio.ByteBuffer
import java.nio.channels.Pipe
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class AudioRecorderImpl(recordParams: AudioRecordParams,
                        private val source: SourceManager = SourceManager(AudioRecordFactory(recordParams), recordParams),
                        private val executor: Executor = Executors.newSingleThreadExecutor()) : AudioRecorder {

    private lateinit var readChannel: ReadableByteChannel
    private var listener: ((ByteBuffer) -> Unit)? = null

    companion object {
        const val DEFAULT_READ_BUFFER_SIZE = 4096
        const val DEFAULT_RECORD_BUFFER_SIZE = 16384
    }

    @Synchronized
    override fun start(): ReadableByteChannel {
        if (!source.isActive()) {
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

    override fun getSource(): SourceManager = source

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
}