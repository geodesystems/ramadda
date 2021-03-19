/*
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

package org.ramadda.repository.output;


import org.ramadda.data.services.RecordTypeHandler;


import org.ramadda.repository.Association;
import org.ramadda.repository.Constants;
import org.ramadda.repository.DateHandler;
import org.ramadda.repository.Entry;
import org.ramadda.repository.EntryManager;
import org.ramadda.repository.Link;
import org.ramadda.repository.PageDecorator;
import org.ramadda.repository.PageHandler;
import org.ramadda.repository.Repository;
import org.ramadda.repository.RepositoryBase;
import org.ramadda.repository.RepositoryManager;
import org.ramadda.repository.RepositoryUtil;
import org.ramadda.repository.Request;
import org.ramadda.repository.Result;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.map.MapManager;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.metadata.MetadataManager;
import org.ramadda.repository.metadata.MetadataType;
import org.ramadda.repository.search.SearchInfo;
import org.ramadda.repository.search.SearchManager;
import org.ramadda.repository.search.SearchProvider;
import org.ramadda.repository.search.SpecialSearch;
import org.ramadda.repository.type.LocalFileTypeHandler;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.repository.util.DateArgument;
import org.ramadda.repository.util.ServerInfo;


import org.ramadda.util.Bounds;
import org.ramadda.util.BufferMapList;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.Json;
import org.ramadda.util.NamedValue;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import java.io.BufferedReader;


import java.io.File;
import java.io.FileInputStream;
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

import java.util.function.*;


/**
 * Provides wiki text processing services
 */
public class WikiManager extends RepositoryManager implements WikiConstants,
        WikiUtil.WikiPageHandler {



    /** list of import items for the text editor menu */
    //J--
    public static final WikiTagCategory[] WIKITAGS = {
        new WikiTagCategory("General",
                            new WikiTag(WIKI_TAG_INFORMATION, null, ATTR_DETAILS, "false",ATTR_SHOWTITLE,"false"),
                            new WikiTag(WIKI_TAG_NAME), 
                            new WikiTag(WIKI_TAG_DESCRIPTION),
                            new WikiTag(WIKI_TAG_RESOURCE, null, ATTR_TITLE,"",ATTR_INCLUDEICON,"true"), 
                            new WikiTag(WIKI_TAG_DATERANGE,"Date Range", ATTR_FORMAT,DateHandler.DEFAULT_TIME_FORMAT),
                            new WikiTag(WIKI_TAG_DATE_FROM, "From Date", ATTR_FORMAT,DateHandler.DEFAULT_TIME_FORMAT),
                            new WikiTag(WIKI_TAG_DATE_TO,"To Date", ATTR_FORMAT,DateHandler.DEFAULT_TIME_FORMAT), 
                            new WikiTag(WIKI_TAG_DATE_CREATE,"Create Date", ATTR_FORMAT,DateHandler.DEFAULT_TIME_FORMAT), 
                            new WikiTag(WIKI_TAG_DATE_CHANGE,"Change Date", ATTR_FORMAT,DateHandler.DEFAULT_TIME_FORMAT), 
                            new WikiTag(WIKI_TAG_LABEL, null, ATTR_TEXT,"",ATTR_ID,"arbitrary id to match with property"),
                            new WikiTag(WIKI_TAG_LINK, null, ATTR_TITLE,"","button","false"),
                            new WikiTag(WIKI_TAG_HTML),
                            new WikiTag("multi", null, "_attrs", "attr1,attr2"),
                            new WikiTag(WIKI_TAG_SIMPLE, null, ATTR_TEXTPOSITION, POS_LEFT),
                            new WikiTag(WIKI_TAG_IMPORT, null, ATTR_ENTRY,""),
                            new WikiTag(WIKI_TAG_EMBED, null, ATTR_ENTRY,"",ATTR_SKIP_LINES,"0",ATTR_MAX_LINES,"1000",ATTR_FORCE,"false",ATTR_MAXHEIGHT,"300",ATTR_ANNOTATE,"true","raw","true","wikify","true"),
                            new WikiTag(WIKI_TAG_FIELD, null, "name", "")),
        new WikiTagCategory("Layout", 
                            new WikiTag(WIKI_TAG_TREE, null, ATTR_DETAILS, "true"),
                            new WikiTag(WIKI_TAG_FULLTREE, null,"depth","10","addprefix","false","showroot","true","labelWidth","20", "showicon","true","types","group,feile,...."),
                            new WikiTag(WIKI_TAG_MENUTREE, null,"depth","10","addprefix","false","showroot","true","menuStyle","","labelWidth","20", "showicon","true","types","group,file,...."), 			    			    
                            new WikiTag(WIKI_TAG_LINKS, null),
                            new WikiTag(WIKI_TAG_LIST), 
                            new WikiTag(WIKI_TAG_TABS, null), 
                            new WikiTag(WIKI_TAG_GRID, null, 
                                        ATTR_TAG, WIKI_TAG_CARD, 
                                        "inner-height","100", 
                                        ATTR_COLUMNS, "3", ATTR_INCLUDEICON, "true", "weights","",
                                        "showSnippet","false",
                                        "showSnippetHover","true",
                                        "showLink","false","showHeading","true","showLine","true"), 
                            new WikiTag(WIKI_TAG_MAP,
                                        null, ATTR_WIDTH, "100%", ATTR_HEIGHT, "400"), 
                            new WikiTag(WIKI_TAG_FRAMES, null, ATTR_WIDTH,"100%", ATTR_HEIGHT,"500"), 
                            new WikiTag(WIKI_TAG_ACCORDION, null, ATTR_TAG, WIKI_TAG_HTML, ATTR_COLLAPSE, "false", "border", "0", ATTR_SHOWLINK, "true", ATTR_INCLUDEICON, "false",ATTR_TEXTPOSITION, POS_LEFT), 
                            //                            new WikiTag(WIKI_TAG_GRID), 
                            new WikiTag(WIKI_TAG_TABLE), 
                            new WikiTag(WIKI_TAG_RECENT, null, ATTR_DAYS, "3"), 
                            new WikiTag(WIKI_TAG_MULTI, null,"#_tag","","#_template",""), 
                            new WikiTag(WIKI_TAG_APPLY, null, APPLY_PREFIX
					+ "tag", WIKI_TAG_HTML, APPLY_PREFIX
					+ "layout", "table", APPLY_PREFIX
					+ "columns", "1", APPLY_PREFIX
					+ "header", "", APPLY_PREFIX
					+ "footer", "", APPLY_PREFIX
					+ "border", "0", APPLY_PREFIX
                                        + "bordercolor", "#000")),
        new WikiTagCategory("Images",
                            new WikiTag(WIKI_TAG_IMAGE,null,
                                        "#"+ATTR_SRC, "", ATTR_WIDTH,"100%", "#"+ATTR_ALIGN,"left|center|right"), 
                            new WikiTag(WIKI_TAG_GALLERY,null,
                                        ATTR_WIDTH, "-100", ATTR_COLUMNS, "3",
					ATTR_POPUP, "true", ATTR_THUMBNAIL, "false",
					ATTR_CAPTION, "Figure ${count}: ${name}",
					ATTR_POPUPCAPTION,
					"over"), 
                            new WikiTag(WIKI_TAG_SLIDESHOW,"Slide Show",
                                        ATTR_TAG, WIKI_TAG_SIMPLE,
					ATTR_TEXTPOSITION,"top",
					ATTR_SHOWLINK, "true",
					ATTR_WIDTH, "400",
					ATTR_HEIGHT,
					"270",
					"#textClass","note",
					"#textStyle","margin:8px;",
					"#showLink","true",
					"bordercolor","#efefef",
					"#" + ATTR_TEXTPOSITION,"top|left|right|bottom"), 
                            new WikiTag(WIKI_TAG_PLAYER, "Image Player", "loopdelay","1000","loopstart","false","imageWidth","600")),
        new WikiTagCategory("Misc",
                            new WikiTag("counter", null, "key", "key"),
                            new WikiTag("caption", null, "label", "","prefix","Image #:"),
                            new WikiTag(WIKI_TAG_CALENDAR, null, ATTR_DAY, "false"),
                            new WikiTag(WIKI_TAG_TIMELINE, null, ATTR_HEIGHT, "150"),
                            new WikiTag(WIKI_TAG_COMMENTS),
                            new WikiTag(WIKI_TAG_TAGCLOUD, null, "#type", "", "threshold","0"), 
                            new WikiTag(WIKI_TAG_PROPERTIES, null, "message","","metadata.types","",ATTR_METADATA_INCLUDE_TITLE,"true","separator","html"),
                            new WikiTag(WIKI_TAG_BREADCRUMBS),
                            new WikiTag(WIKI_TAG_TOOLS),
                            new WikiTag(WIKI_TAG_TOOLBAR),
                            new WikiTag(WIKI_TAG_LAYOUT),
                            new WikiTag(WIKI_TAG_MENU),
                            new WikiTag(WIKI_TAG_ENTRYID),
                            new WikiTag(WIKI_TAG_SEARCH,null,
                                        ATTR_TYPE, "", 
                                        "#"+ATTR_FIELDS,"",
                                        ATTR_METADATA,"",
                                        ARG_MAX, "100",
                                        ARG_SEARCH_SHOWFORM, "false",
                                        SpecialSearch.ATTR_TABS,
                                        SpecialSearch.TAB_LIST),
                            new WikiTag(WIKI_TAG_UPLOAD,null, ATTR_TITLE,"Upload file", ATTR_INCLUDEICON,"false"), 
                            new WikiTag(WIKI_TAG_ROOT)),
    };
    //J++



    /** output type */
    public static final OutputType OUTPUT_WIKI = new OutputType("Wiki",
                                                     "wiki.view",
                                                     OutputType.TYPE_VIEW,
                                                     "", ICON_WIKI);



    /** _more_ */
    private String displayImports;


    /** _more_ */
    private Hashtable<String, String> wikiMacros;




    /**
     * ctor
     *
     * @param repository the repository
     */
    public WikiManager(Repository repository) {
        super(repository);
    }


    /**
     * _more_
     */
    @Override
    public void initAttributes() {
        super.initAttributes();
        displayImports = makeDisplayImports();
        wikiMacros     = new Hashtable<String, String>();
        for (String macro :
                StringUtil.split(
                    getRepository().getProperty("ramadda.wiki.macros", ""),
                    ",", true, true)) {
            wikiMacros.put(macro,
                           getRepository().getProperty("ramadda.wiki.macro."
                               + macro, ""));
        }
        WikiUtil.setGlobalProperties(wikiMacros);

    }


    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param props _more_
     * @param prop _more_
     *
     * @return _more_
     */
    public String getProperty(WikiUtil wikiUtil, Hashtable props,
                              String prop) {
        return getProperty(wikiUtil, props, prop, null);
    }

    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param props _more_
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getProperty(WikiUtil wikiUtil, Hashtable props,
                              String prop, String dflt) {
        String value = Utils.getProperty(props, prop, (String) null);
        if (value == null) {
            value = Utils.getProperty(props, prop.toLowerCase(),
                                      (String) null);
        }
        if ((value == null) && (wikiUtil != null)) {
            value = (String) wikiUtil.getWikiProperty(prop);
            if (value == null) {
                value = (String) wikiUtil.getWikiProperty(prop.toLowerCase());
            }
        }
        if (value == null) {
            return dflt;
        }

        return value;
    }

    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param props _more_
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean getProperty(WikiUtil wikiUtil, Hashtable props,
                               String prop, boolean dflt) {
        String value = getProperty(wikiUtil, props, prop, null);
        if (value == null) {
            return dflt;
        }

        return value.equals("true");
    }

    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param props _more_
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public int getProperty(WikiUtil wikiUtil, Hashtable props, String prop,
                           int dflt) {
        String value = getProperty(wikiUtil, props, prop, null);
        if (value == null) {
            return dflt;
        }

        return Integer.parseInt(value);
    }


    /**
     * Get a wiki property value
     *
     * @param wikiUtil The wiki util
     * @param property the property
     * @param tag _more_
     * @param remainder _more_
     * @param notTags _more_
     *
     * @return the value
     */
    public String getWikiPropertyValue(WikiUtil wikiUtil, String property,
                                       String tag, String remainder,
                                       HashSet notTags) {

        try {
            Entry   entry    = (Entry) wikiUtil.getProperty(ATTR_ENTRY);
            Request request  = (Request) wikiUtil.getProperty(ATTR_REQUEST);

            Entry   theEntry = entry;
            if (tag.equals(WIKI_TAG_IMPORT)) {
                //Old style
                if (remainder.indexOf("=") < 0) {
                    List<String> toks = StringUtil.splitUpTo(remainder, " ",
                                            3);
                    if (toks.size() < 2) {
                        return "<b>Incorrect tag specification: {{" + tag
                               + " " + remainder + "}}</b>";
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
            remainder = remainder.replaceAll("\\\\\"", "_XQUOTE_");
            Hashtable tmpProps = HU.parseHtmlProperties(remainder);
            Hashtable props    = new Hashtable();
            for (Enumeration keys =
                    tmpProps.keys(); keys.hasMoreElements(); ) {
                String key = (String) keys.nextElement();
                if (key.startsWith("#")) {
                    continue;
                }
                String value = (String) tmpProps.get(key);
                value = value.replaceAll("_XQUOTE_", "\"");
                //              System.err.println("\tKEY:" + key +"=" +value);
                props.put(key, value);
                if (key instanceof String) {
                    //Don't do this for now
                    //                    String lowerCaseKey = ((String) key).toLowerCase();
                    //                    props.put(lowerCaseKey, value);
                }
            }

            String entryId = getProperty(wikiUtil, props, ATTR_ENTRY, null);
            if (Utils.stringDefined(entryId)) {
                theEntry = findEntryFromId(request, entry, wikiUtil,
                                           entryId.trim());
                if (theEntry == null) {
                    return getMessage(wikiUtil, props,
                                      "Unknown entry:" + entryId);
                }
            }

            String entryRoot = getProperty(wikiUtil, props, "entryRoot",
                                           null);
            if (entryRoot != null) {
                Entry root = findEntryFromId(request, theEntry, wikiUtil,
                                             entryRoot);
                if (root != null) {
                    props.put("entryRoot", root.getId());
                }
            }



            String propertyKey = null;
            //TODO: figure out a way to look for infinte loops
            if (tag.startsWith(TAG_DESCRIPTION)) {
                propertyKey = theEntry.getId() + "_description";
                if (request.getExtraProperty(propertyKey) != null) {
                    return "<b>Detected circular wiki import:" + tag
                           + "<br>For entry:" + theEntry.getId() + "</b>";
                }
                request.putExtraProperty(propertyKey, property);
            }


            List<Metadata> metadataAttrs =
                getMetadataManager().findMetadata(request, entry,
                    "wikiattribute", true);
            if (metadataAttrs != null) {
                for (Metadata metadata : metadataAttrs) {
                    String mName  = metadata.getAttr1();
                    String mValue = metadata.getAttr2();
                    String mTag   = metadata.getAttr3();
                    if ((mTag.length() > 0) && !mTag.equals(tag)) {
                        continue;
                    }
                    if (props.get("override." + mName) == null) {
                        props.put(mName, mValue);
                    }
                }
            }



            for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
                String key = (String) keys.nextElement();
                if (key.startsWith("override.")) {
                    String value = (String) props.get(key);
                    props.remove(key);
                    props.put(key.substring(9), value);
                }
            }

            theEntry.getTypeHandler().addWikiProperties(theEntry, wikiUtil,
                    tag, props);
            addWikiLink(wikiUtil, theEntry);
            String include = handleWikiImport(wikiUtil, request, entry,
                                 theEntry, tag, props);
            try {
                if (include != null) {
                    return include;
                }

                return wikiUtil.getPropertyValue(property);
            } finally {
                if (propertyKey != null) {
                    request.removeExtraProperty(propertyKey);
                }
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }

    }


    /**
     * _more_
     *
     * @param request the request
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

        if (entryId.startsWith("ancestor:")) {
            String type      = entryId.substring("ancestor:".length()).trim();
            Entry  lastEntry = entry;
            Entry  current   = entry;
            while (true) {
                Entry parent = current.getParentEntry();
                if (parent == null) {
                    break;
                }
                if (parent.getTypeHandler().isType(type)) {
                    lastEntry = parent;
                }
                current = parent;
            }

            //      System.err.println("ancestor:" + lastEntry);
            return lastEntry;
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

        //Finally try it as an alias
        if (theEntry == null) {
            theEntry = getEntryManager().getEntryFromAlias(request, entryId);
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


        boolean       inDiv = getProperty(wikiUtil, props, "inDiv", true);
        String        align = getProperty(wikiUtil, props, ATTR_ALIGN, null);
        String        width = getProperty(wikiUtil, props, ATTR_WIDTH, null);
        StringBuilder extra = new StringBuilder();

        //imagewidth says to resize and cache the image on the server
        //If its defined then add it to the URL
        int imageWidth = getProperty(wikiUtil, props, ATTR_IMAGEWIDTH, -1);
        if (imageWidth > 0) {
            url = url + "&" + ARG_IMAGEWIDTH + "=" + imageWidth;
        }
        String id    = getRepository().getGUID().replaceAll("-", "_");
        String js    = getProperty(wikiUtil, props, "jsCall", (String) null);
        String map   = getProperty(wikiUtil, props, "map", (String) null);
        String mapId = getRepository().getGUID().replaceAll("-", "_");
        HU.attr(extra, "id", id);
        if (width != null) {
            HU.attr(extra, HU.ATTR_WIDTH, width);
        }

        if ( !inDiv && (align != null)) {
            //            extra.append(HU.style("align:" + align + ";"));
            //            extra.append(HU.attr("align", align));
        }

        String alt = getProperty(wikiUtil, props, HU.ATTR_ALT, null);
        if (alt == null) {
            alt = getProperty(wikiUtil, props, HU.ATTR_TITLE, "");
        }

        if (wikiUtil != null) {
            String imageClass = (String) wikiUtil.getProperty("image.class");
            if (imageClass != null) {
                HU.cssClass(extra, imageClass);
            } else {
                HU.cssClass(extra, "wiki-image");
            }
        }


        String style  = getProperty(wikiUtil, props, ATTR_STYLE, "");
        int    border = getProperty(wikiUtil, props, ATTR_BORDER, -1);
        String bordercolor = getProperty(wikiUtil, props, ATTR_BORDERCOLOR,
                                         "#000");

        if (border > 0) {
            style += " border: " + border + "px solid " + bordercolor + ";";
        }
        String left = getProperty(wikiUtil, props, "left", null);
        if (left != null) {
            style += " position:absolute; left: " + left + ";";
        }

        String top = getProperty(wikiUtil, props, "top", null);
        if (top != null) {
            style += " position:absolute;  top: " + top + ";";
        }

        if (style.length() > 0) {
            extra.append(" style=\" " + style + "\" ");
        }


        String caption = getProperty(wikiUtil, props, "caption",
                                     (String) null);
        if (map != null) {
            map = map.replaceAll("_newline_", "&#013;");
            extra.append(" usemap='#" + mapId + "' ");
        }
        String  img  = HU.img(url, alt, extra.toString());



        boolean link = getProperty(wikiUtil, props, ATTR_LINK, false);
        String  iurl = getProperty(wikiUtil, props, "url", (String) null);
        boolean linkResource = getProperty(wikiUtil, props,
                                           ATTR_LINKRESOURCE, false);
        boolean popup = getProperty(wikiUtil, props, ATTR_POPUP, false);
        if (iurl != null) {
            img = HU.href(iurl, img);
        } else if (link) {
            img = HU.href(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                           entry), img);
        } else if (linkResource) {
            img = HU.href(entry.getTypeHandler().getEntryResourceUrl(request,
                    entry), img);
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
            addImagePopupJS(request, wikiUtil, buf, props);
            HU.href(buf, hrefUrl, img, HU.cssClass("popup_image"));

            img = buf.toString();
        }

        StringBuilder sb    = new StringBuilder();
        String        attrs = ((align != null)
                               ? HU.style("text-align:" + align + ";")
                               : "") + HU.cssClass("wiki-image");


        if (inDiv) {
            HU.open(sb, HU.TAG_DIV, attrs);
        } else {
            HU.open(sb, HU.TAG_DIV, "style", ((align != null)
                    ? "float:" + align + ";"
                    : "") + " display:inline-block;text-align:center");
        }
        sb.append(img);
        if (map != null) {
            sb.append("\n<map name='" + mapId + "'>" + map + "</map>\n");
            sb.append(
                HU.importJS(
                    getRepository().getHtdocsUrl(
                        "/lib/jquery.maphilight.js")));
            sb.append(HU.script("$('#" + id + "').maphilight();"));
        }


        if (caption != null) {
            sb.append(HU.br());
            HU.span(sb, caption, HU.cssClass("wiki-image-caption"));
        }
        HU.close(sb, HU.TAG_DIV);
        if (js != null) {
            HU.script(sb, "var imageId = '" + id + "';\n" + js);
        }

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
        return getDimension(null, props, attr, dflt);
    }


    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param props _more_
     * @param attr _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public int getDimension(WikiUtil wikiUtil, Hashtable props, String attr,
                            int dflt) {
        try {
            String s = getProperty(wikiUtil, props, attr, (String) null);
            if (s == null) {
                return dflt;
            }
            s = s.trim();
            boolean isPercent = false;
            while ((s.length() > 0) && s.endsWith("%")) {
                isPercent = true;
                s         = s.substring(0, s.length() - 1).trim();
            }
            if (s.length() == 0) {
                return dflt;
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

        String src = getProperty(wikiUtil, props, ATTR_SRC, (String) null);
        Entry  srcEntry = null;
        if (src == null) {
            srcEntry = entry;
        } else {
            src = src.trim();
            if ((src.length() == 0) || entry.getName().equals(src)
                    || src.equals("this")) {
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
            return getMessage(wikiUtil, props,
                              msgLabel("Could not find src") + src);
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

        String src = getProperty(wikiUtil, props, ATTR_SRC, (String) null);
        if ((src == null) || (src.length() == 0)) {
            if ( !entry.isImage()) {
                return getMessage(wikiUtil, props, msg("Not an image"));
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
            return getMessage(wikiUtil, props,
                              msgLabel("Could not find src") + src);
        }
        if (attachment == null) {
            if ( !srcEntry.isImage()) {
                return getMessage(wikiUtil, props, msg("Not an image"));
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

        return getMessage(wikiUtil, props,
                          msgLabel("Could not find image attachment")
                          + attachment);
    }



    /**
     * Get the message
     *
     *
     * @param wikiUtil _more_
     * @param props the properties
     * @param message the default
     *
     * @return  the message or the default
     */
    public String getMessage(WikiUtil wikiUtil, Hashtable props,
                             String message) {
        return getProperty(wikiUtil, props, ATTR_MESSAGE, message);
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

        String attrPrefix = "";
        if (doingApply) {
            attrPrefix = APPLY_PREFIX;
        }

        boolean blockPopup = getProperty(wikiUtil, props,
                                         attrPrefix + ATTR_BLOCK_POPUP,
                                         false);
        boolean blockShow = getProperty(wikiUtil, props,
                                        attrPrefix + ATTR_BLOCK_SHOW, false);
        String prefix = getProperty(wikiUtil, props,
                                    attrPrefix + ATTR_PREFIX, (String) null);
        String suffix = getProperty(wikiUtil, props,
                                    attrPrefix + ATTR_SUFFIX, (String) null);

        String result = getWikiIncludeInner(wikiUtil, request, originalEntry,
                                            entry, tag, props);

        if (result == null) {
            result = getMessage(wikiUtil, props,
                                "Could not process tag: " + tag);
        }
        String destDiv = getProperty(wikiUtil, props, "destDiv", null);
        if (destDiv != null) {
            String id = HU.getUniqueId("block");
            result = HU.div(result, HU.attrs("id", id));
            result += "\n";
            result += HtmlUtils.script("HtmlUtils.swapHtml('#" + id + "','#"
                                       + destDiv + "');");
        }


        if (result.trim().length() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        String rowLabel = getProperty(wikiUtil, props,
                                      attrPrefix + ATTR_ROW_LABEL,
                                      (String) null);
        if (rowLabel != null) {
            return HU.formEntry(rowLabel, result);
        }

        boolean       wrapInADiv = false;
        StringBuilder style      = new StringBuilder();
        int maxHeight = getProperty(wikiUtil, props, "box." + ATTR_MAXHEIGHT,
                                    -1);
        style.append(getProperty(wikiUtil, props, "box." + ATTR_STYLE, ""));
        String cssClass = getProperty(wikiUtil, props, "box." + ATTR_CLASS,
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
            //Convert ant _nl_, _qt_, etc
            prefix = Utils.convertPattern(prefix);
            prefix = wikifyEntry(request, entry, wikiUtil, prefix, false,
                                 null, null, wikiUtil.getNotTags(), true);
            sb.append(prefix);
        }

        if (wrapInADiv) {
            HU.open(sb, HU.TAG_DIV, ((cssClass != null)
                                     ? "class"
                                     : ""), (cssClass != null)
                                            ? cssClass
                                            : "", "style", style.toString());
        }
        sb.append(result);
        if (wrapInADiv) {
            HU.close(sb, HU.TAG_DIV);
        }

        if (suffix != null) {
            makeWikiUtil(request, false).wikify(sb, suffix, null);
        }

        String blockTitle = getProperty(wikiUtil, props,
                                        attrPrefix + ATTR_BLOCK_TITLE, "");


        if (blockPopup) {
	    String contents = HU.div(sb.toString(),"class=wiki-popup-inner");
	    return HU.makePopupLink(null, blockTitle, contents,
						  new NamedValue("decorate",true),
						  new NamedValue("header",getProperty(wikiUtil,props,"block.header",false)),
						  new NamedValue("draggable",getProperty(wikiUtil,props,"block.draggable",false)));
        }

        if (blockShow) {
            boolean blockOpen = getProperty(wikiUtil, props,
                                            attrPrefix + ATTR_BLOCK_OPEN,
                                            true);

            return HU.makeShowHideBlock(
                blockTitle, sb.toString(), blockOpen,
                HU.cssClass("entry-toggleblock-label"), "",
                getIconUrl(ICON_TOGGLEARROWDOWN),
                getIconUrl(ICON_TOGGLEARROWRIGHT));

        }

        return sb.toString();


    }


    /**
     * _more_
     *
     * @param request the request
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processGetWikiToolbar(Request request) throws Exception {
        Entry entry = getEntryManager().getEntry(request,
                          request.getString(ARG_ENTRYID, ""));
        String handlerId = request.getString("handler", "");
        String toolbar   = makeWikiEditBar(request, entry, handlerId);
        toolbar = getPageHandler().translate(request, toolbar);
        Result result = new Result("", new StringBuilder(toolbar));
        result.setShouldDecorate(false);

        return result;
    }





    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processWikiTags(Request request) throws Exception {
        StringBuilder sb   = new StringBuilder();
        List<String>  tags = new ArrayList<String>();
        for (int i = 0; i < WIKITAGS.length; i++) {
            WikiTagCategory cat = WIKITAGS[i];
            for (int tagIdx = 0; tagIdx < cat.tags.length; tagIdx++) {
                WikiTag      tag = cat.tags[tagIdx];
                List<String> tmp = new ArrayList<String>();
                tmp.add(Json.quote("label:" + tag.tag + " properties"));
                for (int j = 0; j < tag.attrsList.size(); j += 2) {
                    tmp.add(Json.quote(tag.attrsList.get(j) + "=\""
                                       + tag.attrsList.get(j + 1) + "\""));
                }
                tags.add(tag.tag);
                tags.add(Json.list(tmp));
            }
        }
        sb.append(Json.map(tags, false));
        Result result = new Result("", sb, Json.MIMETYPE);
        result.setShouldDecorate(false);

        return result;
    }



    /**
     * _more_
     *
     * @param request the request
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processWikify(Request request) throws Exception {
        String wiki = request.getUnsafeString("text", "");
        if (request.defined(ARG_ENTRYID)) {
            if ( !request.get("doImports", true)) {
                request.putExtraProperty("initchart", "added");
                request.putExtraProperty(MapManager.ADDED_IMPORTS, "added");
            }
            Entry entry = getEntryManager().getEntry(request,
                              request.getString(ARG_ENTRYID, ""));
            wiki = wikifyEntry(request, entry, wiki);
        } else {
            wiki = wikify(request, wiki);
        }
        wiki = getPageHandler().translate(request, wiki);
        Result result = new Result("", new StringBuilder(wiki));
        result.setShouldDecorate(false);

        return result;
    }

    /**
     * _more_
     *
     * @param request the request
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processGetNotebook(Request request) throws Exception {
        Entry entry = getEntryManager().getEntry(request,
                          request.getString(ARG_ENTRYID, ""));
        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry,
                "wiki_notebook", false);


        Metadata metadata = null;
        if ((metadataList != null) && (metadataList.size() > 0)) {
            String id = request.getString("notebookId", "default_notebook");
            for (Metadata m : metadataList) {
                if (Misc.equals(m.getAttr1(), id)) {
                    metadata = m;

                    break;
                }
            }
        }

        if (metadata == null) {
            Result result = new Result("", new StringBuilder("{}"),
                                       Json.MIMETYPE);
            result.setShouldDecorate(false);

            return result;
        }

        Result result = new Result(
                            new FileInputStream(
                                getMetadataManager().getFile(
                                    request, entry, metadata,
                                    2)), Json.MIMETYPE);
        result.setShouldDecorate(false);

        return result;
    }


    /**
     * _more_
     *
     * @param request the request
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSaveNotebook(Request request) throws Exception {
        try {
            return processSaveNotebookInner(request);
        } catch (Exception exc) {

            Result result = new Result("",
                                       new StringBuilder("{'error':'"
                                           + exc.getMessage()
                                           + "'}"), Json.MIMETYPE);

            return result;
        }
    }

    /**
     * _more_
     *
     * @param request the request
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSaveNotebookInner(Request request) throws Exception {
        Entry entry = getEntryManager().getEntry(request,
                          request.getString(ARG_ENTRYID, ""));
        if (entry == null) {
            return new Result(
                "", new StringBuilder("{\"error\":\"cannot find entry\"}"),
                Json.MIMETYPE);
        }
        if ( !getAccessManager().canEditEntry(request, entry)) {
            return new Result(
                "", new StringBuilder("{\"error\":\"cannot edit entry\"}"),
                Json.MIMETYPE);
        }

        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry,
                "wiki_notebook", false);

        String notebookId = request.getString("notebookId",
                                "default_notebook");
        Metadata metadata = null;
        if ((metadataList != null) && (metadataList.size() > 0)) {
            if ( !Utils.stringDefined(notebookId)) {
                metadata = metadataList.get(0);
            } else {
                for (Metadata m : metadataList) {
                    if (Misc.equals(m.getAttr1(), notebookId)) {
                        metadata = m;

                        break;
                    }
                }
            }
        }
        String notebook = request.getString("notebook", "");
        if (metadata == null) {
            File f = getStorageManager().getTmpFile(request, "notebook.json");
            IOUtil.writeFile(f, notebook);
            String theFile = getStorageManager().moveToEntryDir(entry,
                                 f).getName();
            getMetadataManager().addMetadata(
                entry,
                new Metadata(
                    getRepository().getGUID(), entry.getId(),
                    "wiki_notebook", false, notebookId, theFile, "", "", ""));
            getEntryManager().updateEntry(null, entry);

            return new Result("", new StringBuilder("{\"result\":\"ok\"}"),
                              Json.MIMETYPE);
        } else {
            File file = getMetadataManager().getFile(request, entry,
                            metadata, 2);
            getStorageManager().writeFile(file, notebook);

            return new Result("", new StringBuilder("{\"result\":\"ok\"}"),
                              Json.MIMETYPE);
        }
    }



    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request the request
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

        boolean wikify  = getProperty(wikiUtil, props, ATTR_WIKIFY, true);
        String criteria = getProperty(wikiUtil, props, ATTR_IF,
                                      (String) null);
        //      System.err.println("tag:"+ theTag);
        if (criteria != null) {}

        StringBuilder sb = new StringBuilder();
        if (theTag.equals(WIKI_TAG_INFORMATION)) {
            Request myRequest = request.cloneMe();
            myRequest.put(ATTR_SHOWTITLE,
                          "" + getProperty(wikiUtil, props, ATTR_SHOWTITLE,
                                           false));
            boolean details = getProperty(wikiUtil, props, ATTR_DETAILS,
                                          false);
            boolean showResource = getProperty(wikiUtil, props,
                                       "showResource", true);
            if ( !details) {
                return entry.getTypeHandler().getEntryContent(myRequest,
                        entry, false, showResource, props).toString();
            }

            return getRepository().getHtmlOutputHandler().getInformationTabs(
                myRequest, entry, false);
        } else if (theTag.equals(WIKI_TAG_CAPTION)
                   || theTag.equals(WIKI_TAG_IMAGE2)) {}
        else if (theTag.equals(WIKI_TAG_TAGCLOUD)) {
            StringBuilder tagCloud = new StringBuilder();
            int threshold = getProperty(wikiUtil, props, "threshold", 0);
            getMetadataManager().doMakeTagCloudOrList(request,
                    getProperty(wikiUtil, props, "type", ""), tagCloud, true,
                    threshold);

            return tagCloud.toString();
        } else if (theTag.equals(WIKI_TAG_COMMENTS)) {
            return getHtmlOutputHandler().getCommentBlock(request, entry,
                    false).toString();
        } else if (theTag.equals(WIKI_TAG_TOOLBAR)) {
            return getPageHandler().getEntryToolbar(request, entry);
        } else if (theTag.equals(WIKI_TAG_PREV)
                   || theTag.equals(WIKI_TAG_NEXT)
                   || theTag.equals(WIKI_TAG_UP)) {
            Entry parent = entry.getParentEntry();
            if (parent == null) {
                return "";
            }
            boolean prev = theTag.equals(WIKI_TAG_PREV);
            boolean next = theTag.equals(WIKI_TAG_NEXT);
            boolean up   = theTag.equals(WIKI_TAG_UP);
            Entry   other;
            if (prev || next) {
                String sort = getProperty(wikiUtil, props, "sort",
                                          "entryorder,name");
                boolean asc = getProperty(wikiUtil, props, "sortAscending",
                                          true);
                boolean tree = getProperty(wikiUtil, props, "tree",
					   false);
		Entry root = null;
		String entryRoot = getProperty(wikiUtil, props, "root",null);
	
		if(entryRoot!=null) {
		    root = findEntryFromId(request, entry, wikiUtil,
                                             entryRoot);
		}
		if(next) {
		    other = getEntryUtil().getNext(request,  entry,  root, tree, sort, asc);
		} else {
		    other = getEntryUtil().getPrev(request,  entry,  root, tree, sort, asc);
		}

            } else {
                other = parent;
            }
            boolean decorate = getProperty(wikiUtil, props, "decorate",
                                           false);
            boolean showName = getProperty(wikiUtil, props, "showName",
                                           decorate);
            String iconSize = HU.makeDim(getProperty(wikiUtil, props,
                                  "iconSize", decorate
                    ? "12"
                    : "32"), "pt");
            String position = getProperty(wikiUtil, props, "position",
                                          (String) null);
            String style = getProperty(wikiUtil, props, "style", "");
            String img;
            String title         = (other != null)
                                   ? other.getName()
                                   : "";
            String extraImgClass = (other != null)
                                   ? ""
                                   : "ramadda-nav-arrow-disabled";
            if (prev) {
                img = HU.fasIconWithAttr("fa-arrow-left",
                                         HU.attrs("class",
                                             "ramadda-nav-arrow "
                                             + extraImgClass, "title", title,
                                                 "style",
                                                     "font-size:"
                                                         + iconSize) + ";");
            } else if (next) {
                img = HU.fasIconWithAttr("fa-arrow-right",
                                         HU.attrs("class",
                                             "ramadda-nav-arrow "
                                             + extraImgClass, "title", title,
                                                 "style",
                                                     "font-size:" + iconSize
                                                         + ";"));
            } else {
                img = HU.fasIconWithAttr("fa-arrow-up",
                                         HU.attrs("class",
                                             "ramadda-nav-arrow "
                                             + extraImgClass, "title", title,
                                                 "style",
                                                     "font-size:" + iconSize
                                                         + ";"));
            }
            String extraClass = "";
            if (position != null) {
                if (position.equals("fixed")) {
                    extraClass = prev
                                 ? " ramadda-nav-page-fixed-prev "
                                 : " ramadda-nav-page-fixed-next ";
                }
            }
            String result;
            if (other == null) {
                return HU.div(img,
                              HU.attrs("style", style, "class",
                                       " ramadda-nav-page " + extraClass));
            } else {
                String label = "";
                if (showName) {
                    label = HU.div(other.getName(),
                                   " class=ramadda-nav-page-label");
                }
                if (prev) {
                    result = img + label;
                } else {
                    result = label + img;
                }
            }
            String clazz = " ramadda-nav-page ";
            if (decorate) {
                clazz += " ramadda-nav-page-decorated ";
            }
            result = HU.div(result,
                            HU.attrs("style", style, "class",
                                     clazz + extraClass));

            return HU.href(getEntryManager().getEntryUrl(request, other),
                           result);
        } else if (theTag.equals(WIKI_TAG_BREADCRUMBS)) {
            List<Entry> parents = getEntryManager().getParents(request,
                                      entry);
            List<String> breadcrumbs =
                getPageHandler().makeBreadcrumbList(request, parents, null);

            getPageHandler().makeBreadcrumbs(request, breadcrumbs, sb);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_LABEL)) {
            String text = getProperty(wikiUtil, props, ATTR_TEXT, "");
            String id   = getProperty(wikiUtil, props, ATTR_ID,
                                      (String) null);
            if (id != null) {
                text = getWikiMetadataLabel(request, entry, id, text);
            }
            if (wikify) {
                text = wikifyEntry(request, entry, text, false, null, null,
                                   wikiUtil.getNotTags());

            }

            return text;
        } else if (theTag.equals(WIKI_TAG_LINK)) {
            boolean linkResource = getProperty(wikiUtil, props,
                                       ATTR_LINKRESOURCE, false);
            String name  = getEntryDisplayName(entry);
            String title = getProperty(wikiUtil, props, ATTR_TITLE, name);
            title = title.replace("${name}", name);
            String url;
            if (linkResource
                    && (entry.getTypeHandler().isType("link")
                        || entry.isFile() || entry.getResource().isUrl())) {
                url = entry.getTypeHandler().getEntryResourceUrl(request,
                        entry);
            } else {
                String output =
                    getProperty(wikiUtil, props, ATTR_OUTPUT,
                                OutputHandler.OUTPUT_HTML.getId());
                url = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
                                       ARG_OUTPUT, output);
            }


            if (getProperty(wikiUtil, props, "button", false)) {
                return HU.href(
                    url, title,
                    HU.cssClass("ramadda-button ramadda-button-blue")
                    + HU.attr("role", "button"));
            } else {
                return HU.href(url, title);
            }
        } else if (theTag.equals(WIKI_TAG_RESOURCE)) {
            String url = null;
            String label;
            if ( !entry.getResource().isDefined()) {
                url   = entry.getTypeHandler().getPathForEntry(request,
                        entry);
                label = getProperty(wikiUtil, props, ATTR_TITLE, url);
            } else if (entry.getResource().isFile()) {
                url = entry.getTypeHandler().getEntryResourceUrl(request,
                        entry);
                label = getProperty(wikiUtil, props, ATTR_TITLE, "Download");
            } else {
                url   = entry.getResource().getPath();
                label = getProperty(wikiUtil, props, ATTR_TITLE, url);
            }
            if (getProperty(wikiUtil, props, "url", false)) {
                return url;
            }
            if ( !Utils.stringDefined(url)) {
                String message = getProperty(wikiUtil, props, ATTR_MESSAGE,
                                             (String) null);
                if (message != null) {
                    return message;
                }

                return "";
            }


            boolean simple = getProperty(wikiUtil, props, "simple", false);

            boolean includeIcon = getProperty(wikiUtil, props,
                                      ATTR_INCLUDEICON, false);
            if (includeIcon) {
                label = HU.img(getIconUrl("/icons/download.png"))
                        + HU.space(2) + label;
            }

            if ( !simple) {
                return HU.div(HU.href(url, label,
                                      HU.cssClass("ramadda-button")
                                      + HU.attr("role", "button")));
            }
            String extra = "";

            return HU.href(url, label, extra);
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
                getRepository().getTypeHandler(getProperty(wikiUtil, props,
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
            if (getProperty(wikiUtil, props, ATTR_INCLUDEICON, false)) {
                String icon = typeHandler.getIconProperty(null);
                if (icon == null) {
                    icon = ICON_BLANK;
                    img = HU.img(typeHandler.getIconUrl(icon), "",
                                 HU.attr(HU.ATTR_WIDTH, "16"));
                } else {
                    img = HU.img(typeHandler.getIconUrl(icon));
                }
            }

            String label = getProperty(wikiUtil, props, ATTR_TITLE,
                                       typeHandler.getLabel());

            return HU
                .href(request
                    .makeUrl(getRepository().URL_ENTRY_FORM, ARG_GROUP,
                             group.getId(), EntryManager.ARG_TYPE,
                             typeHandler.getType()), img + " " + msg(label));

        } else if (theTag.equals(WIKI_TAG_DESCRIPTION)) {
            String prefix = getProperty(wikiUtil, props, "prefix",
                                        (String) null);
            String suffix = getProperty(wikiUtil, props, "suffix",
                                        (String) null);
            String desc = entry.getTypeHandler().getEntryText(entry);
            desc = desc.trim();
            //            desc = desc.replaceAll("\r\n\r\n", "\n<p>\n"); 
            if (desc.length() > 0) {
                if (prefix != null) {
                    desc = prefix + "\n" + desc;
                }
                if (suffix != null) {
                    desc += "\n" + suffix;
                }
            }
            if (wikify) {
                //Pass in the wikiUtil so any state (e.g., headings) gets passed up
                desc = wikifyEntry(request, entry, wikiUtil, desc, false,
                                   null, null, wikiUtil.getNotTags(), true);
                //                desc = makeWikiUtil(request, false).wikify(desc, null);
            }
            if (getProperty(wikiUtil, props, "convert_newline", false)) {
                desc = desc.replaceAll("\n", "<p>");
            }

            return desc;
        } else if (theTag.equals(WIKI_TAG_LAYOUT)) {
            return getHtmlOutputHandler().makeHtmlHeader(request, entry,
                    getProperty(wikiUtil, props, ATTR_TITLE, "Layout"));
        } else if (theTag.equals(WIKI_TAG_NAME)) {
            String name = getEntryDisplayName(entry);
            if (getProperty(wikiUtil, props, "link", false)) {
                String url = getEntryManager().getEntryUrl(request, entry);
                name = HU.href(url, name, HU.cssClass("ramadda-link"));
            }

            return name;
        } else if (theTag.equals(WIKI_TAG_EMBED)) {
            if ( !entry.isFile()
                    || ( !isTextFile(entry.getResource().getPath())
                         && !getProperty(wikiUtil, props, ATTR_FORCE,
                                         false))) {
                return "Entry isn't a text file";
            }
            StringBuilder txt = new StringBuilder("");
            InputStream fis = getStorageManager().getFileInputStream(
                                  entry.getResource().getPath());
            BufferedReader br =
                new BufferedReader(new InputStreamReader(fis));
            boolean raw = getProperty(wikiUtil, props, "raw",   false);
            boolean embedWikify = getProperty(wikiUtil, props, "wikify", false);
            int skipLines = getProperty(wikiUtil, props, ATTR_SKIP_LINES, 0);
            int maxLines = getProperty(wikiUtil, props, ATTR_MAX_LINES, 1000);
            int maxHeight = getProperty(wikiUtil, props, ATTR_MAXHEIGHT, raw?-1:300);

            boolean annotate = getProperty(wikiUtil, props, ATTR_ANNOTATE, false);
            String  as = getProperty(wikiUtil, props, "as",null);

            int    lineNumber = 0;
            int    cnt        = 0;
            String line;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (skipLines > 0) {
                    skipLines--;
                    continue;
                }
                cnt++;
		if(!embedWikify && !raw) {
		    line = line.replaceAll("<", "&lt;");
		    line = line.replaceAll(">", "&gt;");
		    if (annotate) {
			txt.append("#");
			txt.append(lineNumber);
			txt.append(": ");
		    }
		}
                txt.append(line);
                txt.append("\n");
                if (cnt > maxLines) {
                    break;
                }
            }
            IOUtil.close(fis);
	    if(as!=null) {
		boolean doFile = false;
		if(as.equals("file")) {
		    doFile = true;
		    String ext = IOUtil.getFileExtension(entry.getResource().getPath()).toLowerCase();
		    ext = ext.replace(".","");
		    as = ext;
		}
		if(as.equals("json") || as.equals("geojson")) {
		    return  embedJson(request, txt.toString());
		} else {
		    StringBuilder tmp = new StringBuilder();
		    WikiUtil.Chunk chunk = new WikiUtil.Chunk(as,txt);
		    if(wikiUtil.handleCode(tmp,  chunk, this, doFile)) {
			String s =  tmp.toString();
			if(maxHeight>0) {
			    return HU.div(s,
					  HU.style("max-height:" + maxHeight
						   + "px; overflow-y:auto;"));
			}
			return s;
		    }
		}
		return "Unknown type:" + as;
				    
	    }
	    if(embedWikify) {
		return  wikifyEntry(request, entry, wikiUtil, txt.toString(),
                                             false, null, null, null, false);
	    }
	    if(maxHeight>0) {
		return HU.pre(txt.toString(),
			      HU.style("max-height:" + maxHeight
				       + "px; overflow-y:auto;"));
	    } else if(!raw) {
		return HU.pre(txt.toString());
	    } else {
		return txt.toString();
	    }
        } else if (theTag.equals(WIKI_TAG_FIELD)) {
            String name = getProperty(wikiUtil, props, ATTR_FIELDNAME,
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

            return getDateHandler().formatDate(entry, date,
                    getProperty(wikiUtil, props, ATTR_FORMAT, null));
        } else if (theTag.equals(WIKI_TAG_DATERANGE)) {
            String format = getProperty(wikiUtil, props, ATTR_FORMAT,
                                        (String) null);
            Date date1 = new Date(entry.getStartDate());
            Date date2 = new Date(entry.getEndDate());
            SimpleDateFormat dateFormat =
                getDateHandler().getDateFormat(entry, format);
            String separator = getProperty(wikiUtil, props, ATTR_SEPARATOR,
                                           " -- ");

            return dateFormat.format(date1) + separator
                   + dateFormat.format(date2);
        } else if (theTag.equals(WIKI_TAG_ENTRYID)) {
            return entry.getId();
        } else if (theTag.equals(WIKI_TAG_PROPERTY)) {
            String name  = (String) props.get("name");
            String value = (String) props.get("value");
            if (name != null) {
                if (value != null) {
                    wikiUtil.putWikiProperty(name, value);
                } else {
                    wikiUtil.removeWikiProperty(name);
                }
            }

            return "";
        } else if (theTag.equals(WIKI_TAG_ATTRS)) {
            for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
                String key   = (String) keys.nextElement();
                String value = (String) props.get(key);
                if (key.equals("clearAttributes")) {
                    wikiUtil.clearWikiAttributes();

                    continue;
                }
                wikiUtil.putWikiAttribute(key, value);
            }

            return "";
        } else if (theTag.equals(WIKI_TAG_DISPLAYPROPERTIES)) {
            for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
                String key   = (String) keys.nextElement();
                String value = (String) props.get(key);
                wikiUtil.appendJavascript("addGlobalDisplayProperty('" + key
                                          + "','" + value + "');\n");

            }

            return "";

        } else if (theTag.equals(WIKI_TAG_DISPLAYPROPERTY)) {
            String name  = (String) props.get("name");
            String value = (String) props.get("value");
            if ((name != null) && (value != null)) {
                wikiUtil.appendJavascript("addGlobalDisplayProperty('" + name
                                          + "','" + value + "');\n");

                return "";
            }

            return "";
        } else if (theTag.equals(WIKI_TAG_PROPERTIES)) {
            return makeEntryTabs(request, wikiUtil, entry, props);
        } else if (theTag.equals(WIKI_TAG_STREETVIEW)) {
            ImageOutputHandler ioh =
                (ImageOutputHandler) getRepository().getOutputHandler(
                    ImageOutputHandler.OUTPUT_PLAYER);
            if ( !ioh.isStreetviewEnabled()) {
                String message = getProperty(wikiUtil, props, ATTR_MESSAGE,
                                             "Streetview not enabled");

                return message;
            }
            String width   = getProperty(wikiUtil, props, ATTR_WIDTH, "100%");
            String caption = getProperty(wikiUtil, props, ATTR_CAPTION);
            String heading = getProperty(wikiUtil, props, "heading");
            String imageUrl =
                request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
                                 ARG_OUTPUT,
                                 ioh.OUTPUT_STREETVIEW.toString());
            if (heading != null) {
                imageUrl += "&heading=" + heading;
            }
            String img = HU.img(imageUrl, "", HU.attr("width", width));
            if (caption != null) {
                img += HU.div(caption, HU.cssClass("image-caption"));
            }

            return img;

        } else if (theTag.equals(WIKI_TAG_CARD)) {
            return makeCard(request, wikiUtil, props, entry);
        } else if (theTag.equals(WIKI_TAG_IMAGE)) {
            return getWikiImage(wikiUtil, request, entry, props);
        } else if (theTag.equals(WIKI_TAG_URL)) {
            return getWikiUrl(wikiUtil, request, entry, props);
        } else if (theTag.equals(WIKI_TAG_HTML)) {
            Request newRequest = makeRequest(request, props);
            if (getProperty(wikiUtil, props, ATTR_CHILDREN, false)) {
                List<Entry> children = getEntries(request, wikiUtil,
                                           originalEntry, entry, props);
                for (Entry child : children) {
                    Result result =
                        getHtmlOutputHandler().getHtmlResult(request,
                            OutputHandler.OUTPUT_HTML, child);
                    sb.append(getEntryManager().getEntryLink(request, child,""));
                    sb.append(HU.br());
                    sb.append(new String(result.getContent()));
                    sb.append(HU.p());
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
            List<Entry> children = getEntries(request, wikiUtil,
                                       originalEntry, entry, props);
            boolean doDay = getProperty(wikiUtil, props, ATTR_DAY, false)
                            || request.defined(ARG_DAY);
            getCalendarOutputHandler().outputCalendar(request,
                    getCalendarOutputHandler().makeCalendarEntries(request,
                        children), sb, doDay);

            return sb.toString();


        } else if (theTag.equals(WIKI_TAG_DISPLAY)
                   || theTag.startsWith("display_")
                   || theTag.equals(WIKI_TAG_CHART)) {
            String jsonUrl = null;
            boolean doEntries = getProperty(wikiUtil, props, "doEntries",
                                            false);
            boolean doEntry = getProperty(wikiUtil, props, "doEntry", false);
            if (doEntries || doEntry) {
                jsonUrl = request.entryUrl(
                    getRepository().URL_ENTRY_SHOW, entry, ARG_OUTPUT,
                    JsonOutputHandler.OUTPUT_JSON_POINT.getId());
                if (doEntry) {
                    jsonUrl += "&onlyentry=true";
                }
                if (getProperty(wikiUtil, props, "addAttributes", false)) {
                    jsonUrl += "&addAttributes=true";
                }
                if (getProperty(wikiUtil, props, "addThumbnails", false)) {
                    jsonUrl += "&addThumbnails=true";
                }
                if (getProperty(wikiUtil, props, "addImages", false)) {
                    jsonUrl += "&addImages=true";
                }
                if (getProperty(wikiUtil, props, "addSnippets", false)) {
                    jsonUrl += "&addSnippets=true";
                }
            }
            if (theTag.startsWith("display_")) {
                String newType = theTag.substring(8);
                props.put(ATTR_TYPE, newType);
                theTag = theTag.substring(0, 7);
            }
            List<String> displayProps = new ArrayList<String>();
            if (jsonUrl == null) {
                if (props.get("max") == null) {
                    String max = getProperty(wikiUtil, props, "max", null);
                    if (max != null) {
                        props.put("max", max);
                    }
                }
                jsonUrl = entry.getTypeHandler().getUrlForWiki(request,
                        entry, theTag, props, displayProps);
            }
            //      System.err.println("jsonurl:" +jsonUrl);
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
                    jsonUrl = gridType.getUrlForWiki(request, entry, theTag,
                            props, displayProps);
                }
            }
            getEntryDisplay(request, wikiUtil, entry, originalEntry, theTag,
                            entry.getName(), jsonUrl, sb, props,
                            displayProps);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_GROUP)
                   || theTag.equals(WIKI_TAG_GROUP_OLD)) {
            getEntryDisplay(request, wikiUtil, entry, originalEntry, theTag,
                            entry.getName(), null, sb, props, null);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_TIMELINE)) {
            Entry mainEntry = entry;
            List<Entry> children = getEntries(request, wikiUtil,
                                       originalEntry, mainEntry, props);
            int    height = getProperty(wikiUtil, props, ATTR_HEIGHT, 150);
            String style  = Utils.concatString("height: ", height, "px;");
            getCalendarOutputHandler().makeTimeline(request, mainEntry,
                    children, sb, style, props);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_EARTH)) {
            return getPageHandler().showDialogWarning(
                "Google earth view is no longer available");
        } else if (theTag.equals(WIKI_TAG_MAP)
                   || theTag.equals(WIKI_TAG_MAPENTRY)) {
            handleMapTag(request, wikiUtil, entry, originalEntry, theTag,
                         props, sb);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_TOOLS)) {
            StringBuilder links = new StringBuilder();
            int           cnt   = 0;
            for (Link link :
                    getEntryManager().getEntryLinks(request, entry)) {
                if ( !link.isType(OutputType.TYPE_IMPORTANT)) {
                    continue;
                }

                String label = getIconImage(link.getIcon()) + HU.space(1)
                               + link.getLabel();
                HU.href(links, link.getUrl(), label);
                links.append(HU.br());
                cnt++;
            }
            if (cnt == 0) {
                return "";
            }
            String title = getProperty(wikiUtil, props, ATTR_TITLE,
                                       "Services");
            HU.div(sb, title, HU.cssClass("wiki-h4"));
            HU.div(sb, links.toString(), HU.cssClass("entry-tools-box"));

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_MENU)) {
            String menus = getProperty(wikiUtil, props, ATTR_MENUS, "");
            int type = OutputType.getTypeMask(StringUtil.split(menus, ",",
                           true, true));

            String links = getEntryManager().getEntryActionsTable(request,
                               entry, type);
            if (getProperty(wikiUtil, props, "popup", true)) {
                StringBuilder popup  = new StringBuilder();
                if (getProperty(wikiUtil, props, "breadcrumbs", true)) {
                    StringBuilder tmp = new StringBuilder();
                    List<Entry> parents =
                        getEntryManager().getParents(request, entry);
                    List<String> breadcrumbs =
                        getPageHandler().makeBreadcrumbList(request, parents,
                            null);

                    getPageHandler().makeBreadcrumbs(request, breadcrumbs,
                            tmp);
                    links = tmp.toString() + links;
                }
		links =  HU.div(links,"class=wiki-popup-inner");
                String menuLinkImg = HU.img(getRepository().getIconUrl(
                                         "/icons/menu_arrow.gif"), msg(
                                         "Click to show menu"), HU.cssClass(
                                         "ramadda-breadcrumbs-menu-img"));
		String menuLink = HU.makePopupLink(popup, menuLinkImg, links);
                popup.append(menuLink);
                return popup.toString();
            }

            return links;
        } else if (theTag.equals(WIKI_TAG_MULTI)) {
            //      wikiUtil = initWikiUtil(request, new WikiUtil(wikiUtil), entry);
            Hashtable props2        = new Hashtable();
            Hashtable firstProps    = new Hashtable();
            Hashtable lastProps     = new Hashtable();
            Hashtable notLastProps  = new Hashtable();
            Hashtable notFirstProps = new Hashtable();
            Hashtable<String, List<String>> multiAttrs =
                new Hashtable<String, List<String>>();
            StringBuilder buff           = new StringBuilder("");
            String        tag            = null;
            int           max            = 0;
            String        template       = null;
            int           columns        = -1;
            List<String>  headers        = null;
            String        headerProp     = null;
            String        footerProp     = null;
            String        headerTemplate = null;
            List<Entry>   entries        = null;
            for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
                String key   = (String) keys.nextElement();
                String value = (String) props.get(key);
                if (key.equals("_tag")) {
                    tag = value;
                } else if (key.equals("_template")) {
                    template = value;
                } else if (key.equals("_entries")) {
                    entries = getEntries(request, wikiUtil, entry, value,
                                         props);
                    max = Math.max(max, entries.size());
                } else if (key.equals("_headers")) {
                    headers = StringUtil.split(value, ",");
                } else if (key.equals("_headerTemplate")) {
                    headerTemplate = value;
                } else if (key.equals("_header")) {
                    headerProp = value;
                } else if (key.equals("_footer")) {
                    footerProp = value;
                } else if (key.equals("_columns")) {
                    columns = Integer.parseInt(value);
                } else if (key.startsWith("_")) {
                    key = key.substring(1);
                    List<String> toks = StringUtil.split(value, ",");
                    max = Math.max(max, toks.size());
                    multiAttrs.put(key, toks);
                } else {
                    if (key.startsWith("first.")) {
                        firstProps.put(key.substring("first.".length()),
                                       value);
                    } else if (key.startsWith("last.")) {
                        lastProps.put(key.substring("last.".length()), value);
                    } else if (key.startsWith("notlast.")) {
                        notLastProps.put(key.substring("notlast.".length()),
                                         value);
                    } else if (key.startsWith("notfirst.")) {
                        notFirstProps.put(
                            key.substring("notfirst.".length()), value);
                    } else {
			props2.put(key, value);
		    }
                }
            }

            if (headerProp == null) {
                headerProp = getProperty(wikiUtil, props, "_header",
                                         (String) null);
            }
            if (footerProp == null) {
                footerProp = getProperty(wikiUtil, props, "_footer",
                                         (String) null);
            }
            if ((tag == null) && (template == null)) {
                template = getProperty(wikiUtil, props, "_template",
                                       (String) null);
            }


            if ((tag == null) && (template == null)) {
                return getPageHandler().showDialogError(
                    "No _tag or _template attribute specified");
            }
            int multiCount = getProperty(wikiUtil, props, "multiCount", -1);
            if (multiCount > 0) {
                max = multiCount;
            }

            if (template != null) {
                template = template.replaceAll("\\\\\\{",
                        "{").replaceAll("\\\\\\}", "}");
            }
            if (headerProp != null) {
                buff.append(headerProp);
            }
            if (columns > 0) {
                buff.append("<table width=100%><tr valign=top>\n");
            }
            int    colCnt   = 0;
            String colWidth = null;
            if (columns > 0) {
                colWidth = ((int) 100 / columns) + "%";
            }
            for (int i = 0; i < max; i++) {
		boolean debug = false;
                Hashtable _props = new Hashtable();
                _props.put("displayIndex", "" + i);
                _props.putAll(props2);
                if (i==0) {
                    _props.putAll(firstProps);
                } else {
                    _props.putAll(notFirstProps);
                }
                if (i == max - 1) {
                    _props.putAll(lastProps);
                } else {
                    _props.putAll(notLastProps);
                }
		if(debug) 
		    System.err.println("p4:" + _props.get("chartHeight"));
                String s = template;

                for (Enumeration keys = multiAttrs.keys();
                        keys.hasMoreElements(); ) {
                    String       key    = (String) keys.nextElement();
                    List<String> values = multiAttrs.get(key);
                    if (i < values.size()) {
                        String value = values.get(i);
                        value = value.replaceAll("_comma_", ",");
                        _props.put(key, value);
                        if (s != null) {
                            //                      System.err.println("\t" +key +"=" + value);
                            s = s.replaceAll(
                                "\\$\\{" + key + "\\}", value).replaceAll(
                                "\\$\\{" + "multiIndex" + "\\}",
                                (i + 1) + "");
                        }
                    }
                }
		if(debug) 
		    System.err.println("p5:" + _props.get("chartHeight"));
                if (columns > 0) {
                    colCnt++;
                    if (colCnt > columns) {
                        buff.append("</tr><tr valign=top>");
                        colCnt = 1;
                    }
                    buff.append("<td width='" + colWidth + "'>");
                }

                String header = ((headers != null) && (i < headers.size()))
                                ? headers.get(i)
                                : null;
                if (header != null) {
                    if (headerTemplate != null) {
                        header = headerTemplate.replace("${header}", header);
                    }
                    buff.append(header);
                }
                Entry theEntry = entry;
                if ((entries != null) && (i < entries.size())) {
                    theEntry = entries.get(i);
                }
                if (s != null) {
                    s = s.replaceAll("_dollar_", "\\$");
                    //              System.err.println("WIKIFY:" + tmp.trim());
                    String tmp = wikifyEntry(request, theEntry, wikiUtil, s,
                                             false, null, null, null, false);
                    buff.append(tmp);
                } else {
		    if(debug) {
			System.err.println("p6:" + _props.get("chartHeight"));
			System.err.println("p7:" + firstProps.get("chartHeight"));
		    }
                    buff.append(getWikiIncludeInner(wikiUtil, request,
						    originalEntry, theEntry, tag, _props));
                }
                if (columns > 0) {
                    buff.append("</td>");
                }
            }
            if (columns > 0) {
                buff.append("</tr></table>\n");
            }

            if (footerProp != null) {
                buff.append(footerProp);
            }

            return buff.toString();
        } else if (theTag.equals(WIKI_TAG_APPLY)) {
            StringBuilder style = new StringBuilder(getProperty(wikiUtil,
                                      props, APPLY_PREFIX + ATTR_STYLE, ""));
            int padding = getProperty(wikiUtil, props,
                                      APPLY_PREFIX + ATTR_PADDING, 5);
            int margin = getProperty(wikiUtil, props,
                                     APPLY_PREFIX + ATTR_MARGIN, 5);
            int border = getProperty(wikiUtil, props,
                                     APPLY_PREFIX + ATTR_BORDER, -1);
            String bordercolor = getProperty(wikiUtil, props,
                                             APPLY_PREFIX + ATTR_BORDERCOLOR,
                                             "#000");

            if (border > 0) {
                Utils.append(style, " border: ", border, "px solid ",
                             bordercolor, "; ");
            }

            if (padding > 0) {
                Utils.append(style, " padding: ", padding, "px; ");
            }

            if (margin > 0) {
                Utils.append(style, " margin: ", margin, "px; ");
            }


            int maxHeight = getProperty(wikiUtil, props,
                                        APPLY_PREFIX + "maxheight", -1);
            if (maxHeight > 0) {
                Utils.append(style, " max-height: ", maxHeight,
                             "px;  overflow-y: auto; ");
            }

            int minHeight = getProperty(wikiUtil, props,
                                        APPLY_PREFIX + "minheight", -1);
            if (minHeight > 0) {
                Utils.append(style, " min-height: ", minHeight, "px; ");
            }


            Hashtable tmpProps = new Hashtable(props);
            tmpProps.remove(ATTR_ENTRY);
            Request newRequest = makeRequest(request, props);
            //            System.err.println("cloned:" + newRequest);
            //            {{apply tag="tree" apply.layout="grid" apply.columns="2"}}
            String tag = getProperty(wikiUtil, props, ATTR_APPLY_TAG, "html");
            String prefixTemplate = getProperty(wikiUtil, props,
                                        APPLY_PREFIX + "header", "");
            String suffixTemplate = getProperty(wikiUtil, props,
                                        APPLY_PREFIX + "footer", "");

            List<Entry> children = getEntries(request, wikiUtil,
                                       originalEntry, entry, props, false,
                                       APPLY_PREFIX);
            if (children.size() == 0) {
                return null;
            }
            String layout = getProperty(wikiUtil, props,
                                        APPLY_PREFIX + "layout", "table");
            int columns = getProperty(wikiUtil, props,
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


            boolean includeIcon = getProperty(wikiUtil, props,
                                      APPLY_PREFIX + ATTR_INCLUDEICON, false);
            String divClass = getProperty(wikiUtil, props,
                                          APPLY_PREFIX + "divclass", "");


            boolean includeLinkAfter = false;

            if (layout.equals("accordian") || layout.equals("accordion")) {
                includeLinkAfter = true;
            }

            String divExtra = Utils.concatString(HU.cssClass(divClass),
                                  HU.style(style.toString()));
            int colCnt = 0;

            for (Entry child : children) {
                String childsHtml = my_getWikiInclude(wikiUtil, newRequest,
                                        originalEntry, child, tag, tmpProps,
                                        true);

                String prefix   = prefixTemplate;
                String suffix   = suffixTemplate;
                String urlLabel = getEntryDisplayName(child);
                if (includeIcon) {
                    urlLabel = HU.img(getPageHandler().getIconUrl(request,
                            child)) + " " + urlLabel;
                }

                String childUrl =
                    HU.href(request.entryUrl(getRepository().URL_ENTRY_SHOW,
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
                String icon = HU.img(getPageHandler().getIconUrl(request,
                                  child));
                prefix = prefix.replace("${icon}", icon);
                suffix = suffix.replace("${icon}", icon);

                StringBuilder content = new StringBuilder();
                content.append(prefix);
                HU.open(content, HU.TAG_DIV, divExtra);
                content.append(getSnippet(request, child, true));
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
                    String title = getEntryDisplayName(child);
                    contents.add(title);
                    if (includeIcon) {
                        title = HU.img(getPageHandler().getIconUrl(request,
                                child)) + " " + title;
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
            } else if (layout.equals("accordian")
                       || layout.equals("accordion")) {
                int showBorder = getProperty(wikiUtil, props, "border", 0);
                boolean collapse = getProperty(wikiUtil, props, "collapse",
                                       false);
                HU.makeAccordion(sb, titles, contents, collapse,
                                 ((showBorder == 0)
                                  ? "ramadda-accordion"
                                  : null), null);
            } else {
                throw new IllegalArgumentException("Unknown layout:"
                        + layout);
            }

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_SIMPLE)) {
            return makeSimpleDisplay(request, wikiUtil, props, originalEntry,
                                     entry);
        } else if (theTag.equals(WIKI_TAG_TABS)
                   || theTag.equals(WIKI_TAG_ACCORDION)
                   || theTag.equals(WIKI_TAG_ACCORDIAN)
                   || theTag.equals(WIKI_TAG_SLIDESHOW)
                   || theTag.equals(WIKI_TAG_BOOTSTRAP)
                   || theTag.equals(WIKI_TAG_GRID)) {
            List<Entry> children = getEntries(request, wikiUtil,
                                       originalEntry, entry, props);
            String message = getProperty(wikiUtil, props, ATTR_MESSAGE,
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

            if (getProperty(wikiUtil, props, ATTR_USEDESCRIPTION) != null) {
                boolean useDescription = getProperty(wikiUtil, props,
                                             ATTR_USEDESCRIPTION, true);

                if (useDescription) {
                    dfltTag = WIKI_TAG_SIMPLE;
                } else {
                    dfltTag = WIKI_TAG_HTML;
                }
            }


            boolean includeIcon = getProperty(wikiUtil, props,
                                      ATTR_INCLUDEICON, false);
            if (doingGrid) {
                includeIcon = false;
                if (props.get("showLink") == null) {
                    props.put("showLink", "false");
                }
            }

            boolean showHeading = getProperty(wikiUtil, props, "showHeading",
                                      true);
            boolean headingSmall = getProperty(wikiUtil, props,
                                       "headingSmall", true);
            String headingClass = headingSmall
                                  ? HU.cssClass(
                                      "ramadda-subheading ramadda-subheading-small")
                                  : HU.cssClass("ramadda-subheading");

            boolean showLink = getProperty(wikiUtil, props, ATTR_SHOWLINK,
                                           true);

            boolean includeUrl = getProperty(wikiUtil, props, "includeurl",
                                             false);

            boolean useCookies = getProperty(wikiUtil, props, "cookie",
                                             false);

            String linklabel = getProperty(wikiUtil, props, ATTR_LINKLABEL,
                                           "");

            String width  = getProperty(wikiUtil, props, ATTR_WIDTH, "400");
            int    height = getProperty(wikiUtil, props, ATTR_HEIGHT, 270);

            if (doingSlideshow) {
                props.put(ATTR_WIDTH, width);
                props.put(ATTR_HEIGHT, "" + height);
                props.put(ATTR_CONSTRAINSIZE, "true");
            }

            if (theTag.equals(WIKI_TAG_TABS)) {
                dfltTag = WIKI_TAG_HTML;
            }


            boolean linkResource = getProperty(wikiUtil, props,
                                       ATTR_LINKRESOURCE, false);


            String    tag = getProperty(wikiUtil, props, ATTR_TAG, dfltTag);
            Request   newRequest = makeRequest(request, props);
            Hashtable tmpProps   = new Hashtable(props);
            tmpProps.remove(ATTR_ENTRY);
            tmpProps.remove(ATTR_ENTRIES);
            tmpProps.remove(ATTR_FIRST);
            if (doingGrid) {
                tmpProps.put("showHeading", "false");
                if (tmpProps.get("includeIcon") == null) {
                    tmpProps.put("includeIcon", "true");
                }
            }
            if (children.size() > 0) {
                checkHeading(request, wikiUtil, props, sb);
            }

            for (Entry child : children) {
                String title = getEntryDisplayName(child);
                if (includeIcon) {
                    title = HU.img(getPageHandler().getIconUrl(request,
                            child)) + " " + title;
                }
                titles.add(title);
                //                urls.add(request.entryUrl(getRepository().URL_ENTRY_SHOW, child));
                urls.add(getEntryManager().getEntryUrl(request, child));

                tmpProps.put("defaultToCard", "true");
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
                        url = getEntryManager().getEntryUrl(request, child);
                        //                        url = request.entryUrl(getRepository().URL_ENTRY_SHOW, child);
                    }
                    String href = HU.href(url, linklabel.isEmpty()
                            ? getEntryDisplayName(child)
                            : linklabel);

                    //                    content.append(HU.br());
                    content.append(HU.leftRight("", href));
                }
                contents.add(content.toString());
            }



            if (theTag.equals(WIKI_TAG_ACCORDIAN)
                    || theTag.equals(WIKI_TAG_ACCORDION)) {
                int border = getProperty(wikiUtil, props, ATTR_BORDER, 0);
                boolean collapse = getProperty(wikiUtil, props,
                                       ATTR_COLLAPSE, false);
                HU.makeAccordion(sb, titles, contents, collapse,
                                 ((border == 0)
                                  ? "ramadda-accordion"
                                  : null), null);

                return sb.toString();
            } else if (doingGrid) {

                List<String> weights = null;
                boolean showLine = getProperty(wikiUtil, props, "showLine",
                                       getProperty(wikiUtil, props, "doline",
                                           false));
                String ws = getProperty(wikiUtil, props, "weights",
                                        (String) null);
                if (ws != null) {
                    weights = StringUtil.split(ws, ",", true, true);
                }

                int columns = getProperty(wikiUtil, props, "columns", 3);
                int innerHeight = getProperty(wikiUtil, props,
                                      "inner-height", 150);
                int minHeight = getProperty(wikiUtil, props,
                                            "inner-minheight", -1);
                int maxHeight = getProperty(wikiUtil, props,
                                            "inner-maxheight", 300);

                StringBuilder innerStyle = new StringBuilder();
                if (innerHeight > 0) {
                    Utils.concatBuff(innerStyle, "height:",
                                     innerHeight + "px;");
                    innerStyle.append("overflow-y: auto;");
                }
                if (minHeight > 0) {
                    Utils.concatBuff(innerStyle, "min-height:",
                                     minHeight + "px;");
                    innerStyle.append("overflow-y: auto;");
                }
                if (maxHeight > 0) {
                    Utils.concatBuff(innerStyle, "max-height:",
                                     maxHeight + "px;");
                    innerStyle.append("overflow-y: auto;");
                }
                sb.append(HU.open(HU.TAG_DIV, HU.cssClass("ramadda-grid")));
                sb.append("\n");
                int    rowCnt   = 0;
                int    colCnt   = 10000;
                int    weight   = 12 / columns;

                String boxClass = HU.cssClass("ramadda-grid-box");
                String boxStyle = "";
                width = getProperty(wikiUtil, props, ATTR_WIDTH,
                                    (String) null);
                if (width != null) {
                    boxStyle = HU.style("width:" + width + "px;"
                                        + "display:inline-block;margin:6px;");
                }
                for (int i = 0; i < titles.size(); i++) {
                    Entry child = children.get(i);
                    if (width == null) {
                        colCnt++;
                        if (colCnt >= columns) {
                            if (rowCnt > 0) {
                                sb.append(HU.close(HU.TAG_DIV));
                                if (showLine) {
                                    sb.append("<hr>");
                                } else {
                                    //                                sb.append(HU.br());
                                }
                            }
                            rowCnt++;
                            HU.open(sb, HU.TAG_DIV, HU.cssClass("row"));
                            colCnt = 0;
                        }
                        String weightString = "" + weight;
                        if ((weights != null) && (i < weights.size())) {
                            weightString = weights.get(i);
                        }
                        HU.open(sb, HU.TAG_DIV,
                                HU.cssClass("col-md-" + weightString
                                            + " ramadda-col"));
                    }
                    HU.open(sb, HU.TAG_DIV, boxClass + boxStyle);
                    if (showHeading) {
                        HU.div(sb, HU.href(urls.get(i), titles.get(i)),
                               HU.title(titles.get(i)) + headingClass);
                    }
                    String displayHtml = contents.get(i);
                    HU.div(sb, displayHtml,
                           HU.cssClass("bs-inner")
                           + HU.attr("style", innerStyle.toString()));
                    HU.close(sb, HU.TAG_DIV);
                    if (width == null) {
                        HU.close(sb, HU.TAG_DIV);
                    }
                    sb.append("\n");
                }

                //Close the div if there was anything
                if (width == null) {
                    if (rowCnt > 0) {
                        HU.close(sb, HU.TAG_DIV);
                    }
                }

                //Close the grid div
                HU.close(sb, HU.TAG_DIV);

                return sb.toString();
            } else if (doingSlideshow) {
                // for slideshow
                boolean shownav = getProperty(wikiUtil, props, "shownav",
                                      false);
                boolean autoplay = getProperty(wikiUtil, props, "autoplay",
                                       false);
                int    playSpeed = getProperty(wikiUtil, props, "speed", 5);

                String slideId   = HU.getUniqueId("slides_");

                HU.open(sb, "style", HU.attr("type", "text/css"));
                // need to set the height of the div to include the nav bar
                Utils.concatBuff(sb, "#", slideId, " { width: ",
                                 width + "; height: " + (height + 30), "}\n");


                int border = getProperty(wikiUtil, props, ATTR_BORDER, 1);
                String borderColor = getProperty(wikiUtil, props,
                                         ATTR_BORDERCOLOR, "#aaa");
                sb.append(
                    "#" + slideId + " .slides_container {border: " + border
                    + "px solid " + borderColor + "; width:" + width
                    + ";overflow:hidden;position:relative;display:none;}\n.slides_container div.slide {width:"
                    + width + ";height:" + height + "px;display:block;}\n");
                sb.append("</style>\n\n");
                sb.append("<link rel=\"stylesheet\" href=\"");
                sb.append(
                    getRepository().getHtdocsUrl("/lib/slides/paginate.css"));
                sb.append("\" type=\"text/css\" media=\"screen\" />");
                sb.append("\n");


                // user speed is seconds, script uses milliseconds - 0 == no play
                int startSpeed = (autoplay)
                                 ? playSpeed * 1000
                                 : 0;
                String slideParams =
                    "preload: false, preloadImage: "
                    + HU.squote(
                        getRepository().getHtdocsUrl(
                            "/lib/slides/img/loading.gif")) + ", play: "
                                + startSpeed + ", width: \"" + width + "\""
                                + ", pause: 2500, hoverPause: true"
                                + ", generatePagination: " + shownav + "\n";
                StringBuilder js =
                    new StringBuilder("$(document).ready(function(){\n");
                js.append(
                    "$(function(){\n$(" + HU.squote("#" + slideId)
                    + ").slides({" + slideParams
                    + ",\nslidesLoaded: function() {$('.caption').animate({ bottom:0 },200); }\n});\n});\n");
                js.append("\n});");
                HU.open(sb, HU.TAG_DIV, HU.id(slideId));



                String prevImage =
                    HU.href("#", "<i class=\"fas fa-angle-left prev\"/>");

                String nextImage =
                    HU.href("#", "<i class=\"fas fa-angle-right next\"/>");
                String arrowTDWidth = "20px";

                sb.append(
                    "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tr>\n");
                HU.col(sb, prevImage,
                       "width=\"" + arrowTDWidth
                       + "\" style=\"font-size: 30px\"");
                HU.open(sb, HU.TAG_TD, HU.attr(HU.ATTR_WIDTH, width));
                HU.open(sb, HU.TAG_DIV, HU.cssClass("slides_container"));
                for (int i = 0; i < titles.size(); i++) {
                    String title   = titles.get(i);
                    String content = contents.get(i);
                    sb.append("\n");
                    HU.open(sb, HU.TAG_DIV, HU.cssClass("slide"));
                    sb.append(content);
                    //                    sb.append(HU.br());
                    //                    sb.append(title);
                    HU.close(sb, HU.TAG_DIV);  // slide
                }
                HU.close(sb, HU.TAG_DIV);      // slides_container
                HU.close(sb, HU.TAG_TD);
                HU.col(sb, nextImage,
                       "align=\"right\" width=\"" + arrowTDWidth
                       + "\" style=\"font-size: 30px\"");
                sb.append("</tr></table>");
                HU.close(sb, HU.TAG_DIV);  // slideId

                sb.append(
                    HU.importJS(
                        getRepository().getHtdocsUrl(
                            "/lib/slides/slides.min.jquery.js")));

                wikiUtil.appendJavascript(js.toString());
                //                HU.script(sb, js.toString());

                return sb.toString();
            } else {
                //TABS
                int innerHeight = getProperty(wikiUtil, props,
                                      "inner-height", -1);

                if (innerHeight > 1) {
                    List<String> tmp = new ArrayList<String>();
                    for (String content : contents) {
                        tmp.add(HU.div(content,
                                       HU.style("max-height:" + innerHeight
                                           + "px;overflow-y:auto;")));
                    }
                    contents = tmp;
                }
                String style = getProperty(wikiUtil, props, "tabsStyle", "");
                String divClass = "";
                if (style.equals("min")) {
                    divClass = "ramadda-tabs-min";
                } else if (style.equals("center")) {
                    divClass = "ramadda-tabs-center";
                } else if (style.equals("minarrow")) {
                    divClass = "ramadda-tabs-min ramadda-tabs-minarrow";
                }
                sb.append(HU.open("div", HU.cssClass(divClass)));
                sb.append(OutputHandler.makeTabs(titles, contents, true,
                        useCookies));

                sb.append(HU.close("div"));

                return sb.toString();
            }
        } else if (theTag.equals(WIKI_TAG_TABLE)) {
            List<Entry> entries = getEntries(request, wikiUtil,
                                             originalEntry, entry, props);
            getHtmlOutputHandler().makeTable(request, entries, sb, props);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_WIKITEXT)) {
            StringBuilder editor = new StringBuilder();

            boolean showToolbar = getProperty(wikiUtil, props, "showToolbar",
                                      false);

            String text = entry.getTypeHandler().getTextForWiki(request,
                              entry, props);
            entry.getTypeHandler().addWikiEditor(request, entry, editor,
                    null, HU.getUniqueId(""), "", text, null, !showToolbar,
                    0);

            return editor.toString();
        } else if (theTag.equals(WIKI_TAG_RECENT)) {
            List<Entry> children = getEntries(request, wikiUtil,
                                       originalEntry, entry, props);
            int numDays = getProperty(wikiUtil, props, ATTR_DAYS, 3);
            BufferMapList<Date> map = new BufferMapList<Date>();
            SimpleDateFormat dateFormat =
                new SimpleDateFormat("EEEEE MMMMM d");
            dateFormat.setTimeZone(RepositoryUtil.TIMEZONE_UTC);
            Date firstDay = ((children.size() > 0)
                             ? new Date(children.get(0).getChangeDate())
                             : new Date());
            GregorianCalendar cal1 =
                new GregorianCalendar(RepositoryUtil.TIMEZONE_UTC);
            cal1.setTime(new Date(firstDay.getTime()));
            cal1.set(cal1.MILLISECOND, 0);
            cal1.set(cal1.SECOND, 0);
            cal1.set(cal1.MINUTE, 0);
            cal1.set(cal1.HOUR, 0);
            GregorianCalendar cal2 =
                new GregorianCalendar(RepositoryUtil.TIMEZONE_UTC);
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
                sb.append(HU.makeShowHideBlock(msg, tmp.toString(), true));
            }

            return sb.toString();

        } else if (theTag.equals(WIKI_TAG_PLAYER)
                   || theTag.equals(WIKI_TAG_PLAYER_OLD)) {
            boolean useAttachment = getProperty(wikiUtil, props,
                                        "useAttachment", false);

            List<Entry> children = getEntries(request, wikiUtil,
                                       originalEntry, entry, props, true);
            if (children.size() == 0) {
                return null;
            }
            ImageOutputHandler imageOutputHandler =
                (ImageOutputHandler) getRepository().getOutputHandler(
                    ImageOutputHandler.OUTPUT_PLAYER);
            Request imageRequest = request.cloneMe();

            int     width        = getProperty(wikiUtil, props, ATTR_WIDTH,
                                       0);
            if (width != 0) {
                imageRequest.put(ARG_WIDTH, "" + width);
            }
            boolean loopStart = getProperty(wikiUtil, props, "loopstart",
                                            false);
            if (loopStart) {
                imageRequest.put("loopstart", "true");
            }

            if (useAttachment) {
                imageRequest.put("useAttachment", "true");
            }

            int delay = getProperty(wikiUtil, props, "loopdelay", 0);
            if (delay > 0) {
                imageRequest.put("loopdelay", "" + delay);
            }

            String iwidth = getProperty(wikiUtil, props, "imageWidth",
                                        (String) null);
            if (iwidth != null) {
                imageRequest.put(ARG_WIDTH, iwidth);
            }
            int height = getProperty(wikiUtil, props, ATTR_HEIGHT, 0);
            if (height > 0) {
                imageRequest.put(ARG_HEIGHT, "" + height);
            }
            imageOutputHandler.makePlayer(imageRequest, entry, children, sb,
                                          getProperty(wikiUtil, props,
                                              "show_sort_links",
                                                  false), false);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_GALLERY)) {
            List<Entry> children = getEntries(request, wikiUtil,
                                       originalEntry, entry, props, true);
            if (children.size() == 0) {
                String message = getProperty(wikiUtil, props, ATTR_MESSAGE,
                                             (String) null);
                if (message != null) {
                    return message;
                }
            }
            if (children.size() > 0) {
                checkHeading(request, wikiUtil, props, sb);
            }
            makeGallery(request, wikiUtil, children, props, sb);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_ROOT)) {
            return getRepository().getUrlBase();
        } else if (theTag.equals(WIKI_TAG_FULLTREE) || theTag.equals(WIKI_TAG_MENUTREE)) {
	    boolean doMenu = theTag.equals(WIKI_TAG_MENUTREE);
            int depth = getProperty(wikiUtil, props, "depth", 10);
            boolean addPrefix = getProperty(wikiUtil, props, "addPrefix",
                                            false);
            boolean showRoot = getProperty(wikiUtil, props, "showroot",
                                           false);
            boolean showIcon = getProperty(wikiUtil, props, "showicon",
                                           false);
            List<String> types = StringUtil.split(getProperty(wikiUtil,
                                     props, "types", ""), ",", true, true);

	    String menuId = HU.getUniqueId("tree_");
	    String style = getProperty(wikiUtil,props,"menuStyle","");
	    if (addPrefix) 
		style = "list-style-type:none;" + style;
	    int labelWidth = getProperty(wikiUtil,props,"labelWidth",30);
	    sb.append("\n");
	    doFullTree(request, wikiUtil, originalEntry, entry, props, true, doMenu, menuId,  
                       style, labelWidth, addPrefix, "", showRoot, showIcon, depth, types, sb);

	    sb.append("\n");
	    if(doMenu) {
		HU.script(sb, "$('#" +menuId+"').menu();\n");
	    }
	    sb.append("\n");
            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_CHILDREN_GROUPS)
                   || theTag.equals(WIKI_TAG_CHILDREN_ENTRIES)
                   || theTag.equals(WIKI_TAG_CHILDREN)
                   || theTag.equals(WIKI_TAG_TREE)) {

            if (theTag.equals(WIKI_TAG_CHILDREN_GROUPS)) {
                props.put(ATTR_FOLDERS, "true");
            } else if (theTag.equals(WIKI_TAG_CHILDREN_ENTRIES)) {
                props.put(ATTR_FILES, "true");
            }

            List<Entry> children = getEntries(request, wikiUtil,
                                       originalEntry, entry, props);
            if (children.size() == 0) {
                //              return makeCard(request, wikiUtil, props, entry);
                return getMessage(wikiUtil, props, "No entries available");
            }
            String treePrefix = getProperty(wikiUtil, props, "treePrefix",
                                            "");
            //      if(treePrefix.length()>0) {
            //              treePrefix = makeWikiUtil(request, false).wikify(sb, treePrefix, null);
            //      }

            boolean showCategories = getProperty(wikiUtil, props,
                                         ARG_SHOWCATEGORIES, false);
            if (showCategories) {
                request.put(ARG_SHOWCATEGORIES, "true");
            }
            boolean decorate = getProperty(wikiUtil, props, ATTR_DECORATE,
                                           true);
            boolean showDetails = getProperty(wikiUtil, props, ATTR_DETAILS,
                                      true);
            boolean showIcon = getProperty(wikiUtil, props, "showIcon", true);

            Request newRequest = request.cloneMe();

            if ( !showDetails) {
                newRequest.put(ARG_DETAILS, "false");
            }
            if ( !decorate) {
                newRequest.put(ARG_DECORATE, "false");
            }

            if (children.size() > 0) {
                checkHeading(request, wikiUtil, props, sb);
            }
            int max = request.get(ARG_MAX,
                                  getProperty(wikiUtil, props, ATTR_MAX, -1));
            if ( !getProperty(wikiUtil, props, ARG_SHOWNEXT, true)) {
                newRequest.put(ARG_SHOWNEXT, "false");
            } else if (max > 0) {
                newRequest.put(ARG_MAX, max + "");
            }
            String link = getHtmlOutputHandler().getEntriesList(newRequest,
                              sb, children, true, false, showDetails,
                              showIcon);
            if (getProperty(wikiUtil, props, "form", false)) {
                return treePrefix + link + HU.br() + sb.toString();
            } else {
                return treePrefix + sb.toString();
            }

        } else if (theTag.equals(WIKI_TAG_TREEVIEW)
                   || theTag.equals(WIKI_TAG_FRAMES)) {
            int width = getDimension(wikiUtil, props, ATTR_WIDTH, -100);
            int height = getDimension(wikiUtil, props, ATTR_HEIGHT, 500);

            List<Entry> children = getEntries(request, wikiUtil,
                                       originalEntry, entry, props);
            if (children.size() == 0) {
                return null;
            }
            boolean noTemplate = getProperty(wikiUtil, props, "noTemplate",
                                             true);
            getHtmlOutputHandler().makeTreeView(request, children, sb, width,
                    height, noTemplate);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_LINKS)
                   || theTag.equals(WIKI_TAG_LIST)) {
            boolean isList = theTag.equals(WIKI_TAG_LIST);
            List<Entry> children = getEntries(request, wikiUtil,
                                       originalEntry, entry, props);
            if (children.size() == 0) {
                if (getProperty(wikiUtil, props, "defaultToCard", false)) {
                    return makeCard(request, wikiUtil, props, entry);
                }

                return null;
            }

            boolean highlightThis = getProperty(wikiUtil, props,
                                        "highlightThis", false);
            boolean horizontal = getProperty(wikiUtil, props, "horizontal",
                                             false);
            boolean includeIcon = getProperty(wikiUtil, props,
                                      ATTR_INCLUDEICON, false);


            boolean linkResource = getProperty(wikiUtil, props,
                                       ATTR_LINKRESOURCE, false);
            String separator = (isList
                                ? ""
                                : getProperty(wikiUtil, props,
                                    ATTR_SEPARATOR, horizontal
                    ? "&nbsp|&nbsp;"
                    : ""));

            String output = getProperty(wikiUtil, props, "output",
                                        (String) null);
            String cssClass = getProperty(wikiUtil, props, ATTR_CLASS, "");
            String style    = getProperty(wikiUtil, props, ATTR_STYLE, "");
            String tagOpen  = (isList
                               ? "<li>"
                               : getProperty(wikiUtil, props, ATTR_TAGOPEN,
                                             "<li>"));

            String tagClose = (isList
                               ? ""
                               : getProperty(wikiUtil, props, ATTR_TAGCLOSE,
                                             ""));

            if (includeIcon) {
                tagOpen  = "";
                tagClose = "<br>";
            }

            if (horizontal) {
                tagOpen  = "<div class='ramadda-links-horizontal'>";
                tagClose = "</div>";
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
                    if (output == null) {
                        url = getEntryManager().getEntryUrl(request, child);
                        //                        url = request.entryUrl(getRepository().URL_ENTRY_SHOW, child);
                    } else {
                        url = request.entryUrl(
                            getRepository().URL_ENTRY_SHOW, child,
                            ARG_OUTPUT, output);
                    }
                }

                String linkLabel = getEntryDisplayName(child);
                if (includeIcon) {
                    linkLabel = HU.img(getPageHandler().getIconUrl(request,
                            child)) + HU.space(1) + linkLabel;
                }
                String href = HU.href(url, linkLabel,
                                      HU.cssClass(cssClass)
                                      + HU.style(style));
                boolean highlight =
                    highlightThis && (originalEntry != null)
                    && child.getId().equals(originalEntry.getId());
                if (highlight) {
                    href = HU.div(href, HU.clazz("ramadda-links-highlight"));
                }
                StringBuilder link = new StringBuilder();
                link.append(tagOpen);
                link.append(href);
                link.append(tagClose);
                String s = link.toString();
                links.add(s);
            }

            StringBuilder contentsSB = new StringBuilder();
            String prefix = getProperty(wikiUtil, props, ATTR_LIST_PREFIX,
                                        (String) null);
            String suffix = getProperty(wikiUtil, props, ATTR_LIST_SUFFIX,
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
            boolean showTitle = getProperty(wikiUtil, props, ATTR_SHOWTITLE,
                                            false);


            String innerClass = getProperty(wikiUtil, props, ATTR_INNERCLASS,
                                            "ramadda-links-inner");


            String contentsStyle = getProperty(wikiUtil, props,
                                       "contentsStyle", "");


            String contents = HU.div(contentsSB.toString(),
                                     HU.cssClass(innerClass)
                                     + HU.style(contentsStyle));
            String title = getProperty(wikiUtil, props, ATTR_TITLE, showTitle
                    ? entry.getName()
                    : null);
            if (title != null) {
                String entryUrl =
                    request.entryUrl(getRepository().URL_ENTRY_SHOW, entry);
                title    = HU.href(entryUrl, title);
                contents = HU.h3(title) + contents;
            }
            contents = HU.div(contents, HU.cssClass("ramadda-links"));

            return contents;
        } else {
            String fromTypeHandler =
                entry.getTypeHandler().getWikiInclude(wikiUtil, request,
                    originalEntry, entry, theTag, props);
            if (fromTypeHandler != null) {
                if (wikify) {
                    fromTypeHandler = wikifyEntry(request, entry,
                            fromTypeHandler, false, null, null,
                            wikiUtil.getNotTags());
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

        return null;
    }



    public String embedJson(Request request, String json) throws Exception {
        StringBuilder sb = new StringBuilder();
	HU.importJS(sb, getPageHandler().makeHtdocsUrl("/media/json.js"));
	String id = Utils.getGuid();
	//entry.getResource().getPath(), true);
	String formatted = Json.format(json,true);
	HtmlUtils.open(sb, "div", "id", id);
	HtmlUtils.pre(sb, formatted);
	HtmlUtils.close(sb, "div");
	sb.append(HtmlUtils.script("RamaddaJson.init('" + id + "');"));
	return sb.toString();
    }
	


    /**
     * _more_
     *
     * @param request the request
     * @param entry _more_
     * @param id _more_
     * @param text _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getWikiMetadataLabel(Request request, Entry entry,
                                       String id, String text)
            throws Exception {
        List<Metadata> list = getMetadataManager().findMetadata(request,
                                  entry, "wiki_label", true);
        if (list == null) {
            return text;
        }
        for (Metadata metadata : list) {
            if (Misc.equals(id, metadata.getAttr1())) {
                text = metadata.getAttr2();

                break;
            }
        }

        return text;
    }


    /**
     * _more_
     *
     * @param request the request
     * @param wikiUtil _more_
     * @param entry _more_
     * @param originalEntry _more_
     * @param theTag _more_
     * @param props _more_
     * @param sb _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public MapInfo handleMapTag(Request request, WikiUtil wikiUtil,
                                Entry entry, Entry originalEntry,
                                String theTag, Hashtable props, Appendable sb)
            throws Exception {

        String  width      = getProperty(wikiUtil, props, ATTR_WIDTH, "");
        String  height     = getProperty(wikiUtil, props, ATTR_HEIGHT, "300");
        boolean justPoints = getProperty(wikiUtil, props, "justpoints",
                                         false);
        boolean listEntries = getProperty(wikiUtil, props, ATTR_LISTENTRIES,
                                          false);
        boolean showCheckbox = getProperty(wikiUtil, props, "showCheckbox",
                                           false);
        boolean showSearch = getProperty(wikiUtil, props, "showSearch",
                                         false);
        boolean checkboxOn = getProperty(wikiUtil, props, "checkboxOn", true);
        boolean googleEarth =
            theTag.equals(WIKI_TAG_EARTH)
            && getMapManager().isGoogleEarthEnabled(request);

        List<Entry> children;
        if (theTag.equals(WIKI_TAG_MAPENTRY)) {
            children = new ArrayList<Entry>();
            children.add(entry);
        } else {
            children = getEntries(request, wikiUtil, originalEntry, entry,
                                  props, false, "");
            if (children.isEmpty()) {
                children.add(entry);
            }
        }

        if (children.size() == 0) {
            String message = getProperty(wikiUtil, props, ATTR_MESSAGE,
                                         (String) null);
            if (message != null) {
                sb.append(message);

                return null;
            }
        } else {
            boolean anyHaveLatLon = false;
            for (Entry child : children) {
                if (child.hasLocationDefined() || child.hasAreaDefined()) {
                    anyHaveLatLon = true;

                    break;
                }
            }
            if ( !anyHaveLatLon) {
                String message = getProperty(wikiUtil, props, ATTR_MESSAGE,
                                             (String) null);
                if (message != null) {
                    sb.append(message);

                    return null;
                }
            }
        }

        checkHeading(request, wikiUtil, props, sb);
        Request newRequest = makeRequest(request, props);
        if (googleEarth) {
            getMapManager().getGoogleEarth(newRequest, children, sb,
                                           getProperty(wikiUtil, props,
                                               ATTR_WIDTH,
                                                   ""), getProperty(wikiUtil,
                                                       props, ATTR_HEIGHT,
                                                           ""), listEntries,
                                                               justPoints);
        } else {
            MapOutputHandler mapOutputHandler =
                (MapOutputHandler) getRepository().getOutputHandler(
                    MapOutputHandler.OUTPUT_MAP);
            if (mapOutputHandler == null) {
                sb.append("No maps");

                return null;
            }

            String icon = getProperty(wikiUtil, props, ATTR_ICON);
            if ((icon != null) && icon.startsWith("#")) {
                icon = null;
            }
            if (icon != null) {
                newRequest.put(ARG_ICON, icon);
            }
            if (getProperty(wikiUtil, props, ARG_MAP_ICONSONLY, false)) {
                newRequest.put(ARG_MAP_ICONSONLY, "true");
            }

            Hashtable mapProps = new Hashtable();
            String[]  mapArgs  = {
                "strokeColor", "strokeWidth", "fillColor", "fillOpacity",
                "scrollToZoom", "boxColor", "shareSelected", "doPopup",
                "fill", "selectOnHover", "onSelect", "showDetailsLink",
                "initialZoom:zoom", "defaultMapLayer:layer", "kmlLayer",
                "kmlLayerName", "displayDiv", "initialBounds:bounds",
                "showLatLonPosition"
            };
            for (String mapArg : mapArgs) {
                String key = mapArg;
                if (mapArg.indexOf(":") >= 0) {
                    List<String> toks = StringUtil.splitUpTo(mapArg, ":", 2);
                    mapArg = toks.get(0);
                    key    = toks.get(1);
                }
                String v = getProperty(wikiUtil, props, key);
                if (v != null) {
                    v = v.replace("${entryid}", entry.getId());
                    mapProps.put(mapArg, Json.quote(v));
                }
            }

            String mapSet = getProperty(wikiUtil, props, "mapSettings",
                                        (String) null);
            if (mapSet != null) {
                List<String> msets = StringUtil.split(mapSet, ",");
                for (int i = 0; i < msets.size() - 1; i += 2) {
                    mapProps.put(msets.get(i), Json.quote(msets.get(i + 1)));
                }
            }
            MapInfo map = getMapManager().getMap(newRequest, entry, children,
                              sb, width, height, mapProps, props);
            if (icon != null) {
                newRequest.remove(ARG_ICON);
            }
            newRequest.remove(ARG_MAP_ICONSONLY);

            return map;
        }

        return null;

    }

    /**
     * _more_
     *
     * @param request the request
     * @param wikiUtil _more_
     * @param props _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void checkHeading(Request request, WikiUtil wikiUtil,
                             Hashtable props, Appendable sb)
            throws Exception {
        String heading = getProperty(wikiUtil, props, "heading",
                                     (String) null);
        if (heading != null) {
            sb.append(HU.div(heading, HU.cssClass("ramadda-page-heading")));

        }
    }

    /**
     * _more_
     *
     * @param request the request
     * @param child _more_
     * @param wikify _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getSnippet(Request request, Entry child, boolean wikify)
            throws Exception {
        String snippet = getRawSnippet(request, child, wikify);
        if (snippet == null) {
            return "";
        }

        return HU.div(snippet, HU.cssClass("ramadda-snippet"));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param child _more_
     * @param wikify _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getRawSnippet(Request request, Entry child, boolean wikify)
            throws Exception {
        String snippet = child.getSnippet();
        if (snippet != null) {
            if (wikify) {
                snippet = wikifyEntry(request, child, snippet);
            }

            return snippet;
        }
        String text = child.getTypeHandler().getEntryText(child);
        snippet = StringUtil.findPattern(text, "(?s)<snippet>(.*)</snippet>");
        if (snippet == null) {
            snippet = StringUtil.findPattern(
                text, "(?s)<snippet-hide>(.*)</snippet-hide>");
        }
        child.setSnippet(snippet);
        if ((snippet != null) && wikify) {
            snippet = wikifyEntry(request, child, snippet);
        }

        return snippet;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param wikiUtil _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param props _more_
     * @param addPrefix _more_
     * @param prefix _more_
     * @param showRoot _more_
     * @param showIcon _more_
     * @param depth _more_
     * @param types _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    private void doFullTree(Request request,  WikiUtil wikiUtil,
                            Entry originalEntry, Entry entry,
                            Hashtable props,
			    boolean top, boolean asMenu, String menuId,
			    String style,
			    int labelWidth,
			    boolean addPrefix,
                            String prefix, boolean showRoot,
                            boolean showIcon, int depth, List<String> types,
                            Appendable sb)
            throws Exception {
	if(top) {
	    HU.open(sb,"ul",HU.attrs("id",menuId, "style",style));
	    sb.append("\n");
	}
	
	if ((prefix.length() > 0) || showRoot) {
	    HU.open(sb, "li");
            String label = Utils.clipTo(getEntryManager().getEntryDisplayName(entry),labelWidth,"...");
	    if(showIcon)
		label = HtmlUtils.img(getPageHandler().getIconUrl(request, entry)) + HU.SPACE + label;
            String link =  HtmlUtils.href(getEntryManager().getEntryURL(request, entry), label, HU.attrs("class","ramadda-tree-link"));
	    if(addPrefix) link = prefix +" " + link;
	    if(asMenu) link = HU.div(link);
            sb.append(link);
	    if(top && showRoot) sb.append("<ul>");
	    sb.append("\n");
        }

        if (depth-- < 0) {
	    if(top && showRoot) HU.close(sb,"ul","\n");
	    HU.close(sb,"li","\n");
	    if(top) HU.close(sb,"ul","\n");
            return;
        }
        List<Entry> children = getEntries(request, wikiUtil, originalEntry,
                                          entry, props);

        if (children.size() > 0) {
	    boolean addedUl = false;
            int cnt = 1;
            for (Entry child : children) {
                if (types.size() > 0) {
                    boolean ok = false;
                    for (String type : types) {
                        if (child.getTypeHandler().isType(type)) {
                            ok = true;
                            break;
                        }
                    }
                    if ( !ok) {
                        continue;
                    }
                }

		if(!top) {
		    if(!addedUl) {
			HU.open(sb,"ul",HU.attrs("style",style));
			sb.append("\n");
			addedUl = true;
		    }
		}
                String p = ((prefix.length() > 0)
                            ? prefix + "."
                            : "") + (cnt++);
                doFullTree(request,  wikiUtil, originalEntry, child, props,
			   false, asMenu, null,			   
                           style, labelWidth, addPrefix, p, showRoot, showIcon, depth, types,
                           sb);
	    }
	    if(addedUl) {
		HU.close(sb,"ul","\n");
	    }
        }
	if(top && showRoot) HU.close(sb,"ul","\n");
	HU.close(sb,"li","\n");	
	if(top) HU.close(sb,"ul","\n");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param wikiUtil _more_
     * @param props _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String makeCard(Request request, WikiUtil wikiUtil,
                            Hashtable props, Entry entry)
            throws Exception {

        StringBuilder card = new StringBuilder();
        HU.open(card, HU.TAG_DIV, HU.cssClass("ramadda-card"));
        String entryUrl = request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                           entry);
        boolean showHeading = getProperty(wikiUtil, props, "showHeading",
                                          true);
        if (showHeading) {
            HU.div(card, HU.href(entryUrl, entry.getName()),
                   HU.title(entry.getName())
                   + HU.cssClass("ramadda-subheading"));
        }
        boolean useThumbnail = getProperty(wikiUtil, props, "useThumbnail",
                                           true);
        boolean showSnippet = getProperty(wikiUtil, props, "showSnippet",
                                          false);

        boolean showSnippetHover = getProperty(wikiUtil, props,
                                       "showSnippetHover", false);

        if (showSnippet || showSnippetHover) {
            String snippet = getSnippet(request, entry, false);
            if (Utils.stringDefined(snippet)) {
                snippet = wikifyEntry(request, entry, snippet, false, null,
                                      null, wikiUtil.getNotTags());
                if (showSnippet) {
                    HU.div(card, snippet, HU.cssClass("ramadda-snippet"));

                } else if (showSnippetHover) {
                    HU.div(card, snippet,
                           HU.cssClass("ramadda-snippet-hover"));

                }

            }
        }

        String imageUrl = null;
        if (useThumbnail) {
            imageUrl = getMetadataManager().getThumbnailUrl(request, entry);
        }

        if (imageUrl == null) {
            if (entry.isImage()) {
                imageUrl = getRepository().getHtmlOutputHandler().getImageUrl(
                    request, entry);
            } else if ( !useThumbnail) {
                imageUrl = getMetadataManager().getThumbnailUrl(request,
                        entry);
            }
        }

        //Default to the type icon
        if (imageUrl == null) {
            imageUrl = entry.getTypeHandler().getTypeIconUrl();
        }

        if (imageUrl != null) {
            String img = HU.img(imageUrl, "",
                                HU.attr("loading", "lazy")
                                + HU.style("width:100%;"));
            String  inner;
            boolean popup = getProperty(wikiUtil, props, "popup", true);
            if (popup) {
                boolean popupResource = getProperty(wikiUtil, props,
                                            "popupResource", false);
                String popupGroup = getProperty(wikiUtil, props,
                                        "popupPrefix", "popupgallery");
                String titleId = HU.getUniqueId("id");
                String caption = " <a href='" + entryUrl + "'>"
                                 + entry.getName() + "</a> ";
                String popupUrl = imageUrl;
                if (popupResource) {
                    String path = entry.getResource().getPath().toLowerCase();
                    if (path.endsWith("pdf") || Utils.isImage(path)) {
                        popupUrl = entry.getTypeHandler().getEntryResourceUrl(
                            request, entry);
                    }
                }
                inner =
                    HU.href(popupUrl, img,
                            HU.attr("data-caption", caption)
                            + HU.attr("id", popupGroup + HU.getUniqueId(""))
                            + HU.attr("data-fancybox", popupGroup)
                            + HU.cssClass("popup_image"));
            } else {
                inner = HU.href(entryUrl, img, HU.cssClass(""));
            }
            card.append(HU.div(inner, HU.cssClass("ramadda-imagewrap")));

            if (popup) {
                addImagePopupJS(request, wikiUtil, card, props);
            }
        }

        HU.close(card, HU.TAG_DIV);

        return card.toString();


    }

    /**
     * _more_
     *
     * @param request the request
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
     * @param wikiUtil _more_
     * @param buf      the page StringBuilder
     * @param props    the properties
     */
    public void addImagePopupJS(Request request, WikiUtil wikiUtil,
                                StringBuilder buf, Hashtable props) {
        if (request.getExtraProperty("added fancybox") == null) {
            String captionpos = getProperty(wikiUtil, props,
                                            ATTR_POPUPCAPTION, "none");
            StringBuilder options = new StringBuilder("{");
            //Note: this doesn't work in v3
            if ( !captionpos.equals("none")) {
                options.append("helpers:{title:{");
                if (captionpos.equals("inside")) {
                    options.append("type:'inside'");
                } else if (captionpos.equals("over")) {
                    options.append("type:'over'");
                } else if (captionpos.equals("top")) {
                    options.append("type:'over',");
                    options.append("position:'top'");
                } else {
                    options.append("type:'outside'");
                }
                options.append("}}");
            } else {
                options.append(HU.squote("titleShow"));
                options.append(" : ");
                options.append("false");
            }

            if (getProperty(wikiUtil, props, "popupIframe", false)) {
                //              options.append(",width:600");
                //              options.append(",height:300");
                options.append(",type:'iframe'");
            }


            options.append("}");

            if (wikiUtil != null) {
                wikiUtil.appendJavascript(
                    "$(document).ready(function() {\n $(\"a.popup_image\").fancybox("
                    + options.toString() + ");\n });\n");
            } else {
                buf.append(
                    HU.script(
                        "$(document).ready(function() {\n $(\"a.popup_image\").fancybox("
                        + options.toString() + ");\n });\n"));
            }
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
     * @param request the request
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
     * @param request the request
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
            String url = getEntryManager().getEntryUrl(request, entry);
            wikiUtil.setTitleUrl(url);
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
        for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            if ( !original.hasParameter(key.toString())) {
                newRequest.put(key, props.get(key));
            }
        }
        newRequest.put(ARG_EMBEDDED, "true");

        return newRequest;
    }

    /**
     * Make entry tabs html
     *
     * @param request The request
     * @param wikiUtil _more_
     * @param entry  the entry
     * @param props _more_
     *
     * @return the entry tabs html
     *
     * @throws Exception  problems
     */
    private String makeEntryTabs(Request request, WikiUtil wikiUtil,
                                 Entry entry, Hashtable props)
            throws Exception {

        request = request.cloneMe();
        request.putExtraProperty("wiki.props", props);


        List<String> onlyTheseTypes = null;
        List<String> notTheseTypes  = null;

        String metadataTypesAttr = getProperty(wikiUtil, props,
                                       ATTR_METADATA_TYPES, (String) null);
        String separator = getProperty(wikiUtil, props, "separator",
                                       (String) null);
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
        boolean includeTitle = getProperty(wikiUtil, props,
                                           ATTR_METADATA_INCLUDE_TITLE, true);



        for (TwoFacedObject tfo :
                getRepository().getHtmlOutputHandler().getMetadataHtml(
                    request, entry, onlyTheseTypes, notTheseTypes,
                    includeTitle, separator)) {
            tabTitles.add(tfo.toString());
            tabContents.add(tfo.getId());
        }
        if (tabTitles.size() == 0) {
            return getMessage(wikiUtil, props, "No metadata found");
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
     * @param request the request
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
     * @param request the request
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
     * @param wikiUtil _more_
     * @param originalEntry _more_
     * @param entry  the parent entry
     * @param props  properties
     *
     * @return the list of entries
     *
     * @throws Exception problems
     */
    public List<Entry> getEntries(Request request, WikiUtil wikiUtil,
                                  Entry originalEntry, Entry entry,
                                  Hashtable props)
            throws Exception {

        return getEntries(request, wikiUtil, originalEntry, entry, props,
                          false, "");
    }



    /**
     * Get the entries for the request
     *
     * @param request  the request
     * @param wikiUtil _more_
     * @param originalEntry _more_
     * @param entry    the entry
     * @param props    properties
     * @param onlyImages  true to only show images
     *
     * @return the list of Entry's
     *
     * @throws Exception  problems making list
     */
    public List<Entry> getEntries(Request request, WikiUtil wikiUtil,
                                  Entry originalEntry, Entry entry,
                                  Hashtable props, boolean onlyImages)
            throws Exception {
        return getEntries(request, wikiUtil, originalEntry, entry, props,
                          onlyImages, "");
    }


    /**
     * Get the entries for the request
     *
     * @param request  the request
     * @param wikiUtil _more_
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
    public List<Entry> getEntries(Request request, WikiUtil wikiUtil,
                                  Entry originalEntry, Entry entry,
                                  Hashtable props, boolean onlyImages,
                                  String attrPrefix)
            throws Exception {
        if (props == null) {
            props = new Hashtable();
        } else {
            Hashtable tmp = new Hashtable();
            tmp.putAll(props);
            props = tmp;
        }


        String userDefinedEntries = getProperty(wikiUtil, props,
                                        attrPrefix + ATTR_ENTRIES,
                                        ID_CHILDREN);

        return getEntries(request, wikiUtil, originalEntry, entry,
                          userDefinedEntries, props, onlyImages, attrPrefix);
    }


    /**
     * _more_
     *
     * @param request the request
     * @param wikiUtil _more_
     * @param entry _more_
     * @param filter _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> applyFilter(Request request, WikiUtil wikiUtil,
                                   Entry entry, String filter,
                                   Hashtable props)
            throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);

        return applyFilter(request, wikiUtil, entries, filter, props);
    }


    /**
     * _more_
     *
     * @param request the request
     * @param wikiUtil _more_
     * @param entries _more_
     * @param filter _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> applyFilter(Request request, WikiUtil wikiUtil,
                                   List<Entry> entries, String filter,
                                   Hashtable props)
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
            boolean useAttachment = getProperty(wikiUtil, props,
                                        "useAttachment", false);
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
        } else if (filter.startsWith(FILTER_NAME)) {
            String      name = filter.substring(FILTER_NAME.length());
            List<Entry> tmp  = new ArrayList<Entry>();
            for (Entry child : entries) {
                boolean matches = child.getName().matches(name);
                orNot(tmp, child, matches, doNot);
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
     * @param request the request
     * @param wikiUtil _more_
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
    public List<Entry> getEntries(Request request, WikiUtil wikiUtil,
                                  Entry originalEntry, Entry entry,
                                  String userDefinedEntries, Hashtable props,
                                  boolean onlyImages, String attrPrefix)
            throws Exception {



        if (props == null) {
            props = new Hashtable();
        }

        request = request.cloneMe();

        //If there is a max property then clone the request and set the max
        //For some reason we are using both count and max as attrs
        int count = getProperty(wikiUtil, props, attrPrefix + ATTR_COUNT, -1);

        //Override the max from the url args
        int max = request.get(ARG_MAX, -1);
        if (max == -1) {
            max = getProperty(wikiUtil, props, attrPrefix + ATTR_MAX, count);
        }
        if (max > 0) {
            request.put(ARG_MAX, "" + max);
        }

        List<Entry> entries = getEntries(request, wikiUtil, entry,
                                         userDefinedEntries, props);
        String filter = getProperty(wikiUtil, props,
                                    attrPrefix + ATTR_ENTRIES + ".filter",
                                    (String) null);

        if (getProperty(wikiUtil, props, attrPrefix + ATTR_FOLDERS, false)) {
            filter = FILTER_FOLDER;
        }

        if (getProperty(wikiUtil, props, attrPrefix + ATTR_FILES, false)) {
            filter = FILTER_FILE;
        }


        //TODO - how do we combine filters? what kind of or/and logic?
        if (filter != null) {
            entries = applyFilter(request, wikiUtil, entries, filter, props);
        }



        if (onlyImages
                || getProperty(wikiUtil, props, attrPrefix + ATTR_IMAGES,
                               false)) {
            boolean useAttachment = getProperty(wikiUtil, props,
                                        "useAttachment", false);
            entries = applyFilter(request, wikiUtil,
                                  getImageEntries(request, entries,
                                      useAttachment), filter, props);
        }


        String excludeEntries = getProperty(wikiUtil, props,
                                            attrPrefix + ATTR_EXCLUDE,
                                            (String) null);

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


        //Only do the sort if the user has not done an entry sort
        String sort = null;

        if (request.exists(ARG_ORDERBY)) {
            sort = request.getString(ARG_ORDERBY, SORTBY_NAME);
        }
        if (sort == null) {
            sort = getProperty(wikiUtil, props, attrPrefix + ATTR_SORT,
                               (String) null);
        }

        if (sort != null) {
            String dir = null;
            if (request.exists(ARG_ASCENDING)) {
                dir = request.get(ARG_ASCENDING, true)
                      ? "up"
                      : "down";

            }
            if (dir == null) {
                dir = getProperty(wikiUtil, props,
                                  attrPrefix + ATTR_SORT_ORDER, null);
            }
            //If no dir specified then do ascending if we are sorting by name else do descending
            if (dir == null) {
                if ((sort.indexOf(SORTBY_NAME) >= 0)
                        || (sort.indexOf(SORTBY_ENTRYORDER) >= 0)) {
                    dir = "up";
                } else {
                    dir = "down";
                }
            }
            boolean descending = dir.equals("down");
            entries = getEntryUtil().sortEntries(entries, sort, descending);
        }

        String firstEntries = getProperty(wikiUtil, props,
                                          attrPrefix + ATTR_FIRST,
                                          (String) null);

        if (firstEntries != null) {
            Hashtable<String, Entry> map = new Hashtable<String, Entry>();
            for (Entry child : entries) {
                map.put(child.getId(), child);
                map.put(child.getName(), child);
            }
            List<String> ids = StringUtil.split(firstEntries, ",");
            for (int i = ids.size() - 1; i >= 0; i--) {
                String id         = ids.get(i);
                Entry  firstEntry = map.get(id);
                if (firstEntry == null) {
                    if (StringUtil.containsRegExp(id)) {
                        for (Entry child : entries) {
                            if (child.getName().matches(id)) {
                                firstEntry = child;

                                break;
                            }
                        }
                    }
                }
                if (firstEntry == null) {
                    continue;
                }
                entries.remove(firstEntry);
                entries.add(0, firstEntry);
            }
        }


        String lastEntries = getProperty(wikiUtil, props,
                                         attrPrefix + ATTR_LAST,
                                         (String) null);

        if (lastEntries != null) {
            Hashtable<String, Entry> map = new Hashtable<String, Entry>();
            for (Entry child : entries) {
                map.put(child.getId(), child);
                map.put(child.getName(), child);
            }
            List<String> ids = StringUtil.split(lastEntries, ",");
            for (int i = ids.size() - 1; i >= 0; i--) {
                String id        = ids.get(i);
                Entry  lastEntry = map.get(id);
                if (lastEntry == null) {
                    if (StringUtil.containsRegExp(id)) {
                        for (Entry child : entries) {
                            if (child.getName().matches(id)) {
                                lastEntry = child;

                                break;
                            }
                        }
                    }
                }

                if (lastEntry == null) {
                    continue;
                }
                entries.remove(lastEntry);
                entries.add(lastEntry);
            }
        }

        String name = getProperty(wikiUtil, props, attrPrefix + ATTR_NAME,
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
     * @param wikiUtil _more_
     * @param baseEntry _more_
     * @param ids  list of comma separated ids
     * @param props _more_
     *
     * @return List of Entrys
     *
     * @throws Exception problem getting entries
     */
    public List<Entry> getEntries(Request request, WikiUtil wikiUtil,
                                  Entry baseEntry, String ids,
                                  Hashtable props)
            throws Exception {

        if (props == null) {
            props = new Hashtable();
        }


        Hashtable   searchProps = null;
        List<Entry> entries     = new ArrayList<Entry>();
        Request myRequest = new Request(getRepository(), request.getUser());

        int         max         = -1;
        String      orderBy     = null;
        Boolean     orderDir    = null;
        HashSet     nots        = new HashSet();

        for (String entryid : StringUtil.split(ids, ",", true, true)) {
            if (entryid.startsWith("#")) {
                continue;
            }
            if (entryid.startsWith("not:")) {
                nots.add(entryid.substring("not:".length()));

                continue;
            }


            if (entryid.startsWith("entries.max=")) {
                max = Integer.parseInt(
                    entryid.substring("entries.max=".length()));

                continue;
            }
            if (entryid.startsWith("entries.orderby=")) {
                orderBy = entryid.substring("entries.orderby=".length());

                continue;
            }
            if (entryid.startsWith("entries.orderdir=")) {
                orderDir = new Boolean(
                    entryid.substring("entries.orderdir=".length()).equals(
                        "up"));

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
                entries.addAll(applyFilter(request, wikiUtil, tmp, filter,
                                           props));

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
                    entries.addAll(applyFilter(request, wikiUtil,
                            getEntryManager().getEntry(request, id), filter,
                            props));
                }

                continue;
            }


            if (entryid.equals(ID_ROOT)) {
                entries.addAll(applyFilter(request, wikiUtil,
                                           request.getRootEntry(), filter,
                                           props));

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
                entries.addAll(applyFilter(request, wikiUtil, theBaseEntry,
                                           filter, props));

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
                              getProperty(wikiUtil, searchProps,
                                          ARG_AREA_MODE,
                                          VALUE_AREA_CONTAINS));
                myRequest.put(ARG_MAX,
                              getProperty(wikiUtil, searchProps,
                                          PREFIX_SEARCH + ARG_MAX, "100"));
                addSearchTerms(myRequest, wikiUtil, searchProps,
                               theBaseEntry);

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
                entries.addAll(applyFilter(request, wikiUtil, pair[0],
                                           filter, props));
                entries.addAll(applyFilter(request, wikiUtil, pair[1],
                                           filter, props));

                continue;
            }


            if (entryid.equals(ID_PARENT)) {
                entries.addAll(
                    applyFilter(
                        request, wikiUtil,
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
                            entries.addAll(applyFilter(request, wikiUtil,
                                    sibling, filter, props));
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
                        entries.addAll(applyFilter(request, wikiUtil,
                                grandparent, filter, props));
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
                        request, wikiUtil,
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
                    entries.addAll(applyFilter(request, wikiUtil, children,
                            filter, props));
                } else {
                    entries.addAll(applyFilter(request, wikiUtil, entry,
                            filter, props));
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




        if (nots.size() > 0) {
            List<Entry> etmp = new ArrayList<Entry>();
            for (int i = 0; i < entries.size(); i++) {
                if ( !nots.contains(entries.get(i).getId())) {
                    etmp.add(entries.get(i));
                }
            }
            entries = etmp;
        }

        int randomCnt = getProperty(wikiUtil, props, "randomCount", 0);
        if (randomCnt > 0) {
            List<Entry> rtmp = new ArrayList<Entry>();
            while ((randomCnt-- > 0) && (entries.size() > 0)) {
                int idx = (int) (Math.random() * entries.size());
                if (idx < 0) {
                    idx = 0;
                } else if (idx >= entries.size()) {
                    idx = entries.size() - 1;
                }
                Entry e = entries.get(idx);
                rtmp.add(e);
                entries.remove(idx);
            }

            return rtmp;
        }



        if (orderBy == null) {
            orderBy = (String) props.get("sort");
        }

        if (props.get(ATTR_SORT_ORDER) != null) {
            orderDir = new Boolean(props.get(ATTR_SORT_ORDER).equals("down"));
        }

        if (orderDir == null) {
            orderDir = true;
        }

        if (orderBy != null) {
            if (orderBy.equals("date")) {
                entries = getEntryUtil().sortEntriesOnDate(entries, orderDir);
            } else if (orderBy.equals("createdate")) {
                entries = getEntryUtil().sortEntriesOnCreateDate(entries,
                        orderDir);
            } else {
                entries = getEntryUtil().sortEntriesOnName(entries, orderDir);
            }
        }


        if (max > 0) {
            List<Entry> l = new ArrayList<Entry>();
            for (int i = 0; (i < max) && (i < entries.size()); i++) {
                l.add(entries.get(i));
            }
            entries = l;
        }

        return entries;
    }




    /**
     * _more_
     *
     * @param request the request
     * @param wikiUtil _more_
     * @param props _more_
     * @param baseEntry _more_
     *
     * @throws Exception _more_
     */
    private void addSearchTerms(Request request, WikiUtil wikiUtil,
                                Hashtable props, Entry baseEntry)
            throws Exception {
        String[] args = new String[] {
            ARG_TEXT, ARG_TYPE, ARG_GROUP, ARG_FILESUFFIX, ARG_BBOX,
            ARG_BBOX + ".north", ARG_BBOX + ".west", ARG_BBOX + ".south",
            ARG_BBOX + ".east", DateArgument.ARG_DATA.getFrom(), ARG_MAX,
            ARG_ORDERBY, SearchManager.ARG_PROVIDER,
            DateArgument.ARG_DATA.getTo(),
            DateArgument.ARG_DATA.getRelative(),
            DateArgument.ARG_CREATE.getFrom(),
            DateArgument.ARG_CREATE.getTo(),
            DateArgument.ARG_CREATE.getRelative(),
            DateArgument.ARG_CHANGE.getFrom(),
            DateArgument.ARG_CHANGE.getTo(),
            DateArgument.ARG_CHANGE.getRelative(),
        };
        for (String arg : args) {
            String text = getProperty(wikiUtil, props, PREFIX_SEARCH + arg);
            if (text == null) {
                text = getProperty(wikiUtil, props, arg);
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
     * @param wikiUtil _more_
     * @param imageEntries  the list of image entries
     * @param props         the tag properties
     * @param sb            the string buffer to add to
     *
     * @throws Exception  problem making the gallery
     */
    public void makeGallery(Request request, WikiUtil wikiUtil,
                            List<Entry> imageEntries, Hashtable props,
                            StringBuilder sb)
            throws Exception {

        int width = getProperty(wikiUtil, props, ATTR_WIDTH, -100);
        int serverImageWidth = getProperty(wikiUtil, props, ATTR_IMAGEWIDTH,
                                           -1);

        int     columns = getProperty(wikiUtil, props, ATTR_COLUMNS, 3);
        boolean random  = getProperty(wikiUtil, props, ATTR_RANDOM, false);
        boolean popup   = getProperty(wikiUtil, props, ATTR_POPUP, true);
        boolean thumbnail = getProperty(wikiUtil, props, ATTR_THUMBNAIL,
                                        false);
        String caption = getProperty(wikiUtil, props, ATTR_CAPTION,
                                     "${name}");
        String captionPos = getProperty(wikiUtil, props, ATTR_POPUPCAPTION,
                                        "none");
        boolean showDesc = getProperty(wikiUtil, props, ATTR_SHOWDESCRIPTION,
                                       false);
        if (popup) {
            addImagePopupJS(request, wikiUtil, sb, props);
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
        int    num      = 0;
        int    colCnt   = 0;
        String idPrefix = "gallery";

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
                /*                url = HU.url(
                                  request.makeUrl(repository.URL_ENTRY_GET) + "/"
                                  + getStorageManager().getFileTail(child), ARG_ENTRYID,
                                  child.getId());*/
            }
            if (serverImageWidth > 0) {
                url = url + "&" + ARG_IMAGEWIDTH + "=" + serverImageWidth;
            }


            String extra = "";
            if (width > 0) {
                extra = extra + HU.attr(HU.ATTR_WIDTH, "" + width);
            } else if (width < 0) {
                extra = extra + HU.attr(HU.ATTR_WIDTH, "" + (-width) + "%");
            }
            String name       = getEntryDisplayName(child);
            String theCaption = caption;
            theCaption = theCaption.replace("${count}", "" + num);
            theCaption =
                theCaption.replace("${date}",
                                   formatDate(request,
                                       new Date(child.getStartDate())));
            theCaption = theCaption.replace("${name}", child.getLabel());
            theCaption = theCaption.replace("${description}",
                                            child.getDescription());


            if ((name != null) && !name.isEmpty()) {
                extra = extra + HU.attr(HU.ATTR_ALT, name);
            }
            extra = extra + HU.attr("id", idPrefix + "img" + num);
            String img = HU.img(url, "", extra);

            String entryUrl =
                request.entryUrl(getRepository().URL_ENTRY_SHOW, child);
            buff.append("<div class=\"image-outer\">");
            buff.append("<div class=\"image-inner\">");
            if (popup) {
                String popupExtras = HU.cssClass("popup_image")
                                     + HU.attr("width", "100%");
                if ( !captionPos.equals("none")) {
                    popupExtras += HU.attr("title", theCaption);
                }
                popupExtras += HU.attr("data-fancybox", idPrefix)
                               + HU.attr("data-caption", theCaption);
                buff.append(
                    HU.href(
                        child.getTypeHandler().getEntryResourceUrl(
                            request, child), HU.div(
                            img,
                            HU.attr(
                                "id", idPrefix + "div" + num)), popupExtras));
            } else {
                buff.append(img);
            }
            buff.append("</div>");


            theCaption = HU.href(entryUrl, theCaption,
                                 HU.style("color:#666;font-size:10pt;"));

            buff.append(HU.div(theCaption, HU.cssClass("image-caption")));
            if (showDesc) {
                if (Utils.stringDefined(child.getDescription())) {
                    buff.append("<div class=\"image-description\">");
                    buff.append(child.getDescription());
                    buff.append("</div>");
                }
            }

            buff.append("</div>");
        }
        int    colInt   = 12 / Math.min(12, columns);
        String colClass = "col-md-" + colInt;
        HU.open(sb, "div", HU.cssClass("row"));
        for (StringBuilder buff : colsSB) {
            HU.open(sb, "div",
                    HU.cssClass(colClass + " ramadda-col")
                    + HU.style("padding-left:5px; padding-right:5px;"));
            sb.append(buff);
            HU.close(sb, "div");
        }
        HU.close(sb, "div");

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
		    //                    return "<b>Error: Circular import</b>";
                }
                tag = getProperty(wikiUtil, props, ATTR_OUTPUT,
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
            String title = getProperty(wikiUtil, props, ATTR_TITLE,
                                       result.getTitle());

            boolean inBlock = getProperty(wikiUtil, props, ATTR_SHOWTOGGLE,
                                          getProperty(wikiUtil, props,
                                              ATTR_SHOWHIDE, false));
            if (inBlock && (title != null)) {
                boolean open = getProperty(wikiUtil, props, ATTR_OPEN, true);

                return HU.makeShowHideBlock(
                    title, content, open, HU.cssClass(CSS_CLASS_HEADING_2),
                    "", getIconUrl("ramadda.icon.togglearrowdown"),
                    getIconUrl("ramadda.icon.togglearrowright"));
            }

            return content;
        } catch (Exception exc) {
            StringBuilder msg =
                new StringBuilder("Error processing tag:<br> {{" + tag);
            for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
                Object key   = keys.nextElement();
                Object value = props.get(key);
                msg.append(" " + key + "=\"" + value + "\" ");
            }
            msg.append("}}");

            return getRepository().handleError(request, exc, msg.toString());

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
        if (request.get("doImports", true)) {
            addDisplayImports(request, buttons);
        }
        StringBuilder tags  = new StringBuilder();
        StringBuilder tags2 = new StringBuilder();
        Utils.appendAll(
            tags, getWikiEditLink(
                textAreaId, "Section", "+section title={{name}}_newline__newline_", "-section", ""), getWikiEditLink(
                textAreaId, "Frame", "+frame background=#fff frameSize=0 shadow title=_title_", "-frame", ""), getWikiEditLink(
                textAreaId, "Note", "+note_newline__newline_", "-note", ""), getWikiEditLink(
                textAreaId, "Table", "+table height=400 hover=true cellborder=false rowborder=false stripe=false ordering=false paging=false searching=false_newline_:tr &quot;heading 1&quot; &quot;heading 2&quot;_newline_+tr_newline_:td colum 1_newline_+td_newline_column 2_newline_", "-td_newline_-tr_newline_-table", ""),
	    getWikiEditLink(textAreaId, "Row/Column", "+row_newline_+col-6_newline_", "-col_newline_+col-6_newline_-col_newline_-row", ""),
	    getWikiEditLink(textAreaId, "Tabs", "+tabs_newline_+tab tab title_newline_", "-tab_newline_-tabs_newline_", ""),
	    getWikiEditLink(textAreaId, "Accordion", "+accordion decorate=true collapsible=true activeSegment=0 _newline_+segment segment  title_newline_", "-segment_newline_-accordion_newline_", ""),
	    getWikiEditLink(textAreaId, "Slides", "+slides dots=true slidesToShow=1  bigArrow=true style=_qt__qt__nl_+slide Title_nl_", "-slide_nl_-slides_nl_", ""),



	    getWikiEditLink(textAreaId, "Menu", "+menu_nl_    :menuheader Header_nl_    :menuitem Item 1_nl_    +menu Menu 1_nl_        :menuitem Item 2_nl_        +menuitem style=_qt_width:300px; background:green;_qt_ _nl_        Menu contents_nl_        -menuitem_nl_    -menu_nl_    +menu Menu 2_nl_        :menuitem Item 3_nl_    -menu_nl_-menu", "", ""),
	    getWikiEditLink(textAreaId, "Navigation left", ":navleft leftStyle=_qt_width:250px;_qt_ rightStyle=_qt__qt_  maxLevel=_qt_4_qt_", "", ""),
	    getWikiEditLink(textAreaId, "Navigation top", ":navtop style=_quote__quote_ delimiter=_quote_|_quote_  maxLevel=_qt__qt_", "", ""),
	    getWikiEditLink(textAreaId, "Navigation popup", ":navpopup align=right|left  maxLevel=_qt__qt_", "", ""),	    

	    getWikiEditLink(textAreaId, "Prev arrow", "{{prev position=relative|fixed decorate=false iconSize=32 sort=name,entryorder sortAscending=true style=_qt_left:250px;_qt_  showName=false}}", "", ""),
	    getWikiEditLink(textAreaId, "Next arrow", "{{next position=relative|fixed decorate=false iconSize=32 sort=name,entryorder sortAscending=true style=_dq_  showName=false}}", "", ""), getWikiEditLink(textAreaId, "Title", ":title {{name link=true}}", "", ""), getWikiEditLink(textAreaId, "Heading", ":heading your heading", "", ""), getWikiEditLink(textAreaId, "Heading-1", ":h1 your heading", "", ""), getWikiEditLink(textAreaId, "Heading-2", ":h2 your heading", "", ""), getWikiEditLink(textAreaId, "Heading-3", ":h3 your heading", "", ""));

        Utils.appendAll(tags2,
                getWikiEditLink(textAreaId, "Draggable",
                    "+draggable framed=true header=_quote__quote_ style=_quote_background:#fff;_quote_ toggle=_quote_true_quote_ toggleVisible=_quote_true_quote__newline_",
                        "-draggable", ""), getWikiEditLink(textAreaId,
                            "Expandable",
                                "+expandable header=_quote_quote_ expand=true_newline_",
                                    "-expandable",
                                        ""), getWikiEditLink(textAreaId,
                                            "Grid box",
                                                "+gridboxes-2_newline_+gridbox Title 1_newline_-gridbox_newline_+gridbox Title 2_newline_-gridbox_newline_",
                                                    "-gridboxes",
                                                        ""), getWikiEditLink(textAreaId,
                                                            "Scroll panels",
                                                                "+scroll_newline_+panel color=gradient1 name=home style=_quote__quote_ _newline_+center_newline_<div class=scroll-indicator>Scroll Down</div>_newline_-center_newline_-panel_newline_+panel color=gradient2 name=panel1_newline__newline_-panel_newline_+panel color=blue name=panel2_newline__newline_-panel_newline_", "-scroll", ""), getWikiEditLink(textAreaId, "Inset", "+inset top=0 bottom=0 left=0 right=0 _newline_", "-inset", ""), getWikiEditLink(textAreaId, "Absolute", "\\n+absolute top= bottom= left= right=\\n-absolute", "", ""), getWikiEditLink(textAreaId, "Relative", "\\n+relative\\n-relative", "", ""), getWikiEditLink(textAreaId, "Center", "\\n+center\\n-center", "", ""), getWikiEditLink(textAreaId, "Div", "+div class=_quote__quote_ style=_quote__quote_ background=_quote__quote_ _newline_", "-div", ""), getWikiEditLink(textAreaId, "CSS", "+css_newline_", "-css", ""), getWikiEditLink(textAreaId, "PRE", "+pre_newline_", "-pre", ""), getWikiEditLink(textAreaId, "Javascript", "+js_newline_", "-js", ""), getWikiEditLink(textAreaId, "Code", "```_newline__newline_", "```", ""));

        StringBuilder tags3 = new StringBuilder();
        StringBuilder tags4 = new StringBuilder();
        StringBuilder tags5 = new StringBuilder();

        Utils.appendAll(tags3,
                        getWikiEditLink(textAreaId, "Note", "+note_nl__nl_",
                                        "-note", ""));
        String[] colors = new String[] {
            "plain", "gray", "platinum", "yellow", 
            "azure",  "bone",
            "green",  "cambridgeblue"
        };
        for (String color : colors) {
            tags3.append(
                getWikiEditLink(
                    textAreaId, HU.div(
                        "Note "
                        + color, HU.attrs(
                            "style", "padding:2px; display:inline-block;", "class", "ramadda-background-"
                            + color)), "+note-" + color
                                       + "_nl__nl_", "-note", ""));
        }

        Utils.appendAll(tags4,
                        getWikiEditLink(textAreaId, "Box", "+box_nl__nl_",
                                        "-box", ""));
        for (String color : colors) {
            tags4.append(
                getWikiEditLink(
                    textAreaId, HU.div(
                        "Box "
                        + color, HU.attrs(
                            "style", "padding:2px; display:inline-block;", "class", "ramadda-background-"
                            + color)), "+box-" + color
                                       + "_nl__nl_", "-box", ""));
        }


        List<Link> links = getRepository().getOutputLinks(request,
                               new OutputHandler.State(entry));
        for (Link link : links) {
            if (link.getOutputType() == null) {
                continue;
            }

            String prop = link.getOutputType().getId();
            String js = "javascript:insertTags(" + HU.squote(textAreaId)
                        + "," + HU.squote("{{") + "," + HU.squote("}}") + ","
                        + HU.squote(prop) + ");";
        }

        List<String[]> fromType     = (entry == null)
                                      ? null
                                      : entry.getTypeHandler()
                                          .getWikiEditLinks();
        Appendable     fromTypeBuff = null;
        if ((fromType != null) && (fromType.size() > 0)) {
            fromTypeBuff = new StringBuilder();
            for (String[] pair : fromType) {
                String js = "javascript:insertTags(" + HU.squote(textAreaId)
                            + "," + HU.squote(pair[1]) + ",'');";
                fromTypeBuff.append(HU.href(js, pair[0]));
                fromTypeBuff.append("<br>");
            }
        }

        String        buttonClass = HU.clazz("ramadda-menubar-button");
        StringBuilder help        = new StringBuilder();

	BiFunction<String,String,String> makeButton = (title,contents)->{
	    return HU.makePopupLink(null,HU.div(title,HU.cssClass("ramadda-menubar-button")),
                                                  HU.div(contents, "class='wiki-editor-popup'"),
						  new NamedValue("linkAttributes", buttonClass));
	};
	BiConsumer<String,String> makeHelp = (p,title)->{
	    help.append(HU.href(getRepository().getUrlBase()
				+ p, title,
				"target=_help") + "<br>");
	};

        for (String extraHelp :
                StringUtil.split(request.getString("extrahelp", ""), ",",
                                 true, true)) {
            List<String> toks = StringUtil.splitUpTo(extraHelp, "|", 2);
            if (toks.size() == 2) {
                help.append(HU.href(Utils.encodeUntrustedText(toks.get(0)),
                                    Utils.encodeUntrustedText(toks.get(1)),
                                    "target=_help") + "<br>");
            }
        }

        makeHelp.accept("/userguide/wikitext.html", "Wiki text");
        makeHelp.accept("/userguide/wikidisplay.html", "Displays and Charts");
        makeHelp.accept("/userguide/wikitext.html#sections", "Sections");
        makeHelp.accept("/userguide/wikitext.html#gridlayout", "Grid layout");
        makeHelp.accept("/userguide/wikitext.html#entry",
                        "Specifying the entry");
        makeHelp.accept("/userguide/wikitext.html#entries",
                        "Specifying multiple entries");
        makeHelp.accept("/search/providers", "Search Providers");
        makeHelp.accept("/search/info#entrytypes", "Entry Types");
        makeHelp.accept("/search/info#metadatatypes", "Metadata Types");
        makeHelp.accept("/colortables", "Color Tables");
        String helpButton = makeButton.apply("Help", help.toString());
        String formattingButton = makeButton.apply("Formatting",
                                      HU.hbox(tags, tags2, tags3, tags4));

        StringBuilder text = new StringBuilder();
        Utils.appendAll(
            text, getWikiEditLink(
                textAreaId, "Break", "\\n:br", "", ""), getWikiEditLink(
                textAreaId, "Paragraph", "\\n:p", "", ""), getWikiEditLink(
                textAreaId, "Bold text", "\\'\\'\\'", "\\'\\'\\'", ""), getWikiEditLink(
                textAreaId, "Italic text", "\\'\\'", "\\'\\'", ""), getWikiEditLink(
                textAreaId, "Code", "```\\n", "\\n```", ""), getWikiEditLink(
                textAreaId, "Internal link", "[[", "]]", "Link title"), getWikiEditLink(
                textAreaId, "External link", "[", "]", "http://www.example.com link title"), getWikiEditLink(
                textAreaId, "Level 2 headline", "\\n== ", " ==\\n", "Headline text"), getWikiEditLink(
                textAreaId, "Small text", "<small>", "</small>", "Small text"), getWikiEditLink(
                textAreaId, "Horizontal line", "\\n----\\n", "", ""), getWikiEditLink(
                textAreaId, "Button", ":button url label", "", ""), getWikiEditLink(
                textAreaId, "Remark", "\\n:rem ", "", ""), getWikiEditLink(
                textAreaId, "Reload", "\\n:reload seconds=30 showCheckbox=true showLabel=true", "", ""), getWikiEditLink(
                textAreaId, "After", "+after pause=0 afterFade=5000_newline__newline_", "-after", "After"), getWikiEditLink(
                textAreaId, "Odometer", "{{odometer initCount=0 count=100 immediate=true pause=1000}}", "", ""));


        String textButton = makeButton.apply("Text", text.toString());
        String entriesButton = makeButton.apply("Entries",
                                   makeTagsMenu(textAreaId));
        String displaysButton = HU.href(
                                    "javascript:noop()", "Displays",
                                    HU.attrs(
                                        "id", "displays_button" + textAreaId,
                                        "class",
                                        "ramadda-menubar-button")) + HU
                                            .script(
                                                "wikiInitDisplaysButton('"
                                                + textAreaId + "')");

        String addEntry = OutputHandler.getSelect(request, textAreaId,
                              "Entry ID", true, "entryid", entry, false,
                              buttonClass);

        String addLink = OutputHandler.getSelect(request, textAreaId,
                             "Entry link", true, "wikilink", entry, false,
                             buttonClass);

        String fieldLink = OutputHandler.getSelect(request, textAreaId,
                               "Field name", true, "fieldname", entry, false,
                               buttonClass);

        HU.open(buttons, "div",
                HU.cssClass("ramadda-menubar")
                + HU.attrs("id", textAreaId + "_toolbar"));
        Utils.appendAll(buttons, HU.span("", HU.id(textAreaId + "_prefix")),
                        formattingButton, textButton, entriesButton);
        if (fromTypeBuff != null) {
	    buttons.append(HU.makePopupLink(null,entry.getTypeHandler().getLabel() + " tags",
							  HU.div(fromTypeBuff.toString(), "class='wiki-editor-popup'"),
							  new NamedValue("linkAttributes", buttonClass)));
        }

        Utils.appendAll(buttons, displaysButton, addEntry, addLink,
                        fieldLink);

        if (entry != null) {
            entry.getTypeHandler().addToWikiToolbar(request, entry, buttons,
                    textAreaId);
        }

        buttons.append(helpButton);
        String previewCall = HU.call("wikiPreview", ((entry != null)
                ? HU.squote(entry.getId())
                : "null"), HU.squote(textAreaId));
        String previewButton =
            HU.href("#", "Preview",
                HU.attrs("onclick", previewCall, "id",
                    "wikieditpreviewbutton ", "style", "padding:5px;",
                    "class",
                    " ramadda-menubar-button ramadda-menubar-button-last")) + HU.div("",
                        HU.attrs("id", "wikieditpreview", "style",
                            "display:none;height:800px;overflow-y:auto;position:absolute;left:100px;top:10px;"));

        buttons.append(previewButton);
        HU.close(buttons, "div");

        return buttons.toString();
    }


    /**
     * _more_
     *
     * @param charts _more_
     * @param sb _more_
     * @param textAreaId _more_
     *
     * @return _more_
     */
    public String makeTagsMenu(String textAreaId) {
        StringBuilder sb     = new StringBuilder();
        String        inset  = "&nbsp;&nbsp;";
        int           rowCnt = 0;
        sb.append("<table border=0><tr valign=top><td valign=top>\n");
        for (int i = 0; i < WIKITAGS.length; i++) {
            WikiTagCategory cat = WIKITAGS[i];
            if (rowCnt + cat.tags.length > 10) {
                rowCnt = 0;
                if (i > 0) {
                    sb.append("</td><td>&nbsp;</td><td valign=top>\n");
                }
            }
            sb.append("\n");

            sb.append(HU.b(cat.category));
            sb.append(HU.br());
            rowCnt += cat.tags.length;
            for (int tagIdx = 0; tagIdx < cat.tags.length; tagIdx++) {
                WikiTag tag          = cat.tags[tagIdx];
                String  textToInsert = tag.tag;
                if (tag.attrs.length() > 0) {
                    textToInsert += " " + tag.attrs;
                }

                String js2 = "javascript:insertTags(" + HU.squote(textAreaId)
                             + "," + HU.squote("{{" + textToInsert + " ")
                             + "," + HU.squote("}}") + "," + HU.squote("")
                             + ");";
                sb.append(inset);
                sb.append(HU.href(js2, tag.label));
                sb.append(HU.br());
                sb.append("\n");
            }
            sb.append(HU.br());
        }
        sb.append("</td></tr></table>\n");

        return sb.toString();

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
    private String getWikiEditLink(String textAreaId, String label,
                                   String prefix, String suffix,
                                   String example) {
        String js;
        if (suffix.length() == 0) {
            String prop = prefix + example + suffix;
            js = "javascript:insertText(" + HU.squote(textAreaId) + ","
                 + HU.squote(prop) + ");";
        } else {
            js = "javascript:insertTags(" + HU.squote(textAreaId) + ","
                 + HU.squote(prefix) + "," + HU.squote(suffix) + ","
                 + HU.squote(example) + ");";
        }

        return HU.href(js, label) + "<br>";
    }




    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param src _more_
     * @param props _more_
     *
     * @return _more_
     */
    public String getWikiImageUrl(WikiUtil wikiUtil, String src,
                                  Hashtable props) {
        try {
            Entry   entry      = (Entry) wikiUtil.getProperty(ATTR_ENTRY);
            Request request    = (Request) wikiUtil.getProperty(ATTR_REQUEST);
            Entry   srcEntry   = null;
            String  attachment = null;

            int     idx        = src.indexOf("::");
            if (idx >= 0) {
                List<String> toks = StringUtil.splitUpTo(src, "::", 2);
                if (toks.size() == 2) {
                    src        = toks.get(0);
                    attachment = toks.get(1).substring(1);
                }
            }
            if ((src.length() == 0) || entry.getName().equals(src)) {
                srcEntry = entry;
            } else if (entry instanceof Entry) {
                srcEntry = getEntryManager().findEntryWithName(request,
                        (Entry) entry, src);
            }
            if (srcEntry == null) {
                return null;
            }
            if (attachment == null) {
                if ( !srcEntry.isImage()) {
                    return null;
                }

                return getHtmlOutputHandler().getImageUrl(request, srcEntry);
            }
            if ((attachment != null) && attachment.equals("*")) {
                attachment = null;
            }
            for (Metadata metadata :
                    getMetadataManager().getMetadata(srcEntry)) {
                MetadataType metadataType =
                    getMetadataManager().findType(metadata.getType());
                if (metadataType == null) {
                    continue;
                }
                String url = metadataType.getDisplayImageUrl(request,
                                 srcEntry, metadata, attachment);
                if (url != null) {
                    return url;
                }
            }

            return null;
        } catch (Exception exc) {
            throw new IllegalArgumentException(exc);
        }
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



            boolean doUrl = name.startsWith("url:");
            if (doUrl) {
                name = name.substring("url:".length());
            }

            if (name.startsWith("Category:")) {
                String category = name.substring("Category:".length());
                String url =
                    request.makeUrl(
                        getRepository().getSearchManager().URL_ENTRY_SEARCH,
                        ARG_METADATA_TYPE + ".wikicategory", "wikicategory",
                        ARG_METADATA_ATTR1 + ".wikicategory", category);
                wikiUtil.addCategoryLink(HU.href(url, category));
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

            if ((theEntry == null) && doUrl) {
                return "/";
            }

            if (theEntry != null) {
                if (doUrl) {
                    return getEntryManager().getEntryUrl(request, theEntry);
                }
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
                        url = HU.url(
                            request.entryUrl(
                                getRepository().URL_ENTRY_SHOW,
                                theEntry), ARG_OUTPUT, outputType);
                    }

                    return getEntryManager().getTooltipLink(request,
                            theEntry, label, url);
                }
            }


            System.err.println("missing:" + name);
            //If its an anonymous user then jusst show the label or the name
            if (request.isAnonymous()) {
                String extra = HU.cssClass("wiki-link-noexist");
                if ((label != null) && (label.length() > 0)) {
                    return HU.span(label, extra);
                }

                return HU.span(name, extra);
            }

            String url = request.makeUrl(getRepository().URL_ENTRY_FORM,
                                         ARG_NAME, name, ARG_GROUP,
                                         (entry.isGroup()
                                          ? entry.getId()
                                          : parent.getId()), ARG_TYPE,
                                              TYPE_WIKIPAGE);

            return HU.href(url, name, HU.cssClass("wiki-link-noexist"));
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
        return wikifyEntry(request, entry, wikiContent, wrapInDiv, subGroups,
                           subEntries, null);
    }


    /**
     * _more_
     *
     * @param request the request
     * @param entry _more_
     * @param wikiContent _more_
     * @param wrapInDiv _more_
     * @param subGroups _more_
     * @param subEntries _more_
     * @param notTags _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String wikifyEntry(Request request, Entry entry,
                              String wikiContent, boolean wrapInDiv,
                              List<Entry> subGroups, List<Entry> subEntries,
                              HashSet notTags)
            throws Exception {

        Request myRequest = request.cloneMe();
        WikiUtil wikiUtil =
            initWikiUtil(myRequest,
                         new WikiUtil(Misc.newHashtable(new Object[] {
                             ATTR_REQUEST,
                             myRequest, ATTR_ENTRY, entry })), entry);

        return wikifyEntry(request, entry, wikiUtil, wikiContent, wrapInDiv,
                           subGroups, subEntries, notTags, true);
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
     * @param notTags _more_
     * @param includeJavascript _more_
     *
     * @return the wikified Entry
     *
     * @throws Exception  problem wikifying
     */
    public String wikifyEntry(Request request, Entry entry,
                              WikiUtil wikiUtil, String wikiContent,
                              boolean wrapInDiv, List<Entry> subGroups,
                              List<Entry> subEntries, HashSet notTags,
                              boolean includeJavascript)
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
        if (wrapInDiv) {
            content = HU.div(content, HU.cssClass("wikicontent")) + "\n";
        }
        if (includeJavascript) {
            String js = wikiUtil.getJavascript();
            if (js.length() > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append(content);
                sb.append(HU.script(js));
                content = sb.toString();
            }
        }

        return content;
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
     * @param request the request
     * @param wikiUtil _more_
     * @param props _more_
     * @param originalEntry _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getDescription(Request request, WikiUtil wikiUtil,
                                  Hashtable props, Entry originalEntry,
                                  Entry entry)
            throws Exception {
        String  content;
        boolean wikify = getProperty(wikiUtil, props, ATTR_WIKIFY, false);
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
     * @param request the request
     * @param wikiUtil _more_
     * @param props _more_
     * @param originalEntry _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String makeSimpleDisplay(Request request, WikiUtil wikiUtil,
                                    Hashtable props, Entry originalEntry,
                                    Entry entry)
            throws Exception {


        String fromType = entry.getTypeHandler().getSimpleDisplay(request,
                              props, entry);
        if (fromType != null) {
            return fromType;
        }


        boolean sizeConstrained = getProperty(wikiUtil, props,
                                      ATTR_CONSTRAINSIZE, false);
        String content = getDescription(request, wikiUtil, props,
                                        originalEntry, entry);
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
            String imageWidth = null;
            String divStyle   = "";

            if (sizeConstrained) {
                imageWidth = getProperty(wikiUtil, props, ATTR_WIDTH, "400");
            }

            imageWidth = getProperty(wikiUtil, props, ATTR_IMAGEWIDTH,
                                     imageWidth);

            extra.append(HU.attr(HU.ATTR_WIDTH, "95%"));

            if (imageWidth != null) {
                if (imageWidth.startsWith("-")) {
                    imageWidth = imageWidth.substring(1) + "%";
                }
                //                divStyle +="width:" + imageWidth+";";
            }

            String alt = request.getString(ATTR_ALT,
                                           getEntryDisplayName(entry));
            String imageClass = request.getString("imageclass",
                                    (String) null);
            if (Utils.stringDefined(alt)) {
                extra.append(HU.attr(ATTR_ALT, alt));
            }
            String image = HU.img(imageUrl, "", extra.toString());
            if (request.get(WikiManager.ATTR_LINK, true)) {
                image =
                    HU.href(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                             entry), image);
                /*  Maybe add this later
                    } else if (request.get(WikiManager.ATTR_LINKRESOURCE, false)) {
                    image =  HU.href(
                    entry.getTypeHandler().getEntryResourceUrl(request, entry),
                    image);
                */
            }

            if (haveText && sizeConstrained) {
                int height = getProperty(wikiUtil, props, ATTR_HEIGHT, -1);
                if ((height > 0) && position.equals(POS_BOTTOM)) {
                    divStyle += "overflow-y: auto; max-height:"
                                + (height - 75) + "px;";
                }
            }

            image = HU.div(image,
                           HU.cssClass("entry-simple-image")
                           + HU.style(divStyle));
            if ( !haveText) {
                return image;
            }

            String textClass = "entry-simple-text "
                               + getProperty(wikiUtil, props, "textClass",
                                             "note");
            String textStyle = getProperty(wikiUtil, props, "textStyle",
                                           "margin:8px;");
            String textExtra = HU.cssClass(textClass);
            if (position.equals(POS_NONE)) {
                content = image;
            } else if (position.equals(POS_BOTTOM)) {
                content = image
                          + HU.div(content, HU.style(textStyle) + textExtra);
            } else if (position.equals(POS_TOP)) {
                content = HU.div(content, HU.style(textStyle) + textExtra)
                          + image;
            } else if (position.equals(POS_RIGHT)) {
                content = HU
                    .table(HU
                        .row(HU.col(image)
                             + HU
                             .col(HU.div(
                                 content,
                                 HU.style(
                                     "width:"
                                     + getProperty(
                                         wikiUtil, props, "textWidth",
                                         "150px;") + textStyle) + HU
                                             .cssClass(textClass))), HU
                                                 .attr(HU.ATTR_VALIGN,
                                                     "top")), HU
                                                         .attr(HU
                                                             .ATTR_CELLPADDING, "0"));
            } else if (position.equals(POS_LEFT)) {
                content =
                    HU.table(
                        HU.row(HU.col(
                            HU.div(content,
                                   HU.style(
                                       "width:"
                                       + getProperty(
                                           wikiUtil, props, "textWidth",
                                           "150px;") + textStyle) + HU.cssClass(
                                               textClass))) + HU.col(
                                                   image), HU.attr(
                                                       HU.ATTR_VALIGN,
                                                           "top")), HU.attr(
                                                               HU.ATTR_CELLPADDING,
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
        content = HU.div(content, HU.cssClass("entry-simple"));

        return content;

    }

    /**
     * _more_
     *
     * @param request the request
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getStandardChartDisplay(Request request, Entry entry)
            throws Exception {
        TypeHandler       typeHandler       = entry.getTypeHandler();
        RecordTypeHandler recordTypeHandler = null;
        if (typeHandler instanceof RecordTypeHandler) {
            recordTypeHandler = (RecordTypeHandler) typeHandler;
        }

        String        name = entry.getName();
        StringBuilder wiki = new StringBuilder();

        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry,
                "point_chart_wiki", true);
        if ((metadataList != null) && (metadataList.size() > 0)) {
            wiki.append(metadataList.get(0).getAttr1());
        } else {
            String fromEntry = typeHandler.getProperty(entry, "chart.wiki",
                                   null);
            if (fromEntry != null) {
                wiki.append(fromEntry);
            } else {
                wiki.append(
                    "{{group  howMenu=\"true\"  layoutType=\"columns\"  layoutColumns=\"2\"  }}\n");
                String chartType = (recordTypeHandler == null)
                                   ? typeHandler.getProperty(entry,
                                       "chart.type", "linechart")
                                   : recordTypeHandler.getChartProperty(
                                       request, entry, "chart.type",
                                       "linechart");
                wiki.append(
                    "{{display  xwidth=\"600\"  height=\"400\"   type=\""
                    + chartType
                    + "\"  name=\"\"  layoutHere=\"false\"  showMenu=\"false\"  showTitle=\"false\"  row=\"0\"  column=\"0\"  }}");
                if (entry.isGeoreferenced()) {
                    String mapLayers = getMapManager().getMapLayers();
                    String layerVar  = "";
                    if (mapLayers != null) {
                        mapLayers = mapLayers.replaceAll(";", ",");
                        layerVar  = "mapLayers=\"" + mapLayers + "\"";
                    }
                    String entryAttrs = (recordTypeHandler == null)
                                        ? typeHandler.getProperty(entry,
                                            "chart.wiki.map", "")
                                        : recordTypeHandler.getChartProperty(
                                            request, entry, "chart.wiki.map",
                                            "");

                    if (typeHandler.getTypeProperty("isTrajectory", false)
                            || typeHandler.getProperty(entry, "isTrajectory",
                                false)) {
                        entryAttrs += " isTrajectory=\"true\" ";
                    }
                    wiki.append(
                        "{{display  width=\"600\"  height=\"400\"   type=\"map\" "
                        + layerVar + entryAttrs
                        + " name=\"\"  layoutHere=\"false\"  showMenu=\"true\"  showTitle=\"true\"  row=\"0\"  column=\"1\"  }}");
                }
            }
        }

        return wikifyEntry(request, entry, wiki.toString());
    }


    /**
     * _more_
     *
     * @param request the request
     * @param wikiUtil _more_
     * @param entry _more_
     * @param originalEntry _more_
     * @param tag _more_
     * @param name _more_
     * @param pointDataUrl _more_
     * @param sb _more_
     * @param props _more_
     * @param propList _more_
     *
     * @throws Exception _more_
     */
    private void getEntryDisplay(Request request, WikiUtil wikiUtil,
                                 Entry entry, Entry originalEntry,
                                 String tag, String name,
                                 String pointDataUrl, StringBuilder sb,
                                 Hashtable props, List<String> propList)
            throws Exception {

        String displayType = getProperty(wikiUtil, props, "type",
                                         "linechart");
        this.addDisplayImports(request, sb);
        if (request.getExtraProperty("added plotly") == null) {
            HU.importJS(sb, getHtdocsUrl("/lib/plotly/plotly-latest.min.js"));
            request.putExtraProperty("added plotly", "true");
        }
        List<String> topProps = new ArrayList<String>();
        if (propList == null) {
            propList = new ArrayList<String>();
        }

        List<String> tmpProps = new ArrayList<String>();
        for (int i = 0; i < propList.size(); i += 2) {
            if (props.get(propList.get(i)) == null) {
                tmpProps.add(propList.get(i));
                tmpProps.add(propList.get(i + 1));
            }
        }
        propList = tmpProps;

        List<Metadata> metadataAttrs =
            getMetadataManager().findMetadata(request, entry,
                "wikiattribute", true);
        if (metadataAttrs != null) {
            for (Metadata metadata : metadataAttrs) {
                String attrName = metadata.getAttr1();
                if (props.get(attrName) == null) {
                    propList.add(attrName);
                    propList.add(Json.quote(metadata.getAttr2()));
                }
            }
        }

        StringBuilder js = new StringBuilder();


        for (String showArg : new String[] { ATTR_SHOWMAP, ATTR_SHOWMENU }) {
            String v = (String) getProperty(wikiUtil, props, showArg,
                                            (String) null);
            if (v != null) {
                topProps.add(showArg);
                topProps.add(v);
            }
        }

        String derived = getProperty(wikiUtil, props, "derived");
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

        String providers = getProperty(wikiUtil, props, "providers");
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
                            String icon = provider.getSearchProviderIconUrl();
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
                String icon = searchProvider.getSearchProviderIconUrl();
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

        String entryParent = getProperty(wikiUtil, props, "entryParent");
        if (entryParent != null) {
            Entry theEntry = findEntryFromId(request, entry, null,
                                             entryParent);
            if (theEntry != null) {
                props.put("entryParent", theEntry.getId());
            }
        }


        if ( !request.isAnonymous()) {
            props.put("user", request.getUser().getId());
        }

        String colors = getProperty(wikiUtil, props, ATTR_COLORS);
        if (colors != null) {
            propList.add(ATTR_COLORS);
            propList.add(Json.list(StringUtil.split(colors, ","), true));
            props.remove(ATTR_COLORS);
        }

        boolean showTitle = false;
        if (getProperty(wikiUtil, props, ATTR_SHOWTITLE) != null) {
            propList.add(ATTR_SHOWTITLE);
            propList.add(getProperty(wikiUtil, props, ATTR_SHOWTITLE,
                                     "true"));
            showTitle = Misc.equals("true",
                                    getProperty(wikiUtil, props,
                                        ATTR_SHOWTITLE, "true"));
            topProps.add(ATTR_SHOWTITLE);
            topProps.add(getProperty(wikiUtil, props, ATTR_SHOWTITLE,
                                     "true"));
            props.remove(ATTR_SHOWTITLE);
        }




        if (entry != null) {
            propList.add("entryIcon");
            propList.add(
                Json.quote(
                    entry.getTypeHandler().getEntryIconUrl(
                        request, originalEntry)));
        }



        String timezone = getEntryUtil().getTimezone(entry);
        if (timezone != null) {
            propList.add("timezone");
            TimeZone tz = TimeZone.getTimeZone(timezone);
            propList.add(Json.quote("" + (tz.getRawOffset() / 1000 / 60
                                          / 60)));
        }

        String title = getProperty(wikiUtil, props, ATTR_TITLE,
                                   (String) null);
        String titleId = getProperty(wikiUtil, props, "titleId",
                                     (String) null);
        if (titleId != null) {
            title = getWikiMetadataLabel(request, entry, titleId, title);
        }
        props.remove(ATTR_TITLE);
        if (title != null) {
            title = title.replace("{entry}", entry.getName());
            propList.add(ATTR_TITLE);
            propList.add(Json.quote(title));
        } else {
            propList.add(ATTR_TITLE);
            propList.add(Json.quote(entry.getName()));
        }


        String changeEntries = getProperty(wikiUtil, props, "changeEntries");
        if ((changeEntries != null) && changeEntries.equals("false")) {
            changeEntries = null;
        }
        if (changeEntries != null) {
            List<Entry> children;
            if (changeEntries.equals("true")) {
                children = getEntries(request, wikiUtil, originalEntry,
                                      entry, props);
            } else {
                children = getEntries(request, wikiUtil, entry,
                                      changeEntries, props);
            }
            StringBuilder tmp = new StringBuilder();
            for (Entry child : children) {
                if (tmp.length() > 0) {
                    tmp.append(",");
                }
                tmp.append(child.getId() + ":"
                           + child.getName().replaceAll(",", " "));
            }
            propList.add("entryCollection");
            propList.add(Json.quote(tmp.toString()));
            String tmpname = getProperty(wikiUtil, props,
                                         "changeEntriesLabel");
            if (tmpname != null) {
                propList.add("changeEntriesLabel");
                propList.add(Json.quote(tmpname));
            }
        }

        topProps.add("layoutType");
        topProps.add(Json.quote(getProperty(wikiUtil, props, "layoutType",
                                            "table")));
        props.remove("layoutType");
        topProps.add("layoutColumns");
        topProps.add(getProperty(wikiUtil, props, "layoutColumns", "1"));
        props.remove("layoutColumns");

        //Always add the default map layer to the displaymanager properties so any new maps pick it up
        String defaultLayer =
            getProperty(wikiUtil, props, "defaultMapLayer",
                        getMapManager().getDefaultMapLayer());

        String bounds = (String) props.get("bounds");
        if (bounds != null) {
            props.remove("bounds");
            Utils.add(propList, "bounds", Json.quote(bounds));
        } else if (entry.hasAreaDefined()) {
            Utils.add(propList, "bounds",
                      Json.quote(entry.getNorth() + "," + entry.getWest()
                                 + "," + entry.getSouth() + ","
                                 + entry.getEast()));
        }

        topProps.add("defaultMapLayer");
        topProps.add(Json.quote(defaultLayer));

        String displayDiv = getProperty(wikiUtil, props, "displayDiv");
        if (displayDiv != null) {
            displayDiv = displayDiv.replace("${entryid}", entry.getId());
            Utils.add(propList, "displayDiv", Json.quote(displayDiv));
            props.remove("displayDiv");
        }

        topProps.add("entryId");
        topProps.add(HU.quote(entry.getId()));


        String mainDivId = getProperty(wikiUtil, props, "divid");
        if (mainDivId == null) {
            mainDivId = HU.getUniqueId("displaydiv");
        }
        mainDivId = mainDivId.replace("$entryid", entry.getId());
        if (tag.equals(WIKI_TAG_GROUP) || tag.equals(WIKI_TAG_GROUP_OLD)) {
            for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
                Object key   = keys.nextElement();
                Object value = props.get(key);
                topProps.add(key.toString());
                topProps.add(Json.quote(value.toString()));
            }
            HU.div(sb, "", HU.id(mainDivId));
            request.putExtraProperty(PROP_ADDED_GROUP, "true");
            topProps.addAll(propList);
            js.append("\nvar displayManager = getOrCreateDisplayManager("
                      + HU.quote(mainDivId) + "," + Json.map(topProps, false)
                      + ",true);\n");
            wikiUtil.appendJavascript(js.toString());
            return;
        }


        String fields = getProperty(wikiUtil, props, "fields", (String) null);
        if (fields != null) {
            List<String> toks = StringUtil.split(fields, ",", true, true);
            if (toks.size() > 0) {
                propList.add("fields");
                propList.add(Json.list(toks, true));
            }
            props.remove("fields");
        }




        String anotherDivId = getProperty(wikiUtil, props, "divid");
        String layoutHere = getProperty(wikiUtil, props, "layoutHere",
                                        (String) null);
        if (layoutHere != null) {
            Utils.add(propList, "layoutHere", layoutHere);
        }
        if (anotherDivId == null) {
            anotherDivId = HU.getUniqueId("displaydiv");
        }
        anotherDivId = anotherDivId.replace("$entryid", entry.getId());
        HU.div(sb, "",
               HU.clazz("display-container") + HU.id(anotherDivId)
               + HU.style("position:relative;"));
        Utils.add(propList, "divid", Json.quote(anotherDivId));
        props.remove("layoutHere");

        boolean needToCreateGroup =
            request.getExtraProperty(PROP_ADDED_GROUP) == null;

        String groupDivId = HU.getUniqueId("groupdiv");
        //Put the main div after the display div
        if (needToCreateGroup) {
            HU.div(sb, "", HU.id(groupDivId));
        }

        for (String arg : new String[] {
            "eventSource", "name", "displayFilter", "chartMin", ARG_WIDTH,
            ARG_HEIGHT, ARG_FROMDATE, ARG_TODATE, "column", "row"
        }) {
            String value = getProperty(wikiUtil, props, arg, (String) null);
            if (value != null) {
                Utils.add(propList, arg, Json.quote(value));
            }
            props.remove(arg);
        }


        //Only add the default layer to the display if its been specified
        defaultLayer = getProperty(wikiUtil, props, "defaultLayer",
                                   (String) null);
        //If its a map then check for the default layer
        if (displayType.equals("map")) {
            List<Metadata> layers =
                getMetadataManager().findMetadata(request, entry,
                    "map_layer", true);
            if ((layers != null) && (layers.size() > 0)) {
                defaultLayer = layers.get(0).getAttr1();
            }

            List<Metadata> markers =
                getMetadataManager().findMetadata(request, entry,
                    "map_marker", true);
            if ((markers != null) && (markers.size() > 0)) {
                int cnt = 1;
                for (Metadata mtd : markers) {
                    int idx = 1;
                    //The order is defined in resources/metadata.xml Map Marker metadata
                    List<String> attrs      = new ArrayList<String>();
                    String       markerDesc = mtd.getAttr(idx++);
                    List<String> toks =
                        StringUtil.splitUpTo(mtd.getAttr(idx++), ",", 2);
                    String lat        = (toks.size() > 0)
                                        ? toks.get(0)
                                        : "";
                    String lon        = (toks.size() > 1)
                                        ? toks.get(1)
                                        : "";
                    String markerType = mtd.getAttr(idx++);
                    String markerIcon = mtd.getAttr(idx++);
                    Utils.add(attrs, "metadataId", mtd.getId(),
                              "description", markerDesc, "lat", lat, "lon",
                              lon, "type", markerType, "icon", markerIcon);
                    for (String attr :
                            StringUtil.split(mtd.getAttr(idx++), "\n", true,
                                             true)) {
                        if (attr.startsWith("#")) {
                            continue;
                        }
                        List<String> pair = StringUtil.splitUpTo(attr, "=",
                                                2);
                        attrs.addAll(pair);
                        if (pair.size() == 1) {
                            attrs.add("");
                        }
                    }
                    String json = Json.mapAndQuote(attrs);
                    Utils.add(propList, "marker" + cnt,
                              Json.quote("base64:"
                                         + Utils.encodeBase64(json)));
                    cnt++;
                }
            }
        }


        if (defaultLayer != null) {
            Utils.add(propList, "defaultMapLayer", Json.quote(defaultLayer));
            props.remove("defaultLayer");
            props.remove("defaultMapLayer");
        }


        if (displayType.equals("entrygallery")
                || displayType.equals("entrygrid")) {
            List<Entry> children = getEntries(request, wikiUtil,
                                       originalEntry, entry, props);
            StringBuilder ids = new StringBuilder();
            for (Entry child : children) {
                if (ids.length() > 0) {
                    ids.append(",");
                }
                ids.append(child.getId());
            }
            props.remove("entries");
            Utils.add(propList, "entryIds", Json.quote(ids.toString()));
        }
        props.remove("type");

        for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
            Object key   = keys.nextElement();
            Object value = props.get(key);
            //      System.err.println ("adding:" + key +"=" + value);
            Utils.add(propList, key, Json.quote(value.toString()));
        }

        boolean isMap = displayType.equals("map");
        //Don't do this now
        if (false && isMap) {
            String mapVar = getProperty(wikiUtil, props, ATTR_MAPVAR);
            if (mapVar == null) {
                mapVar = MapInfo.makeMapVar();
                props.put(ATTR_MAPVAR, mapVar);
            }
            props.put("mapHidden", "true");
            MapInfo mapInfo = handleMapTag(request, wikiUtil, entry,
                                           originalEntry, WIKI_TAG_MAPENTRY,
                                           props, sb);
            Utils.add(propList, "theMap", mapVar);
        }

        HU.commentJS(
            js,
            "This gets the global display manager or creates it if not created");

        if (needToCreateGroup) {
            request.putExtraProperty(PROP_ADDED_GROUP, "true");
            Utils.concatBuff(
                js, "\nvar displayManager = getOrCreateDisplayManager(",
                HU.quote(groupDivId), ",", Json.map(topProps, false), ");\n");
        }
        Utils.add(propList, "entryId", HU.quote(entry.getId()));
        if ((pointDataUrl != null)
                && getProperty(wikiUtil, props, "includeData", true)) {
            Utils.add(propList, "data",
                      Utils.concatString("new PointData(", HU.quote(name),
                                         ",  null,null,",
                                         HU.quote(pointDataUrl), ",",
                                         "{entryId:'", entry.getId(), "'}",
                                         ")"));
        }

        if (isMap) {
            List<Metadata> metadataList =
                getMetadataManager().findMetadata(request, entry,
                    "map_displaymap", true);
            if ((metadataList != null) && (metadataList.size() > 0)) {
                String kmlIds       = null;
                String geojsonIds   = null;
                String kmlNames     = null;
                String geojsonNames = null;

                for (Metadata metadata : metadataList) {
                    if ( !Utils.stringDefined(metadata.getAttr1())) {
                        continue;
                    }
                    Entry mapEntry =
                        (Entry) getEntryManager().getEntry(request,
                            metadata.getAttr1());
                    if ((mapEntry == null)
                            || !(mapEntry.getTypeHandler()
                                .isType("geo_shapefile") || mapEntry
                                .getTypeHandler().isType("geo_geojson"))) {
                        continue;
                    }
                    if (mapEntry.getTypeHandler().isType("geo_shapefile")) {
                        if (kmlIds == null) {
                            kmlIds   = mapEntry.getId();
                            kmlNames = mapEntry.getName().replaceAll(",",
                                    " ");
                        } else {
                            kmlIds += "," + mapEntry.getId();
                            kmlNames += ","
                                        + mapEntry.getName().replaceAll(",",
                                            " ");
                        }
                    } else {
                        if (geojsonIds == null) {
                            geojsonIds = mapEntry.getId();
                            geojsonNames = mapEntry.getName().replaceAll(",",
                                    " ");
                        } else {
                            geojsonIds += "," + mapEntry.getId();
                            geojsonNames +=
                                "," + mapEntry.getName().replaceAll(",", " ");
                        }
                    }
                    if (Misc.equals(metadata.getAttr2(), "true")) {
                        Utils.add(propList, "displayAsMap", "true");
                        if (props.get("pruneFeatures") == null) {
                            Utils.add(propList, "pruneFeatures", "true");
                        }
                    }

                    if (kmlIds != null) {
                        Utils.add(propList, "kmlLayer", Json.quote(kmlIds),
                                  "kmlLayerName", Json.quote(kmlNames));
                    }
                    if (geojsonIds != null) {
                        Utils.add(propList, "geojsonLayer",
                                  Json.quote(geojsonIds), "geojsonLayerName",
                                  Json.quote(geojsonNames));
                    }
                }
            }
        }

        wikiUtil.addWikiAttributes(propList);
        js.append("displayManager.createDisplay(" + HU.quote(displayType)
                  + "," + Json.map(propList, false) + ");\n");
        wikiUtil.appendJavascript(js.toString());
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
        getMapManager().addMapImports(request, sb);
        if (request.getExtraProperty("initchart") == null) {
            request.putExtraProperty("initchart", "added");
            sb.append(displayImports);
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
            HU.importJS(sb, "https://www.gstatic.com/charts/loader.js");
            HU.script(sb, "HU.loadGoogleCharts();\n");
            HU.importJS(sb, getPageHandler().getCdnPath("/lib/d3/d3.min.js"));
            HU.importJS(sb, getPageHandler().getCdnPath("/lib/jquery.handsontable.full.min.js"));
            HU.cssLink(
                sb,
                getPageHandler().getCdnPath(
                    "/lib/jquery.handsontable.full.min.css"));

            //Put this here after the google load
            HU.importJS(sb, getPageHandler().getCdnPath("/lib/dom-drag.js"));

            if (getRepository().getMinifiedOk()) {
                HU.importJS(
                    sb,
                    getPageHandler().getCdnPath("/min/display_all.min.js"));
                HU.cssLink(
                    sb, getPageHandler().getCdnPath("/min/display.min.css"));
            } else {
		sb.append("\n");
                HU.cssLink(
                    sb, getPageHandler().getCdnPath("/display/display.css"));
		sb.append("\n");
                HU.importJS(
                    sb, getPageHandler().getCdnPath("/display/pointdata.js"));
		sb.append("\n");
                HU.importJS(
                    sb, getPageHandler().getCdnPath("/display/widgets.js"));
		sb.append("\n");
                HU.importJS(
                    sb,
                    getPageHandler().getCdnPath(
                        "/display/displaymanager.js"));
		sb.append("\n");
                HU.importJS(
                    sb, getPageHandler().getCdnPath("/display/display.js"));
		sb.append("\n");
                HU.importJS(
                    sb,
                    getPageHandler().getCdnPath("/display/displayentry.js"));
		sb.append("\n");
                HU.importJS(
                    sb,
                    getPageHandler().getCdnPath("/display/displaymap.js"));
		sb.append("\n");
                HU.importJS(
                    sb,
                    getPageHandler().getCdnPath("/display/displaymisc.js"));
		sb.append("\n");
                HU.importJS(
                    sb,
                    getPageHandler().getCdnPath("/display/displaychart.js"));
		sb.append("\n");
                HU.importJS(
                    sb,
                    getPageHandler().getCdnPath("/display/displaytable.js"));
		sb.append("\n");
                HU.importJS(
                    sb, getPageHandler().getCdnPath("/display/control.js"));
		sb.append("\n");
                HU.importJS(
                    sb, getPageHandler().getCdnPath("/display/notebook.js"));
                HU.importJS(
                    sb,
                    getPageHandler().getCdnPath("/display/displayplotly.js"));
                HU.importJS(
                    sb, getPageHandler().getCdnPath("/display/displayd3.js"));
                HU.importJS(
                    sb,
                    getPageHandler().getCdnPath("/display/displaytext.js"));
                HU.importJS(
                    sb,
                    getPageHandler().getCdnPath("/display/displayimages.js"));
                HU.importJS(
                    sb,
                    getPageHandler().getCdnPath("/display/displayext.js"));
		sb.append("\n");
            }
            HU.importJS(sb, getPageHandler().getCdnPath("/repositories.js"));
	    sb.append("\n");

            String includes =
                getRepository().getProperty("ramadda.display.includes",
                                            (String) null);
            if (includes != null) {
                for (String include :
                        StringUtil.split(includes, ",", true, true)) {
                    HU.importJS(sb, getFileUrl(include));
                }
            }
	    sb.append("\n");

            return sb.toString();
        } catch (Exception exc) {
            throw new IllegalArgumentException(exc);
        }
    }



    /**
     * Create an attribute with the name and value
     *
     * @param sb _more_
     * @param name  attribute name
     * @param value  value
     *
     */
    private static void attr(StringBuilder sb, String name, String value) {
        Utils.append(sb, " ", name, "=", "&quote;", value, "&quote;", " ");
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
        for (int i = 0; i < attrs.length; i += 2) {
            attr(sb, attrs[i], attrs[i + 1]);
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
        return Utils.concatString(prop, PROP_DELIM, args);
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

        /** _more_ */
        List<String> attrsList = new ArrayList<String>();

        /**
         * _more_
         *
         * @param tag _more_
         */
        WikiTag(String tag) {
            this(tag, null);
        }



        /**
         * _more_
         *
         * @param tag _more_
         * @param label _more_
         * @param attrs _more_
         */
        WikiTag(String tag, String label, String... attrs) {
            this.tag = tag;
            if (label == null) {
                label = StringUtil.camelCase(tag);
            }
            this.label = label;
            for (String attr : attrs) {
                attrsList.add(attr);
            }
            if (attrs.length == 1) {
                this.attrs = attrs[0];
            } else {
                StringBuilder sb  = new StringBuilder();
                int           cnt = 0;
                for (int i = 0; i < attrs.length; i += 2) {
                    if (cnt > 80) {
                        sb.append("_newline_");
                        cnt = 0;
                    }
                    cnt += attrs[i].length() + attrs[i + 1].length();
                    attr(sb, attrs[i], attrs[i + 1]);
                }
                this.attrs = sb.toString();
            }
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
