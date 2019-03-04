package com.voysis.recorder

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

class AudioRecordFactory(private val recordParams: AudioRecordParams) : Factory<AudioRecord> {

    override fun provide(): AudioRecord {
        return AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, recordParams.sampleRate!!, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, recordParams.recordBufferSize!!)
    }
}

internal interface Factory<T> {
    fun provide(): T
}