package com.voysis.events

import com.voysis.model.response.QueryResponse
import com.voysis.model.response.StreamResponse
import java.nio.ByteBuffer

interface Callback {

    /**
     * Called when a successful has been returned from server.
     * @param response object representation of successful json response.
     */
    fun success(response: StreamResponse)

    /**
     * @param error provides throwable.
     */
    fun failure(error: VoysisException)

    /**
     * Called when microphone is turned on and recording begins.
     */
    fun recordingStarted() {
        //no implementation
    }

    /**
     * Called when successful connection is made to server.
     * @param query information about the connection.
     */
    fun queryResponse(query: QueryResponse) {
        //no implementation
    }

    /**
     * Called when recording finishes.
     * @param reason enum explaining why recording finished.
     */
    fun recordingFinished(reason: FinishedReason) {
        //no implementation
    }

    /**
     * Audio data recorded from microphone.
     * @param buffer containing audio data.
     */
    fun audioData(buffer: ByteBuffer) {
        //no implementation
    }
}