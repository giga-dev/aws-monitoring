package com.gigaspaces.actions;

import com.gigaspaces.Instance;

import java.util.Calendar;

public class StopAction extends Action {
    private final Instance instance;
    private final Calendar time;

    public StopAction(Instance instance, Calendar time) {
        super();
        this.instance = instance;
        this.time = time;
    }

    public Instance getInstance() {
        return instance;
    }

    public Calendar getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "StopAction{" +
                "instance=" + instance +
                ", time=" + time +
                '}';
    }
}
