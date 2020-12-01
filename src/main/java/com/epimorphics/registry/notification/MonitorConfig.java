package com.epimorphics.registry.notification;

import java.util.List;

/**
 * Defines the state of the registry monitoring configuration.
 */
interface MonitorConfig {
    /**
     * Determine the topics which should be notified when the item with the given URI changes.
     * @param targetUri The URI of a register item to check for monitors.
     * @return The topics to be notified. If empty, the item is not monitored and no notification is required.
     */
    List<String> getTopics(String targetUri);
}
