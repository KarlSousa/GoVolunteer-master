package com.goa.spotfix.govolunteer;

public class User {
    private String name;
    private String email;
    private String photoURL;
    private int spotfixes_created;
    private int spotfixes_attented;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String name, String email, String photoURL, int spotfixes_created, int spotfixes_attented) {
        this.name = name;
        this.email = email;
        this.photoURL = photoURL;
        this.spotfixes_created = spotfixes_created;
        this.spotfixes_attented = spotfixes_attented;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getSpotfixes_created() {
        return spotfixes_created;
    }

    public void setSpotfixes_created(int spotfixes_created) {
        this.spotfixes_created = spotfixes_created;
    }

    public int getSpotfixes_attented() {
        return spotfixes_attented;
    }

    public void setSpotfixes_attented(int spotfixes_attented) {
        this.spotfixes_attented = spotfixes_attented;
    }

    public String getPhotoURL() {
        return photoURL;
    }

    public void setPhotoURL(String photoURL) {
        this.photoURL = photoURL;
    }

    @Override
    public String toString() {
        return email + " " + photoURL;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
