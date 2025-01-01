/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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
import org.ramadda.util.IO;
import org.ramadda.util.Utils;


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


public class NasaAmesTypeHandler extends PointTypeHandler {
    private SimpleDateFormat dateSDF;
    private static int IDX = PointTypeHandler.IDX_LAST + 1;
    public static final int IDX_DIMENSIONS = IDX++;
    public static final int IDX_ORIGINATOR_NAME = IDX++;
    public static final int IDX_AFFILIATION = IDX++;
    public static final int IDX_INSTRUMENT = IDX++;
    public static final int IDX_CAMPAIGN = IDX++;
    public static final int IDX_COMMENTS = IDX++;

    public NasaAmesTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        return new NasaAmesRecordFile(new IO.Path(getPathForEntry(request, entry,true)));
    } 


    @Override
    protected void handleHarvestedMetadata(Request request,RecordEntry recordEntry,
                                           PointMetadataHarvester metadata)
            throws Exception {
        super.handleHarvestedMetadata(request,recordEntry, metadata);
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

    public static class NasaAmesRecordFile extends CsvFile {
        String name;
        int numDimensions;
        String affiliation;
        String instrument;
        String campaign;
        StringBuilder comments = new StringBuilder();
        Date startDate;

        private SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


        public static final int DATEUNIT_HOURS = 1;
        public static final int DATEUNIT_DAYS = 2;
        public static final int DATEUNIT_SECONDS = 3;
        private int dateIdx = -1;
        private boolean doElapsed = false;
        private int dateUnit;
        private long dateBase;

        public NasaAmesRecordFile(IO.Path path) throws IOException {
            super(path);
        }

        private static class Var {
            String id;
            String label;
            String unit;
            String scale;
            String missing;
            public Var(String id, String label, String unit) {
                this.id    = id;
                this.label = label;
                this.unit  = unit;
            }
            public Var(String id, String label, String unit, String scale,
                       String missing) {
                this(id, label, unit);
                this.scale   = scale;
                this.missing = missing;
            }
            public String toString() {
                return "id:" + id + " label:" + label + " unit:" + unit;
            }

        }

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
        private String clean(String s) {
            int idx = s.indexOf(";");
            if (idx >= 0) {
                s = s.substring(0, idx);
            }

            return s.trim();
        }

        public List<String> tokenize(String s) {
            s = clean(s);

            return StringUtil.split(s, (s.indexOf(",") >= 0)
                                       ? ","
                                       : " ", true, true);
        }

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

        public boolean processAfterReading(VisitInfo visitInfo,
                                           BaseRecord record)
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
