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

package org.ramadda.repository.output;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.sql.Clause;


import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;

import java.net.*;

import java.sql.ResultSet;
import java.sql.Statement;




import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

import java.util.zip.*;


/**
 *
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class CsvOutputHandler extends OutputHandler {


    /** _more_ */
    public static final OutputType OUTPUT_CSV = new OutputType("CSV",
                                                    "default.csv",
                                                    OutputType.TYPE_FEEDS,
                                                    "", ICON_CSV);


    /** _more_ */
    public static final OutputType OUTPUT_ENTRYCSV =
        new OutputType("Entry CSV", "entry.csv", OutputType.TYPE_FEEDS, "",
                       ICON_CSV);


    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public CsvOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_CSV);
        addType(OUTPUT_ENTRYCSV);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if (state.getEntry() != null) {
            links.add(makeLink(request, state.getEntry(), OUTPUT_CSV));
            links.add(makeLink(request, state.getEntry(), OUTPUT_ENTRYCSV));
        }
    }


    /** _more_ */
    public static final String ARG_FIELDS = "fields";

    /** _more_ */
    public static final String ARG_DELIMITER = "delimiter";

    /** _more_ */
    public static final String ARG_FIXEDWIDTH = "fixedwidth";

    /** _more_ */
    public static final String ARG_FULLHEADER = "fullheader";


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listEntries(Request request, List<Entry> entries)
            throws Exception {

        String  delimiter      = request.getString(ARG_DELIMITER, ",");
        boolean fixedWidth     = request.get(ARG_FIXEDWIDTH, false);
        boolean showFullHeader = request.get(ARG_FULLHEADER, false);
        String  filler         = request.getString("filler", " ");

        String fieldsArg =
            request.getString(
                ARG_FIELDS,
                "name,id,type,entry_url,north,south,east,west,url,fields");
        StringBuffer sb          = new StringBuffer();
        StringBuffer header      = new StringBuffer();
        List<String> toks = StringUtil.split(fieldsArg, ",", true, true);
        List<String> fieldNames  = new ArrayList<String>();
        List<String> fieldLabels = new ArrayList<String>();
        for (int i = 0; i < toks.size(); i++) {
            String       tok   = toks.get(i);
            String       name  = tok;
            String       label = tok;
            List<String> pair  = StringUtil.splitUpTo(tok, ";", 2);
            if (pair.size() > 1) {
                name  = pair.get(0);
                label = pair.get(1);
            }
            fieldNames.add(name);
            fieldLabels.add(label);
            if (header.length() > 0) {
                header.append(",");
            }
            header.append(label);
        }

        boolean escapeCommas  = request.get("escapecommas", false);

        int[]   maxStringSize = null;
        //        String[] paddingmaxStringSize = null;
        for (Entry entry : entries) {
            List<Column> columns = entry.getTypeHandler().getColumns();
            if (columns == null) {
                continue;
            }
            if ((maxStringSize == null)
                    || (maxStringSize.length < columns.size())) {
                maxStringSize = new int[columns.size()];
                for (int i = 0; i < maxStringSize.length; i++) {
                    maxStringSize[i] = 0;
                }
            }
            Object[] values = entry.getTypeHandler().getEntryValues(entry);
            for (int col = 0; col < columns.size(); col++) {
                Column column = columns.get(col);
                if ( !column.getCanExport()) {
                    continue;
                }
                if (column.isString()) {
                    String s = sanitize(escapeCommas,
                                        column.getString(values));
                    maxStringSize[col] = Math.max(maxStringSize[col],
                            s.length());
                }
            }
        }

        if (maxStringSize != null) {
            //            for (int i = 0; i < maxStringSize.length; i++) {
            //                System.err.println("i:" + i + " " + maxStringSize[i]);
            //            }
        }

        Hashtable<String, Column> columnMap = null;

        for (Entry entry : entries) {
            if (sb.length() == 0) {
                String headerString = header.toString();
                if (fieldNames.contains("fields")) {
                    List<Column> columns =
                        entry.getTypeHandler().getColumns();

                    if (columns != null) {
                        String tmp = null;
                        int    cnt = 0;
                        for (int col = 0; col < columns.size(); col++) {
                            Column column = columns.get(col);
                            if ( !column.getCanExport()) {
                                continue;
                            }
                            if (tmp == null) {
                                tmp = ",";
                            } else {
                                tmp += ",";
                            }
                            tmp += column.getName();
                            if (fixedWidth) {
                                tmp += ((maxStringSize[col] > 0)
                                        ? "(max:" + maxStringSize[col] + ")"
                                        : "");
                            }

                        }
                        if (tmp == null) {
                            tmp = "";
                        }
                        headerString = headerString.replace(",fields", tmp);
                    }
                }
                if (showFullHeader) {
                    sb.append("#fields=");
                }
                sb.append(headerString);
                sb.append("\n");
            }

            Object[] values = entry.getTypeHandler().getEntryValues(entry);

            int      colCnt = 0;
            for (String field : fieldNames) {
                if (colCnt != 0) {
                    sb.append(delimiter);
                }
                colCnt++;
                if (field.equals("name")) {
                    sb.append(sanitize(escapeCommas, entry.getName()));
                } else if (field.equals("fullname")) {
                    sb.append(sanitize(escapeCommas, entry.getFullName()));
                } else if (field.equals("type")) {
                    sb.append(entry.getTypeHandler().getType());
                } else if (field.equals("icon")) {
                    sb.append(getPageHandler().getIconUrl(request, entry));
                } else if (field.equals("id")) {
                    sb.append(entry.getId());
                } else if (field.equals("entry_url")) {
                    String url = request.makeUrl(repository.URL_ENTRY_SHOW,
                                     ARG_ENTRYID, entry.getId());
                    url = HtmlUtils.urlEncodeSpace(url);
                    url = request.getAbsoluteUrl(url);
                    sb.append(url);
                } else if (field.equals("url")) {
                    if (entry.getResource().isUrl()) {
                        sb.append(
                            entry.getTypeHandler().getPathForEntry(
                                request, entry));
                    } else if (entry.getResource().isFile()) {
                        String url =
                            entry.getTypeHandler().getEntryResourceUrl(
                                request, entry);
                        url = HtmlUtils.urlEncodeSpace(url);
                        url = request.getAbsoluteUrl(url);
                        sb.append(url);
                    } else {}
                } else if (field.equals("latitude")) {
                    sb.append(entry.getLatitude());
                } else if (field.equals("longitude")) {
                    sb.append(entry.getLongitude());
                } else if (field.equals("north")) {
                    sb.append(entry.getNorth());
                } else if (field.equals("south")) {
                    sb.append(entry.getSouth());
                } else if (field.equals("east")) {
                    sb.append(entry.getEast());
                } else if (field.equals("west")) {
                    sb.append(entry.getWest());
                } else if (field.equals("description")) {
                    sb.append(sanitize(escapeCommas, entry.getDescription()));
                } else if (field.equals("size")) {
                    sb.append(entry.getResource().getFileSize());
                } else if (field.equals("fields")) {
                    List<Column> columns =
                        entry.getTypeHandler().getColumns();
                    if (columns != null) {
                        int cnt = 0;
                        for (int col = 0; col < columns.size(); col++) {
                            Column column = columns.get(col);
                            if ( !column.getCanExport()) {
                                continue;
                            }
                            if (cnt > 0) {
                                sb.append(delimiter);
                            }
                            String s = sanitize(escapeCommas,
                                           column.getString(values));
                            sb.append(s);
                            if (fixedWidth) {
                                if (column.isString()) {
                                    int length = s.length();
                                    while (length < maxStringSize[col]) {
                                        sb.append(filler);
                                        length++;
                                    }
                                }
                            }
                            cnt++;
                        }
                    }
                } else {
                    if (columnMap == null) {
                        columnMap = new Hashtable<String, Column>();
                        List<Column> columns =
                            entry.getTypeHandler().getColumns();
                        if (columns != null) {
                            for (int col = 0; col < columns.size(); col++) {
                                Column column = columns.get(col);
                                if ( !column.getCanExport()) {
                                    continue;
                                }
                                columnMap.put(column.getName(), column);
                            }
                        }
                    }
                    Column column = columnMap.get(field);
                    if (column != null) {
                        String s = sanitize(escapeCommas,
                                            column.getString(values));
                        sb.append(s);
                        if (fixedWidth) {
                            if (column.isString()) {
                                int length = s.length();
                                while (length
                                        < maxStringSize[column.getColumnIndex()]) {
                                    sb.append(filler);
                                    length++;
                                }
                            }
                        }
                    } else {
                        sb.append("unknown:" + field);
                    }
                }
            }
            sb.append("\n");
        }

        return new Result("", sb, getMimeType(OUTPUT_CSV));
    }

    /**
     * _more_
     *
     *
     * @param escapeCommas _more_
     * @param s _more_
     *
     * @return _more_
     */
    public String sanitize(boolean escapeCommas, String s) {
        if (s == null) {
            return "";
        }
        s = s.replaceAll("\r\n", " ");
        s = s.replaceAll("\r", " ");
        s = s.replaceAll("\n", " ");
        //quote the columns that have commas in them
        if (s.indexOf(",") >= 0) {
            if (escapeCommas) {
                s = s.replaceAll(",", "_comma_");
            } else {
                //Not sure how to escape the quotes
                s = s.replaceAll("\"", "'");
                //wrap in a quote
                s = "\"" + s + "\"";
            }
        }

        return s;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param typeHandlers _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result listTypes(Request request, List<TypeHandler> typeHandlers)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        for (TypeHandler theTypeHandler : typeHandlers) {
            sb.append(SqlUtil.comma(theTypeHandler.getType(),
                                    theTypeHandler.getDescription()));
            sb.append("\n");
        }

        return new Result("", sb, getMimeType(OUTPUT_CSV));
    }





    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(OutputType output) {
        if (output.equals(OUTPUT_CSV)) {
            return repository.getMimeTypeFromSuffix(".csv");
        }

        return super.getMimeType(output);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> subGroups,
                              List<Entry> entries)
            throws Exception {
        if (group.isDummy()) {
            request.setReturnFilename("Search_Results.csv");
        } else {
            request.setReturnFilename(group.getName() + ".csv");
        }
        if (OUTPUT_ENTRYCSV.equals(outputType)) {
            List<Entry> tmp = new ArrayList<Entry>();
            tmp.add(group);

            return listEntries(request, tmp);
        }
        subGroups.addAll(entries);

        return listEntries(request, subGroups);
    }



}
