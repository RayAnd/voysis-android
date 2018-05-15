package com.voysis.api

import com.voysis.events.Callback
import com.voysis.model.request.Token

import java.io.IOException
import java.util.concurrent.ExecutionException

interface Service {

    /**
     * when `startAudioQuery` has been called, state will turn to `State.BUSY`.
     * when `EventType.AUDIO_QUERY_COMPLETE is returned from the Callback.onCall(event Event) the state will return to `State.IDLE`
     * if an error occurs the state will also return to `State.IDLE`
     *
     * - Returns: state of current audio stream request
     */
    val state: State

    /**
     * This method kicks off an audio query. Under the hood this method invokes
     * `Voysis.Client.refreshSessionToken` -> `Voysis.Client.createAudioQuery` -> `Voysis.Client.streamAudio`
     * which connects to the server, checks the session token , initiates an audio query request and
     * begins streaming audio from the microphone through the open connection
     * Note: this method will call back to the same thread that called `startAudioQuery`
     * for more information on the websocket api calls see https://developers.voysis.com/docs
     *
     * @param callback used by client application
     * @throws IOException if reading/writing error occurs
     */
    @Throws(IOException::class)
    fun startAudioQuery(context: Map<String, Any>? = null, callback: Callback)

    /**
     * Call this method to manually refresh the session token.
     * Note: The sdk automatically handles checking/refreshing and storing the session token.
     * This method is called internally by `startAudioQuery`.
     * Calling this method will preemptively refresh the session token for users who want to manage token refresh themselves.
     *
     * @return Token .
     * @throws ExecutionException if reading/writing error occurs
     */
    @Throws(ExecutionException::class)
    fun refreshSessionToken(): Token

    /**
     * Call to manually stop recording audio and process request
     */
    fun finish()

    /**
     * Call to cancel request.
     */
    fun cancel()
}

enum class State {
    IDLE, BUSY
}