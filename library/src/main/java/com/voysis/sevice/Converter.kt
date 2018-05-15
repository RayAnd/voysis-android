package com.voysis.sevice

import com.google.gson.Gson
import com.voysis.model.request.ApiRequest
import com.voysis.model.request.Headers
import com.voysis.model.request.SocketRequest
import com.voysis.model.response.ApiResponse
import com.voysis.model.response.SocketResponse

/**
 * This class converts objects sent to and received from the webSocket into their correct json or object format.
 */
class Converter(val headers: Headers, private val gson: Gson) {

    /**
     * @param path request path
     * @param entity ApiRequest object
     * @param requestId id used for request/response
     * @return request object in json format
     */
    fun encodeRequest(path: String, entity: ApiRequest?, token: String, requestId: Long): String {
        headers.authorization = "Bearer $token"
        return gson.toJson(SocketRequest(path, headers, entity, requestId.toString()), SocketRequest::class.java)
    }

    /**
     * @param stringResponse json response from server
     * @return socket response object
     */
    fun decodeResponse(stringResponse: String): SocketResponse<*> {
        return gson.fromJson(stringResponse, SocketResponse::class.java)
    }

    /**
     * @param response ApiResponse
     * @param type type object
     * @param <T> ApiResponse subtype
     * @return string converted to ApiResponse subtype object
     */
    fun <T : ApiResponse> convertResponse(response: String, type: Class<T>): T {
        return gson.fromJson(response, type)
    }

    /**
     * @param entity object
     * @return object converted to json
     */
    fun toJson(entity: Any): String {
        return gson.toJson(entity)
    }
}