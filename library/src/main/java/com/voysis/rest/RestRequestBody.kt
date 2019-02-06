package com.voysis.rest

import android.util.Log
import com.voysis.api.Config
import com.voysis.generateReadBufferSize
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.ByteString
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

/**
 * This class manages adding recorded audio to a buffer and sending audio through
 * a rest endpoint. This class is specifically used by okhttp
 */
class RestRequestBody(private val channel: ReadableByteChannel,
                      config: Config) : RequestBody() {
    private val readBufferSize = generateReadBufferSize(config)

    override fun contentType(): MediaType? {
        return MediaType.parse("audio/pcm")
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val buf = ByteBuffer.allocate(readBufferSize)
        while (channel.read(buf) > 0 || buf.position() > 0) {
            buf.flip()
            val byteString = ByteString.of(buf)
            sink.write(byteString)
            buf.compact()
            Log.d(RestRequestBody::class.java.simpleName, "send bytes " + byteString.size())
        }
    }
}