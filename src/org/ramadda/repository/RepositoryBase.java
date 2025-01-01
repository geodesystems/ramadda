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


    /** _more_ */
    public static final HtmlUtils HU = null;
    public static final XmlUtil XU = null;    


    /** _more_ */
    public final RequestUrl URL_HELP = new RequestUrl(this, "/docs");

    /** _more_ */
    public final RequestUrl URL_PING = new RequestUrl(this, "/ping");

    /** _more_ */
    public final RequestUrl URL_CLEARSTATE = new RequestUrl(this,
                                                 "/clearstate");


    /** _more_ */
    public final RequestUrl URL_SSLREDIRECT = new RequestUrl(this,
                                                  "/sslredirect");

    /** _more_ */
    public final RequestUrl URL_INFO = new RequestUrl(this, "/info");

    /** _more_ */
    public final RequestUrl URL_MESSAGE = new RequestUrl(this, "/message");

    /** _more_ */
    public final RequestUrl URL_DUMMY = new RequestUrl(this, "/dummy");

    /** _more_ */
    public final RequestUrl URL_INSTALL = new RequestUrl(this, "/install");





    /** _more_ */
    public final RequestUrl URL_COMMENTS_SHOW = new RequestUrl(this,
                                                    "/entry/comments/show");

    /** _more_ */
    public final RequestUrl URL_COMMENTS_ADD = new RequestUrl(this,
                                                   "/entry/comments/add");

    /** _more_ */
    public final RequestUrl URL_COMMENTS_EDIT = new RequestUrl(this,
                                                    "/entry/comments/edit");


    /** _more_ */
    public final RequestUrl URL_ENTRY_XMLCREATE = new RequestUrl(this,
                                                      "/entry/xmlcreate");


    /** _more_ */
    public final RequestUrl URL_ENTRY_IMPORT = new RequestUrl(this,
                                                   "/entry/import");

    /** _more_ */
    public final RequestUrl URL_ENTRY_ACTION = new RequestUrl(this,
                                                   "/entry/action");

    /** _more_ */
    public final RequestUrl URL_ENTRY_EXPORT = new RequestUrl(this,
                                                   "/entry/export");


    /** _more_ */
    public final RequestUrl URL_ENTRY_PUBLISH = new RequestUrl(this,
                                                    "/entry/publish");

    /** _more_ */
    public final RequestUrl URL_ENTRY_LINKS = new RequestUrl(this,
                                                  "/entry/links");


    /** _more_ */
    public final RequestUrl URL_ASSOCIATION_ADD = new RequestUrl(this,
                                                      "/association/add");

    /** _more_ */
    public final RequestUrl URL_ASSOCIATION_DELETE =
        new RequestUrl(this, "/association/delete");

    /** _more_ */
    public final RequestUrl URL_LIST_HOME = new RequestUrl(this,
                                                "/list/home");

    /** _more_ */
    public final RequestUrl URL_LIST_SHOW = new RequestUrl(this,
                                                "/list/show");

    /** _more_ */
    public final RequestUrl URL_GRAPH_VIEW = new RequestUrl(this,
                                                 "/graph/view");

    /** _more_ */
    public final RequestUrl URL_GRAPH_GET = new RequestUrl(this,
                                                "/graph/get");

    /** _more_ */
    public final RequestUrl URL_ENTRY_SHOW = new RequestUrl(this,
                                                 "/entry/show",
                                                 "View " + LABEL_ENTRY);

    /**  */
    public final RequestUrl URL_ENTRY_DATA = new RequestUrl(this,
                                                 "/entry/data", "Entry Data");

    /** _more_ */
    public final RequestUrl URL_ENTRY = new RequestUrl(this, "/entry",
                                            "View " + LABEL_ENTRY);

    /** _more_ */
    public final RequestUrl URL_ENTRY_COPY = new RequestUrl(this,
                                                 "/entry/copy");

    /** _more_ */
    public final RequestUrl URL_ENTRY_TYPECHANGE = new RequestUrl(this,
                                                       "/entry/typechange");


    /** _more_ */
    public final RequestUrl URL_ENTRY_DELETE = new RequestUrl(this,
                                                   "/entry/delete", "Delete");

    /** _more_ */
    public final RequestUrl URL_ENTRY_DELETELIST = new RequestUrl(this,
                                                       "/entry/deletelist");


    /** _more_ */
    public final RequestUrl URL_ACCESS_FORM = new RequestUrl(this,
                                                  "/access/form", "Permissions");


    /** _more_ */
    public final RequestUrl URL_ACCESS_CHANGE = new RequestUrl(this,
                                                    "/access/change");

    /** _more_ */
    public final RequestUrl URL_ENTRY_CHANGE = new RequestUrl(this,
                                                   "/entry/change");

    /** _more_ */
    public final RequestUrl URL_ENTRY_FORM = new RequestUrl(this,
                                                 "/entry/form",
                                                 "Edit Entry");

    /** _more_ */
    public final RequestUrl URL_ENTRY_EXTEDIT = new RequestUrl(this,
                                                    "/entry/extedit",
                                                    "Extra Edit");

    /** _more_ */
    public final RequestUrl URL_ENTRY_ACTIVITY = new RequestUrl(this,
                                                     "/entry/activity",
                                                     "Entry Activity");

    /** _more_ */
    public final RequestUrl URL_ENTRY_ACCESS = new RequestUrl(this,
                                                   "/entry/access",
                                                   "Edit " + LABEL_ENTRY);


    /** _more_ */
    public final RequestUrl URL_ENTRY_NEW = new RequestUrl(this,
                                                "/entry/new",
                                                "New " + LABEL_ENTRY);

    /** _more_ */
    public final RequestUrl URL_ENTRY_UPLOAD = new RequestUrl(this,
                                                   "/entry/upload",
                                                   "Upload a file");


    /** _more_ */
    public final RequestUrl URL_ENTRY_GETENTRIES = new RequestUrl(this,
                                                       "/entry/getentries");

    /** _more_ */
    public final RequestUrl URL_ENTRY_GET = new RequestUrl(this,
                                                "/entry/get");


    /** _more_ */
    public final RequestUrl URL_USER_LOGIN = new RequestUrl(this,
                                                 "/user/login", true);


    /** _more_ */
    public final RequestUrl URL_USER_REGISTER = new RequestUrl(this,
                                                    "/user/register", true);


    /** _more_ */
    public final RequestUrl URL_USER_FAVORITE = new RequestUrl(this,
                                                    "/user/favorite");

    /** _more_ */
    public final RequestUrl URL_USER_ACTIVITY = new RequestUrl(this,
                                                    "/user/activity");

    /** _more_ */
    public final RequestUrl URL_USER_RESETPASSWORD =
        new RequestUrl(this, "/user/resetpassword");

    /** _more_ */
    public final RequestUrl URL_USER_FINDUSERID = new RequestUrl(this,
                                                      "/user/finduserid");


    /** _more_ */
    public final RequestUrl URL_USER_LOGOUT = new RequestUrl(this,
                                                  "/user/logout");


    /** _more_ */
    public final RequestUrl URL_USER_HOME = new RequestUrl(this,
                                                "/user/home", "Favorites");

    /** _more_ */
    public final RequestUrl URL_USER_PROFILE = new RequestUrl(this,
                                                   "/user/profile",
                                                   "User Profile");

    /** _more_ */
    public final RequestUrl URL_USER_SETTINGS = new RequestUrl(this,
							       "/user/settings", "Settings",
							       "/user/changesettings");

    /** _more_ */
    public final RequestUrl URL_USER_PASSWORD = new RequestUrl(this,
							       "/user/password", "Password",
							       "/user/changepassword");    

    /** _more_ */
    public final RequestUrl URL_USER_CHANGE_SETTINGS = new RequestUrl(this,
								      "/user/changesettings");

    /** _more_ */
    public final RequestUrl URL_USER_CHANGE_PASSWORD = new RequestUrl(this,
								      "/user/changepassword");
    

    /** _more_ */
    public final RequestUrl URL_USER_CART = new RequestUrl(this,
                                                "/user/cart", "Data Cart");

    /** _more_ */
    public final RequestUrl URL_USER_LIST = new RequestUrl(this,
							   "/user/list", "Users","/user/edit","/user/new/form");

    /** _more_ */
    public final RequestUrl URL_USER_EDIT = new RequestUrl(this,
                                                "/user/edit", "Users");

    /** _more_ */
    public static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone("UTC");

    /** _more_ */
    public static final TimeZone TIMEZONE_GMT = TimeZone.getTimeZone("GMT");



    /** _more_ */
    public static final GregorianCalendar calendar =
        new GregorianCalendar(TIMEZONE_UTC);


    /** _more_ */
    private String urlBase = "/repository";

    /**  */
    protected boolean alwaysHttps = false;


    /** _more_ */
    private boolean isMinified;

    /** _more_ */
    private String hostname = "";

    /** _more_ */
    private String ipAddress = "";

    /** _more_ */
    private int httpPort = 80;

    /** _more_ */
    private int httpsPort = -1;

    /** _more_ */
    private boolean clientMode = false;

    /**
     * _more_
     */
    public RepositoryBase() {}

    /**
     * _more_
     *
     * @param port _more_
     *
     * @throws Exception _more_
     */
    public RepositoryBase(int port) throws Exception {
        this.httpPort = port;
    }

    /**
     *     _more_
     *
     *     @return _more_
     */
    public String getGUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected long currentTime() {
        return new Date().getTime();
    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public String absoluteUrl(String url) {
        int port = getPort();
        if ((port == 80) || (port == 0)) {
            return getHttpProtocol() + "://" + getHostname() + url;
        } else {
            return getHttpProtocol() + "://" + getHostname() + ":" + port
                   + url;
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param requestUrl _more_
     */
    public void initRequestUrl(RequestUrl requestUrl) {}


    /**
     * _more_
     *
     * @return _more_
     */
    public int getHttpsPort() {
        return httpsPort;
    }


    /**
     * _more_
     *
     * @param port _more_
     */
    public void setHttpsPort(int port) {
        httpsPort = port;
    }


    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param ip _more_
     */
    public void setIpAddress(String ip) {
        ipAddress = ip;
    }

    /**
     * _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    public String progress(String h) {
        return getMessage(h, Constants.ICON_PROGRESS, false);
    }


    /**
     * _more_
     *
     * @param h _more_
     * @param icon _more_
     * @param showClose _more_
     *
     * @return _more_
     */
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



    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     */
    public String getFileUrl(String f) {
        return Utils.concatString(urlBase, f);
    }

    public String getUrl(String f) {
        return Utils.concatString(urlBase, f);
    }


    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     */
    public String getHtdocsUrl(String f) {
        return getFileUrl(RepositoryUtil.getHtdocsVersionSlash() + f);
    }

    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     */
    public String getIconUrl(String f) {
        if (f == null) {
            return null;
        }

        return urlBase + f;
    }


    /**
     * _more_
     *
     * @param url _more_
     * @param args _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {}

}
