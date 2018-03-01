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

package org.ramadda.plugins.usda;


import org.json.*;

import org.ramadda.data.services.PointTypeHandler;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;

import org.w3c.dom.*;
import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class ArmsTypeHandler extends PointTypeHandler {

    /** _more_ */
    public static final String URL =
        "http://arms-api.azurewebsites.net/api/{type}?report={report}&subject={subject}&series1=FARM&fipsStateCode={state}&series2=FARM&format=csv";

    /** _more_ */
    public static final int IDX_FIRST =
        org.ramadda.data.services.RecordTypeHandler.IDX_LAST + 1;


    /** _more_ */
    public static final int IDX_REPORT = IDX_FIRST + 0;

    /** _more_ */
    public static final int IDX_SUBJECT = IDX_FIRST + 1;


    /** _more_ */
    public static final int IDX_STATE = IDX_FIRST + 2;

    /** _more_ */
    public static final int IDX_SOURCE_URL = IDX_FIRST + 3;



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public ArmsTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param arg _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean okToShowInForm(Entry entry, String arg, boolean dflt) {
        if (entry != null) {
            if (arg.equals("report") || arg.equals("subject")
                    || arg.equals("state") || arg.equals("source_url")) {
                if (entry.getResource().isFile()
                        || entry.getResource().isUrl()) {
                    return false;
                }
            }
        }

        return super.okToShowInForm(entry, arg, dflt);
    }

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getPathForEntry(Request request, Entry entry)
            throws Exception {
        if (entry.isFile()) {
            return super.getPathForEntry(request, entry);
        }
        String type = "FINANCE";

        if (getType().equals("type_usda_arms_crop")) {
            type = "Crop";
        }
        String report  = entry.getValue(IDX_REPORT, (String) null);
        String subject = entry.getValue(IDX_SUBJECT, (String) null);
        String state   = entry.getValue(IDX_STATE, "00");
        if ( !Utils.stringDefined(report) || !Utils.stringDefined(subject)
                || !Utils.stringDefined(state)) {
            return null;
        }
        String url = URL.replace("{type}", type).replace("{report}",
                                 report).replace("{subject}",
                                     subject).replace("{state}", state);
        System.err.println("ARMS URL:" + url);

        return url;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry)
            throws Exception {
        super.initializeNewEntry(request, entry);
        setArmsEntryName(request, entry, false);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param parent _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
        super.initializeEntryFromForm(request, entry, parent, newEntry);
        setArmsEntryName(request, entry, true);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param force _more_
     *
     * @throws Exception _more_
     */
    private void setArmsEntryName(Request request, Entry entry, boolean force)
            throws Exception {
        String name = entry.getName();
        if (Utils.stringDefined(name) && !force) {
            return;
        }
        name = "USDA ARMS - " + getFieldHtml(null, entry, "report") + " - "
               + getFieldHtml(null, entry, "subject") + " - "
               + getFieldHtml(null, entry, "state");
        entry.setName(name);

        if ( !entry.isFile()) {
            entry.setValue(IDX_SOURCE_URL, getPathForEntry(request, entry));
        }

    }


}
