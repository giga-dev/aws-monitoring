package com.gigaspaces;

import com.gigaspaces.actions.Action;
import com.gigaspaces.actions.NotifyBeforeStopAction;
import com.gigaspaces.actions.StopAction;
import com.gigaspaces.actions.WaitAction;
import io.vavr.collection.HashSet;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudtrail.model.Event;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;

import javax.mail.MessagingException;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class Loop {
    final static Logger logger = LoggerFactory.getLogger(Loop.class);

    public static void main(String[] args) throws InterruptedException{
        InstancesService instancesService = new InstancesService();
        List<com.gigaspaces.Instance> snapshot = new ArrayList<>();
        CouldTrailEventsReader couldTrailEventsReader = new CouldTrailEventsReader();
        List<Suspect> suspects = Arrays.asList(new Suspect("denysn", "denysn@gigaspaces.com", Tz.EU)
                , new Suspect("yoram.weinreb", "yoram.weinreb@gigaspaces.com", Tz.Israel)
                , new Suspect("aharon.moll", "aharon.moll@gigaspaces.com", Tz.Israel)
        );
        Brain brain = new Brain(Calendar.getInstance(), suspects);
        logger.info("starting loop");
        //noinspection InfiniteLoopStatement
        while (true) {
            try {
                List<com.gigaspaces.Instance> found = new ArrayList<>();
                List<AWSData<Reservation>> runningInstances = instancesService.describeAllRunningInstances();
                for (AWSData<Reservation> runningInstance : runningInstances) {
                    for (Instance instance : runningInstance.getData().instances()) {
                        find(snapshot, instance.instanceId()).onEmpty(() -> {
                            Option<String> name = Stream.ofAll(instance.tags()).find(t -> "Name".equals(t.key())).map(Tag::value);
                            Option<String> groupName = Stream.ofAll(instance.tags()).find(t -> "spotinst:aws:ec2:group:id".equals(t.key())).map(Tag::value);
                            Option<String> stackId = Stream.ofAll(instance.tags()).find(t -> "aws:cloudformation:stack-id".equals(t.key())).map(Tag::value);
                            logger.info("getting details of instance {} ({})", name, instance.instanceId());
                            Option<Event> started = couldTrailEventsReader.startEvent(runningInstance.getProfile(), runningInstance.getRegion(), instance);
                            if (started.isDefined()) {
                                started.map(s -> {
                                    boolean isSpot = s.username().startsWith("spotinst.session.");
                                    String effectiveUserName = isSpot ? getEffectiveUserName(couldTrailEventsReader, runningInstance, instance, groupName).getOrElse(s.username()) : s.username();
                                    if("AutoScaling".equals(effectiveUserName) && stackId.isDefined()){
                                        effectiveUserName = couldTrailEventsReader.findUserNameByStackId(runningInstance.getProfile(), runningInstance.getRegion(), stackId.get()).getOrElse(effectiveUserName);
                                    }
                                    return new com.gigaspaces.Instance(runningInstance.getProfile(), runningInstance.getRegion(), name, instance.instanceId(), s.username(), s.eventName(), s.eventTime(), instance.instanceType().toString(), isSpot, effectiveUserName);
                                }).forEach(found::add);
                            }
                        }).map(found::add);
                    }
                }

//                CreateStack
//                "aws:cloudformation:stack-id"
//                  "arn:aws:cloudformation:eu-west-1:535075449278:stack/eksctl-eks-cluster-nodegroup-ng-be39c976/4740e2c0-b67e-11e9-90ef-02a08d23d630"

                Collections.sort(snapshot);
//                for (com.gigaspaces.Instance instance : snapshot) {
//                    logger.info("instance: {}", instance);
//                }
                Diff diff = Diff.create(HashSet.ofAll(snapshot), HashSet.ofAll(found));
                if (diff.wasModified()) {
                    HTMLTemplate template = new HTMLTemplate(diff);
                    String htmlBody = template.formatHTMLBody();
//                    logger.info("htmlbody {}", htmlBody);
                    try {
                        String subject = String.format("AWS InstancesService change r%d +%d -%d", diff.getRunningSize(), diff.getAddedSize(), diff.getRemovedSize());
                        io.vavr.collection.List<String> recipients = io.vavr.collection.List.of("barak.barorion@gigaspaces.com");
                        EmailNotifications.send(subject, htmlBody, recipients);
                        logger.info("Email sent to {} " , recipients);
                    } catch (IOException | MessagingException e) {
                        logger.error(e.toString(), e);
                    }
                    snapshot = found;
//                } else {
//                    logger.info("Diff was not modified");
                }
                List<Action> actions = brain.analyze(Calendar.getInstance(), snapshot);
                if(!actions.isEmpty()){
                    logger.info("Executing actions {}", actions);
                    for (Action action : actions) {
                        evaluateAction(action, instancesService, brain);
                    }
                }
            }catch(Exception e){
                logger.error(e.toString(), e);
            }
            Thread.sleep( 1000 * 60);
        }
    }

    private static void evaluateAction(Action action, InstancesService instancesService, Brain brain) {
        if(action instanceof NotifyBeforeStopAction){
            evaluate((NotifyBeforeStopAction) action);
        }else if (action instanceof StopAction){
            evaluate((StopAction)action, instancesService, brain);
        }else if (action instanceof WaitAction){
            evaluate((WaitAction)action, brain);
        }
    }

    private static void evaluate(WaitAction action, Brain brain) {
        Calendar currentTime = Calendar.getInstance();
        if(action.getUntil().getTimeInMillis() < currentTime.getTimeInMillis()){
            logger.info("Executing wait action {}", action);
            action.getAfter().onEmpty(() -> brain.removeAction(action.getInstance())).forEach(ac -> brain.setAction(action.getInstance(), ac));
        }
    }

    private static void evaluate(StopAction action, InstancesService instancesService, Brain brain) {
        logger.info("Executing stop action {}", action);
        // first peek newman mailbox of email with the subject 'Please keep my instance $instance.instanceId running'
        if(!new EmailNotifications().shouldKeepInstance(action.getInstance().getInstanceId())) {
            logger.info("Stopping instance {}", action.getInstance().getInstanceId());
            instancesService.stopInstance(action.getInstance());
            try {
                String subject = String.format("[IMPORTANT] AWS instance (%s) started by (%s) was shutdown by the brain", action.getInstance().getInstanceId(), action.getSubject().getName());
                io.vavr.collection.List<String> recipients = io.vavr.collection.List.of("barak.barorion@gigaspaces.com");
                StoppedHTMLTemplate template = new StoppedHTMLTemplate(action);
                String htmlBody = template.formatHTMLBody();
                EmailNotifications.send(subject, htmlBody, recipients);
                logger.info("A warn email was sent to Email sent to {} ", recipients);
            } catch (IOException | MessagingException e) {
                logger.error(e.toString(), e);
            }
        }else{
            logger.info("Not Stopping instance {} started by {} because he bagged for his life.", action.getInstance().getInstanceId(), action.getSubject().getName());
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.HOUR_OF_DAY, 1);
            brain.setAction(action.getInstance(), new WaitAction(action.getInstance(), cal, Option.none()));
        }
    }

    private static void evaluate(NotifyBeforeStopAction action) {
        logger.info("Executing NotifyBeforeStopAction action {}", action);
        try {
            String subject = String.format("[URGENT] Alert before stopping AWS instance %s", action.getInstance().getInstanceId());
            io.vavr.collection.List<String> recipients = io.vavr.collection.List.of("barak.barorion@gigaspaces.com", action.getSubject().getEmail());
//            io.vavr.collection.List<String> recipients = io.vavr.collection.List.of("barak.barorion@gigaspaces.com");
            WarnHTMLTemplate template = new WarnHTMLTemplate(action);
            String htmlBody = template.formatHTMLBody();
            EmailNotifications.send(subject, htmlBody, recipients);
            logger.info("A warn email was sent to Email sent to {} " , recipients);
        } catch (IOException | MessagingException e) {
            logger.error(e.toString(), e);
        }


    }

    private static Option<String> getEffectiveUserName(CouldTrailEventsReader couldTrailEventsReader, AWSData<Reservation> runningInstance, Instance instance, Option<String> groupName) {
        return groupName.flatMap(gn -> couldTrailEventsReader.findStartOldestEvent(runningInstance.getProfile(), runningInstance.getRegion(), instance)
                .flatMap(e -> readUserFromSpotinstsHistory(runningInstance.getProfile(), runningInstance.getRegion(), gn, e.eventTime())));
    }

    private static Option<String> readUserFromSpotinstsHistory(String profile, Region region, String groupName, Instant eventTime) {
        try {
            return new SpotInsts().readUserHistory(profile, region, groupName, eventTime);
        } catch (Exception e) {
            logger.error(e.toString(), e);
            return Option.none();
        }
    }

    private static Option<com.gigaspaces.Instance> find(List<com.gigaspaces.Instance> snapshot, String instanceId) {
        return io.vavr.collection.List.ofAll(snapshot).find(i -> i.getInstanceId().equals(instanceId));
    }

}
