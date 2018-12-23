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

package org.ramadda.data.point.czo;


import org.ramadda.util.Utils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;

import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 * Class description
 *
 *
 * @version        $version$, Mon, Dec 2, '13
 * @author         Enter your name here...
 */
public class CzoHeader {

    /** _more_ */
    private Hashtable properties = new Hashtable();

    /** _more_ */
    private String name = "";

    /** _more_ */
    private List<String> description = new ArrayList<String>();

    /** _more_ */
    private List<String> vars = new ArrayList<String>();

    /** _more_ */
    private List<String> investigators = new ArrayList<String>();

    /** _more_ */
    private List<String> keywords = new ArrayList<String>();

    /** _more_ */
    private List<String> variableNames = new ArrayList<String>();

    /** _more_ */
    private List<String> citations = new ArrayList<String>();

    /** _more_ */
    private List<String> publications = new ArrayList<String>();

    /** _more_ */
    private List<String> lines;

    /**
     * _more_
     *
     * @param hdr _more_
     *
     * @throws Exception _more_
     */
    public CzoHeader(String hdr) throws Exception {
        processHeader(hdr);
    }

    /** _more_ */
    private int currentIdx;

    /**
     * _more_
     *
     * @param hdr _more_
     *
     * @throws Exception _more_
     */
    private void processHeader(String hdr) throws Exception {
        boolean inDoc    = false;
        boolean inHeader = false;
        lines = StringUtil.split(hdr, "\n");
        String state = "none";
        for (currentIdx = 0; currentIdx < lines.size(); currentIdx++) {
            String line = lines.get(currentIdx);
            if (line.equals("\\doc")) {
                inDoc = true;

                continue;
            }

            if (line.equals("\\header")) {
                inDoc    = false;
                inHeader = true;

                continue;
            }

            if (inDoc) {
                processDocLine(line);
            } else if (inHeader) {
                processHeaderLine(line);
            }
        }

    }


    /**
     * _more_
     *
     * @param line _more_
     *
     * @throws Exception _more_
     */
    private void processDocLine(String line) throws Exception {

        List<String> toks = StringUtil.splitUpTo(line, ".", 2);
        if (toks.size() != 2) {
            if ((line.trim().length() > 0) && !line.endsWith(".")) {
                System.err.println("Bad line:" + line);
            }

            return;
        }
        String property = toks.get(0);
        String value    = toks.get(1);
        if ( !Utils.stringDefined(value)) {
            return;
        }

        if (property.equals("DEFAULT_PARAMETER")) {
            List<String> subtoks   = StringUtil.splitUpTo(value, "=", 2);
            String       propName  = subtoks.get(0).trim();
            String       propValue = subtoks.get(1).trim();
            properties.put(propName, propValue);
            properties.put(propName.toLowerCase(), propValue);

            return;
        }

        if (property.equals("TITLE")) {
            name = value;
        } else if (property.equals("ABSTRACT")) {
            description.add(value);
        } else if (property.equals("KEYWORDS")) {
            keywords = StringUtil.split(value, ",", true, true);
        } else if (property.equals("VARIABLE NAMES")) {
            variableNames = StringUtil.split(value, ",", true, true);
        } else if (property.equals("INVESTIGATOR")) {
            investigators.add(value);
            readAhead(investigators);
        } else if (property.equals("COMMENTS")) {
            description.add(value);
            readAhead(description);
        } else if (property.equals("CITATION")) {
            citations.add(value);
            readAhead(citations);
        } else if (property.equals("PUBLICATIONS")) {
            publications.add(value);
            readAhead(publications);
        }
    }

    /**
     * _more_
     *
     * @param list _more_
     */
    private void readAhead(List<String> list) {
        while (currentIdx < lines.size()) {
            String extraLine = lines.get(++currentIdx);
            if (extraLine.trim().length() == 0) {
                break;
            }
            if (extraLine.matches(
                    "^(TITLE|ABSTRACT|INVESTIGATOR|KEYWORDS|VARIABLE NAMES|CITATION|PUBLICATIONS|COMMENTS)\\..*")) {
                currentIdx--;

                return;
            }
            list.add(extraLine);
        }
    }



    /**
     * _more_
     *
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void makeEntryXml(StringBuffer sb) throws Exception {

        String datafile = Misc.getProperty(properties, "datafile",
                                           (String) null);
        String extra = "";

        if (datafile != null) {
            extra = " file=\"" + datafile + "\" ";
        }
        sb.append(XmlUtil.openTag("entry",
                                  extra
                                  + XmlUtil.attrs("type", "type_point_czo",
                                      "name", name)));
        StringBuffer pointProps = new StringBuffer();
        StringBuffer fields     = new StringBuffer();
        for (int i = 0; i < vars.size(); i++) {
            String line = vars.get(i);
            //            label=acde(cm), value=Snow Depth, units=cm, siteCode=LowMetN_acde, SampleMedium=Snow
            line = line.replace(",", "\n");
            if (line.endsWith(".")) {
                line = line.substring(0, line.length() - 1);
            }
            Properties tmp = new Properties();
            tmp.load(new ByteArrayInputStream(line.getBytes()));
            Properties tmp2 = new Properties();
            //            tmp2.putAll(tmp);


            for (java.util.Enumeration keys = tmp.keys();
                    keys.hasMoreElements(); ) {
                String key   = (String) keys.nextElement();
                String value = (String) tmp.get(key);
                key = key.toLowerCase();
                tmp2.put(key, value);
            }

            tmp = tmp2;

            if (fields.length() > 0) {
                fields.append(",");
            }
            String fieldName = Misc.getProperty(tmp, "label",
                                   "var" + (i + 1));
            if (fieldName.equals("VariableName")) {
                fieldName = Misc.getProperty(tmp, "value", "param");
                String siteCode = Misc.getProperty(tmp, "sitecode",
                                      (String) null);
                if (siteCode != null) {
                    fieldName += "-" + siteCode;
                }
            }
            fieldName = fieldName.replace("\\(", "_");
            fieldName = fieldName.replace("\\)", "");
            fieldName = fieldName.replace(" ", "_");

            //            System.err.println("name:" + fieldName);
            fields.append(fieldName);
            fields.append("[");
            String format = Misc.getProperty(tmp, "format", (String) null);
            if (format != null) {
                fields.append(" type=\"date\" ");
                format = format.replace("\"", "");
                fields.append(" format=\"" + format + "\" ");
                String offset = Misc.getProperty(tmp, "utcoffset",
                                    (String) null);
                if (offset != null) {
                    fields.append(" utcoffset=\"" + offset + "\" ");
                }
            } else {
                String missing = getFieldProperty(tmp, "NoDataValue", null);
                if (missing != null) {
                    fields.append(" missing=\"" + missing + "\" ");
                }
                fields.append(" chartable=\"true\" searchable=\"true\"  ");
            }

            String units = (String) tmp.get("units");
            if (units != null) {
                fields.append(" units=\"" + units + "\" ");
            }

            fields.append("]");
        }

        int skip = new Integer(Misc.getProperty(properties,
                       "databeginsonrow", "0")).intValue() - 1;
        pointProps.append("\n\n");
        pointProps.append("skiplines=" + skip + "\n\n");
        pointProps.append("fields=" + fields);
        pointProps.append("\n\n");
        pointProps.append("position.required=false\n");


        IOUtil.writeFile("point.properties", pointProps.toString());



        sb.append(XmlUtil.tag("properties", "",
                              XmlUtil.getCdata(pointProps.toString())));
        sb.append(XmlUtil.tag("description", "",
                              XmlUtil.getCdata(StringUtil.join("\n",
                                  description))));

        addMetadata(sb, investigators, "project_person");
        addMetadata(sb, variableNames, "thredds.variable");
        addMetadata(sb, keywords, "content.keyword");
        addMetadata(sb, citations, "content.acknowledgement");
        addMetadata(sb, publications, "content.acknowledgement");
        sb.append(XmlUtil.closeTag("entry"));

    }


    /**
     * _more_
     *
     * @param fieldProperties _more_
     * @param propertyName _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private String getFieldProperty(Hashtable fieldProperties,
                                    String propertyName, String dflt) {
        String value = Misc.getProperty(fieldProperties, propertyName,
                                        (String) null);
        if (value == null) {
            value = Misc.getProperty(properties, propertyName, dflt);
        }
        if (value == null) {
            propertyName = propertyName.toLowerCase();
            value = Misc.getProperty(fieldProperties, propertyName,
                                     (String) null);
            if (value == null) {
                value = Misc.getProperty(properties, propertyName, dflt);
            }
        }

        return value;

    }

    /**
     * _more_
     *
     * @param xml _more_
     * @param values _more_
     * @param type _more_
     */
    private void addMetadata(StringBuffer xml, List<String> values,
                             String type) {
        for (String v : values) {
            v = Utils.removeNonAscii(v);
            xml.append(XmlUtil.tag("metadata", XmlUtil.attrs("type", type),
                                   XmlUtil.tag("attr",
                                       "encoded=\"false\" index=\"1\"",
                                       XmlUtil.getCdata(v))));
        }
    }



    /**
     * _more_
     *
     * @param line _more_
     *
     * @throws Exception _more_
     */
    private void processHeaderLine(String line) throws Exception {
        List<String> toks = StringUtil.splitUpTo(line, ".", 2);

        if (toks.size() > 0) {
            //check the first token
            if (toks.get(0).indexOf(" ") >= 0) {
                if (line.startsWith("COL")) {
                    toks = StringUtil.splitUpTo(line, ",", 2);
                } else {
                    System.err.println("Bad line:" + line);

                    return;
                }

            }
        }

        if (toks.size() != 2) {
            if ((line.trim().length() > 0) && !line.endsWith(".")) {
                System.err.println("Bad line:" + line);
            }

            return;
        }
        String property = toks.get(0);
        String value    = toks.get(1);
        if ( !Utils.stringDefined(value)) {
            return;
        }
        if (property.startsWith("COL")) {
            vars.add(value);
        } else {
            System.err.println("???" + property + "=" + value);
        }
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        StringBuffer xml = new StringBuffer("<entries>");
        for (String file : args) {
            String    contents  = IOUtil.readContents(file, CzoHeader.class);
            CzoHeader czoHeader = new CzoHeader(contents);
            czoHeader.makeEntryXml(xml);

        }
        xml.append("</entries>");
        System.out.println(xml);
    }


}
