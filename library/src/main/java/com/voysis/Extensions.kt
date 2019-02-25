package com.voysis

import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Build
import com.voysis.recorder.MimeType
import java.lang.RuntimeException

fun AudioRecord.generateMimeType(): MimeType {
    return MimeType(sampleRate = sampleRate,
            bitsPerSample = getBitsPerSample(),
            bigEndian = false,
            channels = channelCount,
            encoding = getEncodingString())
}

fun AudioRecord.getBitsPerSample() : Int {
    if (Build.VERSION.SDK_INT >= 21) {
        return when (audioFormat) {
            AudioFormat.ENCODING_PCM_FLOAT -> 32
            AudioFormat.ENCODING_PCM_16BIT -> 16
            AudioFormat.ENCODING_PCM_8BIT -> 8
            else -> {
              throw RuntimeException("invalid encoding type")
            }
        }
    } else {
        return when (audioFormat) {
            AudioFormat.ENCODING_PCM_16BIT -> 16
            AudioFormat.ENCODING_PCM_8BIT -> 8
            else -> {
                throw RuntimeException("invalid encoding type")
            }
        }
    }
}

fun AudioRecord.getEncodingString(): String {
    return if (audioFormat == AudioFormat.ENCODING_PCM_FLOAT) {
        "float"
    } else {
        "signed-int"
    }
}