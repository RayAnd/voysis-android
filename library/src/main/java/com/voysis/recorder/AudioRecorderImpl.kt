package com.voysis.recorder

import android.media.AudioRecord
import android.media.AudioRecord.STATE_UNINITIALIZED
import android.util.Log
import com.voysis.generateMimeType
import java.nio.ByteBuffer
import java.nio.channels.Pipe
import java.nio.channels.ReadableByteChannel
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class AudioRecorderImpl(
        private val recordParams: AudioRecordParams,
        private val recordFactory: () -> AudioRecord = AudioRecordFactory(recordParams)::invoke,
        private val executor: Executor = Executors.newSingleThreadExecutor()) : AudioRecorder {
    private var record: AudioRecord? = null
    private var pipe: Pipe? = null

    companion object {
        const val DEFAULT_READ_BUFFER_SIZE = 4096
        const val DEFAULT_RECORD_BUFFER_SIZE = 16384
    }

    @Synchronized
    override fun start(): ReadableByteChannel {
        if (record == null) {
            pipe = Pipe.open()
            record = recordFactory()
            record!!.startRecording()
            executor.execute { write() }
        }
        return pipe!!.source()
    }

    @Synchronized
    override fun stop() = destroyRecorder()

    override fun mimeType(): MimeType? = record?.generateMimeType()

    private fun write() {
        val sink = pipe?.sink()
        try {
            val buffer = ByteArray(recordParams.readBufferSize!!)
            while (isRecording()) {
                record?.read(buffer, 0, buffer.size)
                sink?.write(ByteBuffer.wrap(buffer))
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderImpl", e.toString(), e)
        }
        sink?.close()
    }

    private fun isRecording(): Boolean = record?.recordingState == AudioRecord.RECORDSTATE_RECORDING

    private fun destroyRecorder() {
        if (record?.state != STATE_UNINITIALIZED) {
            record?.stop()
            record?.release()
            record = null
        }
    }
}