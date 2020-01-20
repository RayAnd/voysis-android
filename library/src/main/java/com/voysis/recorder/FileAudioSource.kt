package com.voysis.recorder

import com.voysis.recorder.AudioRecorder.Companion.DEFAULT_READ_BUFFER_SIZE
import java.io.File
import java.io.InputStream

class FileAudioSource(var wavFile: File? = null) : Source {

    private var inputStream: InputStream? = null
    private var bytesRead = 0

    override fun startRecording() {
        inputStream = wavFile!!.inputStream()
    }

    override fun generateMimeType(): MimeType? = MimeType(16000, 16, "signed-int", false, 1)

    override fun generateBuffer(): ByteArray = ByteArray(DEFAULT_READ_BUFFER_SIZE)

    override fun isRecording(): Boolean {
        return bytesRead > -1
    }

    override fun destroy() {
        inputStream?.close()
    }

    override fun read(buffer: ByteArray, i: Int, size: Int): Int {
        bytesRead = inputStream?.read(buffer, 0, buffer.size)!!
        return bytesRead
    }

    override fun read(buffer: ShortArray, i: Int, size: Int): Int {
        return bytesRead
    }
}