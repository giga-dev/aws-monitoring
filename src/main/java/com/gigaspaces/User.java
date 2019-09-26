package com.gigaspaces;

import java.util.Objects;

@SuppressWarnings("WeakerAccess")
public class User{
    private String name;
    private String email;
    private boolean monitored;
    private boolean admin;
    private  Tz timezone;
    private boolean notified;


    public User() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isMonitored() {
        return monitored;
    }

    public void setMonitored(boolean monitored) {
        this.monitored = monitored;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public Tz getTimezone() {
        return timezone;
    }

    public void setTimezone(Tz timezone) {
        this.timezone = timezone;
    }

    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    public boolean is(String name) {
        return name.equals(getEmail()) || getEmail().startsWith(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return isMonitored() == user.isMonitored() &&
                isAdmin() == user.isAdmin() &&
                isNotified() == user.isNotified() &&
                getName().equals(user.getName()) &&
                getEmail().equals(user.getEmail()) &&
                getTimezone() == user.getTimezone();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getEmail(), isMonitored(), isAdmin(), getTimezone(), isNotified());
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", monitored=" + monitored +
                ", admin=" + admin +
                ", timezone=" + timezone +
                ", notified=" + notified +
                '}';
    }

}