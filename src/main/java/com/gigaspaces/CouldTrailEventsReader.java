package com.gigaspaces;

import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.cloudtrail.model.*;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Reservation;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class CouldTrailEventsReader {
    @SuppressWarnings("WeakerAccess")
    final static Logger logger = LoggerFactory.getLogger(CouldTrailEventsReader.class);
    private Map<String, String> lastIdMap = HashMap.empty();
    private Map<String, Reservation> reservations = HashMap.empty();

    @SuppressWarnings("WeakerAccess")
    public CouldTrailEventsReader() {
    }

    public static void main(String[] args) {
        iterate();
    }

    private static void iterate() {
        CouldTrailEventsReader reader = new CouldTrailEventsReader();
        for (String profile : Arrays.asList("gigaspaces-ec2developers", "aws-ec2se")) {
            reader.read(profile).forEach(event -> {
                reader.updateMap(event);
                logger.info("{}", event);
                String key = event.getResource() + ":" + event.getProfile() + ":" + event.getRegion();
                if(!reader.reservations.containsKey(key)) {
                    try (Ec2Client client = Ec2Client.builder()
                            .region(event.getRegion())
                            .credentialsProvider(ProfileCredentialsProvider.builder().profileName(profile).build())
                            .build()) {
                        List<Reservation> r = client.describeInstances(DescribeInstancesRequest.builder().instanceIds(event.getResource()).build()).reservations();
                        if(!r.isEmpty()) {
                            reader.reservations = reader.reservations.put(key, r.get(0));
                        }
                    } catch (Ec2Exception e) {
                        reader.reservations = reader.reservations.put(key, null);
                        if (400 != e.statusCode()) {
                            throw e;
                        }
                    }
                }
            });
        }
        logger.info("Reservation");
        for (Tuple2<String, Reservation> reservation : reader.reservations) {
            logger.info("{}", reservation);
        }
    }

    private void updateMap(InstanceEvent event) {
        lastIdMap = lastIdMap.put(keyFor(event.getProfile(), event.getRegion(), event.getType()), event.getEventId());
    }

    private Stream<InstanceEvent> read(String profile) {
        Stream<InstanceEvent> res = Stream.empty();
        for (Region region : Region.regions()) {
            if (region.equals(Region.US_GOV_EAST_1) || region.equals(Region.US_GOV_WEST_1)
                    || region.equals(Region.CN_NORTH_1) || region.equals(Region.CN_NORTHWEST_1)
                    || region.equals(Region.AWS_GLOBAL)
                    || region.equals(Region.AWS_CN_GLOBAL)
                    || region.equals(Region.AWS_US_GOV_GLOBAL)) continue;
            try (CloudTrailClient client = CloudTrailClient.builder()
                    .region(region)
                    .credentialsProvider(ProfileCredentialsProvider.builder().profileName(profile).build())
                    .build()) {
                Option<String> lastStartEventId = lastIdMap.get(keyFor(profile, region, InstanceEventType.START_INSTANCES));
                Option<String> lastStopEventId = lastIdMap.get(keyFor(profile, region, InstanceEventType.STOP_INSTANCES));
//                logger.info("checking profile: {}, region: {} until {} {}", profile, region, lastStartEventId, lastStopEventId);
                Stream<InstanceEvent> stream = read(client).takeUntil(oneOf(lastStartEventId, lastStopEventId)).take(1000).filter(CouldTrailEventsReader::isEventMonitored)
                        .flatMap(e -> InstanceEvent.from(e, profile, region));
                if (!stream.isEmpty()) {
                    res = Stream.concat(res, stream);
                }
            }
        }
        return res.sortBy(InstanceEvent::getEventTime);

    }

    private Predicate<? super Event> oneOf(Option<String> lastStartEventId, Option<String> lastStopEventId) {
        return e -> {
            boolean ret = lastStartEventId.contains(e.eventId()) || lastStopEventId.contains(e.eventId());
            if (ret) logger.info("oneOf stopped at event {}", e);
            return ret;
        };
    }


    private String keyFor(String profile, Region region, InstanceEventType type) {
        return profile + ":" + region + ":" + type;
    }

    private static boolean isEventMonitored(Event event) {
        return "StartInstances".equals(event.eventName()) || "StopInstances".equals(event.eventName());
    }

    private Stream<Event> read(CloudTrailClient client) {
        LookupAttribute startInstances = LookupAttribute.builder().attributeKey(LookupAttributeKey.EVENT_NAME).attributeValue("StartInstances").build();
        LookupAttribute stopInstances = LookupAttribute.builder().attributeKey(LookupAttributeKey.EVENT_NAME).attributeValue("StopInstances").build();
        LookupEventsRequest startReq = LookupEventsRequest.builder().lookupAttributes(startInstances).build();
        LookupEventsRequest stopReq = LookupEventsRequest.builder().lookupAttributes(stopInstances).build();
        Stream<Event> startStream = Stream.ofAll(client.lookupEventsPaginator(startReq).events());
        Stream<Event> stopStream = Stream.ofAll(client.lookupEventsPaginator(stopReq).events());
        return Stream.concat(stopStream, startStream); //.sortBy(Event::eventTime);
    }
}
