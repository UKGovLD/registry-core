package com.epimorphics.registry.language;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.Startup;
import com.epimorphics.registry.language.message.FileMessageManager;
import com.epimorphics.registry.language.message.MessageManager;
import com.epimorphics.registry.language.message.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MultiLanguageManager implements LanguageManager, Startup {
    private final Logger log = LoggerFactory.getLogger(MultiLanguageManager.class);
    private MessageManager msgManager;

    private String defaultLang = "en";
    private Boolean useCookies = false;
    private LanguageConfig config;
    private Messages defaultMsgs;

    public void setMessageManager(MessageManager msgManager) {
        this.msgManager = msgManager;
    }

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

    @Override public void startup(App app) {
        if (msgManager == null) {
            msgManager = new FileMessageManager().init();
        }
        Messages defaultMsgs = msgManager.getMessages(defaultLang);
        if (defaultMsgs == null) {
            log.error("Messages for default language (" + defaultLang + ") are not configured.");
            defaultMsgs = new Messages.Empty();
        }

        this.defaultMsgs = defaultMsgs;
    }

    @Override public Messages getMessages(String lang) {
        if (lang.equals(defaultLang)) {
            return defaultMsgs;
        } else {
            Messages msgs = msgManager.getMessages(lang);
            if (msgs == null) {
                return defaultMsgs;
            } else {
                return new Messages.WithDefault(msgs, defaultMsgs);
            }
        }
    }
}
