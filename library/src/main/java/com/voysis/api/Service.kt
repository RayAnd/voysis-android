package com.voysis.api

import android.content.Context
import com.voysis.events.Callback
import com.voysis.events.VoysisException
import com.voysis.model.request.FeedbackData
import com.voysis.model.request.InteractionType
import com.voysis.model.request.Token
import com.voysis.recorder.AudioRecorder
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.ExecutionException

interface Service : Closeable {

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
     * @param context (optional) context of previous query
     * @param interactionType (optional) server parameter. Determines type of interaction. current are query, chatbot
     * @param source optional audio source
     * @throws IOException if reading/writing error occurs
     */
    @Throws(IOException::class)
    fun startAudioQuery(callback: Callback, context: Map<String, Any>? = null, interactionType: InteractionType? = null, source: AudioRecorder? = null)

    /**
     * This method executes a text query.
     * Note: this method will call back to the same thread that called `startTextQuery`
     *
     * @param text query to be executed
     * @param callback used by client application
     * @param context (optional) context of previous query
     * @param interactionType (optional) server parameter. Determines type of interaction. current are query, chatbot
     */
    fun sendTextQuery(text: String, callback: Callback, context: Map<String, Any>? = null, interactionType: InteractionType? = null)

    /**
     * Call to manually stop recording audio and process request
     */
    fun finish()

    /**
     * Call to cancel request.
     */
    fun cancel()

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
     * Audio profile id is a uuid set at library initialisation time.
     * After initialization it is persisted in local storage.
     * For information on the audio profile id see @link https://developers.voysis.com/docs/general-concepts#section-audio-profile-identifier
     * @return AudioProfileId.
     */
    fun getAudioProfileId(context: Context): String?

    /**
     * Call this to reset the audioProfileId.
     * @return new audioProfileId.
     */
    fun resetAudioProfileId(context: Context): String

    /**
     * Call this method send feedback following a successful query.
     * @throws ExecutionException if reading/writing error occurs
     * @throws VoysisException if query had not been made or token is invalid
     */
    @Throws(ExecutionException::class, VoysisException::class)
    fun sendFeedback(queryId: String, feedback: FeedbackData)

    /**
     * Service implementations may allocate and hold operating system resources that need to be explicitly released.
     * Closing the service will release these resources but once closed a Service cannot be reused.
     *
     * Note that allocating these resources may be a time intensive operation, such as model loading for local execution mode, and a calling application will need to be acutely aware of when it instantiates and discards Service implementations.
     *
     * @throws VoysisException if any memory could not be released
     */
    @Throws(VoysisException::class)
    override fun close()
}

enum class State {
    IDLE, BUSY
}
