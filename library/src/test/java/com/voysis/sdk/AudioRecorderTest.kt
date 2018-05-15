package com.voysis.sdk

import android.content.Context
import android.media.AudioRecord
import android.media.MediaPlayer
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.whenever
import com.voysis.recorder.AudioRecorder
import com.voysis.recorder.AudioRecorderImpl
import com.voysis.recorder.OnDataResponse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.ExecutorService

@RunWith(MockitoJUnitRunner::class)
class AudioRecorderTest {

    @Mock
    private lateinit var start: MediaPlayer
    @Mock
    private lateinit var stop: MediaPlayer
    @Mock
    private lateinit var record: AudioRecord
    @Mock
    private lateinit var context: Context
    @Mock
    private lateinit var executorService: ExecutorService
    @Mock
    private lateinit var onDataResposne: OnDataResponse

    private lateinit var audioRecorder: AudioRecorder

    @Before
    fun setup() {
        audioRecorder = spy(AudioRecorderImpl(context, record, start, stop, executorService))
    }

    @Test
    fun testRecordingStart() {
        audioRecorder.start(onDataResposne)
        verify<MediaPlayer>(start).setVolume(5f, 5f)
        verify<MediaPlayer>(start).start()
    }

    @Test
    fun testRecordingStop() {
        audioRecorder.start(onDataResposne)
        audioRecorder.stop()
        verify<MediaPlayer>(stop).setVolume(5f, 5f)
        verify<MediaPlayer>(stop).start()
    }

    @Test
    fun testReadLoop() {
        callCompletionListener()
        doReturn(AudioRecord.RECORDSTATE_RECORDING).doReturn(AudioRecord.RECORDSTATE_STOPPED).whenever(record).recordingState
        doAnswer { invocation ->
            (invocation.getArgument<Any>(0) as Runnable).run()
            null
        }.whenever(executorService).execute(ArgumentMatchers.any(Runnable::class.java))
        audioRecorder.start(onDataResposne)
        verify(onDataResposne).onDataResponse(any())
        verify(onDataResposne).onComplete()
    }

    private fun callCompletionListener() {
        doAnswer { invocation ->
            (invocation.getArgument<Any>(0) as MediaPlayer.OnCompletionListener).onCompletion(null)
            null
        }.whenever(start).setOnCompletionListener(ArgumentMatchers.any(MediaPlayer.OnCompletionListener::class.java))
    }
}
