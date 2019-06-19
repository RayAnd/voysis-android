package com.voysis.model.response

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class StreamResponse(@field:SerializedName("entities") private val entities: Any? = null,
                          @field:SerializedName("context") val context: Map<String, Any>? = null,
                          @field:SerializedName("audioQuery") val audioQuery: AudioQuery? = null,
                          @field:SerializedName("textQuery") val textQuery: TextQuery? = null,
                          @field:SerializedName("reply") val reply: Reply? = null,
                          @field:SerializedName("dmReply") val dmReply: DmReply? = null,
                          @field:SerializedName("intent") val intent: String? = null) : ApiResponse() {
    /**
     * @param clazz class object
     * @param <T> respone type
     * @return entities response
    </T> */
    fun <T> parseEntities(clazz: Class<T>): T {
        val gson = Gson()
        val jsonElement = gson.toJsonTree(entities)
        return gson.fromJson(jsonElement, clazz)
    }
}