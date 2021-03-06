/******************************************************************
 * File:        LogReplay.java
 * Created by:  Dave Reynolds
 * Created on:  27 Mar 2013
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

package com.epimorphics.registry.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.epimorphics.appbase.templates.Lib;

/**
 * Take a logstore directory and general a shell script
 * designed to replay it.
 *
 * <pre>
 *    LogReplay dir serviceURL
 * </pre>
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class LogReplay {

    static Pattern FN_PATTERN = Pattern.compile("^on-\\d\\d-\\w+-\\d\\d\\d\\d-\\d\\d-\\d\\d-\\d\\d-(\\w+)-(.*).ttl$");
    static Pattern URL_PATTERN = Pattern.compile("^http://[^/]+/(.*)$");

    public static void main(String[] args) throws IOException {
        if (args.length != 1 && args.length != 2) {
            System.err.println("Usage: LogReplay logstore  [serviceURL]");
        }
        String logstoreDir = args[0];
        String serviceURL = (args.length == 2) ? args[1] : null;

        File logstore = new File(logstoreDir);
        String[] files = logstore.list();
        Arrays.sort(files);
        for (String filename : files) {
            Matcher matcher = FN_PATTERN.matcher(filename);
            if (matcher.matches()) {
                String action = matcher.group(1);
                String target = matcher.group(2);
                target = Lib.theLib.pathDecode(target);
                if (target.endsWith("?")) {
                    target = target.substring(0, target.length() - 1);
                }
                if (serviceURL != null) {
                    Matcher urlM = URL_PATTERN.matcher(target);
                    if (urlM.matches()) {
                        target = serviceURL + urlM.group(1);
                    } else {
                        System.err.println("Failed to parse target URL: " + target);
                        continue;
                    }
                }
                if (action.equals("Register")) {
                    System.out.println(String.format("curl -i -H 'Content-type:text/turtle' -X POST --data '@%s/%s' '%s'", logstoreDir, filename, target));
                } else {
                    System.out.println(String.format("curl -i -H 'Content-type:text/turtle' -X PUT --data '@%s/%s' '%s'", logstoreDir, filename, target));
                }
            } else {
                System.err.println("Failed to parse: " + filename);
            }
        }

    }
}
