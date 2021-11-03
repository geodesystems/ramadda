/**
* Copyright (c) 2008-2021 Geode Systems LLC
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
import org.ramadda.util.NamedValue;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.JQuery;
import org.ramadda.util.MapRegion;
import org.ramadda.util.Utils;

import java.util.Comparator;


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


    public static final String IMPORTS_BEGIN = "<!--imports-->";
    public static final String IMPORTS_END = "<!--end imports-->";



    /** _more_ */
    private static boolean debugTemplates = false;

    /** _more_ */
    public static final String DEFAULT_TEMPLATE = "fixedmapheader";

    /** _more_ */
    public static final String REGISTER_MESSAGE =
        "<div class=\"ramadda-register-outer\"><div class=\"ramadda-register\">Powered by <a href=\"https://geodesystems.com\">Geode Systems RAMADDA</a></div></div>";



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

    public static final String MACRO_IMPORTS= "imports";


    /** _more_          */
    private String webImports;

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
    private boolean showJsonLd;

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



    String searchImg = HU.faIcon("fa-search", "title",
				 "Search", "class",
				 "ramadda-user-menu-image", "id",
				 "searchlink");

    String popupImage;


    /**
     * _more_
     *
     * @param repository _more_
     */
    public PageHandler(Repository repository) {
        super(repository);
        popupImage =
            HU.faIcon("fa-cog", "title",
		      "Login, user settings, help", "class",
		      "ramadda-user-menu-image");
	popupImage = HtmlUtils.div(popupImage, HtmlUtils.cssClass("ramadda-popup-link"));
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

        showJsonLd = getRepository().getProperty("ramadda.showjsonld", false);
        showSearch = getRepository().getProperty("ramadda.showsearch", true);
        createdDisplayMode =
            getRepository().getProperty(PROP_CREATED_DISPLAY_MODE,
                                        "none").trim();
        footer      = repository.getProperty(PROP_HTML_FOOTER, BLANK);
        myLogoImage = getRepository().getProperty(PROP_LOGO_IMAGE, null);
        cacheTemplates =
            getRepository().getProperty("ramadda.cachetemplates", true);

        initWebResources();
    }


    /**
     * _more_
     */
    private void initWebResources() {
        try {
            webImports = "";
            String cssImports = "";
            List<String> cssFiles =
                Utils.split(
                    getStorageManager().readSystemResource(
                        "/org/ramadda/repository/resources/web/cssimports.html"), "\n", true, true);
            String jsImports = "";
            List<String> jsFiles =
                Utils.split(
                    getStorageManager().readSystemResource(
                        "/org/ramadda/repository/resources/web/jsimports.html"), "\n", true, true);

            for (String file : cssFiles) {
                if (file.startsWith("#")) {
                    continue;
                }
                cssImports += HU.cssLink("${root}" + file).trim()
                              + "\n";
            }
            for (String file : jsFiles) {
                if (file.startsWith("#")) {
                    continue;
                }
                jsImports += HU.importJS("${root}" + file).trim()
                             + "\n";
            }
            webImports = applyBaseMacros(cssImports.trim() + "\n"
                                         + jsImports.trim() + "\n");
	    webImports = IMPORTS_BEGIN + "\n" + webImports +"\n" + IMPORTS_END+"\n";
	    


        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
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
	String html = decorateResult(request, result,true,true);
	if(html!=null) {
	    result.setContent(html);
	}
    }


    public String decorateResult(Request request, Result result, boolean prefix, boolean suffix)
            throws Exception {	
	boolean fullTemplate = prefix && suffix;
	//	Runtime.getRuntime().gc();
	double mem1 = Utils.getUsedMemory();

        long t0 = System.currentTimeMillis();
        if ( !request.get(ARG_DECORATE, true)) {
            return null;
        }
        Repository   repository   = getRepository();
        Entry        currentEntry = getSessionManager().getLastEntry(request);
        HtmlTemplate parentTemplate = getTemplate(request, currentEntry);
        if (request.isMobile() && !request.defined(ARG_TEMPLATE)) {
            if ( !parentTemplate.getTemplateProperty("mobile", false)) {
                parentTemplate = getMobileTemplate();
            }
        }
        HtmlTemplate htmlTemplate = parentTemplate;

	if(!fullTemplate) {
	    if(prefix) {
		htmlTemplate = htmlTemplate.getPrefix();
	    } else {
		htmlTemplate = htmlTemplate.getSuffix();
	    }
	}

	String template = htmlTemplate.getTemplate();
        String systemMessage =   getRepository().getSystemMessage(request);
        String entryHeader = (String) result.getProperty(PROP_ENTRY_HEADER,
							 (String)null);
        String entryFooter = (String) result.getProperty(PROP_ENTRY_FOOTER,
							 (String)null);
        String entryBreadcrumbs =
            (String) result.getProperty(PROP_ENTRY_BREADCRUMBS, (String)null);
        String        header   = entryHeader;

        Appendable contents = new StringBuilder();

	if(prefix) {
	    if (Utils.stringDefined(systemMessage)) {
		HU.div(contents, systemMessage,
		       HU.cssClass("ramadda-system-message"));
	    }
	}

        String       jsContent     = getTemplateJavascriptContent();
	String bottom = null;
	if(fullTemplate || suffix) {
	    contents = Utils.append(contents, result.getStringContent());
	    if (htmlTemplate.hasMacro(MACRO_BOTTOM)) {
		bottom = jsContent;
	    } else {
		contents = Utils.append(contents, jsContent);
	    }
	} 
        String content = contents!=null?contents.toString():null;

	StringBuilder head = new StringBuilder();
	if(request.getHead0()!=null) {
	    head.append(request.getHead0());
	}

	//make the request to base.js be unique every time so the browser does not cache it
	HU.importJS(head, getRepository().getUrlBase()+"/htdocs_v" + (new Date().getTime())+"/base.js");
	head.append(webImports);
	String head2 = request.getHead();
	if(head2!=null) head.append(head2);
        if (request.get("ramadda.showjsonld", true)&& showJsonLd && (currentEntry != null)) {
            head.append(getMetadataManager().getJsonLD(request, currentEntry));
        }
	String imports = head.toString();

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
        List<String> navLinks = getNavLinks(request, userLinkTemplate);
        List<String> userLinks = getUserLinks(request, userLinkTemplate,
					      extra, true);
        allLinks.addAll(navLinks);
        allLinks.addAll(userLinks);


        String menuHtml =
            HU.div(StringUtil.join("", allLinks),
                          HU.cssClass("ramadda-user-menu"));

        if (showSearch) {
	    HU.mouseClickHref(extra, "Utils.searchPopup('searchlink','popupanchor');",
			      searchImg, "");
	    HU.span(extra,"",HU.attrs("id","popupanchor","style","position:relative;"));
            extra.append(HU.SPACE2);
        }

	String theFooter = footer;
        Entry    thisEntry = request.getCurrentEntry();
	if(thisEntry!=null) {
	    theFooter+=HU.script("ramaddaThisEntry='" + thisEntry.getId()+"';\n");
	}

	extra.append(HU.makePopup(null, popupImage, menuHtml, arg("my","right top"),arg("at","left bottom"), arg("animate",false)));
        menuHtml = HU.div(extra.toString(), HU.clazz("ramadda-user-menu"));

        String[] macros = new String[] {
            MACRO_LOGO_URL, logoUrl, MACRO_LOGO_IMAGE, logoImage,
            MACRO_HEADER_IMAGE, getHeaderIcon(), MACRO_HEADER_TITLE,
            pageTitle, MACRO_LINKS, menuHtml, MACRO_REPOSITORY_NAME,
            repository.getRepositoryName(), MACRO_FOOTER, theFooter, MACRO_TITLE,
            result.getTitle(), MACRO_BOTTOM, bottom,
            MACRO_SEARCH_URL, getSearchManager().getSearchUrl(request),
            MACRO_CONTENT, content, MACRO_ENTRY_HEADER, entryHeader,
            MACRO_HEADER, header, MACRO_ENTRY_FOOTER, entryFooter,
            MACRO_ENTRY_BREADCRUMBS, entryBreadcrumbs,  MACRO_IMPORTS, imports,
	    MACRO_HEADFINAL, "", MACRO_ROOT, repository.getUrlBase(), 
        };


        long                      t2     = System.currentTimeMillis();
        String                    html   = template;
        Hashtable<String, String> values = new Hashtable<String, String>();
        for (int i = 0; i < macros.length; i += 2) {
	    if(macros[i+1]!=null) {
		values.put(macros[i], macros[i + 1]);
	    }
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
        List<String> templateToks;
	if(htmlTemplate.getWikify()) {
	    template = getWikiManager().wikifyEntry(request, getEntryManager().getRootEntry(), template,false);
	    templateToks  = htmlTemplate.getToks(template);
	} else {
	    templateToks  = htmlTemplate.getToks();
	} 

	for (int i = 0; i < templateToks.size(); i++) {
            String v = templateToks.get(i);
            //Check if even or odd
            if (2 * (i / 2) == i) {
                sb.append(v);
            } else {
                String macroValue = values.get(v);
                if (macroValue != null) {
                    sb.append(macroValue);
                }
            }
        }
        html = sb.toString();
        html = translate(request, html);

	/*
	double mem2 = Utils.getUsedMemory();
	System.err.println("PageDecorator memory:" +  Utils.decimals(mem2-mem1,1) +" length:" + html.length());
	*/
	return html;
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
								    false, null, false,true);
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
                EntryLink entryLink = getEntryManager().getAjaxLink(request, entry, entry.getLabel(), null,
								    false,null, true,true);
                String link = favoritesWrapper.replace("${link}",
                                  entryLink.toString());
                cartLinks.add("<nobr>" + link + "<nobr>");
            }
            favorites.append(HU.br());
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
            HU.div(sb, "",
                          HU.attrs("id", "ramadda-popupdiv", "class",
                                          "ramadda-popup"));
            HU.div(sb, "",
                          HU.attrs("id", "ramadda-selectdiv", "class",
                                          "ramadda-selectdiv"));
            HU.div(sb, "",
                          HU.attrs("id", "ramadda-floatdiv", "class",
                                          "ramadda-floatdiv"));
            sb.append(HU.script(js.toString()));
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
        List<String>  toks   = Utils.splitMacros(template);
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
	//Don't translate for now
	if(true) return s;

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
        List<String> lines   = Utils.split(content, "\n", true, true);
        Properties   phrases = new Properties();
        String       type    =
            IOUtil.stripExtension(IOUtil.getFileTail(file));
        String       name    = type;
        for (String line : lines) {
            if (line.startsWith("#")) {
                continue;
            }
            List<String> toks = Utils.split(line, "=", true, true);
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
     * @param files _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String concatFiles(List<String> files) throws Exception {
        StringBuilder sb     = new StringBuilder();
        String        prefix = "/org/ramadda/repository/htdocs";
        for (String path : files) {
            if (path.startsWith("#")) {
                continue;
            }
            path = applyBaseMacros(path);
            String css = getStorageManager().readSystemResource(prefix
                             + path);
            sb.append("/* from " + path + "*/\n");
            css = applyBaseMacros(css);
            sb.append(css);
            sb.append("\n\n");
        }

        return sb.toString();
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public synchronized List<HtmlTemplate> getTemplates() {
        try {
            return getTemplatesInner();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private synchronized List<HtmlTemplate> getTemplatesInner()
            throws Exception {

        List<HtmlTemplate> theTemplates = htmlTemplates;
        if ( !cacheTemplates || (theTemplates == null)) {
            String mobileId =
                getRepository().getProperty("ramadda.template.mobile",
                                            (String) null);
            HtmlTemplate theMobileTemplate = null;
            //use locals here in case of race conditions
            HtmlTemplate _defaultTemplate = null;
            HtmlTemplate _mobileTemplate  = null;
            theTemplates = new ArrayList<HtmlTemplate>();
            templateMap  = new Hashtable<String, HtmlTemplate>();

            String defaultId =
                getRepository().getProperty(PROP_HTML_TEMPLATE_DEFAULT,
                                            DEFAULT_TEMPLATE);
            if (debugTemplates) {
                System.err.println("getTemplates defaultId=" + defaultId);
            }
            List<String> templatePaths =
                new ArrayList<String>(
                    getRepository().getPluginManager().getTemplateFiles());
            for (String path :
                    Utils.split(
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
		    //                    resource = resource.replace("${imports}", webImports);
                    HtmlTemplate template = new HtmlTemplate(getRepository(),
							     path, resource);
		    int idx = template.getTemplate().indexOf("${content}");
		    if(idx>=0) {
			template.setPrefix(new HtmlTemplate(template, template.getTemplate().substring(0,idx)));
			template.setSuffix(new HtmlTemplate(template, template.getTemplate().substring(idx+"${content}".length())));
		    }


		    template.setTemplate(applyBaseMacros(template.getTemplate()));
		    //		    System.out.println("p: " + path + " " + template.getId()+ " " + template.getName());
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

                    if (_mobileTemplate == null) {
                        if (mobileId != null) {
                            if (template.getId().equals(mobileId)) {
                                _mobileTemplate = template;
                            }
                        } else if (template.getTemplateProperty("mobile",
                                false)) {
                            //Don't do this for now
                            //                      _mobileTemplate = template;
                        }
                    }
                    if ((theMobileTemplate == null)
                            && template.getId().equals("mobile")) {
                        theMobileTemplate = template;
                    }
                    if (_defaultTemplate == null) {
                        if (defaultId == null) {
                            _defaultTemplate = template;
                            if (debugTemplates) {
                                System.err.println("\tset-1:"
                                        + _defaultTemplate);
                            }
                        } else {
                            if (Misc.equals(defaultId, template.getId())) {
                                _defaultTemplate = template;
                                if (debugTemplates) {
                                    System.err.println("\tset-2:"
                                            + _defaultTemplate);
                                }
                            }
                        }
                        if (mobileId == null) {
                            if (template.getTemplateProperty("mobile",
                                    false)) {
                                _mobileTemplate = template;
                            }
                        }
                    }
                } catch (Exception exc) {
                    getLogManager().logError("loading template" + path, exc);
                    //noop
                }
            }
            if (_mobileTemplate == null) {
                _mobileTemplate = theMobileTemplate;
            }
            if (_defaultTemplate == null) {
                _defaultTemplate = theTemplates.get(0);
                if (debugTemplates) {
                    System.err.println("\tset-3:" + _defaultTemplate);
                }
            }
            if (_mobileTemplate == null) {
                _mobileTemplate = _defaultTemplate;
            }
            //            if (getRepository().getCacheResources()) {
            defaultTemplate = _defaultTemplate;
            mobileTemplate  = _mobileTemplate;
            htmlTemplates   = theTemplates;
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
            Hashtable props = HU.parseHtmlProperties(include);
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

	//Have to hard code these 
	String pre = "/org/ramadda/repository/resources/geo/";
	mapRegionFiles.addAll(Utils.makeList(pre+"mapregions.csv",
					     pre+"countrymapregions.csv",
					     pre+"statesmapregions.csv",
					     pre+"citiesmapregions.csv"));

        HashSet seen = new HashSet();
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
            List<String> lines = Utils.split(contents, "\n", true, true);
            lines.remove(0);
            String group = lines.get(0);
            lines.remove(0);
            for (String line : lines) {
                List<String> toks = Utils.split(line, ",");
                if ((toks.size() != 6) && (toks.size() != 4)) {
                    throw new IllegalArgumentException("Bad map region line:"
                            + line + "\nFile:" + path);
                }


                String name = toks.get(0);
                if (seen.contains(name)) {
                    continue;
                }
                seen.add(name);
                if (toks.size() == 4) {
                    mapRegions.add(new MapRegion(toks.get(1), name, group,
                            Utils.decodeLatLon(toks.get(2)),
                            Utils.decodeLatLon(toks.get(3))));
                } else {
                    mapRegions.add(new MapRegion(toks.get(1), name, group,
                            Utils.decodeLatLon(toks.get(2)),
                            Utils.decodeLatLon(toks.get(3)),
                            Utils.decodeLatLon(toks.get(4)),
                            Utils.decodeLatLon(toks.get(5))));
                }
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
	tfos.sort(new Comparator() {
            public int compare(Object o1, Object o2) {
                String s1 = (String) ((TwoFacedObject)o1).getLabel();
                String s2 = (String) ((TwoFacedObject)o2).getLabel();		
                return s1.compareToIgnoreCase(s2);
	    }});

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
        //this forces the possible reload of the templates
        getTemplates();
        if (request == null) {
            if (debugTemplates) {
                System.err.println("getTemplate-1:" + defaultTemplate);
            }
            return defaultTemplate;
        }
        boolean isMobile = request.isMobile();
        //Check for template=... url arg
        String templateId = request.getHtmlTemplateId();
        if (Utils.stringDefined(templateId)) {
            HtmlTemplate template = templateMap.get(templateId);
            if (template != null) {
                if (debugTemplates) {
                    System.err.println("getTemplate-2:" + template);
                }

                return template;
            }
            templateId = null;
        }

        //Check for metadata template definition
        if (entry != null) {
            try {
                List<Metadata> metadataList =
                    getMetadataManager().findMetadata(request, entry,
                        ContentMetadataHandler.TYPE_TEMPLATE, true);
                if (metadataList != null) {
                    for (Metadata metadata : metadataList) {
                        HtmlTemplate template =
                            templateMap.get(metadata.getAttr1());
                        if (template != null) {
                            request.put(ARG_TEMPLATE, template.getId());
                            if (isMobile) {
                                if (template.getTemplateProperty("mobile",
                                        false)) {
                                    if (debugTemplates) {
                                        System.err.println(
                                            "getTemplate metadata:"
                                            + template);
                                    }

                                    return template;
                                }
                            } else {
                                if (debugTemplates) {
                                    System.err.println(
                                        "getTemplate metadata:" + template);
                                }

                                return template;
                            }
                        }
                    }
                }
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        if (isMobile && (mobileTemplate != null)) {
            request.put(ARG_TEMPLATE, mobileTemplate.getId());
            if (debugTemplates) {
                System.err.println("getTemplate mobile:" + mobileTemplate);
            }

            return mobileTemplate;
        }

        User user = request.getUser();
        if ((templateId == null) && (user != null) && !user.getAnonymous()) {
            templateId = user.getTemplate();
            if (templateId != null) {
                if (debugTemplates) {
                    System.err.println("getTemplate from user:" + templateId);
                }
            }
        }

        if (templateId != null) {
            HtmlTemplate template = templateMap.get(templateId);
            if (template != null) {
                return template;
            }
        }
        if (debugTemplates) {
            System.err.println("getTemplate default:" + defaultTemplate);
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
	//for now no translation
	if(true) return msg;

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

        return Utils.concatString(msg(msg), ":", HU.SPACE);
    }

    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    public static String msgHeader(String h) {
        return HU.div(
            msg(h),
            HU.cssClass(
                "ramadda-page-heading ramadda-page-heading-left" /*CSS_CLASS_HEADING_1*/));
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
        String okButton     = HU.submit("OK", okArg);
        String cancelButton = HU.submit("Cancel",
                                  Constants.ARG_CANCEL);
        String buttons = HU.buttons(okButton, cancelButton);
        fb.append(buttons);
        fb.append(HU.formClose());

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
                labels.add(HU.faIcon("fa-sign-in-alt") + " "
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
            labels.add(HU.faIcon("fa-sign-out-alt") + " "
                       + msg("Logout"));
            tips.add(msg("Logout"));
            String label = user.getLabel().replace(" ", "&nbsp;");
            String userIcon = HU.faIcon("fa-user", "title",
                                  "Settings for " + label, "class",
                                  "ramadda-user-menu-image");

            String settingsUrl =
                request.makeUrl(getRepositoryBase().URL_USER_FORM);

            if (makePopup) {
                prefix.append(
                    HU.href(
                        settingsUrl, userIcon,
                        HU.cssClass("ramadda-user-settings-link")));
                prefix.append(HU.space(2));
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
            labels.add(HU.faIcon("fa-question-circle") + " "
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
        boolean      isAdmin = request==null?false:request.isAdmin();
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
            String clazz = "ramadda-highlightable ramadda-linksheader-off";
            if (type.endsWith(requestUrl.getPath())) {
                onLabel = label;
                clazz   = "ramadda-highlightable ramadda-linksheader-on";
            }
            links.add(HU.span(HU.href(url, label),
                                     HU.cssClass(clazz)));
            //            }
        }
        StringBuilder header = new StringBuilder();
        HU.div(
            header,
            StringUtil.join(
                "<span class=\".ramadda-separator\">|</span>",
                links), HU.cssClass("ramadda-linksheader-links"));
        header.append("\n");
        sb.append(HU.tag(HU.TAG_DIV,
                                HU.cssClass("ramadda-linksheader"),
                                header.toString()));
    }



    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    public String showDialogNote(String h, String...extra) {
        return getDialog(h, extra, ICON_DIALOG_INFO, false);
    }

    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    public String showDialogBlank(String h, String...extra) {
        return getDialog(h, extra, null, false);
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
    public String showDialogWarning(String h, String...extra) {
        return getDialog(h, extra, Constants.ICON_DIALOG_WARNING, false);
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
        return getDialog(h, new String[]{buttons}, Constants.ICON_DIALOG_QUESTION,
			 false);
    }

    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    public String showDialogError(String h, String...extra) {
        return showDialogError(h, true, extra);
    }

    /**
     * _more_
     *
     * @param h _more_
     * @param cleanString _more_
     *
     * @return _more_
     */
    public String showDialogError(String h, boolean cleanString, String...extra) {
        if (h == null) {
            h = "null error";
        }
        if (cleanString) {
            h = getDialogString(h);
        }

        return getDialog(h, extra, Constants.ICON_DIALOG_ERROR, false);
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
        s = HU.entityEncode(s);
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


    public String getDialog(String msg, String []extra, String icon, boolean showClose) {
        msg= msg.replaceAll("\n", "<br>").replaceAll("&#10;", "<br>");

	if(extra!=null && extra.length>0) {
	    String tmp = "";
	    for(String e:extra) {
		tmp+=e;
	    }
	    msg += "<br><hr style='margin-top:4px;margin-bottom:4px;' class=ramadda-thin-hr>" + tmp;
	}


        String html = showClose
                      ? HU.jsLink(
                          HU.onMouseClick("hide('messageblock')"),
                          getIconImage(Constants.ICON_CLOSE))
                      : "&nbsp;";
	String clazz = Misc.equals(icon,Constants.ICON_DIALOG_INFO)?"ramadda-message":Misc.equals(icon,Constants.ICON_DIALOG_ERROR)?"alert-danger":Misc.equals(icon,Constants.ICON_DIALOG_WARNING)?"alert-warning":"ramadda-message";
	//For now just use the message class
	clazz = "ramadda-message";
	String faClazz = Misc.equals(icon,Constants.ICON_DIALOG_INFO)?"":Misc.equals(icon,Constants.ICON_DIALOG_ERROR)?"text-danger":Misc.equals(icon,Constants.ICON_DIALOG_WARNING)?"text-warning":"";	
        StringBuilder sb = new StringBuilder();
        sb.append(HU.open(HU.TAG_DIV, "class",
			  clazz+" ramadda-message-plain ", "id", "messageblock"));
        sb.append("<table width=100%><tr valign=top>");
        if (icon != null) {
            sb.append("<td width=5%><div class=\"ramadda-message-icon\">");
            sb.append(getIconImage(icon+" " + faClazz,"style","font-size:32pt;"));	    
            sb.append("</div></td>");
        }
        sb.append("<td><div class=\"ramadda-message-inner\">");
        sb.append(msg);
        sb.append("</div></td>");
        if (showClose) {
            sb.append("<td><div class=\"ramadda-message-link\">");
            sb.append(html);
            sb.append("</div></td>");
        }
        sb.append("</tr></table>");
        HU.close(sb, HU.TAG_DIV);
        sb.append(HU.br());

        return sb.toString();
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
                      ? HU.jsLink(
                          HU.onMouseClick("hide('messageblock')"),
                          getIconImage(Constants.ICON_CLOSE))
                      : "&nbsp;";

        StringBuilder sb = new StringBuilder();
        sb.append(HU.open(HU.TAG_DIV, "class",
                                 "ramadda-message ", "id", "messageblock"));
        sb.append("<table><tr valign=top>");
        if (icon != null) {
            sb.append("<td><div class=\"ramadda-message-link\">");
            sb.append(getIconImage(icon));
            sb.append("</div></td>");
        }
        sb.append("<td><div class=\"ramadda-message-inner\">");
        sb.append(h);
        sb.append("</div></td>");
        if (showClose) {
            sb.append("<td><div class=\"ramadda-message-link\">");
            sb.append(html);
            sb.append("</div></td>");
        }
        sb.append("</tr></table>");
        HU.close(sb, HU.TAG_DIV);
        sb.append(HU.br());

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
                for (String type : Utils.split(types, ",", true, true)) {
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
                                Utils.split(menus, ",", true, true)) {
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
        return HU.img(getIconUrl(request, entry)) + " "
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
                              ? HU.attr(HU.ATTR_TARGET, target)
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
                breadcrumbs.add(0, HU.href(url,
                /*request.entryUrl(getRepository().URL_ENTRY_SHOW, ancestor), */
                name, targetAttr));
            } else {
                String url = HU.url(requestUrl.toString(),
                                           ARG_ENTRYID, ancestor.getId());
                breadcrumbs.add(0, HU.href(url, name));
            }
        }
        String lastLink = null;


        if (target != null) {
            lastLink = HU.href(getEntryManager().getEntryUrl(request,
                    entry), entry.getLabel(), targetAttr);

        } else {
            if (requestUrl == null) {
                lastLink = getEntryManager().getTooltipLink(request, entry,
                        entry.getLabel(), null);
            } else {
                String url = HU.url(requestUrl.toString(),
                                           ARG_ENTRYID, entry.getId());
                lastLink = HU.href(url, entry.getLabel());
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


        PageStyle    pageStyle    = request.getPageStyle(entry);
        OutputType   output       = OutputHandler.OUTPUT_HTML;
        int          length       = 0;


        HtmlTemplate htmlTemplate = getPageHandler().getTemplate(request);
        List<Link> linkList = getEntryManager().getEntryLinks(request, entry);

        String headerLabel =
            HU.href(getEntryManager().getEntryUrl(request, entry),
                           HU.img(getIconUrl(request, entry)) + " "
                           + getEntryDisplayName(entry));

        String links = getEntryManager().getEntryActionsTable(request, entry,
							      OutputType.TYPE_MENU, 
							      linkList, false,
							      null);


	StringBuilder popup = new StringBuilder();
        String menuLinkImg = HU.div(
				    HU.img("fas fa-caret-down"),
				    HU.attr("title","Entry menu") +
				    HU.cssClass(
                                     "ramadda-breadcrumbs-menu-button"));

        String menuLink = HU.makePopup(popup, menuLinkImg, links,arg("title", headerLabel), arg("header",true));
        List<Entry>  parents = getEntryManager().getParents(request, entry);
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
            HU.open(sb, "div", "class", "ramadda-breadcrumbs");
            if (doTable) {
                sb.append(
                    "<table border=0 width=100% cellspacing=0 cellpadding=0><tr valign=center>");
            }
            if (showMenu) {
                if (doTable) {
                    sb.append("<td valign=center width=1%>");
                }
                HU.div(sb, menuLink,
                              HU.cssClass("ramadda-breadcrumbs-menu"));
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
        String treeLink = HU.href(
                              request.entryUrl(
                                  getRepository().URL_ENTRY_SHOW, entry,
                                  ARG_OUTPUT,
                                  output.toString()), getIconImage(
                                      output.getIcon(), "title",
                                      output.getLabel()));

        sb.append(treeLink);
        for (Link link : links) {
            if (link.isType(OutputType.TYPE_TOOLBAR)) {
                String href = HU.href(link.getUrl(),
                                             getIconImage(link.getIcon(),
                                                 "title", link.getLabel()));
                sb.append(HU.inset(href, 0, 3, 0, 0));
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
            HU.div("",
                          HU.cssClass(CSS_CLASS_MENUBUTTON_SEPARATOR));


	NamedValue linkAttr = arg("linkAttributes", HU.cssClass(CSS_CLASS_MENUBUTTON));
        for (Link link : links) {
            if (link.isType(OutputType.TYPE_OTHER)) {
                categoryMenu =
                    getEntryManager().getEntryActionsTable(request, entry,
                        OutputType.TYPE_OTHER, links);
                String categoryName = link.getOutputType().getCategory();
                categoryMenu =
		    HU.makePopup(null, categoryName, categoryMenu.toString(), linkAttr);
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
            //HU.span(msg(menuName), menuClass), 
            menuItems.add(HU.makePopup(null, menuName,
							 entryMenu, linkAttr));

        }

        if (pageStyle.okToShowMenu(entry, PageStyle.MENU_EDIT)
                && (editMenu != null)) {
            if (menuItems.size() > 0) {
                menuItems.add(sep);
            }
            menuItems.add(HU.makePopup(null, "Edit",
							 editMenu, linkAttr));
        }

        if (pageStyle.okToShowMenu(entry, PageStyle.MENU_FEEDS)
                && (exportMenu != null)) {
            if (menuItems.size() > 0) {
                menuItems.add(sep);
            }
            menuItems.add(HU.makePopup(null, "Links",
							 exportMenu, linkAttr));
        }

        if (pageStyle.okToShowMenu(entry, PageStyle.MENU_VIEW)
                && (viewMenu != null)) {
            if (menuItems.size() > 0) {
                menuItems.add(sep);
            }
            menuItems.add(HU.makePopup(null, "View",
							 viewMenu, linkAttr));
        }

        if (pageStyle.okToShowMenu(entry, PageStyle.MENU_OTHER)
                && (categoryMenu != null)) {
            if (menuItems.size() > 0) {
                menuItems.add(sep);
            }
            menuItems.add(categoryMenu);
        }

        String leftTable;
        leftTable = HU.table(
            HU.row(
                HU.cols(Misc.listToStringArray(menuItems)),
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
            String link = HU.href(url, linkLabel);
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
        String id = HU.getUniqueId("crumbs_");
        HU.open(sb, "div", "class", "ramadda-breadcrumbs-list");
        HU.open(sb, "div", "class", "breadCrumbHolder module");
        HU.open(sb, "div", "id", id, "class", "breadCrumb module");
        HU.open(sb, "ul");
        for (Object crumb : breadcrumbs) {
            HU.tag(sb, "li", "", crumb.toString());
        }
        sb.append("</ul></div></div></div>");
        HU.script(sb, "HU.makeBreadcrumbsInit('" + id + "');");
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
            HU.url(
                request.getAbsoluteUrl(getRepository().URL_ENTRY_SHOW),
                ARG_ENTRYID, entry.getId());



        //Table to enclose this toolbar
        sb.append("<table width=\"100%\"><tr><td>");

        // Comments
        if (getRepository().getCommentsEnabled()) {
            List<Comment> comments = entry.getComments();
            if (comments != null) {
                Link link = new Link(
                                request.entryUrl(
                                    getRepository().URL_COMMENTS_SHOW,
                                    entry), ICON_COMMENTS,
                                            "Add/View Comments",
                                            OutputType.TYPE_TOOLBAR);

                String href = HU.href(link.getUrl(),
                                             "Comments:(" + comments.size()
                                             + ")"
                                             + getIconImage(link.getIcon(),
                                                 "title", link.getLabel()));

                sb.append(href);
                sb.append("</td><td>");
            }
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
            String ratings = HU.div(
                                 "",
                                 HU.cssClass("js-kit-rating")
                                 + HU.attr(
                                     HU.ATTR_TITLE,
                                     entry.getFullName()) + HU.attr(
                                         "permalink",
                                         link)) + HU.importJS(
                                             "http://js-kit.com/ratings.js");

            sb.append(
                HU.table(
                    HU.row(
                        HU.col(
                            ratings,
                            HU.attr(
                                HU.ATTR_ALIGN,
                                HU.VALUE_RIGHT)), HU.attr(
                                    HU.ATTR_VALIGN,
                                    HU.VALUE_TOP)), HU.attr(
                                        HU.ATTR_WIDTH, "100%")));
        } else {
            sb.append(HU.p());
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
                HU.href(
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
            //            sb.append(HU.formEntry(BLANK, HU.hr()));
            //TODO: Check for access
            String deleteLink = ( !canEdit
                                  ? ""
                                  : HU.href(request.makeUrl(getRepository().URL_COMMENTS_EDIT,
                                      ARG_DELETE, "true", ARG_ENTRYID,
                                      entry.getId(), ARG_AUTHTOKEN,
                                      getRepository().getAuthToken(request.getSessionId()),
                                      ARG_COMMENT_ID,
                                      comment.getId()), getIconImage(ICON_DELETE,
                                          "title", msg("Delete comment"))));
            if (canEdit) {
                //                sb.append(HU.formEntry(BLANK, deleteLink));
            }
            //            sb.append(HU.formEntry("Subject:", comment.getSubject()));


            String theClass = HU.cssClass("listrow" + rowNum);
            theClass = HU.cssClass(CSS_CLASS_COMMENT_BLOCK);
            rowNum++;
            if (rowNum > 2) {
                rowNum = 1;
            }
            StringBuilder content = new StringBuilder();
            String byLine = HU.span(
                                "Posted by " + comment.getUser().getLabel(),
                                HU.cssClass(
                                    CSS_CLASS_COMMENT_COMMENTER)) + " @ "
                                        + HU.span(
                                            getDateHandler().formatDate(
                                                request,
                                                comment.getDate()), HU.cssClass(
                                                    CSS_CLASS_COMMENT_DATE)) + HU.space(
                                                        1) + deleteLink;
            content.append(
                HU.open(
                    HU.TAG_DIV,
                    HU.cssClass(CSS_CLASS_COMMENT_INNER)));
            content.append(comment.getComment());
            content.append(HU.br());
            content.append(byLine);
            content.append(HU.close(HU.TAG_DIV));
            sb.append(HU
                .div(HU
                    .makeShowHideBlock(HU
                        .span(comment.getSubject(), HU
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
                sb.append(HU.b(msg(cat)));
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
                    Utils.split(
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
        List<String> titles = new ArrayList<String>();
        List<String> tabs   = new ArrayList<String>();
        for (MapLayer mapLayer : getMapLayers()) {
            if (Utils.stringDefined(mapLayer.getLegendLabel())) {
                titles.add(mapLayer.getLegendLabel());
                StringBuffer sb = new StringBuffer(mapLayer.getLegendText());
                if (Utils.stringDefined(mapLayer.getLegendImage())) {
                    sb.append(HU.img(mapLayer.getLegendImage()));
                }
                tabs.add(HU.div(sb.toString(),
                                       HU.cssClass("map-legend-div")));
            }
            mapLayer.addToMap(request, mapInfo);
        }
	titles.add("Some title here");
	tabs.add("Some content here");
	titles.add("Some title here");
	tabs.add("Some content here");
	titles.add("Some title here");
	tabs.add("Some content here");

        StringBuffer rightSide = new StringBuffer();
        rightSide.append("<b>Legends</b><br>");
        rightSide.append(OutputHandler.makeTabs(titles, tabs, true));
	String popupLink = HtmlUtils.div(HU.img("fas fa-layer-group"), HtmlUtils.cssClass("ramadda-popup-link"));
        mapInfo.addRightSide(HU.makePopup(null, popupLink, rightSide.toString(), arg("width","500px"),arg("animate", false),arg("my","right top"), arg("at","left top"),arg("draggable",true),arg("inPlace",true),arg("header",true),arg("fit",true),arg("sticky",true)));
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
            getRepository().getExtEditor().getTypeHandlerSelectors(request, true,
								   includeNonFiles, null);

        HtmlUtils.Selector selector = new HtmlUtils.Selector(
							     HtmlUtils.space(2) +"Find match",
							     TypeHandler.TYPE_FINDMATCH,
							     getRepository().getIconUrl("/icons/blank.gif"), 0, 0,
							     false);
        selector.setAttr(" style=\"padding:6px;\" ");
        items.add(0, selector);
        String selected = (typeHandler != null)
                          ? typeHandler.getType()
                          : "";
        if (true) {
            return HU.select(ARG_TYPE, items, selected);
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

    public StringBuilder  makeEntryPage(Request request, Entry entry, String title, String s) throws Exception {
	StringBuilder sb = new StringBuilder();
	if(entry!=null)
	    entrySectionOpen(request, entry, sb, title, false);
	else
	    sectionOpen(request, sb, title, false);
	sb.append(s);
	if(entry!=null)
	    entrySectionClose(request, entry, sb);
	else
	    sectionClose(request, sb);	
	return sb;
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
        sb.append(HU.sectionOpen(null, showLine));
        if (title != null) {
            HU.sectionTitle(sb, title);
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
        sb.append(HU.sectionClose());
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
	entrySectionOpen(request, entry,null, sb, title, showLine);
    }

    public void entrySectionOpen(Request request, Entry entry, String entryLabel, Appendable sb,
                                 String title, boolean showLine)
	throws Exception {	

	if(request.get(ARG_EMBEDDED,false)) return;
        sb.append(HU.sectionOpen(null, showLine));
        if (entry != null) {
            String label = Utils.stringDefined(entryLabel)?entryLabel:entry.getTypeHandler().getEntryName(entry);
            label = HU.href(getEntryManager().getEntryUrl(request,
                    entry), label);
            HU.sectionTitle(sb, label);
            if (Utils.stringDefined(title)) {
                sb.append(
                    HU.div(
                        HU.div(
                            msg(title),
                            HU.cssClass(
                                "ramadda-heading")), HU.cssClass(
                                    "ramadda-heading-outer")));

            }
        } else {
            HU.sectionTitle(sb, msg(title));
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
	if(request.get(ARG_EMBEDDED,false)) return;
        sb.append(HU.sectionClose());
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
                HU.importJS(
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
        String path;
        if (getRepository().getCdnOk()) {
            path = CDN;
        } else {
            path = getRepository().getUrlBase() + "/"
                   + RepositoryUtil.getHtdocsVersion();
        }

        String root = getRepository().getUrlBase() + "/"
                      + RepositoryUtil.getHtdocsVersion();
        String htdocsBase = makeHtdocsUrl("");

        s = s.replace(
            "${ramadda.bootstrap.version}",
            getRepository().getProperty(
                "ramadda.bootstrap.version", "bootstrap-3.3"));

	String now = htdocsBase + (new Date().getTime());
        return s.replace("${now}",now).replace("${htdocs}", htdocsBase).replace(
            "${cdnpath}", path).replace("${root}", root).replace(
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


        sb.append(HU.hidden(selectId + "_hidden", ((entry != null)
                ? entry.getId()
                : ""), HU.id(selectId + "_hidden")));
        sb.append(HU.formEntry(msgLabel(label),
                                      HU.disabledInput(selectId,
                                          ((entry != null)
                                           ? entry.getFullName()
                                           : ""), HU.id(selectId)
                                           + HU.SIZE_60
                                           + ((entry == null)
                ? HU.cssClass(CSS_CLASS_REQUIRED_DISABLED)
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

        return HU.href(url, (args.length > 0)
                                   ? args[0]
                                   : entry.getLabel());
    }

}
