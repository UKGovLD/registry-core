/******************************************************************
 * File:        TomcatTestContainerFactory.java
 * Created by:  Dave Reynolds
 * Created on:  30 Nov 2012
 *
 * (c) Copyright 2012, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.webapi;

import java.io.File;

import org.apache.catalina.startup.Tomcat;
import org.junit.After;
import org.junit.Before;

public class TomcatTestBase {

    static final String BASE_URL = "http://localhost:8070/";

    Tomcat tomcat ;

    @Before
    public void containerStart() throws Exception {
        String root = "src/main/webapp";
        tomcat = new Tomcat();
        tomcat.setPort(8070);

        tomcat.setBaseDir(".");

        String contextPath = "/";

        File rootF = new File(root);
        if (!rootF.exists()) {
            rootF = new File(".");
        }
        if (!rootF.exists()) {
            System.err.println("Can't find root app: " + root);
            System.exit(1);
        }

        tomcat.addWebapp(contextPath,  rootF.getAbsolutePath());
        tomcat.start();
    }

    @After
    public void containerStop() throws Exception {
        tomcat.stop();
        tomcat.destroy();
    }

}
