/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.record;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;

import ucar.unidata.util.Misc;

import java.io.*;

import java.text.SimpleDateFormat;

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


    /** _more_ */
    public static final String PROP_CHARTABLE = "chartable";

    /** _more_ */
    public static final String PROP_SEARCHABLE = "searchable";

    /** _more_ */
    public static final String PROP_ISDATE = "isdate";

    /** _more_ */
    public static final String PROP_ISTIME = "istime";

    /** _more_ */
    public static final String PROP_SORTORDER = "sortorder";


    /** Latitude-Longitude-altitude properties */
    public static final String PROP_ISLATITUDE = "islatitude";

    /** _more_ */
    public static final String PROP_ISLONGITUDE = "islongitude";

    /** _more_ */
    public static final String PROP_ISALTITUDE = "isaltitude";

    /* For depth sometimes is reversed. 0 is the surface and goes on instead of -1 */

    /** _more_ */
    public static final String PROP_ISALTITUDEREVERSE = "isaltitudereverse";


    /** _more_ */
    public static final String PROP_SEARCH_SUFFIX = "search.suffix";

    /** _more_ */
    public static final String PROP_BITFIELDS = "bitfields";


    /** _more_ */
    public static final String TYPE_STRING = "string";

    /** _more_ */
    public static final String TYPE_URL = "url";

    /** _more_ */
    public static final String TYPE_ENUMERATION = "enumeration";

    /**  */
    public static final String TYPE_MULTIENUMERATION = "multienumeration";

    /** _more_ */
    public static final String TYPE_IMAGE = "image";

    /** _more_ */
    public static final String TYPE_MOVIE = "movie";

    /** _more_ */
    public static final String TYPE_DATE = "date";


    /** _more_ */
    public static final String TYPE_DOUBLE = "double";

    /**  */
    public static final String TYPE_BOOLEAN = "boolean";

    /** _more_ */
    public static final String TYPE_INT = "int";

    /** _more_ */
    private boolean isTypeNumeric = true;

    /** _more_ */
    private boolean isTypeString = false;
    private boolean isTypeEnumeration = false;

    /** _more_ */
    private boolean isTypeDate = false;

    /** _more_ */
    private boolean isDateOffset = false;

    /** _more_ */
    private boolean isDate = false;


    /** _more_ */
    private boolean isGroup = false;

    /** _more_ */
    private int sortOrder = 0;

    /** _more_ */
    private int index = -1;

    /** _more_ */
    private int columnWidth = 0;

    /**  */
    private String group;

    /** _more_ */
    private boolean isTime = false;

    /** _more_ */
    private boolean isLatitude = false;

    /** _more_ */
    private boolean isLongitude = false;

    /** _more_ */
    private boolean isAltitude = false;

    /** _more_ */
    private boolean isAltitudeReverse = false;

    /** _more_ */
    private SimpleDateFormat dateFormat;

    /** _more_ */
    private String sDateFormat;


    /** _more_ */
    private int utcOffset = 0;

    /** _more_ */
    private double roundingFactor = 0;

    /** _more_ */
    private double scale = 1.0;

    /** _more_ */
    private double offset1 = 0.0;

    /** _more_ */
    private double offset2 = 0.0;

    /** _more_ */
    private String name;

    /** _more_ */
    private String label;

    /** _more_ */
    private String description;

    /** _more_ */
    private Hashtable properties = new Hashtable();

    /** _more_ */
    private int paramId;

    /** _more_ */
    private String unit;

    /** _more_ */
    private List<String[]> enumeratedValues;

    /** _more_ */
    private String rawType;

    /** _more_ */
    private String typeName;

    /** _more_ */
    private int arity = 1;

    /** _more_ */
    private ValueGetter valueGetter;

    /** _more_ */
    private boolean skip = false;

    /** _more_ */
    private boolean synthetic = false;

    /** _more_ */
    private double defaultDoubleValue = Double.NaN;

    /** _more_ */
    private String defaultStringValue = null;

    /** _more_ */
    private String headerPattern = null;

    /** _more_ */
    private String type = TYPE_DOUBLE;

    /** _more_ */
    private double missingValue = Double.NaN;


    /** _more_ */
    public static final RecordField FIELD_LATITUDE =
        new RecordField("recordLatitude", "Latitude", "", 0, "");

    /** _more_ */
    public static final RecordField FIELD_LONGITUDE =
        new RecordField("recordLongitude", "Longitude", "", 0, "");

    /** _more_ */
    public static final RecordField FIELD_ELEVATION =
        new RecordField("recordElevation", "Elevation", "", 0, "");

    /** _more_ */
    public static final RecordField FIELD_DATE =
        new RecordField("recordDate", "Date", "", 0, "", TYPE_DATE, "Date",
                        0, false, false);



    /**
     * _more_
     *
     * @param name _more_
     * @param label _more_
     * @param description _more_
     * @param type _more_
     * @param paramId _more_
     * @param unit _more_
     */
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



    /**
     * _more_
     *
     * @param name _more_
     * @param label _more_
     * @param description _more_
     * @param paramId _more_
     * @param unit _more_
     */
    public RecordField(String name, String label, String description,
                       int paramId, String unit) {
        this.name        = name;
        this.label       = label;
        this.description = description;
        this.paramId     = paramId;
        this.unit        = unit;
    }



    /**
     * _more_
     *
     * @param name _more_
     * @param label _more_
     * @param description _more_
     * @param paramId _more_
     * @param unit _more_
     * @param rawType _more_
     * @param typeName _more_
     * @param arity _more_
     * @param searchable _more_
     * @param chartable _more_
     */
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



    /**
     * _more_
     *
     * @param pw _more_
     * @param name _more_
     * @param fields _more_
     * @param addGeolocation _more_
     * @param addElevation _more_
     * @param addDate _more_
     *
     * @throws Exception _more_
     */
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

    /**
     * _more_
     *
     * @param pw _more_
     *
     * @throws Exception _more_
     */
    public static void addJsonFooter(Appendable pw) throws Exception {
        pw.append(JsonUtil.listClose());
        pw.append(JsonUtil.mapClose());
    }

    /**
     * _more_
     *
     * @param fields _more_
     * @param addGeolocation _more_
     * @param addElevation _more_
     * @param addDate _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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



    /**
     * _more_
     *
     * @param sb _more_
     * @param index _more_
     */
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


    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return name + "[" + type + "]: " + label;
    }


    /**
     *  Set the ValueGetter property.
     *
     *  @param value The new value for ValueGetter
     */
    public void setValueGetter(ValueGetter value) {
        valueGetter = value;
    }

    /**
     *  Get the ValueGetter property.
     *
     *  @return The ValueGetter
     */
    public ValueGetter getValueGetter() {
        return valueGetter;
    }


    /**
     * _more_
     *
     * @param pw _more_
     * @param name _more_
     * @param value _more_
     */
    private void attr(PrintWriter pw, String name, String value) {
        pw.print(name);
        pw.append("=\"");
        pw.print(value);
        pw.print("\" ");
    }


    /**
     * _more_
     *
     *
     * @param visitInfo _more_
     * @param pw _more_
     */
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

    /**
     * _more_
     *
     * @return _more_
     */
    public List<String[]> getEnumeratedValues() {
        return enumeratedValues;
    }


    /**
     * _more_
     *
     * @param enums _more_
     */
    public void setEnumeratedValues(List<String[]> enums) {
        this.enumeratedValues = enums;
    }


    /**
     * Set the IsGroup property.
     *
     * @param value The new value for IsGroup
     */
    public void setIsGroup(boolean value) {
        isGroup = value;
    }

    /**
     * Get the IsGroup property.
     *
     * @return The IsGroup
     */
    public boolean getIsGroup() {
        return isGroup;
    }




    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return this.name;
    }

    /**
     *  Set the Label property.
     *
     *  @param value The new value for Label
     */
    public void setLabel(String value) {
        this.label = value;
    }

    /**
     *  Get the Label property.
     *
     *  @return The Label
     */
    public String getLabel() {
        return this.label;
    }


    /**
     *  Set the Description property.
     *
     *  @param value The new value for Description
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     *  Get the Description property.
     *
     *  @return The Description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     *  Set the Unit property.
     *
     *  @param value The new value for Unit
     */
    public void setUnit(String value) {
        this.unit = value;
    }

    /**
     *  Get the Unit property.
     *
     *  @return The Unit
     */
    public String getUnit() {
        return this.unit;
    }



    /**
     *  Set the ParamId property.
     *
     *  @param value The new value for ParamId
     */
    public void setParamId(int value) {
        this.paramId = value;
    }

    /**
     *  Get the ParamId property.
     *
     *  @return The ParamId
     */
    public int getParamId() {
        return this.paramId;
    }

    /**
     *  Set the Searchable property.
     *
     *  @param value The new value for Searchable
     */
    public void setSearchable(boolean value) {
        properties.put(PROP_SEARCHABLE, value + "");
    }

    /**
     *  Get the Searchable property.
     *
     *  @return The Searchable
     */
    public boolean getSearchable() {
        String v = (String) properties.get(PROP_SEARCHABLE);
	if(v==null) return true;
        if (!v.equals("true")) {
            return false;
        }

        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isBitField() {
        return properties.get(PROP_BITFIELDS) != null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String[] getBitFields() {
        String s = (String) properties.get(PROP_BITFIELDS);
        if (s == null) {
            return null;
        }

        return s.split(",");
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getSearchSuffix() {
        return (String) properties.get(PROP_SEARCH_SUFFIX);
    }

    /**
     *  Set the Chartable property.
     *
     *  @param value The new value for Chartable
     */
    public void setChartable(boolean value) {
        properties.put(PROP_CHARTABLE, value + "");
    }

    /**
     * Set the Group property.
     *
     * @param value The new value for Group
     */
    public void setGroup(String value) {
        group = value;
    }

    /**
     * Get the Group property.
     *
     * @return The Group
     */
    public String getGroup() {
        return group;
    }



    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public Object getProperty(String key, Object dflt) {
        Object v = properties.get(key);
        if (v == null) {
            return dflt;
        }

        return v;
    }



    /**
     * _more_
     *
     * @return _more_
     */
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



    /**
     * Set the RawType property.
     *
     * @param value The new value for RawType
     */
    public void setRawType(String value) {
        rawType = value;
    }

    /**
     * Get the RawType property.
     *
     * @return The RawType
     */
    public String getRawType() {
        return ((rawType != null) && (rawType.length() > 0))
               ? rawType
               : getTypeName();
    }



    /**
     * Set the TypeName property.
     *
     * @param value The new value for TypeName
     */
    public void setTypeName(String value) {
        typeName = value;
    }

    /**
     * Get the TypeName property.
     *
     * @return The TypeName
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Set the Arity property.
     *
     * @param value The new value for Arity
     */
    public void setArity(int value) {
        arity = value;
    }

    /**
     * Get the Arity property.
     *
     * @return The Arity
     */
    public int getArity() {
        return arity;
    }


    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    public double convertValue(double v) {
        //TODO: or is this the other way around
        return (v + offset1) * scale + offset2;
    }


    /**
     *  Set the Scale property.
     *
     *  @param value The new value for Scale
     */
    public void setScale(double value) {
        scale = value;
    }

    /**
     *  Get the Scale property.
     *
     *  @return The Scale
     */
    public double getScale() {
        return scale;
    }

    /**
     *  Set the Offset1 property.
     *
     *  @param value The new value for Offset1
     */
    public void setOffset1(double value) {
        offset1 = value;
    }

    /**
     *  Get the Offset1 property.
     *
     *  @return The Offset1
     */
    public double getOffset1() {
        return offset1;
    }

    /**
     *  Set the Offset2 property.
     *
     *  @param value The new value for Offset2
     */
    public void setOffset2(double value) {
        offset2 = value;
    }

    /**
     *  Get the Offset2 property.
     *
     *  @return The Offset2
     */
    public double getOffset2() {
        return offset2;
    }




    /**
     * Set the RoundingFactor property.
     *
     * @param value The new value for RoundingFactor
     */
    public void setRoundingFactor(double value) {
        roundingFactor = value;
    }

    /**
     * Get the RoundingFactor property.
     *
     * @return The RoundingFactor
     */
    public double getRoundingFactor() {
        return roundingFactor;
    }



    /**
     *  Set the Skip property.
     *
     *  @param value The new value for Skip
     */
    public void setSkip(boolean value) {
        skip = value;
    }

    /**
     *  Get the Skip property.
     *
     *  @return The Skip
     */
    public boolean getSkip() {
        return skip;
    }

    /**
     *  Set the Synthetic property.
     *
     *  @param value The new value for Synthetic
     */
    public void setSynthetic(boolean value) {
        synthetic = value;
    }

    /**
     *  Get the Synthetic property.
     *
     *  @return The Synthetic
     */
    public boolean getSynthetic() {
        return synthetic;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasDefaultValue() {
        return hasDefaultDoubleValue() || hasDefaultStringValue();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasDefaultDoubleValue() {
        return !Double.isNaN(defaultDoubleValue);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasDefaultStringValue() {
        return defaultStringValue != null;
    }


    /**
     *  Set the DefaultValue property.
     *
     *  @param value The new value for DefaultValue
     */
    public void setDefaultDoubleValue(double value) {
        defaultDoubleValue = value;
    }

    /**
     *  Get the DefaultValue property.
     *
     *  @return The DefaultValue
     */
    public double getDefaultDoubleValue() {
        return defaultDoubleValue;
    }

    /**
     *  Set the DefaultStringValue property.
     *
     *  @param value The new value for DefaultStringValue
     */
    public void setDefaultStringValue(String value) {
        defaultStringValue = value;
    }

    /**
     *  Get the DefaultStringValue property.
     *
     *  @return The DefaultStringValue
     */
    public String getDefaultStringValue() {
        return defaultStringValue;
    }




    /**
     *  Set the Type property.
     *
     *  @param value The new value for Type
     */
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

    /**
     *  Get the Type property.
     *
     *  @return The Type
     */
    public String getType() {
        return type;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isTypeString() {
        return isTypeString;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isTypeInteger() {
        return (type.equals(TYPE_INT));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isTypeNumeric() {
        return isTypeNumeric;
    }

    public boolean isTypeEnumeration() {
        return isTypeEnumeration;
    }    

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isTypeDate() {
        return isTypeDate;
    }

    /**
     *  Set the DateFormat property.
     *
     *  @param value The new value for DateFormat
     */
    public void setDateFormat(SimpleDateFormat value) {
        dateFormat = value;
    }

    /**
     * _more_
     *
     * @param value _more_
     * @param fmt _more_
     */
    public void setDateFormat(SimpleDateFormat value, String fmt) {
        dateFormat  = value;
        sDateFormat = fmt;
    }

    /**
     * Set the SDateFormat property.
     *
     * @param value The new value for SDateFormat
     */
    public void setSDateFormat(String value) {
        sDateFormat = value;
    }

    /**
     * Get the SDateFormat property.
     *
     * @return The SDateFormat
     */
    public String getSDateFormat() {
        return sDateFormat;
    }




    /**
     *  Get the DateFormat property.
     *
     *  @return The DateFormat
     */
    public SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    /**
     * Set the UtcOffset property.
     *
     * @param value The new value for UtcOffset
     */
    public void setUtcOffset(int value) {
        utcOffset = value;
    }

    /**
     * Get the UtcOffset property.
     *
     * @return The UtcOffset
     */
    public int getUtcOffset() {
        return utcOffset;
    }




    /**
     *  Set the MissingValue property.
     *
     *  @param value The new value for MissingValue
     */
    public void setMissingValue(double value) {
        missingValue = value;
    }

    /**
     *  Get the MissingValue property.
     *
     *  @return The MissingValue
     */
    public double getMissingValue() {
        return missingValue;
    }

    /**
     *  Set the HeaderPattern property.
     *
     *  @param value The new value for HeaderPattern
     */
    public void setHeaderPattern(String value) {
        headerPattern = value;
    }

    /**
     *  Get the HeaderPattern property.
     *
     *  @return The HeaderPattern
     */
    public String getHeaderPattern() {
        return headerPattern;
    }

    /**
     * Set the IsDate property.
     *
     * @param value The new value for IsDate
     */
    public void setIsDate(boolean value) {
        isDate = value;
    }

    /**
     * Get the IsDate property.
     *
     * @return The IsDate
     */
    public boolean getIsDate() {
        return isDate;
    }

    /**
     * Set the IsTime property.
     *
     * @param value The new value for IsTime
     */
    public void setIsTime(boolean value) {
        isTime = value;
    }

    /**
     * Get the IsTime property.
     *
     * @return The IsTime
     */
    public boolean getIsTime() {
        return isTime;
    }

    /* Adding other dimensions latitude longitude */

    /**
     * _more_
     *
     * @param value _more_
     */
    public void setIsLatitude(boolean value) {
        isLatitude = value;
    }

    /**
     * _more_
     *
     * @param value _more_
     */
    public void setIsLongitude(boolean value) {
        isLongitude = value;
    }

    /**
     * _more_
     *
     * @param value _more_
     */
    public void setIsAltitude(boolean value) {
        isAltitude = value;
    }

    /**
     * _more_
     *
     * @param value _more_
     */
    public void setIsAltitudeReverse(boolean value) {
        isAltitudeReverse = value;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsLatitude() {
        return isLatitude;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsLongitude() {
        return isLongitude;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsAltitude() {
        return isAltitude;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsAltitudeReverse() {
        return isAltitudeReverse;
    }


    /**
     * Set the SortOrder property.
     *
     * @param value The new value for SortOrder
     */
    public void setSortOrder(int value) {
        sortOrder = value;
    }

    /**
     * Get the SortOrder property.
     *
     * @return The SortOrder
     */
    public int getSortOrder() {
        return sortOrder;
    }


    /**
     *  Set the ColumnWidth property.
     *
     *  @param value The new value for ColumnWidth
     */
    public void setColumnWidth(int value) {
        columnWidth = value;
    }

    /**
     *  Get the ColumnWidth property.
     *
     *  @return The ColumnWidth
     */
    public int getColumnWidth() {
        return columnWidth;
    }

    /**
     *  Set the Index property.
     *
     *  @param value The new value for Index
     */
    public void setIndex(int value) {
        index = value;
    }

    /**
     *  Get the Index property.
     *
     *  @return The Index
     */
    public int getIndex() {
        return index;
    }



    /**
     *  Set the IsDateOffset property.
     *
     *  @param value The new value for IsDateOffset
     */
    public void setIsDateOffset(boolean value) {
        isDateOffset = value;
    }

    /**
     *  Get the IsDateOffset property.
     *
     *  @return The IsDateOffset
     */
    public boolean getIsDateOffset() {
        return isDateOffset;
    }


}
