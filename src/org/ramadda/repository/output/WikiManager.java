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
import org.ramadda.repository.EntryUtil;
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
import org.ramadda.repository.metadata.License;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.metadata.MetadataHandler;
import org.ramadda.repository.metadata.MetadataManager;
import org.ramadda.repository.metadata.MetadataType;
import org.ramadda.repository.search.SearchInfo;
import org.ramadda.repository.search.SearchManager;
import org.ramadda.repository.search.SearchProvider;
import org.ramadda.repository.search.SpecialSearch;
import org.ramadda.repository.type.LocalFileTypeHandler;
import org.ramadda.repository.type.Column;
import org.ramadda.repository.type.DataTypes;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.repository.util.DateArgument;
import org.ramadda.repository.util.ServerInfo;
import org.ramadda.repository.auth.User;
import org.ramadda.repository.auth.DataPolicy;
import org.ramadda.repository.util.SelectInfo;



import org.ramadda.util.geo.Bounds;
import org.ramadda.util.geo.GeoUtils;
import org.ramadda.util.BufferMapList;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JQuery;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.NamedValue;
import org.ramadda.util.SystemContext;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.WikiPageHandler;
import org.json.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import java.lang.reflect.*;
import java.text.DecimalFormat;
import java.io.*;


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
@SuppressWarnings("unchecked")
public class WikiManager extends RepositoryManager implements  OutputConstants,WikiConstants,
							       WikiPageHandler, SystemContext {






    /** list of import items for the text editor menu */
    //J--
    public static final WikiTagCategory[] WIKITAGS = {
        new WikiTagCategory("General",
                            new WikiTag(WIKI_TAG_INFORMATION, null, ATTR_DETAILS, "false",ATTR_SHOWTITLE,"false"),
                            new WikiTag(WIKI_TAG_NAME,null,"link","true"), 
                            new WikiTag(WIKI_TAG_DESCRIPTION),
                            new WikiTag(WIKI_TAG_RESOURCE, null, ATTR_TITLE,"",ATTR_SHOWICON,"true"),
                            new WikiTag(WIKI_TAG_ENTRYLINK, null, "link","",ATTR_TITLE,"",ATTR_SHOWICON,"true"), 			    
                            new WikiTag(WIKI_TAG_THIS,null),
                            new WikiTag(WIKI_TAG_ANCESTOR,null,"type","entry type"), 			    
                            new WikiTag(WIKI_TAG_DATERANGE,"Date Range", ATTR_FORMAT,DateHandler.DEFAULT_TIME_FORMAT),
                            new WikiTag(WIKI_TAG_DATE_FROM, "From Date", ATTR_FORMAT,DateHandler.DEFAULT_TIME_FORMAT),
                            new WikiTag(WIKI_TAG_DATE_TO,"To Date", ATTR_FORMAT,DateHandler.DEFAULT_TIME_FORMAT), 
                            new WikiTag(WIKI_TAG_DATE_CREATE,"Create Date", ATTR_FORMAT,DateHandler.DEFAULT_TIME_FORMAT), 
                            new WikiTag(WIKI_TAG_DATE_CHANGE,"Change Date", ATTR_FORMAT,DateHandler.DEFAULT_TIME_FORMAT), 
                            new WikiTag(WIKI_TAG_LABEL, null, ATTR_TEXT,"",ATTR_ID,"arbitrary id to match with property"),
                            new WikiTag(WIKI_TAG_LINK, null, ATTR_TITLE,"","button","false"),
                            new WikiTag(WIKI_TAG_HTML,null,"showTitle","false"),
                            new WikiTag("multi", null, "_attrs", "attr1,attr2"),
                            new WikiTag(WIKI_TAG_SIMPLE, null, ATTR_TEXTPOSITION, POS_LEFT),
                            new WikiTag(WIKI_TAG_IMPORT, null, ATTR_ENTRY,"","showTitle","false"),
                            new WikiTag(WIKI_TAG_SHOW_AS, null, ATTR_ENTRY,"","type","entry type to display as","#target","target entry"),
                            new WikiTag(WIKI_TAG_EMBED, null, ATTR_ENTRY,"",ATTR_SKIP_LINES,"0",ATTR_MAX_LINES,"1000","style","",ATTR_FORCE,"false",ATTR_MAXHEIGHT,"300",ATTR_ANNOTATE,"true","raw","true","wikify","true"),
                            new WikiTag(WIKI_TAG_TAGS),
                            new WikiTag(WIKI_TAG_FIELD, null, "name", "")),
        new WikiTagCategory("Layout", 
                            new WikiTag(WIKI_TAG_TABLETREE, null, "simple","false","#maxHeight","500px"),
                            new WikiTag(WIKI_TAG_FULLTREE, null,"depth","10","addprefix","false","showroot","true","labelWidth","20", ATTR_SHOWICON,"true","types","group,feile,...."),
                            new WikiTag(WIKI_TAG_MENUTREE, null,"depth","10","addprefix","false","showroot","true","menuStyle","","labelWidth","20", ATTR_SHOWICON,"true","types","group,file,...."), 			    			    
                            new WikiTag(WIKI_TAG_LINKS, null),
                            new WikiTag(WIKI_TAG_LIST), 
                            new WikiTag(WIKI_TAG_TABS, null), 
                            new WikiTag(WIKI_TAG_GRID, null, 
                                        ATTR_TAG, WIKI_TAG_CARD, 
                                        "inner-height","200", 
					"width","200px",
                                        ATTR_COLUMNS, "3",
					ATTR_SHOWICON, "true",
					"includeChildren","false",
					"addTags","false",
					"showDisplayHeader","false",
					"captionPrefix","",
					"captionSuffix","",
					"#childrenWiki","wiki text to display children, e.g. {{tree details=false}}",
					"#weights","3,6,3",
                                        "showSnippet","false",
                                        "showSnippetHover","true",
                                        "showLink","false","showHeading","true"), 
                            new WikiTag(WIKI_TAG_FLIPCARDS, null, 
                                        "inner","300", 
					"width","300",
					"addTags","false",
					"frontStyle","",
					"backStyle",""),					
                            new WikiTag(WIKI_TAG_MAP,
                                        null, ATTR_WIDTH, "100%", ATTR_HEIGHT, "80vh","listentries","true"), 
                            new WikiTag(WIKI_TAG_FRAMES, null, ATTR_WIDTH,"100%", ATTR_HEIGHT,"500"), 
                            new WikiTag(WIKI_TAG_ACCORDION, null, ATTR_TAG, WIKI_TAG_HTML, ATTR_COLLAPSE, "false", "border", "0", ATTR_SHOWLINK, "true", ATTR_SHOWICON, "false",ATTR_TEXTPOSITION, POS_LEFT), 
                            //                            new WikiTag(WIKI_TAG_GRID), 
                            new WikiTag(WIKI_TAG_TABLE), 
                            new WikiTag(WIKI_TAG_ABSOPEN,null,"canEdit","true","imageEntry","","width","100%","#height","height"),
                            new WikiTag(WIKI_TAG_ABSCLOSE,null), 			    
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
					"decorate","true","imageStyle","","padding","10px",
					ATTR_CAPTION, "Figure ${count}: ${name}",
					"#popupCaption",""), 
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
                            new WikiTag(WIKI_TAG_PLAYER, "Image Player", "loopdelay","1000","loopstart","false","imageWidth","90%")),
        new WikiTagCategory("Misc",
                            new WikiTag("counter", null, "key", "key"),
                            new WikiTag("caption", null, "label", "","prefix","Image #:"),
                            new WikiTag(WIKI_TAG_CALENDAR, null, ATTR_DAY, "false"),
                            new WikiTag(WIKI_TAG_CALENDAR, null, ATTR_DAY, "false"),
                            new WikiTag(WIKI_TAG_DATETABLE, null,"byType","false","showTime","false"),			    			    
                            new WikiTag(WIKI_TAG_TIMELINE, null, ATTR_HEIGHT, "150"),
                            new WikiTag(WIKI_TAG_ZIPFILE, null,"#height",""),
                            new WikiTag(WIKI_TAG_USER, null, "users","user1,user2","delimiter"," ","style","","showAvatar","true","showEmail","true"),
                            new WikiTag(WIKI_TAG_COMMENTS),
                            new WikiTag(WIKI_TAG_TAGCLOUD, null, "#type", "", "threshold","0"), 
                            new WikiTag(WIKI_TAG_PROPERTIES, null, "message","","metadata.types","",ATTR_METADATA_INCLUDE_TITLE,"true","separator","","decorate","false"),
                            new WikiTag(WIKI_TAG_DATAPOLICIES, null, "message","","inherited","true","includePermissions","false"),
			    new WikiTag(WIKI_TAG_WIKITEXT,null,"showToolbar","false"),
                            new WikiTag(WIKI_TAG_BREADCRUMBS),
                            new WikiTag(WIKI_TAG_TOOLS),
                            new WikiTag(WIKI_TAG_TOOLBAR),
                            new WikiTag(WIKI_TAG_LAYOUT),
			    new WikiTag(WIKI_TAG_MENU,null,"title","","popup","true","ifUser","false"),
                            new WikiTag(WIKI_TAG_ENTRYID),
                            new WikiTag(WIKI_TAG_ALIAS,null,"name","alias","entry","entry id"),
                            new WikiTag(WIKI_TAG_SEARCH,null,
                                        ATTR_TYPE, "", 
                                        "#"+ATTR_FIELDS,"",
                                        ATTR_METADATA,"",
                                        ARG_MAX, "100",
                                        ARG_SEARCH_SHOWFORM, "false",
                                        SpecialSearch.ATTR_TABS,
                                        SpecialSearch.TAB_LIST),
                            new WikiTag(WIKI_TAG_UPLOAD,null, ATTR_TITLE,"Upload file", ATTR_SHOWICON,"false"), 
                            new WikiTag(WIKI_TAG_ROOT),
			    new WikiTag("loremipsum")),
    };
    //J++



    /** output type */
    public static final OutputType OUTPUT_WIKI = new OutputType("Wiki",
								"wiki.view",
								OutputType.TYPE_VIEW,
								"", ICON_WIKI);



    private int groupCount = 0;


    /** _more_ */
    private String displayImports;
    private String displayInits;


    /** _more_ */
    private Hashtable<String, String> wikiMacros;



    private WikiUtil dummyWikiUtil = new WikiUtil();

    private Hashtable<String,WikiTagHandler> tagHandlers =  new Hashtable<String,WikiTagHandler>();


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
		 Utils.split(
			     getRepository().getProperty("ramadda.wiki.macros", ""),
			     ",", true, true)) {
            wikiMacros.put(macro,
                           getRepository().getProperty("ramadda.wiki.macro."
						       + macro, ""));
        }
        WikiUtil.setGlobalProperties(wikiMacros);

	tagHandlers =  new Hashtable<String,WikiTagHandler>();
	try {
	    for(Class c: getPluginManager().getSpecialClasses()) {
		if (WikiTagHandler.class.isAssignableFrom(c)) {
		    Constructor ctor = Misc.findConstructor(c,
							    new Class[] { Repository.class });
		
		    if (ctor == null) {
			System.err.println("Could not find WikiTagHandler constructor:" + c.getName());
			continue;
		    }
		    if (ctor != null) {
			WikiTagHandler tagHandler = (WikiTagHandler) ctor.newInstance(new Object[] { getRepository()});
			tagHandler.initTags(tagHandlers);
		    }
		}
	    }
	    for(TypeHandler typeHandler:getRepository().getTypeHandlers()) {
		if (WikiTagHandler.class.isAssignableFrom(typeHandler.getClass())) {
		    ((WikiTagHandler)typeHandler).initTags(tagHandlers);
		}
	    
	    }
	} catch(Exception exc) {
            getLogManager().logError("WikiManager.creating tagHandlers", exc);
	}
    }


    public boolean getShowIcon(WikiUtil wikiUtil, Hashtable props, boolean dflt) {
	// for backwards compatability
	return  getProperty(wikiUtil, props, ATTR_SHOWICON,
			    getProperty(wikiUtil, props,"showicon",true));
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

    public double getProperty(WikiUtil wikiUtil, Hashtable props, String prop,
                           double dflt) {
        String value = getProperty(wikiUtil, props, prop, null);
        if (value == null) {
            return dflt;
        }

        return Double.parseDouble(value);
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
                if (remainder.trim().length()>0 && remainder.indexOf("=") < 0) {
                    List<String> toks = Utils.splitUpTo(remainder, " ",
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
                theEntry = findEntryFromId(request, entry, wikiUtil, props,
                                           entryId.trim());
                if (theEntry == null) {
                    return getMessage(wikiUtil, props,
                                      "Unknown entry:" + entryId);
                }
            }

            String entryRoot = getProperty(wikiUtil, props, ARG_ANCESTOR,
                                           null);
            if (entryRoot != null) {
                Entry root = findEntryFromId(request, theEntry, wikiUtil, props,
                                             entryRoot);
                if (root != null) {
                    props.put(ARG_ANCESTOR, root.getId());
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
						  new String[]{"wikiattribute"}, true);
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

	    if(Utils.getProperty(props,"primaryPage",false)) {
		Entry primaryEntry = (Entry)wikiUtil.getProperty("primaryEntry");
		if(primaryEntry==null || !entry.equals(primaryEntry)) return "";
	    }

	    if(theEntry!=null) {
		theEntry.getTypeHandler().addWikiProperties(theEntry, wikiUtil,
							    tag, props);
		addWikiLink(wikiUtil, theEntry);
	    }
	    //If we are doing an {{import}} of an entry that does have displays but has no group defined then
	    //there can be any number of errors. This fixes it:
	    Object tmpAddedGroup =    tag.equals("import")?request.getExtraProperty(PROP_GROUP_VAR):null;
	    if(tmpAddedGroup!=null) {
		//		System.err.println("tag:" + tag+" removing:" + tmpAddedGroup);
		request.removeExtraProperty(PROP_GROUP_VAR);
	    }



            String include = handleWikiImport(wikiUtil, request, entry,
					      theEntry, tag, props,remainder);
	    if(tmpAddedGroup!=null) {
		request.putExtraProperty(PROP_GROUP_VAR,tmpAddedGroup);
	    }
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
    public String wikifyEntry(Request request, Entry entry, String wikiContent)
	throws Exception {
        return wikifyEntry(request, entry, wikiContent, true);
    }



    /**
     * Wikify the entry
     *
     * @param request The request
     * @param entry   the Entry
     * @param wikiContent  the content to wikify
     * @param wrapInDiv    true to wrap in a div tag
     *
     * @return the wikified Entry
     *
     * @throws Exception  problem wikifying
     */
    public String wikifyEntry(Request request, Entry entry,
                              String wikiContent, boolean wrapInDiv)
	throws Exception {
        return wikifyEntry(request, entry, wikiContent, wrapInDiv, (HashSet)null);
    }


    /**
     * _more_
     *
     * @param request the request
     * @param entry _more_
     * @param wikiContent _more_
     * @param wrapInDiv _more_
     * @param notTags _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String wikifyEntry(Request request, Entry entry,
                              String wikiContent, boolean wrapInDiv,
                              HashSet notTags)
	throws Exception {

	//Check for loops
	boolean isPrimaryRequest = false;
	Hashtable alreadyDoingIt  = (Hashtable) request.getExtraProperty("alreadyDoingIt");
	if(alreadyDoingIt==null) {
	    alreadyDoingIt = new Hashtable();
	    isPrimaryRequest=true;
	    request.putExtraProperty("alreadyDoingIt", alreadyDoingIt);
	}
	List contentList = (List) alreadyDoingIt.get(entry.getId());
	if(contentList==null) {
	    alreadyDoingIt.put(entry.getId(),contentList = new ArrayList());
	}
	if(contentList.contains(wikiContent)) {
	    return "";
	}
	contentList.add(wikiContent);

        Request myRequest = request.cloneMe();
        WikiUtil wikiUtil =
            initWikiUtil(myRequest,
                         new WikiUtil(Misc.newHashtable(new Object[] {
				     ATTR_REQUEST,
				     myRequest, ATTR_ENTRY, entry })), entry);

	if(isPrimaryRequest) {
	    wikiUtil.putProperty("primaryEntry", entry);
	}
        return wikifyEntry(request, entry, wikiUtil, wikiContent, wrapInDiv,
                           notTags, true);
    }


    /**
     * Wikify the entry
     *
     * @param request The request
     * @param entry   the Entry
     * @param wikiUtil The wiki util
     * @param wikiContent  the content to wikify
     * @param wrapInDiv    true to wrap in a div tag
     * @param notTags _more_
     * @param includeJavascript _more_
     *
     * @return the wikified Entry
     *
     * @throws Exception  problem wikifying
     */
    public String wikifyEntry(Request request, Entry entry,
                              WikiUtil wikiUtil, String wikiContent,
                              boolean wrapInDiv,  HashSet notTags,
                              boolean includeJavascript)
	throws Exception {

	if(!request.get(PROP_SHOW_TITLE,true)) {
	    wikiUtil.putProperty(PROP_SHOW_TITLE,"false");
	} else {
	    wikiUtil.removeProperty(PROP_SHOW_TITLE);
	}


        //TODO: We need to keep track of what is getting called so we prevent
        //infinite loops
        String content;
	try {
	    content = wikiUtil.wikify(wikiContent, this, notTags);
	} catch(Exception exc) {
            getLogManager().logError("WikiManager.wikifyEntry", exc);
	    return getPageHandler().showDialogError("An error occurred:" + exc);
	}
        if (wrapInDiv) {
            content = HU.div(content, HU.cssClass("wikicontent")) + "\n";
        }
        if (includeJavascript) {
            String js = wikiUtil.getJavascript(true);
            if (js!=null && js.length() > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append(content);
                sb.append(HU.script(js));
                content = sb.toString();
            }
        }

        return content;
    }





    public Result processFindEntryFromId(Request request) throws Exception {
	StringBuilder sb = new StringBuilder();
	String entryId = request.getString(ARG_ENTRYID,null);
	System.err.println("processFindEntryFromId:" + entryId);
	if(entryId==null) {
	    sb.append("error: no entryId provided");
	    System.err.println("\t" + sb);
	    return new Result("", sb);
	}
	Hashtable props = null;
	String propsArg = request.getString("props",null);
	if(propsArg!=null) props = (Hashtable) getRepository().decodeObject(new String(Utils.decodeBase64(propsArg)));
	if(props==null) props = new Hashtable();
	Entry entry = findEntryFromId(request, null, dummyWikiUtil, props, entryId);
	System.err.println("entry:"  + entryId +" " + entry+ " props:" + props);
	if(entry==null) {
	    sb.append("error: could not find entry:" + entryId);
	    System.err.println("\t" + sb);
	    return new Result("", sb);
	}
	String xml = "<entries>" + getXmlOutputHandler().getEntryXml(request, entry) +"</entries>";
	sb.append(xml);
	return new Result("", sb, repository.getMimeTypeFromSuffix(".xml"));
    }

    public Result processGetEntries(Request request) throws Exception {
	StringBuilder sb = new StringBuilder();
	return new Result("", sb, JsonUtil.MIMETYPE);
    }


    private Entry findEntryFromId(ServerInfo server, Entry entry,
				  WikiUtil wikiUtil, Hashtable props, String entryId) throws Exception {


	String url = HtmlUtils.url(server.getUrl() +"/wiki/findentryfromid");
	String propString = Utils.encodeBase64(getRepository().encodeObject(props==null?new Hashtable():props));
	//	System.err.println("url:" + url);
	String xml = IO.doPost(new URL(url),HU.args(new String[]{ARG_ENTRYID,entryId,"props",propString},true));
	if(xml!=null && xml.startsWith("error:")) 
	    throw new RuntimeException("Error reading remote entry. Server:" + server +" entryId:" + entryId +" " + xml);
	if(!Utils.stringDefined(xml)) throw new RuntimeException("Could not find entry. Server:" + server +" entry id:" + entryId);
	List<Entry> entries=  getEntryManager().createRemoteEntries(getRepository().getTmpRequest(), server, xml);
	return entries.get(0);
    }

    public List<Entry> getEntries(ServerInfo server, WikiUtil wikiUtil,
                                  Entry originalEntry, Entry entry,
                                  Hashtable props, boolean onlyImages,
                                  String attrPrefix)
	throws Exception {
	return null;
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
    public Entry findEntryFromId(Request request, Entry entry,
				 WikiUtil wikiUtil, Hashtable props, String entryId)
	throws Exception {



        Entry theEntry = null;

	//Check for an alias:
	if(wikiUtil!=null) {
	    theEntry =(Entry) wikiUtil.getWikiProperty("entry:" + entryId);
	    if(theEntry!=null) return theEntry;
	}


        int   barIndex = entryId.indexOf("|");
        if (barIndex >= 0) {
            entryId = entryId.substring(0, barIndex);
        }
        if (entryId.equals(ID_THIS)) {
            theEntry = entry;
        }

	ServerInfo serverInfo = getServer(request, entry, wikiUtil, props);
	if(serverInfo!=null) {
	    return findEntryFromId(serverInfo, entry, wikiUtil, props, entryId);
	}

        if (entryId.startsWith(ENTRY_PREFIX_ALIAS)) {
            String alias = Utils.clip(entryId,ENTRY_PREFIX_ALIAS);
            return getEntryManager().getEntryFromAlias(request, alias);
        }

        if (entryId.startsWith(ENTRY_PREFIX_CHILD)) {
	    //child:type:<some type>
            String tok = Utils.clip(entryId,ENTRY_PREFIX_CHILD);
            if (tok.startsWith(ENTRY_PREFIX_TYPE)) {
                tok     = Utils.clip(tok,ENTRY_PREFIX_TYPE);
                request = request.cloneMe();
                request.put(ARG_TYPE, tok);
		tok="";
	    }
	    tok = tok.trim();
	    if(tok.length()>0) {
		entry = findEntryFromId(request,  entry, wikiUtil, props, tok);
	    }

	    List<Entry> children = getEntryManager().getChildren(request, entry);
	    children= EntryUtil.sortEntriesOnDate(children,false);	    
            if (children.size() > 0) {
                return children.get(0);
            }
            return null;
        }

        if (entryId.startsWith(ENTRY_PREFIX_GRANDCHILD)) {
	    //grandchild:type:<some type>
	    List<Entry> children = getEntryManager().getChildren(request, entry);
	    children= EntryUtil.sortEntriesOnDate(children,false);	    
	    if (children.size() == 0) {
		return null;
	    }
            String tok = Utils.clip(entryId,ENTRY_PREFIX_GRANDCHILD);
            if (tok.startsWith(ENTRY_PREFIX_TYPE)) {
                tok     = Utils.clip(tok,ENTRY_PREFIX_TYPE);
                request = request.cloneMe();
                request.put(ARG_TYPE, tok);
	    }
	    for(Entry child: children) {
		List<Entry> gchildren = getEntryManager().getChildren(request, child);
		if (gchildren.size() != 0) {
		    gchildren= EntryUtil.sortEntriesOnDate(gchildren,false);	    
		    return gchildren.get(0);
		}
	    }
	    return null;
        }


	/*
	  ancestor:type
	 */
        if (entryId.startsWith(ENTRY_PREFIX_ANCESTOR)) {
            String type      = Utils.clip(entryId,ENTRY_PREFIX_ANCESTOR).trim();
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


        if (entryId.equals("link") || entryId.startsWith(ENTRY_PREFIX_LINK)) {
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

	
	if(theEntry==null) {
	    List<Entry> entries =
		getEntriesFromEmbeddedSearch(request, wikiUtil,  props, entry,  entryId, 1);
	    if(entries!=null && entries.size()>0) {
		theEntry = entries.get(0);
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
            theEntry = getEntryManager().findEntryFromName(request, null, entryId);
        }

        //Look for relative to the current entry
        if (entry!=null && theEntry == null) {
            theEntry = getEntryManager().findEntryFromPath(request, entry,
							   entryId);
        }

        //Finally try it as an alias
        if (theEntry == null) {
            theEntry = getEntryManager().getEntryFromAlias(request, entryId);
        }

        return theEntry;

    }

    private List<Entry> getEntriesFromEmbeddedSearch(Request request, WikiUtil wikiUtil, Hashtable props,
						     Entry entry, String entryId, int max) throws Exception {
	if(!entryId.startsWith(ENTRY_PREFIX_SEARCH)) {
	    return null;
	}
	entryId = Utils.clip(entryId,ENTRY_PREFIX_SEARCH);
	List<String> toks = Utils.split(entryId,";",true,true);
	/*
	  entry=search:ancestor;type:some_type;orderby:
	*/
	Request myRequest = new Request(getRepository(), request.getUser());
	boolean ascending = false;
	String orderBy = ORDERBY_FROMDATE;
	for(String tok: toks) {
	    if(tok.startsWith(ENTRY_PREFIX_TYPE)) {
		myRequest.put(ARG_TYPE,Utils.clip(tok,ENTRY_PREFIX_TYPE));
	    } else if(tok.startsWith(ENTRY_PREFIX_ORDERBY)) {
		orderBy = Utils.clip(tok,ENTRY_PREFIX_ORDERBY);
	    } else if(tok.startsWith(ENTRY_PREFIX_ASCENDING)) {
		tok= Utils.clip(tok,ENTRY_PREFIX_ASCENDING);		    
		ascending = tok.length()==0|| tok.equals("true");
	    } else if(tok.startsWith(ENTRY_PREFIX_DESCENDENT)) {
		tok = Utils.clip(tok,ENTRY_PREFIX_DESCENDENT);
		if(tok.length()==0) {
		    myRequest.put(ARG_ANCESTOR,entry.getId());
		} else {
		    Entry otherEntry = findEntryFromId(request,  entry, wikiUtil, props, tok);
		    if(otherEntry!=null) {
			myRequest.put(ARG_ANCESTOR,otherEntry.getId());
		    } else {
			System.err.println("Could not find descendent entry:" + tok);
		    }
		}
	    }
	}
	myRequest.put(ARG_ORDERBY,orderBy+(ascending?"_ascending":"_descending"));
	if(max>0) 
	    myRequest.put(ARG_MAX,""+max);
	return  getSearchManager().doSearch(myRequest,new SearchInfo());
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
        String        maxWidth = getProperty(wikiUtil, props, "maxWidth", null);        if (maxWidth != null) {
            style+= "max-width:" + HU.makeDim(maxWidth,"px")+";";
        }	

        String    border = getProperty(wikiUtil, props, ATTR_BORDER, null);
        String bordercolor = getProperty(wikiUtil, props, ATTR_BORDERCOLOR,
                                         "#000");

        if (border!=null || bordercolor!=null) {
	    if(border==null) border="1px solid ";
            style += HU.css("border", border + " "  + bordercolor);
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
            int v =  Integer.parseInt(s);
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
            List<String> toks = Utils.splitUpTo(src, "::", 2);
            if (toks.size() == 2) {
                src        = toks.get(0);
                attachment = toks.get(1);
            }
        }
        src = src.trim();


	for(String s: Utils.split(src,",")) {
	    List<Column> columns = entry.getTypeHandler().getColumns();
	    if(columns!=null) {
		for (Column column : columns) {
		    if(column.isType(DataTypes.DATATYPE_URL) && column.getName().equals(s)) {
			Object[] values = entry.getValues();
			String url    = column.getString(values);
			if(url!=null && url.length()>0) {
			    return getWikiImage(wikiUtil, request, url, entry, props);
			}
		    }
		}
	    }
	}




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
        for (Metadata metadata : getMetadataManager().getMetadata(request,srcEntry)) {
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
                                     String tag, Hashtable props, String remainder,
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
                                            entry, tag, props,remainder);

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

	boolean inAbs = !tag.equals(WIKI_TAG_ABSOPEN) &&
	    !tag.equals(WIKI_TAG_ABSCLOSE) && Misc.equals("true",wikiUtil.getWikiProperty("inabs"));
	if(inAbs) {
	    String style2="position:absolute;";
	    String left = getProperty(wikiUtil,props,"absLeft",null);
	    String right = getProperty(wikiUtil,props,"absRight",null);
	    String top = getProperty(wikiUtil,props,"absTop",null);
	    String bottom = getProperty(wikiUtil,props,"absBottom",null);
	    if(!stringDefined(left) && !stringDefined(right)) left="10px";
	    if(!stringDefined(top) && !stringDefined(bottom)) top="10px";	    
	    if(stringDefined(left)) style2+=HU.css("left",left);
	    if(stringDefined(right)) style2+=HU.css("right",right);
	    if(stringDefined(top)) style2+=HU.css("top",top);
	    if(stringDefined(bottom)) style2+=HU.css("bottom",bottom);
	    style2+=HU.css("display","inline-block");
	    String translateX = getProperty(wikiUtil,props,"absTranslateX",null);
	    String translateY = getProperty(wikiUtil,props,"absTranslateY",null);	    
	    String transform ="";
	    if(translateX!=null)
		transform+=" translateX(" + translateX+") ";
	    if(translateY!=null)
		transform+=" translateY(" + translateY+") ";
	    if(Utils.stringDefined(transform)) 
		style2+="-webkit-transform: " +transform+";transform: " + transform +";";
	    sb.append(HU.open("div",HU.cssClass("ramadda-abs") + HU.style(style2)));
	}

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
                                 wikiUtil.getNotTags(), true);
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
	    return HU.makePopup(null, blockTitle, contents,
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
					HU.cssClass("entry-toggleblock-label ramadda-hoverable"), "",
					getIconUrl(ICON_TOGGLEARROWDOWN),
					getIconUrl(ICON_TOGGLEARROWRIGHT));

        }

	if(inAbs) {
	    sb.append(HU.close("div"));
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
		String label = Utils.makeLabel(tag.tag) + " properties";
                tmp.add(JsonUtil.map(Utils.makeList("label",JsonUtil.quote(label))));
                for (int j = 0; j < tag.attrsList.size(); j += 2) {
                    tmp.add(JsonUtil.map(Utils.makeList("p",JsonUtil.quote(tag.attrsList.get(j)),"ex",
							JsonUtil.quote(tag.attrsList.get(j + 1)))));
                }
                tags.add(tag.tag);
                tags.add(JsonUtil.list(tmp));
            }
        }
        sb.append(JsonUtil.map(tags));
        Result result = new Result("", sb, JsonUtil.MIMETYPE);
        result.setShouldDecorate(false);
        return result;
    }



    public Result processGetWiki(Request request) throws Exception {
        Entry entry = getEntryManager().getEntry(request,
						 request.getString(ARG_ENTRYID, ""));
	StringBuilder wiki = new StringBuilder();
	getRepository().getHtmlOutputHandler().handleDefaultWiki(request, entry, wiki);
        Result result = new Result("", wiki);
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
	wiki = Request.cleanXSS(wiki);
        if (request.defined(ARG_ENTRYID)) {
            if ( !request.get("doImports", true)) {
                request.putExtraProperty("initchart", "added");
                request.putExtraProperty(MapManager.ADDED_IMPORTS, "added");
            }
            Entry entry = getEntryManager().getEntry(request,
						     request.getString(ARG_ENTRYID, ""));
	    if(entry==null) {
		Result result =  getRepository().makeErrorResult(request, "Unknown entry:" + request.getString(ARG_ENTRYID,""),false);
		result.setShouldDecorate(false);
		return result;
	    }
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
					      new String[]{"wiki_notebook"}, false);


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
                                       JsonUtil.MIMETYPE);
            result.setShouldDecorate(false);

            return result;
        }

        Result result = new Result(
				   new FileInputStream(
						       getMetadataManager().getFile(
										    request, entry, metadata,
										    2)), JsonUtil.MIMETYPE);
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
							 + "'}"), JsonUtil.MIMETYPE);

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
			      JsonUtil.MIMETYPE);
        }
        if ( !getAccessManager().canDoEdit(request, entry)) {
            return new Result(
			      "", new StringBuilder("{\"error\":\"cannot edit entry\"}"),
			      JsonUtil.MIMETYPE);
        }

        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry,
					      new String[]{"wiki_notebook"}, false);

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
            getMetadataManager().addMetadata(request,
					     entry,
					     new Metadata(
							  getRepository().getGUID(), entry.getId(),
							  "wiki_notebook", false, notebookId, theFile, "", "", ""));
            getEntryManager().updateEntry(null, entry);

            return new Result("", new StringBuilder("{\"result\":\"ok\"}"),
                              JsonUtil.MIMETYPE);
        } else {
            File file = getMetadataManager().getFile(request, entry,
						     metadata, 2);
            getStorageManager().writeFile(file, notebook);

            return new Result("", new StringBuilder("{\"result\":\"ok\"}"),
                              JsonUtil.MIMETYPE);
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
                                       String theTag, Hashtable props,String remainder)
	throws Exception {

	if(!checkIf(wikiUtil,request,entry,props)) return "";
        boolean wikify  = getProperty(wikiUtil, props, ATTR_WIKIFY, true);
        String criteria = getProperty(wikiUtil, props, ATTR_IF,
                                      (String) null);
        //      System.err.println("tag:"+ theTag);
        if (criteria != null) {}

        StringBuilder sb = new StringBuilder();
	if(theTag.equals("testcheckboxes")) {
	    List values =new ArrayList();
	    List selected =request.get("widgetname",new ArrayList<String>());
	    for(int i=0;i<10;i++) {
		//The values array can be String, TwoFacedObject or a Selector
		HtmlUtils.Selector selector = new HtmlUtils.Selector("Value " + i,"value-" + i);
		selector.setTooltip("Tooltip for: Value " +i); 
		values.add(selector);
	    }
	    sb.append(request.formPost(getRepository().URL_ENTRY_SHOW));
	    sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
	    String extra = "";
	    //The css is the style for the containing box
	    HU.checkboxSelect( sb, "widgetname", values,
			       selected, HU.css("padding","5px","max-height","100px","overflow-y","auto","border","1px solid #ccc;"), extra);

	    sb.append(HU.submit("Submit"));
	    sb.append(HU.formClose());
	    return sb.toString();
	}
	WikiTagHandler tagHandler = tagHandlers.get(theTag);
	if(tagHandler!=null) {
	    String s = tagHandler.handleTag(wikiUtil, request,
					    originalEntry, entry, theTag,
					    props, remainder);
	    if(s!=null) return s;
	}



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
        } else if (theTag.equals(WIKI_TAG_USER)) {
	    List<String> ids = new ArrayList<String>();
	    String userId = getProperty(wikiUtil,props,"user",null);
	    if(userId!=null) {
		ids.add(userId);
	    }
	    String users = getProperty(wikiUtil,props,"users",null);
	    if(users!=null) {
		ids.addAll(Utils.split(users,",",true,true));
	    }	    
	    if(ids.size()==0) {
		User user = entry.getUser();
		if(user!=null) 
		    ids.add(user.getId());
	    }
	    if(ids.size()==0) return "No user:" + userId;
	    String delimiter = getProperty(wikiUtil,props,"delimiter"," ");
	    String template = getProperty(wikiUtil,props,"template",null);
	    boolean showAvatar = getProperty(wikiUtil,props,"showAvatar",true);
	    boolean showSearch = getProperty(wikiUtil,props,"showSearch",true);
	    boolean showEmail = getProperty(wikiUtil,props,"showEmail",true);
	    boolean showDescription = getProperty(wikiUtil,props,"showDescription",true);
	    
	    
	    for(String id:ids) {
		User user = getUserManager().findUser(id);
		if(user==null) continue;
		String avatar = getUserManager().getUserAvatar(request,  user, false, getProperty(wikiUtil,props,"avatarWidth",60),null);
		if(template!=null) {
		    sb.append(Utils.applyMacros(template,
						  "avatar",avatar,
						  "search",
						  getUserManager().getUserSearchLink(request, user),
						  "name",user.getLabel(),
						  "id",user.getId(),						  
						  "description",user.getDescription()));
		    
		    continue;
		}
		
		sb.append(HU.open("div",HU.cssClass("ramadda-user-profile") +HU.style(getProperty(wikiUtil, props,"style",""))));
		sb.append("<table><tr valign=top>");
		if(showAvatar) HU.tag(sb,"td","",avatar);
		sb.append("<td>");
		if(showAvatar)sb.append("<div style='margin-left:10px;'>");
		else sb.append("<div>");		
		if(showSearch) 
		    sb.append(getUserManager().getUserSearchLink(request, user));
		else
		    sb.append(user.getLabel());
		if(showEmail && stringDefined(user.getEmail())) {
		    sb.append("<br>");
		    sb.append(HU.href("mailto:" +user.getEmail(),user.getEmail()));
		}
		sb.append("</div>");
		sb.append("</td></tr></table>");
		if(showDescription && stringDefined(user.getDescription())) {
		    sb.append(HU.div(user.getDescription(),
				     HU.style(HU.css("max-width","150px","max-height","200px","overflow-y","auto"))));
		}	    
		//		sb.append("</td></tr></table>");
		sb.append(HU.close("div"));
		sb.append(delimiter);
	    }
	    return sb.toString(); 
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
		    root = findEntryFromId(request, entry, wikiUtil, props,
					   entryRoot);
		}
		String type = getProperty(wikiUtil, props, "entryType",(String)null);

		if(type!=null) {
		    request= request.cloneMe();
		    request.put(ARG_TYPE, type);
		}

		if(next) {
		    other = getEntryUtil().getNext(request,  entry,  root, tree, sort, asc);
		} else {
		    other = getEntryUtil().getPrev(request,  entry,  root, tree, sort, asc);
		}

            } else {
                other = parent;
            }
            if (other == null) {
		return "";
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
                img = HU.faIcon("fa-arrow-left",
				HU.attrs("class",
					 "ramadda-nav-arrow "
					 + extraImgClass, "title", title,
					 "style",
					 "font-size:"
					 + iconSize) + ";");
            } else if (next) {
                img = HU.faIcon("fa-arrow-right",
				HU.attrs("class",
					 "ramadda-nav-arrow "
					 + extraImgClass, "title", title,
					 "style",
					 "font-size:" + iconSize
					 + ";"));
            } else {
                img = HU.faIcon("fa-arrow-up",
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
                clazz += " ramadda-shadow-box ramadda-nav-page-decorated ";
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
                text = wikifyEntry(request, entry, text, false, 
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
        } else if (theTag.equals(WIKI_TAG_VERSION)) {
	    return RepositoryUtil.getVersion();
        } else if (theTag.equals(WIKI_TAG_MAKELABEL)) {
	    return Utils.makeLabel(remainder);
        } else if (theTag.equals(WIKI_TAG_RESOURCE)) {
            String url = null;
            boolean inline = getProperty(wikiUtil, props,
					 "inline", false);
            String label;
            if ( !entry.getResource().isDefined()) {
                url   = entry.getTypeHandler().getPathForEntry(request,
							       entry,false);
                label = getProperty(wikiUtil, props, ATTR_TITLE, url);
            } else if (entry.getResource().isFile()) {
                url = entry.getTypeHandler().getEntryResourceUrl(request, entry,inline);
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

            boolean showicon = getShowIcon(wikiUtil, props, false);
            if (showicon) {
                label = HU.img(getIconUrl("fas fa-download"))
		    + HU.space(2) + label;
            }

            if ( !simple) {
                return HU.div(HU.href(url, label,
                                      HU.cssClass("ramadda-button")
                                      + HU.attr("role", "button")));
            }
            String extra = "";

            return HU.href(url, label, extra);
        } else if (theTag.equals(WIKI_TAG_ENTRYLINK)) {
	    String link = getProperty(wikiUtil, props, "link","");
            String label =  getProperty(wikiUtil, props, ATTR_TITLE, link);


            String url =  HU.url(getRepository().getUrlBase() +"/entry/show",
				 ARG_ENTRYID, entry.getId(),
				 ARG_OUTPUT,link);

            boolean makeButton = getProperty(wikiUtil, props, "makeButton", false);
            boolean showicon = getShowIcon(wikiUtil, props, false);
            if (showicon) {
                label = HU.img(getIconUrl("/icons/download.png"))
		    + HU.space(2) + label;
            }

            if (makeButton) {
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
            if (getShowIcon(wikiUtil, props, false)) {
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
                                   wikiUtil.getNotTags(), true);
                //                desc = makeWikiUtil(request, false).wikify(desc, null);
            }
            if (getProperty(wikiUtil, props, "convert_newline", false)) {
                desc = desc.replaceAll("\n", "<p>");
            }

            return desc;
        } else if (theTag.equals(WIKI_TAG_LAYOUT)) {
            return getHtmlOutputHandler().makeHtmlHeader(request, entry,
							 getProperty(wikiUtil, props, ATTR_TITLE, "Layout"));
        } else if (theTag.equals("license")) {
	    License license = getMetadataManager().getLicense(remainder.trim());
	    if(license==null) {
		//a hack  for the cc licenses
		license = getMetadataManager().getLicense(remainder.trim()+"-4.0");
	    }
	    if(license==null) {
		return remainder;
	    }
	    String result= "";
	    String icon = license.getIcon();
	    if(icon!=null) {
		result =   HU.image(icon,
                                    HU.attrs("title",license.getName(),"width", "60", "border", "0"));
	    } else {
		result = license.getName();
	    }
	    String url = license.getUrl();
            if(url!=null) result =  HU.href(url, result, "target=_other");
	    return result;
        } else if (theTag.equals(WIKI_TAG_THIS)) {
	    return entry.getId();
        } else if (theTag.equals(WIKI_TAG_ANCESTOR)) {
	    String type = getProperty(wikiUtil, props, "type", null);
	    Entry parent = entry.getParentEntry();
	    if(parent==null) return entry.getId();
	    if(type==null) return parent.getId();
	    while(parent!=null) {
		if(parent.getTypeHandler().isType(type)) return parent.getId();
		parent = parent.getParentEntry();
	    }
	    return "null";
        } else if (theTag.equals(WIKI_TAG_NAME)) {
            String name = getEntryDisplayName(entry);
            if (getProperty(wikiUtil, props, "link", false)) {
		//In case we are making a snapshot we use the overrideurl
		String url = (String)request.getExtraProperty(PROP_OVERRIDE_URL);
		String linkStyle = getProperty(wikiUtil, props, "linkStyle", "");
		if(url!=null && url.equals("#"))  {
		} else if(url==null) {
		    url = getEntryManager().getEntryUrl(request, entry);
		}
                name = HU.href(url, name, HU.cssClass("ramadda-clickable")+HU.style(linkStyle));
            }
            return name;
        } else if (theTag.equals(WIKI_TAG_EMBED)) {
            boolean doUrl = getProperty(wikiUtil, props, "dourl",   false);
	    if(doUrl) {
	    } else {
		if ( !entry.isFile()
		     || ( !isTextFile(entry, entry.getResource().getPath())
			  && !getProperty(wikiUtil, props, ATTR_FORCE,
					  false))) {
		    return "Entry isn't a text file";
		}
	    }
            StringBuilder txt = new StringBuilder("");
            InputStream fis; 
	    if(doUrl) {
		fis = getStorageManager().getInputStream(entry.getResource().getPath());
	    } else {
		fis = getStorageManager().getFileInputStream(getStorageManager().getEntryResourcePath(entry));
	    }
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
	    int maxSize = Utils.getProperty(props,"maxSize",-1);
	    int size = 0;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (skipLines > 0) {
                    skipLines--;
                    continue;
                }
		size+=line.length();
		if(maxSize>=0 && size>maxSize) break;
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
				    false,  (HashSet)null, false);
	    }
	    String style = "";
	    if(maxHeight>0) {
		style += HU.css("max-height", maxHeight   + "px","overflow-y","auto");
	    }
	    style +=  getProperty(wikiUtil, props, "style","");
	    if(maxHeight>0 || !raw) {
		return HU.pre(txt.toString(), HU.style(style));
	    } else {
		return txt.toString();
	    }
        } else if (theTag.equals(WIKI_TAG_FIELD)) {
            String name = getProperty(wikiUtil, props, ATTR_FIELDNAME,
                                      (String) null);
            if (name != null) {
		String  decimalFormat = getProperty(wikiUtil,props,"decimalFormat",null);
		boolean raw = getProperty(wikiUtil, props, "raw",false);
                String fieldValue =
                    entry.getTypeHandler().getFieldHtml(request, entry, props, name,raw);
                if (fieldValue != null) {
		    if(decimalFormat!=null) {
			try {
			    DecimalFormat fmt     = new DecimalFormat(decimalFormat);
			    double d = Double.parseDouble(fieldValue);
			    fieldValue =fmt.format(d);
			} catch(Exception exc) {
			}
		    }
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

            return getDateHandler().formatDate(request, entry, date,
					       getProperty(wikiUtil, props, ATTR_FORMAT, null));
        } else if (theTag.equals(WIKI_TAG_DATERANGE)) {
            String format = getProperty(wikiUtil, props, ATTR_FORMAT,
                                        (String) null);
            Date date1 = new Date(entry.getStartDate());
            Date date2 = new Date(entry.getEndDate());
            SimpleDateFormat dateFormat =
                getDateHandler().getDateFormat(request, entry, format);
            String separator = getProperty(wikiUtil, props, ATTR_SEPARATOR,
                                           " -- ");

            return dateFormat.format(date1) + separator
		+ dateFormat.format(date2);
        } else if (theTag.equals(WIKI_TAG_ALIAS)) {
	    String name = getProperty(wikiUtil,props,"name","alias");
	    String entryId = getProperty(wikiUtil,props,"entry","");	    
	    Entry theEntry = findEntryFromId(request, entry, wikiUtil, props,  entryId);
	    if(theEntry!=null)
		wikiUtil.putWikiProperty("entry:" + name, theEntry);
	    return "";
        } else if (theTag.equals(WIKI_TAG_SHOW_AS)) {
	    String template = null;
	    String type = getProperty(wikiUtil,props,"type",null);

	    String target = getProperty(wikiUtil,props,"target",null);	    
	    if(stringDefined(type)) {
		TypeHandler typeHandler =
                    getRepository().getTypeHandler(type);
		if(typeHandler==null) return "Bad type:" + type;
		template = typeHandler.getWikiTemplate(request);
	    } else if(stringDefined(target)) {
		Entry targetEntry = getEntryManager().getEntry(request,target);
		if(targetEntry==null) return "Bad target entry:" + target;
		template = targetEntry.getTypeHandler().getWikiTemplate(request,targetEntry);
	    }
	    if(!stringDefined(template)) {
		return "No template found";
	    }
	    wikiUtil.putWikiProperty("showTitle","false");
	    String results = wikifyEntry(request,  entry, wikiUtil, template, false,
					 Utils.makeHashSet(WIKI_TAG_DESCRIPTION,WIKI_TAG_SHOW_AS),
					 false);
	    wikiUtil.removeWikiProperty("showTitle");
	    return results;
        } else if (theTag.equals(WIKI_TAG_ENTRYID)) {
            return entry.getId();
        } else if (theTag.equals(WIKI_TAG_PROPERTY)) {
            for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
                String key   = (String) keys.nextElement();
                String value = (String) props.get(key);
		if(key.equals("name") || key.equals("value")) continue;
		wikiUtil.putWikiProperty(key, value);
	    }

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
	    String entryId = Utils.getProperty(props, ARG_ENTRYID,(String)null);
	    if(entryId!=null) {
		Entry propEntry = getEntryManager().getEntry(request,entryId);
		if(propEntry!=null) {
		    String keySuffix = Utils.getProperty(props, "keySuffix","");
		    String keyPrefix = Utils.getProperty(props, "keyPrefix","");
		    String caseType = Utils.getProperty(props, "case","");		    		    
		    int lengthLimit = Utils.getProperty(props,"lengthLimit",1000);
		    String contents = getStorageManager().readFile(getStorageManager().getEntryResourcePath(propEntry));
		    List<String> lines = Utils.split(contents,"\n",true,true);
		    for(int i=1;i<lines.size();i++) {
			List<String> toks = Utils.split(lines.get(i),",");
			if(toks.size()<2) continue;
			String key = toks.get(0);
			String label = Utils.applyCase(caseType,toks.get(1));
			key = key.replaceAll("['\"]+","");
			label = label.replaceAll("['\"]+","");			
			if(label.length()>lengthLimit) {
			    label = label.substring(0,lengthLimit-1)+"...";
			}
			wikiUtil.appendJavascript("addGlobalDisplayProperty(" + HU.squote(keyPrefix+key+keySuffix)
						  + "," + HU.squote(label) + ");\n");
			
		    }
		}
		return "";
	    }

            for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
                String key   = (String) keys.nextElement();
                String value = (String) props.get(key);
		value = value.replaceAll("\n"," ");
                wikiUtil.appendJavascript(HU.call("addGlobalDisplayProperty",HU.squote(key),HU.squote(value)));
            }
            return "";

        } else if (theTag.equals(WIKI_TAG_DISPLAYPROPERTY)) {
            for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
                String key   = (String) keys.nextElement();
                String value = (String) props.get(key);
		if(key.equals("name") || key.equals("value")) continue;
                wikiUtil.appendJavascript("addGlobalDisplayProperty('" + key
                                          + "','" + value + "');\n");

            }

            String name  = (String) props.get("name");
            String value = (String) props.get("value");
            if ((name != null) && (value != null)) {
                wikiUtil.appendJavascript("addGlobalDisplayProperty('" + name
                                          + "','" + value + "');\n");

                return "";
            }

            return "";
        } else if (theTag.equals(WIKI_TAG_DATAPOLICIES)) {
	    boolean inherited =  getProperty(wikiUtil, props, "inherited", true);
	    boolean includeCollection =  getProperty(wikiUtil, props, "includeCollection", false);
	    boolean includePermissions =  getProperty(wikiUtil, props, "includePermissions", false);	    
	    List<DataPolicy> dataPolicies = getAccessManager().getDataPolicies(entry, inherited);
	    if(dataPolicies.size()==0) {
                String message = getProperty(wikiUtil, props, ATTR_MESSAGE,
                                             "");
                return message;

	    }
	    getAccessManager().formatDataPolicies(request, sb, dataPolicies,includeCollection, includePermissions);
	    return sb.toString();
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
        } else if (theTag.equals(WIKI_TAG_ABSCLOSE)) {
	    wikiUtil.putWikiProperty("inabs","false");
	    return "</div>";
        } else if (theTag.equals(WIKI_TAG_ABSOPEN)) {
	    //	    StringBuilder sb = new StringBuilder();
	    String defaultWidth = "800px";
	    String image = getProperty(wikiUtil, props, "image",null);
	    String width = getProperty(wikiUtil,props,"width",null);
	    String height = getProperty(wikiUtil,props,"height",null);	    
	    String imageEntryId= getProperty(wikiUtil,props,"imageEntry",null); 
	    if(image==null && imageEntryId==null) {
		if(width==null) width=defaultWidth;
		if(height==null) height="600px";		
	    } else {
		String iwidth = getProperty(wikiUtil,props,"imageWidth",width);
		String iheight = getProperty(wikiUtil,props,"imageHeight",null);	    
		props.remove("width");
		props.remove("height");		
		if(iwidth==null) iwidth=width=defaultWidth;
		props.put("width",iwidth);
		if(iheight!=null) {
		    props.put("height",iheight);
		}		
	    }
	    String style = HU.css("position","relative",
				  "width",width);
	    if(height!=null) style+=HU.css("height",height);
	    style+=getProperty(wikiUtil,props,"style","");
	    props.put("style","");
	    String id = HU.getUniqueId("canvas_");
	    wikiUtil.putWikiProperty("inabs","true");
	    boolean canEdit = getProperty(wikiUtil, props,"canEdit",false) &&
		getAccessManager().canDoEdit(request, entry);
	    if(canEdit) {
		HU.div(sb,"",HU.style("width:" + width)+HU.id(id+"_header"));
	    }
	    StringBuilder attrs = new StringBuilder();
	    for(int i=0;i<50;i++) {
		String line = getProperty(wikiUtil,props,"line"+i,null);
		if(line!=null) {
		    attrs.append(HU.attr("line" + i,line));
		}
	    }

	    sb.append(HU.open("div", HU.id(id)+HU.style(style)+attrs));
	    sb.append("\n");
	    //	    sb.append(HU.open("canvas", attrs+HU.attrs("definedWidth",width,"id",id,"tabindex","1","width", width,"height",height,"style",HU.css("position","absolute","background","transparent", "left","0px","top","0px"))));
	    //	    sb.append(HU.close("canvas"));
	    sb.append(HU.importJS(getRepository().getHtdocsUrl("/canvas.js")));
	    sb.append("\n");
	    HU.script(sb, "new RamaddaCanvas('"+id+"'," + canEdit+");\n");
	    sb.append("\n");
	    if(image!=null || imageEntryId!=null) {
		if(image!=null)
		    props.put("src",image);
		Entry imageEntry = entry;
		if(imageEntryId!=null)
		    imageEntry =  getEntryManager().getEntry(request,imageEntryId);
		sb.append(HU.div(getWikiImage(wikiUtil, request, imageEntry, props),HU.cssClass("ramadda-bgimage")));
	    }
	    return sb.toString();
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
            myRequest.setEmbedded(true);
	    if(!getProperty(wikiUtil,props,"showTitle",true)) {
		myRequest.put(PROP_SHOW_TITLE,"false");
	    }
            Result result = getEntryManager().processEntryShow(myRequest,
							       entry);
            //            Result result = getHtmlOutputHandler().getHtmlResult(newRequest,
            //                                OutputHandler.OUTPUT_HTML, entry);
            return new String(result.getContent());
        } else if (theTag.equals(WIKI_TAG_ZIPFILE)) {
	    StringBuilder tmp = new StringBuilder();
	    getZipFileOutputHandler().outputZipFile(entry,tmp);
            String height = getProperty(wikiUtil, props, "height",(String)null);
	    String result =  tmp.toString();
	    if(height!=null) {
		result = HU.div(result,HU.attrs("style","max-height:" + HU.makeDim(height,"px")+";overflow-y:auto;"));
	    }
	    return result;
        } else if (theTag.equals(WIKI_TAG_CALENDAR)) {
            List<Entry> children = getEntries(request, wikiUtil,
					      originalEntry, entry, props);
            boolean doDay = getProperty(wikiUtil, props, ATTR_DAY, false)
		|| request.defined(ARG_DAY);
            getCalendarOutputHandler().outputCalendar(request,
						      getCalendarOutputHandler().makeCalendarEntries(request,
												     children), sb, doDay);

            return sb.toString();

        } else if (theTag.equals(WIKI_TAG_DATETABLE)) {
            List<Entry> children = getEntries(request, wikiUtil,
					      originalEntry, entry, props);
            getCalendarOutputHandler().makeDateTable(request,
						     children, sb,
						     getProperty(wikiUtil, props, "byType", false),
						     getProperty(wikiUtil, props, "showTime", false));

            return sb.toString();


        } else if (theTag.equals(WIKI_TAG_DISPLAY)
                   || theTag.startsWith("display_")
                   || theTag.equals(WIKI_TAG_CHART)) {
	    if(entry==null) {
		return "{{"+ theTag+" " +"No entry" +"}}";
	    }
            String ancestor = getProperty(wikiUtil, props, ARG_ANCESTOR,
                                           null);
	    if(stringDefined(ancestor)) {
                Entry ancestorEntry = findEntryFromId(request, entry, wikiUtil, props,
                                             ancestor);
                if (ancestorEntry != null) {
                    props.put("ancestorName",ancestorEntry.getName());
                }
	    }

            String tooltip = getProperty(wikiUtil, props, "tooltip",
					 null);
	    if(tooltip!=null) {
		tooltip = tooltip.replace("${entryid}",entry.getId()).replace("${entryname}",entry.getName());
		tooltip = tooltip.replace("${mainentryid}",originalEntry.getId()).replace("${mainentryname}",originalEntry.getName());		
		props.put("tooltip",tooltip);
	    }

            String jsonUrl = null;
	    ServerInfo serverInfo = getServer(request, entry, wikiUtil, props);
            boolean doEntries = getProperty(wikiUtil, props, "doEntries",
                                            false);
            boolean doEntry = getProperty(wikiUtil, props, "doEntry", false);

            if (doEntries || doEntry) {
		if(serverInfo!=null) {
		    jsonUrl = HtmlUtils.url(serverInfo.getUrl()+  "/entry/show",
					    ARG_ENTRYID,entry.getId(), ARG_OUTPUT,
					    JsonOutputHandler.OUTPUT_JSON_POINT.getId(),"remoteRequest","true");
		} else {
		    String entries = getProperty(wikiUtil,props,"entries",null);
		    jsonUrl = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry, ARG_OUTPUT,
					       JsonOutputHandler.OUTPUT_JSON_POINT.getId());
		    if(entries!=null) jsonUrl = HU.url(jsonUrl,"entries",entries);
		}
		//If there is an ancestor specified then we use the /search/do url
		boolean doSearch = getProperty(wikiUtil, props, "doSearch",false);
		ancestor = getProperty(wikiUtil, props, "ancestor",(String)null);
		
                if (ancestor!=null || doSearch) {
		    jsonUrl = HU.url(getRepository().getUrlBase()+"/search/do", ARG_OUTPUT,
				     JsonOutputHandler.OUTPUT_JSON_POINT.getId());

		}
		if(doSearch) {
		    String type = getProperty(wikiUtil, props, "type",(String) null);
		    if(type!=null)
			jsonUrl += "&type=" + type;
		}

		//		System.err.println("JSON:" + jsonUrl +"  "+ doSearch);

                if (ancestor!=null) {
		    if(ancestor.equals(ID_THIS)) ancestor = entry.getId();
                    jsonUrl += "&ancestor=" + ancestor;
                }
                if (doEntry) {
                    jsonUrl += "&onlyentry=true";
                }
                if (getProperty(wikiUtil, props, "addAttributes", false)) {
                    jsonUrl += "&addAttributes=true";
                }
		if(getProperty(wikiUtil, props, "imagesOnly", false)) {
                    jsonUrl += "&imagesOnly=true";
		}


		String entryTypes = getProperty(wikiUtil, props, "entryTypes",(String)null);
                if (entryTypes!=null) {
                    jsonUrl += "&entryTypes=" + entryTypes;
                }
		String notentryTypes = getProperty(wikiUtil, props, "notEntryTypes",(String)null);
                if (notentryTypes!=null) {
                    jsonUrl += "&notEntryTypes=" + notentryTypes;
                }		
                if (getProperty(wikiUtil, props, "addPointUrl", false)) {
                    jsonUrl += "&addPointUrl=true";
                }		
                if (getProperty(wikiUtil, props, "addThumbnails", false)) {
                    jsonUrl += "&addThumbnails=true";
                }
                if (getProperty(wikiUtil, props, "addImages", false)) {
                    jsonUrl += "&addImages=true";
                }
                if (getProperty(wikiUtil, props, "addMediaUrl", false)) {
                    jsonUrl += "&addMediaUrl=true";
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
		//Push the props
		for(String prop:new String[]{"max","lastRecords"}) {
		    if (props.get(prop) == null) {
			String value = getProperty(wikiUtil, props, prop, null);
			if (value != null) {
			    props.put(prop,value);
			}
                    }
                }

		if(serverInfo!=null) {
		    String max = Utils.getProperty(props,"max",null);
		    String url = serverInfo.getUrl() +"/entry/wikiurl?entryid=" + entry.getId() +(max!=null?"&max=" + max:"");
		    String json = IO.readContents(url);
		    JSONObject obj      = new JSONObject(json);
		    String error = obj.optString("error");
		    if(Utils.stringDefined(error)) throw new RuntimeException("Error getting remote URL:" + error);
		    jsonUrl = obj.optString("url");		    
		} else {
		    //Do this here since the startdate might be in a property tag
		    String startDate = getProperty(wikiUtil, props, "request.startdate", (String) null);
		    if(startDate!=null) props.put("request.startdate", startDate);
		    String endDate = getProperty(wikiUtil, props, "request.enddate", (String) null);
		    if(endDate!=null) props.put("request.enddate", endDate);		    
                    jsonUrl += "&addSnippets=true";
		    jsonUrl = entry.getTypeHandler().getUrlForWiki(request,
								   entry, theTag, props, displayProps);
		}
            }
	    //	    System.err.println("tag:" + theTag+ " jsonurl:" +jsonUrl);
            //Gack - handle the files that are gridded netcdf
            //This is awful to have this here but I just don't know how to 
            //handle these entries
            if ((jsonUrl == null)
		&&
		(entry.getTypeHandler().isType(Constants.TYPE_FILE) ||
		 entry.getResource().isS3())
		&& entry.getResource().getPath().toLowerCase().endsWith(
									".nc")) {
		TypeHandler gridType =
                    getRepository().getTypeHandler("cdm_grid");
                if (gridType != null) {
                    jsonUrl = gridType.getUrlForWiki(request, entry, theTag,
						     props, displayProps);
                }
            }
	    if(Misc.equals("true",request.getExtraProperty(PROP_MAKESNAPSHOT))) {
		if(request.isAnonymous()) throw new RuntimeException("Anonymous users cannot make snapshots");
		List<String[]> snapshotFiles = (List<String[]>) request.getExtraProperty("snapshotfiles");
		File tmpFile = getStorageManager().getTmpFile(request, "point.json");
		Date now = new Date();
		String fileName = jsonUrl.replaceAll("^/.*\\?","").replace("output=points.product&product=points.json&","").replaceAll("[&=\\?]+","_").replace("entryid_","");
		fileName += "_"+  now.getTime() +".json";
		fileName =  Utils.makeID(entry.getName()) +"_"+fileName;
		snapshotFiles.add(new String[]{tmpFile.toString(), fileName, entry.getName()});
		URL url = new URL(request.getAbsoluteUrl(jsonUrl).replace("localhost:","127.0.0.1:"));
		jsonUrl = fileName;
		OutputStream fos = getStorageManager().getFileOutputStream(tmpFile);
		InputStream fis = IO.getInputStream(url);
		IOUtil.writeTo(fis, fos);
		IOUtil.close(fos);
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
        } else if (theTag.equals(WIKI_TAG_DISPLAY_IMPORTS)) {
	    StringBuilder tmp = new StringBuilder();
	    addDisplayImports(request, tmp,true);
	    return tmp.toString();
        } else if (theTag.equals(WIKI_TAG_MAP)
                   || theTag.equals(WIKI_TAG_MAPENTRY)) {
            handleMapTag(request, wikiUtil, entry, originalEntry, theTag,
                         props, sb);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_TOOLS)) {
            StringBuilder links = new StringBuilder();
            int           cnt   = 0;
	    HashSet<String> seen = new HashSet<String>();

            for (Link link :
		     getEntryManager().getEntryLinks(request, entry)) {
                if ( !link.isType(OutputType.TYPE_IMPORTANT)) {
                    continue;
                }
		if(seen.contains(link.getUrl())) continue;
		seen.add(link.getUrl());
                String label = getIconImage(link.getIcon()) + HU.space(1)
		    + link.getLabel();
                HU.href(links, link.getUrl(), label);
                links.append(HU.br());
                cnt++;
            }
	    List<Metadata> mtdList = 
		getMetadataManager().findMetadata(request, entry,
						  new String[]{"output_tools"}, true);
	    if(mtdList!=null) {
		for(Metadata mtd: mtdList) {
		    OutputType type = getRepository().findOutputType(mtd.getAttr1());
		    if(type==null) continue;
		    Link link =getHtmlOutputHandler().makeLink(request, entry, type);
		    if(seen.contains(link.getUrl())) continue;
		    seen.add(link.getUrl());
		    String label = getIconImage(link.getIcon()) + HU.space(1)
			+ link.getLabel();
		    HU.href(links, link.getUrl(), label);
		    links.append(HU.br());
		    cnt++;
		}
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
	    boolean ifUser = getProperty(wikiUtil, props, "ifUser",false);
	    if(ifUser && request.isAnonymous()) {
		return "";
	    }

            String menus = getProperty(wikiUtil, props, ATTR_MENUS, "");
            int type = OutputType.getTypeMask(Utils.split(menus, ",",
							  true, true));

            String links = getEntryManager().getEntryActionsTable(request,
								  entry, type);
	    String title = getProperty(wikiUtil, props, "title",null);
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
								       "fas fa-caret-down"), msg(
												 "Click to show menu"), HU.cssClass(
																    "ramadda-breadcrumbs-menu-img"));
		if(Utils.stringDefined(title)) menuLinkImg += " " + title;
		String menuLink = HU.makePopup(popup, menuLinkImg, links);
                popup.append(menuLink);
                return popup.toString();
            }

	    if(Utils.stringDefined(title)) links = title +"<br>" + links;
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
            int           width          = -1;	    
            List<String>  headers        = null;
            String        headerProp     = null;
            String        footerProp     = null;
            String        headerTemplate = null;
            List<Entry>   entries        = null;
	    String style = null;
	    String layout = getProperty(wikiUtil, props, "_layout","table");
	    String gridBoxWidth= getProperty(wikiUtil, props, "_gridboxwidth","250px");
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
                    headers = Utils.split(value, ",");
                } else if (key.equals("_headerTemplate")) {
                    headerTemplate = value;
                } else if (key.equals("_header")) {
                    headerProp = value;
                } else if (key.equals("_footer")) {
                    footerProp = value;
                } else if (key.equals("_columns")) {
                    columns = Integer.parseInt(value);
                } else if (key.equals("_layout")) {
                    layout = value;
                } else if (key.equals("_gridboxwidth")) {
                    gridBoxWidth = value;		    
                } else if (key.equals("_width")) {
                    width = Integer.parseInt(value);
                } else if (key.equals("_style")) {
                    style=value;
                } else if (key.startsWith("_")) {
                    key = key.substring(1);
                    List<String> toks = Utils.split(value, ",");
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

	    boolean grid = layout.equals("grid");

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
	    if(grid) {
		columns=0;
                buff.append("\n<div class=ramadda-grid>\n");
	    } else if (columns > 0) {
                buff.append("\n<table width=100%><tr valign=top>\n");
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
		if(grid) {
		    buff.append("\n<div class=ramadda-grid-component style='width:" + HU.makeDim(gridBoxWidth,null)+";display:inline-block;'>\n");
		} else if (columns > 0) {
                    colCnt++;
                    if (colCnt > columns) {
                        buff.append("</tr><tr valign=top>");
                        colCnt = 1;
                    }
                    buff.append("<td width='" + colWidth + "'>");
                } else if(width>0) {
                    buff.append("<div style='display:inline-block;width:" + width+";'>");
		}
		if(style!=null) 
                    buff.append("<div style='display:inline-block;" + style+"'>\n");
                Entry theEntry = entry;
                if ((entries != null) && (i < entries.size())) {
                    theEntry = entries.get(i);
                }

                String header = ((headers != null) && (i < headers.size()))
		    ? headers.get(i)
		    : null;
                if (header != null) {
                    if (headerTemplate != null) {
                        header = headerTemplate.replace("${header}", header);
                    }
                    buff.append(header);
                } else if(headerTemplate != null) {
		    String url = getEntryManager().getEntryUrl(request, theEntry);
		    header = headerTemplate.replace("${name}",theEntry.getName()).replace("${entryid}", theEntry.getId()).replace("${entryurl}",url);

		    header =  wikifyEntry(request, theEntry, header, false);
                    buff.append(header);
		}
		Entry  tmpEntry    = (Entry) wikiUtil.getProperty(ATTR_ENTRY);
		wikiUtil.putProperty(ATTR_ENTRY, theEntry);
                if (s != null) {
                    s = s.replaceAll("_dollar_", "\\$").replaceAll("_nl_", "\n").replaceAll("_qt_","\"");
		    //		    System.err.println("WIKIFY:" + s);
		    String tmp=  wikifyEntry(request, theEntry, wikiUtil, s,
                                             false, (HashSet) null, false);


                    buff.append(tmp);
                } else {
		    if(debug) {
			System.err.println("p6:" + _props.get("chartHeight"));
			System.err.println("p7:" + firstProps.get("chartHeight"));
		    }
                    buff.append(getWikiIncludeInner(wikiUtil, request,
						    originalEntry, theEntry, tag, _props,""));
                }
		if(tmpEntry!=null)
		    wikiUtil.putProperty(ATTR_ENTRY, tmpEntry);

		if(style!=null) 
                    buff.append("\n</div>");
		if(grid) {
		    buff.append("\n</div>\n");
		} else if (columns > 0) {
                    buff.append("</td>");
                } else if(width>0) {
                    buff.append("</div>");
		}
            }
	    if(grid) {
                buff.append("</div>\n");
	    } else  if (columns > 0) {
                buff.append("</tr></table>\n");
            }

            if (footerProp != null) {
                buff.append(footerProp);
            }

            return buff.toString();
        } else if (theTag.equals(WIKI_TAG_TEMPLATE)) {
            String template = getProperty(wikiUtil, props,
					  "template","empty");
	    request.put("template",template);
	    return "";
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


            boolean showicon = getProperty(wikiUtil, props,
					   APPLY_PREFIX + ATTR_SHOWICON, false);
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
						      originalEntry, child, tag, tmpProps,remainder,
						      true);

                String prefix   = prefixTemplate;
                String suffix   = suffixTemplate;
                String urlLabel = getEntryDisplayName(child);
                if (showicon) {
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
                content.append(getSnippet(request, child, true,""));
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
                    if (showicon) {
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
        } else if (theTag.equals(WIKI_TAG_MAPPOPUP)) {
            return makeMapPopup(request, wikiUtil, props, originalEntry,
				entry);
        } else if (theTag.equals(WIKI_TAG_TAGS)) {
            String types = getProperty(wikiUtil, props, "types",
				       "enum_tag,content.keyword");
	    List<Metadata> metadataList =
		getMetadataManager().findMetadata(request, entry,
						  types.split(","), false);
	    if(metadataList!=null && metadataList.size()>0) {
		sb.append("<div class=metadata-tags>");
		for(Metadata metadata: metadataList) {
		    String mtd = metadata.getAttr(1);
		    String url = getMetadataManager().findType(metadata.getType()).getSearchUrl(request, metadata);
		    MetadataHandler mtdh = getMetadataManager().findHandler(metadata.getType());
		    String contents = mtdh.getTag(request, metadata);
		    contents = HU.href(url, contents,HU.attr("title","Search"));
		    sb.append(HU.span(contents,""));

		}
		sb.append("</div>");
	    }
	    return sb.toString();
        } else if (theTag.equals(WIKI_TAG_TABS)
                   || theTag.equals(WIKI_TAG_ACCORDION)
                   || theTag.equals(WIKI_TAG_ACCORDIAN)
                   || theTag.equals(WIKI_TAG_SLIDESHOW)
                   || theTag.equals(WIKI_TAG_BOOTSTRAP)
                   || theTag.equals(WIKI_TAG_FLIPCARDS)		   
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
            boolean flipCards = theTag.equals(WIKI_TAG_FLIPCARDS);

            if (getProperty(wikiUtil, props, ATTR_USEDESCRIPTION) != null) {
                boolean useDescription = getProperty(wikiUtil, props,
						     ATTR_USEDESCRIPTION, true);

                if (useDescription) {
                    dfltTag = WIKI_TAG_SIMPLE;
                } else {
                    dfltTag = WIKI_TAG_HTML;
                }
            }

	    if(flipCards) dfltTag = "card";
            boolean showDate = getProperty(wikiUtil, props, "showDate",
					   false);
            String frontStyle = getProperty(wikiUtil, props, "frontStyle","");
            String backStyle = getProperty(wikiUtil, props, "backStyle","");	    
            SimpleDateFormat sdf =         new SimpleDateFormat(getProperty(wikiUtil,props,"dateFormat","MMM dd, yyyy"));
            SimpleDateFormat sdf2 =         new SimpleDateFormat(getProperty(wikiUtil,props,"dateFormat","yyyy-MM-dd HH:mm"));	    
            boolean showicon = getShowIcon(wikiUtil, props, false);
            if (doingGrid) {
		//                showicon = false;
                if (props.get("showLink") == null) {
                    props.put("showLink", "false");
                }
            }

            boolean addTags = getProperty(wikiUtil, props, "addTags",
					  false);

            boolean showHeading = !flipCards && getProperty(wikiUtil, props, "showHeading",
							    true);
            boolean headingSmall = !flipCards && getProperty(wikiUtil, props,
							     "headingSmall", true);
            String headingClass = headingSmall
		? HU.cssClass(
			      "ramadda-subheading ramadda-subheading-small")
		: HU.cssClass("ramadda-subheading");

            boolean showLink = !flipCards && getProperty(wikiUtil, props, ATTR_SHOWLINK,
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
            if (doingGrid || flipCards) {
                tmpProps.put("showHeading", "false");
                if (tmpProps.get(ATTR_SHOWICON) == null) {
                    tmpProps.put(ATTR_SHOWICON, "true");
                }
            }
            if (children.size() > 0) {
                checkHeading(request, wikiUtil, props, sb);
            }

            for (Entry child : children) {
                String title = getEntryDisplayName(child);
                if (showicon) {
		    //                    title = HU.img(getPageHandler().getIconUrl(request,  child)) + " " + title;
                }
                titles.add(title);
                //                urls.add(request.entryUrl(getRepository().URL_ENTRY_SHOW, child));
                urls.add(getEntryManager().getEntryUrl(request, child));

                tmpProps.put("defaultToCard", "true");
		String inner = my_getWikiInclude(wikiUtil, newRequest,
						 originalEntry, child, tag, tmpProps, "", true);
                StringBuilder content =   new StringBuilder();
		List<Metadata> tagList =null;
		if(addTags) {
		    String[] tagTypes;
		    String  tagType = getProperty(wikiUtil, props, "tagTypes",null);
		    if(tagType!=null) {
			tagTypes = Utils.toStringArray(Utils.split(tagType,",",true,true));
		    } else {
			tagTypes = new String[]{"enum_tag","content.keyword"};
		    }
		    tagList = 
			getMetadataManager().findMetadata(request, child,
							  tagTypes, false);
		    if(tagList!=null && tagList.size()>0) {
			content.append("<div class=metadata-tags>");
			for(Metadata metadata: tagList) {
			    MetadataHandler mtdh = getMetadataManager().findHandler(metadata.getType());
			    content.append(mtdh.getTag(request, metadata));
			}
			content.append("</div>");
		    }

		}

		content.append(inner);
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
		String theContent;
		if(flipCards) {
		    String url = getEntryManager().getEntryUrl(request, child);
                    String href = HU.href(url, linklabel.isEmpty()
					  ? getEntryDisplayName(child)
					  : linklabel);		    
		    String back = href;
		    if(showDate) {
			back +="<br>" + sdf.format(new Date(child.getStartDate()));
		    }
		    back = HU.div(back,HU.cssClass("ramadda-flip-card-label"));
		    List<Entry> tmp = new ArrayList<Entry>();
		    tmp.add(child);
		    String front = content.toString();
		    // a hack
		    String myFrontStyle = frontStyle;
		    if(front.replaceAll(" ","").equals("<divclass=\"ramadda-card\"></div>")) {
			myFrontStyle+="background:#eee;";
			front = back;
		    }
		    String flipAttrs = 
			HU.style("width",HU.makeDim(width,"px"),"height",height+"px");
		    theContent= HU.makeFlipCard(front,back,
						flipAttrs,
						HU.attrs("style",myFrontStyle),
						HU.attrs("style",backStyle));
		    theContent = makeComponent(request, wikiUtil, child, theContent,sdf2);
		} else {
		    theContent = content.toString();
		}
		contents.add(theContent);
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
	    } else if (flipCards) {
		String id = Utils.getGuid();
		sb.append(HU.open("div",HU.id(id)));
		sb.append(HU.div("",HU.id(id+"_header")));		
		for(String c: contents) {
		    sb.append(c);
		}
		sb.append(HU.close("div",HU.id(id)));
		getMapManager().addMapImports(request, sb);
		HU.script(sb, "Ramadda.Components.init(" + HU.squote(id)+",{});");
		return sb.toString();
            } else if (doingGrid) {
                boolean addHeader = getProperty(wikiUtil, props, "showDisplayHeader",
						false);

                List<String> weights = null;
                boolean showLine = getProperty(wikiUtil, props, "showLine",
					       getProperty(wikiUtil, props, "doline",
							   false));
                String ws = getProperty(wikiUtil, props, "weights",
                                        (String) null);
                if (ws != null) {
                    weights = Utils.split(ws, ",", true, true);
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
		String id = Utils.getGuid();
                sb.append(HU.open(HU.TAG_DIV, HU.id(id)));
		sb.append(HU.div("",HU.id(id+"_header")));		
                sb.append(HU.open(HU.TAG_DIV, HU.cssClass("ramadda-grid")+HU.id(id)));
                sb.append("\n");
		StringBuilder buff = new StringBuilder();

                int    rowCnt   = 0;
                int    colCnt   = 10000;
                int    weight   = 12 / columns;

                String boxClass = HU.cssClass("ramadda-gridbox ramadda-gridbox-decorated search-component");
                String boxStyle = "";
                width = getProperty(wikiUtil, props, ATTR_WIDTH,
                                    (String) null);
                if (width != null) {
                    boxStyle = HU.style(HU.css("width", HU.makeDim(width,"px"), "display","inline-block","margin","6px"));
                }
                for (int i = 0; i < titles.size(); i++) {
                    Entry child = children.get(i);
                    if (width == null) {
                        colCnt++;
                        if (colCnt >= columns) {
                            if (rowCnt > 0) {
                                buff.append(HU.close(HU.TAG_DIV));
                                if (showLine) {
                                    buff.append("<hr>");
                                } else {
                                    //                                buff.append(HU.br());
                                }
                            }
                            rowCnt++;
                            HU.open(buff, HU.TAG_DIV, HU.cssClass("row"));
                            colCnt = 0;
                        }
                        String weightString = "" + weight;
                        if ((weights != null) && (i < weights.size())) {
                            weightString = weights.get(i);
                        }
                        HU.open(buff, HU.TAG_DIV,
                                HU.cssClass("col-md-" + weightString
                                            + " ramadda-col"));
                    }
		    StringBuilder comp = new StringBuilder();

                    HU.open(comp, HU.TAG_DIV, boxClass + boxStyle);
                    if (showHeading) {
			String title  = titles.get(i);
			String label = title;
			if (showicon) {
			    label = HU.img(getPageHandler().getIconUrl(request,  child)) + " " + label;
			}
                        HU.div(comp, HU.href(urls.get(i), label),  HU.title(title) + headingClass);
                    }
                    String displayHtml = contents.get(i);
                    HU.div(comp, displayHtml,
                           HU.cssClass("bs-inner")
                           + HU.attr("style", innerStyle.toString()));
                    HU.close(comp, HU.TAG_DIV);
		    if(addHeader)
			buff.append(makeComponent(request, wikiUtil, child, comp.toString(),sdf2));
		    else
			buff.append(comp.toString());
                    if (width == null) {
                        HU.close(buff, HU.TAG_DIV);
                    }
                    buff.append("\n");
                }

                //Close the div if there was anything
                if (width == null) {
                    if (rowCnt > 0) {
                        HU.close(buff, HU.TAG_DIV);
                    }
                }

                //Close the grid div
                HU.close(buff, HU.TAG_DIV);
                HU.close(buff, HU.TAG_DIV);		
		sb.append(buff);
		if(addHeader) {
		    getMapManager().addMapImports(request, sb);
		    HU.script(sb, "Ramadda.Components.init(" + HU.squote(id)+");");
		}
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
                    HU.href("#", "<i style='font-size:32pt;'  class=\"ramadda-clickable  fas fa-angle-left prev\"/>");

                String nextImage =
                    HU.href("#", "<i style='font-size:32pt;' class=\"ramadda-clickable  fas fa-angle-right next\"/>");
                String arrowTDWidth = "20px";

                sb.append(
			  "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\"><tr>\n");
                HU.col(sb, prevImage,
                       "width=\"" + arrowTDWidth
                       + "\" style=\"font-size: 30px\"");
                HU.open(sb, HU.TAG_TD, HU.attr(HU.ATTR_WIDTH, width));
                HU.open(sb, HU.TAG_DIV, HU.cssClass("ramadda-slides-container slides_container"));
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
						 null, HU.getUniqueId(""), text, null, !showToolbar,
						 0,true);

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
                    buff.append(getEntryManager().getAjaxLink(request, e, e.getLabel()).toString());
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
            children = getEntryUtil().sortEntriesOnDate(children, true);

	    int skip = getProperty(wikiUtil,props,"skip",0);
	    if(skip>=1) {
		children = EntryUtil.applySkip(children,skip);
	    }
	    double sample = getProperty(wikiUtil,props,"sample",0.0);
	    if(sample>0) {
		children = EntryUtil.applySample(children,sample);
	    }	    


            imageOutputHandler.makePlayer(imageRequest, entry, children, sb,
                                          getProperty(wikiUtil, props,
						      "show_sort_links",
						      false), false);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_GALLERY)) {
            List<Entry> children = getEntries(request, wikiUtil,
					      originalEntry, entry, props);
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
	} else if(theTag.equals("loremipsum")) {
	    int count = getProperty(wikiUtil, props, "count",1);
	    String src  = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum";
	    for(int i=0;i<count && i<10;i++) {
		if(i>0) sb.append("<p>");
		sb.append(src);
	    }
	    return HU.div(sb.toString(),HU.style("font-style:italic;"));
        } else if (theTag.equals(WIKI_TAG_ROOT)) {
            return getRepository().getUrlBase();
        } else if (theTag.equals(WIKI_TAG_FULLTREE) || theTag.equals(WIKI_TAG_MENUTREE)) {
	    boolean doMenu = theTag.equals(WIKI_TAG_MENUTREE);
            int depth = getProperty(wikiUtil, props, "depth", 10);
            boolean addPrefix = getProperty(wikiUtil, props, "addPrefix",
                                            false);
            boolean showRoot = getProperty(wikiUtil, props, "showroot",
                                           false);
            boolean showIcon = getShowIcon(wikiUtil, props, false);
            List<String> types = Utils.split(getProperty(wikiUtil,
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
                   || theTag.equals(WIKI_TAG_TREE)
		   || theTag.equals(WIKI_TAG_TABLETREE)) {
            if (theTag.equals(WIKI_TAG_CHILDREN_GROUPS)) {
                props.put(ATTR_FOLDERS, "true");
            } else if (theTag.equals(WIKI_TAG_CHILDREN_ENTRIES)) {
                props.put(ATTR_FILES, "true");
            }
            List<Entry> children = getEntries(request, wikiUtil,
					      originalEntry, entry, props);
	    

	    if(!chunkDefined(request, wikiUtil,props)) {
		return  makeTableTree(request, wikiUtil,props,children);
	    }

	    List<List> chunkedEntries = getChunks(request,wikiUtil, props, children);
	    List<String> tds = new ArrayList<String>();
	    for(List entries: chunkedEntries) {
		tds.add(makeTableTree(request, wikiUtil,props,(List<Entry>)entries));
	    }

	    return makeChunks(request, wikiUtil, props, tds);
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
	    List<String> pre = null;
	    List<String> post = null;
	    String before = getProperty(wikiUtil, props,"linksBefore",null);
	    String after = getProperty(wikiUtil, props,"linksAfter",null);	    
	    if(before!=null) {
		pre = new ArrayList<String>();
		for(List<String> toks: Utils.multiSplit(before,",",";",2)) {
		    pre.add(HU.href(toks.get(0),toks.size()>1?toks.get(1):toks.get(0)));
		}
	    }
	    if(after!=null) {
		post = new ArrayList<String>();
		for(List<String> toks: Utils.multiSplit(after,",",";",2)) {
		    post.add(HU.href(toks.get(0),toks.size()>1?toks.get(1):toks.get(0)));
		}
	    }



	    if(!chunkDefined(request, wikiUtil,props)) {
		return makeLinks(request,originalEntry,entry, wikiUtil,props,isList, children,pre,post);
	    }
	    List<List> chunkedEntries = getChunks(request,wikiUtil, props, children);	    
	    List<String> tds = new ArrayList<String>();
	    for(List entries: chunkedEntries) {
		tds.add(makeLinks(request,originalEntry,entry, wikiUtil,props,isList, (List<Entry>)entries,pre,post));
		pre = null; post=null;
	    }
	    return makeChunks(request, wikiUtil, props, tds);
        } else {
            String fromTypeHandler =
                entry.getTypeHandler().getWikiInclude(wikiUtil, request,
						      originalEntry, entry, theTag, props);
            if (fromTypeHandler != null) {
                if (wikify) {
                    fromTypeHandler = wikifyEntry(request, entry,
						  fromTypeHandler, false, 
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
					       false, wikiUtil.getNotTags());
                }

                return fromProperty;
            }


            return null;
        }

        return null;
    }


    private String  makeLinks(Request request,Entry originalEntry, Entry entry, WikiUtil wikiUtil,Hashtable props,
			      boolean isList, List<Entry>children,List<String> pre, List<String> post) throws Exception {

	boolean highlightThis = getProperty(wikiUtil, props,
					    "highlightThis", false);
	boolean horizontal = getProperty(wikiUtil, props, "horizontal",
					 false);
	boolean decorate = getProperty(wikiUtil, props, "decorate",
				       false);
	boolean includeSnippet = getProperty(wikiUtil, props,
					     "showSnippet", getProperty(wikiUtil,props,"includeSnippet",false));
	boolean includeDescription = getProperty(wikiUtil, props,
						 "showDescription", false);	
	boolean showicon = getShowIcon(wikiUtil, props, false);

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


	if(decorate) {
	    tagOpen = "<div class=' ramadda-entry-nav-page  ramadda-entry-nav-page-decorated '><div class='ramadda-nav-page-label'>";
	    tagClose = "</div></div>";
	} else {
	    if (showicon) {
		tagOpen  = "<div class=ramadda-entry-link>";
		tagClose = "</div>";
	    }
	}

	if (horizontal) {
	    tagOpen  = "<div class='ramadda-links-horizontal'>";
	    tagClose = "</div>";
	}

	List<String> links = new ArrayList<String>();
	if(pre!=null) for(String s: pre)links.add("<li> " + s);
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
	    if (showicon) {
		linkLabel = HU.img(getPageHandler().getIconUrl(request,
							       child)) + HU.space(1) + linkLabel;
	    }
	    String snippet =  includeSnippet?getSnippet(request,  child, true,""):includeDescription?child.getDescription():null;

	    String href = HU.href(url, linkLabel,
				  HU.cssClass("ramadda-link " + cssClass)
				  + HU.style(style));
	    
	    if(decorate) {
		href=   "<div class=' ramadda-entry-nav-page  ramadda-entry-nav-page-decorated '><div class='ramadda-entry-nav-page-label'>" + href +"</div></div>";
	    }
	    if(snippet!=null) {
		href+=snippet;
	    }
	    boolean highlight =
		highlightThis && (originalEntry != null)
		&& child.getId().equals(originalEntry.getId());
	    if (highlight) {
		href = HU.span(href, HU.clazz("ramadda-links-highlight"));
	    }
	    StringBuilder link = new StringBuilder();
	    if(decorate) {
		link.append("<div class=ramadda-entry-nav-page-outer>");
		link.append(href);
		link.append("</div>");
	    } else {
		link.append(tagOpen);
		link.append(href);
		link.append(tagClose);
	    }
	    String s = link.toString();
	    links.add(s);
	}

	if(post!=null) for(String s: post)links.add("<li> " + s);
	if(decorate) {
	    return Utils.join(links,"",false);
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
    }

    private boolean chunkDefined(Request request, WikiUtil wikiUtil,Hashtable props) throws Exception {
	return getProperty(wikiUtil, props, "chunkSize",-1)>0 ||
	    getProperty(wikiUtil, props, "numChunks",-1)>0;	    

    }

    private List<List> getChunks(Request request, WikiUtil wikiUtil,Hashtable props, List<Entry> entries) throws Exception {
	int chunkSize = getProperty(wikiUtil, props, "chunkSize",0);
	int numChunks = getProperty(wikiUtil, props, "numChunks",0);	    
	int extra = 0;
	if(numChunks>0 && chunkSize==0) {
	    extra = entries.size()%numChunks;
	    chunkSize = (entries.size())/numChunks;
	}
	if(chunkSize==0) {
	    List<List> chunks = new ArrayList<List>();
	    chunks.add(entries);
	    return chunks;
	}
	return Utils.splitList(entries,chunkSize,extra);
    }


    private String makeChunks(Request request, WikiUtil wikiUtil, Hashtable props, List chunks) throws Exception {
	int columns = 0;
	String tmp = getProperty(wikiUtil, props, "chunkColumns",null);
	if(Utils.stringDefined(tmp)) {
	    if(tmp.equals("numChunks")) columns = chunks.size();
	    else columns = Integer.parseInt(tmp);
	}
	String chunkStyle =  getProperty(wikiUtil, props, "chunkStyle","");
	List<String> tds = new ArrayList<String>();
	for(Object chunk: chunks) {
	    tds.add(HU.div(chunk.toString(),HU.attrs("style",chunkStyle)));
	}
	StringBuilder buff = new StringBuilder();
	if(columns>0) 
	    return HU.table(tds,columns,"").toString();
	for(String s: tds) {
	    buff.append(s);
	}
	return buff.toString();
    }


    private  String makeComponent(Request request, WikiUtil wikiUtil,Entry child, String contents, SimpleDateFormat sdf2) throws Exception {
	String author = Utils.getDefined(child.getUser().getName(),child.getUserId());
	String compAttrs = 
	    HU.cssClass("ramadda-component") +
	    HU.attr("component-url",getEntryManager().getEntryURL(request, child)) +
	    HU.attr("component-title",child.getName()) +
	    HU.attr("component-date",sdf2.format(new Date(child.getStartDate()))) +
	    HU.attr("component-author",author);
	if (child.isImage()) {
	    String imageUrl = getRepository().getHtmlOutputHandler().getImageUrl(
										 request, child);
	    
	    compAttrs+=
		HU.attr("component-image",imageUrl);
	}

	if(child.hasLocationDefined()) {
	    compAttrs+=
		HU.attr("component-latitude",""+child.getLatitude()) +
		HU.attr("component-longitude",""+child.getLongitude());

	}
	List<Metadata> tagList = 
	    getMetadataManager().findMetadata(request, child,
					      new String[]{"enum_tag","content.keyword"}, false);
	if(tagList!=null) {
	    StringBuilder tags = null;
	    for(Metadata metadata: tagList) {
		String mtd = metadata.getAttr(1);
		if(tags==null) tags = new StringBuilder();
		else tags.append(",");
		tags.append(mtd);
	    }
	    if(tags!=null)
		compAttrs+=HU.attr("component-tags",tags.toString());
	}
	return HU.div(contents,compAttrs);
    }


    public String makeTableTree(Request request, WikiUtil wikiUtil, Hashtable props, List<Entry> children) throws Exception {
	if (children.size() == 0) {
	    return getMessage(wikiUtil, props, "No entries available");
	}
	if(wikiUtil==null)  wikiUtil = new WikiUtil();
	if(props==null) props = new Hashtable();
	StringBuilder sb = new StringBuilder();
        int max = request.get(ARG_MAX, -1);
        if (max == -1) {
            max = getProperty(wikiUtil, props, ATTR_MAX, -1);
        }
	getRepository().getHtmlOutputHandler().showNext(request,
							    children.size(), max,sb);


	String guid = Utils.getGuid().replaceAll("-","_");
	StringBuilder js = new StringBuilder();
	String var = "entries_" + guid;
	js.append("let " + var +"=");
	request.put("includeproperties","false");
	request.put("includedescription","false");
	request.put("includeservices","false");	    
	if(getProperty(wikiUtil, props, ARG_SHOWCRUMBS,false)) {
	    request.put("includecrumbs","true");	    
	}
	getRepository().getJsonOutputHandler().makeJson(request, children, js);
	js.append(";\n");
	HU.div(sb,"",HU.id(var));
	ArrayList<HtmlUtils.Selector> tfos =
	    new ArrayList<HtmlUtils.Selector>();
	List<Link> links = getRepository().getOutputLinks(
							  request,
							  new OutputHandler.State(
										  getEntryManager().getDummyGroup(
														  "action"), children));
	for (Link link : links) {
	    OutputType outputType = link.getOutputType();
	    if (outputType == null) {
		continue;
	    }
	    if(!outputType.getIsAction()) {
		if (!(outputType.getIsFile() || outputType.getIsEdit())) {
		    continue;
		}
	    }
	    String icon = link.getIcon();
	    if (icon == null) {
		icon = getRepository().getIconUrl(ICON_BLANK);
	    }
	    HtmlUtils.Selector selector = new HtmlUtils.Selector(outputType.getLabel(), outputType.getId(), icon, 20);
	    //A bit of a hack
	    if(selector.getId().equals("repository.copymovelink")) {
		tfos.add(0,selector);
	    } else {
		tfos.add(selector);
	    }
	}


	


	List<String> argProps = new ArrayList<String>();
	List<String> actions = new ArrayList<String>();
	for(HtmlUtils.Selector selector: tfos) {
	    actions.add(JsonUtil.mapAndQuote(Utils.makeList("id",selector.getId(),"label",selector.getLabel())));
	}

	for(String prop: new String[]{"maxHeight","details","simple","showHeader","showDate","showCreateDate","showSize",
				      "showType","showIcon","showThumbnails","showArrow","showForm","showCrumbs"}) {
	    String v =getProperty(wikiUtil, props, prop, (String)null);
	    if(v!=null) {
		argProps.add(prop);
		argProps.add(JsonUtil.quote(v));
	    }
	}

	if(children.size()>0) {
	    //Sample for access
	    Entry entry = children.get(0);
	    if(getAccessManager().canDoDelete(request, entry)) {
		argProps.add("canDelete");
		argProps.add("true");
	    }
	    if(getAccessManager().canDoExport(request, entry)) {
		argProps.add("canExport");
		argProps.add("true");
	    }	    
	}


	if (request.exists(ARG_ASCENDING)) {
	    argProps.add("ascending");
	    argProps.add(JsonUtil.quote(request.getString(ARG_ASCENDING,"true")));
	}
	if (request.exists(ARG_ORDERBY)) {
	    argProps.add("orderby");
	    argProps.add(JsonUtil.quote(request.getString(ARG_ORDERBY, ORDERBY_NAME)));
	}

	argProps.add("actions");
	argProps.add(JsonUtil.list(actions));
	String propArg = JsonUtil.map(argProps);
	js.append("\nRamadda.initEntryTable('" + var+"'," + propArg+"," + var+");\n");
	sb.append(HU.script(js.toString()));
	return sb.toString();
    }


    public boolean checkIf(WikiUtil wikiUtil, Request request,Entry entry, Hashtable props) {
	String column =  getProperty(wikiUtil, props, "if",null);
	if(column==null) return true;
	int idx =column.indexOf(":");
	String value = "true";
	if(idx>0) {
	    value = column.substring(idx+1);
	    column = column.substring(0,idx);

	} 
	//	System.err.println(column +"=" + value);
	Object obj = entry.getValue(column);
	if(obj==null) return true;
	return value.equals(obj.toString());
    }


    public boolean initWikiEditor(Request request, Appendable sb) throws Exception {
        if (request.getExtraProperty("didace") == null) {
            request.putExtraProperty("didace", "true");
            HtmlUtils.importJS(sb, getPageHandler().getCdnPath("/wiki.js"));
            HtmlUtils.importJS(sb, getPageHandler().getCdnPath("/lib/ace/src-min/ace.js"));
	    return true;
	}
	return false;
    }
	


    public String embedJson(Request request, String json) throws Exception {
        StringBuilder sb = new StringBuilder();
	HU.importJS(sb, getPageHandler().makeHtdocsUrl("/media/json.js"));
	String id = Utils.getGuid();
	//entry.getResource().getPath(), true);
	String formatted = JsonUtil.format(json,true);
	HtmlUtils.open(sb, "div", "id", id);
	HtmlUtils.pre(sb, formatted);
	HtmlUtils.close(sb, "div");
	sb.append(HtmlUtils.importJS(getRepository().getHtdocsUrl("/jsonutil.js")));
	sb.append(HtmlUtils.script("RamaddaJsonUtil.init('" + id + "');"));
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
								entry, new String[]{"wiki_label"}, true);
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

        boolean hideIfNoLocations = getProperty(wikiUtil, props, "hideIfNoLocations",false);

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

	
	if(hideIfNoLocations) {
	    boolean ok  = false;
	    for(Entry child: children) {
		ok = child.isGeoreferenced();
		if(ok) break;
	    }
	    if(!ok) {
		sb.append(getProperty(wikiUtil, props, ATTR_MESSAGE,""));
		return  null;
	    }
	}


        if (children == null || children.size() == 0) {
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
		"zoomLevel",
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
                    List<String> toks = Utils.splitUpTo(mapArg, ":", 2);
                    mapArg = toks.get(0);
                    key    = toks.get(1);
                }
                String v = getProperty(wikiUtil, props, key);
                if (v != null) {
                    v = v.replace("${entryid}", entry.getId());
                    mapProps.put(mapArg, JsonUtil.quote(v));
                }
            }

            String mapSet = getProperty(wikiUtil, props, "mapSettings",
                                        (String) null);
            if (mapSet != null) {
                List<String> msets = Utils.split(mapSet, ",");
                for (int i = 0; i < msets.size() - 1; i += 2) {
                    mapProps.put(msets.get(i), JsonUtil.quote(msets.get(i + 1)));
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
    public String getSnippet(Request request, Entry child, boolean wikify, String dflt)
	throws Exception {
        String snippet = getRawSnippet(request, child, wikify);
        if (snippet == null) {
            return dflt;
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
                snippet = wikifyEntry(request, child, snippet,false);
            }
            return snippet;
        }
        String text = child.getTypeHandler().getEntryText(child);
	if(text!=null) {
	    snippet = StringUtil.findPattern(text, "(?s)<snippet>(.*)</snippet>");
	    if (snippet == null) {
		snippet = StringUtil.findPattern(
						 text, "(?s)<snippet-hide>(.*)</snippet-hide>");
		if (snippet == null) {
		    snippet = StringUtil.findPattern(
						     text, "(?s)\\+snippet(.*?)-snippet");
		}
	    }
	}
        child.setSnippet(snippet);
        if ((snippet != null) && wikify) {
            snippet = wikifyEntry(request, child, snippet,false);
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
        boolean includeChildren = getProperty(wikiUtil, props, "includeChildren",
					      false);
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
            String snippet = getSnippet(request, entry, false,null);
            if (Utils.stringDefined(snippet)) {
                snippet = wikifyEntry(request, entry, snippet, false, 
                                      wikiUtil.getNotTags());
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
                imageUrl = getMetadataManager().getThumbnailUrl(request, entry);
            }
        }

	if(includeChildren) {
	    List<Entry> children = getEntryManager().getChildren(request,
								 entry);
	    if(children.size()>0) {
		//TODO: what to do with the children
		boolean showicon = getShowIcon(wikiUtil, props, false);
		String wiki = getProperty(wikiUtil, props,"childrenWiki",
					  "{{links showIcon=" + showicon+"}}"); 
		//		String  list = wikifyEntry(request,  entry, wiki, false,children);
		String  list = wikifyEntry(request,  entry, wiki, false);

		HU.div(card,list,HU.style(HU.css("max-height","200px","overflow-y","auto")));
		imageUrl = null;
	    }
	}



        if (imageUrl != null) {
            String img = HU.img(imageUrl, "",
				HU.cssClass("ramadda-card-image") 
                                +HU.attr("loading", "lazy")
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
		caption = getProperty(wikiUtil, props, "captionPrefix", "") +
		    caption +
		    getProperty(wikiUtil, props, "captionSuffix", "");
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
	    //	    card.append("<div class='ramadda-flip-card'><div class='ramadda-flip-card-inner'><div class='ramadda-flip-card-front'>");
            card.append(HU.div(inner, HU.cssClass("ramadda-imagewrap")));
	    //	    card.append("</div><div class='ramadda-flip-card-back'>");
	    //	    card.append("The back");
	    //	    card.append("</div></div></div>");
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
					  "$(document).ready(function() {\n HU.createFancyBox($(\"a.popup_image\"),"
					  + options.toString() + ");\n });\n");
            } else {
                buf.append(
			   HU.script(
				     "$(document).ready(function() {\n HU.createFancyBox($(\"a.popup_image\"),"
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
        return makeWikiUtil(request, false).wikify(wiki, this);
    }

    public void  makeCallout(Appendable sb,Request request,String contents) throws Exception {
	sb.append(wikify(request, "+callout-info\n" +
			 contents +"\n-calloutinfo\n:p\n"));
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
	    //In case we are making a snapshot
	    String url = (String)request.getExtraProperty(PROP_OVERRIDE_URL);
            if(url==null)
		url = getEntryManager().getEntryUrl(request, entry);
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
	newRequest.setEmbedded(true);
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
        boolean tags = getProperty(wikiUtil, props, "tags", false);
	if(tags)
	    request.put("tags","true");
        boolean showSearch = getProperty(wikiUtil, props, "showSearch", false);	

        if (metadataTypesAttr != null) {
            onlyTheseTypes = new ArrayList<String>();
            notTheseTypes  = new ArrayList<String>();
            for (String type :
		     Utils.split(metadataTypesAttr, ",", true, true)) {
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
        boolean decorate = getProperty(wikiUtil, props,
				       "decorate", false);
        boolean stripe = getProperty(wikiUtil, props,
				     "stripe", true);		


        for (TwoFacedObject tfo :
		 getRepository().getHtmlOutputHandler().getMetadataHtml(
									request, entry, onlyTheseTypes, notTheseTypes,
									includeTitle, separator, decorate,stripe)) {
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


    public Result processWikiUrl(Request request) throws Exception {
        Entry         entry = getEntryManager().getEntry(request);
	StringBuilder sb = new StringBuilder();
	if(entry==null) {
	    sb.append(JsonUtil.mapAndQuote(Utils.makeList("error", "Could not find entry")));
	    return new Result("", sb, JsonUtil.MIMETYPE);
	}
	Hashtable<String,String> props = new Hashtable<String,String>();
	String max = request.getString("max",null);
	if(max!=null) props.put("max",max);
	String jsonUrl = entry.getTypeHandler().getUrlForWiki(request,
							      entry, request.getString("tag",WikiConstants.WIKI_TAG_DISPLAY), props,null);
	jsonUrl = request.getAbsoluteUrl(jsonUrl);
	sb.append(JsonUtil.map(Utils.makeList("url", JsonUtil.quote(jsonUrl))));
	return new Result("", sb, JsonUtil.MIMETYPE);
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

	String remoteServer = getProperty(wikiUtil, props, "remoteServer", null);
	ServerInfo serverInfo = !Utils.stringDefined(remoteServer)?null:new ServerInfo(new URL(remoteServer),"","");
	if(serverInfo!=null) {
	    return getEntries(serverInfo, wikiUtil, originalEntry, entry, props, onlyImages, attrPrefix);
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
                Utils.split(filter.substring(FILTER_TYPE.length()), ";",
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
                Utils.split(filter.substring(FILTER_SUFFIX.length()),
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
                Utils.split(filter.substring(FILTER_ID.length()), ",",
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
     * main getEntries method
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

        String fromDate = getProperty(wikiUtil, props,
				      attrPrefix + ATTR_ENTRIES + ".fromDate",
				      (String) null);


        String toDate = getProperty(wikiUtil, props,
                                    attrPrefix + ATTR_ENTRIES + ".toDate",
                                    (String) null);
	
	//	System.err.println("date:" + fromDate +" " + toDate);
	if(Utils.stringDefined(fromDate) || Utils.stringDefined(toDate)) {
	    Date[] range = Utils.getDateRange(fromDate, toDate, new Date());
	    //	    System.err.println(range[0] +" " + range[1]);
	    List<Entry> tmp = new ArrayList<Entry>();
	    for(Entry e: entries) {
		if(range[0]!=null&& e.getStartDate()<range[0].getTime()) {
		    //		    System.err.println("too old:" + e);
		    continue;
		}
		if(range[1]!=null && e.getEndDate()>range[1].getTime()) {
		    //		    System.err.println("too young:" + e);
		    continue;
		}
		tmp.add(e);
	    }
	    entries=tmp;
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
            for (String id : Utils.split(excludeEntries, ",",true,true)) {
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
            sort = request.getString(ARG_ORDERBY, ORDERBY_NAME);
        }
        if (sort == null) {
            sort = getProperty(wikiUtil, props, attrPrefix + ATTR_SORT_BY,
			       getProperty(wikiUtil, props, attrPrefix + ATTR_SORT,
					   (String) null));
        }

        if (sort != null) {
            String dir = null;
            if (request.exists(ARG_ASCENDING)) {
                dir = request.get(ARG_ASCENDING, true)
		    ? "up"
		    : "down";

            }
            if (dir == null) {
                dir = getProperty(wikiUtil, props,  attrPrefix + ATTR_SORT_DIR,
				  getProperty(wikiUtil, props,  attrPrefix + ATTR_SORT_ORDER, null));
            }
            //If no dir specified then do ascending if we are sorting by name else do descending
            if (dir == null) {
                if ((sort.indexOf(ORDERBY_NAME) >= 0)
		    || (sort.indexOf(ORDERBY_ENTRYORDER) >= 0)) {
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
            List<String> ids = Utils.split(firstEntries, ",",true,true);
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
            List<String> ids = Utils.split(lastEntries, ",",true,true);
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
        Request myRequest = request.cloneMe();
	String prefix = getProperty(wikiUtil,props,"argPrefix","");
        int         max         =   getProperty(wikiUtil, props, ARG_MAX, -1);
        String      orderBy     = null;
        if (orderBy == null) {
            orderBy = getProperty(wikiUtil, props, "sort");
	    if (orderBy == null) {
		orderBy = getProperty(wikiUtil, props, "sortby");
	    }
        }

	boolean descending = getProperty(wikiUtil,props,ATTR_SORT_DIR,
					 getProperty(wikiUtil,props,ATTR_SORT_ORDER,"down")).equals("down");
        HashSet     nots        = new HashSet();

        for (String entryid : Utils.split(ids, ",", true, true)) {
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
                descending=  entryid.substring("entries.orderdir=".length()).equals("down");
                continue;
            }

            entryid = entryid.replace("_COMMA_", ",");


            Entry  theBaseEntry = baseEntry;
	    //	    System.err.println("the Base ENTRY:" + theBaseEntry);
            String type         = null;
	    //children;filter:
            //            entries="children:<other id>
            List<String> toks = Utils.splitUpTo(entryid, ":", 2);
	    //Handle the case like children:
	    if(toks.size()==1 && entryid.endsWith(":")) entryid = entryid.replace(":","");
	    else if (toks.size() == 2) {
                //TODO: handle specifying a type
                entryid = toks.get(0);
                String       suffix = toks.get(1);
                List<String> toks2  = Utils.splitUpTo(suffix, ":", 2);
                if (toks2.size() == 2) {}
                else {
		    if (suffix.equals(ID_THIS)) {
			theBaseEntry =  baseEntry;
		    } else {
			theBaseEntry = getEntryManager().getEntry(request,
								  suffix);
			if(theBaseEntry==null) {
			    throw new IllegalArgumentException("Could not find entry from:"+ suffix);
			}
		    }
                }
            }

            //            entries="children;type;type
            String filter = null;
            //            children;type=foo
            toks = Utils.splitUpTo(entryid, ";", 2);
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
                //                List<String> toks = Utils.splitUpTo(entryid, ID_REMOTE,  2);
                //                String url = toks.get(1);
                continue;
            }

            if (entryid.equals(ID_THIS)) {
                entries.addAll(applyFilter(request, wikiUtil, theBaseEntry,
                                           filter, props));

                continue;
            }



            if (entryid.startsWith(ATTR_ENTRIES + ".filter")) {
                List<String> tokens = Utils.splitUpTo(entryid, "=", 2);
                if (tokens.size() == 2) {
                    props.put(ATTR_ENTRIES + ".filter", tokens.get(1));
                }

                continue;
            }

            boolean isRemote = entryid.startsWith(ATTR_SEARCH_URL);
            if ( !isRemote && entryid.startsWith(ID_SEARCH + ".")) {

                List<String> tokens = Utils.splitUpTo(entryid, "=", 2);
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
			? Utils.splitUpTo(entryid,
					  "=", 2)
			: Utils.splitUpTo(entryid,
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

                entries.addAll(applyFilter(request, wikiUtil,
					   getSearchManager().doSearch(myRequest, new SearchInfo()),
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
		SelectInfo select = new SelectInfo(request, theBaseEntry, max,orderBy,!descending);
                List<Entry> children = getEntryManager().getChildren(myRequest,
								     theBaseEntry,select);
		entries.addAll(applyFilter(request, wikiUtil, children,filter, props));
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


        if (orderBy != null) {
            if (orderBy.equals(ORDERBY_DATE)) {
                entries = getEntryUtil().sortEntriesOnDate(entries, descending);
            } else if (orderBy.equals(ORDERBY_CREATEDATE)) {
                entries = getEntryUtil().sortEntriesOnCreateDate(entries,
								 descending);
            } else if (orderBy.equals(ORDERBY_NUMBER)) {
                entries = getEntryUtil().sortEntriesOnNumber(entries, descending);		
            } else {
                entries = getEntryUtil().sortEntriesOnName(entries, descending);
            }
        }

	max = request.get(ARG_MAX,max);
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
            ARG_TEXT, ARG_TYPE, ARG_GROUP, ARG_ANCESTOR,ARG_FILESUFFIX, ARG_BBOX,
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
                if (arg.equals(ARG_GROUP) || arg.equals(ARG_ANCESTOR)) {
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
        String width = getProperty(wikiUtil, props, ATTR_WIDTH, "100%");
        int serverImageWidth = getProperty(wikiUtil, props, ATTR_IMAGEWIDTH,
                                           -1);

        int     columns = getProperty(wikiUtil, props, ATTR_COLUMNS, 3);
        boolean decorate  = getProperty(wikiUtil, props, "decorate",true);
        boolean random  = getProperty(wikiUtil, props, ATTR_RANDOM, false);
        boolean popup   = getProperty(wikiUtil, props, ATTR_POPUP, true);
        boolean thumbnail = getProperty(wikiUtil, props, ATTR_THUMBNAIL,
                                        false);
        String caption = getProperty(wikiUtil, props, ATTR_CAPTION,
                                     "${name}");
	if(!Utils.stringDefined(caption)) caption=null;
        String popupCaption = getProperty(wikiUtil, props, "popupCaption",caption!=null?caption:"");
        String imageStyle = getProperty(wikiUtil, props, "imageStyle",null);
        String captionPos = getProperty(wikiUtil, props, ATTR_POPUPCAPTION,
                                        "none");
        String padding = getProperty(wikiUtil, props, "padding","10px");
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
		if(child.isImage()) {
		    url = child.getTypeHandler().getEntryResourceUrl(request,
								     child);
		    /*                url = HU.url(
				      request.makeUrl(repository.URL_ENTRY_GET) + "/"
				      + getStorageManager().getFileTail(child), ARG_ENTRYID,
				      child.getId());*/
		}
            }

	    if(url==null) {
		continue;
	    }
            if (serverImageWidth > 0) {
                url = url + "&" + ARG_IMAGEWIDTH + "=" + serverImageWidth;
            }


            String extra = "";
	    if(width.startsWith("-")) {
                extra = extra + HU.attr(HU.ATTR_WIDTH, "" + (width.substring(1)) + "%");
	    } else {
                extra = extra + HU.attr(HU.ATTR_WIDTH, "" + width);
            }
            String name       = getEntryDisplayName(child);
            String theCaption = caption;
	    if(theCaption!=null) {
		theCaption = theCaption.replace("${count}", "" + num);
		theCaption =
		    theCaption.replace("${date}",
				       formatDate(request,
						  new Date(child.getStartDate())));
		theCaption = theCaption.replace("${name}", child.getLabel());
		theCaption = theCaption.replace("${description}",
						child.getDescription());

	    }

            if ((name != null) && !name.isEmpty()) {
                extra = extra + HU.attr(HU.ATTR_ALT, name);
            }
            extra = extra + HU.attr("id", idPrefix + "img" + num) +
		HU.attr("loading","lazy");
            String img = HU.img(url, "", extra);

	    if(imageStyle!=null) {
		img = HU.div(img,HU.attrs("style",imageStyle));
	    }
            String entryUrl =
                request.entryUrl(getRepository().URL_ENTRY_SHOW, child);
	    if(decorate) {
		buff.append("<div class=\"image-outer search-component\">");
		buff.append("<div class=\"image-inner\">");
	    } else {
		buff.append("<div style='padding:" + HU.makeDim(padding,null)+";'>");
	    }
            if (popup) {
		String thePopupCaption = popupCaption;
		thePopupCaption = thePopupCaption.replace("${count}", "" + num);
		thePopupCaption =
		    thePopupCaption.replace("${date}",
					    formatDate(request,
						       new Date(child.getStartDate())));
		thePopupCaption = thePopupCaption.replace("${name}", child.getLabel());
		thePopupCaption = thePopupCaption.replace("${description}",
							  child.getDescription());
                String popupExtras = HU.cssClass("popup_image")
		    + HU.attr("width", "100%");
		//                if ( !captionPos.equals("none")) {
		if(theCaption!=null)
		    popupExtras += HU.attr("title", theCaption);
		//                }
		String dataCaption = HU.href(entryUrl,thePopupCaption).replace("\"","'");
                popupExtras += HU.attr("data-fancybox", idPrefix)
		    + HU.attr("data-caption", dataCaption);
		String popupUrl = child.getResource().isImage()?child.getTypeHandler().getEntryResourceUrl(request, child):url;
                buff.append(
			    HU.href(
				    popupUrl, HU.div(
						     img,
						     HU.attr(
							     "id", idPrefix + "div" + num)), popupExtras));
            } else {
                buff.append(img);
            }
	    if(decorate) {
		buff.append("</div>");
	    }


	    if(theCaption!=null) {
		theCaption = HU.href(entryUrl, theCaption,
				     HU.style("color:#666;font-size:10pt;"));
		buff.append(HU.div(theCaption, HU.cssClass("image-caption")));
	    }
            if (showDesc) {
                if (Utils.stringDefined(child.getDescription())) {
                    buff.append("<div class=\"image-description\">");
                    buff.append(child.getDescription());
                    buff.append("</div>");
                }
            }
	    if(decorate) {
		buff.append("</div>");
	    } else {
		buff.append("</div>");
	    }

        }
        int    colInt   = 12 / Math.min(12, columns);
        String colClass = "col-md-" + colInt;
        HU.open(sb, "div", HU.cssClass("row"));
        for (StringBuilder buff : colsSB) {
            HU.open(sb, "div",
                    HU.cssClass(colClass + " ramadda-col")
                    + (decorate?HU.style("padding-left:5px; padding-right:5px;"):HU.style("padding-left:0px; padding-right:0px;")));
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

    public XmlOutputHandler getXmlOutputHandler() throws Exception {
        return getRepository().getXmlOutputHandler();
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

    public ZipFileOutputHandler getZipFileOutputHandler() {
        try {
            return (ZipFileOutputHandler) getRepository().getOutputHandler(
									   ZipFileOutputHandler.OUTPUT_LIST);
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
                                    String tag, Hashtable props, String remainder) {
        try {

            if ( !tag.equals(WIKI_TAG_IMPORT)) {
                String include = my_getWikiInclude(wikiUtil, request,
						   originalEntry, importEntry, tag, props,remainder,
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
	    myRequest.setSessionId(request.getSessionId());

            for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
                Object key = keys.nextElement();
                myRequest.put(key, props.get(key));
            }

	    if(!getProperty(wikiUtil,props,"showTitle",true)) {
		myRequest.put(PROP_SHOW_TITLE,"false");
	    }

            OutputType outputType = handler.findOutputType(tag);
            myRequest.put(ARG_ENTRYID, importEntry.getId());
            myRequest.put(ARG_OUTPUT, outputType.getId());
	    myRequest.setEmbedded(true);

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
            addDisplayImports(request, buttons,true);
        }
        StringBuilder tags1  = new StringBuilder();
        StringBuilder tags2 = new StringBuilder();
        StringBuilder tags3 = new StringBuilder();
        StringBuilder tags4 = new StringBuilder();
	Utils.TriFunction<String,String,String,String> l = (title,pre,post)->{
	    return getWikiEditLink(textAreaId,title,pre,post,"");
	};

	Utils.QuadFunction<String,String,String,String,String> l2 = (title,pre,post,tt)->{
	    return getWikiEditLink(textAreaId,title,pre,post,tt);
	};
	
        Utils.appendAll(tags1,
			l.call("Section", "+section title={{name}}_newline__newline_", "-section"),
			l.call( "Frame", "+frame background=#fff frameSize=0 shadow title=_title_", "-frame"),
			l.call( "Note", "+note_newline__newline_", "-note"),
			l.call( "Table", "+table height=400 hover=true cellborder=false rowborder=false stripe=false ordering=false paging=false searching=false_newline_:tr &quot;heading 1&quot; &quot;heading 2&quot;_newline_+tr_newline_:td colum 1_newline_+td_newline_column 2_newline_", "-td_newline_-tr_newline_-table"),
			l.call( "Row/Column", "+row_newline_+col-6_newline_", "-col_newline_+col-6_newline_-col_newline_-row"),
			l.call( "Left-right", "+leftright_nl_+left_nl_-left_nl_+right_nl_-right_nl_", "-leftright"),
			l.call( "Left-middle-right", "+leftmiddleright_nl_+left_nl_-left_nl_+middle_nl_-middle_nl_+right_nl_-right_nl_", "-leftmiddleright"),
			l.call( "Tabs", "+tabs_newline_+tab tab title_newline_", "-tab_newline_-tabs_newline_"),
			l.call( "Accordion", "+accordion decorate=false collapsible=true activeSegment=0 _newline_+segment segment  title_newline_", "-segment_newline_-accordion_newline_"),
			l.call( "Slides", "+slides dots=true slidesToShow=1  bigArrow=true style=_qt__qt__nl_+slide Title_nl_", "-slide_nl_-slides_nl_"),
			l.call("Grid box", "+grid #decorated=true #columns=_qt_1fr 2fr_qt_ _nl_:filler_nl_+gridbox #flex=1 #style=_qt__qt_ #width=_qt__qt_ #title=_qt_Title 1_qt__nl_-gridbox_nl_+gridbox #title=_qt_Title 2_qt__nl_-gridbox_nl_:filler_nl_", "-grid"),
			l.call("Scroll panels","+scroll_newline_+panel color=gradient1 name=home style=_quote__quote_ _newline_+center_newline_<div class=scroll-indicator>Scroll Down</div>_newline_-center_newline_-panel_newline_+panel color=gradient2 name=panel1_newline__newline_-panel_newline_+panel color=blue name=panel2_newline__newline_-panel_newline_", "-scroll") 
			
			); 

        Utils.appendAll(tags2,
			l.call("Popup", "+popup link=_qt_Link_qt_ icon=_qt_fa-solid fa-arrow-right-from-bracket_qt_ title=_qt_Title_qt_ header=true draggable=true decorate=true sticky=true my=_qt__qt_ at=_qt__qt_ animate=false_nl__nl_", "-popup_nl_"),
			l.call("Menu", "+menu_nl_    :menuheader Header_nl_    :menuitem Item 1_nl_    +menu Menu 1_nl_        :menuitem Item 2_nl_        +menuitem style=_qt_width:300px; _qt_ _nl_        Menu contents_nl_        -menuitem_nl_    -menu_nl_    +menu Menu 2_nl_        :menuitem Item 3_nl_    -menu_nl_-menu", ""),
			l.call("Navigation left", ":navleft leftStyle=_qt_width:250px;_qt_ rightStyle=_qt__qt_  maxLevel=_qt_4_qt_", ""),
			l.call("Navigation top", ":navtop style=_quote__quote_ delimiter=_quote_|_quote_  maxLevel=_qt__qt_", ""),
			l.call("Navigation popup", ":navpopup align=right|left  maxLevel=_qt__qt_", ""),	    
			l.call("Prev arrow", "{{prev position=relative|fixed decorate=false iconSize=32 sort=name,entryorder sortAscending=true style=_qt_left:250px;_qt_  showName=false}}", ""),
			l.call("Next arrow", "{{next position=relative|fixed decorate=false iconSize=32 sort=name,entryorder sortAscending=true style=_dq_  showName=false}}", ""),
			l.call("Draggable", "+draggable framed=true header=_quote__quote_ style=_quote_background:#fff;_quote_ toggle=_quote_true_quote_ toggleVisible=_quote_true_quote__newline_",
			       "-draggable"),
			l.call("Expandable",   "+expandable header=_quote_quote_ expand=true_newline_", "-expandable"),
			l.call("Fullscreen",   "+fullscreen", "-fullscreen"),
			l.call("Inset", "+inset top=0 bottom=0 left=0 right=0 _newline_", "-inset"),
			l.call("Absolute", "\\n+absolute top= bottom= left= right=\\n","-absolute"),
			l.call("Relative", "\\n+relative\\n","-relative"),
			l.call("Center", "\\n+center\\n","-center")
			);





        Utils.appendAll(tags3, l.call( "Note", "+note style=\"\" _nl__nl_", "-note"));
        String[] colors = new String[] {"gray",  "yellow"};
        for (String color : colors) {
            tags3.append(
			 getWikiEditLink(
					 textAreaId, HU.div(
							    "Note "
							    + color, HU.attrs(
									      "style", "padding:2px; display:inline-block;", "class", "ramadda-background-"
									      + color)), 
					 "+note-" + color + "_nl__nl_", "-note",""));
        }

        Utils.appendAll(tags3, l.call( "Box", "+box style=\"\" _nl__nl_", "-box"));
        for (String color : colors) {
            tags3.append(
			 getWikiEditLink(
					 textAreaId, HU.div(
							    "Box "
							    + color, HU.attrs(
									      "style", "padding:2px; display:inline-block;", "class", "ramadda-background-"
									      + color)), "+box-" + color
					 + "_nl__nl_", "-box",""));
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
	    return HU.makePopup(null,HU.div(title,HU.cssClass("ramadda-menubar-button")),
				HU.div(contents, "class='wiki-editor-popup'"),
				new NamedValue("linkAttributes", buttonClass));
	};

        String previewButton =
	    HU.href("#", "Preview",
		    HU.attrs("id", textAreaId+"_previewbutton", "xstyle", "padding:5px;",
			     "xclass",
			     "ramadda-menubar-button ramadda-menubar-button-last"))
	    +   HU.div("",HU.attrs("id", textAreaId+"_preview", "class", "wiki-editor-preview"));

        String colorButton =
	    HU.href("#", "Color",
		    HU.attrs("id", textAreaId+"_color", "xstyle", "padding:5px;",
			     "xclass",
			     "ramadda-menubar-button ramadda-menubar-button-last"));


        String wcButton =
	    HU.href("#", "Word Count",
		    HU.attrs("id", textAreaId+"_wordcount", "xstyle", "padding:5px;",
			     "xclass",
			     "ramadda-menubar-button ramadda-menubar-button-last"));



	if(getSearchManager().isGptEnabled()) {
	    help.append(HU.href("#", "GPT",
				HU.attrs("id", textAreaId+"_rewrite")));
	    help.append("<br>");
	}
	help.append(Utils.join("<br>",previewButton, colorButton, wcButton) +"<div class=ramadda-thin-hr></div><b>Help</b><br>");


	BiConsumer<String,String> makeHelp = (p,title)->{
	    help.append(HU.href(getRepository().getUrlBase()
				+ p, title,
				"target=_help") + "<br>");
	};

        for (String extraHelp :
		 Utils.split(request.getString("extrahelp", ""), ",",
			     true, true)) {
            List<String> toks = Utils.splitUpTo(extraHelp, "|", 2);
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



        String helpButton = makeButton.apply("Etc...", help.toString());
        String formattingButton = makeButton.apply("Formatting",
						   HU.hbox(tags1, tags2,tags3,tags4));

        StringBuilder misc1 = new StringBuilder();
        StringBuilder misc2 = new StringBuilder();
        StringBuilder misc3 = new StringBuilder();
        StringBuilder misc4 = new StringBuilder();			
        Utils.appendAll(misc4,
                        l.call( "Callout", "+callout_nl__nl_", "-callout"),
                        l.call( "Callout info", "+callout-info_nl__nl_", "-callout"),
                        l.call( "Callout tip", "+callout-tip_nl__nl_", "-callout"),
                        l.call( "Callout question", "+callout-question_nl__nl_", "-callout"),
                        l.call( "Callout warning", "+callout-warning_nl__nl_", "-callout"),
                        l.call( "Text Balloon", "+balloon-left avatar=true #width=400px #style=\"background:#fffeec;\"_nl__nl_", "-balloon"),
                        l.call( "Skip", "+skip_nl__nl_", "-skip"));
	Utils.appendAll(misc3,
			l.call( "Macro", ":macro name value_nl_${name}_nl_", ""),
			l.call( "Template", "+template template_name_nl_... ${var1} ... ${var2}_nl_", "-template"),
			l.call( "Apply template", "+apply template_name_nl_:var var1 Some value_nl_+var var2_nl_Some other value_nl_..._nl_-var_nl_", "-apply"),
			l.call( "Inline apply", ":apply template_name var1=\"some value\" var2=\"Some other value\"", ""),
			l.call( "CSS", "+css_newline_", "-css"),
			l.call( "PRE", "+pre_newline_", "-pre"),
                        l.call( "Xml", "+xml addCopy=true addDownload=true downloadFile=download.xml_nl__nl_", "-xml"),
			l.call( "Javascript", "+js_newline_", "-js"),
			l.call( "Code", "```_newline__newline_", "```"),
			l.call( "Property", "{{property name=value", "}}"));

        Utils.appendAll(misc1,
			l.call( "Title", ":title {{name link=true}}", ""),
			l.call( "Heading", ":heading your heading", ""),
			l.call( "Heading-1", ":h1 your heading", ""),
			l.call( "Heading-2", ":h2 your heading", ""),
			l.call( "Heading-3", ":h3 your heading", ""),	    
			l.call("Break", "\\n:br", ""),
			l.call("Paragraph", "\\n:p", ""),
			l.call("Bold text", "\\'\\'\\'", "\\'\\'\\'"),
			l.call("Italic text", "\\'\\'", "\\'\\'"),
			l.call("Code", "```\\n", "\\n```"));
        Utils.appendAll(misc2,
			l2.call("Internal link", "[[", "]]", "Link title"),
			l2.call("External link", "[", "]", "http://www.example.com link title"),
			l2.call("Small text", "<small>", "</small>", "Small text"),
			l.call("Horizontal line", "\\n----\\n", ""),
			l.call("Button", ":button url label", ""),
			l.call("Remark", "\\n:rem ", ""),
			l.call("Draft", "+draft", "-draft"),
			l.call("Reload", "\\n:reload seconds=30 showCheckbox=true showLabel=true", ""),
			l2.call("After", "+after pause=0 afterFade=5000_newline__newline_", "-after", "After"),
			l.call("Odometer", "{{odometer initCount=0 count=100 immediate=true pause=1000}}", ""));
	

        String textButton = makeButton.apply("Misc",
					     HU.hbox(misc1, misc2,misc3,misc4));

        String entriesButton = makeButton.apply("Entries",
						makeTagsMenu(entry,textAreaId));
        String displaysButton = HU.href(
					"javascript:noop()", "Displays",
					HU.attrs(
						 "id", "displays_button" + textAreaId,
						 "class",
						 "ramadda-menubar-button"));


        String addEntry = OutputHandler.getSelect(request, textAreaId,
						  "Entry ID", true, "entryid", entry, false,
						  buttonClass);



        String importEntry = OutputHandler.getSelect(request, textAreaId,
						     "Import Entry", true, "wikilink", entry, false,
						     buttonClass);

        String fieldLink = OutputHandler.getSelect(request, textAreaId,
						   "Field ID", true, "fieldname", entry, false,
						   buttonClass);

        HU.open(buttons, "div",
                HU.cssClass("ramadda-menubar")
                + HU.attrs("id", textAreaId + "_toolbar"));
        Utils.appendAll(buttons, HU.span("", HU.id(textAreaId + "_prefix")),
                        formattingButton, textButton, entriesButton);
        if (fromTypeBuff != null) {
	    buttons.append(HU.makePopup(null,entry.getTypeHandler().getLabel() + " tags",
					HU.div(fromTypeBuff.toString(), "class='wiki-editor-popup'"),
					new NamedValue("linkAttributes", buttonClass)));
        }

        Utils.appendAll(buttons, importEntry, displaysButton,  addEntry, 
                        fieldLink);

        if (entry != null) {
            entry.getTypeHandler().addToWikiToolbar(request, entry, buttons,
						    textAreaId);
        }

        buttons.append(helpButton);
	//        buttons.append(previewButton);
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
    public String makeTagsMenu(Entry entry,String textAreaId) {
        StringBuilder sb     = new StringBuilder();
        String        inset  = "&nbsp;&nbsp;";
        int           rowCnt = 0;
	List<String[]> fromType = new ArrayList<String[]>();
	if(entry!=null) {
	    entry.getTypeHandler().getWikiTags(fromType,entry);
	}
	sb.append("<table border=0><tr valign=top>\n");
	if(fromType.size()>0) {
            sb.append("<td valign=top>");
            sb.append(HU.b(entry.getTypeHandler().getLabel()));
            sb.append(HU.br());
	    for(String[]tuple: fromType) {
                String  textToInsert = tuple[1].replace("\n","\\n");
                String js2 = "javascript:insertTags(" + HU.squote(textAreaId)
		    + "," + HU.squote("{{" + textToInsert + " ")
		    + "," + HU.squote("}}") + "," + HU.squote("")
		    + ");";
                sb.append(inset);
                sb.append(HU.href(js2, tuple[0]));
                sb.append(HU.br());
                sb.append("\n");
	    }
            sb.append("</td>");
	}

	sb.append("<td valign=top>\n");
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
        return HU.div(HU.href(js, label),HU.cssClass("wiki-editor-popup-link"));
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

	    if(src.startsWith("/") || HU.isFontAwesome(src)) return src;
            Entry   entry      = (Entry) wikiUtil.getProperty(ATTR_ENTRY);
            Request request    = (Request) wikiUtil.getProperty(ATTR_REQUEST);
            Entry   srcEntry   = null;
            String  attachment = null;

            int     idx        = src.indexOf("::");
            if (idx >= 0) {
                List<String> toks = Utils.splitUpTo(src, "::", 2);
                if (toks.size() == 2) {
                    src        = toks.get(0);
                    attachment = toks.get(1);
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
		     getMetadataManager().getMetadata(request,srcEntry)) {
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
       Implements from WikiPageHandler interface
       Checks for hasChildrenOfType=<some entry type>
    */
    public boolean ifBlockOk(WikiUtil wikiUtil, String attrs, StringBuilder ifBuffer)  {
	try {
	    Entry   entry   = (Entry) wikiUtil.getProperty(ATTR_ENTRY);
	    if(entry==null) return true;
	    Request request    = (Request) wikiUtil.getProperty(ATTR_REQUEST);
	    Hashtable props = HU.parseHtmlProperties(attrs);
	    String ofType = Utils.getProperty(props,"hasChildrenOfType",null);
	    if(ofType!=null) {
		List<String> types = Utils.split(ofType,",",true,true);
		for(Entry child: getEntryManager().getChildren(request, entry)) {
		    for(String t: types) {
			if(child.getTypeHandler().isType(t)) {
			    return true;
			}
		    }
		}
		return false;
	    }
	    return true;
	} catch(Exception exc) {
	    throw new RuntimeException(exc);
	}
    }

    public boolean titleOk(WikiUtil wikiUtil) {
	if(Misc.equals(wikiUtil.getProperty(PROP_SHOW_TITLE),"false")) {
	    return false;
	}
	return true;
    }



    /** methods that implement SystemContext*/
    public void putSystemContextCache(String key, String value) {
	key = Utils.makeID(key);
	try {
	    getStorageManager().putCacheObject("SystemContext",key,value);
	} catch(Exception exc) {
	    throw new RuntimeException(exc);
	}
    }

    public String getSystemContextCache(String key,long ttl) {
	key = Utils.makeID(key);
	try {
	    return (String) getStorageManager().getCacheObject("SystemContex",key,ttl);
	} catch(Exception exc) {
	    throw new RuntimeException(exc);
	}
    }

    public String getSystemContextProperty(String key, String dflt) {
	return getRepository().getProperty(key,dflt);
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
                List<String> foo = Utils.split(name, "#");
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

            if (theEntry == null) {
		List<Entry> entries =  getEntryManager().getEntriesFromAlias(request,name);
		if(entries.size()>0) {
		    theEntry = entries.get(0);
		}
	    }


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
            content = entry.getStringValue(0, entry.getDescription());
            wikify  = true;
        } else {
            content = entry.getDescription();
        }

        if (wikify) {
            if ( !originalEntry.equals(entry)) {
                content = wikifyEntry(request, entry, content, false);
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

	String wikiText = entry.getTypeHandler().getWikiText(request, entry,WIKI_TAG_SIMPLE);
	if(wikiText!=null) {
	    return wikifyEntry(request, entry, wikiText);
	}

        boolean sizeConstrained = getProperty(wikiUtil, props,
					      ATTR_CONSTRAINSIZE, false);
        String content = getDescription(request, wikiUtil, props,
                                        originalEntry, entry);

	if(TypeHandler.isWikiText(content)) {
	    content = getSnippet(request, entry, true,"");
	}

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
            String position = request.getString(ATTR_TEXTPOSITION, POS_TOP);
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
        content = HU.div(content, HU.cssClass("entry-simple"));
        return content;

    }

    public String makeMapPopup(Request request, WikiUtil wikiUtil,
                                    Hashtable props, Entry originalEntry,
                                    Entry entry)
	throws Exception {
	String template = entry.getTypeHandler().getBubbleTemplate( request,  entry);
	if(template!=null)
	    return wikifyEntry(request, entry, template);
	return wikifyEntry(request, entry, "+section title={{name}}\n{{simple}}\n-section");
	//	return  makeSimpleDisplay(request,  wikiUtil, props, originalEntry, entry);
    }




    private String getGroupVar(Request request) {
	groupCount++;
	if(groupCount>1000000) groupCount=0;
	String var = "displayManager" + groupCount;
	request.putExtraProperty(PROP_GROUP_VAR, var);
	return var;
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
					      new String[]{"point_chart_wiki"}, true);
        if ((metadataList != null) && (metadataList.size() > 0)) {
            wiki.append(metadataList.get(0).getAttr1());
        } else {
            String fromEntry = typeHandler.getProperty(entry, "chart.wiki",
						       null);
            if (fromEntry != null) {
                wiki.append(fromEntry);
            } else {
                wiki.append(
			    "{{group  showMenu=true  layoutType=columns  layoutColumns=2  }}\n");
                String chartType = (recordTypeHandler == null)
		    ? typeHandler.getProperty(entry,
					      "chart.type", "linechart")
		    : recordTypeHandler.getChartProperty(
							 request, entry, "chart.type",
							 "linechart");
                wiki.append(
			    "{{display_" + chartType +"   xwidth=600  height=400   layoutHere=false showMenu=true  showTitle=false  row=0  column=0  }}");
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
				"{{display_map  width=\"600\"  height=\"400\"   "
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

        boolean isNotebook =   displayType.equals("notebook");	


        boolean isMap = displayType.equals("map") ||
	    displayType.equals("editablemap") ||
	    displayType.equals("imdv") ||
	    displayType.equals("entrylist") || isNotebook;
	//	System.err.println("type:" + displayType +" map:" + isMap);
	if(displayType.equals("editablemap")||displayType.equals("imdv")) {
	    HU.importJS(sb, getPageHandler().getCdnPath("/lib/here.js"));
	}

        this.addDisplayImports(request, sb, true);


	if(isNotebook) {
	    initWikiEditor(request,  sb);
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
					      new String[]{"wikiattribute"}, true);
        if (metadataAttrs != null) {
            for (Metadata metadata : metadataAttrs) {
                String attrName = metadata.getAttr1();
                if (props.get(attrName) == null) {
                    propList.add(attrName);
                    propList.add(JsonUtil.quote(metadata.getAttr2()));
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
            List<String> toks = Utils.split(derived, ",", true, true);

            List<String> jsonObjects = new ArrayList<String>();
            for (String tok : toks) {
                List<String> toks2 = Utils.split(tok, ":", true, true);
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
                jsonObjects.add(JsonUtil.mapAndQuote(jObj).replaceAll("\n", " "));
            }
            String json = JsonUtil.list(jsonObjects);
            //            System.err.println("json:" + json);
            props.put("derived", json);
        }

        String providers = getProperty(wikiUtil, props, "providers");
        if (providers != null) {
            List<String> processed = new ArrayList<String>();
	    HashSet seen = new HashSet();
            for (String tok : Utils.split(providers, ",")) {
                //                System.err.println ("Tok:" + tok);
                if (tok.startsWith("name:") || tok.startsWith("category:")) {
                    boolean doName  = tok.startsWith("name:");
                    String  pattern = tok.substring(doName
						    ? "name:".length()
						    : "category:".length());
                    //                    System.err.println ("doName:" + doName +" pattern:" + pattern);
                    for (SearchProvider searchProvider :
			     getSearchManager().getSearchProviders()) {
			if(seen.contains(searchProvider.getId())) continue;
			seen.add(searchProvider.getId());
                        String  target  = doName
			    ? searchProvider.getName()
			    : searchProvider.getCategory();
                        boolean include = target.equals(pattern);
                        if ( !include) {
                            try {
                                include = target.matches(pattern);
                            } catch (Exception ignore) {
                                System.err.println("bad pattern:" + pattern);
                            }
                        }

                        if (include) {
                            String icon = searchProvider.getSearchProviderIconUrl();
                            if (icon == null) {
                                icon = "${root}/icons/magnifier.png";
                            }
                            icon = getPageHandler().applyBaseMacros(icon);
			    String v =JsonUtil.map(Utils.makeList("id",JsonUtil.quote(searchProvider.getId()),
								  "type",JsonUtil.quote(searchProvider.getType()),
								  "name",JsonUtil.quote(searchProvider.getName()),
								  "capabilities",JsonUtil.quote(searchProvider.getCapabilities()),					       
								  "icon",JsonUtil.quote(icon),
								  "category",JsonUtil.quote(searchProvider.getCategory())));
                            processed.add(v);
                        }
                    }

                    continue;
                }

                List<String> subToks = Utils.split(tok, ":", true, true);
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
		if(!seen.contains(id)) {
		    seen.add(id);
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
		    String v =JsonUtil.map(Utils.makeList("id",JsonUtil.quote(id),
							  "type",JsonUtil.quote(searchProvider.getType()),
							  "name",JsonUtil.quote(label),
							  "capabilities",JsonUtil.quote(searchProvider.getCapabilities()),					       
							  "icon",JsonUtil.quote(icon),
							  "category",JsonUtil.quote(searchProvider.getCategory())));
		    processed.add(v);
		}
	    }
            props.put("providers", "json:"+ JsonUtil.list(processed));
        }

        String entryParent = getProperty(wikiUtil, props, "entryParent");
        if (entryParent != null) {
            Entry theEntry = findEntryFromId(request, entry, null,props,
                                             entryParent);
            if (theEntry != null) {
                props.put("entryParent", theEntry.getId());
            }
        }


        if (!request.isAnonymous()) {
            props.put("user", request.getUser().getId());
        }

        if (getAccessManager().canDoEdit(request, entry)) {
            props.put("canEdit", "true");
	    String hereKey = GeoUtils.getHereKey();
	    String googleKey = GeoUtils.getGoogleKey();	
	    if(hereKey!=null) {
		props.put("hereRoutingEnabled","true");
	    }
	    if(googleKey!=null) {
		props.put("googleRoutingEnabled","true");
	    }	    
	}

        if (!request.isAnonymous()) {
            props.put("user", request.getUser().getId());
        }	

        String colors = getProperty(wikiUtil, props, ATTR_COLORS);
        if (colors != null) {
            propList.add(ATTR_COLORS);
            propList.add(JsonUtil.list(Utils.split(colors, ","), true));
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
			 JsonUtil.quote(
					entry.getTypeHandler().getEntryIconUrl(
									       request, originalEntry)));
        }



        String timezone = getEntryUtil().getTimezone(request, entry);
        if (timezone != null) {
            propList.add("timezone");
            TimeZone tz = TimeZone.getTimeZone(timezone);
            propList.add(JsonUtil.quote("" + (tz.getRawOffset() / 1000 / 60
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
            propList.add(JsonUtil.quote(title));
        } else {
            propList.add(ATTR_TITLE);
            propList.add(JsonUtil.quote(entry.getName()));
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
	    Entry first  = getEntryUtil().findEntry(children,  getProperty(wikiUtil, props,"firstEntry"));
            StringBuilder tmp = new StringBuilder();
	    if(first!=null)
		tmp.append(first.getId() + ":"
                           + first.getName().replaceAll(",", " ").replaceAll("\"","&quot;"));
	    
            for (Entry child : children) {
		if(first!=null && first.equals(child)) continue;
                if (tmp.length() > 0) {
                    tmp.append(",");
                }
                tmp.append(child.getId() + ":"
                           + child.getName().replaceAll(",", " ").replaceAll("\"","&quot;"));
            }
            propList.add("entryCollection");
            propList.add(JsonUtil.quote(tmp.toString()));
            String tmpname = getProperty(wikiUtil, props,
                                         "changeEntriesLabel");
            if (tmpname != null) {
                propList.add("changeEntriesLabel");
                propList.add(JsonUtil.quote(tmpname));
            }
        }

        topProps.add("layoutType");
        topProps.add(JsonUtil.quote(getProperty(wikiUtil, props, "layoutType",
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
            Utils.add(propList, "bounds", JsonUtil.quote(bounds));
        } else if (entry.hasAreaDefined()) {
            Utils.add(propList, "bounds",
                      JsonUtil.quote(entry.getNorth() + "," + entry.getWest()
				     + "," + entry.getSouth() + ","
				     + entry.getEast()));
        }

        topProps.add("defaultMapLayer");
        topProps.add(JsonUtil.quote(defaultLayer));

        String displayDiv = getProperty(wikiUtil, props, "displayDiv");
        if (displayDiv != null) {
            displayDiv = displayDiv.replace("${entryid}", entry.getId());
            Utils.add(propList, "displayDiv", JsonUtil.quote(displayDiv));
            props.remove("displayDiv");
        }

	if(entry.getParentEntry()!=null) {
	    topProps.add("parentEntryId");
	    topProps.add(HU.quote(entry.getParentEntry().getId()));
	}

        if ( !request.isAnonymous()) {
	    String sessionId = request.getSessionId();
	    if(sessionId!=null) {
		String authToken = RepositoryUtil.hashString(sessionId);
		topProps.add("authToken");
		topProps.add(HU.quote(authToken));
	    }
	}


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
                topProps.add(JsonUtil.quote(value.toString()));
            }
            HU.div(sb, "", HU.id(mainDivId));
	    String groupVar = getGroupVar(request);
            topProps.addAll(propList);
            js.append("\nvar " + groupVar +" = getOrCreateDisplayManager("
                      + HU.quote(mainDivId) + "," + JsonUtil.map(topProps)
                      + ",true);\n");
            wikiUtil.appendJavascript(js.toString());
            return;
        } 


        String fields = getProperty(wikiUtil, props, "fields", (String) null);
        if (fields != null) {
            List<String> toks = Utils.split(fields, ",", true, true);
            if (toks.size() > 0) {
                propList.add("fields");
                propList.add(JsonUtil.list(toks, true));
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

	String style = "position:relative;" + getProperty(wikiUtil, props, "outerDisplayStyle","");
        HU.div(sb, "",
               HU.clazz("display-container") + HU.id(anotherDivId)
               + HU.style(style));
        Utils.add(propList, "divid", JsonUtil.quote(anotherDivId));
        props.remove("layoutHere");


	String groupVar = (String) request.getExtraProperty(PROP_GROUP_VAR);
	boolean needToCreateGroup = groupVar == null;

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
                Utils.add(propList, arg, JsonUtil.quote(value));
            }
            props.remove(arg);
        }


        //Only add the default layer to the display if its been specified
        defaultLayer = getProperty(wikiUtil, props, "defaultLayer",
                                   (String) null);



        //If its a map then check for the default layer
        if (isMap) {
            List<Metadata> layers =
                getMetadataManager().findMetadata(request, entry,
						  new String[]{"map_layer"}, true);
            if ((layers != null) && (layers.size() > 0)) {
                defaultLayer = layers.get(0).getAttr1();
            }

	    getMapManager().addMapMarkerMetadata(request, entry, propList);

	}

	if (defaultLayer != null) {
            Utils.add(propList, "defaultMapLayer", JsonUtil.quote(defaultLayer));
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
            Utils.add(propList, "entryIds", JsonUtil.quote(ids.toString()));
        }
        props.remove("type");

        for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
            Object key   = keys.nextElement();
            String value = props.get(key).toString();
            //      System.err.println ("adding:" + key +"=" + value);
	    if(value.startsWith("json:")) {
		value = value.substring(5);
	    } else {
		value = JsonUtil.quote(value);
	    }
	    Utils.add(propList, key, value);
        }


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

        if (needToCreateGroup) {
	    groupVar = getGroupVar(request);
            Utils.concatBuff(
			     js, "\nvar " + groupVar +" = getOrCreateDisplayManager(",
			     HU.quote(groupDivId), ",", JsonUtil.map(topProps), ");\n");
        }
        Utils.add(propList, "entryId", HU.quote(entry.getId()),"thisEntryType",HU.quote(entry.getTypeHandler().getType()));
	if(entry.isFile()) {
            String fileUrl = entry.getTypeHandler().getEntryResourceUrl(request,  entry);
	    Utils.add(propList,"fileUrl",HU.quote(fileUrl));
	}


        if ((pointDataUrl != null)
	    && getProperty(wikiUtil, props, "includeData", true)) {
            Utils.add(propList, "data",
                      Utils.concatString("new PointData(", HU.quote(name),
                                         ",  null,null,\n",
                                         HU.quote(pointDataUrl), ",",
                                         "\n{entryId:'", entry.getId(), "'}",
                                         ")"));
        }

        if (isMap) {
            List<Metadata> metadataList =
                getMetadataManager().findMetadata(request, entry,
						  new String[]{"map_displaymap"}, true);
            if ((metadataList != null) && (metadataList.size() > 0)) {
                String kmlIds       = null;
                String geojsonIds   = null;
                String kmlNames     = null;
                String annotatedIds     = null;		
                String annotatedNames     = null;		
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
			     .isType("geo_shapefile") || mapEntry.getTypeHandler().isType("geo_geojson") ||
			     mapEntry.getTypeHandler().isType("geo_editable_json"))) {
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
		    } else  if (mapEntry.getTypeHandler().isType("geo_editable_json")) {
                        if (annotatedIds == null) {
                            annotatedIds   = mapEntry.getId();
                            annotatedNames = mapEntry.getName().replaceAll(",",
									   " ");
                        } else {
                            annotatedIds += "," + mapEntry.getId();
                            annotatedNames += ","
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

		    if(annotatedIds!=null) {
			Utils.add(propList, "annotationLayer", JsonUtil.quote(annotatedIds),
				  "annotationLayerName", JsonUtil.quote(annotatedNames));
		    }
			
		    if(props.get("kmlLayer")==null && props.get("geojsonLayer")==null) {
			if (kmlIds != null) {
			    Utils.add(propList, "kmlLayer", JsonUtil.quote(kmlIds),
				      "kmlLayerName", JsonUtil.quote(kmlNames));
			}
			if (geojsonIds != null) {
			    Utils.add(propList, "geojsonLayer",
				      JsonUtil.quote(geojsonIds), "geojsonLayerName",
				      JsonUtil.quote(geojsonNames));
			}
                    }
                }
            }
	}

        wikiUtil.addWikiAttributes(propList);
	js.append("\n");
	js.append(groupVar+".createDisplay(" + HU.quote(displayType)
                  + "," + JsonUtil.map(propList) + ");\n");
	//xxxx
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
					    "/display/widgets.js",
					    "/display/display.js",
					    "/display/displaymanager.js",
					    "/display/displayentry.js",
					    "/display/displaymap.js",
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


    private ServerInfo getServer(Request request, Entry entry,
				 WikiUtil wikiUtil, Hashtable props) throws Exception {
	ServerInfo server = entry!=null?entry.getRemoteServer():null;
	if(server!=null) return server;
	String remoteServer = getProperty(wikiUtil, props, "remoteServer", null);
	return  !Utils.stringDefined(remoteServer)?null:new ServerInfo(new URL(remoteServer),"","");

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
