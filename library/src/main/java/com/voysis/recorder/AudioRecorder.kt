package com.voysis.recorder

import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

interface AudioRecorder {

    companion object {
        const val DEFAULT_READ_BUFFER_SIZE = 4096
        const val DEFAULT_RECORD_BUFFER_SIZE = 16384
    }

    var source: Source

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

    fun invokeListener(array: ShortArray)
}