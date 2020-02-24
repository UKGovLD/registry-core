package com.epimorphics.registry.notification;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.Startup;
import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.message.MessagingService;
import com.epimorphics.registry.message.ProcessIfChanges;
import com.epimorphics.registry.store.RegisterEntryInfo;
import com.epimorphics.registry.store.StoreAPI;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.util.NameUtils;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class MonitorRegister implements MonitorConfig, Startup {
    private static final String MONITOR_REGISTER = "/system/monitor"; // relative to base URI

    private final Logger log = LoggerFactory.getLogger(MonitorRegister.class);
    private final List<RegisterMonitor> registers = new ArrayList<>();
    private List<String> defaultTopics = Collections.emptyList();
    private TopicRegister topicRegister;

    public void setDefaultTopic(String topics) {
        this.defaultTopics = Arrays.asList(topics.split(","));
    }

    public void setTopicRegister(TopicRegister topicRegister) {
        this.topicRegister = topicRegister;
    }

    @Override public void startup(App app) {
        Registry reg = app.getA(Registry.class);
        initMonitor(reg);

        String register = reg.getBaseURI() + MONITOR_REGISTER;
        MessagingService msgSvc = reg.getMessagingService();
        MessagingService.Process onMonitorChange = new ProcessIfChanges(msg -> initMonitor(reg), register);
        msgSvc.processMessages(onMonitorChange);
    }

    @Override public List<String> getTopics(String targetUri) {
        return registers.stream()
                .filter(register -> register.monitors(targetUri))
                .flatMap(register -> register.topics().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    private synchronized void initMonitor(Registry reg) {
        registers.clear();

        StoreAPI store = reg.getStore();
        String uri = reg.getBaseURI() + MONITOR_REGISTER;

        store.beginSafeRead();
        Description desc = store.getDescription(uri);
        if (desc instanceof Register) {
            Register register = desc.asRegister();
            List<RegisterEntryInfo> members = register.getMembers();
            members.forEach(member -> addMonitoredRegister(member, store));
        } else {
            log.warn("System register " + uri + " does not exist - unable to monitor changes.");
        }

        store.endSafeRead();
    }

    private void addMonitoredRegister(RegisterEntryInfo entry, StoreAPI store) {
        Resource root = store.getDescription(entry.getEntityURI()).getRoot();
        if (root.hasProperty(RDF.type, RegistryVocab.MonitorSpec)) {
            String register = getRegisterUri(root);
            if (register != null) {
                List<String> excludes = getExclusions(root);
                List<String> topics = getTopics(root);
                RegisterMonitor monitor = new RegisterMonitor(register, excludes, topics);

                registers.add(monitor);

                log.info("Monitoring register: " + register);
            } else {
                log.warn("Unable to add monitor entry " + entry.getItemURI() + ": Resource must specify a reg:monitors value.");
            }
        } else {
            log.warn("Unable to add monitor entry " + entry.getItemURI() + ": Resource must have a rdf:type value of reg:MonitorSpec.");
        }
    }

    private String getRegisterUri(Resource root) {
        Statement monitors = root.getProperty(RegistryVocab.monitors);
        if (monitors == null) {
            return null;
        }

        String register = monitors.getObject().asResource().getURI();
        return NameUtils.stripLastSlash(register);
    }

    private List<String> getExclusions(Resource root) {
        return root.listProperties(RegistryVocab.ignores)
                .mapWith(stmt -> stmt.getObject().asResource().getURI())
                .mapWith(NameUtils::stripLastSlash)
                .toList();
    }

    private List<String> getTopics(Resource root) {
        StmtIterator topicsIt = root.listProperties(RegistryVocab.notifies);
        if (!topicsIt.hasNext()) {
            return defaultTopics;
        }

        return topicsIt.mapWith(stmt -> {
            RDFNode topicRef = stmt.getObject();
            if (topicRef.isLiteral()) {
                return topicRef.asLiteral().getLexicalForm();
            } else {
                String topicUri = topicRef.asResource().getURI();
                if (topicRegister == null) {
                    log.error("Unable to resolve topic " + topicUri + ": Topic register is not configured.");
                    return null;
                }

                return topicRegister.getTopicName(topicUri);
            }
        }).filterDrop(Objects::isNull).toList();
    }

    private static class RegisterMonitor {
        private final String register;
        private final List<String> excludes;
        private final List<String> topics;

        RegisterMonitor(String register, List<String> excludes, List<String> topics) {
            this.register = register;
            this.excludes = excludes;
            this.topics = topics;
        }

        Boolean monitors(String targetUri) {
            return isSubRegister(register, targetUri) && !excludes(targetUri);
        }

        List<String> topics() {
            return topics;
        }

        private Boolean excludes(String targetUri) {
            return excludes.stream().anyMatch(exclude -> isSubRegister(exclude, targetUri));
        }

        private Boolean isSubRegister(String parent, String child) {
            return child.equals(parent) || child.startsWith(parent + "/") || child.replace("/_", "/").equals(parent);
        }
    }
}
