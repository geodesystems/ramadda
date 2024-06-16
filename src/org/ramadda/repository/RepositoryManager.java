/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;


import org.ramadda.repository.admin.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.database.DatabaseManager;
import org.ramadda.repository.harvester.HarvesterManager;
import org.ramadda.repository.map.MapManager;
import org.ramadda.repository.metadata.MetadataManager;
import org.ramadda.repository.output.WikiManager;
import org.ramadda.repository.search.SearchManager;

import org.ramadda.util.JsonUtil;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.HtmlUtilsConstants;
import org.ramadda.util.IO;
import org.ramadda.util.NamedValue;
import org.ramadda.util.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

/**
 *
 * @author RAMADDA Development Team
 */
@SuppressWarnings("unchecked")
public class RepositoryManager implements RepositorySource, Constants, RequestHandler {

    public static final String HELP_ROOT =    "https://ramadda.org/repository";

    protected Repository repository;


    public static final HtmlUtils HU = null;

    public static final JsonUtil JU = null;    

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
     *
     * @param args _more_
     * @param arg _more_
     * @param dflt _more_
      * @return _more_
     */
    public static boolean getArg(Hashtable args, String arg, boolean dflt) {
        return Utils.getProperty(args, arg, dflt);
    }

    /**
     *
     * @param args _more_
     * @param arg _more_
     * @param dflt _more_
      * @return _more_
     */
    public static String getArg(Hashtable args, String arg, String dflt) {
        return Utils.getProperty(args, arg, dflt);
    }

    /**
     *
     * @param args _more_
      * @return _more_
     */
    public static Hashtable makeArgs(Object... args) {
        return Utils.makeMap(args);
    }

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


    public void linkJS(Request request, StringBuilder sb, String js) {
        if (request.getExtraProperty(js) == null) {
            request.putExtraProperty(js, "true");
	    HU.importJS(sb, js);
	}
    }


    public void linkCSS(Request request, StringBuilder sb, String css) {
        if (request.getExtraProperty(css) == null) {
            request.putExtraProperty(css, "true");
	    sb.append(HU.cssLink(css));
	}
    }
    


    /**
     *
     * @param snippet _more_
     *
     * @return _more_
     */
    public String makeSnippet(String snippet) {
        return makeSnippet(snippet, false);
    }

    /**
     *
     * @param snippet _more_
     * @param stripTags _more_
     *
     * @return _more_
     */
    public String makeSnippet(String snippet, boolean stripTags) {
        if (stripTags) {
            snippet = Utils.stripTags(snippet);
        }

        return "<snippet>" + snippet + "</snippet>";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public RepositoryBase getRepositoryBase() {
        return repository;
    }


    public String formPropValue(Request request, String prop,String dflt) {
	return request.getString(prop,getRepository().getProperty(prop,dflt));
    }

    public boolean formPropValue(Request request, String prop, boolean dflt) {
	return request.get(prop,getRepository().getProperty(prop,dflt));
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
     *
     * @param sb _more_
     * @param request _more_
     * @param label _more_
     * @param contents _more_
     *
     * @throws Exception _more_
     */
    public void formEntry(Appendable sb, Request request, String label,
                          String contents)
            throws Exception {
        if (request.isMobile()) {
            sb.append("<tr><td><div class=\"formlabel\">");
            sb.append(label);
            sb.append("</div>");
            sb.append(contents);
            sb.append("</td></tr>");
        } else {
            HtmlUtils.formEntry(sb, label, contents);
        }
    }



    /**
     *
     * @param request _more_
     * @param label _more_
     *
     * @return _more_
     */
    public String formEntry(Request request, String label) {
        return "<tr><td colspan=2 class=ramadda-form-header><div style='font-weight:bold;margin-left:10px;'>"
               + label + "</b></td></tr>";
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
            return "<tr valign=top><td align=right  class=formlabel>" + label
                   + "</td><td>" + contents + "</td></tr>";
            //      return "<tr valign=top><td align=right><div class=\"formlabel\">" + label
            //          + "</div></td><td>" + contents + "</td></tr>";
            //            return HtmlUtils.formEntryTop(label, contents);
        }
    }

    /**
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public NamedValue arg(String name, Object value) {
        return new NamedValue(name, value);
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


    public boolean stringDefined(Object s) {
	return Utils.stringDefined(s);
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


    /*
      If just one arg then return the PageHandler.getCdnPath
      If 2 args then the first arg is the full /src/org/ramadda/... path and the
      second arg is the short path, e.g., /media/annotation.js
     */
    public String getHtdocsPath(String ...path) {
	return getPageHandler().getCdnPath(path[0],path.length>1?path[1]:path[0]);
	//	return getPageHandler().makeHtdocsUrl(path);
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
        return PageHandler.msg(msg);
    }

    public static String noMsg(String msg) {
        return PageHandler.noMsg(msg);
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
                                           getEntryUtil().getTimezone(request,entry));
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

    public Request getAdminRequest() {
	return getRepository().getAdminRequest();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public MailManager getMailManager() {
        return repository.getMailManager();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public UserManager getUserManager() {
        return repository.getUserManager();
    }

    public AuthManager getAuthManager() {
        return repository.getAuthManager();
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
     *
     * @return _more_
     */
    public EntryUtil getEntryUtil() {
        return repository.getEntryUtil();
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

    public LLMManager getLLMManager() {
        return repository.getLLMManager();
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
    public void logException(String message, Throwable exc) {
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
        String onSubmit = " onsubmit=\"return Utils.submitEntryForm('#" + id
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
        String onSubmit = " onclick=\"return Utils.submitEntryForm('#" + id
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
        return getEntryDisplayName(entry, null);
    }

    /**
     *
     * @param entry _more_
     * @param template _more_
     *
     * @return _more_
     */
    public String getEntryDisplayName(Entry entry, String template) {
        String name = entry.getTypeHandler().getEntryName(entry);
        if ( !Utils.stringDefined(name)) {
            name = entry.getBaseLabel();
            if ( !Utils.stringDefined(name)) {
                name = entry.getTypeHandler().getLabel() + ": "
                       + new Date(entry.getStartDate());
            }
        }

        if (template != null) {
            name = template.replace("${name}", name);
            String date = getDateHandler().formatYYYYMMDD(
                              new Date(entry.getStartDate()));
            name = name.replace("${date}", date);
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

    public boolean isTextFile(Entry entry, String file) {
	if(entry.getTypeHandler().isType("type_file_text")) return true;
	return isTextFile(file);
    }


    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     */
    public boolean isTextFile(String file) {
        String suffix = IO.getFileExtension(file);
        suffix = suffix.replace(".", "");
        if (textSuffixes == null) {
            HashSet<String> tmp = new HashSet<String>();
            tmp.addAll(
                Utils.split(
                    getRepository().getProperty("ramadda.suffixes.text", ""),
                    ",", true, true));
            textSuffixes = tmp;
        }

        return textSuffixes.contains(suffix.toLowerCase());
    }

    /**
     * _more_
     *
     * @param msg _more_
     *
     * @return _more_
     */
    public String messageNote(String msg) {
        return getPageHandler().showDialogNote(msg);
    }

    /**
     * _more_
     *
     * @param msg _more_
     *
     * @return _more_
     */
    public String messageBlank(String msg) {
        return getPageHandler().showDialogBlank(msg);
    }

    /**
     * _more_
     *
     * @param msg _more_
     * @param buttons _more_
     *
     * @return _more_
     */
    public String messageQuestion(String msg, String buttons) {
        return getPageHandler().showDialogQuestion(msg, buttons);
    }

    /**
     * _more_
     *
     * @param msg _more_
     *
     * @return _more_
     */
    public String messageWarning(String msg) {
        return getPageHandler().showDialogWarning(msg);
    }

    /**
     * _more_
     *
     * @param msg _more_
     *
     * @return _more_
     */
    public String messageError(String msg) {
        return getPageHandler().showDialogError(msg);
    }




}
