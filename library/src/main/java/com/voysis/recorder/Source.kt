package com.voysis.recorder

interface Source {
    fun startRecording()
    fun generateMimeType(): MimeType?
    fun generateBuffer(): ByteArray
    fun isRecording(): Boolean
    fun destroy()
    fun read(buffer: ByteArray, i: Int, size: Int): Int
    fun read(buffer: ShortArray, i: Int, size: Int): Int
}
