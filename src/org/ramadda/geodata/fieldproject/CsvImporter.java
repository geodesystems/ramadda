/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.fieldproject;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;

import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import java.util.List;


/**
 */
@SuppressWarnings("unchecked")
public class CsvImporter extends ImportHandler {


    /** _more_ */
    public static final String TYPE_CSV = "CSV";

    /** _more_ */
    public static final String ARG_CSV_TYPE = "csv.type";

    /**
     * ctor
     *
     * @param repository _more_
     */
    public CsvImporter(Repository repository) {
        super(repository);
    }


    /**
     * _more_
     *
     * @param importTypes _more_
     * @param formBuffer _more_
     */
    @Override
    public void addImportTypes(List<TwoFacedObject> importTypes,
                               Appendable formBuffer) {
        super.addImportTypes(importTypes, formBuffer);
        importTypes.add(new TwoFacedObject("Csv Site Import", TYPE_CSV));
        //        formBuffer.append(HtmlUtils.formEntry(msgLabel("CSV Type"), HtmlUtils.input(ARG_CSV_TYPE,"")));
    }


    /**
     *
     * @param request _more_
     * @param parent _more_
     * @param fileName _more_
     * @param stream _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public InputStream getStream(Request request, Entry parent,
                                 String fileName, InputStream stream)
            throws Exception {
        if ( !request.getString(ARG_IMPORT_TYPE, "").equals(TYPE_CSV)) {
            return null;
        }

        StringBuffer sb = new StringBuffer("<entries>\n");
        String csv = new String(
                         IOUtil.readBytes(
                             getStorageManager().getFileInputStream(
                                 fileName)));

        //Check for CZO format
        if (csv.startsWith("SiteCode,SiteName,")) {
            processCZO(sb, csv);
        } else {
            processDefault(sb, csv);
        }

        sb.append("</entries>");

        return new ByteArrayInputStream(sb.toString().getBytes());
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param csv _more_
     *
     * @throws Exception _more_
     */
    private void processCZO(StringBuffer sb, String csv) throws Exception {
        String   entryType = "project_site";
        int[]    indices   = {
            0, 4, 6, 7, 8, 9, 10, 11, 12
        };
        String[] columns   = {
            "short_name", "latlong_datum", "vertical_datum", "local_x",
            "local_y", "local_projection", "position_accuracy", "state",
            "county"
        };
        int cnt = 0;
        for (String line : StringUtil.split(csv, "\n", true, true)) {
            cnt++;
            if (cnt == 1) {
                continue;
            }
            List<String> toks = StringUtil.split(line, ",");
            //SiteCode,SiteName,Latitude,Longitude,LatLongDatumID,Elevation_m,VerticalDatum,LocalX,LocalY,LocalProjectionID,PosAccuracy_m,State,County,Comments
            String       name     = getValue(1, toks);
            String       desc     = getValue(13, toks);
            double       lat      = Double.parseDouble(getValue(2, toks));
            double       lon      = Double.parseDouble(getValue(3, toks));
            double       elev     = Double.parseDouble(getValue(5, toks));

            StringBuffer innerXml = new StringBuffer();
            for (int colCnt = 0; colCnt < columns.length; colCnt++) {
                String col      = columns[colCnt];
                String colValue = getValue(indices[colCnt], toks);
                if (Utils.stringDefined(colValue)) {
                    innerXml.append(XmlUtil.tag(col, "", colValue));
                }
            }
            String attrs = XmlUtil.attrs(new String[] {
                ATTR_TYPE, entryType, ATTR_LATITUDE, "" + lat, ATTR_LONGITUDE,
                "" + lon, ATTR_ALTITUDE, "" + elev, ATTR_NAME, name,
                ATTR_DESCRIPTION, desc
            });
            sb.append(XmlUtil.tag("entry", attrs, innerXml.toString()));
        }


    }



    /**
     * _more_
     *
     * @param sb _more_
     * @param csv _more_
     *
     * @throws Exception _more_
     */
    private void processDefault(StringBuffer sb, String csv)
            throws Exception {

        int    IDX_LAT         = 0;
        int    IDX_LON         = 1;
        int    IDX_NAME        = 2;
        int    IDX_DESCRIPTION = 3;

        String entryType       = "project_site";
        List<String> columns = (List<String>) Misc.toList(new String[] {
                                   "site_type",
                                   "status", "short_name",
        //            "location",
        "network" });
        for (String line : StringUtil.split(csv, "\n", true, true)) {
            if (line.startsWith("#")) {
                line = line.substring(1);
                List<String> toks = StringUtil.splitUpTo(line, "=", 2);
                if ((toks.size() == 2) && toks.get(0).equals("fields")) {
                    columns = StringUtil.split(toks.get(1).trim(), ",");
                }

                continue;
            }
            List<String> toks     = StringUtil.split(line, ",");
            double       lat = Double.parseDouble(getValue(IDX_LAT, toks));
            double       lon = Double.parseDouble(getValue(IDX_LON, toks));
            String       name     = getValue(IDX_NAME, toks);
            String       desc     = getValue(IDX_DESCRIPTION, toks);
            StringBuffer innerXml = new StringBuffer();
            int          colCnt   = 0;
            for (String col : columns) {
                String colValue = getValue(IDX_DESCRIPTION + colCnt + 1,
                                           toks);
                if (Utils.stringDefined(colValue)) {
                    innerXml.append(XmlUtil.tag(col, "", colValue));
                }
                colCnt++;
            }
            String attrs = XmlUtil.attrs(new String[] {
                ATTR_TYPE, entryType, ATTR_LATITUDE, "" + lat, ATTR_LONGITUDE,
                "" + lon, ATTR_NAME, name, ATTR_DESCRIPTION, desc
            });
            sb.append(XmlUtil.tag("entry", attrs, innerXml.toString()));
        }

    }


    /**
     * _more_
     *
     * @param idx _more_
     * @param toks _more_
     *
     * @return _more_
     */
    private String getValue(int idx, List<String> toks) {
        if (idx < toks.size()) {
            return toks.get(idx);
        }

        return "";
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param repository _more_
     * @param uploadedFile _more_
     * @param parentEntry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleRequest(Request request, Repository repository,
                                String uploadedFile, Entry parentEntry)
            throws Exception {
        if (true) {
            return null;
        }
        List<Entry> entries = new ArrayList<Entry>();
        String csv = new String(
                         IOUtil.readBytes(
                             getStorageManager().getFileInputStream(
                                 uploadedFile)));
        processCsv(request, parentEntry, csv, entries);

        StringBuffer sb = new StringBuffer();
        for (Entry entry : entries) {
            entry.setUser(request.getUser());
        }
        getEntryManager().addNewEntries(request, entries);
        sb.append(msgHeader("Imported entries"));
        sb.append("<ul>");
        for (Entry entry : entries) {
            sb.append("<li> ");
            sb.append(getPageHandler().getBreadCrumbs(request, entry,
                    parentEntry));
        }

        return getEntryManager().addEntryHeader(request, parentEntry,
                new Result("", sb));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param parentEntry _more_
     * @param csv _more_
     * @param entries _more_
     *
     * @throws Exception _more_
     */
    private void processCsv(Request request, Entry parentEntry, String csv,
                            List<Entry> entries)
            throws Exception {}

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        CsvImporter importer = new CsvImporter(null);
        for (String file : args) {
            List<File>   files = new ArrayList<File>();
            StringBuffer sb    = new StringBuffer();
            //            importer.processXml(null, "parent", IOUtil.readContents(file,(String)null),files, sb);
        }
    }

}
