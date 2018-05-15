package com.voysis.events

import com.voysis.model.response.ApiResponse

class Event(private val response: ApiResponse?, val eventType: EventType) {

    /**
     * @param <T> type of apiResponse
     * @return apiResponse
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : ApiResponse> getResponse(): T {
        return response as T
    }
}