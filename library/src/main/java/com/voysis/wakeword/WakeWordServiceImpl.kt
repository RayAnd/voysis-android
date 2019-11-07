package com.voysis.wakeword

import android.content.Context
import com.voysis.api.Service
import com.voysis.api.State
import com.voysis.events.Callback
import com.voysis.events.WakeWordState
import com.voysis.model.request.FeedbackData
import com.voysis.model.request.InteractionType
import com.voysis.model.request.Token
import com.voysis.recorder.AudioRecorder

internal class WakeWordServiceImpl(private val recorder: AudioRecorder,
                                   private val wakeword: WakeWordDetector,
                                   private val serviceImpl: Service) : WakeWordService {

    override val state: State
        get() = serviceImpl.state

    override fun startListening(callback: Callback, context: Map<String, Any>?, interactionType: InteractionType?) {
        if (state == State.IDLE) {
            val pipe = recorder.start()
            wakeword.listen(pipe) {
                callback.wakeword(it)
                if (it == WakeWordState.DETECTED) {
                    serviceImpl.startAudioQuery(callback, context, interactionType, recorder)
                }
            }
        }
    }

    override fun stopListening() {
        wakeword.stop()
    }

    override fun startAudioQuery(callback: Callback, context: Map<String, Any>?, interactionType: InteractionType?, source: AudioRecorder?) {
        if (wakeword.isActive()) {
            wakeword.stop {
                serviceImpl.startAudioQuery(callback, context, interactionType, recorder)
            }
        } else {
            serviceImpl.startAudioQuery(callback, context, interactionType, recorder)
        }
    }

    override fun sendTextQuery(text: String, callback: Callback, context: Map<String, Any>?, interactionType: InteractionType?) {
        serviceImpl.sendTextQuery(text, callback, context, interactionType)
    }

    override fun finish() {
        if (serviceImpl.state == State.BUSY) {
            serviceImpl.finish()
        }
    }

    override fun cancel() {
        wakeword.cancel()
        serviceImpl.cancel()
    }

    override fun refreshSessionToken(): Token = serviceImpl.refreshSessionToken()

    override fun getAudioProfileId(context: Context): String? = serviceImpl.getAudioProfileId(context)

    override fun resetAudioProfileId(context: Context): String = serviceImpl.resetAudioProfileId(context)

    override fun sendFeedback(queryId: String, feedback: FeedbackData) = serviceImpl.sendFeedback(queryId, feedback)

    override fun close() {
        cancel()
        serviceImpl.close()
    }
}