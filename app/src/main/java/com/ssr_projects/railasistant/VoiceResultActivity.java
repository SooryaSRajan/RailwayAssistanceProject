package com.ssr_projects.railasistant;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceResultActivity extends AppCompatActivity {
    private SpeechRecognizer speechRecognizer = null;
    private Intent speechRecognizerIntent;
    private String TAG = "VoiceResultActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_result);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        getSupportActionBar().hide();
        getWindow().setStatusBarColor(Color.rgb(241, 241, 241));

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                Log.e(TAG, "onReadyForSpeech: " );
                adjustAudio(true);
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.e(TAG, "onBeginningOfSpeech: " );
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {
                Log.e(TAG, "onEndOfSpeech: ");
            }

            @Override
            public void onError(int i) {
                Log.e(TAG, "onError: " + i);
                Intent intent = new Intent();
                setResult(Activity.RESULT_CANCELED, intent);
                finish();
            }

            @Override
            public void onResults(Bundle bundle) {
                adjustAudio(false);
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                Toast.makeText(VoiceResultActivity.this, data.get(0), Toast.LENGTH_SHORT).show();
                String answer = data.get(0);

                Intent intent = new Intent();
                Intent i = getIntent();
                intent.putExtra("KEY", answer);
                intent.putExtra("QUERY", i.getStringExtra("QUERY"));
                setResult(Activity.RESULT_OK, intent);
                finish();

                Log.e(TAG, "onResults: " + answer);
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {
            }
        });

        speechRecognizer.startListening(speechRecognizerIntent);

    }

    public void adjustAudio(boolean setMute) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int adJustMute;
            if (setMute) {
                adJustMute = AudioManager.ADJUST_MUTE;
            } else {
                adJustMute = AudioManager.ADJUST_UNMUTE;
            }
            audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, adJustMute, 0);
            audioManager.adjustStreamVolume(AudioManager.STREAM_ALARM, adJustMute, 0);
            audioManager.adjustStreamVolume(AudioManager.STREAM_RING, adJustMute, 0);
        } else {
            audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, setMute);
            audioManager.setStreamMute(AudioManager.STREAM_ALARM, setMute);
            audioManager.setStreamMute(AudioManager.STREAM_RING, setMute);
        }
    }
}