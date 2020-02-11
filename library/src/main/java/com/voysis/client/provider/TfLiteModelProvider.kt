package com.voysis.client.provider

import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class TfLiteModelProvider(private val resourcePath: String, private var assetManager: AssetManager) {

    fun createTfLiteInterpreter(path: String): Interpreter {
        return Interpreter(loadTfLiteFile(path))
    }

    private fun loadTfLiteFile(modelPath: String): ByteBuffer {
        val fileDescriptor: AssetFileDescriptor = assetManager.openFd("$resourcePath/".plus(modelPath))
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset: Long = fileDescriptor.startOffset
        val declaredLength: Long = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}