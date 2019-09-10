package com.voysis.recorder

data class AudioRecordParams(val sampleRate: Int? = null, val readBufferSize: Int? = null, val recordBufferSize: Int? = null)