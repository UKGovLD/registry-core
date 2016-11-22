/******************************************************************
 * File:        RequestLogger.java
 * Created by:  Dave Reynolds
 * Created on:  21 Nov 2016
 * 
 * (c) Copyright 2016, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.message;

import java.io.IOException;
import java.io.InputStream;

import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.Registry;

/**
 * Support for logging update requests to allow replay and replication.
 * The configured instance will define where logs go and any actions to trigger.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface RequestLogger {
    
    public void setRegistry(Registry registry);
    
    /**
     * Log the command to a configured log directory.
     * Returns the name of the created log file
     */
    public String writeLog(Command command) throws IOException;

    /**
     * Reconstruct a command from the a log entry originally created with writeLog
     */
    public Command getLog(InputStream in) throws IOException;
    
}
