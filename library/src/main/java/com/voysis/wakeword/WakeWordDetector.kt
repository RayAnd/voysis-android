package com.voysis.wakeword

import com.voysis.events.WakeWordState
import java.nio.channels.ReadableByteChannel

interface WakeWordDetector {

    /**
     * listens for wakeword detection
     * @param source audio source used for detection
     * @param callback notifies user of wakeword state
     */
    fun listen(source: ReadableByteChannel, callback: (WakeWordState) -> Unit)

    /**
     * stops wakeword
     * @param callback (optional) notifies user when wakeword is no longer reading from audio source
     */
    fun stop(callback: ((WakeWordState) -> Unit)? = null)

    fun isActive(): Boolean
}
