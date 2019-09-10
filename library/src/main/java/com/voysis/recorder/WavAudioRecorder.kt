package com.voysis.recorder

import android.util.Log
import com.voysis.events.VoysisException
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class WavAudioRecorder(var wavFile: File? = null,
                       private val executor: Executor = Executors.newSingleThreadExecutor()) : AudioRecorder {

    companion object {
        const val DEFAULT_READ_BUFFER_SIZE = 4096
        const val DEFAULT_RECORD_BUFFER_SIZE = 16384
    }

    @Synchronized
    override fun start(callback: OnDataResponse) {
        callback.onRecordingStarted(generateMimeType())
        executor.execute { write(callback) }
    }

    private fun generateMimeType(): MimeType {
        return MimeType(16000, 16, "signed-int", false, 1)
    }

    @Synchronized
    override fun stop() {
    }

    private fun write(callback: OnDataResponse) {
        if (wavFile != null && wavFile?.exists()!!) {
            val inputStream = wavFile!!.inputStream()
            val buf = ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE)
            buf.clear()
            val buffer = ByteArray(DEFAULT_READ_BUFFER_SIZE)
            var limit = 0
            try {
                var bytesRead = inputStream.read(buffer, 0, buffer.size)
                while ((bytesRead >= 0 || buf.position() > 0)) {
                    limit += bytesRead
                    buf.put(buffer, 0, bytesRead)
                    buf.flip()
                    callback.onDataResponse(buf)
                    buf.compact()
                    bytesRead = inputStream.read(buffer, 0, buffer.size)
                }
            } catch (e: Exception) {
                Log.e("complete", e.toString())
            } finally {
                inputStream.close()
            }
            callback.onComplete()
        } else {
            throw VoysisException("No file selected")
        }
    }
}