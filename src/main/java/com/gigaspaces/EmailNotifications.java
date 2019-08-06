package com.gigaspaces;
import io.vavr.collection.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

@SuppressWarnings("WeakerAccess")
public class EmailNotifications {
    @SuppressWarnings("unused")
    final static Logger logger = LoggerFactory.getLogger(EmailNotifications.class);

    private Properties emailProperties;
    private Session mailSession;
    private MimeMessage emailMessage;


    @SuppressWarnings("WeakerAccess")
    public static void send(String subject, String body, List<String> recipients) throws IOException, MessagingException {
        EmailNotifications javaEmail = new EmailNotifications();

        javaEmail.setMailServerProperties();
        javaEmail.createEmailMessage(subject, body, recipients);
        javaEmail.sendEmail();
    }

    private void setMailServerProperties() throws IOException {
        emailProperties = readEmailProperties();
        emailProperties.put("mail.smtp.port", "587");
        emailProperties.put("mail.smtp.auth", "true");
        emailProperties.put("mail.smtp.starttls.enable", "true");

    }


    private void createEmailMessage(String emailSubject, String emailBody, List<String> toEmails) throws
            MessagingException {

        mailSession = Session.getDefaultInstance(emailProperties, null);
        emailMessage = new MimeMessage(mailSession);

        for (String toEmail : toEmails) {
            emailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        }

        emailMessage.setSubject(emailSubject);
        emailMessage.setContent(emailBody, "text/html");

    }

    private void sendEmail() throws MessagingException {
        String emailHost = "smtp.gmail.com";
        String fromUser = emailProperties.getProperty("fromUser");
        String fromUserEmailPassword = emailProperties.getProperty("fromUserEmailPassword");

        Transport transport = mailSession.getTransport("smtp");

        transport.connect(emailHost, fromUser, fromUserEmailPassword);
        transport.sendMessage(emailMessage, emailMessage.getAllRecipients());
        transport.close();
    }

    private Properties readEmailProperties() throws IOException {
        try(InputStream is = new FileInputStream("email.properties")){
            Properties res = new Properties();
            res.load(is);
            return res;
        }
    }

    private void checkEmail() throws MessagingException, IOException {
        Properties properties = new Properties();

        String host = "pop.gmail.com";
        properties.put("mail.pop3.host", host);
        properties.put("mail.pop3.port", "995");
        properties.put("mail.pop3.starttls.enable", "true");
        Session emailSession = Session.getDefaultInstance(properties);
        Store store = emailSession.getStore("pop3s");
        Properties ep = readEmailProperties();
        store.connect(host, ep.getProperty("fromUser"), ep.getProperty("fromUserEmailPassword"));
        Folder emailFolder = store.getFolder("INBOX");
        emailFolder.open(Folder.READ_WRITE);
        Message[] messages = emailFolder.getMessages();
        logger.info("messages.length--- {}", messages.length);

        for (int i = 0, n = messages.length; i < n; i++) {
            Message message = messages[i];
            logger.info("---------------------------------");
            logger.info("Email Number {}", (i + 1));
            logger.info("Subject: {}", message.getSubject());
            logger.info("From: {}", message.getFrom()[0]);
            logger.info("Text: {}", message.getContent().toString());
            logger.info("Sent Date: {}", message.getSentDate());
            if(message.getSubject().contains("Delivery Status Notification")
                    || message.getSubject().startsWith("Re: ")
                    || message.getFrom()[0].toString().contains("noreply")
                    || message.getFrom()[0].toString().contains("do-not-reply")){
                logger.info("deleting mail {} from {}",  message.getSubject(), message.getFrom()[0]);
                message.setFlag(Flags.Flag.DELETED, true);
            }
        }
        emailFolder.close(true);
        store.close();
    }

    public boolean shouldKeepInstance(String instanceId) {
        boolean ret = false;
        Properties properties = new Properties();

        String host = "pop.gmail.com";
        properties.put("mail.pop3.host", host);
        properties.put("mail.pop3.port", "995");
        properties.put("mail.pop3.starttls.enable", "true");
        Session emailSession = Session.getDefaultInstance(properties);
        try {
            Store store = emailSession.getStore("pop3s");
            Properties ep = readEmailProperties();
            store.connect(host, ep.getProperty("fromUser"), ep.getProperty("fromUserEmailPassword"));
            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_WRITE);
            Message[] messages = emailFolder.getMessages();
            logger.info("messages.length--- {}", messages.length);
            String subject = String.format("Please keep my instance: %s running", instanceId);

            for (int i = 0, n = messages.length; i < n; i++) {
                Message message = messages[i];
                logger.info("---------------------------------");
                logger.info("Email Number {}", (i + 1));
                logger.info("Subject: {}", message.getSubject());
                logger.info("From: {}", message.getFrom()[0]);
                logger.info("Text: {}", message.getContent().toString());
                logger.info("Sent Date: {}", message.getSentDate());
                if (message.getSubject().trim().equalsIgnoreCase(subject)){
                    message.setFlag(Flags.Flag.DELETED, true);
                    ret = true;
                }
            }
            emailFolder.close(true);
            store.close();
        }catch(Exception e){
            logger.error(e.toString(), e);
        }
        return ret;
    }

    public static void main(String[] args) throws IOException, MessagingException {
        EmailNotifications emailNotifications = new EmailNotifications();
        emailNotifications.checkEmail();
    }
}
