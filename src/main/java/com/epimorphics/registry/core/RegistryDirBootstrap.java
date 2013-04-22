/******************************************************************
 * File:        RegistryDirBootstrap.java
 * Created by:  Dave Reynolds
 * Created on:  22 Apr 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.util.FileUtil;


/**
 * Checks the configured root directory to ensure it has all the 
 * configuration elements we expect. Any missing ones are initialized
 * by copying files from the webapp resources. Should be run before
 * the services configuration starts up.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class RegistryDirBootstrap implements ServletContextListener {
    static Logger log = LoggerFactory.getLogger(RegistryDirBootstrap.class);

    public static final String ROOT_DIR_PARAM = "registry-file-root";
    
    String filebase;
    String fileRoot;
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        filebase = withTrailingSlash(context.getRealPath("/"));
        
        fileRoot = withTrailingSlash(context.getInitParameter(ROOT_DIR_PARAM));

            ensureDirectory("config");
            ensureFile("config/user.ini", "WEB-INF/user.ini");
            ensureFile("config/root-register.ttl", "WEB-INF/root-register.ttl");
            ensureDirectory("boot", "WEB-INF/boot");
            ensureDirectory("ui/css", "WEB-INF/ui/css");
            ensureDirectory("ui/images", "WEB-INF/ui/images");
            ensureDirectory("ui/img", "WEB-INF/ui/img");
            ensureDirectory("ui/js", "WEB-INF/ui/js");
            ensureDirectory("templates", "WEB-INF/templates");
            ensureFile("proxy-conf.sh", "WEB-INF/proxy-conf.sh");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // nothing
    }

    private void ensureDirectory(String dir) {
        FileUtil.ensureDir(fileRoot + dir);
    }
    
    private void ensureFile(String dest, String src) {
        if (!new File(fileRoot + dest).exists()) {
            log.info("Intializing " + dest);
            try {
                FileUtil.copyResource(filebase + src, fileRoot + dest);
            } catch (IOException e) {
                log.error("Failed to initialize " + dest, e);
            }
        }
    }
    
    private void ensureDirectory(String dest, String src)  {
        if (!new File(fileRoot + dest).exists()) {
            log.info("Intializing " + dest);
            try {
                FileUtil.copyDirectory(Paths.get(filebase, src), Paths.get(fileRoot, dest));
            } catch (IOException e) {
                log.error("Failed to initialize " + dest, e);
            }
        }
    }
    
    private String withTrailingSlash(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

}
