package com.voysis.metrics

import android.os.Environment
import com.google.gson.Gson
import com.voysis.model.request.Duration
import java.io.File

class TimeCollector(private val filesDir: String) {

    fun save(id: String, duration: Duration) {
        if (isExternalStorageWritable()) {
            val file = File(filesDir, id.plus(".times"))
            file.writeText(Gson().toJson(duration).toString())
        }
    }

    private fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
}