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


import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;


import org.ramadda.repository.RepositoryUtil;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Place;
import org.ramadda.util.Utils;
import org.ramadda.util.text.*;

import org.w3c.dom.*;

import org.w3c.dom.Element;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import java.util.Properties;
import java.util.TimeZone;


/**
 */
public class AcsFile extends CsvFile {

    /** _more_ */
    private static Hashtable specialNames;

    /** _more_ */
    public byte[] bytes;

    /** _more_ */
    private List<String> labels;

    /** _more_ */
    private boolean includeSpecial = true;

    /**
     * ctor
     *
     * @param filename _more_
     *
     * @throws IOException _more_
     */
    public AcsFile(String filename) throws IOException {
        super(filename);
    }

    /**
     * _more_
     *
     * @param filename _more_
     * @param labels _more_
     * @param includeSpecial _more_
     *
     * @throws IOException _more_
     */
    public AcsFile(String filename, List<String> labels,
                   boolean includeSpecial)
            throws IOException {
        super(filename);
        this.labels         = labels;
        this.includeSpecial = includeSpecial;
    }




    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean isNameSpecial(String name) throws Exception {
        if (specialNames == null) {
            Hashtable p = new Hashtable();
            for (String line :
                    StringUtil.split(
                        IOUtil.readContents(
                            "/org/ramadda/plugins/census/resources/in.txt",
                            getClass()), "\n", true, true)) {
                if (line.startsWith("#")) {
                    continue;
                }
                List<String> toks = StringUtil.splitUpTo(line, "=", 2);
                p.put(toks.get(0), "");
            }
            specialNames = p;
        }

        return specialNames.get(name) != null;
    }

    /** _more_ */
    private List<CensusVariable> vars;

    /** _more_ */
    private Hashtable<String, CensusVariable> varMap;

    /**
     * _more_
     *
     * @param vars _more_
     */
    public void setVariables(List<CensusVariable> vars) {
        this.vars   = vars;
        this.varMap = new Hashtable<String, CensusVariable>();
        for (CensusVariable var : vars) {
            varMap.put(var.getId(), var);
        }
    }


    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private CensusVariable getVariable(String value) throws Exception {
        CensusVariable var = null;
        if (varMap != null) {
            var = varMap.get(value);
        }
        if (var == null) {
            var = CensusVariable.getVariable(value);
        }

        return var;
    }

    /**
     * Gets called when first reading the file. Parses the header
     *
     * @param visitInfo visit info
     *
     * @return the visit info
     *
     *
     * @throws Exception _more_
     */
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {

        if (bytes == null) {
            ByteArrayOutputStream bos  = new ByteArrayOutputStream();
            BufferedOutputStream  bbos = new BufferedOutputStream(bos);
            long                  t1   = System.currentTimeMillis();
            String json = new String(
                              IOUtil.readBytes(
                                  visitInfo.getRecordIO().getInputStream()));

            long t2 = System.currentTimeMillis();
            //            System.out.println("json:" + json);

            try {
                PrintWriter   writer     = new PrintWriter(bbos);
                JSONArray     obj = new JSONArray(new JSONTokener(json));
                long          t3         = System.currentTimeMillis();
                JSONArray     headerJson = obj.getJSONArray(0);
                StringBuilder header     = new StringBuilder("#fields=");
                boolean[]     numeric    = new boolean[headerJson.length()];
                int[]         depends    = new int[headerJson.length()];
                boolean[]     special    = new boolean[headerJson.length()];
                boolean[]     skip       = new boolean[headerJson.length()];
                String[]      rowValues  = new String[headerJson.length()];
                //            System.err.println(" labels:" + labels);
                long t4 = System.currentTimeMillis();
                for (int i = 0; i < headerJson.length(); i++) {
                    String         value = headerJson.getString(i);
                    CensusVariable var   = getVariable(value);
                    String         type  = RecordField.TYPE_DOUBLE;
                    numeric[i] = true;
                    depends[i] = -1;
                    special[i] = isNameSpecial(value);
                    //                System.err.println("n:" + value +" special:"+ isSpecial);

                    if (special[i]) {
                        if ( !includeSpecial) {
                            skip[i] = true;
                        }
                    }

                    if (var != null) {
                        skip[i] = var.getSkip();
                    }
                    if (skip[i]) {
                        continue;
                    }

                    if (i > 0) {
                        header.append(", ");
                    }
                    skip[i] = false;
                    if (special[i] || value.equals("NAME")) {
                        type       = "string";
                        numeric[i] = false;
                    }
                    String label = value;

                    if (var != null) {
                        depends[i] = var.getDependsIndex();
                        label      = var.getLabel();
                        skip[i]    = var.getSkip();
                        if ((label.length() < 20)
                                && (var.getConcept().length() > 0)) {
                            label = var.getConcept() + " - " + label;
                        }
                        if (depends[i] >= 0) {
                            label = "% " + label;
                        }
                    }
                    if ((labels != null) && (i < labels.size())) {
                        String tmp = labels.get(i).trim();
                        if ((tmp.length() > 0) && !tmp.equals("default")) {
                            label = tmp;
                        }
                    }
                    label = cleanUpLabel(label);


                    header.append(makeField(value, attrType(type),
                                            attrLabel(label), numeric[i]
                            ? attrChartable()
                            : ""));
                }
                header.append(", ");
                header.append(makeField("latitude",
                                        attr(RecordField.PROP_ISLATITUDE,
                                             "true"), attrLabel("Latitude")));
                header.append(", ");
                header.append(
                    makeField(
                        "longitude",
                        attr(RecordField.PROP_ISLONGITUDE, "true"),
                        attrLabel("Longitude")));


                //            System.err.println(header);
                writer.println(header);
                long   t5  = System.currentTimeMillis();
                String lat = "NaN";
                String lon = "NaN";
                long   tt1 = System.currentTimeMillis();
                for (int i = 1; i < obj.length(); i++) {
                    JSONArray    row         = obj.getJSONArray(i);
                    int          colIdx      = 0;
                    int          len         = row.length();
                    List<String> specialOnes = new ArrayList<String>();

                    for (int allColIdx = 0; allColIdx < len; allColIdx++) {
                        String value = row.optString(allColIdx, null);
                        if (special[allColIdx]) {
                            specialOnes.add(value);
                        }

                        if (value == null) {
                            if (numeric[allColIdx]) {
                                value = "NaN";
                            } else {
                                value = "";
                            }
                        }

                        if (depends[allColIdx] >= 0) {
                            double v1 = Double.parseDouble(value);
                            double v2 = Double.parseDouble(
                                            rowValues[depends[allColIdx]]);
                            //System.err.println("col:" + colIdx +" on:" + depends[allColIdx] + " v1:" + v1 +" v2:" + v2);
                            value = "" + ((int) (1000 * v1 / v2)) / 10.0;
                        }

                        rowValues[allColIdx] = value;

                        if (skip[allColIdx]) {
                            continue;
                        }

                        if (colIdx > 0) {
                            writer.print(",");
                        }
                        boolean quote = value.indexOf(",") >= 0;
                        if (quote) {
                            writer.print("\"");
                        }
                        writer.print(value);
                        if (quote) {
                            writer.print("\"");
                        }
                        colIdx++;
                    }

                    Place place = null;
                    //TODO: maybe a bit inefficient
                    while ((specialOnes.size() > 0) && (place == null)) {
                        String codeVal = StringUtil.join("", specialOnes);
                        place = Place.getPlace(codeVal);
                        if (place == null) {}
                        if (place != null) {
                            break;
                        }
                        specialOnes.remove(specialOnes.size() - 1);
                    }
                    if (place != null) {
                        lat = Double.toString(place.getLatitude());
                        lon = Double.toString(place.getLongitude());
                        //                    System.err.println(" Place:" + place +" " + lat +" " + lon);
                    } else {
                        lat = "NaN";
                        lon = "NaN";
                        System.err.println(" no place " + row.toString());
                    }

                    writer.print(",");
                    writer.print(lat);
                    writer.print(",");
                    writer.print(lon);
                    writer.print("\n");
                }
                writer.flush();
                writer.close();
                bytes = bos.toByteArray();
                long tt2 = System.currentTimeMillis();
                //            Utils.printTimes("ACS File:", tt1,tt2);
            } catch (Exception exc) {
                System.err.println("Error reading census data:" + json);

                throw exc;
            }
        }

        RecordIO recordIO = new RecordIO(new ByteArrayInputStream(bytes));
        visitInfo.setRecordIO(recordIO);
        putProperty(PROP_HEADER_STANDARD, "true");
        //        putProperty("picky", "false");
        super.prepareToVisit(visitInfo);

        return visitInfo;


    }

    /**
     * _more_
     */
    @Override
    public void doQuickVisit() {
        //noop

    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private String cleanUpLabel(String s) {
        s = s.replaceAll(":", " ").replaceAll("--", " ");
        s = s.replaceAll(" + ", " ").trim();

        return s;
    }


    /**
     * _more_
     *
     * @param record _more_
     * @param field _more_
     * @param tok _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public double parseValue(TextRecord record, RecordField field, String tok)
            throws Exception {
        //        tok = tok.replaceAll(",", "");
        //TODO: what does D mean?
        //        if(tok.equals("(D)")|| tok.equals("(X)") || tok.equals("(NA)")) return Double.NaN;

        return Double.parseDouble(tok);
    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        PointFile.test(args, AcsFile.class);
    }


}
