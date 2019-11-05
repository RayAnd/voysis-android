package com.voysis.recorder

import java.nio.channels.ReadableByteChannel

interface AudioRecorder {

    /**
     * stop recording audio
     */
    fun stop()

    /**
     * @return channel containing source to read from
     */
    fun start(): ReadableByteChannel

    fun mimeType(): MimeType?
}