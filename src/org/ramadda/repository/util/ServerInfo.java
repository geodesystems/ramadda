/**
   Copyright (c) 2008-2026 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.util;

import org.ramadda.repository.Constants;
import org.ramadda.repository.RepositoryBase;
import org.ramadda.util.Utils;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.util.Misc;
import org.ramadda.util.MyXmlUtil;

import java.net.URL;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class ServerInfo implements Constants {
    public static final String TAG_INFO_REPOSITORY = "repositoryinfo";
    public static final String TAG_INFO_DESCRIPTION = "description";
    public static final String TAG_INFO_SSLPORT = "sslport";
    public static final String TAG_INFO_URL = "url";    
    public static final String TAG_INFO_PORT = "port";
    public static final String TAG_INFO_HOSTNAME = "hostname";
    public static final String TAG_INFO_BASEPATH = "basepath";
    public static final String TAG_INFO_TITLE = "title";
    public static final String TAG_INFO_SLUG = "slug";
    public static final String TAG_INFO_SEARCHROOT = "searchroot";        
    public static final String TAG_INFO_EMAIL = "email";
    public static final String TAG_INFO_ISREGISTRY = "isregistry";
    public static final String ID_THIS = "this";

    private String url;
    private String hostname;
    private int port;
    private int sslPort = -1;
    private String basePath;
    private String title;
    private String description;
    private String email;
    private boolean isRegistry = false;
    private boolean enabled = false;
    private boolean live = true;    
    private String searchRoot;
    private String slug;

    public ServerInfo(URL url, String title, String description) {
        this.url         = url.toString();
        this.hostname    = url.getHost();
        this.port        = url.getPort();
        this.basePath    = url.getPath();
        this.title       = title;
        this.description = description;
    }

    public ServerInfo(String url, String hostname, int port, String title,
                      String description) {
        this(url, hostname, port, -1, "/repository", title, description, "",
             false, false,false,"","");
    }

    public ServerInfo(String url, Element element) {
	this(element);
	this.url = url;
    }

    public ServerInfo(Element element) {
        //        System.err.println("server:" + MyXmlUtil.toString(element));
        this.hostname = clean(MyXmlUtil.getGrandChildText(element, TAG_INFO_HOSTNAME,""));
        this.port = Integer.parseInt(MyXmlUtil.getGrandChildText(element,
							       TAG_INFO_PORT, "80"));

        String sslPortString = MyXmlUtil.getGrandChildText(element,
							 TAG_INFO_SSLPORT, null);

        if (sslPortString != null) {
            this.sslPort = Integer.parseInt(sslPortString);
        } else {
            this.sslPort = -1;
        }
        this.basePath = clean(MyXmlUtil.getGrandChildText(element, TAG_INFO_BASEPATH,"/repository"));
        this.title = clean(MyXmlUtil.getGrandChildText(element, TAG_INFO_TITLE, ""));
        this.slug = clean(MyXmlUtil.getGrandChildText(element, TAG_INFO_SLUG, ""));	
	this.searchRoot=clean(MyXmlUtil.getGrandChildText(element, TAG_INFO_SEARCHROOT, ""));	
        this.description = clean(MyXmlUtil.getGrandChildText(element, TAG_INFO_DESCRIPTION, ""));
        this.email = clean(MyXmlUtil.getGrandChildText(element, TAG_INFO_EMAIL, ""));
        this.url = clean(MyXmlUtil.getGrandChildText(element, TAG_INFO_URL, ""));
        this.isRegistry = true;
    }

    private String clean(String s) {
	if(s!=null) return HtmlUtils.sanitizeString(Utils.stripTags(s));
	return s;
    }

    public ServerInfo(String url, String hostname, int port, int sslPort,
                      String basePath, String title, String description,
                      String email,
		      boolean isRegistry,
		      boolean enabled,
		      boolean live,
		      String root,String slug) {
        this.url         = url;
        this.hostname    = hostname;
        this.port        = port;
        this.sslPort     = sslPort;
        this.basePath    = basePath;
        this.title       = title;
        this.description = description;
        this.email       = email;
        this.isRegistry  = isRegistry;
        this.enabled     = enabled;
        this.live        = live;	
	this.searchRoot = root;
	this.slug  =slug;
    }

    public String toString() {
        return getLabel() +" " + getUrl();
    }

    public int hashCode() {
        return getId().hashCode();
    }

    public boolean equals(Object o) {
        if ( !(o instanceof ServerInfo)) {
            return false;
        }
        ServerInfo that = (ServerInfo) o;

        return this.getId().equals(that.getId());
    }

    public Element toXml(RepositoryBase repository, Document doc)
	throws Exception {
        Element info = MyXmlUtil.create(doc, TAG_INFO_REPOSITORY, null,
                                      new String[] {});
        MyXmlUtil.create(doc, TAG_INFO_DESCRIPTION, info, description, null);
        MyXmlUtil.create(doc, TAG_INFO_TITLE, info, title, null);
        MyXmlUtil.create(doc, TAG_INFO_URL, info, getUrl());
        MyXmlUtil.create(doc, TAG_INFO_SLUG, info, slug!=null?slug:"", null);
        MyXmlUtil.create(doc, TAG_INFO_SEARCHROOT, info, searchRoot!=null?searchRoot:"", null);
        MyXmlUtil.create(doc, TAG_INFO_HOSTNAME, info, hostname, null);
        MyXmlUtil.create(doc, TAG_INFO_BASEPATH, info, basePath, null);
        MyXmlUtil.create(doc, TAG_INFO_EMAIL, info,
                       repository.getProperty(PROP_ADMIN_EMAIL, ""), null);
        MyXmlUtil.create(doc, TAG_INFO_PORT, info, "" + port, null);
        if (sslPort > 0) {
            MyXmlUtil.create(doc, TAG_INFO_SSLPORT, info, "" + sslPort, null);
        }
        MyXmlUtil.create(doc, TAG_INFO_ISREGISTRY, info, "" + isRegistry, null);

        return info;
    }

    public String getId() {
        return getUrl();
    }

    public String getHref(String extra) {
        return getHref(extra, false);
    }

    public String getHref(String extra, boolean includeUrl) {
        return HtmlUtils.href("http://" + hostname + ":" + port + basePath,
                              getLabel(includeUrl), extra);
    }

    public String getUrl() {
        if (url != null) {
            return url;
        }

	if(sslPort>0) {
	    if(sslPort==443)
		return "https://" + hostname + basePath;
            return "https://" + hostname + basePath+":" + sslPort;
	}
        if ((port == -1) || (port == 80)) {
            return "http://" + hostname + basePath;
        }

        return "http://" + hostname + ":" + port + basePath;
    }

    public String getLabel() {
        return getLabel(false);
    }

    public String getLabel(boolean includeUrl) {
        if ((title != null) && (title.length() > 0)) {
            return title + (includeUrl
                            ? HtmlUtils.space(3) + getUrl()
                            : "");
        }
        if (port != 80 && port>0) {
            return hostname + ":" + port;
        }

        return hostname;
    }

    /**
     *  Set the Hostname property.
     *
     *  @param value The new value for Hostname
     */
    public void setHostname(String value) {
        this.hostname = value;
    }

    /**
     *  Get the Hostname property.
     *
     *  @return The Hostname
     */
    public String getHostname() {
        return this.hostname;
    }

    /**
     *  Set the Port property.
     *
     *  @param value The new value for Port
     */
    public void setPort(int value) {
        this.port = value;
    }

    /**
     *  Get the Port property.
     *
     *  @return The Port
     */
    public int getPort() {
        return this.port;
    }

    /**
     *  Set the SslPort property.
     *
     *  @param value The new value for SslPort
     */
    public void setSslPort(int value) {
        this.sslPort = value;
    }

    /**
     *  Get the SslPort property.
     *
     *  @return The SslPort
     */
    public int getSslPort() {
        return this.sslPort;
    }

    /**
     *  Set the BasePath property.
     *
     *  @param value The new value for BasePath
     */
    public void setBasePath(String value) {
        this.basePath = value;
    }

    /**
     *  Get the BasePath property.
     *
     *  @return The BasePath
     */
    public String getBasePath() {
        return this.basePath;
    }

    /**
     *  Set the Title property.
     *
     *  @param value The new value for Title
     */
    public void setTitle(String value) {
        this.title = value;
    }

    /**
     *  Get the Title property.
     *
     *  @return The Title
     */
    public String getTitle() {
        return this.title;
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
     * Set the Email property.
     *
     * @param value The new value for Email
     */
    public void setEmail(String value) {
        email = value;
    }

    /**
     * Get the Email property.
     *
     * @return The Email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Set the IsRegistry property.
     *
     * @param value The new value for IsRegistry
     */
    public void setIsRegistry(boolean value) {
        this.isRegistry = value;
    }

    /**
     * Get the IsRegistry property.
     *
     * @return The IsRegistry
     */
    public boolean getIsRegistry() {
        return this.isRegistry;
    }

    /**
     * Set the Enabled property.
     *
     * @param value The new value for Enabled
     */
    public void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     * Get the Enabled property.
     *
     * @return The Enabled
     */
    public boolean getEnabled() {
        return enabled;
    }

    public String getArgBase() {
        return getId().replaceAll("[^a-zA-Z0-9]+", "");
    }
    /**
       Set the SearchRoot property.

       @param value The new value for SearchRoot
    **/
    public void setSearchRoot (String value) {
	searchRoot = value;
    }

    /**
       Get the SearchRoot property.

       @return The SearchRoot
    **/
    public String getSearchRoot () {
	if(searchRoot==null) return "";
	return searchRoot;
    }

    /**
       Set the Slug property.

       @param value The new value for Slug
    **/
    public void setSlug (String value) {
	slug = value;
    }

    /**
       Get the Slug property.

       @return The Slug
    **/
    public String getSlug () {
	return slug;
    }

    /**
       Set the Live property.

       @param value The new value for Live
    **/
    public void setLive (boolean value) {
	live = value;
    }

    /**
       Get the Live property.

       @return The Live
    **/
    public boolean getLive () {
	return live;
    }

}
