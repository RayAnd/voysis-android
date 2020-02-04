package com.voysis.wakeword

import org.apache.commons.collections.buffer.CircularFifoBuffer
import org.tensorflow.lite.Interpreter

fun processWakeword(buffer: CircularFifoBuffer, interpreter: Interpreter): Float {
    val input = buffer.toArray().map { it as Float }.toFloatArray()
    val output = Array(1) { FloatArray(2) }
    interpreter.run(input, output)
    return output[0][1]
}

fun isAboveThreshold(input: Float, threshold: Float): Int = if (input >= threshold) {
    1
} else {
    0
}


fun detected(countQueue: CircularFifoBuffer, leftThreshold: Int): Boolean {
    val count = countQueue.toArray().map { it as Int }.toIntArray()
    return count.filter { it == 1 }.size > leftThreshold * 0.5
}