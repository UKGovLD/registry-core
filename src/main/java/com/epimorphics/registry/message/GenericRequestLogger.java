/******************************************************************
 * File:        GenericRequestLogger.java
 * Created by:  Dave Reynolds
 * Created on:  21 Nov 2016
 * 
 * (c) Copyright 2016, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.message;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.Command.Operation;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.util.RunShell;
import com.epimorphics.util.EpiException;

public class GenericRequestLogger implements RequestLogger {
    Registry registry;
    String   logDir;
    String   notificationScript;
    
    public void setRegistry(Registry registry) {
        this.registry = registry;
    }
    
    public void setLogDir(String logDir) {
        this.logDir = logDir;
    }
    
    public void setNotificationScript(String notificationScript) {
        this.notificationScript = notificationScript;
    }
    
    @Override
    public String writeLog(Command command) throws IOException {
        String logfile = logDir + File.separator + String.format( "on-%s-%s.ttl",
                new SimpleDateFormat("dd-MMM-yyyy-HH-mm-ss").format(new Date()), command.getOperation().name() );
        BufferedWriter writer = new BufferedWriter( new FileWriterWithEncoding(logfile, StandardCharsets.UTF_8) );
        try {
            String operation = command.getOperation().name();
            String target = command.getTarget().substring( registry.getBaseURI().length() + 1 );
            MultivaluedMap<String, String> parameters = command.getParameters();
            StringBuffer specline = new StringBuffer();
            specline.append("# ");
            specline.append(operation + "|");
            specline.append(target + "?");
            boolean started = false;
            for (String key : parameters.keySet()) {
                for (String value : parameters.get(key)) {
                    if (started) {
                        specline.append("&");
                    } else {
                        started = true;
                    }
                    specline.append(key + "=" + value);
                }
            }
            specline.append("\n");
            writer.write( specline.toString() );
            if (command.getPayload() != null) {
                command.getPayload().write(writer, "Turtle");
            }
        } finally {
            writer.close();
        }
        if ( notificationScript != null ) {
            new RunShell(notificationScript).run( logfile );
        }
        return logfile;
    }

    @Override
    public Command getLog(InputStream in) throws IOException {
        try {
            BufferedReader reader = new BufferedReader( new InputStreamReader(in, StandardCharsets.UTF_8) );
            String commandLine = reader.readLine();
            Matcher matcher = COMMAND_PATTERN.matcher(commandLine);
            if (matcher.matches()) {
                String op = matcher.group(1);
                String target = matcher.group(2);
                String query = matcher.group(3);
                Command command = registry.make(Operation.valueOf(op), target, query);
                Model payload = ModelFactory.createDefaultModel();
                payload.read(reader, registry.getBaseDomain() + target, "Turtle");
                command.setPayload(payload);
                command.setRequestor("Log replay");
                return command;
            } else {
                throw new EpiException("Illegal command log, command line format doesn't match");
            }
        } finally {
            in.close();
        }
    }
    static Pattern COMMAND_PATTERN = Pattern.compile("^# (\\w*)\\|([^?]*)\\?(.*)$");

    
}
