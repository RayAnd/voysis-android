package com.voysis.recorder

import android.media.AudioRecord
import android.media.AudioRecord.STATE_UNINITIALIZED
import android.util.Log
import com.voysis.calculateMaxRecordingLength
import com.voysis.generateMimeType
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class AudioRecorderImpl(
        private val recordParams: AudioRecordParams,
        private val recordFactory: () -> AudioRecord = AudioRecordFactory(recordParams)::invoke,
        private val executor: Executor = Executors.newSingleThreadExecutor()) : AudioRecorder {
    private val maxBytes = calculateMaxRecordingLength(recordParams.sampleRate!!)
    private var record: AudioRecord? = null

    companion object {
        const val DEFAULT_READ_BUFFER_SIZE = 4096
        const val DEFAULT_RECORD_BUFFER_SIZE = 16384
    }

    @Synchronized
    override fun start(callback: OnDataResponse) {
        stopRecorder()
        record = recordFactory().apply {
            startRecording()
            callback.onRecordingStarted(generateMimeType())
        }
        executor.execute { write(callback) }
    }

    @Synchronized
    override fun stop() {
        stopRecorder()
    }

    private fun write(callback: OnDataResponse) {
        val buf = ByteBuffer.allocate(recordParams.readBufferSize!!)
        buf.clear()
        val buffer = ByteArray(recordParams.readBufferSize)
        var bytesRead: Int
        var limit = 0
        try {
            while (isRecording() && limit < maxBytes) {
                bytesRead = record?.read(buffer, 0, buffer.size)!!
                if ((bytesRead >= 0 || buf.position() > 0)) {
                    limit += bytesRead
                    buf.put(buffer, 0, bytesRead)
                    buf.flip()
                    callback.onDataResponse(buf)
                    buf.compact()
                } else {
                    break
                }
            }
        } catch (e: Exception) {
            Log.e("complete", e.toString())
        }
        callback.onComplete()
    }

    private fun isRecording(): Boolean = record?.recordingState == AudioRecord.RECORDSTATE_RECORDING

    private fun stopRecorder() {
        if (record?.state != STATE_UNINITIALIZED) {
            record?.stop()
            record?.release()
            record = null
        }
    }
}