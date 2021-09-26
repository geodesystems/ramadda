/*
* Copyright (c) 2008-2021 Geode Systems LLC
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

package org.ramadda.repository.type;


import org.json.*;

import org.ramadda.repository.Constants;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.RepositoryBase;
import org.ramadda.repository.RepositoryUtil;
import org.ramadda.repository.Request;
import org.ramadda.repository.database.DatabaseManager;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;
import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.Element;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


/**
 */

public class Column implements DataTypes, Constants, Cloneable {

    /** _more_          */
    public static final HtmlUtils HU = null;


    /** _more_ */
    static int xcnt;

    /** _more_ */
    public String myid = "column-" + (xcnt++);


    /** _more_ */
    public static final String ARG_EDIT_PREFIX = "edit_";

    /** _more_ */
    public static final String ARG_SEARCH_PREFIX = "search.";

    /** _more_ */
    public static final String OUTPUT_HTML = "html";

    /** _more_ */
    public static final String OUTPUT_CSV = "csv";

    /** _more_ */
    private SimpleDateFormat dateTimeFormat =
        new SimpleDateFormat("yyyy-MM-dd HH:mm Z");

    /** _more_ */
    private SimpleDateFormat fullDateTimeFormat =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    /** _more_ */
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /** _more_ */
    private SimpleDateFormat dateParser = null;

    /** _more_ */
    private boolean importDateIsEpoch = false;

    /** _more_ */
    public static final String EXPR_EQUALS = "=";

    /** _more_ */
    public static final String EXPR_LE = "<=";

    /** _more_          */
    public static final String EXPR_LT = "<";

    /** _more_          */
    public static final String EXPR_GT = ">";

    /** _more_ */
    public static final String EXPR_GE = ">=";

    /** _more_ */
    public static final String EXPR_BETWEEN = "between";

    /** _more_ */
    public static final List EXPR_ITEMS =
        Misc.newList(new TwoFacedObject("", ""),
                     new TwoFacedObject("=", EXPR_EQUALS),
                     new TwoFacedObject("<=", EXPR_LE),
                     new TwoFacedObject(">=", EXPR_GE),
                     new TwoFacedObject("between", EXPR_BETWEEN));

    /** _more_ */
    public static final String EXPR_PATTERN = EXPR_EQUALS + "|" + EXPR_LE
                                              + "|" + EXPR_GE + "|"
                                              + EXPR_BETWEEN;

    /** _more_ */
    public static final String SEARCHTYPE_TEXT = "text";

    /** _more_ */
    public static final String SEARCHTYPE_SELECT = "select";


    /** _more_ */
    public static final String TAG_COLUMN = "column";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_FORMAT = "format";

    /** _more_ */
    public static final String ATTR_CHANGETYPE = "changetype";

    /** _more_ */
    public static final String ATTR_SHOWINFORM = "showinform";

    /** _more_ */
    public static final String ATTR_GROUP = "group";

    /** _more_ */
    public static final String ATTR_UNIT = "unit";

    /** _more_ */
    public static final String ATTR_OLDNAMES = "oldnames";

    /** _more_ */
    public static final String ATTR_SUFFIX = "suffix";



    /** _more_ */
    public static final String ATTR_LOOKUPDB = "lookupdb";

    /** _more_ */
    public static final String ATTR_HELP = "help";

    /** _more_ */
    public static final String ATTR_SORT_ORDER = "sortOrder";

    /** _more_ */
    public static final String ATTR_PROPERTIES = "properties";

    /** _more_ */
    public static final String ATTR_LABEL = "label";

    /** _more_ */
    public static final String ATTR_DESCRIPTION = "description";

    /** _more_ */
    public static final String ATTR_TYPE = "type";

    /** _more_ */
    public static final String ATTR_ISINDEX = "isindex";

    /** _more_ */
    public static final String ATTR_ISCATEGORY = "iscategory";

    /** _more_ */
    public static final String ATTR_CANSEARCH = "cansearch";

    /** _more_          */
    public static final String ATTR_CANSORT = "cansort";

    /** _more_ */
    public static final String ATTR_SEARCHROWS = "searchrows";

    /** _more_ */
    public static final String ATTR_CANSEARCHTEXT = "cansearchtext";

    /** _more_ */
    public static final String ATTR_ADVANCED = "advanced";

    /** _more_ */
    public static final String ATTR_CANLIST = "canlist";

    /** _more_ */
    public static final String ATTR_CANDISPLAY = "candisplay";

    /** _more_ */
    public static final String ATTR_EDITABLE = "editable";

    /** _more_ */
    public static final String ATTR_VALUES = "values";

    /** _more_ */
    public static final String ATTR_DEFAULT = "default";

    /** _more_ */
    public static final String ATTR_SIZE = "size";

    /** _more_ */
    public static final String ATTR_MIN = "min";

    /** _more_ */
    public static final String ATTR_MAX = "max";

    /** _more_ */
    public static final String ATTR_REQUIRED = "required";

    /** _more_ */
    public static final String ATTR_ROWS = "rows";

    /** _more_ */
    public static final String ATTR_COLUMNS = "columns";

    /** _more_ */
    public static final String ATTR_SEARCHTYPE = "searchtype";

    /** _more_ */
    public static final String ATTR_SHOWINHTML = "showinhtml";


    /** _more_ */
    public static final String ATTR_SHOWLABEL = "showlabel";

    /** _more_ */
    public static final String ATTR_CANEXPORT = "canexport";




    /** Lat/Lon format */
    private DecimalFormat latLonFormat = new DecimalFormat("##0.00");


    /** _more_ */
    private DecimalFormat numberFormat;


    /** _more_ */
    private DecimalFormat intFormat = new DecimalFormat("#0");


    /** _more_ */
    private TypeHandler typeHandler;

    /** _more_ */
    private Element xmlElement;

    /** _more_ */
    private String name;

    /** _more_ */
    private String fullName;

    /** _more_ */
    private String group;

    /** _more_ */
    private List oldNames;

    /** _more_ */
    private String label;

    /** _more_ */
    private String description;

    /** _more_ */
    private String htmlTemplate;

    /** _more_ */
    private String displayTemplate;

    /** _more_ */
    private String displayPatternFrom;

    /** _more_ */
    private String displayPatternTo;

    /** _more_ */
    private String type;

    /** _more_          */
    private String unit;

    /** _more_ */
    private boolean changeType = false;

    /** _more_ */
    private boolean showEmpty = true;

    /** _more_          */
    private boolean addNot = false;

    /** _more_          */
    private boolean addFileToSearch = false;


    /** _more_          */
    private int numberOfSearchWidgets = 1;

    /** _more_ */
    private String suffix;

    /** _more_ */
    private String help;

    /** _more_          */
    private String placeholder;

    /** _more_          */
    private String placeholderMin;

    /** _more_          */
    private String placeholderMax;


    /** _more_ */
    private int sortOrder = 1000;

    /** _more_ */
    private String searchType = SEARCHTYPE_TEXT;

    /** _more_ */
    private boolean isIndex;

    /** _more_ */
    private boolean doStats = false;

    /** _more_ */
    private boolean isWiki;

    /** _more_ */
    private boolean isCategory;

    /** _more_ */
    private boolean canSearch;

    /** _more_          */
    private boolean canSort;

    /** _more_ */
    private int searchRows;

    /** _more_ */
    private boolean canSearchText;


    /** _more_ */
    private boolean advancedSearch;

    /** _more_ */
    private boolean editable;

    /** _more_ */
    private boolean canList;

    /** _more_ */
    private boolean canDisplay;

    /** _more_ */
    private List<TwoFacedObject> enumValues;

    /** _more_          */
    private List<TwoFacedObject> jsonValues;

    /** _more_ */
    private Hashtable<String, String> enumMap = new Hashtable<String,
                                                    String>();

    /** _more_ */
    private List<Display> displays = new ArrayList<Display>();


    /** _more_ */
    private String alias;



    /** _more_ */
    private String dflt;

    /** _more_ */
    private String databaseDflt;

    /** _more_ */
    private double databaseDfltNum = Double.NaN;

    /** _more_ */
    private String databaseDefaultPropertyName;


    /** _more_ */
    private double dfltDouble = Double.NaN;

    /** _more_ */
    private int size = 200;

    /** _more_ */
    private double min = Double.NaN;

    /** _more_ */
    private double max = Double.NaN;

    /** _more_ */
    private boolean required = false;

    /** _more_ */
    private int rows = 1;


    /** _more_ */
    private int columns = 40;

    /** _more_ */
    private String propertiesFile;

    /** _more_ */
    private int columnIndex;

    /** _more_ */
    private int offset;


    /** _more_ */
    private boolean canShow = true;


    /** _more_ */
    private boolean showLabel = true;

    /** _more_ */
    private boolean canExport = true;


    /** _more_ */
    private boolean showInForm = true;

    /** _more_          */
    private String lookupDB;

    /** _more_ */
    private Hashtable<String, String> properties = new Hashtable<String,
                                                       String>();

    /**
     * _more_
     *
     * @param typeHandler _more_
     * @param name _more_
     * @param type _more_
     * @param offset _more_
     *
     * @throws Exception _more_
     */
    public Column(TypeHandler typeHandler, String name, String type,
                  int offset)
            throws Exception {
        this.typeHandler = typeHandler;
        this.name        = name;
        this.type        = type;
        this.offset      = offset;
    }


    /**
     * _more_
     *
     * @param typeHandler _more_
     * @param element _more_
     * @param offset _more_
     *
     * @throws Exception _more_
     */
    public Column(TypeHandler typeHandler, Element element, int offset)
            throws Exception {

        this.xmlElement  = element;
        this.typeHandler = typeHandler;
        this.offset      = offset;

        name             = XmlUtil.getAttribute(element, ATTR_NAME);
        unit = XmlUtil.getAttribute(element, ATTR_UNIT, (String) null);
        group = XmlUtil.getAttribute(element, ATTR_GROUP, (String) null);
        oldNames = Utils.split(XmlUtil.getAttribute(element, ATTR_OLDNAMES,
                ""), ",", true, true);
        suffix = Utils.getAttributeOrTag(element, ATTR_SUFFIX, "");
        help = Utils.getAttributeOrTag(element, ATTR_HELP, (String) null);
        placeholder = Utils.getAttributeOrTag(element, "placeholder",
                (String) null);
        placeholderMin = Utils.getAttributeOrTag(element, "placeholderMin",
                placeholder);
        placeholderMax = Utils.getAttributeOrTag(element, "placeholderMax",
                placeholder);

        sortOrder = Utils.getAttributeOrTag(element, ATTR_SORT_ORDER, 1000);
        //The suffix might have the ${root} macro in it
        if (typeHandler != null) {
            suffix =
                typeHandler.getRepository().getPageHandler().applyBaseMacros(
                    suffix);
        }

        label = Utils.getAttributeOrTag(element, ATTR_LABEL, name);
        searchType = XmlUtil.getAttribute(element, ATTR_SEARCHTYPE,
                                          searchType);
        propertiesFile = XmlUtil.getAttribute(element, ATTR_PROPERTIES,
                (String) null);

        String dttmFormat = XmlUtil.getAttribute(element, ATTR_FORMAT,
                                (String) null);
        if (dttmFormat != null) {
            if (dttmFormat.equals("epoch")) {
                importDateIsEpoch = true;
            } else {
                dateParser = new SimpleDateFormat(dttmFormat);
            }
        }



        List displayNodes = XmlUtil.findChildren(element, "display");
        for (int i = 0; i < displayNodes.size(); i++) {
            Element node = (Element) displayNodes.get(i);
            displays.add(new Display(node));
        }


        description = getAttributeOrTag(element, ATTR_DESCRIPTION, label);

        displayPatternFrom = Utils.getAttributeOrTag(element,
                "displayPatternFrom", (String) null);
        displayPatternTo = Utils.getAttributeOrTag(element,
                "displayPatternTo", (String) null);

        displayTemplate = Utils.getAttributeOrTag(element, "displayTemplate",
                (String) null);

        htmlTemplate = Utils.getAttributeOrTag(element, "htmlTemplate",
                (String) null);

        type = Utils.getAttributeOrTag(element, ATTR_TYPE, DATATYPE_STRING);
        changeType = getAttributeOrTag(element, ATTR_CHANGETYPE, false);

        showEmpty  = getAttributeOrTag(element, "showempty", true);
        addNot     = getAttributeOrTag(element, "addnot", addNot);
        addFileToSearch = getAttributeOrTag(element, "addfiletosearch",
                                            addFileToSearch);
        dflt    = getAttributeOrTag(element, ATTR_DEFAULT, "").trim();
        doStats = getAttributeOrTag(element, "dostats", doStats);

        databaseDflt = getAttributeOrTag(element, "databaseDefault",
                                         (String) null);
        alias      = getAttributeOrTag(element, "alias", (String) null);
        isIndex    = getAttributeOrTag(element, ATTR_ISINDEX, false);
        isWiki     = getAttributeOrTag(element, "iswiki", false);
        isCategory = getAttributeOrTag(element, ATTR_ISCATEGORY, false);
        canSearch  = getAttributeOrTag(element, ATTR_CANSEARCH, false);
        canSort    = getAttributeOrTag(element, ATTR_CANSORT, false);
        searchRows = getAttributeOrTag(element, ATTR_SEARCHROWS, 1);
        canSearchText = getAttributeOrTag(element, ATTR_CANSEARCHTEXT,
                                          canSearch);
        advancedSearch = getAttributeOrTag(element, ATTR_ADVANCED, false);
        editable       = getAttributeOrTag(element, ATTR_EDITABLE, true);
        showInForm = getAttributeOrTag(element, ATTR_SHOWINFORM, showInForm);
        canShow        = getAttributeOrTag(element, ATTR_SHOWINHTML, canShow);
        showLabel      = getAttributeOrTag(element, ATTR_SHOWLABEL,
                                           showLabel);

        canExport      = getAttributeOrTag(element, ATTR_CANEXPORT,
                                           canExport);
        canList        = getAttributeOrTag(element, ATTR_CANLIST, true);
        canDisplay     = getAttributeOrTag(element, ATTR_CANDISPLAY, true);
        size           = getAttributeOrTag(element, ATTR_SIZE, size);
        min            = getAttributeOrTag(element, ATTR_MIN, min);
        max            = getAttributeOrTag(element, ATTR_MAX, max);
        required       = getAttributeOrTag(element, ATTR_REQUIRED, required);
        rows           = getAttributeOrTag(element, ATTR_ROWS, rows);
        columns        = getAttributeOrTag(element, ATTR_COLUMNS, columns);

        lookupDB = getAttributeOrTag(element, ATTR_LOOKUPDB, (String) null);

        String tmp = getAttributeOrTag(element, "numberFormat", null);
        if (tmp != null) {
            numberFormat = new DecimalFormat(tmp);
        }

        List propNodes = XmlUtil.findChildren(element, "property");
        for (int i = 0; i < propNodes.size(); i++) {
            Element propNode = (Element) propNodes.get(i);
            properties.put(XmlUtil.getAttribute(propNode, "name"),
                           XmlUtil.getAttribute(propNode, "value"));
        }


        numberOfSearchWidgets = XmlUtil.getAttribute(element,
                "numberOfSearchWidgets", numberOfSearchWidgets);

        if (isEnumeration()) {
            String valueString = XmlUtil.getAttribute(element, ATTR_VALUES,
                                     (String) null);
            if (valueString != null) {
                setEnums(valueString, ",");
            } else {
                valueString = XmlUtil.getGrandChildText(element, ATTR_VALUES,
                        (String) null);
                if (valueString != null) {
                    setEnums(valueString, "\n");
                }
            }
            if (enumValues == null) {
                enumValues = new ArrayList<TwoFacedObject>();
            }
        }

        if (isNumeric() && Utils.stringDefined(dflt)) {
            dfltDouble = Double.parseDouble(dflt);
        }

        dateTimeFormat.setTimeZone(RepositoryBase.TIMEZONE_UTC);
    }

    /**
     * _more_
     *
     * @param attr _more_
     * @param v _more_
     *
     * @return _more_
     */
    public String getDisplayAttribute(String attr, Object v) {
        Display d = getDisplay(v);
        if (d == null) {
            return null;
        }

        return d.getAttribute(attr);
    }



    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    public Display getDisplay(Object v) {
        if (displays.size() == 0) {
            return null;
        }
        if (v == null) {
            return null;
        }
        if (isNumeric()) {
            double value = new Double(v.toString());
            for (Display d : displays) {
                if ( !Double.isNaN(d.min) && !Double.isNaN(d.max)) {
                    if ((value >= d.min) && (value <= d.max)) {
                        return d;
                    }
                }
            }
        } else {
            for (Display d : displays) {
                if (d.value.equals(v)) {
                    return d;
                }
            }
        }

        return null;
    }


    /**
     * _more_
     *
     * @param v
     *
     * @return _more_
     */
    public String decorate(String v) {

        Display d = getDisplay(v);
        if (d == null) {
            return v;
        }

        //      if(true) throw new IllegalArgumentException("");
        return d.decorate(v);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws CloneNotSupportedException _more_
     */
    public Column cloneColumn() throws CloneNotSupportedException {
        Column column = (Column) this.clone();

        return column;
    }

    /**
     * _more_
     *
     * @param columns _more_
     *
     * @return _more_
     */
    public static List<String> getNames(List<Column> columns) {
        List<String> names = new ArrayList<String>();
        for (Column c : columns) {
            names.addAll(c.getColumnNames());
        }

        return names;
    }


    /**
     * _more_
     *
     * @param columns _more_
     *
     * @return _more_
     */
    public static Object[] makeValueArray(List<Column> columns) {
        int size = 0;
        for (Column c : columns) {
            if (c.isType(DATATYPE_LATLON)) {
                size += 2;
            } else if (c.isType(DATATYPE_LATLONBBOX)) {
                size += 4;
            } else {
                size++;
            }
        }

        return new Object[size];
    }




    /**
     * _more_
     *
     * @param columns _more_
     *
     * @return _more_
     */
    public static List<Column> sortColumns(List<Column> columns) {
        List<Column> tmp = new ArrayList<Column>();
        tmp.addAll(columns);
        Comparator comp = new Comparator() {
            public int compare(Object o1, Object o2) {
                Column c1 = (Column) o1;
                Column c2 = (Column) o2;
                if (c1.sortOrder < c2.sortOrder) {
                    return -1;
                }
                if (c1.sortOrder > c2.sortOrder) {
                    return 1;
                }

                return 0;
            }
        };
        Collections.sort(tmp, comp);

        return tmp;
    }

    /**
     * _more_
     *
     * @param valueString _more_
     * @param delimiter _more_
     *
     * @throws Exception _more_
     */
    private void setEnums(String valueString, String delimiter)
            throws Exception {
        List<String> tmp = typeHandler.getColumnEnumerationProperties(this,
                               valueString, delimiter);
        enumValues = new ArrayList<TwoFacedObject>();
        for (String tok : tmp) {
            if (tok.startsWith("#")) {
                continue;
            }
            String label = tok;
            String value = tok;
            if (tok.indexOf(":") >= 0) {
                List<String> toks = Utils.splitUpTo(tok, ":", 2);
                value = toks.get(0);
                label = toks.get(1);
            } else if (tok.indexOf("=") >= 0) {
                List<String> toks = Utils.splitUpTo(tok, "=", 2);

                value = toks.get(0);
                label = toks.get(1);
            }
            enumValues.add(new TwoFacedObject(label, value));
            enumMap.put(value, label);
        }
    }



    /**
     * _more_
     *
     * @param t _more_
     *
     * @return _more_
     */
    public boolean isType(String t) {
        return type.equals(t);
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public String getProperty(String key) {
        return getProperty(key, null);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getProperty(String key, String dflt) {
        String prop = properties.get(key);
        if ((prop == null) && (xmlElement != null)) {
            prop = XmlUtil.getAttribute(xmlElement, key, (String) null);
        }
        if (prop == null) {
            return dflt;
        }

        return prop;
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void putProperty(String key, String value) {
        properties.put(key, value);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Hashtable getProperties() {
        return properties;
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public String msg(String s) {
        return typeHandler.msg(s);
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public String msgLabel(String s) {
        return typeHandler.msgLabel(s);
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
     * @return _more_
     */
    public DatabaseManager getDatabaseManager() {
        return getRepository().getDatabaseManager();
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getJson(Request request) throws Exception {
        List<String> col = new ArrayList<String>();
        col.add("name");
        col.add(Json.quote(getName()));
        col.add("label");
        col.add(Json.quote(getLabel()));
        col.add("type");
        col.add(Json.quote(getType()));
        col.add("namespace");
        col.add(Json.quote(getTableName()));
        col.add("suffix");
        col.add(Json.quote(getSuffix()));
        col.add("canshow");
        col.add("" + getCanShow());
        col.add("cansearch");
        col.add("" + getCanSearch());
        col.add("cansort");
        col.add("" + getCanSort());
        col.add("searchrows");
        col.add("" + getSearchRows());
        col.add("cansearchtext");
        col.add("" + getCanSearchText());
        col.add("canlist");
        col.add("" + getCanList());
        col.add("candisplay");
        col.add("" + getCanDisplay());
        if (isEnumeration()) {
            List<String>         enums  = new ArrayList<String>();
            List<TwoFacedObject> values = null;
            if (isType(DATATYPE_ENUMERATION)) {
                values = enumValues;
            }
            if ((values == null) || (values.size() == 0)) {
                values = typeHandler.getEnumValues(request, this, null);
            }
            if (values != null) {
                for (TwoFacedObject tfo : values) {
                    enums.add(
                        Json.map(
                            "value", Json.quote(tfo.getId().toString()),
                            "label", Json.quote(tfo.getLabel().toString())));
                }
            }
            col.add("values");
            col.add(Json.list(enums));
        }

        return Json.map(col);


    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isNumeric() {
        return isInteger() || isDouble();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isInteger() {
        return isType(DATATYPE_INT);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isPrivate() {
        return isType(DATATYPE_PASSWORD);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isBoolean() {
        return isType(DATATYPE_BOOLEAN);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEnumeration() {
        return isType(DATATYPE_ENUMERATION)
               || isType(DATATYPE_ENUMERATIONPLUS);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isDate() {
        return isType(DATATYPE_DATETIME) || isType(DATATYPE_DATE);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isDouble() {
        return isType(DATATYPE_DOUBLE) || isType(DATATYPE_PERCENTAGE);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isString() {
        return isType(DATATYPE_STRING) || isEnumeration()
               || isType(DATATYPE_CLOB) || isType(DATATYPE_JSONLIST)
               || isType(DATATYPE_ENTRY) || isType(DATATYPE_EMAIL)
               || isType(DATATYPE_WIKI) || isType(DATATYPE_URL)
               || isType(DATATYPE_LIST);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * _more_
     *
     * @param values _more_
     *
     * @return _more_
     */
    public Object getObject(Object[] values) {
        if (values == null) {
            return null;
        }
        int idx = getOffset();
        if (idx >= values.length) {
            return null;
        }
        if (values[idx] == null) {
            return null;
        }

        return values[idx];
    }


    /**
     * _more_
     *
     * @param values _more_
     *
     * @return _more_
     */
    public String getString(Object[] values) {
        if (values == null) {
            return null;
        }
        int idx = getOffset();
        if (idx >= values.length) {
            return null;
        }
        if (values[idx] == null) {
            return null;
        }
        if (isType(DATATYPE_PASSWORD)) {
            return null;
        }

        return values[idx].toString();
    }


    /**
     * _more_
     *
     * @param values _more_
     *
     * @return _more_
     */
    public double getDouble(Object[] values) {
        Object o = getObject(values);
        if (o == null) {
            return Double.NaN;
        }

        return ((Double) o).doubleValue();
    }



    /**
     * _more_
     *
     * @param values _more_
     * @param idx _more_
     *
     * @return _more_
     */
    public String toString(Object[] values, int idx) {
        if (values == null) {
            return ((dflt != null)
                    ? dflt
                    : "");
        }
        if (values[idx] == null) {
            return ((dflt != null)
                    ? dflt
                    : "");
        }

        return values[idx].toString();
    }


    /**
     * _more_
     *
     * @param values _more_
     * @param idx _more_
     * @param raw _more_
     *
     * @return _more_
     */
    private String toLatLonString(Object[] values, int idx, boolean raw) {
        if (values == null) {
            return ((dflt != null)
                    ? dflt
                    : "NA");
        }
        if (values[idx] == null) {
            return ((dflt != null)
                    ? dflt
                    : "NA");
        }
        if ( !latLonOk(values[idx])) {
            return "NA";
        }
        double d = ((Double) values[idx]).doubleValue();
        if (raw) {
            return "" + d;
        }

        return latLonFormat.format(d);
    }

    /**
     * _more_
     *
     * @param values _more_
     * @param idx _more_
     *
     * @return _more_
     */
    private boolean toBoolean(Object[] values, int idx) {
        if (values[idx] == null) {
            if (Utils.stringDefined(dflt)) {
                return new Boolean(dflt).booleanValue();
            }

            return true;
        }

        return ((Boolean) values[idx]).booleanValue();
    }


    /**
     * _more_
     *
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param output _more_
     * @param values _more_
     * @param raw _more_
     *
     * @throws Exception _more_
     */
    public void formatValue(Request request, Entry entry, Appendable sb,
                            String output, Object[] values, boolean raw)
            throws Exception {
        formatValue(request, entry, sb, output, values, null, raw);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param result _more_
     * @param output _more_
     * @param values _more_
     * @param sdf _more_
     * @param raw _more_
     *
     * @throws Exception _more_
     */
    public void formatValue(Request request, Entry entry, Appendable result,
                            String output, Object[] values,
                            SimpleDateFormat sdf, boolean raw)
            throws Exception {

        Appendable sb  = new StringBuilder();
        boolean    csv = Misc.equals(output, OUTPUT_CSV);
        if (csv) {
            raw = true;
        }
        String delimiter = csv
                           ? "|"
                           : ",";
        if (isType(DATATYPE_LATLON)) {
            sb.append(toLatLonString(values, offset, raw));
            sb.append(delimiter);
            sb.append(toLatLonString(values, offset + 1, raw));
        } else if (isType(DATATYPE_LATLONBBOX)) {
            sb.append(toLatLonString(values, offset, raw));
            sb.append(delimiter);
            sb.append(toLatLonString(values, offset + 1, raw));
            sb.append(delimiter);
            sb.append(toLatLonString(values, offset + 2, raw));
            sb.append(delimiter);
            sb.append(toLatLonString(values, offset + 3, raw));
        } else if (isType(DATATYPE_PERCENTAGE)) {
            if (raw) {
                sb.append(toString(values, offset));
            } else {
                //                System.err.println("offset:" + offset +" values:");
                //                Misc.printArray("", values);
                double percent = (Double) values[offset];
                sb.append((int) (percent * 100) + "");
            }

        } else if (isType(DATATYPE_DOUBLE)) {
            if (raw) {
                sb.append(toString(values, offset));
            } else {
                Double v = (Double) values[offset];
                if (v == null) {
                    return;
                }
                if ((v == dfltDouble) && !getShowEmpty()) {
                    return;
                }
                String s = ((numberFormat != null)
                            ? numberFormat.format(v)
                            : Utils.formatComma(v));
                sb.append(s);
            }
        } else if (isType(DATATYPE_DATETIME)) {
            String s;
            if (sdf != null) {
                s = sdf.format((Date) values[offset]);
            } else {
                s = dateTimeFormat.format((Date) values[offset]);
            }
            sb.append(s);
        } else if (isType(DATATYPE_DATE)) {
            if (values[offset] == null) {
                sb.append("null");
            } else {
                String s;
                if (sdf != null) {
                    s = sdf.format((Date) values[offset]);
                } else {
                    s = dateFormat.format((Date) values[offset]);
                }
                sb.append(s);
            }
        } else if (isType(DATATYPE_ENTRY)) {
            String entryId  = toString(values, offset);
            Entry  theEntry = null;
            if (Utils.stringDefined(entryId)) {
                try {
                    theEntry =
                        getRepository().getEntryManager().getEntry(null,
                            entryId);
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
            }
            if (raw) {
                sb.append(entryId);
            } else {
                if (theEntry != null) {
                    try {
                        String link =
                            getRepository().getEntryManager().getAjaxLink(
                                request, theEntry,
                                theEntry.getName()).toString();
                        sb.append(link);
                    } catch (Exception exc) {
                        throw new RuntimeException(exc);
                    }

                } else {
                    sb.append("---");
                }

            }
        } else if (isType(DATATYPE_EMAIL)) {
            String s = toString(values, offset);
            if (raw) {
                sb.append(s);
            } else {
                sb.append("<a href=\"mailto:" + s + "\">" + s + "</a>");
            }
        } else if (isType(DATATYPE_JSONLIST)) {
            String s = toString(values, offset);
            if (raw) {
                sb.append(s);
            } else {
                if (s.length() > 0) {
                    sb.append("<table border =0 style=\"border:0px;\">");
                    JSONArray array = new JSONArray(s);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj   = array.getJSONObject(i);
                        String[]   names = JSONObject.getNames(obj);
                        Arrays.sort(names);
                        for (String name : names) {
                            if (name.equals("id")) {
                                continue;
                            }
                            sb.append(
                                "<tr><td align=right  style=\"   font-weight: bold;border:0px;\">");
                            sb.append(name);
                            sb.append(":");
                            sb.append("</td><td style=\"border:0px;\">");
                            sb.append(obj.optString(name));
                            sb.append("</td><tr>");
                        }
                        //                        sb.append("<tr><td colspan=2  style=\"border:0px;\">&nbsp;</td></tr>");
                    }
                    sb.append("</table>");
                }
            }
        } else if (isType(DATATYPE_URL)) {
            String       s    = toString(values, offset);
            List<String> urls = Utils.split(s, "\n");
            if (raw) {
                s = StringUtil.join(delimiter, urls);
                sb.append(s);
            } else {
                int cnt = 0;
                for (String url : urls) {
                    if (cnt > 0) {
                        sb.append("<br>");
                    }
                    cnt++;
                    sb.append("<a target=_blank href=\"" + url + "\">" + url
                              + "</a>");
                }
            }


        } else {
            String s = toString(values, offset);
            if (raw) {
                if (s.indexOf(",") >= 0) {
                    s = "\"" + s + "\"";
                }
            } else {
                if (isType(DATATYPE_LIST)) {
                    s = s.replaceAll("\n", "<br>");
                }
            }
            if (s.length() == 0) {
                if ( !getShowEmpty()) {
                    return;
                }
            }

            if (rows > 1) {
                s = getRepository().getWikiManager().wikifyEntry(request,
                        entry, s, false, null, null);
            } else if (isEnumeration() && !raw) {
                String label = getEnumLabel(s);
                if (label != null) {
                    s = label;
                }
            }
            sb.append(s);
        }

        String s = sb.toString();
        s = typeHandler.decorateValue(null, entry, this, s);
        if (htmlTemplate != null) {
            result.append(htmlTemplate.replace("${value}", s));
        } else {
            result.append(s);
        }

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Hashtable<String, String> getEnumTable() {
        return enumMap;
    }


    /**
     * Gets the string to display for enumeration values
     *
     * @param value _more_
     *
     * @return _more_
     */
    public String getEnumLabel(String value) {
        return getEnumLabel(value, true);
    }

    /**
     * _more_
     *
     * @param value _more_
     * @param forDisplay _more_
     *
     * @return _more_
     */
    public String getEnumLabel(String value, boolean forDisplay) {
        String label = getEnumLabelInner(value);
        if ( !forDisplay && (label.length() == 0)) {
            label = "&lt;blank&gt;";
        }

        return label;
    }

    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     */
    private String getEnumLabelInner(String value) {
        if (value == null) {
            return "null";
        }
        //enumMap is from the values field in the types.xml column specifier
        String label = enumMap.get(value);
        if (label == null) {
            //If we don't have a label in the enumMap then see if we have a displayTemplate (from types.xml)
            //<column name="status"  type="enumerationplus"  values="active:Active,inactive:Inactive" label="Status" cansearch="true" 
            //displayTemplate="Type:${value}" />
            if (displayTemplate != null) {
                return displayTemplate.replace("${value}", value);
            }
            //If we don't have a displayTemplate then check for a displayPattern. This is in types.xml as, e.g.:
            // <column name="status"  type="enumerationplus"  values="active:Active,inactive:Inactive" label="Status" cansearch="true" 
            // displayPatternFrom=".*([0-9]+).*" displayPatternTo="Should be a number:$1"/>
            if ((displayPatternFrom != null) && (displayPatternTo != null)) {
                //only do this if it matches the pattern
                if (value.matches(displayPatternFrom)) {
                    return value.replaceAll(displayPatternFrom,
                                            displayPatternTo);
                }
            }

            return value;
        }

        return label;
    }



    /**
     * _more_
     *
     * @param label _more_
     *
     * @return _more_
     */
    public String getEnumValue(String label) {
        if (label == null) {
            return null;
        }
        for (Enumeration keys = enumMap.keys(); keys.hasMoreElements(); ) {
            String value = (String) keys.nextElement();
            String l     = (String) enumMap.get(value);
            if (l.equals(label)) {
                return value;
            }
        }

        return label;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getOffset() {
        return offset;
    }

    /**
     * _more_
     *
     * @param o _more_
     */
    public void setOffset(int o) {
        offset = o;
    }

    /**
     * _more_
     *
     * @param statement _more_
     * @param values _more_
     * @param statementIdx _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected int setValues(PreparedStatement statement, Object[] values,
                            int statementIdx)
            throws Exception {

        try {
            return setValuesInner(statement, values, statementIdx);
        } catch (Exception exc) {
            String msg = "Error setting value. Column:" + getName()
                         + " value:" + values[offset];
            System.err.println(msg);

            throw new RuntimeException(msg, exc);
        }
    }

    /**
     * _more_
     *
     * @param statement _more_
     * @param values _more_
     * @param statementIdx _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private int setValuesInner(PreparedStatement statement, Object[] values,
                               int statementIdx)
            throws Exception {

        if (offset >= values.length) {
            return 0;
        }
        if (isType(DATATYPE_INT)) {
            if (values[offset] != null) {
                statement.setInt(statementIdx,
                                 ((Integer) values[offset]).intValue());
            } else {
                int value = 0;
                if (Utils.stringDefined(dflt)) {
                    value = Integer.parseInt(dflt);
                }
                statement.setInt(statementIdx, value);
            }
            statementIdx++;
        } else if (isDouble()) {
            if (values[offset] != null) {
                double value = ((Double) values[offset]).doubleValue();
                if ( !Double.isNaN(value)) {
                    if ( !Double.isNaN(min)) {
                        if (value < min) {
                            throw new IllegalArgumentException(
                                "Invalid value for " + getLabel() + " "
                                + value + " < " + min);
                        }
                    }
                    if ( !Double.isNaN(max)) {
                        if (value > max) {
                            throw new IllegalArgumentException(
                                "Invalid value for " + getLabel() + " "
                                + value + " > " + max);
                        }
                    }
                }
                statement.setDouble(statementIdx, value);
            } else {
                //                double value = Double.NaN;
                double value = 0;
                if (Utils.stringDefined(dflt)) {
                    value = Double.parseDouble(dflt);
                }
                statement.setDouble(statementIdx, value);
            }
            statementIdx++;
        } else if (isType(DATATYPE_BOOLEAN)) {
            if (values[offset] != null) {
                boolean v = ((Boolean) values[offset]).booleanValue();
                statement.setInt(statementIdx, (v
                        ? 1
                        : 0));
            } else {
                int value = 0;
                if (Utils.stringDefined(dflt)) {
                    value = dflt.equals("true")
                            ? 1
                            : 0;
                }
                statement.setInt(statementIdx, value);
            }
            statementIdx++;
        } else if (isDate()) {
            Date dttm = (Date) values[offset];
            getDatabaseManager().setDate(statement, statementIdx, dttm);
            statementIdx++;
        } else if (isType(DATATYPE_LATLON)) {
            if (values[offset] != null) {
                double lat = ((Double) values[offset]).doubleValue();
                double lon = ((Double) values[offset + 1]).doubleValue();
                if (Double.isNaN(lat)) {
                    lat = Entry.NONGEO;
                }
                if (Double.isNaN(lon)) {
                    lon = Entry.NONGEO;
                }
                statement.setDouble(statementIdx, lat);
                statement.setDouble(statementIdx + 1, lon);
            } else {
                statement.setDouble(statementIdx, Entry.NONGEO);
                statement.setDouble(statementIdx + 1, Entry.NONGEO);
            }
            statementIdx += 2;
        } else if (isType(DATATYPE_LATLONBBOX)) {
            for (int i = 0; i < 4; i++) {
                if (values[offset + i] != null) {
                    statement.setDouble(
                        statementIdx++,
                        ((Double) values[offset + i]).doubleValue());
                } else {
                    statement.setDouble(statementIdx++, Entry.NONGEO);
                }
            }
        } else if (isType(DATATYPE_PASSWORD)) {
            if (values[offset] != null) {
                String value = new String(Utils.encodeBase64(toString(values,
                                   offset)).getBytes());
                statement.setString(statementIdx, value);
            } else {
                statement.setString(statementIdx, null);
            }
            statementIdx++;
        } else {
            //            System.err.println("\tset statement:" + offset + " " + values[offset]);
            if (values[offset] != null) {
                String value = toString(values, offset);
                //Check the value
                if (required) {
                    if (value.trim().length() == 0) {
                        throw new IllegalArgumentException("Value "
                                + getLabel() + " is required");
                    }
                }
                getDatabaseManager().setString(statement, statementIdx,
                        getName(), value, size);
            } else {
                statement.setString(statementIdx, null);
            }
            statementIdx++;
        }

        return statementIdx;

    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param values _more_
     * @param node _more_
     *
     * @throws Exception _more_
     */
    public void addToEntryNode(Entry entry, Object[] values, Element node)
            throws Exception {
        if (values[offset] == null) {
            return;
        }
        String stringValue = null;
        //Don't export the password
        if (isType(DATATYPE_PASSWORD)) {
            return;
        }
        if (isType(DATATYPE_LATLON)) {
            stringValue = values[offset] + ";" + values[offset + 1];
        } else if (isType(DATATYPE_LATLONBBOX)) {
            stringValue = values[offset] + ";" + values[offset + 1] + ";"
                          + values[offset + 2] + ";" + values[offset + 3];
        } else if (isDate()) {
            fullDateTimeFormat.setTimeZone(RepositoryBase.TIMEZONE_UTC);
            stringValue = fullDateTimeFormat.format((Date) values[offset]);
        } else {
            stringValue = values[offset].toString();
        }
        boolean encode    = true;
        Element valueNode = XmlUtil.create(node.getOwnerDocument(), name);
        node.appendChild(valueNode);
        valueNode.setAttribute("encoded", "" + encode);
        valueNode.appendChild(XmlUtil.makeCDataNode(node.getOwnerDocument(),
                stringValue, encode));
    }


    /**
     * _more_
     *
     *
     * @param entry _more_
     * @param results _more_
     * @param values _more_
     * @param valueIdx _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public int readValues(Entry entry, ResultSet results, Object[] values,
                          int valueIdx)
            throws Exception {
        if (isType(DATATYPE_INT)) {
            int value = results.getInt(valueIdx);
            if (results.wasNull()) {
                if (databaseDflt != null) {
                    if (Double.isNaN(databaseDfltNum)) {
                        databaseDfltNum = Double.parseDouble(databaseDflt);
                    }
                    value = (int) databaseDfltNum;
                }
            }
            values[offset] = new Integer(value);
            valueIdx++;
        } else if (isType(DATATYPE_PERCENTAGE)) {
            values[offset] = new Double(results.getDouble(valueIdx));
            valueIdx++;
        } else if (isDouble()) {
            double value = results.getDouble(valueIdx);
            if (results.wasNull()) {
                //                System.err.println(this  +" was null:" + value);
                if (databaseDflt != null) {
                    if (Double.isNaN(databaseDfltNum)) {
                        databaseDfltNum = Double.parseDouble(databaseDflt);
                    }
                    value = databaseDfltNum;
                }
            }
            //            System.err.println("col: " + this +" " + results.wasNull() +" value:" + value);
            //            System.err.println(this  +" value=" + value);
            values[offset] = new Double(value);
            valueIdx++;
        } else if (isType(DATATYPE_BOOLEAN)) {
            String value = results.getString(valueIdx);
            if ((value == null) || (value.length() == 0)) {
                value = dflt;
            }
            if (value == null) {
                value = "0";
            }

            values[offset] = new Boolean(value.equals("true")
                                         || value.equals("1"));
            valueIdx++;
        } else if (isDate()) {
            values[offset] = getDatabaseManager().getTimestamp(results,
                    valueIdx);
            valueIdx++;
        } else if (isType(DATATYPE_LATLON)) {
            values[offset] = new Double(results.getDouble(valueIdx));
            valueIdx++;
            values[offset + 1] = new Double(results.getDouble(valueIdx));
            valueIdx++;
        } else if (isType(DATATYPE_LATLONBBOX)) {
            values[offset]     = new Double(results.getDouble(valueIdx++));
            values[offset + 1] = new Double(results.getDouble(valueIdx++));
            values[offset + 2] = new Double(results.getDouble(valueIdx++));
            values[offset + 3] = new Double(results.getDouble(valueIdx++));
        } else if (isType(DATATYPE_PASSWORD)) {
            String value = results.getString(valueIdx);
            if (value != null) {
                byte[] bytes = Utils.decodeBase64(value);
                if (bytes != null) {
                    value = new String(bytes);
                }
            }
            values[offset] = value;
            valueIdx++;
        } else {
            String s = getDatabaseManager().getString(results, valueIdx);
            if ( !Utils.stringDefined(s)) {
                if (databaseDflt != null) {
                    s = databaseDflt;
                } else {
                    if (databaseDefaultPropertyName == null) {
                        databaseDefaultPropertyName = getName()
                                + ".databaseDefault";
                    }
                    s = entry.getTypeHandler().getTypeProperty(
                        databaseDefaultPropertyName, s);
                }
            }
            values[offset] = s;
            valueIdx++;
        }

        return valueIdx;
    }


    /**
     * _more_
     *
     * @param statement _more_
     * @param name _more_
     * @param type _more_
     * @param ignoreErrors _more_
     *
     * @throws Exception _more_
     */
    private void defineColumn(Statement statement, String name, String type,
                              boolean ignoreErrors)
            throws Exception {

        String sql = "alter table " + getTableName() + " add column " + name
                     + " " + type;
        SqlUtil.loadSql(sql, statement, ignoreErrors, null);

        if (changeType) {
            sql = getDatabaseManager().getAlterTableSql(getTableName(), name,
                    type);
            //            System.err.println("altering table: " + sql);
            SqlUtil.loadSql(sql, statement, ignoreErrors, null);
        }
    }


    /**
     * _more_
     *
     *
     * @param statement _more_
     *
     * @throws Exception _more_
     */
    public void createTable(Statement statement) throws Exception {
        createTable(statement, true);
    }

    /**
     * _more_
     *
     * @param statement _more_
     * @param ignoreErrors _more_
     *
     * @throws Exception _more_
     */
    public void createTable(Statement statement, boolean ignoreErrors)
            throws Exception {
        if (isType(DATATYPE_STRING) || isType(DATATYPE_PASSWORD)
                || isType(DATATYPE_EMAIL) || isType(DATATYPE_URL)
                || isType(DATATYPE_JSONLIST) || isType(DATATYPE_FILE)
                || isType(DATATYPE_ENTRY)) {
            defineColumn(statement, name, "varchar(" + size + ") ",
                         ignoreErrors);
        } else if (isType(DATATYPE_WIKI)) {
            defineColumn(statement, name,
                         getDatabaseManager().convertType("clob", 24000),
                         ignoreErrors);
        } else if (isType(DATATYPE_LIST)) {
            defineColumn(statement, name, "varchar(" + size + ") ",
                         ignoreErrors);
        } else if (isType(DATATYPE_CLOB)) {
            String clobType = getDatabaseManager().convertType("clob", size);
            defineColumn(statement, name, clobType, ignoreErrors);
        } else if (isEnumeration()) {
            defineColumn(statement, name, "varchar(" + size + ") ",
                         ignoreErrors);
        } else if (isType(DATATYPE_INT)) {
            defineColumn(statement, name, "int", ignoreErrors);
        } else if (isDouble()) {
            defineColumn(statement, name,
                         getDatabaseManager().convertType("double"),
                         ignoreErrors);
        } else if (isType(DATATYPE_BOOLEAN)) {
            //use int as boolean for database compatibility
            defineColumn(statement, name, "int", ignoreErrors);

        } else if (isDate()) {
            defineColumn(statement, name,
                         getDatabaseManager().convertSql("ramadda.datetime"),
                         ignoreErrors);
        } else if (isType(DATATYPE_LATLON)) {
            defineColumn(statement, name + "_lat",
                         getDatabaseManager().convertType("double"),
                         ignoreErrors);
            defineColumn(statement, name + "_lon",
                         getDatabaseManager().convertType("double"),
                         ignoreErrors);
        } else if (isType(DATATYPE_LATLONBBOX)) {
            defineColumn(statement, name + "_north",
                         getDatabaseManager().convertType("double"),
                         ignoreErrors);
            defineColumn(statement, name + "_west",
                         getDatabaseManager().convertType("double"),
                         ignoreErrors);
            defineColumn(statement, name + "_south",
                         getDatabaseManager().convertType("double"),
                         ignoreErrors);
            defineColumn(statement, name + "_east",
                         getDatabaseManager().convertType("double"),
                         ignoreErrors);

        } else {
            throw new IllegalArgumentException("Unknown column type:" + type
                    + " for " + name);
        }


        if (oldNames != null) {
            for (int i = 0; i < oldNames.size(); i++) {
                String sql = "update " + getTableName() + " set " + name
                             + " = " + oldNames.get(i);
                SqlUtil.loadSql(sql, statement, ignoreErrors, null);
                sql = "alter table " + getTableName() + " drop "
                      + oldNames.get(i);
                SqlUtil.loadSql(sql, statement, true, null);
            }
        }

        if (isIndex) {
            SqlUtil.loadSql("CREATE INDEX " + getTableName() + "_INDEX_"
                            + name + "  ON " + getTableName() + " (" + name
                            + ")", statement, ignoreErrors, null);
        }

    }


    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     */
    public Object convert(String value) {
        if (isType(DATATYPE_INT)) {
            return new Integer(value);
        } else if (isDouble()) {
            return new Double(value);
        } else if (isType(DATATYPE_BOOLEAN)) {
            return new Boolean(value);
        } else if (isType(DATATYPE_DATETIME)) {
            //TODO
        } else if (isType(DATATYPE_DATE)) {
            //TODO
        } else if (isType(DATATYPE_LATLON)) {
            //TODO
        } else if (isType(DATATYPE_LATLONBBOX)) {
            //TODO
        }

        return value;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getTableName() {
        return typeHandler.getTableName();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getUnit() {
        return unit;
    }

    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    private boolean latLonOk(Object o) {
        if (o == null) {
            return false;
        }
        Double d = (Double) o;

        return latLonOk(d.doubleValue());
    }

    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    private boolean latLonOk(double v) {
        return ((v == v) && (v != Entry.NONGEO));
    }

    /**
     * _more_
     *
     * @param clauses _more_
     */
    public void addGeoExclusion(List<Clause> clauses) {
        if (isType(DATATYPE_LATLON)) {
            String id = getFullName();
            clauses.add(Clause.neq(id + "_lat", Entry.NONGEO));
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param where _more_
     * @param searchCriteria _more_
     *
     * @throws Exception _more_
     */
    public void assembleWhereClause(Request request, List<Clause> where,
                                    Appendable searchCriteria)
            throws Exception {

        boolean[] fromFile  = { false };
        String    searchArg = getSearchArg();
        boolean   doNegate  = false;
        if (addNot && request.defined(searchArg + "_not")) {
            doNegate = request.get(searchArg + "_not", false);
        }

        List<String> values = null;

        if (addFileToSearch) {
            String file = request.getUploadedFile(searchArg + "_file");
            if (file != null) {
                values = StringUtil.split(
                    getRepository().getStorageManager().readFile(file), "\n",
                    true, true);
            }
        }



        String          columnName = getFullName();
        DatabaseManager dbm        = getDatabaseManager();
        //      System.err.println("s:" + searchArg);
        if (isType(DATATYPE_LATLON)) {
            double north = request.get("north",
                                       request.get(searchArg + "_north",
                                           request.get(ARG_AREA_NORTH,
                                               Double.NaN)));
            double south = request.get("south",
                                       request.get(searchArg + "_south",
                                           request.get(ARG_AREA_SOUTH,
                                               Double.NaN)));
            double east = request.get("east",
                                      request.get(searchArg + "_east",
                                          request.get(ARG_AREA_EAST,
                                              Double.NaN)));
            double west = request.get("west",
                                      request.get(searchArg + "_west",
                                          request.get(ARG_AREA_WEST,
                                              Double.NaN)));


            if (latLonOk(north)) {
                where.add(Clause.le(columnName + "_lat", north));
            }
            if (latLonOk(south)) {
                where.add(Clause.ge(columnName + "_lat", south));
            }
            if (latLonOk(west)) {
                where.add(Clause.ge(columnName + "_lon", west));
            }
            if (latLonOk(east)) {
                where.add(Clause.le(columnName + "_lon", east));
            }
            getRepository().getSessionManager().setArea(request, north, west,
                    south, east);
        } else if (isType(DATATYPE_LATLONBBOX)) {
            double north = request.get(searchArg + "_north",
                                       request.get("north", Double.NaN));
            double south = request.get(searchArg + "_south",
                                       request.get("south", Double.NaN));
            double east = request.get(searchArg + "_east",
                                      request.get("east", Double.NaN));
            double west = request.get(searchArg + "_west",
                                      request.get("west", Double.NaN));

            if (latLonOk(north)) {
                where.add(Clause.le(columnName + "_north", north));
            }
            if (latLonOk(south)) {
                where.add(Clause.ge(columnName + "_south", south));
            }
            if (latLonOk(west)) {
                where.add(Clause.ge(columnName + "_west", west));
            }
            if (latLonOk(east)) {
                where.add(Clause.le(columnName + "_east", east));
            }
            getRepository().getSessionManager().setArea(request, north, west,
                    south, east);

        } else if (isNumeric()) {
            String expr = request.getEnum(searchArg + "_expr", "", "",
                                          EXPR_EQUALS, EXPR_LE, EXPR_GE,
                                          EXPR_BETWEEN, "&lt;=", "&gt;=");
            expr = expr.replace("&lt;", "<").replace("&gt;", ">");
            double from  = request.get(searchArg + "_from", Double.NaN);
            double to    = request.get(searchArg + "_to", Double.NaN);
            double value = request.get(searchArg, Double.NaN);

            if (isType(DATATYPE_PERCENTAGE)) {
                from  = from / 100.0;
                to    = to / 100.0;
                value = value / 100.0;
            }
            if ((from == from) && (to != to)) {
                to = value;
            } else if ((from != from) && (to == to)) {
                from = value;
            } else if ((from != from) && (to != to)) {
                from = value;
                to   = value;
            }
            if (from == from) {
                if (expr.equals("")) {
                    expr = EXPR_EQUALS;
                }
                if (expr.equals(EXPR_EQUALS)) {
                    where.add(Clause.eq(getFullName(), from));
                } else if (expr.equals(EXPR_LE)) {
                    where.add(Clause.le(getFullName(), from));
                } else if (expr.equals(EXPR_GE)) {
                    where.add(Clause.ge(getFullName(), from));
                } else if (expr.equals(EXPR_BETWEEN)) {
                    where.add(Clause.ge(getFullName(), from));
                    where.add(Clause.le(getFullName(), to));
                } else if (expr.length() > 0) {
                    throw new IllegalArgumentException("Unknown expression:"
                            + expr);
                }
            }
        } else if (isType(DATATYPE_BOOLEAN)) {
            if (request.defined(searchArg)) {
                where.add(Clause.eq(columnName, (request.get(searchArg, true)
                        ? 1
                        : 0)));
            }
        } else if (isDate()) {
            String relativeArg = searchArg + "_relative";
            Date[] dateRange = request.getDateRange(searchArg + "_fromdate",
                                   searchArg + "_todate", relativeArg,
                                   null /*new Date()*/);

            Clause tmp;
            if (dateRange[0] != null) {
                where.add(tmp = Clause.ge(columnName, dateRange[0]));
                System.err.println("min date:" + dateRange[0] + " " + tmp);
            }

            if (dateRange[1] != null) {
                where.add(tmp = Clause.le(columnName, dateRange[1]));
                System.err.println("max date:" + dateRange[1] + " " + tmp);
            }
        } else if (isType(DATATYPE_ENTRY)) {
            String value = request.getString(searchArg + "_hidden", "");
            if (Utils.stringDefined(value)) {
                where.add(Clause.eq(columnName, value));
            }
        } else if (isType(DATATYPE_LIST)) {
            boolean hadFile = values != null;
            if (values == null) {
                String value = getSearchValue(request);
                if (Utils.stringDefined(value)) {
                    values = Utils.split(value.trim(), "\n", true, true);
                }
            }
            if (values != null) {
                List<Clause> subs = new ArrayList<Clause>();
                for (String v : values) {
                    if (doNegate) {
                        subs.add(Clause.neq(columnName, v));
                    } else {
                        subs.add(Clause.eq(columnName, v));
                        if ( !hadFile) {
			    //Not sure we should do this for enums
                            subs.add(dbm.makeLikeTextClause(columnName,
                                    "%" + v + "%", false));
                        }
                    }
                }
                if (doNegate) {
                    where.add(Clause.and(subs));
                } else {
                    where.add(Clause.or(subs));
                }
            }
        } else if (isEnumeration()) {
            if (values == null) {
                values = getSearchValues(request);
            }
            if ((values != null) && (values.size() > 0)) {
                List<Clause> subClauses = new ArrayList<Clause>();
                for (String value : values) {
                    if (value.equals("_blank_")) {
                        value = "";
                    }
                    if (value.equals(TypeHandler.ALL)) {
                        continue;
                    }
                    if (value.startsWith("!")) {
                        subClauses.add(Clause.neq(columnName,
                                value.substring(1), doNegate));
                    } else {
                        subClauses.add(Clause.eq(columnName, value,
                                doNegate));
                    }
                }
                if (subClauses.size() > 0) {
                    where.add(Clause.or(subClauses));
                }
            }
        } else {
            //            String value = getSearchValue(request);
            if (values == null) {
                String value = getSearchValue(request);
                if (Utils.stringDefined(value)) {
                    if (value.equals("_blank_")) {
                        value = "";
                    }
                    addTextSearch(value, where, doNegate);
                }
            } else {
                //This is for the file upload. Just do equality not the like that addTextSearch does
                List<Clause> clauses = new ArrayList<Clause>();
                for (String value : values) {
                    if ( !Utils.stringDefined(value)) {
                        continue;
                    }
                    if (value.equals("_blank_")) {
                        value = "";
                    }
		    //For now just check for straight equality
		    //                    clauses.add(Clause.upperEquals(getFullName(), value, doNegate));
                    clauses.add(Clause.eq(getFullName(), value, doNegate));
                }
                if (clauses.size() > 0) {
                    Clause clause;
                    if (doNegate) {
                        clause = Clause.and(clauses);
                    } else {
                        clause = Clause.or(clauses);
                    }
                    where.add(clause);
                }
            }
        }
    }


    /**
     * _more_
     *
     * @param text _more_
     * @param where _more_
     * @param doNegate _more_
     */
    public void addTextSearch(String text, List<Clause> where,
                              boolean doNegate) {
        String       DELIM_AND = " AND ";
        String       DELIM_OR  = " OR ";
        List<Clause> clauses   = new ArrayList<Clause>();
        text = text.replace("_comma_", ",").replace("_space_", " ");
        String       delimiter = (text.indexOf(DELIM_AND) >= 0)
                                 ? DELIM_AND
                                 : DELIM_OR;
        List<String> andToks   = Utils.split(text, delimiter);
        if (andToks.size() == 0) {
            andToks.add("");
        }
        List<Clause> subClauses = new ArrayList<Clause>();
        for (String value : andToks) {
            String trimmed = value.trim();
            if (trimmed.startsWith("\"") && trimmed.trim().endsWith("\"")) {
                value = Utils.unquote(trimmed);
                subClauses.add(Clause.eq(getFullName(), value, doNegate));
                continue;
            }
            if (trimmed.equals("<blank>") || trimmed.equals("_blank_")) {
                subClauses.add(Clause.eq(getFullName(), "", doNegate));
            } else if (trimmed.startsWith("!")) {
                value = trimmed.substring(1);
                if (value.length() == 0) {
                    subClauses.add(Clause.neq(getFullName(), "", doNegate));
                } else {
                    subClauses.add(Clause.notLike(getFullName(),
                            "%" + value + "%"));
                }
            } else if (trimmed.startsWith("=")) {
                value = trimmed.substring(1);
                subClauses.add(Clause.eq(getFullName(), value, doNegate));
            } else if ( !trimmed.startsWith("%") && trimmed.endsWith("%")) {
                subClauses.add(
                    getDatabaseManager().makeLikeTextClause(
                        getFullName(), value, doNegate));
            } else {
                if (trimmed.length() == 0) {
                    subClauses.add(Clause.eq(getFullName(), "", doNegate));
                } else {
                    subClauses.add(
                        getDatabaseManager().makeLikeTextClause(
                            getFullName(), "%" + value + "%", doNegate));
                }
            }
        }
        if (subClauses.size() == 1) {
            clauses.add(subClauses.get(0));
        } else if (subClauses.size() > 1) {
            if (delimiter.equals(DELIM_AND)) {
                clauses.add(Clause.and(subClauses));
            } else {
                clauses.add(Clause.or(subClauses));
            }
        }
        //      System.err.println(this+" CLAUSES:" + clauses);
        if (clauses.size() > 0) {
            where.add(Clause.or(clauses));
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private String getSearchValue(Request request) {
        String dflt      = null;
        String searchArg = getSearchArg();
        if (request.defined(searchArg)) {
            return request.getString(searchArg, dflt);
        }

        return dflt;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<String> getSearchValues(Request request) throws Exception {
        String       searchArg = getSearchArg();
        List<String> result    = new ArrayList<String>();
        for (int i = 0; i < numberOfSearchWidgets; i++) {
            String sarg = searchArg + ((i == 0)
                                       ? ""
                                       : "" + i);
            for (String arg :
                    (List<String>) request.get(sarg,
                        new ArrayList<String>())) {
                //            result.addAll(Utils.split(arg, ",", true));
                result.add(arg);
            }
        }

        return result;
    }


    /**
     * _more_
     *
     * @param arg _more_
     * @param value _more_
     * @param values _more_
     *
     * @return _more_
     */
    public int matchValue(String arg, Object value, Object[] values) {
        if (isType(DATATYPE_LATLON)) {
            //TODO:
        } else if (isType(DATATYPE_LATLONBBOX)) {
            //TODO:
        } else if (isType(DATATYPE_BOOLEAN)) {
            if (arg.equals(getFullName())) {
                if (values[offset].toString().equals(value)) {
                    return TypeHandler.MATCH_TRUE;
                }

                return TypeHandler.MATCH_FALSE;
            }
        } else if (isNumeric()) {
            //
        } else {
            if (arg.equals(getFullName())) {
                if (values[offset].equals(value)) {
                    return TypeHandler.MATCH_TRUE;
                }

                return TypeHandler.MATCH_FALSE;
            }
        }

        return TypeHandler.MATCH_UNKNOWN;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param formBuffer _more_
     * @param entry _more_
     * @param values _more_
     * @param state _more_
     * @param formInfo _more_
     * @param sourceTypeHandler _more_
     *
     * @throws Exception _more_
     */
    public void addToEntryForm(Request request, Entry entry,
                               Appendable formBuffer, Object[] values,
                               Hashtable state, FormInfo formInfo,
                               TypeHandler sourceTypeHandler)
            throws Exception {
        if ( !showInForm) {
            return;
        }
        String widget = getFormWidget(request, entry, values, formInfo);
        widget = sourceTypeHandler.getFormWidget(request, entry, this,
                widget);
        //        String rightSide = sourceTypeHandler.getFormHelp(request, entry, this);
        //        formBuffer.append(HtmlUtils.formEntry(getLabel() + ":",
        //                                             HtmlUtils.hbox(widget, rightSide)));
        if ((group != null) && (state.get(group) == null)) {
            formBuffer.append(
                HtmlUtils.row(
                    HtmlUtils.colspan(
                        HtmlUtils.div(group, " class=\"formgroupheader\" "),
                        2)));
            state.put(group, group);
        }

        if (help != null) {
            formBuffer.append(typeHandler.formEntry(request, "",
                    getRepository().getPageHandler().applyBaseMacros(help)));
        }


        if (rows > 1) {
            formBuffer.append(typeHandler.formEntryTop(request,
                    getLabel() + ":", widget));
        } else {
            formBuffer.append(typeHandler.formEntry(request,
                    getLabel() + ":", widget));
        }
        formBuffer.append("\n");
    }




    //For now just change the edit argument by adding a edit_ prefix

    /**
     * _more_
     *
     * @return _more_
     */
    public String getEditArg() {
        return ARG_EDIT_PREFIX + getFullName().replace(".", "_");
    }


    /** _more_          */
    private String overrideSearchArg;

    /**
     * _more_
     *
     * @param arg _more_
     */
    public void setSearchArg(String arg) {
        overrideSearchArg = arg;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getSearchArg() {
        if (overrideSearchArg != null) {
            return overrideSearchArg;
        }

        return ARG_SEARCH_PREFIX + getFullName();
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param values _more_
     * @param formInfo _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getFormWidget(Request request, Entry entry,
                                Object[] values, FormInfo formInfo)
            throws Exception {

        String widget = "";

        String urlArg = getEditArg();

        if (isType(DATATYPE_LATLON)) {
            double lat = 0;
            double lon = 0;
            if (values != null) {
                lat = ((Double) values[offset]).doubleValue();
                lon = ((Double) values[offset + 1]).doubleValue();
            }
            MapInfo map = getRepository().getMapManager().createMap(request,
                              entry, true, null);
            widget = map.makeSelector(urlArg, true,
                                      new String[] { latLonOk(lat)
                    ? lat + ""
                    : "", latLonOk(lon)
                          ? lon + ""
                          : "" });
        } else if (isType(DATATYPE_LATLONBBOX)) {
            String[] nwse = null;
            if (values != null) {
                nwse = new String[] { latLonOk(values[offset + 0])
                                      ? values[offset + 0] + ""
                                      : "", latLonOk(values[offset + 1])
                                            ? values[offset + 1] + ""
                                            : "", latLonOk(values[offset + 2])
                        ? values[offset + 2] + ""
                        : "", latLonOk(values[offset + 3])
                              ? values[offset + 3] + ""
                              : "", };
            }
            MapInfo map = getRepository().getMapManager().createMap(request,
                              entry, true, null);
            widget = map.makeSelector(urlArg, true, nwse, "", "");
        } else if (isType(DATATYPE_BOOLEAN)) {
            boolean value = true;
            if (values != null) {
                if (toBoolean(values, offset)) {
                    value = true;
                } else {
                    value = false;
                }
            } else {
                value = Misc.equals(dflt, "true");
            }
            //            widget = HtmlUtils.checkbox(urlArg, "true", value);
            List<TwoFacedObject> items = new ArrayList<TwoFacedObject>();
            items.add(new TwoFacedObject("Yes", "true"));
            items.add(new TwoFacedObject("No", "false"));
            widget = HtmlUtils.select(urlArg, items, value
                    ? "true"
                    : "false", HtmlUtils.cssClass("search-select"));
        } else if (isType(DATATYPE_DATETIME)) {
            Date date;
            if (values != null) {
                date = (Date) values[offset];
            } else {
                date = new Date();
            }
            widget = getRepository().getDateHandler().makeDateInput(request,
                    urlArg, "", date, null);
        } else if (isType(DATATYPE_DATE)) {
            Date date;
            if (values != null) {
                date = (Date) values[offset];
            } else {
                date = new Date();
            }
            widget = getRepository().getDateHandler().makeDateInput(request,
                    urlArg, "", date, null, false);
        } else if (isType(DATATYPE_ENUMERATION)) {
            String value = ((dflt != null)
                            ? dflt
                            : "");
            value = request.getString(urlArg, ((values != null)
                    ? (String) toString(values, offset)
                    : ""));
            widget = HtmlUtils.select(urlArg, enumValues, value,
                                      HtmlUtils.cssClass("column-select"));
        } else if (isType(DATATYPE_ENUMERATIONPLUS)) {
            String value = ((dflt != null)
                            ? dflt
                            : "");
            value = request.getString(urlArg, ((values != null)
                    ? (String) toString(values, offset)
                    : ""));
            List enums = getEnumPlusValues(request, entry);
            widget = HtmlUtils.select(
                urlArg, enums, value,
                HtmlUtils.cssClass("column-select")) + "  or:  "
                    + HtmlUtils.input(
                        urlArg + "_plus", "", HtmlUtils.SIZE_20);
        } else if (isType(DATATYPE_INT)) {
            String value = ((dflt != null)
                            ? dflt
                            : "");
            if (values != null) {
                value = "" + toString(values, offset);
            }
            widget = HtmlUtils.input(urlArg, value, HtmlUtils.SIZE_10);
        } else if (isType(DATATYPE_DOUBLE)) {
            String domId = HtmlUtils.getUniqueId("input_");
            if ( !Double.isNaN(max)) {
                formInfo.addMaxValidation(getLabel(), domId, max);
            }
            if ( !Double.isNaN(min)) {
                formInfo.addMinValidation(getLabel(), domId, min);
            }

            String value = ((dflt != null)
                            ? dflt
                            : "");
            if (values != null) {
                value = "" + toString(values, offset);
            }
            widget = HtmlUtils.input(urlArg, value,
                                     HtmlUtils.SIZE_10 + HtmlUtils.id(domId));
        } else if (isType(DATATYPE_PERCENTAGE)) {
            String value = ((dflt != null)
                            ? dflt
                            : "0");

            if (values != null) {
                value = "" + toString(values, offset);
            }
            if (value.trim().length() == 0) {
                value = "0";
            }
            double d          = new Double(value).doubleValue();
            int    percentage = (int) (d * 100);
            widget = HtmlUtils.input(urlArg, percentage + "",
                                     HtmlUtils.SIZE_5) + "%";
        } else if (isType(DATATYPE_PASSWORD)) {
            String value = ((dflt != null)
                            ? dflt
                            : "");
            if (values != null) {
                value = "" + toString(values, offset);
            }
            widget = HtmlUtils.password(urlArg, value,
                                        HtmlUtils.attr("size", ((columns > 0)
                    ? "" + columns
                    : "10")));
        } else if (isType(DATATYPE_FILE)) {
            String value = ((dflt != null)
                            ? dflt
                            : "");
            if (values != null) {
                value = "" + toString(values, offset);
            }
            widget = HtmlUtils.fileInput(urlArg, "");
        } else if (isType(DATATYPE_ENTRY)) {
            String value = "";
            if (values != null) {
                value = toString(values, offset);
            }
            widget =
                getRepository().getEntryManager().getEntryFormSelect(request,
                    entry, urlArg, value);
        } else {
            String value = ((dflt != null)
                            ? dflt
                            : "");
            if (values != null) {
                value = toString(values, offset);
            } else if (request.defined(urlArg)) {
                value = request.getString(urlArg);
            }
            if (searchType.equals(SEARCHTYPE_SELECT)) {
                Hashtable props =
                    getRepository().getFieldProperties(propertiesFile);
                List<TwoFacedObject> tfos = new ArrayList<TwoFacedObject>();
                if (props != null) {
                    for (Enumeration keys = props.keys();
                            keys.hasMoreElements(); ) {
                        String xid = (String) keys.nextElement();
                        if (xid.endsWith(".label")) {
                            xid = xid.substring(0,
                                    xid.length() - ".label".length());
                            tfos.add(new TwoFacedObject(getLabel(xid), xid));
                        }
                    }
                }

                tfos = (List<TwoFacedObject>) Misc.sort(tfos);
                if (tfos.size() == 0) {
                    widget = HtmlUtils.input(urlArg, value, " size=10 ");
                } else {
                    widget = HtmlUtils.select(urlArg, tfos, value);
                }
            } else {
                String domId = HtmlUtils.getUniqueId("input_");
                if ((rows > 1) || isWiki) {
                    if (isType(DATATYPE_LIST)) {
                        value = StringUtil.join("\n",
                                Utils.split(value, ",", true, true));
                    }
                    if (isWiki) {
                        StringBuilder tmp = new StringBuilder();
                        typeHandler.addWikiEditor(request, entry, tmp,
                                formInfo, urlArg, value, null, false, size,
                                true);
                        widget = tmp.toString();
                    } else {
                        int areaRows = rows;
                        widget = HtmlUtils.textArea(urlArg, value, areaRows,
                                columns, HtmlUtils.id(domId));
                    }
                } else {
                    widget = HtmlUtils.input(urlArg, value,
                                             HtmlUtils.id(domId) + " size=\""
                                             + columns + "\"");
                }
                if (size > 0) {
                    formInfo.addMaxSizeValidation(getLabel(), domId, size);
                }
                if (required) {
                    formInfo.addRequiredValidation(getLabel(), domId);
                }
            }
        }

        if (required) {
            widget = widget + " "
                     + HtmlUtils.span(
                         "* " + msg("required"),
                         HtmlUtils.cssClass("ramadda-required-field"));
        }

        return widget;
    }

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List getEnumPlusValues(Request request, Entry entry)
            throws Exception {
        List<TwoFacedObject> enums = typeHandler.getEnumValues(request, this,
                                         entry);
        //TODO: Check for Strings vs TwoFacedObjects
        if (enumValues != null) {
            List tmp = new ArrayList();
            for (Object o : enums) {
                if ( !(o instanceof TwoFacedObject)) {
                    o = new TwoFacedObject(o);
                }
                //                if ( !TwoFacedObject.contains(enumValues, o)) {
                if ( !enumValues.contains(o)) {
                    tmp.add(o);
                }
            }
            tmp.addAll(enumValues);
            enums = tmp;
            //            System.err.print("TMPS: " + enums);
        }

        return enums;
    }

    /**
     * _more_
     *
     * @param values _more_
     *
     * @return _more_
     */
    public double[] getLatLonBbox(Object[] values) {
        return new double[] { (Double) values[offset],
                              (Double) values[offset + 1],
                              (Double) values[offset + 2],
                              (Double) values[offset + 3] };
    }


    /**
     * _more_
     *
     * @param values _more_
     *
     * @return _more_
     */
    public double[] getLatLon(Object[] values) {
        Double lat = (Double) values[offset];
        Double lon = (Double) values[offset + 1];

        return new double[] { (lat == null)
                              ? Double.NaN
                              : lat.doubleValue(), (lon == null)
                ? Double.NaN
                : lon.doubleValue() };

    }

    /**
     * _more_
     *
     * @param values _more_
     *
     * @return _more_
     */
    public boolean hasLatLon(Object[] values) {
        if ((values[offset] == null)
                || ((Double) values[offset]).doubleValue() == Entry.NONGEO) {
            return false;
        }
        if ((values[offset + 1] == null)
                || ((Double) values[offset + 1]).doubleValue()
                   == Entry.NONGEO) {
            return false;
        }

        return true;
    }

    /**
     * _more_
     *
     * @param values _more_
     *
     * @return _more_
     */
    public boolean hasLatLonBox(Object[] values) {
        for (int i = 0; i < 4; i++) {
            if ((values[offset + i] == null)
                    || ((Double) values[offset + i]).doubleValue()
                       == Entry.NONGEO) {
                return false;
            }
        }

        return true;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    public void setValue(Request request, Entry entry, Object[] values)
            throws Exception {

        if ( !showInForm || !editable) {
            //            System.err.println (this + " not adding to form" );
            return;
        }

        String urlArg = getEditArg();


        if (isType(DATATYPE_LATLON)) {
            if (request.exists(urlArg + "_latitude")) {
                values[offset] = new Double(request.getString(urlArg
                        + "_latitude", "0").trim());
                values[offset + 1] = new Double(request.getString(urlArg
                        + "_longitude", "0").trim());
            } else if (request.exists(urlArg + ".latitude")) {
                String latString = request.getString(urlArg + ".latitude",
                                       "0").trim();
                String lonString = request.getString(urlArg + ".longitude",
                                       "0").trim();
                double lat = Entry.NONGEO;
                double lon = Entry.NONGEO;
                if (Utils.stringDefined(latString)) {
                    lat = Utils.decodeLatLon(latString);
                }
                if (Utils.stringDefined(lonString)) {
                    lon = Utils.decodeLatLon(lonString);
                }

                values[offset]     = lat;
                values[offset + 1] = lon;
            }

        } else if (isType(DATATYPE_LATLONBBOX)) {
            if (request.exists(urlArg + "_north")) {
                values[offset] = new Double(request.get(urlArg + "_north",
                        Entry.NONGEO));
                values[offset + 1] = new Double(request.get(urlArg + "_west",
                        Entry.NONGEO));
                values[offset + 2] = new Double(request.get(urlArg
                        + "_south", Entry.NONGEO));
                values[offset + 3] = new Double(request.get(urlArg + "_east",
                        Entry.NONGEO));
            } else {
                values[offset] = new Double(request.get(urlArg + ".north",
                        Entry.NONGEO));
                values[offset + 1] = new Double(request.get(urlArg + ".west",
                        Entry.NONGEO));
                values[offset + 2] = new Double(request.get(urlArg
                        + ".south", Entry.NONGEO));
                values[offset + 3] = new Double(request.get(urlArg + ".east",
                        Entry.NONGEO));

            }
        } else if (isDate()) {
            values[offset] = request.getDate(urlArg, new Date());
        } else if (isType(DATATYPE_BOOLEAN)) {
            //Note: using the default will not work if we use checkboxes for the widget
            //For now we are using a yes/no combobox
            String value = request.getString(urlArg,
                                             (Utils.stringDefined(dflt)
                    ? dflt
                    : "true")).toLowerCase();
            //            String value = request.getString(urlArg, "false");
            values[offset] = new Boolean(value);
        } else if (isType(DATATYPE_ENUMERATION)) {
            if (request.exists(urlArg)) {
                values[offset] = request.getAnonymousEncodedString(urlArg,
                        ((dflt != null)
                         ? dflt
                         : ""));
            } else {
                values[offset] = dflt;
            }
        } else if (isType(DATATYPE_LIST)) {
            if (request.exists(urlArg)) {
                String value = request.getAnonymousEncodedString(urlArg,
                                   ((dflt != null)
                                    ? dflt
                                    : ""));
                value = StringUtil.join(", ",
                                        Utils.split(value, "\n", true, true));
                values[offset] = value;
            } else {
                values[offset] = dflt;
            }

        } else if (isType(DATATYPE_ENUMERATIONPLUS)) {
            String theValue = "";
            if (request.defined(urlArg + "_plus")) {
                theValue = request.getAnonymousEncodedString(urlArg
                        + "_plus", ((dflt != null)
                                    ? dflt
                                    : ""));
            } else if (request.defined(urlArg)) {
                theValue = request.getAnonymousEncodedString(urlArg,
                        ((dflt != null)
                         ? dflt
                         : ""));

            } else {
                theValue = dflt;
            }
            values[offset] = theValue;
            typeHandler.addEnumValue(this, entry, theValue);
        } else if (isType(DATATYPE_INT)) {
            int dfltValue = (Utils.stringDefined(dflt)
                             ? new Integer(dflt).intValue()
                             : 0);
            if (request.exists(urlArg)) {
                values[offset] = new Integer(request.get(urlArg, dfltValue));
            } else {
                values[offset] = dfltValue;
            }

        } else if (isType(DATATYPE_PERCENTAGE)) {
            double dfltValue = (Utils.stringDefined(dflt)
                                ? new Double(dflt.trim()).doubleValue()
                                : 0);
            if (request.exists(urlArg)) {
                values[offset] = new Double(request.get(urlArg, dfltValue)
                                            / 100);
            } else {
                values[offset] = dfltValue;

            }
        } else if (isDouble()) {
            double dfltValue = (Utils.stringDefined(dflt)
                                ? new Double(dflt.trim()).doubleValue()
                                : 0);
            if (request.exists(urlArg)) {
                values[offset] = new Double(request.get(urlArg, dfltValue));
            } else {
                values[offset] = dfltValue;

            }
        } else if (isType(DATATYPE_ENTRY)) {
            values[offset] = request.getString(urlArg + "_hidden", "");
        } else {
            //string
            if (request.exists(urlArg)) {
                values[offset] = request.getAnonymousEncodedString(urlArg,
                        ((dflt != null)
                         ? dflt
                         : ""));
            } else {
                values[offset] = dflt;
            }
        }

    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param values _more_
     * @param value _more_
     *
     * @throws Exception _more_
     */
    public void setValue(Entry entry, Object[] values, String value)
            throws Exception {

        if (isType(DATATYPE_LATLON)) {
            List<String> toks = Utils.split(value, ";", true, true);
            if (toks.size() == 2) {
                values[offset]     = new Double(toks.get(0));
                values[offset + 1] = new Double(toks.get(1));
            } else {
                toks = Utils.split(value, "|", true, true);
                if (toks.size() == 2) {
                    values[offset]     = new Double(toks.get(0));
                    values[offset + 1] = new Double(toks.get(1));
                } else {
                    //What to do here
                }
            }
        } else if (isType(DATATYPE_LATLONBBOX)) {
            List<String> toks = Utils.split(value, ";", true, true);
            values[offset]     = new Double(toks.get(0));
            values[offset + 1] = new Double(toks.get(1));
            values[offset + 2] = new Double(toks.get(2));
            values[offset + 3] = new Double(toks.get(3));
        } else if (isDate()) {
            fullDateTimeFormat.setTimeZone(RepositoryBase.TIMEZONE_UTC);
            values[offset] = parseDate(value);
        } else if (isEnumeration()) {
            values[offset] = value;
        } else if (isType(DATATYPE_BOOLEAN)) {
            if (Utils.stringDefined(value)) {
                values[offset] = new Boolean(value);
            } else {
                values[offset] = new Boolean(false);
            }
        } else if (isType(DATATYPE_INT)) {
            try {
                if (Utils.stringDefined(value)) {
                    values[offset] = new Integer(value);
                } else {
                    values[offset] = new Integer(0);
                }
            } catch (NumberFormatException nfe) {
                throw new RuntimeException("Error parsing integer column:"
                                           + getName() + " value:" + value);

            }
        } else if (isType(DATATYPE_PERCENTAGE) || isDouble()) {
            if (Utils.stringDefined(value)) {
                values[offset] = new Double(value);
            } else {
                values[offset] = new Double(0);
            }
        } else if (isType(DATATYPE_ENTRY)) {
            values[offset] = value;
        } else {
            values[offset] = value;
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
    private Date parseDate(String value) throws Exception {
        if ( !Utils.stringDefined(value)) {
            return null;
        }

        if (importDateIsEpoch) {
            long l = new Long(value).longValue();

            return new Date(l);
        }

        if (dateParser != null) {
            try {
                return dateParser.parse(value);
            } catch (java.text.ParseException pe) {
                throw new IllegalArgumentException("Column:" + getName()
                        + " could not parse date:" + value);
            }
        }

        return Utils.parseDate(value);
    }

    /**
     * _more_
     *
     * @param formBuffer _more_
     * @param request _more_
     * @param where _more_
     *
     * @throws Exception _more_
     */
    public void addToSearchForm(Request request, Appendable formBuffer,
                                List<Clause> where)
            throws Exception {
        addToSearchForm(request, formBuffer, where, null);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param searchArg _more_
     * @param fromEntry _more_
     *
     * @return _more_
     */
    private String[] getNWSE(Request request, Entry entry, String searchArg,
                             boolean fromEntry) {
        if ( !fromEntry && request.defined(searchArg + "_north")) {
            return new String[] { request.getString(searchArg + "_north", ""),
                                  request.getString(searchArg + "_west", ""),
                                  request.getString(searchArg + "_south", ""),
                                  request.getString(searchArg + "_east",
                                  ""), };
        }
        if ((entry != null) && entry.hasAreaDefined()) {
            return new String[] { "" + entry.getNorth(), "" + entry.getWest(),
                                  "" + entry.getSouth(),
                                  "" + entry.getEast() };

        }

        return new String[] { "", "", "", "" };
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param formBuffer _more_
     * @param where _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void addToSearchForm(Request request, Appendable formBuffer,
                                List<Clause> where, Entry entry)
            throws Exception {

        if ( !getCanSearch()) {
            return;
        }

        String       searchArg  = getSearchArg();
        String       columnName = getFullName();

        List<Clause> tmp        = new ArrayList<Clause>(where);
        String       widget     = "";
        String       widgetId   = searchArg.replaceAll("\\.", "_");
        if (isType(DATATYPE_LATLON)) {
            String[] nwseValues = getNWSE(request, null, searchArg, false);
            String[] nwseView   = getNWSE(request, entry, searchArg, true);
            MapInfo map = getRepository().getMapManager().createMap(request,
                              entry, true, null);
            widget = map.makeSelector(searchArg, true, nwseValues, nwseView,
                                      "", "");
        } else if (isType(DATATYPE_LATLONBBOX)) {
            String[] nwseValues = getNWSE(request, null, searchArg, false);
            String[] nwseView   = getNWSE(request, entry, searchArg, true);
            MapInfo map = getRepository().getMapManager().createMap(request,
                              entry, true, null);
            widget = map.makeSelector(searchArg, true, nwseValues, nwseView,
                                      "", "");
        } else if (isDate()) {
            List dateSelect = new ArrayList();
            dateSelect.add(new TwoFacedObject(msg("Relative Date"), ""));
            dateSelect.add(new TwoFacedObject(msg("Last hour"), "-1 hour"));
            dateSelect.add(new TwoFacedObject(msg("Last 3 hours"),
                    "-3 hours"));
            dateSelect.add(new TwoFacedObject(msg("Last 6 hours"),
                    "-6 hours"));
            dateSelect.add(new TwoFacedObject(msg("Last 12 hours"),
                    "-12 hours"));
            dateSelect.add(new TwoFacedObject(msg("Last day"), "-1 day"));
            dateSelect.add(new TwoFacedObject(msg("Last week"), "-7 days"));
            dateSelect.add(new TwoFacedObject(msg("Last 2 weeks"),
                    "-14 days"));
            dateSelect.add(new TwoFacedObject(msg("Last month"), "-1 month"));

            String dateSelectValue;
            String relativeArg = searchArg + "_relative";
            if (request.exists(relativeArg)) {
                dateSelectValue = request.getString(relativeArg, "");
            } else {
                dateSelectValue = "none";
            }
            if (dateSelectValue.equals("")) {
                dateSelectValue = "none";
            }
            String dateSelectInput =
                HtmlUtils.select(searchArg + "_relative", dateSelect,
                                 dateSelectValue,
                                 HtmlUtils.cssClass("search-select"));

            widget = getRepository().getDateHandler().makeDateInput(
                request, searchArg + "_fromdate", "searchform", null, null,
                isType(DATATYPE_DATETIME)) + HtmlUtils.space(1)
                    + HtmlUtils.img(getRepository().getIconUrl(ICON_RANGE))
                    + HtmlUtils.space(1)
                    + getRepository().getDateHandler().makeDateInput(
                        request, searchArg + "_todate", "searchform", null,
                            null, isType(
                                DATATYPE_DATETIME)) + HtmlUtils.space(4)
                                    + msgLabel("Or") + dateSelectInput;
        } else if (isType(DATATYPE_BOOLEAN)) {
            widget = HtmlUtils.select(
                searchArg,
                Misc.newList(TypeHandler.ALL_OBJECT, "True", "False"),
                request.getString(searchArg, ""),
                HtmlUtils.cssClass("search-select"));
            //        } else if (isType(DATATYPE_ENUMERATION)) {
            //            List tmpValues = Misc.newList(TypeHandler.ALL_OBJECT);
            //            tmpValues.addAll(enumValues);
            //            widget = HtmlUtils.select(searchArg, tmpValues, request.getString(searchArg));
        } else if (isType(DATATYPE_ENUMERATIONPLUS)
                   || isType(DATATYPE_ENUMERATION)) {
            List tmpValues;
            if (searchRows > 1) {
                tmpValues = new ArrayList();
            } else {
                tmpValues = Misc.newList(TypeHandler.ALL_OBJECT);
            }
            List<TwoFacedObject> values = typeHandler.getEnumValues(request,
                                              this, entry);
            for (TwoFacedObject o : values) {
                TwoFacedObject tfo = null;
                if (enumValues != null) {
                    tfo = TwoFacedObject.findId(o.getId(), enumValues);
                }
                if (tfo != null) {
                    tmpValues.add(tfo);
                } else {
                    String label = getEnumLabel("" + o, false);
                    tmpValues.add(new TwoFacedObject(label, o));
                }
            }

            //            String selectExtra = HtmlUtils.id(searchArg + "_id");
            String selectExtra = "";
            if (searchRows > 1) {
                selectExtra += " multiple rows=\"" + searchRows + "\" ";
            } else {
                selectExtra += HtmlUtils.cssClass("search-select");
            }

            //            System.err.println(getName() + " values=" + tmpValues);
            widget = "";
            for (int i = 0; i < numberOfSearchWidgets; i++) {
                String arg = searchArg + ((i == 0)
                                          ? ""
                                          : "" + i);
                widget += HtmlUtils.select(
                    arg, tmpValues,
                    request.get(arg, new ArrayList<String>()),
                    selectExtra + ((i == 0)
                                   ? HU.attr("id", widgetId)
                                   : ""));
                widget += " ";
            }

        } else if (isNumeric()) {
            String toId = Utils.makeID(searchArg + "_to");
            String expr = HtmlUtils.select(
                              searchArg + "_expr", EXPR_ITEMS,
                              request.getString(searchArg + "_expr", ""),
                              HU.attr("to-id", toId)
                              + HtmlUtils.cssClass(
                                  "search-select ramadda-range-select"));
            widget = expr
                     + HtmlUtils.input(searchArg + "_from",
                                       request.getString(searchArg + "_from",
                                           ""), ((placeholderMin != null)
                    ? HU.attr("placeholder", placeholderMin)
                    : "") + HU.attr("size", "10")) + " "
                    + HtmlUtils.input(searchArg + "_to",
                                      request.getString(searchArg + "_to",
                                          ""), ((placeholderMax != null)
                    ? HU.attr("placeholder", placeholderMax)
                    : "") + HU.attr("id", toId) + HU.attr("size", "10"));
        } else if (isType(DATATYPE_ENTRY)) {
            String entryId  = request.getString(searchArg + "_hidden", "");
            Entry  theEntry = null;
            if ((entryId != null) && (entryId.length() > 0)) {
                try {
                    theEntry =
                        getRepository().getEntryManager().getEntry(null,
                            entryId);
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
            }

            String select =
                getRepository().getHtmlOutputHandler().getSelect(request,
                    searchArg, "Select", true, null, entry);
            StringBuffer sb = new StringBuffer();
            sb.append(HtmlUtils.hidden(searchArg + "_hidden", entryId,
                                       HtmlUtils.id(searchArg + "_hidden")));
            sb.append(HtmlUtils.disabledInput(searchArg, ((theEntry != null)
                    ? theEntry.getFullName()
                    : ""), HtmlUtils.id(searchArg)
                           + HtmlUtils.SIZE_60) + select);

            widget = sb.toString();
        } else {
            if (searchType.equals(SEARCHTYPE_SELECT)) {
                long t1 = System.currentTimeMillis();
                Statement statement = typeHandler.select(request,
                                          SqlUtil.distinct(columnName), tmp,
                                          "");
                long t2 = System.currentTimeMillis();
                String[] values = SqlUtil.readString(
                                      getDatabaseManager().getIterator(
                                          statement), 1);
                long t3 = System.currentTimeMillis();
                //                System.err.println("TIME:" + (t2-t1) + " " + (t3-t2));
                List<TwoFacedObject> list = new ArrayList();
                for (int i = 0; i < values.length; i++) {
                    if (values[i] == null) {
                        continue;
                    }
                    list.add(new TwoFacedObject(getLabel(values[i]),
                            values[i]));
                }

                List sorted = Misc.sort(list);
                list = new ArrayList<TwoFacedObject>();
                list.addAll(sorted);
                if (list.size() == 1) {
                    widget = HtmlUtils.hidden(searchArg,
                            (String) list.get(0).getId()) + " "
                                + list.get(0).toString();
                } else {
                    list.add(0, TypeHandler.ALL_OBJECT);
                    widget = HtmlUtils.select(searchArg, list);
                }
                //            } else if (rows > 1) {
                //                widget = HtmlUtils.textArea(searchArg, request.getString(searchArg, ""),
                //                                           rows, columns);
            } else {

                String text = request.getString(searchArg, "");
                text = text.replaceAll("\"", "&quot;");
                //                String text  = Utils.unquote(request.getString(searchArg, ""));

                boolean isList = isType(DATATYPE_LIST);
                String  attrs  = HU.attr("id", widgetId);
                if (placeholder != null) {
                    attrs += HU.attr("placeholder", placeholder);
                }
                if (isList) {
                    widget = HtmlUtils.textArea(searchArg, text, 5, 20,
                            attrs);
                } else {
                    widget = HtmlUtils.input(searchArg, text,
                                             HtmlUtils.SIZE_20 + attrs);
                }
            }
        }

        if (addNot) {
            widget += " "
                      + HU.labeledCheckbox(searchArg + "_not", "true",
                                           request.get(searchArg + "_not",
                                               false), "Not");
        }
        if (lookupDB != null) {
            //This uses the plugins/db plugin
            List<String> toks       = StringUtil.splitUpTo(lookupDB, ":", 2);
            String       otherTable = toks.get(0);
            String       otherCol   = (toks.size() == 2)
                                      ? toks.get(1)
                                      : getName();
            String extraLink = "DB.doDbSearch(" + HU.squote(entry.getName())
                               + "," + HU.squote(this.getName()) + ","
                               + HU.squote(widgetId) + ","
                               + HU.squote(otherTable) + ","
                               + HU.squote(otherCol) + ");";
            //              System.err.println(extraLink);
            widget += " "
                      + HU.href("javascript:" + extraLink,
                                "Lookup " + getName());
        }

        if (addFileToSearch) {
            boolean visible = request.exists(searchArg + "_file");
            String  file    = "";
            if (visible) {
                file +=
                    "<div class=ramadda-note>Note: You need to specify the file again for subsequent searches</div>";
            }
            file +=
                HU.fileInput(searchArg + "_file", "")
                + " File contains search values, one per line. Must be exact match.";
            widget += HU.makeShowHideBlock("File...", file, visible);
        }


        formBuffer.append(typeHandler.formEntry(request, getLabel() + ":",
                "<table cellspacing=0 cellpadding=0 border=0>"
                + HtmlUtils.row(HtmlUtils.cols(widget, suffix))
                + "</table>"));
        formBuffer.append("\n");
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
    protected String getLabel(String value) throws Exception {
        String desc = getRepository().getFieldDescription(value + ".label",
                          propertiesFile);
        if (desc == null) {
            desc = value;
        } else {
            if (desc.indexOf("${value}") >= 0) {
                desc = desc.replace("${value}", value);
            }
        }

        return desc;

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getFullName() {
        if (fullName == null) {
            fullName = getTableName() + "." + name;
        }

        return fullName;
    }


    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getColumnNames() {
        List<String> names = null;
        if (names == null) {
            names = new ArrayList<String>();
            if (isType(DATATYPE_LATLON)) {
                names.add(name + "_lat");
                names.add(name + "_lon");
            } else if (isType(DATATYPE_LATLONBBOX)) {
                names.add(name + "_north");
                names.add(name + "_west");
                names.add(name + "_south");
                names.add(name + "_east");
            } else {
                names.add(name);
            }
        }

        return names;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getSortByColumn() {
        if (isType(DATATYPE_LATLON)) {
            return name + "_lat";
        }
        if (isType(DATATYPE_LATLONBBOX)) {
            return name + "_north";
        }

        return name;
    }


    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getGroup() {
        return group;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getPropertiesFile() {
        return propertiesFile;
    }

    /**
     * Set the Label property.
     *
     * @param value The new value for Label
     */
    public void setLabel(String value) {
        label = value;
    }

    /**
     * Get the Label property.
     *
     * @return The Label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set the Description property.
     *
     * @param value The new value for Description
     */
    public void setDescription(String value) {
        description = value;
    }

    /**
     * Get the Description property.
     *
     * @return The Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the Type property.
     *
     * @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    /**
     * Get the Type property.
     *
     * @return The Type
     */
    public String getType() {
        return type;
    }

    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public boolean isField(String name) {
        return Misc.equals(this.name, name) || Misc.equals(this.label, name);
    }

    /**
     * Set the IsIndex property.
     *
     * @param value The new value for IsIndex
     */
    public void setIsIndex(boolean value) {
        isIndex = value;
    }

    /**
     * Get the IsIndex property.
     *
     * @return The IsIndex
     */
    public boolean getIsIndex() {
        return isIndex;
    }



    /**
     * Set the IsCategory property.
     *
     * @param value The new value for IsCategory
     */
    public void setIsCategory(boolean value) {
        isCategory = value;
    }

    /**
     * Get the IsCategory property.
     *
     * @return The IsCategory
     */
    public boolean getIsCategory() {
        return isCategory;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getShowEmpty() {
        return showEmpty;
    }



    /**
     *  Set the CanShow property.
     *
     *  @param value The new value for CanShow
     */
    public void setCanShow(boolean value) {
        canShow = value;
    }

    /**
     *  Get the CanShow property.
     *
     *  @return The CanShow
     */
    public boolean getCanShow() {
        if (isType(DATATYPE_PASSWORD)) {
            return false;
        }

        return canShow;
    }





    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getCanExport() {
        return canExport;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getShowLabel() {
        return showLabel;
    }


    /**
     * Set the IsSearchable property.
     *
     * @param value The new value for IsSearchable
     */
    public void setCanSearch(boolean value) {
        canSearch = value;
    }

    /**
     * Get the IsSearchable property.
     *
     * @return The IsSearchable
     */
    public boolean getCanSearch() {
        return canSearch;
    }

    /**
     *
     * @return _more_
     */
    public boolean getCanSort() {
        return canSort;
    }

    /**
     * Set the SearchRows property.
     *
     * @param value The new value for SearchRows
     */
    public void setSearchRows(int value) {
        searchRows = value;
    }

    /**
     * Get the SearchRows property.
     *
     * @return The SearchRows
     */
    public int getSearchRows() {
        return searchRows;
    }




    /**
     * Set the IsSearchable property.
     *
     * @param value The new value for IsSearchable
     */
    public void setCanSearchText(boolean value) {
        canSearchText = value;
    }

    /**
     * Get the IsSearchable property.
     *
     * @return The IsSearchable
     */
    public boolean getCanSearchText() {
        return canSearchText;
    }

    /**
     * Get the IsSearchable property.
     *
     * @return The IsSearchable
     */
    public boolean getAdvancedSearch() {
        return advancedSearch;
    }




    /**
     * Set the IsListable property.
     *
     * @param value The new value for IsListable
     */
    public void setCanList(boolean value) {
        canList = value;
    }

    /**
     * Get the IsListable property.
     *
     * @return The IsListable
     */
    public boolean getCanList() {
        return canList;
    }


    /**
     * _more_
     *
     * @param value _more_
     */
    public void setCanDisplay(boolean value) {
        canDisplay = value;
    }

    /**
     * Get the IsDisplayable property.
     *
     * @return The IsDisplayable
     */
    public boolean getCanDisplay() {
        return canDisplay;
    }



    /**
     * Set the Values property.
     *
     * @param value The new value for Values
     */
    public void setValues(List<TwoFacedObject> value) {
        enumValues = value;
    }

    /**
     * Get the Values property.
     *
     * @return The Values
     */
    public List<TwoFacedObject> getValues() {
        return enumValues;
    }

    /**
     * Set the Dflt property.
     *
     * @param value The new value for Dflt
     */
    public void setDflt(String value) {
        dflt = value;
    }

    /**
     * Get the Dflt property.
     *
     * @return The Dflt
     */
    public String getDflt() {
        return dflt;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return name;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getRows() {
        return rows;
    }

    /**
     *  Set the Size property.
     *
     *  @param value The new value for Size
     */
    public void setSize(int value) {
        size = value;
    }

    /**
     *  Get the Size property.
     *
     *  @return The Size
     */
    public int getSize() {
        return size;
    }


    /**
     *  Set the Editable property.
     *
     *  @param value The new value for Editable
     */
    public void setEditable(boolean value) {
        editable = value;
    }

    /**
     *  Get the Editable property.
     *
     *  @return The Editable
     */
    public boolean getEditable() {
        return editable;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getShowInForm() {
        return showInForm;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getHelp() {
        return help;
    }



    /**
     * _more_
     *
     * @param node _more_
     * @param attrOrTag _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getAttributeOrTag(Element node, String attrOrTag,
                                    String dflt)
            throws Exception {
        String attrValue = Utils.getAttributeOrTag(node, attrOrTag,
                               (String) null);
        if (attrValue == null) {
            attrValue = XmlUtil.getAttributeFromTree(node, attrOrTag);
        }

        if (attrValue != null) {
            properties.put(attrOrTag, attrValue);

            return attrValue;
        }

        return dflt;
    }

    /**
     * _more_
     *
     * @param node _more_
     * @param attrOrTag _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getAttributeOrTagOrFromType(Element node, String attrOrTag,
            String dflt)
            throws Exception {
        String s = getAttributeOrTag(node, attrOrTag, (String) null);
        if (s == null) {
            s = typeHandler.getTypeProperty(getName() + "." + attrOrTag,
                                            dflt);
            if (getName().equals("fields")) {
                System.err.println(getName() + " type = " + typeHandler);
            }
        }
        if (s != null) {
            System.err.println(getName() + " ." + attrOrTag + " =" + s);
        }

        return s;
    }



    /**
     * _more_
     *
     * @param node _more_
     * @param attrOrTag _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean getAttributeOrTag(Element node, String attrOrTag,
                                      boolean dflt)
            throws Exception {
        String attrValue = getAttributeOrTag(node, attrOrTag, (String) null);
        if (attrValue == null) {
            return dflt;
        }

        return attrValue.equals("true");
    }


    /**
     * _more_
     *
     * @param node _more_
     * @param attrOrTag _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private int getAttributeOrTag(Element node, String attrOrTag, int dflt)
            throws Exception {
        String attrValue = getAttributeOrTag(node, attrOrTag, (String) null);
        if (attrValue == null) {
            return dflt;
        }

        return new Integer(attrValue).intValue();
    }


    /**
     * _more_
     *
     * @param node _more_
     * @param attrOrTag _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public double getAttributeOrTag(Element node, String attrOrTag,
                                    double dflt)
            throws Exception {
        String attrValue = getAttributeOrTag(node, attrOrTag, (String) null);
        if (attrValue == null) {
            return dflt;
        }

        return new Double(attrValue).doubleValue();
    }

    /**
     * Set the ColumnIndex property.
     *
     * @param value The new value for ColumnIndex
     */
    public void setColumnIndex(int value) {
        columnIndex = value;
    }

    /**
     * Get the ColumnIndex property.
     *
     * @return The ColumnIndex
     */
    public int getColumnIndex() {
        return columnIndex;
    }


    /**
     *  Set the Alias property.
     *
     *  @param value The new value for Alias
     */
    public void setAlias(String value) {
        alias = value;
    }

    /**
     *  Get the Alias property.
     *
     *  @return The Alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Set the DoStats property.
     *
     * @param value The new value for DoStats
     */
    public void setDoStats(boolean value) {
        doStats = value;
    }

    /**
     * Get the DoStats property.
     *
     * @return The DoStats
     */
    public boolean getDoStats() {
        return doStats;
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Apr 29, '20
     * @author         Enter your name here...
     */
    public static class Display {

        /** _more_ */
        String value;

        /** _more_ */
        String background;



        /** _more_ */
        String color;

        /** _more_ */
        String mapFillColor;

        /** _more_ */
        String template;

        /** _more_ */
        String icon;

        /** _more_ */
        double min;

        /** _more_ */
        double max;

        /**
         * _more_
         *
         * @param element _more_
         */
        public Display(Element element) {
            value = XmlUtil.getAttribute(element, "value", "");
            background = XmlUtil.getAttribute(element, "background",
                    (String) null);
            color = XmlUtil.getAttribute(element, "color", (String) null);
            mapFillColor = XmlUtil.getAttribute(element, "mapFillColor",
                    (String) null);
            template = XmlUtil.getAttribute(element, "template",
                                            (String) null);
            icon = XmlUtil.getAttribute(element, "icon", (String) null);
            min  = XmlUtil.getAttribute(element, "min", Double.NaN);
            max  = XmlUtil.getAttribute(element, "max", Double.isNaN(min)
                    ? Double.NaN
                    : Double.MAX_VALUE);
        }

        /**
         * _more_
         *
         * @param attr _more_
         *
         * @return _more_
         */
        public String getAttribute(String attr) {
            if (attr.equals("mapFillColor")) {
                return mapFillColor;
            }
            if (attr.equals("background")) {
                return background;
            }
            if (attr.equals("color")) {
                return color;
            }

            return null;
        }


        /**
         * _more_
         *
         * @param v _more_
         *
         * @return _more_
         */
        public String decorate(String v) {
            String style = "";
            if (background != null) {
                style += "background:" + background + ";";
            }
            if (color != null) {
                style += "color:" + color + ";";
            }

            if (style.length() > 0) {
                v = HtmlUtils.div(v, HtmlUtils.style(style));
            }
            if (template != null) {
                v = template.replace("${value}", v);
            }

            return v;
        }
    }

    /**
     *  Get the Lookupdb property.
     *
     *  @return The Lookupdb
     */
    public String getLookupDB() {
        return lookupDB;
    }




}
