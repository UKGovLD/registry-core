package com.epimorphics.registry.language;

import java.util.List;

public class LanguageManagerImpl implements LanguageManager {
    private LanguageConfig config;

    public void setConfig(LanguageConfig config) {
        this.config = config;
    }

    public Boolean isMultilingual() {
        return getLanguages().size() > 1;
    }

    public List<Language> getLanguages() {
        return config.getLanguages();
    }
}
