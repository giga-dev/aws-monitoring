package com.gigaspaces;

import io.vavr.control.Option;
import org.ocpsoft.prettytime.PrettyTime;
import software.amazon.awssdk.regions.Region;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;

public class Instance implements Comparable<Instance> {
    private String profile;
    private Region  region;
    private Option<String> name;
    private String instanceId;
    private String startedBy;
    private String eventName;
    private Instant startedAt;
    private String type;

    public Instance(String profile, Region region, Option<String> name, String instanceId, String startedBy, String eventName, Instant startedAt, String type) {
        this.profile = profile;
        this.region = region;
        this.name = name;
        this.instanceId = instanceId;
        this.startedBy = startedBy;
        this.eventName = eventName;
        this.startedAt = startedAt;
        this.type = type;
    }

    @Override
    public int compareTo(Instance instance) {
        return startedAt.compareTo(instance.startedAt);
    }

    @SuppressWarnings("unused")
    public String getProfile() {
        return profile;
    }

    @SuppressWarnings("unused")
    public Region getRegion() {
        return region;
    }

    @SuppressWarnings("unused")
    public Option<String> getName() {
        return name;
    }

    @SuppressWarnings("WeakerAccess")
    public String getInstanceId() {
        return instanceId;
    }

    @SuppressWarnings("unused")
    public String getStartedBy() {
        return startedBy;
    }

    @SuppressWarnings("unused")
    public String getEventName() {
        return eventName;
    }

    @SuppressWarnings("unused")
    public Instant getStartedAt() {
        return startedAt;
    }

    @SuppressWarnings("unused")
    public String getType() {
        return type;
    }

    @SuppressWarnings("unused")
    public String getSince(){
        return new PrettyTime().format(Date.from(startedAt));
    }

    @SuppressWarnings("unused")
    public String getNameOr(){
        return name.getOrElse("---");
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Instance)) return false;
        Instance instance = (Instance) o;
        return getInstanceId().equals(instance.getInstanceId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInstanceId());
    }

    @Override
    public String toString() {
        PrettyTime p = new PrettyTime();
        return "Instance{" +
                "Started " + p.format(Date.from(startedAt)) + " (" + startedAt + ") " +
                ", by='" + startedBy + '\'' +
                ", name=" + name +
                ", type=" + type +
                ", instanceId='" + instanceId + '\'' +
                ", profile='" + profile + '\'' +
                ", region=" + region +
                ", eventName='" + eventName + '\'' +
                '}';
    }

}
