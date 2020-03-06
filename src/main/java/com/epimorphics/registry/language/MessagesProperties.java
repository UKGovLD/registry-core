package com.epimorphics.registry.language;

import com.epimorphics.registry.language.message.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Properties;

public class MessagesProperties implements Messages {
    private final Logger log = LoggerFactory.getLogger(MessagesProperties.class);
    private final String lang;
    private final Properties props;

    public MessagesProperties(String lang, Properties props) {
        this.lang = lang;
        this.props = props;
    }

    @Override public String get(String id, String... params) {
        String msgFormat = props.getProperty(id);
        if (msgFormat == null) {
            log.warn("A message with id: " + id + " was expected for language: " + lang + " but was not found.");
            return "[ MESSAGE MISSING ]";
        }

        MessageFormat messageFormat = new MessageFormat(msgFormat);
        if (messageFormat.getFormats().length != params.length) {
            log.error("A message with id: " + id + " expects " + messageFormat.getFormats().length + " parameters but " + params.length + " are provided.");
        }

        return messageFormat.format((Object[]) params);
    }
}
