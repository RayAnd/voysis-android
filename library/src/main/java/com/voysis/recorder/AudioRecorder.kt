package com.voysis.recorder

import java.nio.ByteBuffer

interface AudioRecorder {

    /**
     * stop recording audio
     */
    fun stop()

    /**
     * @param callback called periodically as data is returned from the mic.
     */
    fun start(callback: OnDataResponse)

    /**
     * @return valid MimeType object.
     */
    fun getMimeType(): MimeType
}

interface OnDataResponse {

    /**
     * called when ByteBuffer fills
     * @param buffer containing recorded audio in bytes.
     */
    fun onDataResponse(buffer: ByteBuffer)

    /**
     * called when microphone begins recording audio
     */
    fun onRecordingStarted()

    /**
     * called when mic record loop completes
     */
    fun onComplete()
}