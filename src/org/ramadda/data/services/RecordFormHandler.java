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

package org.ramadda.data.services;


import org.ramadda.data.point.PointFile;
import org.ramadda.data.record.*;



import org.ramadda.data.record.*;




import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.job.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;

import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;



import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

import java.io.*;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 * Creates most of the web interfaces - the subset/products form, map overview, etc.
 *
 * @author         Jeff McWhirter
 */
public class RecordFormHandler extends RepositoryManager implements RecordConstants {

    /** _more_ */
    public static final String ARG_START = "start";

    /** _more_ */
    public static final String ARG_NUMPOINTS = "numpoints";

    /** an array of colors */
    public static final Color[] COLORS = {
        Color.blue, Color.black, Color.red, Color.green, Color.orange,
        Color.cyan, Color.magenta, Color.pink, Color.yellow
    };




    /** formats # points */
    private static DecimalFormat pointCountFormat =
        new DecimalFormat("#,##0");

    /** formats size */
    private DecimalFormat sizeFormat = new DecimalFormat("####0.00");

    /** date format */
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm Z");

    /** the output handler */
    private RecordOutputHandler recordOutputHandler;

    /** _more_ */
    public static final String UNIT_M = "m";


    /**
     * ctor
     *
     * @param recordOutputHandler _more_
     */
    public RecordFormHandler(RecordOutputHandler recordOutputHandler) {
        super(recordOutputHandler.getRepository());
        this.recordOutputHandler = recordOutputHandler;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public RecordOutputHandler getOutputHandler() {
        return recordOutputHandler;
    }


    /**
     * get the job manager
     *
     * @return the job manager
     */
    public JobManager getJobManager() {
        return recordOutputHandler.getRecordJobManager();
    }


    /**
     * get the job manager
     *
     * @return the job manager
     */
    public RecordJobManager getRecordJobManager() {
        return (RecordJobManager) recordOutputHandler.getRecordJobManager();
    }


    /**
     * format the file size
     *
     * @param bytes number of bytes
     *
     * @return formatted size
     */
    public String formatFileSize(long bytes) {
        if (bytes < 1000) {
            return "" + bytes;
        }
        if (bytes < 1000000) {
            return sizeFormat.format(bytes / 1000.0) + "&nbsp;KB";
        }
        if (bytes < 1000000000) {
            return sizeFormat.format(bytes / 1000000.0) + "&nbsp;MB";
        }

        return sizeFormat.format(bytes / 1000000000.0) + "&nbsp;GB";
    }


    /**
     * make the selector for the given format
     *
     * @param t format
     *
     * @return selector
     */
    public HtmlUtils.Selector getSelect(OutputType t) {
        if (t.getIcon() != null) {
            return new HtmlUtils.Selector(
                t.getLabel(), t.getId(),
                getRepository().iconUrl(t.getIcon()));
        }

        return new HtmlUtils.Selector(t.getLabel(), t.getId(), null);
    }


    /**
     * format number of points
     *
     * @param cnt number of points
     *
     * @return formatted points
     */
    public static String formatPointCount(long cnt) {
        synchronized (pointCountFormat) {
            return pointCountFormat.format(cnt);
        }
    }


    /**
     * format date
     *
     * @param date date
     *
     * @return formatted date
     */
    public String formatDate(Date date) {
        synchronized (sdf) {
            return sdf.format(date);
        }
    }




    /**
     * list the metadata for the given entry
     *
     * @param request request
     * @param outputType output type
     * @param recordEntry _more_
     *
     * @return ramadda result
     *
     * @throws Exception On badness
     */
    public Result outputEntryMetadata(Request request, OutputType outputType,
                                      RecordEntry recordEntry)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        getEntryMetadata(request, recordEntry, sb);

        return new Result("", sb);
    }



    /**
     * make a color object
     *
     * @param request the request
     * @param arg which url arg
     * @param colorCnt colors
     *
     * @return the color
     */
    public Color getColor(Request request, String arg, int[] colorCnt) {
        Color c = null;
        if (request.defined(arg)) {
            String cs = request.getString(arg, null);
            if ( !cs.startsWith("#")) {
                cs = "#" + cs;
            }
            c = HtmlUtils.decodeColor(cs, (Color) null);
        }
        if (c == null) {
            c = COLORS[colorCnt[0] % COLORS.length];
            colorCnt[0]++;
        }

        return c;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param recordEntry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void getEntryMetadata(Request request, RecordEntry recordEntry,
                                 StringBuffer sb)
            throws Exception {
        try {
            getEntryMetadataInner(request, recordEntry, sb);
        } catch (Exception exc) {
            //For now ignore the error since if we encounter bad data doing anything with the entry is broken
            getLogManager().logError("Error reading record metadata:"
                                     + recordEntry.getEntry().getName(), exc);
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param recordEntry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    private void getEntryMetadataInner(Request request,
                                       RecordEntry recordEntry,
                                       StringBuffer sb)
            throws Exception {
        RecordFile recordFile = recordEntry.getRecordFile();
        if (recordFile == null) {
            return;
        }

        sb.append(recordFile.getHtmlDescription());
        List<RecordField> fields = null;

        //Don't do this for now
        //recordEntry.getRecordFile().getFields(true);


        if (fields == null) {
            return;
        }
        long numRecords = recordEntry.getNumRecords();
        if (numRecords > 0) {
            sb.append(HtmlUtils.b(msgLabel("Number of points")));
            sb.append(" " + numRecords);
        } else {
            sb.append(HtmlUtils.b(msgLabel("Number of points")));
            sb.append(" " + msg("unknown"));
        }
        sb.append(msgHeader("Fields"));
        sb.append(HtmlUtils.formTable());
        sb.append(HtmlUtils.row(HtmlUtils.cols(new Object[] {
            HtmlUtils.b(msg("Field Name")),
            HtmlUtils.b(msg("Label")), HtmlUtils.b(msg("Description")),
            HtmlUtils.b(msg("Unit")), HtmlUtils.b(msg("Type")) })));
        for (RecordField field : fields) {
            String type = field.getRawType();
            if (field.getArity() > 1) {
                //              type = type +" [" + field.getArity() +"]";
            }
            String unit = field.getUnit();
            if (unit == null) {
                unit = "";
            }

            sb.append(HtmlUtils.rowTop(HtmlUtils.cols(new Object[] {
                field.getName(),
                field.getLabel(), field.getDescription(), unit,
                ((type == null)
                 ? ""
                 : type) })));
        }
        sb.append(HtmlUtils.formTableClose());

        StringBuffer info = new StringBuffer();
        recordEntry.getRecordFile().getInfo(info);
        if (info.length() > 0) {
            sb.append(msgHeader("Extra"));
            sb.append(HtmlUtils.pre(info.toString()));
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param recordEntry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntryView(Request request, OutputType outputType,
                                  final RecordEntry recordEntry)
            throws Exception {

        final StringBuffer sb = new StringBuffer();
        final List<RecordField> fields =
            recordEntry.getRecordFile().getFields();
        int start = request.get(ARG_START, 0);
        request.put(ARG_START, start + 50);
        int step = 50;
        sb.append(HtmlUtils.href(request.getUrl(), msg("Next") + " " + step));
        sb.append(HtmlUtils.br());
        sb.append("<table cellspacing=0 cellpadding=5 border=1>");
        final int[]   cnt     = { 0 };
        RecordVisitor visitor = new BridgeRecordVisitor(getOutputHandler()) {

            public boolean doVisitRecord(RecordFile file,
                                         VisitInfo visitInfo, Record record) {

                if (cnt[0] == 0) {
                    String style = HtmlUtils.style("background: #c3d9ff;");
                    sb.append("<tr valign=bottom>");
                    sb.append("<td " + style + ">");
                    sb.append(HtmlUtils.b("Index"));
                    sb.append("</td>");
                    for (int fieldCnt = 0; fieldCnt < fields.size();
                            fieldCnt++) {
                        RecordField field = fields.get(fieldCnt);
                        if (field.getSkip()) {
                            continue;
                        }
                        if (field.getSynthetic()) {
                            continue;
                        }
                        if (field.isBitField()) {
                            String[] bitFields = field.getBitFields();
                            sb.append("<td align=center " + style + ">");
                            sb.append(field.getName());
                            sb.append("</td>");
                            for (int i = 0; i < bitFields.length; i++) {
                                sb.append("<td align=center " + style + ">");
                                sb.append(HtmlUtils.b("Bit #" + i + "<br>"
                                        + bitFields[i]));
                                sb.append("</td>");
                            }

                        } else {
                            sb.append("<td align=center " + style + ">");
                            if (field.getArity() > 1) {
                                sb.append(HtmlUtils.b(field.getName()
                                        + "&nbsp;[" + field.getArity()
                                        + "]"));
                            } else {
                                sb.append(HtmlUtils.b(field.getName()));
                            }
                            sb.append("</td>");
                        }
                    }
                    sb.append("</tr>");
                }
                sb.append("<tr>");
                sb.append("<td>");
                sb.append(visitInfo.getRecordIndex());
                sb.append("</td>");
                for (int fieldCnt = 0; fieldCnt < fields.size(); fieldCnt++) {
                    RecordField field = fields.get(fieldCnt);
                    if (field.getSynthetic()) {
                        continue;
                    }
                    if (field.getSkip()) {
                        continue;
                    }

                    if (field.isTypeString()) {
                        sb.append("<td align=right>");
                        sb.append(record.getStringValue(field.getParamId()));
                        sb.append("</td>");

                        continue;
                    }
                    if (field.isTypeDate()) {
                        sb.append("<td align=right>");
                        Date date =
                            (Date) record.getObjectValue(field.getParamId());
                        sb.append(formatDate(date));
                        sb.append("</td>");

                        continue;
                    }
                    if (field.isBitField()) {
                        String[] bitFields = field.getBitFields();
                        int value = (int) record.getValue(field.getParamId());
                        sb.append("<td align=right>");
                        sb.append(value);
                        sb.append("</td>");
                        for (int i = 0; i < bitFields.length; i++) {
                            sb.append("<td align=right>");
                            if ((value & 1 << i) != 0) {
                                sb.append("1");
                            } else {
                                sb.append("0");
                            }
                            sb.append("</td>");
                        }

                        continue;
                    }


                    sb.append("<td align=right>");
                    if (field.getArity() > 1) {
                        sb.append("...");
                    } else {
                        ValueGetter getter = field.getValueGetter();
                        //                        System.err.println("field:" + field);
                        if (getter == null) {
                            double value =
                                record.getValue(field.getParamId());
                            sb.append("" + value);
                        } else {
                            sb.append(getter.getStringValue(record, field,
                                    visitInfo));
                        }
                    }
                    sb.append("</td>");
                }
                cnt[0]++;

                return true;

            }

        };

        VisitInfo visitInfo = new VisitInfo();
        visitInfo.setStart(start);
        visitInfo.setStop(start + step);
        getRecordJobManager().visitSequential(request, recordEntry, visitor,
                visitInfo);

        sb.append("</table>");

        return new Result("", sb);

    }


    /**
     * get the product formats selected in the request
     *
     * @param request the request
     *
     * @return product formats
     */
    public HashSet<String> getFormats(Request request) {
        HashSet<String> formats = new HashSet<String>();
        for (String format :
                (List<String>) request.get(ARG_PRODUCT,
                                           new ArrayList<String>())) {
            formats.add(format);
        }

        return formats;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param bounds _more_
     *
     * @return _more_
     */
    public double getDefaultRadiusDegrees(Request request,
                                          Rectangle2D.Double bounds) {
        int width = request.get(ARG_WIDTH, DFLT_WIDTH);
        if ((bounds != null) && (width != 0)) {
            double degreesPerCell = bounds.getWidth() / width;

            return (degreesPerCell * 2);
        }

        return 0;
    }



}
