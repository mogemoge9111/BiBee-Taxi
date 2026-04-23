package com.example.bibeetaxi;

public class UserProfile {
    public String name, surname, email, userType, photoUrl;
    public double rating;

    public UserProfile() {}

    public UserProfile(String name, String surname, String email, String userType) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.userType = userType;
        this.rating = 0.0;
    }
}