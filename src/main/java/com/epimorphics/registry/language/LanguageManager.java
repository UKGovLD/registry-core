package com.epimorphics.registry.language;

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

    class Default implements LanguageManager {
        @Override public Boolean isMultilingual() { return false; }
        @Override public List<Language> getLanguages() { return Collections.emptyList(); }
        @Override public Boolean getUseCookies() { return false; }
    }
}