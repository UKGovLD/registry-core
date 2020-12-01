package com.epimorphics.registry.language;

import java.util.List;

public class MultiLanguageManager implements LanguageManager {
    private Boolean useCookies = false;
    private LanguageConfig config;

    public void setUseCookies(Boolean useCookies) {
        this.useCookies = useCookies;
    }

    public void setConfig(LanguageConfig config) {
        this.config = config;
    }

    public Boolean isMultilingual() {
        return getLanguages().size() > 1;
    }

    public List<Language> getLanguages() {
        return config.getLanguages();
    }

    public Boolean getUseCookies() {
        return useCookies;
    }
}
