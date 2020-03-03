package com.epimorphics.registry.language;

import com.epimorphics.registry.language.message.FileMessageManager;
import com.epimorphics.registry.language.message.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MultiLanguageManager implements LanguageManager {
    private final Logger log = LoggerFactory.getLogger(MultiLanguageManager.class);
    private FileMessageManager msgManager = new FileMessageManager();

    private String defaultLang = "en";
    private Boolean useCookies = false;
    private LanguageConfig config;

    public void setUseCookies(Boolean useCookies) {
        this.useCookies = useCookies;
    }

    @Override public Boolean getUseCookies() {
        return useCookies;
    }

    public void setDefaultLanguage(String defaultLang) {
        this.defaultLang = defaultLang;
    }

    @Override public String getDefaultLanguage() {
        return defaultLang;
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

    @Override public Messages getMessages(String lang) {
        Messages msgs = msgManager.getMessages(lang);
        if (msgs != null) {
            return msgs;
        }

        msgs = msgManager.getMessages(defaultLang);
        if (msgs != null) {
            return msgs;
        }

        log.error("Messages for language " + lang + " not found, and default language (" + defaultLang + ") messages are not configured.");
        return new Messages.Empty();
    }
}
