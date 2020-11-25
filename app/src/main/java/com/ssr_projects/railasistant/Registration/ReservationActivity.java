package com.ssr_projects.railasistant.Registration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ssr_projects.railasistant.ConversationAdapter;
import com.ssr_projects.railasistant.MainActivity;
import com.ssr_projects.railasistant.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarEntry;

import pl.droidsonroids.gif.GifImageView;

public class ReservationActivity extends AppCompatActivity {
    private SpeechRecognizer speechRecognizer = null;
    private Intent speechRecognizerIntent;
    private TextToSpeech textToSpeech;
    private Handler handler;
    private Runnable runnable;
    int countDown = 60;
    private final int TIME = 1000;
    private TextView timerTextView, titleTextView;
    private GifImageView gifImageView;
    private ListView listView;
    private ArrayList<HashMap> mapArrayList = new ArrayList<>();
    private int state = -1;
    private final int STATION_STATE_0 = 0;
    private final int TRAINS_TO_STATE_1 = 12;
    private final int ORDINARY_STATE = 1122;
    private final int EXPRESS_STATE = 1432;
    private final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("TRAIN TABLE");
    private final String TAG = "Reservation Activity";
    private HashMap map;
    private ConversationAdapter adapter;
    private boolean isSpeakingFlag = false;
    private String informationToDisplay = "";
    private ArrayList<HashMap<String, DataSnapshot>> trainInformation = new ArrayList<>();
    private int optionPosition = 0;
    private String trainType;
    private final String TYPE_ORDINARY = "ORDINARY";
    private final String TYPE_EXPRESS = "EXPRESS";
    private String stationSelected;
    private int noOfSeats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation);

        getSupportActionBar().hide();
        getWindow().setStatusBarColor(Color.rgb(0, 0, 0));

        timerTextView = findViewById(R.id.reservation_state_timer);
        titleTextView = findViewById(R.id.reservation_state);
        listView = findViewById(R.id.list_view_reservation);
        gifImageView = findViewById(R.id.gif_view_reservation);

        adapter = new ConversationAdapter(mapArrayList, ReservationActivity.this);
        listView.setAdapter(adapter);


        titleTextView.setText("Reservation");

        final Handler handlerUI = new Handler(Looper.getMainLooper());
        handler = new Handler();
        runnable = new Runnable() {
            public void run() {
                countDown--;

                handlerUI.post(new Runnable() {
                    @Override
                    public void run() {
                        timerTextView.setText("" + countDown);
                    }
                });

                if(countDown == 0){
                    Toast.makeText(ReservationActivity.this, "Timed out", Toast.LENGTH_SHORT).show();
                    speechRecognizer.stopListening();
                    speechRecognizer.cancel();
                    speechRecognizer.destroy();
                    FirebaseAuth.getInstance().signOut();
                    handler.removeCallbacks(this);
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                }
                handler.postDelayed(this, TIME);
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
                    Log.e(TAG, "onInit: Success" );
                    state = STATION_STATE_0;
                    textToSpeech.speak("Which station do you wanna go to?", TextToSpeech.QUEUE_ADD, null, "state_1");
                    map = new HashMap();
                    map.put("POSITION", "LEFT");
                    map.put("TEXT", "Which station do you wanna go to?" + informationToDisplay);
                    mapArrayList.add(map);
                    adapter.notifyDataSetChanged();

                }
                else{
                    Log.e(TAG, "onInit: Error" );
                }
            }
        });

        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
                isSpeakingFlag = true;
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognizer.stopListening();
                    }
                });
            }

            @Override
            public void onDone(String s) {
                isSpeakingFlag = false;

                if(s.contains("state_1")){
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            speechRecognizer.startListening(speechRecognizerIntent);
                        }
                    });
                }

                else if(s.contains("train_available")){
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            speechRecognizer.startListening(speechRecognizerIntent);
                        }
                    });
                }

                else if(s.contains("cancel_seat")){
                    Log.e(TAG, "onDone: cancel" );
                    handler.removeCallbacks(runnable);
                    Handler mHandler = new Handler(Looper.getMainLooper());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            speechRecognizer.cancel();
                            speechRecognizer.destroy();
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });

                }

                else if(s.contains("train_selected")){
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(trainType.contains(TYPE_EXPRESS)){
                                textToSpeech.speak("This is an express train, what category do you want?", TextToSpeech.QUEUE_ADD, null, "state_1");
                                map = new HashMap();
                                map.put("POSITION", "LEFT");
                                map.put("TEXT", "This is an express train, what category do you want? 3AC, 2AC, 1AC, CC, FC, SC, ST");
                                mapArrayList.add(map);
                                adapter.notifyDataSetChanged();
                                state = EXPRESS_STATE;
                            }
                            else if(trainType.contains(TYPE_ORDINARY)){
                                textToSpeech.speak("This is an ordinary train, how many seats do you want?", TextToSpeech.QUEUE_ADD, null, "state_1");
                                map = new HashMap();
                                map.put("POSITION", "LEFT");
                                map.put("TEXT", "This is an ordinary train, how many seats do you want?");
                                mapArrayList.add(map);
                                adapter.notifyDataSetChanged();
                                state = ORDINARY_STATE;
                            }
                        }
                    });
                }

            }

            @Override
            public void onError(String s) {

            }
        });

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float v) {
                Log.e(TAG, "onRmsChanged: "  );
                if(v > 6){
                    gifImageView.setVisibility(View.VISIBLE);
                }
                else{
                    gifImageView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {
                gifImageView.setVisibility(View.GONE);
            }

            @Override
            public void onError(int i) {
                gifImageView.setVisibility(View.GONE);
                if(!isSpeakingFlag)
                speechRecognizer.startListening(speechRecognizerIntent);
                Log.e(TAG, "onError: "  );
            }

            @Override
            public void onResults(Bundle bundle) {
                gifImageView.setVisibility(View.GONE);
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String answer = data.get(0);
                answer = answer.toLowerCase();
                Toast.makeText(ReservationActivity.this, answer, Toast.LENGTH_SHORT).show();
                speechRecognizer.stopListening();

                map = new HashMap();
                map.put("TEXT", answer);
                map.put("POSITION", "RIGHT");
                mapArrayList.add(map);
                adapter.notifyDataSetChanged();

                if(answer.contains("cancel")){
                    textToSpeech.speak("Cancelling Process", TextToSpeech.QUEUE_ADD, null, "hello");
                    map = new HashMap();
                    map.put("POSITION", "LEFT");
                    map.put("TEXT", "Cancelling Process");
                    mapArrayList.add(map);
                    adapter.notifyDataSetChanged();
                    speechRecognizer.cancel();
                    speechRecognizer.destroy();
                    FirebaseAuth.getInstance().signOut();
                    handler.removeCallbacks(runnable);
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                }

                else{
                    switch(state){
                        case STATION_STATE_0:
                            trainInformation.clear();
                            final Query query = databaseReference.child("STATIONS").orderByChild("STATION NAME").equalTo(answer.toUpperCase());
                            final String finalAnswer = answer;
                            query.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    int isAvailableflag = 0;
                                    for(final DataSnapshot snap : snapshot.getChildren()){
                                        isAvailableflag++;
                                        final String stationCode = snap.child("STATION CODE").getValue().toString();
                                        informationToDisplay = "";
                                        databaseReference.child("ARRIVAL").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                countDown = 60;
                                                int count = 0;
                                                stationSelected = finalAnswer.trim();
                                                textToSpeech.speak("The following trains are available, which train do you want?", TextToSpeech.QUEUE_ADD, null, "train_available");
                                                map = new HashMap();
                                                map.put("POSITION", "LEFT");
                                                map.put("TEXT", "The following trains are available, which train do you want? (state train number)" + informationToDisplay);
                                                mapArrayList.add(map);
                                                adapter.notifyDataSetChanged();

                                                for(final DataSnapshot snapSuper : snapshot.getChildren()){
                                                    if(snapSuper.child("STATION CODE").getValue().toString().contains(stationCode)) {
                                                        count++;
                                                        Log.e(TAG, "onDataChange: " + snapSuper);
                                                        informationToDisplay += count + ". \n";
                                                        informationToDisplay += "Arrival time: ";
                                                        informationToDisplay += snapSuper.child("ARRIVAL").getValue().toString();
                                                        databaseReference.child("TRAINS").addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot snap : snapshot.getChildren()) {
                                                                    if (snap.child("TRAIN NO").getValue().toString().contains(snapSuper.child("TRAIN NO").getValue().toString())){
                                                                        HashMap<String, DataSnapshot> trainMap = new HashMap();
                                                                        trainMap.put("ARRIVAL", snapSuper);
                                                                        trainMap.put("TRAIN", snap);
                                                                        trainInformation.add(trainMap);
                                                                        Log.e(TAG, "onDataChange: trainssssss" + snap );
                                                                        informationToDisplay += "\nTrain Name: ";
                                                                        informationToDisplay += snap.child("TRAIN NAME").getValue().toString();
                                                                        informationToDisplay += "\nTrain Number: ";
                                                                        informationToDisplay += snap.child("TRAIN NO").getValue().toString();
                                                                        Log.e(TAG, "onDataChange: Information" + informationToDisplay );
                                                                        map = new HashMap();
                                                                        map.put("POSITION", "LEFT");
                                                                        map.put("TEXT", informationToDisplay);
                                                                        mapArrayList.add(map);
                                                                        adapter.notifyDataSetChanged();
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {

                                                            }
                                                        });
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });
                                    }
                                    if(isAvailableflag == 0){
                                        textToSpeech.speak("No stations were found, please try again or say cancel to quit", TextToSpeech.QUEUE_ADD, null, "state_1");
                                        map = new HashMap();
                                        map.put("POSITION", "LEFT");
                                        map.put("TEXT", "No stations were found, please try again or say cancel to quit");
                                        mapArrayList.add(map);
                                        adapter.notifyDataSetChanged();
                                    }
                                    else{
                                        state = TRAINS_TO_STATE_1;
                                    }

                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                            break;

                        case TRAINS_TO_STATE_1:
                            answer = answer.replaceAll("\\s", "");
                            countDown = 60;
                            int check = 0;
                            int position = -1;
                                for(HashMap<String, DataSnapshot> trainListMap : trainInformation){
                                    DataSnapshot snapshot = trainListMap.get("TRAIN");
                                    DataSnapshot arrivalSnapshot = trainListMap.get("ARRIVAL");
                                    position++;
                                    if(snapshot.child("TRAIN NO").getValue().toString().equals(answer)){
                                        optionPosition = position;
                                        check++;
                                        String trainDetails = "TRAIN No: " + snapshot.child("TRAIN NO").getValue().toString();
                                        trainDetails += "\n" + "TRAIN NAME: " + snapshot.child("TRAIN NAME").getValue().toString();
                                        trainDetails += "\n" + "ARRIVAL TIME: " + arrivalSnapshot.child("ARRIVAL").getValue().toString();
                                        if(snapshot.child("TYPE").getValue().toString().contains(TYPE_ORDINARY))
                                            trainType = TYPE_ORDINARY;

                                        else if(snapshot.child("TYPE").getValue().toString().contains(TYPE_EXPRESS))
                                            trainType = TYPE_EXPRESS;

                                        textToSpeech.speak("Your option was: " + answer, TextToSpeech.QUEUE_ADD, null, "train_selected");
                                        map = new HashMap();
                                        map.put("POSITION", "LEFT");
                                        map.put("TEXT", "Your option was: " + answer + "\n" + trainDetails);
                                        mapArrayList.add(map);
                                        adapter.notifyDataSetChanged();
                                        break;
                                    }
                                }
                                if(check == 0){
                                    textToSpeech.speak("Invalid option, try again or say cancel to exit", TextToSpeech.QUEUE_ADD, null, "state_1");
                                    map = new HashMap();
                                    map.put("POSITION", "LEFT");
                                    map.put("TEXT", "Invalid option, try again or say cancel to exit");
                                    mapArrayList.add(map);
                                    adapter.notifyDataSetChanged();
                                }
                            break;

                        case ORDINARY_STATE:
                            try {
                                final int noOfSeatsAnswer = Integer.parseInt(answer);
                                DataSnapshot snapshot = trainInformation.get(optionPosition).get("TRAIN");
                                databaseReference.child("TRAINS").child(snapshot.getKey()).child("A_SC").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        int availableSeats = Integer.parseInt(snapshot.getValue().toString());
                                        if (availableSeats > noOfSeatsAnswer){
                                         noOfSeats = noOfSeatsAnswer;
                                            textToSpeech.speak("Seats are available in this train", TextToSpeech.QUEUE_ADD, null, "state_1");
                                            map = new HashMap();
                                            map.put("POSITION", "LEFT");
                                            map.put("TEXT", "Seats are available in this train");
                                            mapArrayList.add(map);
                                            adapter.notifyDataSetChanged();
                                        }
                                        else{
                                            textToSpeech.speak("Sorry, seats are not available in this train, cancelling process", TextToSpeech.QUEUE_ADD, null, "cancel_seat");
                                            map = new HashMap();
                                            map.put("POSITION", "LEFT");
                                            map.put("TEXT", "Sorry, seats are not available in this train");
                                            mapArrayList.add(map);
                                            adapter.notifyDataSetChanged();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            }
                            catch (Exception e){

                            }
                            break;
                            case  EXPRESS_STATE:

                            break;


                    }
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {
                gifImageView.setVisibility(View.GONE);
            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

    }
}