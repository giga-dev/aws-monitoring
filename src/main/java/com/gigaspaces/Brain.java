package com.gigaspaces;

import com.gigaspaces.actions.Action;
import com.gigaspaces.actions.NotifyBeforeStopAction;
import com.gigaspaces.actions.StopAction;
import com.gigaspaces.actions.WaitAction;
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
                        logger.info("put notify before stop action {}", action);
                        actions.put(instance, action);
                        res.add(action);
                    }else if(action instanceof  NotifyBeforeStopAction){
                        Calendar notifyTime = ((NotifyBeforeStopAction) action).getTime();
                        long seconds = (notifyTime.getTimeInMillis() - time.getTimeInMillis()) / 1000;
                        long minuets = (int)(seconds / 60);
                        if(15 <= Math.abs(minuets)) {
                            action = new StopAction(instance, time, suspect);
                            logger.info("put stop action {}", action);
                            actions.put(instance, action);
                            res.add(action);
                        }else{
                            logger.info("NotifyBeforeStopAction has more {}  minuets for action {}",  15 - Math.abs(minuets), action);
                        }
                    }else if (action instanceof WaitAction){
                        WaitAction waitAction = (WaitAction) action;
                        logger.info("wait action {} in queue", waitAction);
                        Calendar currentTime = Calendar.getInstance();
                        if(waitAction.getUntil().getTimeInMillis() < currentTime.getTimeInMillis()){
                            logger.info("Disposing wait action {}", action);
                            actions.remove(waitAction.getInstance());
                        }else{
                            long milis = Math.abs(waitAction.getUntil().getTimeInMillis() - currentTime.getTimeInMillis());
                            long seconds = milis / 1000;
                            long minuets = (int)(seconds / 60);
                            logger.info("Wait action {} has more {} minuets", action, minuets);
                        }
                    }
                }
            }
        }
        logger.info("\n");
        logger.info("\n");
        logger.info("analyzer res has [{}] elements", res.size());
        for(int i = 0; i < res.size(); ++i){
            logger.info("- res [{}] is: {}", i, res);
        }
        logger.info("\n");
        logger.info("analyzer actions has [{}] values", actions.size());
        int i = 0;
        for (Action value : actions.values()) {
            logger.info("- actions [{}] is: {}", i, value);
            i+= 1;
        }
        logger.info("\n");
        logger.info("\n");
        return res;
    }

    private List<Tz> computeOutOfOfficeTimeZone(Calendar time) {
        ArrayList<Tz> res = new ArrayList<>();
        int dow = time.get(Calendar.DAY_OF_WEEK);
        if((dow == Calendar.FRIDAY) || (dow == Calendar.SATURDAY) || (17 <= time.get(Calendar.HOUR_OF_DAY) || (time.get(Calendar.HOUR_OF_DAY) <= 7))){
            res.add(Tz.Israel);
        }
        if((dow == Calendar.SATURDAY) || (dow == Calendar.SUNDAY) | (17 <= time.get(Calendar.HOUR_OF_DAY) || (time.get(Calendar.HOUR_OF_DAY) <= 7))){
            res.add(Tz.EU);
        }
        if((dow == Calendar.SATURDAY) || (dow == Calendar.SUNDAY) || ((time.get(Calendar.HOUR_OF_DAY) <= 5  && 16 <= time.get(Calendar.HOUR_OF_DAY)))){
            res.add(Tz.US);
        }
        return res;
    }


    @SuppressWarnings("WeakerAccess")
    public void setAction(Instance instance, Action action){
        logger.info("setAction {} {}", instance, action);
        actions.put(instance, action);
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null)
            return false;
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA)
                && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }
}
