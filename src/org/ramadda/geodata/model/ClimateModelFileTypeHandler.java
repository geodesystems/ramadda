/*
* Copyright (c) 2008-2018 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.geodata.model;


import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.type.GenericTypeHandler;
import org.ramadda.repository.type.GranuleTypeHandler;
import org.ramadda.repository.type.TypeHandler;

import org.w3c.dom.Element;

import ucar.unidata.util.IOUtil;

import java.util.Date;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


/**
 * A class for handling climate model files
 */
public class ClimateModelFileTypeHandler extends GranuleTypeHandler {

    /** the default file regex */
    public static final String FILE_REGEX =
        "([^_]+)_([^_]+)_(.*)_(ens..|mean|sprd|clim)(_([^_]+))?.nc";

    /** local regex */
    private String myRegex = null;

    /** the regex property */
    public static final String PROP_FILE_PATTERN = "model.file.pattern";

    /** pattern for file names */
    //public static final Pattern pattern = Pattern.compile(FILE_REGEX);
    protected Pattern pattern = null;

    /** ClimateModelFile type */
    public static final String TYPE_CLIMATE_MODELFILE = "climate_modelfile";

    /**
     * Create a ClimateModelFileTypeHandler
     *
     * @param repository  the repository
     * @param entryNode   the defining xml
     *
     * @throws Exception  problems
     */
    public ClimateModelFileTypeHandler(Repository repository,
                                       Element entryNode)
            throws Exception {
        this(repository, entryNode, null);
    }

    /**
     * Create a ClimateModelFileTypeHandler with the given pattern
     *
     * @param repository  the repository
     * @param entryNode   the node
     * @param regex_pattern   the pattern for file names
     *
     * @throws Exception  the pattern
     */
    public ClimateModelFileTypeHandler(Repository repository,
                                       Element entryNode,
                                       String regex_pattern)
            throws Exception {
        super(repository, entryNode);
        if (regex_pattern == null) {
            myRegex = getTypeProperty(PROP_FILE_PATTERN, null);
        } else {
            myRegex = regex_pattern;
        }
        if (myRegex != null) {
            pattern = Pattern.compile(myRegex);
        }
    }

    /**
     * Initialize the entry
     *
     * @param request   the request
     * @param entry the Entry to initialize
     *
     * @throws Exception  problems during initialization
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry)
            throws Exception {
        super.initializeNewEntry(request, entry);
        Object[] values = getEntryValues(entry);
        if ((values[1] != null) && !values[1].toString().isEmpty()) {
            //System.err.println("already have  values set");
            return;
        }
        if (pattern == null) {
            return;
        }
        //System.err.println("no values set");
        String filepath = entry.getFile().toString();
        // strip off entry.das in case it's an opendap link from RAMADDA
        filepath = filepath.replaceAll("/entry.das", "");
        String filename = IOUtil.getFileTail(filepath);
        // Filename looks like  var_model_scenario_ens??_<date>.nc
        Matcher m = pattern.matcher(filename);
        if ( !m.find()) {
            System.err.println("no match");

            return;
        }
        String var        = m.group(1);
        String model      = m.group(2);
        String experiment = m.group(3);
        String member     = m.group(4);
        String date       = m.group(6);
        String frequency  = "Monthly";
        if (filepath.indexOf("Daily") >= 0) {
            frequency = "Daily";
        }

        /*
     <column name="collection_id" type="string"  label="Collection ID" showinhtml="false" showinform="false"/>
     <column name="model" type="enumerationplus"  label="Model"  showinhtml="true" xxxxvalues="file:/org/ramadda/data/model/models.txt"/>
     <column name="experiment" type="enumerationplus"  label="Experiment" xxxxvalues="file:/org/ramadda/data/model/experiments.txt" showinhtml="true" />
     <column name="ensemble" type="string"  label="Ensemble"/>
     <column name="variable" type="enumerationplus"  label="Variable"  xxxxxvalues="file:/org/ramadda/data/model/vars.txt"/>
        */

        int idx = 1;
        values[idx++] = model;
        values[idx++] = experiment;
        values[idx++] = member;
        values[idx++] = var;

    }



    /**
     * This gets called from the wiki chart displays
     * we route the request to the cdm_grid type handler
     *
     * @param request The request
     * @param entry The entry
     * @param tag The wiki tag being used
     * @param props _more_
     *
     * @return The point time series url
     */
    @Override
    public String getUrlForWiki(Request request, Entry entry, String tag,
                                Hashtable props) {
        try {
            TypeHandler gridType = getRepository().getTypeHandler("cdm_grid");
            if (gridType != null) {
                return gridType.getUrlForWiki(request, entry, tag, props);
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }

        return super.getUrlForWiki(request, entry, tag, props);

    }



    /**
     * Test it
     *
     * @param args  the arguments
     * public static void main(String[] args) {
     *   for (String arg : args) {
     *       Matcher m = pattern.matcher(arg);
     *       if ( !m.find()) {
     *           System.err.println("no match x");
     *       } else {
     *           System.err.println("match");
     *           String var = m.group(1);
     *           System.err.println("var:" + var);
     *       }
     *   }
     * }
     *
     * @param request _more_
     * @param entry _more_
     * @param tag _more_
     *
     * @return _more_
     */



}
