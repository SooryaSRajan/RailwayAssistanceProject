package com.ssr_projects.railasistant;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;

import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity {

    private String mCommand;
    private ImageButton commandButton;
    private EditText commandBox;
    private CountDownTimer countDownTimer;
    private SpeechRecognizer speechRecognizer = null;
    private Intent speechRecognizerIntent;
    private Handler handler = new Handler();
    private boolean isUserVoiceRecognized = false, setViewFlag = false, setForceOverrideFlag = false,
        enablePopUpMenu = false;
    private TextToSpeech textToSpeech;
    private String TAG = "MainActivity";
    private String keyWord = null, intentKey;
    private Runnable runnable;
    private final int TIME = 1000;
    private LocationManager mLocationManager;
    private ListView listView;
    private ArrayList<HashMap> mArrayList = new ArrayList<>();
    private ConversationAdapter adapter;
    private HashMap map;
    private TextView helloTextView;
    private GifImageView gifImageView, voiceGifView;
    private int intCount = 0, twoMinuteCounter = 0, rollBackCounter = 0;
    private DatabaseReference mRef = FirebaseDatabase.getInstance().getReference();
    private String commandsUsed = "List of commands that can be used are: " +
            "\n->Hello Assistant " +
            "\n->How many seats are available in *train name* " +
            "\n->Are there trains to *city* " +
            "\n->When will train *train number* arrive at *station code*" +
            "\n->What is the Time" +
            "\n->What is the Date";
    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        helloTextView = findViewById(R.id.hello_text);
        gifImageView = findViewById(R.id.ai_gif);
        voiceGifView = findViewById(R.id.voice_recognizer_animation);
        voiceGifView.setVisibility(View.GONE);

        commandBox = findViewById(R.id.command_box);
        commandBox.setEnabled(false);
        commandBox.setVisibility(View.GONE);

        commandButton = findViewById(R.id.command_button);
        commandButton.setVisibility(View.GONE);

        listView = findViewById(R.id.list_view);
        adapter = new ConversationAdapter(mArrayList, MainActivity.this);
        listView.setAdapter(adapter);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, long l) {
                if(enablePopUpMenu) {
                    PopupMenu popupMenu = new PopupMenu(MainActivity.this, view);
                    popupMenu.getMenuInflater().inflate(R.menu.menu, popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            if (menuItem.getItemId() == R.id.remove) {
                                mArrayList.remove(i);
                                adapter.notifyDataSetChanged();
                            } else if (menuItem.getItemId() == R.id.copy) {
                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getApplication().getSystemService(Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text 2", mArrayList.get(i).get("TEXT").toString());
                                clipboard.setPrimaryClip(clip);
                            }
                            return true;
                        }
                    });
                    popupMenu.show();
                }
                return true;
            }
        });

        getSupportActionBar().hide();
        getWindow().setStatusBarColor(Color.rgb(0, 0, 0));

        countDownTimer = new CountDownTimer(120000, 1000) { //40000 milli seconds is total time, 1000 milli seconds is time interval

            public void onTick(long millisUntilFinished) {
                Log.e(TAG, "onTick: " );
            }
            public void onFinish() {
                commandBox.setText("");
                setForceOverrideFlag = false;
                commandBox.setEnabled(false);
                commandBox.setVisibility(View.GONE);
                commandButton.setVisibility(View.GONE);

            }
        };

        if(checkIfPermissionGranted()){

            textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if(status != TextToSpeech.ERROR) {
                        textToSpeech.setLanguage(Locale.US);
                    }
                }
            });

            runnable = new Runnable() {
                public void run() {
                    intCount++;
                    twoMinuteCounter++;
                    if(!isUserVoiceRecognized){
                        speechRecognizer.startListening(speechRecognizerIntent);
                        Log.e(TAG, "run: Start listening called");
                    }
                    Log.e(TAG, "run: " + setViewFlag );
                    if(intCount == 10 && !setViewFlag){
                        intCount = 0;
                        twoMinuteCounter = 0;
                        mArrayList.clear();
                        adapter.notifyDataSetChanged();
                        helloTextView.setVisibility(View.VISIBLE);
                        gifImageView.setVisibility(View.VISIBLE);
                    }
                    else if(intCount == 10){
                        intCount = 0;
                    }
                    Log.e(TAG, "run: " + twoMinuteCounter );
                    if(twoMinuteCounter == 140){
                        mArrayList.clear();
                        adapter.notifyDataSetChanged();
                        if(!isUserVoiceRecognized){
                            helloTextView.setVisibility(View.VISIBLE);
                            gifImageView.setVisibility(View.VISIBLE);
                            voiceGifView.setVisibility(View.GONE);
                        }
                    }

                    if(twoMinuteCounter == 155 && mArrayList.isEmpty()){
                        helloTextView.setVisibility(View.VISIBLE);
                        gifImageView.setVisibility(View.VISIBLE);
                        twoMinuteCounter = 0;
                        voiceGifView.setVisibility(View.GONE);
                    }
                    else if(twoMinuteCounter == 150){
                        twoMinuteCounter = 0;
                    }

                    Log.e(TAG, "run: Thread");
                    handler.postDelayed(this, TIME);
                }
            };

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

            speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

            commandButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mCommand = commandBox.getText().toString();
                    if(!mCommand.isEmpty() && commandBox.isEnabled()){
                        handler.removeCallbacks(runnable);
                        isUserVoiceRecognized = true;
                        speechRecognizer.stopListening();
                        voiceGifView.setVisibility(View.GONE);
                        helloTextView.setVisibility(View.GONE);
                        gifImageView.setVisibility(View.GONE);
                        setViewFlag = true;
                        commandBox.setText("");
                        map = new HashMap();
                        map.put("TEXT", mCommand);
                        map.put("POSITION", "RIGHT");
                        mArrayList.add(map);
                        adapter.notifyDataSetChanged();

                        inputTextFunction(mCommand);

                    }
                }
            });


            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String s) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            isUserVoiceRecognized = true;
                            handler.removeCallbacks(runnable);
                            speechRecognizer.stopListening();
                            commandBox.setEnabled(false);
                            Log.e(TAG, "run: Uttering" );
                            commandButton.setEnabled(false);
                            Log.e(TAG, "run: " +  commandButton.isEnabled());
                        }
                    });
                }

                @Override
                public void onDone(String s) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            commandButton.setEnabled(true);
                            commandBox.setEnabled(true);
                        }
                    });
                    isUserVoiceRecognized = false;
                    if(s.contains("details")){
                        Log.e(TAG, "onDone: " + s );
                        Intent i = new Intent(MainActivity.this, VoiceResultActivity.class);
                        i.putExtra("QUERY", "details");
                        startActivityForResult(i, 1000);
                    }

                    else if(s.contains("override_code_menu")){
                        Log.e(TAG, "onDone: " + s );
                        Intent i = new Intent(MainActivity.this, PassCodeActivity.class);
                        startActivityForResult(i, 5010);
                    }

                    else if(s.contains("override_disable_menu")){
                        Log.e(TAG, "onDone: " + s );
                        Intent i = new Intent(MainActivity.this, PassCodeActivity.class);
                        startActivityForResult(i, 6010);
                    }

                    else if(s.contains("override_success")){
                        restartRecognizer();
                        setViewFlag = false;
                    }

                    else if(s.contains("override_code_disable")){
                        Log.e(TAG, "onDone: " + s );
                        Intent i = new Intent(MainActivity.this, PassCodeActivity.class);
                        startActivityForResult(i, 1812);
                    }

                    else if(s.contains("train category")){
                        Log.e(TAG, "onDone: " + s );
                        Intent i = new Intent(MainActivity.this, VoiceResultActivity.class);
                        i.putExtra("QUERY", "train category");
                        startActivityForResult(i, 1000);
                    }

                    else if(s.contains("override_code")){
                        Log.e(TAG, "onDone: " + s );
                        Intent i = new Intent(MainActivity.this, PassCodeActivity.class);
                        startActivityForResult(i, 2707);
                    }

                    else if(s.contains("hello")){
                        restartRecognizer();
                        setViewFlag = false;
                    }

                    else if(s.contains("invalid")){
                        restartRecognizer();
                        setViewFlag = false;
                    }

                    else if(s.contains("received")){
                        restartRecognizer();
                        setViewFlag = false;
                    }

                    else if(s.contains("time/date")){
                        restartRecognizer();
                        setViewFlag = false;
                    }

                    Log.e(TAG, "onDone: " );
                }

                @Override
                public void onError(String s) {

                }
            });

            speechRecognizer.startListening(speechRecognizerIntent);

            restartRecognizer();
        }

        else{
            requestPermissionForRecorder();
        }

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                isUserVoiceRecognized = true;
                Log.e(TAG, "onReadyForSpeech: " );
            }

            @Override
            public void onBeginningOfSpeech() {
                setViewFlag = true;
                isUserVoiceRecognized = true;
            }

            @Override
            public void onRmsChanged(float v) {
                if(!(gifImageView.getVisibility() == View.VISIBLE) && v>6){
                    voiceGifView.setVisibility(View.VISIBLE);
                }

                if(v>6){
                    commandButton.setEnabled(false);
                    Log.e(TAG, "onRmsChanged: Disabled button" );
                }
            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {
                voiceGifView.setVisibility(View.GONE);
            }

            @Override
            public void onError(int i) {
                commandButton.setEnabled(true);
                voiceGifView.setVisibility(View.GONE);
                isUserVoiceRecognized = false;
                setViewFlag = false;
            }

            @Override
            public void onResults(Bundle bundle) {

                setViewFlag = true;
                voiceGifView.setVisibility(View.GONE);
                gifImageView.setVisibility(View.GONE);
                helloTextView.setVisibility(View.GONE);
                isUserVoiceRecognized = true;
                speechRecognizer.stopListening();
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                String answer = data.get(0);
                answer = answer.toLowerCase();

                map = new HashMap();
                map.put("TEXT", answer);
                map.put("POSITION", "RIGHT");
                mArrayList.add(map);
                adapter.notifyDataSetChanged();

                commandButton.setEnabled(false);
                inputTextFunction(answer);

            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {
                isUserVoiceRecognized = true;
            }
        });

    }

    private void inputTextFunction(String answer){
        commandButton.setEnabled(false);
        if(answer.contains("hello assistant")){

            handler.removeCallbacks(runnable);
            textToSpeech.speak("Hello, what do you want?", TextToSpeech.QUEUE_ADD, null, "hello");

            map = new HashMap();
            map.put("POSITION", "LEFT");
            map.put("TEXT", "Hello, what do you want?");
            mArrayList.add(map);
            adapter.notifyDataSetChanged();

        }

        else if(answer.contains("force override enable text")){
            if(!setForceOverrideFlag) {
                handler.removeCallbacks(runnable);
                textToSpeech.speak("Force overriding voice input. Enter pass code", TextToSpeech.QUEUE_ADD, null, "override_code");

                map = new HashMap();
                map.put("POSITION", "LEFT");
                map.put("TEXT", "Force overriding voice input. Enter pass code ");
                mArrayList.add(map);
                adapter.notifyDataSetChanged();
            }

            else{
                handler.removeCallbacks(runnable);
                textToSpeech.speak("Force override already enabled", TextToSpeech.QUEUE_ADD, null, "hello");

                map = new HashMap();
                map.put("POSITION", "LEFT");
                map.put("TEXT", "Force override already enabled");
                mArrayList.add(map);
                adapter.notifyDataSetChanged();
            }
        }

        else if(answer.contains("override disable text")){
            if(setForceOverrideFlag) {
                handler.removeCallbacks(runnable);
                textToSpeech.speak("Force overriding disable procedure. Enter pass code", TextToSpeech.QUEUE_ADD, null, "override_code_disable");

                map = new HashMap();
                map.put("POSITION", "LEFT");
                map.put("TEXT", "Force overriding disable procedure. Enter pass code ");
                mArrayList.add(map);
                adapter.notifyDataSetChanged();
            }

            else{
                handler.removeCallbacks(runnable);
                textToSpeech.speak("override already disabled", TextToSpeech.QUEUE_ADD, null, "hello");

                map = new HashMap();
                map.put("POSITION", "LEFT");
                map.put("TEXT", "Force override already disabled");
                mArrayList.add(map);
                adapter.notifyDataSetChanged();
            }
        }

        else if(answer.contains("override enable menu")){
            if(!enablePopUpMenu) {
                handler.removeCallbacks(runnable);
                textToSpeech.speak("Force overriding menu. Enter pass code", TextToSpeech.QUEUE_ADD, null, "override_code_menu");

                map = new HashMap();
                map.put("POSITION", "LEFT");
                map.put("TEXT", "Force overriding menu. Enter pass code ");
                mArrayList.add(map);
                adapter.notifyDataSetChanged();
            }

            else{
                handler.removeCallbacks(runnable);
                textToSpeech.speak("Force override menu already enabled", TextToSpeech.QUEUE_ADD, null, "hello");

                map = new HashMap();
                map.put("POSITION", "LEFT");
                map.put("TEXT", "Force override menu already enabled");
                mArrayList.add(map);
                adapter.notifyDataSetChanged();
            }
        }

        else if(answer.contains("override disable menu")){
            if(enablePopUpMenu) {
                handler.removeCallbacks(runnable);
                textToSpeech.speak("Force overriding menu. Enter pass code", TextToSpeech.QUEUE_ADD, null, "override_disable_menu");

                map = new HashMap();
                map.put("POSITION", "LEFT");
                map.put("TEXT", "Force overriding voice menu. Enter pass code ");
                mArrayList.add(map);
                adapter.notifyDataSetChanged();
            }

            else{
                handler.removeCallbacks(runnable);
                textToSpeech.speak("Force override menu already disabled", TextToSpeech.QUEUE_ADD, null, "hello");

                map = new HashMap();
                map.put("POSITION", "LEFT");
                map.put("TEXT", "Force override already disabled");
                mArrayList.add(map);
                adapter.notifyDataSetChanged();
            }
        }



        else if(answer.contains("who")){
            if(answer.contains("you")){
                handler.removeCallbacks(runnable);
                textToSpeech.speak("Hello, I am the railway assistant, I can assist you through the railway station and I can also answer your questions about trains", TextToSpeech.QUEUE_ADD, null, "hello");

                map = new HashMap();
                map.put("POSITION", "LEFT");
                map.put("TEXT", "Hello, I am the railway assistant, I can assist you through the railway station and I can also answer your questions about trains");
                mArrayList.add(map);
                adapter.notifyDataSetChanged();
            }
        }


        else if(answer.contains("show command")){
            handler.removeCallbacks(runnable);
            textToSpeech.speak("The commands you can use are here", TextToSpeech.QUEUE_ADD, null, "received");
            map = new HashMap();
            map.put("POSITION", "LEFT");
            map.put("TEXT", commandsUsed);
            mArrayList.add(map);
            adapter.notifyDataSetChanged();

        }

        else if(answer.contains("how many seats are available in")){
            Log.e(TAG, "onResults: seats");

            handler.removeCallbacks(runnable);
            final String finalAnswer2 = answer;

            mRef.child("TRAIN TABLE").child("TRAINS").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int flag = 0;
                    for(DataSnapshot snap : snapshot.getChildren()){
                        if(finalAnswer2.contains(snap.child("TRAIN NAME").getValue().toString().toLowerCase())){
                            textToSpeech.speak("What category", TextToSpeech.QUEUE_ADD, null, "train category");
                            intentKey = snap.getKey();
                            map = new HashMap();
                            map.put("POSITION", "LEFT");
                            map.put("TEXT", "What category? 3AC, 2AC, 1AC, CC, FC, SC, ST");
                            mArrayList.add(map);
                            flag++;
                            adapter.notifyDataSetChanged();
                        }
                    }
                    if(flag == 0){
                        textToSpeech.speak("No data was found, sorry", TextToSpeech.QUEUE_ADD, null, "hello");
                        map = new HashMap();
                        map.put("POSITION", "LEFT");
                        map.put("TEXT", "No data was found, sorry");
                        mArrayList.add(map);
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

        else if(answer.contains("are there trains to") || answer.contains("trains to")){
            Log.e(TAG, "onResults: Is there a train" );
            handler.removeCallbacks(runnable);

            final String finalAnswer1 = answer;
            mRef.child("TRAIN TABLE").child("STATIONS").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.e(TAG, "onDataChange: Is there train snapshots" );
                    int flag = 0;
                    String city = null, key, finalKey = null;
                    final ArrayList<String> cityCode = new ArrayList<>();
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        city = snap.child("CITY").getValue().toString().toLowerCase();
                        key = snap.child("STATION CODE").getValue().toString().toLowerCase();
                        Log.e(TAG, "onDataChange: " + city );
                        if (finalAnswer1.contains(city)) {
                            finalKey = city;
                            Log.e(TAG, "onDataChange: If" + city );
                            cityCode.add(key);
                            Log.e(TAG, "onDataChange: " + key );
                            flag++;
                        }
                    }

                    if (flag != 0) {
                        Log.e(TAG, "onDataChange: Keys found" );
                        final String finalCity = finalKey;
                        mRef.child("TRAIN TABLE").child("ARRIVAL").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String speech = "Trains are available to " + finalCity + " for station codes: ";
                                String details = "\n\n";
                                int flag = 0;
                                Log.e(TAG, "onDataChange: ARRIVAL data for available train" );
                                for (DataSnapshot snap : snapshot.getChildren()) {
                                    for (int i = 0; i < cityCode.size(); i++) {
                                        Log.e(TAG, "onDataChange: CODE" + snap.child("STATION CODE").getValue().toString() + " 0 " + cityCode.get(i));
                                        if (snap.child("STATION CODE").getValue().toString().toLowerCase().contains(cityCode.get(i))) {
                                            Log.e(TAG, "onDataChange: CODE" + snap.child("STATION CODE").getValue().toString() + " 0 " + cityCode.get(i));
                                            flag++;
                                            speech = speech + cityCode.get(i) + " ";
                                            details = details + snap.getValue() + "\n\n";
                                        }
                                    }
                                }
                                if(flag == 0){
                                    textToSpeech.speak("No data was found, sorry", TextToSpeech.QUEUE_ADD, null, "hello");
                                    map = new HashMap();
                                    map.put("POSITION", "LEFT");
                                    map.put("TEXT", "No data was found, sorry");
                                    mArrayList.add(map);
                                    adapter.notifyDataSetChanged();
                                }

                                else{
                                    String readableString =  details;
                                    readableString = readableString.replace("{", " ");
                                    readableString = readableString.replace("}", "");
                                    readableString = readableString.replace("=", ":");
                                    readableString = readableString.replace(",", "\n");

                                    textToSpeech.speak(speech, TextToSpeech.QUEUE_ADD, null, "hello");
                                    map = new HashMap();
                                    map.put("POSITION", "LEFT");
                                    map.put("TEXT", speech + readableString);
                                    mArrayList.add(map);
                                    adapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                    else{
                        textToSpeech.speak("No data was found, sorry", TextToSpeech.QUEUE_ADD, null, "hello");
                        map = new HashMap();
                        map.put("POSITION", "LEFT");
                        map.put("TEXT", "No data was found, sorry");
                        mArrayList.add(map);
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        }

        else if(answer.contains("when will train") && answer.contains("arrive at")){
            Log.e(TAG, "onResults: train arrive" );

            final String[] key = new String[1], key1 = new String[1];

            handler.removeCallbacks(runnable);
            final String finalAnswer = answer;

            mRef.child("TRAIN TABLE").child("ARRIVAL").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int flag = 0;
                    if(snapshot.exists()) {
                        Log.e(TAG, "onDataChange: Snap shot exists" );
                        for (DataSnapshot snap : snapshot.getChildren()){
                            key[0] = snap.child("TRAIN NO").getValue().toString().toLowerCase();
                            key1[0] = snap.child("STATION CODE").getValue().toString().toLowerCase();

                            Log.e(TAG, "onDataChange: Key" + key[0] + " Code " + key1[0]);
                            if (finalAnswer.contains(key[0]) && finalAnswer.contains(key1[0])) {

                                textToSpeech.speak("Train " + key[0] + " will arrive on " + snap.child("ARRIVAL").getValue().toString() + " at " + key1[0], TextToSpeech.QUEUE_ADD, null, "hello");

                                flag ++;

                                map = new HashMap();
                                map.put("POSITION", "LEFT");
                                map.put("TEXT", "Train " + key[0] + " will arrive on " + snap.child("ARRIVAL").getValue().toString() + " at " + key1[0]);
                                mArrayList.add(map);
                                adapter.notifyDataSetChanged();
                            }
                        }

                        if(flag == 0){
                            textToSpeech.speak("Sorry, no trains of that number was found", TextToSpeech.QUEUE_ADD, null, "hello");
                            map = new HashMap();
                            map.put("POSITION", "LEFT");
                            map.put("TEXT", "Sorry, no trains of that number was found");
                            mArrayList.add(map);
                            adapter.notifyDataSetChanged();
                        }
                    }
                    else{
                        textToSpeech.speak("Sorry, no data was found", TextToSpeech.QUEUE_ADD, null, "hello");
                        map = new HashMap();
                        map.put("POSITION", "RIGHT");
                        map.put("TEXT", "Sorry, no data was found");
                        mArrayList.add(map);
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        }

        else if(answer.contains("hello") || answer.contains("hay") || answer.contains("sup") || answer.contains("hi") || answer.contains("hey")){

            handler.removeCallbacks(runnable);
            textToSpeech.speak("Hey, how can I help you?", TextToSpeech.QUEUE_ADD, null, "hello");

            map = new HashMap();
            map.put("POSITION", "LEFT");
            map.put("TEXT", "Hey, how can I help you?");
            mArrayList.add(map);
            adapter.notifyDataSetChanged();

        }

        else if(answer.contains("what is the time") || answer.contains("time")){

            String time = "The time is " + getTime();
            handler.removeCallbacks(runnable);
            textToSpeech.speak(time, TextToSpeech.QUEUE_ADD, null, "time/date");

            map = new HashMap();
            map.put("POSITION", "LEFT");
            map.put("TEXT", time);
            mArrayList.add(map);
            adapter.notifyDataSetChanged();
        }

        else if(answer.contains("what is the date") || answer.contains("date")){

            String date = "The date is " + getDate();
            handler.removeCallbacks(runnable);
            textToSpeech.speak(date, TextToSpeech.QUEUE_ADD, null, "time/date");

            map = new HashMap();
            map.put("POSITION", "LEFT");
            map.put("TEXT", date);
            mArrayList.add(map);
            adapter.notifyDataSetChanged();
        }

        else if(answer.contains("shut up")){
            handler.removeCallbacks(runnable);
            textToSpeech.speak("okay", TextToSpeech.QUEUE_ADD, null, "time/date");

            map = new HashMap();
            map.put("POSITION", "LEFT");
            map.put("TEXT", "Okay");
            mArrayList.add(map);
            adapter.notifyDataSetChanged();
        }

        else {
            handler.removeCallbacks(runnable);
            textToSpeech.speak("Sorry, I didn't get you, can you please repeat", TextToSpeech.QUEUE_ADD, null, "invalid");
            isUserVoiceRecognized = false;
            map = new HashMap();
            map.put("POSITION", "LEFT");
            map.put("TEXT", "Sorry, I didn't get you, can you please repeat");
            mArrayList.add(map);
            adapter.notifyDataSetChanged();
        }

    }

    private boolean checkIfPermissionGranted() {
        int recorder_permission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO);

        return recorder_permission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionForRecorder(){
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1000) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void restartRecognizer() {
        intCount = 0;
        twoMinuteCounter = 0;
        Handler loopHandler = new Handler(Looper.getMainLooper());
        loopHandler.post(new Runnable() {
            @Override
            public void run() {
                speechRecognizer.startListening(speechRecognizerIntent);
                handler.postDelayed(runnable, TIME);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 2707 && resultCode == Activity.RESULT_OK){
            textToSpeech.speak("Force override was successful", TextToSpeech.QUEUE_FLUSH, null,"override_success");
            setForceOverrideFlag = true;
            commandBox.setEnabled(true);
            commandBox.setVisibility(View.VISIBLE);
            commandButton.setVisibility(View.VISIBLE);
            countDownTimer.start();
            Log.e(TAG, "onActivityResult: " + commandBox.getVisibility() );
            map = new HashMap();
            map.put("POSITION", "LEFT");
            map.put("TEXT", "Force override was successful");
            mArrayList.add(map);
            adapter.notifyDataSetChanged();

        }

        else if(requestCode == 2707 && resultCode == Activity.RESULT_CANCELED){
            textToSpeech.speak("Force override failed", TextToSpeech.QUEUE_FLUSH, null,"hello");

            map = new HashMap();
            map.put("POSITION", "LEFT");
            map.put("TEXT", "Force override failed");
            mArrayList.add(map);
            adapter.notifyDataSetChanged();
        }

        else if(requestCode == 1812 && resultCode == Activity.RESULT_OK){
            textToSpeech.speak("Force disable was successful", TextToSpeech.QUEUE_FLUSH, null,"override_success");
            setForceOverrideFlag = false;
            commandBox.setVisibility(View.GONE);
            commandButton.setVisibility(View.GONE);
            commandBox.setEnabled(false);
            countDownTimer.start();
            Log.e(TAG, "onActivityResult: " + commandBox.getVisibility() );
            map = new HashMap();
            map.put("POSITION", "LEFT");
            map.put("TEXT", "Force disable was successful");
            mArrayList.add(map);
            adapter.notifyDataSetChanged();

        }

        else if(requestCode == 1812 && resultCode == Activity.RESULT_CANCELED){
            textToSpeech.speak("Force disable failed", TextToSpeech.QUEUE_FLUSH, null,"hello");

            map = new HashMap();
            map.put("POSITION", "LEFT");
            map.put("TEXT", "Force disable failed");
            mArrayList.add(map);
            adapter.notifyDataSetChanged();
        }

        /**
         * */

        else if(requestCode == 5010 && resultCode == Activity.RESULT_OK){
            textToSpeech.speak("Force override was successful", TextToSpeech.QUEUE_FLUSH, null,"override_success");
            enablePopUpMenu = true;
            map = new HashMap();
            map.put("POSITION", "LEFT");
            map.put("TEXT", "Force override was successful");
            mArrayList.add(map);
            adapter.notifyDataSetChanged();

        }

        else if(requestCode == 5010 && resultCode == Activity.RESULT_CANCELED){
            textToSpeech.speak("Force override failed", TextToSpeech.QUEUE_FLUSH, null,"hello");

            map = new HashMap();
            map.put("POSITION", "LEFT");
            map.put("TEXT", "Force override failed");
            mArrayList.add(map);
            adapter.notifyDataSetChanged();
        }

        else if(requestCode == 6010 && resultCode == Activity.RESULT_OK){
            textToSpeech.speak("Force disable was successful", TextToSpeech.QUEUE_FLUSH, null,"override_success");
            Log.e(TAG, "onActivityResult: " + commandBox.getVisibility() );
            enablePopUpMenu = false;
            map = new HashMap();
            map.put("POSITION", "LEFT");
            map.put("TEXT", "Force disable was successful");
            mArrayList.add(map);
            adapter.notifyDataSetChanged();

        }

        else if(requestCode == 6010 && resultCode == Activity.RESULT_CANCELED){
            textToSpeech.speak("Force disable failed", TextToSpeech.QUEUE_FLUSH, null,"hello");

            map = new HashMap();
            map.put("POSITION", "LEFT");
            map.put("TEXT", "Force disable failed");
            mArrayList.add(map);
            adapter.notifyDataSetChanged();
        }



        else if(requestCode == 1000 && resultCode == Activity.RESULT_OK){
            assert data != null;
            keyWord = data.getStringExtra("KEY").toLowerCase();

            map = new HashMap();
            map.put("POSITION", "RIGHT");
            map.put("TEXT", keyWord);
            mArrayList.add(map);
            adapter.notifyDataSetChanged();

            if(data.getStringExtra("QUERY").contains("train category")){
                DatabaseReference databaseReference = null;
                String reference = null;
                int flag = 0;
                if(keyWord.contains("1ac") || keyWord.contains("1 ac")){
                    reference = "A_1AC";
                    flag++;
                }
                else if(keyWord.contains("2ac") || keyWord.contains("2 ac")){
                    reference = "A_2AC";
                    flag++;
                }
                else if(keyWord.contains("3ac") || keyWord.contains("3 ac")){
                    reference = "A_3AC";
                    flag++;
                }
                else if(keyWord.contains("cc")|| keyWord.contains("c c")){
                    reference = "A_CC";
                    flag++;
                }
                else if(keyWord.contains("fc") || keyWord.contains("f c")){
                    reference = "A_FC";
                    flag++;
                }
                else if(keyWord.contains("sc") || keyWord.contains("s c")){
                    reference = "A_SC";
                    flag++;
                }
                else if(keyWord.contains("st") || keyWord.contains("s t")){
                    reference = "A_ST";
                    flag++;
                }

                final String finalReference = reference;
                Log.e(TAG, "onActivityResult: " + intentKey );
                if(flag!=0){
                    mRef.child("TRAIN TABLE").child("TRAINS").child(intentKey.trim()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Log.e(TAG, "onDataChange: " + snapshot );
                            textToSpeech.speak(snapshot.child("TRAIN NAME").getValue().toString() + " train " + "has " + snapshot.child(finalReference).getValue().toString() + " seats" + " in " + finalReference, TextToSpeech.QUEUE_FLUSH, null, "received");

                            map = new HashMap();
                            map.put("POSITION", "LEFT");
                            map.put("TEXT", snapshot.child("TRAIN NAME").getValue().toString() + " train " + "has " + snapshot.child(finalReference).getValue().toString() + " seats" + " in " + finalReference);
                            mArrayList.add(map);
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
                else{
                    textToSpeech.speak("Something happened, please try again later", TextToSpeech.QUEUE_FLUSH, null, "received");

                    map = new HashMap();
                    map.put("POSITION", "LEFT");
                    map.put("TEXT","Something happened, please try again later");
                    mArrayList.add(map);
                    adapter.notifyDataSetChanged();
                }

            }

        }
        else{
            textToSpeech.speak("Something happened, please try again later", TextToSpeech.QUEUE_FLUSH, null, "received");

            map = new HashMap();
            map.put("POSITION", "LEFT");
            map.put("TEXT","Something happened, please try again later");
            mArrayList.add(map);
            adapter.notifyDataSetChanged();

        }
    }
    public String getTime() {
        Date currentTime = Calendar.getInstance().getTime();
        DateFormat timeFormat = new SimpleDateFormat("HH:mm");
        return timeFormat.format(currentTime);
    }

    public String getDate() {
        Date currentTime = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
        return dateFormat.format(currentTime);
    }

}