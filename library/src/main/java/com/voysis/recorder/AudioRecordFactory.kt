package com.voysis.recorder

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

class AudioRecordFactory(private val recordParams: AudioRecordParams) {
    fun make(): AudioRecord {
        return AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, recordParams.sampleRate!!, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, recordParams.recordBufferSize!!)
    }
}