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
     * Will create a ReadableByteChannel to be read from and will begin writing to channel using
     * audio source. Subsequent calls will return the same channel until stop() is called
     * @return channel containing source to read from
     */
    fun start(): ReadableByteChannel

    fun mimeType(): MimeType?
}