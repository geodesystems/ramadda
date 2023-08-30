/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point.ocean;


import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;

import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import ucar.unidata.util.StringUtil;

import java.io.*;

import java.util.Date;
import java.util.List;


/**
 * A reader for the SeaBird CNV file format
 *
 * Run this with:
 * java org.ramadda.data.point.ocean.CnvPointFile <filename>
 *
 * The entry type is defined in the point plugin:
 *  org/ramadda/geodata/point/ocean/oceantypes.xml
 */
public class CnvPointFile extends CsvFile {

    /**
     * ctor
     *
     * @throws IOException On badneess
     */
    public CnvPointFile(IO.Path path) throws IOException {
        super(path);
    }


    /**
     * Used by CsvPointFile to know when to stop reading the header
     *
     * @return header delimiter
     */
    @Override
    public String getHeaderDelimiter() {
        return "*END*";
    }

    /**
     * This is the data column delimiter
     *
     * @return column delimiter
     */
    @Override
    public String getDelimiter() {
        return DELIMITER_SPACES;
    }


    /**
     * This gets called before the file is read. It pulls out the fields and
     * other metadata from the header
     *
     * @param visitInfo Contains all info about this visit
     *
     * @return The incoming visitInfo
     *
     * @throws Exception On badness
     */
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        //Have the parent class read the header lines
        super.prepareToVisit(visitInfo);

        //If the entry type in the plugin had attributes then we make a filemetadata array here
        //Pull out the appropriate metadata and set the array values
        //Then at the end we call setFileMetadata(fileMetadata);
        //Object[]fileMetadata = new String[]{"","","",""};
        //e.g.: fileMetadata[0] = platform;

        //Pull out metadata from the header
        List<String> headerLines = getHeaderLines();
        double       lat         = 0;
        double       lon         = 0;
        String       time        = null;

        //* NMEA Latitude = 25 27.85 S
        //* NMEA Longitude = 044 15.34 E
        for (String line : headerLines) {
            if (line.indexOf("NMEA Latitude") >= 0) {
                List<String> toks = StringUtil.splitUpTo(line, "=", 2);
                lat = decode(toks.get(1));
            } else if (line.indexOf("NMEA Longitude") >= 0) {
                List<String> toks = StringUtil.splitUpTo(line, "=", 2);
                lon = decode(toks.get(1));
            } else if (line.indexOf("NMEA UTC") >= 0) {
                List<String> toks = StringUtil.splitUpTo(line, "=", 2);
                //Aug 24 2011  07:55:20
                if ( !toks.get(1).equals("none")) {
                    Date date = makeDateFormat("MMM dd yyyy HH:mm:ss").parse(
                                    toks.get(1));
                    time = makeDateFormat(DFLT_DATE_FORMAT).format(date);
                    //                    System.err.println("time:" + time);
                }
            }
        }

        //Make the fields string
        StringBuffer fields = new StringBuffer();
        fields.append(makeField(FIELD_LATITUDE, attrValue("" + lat)));
        fields.append(",");
        fields.append(makeField(FIELD_LONGITUDE, attrValue("" + lon)));

        if (time != null) {
            fields.append(",");
            fields.append(makeField(FIELD_TIME, attrValue(time),
                                    attrType(RecordField.TYPE_DATE)));
        }


        StringBuffer comments = new StringBuffer();
        for (String line : headerLines) {
            //# name 0 = scan: Scan Count
            if (line.startsWith("*")) {
                //Strip off *
                while (line.startsWith("*")) {
                    line = line.substring(1).trim();
                }
                comments.append(line);
                comments.append("<br>\n");
            }
            if (line.startsWith("# name ")) {
                List<String> toks  = StringUtil.splitUpTo(line, "=", 2);
                List<String> tuple = StringUtil.splitUpTo(toks.get(1), ":",
                                         2);
                fields.append(",");

                String name = tuple.get(0);
                String desc = (tuple.size() > 1)
                              ? tuple.get(1)
                              : name;
                toks = StringUtil.splitUpTo(desc, "[", 2);

                String unit = "";
                if (toks.size() > 1) {
                    desc = toks.get(0).trim();
                    unit = attrUnit(toks.get(1).replace("]", "").trim());
                    unit = unit.replace(",", "-");
                }
                desc = desc.replace(",", "-");
		String id = Utils.removeNonAscii(Utils.makeID(name));
                fields.append(makeField(id, unit, attr("label", desc),
                                        attrChartable(), attrSearchable()));
            }
        }

        //        System.err.println (fields);


        //Clean up non ascii
        String entryDesc = comments.toString();
        entryDesc = entryDesc.replaceAll("[^\n\\x20-\\x7E]+", " ");
        //This gets used by ramadda when creating an entry
        setDescriptionFromFile(entryDesc);

        //Store the fields
        putProperty(PROP_FIELDS, fields.toString());

        return visitInfo;
    }


    /**
     * utility decode the lat/lon
     *
     * @param lls latlon string
     *
     * @return decimal degrees
     */
    private double decode(String lls) {
	lls = lls.trim();
        lls = lls.replace(" ", ":");
        lls = lls.replace(":S", "S");
        lls = lls.replace(":N", "N");
        lls = lls.replace(":E", "E");
        lls = lls.replace(":W", "W");

        return Utils.decodeLatLon(lls);
    }


    /**
     * to run do:
     *     java org.ramadda.data.point.ocean.CnvPointFile <files>
     *
     * @param args cnv files
     */
    public static void main(String[] args) {
        PointFile.test(args, CnvPointFile.class);
    }

}
