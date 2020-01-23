package com.voysis.recorder

import com.voysis.recorder.AudioRecorder.Companion.DEFAULT_READ_BUFFER_SIZE
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference

class FileAudioSource(var wavFile: File? = null) : Source {

    private var inputStream: AtomicReference<InputStream>? = null

    override fun startRecording() {
        inputStream = AtomicReference(wavFile!!.inputStream())
    }

    override fun generateMimeType(): MimeType? = MimeType(16000, 16, "signed-int", false, 1)

    override fun generateBuffer(): ByteArray = ByteArray(DEFAULT_READ_BUFFER_SIZE)

    override fun isRecording(): Boolean {
        return inputStream?.get() != null
    }

    override fun destroy() {
        val stream = inputStream?.getAndSet(null)
        stream?.close()
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, i: Int, size: Int): Int {
        val bytesRead = inputStream?.get()?.read(buffer, 0, buffer.size)!!
        if (bytesRead <= -1) {
            destroy()
        }
        return bytesRead
    }

    override fun read(buffer: ShortArray, i: Int, size: Int): Int {
        throw NotImplementedError("InputStream implementation only reads to byteArray")
    }
}