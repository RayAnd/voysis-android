package com.voysis.sdk

import android.media.AudioFormat
import android.media.AudioRecord
import com.nhaarman.mockito_kotlin.doNothing
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.whenever
import com.voysis.recorder.AudioRecordFactory
import com.voysis.recorder.AudioSource
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SourceManagerTest : ClientTest() {

    private lateinit var source: AudioSource

    @Mock
    private lateinit var factory: AudioRecordFactory

    private var record = mock<AudioRecord> {
        on { audioFormat } doReturn AudioFormat.ENCODING_PCM_16BIT
        on { sampleRate } doReturn 16000
    }

    @Before
    fun setup() {
        doReturn(record).whenever(factory).make()
        doNothing().whenever(record).startRecording()
        source = spy(AudioSource(factory))
    }

    @Test
    fun testGetMimeType() {
        source.startRecording()
        val mimeType = source.generateMimeType()!!
        Assert.assertEquals(mimeType.encoding, "signed-int")
        Assert.assertEquals(mimeType.bitsPerSample, 16)
        Assert.assertEquals(mimeType.sampleRate, 16000)
        Assert.assertEquals(mimeType.bigEndian, false)
    }

    @Test(expected = RuntimeException::class)
    fun testGetMimeTypeThrowException() {
        doReturn(AudioFormat.ENCODING_INVALID).whenever(record).audioFormat
        val source = spy(AudioSource(factory))
        source.startRecording()
        source.generateMimeType()
    }
}