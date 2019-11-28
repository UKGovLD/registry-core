package com.epimorphics.registry.notification;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.Startup;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.message.Message;
import com.epimorphics.registry.message.MessagingService;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;

public class RegistryMonitor implements Startup {

    private final Logger log = LoggerFactory.getLogger(RegistryMonitor.class);

    private State state;
    private NotificationAgent agent;

    public void setState(State state) {
        this.state = state;
    }

    public void setAgent(NotificationAgent agent) {
        this.agent = agent;
    }

    @Override public void startup(App app) {
        Registry reg = app.getA(Registry.class);
        MessagingService msgSvc = reg.getMessagingService();
        msgSvc.processMessages(this::onRegisterChange);
    }

    private void onRegisterChange(Message msg) {
        String content = extractContent(msg);
        String targetUri = msg.getTarget();
        String operation = msg.getOperation();

        if (state.isMonitored(targetUri)) {
            try {
                agent.send(content, targetUri, operation);
            } catch (Exception e) {
                log.error("Failed to send notification for monitored register " + targetUri + ".", e);
            }
        }
    }

    private String extractContent(Message msg) {
        Model model = msg.getMessageAsModel();
        if (model != null) {
            StringWriter writer = new StringWriter();
            model.write(writer, FileUtils.langTurtle);
            return writer.toString();
        }

        return msg.getMessageAsString();
    }

    interface State {
        Boolean isMonitored(String targetUri);
    }
}

