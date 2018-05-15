package com.voysis.model.response

import com.google.gson.Gson

data class AudioStreamResponse(private val entities: Any? = null,
                               val context: Map<String, Any>? = null,
                               val audioQuery: Query? = null,
                               val textQuery: TextQuery? = null,
                               val reply: Reply? = null,
                               val intent: String? = null) : ApiResponse() {
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