package com.gigaspaces.actions;

import com.gigaspaces.Instance;
import com.gigaspaces.Suspect;
import com.gigaspaces.actions.Action;

import java.util.Calendar;

public class NotifyBeforeStopAction extends Action {
    private final Instance instance;
    private final Calendar time;
    private Suspect subject;

    public NotifyBeforeStopAction(Instance instance, Calendar time, Suspect subject) {
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
        return "NotifyBeforeStopAction{" +
                "instance=" + instance +
                ", time=" + time +
                '}';
    }
}
