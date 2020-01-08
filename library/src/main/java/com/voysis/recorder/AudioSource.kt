package com.voysis.recorder

import android.media.AudioRecord
import com.voysis.generateMimeType
import java.util.concurrent.atomic.AtomicBoolean

class AudioSource(private var source: AudioRecordFactory, private val recordParams: AudioRecordParams) {
    private val isActive = AtomicBoolean(false)
    private var record: AudioRecord? = null

    fun isActive(): Boolean = isActive.get()

    fun startRecording() {
        destroy()
        record = source.make()
        record?.startRecording()
        isActive.set(true)
    }

    fun generateMimeType(): MimeType? {
        return record?.generateMimeType()
    }

    fun generateBuffer(): ByteArray = ByteArray(recordParams.readBufferSize!!)

    fun isRecording(): Boolean = isActive.get() && record?.recordingState == AudioRecord.RECORDSTATE_RECORDING

    fun destroy() {
        if (record?.state != AudioRecord.STATE_UNINITIALIZED) {
            record?.stop()
            record?.release()
        }
        isActive.set(false)
    }

    fun read(buffer: ByteArray, i: Int, size: Int): Int {
        return record?.read(buffer, i, size) ?: -1
    }

    fun read(buffer: ShortArray, i: Int, size: Int): Int {
        return record?.read(buffer, i, size) ?: -1
    }
}