package com.voysis.metrics

import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.Date

/**
 * This class allow save the audio and result of each execution.
 */
class AudioSaver(private val filesDir: String) {

    private var rawAudioTempFile: File? = null
    private var audioOutput: FileChannel? = null

    /**
     * Initialize all for a new execution.
     */
    init {
        rawAudioTempFile = File(filesDir, Date().time.toString().plus(".raw"))
        audioOutput = FileOutputStream(rawAudioTempFile).channel
    }

    /**
     * @param buf chunk of audio to save
     */
    fun write(buf: ByteBuffer) {
        if (!isExternalStorageWritable()) {
            return
        }
        if (rawAudioTempFile != null) {
            val readBuf = buf.asReadOnlyBuffer()
            readBuf.position(0)
            audioOutput?.write(readBuf)
        }
    }

    /**
     * @param id Id of the execution. It is going to be use to save the files with that name
     * @param result decode result of the audio. It is going to be storage with same id its audio.
     */
    fun save(id: String, result: String) {
        if (isExternalStorageWritable()) {
            if (rawAudioTempFile != null) {
                closeOutput()
                writeResult(id, result)
                clean()
            }
        }
    }

    fun clean() {
        closeOutput()
        rawAudioTempFile?.delete()
        rawAudioTempFile = null
        audioOutput = null
    }

    private fun closeOutput() {
        if (audioOutput != null && audioOutput!!.isOpen) {
            audioOutput!!.close()
        }
    }

    private fun writeResult(id: String, result: String) {
        val idRawfile = File(filesDir, id.plus(".raw"))
        rawAudioTempFile?.renameTo(idRawfile)
        val resultFile = File(filesDir, id.plus(".result"))
        resultFile.writeText(result)
    }

    private fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
}
