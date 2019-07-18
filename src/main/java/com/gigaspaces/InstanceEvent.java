package com.gigaspaces;


import io.vavr.collection.Stream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudtrail.model.Event;
import software.amazon.awssdk.services.cloudtrail.model.Resource;

import java.time.Instant;

import static com.gigaspaces.InstanceEventType.START_INSTANCES;
import static com.gigaspaces.InstanceEventType.STOP_INSTANCES;

@SuppressWarnings("WeakerAccess")
public class InstanceEvent {
    private final InstanceEventType type;
    private final String eventId;
    private final Instant eventTime;
    private final String username;
    private final String resource;
    private final String profile;
    private final Region region;

    public static Stream<InstanceEvent> from(Event event, String profile, Region region){
        return Stream.ofAll(event.resources()).map(r -> fromResource(r, event, profile, region));
    }

    private static InstanceEvent fromResource(Resource resource, Event event, String profile, Region region) {
        return new InstanceEvent("StartInstances".equals(event.eventName()) ? START_INSTANCES : STOP_INSTANCES, event.eventId(), event.eventTime(), event.username(), resource.resourceName(), profile, region);
    }

    public InstanceEventType getType() {
        return type;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public String getUsername() {
        return username;
    }

    public String getResource() {
        return resource;
    }

    public String getProfile() {
        return profile;
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public String toString() {
        return "InstanceEvent{" +
                "type=" + type +
                ", eventId='" + eventId + '\'' +
                ", eventTime=" + eventTime +
                ", username='" + username + '\'' +
                ", resource='" + resource + '\'' +
                ", profile='" + profile + '\'' +
                ", region=" + region +
                '}';
    }


    private InstanceEvent(InstanceEventType type, String eventId, Instant eventTime, String username, String resource, String profile, Region region) {
        this.type = type;
        this.eventId = eventId;
        this.eventTime = eventTime;
        this.username = username;
        this.resource = resource;
        this.profile = profile;
        this.region = region;
    }
}
