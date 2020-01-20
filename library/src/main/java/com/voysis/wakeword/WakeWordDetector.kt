package com.voysis.wakeword

import com.voysis.events.WakeWordState

interface WakeWordDetector {

    /**
     * listens for wakeword detection
     * @param callback notifies user of wakeword state
     */
    fun listen(callback: (WakeWordState) -> Unit)

    /**
     * stops wakeword
     * @param callback (optional) notifies user when wakeword is no longer reading from audio source
     */
    fun stop(callback: ((WakeWordState) -> Unit)? = null)

    fun isActive(): Boolean
}

enum class DetectorType {
    //wakeword will stop executing after detection freeing up audio source
    SINGLE,
    //wakeword will continue to execute until stopped
    CONTINIOUS
}