package com.voysis.recorder

import android.content.Context
import android.media.MediaPlayer
import com.voysis.sdk.R

class AudioPlayer(private val context: Context,
                  private var audioStart: MediaPlayer? = MediaPlayer.create(context, R.raw.voysis_on),
                  private var audioStop: MediaPlayer? = MediaPlayer.create(context, R.raw.voysis_off)) {

    fun playStartAudio(callback: () -> Unit) {
        audioStart = audioStart ?: MediaPlayer.create(context, R.raw.voysis_on)
        audioStart?.setOnCompletionListener {
            callback()
            audioStart?.reset()
            audioStart?.release()
            audioStart = null
        }
        audioStart?.setVolume(5f, 5f)
        audioStart?.start()
    }

    fun playStopAudio() {
        audioStop = audioStop ?: MediaPlayer.create(context, R.raw.voysis_off)
        audioStop?.setOnCompletionListener {
            audioStop?.reset()
            audioStop?.release()
            audioStop = null
        }
        audioStop?.setVolume(5f, 5f)
        audioStop?.start()
    }
}
