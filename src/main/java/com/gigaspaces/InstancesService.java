package com.gigaspaces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.ArrayList;
import java.util.List;

public class InstancesService {
    @SuppressWarnings("WeakerAccess")
    final static Logger logger = LoggerFactory.getLogger(CouldTrailEventsReader.class);

    public InstancesService() {
    }

    @SuppressWarnings("WeakerAccess")
    public List<AWSData<Reservation>> describeAllRunningInstances() {
        List<AWSData<Reservation>> res = new ArrayList<>();
        for (String profile : Util.PROFILES) {
            for (Region region : Util.REGIONS) {
                try (Ec2Client client = Ec2Client.builder()
                        .region(region)
                        .credentialsProvider(ProfileCredentialsProvider.builder().profileName(profile).build())
                        .build()) {
                    DescribeInstancesRequest runningReq = DescribeInstancesRequest.builder().filters(Filter.builder().name("instance-state-name").values("running").build()).build();
                    for (Reservation reservation : client.describeInstancesPaginator(runningReq).reservations()) {
                        res.add(new AWSData<>(profile, region, reservation));
                    }
                } catch(Ec2Exception e){
                    if (400 != e.statusCode()) {
                        throw e;
                    }
                }
            }
        }
        return res;
    }

    public static void main(String[] args) {
        InstancesService instances = new InstancesService();
        for (AWSData<Reservation> instance : instances.describeAllRunningInstances()) {
            logger.info("instance {}", instance);
        }
    }

    void stopInstance(Instance instance) {
        try (Ec2Client client = Ec2Client.builder()
                .region(instance.getRegion())
                .credentialsProvider(ProfileCredentialsProvider.builder().profileName(instance.getProfile()).build())
                .build()) {
            client.stopInstances(StopInstancesRequest.builder().instanceIds(instance.getInstanceId()).build());
        } catch(Ec2Exception e){
            if (400 != e.statusCode()) {
                throw e;
            }
        }

    }
}
