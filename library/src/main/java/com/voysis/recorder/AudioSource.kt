package com.voysis.recorder

import android.media.AudioRecord
import com.voysis.generateMimeType
import java.util.concurrent.atomic.AtomicBoolean

class AudioSource(private var source: AudioRecordFactory) : Source {
    private val isActive = AtomicBoolean(false)
    private var record: AudioRecord? = null

    override fun isActive(): Boolean = isActive.get()

    override fun startRecording() {
        destroy()
        record = source.make()
        record?.startRecording()
        isActive.set(true)
    }

    override fun generateMimeType(): MimeType? {
        return record?.generateMimeType()
    }

    override fun generateBuffer(): ByteArray = ByteArray(source.recordParams.readBufferSize!!)

    override fun isRecording(): Boolean = isActive.get() && record?.recordingState == AudioRecord.RECORDSTATE_RECORDING

    @Synchronized
    override fun destroy() {
        if (record?.state != AudioRecord.STATE_UNINITIALIZED) {
            record?.stop()
            record?.release()
        }
        isActive.set(false)
    }

    override fun read(buffer: ByteArray, i: Int, size: Int): Int {
        return record?.read(buffer, i, size) ?: -1
    }

    override fun read(buffer: ShortArray, i: Int, size: Int): Int {
        return record?.read(buffer, i, size) ?: -1
    }
}