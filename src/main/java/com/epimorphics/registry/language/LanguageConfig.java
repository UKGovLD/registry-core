package com.epimorphics.registry.language;

import java.util.List;

/**
 * Maintains the state of the registry's supported languages.
 */
interface LanguageConfig {
    /**
     * @return The list of languages supported by the registry.
     */
    List<Language> getLanguages();
}
