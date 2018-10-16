package com.voysis.android

import android.Manifest
import android.annotation.TargetApi
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
import com.voysis.events.FinishedReason
import com.voysis.events.VoysisException
import com.voysis.model.request.FeedbackData
import com.voysis.model.response.StreamResponse
import com.voysis.sevice.DataConfig
import kotlinx.android.synthetic.main.activity_main.cancel
import kotlinx.android.synthetic.main.activity_main.eventText
import kotlinx.android.synthetic.main.activity_main.responseText
import kotlinx.android.synthetic.main.activity_main.send
import kotlinx.android.synthetic.main.activity_main.start
import kotlinx.android.synthetic.main.activity_main.stop
import kotlinx.android.synthetic.main.activity_main.textInput
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), Callback {

    private val url = "INSERT_URL"
    private val config = DataConfig(isVadEnabled = true, url = URL(url), refreshToken = "INSERT_TOKEN", userId = "")
    private lateinit var service: Service
    private val executor = Executors.newSingleThreadExecutor()
    private var context: Map<String, Any>? = null
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var feedbackData = FeedbackData()
    private var startTime: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //audio permissions must be accepted before using the Voysis Service.
        acceptAudioPermissionIfNeeded()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            val alertDialog = AlertDialog.Builder(this, R.style.Theme_AppCompat_Dialog_Alert)
                    .setMessage("must accept permission")
                    .setTitle("Alert")
                    .setCancelable(false)
                    .create()
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK") { dialog, _ ->
                dialog.dismiss()
                this@MainActivity.acceptAudioPermissionIfNeeded()
            }
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
        send.setOnClickListener { onSendClicked() }
        cancel.setOnClickListener { service.cancel() }
    }

    private fun onSendClicked() {
        val text = textInput.text.toString()
        executor.submit { service.sendTextQuery(context, text, this) }
    }

    private fun onStartClicked() {
        if (service.state == State.IDLE) {
            executor.submit { startAudioQuery() }
        } else {
            Toast.makeText(this, "query in progress ", LENGTH_SHORT).show()
        }
    }

    private fun startAudioQuery() {
        service.startAudioQuery(context = context, callback = this)
    }

    override fun success(response: StreamResponse) {
        submitFeedback(response)
        context = response.context
        runOnUiThread {
            setText("Query Complete")
            responseText.text = gson.toJson(response, StreamResponse::class.java)
        }
    }

    private fun submitFeedback(response: StreamResponse) {
        if (startTime != null) {
            feedbackData.durations.complete = System.currentTimeMillis() - startTime!!
            val queryId = response.id
            executor.submit { sendFeedback(queryId) }
        }
    }

    override fun failure(error: VoysisException) {
        setText(error.message.toString())
    }

    override fun recordingStarted() {
        startTime = System.currentTimeMillis()
        setText("Recording Started")
    }

    override fun recordingFinished(reason: FinishedReason) {
        if (reason == FinishedReason.VAD_RECEIVED) {
            setText("Vad Received")
            feedbackData.durations.vad = System.currentTimeMillis() - startTime!!
        } else if (reason == FinishedReason.MANUAL_STOP) {
            setText("Recording Finished")
        }
    }

    private fun sendFeedback(queryId: String) {
        try {
            service.sendFeedback(queryId, feedbackData)
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

    private fun acceptAudioPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAudioPermission()
        } else {
            init()
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun checkAudioPermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 123)
        } else {
            init()
        }
    }

}
