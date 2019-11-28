package com.epimorphics.registry.notification;

public interface NotificationAgent {
    void send(String msg, String target, String operation);
}
