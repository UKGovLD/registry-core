package com.epimorphics.registry.language.message;

/**
 * Manages the messaging implementations for each supported languages.
 */
public interface MessageManager {
    /**
     * Returns the messages for a given language.
     * @param lang The requested language.
     * @return The messages for the given language, if they exist. Otherwise, null.
     */
    Messages getMessages(String lang);
}
