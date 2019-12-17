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
        if (::record.isInitialized && record.state != AudioRecord.STATE_UNINITIALIZED) {
            record.stop()
            record.release()
        }
        isActive.set(false)
    }

    fun read(buffer: ByteArray, i: Int, size: Int): Int {
        return record.read(buffer, i, size)
    }

    fun read(buffer: ShortArray, i: Int, size: Int): Int {
        return record.read(buffer, i, size)
    }
}