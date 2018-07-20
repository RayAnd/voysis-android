package com.voysis.sdk

import android.content.Context
import android.media.AudioRecord
import android.media.AudioRecord.RECORDSTATE_RECORDING
import android.media.AudioRecord.RECORDSTATE_STOPPED
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.whenever
import com.voysis.recorder.AudioPlayer
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
    private lateinit var context: Context
    @Mock
    private lateinit var executorService: ExecutorService
    @Mock
    private lateinit var player: AudioPlayer
    @Mock
    private lateinit var onDataResposne: OnDataResponse

    var record = mock<AudioRecord> {
        on { recordingState } doReturn RECORDSTATE_RECORDING doReturn RECORDSTATE_STOPPED
    }

    private lateinit var audioRecorder: AudioRecorder

    @Before
    fun setup() {
        audioRecorder = spy(AudioRecorderImpl(context, player, record, executorService))
    }

    @Test
    fun testRecordingStart() {
        callCompletionListener()
        audioRecorder.start(onDataResposne)
        verify(player).playStartAudio(any())
        verify(executorService).execute(any())
    }

    @Test
    fun testRecordingStop() {
        audioRecorder.start(onDataResposne)
        audioRecorder.stop()
        verify(player).playStopAudio()
    }

    @Test
    fun testReadLoop() {
        callCompletionListener()
        doAnswer { invocation ->
            (invocation.getArgument<Any>(0) as Runnable).run()
            null
        }.whenever(executorService).execute(ArgumentMatchers.any(Runnable::class.java))
        audioRecorder.start(onDataResposne)
        verify(onDataResposne).onDataResponse(any())
        verify(onDataResposne).onComplete()
    }

    @Test
    fun testOnCompleteCalledWheStopRecordingBeforeSoundBiteCompletes() {
        doAnswer { invocation ->
            audioRecorder.stop()
            (invocation.getArgument<Any>(0) as () -> Unit).invoke()
        }.whenever(player).playStartAudio(anyOrNull())
        audioRecorder.start(onDataResposne)
        verify(onDataResposne, times(0)).onDataResponse(any())
        verify(onDataResposne).onComplete()
    }

    private fun callCompletionListener() {
        doAnswer { invocation ->
            (invocation.getArgument<Any>(0) as () -> Unit).invoke()
        }.whenever(player).playStartAudio(anyOrNull())
    }
}
