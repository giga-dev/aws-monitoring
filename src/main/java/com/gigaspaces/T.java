package com.gigaspaces;

import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class T {
    final static Logger logger = LoggerFactory.getLogger(T.class);
    public static void main(String[] args) throws UnknownHostException {
        BasicConfigurator.configure();

        System.setProperty("java.net.preferIPv6Addresses","true");
        InetAddress name = InetAddress.getByName("fe00:aa:bb:cc::2");
        logger.info("address is {}", name);

//        String address = InetAddress.getLoopbackAddress().getHostAddress();
//        logger.info("address is {}", address);
    }
}
