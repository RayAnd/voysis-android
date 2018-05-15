package com.voysis.recorder

import android.content.Context
import android.media.AudioRecord
import android.media.AudioRecord.STATE_UNINITIALIZED
import android.media.MediaPlayer
import android.util.Log
import com.voysis.createAudioRecorder
import com.voysis.sdk.R
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class AudioRecorderImpl(context: Context,
                        private val record: AudioRecord = createAudioRecorder(),
                        private val audioStart: MediaPlayer? = MediaPlayer.create(context, R.raw.voysis_on),
                        private val audioStop: MediaPlayer? = MediaPlayer.create(context, R.raw.voysis_off),
                        private val executor: Executor = Executors.newSingleThreadExecutor()) : AudioRecorder {

    private val inProgress = AtomicBoolean()
    private val maxBytes = 320000

    companion object {
        const val BUFFER_SIZE = 2048
    }

    @Synchronized
    override fun start(callback: OnDataResponse) {
        stopRecorder()
        inProgress.set(true)
        if (audioStart != null) {
            audioStart.setOnCompletionListener { _ -> startRecording(callback) }
            playAudio(audioStart)
        } else {
            startRecording(callback)
        }
    }

    private fun startRecording(callback: OnDataResponse) {
        if (inProgress.get()) {
            executor.execute { write(callback) }
        }
    }

    @Synchronized
    override fun stop() {
        stopRecorder()
        if (audioStop != null && inProgress.get()) {
            playAudio(audioStop)
            inProgress.set(false)
        }
    }

    private fun write(callback: OnDataResponse) {
        record.startRecording()
        callback.onRecordingStarted()
        val buf = ByteBuffer.allocate(BUFFER_SIZE)
        buf.clear()
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        var limit = 0
        try {
            while (isRecording() && limit < maxBytes) {
                bytesRead = record.read(buffer, 0, buffer.size)
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

    private fun isRecording(): Boolean = record.recordingState == AudioRecord.RECORDSTATE_RECORDING

    private fun stopRecorder() {
        if (record.state != STATE_UNINITIALIZED) {
            record.stop()
        }
    }

    private fun playAudio(audio: MediaPlayer) {
        audio.setVolume(5f, 5f)
        audio.start()
    }
}