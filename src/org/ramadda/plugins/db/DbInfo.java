/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.db;


import org.ramadda.data.point.text.*;

import org.ramadda.data.record.*;
import org.ramadda.data.services.PointOutputHandler;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;



import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.*;

import org.ramadda.repository.output.CalendarOutputHandler;
import org.ramadda.repository.output.MapOutputHandler;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.output.RssOutputHandler;
import org.ramadda.repository.output.WikiConstants;
import org.ramadda.repository.type.*;
import org.ramadda.util.geo.Bounds;
import org.ramadda.util.FormInfo;
import org.ramadda.util.GoogleChart;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.JQuery;
import org.ramadda.util.JsonUtil;

import org.ramadda.util.geo.KmlUtil;
import org.ramadda.util.RssUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.XlsUtil;
import org.ramadda.util.XmlUtils;
import org.ramadda.util.sql.*;


import org.w3c.dom.*;
import org.w3c.dom.*;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlUtil;



import java.io.*;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import java.sql.Statement;

import java.text.DecimalFormat;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import java.util.TimeZone;


/**
 *
 */


public class DbInfo {

    /** _more_ */
    private DbTypeHandler typeHandler;

    /** _more_ */
    private int numberOfInternalColumns;

    /** _more_ */
    private boolean hasLocation = false;

    /** _more_ */
    private boolean hasEmail = false;

    /** _more_ */
    private boolean hasDate = false;

    /** _more_ */
    private boolean hasNumber = false;


    /** _more_ */
    private Hashtable<String, Column> columnMap = new Hashtable<String,
                                                      Column>();

    /** _more_ */
    private List<Column> numberColumns = new ArrayList<Column>();

    /** _more_ */
    private List<Column> dateColumns = new ArrayList<Column>();

    /** _more_ */
    private Column dateColumn;

    /** _more_ */
    private List<Column> categoryColumns = new ArrayList<Column>();

    /** _more_ */
    private Column mapCategoryColumn = null;

    /** _more_ */
    private List<Column> enumColumns = new ArrayList<Column>();

    /** _more_ */
    private List<Column> allColumns;

    /** _more_ */
    private List<Column> sortedColumns;

    /** _more_ */
    private String[] namesArray;


    /** _more_ */
    protected List<Column> columnsToUse;

    /** _more_ */
    private List<Column> columnsToUseSorted;

    /** _more_ */
    private Column keyColumn;

    /** _more_ */
    private Column dfltSortColumn;

    /** _more_ */
    private boolean dfltSortAsc = false;

    /** _more_ */
    private String labelColumnNames;

    /** _more_ */
    private List<Column> labelColumns;


    /** _more_ */
    private Column descColumn;

    /** _more_ */
    private Column urlColumn;

    /** _more_ */
    private Column latLonColumn;

    /** _more_ */
    private Column latColumn;

    /** _more_ */
    private Column lonColumn;

    /** _more_ */
    private Column polygonColumn;

    /**
     * _more_
     *
     * @param typeHandler _more_
     * @param numberOfInternalColumns _more_
     */
    public DbInfo(DbTypeHandler typeHandler, int numberOfInternalColumns) {
        this.typeHandler             = typeHandler;
        this.numberOfInternalColumns = numberOfInternalColumns;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Repository getRepository() {
        return typeHandler.getRepository();
    }

    /**
     * _more_
     *
     * @param allColumns _more_
     * @param labelColumnNames _more_
     *
     * @throws Exception _more_
     */
    public void initColumns(List<Column> allColumns, String labelColumnNames)
            throws Exception {

        this.allColumns       = allColumns;
        this.labelColumnNames = labelColumnNames;
        //        List<String> columnNames =  new ArrayList<String>(tableHandler.getColumnNames());
        //        namesArray = StringUtil.listToStringArray(columnNames);
        columnMap = new Hashtable<String, Column>();

        int cnt = 0;
        numberColumns   = new ArrayList<Column>();
        categoryColumns = new ArrayList<Column>();
        enumColumns     = new ArrayList<Column>();
        dateColumns     = new ArrayList<Column>();
        hasDate         = false;
        labelColumns    = null;
        descColumn      = null;
        urlColumn       = null;
        keyColumn       = null;
        dfltSortColumn  = null;
        columnsToUse    = new ArrayList<Column>();
        for (int colIdx = 0; colIdx < allColumns.size(); colIdx++) {
            if (colIdx > numberOfInternalColumns) {
                columnsToUse.add(allColumns.get(colIdx));
            }
        }

        for (Column column : columnsToUse) {
            boolean doStats = Misc.equals(column.getProperty("dostats"),
                                          "true");
            if (doStats) {
                column.setDoStats(true);
            }
            if (Misc.equals(column.getProperty("isKey"), "true")) {
                keyColumn = column;
            }


            if (Misc.equals(column.getProperty("islabel"), "true")) {
                if (labelColumns == null) {
                    labelColumns = new ArrayList<Column>();
                }
                labelColumns.add(column);
            }
            if ((descColumn == null)
                    && column.getType().equals(Column.DATATYPE_STRING)
                    && (column.getRows() > 1)) {
                descColumn = column;
            }
            if (Misc.equals(column.getProperty("defaultsort"), "true")) {
                dfltSortColumn = column;
                dfltSortAsc = Misc.equals(column.getProperty("ascending"),
                                          "true");
            }

            cnt++;
            if (column.getType().equals(Column.DATATYPE_EMAIL)) {
                hasEmail = true;
            }
            if (column.getType().equals(Column.DATATYPE_URL)) {
                urlColumn = column;
            }

            if (column.getName().equals("latitude")) {
                latColumn   = column;
                hasLocation = (latColumn != null) && (lonColumn != null);
            } else if (column.getName().equals("longitude")) {
                lonColumn   = column;
                hasLocation = (latColumn != null) && (lonColumn != null);
            }

            if (column.getName().equals("polygon")) {
                polygonColumn = column;
            }
            if (column.getType().equals(Column.DATATYPE_LATLONBBOX)
                    || column.getType().equals(Column.DATATYPE_LATLON)) {
                hasLocation  = true;
                latLonColumn = column;
            }
            if (column.isDate()) {
                hasDate = true;
                dateColumns.add(column);
                if (dateColumn == null) {
                    dateColumn = column;
                }
            }
            if (column.isNumeric()) {
                numberColumns.add(column);
                hasNumber = true;
            }
            if (column.isEnumeration()) {
                enumColumns.add(column);
            }

            if (column.isEnumeration()
                    && Misc.equals(column.getProperty("iscategory"),
                                   "true")) {
                if ((mapCategoryColumn == null)
                        && Misc.equals(column.getProperty("formap"),
                                       "true")) {
                    mapCategoryColumn = column;
                }
                categoryColumns.add(column);
            }
            columnMap.put(column.getName(), column);
            for (String name : column.getColumnNames()) {
                columnMap.put(name, column);
            }
        }

        if (labelColumnNames.length() > 0) {
            for (String label :
                    StringUtil.split(labelColumnNames, ",", true, true)) {
                Column column = columnMap.get(label);
                if (column != null) {
                    if (labelColumns == null) {
                        labelColumns = new ArrayList<Column>();
                    }
                    labelColumns.add(column);
                }
            }
        }


        if ((mapCategoryColumn == null) && (categoryColumns.size() > 0)) {
            mapCategoryColumn = categoryColumns.get(0);
        }


    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Column getKeyColumn() {
        return keyColumn;
    }



    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public Column getColumn(String name) {
        return columnMap.get(name);
    }


    /**
     * Set the ColumnsToUse property.
     *
     * @param value The new value for ColumnsToUse
     */
    public void setColumnsToUse(List<Column> value) {
        columnsToUse = value;
    }

    /**
     * Get the ColumnsToUse property.
     *
     * @return The ColumnsToUse
     */
    public List<Column> getColumnsToUse() {
        return columnsToUse;
    }

    public List<Column> getRealColumnsToUse() {	
	List<Column> tmp = new ArrayList<Column>();
	for(Column c: columnsToUse) {
	    if(!c.isSynthetic()) {
		tmp.add(c);
	    }
	}
	return tmp;
    }

    /**
     * _more_
     *
     * @param sorted _more_
     *
     * @return _more_
     */
    public List<Column> getColumnsToUse(boolean sorted) {
        if (sorted) {
            if (columnsToUseSorted == null) {
                columnsToUseSorted = Column.sortColumns(columnsToUse);
            }

            return columnsToUseSorted;
        }

        return columnsToUse;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<Column> getColumns() {
        return allColumns;
    }

    /**
     * _more_
     *
     * @param sorted _more_
     *
     * @return _more_
     */
    public List<Column> getColumns(boolean sorted) {
        if (sorted) {
            if (sortedColumns == null) {
                sortedColumns = Column.sortColumns(allColumns);
            }

            return sortedColumns;
        }

        return allColumns;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<Column> getEnumColumns() {
        return enumColumns;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<Column> getLabelColumns() {
        return labelColumns;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<Column> getNumberColumns() {
        return numberColumns;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<Column> getCategoryColumns() {
        return categoryColumns;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<Column> getDateColumns() {
        return dateColumns;
    }


    /**
     * Set the LatColumn property.
     *
     * @param value The new value for LatColumn
     */
    public void setLatColumn(Column value) {
        latColumn = value;
    }

    /**
     * Set the DfltSortAsc property.
     *
     * @param value The new value for DfltSortAsc
     */
    public void setDfltSortAsc(boolean value) {
        dfltSortAsc = value;
    }

    /**
     * Get the DfltSortAsc property.
     *
     * @return The DfltSortAsc
     */
    public boolean getDfltSortAsc() {
        return dfltSortAsc;
    }


    /**
     * Set the DescColumn property.
     *
     * @param value The new value for DescColumn
     */
    public void setDescColumn(Column value) {
        descColumn = value;
    }

    /**
     * Get the DescColumn property.
     *
     * @return The DescColumn
     */
    public Column getDescColumn() {
        return descColumn;
    }

    /**
     * Set the UrlColumn property.
     *
     * @param value The new value for UrlColumn
     */
    public void setUrlColumn(Column value) {
        urlColumn = value;
    }

    /**
     * Get the UrlColumn property.
     *
     * @return The UrlColumn
     */
    public Column getUrlColumn() {
        return urlColumn;
    }




    /**
     * Set the DfltSortColumn property.
     *
     * @param value The new value for DfltSortColumn
     */
    public void setDfltSortColumn(Column value) {
        dfltSortColumn = value;
    }

    /**
     * Get the DfltSortColumn property.
     *
     * @return The DfltSortColumn
     */
    public Column getDfltSortColumn() {
        return dfltSortColumn;
    }



    /**
     * Get the LatColumn property.
     *
     * @return The LatColumn
     */
    public Column getLatColumn() {
        return latColumn;
    }

    /**
     * Set the LonColumn property.
     *
     * @param value The new value for LonColumn
     */
    public void setLonColumn(Column value) {
        lonColumn = value;
    }

    /**
     * Get the LonColumn property.
     *
     * @return The LonColumn
     */
    public Column getLonColumn() {
        return lonColumn;
    }

    /**
     *
     * @return _more_
     */
    public Column getPolygonColumn() {
        return polygonColumn;
    }

    /**
     * Set the LatLonColumn property.
     *
     * @param value The new value for LatLonColumn
     */
    public void setLatLonColumn(Column value) {
        latLonColumn = value;
    }

    /**
     * Get the LatLonColumn property.
     *
     * @return The LatLonColumn
     */
    public Column getLatLonColumn() {
        return latLonColumn;
    }



    /**
     * Set the MapCategoryColumn property.
     *
     * @param value The new value for MapCategoryColumn
     */
    public void setMapCategoryColumn(Column value) {
        mapCategoryColumn = value;
    }

    /**
     * Get the MapCategoryColumn property.
     *
     * @return The MapCategoryColumn
     */
    public Column getMapCategoryColumn() {
        return mapCategoryColumn;
    }

    /**
     * Set the DateColumn property.
     *
     * @param value The new value for DateColumn
     */
    public void setDateColumn(Column value) {
        dateColumn = value;
    }

    /**
     * Get the DateColumn property.
     *
     * @return The DateColumn
     */
    public Column getDateColumn() {
        return dateColumn;
    }



    /**
     * Set the HasEmail property.
     *
     * @param value The new value for HasEmail
     */
    public void setHasEmail(boolean value) {
        hasEmail = value;
    }

    /**
     * Get the HasEmail property.
     *
     * @return The HasEmail
     */
    public boolean getHasEmail() {
        return hasEmail;
    }

    /**
     * Set the HasDate property.
     *
     * @param value The new value for HasDate
     */
    public void setHasDate(boolean value) {
        hasDate = value;
    }

    /**
     * Get the HasDate property.
     *
     * @return The HasDate
     */
    public boolean getHasDate() {
        return hasDate;
    }

    /**
     * Set the HasNumber property.
     *
     * @param value The new value for HasNumber
     */
    public void setHasNumber(boolean value) {
        hasNumber = value;
    }

    /**
     * Get the HasNumber property.
     *
     * @return The HasNumber
     */
    public boolean getHasNumber() {
        return hasNumber;
    }

    /**
     * Set the HasLocation property.
     *
     * @param value The new value for HasLocation
     */
    public void setHasLocation(boolean value) {
        hasLocation = value;
    }

    /**
     * Get the HasLocation property.
     *
     * @return The HasLocation
     */
    public boolean getHasLocation() {
        return hasLocation;
    }




}
