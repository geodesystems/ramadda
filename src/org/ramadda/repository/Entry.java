/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;

import org.ramadda.repository.auth.Permission;
import org.ramadda.repository.auth.User;
import org.ramadda.repository.auth.UserManager;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.metadata.MetadataManager;
import org.ramadda.repository.metadata.MetadataType;
import org.ramadda.repository.type.Column;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.repository.util.ServerInfo;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.ramadda.util.geo.Bounds;

import org.w3c.dom.Element;

import ucar.unidata.util.Misc;

import java.awt.geom.Rectangle2D;

import java.io.File;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

/**
 * The class to hold Entry information
 */
@SuppressWarnings("unchecked")
public class Entry implements Cloneable {

    public static final HtmlUtils HU = null;

    /** ID delimiter */
    public static final String IDDELIMITER = ":";

    /** path delimiter */
    public static final String PATHDELIMITER = "/";

    /** non-geo identifier */
    public static final double NONGEO = -999999;

    //New installs will use a clob for the description
    //    public static final int MAX_DESCRIPTION_LENGTH = Integer.MAX_VALUE;

    public static final int MAX_DESCRIPTION_LENGTH = 32000;

    public static final int MAX_NAME_LENGTH = 200;

    public static final int DEFAULT_ORDER = 999;

    /** List of comments */
    List<Comment> comments;

    /** List of permissions */
    List<Permission> permissions = null;

    /** permission map */
    Hashtable permissionMap = new Hashtable();

    /** List of associations */
    List<Association> associations;

    /** Entry metadata */
    List<Metadata> metadata;

    /** the id */
    private String id;

    /** the name */
    private String name = "";

    /** the description */
    private String description = "";

    private String snippet;

    /** the parent entry */
    private Entry parentEntry;

    /** the parent entry id */
    private String parentEntryId;

    /**  */
    private String remoteParentEntryId;

    /** the tree id */
    private String treeId;

    private boolean metadataChanged = false;

    /** the user (owner) */
    private User user;

    private int entryOrder = DEFAULT_ORDER;

    /** the create date */
    private long createDate = 0L;

    /** the change date */
    private long changeDate = 0L;

    /** is this a stoopid entry */
    boolean isDummy = false;

    boolean cacheOk = true;    

    long cacheActiveLimit = -1;

    /** the associated values (columns) */
    Object[] values;

    /** the resource */
    private Resource resource = new Resource();

    /** the category */
    private String category;

    /** the type handler for this entry */
    private TypeHandler typeHandler;

    private TypeHandler masterTypeHandler;

    /** the start date */
    private long startDate = DateHandler.NULL_DATE;

    /** the end date */
    private long endDate = DateHandler.NULL_DATE;

    /** the south value */
    private double south = NONGEO;

    /** the north value */
    private double north = NONGEO;

    /** the east value */
    private double east = NONGEO;

    /** the west value */
    private double west = NONGEO;

    /** the altitude bottom value */
    private double altitudeBottom = NONGEO;

    /** the altitude top value */
    private double altitudeTop = NONGEO;

    /** is this a local file */
    private boolean isLocalFile = false;

    private ServerInfo remoteServer;

    private String remoteUrl;

    private String remoteId;    

    /** the icon for this Entry */
    private String icon;

    /** the Entry properties */
    private Hashtable properties;

    /** the Entry properties string */
    private String propertiesString;

    /** transient properties */
    private Hashtable transientProperties = new Hashtable();

    /** the group property */
    private boolean isGroup = false;

    /** the chillens ids */
    private List<String> childIds;

    private List<Entry> children;

    private Element xmlNode;

    /**
     * Default constructor
     */
    public Entry() {}

    /**
     * Copy constructor
     *
     * @param that  the Entry to copy
     */
    public Entry(Entry that) {
        //        super(that);
        initWith(that, true);
    }

    /**
     * Create an Entry with the id
     *
     * @param id  the id
     */
    public Entry(String id) {
        setId(id);
    }

    /**
     * Create a new Entry with the type and dummy flag
     *
     * @param handler  the type handler
     * @param isDummy  true if stoopid
     */
    public Entry(TypeHandler handler, boolean isDummy) {
        this(handler, isDummy, "Listing");
    }

    /**
     * Create a new Entry with the type and dummy flag
     *
     * @param handler  the type handler
     * @param isDummy  true if stoopid
     * @param dummyName  the dummy name
     */
    public Entry(TypeHandler handler, boolean isDummy, String dummyName) {
        this("", handler);
        this.isDummy = isDummy;
        setName(dummyName);
        setDescription("");
    }

    /**
     * Create an Entry with the id and type handler
     *
     * @param id  the Entry id
     * @param typeHandler  the type handler
     */
    public Entry(String id, TypeHandler typeHandler) {
        this(id);
        this.typeHandler = typeHandler;
    }

    /**
     * Create an Entry with the id and type handler
     *
     * @param id  the Entry id
     * @param typeHandler  the type handler
     * @param isGroup  true if a group
     */
    public Entry(String id, TypeHandler typeHandler, boolean isGroup) {
        this(id);
        this.typeHandler = typeHandler;
        this.isGroup     = isGroup;
    }

    public void sanitize() {
	setName(HU.strictSanitizeString(getName()));
	setDescription(HU.strictSanitizeString(getDescription()));	
	remoteUrl = HU.strictSanitizeString(remoteUrl);
	snippet = HU.strictSanitizeString(snippet);
	if(resource!=null)resource.sanitize();
	Object []values= getValues();
	if(values!=null) {
	    for(int i=0;i<values.length;i++) {
		Object o=values[i];
		if(o instanceof String)
		    values[i] = HU.strictSanitizeString(o.toString());
	    }
	}
	List<Metadata> mtd = getMetadataList(null);
	if(mtd!=null) {
	    for(Metadata m: mtd) {
		m.sanitize();
	    }		
	}
    }

    /**
     * Create a generated Entry
     *
     * @param request  the Request
     * @param id       the id
     *
     * @return  the new Entry
     */
    public Entry createGeneratedEntry(Request request, String id) {
        return null;
    }

    /**
     *  Set the ChildIds property.
     *
     *  @param value The new value for ChildIds
     */
    public void setChildIds(List<String> value) {
        childIds = value;
    }

    /**
     *  Get the ChildIds property.
     *
     *  @return The ChildIds
     */
    public List<String> getChildIds() {
        return childIds;
    }

    /**
     * Clone this Entry
     *
     * @return  a clone
     *
     * @throws CloneNotSupportedException  on badness
     */
    public Object clone() throws CloneNotSupportedException {
        Entry that = (Entry) super.clone();
        that.associations = null;
        that.childIds     = this.childIds;

        return that;
    }

    /**
     * Get the full name of the entry
     *
     * @return  the full name
     */
    public String getFullName() {
        return getFullName(false);
    }

    /**
     * Get the full name of the Entry
     *
     * @param encodeForUrl  encode it for the URL
     *
     * @return  the full name (encoded or not based on encodeForUrl)
     */
    public String getFullName(boolean encodeForUrl) {
        String name = getName();
        //        boolean debug = name.indexOf("crap")>=0;
        //Encode any URLish characters
        if (encodeForUrl) {
            name = encodeName(name);
        }
        Entry parent = getParentEntry();
        if (parent != null) {
            String parentName = parent.getFullName(encodeForUrl);
            return parentName + PATHDELIMITER + name;
        }

        return name;
    }

    /**
     * Encode the name
     *
     * @param name  the name
     *
     * @return  the encoded name
     */
    public static String encodeName(String name) {
        name = name.replaceAll("\\/", "%2F");
        name = name.replaceAll("\\?", "%3F");
        name = name.replaceAll("\\&", "%26");
        name = name.replaceAll("\\#", "%23");

        return name;
    }

    /**
     * Decode the name
     *
     * @param name the encoded name
     *
     * @return  the decoded name
     */
    public static String decodeName(String name) {
        name = name.replaceAll("%2F", "/");
        name = name.replaceAll("%3F", "?");
        name = name.replaceAll("%26", "&");

        return name;
    }

    /**
     * Get the File associated with this Entry
     *
     * @return  the file or null if no file
     */
    public File getFile() {
        return getTypeHandler().getFileForEntry(this);
    }

    public String getResourcePath(Request request) throws Exception {
	return getTypeHandler().getPathForEntry( request, this,true);
    }    

    public void initWith(Entry template) {
        initWith(template, false);
    }

    /**
     *  Initialize the Entry with the template
     *
     *  @param template  the template
     * @param clone _more_
     */
    public void initWith(Entry template, boolean clone) {
	Request request= typeHandler.getRepository().getAdminRequest();
        if (Utils.stringDefined(template.getName())) {
            setName(template.getName());
        }
        if (Utils.stringDefined(template.getDescription())) {
            setDescription(template.getDescription());
        }
        setTypeHandler(template.getTypeHandler());

        if (template.resource != null) {
            this.resource = new Resource(template.resource);
        }

        if (template.getMetadataList(null) != null) {
            List<Metadata> thisMetadata = new ArrayList<Metadata>();
            for (Metadata metadata : template.getMetadataList(null)) {
                metadata.setEntryId(getId());
                thisMetadata.add(metadata);
            }
            setMetadata(thisMetadata);
	    setMetadataChanged(true);
        }

        if (template.hasCreateDate()) {
            setCreateDate(template.getCreateDate());
            setChangeDate(template.getChangeDate());
        }
        if (template.hasStartDate()) {
            setStartDate(template.getStartDate());
            setEndDate(template.getEndDate());
        }

        setNorth(template.getNorth(request));
        setSouth(template.getSouth(request));
        setEast(template.getEast(request));
        setWest(template.getWest(request));
        this.altitudeTop    = template.altitudeTop;
        this.altitudeBottom = template.altitudeBottom;

        Object[] values = template.getValues();
        this.values = values;

    }

    private double cleanLat(double lat) {
        return Math.max(Math.min(lat, 90), -90);
    }

    private double cleanLon(double lon) {
        return Math.max(Math.min(lon, 180), -180);
    }

    public String getBoundsString(Request request) {
	return getBoundsString(request, false);
    }

    public String getBoundsString(Request request,boolean addLabel) {	
        if ( !hasAreaDefined(request)) {
            return null;
        }
	if(addLabel)
	    return "North: " +getNorth(request) + " West: " + getWest(request) + " South: " + getSouth(request) + " East: " + getEast(request);
        return getNorth(request) + "," + getWest(request) + "," + getSouth(request) + "," + getEast(request);
    }

    /**
     * Get the geographic bounds
     *
     * @return  the bounds
     */
    public Rectangle2D.Double getBounds(Request request) {
        return new Rectangle2D.Double(cleanLon(getWest(request)), cleanLat(getSouth(request)),
                                      cleanLon(getEast(request)) - cleanLon(getWest(request)),
                                      cleanLat(getNorth(request)) - cleanLat(getSouth(request)));
    }

    /**
     * Set the geographic bounds
     *
     * @param rect  the bounds
     */
    public void setBounds(Rectangle2D.Double rect) {
        setWest(cleanLon(rect.getX()));
        setSouth(cleanLat(rect.getY()));
        setEast(cleanLon(west + rect.getWidth()));
        setNorth(cleanLat(south + rect.getHeight()));
    }

    /**
     * Initialize the Entry with these values
     *
     * @param name         The Entry name
     * @param description  The Entry description
     * @param parentEntry  the parent Entry
     * @param user         the loser
     * @param resource     the resource
     * @param category     the category
     * @param entryOrder _more_
     * @param createDate   the creation date
     * @param changeDate   the change date
     * @param startDate    the start date
     * @param endDate      the start date
     * @param values       the Entry values
     */
    public void initEntry(String name, String description, Entry parentEntry,
                          User user, Resource resource, String category,
                          int entryOrder, long createDate, long changeDate,
                          long startDate, long endDate, Object[] values) {
        this.id = id;
        setName(name);
        setDescription(description);
        this.parentEntry = parentEntry;
        this.user        = user;
        this.entryOrder  = entryOrder;
        setCreateDate(createDate);
        setChangeDate(changeDate);

        this.resource = resource;
        this.category = category;
        if ((category == null) || (category.length() == 0)) {
            this.category = typeHandler.getDefaultCategory();
        }
        if (this.category == null) {
            this.category = "";
        }
        setStartDate(startDate);
        setEndDate( endDate);
        this.values    = values;

        if (typeHandler != null) {
            typeHandler.initEntryHasBeenCalled(this);
        }

    }

    /**
       Set the MetadataChanged property.

       @param value The new value for MetadataChanged
    **/
    public void setMetadataChanged (boolean value) {
	metadataChanged = value;
    }

    /**
       Get the MetadataChanged property.

       @return The MetadataChanged
    **/
    public boolean getMetadataChanged () {
	return metadataChanged;
    }

    /**
     * Is this a File
     *
     * @return  true if a File
     */
    public boolean isFile() {
        return (resource != null) && resource.isFile();
    }

    public boolean isImage() {
        return getTypeHandler().isImage(this);
    }

    public boolean isFileType() {
        return (resource != null) && resource.isFileType();
    }

    public String getInsertSql() {
        return null;
    }

    /**
     * Set the resource property.
     *
     * @param value The new value for resource
     */
    public void setResource(Resource value) {
        resource = value;
    }

    /**
     * Get the resource property.
     *
     * @return The resource
     */
    public Resource getResource() {
        return resource;
    }

    /**
     * Set the date for this Entry
     *
     * @param value  the date (seconds)
     */
    public void setDate(long value) {
        setCreateDate(value);
        setChangeDate(value);
        setStartDate(value);
        setEndDate(value);
    }

    public void clearDate() {
	setStartAndEndDate(DateHandler.NULL_DATE);
    }

    /**
     * Set the StartDate property.
     *
     * @param value The new value for StartDate
     */
    public void setStartDate(long value) {
        startDate = value;
    }

    public void setStartDate(Date value) {
        if (value != null) {
            setStartDate(value.getTime()); 
        } else {
	    setStartDate(DateHandler.NULL_DATE);
	}
    }

    public void setEndDate(Date value) {
        if (value != null) {
            setEndDate(value.getTime());
        } else {
	    setEndDate(DateHandler.NULL_DATE);
	}
    }

    public void setStartAndEndDate(long value) {
        setStartDate(value);
        setEndDate(value);
    }

    /**
     * Get the StartDate property.
     *
     * @return The StartDate
     */
    public long getStartDate() {
        return startDate;
    }

    /**
     * Set the EndDate property.
     *
     * @param value The new value for EndDate
     */
    public void setEndDate(long value) {
        endDate = value;
    }

    /**
     * Get the EndDate property.
     *
     * @return The EndDate
     */
    public long getEndDate() {
        return endDate;
    }

    /**
     * Is this the top (first) entry?
     *
     * @return true if it is
     */
    public boolean isTopEntry() {
        return isGroup() && (getParentEntryId() == null);
    }

    /**
     *  Set the RemoteParentEntryId property.
     *
     *  @param value The new value for RemoteParentEntryId
     */
    public void setRemoteParentEntryId(String value) {
        remoteParentEntryId = value;
    }

    /**
     *  Get the RemoteParentEntryId property.
     *
     *  @return The RemoteParentEntryId
     */
    public String getRemoteParentEntryId() {
        return remoteParentEntryId;
    }

    /**
     * Is this the top (first) group?
     *
     * @return true if it is
     */
    public boolean isTopGroup() {
        return isTopEntry();
    }

    /**
     * Is this a Group?
     *
     * @return true if this is a Group?
     */
    public boolean isGroup() {
        if (isGroup) {
            return true;
        }
        if (typeHandler != null) {
            return typeHandler.isGroup();
        }

        return false;
    }

    /**
     * Set the Group Property
     *
     * @param g   true if a Group
     */
    public void setGroup(boolean g) {
        isGroup = g;
    }

    /**
     * Set the Type property.
     *
     * @param value The new value for Type
     */
    public void setTypeHandler(TypeHandler value) {
        typeHandler = value;
    }

    public void setMasterTypeHandler(TypeHandler value) {
        masterTypeHandler = value;
    }

    public TypeHandler getMasterTypeHandler() {
        if (masterTypeHandler != null) {
            return masterTypeHandler;
        }

        return getTypeHandler();
    }

    /**
     * Get the type
     *
     * @return  the type
     */
    public String getType() {
        return typeHandler.getType();
    }

    /**
     * Get the Type property.
     *
     * @return The Type
     */
    public TypeHandler getTypeHandler() {
	if(typeHandler!=null) return typeHandler.getRepository().getTypeHandler(typeHandler);
        return null;
    }

    /**
     * Set the Values property.
     *
     * @param value The new value for Values
     */
    public void setValues(Object[] value) {
        values = value;
    }

    /**
     * Get the Values property.
     *
     * @return The Values
     */
    public Object[] getValues() {
        return getValues(false);
    }

    /**
     * Get the Values property
     * @param clone true to clone the values
     * @return the values or a clone of them
     */
    public Object[] getValues(boolean clone) {
        if ((values == null) || !clone) {
            return values;
        }
        Object[] newValues = new Object[values.length];
        System.arraycopy(values, 0, newValues, 0, values.length);

        return newValues;
    }

    public int getColumnIndex(String name) {
        if (name == null) {
            return -1;
        }
        Column c = getTypeHandler().getColumn(name);

        if (c == null) {
            return -1;
        }

        return c.getOffset();
    }

    public Column getColumn(int index) {
	return getTypeHandler().findColumn(index);
    }
    public Column getColumn(String col) {
	return getTypeHandler().findColumn(col);
    }    

    public Object getValue(Request request,Column column,boolean useDefault) {
	if(column == null) {
	    return null;
	}
	Object value = column.getValue(request, this);
	if(value==null && useDefault)
	    value = column.getDflt();
	return value;
    }

    public Object getValue(Request request,Column column) {
	return getValue(request, column, false);
    }

    public Object getValue(Request request, int index) {
	return getValue(request, getTypeHandler().findColumn(index));
    }

    public Object getValue(Request request, String col,boolean useDefault) {
	return getValue(request, getColumn(col), useDefault);
    }    

    public Object getValue(Request request,String col) {
	return getValue(request,col,false);
    }

    public Object getValue(Request request,Column col,Object dflt) {
	Object o =  getValue(request,col,false);
	if(o==null) return dflt;
	return o;
    }

    public Object getValue(Request request,String col,Object dflt) {
	return getValue(request,getColumn(col),dflt);
    }

    public String getStringValue(Request request,Column col,String dflt) {
        Object  value = getValue(request, col);
	if(value==null) return dflt;
	return value.toString();

    }

    public String getEnumValue(Request request,String col,String dflt) {
	return getEnumValue(request, getColumn(col),dflt);
    }

    public String getEnumValue(Request request,Column col,String dflt) {
        Object  value = getValue(request, col);
	if(value==null) return dflt;
	return col.getEnumLabel(value.toString());

    }

    public String getStringValue(Request request,String col,String dflt) {
	return getStringValue(request, getColumn(col),dflt);
    }

    public String getStringValue(Request request,int index, String dflt) {
	return getStringValue(request, getColumn(index),dflt);
    }

    public double getDoubleValue(Request request,Column col,double dflt) {
        Object  value = getValue(request, col);
	if(value==null) return dflt;
	if(value instanceof Double) {
	    return ((Double)value).doubleValue();
	}

        String sValue = value.toString();
        if (sValue.length() == 0) {
            return dflt;
        }
        double retval = dflt;
        try {
            retval = Double.parseDouble(sValue);
        } catch (Exception e) {
            retval = dflt;
        }
        return retval;
    }

    public double getDoubleValue(Request request, int index, double dflt) {
	return getDoubleValue(request, getColumn(index), dflt);
    }

    public double getDoubleValue(Request request, String index, double dflt) {
	return getDoubleValue(request, getColumn(index), dflt);
    }

    public int getIntValue(Request request,Column col,int dflt) {
        Object  value = getValue(request, col);
	if(value==null) return dflt;
	if(value instanceof Integer) {
	    return ((Integer)value).intValue();
	}

        String sValue = value.toString();
        if (sValue.length() == 0) {
            return dflt;
        }
        int retval = dflt;
        try {
            retval = Integer.parseInt(sValue);
        } catch (Exception e) {
            retval = dflt;
        }
        return retval;
    }

    public int getIntValue(Request request, int index, int dflt) {
	return getIntValue(request,getColumn(index),dflt);
    }
    public int getIntValue(Request request, String index, int dflt) {
	return getIntValue(request,getColumn(index),dflt);
    }    

    public boolean getBooleanValue(Request request,Column col,boolean dflt) {
        Object  value = getValue(request, col);
	if(value==null) return dflt;
	if(value instanceof Boolean) {
	    return ((Boolean)value).booleanValue();
	}

        String sValue = value.toString();
        if (sValue.length() == 0) {
            return dflt;
        }
        boolean retval = dflt;
        try {
            retval = sValue.equals("true");
        } catch (Exception e) {
            retval = dflt;
        }
        return retval;
    }

    public boolean getBooleanValue(Request request,int index, boolean dflt) {
	return getBooleanValue(request, getColumn(index), dflt);
    }
    public boolean getBooleanValue(Request request,String index, boolean dflt) {
	return getBooleanValue(request, getColumn(index), dflt);
    }    

    public boolean setValue(String col, Object v) throws Exception {
	Column column = getTypeHandler().findColumn(col);
	return setValue(column, v);
    }

    public boolean setValue(Column column, Object v) throws Exception {
	if(column == null) {
	    return false;
	}
	column.setValue(this,v);
	return true;
	//	setValue(column.getOffset(),v);
    }

    public void setValue(int idx, Object v) {
        Object[] values = getTypeHandler().getEntryValues(this);
        if (idx >= values.length) {
            throw new IllegalArgumentException(
					       "Error in Entry.setValue: bad index: " + idx + " length is:"
					       + values.length);
        }
        values[idx] = v;
    }

    /**
     * Return a String representation of this Object
     *
     * @return a String representation of this Object
     */
    public String toString() {
        //        return name + " id:" + id + "  type:" + getTypeHandler();
        return name + " ";
    }

    public void printMe() {
        System.err.println(this.toString());
        if (values != null) {
            for (Object obj : values) {
                System.err.println("\tvalue:" + obj);
            }
        }

    }

    public boolean hasDate() {
        return (startDate != 0L) && (startDate != createDate);
    }

    public boolean hasCreateDate() {
        return (createDate != 0L);
    }

    public boolean hasStartDate() {
        return (startDate != 0L);
    }

    /**
     * Does this entry have a location defined
     *
     * @return true if this entry has a location defined
     */
    public boolean hasLocationDefined(Request request) {
	double north = getNorth(request);
	double west = getWest(request);	
	double south = getSouth(request);
	double east = getEast(request);		
        if ((south != NONGEO) && (east != NONGEO) && Utils.isReal(south)
	    && Utils.isReal(east) && !hasAreaDefined(request)) {
            if (Utils.between(east, -180, 180)
		&& Utils.between(south, -90, 90)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get the location (lat,lon) of the entry
     *
     * @return the location (lat,lon)
     */
    public double[] getLocation(Request request) {
        return new double[] { getSouth(request), getEast(request) };
    }

    /**
     * Get the latitude of the entry
     *
     * @return the latitude of the entry
     */
    public double getLatitude(Request request) {
        return getSouth(request);
    }

    /**
     * Get the longitude of the entry
     *
     * @return the longitude of the entry
     */
    public double getLongitude(Request request) {
        return getEast(request);
    }

    /**
     * Get the center of the location
     *
     * @return the center of the location - [latitude,longitude]
     */
    public double[] getCenter(Request request) {
        return new double[] { getSouth(request) + (getNorth(request) - getSouth(request)) / 2,
			      getEast(request) + (getWest(request) - getEast(request)) / 2 };
    }

    public boolean isGeoreferenced(Request request) {
        return hasAreaDefined(request) || hasLocationDefined(request);
    }

    /**
     * Does this entry have an area defined?
     *
     * @return true if this entry has an area defined
     */
    public boolean hasAreaDefined(Request request) {
	double north = getNorth(request);
	double west = getWest(request);	
	double south = getSouth(request);
	double east = getEast(request);		
        if ( !Utils.isReal(south) || !Utils.isReal(east)
	     || !Utils.isReal(north) || !Utils.isReal(west)) {
            return false;
        }
        if ( !(Utils.between(east, -180, 180)
	       && Utils.between(south, -90, 90)
	       && Utils.between(west, -180, 180)
	       && Utils.between(north, -90, 90))) {
            return false;
        }

        if ((south != NONGEO) && (east != NONGEO) && (north != NONGEO)
	    && (west != NONGEO)) {
            if ((south == north) && (east == west)) {
                return false;
            }

            return true;
        }

        return false;
    }

    public void normalizeLongitude() {
        if (east > 180) {
            double delta = (east % 180);
            east -= delta;
            west -= delta;
        }
    }

    /**
     * Trim the area resolution
     */
    public void trimAreaResolution() {
        double diff = (south - north);
        if (Math.abs(diff) > 1) {
            south = ((int) (south * 1000)) / 1000.0;
            north = ((int) (north * 1000)) / 1000.0;
        }
        diff = (east - west);
        if (Math.abs(diff) > 1) {
            east = ((int) (east * 1000)) / 1000.0;
            west = ((int) (west * 1000)) / 1000.0;
        }

    }

    /**
     * Clear the geographic bounds
     */
    public void clearArea() {
        south = north = east = west = NONGEO;
    }

    /**
     * Get the label for this Entry
     *
     * @return  the label
     */
    public String getLabel() {
        String label = getBaseLabel();
        if (label.length() > 0) {
            return label;
        }

        return getTypeHandler().getLabel() + ": " + new Date(startDate);
    }

    /**
     * Get the base label
     *
     * @return the base label
     */
    public String getBaseLabel() {
        if (Utils.stringDefined(name)) {
            return name;
        }
	//        if ((description != null) && (description.trim().length() > 0)) {
	//            return description;
	//        }
        return "";

    }

    /**
     * Set the location from the other Entry
     *
     * @param that  the other Entry
     */
    public void setLocation(Entry that) {
        setNorth(that.north);
        setSouth(that.south);
        setEast(that.east);
        setWest(that.west);
        this.altitudeTop    = that.altitudeTop;
        this.altitudeBottom = that.altitudeBottom;
    }

    /**
     * Set the location
     *
     * @param lat  the latitude
     * @param lon  the longitude
     */
    public void setLocation(double lat, double lon) {
        setLocation(lat, lon, Double.NaN);
    }

    public void setLocation(double lat, double lon, double alt) {
        setNorth(lat);
        setSouth(lat);
        setEast(lon);
        setWest(lon);
        this.altitudeTop    = alt;
        this.altitudeBottom = alt;
    }

    /**
     * Set the South property.
     *
     * @param value The new value for South
     */
    public void setSouth(double value) {
        south = value;
    }

    /**
     * Get the South property.
     *
     * @return The South
     */
    public double getSouth(Request request) {
        return filterGeo(request,((south == south)
				  ? south
				  : NONGEO));
    }

    /**
     * Set the Bounds property.
     *
     * @param bounds _more_
     */
    public void setBounds(Bounds bounds) {
        if (bounds != null) {
            setNorth(bounds.getNorth());
            setWest(bounds.getWest());
            setSouth(bounds.getSouth());
            setEast(bounds.getEast());

        }
    }

    /**
     * Set the North property.
     *
     * @param value The new value for North
     */
    public void setNorth(double value) {
        north = value;
    }

    public double filterGeo(Request request, double v) {
	if(request==null) return v;
	return request.filterGeo(this,v);
    }

    /**
     * Get the North property.
     *
     * @return The North
     */
    public double getNorth(Request request) {
        return filterGeo(request,((north == north)
				  ? north
				  : NONGEO));
    }

    /**
     * Set the Latitude Property
     *
     * @param value the Latitude Property
     */
    public void setLatitude(double value) {
        setNorth(value);
        setSouth(value);
    }

    /**
     * Set the Longitude Property
     *
     * @param value  the Longitude Property
     */
    public void setLongitude(double value) {
        east = value;
        west = value;
    }

    /**
     * Set the Altitude Property
     *
     * @param value the Altitude Property
     */
    public void setAltitude(double value) {
        altitudeTop    = value;
        altitudeBottom = value;
    }

    /**
     *  Set the AltitudeTop property.
     *
     *  @param value The new value for AltitudeTop
     */
    public void setAltitudeTop(double value) {
        altitudeTop = value;
    }

    /**
     *  Get the AltitudeTop property.
     *
     *  @return The AltitudeTop
     */
    public double getAltitudeTop() {
        return altitudeTop;
    }

    /**
     *  Set the AltitudeBottom property.
     *
     *  @param value The new value for AltitudeBottom
     */
    public void setAltitudeBottom(double value) {
        altitudeBottom = value;
    }

    /**
     *  Get the AltitudeBottom property.
     *
     *  @return The AltitudeBottom
     */
    public double getAltitudeBottom() {
        return altitudeBottom;
    }

    /**
     * Get the Altitude Property
     *
     * @return the Altitude Property
     */
    public double getAltitude() {
        return altitudeTop;
    }

    /**
     * Does this have a top altitude?
     *
     * @return  true if it does
     */
    public boolean hasAltitudeTop() {
        return (altitudeTop == altitudeTop) && (altitudeTop != NONGEO);
    }

    /**
     * Does this have a bottom altitude
     *
     * @return true if this has a bottom altitude
     */
    public boolean hasAltitudeBottom() {
        return (altitudeBottom == altitudeBottom)
	    && (altitudeBottom != NONGEO);
    }

    /**
     * Does this have an altitude defined?
     *
     * @return true if this has an altitude defined?
     */
    public boolean hasAltitude() {
        return hasAltitudeTop() && hasAltitudeBottom()
	    && (altitudeBottom == altitudeTop);
    }

    /**
     * Does this have a north defined?
     *
     * @return true if it does
     */
    public boolean hasNorth() {
        return (north == north) && (north != NONGEO);
    }

    /**
     * Does this have a south value defined?
     *
     * @return true if it does
     */
    public boolean hasSouth() {
        return (south == south) && (south != NONGEO);
    }

    /**
     * Does this have an east value defined?
     *
     * @return true if it does
     */
    public boolean hasEast() {
        return (east == east) && (east != NONGEO);
    }

    /**
     * Does this have a west value defined?
     *
     * @return true if it does
     */
    public boolean hasWest() {
        return (west == west) && (west != NONGEO);
    }

    /**
     * Set the East property.
     *
     * @param value The new value for East
     */
    public void setEast(double value) {
        east = value;
    }

    /**
     * Get the East property.
     *
     * @return The East
     */
    public double getEast(Request request) {
        return filterGeo(request,((east == east)
				  ? east
				  : NONGEO));
    }

    /**
     * Set the West property.
     *
     * @param value The new value for West
     */
    public void setWest(double value) {
        west = value;
    }

    /**
     * Get the West property.
     *
     * @return The West
     */
    public double getWest(Request request) {
        return filterGeo(request,((west == west)
				  ? west
				  : NONGEO));
    }

    /**
     * Set the Category property.
     *
     * @param value The new value for Category
     */
    public void setCategory(String value) {
        category = value;
    }

    /**
     * Get the Category property.
     *
     * @return The Category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Set the IsLocalFile property.
     *
     * @param value The new value for IsLocalFile
     */
    public void setIsLocalFile(boolean value) {
        isLocalFile = value;
    }

    /**
     * Get the IsLocalFile property.
     *
     * @return The IsLocalFile
     */
    public boolean getIsLocalFile() {
        return isLocalFile;
    }

    /**
     *  Set the Icon property.
     *
     *  @param value The new value for Icon
     */
    public void setIcon(String value) {
        icon = value;
    }

    /**
     *  Get the Icon property.
     *
     *  @return The Icon
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Get the IsRemoteEntry property.
     *
     * @return The IsRemoteEntry
     */
    public boolean getIsRemoteEntry() {
        return remoteServer != null;
    }

    /**
     * Set the RemoteServer property.
     *
     * @param value The new value for RemoteServer
     */
    public void setRemoteServer(ServerInfo value) {
        remoteServer = value;
    }

    /**
     * Get the RemoteServer property.
     *
     * @return The RemoteServer
     */
    public ServerInfo getRemoteServer() {
        return remoteServer;
    }

    /**
     * Set the RemoteUrl property.
     *
     * @param value The new value for RemoteUrl
     */
    public void setRemoteUrl(String value) {
        remoteUrl = value;
    }

    /**
     * Get the RemoteUrl property.
     *
     * @return The RemoteUrl
     */
    public String getRemoteUrl() {
        return remoteUrl;
    }

    /**
     * Set the RemoteId property.
     *
     * @param value The new value for RemoteId
     */
    public void setRemoteId(String value) {
        remoteId = value;
    }

    /**
     * Get the RemoteId property.
     *
     * @return The RemoteId
     */
    public String getRemoteId() {
        return remoteId;
    }

    public void clearTransientProperties() {
        transientProperties = new Hashtable();
    }

    /**
     * Get the transient property
     *
     * @param key  the property key
     *
     * @return  the property or null
     */
    public Object getTransientProperty(Object key) {
        return transientProperties.get(key);
    }

    /**
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Object getAndRemoveTransientProperty(Object key) {
        Object o = transientProperties.get(key);
        if (o != null) {
            transientProperties.remove(key);
        }

        return o;
    }

    /**
     * Add a transient property
     *
     * @param key   the key
     * @param value   the value
     */
    public void putTransientProperty(Object key, Object value) {
        transientProperties.put(key, value);
    }

    /**
     * Add a property
     * @param key   the property key
     * @param value The new value for property
     *
     * @throws Exception  problems
     */
    public void putProperty(String key, Object value) throws Exception {
        getProperties(true).put(key, value);
    }

    /**
     * Get the property
     *
     * @param key  the key
     *
     * @return  the property value or null
     *
     * @throws Exception  No properties
     */
    public Object getProperty(String key) throws Exception {
        Hashtable properties = getProperties();
        if (properties == null) {
            return null;
        }

        return properties.get(key);
    }

    /**
     *  Get the Properties property.
     *
     *  @return The Properties
     *
     * @throws Exception _more_
     */
    public Hashtable getProperties() throws Exception {
        return getProperties(false);
    }

    /**
     * Get the Properties property
     *
     * @param force  create if necessary
     *
     * @return  the properties
     *
     * @throws Exception  problems
     */
    public Hashtable getProperties(boolean force) throws Exception {
        if (properties == null) {
            if (propertiesString != null) {
                properties =
                    (Hashtable) Repository.decodeObject(propertiesString);
                propertiesString = null;
            }
            if ((properties == null) && force) {
                properties = new Hashtable();
            }
        }

        return this.properties;
    }

    /**
     *  Get the PropertiesString property.
     *
     *  @return The PropertiesString
     *
     * @throws Exception _more_
     */
    public String getPropertiesString() throws Exception {
        if (properties != null) {
            return Repository.encodeObject(properties);
        }

        return null;
    }

    /**
     * Initialize the Entry with these values
     *
     * @param name         The Entry name
     * @param description  The Entry description
     * @param parentEntry  the parent Entry
     * @param user         the loser
     * @param createDate   the creation date
     * @param changeDate   the change date
     */
    public void init(String name, String description, Entry parentEntry,
                     User user, long createDate, long changeDate) {
	setName(name);
        setDescription(description);
        this.parentEntry = parentEntry;
        this.user        = user;
        setCreateDate(createDate);
        setChangeDate(changeDate);
    }

    /**
     * Is this equal to the other Object?
     *
     * @param o  the other Object
     *
     * @return  true if they are equal
     */
    public boolean equals(Object o) {
	if(o==null) return false;
        if ( !o.getClass().equals(getClass())) {
            return false;
        }
        Entry that = (Entry) o;

        return equalsEntry(that);
    }

    /**
     * @return _more_
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public boolean equalsEntry(Entry that) {
        if (that == null) {
            return false;
        }

        return Misc.equals(this.id, that.id);
    }

    public boolean sameDate() {
        return (createDate == startDate) && (createDate == endDate);
    }

    /**
     *  Set the EntryOrder property.
     *
     *  @param value The new value for EntryOrder
     */
    public void setEntryOrder(int value) {
        entryOrder = value;
    }

    /**
     *  Get the EntryOrder property.
     *
     *  @return The EntryOrder
     */
    public int getEntryOrder() {
        return entryOrder;
    }

    /**
     * Set the CreateDate property.
     *
     * @param value The new value for CreateDate
     */
    public void setCreateDate(long value) {
        createDate = value;
    }

    /**
     * Get the CreateDate property.
     *
     * @return The CreateDate
     */
    public long getCreateDate() {
        return createDate;
    }

    /**
     * Set the ChangeDate property.
     *
     * @param value The new value for ChangeDate
     */
    public void setChangeDate(long value) {
        changeDate = value;
        //        if(getName().endsWith("ass")) {
        //            ucar.unidata.util.Misc.printStack("setChangeDate:" + getName() + " " + value,7);
        //        }
    }

    /**
     * Get the ChangeDate property.
     *
     * @return The ChangeDate
     */
    public long getChangeDate() {
        return changeDate;
    }

    /**
     * Set the Group property.
     *
     * @param value The new value for Group
     */
    public void setParentEntry(Entry value) {
        parentEntry = value;
        if (parentEntry != null) {
            parentEntryId = parentEntry.getId();
        } else {
            parentEntryId = null;
        }
    }

    /**
     * Get the parent Entry
     *
     * @return the parent Entry
     */
    public Entry getParentEntry() {
        return parentEntry;
    }

    public Entry getAncestor(String type) {
        if (this.getTypeHandler().isType(type)) {
            return this;
        }
        if (parentEntry != null) {
            return parentEntry.getAncestor(type);
        }

        return null;
    }

    /**
     * Set the ParentGroupId property.
     *
     * @param value The new value for ParentGroupId
     */
    public void setParentGroupId(String value) {
        setParentEntryId(value);
    }

    /**
     * Set the ParentEntryId property.
     *
     * @param value The new value for ParentEntryId
     */
    public void setParentEntryId(String value) {
        parentEntryId = value;
    }

    /**
     * Get the ParenEntryId
     *
     * @return the ParenEntryId
     */
    public String getParentEntryId() {
        return ((parentEntry != null)
                ? parentEntry.getId()
                : parentEntryId);
    }

    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
        if (name.length() > MAX_NAME_LENGTH) {
            name = name.substring(0, MAX_NAME_LENGTH - 1);
        }
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        if (name == null) {
            name = "";
        }

        return name;
    }

    /**
     * Set the Description property.
     *
     * @param value The new value for Description
     */
    public void setDescription(String value) {
	//        if (description == null) {
	//            return;
	//        }
        description = value;
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            description = description.substring(0,
						MAX_DESCRIPTION_LENGTH - 1);
        }
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
     * Set the Snippet property.
     *
     * @param value The new value for Snippet
     */
    public void setSnippet(String value) {
        snippet = value;
    }

    /**
     * Get the Snippet property.
     *
     * @return The Snippet
     */
    public String getSnippet() {
        return snippet;
    }

    /**
     * Set the Id property.
     *
     * @param value The new value for Id
     */
    public void setId(String value) {
        id = value;
    }

    /**
     * Get the Id property.
     *
     * @return The Id
     */
    public String getId() {
        return id;
    }

    /**
     *  Set the User property.
     *
     *  @param value The new value for User
     */
    public void setUser(User value) {
        user = value;
    }

    /**
     *  Get the User property.
     *
     *  @return The User
     */
    public User getUser() {
        return user;
    }

    public String getUserId() {
        if (user == null) {
            return UserManager.USER_ANONYMOUS;
        }

        return user.getId();
    }

    /**
     * Clear the metadata
     */
    public void clearMetadata() {
        metadata = null;
    }

    /**
     * Does this have any metaddata like value
     *
     * @param value  the metadata to check
     *
     * @return  true if it does
     */
    public boolean hasMetadata(Metadata value) {
        if (metadata == null) {
            return false;
        }

        return metadata.contains(value);
    }

    public boolean hasMetadataOfType(String type) {
        if (metadata == null) {
            return false;
        }
        for (Metadata myMetadata : metadata) {
            if (myMetadata.getType().equals(type)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Set the Metadata property.
     *
     * @param value The new value for Metadata
     */
    public void setMetadata(List<Metadata> value) {
        metadata = value;
    }

    /**
     * Get the Metadata property.
     *
     * @return The Metadata
     */
    public List<Metadata> getMetadataList(Request request) {
	return getMetadataList(request, true);
    }

    public List<Metadata> getMetadataList(Request request,boolean checkAccess) {	
	if(checkAccess && metadata!=null && request!=null) {
	    List<Metadata> restricted=new ArrayList<Metadata>();
	    if(MetadataManager.debugGetMetadata) System.err.println("Entry getMetadata:" + this +" list:" +metadata);
	    for(Metadata mtd: metadata) {
		MetadataType type=mtd.getMetadataType();
		if(type==null) {
		    if(MetadataManager.debugGetMetadata) 
			System.err.println("\tnull type:" + mtd);
		} else if(!type.isPrivate(request,this,mtd)) {
		    if(MetadataManager.debugGetMetadata) 
			System.err.println("\tok:" + mtd);
		    restricted.add(mtd);
		}else {
		    if(MetadataManager.debugGetMetadata) 
			System.err.println("\tprivate:" + mtd);
		}
	    }
	    if(MetadataManager.debugGetMetadata) System.err.println("\trestricted list:" +restricted);
	    return restricted;
	}
        return metadata;
    }

    public void clearAssociations() {
        associations = null;
    }

    /**
     * Set the Associations property.
     *
     * @param value The new value for Associations
     */
    public void setAssociations(List<Association> value) {
        associations = value;
    }

    /**
     * Get the Associations property.
     *
     * @return The Associations
     */
    public List<Association> getAssociations() {
        return associations;
    }

    public void addAssociation(Association value) {
        if (associations == null) {
            associations = new ArrayList<Association>();
        }
        associations.add(value);

    }

    /**
     * Set the Comments property.
     *
     * @param value The new value for Comments
     */
    public void setComments(List<Comment> value) {
        comments = value;
    }

    /**
     * Get the Comments property.
     *
     * @return The Comments
     */
    public List<Comment> getComments() {
        return comments;
    }

    public void addComment(Comment value) {
        if (comments == null) {
            comments = new ArrayList<Comment>();
        }
        comments.add(value);

    }

    /**
     * Set the Permissions property.
     *
     * @param value The new value for Permissions
     */
    public void setPermissions(List<Permission> value) {
        permissions = value;
    }

    /**
     * Get the Permissions property.
     *
     * @return The Permissions
     */
    public List<Permission> getPermissions() {
        return permissions;
    }

    public void addPermission(Permission value) {
        if (permissions == null) {
            permissions = new ArrayList<Permission>();
        }
        permissions.add(value);

    }

    public boolean isDummy() {
        return isDummy;
    }

    /**
     *  Set the Children property.
     *
     *  @param value The new value for Children
     */
    public void setChildren(List<Entry> value) {
        children = value;
    }

    /**
     *  Get the Children property.
     *
     *  @return The Children
     */
    public List<Entry> getChildren() {
        return children;
    }

    /**
     *  Set the XmlNode property.
     *
     *  @param value The new value for XmlNode
     */
    public void setXmlNode(Element value) {
        xmlNode = value;
    }

    /**
     *  Get the XmlNode property.
     *
     *  @return The XmlNode
     */
    public Element getXmlNode() {
        return xmlNode;
    }

    /**
       Set the CacheOk property.

       @param value The new value for CacheOk
    **/
    public void setCacheOk (boolean value) {
	cacheOk = value;
    }

    /**
       Get the CacheOk property.

       @return The CacheOk
    **/
    public boolean getCacheOk () {
	return cacheOk;
    }

    /**
       Set the CacheActiveLimit property.

       @param value The new value for CacheActiveLimit
    **/
    public void setCacheActiveLimit (long value) {
	cacheActiveLimit = value;
    }

    /**
       Get the CacheActiveLimit property.

       @return The CacheActiveLimit
    **/
    public long getCacheActiveLimit () {
	return cacheActiveLimit;
    }

    public static class EntryHistory {
	public Date date;
	public String id;
	public String description;
	public String name;
	public Hashtable props;
	public EntryHistory(Entry entry) {
	    date = new Date();
	    this.id = entry.getId();
	    this.name = entry.getName();
	    this.description = entry.getDescription();
	}

	public void putProperty(Object key, Object value) {
	    if(props==null) props = new Hashtable();
	    props.put(key,value);
	}
	public Object getProperty(Object key,Object dflt) {
	    if(props==null) return  dflt;
	    Object v = props.get(key);
	    if(v==null) return dflt;
	    return v;
	}	

	public Date getDate() {
	    return date;
	}

	public String getDescription() {
	    return description;
	}

    }

}
