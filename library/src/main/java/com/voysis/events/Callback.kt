package com.voysis.events

interface Callback {

    /**
     * @param event represents response from sdk
     */
    fun call(event: Event)

    /**
     * @param error provides throwable.
     */
    fun onError(error: VoysisException)
}