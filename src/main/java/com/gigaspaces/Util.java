package com.gigaspaces;

import io.vavr.collection.List;
import software.amazon.awssdk.regions.Region;

@SuppressWarnings("WeakerAccess")
public class Util {
    public static final List<String> PROFILES = List.of("gigaspaces-ec2developers", "aws-ec2se");
    public static final List<Region> REGIONS = List.ofAll(Region.regions()).filter(region -> !region.equals(Region.US_GOV_EAST_1) && !region.equals(Region.US_GOV_WEST_1)
            && !region.equals(Region.CN_NORTH_1) && !region.equals(Region.CN_NORTHWEST_1)
            && !region.equals(Region.AWS_GLOBAL)
            && !region.equals(Region.AWS_CN_GLOBAL)
            && !region.equals(Region.AWS_US_GOV_GLOBAL));

}
