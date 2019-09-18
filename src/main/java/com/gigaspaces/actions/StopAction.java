package com.gigaspaces.actions;

import com.gigaspaces.Instance;
import com.gigaspaces.Suspect;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class StopAction extends Action {
    private final Instance instance;
    private final Calendar time;
    private Suspect subject;

    public StopAction(Instance instance, Calendar time, Suspect subject) {
        super();
        this.instance = instance;
        this.time = time;
        this.subject = subject;
    }

    public Instance getInstance() {
        return instance;
    }

    public Calendar getTime() {
        return time;
    }

    public Suspect getSubject() {
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
