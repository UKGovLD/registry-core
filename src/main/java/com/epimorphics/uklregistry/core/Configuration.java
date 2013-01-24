/******************************************************************
 * File:        Configuration.java
 * Created by:  Dave Reynolds
 * Created on:  21 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.core;

import java.util.Map;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.server.core.Service;
import com.epimorphics.server.core.ServiceBase;
import com.epimorphics.server.core.ServiceConfig;
import com.epimorphics.uklregistry.store.Description;
import com.epimorphics.uklregistry.store.Register;
import com.epimorphics.uklregistry.store.StoreAPI;
import com.epimorphics.uklregistry.vocab.Registry;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Encapsulates configuration information for the registry such as base URI.
 * Parameters can be set by the web.xml entry for this service.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Configuration extends ServiceBase implements Service {
    static final Logger log = LoggerFactory.getLogger( Configuration.class );

    public static final String BASE_URI_PARAM = "baseURI";
    public static final String SERVICE_NAME = "configuration";
    public static final String ROOT_REGISTER_FILE_PARAM = "rootRegisterSpec";

    @Override
    public void init(Map<String, String> config, ServletContext context) {
        this.config = config;
    }

    @Override
    public void postInit() {
        StoreAPI store = CommandFactory.get().getStore();

        Register root = store.getRegister(getBaseURI() + "/");
        if (root == null) {
            // Blank store, need to install a root register
            for(String rootRegisterSrc : getRequiredFileParam(ROOT_REGISTER_FILE_PARAM).split("\\|")) {
                Model model = FileManager.get().loadModel(rootRegisterSrc);
                ResIterator ri = model.listSubjectsWithProperty(RDF.type, Registry.Register);
                if (ri.hasNext()) {
                    Description rootRegister = new Description();
                    rootRegister.setRoot( ri.next() );
                    store.storeDescription(rootRegister);
                } else {
                    // Simple file such as a vocab
                    Description vocab = new Description();
                    vocab.setRoot( model.createResource("file:" + rootRegisterSrc) );
                    store.storeDescription(vocab);
                }
            }
            log.info("Installed bootstrap root register");
        }

    }

    static String baseURI;     // Cache since we look this up a *lot*
    public static String getBaseURI() {
        if (baseURI == null) {
            baseURI = get().getRequiredParam(BASE_URI_PARAM);
        }
        return baseURI;
    }

    public static Configuration get() {
        return (Configuration)ServiceConfig.get().getService(SERVICE_NAME);
    }
}
