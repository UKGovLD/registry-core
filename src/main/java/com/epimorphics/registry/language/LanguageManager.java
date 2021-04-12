package com.epimorphics.registry.language;

import com.epimorphics.registry.language.message.Messages;

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

}