/******************************************************************
 * File:        ForwardingServiceImpl.java
 * Created by:  Dave Reynolds
 * Created on:  18 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.core;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.ForwardingRecord.Type;
import com.epimorphics.registry.util.Trie;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.server.core.Service;
import com.epimorphics.server.core.ServiceBase;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Default forwarding service implementation. Uses a trie to store and match
 * forwarding instructions and passes proxy instructions to a front-end nginx
 * instance.
 * <p>
 * Configuration parameters:
 * <ul>
 *   <li><strong>proxyConfDir</strong> the directory in which to generate proxy-foo.conf files for nginx configuration</li>
 *   <li><strong>proxyRestartScript</strong> a script to force ningx instance to reconsult the proxy config files, must be runnable as sudo without a password</li>
 * </ul>
 * </p>
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ForwardingServiceImpl extends ServiceBase implements ForwardingService, Service {
    static Logger log = LoggerFactory.getLogger(ForwardingServiceImpl.class);

    public static final String PROXY_CONF_DIR_PARAM = "proxyConfDir";
    public static final String PROXY_RESTART_SCRIPT_PARAM = "proxyRestartScript";

    protected String confDir;
    protected String script;

    protected Trie<ForwardingRecord> trie = new Trie<ForwardingRecord>();
    protected Map<String, ForwardingRecord> proxyForwards = new HashMap<String, ForwardingRecord>();
    boolean configUpdateNeeded = false;

    @Override
    public void init(Map<String, String> config, ServletContext context) {
        super.init(config, context);

        confDir = getRequiredFileParam(PROXY_CONF_DIR_PARAM);
        script = getRequiredFileParam(PROXY_RESTART_SCRIPT_PARAM);
    }

    @Override
    public void update(RegisterItem item) {
        if (item.getStatus().isAccepted()) {
            register( recordFor(item) );
        } else {
            String loc = item.getEntity().getURI();
            String base = Registry.get().getBaseURI();
            if (loc.startsWith(base)) {
                loc = loc.substring(base.length());
                unregister(loc);
            }
        }
        updateConfig();
    }

    public ForwardingRecord recordFor(RegisterItem item) {
        Resource record = item.getEntity();
        ForwardingRecord.Type type = Type.FORWARD;
        if (record.hasProperty(RDF.type, RegistryVocab.FederatedRegister)) {
            type = Type.FEDERATE;
        } else if (record.hasProperty(RDF.type, RegistryVocab.DelegatedRegister)) {
            type = Type.DELEGATE;
        }
        String target = record.getPropertyResourceValue(RegistryVocab.delegationTarget).getURI();
        if (type == Type.DELEGATE) {
            DelegationRecord dr = new DelegationRecord(record.getURI(), target, type);
            Resource s = record.getPropertyResourceValue(RegistryVocab.enumerationSubject);
            if (s != null) dr.setSubject(s);
            Resource p = record.getPropertyResourceValue(RegistryVocab.enumerationPredicate);
            if (p != null) dr.setPredicate(p);
            Resource o = record.getPropertyResourceValue(RegistryVocab.enumerationObject);
            if (o != null) dr.setObject(o);
            return dr;
        } else {
            ForwardingRecord fr = new ForwardingRecord(record.getURI(), target, type);
            int code = RDFUtil.getIntValue(record, RegistryVocab.forwardingCode, -1);
            if (code != -1) {
                fr.setForwardingCode( code );
            }
            return fr;
        }
    }

    @Override
    public synchronized void register(ForwardingRecord record) {
        String loc = record.getLocation();
        String base = Registry.get().getBaseURI();
        if (loc.startsWith(base)) {
            loc = loc.substring(base.length());
        } else {
            log.error("Attempted to forward an external URL, will be ignored: " + loc);
            return;
        }
        log.info("Registering delegation path at " + loc + " -> " + record.getTarget() + " [" + record.getForwardingCode() + "]");
        if (record.getForwardingCode() == 200) {
            proxyForwards.put(loc, record);
            configUpdateNeeded = true;
        }
        trie.register(loc, record);
    }

    @Override
    public synchronized void unregister(String path) {
        log.info("Unregistering delegation path: " + path);
        trie.unregister(path);
        if (proxyForwards.containsKey(path)) {
            proxyForwards.remove(path);
            configUpdateNeeded = true;
        }
    }

    @Override
    public synchronized MatchResult match(String path) {
        Trie.MatchResult<ForwardingRecord> mr = trie.match(path);
        if (mr != null) {
            return new MatchResult(mr.getMatch(), mr.getPathRemainder());
        } else {
            return null;
        }
    }

    @Override
    public synchronized void updateConfig() {
        if (configUpdateNeeded) {
            configUpdateNeeded = false;
            // TODO configure front end proxy, registrations should record 200 cases as a separate list
        }
    }


}
