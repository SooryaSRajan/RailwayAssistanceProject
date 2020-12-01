package com.ssr_projects.railasistant;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ssr_projects.railasistant.Registration.QRCodeActivity;
import com.ssr_projects.railasistant.Registration.ReservationLogin;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
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
    private ListView listView;
    private ArrayList<HashMap> mArrayList = new ArrayList<>();
    private ConversationAdapter adapter;
    private HashMap map;
    private TextView helloTextView;
    private GifImageView gifImageView, voiceGifView;
    private int intCount = 0, twoMinuteCounter = 0;
    private DatabaseReference mRef = FirebaseDatabase.getInstance().getReference();
    private String commandsUsed =
            "List of commands that can be used are: " +
            "\n->Hello Assistant" +
            "\n->Who are you" +
            "\n->What are the available trains*" +
            "\n->How many seats are available in *train name* " +
            "\n->Are there trains to *station name* (from palakkad)" +
            "\n->When will train *train name* arrive at *station name*" +
            "\n->When will train *train name* start from *station name*" +
            "\n->When will the next train reach *station Name*" +
            "\n->When will the next train arrive here" +
            "\n->When will *train name* reach here" +
            "\n->Which train will arrive at platform *platform no*" +
            "\n->Which platform will *train name* arrive at " +
            "\n->Show run schedule" +
            "\n->Which is the train number of *train name*" +
            "\n->When will trains going to *station name* reach here" +
            "\n->What is the train name of *train number*" +
            "\n->I want to reserve a ticket" +
            "\n->Show station details of *station name*" +
            "\n->What are the connected stations*" +
            "\n->Show QR code" +
            "\n->What is the Time" +
            "\n->What is the Date";

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private final int TIME_GREATER = -1;
    private final int TIME_LESSER = 1;
    private final int TIME_EQUAL = 0;
    private AudioManager mAudioManager;
    private int mStreamVolume = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(FirebaseAuth.getInstance().getCurrentUser() != null)
            FirebaseAuth.getInstance().signOut();

        Log.e(TAG, "onCreate: Create" );
        helloTextView = findViewById(R.id.hello_text);
        gifImageView = findViewById(R.id.ai_gif);
        voiceGifView = findViewById(R.id.voice_recognizer_animation);
        voiceGifView.setVisibility(View.GONE);

        adjustAudio(true);

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
                    if(intCount == 20 && !setViewFlag){
                        intCount = 0;
                        twoMinuteCounter = 0;
                        mArrayList.clear();
                        adapter.notifyDataSetChanged();
                        helloTextView.setVisibility(View.VISIBLE);
                        gifImageView.setVisibility(View.VISIBLE);
                    }
                    else if(intCount == 20){
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

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);// getting system volume into var for later un-muting
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

                    else if(s.contains("reg_qr_code")){
                        Log.e(TAG, "onDone: " + s );
                        Intent i = new Intent(MainActivity.this, QRCodeActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        textToSpeech.shutdown();
                        finish();
                    }

                    else if(s.contains("reservation_yes_no")){
                        Log.e(TAG, "onDone: " + s );
                        Intent i = new Intent(MainActivity.this, VoiceResultActivity.class);
                        startActivityForResult(i, 8080);
                    }

                    else if(s.contains("reg_sign_in")){
                        Log.e(TAG, "onDone: " + s );
                        Intent i = new Intent(MainActivity.this, ReservationLogin.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        textToSpeech.shutdown();
                        finish();
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
                adjustAudio(true);
                isUserVoiceRecognized = true;
                Log.e(TAG, "onReadyForSpeech: " );
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                Log.e(TAG, "onReadyForSpeech: " + data );
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
                Log.e(TAG, "onError: " + i );
            }

            @Override
            public void onResults(Bundle bundle) {
                adjustAudio(false);
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
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String answer = data.get(0);
                Log.e(TAG, "partial: " + answer );
            }

            @Override
            public void onEvent(int i, Bundle bundle) {
                isUserVoiceRecognized = true;
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String answer = data.get(0);
                Log.e(TAG, "onEvent: " + answer );
            }
        });

    }

    private void inputTextFunction(final String answer){
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

        else if(answer.contains("what are") && answer.contains("available") && answer.contains("train")){
            handler.removeCallbacks(runnable);
            mRef.child("TRAIN TABLE").child("TRAINS").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String trainName = "\n\n";
                    for(DataSnapshot snap : snapshot.getChildren()){
                        trainName += "Train Name: " + snap.child("TRAIN NAME").getValue().toString() + "\nTrain Number: " + snap.child("TRAIN NO").getValue().toString() + "\n\n";
                    }

                    textToSpeech.speak("The available trains are given below", TextToSpeech.QUEUE_ADD, null, "hello");
                    map = new HashMap();
                    map.put("POSITION", "LEFT");
                    map.put("TEXT", "The available trains are given below : " + trainName);
                    mArrayList.add(map);
                    adapter.notifyDataSetChanged();

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

        else if(answer.contains("show station details of")){
            handler.removeCallbacks(runnable);
            mRef.child("TRAIN TABLE").child("STATIONS").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int flag = 0;
                    for(DataSnapshot snap : snapshot.getChildren()){
                        if(answer.contains(snap.child("STATION NAME").getValue().toString().toLowerCase())){
                            flag++;
                            String stationName = snap.child("STATION NAME").getValue().toString();
                            textToSpeech.speak("The station details of " + stationName + " is given below ", TextToSpeech.QUEUE_ADD, null, "hello");
                            map = new HashMap();
                            String readableString = snap.getValue().toString();

                            readableString = readableString.replace("{", " ");
                            readableString = readableString.replace("}", "\n\n");
                            readableString = readableString.replace("=", " : ");
                            readableString = readableString.replace(",", "\n");

                            map.put("POSITION", "LEFT");
                            map.put("TEXT", "The station details of " + stationName + " is given below: \n\n" + readableString);
                            mArrayList.add(map);
                            adapter.notifyDataSetChanged();
                            break;
                        }
                    }
                    if(flag == 0){
                        textToSpeech.speak("Sorry, that was not a valid station", TextToSpeech.QUEUE_ADD, null, "hello");
                        map = new HashMap();
                        map.put("POSITION", "LEFT");
                        map.put("TEXT", "Sorry, that was not a valid station");
                        mArrayList.add(map);
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

        else if(answer.contains("what are the connected stations")){
            handler.removeCallbacks(runnable);
            mRef.child("TRAIN TABLE").child("STATIONS").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int flag = 0;
                    String allStationNames = "";
                    for(DataSnapshot snap : snapshot.getChildren()){
                            flag++;
                            if(!snap.child("STATION CODE").getValue().toString().contains("PGT"))
                            allStationNames += flag + ". " + snap.child("STATION NAME").getValue().toString() + "\n";

                    }
                    if(flag != 0){
                        textToSpeech.speak("The following are the stations connected to this station ", TextToSpeech.QUEUE_ADD, null, "hello");
                        map = new HashMap();
                        map.put("POSITION", "LEFT");
                        map.put("TEXT", "The following are the stations connected to this station: " + "\n" + allStationNames);
                        mArrayList.add(map);
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
/**
 * when will the next train reach here
 */
        else if(answer.contains("when will") && answer.contains("next train") && (answer.contains("arrive here") || answer.contains("reach here")) ){
            handler.removeCallbacks(runnable);
            final String[] trainTime = {""};
            mRef.child("TRAIN TABLE").child("ARRIVAL").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        if (snap.child("STATION CODE").getValue().toString().contains("PGT")) {
                            String time = snap.child("ARRIVAL").getValue().toString();
                            String trainNo = snap.child("TRAIN NO").getValue().toString();
                            try {
                                if (compareToSystemTime(time) == TIME_GREATER || compareToSystemTime(time) == TIME_EQUAL) {
                                    trainTime[0] += "\nTrain Number: " + trainNo + " Time: " +time;
                                }

                            } catch (Exception e) {
                                Log.e(TAG, "Time Exception: " + e.toString());
                            }
                        }
                    }
                    if (trainTime[0] != "") {
                        textToSpeech.speak("The following timings were found", TextToSpeech.QUEUE_ADD, null, "hello");
                        map = new HashMap();
                        map.put("POSITION", "LEFT");
                        map.put("TEXT", "The following timings were found" + "\n" + trainTime[0]);
                        mArrayList.add(map);
                        adapter.notifyDataSetChanged();
                    }
                    else{
                        textToSpeech.speak("Sorry, no trains are available today, after this time", TextToSpeech.QUEUE_ADD, null, "hello");
                        map = new HashMap();
                        map.put("POSITION", "LEFT");
                        map.put("TEXT", "Sorry, no trains are available today, after this time");
                        mArrayList.add(map);
                        adapter.notifyDataSetChanged();
                    }
                }


                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

        /**
         * When will the next train reach *station*
         */

        else if(answer.contains("when will") && answer.contains("next train") && answer.contains("reach")){
            handler.removeCallbacks(runnable);
            final String[] trainTime = {""};
            final String[] stationName = new String[1];
            final int[] count = {0};
            mRef.child("TRAIN TABLE").child("STATIONS").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for(final DataSnapshot snapSuper : snapshot.getChildren()){
                        Log.e(TAG, "onDataChange: Next train checking in for given train");
                    if(answer.contains(snapSuper.child("STATION NAME").getValue().toString().toLowerCase())){
                        count[0]++;
                        stationName[0] = snapSuper.child("STATION NAME").getValue().toString().toLowerCase();
                        final String stationCode = snapSuper.child("STATION CODE").getValue().toString();
                        mRef.child("TRAIN TABLE").child("ARRIVAL").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for(DataSnapshot snap : snapshot.getChildren()){
                                    if(snap.child("STATION CODE").getValue().toString().contains(stationCode)
                                        && snap.child("TYPE").getValue().toString().contains("TO")){
                                        String time = snap.child("ARRIVAL").getValue().toString();
                                        try {
                                            if (compareToSystemTime(time) == TIME_GREATER || compareToSystemTime(time) == TIME_EQUAL) {
                                                trainTime[0] += "\n"  + "Train Number: " + snap.child("TRAIN NO").getValue().toString() + " Time: " + time;
                                            }

                                        } catch (Exception e) {
                                            Log.e(TAG, "Chat Adapter Time Exception: " + e.toString());
                                        }
                                    }
                                }
                                if (trainTime[0] != "") {
                                    textToSpeech.speak("The following timings were found for station " + stationName[0], TextToSpeech.QUEUE_ADD, null, "hello");
                                    map = new HashMap();
                                    map.put("POSITION", "LEFT");
                                    map.put("TEXT", "The following timings were found for station " + stationName[0] + "\n" + trainTime[0]);
                                    mArrayList.add(map);
                                    adapter.notifyDataSetChanged();
                                }
                                else{
                                    textToSpeech.speak("Sorry, no trains are available today, after this time", TextToSpeech.QUEUE_ADD, null, "hello");
                                    map = new HashMap();
                                    map.put("POSITION", "LEFT");
                                    map.put("TEXT", "Sorry, no trains are available today, after this time");
                                    mArrayList.add(map);
                                    adapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                    }
                    if(count[0] == 0){
                        textToSpeech.speak("Sorry, no trains were found", TextToSpeech.QUEUE_ADD, null, "hello");
                        map = new HashMap();
                        map.put("POSITION", "LEFT");
                        map.put("TEXT", "Sorry, no trains were found");
                        mArrayList.add(map);
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        }


        else if(answer.contains("when will") && (answer.contains("trains going to") || answer.contains("train going to")) && (answer.contains("reach this station") || answer.contains("reach the station"))){
            handler.removeCallbacks(runnable);
            final String[] trainTime = {""};
            final String[] stationName = new String[1];
            final int[] count = {0};
            mRef.child("TRAIN TABLE").child("STATIONS").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for(final DataSnapshot snapSuper : snapshot.getChildren()){
                        Log.e(TAG, "onDataChange: Next train checking in for given train");
                        if(answer.contains(snapSuper.child("STATION NAME").getValue().toString().toLowerCase())){
                            count[0]++;
                            stationName[0] = snapSuper.child("STATION NAME").getValue().toString().toLowerCase();
                            final String stationCode = snapSuper.child("STATION CODE").getValue().toString();
                            mRef.child("TRAIN TABLE").child("ARRIVAL").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for(DataSnapshot snap : snapshot.getChildren()){
                                        if(snap.child("STATION CODE").getValue().toString().contains(stationCode)
                                                && snap.child("TYPE").getValue().toString().contains("TO")){
                                            for(DataSnapshot snapSub : snapshot.getChildren()){
                                                if(snapSub.child("TRAIN NO").getValue().toString().contains(snap.child("TRAIN NO").getValue().toString())
                                                && snapSub.child("STATION CODE").getValue().toString().contains("PGT")){
                                                    if(!trainTime[0].contains("Train Number: " + snap.child("TRAIN NO").getValue().toString() + " Time: " + snapSub.child("ARRIVAL").getValue().toString()))
                                                    trainTime[0] += "\n"  + "Train Number: " + snap.child("TRAIN NO").getValue().toString() + " Time: " + snapSub.child("ARRIVAL").getValue().toString();
                                                }
                                            }

                                        }
                                    }
                                    if (trainTime[0] != "") {
                                        textToSpeech.speak("The trains going to " + stationName[0] + " will reach here by the following time", TextToSpeech.QUEUE_ADD, null, "hello");
                                        map = new HashMap();
                                        map.put("POSITION", "LEFT");
                                        map.put("TEXT", "The trains going to " + stationName[0] + " will reach here by the following time \n" + trainTime[0]);
                                        mArrayList.add(map);
                                        adapter.notifyDataSetChanged();
                                    }
                                    else{
                                        textToSpeech.speak("Sorry, no trains were found", TextToSpeech.QUEUE_ADD, null, "hello");
                                        map = new HashMap();
                                        map.put("POSITION", "LEFT");
                                        map.put("TEXT", "Sorry, no trains were found");
                                        mArrayList.add(map);
                                        adapter.notifyDataSetChanged();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }
                    }
                    if(count[0] == 0){
                        textToSpeech.speak("Sorry, no trains were found", TextToSpeech.QUEUE_ADD, null, "hello");
                        map = new HashMap();
                        map.put("POSITION", "LEFT");
                        map.put("TEXT", "Sorry, no trains were found");
                        mArrayList.add(map);
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        }


        /**
         * when will *train* reach here
         * Doesnt depend on system time
         */

        else if(answer.contains("when will") && answer.contains("reach here")){
            handler.removeCallbacks(runnable);
            mRef.child("TRAIN TABLE").child("TRAINS").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int count = 0;
                    String trainInformation = "";
                    for(DataSnapshot snap : snapshot.getChildren()){
                        if(answer.contains(snap.child("TRAIN NAME").getValue().toString().toLowerCase())){
                             count++;
                             trainInformation += "Train Name: " + snap.child("TRAIN NAME").getValue().toString() + "\n";
                             final String trainKey = snap.child("TRAIN NO").getValue().toString();
                             trainInformation += "Train Number: " + trainKey + "\n";
                            final String finalTrainInformation = trainInformation;
                            mRef.child("TRAIN TABLE").child("ARRIVAL").addListenerForSingleValueEvent(new ValueEventListener() {
                              @Override
                              public void onDataChange(@NonNull DataSnapshot snapshot) {
                                  String trainTime = "";
                                  for(DataSnapshot snap : snapshot.getChildren()) {
                                    if(snap.child("TRAIN NO").getValue().toString().contains(trainKey) && snap.child("STATION CODE").getValue().toString().contains("PGT")){
                                        trainTime += snap.child("ARRIVAL").getValue().toString() + "\n";
                                    }
                                  }
                                  if(trainTime != ""){
                                      textToSpeech.speak("The following timings were found for the train", TextToSpeech.QUEUE_ADD, null, "hello");
                                      map = new HashMap();
                                      map.put("POSITION", "LEFT");
                                      map.put("TEXT", "The following timings were found for the train" + "\n" + finalTrainInformation + "\n" + trainTime);
                                  }
                                  else{
                                      textToSpeech.speak("Sorry, no timings were found", TextToSpeech.QUEUE_ADD, null, "hello");
                                      map = new HashMap();
                                      map.put("POSITION", "LEFT");
                                      map.put("TEXT", "Sorry, no timings were found");
                                  }
                                  mArrayList.add(map);
                                  adapter.notifyDataSetChanged();
                              }

                              @Override
                              public void onCancelled(@NonNull DatabaseError error) {

                              }
                          });
                        }
                    }
                    if(count == 0){
                        textToSpeech.speak("Sorry, invalid train name", TextToSpeech.QUEUE_ADD, null, "hello");
                        map = new HashMap();
                        map.put("POSITION", "LEFT");
                        map.put("TEXT", "Sorry, invalid train name");
                        mArrayList.add(map);
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
            }

        else if(answer.contains("want to reserve a ticket")){

            handler.removeCallbacks(runnable);
            textToSpeech.speak("Okay, do you have an account? Yes or No", TextToSpeech.QUEUE_ADD, null, "reservation_yes_no");

            map = new HashMap();
            map.put("POSITION", "LEFT");
            map.put("TEXT", "Okay, do you have an account? Yes or No");
            mArrayList.add(map);
            adapter.notifyDataSetChanged();

        }

        else if(answer.contains("show qr code")){
            handler.removeCallbacks(runnable);
            textToSpeech.speak("Sure, scan this QR code to register an account to reserve tickets", TextToSpeech.QUEUE_ADD, null, "reg_qr_code");

            map = new HashMap();
            map.put("POSITION", "LEFT");
            map.put("TEXT", "Sure, scan this QR code to register an account to reserve tickets");
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
                        if(snap.child("TYPE").getValue().toString().contains("EXPRESS") && finalAnswer2.contains(snap.child("TRAIN NAME").getValue().toString().toLowerCase())){
                            textToSpeech.speak("What category", TextToSpeech.QUEUE_ADD, null, "train category");
                            intentKey = snap.getKey();
                            map = new HashMap();
                            map.put("POSITION", "LEFT");
                            map.put("TEXT", "What category? 3AC, 2AC, 1AC, CC, FC, SC, ST");
                            mArrayList.add(map);
                            flag++;
                            adapter.notifyDataSetChanged();
                        }
                        else if(snap.child("TYPE").getValue().toString().contains("ORDINARY") &&
                                finalAnswer2.contains(snap.child("TRAIN NAME").getValue().toString().toLowerCase())){
                            String noOfSeats = snap.child("A_SC").getValue().toString();
                            textToSpeech.speak("This is an ordinary train, SC class has " + noOfSeats + " seats", TextToSpeech.QUEUE_ADD, null, "hello");
                            intentKey = snap.getKey();
                            map = new HashMap();
                            map.put("POSITION", "LEFT");
                            map.put("TEXT", "This is an ordinary train, SC class has " + noOfSeats + " seats");
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
        else if(answer.contains("show") && (answer.contains("run schedule") || answer.contains("runs schedule"))){
            handler.removeCallbacks(runnable);
            Query query = mRef.child("TRAIN TABLE").child("PLATFORM").orderByChild("PLATFORM NO");
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String readableString = "\n";
                    int i = 0;
                    for(DataSnapshot snap : snapshot.getChildren()){
                        i++;
                        readableString += i + ".\n" + snap.getValue();
                    }
                    readableString = readableString.replace("{", " ");
                    readableString = readableString.replace("}", "\n\n");
                    readableString = readableString.replace("=", " : ");
                    readableString = readableString.replace(",", "\n");

                    textToSpeech.speak("Run schedule for the platforms are given below", TextToSpeech.QUEUE_ADD, null, "hello");
                    map = new HashMap();
                    map.put("POSITION", "LEFT");
                    map.put("TEXT", "Run schedule for the platforms are given below\n" + readableString);
                    mArrayList.add(map);
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

        else if(answer.contains("what is") && answer.contains("train number") && answer.contains("of")){
            handler.removeCallbacks(runnable);
            mRef.child("TRAIN TABLE").child("TRAINS").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int flag = 0;
                    for(DataSnapshot snap: snapshot.getChildren()){
                        if(answer.contains(Objects.requireNonNull(snap.child("TRAIN NAME").getValue()).toString().toLowerCase())){
                            flag++;
                            textToSpeech.speak("The train number of " + snap.child("TRAIN NAME").getValue().toString() + " is " + snap.child("TRAIN NO").getValue().toString(), TextToSpeech.QUEUE_ADD, null, "hello");
                            map = new HashMap();
                            map.put("POSITION", "LEFT");
                            map.put("TEXT", "The train number of " + snap.child("TRAIN NAME").getValue().toString() + " is " + snap.child("TRAIN NO").getValue().toString());
                            mArrayList.add(map);
                            adapter.notifyDataSetChanged();
                            break;
                        }
                    }
                    if(flag == 0){
                        textToSpeech.speak("Sorry, incorrect train name", TextToSpeech.QUEUE_ADD, null, "hello");
                        map = new HashMap();
                        map.put("POSITION", "LEFT");
                        map.put("TEXT", "Sorry, incorrect train name");
                        mArrayList.add(map);
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

        else if(answer.contains("what is") && answer.contains("train name") && answer.contains("of")){
            handler.removeCallbacks(runnable);
            mRef.child("TRAIN TABLE").child("TRAINS").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int flag = 0;
                    for(DataSnapshot snap: snapshot.getChildren()){
                        if(answer.replaceAll("\\s", "").contains(Objects.requireNonNull(snap.child("TRAIN NO").getValue()).toString().toLowerCase())){
                            flag++;
                            textToSpeech.speak("The train name of " + snap.child("TRAIN NO").getValue().toString() + " is " + snap.child("TRAIN NAME").getValue().toString(), TextToSpeech.QUEUE_ADD, null, "hello");
                            map = new HashMap();
                            map.put("POSITION", "LEFT");
                            map.put("TEXT", "The train name of " + snap.child("TRAIN NO").getValue().toString() + " is " + snap.child("TRAIN NAME").getValue().toString());
                            mArrayList.add(map);
                            adapter.notifyDataSetChanged();
                            break;
                        }
                    }
                    if(flag == 0){
                        textToSpeech.speak("Sorry, incorrect train number", TextToSpeech.QUEUE_ADD, null, "hello");
                        map = new HashMap();
                        map.put("POSITION", "LEFT");
                        map.put("TEXT", "Sorry, incorrect train number");
                        mArrayList.add(map);
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }


        else if(answer.contains("which train") && answer.contains("platform")){
            handler.removeCallbacks(runnable);
            final String[] trainName = {""};
            final String[] trainNo = {""};
            final String[] platformNo = {""};
            final String finalAnswer = answer;
            mRef.child("TRAIN TABLE").child("PLATFORM").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int flag = 0;
                    for(DataSnapshot snap : snapshot.getChildren()){
                        if(finalAnswer.contains(snap.child("PLATFORM NO").getValue().toString())){
                            flag++;
                            platformNo[0] = snap.child("PLATFORM NO").getValue().toString();
                            trainNo[0] = snap.child("TRAIN NO").getValue().toString();
                            mRef.child("TRAIN TABLE").child("TRAINS").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    int flag = 0;
                                    for(DataSnapshot snap : snapshot.getChildren()){
                                        if(snap.child("TRAIN NO").getValue().toString().contains(trainNo[0])){
                                            flag++;
                                            trainName[0] = snap.child("TRAIN NAME").getValue().toString();
                                        }
                                    }
                                    if(flag != 0){
                                        textToSpeech.speak("Train " + trainName[0] + " will arrive at platform " + platformNo[0], TextToSpeech.QUEUE_ADD, null, "hello");
                                        map = new HashMap();
                                        map.put("POSITION", "LEFT");
                                        map.put("TEXT", "Train " + trainName[0] + " will arrive at platform " + platformNo[0]);
                                        mArrayList.add(map);
                                        adapter.notifyDataSetChanged();
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
                    }
                    if(flag == 0){
                        //invalid
                        textToSpeech.speak("Sorry, invalid platform number", TextToSpeech.QUEUE_ADD, null, "hello");
                        map = new HashMap();
                        map.put("POSITION", "LEFT");
                        map.put("TEXT", "Sorry, invalid platform number");
                        mArrayList.add(map);
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

        else if(answer.contains("which platform") && answer.contains("arrive")){
            handler.removeCallbacks(runnable);
            final String[] trainName = new String[1];
            final String[] trainNo = new String[1];
            final String[] platformNo = {""};

            mRef.child("TRAIN TABLE").child("TRAINS").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int flag = 0;
                   for(DataSnapshot snap : snapshot.getChildren()){
                       if(answer.contains(snap.child("TRAIN NAME").getValue().toString().toLowerCase())){
                           flag++;
                           trainName[0] = snap.child("TRAIN NAME").getValue().toString();
                           trainNo[0] = snap.child("TRAIN NO").getValue().toString();

                           mRef.child("TRAIN TABLE").child("PLATFORM").addListenerForSingleValueEvent(new ValueEventListener() {
                               @Override
                               public void onDataChange(@NonNull DataSnapshot snapshot) {
                                   int flag = 0;
                                   for(DataSnapshot snap : snapshot.getChildren()){
                                       Log.e(TAG, "onDataChange: Platform " + snap );
                                       Log.e(TAG, "onDataChange: " + snap.child("TRAIN NO").getValue().toString() + " " + trainNo[0] );
                                       if(snap.child("TRAIN NO").getValue().toString().contains(trainNo[0])){
                                           flag++;
                                           platformNo[0] = snap.child("PLATFORM NO").getValue().toString();
                                           break;
                                       }
                                   }
                                   if(flag != 0){
                                       textToSpeech.speak("Train " + trainName[0] + " will arrive at platform " + platformNo[0], TextToSpeech.QUEUE_ADD, null, "hello");
                                       map = new HashMap();
                                       map.put("POSITION", "LEFT");
                                       map.put("TEXT", "Train " + trainName[0] + " will arrive at platform " + platformNo[0]);
                                       mArrayList.add(map);
                                       adapter.notifyDataSetChanged();
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

                           break;
                       }
                   }
                   if(flag == 0){
                       textToSpeech.speak("Sorry, that was an invalid train name", TextToSpeech.QUEUE_ADD, null, "hello");
                       map = new HashMap();
                       map.put("POSITION", "LEFT");
                       map.put("TEXT", "Sorry, that was an invalid train name");
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
                                        if (snap.child("TYPE").getValue().toString().contains("TO") && snap.child("STATION CODE").getValue().toString().toLowerCase().contains(cityCode.get(i))) {
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
/**
 * When will train ** reach **
 */
        else if(answer.contains("when will train") && answer.contains("arrive at")){
            handler.removeCallbacks(runnable);
            final String[] stationName = new String[1];
            final String[] trainNumber = new String[1];
            final String[] trainName = new String[1];
            final String[] stationCode = new String[1];
            final String[] timingsFound = {""};
            mRef.child("TRAIN TABLE").child("TRAINS").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int flag = 0;
                    for(final DataSnapshot snap : snapshot.getChildren()){
                        if(answer.contains(snap.child("TRAIN NAME").getValue().toString().toLowerCase())){
                            flag ++;
                            trainName[0] = snap.child("TRAIN NAME").getValue().toString();
                            trainNumber[0] = snap.child("TRAIN NO").getValue().toString();

                            mRef.child("TRAIN TABLE").child("STATIONS").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    int flag = 0;
                                    for(DataSnapshot snap : snapshot.getChildren()){
                                        if(answer.contains(snap.child("STATION NAME").getValue().toString().toLowerCase())){
                                            flag++;
                                            stationName[0] = snap.child("STATION NAME").getValue().toString();
                                            stationCode[0] = snap.child("STATION CODE").getValue().toString();

                                            mRef.child("TRAIN TABLE").child("ARRIVAL").addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    int flag = 0;
                                                    for(DataSnapshot snap : snapshot.getChildren()){
                                                        if(snap.child("TRAIN NO").getValue().toString().contains(trainNumber[0])
                                                            && snap.child("STATION CODE").getValue().toString().contains(stationCode[0])
                                                            && snap.child("TYPE").getValue().toString().contains("TO")){
                                                            flag++;
                                                            timingsFound[0] += snap.child("ARRIVAL").getValue().toString() + "\n";
                                                        }
                                                    }
                                                    if(flag == 0){
                                                        //timing not found
                                                        textToSpeech.speak("Sorry, no timings were found", TextToSpeech.QUEUE_ADD, null, "hello");
                                                        map = new HashMap();
                                                        map.put("POSITION", "LEFT");
                                                        map.put("TEXT", "Sorry, no timings were found");
                                                        mArrayList.add(map);
                                                        adapter.notifyDataSetChanged();

                                                    }
                                                    else{
                                                        textToSpeech.speak("Timings were found for " + trainName[0] + " for station " + stationName[0], TextToSpeech.QUEUE_ADD, null, "hello");
                                                        map = new HashMap();
                                                        map.put("POSITION", "LEFT");
                                                        map.put("TEXT", "Timings were found for " + trainName[0] + " for station " + stationName[0] + ":\n"
                                                        + timingsFound[0]);
                                                        mArrayList.add(map);
                                                        adapter.notifyDataSetChanged();
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {

                                                }
                                            });
                                            break;
                                        }
                                    }
                                    if(flag == 0){
                                        //station invalid
                                        textToSpeech.speak("Sorry, invalid station name", TextToSpeech.QUEUE_ADD, null, "hello");
                                        map = new HashMap();
                                        map.put("POSITION", "LEFT");
                                        map.put("TEXT", "Sorry, invalid station name");
                                        mArrayList.add(map);
                                        adapter.notifyDataSetChanged();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                            break;
                        }
                    }
                    if(flag == 0){
                        //Train invalid
                        textToSpeech.speak("Sorry, invalid train name", TextToSpeech.QUEUE_ADD, null, "hello");
                        map = new HashMap();
                        map.put("POSITION", "LEFT");
                        map.put("TEXT", "Sorry, invalid train name");
                        mArrayList.add(map);
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

        else if(answer.contains("when will train") && (answer.contains("leave") || answer.contains("start from"))){
            handler.removeCallbacks(runnable);
            final String[] stationName = new String[1];
            final String[] trainNumber = new String[1];
            final String[] trainName = new String[1];
            final String[] stationCode = new String[1];
            final String[] timingsFound = {""};
            mRef.child("TRAIN TABLE").child("TRAINS").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int flag = 0;
                    for(final DataSnapshot snap : snapshot.getChildren()){
                        if(answer.contains(snap.child("TRAIN NAME").getValue().toString().toLowerCase())){
                            flag ++;
                            trainName[0] = snap.child("TRAIN NAME").getValue().toString();
                            trainNumber[0] = snap.child("TRAIN NO").getValue().toString();

                            mRef.child("TRAIN TABLE").child("STATIONS").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    int flag = 0;
                                    for(DataSnapshot snap : snapshot.getChildren()){
                                        if(answer.contains(snap.child("STATION NAME").getValue().toString().toLowerCase())){
                                            flag++;
                                            stationName[0] = snap.child("STATION NAME").getValue().toString();
                                            stationCode[0] = snap.child("STATION CODE").getValue().toString();
                                            mRef.child("TRAIN TABLE").child("ARRIVAL").addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    int flag = 0;
                                                    for(DataSnapshot snap : snapshot.getChildren()){
                                                        if(snap.child("TRAIN NO").getValue().toString().contains(trainNumber[0])
                                                                && snap.child("STATION CODE").getValue().toString().contains(stationCode[0])){
                                                            flag++;
                                                            if(snap.child("DEPARTURE").getValue().toString().contains("NULL")){
                                                                timingsFound[0] += "12:47" + "\n";
                                                            }
                                                            else{
                                                                timingsFound[0] += snap.child("DEPARTURE").getValue().toString() + "\n";
                                                            }
                                                        }
                                                    }
                                                    if(flag == 0){
                                                        //timing not found
                                                        textToSpeech.speak("Sorry, no timings were found", TextToSpeech.QUEUE_ADD, null, "hello");
                                                        map = new HashMap();
                                                        map.put("POSITION", "LEFT");
                                                        map.put("TEXT", "Sorry, no timings were found");
                                                        mArrayList.add(map);
                                                        adapter.notifyDataSetChanged();

                                                    }
                                                    else{
                                                        textToSpeech.speak("Train " + trainName[0] + "will leave at the following times", TextToSpeech.QUEUE_ADD, null, "hello");
                                                        map = new HashMap();
                                                        map.put("POSITION", "LEFT");
                                                        map.put("TEXT", "Train " + trainName[0] + " will leave at the following times \n " + stationName[0] + ":\n"
                                                                + timingsFound[0]);
                                                        mArrayList.add(map);
                                                        adapter.notifyDataSetChanged();
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {

                                                }
                                            });
                                            break;
                                        }
                                    }
                                    if(flag == 0){
                                        //station invalid
                                        textToSpeech.speak("Sorry, invalid station name", TextToSpeech.QUEUE_ADD, null, "hello");
                                        map = new HashMap();
                                        map.put("POSITION", "LEFT");
                                        map.put("TEXT", "Sorry, invalid station name");
                                        mArrayList.add(map);
                                        adapter.notifyDataSetChanged();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                            break;
                        }
                    }
                    if(flag == 0){
                        //Train invalid
                        textToSpeech.speak("Sorry, invalid train name", TextToSpeech.QUEUE_ADD, null, "hello");
                        map = new HashMap();
                        map.put("POSITION", "LEFT");
                        map.put("TEXT", "Sorry, invalid train name");
                        mArrayList.add(map);
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }


        else if(answer.equals("hello") || answer.equals("hay") || answer.equals("sup") || answer.equals("hi") || answer.equals("hey")){

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

        else if(answer.contains("shut up") || answer.contains("f*** off")){
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
            isUserVoiceRecognized = false;

            Random random = new Random();
            random.setSeed(System.currentTimeMillis());
            int a = random.nextInt();

            random = new Random();
            random.setSeed(System.currentTimeMillis());
            int b = random.nextInt();

            a = b*a-b;

            Log.e(TAG, "inputTextFunction: " + a );
            if(a%2 == 0){
                if(a>0){
                    textToSpeech.speak("Sorry, What was that?", TextToSpeech.QUEUE_ADD, null, "invalid");
                    map = new HashMap();
                    map.put("POSITION", "LEFT");
                    map.put("TEXT", "Sorry, What was that?");
                    mArrayList.add(map);
                    adapter.notifyDataSetChanged();
                }

                else{
                    textToSpeech.speak("Sorry, Can you repeat it?", TextToSpeech.QUEUE_ADD, null, "invalid");
                    map = new HashMap();
                    map.put("POSITION", "LEFT");
                    map.put("TEXT", "Sorry, Can you repeat it?");
                    mArrayList.add(map);
                    adapter.notifyDataSetChanged();
                }

            }

            else if(a%3 == 0){
                textToSpeech.speak("Sorry, I didn't get you, can you please repeat", TextToSpeech.QUEUE_ADD, null, "invalid");
                map = new HashMap();
                map.put("POSITION", "LEFT");
                map.put("TEXT", "Sorry, I didn't get you, can you please repeat");
                mArrayList.add(map);
                adapter.notifyDataSetChanged();

            }
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
        else if(requestCode == 8080 && resultCode == Activity.RESULT_OK){

            Log.e(TAG, "onActivityResult: " + commandBox.getVisibility() );
            setForceOverrideFlag = false;
            commandBox.setVisibility(View.GONE);
            commandButton.setVisibility(View.GONE);
            commandBox.setEnabled(false);
            countDownTimer.start();

            keyWord = data.getStringExtra("KEY").toLowerCase();

            map = new HashMap();
            map.put("POSITION", "RIGHT");
            map.put("TEXT", keyWord);
            mArrayList.add(map);
            adapter.notifyDataSetChanged();

            if(keyWord.contains("yes")){
                map = new HashMap();
                map.put("POSITION", "LEFT");
                map.put("TEXT", "Okay, Sign into your account now");
                mArrayList.add(map);
                adapter.notifyDataSetChanged();

                textToSpeech.speak("Okay, Sign into your account now", TextToSpeech.QUEUE_FLUSH, null,"reg_sign_in");
            }

            else if(keyWord.contains("no")){
                map = new HashMap();
                map.put("POSITION", "LEFT");
                map.put("TEXT", "Okay, Scan this QR code to create an account");
                mArrayList.add(map);
                adapter.notifyDataSetChanged();

                textToSpeech.speak("Okay, Scan this QR code to create an account", TextToSpeech.QUEUE_FLUSH, null,"reg_qr_code");
            }

            else {
                map = new HashMap();
                map.put("POSITION", "LEFT");
                map.put("TEXT", "Sorry, that doesn't match your query");
                mArrayList.add(map);
                adapter.notifyDataSetChanged();

                textToSpeech.speak("Sorry, that doesn't match your query", TextToSpeech.QUEUE_FLUSH, null,"hello");
            }
        }

        else if(requestCode == 8080 && resultCode == Activity.RESULT_CANCELED){
            textToSpeech.speak("Sorry, something happened, please try again later", TextToSpeech.QUEUE_FLUSH, null,"hello");
            map = new HashMap();
            map.put("POSITION", "LEFT");
            map.put("TEXT", "Sorry, something happened, please try again later");
            mArrayList.add(map);
            adapter.notifyDataSetChanged();
        }

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy: Destroyed"  );
        speechRecognizer.cancel();
        speechRecognizer.destroy();
        countDownTimer.cancel();
        handler.removeCallbacks(runnable);
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.e(TAG, "onPause: Paused");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume: Resume" );
    }

    private int compareToSystemTime(String timeString){
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        String systemTime = format.format(new Date());

        Log.e(TAG, "compareTimeString: Time is being compared" + timeString + " " + systemTime);

        String[] inputTimeString = timeString.split(":");
        String[] systemTimeString = systemTime.split(":");
        int inputHour = Integer.parseInt(inputTimeString[0]);
        int inputMinute = Integer.parseInt(inputTimeString[1]);

        int systemHour = Integer.parseInt(systemTimeString[0]);
        int systemMinute = Integer.parseInt(systemTimeString[1]);

        if((systemHour - inputHour) < 0){
            return TIME_GREATER;
        }
        else if((systemHour - inputHour) > 0){
            return TIME_LESSER;
        }

        else{
            if((systemMinute - inputMinute) < 0){
                return TIME_GREATER;
            }

            else if((systemMinute - inputMinute) > 0){
                return TIME_LESSER;
            }

            else{
                return TIME_EQUAL;
            }
        }
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