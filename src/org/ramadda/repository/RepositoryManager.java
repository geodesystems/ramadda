/*
* Copyright (c) 2008-2019 Geode Systems LLC
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


import org.ramadda.repository.admin.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.database.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.JsonOutputHandler;

import org.ramadda.repository.output.WikiManager;
import org.ramadda.repository.search.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;


import org.ramadda.util.sql.SqlUtil;



import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;


import java.io.*;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.*;



import java.net.*;



import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



/**
 *
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class RepositoryManager implements RepositorySource, Constants,
                                          RequestHandler {



    /** _more_ */
    public static final String HELP_ROOT =
        "http://geodesystems.com/repository";


    /** _more_ */
    protected Repository repository;





    /**
     * _more_
     *
     * @param repository _more_
     */
    public RepositoryManager(Repository repository) {
        this.repository = repository;
        if (this.repository != null) {
            this.repository.addRepositoryManager(this);
        }
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void shutdown() throws Exception {}



    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addAdminSettings(Request request, StringBuffer sb)
            throws Exception {}

    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void applyAdminSettings(Request request) throws Exception {}

    /**
     * _more_
     *
     * @return _more_
     */
    public Repository getRepository() {
        return repository;
    }

    /**
     * _more_
     *
     * @param repository _more_
     */
    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public RepositoryBase getRepositoryBase() {
        return repository;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param label _more_
     * @param contents _more_
     *
     * @return _more_
     */
    public String formEntry(Request request, String label, String contents) {
        if (request.isMobile()) {
            return "<tr><td><div class=\"formlabel\">" + label + "</div>"
                   + contents + "</td></tr>";
        } else {
            //            return "<tr><td><div class=\"formlabel\">" + label
            //                   + "</div></td><td>" + contents + "</td></tr>";
            return HtmlUtils.formEntry(label, contents);
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param label _more_
     * @param contents _more_
     *
     * @return _more_
     */
    public static String formEntryTop(Request request, String label,
                                      String contents) {
        if (request.isMobile()) {
            return "<tr><td><div class=\"formlabel\">" + label + "</div>"
                   + contents + "</td></tr>";
        } else {
            //            return "<tr valign=top><td><div class=\"formlabel\">" + label
            //                   + "</div></td><td>" + contents + "</td></tr>";
            return HtmlUtils.formEntryTop(label, contents);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getActive() {
        if ((repository == null) || !repository.getActive()) {
            return false;
        }

        return true;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param result _more_
     *
     * @return _more_
     */
    public Result addHeaderToAncillaryPage(Request request, Result result) {
        return result;
        //        return getEntryManager().addEntryHeader(request, null, result);

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param message _more_
     */
    public void fatalError(Request request, String message) {
        throw new IllegalArgumentException(message);
    }



    /**
     * _more_
     *
     * @param bytes _more_
     * @param decorate _more_
     *
     * @return _more_
     */
    public static String formatFileLength(double bytes, boolean decorate) {
        String s = formatFileLength(bytes);
        if (decorate && (s.length() > 0)) {
            return " (" + s + ")";
        }

        return s;
    }

    /**
     * _more_
     *
     * @param bytes _more_
     *
     * @return _more_
     */
    public static String formatFileLength(double bytes) {
        if (bytes < 0) {
            return "";
        }

        if (bytes < 5000) {
            return ((int) bytes) + " bytes";
        }
        if (bytes < 1000000) {
            bytes = ((int) ((bytes * 100) / 1000.0)) / 100.0;

            return ((int) bytes) + " KB";
        }
        bytes = ((int) ((bytes * 100) / 1000000.0)) / 100.0;

        return bytes + " MB";
    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public String getFileUrl(String url) {
        return getRepository().getFileUrl(url);
    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public String getHtdocsUrl(String url) {
        return getRepository().getHtdocsUrl(url);
    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public String getIconUrl(String url) {
        return getRepository().getIconUrl(url);
    }




    /**
     * _more_
     *
     *
     * @param request _more_
     * @param sb _more_
     * @param label _more_
     * @param value _more_
     */
    public void addCriteria(Request request, Appendable sb, String label,
                            Object value) {
        try {
            String sv;
            if (value instanceof Date) {
                Date dttm = (Date) value;
                sv = formatDate(request, dttm);
            } else {
                sv = value.toString();
            }
            sv = sv.replace("<", "&lt;");
            sv = sv.replace(">", "&gt;");
            sb.append("<tr valign=\"top\"><td align=right>");
            sb.append(HtmlUtils.b(label));
            sb.append("</td><td>");
            sb.append(sv);
            sb.append("</td></tr>");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * _more_
     *
     * @param msg _more_
     *
     * @return _more_
     */
    public static String msg(String msg) {
        return Repository.msg(msg);
    }

    /**
     * _more_
     *
     * @param msg _more_
     *
     * @return _more_
     */
    public static String boldMsg(String msg) {
        return HtmlUtils.b(msg(msg));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param s _more_
     *
     * @return _more_
     */
    public String translateMsg(Request request, String s) {
        return getRepository().translate(request, msg(s));
    }


    /**
     * _more_
     *
     * @param msg _more_
     *
     * @return _more_
     */
    public static String msgLabel(String msg) {
        return Repository.msgLabel(msg);
    }

    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    public static String msgHeader(String h) {
        return Repository.msgHeader(h);
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String tableSubHeader(String s) {
        return HtmlUtils.row(HtmlUtils.colspan(subHeader(s), 2));
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String subHeader(String s) {
        return HtmlUtils.div(s, HtmlUtils.cssClass(CSS_CLASS_HEADING_2));
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String formHeader(String s) {
        return HtmlUtils.div(s, HtmlUtils.cssClass("formgroupheader"));
    }


    /**
     * _more_
     *
     * @param url _more_
     * @param label _more_
     *
     * @return _more_
     */
    public String subHeaderLink(String url, String label) {
        return HtmlUtils.href(url, label,
                              HtmlUtils.cssClass(CSS_CLASS_HEADING_2_LINK));
    }


    /**
     * _more_
     *
     * @param url _more_
     * @param label _more_
     * @param toggle _more_
     *
     * @return _more_
     */
    public String subHeaderLink(String url, String label, boolean toggle) {
        //        if(true) return "x";
        String img = HtmlUtils.img(getIconUrl(toggle
                ? ICON_MINUS
                : ICON_PLUS));
        label = img + HtmlUtils.space(1) + label;
        String html =
            HtmlUtils.href(url, label,
                           HtmlUtils.cssClass(CSS_CLASS_HEADING_2_LINK));

        return html;
        //return "<table border=1><tr valign=bottom><td>" + html +"</table>";
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param d _more_
     *
     * @return _more_
     */
    public String formatDate(Request request, Date d) {
        return getDateHandler().formatDate(request, d);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param d _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public String formatDate(Request request, Date d, Entry entry) {
        return getDateHandler().formatDate(request, d,
                                           getEntryUtil().getTimezone(entry));
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public DatabaseManager getDatabaseManager() {
        return repository.getDatabaseManager();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public RegistryManager getRegistryManager() {
        return repository.getRegistryManager();
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getPropertyFromTree(String name, String dflt) {
        return repository.getPropertyFromTree(name, dflt);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private String xxxgetProperty(String name, String dflt) {
        return repository.getProperty(name, dflt);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private boolean xxxgetProperty(String name, boolean dflt) {
        return repository.getProperty(name, dflt);
    }


    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    public String header(String h) {
        return RepositoryUtil.header(h);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Admin getAdmin() {
        return repository.getAdmin();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public UserManager getUserManager() {
        return repository.getUserManager();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public SessionManager getSessionManager() {
        return repository.getSessionManager();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public LogManager getLogManager() {
        return repository.getLogManager();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public ActionManager getActionManager() {
        return repository.getActionManager();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public AccessManager getAccessManager() {
        return repository.getAccessManager();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public EntryManager getEntryManager() {
        return repository.getEntryManager();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public EntryUtil getEntryUtil() {
        return getEntryManager().getEntryUtil();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public PageHandler getPageHandler() {
        return repository.getPageHandler();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public DateHandler getDateHandler() {
        return repository.getDateHandler();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public SearchManager getSearchManager() {
        return repository.getSearchManager();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public AssociationManager getAssociationManager() {
        return repository.getAssociationManager();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public MetadataManager getMetadataManager() {
        return repository.getMetadataManager();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public WikiManager getWikiManager() {
        return repository.getWikiManager();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public HarvesterManager getHarvesterManager() {
        return repository.getHarvesterManager();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public StorageManager getStorageManager() {
        return repository.getStorageManager();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public PluginManager getPluginManager() {
        return repository.getPluginManager();
    }

    /**
     * _more_
     *
     * @return _more_
     */

    public MapManager getMapManager() {
        return repository.getMapManager();
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param message _more_
     */
    protected void log(Request request, String message) {
        getRepository().getLogManager().log(request, message);

    }



    /**
     * _more_
     *
     * @param message _more_
     * @param exc _more_
     */
    public void logException(String message, Exception exc) {
        getRepository().getLogManager().logError(message, exc);
    }


    /**
     * _more_
     *
     * @param message _more_
     * @param exc _more_
     */
    public void logError(String message, Throwable exc) {
        getRepository().getLogManager().logError(message, exc);
    }


    /**
     * _more_
     *
     * @param message _more_
     */
    public void logInfo(String message) {
        System.err.println(message);
        getRepository().getLogManager().logInfo(message);
    }

    /**
     * _more_
     */
    public void adminSettingsChanged() {}


    /** _more_ */
    private static int dialogCnt = 0;

    /**
     * _more_
     *
     * @param sb _more_
     * @param message _more_
     *
     * @return _more_
     */
    public String makeFormSubmitDialog(Appendable sb, String message) {
        String id = "dialog-message" + (dialogCnt++);
        String onSubmit = " onsubmit=\"return submitEntryForm('#" + id
                          + "');\" ";
        String loadingImage =
            HtmlUtils.img(getRepository().getIconUrl(ICON_PROGRESS));
        Utils.append(sb,
                     "<div style=\"display:none;\" id=\"" + id + "\">"
                     + loadingImage + " " + message + "</div>");

        return onSubmit;

    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param message _more_
     *
     * @return _more_
     */
    public String makeButtonSubmitDialog(Appendable sb, String message) {
        String id = HtmlUtils.getUniqueId("dialog-message");
        String onSubmit = " onclick=\"return submitEntryForm('#" + id
                          + "');\" ";
        String loadingImage =
            HtmlUtils.img(getRepository().getIconUrl(ICON_PROGRESS));
        Utils.append(sb,
                     "<div style=\"display:none;\" id=\"" + id + "\">"
                     + loadingImage + " " + message + "</div>");

        return onSubmit;

    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public String getEntryDisplayName(Entry entry) {
        String name = entry.getTypeHandler().getEntryName(entry);
        if ( !Utils.stringDefined(name)) {
            name = entry.getBaseLabel();
            if ( !Utils.stringDefined(name)) {
                name = entry.getTypeHandler().getLabel() + ": "
                       + new Date(entry.getStartDate());
            }
        }

        return name;
    }

    /**
     * _more_
     */
    public void clearCache() {}

    /**
     * _more_
     */
    public void initAttributes() {}

    /**
     * A method to find out if a radio button should be selected based on the request arguments
     * @param request     the request
     * @param requestArg  the request argument
     * @param buttonValue the value to check against
     * @param dflt        default if requestArg is not defined in request
     * @return  true if requestArg is present and equals buttonValue, else dflt
     */
    public static boolean getShouldButtonBeSelected(Request request,
            String requestArg, String buttonValue, boolean dflt) {
        if (request.defined(requestArg)) {
            return request.getString(requestArg).equals(buttonValue);
        }

        return dflt;
    }



    /** _more_ */
    private HashSet<String> textSuffixes;

    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     */
    public boolean isTextFile(String file) {
        String suffix = IOUtil.getFileExtension(file);
        suffix = suffix.replace(".", "");
        if (textSuffixes == null) {
            HashSet<String> tmp = new HashSet<String>();
            tmp.addAll(
                StringUtil.split(
                    getRepository().getProperty("ramadda.suffixes.text", ""),
                    ",", true, true));
            textSuffixes = tmp;
        }

        return textSuffixes.contains(suffix.toLowerCase());
    }


}
