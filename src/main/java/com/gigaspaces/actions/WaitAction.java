package com.gigaspaces.actions;

import com.gigaspaces.Instance;
import io.vavr.control.Option;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class WaitAction extends Action  {
    private final Instance instance;
    private Calendar until;
    private final Option<Action> after;

    public WaitAction(Instance instance, Calendar until, Option<Action> after) {
        this.instance = instance;
        this.until = until;
        this.after = after;
    }

    public Instance getInstance() {
        return instance;
    }

    public Calendar getUntil() {
        return until;
    }

    public Option<Action> getAfter() {
        return after;
    }

    @Override
    public String toString() {
        return "WaitAction{" +
                "instance=" + instance +
                "until=" +  formatTime(until) +
                ", after=" + after +
                '}';
    }
}
