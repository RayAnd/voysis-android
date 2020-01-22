package com.voysis.recorder

import com.voysis.recorder.AudioRecorder.Companion.DEFAULT_READ_BUFFER_SIZE
import java.io.File
import java.io.IOException
import java.io.InputStream

class FileAudioSource(var wavFile: File? = null) : Source {

    private var inputStream: InputStream? = null
    private var bytesRead = -1

    override fun startRecording() {
        inputStream = wavFile!!.inputStream()
        bytesRead = 0
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
        try {
            bytesRead = inputStream?.read(buffer, 0, buffer.size)!!
        } catch (e: IOException) {
            return -1
        }
        return bytesRead
    }

    override fun read(buffer: ShortArray, i: Int, size: Int): Int {
        //InputStream implementation only reads to byteArray
        return bytesRead
    }
}