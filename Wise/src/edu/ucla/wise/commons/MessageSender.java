package edu.ucla.wise.commons;

import javax.activation.DataHandler;
import javax.mail.AuthenticationFailedException;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.MethodNotSupportedException;
import javax.mail.Multipart;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;

import edu.ucla.wise.email.EmailMessage;
import edu.ucla.wise.email.EmailProperties;
import edu.ucla.wise.initializer.WiseProperties;

/**
 * This class encapsulates some specific methods to send messages from Message
 * Sequences.
 * 
 * @author Douglas Bell
 * @version 1.0
 */
public class MessageSender {
    public static final Logger LOGGER = Logger.getLogger(MessageSender.class);
    /** Instance Variables */
    public Session session;
    private String fromStr, replyStr;

    private final WiseProperties wiseProperties;

    /**
     * Constructor : gets the email session from WISE Application.
     */
    public MessageSender(WiseProperties wiseProperties) {
        this.wiseProperties = wiseProperties;
        this.session = WISEApplication.getMailSession(null, wiseProperties);
    }

    /**
     * Constructor : gets the email session from WISE Application and other
     * instance variables from MessageSequence.
     * 
     * @param msgSeq
     *            Message sequence for which sender has to be created.
     */
    public MessageSender(MessageSequence msgSeq, WiseProperties wiseProperties) {
        this.wiseProperties = wiseProperties;
        // String myFromID = msg_seq.emailID();
        /* WISEApplication knows how to look up passwords */
        this.session = WISEApplication.getMailSession(null, wiseProperties);
        this.fromStr = msgSeq.getFromString();
        this.replyStr = msgSeq.getReplyString();
    }

    // public void set_fromString(String fromString)
    // {
    //
    // }

    /**
     * looks up, compose, and send email message
     * 
     * @param msg
     *            Message object to be sent to the User.
     * @param messageUseID
     *            The id used for generating URL which is unique to the user.
     * @param toUser
     *            User to whom the message has to be sent.
     * @param db
     *            Data bank object used from talking to DB.
     * @return String Empty string if successful.
     */
    public String sendMessage(Message msg, String messageUseID, User toUser, DataBank db,
            EmailProperties emailProperties) {
        String salutation = toUser.getSalutation();
        String lastname = toUser.getLastName();
        String email = toUser.getEmail();
        String inviteeID = toUser.getId();

        EmailMessage emailMessage = new EmailMessage(email, salutation, lastname);

        /* these are all pretty fixed relationships */
        String ssid = toUser.getCurrentSurvey().getStudySpace().id;
        return this.sendMessage(msg, messageUseID, emailMessage, ssid, db, inviteeID, emailProperties);
    }

    /**
     * Sends an email to the User.
     * 
     * @param msg
     *            Message object to be sent to the User.
     * @param messageUseID
     *            The id used for generating URL which is unique to the user.
     * @param toEmail
     *            Email id to which message has to be sent.
     * @param salutation
     *            Salutation to address the invitee to whom email has to be
     *            sent.
     * @param lastname
     *            Last name of the invitee.
     * @param ssid
     *            Studyspace id for generation of the URL.
     * @param db
     *            Data bank object used from talking to DB.
     * @param inviteeId
     *            Id of the user to whom email has to be sent
     * @return String Empty string if successful.
     */
    public String sendMessage(Message msg, String messageUseID, EmailMessage emailMessage, String ssid, DataBank db,
            String inviteeId, EmailProperties emailProperties) {
        String outputString = "uncaught exception";
        String message = null;
        try {
            /* create message object */
            MimeMessage mMessage = new MimeMessage(this.session);

            /* send message to each of the users */
            InternetAddress tmpAddr = new InternetAddress(this.fromStr);
            mMessage.setFrom(tmpAddr);
            if (this.replyStr != null) {
                tmpAddr = new InternetAddress(this.replyStr);
                mMessage.setReplyTo(new InternetAddress[] { tmpAddr });
            }
            java.util.Date today = new java.util.Date();
            mMessage.setSentDate(today);
            mMessage.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(emailMessage.getToEmail()));
            mMessage.setSubject(msg.subject);

            /* check if message produces an html body; null indicates no */
            message = msg.composeHtmlBody(emailMessage.getSalutation(), emailMessage.getLastname(), ssid, messageUseID);
            message = db.replacePattern(message, inviteeId);

            /* if message is null go ahead and prepare a text body */
            if (message == null) {
                message = msg.composeTextBody(emailMessage.getSalutation(), emailMessage.getLastname(), ssid,
                        messageUseID);
                message = db.replacePattern(message, inviteeId);
                mMessage.setText(message);
            } else {

                /*
                 * create an "Alternative" Multipart message to send both html &
                 * text email
                 */
                Multipart mp = new MimeMultipart("alternative");

                /* add text body part */
                BodyPart bpText = new MimeBodyPart();
                bpText.setDataHandler(new DataHandler(msg.composeTextBody(emailMessage.getSalutation(),
                        emailMessage.getLastname(), ssid, messageUseID), "text/plain"));
                mp.addBodyPart(bpText);

                /* add html body part */
                BodyPart bpHtml = new MimeBodyPart();
                bpHtml.setDataHandler(new DataHandler(message, "text/html"));
                mp.addBodyPart(bpHtml);

                /* set the message body */
                mMessage.setContent(mp);
            }

            // System.out.println(message);
            /* send message and return the result */
            outputString = mailingProcess(mMessage, this.session, this.fromStr, this.replyStr, emailMessage,
                    emailProperties);

        } catch (MessagingException e) {
            LOGGER.error(
                    "\r\nWISE - MESSAGE SENDER on email message: " + message + ".\r\n Full error: " + e.toString(), e);
        } catch (Exception e) {
            LOGGER.error(
                    "\r\nWISE - MESSAGE SENDER on email message: " + message + ".\r\n Full error: " + e.toString(), e);
        }
        return outputString;
    }

    /**
     * This method tests sending messages.
     * 
     * @param msgText
     *            Message test that has to be sent.
     * @return String If the mail sending is successful or not.
     */
    public String sendTest(String msgText, String fromEmail, String toEmail, EmailMessage emailMessage,
            EmailProperties emailProperties) {
        String outputString = "";
        try {

            /* create message object */
            MimeMessage message = new MimeMessage(this.session);

            /* send message to each of the users */
            message.setFrom(new InternetAddress(fromEmail));
            java.util.Date today = new java.util.Date();
            message.setSentDate(today);
            message.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("This is a test");
            message.setText(msgText);

            /* send message and analyze the mailing failure */
            String msgResult = mailingProcess(message, this.session, this.fromStr, this.replyStr, emailMessage,
                    emailProperties);
            if (msgResult.equalsIgnoreCase("")) {
                outputString = "D";
            } else {
                outputString = msgResult;
            }

        } catch (MessagingException e) {
            LOGGER.error("WISE EMAIL - MESSAGE SENDER - SEND REMINDER: " + e.toString(), null);
        } catch (Exception e) {
            LOGGER.error("WISE EMAIL - MESSAGE SENDER - SEND REMINDER: " + e.toString(), null);
        }

        return outputString;
    }

    /**
     * Sends the actual email to the user.
     * 
     * @param msg
     *            Mime message which contains the details for sending email.
     * @return String Empty if successful.
     * @throws MessagingException
     * @throws Exception
     */
    public static String mailingProcess(MimeMessage msg, Session session, String fromEmail, String replyEmail,
            EmailMessage emailMessage, EmailProperties emailProperties) throws MessagingException, Exception {
        String mailingResult = "";
        if (msg == null) {
            return "msg is null";
        }
        try {
            if (session == null) {
                LOGGER.info("Session is null!!");
            }
            Transport tr = session.getTransport("smtp");

            if (tr == null) {
                LOGGER.info("tr is null!!");
            }

            boolean sslEmail = true;

            msg.saveChanges(); // don't forget this
            if (msg.getAllRecipients() == null) {
                LOGGER.info("Get All Recepients is null");
            }
            if (sslEmail) {
                tr.connect(emailProperties.getEmailHost(), emailProperties.getUsername(), emailProperties.getPassword());
                Transport.send(msg, msg.getAllRecipients());
            } else {
                Transport.send(msg, msg.getAllRecipients());
            }
            tr.close();

        } catch (AuthenticationFailedException e) {
            LOGGER.error("Message_Sender - Authentication failed. From string: " + fromEmail + "; Reply: " + replyEmail
                    + ". \n", e);
            mailingResult = "Authentication process failed";
            return mailingResult;
        } catch (SendFailedException e) {
            LOGGER.error("Message_Sender - Invalid email address. " + e.toString(), e);
            mailingResult = "Email address is invalid.";
            return mailingResult;
        } catch (MethodNotSupportedException e) {
            LOGGER.error("Message_Sender - Unsupported message type. " + e.toString(), e);
            mailingResult = "Message is not supported.";
            return mailingResult;
        } catch (Exception e) {
            LOGGER.info("Message_Sender - mailing_process failure: " + e.toString(), e);
            mailingResult = "Email failed (null pointer error): " + e.toString();
            throw e;
        }
        return mailingResult;
    }
}