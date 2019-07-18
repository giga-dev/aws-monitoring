package com.gigaspaces;

import io.vavr.collection.HashSet;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cloudtrail.model.Event;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Loop {
    @SuppressWarnings("WeakerAccess")
    final static Logger logger = LoggerFactory.getLogger(Loop.class);

    public static void main(String[] args) throws InterruptedException{
        InstancesService instancesService = new InstancesService();
        List<com.gigaspaces.Instance> snapshot = new ArrayList<>();
        CouldTrailEventsReader couldTrailEventsReader = new CouldTrailEventsReader();
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
                            logger.info("getting details of instance {} ({})", name, instance.instanceId());
                            Option<Event> started = couldTrailEventsReader.startEvent(runningInstance.getProfile(), runningInstance.getRegion(), instance);
                            if (started.isDefined()) {
                                started.map(s -> new com.gigaspaces.Instance(runningInstance.getProfile(), runningInstance.getRegion(), name, instance.instanceId(), s.username(), s.eventName(), s.eventTime(), instance.instanceType().toString()))
                                        .forEach(found::add);
                            }
                        }).map(found::add);
                    }
                }
                Collections.sort(snapshot);
                for (com.gigaspaces.Instance instance : snapshot) {
                    logger.info("instance: {}", instance);
                }
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
                } else {
                    logger.info("Diff was not modified");
                }
            }catch(Exception e){
                logger.error(e.toString(), e);
            }
            Thread.sleep( 1000 * 60);
        }
    }
    private static Option<com.gigaspaces.Instance> find(List<com.gigaspaces.Instance> snapshot, String instanceId) {
        return io.vavr.collection.List.ofAll(snapshot).find(i -> i.getInstanceId().equals(instanceId));
    }

}
