package com.epimorphics.registry.notification;

import java.util.List;

/**
 * A notification of a change within the registry, that can be sent to an external messaging system.
 */
public interface Notification {
    /**
     * @return The messaging topics to notify.
     */
    List<String> getTopics();

    /**
     * @return The current state of the target item, if the operation was constructive. Otherwise, null.
     */
    String getMessage();

    /**
     * @return The URI of the item which was changed.
     */
    String getTarget();

    /**
     * @return The operation that was performed on the target item.
     */
    String getOperation();

    class Base implements Notification {
        private final List<String> topics;
        private final String msg;
        private final String target;
        private final String operation;

        Base(List<String> topics, String msg, String target, String operation) {
            this.topics = topics;
            this.msg = msg;
            this.target = target;
            this.operation = operation;
        }

        @Override public List<String> getTopics() { return topics; }
        @Override public String getMessage() { return msg; }
        @Override public String getTarget() { return target; }
        @Override public String getOperation() { return operation; }
    }
}