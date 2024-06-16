/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.census;


import org.json.*;

import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;

import org.ramadda.util.JsonUtil;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.Place;

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

    /**  */
    public static final int IDX_PATTERN = IDX++;

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
        for (CensusVariable variable : getVariables(request,entry)) {
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
        for (CensusVariable variable : getVariables(request,entry)) {
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
     * @param properties _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        String               header  = entry.getStringValue(request,IDX_HEADER, "");
        boolean includeSpecial = entry.getBooleanValue(request,IDX_INCLUDE_LOCALES, false);
        List<CensusVariable> vars    = getVariables(request,entry);
        String               pattern = (String) entry.getValue(request,IDX_PATTERN);
        if ((pattern == null) || (pattern.trim().length() == 0)) {
            List<Metadata> metadataList =
                getMetadataManager().findMetadata(request, entry,
                    "census_name_pattern", true);
            if ((metadataList != null) && (metadataList.size() > 0)) {
                pattern = metadataList.get(0).getAttr1();
            }
        }
        AcsFile file = new AcsFile(new IO.Path(getPathForEntry(request, entry,true)),
                                   StringUtil.split(header, "\n", true,
						    true), includeSpecial, pattern);

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
    private List<String> getIndicatorIds(Request request,Entry entry) throws Exception {
        List<String> ids = new ArrayList<String>();
        for (CensusVariable variable : getVariables(request,entry)) {
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
    private List<CensusVariable> getVariables(Request request,Entry entry) throws Exception {
        List<CensusVariable> vars = new ArrayList<CensusVariable>();
        if (entry == null) {
            return vars;
        }
	Hashtable<String,Integer> indices = new Hashtable<String,Integer>();
	int cnt=0;
        for (String id : getFieldLines(request,entry)) {
            int     index = CensusVariable.NULL_INDEX;
	    String alias=null;
            boolean skip  = false;
            if (id.indexOf(":") >= 0) {
                List<String> toks = StringUtil.split(id, ":");
                id = toks.get(0);
                for (int i = 1; i < toks.size(); i++) {
                    String tok = toks.get(i).trim();
                    if (tok.startsWith("%")) {
			tok = tok.substring(1).trim();
			if(tok.matches("^-?\\d+$")) {
			    index = Integer.decode(tok).intValue();
			} else {
			    Integer lookup = indices.get(tok);
			    if(lookup!=null) {
				index = lookup.intValue();
				//				System.err.println("look up:" + tok +" index=" + i);
			    } else {
				//				System.err.println("can't find look up:" + tok);
			    }
			}
                    } else if (tok.equals("skip")) {
                        skip = true;
                    } else {
			alias=tok;
		    }
                }
            }
	    //	    System.err.println("ID:" + id +" index:" + cnt);
	    indices.put(id,cnt++);
            CensusVariable var = CensusVariable.getVariable(id);
            if (var != null) {
                var = var.cloneMe();
		if(alias!=null) var.setAlias(alias);
                var.setSkip(skip);
                if (index != CensusVariable.NULL_INDEX) {
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
    private List<String> getFieldLines(Request request,Entry entry) throws Exception {
        if (entry == null) {
            return new ArrayList<String>();
        }
        String       s      = entry.getStringValue(request,IDX_FIELDS, "").trim();
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
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {
        if (entry.isFile()) {
            return super.getPathForEntry(request, entry, forRead);
        }
        String getArgValue = StringUtil.join(",", getIndicatorIds(request,entry));

        String forType     = entry.getStringValue(request,IDX_FOR_TYPE, "us");
        String forValue    = entry.getStringValue(request,IDX_FOR_VALUE, "");
        String forArgValue = forType + ":" + (Utils.stringDefined(forValue)
                ? forValue
                : "*");

        String inType1     = entry.getStringValue(request,IDX_IN_TYPE1, "");
        String inValue1    = entry.getStringValue(request,IDX_IN_VALUE1, "");
        String inType2     = entry.getStringValue(request,IDX_IN_TYPE2, "");
        String inValue2    = entry.getStringValue(request,IDX_IN_VALUE2, "");
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
     * @param fromImport _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
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
            entry.setValue(IDX_SOURCE_URL, getPathForEntry(request, entry,false));
        }


        String inValue  = entry.getStringValue(request,IDX_IN_VALUE1, "");
        String inType   = entry.getStringValue(request,IDX_IN_TYPE1, "");
        String forValue = entry.getStringValue(request,IDX_FOR_VALUE, "");
        String forType  = entry.getStringValue(request,IDX_FOR_TYPE, "");

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
