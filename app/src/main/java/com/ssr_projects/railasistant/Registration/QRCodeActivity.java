package com.ssr_projects.railasistant.Registration;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ssr_projects.railasistant.MainActivity;
import com.ssr_projects.railasistant.R;

import java.util.ArrayList;
import java.util.Locale;

public class QRCodeActivity extends AppCompatActivity {
    private TextView mTimer;
    private static final int TIME = 1000;
    int countDown = 60;
    private SpeechRecognizer speechRecognizer = null;
    private Intent speechRecognizerIntent;
    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_q_r_code);

        getSupportActionBar().hide();
        getWindow().setStatusBarColor(Color.rgb(0, 0, 0));
        mTimer = findViewById(R.id.qr_timer);

        final Handler mHandler = new Handler(Looper.getMainLooper());
        handler = new Handler();
         runnable = new Runnable() {
            public void run() {
                if (countDown != 0) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mTimer.setText(countDown + "");
                        }
                    });
                    handler.postDelayed(this, TIME);
                }
                else{
                    speechRecognizer.cancel();
                    speechRecognizer.destroy();
                    handler.removeCallbacks(runnable);
                    Toast.makeText(QRCodeActivity.this, "Timed Out", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(QRCodeActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
                countDown--;
            }
        };

        handler.postDelayed(runnable, TIME);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.startListening(speechRecognizerIntent);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {
                speechRecognizer.startListening(speechRecognizerIntent);
            }

            @Override
            public void onResults(Bundle bundle) {
                speechRecognizer.stopListening();
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String command = data.get(0);
                command = command.toLowerCase();

                if(command.contains("cancel")){
                    speechRecognizer.cancel();
                    speechRecognizer.destroy();
                    handler.removeCallbacks(runnable);
                    Toast.makeText(QRCodeActivity.this, "Cancelling Process", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(QRCodeActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }

                else{
                    speechRecognizer.startListening(speechRecognizerIntent);
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {
                speechRecognizer.startListening(speechRecognizerIntent);
            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speechRecognizer.stopListening();
        speechRecognizer.cancel();
        speechRecognizer.destroy();

    }
}