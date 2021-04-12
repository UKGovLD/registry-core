package com.epimorphics.registry.language;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.Startup;
import com.epimorphics.registry.language.message.FileMessageManager;
import com.epimorphics.registry.language.message.MessageManager;
import com.epimorphics.registry.language.message.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class DefaultLanguageManager implements LanguageManager, Startup {
    private final Logger log = LoggerFactory.getLogger(DefaultLanguageManager.class);
    private MessageManager msgManager;
    private String defaultLang = "en";
    private Messages defaultMsgs;

    public DefaultLanguageManager init() {
        if (msgManager == null) {
            this.msgManager = new FileMessageManager().init();
        }
        Messages defaultMsgs = msgManager.getMessages(defaultLang);
        if (defaultMsgs == null) {
            log.warn("Messages for default language (" + defaultLang + ") are not configured.");
            defaultMsgs = new Messages.Empty();
        }

        this.defaultMsgs = defaultMsgs;
        return this;
    }

    public void setDefaultLang(String lang) {
        this.defaultLang = lang;
    }

    public void setMessageManager(MessageManager msgManager) {
        this.msgManager = msgManager;
    }

    @Override public void startup(App app) {
        init();
    }

    @Override public Boolean isMultilingual() {
        return false;
    }

    @Override public List<Language> getLanguages() {
        return Collections.emptyList();
    }

    @Override public Boolean getUseCookies() {
        return false;
    }

    @Override public String getDefaultLanguage() {
        return defaultLang;
    }

    @Override public Messages getMessages(String lang) {
        return defaultMsgs;
    }
}
