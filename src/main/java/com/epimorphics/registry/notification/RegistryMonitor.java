package com.epimorphics.registry.notification;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.Startup;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.message.Message;
import com.epimorphics.registry.message.MessagingService;
import com.epimorphics.registry.store.StoreAPI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.List;

public class RegistryMonitor implements Startup {

    private final Logger log = LoggerFactory.getLogger(RegistryMonitor.class);

    private State state;
    private NotificationAgent agent;
    private StoreAPI store;

    public void setState(State state) {
        this.state = state;
    }

    public void setAgent(NotificationAgent agent) {
        this.agent = agent;
    }

    @Override public void startup(App app) {
        Registry reg = app.getA(Registry.class);
        this.store = reg.getStore();

        MessagingService msgSvc = reg.getMessagingService();
        msgSvc.processMessages(this::onRegisterChange);
    }

    private void onRegisterChange(Message msg) {
        String content = extractContent(msg);
        String targetUri = msg.getTarget();
        String operation = msg.getOperation();

        List<String> topics = state.getTopics(targetUri);
        try {
            topics.forEach(topic -> agent.send(topic, content, targetUri, operation));
        } catch (Exception e) {
            log.error("Failed to send notification for monitored register " + targetUri + ".", e);
        }
    }

    private String extractContent(Message msg) {
        Model model = getModel(msg);
        if (model != null) {
            StringWriter writer = new StringWriter();
            model.write(writer, FileUtils.langTurtle);
            return writer.toString();
        }

        return msg.getMessageAsString();
    }

    private Model getModel(Message msg) {
        if (msg.getEntity() != null) {
            // Retrieve full metadata
            store.beginSafeRead();
            try {
                return store.getItem(msg.getTarget(), true).getModel();
            } finally {
                store.endSafeRead();
            }
        } else {
            return msg.getMessageAsModel();
        }
    }

    interface State {
        List<String> getTopics(String targetUri);
    }
}