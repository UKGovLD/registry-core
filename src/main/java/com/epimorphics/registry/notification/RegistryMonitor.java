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

    private Config config;
    private NotificationAgent agent;
    private StoreAPI store;

    public void setConfig(Config config) {
        this.config = config;
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
        String targetUri = msg.getTarget();
        String operation = msg.getOperation();
        List<String> topics = config.getTopics(targetUri);
        if (!topics.isEmpty()) { // If there are no topics to send to, then the entry is not monitored
            String content = extractContent(msg);
            Notification notification = new Notification.Base(topics, content, targetUri, operation);
            try {
                agent.send(notification);
            } catch (Exception e) {
                log.error("Failed to send notification for monitored register item " + targetUri + ".", e);
            }
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
            } catch (Exception e) {
                log.error("Failed to read item metadata from store - reverting to original message.");
                return msg.getMessageAsModel();
            } finally {
                store.endSafeRead();
            }
        } else {
            return msg.getMessageAsModel();
        }
    }

    /**
     * Defines the state of the registry monitoring configuration.
     */
    interface Config {
        /**
         * Determine the topics which should be notified when the item with the given URI changes.
         * @param targetUri The URI of a register item to check for monitors.
         * @return The topics to be notified. If empty, the item is not monitored and no notification is required.
         */
        List<String> getTopics(String targetUri);
    }
}