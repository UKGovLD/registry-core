/******************************************************************
 * File:        LibReg.java
 * Created by:  Dave Reynolds
 * Created on:  31 Jan 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.webapi;

import java.util.List;

import com.epimorphics.rdfutil.ModelWrapper;
import com.epimorphics.rdfutil.RDFNodeWrapper;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.store.RegisterEntryInfo;
import com.epimorphics.registry.store.StoreAPI;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.server.core.Service;
import com.epimorphics.server.core.ServiceBase;
import com.epimorphics.server.templates.LibPlugin;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Some supporting methods to help Velocity UI access the registry store.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class LibReg extends ServiceBase implements LibPlugin, Service {

    public StoreAPI getStore() {
        return Registry.get().getStore();
    }
    
    public RDFNodeWrapper getResource(String uri) {
        return wrapNode( getStore().getCurrentVersion(uri, false).getRoot() );
    }
    
    private ModelWrapper wrapModel(Model m) {
        m.setNsPrefixes( Prefixes.get() );
        return new ModelWrapper( m );
    }
    
    private RDFNodeWrapper wrapNode(Resource root) {
        return wrapModel( root.getModel() ).getNode(root);
    }
    
    public List<RegisterEntryInfo> listMembers(Object arg) {
        Register reg = null;;
        if (arg instanceof String) {
            reg = getStore().getCurrentVersion((String)arg, false).asRegister();
        } else if (arg instanceof RDFNodeWrapper) {
            reg = new Register( ((RDFNodeWrapper)arg).asResource() );
        } else if (arg instanceof Register) {
            reg = (Register) arg;
        } else {
            return null;
        }
        return getStore().listMembers(reg);
    }
}
