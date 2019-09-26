package com.gigaspaces.actions;

import com.gigaspaces.Instance;
import com.gigaspaces.User;

import java.util.Calendar;

public class StopAction extends Action {
    private final Instance instance;
    private final Calendar time;
    private User subject;

    public StopAction(Instance instance, Calendar time, User subject) {
        super();
        this.instance = instance;
        this.time = time;
        this.subject = subject;
    }

    public Instance getInstance() {
        return instance;
    }

    @SuppressWarnings("unused")
    public Calendar getTime() {
        return time;
    }

    public User getSubject() {
        return subject;
    }

    @Override
    public String toString() {
        return "StopAction{" +
                "instance=" + instance +
                ", time=" + formatTime(time) +
                '}';
    }
}
