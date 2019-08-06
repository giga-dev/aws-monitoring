package com.gigaspaces;

import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.cloudtrail.model.*;
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

    public Option<Event> findStartOldestEvent(String profile, Region region, Instance instance) {
        try (CloudTrailClient client = CloudTrailClient.builder()
                .region(region)
                .credentialsProvider(ProfileCredentialsProvider.builder().profileName(profile).build())
                .build()) {
            Option<Event> oldest = Option.none();
            LookupAttribute instanceId = LookupAttribute.builder().attributeKey(LookupAttributeKey.RESOURCE_NAME).attributeValue(instance.instanceId()).build();
            LookupEventsRequest req = LookupEventsRequest.builder().lookupAttributes(instanceId).build();
            for (Event event : client.lookupEventsPaginator(req).events()) {
                if ("RunInstances".equals(event.eventName()) || "StartInstances".equals(event.eventName())) {
                    oldest = Option.some(event);
                }
            }
            return oldest;
        }
    }


    public static void main(String[] args) {
        CouldTrailEventsReader couldTrailEventsReader = new CouldTrailEventsReader();
        Option<String> stack = couldTrailEventsReader.findUserNameByStackId("gigaspaces-ec2developers", Region.EU_WEST_1, "arn:aws:cloudformation:eu-west-1:535075449278:stack/eksctl-eks-cluster-nodegroup-ng-be39c976/4740e2c0-b67e-11e9-90ef-02a08d23d630");
        logger.info("stack is {}", stack);
    }
    public Option<String> findUserNameByStackId(String profile, Region region, String stackId) {
        try (CloudTrailClient client = CloudTrailClient.builder()
                .region(region)
                .credentialsProvider(ProfileCredentialsProvider.builder().profileName(profile).build())
                .build()) {
            LookupAttribute instanceId = LookupAttribute.builder().attributeKey(LookupAttributeKey.EVENT_NAME).attributeValue("CreateStack").build();
            LookupEventsRequest req = LookupEventsRequest.builder().lookupAttributes(instanceId).build();
            for (Event event : client.lookupEventsPaginator(req).events()) {
                for (Resource resource : event.resources()) {
                    if("AWS::CloudFormation::Stack".equals(resource.resourceType()) && stackId.equals(resource.resourceName())){
                        return Option.some(event.username());
                    }
                }
            }
            return Option.none();
        }
    }
}
