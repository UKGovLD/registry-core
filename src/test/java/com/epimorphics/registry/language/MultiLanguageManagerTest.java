package com.epimorphics.registry.language;

import com.epimorphics.registry.language.message.MessageManager;
import com.epimorphics.registry.language.message.Messages;
import org.junit.Test;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.instanceOf;

public class MultiLanguageManagerTest {

    private MultiLanguageManager langManager;
    private Messages enMessages = mock(Messages.class);
    private Messages frMessages = mock(Messages.class);

    public MultiLanguageManagerTest() throws IOException {
        MessageManager msgManager = mock(MessageManager.class);
        when(msgManager.getMessages("en")).thenReturn(enMessages);
        when(msgManager.getMessages("fr")).thenReturn(frMessages);

        this.langManager = new MultiLanguageManager(msgManager);
    }

    @Test
    public void getExistingLanguage_expectSuccess() {
        assertEquals(enMessages, langManager.getMessages("en"));
        assertEquals(frMessages, langManager.getMessages("fr"));
    }

    @Test
    public void getNonExistingLanguage_expectDefaultDefault() {
        assertEquals(enMessages, langManager.getMessages("it"));
    }

    @Test
    public void getNonExistingLanguage_defaultExists_expectSetDefault() {
        langManager.setDefaultLanguage("fr");
        assertEquals(frMessages, langManager.getMessages("gr"));
    }

    @Test
    public void getNonExistingLanguage_defaultDoesNotExist_expectEmptyMessage() {
        langManager.setDefaultLanguage("pl");
        assertThat(langManager.getMessages("jp"), instanceOf(Messages.Empty.class));
    }

}
