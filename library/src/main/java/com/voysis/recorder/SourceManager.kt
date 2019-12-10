package com.voysis.recorder

import android.media.AudioRecord
import com.voysis.generateMimeType
import java.util.concurrent.atomic.AtomicBoolean

class SourceManager(private var audio: AudioRecordFactory, private val recordParams: AudioRecordParams) {
    private val isActive = AtomicBoolean(false)
    private lateinit var record: AudioRecord

    fun isActive(): Boolean = isActive.get()

    fun startRecording() {
        isActive.set(true)
        record = audio.make()
        record.startRecording()
    }

    fun generateMimeType(): MimeType? {
        return record.generateMimeType()
    }

    fun generateBuffer(): ByteArray = ByteArray(recordParams.readBufferSize!!)

    fun isRecording(): Boolean = isActive.get() && record.recordingState == AudioRecord.RECORDSTATE_RECORDING

    fun destroy() {
        if (record.state != AudioRecord.STATE_UNINITIALIZED) {
            isActive.set(false)
            record.stop()
            record.release()
        }
    }

    fun read(buffer: ByteArray, i: Int, size: Int): Int {
        return record.read(buffer, i, size)
    }
}