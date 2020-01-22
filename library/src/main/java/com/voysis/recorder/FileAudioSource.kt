package com.voysis.recorder

import com.voysis.recorder.AudioRecorder.Companion.DEFAULT_READ_BUFFER_SIZE
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

class FileAudioSource(var wavFile: File? = null) : Source {

    private var inputStream: InputStream? = null
    private var isActive = AtomicBoolean(false)

    override fun startRecording() {
        inputStream = wavFile!!.inputStream()
        isActive.set(true)
    }

    override fun generateMimeType(): MimeType? = MimeType(16000, 16, "signed-int", false, 1)

    override fun generateBuffer(): ByteArray = ByteArray(DEFAULT_READ_BUFFER_SIZE)

    override fun isRecording(): Boolean {
        return isActive.get()
    }

    override fun destroy() {
        synchronized(this) {
            isActive.set(false)
            inputStream?.close()
        }
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, i: Int, size: Int): Int {
        synchronized(this) {
            val bytesRead = if (isActive.get()) inputStream?.read(buffer, 0, buffer.size)!! else -1
            if (bytesRead <= -1) {
                isActive.set(false)
            }
            return bytesRead
        }
    }

    override fun read(buffer: ShortArray, i: Int, size: Int): Int {
        throw NotImplementedError("InputStream implementation only reads to byteArray")
    }
}