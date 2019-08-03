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


import org.ramadda.repository.auth.Permission;
import org.ramadda.repository.auth.User;

import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.map.MapLayer;
import org.ramadda.repository.metadata.ContentMetadataHandler;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.output.CalendarOutputHandler;
import org.ramadda.repository.output.HtmlOutputHandler;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.output.PageStyle;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.util.CategoryBuffer;
import org.ramadda.util.HtmlTemplate;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.MapRegion;

import org.ramadda.util.Utils;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;


/**
 * The main class.
 *
 */
public class PageHandler extends RepositoryManager {


    /** _more_ */
    public static final String DEFAULT_TEMPLATE = "fixedmapheader";

    /** _more_ */
    public static final String REGISTER_MESSAGE =
        "<div class=\"ramadda-register-outer\"><div class=\"ramadda-register\">Powered by <a href=\"http://geodesystems.com\">Geode Systems RAMADDA</a></div></div>";



    /** _more_ */
    private List<MapLayer> mapLayers = null;


    /** _more_ */
    private List<MapRegion> mapRegions = new ArrayList<MapRegion>();



    /** _more_ */
    public static final String MSG_PREFIX = "<msg ";

    /** _more_ */
    public static final String MSG_SUFFIX = " msg>";


    /** html template macro */
    public static final String MACRO_LINKS = "links";

    /** html template macro */
    public static final String MACRO_LOGO_URL = "logo.url";

    /** html template macro */
    public static final String MACRO_LOGO_IMAGE = "logo.image";

    /** html template macro */
    public static final String MACRO_SEARCH_URL = "search.url";

    /** html template macro */
    public static final String MACRO_ENTRY_HEADER = "entry.header";

    /** html template macro */
    public static final String MACRO_HEADER = "header";

    /** html template macro */
    public static final String MACRO_ENTRY_FOOTER = "entry.footer";

    /** html template macro */
    public static final String MACRO_ENTRY_BREADCRUMBS = "entry.breadcrumbs";

    /** html template macro */
    public static final String MACRO_HEADER_IMAGE = "header.image";

    /** html template macro */
    public static final String MACRO_HEADER_TITLE = "header.title";

    /** html template macro */
    public static final String MACRO_USERLINK = "userlinks";


    /** html template macro */
    public static final String MACRO_FAVORITES = "favorites";


    /** html template macro */
    public static final String MACRO_REPOSITORY_NAME = "repository_name";

    /** _more_ */
    public static final String MACRO_REGISTER = "register";

    /** html template macro */
    public static final String MACRO_FOOTER = "footer";

    /** html template macro */
    public static final String MACRO_TITLE = "title";

    /** html template macro */
    public static final String MACRO_ROOT = "root";

    /** html template macro */
    public static final String MACRO_HEADFINAL = "headfinal";

    /** html template macro */
    public static final String MACRO_BOTTOM = "bottom";

    /** html template macro */
    public static final String MACRO_CONTENT = "content";


    /** _more_ */
    private List<HtmlTemplate> htmlTemplates;

    /** _more_ */
    private Hashtable<String, HtmlTemplate> templateMap;

    /** _more_ */
    private HtmlTemplate mobileTemplate;

    /** _more_ */
    private HtmlTemplate defaultTemplate;

    /** _more_ */
    private HtmlTemplate mapTemplate;


    /** _more_ */
    private Properties phraseMap;


    /** _more_ */
    private Hashtable<String, Properties> languageMap = new Hashtable<String,
                                                            Properties>();

    /** _more_ */
    private List<TwoFacedObject> languages = new ArrayList<TwoFacedObject>();






    /**
     * Set this to true to print to a file the missing messages and this also
     *   adds a "NA:" to the missing labels.
     */
    private static boolean debugMsg = false;

    /** _more_ */
    private static PrintWriter allMsgOutput;

    /** _more_ */
    private static PrintWriter missingMsgOutput;


    /** _more_ */
    private static HashSet<String> seenMsg = new HashSet<String>();


    /** _more_ */
    private String headerIcon;

    /** _more_ */
    private Hashtable<String, String> typeToWikiTemplate =
        new Hashtable<String, String>();

    /** _more_ */
    public static final String TEMPLATE_DEFAULT = "default";

    /** _more_ */
    public static final String TEMPLATE_CONTENT = "content";


    /** _more_ */
    private boolean showCreateDate;

    /** _more_ */
    private boolean showSearch;

    /** _more_ */
    private String shortDateFormat;

    /** _more_ */
    private String createdDisplayMode;

    /** _more_ */
    private String myLogoImage;

    /** _more_ */
    private String footer;

    /** _more_ */
    private boolean cacheTemplates;


    /** _more_ */
    TimeZone defaultTimeZone;


    /** _more_ */
    private Hashtable<String, SimpleDateFormat> dateFormats =
        new Hashtable<String, SimpleDateFormat>();


    /** _more_ */
    protected List<SimpleDateFormat> parseFormats;



    /**
     * _more_
     *
     * @param repository _more_
     */
    public PageHandler(Repository repository) {
        super(repository);
    }



    /**
     * _more_
     */
    @Override
    public void initAttributes() {
        super.initAttributes();
        //Clear out any loaded templates
        clearCache();
        showCreateDate =
            getRepository().getProperty(PROP_ENTRY_TABLE_SHOW_CREATEDATE,
                                        false);

        showSearch = getRepository().getProperty("ramadda.showsearch", true);
        createdDisplayMode =
            getRepository().getProperty(PROP_CREATED_DISPLAY_MODE,
                                        "none").trim();
        footer      = repository.getProperty(PROP_HTML_FOOTER, BLANK);
        myLogoImage = getRepository().getProperty(PROP_LOGO_IMAGE, null);
        cacheTemplates =
            getRepository().getProperty("ramadda.cachetemplates", true);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getHeaderIcon() {
        if (headerIcon == null) {
            headerIcon = getIconUrl(ICON_HEADER);
        }

        return headerIcon;
    }


    /**
     * _more_
     */
    public void adminSettingsChanged() {
        super.adminSettingsChanged();
        phraseMap = null;
    }

    /**
     * _more_
     *
     *
     * @param request The request
     * @param result _more_
     *
     * @throws Exception _more_
     */
    public void decorateResult(Request request, Result result)
            throws Exception {

        long t0 = System.currentTimeMillis();

        if ( !request.get(ARG_DECORATE, true)) {
            return;
        }
        Repository   repository   = getRepository();
        Entry        currentEntry = getSessionManager().getLastEntry(request);
        String       template     = null;
        HtmlTemplate htmlTemplate;
        if (request.isMobile() && !request.defined(ARG_TEMPLATE)) {
            htmlTemplate = getMobileTemplate();
        } else {
            htmlTemplate = getTemplate(request, currentEntry);
        }
        template = htmlTemplate.getTemplate();
        List<String> templateToks  = htmlTemplate.getToks();

        String       systemMessage =
            getRepository().getSystemMessage(request);


        String       jsContent     = getTemplateJavascriptContent();

        String entryHeader = (String) result.getProperty(PROP_ENTRY_HEADER,
                                 "");
        String entryFooter = (String) result.getProperty(PROP_ENTRY_FOOTER,
                                 "");
        String entryBreadcrumbs =
            (String) result.getProperty(PROP_ENTRY_BREADCRUMBS, "");
        String        header   = entryHeader;

        StringBuilder contents = new StringBuilder();

        if ((systemMessage != null) && (systemMessage.length() > 0)) {
            HtmlUtils.div(contents, systemMessage,
                          HtmlUtils.cssClass("ramadda-system-message"));
        }

        String registerMessage = "";
        if ( !getAdmin().isRegistered()
                && getAdmin().getInstallationComplete()) {
            if ( !getRepository().getProperty("ramadda.hidepoweredby",
                    false)) {
                if ( !htmlTemplate.hasMacro(MACRO_REGISTER)) {
                    contents.append(REGISTER_MESSAGE);
                } else {
                    registerMessage = REGISTER_MESSAGE;
                }
            }
        }

        Utils.append(contents, result.getStringContent(), jsContent);
        String content = contents.toString();

        String head    = (String) result.getProperty(PROP_HTML_HEAD);
        if (head == null) {
            head = (String) request.getExtraProperty(PROP_HTML_HEAD);
        }
        if (head == null) {
            head = "";
        }
        String logoImage = getLogoImage(result);
        String logoUrl   = (String) result.getProperty(PROP_LOGO_URL);
        if ( !Utils.stringDefined(logoUrl)) {
            logoUrl = getRepository().getProperty(PROP_LOGO_URL, "");
        }
        if ( !Utils.stringDefined(logoUrl)) {
            logoUrl = getRepository().getUrlBase();
        }
        logoUrl = applyBaseMacros(logoUrl);


        String pageTitle = (String) result.getProperty(PROP_REPOSITORY_NAME);
        if (pageTitle == null) {
            pageTitle = repository.getRepositoryName();
        }

        if (pageTitle.equals("none")) {
            pageTitle = "";
        }


        for (PageDecorator pageDecorator :
                repository.getPluginManager().getPageDecorators()) {
            String tmpTemplate = pageDecorator.decoratePage(repository,
                                     request, template, currentEntry);
            if (tmpTemplate != null) {
                template = tmpTemplate;
            }
        }


        StringBuilder extra = new StringBuilder();

        String userLinkTemplate =
            "<div onClick=\"document.location=\'${url}\'\"  class=\"ramadda-user-link\">${label}</div>";
        List<String> allLinks = new ArrayList<String>();
        String searchImg = HtmlUtils.faIcon("fa-search", "title",
                                            msg("Search"), "class",
                                            "ramadda-user-menu-image", "id",
                                            "searchlink");

        List<String> navLinks = getNavLinks(request, userLinkTemplate);
        List<String> userLinks = getUserLinks(request, userLinkTemplate,
                                     extra, true);

        allLinks.addAll(navLinks);
        allLinks.addAll(userLinks);

        String popupImage =
            HtmlUtils.faIcon("fa-cog", "title",
                             msg("Login, user settings, help"), "class",
                             "ramadda-user-menu-image");


        String menuHtml =
            HtmlUtils.div(StringUtil.join("", allLinks),
                          HtmlUtils.cssClass("ramadda-user-menu"));


        if (showSearch) {
            String searchLink =
                HtmlUtils.mouseClickHref("ramaddaSearchPopup('searchlink');",
                                         searchImg, "");
            extra.append(searchLink);
            extra.append(HtmlUtils.space(2));
        }
        extra.append(makePopupLink(popupImage, menuHtml, false, true));
        menuHtml = HtmlUtils.div(extra.toString(),
                                 HtmlUtils.clazz("ramadda-user-menu"));
        long     t1     = System.currentTimeMillis();

        String[] macros = new String[] {
            MACRO_LOGO_URL, logoUrl, MACRO_LOGO_IMAGE, logoImage,
            MACRO_HEADER_IMAGE, getHeaderIcon(), MACRO_HEADER_TITLE,
            pageTitle, MACRO_LINKS, menuHtml, MACRO_REPOSITORY_NAME,
            repository.getRepositoryName(), MACRO_FOOTER, footer, MACRO_TITLE,
            result.getTitle(), MACRO_BOTTOM, result.getBottomHtml(),
            MACRO_SEARCH_URL, getSearchManager().getSearchUrl(request),
            MACRO_CONTENT, content, MACRO_ENTRY_HEADER, entryHeader,
            MACRO_HEADER, header, MACRO_ENTRY_FOOTER, entryFooter,
            MACRO_ENTRY_BREADCRUMBS, entryBreadcrumbs, MACRO_REGISTER,
            registerMessage, MACRO_HEADFINAL, head, MACRO_ROOT,
            repository.getUrlBase(), "", ""
        };


        long                      t2     = System.currentTimeMillis();
        String                    html   = template;
        Hashtable<String, String> values = new Hashtable<String, String>();
        for (int i = 0; i < macros.length; i += 2) {
            values.put(macros[i], macros[i + 1]);
        }
        for (String property : htmlTemplate.getPropertyIds()) {
            values.put(Utils.concatString("${", property, "}"),
                       getRepository().getProperty(property, ""));
        }

        if (htmlTemplate.hasMacro(MACRO_FAVORITES)) {
            values.put(MACRO_FAVORITES, getFavorites(request, htmlTemplate));
        }

        StringBuilder sb = new StringBuilder();
        //Toks are [html,macro,html,macro,...,html]
        for (int i = 0; i < templateToks.size(); i++) {
            String v = templateToks.get(i);
            //Check if even or odd
            if (2 * (i / 2) == i) {
                sb.append(v);
            } else {
                String macroValue = values.get(v);
                if (macroValue == null) {
                    System.err.println("Whoa, no macro value:" + v);
                } else {
                    sb.append(macroValue);
                }
            }
        }
        html = sb.toString();

        long t3 = System.currentTimeMillis();
        html = translate(request, html);
        long t4 = System.currentTimeMillis();
        //        System.err.println ("html template: total:" + (t4-t0) + "  preface:" + (t1-t0) +" array:" + (t2-t1) +" replace:" + 
        //                            (t3-t2) +" translate:" + (t4-t3));
        /*
        if(html.indexOf("${")>=0) {
            System.out.println("Got macro in:" + request);
            int idx = html.indexOf("${");
            idx-=10;
            if(idx<0) idx=0;
            System.out.println("html:" + html.substring(idx));
        }
        */
        result.setContent(html);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param htmlTemplate _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getFavorites(Request request, HtmlTemplate htmlTemplate)
            throws Exception {

        String favoritesWrapper = htmlTemplate.getTemplateProperty(
                                      "ramadda.template.favorites.wrapper",
                                      "${link}");
        String favoritesTemplate =
            htmlTemplate.getTemplateProperty(
                "ramadda.template.favorites",
                "<span class=\"linkslabel\">Favorites:</span>${entries}");
        String favoritesSeparator =
            htmlTemplate.getTemplateProperty(
                "ramadda.template.favorites.separator", "");

        List<FavoriteEntry> favoritesList =
            getUserManager().getFavorites(request, request.getUser());
        StringBuilder favorites = new StringBuilder();
        if (favoritesList.size() > 0) {
            List favoriteLinks = new ArrayList();
            int  favoriteCnt   = 0;
            for (FavoriteEntry favorite : favoritesList) {
                if (favoriteCnt++ > 100) {
                    break;
                }

                Entry entry = favorite.getEntry();
                EntryLink entryLink = getEntryManager().getAjaxLink(request,
                                          entry, entry.getLabel(), null,
                                          false, null, false);
                if (entryLink != null) {
                    String link = favoritesWrapper.replace("${link}",
                                      entryLink.toString());
                    favoriteLinks.add("<nobr>" + link + "</nobr>");
                }
            }
            favoritesTemplate = applyBaseMacros(favoritesTemplate);
            favorites.append(favoritesTemplate.replace("${entries}",
                    StringUtil.join(favoritesSeparator, favoriteLinks)));
        }

        List<Entry> cartEntries = getUserManager().getCart(request);
        if (cartEntries.size() > 0) {
            String cartTemplate =
                htmlTemplate.getTemplateProperty("ramadda.template.cart",
                    "<b>Cart:<b><br>${entries}");
            List cartLinks = new ArrayList();
            for (Entry entry : cartEntries) {
                EntryLink entryLink = getEntryManager().getAjaxLink(request,
                                          entry, entry.getLabel(), null,
                                          false);
                String link = favoritesWrapper.replace("${link}",
                                  entryLink.toString());
                cartLinks.add("<nobr>" + link + "<nobr>");
            }
            favorites.append(HtmlUtils.br());
            favorites.append(cartTemplate.replace("${entries}",
                    StringUtil.join(favoritesSeparator, cartLinks)));
        }



        return favorites.toString();
    }


    /**
     * _more_
     *
     * @param result _more_
     *
     * @return _more_
     */
    public String getLogoImage(Result result) {
        String logoImage = null;
        if (result != null) {
            logoImage = (String) result.getProperty(PROP_LOGO_IMAGE);
        }
        if (logoImage == null) {
            logoImage = myLogoImage;
        }
        if (logoImage == null) {
            logoImage = "${root}/images/logo.png";
        }

        logoImage = applyBaseMacros(logoImage);

        return logoImage;
    }



    /** _more_ */
    private String templateJavascriptContent;

    /**
     * _more_
     *
     * @return _more_
     */
    public String getTemplateJavascriptContent() {
        if (templateJavascriptContent == null) {
            StringBuilder js = new StringBuilder();
            js.append(
                "$( document ).ready(function() {Utils.initPage(); });");

            //j-
            StringBuilder sb = new StringBuilder();
            HtmlUtils.div(sb, "",
                          HtmlUtils.attrs("id", "ramadda-popupdiv", "class",
                                          "ramadda-popup"));
            HtmlUtils.div(sb, "",
                          HtmlUtils.attrs("id", "ramadda-selectdiv", "class",
                                          "ramadda-selectdiv"));
            HtmlUtils.div(sb, "",
                          HtmlUtils.attrs("id", "ramadda-floatdiv", "class",
                                          "ramadda-floatdiv"));
            sb.append(HtmlUtils.script(js.toString()));
            //j+
            templateJavascriptContent = sb.toString();
        }

        return templateJavascriptContent;
    }




    /**
     * _more_
     *
     * @param template _more_
     * @param ignoreErrors _more_
     *
     * @return _more_
     */
    public String processTemplate(String template, boolean ignoreErrors) {
        List<String>  toks   = StringUtil.splitMacros(template);
        StringBuilder result = new StringBuilder();
        if (toks.size() > 0) {
            result.append(toks.get(0));
            for (int i = 1; i < toks.size(); i++) {
                if (2 * (i / 2) == i) {
                    result.append(toks.get(i));
                } else {
                    String prop = getRepository().getProperty(toks.get(i),
                                      (String) null);
                    if (prop == null) {
                        if (ignoreErrors) {
                            prop = "${" + toks.get(i) + "}";
                        } else {
                            throw new IllegalArgumentException(
                                "Could not find property:" + toks.get(i)
                                + ":");
                        }
                    }
                    if (prop.startsWith("bsf:")) {
                        prop = new String(
                            Utils.decodeBase64(prop.substring(4)));
                    }
                    result.append(prop);
                }
            }
        }

        return result.toString();
    }



    /**
     * _more_
     *
     * @param request The request
     * @param s _more_
     *
     * @return _more_
     */
    public String translate(Request request, String s) {

        if (s == null) {
            return "";
        }

        String     language = request.getLanguage();
        Properties tmpMap;
        Properties map = (Properties) languageMap.get(
                             getRepository().getLanguageDefault());

        if (map == null) {
            map = new Properties();
        }
        tmpMap = (Properties) languageMap.get(getRepository().getLanguage());
        if (tmpMap != null) {
            map.putAll(tmpMap);
        }
        tmpMap = (Properties) languageMap.get(language);

        if (tmpMap != null) {
            map.putAll(tmpMap);
        }

        Properties tmpPhraseMap = phraseMap;
        if (tmpPhraseMap == null) {
            String phrases = getRepository().getProperty(PROP_ADMIN_PHRASES,
                                 (String) null);
            if (phrases != null) {
                Object[] result = parsePhrases("", phrases);
                tmpPhraseMap = (Properties) result[2];
                phraseMap    = tmpPhraseMap;
            }
        }

        if (tmpPhraseMap != null) {
            map.putAll(tmpPhraseMap);
        }


        return replaceMsgNew(s.toString(), map);


        /**
         * StringBuilder stripped     = new StringBuilder();
         * int           prefixLength = MSG_PREFIX.length();
         * int           suffixLength = MSG_PREFIX.length();
         * //        System.out.println(s);
         * int transCnt = 0;
         * while (s.length() > 0) {
         *   String tmp  = s;
         *   int    idx1 = s.indexOf(MSG_PREFIX);
         *   if (idx1 < 0) {
         *       stripped.append(s);
         *       break;
         *   }
         *   String text = s.substring(0, idx1);
         *   if (text.length() > 0) {
         *       stripped.append(text);
         *   }
         *   s = s.substring(idx1 + 1);
         *
         *   int idx2 = s.indexOf(MSG_SUFFIX);
         *   if (idx2 < 0) {
         *       //Should never happen
         *       throw new IllegalArgumentException(
         *           "No closing message suffix:" + s);
         *   }
         *   String key   = s.substring(prefixLength - 1, idx2);
         *   String value = null;
         *   if (map != null) {
         *       value = (String) map.get(key);
         *   }
         *   if (debugMsg) {
         *       try {
         *           if (allMsgOutput == null) {
         *               allMsgOutput = new PrintWriter(
         *                   new FileOutputStream("allmessages.pack"));
         *               missingMsgOutput = new PrintWriter(
         *                   new FileOutputStream("missingmessages.pack"));
         *           }
         *           if ( !seenMsg.contains(key)) {
         *               allMsgOutput.println(key + "=");
         *               allMsgOutput.flush();
         *               System.err.println(key);
         *               if (value == null) {
         *                   missingMsgOutput.println(key + "=");
         *                   missingMsgOutput.flush();
         *               }
         *               seenMsg.add(key);
         *           }
         *       } catch (Exception exc) {
         *           throw new RuntimeException(exc);
         *       }
         *   }
         *
         *
         *   if (value == null) {
         *       value = key;
         *       if (debugMsg) {
         *           value = "NA:" + key;
         *       }
         *   }
         *   stripped.append(value);
         *   s = s.substring(idx2 + suffixLength);
         *   transCnt++;
         * }
         * return stripped.toString();
         */



    }


    /**
     * _more_
     *
     * @param s _more_
     * @param map _more_
     *
     * @return _more_
     */
    private static String replaceMsgNew(String s, Properties map) {
        StringBuilder stripped     = new StringBuilder();
        int           prefixLength = MSG_PREFIX.length();
        int           suffixLength = MSG_PREFIX.length();
        int           currentIdx   = 0;
        int           length       = s.length();
        while (currentIdx < length) {
            String tmp  = s;
            int    idx1 = s.indexOf(MSG_PREFIX, currentIdx);
            if (idx1 < 0) {
                stripped.append(s.substring(currentIdx));

                break;
            }
            String text = s.substring(currentIdx, idx1);
            stripped.append(text);
            currentIdx = idx1 + 1;

            int idx2 = s.indexOf(MSG_SUFFIX, currentIdx);
            if (idx2 < 0) {
                //Should never happen
                throw new IllegalArgumentException(
                    "No closing message suffix:" + s);
            }
            String key   = s.substring(currentIdx + prefixLength - 1, idx2);
            String value = null;
            if (map != null) {
                value = (String) map.get(key);
            }
            if (debugMsg) {
                try {
                    if (allMsgOutput == null) {
                        allMsgOutput = new PrintWriter(
                            new FileOutputStream("allmessages.pack"));
                        missingMsgOutput = new PrintWriter(
                            new FileOutputStream("missingmessages.pack"));
                    }
                    if ( !seenMsg.contains(key)) {
                        allMsgOutput.println(key + "=");
                        allMsgOutput.flush();
                        System.err.println(key);
                        if (value == null) {
                            missingMsgOutput.println(key + "=");
                            missingMsgOutput.flush();
                        }
                        seenMsg.add(key);
                    }
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
            }

            if (map != null) {
                value = (String) map.get(key);
            }

            if (value == null) {
                value = key;
                if (debugMsg) {
                    value = "NA:" + key;
                }
            }
            stripped.append(value);
            currentIdx = idx2 + suffixLength;
        }

        return stripped.toString();
    }



    /**
     * _more_
     *
     *
     * @param file _more_
     * @param content _more_
     *
     * @return _more_
     */
    private Object[] parsePhrases(String file, String content) {
        List<String> lines   = StringUtil.split(content, "\n", true, true);
        Properties   phrases = new Properties();
        String       type    =
            IOUtil.stripExtension(IOUtil.getFileTail(file));
        String       name    = type;
        for (String line : lines) {
            if (line.startsWith("#")) {
                continue;
            }
            List<String> toks = StringUtil.split(line, "=", true, true);
            if (toks.size() == 0) {
                continue;
            }
            String key = toks.get(0).trim();
            String value;
            if (toks.size() == 1) {
                if ( !debugMsg) {
                    continue;
                }
                value = "UNDEF:" + key;
            } else {
                value = toks.get(1).trim();
            }
            if (key.equals("language.id")) {
                type = value;
            } else if (key.equals("language.name")) {
                name = value;
            } else {
                if (value.length() == 0) {
                    if (debugMsg) {
                        value = "UNDEF:" + value;
                    } else {
                        continue;
                    }
                }


                phrases.put(key, value);
            }
        }

        return new Object[] { type, name, phrases };
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<TwoFacedObject> getLanguages() {
        return languages;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public HtmlTemplate getMobileTemplate() {
        getTemplates();

        return mobileTemplate;
    }


    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public String makeHtdocsUrl(String url) {
        return getRepository().getUrlBase() + "/"
               + RepositoryUtil.getHtdocsVersion() + url;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<HtmlTemplate> getTemplates() {

        List<HtmlTemplate> theTemplates = htmlTemplates;
        if ( !cacheTemplates || (theTemplates == null)) {
            String mobileId =
                getRepository().getProperty("ramadda.template.mobile",
                                            (String) null);
            HtmlTemplate theMobileTemplate = null;
            defaultTemplate = null;
            mobileTemplate  = null;
            String imports = "";
            try {
                imports = getStorageManager().readSystemResource(
                    "/org/ramadda/repository/resources/web/imports.html");
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }

            imports      = applyBaseMacros(imports);
            theTemplates = new ArrayList<HtmlTemplate>();
            templateMap  = new Hashtable<String, HtmlTemplate>();

            String defaultId =
                getRepository().getProperty(PROP_HTML_TEMPLATE_DEFAULT,
                                            DEFAULT_TEMPLATE);
            List<String> templatePaths =
                new ArrayList<String>(
                    getRepository().getPluginManager().getTemplateFiles());
            for (String path :
                    StringUtil.split(
                        getRepository().getProperty(
                            PROP_HTML_TEMPLATES,
                            "%resourcedir%/template.html"), ";", true,
                                true)) {
                path = getStorageManager().localizePath(path);
                templatePaths.add(path);
            }



            for (String path : templatePaths) {
                try {
                    //Skip resources called template.html that might be for other things
                    if (IOUtil.getFileTail(path).equals("template.html")) {
                        continue;
                    }
                    String resource =
                        getStorageManager().readSystemResource(path);
                    try {
                        resource = processTemplate(resource);
                    } catch (Exception exc) {
                        getLogManager().logError(
                            "failed to process template:" + path, exc);

                        continue;
                    }
                    String[] changes = { "userlink", MACRO_USERLINK,
                                         "html.imports", "imports", };
                    for (int i = 0; i < changes.length; i += 2) {
                        resource = resource.replace("${" + changes[i] + "}",
                                "${" + changes[i + 1] + "}");
                    }

                    resource = resource.replace("${imports}", imports);
                    HtmlTemplate template = new HtmlTemplate(getRepository(),
                                                path, resource);
                    //Check if we got some other ...template.html file from a plugin
                    if (template.getId() == null) {
                        System.err.println("template: no id in " + path);

                        continue;
                    }
                    templateMap.put(template.getId(), template);
                    theTemplates.add(template);

                    if ((mapTemplate == null)
                            && template.getId().equals("mapheader")) {
                        mapTemplate = template;
                    }

                    if (mobileTemplate == null) {
                        if (mobileId != null) {
                            if (template.getId().equals(mobileId)) {
                                mobileTemplate = template;
                            }
                        } else if (template.getTemplateProperty("mobile",
                                false)) {
                            //Don't do this for now
                            //                      mobileTemplate = template;
                        }
                    }
                    if ((theMobileTemplate == null)
                            && template.getId().equals("mobile")) {
                        theMobileTemplate = template;
                    }
                    if (defaultTemplate == null) {
                        if (defaultId == null) {
                            defaultTemplate = template;
                        } else {
                            if (Misc.equals(defaultId, template.getId())) {
                                defaultTemplate = template;
                            }
                        }
                    }
                } catch (Exception exc) {
                    getLogManager().logError("loading template" + path, exc);
                    //noop
                }
            }
            if (mobileTemplate == null) {
                mobileTemplate = theMobileTemplate;
            }
            if (defaultTemplate == null) {
                defaultTemplate = theTemplates.get(0);
            }
            if (mobileTemplate == null) {
                mobileTemplate = defaultTemplate;
            }
            //            if (getRepository().getCacheResources()) {
            htmlTemplates = theTemplates;
            //            }
        }

        return theTemplates;

    }


    /**
     * _more_
     *
     * @param html _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String processTemplate(String html) throws Exception {
        StringBuilder template = new StringBuilder();
        while (true) {
            int idx1 = html.indexOf("<include");
            if (idx1 < 0) {
                template.append(html);

                break;
            }
            template.append(html.substring(0, idx1));
            html = html.substring(idx1);
            idx1 = html.indexOf(">") + 1;
            String include = html.substring(0, idx1);
            include = include.substring("<include".length());
            include = include.replace(">", "");
            Hashtable props = HtmlUtils.parseHtmlProperties(include);
            String    url   = (String) props.get("href");
            if (url != null) {
                String includedContent =
                    getStorageManager().readSystemResource(new URL(url));
                //                String includedContent =  IOUtil.readContents(url, Repository.class);
                template.append(includedContent);
            }
            html = html.substring(idx1);
        }
        html = template.toString();
        if (html.indexOf("${imports}") < 0) {
            html = html.replace("<head>", "<head>\n${imports}");
        }
        if (html.indexOf("${headfinal}") < 0) {
            html = html.replace("</head>", "${headfinal}\n</head>");
        }

        return html;
    }



    /** _more_ */
    private HashSet<String> seenPack = new HashSet<String>();

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void loadLanguagePacks() throws Exception {
        //        getLogManager().logInfoAndPrint("RAMADDA: loadLanguagePacks");
        List sourcePaths =
            Misc.newList(
                getStorageManager().getSystemResourcePath() + "/languages",
                getStorageManager().getPluginsDir().toString());
        for (int i = 0; i < sourcePaths.size(); i++) {
            String       dir     = (String) sourcePaths.get(i);
            List<String> listing = getRepository().getListing(dir,
                                       getClass());
            for (String path : listing) {
                if ( !path.endsWith(".pack")) {
                    if (i == 0) {
                        getLogManager().logInfoAndPrint(
                            "RAMADDA: not ends with .pack:" + path);
                    }

                    continue;
                }
                if (seenPack.contains(path)) {
                    continue;
                }
                seenPack.add(path);
                String content =
                    getStorageManager().readUncheckedSystemResource(path,
                        (String) null);
                if (content == null) {
                    getLogManager().logInfoAndPrint(
                        "RAMADDA: could not read:" + path);

                    continue;
                }
                Object[]   result     = parsePhrases(path, content);
                String     type       = (String) result[0];
                String     name       = (String) result[1];
                Properties properties = (Properties) result[2];
                if (type != null) {
                    if (name == null) {
                        name = type;
                    }
                    languages.add(new TwoFacedObject(name, type));
                    languageMap.put(type, properties);
                } else {
                    getLogManager().logError("No _type_ found in: " + path);
                }
            }
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<MapRegion> getMapRegions() {
        return getMapRegions(null);
    }

    /**
     * _more_
     *
     * @param group _more_
     *
     * @return _more_
     */
    public List<MapRegion> getMapRegions(String group) {
        if (group == null) {
            return mapRegions;
        }
        List<MapRegion> regions = new ArrayList<MapRegion>();
        for (MapRegion region : mapRegions) {
            if (region.isGroup(group)) {
                regions.add(region);
            }
        }

        return regions;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param inputArgBase _more_
     *
     * @return _more_
     */
    public String getMapRegionSelector(Request request, String inputArgBase) {
        StringBuilder sb = new StringBuilder();

        return sb.toString();
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void loadMapRegions() throws Exception {
        List<String> mapRegionFiles = new ArrayList<String>();
        List<String> allFiles       = getPluginManager().getAllFiles();
        for (String f : allFiles) {
            if (f.endsWith("regions.csv")) {
                mapRegionFiles.add(f);
            }
        }

        String dir = getStorageManager().getSystemResourcePath() + "/geo";
        List<String> listing = getRepository().getListing(dir, getClass());
        for (String f : listing) {
            if (f.endsWith("regions.csv")) {
                mapRegionFiles.add(f);
            }
        }

        for (String path : mapRegionFiles) {
            String contents =
                getStorageManager().readUncheckedSystemResource(path,
                    (String) null);
            if (contents == null) {
                getLogManager().logInfoAndPrint("RAMADDA: could not read:"
                        + path);

                continue;
            }
            //Name,ID,Group,North,West,South,East
            //Group
            List<String> lines = StringUtil.split(contents, "\n", true, true);
            lines.remove(0);
            String group = lines.get(0);
            lines.remove(0);
            for (String line : lines) {
                List<String> toks = StringUtil.split(line, ",");
                if (toks.size() != 6) {
                    throw new IllegalArgumentException("Bad map region line:"
                            + line + "\nFile:" + path);
                }


                mapRegions.add(
                    new MapRegion(
                        toks.get(1), toks.get(0), group,
                        Utils.decodeLatLon(toks.get(2)),
                        Utils.decodeLatLon(toks.get(3)),
                        Utils.decodeLatLon(toks.get(4)),
                        Utils.decodeLatLon(toks.get(5))));
            }

        }
    }



    /**
     * _more_
     *
     * @param request The request
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getTemplateProperty(Request request, String name,
                                      String dflt) {
        return getTemplate(request).getTemplateProperty(name, dflt);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<TwoFacedObject> getTemplateSelectList() {
        List<TwoFacedObject> tfos = new ArrayList<TwoFacedObject>();
        tfos.add(new TwoFacedObject("-default-", ""));
        for (HtmlTemplate template : getTemplates()) {
            tfos.add(new TwoFacedObject(template.getName(),
                                        template.getId()));
        }

        return tfos;

    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public HtmlTemplate getTemplate(Request request) {
        Entry currentEntry = null;
        if (request != null) {
            try {
                currentEntry = getSessionManager().getLastEntry(request);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        return getTemplate(request, currentEntry);
    }


    /**
     * Find the html template for the given request
     *
     * @param request The request
     * @param entry _more_
     *
     * @return _more_
     */
    public HtmlTemplate getTemplate(Request request, Entry entry) {
        HtmlTemplate template = getTemplateInner(request, entry);
        if ((template != null) && !getAdmin().isRegistered()) {
            if ( !(template.getId().equals("mapheader")
                    || template.getId().equals("mobile")
                    || template.getId().equals("empty"))) {
                if ( !getRepository().getProperty("ramadda.override",
                        false)) {
                    if (mapTemplate != null) {
                        return mapTemplate;
                    }

                    return getTemplates().get(0);
                }
            }
        }

        return template;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    private HtmlTemplate getTemplateInner(Request request, Entry entry) {
        //this forces the possible reload of the templates
        getTemplates();
        if (request == null) {
            return defaultTemplate;
        }
        boolean isMobile = request.isMobile();
        //Check for template=... url arg
        String templateId = request.getHtmlTemplateId();
        if (Utils.stringDefined(templateId)) {
            HtmlTemplate template = templateMap.get(templateId);
            if (template != null) {
                return template;
            }
        }

        //Check for metadata template definition
        if ( !Utils.stringDefined(templateId) && (entry != null)) {
            try {
                List<Metadata> metadataList =
                    getMetadataManager().findMetadata(request, entry,
                        ContentMetadataHandler.TYPE_TEMPLATE, true);
                if (metadataList != null) {
                    for (Metadata metadata : metadataList) {
                        templateId = metadata.getAttr1();
                        if (isMobile) {
                            HtmlTemplate template =
                                templateMap.get(templateId);
                            if ((template != null)
                                    && template.getTemplateProperty("mobile",
                                        false)) {
                                request.put(ARG_TEMPLATE, template.getId());

                                return template;
                            }
                        } else {
                            request.put(ARG_TEMPLATE, templateId);
                        }

                        break;
                    }
                }
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        if (isMobile && (mobileTemplate != null)) {
            request.put(ARG_TEMPLATE, mobileTemplate.getId());

            return mobileTemplate;
        }


        User user = request.getUser();
        if ((templateId == null) && (user != null) && !user.getAnonymous()) {
            templateId = user.getTemplate();
        }

        if (templateId != null) {
            HtmlTemplate template = templateMap.get(templateId);
            if (template != null) {
                return template;
            }
        }

        return defaultTemplate;
    }




    /**
     * _more_
     *
     * @param msg _more_
     *
     * @return _more_
     */
    public static String msg(String msg) {
        if (msg == null) {
            return null;
        }
        if (msg.indexOf(MSG_PREFIX) >= 0) {
            //            System.err.println("bad msg:" + msg+"\n" + LogUtil.getStackTrace());
            //            throw new IllegalArgumentException("bad msg:" + msg);
            return msg;

        }

        return Utils.concatString(MSG_PREFIX, msg, MSG_SUFFIX);
    }

    /**
     * _more_
     *
     * @param msg _more_
     *
     * @return _more_
     */
    public static String msgLabel(String msg) {
        if (msg == null) {
            return null;
        }
        if (msg.length() == 0) {
            return msg;
        }

        return Utils.concatString(msg(msg), ":", HtmlUtils.space(1));
    }

    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    public static String msgHeader(String h) {
        return HtmlUtils.div(
            msg(h),
            HtmlUtils.cssClass(
                "ramadda-page-heading ramadda-page-heading-left" /*CSS_CLASS_HEADING_1*/));
    }




    /**
     * _more_
     *
     * @param sb _more_
     * @param date _more_
     * @param url _more_
     * @param dayLinks _more_
     *
     * @throws Exception _more_
     */
    public void createMonthNav(Appendable sb, Date date, String url,
                               Hashtable dayLinks)
            throws Exception {

        GregorianCalendar cal =
            new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);
        cal.setTime(date);
        int[] theDate  = CalendarOutputHandler.getDayMonthYear(cal);
        int   theDay   = cal.get(cal.DAY_OF_MONTH);
        int   theMonth = cal.get(cal.MONTH);
        int   theYear  = cal.get(cal.YEAR);
        while (cal.get(cal.DAY_OF_MONTH) > 1) {
            cal.add(cal.DAY_OF_MONTH, -1);
        }
        GregorianCalendar prev =
            new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);
        prev.setTime(date);
        prev.add(cal.MONTH, -1);
        GregorianCalendar next =
            new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);
        next.setTime(date);
        next.add(cal.MONTH, 1);

        HtmlUtils.open(sb, HtmlUtils.TAG_TABLE, HtmlUtils.ATTR_BORDER, "1",
                       HtmlUtils.ATTR_CELLSPACING, "0",
                       HtmlUtils.ATTR_CELLPADDING, "0");
        String[] dayNames = {
            "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"
        };
        String prevUrl = HtmlUtils.space(1)
                         + HtmlUtils.href(
                             url + "&"
                             + CalendarOutputHandler.getUrlArgs(
                                 prev), "&lt;");
        String nextUrl =
            HtmlUtils.href(
                url + "&" + CalendarOutputHandler.getUrlArgs(next),
                HtmlUtils.ENTITY_GT) + HtmlUtils.space(1);
        HtmlUtils.open(sb, HtmlUtils.TAG_TR, HtmlUtils.ATTR_VALIGN,
                       HtmlUtils.VALUE_TOP);
        HtmlUtils.open(sb, HtmlUtils.TAG_TD, HtmlUtils.ATTR_COLSPAN, "7",
                       HtmlUtils.ATTR_ALIGN, HtmlUtils.VALUE_CENTER,
                       HtmlUtils.ATTR_CLASS, "calnavmonthheader");

        HtmlUtils.open(sb, HtmlUtils.TAG_TABLE, "class", "calnavtable",
                       HtmlUtils.ATTR_CELLSPACING, "0",
                       HtmlUtils.ATTR_CELLPADDING, "0", HtmlUtils.ATTR_WIDTH,
                       "100%");
        HtmlUtils.open(sb, HtmlUtils.TAG_TR);
        HtmlUtils.col(sb, prevUrl,
                      HtmlUtils.attrs(HtmlUtils.ATTR_WIDTH, "1",
                                      HtmlUtils.ATTR_CLASS,
                                      "calnavmonthheader"));
        HtmlUtils.col(
            sb, DateUtil.MONTH_NAMES[cal.get(cal.MONTH)] + HtmlUtils.space(1)
            + theYear, HtmlUtils.attr(
                HtmlUtils.ATTR_CLASS, "calnavmonthheader"));

        HtmlUtils.col(sb, nextUrl,
                      HtmlUtils.attrs(HtmlUtils.ATTR_WIDTH, "1",
                                      HtmlUtils.ATTR_CLASS,
                                      "calnavmonthheader"));
        HtmlUtils.close(sb, HtmlUtils.TAG_TABLE);
        HtmlUtils.close(sb, HtmlUtils.TAG_TR);
        HtmlUtils.open(sb, HtmlUtils.TAG_TR);
        for (int colIdx = 0; colIdx < 7; colIdx++) {
            HtmlUtils.col(sb, dayNames[colIdx],
                          HtmlUtils.attrs(HtmlUtils.ATTR_WIDTH, "14%",
                                          HtmlUtils.ATTR_CLASS,
                                          "calnavdayheader"));
        }
        HtmlUtils.close(sb, HtmlUtils.TAG_TR);
        int startDow = cal.get(cal.DAY_OF_WEEK);
        while (startDow > 1) {
            cal.add(cal.DAY_OF_MONTH, -1);
            startDow--;
        }
        for (int rowIdx = 0; rowIdx < 6; rowIdx++) {
            HtmlUtils.open(sb, HtmlUtils.TAG_TR, HtmlUtils.ATTR_VALIGN,
                           HtmlUtils.VALUE_TOP);
            for (int colIdx = 0; colIdx < 7; colIdx++) {
                int     thisDay    = cal.get(cal.DAY_OF_MONTH);
                int     thisMonth  = cal.get(cal.MONTH);
                int     thisYear   = cal.get(cal.YEAR);
                boolean currentDay = false;
                String  dayClass   = "calnavday";
                if (thisMonth != theMonth) {
                    dayClass = "calnavoffday";
                } else if ((theMonth == thisMonth) && (theYear == thisYear)
                           && (theDay == thisDay)) {
                    dayClass   = "calnavtheday";
                    currentDay = true;
                }
                String content;
                if (dayLinks != null) {
                    String key = thisYear + "/" + thisMonth + "/" + thisDay;
                    if (dayLinks.get(key) != null) {
                        content = HtmlUtils.href(url + "&"
                                + CalendarOutputHandler.getUrlArgs(cal), ""
                                    + thisDay);
                        if ( !currentDay) {
                            dayClass = "calnavoffday";
                        }
                    } else {
                        content  = "" + thisDay;
                        dayClass = "calnavday";
                    }
                } else {
                    content = HtmlUtils.href(
                        url + "&" + CalendarOutputHandler.getUrlArgs(cal),
                        "" + thisDay);
                }

                sb.append(HtmlUtils.col(content,
                                        HtmlUtils.cssClass(dayClass)));
                sb.append("\n");
                cal.add(cal.DAY_OF_MONTH, 1);
            }
            if (cal.get(cal.MONTH) > theMonth) {
                break;
            }
            if (cal.get(cal.YEAR) > theYear) {
                break;
            }
        }
        sb.append(HtmlUtils.close(HtmlUtils.TAG_TABLE));

    }


    /**
     * _more_
     *
     * @param link _more_
     * @param menuContents _more_
     *
     * @return _more_
     */
    public String makePopupLink(String link, String menuContents) {
        return makePopupLink(link, menuContents, false, false);
    }


    /**
     * _more_
     *
     * @param link _more_
     * @param menuContents _more_
     * @param makeClose _more_
     * @param alignLeft _more_
     *
     * @return _more_
     */
    public String makePopupLink(String link, String menuContents,
                                boolean makeClose, boolean alignLeft) {
        return makePopupLink(link, menuContents,
                             " class=\"ramadda-popup-link\" ", makeClose,
                             alignLeft);
    }


    /**
     * _more_
     *
     * @param link _more_
     * @param menuContents _more_
     * @param linkAttributes _more_
     *
     * @return _more_
     */
    public String makePopupLink(String link, String menuContents,
                                String linkAttributes) {
        return makePopupLink(link, menuContents, linkAttributes, false,
                             false);
    }

    /**
     * _more_
     *
     * @param link _more_
     * @param menuContents _more_
     * @param linkAttributes _more_
     * @param makeClose _more_
     * @param alignLeft _more_
     *
     * @return _more_
     */
    public String makePopupLink(String link, String menuContents,
                                String linkAttributes, boolean makeClose,
                                boolean alignLeft) {
        StringBuilder sb = new StringBuilder();
        link = makePopupLink(link, menuContents, linkAttributes, makeClose,
                             alignLeft, sb);

        return link + sb;
    }

    /**
     * _more_
     *
     * @param link _more_
     * @param menuContents _more_
     * @param linkAttributes _more_
     * @param makeClose _more_
     * @param alignLeft _more_
     * @param popup _more_
     *
     * @return _more_
     */
    public String makePopupLink(String link, String menuContents,
                                String linkAttributes, boolean makeClose,
                                boolean alignLeft, Appendable popup) {
        try {

            String compId = "menu_" + HtmlUtils.blockCnt++;
            String linkId = "menulink_" + HtmlUtils.blockCnt++;
            popup.append(makePopupDiv(menuContents, compId, makeClose));
            String onClick =
                HtmlUtils.onMouseClick(HtmlUtils.call("showPopup",
                    HtmlUtils.comma(new String[] { "event",
                    HtmlUtils.squote(linkId), HtmlUtils.squote(compId),
                    (alignLeft
                     ? "1"
                     : "0") })));
            String href = HtmlUtils.href("javascript:noop();", link,
                                         onClick + HtmlUtils.id(linkId)
                                         + linkAttributes);

            return href;
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        }


    }



    /**
     * _more_
     *
     * @param link _more_
     * @param innerContents _more_
     * @param initCall _more_
     *
     * @return _more_
     */
    public String makeStickyPopup(String link, String innerContents,
                                  String initCall) {
        boolean alignLeft = true;
        String  compId    = "menu_" + HtmlUtils.blockCnt++;
        String  linkId    = "menulink_" + HtmlUtils.blockCnt++;
        String  contents  = makeStickyPopupDiv(innerContents, compId);
        String onClick =
            HtmlUtils.onMouseClick(HtmlUtils.call("showStickyPopup",
                HtmlUtils.comma(new String[] { "event",
                HtmlUtils.squote(linkId), HtmlUtils.squote(compId), (alignLeft
                ? "1"
                : "0") })) + initCall);
        String href = HtmlUtils.href("javascript:noop();", link,
                                     onClick + HtmlUtils.id(linkId));

        return href + contents;
    }



    /**
     * _more_
     *
     * @param contents _more_
     * @param compId _more_
     *
     * @return _more_
     */
    public String makeStickyPopupDiv(String contents, String compId) {
        StringBuilder menu = new StringBuilder();
        String cLink = HtmlUtils.jsLink(
                           HtmlUtils.onMouseClick(
                               HtmlUtils.call(
                                   "hideElementById",
                                   HtmlUtils.squote(compId))), getIconImage(
                                       ICON_CLOSE, "title", "Close", "class",
                                       "ramadda-popup-close"), "");
        contents = cLink + HtmlUtils.br() + contents;

        menu.append(HtmlUtils.div(contents,
                                  HtmlUtils.id(compId)
                                  + HtmlUtils.cssClass(CSS_CLASS_POPUP)));

        return menu.toString();
    }




    /**
     * _more_
     *
     * @param contents _more_
     * @param compId _more_
     * @param makeClose _more_
     *
     * @return _more_
     */
    public String makePopupDiv(String contents, String compId,
                               boolean makeClose) {
        StringBuilder menu = new StringBuilder();
        if (makeClose) {
            String cLink = HtmlUtils.jsLink(
                               HtmlUtils.onMouseClick("hidePopupObject();"),
                               getIconImage(
                                   ICON_CLOSE, "title", "Close", "class",
                                   "ramadda-popup-close"), "");
            contents = cLink + HtmlUtils.br() + contents;
        }

        menu.append(HtmlUtils.div(contents,
                                  HtmlUtils.id(compId)
                                  + HtmlUtils.attr("style", "display:none;")
                                  + HtmlUtils.cssClass(CSS_CLASS_POPUP)));


        return menu.toString();
    }



    /**
     * _more_
     *
     * @param request The request
     * @param url _more_
     * @param okArg _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String makeOkCancelForm(Request request, RequestUrl url,
                                          String okArg, String extra) {
        StringBuilder fb = new StringBuilder();
        fb.append(request.form(url));
        fb.append(extra);
        String okButton     = HtmlUtils.submit("OK", okArg);
        String cancelButton = HtmlUtils.submit("Cancel",
                                  Constants.ARG_CANCEL);
        String buttons = HtmlUtils.buttons(okButton, cancelButton);
        fb.append(buttons);
        fb.append(HtmlUtils.formClose());

        return fb.toString();
    }




    /**
     * Get the login/settings/help links
     *
     * @param request the request
     * @param template template to make the links
     * @param prefix _more_
     * @param makePopup _more_
     *
     * @return user links
     */
    private List<String> getUserLinks(Request request, String template,
                                      StringBuilder prefix,
                                      boolean makePopup) {
        User user   = request.getUser();

        List extras = new ArrayList();
        List urls   = new ArrayList();
        List labels = new ArrayList();
        List tips   = new ArrayList();


        //System.err.println("Request:" + request.getUrl());

        if (user.getAnonymous()) {
            if (getUserManager().canDoLogin(request)) {
                extras.add("");
                String url;

                String path = request.getRequestPath();
                //If it was  a post or  if this was a user access request 
                //then don't include the redirect back to this page
                if (request.isPost() || (path.indexOf("/user/") >= 0)
                        || (path.indexOf("/admin/") >= 0)) {
                    url = request.makeUrl(getRepositoryBase().URL_USER_LOGIN);
                } else {
                    //The request.getUrlArgs will always exclude the passwords
                    request.remove(ARG_MESSAGE);
                    request.remove(ARG_REDIRECT);
                    request.remove(ARG_USER_ID);
                    String redirect = Utils.encodeBase64(request.getUrl());
                    url = request.makeUrl(getRepositoryBase().URL_USER_LOGIN,
                                          ARG_REDIRECT, redirect);
                }

                urls.add(url);
                labels.add(HtmlUtils.faIcon("fa-sign-in-alt") + " "
                           + msg("Login"));
                tips.add(msg("Login"));
            }

            if (getUserManager().isCartEnabled()) {
                extras.add("");
                urls.add(request.makeUrl(getRepositoryBase().URL_USER_CART));
                labels.add(getIconImage("/icons/cart.png") + " "
                           + msg("Data Cart"));
                tips.add(msg("View data cart"));
            }
        } else {
            extras.add("");
            urls.add(request.makeUrl(getRepositoryBase().URL_USER_LOGOUT));
            labels.add(HtmlUtils.faIcon("fa-sign-out-alt") + " "
                       + msg("Logout"));
            tips.add(msg("Logout"));
            String label = user.getLabel().replace(" ", "&nbsp;");
            String userIcon = HtmlUtils.faIcon("fa-user", "title",
                                  "Settings for " + label, "class",
                                  "ramadda-user-menu-image");

            String settingsUrl =
                request.makeUrl(getRepositoryBase().URL_USER_FORM);

            if (makePopup) {
                prefix.append(
                    HtmlUtils.href(
                        settingsUrl, userIcon,
                        HtmlUtils.cssClass("ramadda-user-settings-link")));
                prefix.append(HtmlUtils.space(2));
            } else {
                extras.add("");
                urls.add(settingsUrl);
                labels.add(label);
                tips.add(msg("Go to user settings"));
            }
        }

        if (getRepository()
                .getProperty(getUserManager()
                    .PROP_SHOW_HELP, true) && (getRepository()
                        .getPluginManager().getDocUrls().size() > 0)) {
            urls.add(request.makeUrl(getRepositoryBase().URL_HELP));
            extras.add("");
            labels.add(HtmlUtils.faIcon("fa-question-circle") + " "
                       + msg("Help"));
            tips.add(msg("View Help"));
        }


        List<String> links = new ArrayList<String>();
        for (int i = 0; i < urls.size(); i++) {
            String link = template.replace("${label}",
                                           labels.get(i).toString());
            link = link.replace("${url}", urls.get(i).toString());
            link = link.replace("${tooltip}", tips.get(i).toString());
            link = link.replace("${extra}", extras.get(i).toString());
            links.add(link);
        }


        return links;
    }




    /**
     * _more_
     *
     *
     * @param request The request
     * @param template _more_
     * @return _more_
     */
    private List<String> getNavLinks(Request request, String template) {
        List<String> links   = new ArrayList<String>();
        boolean      isAdmin = false;
        if (request != null) {
            User user = request.getUser();
            isAdmin = user.getAdmin();
        }


        ApiMethod homeApi = getRepository().getApiManager().getHomeApi();
        for (ApiMethod apiMethod :
                getRepository().getApiManager().getTopLevelMethods()) {
            if (apiMethod.getMustBeAdmin() && !isAdmin) {
                continue;
            }
            if ( !apiMethod.getIsTopLevel()) {
                continue;
            }
            String label = msg(apiMethod.getName());
            String icon  = apiMethod.getIcon();
            String url;
            if (apiMethod == homeApi) {
                url = getFileUrl(apiMethod.getRequest());
            } else {
                url = request.makeUrl(apiMethod.getUrl());
            }

            if (icon != null) {
                label = getIconImage(icon) + " " + label;
            }

            String html = template.replace("${url}", url);
            html = html.replace("${label}", label);

            html = html.replace("${topgroup}",
                                request.getRootEntry().getName());
            links.add(html);
        }


        return links;
    }


    /**
     * _more_
     *
     * @param request The request
     * @param sb _more_
     * @param urls _more_
     * @param arg _more_
     *
     *
     * @throws Exception _more_
     */
    public void makeLinksHeader(Request request, Appendable sb,
                                List<RequestUrl> urls, String arg)
            throws Exception {
        List<String> links   = new ArrayList();
        String       type    = request.getRequestPath();
        String       onLabel = null;
        for (RequestUrl requestUrl : urls) {
            String label = requestUrl.getLabel();
            label = msg(label);
            if (label == null) {
                label = requestUrl.toString();
            }
            String url   = request.makeUrl(requestUrl) + arg;
            String clazz = "ramadda-linksheader-off";
            if (type.endsWith(requestUrl.getPath())) {
                onLabel = label;
                clazz   = "ramadda-linksheader-on";
            }
            links.add(HtmlUtils.span(HtmlUtils.href(url, label),
                                     HtmlUtils.cssClass(clazz)));
            //            }
        }
        StringBuilder header = new StringBuilder();
        HtmlUtils.div(
            header,
            StringUtil.join(
                "<span class=\".ramadda-separator\">|</span>",
                links), HtmlUtils.cssClass("ramadda-linksheader-links"));
        header.append("\n");
        if (Utils.stringDefined(onLabel)) {
            header.append(
                HtmlUtils.div(
                    msg(onLabel), HtmlUtils.cssClass("ramadda-page-title")));
        }
        sb.append(HtmlUtils.tag(HtmlUtils.TAG_DIV,
                                HtmlUtils.cssClass("ramadda-linksheader"),
                                header.toString()));
    }



    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    public String showDialogNote(String h) {
        return getMessage(h, "/icons/information-32.png", false);
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
     *
     * @return _more_
     */
    public String showDialogWarning(String h) {
        return getMessage(h, Constants.ICON_WARNING, false);
    }


    /**
     * _more_
     *
     * @param h _more_
     * @param buttons _more_
     *
     * @return _more_
     */
    public String showDialogQuestion(String h, String buttons) {
        return getMessage(h + "<p><hr>" + buttons, Constants.ICON_QUESTION,
                          false);
    }

    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    public String showDialogError(String h) {
        return showDialogError(h, true);
    }

    /**
     * _more_
     *
     * @param h _more_
     * @param cleanString _more_
     *
     * @return _more_
     */
    public String showDialogError(String h, boolean cleanString) {
        if (cleanString) {
            h = getDialogString(h);
        }

        return getMessage(h, Constants.ICON_ERROR, false);
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String getDialogString(String s) {
        s = s.replaceAll("<pre>", "PREOPEN");
        s = s.replaceAll("</pre>", "PRECLOSE");
        s = HtmlUtils.entityEncode(s);
        s = s.replace("&#60;msg&#32;", "<msg ");
        s = s.replace("&#32;msg&#62;", " msg>");
        s = s.replace("&#32;", " ");
        s = s.replace("&#60;p&#62;", "<p>");
        s = s.replace("&#60;br&#62;", "<br>");
        s = s.replace("&#38;nbsp&#59;", "&nbsp;");
        s = s.replaceAll("PREOPEN", "<pre>");
        s = s.replaceAll("PRECLOSE", "</pre>");

        return s;
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
        h = h.replaceAll("\n", "<br>");
        h = h.replaceAll("&#10;", "<br>");
        String html = showClose
                      ? HtmlUtils.jsLink(
                          HtmlUtils.onMouseClick("hide('messageblock')"),
                          getIconImage(Constants.ICON_CLOSE))
                      : "&nbsp;";
        StringBuilder sb = new StringBuilder();
        sb.append(HtmlUtils.open(HtmlUtils.TAG_DIV, "class",
                                 "ramadda-message", "id", "messageblock"));
        sb.append(
            "<table><tr valign=top><td><div class=\"ramadda-message-link\">");
        sb.append(getIconImage(icon));
        sb.append("</div></td><td><div class=\"ramadda-message-inner\">");
        sb.append(h);
        sb.append("</div></td>");
        if (showClose) {
            sb.append("<td><div class=\"ramadda-message-link\">");
            sb.append(html);
            sb.append("</div></td>");
        }
        sb.append("</tr></table>");
        HtmlUtils.close(sb, HtmlUtils.TAG_DIV);
        sb.append(HtmlUtils.br());

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public PageStyle doMakePageStyle(Request request, Entry entry) {
        try {
            PageStyle pageStyle = new PageStyle();
            if (request.isMobile()) {
                pageStyle.setShowToolbar(false);
            }
            //for now - default the toolbar=false
            pageStyle.setShowToolbar(false);

            if (request.exists(PROP_NOSTYLE)
                    || getRepository().getProperty(PROP_NOSTYLE, false)) {
                return pageStyle;
            }
            List<Metadata> metadataList =
                getMetadataManager().findMetadata(request, entry,
                    ContentMetadataHandler.TYPE_PAGESTYLE, true);
            if ((metadataList == null) || (metadataList.size() == 0)) {
                return pageStyle;
            }

            //menus -1, showbreadcrumbs-2, toolbar-3, entry header-4, layout toolbar-5, type-6,  apply to this-7, wiki-8
            Metadata theMetadata = null;
            for (Metadata metadata : metadataList) {
                if (Misc.equals(metadata.getAttr(7), "false")) {
                    if (metadata.getEntryId().equals(entry.getId())) {
                        continue;
                    }
                }
                String types = metadata.getAttr(6);
                if ((types == null) || (types.trim().length() == 0)) {
                    theMetadata = metadata;

                    break;
                }
                for (String type : StringUtil.split(types, ",", true, true)) {
                    if (type.equals("file") && !entry.isGroup()) {
                        theMetadata = metadata;

                        break;
                    }
                    if (type.equals("folder") && entry.isGroup()) {
                        theMetadata = metadata;

                        break;
                    }
                    if (entry.getTypeHandler().isType(type)) {
                        theMetadata = metadata;

                        break;
                    }
                }
            }

            if (theMetadata == null) {
                return pageStyle;
            }

            pageStyle.setShowBreadcrumbs(Misc.equals(theMetadata.getAttr2(),
                    "true"));

            pageStyle.setShowToolbar(Misc.equals(theMetadata.getAttr3(),
                    "true"));
            pageStyle.setShowEntryHeader(Misc.equals(theMetadata.getAttr4(),
                    "true"));
            pageStyle.setShowLayoutToolbar(
                Misc.equals(theMetadata.getAttr(5), "true"));

            boolean canEdit = getAccessManager().canDoAction(request, entry,
                                  Permission.ACTION_EDIT);
            if ( !canEdit) {
                String menus = theMetadata.getAttr1();
                if ((menus != null) && (menus.trim().length() > 0)) {
                    if (menus.equals("none")) {
                        pageStyle.setShowMenubar(false);
                    } else {
                        for (String menu :
                                StringUtil.split(menus, ",", true, true)) {
                            pageStyle.setMenu(menu);
                        }
                    }
                }
            }
            if ((theMetadata.getAttr(8) != null)
                    && (theMetadata.getAttr(8).trim().length() > 0)) {
                pageStyle.setWikiTemplate(theMetadata.getAttr(8));
            }

            return pageStyle;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getConfirmBreadCrumbs(Request request, Entry entry)
            throws Exception {
        return HtmlUtils.img(getIconUrl(request, entry)) + " "
               + getBreadCrumbs(request, entry);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public String getBreadCrumbs(Request request, Entry entry)
            throws Exception {
        return getBreadCrumbs(request, entry, null, null, 80);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param stopAt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getBreadCrumbs(Request request, Entry entry, Entry stopAt)
            throws Exception {
        return getBreadCrumbs(request, entry, stopAt, null, 80);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param stopAt _more_
     * @param requestUrl _more_
     * @param lengthLimit _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getBreadCrumbs(Request request, Entry entry, Entry stopAt,
                                 RequestUrl requestUrl, int lengthLimit)
            throws Exception {
        if (entry == null) {
            return BLANK;
        }

        if (requestUrl == null) {
            requestUrl = getRepository().URL_ENTRY_SHOW;
        }
        List breadcrumbs = new ArrayList();
        Entry parent = getEntryManager().findGroup(request,
                           entry.getParentEntryId());
        int         length          = 0;
        List<Entry> parents         = new ArrayList<Entry>();
        int         totalNameLength = 0;
        while (parent != null) {
            parents.add(parent);
            String name = parent.getName();
            totalNameLength += name.length();
            if (stopAt != null) {
                if (stopAt.getId().equals(parent.getId())) {
                    break;
                }
            }

            parent = getEntryManager().findGroup(request,
                    parent.getParentEntryId());
        }

        boolean needToClip = totalNameLength > lengthLimit;
        String  target     = (request.defined(ARG_TARGET)
                              ? request.getString(ARG_TARGET, "")
                              : null);
        String  targetAttr = ((target != null)
                              ? HtmlUtils.attr(HtmlUtils.ATTR_TARGET, target)
                              : "");



        for (Entry ancestor : parents) {

            if (length > lengthLimit) {
                breadcrumbs.add(0, "...");

                break;
            }
            String name = ancestor.getName();
            if (needToClip && (name.length() > 20)) {
                name = name.substring(0, 19) + "...";
            }
            length += name.length();


            if (target != null) {
                String url = getEntryManager().getEntryUrl(request, ancestor);
                breadcrumbs.add(0, HtmlUtils.href(url,
                /*request.entryUrl(getRepository().URL_ENTRY_SHOW, ancestor), */
                name, targetAttr));
            } else {
                String url = HtmlUtils.url(requestUrl.toString(),
                                           ARG_ENTRYID, ancestor.getId());
                breadcrumbs.add(0, HtmlUtils.href(url, name));
            }
        }
        String lastLink = null;


        if (target != null) {
            lastLink = HtmlUtils.href(getEntryManager().getEntryUrl(request,
                    entry), entry.getLabel(), targetAttr);

        } else {
            if (requestUrl == null) {
                lastLink = getEntryManager().getTooltipLink(request, entry,
                        entry.getLabel(), null);
            } else {
                String url = HtmlUtils.url(requestUrl.toString(),
                                           ARG_ENTRYID, entry.getId());
                lastLink = HtmlUtils.href(url, entry.getLabel());
            }
        }

        if (lastLink != null) {
            breadcrumbs.add(lastLink);
        }

        return StringUtil.join(BREADCRUMB_SEPARATOR_PAD, breadcrumbs);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param title _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getEntryHeader(Request request, Entry entry,
                                 Appendable title)
            throws Exception {

        if (entry == null) {
            return BLANK;
        }
        if (request == null) {
            request = getRepository().getTmpRequest(entry);
        }


        Entry     root      = request.getRootEntry();
        PageStyle pageStyle = request.getPageStyle(entry);


        Entry parent = getEntryManager().findGroup(request,
                           entry.getParentEntryId());
        OutputType   output       = OutputHandler.OUTPUT_HTML;
        int          length       = 0;


        HtmlTemplate htmlTemplate = getPageHandler().getTemplate(request);
        List<Link> linkList = getEntryManager().getEntryLinks(request, entry);

        String headerLabel =
            HtmlUtils.href(getEntryManager().getEntryUrl(request, entry),
                           HtmlUtils.img(getIconUrl(request, entry)) + " "
                           + getEntryDisplayName(entry));

        String links =
            getEntryManager().getEntryActionsTable(request, entry,
                OutputType.TYPE_FILE | OutputType.TYPE_EDIT
                | OutputType.TYPE_VIEW | OutputType.TYPE_OTHER, linkList,
                    false, headerLabel);


        StringBuilder popup = new StringBuilder();
        String menuLinkImg = HtmlUtils.div(
                                 "",
                                 HtmlUtils.cssClass(
                                     "ramadda-breadcrumbs-menu-button"));
        String menuLink = getPageHandler().makePopupLink(menuLinkImg, links,
                              "", true, false, popup);


        List<Entry> parents  = new ArrayList<Entry>();
        boolean     seenRoot = entry.getId().equals(root.getId());
        //crumbs
        //        parents.add(entry);

        while ( !seenRoot && (parent != null)) {
            seenRoot = parent.getId().equals(root.getId());
            parents.add(parent);
            parent = getEntryManager().findGroup(request,
                    parent.getParentEntryId());
        }

        List<String> titleList = new ArrayList();
        List<String> breadcrumbs = makeBreadcrumbList(request, parents,
                                       titleList);

        boolean showBreadcrumbs = pageStyle.getShowBreadcrumbs(entry);
        boolean showMenu        = pageStyle.getShowMenubar(entry);
        boolean showToolbar     = pageStyle.getShowToolbar(entry);


        //FOR NOW:
        showToolbar = false;

        String  toolbar = showToolbar
                          ? getEntryToolbar(request, entry)
                          : "";

        boolean doTable = true;
        String  header  = "";
        if (showBreadcrumbs) {
            StringBuilder sb = new StringBuilder();
            HtmlUtils.open(sb, "div", "class", "ramadda-breadcrumbs");
            if (doTable) {
                sb.append(
                    "<table border=0 width=100% cellspacing=0 cellpadding=0><tr valign=center>");
            }
            if (showMenu) {
                if (doTable) {
                    sb.append("<td valign=center width=1%>");
                }
                HtmlUtils.div(sb, menuLink,
                              HtmlUtils.cssClass("ramadda-breadcrumbs-menu"));
                if (doTable) {
                    sb.append("</td>");
                }
            }

            if (doTable) {
                sb.append("<td>");
            }
            makeBreadcrumbs(request, breadcrumbs, sb);
            if (doTable) {
                sb.append("</td>");
                sb.append("<td align=right width=100>");
            }
            sb.append(toolbar);
            if (doTable) {
                sb.append("</td></tr></table>");
            }
            sb.append("</div>");
            sb.append(popup);
            header = sb.toString();
        } else {
            if ( !request.isAnonymous()) {
                header = menuLink + popup;
            }
        }
        title.append(StringUtil.join(BREADCRUMB_SEPARATOR_PAD, titleList));

        return header;


    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getEntryToolbar(Request request, Entry entry)
            throws Exception {
        List<Link>    links  = getEntryManager().getEntryLinks(request,
                                   entry);
        StringBuilder sb     = new StringBuilder();

        OutputType    output = HtmlOutputHandler.OUTPUT_INFO;
        String treeLink = HtmlUtils.href(
                              request.entryUrl(
                                  getRepository().URL_ENTRY_SHOW, entry,
                                  ARG_OUTPUT,
                                  output.toString()), getIconImage(
                                      output.getIcon(), "title",
                                      output.getLabel()));

        sb.append(treeLink);
        for (Link link : links) {
            if (link.isType(OutputType.TYPE_TOOLBAR)) {
                String href = HtmlUtils.href(link.getUrl(),
                                             getIconImage(link.getIcon(),
                                                 "title", link.getLabel()));
                sb.append(HtmlUtils.inset(href, 0, 3, 0, 0));
            }
        }

        return sb.toString();
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getEntryMenubar(Request request, Entry entry)
            throws Exception {


        List<Link> links = getEntryManager().getEntryLinks(request, entry);

        String entryMenu = getEntryManager().getEntryActionsTable(request,
                               entry, OutputType.TYPE_FILE, links, true,
                               null);
        String editMenu = getEntryManager().getEntryActionsTable(request,
                              entry, OutputType.TYPE_EDIT, links, true, null);
        String exportMenu = getEntryManager().getEntryActionsTable(request,
                                entry, OutputType.TYPE_FEEDS, links, true,
                                null);
        String viewMenu = getEntryManager().getEntryActionsTable(request,
                              entry, OutputType.TYPE_VIEW, links, true, null);

        String       categoryMenu = null;
        List<String> menuItems    = new ArrayList<String>();
        String sep =
            HtmlUtils.div("",
                          HtmlUtils.cssClass(CSS_CLASS_MENUBUTTON_SEPARATOR));


        String menuClass = HtmlUtils.cssClass(CSS_CLASS_MENUBUTTON);
        for (Link link : links) {
            if (link.isType(OutputType.TYPE_OTHER)) {
                categoryMenu =
                    getEntryManager().getEntryActionsTable(request, entry,
                        OutputType.TYPE_OTHER, links);
                String categoryName = link.getOutputType().getCategory();
                //HtmlUtils.span(msg(categoryName), menuClass),
                categoryMenu =
                    getPageHandler().makePopupLink(msg(categoryName),
                        categoryMenu.toString(), menuClass, false, true);

                break;
            }
        }



        PageStyle pageStyle = request.getPageStyle(entry);

        /*
          puts these here so we can extract the file names for the .pack files
          msg("File")
          msg("Edit")
          msg("View")
          msg("Connect")
          msg("Data")
         */

        if (pageStyle.okToShowMenu(entry, PageStyle.MENU_FILE)
                && (entryMenu != null)) {
            if (menuItems.size() > 0) {
                menuItems.add(sep);
            }
            String menuName = "File";
            //Do we really want to change the name of the menu based on the entry type?
            if (entry.isGroup()) {
                //                menuName="Folder";
            }
            //HtmlUtils.span(msg(menuName), menuClass), 
            menuItems.add(getPageHandler().makePopupLink(msg(menuName),
                    entryMenu, menuClass, false, true));

        }

        if (pageStyle.okToShowMenu(entry, PageStyle.MENU_EDIT)
                && (editMenu != null)) {
            if (menuItems.size() > 0) {
                menuItems.add(sep);
            }
            menuItems.add(getPageHandler().makePopupLink(msg("Edit"),
                    editMenu, menuClass, false, true));
        }

        if (pageStyle.okToShowMenu(entry, PageStyle.MENU_FEEDS)
                && (exportMenu != null)) {
            if (menuItems.size() > 0) {
                menuItems.add(sep);
            }
            menuItems.add(getPageHandler().makePopupLink(msg("Links"),
                    exportMenu, menuClass, false, true));
        }

        if (pageStyle.okToShowMenu(entry, PageStyle.MENU_VIEW)
                && (viewMenu != null)) {
            if (menuItems.size() > 0) {
                menuItems.add(sep);
            }
            menuItems.add(getPageHandler().makePopupLink(msg("View"),
                    viewMenu, menuClass, false, true));
        }

        if (pageStyle.okToShowMenu(entry, PageStyle.MENU_OTHER)
                && (categoryMenu != null)) {
            if (menuItems.size() > 0) {
                menuItems.add(sep);
            }
            menuItems.add(categoryMenu);
        }

        String leftTable;
        leftTable = HtmlUtils.table(
            HtmlUtils.row(
                HtmlUtils.cols(Misc.listToStringArray(menuItems)),
                " cellpadding=0 cellspacing=0 border=0 "));

        return leftTable;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param parents _more_
     * @param titleList _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<String> makeBreadcrumbList(Request request,
                                           List<Entry> parents,
                                           List<String> titleList)
            throws Exception {
        List<String> breadcrumbs = new ArrayList<String>();

        for (Entry ancestor : parents) {
            String show =
                (String) ancestor.getTransientProperty("showinbreadcrumbs");
            if ((show != null) && show.equals("false")) {
                break;
            }
            String name = getEntryDisplayName(ancestor);
            String linkLabel;
            linkLabel = name;
            if (titleList != null) {
                titleList.add(0, name);
            }


            String url  = getEntryManager().getEntryUrl(request, ancestor);
            String link = HtmlUtils.href(url, linkLabel);
            breadcrumbs.add(0, link);
        }

        return breadcrumbs;

    }



    /**
     * _more_
     *
     * @param request the request
     * @param breadcrumbs list of crumbs
     * @param sb buffer
     *
     *
     * @throws Exception _more_
     */
    public void makeBreadcrumbs(Request request, List<String> breadcrumbs,
                                Appendable sb)
            throws Exception {
        String id = HtmlUtils.getUniqueId("crumbs_");
        HtmlUtils.open(sb, "div", "class", "ramadda-breadcrumbs-list");
        HtmlUtils.open(sb, "div", "class", "breadCrumbHolder module");
        HtmlUtils.open(sb, "div", "id", id, "class", "breadCrumb module");
        HtmlUtils.open(sb, "ul");
        for (Object crumb : breadcrumbs) {
            HtmlUtils.tag(sb, "li", "", crumb.toString());
        }
        sb.append("</ul></div></div></div>");
        HtmlUtils.script(sb,
                         JQuery.ready("HtmlUtil.makeBreadcrumbs('" + id
                                      + "');"));
    }

    /** _more_ */
    private Image remoteImage;


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void loadResources() throws Exception {
        loadLanguagePacks();
        loadMapRegions();
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getIconUrlInner(Request request, Entry entry)
            throws Exception {

        if (entry.getIcon() != null) {
            return getIconUrl(entry.getIcon());
        }
        if (getEntryManager().isAnonymousUpload(entry)) {
            return getIconUrl(ICON_ENTRY_UPLOAD);
        }
        if (request.defined(ARG_ICON)) {
            return getIconUrl(request.getString(ARG_ICON, ""));
        }


        return entry.getTypeHandler().getEntryIconUrl(request, entry);
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getIconUrl(Request request, Entry entry) throws Exception {
        String iconPath = getIconUrlInner(request, entry);

        if (iconPath == null) {
            return getIconUrl(ICON_BLANK);
            //            return null;
        }

        return iconPath;
    }

    /**
     * Function to get share button, ratings and also Numbers of Comments and comments icon getComments(request, entry);
     * This will only be painted if there is a menubar.
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return String with the HTML
     *
     * @throws Exception _more_
     */

    public String entryFooter(Request request, Entry entry) throws Exception {

        if (entry == null) {
            entry = request.getRootEntry();
        }
        StringBuilder sb = new StringBuilder();

        String entryUrl =
            HtmlUtils.url(
                request.getAbsoluteUrl(getRepository().URL_ENTRY_SHOW),
                ARG_ENTRYID, entry.getId());



        //Table to enclose this toolbar
        sb.append("<table width=\"100%\"><tr><td>");

        // Comments
        List<Comment> comments = entry.getComments();
        if (comments != null) {
            Link link =
                new Link(request.entryUrl(getRepository().URL_COMMENTS_SHOW,
                                          entry), ICON_COMMENTS,
                                              "Add/View Comments",
                                              OutputType.TYPE_TOOLBAR);

            String href = HtmlUtils.href(link.getUrl(),
                                         "Comments:(" + comments.size() + ")"
                                         + getIconImage(link.getIcon(),
                                             "title", link.getLabel()));

            sb.append(href);
            sb.append("</td><td>");
        }

        /*
          Don't include the sharing from addthis.com for now since I think theyre doing tracking
        String title = getEntryManager().getEntryDisplayName(entry);
        String share =
            "<script type=\"text/javascript\">"
            + "var addthis_disable_flash=\"true\"; addthis_pub=\"jeffmc\";</script>"
            + "<a href=\"http://www.addthis.com/bookmark.php?v=20\" "
            + "onclick=\"return addthis_open(this, '', '" + entryUrl + "', '"
            + title
            + "')\"><img src=\"http://s7.addthis.com/static/btn/lg-share-en.gif\" width=\"125\" height=\"16\" alt=\"Bookmark and Share\" style=\"border:0\"/></a><script type=\"text/javascript\" src=\"http://s7.addthis.com/js/200/addthis_widget.js\"></script>";


        sb.append(share);
        */

        sb.append("</td><td>");


        // Ratings 
        boolean doRatings = getRepository().getProperty(PROP_RATINGS_ENABLE,
                                true);
        if (doRatings) {
            String link = request.makeUrl(getRepository().URL_COMMENTS_SHOW,
                                          ARG_ENTRYID, entry.getId());
            String ratings = HtmlUtils.div(
                                 "",
                                 HtmlUtils.cssClass("js-kit-rating")
                                 + HtmlUtils.attr(
                                     HtmlUtils.ATTR_TITLE,
                                     entry.getFullName()) + HtmlUtils.attr(
                                         "permalink",
                                         link)) + HtmlUtils.importJS(
                                             "http://js-kit.com/ratings.js");

            sb.append(
                HtmlUtils.table(
                    HtmlUtils.row(
                        HtmlUtils.col(
                            ratings,
                            HtmlUtils.attr(
                                HtmlUtils.ATTR_ALIGN,
                                HtmlUtils.VALUE_RIGHT)), HtmlUtils.attr(
                                    HtmlUtils.ATTR_VALIGN,
                                    HtmlUtils.VALUE_TOP)), HtmlUtils.attr(
                                        HtmlUtils.ATTR_WIDTH, "100%")));
        } else {
            sb.append(HtmlUtils.p());
        }


        sb.append("</td></tr></table>");

        return sb.toString();
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getCommentHtml(Request request, Entry entry)
            throws Exception {
        boolean canEdit = getAccessManager().canDoAction(request, entry,
                              Permission.ACTION_EDIT);
        boolean canComment = getAccessManager().canDoAction(request, entry,
                                 Permission.ACTION_COMMENT);

        StringBuilder sb = new StringBuilder();
        List<Comment> comments =
            getRepository().getCommentManager().getComments(request, entry);

        if (canComment) {
            sb.append(
                HtmlUtils.href(
                    request.entryUrl(
                        getRepository().URL_COMMENTS_ADD,
                        entry), "Add Comment"));
        }


        if (comments.size() == 0) {
            sb.append("<br>");
            sb.append(msg("No comments"));
        }
        //        sb.append("<table>");
        int rowNum = 1;
        for (Comment comment : comments) {
            //            sb.append(HtmlUtils.formEntry(BLANK, HtmlUtils.hr()));
            //TODO: Check for access
            String deleteLink = ( !canEdit
                                  ? ""
                                  : HtmlUtils.href(request.makeUrl(getRepository().URL_COMMENTS_EDIT,
                                      ARG_DELETE, "true", ARG_ENTRYID,
                                      entry.getId(), ARG_AUTHTOKEN,
                                      getRepository().getAuthToken(request.getSessionId()),
                                      ARG_COMMENT_ID,
                                      comment.getId()), getIconImage(ICON_DELETE,
                                          "title", msg("Delete comment"))));
            if (canEdit) {
                //                sb.append(HtmlUtils.formEntry(BLANK, deleteLink));
            }
            //            sb.append(HtmlUtils.formEntry("Subject:", comment.getSubject()));


            String theClass = HtmlUtils.cssClass("listrow" + rowNum);
            theClass = HtmlUtils.cssClass(CSS_CLASS_COMMENT_BLOCK);
            rowNum++;
            if (rowNum > 2) {
                rowNum = 1;
            }
            StringBuilder content = new StringBuilder();
            String byLine = HtmlUtils.span(
                                "Posted by " + comment.getUser().getLabel(),
                                HtmlUtils.cssClass(
                                    CSS_CLASS_COMMENT_COMMENTER)) + " @ "
                                        + HtmlUtils.span(
                                            getDateHandler().formatDate(
                                                request,
                                                comment.getDate()), HtmlUtils.cssClass(
                                                    CSS_CLASS_COMMENT_DATE)) + HtmlUtils.space(
                                                        1) + deleteLink;
            content.append(
                HtmlUtils.open(
                    HtmlUtils.TAG_DIV,
                    HtmlUtils.cssClass(CSS_CLASS_COMMENT_INNER)));
            content.append(comment.getComment());
            content.append(HtmlUtils.br());
            content.append(byLine);
            content.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
            sb.append(HtmlUtils
                .div(HtmlUtils
                    .makeShowHideBlock(HtmlUtils
                        .span(comment.getSubject(), HtmlUtils
                            .cssClass(CSS_CLASS_COMMENT_SUBJECT)), content
                                .toString(), true, ""), theClass));
        }

        return sb.toString();
    }



    /**
     * _more_
     */
    @Override
    public void clearCache() {
        super.clearCache();
        templateJavascriptContent = null;
        htmlTemplates             = null;
        mobileTemplate            = null;
        defaultTemplate           = null;
        typeToWikiTemplate        = new Hashtable<String, String>();
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param templateType _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getWikiTemplate(Request request, Entry entry,
                                  String templateType)
            throws Exception {
        if (entry.isDummy()) {
            return null;
        }
        String entryType = entry.getTypeHandler().getType();
        String key       = entryType + "." + templateType;
        String wiki      = typeToWikiTemplate.get(key);
        if (wiki != null) {
            return wiki;
        }

        String propertyPrefix = "ramadda.wikitemplate." + templateType + ".";
        String property = getRepository().getProperty(propertyPrefix
                              + entryType, null);
        if (property != null) {
            wiki = getRepository().getResource(property);
        }
        if (wiki == null) {
            String tmp = propertyPrefix + (entry.isGroup()
                                           ? "folder"
                                           : "file");
            wiki = getRepository().getResource(
                getRepository().getProperty(tmp, ""));
        }
        if (wiki != null) {
            typeToWikiTemplate.put(key, wiki);
        }

        return wiki;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param cb _more_
     */
    public void doTableLayout(Request request, Appendable sb,
                              CategoryBuffer cb) {

        try {

            sb.append("<table width=100%><tr valign=top>");

            int colCnt = 0;
            for (String cat : cb.getCategories()) {
                String content = cb.get(cat).toString();
                if (content.length() == 0) {
                    continue;
                }
                colCnt++;
                if (colCnt > 4) {
                    sb.append("</tr><tr valign=top>");
                    sb.append("<td colspan=4><hr></td>");
                    sb.append("</tr><tr valign=top>");
                    colCnt = 1;
                }

                sb.append("<td width='25%'>");
                sb.append(HtmlUtils.b(msg(cat)));
                sb.append(
                    "<div style=\"solid black; max-height: 150px; overflow-y: auto\";>");
                sb.append("<ul>");
                sb.append(content);
                sb.append("</ul>");
                sb.append("</div>");
                sb.append("</td>");

            }
            sb.append("</table>");
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getCreatedDisplayMode() {
        return createdDisplayMode;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean showEntryTableCreateDate() {
        return showCreateDate;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private List<MapLayer> getMapLayers() {
        if (mapLayers == null) {
            List<MapLayer> tmp = new ArrayList<MapLayer>();
            for (String base :
                    StringUtil.split(
                        getRepository().getProperty(
                            "ramadda.map.extras", ""), ",", true, true)) {
                tmp.addAll(MapLayer.makeLayers(getRepository(), base));
            }
            mapLayers = tmp;
        }

        return mapLayers;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param mapInfo _more_
     */
    public void addToMap(Request request, MapInfo mapInfo) {
        //        if(mapInfo.forSelection()) return;
        //http://wms.alaskamapped.org/extras?

        List<String> titles = new ArrayList<String>();
        List<String> tabs   = new ArrayList<String>();
        for (MapLayer mapLayer : getMapLayers()) {
            if (Utils.stringDefined(mapLayer.getLegendLabel())) {
                titles.add(mapLayer.getLegendLabel());
                StringBuffer sb = new StringBuffer(mapLayer.getLegendText());
                if (Utils.stringDefined(mapLayer.getLegendImage())) {
                    sb.append(HtmlUtils.img(mapLayer.getLegendImage()));
                }
                tabs.add(HtmlUtils.div(sb.toString(),
                                       HtmlUtils.cssClass("map-legend-div")));
            }
            mapLayer.addToMap(request, mapInfo);
        }

        StringBuffer rightSide = new StringBuffer();
        rightSide.append("<b>Legends</b><br>");
        rightSide.append(OutputHandler.makeTabs(titles, tabs, true));

        mapInfo.addRightSide(
            getPageHandler().makeStickyPopup(
                HtmlUtils.img(
                    getRepository().getFileUrl(
                        "/icons/map_go.png")), rightSide.toString(), null));
        //        mapInfo.addRightSide(HtmlUtils.makeShowHideBlock("", rightSide.toString(),false));
        //        mapInfo.addRightSide(HtmlUtils.makeShowHideBlock("", rightSide.toString(),false));

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param typeHandler _more_
     * @param includeNonFiles _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String makeFileTypeSelector(Request request,
                                       TypeHandler typeHandler,
                                       boolean includeNonFiles)
            throws Exception {
        List<HtmlUtils.Selector> items =
            getEntryManager().getTypeHandlerSelectors(request, true,
                includeNonFiles, null);

        HtmlUtils.Selector selector = new HtmlUtils.Selector(
                                          "Find match",
                                          TypeHandler.TYPE_FINDMATCH,
                                          getRepository().getIconUrl(
                                              "/icons/blank.gif"), 0, 0,
                                                  false);
        selector.setAttr(" style=\"padding:6px;\" ");
        items.add(0, selector);
        String selected = (typeHandler != null)
                          ? typeHandler.getType()
                          : "";
        if (true) {
            return HtmlUtils.select(ARG_TYPE, items, selected);
        }

        return repository.makeTypeSelect(items, request, false, selected,
                                         false, null);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param title _more_
     *
     * @throws Exception _more_
     */
    public void entrySectionOpen(Request request, Entry entry, Appendable sb,
                                 String title)
            throws Exception {
        entrySectionOpen(request, entry, sb, title, false);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param title _more_
     * @param text _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result makeEntryHeaderResult(Request request, Entry entry,
                                        String title, String text)
            throws Exception {
        Appendable sb = new StringBuilder();
        entrySectionOpen(request, entry, sb, title, false);
        sb.append(text);
        entrySectionClose(request, entry, sb);

        return new Result("", sb);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param title _more_
     * @param showLine _more_
     *
     * @throws Exception _more_
     */
    public void sectionOpen(Request request, Appendable sb, String title,
                            boolean showLine)
            throws Exception {
        sb.append(HtmlUtils.sectionOpen(null, showLine));
        if (title != null) {
            HtmlUtils.sectionTitle(sb, title);
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void sectionClose(Request request, Appendable sb)
            throws Exception {
        sb.append(HtmlUtils.sectionClose());
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param title _more_
     * @param showLine _more_
     *
     * @throws Exception _more_
     */
    public void entrySectionOpen(Request request, Entry entry, Appendable sb,
                                 String title, boolean showLine)
            throws Exception {
        sb.append(HtmlUtils.sectionOpen(null, showLine));
        if (entry != null) {
            String label = entry.getTypeHandler().getEntryName(entry);
            label = HtmlUtils.href(getEntryManager().getEntryUrl(request,
                    entry), label);
            HtmlUtils.sectionTitle(sb, label);
            if (Utils.stringDefined(title)) {
                sb.append(
                    HtmlUtils.div(
                        HtmlUtils.div(
                            msg(title),
                            HtmlUtils.cssClass(
                                "ramadda-heading")), HtmlUtils.cssClass(
                                    "ramadda-heading-outer")));

            }
        } else {
            HtmlUtils.sectionTitle(sb, msg(title));
        }

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void entrySectionClose(Request request, Entry entry, Appendable sb)
            throws Exception {
        sb.append(HtmlUtils.sectionClose());
    }


    /**
     *
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addGoogleJSImport(Request request, Appendable sb)
            throws Exception {
        if (request.getExtraProperty("googlejsapi") == null) {
            request.putExtraProperty("googlejsapi", "added");
            sb.append(
                HtmlUtils.importJS(
                    getRepository().getProperty("ramadda.google.js", "")));
        }
    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String dummy =
            "big long string big long string big long string big long string big long string big long string big long string big long string big long string big long string big long string big long string big long string big long string big long string big long string ";

        /*
Time:499 cnt:1000
Time:1129 cnt:2000
Time:2534 cnt:3000
Time:4517 cnt:4000
Time:7226 cnt:5000
Time:10535 cnt:6000
Time:14625 cnt:7000
        */

        for (int j = 1; j < 10; j++) {
            StringBuffer s = new StringBuffer();
            for (int i = 0; i < j * 1000; i++) {
                s.append(dummy);
                String msg = "msg " + i;
                s.append(MSG_PREFIX + msg + MSG_SUFFIX);
            }
            s.append(dummy);
            long   t1 = System.currentTimeMillis();
            String r1 = replaceMsgOld(s.toString(), null);
            long   t2 = System.currentTimeMillis();
            String r2 = replaceMsgNew(s.toString(), null);
            long   t3 = System.currentTimeMillis();
            if ( !r1.equals(r2)) {
                System.err.println("bad:" + r1.length() + "  r2:"
                                   + r2.length() + " " + r1.equals(r2));
            }
            System.err.println(" cnt:" + (j * 1000) + " " + (t2 - t1) + " "
                               + (t3 - t2));
        }
    }

    /**
     * _more_
     *
     * @param s _more_
     * @param map _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private static String replaceMsgOld(String s, Properties map)
            throws Exception {
        boolean debug = s.length() > 100000;
        String  tmps  = s;
        if (debug) {
            //            System.err.println ("Translate - s.length= " + s.length());
        }


        StringBuilder stripped     = new StringBuilder();
        int           prefixLength = MSG_PREFIX.length();
        int           suffixLength = MSG_PREFIX.length();
        //        System.out.println(s);
        int transCnt = 0;
        while (s.length() > 0) {
            String tmp  = s;
            int    idx1 = s.indexOf(MSG_PREFIX);
            if (idx1 < 0) {
                stripped.append(s);

                break;
            }
            String text = s.substring(0, idx1);
            if (text.length() > 0) {
                stripped.append(text);
            }
            s = s.substring(idx1 + 1);

            int idx2 = s.indexOf(MSG_SUFFIX);
            if (idx2 < 0) {
                //Should never happen
                throw new IllegalArgumentException(
                    "No closing message suffix:" + s);
            }
            String key   = s.substring(prefixLength - 1, idx2);
            String value = null;
            if (value == null) {
                value = key;
            }
            stripped.append(value);
            s = s.substring(idx2 + suffixLength);
            transCnt++;
        }
        if (debug) {
            //            System.err.println ("Translate:  " + tmps.length() +" " + transCnt);
        }

        return stripped.toString();
    }

    /** _more_ */
    private static final String CDN =
        "https://cdn.jsdelivr.net/gh/geodesystems/ramadda/src/org/ramadda/repository/htdocs";


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public String applyBaseMacros(String s) {

        String dotmini = getRepository().getMinifiedOk()
                         ? ".min"
                         : "";
        String mini    = getRepository().getMinifiedOk()
                         ? "min/"
                         : "";
        //        System.err.println(mini +" " + getRepository().getMinifiedOk());
        String path;
        if (getRepository().getCdnOk()) {
            path = CDN;
        } else {
            path = getRepository().getUrlBase() + "/"
                   + RepositoryUtil.getHtdocsVersion();
        }


        String htdocsBase = makeHtdocsUrl("");

        return s.replace("${htdocs}", htdocsBase).replace(
            "${cdnpath}", path).replace(
            "${root}", getRepository().getUrlBase()).replace(
            "${baseentry}", getEntryManager().getRootEntry().getId()).replace(
            "${min}", mini).replace("${dotmin}", dotmini);
    }

    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     */
    public String getCdnPath(String path) {
        if (getRepository().getCdnOk()) {
            return CDN + path;
        } else {
            return getRepository().getHtdocsUrl(path);
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param selectId _more_
     * @param sb _more_
     * @param label _more_
     * @param extra _more_
     *
     * @throws Exception _more_
     */
    public void addEntrySelect(Request request, Entry entry, String selectId,
                               Appendable sb, String label, String extra)
            throws Exception {
        String baseSelect = OutputHandler.getGroupSelect(request, selectId);


        sb.append(HtmlUtils.hidden(selectId + "_hidden", ((entry != null)
                ? entry.getId()
                : ""), HtmlUtils.id(selectId + "_hidden")));
        sb.append(HtmlUtils.formEntry(msgLabel(label),
                                      HtmlUtils.disabledInput(selectId,
                                          ((entry != null)
                                           ? entry.getFullName()
                                           : ""), HtmlUtils.id(selectId)
                                           + HtmlUtils.SIZE_60
                                           + ((entry == null)
                ? HtmlUtils.cssClass(CSS_CLASS_REQUIRED_DISABLED)
                : "")) + baseSelect + extra));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param args _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getEntryHref(Request request, Entry entry, String... args)
            throws Exception {
        /*
        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry,
                                              ContentMetadataHandler.TYPE_ALIAS,false);
        String url;
        if(metadataList.size()>0) {
            url = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry);
        } else {
            url = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry);
        }
        */
        String url = getEntryManager().getEntryUrl(request, entry);

        return HtmlUtils.href(url, (args.length > 0)
                                   ? args[0]
                                   : entry.getLabel());
    }

}
