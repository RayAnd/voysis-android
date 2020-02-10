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
    fun extractModel(fName: String, subDir: String? = null): String
}

/**
 * Local Model Assests extractor
 *
 * @param context current context
 * @param filesDir optional input files directory
 * @param assetManager optional app assets manager
 */
internal class LocalModelAssetProvider(context: Context,
                                       private val filesDir: String = context.filesDir.absolutePath.toString(),
                                       private val assetManager: AssetManager = context.assets) : ModelFileProvider {

    private val processedFiles = mutableSetOf<String>()

    override fun extractModel(fName: String, subDir: String?): String {
        try {
            val fullPath = filesDir.plus("/").plus(fName).plus("/")
            val assetPath = if (subDir != null) fName.plus("/$subDir") else fName
            val assets = assetManager.list(assetPath)
            if (assets!!.isEmpty()) {
                copyFile(assetPath)
            } else {
                for (i in assets.indices) {
                    extractModel(assetPath.plus("/").plus(assets[i]))
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
            val newFileName = filesDir.plus("/").plus(filename)
            val file = File(newFileName)
            val dir = File(file.parent!!)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            if (file.exists()) {
                Log.println(Log.VERBOSE, "LocalModelAssetProvider", "File: $filename already exists in path: $filesDir")
                processedFiles.add(newFileName)
                return
            } else {
                deleteFilesInDir(dir)
            }
            val copiedFileOutput = FileOutputStream(newFileName)
            assetManager.open(filename).copyTo(copiedFileOutput, 4096)
            copiedFileOutput.flush()
            copiedFileOutput.close()
            processedFiles.add(newFileName)
        } catch (e: Exception) {
            Log.e("AssetProvider", "Error copying file: $filename", e)
        }
    }

    private fun deleteFilesInDir(dir: File) {
        dir.listFiles()?.forEach {
            if (!processedFiles.contains(it.path)) {
                Log.println(Log.VERBOSE, "LocalModelAssetProvider", "Deleting file: ${it.name} from directory: $filesDir")
                it.delete()
            }
        }
    }
}