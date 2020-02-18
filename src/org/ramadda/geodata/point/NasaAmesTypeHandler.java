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

package org.ramadda.geodata.point;


import org.ramadda.data.point.PointMetadataHarvester;


import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.record.*;


import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordEntry;
import org.ramadda.data.services.RecordTypeHandler;


import org.ramadda.repository.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;


import org.ramadda.util.Utils;
import org.ramadda.util.text.CsvUtil;


import org.w3c.dom.*;

import ucar.unidata.geoloc.Bearing;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class NasaAmesTypeHandler extends PointTypeHandler {


    /** _more_ */
    private SimpleDateFormat dateSDF;

    /** _more_ */
    private static int IDX = PointTypeHandler.IDX_LAST + 1;

    /** _more_ */
    public static final int IDX_DIMENSIONS = IDX++;

    /** _more_ */
    public static final int IDX_ORIGINATOR_NAME = IDX++;

    /** _more_ */
    public static final int IDX_AFFILIATION = IDX++;

    /** _more_ */
    public static final int IDX_INSTRUMENT = IDX++;

    /** _more_ */
    public static final int IDX_CAMPAIGN = IDX++;

    /** _more_ */
    public static final int IDX_COMMENTS = IDX++;




    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public NasaAmesTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
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
        return new NasaAmesRecordFile(getPathForEntry(request, entry));
    }

    /**
     * _more_
     *
     * @param recordEntry _more_
     * @param metadata _more_
     *
     * @throws Exception _more_
     */
    protected void handleHarvestedMetadata(RecordEntry recordEntry,
                                           PointMetadataHarvester metadata)
            throws Exception {
        super.handleHarvestedMetadata(recordEntry, metadata);
        NasaAmesRecordFile f =
            (NasaAmesRecordFile) recordEntry.getRecordFile();
        Entry entry = recordEntry.getEntry();
        entry.setValue(IDX_DIMENSIONS, f.numDimensions);
        entry.setValue(IDX_ORIGINATOR_NAME, f.name);
        entry.setValue(IDX_AFFILIATION, f.affiliation);
        entry.setValue(IDX_INSTRUMENT, f.instrument);
        entry.setValue(IDX_CAMPAIGN, f.campaign);
        entry.setValue(IDX_COMMENTS, f.comments);
        if ( !metadata.hasTimeRange() && (f.startDate != null)) {
            entry.setStartAndEndDate(f.startDate.getTime());
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Dec 8, '18
     * @author         Enter your name here...
     */
    public static class NasaAmesRecordFile extends CsvFile {


        /** _more_ */
        String name;

        /** _more_ */
        int numDimensions;

        /** _more_ */
        String affiliation;

        /** _more_ */
        String instrument;

        /** _more_ */
        String campaign;

        /** _more_ */
        StringBuilder comments = new StringBuilder();

        /** _more_ */
        Date startDate;

        /** _more_ */
        private SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


        /** _more_ */
        public static final int DATEUNIT_HOURS = 1;

        /** _more_ */
        public static final int DATEUNIT_DAYS = 2;

        /** _more_ */
        public static final int DATEUNIT_SECONDS = 3;

        /** _more_ */
        private int dateIdx = -1;

        /** _more_ */
        private boolean doElapsed = false;

        /** _more_ */
        private int dateUnit;

        /** _more_ */
        private long dateBase;

        /**
         * _more_
         *
         * @param filename _more_
         *
         * @throws IOException _more_
         */
        public NasaAmesRecordFile(String filename) throws IOException {
            super(filename);
        }


        /**
         * Class description
         *
         *
         * @version        $version$, Sun, Dec 22, '19
         * @author         Enter your name here...
         */
        private static class Var {

            /** _more_ */
            String id;

            /** _more_ */
            String label;

            /** _more_ */
            String unit;

            /** _more_ */
            String scale;

            /** _more_ */
            String missing;

            /**
             * _more_
             *
             * @param id _more_
             * @param label _more_
             * @param unit _more_
             */
            public Var(String id, String label, String unit) {
                this.id    = id;
                this.label = label;
                this.unit  = unit;
            }

            /**
             * _more_
             *
             * @param id _more_
             * @param label _more_
             * @param unit _more_
             * @param scale _more_
             * @param missing _more_
             */
            public Var(String id, String label, String unit, String scale,
                       String missing) {
                this(id, label, unit);
                this.scale   = scale;
                this.missing = missing;
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public String toString() {
                return "id:" + id + " label:" + label + " unit:" + unit;
            }

        }


        /**
         * _more_
         *
         * @param tok _more_
         *
         * @return _more_
         */
        private Var parseVar(String tok) {
            tok = tok.trim();
            String var   = tok;
            String label = var;
            String unit  = "";
            int    idx   = var.indexOf("(");
            if (idx >= 0) {
                //              System.err.println("VAR:" + var+ " idx:" + idx);
                var  = tok.substring(0, idx - 1).trim();
                unit = tok.substring(idx + 1, tok.length() - 1).trim();
            } else {
                List<String> toks = StringUtil.split(var, ",", true, true);
                if (toks.size() >= 2) {
                    var  = toks.get(0);
                    unit = toks.get(1);
                    if (toks.size() > 2) {
                        label = toks.get(2);
                    }
                }
            }
            var = var.replaceAll(",", "_").replaceAll("_$",
                                 "").replaceAll(" ", "_").trim();
            String _var = var.toLowerCase();
            if (_var.equals("lat") || _var.equals("gps_latitude")) {
                var = "latitude";
            } else if (_var.equals("lon") || _var.equals("gps_longitude")) {
                var = "longitude";
            }

            unit = unit.replaceAll(",", " - ");

            return new Var(Utils.makeID(var), label, unit);
        }

        /**
         * _more_
         *
         * @param s _more_
         *
         * @return _more_
         */
        private String clean(String s) {
            int idx = s.indexOf(";");
            if (idx >= 0) {
                s = s.substring(0, idx);
            }

            return s.trim();
        }

        /**
         * _more_
         *
         * @param s _more_
         *
         * @return _more_
         */
        public List<String> tokenize(String s) {
            s = clean(s);

            return StringUtil.split(s, (s.indexOf(",") >= 0)
                                       ? ","
                                       : " ", true, true);
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

            boolean      debug = false;
            List<String> toks  = tokenize(visitInfo.getRecordIO().readLine());
            int numHeaderLines = (int) Double.parseDouble(toks.get(0));
            numDimensions = Integer.parseInt(toks.get(1).substring(0, 1));
            putProperty("delimiter", "commasorspaces");
            putProperty(PROP_SKIPLINES, "" + (numHeaderLines - 1));
            super.prepareToVisit(visitInfo);
            List<String> header = getHeaderLines();
            int          idx    = 0;
            name        = header.get(idx++);
            affiliation = header.get(idx++);
            instrument  = header.get(idx++);
            campaign    = header.get(idx++);
            //Skip volume
            header.get(idx++);
            List<String> dateToks = tokenize(header.get(idx++));
            if (dateToks.size() == 6) {
                startDate =
                    new SimpleDateFormat("yyyy-MM-dd").parse(dateToks.get(0)
                                         + "-" + dateToks.get(1) + "-"
                                         + dateToks.get(2));
            }

            List<Var> vars = new ArrayList<Var>();
            //      String tmps = clean(header.get(idx++));
            String intervalSize = header.get(idx++);
            if (debug) {
                System.err.println("num dimensions:" + numDimensions);
            }
            for (int i = 0; i < numDimensions; i++) {
                //              if(debug)System.err.println("\tindependent:" + header.get(idx));
                Var var = parseVar(clean(header.get(idx++)));
                if (debug) {
                    System.err.println("\tindependent:" + var);
                }
                vars.add(var);
            }

            List<String> missings = new ArrayList<String>();
            List<String> scales   = new ArrayList<String>();

            for (int indCnt = 0; indCnt < numDimensions; indCnt++) {
                int numDependents =
                    (int) Double.parseDouble(clean(header.get(idx++)));
                if (debug) {
                    System.err.println("num dependents:" + numDependents);
                }
                int dcnt = 0;
                while (dcnt < numDependents) {
                    if (debug) {
                        System.err.println("\tscale:" + header.get(idx));
                    }
                    List<String> tmp = tokenize(header.get(idx++));
                    dcnt += tmp.size();
                    scales.addAll(tmp);
                }

                dcnt = 0;
                while (dcnt++ < numDependents) {
                    if (debug) {
                        System.err.println("\tmissing:" + header.get(idx));
                    }
                    List<String> tmp = tokenize(header.get(idx++));
                    dcnt += tmp.size();
                    missings.addAll(tmp);
                }

                for (int i = 0; i < numDependents; i++) {
                    if (debug) {
                        System.err.println("\tdependent:" + header.get(idx));
                    }
                    vars.add(parseVar(clean(header.get(idx++))));
                }
            }

            int numComments =
                (int) Double.parseDouble(clean(header.get(idx++)));
            for (int i = 0; i < numComments; i++) {
                comments.append(header.get(idx++) + "\n");
            }

            numComments = (int) Double.parseDouble(clean(header.get(idx++)));
            for (int i = 0; i < numComments - 1; i++) {
                comments.append(header.get(idx++) + "\n");
            }

            int cnt = 0;
            for (int i = numDimensions; i < vars.size(); i++) {
                vars.get(i).scale   = scales.get(cnt);
                vars.get(i).missing = missings.get(cnt);
                cnt++;
            }
            cnt = 0;
            String varLine = header.get(idx++).replaceAll(" +\\(", "(");
            for (String tok : StringUtil.split(varLine, " ", true, true)) {
                Var v   = parseVar(tok);
                Var var = vars.get(cnt++);
            }
            String[] fields = new String[vars.size()];
            for (int i = 0; i < vars.size(); i++) {
                Var var = vars.get(i);
                if ((i == 0)
                        && ((var.id.indexOf("elapsed") >= 0)
                            || (var.label.toLowerCase().indexOf("elapsed")
                                >= 0))) {
                    dateIdx   = i;
                    doElapsed = true;
                    var.id    = "elapsed_time";
                    //              var.label = "Elapsed Time";
                } else if (var.id.equals("time")) {
                    //hours since 1999-09-12 18:00:00
                    String unit = StringUtil.findPattern(var.unit,
                                      "(.*) since ");
                    String dttm = StringUtil.findPattern(var.unit,
                                      "since (.*)");
                    if ((unit != null) && (dttm != null)) {
                        dateIdx  = i;
                        unit     = unit.trim();
                        dttm     = dttm.trim();
                        dateBase = sdf.parse(dttm).getTime();
                        if (unit.equals("hours")) {
                            dateUnit = DATEUNIT_HOURS;
                        } else if (unit.equals("days")) {
                            dateUnit = DATEUNIT_DAYS;
                        } else if (unit.equals("seconds")) {
                            dateUnit = DATEUNIT_SECONDS;
                        }
                    }
                }
                fields[i] = makeField(var.id,
                                      attrLabel(Utils.makeLabel(var.id)),
                                      attrType("double"), (var.unit != null)
                        ? attrUnit(var.unit)
                        : null, (var.missing != null)
                                ? attrMissing(Double.parseDouble(var.missing))
                                : null, (var.scale != null)
                                        ? " scale=" + var.scale + " "
                                        : null);
                if (debug) {
                    System.err.println("FIELD:" + fields[i]);
                }
            }
            putFields(fields);

            return visitInfo;

        }


        /**
         * _more_
         *
         * @param visitInfo _more_
         * @param record _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        public boolean processAfterReading(VisitInfo visitInfo, Record record)
                throws Exception {
            if ( !super.processAfterReading(visitInfo, record)) {
                return false;
            }

            if (dateIdx < 0) {
                return true;
            }
            TextRecord textRecord = (TextRecord) record;
            double     value      = textRecord.getValue(dateIdx + 1);
            if (doElapsed && (startDate != null)) {
                record.setRecordTime(startDate.getTime()
                                     + (long) value * 1000);
            } else {
                if (dateUnit == DATEUNIT_HOURS) {
                    value = value * 60 * 60;
                } else if (dateUnit == DATEUNIT_DAYS) {
                    value = value * 24 * 60 * 60;
                }
                record.setRecordTime(dateBase + ((long) value) * 1000);
            }

            return true;
        }
    }

}
