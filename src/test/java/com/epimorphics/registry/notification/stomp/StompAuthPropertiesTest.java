package com.epimorphics.registry.notification.stomp;

import com.epimorphics.appbase.core.App;
import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import static org.mockito.Mockito.*;

public class StompAuthPropertiesTest {
    private App app = mock(App.class);
    private StompJmsConnectionFactory connectionFct = mock(StompJmsConnectionFactory.class);

    @BeforeEach
    public void before() {
        when(app.getA(StompJmsConnectionFactory.class)).thenReturn(connectionFct);
    }

    @Test
    public void readsPropertiesFromFile() throws IOException {
        File tmp = File.createTempFile("junit", "stompAuthProperties");
        new FileWriter(tmp)
                .append("username = admin\n")
                .append("password = 123pass")
                .close();

        StompAuthProperties auth = new StompAuthProperties(tmp.getAbsolutePath());
        auth.startup(app);

        verify(connectionFct).setUsername("admin");
        verify(connectionFct).setPassword("123pass");
    }

    @Test
    public void returnsNullForEmptyProperties() {
        Properties props = new Properties();
        StompAuthProperties auth = new StompAuthProperties(props);
        auth.startup(app);

        verify(connectionFct).setUsername(null);
        verify(connectionFct).setPassword(null);
    }
}