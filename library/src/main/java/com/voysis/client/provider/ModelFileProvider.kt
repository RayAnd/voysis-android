package com.voysis.client.provider

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Model file extract to get ready model files
 */
internal interface ModelFileProvider {

    /**
     * Read file and write it in an accessible location
     *
     * @param fName file Name to extract
     * @return model extracted file location
     */
    fun extractModel(fName: String): String
}

/**
 * Local Model Assests extractor
 *
 * @param context current context
 * @param filesDir optional input files directory
 * @param assetManager optional app assets manager
 */
internal class LocalModelAssetProvider(context: Context,
                                       private val filesDir: String = context.filesDir.toString(),
                                       private val assetManager: AssetManager = context.assets) : ModelFileProvider {

    override fun extractModel(path: String): String {
        try {
            var fullPath: String
            val assets = assetManager.list(path)
            if (assets.isEmpty()) {
                fullPath = path
                copyFile(path)
            } else {
                fullPath = filesDir.plus(path)
                val dir = File(fullPath)
                fullPath = fullPath.plus("/")
                if (!dir.exists())
                    dir.mkdir()
                for (i in assets!!.indices) {
                    extractModel(path + "/" + assets[i])
                }
            }
            return fullPath
        } catch (ex: IOException) {
            Log.e("AssetProvider", "I/O Exception", ex)
        }
        return ""
    }

    private fun copyFile(filename: String) {
        try {
            val newFileName = filesDir.plus(filename)
            var copiedFileOutput = FileOutputStream(newFileName)
            assetManager.open(filename).copyTo(copiedFileOutput, 4096)
            copiedFileOutput.flush()
            copiedFileOutput.close()
        } catch (e: Exception) {
            Log.e("AssetProvider", e.message)
        }
    }
}