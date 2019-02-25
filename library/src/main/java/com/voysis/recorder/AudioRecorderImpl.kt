package com.voysis.recorder

import android.content.Context
import android.media.AudioRecord
import android.media.AudioRecord.STATE_UNINITIALIZED
import android.util.Log
import com.voysis.api.Config
import com.voysis.calculateMaxRecordingLength
import com.voysis.createAudioRecorder
import com.voysis.generateAudioRecordParams
import com.voysis.generateMimeType
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class AudioRecorderImpl(
        context: Context,
        config: Config,
        private val player: AudioPlayer = AudioPlayer(context),
        private val recordParams: AudioRecordParams = generateAudioRecordParams(context, config),
        private var record: AudioRecord? = createAudioRecorder(recordParams),
        private val executor: Executor = Executors.newSingleThreadExecutor()) : AudioRecorder {
    private val maxBytes = calculateMaxRecordingLength(recordParams.sampleRate!!)
    private var mimeType = record!!.generateMimeType()
    private val inProgress = AtomicBoolean()

    companion object {
        const val DEFAULT_READ_BUFFER_SIZE = 4096
        const val DEFAULT_RECORD_BUFFER_SIZE = 16384
    }

    @Synchronized
    override fun start(callback: OnDataResponse) {
        stopRecorder()
        record = record ?: createAudioRecorder(recordParams)
        inProgress.set(true)
        execute(callback)
        player.playStartAudio()
    }

    private fun execute(callback: OnDataResponse) {
        if (inProgress.get()) {
            executor.execute { write(callback) }
        } else {
            callback.onComplete()
        }
    }

    @Synchronized
    override fun stop() {
        stopRecorder()
        if (inProgress.compareAndSet(true, false)) {
            player.playStopAudio()
        }
    }

    override fun getMimeType(): MimeType {
        return mimeType
    }

    private fun write(callback: OnDataResponse) {
        record?.startRecording()
        callback.onRecordingStarted()
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