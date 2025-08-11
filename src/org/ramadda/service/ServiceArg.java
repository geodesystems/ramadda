/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.service;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.job.JobManager;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.lang.reflect.*;

import java.net.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;

import java.util.Enumeration;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import java.util.regex.*;
import java.util.zip.*;


public class ServiceArg extends ServiceElement {
    private static final String TYPE_STRING = "string";
    private static final String TYPE_ENUMERATION = "enumeration";
    private static final String TYPE_INT = "int";
    private static final String TYPE_FLOAT = "float";
    private static final String TYPE_ENTRY = "entry";
    private static final String TYPE_FLAG = "flag";
    private static final String TYPE_FILE = "file";    
    private static final String TYPE_CATEGORY = "category";
    private static final String TYPE_DATE = "date";
    private Service service;
    private String name;
    private String value;
    private String dflt;
    private String prefix;
    private String group;
    private String file;
    private String filePattern;
    private SimpleDateFormat dateFormat;
    private String dateFormatString;
    private boolean nameDefined = false;
    private boolean ifDefined = true;
    private boolean include = true;
    private boolean multiple = false;
    private boolean first = false;
    private String multipleJoin;
    private boolean sameRow = false;
    private String label;
    private String help;
    private String type;
    private String depends;
    private boolean addAll;
    private boolean addNone;
    private String placeHolder;
    private String entryType;
    private List<String> entryTypes;
    private String entryPattern;
    private String matchPattern;    
    private boolean isPrimaryEntry = false;
    private boolean required = false;
    private boolean copy = false;
    private String fileName;
    private int size;
    private List<TwoFacedObject> values = new ArrayList<TwoFacedObject>();
    private String valuesProperty;

    public ServiceArg(Service service, Element node, int idx)
            throws Exception {
        super(node);
        this.service = service;

        type = XmlUtil.getAttribute(node, Service.ATTR_TYPE, (String) null);
        depends      = XmlUtil.getAttribute(node, "depends", (String) null);
        addAll       = XmlUtil.getAttribute(node, "addAll", false);
        addNone      = XmlUtil.getAttribute(node, "addNone", false);

        entryType = XmlUtil.getAttributeFromTree(node,
                Service.ATTR_ENTRY_TYPE, (String) null);

        dateFormat =
            RepositoryUtil.makeDateFormat(dateFormatString =
                XmlUtil.getAttributeFromTree(node, "dateFormat",
                                             "yyyy-MM-dd"));
        if (entryType != null) {
            entryTypes = StringUtil.split(entryType, ",", true, true);
        }
        entryPattern = XmlUtil.getAttributeFromTree(node,
                Service.ATTR_ENTRY_PATTERN, (String) null);
        matchPattern = XmlUtil.getAttributeFromTree(node,
						      "matchPattern", (String) null);	

        placeHolder = XmlUtil.getAttribute(node, "placeHolder",
                                           (String) null);
        isPrimaryEntry = XmlUtil.getAttribute(node, Service.ATTR_PRIMARY,
                isPrimaryEntry);

        prefix = XmlUtil.getAttribute(node, "prefix", (String) null);
        value  = Utils.getAttributeOrTag(node, "value", "");
        dflt   = Utils.getAttributeOrTag(node, "default", "");
        name   = XmlUtil.getAttribute(node, Service.ATTR_NAME, (String) null);

        if ((name == null) && isEntry() && isPrimaryEntry) {
            name = "input_file";
        }

        nameDefined = name != null;
        if ((name == null) && (prefix != null)) {
            name = prefix.replaceAll("-", "");
        }
        if ((name == null) && Utils.stringDefined(value)) {
            name = value.replaceAll("-", "");
        }
        if (name == null) {
            name = "arg" + idx;
        }

        group = XmlUtil.getAttribute(node, Service.ATTR_GROUP, (String) null);
        file = XmlUtil.getAttribute(node, Service.ATTR_FILE, (String) null);
        filePattern = XmlUtil.getAttribute(node, "filePattern",
                                           (String) null);
        required = XmlUtil.getAttribute(node, "required", required);
        copy     = XmlUtil.getAttribute(node, "copy", false);
        valuesProperty = XmlUtil.getAttribute(node, "valuesProperty",
                (String) null);
        label    = XmlUtil.getAttribute(node, Service.ATTR_LABEL, name);	
        help     = Utils.getAttributeOrTag(node, "help", "");
        fileName = XmlUtil.getAttribute(node, "filename", "${src}");
        if (isInt()) {
            size = 5;
        } else if (isFloat()) {
            size = 10;
        } else {
            size = 24;
        }
        size      = XmlUtil.getAttribute(node, Service.ATTR_SIZE, size);
        ifDefined = XmlUtil.getAttribute(node, "ifdefined", ifDefined);
        include   = XmlUtil.getAttribute(node, "include", true);
        multiple  = XmlUtil.getAttribute(node, "multiple", multiple);
        first     = XmlUtil.getAttribute(node, "first", false);
        multipleJoin = XmlUtil.getAttribute(node, "multipleJoin",
                                            (String) null);
        sameRow = XmlUtil.getAttribute(node, "sameRow", sameRow);
        size    = XmlUtil.getAttribute(node, "size", size);
        for (String tok :
                StringUtil.split(Utils.getAttributeOrTag(node,
                    Service.ATTR_VALUES, ""), ",", true, true)) {
            List<String> toks  = StringUtil.splitUpTo(tok, ":", 2);

            String       value = toks.get(0);
            String       label;
            if (toks.size() > 1) {
                label = toks.get(1);
            } else {
                label = value;
            }
            values.add(new TwoFacedObject(label, value));
        }
    }

    public void toXml(Appendable xml) throws Exception {
        StringBuilder attrs = new StringBuilder();
        Service.attr(attrs, Service.ATTR_TYPE, type);
        Service.attr(attrs, "depends", depends);
        Service.attr(attrs, "dateFormat", dateFormatString);
        Service.attr(attrs, "addAll", addAll);
        Service.attr(attrs, "addNone", addNone);
        Service.attr(attrs, Service.ATTR_ENTRY_TYPE, entryType);
        Service.attr(attrs, Service.ATTR_ENTRY_PATTERN, entryPattern);
        Service.attr(attrs, "matchPattern", matchPattern);	
        Service.attr(attrs, "placeHolder", placeHolder);
        Service.attr(attrs, Service.ATTR_PRIMARY, isPrimaryEntry);
        Service.attr(attrs, "prefix", prefix);
        Service.attr(attrs, "value", value);
        Service.attr(attrs, "default", dflt);
        Service.attr(attrs, Service.ATTR_NAME, name);
        Service.attr(attrs, Service.ATTR_GROUP, group);
        Service.attr(attrs, Service.ATTR_FILE, file);
        Service.attr(attrs, "filePattern", filePattern);
        Service.attr(attrs, "required", required);
        Service.attr(attrs, "copy", copy);
        Service.attr(attrs, "valuesProperty", valuesProperty);
        Service.attr(attrs, Service.ATTR_LABEL, label);
        Service.attr(attrs, "help", help);
        Service.attr(attrs, "filename", fileName);
        Service.attr(attrs, Service.ATTR_SIZE, size);
        Service.attr(attrs, "ifdefined", ifDefined);
        Service.attr(attrs, "include", include);
        Service.attr(attrs, "multiple", multiple);
        Service.attr(attrs, "first", first);
        Service.attr(attrs, "multipleJoin", multipleJoin);
        Service.attr(attrs, "sameRow", sameRow);
        if (values.size() > 0) {
            List<String> valueAttrs = new ArrayList<String>();
            for (TwoFacedObject tfo : values) {
                valueAttrs.add(tfo.getId() + ":" + tfo.getLabel());
            }
            Service.attr(attrs, "values", StringUtil.join(",", valueAttrs));
        }

        xml.append(XmlUtil.tag(Service.TAG_ARG, attrs.toString()));
    }

    public boolean isApplicable(Entry entry, boolean debug) {
        boolean defaultReturn = true;
	//	debug  = entryType.equals("media_gs_thumbnail");

        if (debug) {
            System.err.println("Service.Arg.isApplicable:" + getName() +" " +getLabel() +
			       " entry type:" + entryType + " pattern:"
                               + entryPattern);
        }
        if (matchPattern != null) {
            if (entry.getResource().getPath().toLowerCase().matches(
                    matchPattern)) {
                return true;
            }
        }

        if (entryTypes != null) {
	    if(debug) System.err.println("entryTypes:" + entryTypes);
            boolean isType = false;
            for (String type : entryTypes) {
                if (entry.getTypeHandler().isType(type)) {
                    isType = true;
                    break;
                }
            }
            if ( !isType) {
                if (debug) {
                    System.err.println("\tentry is not type: " + type);
                }

                return false;
            }

            if (entryPattern == null) {
                if (debug) {
                    System.err.println("\thas entry type:" + entryType);
                }

                return true;
            }
	}
        if (entryPattern != null) {
            if (entry.getResource().getPath().toLowerCase().matches(
                    entryPattern)) {
                return true;
            }

            return false;
        }

        return defaultReturn;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public String toString() {
        return getName() + " " + getLabel();
    }

    public boolean isValueArg() {
        return (type == null) && !isCategory() && !nameDefined
               && Utils.stringDefined(value);
    }

    public boolean getIfDefined() {
        return ifDefined;
    }

    public boolean getInclude() {
        return include;
    }

    public boolean isEnumeration() {
        return (type != null) && type.equals(TYPE_ENUMERATION);
    }

    public boolean isDate() {
        return (type != null) && type.equals(TYPE_DATE);
    }

    public boolean isFlag() {
        return (type != null) && type.equals(TYPE_FLAG);
    }

    public boolean isFile() {
        return (type != null) && type.equals(TYPE_FILE);
    }

    public boolean isEntry() {
        return (type != null) && type.equals(TYPE_ENTRY);
    }

    public boolean isInt() {
        return (type != null) && type.equals(TYPE_INT);
    }

    public boolean isFloat() {
        return (type != null) && type.equals(TYPE_FLOAT);
    }

    public boolean isPrimaryEntry() {
        return isPrimaryEntry;
    }

    public boolean isCategory() {
        return (type != null) && type.equals(TYPE_CATEGORY);
    }

    public String getGroup() {
        return group;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getEntryType() {
        return entryType;
    }

    public List<String> getEntryTypes() {
        return entryTypes;
    }

    /**
     * Set the Value property.
     *
     * @param value The new value for Value
     */
    public void setValue(String value) {
        value = value;
    }

    /**
     * Get the Value property.
     *
     * @return The Value
     */
    public String getValue() {
        return value;
    }

    /**
     * Get the Id property.
     *
     * @return The Id
     */
    public String getName() {
        return name;
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

    public String getHelp() {
        return help;
    }

    /**
     * Set the Size property.
     *
     * @param value The new value for Size
     */
    public void setSize(int value) {
        size = value;
    }

    /**
     * Get the Size property.
     *
     * @return The Size
     */
    public int getSize() {
        return size;
    }

    /**
     * Set the FileName property.
     *
     * @param value The new value for FileName
     */
    public void setFileName(String value) {
        fileName = value;
    }

    /**
     * Get the FileName property.
     *
     * @return The FileName
     */
    public String getFileName() {
        return fileName;
    }

    public String getFile() {
        return file;
    }

    public String getFilePattern() {
        return filePattern;
    }

    public String getValuesProperty() {
        return valuesProperty;
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
     * Set the Values property.
     *
     * @param value The new value for Values
     */
    public void setValues(List<TwoFacedObject> value) {
        values = value;
    }

    /**
     * Get the Values property.
     *
     * @return The Values
     */
    public List<TwoFacedObject> getValues() {
        return values;
    }

    /**
     *  Get the Category property.
     *
     *  @return The Category
     */
    public String getCategory() {
        if ( !isCategory()) {
            return null;
        }

        return label;
    }

    public String getDefault() {
        return dflt;
    }

    public boolean getSameRow() {
        return sameRow;
    }

    public boolean getFirst() {
        return first;
    }

    public boolean getAddAll() {
        return addAll;
    }

    public String getPlaceHolder() {
        return placeHolder;
    }

    public boolean getAddNone() {
        return addNone;
    }

    public boolean getCopy() {
        return copy;
    }

    public String getDepends() {
        return depends;
    }

    public String getMultipleJoin() {
        return multipleJoin;
    }

    public SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

}
