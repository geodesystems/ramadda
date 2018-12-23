/*
* Copyright (c) 2008-2019 Geode Systems LLC
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

package org.ramadda.plugins.census;


import org.json.*;

import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Place;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;

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
public class AcsTypeHandler extends PointTypeHandler {

    /** _more_ */
    public static final String URL =
        "https://api.census.gov/data/{year}/acs/acs5";


    /** _more_ */
    private static int IDX =
        org.ramadda.data.services.RecordTypeHandler.IDX_LAST + 1;


    /** _more_ */
    public static final int IDX_FIELDS = IDX++;

    /** _more_ */
    public static final int IDX_HEADER = IDX++;

    /** _more_ */
    public static final int IDX_FOR_TYPE = IDX++;

    /** _more_ */
    public static final int IDX_FOR_VALUE = IDX++;

    /** _more_ */
    public static final int IDX_IN_TYPE1 = IDX++;

    /** _more_ */
    public static final int IDX_IN_VALUE1 = IDX++;

    /** _more_ */
    public static final int IDX_IN_TYPE2 = IDX++;

    /** _more_ */
    public static final int IDX_IN_VALUE2 = IDX++;

    /** _more_ */
    public static final int IDX_SOURCE_URL = IDX++;

    /** _more_ */
    public static final int IDX_INCLUDE_LOCALES = IDX++;



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public AcsTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param column _more_
     * @param tmpSb _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    public void formatColumnHtmlValue(Request request, Entry entry,
                                      Column column, Appendable tmpSb,
                                      Object[] values)
            throws Exception {
        if ( !column.getName().equals("fields")) {
            super.formatColumnHtmlValue(request, entry, column, tmpSb,
                                        values);

            return;
        }


        tmpSb.append(HtmlUtils.formTable());
        for (CensusVariable variable : getVariables(entry)) {
            tmpSb.append(
                HtmlUtils.row(
                    HtmlUtils.cols(
                        HtmlUtils.b(variable.getId()), "&nbsp;&nbsp;",
                        variable.getLabel())));
            tmpSb.append(HtmlUtils.row(HtmlUtils.cols("", "",
                    variable.getConcept())));
        }
        tmpSb.append(HtmlUtils.formTableClose());

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param column _more_
     * @param widget _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getFormWidget(Request request, Entry entry, Column column,
                                String widget)
            throws Exception {
        if ( !column.getName().equals("fields")) {
            return super.getFormWidget(request, entry, column, widget);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(HtmlUtils.input("acs_get_text", "", 60,
                                  HtmlUtils.id("acs_get_text")
                                  + HtmlUtils.attr("placeholder", "Search")));
        sb.append(
            HtmlUtils.importJS(
                getRepository().getFileUrl("/census/census.js")));
        sb.append(
            HtmlUtils.cssLink(
                getRepository().getFileUrl("/census/census.css")));

        sb.append(HtmlUtils.href(getRepository().getUrlBase()
                                 + "/census/index.html", "Help",
                                     " target=\"_variables\""));
        sb.append(HtmlUtils.br());
        sb.append(HtmlUtils.div("",
                                HtmlUtils.id("acs_list")
                                + HtmlUtils.cssClass("ramadda-acs-list")));

        sb.append(HtmlUtils.formTable());
        for (CensusVariable variable : getVariables(entry)) {
            sb.append(
                HtmlUtils.rowTop(
                    HtmlUtils.cols(
                        HtmlUtils.b(variable.getId()), "&nbsp;&nbsp;",
                        variable.getLabel(), variable.getConcept())));
        }
        sb.append(HtmlUtils.formTableClose());

        return HtmlUtils.hbox(widget, sb.toString());
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry)
            throws Exception {
        String               header = entry.getValue(IDX_HEADER, "");
        boolean includeSpecial = entry.getValue(IDX_INCLUDE_LOCALES, false);
        List<CensusVariable> vars   = getVariables(entry);
        AcsFile file = new AcsFile(getPathForEntry(request, entry),
                                   StringUtil.split(header, "\n", true,
                                       true), includeSpecial);

        file.putProperty("output.latlon", "false");
        file.setVariables(vars);

        return file;
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<String> getIndicatorIds(Entry entry) throws Exception {
        List<String> ids = new ArrayList<String>();
        for (CensusVariable variable : getVariables(entry)) {
            ids.add(variable.getId());
        }

        return ids;

    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<CensusVariable> getVariables(Entry entry) throws Exception {
        List<CensusVariable> vars = new ArrayList<CensusVariable>();
        if (entry == null) {
            return vars;
        }
        for (String id : getFieldLines(entry)) {
            int     index = -1;
            boolean skip  = false;
            if (id.indexOf(":") >= 0) {
                List<String> toks = StringUtil.split(id, ":");
                id = toks.get(0);
                for (int i = 1; i < toks.size(); i++) {
                    String tok = toks.get(i).trim();
                    if (tok.startsWith("%")) {
                        index = Integer.decode(tok.substring(1)).intValue();
                    } else if (tok.equals("skip")) {
                        skip = true;
                    }
                }


            }
            CensusVariable var = CensusVariable.getVariable(id);
            if (var != null) {
                var = var.cloneMe();
                var.setSkip(skip);
                if (index >= 0) {
                    var.setDependsIndex(index);

                }
                vars.add(var);
            }
        }

        return vars;
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<String> getFieldLines(Entry entry) throws Exception {
        if (entry == null) {
            return new ArrayList<String>();
        }
        String       s      = entry.getValue(IDX_FIELDS, "").trim();
        List<String> fields = new ArrayList<String>();
        for (String line : StringUtil.split(s, "\n", true, true)) {
            if (line.startsWith("#")) {
                continue;
            }
            fields.add(line);
        }

        return fields;
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
        /*
        if (entry != null) {
            if (arg.equals("report") || arg.equals("subject")
                    || arg.equals("state") || arg.equals("source_url")) {
                if (entry.getResource().isFile()
                        || entry.getResource().isUrl()) {
                    return false;
                }
            }
        }
        */
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
        String getArgValue = StringUtil.join(",", getIndicatorIds(entry));

        String forType     = entry.getValue(IDX_FOR_TYPE, "us");
        String forValue    = entry.getValue(IDX_FOR_VALUE, "");
        String forArgValue = forType + ":" + (Utils.stringDefined(forValue)
                ? forValue
                : "*");

        String inType1     = entry.getValue(IDX_IN_TYPE1, "");
        String inValue1    = entry.getValue(IDX_IN_VALUE1, "");
        String inType2     = entry.getValue(IDX_IN_TYPE2, "");
        String inValue2    = entry.getValue(IDX_IN_VALUE2, "");
        String key = getRepository().getProperty("census.api.key",
                         (String) null);
        //        "http://api.census.gov/data/2013/acs5?get=NAME,B01001_001E&for=county+subdivision:*&in=state:04";
        String url = HtmlUtils.url(URL, new String[] { "get",
                getArgValue.replaceAll(" ", "+"), "for",
                forArgValue.replaceAll(" ", "+") }, false);

        if (key != null) {
            url += "&key=" + key;
        }
        url = url.replace("{year}", "2015");
        //        System.err.println(url);
        if (Utils.stringDefined(inType1)) {
            String inArgValue = inType1 + ":" + (Utils.stringDefined(inValue1)
                    ? inValue1
                    : "*");
            if (Utils.stringDefined(inType2)) {
                inArgValue += "+" + inType2 + ":"
                              + (Utils.stringDefined(inValue2)
                                 ? inValue2
                                 : "*");
            }
            url += "&"
                   + HtmlUtils.arg("in", inArgValue.replaceAll(" ", "+"),
                                   false);
        }

        //        System.err.println("ACS URL:" + url);

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
        setAcsEntryName(request, entry, false);
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
        setAcsEntryName(request, entry, true);
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
    private void setAcsEntryName(Request request, Entry entry, boolean force)
            throws Exception {
        if ( !entry.isFile()) {
            entry.setValue(IDX_SOURCE_URL, getPathForEntry(request, entry));
        }


        String inValue  = entry.getValue(IDX_IN_VALUE1, "");
        String inType   = entry.getValue(IDX_IN_TYPE1, "");
        String forValue = entry.getValue(IDX_FOR_VALUE, "");
        String forType  = entry.getValue(IDX_FOR_TYPE, "");


        if (Utils.stringDefined(inValue)) {
            Place place = Place.getPlace(inValue);
            if (place != null) {
                entry.setLatitude(place.getLatitude());
                entry.setLongitude(place.getLongitude());
            }
        } else if (Utils.stringDefined(forValue)) {
            Place place = Place.getPlace(forValue);
            if (place != null) {
                entry.setLatitude(place.getLatitude());
                entry.setLongitude(place.getLongitude());
            }
        }
    }


}
