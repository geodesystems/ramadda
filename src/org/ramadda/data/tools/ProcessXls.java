/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.tools;


import org.apache.poi.hssf.usermodel.*;

import org.ramadda.util.Utils;
import org.ramadda.util.geo.GeoUtils;

import ucar.unidata.util.*;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.*;


/**
 * Class description
 *
 *
 * @version        $version$, Fri, Aug 23, '13
 * @author         Enter your name here...
 */
public class ProcessXls {

    /** _more_ */
    int nextId = 0;


    /** _more_ */
    StringBuffer sb = new StringBuffer();

    /** _more_ */
    StringBuffer end = new StringBuffer();

    /**
     * _more_
     */
    public ProcessXls() {}



    /**
     * _more_
     *
     * @param row _more_
     * @param idx _more_
     *
     * @return _more_
     */
    public static String getCell(HSSFRow row, short idx) {
        HSSFCell cell = row.getCell(idx);
        if (cell == null) {
            return "";
        }

        return cell.toString().trim();
    }


    /**
     * _more_
     *
     * @param filename _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String makeEntries(String filename) throws Exception {
        sb.append("<entries>");
        /*

        sb.append(XmlUtil.tag("entry", XmlUtil.attrs(new String[]{
                        "type",
                        "group",
                        "name",
                        "Projects",
                        "id","project",
                    }),""));
        */

        InputStream  myxls = IOUtil.getInputStream(filename,
                                 ProcessXls.class);
        HSSFWorkbook wb       = new HSSFWorkbook(myxls);
        HSSFSheet    sheet    = wb.getSheetAt(0);
        int          skipRows = 2;
        for (int rowIdx = sheet.getFirstRowNum();
                rowIdx <= sheet.getLastRowNum(); rowIdx++) {
            if (skipRows-- > 0) {
                //                System.err.println("skipping");
                continue;
            }
            HSSFRow row = sheet.getRow(rowIdx);
            if ((row == null) || (rowIdx == 0)) {
                continue;
            }
            processRow(row);
        }

        sb.append(end);
        sb.append("</entries>");

        return sb.toString();
    }



    /**
     * _more_
     *
     * @return _more_
     */
    private String getId() {
        return "entry" + (nextId++);
    }

    /**
     * _more_
     *
     * @param row _more_
     *
     * @throws Exception _more_
     */
    private void processRow(HSSFRow row) throws Exception {
        StringBuffer inner   = new StringBuffer();
        String       entryId = getId();
        int          numCols = 5;

        String[]     cols    = new String[numCols];
        for (short col = 0; col < numCols; col++) {
            cols[col] = getCell(row, col);
        }


        String name;
        double lat = 0;
        double lon = 0;





        name = cols[2];
        lat  = getLatLon(cols[3]);
        lon  = getLatLon(cols[4]);
        String shortName = "CRRN-" + ((int) (Double.parseDouble(cols[0])));

        inner.append(XmlUtil.tag("network", "", XmlUtil.getCdata("CCRN")));
        inner.append(XmlUtil.tag("status", "", XmlUtil.getCdata("active")));
        inner.append(XmlUtil.tag("short_name", "",
                                 XmlUtil.getCdata(shortName)));
        inner.append(XmlUtil.tag("site_type", "",
                                 XmlUtil.getCdata("multipurpose")));
        sb.append(XmlUtil.tag("entry", XmlUtil.attrs(new String[] {
            "type", "project_site", "name", name,
            //                        "url",
            //                        url,
            "id", entryId,
            //                        "parent",  orgParent,
            "latitude", "" + lat, "longitude", "" + lon,
        }), inner.toString()));

    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private double getLatLon(String s) {
        s = s.replaceAll("'", ":");
        s = s.replaceAll("\"", "");
        s = s.replaceAll("[^0-9:\\.NSEWnsew]", ":");

        //        System.out.println("s:" + s +" decoded:" + Utils.decodeLatLon(s));
        return GeoUtils.decodeLatLon(s);
    }


    /**
     * _more_
     *
     * @param inner _more_
     * @param str _more_
     *
     * @throws Exception _more_
     */
    private void setSponsors(StringBuffer inner, String str)
            throws Exception {
        str = str.replaceAll("/", ",").replaceAll("\n", ",");
        List<String> sponsors = StringUtil.split(str, ",", true, true);
        for (String s : sponsors) {
            s = getString(s).trim();
            if (s.length() == 0) {
                continue;
            }
            //            System.out.println(s);
            inner.append(XmlUtil.tag("metadata",
                                     XmlUtil.attrs("type", "project_funding",
                                         "attr1", s)));
        }
    }


    /**
     * _more_
     *
     * @param attrs _more_
     * @param startDate _more_
     * @param endDate _more_
     *
     * @return _more_
     */
    private String setDate(StringBuffer attrs, String startDate,
                           String endDate) {
        String status = "active";
        startDate = startDate.replace(".0", "");
        startDate = startDate.replaceAll("s$", "");
        startDate = startDate.replace("summer", "").trim();
        startDate = startDate.replace("\\?", "");
        if (startDate.equals("0")) {
            startDate = "";
        }
        if (startDate.length() != 4) {
            String yyyy = StringUtil.findPattern(startDate,
                              "-(\\d\\d\\d\\d).*");
            if (yyyy == null) {
                yyyy = StringUtil.findPattern(startDate, "/(\\d\\d\\d\\d).*");
            }
            if (yyyy == null) {
                yyyy = StringUtil.findPattern(startDate, "^(20\\d\\d).*");
            }
            if (yyyy == null) {
                yyyy = StringUtil.findPattern(startDate, ".*(20\\d\\d)$");
            }
            if (yyyy != null) {
                startDate = yyyy;
            }
        }
        //            if(!seen.contains(startDate)) {
        //                seen.add(startDate);
        //                System.out.println("start:" + startDate);
        //            }

        if (startDate.length() == 4) {
            attrs.append(XmlUtil.attr("fromdate", startDate + "-01-01"));
        } else {
            //                System.out.println("unknown start date:" + startDate);
        }
        if (endDate.length() > 0) {
            if (endDate.equals("current")
                    || (endDate.indexOf("active") >= 0)) {
                status = "active";
            } else if (endDate.indexOf("unknown") >= 0) {
                status = "unknown";
            } else if (endDate.indexOf("none") >= 0) {
                status = "unknown";
            } else if (endDate.indexOf("in development") >= 0) {
                status = "in_development";
            } else if (endDate.indexOf("inactive") >= 0) {
                status = "inactive";
            } else if (endDate.indexOf("potential") >= 0) {
                status = "potential";
            } else if (endDate.indexOf("nominated") >= 0) {
                status = "nominated";
            } else if (endDate.indexOf("funded") >= 0) {
                status = "funded";
            } else {
                String yyyy = StringUtil.findPattern(endDate,
                                  ".*(\\d\\d\\d\\d).*");
                if (yyyy != null) {
                    attrs.append(XmlUtil.attr("todate", yyyy + "-12-31"));
                    status = "inactive";
                } else {
                    status = "inactive";
                    //                    System.out.println ("end date:" + endDate);
                }
            }
        }

        return status;
    }





    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public static String chop(Object o) {
        String s = o.toString();
        if (s.length() > 15) {
            return s.substring(0, 14).trim() + "...";
        }

        return s.trim();
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String getString(String s) throws Exception {
        String newString = new String(s.getBytes("utf-8"));
        newString = newString.replaceAll("[^ -~]", "");

        //        if(newString.trim().length()>0) System.out.println(newString);
        return newString;
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        ProcessXls processor = new ProcessXls();
        String     xml       = processor.makeEntries(args[0]);
        System.out.println(xml);
    }



}
