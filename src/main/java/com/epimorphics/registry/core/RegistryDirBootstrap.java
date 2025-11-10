/******************************************************************
 * File:        RegistryDirBootstrap.java
 * Created by:  Dave Reynolds
 * Created on:  22 Apr 2013
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

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
    public static String fileRoot;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        filebase = withTrailingSlash(context.getRealPath("/"));

        fileRoot = withTrailingSlash(context.getInitParameter(ROOT_DIR_PARAM));

            ensureDirectory("config");
            ensureFile("config/user.ini", "WEB-INF/user.ini");
            ensureFile("config/root-register.ttl", "WEB-INF/root-register.ttl");
            ensureFile("config/app.conf", "WEB-INF/app.conf");
            ensureDirectory("boot", "WEB-INF/boot");
            ensureDirectory("ui");
            // Modern versions use ui/assets for better separation
            if ( ! new File(fileRoot + "ui/assets").exists() ) {
                ensureDirectory("ui/css", "ui/css");
                ensureDirectory("ui/images", "ui/images");
                ensureDirectory("ui/img", "ui/img");
                ensureDirectory("ui/js", "ui/js");
            }
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
            log.info("Intializing {}", dest);
            try {
                FileUtil.copyResource(filebase + src, fileRoot + dest);
                if (dest.endsWith(".sh")) {
                    File script = new File(fileRoot + dest);
                    script.setExecutable(true);
                    try {
                        Runtime.getRuntime().exec(new String[]{"chmod 755 " + fileRoot + dest});
                    } catch (Exception e) {
                        log.warn("Failed to set full execute permissions for {}", dest, e);
                    }
                }
            } catch (IOException e) {
                log.error("Failed to initialize {}", dest, e);
            }
        }
    }

    private void ensureDirectory(String dest, String src)  {
        if (!new File(fileRoot + dest).exists()) {
            log.info("Initializing {}", dest);
            try {
                FileUtil.copyDirectory(Paths.get(filebase, src), Paths.get(fileRoot, dest));
            } catch (IOException e) {
                log.error("Failed to initialize {}", dest, e);
            }
        }
    }

    private String withTrailingSlash(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

}
