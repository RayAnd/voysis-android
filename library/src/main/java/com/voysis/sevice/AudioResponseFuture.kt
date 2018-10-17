package com.voysis.sevice

import com.voysis.api.StreamingStoppedReason
import com.voysis.api.StreamingStoppedReason.NONE
import java.util.concurrent.atomic.AtomicReference

class AudioResponseFuture : QueryFuture() {

    private val reason = AtomicReference(NONE)

    /**
     * @param enum reason for stream to complete
     */
    var responseReason: StreamingStoppedReason
        get() = reason.get()
        set(reason) = this.reason.set(reason)
}