package com.epimorphics.registry.language;

import com.epimorphics.registry.language.message.FileMessageManager;
import com.epimorphics.registry.language.message.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Multilingual registry support.
 */
public interface LanguageManager {
    /**
     * Determine whether the registry is configured to support multiple languages.
     * @return True if and only if multiple languages are supported.
     */
    Boolean isMultilingual();

    /**
     * @return All of the supported languages.
     */
    List<Language> getLanguages();

    /**
     * Determines whether to use cookies to store users' language preference.
     * @return True if and only if cookies should be used. Otherwise, false.
     */
    Boolean getUseCookies();

    /**
     * @return The default language in which to render messages and registry content.
     */
    String getDefaultLanguage();

    /**
     * @param lang The two-letter code for the language.
     * @return The messages for the requested language, if available. Otherwise, a default language.
     */
    Messages getMessages(String lang);

    class Default implements LanguageManager {
        private final Logger log = LoggerFactory.getLogger(Default.class);
        private final FileMessageManager msgManager = new FileMessageManager();
        private final String defaultLang = "en";

        @Override public Boolean isMultilingual() { return false; }
        @Override public List<Language> getLanguages() { return Collections.emptyList(); }
        @Override public Boolean getUseCookies() { return null; }
        @Override public String getDefaultLanguage() { return defaultLang; }

        @Override public Messages getMessages(String lang) {
            Messages msgs = msgManager.getMessages(defaultLang);
            if (msgs != null) {
                return msgs;
            }

            log.error("Messages for default language (" + defaultLang + ") are not configured.");
            return new Messages.Empty();
        }
    }
}