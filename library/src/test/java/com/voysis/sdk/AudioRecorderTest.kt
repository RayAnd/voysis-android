package com.voysis.sdk

import android.media.AudioRecord
import com.nhaarman.mockito_kotlin.*
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.whenever
import com.voysis.calculateMaxRecordingLength
import com.voysis.metrics.AudioSaver
import com.voysis.recorder.AudioRecordFactory
import com.voysis.recorder.AudioRecordParams
import com.voysis.recorder.AudioRecorder
import com.voysis.recorder.AudioRecorderImpl
import com.voysis.recorder.AudioSource
import com.voysis.wakeword.WakeWordDetectorImpl
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.Mockito.argThat
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService

@RunWith(MockitoJUnitRunner::class)
class AudioRecorderTest : ClientTest() {

    @Mock
    private lateinit var executorService: ExecutorService

    private lateinit var source: AudioSource

    private lateinit var audioRecorder: AudioRecorder

    @Mock
    private lateinit var factory: AudioRecordFactory

    private var record = mock<AudioRecord> {
        on { read(any<ByteArray>(), any(), any()) } doReturn 4096
    }

    @Before
    fun setup() {
        doReturn(record).whenever(factory).make()
        source = spy(AudioSource(factory))

        doAnswer { invocation ->
            (invocation.getArgument<Any>(0) as Runnable).run()
            null
        }.whenever(executorService).execute(ArgumentMatchers.any(Runnable::class.java))
        audioRecorder = spy(AudioRecorderImpl(AudioRecordParams(-1, 4096, 16000), source, executorService))
    }

    @Test
    fun testWriteLoop() {
        doReturn(ByteArray(4096)).whenever(source).generateBuffer()
        doReturn(false).doReturn(true).doReturn(false).whenever(source).isRecording()
        val buffer = ByteBuffer.allocate(WakeWordDetectorImpl.sourceBufferSize)
        val byteChannel = audioRecorder.start()
        assertEquals(byteChannel.read(buffer), 4096)
        assertEquals(byteChannel.read(buffer), -1)
    }

    @Test
    fun testMaxRecordingLength() {
        assertEquals(320000, calculateMaxRecordingLength(16000))
        assertEquals(820000, calculateMaxRecordingLength(41000))
        assertEquals(960000, calculateMaxRecordingLength(48000))
    }

    @Test
    fun testInvokeListener() {
        val audioSaver = mock(AudioSaver::class.java)
        val expectedBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        expectedBuffer.putShort(312)
        expectedBuffer.putShort(432)
        audioRecorder.registerWriteListener { audioSaver.write(it) }

        audioRecorder.invokeListener(shortArrayOf(312, 432))
        val captor = argumentCaptor<ByteBuffer>()
        verify(audioSaver).write(captor.capture())
        assertEquals(4,captor.firstValue.array().size)
        assertTrue(expectedBuffer.array().contentEquals(captor.firstValue.array()))
    }
}
