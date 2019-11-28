package com.epimorphics.registry.notification.stomp;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.Startup;
import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class StompAuthProperties implements Startup {
    private static Logger log = LoggerFactory.getLogger(StompAuthProperties.class);

    private final Properties props;

    public StompAuthProperties() {
        this("/opt/ldregistry/config/stomp/auth.conf");
    }

    StompAuthProperties(String authLoc) {
        this(getProperties(authLoc));
    }

    StompAuthProperties(Properties props) {
        this.props = props;
    }

    private static Properties getProperties(String authLoc) {
        Properties props = new Properties();
        log.info("Configuring STOMP credentials at " + authLoc);
        try (FileReader reader = new FileReader(authLoc)) {
            props.load(reader);
            return props;
        } catch (IOException ioe) {
            log.info("STOMP credentials file not found at " + authLoc + ". Continuing without authentication.");
            return props;
        }
    }

    @Override public void startup(App app) {
        StompJmsConnectionFactory connectionFct = app.getA(StompJmsConnectionFactory.class);
        if (connectionFct != null) {
            connectionFct.setUsername(props.getProperty("username"));
            connectionFct.setPassword(props.getProperty("password"));
        }
    }
}
