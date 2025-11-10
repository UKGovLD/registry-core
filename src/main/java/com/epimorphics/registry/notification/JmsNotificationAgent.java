package com.epimorphics.registry.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;

public class JmsNotificationAgent implements NotificationAgent {
    private final Logger log = LoggerFactory.getLogger(JmsNotificationAgent.class);

    private TopicConnectionFactory connectionFct;
    private Boolean disableMessageId = true;

    public void setConnectionFactory(TopicConnectionFactory connectionFct) {
        this.connectionFct = connectionFct;
    }

    public void setDisableMessageId(Boolean disableMessageId) {
        this.disableMessageId = disableMessageId;
    }

    @Override public void send(Notification notification) throws Exception {
        String target = notification.getTarget();
        String operation = notification.getOperation();
        String msg = notification.getMessage();

        if (connectionFct == null) {
            log.error("Connection factory not configured - unable to send message for target {}.", target);
        }

        try (TopicConnection connection = connectionFct.createTopicConnection()) {
            connection.start();
            TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

            for (String topic: notification.getTopics()) {
                Topic topicObj = session.createTopic(topic);
                TopicPublisher producer = session.createPublisher(topicObj);
                producer.setDisableMessageID(disableMessageId);

                Message jmsMsg = session.createTextMessage(msg);
                jmsMsg.setStringProperty("target", target);
                jmsMsg.setStringProperty("operation", operation);

                log.debug("Sending JMS notification to topic: {} for target: {}, operation: {}, message: {}", topic, target, operation, msg);
                producer.publish(jmsMsg);
            }
        } catch (Exception e) {
            log.error("Failed to send JMS notification for target: {}", target, e);
            throw e;
        }
    }
}
