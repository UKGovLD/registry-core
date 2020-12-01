package com.epimorphics.registry.notification;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.Startup;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.*;

public class BacklogNotificationAgent implements NotificationAgent, Startup {
    private final Logger log = LoggerFactory.getLogger(BacklogNotificationAgent.class);
    private final Queue<String> queue = new LinkedList<>();
    private ObjectMapper mapper = new ObjectMapper();

    private File location = new File("/var/opt/ldregistry/notification/backlog");
    private Long limit;
    private NotificationAgent agent;

    public void setLocation(String location) {
        this.location = new File(location);
    }

    public void setLimit(Long limit) {
        this.limit = limit;
    }

    public void setAgent(NotificationAgent agent) {
        this.agent = agent;
    }

    @Override public void startup(App app) {
        if (location.exists()) {
            try {
                FileUtils.cleanDirectory(location);
            } catch (IOException ioe) {
                log.error("Failed to initialize notification backlog.", ioe);
            }
        } else {
            location.mkdirs();
        }
    }

    @Override public void send(Notification notification) throws Exception {
        try {
            clearBacklog();
            agent.send(notification);
        } catch (Exception e) {
            log.info("Failed to send notification for target: " + notification.getTarget() + " - adding to backlog.");
            addToBacklog(notification);
            throw e;
        }
    }

    private void addToBacklog(Notification notification) {
        if (limit == null || queue.size() < limit) {
            try {
                File file = writeToFile(notification);
                queue.add(file.getAbsolutePath());
            } catch (Exception e) {
                log.error("Failed to add notification to backlog.", e);
            }
        } else {
            log.error("Failed to add notification to backlog - backlog is full!");
        }
    }

    private File writeToFile(Notification notification) throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put("topics", String.join(",", notification.getTopics()));
        properties.put("target", notification.getTarget());
        properties.put("operation", notification.getOperation());
        String msg = notification.getMessage();
        if (msg != null) {
            properties.put("message", notification.getMessage());
        }

        File file = new File(location, getFileName(notification));
        mapper.writeValue(file, properties);

        return file;
    }

    private String getFileName(Notification notification) throws Exception {
        String timestamp = Instant.now().toString();
        String target = URLEncoder.encode(notification.getTarget(), "UTF-8");
        return timestamp + "_" + target;
    }

    private synchronized void clearBacklog() throws Exception {
        while (!queue.isEmpty()) {
            String next = queue.peek();
            File file = new File(next);
            Notification notification;
            try {
                notification = readFromFile(file);
            } catch (IOException ioe) {
                log.error("Failed to read notification from backlog.");
                return;
            }

            agent.send(notification);
            queue.remove();
            file.delete();
        }
    }

    private Notification readFromFile(File file) throws IOException {
        JsonNode json = mapper.readTree(file);
        return new JsonNotification(json);
    }

    private static class JsonNotification implements Notification {
        private final JsonNode json;

        JsonNotification(JsonNode json) {
            this.json = json;
        }

        @Override public List<String> getTopics() {
            String[] values = json.get("topics").asText().split(",");
            return Arrays.asList(values);
        }

        @Override public String getMessage() {
            JsonNode node = json.get("message");
            if (node != null) {
                return node.asText();
            } else return null;
        }

        @Override public String getTarget() { return json.get("target").asText(); }
        @Override public String getOperation() { return json.get("operation").asText(); }
    }
}