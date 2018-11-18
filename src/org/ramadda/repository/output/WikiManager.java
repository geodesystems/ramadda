/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

package org.ramadda.repository.output;


import org.ramadda.repository.Association;
import org.ramadda.repository.Constants;
import org.ramadda.repository.Entry;
import org.ramadda.repository.EntryManager;
import org.ramadda.repository.Link;
import org.ramadda.repository.PageDecorator;
import org.ramadda.repository.Repository;
import org.ramadda.repository.RepositoryBase;
import org.ramadda.repository.RepositoryManager;
import org.ramadda.repository.RepositoryUtil;
import org.ramadda.repository.Request;
import org.ramadda.repository.Result;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.metadata.MetadataType;
import org.ramadda.repository.search.SearchInfo;
import org.ramadda.repository.search.SearchManager;
import org.ramadda.repository.search.SearchProvider;
import org.ramadda.repository.search.SpecialSearch;
import org.ramadda.repository.type.LocalFileTypeHandler;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.repository.util.DateArgument;
import org.ramadda.repository.util.ServerInfo;
import org.ramadda.util.BufferMapList;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;

/**
 * Provides wiki text processing services
 */
public class WikiManager extends RepositoryManager implements WikiConstants,
        WikiUtil.WikiPageHandler {

    /** layout attributes */
    public static final String ATTRS_LAYOUT = attrs(ATTR_TEXTPOSITION,
                                                  POS_LEFT);

    /** list of import items for the text editor menu */
    //J--
    public static final WikiTagCategory[] WIKITAGS = {
        new WikiTagCategory("Information",
                            new WikiTag(WIKI_TAG_INFORMATION, attrs(ATTR_DETAILS, "false",ATTR_SHOWTITLE,"false")),
                            new WikiTag(WIKI_TAG_NAME), 
                            new WikiTag(WIKI_TAG_DESCRIPTION),
                            new WikiTag(WIKI_TAG_RESOURCE, attrs(ATTR_TITLE,"",ATTR_INCLUDEICON,"true")), 
                            new WikiTag(WIKI_TAG_DATERANGE, attrs(ATTR_FORMAT,RepositoryBase.DEFAULT_TIME_FORMAT)),
                            new WikiTag(WIKI_TAG_DATE_FROM, attrs(ATTR_FORMAT,RepositoryBase.DEFAULT_TIME_FORMAT)),
                            new WikiTag(WIKI_TAG_DATE_TO,attrs(ATTR_FORMAT,RepositoryBase.DEFAULT_TIME_FORMAT)), 
                            new WikiTag(WIKI_TAG_DATE_CREATE,attrs(ATTR_FORMAT,RepositoryBase.DEFAULT_TIME_FORMAT)), 
                            new WikiTag(WIKI_TAG_DATE_CHANGE,attrs(ATTR_FORMAT,RepositoryBase.DEFAULT_TIME_FORMAT)), 

                            new WikiTag(WIKI_TAG_LINK, attrs(ATTR_TITLE,"","button","false")),
                            new WikiTag(WIKI_TAG_HTML),
                            new WikiTag(WIKI_TAG_SIMPLE, attrs(ATTR_TEXTPOSITION, POS_LEFT)),
                            new WikiTag(WIKI_TAG_IMPORT, attrs(ATTR_ENTRY,"")),
                            new WikiTag(WIKI_TAG_EMBED, attrs(ATTR_ENTRY,"",ATTR_SKIP_LINES,"0",ATTR_MAX_LINES,"1000",ATTR_FORCE,"false",ATTR_MAXHEIGHT,"300",ATTR_ANNOTATE,"false")),
                            new WikiTag(WIKI_TAG_FIELD, attrs("name", ""))),
        new WikiTagCategory("Layout", 
                            new WikiTag(WIKI_TAG_LINKS, attrs(
                                                              "_" + ATTR_SHOWTITLE,"",
                                                              "_" + ATTR_TITLE,"",
                                                              "_"+ ATTR_INCLUDEICON,"true",
                                                              "_" + ATTR_INNERCLASS,"",
                                                              "_"+ ATTR_LINKRESOURCE, "true", 
                                                              "_"+ ATTR_SEPARATOR, " | ", 
                                                              "_"+ ATTR_TAGOPEN, "", 
                                                              "_"+ ATTR_TAGCLOSE, "")), 
                            new WikiTag(WIKI_TAG_LIST), 
                            new WikiTag(WIKI_TAG_TABS, attrs(
                                                             ATTR_TAG, WIKI_TAG_HTML, ATTR_SHOWLINK, "true", ATTR_INCLUDEICON, "false") + ATTRS_LAYOUT), 
                            new WikiTag(WIKI_TAG_GRID, attrs(
                                                                  ATTR_TAG, WIKI_TAG_LINKS, "inner-height","100", ATTR_COLUMNS, "3", ATTR_INCLUDEICON, "true", "weights","","doline","true")), 
                            new WikiTag(WIKI_TAG_TREE, attrs(
                                                             ATTR_DETAILS, "true")), 
                            new WikiTag(WIKI_TAG_TREEVIEW, attrs(ATTR_WIDTH,"750", ATTR_HEIGHT,"500")), 
                            new WikiTag(WIKI_TAG_ACCORDIAN, attrs(
                                                                  ATTR_TAG, WIKI_TAG_HTML, ATTR_COLLAPSE, "false", "border", "0", ATTR_SHOWLINK, "true", ATTR_INCLUDEICON, "false") + ATTRS_LAYOUT), 
                            //                            new WikiTag(WIKI_TAG_GRID), 
                            new WikiTag(WIKI_TAG_TABLE), 
                            new WikiTag(WIKI_TAG_RECENT, attrs(
                                                               ATTR_DAYS, "3")), 
                            new WikiTag(WIKI_TAG_APPLY, attrs(APPLY_PREFIX
                                                              + "tag", WIKI_TAG_HTML, APPLY_PREFIX
                                                              + "layout", "table", APPLY_PREFIX
                                                              + "columns", "1", APPLY_PREFIX
                                                              + "header", "", APPLY_PREFIX
                                                              + "footer", "", APPLY_PREFIX
                                                              + "border", "0", APPLY_PREFIX
                                                              + "bordercolor", "#000"))),
        
        new WikiTagCategory("Earth",            
                            new WikiTag(WIKI_TAG_MAP,
                                        attrs(ATTR_WIDTH, "100%", ATTR_HEIGHT, "400",
                                              ATTR_LISTENTRIES, "true", ATTR_DETAILS, "false",
                                              "showCheckbox", "true",
                                              "showSearch", "false",
                                              ATTR_ICON, "#/icons/dots/green.png", ARG_MAP_ICONSONLY,
                                              "false")), 
                            /*
                              keep the implementation around for legacy wikis but don't show this anymore
                              as we can do {{map entries="this"}}
                            new WikiTag(WIKI_TAG_MAPENTRY,
                                        attrs(ATTR_WIDTH, "100%", ATTR_HEIGHT, "400",
                                              ATTR_DETAILS,
                                              "false")), 
                            */
                            new WikiTag(WIKI_TAG_EARTH,
                                        attrs(ATTR_WIDTH, "400", ATTR_HEIGHT, "400",
                                              ATTR_LISTENTRIES, "false"))),
        
        new WikiTagCategory("Images",
                            new WikiTag(WIKI_TAG_IMAGE,
                                        attrs(ATTR_SRC, "", ATTR_ALIGN,
                                              "left|center|right")), 
                            new WikiTag(WIKI_TAG_GALLERY,
                                        attrs(ATTR_WIDTH, "200", ATTR_COLUMNS, "3",
                                              ATTR_POPUP, "true", ATTR_THUMBNAIL, "true",
                                              ATTR_CAPTION, "Figure ${count}: ${name}",
                                              ATTR_POPUPCAPTION,
                                              "over")), 
                            new WikiTag(WIKI_TAG_SLIDESHOW,
                                        attrs(ATTR_TAG, WIKI_TAG_SIMPLE,
                                              ATTR_SHOWLINK, "true") + ATTRS_LAYOUT
                                        + attrs(ATTR_WIDTH, "400",
                                                ATTR_HEIGHT,
                                                "270")), 
                            new WikiTag(WIKI_TAG_PLAYER, attrs("loopdelay","1000","loopstart","false"))),
        new WikiTagCategory("Misc",
                            new WikiTag(WIKI_TAG_CALENDAR, attrs(ATTR_DAY, "false")),
                            new WikiTag(WIKI_TAG_TIMELINE, attrs(ATTR_HEIGHT, "150")),
                            new WikiTag(WIKI_TAG_GRAPH,
                                        attrs(ATTR_WIDTH, "400", ATTR_HEIGHT,
                                              "400")), 
                            new WikiTag(WIKI_TAG_COMMENTS),
                            new WikiTag(WIKI_TAG_TAGCLOUD,
                                        attrs("type", "", "threshold",
                                              "0")), 
                            new WikiTag(WIKI_TAG_PROPERTIES, attrs("message","","metadata.types","",ATTR_METADATA_INCLUDE_TITLE,"true")),
                            new WikiTag(WIKI_TAG_BREADCRUMBS),
                            new WikiTag(WIKI_TAG_TOOLS),
                            new WikiTag(WIKI_TAG_TOOLBAR),
                            new WikiTag(WIKI_TAG_LAYOUT),
                            new WikiTag(WIKI_TAG_MENU),
                            new WikiTag(WIKI_TAG_ENTRYID),
                            new WikiTag(WIKI_TAG_SEARCH,
                                        attrs(ATTR_TYPE, "", 
                                              ATTR_FIELDS,"",
                                              ATTR_METADATA,"",
                                              ARG_MAX, "100",
                                              ARG_SEARCH_SHOWFORM, "false",
                                              SpecialSearch.ATTR_TABS,
                                              SpecialSearch
                                              .TAB_LIST)), 
                            new WikiTag(WIKI_TAG_UPLOAD,
                                        attrs(ATTR_TITLE,
                                              "Upload file",
                                              ATTR_INCLUDEICON,
                                              "false")), 
                            new WikiTag(WIKI_TAG_ROOT)),
        
        new WikiTagCategory("Display",
                            new WikiTag(WIKI_TAG_GROUP, "Display group",
                                        attrs("layoutType", "table", 
                                              "layoutColumns", "1",
                                              ATTR_SHOWMENU, "false")), 
                            new WikiTag(WIKI_TAG_DISPLAY, "Search form",
                                        attrs(ATTR_TYPE, "entrylist", ATTR_WIDTH, "800",
                                              ATTR_HEIGHT, "400", 
                                              "orientation", "vertical",
                                              ATTR_SHOWFORM, "true", 
                                              ATTR_FORMOPEN, "true",
                                              ATTR_LAYOUTHERE, "true", 
                                              ATTR_SHOWMENU, "true",
                                              ATTR_SHOWTITLE,    "true")), 
                            new WikiTag(WIKI_TAG_DISPLAY, "Map",
                                        attrs(ATTR_TYPE, "map", 
                                              ATTR_WIDTH, "100%",
                                              ATTR_HEIGHT, "400", 
                                              ATTR_LAYOUTHERE, "true", 
                                              ATTR_SHOWMENU, "true",
                                              ATTR_SHOWTITLE, "true")), 
                            new WikiTag(WIKI_TAG_DISPLAY,
                                        "Line chart",
                                        attrs(ATTR_TYPE, "linechart", ATTR_WIDTH, "800",
                                              ATTR_HEIGHT, "400", "fields", "",
                                              ATTR_LAYOUTHERE, "true", 
                                              ATTR_SHOWMENU, "true", 
                                              ATTR_SHOWTITLE, "true")),
                            new WikiTag(WIKI_TAG_DISPLAY,
                                        "Table",
                                        attrs(ATTR_TYPE, "table", ATTR_HEIGHT, "400", "fields", "",
                                              ATTR_LAYOUTHERE, "true", 
                                              ATTR_SHOWMENU, "true", 
                                              ATTR_SHOWTITLE, "true")),
                            new WikiTag(WIKI_TAG_DISPLAY,
                                        "Summary Stats",
                                        attrs(ATTR_TYPE, "stats", "loadData","false", "fields", "",
                                              ATTR_LAYOUTHERE, "true", 
                                              ATTR_SHOWMENU, "true", 
                                              ATTR_SHOWTITLE, "true"))
                            )
    };
    //J++



    /** output type */
    public static final OutputType OUTPUT_WIKI = new OutputType("Wiki",
                                                     "wiki.view",
                                                     OutputType.TYPE_VIEW,
                                                     "", ICON_WIKI);



    /**
     * ctor
     *
     * @param repository the repository
     */
    public WikiManager(Repository repository) {
        super(repository);
    }



    /**
     * Get a wiki property value
     *
     * @param wikiUtil The wiki util
     * @param property the property
     *
     * @return the value
     */
    public String getWikiPropertyValue(WikiUtil wikiUtil, String property, String []notTags) {

        try {
            Entry   entry   = (Entry) wikiUtil.getProperty(ATTR_ENTRY);
            Request request = (Request) wikiUtil.getProperty(ATTR_REQUEST);
            //Check for infinite loop
            property = property.trim();
            if (property.length() == 0) {
                return "";
            }

            property = property.replaceAll("(?m)^\\s*//.*?$", "");
            property = property.replaceAll(".*<p></p>[\\n\\r]+", "");
            property = property.replaceAll("\\n", " ");
            property = property.replaceAll("\r", "");



            List<String> toks  = StringUtil.splitUpTo(property, " ", 2);
            String       stoks = toks.toString();
            if (toks.size() == 0) {
                return "<b>Incorrect import specification:" + property
                       + "</b>";
            }
            String tag       = toks.get(0);
            if(notTags!=null) {
                for(String notTag: notTags) {
                    if(notTag.equals(tag)) return "";
                }
            }

            String remainder = "";
            if (toks.size() > 1) {
                remainder = toks.get(1);
            }


            Entry theEntry = entry;
            if (tag.equals(WIKI_TAG_IMPORT)) {
                //Old style
                if (remainder.indexOf("=") < 0) {
                    toks = StringUtil.splitUpTo(remainder, " ", 3);
                    if (toks.size() < 2) {
                        return "<b>Incorrect import specification:"
                               + property + "</b>";
                    }
                    String id = toks.get(0).trim();
                    tag = toks.get(1).trim();
                    if (toks.size() == 3) {
                        remainder = toks.get(2);
                    } else {
                        remainder = "";
                    }
                    theEntry = findWikiEntry(request, wikiUtil, id, entry);
                    if (theEntry == null) {
                        return "<b>Could not find entry&lt;" + id
                               + "&gt;</b>";
                    }
                }
            }





            Hashtable tmpProps = StringUtil.parseHtmlProperties(remainder);




            Hashtable props    = new Hashtable();
            for (Enumeration keys =
                    tmpProps.keys(); keys.hasMoreElements(); ) {
                Object key   = keys.nextElement();
                Object value = tmpProps.get(key);
                props.put(key, value);
                if (key instanceof String) {
                    String lowerCaseKey = ((String) key).toLowerCase();
                    props.put(lowerCaseKey, value);
                }
            }


            String entryId = (String) props.get(ATTR_ENTRY);
            if (Utils.stringDefined(entryId)) {
                theEntry = findEntryFromId(request, entry, wikiUtil,
                                           entryId.trim());
                if (theEntry == null) {
                    return getMessage(props, "Unknown entry:" + entryId);
                }
            }

            //TODO: figure out a way to look for infinte loops
            /*
            String propertyKey = theEntry.getId() + "_" + property;
            if (request.getExtraProperty(propertyKey) != null) {
                return "<b>Detected circular wiki import:" + property +
                    "<br>For entry:" +  theEntry.getId()
                    + "</b>";
            }
            request.putExtraProperty(propertyKey, property);
            */


            addWikiLink(wikiUtil, theEntry);
            String include = handleWikiImport(wikiUtil, request, entry,
                                 theEntry, tag, props);
            if (include != null) {
                return include;
            }

            return wikiUtil.getPropertyValue(property);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param wikiUtil _more_
     * @param entryId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry findEntryFromId(Request request, Entry entry,
                                  WikiUtil wikiUtil, String entryId)
            throws Exception {

        Entry theEntry = null;
        int   barIndex = entryId.indexOf("|");

        if (barIndex >= 0) {
            entryId = entryId.substring(0, barIndex);
        }
        if (entryId.equals(ID_THIS)) {
            theEntry = entry;
        }

        if (entryId.startsWith("alias:")) {
            String alias = entryId.substring("alias:".length());

            return getEntryManager().getEntryFromAlias(request, alias);
        }


        if (entryId.startsWith("child:")) {
            String tok = entryId.substring("child:".length());
            if (tok.startsWith("type:")) {
                tok     = tok.substring("type:".length());
                request = request.cloneMe();
                request.put(ARG_TYPE, tok);
                List<Entry> children = getEntryManager().getChildren(request,
                                           entry);
                if (children.size() > 0) {
                    return children.get(0);
                }

                return null;
            }
            List<Entry> children = getEntryManager().getChildren(request,
                                       entry);
            if (children.size() > 0) {
                return children.get(0);
            }

            return null;
        }


        if (entryId.equals("link") || entryId.startsWith("link:")) {

            String type = StringUtil.findPattern(entryId, ":(.*)$");
            //            System.err.println("Link: " + type);
            List<Association> associations =
                getRepository().getAssociationManager().getAssociations(
                    request, entry.getId());
            //            System.err.println("associations: " + associations);
            for (Association association : associations) {
                Entry otherEntry =
                    getAssociationManager().getOtherEntry(request,
                        association, entry);
                //                System.err.println("other entry: " + otherEntry);
                if (otherEntry == null) {
                    continue;
                }
                if ((type != null)
                        && !otherEntry.getTypeHandler().isType(type)) {
                    System.err.println("not type");

                    continue;
                }
                theEntry = otherEntry;

                break;
            }
        }


        if (entryId.equals(ID_ROOT)) {
            theEntry = request.getRootEntry();
        }
        if ((theEntry == null) && entryId.equals(ID_PARENT)) {
            theEntry = getEntryManager().getEntry(request,
                    entry.getParentEntryId());
        }

        if ((theEntry == null) && entryId.equals(ID_GRANDPARENT)) {
            theEntry = getEntryManager().getEntry(request,
                    entry.getParentEntryId());
            if (theEntry != null) {
                theEntry = getEntryManager().getEntry(request,
                        theEntry.getParentEntryId());
            }
        }

        if (theEntry == null) {
            theEntry = getEntryManager().getEntry(request, entryId);
        }
        if (theEntry == null) {
            theEntry = findWikiEntry(request, wikiUtil, entryId, entry);
        }

        //Ugghh - I really have to unify the EntryManager find entry methods
        //Look for file path based entry id
        if ((theEntry == null) && entryId.startsWith("/")) {
            theEntry = getEntryManager().findEntryFromName(request, entryId,
                    request.getUser(), false);
        }

        //Look for relative to the current entry
        if (theEntry == null) {
            theEntry = getEntryManager().findEntryFromPath(request, entry,
                    entryId);
        }

        return theEntry;

    }

    /**
     * Get a wiki image link
     *
     *
     * @param wikiUtil The wiki util
     * @param request The request
     * @param url  the image url
     * @param entry  the entry
     * @param props  the properties
     *
     * @return the link
     *
     * @throws Exception problems
     */
    public String getWikiImage(WikiUtil wikiUtil, Request request,
                               String url, Entry entry, Hashtable props)
            throws Exception {


        boolean       inDiv = Misc.getProperty(props, "inDiv", true);
        String        align = (String) props.get(ATTR_ALIGN);
        String        width = (String) props.get(ATTR_WIDTH);
        StringBuilder extra = new StringBuilder();

        //imagewidth says to resize and cache the image on the server
        //If its defined then add it to the URL
        int imageWidth = Misc.getProperty(props, ATTR_IMAGEWIDTH, -1);
        if (imageWidth > 0) {
            url = url + "&" + ARG_IMAGEWIDTH + "=" + imageWidth;
        }
        if (width != null) {
            HtmlUtils.attr(extra, HtmlUtils.ATTR_WIDTH, width);
        }

        if ( !inDiv && (align != null)) {
            //            extra.append(HtmlUtils.style("align:" + align + ";"));
            //            extra.append(HtmlUtils.attr("align", align));
        }

        String alt = (String) props.get(HtmlUtils.ATTR_ALT);
        if (alt == null) {
            alt = (String) props.get(HtmlUtils.ATTR_TITLE);
        }

        if (alt == null) {
            String name = getEntryDisplayName(entry);
            if ((name != null) && !name.isEmpty()) {
                alt = name;
            }
        }

        if (alt != null) {
            HtmlUtils.attr(extra, HtmlUtils.ATTR_ALT, alt);
        }

        if (wikiUtil != null) {
            String imageClass = (String) wikiUtil.getProperty("image.class");
            if (imageClass != null) {
                HtmlUtils.cssClass(extra, imageClass);
            } else {
                HtmlUtils.cssClass(extra, "wiki-image");
            }
        }


        String style       = Misc.getProperty(props, ATTR_STYLE, "");
        int    border      = Misc.getProperty(props, ATTR_BORDER, -1);
        String bordercolor = Misc.getProperty(props, ATTR_BORDERCOLOR,
                                 "#000");

        if (border > 0) {
            style += " border: " + border + "px solid " + bordercolor + ";";
        }
        String left = (String) props.get("left");
        if (left != null) {
            style += " position:absolute; left: " + left + ";";
        }

        String top = (String) props.get("top");
        if (top != null) {
            style += " position:absolute;  top: " + top + ";";
        }

        if (style.length() > 0) {
            extra.append(" style=\" " + style + "\" ");
        }


        String caption = Misc.getProperty(props, "caption", (String) null);


        String img = HtmlUtils.img(url, getEntryDisplayName(entry),
                                   extra.toString());
        boolean link = Misc.equals("true", props.get(ATTR_LINK));
        boolean linkResource = Misc.getProperty(props, ATTR_LINKRESOURCE,
                                   false);

        boolean popup = Misc.getProperty(props, ATTR_POPUP, false);
        if (link) {
            img = HtmlUtils.href(
                request.entryUrl(getRepository().URL_ENTRY_SHOW, entry), img);
        } else if (linkResource) {
            img = HtmlUtils.href(
                entry.getTypeHandler().getEntryResourceUrl(request, entry),
                img);
        } else if (popup) {
            //A hack to see if this image is an attachment (e.g. src="::*")
            String hrefUrl;
            if (url.indexOf("/metadata/view") >= 0) {
                hrefUrl = url;
            } else {
                hrefUrl = entry.getTypeHandler().getEntryResourceUrl(request,
                        entry);
            }
            StringBuilder buf = new StringBuilder();
            addImagePopupJS(request, buf, props);
            HtmlUtils.href(buf, hrefUrl, img,
                           HtmlUtils.cssClass("popup_image"));

            img = buf.toString();
        }

        StringBuilder sb    = new StringBuilder();
        String        attrs = ((align != null)
                               ? HtmlUtils.style("text-align:" + align + ";")
                               : "") + HtmlUtils.cssClass("wiki-image");


        if (inDiv) {
            HtmlUtils.open(sb, HtmlUtils.TAG_DIV, attrs);
        } else {
            HtmlUtils.open(sb, HtmlUtils.TAG_DIV, "style", ((align != null)
                    ? "float:" + align + ";"
                    : "") + " display:inline-block;text-align:center");
        }
        sb.append(img);
        if (caption != null) {
            sb.append(HtmlUtils.br());
            HtmlUtils.span(sb, caption,
                           HtmlUtils.cssClass("wiki-image-caption"));
        }
        HtmlUtils.close(sb, HtmlUtils.TAG_DIV);

        return sb.toString();

    }

    /**
     * _more_
     *
     * @param props _more_
     * @param attr _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public int getDimension(Hashtable props, String attr, int dflt) {
        try {
            String s = Misc.getProperty(props, attr, (String) null);
            if (s == null) {
                return dflt;
            }
            s = s.trim();
            boolean isPercent = s.endsWith("%");
            if (isPercent) {
                s = s.substring(0, s.length() - 1);
            }
            int v = new Integer(s).intValue();
            if (isPercent) {
                v = -v;
            }

            return v;
        } catch (Exception exc) {
            getLogManager().logError("WikiManager.getDimension", exc);

            return dflt;
        }
    }



    /**
     * Get the wiki url
     *
     * @param wikiUtil The wiki util
     * @param request The request
     * @param entry  the entry
     * @param props  the properties
     *
     * @return  the url
     *
     * @throws Exception problems
     */
    public String getWikiUrl(WikiUtil wikiUtil, Request request, Entry entry,
                             Hashtable props)
            throws Exception {

        String src      = (String) props.get("src");
        Entry  srcEntry = null;
        if (src == null) {
            srcEntry = entry;
        } else {
            src = src.trim();
            if ((src.length() == 0) || entry.getName().equals(src)) {
                srcEntry = entry;
            } else if (entry instanceof Entry) {
                srcEntry = getEntryManager().findEntryWithName(request,
                        (Entry) entry, src);
            }
        }
        if (srcEntry == null) {
            srcEntry = getEntryManager().getEntry(request, src);
        }

        if (srcEntry == null) {
            return getMessage(props, msgLabel("Could not find src") + src);
        }

        return request.entryUrl(getRepository().URL_ENTRY_SHOW, srcEntry);

    }


    /**
     * Get the wiki image link
     *
     * @param wikiUtil The wiki util
     * @param request The request
     * @param entry  the entry
     * @param props  the properties
     *
     * @return  the link
     *
     * @throws Exception problems
     */
    public String getWikiImage(WikiUtil wikiUtil, Request request,
                               Entry entry, Hashtable props)
            throws Exception {

        String src = (String) props.get(ATTR_SRC);
        if ((src == null) || (src.length() == 0)) {
            if ( !entry.isImage()) {
                return getMessage(props, msg("Not an image"));
            }

            return getWikiImage(wikiUtil, request,
                                getHtmlOutputHandler().getImageUrl(request,
                                    entry), entry, props);
        }

        String attachment = null;
        int    idx        = src.indexOf("::");
        if (idx >= 0) {
            List<String> toks = StringUtil.splitUpTo(src, "::", 2);
            if (toks.size() == 2) {
                src        = toks.get(0);
                attachment = toks.get(1).substring(1);
            }
        }
        src = src.trim();
        Entry srcEntry = null;

        if ((src.length() == 0) || entry.getName().equals(src)) {
            srcEntry = entry;
        } else if (entry instanceof Entry) {
            srcEntry = getEntryManager().findEntryWithName(request,
                    (Entry) entry, src);
        }
        if (srcEntry == null) {
            return getMessage(props, msgLabel("Could not find src") + src);
        }
        if (attachment == null) {
            if ( !srcEntry.isImage()) {
                return getMessage(props, msg("Not an image"));
            }

            return getWikiImage(wikiUtil, request,
                                getHtmlOutputHandler().getImageUrl(request,
                                    srcEntry), srcEntry, props);
        }


        if ((attachment != null) && attachment.equals("*")) {
            attachment = null;
        }
        for (Metadata metadata : getMetadataManager().getMetadata(srcEntry)) {
            MetadataType metadataType =
                getMetadataManager().findType(metadata.getType());
            if (metadataType == null) {
                continue;
            }
            String url = metadataType.getDisplayImageUrl(request, srcEntry,
                             metadata, attachment);
            if (url != null) {
                return getWikiImage(wikiUtil, request, url, srcEntry, props);
            }
        }

        return getMessage(props,
                          msgLabel("Could not find image attachment")
                          + attachment);
    }



    /**
     * Get the message
     *
     * @param props the properties
     * @param message the default
     *
     * @return  the message or the default
     */
    public String getMessage(Hashtable props, String message) {
        return Misc.getProperty(props, ATTR_MESSAGE, message);
    }



    /**
     * Get the text for the include
     *
     * @param wikiUtil The wiki util
     * @param request The request
     * @param originalEntry _more_
     * @param entry the entry
     * @param tag  the tag
     * @param props    the properties
     * @param doingApply _more_
     *
     * @return  the include text
     *
     * @throws Exception  problems
     */
    private String my_getWikiInclude(WikiUtil wikiUtil, Request request,
                                     Entry originalEntry, Entry entry,
                                     String tag, Hashtable props,
                                     boolean doingApply)
            throws Exception {
        //        boolean doingApply = tag.equals(WIKI_TAG_APPLY);
        String attrPrefix = "";
        if (doingApply) {
            attrPrefix = APPLY_PREFIX;
        }

        boolean blockPopup = Misc.getProperty(props,
                                 attrPrefix + ATTR_BLOCK_POPUP, false);
        boolean blockShow = Misc.getProperty(props,
                                             attrPrefix + ATTR_BLOCK_SHOW,
                                             false);
        String prefix = Misc.getProperty(props, attrPrefix + ATTR_PREFIX,
                                         (String) null);
        String suffix = Misc.getProperty(props, attrPrefix + ATTR_SUFFIX,
                                         (String) null);

        String result = getWikiIncludeInner(wikiUtil, request, originalEntry,
                                            entry, tag, props);
        if (result == null) {
            result = getMessage(props, "Could not process tag: " + tag);
        }
        if (result.trim().length() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        String rowLabel = Misc.getProperty(props,
                                           attrPrefix + ATTR_ROW_LABEL,
                                           (String) null);
        if (rowLabel != null) {
            return HtmlUtils.formEntry(rowLabel, result);
        }

        boolean       wrapInADiv = false;
        StringBuilder style      = new StringBuilder();
        int maxHeight = Misc.getProperty(props, "box." + ATTR_MAXHEIGHT, -1);
        style.append(Misc.getProperty(props, "box." + ATTR_STYLE, ""));
        String cssClass = Misc.getProperty(props, "box." + ATTR_CLASS,
                                           (String) null);
        if (cssClass != null) {
            wrapInADiv = true;
        }
        if (maxHeight > 0) {
            wrapInADiv = true;
            style.append(" max-height: " + maxHeight
                         + "px;  overflow-y: auto; ");
        }

        if (prefix != null) {
            makeWikiUtil(request, false).wikify(sb, prefix, null);
        }

        if (wrapInADiv) {
            HtmlUtils.open(sb, HtmlUtils.TAG_DIV, ((cssClass != null)
                    ? "class"
                    : ""), (cssClass != null)
                           ? cssClass
                           : "", "style", style.toString());
        }
        sb.append(result);
        if (wrapInADiv) {
            HtmlUtils.close(sb, HtmlUtils.TAG_DIV);
        }

        if (suffix != null) {
            makeWikiUtil(request, false).wikify(sb, suffix, null);
        }

        String blockTitle = Misc.getProperty(props,
                                             attrPrefix + ATTR_BLOCK_TITLE,
                                             "");
        if (blockPopup) {
            return getPageHandler().makePopupLink(blockTitle, sb.toString());
        }

        if (blockShow) {
            boolean blockOpen = Misc.getProperty(props,
                                    attrPrefix + ATTR_BLOCK_OPEN, true);

            return HtmlUtils.makeShowHideBlock(blockTitle, sb.toString(),
                    blockOpen, HtmlUtils.cssClass("entry-toggleblock-label"),
                    "", iconUrl("ramadda.icon.togglearrowdown"),
                    iconUrl("ramadda.icon.togglearrowright"));

        }

        return sb.toString();

    }


    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param theTag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getWikiIncludeInner(WikiUtil wikiUtil, Request request,
                                       Entry originalEntry, Entry entry,
                                       String theTag, Hashtable props)
            throws Exception {

        boolean wikify   = Misc.getProperty(props, ATTR_WIKIFY, true);

        String  criteria = Misc.getProperty(props, ATTR_IF, (String) null);
        if (criteria != null) {}

        StringBuilder sb = new StringBuilder();
        //        System.err.println("theTag:" + theTag);
        if (theTag.equals(WIKI_TAG_INFORMATION)) {
            Request myRequest = request.cloneMe();
            myRequest.put(ATTR_SHOWTITLE,
                          "" + Misc.getProperty(props, ATTR_SHOWTITLE, true));
            boolean details = Misc.getProperty(props, ATTR_DETAILS, false);
            if ( !details) {
                return entry.getTypeHandler().getEntryContent(myRequest,
                        entry, false, true).toString();
            }

            return getRepository().getHtmlOutputHandler().getInformationTabs(
                myRequest, entry, false);
        } else if (theTag.equals(WIKI_TAG_TAGCLOUD)) {
            StringBuilder tagCloud  = new StringBuilder();
            int           threshold = Misc.getProperty(props, "threshold", 0);
            getMetadataManager().doMakeTagCloudOrList(request,
                    Misc.getProperty(props, "type", ""), tagCloud, true,
                    threshold);

            return tagCloud.toString();
        } else if (theTag.equals(WIKI_TAG_COMMENTS)) {
            return getHtmlOutputHandler().getCommentBlock(request, entry,
                    false).toString();
        } else if (theTag.equals(WIKI_TAG_TOOLBAR)) {
            return getPageHandler().getEntryToolbar(request, entry);
        } else if (theTag.equals(WIKI_TAG_BREADCRUMBS)) {
            List<Entry> children = getEntries(request, originalEntry, entry,
                                       props);
            List<String> breadcrumbs =
                getPageHandler().makeBreadcrumbList(request, children, null);

            getPageHandler().makeBreadcrumbs(request, breadcrumbs, sb);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_LINK)) {
            boolean linkResource = Misc.getProperty(props, ATTR_LINKRESOURCE,
                                       false);
            String name  = getEntryDisplayName(entry);
            String title = Misc.getProperty(props, ATTR_TITLE, name);
            title = title.replace("${name}", name);
            String url;
            if (linkResource
                    && (entry.getTypeHandler().isType("link")
                        || entry.isFile() || entry.getResource().isUrl())) {
                url = entry.getTypeHandler().getEntryResourceUrl(request,
                        entry);
            } else {
                String output = Misc.getProperty(props, ATTR_OUTPUT,
                                    OutputHandler.OUTPUT_HTML.getId());
                url = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
                                       ARG_OUTPUT, output);
            }


            if (Misc.getProperty(props, "button", false)) {
                return HtmlUtils.href(
                    url, title,
                    HtmlUtils.cssClass("btn btn-primary btn-default")
                    + HtmlUtils.attr("role", "button"));
            } else {
                return HtmlUtils.href(url, title);
            }
        } else if (theTag.equals(WIKI_TAG_RESOURCE)) {
            if ( !entry.getResource().isDefined()) {
                String message = Misc.getProperty(props, ATTR_MESSAGE,
                                     (String) null);
                if (message != null) {
                    return message;
                }

                return "";
            }

            String url;
            String label;
            if (entry.getResource().isFile()) {
                url = entry.getTypeHandler().getEntryResourceUrl(request,
                        entry);
                label = Misc.getProperty(props, ATTR_TITLE, "Download");
            } else {
                url   = entry.getResource().getPath();
                label = Misc.getProperty(props, ATTR_TITLE, url);
            }
            if (Misc.getProperty(props, "url", false)) {
                return url;
            }
            boolean includeIcon = Misc.getProperty(props, ATTR_INCLUDEICON,
                                      false);
            if (includeIcon) {
                label = HtmlUtils.img(iconUrl("/icons/download.png"))
                        + HtmlUtils.space(2) + label;

                return HtmlUtils.div(HtmlUtils.href(url, label,
                        HtmlUtils.cssClass("btn btn-primary btn-lg")
                        + HtmlUtils.attr("role", "button")));

            }

            String extra = "";
            return HtmlUtils.href(url, label, extra);
        } else if (theTag.equals(WIKI_TAG_UPLOAD)) {
            Entry group = getEntryManager().findGroup(request);
            if ( !getEntryManager().canAddTo(request, group)) {
                return "";
            }
            // can't add to local file view
            if (group.getIsLocalFile()
                    || (group.getTypeHandler()
                        instanceof LocalFileTypeHandler)) {
                return "";
            }
            TypeHandler typeHandler =
                getRepository().getTypeHandler(Misc.getProperty(props,
                    ATTR_TYPE, TypeHandler.TYPE_FILE));
            if (typeHandler == null) {
                return "ERROR: unknown type";
            }
            if ( !typeHandler.getForUser()) {
                return "";
            }
            if (typeHandler.isAnyHandler()) {
                return "";
            }
            if ( !typeHandler.canBeCreatedBy(request)) {
                return "";
            }
            String img = "";
            if (Misc.getProperty(props, ATTR_INCLUDEICON, false)) {
                String icon = typeHandler.getIconProperty(null);
                if (icon == null) {
                    icon = ICON_BLANK;
                    img = HtmlUtils.img(typeHandler.iconUrl(icon), "",
                                        HtmlUtils.attr(HtmlUtils.ATTR_WIDTH,
                                            "16"));
                } else {
                    img = HtmlUtils.img(typeHandler.iconUrl(icon));
                }
            }

            String label = Misc.getProperty(props, ATTR_TITLE,
                                            typeHandler.getLabel());

            return HtmlUtils
                .href(request
                    .makeUrl(getRepository().URL_ENTRY_FORM, ARG_GROUP,
                             group.getId(), EntryManager.ARG_TYPE,
                             typeHandler.getType()), img + " " + msg(label));

        } else if (theTag.equals(WIKI_TAG_DESCRIPTION)) {
            String prefix = Misc.getProperty(props, "prefix", (String) null);
            String suffix = Misc.getProperty(props, "suffix", (String) null);
            String desc   = entry.getDescription();
            desc = desc.trim();
            desc = desc.replaceAll("\r\n\r\n", "\n<p>\n");
            if (desc.length() > 0) {
                if (prefix != null) {
                    desc = prefix + "\n" + desc;
                }
                if (suffix != null) {
                    desc += "\n" + suffix;
                }
            }
            if (wikify) {
                desc = wikifyEntry(request, entry, desc, false, null, null, wikiUtil.getNotTags());
                //                desc = makeWikiUtil(request, false).wikify(desc, null);
            }
            if (Misc.getProperty(props, "convert_newline", false)) {
                desc = desc.replaceAll("\n", "<p>");
            }

            return desc;
        } else if (theTag.equals(WIKI_TAG_LAYOUT)) {
            return getHtmlOutputHandler().makeHtmlHeader(request, entry,
                    Misc.getProperty(props, ATTR_TITLE, "Layout"));
        } else if (theTag.equals(WIKI_TAG_NAME)) {
            return getEntryDisplayName(entry);
        } else if (theTag.equals(WIKI_TAG_EMBED)) {
            if ( !entry.isFile()
                    || ( !isTextFile(entry.getResource().getPath())
                         && !Misc.getProperty(props, ATTR_FORCE, false))) {
                return "Entry isn't a text file";
            }
            StringBuilder txt = new StringBuilder("");

            InputStream fis = getStorageManager().getFileInputStream(
                                  entry.getResource().getPath());
            BufferedReader br =
                new BufferedReader(new InputStreamReader(fis));
            int     skipLines  = Misc.getProperty(props, ATTR_SKIP_LINES, 0);
            int     maxLines   = Misc.getProperty(props, ATTR_MAX_LINES,
                                     1000);
            int     maxHeight  = Misc.getProperty(props, ATTR_MAXHEIGHT, 300);
            boolean annotate   = Misc.getProperty(props, ATTR_ANNOTATE,
                                     false);
            int     lineNumber = 0;
            int     cnt        = 0;
            String  line;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (skipLines > 0) {
                    skipLines--;

                    continue;
                }
                cnt++;
                line = line.replaceAll("<", "&lt;");
                line = line.replaceAll(">", "&gt;");
                if (annotate) {
                    txt.append("#");
                    txt.append(lineNumber);
                    txt.append(": ");
                }
                txt.append(line);
                txt.append("\n");
                if (cnt > maxLines) {
                    break;
                }
            }
            IOUtil.close(fis);

            return HtmlUtils.pre(txt.toString(),
                                 HtmlUtils.style("max-height:" + maxHeight
                                     + "px; overflow-y:auto;"));
        } else if (theTag.equals(WIKI_TAG_FIELD)) {
            String name = Misc.getProperty(props, ATTR_FIELDNAME,
                                           (String) null);
            if (name != null) {
                String fieldValue =
                    entry.getTypeHandler().getFieldHtml(request, entry, name);
                if (fieldValue != null) {
                    return fieldValue;
                }

                return "Could not find field: " + name;
            } else {
                return "No name=... specified in wiki tag";
            }
        } else if (theTag.equals(WIKI_TAG_DATE_FROM)
                   || theTag.equals(WIKI_TAG_DATE_TO)
                   || theTag.equals(WIKI_TAG_DATE_CREATE)
                   || theTag.equals(WIKI_TAG_DATE_CHANGE)) {
            String format =
                Misc.getProperty(props, ATTR_FORMAT,
                                 RepositoryBase.DEFAULT_TIME_FORMAT);
            Date date;
            if (theTag.equals(WIKI_TAG_DATE_FROM)) {
                date = new Date(entry.getStartDate());
            } else if (theTag.equals(WIKI_TAG_DATE_TO)) {
                date = new Date(entry.getEndDate());
            } else if (theTag.equals(WIKI_TAG_DATE_CREATE)) {
                date = new Date(entry.getCreateDate());
            } else {
                date = new Date(entry.getChangeDate());
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            String tz = getEntryUtil().getTimezone(entry);
            if(tz!=null) {
                dateFormat.setTimeZone(TimeZone.getTimeZone(tz));
            } else {

                dateFormat.setTimeZone(RepositoryUtil.TIMEZONE_DEFAULT);
            }

            return dateFormat.format(date);

        } else if (theTag.equals(WIKI_TAG_DATERANGE)) {
            String format =
                Misc.getProperty(props, ATTR_FORMAT,
                                 RepositoryBase.DEFAULT_TIME_FORMAT);
            Date             date1      = new Date(entry.getStartDate());
            Date             date2      = new Date(entry.getEndDate());
            SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            dateFormat.setTimeZone(RepositoryUtil.TIMEZONE_DEFAULT);

            String separator = Misc.getProperty(props, ATTR_SEPARATOR,
                                   " -- ");

            return dateFormat.format(date1) + separator
                   + dateFormat.format(date2);
        } else if (theTag.equals(WIKI_TAG_ENTRYID)) {
            return entry.getId();
        } else if (theTag.equals(WIKI_TAG_PROPERTIES)) {
            return makeEntryTabs(request, entry, props);
        } else if (theTag.equals(WIKI_TAG_STREETVIEW)) {
            ImageOutputHandler ioh =
                (ImageOutputHandler) getRepository().getOutputHandler(
                    ImageOutputHandler.OUTPUT_PLAYER);
            if ( !ioh.isStreetviewEnabled()) {
                String message = Misc.getProperty(props, ATTR_MESSAGE,
                                     "Streetview not enabled");

                return message;
            }
            String width = Misc.getProperty(props, ATTR_WIDTH, "100%");
            String caption = Misc.getProperty(props, ATTR_CAPTION,
                                 (String) null);
            String heading = Misc.getProperty(props, "heading",
                                 (String) null);
            String imageUrl =
                request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
                                 ARG_OUTPUT,
                                 ioh.OUTPUT_STREETVIEW.toString());
            if (heading != null) {
                imageUrl += "&heading=" + heading;
            }
            String img = HtmlUtils.img(imageUrl, "",
                                       HtmlUtils.attr("width", width));
            if (caption != null) {
                img += HtmlUtils.div(caption,
                                     HtmlUtils.cssClass("image-caption"));
            }

            return img;

        } else if (theTag.equals(WIKI_TAG_IMAGE)) {
            return getWikiImage(wikiUtil, request, entry, props);
        } else if (theTag.equals(WIKI_TAG_URL)) {
            return getWikiUrl(wikiUtil, request, entry, props);
        } else if (theTag.equals(WIKI_TAG_HTML)) {
            Request newRequest = makeRequest(request, props);
            if (Misc.getProperty(props, ATTR_CHILDREN, false)) {
                List<Entry> children = getEntries(request, originalEntry,
                                           entry, props);
                for (Entry child : children) {
                    Result result =
                        getHtmlOutputHandler().getHtmlResult(request,
                            OutputHandler.OUTPUT_HTML, child);
                    sb.append(getEntryManager().getEntryLink(request, child));
                    sb.append(HtmlUtils.br());
                    sb.append(new String(result.getContent()));
                    sb.append(HtmlUtils.p());
                }

                return sb.toString();
            }


            Request myRequest = request.cloneMe();
            myRequest.put(ARG_ENTRYID, entry.getId());
            myRequest.put(ARG_OUTPUT, OutputHandler.OUTPUT_HTML.getId());
            myRequest.put(ARG_EMBEDDED, "true");


            Result result = getEntryManager().processEntryShow(myRequest,
                                entry);
            //            Result result = getHtmlOutputHandler().getHtmlResult(newRequest,
            //                                OutputHandler.OUTPUT_HTML, entry);

            return new String(result.getContent());
        } else if (theTag.equals(WIKI_TAG_CALENDAR)) {
            List<Entry> children = getEntries(request, originalEntry, entry,
                                       props);
            boolean doDay = Misc.getProperty(props, ATTR_DAY, false);
            getCalendarOutputHandler().outputCalendar(request,
                    getCalendarOutputHandler().makeCalendarEntries(request,
                        children), sb, doDay);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_DISPLAY)
                   || theTag.equals(WIKI_TAG_CHART)) {
            String jsonUrl = entry.getTypeHandler().getUrlForWiki(request,
                                                                  entry, theTag, props);
            //Gack - handle the files that are gridded netcdf
            //This is awful to have this here but I just don't know how to 
            //handle these entries
            if ((jsonUrl == null)
                    && entry.getTypeHandler().isType(Constants.TYPE_FILE)
                    && entry.getResource().getPath().toLowerCase().endsWith(
                        ".nc")) {
                TypeHandler gridType =
                    getRepository().getTypeHandler("cdm_grid");
                if (gridType != null) {
                    jsonUrl = gridType.getUrlForWiki(request, entry, theTag, props);
                }
            }
            getEntryDisplay(request, entry, theTag, entry.getName(), jsonUrl,
                            sb, props);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_GROUP)
                   || theTag.equals(WIKI_TAG_GROUP_OLD)) {
            getEntryDisplay(request, entry, theTag, entry.getName(), null,
                            sb, props);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_GRAPH)) {
            int width  = getDimension(props, ATTR_WIDTH, 400);
            int height = getDimension(props, ATTR_HEIGHT, 300);
            List<Entry> children = getEntries(request, originalEntry, entry,
                                       props);
            getGraphOutputHandler().getGraph(request, entry, children, sb,
                                             width, height);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_TIMELINE)) {
            Entry mainEntry = entry;
            List<Entry> children = getEntries(request, originalEntry,
                                       mainEntry, props);
            int    height = Misc.getProperty(props, ATTR_HEIGHT, 150);
            String style  = "height: " + height + "px;";
            getCalendarOutputHandler().makeTimeline(request, mainEntry,
                                                    children, sb, style, props);
            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_MAP)
                   || theTag.equals(WIKI_TAG_EARTH)
                   || theTag.equals(WIKI_TAG_MAPENTRY)) {
            int     width      = getDimension(props, ATTR_WIDTH, -100);
            int     height     = getDimension(props, ATTR_HEIGHT, 300);
            boolean justPoints = Misc.getProperty(props, "justpoints", false);
            boolean listEntries = Misc.getProperty(props, ATTR_LISTENTRIES,
                                      false);
            boolean showCheckbox = Misc.getProperty(props, "showCheckbox",
                                       false);
            boolean showSearch = Misc.getProperty(props, "showSearch",
                                       false);
            boolean checkboxOn = Misc.getProperty(props, "checkboxOn", true);
            boolean googleEarth =
                theTag.equals(WIKI_TAG_EARTH)
                && getMapManager().isGoogleEarthEnabled(request);

            List<Entry> children;
            if (theTag.equals(WIKI_TAG_MAPENTRY)) {
                children = new ArrayList<Entry>();
                children.add(entry);
            } else {
                children = getEntries(request, originalEntry, entry, props,
                                      false, "");
                if (children.isEmpty()) {
                    children.add(entry);
                }
            }






            if (children.size() == 0) {
                String message = Misc.getProperty(props, ATTR_MESSAGE,
                                     (String) null);
                if (message != null) {
                    return message;
                }
            } else {
                boolean anyHaveLatLon = false;
                for (Entry child : children) {
                    if (child.hasLocationDefined()
                            || child.hasAreaDefined()) {
                        anyHaveLatLon = true;

                        break;
                    }
                }
                if ( !anyHaveLatLon) {
                    String message = Misc.getProperty(props, ATTR_MESSAGE,
                                         (String) null);
                    if (message != null) {
                        return message;
                    }
                }
            }

            checkHeading(request, props, sb);
            Request newRequest = makeRequest(request, props);
            if (googleEarth) {
                getMapManager().getGoogleEarth(newRequest, children, sb,
                        Misc.getProperty(props, ATTR_WIDTH, -1),
                        Misc.getProperty(props, ATTR_HEIGHT, -1),
                        listEntries, justPoints);
            } else {
                MapOutputHandler mapOutputHandler =
                    (MapOutputHandler) getRepository().getOutputHandler(
                        MapOutputHandler.OUTPUT_MAP);
                if (mapOutputHandler == null) {
                    return "No maps";
                }
                //Request   newRequest       = request.cloneMe();
                //newRequest.putAll(props);
                boolean details = Misc.getProperty(props, ATTR_DETAILS,
                                      false);
                String icon = Misc.getProperty(props, ATTR_ICON,
                                  (String) null);

                String zoom = Misc.getProperty(props, "initialZoom",
                                  (String) null);
                String doCategories = Misc.getProperty(props, "doCategories",
                                                       "true");

                String layer = Misc.getProperty(props, ATTR_LAYER,
                                   (String) null);

                if ((icon != null) && icon.startsWith("#")) {
                    icon = null;
                }
                if (icon != null) {
                    newRequest.put(ARG_ICON, icon);
                }
                if (Misc.equals("true",
                                Misc.getProperty(props, ARG_MAP_ICONSONLY,
                                    (String) null))) {
                    newRequest.put(ARG_MAP_ICONSONLY, "true");
                }
                List<Object[]> mapProps = new ArrayList<Object[]>();

                if (layer != null) {
                    mapProps.add(new Object[] { "defaultMapLayer",
                            Json.quote(layer) });
                }

                String[] mapArgs = {
                    "strokeColor", "fillColor", "fillOpacity", "scrollToZoom",
                    "selectOnHover", "onSelect","showDetailsLink"
                };
                for (String mapArg : mapArgs) {
                    String v = (String) props.get(mapArg);
                    if (v != null) {
                        mapProps.add(new Object[] { mapArg, Json.quote(v) });
                    }
                }
                if (zoom != null) {
                    mapProps.add(new Object[] { "initialZoom", zoom });
                }
                if (props.get("mapSettings") != null) {
                    String       mapSet = (String) props.get("mapSettings");
                    List<String> msets  = StringUtil.split(mapSet, ",");
                    for (int i = 0; i < msets.size() - 1; i++) {
                        mapProps.add(new Object[] { msets.get(i),
                                Json.quote(msets.get(i + 1)) });
                    }
                }
                String mapVar = Misc.getProperty(props, "mapVar",
                                                 (String) null);
                MapInfo map = getMapManager().getMap(newRequest, children,
                                  sb, width, height, mapProps, "detailed",
                                  "" + details, "listEntries",
                                                     listEntries + "", "showSearch", ""+showSearch,
                                                     "showCheckbox",
                                  "" + showCheckbox, "checkboxOn",
                                                     "" + checkboxOn,
                                                     "mapVar", mapVar,
                                                     "doCategories",doCategories);
                if (icon != null) {
                    newRequest.remove(ARG_ICON);
                }
                newRequest.remove(ARG_MAP_ICONSONLY);
            }

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_TOOLS)) {
            StringBuilder links = new StringBuilder();
            int           cnt   = 0;
            for (Link link :
                    getEntryManager().getEntryLinks(request, entry)) {
                if ( !link.isType(OutputType.TYPE_IMPORTANT)) {
                    continue;
                }
                String label = HtmlUtils.img(link.getIcon())
                               + HtmlUtils.space(1) + link.getLabel();
                HtmlUtils.href(links, link.getUrl(), label);
                links.append(HtmlUtils.br());
                cnt++;
            }
            if (cnt == 0) {
                return "";
            }
            String title = Misc.getProperty(props, ATTR_TITLE, "Services");
            HtmlUtils.div(sb, title, HtmlUtils.cssClass("wiki-h4"));
            HtmlUtils.div(sb, links.toString(),
                          HtmlUtils.cssClass("entry-tools-box"));

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_MENU)) {
            String menus = Misc.getProperty(props, ATTR_MENUS, "");
            int type = OutputType.getTypeMask(StringUtil.split(menus, ",",
                           true, true));

            String links = getEntryManager().getEntryActionsTable(request,
                               entry, type);
            if (Misc.getProperty(props, "popup", false)) {
                StringBuilder popup = new StringBuilder();
                String menuLinkImg =
                    HtmlUtils.img(
                        getRepository().iconUrl("/icons/menu_arrow.gif"),
                        msg("Click to show menu"),
                        HtmlUtils.cssClass("ramadda-breadcrumbs-menu-img"));
                String menuLink = getPageHandler().makePopupLink(menuLinkImg,
                                      links, "", true, false, popup);

                popup.append(menuLink);

                return popup.toString();
            }

            return links;
        } else if (theTag.equals(WIKI_TAG_SEARCH)) {

            String type = Misc.getProperty(props, ATTR_TYPE,
                                           Misc.getProperty(props, ATTR_ID,
                                               TypeHandler.TYPE_ANY));

            String provider = Misc.getProperty(props,
                                  SearchManager.ARG_PROVIDER, (String) null);
            TypeHandler typeHandler = getRepository().getTypeHandler(type);

            if (typeHandler == null) {
                return "Could not find search type: " + type;
            }
            String  incomingMax = request.getString(ARG_MAX, (String) null);
            Request myRequest   = copyRequest(request, props);

            if (provider != null) {
                myRequest.put(SearchManager.ARG_PROVIDER, provider);
            }

            //Pass the wiki attribute into the request to the special search
            String fields = Misc.getProperty(props, ATTR_FIELDS,
                                             (String) null);
            if (fields == null) {
                fields = entry.getTypeHandler().getTypeProperty(
                    "map.chart.fields", (String) null);
            }

            if (fields != null) {
                myRequest.put(SpecialSearch.ARG_FIELDS, fields);
            }

            String metadata = Misc.getProperty(props, ATTR_METADATA,
                                  (String) null);
            if (metadata != null) {
                myRequest.put(SpecialSearch.ARG_METADATA, metadata);
            }

            if ( !myRequest.defined(ARG_SEARCH_SHOWHEADER)) {
                myRequest.put(ARG_SEARCH_SHOWHEADER, "false");
            }
            if ( !myRequest.defined(ARG_SEARCH_SHOWFORM)) {
                myRequest.put(ARG_SEARCH_SHOWFORM, "false");
            }
            if ( !myRequest.defined(ARG_SHOWCATEGORIES)) {
                myRequest.put(ARG_SHOWCATEGORIES, "false");
            }

            addSearchTerms(myRequest, props, entry);

            if (incomingMax != null) {
                myRequest.put(ARG_MAX, incomingMax);
            } else {
                if ( !myRequest.exists(ARG_MAX)) {
                    myRequest.put(ARG_MAX, "10");
                }
            }
            SpecialSearch ss = typeHandler.getSpecialSearch();
            ss.processSearchRequest(myRequest, sb);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_APPLY)) {
            StringBuilder style = new StringBuilder(Misc.getProperty(props,
                                      APPLY_PREFIX + ATTR_STYLE, ""));
            int padding = Misc.getProperty(props,
                                           APPLY_PREFIX + ATTR_PADDING, 5);
            int margin = Misc.getProperty(props, APPLY_PREFIX + ATTR_MARGIN,
                                          5);
            int border = Misc.getProperty(props, APPLY_PREFIX + ATTR_BORDER,
                                          -1);
            String bordercolor = Misc.getProperty(props,
                                     APPLY_PREFIX + ATTR_BORDERCOLOR, "#000");

            if (border > 0) {
                style.append(" border: " + border + "px solid " + bordercolor
                             + "; ");
            }

            if (padding > 0) {
                style.append(" padding: " + padding + "px; ");
            }

            if (margin > 0) {
                style.append(" margin: " + margin + "px; ");
            }


            int maxHeight = Misc.getProperty(props,
                                             APPLY_PREFIX + "maxheight", -1);
            if (maxHeight > 0) {
                style.append(" max-height: " + maxHeight
                             + "px;  overflow-y: auto; ");
            }

            int minHeight = Misc.getProperty(props,
                                             APPLY_PREFIX + "minheight", -1);
            if (minHeight > 0) {
                style.append(" min-height: " + minHeight + "px; ");
            }


            Hashtable tmpProps = new Hashtable(props);
            tmpProps.remove(ATTR_ENTRY);
            Request newRequest = makeRequest(request, props);
            //            System.err.println("cloned:" + newRequest);
            //            {{apply tag="tree" apply.layout="grid" apply.columns="2"}}
            String tag = Misc.getProperty(props, ATTR_APPLY_TAG, "html");
            String prefixTemplate = Misc.getProperty(props,
                                        APPLY_PREFIX + "header", "");
            String suffixTemplate = Misc.getProperty(props,
                                        APPLY_PREFIX + "footer", "");

            List<Entry> children = getEntries(request, originalEntry, entry,
                                       props, false, APPLY_PREFIX);
            if (children.size() == 0) {
                return null;
            }
            String layout = Misc.getProperty(props, APPLY_PREFIX + "layout",
                                             "table");
            int columns = Misc.getProperty(props,
                                           APPLY_PREFIX + ATTR_COLUMNS, 1);
            if (columns > children.size()) {
                columns = children.size();
            }
            String colWidth = "";
            if (layout.equals("table")) {
                if (columns > 1) {
                    sb.append(
                        "<table border=0 cellspacing=5 cellpadding=5  width=100%>");
                    sb.append("<tr valign=top>");
                    colWidth = ((int) (100.0 / columns)) + "%";
                }
            }
            List<String> contents = new ArrayList<String>();
            List<String> titles   = new ArrayList<String>();


            boolean includeIcon = Misc.getProperty(props,
                                      APPLY_PREFIX + ATTR_INCLUDEICON, false);
            String divClass = Misc.getProperty(props,
                                  APPLY_PREFIX + "divclass", "");


            boolean includeLinkAfter = false;

            if (layout.equals("accordian")) {
                includeLinkAfter = true;
            }

            String divExtra = HtmlUtils.concat(HtmlUtils.cssClass(divClass),
                                  HtmlUtils.style(style.toString()));
            int colCnt = 0;

            for (Entry child : children) {
                String childsHtml = my_getWikiInclude(wikiUtil, newRequest,
                                        originalEntry, child, tag, tmpProps,
                                        true);

                String prefix   = prefixTemplate;
                String suffix   = suffixTemplate;
                String urlLabel = getEntryDisplayName(child);
                if (includeIcon) {
                    urlLabel =
                        HtmlUtils.img(getPageHandler().getIconUrl(request,
                            child)) + " " + urlLabel;
                }

                String childUrl = HtmlUtils.href(
                                      request.entryUrl(
                                          getRepository().URL_ENTRY_SHOW,
                                          child), urlLabel);
                prefix = prefix.replace(
                    "${name}",
                    getEntryDisplayName(child).replace(
                        "${description}", child.getDescription()));
                suffix = suffix.replace(
                    "${name}",
                    getEntryDisplayName(child).replace(
                        "${description}", child.getDescription()));
                prefix = prefix.replace("${url}", childUrl);
                suffix = suffix.replace("${url}", childUrl);
                String icon =
                    HtmlUtils.img(getPageHandler().getIconUrl(request,
                        child));
                prefix = prefix.replace("${icon}", icon);
                suffix = suffix.replace("${icon}", icon);

                StringBuilder content = new StringBuilder();
                content.append(prefix);
                HtmlUtils.open(content, HtmlUtils.TAG_DIV, divExtra);
                content.append(getSnippet(request, child));
                content.append(childsHtml);
                content.append(suffix);
                if (includeLinkAfter) {
                    content.append(childUrl);
                }

                if (layout.equals("table")) {
                    if (columns > 1) {
                        if (colCnt >= columns) {
                            sb.append("</tr>");
                            sb.append("<tr valign=top>");
                            colCnt = 0;
                        }
                        sb.append("<td width=" + colWidth + ">");
                    }
                    colCnt++;
                    sb.append(content);
                    if (columns > 1) {
                        sb.append("</td>");
                    }
                } else {
                    //                    contents.add(content.toString());
                    contents.add("Contents");
                    String title = getEntryDisplayName(child);
                    if (includeIcon) {
                        title = HtmlUtils.img(
                            getPageHandler().getIconUrl(
                                request, child)) + " " + title;
                    }
                    titles.add(title);
                }
            }

            if (layout.equals("table")) {
                if (columns > 1) {
                    sb.append("</table>");
                }
            } else if (layout.equals("tabs")) {

                sb.append(OutputHandler.makeTabs(titles, contents, true,
                        false));
            } else if (layout.equals("accordian")) {
                int     showBorder = Misc.getProperty(props, "border", 0);
                boolean collapse = Misc.getProperty(props, "collapse", false);
                HtmlUtils.makeAccordian(sb, titles, contents, collapse,
                                        ((showBorder == 0)
                                         ? "ramadda-accordian"
                                         : null), null);
            } else {
                throw new IllegalArgumentException("Unknown layout:"
                        + layout);
            }

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_SIMPLE)) {
            return makeSimpleDisplay(request, props, originalEntry, entry);
        } else if (theTag.equals(WIKI_TAG_TABS)
                   || theTag.equals(WIKI_TAG_ACCORDIAN)
                   || theTag.equals(WIKI_TAG_SLIDESHOW)
                   || theTag.equals(WIKI_TAG_BOOTSTRAP)
                   || theTag.equals(WIKI_TAG_GRID)) {
            List<Entry> children = getEntries(request, originalEntry, entry,
                                       props);
            String message = Misc.getProperty(props, ATTR_MESSAGE,
                                 (String) null);
            if ((children.size() == 0) && (message != null)) {
                return message;
            }
            boolean doingSlideshow = theTag.equals(WIKI_TAG_SLIDESHOW);
            boolean doingGrid = theTag.equals(WIKI_TAG_GRID)
                                || theTag.equals(WIKI_TAG_BOOTSTRAP);
            List<String> titles   = new ArrayList<String>();
            List<String> urls     = new ArrayList<String>();
            List<String> contents = new ArrayList<String>();
            String       dfltTag  = WIKI_TAG_SIMPLE;

            if (props.get(ATTR_USEDESCRIPTION) != null) {
                boolean useDescription = Misc.getProperty(props,
                                             ATTR_USEDESCRIPTION, true);

                if (useDescription) {
                    dfltTag = WIKI_TAG_SIMPLE;
                } else {
                    dfltTag = WIKI_TAG_HTML;
                }
            }

            boolean showLink = Misc.getProperty(props, ATTR_SHOWLINK, true);
            boolean includeIcon = Misc.getProperty(props, ATTR_INCLUDEICON,
                                      false);
            boolean includeUrl = Misc.getProperty(props, "includeurl", false);
            if (doingGrid) {
                includeIcon = false;
            }
            boolean useCookies = Misc.getProperty(props, "cookie", false);
            String  linklabel  = Misc.getProperty(props, ATTR_LINKLABEL, "");

            String  width      = Misc.getProperty(props, ATTR_WIDTH, "400");
            int     height     = Misc.getProperty(props, ATTR_HEIGHT, 270);

            if (doingSlideshow) {
                props.put(ATTR_WIDTH, width);
                props.put(ATTR_HEIGHT, "" + height);
                props.put(ATTR_CONSTRAINSIZE, "true");
            }

            if (theTag.equals(WIKI_TAG_TABS)) {
                dfltTag = WIKI_TAG_HTML;
            }


            boolean linkResource = Misc.getProperty(props, ATTR_LINKRESOURCE,
                                       false);


            String    tag        = Misc.getProperty(props, ATTR_TAG, dfltTag);
            Request   newRequest = makeRequest(request, props);
            Hashtable tmpProps   = new Hashtable(props);
            tmpProps.remove(ATTR_ENTRY);
            tmpProps.remove(ATTR_ENTRIES);
            tmpProps.remove(ATTR_FIRST);


            if (children.size() > 0) {
                checkHeading(request, props, sb);
            }

            for (Entry child : children) {
                String title = getEntryDisplayName(child);
                if (includeIcon) {
                    title =
                        HtmlUtils.img(getPageHandler().getIconUrl(request,
                            child)) + " " + title;
                }
                titles.add(title);
                urls.add(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                          child));

                StringBuilder content =
                    new StringBuilder(my_getWikiInclude(wikiUtil, newRequest,
                        originalEntry, child, tag, tmpProps, true));
                if (showLink) {
                    String url;
                    if (linkResource
                            && (child.isFile()
                                || child.getResource().isUrl())) {
                        url = child.getTypeHandler().getEntryResourceUrl(
                            request, child);
                    } else {
                        url = request.entryUrl(
                            getRepository().URL_ENTRY_SHOW, child);
                    }
                    String href = HtmlUtils.href(url, linklabel.isEmpty()
                            ? getEntryDisplayName(child)
                            : linklabel);

                    content.append(HtmlUtils.br());
                    content.append(HtmlUtils.leftRight("", href));
                }
                contents.add(content.toString());
            }



            if (theTag.equals(WIKI_TAG_ACCORDIAN)) {
                int border = Misc.getProperty(props, ATTR_BORDER, 0);
                boolean collapse = Misc.getProperty(props, ATTR_COLLAPSE,
                                       false);
                HtmlUtils.makeAccordian(sb, titles, contents, collapse,
                                        ((border == 0)
                                         ? "ramadda-accordian"
                                         : null), null);

                return sb.toString();
            } else if (doingGrid) {
                List<String> weights = null;
                boolean      doLine  = Misc.getProperty(props, "doline",
                                           true);
                String ws = Misc.getProperty(props, "weights", (String) null);
                if (ws != null) {
                    weights = StringUtil.split(ws, ",", true, true);
                }

                int columns     = Misc.getProperty(props, "columns", 3);
                int innerHeight = Misc.getProperty(props, "inner-height", -1);
                int minHeight = Misc.getProperty(props, "inner-minheight",
                                    -1);
                int maxHeight = Misc.getProperty(props, "inner-maxheight",
                                    300);

                StringBuilder innerStyle = new StringBuilder();
                if (innerHeight > 0) {
                    HtmlUtils.concat(innerStyle, "height:",
                                     innerHeight + "px;");
                    innerStyle.append("overflow-y: auto;");
                }
                if (minHeight > 0) {
                    HtmlUtils.concat(innerStyle, "min-height:",
                                     minHeight + "px;");
                    innerStyle.append("overflow-y: auto;");
                }
                if (maxHeight > 0) {
                    HtmlUtils.concat(innerStyle, "max-height:",
                                     maxHeight + "px;");
                    innerStyle.append("overflow-y: auto;");
                }
                sb.append(HtmlUtils.open(HtmlUtils.TAG_DIV,
                                         HtmlUtils.cssClass("ramadda-grid")));
                int rowCnt = 0;
                int colCnt = 10000;
                int weight = 12 / columns;

                for (int i = 0; i < titles.size(); i++) {
                    Entry child = children.get(i);
                    colCnt++;
                    if (colCnt >= columns) {
                        if (rowCnt > 0) {
                            sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
                            if (doLine) {
                                sb.append("<hr>");
                            } else {
                                sb.append(HtmlUtils.br());
                            }
                        }
                        rowCnt++;
                        HtmlUtils.open(sb, HtmlUtils.TAG_DIV,
                                       HtmlUtils.cssClass("row"));
                        colCnt = 0;
                    }
                    String weightString = "" + weight;
                    if ((weights != null) && (i < weights.size())) {
                        weightString = weights.get(i);
                    }
                    HtmlUtils.open(sb, HtmlUtils.TAG_DIV,
                                   HtmlUtils.cssClass("col-md-"
                                       + weightString));
                    HtmlUtils.open(sb, HtmlUtils.TAG_DIV,
                                   HtmlUtils.cssClass("ramadda-box"));

                    HtmlUtils.div(sb,
                                  HtmlUtils.href(urls.get(i), titles.get(i)),
                                  HtmlUtils.cssClass("ramadda-page-heading"));

                    String displayHtml = contents.get(i);
                    String snippet     = getSnippet(request, child);
                    if (Utils.stringDefined(snippet)) {
                        snippet = wikifyEntry(request, child, snippet, false,
                                              null, null, wikiUtil.getNotTags());
                        displayHtml = snippet += displayHtml;
                    }


                    HtmlUtils.div(sb, displayHtml,
                                  HtmlUtils.cssClass("bs-inner")
                                  + HtmlUtils.attr("style",
                                      innerStyle.toString()));
                    HtmlUtils.close(sb, HtmlUtils.TAG_DIV);
                    HtmlUtils.close(sb, HtmlUtils.TAG_DIV);
                }

                //Close the div if there was anything
                if (rowCnt > 0) {
                    HtmlUtils.close(sb, HtmlUtils.TAG_DIV);
                }

                //Close the grid div
                HtmlUtils.close(sb, HtmlUtils.TAG_DIV);

                return sb.toString();
            } else if (doingSlideshow) {
                // for slideshow
                boolean shownav = Misc.getProperty(props, "shownav", false);
                boolean autoplay = Misc.getProperty(props, "autoplay", false);
                int     playSpeed   = Misc.getProperty(props, "speed", 5);

                String  arrowWidth  = "24";
                String  arrowHeight = "43";
                String  slideId     = HtmlUtils.getUniqueId("slides_");

                HtmlUtils.open(sb, "style",
                               HtmlUtils.attr("type", "text/css"));
                // need to set the height of the div to include the nav bar
                HtmlUtils.concat(sb, "#", slideId, " { width: ",
                                 width + "; height: " + (height + 30), "}\n");


                int border = Misc.getProperty(props, ATTR_BORDER, 1);
                String borderColor = Misc.getProperty(props,
                                         ATTR_BORDERCOLOR, "#aaa");
                sb.append(
                    "#" + slideId + " .slides_container {border: " + border
                    + "px solid " + borderColor + "; width:" + width
                    + ";overflow:hidden;position:relative;display:none;}\n.slides_container div.slide {width:"
                    + width + ";height:" + height + "px;display:block;}\n");
                sb.append("</style>\n\n");
                sb.append("<link rel=\"stylesheet\" href=\"");
                sb.append(
                    getRepository().htdocsUrl("/lib/slides/paginate.css"));
                sb.append("\" type=\"text/css\" media=\"screen\" />");
                sb.append("\n");


                // user speed is seconds, script uses milliseconds - 0 == no play
                int startSpeed = (autoplay)
                                 ? playSpeed * 1000
                                 : 0;
                String slideParams =
                    "preload: false, preloadImage: "
                    + HtmlUtils.squote(
                        getRepository().htdocsUrl(
                            "/lib/slides/img/loading.gif")) + ", play: "
                                + startSpeed + ", width: \"" + width + "\""
                                + ", pause: 2500, hoverPause: true"
                                + ", generatePagination: " + shownav + "\n";
                StringBuilder js = new StringBuilder();

                js.append(
                    "$(function(){\n$(" + HtmlUtils.squote("#" + slideId)
                    + ").slides({" + slideParams
                    + ",\nslidesLoaded: function() {$('.caption').animate({ bottom:0 },200); }\n});\n});\n");
                HtmlUtils.open(sb, HtmlUtils.TAG_DIV, HtmlUtils.id(slideId));



                String prevImage =
                    HtmlUtils.href(
                        "#",
                        HtmlUtils.img(
                            getRepository().htdocsUrl(
                                "/lib/slides/img/arrow-prev.png"), "Prev",
                                    " width=18 "), HtmlUtils.cssClass(
                                        "prev"));

                String nextImage =
                    HtmlUtils.href(
                        "#",
                        HtmlUtils.img(
                            getRepository().htdocsUrl(
                                "/lib/slides/img/arrow-next.png"), "Next",
                                    " width=18 "), HtmlUtils.cssClass(
                                        "next"));


                sb.append(
                    "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tr>\n");
                HtmlUtils.col(sb, prevImage, "width=1");
                HtmlUtils.open(sb, HtmlUtils.TAG_TD,
                               HtmlUtils.attr(HtmlUtils.ATTR_WIDTH, width));
                HtmlUtils.open(sb, HtmlUtils.TAG_DIV,
                               HtmlUtils.cssClass("slides_container"));
                for (int i = 0; i < titles.size(); i++) {
                    String title   = titles.get(i);
                    String content = contents.get(i);
                    sb.append("\n");
                    HtmlUtils.open(sb, HtmlUtils.TAG_DIV,
                                   HtmlUtils.cssClass("slide"));
                    sb.append(content);
                    //                    sb.append(HtmlUtils.br());
                    //                    sb.append(title);
                    HtmlUtils.close(sb, HtmlUtils.TAG_DIV);  // slide
                }
                HtmlUtils.close(sb, HtmlUtils.TAG_DIV);  // slides_container
                HtmlUtils.close(sb, HtmlUtils.TAG_TD);
                HtmlUtils.col(sb, nextImage, "width=1");
                sb.append("</tr></table>");
                HtmlUtils.close(sb, HtmlUtils.TAG_DIV);      // slideId

                sb.append(
                    HtmlUtils.importJS(
                        getRepository().htdocsUrl(
                            "/lib/slides/slides.min.jquery.js")));

                HtmlUtils.script(sb, js.toString());

                return sb.toString();
            } else {
                //TABS
                sb.append(OutputHandler.makeTabs(titles, contents, true,
                        useCookies));

                return sb.toString();
            }
        } else if (false && theTag.equals(WIKI_TAG_GRID)) {
            getHtmlOutputHandler().makeGrid(request,
                                            getEntries(request,
                                                originalEntry, entry,
                                                    props), sb);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_TABLE)) {
            List<Entry> entries = getEntries(request, originalEntry, entry,
                                             props);
            getHtmlOutputHandler().makeTable(request, entries, sb);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_RECENT)) {
            List<Entry> children = getEntries(request, originalEntry, entry,
                                       props);
            int numDays = Misc.getProperty(props, ATTR_DAYS, 3);
            BufferMapList<Date> map = new BufferMapList<Date>();
            SimpleDateFormat dateFormat =
                new SimpleDateFormat("EEEEE MMMMM d");
            dateFormat.setTimeZone(RepositoryUtil.TIMEZONE_DEFAULT);
            Date firstDay = ((children.size() > 0)
                             ? new Date(children.get(0).getChangeDate())
                             : new Date());
            GregorianCalendar cal1 =
                new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);
            cal1.setTime(new Date(firstDay.getTime()));
            cal1.set(cal1.MILLISECOND, 0);
            cal1.set(cal1.SECOND, 0);
            cal1.set(cal1.MINUTE, 0);
            cal1.set(cal1.HOUR, 0);
            GregorianCalendar cal2 =
                new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);
            cal2.setTime(cal1.getTime());
            cal2.roll(cal2.DAY_OF_YEAR, 1);

            for (int i = 0; i < numDays; i++) {
                Date date1 = cal1.getTime();
                Date date2 = cal2.getTime();
                cal2.setTime(cal1.getTime());
                cal1.roll(cal1.DAY_OF_YEAR, -1);
                for (Entry e : children) {
                    Date changeDate = new Date(e.getChangeDate());
                    if ((changeDate.getTime() < date1.getTime())
                            || (changeDate.getTime() > date2.getTime())) {
                        continue;
                    }
                    Appendable buff = map.get(date1);
                    buff.append("<tr><td width=75%>&nbsp;&nbsp;&nbsp;");
                    buff.append(getEntryManager().getAjaxLink(request, e,
                            e.getLabel()).toString());
                    buff.append("</td><td width=25% align=right><i>");
                    buff.append(formatDate(request, changeDate));
                    buff.append("</i></td></tr>");
                }
            }
            for (Date date : map.getKeys()) {
                Appendable tmp = new StringBuilder();
                String     msg = msg("New on") + " "
                                 + dateFormat.format(date);
                tmp.append("<table width=100% border=0>");
                tmp.append(map.get(date).toString());
                tmp.append("</table>");
                sb.append(HtmlUtils.makeShowHideBlock(msg, tmp.toString(),
                        true));
            }

            return sb.toString();

        } else if (theTag.equals(WIKI_TAG_PLAYER)
                   || theTag.equals(WIKI_TAG_PLAYER_OLD)) {
            boolean useAttachment = Misc.getProperty(props, "useAttachment",
                                        false);

            List<Entry> children = getEntries(request, originalEntry, entry,
                                       props, true);
            if (children.size() == 0) {
                return null;
            }
            ImageOutputHandler imageOutputHandler =
                (ImageOutputHandler) getRepository().getOutputHandler(
                    ImageOutputHandler.OUTPUT_PLAYER);
            Request imageRequest = request.cloneMe();

            int     width        = Misc.getProperty(props, ATTR_WIDTH, 0);
            if (width != 0) {
                imageRequest.put(ARG_WIDTH, "" + width);
            }
            boolean loopStart = Misc.getProperty(props, "loopstart", false);
            if (loopStart) {
                imageRequest.put("loopstart", "true");
            }

            if (useAttachment) {
                imageRequest.put("useAttachment", "true");
            }

            int delay = Misc.getProperty(props, "loopdelay", 0);
            if (delay > 0) {
                imageRequest.put("loopdelay", "" + delay);
            }

            int height = Misc.getProperty(props, ATTR_HEIGHT, 0);
            if (height > 0) {
                imageRequest.put(ARG_HEIGHT, "" + height);
            }
            imageOutputHandler.makePlayer(imageRequest, entry, children, sb,
                                          Misc.getProperty(props,
                                              "show_sort_links",
                                                  false), false);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_GALLERY)) {
            List<Entry> children = getEntries(request, originalEntry, entry,
                                       props, true);
            if (children.size() == 0) {
                String message = Misc.getProperty(props, ATTR_MESSAGE,
                                     (String) null);
                if (message != null) {
                    return message;
                }
            }
            if (children.size() > 0) {
                checkHeading(request, props, sb);
            }
            makeGallery(request, children, props, sb);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_ROOT)) {
            return getRepository().getUrlBase();
        } else if (theTag.equals(WIKI_TAG_CHILDREN_GROUPS)
                   || theTag.equals(WIKI_TAG_CHILDREN_ENTRIES)
                   || theTag.equals(WIKI_TAG_CHILDREN)
                   || theTag.equals(WIKI_TAG_TREE)) {

            if (theTag.equals(WIKI_TAG_CHILDREN_GROUPS)) {
                props.put(ATTR_FOLDERS, "true");
            } else if (theTag.equals(WIKI_TAG_CHILDREN_ENTRIES)) {
                props.put(ATTR_FILES, "true");
            }

            List<Entry> children = getEntries(request, originalEntry, entry,
                                       props);

            if (children.size() == 0) {
                return null;
            }
            boolean showCategories = Misc.getProperty(props,
                                         ARG_SHOWCATEGORIES, false);
            if (showCategories) {
                request.put(ARG_SHOWCATEGORIES, "true");
            }
            boolean decorate    = Misc.getProperty(props, ATTR_DECORATE,
                                      true);
            boolean showDetails = Misc.getProperty(props, ATTR_DETAILS, true);

            Request newRequest  = request.cloneMe();

            if ( !showDetails) {
                newRequest.put(ARG_DETAILS, "false");
            }
            if ( !decorate) {
                newRequest.put(ARG_DECORATE, "false");
            }

            if (children.size() > 0) {
                checkHeading(request, props, sb);
            }

            String link = getHtmlOutputHandler().getEntriesList(newRequest,
                              sb, children, true, false, showDetails);
            if (Misc.getProperty(props, "form", false)) {
                return link + HtmlUtils.br() + sb.toString();
            } else {
                return sb.toString();
            }

        } else if (theTag.equals(WIKI_TAG_TREEVIEW)) {
            int width  = getDimension(props, ATTR_WIDTH, -100);
            int height = getDimension(props, ATTR_HEIGHT, 500);

            List<Entry> children = getEntries(request, originalEntry, entry,
                                       props);
            if (children.size() == 0) {
                return null;
            }
            getHtmlOutputHandler().makeTreeView(request, children, sb, width,
                    height);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_LINKS)
                   || theTag.equals(WIKI_TAG_LIST)) {
            boolean isList = theTag.equals(WIKI_TAG_LIST);
            List<Entry> children = getEntries(request, originalEntry, entry,
                                       props);
            if (children.size() == 0) {
                return null;
            }
            boolean includeIcon = Misc.getProperty(props, ATTR_INCLUDEICON,
                                      false);
            boolean linkResource = Misc.getProperty(props, ATTR_LINKRESOURCE,
                                       false);
            String separator = (isList
                                ? ""
                                : Misc.getProperty(props, ATTR_SEPARATOR,
                                    ""));
            String cssClass = Misc.getProperty(props, ATTR_CLASS, "");
            String style    = Misc.getProperty(props, ATTR_STYLE, "");
            String tagOpen  = (isList
                               ? "<li>"
                               : Misc.getProperty(props, ATTR_TAGOPEN,
                                   "<li>"));

            String tagClose = (isList
                               ? ""
                               : Misc.getProperty(props, ATTR_TAGCLOSE, ""));

            if (includeIcon) {
                tagOpen  = "";
                tagClose = "<br>";
            }

            List<String> links = new ArrayList<String>();
            for (Entry child : children) {
                String url;
                if (linkResource
                        && (child.getTypeHandler().isType("link")
                            || child.isFile()
                            || child.getResource().isUrl())) {
                    url = child.getTypeHandler().getEntryResourceUrl(request,
                            child);
                } else {
                    url = request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                           child);
                }

                String linkLabel = getEntryDisplayName(child);
                if (includeIcon) {
                    linkLabel =
                        HtmlUtils.img(getPageHandler().getIconUrl(request,
                            child)) + HtmlUtils.space(1) + linkLabel;
                }
                String href = HtmlUtils.href(url, linkLabel,
                                             HtmlUtils.cssClass(cssClass)
                                             + HtmlUtils.style(style));
                StringBuilder link = new StringBuilder();
                link.append(tagOpen);
                link.append(href);
                link.append(tagClose);
                links.add(link.toString());
            }

            StringBuilder contentsSB = new StringBuilder();
            String prefix = Misc.getProperty(props, ATTR_LIST_PREFIX,
                                             (String) null);
            String suffix = Misc.getProperty(props, ATTR_LIST_SUFFIX,
                                             (String) null);
            if (prefix != null) {
                contentsSB.append(prefix);
            } else if (tagOpen.equals("<li>")) {
                contentsSB.append("<ul>");
            }
            contentsSB.append(StringUtil.join(separator, links));
            if (suffix != null) {
                contentsSB.append(suffix);
            } else if (tagOpen.equals("<li>")) {
                contentsSB.append("</ul>");
            }

            String  title = Misc.getProperty(props, ATTR_TITLE,
                                             (String) null);
            boolean showTitle = Misc.getProperty(props, ATTR_SHOWTITLE,
                                    false);
            String innerClass = Misc.getProperty(props, ATTR_INNERCLASS,
                                    "ramadda-links-inner");

            String contents = HtmlUtils.div(contentsSB.toString(),
                                            HtmlUtils.cssClass(innerClass));
            if ((title != null) || showTitle) {
                if (title == null) {
                    title = entry.getName();
                }
                String entryUrl =
                    request.entryUrl(getRepository().URL_ENTRY_SHOW, entry);
                title    = HtmlUtils.href(entryUrl, title);
                contents = HtmlUtils.h3(title) + contents;
            }
            contents = HtmlUtils.div(contents,
                                     HtmlUtils.cssClass("ramadda-links"));

            return contents;
        } else {
            String fromTypeHandler =
                entry.getTypeHandler().getWikiInclude(wikiUtil, request,
                    originalEntry, entry, theTag, props);
            if (fromTypeHandler != null) {
                if (wikify) {
                    fromTypeHandler = wikifyEntry(request, entry,
                                                  fromTypeHandler, false, null, null,  wikiUtil.getNotTags());
                }

                return fromTypeHandler;
            }

            for (PageDecorator pageDecorator :
                    repository.getPluginManager().getPageDecorators()) {
                String fromPageDecorator =
                    pageDecorator.getWikiInclude(wikiUtil, request,
                        originalEntry, entry, theTag, props);
                if (fromPageDecorator != null) {
                    return fromPageDecorator;
                }

            }

            //Check for the wiki.<tag> property
            String fromProperty = getRepository().getProperty("wiki."
                                      + theTag, (String) null);
            if (fromProperty != null) {
                if (wikify) {
                    fromProperty = wikifyEntry(request, entry, fromProperty,
                            false, null, null, wikiUtil.getNotTags());
                }

                return fromProperty;
            }


            return null;
        }

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param props _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void checkHeading(Request request, Hashtable props, Appendable sb)
            throws Exception {
        String heading = Misc.getProperty(props, "heading", (String) null);
        if (heading != null) {
            sb.append(
                HtmlUtils.div(
                    heading, HtmlUtils.cssClass("ramadda-page-heading")));

        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param child _more_
     *
     * @return _more_
     */
    public String getSnippet(Request request, Entry child) {
        String snippet = StringUtil.findPattern(child.getDescription(),
                             "(?s)<snippet>(.*)</snippet>");


        if (snippet == null) {
            snippet = StringUtil.findPattern(child.getDescription(),
                                             ":blurb([^\\n]+)\\n");
        }
        if (snippet != null) {
            snippet = HtmlUtils.div(snippet,
                                    HtmlUtils.cssClass("ramadda-snippet"));
        } else {
            snippet = "";
        }

        return snippet;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Request copyRequest(Request request, Hashtable props)
            throws Exception {
        Request clonedRequest = request.cloneMe();
        clonedRequest.putAll(props);

        return clonedRequest;
    }



    /**
     * Add the image popup javascript
     *
     * @param request  the Request
     * @param buf      the page StringBuilder
     * @param props    the properties
     */
    public void addImagePopupJS(Request request, StringBuilder buf,
                                Hashtable props) {
        if (request.getExtraProperty("added fancybox") == null) {
            String captionpos = Misc.getProperty(props, ATTR_POPUPCAPTION,
                                    "none");
            StringBuilder options = new StringBuilder("{");
            if ( !captionpos.equals("none")) {
                options.append(HtmlUtils.squote("titlePosition"));
                options.append(" : ");
                if (captionpos.equals("inside")) {
                    options.append(HtmlUtils.squote("inside"));
                } else if (captionpos.equals("over")) {
                    options.append(HtmlUtils.squote("over"));
                } else {
                    options.append(HtmlUtils.squote("outside"));
                }
            } else {
                options.append(HtmlUtils.squote("titleShow"));
                options.append(" : ");
                options.append("false");
            }
            options.append("}");

            buf.append(
                HtmlUtils.script(
                    "$(document).ready(function() {\n $(\"a.popup_image\").fancybox("
                    + options.toString() + ");\n });\n"));

            request.putExtraProperty("added fancybox", "yes");
        }
    }

    /**
     * Make a WikiUtil class for the request
     *
     * @param request  the Request
     * @param makeHeadings  true to make headings
     *
     * @return  the WikiUtil
     */
    private WikiUtil makeWikiUtil(Request request, boolean makeHeadings) {
        WikiUtil wikiUtil = new WikiUtil();
        wikiUtil.setMakeHeadings(makeHeadings);

        return initWikiUtil(request, wikiUtil, null);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param wiki _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String wikify(Request request, String wiki) throws Exception {
        return makeWikiUtil(request, false).wikify(wiki, null);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param wikiUtil _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public WikiUtil initWikiUtil(Request request, WikiUtil wikiUtil,
                                 Entry entry) {
        wikiUtil.setMobile(request.isMobile());
        if ( !request.isAnonymous()) {
            wikiUtil.setUser(request.getUser().getId());
        }
        if (entry != null) {
            wikiUtil.setTitleUrl(
                request.entryUrl(getRepository().URL_ENTRY_SHOW, entry));
        }

        return wikiUtil;
    }


    /**
     * _more_
     *
     * @param original _more_
     * @param props _more_
     *
     * @return _more_
     */
    private Request makeRequest(Request original, Hashtable props) {
        Request newRequest = original.cloneMe();
        newRequest.putAll(props);
        newRequest.put(ARG_EMBEDDED, "true");

        return newRequest;
    }

    /**
     * Make entry tabs html
     *
     * @param request The request
     * @param entry  the entry
     * @param props _more_
     *
     * @return the entry tabs html
     *
     * @throws Exception  problems
     */
    private String makeEntryTabs(Request request, Entry entry,
                                 Hashtable props)
            throws Exception {

        request = request.cloneMe();
        request.putExtraProperty("wiki.props", props);


        List<String> onlyTheseTypes = null;
        List<String> notTheseTypes  = null;

        String metadataTypesAttr = Misc.getProperty(props,
                                       ATTR_METADATA_TYPES, (String) null);
        if (metadataTypesAttr != null) {
            onlyTheseTypes = new ArrayList<String>();
            notTheseTypes  = new ArrayList<String>();
            for (String type :
                    StringUtil.split(metadataTypesAttr, ",", true, true)) {
                if (type.startsWith("!")) {
                    notTheseTypes.add(type.substring(1));
                } else {
                    onlyTheseTypes.add(type);
                }
            }
        }
        List tabTitles   = new ArrayList<String>();
        List tabContents = new ArrayList<String>();
        boolean includeTitle = Misc.getProperty(props,
                                   ATTR_METADATA_INCLUDE_TITLE, true);



        for (TwoFacedObject tfo :
                getRepository().getHtmlOutputHandler().getMetadataHtml(
                    request, entry, onlyTheseTypes, notTheseTypes,
                    includeTitle)) {
            tabTitles.add(tfo.toString());
            tabContents.add(tfo.getId());
        }
        if (tabTitles.size() == 0) {
            return getMessage(props, "No metadata found");
        }
        if ( !includeTitle) {
            return StringUtil.join("<br>", tabContents);
        }
        if (tabContents.size() > 1) {
            return OutputHandler.makeTabs(tabTitles, tabContents, true);
        }

        return tabContents.get(0).toString();

    }


    /**
     * Get the entries that are images
     *
     *
     * @param request _more_
     * @param entries  the list of entries
     * @param useAttachment _more_
     *
     * @return  the list of entries that are images
     *
     * @throws Exception _more_
     */
    public List<Entry> getImageEntries(Request request, List<Entry> entries,
                                       boolean useAttachment)
            throws Exception {
        return getImageEntriesOrNot(request, entries, false, useAttachment);
    }

    /**
     * _more_
     *
     * @param entries _more_
     * @param entry _more_
     * @param flag _more_
     * @param orNot _more_
     */
    private void orNot(List<Entry> entries, Entry entry, boolean flag,
                       boolean orNot) {
        if (orNot) {
            if ( !flag) {
                entries.add(entry);
            }
        } else {
            if (flag) {
                entries.add(entry);
            }
        }
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entries _more_
     * @param orNot _more_
     * @param useAttachment _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> getImageEntriesOrNot(Request request,
                                            List<Entry> entries,
                                            boolean orNot,
                                            boolean useAttachment)
            throws Exception {
        List<Entry> imageEntries = new ArrayList<Entry>();
        for (Entry entry : entries) {
            boolean isImage = entry.isImage();
            if ( !isImage && useAttachment) {
                isImage = getMetadataManager().getImageUrls(request,
                        entry).size() > 0;
            }
            orNot(imageEntries, entry, isImage, orNot);
        }

        return imageEntries;
    }

    /**
     * Get the entries for the request
     *
     * @param request The request
     * @param originalEntry _more_
     * @param entry  the parent entry
     * @param props  properties
     *
     * @return the list of entries
     *
     * @throws Exception problems
     */
    public List<Entry> getEntries(Request request, Entry originalEntry,
                                  Entry entry, Hashtable props)
            throws Exception {

        return getEntries(request, originalEntry, entry, props, false, "");
    }



    /**
     * Get the entries for the request
     *
     * @param request  the request
     * @param originalEntry _more_
     * @param entry    the entry
     * @param props    properties
     * @param onlyImages  true to only show images
     *
     * @return the list of Entry's
     *
     * @throws Exception  problems making list
     */
    public List<Entry> getEntries(Request request, Entry originalEntry,
                                  Entry entry, Hashtable props,
                                  boolean onlyImages)
            throws Exception {
        return getEntries(request, originalEntry, entry, props, onlyImages,
                          "");
    }


    /**
     * Get the entries for the request
     *
     * @param request  the request
     * @param originalEntry _more_
     * @param entry    the entry
     * @param props    properties
     * @param onlyImages  true to only show images
     * @param attrPrefix _more_
     *
     * @return the list of Entry's
     *
     * @throws Exception  problems making list
     */
    public List<Entry> getEntries(Request request, Entry originalEntry,
                                  Entry entry, Hashtable props,
                                  boolean onlyImages, String attrPrefix)
            throws Exception {
        if (props == null) {
            props = new Hashtable();
        } else {
            Hashtable tmp = new Hashtable();
            tmp.putAll(props);
            props = tmp;
        }

        String userDefinedEntries = Misc.getProperty(props,
                                        attrPrefix + ATTR_ENTRIES,
                                        ID_CHILDREN);

        return getEntries(request, originalEntry, entry, userDefinedEntries,
                          props, onlyImages, attrPrefix);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param filter _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> applyFilter(Request request, Entry entry,
                                   String filter, Hashtable props)
            throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);

        return applyFilter(request, entries, filter, props);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param filter _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> applyFilter(Request request, List<Entry> entries,
                                   String filter, Hashtable props)
            throws Exception {
        if (filter == null) {
            return entries;
        }
        boolean doNot = false;
        if (filter.startsWith("!")) {
            doNot  = true;
            filter = filter.substring(1);
        }
        if (filter.equals(FILTER_IMAGE)) {
            boolean useAttachment = Misc.getProperty(props, "useAttachment",
                                        false);
            entries = getImageEntriesOrNot(request, entries, doNot,
                                           useAttachment);
        } else if (filter.equals(FILTER_FILE)) {
            List<Entry> tmp = new ArrayList<Entry>();
            for (Entry child : entries) {
                orNot(tmp, child, !child.isGroup(), doNot);
            }
            entries = tmp;
        } else if (filter.equals(FILTER_GEO)) {
            List<Entry> tmp = new ArrayList<Entry>();
            for (Entry child : entries) {
                orNot(tmp, child, child.isGeoreferenced(), doNot);
            }
            entries = tmp;
        } else if (filter.equals(FILTER_FOLDER)) {
            List<Entry> tmp = new ArrayList<Entry>();
            for (Entry child : entries) {
                orNot(tmp, child, child.isGroup(), doNot);
            }
            entries = tmp;
        } else if (filter.startsWith(FILTER_TYPE)) {
            List<String> types =
                StringUtil.split(filter.substring(FILTER_TYPE.length()), ";",
                                 true, true);
            List<Entry> tmp = new ArrayList<Entry>();
            for (Entry child : entries) {
                for (String type : types) {
                    boolean matches = child.getTypeHandler().isType(type);
                    orNot(tmp, child, matches, doNot);
                    if (matches && !doNot) {
                        break;
                    }
                    if ( !matches && doNot) {
                        break;
                    }
                }
            }
            entries = tmp;
        } else if (filter.startsWith(FILTER_SUFFIX)) {
            List<String> suffixes =
                StringUtil.split(filter.substring(FILTER_SUFFIX.length()),
                                 ",", true, true);
            List<Entry> tmp = new ArrayList<Entry>();
            for (Entry child : entries) {
                for (String suffix : suffixes) {
                    boolean matches =
                        child.getResource().getPath().endsWith(suffix);
                    orNot(tmp, child, matches, doNot);
                    if (matches) {
                        break;
                    }
                }
            }
            entries = tmp;
        } else if (filter.startsWith(FILTER_ID)) {
            List<String> ids =
                StringUtil.split(filter.substring(FILTER_ID.length()), ",",
                                 true, true);
            List<Entry> tmp = new ArrayList<Entry>();
            for (Entry child : entries) {
                for (String id : ids) {
                    boolean matches = child.getId().equals(id);
                    orNot(tmp, child, matches, doNot);
                    if (matches && !doNot) {
                        break;
                    }
                    if ( !matches && doNot) {
                        break;
                    }
                }
            }
            entries = tmp;
        }

        return entries;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param userDefinedEntries _more_
     * @param props _more_
     * @param onlyImages _more_
     * @param attrPrefix _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> getEntries(Request request, Entry originalEntry,
                                  Entry entry, String userDefinedEntries,
                                  Hashtable props, boolean onlyImages,
                                  String attrPrefix)
            throws Exception {



        if (props == null) {
            props = new Hashtable();
        }

        request = request.cloneMe();

        //If there is a max property then clone the request and set the max
        //For some reason we are using both count and max as attrs
        int count = Misc.getProperty(props, attrPrefix + ATTR_COUNT, -1);
        int max   = Misc.getProperty(props, attrPrefix + ATTR_MAX, count);
        if (max > 0) {
            request.put(ARG_MAX, "" + max);
        }


        List<Entry> entries = getEntries(request, entry, userDefinedEntries,
                                         props);
        String filter = Misc.getProperty(props,
                                         attrPrefix + ATTR_ENTRIES
                                         + ".filter", (String) null);

        if (Misc.getProperty(props, attrPrefix + ATTR_FOLDERS, false)) {
            filter = FILTER_FOLDER;
        }

        if (Misc.getProperty(props, attrPrefix + ATTR_FILES, false)) {
            filter = FILTER_FILE;
        }


        //TODO - how do we combine filters? what kind of or/and logic?
        if (filter != null) {
            entries = applyFilter(request, entries, filter, props);
        }



        if (onlyImages
                || Misc.getProperty(props, attrPrefix + ATTR_IMAGES, false)) {
            boolean useAttachment = Misc.getProperty(props, "useAttachment",
                                        false);
            entries = applyFilter(request,
                                  getImageEntries(request, entries,
                                      useAttachment), filter, props);
        }


        String excludeEntries = Misc.getProperty(props,
                                    attrPrefix + ATTR_EXCLUDE, (String) null);

        if (excludeEntries != null) {
            HashSet seen = new HashSet();
            for (String id : StringUtil.split(excludeEntries, ",")) {
                if (id.equals(ID_THIS)) {
                    seen.add(originalEntry.getId());
                } else {
                    seen.add(id);
                }
            }
            List<Entry> okEntries = new ArrayList<Entry>();
            for (Entry e : entries) {
                if ( !seen.contains(e.getId())
                        && !seen.contains(e.getName())) {
                    okEntries.add(e);
                }
            }
            entries = okEntries;
        }


        String sort = Misc.getProperty(props, attrPrefix + ATTR_SORT,
                                       (String) null);
        if (sort != null) {
            boolean ascending = Misc.getProperty(props,
                                    attrPrefix + ATTR_SORT_ORDER,
                                    "up").equals("up");
            if (sort.equals(SORT_DATE)) {
                entries = getEntryUtil().sortEntriesOnDate(entries,
                        !ascending);
            } else if (sort.equals(SORT_CHANGEDATE)) {
                entries = getEntryUtil().sortEntriesOnChangeDate(entries,
                        !ascending);
            } else if (sort.equals(SORT_NAME)) {
                entries = getEntryUtil().sortEntriesOnName(entries,
                        !ascending);
            } else if (sort.startsWith("number:")) {
                entries = getEntryUtil().sortEntriesOnPattern(entries,
                        ascending, sort.substring(7));

            } else {
                throw new IllegalArgumentException("Unknown sort:" + sort);
            }
        }

        String firstEntries = Misc.getProperty(props,
                                  attrPrefix + ATTR_FIRST, (String) null);

        if (firstEntries != null) {
            Hashtable<String, Entry> map = new Hashtable<String, Entry>();
            for (Entry child : entries) {
                map.put(child.getId(), child);
            }
            List<String> ids = StringUtil.split(firstEntries, ",");
            for (int i = ids.size() - 1; i >= 0; i--) {
                Entry firstEntry = map.get(ids.get(i));
                if (firstEntry == null) {
                    continue;
                }
                entries.remove(firstEntry);
                entries.add(0, firstEntry);
            }
        }


        String lastEntries = Misc.getProperty(props, attrPrefix + ATTR_LAST,
                                 (String) null);

        if (lastEntries != null) {
            Hashtable<String, Entry> map = new Hashtable<String, Entry>();
            for (Entry child : entries) {
                map.put(child.getId(), child);
            }
            List<String> ids = StringUtil.split(lastEntries, ",");
            for (int i = ids.size() - 1; i >= 0; i--) {
                Entry lastEntry = map.get(ids.get(i));
                if (lastEntry == null) {
                    continue;
                }
                entries.remove(lastEntry);
                entries.add(lastEntry);
            }
        }

        String name = Misc.getProperty(props, attrPrefix + ATTR_NAME,
                                       (String) null);
        String pattern = (name == null)
                         ? null
                         : getPattern(name);
        if (name != null) {
            List<Entry> tmp = new ArrayList<Entry>();
            for (Entry child : entries) {
                if (entryMatches(child, pattern, name)) {
                    tmp.add(child);
                }
            }
            entries = tmp;
        }



        if (max > 0) {
            List<Entry> tmp = new ArrayList<Entry>();
            for (Entry child : entries) {
                tmp.add(child);
                if (tmp.size() >= max) {
                    break;
                }
            }
            entries = tmp;
        }

        return entries;

    }


    /**
     * Get the entries corresponding to the ids
     *
     * @param request the Request
     * @param baseEntry _more_
     * @param ids  list of comma separated ids
     * @param props _more_
     *
     * @return List of Entrys
     *
     * @throws Exception problem getting entries
     */
    public List<Entry> getEntries(Request request, Entry baseEntry,
                                  String ids, Hashtable props)
            throws Exception {

        if (props == null) {
            props = new Hashtable();
        }
        Hashtable   searchProps = null;
        List<Entry> entries     = new ArrayList<Entry>();
        Request myRequest = new Request(getRepository(), request.getUser());


        for (String entryid : StringUtil.split(ids, ",", true, true)) {
            if (entryid.startsWith("#")) {
                continue;
            }

            entryid = entryid.replace("_COMMA_", ",");
            Entry  theBaseEntry = baseEntry;
            String type         = null;
            //            entries="children:<other id>
            List<String> toks = StringUtil.splitUpTo(entryid, ":", 2);
            if (toks.size() == 2) {
                //TODO: handle specifying a type
                entryid = toks.get(0);
                String       suffix = toks.get(1);
                List<String> toks2  = StringUtil.splitUpTo(suffix, ":", 2);
                if (toks2.size() == 2) {}
                else {
                    theBaseEntry = getEntryManager().getEntry(request,
                            suffix);
                }
            }


            //            entries="children;type;type
            String filter = null;
            //            children;type=foo
            toks = StringUtil.splitUpTo(entryid, ";", 2);
            if (toks.size() > 1) {
                entryid = toks.get(0);
                filter  = toks.get(1);
            }


            if (entryid.equals(ID_ANCESTORS)) {
                List<Entry> tmp    = new ArrayList<Entry>();
                Entry       parent = theBaseEntry.getParentEntry();
                while (parent != null) {
                    tmp.add(0, parent);
                    parent = parent.getParentEntry();
                }
                entries.addAll(applyFilter(request, tmp, filter, props));

                continue;
            }

            if (entryid.equals(ID_LINKS)) {
                List<Association> associations =
                    getRepository().getAssociationManager().getAssociations(
                        request, theBaseEntry.getId());
                for (Association association : associations) {
                    String id = null;
                    if ( !association.getFromId().equals(
                            theBaseEntry.getId())) {
                        id = association.getFromId();
                    } else if ( !association.getToId().equals(
                            theBaseEntry.getId())) {
                        id = association.getToId();
                    } else {
                        continue;
                    }
                    entries.addAll(applyFilter(request,
                            getEntryManager().getEntry(request, id), filter,
                            props));
                }

                continue;
            }

            if (entryid.equals(ID_ROOT)) {
                entries.addAll(applyFilter(request, request.getRootEntry(),
                                           filter, props));

                continue;
            }

            if (entryid.startsWith(ID_REMOTE)) {
                //TBD
                //http://ramadda.org/repository/entry/show/Home/RAMADDA+Examples?entryid=a96b9616-40b0-41f5-914a-fb1be157d97c
                //                List<String> toks = StringUtil.splitUpTo(entryid, ID_REMOTE,  2);
                //                String url = toks.get(1);
                continue;
            }

            if (entryid.equals(ID_THIS)) {
                entries.addAll(applyFilter(request, theBaseEntry, filter,
                                           props));

                continue;
            }


            if (entryid.startsWith(ATTR_ENTRIES + ".filter")) {
                List<String> tokens = StringUtil.splitUpTo(entryid, "=", 2);
                if (tokens.size() == 2) {
                    props.put(ATTR_ENTRIES + ".filter", tokens.get(1));
                }

                continue;
            }


            boolean isRemote = entryid.startsWith(ATTR_SEARCH_URL);
            if ( !isRemote && entryid.startsWith(ID_SEARCH + ".")) {
                List<String> tokens = StringUtil.splitUpTo(entryid, "=", 2);
                if (tokens.size() == 2) {
                    if (searchProps == null) {
                        searchProps = new Hashtable();
                        searchProps.putAll(props);
                    }
                    searchProps.put(tokens.get(0), tokens.get(1));
                    myRequest.put(tokens.get(0), tokens.get(1));
                }

                continue;
            }

            if (isRemote || entryid.equals(ID_SEARCH)) {
                if (searchProps == null) {
                    searchProps = props;
                }

                myRequest.put(ARG_AREA_MODE,
                              Misc.getProperty(searchProps, ARG_AREA_MODE,
                                  VALUE_AREA_CONTAINS));
                myRequest.put(ARG_MAX,
                              Misc.getProperty(searchProps,
                                  PREFIX_SEARCH + ARG_MAX, "100"));
                addSearchTerms(myRequest, searchProps, theBaseEntry);

                if (isRemote) {
                    List<String> tokens = (entryid.indexOf("=") >= 0)
                                          ? StringUtil.splitUpTo(entryid,
                                              "=", 2)
                                          : StringUtil.splitUpTo(entryid,
                                              ":", 2);
                    ServerInfo serverInfo =
                        new ServerInfo(new URL(tokens.get(1)),
                                       "remote server", "");

                    List<ServerInfo> servers = new ArrayList<ServerInfo>();
                    servers.add(serverInfo);
                    getSearchManager().doDistributedSearch(myRequest,
                            servers, theBaseEntry, entries);

                    continue;
                }

                List<Entry>[] pair = getSearchManager().doSearch(myRequest,
                                         new SearchInfo());
                //                if(myRequest.defined(ARG_PROVIDER)) {
                //List<Entry>[] pair = getEntryManager().getEntries(myRequest);
                entries.addAll(applyFilter(request, pair[0], filter, props));
                entries.addAll(applyFilter(request, pair[1], filter, props));

                continue;
            }


            if (entryid.equals(ID_PARENT)) {
                entries.addAll(
                    applyFilter(
                        request,
                        getEntryManager().getEntry(
                            request,
                            theBaseEntry.getParentEntryId()), filter, props));

                continue;
            }


            if (entryid.startsWith(ID_SIBLINGS)) {
                Entry parent = getEntryManager().getEntry(request,
                                   theBaseEntry.getParentEntryId());
                if (parent != null) {
                    for (Entry sibling :
                            getEntryManager().getChildren(request, parent)) {
                        if ( !sibling.getId().equals(theBaseEntry.getId())) {
                            if (type != null) {
                                if ( !sibling.getTypeHandler().isType(type)) {
                                    continue;
                                }
                            }
                            entries.addAll(applyFilter(request, sibling,
                                    filter, props));
                        }
                    }
                }

                continue;
            }


            if (entryid.equals(ID_GRANDPARENT)) {
                Entry parent = getEntryManager().getEntry(request,
                                   theBaseEntry.getParentEntryId());
                if (parent != null) {
                    Entry grandparent = getEntryManager().getEntry(request,
                                            parent.getParentEntryId());
                    if (grandparent != null) {
                        entries.addAll(applyFilter(request, grandparent,
                                filter, props));
                    }
                }

                continue;
            }


            if (entryid.equals(ID_CHILDREN)) {
                List<Entry> children = getEntryManager().getChildren(request,
                                           theBaseEntry);
                entries.addAll(children);

                continue;
            }


            if (entryid.equals(ID_GRANDCHILDREN)
                    || entryid.equals(ID_GREATGRANDCHILDREN)) {
                List<Entry> children = getEntryManager().getChildren(request,
                                           theBaseEntry);
                List<Entry> grandChildren = new ArrayList<Entry>();
                for (Entry child : children) {
                    //Include the children non folders
                    if ( !child.isGroup()) {
                        grandChildren.add(child);
                    } else {
                        grandChildren.addAll(
                            getEntryManager().getChildren(request, child));
                    }
                }


                if (entryid.equals(ID_GREATGRANDCHILDREN)) {
                    List<Entry> greatgrandChildren = new ArrayList<Entry>();
                    for (Entry child : grandChildren) {
                        if ( !child.isGroup()) {
                            greatgrandChildren.add(child);
                        } else {
                            greatgrandChildren.addAll(
                                getEntryManager().getChildren(
                                    request, child));

                        }
                    }
                    grandChildren = greatgrandChildren;
                }

                entries.addAll(
                    applyFilter(
                        request,
                        getEntryUtil().sortEntriesOnDate(
                            grandChildren, true), filter, props));

                continue;
            }

            boolean addChildren = false;
            if (entryid.startsWith("+")) {
                addChildren = true;
                entryid     = entryid.substring(1);
            }

            Entry entry = getEntryManager().getEntry(request, entryid);
            if (entry != null) {
                if (addChildren) {
                    List<Entry> children =
                        getEntryManager().getChildrenAll(request, entry,
                            null);
                    entries.addAll(applyFilter(request, children, filter,
                            props));
                } else {
                    entries.addAll(applyFilter(request, entry, filter,
                            props));
                }
            }
        }

        HashSet     seen = new HashSet();
        List<Entry> tmp  = new ArrayList<Entry>();
        for (Entry entry : entries) {
            if ( !seen.contains(entry.getId())) {
                seen.add(entry.getId());
                tmp.add(entry);
            }
        }
        entries = tmp;


        return entries;

    }




    /**
     * _more_
     *
     * @param request _more_
     * @param props _more_
     * @param baseEntry _more_
     *
     * @throws Exception _more_
     */
    private void addSearchTerms(Request request, Hashtable props,
                                Entry baseEntry)
            throws Exception {
        String[] args = new String[] {
            ARG_TEXT, ARG_TYPE, ARG_GROUP, ARG_FILESUFFIX, ARG_BBOX,
            ARG_BBOX + ".north", ARG_BBOX + ".west", ARG_BBOX + ".south",
            ARG_BBOX + ".east", DateArgument.ARG_DATA.getFrom(),
            SearchManager.ARG_PROVIDER, DateArgument.ARG_DATA.getTo(),
            DateArgument.ARG_DATA.getRelative(),
            DateArgument.ARG_CREATE.getFrom(),
            DateArgument.ARG_CREATE.getTo(),
            DateArgument.ARG_CREATE.getRelative(),
            DateArgument.ARG_CHANGE.getFrom(),
            DateArgument.ARG_CHANGE.getTo(),
            DateArgument.ARG_CHANGE.getRelative(),
        };
        for (String arg : args) {
            String text = (String) props.get(PREFIX_SEARCH + arg);
            if (text == null) {
                text = (String) props.get(arg);
            }

            if (text != null) {
                if (arg.equals(ARG_GROUP)) {
                    //TODO: Handle other identifiers
                    if (text.equals(ID_THIS)) {
                        text = baseEntry.getId();
                    }
                }
                request.put(arg, text);
            }
        }
    }


    /**
     * Make the gallery
     *
     * @param request   the request
     * @param imageEntries  the list of image entries
     * @param props         the tag properties
     * @param sb            the string buffer to add to
     *
     * @throws Exception  problem making the gallery
     */
    public void makeGallery(Request request, List<Entry> imageEntries,
                            Hashtable props, StringBuilder sb)
            throws Exception {

        int     width        = Misc.getProperty(props, ATTR_WIDTH, -90);
        int serverImageWidth = Misc.getProperty(props, ATTR_IMAGEWIDTH, -1);

        int     columns      = Misc.getProperty(props, ATTR_COLUMNS, 3);
        boolean random       = Misc.getProperty(props, ATTR_RANDOM, false);
        boolean popup        = Misc.getProperty(props, ATTR_POPUP, true);
        boolean thumbnail    = Misc.getProperty(props, ATTR_THUMBNAIL, true);
        String  caption = Misc.getProperty(props, ATTR_CAPTION, "${name}");
        String captionPos = Misc.getProperty(props, ATTR_POPUPCAPTION,
                                             "none");
        boolean showDesc = Misc.getProperty(props, ATTR_SHOWDESCRIPTION,
                                            false);
        if (popup) {
            addImagePopupJS(request, sb, props);
        }
        int size = imageEntries.size();
        if (random && (size > 1)) {
            int randomIdx = (int) (Math.random() * size);
            if (randomIdx >= size) {
                randomIdx = size;
            }
            Entry randomEntry = imageEntries.get(randomIdx);
            imageEntries = new ArrayList<Entry>();
            imageEntries.add(randomEntry);
        }


        StringBuilder[] colsSB = new StringBuilder[columns];
        for (int i = 0; i < columns; i++) {
            colsSB[i] = new StringBuilder();
        }
        int num    = 0;
        int colCnt = 0;

        for (Entry child : imageEntries) {
            num++;
            if (colCnt >= columns) {
                colCnt = 0;
            }
            StringBuilder buff = colsSB[colCnt];
            colCnt++;
            String url = null;

            if (thumbnail) {
                List<String> urls = new ArrayList<String>();
                getMetadataManager().getThumbnailUrls(request, child, urls);
                if (urls.size() > 0) {
                    url = urls.get(0);
                }
            }

            if (url == null) {
                url = child.getTypeHandler().getEntryResourceUrl(request,
                        child);
                /*                url = HtmlUtils.url(
                    request.makeUrl(repository.URL_ENTRY_GET) + "/"
                    + getStorageManager().getFileTail(child), ARG_ENTRYID,
                    child.getId());*/
            }
            if (serverImageWidth > 0) {
                url = url + "&" + ARG_IMAGEWIDTH + "=" + serverImageWidth;
            }


            String extra = "";
            if (width > 0) {
                extra = extra
                        + HtmlUtils.attr(HtmlUtils.ATTR_WIDTH, "" + width);
            } else if (width < 0) {
                extra = extra
                    + HtmlUtils.attr(HtmlUtils.ATTR_WIDTH, "" + (-width)+"%");
            }
            String name = getEntryDisplayName(child);
            if ((name != null) && !name.isEmpty()) {
                extra = extra + HtmlUtils.attr(HtmlUtils.ATTR_ALT, name);
            }
            String img = HtmlUtils.img(url, "", extra);

            String entryUrl =
                request.entryUrl(getRepository().URL_ENTRY_SHOW, child);
            buff.append("<div class=\"image-outer\">");
            buff.append("<div class=\"image-inner\">");
            String theCaption = caption;
            theCaption = theCaption.replace("${count}", "" + num);
            theCaption =
                theCaption.replace("${date}",
                                   formatDate(request,
                                       new Date(child.getStartDate())));
            theCaption = theCaption.replace("${name}", child.getLabel());
            theCaption = theCaption.replace("${description}",
                                            child.getDescription());

            if (popup) {
                String popupExtras = HtmlUtils.cssClass("popup_image") + HtmlUtils.attr("width","100%");
                if ( !captionPos.equals("none")) {
                    popupExtras += HtmlUtils.attr("title", theCaption);
                }
                buff.append(
                    HtmlUtils.href(
                        child.getTypeHandler().getEntryResourceUrl(
                            request, child), img, popupExtras));
            } else {
                buff.append(img);
            }
            buff.append("</div>");


            theCaption =
                HtmlUtils.href(entryUrl, theCaption,
                               HtmlUtils.style("color:#666;font-size:10pt;"));

            buff.append(HtmlUtils.div(theCaption,
                                      HtmlUtils.cssClass("image-caption")));
            if (showDesc) {
                if (Utils.stringDefined(child.getDescription())) {
                    buff.append("<div class=\"image-description\">");
                    buff.append(child.getDescription());
                    buff.append("</div>");
                }
            }

            buff.append("</div>");
        }
        int colInt = 12/Math.min(12,columns);
        String colClass = "col-md-"+ colInt;
        HtmlUtils.open(sb,"div",HtmlUtils.cssClass("row"));
        for (StringBuilder buff : colsSB) {
            HtmlUtils.open(sb, "div", HtmlUtils.cssClass(colClass)+ HtmlUtils.style("padding-left:5px; padding-right:5px;"));
            sb.append(buff);
            HtmlUtils.close(sb, "div");
        }
        HtmlUtils.close(sb, "div");
    }


    /**
     * utility to get the htmloutputhandler
     *
     * @return htmloutputhandler
     */
    public HtmlOutputHandler getHtmlOutputHandler() {
        return getRepository().getHtmlOutputHandler();
    }

    /**
     * Get the calendar output handler
     *
     * @return the calendar output handler
     */
    public CalendarOutputHandler getCalendarOutputHandler() {
        try {
            return (CalendarOutputHandler) getRepository().getOutputHandler(
                CalendarOutputHandler.OUTPUT_CALENDAR);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public GraphOutputHandler getGraphOutputHandler() {
        try {
            return (GraphOutputHandler) getRepository().getOutputHandler(
                GraphOutputHandler.OUTPUT_GRAPH);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }



    /**
     * Handle the wiki import
     *
     * @param wikiUtil The wiki util
     * @param request The request
     * @param originalEntry _more_
     * @param importEntry  the import entry
     * @param tag   the tag
     * @param props properties
     *
     * @return the include output
     */
    private String handleWikiImport(WikiUtil wikiUtil, final Request request,
                                    Entry originalEntry, Entry importEntry,
                                    String tag, Hashtable props) {
        try {
            if ( !tag.equals(WIKI_TAG_IMPORT)) {
                String include = my_getWikiInclude(wikiUtil, request,
                                     originalEntry, importEntry, tag, props,
                                     tag.equals(WIKI_TAG_APPLY));
                if (include != null) {
                    return include;
                }
            } else {
                if (originalEntry.getId().equals(importEntry.getId())) {
                    return "<b>Error: Circular import</b>";
                }
                tag = Misc.getProperty(props, ATTR_OUTPUT,
                                       OutputHandler.OUTPUT_HTML.getId());
            }

            OutputHandler handler = getRepository().getOutputHandler(tag);
            if (handler == null) {
                return null;
            }

            Request myRequest =
                new Request(request,
                            getRepository().URL_ENTRY_SHOW.toString()) {
                public void putExtraProperty(Object key, Object value) {
                    request.putExtraProperty(key, value);
                }
                public Object getExtraProperty(Object key) {
                    return request.getExtraProperty(key);
                }
            };

            for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
                Object key = keys.nextElement();
                myRequest.put(key, props.get(key));
            }

            OutputType outputType = handler.findOutputType(tag);
            myRequest.put(ARG_ENTRYID, importEntry.getId());
            myRequest.put(ARG_OUTPUT, outputType.getId());
            myRequest.put(ARG_EMBEDDED, "true");

            Result result = getEntryManager().processEntryShow(myRequest,
                                importEntry);
            if (result == null) {
                return null;
            }

            String content = result.getStringContent();
            String title = Misc.getProperty(props, ATTR_TITLE,
                                            result.getTitle());

            boolean inBlock = Misc.getProperty(props, ATTR_SHOWTOGGLE,
                                  Misc.getProperty(props, ATTR_SHOWHIDE,
                                      false));
            if (inBlock && (title != null)) {
                boolean open = Misc.getProperty(props, ATTR_OPEN, true);

                return HtmlUtils.makeShowHideBlock(title, content, open,
                        HtmlUtils.cssClass(CSS_CLASS_HEADING_2), "",
                        iconUrl("ramadda.icon.togglearrowdown"),
                        iconUrl("ramadda.icon.togglearrowright"));
            }

            return content;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * Find the wiki entry for the request
     *
     * @param request The request
     * @param wikiUtil The wiki util
     * @param name     the name
     * @param parent   the parent
     *
     * @return the Entry
     *
     * @throws Exception problem retreiving Entry
     */
    public Entry findWikiEntry(Request request, WikiUtil wikiUtil,
                               String name, Entry parent)
            throws Exception {
        Entry theEntry = null;
        if ((parent != null  /* top group */
                ) && parent.isGroup()) {
            String pattern = getPattern(name);
            for (Entry child :
                    getEntryManager().getChildren(request, (Entry) parent)) {
                if (entryMatches(child, pattern, name)) {
                    return child;
                }
            }
        }
        theEntry = getEntryManager().getEntry(request, name);
        if (theEntry != null) {
            return theEntry;
        }

        return theEntry;
    }


    /**
     * Check to see if an entry matches the pattern or name
     *
     * @param child   the child
     * @param pattern the pattern
     * @param name    the name
     *
     * @return true if a match
     */
    private boolean entryMatches(Entry child, String pattern, String name) {
        String entryName = child.getName().trim().toLowerCase();
        if (pattern != null) {
            if (entryName.matches(pattern)) {
                return true;
            }
            String path = child.getResource().getPath();
            if (path != null) {
                path = path.toLowerCase();
                if (path.matches(pattern)) {
                    return true;
                }
            }
        } else if (name.startsWith("type:")) {
            if (child.getTypeHandler().isType(
                    name.substring("type:".length()))) {
                return true;
            }
        } else if (entryName.equalsIgnoreCase(name)) {
            return true;
        }

        return false;

    }

    /**
     * Make a pattern from the name
     *
     * @param name  the name
     *
     * @return the regex pattern
     */
    private String getPattern(String name) {
        return ((name.indexOf("*") >= 0)
                ? StringUtil.wildcardToRegexp(name)
                : null);
    }

    /**
     *
     * Make the wiki edit bar
     *
     * @param request The request
     * @param entry   the Entry
     * @param textAreaId  the textAreaId
     *
     * @return  the edit bar
     *
     * @throws Exception problems
     */
    public String makeWikiEditBar(Request request, Entry entry,
                                  String textAreaId)
            throws Exception {


        StringBuilder buttons = new StringBuilder();
        buttons.append(
            addWikiEditButton(
                textAreaId, "button_section.png", "Section",
                "+section title={{name}}newlinenewline", "-section",
                "Section", "mw-editbutton-bold"));
        buttons.append(addWikiEditButton(textAreaId, "button_bold.png",
                                         "Bold text", "\\'\\'\\'",
                                         "\\'\\'\\'", "Bold text",
                                         "mw-editbutton-bold"));
        buttons.append(addWikiEditButton(textAreaId, "button_italic.png",
                                         "Italic text", "\\'\\'", "\\'\\'",
                                         "Italic text",
                                         "mw-editbutton-italic"));
        buttons.append(addWikiEditButton(textAreaId, "button_link.png",
                                         "Internal link", "[[", "]]",
                                         "Link title", "mw-editbutton-link"));
        buttons.append(
            addWikiEditButton(
                textAreaId, "button_extlink.png",
                "External link (remember http:// prefix)", "[", "]",
                "http://www.example.com link title",
                "mw-editbutton-extlink"));
        buttons.append(addWikiEditButton(textAreaId, "button_headline.png",
                                         "Level 2 headline", "\\n== ",
                                         " ==\\n", "Headline text",
                                         "mw-editbutton-headline"));
        buttons.append(addWikiEditButton(textAreaId, "button_linebreak.png",
                                         "Line break", "<br>", "", "",
                                         "mw-editbutton-headline"));
        buttons.append(addWikiEditButton(textAreaId, "button_strike.png",
                                         "Strike Through", "<s>", "</s>",
                                         "Strike-through text",
                                         "mw-editbutton-headline"));
        buttons.append(addWikiEditButton(textAreaId,
                                         "button_upper_letter.png",
                                         "Super Script", "<sup>", "</sup>",
                                         "Super script text",
                                         "mw-editbutton-headline"));
        buttons.append(addWikiEditButton(textAreaId,
                                         "button_lower_letter.png",
                                         "Sub Script", "<sub>", "</sub>",
                                         "Subscript script text",
                                         "mw-editbutton-headline"));
        buttons.append(addWikiEditButton(textAreaId, "button_small.png",
                                         "Small text", "<small>", "</small>",
                                         "Small text",
                                         "mw-editbutton-headline"));
        buttons.append(addWikiEditButton(textAreaId, "button_blockquote.png",
                                         "Insert block quote",
                                         "<blockquote>", "</blockquote>",
                                         "Quoted text",
                                         "mw-editbutton-headline"));
        //        buttons.append(addWikiEditButton(textAreaId,"button_image.png","Embedded file","[[File:","]]","Example.jpg","mw-editbutton-image"));
        //        buttons.append(addWikiEditButton(textAreaId,"button_media.png","File link","[[Media:","]]","Example.ogg","mw-editbutton-media"));
        //        buttons.append(addWikiEditButton(textAreaId,"button_nowiki.png","Ignore wiki formatting","\\x3cnowiki\\x3e","\\x3c/nowiki\\x3e","Insert non-formatted text here","mw-editbutton-nowiki"));
        //        buttons.append(addWikiEditButton(textAreaId,"button_sig.png","Your signature with timestamp","--~~~~","","","mw-editbutton-signature"));
        buttons.append(addWikiEditButton(textAreaId, "button_hr.png",
                                         "Horizontal line", "\\n----\\n", "",
                                         "", "mw-editbutton-hr"));


        StringBuilder importMenu = new StringBuilder();
        String        inset      = "&nbsp;&nbsp;";
        int           rowCnt     = 0;
        importMenu.append("<table border=0><tr valign=top><td valign=top>\n");
        for (int i = 0; i < WIKITAGS.length; i++) {
            WikiTagCategory cat = WIKITAGS[i];
            if (rowCnt + cat.tags.length > 10) {
                rowCnt = 0;
                if (i > 0) {
                    importMenu.append(
                        "</td><td>&nbsp;</td><td valign=top>\n");
                }
            }
            importMenu.append("\n");

            importMenu.append(HtmlUtils.b(cat.category));
            importMenu.append(HtmlUtils.br());
            rowCnt += cat.tags.length;
            for (int tagIdx = 0; tagIdx < cat.tags.length; tagIdx++) {
                WikiTag tag          = cat.tags[tagIdx];
                String  textToInsert = tag.tag;
                if (tag.attrs.length() > 0) {
                    textToInsert += " " + tag.attrs;
                }

                String js2 = "javascript:insertTags("
                             + HtmlUtils.squote(textAreaId) + ","
                             + HtmlUtils.squote("{{" + textToInsert + " ")
                             + "," + HtmlUtils.squote("}}") + ","
                             + HtmlUtils.squote("") + ");";
                importMenu.append(inset);
                importMenu.append(HtmlUtils.href(js2, tag.label));
                importMenu.append(HtmlUtils.br());
                importMenu.append("\n");
            }
            importMenu.append(HtmlUtils.br());
        }
        importMenu.append("</td></tr></table>\n");
        List<Link> links = getRepository().getOutputLinks(request,
                               new OutputHandler.State(entry));



        for (Link link : links) {
            if (link.getOutputType() == null) {
                continue;
            }

            String prop = link.getOutputType().getId();
            String js = "javascript:insertTags("
                        + HtmlUtils.squote(textAreaId) + ","
                        + HtmlUtils.squote("{{") + ","
                        + HtmlUtils.squote("}}") + ","
                        + HtmlUtils.squote(prop) + ");";
        }


        StringBuilder importOutputMenu = new StringBuilder();
        /*
                List<OutputType> allTypes = getRepository().getOutputTypes();
                //        importMenu.append("<hr>");
                for(OutputType type: allTypes) {
                    String prop = type.getId();
                    String js = "javascript:insertTags(" + HtmlUtils.squote(textAreaId)+"," +
                        HtmlUtils.squote("{{import ") +","+
                        HtmlUtils.squote(" " + type.getId()+" }}") +","+
                        HtmlUtils.squote("entryid")+");";
                    importOutputMenu.append(HtmlUtils.href(js,type.getLabel()));
                    importOutputMenu.append(HtmlUtils.br());
                }
        */


        String importMenuLabel = msg("Add property");
        //            HtmlUtils.img(iconUrl("/icons/wiki/button_import.png"),
        //                         "Import Entry Property");
        String importButton = getPageHandler().makePopupLink(importMenuLabel,
                                  HtmlUtils.hbox(importMenu.toString(),
                                      importOutputMenu.toString()));

        String addEntry = OutputHandler.getSelect(request, textAreaId,
                              "Add entry id", true, "entryid", entry, false);


        String addLink = OutputHandler.getSelect(request, textAreaId,
                             "Add entry link", true, "wikilink", entry,
                             false);

        buttons.append(HtmlUtils.space(2));
        buttons.append(importButton);
        buttons.append(HtmlUtils.space(2));
        buttons.append(addEntry);
        buttons.append(HtmlUtils.space(2));
        buttons.append(addLink);

        return buttons.toString();

    }






    /**
     * Add a wiki edit button
     *
     *
     * @param textAreaId  the TextArea
     * @param icon        the icon
     * @param label       the label
     * @param prefix      the prefix
     * @param suffix      the suffix
     * @param example     example string
     * @param huh         huh?
     *
     * @return  the html for the button
     */
    private String addWikiEditButton(String textAreaId, String icon,
                                     String label, String prefix,
                                     String suffix, String example,
                                     String huh) {
        String prop = prefix + example + suffix;
        String js;
        if (suffix.length() == 0) {
            js = "javascript:insertText(" + HtmlUtils.squote(textAreaId)
                 + "," + HtmlUtils.squote(prop) + ");";
        } else {
            js = "javascript:insertTags(" + HtmlUtils.squote(textAreaId)
                 + "," + HtmlUtils.squote(prefix) + ","
                 + HtmlUtils.squote(suffix) + "," + HtmlUtils.squote(example)
                 + ");";
        }

        return HtmlUtils.href(js,
                              HtmlUtils.img(iconUrl("/icons/wiki/" + icon),
                                            label));

    }




    /**
     * Get a wiki link
     *
     * @param wikiUtil The wiki util
     * @param name     the name
     * @param label    the label
     *
     * @return  the wiki link
     */
    public String getWikiLink(WikiUtil wikiUtil, String name, String label) {

        try {
            Entry   entry   = (Entry) wikiUtil.getProperty(ATTR_ENTRY);
            Request request = (Request) wikiUtil.getProperty(ATTR_REQUEST);
            Entry   parent  = entry.getParentEntry();


            name = name.trim();
            String outputType = null;
            if (name.indexOf("#") > 0) {
                List<String> foo = StringUtil.split(name, "#");
                name = foo.get(0);
                if (foo.size() > 1) {
                    outputType = foo.get(1);
                }
            }


            if (name.startsWith("Category:")) {
                String category = name.substring("Category:".length());
                String url =
                    request.makeUrl(
                        getRepository().getSearchManager().URL_ENTRY_SEARCH,
                        ARG_METADATA_TYPE + ".wikicategory", "wikicategory",
                        ARG_METADATA_ATTR1 + ".wikicategory", category);
                wikiUtil.addCategoryLink(HtmlUtils.href(url, category));
                List categories =
                    (List) wikiUtil.getProperty("wikicategories");
                if (categories == null) {
                    wikiUtil.putProperty("wikicategories",
                                         categories = new ArrayList());
                }
                categories.add(category);

                return "";
            }

            Entry theEntry = null;
            theEntry = getEntryManager().findEntryWithName(request,
                    (Entry) entry, name);

            //If the entry is a group first check its children.
            if (theEntry == null) {
                if (entry.isGroup()) {
                    theEntry = findWikiEntry(request, wikiUtil, name,
                                             (Entry) entry);
                }
            }
            if (theEntry == null) {
                theEntry = findWikiEntry(request, wikiUtil, name, parent);
            }

            if (theEntry != null) {
                addWikiLink(wikiUtil, theEntry);
                if (label.trim().length() == 0) {
                    label = getEntryDisplayName(theEntry);
                }
                if (theEntry.getType().equals(TYPE_WIKIPAGE)) {
                    String url =
                        request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                         theEntry, ARG_OUTPUT,
                                         OUTPUT_WIKI.toString());

                    return getEntryManager().getTooltipLink(request,
                            theEntry, label, url);

                } else {
                    String url = null;
                    // Add output
                    if (outputType != null) {
                        url = HtmlUtils.url(
                            request.entryUrl(
                                getRepository().URL_ENTRY_SHOW,
                                theEntry), ARG_OUTPUT, outputType);
                    }

                    return getEntryManager().getTooltipLink(request,
                            theEntry, label, url);
                }
            }


            //If its an anonymous user then jusst show the label or the name
            if (request.isAnonymous()) {
                String extra = HtmlUtils.cssClass("wiki-link-noexist");
                if ((label != null) && (label.length() > 0)) {
                    return HtmlUtils.span(label, extra);
                }

                return HtmlUtils.span(name, extra);
            }

            String url = request.makeUrl(getRepository().URL_ENTRY_FORM,
                                         ARG_NAME, name, ARG_GROUP,
                                         (entry.isGroup()
                                          ? entry.getId()
                                          : parent.getId()), ARG_TYPE,
                                              TYPE_WIKIPAGE);

            return HtmlUtils.href(url, name,
                                  HtmlUtils.cssClass("wiki-link-noexist"));
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }

    }



    /**
     * Wikify the entry
     *
     * @param request The request
     * @param entry   the Entry
     * @param wikiContent  the content
     *
     * @return wikified content
     *
     * @throws Exception  problem wikifying
     */
    public String wikifyEntry(Request request, Entry entry,
                              String wikiContent)
            throws Exception {
        return wikifyEntry(request, entry, wikiContent, true, null, null);
    }


    /**
     * Wikify the entry
     *
     * @param request The request
     * @param entry   the Entry
     * @param wikiContent  the content to wikify
     * @param wrapInDiv    true to wrap in a div tag
     * @param subGroups    the list of subgroups to include
     * @param subEntries   the list of subentries to include
     *
     * @return the wikified Entry
     *
     * @throws Exception  problem wikifying
     */
    public String wikifyEntry(Request request, Entry entry,
                              String wikiContent, boolean wrapInDiv,
                              List<Entry> subGroups, List<Entry> subEntries)
            throws Exception {
        return wikifyEntry(request, entry, wikiContent, wrapInDiv, subGroups, subEntries, null);
    }

        
    public String wikifyEntry(Request request, Entry entry,
                              String wikiContent, boolean wrapInDiv,
                              List<Entry> subGroups, List<Entry> subEntries,
                              String[]notTags)
            throws Exception {
        
        Request myRequest = request.cloneMe();
        WikiUtil wikiUtil =
            initWikiUtil(myRequest,
                         new WikiUtil(Misc.newHashtable(new Object[] {
                             ATTR_REQUEST,
                             myRequest, ATTR_ENTRY, entry })), entry);

        return wikifyEntry(request, entry, wikiUtil, wikiContent, wrapInDiv,
                           subGroups, subEntries, notTags);
    }


    /**
     * Wikify the entry
     *
     * @param request The request
     * @param entry   the Entry
     * @param wikiUtil The wiki util
     * @param wikiContent  the content to wikify
     * @param wrapInDiv    true to wrap in a div tag
     * @param subGroups    the list of subgroups to include
     * @param subEntries   the list of subentries to include
     *
     * @return the wikified Entry
     *
     * @throws Exception  problem wikifying
     */
    public String wikifyEntry(Request request, Entry entry,
                              WikiUtil wikiUtil, String wikiContent,
                              boolean wrapInDiv, List<Entry> subGroups,
                              List<Entry> subEntries, String[]notTags)
            throws Exception {
        List children = new ArrayList();
        if (subGroups != null) {
            wikiUtil.putProperty(entry.getId() + "_subgroups", subGroups);
            children.addAll(subGroups);
        }

        if (subEntries != null) {
            wikiUtil.putProperty(entry.getId() + "_subentries", subEntries);
            children.addAll(subEntries);
        }

        //TODO: We need to keep track of what is getting called so we prevent
        //infinite loops
        String content = wikiUtil.wikify(wikiContent, this, notTags);
        if ( !wrapInDiv) {
            return content;
        }

        return HtmlUtils.div(content, HtmlUtils.cssClass("wikicontent"));
    }

    /**
     * Add a wiki link
     *
     * @param wikiUtil The wiki util
     * @param toEntry  the entry to add to
     */
    public void addWikiLink(WikiUtil wikiUtil, Entry toEntry) {
        if (toEntry == null) {
            return;
        }
        Hashtable links = (Hashtable) wikiUtil.getProperty("wikilinks");
        if (links == null) {
            wikiUtil.putProperty("wikilinks", links = new Hashtable());
        }
        links.put(toEntry, toEntry);
    }


    /**
     * Class for holding attributes
     */
    public static class Attr {

        /** Attribute name */
        String name;

        /** the default */
        String dflt;

        /** the label */
        String label;

        /**
         * Create an Attribute
         *
         * @param name  the name
         * @param dflt  the default
         * @param label the label
         */
        public Attr(String name, String dflt, String label) {
            this.name  = name;
            this.dflt  = dflt;
            this.label = label;
        }

        /**
         * Return a String version of this object
         *
         * @return a String version of this object
         */
        public String toString() {
            return name;
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param props _more_
     * @param originalEntry _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getDescription(Request request, Hashtable props,
                                  Entry originalEntry, Entry entry)
            throws Exception {
        String  content;
        boolean wikify = Misc.getProperty(props, ATTR_WIKIFY, false);
        if (entry.getTypeHandler().isType(TYPE_WIKIPAGE)) {
            content = entry.getValue(0, entry.getDescription());
            wikify  = true;
        } else {
            content = entry.getDescription();
        }

        if (wikify) {
            if ( !originalEntry.equals(entry)) {
                content = wikifyEntry(request, entry, content, false, null,
                                      null);
            } else {
                content = makeWikiUtil(request, false).wikify(content, null);
            }
        }

        return content;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param props _more_
     * @param originalEntry _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String makeSimpleDisplay(Request request, Hashtable props,
                                    Entry originalEntry, Entry entry)
            throws Exception {


        String fromType = entry.getTypeHandler().getSimpleDisplay(request,
                              props, entry);
        if (fromType != null) {
            return fromType;
        }


        boolean sizeConstrained = Misc.getProperty(props, ATTR_CONSTRAINSIZE,
                                      false);;
        String  content = getDescription(request, props, originalEntry,
                                         entry);
        boolean haveText = Utils.stringDefined(content);
        String  imageUrl = null;

        if (entry.isImage()) {
            imageUrl =
                getRepository().getHtmlOutputHandler().getImageUrl(request,
                    entry);
        } else {
            List<String> urls = new ArrayList<String>();
            getMetadataManager().getThumbnailUrls(request, entry, urls);
            if (urls.size() > 0) {
                imageUrl = urls.get(0);
            }
        }


        if (imageUrl != null) {
            StringBuilder extra = new StringBuilder();
            String position = request.getString(ATTR_TEXTPOSITION, POS_LEFT);
            boolean layoutHorizontal = position.equals(POS_RIGHT)
                                       || position.equals(POS_LEFT);
            int imageWidth = 0;

            if (sizeConstrained) {
                imageWidth = Misc.getProperty(props, ATTR_WIDTH, 400);
                //Give some space to the text on the side
                if (haveText && layoutHorizontal) {
                    imageWidth -= 200;
                }
            }

            imageWidth = Misc.getProperty(props, ATTR_IMAGEWIDTH, imageWidth);

            if (imageWidth > 0) {
                extra.append(HtmlUtils.attr(HtmlUtils.ATTR_WIDTH,
                                            "" + imageWidth));
            } else if (imageWidth < 0) {
                extra.append(HtmlUtils.attr(HtmlUtils.ATTR_WIDTH,
                                            "" + (-imageWidth) + "%"));
            }

            String alt = request.getString(ATTR_ALT,
                                           getEntryDisplayName(entry));
            String imageClass = request.getString("imageclass",
                                    (String) null);
            if (Utils.stringDefined(alt)) {
                extra.append(HtmlUtils.attr(ATTR_ALT, alt));
            }
            String image = HtmlUtils.img(imageUrl, "", extra.toString());
            if (request.get(WikiManager.ATTR_LINK, true)) {
                image = HtmlUtils.href(
                    request.entryUrl(getRepository().URL_ENTRY_SHOW, entry),
                    image);
                /*  Maybe add this later
                } else if (request.get(WikiManager.ATTR_LINKRESOURCE, false)) {
                    image =  HtmlUtils.href(
                        entry.getTypeHandler().getEntryResourceUrl(request, entry),
                        image);
                */
            }

            String extraDiv = "";
            if (haveText && sizeConstrained) {
                int height = Misc.getProperty(props, ATTR_HEIGHT, -1);
                if ((height > 0) && position.equals(POS_BOTTOM)) {
                    extraDiv =
                        HtmlUtils.style("overflow-y: hidden; max-height:"
                                        + (height - 75) + "px;");
                }
            }
            image = HtmlUtils.div(image,
                                  HtmlUtils.cssClass("entry-simple-image")
                                  + extraDiv);
            if ( !haveText) {
                return image;
            }

            String textClass = "entry-simple-text";
            if (position.equals(POS_NONE)) {
                content = image;
            } else if (position.equals(POS_BOTTOM)) {
                content = image
                          + HtmlUtils.div(content,
                                          HtmlUtils.cssClass(textClass));
            } else if (position.equals(POS_TOP)) {
                content =
                    HtmlUtils.div(content, HtmlUtils.cssClass(textClass))
                    + image;
            } else if (position.equals(POS_RIGHT)) {
                content =
                    HtmlUtils.table(
                        HtmlUtils.row(
                            HtmlUtils.col(image)
                            + HtmlUtils.col(
                                HtmlUtils.div(
                                    content,
                                    HtmlUtils.cssClass(
                                        textClass))), HtmlUtils.attr(
                                            HtmlUtils.ATTR_VALIGN,
                                            "top")), HtmlUtils.attr(
                                                HtmlUtils.ATTR_CELLPADDING,
                                                    "0"));
            } else if (position.equals(POS_LEFT)) {
                content =
                    HtmlUtils.table(
                        HtmlUtils.row(
                            HtmlUtils.col(
                                HtmlUtils.div(
                                    content,
                                    HtmlUtils.cssClass(
                                        textClass))) + HtmlUtils.col(
                                            image), HtmlUtils.attr(
                                            HtmlUtils.ATTR_VALIGN,
                                            "top")), HtmlUtils.attr(
                                                HtmlUtils.ATTR_CELLPADDING,
                                                    "0"));
            } else {
                content = "Unknown position:" + position;
            }
        }


        if (entry.getTypeHandler().isGroup()
                && entry.getTypeHandler().isType(TypeHandler.TYPE_GROUP)) {
            //Do we tack on the listing
            StringBuilder sb = new StringBuilder();
            List<Entry> children = getEntryManager().getChildren(request,
                                       entry);
            if (children.size() > 0) {
                String link = getHtmlOutputHandler().getEntriesList(request,
                //                                  sb, children, true, false, true);
                sb, children, false, false, false);
                content = content + sb;
            }
        }
        content = HtmlUtils.div(content, HtmlUtils.cssClass("entry-simple"));

        return content;

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
    public String getStandardChartDisplay(Request request, Entry entry)
            throws Exception {
        String        name = entry.getName();
        StringBuilder wiki = new StringBuilder();
        wiki.append(
            "{{group  showTitle=\"true\"  showMenu=\"true\"  layoutType=\"columns\"  layoutColumns=\"2\"  }}\n");
        String chartType =
            entry.getTypeHandler().getTypeProperty("chart.type", "linechart");
        wiki.append(
            "{{display  xwidth=\"600\"  height=\"400\"   type=\"" + chartType
            + "\"  name=\"\"  layoutHere=\"false\"  showMenu=\"true\"  showTitle=\"true\"  row=\"0\"  column=\"0\"  }}");

        if (entry.isGeoreferenced()
        /*|| getEntryManager().isSynthEntry(entry.getId())*/
        ) {
            String mapLayers = getRepository().getProperty(PROP_MAP_LAYERS,
                                   null);
            String layerVar = "";
            if (mapLayers != null) {
                mapLayers = mapLayers.replaceAll(";", ",");
                layerVar  = "mapLayers=\"" + mapLayers + "\"";
            }
            String entryAttrs = entry.getTypeHandler().getProperty(entry, "chart.wiki.map","");
            if(entry.getTypeHandler().getTypeProperty("isTrajectory", false) ||
               entry.getTypeHandler().getProperty(entry, "isTrajectory", false)) {
                entryAttrs +=" isTrajectory=\"true\" ";
            }
            wiki.append(
                "{{display  width=\"600\"  height=\"400\"   type=\"map\" "
                + layerVar
                + entryAttrs
                + " name=\"\"  layoutHere=\"false\"  showMenu=\"true\"  showTitle=\"true\"  row=\"0\"  column=\"1\"  }}");
        }

        Hashtable props = new Hashtable();

        props.put("layoutHere", "false");
        props.put("layoutType", "table");
        props.put("layoutColumns", "2");
        props.put("showMenu", "true");
        props.put("showMap", "" + entry.isGeoreferenced());

        return wikifyEntry(request, entry, wiki.toString());
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param tag _more_
     * @param name _more_
     * @param url _more_
     * @param sb _more_
     * @param props _more_
     *
     * @throws Exception _more_
     */
    public void getEntryDisplay(Request request, Entry entry, String tag,
                                String name, String url, StringBuilder sb,
                                Hashtable props)
            throws Exception {

        this.addDisplayImports(request, sb);
        List<String>  topProps = new ArrayList<String>();


        List<String>  propList = new ArrayList<String>();


        StringBuilder js       = new StringBuilder();


        for (String showArg : new String[] { ATTR_SHOWMAP, ATTR_SHOWMENU }) {
            topProps.add(showArg);
            topProps.add("" + Misc.getProperty(props, showArg, false));
        }

        if (props.get(ATTR_SHOWMENU) != null) {
            propList.add(ATTR_SHOWMENU);
            propList.add(Misc.getProperty(props, ATTR_SHOWMENU, "true"));
            props.remove(ATTR_SHOWMENU);
        }

        String derived = (String) props.get("derived");
        if (derived != null) {
            //derived=temp_f:Temp F:temperature;pressure:v1*9/5+32:isRow:true:decimals:2
            //{'name':'temp_f','label':'Temp F', 'columns':'temperature','function':'v1*9/5+32', 'isRow':true,'decimals':2,},
            List<String> toks = StringUtil.split(derived, ",", true, true);

            List<String> jsonObjects = new ArrayList<String>();
            for (String tok : toks) {
                List<String> toks2 = StringUtil.split(tok, ":", true, true);
                if (toks2.get(0).startsWith("#")) {
                    continue;
                }
                List<String> jObj = new ArrayList<String>();
                jObj.add("name");
                jObj.add(toks2.get(0));
                jObj.add("label");
                jObj.add(toks2.get(1));
                jObj.add("columns");
                jObj.add(toks2.get(2));
                jObj.add("function");
                jObj.add(toks2.get(3));
                for (int i = 4; i < toks2.size(); i += 2) {
                    jObj.add(toks2.get(i));
                    jObj.add(toks2.get(i + 1));
                }
                jsonObjects.add(Json.mapAndQuote(jObj).replaceAll("\n", " "));
            }
            String json = Json.list(jsonObjects);
            //            System.err.println("json:" + json);
            props.put("derived", json);
        }

        String providers = (String) props.get("providers");
        if (providers != null) {
            List<String> processed = new ArrayList<String>();
            for (String tok : StringUtil.split(providers, ",")) {
                //                System.err.println ("Tok:" + tok);
                if (tok.startsWith("name:") || tok.startsWith("category:")) {
                    boolean doName  = tok.startsWith("name:");
                    String  pattern = tok.substring(doName
                            ? "name:".length()
                            : "category:".length());
                    //                    System.err.println ("doName:" + doName +" pattern:" + pattern);
                    for (SearchProvider provider :
                            getSearchManager().getSearchProviders()) {
                        String  target  = doName
                                          ? provider.getName()
                                          : provider.getCategory();
                        boolean include = target.equals(pattern);
                        if ( !include) {
                            try {
                                include = target.matches(pattern);
                            } catch (Exception ignore) {
                                System.err.println("bad pattern:" + pattern);
                            }
                        }

                        if (include) {
                            String icon = provider.getIconUrl();
                            if (icon == null) {
                                icon = "${root}/icons/magnifier.png";
                            }
                            icon = getPageHandler().applyBaseMacros(icon);
                            String v =
                                provider.getId().replace(":", "_COLON_")
                                + ":"
                                + provider.getName().replace(":",
                                    "-").replace(",", " ") + ":" + icon + ":"
                                        + provider.getCategory();

                            processed.add(v);
                        }
                    }

                    continue;
                }

                List<String> subToks = StringUtil.split(tok, ":", true, true);
                if (subToks.size() == 0) {
                    continue;
                }
                SearchProvider searchProvider =
                    getSearchManager().getSearchProvider(subToks.get(0));
                if (searchProvider == null) {
                    System.err.println("Can't find search provider:"
                                       + subToks);

                    continue;

                }
                String id   = searchProvider.getId();
                String icon = searchProvider.getIconUrl();
                if (icon == null) {
                    icon = "${root}/icons/magnifier.png";
                }
                icon = getPageHandler().applyBaseMacros(icon);
                String label = searchProvider.getName();

                if (subToks.size() > 1) {
                    label = subToks.get(1);
                }

                if (subToks.size() > 2) {
                    icon = subToks.get(2);
                }
                String v = id.replace(":", "_COLON_") + ":"
                           + label.replace(":", "-").replace(",", " ") + ":"
                           + icon + ":" + searchProvider.getCategory();
                processed.add(v);
            }
            //            for(String s: processed) {
            //                System.out.println(s);
            //            }
            props.put("providers", StringUtil.join(",", processed));

        }


        String entryParent = (String) props.get("entryParent");
        if (entryParent != null) {
            Entry theEntry = findEntryFromId(request, entry, null,
                                             entryParent);
            if (theEntry != null) {
                props.put("entryParent", theEntry.getId());
            }
        }


        String colors = (String) props.get(ATTR_COLORS);
        if (colors != null) {
            propList.add(ATTR_COLORS);
            propList.add(Json.list(StringUtil.split(colors, ","), true));
            props.remove(ATTR_COLORS);
        }

        boolean showTitle = false;
        if (props.get(ATTR_SHOWTITLE) != null) {
            propList.add(ATTR_SHOWTITLE);
            propList.add(Misc.getProperty(props, ATTR_SHOWTITLE, "true"));
            showTitle = Misc.equals("true",
                                    Misc.getProperty(props, ATTR_SHOWTITLE,
                                        "true"));
            topProps.add(ATTR_SHOWTITLE);
            topProps.add(Misc.getProperty(props, ATTR_SHOWTITLE, "true"));
            props.remove(ATTR_SHOWTITLE);
        }


        String title = Misc.getProperty(props, ATTR_TITLE, (String) null);
        if (title != null) {
            propList.add(ATTR_TITLE);
            propList.add(Json.quote(title));
        } else {
            propList.add(ATTR_TITLE);
            propList.add(Json.quote(entry.getName()));
        }
        topProps.add("layoutType");
        topProps.add(Json.quote(Misc.getProperty(props, "layoutType",
                "table")));
        props.remove("layoutType");
        topProps.add("layoutColumns");
        topProps.add(Misc.getProperty(props, "layoutColumns", "1"));
        props.remove("layoutColumns");

        //Always add the default map layer to the displaymanager properties so any new maps pick it up
        String defaultLayer = Misc.getProperty(
                                  props, "defaultMapLayer",
                                  getRepository().getProperty(
                                      "ramadda.map.defaultlayer",
                                      "google.terrain"));

        topProps.add("defaultMapLayer");
        topProps.add(Json.quote(defaultLayer));


        String mainDivId = (String) props.get("divid");
        if (mainDivId == null) {
            mainDivId = HtmlUtils.getUniqueId("displaydiv");
        }
        mainDivId = mainDivId.replace("$entryid", entry.getId());

        topProps.add("entryId");
        topProps.add(HtmlUtils.quote(entry.getId()));


        if (tag.equals(WIKI_TAG_GROUP) || tag.equals(WIKI_TAG_GROUP_OLD)) {
            for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
                Object key   = keys.nextElement();
                Object value = props.get(key);
                topProps.add(key.toString());
                topProps.add(Json.quote(value.toString()));
            }
            sb.append(HtmlUtils.div("", HtmlUtils.id(mainDivId)));
            sb.append("\n");

            js.append("var displayManager = getOrCreateDisplayManager("
                      + HtmlUtils.quote(mainDivId) + ","
                      + Json.map(topProps, false) + ",true);\n");
            sb.append(HtmlUtils.script(js.toString()));

            return;
        }


        boolean needToCreateGroup = request.getExtraProperty("added group")
                                    == null;
        request.putExtraProperty("added group", "true");

        String fields = Misc.getProperty(props, "fields", (String) null);
        if (fields != null) {
            List<String> toks = StringUtil.split(fields, ",", true, true);
            if (toks.size() > 0) {
                propList.add("fields");
                propList.add(Json.list(toks, true));
            }
            props.remove("fields");
        }

        String  anotherDivId = (String) props.get("divid");
        boolean layoutHere   = Misc.getProperty(props, "layoutHere", true);
        if ((anotherDivId != null) || layoutHere) {
            propList.add("layoutHere");
            propList.add("true");
            if (anotherDivId == null) {
                anotherDivId = HtmlUtils.getUniqueId("displaydiv");
            }
            anotherDivId = anotherDivId.replace("$entryid", entry.getId());
            sb.append(HtmlUtils.div("", HtmlUtils.id(anotherDivId)));
            propList.add("divid");
            propList.add(Json.quote(anotherDivId));
        }
        props.remove("layoutHere");

        //Put the main div after the display div
        sb.append(HtmlUtils.div("", HtmlUtils.id(mainDivId)));
        sb.append("\n");

        for (String arg : new String[] {
            "eventSource", "name", "displayFilter", "chartMin", ARG_WIDTH,
            ARG_HEIGHT, ARG_FROMDATE, ARG_TODATE, "column", "row"
        }) {
            String value = Misc.getProperty(props, arg, (String) null);
            if (value != null) {
                propList.add(arg);
                propList.add(Json.quote(value));
            }
            props.remove(arg);
        }


        //Only add the default layer to the display if its been specified
        defaultLayer = Misc.getProperty(props, "defaultLayer", (String) null);
        if (defaultLayer != null) {
            propList.add("defaultMapLayer");
            propList.add(Json.quote(defaultLayer));
            props.remove("defaultLayer");
        }

        String displayType = Misc.getProperty(props, "type", "linechart");
        props.remove("type");


        for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
            Object key   = keys.nextElement();
            Object value = props.get(key);
            propList.add(key.toString());
            propList.add(Json.quote(value.toString()));
        }




        js.append(
            "\n//This gets the global display manager or creates it if not created\n");
        js.append("var displayManager = getOrCreateDisplayManager("
                  + HtmlUtils.quote(mainDivId) + ","
                  + Json.map(topProps, false) + ");\n");

        propList.add("entryId");
        propList.add(HtmlUtils.quote(entry.getId()));


        if ((url != null) && Misc.getProperty(props, "includeData", true)) {
            String pointProps = "{entryId:'" + entry.getId() + "'}";
            propList.add("data");
            propList.add("new  PointData(" + HtmlUtils.quote(name)
                         + ",  null,null," + HtmlUtils.quote(url) + ","
                         + pointProps + ")");
        }

        js.append("displayManager.createDisplay("
                  + HtmlUtils.quote(displayType) + ","
                  + Json.map(propList, false) + ");\n");

        sb.append("\n");
        sb.append(HtmlUtils.script(js.toString()));
        //        sb.append(HtmlUtils.script(JQuery.ready(js.toString())));

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addDisplayImports(Request request, Appendable sb)
            throws Exception {

        getMapManager().addMapImports(request, sb);

        if (request.getExtraProperty("initchart") == null) {
            request.putExtraProperty("initchart", "added");
            //            getPageHandler().addGoogleJSImport(request, sb);
            sb.append(
                HtmlUtils.importJS(
                    "https://www.gstatic.com/charts/loader.js"));

            sb.append(HtmlUtils.script(
            //                    "google.load(\"visualization\", \"1\", {packages:['corechart','table','bar']});\n"));
            "google.charts.load(\"43\", {packages:['corechart','table','bar']});\n"));
            sb.append(HtmlUtils.importJS(htdocsUrl("/lib/d3/d3.min.js")));

            sb.append(
                HtmlUtils.importJS(
                    htdocsUrl("/lib/jquery.handsontable.full.min.js")));
            sb.append(
                HtmlUtils.cssLink(
                    htdocsUrl("/lib/jquery.handsontable.full.min.css")));


            //Put this here after the google load
            sb.append(HtmlUtils.cssLink(htdocsUrl("/display.css")));


            /*

            sb.append(HtmlUtils.cssLink(htdocsUrl("/lib/timelinejs/css/timeline.css")));
            sb.append(HtmlUtils.importJS(htdocsUrl("/lib/timelinejs/js/timeline.js")));
            sb.append(HtmlUtils.importJS(htdocsUrl("/lib/timelinejs/js/storyjs-embed.js")));
            */
            HtmlUtils.importJS(sb, htdocsUrl("/db/dom-drag.js"));
            sb.append(HtmlUtils.importJS(htdocsUrl("/db/dom-drag.js")));

            sb.append(HtmlUtils.importJS(htdocsUrl("/display/pointdata.js")));
            sb.append(HtmlUtils.importJS(htdocsUrl("/display/utils.js")));
            sb.append(
                HtmlUtils.importJS(htdocsUrl("/display/displaymanager.js")));
            sb.append(HtmlUtils.importJS(htdocsUrl("/display/display.js")));
            sb.append(
                HtmlUtils.importJS(htdocsUrl("/display/displayentry.js")));

            sb.append(
                HtmlUtils.importJS(htdocsUrl("/display/displaymap.js")));
            sb.append(
                HtmlUtils.importJS(htdocsUrl("/display/displaychart.js")));
            sb.append(
                HtmlUtils.importJS(htdocsUrl("/display/displaytable.js")));
            sb.append(HtmlUtils.importJS(htdocsUrl("/display/control.js")));
            sb.append(HtmlUtils.importJS(htdocsUrl("/display/displayd3.js")));
            sb.append(
                HtmlUtils.importJS(htdocsUrl("/display/displayext.js")));
            sb.append(HtmlUtils.importJS(htdocsUrl("/repositories.js")));



            String includes =
                getRepository().getProperty("ramadda.display.includes",
                                            (String) null);
            if (includes != null) {
                for (String include :
                        StringUtil.split(includes, ",", true, true)) {
                    sb.append(HtmlUtils.importJS(fileUrl(include)));
                }
            }
        }
    }



    /**
     * Create an attribute with the name and value
     *
     * @param name  attribute name
     * @param value  value
     *
     * @return  the attribute string
     */
    private static String attr(String name, String value) {
        return " " + name + "=" + value + " ";
    }

    /**
     * Generate a list of attributes
     *
     * @param attrs  set of attrs
     *
     * @return  the string version
     */
    private static String attrs(String... attrs) {
        StringBuilder sb = new StringBuilder();
        String        qt = "&quote;";

        for (int i = 0; i < attrs.length; i += 2) {
            sb.append(attr(attrs[i], qt + attrs[i + 1] + qt));
        }

        return sb.toString();
    }

    /**
     * Create a property string
     *
     * @param prop  the property
     * @param args  the property arguments
     *
     * @return  the property string
     */
    private static String prop(String prop, String args) {
        return prop + PROP_DELIM + args;
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Mar 5, '14
     * @author         Enter your name here...
     */
    private static class WikiTag {

        /** _more_ */
        String label;

        /** _more_ */
        String tag;

        /** _more_ */
        String attrs;

        /**
         * _more_
         *
         * @param tag _more_
         */
        WikiTag(String tag) {
            this(tag, "");
        }

        /**
         * _more_
         *
         * @param tag _more_
         * @param attrs _more_
         */
        WikiTag(String tag, String attrs) {
            this(tag, tag, attrs);
        }

        /**
         * _more_
         *
         * @param tag _more_
         * @param label _more_
         * @param attrs _more_
         */
        WikiTag(String tag, String label, String attrs) {
            this.tag   = tag;
            this.label = label;
            this.attrs = attrs;
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Mar 5, '14
     * @author         Enter your name here...
     */
    private static class WikiTagCategory {

        /** _more_ */
        String category;

        /** _more_ */
        WikiTag[] tags;

        /**
         * _more_
         *
         * @param c _more_
         * @param tagArgs _more_
         */
        WikiTagCategory(String c, WikiTag... tagArgs) {
            this.category = c;

            /**
             * Comparator comp = new Comparator() {
             *   public int compare(Object o1, Object o2) {
             *       return ((WikiTag) o1).tag.compareTo(((WikiTag) o2).tag);
             *   }
             * };
             * Arrays.sort(tagArgs, comp);
             */
            tags = tagArgs;
        }
    }

    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        String s = "hello there\n//some comment\nand after the comment";
        s = s.replaceAll("(?m)^//[^$]*$", "");
        System.err.println(s);
    }

}
