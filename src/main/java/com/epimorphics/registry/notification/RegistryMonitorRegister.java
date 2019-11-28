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
                targetUri.equals(register) || targetUri.startsWith(register + "/")
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
            members.stream().map(RegisterEntryInfo::getEntityURI).forEach(registers::add);
        } else {
            log.warn("System register " + uri + " does not exist - unable to monitor changes.");
        }

        store.endSafeRead();
    }
}
