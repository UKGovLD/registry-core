/******************************************************************
 * File:        ForwardingServiceImpl.java
 * Created by:  Dave Reynolds
 * Created on:  18 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *****************************************************************/

package com.epimorphics.registry.core;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.Filter;
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

    static final String PROXY_FILE = "proxy-registry.conf";

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
        } else {
            if (proxyForwards.containsKey(loc)) {
                // Switch from a 200 to a non-200 forward
                proxyForwards.remove(loc);
                configUpdateNeeded = true;
            }
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
            try {
                BufferedWriter file = new BufferedWriter( new FileWriter( confDir + "/" + PROXY_FILE ) );
                for (String path : proxyForwards.keySet()) {
                    file.write( String.format("location %s {\n    proxy_pass %s ;\n}\n", path, proxyForwards.get(path).getTarget()));
                }
                file.close();

                Process process = Runtime.getRuntime().exec(new String[]{ "/usr/bin/sudo", script});
                String errors = "";
                InputStream errorstream = process.getErrorStream();
                if (errorstream != null) {
                    errors =  FileManager.get().readWholeFileAsUTF8(errorstream);
                    errorstream.close();
                }
                int exitcode = process.waitFor();
                if (exitcode != 0) {
                    log.error("Failed to update nginx proxy config (code: " + exitcode + ") " + errors);
                } else {
                    log.info("Updated proxy configuration");
                }
            } catch (Exception e) {
                log.error("Fail to write or invoke new proxy configuration", e);
            }
            configUpdateNeeded = false;
        }
    }

    @Override
    public List<DelegationRecord> listDelegations(String path) {
        List<ForwardingRecord> records = trie.findAll(path, new Filter<ForwardingRecord>() {
            @Override
            public boolean accept(ForwardingRecord o) {
                return o instanceof DelegationRecord;
            }
        });
        List<DelegationRecord> results = new ArrayList<DelegationRecord>(records.size());
        for (ForwardingRecord rec : records) {
            results.add( (DelegationRecord)rec );
        }
        return results;
    }


}
