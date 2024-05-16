/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;


import org.ramadda.repository.auth.AccessException;
import org.ramadda.repository.auth.User;
import org.ramadda.repository.auth.SessionManager;

import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.map.MapLayer;
import org.ramadda.repository.metadata.ContentMetadataHandler;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.output.CalendarOutputHandler;
import org.ramadda.repository.output.HtmlOutputHandler;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.RssOutputHandler;
import org.ramadda.repository.output.AtomOutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.output.PageStyle;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.util.CategoryBuffer;
import org.ramadda.util.HtmlTemplate;
import org.ramadda.util.IO;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.MapRegion;
import org.ramadda.util.NamedValue;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.GeoUtils;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.Comparator;
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
@SuppressWarnings("unchecked")
public class PageHandler extends RepositoryManager {

    private static  boolean debugTemplates = false;

    /**  */
    public static final String IMPORTS_BEGIN = "<!--imports-->";

    /**  */
    public static final String IMPORTS_END = "<!--end imports-->";


    /**  */
    public static final String PREFIX_NOPRELOAD = "nopreload:";



    /** _more_ */
    public static final String DEFAULT_TEMPLATE = "fixedmapheader";

    /** _more_ */
    private static final String ACK_MESSAGE =
        "<div class='ramadda-acknowledgement'><a title='geodesystems.com' href='https://geodesystems.com'><img loading=lazy width=100px  src='${cdnpath}/images/poweredby.png'></a><br><a title='Help' href=${root}/userguide/index.html><i class='fas fa-question-circle'></i></a>&nbsp;<a title='Server Information' href=${root}/info><i class='fas fa-circle-info'></i></a></div>";

    /**  */
    private String ackMessage;



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

    public static final String MACRO_PAGEHEADER = "pageheader";

    /** html template macro */
    public static final String MACRO_LOGO_URL = "logo.url";

    /** html template macro */
    public static final String MACRO_LOGO_IMAGE = "logo.image";

    /** html template macro */
    public static final String MACRO_SEARCH_URL = "search.url";

    public static final String MACRO_ENTRY_NAME = "entry.name";
    public static final String MACRO_ENTRY_URL = "entry.url";    

    /** html template macro */
    public static final String MACRO_ENTRY_HEADER = "entry.header";

    /** html template macro */
    public static final String MACRO_HEADER = "header";

    /** html template macro */
    public static final String MACRO_ENTRY_FOOTER = "entry.footer";




    /** html template macro */
    public static final String MACRO_ENTRY_BREADCRUMBS = "entry.breadcrumbs";

    public static final String MACRO_ENTRY_POPUP = "entry.popup";
    /** html template macro */
    public static final String MACRO_HEADER_IMAGE = "header.image";

    /** html template macro */
    public static final String MACRO_HEADER_TITLE = "header.title";

    /** html template macro */
    public static final String MACRO_USERLINK = "userlinks";



    /** html template macro */
    public static final String MACRO_REPOSITORY_NAME = "repository_name";

    /** _more_ */
    public static final String MACRO_REGISTER = "register";

    /** html template macro */
    public static final String MACRO_FOOTER = "footer";

    /**  */
    public static final String MACRO_FOOTER_ACKNOWLEDGEMENT =
        "footer.acknowledgement";




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

    /**  */
    public static final String MACRO_IMPORTS = "imports";


    /** _more_ */
    private String webImports;

    private String displayImports;

    /** _more_ */
    private List<HtmlTemplate> htmlTemplates;


    /** _more_ */
    private Hashtable<String, HtmlTemplate> templateMap;


    /** _more_ */
    private HtmlTemplate mapTemplate;


    /** _more_ */
    private Hashtable<String, StringBuilder> languageMap = new Hashtable<String,StringBuilder>();


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
    public static final String ID_TEMPLATE_DEFAULT = "_DEFAULT_";

    public static final String ID_TEMPLATE_MOBILE = "_MOBILE_";    

    /** _more_ */
    public static final String TEMPLATE_CONTENT = "content";
    public static final String TEMPLATE_DEFAULT = "default";    


    /** _more_ */
    private boolean showCreateDate;

    /** _more_ */
    private boolean showJsonLd;    

    private boolean showTwitterCard;
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

    /**  */
    private boolean showHelp = true;

    /**  */
    private boolean noStyle = false;

    /**  */
    private String logoUrl = "";

    /**  */
    private String bootstrapVersion = "bootstrap-5.1.3";


    /** _more_ */
    TimeZone defaultTimeZone;


    /** _more_ */
    private Hashtable<String, SimpleDateFormat> dateFormats =
        new Hashtable<String, SimpleDateFormat>();

    /** _more_ */
    protected List<SimpleDateFormat> parseFormats;



    /**  */
    String searchImg = HU.faIcon("ramadda-header-icon fas fa-search", "title", "Search", "class",
                                 "ramadda-user-menu-image", "id",
                                 "searchlink");

    /**  */
    String popupImage;


    /**
     * _more_
     *
     * @param repository _more_
     */
    public PageHandler(Repository repository) {
        super(repository);
        popupImage = HU.faIcon("ramadda-header-icon fas fa-cog", "title",
                               "Login, user settings, help", "class",
                               "ramadda-user-menu-image");
        popupImage = HtmlUtils.div(popupImage,
                                   HtmlUtils.cssClass("ramadda-popup-link"));
    }



    /**
     * _more_
     */
    @Override
    public void initAttributes() {
        super.initAttributes();

        displayImports = makeDisplayImports();	

        //Clear out any loaded templates
        clearCache();
        showCreateDate =
            getRepository().getProperty(PROP_ENTRY_TABLE_SHOW_CREATEDATE,
                                        false);

        showJsonLd = getRepository().getProperty("ramadda.showjsonld", false);
        showTwitterCard = getRepository().getProperty("ramadda.showtwittercard", true);	
        showSearch = getRepository().getProperty("ramadda.showsearch", true);
        createdDisplayMode =
            getRepository().getProperty(PROP_CREATED_DISPLAY_MODE,
                                        "all").trim();
        footer      = repository.getProperty(PROP_HTML_FOOTER, BLANK);
        myLogoImage = getRepository().getProperty(PROP_LOGO_IMAGE, null);
        noStyle     = getRepository().getProperty(PROP_NOSTYLE, false);
        showHelp    = getRepository().getProperty(PROP_SHOW_HELP, true);
        cacheTemplates =
            getRepository().getProperty("ramadda.cachetemplates", true);

        bootstrapVersion =
            getRepository().getProperty("ramadda.bootstrap.version",
                                        bootstrapVersion);
        logoUrl = getRepository().getProperty(PROP_LOGO_URL, "");
        initWebResources();
    }


    /**
     *
     * @param resource _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public List<String[]> readWebResources(String resource) throws Exception {
        List<String[]> result = new ArrayList<String[]>();
        List<String> files =
            Utils.split(getStorageManager().readSystemResource(resource),
                        "\n", true, true);

        boolean minified = getRepository().getMinifiedOk();
        String  path;
        boolean cdnOk = getRepository().getCdnOk();
        if (cdnOk) {
            path = getCdn();
        } else {
            path = getRepository().getUrlBase() + "/"
                   + RepositoryUtil.getHtdocsVersion();
        }
        //      System.err.println("cdn:" + cdnOk);
        for (String file : files) {
            if (file.startsWith("#")) {
                continue;
            }
            if (file.startsWith("full:")) {
                if (minified) {
                    continue;
                }
                file = file.substring("full:".length());
            } else if (file.startsWith("min:")) {
                if ( !minified) {
                    continue;
                }
                file = file.substring("min:".length());
            } else if (file.startsWith("cdn:")) {
                if ( !cdnOk) {
                    continue;
                }
                file = file.substring("cdn:".length());
            } else if (file.startsWith("nocdn:")) {
                if (cdnOk) {
                    continue;
                }
                file = file.substring("nocdn:".length());
            }
            boolean nopreload = file.startsWith(PREFIX_NOPRELOAD);
            if (nopreload) {
                file = file.substring(PREFIX_NOPRELOAD.length());
            }
            //      System.err.println("\tfile:" + file);
            if ( !file.startsWith("http")) {
                file = path + file;
            }

            if (nopreload) {
                file = PREFIX_NOPRELOAD + file;
            }
            int intIdx = file.indexOf(":integrity:");
            if (intIdx >= 0) {
                String integrity = file.substring(intIdx
                                       + ":integrity:".length());
                file = file.substring(0, intIdx);
                result.add(new String[] { file, integrity });
            } else {
                result.add(new String[] { file });
            }
        }

        return result;
    }





    /**
     * _more_
     *
     * @param request the request
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addDisplayImports(Request request, Appendable sb)
	throws Exception {
	addDisplayImports(request, sb, true);
    }

    public void addDisplayImports(Request request, Appendable sb, boolean includeMap)
	throws Exception {
	if(includeMap) {
	    getMapManager().addMapImports(request, sb);
	}
        if (request.getExtraProperty("initchart") == null) {
            request.putExtraProperty("initchart", "added");
	    request.appendHead(displayImports);
        }
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public String makeDisplayImports() {
        try {
            Appendable sb = Utils.makeAppendable();
	    getPageHandler().addJSImports(sb, "/org/ramadda/repository/resources/web/wikijsimports.txt");

            if (getRepository().getMinifiedOk()) {
                HU.importJS(sb, getPageHandler().getCdnPath("/min/display_all.min.js"));
		String css = getPageHandler().getCdnPath("/min/display.min.css");
		HU.cssPreloadLink(sb, css);
		//HU.cssLink(sb, css);
		sb.append("\n");
            } else {
		sb.append("\n");
		String css = getPageHandler().getCdnPath("/display/display.css");
		HU.cssPreloadLink(sb, css);
		//                HU.cssLink(sb, css);
		sb.append("\n");
		for(String js: new String[]{"/colortables.js",
					    //"/esdlcolortables.js",
					    "/display/pointdata.js", 
					    "/display/filters.js", 
					    "/display/widgets.js",
					    "/display/animation.js",					    
					    "/display/colorby.js",
					    "/display/glyph.js",
					    "/display/display.js",
					    "/display/displaymanager.js",
					    "/display/displayentry.js",
					    "/display/displaymap.js",
					    "/display/othermaps.js",					    
					    "/display/imdv.js",
					    "/display/mapglyph.js",
					    "/display/displayimages.js",
					    "/display/displaymisc.js",
					    "/display/displaychart.js",
					    "/display/displaytable.js",
					    "/display/control.js",
					    "/display/notebook.js",
					    "/display/displayplotly.js",
					    "/display/displayd3.js",
					    "/display/displaytext.js",
					    "/display/displayext.js",
					    "/display/displaythree.js",					    
					    "/repositories.js"}) {
		    HU.importJS(sb, getPageHandler().getCdnPath(js));
		    sb.append("\n");
		}
	    }

            String includes =
                getRepository().getProperty("ramadda.display.includes",
                                            (String) null);
            if (includes != null) {
                for (String include :
			 Utils.split(includes, ",", true, true)) {
                    HU.importJS(sb, getFileUrl(include));
                }
		sb.append("\n");
            }

            return sb.toString();
        } catch (Exception exc) {
            throw new IllegalArgumentException(exc);
        }
    }




    /**
     *
     * @param sb _more_
     * @param resourcePath _more_
     *
     * @throws Exception _more_
     */
    public void addJSImports(Appendable sb, String resourcePath)
            throws Exception {
        for (String[] file : readWebResources(resourcePath)) {
            HU.importJS(sb, file[0], (file.length > 1)
                                     ? file[1]
                                     : null);
            sb.append("\n");
        }
    }



    /**
     * _more_
     */
    private void initWebResources() {
        try {

            StringBuilder imports = new StringBuilder();
            addJSImports(
                imports,
                "/org/ramadda/repository/resources/web/jsimports.html");
            for (String[] tuple : readWebResources(
                    "/org/ramadda/repository/resources/web/cssimports.html")) {
                String file = tuple[0];
                if (file.startsWith(PREFIX_NOPRELOAD)) {
                    HU.cssLink(imports,
                               file.substring(PREFIX_NOPRELOAD.length()));
                } else {
                    HU.cssPreloadLink(imports, file);
                }
            }
            webImports = applyBaseMacros(imports.toString());
            webImports = IMPORTS_BEGIN + "\n" + webImports + "\n"
                         + IMPORTS_END + "\n";
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
     *
     * @param entry _more_
      * @return _more_
     */
    public String getEntryTooltip(Entry entry) {
        return noMsg(entry.getName()) + HU.NL + " - "
	    + msg(entry.getTypeHandler().getLabel());
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
        String html = decorateResult(request, result, true, true);
        if (html != null) {
            result.setContent(html);
        }
    }


    /**
     *
     * @param request _more_
     * @param result _more_
     * @param prefix _more_
     * @param suffix _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String decorateResult(Request request, Result result,
                                 boolean prefix, boolean suffix)
            throws Exception {
        StringBuilder sb = new StringBuilder();
        if ( !decorateResult(request, result, sb, prefix, suffix)) {
            return null;
        }

        return sb.toString();
    }

    private String wrapPageLink(String s) {
        return  "<span class=ramadda-page-link>" + s + "</span>";
    }


    /**
     *
     * @param request _more_
     * @param result _more_
     * @param sb _more_
     * @param prefix _more_
     * @param suffix _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public boolean decorateResult(Request request, Result result,
                                  Appendable sb, boolean prefix,
                                  boolean suffix)
            throws Exception {

        boolean fullTemplate = prefix && suffix;
        //      Runtime.getRuntime().gc();
        double mem1 = Utils.getUsedMemory();

        long   t0   = System.currentTimeMillis();
        if ( !request.get(ARG_DECORATE, true)) {
            return false;
        }
        Repository   repository     = getRepository();
        Entry        currentEntry = getSessionManager().getLastEntry(request);
        HtmlTemplate parentTemplate = getTemplate(request, currentEntry);
	if(parentTemplate==null) {
	    System.err.println("GACK: "+request);
	    System.exit(0);
	}
        if (request.isMobile() && !request.defined(ARG_TEMPLATE)) {
            if ( !parentTemplate.getTemplateProperty("mobile", false)) {
                parentTemplate = getMobileTemplate();
            }
        }


        HtmlTemplate htmlTemplate = parentTemplate;

        if ( !fullTemplate) {
            if (prefix) {
                htmlTemplate = htmlTemplate.getPrefix();
            } else {
                htmlTemplate = htmlTemplate.getSuffix();
            }
        }



        String template = htmlTemplate.getTemplate();

        String entryHeader = (String) result.getProperty(PROP_ENTRY_HEADER,
                                 (String) null);
        String entryFooter = (String) result.getProperty(PROP_ENTRY_FOOTER,
                                 (String) null);
        String entryBreadcrumbs =
            (String) result.getProperty(PROP_ENTRY_BREADCRUMBS,
                                        (String) null);
        String entryPopup =
            (String) result.getProperty(PROP_ENTRY_POPUP,
                                        (String) null);	
        Entry  thisEntry = request.getCurrentEntry();
        String     header        = entryHeader;
	String pageHeader = "";

	if(prefix) {
	    List<Metadata> pageHeaderMtd = 
		getMetadataManager().findMetadata(request, thisEntry!=null?thisEntry:getEntryManager().getRootEntry(),
						  "content.pageheader",true);
	    if(pageHeaderMtd!=null && pageHeaderMtd.size()>0) {
		pageHeader = getWikiManager().wikifyEntry(request,thisEntry!=null?thisEntry:getEntryManager().getRootEntry(),pageHeaderMtd.get(0).getAttr1());
	    }
	}


	String headFinal = "";
	if(prefix) {
	    String  headContent = request.getHeadContent();
	    if(headContent!=null) {
		headFinal = headContent;
	    }
	}

        Appendable contents      = new StringBuilder();

        String     systemMessage = getRepository().getSystemMessage(request);
        boolean    hasContents   = htmlTemplate.hasMacro(MACRO_CONTENT);
        String     extraMessage  = null;
        if (Utils.stringDefined(systemMessage)) {
            extraMessage = HU.div(systemMessage,
                                  HU.cssClass("ramadda-system-message"));
        }

        if ((extraMessage != null) && hasContents) {
            contents.append(extraMessage);
        }



        String jsContent = getTemplateJavascriptContent();
        String bottom    = null;
	String extraFooter = null;
        if (fullTemplate || suffix) {
            contents = Utils.append(contents, result.getStringContent());
            if (htmlTemplate.hasMacro(MACRO_BOTTOM)) {
                bottom = jsContent;
            } else {
		extraFooter = jsContent;
		//                contents = Utils.append(contents, jsContent);
            }
        }
        String        content = (contents != null)
                                ? contents.toString()
                                : null;


        StringBuilder head    = new StringBuilder();
        if (request.getHead0() != null) {
            head.append(request.getHead0());
            request.clearHead0();
        }

        //make the request to base.js be unique every time so the browser does not cache it
        HU.script(head, getRepository().getBaseJs(request));
        head.append(webImports);
        String head2 = request.getHead();
        if (head2 != null) {
            head.append(head2);
            request.clearHead();
        }
	if(request.getIsEntryShow() && prefix && currentEntry!=null) {
	    getMetadataManager().addHtmlMetadata(request, currentEntry, head, showJsonLd, showTwitterCard);

            String rssUrl =
		HU.url(request.getAbsoluteUrl(getRepository().URL_ENTRY_SHOW+"/" + IO.stripExtension(currentEntry.getName()) + ".rss"),
		       ARG_ENTRYID, currentEntry.getId(),
		       ARG_OUTPUT, RssOutputHandler.OUTPUT_RSS_FULL.toString());
	    head.append("<link rel='alternate' type='application/rss+xml' ");
	    HU.attrs(head,"title",currentEntry.getName(),"href",   rssUrl);
	    head.append("/>\n");
            String atomUrl =
		HU.url(request.getAbsoluteUrl(getRepository().URL_ENTRY_SHOW+"/" + IO.stripExtension(currentEntry.getName()) + ".xml"),
		       ARG_ENTRYID, currentEntry.getId(),
		       ARG_OUTPUT, AtomOutputHandler.OUTPUT_ATOM.toString());
	    head.append("<link rel='alternate' type='application/atom+xml' ");
	    HU.attrs(head,"title",currentEntry.getName(),"href",   atomUrl);
	    head.append("/>\n");

        }





        String imports   = head.toString();
        String logoImage = getLogoImage(result);
        String logoUrl   = (String) result.getProperty(PROP_LOGO_URL);
        if ( !Utils.stringDefined(logoUrl)) {
	    logoUrl = getRepository().getProperty(request.getRequestHostname()+".logourl");
	}

        if ( !Utils.stringDefined(logoUrl)) {
            logoUrl = this.logoUrl;
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
        List<String> navLinks = null;

        try {
	    navLinks = getNavLinks(request, userLinkTemplate);
	} catch(Exception exc) {
            Throwable     inner     = LogUtil.getInnerException(exc);
	    if(inner instanceof AccessException) {
		//Ignore access exception since the top-level entry might have an access control setting
	    } else {
		throw exc;
	    }
	}
        List<String> userLinks = getUserLinks(request, userLinkTemplate,
					      extra, true);
	if(navLinks!=null)        allLinks.addAll(navLinks);
        allLinks.addAll(userLinks);
        String menuHtml = HU.div(StringUtil.join("", allLinks),
				 HU.id("ramadda_user_menu")+
                                 HU.cssClass("ramadda-user-menu-popup"));

	List<String> pageLinks = new ArrayList<String>();

        if (showSearch) {
	    pageLinks.add(wrapPageLink(
				       HU.mouseClickHref("Utils.searchPopup('searchlink','popupanchor');", searchImg, "")+
				       HU.span("", HU.attrs("id", "popupanchor", "style", "position:relative;"))));
        }

	pageLinks.add(HU.span("",HU.attrs("style","display:block;","id","ramadda_links_prefix")));
        StringBuilder theFooter = new StringBuilder(footer);
	if(extraFooter!=null) theFooter.append(extraFooter);


	


        if (suffix && thisEntry != null) {
	    String footerScript = "ramaddaThisEntry='" + thisEntry.getId() + "';\n";
	    if(thisEntry.isGroup() && getAccessManager().canDoNew(request, thisEntry)) {
		footerScript+=HU.call("RamaddaUtil.initDragAndDropOnHeader",
				      HU.squote(thisEntry.getId())+"," +
				      HU.squote(getAuthManager().getAuthToken(request.getSessionId())));
	    }
            theFooter.append(HU.script(footerScript));
	    List<Metadata> footerMtd = 
                    getMetadataManager().findMetadata(request, thisEntry,
						      "content.footer",true);
	    if(footerMtd!=null && footerMtd.size()>0) {
		for(Metadata mtd:footerMtd) {
		    String w= getWikiManager().wikifyEntry(request, thisEntry, mtd.getAttr1());
		    theFooter.append(w);
		}
	    }

	    List<Metadata> headerMtd = 
                    getMetadataManager().findMetadata(request, thisEntry,
						      "content.header",true);
	    if(headerMtd!=null && headerMtd.size()>0) {
		StringBuilder headerSB = new StringBuilder();
		for(Metadata mtd:headerMtd) {
		    String w= getWikiManager().wikifyEntry(request, thisEntry, mtd.getAttr1());
		    headerSB.append(w);
		}
		HU.div(theFooter,headerSB.toString(),HU.clazz("ramadda-header-floating"));
	    }


	}

	List<String> messages= (List<String>)getSessionManager().getSessionProperty(request, SessionManager.SESSION_PROPERTY_ERRORMESSAGES);
	if(messages!=null) {
	    getSessionManager().removeSessionProperty(request, SessionManager.SESSION_PROPERTY_ERRORMESSAGES);
	    HU.div(theFooter,Utils.join(messages,"<br>"),HU.clazz("ramadda-header-floating ramadda-session-error"));
	}


	pageLinks.add(wrapPageLink(HU.makePopup(null, popupImage, menuHtml,
				   arg("my", "right top"),
				   arg("at", "left bottom"),
						arg("animate", false))));
	if(extra.length()>0)
	    pageLinks.add(wrapPageLink(extra.toString()));
	pageLinks.add(wrapPageLink(HU.span("",HU.id("ramadda_links_suffix"))));
        menuHtml = HU.span(Utils.join(pageLinks,""),HU.clazz("ramadda-user-menu"));


        String[] macros = new String[] {
	    MACRO_PAGEHEADER,pageHeader,
            MACRO_LOGO_URL, logoUrl, MACRO_LOGO_IMAGE, logoImage,
            MACRO_HEADER_IMAGE, getHeaderIcon(), MACRO_HEADER_TITLE,
            pageTitle, MACRO_LINKS, menuHtml, MACRO_REPOSITORY_NAME,
            repository.getRepositoryName(), MACRO_FOOTER, theFooter.toString(),
            MACRO_FOOTER_ACKNOWLEDGEMENT, getAckMessage(), MACRO_TITLE,
            Utils.stripTags(result.getTitle()), MACRO_BOTTOM, bottom, MACRO_SEARCH_URL,
            getSearchManager().getSearchUrl(request), MACRO_CONTENT, content,
            MACRO_ENTRY_HEADER, entryHeader, MACRO_HEADER, header,
	    MACRO_ENTRY_NAME,
	    (String) result.getProperty(PROP_ENTRY_NAME,  "RAMADDA"),
	    MACRO_ENTRY_URL,  
	    (String) result.getProperty(PROP_ENTRY_URL,  getRepository().getUrlBase()),
            MACRO_ENTRY_FOOTER, entryFooter, MACRO_ENTRY_POPUP,entryPopup,
	    MACRO_ENTRY_BREADCRUMBS,
            entryBreadcrumbs, MACRO_IMPORTS, imports, MACRO_HEADFINAL, headFinal,
            MACRO_ROOT, repository.getUrlBase(),
        };

        long                      t2     = System.currentTimeMillis();
        String                    html   = template;
        Hashtable<String, String> values = new Hashtable<String, String>();
        for (int i = 0; i < macros.length; i += 2) {
            if (macros[i + 1] != null) {
                values.put(macros[i], macros[i + 1]);
            }
        }
        for (String property : htmlTemplate.getPropertyIds()) {
            values.put(Utils.concatString("${", property, "}"),
                       getRepository().getProperty(property, ""));
        }


        //Toks are [html,macro,html,macro,...,html]
        List<String> templateToks;
        if (htmlTemplate.getWikify()) {
            template = getWikiManager().wikifyEntry(request,
                    getEntryManager().getRootEntry(), template, false);
            templateToks = htmlTemplate.getToks(template);
        } else {
            templateToks = htmlTemplate.getToks();
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

        if ((extraMessage != null) && !hasContents && prefix) {
            sb.append(extraMessage);
        }
        return true;
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
            logoImage = "${cdnpath}/images/logo.png";
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
        List<Utils.Macro>  toks   = Utils.splitMacros(template);
        StringBuilder result = new StringBuilder();
        if (toks.size() > 0) {
	    for(Utils.Macro macro: toks) {
		if(macro.isText()) {
		    result.append(macro.getText());
		} else {
                    String prop = getRepository().getProperty(macro.getId(),
							      (String) null);
                    if (prop == null) {
                        if (ignoreErrors) {
                            prop = "${" + macro.getId() + "}";
                        } else {
                            throw new IllegalArgumentException("Could not find property:" + macro.getId()
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
        StringBuilder   phrases = new StringBuilder();
        String       type    =
            IO.stripExtension(IO.getFileTail(file));
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
                phrases.append(key+"="+value);
		phrases.append("\n");
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

    private String languagesJson;
    public String  getLanguagesJson() {
	if(languagesJson==null) {
	    List<String> l =new ArrayList<String>();
	    l.add(JsonUtil.map("id",JsonUtil.quote("en"),
			       "label",JsonUtil.quote("English")));
		
	    for(TwoFacedObject tfo: getLanguages()) {
		l.add(JsonUtil.map("id",JsonUtil.quote(tfo.getId()),
				   "label",JsonUtil.quote(tfo.getLabel())));
	    }
	    languagesJson = JsonUtil.list(l);
	}
	return languagesJson;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public HtmlTemplate getMobileTemplate() {
        return getTemplateMap().get(ID_TEMPLATE_MOBILE);
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



    private Hashtable<String, HtmlTemplate> getTemplateMap() {
	try {
	    return  checkTemplates();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<HtmlTemplate> getTemplates() {
        try {
	    if(debugTemplates)
		System.err.println("PageHandler.getTemplates");
	    checkTemplates();
	    List<HtmlTemplate> tmp_theTemplates =  htmlTemplates;
	    if(debugTemplates)
		System.err.println("\tPageHandler.tmp_theTemplates:" + (tmp_theTemplates!=null));
	    while(tmp_theTemplates==null) {
		if(debugTemplates)
		    System.err.println("\tsleeping");
		Misc.sleep(10);
		checkTemplates();
		tmp_theTemplates=  htmlTemplates;
		if(debugTemplates)
		    System.err.println("\tPageHandler.tmp_theTemplates:" + (tmp_theTemplates!=null));
	    }
	    return tmp_theTemplates;
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
    private synchronized Hashtable<String, HtmlTemplate> checkTemplates()
            throws Exception {
	Hashtable<String,HtmlTemplate> tmp_templateMap  = templateMap;
	if(!cacheTemplates) {
	    if(debugTemplates)
		System.err.println("PageHandler.checkTemplates: not cached");
	    tmp_templateMap  =null;
	}
	if(tmp_templateMap!=null && htmlTemplates!=null) {
	    if(debugTemplates)
		System.err.println("PageHandler.checkTemplates: template map not null");
	    return tmp_templateMap;
	}
	String mobileId =
	    getRepository().getProperty("ramadda.template.mobile",
					(String) null);
	HtmlTemplate theMobileTemplate = null;
	//use locals here in case of race conditions
	HtmlTemplate _defaultTemplate = null;
	HtmlTemplate _mobileTemplate  = null;
	List<HtmlTemplate> tmp_theTemplates =   new ArrayList<HtmlTemplate>();
	tmp_templateMap  = new Hashtable<String, HtmlTemplate>();

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
		if (IO.getFileTail(path).equals("template.html")) {
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
		if (idx >= 0) {
		    template.setPrefix(new HtmlTemplate(template,
							template.getTemplate().substring(0, idx)));
		    template.setSuffix(new HtmlTemplate(template,
							template.getTemplate().substring(idx
											 + "${content}".length())));
		}


		template.setTemplate(applyBaseMacros(template.getTemplate()));
		if (template.getPrefix() != null) {
		    template.getPrefix().setTemplate(
						     applyBaseMacros(
								     template.getPrefix().getTemplate()));
		}
		if (template.getSuffix() != null) {
		    template.getSuffix().setTemplate(
						     applyBaseMacros(
								     template.getSuffix().getTemplate()));
		}


		//Check if we got some other ...template.html file from a plugin
		if (template.getId() == null) {
		    System.err.println("template: no id in " + path);

		    continue;
		}
		tmp_templateMap.put(template.getId(), template);
		tmp_theTemplates.add(template);

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
			    System.err.println("\tset-1:"+ _defaultTemplate);
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
	    _defaultTemplate = tmp_theTemplates.get(0);
	    if (debugTemplates) {
		System.err.println("\tset-3:" + _defaultTemplate);
	    }
	}
	if (_mobileTemplate == null) {
	    _mobileTemplate = _defaultTemplate;
	}
	tmp_templateMap.put(ID_TEMPLATE_DEFAULT, _defaultTemplate);
	tmp_templateMap.put(ID_TEMPLATE_MOBILE, _mobileTemplate);	    
	htmlTemplates   = tmp_theTemplates;
	if(debugTemplates)
	    System.err.println("PageHandler.checkTemplates: done htmlTemplates: " + (htmlTemplates!=null));
	templateMap  = tmp_templateMap;
	return tmp_templateMap;
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





    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void loadLanguagePacks() throws Exception {
	languageMap = new Hashtable<String,StringBuilder>();
	HashSet<String> seenPack = new HashSet<String>();
        List sourcePaths =
	    Misc.newList(
			 "/org/ramadda/repository/htdocs/languages",
			 getStorageManager().getHtdocsDir() + "/languages",
			 getStorageManager().getPluginsDir().toString());
        for (int i = 0; i < sourcePaths.size(); i++) {
            String       dir     = (String) sourcePaths.get(i);
            List<String> listing = getRepository().getListing(dir,getClass());
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
                    getLogManager().logInfoAndPrint("RAMADDA: could not read:" + path);
                    continue;
                }
                Object[]   result     = parsePhrases(path, content);
                String     type       = (String) result[0];
                String     name       = (String) result[1];
                StringBuilder phrases = (StringBuilder) result[2];
                if (type != null) {
                    if (name == null) {
                        name = type;
                    }
		    StringBuilder existing = languageMap.get(type);
		    if(existing ==null) {
			languages.add(new TwoFacedObject(name, type));
			existing = new StringBuilder();
			languageMap.put(type,existing);
		    }
		    existing.append("\n");
		    existing.append(phrases);
                } else {
                    getLogManager().logError("No _type_ found in: " + path);
                }
            }
        }
    }

    public StringBuilder getLanguage(String lang)  {
	//	try {loadLanguagePacks();}catch(Exception exc) {}
	return 	languageMap.get(lang);
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
        mapRegionFiles.addAll(Utils.makeListFromValues(pre + "mapregions.csv",
                                             pre + "countrymapregions.csv",
                                             pre + "statesmapregions.csv",
                                             pre + "citiesmapregions.csv"));

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
                            GeoUtils.decodeLatLon(toks.get(2)),
                            GeoUtils.decodeLatLon(toks.get(3))));
                } else {
                    mapRegions.add(new MapRegion(toks.get(1), name, group,
                            GeoUtils.decodeLatLon(toks.get(2)),
                            GeoUtils.decodeLatLon(toks.get(3)),
                            GeoUtils.decodeLatLon(toks.get(4)),
                            GeoUtils.decodeLatLon(toks.get(5))));
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
                String s1 = (String) ((TwoFacedObject) o1).getLabel();
                String s2 = (String) ((TwoFacedObject) o2).getLabel();

                return s1.compareToIgnoreCase(s2);
            }
        });

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
	Hashtable<String, HtmlTemplate> templateMap = getTemplateMap();    
	//	debugTemplates = true;
	if (debugTemplates) 	    System.err.println("getTemplate");
        if (request == null) {
	    if (debugTemplates) 	    System.err.println("\tgetTemplate:no request");
            return templateMap.get(TEMPLATE_DEFAULT);
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
                        new String[] {
                            ContentMetadataHandler.TYPE_TEMPLATE }, true);
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

	if(isMobile) {
	    HtmlTemplate mobileTemplate=getMobileTemplate();
	    if (mobileTemplate != null) {
		request.put(ARG_TEMPLATE, mobileTemplate.getId());
		if (debugTemplates) {
		    System.err.println("getTemplate mobile:" + mobileTemplate);
		}
		return mobileTemplate;
	    }
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

	if(!stringDefined(templateId)) {
	    templateId = getRepository().getProperty(request.getRequestHostname()+".template");
	}

        if (templateId != null) {
            HtmlTemplate template = templateMap.get(templateId);
	    if (debugTemplates)
		System.err.println("\tgetTemplate: template id:" + templateId +" template:" + template);

            if (template != null) {
                return template;
            }
        }



	HtmlTemplate template = templateMap.get(ID_TEMPLATE_DEFAULT);
	if(template==null) {
	    //	    System.err.println("\tgetTemplate: using default:" + templateMap);
	}
	if (debugTemplates) 	 System.err.println("\tgetTemplate: using default:" + template);
	return template;
    }




    public static String noMsg(String msg) {
	String s =  HU.span(msg,HU.clazz("ramadda-notranslate"));
	return s;
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
        if (true) {
            return HU.span(msg,"");
        }

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
                "ramadda-page-heading-bg" /*CSS_CLASS_HEADING_1*/));
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
        String cancelButton = HU.submit(LABEL_CANCEL, Constants.ARG_CANCEL);
        String buttons      = HU.buttons(okButton, cancelButton);
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
                    request.remove(ARG_MESSAGE,ARG_REDIRECT,ARG_USER_ID);
                    String redirect = Utils.encodeBase64(request.getUrl());
                    url = request.makeUrl(getRepositoryBase().URL_USER_LOGIN,
                                          ARG_REDIRECT, redirect);
                }

                urls.add(url);
                labels.add(HU.faIcon("fa-sign-in-alt") + " " + msg("Login"));
                tips.add(msg("Login"));
            }

        } else {
            extras.add("");
            urls.add(request.makeUrl(getRepositoryBase().URL_USER_LOGOUT));
            labels.add(HU.faIcon("fa-sign-out-alt") + " " + msg("Logout"));
            tips.add(msg("Logout"));
            String label = user.getLabel().replace(" ", "&nbsp;");
	    String avatar = getUserManager().getUserAvatar(request, request.getUser(),true,25,
							   HU.attrs("class","ramadda-user-menu-image","title","User Settings - "+
								    request.getUser().getLabel()));
            String userIcon = avatar!=null?avatar:HU.faIcon("fa-user", "title",
							    "User Settings", "class",
							    "ramadda-user-menu-image");

            String settingsUrl =
                request.makeUrl(getRepositoryBase().URL_USER_SETTINGS);

            if (makePopup) {
                prefix.append(
                    HU.href(
                        settingsUrl, userIcon,
                        HU.cssClass("ramadda-user-settings-link")));
            } else {
                extras.add("");
                urls.add(settingsUrl);
                labels.add(label);
                tips.add(msg("Go to user settings"));
            }
        }

        if (showHelp
                && (getRepository().getPluginManager().getDocUrls().size()
                    > 0)) {
            urls.add(request.makeUrl(getRepositoryBase().URL_HELP));
            extras.add("");
            labels.add(HU.faIcon("fa-question-circle") + " " + msg("Help"));
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
        boolean      isAdmin = (request == null)
                               ? false
                               : request.isAdmin();
        ApiMethod    homeApi = getRepository().getApiManager().getHomeApi();
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
            if (label != null) label = HU.span(label,HU.clazz("ramadda-nowrap"));
            label = msg(label);
            if (label == null) {
                label = requestUrl.toString();
            }
            String url   = request.makeUrl(requestUrl) + arg;
            String clazz = "ramadda-highlightable ramadda-linksheader-off";
            if (requestUrl.matches(request)) {
                onLabel = label;
                clazz   = "ramadda-highlightable ramadda-linksheader-on";
            }
            links.add(HU.span(HU.href(url, label), HU.cssClass(clazz)));
            //            }
        }
        StringBuilder header = new StringBuilder();
	//add a space after so the whole line can be broken
        HU.div(header,
               StringUtil.join(
                   "<span class=\"ramadda-separator\">|</span>",
                   links), HU.cssClass("ramadda-linksheader-links"));
        header.append("\n");
        sb.append(HU.tag(HU.TAG_DIV, HU.cssClass("ramadda-linksheader"),
                         header.toString()));
    }



    /**
     * _more_
     *
     * @param h _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public String showDialogNote(String h, String... extra) {
        return getDialog(h, extra, ICON_DIALOG_INFO, false);
    }

    /**
     * _more_
     *
     * @param h _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public String showDialogBlank(String h, String... extra) {
        return getDialog(h, extra, null, false);
    }

    /**
     *
     * @param entry _more_
      * @return _more_
     */
    public String showAccessRestricted(Entry entry) {
        return showDialogWarning("Access to " + entry.getName()
                                 + " is restricted");
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
     * @param extra _more_
     *
     * @return _more_
     */
    public String showDialogWarning(String h, String... extra) {
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
        return getDialog(h, new String[] { buttons },
                         Constants.ICON_DIALOG_QUESTION, false);
    }

    /**
     * _more_
     *
     * @param h _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public String showDialogError(String h, String... extra) {
        return showDialogError(h, true, extra);
    }

    /**
     * _more_
     *
     * @param h _more_
     * @param cleanString _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public String showDialogError(String h, boolean cleanString,
                                  String... extra) {
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
	//Remove url args
	s = s.replaceAll("\\?[^ \"]+","---");
        s = s.replaceAll("(?i)<span *>", "SPANOPEN");
        s = s.replaceAll("(?i)</span *>", "SPANCLOSE");	
        s = s.replaceAll("(?i)<pre *>", "PREOPEN");
        s = s.replaceAll("(?i)</pre *>", "PRECLOSE");
        s = HU.entityEncode(s);
        s = s.replace("&#32;", " ");
        s = s.replace("&#60;p&#62;", "<p>");
        s = s.replace("&#60;br&#62;", "<br>");
        s = s.replace("&#38;nbsp&#59;", "&nbsp;");
        s = s.replaceAll("SPANOPEN", "<span>");
        s = s.replaceAll("SPANCLOSE", "</span>");	
        s = s.replaceAll("PREOPEN", "<pre>");
        s = s.replaceAll("PRECLOSE", "</pre>");
        return s;
    }


    /**
     *
     * @param msg _more_
     * @param extra _more_
     * @param icon _more_
     * @param showClose _more_
     *
     * @return _more_
     */
    public String getDialog(String msg, String[] extra, String icon,
                            boolean showClose) {
        msg = msg.replaceAll("\n", "<br>").replaceAll("&#10;", "<br>");

        if ((extra != null) && (extra.length > 0)) {
            String tmp = "";
            for (String e : extra) {
                tmp += e;
            }
            msg += HU.div(tmp,HU.cssClass("ramadda-message-extra"));
        }


        String html = showClose
                      ? HU.jsLink(HU.onMouseClick("hide('messageblock')"),
                                  getIconImage(Constants.ICON_CLOSE))
                      : "&nbsp;";
        String clazz = (String)Utils.multiEquals(icon,"ramadda-message-plain",
						 Constants.ICON_DIALOG_INFO,
						 "ramadda-message-info",
						 Constants.ICON_DIALOG_ERROR,
						 "ramadda-message-error",
						 Constants.ICON_DIALOG_QUESTION,
						 "ramadda-message-question",						 
						 Constants.ICON_DIALOG_WARNING,
						 "ramadda-message-warning");
        String faClazz = (String)Utils.multiEquals(icon,"text-primary",
						   Constants.ICON_DIALOG_INFO,
						   "text-primary",
						   Constants.ICON_DIALOG_ERROR,
						   "text-danger",
						   Constants.ICON_DIALOG_WARNING,
						   "text-warning");
        StringBuilder sb      = new StringBuilder();
        sb.append(HU.open(HU.TAG_DIV, "class",
                          "ramadda-message " + clazz));
        sb.append("<table width=100%><tr valign=top>");
        if (icon != null) {
            sb.append("<td width=5%><div class=\"ramadda-message-icon\">");
            sb.append(getIconImage(icon + " " + faClazz, "style",
                                   "xfont-size:24pt;"));
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
                      ? HU.jsLink(HU.onMouseClick("hide('messageblock')"),
                                  getIconImage(Constants.ICON_CLOSE))
                      : "&nbsp;";

        StringBuilder sb = new StringBuilder();
        sb.append(HU.open(HU.TAG_DIV, "class", "ramadda-message ", "id",
                          "messageblock"));
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

            if (request.exists(PROP_NOSTYLE) || noStyle) {
                return pageStyle;
            }
            List<Metadata> metadataList =
                getMetadataManager().findMetadata(request, entry,
                    new String[] { ContentMetadataHandler.TYPE_PAGESTYLE },
                    true);
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
                    if ((type.equals("file") || type.equals("anyfile")) && !entry.isGroup()) {
                        theMetadata = metadata;
                        break;
                    }
                    if ((type.equals("folder") || type.equals("anygroup")) && entry.isGroup()) {
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

            boolean canEdit = getAccessManager().canDoEdit(request, entry);
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
	
        return getEntryIconImage(request,entry)+ " "  + getBreadCrumbs(request, entry);
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
        return getBreadCrumbs(request, entry, null, null, 80,-1);
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
        return getBreadCrumbs(request, entry, stopAt, null, 80,-1);
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
                                 RequestUrl requestUrl, int lengthLimit,int maxCount)
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
	    if(maxCount>=0 && parents.size()>=maxCount-1) {
		break;
	    }
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
                String url = HU.url(requestUrl.toString(), ARG_ENTRYID,
                                    ancestor.getId());
                breadcrumbs.add(0, HU.href(url, name));
            }
        }
        String lastLink = null;


        if (target != null) {
            lastLink = HU.href(getEntryManager().getEntryUrl(request, entry),
                               entry.getLabel(), targetAttr);

        } else {
            if (requestUrl == null) {
                lastLink = getEntryManager().getTooltipLink(request, entry,
                        entry.getLabel(), null);
            } else {
                String url = HU.url(requestUrl.toString(), ARG_ENTRYID,
                                    entry.getId());
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
                                 Appendable title,Appendable entryMenu)
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
        String headerLabel =
            HU.href(getEntryManager().getEntryUrl(request, entry),
                    getEntryIconImage(request,entry)
		    + " "
                    + getEntryDisplayName(entry));



        String        menuId = HU.getUniqueId("menulink");
        String menuLinkImg =      HU.img("fas fa-caret-down");
	//                   + HU.cssClass(
	//                       "ramadda-breadcrumbs-menu-button ramadda-clickable"));

        String menuLink =
            HU.span(menuLinkImg,
                   HU.attr("id", menuId) + HU.attr("title", "Entry menu")+
		     HU.cssClass(
                       "ramadda-breadcrumbs-menu-button ramadda-clickable") +
                    HU.onMouseClick(HU.call("RamaddaUtils.showEntryPopup",
                                            HU.squote(menuId),
                                            HU.squote(entry.getId()),
                                            HU.squote(headerLabel))));

	entryMenu.append(menuLink);
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
                HU.div(sb, menuLink, HU.cssClass("ramadda-breadcrumbs-menu"));
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
                //                sb.append("<td align=right width=100>");
            }
            sb.append(toolbar);
            if (doTable) {
                sb.append("</td></tr></table>");
            }
            sb.append("</div>");
            header = sb.toString();
        } else {
            if ( !request.isAnonymous()) {
                header = menuLink;
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
                                      getIconImage(link.getIcon(), "title",
                                          link.getLabel()));
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
        String sep = HU.div("", HU.cssClass(CSS_CLASS_MENUBUTTON_SEPARATOR));


        NamedValue linkAttr = arg("linkAttributes",
                                  HU.cssClass(CSS_CLASS_MENUBUTTON));
        for (Link link : links) {
            if (link.isType(OutputType.TYPE_OTHER)) {
                categoryMenu =
                    getEntryManager().getEntryActionsTable(request, entry,
                        OutputType.TYPE_OTHER, links);
                String categoryName = link.getOutputType().getCategory();
                categoryMenu = HU.makePopup(null, categoryName,
                                            categoryMenu.toString(),
                                            linkAttr);

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
            menuItems.add(HU.makePopup(null, menuName, entryMenu, linkAttr));

        }

        if (pageStyle.okToShowMenu(entry, PageStyle.MENU_EDIT)
                && (editMenu != null)) {
            if (menuItems.size() > 0) {
                menuItems.add(sep);
            }
            menuItems.add(HU.makePopup(null, "Edit", editMenu, linkAttr));
        }

        if (pageStyle.okToShowMenu(entry, PageStyle.MENU_FEEDS)
                && (exportMenu != null)) {
            if (menuItems.size() > 0) {
                menuItems.add(sep);
            }
            menuItems.add(HU.makePopup(null, "Links", exportMenu, linkAttr));
        }

        if (pageStyle.okToShowMenu(entry, PageStyle.MENU_VIEW)
                && (viewMenu != null)) {
            if (menuItems.size() > 0) {
                menuItems.add(sep);
            }
            menuItems.add(HU.makePopup(null, "View", viewMenu, linkAttr));
        }

        if (pageStyle.okToShowMenu(entry, PageStyle.MENU_OTHER)
                && (categoryMenu != null)) {
            if (menuItems.size() > 0) {
                menuItems.add(sep);
            }
            menuItems.add(categoryMenu);
        }

        String leftTable;
        leftTable =
            HU.table(HU.row(HU.cols(Utils.toStringArray(menuItems)),
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
	if(request.isMobile() && breadcrumbs.size()>0) {
	    HU.div(sb,breadcrumbs.get(breadcrumbs.size()-1),HU.style("margin-left:5px;"));
	} else {
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

    }


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

    public String getEntryIconImage(Request request, Entry entry) throws Exception {
	return HU.img(getIconUrl(request, entry),"",HU.attr("width",ICON_WIDTH));
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
            HU.url(request.getAbsoluteUrl(getRepository().URL_ENTRY_SHOW),
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
                                      "Comments:(" + comments.size() + ")"
                                      + getIconImage(link.getIcon(), "title",
                                          link.getLabel()));

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

        sb.append("</td><td><p></td></tr></table>");

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
        boolean       canEdit = getAccessManager().canDoEdit(request, entry);
        boolean canComment = getAccessManager().canDoComment(request, entry);

        StringBuilder sb      = new StringBuilder();
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
							    entry.getId(),
							    ARG_AUTHTOKEN,
							    getAuthManager().getAuthToken(request.getSessionId()),
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
            String byLine =
                HU.span(
                    "Posted by " + comment.getUser().getLabel(),
                    HU.cssClass(CSS_CLASS_COMMENT_COMMENTER)) + " @ "
                        + HU.span(
                            getDateHandler().formatDate(
                                request, comment.getDate()), HU.cssClass(
                                CSS_CLASS_COMMENT_DATE)) + HU.space(1)
                                    + deleteLink;
            content.append(HU.open(HU.TAG_DIV,
                                   HU.cssClass(CSS_CLASS_COMMENT_INNER)));
            content.append(comment.getComment());
            content.append(HU.br());
            content.append(byLine);
            content.append(HU.close(HU.TAG_DIV));
            sb.append(
                HU.div(HU.makeShowHideBlock(
                    HU.span(
                        comment.getSubject(),
                        HU.cssClass(
                            CSS_CLASS_COMMENT_SUBJECT)), content.toString(),
                                true, ""), theClass));
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
	templateMap = null;
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
        String popupLink =
            HtmlUtils.div(HU.img("fas fa-layer-group"),
                          HtmlUtils.cssClass("ramadda-popup-link"));
        mapInfo.addRightSide(HU.makePopup(null, popupLink,
                                          rightSide.toString(),
                                          arg("width", "500px"),
                                          arg("animate", false),
                                          arg("my", "right top"),
                                          arg("at", "left top"),
                                          arg("draggable", true),
                                          arg("inPlace", true),
                                          arg("header", true),
                                          arg("fit", true),
                                          arg("sticky", true)));
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
            getRepository().getExtEditor().getTypeHandlerSelectors(request,
                true, includeNonFiles, null);

        HtmlUtils.Selector selector = new HtmlUtils.Selector(
                                          HtmlUtils.space(2) + "Find match",
                                          TypeHandler.TYPE_FINDMATCH,
                                          getRepository().getIconUrl(
                                              "/icons/blank.gif"), 0, 0,
                                                  false);
        selector.setAttr(" style=\"padding:6px;\" ");
        items.add(0, selector);
        String selected = (typeHandler != null)
                          ? typeHandler.getType()
                          : TypeHandler.TYPE_FINDMATCH;
        if (true) {
            return HU.select(ARG_TYPE, items, selected);
        }

        return repository.makeTypeSelect(items, request, ARG_TYPE,"",false, selected,
                                         false, null,false);
    }

    public void makeEntrySection(Request request, Entry entry, Appendable sb,
                                 String title,String contents)
            throws Exception {
	entrySectionOpen(request, entry, sb,title);
	sb.append(contents);
	entrySectionClose(request, entry, sb);
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
     *
     * @param request _more_
     * @param entry _more_
     * @param title _more_
     * @param s _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public StringBuilder makeEntryPage(Request request, Entry entry,
                                       String title, String s)
            throws Exception {
        StringBuilder sb = new StringBuilder();
        if (entry != null) {
            entrySectionOpen(request, entry, sb, title, false);
        } else {
            sectionOpen(request, sb, title, false);
        }
        sb.append(s);
        if (entry != null) {
            entrySectionClose(request, entry, sb);
        } else {
            sectionClose(request, sb);
        }

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
                            boolean showLine) {

	try {
	    sb.append(HU.sectionOpen(null, showLine));
	    if (title != null) {
		HU.sectionTitle(sb, title);
	    }
	} catch(Exception exc) {
	    throw new RuntimeException(exc);
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
    public void sectionClose(Request request, Appendable sb)  {
	try {
	    sb.append(HU.sectionClose());
	} catch(Exception exc) {
	    throw new RuntimeException(exc);
	}

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
                                 String title, boolean force)
            throws Exception {
        entrySectionOpen(request, entry, null, sb, title, force);
    }


    public void entrySectionOpen(Request request, Entry entry,
                                 String entryLabel, Appendable sb,
                                 String title, boolean force)
            throws Exception {

        if (!force && request.isEmbedded()) {
            return;
        }
        sb.append(HU.sectionOpen(null, false));
        if (entry != null) {
            String label = Utils.stringDefined(entryLabel)
                           ? entryLabel
		: getEntryDisplayName(entry);
            label = HU.href(getEntryManager().getEntryUrl(request, entry),
                            label);
            HU.sectionTitle(sb, label);
            if (Utils.stringDefined(title)) {
                sb.append(
                    HU.div(HU.div(
                        msg(title),
                        HU.cssClass("ramadda-heading")), HU.cssClass(
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
	entrySectionClose(request, entry, sb, false);
    }

    public void entrySectionClose(Request request, Entry entry, Appendable sb,boolean force)
	throws Exception {	
        if (!force &&request.isEmbedded()) {
            return;
        }
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
    public static void main(String[] args) throws Exception {}

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
    private static String CDNROOT = null;
    private static String CDNHTDOCS = null;    

    /**
      * @return _more_
     */
    private String getAckMessage() {
        if (ackMessage == null) {
            ackMessage = applyBaseMacros(ACK_MESSAGE);
        }

        return ackMessage;
    }


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
        String libpath;
        String path;
        if (getRepository().getCdnOk()) {
            path    = getCdn();
            libpath = getCdn();
        } else {
            path = getRepository().getUrlBase() + "/"
                   + RepositoryUtil.getHtdocsVersion();
            libpath = getRepository().getUrlBase() + "/"
                      + RepositoryUtil.getHtdocsVersion();
        }


        String root       = getRepository().getUrlBase();
        String htdocsBase = makeHtdocsUrl("");

        s = s.replace("${ramadda.bootstrap.version}", bootstrapVersion);
        String now = htdocsBase + (new Date().getTime());

        s = s.replace("${now}", now);
        s = s.replace("${htdocs}", htdocsBase);
        s = s.replace("${cdnpath}", path);
        s = s.replace("${cdnlibpath}", libpath).replace(
            "${root}", root).replace(
            "${baseentry}", getEntryManager().getRootEntry().getId()).replace(
            "${min}", mini).replace("${dotmin}", dotmini);

        return s;
    }

    public String getArk(Request request, Entry entry,boolean showShort) {
	String naan = getRepository().getProperty("ramadda.naan",null);
	if(naan==null) return null;
	String id = HU.getUniqueId("ark");
	String url = "https://n2t.net/ark:/" + naan +"/" + entry.getId().replace("-","_");
	String label = showShort?"https://n2t.net/ark/...":url;
	return HU.getIconImage("fas fa-copy")+" " +HU.span(label,HU.attrs("id",id,"copy-message","ARK ID has been copied","data-copy",url))+
	    HU.script("Utils.initCopyable('#"+id+"');");
    }

    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     */
    public String getCdnPath(String path) {
	return getCdnPath(path,path);
    }

    public String getCdnPath(String fullPath,String path) {	
        if (getRepository().getCdnOk()) {
	    if(fullPath.startsWith("/src"))
		return getCdnRoot() + fullPath;
            return getCdn() + fullPath;
        } else {
            return getRepository().getHtdocsUrl(path);
        }
    }



    private String getCdnRoot() {
	getCdn();
	return CDNROOT;
    }

    /**
     *  @return _more_
     */
    private String getCdn() {
        if (CDNHTDOCS == null) {
            CDNROOT = "https://cdn.jsdelivr.net/gh/geodesystems/ramadda@"  + RepositoryUtil.getVersion();
            CDNHTDOCS = CDNROOT + "/src/org/ramadda/repository/htdocs";
            if (getRepository().getCdnOk()) {
                System.err.println("RAMADDA: Using CDN:" + CDNROOT);
            }
        }
        return CDNHTDOCS;
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
	String event = HU.onMouseClick(OutputHandler.getSelectEvent(request, selectId, false, "", entry));
        sb.append(HU.hidden(selectId + "_hidden", ((entry != null)
                ? entry.getId()
                : ""), HU.id(selectId + "_hidden")));
        sb.append(HU.formEntry(msgLabel(label),
                               HU.disabledInput(selectId, ((entry != null)
                ? entry.getFullName()
							   : ""), HU.id(selectId) + event + HU.style("cursor:pointer;") +HU.SIZE_60 + ((entry == null)
                ? HU.cssClass(CSS_CLASS_REQUIRED_DISABLED)
						       : "")) + HU.space(2) + baseSelect + extra));
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
            new String[]{ContentMetadataHandler.TYPE_ALIAS},false);
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
