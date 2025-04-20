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

public class DbInfo {
    private DbTypeHandler typeHandler;
    private int numberOfInternalColumns;
    private boolean hasLocation = false;
    private boolean hasEmail = false;
    private boolean hasDate = false;
    private boolean hasNumber = false;
    private Hashtable<String, Column> columnMap = new Hashtable<String,
                                                      Column>();

    private List<Column> numberColumns = new ArrayList<Column>();
    private List<Column> dateColumns = new ArrayList<Column>();
    private Column dateColumn;
    private List<Column> categoryColumns = new ArrayList<Column>();
    private Column mapCategoryColumn = null;
    private List<Column> enumColumns = new ArrayList<Column>();
    private List<Column> allColumns;
    private List<Column> sortedColumns;
    private String[] namesArray;
    protected List<Column> columnsToUse;
    private List<Column> columnsToUseSorted;
    private Column keyColumn;
    private Column dfltSortColumn;
    private boolean dfltSortAsc = false;
    private String labelColumnNames;
    private List<Column> labelColumns;
    private Column descColumn;
    private Column urlColumn;
    private Column latLonColumn;
    private Column latColumn;
    private Column lonColumn;
    private Column polygonColumn;

    public DbInfo(DbTypeHandler typeHandler, int numberOfInternalColumns) {
        this.typeHandler             = typeHandler;
        this.numberOfInternalColumns = numberOfInternalColumns;
    }

    public Repository getRepository() {
        return typeHandler.getRepository();
    }

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

    public Column getKeyColumn() {
        return keyColumn;
    }

    public Column getColumn(String name) {
        return columnMap.get(name);
    }

    public void setColumnsToUse(List<Column> value) {
        columnsToUse = value;
    }

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

    public List<Column> getColumnsToUse(boolean sorted) {
        if (sorted) {
            if (columnsToUseSorted == null) {
                columnsToUseSorted = Column.sortColumns(columnsToUse);
            }

            return columnsToUseSorted;
        }

        return columnsToUse;
    }

    public List<Column> getColumns() {
        return allColumns;
    }

    public List<Column> getColumns(boolean sorted) {
        if (sorted) {
            if (sortedColumns == null) {
                sortedColumns = Column.sortColumns(allColumns);
            }

            return sortedColumns;
        }

        return allColumns;
    }

    public List<Column> getEnumColumns() {
        return enumColumns;
    }

    public List<Column> getLabelColumns() {
        return labelColumns;
    }

    public List<Column> getNumberColumns() {
        return numberColumns;
    }

    public List<Column> getCategoryColumns() {
        return categoryColumns;
    }

    public List<Column> getDateColumns() {
        return dateColumns;
    }

    public void setLatColumn(Column value) {
        latColumn = value;
    }

    public void setDfltSortAsc(boolean value) {
        dfltSortAsc = value;
    }

    public boolean getDfltSortAsc() {
        return dfltSortAsc;
    }

    public void setDescColumn(Column value) {
        descColumn = value;
    }

    public Column getDescColumn() {
        return descColumn;
    }

    public void setUrlColumn(Column value) {
        urlColumn = value;
    }

    public Column getUrlColumn() {
        return urlColumn;
    }

    public void setDfltSortColumn(Column value) {
        dfltSortColumn = value;
    }

    public Column getDfltSortColumn() {
        return dfltSortColumn;
    }

    public Column getLatColumn() {
        return latColumn;
    }

    public void setLonColumn(Column value) {
        lonColumn = value;
    }

    public Column getLonColumn() {
        return lonColumn;
    }

    public Column getPolygonColumn() {
        return polygonColumn;
    }

    public void setLatLonColumn(Column value) {
        latLonColumn = value;
    }

    public Column getLatLonColumn() {
        return latLonColumn;
    }

    public void setMapCategoryColumn(Column value) {
        mapCategoryColumn = value;
    }

    public Column getMapCategoryColumn() {
        return mapCategoryColumn;
    }

    public void setDateColumn(Column value) {
        dateColumn = value;
    }

    public Column getDateColumn() {
        return dateColumn;
    }

    public void setHasEmail(boolean value) {
        hasEmail = value;
    }

    public boolean getHasEmail() {
        return hasEmail;
    }

    public void setHasDate(boolean value) {
        hasDate = value;
    }

    public boolean getHasDate() {
        return hasDate;
    }

    public void setHasNumber(boolean value) {
        hasNumber = value;
    }

    public boolean getHasNumber() {
        return hasNumber;
    }

    public void setHasLocation(boolean value) {
        hasLocation = value;
    }

    public boolean getHasLocation() {
        return hasLocation;
    }

}
