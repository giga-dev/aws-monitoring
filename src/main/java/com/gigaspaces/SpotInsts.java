package com.gigaspaces;

import io.vavr.control.Option;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;

import java.io.File;
import java.io.FileInputStream;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;

public class SpotInsts{
//    https://api.spotinst.com/spotinst-api/elastigroup/amazon-web-services/list-all/
//    https://api.spotinst.io/audit/events?fromDate={fromDate}&toDate={toDate}&seAccountId={ACCOUNT_ID}
@SuppressWarnings("WeakerAccess")
final static Logger logger = LoggerFactory.getLogger(SpotInsts.class);

    private String token;
    private String seAccountId;
    private String devAccountId;

    @SuppressWarnings("WeakerAccess")
    public SpotInsts() throws IOException {
        this("ec2se");
    }

    @SuppressWarnings("WeakerAccess")
    public SpotInsts(String tokenName) throws IOException {
        Properties p = new Properties();
        try(FileInputStream is = new FileInputStream(new File("spotinsts_token"))){
            p.load(is);
        }
        token = p.getProperty(tokenName);
        seAccountId = p.getProperty("se_accountId");
        devAccountId = p.getProperty("dev_accountId");
//        logger.info("token is {}", token);
    }

    @SuppressWarnings("WeakerAccess")
    public Option<String> readUserHistory(@SuppressWarnings("unused") String profile, @SuppressWarnings("unused") Region region, String groupName, Instant eventTime) {
//        logger.info("Searching for spotinsts user for group {}", groupName);
        ZonedDateTime zdt = ZonedDateTime.ofInstant(eventTime, ZoneId.systemDefault());
        Calendar cal = GregorianCalendar.from(zdt);
        cal.add(Calendar.DAY_OF_YEAR, -2);
        long start = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, 4);
        long end = cal.getTimeInMillis();
        try(CloseableHttpClient httpclient = HttpClients.createDefault()){
//            HttpGet httpget = new HttpGet("https://api.spotinst.io/aws/ec2/group?accountId=" + seAccountId);
            HttpGet httpget = new HttpGet(String.format("https://api.spotinst.io/audit/events?fromDate=%d&toDate=%d&accountId=%s",start, end , profile.contains("se") ? seAccountId : devAccountId));
            httpget.setConfig(RequestConfig.custom()
                    .setConnectionRequestTimeout(1000).setConnectTimeout(1000).setSocketTimeout(1000).build());
            httpget.addHeader("Content-Type", "application/json");
            httpget.addHeader("Authorization", "Bearer " + token);
            try(CloseableHttpResponse response = httpclient.execute(httpget)){
//                for (Header header : response.getAllHeaders()) {
//                    logger.info("response header {}", header);
//                };
                String bodyAsString = EntityUtils.toString(response.getEntity());
//                logger.info("while searching for group {} got body is {}", groupName, bodyAsString);
                JSONObject body = new JSONObject(bodyAsString);
                for (Object item : body.getJSONObject("response").getJSONArray("items")) {
                    JSONObject jItem = (JSONObject) item;
                    if(jItem.getString("resourceType").equals("Elastigroup")
                            && jItem.getString("actionType").equals("Create")
                            && jItem.getString("resourceId").equals(groupName)) {
//                        logger.info("spotinsts user for group {} is {}", groupName, jItem.getString("user"));
                        return Option.some(jItem.getString("user"));
                    }
                }
            }
            return Option.none();
        }catch(Exception e){
            logger.error(e.toString(), e);
            return Option.none();
        }
    }

    public static void main(String[] args) throws IOException {
        new SpotInsts().getAccountId();
    }

    private void getAccountId(){
//        https://api.spotinst.io/setup/account?awsAccountId={AWS_ACCOUNT_ID}
        try(CloseableHttpClient httpclient = HttpClients.createDefault()){
//            HttpGet httpget = new HttpGet("https://api.spotinst.io/aws/ec2/group?accountId=" + seAccountId);
            HttpGet httpget = new HttpGet(String.format("https://api.spotinst.io/setup/account?awsAccountId=%s", "535075449278"));
            httpget.setConfig(RequestConfig.custom()
                    .setConnectionRequestTimeout(1000).setConnectTimeout(1000).setSocketTimeout(1000).build());
            httpget.addHeader("Content-Type", "application/json");
            httpget.addHeader("Authorization", "Bearer " + token);
            try(CloseableHttpResponse response = httpclient.execute(httpget)){
//                for (Header header : response.getAllHeaders()) {
//                    logger.info("response header {}", header);
//                };
                String bodyAsString = EntityUtils.toString(response.getEntity());
                logger.info("got body: {}", bodyAsString);

            }
            return;
        }catch(Exception e){
            logger.error(e.toString(), e);
            return;
        }

    }
}
