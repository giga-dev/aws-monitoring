package com.gigaspaces;


public class Suspect {
    private String name;
    private String email;
    private Tz timezone;

    public Suspect(String name, String email, Tz timezone) {
        this.name = name;
        this.email = email;
        this.timezone = timezone;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Tz getTimezone() {
        return timezone;
    }
}
