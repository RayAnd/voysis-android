package com.voysis.api

import com.voysis.model.request.FeedbackData
import com.voysis.model.request.InteractionType
import com.voysis.model.response.QueryResponse
import com.voysis.recorder.MimeType
import com.voysis.sevice.QueryFuture
import java.io.IOException
import java.nio.channels.ReadableByteChannel
import java.util.concurrent.Future

interface Client {

    /**
     * Call this method to create a new audioQueryRequest.
     *
     * @param context (optional) context response from previous request.
     * @param userId (optional) user ID that uniquely identified a user.
     * @param token session token assigned to the query.
     * @param mimeType information about recorded audio.
     * @return future containing audioQueryResponse json string or error.
     */
    fun createAudioQuery(context: Map<String, Any>? = null, interactionType: InteractionType?, userId: String?, token: String, mimeType: MimeType): Future<String>

    /**
     * Call this method to execute a text query.
     *
     * @param context (optional) context response from previous request.
     * @param text query to be executed.
     * @param userId (optional) user ID that uniquely identified a user.
     * @param token session token assigned to the query.
     * @return future containing response json string or error.
     */
    fun sendTextQuery(context: Map<String, Any>?, interactionType: InteractionType?, text: String, userId: String?, token: String): Future<String>

    /**
     * Call this method to stream audio to server and return an audioStreamResponse
     *
     * @param channel ReadableByteChannel containing bytes to be sent to server
     * @param queryResponse returned from `createAudioQuery(context, token)`
     * @return queryFuture containing audioStreamResponse json string or error.
     * @throws IOException if there is an issue reading from ReadableByteChannel
     */
    @Throws(IOException::class)
    fun streamAudio(channel: ReadableByteChannel, queryResponse: QueryResponse): QueryFuture

    /**
     * Call this to manually cancel open network requests
     */
    fun cancelStreaming()

    /**
     * Call this method to manually refresh the session token.
     * @param refreshToken refreshToken.
     * @return future containing Token json string or error.
     */
    fun refreshSessionToken(refreshToken: String): Future<String>

    fun sendFeedback(queryId: String, feedback: FeedbackData, token: String): Future<String>
}