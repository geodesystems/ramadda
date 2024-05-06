/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.census;


import org.json.*;


import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;


import org.ramadda.repository.RepositoryUtil;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.geo.GeoUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.Place;

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
@SuppressWarnings("unchecked")
public class AcsFile extends CsvFile {




    /** _more_ */
    private static Hashtable specialNames;

    /** _more_ */
    public byte[] bytes;

    /** _more_ */
    private List<String> labels;

    /** _more_ */
    private boolean includeSpecial = true;

    /**  */
    private String namePattern;

    /**
     * ctor
     *
     *
     * @throws IOException _more_
     */
    public AcsFile(IO.Path path) throws IOException {
        super(path);
    }

    /**
     * _more_
     *
     * @param labels _more_
     * @param includeSpecial _more_
     * @param pattern _more_
     *
     * @throws IOException _more_
     */
    public AcsFile(IO.Path path, List<String> labels,
                   boolean includeSpecial, String pattern)
            throws IOException {
        super(path);
        this.labels         = labels;
        this.includeSpecial = includeSpecial;
        if ((pattern != null) && pattern.equals("")) {
            pattern = null;
        }
        this.namePattern = pattern;

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
        for (int i=0;i<vars.size();i++) {
	    var = vars.get(i);
	    if(var.getId().equals(value)) {
		vars.remove(i);
		return var;
	    }
	}

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
                boolean[]     name       = new boolean[headerJson.length()];
                boolean[]     special    = new boolean[headerJson.length()];
                boolean[]     isState    = new boolean[headerJson.length()];		
                boolean[]     skip       = new boolean[headerJson.length()];
                String[]      rowValues  = new String[headerJson.length()];
                //            System.err.println(" labels:" + labels);
                long t4 = System.currentTimeMillis();
                for (int i = 0; i < headerJson.length(); i++) {
                    String         value = headerJson.getString(i);
                    CensusVariable var   = getVariable(value);
                    String         type  = RecordField.TYPE_DOUBLE;
                    numeric[i] = true;
                    depends[i] = CensusVariable.NULL_INDEX;
                    name[i]    = false;
                    special[i] = isNameSpecial(value);
		    isState[i] = value.equals("state");
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
                    name[i] = value.equals("NAME");
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
			String alias = var.getAlias();
			if(alias!=null) label = alias;
                        if (depends[i] != CensusVariable.NULL_INDEX) {
                            label = "% " + label;
			    value+="_percentage";
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
		    if(value.equals("state")) {
			header.append(", ");
			header.append(makeField("state_name", attrType("string"),
						attrLabel("State Name"), ""));
		    }
			
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



		//		System.err.println("header:" + header);
                writer.println(header);
                long   t5  = System.currentTimeMillis();
                String lat = "NaN";
                String lon = "NaN";
                long   tt1 = System.currentTimeMillis();
                for (int rowIdx = 1; rowIdx < obj.length(); rowIdx++) {
                    JSONArray     row         = obj.getJSONArray(rowIdx);
                    int           colIdx      = 0;
                    int           len         = row.length();
                    List<String>  specialOnes = new ArrayList<String>();

                    boolean       rowOk       = true;
                    StringBuilder rowBuff     = new StringBuilder();
                    for (int allColIdx = 0; allColIdx < len; allColIdx++) {
                        String value = row.optString(allColIdx, null);

                        if (special[allColIdx]) {
                            specialOnes.add(value);
                        }

                        if (name[allColIdx] && (namePattern != null)) {
                            if ( !value.matches(namePattern)) {
                                rowOk = false;
                                continue;
                            }
                        }

                        if (value == null) {
                            if (numeric[allColIdx]) {
                                value = "NaN";
                            } else {
                                value = "";
                            }
                        } else if (numeric[allColIdx]) {
                            double tmp = Double.parseDouble(value);
                            //A hack to catch -666,000,000 values I've seen
                            if (tmp < -10000) {
                                value = "NaN";
                            }
                        }

                        if (depends[allColIdx] != CensusVariable.NULL_INDEX) {
                            double v1 = Double.parseDouble(value);
			    int index = depends[allColIdx];
                            double v2 = Double.parseDouble(rowValues[index>=0?index:allColIdx+index]);
			    //			    if(rowIdx<100)System.err.println("col:" + colIdx +" on:" + depends[allColIdx] + " v1:" + v1 +" v2:" + v2);
                            value = "" + ((int) (1000 * v1 / v2)) / 10.0;
                        }

                        rowValues[allColIdx] = value;

                        if (skip[allColIdx]) {
                            continue;
                        }

                        if (colIdx > 0) {
                            rowBuff.append(",");
                        }
                        boolean quote = value.indexOf(",") >= 0;
                        if (quote) {
                            rowBuff.append("\"");
                        }
                        rowBuff.append(value);
                        if (quote) {
                            rowBuff.append("\"");
                        }
                        if (isState[allColIdx]) {
			    String stateName = GeoUtils.getStatesMap().get(value);
			    if(stateName==null) stateName = "";
                            rowBuff.append(",");
			    rowBuff.append(stateName);
                        }

                        colIdx++;
                    }
                    if ( !rowOk) {
                        continue;
                    }
		    //		    if(rowIdx<10)System.err.println("row:" + rowBuff);
                    writer.print(rowBuff.toString());

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
                exc.printStackTrace();

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
