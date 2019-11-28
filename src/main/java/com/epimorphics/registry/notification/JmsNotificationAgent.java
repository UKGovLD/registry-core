package com.epimorphics.registry.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;

public class JmsNotificationAgent implements NotificationAgent {
    private final Logger log = LoggerFactory.getLogger(JmsNotificationAgent.class);

    private TopicConnectionFactory connectionFct;
    private String destination;
    private Boolean disableMessageId = true;

    public void setConnectionFactory(TopicConnectionFactory connectionFct) {
        this.connectionFct = connectionFct;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setDisableMessageId(Boolean disableMessageId) {
        this.disableMessageId = disableMessageId;
    }

    @Override public void send(String msg, String target, String operation) {
        if (connectionFct == null) {
            log.error("Connection factory not configured - unable to send message for target " + target + ".");
        }

        try (TopicConnection connection = connectionFct.createTopicConnection()) {
            connection.start();
            TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(destination);
            TopicPublisher producer = session.createPublisher(topic);
            producer.setDisableMessageID(disableMessageId);

            Message jmsMsg = session.createTextMessage(msg);
            jmsMsg.setStringProperty("target", target);
            jmsMsg.setStringProperty("operation", operation);

            producer.publish(jmsMsg);
        } catch (Exception e) {
            log.error("Failed to send STOMP notification for target " + target + ".", e);
        }
    }
}
