/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class ServiceInfo {

    /** _more_ */
    public static final String TYPE_KML = "kml";

    /** _more_ */
    public static final String TYPE_WMS = "wms";

    /** _more_ */
    public static final String TYPE_GRID = "grid";


    /** _more_ */
    public static final String TYPE_NA = "na";

    /** _more_ */
    private String type;

    /** _more_ */
    private String name;

    /** _more_ */
    private String url;

    /** _more_ */
    private String icon;

    /** _more_ */
    private String mimeType;


    /**
     * _more_
     *
     * @param type _more_
     * @param name _more_
     * @param url _more_
     */
    public ServiceInfo(String type, String name, String url) {
        this(type, name, url, null);
    }

    /**
     * _more_
     *
     * @param type _more_
     * @param name _more_
     * @param url _more_
     * @param icon _more_
     */
    public ServiceInfo(String type, String name, String url, String icon) {
        this(type, name, url, icon, null);
    }

    /**
     * _more_
     *
     * @param type _more_
     * @param name _more_
     * @param url _more_
     * @param icon _more_
     * @param mimeType _more_
     */
    public ServiceInfo(String type, String name, String url, String icon,
                       String mimeType) {
        this.type     = type;
        this.name     = name;
        this.url      = url;
        this.icon     = icon;
        this.mimeType = mimeType;
    }


    /**
     *
     * @return _more_
     */
    public String toString() {
        return "service:" + name + " url:" + url;
    }

    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public boolean isType(String type) {
        return this.type.equals(type);
    }

    /**
     * _more_
     *
     * @param object _more_
     *
     * @return _more_
     */
    @Override
    public boolean equals(Object object) {
        if ( !(object instanceof ServiceInfo)) {
            return false;
        }
        ServiceInfo that = (ServiceInfo) object;

        return this.url.equals(that.url);
    }


    /**
     *
     * @return _more_
     */
    @Override
    public int hashCode() {
        return url.hashCode();
    }


    /**
     *  Set the Type property.
     *
     *  @param value The new value for Type
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     *  Get the Type property.
     *
     *  @return The Type
     */
    public String getType() {
        return this.type;
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
     *  Set the Url property.
     *
     *  @param value The new value for Url
     */
    public void setUrl(String value) {
        this.url = value;
    }

    /**
     *  Get the Url property.
     *
     *  @return The Url
     */
    public String getUrl() {
        return this.url;
    }


    /**
     *  Set the Icon property.
     *
     *  @param value The new value for Icon
     */
    public void setIcon(String value) {
        this.icon = value;
    }

    /**
     *  Get the Icon property.
     *
     *  @return The Icon
     */
    public String getIcon() {
        return this.icon;
    }

    /**
     *  Set the MimeType property.
     *
     *  @param value The new value for MimeType
     */
    public void setMimeType(String value) {
        mimeType = value;
    }

    /**
     *  Get the MimeType property.
     *
     *  @return The MimeType
     */
    public String getMimeType() {
        return mimeType;
    }


}
