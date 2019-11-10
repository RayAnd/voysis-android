package com.voysis.recorder

import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

interface AudioRecorder {

    /**
     * stop recording audio
     */
    fun stop()

    /**
     * Audio data from audio source.
     * @param listener called with ByteBuffer when audio data available.
     */
    fun registerWriteListener(listener: (ByteBuffer) -> Unit)

    fun removeWriteListener()

    /**
     * @return channel containing source to read from
     */
    fun start(): ReadableByteChannel

    fun mimeType(): MimeType?
}