package com.voysis.api

import com.voysis.recorder.AudioRecordParams

interface BaseConfig {

    val userId: String?

    /**
     * Configurable audio parameters object used by underlying AudioRecord object.
     * If left blank the AudioRecord object will be created using default parameters.
     * @note It is recommended to leave this object blank unless non default values are
     * explicitly required by a specific use case.
     * @return audioRecordParams
     */
    val audioRecordParams: AudioRecordParams?
}
