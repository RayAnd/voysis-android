package com.voysis.wakeword

import com.voysis.api.Service
import com.voysis.events.Callback
import com.voysis.model.request.InteractionType

interface WakeWordService : Service {

    /**
     * This method kicks off wakeword detection and executes a query upon successful
     * detection of wakeword passing the input parameters to the query execution.
     *
     * stages and success/fail responses are captured in the callback interface.
     * We do not guarantee that the response callbacks will occure on the thread that called startListening
     *
     * This method should be implemented as nonblocking
     *
     * @param callback used by client application
     * @param context (optional) context of previous query
     * @param interactionType (optional) server parameter determining type of interaction. current are query, chatbot
     */
    fun startListening(callback: Callback, context: Map<String, Any>? = null, interactionType: InteractionType? = null)

    /**
     * This method stops the wakeword service
     */
    fun stopListening()
}