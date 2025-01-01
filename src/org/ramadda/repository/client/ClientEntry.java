/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.client;


import org.ramadda.repository.Constants;
import org.ramadda.repository.Resource;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.xml.XmlUtil;


import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 * Class Entry _more_
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class ClientEntry implements Constants {

    /** _more_ */
    public static final double NONGEO = -999999;

    /** _more_ */
    private String id;

    /** _more_ */
    private String name = "";

    /** _more_ */
    private String description = "";

    /** _more_ */
    private String parentGroupId;

    /** _more_ */
    private String user;

    /** _more_ */
    private long createDate;

    /** _more_ */
    private long startDate;

    /** _more_ */
    private long endDate;


    /** _more_ */
    private Resource resource = new Resource();

    /** _more_ */
    private String dataType;


    /** _more_ */
    private double south = NONGEO;

    /** _more_ */
    private double north = NONGEO;

    /** _more_ */
    private double east = NONGEO;

    /** _more_ */
    private double west = NONGEO;


    /** _more_ */
    private List<Service> services = new ArrayList<Service>();


    /** _more_ */
    private List<ClientEntry> children = new ArrayList<ClientEntry>();

    /** _more_ */
    private String type;


    /** _more_ */
    private boolean isGroup = false;


    /**
     * _more_
     *
     * @param id _more_
     */
    public ClientEntry(String id) {
        this.id = id;
    }



    /**
     * _more_
     *
     * @param node _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static ClientEntry getEntry(Element node) throws Exception {
        ClientEntry entry = new ClientEntry(XmlUtil.getAttribute(node,
                                ATTR_ID));
        entry.init(node);

        return entry;
    }



    /**
     * _more_
     *
     * @param node _more_
     *
     * @throws Exception _more_
     */
    public void init(Element node) throws Exception {
        setName(XmlUtil.getAttribute(node, ATTR_NAME));
        setType(XmlUtil.getAttribute(node, ATTR_TYPE, ""));
        setParentGroupId(XmlUtil.getAttribute(node, ATTR_GROUP, ""));
        setIsGroup(XmlUtil.getAttribute(node, ATTR_ISGROUP, false));
        north = XmlUtil.getAttribute(node, ATTR_NORTH, north);
        south = XmlUtil.getAttribute(node, ATTR_SOUTH, south);
        east  = XmlUtil.getAttribute(node, ATTR_EAST, east);
        west  = XmlUtil.getAttribute(node, ATTR_WEST, west);


        String desc = XmlUtil.getGrandChildText(node, TAG_DESCRIPTION);
        if (desc != null) {
            setDescription(desc);
        }
        if (XmlUtil.hasAttribute(node, ATTR_RESOURCE)) {
            setResource(new Resource(XmlUtil.getAttribute(node,
                    ATTR_RESOURCE), XmlUtil.getAttribute(node,
                        ATTR_RESOURCE_TYPE)));
        }

        NodeList elements = XmlUtil.getElements(node);
        for (int i = 0; i < elements.getLength(); i++) {
            Element childNode = (Element) elements.item(i);
            if (childNode.getTagName().equals(TAG_ENTRY)) {
                ClientEntry childEntry = getEntry(childNode);
                addChild(childEntry);
            } else if (childNode.getTagName().equals(TAG_SERVICE)) {
                addService(
                    new ClientEntry.Service(
                        XmlUtil.getAttribute(childNode, ATTR_TYPE),
                        XmlUtil.getAttribute(childNode, ATTR_URL)));
            } else {
                System.err.println("Unknown xml tag:"
                                   + XmlUtil.toString(childNode));
            }
        }


    }



    /**
     * _more_
     *
     * @return _more_
     */
    public List<Service> getServices() {
        return services;
    }

    /**
     * _more_
     *
     * @param service _more_
     */
    public void addService(Service service) {
        services.add(service);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<ClientEntry> getChildren() {
        return children;
    }

    /**
     * _more_
     *
     *
     * @param child _more_
     */
    public void addChild(ClientEntry child) {
        children.add(child);
    }

    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !o.getClass().equals(getClass())) {
            return false;
        }
        ClientEntry that = (ClientEntry) o;
        if ((this.id == null) && (that.id == null)) {
            return true;
        }
        if ((this.id == null) && (that.id != null)) {
            return false;
        }
        if ((this.id != null) && (that.id == null)) {
            return false;
        }

        return this.id.equals(that.id);
    }


    /**
     *  Set the Type property.
     *
     *  @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
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
     *  Set the IsGroup property.
     *
     *  @param value The new value for IsGroup
     */
    public void setIsGroup(boolean value) {
        isGroup = value;
    }

    /**
     *  Get the IsGroup property.
     *
     *  @return The IsGroup
     */
    public boolean getIsGroup() {
        return isGroup;
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
     * Set the ParentId property.
     *
     * @param value The new value for ParentId
     */
    public void setParentGroupId(String value) {
        parentGroupId = value;
    }

    /**
     * Get the ParentId property.
     *
     * @return The ParentId
     */
    public String getParentGroupId() {
        return parentGroupId;
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
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
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
    public void setUser(String value) {
        user = value;
    }

    /**
     *  Get the User property.
     *
     *  @return The User
     */
    public String getUser() {
        return user;
    }





    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return toString("");
    }


    /**
     * _more_
     *
     * @param prefix _more_
     *
     * @return _more_
     */
    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer();
        sb.append(prefix + "entry:" + name + " id:" + id + " type:" + type);
        sb.append("\n");
        for (Service service : services) {
            sb.append(prefix + "    service: " + service);
            sb.append("\n");
        }
        for (ClientEntry child : children) {
            sb.append(prefix + "" + child.toString(prefix + "\t"));
            sb.append("\n");
        }

        return sb.toString();
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
     * _more_
     *
     * @return _more_
     */
    public boolean hasNorth() {
        return (north == north) && (north != NONGEO);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasSouth() {
        return (south == south) && (south != NONGEO);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasEast() {
        return (east == east) && (east != NONGEO);
    }

    /**
     * _more_
     *
     * @return _more_
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
     * Class Service _more_
     *
     *
     * @author RAMADDA Development Team
     */
    public static class Service {

        /** _more_ */
        private String type;

        /** _more_ */
        private String url;

        /**
         * _more_
         *
         * @param type _more_
         * @param url _more_
         */
        public Service(String type, String url) {
            this.type = type;
            this.url  = url;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            return "type:" + type + " url:" + url;

        }

        /**
         *  Set the Type property.
         *
         *  @param value The new value for Type
         */
        public void setType(String value) {
            type = value;
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
         *  Set the Url property.
         *
         *  @param value The new value for Url
         */
        public void setUrl(String value) {
            url = value;
        }

        /**
         *  Get the Url property.
         *
         *  @return The Url
         */
        public String getUrl() {
            return url;
        }



    }


}
