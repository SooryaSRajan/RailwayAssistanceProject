package com.ssr_projects.railasistant.Registration;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.ssr_projects.railasistant.R;

public class ReservationTicketActivity extends AppCompatActivity {
private final String TAG = "ReservationTicket";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation_ticket);

        Intent i = getIntent();
        ReservationData data = (ReservationData) i.getSerializableExtra("CLASS");

        Log.e(TAG, "onCreate: " + data.getStationName());


    }
}