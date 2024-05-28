/**
* Copyright (c) 2008-2015 Geode Systems LLC
* This Software is licensed under the Geode Systems RAMADDA License available in the source distribution in the file 
* ramadda_license.txt. The above copyright notice shall be included in all copies or substantial portions of the Software.
*/

/**
 * Copyright (c) 2008-2015 Geode Systems LLC
 * This Software is licensed under the Geode Systems RAMADDA License available in the source distribution in the file
 * ramadda_license.txt. The above copyright notice shall be included in all copies or substantial portions of the Software.
 */
package gov.noaa.esrl.psd.repository.data;


import org.ramadda.geodata.cdmdata.PointDatabaseTypeHandler;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.RepositoryUtil;
import org.ramadda.repository.Request;
import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.Element;

import ucar.unidata.geoloc.StationImpl;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.File;

import java.sql.Connection;
import java.sql.PreparedStatement;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;


/**
 * Class description
 *
 *
 * @version        $version$, Thu, Apr 2, '15
 * @author         Enter your name here...
 */
@SuppressWarnings("unchecked")
public class UshcnPointDatabaseTypeHandler extends PointDatabaseTypeHandler {

    /** _more_ */
    private static final String BLANK_DELIM = "\\s+";

    /** _more_ */
    private static final String MISSING_VALUE = "-99.99";

    /** _more_ */
    private static final String MISSING_VALUE1 = "-99.99";

    /** _more_ */
    private static final String MISSING_VALUE2 = "-9999";

    /** _more_ */
    private static final String COL_STATION = "obStation";

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public UshcnPointDatabaseTypeHandler(Repository repository,
                                         Element entryNode)
            throws Exception {
        super(repository, entryNode);
        // TODO Auto-generated constructor stub
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
        String sourceFile = IOUtil.getFileTail(dataFile.toString());
        List<PointDataMetadata> metadata  =
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

        String dataContents =
            getStorageManager().readSystemResource(dataFile);
        System.err.println("number of characters = " + dataContents.length());
        List<String> dataLines = StringUtil.split(dataContents, "\n", true,
                                     true);

        System.err.println("number of lines = " + dataLines.size());

        // set the header params based on the file name
        String param = null;
        String unit  = null;
        if (sourceFile.indexOf("ppt") > 0) {  // precip
            param = "Precipitation";
            unit  = "mm";
        } else {                              // some sort of temp
            unit = "degC";
            if (sourceFile.indexOf("tmx") > 0) {
                param = "Max_Temperature";
            } else if (sourceFile.indexOf("tmn") > 0) {
                param = "Min_Temperature";
            } else {
                param = "Temperature";
            }
        }
        boolean daily = true;
        if (sourceFile.indexOf("dly") > 0) {
            param = "Daily_" + param;
        } else if (sourceFile.indexOf("mth") > 0) {
            param = "Monthly_" + param;
            daily = false;
        }

        metadata.add(new PointDataMetadata(tableName, COL_STATION,
                                           metadata.size(), "Station",
                                           "Station", "",
                                           PointDataMetadata.TYPE_STRING));

        String colName = SqlUtil.cleanName(param).trim();
        colName = "ob_" + colName;
        metadata.add(new PointDataMetadata(tableName, colName,
                                           metadata.size(), param, param,
                                           unit,
                                           PointDataMetadata.TYPE_DOUBLE));


        createDatabase(entry, metadata, connection);
        insertData(request,entry, metadata, dataLines, connection, true, daily);
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param metadata _more_
     * @param fdp _more_
     * @param dataLines _more_
     * @param connection _more_
     * @param newEntry _more_
     * @param daily _more_
     *
     * @throws Exception _more_
     */
    private void insertData(Request request,Entry entry, List<PointDataMetadata> metadata,
                            List<String> dataLines, Connection connection,
                            boolean newEntry, boolean daily)
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
        boolean        didone     = false;

        Hashtable      properties = getProperties(entry);
        int            baseId     = Misc.getProperty(properties, PROP_ID, 0);
        int            totalCnt   = Misc.getProperty(properties, PROP_CNT, 0);
        long           t1         = System.currentTimeMillis();
        StationImpl    station    = null;
        double         dayValue   = 0,
                       lat        = 0,
                       lon        = 0,
                       alt        = 0;
        List<DayValue> dayValues  = null;
        boolean        packed     = false;
        int            wordsize   = 0;
        for (int i = 0; i < dataLines.size(); i++) {
            String line = dataLines.get(i);
            //if (line.indexOf(" ") > 6) {
            if (line.length() < 72) {
                station = parseStation(line);
                System.err.println("processing data for "
                                   + station.getName());
                lat = station.getLatitude();
                lon = station.getLongitude();
                alt = station.getAltitude();
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

                continue;
            }
            //line = line.replaceAll("-9999"," "+MISSING_VALUE);
            // parse lines
            dayValues = new ArrayList<DayValue>();
            //String[] tokens = line.split(BLANK_DELIM);
            packed   = (line.indexOf(".") < 0);
            wordsize = (packed)
                       ? 5
                       : 7;
            int start = 0;
            try {
                if (daily) {
                    String year    = line.substring(0, 4).trim();
                    String month   = line.substring(4, 7).trim();
                    String days    = line.substring(7, 10).trim();
                    int    numDays = (int) Misc.parseDouble(days);
                    for (int day = 0; day < numDays; day++) {
                        StringBuilder buf = new StringBuilder(year);
                        buf.append("-");
                        buf.append(StringUtil.padLeft(month, 2, "0"));  // month
                        buf.append("-");
                        buf.append(StringUtil.padLeft("" + (day + 1), 2,
                                "0"));  // day
                        start = 10 + day * wordsize;
                        String dayString = line.substring(start,
                                               start + wordsize).trim();
                        if (dayString.equals(MISSING_VALUE)
                                || dayString.equals(MISSING_VALUE2)) {
                            dayValue = Double.NaN;
                        } else {
                            dayValue = Misc.parseDouble(dayString);
                            if (packed) {
                                dayValue /= 10;
                            }
                        }
                        dayValues.add(new DayValue(buf.toString(), dayValue));
                    }
                } else {
                    String year = line.substring(0, 4).trim();
                    for (int month = 0; month < 12; month++) {
                        StringBuilder buf = new StringBuilder(year);
                        buf.append("-");
                        buf.append(StringUtil.padLeft("" + (month + 1), 2,
                                "0"));  // month
                        buf.append("-01");
                        start = 4 + month * wordsize;
                        String dayString = line.substring(start,
                                               start + wordsize).trim();
                        if (dayString.equals(MISSING_VALUE)
                                || dayString.equals(MISSING_VALUE2)) {
                            dayValue = Double.NaN;
                        } else {
                            dayValue = Misc.parseDouble(dayString);
                            if (packed) {
                                dayValue /= 10;
                            }
                        }
                        dayValues.add(new DayValue(buf.toString(), dayValue));
                    }

                }
            } catch (Exception excp) {
                System.err.println("got " + excp.getClass().getName()
                                   + " on line: " + line);

                continue;
            }
            for (DayValue dv : dayValues) {

                Date time    = dv.getDate();

                long tmpTime = time.getTime();
                if (tmpTime < minTime) {
                    minTime = tmpTime;
                }
                if (tmpTime > maxTime) {
                    maxTime = tmpTime;
                }

                calendar.setTime(time);
                boolean hadGoodNumericValue = false;
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
                        value =
                            Integer.valueOf(calendar.get(GregorianCalendar.HOUR));
                    } else if (COL_MONTH.equals(pdm.getColumnName())) {
                        value = Integer.valueOf(
                            calendar.get(GregorianCalendar.MONTH));
                    } else if (COL_STATION.equals(pdm.getColumnName())) {
                        value = station.getName();
                    } else {
                        double d = dv.getValue();
                        hadGoodNumericValue = (d == d);
                        value               = Double.valueOf(checkWriteValue(d));
                    }
                    values[pdm.getColumnNumber()] = value;
                }
                if ( !hadGoodNumericValue) {
                    continue;
                }
                totalCnt++;
                getDatabaseManager().setValues(insertStmt, values);
                insertStmt.addBatch();
                batchCnt++;
                if (batchCnt > 1000) {
                    insertStmt.executeBatch();
                    batchCnt = 0;
                }
                if (((cnt++) % 5000) == 0) {
                    System.err.println("added " + cnt + " observations");
                }
            }
        }


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

    /** _more_ */
    private static int[] idx = { 0, 9, 34, 43, 53 };


    /**
     * _more_
     *
     * @param line _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private StationImpl parseStation(String line) throws Exception {
        String id   = line.substring(idx[0], idx[1]).trim();
        String name = line.substring(idx[1], idx[2]).trim();
        double lat  = Misc.parseDouble(line.substring(idx[2], idx[3]).trim());
        double lon  = Misc.parseDouble(line.substring(idx[3], idx[4]).trim());
        double alt = Misc.parseDouble(line.substring(idx[4],
                         idx[4] + 7).trim());
        StationImpl s = new StationImpl(name, name, id, lat, lon, alt);

        return s;
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Apr 2, '15
     * @author         Enter your name here...
     */
    public static class DayValue {

        /** _more_ */
        private String day;

        /** _more_ */
        private double value;

        /** _more_ */
        private Date date;

        /** _more_ */
        private static SimpleDateFormat formatter =
            new SimpleDateFormat("yyyy-MM-dd");

        /**
         * _more_
         *
         * @param day _more_
         * @param value _more_
         */
        public DayValue(String day, double value) {
            this.day   = day;
            this.value = value;
            try {
                date = formatter.parse(day);
            } catch (ParseException pe) {}
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public double getValue() {
            return value;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getDayString() {
            return day;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Date getDate() {
            return date;
        }
    }


}
