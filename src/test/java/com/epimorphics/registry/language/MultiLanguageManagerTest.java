package com.epimorphics.registry.language;

import com.epimorphics.appbase.core.App;
import com.epimorphics.registry.language.message.MessageManager;
import com.epimorphics.registry.language.message.Messages;
import org.junit.Test;
import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.instanceOf;

public class MultiLanguageManagerTest {

    private MultiLanguageManager langManager;

    public MultiLanguageManagerTest() {
        Properties en = new Properties();
        en.setProperty("test.msg", "Test message en");
        en.setProperty("test.new", "New message en");

        Properties fr = new Properties();
        fr.setProperty("test.msg", "Test message fr");

        MessageManager msgManager = mock(MessageManager.class);
        when(msgManager.getMessages("en")).thenReturn(new MessagesProperties("en", en));
        when(msgManager.getMessages("fr")).thenReturn(new MessagesProperties("fr", fr));

        this.langManager = new MultiLanguageManager();
        this.langManager.setMessageManager(msgManager);
        this.langManager.startup(mock(App.class));
    }

    @Test
    public void getExistingLanguage_expectSuccess() {
        assertEquals("Test message en", langManager.getMessages("en").get("test.msg"));
        assertEquals("Test message fr", langManager.getMessages("fr").get("test.msg"));
    }

    @Test
    public void getNonExistingLanguage_expectEnglish() {
        assertEquals("Test message en", langManager.getMessages("it").get("test.msg"));
    }

    @Test
    public void getNonExistingLanguage_setDefault_expectDefault() {
        langManager.setDefaultLanguage("fr");
        langManager.startup(mock(App.class));
        assertEquals("Test message fr", langManager.getMessages("gr").get("test.msg"));
    }

    @Test
    public void getNonExistingLanguage_defaultDoesNotExist_expectEmptyMessage() {
        langManager.setDefaultLanguage("pl");
        langManager.startup(mock(App.class));
        assertEquals("", langManager.getMessages("gr").get("test.msg"));
    }

    @Test
    public void getNewMessage_fallsBackToDefaultLanguage() {
        assertEquals("New message en", langManager.getMessages("fr").get("test.new"));
    }
}
