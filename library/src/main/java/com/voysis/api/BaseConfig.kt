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

    /**
     * Sets type of service. Currently supported: default and wakeword.
     * Defaut supports single utterances. Wakeword supports single utterances wrapped with wakeword
     * functionality
     */
    val serviceType: ServiceType?

    /**
     * @return optional resourcePath is a reference to the necessary binary sets for local execution
     */
    val resourcePath: String? get() = ""

    /**
     * @return optional languageModelPath. If populated this path will be used for language model while in local execution
     */
    val languageModelPath: String? get() = ""
}

enum class ServiceType {
    DEFAULT,
    WAKEWORD
}
