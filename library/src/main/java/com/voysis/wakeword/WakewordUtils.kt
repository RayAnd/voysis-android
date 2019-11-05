package com.voysis.wakeword

import org.apache.commons.collections.buffer.CircularFifoBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun addBytesToBuffer(source: ByteBuffer, ringBuffer: CircularFifoBuffer) {
    val array = ByteArray(WakeWordDetectorImpl.byteWindowSize)
    source.get(array, 0, WakeWordDetectorImpl.byteWindowSize)
    addToBuffer(array, ringBuffer)
}

fun addToBuffer(array: ByteArray, buffer: CircularFifoBuffer) {
    for (i in array.indices step 2) {
        buffer.add(convertToFloat(array, i))
    }
}

fun convertToFloat(array: ByteArray, i: Int): Float {
    val bb = ByteBuffer.allocate(2)
    bb.order(ByteOrder.LITTLE_ENDIAN)
    bb.put(array[i])
    bb.put(array[i + 1])
    return bb.getShort(0).toFloat() / 32768f
}