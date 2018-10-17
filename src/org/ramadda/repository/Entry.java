/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

package org.ramadda.repository;


import org.ramadda.repository.auth.Permission;
import org.ramadda.repository.auth.User;
import org.ramadda.repository.auth.UserManager;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.type.Column;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.repository.util.ServerInfo;

import org.ramadda.util.Utils;

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
public class Entry implements Cloneable {

    /** ID delimiter */
    public static final String IDDELIMITER = ":";

    /** path delimiter */
    public static final String PATHDELIMITER = "/";

    /** non-geo identifier */
    public static final double NONGEO = -999999;


    /** _more_ */
    public static final int MAX_DESCRIPTION_LENGTH = 15000;

    /** _more_ */
    public static final int MAX_NAME_LENGTH = 200;

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

    /** the parent entry */
    private Entry parentEntry;

    /** the parent entry id */
    private String parentEntryId;

    /** the tree id */
    private String treeId;


    /** the user (owner) */
    private User user;

    /** the create date */
    private long createDate = 0L;

    /** the change date */
    private long changeDate = 0L;

    /** is this a stoopid entry */
    boolean isDummy = false;

    /** the associated values (columns) */
    Object[] values;

    /** the resource */
    private Resource resource = new Resource();

    /** the category */
    private String category;

    /** the type handler for this entry */
    private TypeHandler typeHandler;

    /** _more_ */
    private TypeHandler masterTypeHandler;

    /** the start date */
    private long startDate = 0L;

    /** the end date */
    private long endDate = 0L;

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


    /** _more_ */
    private ServerInfo remoteServer;

    /** _more_ */
    private String remoteUrl;




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


    /** List of subgroups */
    List<Entry> subGroups;

    /** List of subentries */
    List<Entry> subEntries;


    /** the chillens ids */
    private List<String> childIds;

    /** _more_ */
    private List<Entry> children;

    /** _more_ */
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
        this(handler, isDummy, "Search Results");
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
        //        if(debug)
        //            System.err.println ("getFullName:" + name);
        Entry parent = getParentEntry();
        if (parent != null) {
            String parentName = parent.getFullName(encodeForUrl);

            //            if(debug)
            //                System.err.println ("parent name:" + parentName);
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


    /**
     * _more_
     *
     * @param template _more_
     */
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

        if (template.getMetadata() != null) {
            List<Metadata> thisMetadata = new ArrayList<Metadata>();
            for (Metadata metadata : template.getMetadata()) {
                metadata.setEntryId(getId());
                thisMetadata.add(metadata);
            }
            setMetadata(thisMetadata);
        }


        if (template.hasCreateDate()) {
            setCreateDate(template.getCreateDate());
            setChangeDate(template.getChangeDate());
        }
        if (template.hasStartDate()) {
            setStartDate(template.getStartDate());
            setEndDate(template.getEndDate());
        }

        setNorth(template.getNorth());
        setSouth(template.getSouth());
        setEast(template.getEast());
        setWest(template.getWest());
        this.altitudeTop    = template.altitudeTop;
        this.altitudeBottom = template.altitudeBottom;

        Object[] values = template.getValues();
        this.values = values;

    }

    /**
     * _more_
     *
     * @param lat _more_
     *
     * @return _more_
     */
    private double cleanLat(double lat) {
        return Math.max(Math.min(lat, 90), -90);
    }

    /**
     * _more_
     *
     * @param lon _more_
     *
     * @return _more_
     */
    private double cleanLon(double lon) {
        return Math.max(Math.min(lon, 180), -180);
    }

    /**
     * Get the geographic bounds
     *
     * @return  the bounds
     */
    public Rectangle2D.Double getBounds() {
        return new Rectangle2D.Double(cleanLon(west), cleanLat(south),
                                      cleanLon(east) - cleanLon(west),
                                      cleanLat(north) - cleanLat(south));
    }


    /**
     * Set the geographic bounds
     *
     * @param rect  the bounds
     */
    public void setBounds(Rectangle2D.Double rect) {
        west  = cleanLon(rect.getX());
        south = cleanLat(rect.getY());
        east  = cleanLon(west + rect.getWidth());
        north = cleanLat(south + rect.getHeight());
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
     * @param createDate   the creation date
     * @param changeDate   the change date
     * @param startDate    the start date
     * @param endDate      the start date
     * @param values       the Entry values
     */
    public void initEntry(String name, String description, Entry parentEntry,
                          User user, Resource resource, String category,
                          long createDate, long changeDate, long startDate,
                          long endDate, Object[] values) {
        //        super.init(name, description, parentEntry, user, createDate,changeDate);
        this.id = id;
        setName(name);
        setDescription(description);
        this.parentEntry = parentEntry;
        this.user        = user;

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
        this.startDate = startDate;
        this.endDate   = endDate;
        this.values    = values;
    }


    /**
     * Is this a File
     *
     * @return  true if a File
     */
    public boolean isFile() {
        return (resource != null) && resource.isFile();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isImage() {
        return getTypeHandler().isImage(this);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isFileType() {
        return (resource != null) && resource.isFileType();
    }


    /**
     * _more_
     *
     * @return _more_
     */
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




    /**
     * Set the StartDate property.
     *
     * @param value The new value for StartDate
     */
    public void setStartDate(long value) {
        startDate = value;
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
     * Is this the top (first) group?
     *
     * @return true if it is
     * @deprecated use isTopEntry
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

    /**
     * _more_
     *
     * @param value _more_
     */
    public void setMasterTypeHandler(TypeHandler value) {
        masterTypeHandler = value;
    }

    /**
     * _more_
     *
     * @return _more_
     */
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
        return typeHandler;
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

    /**
     * _more_
     *
     * @param index _more_
     *
     * @return _more_
     */
    public Object getValue(int index) {
        if ((values == null) || (index >= values.length)
                || (values[index] == null)) {
            return null;
        }

        return values[index];
    }



    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
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


    /**
     * Get the string value of the values index
     *
     * @param index  the values index
     * @param dflt   the default value
     *
     * @return  a String representation of the indexed value
     */
    public String getValue(int index, String dflt) {
        if ((values == null) || (index < 0) || (index >= values.length)
                || (values[index] == null)) {
            return dflt;
        }

        return values[index].toString();
    }

    /**
     * Get the indexed value as a double
     *
     * @param index  index in getValues array;
     * @param dflt   the default value
     *
     * @return  the double value (or dflt)
     */
    public double getValue(int index, double dflt) {
        String sValue = getValue(index, "");
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

    /**
     * Get the indexed value as a double
     *
     * @param index  index in getValues array;
     * @param dflt   the default value
     *
     * @return  the boolean value (or dflt)
     */
    public boolean getValue(int index, boolean dflt) {
        String sValue = getValue(index, "");
        if (sValue.length() == 0) {
            return dflt;
        }
        boolean retval = dflt;
        try {
            retval = Boolean.parseBoolean(sValue);
        } catch (Exception e) {
            retval = dflt;
        }

        return retval;
    }

    /**
     * _more_
     *
     * @param idx _more_
     * @param v _more_
     */
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
        return name + " id:" + id + "  type:" + getTypeHandler();
    }

    public void printMe() {
        System.err.println(this.toString());
        if(values!=null) {
            for(Object obj: values) {
                System.err.println("\tvalue:" + obj);
            }
        }

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasDate() {
        return (startDate != 0L) && (startDate != createDate);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasCreateDate() {
        return (createDate != 0L);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasStartDate() {
        return (startDate != 0L);
    }

    /**
     * Does this entry have a location defined
     *
     * @return true if this entry has a location defined
     */
    public boolean hasLocationDefined() {
        if ((south != NONGEO) && (east != NONGEO) && !hasAreaDefined()) {
            return true;
        }

        return false;
    }

    /**
     * Get the location (lat,lon) of the entry
     *
     * @return the location (lat,lon)
     */
    public double[] getLocation() {
        return new double[] { south, east };
    }

    /**
     * Get the latitude of the entry
     *
     * @return the latitude of the entry
     */
    public double getLatitude() {
        return south;
    }

    /**
     * Get the longitude of the entry
     *
     * @return the longitude of the entry
     */
    public double getLongitude() {
        return east;
    }


    /**
     * Get the center of the location
     *
     * @return the center of the location - [latitude,longitude]
     */
    public double[] getCenter() {
        return new double[] { south + (north - south) / 2,
                              east + (west - east) / 2 };
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isGeoreferenced() {
        return hasAreaDefined() || hasLocationDefined();
    }

    /**
     * Does this entry have an area defined?
     *
     * @return true if this entry has an area defined
     */
    public boolean hasAreaDefined() {
        if ((south != NONGEO) && (east != NONGEO) && (north != NONGEO)
                && (west != NONGEO)) {
            if ((south == north) && (east == west)) {
                return false;
            }

            return true;
        }

        return false;
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
        if ((name != null) && (name.trim().length() > 0)) {
            return name;
        }
        if ((description != null) && (description.trim().length() > 0)) {
            return description;
        }

        return "";

    }



    /**
     * Set the location from the other Entry
     *
     * @param that  the other Entry
     */
    public void setLocation(Entry that) {
        this.north          = that.north;
        this.south          = that.south;
        this.east           = that.east;
        this.west           = that.west;
        this.altitudeTop    = that.altitudeTop;
        this.altitudeBottom = that.altitudeBottom;
    }


    /**
     * Set the location
     *
     * @param lat  the latitude
     * @param lon  the longitude
     * @param alt  the altitude
     */
    public void setLocation(double lat, double lon, double alt) {
        this.north          = lat;
        this.south          = lat;
        this.east           = lon;
        this.west           = lon;
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
    public double getSouth() {
        return ((south == south)
                ? south
                : NONGEO);
    }

    /**
     * Set the North property.
     *
     * @param value The new value for North
     */
    public void setNorth(double value) {
        north = value;
    }

    /**
     * Get the North property.
     *
     * @return The North
     */
    public double getNorth() {
        return ((north == north)
                ? north
                : NONGEO);
    }

    /**
     * Set the Latitude Property
     *
     * @param value the Latitude Property
     */
    public void setLatitude(double value) {
        north = value;
        south = value;
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
    public double getEast() {
        return ((east == east)
                ? east
                : NONGEO);
    }

    /**
     * Get the lat/lon bounds
     *
     * @return  the bounds (NW,NE,SE,SW,NW)
     */
    public double[][] getLatLonBounds() {
        return new double[][] {
            { north, north, south, south, north },
            { west, east, east, west, west }
        };
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
    public double getWest() {
        return ((west == west)
                ? west
                : NONGEO);
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
     *  Set the SubGroups property.
     *
     *  @param value The new value for SubGroups
     */
    public void setSubGroups(List<Entry> value) {
        subGroups = value;
    }

    /**
     *  Get the SubGroups property.
     *
     *  @return The SubGroups
     */
    public List<Entry> getSubGroups() {
        return subGroups;
    }

    /**
     * Set the SubEntries property.
     *
     * @param value The new value for SubEntries
     */
    public void setSubEntries(List<Entry> value) {
        subEntries = value;
    }

    /**
     * Get the SubEntries property.
     *
     * @return The SubEntries
     */
    public List<Entry> getSubEntries() {
        return subEntries;
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
        if ( !o.getClass().equals(getClass())) {
            return false;
        }
        Entry that = (Entry) o;

        return Misc.equals(this.id, that.id);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean sameDate() {
        return (createDate == startDate) && (createDate == endDate);
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
     * @deprecated use setParentEntry
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


    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
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
        if (description == null) {
            return;
        }
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

    /**
     * _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
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
    public List<Metadata> getMetadata() {
        return metadata;
    }



    /**
     * _more_
     */
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


    /**
     * _more_
     *
     * @param value _more_
     */
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

    /**
     * _more_
     *
     * @param value _more_
     */
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
        permissions   = value;
        permissionMap = new Hashtable();
        if (permissions != null) {
            for (Permission permission : permissions) {
                permissionMap.put(permission.getAction(),
                                  permission.getRoles());
            }
        }
    }

    /**
     * _more_
     *
     * @param action _more_
     *
     * @return _more_
     */
    public List getRoles(String action) {
        return (List) permissionMap.get(action);
    }


    /**
     * Get the Permissions property.
     *
     * @return The Permissions
     */
    public List<Permission> getPermissions() {
        return permissions;
    }




    /**
     * _more_
     *
     * @param value _more_
     */
    public void addPermission(Permission value) {
        if (permissions == null) {
            permissions = new ArrayList<Permission>();
        }
        permissions.add(value);

    }


    /**
     * _more_
     *
     * @return _more_
     */
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



}
