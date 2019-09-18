package com.gigaspaces.actions;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public abstract class Action {
    String formatTime(Calendar cal){
        return new SimpleDateFormat( "yyyy/MM/dd HH:mm:ss" ).format(cal.getTime());
    }
}
