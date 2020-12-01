package com.epimorphics.registry.language;

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
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LanguageRegister implements LanguageConfig, Startup {
    private static final String REGISTER = "/system/language";

    private Logger log = LoggerFactory.getLogger(LanguageRegister.class);
    private List<Language> languages = new ArrayList<>();

    @Override public void startup(App app) {
        Registry reg = app.getA(Registry.class);
        initLanguages(reg);

        String register = reg.getBaseURI() + REGISTER;
        MessagingService msgSvc = reg.getMessagingService();
        MessagingService.Process onMonitorChange = new ProcessIfChanges(msg -> initLanguages(reg), register);

        msgSvc.processMessages(onMonitorChange);
    }

    public synchronized void initLanguages(Registry reg) {
        List<Language> nextLanguages = new ArrayList<>();

        String uri = reg.getBaseURI() + REGISTER;
        StoreAPI store = reg.getStore();
        store.beginSafeRead();

        try {
            Description desc = store.getDescription(uri);
            if (desc instanceof Register) {
                Register register = desc.asRegister();
                List<RegisterEntryInfo> members = register.getMembers();
                members.forEach(entry -> addLanguage(entry, store, nextLanguages));
                this.languages = nextLanguages;
            } else {
                log.warn("System register " + uri + " does not exist - unable to configure languages.");
            }
        } finally {
            store.endSafeRead();
        }
    }

    private void addLanguage(RegisterEntryInfo entry, StoreAPI store, List<Language> languages) {
        Resource root = store.getDescription(entry.getEntityURI()).getRoot();
        if (root.hasProperty(RDF.type, RegistryVocab.Language)) {
            Statement langStmt = root.getProperty(RegistryVocab.languageCode);
            if (langStmt != null) {
                String lang = langStmt.getObject().asLiteral().getLexicalForm();
                String label = getLabel(root, lang);
                languages.add(new Language.Base(lang, label));
                log.info("Registered language: " + label + " (" + lang + ")");
            } else {
                log.warn("Unable to add language entry " + entry.getItemURI() + ": Resource must specify a dbo:languageCode value.");
            }
        } else {
            log.warn("Unable to add language entry " + entry.getItemURI() + ": Resource must have a rdf:type value of dbo:Language.");
        }

    }

    private String getLabel(Resource root, String lang) {
        Statement nativeLabel = root.getProperty(RDFS.label, lang);
        if (nativeLabel != null) {
            return nativeLabel.getObject().asLiteral().getLexicalForm();
        }

        Statement anyLabel = root.getProperty(RDFS.label);
        if (anyLabel != null) {
            return anyLabel.getObject().asLiteral().getLexicalForm();
        }

        return lang;
    }

    public List<Language> getLanguages() {
        return languages;
    }
}
