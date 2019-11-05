package com.voysis.wakeword

import com.voysis.api.Service
import com.voysis.events.Callback
import com.voysis.model.request.InteractionType

interface WakeWordService : Service {

    fun startListening(context: Map<String, Any>? = null, callback: Callback, interactionType: InteractionType? = null)

    fun stopListening()
}