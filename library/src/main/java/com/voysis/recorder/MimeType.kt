package com.voysis.recorder

data class MimeType(val sampleRate: Int,
                    val bitsPerSample: Int,
                    val encoding: String,
                    val bigEndian: Boolean,
                    val channels: Int) {

    fun getDescription(): String {
        return "audio/pcm;" +
                "encoding=$encoding;" +
                "rate=$sampleRate;" +
                "bits=$bitsPerSample;" +
                "channels=$channels;" +
                "big-endian=$bigEndian"
    }
}