package com.goa.spotfix.govolunteer;

import java.io.Serializable;
import java.util.HashMap;

public class Spotfix implements Serializable {

    private int peopleRequired, noOfPeopleJoined;
    private double latitude, longitude;
    private String address;
    private String placeName;
    private String date;
    private String time;
    private String photoURL;
    private String uploaderId;
    private String spotfixId;
    private HashMap<String, String> joinedIds = new HashMap<>();

    public Spotfix() {
        // Default constructor required for calls to DataSnapshot.getValue(Spotfix.class)
    }

    public Spotfix(int noOfPeopleRequired, int noOfPeopleJoined, double latitude, double longitude, String address, String place, String date, String time, String imageURL, String uploaderId, String spotfixId, HashMap<String, String> joinedIds) {
        this.peopleRequired = noOfPeopleRequired;
        this.noOfPeopleJoined = noOfPeopleJoined;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.placeName = place;
        this.date = date;
        this.time = time;
        this.photoURL = imageURL;
        this.uploaderId = uploaderId;
        this.spotfixId = spotfixId;
        this.joinedIds = joinedIds;
    }

    public int getPeopleRequired() {
        return peopleRequired;
    }

    public void setPeopleRequired(int peopleRequired) {
        this.peopleRequired = peopleRequired;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getPhotoURL() {
        return photoURL;
    }

    public void setPhotoURL(String photoURL) {
        this.photoURL = photoURL;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(String uploaderId) {
        this.uploaderId = uploaderId;
    }

    public int getNoOfPeopleJoined() {
        return noOfPeopleJoined;
    }

    public void setNoOfPeopleJoined(int noOfPeopleJoined) {
        this.noOfPeopleJoined = noOfPeopleJoined;
    }

    public String getSpotfixId() {
        return spotfixId;
    }

    public void setSpotfixId(String spotfixId) {
        this.spotfixId = spotfixId;
    }

    public HashMap<String, String> getJoinedIds() {
        return joinedIds;
    }

    public void setJoinedIds(HashMap<String, String> joinedIds) {
        this.joinedIds = joinedIds;
    }
}
