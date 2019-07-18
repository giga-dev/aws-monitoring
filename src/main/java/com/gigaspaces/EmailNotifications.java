package com.gigaspaces;
import io.vavr.collection.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
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
}
