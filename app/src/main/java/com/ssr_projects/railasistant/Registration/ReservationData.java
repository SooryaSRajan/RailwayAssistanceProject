package com.ssr_projects.railasistant.Registration;

public class ReservationData {

    private String stationName;
    private String trainNumber;
    private String trainName;
    private String trainSeatType;
    private int noOfSeats;
    private int costOfBooking;

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public void setTrainNumber(String trainNumber) {
        this.trainNumber = trainNumber;
    }

    public void setTrainName(String trainName) {
        this.trainName = trainName;
    }

    public void setTrainSeatType(String trainSeatType) {
        this.trainSeatType = trainSeatType;
    }

    public void setNoOfSeats(int noOfSeats) {
        this.noOfSeats = noOfSeats;
    }

    public void setCostOfBooking(int costOfBooking) {
        this.costOfBooking = costOfBooking;
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

    public String getTrainSeatType() {
        return trainSeatType;
    }

    public int getNoOfSeats() {
        return noOfSeats;
    }

    public int getCostOfBooking() {
        return costOfBooking;
    }
}
