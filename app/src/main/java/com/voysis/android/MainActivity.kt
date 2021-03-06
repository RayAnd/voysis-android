package com.voysis.android

import android.Manifest
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.voysis.api.Service
import com.voysis.api.ServiceProvider
import com.voysis.api.State
import com.voysis.events.Callback
import com.voysis.events.FinishedReason
import com.voysis.events.VoysisException
import com.voysis.model.response.StreamResponse
import com.voysis.sevice.DataConfig
import kotlinx.android.synthetic.main.activity_main_wakeword.cancel
import kotlinx.android.synthetic.main.activity_main_wakeword.eventText
import kotlinx.android.synthetic.main.activity_main_wakeword.responseText
import kotlinx.android.synthetic.main.activity_main_wakeword.send
import kotlinx.android.synthetic.main.activity_main_wakeword.start
import kotlinx.android.synthetic.main.activity_main_wakeword.stop
import kotlinx.android.synthetic.main.activity_main_wakeword.textInput
import java.net.URL
import java.util.concurrent.Executors

/*
 * This activity describes the instantiation and usage of a default Voysis instance:
 * Can be used to make single queries against url endpoint or embedded service.
 */

class MainActivity : AppCompatActivity(), Callback {

    private val config = DataConfig(isVadEnabled = true, url = URL("INSERT_URL"), refreshToken = "INSERT_TOKEN", userId = "")
    private val executor = Executors.newSingleThreadExecutor()
    private var context: Map<String, Any>? = null
    private lateinit var service: Service

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        acceptAudioPermissionIfNeeded()
    }

    override fun recordingStarted() {
        setEventText("Recording Started")
    }

    override fun recordingFinished(reason: FinishedReason) {
        if (reason == FinishedReason.VAD_RECEIVED) {
            setEventText("Vad Received")
        } else if (reason == FinishedReason.MANUAL_STOP) {
            setEventText("Recording Finished")
        }
    }

    override fun success(response: StreamResponse) {
        context = response.context
        setEventText("Query Complete")
        setResponseText(response)
    }

    override fun failure(error: VoysisException) {
        setEventText(error.message.toString())
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            init()
        } else {
            //handle permission not accepted
        }
    }

    override fun onStop() {
        super.onStop()
        service.cancel()
    }

    private fun init() {
        /*
         * `makeCloud()` denotes service creation as a network service. Requires a URL endpoint and
         * refresh token. See documentation for more information
         */
        service = ServiceProvider().makeCloud(applicationContext, config)
        setupClickListeners()
        /*
         *`makeLocal()` denotes service creation as a an embedded service. Requires a model files in assets folder.
         * See documentation for more information
         */
        //service = ServiceProvider().makeLocal(applicationContext, config)
    }

    private fun setupClickListeners() {
        start.setOnClickListener { onStartClicked() }
        stop.setOnClickListener { service.finish() }
        send.setOnClickListener { onSendClicked() }
        cancel.setOnClickListener { service.cancel() }
    }

    private fun onSendClicked() {
        val text = textInput.text.toString()
        executor.submit { service.sendTextQuery(text, this, context) }
    }

    private fun onStartClicked() {
        if (service.state == State.IDLE) {
            executor.submit { startAudioQuery() }
        } else {
            Toast.makeText(this, "query in progress ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startAudioQuery() {
        service.startAudioQuery(context = context, callback = this)
    }

    private fun setEventText(text: String) {
        runOnUiThread {
            eventText.text = text
        }
    }

    private fun setResponseText(response: StreamResponse) {
        runOnUiThread {
            val text = GsonBuilder().setPrettyPrinting().create().toJson(response, StreamResponse::class.java)
            responseText.text = text
        }
    }

    private fun acceptAudioPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkAudioPermission()) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 123)
            return
        }
        init()
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun checkAudioPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

}
