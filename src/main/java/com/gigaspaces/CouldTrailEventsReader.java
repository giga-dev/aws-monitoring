package com.gigaspaces;

import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.cloudtrail.model.Event;
import software.amazon.awssdk.services.cloudtrail.model.LookupAttribute;
import software.amazon.awssdk.services.cloudtrail.model.LookupAttributeKey;
import software.amazon.awssdk.services.cloudtrail.model.LookupEventsRequest;
import software.amazon.awssdk.services.ec2.model.Instance;

@SuppressWarnings("WeakerAccess")
public class CouldTrailEventsReader {
    @SuppressWarnings("unused")
    final static Logger logger = LoggerFactory.getLogger(CouldTrailEventsReader.class);

    public CouldTrailEventsReader() {
    }


    @SuppressWarnings("WeakerAccess")
    public Option<Event> startEvent(String profile, Region region, Instance instance) {
        try (CloudTrailClient client = CloudTrailClient.builder()
                .region(region)
                .credentialsProvider(ProfileCredentialsProvider.builder().profileName(profile).build())
                .build()) {
            LookupAttribute instanceId = LookupAttribute.builder().attributeKey(LookupAttributeKey.RESOURCE_NAME).attributeValue(instance.instanceId()).build();
            LookupEventsRequest req = LookupEventsRequest.builder().lookupAttributes(instanceId).build();
            return findStartEvent(client.lookupEventsPaginator(req).events());
        }
    }

    // find the first RunInstance or StartInstance but abort on StopInstance
    private static Option<Event> findStartEvent(SdkIterable<Event> events) {
        for (Event event : events) {
            if ("StopInstances".equals(event.eventName())) {
                return Option.none();
            }
            if ("RunInstances".equals(event.eventName()) || "StartInstances".equals(event.eventName())) {
                return Option.some(event);
            }
        }
        return Option.none();
    }
}
