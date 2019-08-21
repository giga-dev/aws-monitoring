package com.gigaspaces;

import com.gigaspaces.actions.Action;
import com.gigaspaces.actions.NotifyBeforeStopAction;
import com.gigaspaces.actions.StopAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

class Brain {
    final static Logger logger = LoggerFactory.getLogger(Brain.class);

    private Map<Instance, Action> actions = new HashMap<>();

    private Calendar currentDay;
    private List<Suspect> suspects;

    Brain(Calendar currentDay, List<Suspect> suspects) {
        this.currentDay = currentDay;
        this.suspects = suspects;
    }

    List<Action> analyze(Calendar time, List<Instance> snapshot){
        if(!isSameDay(time, currentDay)){
            actions = new HashMap<>();
            currentDay = time;
        }
        List<Action> res = new ArrayList<>();

        List<Tz> outOfOffinceTZ = computeOutOfOfficeTimeZone(time);
        logger.info("analyzing at {} out of office zones are {}", new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(time.getTime()), outOfOffinceTZ);
        for (Suspect suspect : suspects) {
            if(outOfOffinceTZ.contains(suspect.getTimezone())){
                for (Instance instance : snapshot) {
                    if(instance.isSpot()){
                        continue;
                    }
                    if(!suspect.getName().equals(instance.getEffectiveUserName())){
                        continue;
                    }
                    Action action = actions.get(instance);
                    if(action == null){
                        action = new NotifyBeforeStopAction(instance, time, suspect);
                        actions.put(instance, action);
                        res.add(action);
                    }else if(action instanceof  NotifyBeforeStopAction){
                        Calendar notifyTime = ((NotifyBeforeStopAction) action).getTime();
                        long seconds = (notifyTime.getTimeInMillis() - time.getTimeInMillis()) / 1000;
                        long minuets = (int)(seconds / 60);
//                        int hours = (int) (minuets / 60);
//                        if(1 < Math.abs(hours)) {
                        if(5 <= Math.abs(minuets)) {
                            action = new StopAction(instance, time, suspect);
                            actions.put(instance, action);
                            res.add(action);
                        }
                    }
                }
            }
        }
        return res;
    }

    private List<Tz> computeOutOfOfficeTimeZone(Calendar time) {
        ArrayList<Tz> res = new ArrayList<>();
        int dow = time.get(Calendar.DAY_OF_WEEK);
        if((dow == Calendar.FRIDAY) || (dow == Calendar.SATURDAY) || 18 <= time.get(Calendar.HOUR_OF_DAY)){
            res.add(Tz.Israel);
        }
        if(((dow == Calendar.SATURDAY) || (dow == Calendar.SUNDAY) || 18 <= time.get(Calendar.HOUR_OF_DAY))){
            res.add(Tz.EU);
        }
        if((dow == Calendar.SATURDAY) || (dow == Calendar.SUNDAY) || ((time.get(Calendar.HOUR_OF_DAY) <= 5  && 16 <= time.get(Calendar.HOUR_OF_DAY)))){
            res.add(Tz.US);
        }
        return res;
    }


    public void setAction(Instance instance, Action action){
        actions.put(instance, action);
    }

    public void removeAction(Instance instance){
        actions.remove(instance);
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null)
            return false;
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA)
                && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }
}
