package com.voysis.android;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.voysis.api.Config;
import com.voysis.api.Service;
import com.voysis.api.ServiceProvider;
import com.voysis.api.ServiceType;
import com.voysis.events.Callback;
import com.voysis.events.FinishedReason;
import com.voysis.events.VoysisException;
import com.voysis.events.WakeWordState;
import com.voysis.model.response.QueryResponse;
import com.voysis.model.response.StreamResponse;
import com.voysis.sevice.DataConfig;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivityJava extends AppCompatActivity implements Callback {

    private static final String TAG = MainActivityJava.class.getSimpleName();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private TextView responseText;
    private TextView eventText;
    private Service service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        responseText = findViewById(R.id.responseText);
        eventText = findViewById(R.id.eventText);

        try {
            Config config = new DataConfig(true, new URL("INSERT_URL"), "INSERT_TOKEN", "INSERT_USERID", null, null, ServiceType.DEFAULT, "");
            ServiceProvider serviceprovider = new ServiceProvider();
            service = serviceprovider.makeCloud(this, config);
        } catch (MalformedURLException e) {
            Log.e(TAG, "MalformedURLException", e);
        }
        setupButtons();
    }

    @Override
    public void success(@NotNull final StreamResponse response) {
        Log.d(TAG, "success:");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setText("Query Complete");
                responseText.setText(gson.toJson(response, StreamResponse.class));
            }
        });
    }

    @Override
    public void failure(@NotNull VoysisException error) {
        Log.d(TAG, "failure: ");
        setText(error.getMessage());
    }

    @Override
    public void recordingStarted() {
        Log.d(TAG, "recordingStarted: ");
        setText("recordingStarted");
    }

    @Override
    public void queryResponse(@NotNull QueryResponse query) {
        Log.d(TAG, "queryResponse: ");
    }

    @Override
    public void recordingFinished(@NotNull FinishedReason reason) {
        Log.d(TAG, "recordingFinished: ");
        setText(reason.name());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ((grantResults != null) && (grantResults[0] == PackageManager.PERMISSION_DENIED)) {
            Log.e(TAG, "onRequestPermissionsResult: ", new Throwable("permission not granted"));
        } else {
            startAudioQuery();
        }
    }

    private void setupButtons() {
        Button start = findViewById(R.id.start);
        Button stop = findViewById(R.id.stop);
        Button cancel = findViewById(R.id.cancel);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPermissionAndStartQuery();
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                service.finish();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                service.cancel();
            }
        });
    }

    private void checkPermissionAndStartQuery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestRecordPermission();
        } else {
            startAudioQuery();
        }
    }

    private void startAudioQuery() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    service.startAudioQuery(MainActivityJava.this, null, null);
                } catch (IOException e) {
                    Log.e(TAG, "checkPermissionAndStartQuery: ", e);
                }
            }
        });
    }

    private void setText(final CharSequence text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                eventText.setText(text);
            }
        });
    }

    private void requestRecordPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] { android.Manifest.permission.RECORD_AUDIO }, 123);
            }
        }
    }

    @Override
    public void wakeword(@NotNull WakeWordState state) {
        setText(state.name());
    }
}
