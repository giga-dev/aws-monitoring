package com.gigaspaces;

import com.amazonaws.ClientConfiguration;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.MaxNumberOfRetriesCondition;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.time.Duration;
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
//                        .overrideConfiguration(ClientOverrideConfiguration.builder().retryPolicy())
                        .overrideConfiguration(ClientOverrideConfiguration.builder()
                                .apiCallAttemptTimeout(Duration.ofSeconds(30))
                                .retryPolicy(RetryPolicy.builder()
                                        .backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofSeconds(10)))
                                        .numRetries(Integer.MAX_VALUE)
                                        .retryCondition(MaxNumberOfRetriesCondition.create(Integer.MAX_VALUE))
                                        .build())
                                .build())
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
        Option<String> groupName = instance.getSpotGroupName();
        if(!groupName.isDefined()){
            try (Ec2Client client = Ec2Client.builder()
                    .region(instance.getRegion())
                    .credentialsProvider(ProfileCredentialsProvider.builder().profileName(instance.getProfile()).build())
                    .build()) {
                client.stopInstances(StopInstancesRequest.builder().instanceIds(instance.getInstanceId()).build());
            } catch (Ec2Exception e) {
                if (400 != e.statusCode()) {
                    throw e;
                }
            }
        }else{
            String gn = groupName.get();
            try {
                SpotInsts spotInsts = new SpotInsts();
                spotInsts.stopGroup(instance.getProfile(), gn);
            }catch(Exception e){
                logger.error(e.toString(), e);
            }
        }

    }
}
