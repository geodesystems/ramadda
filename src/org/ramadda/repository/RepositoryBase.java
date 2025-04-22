/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import ucar.unidata.xml.XmlUtil;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;

/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class RepositoryBase implements Constants, RepositorySource {

    public static final HtmlUtils HU = null;
    public static final XmlUtil XU = null;    

    public final RequestUrl URL_HELP = new RequestUrl(this, "/docs");

    public final RequestUrl URL_PING = new RequestUrl(this, "/ping");

    public final RequestUrl URL_CLEARSTATE = new RequestUrl(this,
                                                 "/clearstate");

    public final RequestUrl URL_SSLREDIRECT = new RequestUrl(this,
                                                  "/sslredirect");

    public final RequestUrl URL_INFO = new RequestUrl(this, "/info");

    public final RequestUrl URL_MESSAGE = new RequestUrl(this, "/message");

    public final RequestUrl URL_DUMMY = new RequestUrl(this, "/dummy");

    public final RequestUrl URL_INSTALL = new RequestUrl(this, "/install");

    public final RequestUrl URL_COMMENTS_SHOW = new RequestUrl(this,
                                                    "/entry/comments/show");

    public final RequestUrl URL_COMMENTS_ADD = new RequestUrl(this,
                                                   "/entry/comments/add");

    public final RequestUrl URL_COMMENTS_EDIT = new RequestUrl(this,
                                                    "/entry/comments/edit");

    public final RequestUrl URL_ENTRY_XMLCREATE = new RequestUrl(this,
                                                      "/entry/xmlcreate");

    public final RequestUrl URL_ENTRY_IMPORT = new RequestUrl(this,
                                                   "/entry/import");

    public final RequestUrl URL_ENTRY_ACTION = new RequestUrl(this,
                                                   "/entry/action");

    public final RequestUrl URL_ENTRY_EXPORT = new RequestUrl(this,
                                                   "/entry/export");

    public final RequestUrl URL_ENTRY_PUBLISH = new RequestUrl(this,
                                                    "/entry/publish");

    public final RequestUrl URL_ENTRY_LINKS = new RequestUrl(this,
                                                  "/entry/links");

    public final RequestUrl URL_ASSOCIATION_ADD = new RequestUrl(this,
                                                      "/association/add");

    public final RequestUrl URL_ASSOCIATION_DELETE =
        new RequestUrl(this, "/association/delete");

    public final RequestUrl URL_LIST_HOME = new RequestUrl(this,
                                                "/list/home");

    public final RequestUrl URL_LIST_SHOW = new RequestUrl(this,
                                                "/list/show");

    public final RequestUrl URL_GRAPH_VIEW = new RequestUrl(this,
                                                 "/graph/view");

    public final RequestUrl URL_GRAPH_GET = new RequestUrl(this,
                                                "/graph/get");

    public final RequestUrl URL_ENTRY_SHOW = new RequestUrl(this,
                                                 "/entry/show",
                                                 "View " + LABEL_ENTRY);

    /**  */
    public final RequestUrl URL_ENTRY_DATA = new RequestUrl(this,
                                                 "/entry/data", "Entry Data");

    public final RequestUrl URL_ENTRY = new RequestUrl(this, "/entry",
                                            "View " + LABEL_ENTRY);

    public final RequestUrl URL_ENTRY_COPY = new RequestUrl(this,
                                                 "/entry/copy");

    public final RequestUrl URL_ENTRY_TYPECHANGE = new RequestUrl(this,
                                                       "/entry/typechange");

    public final RequestUrl URL_ENTRY_DELETE = new RequestUrl(this,
                                                   "/entry/delete", "Delete");

    public final RequestUrl URL_ENTRY_DELETELIST = new RequestUrl(this,
                                                       "/entry/deletelist");

    public final RequestUrl URL_ACCESS_FORM = new RequestUrl(this,
                                                  "/access/form", "Permissions");

    public final RequestUrl URL_ACCESS_CHANGE = new RequestUrl(this,
                                                    "/access/change");

    public final RequestUrl URL_ENTRY_CHANGE = new RequestUrl(this,
                                                   "/entry/change");

    public final RequestUrl URL_ENTRY_FORM = new RequestUrl(this,
                                                 "/entry/form",
                                                 "Edit Entry");

    public final RequestUrl URL_ENTRY_EXTEDIT = new RequestUrl(this,
                                                    "/entry/extedit",
                                                    "Extra Edit");

    public final RequestUrl URL_ENTRY_ACTIVITY = new RequestUrl(this,
                                                     "/entry/activity",
                                                     "Entry Activity");

    public final RequestUrl URL_ENTRY_ACCESS = new RequestUrl(this,
                                                   "/entry/access",
                                                   "Edit " + LABEL_ENTRY);

    public final RequestUrl URL_ENTRY_NEW = new RequestUrl(this,
                                                "/entry/new",
                                                "New " + LABEL_ENTRY);

    public final RequestUrl URL_ENTRY_UPLOAD = new RequestUrl(this,
                                                   "/entry/upload",
                                                   "Upload a file");

    public final RequestUrl URL_ENTRY_GETENTRIES = new RequestUrl(this,
                                                       "/entry/getentries");

    public final RequestUrl URL_ENTRY_GET = new RequestUrl(this,
                                                "/entry/get");

    public final RequestUrl URL_USER_LOGIN = new RequestUrl(this,
                                                 "/user/login", true);

    public final RequestUrl URL_USER_REGISTER = new RequestUrl(this,
                                                    "/user/register", true);

    public final RequestUrl URL_USER_FAVORITE = new RequestUrl(this,
                                                    "/user/favorite");

    public final RequestUrl URL_USER_ACTIVITY = new RequestUrl(this,
                                                    "/user/activity");

    public final RequestUrl URL_USER_RESETPASSWORD =
        new RequestUrl(this, "/user/resetpassword");

    public final RequestUrl URL_USER_FINDUSERID = new RequestUrl(this,
                                                      "/user/finduserid");

    public final RequestUrl URL_USER_LOGOUT = new RequestUrl(this,
                                                  "/user/logout");

    public final RequestUrl URL_USER_HOME = new RequestUrl(this,
                                                "/user/home", "Favorites");

    public final RequestUrl URL_USER_PROFILE = new RequestUrl(this,
                                                   "/user/profile",
                                                   "User Profile");

    public final RequestUrl URL_USER_SETTINGS = new RequestUrl(this,
							       "/user/settings", "Settings",
							       "/user/changesettings");

    public final RequestUrl URL_USER_PASSWORD = new RequestUrl(this,
							       "/user/password", "Password",
							       "/user/changepassword");    

    public final RequestUrl URL_USER_CHANGE_SETTINGS = new RequestUrl(this,
								      "/user/changesettings");

    public final RequestUrl URL_USER_CHANGE_PASSWORD = new RequestUrl(this,
								      "/user/changepassword");

    public final RequestUrl URL_USER_CART = new RequestUrl(this,
                                                "/user/cart", "Data Cart");

    public final RequestUrl URL_USER_LIST = new RequestUrl(this,
							   "/user/list", "Users","/user/edit","/user/new/form");

    public final RequestUrl URL_USER_EDIT = new RequestUrl(this,
                                                "/user/edit", "Users");

    public static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone("UTC");

    public static final TimeZone TIMEZONE_GMT = TimeZone.getTimeZone("GMT");

    public static final GregorianCalendar calendar =
        new GregorianCalendar(TIMEZONE_UTC);

    private String urlBase = "/repository";

    /**  */
    protected boolean alwaysHttps = false;

    private boolean isMinified;

    private String hostname = "";

    private String ipAddress = "";

    private int httpPort = 80;

    private int httpsPort = -1;

    private boolean clientMode = false;

    public RepositoryBase() {}

    public RepositoryBase(int port) throws Exception {
        this.httpPort = port;
    }

    public String getGUID() {
        return UUID.randomUUID().toString();
    }

    protected long currentTime() {
        return new Date().getTime();
    }

    public String absoluteUrl(String url) {
        int port = getPort();
        if ((port == 80) || (port == 0)) {
            return getHttpProtocol() + "://" + getHostname() + url;
        } else {
            return getHttpProtocol() + "://" + getHostname() + ":" + port
                   + url;
        }
    }

    public String getHttpProtocol() {
        if (getAlwaysHttps()) {
            return "https";
        }

        return "http";
    }

    /**
      * @return _more_
     */
    public boolean getAlwaysHttps() {
        return alwaysHttps;
    }

    public void initRequestUrl(RequestUrl requestUrl) {}

    public int getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(int port) {
        httpsPort = port;
    }

    public String getHttpsUrl(String url) {
        String hostname = getHostname();
        int    port     = getHttpsPort();
        if (port < 0) {
            return getHttpProtocol() + "://" + hostname + ":" + getPort()
                   + url;
        }
        //Don't include the default https port in the url 
        if ((port == 0) || (port == 443)) {
            return "https://" + hostname + url;
        } else {
            return "https://" + hostname + ":" + port + url;
        }
    }

    public String getProperty(String name, String dflt) {
        return dflt;
    }

    /**
     * Note: this is overwritten in the Repository class
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean getProperty(String name, boolean dflt) {
        return dflt;
    }

    public RepositoryBase getRepositoryBase() {
        return this;
    }

    /**
     * Set the Hostname property.
     *
     * @param value The new value for Hostname
     */
    public void setHostname(String value) {
        hostname = value;
    }

    public void setIpAddress(String ip) {
        ipAddress = ip;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Get the Hostname property.
     *
     * @return The Hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Set the Port property.
     *
     * @param value The new value for Port
     */
    public void setPort(int value) {
        httpPort = value;
    }

    /**
     * Get the Port property.
     *
     * @return The Port
     */
    public int getPort() {
        return httpPort;
    }

    public String progress(String h) {
        return getMessage(h, Constants.ICON_PROGRESS, false);
    }

    public String getMessage(String h, String icon, boolean showClose) {
        String html =
            HtmlUtils.jsLink(HtmlUtils.onMouseClick("hide('messageblock')"),
                             getIconImage(Constants.ICON_CLOSE));
        if ( !showClose) {
            html = "&nbsp;";
        }
        h = "<div class=\"innernote\"><table cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr><td valign=\"top\">"
            + HtmlUtils.img(getIconUrl(icon)) + HtmlUtils.space(2)
            + "</td><td valign=\"bottom\"><span class=\"notetext\">" + h
            + "</span></td></tr></table></div>";

        return "\n<table border=\"0\" id=\"messageblock\"><tr><td><div class=\"note\"><table><tr valign=top><td>"
               + h + "</td><td>" + html + "</td></tr></table>"
               + "</div></td></tr></table>\n";
    }

    public String getFileUrl(String f) {
        return Utils.concatString(urlBase, f);
    }

    public String getUrl(String f) {
        return Utils.concatString(urlBase, f);
    }

    public String getHtdocsUrl(String f) {
        return getFileUrl(RepositoryUtil.getHtdocsVersionSlash() + f);
    }

    public String getIconUrl(String f) {
        if (f == null) {
            return null;
        }

        return urlBase + f;
    }

    public String getIconImage(String url, String... args) {
        if (HU.isFontAwesome(url)) {
            return HU.faIcon(url, args);
        } else {
            return HU.image(getIconUrl(url), args);
        }
    }

    /**
     * Set the UrlBase property.
     *
     * @param value The new value for UrlBase
     */
    public void setUrlBase(String value) {
        urlBase = value;
    }

    /**
     * Get the UrlBase property.
     *
     * @return The UrlBase
     */
    public String getUrlBase() {
        return urlBase;
    }

    /**
     * Set the IsMinified property.
     *
     * @param value The new value for IsMinified
     */
    public void setIsMinified(boolean value) {
        isMinified = value;
    }

    /**
     * Get the IsMinified property.
     *
     * @return The IsMinified
     */
    public boolean getIsMinified() {
        return isMinified;
    }

    public static void main(String[] args) throws Exception {}

}
