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
public class NassTypeHandler extends PointTypeHandler {

    /** _more_ */
    public static final String URL =
        "http://nass-api.azurewebsites.net/api/api_get?agg_level_desc={agg_level}&freq_desc=ANNUAL&source_desc={source}&state_name={state}&commodity_desc={commodity}&format=csv";


    /** _more_ */
    public static final int IDX_FIRST =
        org.ramadda.data.services.RecordTypeHandler.IDX_LAST + 1;


    /** _more_ */
    public static final int IDX_SOURCE = IDX_FIRST;

    /** _more_ */
    public static final int IDX_COMMODITY = IDX_FIRST + 1;


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
    public NassTypeHandler(Repository repository, Element entryNode)
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
            if (arg.equals("source") || arg.equals("commodity")
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
        String source    = entry.getValue(IDX_SOURCE, (String) null);
        String commodity = entry.getValue(IDX_COMMODITY, (String) null);
        String state     = entry.getValue(IDX_STATE, "US TOTAL");
        if ( !Utils.stringDefined(source) || !Utils.stringDefined(commodity)
                || !Utils.stringDefined(state)) {
            return null;
        }
        String aggLevel = state.equals("US TOTAL")
                          ? "NATIONAL"
                          : "STATE";

        state     = state.replace(" ", "%20");
        commodity = HtmlUtils.urlEncodeExceptSpace(commodity);
        commodity = commodity.replace(" ", "%20");
        String url = URL.replace("{source}", source).replace("{commodity}",
                                 commodity).replace("{state}",
                                     state).replace("{agg_level}", aggLevel);
        System.err.println("NASS URL:" + url);

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
        setNassEntryName(request, entry, false);
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
        setNassEntryName(request, entry, true);
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
    private void setNassEntryName(Request request, Entry entry, boolean force)
            throws Exception {
        String name = entry.getName();
        if (Utils.stringDefined(name) && !force) {
            return;
        }
        name = "USDA NASS - " + getFieldHtml(null, entry, "source") + " - "
               + getFieldHtml(null, entry, "commodity") + " - "
               + getFieldHtml(null, entry, "state");
        entry.setName(name);

        if ( !entry.isFile()) {
            entry.setValue(IDX_SOURCE_URL, getPathForEntry(request, entry));
        }

    }


}
