package com.voysis.wakeword

import com.voysis.events.WakeWordState
import java.nio.channels.ReadableByteChannel

interface WakeWordDetector {

    /**
     * listens to for wakeword detection
     * @param source audio source used for detection
     * @param callback notifys user of wakeword state
     */
    fun listen(source: ReadableByteChannel, callback: (WakeWordState) -> Unit)

    /**
     * stops wakeword
     * @param callback notifys user when wakeword is no longer reading from audio source
     */
    fun stop(callback: (WakeWordState) -> Unit)

    /**
     * stops wakeword without notifying user
     */
    fun cancel()

    fun isActive(): Boolean
}
