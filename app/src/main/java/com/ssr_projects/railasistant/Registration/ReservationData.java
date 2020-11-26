package com.ssr_projects.railasistant.Registration;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ReservationData implements Serializable {

    private String stationName;
    private String trainNumber;
    private String trainName;
    private String trainType;
    private String trainSeatType;
    private int noOfSeats;
    private int costOfBooking;
    private String distanceOfTrain;
    private String trainTime;
    private String reservationTime;
    private String trainKey;

    public ReservationData(String stationName, String trainNumber, String trainName, String trainType, String trainSeatType, int noOfSeats, int costOfBooking, String distanceOfTrain, String trainTime, String trainKey) {
        this.stationName = stationName;
        this.trainNumber = trainNumber;
        this.trainName = trainName;
        this.trainType = trainType;
        this.trainSeatType = trainSeatType;
        this.noOfSeats = noOfSeats;
        this.costOfBooking = costOfBooking;
        this.distanceOfTrain = distanceOfTrain;
        this.trainTime = trainTime;
        this.trainKey = trainKey;
        reservationTime = getTime();
    }

    public String getTrainKey() {
        return trainKey;
    }

    public String getTrainTime() {
        return trainTime;
    }

    public String getReservationTime() {
        return reservationTime;
    }

    public String getDistanceOfTrain() {
        return distanceOfTrain;
    }

    public String getStationName() {
        return stationName;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public String getTrainName() {
        return trainName;
    }

    public String getTrainType() {
        return trainType;
    }

    public String getTrainSeatType() {
        return trainSeatType;
    }

    public int getNoOfSeats() {
        return noOfSeats;
    }

    public int getCostOfBooking() {
        return costOfBooking;
    }

    public String getTime() {
        Date currentTime = Calendar.getInstance().getTime();
        DateFormat timeFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm");
        return timeFormat.format(currentTime);
    }

}
