package com.gigaspaces;

import com.gigaspaces.actions.*;
import io.vavr.collection.HashSet;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Responder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.servicemetadata.EmailServiceMetadata;
import software.amazon.awssdk.services.cloudtrail.model.Event;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;

import javax.mail.Flags;
import javax.mail.MessagingException;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class Loop {
    @SuppressWarnings("WeakerAccess")
    final static Logger logger = LoggerFactory.getLogger(Loop.class);
    private Users users;
    private InstancesService instancesService;

    private Loop(Users users){
        this.users = users;
    }
    private io.vavr.collection.List<String> notified(){
        return users.notified().map(User::getEmail);
    }
    private void run() throws InterruptedException {
        instancesService = new InstancesService();
        List<com.gigaspaces.Instance> snapshot = new ArrayList<>();
        CouldTrailEventsReader couldTrailEventsReader = new CouldTrailEventsReader();

        io.vavr.collection.List<User> suspects = users.monitored();
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

                Collections.sort(snapshot);
                Diff diff = Diff.create(HashSet.ofAll(snapshot), HashSet.ofAll(found));
                if (diff.wasModified()) {
                    HTMLTemplate template = new HTMLTemplate(diff);
                    String htmlBody = template.formatHTMLBody();
                    try {
                        String subject = String.format("AWS InstancesService change r%d +%d -%d", diff.getRunningSize(), diff.getAddedSize(), diff.getRemovedSize());
                        EmailNotifications.send(subject, htmlBody, notified());
                        logger.info("Email sent to {} " , notified());
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
                List<com.gigaspaces.Instance> finalSnapshot = snapshot;
                readAdminCommands(snapshot).forEach(cmd -> execute(cmd, finalSnapshot));

            }catch(Exception e){
                logger.error(e.toString(), e);
            }
            Thread.sleep( 1000 * 60);
        }
    }

    private void execute(AdminCommand cmd, List<com.gigaspaces.Instance> snapshot) {
        try{
            logger.info("Executing admin command {}", cmd);
            if(cmd instanceof ListAdminCommand) {
                Diff diff = new Diff(io.vavr.collection.List.ofAll(snapshot));
                HTMLTemplate template = new HTMLTemplate(diff);
                String htmlBody = template.formatHTMLBody();
                try {
                    String subject = String.format("[AWS reply] list %d", diff.getRunningSize());
                    EmailNotifications.send(subject, htmlBody, io.vavr.collection.List.of(cmd.getRequester()));
                    logger.info("Email sent to {} ", notified());
                } catch (IOException | MessagingException e) {
                    logger.error(e.toString(), e);
                }
            }else if(cmd instanceof StopAdminCommand){
                StopAdminCommand stopAdminCommand = (StopAdminCommand)cmd;
                logger.info("Stopping instance {}", stopAdminCommand.getInstance());
                instancesService.stopInstance(stopAdminCommand.getInstance());
                try {
                    String subject = String.format("[AWS reply] stop %s", stopAdminCommand.getInstance().getInstanceId());
                    io.vavr.collection.List<String> recipients = notified();
                    String htmlBody = "";
                    EmailNotifications.send(subject, htmlBody, recipients);
                    logger.info("A stop email was sent to Email sent to {} ", recipients);
                } catch (IOException | MessagingException e) {
                    logger.error(e.toString(), e);
                }
            }

        }catch (Exception e){
            logger.error(e.toString(), e);
        }
    }

    private io.vavr.collection.List<AdminCommand> readAdminCommands(final List<com.gigaspaces.Instance> snapshot) {
        EmailNotifications en = new EmailNotifications();
        io.vavr.collection.Set<String> admins = HashSet.ofAll(users.admin().map(User::getEmail));
        return en.processEmails(message -> {
            try {
                String email = extractEmail(message.getFrom()[0].toString());
                if (admins.contains(email)) {
                    message.setFlag(Flags.Flag.DELETED, true);
                    return createAdminCommand(message.getSubject().trim(), email, snapshot);
                }else{
                    return Option.none();
                }
            }catch(Exception e){
                logger.error(e.toString(), e);
                return Option.none();
            }
        });
    }

    private Option<AdminCommand> createAdminCommand(String request, String email, List<com.gigaspaces.Instance> snapshot) {
        if("list".equals(request)) {
            return Option.some(new ListAdminCommand(email));
        }else if(request != null && request.startsWith("stop") && 1 < request.split("\\s+").length){
            String id = request.split("\\s+")[1];
            Option<com.gigaspaces.Instance> found = io.vavr.collection.List.ofAll(snapshot).find(instance -> instance.getInstanceId().equals(id));
            return found.map(instance -> new StopAdminCommand(email, instance));
        }
        return Option.none();
    }

    // Bella Greene <bella.greene@yellowwhitecontacts.info>
    private String extractEmail(String name) {
        return name.substring(name.indexOf("<") + 1, name.indexOf(">"));
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        Loop loop = new Loop(Users.readFrom("config.ini"));
        loop.run();
    }

    private  void evaluateAction(Action action, InstancesService instancesService, Brain brain) {
        if(action instanceof NotifyBeforeStopAction){
            evaluate((NotifyBeforeStopAction) action);
        }else if (action instanceof StopAction){
            evaluate((StopAction)action, instancesService, brain);
        }
    }


    private  void evaluate(StopAction action, InstancesService instancesService, Brain brain) {
        logger.info("Executing stop action {}", action);
        // first peek newman mailbox of email with the subject 'Please keep my instance $instance.instanceId running'
        if(!new EmailNotifications().shouldKeepInstance(action.getInstance().getInstanceId())) {
            logger.info("Stopping instance {}", action.getInstance().getInstanceId());
            instancesService.stopInstance(action.getInstance());
            try {
                String subject = String.format("[IMPORTANT] AWS instance (%s) started by (%s) was shutdown by the brain", action.getInstance().getInstanceId(), action.getSubject().getName());
                io.vavr.collection.List<String> recipients = notified();
                StoppedHTMLTemplate template = new StoppedHTMLTemplate(action);
                String htmlBody = template.formatHTMLBody();
                EmailNotifications.send(subject, htmlBody, recipients);
                logger.info("A stop email was sent to Email sent to {} ", recipients);
            } catch (IOException | MessagingException e) {
                logger.error(e.toString(), e);
            }
        }else{
            logger.info("Not Stopping instance {} started by {} because he bagged for his life.", action.getInstance().getInstanceId(), action.getSubject().getName());
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, 45);
            brain.setAction(action.getInstance(), new WaitAction(action.getInstance(), cal, Option.none()));
        }
    }

    private  void evaluate(NotifyBeforeStopAction action) {
        logger.info("Executing NotifyBeforeStopAction action {}", action);
        try {
            String subject = String.format("[URGENT] Alert before stopping AWS instance %s", action.getInstance().getInstanceId());
            io.vavr.collection.List<String> recipients = notified();
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
