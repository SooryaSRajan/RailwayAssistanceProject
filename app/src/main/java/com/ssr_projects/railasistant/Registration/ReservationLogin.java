package com.ssr_projects.railasistant.Registration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.ssr_projects.railasistant.MainActivity;
import com.ssr_projects.railasistant.R;

import java.util.ArrayList;
import java.util.Locale;

public class ReservationLogin extends AppCompatActivity {
    private static final int TIME = 1000;
    private EditText passwordForm, userNameForm;
    private String passwordString, userNameString;
    private Button signInButton;
    private TextView mTimer;
    private ProgressBar progressBar;
    int countDown = 60;
    private SpeechRecognizer speechRecognizer = null;
    private Intent speechRecognizerIntent;
    private Handler handler;
    private Runnable runnable;
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation_login);

        getSupportActionBar().hide();
        getWindow().setStatusBarColor(Color.rgb(0, 0, 0));

        passwordForm = findViewById(R.id.reg_password);
        userNameForm = findViewById(R.id.reg_user_name);
        signInButton = findViewById(R.id.sign_in_button);
        progressBar = findViewById(R.id.sign_in_progress);
        mTimer = findViewById(R.id.reg_timer);

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
                    Toast.makeText(ReservationLogin.this, "Timed Out", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ReservationLogin.this, MainActivity.class);
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

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.US);
                }
            }
        });

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
                    Toast.makeText(ReservationLogin.this, "Cancelling Process", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ReservationLogin.this, MainActivity.class);
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

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                view.setVisibility(View.INVISIBLE);
                int flag = 0;
                userNameString = userNameForm.getText().toString().trim();
                passwordString = passwordForm.getText().toString();

                if(userNameString.isEmpty()){
                    userNameForm.setError("Empty");
                    flag++;
                    view.setVisibility(View.VISIBLE);
                }

                if(passwordString.isEmpty()){
                    passwordForm.setError("Empty");
                    flag++;
                    view.setVisibility(View.VISIBLE);
                }

                if(flag == 0){
                    userNameString = userNameString.trim() + "@gmail.com";
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(userNameString, passwordString).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){
                                speechRecognizer.cancel();
                                speechRecognizer.destroy();
                                handler.removeCallbacks(runnable);
                                Intent intent = new Intent(ReservationLogin.this, ReservationActivity.class);
                                startActivity(intent);
                                finish();
                            }
                            else{
                                view.setVisibility(View.INVISIBLE);
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("Login", "onFailure: " + e.toString() );
                            if(e.toString().contains("There is no user record corresponding to this identifier")){
                                speechRecognizer.stopListening();
                                textToSpeech.speak("Sorry, this account was not found, please try again", TextToSpeech.QUEUE_ADD, null, "not_found");
                            }
                        }
                    });
                }

            }
        });

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }
}