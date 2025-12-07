/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.record;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.MyDateFormat;
import org.ramadda.util.Utils;

import ucar.unidata.util.Misc;

import java.io.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

/**
 * Holds information about the record's parameters
 *
 *
 * @author Jeff McWhirter
 */
@SuppressWarnings("unchecked")
public class RecordField {
    public static final String PROP_CHARTABLE = "chartable";
    public static final String PROP_SEARCHABLE = "searchable";
    public static final String PROP_ISDATE = "isdate";
    public static final String PROP_ISTIME = "istime";
    public static final String PROP_SORTORDER = "sortorder";

    /** Latitude-Longitude-altitude properties */
    public static final String PROP_ISLATITUDE = "islatitude";
    public static final String PROP_ISLONGITUDE = "islongitude";
    public static final String PROP_ISALTITUDE = "isaltitude";

    /* For depth sometimes is reversed. 0 is the surface and goes on instead of -1 */
    public static final String PROP_ISALTITUDEREVERSE = "isaltitudereverse";
    public static final String PROP_SEARCH_SUFFIX = "search.suffix";
    public static final String PROP_BITFIELDS = "bitfields";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_URL = "url";
    public static final String TYPE_ENUMERATION = "enumeration";
    public static final String TYPE_MULTIENUMERATION = "multienumeration";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_MOVIE = "movie";
    public static final String TYPE_DATE = "date";
    public static final String TYPE_DOUBLE = "double";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_INT = "int";
    private boolean isTypeNumeric = true;
    private boolean isTypeString = false;
    private boolean isTypeEnumeration = false;
    private boolean isTypeDate = false;
    private boolean isDateOffset = false;
    private boolean isDate = false;
    private boolean isGroup = false;
    private int sortOrder = 0;
    private int index = -1;
    private int columnWidth = 0;
    private String group;
    private boolean isTime = false;
    private boolean isLatitude = false;
    private boolean isLongitude = false;
    private boolean isAltitude = false;
    private boolean isAltitudeReverse = false;
    private MyDateFormat dateFormat;
    private int utcOffset = 0;
    private double roundingFactor = 0;
    private double scale = 1.0;
    private double offset1 = 0.0;
    private double offset2 = 0.0;
    private String name;
    private String label;
    private String description;
    private Hashtable properties = new Hashtable();
    private int paramId;
    private String unit;
    private List<String[]> enumeratedValues;
    private String rawType;
    private String typeName;
    private int arity = 1;
    private ValueGetter valueGetter;
    private boolean skip = false;
    private boolean synthetic = false;
    private double defaultDoubleValue = Double.NaN;
    private String defaultStringValue = null;
    private String headerPattern = null;
    private String type = TYPE_DOUBLE;
    private double missingValue = Double.NaN;

    public static final RecordField FIELD_LATITUDE =
        new RecordField("recordLatitude", "Latitude", "", 0, "");

    public static final RecordField FIELD_LONGITUDE =
        new RecordField("recordLongitude", "Longitude", "", 0, "");

    public static final RecordField FIELD_ELEVATION =
        new RecordField("recordElevation", "Elevation", "", 0, "");

    public static final RecordField FIELD_DATE =
        new RecordField("recordDate", "Date", "", 0, "", TYPE_DATE, "Date",
                        0, false, false);

    public RecordField(String name, String label, String description,
                       String type, int paramId, String unit) {
        this.name        = name;
        this.label       = label;
        this.description = description;
        this.rawType     = type;
        setType(type);
        this.paramId = paramId;
        this.unit    = unit;
    }

    public RecordField(String name, String label, String description,
                       int paramId, String unit) {
        this.name        = name;
        this.label       = label;
        this.description = description;
        this.paramId     = paramId;
        this.unit        = unit;
    }

    public RecordField(String name, String label, String description,
                       int paramId, String unit, String rawType,
                       String typeName, int arity, boolean searchable,
                       boolean chartable) {
        this.name        = name;
        this.label       = label;
        this.description = description;
        this.paramId     = paramId;
        this.unit        = unit;
        this.type        = this.rawType = rawType;
        this.typeName    = typeName;
        this.arity       = arity;
        if (searchable) {
            properties.put(PROP_SEARCHABLE, "true");
        } else {
            properties.put(PROP_SEARCHABLE, "false");
        }
        if (chartable) {
            properties.put(PROP_CHARTABLE, "true");
        } else {
            properties.put(PROP_CHARTABLE, "false");
        }
    }

    public static void addJsonHeader(Appendable pw, String name,
                                     List<RecordField> fields,
                                     boolean addGeolocation,
                                     boolean addElevation, boolean addDate)
            throws Exception {
        pw.append(JsonUtil.mapOpen());
        pw.append(JsonUtil.attr(JsonUtil.FIELD_NAME, name, true));
        pw.append(",\n");
        pw.append(JsonUtil.attr(JsonUtil.FIELD_FIELDS,
                            RecordField.getJson(fields, addGeolocation,
                                addElevation, addDate)));
        pw.append(",\n");
        pw.append(JsonUtil.mapKey(JsonUtil.FIELD_DATA));
        pw.append(JsonUtil.listOpen());
    }

    public static void addJsonFooter(Appendable pw) throws Exception {
        pw.append(JsonUtil.listClose());
        pw.append(JsonUtil.mapClose());
    }

    public static String getJson(List<RecordField> fields,
                                 boolean addGeolocation,
                                 boolean addElevation, boolean addDate)
            throws Exception {
        Appendable   sb           = new StringBuffer();
        List<String> fieldStrings = new ArrayList<String>();
        int          headerCnt    = 0;
        StringBuffer fieldSB;
        for (RecordField field : fields) {
            if (field.getSynthetic()) {
                continue;
            }
            if (field.getArity() > 1) {
                continue;
            }
            fieldSB = new StringBuffer();
            field.addJson(fieldSB, headerCnt);
            fieldStrings.add(fieldSB.toString());
            headerCnt++;
            if (field.getIsLatitude()) {
                addGeolocation = false;
            }
            if (field.getIsAltitude()) {
                addElevation = false;
            }
        }
        if (addGeolocation) {
            fieldSB = new StringBuffer();
            FIELD_LATITUDE.addJson(fieldSB, headerCnt++);
            fieldStrings.add(fieldSB.toString());
            fieldSB = new StringBuffer();
            FIELD_LONGITUDE.addJson(fieldSB, headerCnt++);
            fieldStrings.add(fieldSB.toString());

        }
        if (addElevation) {
            fieldSB = new StringBuffer();
            FIELD_ELEVATION.addJson(fieldSB, headerCnt++);
            fieldStrings.add(fieldSB.toString());
        }
        if (addDate) {
            fieldSB = new StringBuffer();
            FIELD_DATE.addJson(fieldSB, headerCnt++);
            fieldStrings.add(fieldSB.toString());
        }

        return JsonUtil.list(fieldStrings);
    }

    public void addJson(StringBuffer sb, int index) {
        List<String> items    = new ArrayList<String>();
        String       dataType = type;
        items.add("index");
        items.add("" + index);
        items.add("id");
        items.add(HtmlUtils.quote(name));
        items.add("label");
        items.add(JsonUtil.quote(label));
        if (group != null) {
            items.add("group");
            items.add(JsonUtil.quote(group));
        }
        if (description != null) {
            items.add("description");
            items.add(JsonUtil.quote(description.replaceAll("\n", " ")));
        }
        if ((enumeratedValues != null) && (enumeratedValues.size() > 0)) {
            String       v   = "";
            List<String> tmp = new ArrayList<String>();
            for (String[] tuple : enumeratedValues) {
                tmp.add(tuple[0]);
                tmp.add(tuple[1]);
            }
            items.add("enumeratedValues");
            items.add(JsonUtil.mapAndQuote(tmp));
        }

        if (isGroup) {
            items.add("isGroup");
            items.add("true");
        }

        if (group != null) {
            items.add("group");
            items.add(JsonUtil.quote(group));
        }
        items.add("type");
        items.add(JsonUtil.quote(dataType));
        items.add("unit");
        items.add(Utils.stringDefined(unit)
                  ? JsonUtil.quote(unit)
                  : "null");

        String canEdit = (String) getProperty("canedit", null);
        if (canEdit != null) {
            items.add("canedit");
            items.add(JsonUtil.quote(canEdit));
        }
	Object values = getProperty("values", null);
        if (values != null) {
            items.add("values");
	    if(values instanceof List) values = Utils.join((List)values,",");
            items.add(JsonUtil.quote(values.toString()));
        }

        items.add("chartable");
        items.add("" + getChartable());
        items.add("sortorder");
        items.add("" + sortOrder);
        items.add("searchable");
        items.add("" + getSearchable());

        if (isLatitude || name.equals(FIELD_LATITUDE.getName())) {
            items.add("isLatitude");
            items.add("true");
        }
        if (isLongitude || name.equals(FIELD_LONGITUDE.getName())) {
            items.add("isLongitude");
            items.add("true");
        }
        if (name.equals(FIELD_ELEVATION.getName())) {
            items.add("isElevation");
            items.add("true");
        }

        if (name.equals(FIELD_DATE.getName())) {
            items.add("isDate");
            items.add("true");
        }

        sb.append(JsonUtil.map(items));
    }

    public String toString() {
        return name + "[" + type + "]: " + label;
    }

    public void setValueGetter(ValueGetter value) {
        valueGetter = value;
    }

    public ValueGetter getValueGetter() {
        return valueGetter;
    }

    private void attr(PrintWriter pw, String name, String value) {
        pw.print(name);
        pw.append("=\"");
        pw.print(value);
        pw.print("\" ");
    }

    public void printCsvHeader(VisitInfo visitInfo, PrintWriter pw) {
        pw.print(getName());
        pw.print("[");
        if ((unit != null) && (unit.length() > 0)) {
            attr(pw, "unit", unit);
        }
        if (arity > 1) {
            attr(pw, "size", "" + arity);
        }
        if (isTypeString) {
            attr(pw, "type", type);
        } else if (isTypeDate) {
            attr(pw, "type", TYPE_DATE);
            if (isDateOffset) {
                attr(pw, "isDateOffset", "true");
            }
        } else {
            //Default is numeric
        }

        if (Utils.stringDefined(label)) {
            attr(pw, "label", label);
        }

        pw.print("]");

    }

    public List<String[]> getEnumeratedValues() {
        return enumeratedValues;
    }

    public void setEnumeratedValues(List<String[]> enums) {
        this.enumeratedValues = enums;
    }

    public void setIsGroup(boolean value) {
        isGroup = value;
    }

    public boolean getIsGroup() {
        return isGroup;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getName() {
        return this.name;
    }

    public void setLabel(String value) {
        this.label = value;
    }

    public String getLabel() {
        return this.label;
    }

    public void setDescription(String value) {
        this.description = value;
    }

    public String getDescription() {
        return this.description;
    }

    public void setUnit(String value) {
        this.unit = value;
    }

    public String getUnit() {
        return this.unit;
    }

    public void setParamId(int value) {
        this.paramId = value;
    }

    public int getParamId() {
        return this.paramId;
    }

    public void setSearchable(boolean value) {
        properties.put(PROP_SEARCHABLE, value + "");
    }

    public boolean getSearchable() {
        String v = (String) properties.get(PROP_SEARCHABLE);
	if(v==null) return true;
        if (!v.equals("true")) {
            return false;
        }

        return true;
    }

    public boolean isBitField() {
        return properties.get(PROP_BITFIELDS) != null;
    }

    public String[] getBitFields() {
        String s = (String) properties.get(PROP_BITFIELDS);
        if (s == null) {
            return null;
        }

        return s.split(",");
    }

    public String getSearchSuffix() {
        return (String) properties.get(PROP_SEARCH_SUFFIX);
    }

    public void setChartable(boolean value) {
        properties.put(PROP_CHARTABLE, value + "");
    }

    public void setGroup(String value) {
        group = value;
    }

    public String getGroup() {
        return group;
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public Object getProperty(String key, Object dflt) {
        Object v = properties.get(key);
        if (v == null) {
            return dflt;
        }

        return v;
    }

    public boolean getChartable() {
        String v = (String) properties.get(PROP_CHARTABLE);
        //Default to true now
        if (v == null) {
            return true;
        }
        if ( !v.equals("true")) {
            return false;
        }

        return true;
    }

    public void setRawType(String value) {
        rawType = value;
    }

    public String getRawType() {
        return ((rawType != null) && (rawType.length() > 0))
               ? rawType
               : getTypeName();
    }

    public void setTypeName(String value) {
        typeName = value;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setArity(int value) {
        arity = value;
    }

    public int getArity() {
        return arity;
    }

    public double convertValue(double v) {
        //TODO: or is this the other way around
        return (v + offset1) * scale + offset2;
    }
    public void setScale(double value) {
        scale = value;
    }

    public double getScale() {
        return scale;
    }

    public void setOffset1(double value) {
        offset1 = value;
    }

    public double getOffset1() {
        return offset1;
    }

    public void setOffset2(double value) {
        offset2 = value;
    }

    public double getOffset2() {
        return offset2;
    }

    public void setRoundingFactor(double value) {
        roundingFactor = value;
    }

    public double getRoundingFactor() {
        return roundingFactor;
    }

    public void setSkip(boolean value) {
        skip = value;
    }

    public boolean getSkip() {
        return skip;
    }

    public void setSynthetic(boolean value) {
        synthetic = value;
    }

    public boolean getSynthetic() {
        return synthetic;
    }

    public boolean hasDefaultValue() {
        return hasDefaultDoubleValue() || hasDefaultStringValue();
    }

    public boolean hasDefaultDoubleValue() {
        return !Double.isNaN(defaultDoubleValue);
    }

    public boolean hasDefaultStringValue() {
        return defaultStringValue != null;
    }

    public void setDefaultDoubleValue(double value) {
        defaultDoubleValue = value;
    }

    public double getDefaultDoubleValue() {
        return defaultDoubleValue;
    }

    public void setDefaultStringValue(String value) {
        defaultStringValue = value;
    }

    public String getDefaultStringValue() {
        return defaultStringValue;
    }

    public void setType(String value) {
        type = value;
        isTypeNumeric = value.equals("numeric") || value.equals("integer")
                        || value.equals(TYPE_DOUBLE)
                        || value.equals(TYPE_INT);
        isTypeString = value.equals(TYPE_STRING) || value.equals(TYPE_URL)
                       || value.equals(TYPE_BOOLEAN)
                       || value.equals(TYPE_IMAGE)
                       || value.equals(TYPE_MOVIE)
                       || value.equals(TYPE_MULTIENUMERATION)
                       || value.equals(TYPE_ENUMERATION);
        isTypeEnumeration =  value.equals(TYPE_MULTIENUMERATION)
	    || value.equals(TYPE_ENUMERATION);	
        isTypeDate = value.equals(TYPE_DATE);
        //      System.err.println("set type:" + type + " is:" +isTypeNumeric +" " + isTypeString +" " + isTypeDate);
    }

    public String getType() {
        return type;
    }

    public boolean isTypeString() {
        return isTypeString;
    }

    public boolean isTypeInteger() {
        return (type.equals(TYPE_INT));
    }

    public boolean isTypeNumeric() {
        return isTypeNumeric;
    }

    public boolean isTypeEnumeration() {
        return isTypeEnumeration;
    }    

    public boolean isTypeDate() {
        return isTypeDate;
    }

    public void setDateFormat(MyDateFormat value) {
        dateFormat = value;
    }

    public MyDateFormat getDateFormat() {
        return dateFormat;
    }

    public void setUtcOffset(int value) {
        utcOffset = value;
    }

    public int getUtcOffset() {
        return utcOffset;
    }

    public void setMissingValue(double value) {
        missingValue = value;
    }

    public double getMissingValue() {
        return missingValue;
    }

    public void setHeaderPattern(String value) {
        headerPattern = value;
    }

    public String getHeaderPattern() {
        return headerPattern;
    }

    public void setIsDate(boolean value) {
        isDate = value;
    }

    public boolean getIsDate() {
        return isDate;
    }

    public void setIsTime(boolean value) {
        isTime = value;
    }

    public boolean getIsTime() {
        return isTime;
    }

    /* Adding other dimensions latitude longitude */

    public void setIsLatitude(boolean value) {
        isLatitude = value;
    }

    public void setIsLongitude(boolean value) {
        isLongitude = value;
    }

    public void setIsAltitude(boolean value) {
        isAltitude = value;
    }

    public void setIsAltitudeReverse(boolean value) {
        isAltitudeReverse = value;
    }

    public boolean getIsLatitude() {
        return isLatitude;
    }

    public boolean getIsLongitude() {
        return isLongitude;
    }

    public boolean getIsAltitude() {
        return isAltitude;
    }

    public boolean getIsAltitudeReverse() {
        return isAltitudeReverse;
    }

    public void setSortOrder(int value) {
        sortOrder = value;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setColumnWidth(int value) {
        columnWidth = value;
    }

    public int getColumnWidth() {
        return columnWidth;
    }

    public void setIndex(int value) {
        index = value;
    }

    public int getIndex() {
        return index;
    }

    public void setIsDateOffset(boolean value) {
        isDateOffset = value;
    }

    public boolean getIsDateOffset() {
        return isDateOffset;
    }

}
