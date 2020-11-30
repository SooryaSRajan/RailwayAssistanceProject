package com.ssr_projects.railasistant.Registration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ssr_projects.railasistant.MainActivity;
import com.ssr_projects.railasistant.R;

import java.util.Objects;
import java.util.UUID;

public class ReservationTicketActivity extends AppCompatActivity {
    private static final long TIME = 1000 ;
    private final String TAG = "ReservationTicket";
    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("TRAIN TABLE");
    int countDown = 20;
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation_ticket);

        TextView trainName = findViewById(R.id.train_name);
        final TextView userName = findViewById(R.id.user_name);
        final TextView cardNumber = findViewById(R.id.card_number);
        TextView toStation = findViewById(R.id.to_station);
        TextView trainNo = findViewById(R.id.train_no);
        TextView trainCategory = findViewById(R.id.train_category);
        TextView trainSeatType = findViewById(R.id.train_seat_type);
        TextView trainNoOfSeats = findViewById(R.id.train_no_of_seats);
        TextView arrivalTime = findViewById(R.id.arrival_time);
        TextView departureTime = findViewById(R.id.departure_time);
        TextView reservationTime = findViewById(R.id.reservation_time);
        TextView transactionId = findViewById(R.id.trancaction_id);
        TextView amountPaid = findViewById(R.id.total_amount);

        Intent i = getIntent();
        final ReservationData data = (ReservationData) i.getSerializableExtra("CLASS");
        data.setUserId(FirebaseAuth.getInstance().getUid());

        assert data != null;
        Log.e(TAG, "onCreate: " + data.getStationName());
        databaseReference.child("RESERVATION").push().setValue(data);
        String trainKey = data.getTrainKey();
        String seatType = null;

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            public void run() {
                if (countDown != 0) {
                    handler.postDelayed(this, TIME);
                }
                else{
                    if(FirebaseAuth.getInstance().getCurrentUser() != null)
                    FirebaseAuth.getInstance().signOut();
                    Toast.makeText(ReservationTicketActivity.this, "Timed Out", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ReservationTicketActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
                countDown--;
            }
        };


        handler.postDelayed(runnable, TIME);

        FirebaseDatabase.getInstance().getReference().child("USERS").child("PASSENGER").child(FirebaseAuth.getInstance().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.e(TAG, "onDataChange: " + snapshot );
                String userNameString = null;
                String cardNumberString = null;
                for(DataSnapshot snap : snapshot.getChildren()) {
                    Log.e(TAG, "onDataChange: " + snap);
                    if(snap.getKey().contains("FIRST")){
                        userNameString = snap.getValue().toString();
                    }
                    else if(snap.getKey().contains("LAST")){
                        userNameString = userNameString + " " + snap.getValue().toString();
                    }
                    else if(snap.getKey().contains("CARD")){
                     cardNumberString = snap.getValue().toString();
                    }
                    userName.setText(userNameString);
                    cardNumber.setText(cardNumberString);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

       try {
          toStation.setText(data.getStationName() + "");
          trainNoOfSeats.setText(data.getNoOfSeats() + "");
      }
      catch (Exception e){
          Log.e(TAG, "onCreate: " + e.toString() );
      }
        trainName.setText(data.getTrainName());
        trainNo.setText(data.getTrainNumber());
        trainCategory.setText(data.getTrainType());
        trainSeatType.setText(data.getTrainSeatType());
        arrivalTime.setText(data.getTrainTime());
        reservationTime.setText(data.getReservationTime());
        amountPaid.setText(data.getCostOfBooking() + "");
        String reservationTransactionID = UUID.randomUUID().toString();
        transactionId.append(reservationTransactionID);
        departureTime.setText(data.getDepartureTime());

        databaseReference = databaseReference.child("TRAINS").child(trainKey);

        if(data.getTrainSeatType().contains("1AC")){
            seatType = "A_1AC";
        }

        else if(data.getTrainSeatType().contains("2AC")){
            seatType = "A_2AC";
        }


        else if(data.getTrainSeatType().contains("3AC")){
            seatType = "A_3AC";
        }


        else if(data.getTrainSeatType().contains("CC")){
            seatType = "A_CC";
        }


        else if(data.getTrainSeatType().contains("FC")){
            seatType = "A_FC";
        }


        else if(data.getTrainSeatType().contains("ST")){
            seatType = "A_ST";
        }

        final String finalSeatType = seatType;

        if(seatType != null)
        databaseReference.child(seatType).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.e(TAG, "onDataChange: No of seats: " + snapshot.getValue() );
                databaseReference.child(finalSeatType).setValue(Integer.parseInt(Objects.requireNonNull(snapshot.getValue()).toString()) - data.getNoOfSeats());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }
}