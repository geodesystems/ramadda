/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.records;


import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.MyDateFormat;

import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;


/**
 */
@SuppressWarnings("unchecked")
public class SimpleRecordsTypeHandler extends PointTypeHandler {


    /** _more_ */
    private static int IDX = PointTypeHandler.IDX_LAST + 1;

    /** _more_ */
    private static int IDX_FIELDS = IDX++;

    /** _more_ */
    private static int IDX_DATA = IDX++;


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public SimpleRecordsTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
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
        return new SimpleRecordsRecordFile(getRepository(), entry);
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
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {
        return "";
    }

    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     */
    public String cleanValue(String value) {
        value = value.replaceAll("\r", "");
        value = value.replaceAll(":", "_colon_");
        value = value.replaceAll("\n", "_nl_");
        value = value.replaceAll(",", "_comma_");

        return value;
    }


    /**
     * _more_
     *
     * @param time _more_
     *
     * @return _more_
     */
    public String getTimeValue(String time) {
        if (time.length() == 0) {
            System.err.println(" adding time");
            time = "00:00:00";
        } else if (time.matches("^\\d\\d?$")) {
            System.err.println(" adding mm:ss");
            time += ":00:00";
        } else if (time.matches("^\\d\\d?:\\d\\d?$")) {
            System.err.println(" adding ss");
            time += ":00";
        }
        System.err.println("time:" + time);

        return "T" + time;
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
    public Result processEntryAccess(Request request, Entry entry)
            throws Exception {

        if ( !getAccessManager().canDoEdit(request, entry)) {
            return new Result("Error",
                              new StringBuilder("Cannot edit entry"));
        }
        List<RecordField> fields =
            new SimpleRecordsRecordFile(getRepository(),
                                        entry).getFields(false);
        String action = request.getString("action", "none");
        if (action.equals("new")) {
            StringBuilder line = new StringBuilder();
            int           cnt  = 0;
            for (RecordField field : fields) {
                if (cnt > 0) {
                    line.append(",");
                }
                line.append(field.getName());
                line.append(":");
                String value = request.getString(field.getName(), "");
                if (Misc.equals(field.getType(), "date")) {
                    String time = request.getString(field.getName()
                                      + ".time", "").trim();
                    value += getTimeValue(time);
                }
                value = cleanValue(value);
                line.append(value);
                cnt++;
            }
            String data = entry.getStringValue(request,IDX_DATA, "").trim();
            data = data + "\n" + line;
            entry.setValue(IDX_DATA, data);
            getEntryManager().updateEntry(request, entry);
        } else if (action.equals("newform")) {
            StringBuilder sb = new StringBuilder();
            getPageHandler().entrySectionOpen(request, entry, sb,
                    "New Record");
            sb.append(HtmlUtils.form(getRepository().getUrlBase()
                                     + "/entry/access"));
            sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
            sb.append(HtmlUtils.hidden("action", "new"));
            sb.append(HtmlUtils.submit("Add Record", ARG_SUBMIT));
            sb.append(HtmlUtils.formTable());
            boolean skipNextField = false;
            for (RecordField field : fields) {
                if (skipNextField) {
                    skipNextField = false;

                    continue;
                }
                String extra = "";
                String desc  = field.getDescription();
                if (desc == null) {
                    desc = "";
                }
                if (field.isTypeNumeric()) {
                    extra += " size=5 ";
                }
                String rows = (String) field.getProperty("rows", null);
                String dflt = (String) field.getProperty("default", "");
                boolean showTime = Misc.equals("true",
                                       field.getProperty("showTime",
                                           "false"));
                String widget = null;
                List   values = (List) field.getProperty("values", null);
                if (values != null) {
                    widget = HtmlUtils.select(field.getName(), values, dflt);
                } else if (rows != null) {
                    widget = HtmlUtils.textArea(field.getName(), dflt,
                            Integer.parseInt(rows), 60);
                } else if (field.getIsLatitude()) {

                    /**
                     * MapInfo map = getRepository().getMapManager().createMap(request,
                     *                                                       entry, true, null);
                     * widget = map.makeSelector(field.getName(), true,
                     *                 new String[] { latLonOk(lat)
                     *                                ? lat + ""
                     *                                : "", latLonOk(lon)
                     *                                ? lon + ""
                     *                                : "" });
                     * skipNextField = true;
                     */
                } else if (Misc.equals(field.getType(), "date")) {
                    widget = getDateHandler().makeDateInput(request,
                            field.getName(), "flexiform", new Date(),
							    getEntryUtil().getTimezone(request, entry), showTime);
                } else {
                    widget = HtmlUtils.input(field.getName(), dflt, extra);
                }
                sb.append(HtmlUtils.formEntry(field.getLabel() + ":",
                        widget + " " + desc));
            }
            sb.append(HtmlUtils.formTableClose());
            sb.append(HtmlUtils.submit("Add Record", ARG_SUBMIT));
            sb.append(HtmlUtils.formClose());
            getPageHandler().entrySectionClose(request, entry, sb);
            Result r = new Result("Edit", sb);
            r.setShouldDecorate(true);

            return getEntryManager().addEntryHeader(request, entry, r);
        } else if (action.equals("edit")) {
            StringBuilder sb = new StringBuilder();
            getPageHandler().entrySectionOpen(request, entry, sb, "Edit");
            sb.append(
                HtmlUtils.href(
                    getRepository().getUrlBase()
                    + "/entry/access?action=newform&entryid="
                    + entry.getId(), "Add Record",
                                     " class=ramadda-button") + "<p>");
            sb.append(HtmlUtils.formPost(getRepository().getUrlBase()
                                         + "/entry/access"));
            sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
            sb.append(HtmlUtils.hidden("action", "doedit"));
            sb.append(HtmlUtils.submit("Edit", ARG_SUBMIT));
            sb.append("<p>");
            sb.append("<table class=entry-table>");
            int                cnt  = 0;
            List<List<String>> rows = getRows(entry, fields);
            sb.append("<thead valign=top>");
            sb.append(HtmlUtils.th("Delete"));
            for (RecordField field : fields) {
                sb.append(HtmlUtils.th(field.getLabel(), " align=center"));
            }
            sb.append("</tr>");
            boolean odd = true;
            for (List<String> cols : rows) {
                sb.append("<tr valign=top " + (odd
                        ? " class=ramadda-row-odd "
                        : " class=ramadda-row-even ") + ">");
                odd = !odd;
                sb.append(HtmlUtils.td(HtmlUtils.checkbox("del_" + cnt,
                        "true", false)));
                sb.append(HtmlUtils.hidden("row" + cnt, "true"));
                for (int i = 0; i < cols.size(); i++) {
                    RecordField field = fields.get(i);
                    boolean showTime = Misc.equals("true",
                                           field.getProperty("showTime",
                                               "false"));
                    String col = cols.get(i);
                    sb.append("<td>");
                    col = col.replaceAll("\"", "&quote;");
                    String widget;
                    String arg    = "v_" + cnt + "_" + i;
                    List   values = (List) field.getProperty("values", null);
                    String wrows  = (String) field.getProperty("rows", null);
                    if (values != null) {
                        widget = HtmlUtils.select(arg, values, col);
                    } else if (wrows != null) {
                        widget = HtmlUtils.textArea(arg, col,
                                Integer.parseInt(wrows), 20);
                    } else if (field.isTypeNumeric()) {
                        widget = HtmlUtils.input(arg, col, " size=5 ");
                    } else if (Misc.equals(field.getType(), "date")) {
                        try {
                            widget = getDateHandler().makeDateInput(request,
                                    arg, "flexiform", Utils.parseDate(col),
								    getEntryUtil().getTimezone(request, entry),
                                    showTime);
                        } catch (Exception exc) {
                            widget = "Bad date: "
                                     + HtmlUtils.input(arg, col, "");
                        }
                    } else {
                        widget = HtmlUtils.input(arg, col, "");
                    }
                    sb.append(widget);
                    sb.append("</td>");
                }

                sb.append("</tr>");
                cnt++;
            }
            sb.append("</table>");
            sb.append("<br>");
            sb.append(HtmlUtils.submit("Edit", ARG_SUBMIT));
            sb.append(HtmlUtils.formClose());
            getPageHandler().entrySectionClose(request, entry, sb);
            Result r = new Result("Edit", sb);
            r.setShouldDecorate(true);

            return getEntryManager().addEntryHeader(request, entry, r);
        } else if (action.equals("doedit")) {
            StringBuilder csv = new StringBuilder();
            int           cnt = 0;
            while (request.defined("row" + cnt)) {
                if (request.get("del_" + cnt, false)) {
                    cnt++;

                    continue;
                }
                for (int i = 0; i < fields.size(); i++) {
                    RecordField field = fields.get(i);
                    String      arg   = "v_" + cnt + "_" + i;
                    if (i > 0) {
                        csv.append(",");
                    }
                    String value = request.getString(arg);
                    if (Misc.equals(field.getType(), "date")) {
                        String time = request.getString(arg + ".time",
                                          "").trim();
                        value += getTimeValue(time);
                    }
                    csv.append(field.getName() + ":" + cleanValue(value));
                }
                csv.append("\n");
                cnt++;
            }
            entry.setValue(IDX_DATA, csv.toString());
            getEntryManager().updateEntry(request, entry);

            return new Result(getRepository().getUrlBase()
                              + "/entry/access?action=edit&entryid="
                              + entry.getId());
        } else {
            return new Result("Error",
                              new StringBuilder("Unknown action:" + action));
        }

        String url = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry);

        return new Result(url);

    }


    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if (tag.equals("simple_add")) {
            if ( !getAccessManager().canDoEdit(request, entry)) {
                return "";
            }

            return HtmlUtils.href(getRepository().getUrlBase()
                    + "/entry/access?action=newform&entryid="
                    + entry.getId(), "Add Record",
                        " class=ramadda-button") + " "
                            + HtmlUtils.href(getRepository().getUrlBase()
                                + "/entry/access?action=edit&entryid="
                                    + entry.getId(), "Edit Records",
                                        " class=ramadda-button") + "<p>";
        }

        return null;
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param recordFields _more_
     *
     * @return _more_
     */
    public List<List<String>> getRows(Entry entry,
                                      List<RecordField> recordFields) {
	Request request = getRepository().getAdminRequest();
        List<List<String>> rows = new ArrayList<List<String>>();
        for (String line :
                StringUtil.split(entry.getStringValue(request,IDX_DATA, ""), "\n", true,
                                 true)) {
            List<String>              toks = StringUtil.split(line, ",");
            Hashtable<String, String> map  = new Hashtable<String, String>();
            for (String tuple : toks) {
                List<String> toks2 = StringUtil.splitUpTo(tuple, ":", 2);
                String       value = (toks2.size() == 1)
                                     ? ""
                                     : toks2.get(1);
                value = value.replaceAll("_colon_", ":");
                value = value.replaceAll("_nl_", "\n");
                value = value.replaceAll("_comma_", ",");
                map.put(toks2.get(0), value);
            }
            int  cnt  = 0;
            List cols = new ArrayList();
            rows.add(cols);
            for (RecordField field : recordFields) {
                String value = map.get(field.getName());
                if (value == null) {
                    value = "";
                }
                cols.add(value);
                cnt++;
            }
        }

        return rows;
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Dec 8, '18
     * @author         Enter your name here...
     */
    public class SimpleRecordsRecordFile extends CsvFile {

        /** _more_ */
        Repository repository;

        /** _more_ */
        Entry entry;

        /** _more_ */
        List<RecordField> recordFields;

        /**
         * _more_
         *
         *
         * @param repository _more_
         * @param entry _more_
         *
         * @throws IOException _more_
         */
        public SimpleRecordsRecordFile(Repository repository, Entry entry)
                throws IOException {
            this.repository = repository;
            this.entry      = entry;
            //            putProperty("output.time","false");
        }


        /**
         * _more_
         *
         * @param buffered _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public InputStream doMakeInputStream(boolean buffered)
                throws Exception {
            getFields(false);
            //field:value,field:value,
            StringBuilder      csv  = new StringBuilder();
            List<List<String>> rows = getRows(entry, recordFields);
            for (List<String> cols : rows) {
                csv.append(Utils.columnsToString(cols, ","));
                csv.append("\n");
            }
            byte[]               bytes = csv.toString().getBytes();
            ByteArrayInputStream bais  = new ByteArrayInputStream(bytes);

            return bais;

        }

        /**
         * _more_
         *
         * @param failureOk _more_
         *
         * @return _more_
         */
        @Override
        public List<RecordField> getFields(boolean failureOk) {
            if (recordFields != null) {
                return recordFields;
            }
	    Request request = getRepository().getAdminRequest();
            recordFields = new ArrayList<RecordField>();
            int cnt = 1;
            for (String line :
                    StringUtil.split(entry.getStringValue(request,IDX_FIELDS, ""), "\n",
                                     true, true)) {
                if (line.startsWith("#")) {
                    continue;
                }
                List<String> toks     = StringUtil.splitUpTo(line, " ", 2);
                String       id       = toks.get(0);
                String       desc     = "";
                String       type     = "double";
                String       rows     = null;
                String       dflt     = null;
                String       values   = null;
                String       label    = null;
                String       showTime = null;
                if (toks.size() == 2) {
                    List<String> args = Utils.parseCommandLine(toks.get(1));
                    for (int i = 0; i < args.size(); i += 2) {
                        String arg = args.get(i);
                        if (arg.equals("type")) {
                            type = args.get(i + 1);
                        } else if (arg.equals("description")) {
                            desc = args.get(i + 1);
                        } else if (arg.equals("showTime")) {
                            showTime = args.get(i + 1);
                        } else if (arg.equals("label")) {
                            label = args.get(i + 1);
                        } else if (arg.equals("rows")) {
                            rows = args.get(i + 1);
                        } else if (arg.equals("values")) {
                            values = args.get(i + 1);
                        } else if (arg.equals("default")) {
                            dflt = args.get(i + 1);
                        } else {
                            System.err.println("unknown arg:" + arg);
                        }
                    }
                }
                if (type.equals("location")) {
                    RecordField field1 = new RecordField("latitude",
                                             "Latitude", desc, cnt, null);
                    field1.setIsLatitude(true);
                    field1.setType("double");
                    cnt++;
                    RecordField field2 = new RecordField("longitude",
                                             "Longitude", desc, cnt, null);
                    field2.setIsLongitude(true);
                    field2.setType("double");
                    recordFields.add(field1);
                    recordFields.add(field2);
                } else {
                    RecordField field = new RecordField(id, (label != null)
                            ? label
                            : Utils.makeLabel(id), desc, cnt, null);
                    field.setType(type);
                    if (type.equals("date")) {
			String tz =  getEntryUtil().getTimezone(repository.getTmpRequest(), entry);
                        field.setDateFormat(new MyDateFormat("yyyy-MM-dd'T'HH:mm:ss Z",tz));
                    }

                    if (type.equals("date") || field.isTypeNumeric()) {
                        field.setChartable(true);
                    }
                    if (values != null) {
                        field.setProperty("values",
                                          StringUtil.split(values, ",", true,
                                              true));
                    }
                    if (rows != null) {
                        field.setProperty("rows", rows);
                    }
                    if (showTime != null) {
                        field.setProperty("showTime", showTime);
                    }
                    if (dflt != null) {
                        field.setProperty("default", dflt);
                    }
                    recordFields.add(field);
                    cnt++;
                }
                //, attrFormat("yyyy-D")),  attrChartable(),  attrUnit("seconds"), attrLabel("Day Length")),
            }

            return recordFields;
        }

        /**
         * _more_
         *
         * @param visitInfo _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        public VisitInfo prepareToVisit(VisitInfo visitInfo)
                throws Exception {
            super.prepareToVisit(visitInfo);

            return visitInfo;
        }
    }


}
