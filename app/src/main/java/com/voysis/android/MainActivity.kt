package com.voysis.android

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import com.google.gson.GsonBuilder
import com.voysis.api.Service
import com.voysis.api.ServiceProvider
import com.voysis.api.State
import com.voysis.events.Callback
import com.voysis.events.Event
import com.voysis.events.EventType
import com.voysis.events.VoysisException
import com.voysis.model.request.FeedbackEntity
import com.voysis.model.response.ApiResponse
import com.voysis.model.response.AudioStreamResponse
import com.voysis.sevice.DataConfig
import kotlinx.android.synthetic.main.activity_main.cancel
import kotlinx.android.synthetic.main.activity_main.eventText
import kotlinx.android.synthetic.main.activity_main.responseText
import kotlinx.android.synthetic.main.activity_main.start
import kotlinx.android.synthetic.main.activity_main.stop
import java.net.URL
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private val url = "INSERT_URL"
    private val config = DataConfig(isVadEnabled = true, url = URL(url), refreshToken = "INSERT_TOKEN", userId = "")
    private lateinit var service: Service
    private val executor = Executors.newSingleThreadExecutor()
    private var context: Map<String, Any>? = null
    private val gson = GsonBuilder().setPrettyPrinting().create();
    private var feedbackEntity = FeedbackEntity()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //audio permissions must be accepted before using the Voysis Service.
        acceptAudioPermission()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            val alertDialog = AlertDialog.Builder(this, R.style.Theme_AppCompat_Dialog_Alert)
                    .setMessage("must accept permission")
                    .setTitle("Alert")
                    .setCancelable(false)
                    .create()
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", { dialog, _ ->
                dialog.dismiss()
                this@MainActivity.acceptAudioPermission()
            })
            alertDialog.show()
        } else {
            init()
        }
    }

    override fun onStop() {
        super.onStop()
        service.cancel()
    }

    private fun init() {
        service = ServiceProvider().make(applicationContext, config)
        start.setOnClickListener { onStartClicked() }
        stop.setOnClickListener { service.finish() }
        cancel.setOnClickListener { service.cancel() }
    }

    private fun onStartClicked() {
        if (service.state == State.IDLE) {
            executor.submit({ startAudioQuery() })
        } else {
            Toast.makeText(this, "query in progress ", LENGTH_SHORT).show()
        }
    }

    private fun startAudioQuery() {
        service.startAudioQuery(context = context, callback = object : Callback {
            override fun call(event: Event) {
                when (event.eventType) {
                    EventType.RECORDING_STARTED -> setText("Recording Started")
                    EventType.RECORDING_FINISHED -> setText("Recording Finished")
                    EventType.VAD_RECEIVED -> vadReceived()
                    EventType.AUDIO_QUERY_CREATED -> setText("Query Created")
                    EventType.AUDIO_QUERY_COMPLETED -> onResponse(event.getResponse<AudioStreamResponse>())
                }
            }

            override fun onError(error: VoysisException) {
                setText(error.message.toString())
            }
        })
    }

    private fun vadReceived() {
        setText("Vad Received")
        feedbackEntity.durations.vad = System.currentTimeMillis();
    }

    private fun onResponse(query: ApiResponse) {
        feedbackEntity.durations.complete = System.currentTimeMillis();
        executor.submit({ sendFeedback() })
        context = (query as AudioStreamResponse).context
        runOnUiThread {
            setText("Query Complete")
            responseText.text = gson.toJson(query, AudioStreamResponse::class.java)
        }
    }

    private fun sendFeedback() {
        try {
            service.sendFeedback(feedbackEntity)
            Log.d("MainActivity", "feedback sent")
        } catch (e: Exception) {
            Log.e("MainActivity", e.message)
        }
    }

    private fun setText(text: String) {
        runOnUiThread {
            eventText.text = text
        }
    }

    private fun acceptAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 123)
            } else {
                init()
            }
        }
    }

}
