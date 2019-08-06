package com.gigaspaces;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;

import java.util.Collections;
import java.util.List;

public class Alarms {
    final static Logger logger = LoggerFactory.getLogger(Alarms.class);

    public static void main(String[] args) {
        String instanceId = "i-04735d1528cc1a1f8";

        final AmazonCloudWatch cw1 =
                AmazonCloudWatchClientBuilder.standard()
                        .withRegion("us-east-2")
                        .withCredentials(new ProfileCredentialsProvider("aws-ec2se"))
                        .build();
//        cw1.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(instanceId + " idle"));


        PutMetricAlarmRequest req = new PutMetricAlarmRequest();
        req.setAlarmName(instanceId + " idle");
        req.setMetricName("CPUUtilization");
        req.setDimensions(Collections.singleton(new Dimension().withName("InstanceId").withValue(instanceId)));
        req.setPeriod(5 * 60);
        req.setEvaluationPeriods(1);
        req.setDatapointsToAlarm(1);
        req.setThreshold(20.0);
        req.setComparisonOperator(ComparisonOperator.LessThanOrEqualToThreshold);
        req.setNamespace("AWS/EC2");
        req.setStatistic("Average");
        req.setAlarmActions(Collections.singleton("arn:aws:sns:us-east-2:210661820370:cloudwatch_event_parser"));
        PutMetricAlarmResult res = cw1.putMetricAlarm(req);
        logger.info("res is {}", res);
/*
//        List<MetricAlarm> ma = cw1.describeAlarms(new DescribeAlarmsRequest().withAlarmNames(Collections.singleton(instanceId + " idle"))).getMetricAlarms();
        List<MetricAlarm> ma = cw1.describeAlarms(new DescribeAlarmsRequest().withAlarmNames(Collections.singleton("awsec2-i-04735d1528cc1a1f8-Low-CPU-Utilization"))).getMetricAlarms();
        logger.info("discover me {}", ma);
        for (MetricAlarm metricAlarm : ma) {
            logger.info("metricAlarm.getExtendedStatistic() {}", metricAlarm.getExtendedStatistic());
            logger.info("metricAlarm.getAlarmActions() {}", metricAlarm.getAlarmActions());
            logger.info("metricAlarm.getStateValue() {}", metricAlarm.getStateValue());
        }
*/
//        DescribeAlarmHistoryResult history = cw1.describeAlarmHistory(new DescribeAlarmHistoryRequest().withAlarmName("i-04735d1528cc1a1f8 idle"));

        cw1.shutdown();

        /*
        for (String profile : Util.PROFILES) {
            for (Region region : Util.REGIONS) {
                final AmazonCloudWatch cw =
                        AmazonCloudWatchClientBuilder.standard()
                                .withRegion(region.id())
                                .withCredentials(new ProfileCredentialsProvider(profile))
                                .build();
                boolean done = false;
//                DescribeAlarmsRequest request = new DescribeAlarmsRequest().withAlarmNames("stop_instance_if_idle_alarm");
                DescribeAlarmsRequest request = new DescribeAlarmsRequest();
                logger.info("request is {}", request);
                while (!done) {

                    DescribeAlarmsResult response = cw.describeAlarms(request);
                    logger.info("got response {}", response);

                    for (MetricAlarm alarm : response.getMetricAlarms()) {
                        logger.info("Retrieved alarm %{}", alarm.getAlarmName());
                    }

                    request.setNextToken(response.getNextToken());

                    if (response.getNextToken() == null) {
                        done = true;
                    }
                }
                cw.shutdown();
                // cw.describeAlarmsForMetric()
                //"i-04735d1528cc1a1f8" "aws-ec2se"	"us-east-2"
            }
        }*/
    }

}

/*
aws cloudwatch put-metric-alarm --alarm-name cpu-mon --alarm-description "Alarm when CPU exceeds 70 percent" --metric-name CPUUtilization --namespace AWS/EC2 --statistic Average --period 300 --threshold 70 --comparison-operator GreaterThanThreshold  --dimensions "Name=InstanceId,Value=i-12345678" --evaluation-periods 2 --alarm-actions arn:aws:sns:us-east-1:111122223333:MyTopic --unit Percent
 */