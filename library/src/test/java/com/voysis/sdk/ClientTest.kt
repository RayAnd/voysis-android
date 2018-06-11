package com.voysis.sdk

import com.voysis.model.request.Headers
import com.voysis.model.response.Audio
import com.voysis.model.response.QueryResponse
import com.voysis.model.response.Links
import com.voysis.model.response.Queries
import com.voysis.model.response.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

open class ClientTest {

    var headers = Headers("id", "agent")
    val tokenResponseValid = """{"token":"token","expiresAt":"$expiresIn1Minute"}"""
    var tokenResponseExpired = """{"token":"token","expiresAt":"$expiresIn25Seconds"}"""
    var response = """{"type":"response","entity":{"id":"2b95ff76-0de3-42d6-91c5-f61be84083bb","locale":"en-US","_links":{"self":{"href":"/queries/2b95ff76-0de3-42d6-91c5-f61be84083bb"},"audio":{"href":"/queries/2b95ff76-0de3-42d6-91c5-f61be84083bb/audio"}},"_embedded":{}},"requestId":"2","responseCode":201,"responseMessage":"Created"}"""
    var queryFutureResponse = """{"id":"12","locale":"en-US","conversationId":"1","queryType":"audio","audioQuery":{"mimeType":"audio/pcm"},"_links":{"self":{"href":"/queries/1"},"audio":{"href":"/queries/1/audio"}},"_embedded":{}}"""
    var notification = """{"type":"notification","notificationType":"query_complete","entity":{"id":"1","queryType":"audio","audioQuery":{"mimeType":"audio/pcm"},"intent":"Play Playlist","reply":{"text":"Playing My Favourite Music"},"entities":{"playlist_name":"My Favourite Music"},"_links":{"self":{"href":"/conversations/1/queries/1"},"audio":{"href":"/conversations/1/queries/1/audio"},"conversation":{"href":"/conversations/1"}}}}"""
    var vad = """{"type":"notification","notificationType":"vad_stop"}"""

    private val links: Links
        get() {
            val queries = Queries("http://test.com")
            val audio = Audio("http://test.com")
            return Links(null, queries, audio)
        }

    private val expiresIn25Seconds: String
        get() {
            return getExpiry(25)
        }

    private val expiresIn1Minute: String
        get() {
            return getExpiry(60)
        }

    private fun getExpiry(time: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.SECOND, time)
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
        return format.format(cal.time)
    }

    fun createAudioQueryResponse(): QueryResponse {
        val audioQueryResponse = QueryResponse(Query(), "")
        audioQueryResponse.links = links
        return audioQueryResponse
    }
}