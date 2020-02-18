package com.epimorphics.registry.notification;

/**
 * Defines a way of notifying an external messaging system of changes in the registry.
 */
public interface NotificationAgent {
    /**
     * Send a notification.
     * @param notification The notification to send.
     * @throws Exception When the notification failed.
     */
    void send(Notification notification) throws Exception;
}
