/******************************************************************
 * File:        Parameters.java
 * Created by:  Dave Reynolds
 * Created on:  21 Jan 2013
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

package com.epimorphics.registry.webapi;

/**
 * Manifest constants defining the paramters available in the web API.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Parameters {
    public static final String VALIDATE = "validate";
    public static final String STATUS_UPDATE = "update";
    public static final String FORCE = "force";
    public static final String QUERY = "query";
    public static final String TAG = "tag";
    public static final String GRAPH = "graph";
    public static final String ANNOTATION = "annotation";

    public static final String VIEW = "_view";
    public static final String WITH_METADATA = "with_metadata";
//    public static final String WITH_VERSION = "version";
    public static final String VERSION_LIST = "version_list";
    public static final String ENTITY_LOOKUP = "entity";
    public static final String VERSION_AT = "_versionAt";

    public static final String COLLECTION_METADATA_ONLY = "non-member-properties";
    public static final String FIRST_PAGE = "firstPage";
    public static final String PAGE_NUMBER = "_page";

    public static final String STATUS = "status";

    public static final String BATCH_REFERENCED = "batch-referenced";
    public static final String BATCH_MANAGED = "batch-managed";

    public static final String FORMAT = "_format";

}
