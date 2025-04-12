/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;

import org.json.*;

import org.ramadda.repository.Constants;
import org.ramadda.repository.DateHandler;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.RepositoryBase;
import org.ramadda.repository.RepositoryUtil;
import org.ramadda.repository.Request;
import org.ramadda.repository.database.DatabaseManager;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.Bounds;
import org.ramadda.util.geo.GeoUtils;
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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

@SuppressWarnings("unchecked")
public class Column implements DataTypes, Constants, Cloneable {
    static int xcnt;
    public String myid = "column-" + (xcnt++);

    public static final HtmlUtils HU = null;
    public static final boolean DEBUG_TIME=false;
    public static final boolean DEBUG=false;

    public static final String ARG_EDIT_PREFIX = "edit_";
    public static final String ARG_SEARCH_PREFIX = "search.";
    public static final String OUTPUT_HTML = "html";
    public static final String OUTPUT_CSV = "csv";

    public static final String EXPR_EQUALS = "=";
    public static final String EXPR_LE = "<=";
    public static final String EXPR_LT = "<";
    public static final String EXPR_GT = ">";
    public static final String EXPR_GE = ">=";
    public static final String EXPR_BETWEEN = "between";

    public static final List EXPR_ITEMS =
        Misc.newList(new TwoFacedObject("", ""),
                     new TwoFacedObject("=", EXPR_EQUALS),
                     new TwoFacedObject("<=", EXPR_LE),
                     new TwoFacedObject(">=", EXPR_GE),
                     new TwoFacedObject("range", EXPR_BETWEEN));

    public static final String EXPR_PATTERN = EXPR_EQUALS + "|" + EXPR_LE
	+ "|" + EXPR_GE + "|"
	+ EXPR_BETWEEN;

    public static final String SEARCHTYPE_TEXT = "text";
    public static final String SEARCHTYPE_SELECT = "select";
    public static final String TAG_COLUMN = "column";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_FORMAT = "format";
    public static final String ATTR_CHANGETYPE = "changetype";
    public static final String ATTR_SHOWINFORM = "showinform";
    public static final String ATTR_GROUP = "group";
    public static final String ATTR_SEARCHGROUP = "searchgroup";
    public static final String ATTR_SUBGROUP = "subgroup";    
    public static final String ATTR_UNIT = "unit";
    public static final String ATTR_OLDNAMES = "oldnames";
    public static final String ATTR_SUFFIX = "suffix";
    public static final String ATTR_LOOKUPDB = "lookupdb";
    public static final String ATTR_HELP = "help";
    public static final String ATTR_POSTFIX = "postfix";    
    public static final String ATTR_SORT_ORDER = "sortOrder";
    public static final String ATTR_PROPERTIES = "properties";
    public static final String ATTR_LABEL = "label";
    public static final String ATTR_DESCRIPTION = "description";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_ISINDEX = "isindex";
    public static final String ATTR_ISCATEGORY = "iscategory";
    public static final String ATTR_CANSEARCH = "cansearch";
    public static final String ATTR_CANSORT = "cansort";
    public static final String ATTR_SEARCHROWS = "searchrows";
    public static final String ATTR_CANSEARCHTEXT = "cansearchtext";
    public static final String ATTR_ADVANCED = "advanced";
    public static final String ATTR_CANLIST = "canlist";
    public static final String ATTR_CANDISPLAY = "candisplay";
    public static final String ATTR_EDITABLE = "editable";
    public static final String ATTR_VALUES = "values";
    public static final String ATTR_DEFAULT = "default";
    public static final String ATTR_SIZE = "size";
    public static final String ATTR_MIN = "min";
    public static final String ATTR_MAX = "max";
    public static final String ATTR_REQUIRED = "required";
    public static final String ATTR_ROWS = "rows";
    public static final String ATTR_COLUMNS = "columns";
    public static final String ATTR_SEARCHTYPE = "searchtype";
    public static final String ATTR_SHOWINHTML = "showinhtml";
    public static final String ATTR_SHOWLABEL = "showlabel";
    public static final String ATTR_CANEXPORT = "canexport";

    private SimpleDateFormat dateTimeFormat =
        new SimpleDateFormat("yyyy-MM-dd HH:mm Z");

    private SimpleDateFormat fullDateTimeFormat =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private SimpleDateFormat displayFormat = null;
    private SimpleDateFormat dateParser = null;
    private boolean importDateIsEpoch = false;

    /** Lat/Lon format */
    private DecimalFormat latLonFormat = new DecimalFormat("##0.00");
    private DecimalFormat numberFormat;
    private DecimalFormat intFormat = new DecimalFormat("#0");
    private TypeHandler typeHandler;
    private Element xmlElement;
    private String name;
    private String fullName;
    private String displayGroup;
    private String subGroup;    
    private String editGroup;    
    private String searchGroup;    
    private List oldNames;
    private String label;
    private Pattern initPattern;
    private String searchLabel;    
    private String description;
    private String htmlTemplate;
    private String displayLabel;    
    private List<Utils.Macro> macros;
    private String displayTemplate;
    private String displayPatternFrom;
    private String displayPatternTo;
    private String type;
    private String delimiter;
    private boolean addRawInput;
    private boolean changeType = false;
    private String dropColumnVersion;
    private boolean showEmpty = true;

    private boolean addNot = false;
    private boolean doPolygonSearch  = false;
    private boolean addFileToSearch = false;
    private boolean isMediaUrl = false;
    private boolean adminOnly=false;
    private boolean isGeoAccess=false;
    private boolean doInlineEdit = false;
    private boolean addBulkUpload = false;
    private String bulkUploadHelp ="";
    private String suffix;
    private String displaySuffix;    
    private String help;
    private String postFix;    
    private String searchHelp;
    private String placeholder;
    private String placeholderMin;
    private String placeholderMax;
    private int sortOrder = 1000;
    private String searchType = SEARCHTYPE_TEXT;
    private boolean isIndex;
    private boolean doStats = false;
    private boolean isWiki;
    private boolean isCategory;
    private boolean canSearch;
    private boolean canSort;
    private boolean showEnumerationMenu = true;
    private boolean showEnumerationPopup = false;
    private boolean addBlankToEnumerationMenu = true;
    private boolean enumerationSearchMultiples = false;
    private boolean enumerationShowCheckboxes= false;
    private String menuWidth;
    private int searchRows;
    private boolean canSearchText;
    private boolean advancedSearch;
    private boolean editable;
    private boolean canList;
    private boolean canDisplay;
    private List<HtmlUtils.Selector> enumValues;
    private List<String> icons;
    private List<TwoFacedObject> jsonValues;
    private LinkedHashMap<String, String> enumMap = new LinkedHashMap<String,
	String>();

    private List<Display> displays = new ArrayList<Display>();
    private String alias;
    private String dflt;
    private String databaseDflt;
    private double databaseDfltNum = Double.NaN;
    private String databaseDefaultPropertyName;
    private double dfltDouble = Double.NaN;
    private int size = 400;
    private String entryType;
    private double min = Double.NaN;
    private double max = Double.NaN;
    private boolean required = false;
    private int rows = 1;
    private int columns = 40;
    private String propertiesFile;
    private int columnIndex;
    private int offset;
    private boolean canShow = true;
    private boolean showLabel = true;
    private boolean canExport = true;
    private boolean showInForm = true;
    private String lookupDB;

    private Hashtable<String, String> properties = new Hashtable<String,
	String>();

    private List<Column> groupedColumns;

    public Column(TypeHandler typeHandler, String name, String type,
                  int offset)
	throws Exception {
        this.typeHandler = typeHandler;
        this.name        = name;
        this.type        = type;
        this.offset      = offset;
    }

    public Column(TypeHandler typeHandler, Element element, int offset)
	throws Exception {

        this.xmlElement  = element;
        this.typeHandler = typeHandler;
        this.offset      = offset;

	//	TypeHandler.printAttrs(element,false);

        name             = XmlUtil.getAttribute(element, ATTR_NAME);
	delimiter= XmlUtil.getAttribute(element, "delimiter",",");
	addRawInput= XmlUtil.getAttribute(element, "addrawinput",false);
        String sinitPattern = XmlUtil.getAttribute(element, "initpattern",(String)null);
	if(sinitPattern!=null) {
	    initPattern  = Pattern.compile(sinitPattern);
	}
        String group  = XmlUtil.getAttribute(element, "group",(String)null);
        displayGroup =  XmlUtil.getAttribute(element, "displaygroup", group);
        searchGroup =  XmlUtil.getAttribute(element, ATTR_SEARCHGROUP, displayGroup);
        editGroup = XmlUtil.getAttribute(element, "editgroup", group);
        subGroup  = XmlUtil.getAttribute(element, "subgroup",(String)null);
        oldNames = Utils.split(XmlUtil.getAttribute(element, ATTR_OLDNAMES,
						    ""), ",", true, true);
        displaySuffix = Utils.getAttributeOrTag(element, "displaysuffix", "");	
        suffix = Utils.getAttributeOrTag(element, ATTR_SUFFIX, displaySuffix);
        help = Utils.getAttributeOrTag(element, ATTR_HELP, (String) null);
        postFix = Utils.getAttributeOrTag(element, ATTR_POSTFIX, (String) null);	
        searchHelp = Utils.getAttributeOrTag(element, "searchhelp", (String) null);	

	//	if(Utils.stringDefined(suffix))
	//	    System.err.println(typeHandler +" " +name + " suffix:" + Utils.clip(suffix,20,"...").replaceAll("\n"," "));	

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
        searchLabel = Utils.getAttributeOrTag(element, "searchlabel",label);

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

        dttmFormat = XmlUtil.getAttribute(element, "displayFormat",
					  (String) null);
        if (dttmFormat != null) {
	    displayFormat = new SimpleDateFormat(dttmFormat);
        }

        List propNodes = XmlUtil.findChildren(element, "property");
        for (int i = 0; i < propNodes.size(); i++) {
            Element propNode = (Element) propNodes.get(i);
            properties.put(XmlUtil.getAttribute(propNode, "name"),
                           XmlUtil.getAttribute(propNode, "value"));
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

        displayLabel = Utils.getAttributeOrTag(element, "displaylabel",
					       (String) null);

        htmlTemplate = Utils.getAttributeOrTag(element, "htmltemplate",
					       (String) null);

	if(htmlTemplate!=null) {
	    macros  =Utils.splitMacros(htmlTemplate);
	}

        type = Utils.getAttributeOrTag(element, ATTR_TYPE, DATATYPE_STRING);
        changeType = getAttributeOrTag(element, ATTR_CHANGETYPE, false);
	dropColumnVersion = XmlUtil.getAttribute(element,"dropcolumnversion",(String)null);

        showEmpty  = getAttributeOrTag(element, "showempty", true);

        doPolygonSearch     = getAttributeOrTag(element, "dopolygonsearch", false);
	//If it is a latlon then always do the negation
        addNot     = getAttributeOrTag(element, "addnot", isType(DATATYPE_LATLON));
        addFileToSearch = getAttributeOrTag(element, "addfiletosearch",
                                            addFileToSearch);
	adminOnly= XmlUtil.getAttribute(element, "adminonly", false);

	isGeoAccess= getAttributeOrTag(element, "isgeoaccess", false);
        isMediaUrl = getAttributeOrTag(element, "ismediaurl", false);
        dflt       = getAttributeOrTag(element, ATTR_DEFAULT, "").trim();
        doStats    = getAttributeOrTag(element, "dostats", Utils.getProperty(properties,"dostats",false));
        isIndex    = getAttributeOrTag(element, ATTR_ISINDEX, Utils.getProperty(properties,ATTR_ISINDEX,false));
        databaseDflt = getAttributeOrTag(element, "databaseDefault",
                                         (String) null);
        alias      = getAttributeOrTag(element, "alias", (String) null);

        doInlineEdit    = getAttributeOrTag(element, "doinlineedit", false);
        addBulkUpload    = getAttributeOrTag(element, "addbulkupload", false);
        bulkUploadHelp    = getAttributeOrTag(element, "bulkuploadhelp", "Upload file");
	menuWidth= getAttributeOrTag(element, "menuwidth",null);
	showEnumerationMenu= getAttributeOrTag(element, "showenumerationmenu", true);
	showEnumerationPopup= getAttributeOrTag(element, "showenumerationpopup", true);
	addBlankToEnumerationMenu=getAttributeOrTag(element, "addblanktoenumerationmenu", true);

        isWiki     = getAttributeOrTag(element, "iswiki", false);
        isCategory = getAttributeOrTag(element, ATTR_ISCATEGORY, false);
        canSearch  = getAttributeOrTag(element, ATTR_CANSEARCH, false);
        canSort    = getAttributeOrTag(element, ATTR_CANSORT, false);
        searchRows = getAttributeOrTag(element, ATTR_SEARCHROWS, 1);
        canSearchText = getAttributeOrTag(element, ATTR_CANSEARCHTEXT,canSearch);
	enumerationSearchMultiples = getAttributeOrTag(element,"enumeration_search_multiples",
						       getAttributeOrTag(element,"enumeration_multiples",false));
	enumerationSearchMultiples=true;
	enumerationShowCheckboxes = getAttributeOrTag(element,"enumeration_show_checkboxes",false);	

        advancedSearch = getAttributeOrTag(element, ATTR_ADVANCED, false);
        editable       = getAttributeOrTag(element, ATTR_EDITABLE, true);
        showInForm = getAttributeOrTag(element, ATTR_SHOWINFORM,
				       getAttributeOrTag(element, "canedit",showInForm));
        canShow        = getAttributeOrTag(element, ATTR_SHOWINHTML, canShow);
        showLabel      = getAttributeOrTag(element, ATTR_SHOWLABEL, showLabel);
        canExport      = getAttributeOrTag(element, ATTR_CANEXPORT, canExport);
        canList        = getAttributeOrTag(element, ATTR_CANLIST, true);
        canDisplay     = getAttributeOrTag(element, ATTR_CANDISPLAY, true);
	size           = getAttributeOrTag(element, ATTR_SIZE, isType(DATATYPE_CLOB)?1000000:	isType(DATATYPE_ENTRY_LIST)?5000:size);
	entryType = getAttributeOrTag(element,"entryType",null);

        min            = getAttributeOrTag(element, ATTR_MIN, min);
        max            = getAttributeOrTag(element, ATTR_MAX, max);
        required       = getAttributeOrTag(element, ATTR_REQUIRED, required);
        rows           = getAttributeOrTag(element, ATTR_ROWS, rows);
        columns        = getAttributeOrTag(element, ATTR_COLUMNS, columns);

	String tmpIcons = getAttributeOrTag(element, "iconmap", null);
	if(tmpIcons!=null) {
	    icons = Utils.split(tmpIcons,",");
	}
        lookupDB = getAttributeOrTag(element, ATTR_LOOKUPDB, (String) null);

        String tmp = getAttributeOrTag(element, "numberFormat",
				       getAttributeOrTag(element, "numberformat", null));
        if (tmp != null) {
            numberFormat = new DecimalFormat(tmp);
        }

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
                enumValues = new ArrayList<HtmlUtils.Selector>();
            }
        }

        if (isNumeric() && Utils.stringDefined(dflt)) {
            dfltDouble = Double.parseDouble(dflt);
        }

	if(isSynthetic()) {
	    showInForm= false;
	}
        dateTimeFormat.setTimeZone(RepositoryBase.TIMEZONE_UTC);
    }

    public void debug(Object o) {
    }

    public TypeHandler getTypeHandler() {
	return typeHandler;
    }

    public void initializeNew(Request request, Entry entry,String fileName) throws Exception {
	if(initPattern==null) return;
	Matcher matcher = initPattern.matcher(fileName);
        if (!matcher.find()) {
	    return;
	}
	String v = matcher.group(1);	
	if(v==null) return;
	//	System.err.println("column:" + this +" init value:" + v);
        Object[] values = entry.getTypeHandler().getEntryValues(entry);
	setValue( entry,  values, v);

    }

    public String getDisplayAttribute(String attr, Object v) {
        Display d = getDisplay(v);
        if (d == null) {
            return null;
        }

        return d.getAttribute(attr);
    }

    public Display getDisplay(Object v) {
        if (displays.size() == 0) {
            return null;
        }
        if (v == null) {
            return null;
        }
        if (isNumeric()) {
            double value = Double.parseDouble(v.toString());
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

    public String getIcon(String v) {
	if(icons!=null) {
	    String icon = null;
	    for(int i=0;i<icons.size();i+=2) {
		if(v.matches(icons.get(i))) {
		    icon = icons.get(i+1);
		    break;
		}
	    }	    
	    if(icon!=null) 
		return  getRepository().getIconUrl(icon);
	}
	return null;
    }

    public String decorate(String v) {
        Display d = getDisplay(v);
        if (d == null) {
	    String icon = getIcon(v);
	    if(icon!=null) {
		v = HU.image(icon) + HU.space(1) + v;
	    }
            return v;
        }

	return d.decorate(v);
    }

    public Column cloneColumn() throws CloneNotSupportedException {
        Column column = (Column) this.clone();
        return column;
    }

    public static List<String> getNames(List<Column> columns) {
        List<String> names = new ArrayList<String>();
        for (Column c : columns) {
            names.addAll(c.getColumnNames());
        }

        return names;
    }

    public static Object[] makeValueArray(List<Column> columns) {
        int size = 0;
        for (Column c : columns) {
            if (c.isSynthetic()) {
		//noop
	    } else if (c.isType(DATATYPE_LATLON)) {
                size += 2;
            } else if (c.isType(DATATYPE_LATLONBBOX)) {
                size += 4;
            } else {
                size++;
            }
        }

        return new Object[size];
    }

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

    private void setEnums(String valueString, String delimiter)
	throws Exception {
        List<String> tmp = typeHandler.getColumnEnumerationProperties(this,
								      valueString, delimiter);
        enumValues = new ArrayList<HtmlUtils.Selector>();

	String icon = null;

	int cnt = 0;
	for (String tok : tmp) {
	    tok  =tok.trim();
            if (tok.startsWith("#")) {
                continue;
            }
	    //Only pass through the blank if it is the first in the list
	    if(tok.equals("") && enumValues.size()>0) {
                continue;
            }
            if (tok.equals("_blank_")) tok = "";

	    int margin = 0;
	    if(tok.startsWith("heading:")) {
		tok = tok.substring("heading:".length());
		enumValues.add(new HtmlUtils.Selector(tok,"",null,0,true));
		//Force the icon so the menu is indented
		//icon = getRepository().getPageHandler().getIconUrl("/icons/blank16.png");
		continue;
	    }
            String label = tok;
            String value = null;
            if (tok.indexOf(":") >= 0) {
                List<String> toks = Utils.splitUpTo(tok, ":", 2);
                value = toks.get(0);
                label = toks.get(1);
            } else if (tok.indexOf("=") >= 0) {
                List<String> toks = Utils.splitUpTo(tok, "=", 2);
                value = toks.get(0);
                label = toks.get(1);
            }
	    while(label.startsWith(">")) {
		margin+=5;
		label = label.substring(1);
	    }
	    if(value==null) value=label;
            enumValues.add(new HtmlUtils.Selector(label, value,icon,icon!=null?0:margin));
            enumMap.put(value, label);
        }
    }

    public boolean isType(String t) {
        return type.equals(t);
    }

    public boolean getDoInlineEdit() {
	return doInlineEdit;
    }

    public String getProperty(String key) {
        return getProperty(key, null);
    }

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

    public void putProperty(String key, String value) {
        properties.put(key, value);
    }

    public Hashtable getProperties() {
        return properties;
    }

    public String msg(String s) {
        return typeHandler.msg(s);
    }

    public String msgLabel(String s) {
        return typeHandler.msgLabel(s);
    }

    public Repository getRepository() {
        return typeHandler.getRepository();
    }

    public DatabaseManager getDatabaseManager() {
        return getRepository().getDatabaseManager();
    }

    /**
       method
     */
    public String getJson(Request request,TypeHandler sourceTypeHandler) throws Exception {
	if(adminOnly &&!request.isAdmin()) return null;
        List<String> col = new ArrayList<String>();
        col.add("name");
        col.add(JsonUtil.quote(getName()));
        col.add("label");
        col.add(JsonUtil.quote(getLabel()));
        col.add("searchLabel");
        col.add(JsonUtil.quote(getSearchLabel()));	
        col.add("searchGroup");
        col.add(JsonUtil.quote(searchGroup));
        col.add("searchShowCheckboxes");
        col.add(""+enumerationShowCheckboxes);
        col.add("searchMultiples");
        col.add(""+enumerationSearchMultiples);

        col.add("type");
        col.add(JsonUtil.quote(getType()));
	if(displayGroup!=null) {
	    col.add("group");
	    col.add(JsonUtil.quote(displayGroup));
	}
        col.add("namespace");
        col.add(JsonUtil.quote(getTableName()));
        col.add("suffix");
        col.add(JsonUtil.quote(getSuffix()));
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
	    boolean forSearch = request.get("forsearch",false);
            List<String>         enums  = new ArrayList<String>();
            List<HtmlUtils.Selector> values = null;
            if (isType(DATATYPE_ENUMERATION) || isMultiEnumeration()) {
                values = enumValues;
            }
	    if(forSearch) values = null;
            if ((values == null) || (values.size() == 0)) {
                values = sourceTypeHandler.getEnumValues(request, this, null);
            }


            if (values != null) {
                for (HtmlUtils.Selector tfo : values) {
                    enums.add(JsonUtil.map(Utils.makeListFromValues("value",
								    JsonUtil.quote(tfo.getId().toString()), "label",
								    JsonUtil.quote(tfo.getLabel().toString()))));
                }
            }
            col.add("values");
            col.add(JsonUtil.list(enums));
        }

        return JsonUtil.map(col);

    }

    public boolean isNumeric() {
        return isInteger() || isDouble();
    }

    public boolean isInteger() {
        return isType(DATATYPE_INT);
    }

    public boolean isSynthetic() {
        return isType(DATATYPE_SYNTHETIC);
    }    

    public boolean isPrivate() {
        return isType(DATATYPE_PASSWORD);
    }

    public boolean isBoolean() {
        return isType(DATATYPE_BOOLEAN);
    }

    public boolean isGeo() {
	return isType(DATATYPE_LATLON) ||isType(DATATYPE_LATLONBBOX);
    }

    public boolean isLatLon() {
	return isType(DATATYPE_LATLON);
    }

    public boolean isEnumeration() {
        return isType(DATATYPE_ENUMERATION)
	    || isType(DATATYPE_ENUMERATIONPLUS)
	    || isType(DATATYPE_MULTIENUMERATION);
    }

    public boolean isMultiEnumeration() {
        return  isType(DATATYPE_MULTIENUMERATION);
    }    

    public boolean isDate() {
        return isType(DATATYPE_DATETIME) || isType(DATATYPE_DATE);
    }

    public boolean isDouble() {
        return isType(DATATYPE_DOUBLE) || isType(DATATYPE_PERCENTAGE);
    }

    public boolean isString() {
        return isType(DATATYPE_STRING) || isEnumeration()
	    || isType(DATATYPE_CLOB) || isType(DATATYPE_JSONLIST)
	    || isType(DATATYPE_ENTRY) || isType(DATATYPE_ENTRY_LIST) || isType(DATATYPE_EMAIL)
	    || isType(DATATYPE_WIKI) || isType(DATATYPE_URL)
	    || isType(DATATYPE_LIST);
    }

    public boolean isMediaUrl() {
        return isMediaUrl;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean getAdminOnly() {
	return adminOnly;
    }

    private boolean accessOk(Request request,Entry entry) {
	if(isPrivate()) {
	    return request.isAdmin() || request.isOwner(entry);
	}
	if(adminOnly) {
	    return request.isAdmin() || request.isOwner(entry);
	}
	if(isGeoAccess || isGeo()) {
	    return request.geoOk(entry);
	}
	return true;
    }

    public Object uncheckedGetObject(Request request, Object[] values) {
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

    public String uncheckedGetString(Request request,Object[] values) {
	Object o = uncheckedGetObject(request, values);
        if (o == null) {
            return null;
        }
	return o.toString();
    }

    public double uncheckedGetDouble(Request request, Object[] values) {
        Object o = getValue(request, values);
        if (o == null) {
            return Double.NaN;
        }
        return ((Double) o).doubleValue();
    }

    public double getDouble(Request request, Entry entry) {
        Object o = getValue(request, entry);
        if (o == null) {
            return Double.NaN;
        }
        return ((Double) o).doubleValue();
    }

    private String toString(Object[] values, int idx) {
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
        if ( !GeoUtils.latLonOk(values[idx])) {
            return "NA";
        }
        double d = ((Double) values[idx]).doubleValue();
        if (raw) {
            return "" + d;
        }

        return latLonFormat.format(d);
    }

    private boolean toBoolean(Object[] values, int idx) {
        if (values[idx] == null) {
            if (Utils.stringDefined(dflt)) {
                return Boolean.parseBoolean(dflt);
            }

            return true;
        }

        return ((Boolean) values[idx]).booleanValue();
    }

    public Object getValue(Request request, Entry entry) {
	if(!accessOk(request, entry)) {
	    return null;

	}
	return getValue(request, entry.getValues());
    }

    private static final double[]NULL_BBOX={Double.NaN,Double.NaN,Double.NaN,Double.NaN};
    private static final double[]NULL_LATLON={Double.NaN,Double.NaN,Double.NaN,Double.NaN};    

    public double[] getLatLonBbox(Request request,Entry entry) {
	if(entry==null) return NULL_BBOX;
	if(!accessOk(request, entry)) return NULL_BBOX;
	return getLatLonBbox(request, entry.getValues());
    }

    public double[] getLatLon(Request request,Entry entry) {
	return getLatLon(request, entry,entry.getValues());
    }

    public double[] getLatLon(Request request,Entry entry,Object []values) {
	if(entry==null) return NULL_LATLON;
	if(!accessOk(request, entry)) return NULL_LATLON;
	return getLatLon(request, values);
    }

    private Object getValue(Request request, Object[]values) {
	int index = getOffset();
        if ((values == null) || (index >= values.length)
	    || (values[index] == null)) {
            return null;
        }
        return values[index];	
    }

    public double[] getLatLonBbox(Request request,Object[]values) {
        return new double[] { Utils.getDouble(values[offset]),
			      Utils.getDouble(values[offset + 1]),
                              Utils.getDouble(values[offset + 2]),
                              Utils.getDouble(values[offset + 3]) };
    }

    public double[] getLatLon(Request request,Object []values) {
	if(values==null) return NULL_LATLON;
	double lat = Double.NaN;
	double lon = Double.NaN;
	if (values != null) {
	    lat = Utils.getDouble(values[offset]);
	    lon = Utils.getDouble(values[offset + 1]);
	    if(lat==Entry.NONGEO) lat = Double.NaN;
	    if(lon==Entry.NONGEO) lon = Double.NaN;	    
	}
	return new double[]{lat,lon};
    }

    public boolean hasLatLon(Request request,Entry entry) {
	double[]latlon = getLatLon(request,entry);
	return !Double.isNaN(latlon[0]) && !Double.isNaN(latlon[1]);
    }

    public boolean hasLatLonBox(Request request,Entry entry) {
	double[]latlon = getLatLonBbox(request,entry);
	return !Double.isNaN(latlon[0]) && !Double.isNaN(latlon[1]) && !Double.isNaN(latlon[2])&& !Double.isNaN(latlon[3]);
    }

    public String formatValue(Request request, Entry entry,  Object[] values,boolean...notMacros)
	throws Exception {
	StringBuilder sb = new StringBuilder();
        formatValue(request, entry, sb, null, values, null, false,notMacros);
	return sb.toString();
    }

    public void formatValue(Request request, Entry entry, Appendable sb,
                            String output, Object[] values, boolean raw,boolean...notMacros)
	throws Exception {
        formatValue(request, entry, sb, output, values, null, raw,notMacros);
    }

    public static String escapeComma(String s) {
	if(s==null) return s;
	s = s.replace("\\,","_comma_");
	return s;
    }
    public static String unescapeComma(String s) {
	if(s==null) return s;
	s = s.replace("_comma_",",");
	return s;
    }    

    /**
       get display value for html
       method
     */
    public boolean formatValue(Request request, Entry entry,
			       Appendable result,
			       String output, Object[] values,
			       SimpleDateFormat sdf, boolean raw,boolean...notMacros)
	throws Exception {
	if(!accessOk(request, entry)) return false;
	boolean addSuffix = true;
        Appendable sb  = new StringBuilder();
        boolean    csv = Misc.equals(output, OUTPUT_CSV);
        if (csv) {
            raw = true;
        }
        String delimiter = csv
	    ? "|"
	    : ",";
        //I think we always want to use ',' as the delimiter
        delimiter = ",";
        //      System.err.println("COL:" + this+" " + getType());

	if(isSynthetic()) {
	    //noop
	} else if (isLatLon()) {
            sb.append(toLatLonString(values, offset, raw));
            sb.append(delimiter);
            sb.append(toLatLonString(values, offset + 1, raw));
	    double lat = Utils.getDouble(values[offset]);
	    double lon = Utils.getDouble(values[offset+1]);	    
	    if(request.get("addmap",false) && !Double.isNaN(lat) && !Double.isNaN(lon) && lat!=Entry.NONGEO && lon!=Entry.NONGEO) {
		MapInfo map = new MapInfo(request, getRepository(),"200","200");
		map.addMarker("",lat,  lon, null,"","");
		map.center();
		sb.append(getRepository().getMapManager().getHtmlImports(request));
		sb.append(map.getHtml());
	    }
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
                double percent = Utils.getDouble(values[offset]);
                sb.append((int) (percent * 100) + "");
            }
        } else if (isType(DATATYPE_DOUBLE)) {
            if (raw) {
                sb.append(toString(values, offset));
            } else {
                Double v = Utils.getDouble(values[offset]);
                if (v == null) {
                    return false;
                }
                if ((v == dfltDouble) && !getShowEmpty()) {
                    return false;
                }
		if(Double.isNaN(v)) {
		    if ( !getShowEmpty()) {
			return false;
		    }
		    sb.append("NA");
		    addSuffix = false;
		} else {
		    String s = ((numberFormat != null)
				? numberFormat.format(v)
				: Utils.formatComma(v));
		    sb.append(s);
		}
            }
        } else if (isType(DATATYPE_DATETIME)) {
            String s;
	    Date date=(Date) values[offset];
	    if(date==null) {
		s="NA";
	    } else {
		if (sdf != null) {
		    s = sdf.format(date);
		} else {
		    s = dateTimeFormat.format(date);
		}
	    }
            sb.append(s);

        } else if (isType(DATATYPE_DATE)) {
            if (values[offset] == null) {
                sb.append("NA");
            } else {
                String s;
                if (displayFormat != null) {
                    s = displayFormat.format((Date) values[offset]);
		} else  if (sdf != null) {
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
        } else if (isType(DATATYPE_ENTRY_LIST)) {
            String entryIds  = toString(values, offset);
            List<Entry> entries=null;
            if (Utils.stringDefined(entryIds)) {
		entries=new ArrayList<Entry>();
		for(String entryId: Utils.split(entryIds,",",true,true)) {
		    try {
			entries.add(getRepository().getEntryManager().getEntry(null,  entryId));
		    } catch (Exception exc) {
			throw new RuntimeException(exc);
		    }
		}
            }
            if (raw) {
                sb.append(entryIds);
            } else {
                if (entries != null) {
                    try {
			int cnt=0;
			for(Entry theEntry: entries) {
			    String link =
				getRepository().getEntryManager().getAjaxLink(
									      request, theEntry,
									      theEntry.getName()).toString();

			    if(cnt++>0) sb.append(", ");
			    sb.append(link);
			}
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
		    s = escapeComma(s);
                    s = s.replace(",", "<br>");
		    s = unescapeComma(s);
                }
            }
            if (s.length() == 0) {
                if ( !getShowEmpty()) {
                    return false;
                }
            }

            if (rows > 1) {
                s = getRepository().getWikiManager().wikifyEntry(request,
								 entry, s, false, null);
            } else if (isEnumeration() && !raw) {
		if(isMultiEnumeration()) {
		    String group="";
		    for(String tok:Utils.split(s,delimiter,true,true)) {
			String label = getEnumLabel(tok);
			if(group.length()>0)group+="; ";
			if (label != null) {
			    group+= label;
			} else {
			    group+=tok;
			}
		    }
		    s = group;
		} else {
		    String v = s;
		    String label = getEnumLabel(s);
		    if (label != null) {
			s = label;
		    }
		    String icon  =getIcon(v);
		    if(icon!=null) {
			s = HU.image(icon) +HU.space(1) + s;
		    }
		}
            }
            sb.append(s);
        }

	if(addSuffix && Utils.stringDefined(displaySuffix)) {
	    sb.append("&nbsp;");
	    sb.append(displaySuffix);
	}
        String s = sb.toString();
	if(macros!=null && (notMacros.length==0 || !notMacros[0])) {
	    s = typeHandler.applyMacros(request,entry,macros,values,values[offset],s);
	}
        s = typeHandler.decorateValue(null, entry, this, s);
	result.append(s);
	return true;

    }

    public LinkedHashMap<String, String> getEnumTable() {
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

    public String getEnumLabel(String value, boolean forDisplay) {
        String label = getEnumLabelInner(value);
        if ( !forDisplay && (label.length() == 0)) {
            label = "&lt;blank&gt;";
        }

        return label;
    }

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

    public String getEnumValue(String label) {
        if (label == null) {
            return null;
        }
        for (String key : enumMap.keySet()) {
            String l = enumMap.get(key);
            if (l.equals(label)) {
                return key;
            }
        }

        return label;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int o) {
        offset = o;
    }

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

    private int setValuesInner(PreparedStatement statement, Object[] values,
                               int statementIdx)
	throws Exception {

        if (offset >= values.length) {
            return 0;
        }
	if(isSynthetic()) {
	    //noop
	} else if (isType(DATATYPE_INT)) {
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
                double  value = ((Double) values[offset]).doubleValue();
                boolean isNaN = Double.isNaN(value);
                if ( !isNaN) {
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
                if (isNaN) {
                    getDatabaseManager().setNaN(statement, statementIdx);
                } else {
                    statement.setDouble(statementIdx, value);
                }
            } else {
                //                double value = Double.NaN;
                double value = 0;
                if (Utils.stringDefined(dflt)) {
                    value = Double.parseDouble(dflt);
                }
		if(Double.isNaN(value)) {
                    getDatabaseManager().setNaN(statement, statementIdx);
		} else {
		    statement.setDouble(statementIdx, value);
		}
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
	    if(DEBUG_TIME || DEBUG)
		System.err.println("Column:set statement:" + this +" date:" +dttm);
            getDatabaseManager().setDate(statement, statementIdx, dttm);
            statementIdx++;
        } else if (isLatLon()) {
            if (values[offset] != null) {
                double lat = Utils.getDouble(values[offset]);
                double lon = Utils.getDouble(values[offset + 1]);
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
		boolean isClob= isType(DATATYPE_CLOB);
                getDatabaseManager().setString(statement, statementIdx,
					       getName(), value, isClob?Integer.MAX_VALUE:size);
            } else {
                statement.setString(statementIdx, null);
            }
            statementIdx++;
        }

        return statementIdx;

    }

    public void addToEntryNode(Request request,Entry entry, Object[] values, Element node,boolean encode)
	throws Exception {
	if(!accessOk(request, entry)) return;
        if (values[offset] == null) {
            return;
        }
        String stringValue = null;
        //Don't export the password
        if (isType(DATATYPE_PASSWORD)) {
            return;
        }
	if(isSynthetic()) {
	    //noop
	} else if (isLatLon()) {
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
        Element valueNode = XmlUtil.create(node.getOwnerDocument(), name);
        node.appendChild(valueNode);
        valueNode.setAttribute("encoded", encode?"true":"false");
        valueNode.appendChild(XmlUtil.makeCDataNode(node.getOwnerDocument(),
						    stringValue, encode));
    }

    public int readValues(Entry entry, ResultSet results, Object[] values,
                          int valueIdx)
	throws Exception {
	if(isSynthetic()) {
	    //noop
	} else  if (isType(DATATYPE_INT)) {
            int value = results.getInt(valueIdx);
            if (results.wasNull()) {
                if (databaseDflt != null) {
                    if (Double.isNaN(databaseDfltNum)) {
                        databaseDfltNum = Double.parseDouble(databaseDflt);
                    }
                    value = (int) databaseDfltNum;
                }
            }
            values[offset] = Integer.valueOf(value);
            valueIdx++;
        } else if (isType(DATATYPE_PERCENTAGE)) {
            values[offset] = Double.valueOf(results.getDouble(valueIdx));
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
                } else {
                    //Added this so we can handle storing nans in the db
                    value = Double.NaN;
                }
            }
            //            System.err.println("col: " + this +" " + results.wasNull() +" value:" + value);
            //            System.err.println(this  +" value=" + value);
            values[offset] = Double.valueOf(value);
            valueIdx++;
        } else if (isType(DATATYPE_BOOLEAN)) {
            String value = results.getString(valueIdx);
            if ((value == null) || (value.length() == 0)) {
                value = dflt;
            }
            if (value == null) {
                value = "0";
            }

            values[offset] =  Boolean.valueOf(value.equals("true")
					      || value.equals("1"));
            valueIdx++;
        } else if (isDate()) {
            values[offset] = getDatabaseManager().getTimestamp(results,  valueIdx,false);
	    if(DEBUG_TIME || DEBUG)
		System.err.println("Column:from db:" + this+" " + values[offset]);
            valueIdx++;
        } else if (isLatLon()) {
            values[offset] = Double.valueOf(results.getDouble(valueIdx));
            valueIdx++;
            values[offset + 1] = Double.valueOf(results.getDouble(valueIdx));
            valueIdx++;
        } else if (isType(DATATYPE_LATLONBBOX)) {
            values[offset]     = Double.valueOf(results.getDouble(valueIdx++));
            values[offset + 1] = Double.valueOf(results.getDouble(valueIdx++));
            values[offset + 2] =  Double.valueOf(results.getDouble(valueIdx++));
            values[offset + 3] = Double.valueOf(results.getDouble(valueIdx++));
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

    private void defineColumn(Statement statement, String name, String type,
                              boolean ignoreErrors)
	throws Exception {
	if(dropColumnVersion!=null) {
	    String key = getTableName()+"." + name +".dropcolumn." +dropColumnVersion;
	    if(!getRepository().getProperty(key,false)) {
		String sql = "alter table " + getTableName() + " drop column " + name;
		getRepository().getLogManager().logInfoAndPrint("Dropping column:" + getTableName() + "." + name + " with version:" + dropColumnVersion);
		SqlUtil.loadSql(sql, statement, true/*ignoreErrors*/, null);
		getRepository().writeGlobal(key,true);
	    }
	}
	//	System.out.println("altering table: " + sql);
        String sql = getDatabaseManager().getAddColumnSql(getTableName(), name,type);
	//Always ignore errors here
        SqlUtil.loadSql(sql, statement, true/*ignoreErrors*/, null);

        if (changeType) {
            sql = getDatabaseManager().getAlterTableSql(getTableName(), name,type);
	    SqlUtil.loadSql(sql, statement, ignoreErrors, null);
	    //	    System.out.println("OK altering table: " + sql);
        }
    }

    public void createTable(Statement statement, boolean ignoreErrors)
	throws Exception {
	if (isSynthetic()) {
	    return;
	} else  if (isType(DATATYPE_STRING) || isType(DATATYPE_PASSWORD)
		    || isType(DATATYPE_EMAIL) || isType(DATATYPE_URL)
		    || isType(DATATYPE_JSONLIST) || isType(DATATYPE_FILE)
		    || isType(DATATYPE_ENTRY) || isType(DATATYPE_ENTRY_LIST)) {
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
        } else if (isLatLon()) {
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

    public Object convert(String value) {
	if(isSynthetic()) {
	    //noop
	} else  if (isType(DATATYPE_INT)) {
            return Integer.parseInt(value);
        } else if (isDouble()) {
            return Double.parseDouble(value);
        } else if (isType(DATATYPE_BOOLEAN)) {
            return Boolean.parseBoolean(value);
        } else if (isType(DATATYPE_DATETIME)) {
            //TODO
        } else if (isType(DATATYPE_DATE)) {
            //TODO
        } else if (isLatLon()) {
            //TODO
        } else if (isType(DATATYPE_LATLONBBOX)) {
            //TODO
        }

        return value;
    }

    public String getTableName() {
        return typeHandler.getTableName();
    }

    public void addGeoExclusion(List<Clause> clauses) {
        if (isLatLon()) {
            String id = getFullName();
            clauses.add(Clause.neq(id + "_lat", Entry.NONGEO));
        }
    }

    public boolean getAreaSearchContains(Request request) {
	String searchArg=getSearchArg();
	return  request.getString(searchArg+"_"+ARG_AREA_MODE, VALUE_AREA_OVERLAPS).equals(VALUE_AREA_OVERLAPS);
    }

    public double[] getAreaSearchArgs(Request request) {
	String searchArg=getSearchArg();
	double north = request.get(searchArg + "_north",Double.NaN);
	double south = request.get(searchArg + "_south",Double.NaN);
	double east = request.get(searchArg + "_east",Double.NaN);
	double west = request.get(searchArg + "_west", Double.NaN);
	return new double[]{north,west,south,east};

    }

    public void assembleWhereClause(Request request, List<Clause> where,  Appendable searchCriteria)
	throws Exception {
	assembleWhereClause(request, where, searchCriteria,getSearchArg());
    }

    private void assembleWhereClause(Request request, List<Clause> where,
				     Appendable searchCriteria,String searchArg)
	throws Exception {	

	if(isSynthetic()) {
	    List<Clause> ors = new ArrayList<Clause>();
	    for(Column c: getGroupedColumns()) {
		String arg= c.getSearchArg()+"." +this.getName();
		c.assembleWhereClause(request, ors,searchCriteria, arg);
	    }
	    if(ors.size()>0) where.add(Clause.or(ors));
	    return;
	}

        boolean[] fromFile  = { false };
        boolean   doNegate  = false;
        if (request.defined(searchArg + "_not")) {
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
        if (isLatLon()) {
	    double[] nwse  = getAreaSearchArgs(request);

	    double north=nwse[0],west=nwse[1],south=nwse[2],east=nwse[3];
	    if(request.defined(ARG_SEARCH_POLYGON)) {
		String poly = request.getString(ARG_SEARCH_POLYGON,"").trim();
		List<Double> d = Utils.getDoubles(poly);
		if(d.size()>2) {
		    Bounds b= new Bounds();
		    for(int i=0;i<d.size();i+=2)
			b.expand(d.get(i),d.get(i+1));
		    north=b.getNorth();
		    west=b.getWest();
		    south=b.getSouth();
		    east=b.getEast();
		}
	    }

	    List<Clause> ns = new ArrayList<Clause>();
	    List<Clause> ew = new ArrayList<Clause>();	    
	    Clause top=null,bottom=null,right=null,left=null;
            if (GeoUtils.latLonOk(north)) {
		if(doNegate)
		    ns.add(top=Clause.gt(columnName + "_lat", north));
		else
		    ns.add(Clause.le(columnName + "_lat", north));
	    }
            if (GeoUtils.latLonOk(south)) {
		if(doNegate)
		    ns.add(bottom=Clause.lt(columnName + "_lat", south));
		else
		    ns.add(Clause.ge(columnName + "_lat", south));		
            }
            if (GeoUtils.latLonOk(west)) {
		if(doNegate)
		    ew.add(left=Clause.lt(columnName + "_lon", west));
		else
		    ew.add(Clause.ge(columnName + "_lon", west));		
            }
            if (GeoUtils.latLonOk(east)) {
		if(doNegate)
		    ew.add(right=Clause.gt(columnName + "_lon", east));
		else
		    ew.add(Clause.le(columnName + "_lon", east));		
            }
	    if(!doNegate) {
		where.addAll(ns);
		where.addAll(ew);		
	    } else {
		List<Clause> ors = new ArrayList<Clause>();
		if(left!=null) {
		    ors.add(left);
		}
		if(right!=null) {
		    ors.add(right);
		}
		if(top!=null) {
		    ors.add(top);
		}
		if(bottom!=null) {
		    ors.add(bottom);
		}						
		if(ors.size()>0) {
		    where.add(Clause.or(ors));
		}
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

            if (GeoUtils.latLonOk(north)) {
                where.add(Clause.le(columnName + "_north", north));
            }
            if (GeoUtils.latLonOk(south)) {
                where.add(Clause.ge(columnName + "_south", south));
            }
            if (GeoUtils.latLonOk(west)) {
                where.add(Clause.ge(columnName + "_west", west));
            }
            if (GeoUtils.latLonOk(east)) {
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

	    if (Double.isNaN(from) && Double.isNaN(to)) {
		if (!Double.isNaN(value)) {
		    if (expr.equals(EXPR_EQUALS) || expr.equals("")) {
			where.add(Clause.eq(getFullName(), value));
		    } else if (expr.equals(EXPR_LE)) {
			where.add(Clause.le(getFullName(), value));
		    } else if (expr.equals(EXPR_GE)) {
			where.add(Clause.ge(getFullName(), value));
		    } else {
			throw new IllegalArgumentException("Unknown expression:"
							   + expr);
		    }
		}
            } else if(!Double.isNaN(from) || !Double.isNaN(to)) {
                if (expr.equals(EXPR_EQUALS)) {
                    where.add(Clause.eq(getFullName(), from));
                } else if (expr.equals(EXPR_LE)) {
		    if(!Double.isNaN(from))
			where.add(Clause.le(getFullName(), from));
                } else if (expr.equals(EXPR_GE)) {
		    if(!Double.isNaN(from))
			where.add(Clause.ge(getFullName(), from));
                } else if (expr.equals(EXPR_BETWEEN) || expr.equals("")) {
		    if(!Double.isNaN(from))
			where.add(Clause.ge(getFullName(), from));
		    if(!Double.isNaN(to))
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
        } else if (isType(DATATYPE_ENTRY_LIST)) {
	    //TODO:
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
	} else if(isMultiEnumeration()) {
	    //public static Clause like(String column, Object value, boolean not) {
	    //	.eq || like "value,%" || like "%,value,%" || like "%,value"
	    //110020155623,"4952,2082,2084"
	    //110009841154,4952
	    //110039185789,4952
	    List<Clause> subClauses = new ArrayList<Clause>();
            if (values == null) {
                values = getSearchValues(request,searchArg);
            }

            if ((values != null) && (values.size() > 0)) {
                for (String value : values) {
                    if (value.equals("_blank_")) {
                        value = "";
                    }
                    if (value.equals(TypeHandler.ALL)) {
                        continue;
                    }
		    boolean not = value.startsWith("!");
		    subClauses.add(Clause.or(Clause.eq(columnName, value,not),
					     Clause.like(columnName, value+delimiter+"%",not),
					     Clause.like(columnName, "%"+delimiter+value+delimiter+"%",not),
					     Clause.like(columnName, "%"+delimiter+value,not)));
                }
            }
	    String raw = request.getString(getRawArg(searchArg),null);
	    if(Utils.stringDefined(raw)) {
		subClauses.add(Clause.stringClause(columnName, raw));
	    }
	    if (subClauses.size() > 0) {
		where.add(Clause.or(subClauses));
	    }
        } else if (isEnumeration()) {
	    List<Clause> subClauses = new ArrayList<Clause>();
            if (values == null) {
                values = getSearchValues(request,searchArg);
            }
            if ((values != null) && (values.size() > 0)) {
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
            }
	    String raw = request.getString(getRawArg(searchArg),null);
	    if(raw!=null) {
		subClauses.add(Clause.stringClause(columnName, raw));
	    }
	    if (subClauses.size() > 0) {
		where.add(Clause.or(subClauses));
	    }
        } else {
            //            String value = getSearchValue(request);
            if (values == null) {
		List<Clause> ors = new ArrayList<Clause>();
		for(String value: getSearchValues(request,searchArg)) {
		    if (Utils.stringDefined(value)) {
			if (value.equals("_blank_")) {
			    value = "";
			}
			addTextSearch(value, ors, doNegate);
		    }
                }
		if(ors.size()==1) {
		    where.add(ors.get(0));
		} else if(ors.size()>1) {
		    where.add(Clause.or(ors));
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
	    } else if (trimmed.equals("_notblank_")) {
                subClauses.add(Clause.eq(getFullName(), "", true));
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
	//	System.err.println(this+" CLAUSES:" + clauses);
        if (clauses.size() > 0) {
            where.add(Clause.or(clauses));
        }
    }

    private String getSearchValue(Request request) {
        String dflt      = null;
        String searchArg = getSearchArg();
        if (request.defined(searchArg)) {
            return request.getString(searchArg, dflt);
        }

        return dflt;
    }

    private List<String> getSearchValues(Request request, String searchArg) throws Exception {
        List<String> result    = new ArrayList<String>();
	for (Object arg : request.get(searchArg,  new ArrayList())) {
	    result.add(arg.toString());
        }
        return result;
    }

    public int matchValue(String arg, Object value, Object[] values) {
        if (isLatLon()) {
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
       edit form
       method
     */
    public void addToEntryForm(Request request, Entry parentEntry,
			       Entry entry,
                               Appendable formBuffer, Object[] values,
                               Hashtable state, FormInfo formInfo,
                               TypeHandler sourceTypeHandler)
	throws Exception {
        if ( !showInForm) {
            return;
        }
        String widget = getFormWidget(request, entry, sourceTypeHandler, values, formInfo);
        widget = sourceTypeHandler.getFormWidget(request, entry, this, widget);
        if (Utils.stringDefined(help)) {
            formBuffer.append(typeHandler.formEntry(request, "",
						    getRepository().getPageHandler().applyBaseMacros(TypeHandler.wrapHelp(help))));
        }

	typeHandler.addWidgetHelp(request,entry,formBuffer,this,values);

	if(Utils.stringDefined(suffix)) {
	    widget = HU.hbox(widget, suffix);
	}

	if(entry==null && addBulkUpload && sourceTypeHandler.getTypeProperty("form.bulkupload.show",true)) {
	    widget+= HU.div(HU.makeShowHideBlock("Bulk Upload",
						 bulkUploadHelp +"<br>"+HU.fileInput(ARG_BULKUPLOAD, ""),
						 false),"");
	}	

	String label = sourceTypeHandler.getFormLabel(parentEntry, entry, getName(),getLabel());
	if(sourceTypeHandler.getTypeProperty("form." + getName() + ".vertical",false)) {
            HU.formEntry(formBuffer,HU.b(label) + ":<br>"+ widget);
	} else  if (rows > 1) {
            formBuffer.append(typeHandler.formEntryTop(request,  label + ":", widget));
        } else {
            formBuffer.append(typeHandler.formEntry(request, label + ":", widget));
        }
	if(Utils.stringDefined(postFix)) {
	    formBuffer.append("<tr><td colspan=2>" + postFix+"</td></tr>");
	}
        formBuffer.append("\n");
    }

    //For now just change the edit argument by adding a edit_ prefix

    public String getEditArg() {
        return ARG_EDIT_PREFIX + getFullName().replace(".", "_");
    }

    private String overrideSearchArg;

    public void setSearchArg(String arg) {
        overrideSearchArg = arg;
    }

    public String getRawArg(String arg) {
	return arg+"_raw";
    }

    public String getSearchArg() {
        if (overrideSearchArg != null) {
            return overrideSearchArg;
        }

        return ARG_SEARCH_PREFIX + getFullName();
    }

    private boolean doShowEnumerationPopup(int size) {
	return showEnumerationPopup && size>5;
    }


    /** method */
    public String getFormWidget(Request request, Entry entry,
				TypeHandler sourceTypeHandler,
                                Object[] values, FormInfo formInfo)
	throws Exception {

        String widget = "";
        String urlArg = getEditArg();

	if(isSynthetic()) {
	    return "";
	} 
        if (isLatLon()) {
            double[] latlon = getLatLon(request,entry,values);
            MapInfo map = getRepository().getMapManager().createMap(request,
								    entry, true, null);
            widget = map.makeSelector(urlArg, true,
                                      new String[] { GeoUtils.latLonOk(latlon[0])
						     ? latlon[0] + ""
						     : "", GeoUtils.latLonOk(latlon[1])
						     ? latlon[1] + ""
						     : "" });
        } else if (isType(DATATYPE_LATLONBBOX)) {
            String[] nwse = null;
            if (values != null) {
                nwse = new String[] { GeoUtils.latLonOk(values[offset + 0])
                                      ? values[offset + 0] + ""
                                      : "", GeoUtils.latLonOk(values[offset + 1])
				      ? values[offset + 1] + ""
				      : "", GeoUtils.latLonOk(values[offset + 2])
				      ? values[offset + 2] + ""
				      : "", GeoUtils.latLonOk(values[offset + 3])
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
            //            widget = HU.checkbox(urlArg, "true", value);
            List<TwoFacedObject> items = new ArrayList<TwoFacedObject>();
            items.add(new TwoFacedObject("Yes", "true"));
            items.add(new TwoFacedObject("No", "false"));
            widget = HU.select(urlArg, items, value
			       ? "true"
			       : "false", HU.cssClass("search-select"));
        } else if (isType(DATATYPE_DATETIME)) {
            Date date=null;
            if (values != null) {
                date = (Date) values[offset];
            } else {
		if(!isDefaultNone()) 
		    date = new Date();
            }
            widget = getRepository().getDateHandler().makeDateInput(request,
								    urlArg, "", date, null);
        } else if (isType(DATATYPE_DATE)) {
            Date date=null;
            if (values != null) {
                date = (Date) values[offset];
            } else {
		if(!isDefaultNone()) 
		    date = new Date();
            }
            widget = getRepository().getDateHandler().makeDateInput(request,
								    urlArg, "", date, null, false);
        } else if (isType(DATATYPE_ENUMERATION) || isMultiEnumeration()) {
            String value = ((dflt != null)
                            ? dflt
                            : "");
            value = request.getString(urlArg, ((values != null)
					       ? (String) toString(values, offset)
					       : value));
	    //For now ask the typehandler for the enum values
	    //we used to just use the  TypeHandler.enumValues but that is wrong?
	    List<HtmlUtils.Selector> enumValues;

	    if(false &&
	       isType(DATATYPE_ENUMERATION) &&
	       this.enumValues!=null && this.enumValues.size()>0) {
		enumValues = this.enumValues;
	    } else {
		enumValues = sourceTypeHandler.getEnumValues(request, this, null);
	    }

	    String widgetId = HU.getUniqueId("widget_");
	    String width = (String) Utils.getNonNull(menuWidth,"14em");
            widget = HU.select(urlArg, enumValues, value,
			       HU.attrs("id",widgetId,"class","ramadda-pulldown-with-icons","width",width));

	    if(doShowEnumerationPopup(enumValues.size())) {
		widget+=getEnumerationPopup(widgetId,false);
	    }

        } else if (isType(DATATYPE_ENUMERATIONPLUS)) {
            String value = ((dflt != null)
                            ? dflt
                            : "");
            value = request.getString(urlArg, ((values != null)
					       ? (String) toString(values, offset)
					       : value));
	    //This is a hack to fix a problem with changing from an enumeration to a string
	    //If we do this then lucene has a problem with indexing this column
	    if(showEnumerationMenu) {
		List enums = getEnumPlusValues(request, entry,sourceTypeHandler);
		if(addBlankToEnumerationMenu) {
		    if(!HtmlUtils.Selector.contains(enums,""))
			enums.add(0,new HtmlUtils.Selector("&lt;blank&gt;",""));
		}
		String widgetId = HU.getUniqueId("widget_");
		String width = (String) Utils.getNonNull(menuWidth,"14em");
		widget = HU.select(urlArg, enums, value, HU.attrs("id",widgetId,"width",width,
								  "class","ramadda-pulldown-with-icons")) +
		    "  or:  "  +
		    HU.input(urlArg + "_plus", "", HU.attr("size",""+columns));

		if(doShowEnumerationPopup(enums.size())) {
		    widget+=getEnumerationPopup(widgetId,false);
		}

	    } else {
		widget = HU.input(urlArg, value,HU.attr("size",""+ columns));
	    }

        } else if (isType(DATATYPE_INT)) {
            String value = ((dflt != null)
                            ? dflt
                            : "");
            if (values != null) {
                value = "" + toString(values, offset);
            }
            widget = HU.input(urlArg, value, HU.SIZE_10);
        } else if (isType(DATATYPE_DOUBLE)) {
            String domId = HU.getUniqueId("input_");
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
            widget = HU.input(urlArg, value,
			      HU.SIZE_10 + HU.id(domId));
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
            double d          = Double.parseDouble(value);
            int    percentage = (int) (d * 100);
            widget = HU.input(urlArg, percentage + "",
			      HU.SIZE_5) + "%";
        } else if (isType(DATATYPE_PASSWORD)) {
            String value = ((dflt != null)
                            ? dflt
                            : "");
            if (values != null) {
                value = "" + toString(values, offset);
            }
            widget = HU.password(urlArg, value,
				 HU.attr("size", ((columns > 0)
						  ? "" + columns
						  : "10")));
        } else if (isType(DATATYPE_FILE)) {
            String value = ((dflt != null)
                            ? dflt
                            : "");
            if (values != null) {
                value = "" + toString(values, offset);
            }
            widget = HU.fileInput(urlArg, "");
        } else if (isType(DATATYPE_ENTRY)) {
            String value = "";
            if (values != null) {
                value = toString(values, offset);
            }
            widget =
                getRepository().getEntryManager().getEntryFormSelect(request,
								     entry, urlArg, value);
        } else if (isType(DATATYPE_ENTRY_LIST)) {
	    //TODO
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
                    widget = HU.input(urlArg, value, " size=10 ");
                } else {
                    widget = HU.select(urlArg, tfos, value);
                }
            } else {
                String domId = HU.getUniqueId("input_");
                if ((rows > 1) || isWiki) {
		    value = value.replace("\\n","\n");
                    if (isType(DATATYPE_LIST)) {
			value = escapeComma(value);
			value = StringUtil.join("\n",Utils.split(value, ",", true, true));
			value = unescapeComma(value);
                    }
                    if (isWiki) {
                        StringBuilder tmp = new StringBuilder();
                        typeHandler.addWikiEditor(request, entry, tmp,
						  formInfo, urlArg, value, null, false, size,
						  true);
                        widget = tmp.toString();
                    } else {
                        int areaRows = rows;
                        widget = HU.textArea(urlArg, value, areaRows,
					     columns, HU.id(domId));
                    }
                } else {
                    widget = HU.input(urlArg, value,
				      HU.id(domId) + " size=\""
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
		+ HU.span(
			  "* " + msg("required"),
			  HU.cssClass("ramadda-required-field"));
        }

        return widget;
    }

    private List getEnumPlusValues(Request request, Entry entry,TypeHandler sourceTypeHandler)
	throws Exception {
        List<HtmlUtils.Selector> enums = sourceTypeHandler.getEnumValues(request, this,
								   entry);
        //TODO: Check for Strings vs Selector
        if (enumValues != null) {
            List tmp = new ArrayList();
            for (Object o : enums) {
                if ( !(o instanceof HtmlUtils.Selector)) {
                    o = new HtmlUtils.Selector(o.toString(),o.toString());
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

    private String getEnumerationPopup(String widgetId,boolean forSearch) {
	String single = forSearch && searchRows>1?"false":"true";
	String popupArgs = "{icon:true,makeButtons:false,after:true,single:" + single+"}";
	return HU.script(HU.call("HtmlUtils.makeSelectTagPopup",
				 HU.quote("#"+widgetId),
				 popupArgs));
    }

    public void setValue(Request request, Entry entry, Object[] values)
	throws Exception {

	//        if ( !showInForm || !editable) {
	//Don't check for editable here as the API call to change a field goes hits this method
        if ( !showInForm) {
            //            System.err.println (this + " not adding to form" );
            return;
        }

        String urlArg = getEditArg();
	if (isSynthetic()) {
	    return;
	} 
	if (isLatLon()) {
            if (request.exists(urlArg + "_latitude")) {
                values[offset] = Double.parseDouble(request.getString(urlArg
								      + "_latitude", "0").trim());
                values[offset + 1] = Double.parseDouble(request.getString(urlArg
									  + "_longitude", "0").trim());
            } else if (request.exists(urlArg + ".latitude")) {
                String latString = request.getString(urlArg + ".latitude",
						     "0").trim();
                String lonString = request.getString(urlArg + ".longitude",
						     "0").trim();
                double lat = Entry.NONGEO;
                double lon = Entry.NONGEO;
                if (Utils.stringDefined(latString)) {
                    lat = GeoUtils.decodeLatLon(latString);
                }
                if (Utils.stringDefined(lonString)) {
                    lon = GeoUtils.decodeLatLon(lonString);
                }
                values[offset]     = lat;
                values[offset + 1] = lon;
            }

        } else if (isType(DATATYPE_LATLONBBOX)) {
            if (request.exists(urlArg + "_north")) {
                values[offset] = Double.valueOf(request.get(urlArg + "_north",
							    (Double)Utils.getNonNull(values[offset], Double.valueOf(Entry.NONGEO))));
                values[offset + 1] = Double.valueOf(request.get(urlArg + "_west",
								(Double)Utils.getNonNull(values[offset+1], Double.valueOf(Entry.NONGEO))));
		values[offset + 2] = Double.valueOf(request.get(urlArg+ "_south",
								(Double)Utils.getNonNull(values[offset+2], Double.valueOf(Entry.NONGEO))));							    

                values[offset + 3] = Double.valueOf(request.get(urlArg + "_east",
								(Double)Utils.getNonNull(values[offset+3], Double.valueOf(Entry.NONGEO))));							    
	    } else {
                values[offset] = Double.valueOf(request.get(urlArg + ".north",
							    (Double)Utils.getNonNull(values[offset+0], Double.valueOf(Entry.NONGEO))));							    
		values[offset + 1] = Double.valueOf(request.get(urlArg + ".west",
								(Double)Utils.getNonNull(values[offset+1], Double.valueOf(Entry.NONGEO))));							    
		values[offset + 2] = Double.valueOf(request.get(urlArg  + ".south",
								(Double)Utils.getNonNull(values[offset+2], Double.valueOf(Entry.NONGEO))));							    							    
		values[offset + 3] = Double.valueOf(request.get(urlArg + ".east",
								(Double)Utils.getNonNull(values[offset+3], Double.valueOf(Entry.NONGEO))));							    
	    }
        } else if (isDate()) {
	    Date defaultDate= (Date) Utils.getNonNull(values[offset],new Date());
	    if(isDefaultNone()) defaultDate=null;

	    values[offset] = request.getDate(urlArg,defaultDate);
	    if(DEBUG_TIME||DEBUG)
		System.err.println("Column: from request:"+this+"  Default:" + defaultDate+ " VALUE:" + values[offset]);
        } else if (isType(DATATYPE_BOOLEAN)) {
            //Note: using the default will not work if we use checkboxes for the widget
            //For now we are using a yes/no combobox
            String value = request.getString(urlArg,
					     Utils.getNonNull(values[offset],
							      dflt,"true").toString()).toLowerCase();
            values[offset] =  Boolean.parseBoolean(value);
        } else if (isType(DATATYPE_ENUMERATION) || isMultiEnumeration()) {
            if (request.exists(urlArg)) {
                values[offset] = request.getAnonymousEncodedString(urlArg,
								   (String)Utils.getNonNull(values[offset],
											    dflt,""));
            } else {
                values[offset] = Utils.getNonNull(values[offset],dflt);
            }
        } else if (isType(DATATYPE_LIST)) {
            if (request.exists(urlArg)) {
                String value = request.getAnonymousEncodedString(urlArg,
								 (String)Utils.getNonNull(values[offset],dflt,""));
		value = value.replace(",","\\,");
                value = StringUtil.join(", ",
                                        Utils.split(value, "\n", true, true));
                values[offset] = value;
            } else {
                values[offset] = Utils.getNonNull(values[offset],dflt);
            }

        } else if (isType(DATATYPE_ENUMERATIONPLUS)) {
            String theValue = "";
	    boolean debug = urlArg.indexOf("tribe")>=0;
            if (request.defined(urlArg + "_plus")) {
                theValue = request.getAnonymousEncodedString(urlArg
							     + "_plus", ((dflt != null)
									 ? dflt
									 : ""));
            } else if (request.exists(urlArg)) {
                theValue = request.getAnonymousEncodedString(urlArg,
							     ((dflt != null)
							      ? dflt
							      : ""));
            } else {
                theValue = (String)Utils.getNonNull(values[offset],dflt);
            }
            values[offset] = theValue;
            typeHandler.addEnumValue(this, entry, theValue);
        } else if (isType(DATATYPE_INT)) {
            Integer dfltValue = (Integer)Utils.getNonNull(values[offset],
							  (Utils.stringDefined(dflt)
							   ? Integer.parseInt(dflt)
							   : 0));
            if (request.exists(urlArg)) {
                values[offset] = Integer.valueOf(request.get(urlArg, dfltValue));
            } else {
                values[offset] = dfltValue;
            }

        } else if (isType(DATATYPE_PERCENTAGE)) {
            Double dfltValue = (Double)Utils.getNonNull(values[offset],
							(Utils.stringDefined(dflt)
							 ? Double.valueOf(dflt.trim())
							 : 0));
            if (request.exists(urlArg)) {
                values[offset] = Double.valueOf(request.get(urlArg, dfltValue)
						/ 100);
            } else {
                values[offset] = dfltValue;
            }
        } else if (isDouble()) {
            Double dfltValue = (Double)Utils.getNonNull(values[offset],
							(Utils.stringDefined(dflt)
							 ? Double.valueOf(dflt.trim())
							 : 0));
            if (request.exists(urlArg)) {
		String v = request.getString(urlArg,"");
		if(v.trim().length()==0) {
		    values[offset] = Double.NaN;
		} else {
		    values[offset] = Double.valueOf(request.get(urlArg, dfltValue));
		}
            } else {
                values[offset] = dfltValue;

            }
        } else if (isType(DATATYPE_ENTRY)) {
            values[offset] = request.getString(urlArg + "_hidden", (String)Utils.getNonNull(values[offset],
											    ""));
        } else if (isType(DATATYPE_ENTRY_LIST)) {
            values[offset] = request.getString(urlArg + "_hidden", (String)Utils.getNonNull(values[offset],
											    ""));	    
        } else {
            //string
	    if (request.exists(urlArg)) {
		if(isType(DATATYPE_CLOB)) {
		    values[offset] = request.getString(urlArg,
						       ((dflt != null)
							? dflt
							: ""));
		} else {
		    values[offset] = request.getAnonymousEncodedString(urlArg,
								       ((dflt != null)
									? dflt
									: ""));
		}
            } else {
                values[offset] = Utils.getNonNull(values[offset],dflt);
            }
        }

    }

    public boolean isDefaultNone() {
	return Utils.equals(dflt,"none");
    }

    public void setValue(Entry entry,  Object value)
	throws Exception {
	setValue(entry,entry.getTypeHandler().getEntryValues(entry), value);
    }

    public void setValue(Entry entry, Object[] values, Object value)
	throws Exception {

	if (isSynthetic()) {
	    return;
	}

        if (isLatLon()) {
            List<String> toks = Utils.split(value, ";", true, true);
            if (toks.size() == 2) {
                values[offset]     = Double.parseDouble(toks.get(0));
                values[offset + 1] = Double.parseDouble(toks.get(1));
            } else {
                toks = Utils.split(value, "|", true, true);
                if (toks.size() == 2) {
                    values[offset]     = Double.parseDouble(toks.get(0));
                    values[offset + 1] = Double.parseDouble(toks.get(1));
                } else {
                    //What to do here
                }
            }
        } else if (isType(DATATYPE_LATLONBBOX)) {
            List<String> toks = Utils.split(value, ";", true, true);
            values[offset]     = Double.parseDouble(toks.get(0));
            values[offset + 1] = Double.parseDouble(toks.get(1));
            values[offset + 2] = Double.parseDouble(toks.get(2));
	    values[offset + 3] = Double.parseDouble(toks.get(3));
        } else if (isDate()) {
            fullDateTimeFormat.setTimeZone(RepositoryBase.TIMEZONE_UTC);
            values[offset] = parseDate(value.toString());
        } else if (isEnumeration()) {
            values[offset] = value;
        } else if (isType(DATATYPE_BOOLEAN)) {
            if (Utils.stringDefined(value)) {
                values[offset] = Boolean.parseBoolean(value.toString());
            } else {
                values[offset] = Boolean.FALSE;
            }
        } else if (isType(DATATYPE_INT)) {
            try {
                if (Utils.stringDefined(value)) {
                    values[offset] = Integer.parseInt(value.toString());
                } else {
                    values[offset] = Integer.valueOf(0);
                }
            } catch (NumberFormatException nfe) {
                throw new RuntimeException("Error parsing integer column:"
                                           + getName() + " value:" + value);

            }
        } else if (isType(DATATYPE_PERCENTAGE) || isDouble()) {
            if (Utils.stringDefined(value)) {
                values[offset] = Utils.getDouble(value);
            } else {
                values[offset] = Double.valueOf(0);
            }
        } else if (isType(DATATYPE_ENTRY)) {
            values[offset] = value;
        } else if (isType(DATATYPE_ENTRY_LIST)) {
            values[offset] = value;	    
        } else {
            values[offset] = value;
        }
    }

    private Date parseDate(String value) throws Exception {
        if ( !Utils.stringDefined(value)) {
            return null;
        }

        if (importDateIsEpoch) {
            long l = Long.parseLong(value);

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

    public void addToSearchForm(Request request, Appendable formBuffer,
                                List<Clause> where,boolean...horizontal)
	throws Exception {
        addToSearchForm(request, formBuffer, where, null,horizontal);
    }

    private String[] getNWSE(Request request, Entry entry, String searchArg,
                             boolean fromEntry) {
        if ( !fromEntry && request.defined(searchArg + "_north")) {
            return new String[] { request.getString(searchArg + "_north", ""),
                                  request.getString(searchArg + "_west", ""),
                                  request.getString(searchArg + "_south", ""),
                                  request.getString(searchArg + "_east",
						    ""), };
        }
        if ((entry != null) && entry.hasAreaDefined(request)) {
            return new String[] { "" + entry.getNorth(request), "" + entry.getWest(request),
                                  "" + entry.getSouth(request),
                                  "" + entry.getEast(request) };

        }

        return new String[] { "", "", "", "" };
    }

    public void addToSearchForm(Request request, Appendable formBuffer,
                                List<Clause> where, Entry entry,boolean...horizontal)
	throws Exception {

        if ( !getCanSearch()) {
            return;
        }
        String       searchArg  = getSearchArg();
	addToSearchForm(request, formBuffer, where, entry, searchArg,horizontal);
    }

    /**
       search form
       method
    */
    private void addToSearchForm(Request request, Appendable formBuffer,
				 List<Clause> where, Entry entry, String searchArg,
				 boolean...horizontal)
	throws Exception {	
	boolean vertical = horizontal.length>0?!horizontal[0]:true;

        String       columnName = getFullName();

        List<Clause> tmp        = (where != null)
	    ? new ArrayList<Clause>(where)
	    : null;
        String       widget     = "";
        String       widgetId   = searchArg.replaceAll("\\.", "_");
	if (isSynthetic()) {
	    StringBuilder sb = new StringBuilder();
	    sb.append(HU.formTable());
	    for(Column c: getGroupedColumns()) {
		c.addToSearchForm(request, sb,where,entry,c.getSearchArg()+"." +
				  this.getName());
	    }
	    sb.append(HU.formTableClose());
	    widget = HU.makeToggleInline(searchHelp!=null?searchHelp:"", sb.toString(),false);
	    //	    widget=sb.toString();
	} else if (isLatLon()) {
            String[] nwseValues = getNWSE(request, null, searchArg, false);
            String[] nwseView   = getNWSE(request, entry, searchArg, true);
            MapInfo map = getRepository().getMapManager().createMap(request,
								    entry, true, null);
            widget = map.makeSelector(searchArg, true, nwseValues, nwseView,
                                      "", "",doPolygonSearch,request.getString(ARG_SEARCH_POLYGON,""));
        } else if (isType(DATATYPE_LATLONBBOX)) {
            String[] nwseValues = getNWSE(request, null, searchArg, false);
            String[] nwseView   = getNWSE(request, entry, searchArg, true);
            MapInfo map = getRepository().getMapManager().createMap(request,
								    entry, true, null);
            widget = map.makeSelector(searchArg, true, nwseValues, nwseView,
                                      "", "",doPolygonSearch,request.getString(ARG_SEARCH_POLYGON,""));
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
                HU.select(searchArg + "_relative", dateSelect,
			  dateSelectValue,
			  HU.cssClass("search-select"));
            widget = getRepository().getDateHandler().makeDateInput(
								    request, searchArg + "_fromdate", "searchform", null, null,
								    isType(DATATYPE_DATETIME)) + HU.space(1)
		+ HU.img(getRepository().getIconUrl(ICON_RANGE))
		+ HU.space(1)
		+ getRepository().getDateHandler().makeDateInput(
								 request, searchArg + "_todate", "searchform", null,
								 null, isType(
									      DATATYPE_DATETIME)) + (vertical?HU.br():HU.space(4))
		+ msgLabel("Or") + dateSelectInput;
        } else if (isType(DATATYPE_BOOLEAN)) {
            widget = HU.select(
			       searchArg,
			       Misc.newList(TypeHandler.ALL_OBJECT, "true", "false"),
			       request.getSanitizedString(searchArg, ""),
			       HU.cssClass("search-select"));
        } else if (isEnumeration()) {
            List tmpValues;
            if (searchRows > 1) {
                tmpValues = new ArrayList();
            } else {
                tmpValues = Misc.newList(TypeHandler.ALL_OBJECT);
            }
            List<HtmlUtils.Selector> values = typeHandler.getEnumValues(request, this, entry);
            for (HtmlUtils.Selector o : values) {
                HtmlUtils.Selector tfo = null;
                if (enumValues != null) {
                    tfo = HtmlUtils.Selector.findId(o.getId(), enumValues);
                }
                if (tfo != null) {
                    tmpValues.add(tfo);
                } else {
                    String label = getEnumLabel(o.getLabel(), false);
                    tmpValues.add(new HtmlUtils.Selector(label, o.getId()));
                }
            }

            //            String selectExtra = HU.id(searchArg + "_id");
            String selectExtra = "";
            if (searchRows > 1) {
                selectExtra += " multiple rows=\"" + searchRows + "\" ";
            } else {
                selectExtra += HU.cssClass("search-select");
            }

            //      if(true) return;
            //            System.err.println(getName() + " values=" + tmpValues);
            StringBuilder tmpb = new StringBuilder();
	    if(addRawInput) {
		tmpb.append(HU.input(getRawArg(searchArg), request.getString(getRawArg(searchArg),""),
				     HU.attr("placeholder","Enter " + label +" directly")+
				     HU.SIZE_20));
		tmpb.append(HU.space(1));
		tmpb.append("Or select:");
		tmpb.append(HU.space(1));		
	    }

	    List<String> selectedValues = new ArrayList<String>();
	    for(String v: getSearchValues(request,searchArg)) {
		if(Utils.stringDefined(v) && !TypeHandler.ALL.equals(v)) {
		    selectedValues.add(v);
		}
	    }
	    selectedValues.add("DUMMYVALUE");
	    int i=0;
	    if(enumerationShowCheckboxes) {
		for(HtmlUtils.Selector tfo:values) {
		    tmpb.append(HU.labeledCheckbox(searchArg,
						   tfo.getId().toString(),
						   selectedValues.contains(tfo.getId()),
						   tfo.getLabel().toString()));
		    tmpb.append("<br>");
		}
	    } else {
		for(String value:selectedValues) {
		    String arg = searchArg;
		    tmpb.append(HU.select(arg, tmpValues,
					  value,
					  selectExtra + ((i == 0)
							 ? HU.attr("id", widgetId)
							 : "")));
		    tmpb.append(" ");
		    i++;
		    if(!enumerationSearchMultiples) break;
		}
            }

	    if(doShowEnumerationPopup(values.size())) {
		tmpb.append(getEnumerationPopup(widgetId,true));
	    }
	    widget = tmpb.toString();
	    if(enumerationSearchMultiples) {
		widget = HU.div(widget, HU.cssClass("ramadda-widgets-enumeration"));
	    }
        } else if (isNumeric()) {
            String toId = Utils.makeID(searchArg + "_to");
            String expr = HU.select(
				    searchArg + "_expr", EXPR_ITEMS,
				    request.getString(searchArg + "_expr", ""),
				    HU.attr("to-id", toId)
				    + HU.cssClass(
						  "search-select ramadda-range-select"));
	    String size= "4";
            widget = expr
		+ HU.input(searchArg + "_from",
			   request.getSanitizedString(searchArg + "_from",
						      ""), ((placeholderMin != null)
							    ? HU.attr("placeholder", placeholderMin)
							    : "") + HU.attr("size", size)) + " "
		+ HU.input(searchArg + "_to",
			   request.getSanitizedString(searchArg + "_to",
						      ""), ((placeholderMax != null)
							    ? HU.attr("placeholder", placeholderMax)
							    : "") + HU.attr("id", toId) + HU.attr("size", size));
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
            sb.append(HU.hidden(searchArg + "_hidden", entryId,
				HU.id(searchArg + "_hidden")));
            sb.append(HU.disabledInput(searchArg, ((theEntry != null)
						   ? theEntry.getFullName()
						   : ""), HU.id(searchArg)
				       + HU.SIZE_60) + select);

            widget = sb.toString();
        } else if (isType(DATATYPE_ENTRY_LIST)) {
	    //TODO
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
            sb.append(HU.hidden(searchArg + "_hidden", entryId,
				HU.id(searchArg + "_hidden")));
            sb.append(HU.disabledInput(searchArg, ((theEntry != null)
						   ? theEntry.getFullName()
						   : ""), HU.id(searchArg)
				       + HU.SIZE_60) + select);

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
                //                System.err.println("TIME:" + (t2 - t1) + " " + (t3 - t2));

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
                    widget = HU.hidden(searchArg,
				       (String) list.get(0).getId()) + " "
			+ list.get(0).toString();
                } else {
                    list.add(0, TypeHandler.ALL_OBJECT);
                    widget = HU.select(searchArg, list);
                }
                //            } else if (rows > 1) {
                //                widget = HU.textArea(searchArg, request.getString(searchArg, ""),
                //                                           rows, columns);
            } else {
                String text = request.getSanitizedString(searchArg, "");
                text = text.replaceAll("\"", "&quot;");
                //                String text  = Utils.unquote(request.getString(searchArg, ""));

                boolean isList = isType(DATATYPE_LIST);
                String  attrs  = HU.attr("id", widgetId);
                if (placeholder != null) {
                    attrs += HU.attr("placeholder", placeholder);
                }
                if (isList) {
                    widget = HU.textArea(searchArg, text, 5, 20,
					 attrs);
                } else {
		    StringBuilder tmpSB = new StringBuilder();
		    List<String> searchValues = new ArrayList<String>();
		    for(String value:getSearchValues(request,searchArg)) {
			if(!Utils.stringDefined(value)) continue;
			searchValues.add(value);
		    }
		    if(searchValues.size()==0) searchValues.add("");
		    for(String s:searchValues) {
			s = s.trim();
			s = s.replaceAll("\"", "&quot;");
			s = HU.sanitizeString(s);
			tmpSB.append(HU.input(searchArg, s,
					      HU.SIZE_20 + attrs));
			tmpSB.append("&nbsp;");
		    }
		    widget = HU.div(tmpSB.toString(), HU.cssClass("ramadda-widgets-text"));
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
                    "<div class=ramadda-note>Note: You need to specify the file again for subsequent searches</div><br>";
            }
            file +=
                HU.fileInput(searchArg + "_file", "")
                + " File contains search values, one per line. Must be exact match.";
            widget += HU.div(HU.makeShowHideBlock("File...", file, visible));
        }

	if(horizontal.length>0 && horizontal[0]) {
	    HU.formEntry(formBuffer,    msgLabel(getSearchLabel()),widget);
	} else {
	    HU.formEntry(formBuffer,    HU.b(getSearchLabel()) + ":<br>"+widget);
	}	    
	//        typeHandler.formEntry(formBuffer, request, getLabel() + ":",widget);

        formBuffer.append("\n");

    }

    private List<Column> getGroupedColumns() throws Exception {
	if(groupedColumns==null) {
	    List<Column> tmp = new ArrayList<Column>();
	    for(String c:Utils.split(Utils.getAttributeOrTag(xmlElement, "groupedcolumns", ""))) {
		Column column = typeHandler.findColumn(c);
		if(column==null) {
		    System.err.println("Could not find column:" + this +" column:" + c);
		    continue;
		}
		tmp.add(column);
	    }
	    groupedColumns = tmp;
	}
	return groupedColumns;
    }

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

    public String getFullName() {
        if (fullName == null) {
            fullName = getTableName() + "." + name;
        }
        return fullName;
    }

    public void setName(String value) {
        name = value;
    }

    public List<String> getColumnNames() {
        List<String> names = null;
        if (names == null) {
            names = new ArrayList<String>();
            if (isSynthetic()) {
		//noop
	    } else if (isLatLon()) {
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

    public String getSortByColumn() {
        if (isLatLon()) {
            return name + "_lat";
        }
        if (isType(DATATYPE_LATLONBBOX)) {
            return name + "_north";
        }

        return name;
    }

    public String getName() {
        return name;
    }

    public String getDisplayGroup(){
        return displayGroup;
    }

    public String getEditGroup(){
        return editGroup;
    }    

    public String getSubGroup(){
	return subGroup;
    }

    public String getPropertiesFile() {
        return propertiesFile;
    }

    public void setLabel(String value) {
        label = value;
    }

    public String getDisplayLabel() {
	if(displayLabel!=null) return displayLabel;
	return getLabel();
    }

    public String getLabel() {
        return label;
    }

    public String getSearchLabel() {
        return searchLabel;
    }    

    public void setDescription(String value) {
        description = value;
    }

    public String getDescription() {
        return description;
    }

    public void setType(String value) {
        type = value;
    }

    public String getType() {
        return type;
    }

    public boolean isField(String name) {
        return Misc.equals(this.name, name) || Misc.equals(this.label, name);
    }

    public void setValues(List<HtmlUtils.Selector> value) {
        enumValues = value;
    }

    public List<HtmlUtils.Selector> getValues() {
        return enumValues;
    }

    public void setDflt(String value) {
        dflt = value;
    }

    public String getDflt() {
        return dflt;
    }

    public String toString() {
        return name+" type:" + type +" offset:" + offset;
    }

    public int getRows() {
        return rows;
    }

    public void setSize(int value) {
        size = value;
    }

    public int getSize() {
        return size;
    }

    public void setEditable(boolean value) {
        editable = value;
    }

    public boolean getEditable() {
        return editable;
    }

    public boolean getShowInForm() {
        return showInForm;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getHelp() {
        return help;
    }

    public String getPostFix() {
        return postFix;
    }    

    public String getAttributeOrTag(Element node, String attrOrTag,
                                    String dflt)
	throws Exception {
        String attrValue = Utils.getAttributeOrTag(node, attrOrTag,
						   (String) null);
        if (attrValue == null) {
            attrValue = XmlUtil.getAttributeFromTree(node, attrOrTag);
        }

	if(attrValue==null && typeHandler!=null) {
	    attrValue = typeHandler.getTypeProperty("column." + attrOrTag,(String)null);
	}

        if (attrValue != null) {
            properties.put(attrOrTag, attrValue);
            return attrValue;
        }

        return dflt;
    }

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

    private boolean getAttributeOrTag(Element node, String attrOrTag,
                                      boolean dflt)
	throws Exception {
        String attrValue = getAttributeOrTag(node, attrOrTag, (String) null);
        if (attrValue == null) {
            return dflt;
        }

        return attrValue.equals("true");
    }

    private int getAttributeOrTag(Element node, String attrOrTag, int dflt)
	throws Exception {
        String attrValue = getAttributeOrTag(node, attrOrTag, (String) null);
        if (attrValue == null) {
            return dflt;
        }

        return Integer.parseInt(attrValue);
    }

    public double getAttributeOrTag(Element node, String attrOrTag,
                                    double dflt)
	throws Exception {
        String attrValue = getAttributeOrTag(node, attrOrTag, (String) null);
        if (attrValue == null) {
            return dflt;
        }

        return Double.parseDouble(attrValue);
    }

    public void setColumnIndex(int value) {
        columnIndex = value;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public void setAlias(String value) {
        alias = value;
    }

    public String getAlias() {
        return alias;
    }

    public void setDoStats(boolean value) {
        doStats = value;
    }

    public boolean getDoStats() {
        return doStats;
    }

    public String getLookupDB() {
        return lookupDB;
    }

    public String getDelimiter() {
	return delimiter;
    }

    public void setIsIndex(boolean value) {
        isIndex = value;
    }

    public boolean getIsIndex() {
        return isIndex;
    }

    public void setIsCategory(boolean value) {
        isCategory = value;
    }

    public boolean getIsCategory() {
        return isCategory;
    }

    public boolean getShowEmpty() {
        return showEmpty;
    }

    public void setCanShow(boolean value) {
        canShow = value;
    }

    public boolean getCanShow() {
        if (isType(DATATYPE_PASSWORD)) {
            return false;
        }

        return canShow;
    }

    public boolean getCanExport() {
        return canExport;
    }

    public boolean getShowLabel() {
        return showLabel;
    }

    public void setCanSearch(boolean value) {
        canSearch = value;
    }

    public boolean getCanSearch() {
        return canSearch;
    }

    public boolean getCanSort() {
        return canSort;
    }

    public void setSearchRows(int value) {
        searchRows = value;
    }

    public int getSearchRows() {
        return searchRows;
    }

    public void setCanSearchText(boolean value) {
        canSearchText = value;
    }

    public boolean getCanSearchText() {
        return canSearchText;
    }

    public boolean getAdvancedSearch() {
        return advancedSearch;
    }

    public void setCanList(boolean value) {
        canList = value;
    }

    public boolean getCanList() {
        return canList;
    }

    public void setCanDisplay(boolean value) {
        canDisplay = value;
    }

    public boolean getCanDisplay() {
        return canDisplay;
    }

    public static class Display {
        String value;
        String background;
        String color;
        String mapFillColor;
        String template;
        String icon;
        double min;
        double max;

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

        public String decorate(String v) {
            String style = "";
            if (background != null) {
                style += "background:" + background + ";";
            }
            if (color != null) {
                style += "color:" + color + ";";
            }

            if (style.length() > 0) {
                v = HU.div(v, HU.style(style));
            }
            if (template != null) {
                v = template.replace("${value}", v);
            }

            return v;
        }
    }

}
