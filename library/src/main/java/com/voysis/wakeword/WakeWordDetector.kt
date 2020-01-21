package com.voysis.wakeword

import com.voysis.events.WakeWordState

interface WakeWordDetector {

    /**
     * listens for wakeword detection
     * @param callback notifies user of wakeword state
     */
    fun listen(callback: (WakeWordState) -> Unit)

    /**
     * Stops wakeword detection on source input. Note: Often a user will want to continue using
     * the same source once detection has completed. This method stops the detection loop, freeing
     * up the source for further use. Note: This method does not close the source input.
     * @param callback (optional) notifies user when wakeword is no longer reading from audio source
     */
    fun stopDetection(callback: ((WakeWordState) -> Unit)? = null)

    /**
     * Closes the source recorder input. If WakeWordDetector is used in isolation, ie: outside wakewordService
     * the source will need to be closed once the user has finished with detection.
     */
    fun closeSource()

    fun isActive(): Boolean
}

enum class DetectorType {
    //wakeword will stop executing after detection freeing up audio source
    SINGLE,
    //wakeword will continue to execute until stopped
    CONTINIOUS
}