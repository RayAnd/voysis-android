package com.voysis.recorder

import android.util.Log
import com.voysis.events.VoysisException
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.Pipe
import java.nio.channels.ReadableByteChannel
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class WavAudioRecorder(var wavFile: File? = null,
                       private val executor: Executor = Executors.newSingleThreadExecutor()) : AudioRecorder {

    private var listener: ((ByteBuffer) -> Unit)? = null

    companion object {
        const val DEFAULT_READ_BUFFER_SIZE = 4096
    }

    private var pipe: Pipe? = null

    override fun start(): ReadableByteChannel {
        pipe = Pipe.open()
        executor.execute { write() }
        return pipe!!.source()
    }

    override fun mimeType(): MimeType? = MimeType(16000, 16, "signed-int", false, 1)

    override fun getSource(): SourceManager {
        throw NotImplementedError("this operation is not permitted")
    }

    @Synchronized
    override fun stop() {
    }

    override fun registerWriteListener(listener: (ByteBuffer) -> Unit) {
        this.listener = listener
    }

    override fun removeWriteListener() {
        listener = null
    }

    private fun write() {
        if (exists()) {
            val inputStream = wavFile!!.inputStream()
            val buffer = ByteArray(DEFAULT_READ_BUFFER_SIZE)
            val sink = pipe?.sink()
            try {
                do {
                    val bytesRead = inputStream.read(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        val byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead)
                        listener?.invoke(byteBuffer)
                        sink?.write(byteBuffer)
                    }
                } while (bytesRead >= 0)
            } catch (e: Exception) {
                Log.e("complete", e.toString())
            } finally {
                inputStream.close()
                sink?.close()
            }
        } else {
            throw VoysisException("No file selected")
        }
    }

    private fun exists() = wavFile != null && wavFile!!.exists()
}