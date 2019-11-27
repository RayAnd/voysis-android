package com.voysis.sdk

import android.media.AudioRecord
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.whenever
import com.voysis.calculateMaxRecordingLength
import com.voysis.recorder.AudioRecordFactory
import com.voysis.recorder.AudioRecordParams
import com.voysis.recorder.AudioRecorder
import com.voysis.recorder.AudioRecorderImpl
import com.voysis.recorder.SourceManager
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

    private lateinit var source: SourceManager

    private lateinit var audioRecorder: AudioRecorder

    @Mock
    private lateinit var factory: AudioRecordFactory

    private var record = mock<AudioRecord> {
        on { recordingState } doReturn AudioRecord.RECORDSTATE_RECORDING doReturn AudioRecord.RECORDSTATE_STOPPED
        on { read(any<ByteArray>(), any(), any()) } doReturn 4096
    }

    @Before
    fun setup() {
        doReturn(record).whenever(factory).make()
        source = SourceManager(factory, AudioRecordParams(-1, 4096, 16000))
        doAnswer { invocation ->
            (invocation.getArgument<Any>(0) as Runnable).run()
            null
        }.whenever(executorService).execute(ArgumentMatchers.any(Runnable::class.java))
        audioRecorder = spy(AudioRecorderImpl(AudioRecordParams(-1, 4096, 16000), source, executorService))
    }

    @Test
    fun testWriteLoop() {
        val buffer = ByteBuffer.allocate(WakeWordDetectorImpl.byteSampleSize)
        val source = audioRecorder.start()
        assertEquals(source.read(buffer) , 4096)
        assertEquals(source.read(buffer) , -1)
    }

    @Test
    fun testMaxRecordingLength() {
        assertEquals(320000, calculateMaxRecordingLength(16000))
        assertEquals(820000, calculateMaxRecordingLength(41000))
        assertEquals(960000, calculateMaxRecordingLength(48000))
    }
}
