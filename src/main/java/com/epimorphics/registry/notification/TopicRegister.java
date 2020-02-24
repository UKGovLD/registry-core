package com.epimorphics.registry.notification;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.Startup;
import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.store.RegisterEntryInfo;
import com.epimorphics.registry.store.StoreAPI;
import com.epimorphics.registry.vocab.RegistryVocab;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopicRegister implements Startup {
    private static final String TOPIC_REGISTER = "/system/topic"; // relative to base URI

    private final Logger log = LoggerFactory.getLogger(MonitorRegister.class);
    private Map<String, String> topicsByUri = new HashMap<>();

    @Override public void startup(App app) {
        Registry reg = app.getA(Registry.class);
        initTopics(reg);
    }

    private synchronized void initTopics(Registry reg) {
        topicsByUri.clear();

        StoreAPI store = reg.getStore();
        String uri = reg.getBaseURI() + TOPIC_REGISTER;

        store.beginSafeRead();
        Description desc = store.getDescription(uri);
        if (desc instanceof Register) {
            Register register = desc.asRegister();
            List<RegisterEntryInfo> members = register.getMembers();
            members.forEach(member -> addTopic(member, store));
        } else {
            log.warn("System register " + uri + " does not exist - unable to register topics.");
        }

        store.endSafeRead();
    }

    private void addTopic(RegisterEntryInfo entry, StoreAPI store) {
        String topicUri = entry.getItemURI();
        Resource root = store.getDescription(entry.getEntityURI()).getRoot();
        if (root.hasProperty(RDF.type, RegistryVocab.Topic)) {
            Statement topicNameStmt = root.getProperty(DCTerms.identifier);
            if (topicNameStmt == null) {
                log.warn("Unable to configure topic " + topicUri + ": Resource must specify a dct:identifier value.");
            } else {
                String name = topicNameStmt.getLiteral().getLexicalForm();
                topicsByUri.put(topicUri, name);
            }
        } else {
            log.warn("Unable to configure topic " + topicUri + ": Resource must have a rdf:type value of reg:Topic.");
        }
    }

    public String getTopicName(String uri) {
        return topicsByUri.get(uri);
    }
}
