package com.gigaspaces;

import software.amazon.awssdk.regions.Region;

public class AWSData<T> {
    private String profile;
    private Region region;
    private T data;

    public AWSData(String profile, Region region, T data) {
        this.profile = profile;
        this.region = region;
        this.data = data;
    }

    public String getProfile() {
        return profile;
    }

    public Region getRegion() {
        return region;
    }

    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        return "AWSData{" +
                "profile='" + profile + '\'' +
                ", region=" + region +
                ", data=" + data +
                '}';
    }
}
