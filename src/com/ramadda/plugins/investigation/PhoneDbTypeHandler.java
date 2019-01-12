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

/**
 * Copyright (c) 2008-2015 Geode Systems LLC
 * This Software is licensed under the Geode Systems RAMADDA License available in the source distribution in the file
 * ramadda_license.txt. The above copyright notice shall be included in all copies or substantial portions of the Software.
 */

package com.ramadda.plugins.investigation;


import org.apache.commons.lang.text.StrTokenizer;


import org.ramadda.plugins.db.*;


import org.ramadda.repository.*;

import org.ramadda.repository.output.*;


import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;


import org.ramadda.util.Utils;

import org.ramadda.util.sql.Clause;


import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;



import java.io.File;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;



/**
 *
 */

public class PhoneDbTypeHandler extends DbTypeHandler {

    /** _more_ */
    public static final String VIEW_CALL_LISTING = "call.listing";

    /** _more_ */
    public static final String VIEW_CALL_GRAPH = "call.graph";

    /** _more_ */
    public static final String VIEW_CALL_GRAPH_DATA = "call.graphdata";

    /** _more_ */
    private static final String ARROW = " &rarr; ";


    /** _more_ */
    public static final String ARG_DB_NAMES = "db.names";

    /** _more_ */
    public static final String ARG_IDS = "ids";

    /** _more_ */
    public static final String ARG_NUMBER = "number";

    /** _more_ */
    public static final String ARG_ANONYMIZE = "anonymize";

    /** _more_ */
    Column fromNumberColumn;

    /** _more_ */
    Column toNumberColumn;

    /** _more_ */
    Column fromNameColumn;

    /** _more_ */
    Column toNameColumn;

    /** _more_ */
    Column dateColumn;


    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param tableName _more_
     * @param tableNode _more_
     * @param desc _more_
     *
     * @throws Exception _more_
     */
    public PhoneDbTypeHandler(Repository repository, String tableName,
                              Element tableNode, String desc)
            throws Exception {
        super(repository, tableName, tableNode, desc);
    }


    /**
     * _more_
     *
     * @param columnNodes _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initDbColumns(List<Element> columnNodes) throws Exception {
        super.initDbColumns(columnNodes);
        viewList.add(1, new TwoFacedObject("Call Listing",
                                           VIEW_CALL_LISTING));
        viewList.add(1, new TwoFacedObject("Call Graph", VIEW_CALL_GRAPH));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param view _more_
     * @param headerToks _more_
     * @param baseUrl _more_
     * @param addNext _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addHeaderItems(Request request, Entry entry, String view,
                               List<String> headerToks, String baseUrl,
                               boolean[] addNext)
            throws Exception {

        super.addHeaderItems(request, entry, view, headerToks, baseUrl,
                             addNext);
        if (view.equals(VIEW_CALL_LISTING)) {
            addNext[0] = true;
            headerToks.add(HtmlUtils.b(msg("Call Listing")));
        } else {
            headerToks.add(
                HtmlUtils.href(
                    baseUrl + "&" + ARG_DB_VIEW + "=" + VIEW_CALL_LISTING,
                    msg("Call Listing")));
        }

        if (view.equals(VIEW_CALL_GRAPH)) {
            addNext[0] = true;
            headerToks.add(HtmlUtils.b(msg("Call Graph")));
        } else {
            headerToks.add(
                HtmlUtils.href(
                    baseUrl + "&" + ARG_DB_VIEW + "=" + VIEW_CALL_GRAPH,
                    msg("Call Graph")));
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param column _more_
     * @param values _more_
     * @param sdf _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    @Override
    public String formatTableValue(Request request, Entry entry,
                                   Appendable sb, Column column,
                                   Object[] values, SimpleDateFormat sdf)
            throws Exception {
        if (column.equals(fromNumberColumn)
                || column.equals(toNumberColumn)) {
            String s = formatNumber(column.getString(values));
            Utils.append(sb, s);

            return s;
        } else {
            return super.formatTableValue(request, entry, sb, column, values,
                                          sdf);
        }
    }




    /**
     * _more_
     *
     * @param n _more_
     *
     * @return _more_
     */
    public String formatNumber(String n) {
        n = n.trim();
        if (n.length() != 10) {
            return n;
        }

        return n.substring(0, 3) + "-" + n.substring(3, 6) + "-"
               + n.substring(6);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param formBuffer _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addToEditForm(Request request, Entry entry,
                              Appendable formBuffer)
            throws Exception {
        super.addToEditForm(request, entry, formBuffer);
        Utils.append(
            formBuffer,
            HtmlUtils.row(HtmlUtils.colspan("Enter numbers and names", 2)));
        Utils.append(
            formBuffer,
            HtmlUtils.formEntry(
                msgLabel("Numbers and names"),
                HtmlUtils.textArea(
                    ARG_DB_NAMES,
                    "#number=name\n#e.g.:\n#303-555-1212= some name\n", 8,
                    50)));



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
        if (newEntry) {
            return;
        }
        List<String[]> numberToName = new ArrayList<String[]>();
        for (String line :
                StringUtil.split(request.getString(ARG_DB_NAMES, ""), "\n",
                                 true, true)) {
            line = line.trim();
            if (line.startsWith("#")) {
                continue;
            }
            List<String> numberAndName = StringUtil.splitUpTo(line, "=", 2);
            if (numberAndName.size() < 2) {
                continue;
            }
            String number = numberAndName.get(0).trim();
            String name   = numberAndName.get(1).trim();
            number = cleanUpNumber(number);
            if ((number.length() > 0) && (name.length() > 0)) {
                numberToName.add(new String[] { number, name });
            }
        }
        if (numberToName.size() > 0) {
            for (String[] pair : numberToName) {
                String       number    = pair[0];
                String       name      = pair[1];
                List<Clause> fromWhere = new ArrayList<Clause>();
                fromWhere.add(Clause.eq(COL_ID, entry.getId()));
                fromWhere.add(Clause.eq(fromNumberColumn.getName(), number));
                Clause fromClause = Clause.and(fromWhere);
                getDatabaseManager().update(
                    tableHandler.getTableName(), fromClause,
                    new String[] { getFromNameColumn() },
                    new Object[] { name });


                List<Clause> toWhere = new ArrayList<Clause>();
                toWhere.add(Clause.eq(COL_ID, entry.getId()));
                toWhere.add(Clause.eq(toNumberColumn.getName(), number));
                Clause toClause = Clause.and(toWhere);
                getDatabaseManager().update(
                    tableHandler.getTableName(), toClause,
                    new String[] { getToNameColumn() },
                    new Object[] { name });
            }
        }
    }

    /**
     * _more_
     *
     * @param number _more_
     *
     * @return _more_
     */
    public String cleanUpNumber(String number) {
        return number;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getFromNameColumn() {
        return "from_name";
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getToNameColumn() {
        return "to_name";
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
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        String view = request.getString(ARG_DB_VIEW, VIEW_TABLE);

        if (view.equals(VIEW_CALL_GRAPH_DATA)) {
            return handleCallGraphData(request, entry);
        }

        return super.getHtmlDisplay(request, entry);
    }

    /** _more_ */
    private int cnt = 0;

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param valueList _more_
     * @param fromSearch _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleCallGraphPage(Request request, Entry entry,
                                      List<Object[]> valueList,
                                      boolean fromSearch)
            throws Exception {
        StringBuilder sb = new StringBuilder();
        addViewHeader(request, entry, sb, VIEW_CALL_GRAPH, valueList.size(),
                      fromSearch);

        sb.append("\n\n");
        StringBuilder      js     = new StringBuilder();
        GraphOutputHandler goh    = getWikiManager().getGraphOutputHandler();
        int                width  = 800;
        int                height = 500;
        String             id     = goh.addPrefixHtml(sb, js, width, height);
        List<String>       nodes  = new ArrayList<String>();
        List<String>       links  = new ArrayList<String>();
        getNodesAndLinks(request, entry, valueList, nodes, links);
        goh.addSuffixHtml(sb, js, id, nodes, links, width, height);
        Result result = new Result(msg("Graph"), sb);

        return result;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param valueList _more_
     * @param nodes _more_
     * @param links _more_
     *
     * @throws Exception _more_
     */
    private void getNodesAndLinks(Request request, Entry entry,
                                  List<Object[]> valueList,
                                  List<String> nodes, List<String> links)
            throws Exception {
        GraphOutputHandler goh = getWikiManager().getGraphOutputHandler();
        String                     iconUrl = getEntryIcon(request, entry);
        HashSet<String>            seen    = new HashSet<String>();
        Hashtable<String, Integer> count   = new Hashtable<String, Integer>();
        for (int i = 0; i < valueList.size(); i++) {
            Object[] values     = valueList.get(i);
            String   fromNumber = fromNumberColumn.getString(values);
            String   toNumber   = toNumberColumn.getString(values);

            String entryUrl =
                request.entryUrl(getRepository().URL_ENTRY_SHOW, entry);
            StringBuilder row = new StringBuilder();
            if ( !seen.contains(fromNumber)) {
                String searchUrl =
                    HtmlUtils.url(
                        request.makeUrl(getRepository().URL_ENTRY_SHOW),
                        new String[] {
                    ARG_ENTRYID, entry.getId(), ARG_DB_SEARCH, "true",
                    getSearchUrlArgument(fromNumberColumn), fromNumber
                });
                nodes.add(Json.map(new String[] {
                    goh.ATTR_NAME, formatNumber(fromNumber), goh.ATTR_LABEL,
                    fromNameColumn.getString(values), goh.ATTR_NODEID,
                    fromNumber, goh.ATTR_URL, searchUrl, goh.ATTR_GRAPHURL,
                    getDataUrl(request, entry, fromNumber), goh.ATTR_ICON,
                    iconUrl
                }, true));
                seen.add(fromNumber);
            }
            if ( !seen.contains(toNumber)) {
                String searchUrl =
                    HtmlUtils.url(
                        request.makeUrl(getRepository().URL_ENTRY_SHOW),
                        new String[] {
                    ARG_ENTRYID, entry.getId(), ARG_DB_SEARCH, "true",
                    getSearchUrlArgument(fromNumberColumn), fromNumber
                });
                nodes.add(Json.map(new String[] {
                    goh.ATTR_NAME, formatNumber(toNumber), goh.ATTR_LABEL,
                    toNameColumn.getString(values), goh.ATTR_NODEID, toNumber,
                    goh.ATTR_URL, searchUrl, goh.ATTR_GRAPHURL,
                    getDataUrl(request, entry, toNumber), goh.ATTR_ICON,
                    iconUrl
                }, true));
                seen.add(toNumber);
            }
            links.add(Json.map(new String[] {
                goh.ATTR_SOURCE_ID, fromNumber, goh.ATTR_TARGET_ID, toNumber,
                goh.ATTR_TITLE, ""
            }, true));

        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param number _more_
     *
     * @return _more_
     */
    private String getDataUrl(Request request, Entry entry, String number) {
        return HtmlUtils.url(request.makeUrl(getRepository().URL_ENTRY_SHOW),
                             new String[] {
            ARG_ENTRYID, entry.getId(), ARG_DB_VIEW, VIEW_CALL_GRAPH_DATA,
            ARG_NUMBER, number
        });
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
    public Result handleCallGraphData(Request request, Entry entry)
            throws Exception {
        StringBuilder      sb     = new StringBuilder();
        GraphOutputHandler goh    = getWikiManager().getGraphOutputHandler();
        String             number = request.getString(ARG_NUMBER, "");
        List<Clause>       where;
        where = new ArrayList<Clause>();
        where.add(Clause.eq(COL_ID, entry.getId()));
        where.add(
            Clause.or(
                Clause.eq(fromNumberColumn.getFullName(), number),
                Clause.eq(toNumberColumn.getFullName(), number)));
        List<Object[]> values = readValues(request, entry, Clause.and(where));
        List<String>   nodes  = new ArrayList<String>();
        List<String>   links  = new ArrayList<String>();
        getNodesAndLinks(request, entry, values, nodes, links);
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("\"nodes\":[\n");
        json.append(StringUtil.join(",", nodes));
        json.append("]");
        json.append(",\n");
        json.append("\"links\":[\n");
        json.append(StringUtil.join(",", links));
        json.append("]\n");
        json.append("}\n");

        return new Result(BLANK, json,
                          getRepository().getMimeTypeFromSuffix(".json"));

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param view _more_
     * @param action _more_
     * @param fromSearch _more_
     * @param valueList _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result makeListResults(Request request, Entry entry, String view,
                                  String action, boolean fromSearch,
                                  List<Object[]> valueList)
            throws Exception {
        if (view.equals(VIEW_CALL_LISTING)) {
            return handleCallListing(request, entry, valueList, fromSearch);
        }
        if (view.equals(VIEW_CALL_GRAPH)) {
            return handleCallGraphPage(request, entry, valueList, fromSearch);
        }

        return super.makeListResults(request, entry, view, action,
                                     fromSearch, valueList);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param valueList _more_
     * @param fromSearch _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleCallListing(Request request, Entry entry,
                                    List<Object[]> valueList,
                                    boolean fromSearch)
            throws Exception {

        Hashtable<String, Number> numberMap = new Hashtable<String, Number>();
        List<Number>              numbers   = new ArrayList<Number>();
        StringBuilder             sb        = new StringBuilder();
        addViewHeader(request, entry, sb, VIEW_CALL_LISTING,
                      valueList.size(), fromSearch);

        for (Object[] tuple : valueList) {
            String fromNumber = fromNumberColumn.getString(tuple);

            Number from       = numberMap.get(fromNumber);
            if (from == null) {
                from = new Number(fromNumber,
                                  fromNameColumn.getString(tuple));
                numbers.add(from);
                numberMap.put(from.number, from);
            }
            String toNumber = toNumberColumn.getString(tuple);
            Number to       = numberMap.get(toNumber);
            if (to == null) {
                to = new Number(toNumber, toNameColumn.getString(tuple));
                numbers.add(to);
                numberMap.put(to.number, to);
            }
            String date = dateColumn.getString(tuple);
            from.addOutbound(to, tuple);
            to.addInbound(from, tuple);
        }

        SimpleDateFormat sdf = getDateFormat(entry);

        Collections.sort(numbers);
        for (Number n : numbers) {
            //Only show numbers with outbounds
            if ((n.outbound.size() <= 1) && (n.inbound.size() <= 1)) {
                continue;
            }

            sb.append(HtmlUtils.p());
            sb.append(HtmlUtils.div(getTitle(n),
                                    HtmlUtils.cssClass("ramadda-heading-1")));

            if (n.outbound.size() > 0) {
                sb.append(
                    HtmlUtils.open(
                        HtmlUtils.TAG_DIV,
                        HtmlUtils.style("margin-top:0px;margin-left:20px;")));
                StringBuilder numberSB =
                    new StringBuilder("<table cellspacing=5 width=100%>");
                for (Number outbound : n.getSortedOutbound()) {
                    numberSB.append("<tr valign=top><td width=10%>");
                    String searchUrl =
                        HtmlUtils.url(
                            request.makeUrl(getRepository().URL_ENTRY_SHOW),
                            new String[] {
                        ARG_ENTRYID, entry.getId(), ARG_DB_SEARCH, "true",
                        getSearchUrlArgument(fromNumberColumn), n.number,
                        toNumberColumn.getSearchArg(), outbound.number,
                    });

                    numberSB.append(HtmlUtils.href(searchUrl,
                            getTitle(outbound)));
                    numberSB.append("</td>");

                    List<Object[]> calls  = n.getOutboundCalls(outbound);
                    StringBuilder  callSB = new StringBuilder();
                    for (Object[] values : calls) {
                        StringBuilder dateSB = new StringBuilder();
                        dateColumn.formatValue(entry, dateSB,
                                Column.OUTPUT_HTML, values, sdf);
                        StringBuilder html = new StringBuilder();
                        getHtml(request, html, entry, values);
                        callSB.append(
                            HtmlUtils.makeShowHideBlock(
                                dateSB.toString(),
                                HtmlUtils.insetLeft(
                                    HtmlUtils.div(
                                        html.toString(),
                                        HtmlUtils.cssClass(
                                            "ramadda-popup-box")), 10), false));
                    }
                    numberSB.append("<td width=5% align=right>");
                    numberSB.append(calls.size());
                    numberSB.append("</td><td width=85%>");
                    numberSB.append(HtmlUtils.makeShowHideBlock("Details",
                            HtmlUtils.insetLeft(callSB.toString(), 10),
                            false));
                    numberSB.append("</td></tr>");
                }
                numberSB.append("</table>");
                sb.append(HtmlUtils.makeShowHideBlock("Outbound",
                        HtmlUtils.insetLeft(numberSB.toString(), 10), true));
                sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
            }


            if (n.inbound.size() > 0) {
                sb.append(
                    HtmlUtils.open(
                        HtmlUtils.TAG_DIV,
                        HtmlUtils.style("margin-top:0px;margin-left:20px;")));
                StringBuilder numberSB =
                    new StringBuilder("<table cellspacing=5 width=100%>");
                for (Number inbound : n.getSortedInbound()) {
                    numberSB.append("<tr valign=top><td width=10%>");
                    String searchUrl =
                        HtmlUtils.url(
                            request.makeUrl(getRepository().URL_ENTRY_SHOW),
                            new String[] {
                        ARG_ENTRYID, entry.getId(), ARG_DB_SEARCH, "true",
                        getSearchUrlArgument(fromNumberColumn), n.number,
                        toNumberColumn.getSearchArg(), inbound.number,
                    });

                    numberSB.append(HtmlUtils.href(searchUrl,
                            getTitle(inbound)));
                    numberSB.append("</td>");

                    List<Object[]> calls  = n.getInboundCalls(inbound);
                    StringBuilder  callSB = new StringBuilder();
                    for (Object[] values : calls) {
                        StringBuilder dateSB = new StringBuilder();
                        dateColumn.formatValue(entry, dateSB,
                                Column.OUTPUT_HTML, values, sdf);
                        StringBuilder html = new StringBuilder();
                        getHtml(request, html, entry, values);
                        callSB.append(
                            HtmlUtils.makeShowHideBlock(
                                dateSB.toString(),
                                HtmlUtils.insetLeft(
                                    HtmlUtils.div(
                                        html.toString(),
                                        HtmlUtils.cssClass(
                                            "ramadda-popup-box")), 10), false));
                    }
                    numberSB.append("<td width=5% align=right>");
                    numberSB.append(calls.size());
                    numberSB.append("</td><td width=85%>");
                    numberSB.append(HtmlUtils.makeShowHideBlock("Details",
                            HtmlUtils.insetLeft(callSB.toString(), 10),
                            false));
                    numberSB.append("</td></tr>");
                }
                numberSB.append("</table>");
                sb.append(HtmlUtils.makeShowHideBlock("Inbound",
                        HtmlUtils.insetLeft(numberSB.toString(), 10), true));
                sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
            }

        }

        return new Result(getTitle(request, entry), sb);
    }


    /**
     * _more_
     *
     * @param n _more_
     *
     * @return _more_
     */
    private String getTitle(Number n) {
        if (Utils.stringDefined(n.name)) {
            return n.name + " (" + formatNumber(n.number) + ")";
        }

        return formatNumber(n.number);
    }

    /**
     *
     * @param entry _more_
     * @param values _more_
     * @param sdf _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getMapLabel(Entry entry, Object[] values,
                              SimpleDateFormat sdf)
            throws Exception {
        StringBuilder sb = new StringBuilder();
        dateColumn.formatValue(entry, sb, Column.OUTPUT_HTML, values, sdf);
        sb.append(": ");
        sb.append(formatNumber(fromNumberColumn.getString(values)));
        sb.append(ARROW);
        sb.append(formatNumber(toNumberColumn.getString(values)));

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param values _more_
     * @param sdf _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getKmlLabel(Entry entry, Object[] values,
                              SimpleDateFormat sdf)
            throws Exception {
        StringBuilder sb = new StringBuilder();
        //        dateColumn.formatValue(entry, sb, Column.OUTPUT_HTML, values, sdf);
        //        sb.append(": ");
        sb.append(formatNumber(fromNumberColumn.getString(values)));
        sb.append(" - ");
        sb.append(formatNumber(toNumberColumn.getString(values)));

        return sb.toString();
    }



    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getDefaultDateFormatString() {
        return "yyyy/MM/dd HH:mm z";
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param values _more_
     * @param sdf _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getCalendarLabel(Entry entry, Object[] values,
                                   SimpleDateFormat sdf)
            throws Exception {
        SimpleDateFormat timesdf = new SimpleDateFormat("HH:mm");
        timesdf.setTimeZone(sdf.getTimeZone());
        StringBuilder sb = new StringBuilder();
        dateColumn.formatValue(entry, sb, Column.OUTPUT_HTML, values,
                               timesdf);
        sb.append(": ");
        sb.append(formatNumber(fromNumberColumn.getString(values)));
        sb.append(ARROW);
        sb.append(formatNumber(toNumberColumn.getString(values)));

        return sb.toString();
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param formBuffer _more_
     */
    @Override
    public void createBulkForm(Request request, Entry entry, Appendable sb,
                               Appendable formBuffer) {
        StringBuilder bulkSB = new StringBuilder();
        makeForm(request, entry, bulkSB);
        StringBuilder bulkButtons = new StringBuilder();
        bulkButtons.append(HtmlUtils.submit(msg("Create entries"),
                                            ARG_DB_CREATE));
        bulkButtons.append(HtmlUtils.submit(msg("Cancel"), ARG_DB_LIST));
        bulkSB.append(HtmlUtils.p());
        bulkSB.append(header("Upload a call log file"));
        bulkSB.append(HtmlUtils.p());
        bulkSB.append(bulkButtons);
        bulkSB.append(HtmlUtils.p());
        bulkSB.append(msgLabel("File"));
        bulkSB.append(HtmlUtils.fileInput(ARG_DB_BULK_FILE,
                                          HtmlUtils.SIZE_60));

        bulkSB.append(HtmlUtils.p());
        bulkSB.append(msgLabel("Enter names for the numbers"));
        bulkSB.append(HtmlUtils.br());
        bulkSB.append(
            HtmlUtils.textArea(
                ARG_DB_NAMES,
                "#number=name\n#e.g.:\n#303-555-1212= some name\n", 8, 50));
        bulkSB.append(HtmlUtils.p());
        bulkSB.append(HtmlUtils.formEntry("",
                                          HtmlUtils.checkbox(ARG_ANONYMIZE,
                                              "true", false) + " "
                                                  + "Anonymize numbers"));
        bulkSB.append(HtmlUtils.p());
        bulkSB.append(bulkButtons);
        bulkSB.append(HtmlUtils.formClose());
        Utils.append(sb, bulkSB.toString());
    }



    /**
     * _more_
     *
     * @param date _more_
     *
     * @return _more_
     */
    public String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

        return sdf.format(date);
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        /*
          Hashtable<String,CellSite>   sites = CellSite.getSites(CellSite.CARRIER_VERIZON);
          for(String callFile: args) {
          String delimiter = "\r";
          String contents = IOUtil.readContents(callFile, CellSite.class);
          }
        */
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Aug 23, '13
     * @author         Enter your name here...
     */
    public static class Number implements Comparable {

        /** _more_ */
        String number;

        /** _more_ */
        String name;

        /** _more_ */
        int count = 0;

        /** _more_ */
        Hashtable<Number, List<Object[]>> outboundCalls =
            new Hashtable<Number, List<Object[]>>();

        /** _more_ */
        Hashtable<Number, List<Object[]>> inboundCalls =
            new Hashtable<Number, List<Object[]>>();

        /** _more_ */
        List<Number> outbound = new ArrayList<Number>();

        /** _more_ */
        List<Number> inbound = new ArrayList<Number>();


        /**
         * _more_
         *
         * @param number _more_
         * @param name _more_
         */
        public Number(String number, String name) {
            this.number = number;
            this.name   = name;
        }

        /**
         * _more_
         *
         * @param n _more_
         * @param tuple _more_
         */
        public void addOutbound(Number n, Object[] tuple) {
            count++;
            List<Object[]> calls = outboundCalls.get(n);
            if (calls == null) {
                outbound.add(n);
                outboundCalls.put(n, calls = new ArrayList<Object[]>());
            }
            calls.add(tuple);
        }

        /**
         * _more_
         *
         * @param n _more_
         * @param tuple _more_
         */
        public void addInbound(Number n, Object[] tuple) {
            count++;
            List<Object[]> calls = inboundCalls.get(n);
            if (calls == null) {
                inbound.add(n);
                inboundCalls.put(n, calls = new ArrayList<Object[]>());
            }
            calls.add(tuple);
        }



        /**
         * _more_
         *
         * @param n _more_
         *
         * @return _more_
         */
        public List<Object[]> getOutboundCalls(Number n) {
            return outboundCalls.get(n);
        }

        /**
         * _more_
         *
         * @param n _more_
         *
         * @return _more_
         */
        public List<Object[]> getInboundCalls(Number n) {
            return inboundCalls.get(n);
        }

        /**
         * _more_
         *
         * @param n _more_
         *
         * @return _more_
         */
        public int getOutboundCount(Number n) {
            return outboundCalls.get(n).size();
        }

        /**
         * _more_
         *
         * @param n _more_
         *
         * @return _more_
         */
        public int getInboundCount(Number n) {
            return inboundCalls.get(n).size();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public List<Number> getSortedOutbound() {
            Comparator comp = new Comparator() {
                public int compare(Object o1, Object o2) {
                    Number e1 = (Number) o1;
                    Number e2 = (Number) o2;
                    int    c1 = getOutboundCount(e1);
                    int    c2 = getOutboundCount(e2);
                    if (c1 < c2) {
                        return 1;
                    }
                    if (c1 > c2) {
                        return -1;
                    }

                    return 0;
                }
            };
            Object[] array = outbound.toArray();
            Arrays.sort(array, comp);

            return (List<Number>) Misc.toList(array);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public List<Number> getSortedInbound() {
            Comparator comp = new Comparator() {
                public int compare(Object o1, Object o2) {
                    Number e1 = (Number) o1;
                    Number e2 = (Number) o2;
                    int    c1 = getInboundCount(e1);
                    int    c2 = getInboundCount(e2);
                    if (c1 < c2) {
                        return 1;
                    }
                    if (c1 > c2) {
                        return -1;
                    }

                    return 0;
                }
            };
            Object[] array = inbound.toArray();
            Arrays.sort(array, comp);

            return (List<Number>) Misc.toList(array);
        }



        /**
         * _more_
         *
         * @param o _more_
         *
         * @return _more_
         */
        public int compareTo(Object o) {
            Number that = (Number) o;
            if (this.count < that.count) {
                return 1;
            }
            if (this.count > that.count) {
                return -1;
            }

            return 0;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        @Override
        public int hashCode() {
            return number.hashCode();
        }

        /**
         * _more_
         *
         * @param o _more_
         *
         * @return _more_
         */
        @Override
        public boolean equals(Object o) {
            if ( !(o instanceof Number)) {
                return false;
            }

            return number.equals(((Number) o).number);
        }
    }




}
