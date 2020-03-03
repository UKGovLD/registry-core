package com.epimorphics.registry.language;

import com.epimorphics.registry.language.message.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class MessageProperties implements Messages {
    private final Logger log = LoggerFactory.getLogger(MessageProperties.class);
    private final Properties props;

    public MessageProperties(Properties props) {
        this.props = props;
    }

    @Override public String getLang() {
        return props.getProperty("lang");
    }

    @Override public String get(String id, String... params) {
        String msgFormat = props.get(id).toString();
        if (msgFormat == null) {
            log.warn("A message with id: " + id + " was expected for language: " + getLang() + " but was not found.");
            return "[ MESSAGE MISSING ]";
        }

        return String.format(msgFormat, (Object) params);
    }
}
