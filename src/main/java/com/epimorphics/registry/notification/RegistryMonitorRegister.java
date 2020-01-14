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
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RegistryMonitorRegister implements RegistryMonitor.State, Startup {
    private static final String MONITOR_REGISTER = "/system/monitor"; // relative to base URI

    private Logger log = LoggerFactory.getLogger(RegistryMonitorRegister.class);
    private final List<String> registers = new ArrayList<>();

    @Override public void startup(App app) {
        Registry reg = app.getA(Registry.class);
        initMonitor(reg);

        String register = reg.getBaseURI() + MONITOR_REGISTER;
        MessagingService msgSvc = reg.getMessagingService();
        MessagingService.Process onMonitorChange = new ProcessIfChanges(msg -> initMonitor(reg), register);

        msgSvc.processMessages(onMonitorChange);
    }

    @Override public Boolean isMonitored(String targetUri) {
        return registers.stream().anyMatch(register ->
                targetUri.equals(register) || targetUri.startsWith(register + "/") || targetUri.replace("/_", "/").equals(register)
        );
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
            Statement stmt = root.getProperty(RegistryVocab.monitors);
            if (stmt != null) {
                String monitor = stmt.getObject().asResource().getURI();
                registers.add(monitor);
                log.info("Monitoring register: " + monitor);
            } else {
                log.warn("Unable to add monitor entry " + entry.getItemURI() + ": Resource must specify a reg:monitors value.");
            }
        } else {
            log.warn("Unable to add monitor entry " + entry.getItemURI() + ": Resource must have a rdf:type value of reg:MonitorSpec.");
        }
    }
}
