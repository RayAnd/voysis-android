package com.voysis.sdk

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioRecord.RECORDSTATE_RECORDING
import android.media.AudioRecord.RECORDSTATE_STOPPED
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.doNothing
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.whenever
import com.voysis.calculateMaxRecordingLength
import com.voysis.generateMimeType
import com.voysis.recorder.AudioRecordParams
import com.voysis.recorder.AudioRecorder
import com.voysis.recorder.AudioRecorderImpl
import com.voysis.wakeword.WakeWordDetectorImpl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService

@RunWith(MockitoJUnitRunner::class)
class AudioRecorderTest : ClientTest() {

    @Mock
    private lateinit var executorService: ExecutorService
    @Mock
    private lateinit var factory: () -> AudioRecord

    private lateinit var audioRecorder: AudioRecorder

    private var record = mock<AudioRecord> {
        on { recordingState } doReturn RECORDSTATE_RECORDING doReturn RECORDSTATE_STOPPED
        on { audioFormat } doReturn AudioFormat.ENCODING_PCM_16BIT
        on { sampleRate } doReturn 16000
        on { read(any<ByteArray>(), any(), any()) } doReturn 4096
    }

    @Before
    fun setup() {
        doNothing().whenever(record).startRecording()
        doReturn(record).whenever(factory).invoke()
        doAnswer { invocation ->
            (invocation.getArgument<Any>(0) as Runnable).run()
            null
        }.whenever(executorService).execute(ArgumentMatchers.any(Runnable::class.java))

        audioRecorder = spy(AudioRecorderImpl(AudioRecordParams(16000, 4096, 16000), factory, executorService))
    }

    @Test
    fun testWriteLoop() {
        val buffer = ByteBuffer.allocate(WakeWordDetectorImpl.byteSampleSize)
        val source = audioRecorder.start()
        assertEquals(source.read(buffer) , 4096)
        assertEquals(source.read(buffer) , -1)
    }

    @Test
    fun testGetMimeType() {
        val record = factory()
        val mimeType = record.generateMimeType()
        assertEquals(mimeType.encoding, "signed-int")
        assertEquals(mimeType.bitsPerSample, 16)
        assertEquals(mimeType.sampleRate, 16000)
        assertEquals(mimeType.bigEndian, false)
    }

    @Test(expected = RuntimeException::class)
    fun testGetMimeTypeThrowException() {
        doReturn(AudioFormat.ENCODING_INVALID).whenever(record).audioFormat
        audioRecorder = spy(AudioRecorderImpl(AudioRecordParams(-1, 4096, 16000), factory, executorService))
        audioRecorder.start()
        audioRecorder.mimeType()
    }

    @Test
    fun testMaxRecordingLength() {
        assertEquals(320000, calculateMaxRecordingLength(16000))
        assertEquals(820000, calculateMaxRecordingLength(41000))
        assertEquals(960000, calculateMaxRecordingLength(48000))
    }
}
