package com.epimorphics.registry.notification;

/**
 * Defines a way of notifying an external messaging system of changes in the registry.
 */
public interface NotificationAgent {
    /**
     * Send a message with the given parameters.
     * @param topic The topic on which to send the notification.
     * @param msg The current state of the target item, if the operation was constructive. Otherwise, null.
     * @param target The URI of the item which was changed.
     * @param operation The operation that was performed on the registry item.
     */
    void send(String topic, String msg, String target, String operation);
}
