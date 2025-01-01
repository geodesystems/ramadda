/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.cdmdata;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.database.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.OutputHandler;

import org.ramadda.repository.type.*;

import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.geo.KmlUtil;
import org.ramadda.util.sql.Clause;

import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;


import org.w3c.dom.*;
import org.w3c.dom.Element;

import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;


import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;


import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
//import ucar.nc2.dt.PointObsDataset;
//import ucar.nc2.dt.PointObsDatatype;

import ucar.nc2.dt.grid.GridDataset;

import ucar.nc2.ft.FeatureCollection;

import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.NestedPointFeatureCollection;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.point.*;

import ucar.unidata.data.point.PointObFactory;


import ucar.unidata.data.point.TextPointDataSource;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import visad.FieldImpl;

import java.awt.*;
import java.awt.Image;
import java.awt.image.*;

import java.io.*;

import java.io.File;
import java.io.InputStream;



import java.net.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Formatter;

import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Hashtable;
import java.util.List;


import javax.swing.*;





/**
 *
 * @version $Revision: 1.3 $
 */
@SuppressWarnings({ "unchecked", "deprecation" })
public class PointDatabaseTypeHandler extends BlobTypeHandler {

    /** _more_ */
    public static final String PROP_ID = "point.id";

    /** _more_ */
    public static final String PROP_CNT = "point.cnt";

    /** _more_ */
    public static String TYPE_POINTDATABASE = "pointdatabase";

    /** _more_ */
    public static final String FORMAT_HTML = "html";

    /** _more_ */
    public static final String FORMAT_KML = "kml";

    /** _more_ */
    public static final String FORMAT_TIMESERIES = "timeseries";

    /** _more_ */
    public static final String FORMAT_TIMESERIES_CHART = "timeseries_chart";

    /** _more_ */
    public static final String FORMAT_TIMESERIES_DATA = "timeseries_data";


    /** _more_ */
    public static final String FORMAT_TIMELINE = "timeline";

    /** _more_ */
    public static final String FORMAT_CSV = "csv";


    /** _more_ */
    public static final String FORMAT_CHART = "chart";

    /** _more_ */
    public static final String FORMAT_CSVHEADER = "csvheader";

    /** _more_ */
    public static final String FORMAT_CSVIDV = "csvidv";

    /** _more_ */
    public static final String FORMAT_XLS = "xls";

    /** _more_ */
    public static final String FORMAT_NETCDF = "netcdf";

    /** _more_ */
    public static final String FORMAT_MAP = "map";


    /** _more_ */
    public static final double MISSING = -987654.987654;

    /** _more_ */
    public static final String ARG_POINT_TIMESERIES_TITLE = "timeseriestitle";

    /** _more_ */
    public static final String ARG_POINT_STRIDE = "stride";

    /** _more_ */
    public static final String ARG_POINT_CHART_USETIMEFORNAME =
        "usetimeforname";

    /** _more_ */
    public static final String ARG_POINT_REDIRECT = "redirect";

    /** _more_ */
    public static final String ARG_POINT_CHANGETYPE = "changetype";

    /** _more_ */
    public static final String ARG_POINT_ASCENDING = "ascending";

    /** _more_ */
    public static final String ARG_POINT_SORTBY = "sortby";

    /** _more_ */
    public static final String ARG_POINT_UPLOAD_FILE = "upload_file";


    /** _more_ */
    public static final String ARG_POINT_IMAGE_WIDTH = "image_width";

    /** _more_ */
    public static final String ARG_POINT_IMAGE_HEIGHT = "image_height";

    /** _more_ */
    public static final String ARG_POINT_VIEW = "pointview";

    /** _more_ */
    public static final String VIEW_UPLOAD = "upload";

    /** _more_ */
    public static final String VIEW_SEARCHFORM = "searchform";

    /** _more_ */
    public static final String VIEW_METADATA = "metadata";

    /** _more_ */
    public static final String VIEW_DEFAULT = "default";


    /** _more_ */
    public static final String ARG_POINT_FORMAT = "format";

    /** _more_ */
    public static final String ARG_POINT_SEARCH = "search";

    /** _more_ */
    public static final String ARG_POINT_FROMDATE = "fromdate";

    /** _more_ */
    public static final String ARG_POINT_TODATE = "todate";

    /** _more_ */
    public static final String ARG_POINT_BBOX = "bbox";

    /** _more_ */
    public static final String ARG_POINT_HOUR = "hour";

    /** _more_ */
    public static final String ARG_POINT_MONTH = "month";

    /** _more_ */
    public static final String ARG_POINT_PARAM = "what";

    /** _more_ */
    public static final String ARG_POINT_PARAM_ALL = "what_all";

    /** _more_ */
    public static final String ARG_POINT_FIELD_VALUE = "value_";

    /** _more_ */
    public static final String ARG_POINT_FIELD_EXACT = "exact_";

    /** _more_ */
    public static final String ARG_POINT_FIELD_OP = "op_";


    /** _more_ */
    public static final String OP_LT = "op_lt";

    /** _more_ */
    public static final String OP_GT = "op_gt";

    /** _more_ */
    public static final String OP_EQUALS = "op_equals";

    /** _more_ */
    public static final String COL_ID = "obid";

    /** _more_ */
    public static final String COL_DATE = "obtime";

    /** _more_ */
    public static final String COL_LATITUDE = "latitude";

    /** _more_ */
    public static final String COL_LONGITUDE = "longitude";

    /** _more_ */
    public static final String COL_ALTITUDE = "altitude";

    /** _more_ */
    public static final String COL_MONTH = "obmonth";

    /** _more_ */
    public static final String COL_HOUR = "obhour";

    /** _more_ */
    public static final int NUM_BASIC_COLUMNS = 7;

    /** _more_ */
    private List<TwoFacedObject> months;

    /** _more_ */
    private List hours;

    /** _more_ */
    private String chartTemplate;


    /** _more_ */
    private Hashtable<String, List<PointDataMetadata>> metadataCache =
        new Hashtable<String, List<PointDataMetadata>>();



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public PointDatabaseTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param tableName _more_
     *
     * @return _more_
     */
    public boolean shouldExportTable(String tableName) {
        if (tableName.startsWith("pt_")) {
            return false;
        }

        return super.shouldExportTable(tableName);
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void initAfterDatabaseImport() throws Exception {
        super.initAfterDatabaseImport();
        Statement entryStmt =
            getDatabaseManager().select(Tables.ENTRIES.COLUMNS,
                                        Tables.ENTRIES.NAME,
                                        Clause.eq(Tables.ENTRIES.COL_TYPE,
                                            TYPE_POINTDATABASE));

        SqlUtil.Iterator iter = getDatabaseManager().getIterator(entryStmt);
        ResultSet        results;
        while ((results = iter.getNext()) != null) {
            Entry entry = this.createEntryFromDatabase(null, results, false);

            List<Metadata> metadataList =
                getMetadataManager().findMetadata(null, entry,
                    new String[] { ContentMetadataHandler.TYPE_ATTACHMENT },
                    true);
            System.err.println("Initializing point database entry:"
                               + entry.getFullName());
            int     cnt     = 0;
            Request request = getRepository().getTmpRequest();
            for (Metadata metadata : metadataList) {
                File dataFile =
                    new File(
                        IOUtil.joinDir(
                            getRepository().getStorageManager().getEntryDir(
                                metadata.getEntryId(),
                                false), metadata.getAttr1()));
                if (cnt == 0) {
                    Connection connection =
                        getDatabaseManager().getConnection();
                    //                    connection.setAutoCommit(false);
                    createDatabase(request, entry, dataFile,
                                   entry.getParentEntry(), connection);
                    //                    connection.commit();
                    //                    connection.setAutoCommit(true);
                    getDatabaseManager().closeConnection(connection);
                } else {
                    loadData(request, entry, dataFile);
                }
                cnt++;
            }

        }
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    protected String getTableName(String id) {
        id = id.replace("-", "_");

        return "pt_" + id;
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    protected String getTableName(Entry entry) {
        return getTableName(entry.getId());
    }



    //TODO: Handle the initialize entry from xml

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param parent _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
        if ( !newEntry) {
            return;
        }
        Hashtable  properties = getProperties(entry);

        Connection connection = getDatabaseManager().getConnection();
        //        connection.setAutoCommit(false);
        try {
            createDatabase(request, entry, entry.getFile(), parent,
                           connection);
            //            connection.commit();
            //            connection.setAutoCommit(true);
            getEntryManager().addAttachment(request,
                entry, new File(entry.getResource().getPath()), false);
            entry.setResource(new Resource());
        } catch (Exception exc) {
            getDatabaseManager().closeConnection(connection);
            try {
                deleteFromDatabase(getTableName(entry));
            } catch (Exception ignore) {}

            throw exc;
        } finally {
            getDatabaseManager().closeConnection(connection);
        }
    }


    /**
     * _more_
     *
     * @param newEntry _more_
     * @param oldEntry _more_
     *
     * @throws Exception _more_
     */
    public void initializeCopiedEntry(Entry newEntry, Entry oldEntry)
            throws Exception {
        super.initializeCopiedEntry(newEntry, oldEntry);

        //False says don't get the metadata objects from the cache.
        //Create them from the DB
        List<PointDataMetadata> oldMetadata =
            getMetadata(getTableName(oldEntry), false);

        Connection connection = getDatabaseManager().getConnection();
        //        connection.setAutoCommit(false);
        try {
            String newTableName = getTableName(newEntry);
            //Set the name to the new table name
            for (PointDataMetadata pdm : oldMetadata) {
                pdm.tableName = newTableName;
            }
            createDatabase(newEntry, oldMetadata, connection);
            getDatabaseManager().copyTable(getTableName(oldEntry),
                                           getTableName(newEntry),
                                           connection);
            //            connection.commit();
            //            connection.setAutoCommit(true);
        } catch (Exception exc) {
            try {
                getDatabaseManager().closeConnection(connection);
            } catch (Exception ignore) {}
            try {
                deleteFromDatabase(getTableName(newEntry));
            } catch (Exception ignore) {}

            throw exc;
        } finally {
            getDatabaseManager().closeConnection(connection);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param dataFile _more_
     * @param parent _more_
     * @param connection _more_
     *
     * @throws Exception _more_
     */
    protected void createDatabase(Request request, Entry entry,
                                  File dataFile, Entry parent,
                                  Connection connection)
            throws Exception {

        String                  tableName = getTableName(entry);
        List<PointDataMetadata> metadata = new ArrayList<PointDataMetadata>();
        List<PointDataMetadata> stringMetadata =
            new ArrayList<PointDataMetadata>();
        List<PointDataMetadata> numericMetadata =
            new ArrayList<PointDataMetadata>();

        metadata.add(new PointDataMetadata(tableName, COL_ID,
                                           metadata.size(), "ID", "ID", "",
                                           PointDataMetadata.TYPE_INT));
        metadata.add(new PointDataMetadata(tableName, COL_DATE,
                                           metadata.size(),
                                           "Observation Time",
                                           "Observation Time", "",
                                           PointDataMetadata.TYPE_DATE));
        metadata.add(new PointDataMetadata(tableName, COL_LONGITUDE,
                                           metadata.size(), "Longitude",
                                           "Longitude", "degrees",
                                           PointDataMetadata.TYPE_DOUBLE));
        metadata.add(new PointDataMetadata(tableName, COL_LATITUDE,
                                           metadata.size(), "Latitude",
                                           "Latitude", "degrees",
                                           PointDataMetadata.TYPE_DOUBLE));
        metadata.add(new PointDataMetadata(tableName, COL_ALTITUDE,
                                           metadata.size(), "Altitude",
                                           "Altitude", "m",
                                           PointDataMetadata.TYPE_DOUBLE));
        metadata.add(new PointDataMetadata(tableName, COL_MONTH,
                                           metadata.size(), "",
                                           PointDataMetadata.TYPE_INT));
        metadata.add(new PointDataMetadata(tableName, COL_HOUR,
                                           metadata.size(), "",
                                           PointDataMetadata.TYPE_INT));

        System.err.println("get dataset:" + dataFile);

        FeatureDatasetPoint fdp = getDataset(entry, parent, dataFile);
        if (fdp == null) {
            throw new IllegalArgumentException(
                "Could not open file as point observation data");
        }

        List vars = fdp.getDataVariables();

        for (VariableSimpleIF var : (List<VariableSimpleIF>) vars) {
            String unit = var.getUnitsString();
            if (unit == null) {
                unit = "";
            }
            DataType type    = var.getDataType();
            String   varName = var.getShortName();
            String   colName = SqlUtil.cleanName(varName).trim();
            if (colName.equals("latitude") || colName.equals("longitude")
                    || colName.equals("altitude") || colName.equals("date")
                    || colName.equals("time")) {
                continue;
            }
            if (colName.length() == 0) {
                continue;
            }
            colName = "ob_" + colName;
            boolean isString = var.getDataType().equals(DataType.STRING)
                               || var.getDataType().equals(DataType.CHAR);

            List<PointDataMetadata> listToAddTo = (isString
                    ? stringMetadata
                    : numericMetadata);
            listToAddTo.add(new PointDataMetadata(tableName, colName,
                    metadata.size(), varName, var.getFullName(), unit,
                    (isString
                     ? PointDataMetadata.TYPE_STRING
                     : PointDataMetadata.TYPE_DOUBLE)));
        }
        for (PointDataMetadata pdm : stringMetadata) {
            pdm.setColumnNumber(metadata.size());
            metadata.add(pdm);
        }
        for (PointDataMetadata pdm : numericMetadata) {
            pdm.setColumnNumber(metadata.size());
            metadata.add(pdm);
        }
        createDatabase(entry, metadata, connection);
        insertData(request, entry, metadata, fdp, connection, true);
    }




    /**
     * _more_
     *
     * @param entry _more_
     * @param metadata _more_
     * @param connection _more_
     *
     * @throws Exception _more_
     */
    protected void createDatabase(Entry entry,
                                  List<PointDataMetadata> metadata,
                                  Connection connection)
            throws Exception {
        String       tableName = getTableName(entry);
        StringBuffer sql       = new StringBuffer();
        List<String> indexSql  = new ArrayList<String>();
        int          indexCnt  = 0;

        sql.append("CREATE TABLE ");
        sql.append(tableName);
        sql.append(" (");
        boolean first = true;
        for (PointDataMetadata pdm : metadata) {
            if ( !first) {
                sql.append(",");
            }
            first = false;
            sql.append(pdm.getColumnName());
            sql.append(" ");
            sql.append(pdm.getDatabaseType());
            if (pdm.isBasic()) {
                indexSql.add("CREATE INDEX " + tableName + "_I"
                             + (indexCnt++) + " ON " + tableName + " ("
                             + pdm.getColumnName() + ");");
            }
        }
        sql.append(")");
        getDatabaseManager().execute(
            connection, getDatabaseManager().convertSql(sql.toString()),
            1000, 10000);

        for (String index : indexSql) {
            getDatabaseManager().loadSql(connection, index, false, false);
        }

        List<PointDataMetadata> existingMetadata =
            getMetadata(getTableName(entry));

        if (existingMetadata.size() == 0) {
            System.err.println("adding pdm");
            List<Object[]> valueList = new ArrayList<Object[]>();
            for (PointDataMetadata pdm : metadata) {
                valueList.add(pdm.getValues());
            }
            getDatabaseManager().executeInsert(
                Tables.POINTDATAMETADATA.INSERT, valueList);
        }
    }



    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    protected double checkWriteValue(double v) {
        if (v != v) {
            return MISSING;
        }
        if (v == Double.POSITIVE_INFINITY) {
            return Double.MAX_VALUE;
        }
        if (v == Double.NEGATIVE_INFINITY) {
            return -Double.MAX_VALUE;
        }

        return v;
    }


    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    private double checkReadValue(double v) {
        if (v == MISSING) {
            return Double.NaN;
        }

        return v;
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     * @param fdp _more_
     * @param connection _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    private void insertData(Request request, Entry entry,
                            List<PointDataMetadata> metadata,
                            FeatureDatasetPoint fdp, Connection connection,
                            boolean newEntry)
            throws Exception {

        String   tableName = getTableName(entry);
        String[] ARRAY     = new String[metadata.size()];
        for (PointDataMetadata pdm : metadata) {
            ARRAY[pdm.getColumnNumber()] = pdm.getColumnName();
        }
        String insertString = SqlUtil.makeInsert(tableName,
                                  SqlUtil.commaNoDot(ARRAY),
                                  SqlUtil.getQuestionMarks(ARRAY.length));


        double north   = 0,
               south   = 0,
               east    = 0,
               west    = 0;

        long   minTime = (newEntry
                          ? Long.MAX_VALUE
                          : entry.getStartDate());
        long   maxTime = (newEntry
                          ? Long.MIN_VALUE
                          : entry.getEndDate());
        PreparedStatement insertStmt =
            connection.prepareStatement(insertString);
        Object[] values   = new Object[metadata.size()];
        int      cnt      = 0;
        int      batchCnt = 0;
        GregorianCalendar calendar =
            new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);
        boolean   didone     = false;

        Hashtable properties = getProperties(entry);
        int       baseId     = Misc.getProperty(properties, PROP_ID, 0);
        int       totalCnt   = Misc.getProperty(properties, PROP_CNT, 0);
        long      t1         = System.currentTimeMillis();

        long      tt1        = System.currentTimeMillis();
        //        for(int i=0;i<200;i++) {

        PointFeatureIterator pfi = CdmDataOutputHandler.getPointIterator(fdp);
        while (pfi.hasNext()) {
            PointFeature                      po = (PointFeature) pfi.next();
            ucar.unidata.geoloc.EarthLocation el = po.getLocation();
            if (el == null) {
                continue;
            }

            double lat     = el.getLatitude();
            double lon     = el.getLongitude();
            double alt     = el.getAltitude();
            Date   time    = po.getNominalTimeAsDate();


            long   tmpTime = time.getTime();
            if (tmpTime < minTime) {
                minTime = tmpTime;
            }
            if (tmpTime > maxTime) {
                maxTime = tmpTime;
            }

            if (didone) {
                north = Math.max(north, lat);
                south = Math.min(south, lat);
                west  = Math.min(west, lon);
                east  = Math.max(east, lon);
            } else {
                north = (newEntry
                         ? lat
                         : entry.hasNorth()
                           ? entry.getNorth(request)
                           : lat);
                south = (newEntry
                         ? lat
                         : entry.hasSouth()
                           ? entry.getSouth(request)
                           : lat);
                east  = (newEntry
                         ? lon
                         : entry.hasEast()
                           ? entry.getEast(request)
                           : lon);
                west  = (newEntry
                         ? lon
                         : entry.hasWest()
                           ? entry.getWest(request)
                           : lon);
            }
            didone = true;

            calendar.setTime(time);
            StructureData structure           = po.getData();
            boolean       hadAnyNumericValues = false;
            boolean       hadGoodNumericValue = false;
            /*
            if(totalCnt<5) {
                StructureMembers.Member member =
                    structure.findMember("altitude");
                if(member!=null) {
                    double d = structure.convertScalarFloat(member);
                } else {
                    System.err.println("no member");

                }
            }
            */


            for (PointDataMetadata pdm : metadata) {
                Object value;
                if (COL_ID.equals(pdm.getColumnName())) {
                    value = Integer.valueOf(baseId);
                    baseId++;
                } else if (COL_LATITUDE.equals(pdm.getColumnName())) {
                    value = Double.valueOf(checkWriteValue(lat));
                } else if (COL_LONGITUDE.equals(pdm.getColumnName())) {
                    value = Double.valueOf(checkWriteValue(lon));
                } else if (COL_ALTITUDE.equals(pdm.getColumnName())) {
                    value = Double.valueOf(checkWriteValue(alt));
                } else if (COL_DATE.equals(pdm.getColumnName())) {
                    value = time;
                } else if (COL_HOUR.equals(pdm.getColumnName())) {
                    value = Integer.valueOf(calendar.get(GregorianCalendar.HOUR));
                } else if (COL_MONTH.equals(pdm.getColumnName())) {
                    value = Integer.valueOf(calendar.get(GregorianCalendar.MONTH));
                } else {
                    StructureMembers.Member member =
                        structure.findMember((String) pdm.shortName);
                    if (pdm.isString()) {
                        value = structure.getScalarString(member);
                        if (value == null) {
                            value = "";
                        }
                        value = value.toString().trim();
                    } else {
                        double d = structure.convertScalarFloat(member);
                        hadAnyNumericValues = true;
                        if (d == d) {
                            hadGoodNumericValue = true;
                        }
                        value = Double.valueOf(checkWriteValue(d));
                    }
                }
                values[pdm.getColumnNumber()] = value;
            }
            if (hadAnyNumericValues && !hadGoodNumericValue) {
                continue;
            }
            totalCnt++;
            getDatabaseManager().setValues(insertStmt, values);
            insertStmt.addBatch();
            batchCnt++;
            if (batchCnt > 100) {
                insertStmt.executeBatch();
                batchCnt = 0;
            }
            if (((++cnt) % 5000) == 0) {
                System.err.println("added " + cnt + " observations "
                                   + (System.currentTimeMillis() - tt1));
            }
        }

        //        }

        if (batchCnt > 0) {
            insertStmt.executeBatch();
        }
        insertStmt.close();

        long t2 = System.currentTimeMillis();
        System.err.println("inserted " + cnt + " observations in "
                           + (t2 - t1) + "ms");

        properties.put(PROP_CNT, totalCnt + "");
        properties.put(PROP_ID, baseId + "");
        setProperties(entry, properties);

        if (didone) {
            entry.setWest(west);
            entry.setEast(east);
            entry.setNorth(north);
            entry.setSouth(south);
        }

        if (minTime != Long.MAX_VALUE) {
            entry.setStartDate(minTime);
            entry.setEndDate(maxTime);
        }


    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param file _more_
     *
     * @throws Exception _more_
     */
    private void loadData(Request request, Entry entry, File file)
            throws Exception {

        FeatureDatasetPoint fdp = getDataset(entry, entry.getParentEntry(),
                                             file);
        List<PointDataMetadata> metadata = getMetadata(getTableName(entry));
        Connection connection = getDatabaseManager().getConnection();
        try {
            //            connection.setAutoCommit(false);
            insertData(request, entry, metadata, fdp, connection, true);
            //            connection.commit();
            getEntryManager().addAttachment(request,entry, file, true);
            for (PointDataMetadata pdm : metadata) {
                pdm.enumeratedValues = null;
            }
        } finally {
            getDatabaseManager().closeConnection(connection);
        }

    }


    /**
     * _more_
     *
     *
     * @param sb _more_
     * @param request _more_
     * @param entry _more_
     *
     *
     * @throws Exception _more_
     */
    private void doUpload(StringBuffer sb, Request request, Entry entry)
            throws Exception {
        if (request.exists(ARG_POINT_UPLOAD_FILE)) {
            File file =
                new File(request.getUploadedFile(ARG_POINT_UPLOAD_FILE));
            loadData(request, entry, file);
            sb.append(
                getPageHandler().showDialogNote("New data has been loaded"));
        } else {
            sb.append(request.uploadForm(getRepository().URL_ENTRY_SHOW));
            sb.append(msgLabel("New data file"));
            sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
            sb.append(HtmlUtils.hidden(ARG_POINT_VIEW, VIEW_UPLOAD));

            sb.append(HtmlUtils.fileInput(ARG_POINT_UPLOAD_FILE,
                                          HtmlUtils.SIZE_50));
            sb.append(HtmlUtils.br());
            sb.append(HtmlUtils.submit("Add new data"));
            sb.append(request.uploadForm(getRepository().URL_ENTRY_SHOW));
            sb.append(HtmlUtils.formClose());
        }
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
    private Result doSearch(Request request, Entry entry) throws Exception {

        String  format   = request.getString(ARG_POINT_FORMAT, FORMAT_HTML);
        String  baseName = IOUtil.stripExtension(entry.getName());
        boolean redirect = request.get(ARG_POINT_REDIRECT, false);
        request.remove(ARG_POINT_REDIRECT);
        request.remove(ARG_POINT_SEARCH);
        if (format.equals(FORMAT_TIMESERIES)
                || format.equals(FORMAT_TIMESERIES_CHART)) {
            StringBuffer sb = new StringBuffer();
            getHtmlHeader(request, sb, entry, null);
            if (format.equals(FORMAT_TIMESERIES)) {}
            else {
                /*
                //  for amcharts flash
                if (chartTemplate == null) {
                    chartTemplate = getRepository().getResource(
                        "/org/ramadda/repository/resources/chart/amline.html");
                    chartTemplate = chartTemplate.replace("${root}",
                            getRepository().getUrlBase());
                    chartTemplate = chartTemplate.replace("${root}",
                            getRepository().getUrlBase());
                    chartTemplate = chartTemplate.replace("${root}",
                            getRepository().getUrlBase());
                }
                */

                //  for dycharts javascript
                chartTemplate = getRepository().getResource(
                    "/org/ramadda/repository/resources/chart/dycharts.html");
                chartTemplate = chartTemplate.replace("${root}",
                        getRepository().getUrlBase());
                String title = request.getString(ARG_POINT_TIMESERIES_TITLE,
                                   entry.getName());
                if (title.equals("")) {
                    title = entry.getName();
                }
                chartTemplate = chartTemplate.replace("${title}", title);
                chartTemplate = chartTemplate.replace("${options}", "");

                String html = chartTemplate;
                request.put(ARG_POINT_FORMAT, FORMAT_TIMESERIES_DATA);
                String dataUrl = request.getRequestPath() + "/" + baseName
                                 + ".xml" + "?"
                                 + request.getUrlArgs(null, getSet(OP_LT));
                html = html.replace("${dataurl}", dataUrl);

                sb.append(html);
            }

            return new Result("Search Results", sb);
        }

        if (redirect) {
            String urlSuffix = ".html";
            if (format.equals(FORMAT_CSV) || format.equals(FORMAT_CSVIDV)
                    || format.equals(FORMAT_CSVHEADER)) {
                urlSuffix = ".csv";
            } else if (format.equals(FORMAT_XLS)) {
                urlSuffix = ".xls";
            } else if (format.equals(FORMAT_KML)) {
                urlSuffix = ".kml";
            } else if (format.equals(FORMAT_NETCDF)) {
                urlSuffix = ".nc";
            }

            String redirectUrl = request.getRequestPath() + "/"
                                 + HtmlUtils.urlEncode(baseName) + urlSuffix
                                 + "?"
                                 + request.getUrlArgs(null, getSet(OP_LT));

            return new Result(redirectUrl);
        }



        String tableName = getTableName(entry);
        Date[] dateRange = request.getDateRange(ARG_POINT_FROMDATE,
                               ARG_POINT_TODATE, null);
        List<Clause> clauses = new ArrayList<Clause>();

        if (dateRange[0] != null) {
            clauses.add(Clause.ge(COL_DATE, dateRange[0]));
        }

        if (dateRange[1] != null) {
            clauses.add(Clause.le(COL_DATE, dateRange[1]));
        }

        if (request.defined(ARG_POINT_BBOX + "_north")) {
            clauses.add(Clause.le(COL_LATITUDE,
                                  request.get(ARG_POINT_BBOX + "_north",
                                      90.0)));

        }


        if (request.defined(ARG_POINT_BBOX + "_south")) {
            clauses.add(Clause.ge(COL_LATITUDE,
                                  request.get(ARG_POINT_BBOX + "_south",
                                      90.0)));
        }


        if (request.defined(ARG_POINT_BBOX + "_west")) {
            clauses.add(Clause.ge(COL_LONGITUDE,
                                  request.get(ARG_POINT_BBOX + "_west",
                                      -180.0)));
        }

        if (request.defined(ARG_POINT_BBOX + "_east")) {
            clauses.add(Clause.le(COL_LONGITUDE,
                                  request.get(ARG_POINT_BBOX + "_east",
                                      180.0)));

        }

        if (request.defined(ARG_POINT_HOUR)) {
            clauses.add(Clause.eq(COL_HOUR,
                                  request.getString(ARG_POINT_HOUR)));
        }

        if (request.defined(ARG_POINT_MONTH)) {
            clauses.add(Clause.eq(COL_MONTH,
                                  request.getString(ARG_POINT_MONTH)));
        }

        List<PointDataMetadata> metadata = getMetadata(getTableName(entry));
        List<PointDataMetadata> tmp      = new ArrayList<PointDataMetadata>();
        if (request.get(ARG_POINT_PARAM_ALL, false)) {
            tmp = metadata;
        } else {
            List<String> whatList =
                (List<String>) request.get(ARG_POINT_PARAM, new ArrayList());
            HashSet seen = new HashSet();
            for (String col : whatList) {
                seen.add(col);
            }
            for (PointDataMetadata pdm : metadata) {
                if (seen.contains("" + pdm.getColumnNumber())) {
                    tmp.add(pdm);
                }
            }
        }


        //Strip out the month/hour
        List<PointDataMetadata> columnsToUse =
            new ArrayList<PointDataMetadata>();
        for (PointDataMetadata pdm : tmp) {
            //Skip the db month and hour
            if (pdm.getColumnName().equals(COL_MONTH)
                    || pdm.getColumnName().equals(COL_HOUR)) {
                continue;
            }
            columnsToUse.add(pdm);
        }


        for (PointDataMetadata pdm : metadata) {
            if (pdm.isBasic() && !pdm.getColumnName().equals(COL_ALTITUDE)) {
                continue;
            }
            String suffix = "" + pdm.getColumnNumber();
            if ( !request.defined(ARG_POINT_FIELD_VALUE + suffix)) {
                continue;
            }
            if (pdm.isString()) {
                String value = request.getString(ARG_POINT_FIELD_VALUE
                                   + suffix, "");
                if (request.get(ARG_POINT_FIELD_EXACT + suffix, false)) {
                    clauses.add(Clause.eq(pdm.getColumnName(), value));
                } else {
                    clauses.add(Clause.like(pdm.getColumnName(),
                                            "%" + value + "%"));
                }
            } else {
                String op = request.getString(ARG_POINT_FIELD_OP + suffix,
                                OP_LT);
                double value = request.get(ARG_POINT_FIELD_VALUE + suffix,
                                           0.0);
                if (op.equals(OP_LT)) {
                    clauses.add(Clause.le(pdm.getColumnName(), value));
                } else if (op.equals(OP_GT)) {
                    clauses.add(Clause.ge(pdm.getColumnName(), value));
                } else {
                    clauses.add(Clause.eq(pdm.getColumnName(), value));
                }
            }
        }


        StringBuffer cols = null;
        List<PointDataMetadata> queryColumns =
            new ArrayList<PointDataMetadata>();

        //Always add the basic columns
        for (PointDataMetadata pdm : metadata) {
            if (pdm.isBasic()) {
                queryColumns.add(pdm);
            }
        }

        for (PointDataMetadata pdm : columnsToUse) {
            if ( !pdm.isBasic()) {
                queryColumns.add(pdm);
            }
        }


        for (PointDataMetadata pdm : queryColumns) {
            if (cols == null) {
                cols = new StringBuffer();
            } else {
                cols.append(",");
            }
            cols.append(pdm.getColumnName());
        }

        if (cols == null) {
            cols = new StringBuffer();
        }


        String sortByCol = COL_DATE;
        String orderDir  = (request.get(ARG_POINT_ASCENDING, false)
                            ? " ASC "
                            : " DESC ");

        String sortByArg = request.getString(ARG_POINT_SORTBY, "");
        for (PointDataMetadata pdm : metadata) {
            if (pdm.getColumnName().equals(sortByArg)) {
                sortByCol = sortByArg;

                break;
            }
        }

        int max    = request.get(ARG_MAX, 1000);
        int stride = request.get(ARG_POINT_STRIDE, 1);
        if (stride > 1) {
            max = max * stride;
        }
        Statement stmt = getDatabaseManager().select(
                             cols.toString(), Misc.newList(tableName),
                             Clause.and(Clause.toArray(clauses)),
                             " ORDER BY " + sortByCol + orderDir
                             + getDatabaseManager().getLimitString(
                                 request.get(ARG_SKIP, 0), max), max);

        SqlUtil.Iterator iter = getDatabaseManager().getIterator(stmt);
        ResultSet        results;
        int              cnt           = 0;
        List<PointData>  pointDataList = new ArrayList<PointData>();


        int              skipHowMany   = 0;
        while ((results = iter.getNext()) != null) {
            if (skipHowMany > 0) {
                skipHowMany--;

                continue;
            }
            if (stride > 1) {
                skipHowMany = stride - 1;
            }
            int col = 1;
            PointData pointData =
                new PointData(results.getInt(col++),
                              getDatabaseManager().getDate(results, col++),
                              checkReadValue(results.getDouble(col++)),
                              checkReadValue(results.getDouble(col++)),
                              checkReadValue(results.getDouble(col++)), 0, 0);
            List values = new ArrayList();
            //Add in the selected basic values
            for (PointDataMetadata pdm : columnsToUse) {
                if (pdm.isBasic()) {
                    values.add(pointData.getValue(pdm.getColumnName()));
                }
            }

            while (col <= queryColumns.size()) {
                PointDataMetadata pdm = queryColumns.get(col - 1);
                if (pdm.isDate()) {
                    continue;
                }
                if (pdm.isString()) {
                    pointData.setValue(pdm, results.getString(col).trim());
                } else {
                    double d = checkReadValue(results.getDouble(col));
                    pointData.setValue(pdm, Double.valueOf(d));
                }
                col++;
            }
            pointDataList.add(pointData);
        }




        if (format.equals(FORMAT_HTML) || format.equals(FORMAT_TIMELINE)) {
            return makeSearchResultsHtml(request, entry, columnsToUse,
                                         pointDataList,
                                         format.equals(FORMAT_TIMELINE));
        }
        if (format.equals(FORMAT_KML)) {
            return makeSearchResultsKml(request, entry, columnsToUse,
                                        pointDataList,
                                        format.equals(FORMAT_TIMELINE));
        } else if (format.equals(FORMAT_CSV) || format.equals(FORMAT_CSVIDV)
                   || format.equals(FORMAT_CSVHEADER)
                   || format.equals(FORMAT_XLS)) {
            ensureMinimalParameters(columnsToUse, metadata);

            return makeSearchResultsCsv(request, entry, columnsToUse,
                                        pointDataList, format);
        } else if (format.equals(FORMAT_CHART)) {
            return makeSearchResultsChart(request, entry, columnsToUse,
                                          pointDataList);
        } else if (format.equals(FORMAT_TIMESERIES_DATA)) {
            return makeSearchResultsTimeSeriesData(request, entry,
                    columnsToUse, pointDataList);
        } else if (format.equals(FORMAT_MAP)) {
            return makeSearchResultsMap(request, entry, columnsToUse,
                                        pointDataList);
        } else {
            ensureMinimalParameters(columnsToUse, metadata);

            return makeSearchResultsNetcdf(request, entry, columnsToUse,
                                           pointDataList);
        }

    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private HashSet<String> getSet(String s) {
        HashSet h = new HashSet();
        h.add(s);

        return h;
    }

    /**
     * _more_
     *
     * @param columnsToUse _more_
     * @param metadata _more_
     */
    private void ensureMinimalParameters(
            List<PointDataMetadata> columnsToUse,
            List<PointDataMetadata> metadata) {
        if ( !PointDataMetadata.contains(columnsToUse, COL_DATE)) {
            columnsToUse.add(0, PointDataMetadata.find(metadata, COL_DATE));
        }


        if ( !PointDataMetadata.contains(columnsToUse, COL_LATITUDE)) {
            columnsToUse.add(0, PointDataMetadata.find(metadata,
                    COL_LATITUDE));
        }

        if ( !PointDataMetadata.contains(columnsToUse, COL_LONGITUDE)) {
            columnsToUse.add(0, PointDataMetadata.find(metadata,
                    COL_LONGITUDE));
        }
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param entry _more_
     * @param list _more_
     *
     * @throws Exception _more_
     */
    private void getHtmlHeader(Request request, StringBuffer sb, Entry entry,
                               List<PointData> list)
            throws Exception {
        StringBuffer searchForm = new StringBuffer();
        searchForm.append("<ul><hr>");
        createSearchForm(searchForm, request, entry);
        searchForm.append("<hr></ul>");

        StringBuffer cntSB    = new StringBuffer();


        int          max      = request.get(ARG_MAX, 1000);
        int          numItems = ((list == null)
                                 ? max
                                 : list.size());
        if ((numItems > 0)
                && ((numItems == max) || request.defined(ARG_SKIP))) {
            int skip = Math.max(0, request.get(ARG_SKIP, 0));
            cntSB.append(msgLabel("Showing") + (skip + 1) + "-"
                         + (skip + numItems));
            List<String> toks = new ArrayList<String>();
            String       url;
            if (skip > 0) {
                request.put(ARG_SKIP, (skip - max) + "");
                url = request.getRequestPath() + "?"
                      + request.getUrlArgs(null, getSet(OP_LT));
                request.put(ARG_SKIP, skip + "");
                toks.add(HtmlUtils.href(url, msg("Previous...")));
            }

            if (numItems >= max) {
                request.put(ARG_SKIP, (skip + max) + "");
                url = request.getRequestPath() + "?"
                      + request.getUrlArgs(null, getSet(OP_LT));
                request.put(ARG_SKIP, skip + "");
                toks.add(HtmlUtils.href(url, msg("Next...")));
            }


            if (numItems >= max) {
                request.put(ARG_MAX, "" + (max + 100));
                url = request.getRequestPath() + "?"
                      + request.getUrlArgs(null, getSet(OP_LT));
                toks.add(HtmlUtils.href(url, msg("View More")));
                request.put(ARG_MAX, "" + (max / 2));
                url = request.getRequestPath() + "?"
                      + request.getUrlArgs(null, getSet(OP_LT));
                toks.add(HtmlUtils.href(url, msg("View Less")));
            }

            request.put(ARG_SKIP, "" + skip);
            request.put(ARG_MAX, "" + max);
            if (toks.size() > 0) {
                cntSB.append(HtmlUtils.space(2));
                cntSB.append(StringUtil.join(HtmlUtils.span("&nbsp;|&nbsp;",
                        HtmlUtils.cssClass(CSS_CLASS_SEPARATOR)), toks));
            }
        }



        sb.append(getHeader(request, entry));


        sb.append(header(msg("Point Data Search Results")));
        sb.append(cntSB);
        sb.append(HtmlUtils.makeShowHideBlock(msg("Search Again"),
                searchForm.toString(), false));



    }





    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param columnsToUse _more_
     * @param list _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result makeSearchResultsChart(
            Request request, Entry entry,
            List<PointDataMetadata> columnsToUse, List<PointData> list)
            throws Exception {

        StringBuffer sb = new StringBuffer();
        getHtmlHeader(request, sb, entry, list);

        if (list.size() == 0) {
            sb.append(msg("No results found"));

            return new Result("Point Search Results", sb);
        }
        getPageHandler().addGoogleJSImport(request, sb);
        sb.append(
            "<script type=\"text/javascript\">\ngoogle.load('visualization', '1', {'packages':['motionchart']});\ngoogle.setOnLoadCallback(drawChart);\nfunction drawChart() {\n        var data = new google.visualization.DataTable();\n");
        sb.append("data.addRows(" + list.size() + ");\n");

        sb.append("data.addColumn('string', 'Location');\n");
        sb.append("data.addColumn('date', 'Date');\n");

        int    baseCnt   = 2;

        String entityCol = null;
        boolean useTimeForName = request.get(ARG_POINT_CHART_USETIMEFORNAME,
                                             false);
        String           dateFormat = "yyyy/MM/dd HH:mm:ss";
        SimpleDateFormat sdf        = new SimpleDateFormat(dateFormat);
        for (PointDataMetadata pdm : columnsToUse) {
            String pdmName = pdm.shortName.toLowerCase();

            if (pdm.getColumnName().equals(COL_ID)) {
                continue;
            }
            if (entityCol == null) {
                if ((pdmName.indexOf("station") >= 0)
                        || pdmName.equals("region") || pdmName.equals("id")
                        || pdmName.equals("idn")) {
                    entityCol = pdm.getColumnName();

                    continue;
                }
            }
            if (pdmName.indexOf("name") >= 0) {
                entityCol = pdm.getColumnName();

                break;
            }
        }

        //        System.err.println ("entityCol:" + entityCol);

        for (PointDataMetadata pdm : columnsToUse) {
            if ((entityCol != null) && pdm.isColumn(entityCol)) {
                continue;
            }
            String varName = pdm.formatName();
            varName = varName.replace("'", "\\'");
            if (pdm.isString()) {
                sb.append("data.addColumn('string', '" + varName + "');\n");
            } else if (pdm.isDate()) {
                //For now skip the date
                //                sb.append("data.addColumn('string', '" + varName+"');\n");
            } else {
                sb.append("data.addColumn('number', '" + varName + "');\n");
            }
        }


        GregorianCalendar cal =
            new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);

        int row = -1;

        sb.append("var theDate;\n");
        for (PointData pointData : list) {
            row++;
            cal.setTime(pointData.date);
            String entityName;
            if (useTimeForName) {
                entityName = sdf.format(pointData.date);
            } else if (entityCol != null) {
                entityName = pointData.getValue(entityCol).toString().trim();
            } else {
                entityName = "latlon_" + pointData.lat + "/" + pointData.lon;
            }

            entityName = entityName.replace("'", "\\'");
            sb.append("theDate = new Date(" + cal.get(cal.YEAR) + ","
                      + cal.get(cal.MONTH) + "," + cal.get(cal.DAY_OF_MONTH)
                      + ");\n");

            sb.append("theDate.setHours(" + cal.get(cal.HOUR) + ","
                      + cal.get(cal.MINUTE) + "," + cal.get(cal.SECOND) + ","
                      + cal.get(cal.MILLISECOND) + ");\n");

            sb.append("data.setValue(" + row + ", 0, '" + entityName
                      + "');\n");
            sb.append("data.setValue(" + row + ", 1, theDate);\n");


            int cnt = -1;
            for (PointDataMetadata pdm : columnsToUse) {
                //Already did the entity
                if ((entityCol != null) && pdm.isColumn(entityCol)) {
                    continue;
                }
                Object value = pointData.getValue(pdm);
                if (row == 0) {
                    sb.append("//" + pdm.shortName + "\n");
                }
                if (pdm.isString()) {
                    cnt++;
                    String tmp = value.toString().trim();
                    tmp = tmp.replace("'", "\\'");
                    sb.append("data.setValue(" + row + ", " + (cnt + baseCnt)
                              + ", '" + tmp + "');\n");
                } else if (pdm.isDate()) {
                    //                    sb.append("data.setValue(" +row+", " + (cnt+baseCnt) +", " +value +");\n");
                } else {
                    cnt++;
                    sb.append("data.setValue(" + row + ", " + (cnt + baseCnt)
                              + ", " + value + ");\n");

                }
            }
        }

        sb.append(
            "var chart = new google.visualization.MotionChart(document.getElementById('chart_div'));\n");
        sb.append("chart.draw(data, {width: 800, height:500});\n");
        sb.append("}\n");
        sb.append("</script>\n");
        sb.append(
            "<div id=\"chart_div\" style=\"width: 800px; height: 500px;\"></div>\n");

        return new Result("Point Search Results", sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param columnsToUse _more_
     * @param list _more_
     * @param showTimeline _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result makeSearchResultsKml(Request request, Entry entry,
                                        List<PointDataMetadata> columnsToUse,
                                        List<PointData> list,
                                        boolean showTimeline)
            throws Exception {
        Element root    = KmlUtil.kml(entry.getName());
        Element docNode = KmlUtil.document(root, entry.getName());

        for (PointData pointData : list) {
            int          lblCnt = 0;
            StringBuffer info   = new StringBuffer("");
            StringBuffer label  = new StringBuffer("");
            info.append("<b>Date:</b> " + pointData.date + "<br>");
            int cnt = -1;
            for (PointDataMetadata pdm : columnsToUse) {
                cnt++;
                if (pdm.isBasic()) {
                    continue;
                }
                Object value = pointData.getValue(pdm);
                if (lblCnt < 4) {
                    if (lblCnt > 0) {
                        label.append("/");
                    }
                    lblCnt++;
                    if (value instanceof Double) {
                        value = Misc.format(((Double) value).doubleValue());
                        label.append(value + pdm.formatUnit() + "");
                    } else {
                        label.append("" + value);
                    }
                }
                info.append(HtmlUtils.b(pdm.formatName()) + ":" + value + " "
                            + pdm.formatUnit() + "<br>");
            }

            Element placemark = KmlUtil.placemark(docNode, label.toString(),
                                    info.toString(), pointData.lat,
                                    pointData.lon, pointData.alt, null);
            KmlUtil.timestamp(placemark, pointData.date);
        }

        StringBuffer sb = new StringBuffer(XmlUtil.toString(root));

        return new Result(msg("Point Data"), sb,
                          getRepository().getMimeTypeFromSuffix(".kml"));

    }





    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param columnsToUse _more_
     * @param list _more_
     * @param showTimeline _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result makeSearchResultsHtml(
            Request request, Entry entry,
            List<PointDataMetadata> columnsToUse, List<PointData> list,
            boolean showTimeline)
            throws Exception {



        StringBuffer sb = new StringBuffer();
        getHtmlHeader(request, sb, entry, list);

        if (list.size() == 0) {
            sb.append(msg("No results found"));

            return new Result("Point Search Results", sb);
        }


        if (showTimeline) {
            String timelineAppletTemplate =
                getRepository().getResource(
                    "/org/ramadda/repository/resources/web/timelineapplet.html");
            List times  = new ArrayList();
            List labels = new ArrayList();
            List ids    = new ArrayList();
            for (PointData pointData : list) {
                times.add(SqlUtil.format(pointData.date));
                labels.add(SqlUtil.format(pointData.date));
                ids.add("");
            }
            String tmp = StringUtil.replace(timelineAppletTemplate,
                                            "${times}",
                                            StringUtil.join(",", times));
            tmp = StringUtil.replace(tmp, "${root}",
                                     getRepository().getUrlBase());
            tmp = StringUtil.replace(tmp, "${labels}",
                                     StringUtil.join(",", labels));
            tmp = StringUtil.replace(tmp, "${ids}",
                                     StringUtil.join(",", ids));
            tmp = StringUtil.replace(tmp, "${loadurl}", "");
            sb.append(tmp);
        }

        sb.append("<table>");
        StringBuffer header = new StringBuffer();
        for (PointDataMetadata pdm : columnsToUse) {
            header.append(HtmlUtils.cols(HtmlUtils.b(pdm.formatName() + " "
                    + pdm.formatUnit())));
        }
        sb.append(HtmlUtils.row(header.toString(),
                                HtmlUtils.attr(HtmlUtils.ATTR_ALIGN,
                                    "center")));



        for (PointData pointData : list) {
            StringBuffer row = new StringBuffer();
            int          cnt = -1;
            for (PointDataMetadata pdm : columnsToUse) {
                cnt++;
                Object value = pointData.getValue(pdm);
                if (value instanceof Double) {
                    double d = ((Double) value).doubleValue();
                    row.append(HtmlUtils.cols("" + Misc.format(d)));
                } else {
                    row.append(HtmlUtils.cols("" + value));
                }
            }
            sb.append(HtmlUtils.row(row.toString(),
                                    HtmlUtils.attr(HtmlUtils.ATTR_ALIGN,
                                        "right")));
        }
        sb.append("</table>");

        return new Result("Point Search Results", sb);
    }










    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param columnsToUse _more_
     * @param list _more_
     * @param format _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result makeSearchResultsCsv(Request request, Entry entry,
                                        List<PointDataMetadata> columnsToUse,
                                        List<PointData> list, String format)
            throws Exception {
        StringBuffer sb = getCsv(columnsToUse, list, format);

        return new Result("", sb, "text/csv");
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param columnsToUse _more_
     * @param list _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result makeSearchResultsTimeSeriesXml(Request request,
            Entry entry, List<PointDataMetadata> columnsToUse,
            List<PointData> list)
            throws Exception {
        StringBuffer sb = getXml(columnsToUse, list);

        return new Result("", sb, "text/csv");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param columnsToUse _more_
     * @param list _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result makeSearchResultsTimeSeriesData(Request request,
            Entry entry, List<PointDataMetadata> columnsToUse,
            List<PointData> list)
            throws Exception {
        StringBuffer sb = getCsv(columnsToUse, list, FORMAT_TIMESERIES_DATA);

        return new Result("", sb, "text/csv");
    }

    /**
     * <?xml version="1.0" encoding="UTF-8"?>
     * <chart>
     * <!--<message><![CDATA[You can broadcast any message to chart from data XML file]]></message> -->
     *   <series>
     *           <value xid="0">1949</value>
     *           <value xid="1">1950</value>
     *   </series>
     *   <graphs>
     *           <graph gid="1">
     *                   <value xid="0">2.54</value>
     *                   <value xid="1">2.51</value>
     *           </graph>
     *           <graph gid="2">
     *                   <value xid="0">20.21</value>
     *                   <value xid="1">19.73</value>
     *           </graph>
     *
     *   </graphs>
     * </chart>
     *
     * @param columnsToUse _more_
     * @param list _more_
     * @param format _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */

    /**
     * _more_
     *
     * @param columnsToUse _more_
     * @param list _more_
     * @param format _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private StringBuffer getCsv(List<PointDataMetadata> columnsToUse,
                                List<PointData> list, String format)
            throws Exception {
        boolean addMetadata = format.equals(FORMAT_CSVIDV);
        boolean addHeader = format.equals(FORMAT_CSVHEADER)
                            || format.equals(FORMAT_TIMESERIES_DATA);
        boolean          xls        = format.equals(FORMAT_XLS);

        String           dateFormat = "yyyy/MM/dd HH:mm:ss Z";
        SimpleDateFormat sdf        = new SimpleDateFormat(dateFormat);
        sdf.setTimeZone(RepositoryUtil.TIMEZONE_DEFAULT);
        StringBuffer sb    = new StringBuffer();
        String       comma = ", ";
        List         rows  = new ArrayList();

        if (addHeader) {
            int cnt = 0;
            for (PointDataMetadata pdm : columnsToUse) {
                if (cnt != 0) {
                    sb.append(",");
                }
                sb.append(pdm.shortName);
                sb.append(pdm.formatUnit());
                cnt++;
            }
            sb.append("\n");
        }

        if (addMetadata) {
            StringBuffer h1 = new StringBuffer();
            StringBuffer h2 = new StringBuffer();
            h1.append("(index) -> (");
            int cnt = 0;
            for (PointDataMetadata pdm : columnsToUse) {
                if (cnt != 0) {
                    h1.append(",");
                    h2.append(",");
                }
                h1.append(pdm.varName());
                h2.append(pdm.varName());
                if (pdm.isString()) {
                    h1.append("(Text)");
                    h2.append("(Text)");
                } else {
                    h2.append("[");
                    if ((pdm.unit != null) && (pdm.unit.length() > 0)) {
                        h2.append(" unit=\"" + pdm.unit + "\" ");
                    }
                    if (pdm.varType.equals(pdm.TYPE_DATE)) {
                        h2.append(" fmt=\"" + dateFormat + "\" ");
                    }
                    h2.append("]");
                }
                cnt++;
            }
            h1.append(")");
            sb.append(h1);
            sb.append("\n");
            sb.append(h2);
            sb.append("\n");
        }


        for (PointData pointData : list) {
            boolean didOne = false;
            for (PointDataMetadata pdm : columnsToUse) {
                Object value = pointData.getValue(pdm);
                if (didOne) {
                    sb.append(comma);
                }
                didOne = true;
                if (pdm.isDate()) {
                    sb.append(sdf.format((Date) value));
                } else {
                    sb.append(value);
                }
            }
            sb.append("\n");
        }

        return sb;
    }




    /**
     * _more_
     *
     * @param columnsToUse _more_
     * @param list _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private StringBuffer getXml(List<PointDataMetadata> columnsToUse,
                                List<PointData> list)
            throws Exception {
        String           format      = "";
        boolean          addMetadata = format.equals(FORMAT_CSVIDV);
        boolean          addHeader   = format.equals(FORMAT_CSVHEADER);
        boolean          xls         = format.equals(FORMAT_XLS);

        String           dateFormat  = "yyyy/MM/dd HH:mm:ss Z";
        SimpleDateFormat sdf         = new SimpleDateFormat(dateFormat);
        sdf.setTimeZone(RepositoryUtil.TIMEZONE_DEFAULT);
        StringBuffer sb    = new StringBuffer();
        String       comma = ", ";
        List         rows  = new ArrayList();

        if (addHeader) {
            int cnt = 0;
            for (PointDataMetadata pdm : columnsToUse) {
                if (cnt != 0) {
                    sb.append(",");
                }
                sb.append(pdm.shortName);
                sb.append(pdm.formatUnit());
                cnt++;
            }
            sb.append("\n");
        }

        if (addMetadata) {
            StringBuffer h1 = new StringBuffer();
            StringBuffer h2 = new StringBuffer();
            h1.append("(index) -> (");
            int cnt = 0;
            for (PointDataMetadata pdm : columnsToUse) {
                if (cnt != 0) {
                    h1.append(",");
                    h2.append(",");
                }
                h1.append(pdm.varName());
                h2.append(pdm.varName());
                if (pdm.isString()) {
                    h1.append("(Text)");
                    h2.append("(Text)");
                } else {
                    h2.append("[");
                    if ((pdm.unit != null) && (pdm.unit.length() > 0)) {
                        h2.append(" unit=\"" + pdm.unit + "\" ");
                    }
                    if (pdm.varType.equals(pdm.TYPE_DATE)) {
                        h2.append(" fmt=\"" + dateFormat + "\" ");
                    }
                    h2.append("]");
                }
                cnt++;
            }
            h1.append(")");
            sb.append(h1);
            sb.append("\n");
            sb.append(h2);
            sb.append("\n");
        }


        for (PointData pointData : list) {
            boolean didOne = false;
            for (PointDataMetadata pdm : columnsToUse) {
                Object value = pointData.getValue(pdm);
                if (didOne) {
                    sb.append(comma);
                }
                didOne = true;
                if (pdm.isDate()) {
                    sb.append(sdf.format((Date) value));
                } else {
                    sb.append(value);
                }
            }
            sb.append("\n");
        }

        return sb;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param columnsToUse _more_
     * @param list _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result makeSearchResultsMap(Request request, Entry entry,
                                        List<PointDataMetadata> columnsToUse,
                                        List<PointData> list)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(getHeader(request, entry));
        String icon = getIconUrl("/icons/pointdata.gif");
        MapInfo map = getRepository().getMapManager().createMap(request,
                          entry, request.getString(ARG_WIDTH, "800"),
                          request.getString(ARG_HEIGHT, "500"), false, null);
        int cnt = 0;
        for (PointData pointData : list) {
            StringBuffer info = new StringBuffer("");
            cnt++;

            info.append("Date:" + pointData.date);
            info.append("<br>");
            for (PointDataMetadata pdm : columnsToUse) {
                Object value = pointData.getValue(pdm);
                info.append(pdm.shortName + ":" + value);
                info.append("<br>");
            }

            map.addMarker(HtmlUtils.quote("" + cnt), pointData.lat,
                          pointData.lon, icon, "", info.toString());
        }
        map.center();
        sb.append(map.getHtml());

        return new Result("Point Search Results", sb);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param columnsToUse _more_
     * @param list _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result makeSearchResultsNetcdf(
            Request request, Entry entry,
            List<PointDataMetadata> columnsToUse, List<PointData> list)
            throws Exception {

        TextPointDataSource dataSource = new TextPointDataSource("dummy.csv");
        StringBuffer        sb = getCsv(columnsToUse, list, FORMAT_CSVIDV);
        FieldImpl field = dataSource.makeObs(sb.toString(), ",", null, null,
                                             null, false, false);

        File file = getStorageManager().getTmpFile(request, "test.nc");
        PointObFactory.writeToNetcdf(file, field);
        Result result =
            new Result("", getStorageManager().getFileInputStream(file),
                       "application/x-netcdf");
        result.addHttpHeader(HtmlUtils.HTTP_CONTENT_LENGTH,
                             "" + file.length());

        return result;
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
    private String getHeader(Request request, Entry entry) throws Exception {
        boolean canEdit     = getAccessManager().canDoEdit(request, entry);

        List    headerLinks = new ArrayList();
        String  view = request.getString(ARG_POINT_VIEW, VIEW_SEARCHFORM);


        boolean doSearch = request.exists(ARG_POINT_SEARCH)
                           || request.exists(ARG_POINT_FORMAT);

        if ( !doSearch && view.equals(VIEW_SEARCHFORM)) {
            headerLinks.add(HtmlUtils.b(msg("Search form")));
        } else {
            Object tmp1 = request.remove(ARG_POINT_SEARCH);
            Object tmp2 = request.remove(ARG_POINT_FORMAT);
            headerLinks.add(
                HtmlUtils.href(
                    request.entryUrl(getRepository().URL_ENTRY_SHOW, entry),
                    msg("Search form")));

            if (tmp1 != null) {
                request.put(ARG_POINT_SEARCH, tmp1);
            }
            if (tmp2 != null) {
                request.put(ARG_POINT_FORMAT, tmp2);
            }
        }

        if (view.equals(VIEW_METADATA)) {
            headerLinks.add(HtmlUtils.b(msg("Metadata")));
        } else {
            headerLinks.add(
                HtmlUtils.href(
                    request.entryUrl(
                        getRepository().URL_ENTRY_SHOW, entry,
                        ARG_POINT_VIEW, VIEW_METADATA), msg("Metadata")));
        }

        if (canEdit) {
            if (view.equals(VIEW_UPLOAD)) {
                headerLinks.add(HtmlUtils.b(msg("Upload more data")));
            } else {
                headerLinks.add(
                    HtmlUtils.href(
                        request.entryUrl(
                            getRepository().URL_ENTRY_SHOW, entry,
                            ARG_POINT_VIEW, VIEW_UPLOAD), msg(
                                "Upload more data")));
            }
        }

        headerLinks.add(
            HtmlUtils.href(
                request.entryUrl(
                    getRepository().URL_ENTRY_SHOW, entry, ARG_POINT_VIEW,
                    VIEW_DEFAULT), msg("Show default")));
        String header = StringUtil.join("&nbsp;|&nbsp;", headerLinks);

        return HtmlUtils.center(header);
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
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        boolean canEdit = getAccessManager().canDoEdit(request, entry);

        String  view    = request.getString(ARG_POINT_VIEW, VIEW_SEARCHFORM);
        if (view.equals(VIEW_DEFAULT)) {
            return null;
        }

        boolean doSearch = request.exists(ARG_POINT_SEARCH)
                           || request.exists(ARG_POINT_FORMAT);
        if (doSearch) {
            return doSearch(request, entry);
        }

        StringBuffer sb = new StringBuffer();
        if (view.equals(VIEW_METADATA)) {
            if (request.getString(ARG_RESPONSE, "").equals("xml")) {
                return showMetadataXml(request, entry);
            }
            sb.append(getHeader(request, entry));
            showMetadata(sb, request, entry);
        } else if (view.equals(VIEW_UPLOAD)) {
            sb.append(getHeader(request, entry));
            doUpload(sb, request, entry);
        } else {
            sb.append(getHeader(request, entry));
            sb.append(entry.getDescription());
            createSearchForm(sb, request, entry);
        }

        return new Result("Point Data", sb);
    }





    /**
     * _more_
     */
    private void initSelectors() {
        if (months == null) {
            months = Misc.toList(new Object[] {
                new TwoFacedObject("------", ""),
                new TwoFacedObject("January", 0),
                new TwoFacedObject("February", 1),
                new TwoFacedObject("March", 2),
                new TwoFacedObject("April", 3), new TwoFacedObject("May", 4),
                new TwoFacedObject("June", 5), new TwoFacedObject("July", 6),
                new TwoFacedObject("August", 7),
                new TwoFacedObject("September", 8),
                new TwoFacedObject("October", 9),
                new TwoFacedObject("November", 10),
                new TwoFacedObject("December", 11)
            });
        }
        if (hours == null) {
            hours = new ArrayList();
            hours.add(new TwoFacedObject("------", ""));
            for (int i = 0; i < 24; i++) {
                hours.add("" + i);
            }
        }
    }



    /**
     * _more_
     *
     * @param sb _more_
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    private void showMetadata(StringBuffer sb, Request request, Entry entry)
            throws Exception {


        String                  tableName = getTableName(entry);
        boolean canEdit = getAccessManager().canDoEdit(request, entry);

        List<PointDataMetadata> metadata  = getMetadata(getTableName(entry));
        if (canEdit && request.defined(ARG_POINT_CHANGETYPE)) {
            String column = request.getString(ARG_POINT_CHANGETYPE, "");
            for (PointDataMetadata pdm : metadata) {
                if (pdm.isString() && pdm.getColumnName().equals(column)) {
                    if (pdm.varType.equals(pdm.TYPE_STRING)) {
                        pdm.varType = pdm.TYPE_ENUMERATION;
                    } else {
                        pdm.varType = pdm.TYPE_STRING;
                    }
                    getDatabaseManager()
                        .update(
                            Tables.POINTDATAMETADATA.NAME,
                            Clause.and(
                                Clause.eq(
                                    Tables.POINTDATAMETADATA.COL_TABLENAME,
                                    tableName), Clause
                                        .eq(
                                        Tables.POINTDATAMETADATA
                                            .COL_COLUMNNAME, pdm
                                            .getColumnName())), new String[] {
                                                Tables.POINTDATAMETADATA
                                                    .COL_VARTYPE }, new String[] {
                                                        pdm.varType });

                    break;
                }
            }
        }
        sb.append("<table>");
        sb.append(
            HtmlUtils.row(
                HtmlUtils.cols(
                    HtmlUtils.b(msg("Short Name")),
                    HtmlUtils.b(msg("Long Name")),
                    HtmlUtils.b(msg("Unit Name")),
                    HtmlUtils.b(msg("Type")))));
        for (PointDataMetadata pdm : metadata) {
            String type = pdm.varType;
            if (canEdit && pdm.isString()) {
                type = HtmlUtils.href(
                    request.entryUrl(
                        getRepository().URL_ENTRY_SHOW, entry,
                        ARG_POINT_VIEW, VIEW_METADATA, ARG_POINT_CHANGETYPE,
                        pdm.getColumnName()), type,
                            HtmlUtils.title(msg("Change type")));

            }
            sb.append(HtmlUtils.row(HtmlUtils.cols(pdm.shortName,
                    pdm.longName, pdm.unit, type)));
        }
        sb.append("</table>");
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
    private Result showMetadataXml(Request request, Entry entry)
            throws Exception {
        String                  tableName = getTableName(entry);
        List<PointDataMetadata> metadata  = getMetadata(getTableName(entry));
        Document                doc       = XmlUtil.makeDocument();
        Element                 root      =
            doc.createElement("pointmetadata");
        StringBuffer            xml       = new StringBuffer();
        for (PointDataMetadata pdm : metadata) {
            String  type    = pdm.varType;
            Element colNode = XmlUtil.create("column", root);
            XmlUtil.create(doc, "id", colNode, "" + pdm.getColumnNumber(),
                           null);
            XmlUtil.create(doc, "isbasic", colNode, "" + pdm.isBasic(), null);
            XmlUtil.create(doc, "shortname", colNode, pdm.shortName, null);
            XmlUtil.create(doc, "longname", colNode, pdm.longName, null);
            XmlUtil.create(doc, "unit", colNode, pdm.unit, null);
            XmlUtil.create(doc, "type", colNode, pdm.varType, null);
        }

        return new Result("", new StringBuffer(XmlUtil.toString(root)),
                          "text/xml");
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    private void createSearchForm(StringBuffer sb, Request request,
                                  Entry entry)
            throws Exception {

        initSelectors();
        List<PointDataMetadata> metadata  = getMetadata(getTableName(entry));
        String                  timezone  = getEntryUtil().getTimezone(request, entry);
        String                  tableName = getTableName(entry);



        Date[] dateRange = request.getDateRange(ARG_POINT_FROMDATE,
                               ARG_POINT_TODATE, null);

        if (dateRange[0] == null) {
            dateRange[0] = new Date(entry.getStartDate());
        }
        if (dateRange[1] == null) {
            dateRange[1] = new Date(entry.getEndDate());
        }


        StringBuffer basicSB = new StringBuffer();

        basicSB.append(HtmlUtils.formTable());
        basicSB.append(HtmlUtils.hidden(ARG_OUTPUT,
                                        OutputHandler.OUTPUT_HTML));
        basicSB.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));

        basicSB.append(
            HtmlUtils.formEntry(
                msgLabel("From Date"),
                getDateHandler().makeDateInput(
                    request, ARG_POINT_FROMDATE, "pointsearch", dateRange[0],
                    timezone)));

        basicSB.append(
            HtmlUtils.formEntry(
                msgLabel("To Date"),
                getDateHandler().makeDateInput(
                    request, ARG_POINT_TODATE, "pointsearch", dateRange[1],
                    timezone)));


        basicSB.append(
            HtmlUtils.formEntry(
                msgLabel("Month"),
                HtmlUtils.select(
                    ARG_POINT_MONTH, months,
                    request.getString(ARG_POINT_MONTH, "")) + " "
                        + msgLabel("Hour")
                        + HtmlUtils.select(
                            ARG_POINT_HOUR, hours,
                            request.getString(ARG_POINT_HOUR, ""))));


        MapInfo map = getRepository().getMapManager().createMap(request,
                          entry, true, null);
        map.addBox(request,  entry, new MapProperties("blue", false));
        map.centerOn(request,entry);

        String llb = map.makeSelector(ARG_POINT_BBOX, true, null);
        basicSB.append(HtmlUtils.formEntryTop(msgLabel("Location"), llb));
        basicSB.append(HtmlUtils.hidden(ARG_POINT_REDIRECT, "true"));
        basicSB.append(HtmlUtils.formTableClose());


        String max = HtmlUtils.input(ARG_MAX,
                                     request.getString(ARG_MAX, "1000"),
                                     HtmlUtils.SIZE_5
                                     + HtmlUtils.id(ARG_MAX));
        List formats = Misc.toList(new Object[] {
            new TwoFacedObject("Html", FORMAT_HTML),
            new TwoFacedObject("Interactive Chart", FORMAT_CHART),
            new TwoFacedObject("Interactive Time Series",
                               FORMAT_TIMESERIES_CHART),
            new TwoFacedObject("Time Series Image", FORMAT_TIMESERIES),
            new TwoFacedObject("Map", FORMAT_MAP),
            new TwoFacedObject("Google Earth KML", FORMAT_KML),
            //            new TwoFacedObject("CSV-Plain", FORMAT_CSV),
            new TwoFacedObject("CSV", FORMAT_CSVHEADER),
            new TwoFacedObject("CSV-IDV", FORMAT_CSVIDV),
            new TwoFacedObject("NetCDF", FORMAT_NETCDF)
        });

        String format     = request.getString(ARG_POINT_FORMAT, FORMAT_HTML);



        List   sortByList = new ArrayList();
        sortByList.add(new TwoFacedObject("Date", COL_DATE));
        for (PointDataMetadata pdm : metadata) {
            if (pdm.isBasic()) {
                continue;
            }
            sortByList.add(new TwoFacedObject(pdm.formatName(),
                    pdm.getColumnName()));
        }

        int          cnt = Misc.getProperty(getProperties(entry), PROP_CNT,
                                            0);

        StringBuffer outputSB = new StringBuffer();
        outputSB.append(HtmlUtils.formTable());
        String sortBy = HtmlUtils.select(ARG_POINT_SORTBY, sortByList,
                                         request.getString(ARG_POINT_SORTBY,
                                             ""));


        outputSB.append(
            HtmlUtils.formEntry(
                msgLabel("Format"),
                HtmlUtils.select(ARG_POINT_FORMAT, formats, format)));


        String totalLabel = HtmlUtils.jsLink(
                                HtmlUtils.onMouseClick("clearPointCount();"),
                                cnt + " " + msg("total"),
                                HtmlUtils.attr(
                                    HtmlUtils.ATTR_ALT,
                                    msg(
                                    "Click to clear count")) + HtmlUtils.attr(
                                        HtmlUtils.ATTR_TITLE,
                                        msg("Click to clear count")));



        outputSB.append(HtmlUtils.formEntry(msgLabel("Max"),
                                            max + HtmlUtils.space(1) + "("
                                            + totalLabel + ")"));
        outputSB.append(
            HtmlUtils.script(
                "function clearPointCount() {obj=util.getDomObject('"
                + ARG_MAX + "');\nif(!obj)return;obj.obj.value='" + cnt
                + "';\n}"));
        List skip = Misc.toList(new Object[] {
            new TwoFacedObject("None", 1),
            new TwoFacedObject("Every other one", 2),
            new TwoFacedObject("Every third", 3),
            new TwoFacedObject("Every fourth", 4),
            new TwoFacedObject("Every fifth", 5),
            new TwoFacedObject("Every tenth", 10),
            new TwoFacedObject("Every twentieth", 20),
            new TwoFacedObject("Every fiftieth", 50),
            new TwoFacedObject("Every hundredth", 100)
        });

        outputSB.append(
            HtmlUtils.formEntry(
                msgLabel("Skip"),
                HtmlUtils.select(
                    ARG_POINT_STRIDE, skip,
                    request.getString(ARG_POINT_STRIDE, "1"))));
        outputSB.append(
            HtmlUtils.formEntry(
                msgLabel("Sort by"),
                sortBy + " "
                + HtmlUtils.checkbox(
                    ARG_POINT_ASCENDING, "true",
                    request.get(ARG_POINT_ASCENDING, true)) + " "
                        + msg("ascending")));

        StringBuffer advOutputSB = new StringBuffer();

        advOutputSB.append(HtmlUtils.formTable());

        advOutputSB.append(
            HtmlUtils.formEntry(
                msgLabel("Time Series"),
                msgLabel("Chart Title")
                + HtmlUtils.input(
                    ARG_POINT_TIMESERIES_TITLE,
                    request.getString(ARG_POINT_TIMESERIES_TITLE, ""),
                    HtmlUtils.SIZE_60)));


        advOutputSB.append(
            HtmlUtils.formEntry(
                msgLabel("Interactive Chart"),
                HtmlUtils.checkbox(
                    ARG_POINT_CHART_USETIMEFORNAME, "true",
                    request.get(
                        ARG_POINT_CHART_USETIMEFORNAME,
                        false)) + HtmlUtils.space(1)
                                + msg("Use time as name")));

        advOutputSB.append(HtmlUtils.formTableClose());


        /*
        outputSB.append(
            HtmlUtils.formEntry(
                msgLabel("Settings"),
                HtmlUtils.makeShowHideBlock(
                    msg("..."), advOutputSB.toString(), false)));
        */

        outputSB.append(HtmlUtils.formTableClose());

        StringBuffer extra = new StringBuffer();
        extra.append(HtmlUtils.formTable());
        List ops = Misc.newList(new TwoFacedObject("&lt;", OP_LT),
                                new TwoFacedObject("&gt;", OP_GT),
                                new TwoFacedObject("=", OP_EQUALS));

        for (PointDataMetadata pdm : metadata) {
            if (pdm.isBasic() && !pdm.getColumnName().equals(COL_ALTITUDE)) {
                continue;
            }
            String suffix    = HtmlUtils.space(1) + pdm.formatUnit();
            String argSuffix = "" + pdm.getColumnNumber();
            String label     = pdm.formatName() + ":";
            if (pdm.isEnumeration()) {
                List values = pdm.enumeratedValues;
                if (values == null) {
                    Statement stmt = getDatabaseManager().select(
                                         SqlUtil.distinct(
                                             pdm.getColumnName()), tableName,
                                                 (Clause) null);
                    values = Misc.toList(
                        SqlUtil.readString(
                            getDatabaseManager().getIterator(stmt), 1));
                    values = new ArrayList(Misc.sort(values));
                    values.add(0, "");
                    pdm.enumeratedValues = values;
                }
                String field =
                    HtmlUtils.select(ARG_POINT_FIELD_VALUE + argSuffix,
                                     values,
                                     request.getString(ARG_POINT_FIELD_VALUE
                                         + argSuffix, ""));
                extra.append(HtmlUtils.formEntry(label, field));
            } else if (pdm.isString()) {
                String field =
                    HtmlUtils.input(ARG_POINT_FIELD_VALUE + argSuffix,
                                    request.getString(ARG_POINT_FIELD_VALUE
                                        + argSuffix, ""), HtmlUtils.SIZE_20);
                String cbx = HtmlUtils.checkbox(ARG_POINT_FIELD_EXACT
                                 + argSuffix, "true",
                                     request.get(ARG_POINT_FIELD_EXACT
                                         + argSuffix, false)) + " "
                                             + msg("Exact");
                extra.append(HtmlUtils.formEntry(label, field + " " + cbx));
            } else {
                String op =
                    HtmlUtils.select(ARG_POINT_FIELD_OP + argSuffix, ops,
                                     request.getString(ARG_POINT_FIELD_OP
                                         + argSuffix, ""));
                String field =
                    HtmlUtils.input(ARG_POINT_FIELD_VALUE + argSuffix,
                                    request.getString(ARG_POINT_FIELD_VALUE
                                        + argSuffix, ""), HtmlUtils.SIZE_10);
                extra.append(HtmlUtils.formEntry(label,
                        op + " " + field + suffix));
            }
        }
        extra.append(HtmlUtils.formTableClose());


        StringBuffer params = new StringBuffer();
        params.append("<ul>");
        params.append(HtmlUtils.checkbox(ARG_POINT_PARAM_ALL, "true",
                                         request.get(ARG_POINT_PARAM_ALL,
                                             false)));
        params.append(HtmlUtils.space(1));
        params.append(msg("All"));
        params.append(HtmlUtils.br());
        List list   = request.get(ARG_POINT_PARAM, new ArrayList());
        int  cbxCnt = 0;

        for (PointDataMetadata pdm : metadata) {
            if (pdm.isObMonth() || pdm.isObHour()) {
                continue;
            }
            if (pdm.isBasic()) {
                //                continue;
            }
            String  value   = "" + pdm.getColumnNumber();

            boolean checked = ((list.size() == 0)
                               ? pdm.isBasic()
                               : list.contains(value));
            String  cbxId   = "cbx" + (cbxCnt++);
            String cbxExtra = HtmlUtils.id(cbxId)
                              + HtmlUtils.attr(HtmlUtils.ATTR_ONCLICK,
                                  HtmlUtils.call("HtmlUtils.checkboxClicked",
                                      HtmlUtils.comma("event",
                                          HtmlUtils.squote(ARG_POINT_PARAM),
                                          HtmlUtils.squote(cbxId))));
            params.append(HtmlUtils.checkbox(ARG_POINT_PARAM, value, checked,
                                             cbxExtra));



            params.append(HtmlUtils.space(1));
            params.append(pdm.formatName());
            params.append(HtmlUtils.br());
        }
        params.append("</ul>");

        //        sb.append(header(msg("Point Data Search")));
        sb.append(
            HtmlUtils.formPost(
                getRepository().URL_ENTRY_SHOW.toString(),
                HtmlUtils.attr("name", "pointsearch")
                + HtmlUtils.id("pointsearch")));

        sb.append(HtmlUtils.submit(LABEL_SEARCH, ARG_POINT_SEARCH));
        sb.append(HtmlUtils.p());

        sb.append(
            HtmlUtils.table(
                HtmlUtils.row(
                    HtmlUtils.cols(
                        msgHeader("Select") + basicSB.toString(),
                        msgHeader("Results")
                        + outputSB.toString()), HtmlUtils.attr(
                            HtmlUtils.ATTR_VALIGN, "top")), HtmlUtils.attr(
                                HtmlUtils.ATTR_WIDTH, "100%")));

        /*        sb.append(HtmlUtils.makeShowHideBlock(msg("Basic"),
                                             basicSB.toString(), true));

        sb.append(HtmlUtils.makeShowHideBlock(msg("Output"),
                                             outputSB.toString(), false));
        */
        sb.append(HtmlUtils.makeShowHideBlock(msg("Advanced Search"),
                extra.toString(), false));

        sb.append(HtmlUtils.makeShowHideBlock(msg("Parameters"),
                params.toString(), false));

        sb.append(HtmlUtils.makeShowHideBlock(msg("Settings"),
                advOutputSB.toString(), false));
        sb.append(HtmlUtils.p());
        sb.append(HtmlUtils.submit(LABEL_SEARCH, ARG_POINT_SEARCH));
        sb.append(HtmlUtils.formClose());





    }





    /**
     * _more_
     *
     * @param tableName _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<PointDataMetadata> getMetadata(String tableName)
            throws Exception {
        return getMetadata(tableName, true);
    }


    /**
     * _more_
     *
     * @param tableName _more_
     * @param checkCache _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<PointDataMetadata> getMetadata(String tableName,
            boolean checkCache)
            throws Exception {

        List<PointDataMetadata> metadata = (checkCache
                                            ? metadataCache.get(tableName)
                                            : null);
        if (metadata == null) {
            if (metadataCache.size() > 100) {
                metadataCache = new Hashtable<String,
                        List<PointDataMetadata>>();
            }
            metadata = new ArrayList<PointDataMetadata>();
            Statement statement =
                getDatabaseManager()
                    .select(Tables.POINTDATAMETADATA.COLUMNS, Tables
                        .POINTDATAMETADATA.NAME, Clause
                        .eq(Tables.POINTDATAMETADATA
                            .COL_TABLENAME, tableName), " ORDER BY "
                                + Tables.POINTDATAMETADATA.COL_COLUMNNUMBER
                                + " ASC ");

            SqlUtil.Iterator iter =
                getDatabaseManager().getIterator(statement);
            ResultSet results;
            while ((results = iter.getNext()) != null) {
                int col = 1;
                metadata.add(new PointDataMetadata(results.getString(col++),
                        results.getString(col++), results.getInt(col++),
                        results.getString(col++), results.getString(col++),
                        results.getString(col++), results.getString(col++)));
            }
            if (checkCache && (metadata.size() > 0)) {
                metadataCache.put(tableName, metadata);
            }
        }

        return metadata;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param statement _more_
     * @param id _more_
     * @param parent _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    public void deleteEntry(Request request, Statement statement, String id,
                            Entry parent, Object[] values)
            throws Exception {
        super.deleteEntry(request, statement, id, parent, values);
        String tableName = getTableName(id);
        deleteFromDatabase(tableName);
    }

    /**
     * _more_
     *
     * @param tableName _more_
     */
    private void deleteFromDatabase(String tableName) {
        StringBuffer sql = new StringBuffer();
        try {
            sql.append("drop table " + tableName);
            getDatabaseManager().executeAndClose(sql.toString(), 1000, 10000);
        } catch (Exception ignore) {}
        try {
            getDatabaseManager().delete(
                Tables.POINTDATAMETADATA.NAME,
                Clause.eq(Tables.POINTDATAMETADATA.COL_TABLENAME, tableName));
        } catch (Exception ignore) {}
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param formBuffer _more_
     * @param entry _more_
     * @param formInfo _more_
     * @param sourceTypeHandler _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addColumnsToEntryForm(Request request, Appendable formBuffer,
                                      Entry parentEntry, Entry entry, FormInfo formInfo,
                                      TypeHandler sourceTypeHandler, HashSet seen)
            throws Exception {
        //noop
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param parent _more_
     * @param file _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected FeatureDatasetPoint getDataset(Entry entry, Entry parent,
                                             File file)
            throws Exception {
        Formatter buf = new Formatter();
        getStorageManager().checkReadFile(file);
        if (file.toString().toLowerCase().endsWith(".csv")) {
            TextPointDataSource dataSource =
                new TextPointDataSource("dummy.csv");
            String contents = getStorageManager().readSystemResource(file);
            FieldImpl field = dataSource.makeObs(contents, ",", null, null,
                                  null, false, false);
            file = getStorageManager().getTmpFile(null, "test.nc");
            PointObFactory.writeToNetcdf(file, field);
        }


        List<Metadata> metadataList =
            getMetadataManager().findMetadata(null, entry,
                new String[] { ContentMetadataHandler.TYPE_ATTACHMENT },
                true);
        if (metadataList == null) {
            if (parent != null) {
                metadataList = getMetadataManager().findMetadata(null,
                        parent,
                        new String[] {
                            ContentMetadataHandler.TYPE_ATTACHMENT }, true,
                                false);
            }
        }

        if (metadataList != null) {
            for (Metadata metadata : metadataList) {
                if (metadata.getAttr1().endsWith(".ncml")) {
                    File templateNcmlFile =
                        new File(IOUtil
                            .joinDir(getRepository().getStorageManager()
                                .getEntryDir(metadata.getEntryId(),
                                             false), metadata.getAttr1()));
                    String ncml = getStorageManager().readSystemResource(
                                      templateNcmlFile);
                    String filePath = file.toString();
                    filePath = filePath.replace("\\", "/");
                    if (filePath.indexOf(":") >= 0) {
                        filePath = IOUtil.getURL(filePath,
                                getClass()).toString();
                    }
                    ncml = ncml.replace("${location}", filePath);
                    File ncmlFile =
                        getStorageManager().getScratchFile(entry.getId()
                            + "_" + metadata.getId() + ".ncml");
                    IOUtil.writeBytes(ncmlFile, ncml.getBytes());
                    file = new File(ncmlFile.toString());

                    break;
                }
            }
        }


        FeatureDatasetPoint pods =
            (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
                ucar.nc2.constants.FeatureType.POINT, file.toString(), null,
                buf);
        if (pods == null) {  // try as ANY_POINT
            pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
                ucar.nc2.constants.FeatureType.ANY_POINT, file.toString(),
                null, buf);
        }

        return pods;
    }

    /**
     * Class PointDataMetadata _more_
     *
     *
     */
    public static class PointDataMetadata {

        /** _more_ */
        public static final String TYPE_STRING = "string";

        /** _more_ */
        public static final String TYPE_DOUBLE = "double";

        /** _more_ */
        public static final String TYPE_INT = "int";

        /** _more_ */
        public static final String TYPE_DATE = "date";

        /** _more_ */
        public static final String TYPE_ENUMERATION = "enumeration";


        /** _more_ */
        private String tableName;

        /** _more_ */
        private String columnName;

        /** _more_ */
        private String shortName;

        /** _more_ */
        private String longName;

        /** _more_ */
        private int columnNumber;

        /** _more_ */
        private String varType;

        /** _more_ */
        private String unit;

        /** _more_ */
        private List enumeratedValues;

        /**
         * _more_
         *
         * @param tableName _more_
         * @param columnName _more_
         * @param type _more_
         * @param unit _more_
         * @param column _more_
         */
        public PointDataMetadata(String tableName, String columnName,
                                 int column, String unit, String type) {
            this(tableName, columnName, column, columnName, columnName, unit,
                 type);
        }


        /**
         * _more_
         *
         * @param tableName _more_
         * @param columnName _more_
         * @param shortName _more_
         * @param longName _more_
         * @param type _more_
         * @param unit _more_
         * @param column _more_
         */
        public PointDataMetadata(String tableName, String columnName,
                                 int column, String shortName,
                                 String longName, String unit, String type) {
            this.tableName = tableName;
            this.setColumnName(columnName);
            this.shortName = shortName;
            this.longName  = longName;
            this.setColumnNumber(column);
            this.unit    = unit;
            this.varType = type;

        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getDatabaseType() {
            if (varType.equals(TYPE_DOUBLE)) {
                return "ramadda.double";
            } else if (varType.equals(TYPE_INT)) {
                return "int";
            } else if (varType.equals(TYPE_DATE)) {
                return "ramadda.datetime";
            }

            return "varchar(1000)";
        }


        /**
         * _more_
         *
         * @param c _more_
         *
         * @return _more_
         */
        public boolean isColumn(String c) {
            return Misc.equals(getColumnName(), c);
        }



        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isObMonth() {
            return isColumn(COL_MONTH);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isObHour() {
            return isColumn(COL_HOUR);
        }


        /**
         * _more_
         *
         * @param list _more_
         * @param col _more_
         *
         * @return _more_
         */
        public static boolean contains(List<PointDataMetadata> list,
                                       String col) {
            for (PointDataMetadata pdm : list) {
                if (pdm.isColumn(col)) {
                    return true;
                }
            }

            return false;
        }

        /**
         * _more_
         *
         * @param list _more_
         * @param col _more_
         *
         * @return _more_
         */
        public static PointDataMetadata find(List<PointDataMetadata> list,
                                             String col) {
            for (PointDataMetadata pdm : list) {
                if (pdm.isColumn(col)) {
                    return pdm;
                }
            }

            return null;
        }




        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            //            return columnName + " " + shortName + "  "
            //                   + varType;
            return getColumnName();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String formatName() {
            return shortName.replace("_", " ");
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String varName() {
            String n = shortName.replace(" ", "_");

            return n;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public String formatUnit() {
            if ((unit == null) || (unit.length() == 0)) {
                return "";
            }

            return "[" + unit + "]";

        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean hasUnit() {
            if ((unit == null) || (unit.length() == 0)) {
                return false;
            }

            return true;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Object[] getValues() {
            return new Object[] {
                tableName, getColumnName(), Integer.valueOf(getColumnNumber()),
                shortName, longName, unit, varType
            };
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isString() {
            return varType.equals(TYPE_STRING)
                   || varType.equals(TYPE_ENUMERATION);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isDate() {
            return varType.equals(TYPE_DATE);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isEnumeration() {
            return varType.equals(TYPE_ENUMERATION);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isNumeric() {
            return varType.equals(TYPE_DOUBLE) || varType.equals(TYPE_INT);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isBasic() {
            return getColumnNumber() < NUM_BASIC_COLUMNS;
        }


        /**
         * _more_
         *
         * @param columnNumber _more_
         */
        public void setColumnNumber(int columnNumber) {
            this.columnNumber = columnNumber;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public int getColumnNumber() {
            return columnNumber;
        }


        /**
         * _more_
         *
         * @param columnName _more_
         */
        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public String getColumnName() {
            return columnName;
        }

    }


    /**
     * Class PointData _more_
     *
     *
     */
    public static class PointData {

        /** _more_ */
        int id;

        /** _more_ */
        double lat;

        /** _more_ */
        double lon;

        /** _more_ */
        double alt;

        /** _more_ */
        Date date;

        /** _more_ */
        int month;

        /** _more_ */
        int hour;

        /** _more_ */
        Hashtable values = new Hashtable();

        /**
         * _more_
         *
         *
         * @param id _more_
         * @param date _more_
         * @param lat _more_
         * @param lon _more_
         * @param alt _more_
         * @param month _more_
         * @param hour _more_
         */
        public PointData(int id, Date date, double lon, double lat,
                         double alt, int month, int hour) {
            this.id    = id;
            this.lat   = lat;
            this.lon   = lon;
            this.alt   = alt;
            this.date  = date;
            this.month = month;
            this.hour  = hour;
        }

        /**
         * _more_
         *
         * @param pdm _more_
         * @param v _more_
         */
        public void setValue(PointDataMetadata pdm, Object v) {
            values.put(pdm.getColumnName(), v);
        }

        /**
         * _more_
         *
         * @param pdm _more_
         *
         * @return _more_
         */
        public Object getValue(PointDataMetadata pdm) {
            return getValue(pdm.getColumnName());
        }

        /**
         * _more_
         *
         * @param col _more_
         *
         * @return _more_
         */
        public Object getValue(String col) {
            if (col == null) {
                return null;
            }
            if (col.equals(COL_ID)) {
                return  Integer.valueOf(id);
            }
            if (col.equals(COL_LATITUDE)) {
                return Double.valueOf(lat);
            }
            if (col.equals(COL_LONGITUDE)) {
                return Double.valueOf(lon);
            }
            if (col.equals(COL_ALTITUDE)) {
                return Double.valueOf(alt);
            }
            if (col.equals(COL_DATE)) {
                return date;
            }
            if (col.equals(COL_MONTH)) {
                return Double.valueOf(month);
            }
            if (col.equals(COL_HOUR)) {
                return Double.valueOf(hour);
            }

            return values.get(col);
        }


    }


}
