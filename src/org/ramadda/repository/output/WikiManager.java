/*
 * Copyright (c) 2008-2023 Geode Systems LLC
 * SPDX-License-Identifier: Apache-2.0
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
import org.ramadda.util.LabeledObject;
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
public class WikiManager extends RepositoryManager
    implements OutputConstants,WikiConstants, WikiPageHandler, SystemContext {


    /** output type */
    public static final OutputType OUTPUT_WIKI = new OutputType("Wiki",
								"wiki.view",
								OutputType.TYPE_VIEW,
								"", ICON_WIKI);



    //max number of entries in fulltree/menutree
    private static final int ENTRY_TREE_MAX_COUNT = 1000;
    private static final int ENTRY_TREE_MAX_DEPTH = 10;    

    private int groupCount = 0;
    
    private String wikiMenuFormattingButton;
    private String wikiMenuTagsButton;
    private String wikiMenuEtcButton;
    private String wikiMenuHelpButton;



    /** _more_ */
    private String displayImports;
    private String displayInits;


    /** _more_ */
    private Hashtable<String, String> wikiMacros;



    private WikiUtil dummyWikiUtil = new WikiUtil();

    private Hashtable<String,WikiTagHandler> tagHandlers =  new Hashtable<String,WikiTagHandler>();


    private boolean defaultTableFormOpen = false;

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
	defaultTableFormOpen =  getRepository().getProperty("ramadda.wiki.table.formopen",false);

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
			    getProperty(wikiUtil, props,"showicon",dflt));
    }

    public String applyTemplate(Request request,  Entry entry, String template,String...args) throws Exception {
	template = template.replace("${icon}",getPageHandler().getEntryIconImage(request, entry));
	for(int i=0;i<args.length;i+=2) {
	    template = template.replace(args[i],args[i+1]);
	}


	return template;
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
	    getLogManager().logError("Processing wiki tag:" +tag,exc);
	    return wikiUtil.wikiError(new StringBuilder(),"Error processing tag:" + tag +" " + exc.getMessage());
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

	if(wikiContent.indexOf("${wikiproperty.")>=0) {
	    boolean debug = getRepository().getProperty("wikiproperty.debug",false);
	    List<Utils.Macro> macros = Utils.splitMacros(wikiContent,"${wikiproperty.","}");
	    StringBuilder sb = new StringBuilder();
	    if(debug)
		System.err.println("checking wiki property for entry:" + entry);
	    for(Utils.Macro macro: macros) {
		if(macro.isText()) {
		    sb.append(macro.getText());
		    continue;
		}

		String  v = null;
		List<Metadata> metadataList =
		    getMetadataManager().findMetadata(request, entry,
						      new String[]{"wikiproperty"}, true);

		if ((metadataList != null) && (metadataList.size() > 0)) {
		    for(Metadata metadata: metadataList) {
			if(("wikiproperty."+metadata.getAttr1().trim()).equals(macro.getText())) {
			    v = metadata.getAttr2();
			    break;
			}
		    }
		}

		if(v==null) {
		    v = getRepository().getProperty(macro.getText(),null);
		}

		if(v==null) {
		    v = macro.getProperty("default","");
		    if(debug)
			System.err.println("\twiki property:" + macro.getText()+ " using default:" + v);
		} else {
		    if(debug)
			System.err.println("\twiki property:" + macro.getText()+ " got value:" + v);
		}

		if(v!=null) {
		    sb.append(v);
		    continue;
		}
	    }
	    wikiContent=sb.toString();
	}

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
                         new WikiUtil(Misc.newHashtable(new Object[] {ATTR_ENTRY, entry })), entry);

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
	    return messageError("An error occurred:" + exc);
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



    private SelectInfo getSelectFromString(Request request, Entry entry, WikiUtil wikiUtil,
					   Hashtable props,
					   String string) throws Exception {
	Request myRequest = request.cloneMe();

	List<String> toks = Utils.split(string,";",true,true);
	String orderBy = null;//ORDERBY_FROMDATE;
	Boolean ascending = null;
	String name=null;
        String filter = getProperty(wikiUtil, props,
				    ATTR_ENTRIES + ".filter",
				    (String) null);
	SelectInfo select =  new SelectInfo(myRequest);
	select.setEntry(entry);
	for(String tok: toks) {
	    List<String> pair = Utils.splitUpTo(tok,":",2);
	    if(pair.size()!=2) {
		if(pair.size()>0) {
		    //check for the case of, e.g. children:entryid
		    String id = pair.get(0);
		    Entry newEntry = findEntryFromId(request,  entry, wikiUtil, props, id);
		    if(newEntry!=null) {
			entry=newEntry;
			select.setEntry(entry);
			continue;
		    }
		}
		System.err.println("WikiManager.getSelectFromString - bad specifier:" + tok);
		continue;
		
	    }
	    
	    String what = pair.get(0).trim();
	    String value = pair.get(1).trim();		    
	    //	    System.err.println("\twhat:" + what +"="+value);
		
	    if(what.equals(ARG_TYPE)) {
		select.setType(value);
		myRequest.put(ARG_TYPE, value);
	    } else if(what.equals(ARG_ORDERBY)) {
		orderBy=value;
		select.setOrderBy(orderBy);
	    } else if(what.equals("filter")) {
		filter = value;
	    } else if(what.equals(ARG_MAX)) {
		int max = Integer.parseInt(value);
		myRequest.put(ARG_MAX,""+max);
		select.setMax(max);
	    } else if(what.equals(ARG_NAME)) {
		name  =value;
		myRequest.put(ARG_TEXT,"name:"+value);
	    } else if(what.equals(ARG_TEXT)) {
		name  =value;
		myRequest.put(ARG_TEXT,value);
	    } else if(what.equals(ARG_SIZE_MIN)) {
		myRequest.put(ARG_SIZE_MIN,value);
	    } else if(what.equals(ARG_SIZE_MAX)) {
		myRequest.put(ARG_SIZE_MAX,value);								
	    } else if(what.equals(ARG_ASCENDING)) {
		ascending = value.length()==0|| value.equals("true");			
		select.setAscending(ascending);
	    } else if(what.equals(ARG_DESCENDING)) {
		select.setAscending(ascending = value.equals("false"));
	    } else if(what.equals("sortdir")) {
		if(value.equals("up") || value.equals("ascending")) 
		    select.setAscending(ascending = true);
		else if(value.equals("down") || value.equals("descending")) 
		    select.setAscending(ascending = false);
		else System.err.println("Unknown sort dir:" + what);
	    } else if(what.equals("entry")) {
		entry = findEntryFromId(request,  entry, wikiUtil, props, value);
		select.setEntry(entry);
		if(entry==null)
		    System.err.println("WikiManager.getSelectFromString - null entry with value:" + value);
	    } else if(what.equals(ARG_DESCENDENT) || what.equals(ARG_ANCESTOR)) {
		if(value.length()==0) {
		    myRequest.put(ARG_ANCESTOR,entry.getId());
		} else {
		    Entry otherEntry = findEntryFromId(request,  entry, wikiUtil, props, value);
		    if(otherEntry!=null) {
			myRequest.put(ARG_ANCESTOR,otherEntry.getId());
		    } else {
			System.err.println("Could not find descendent entry:" + tok);
		    }
		}
	    }   else {
		System.err.println("Unknown child specifier:" + what +"=" + value);
	    }
	}		
	if(orderBy!=null)
	    myRequest.put(ARG_ORDERBY,orderBy);
	if(ascending!=null) 
	    myRequest.put(ATTR_SORT_DIR,ascending?"up":"down");
	if(filter!=null) select.setFilter(filter);
	return select;
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

	SelectInfo select = null;

	//Check for an alias:
	if(wikiUtil!=null) {
	    theEntry =(Entry) wikiUtil.getWikiProperty("entry:" + entryId);
	    if(theEntry!=null) return theEntry;
	}

	Utils.TriFunction<SelectInfo,String,String,String> matches = getIdMatcher(request, entry,wikiUtil,props);

        int   barIndex = entryId.indexOf("|");
        if (barIndex >= 0) {
            entryId = entryId.substring(0, barIndex);
        }
        if (entryId.equals(ID_THIS)) {
	    return entry;
        }

        if (entryId.equals(ID_ROOT)) {
            return  request.getRootEntry();
        }



	ServerInfo serverInfo = getServer(request, entry, wikiUtil, props);
	if(serverInfo!=null) {
	    return findEntryFromId(serverInfo, entry, wikiUtil, props, entryId);
	}

        if (entryId.startsWith(PREFIX_ALIAS)) {
            String alias = Utils.clip(entryId,PREFIX_ALIAS);
            return getEntryManager().getEntryFromAlias(request, alias);
        }


	if((select = matches.call(entryId,ID_CHILD,PREFIX_CHILD))!=null) { 
	    List<Entry> children =  getEntryManager().getChildren(request,select.getEntry(),select);
            if (children.size() > 0) {
                return children.get(0);
            }
	    return null;
	}


	if((select = matches.call(entryId,ID_GRANDCHILD,PREFIX_GRANDCHILD))!=null) { 
	    SelectInfo select2 = new SelectInfo(request);
	    select2.setOrderBy(select.getOrderBy());
	    select2.setAscending(select.getAscending());	    
	    List<Entry> children =  getEntryManager().getChildren(request,select.getEntry(),select2);
	    if (children.size() == 0) {
		return null;
	    }
	    List<Entry> all = new ArrayList<Entry>();
	    select2.setType(select.getType());
	    for(Entry child: children) {
		all.addAll(getEntryManager().getChildren(request,child,select2));
	    }
	    all = getEntryManager().applyFilter(request, all,select);
	    if(all.size()>0) return all.get(0);
	    return null;
        }


	/*
	  ancestor:type
	*/
	if((select = matches.call(entryId,ID_ANCESTOR,PREFIX_ANCESTOR))!=null) { 
            Entry  lastEntry = select.getEntry();
            Entry  current   = select.getEntry();
	    String type = select.getType();
            while (true) {
                Entry parent = current.getParentEntry();
                if (parent == null) {
                    break;
                }
		if(type!=null) {
		    if (parent.getTypeHandler().isType(type)) {
			lastEntry = parent;
		    }
		} else {
		    lastEntry = parent;
		}
                current = parent;
            }
            return lastEntry;
        }


	if((select = matches.call(entryId,ID_LINK,PREFIX_LINK))!=null) { 		
            String type = select.getType();
            List<Association> associations =
                getRepository().getAssociationManager().getAssociations(
									request, select.getEntry().getId());
            for (Association association : associations) {
                Entry otherEntry =
                    getAssociationManager().getOtherEntry(request,
							  association, entry);
                if (otherEntry == null) {
                    continue;
                }
                if ((type != null)
		    && !otherEntry.getTypeHandler().isType(type)) {
                    System.err.println("not type");
                    continue;
                }
                return  otherEntry;
            }
	    return null;
        }


	if((select = matches.call(entryId,ID_PARENT,PREFIX_PARENT))!=null) { 		
            return  getEntryManager().getEntry(request,
					       select.getEntry().getParentEntryId());
        }

	if((select = matches.call(entryId,ID_GRANDPARENT,PREFIX_GRANDPARENT))!=null) { 
            return  getEntryManager().getEntry(request,
					       select.getEntry().getParentEntryId());
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
	if(!entryId.equals(ID_SEARCH) && !entryId.startsWith(PREFIX_SEARCH)) {
	    return null;
	}
	/*
	  entry=search:ancestor;type:some_type;orderby:
	*/
	SelectInfo select = getSelectFromString(request, entry, wikiUtil,
						props,entryId.equals(ID_SEARCH)?"":
						Utils.clip(entryId,PREFIX_SEARCH));
	//xxxxx
	//	System.err.println("R:" + select.getRequest().format());
	List<Entry> entries=  getSearchManager().doSearch(select.getRequest(),select);
	return getEntryManager().applyFilter(request, entries, select);
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

        boolean       screenshot = getProperty(wikiUtil, props, "screenshot", false);
	if(width!=null && width.equals("screenshot")) {
	    screenshot = true;
	    width=null;
	}

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
                                         "#ccc");

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

	if(screenshot) {
            HU.attr(extra,"onload","HtmlUtils.initScreenshot(this);this.onload=null;");
	    style+="display:none;";
	}


        if (style.length() > 0) {
            HU.attr(extra,"style", style);
        }


        String captionPosition = getProperty(wikiUtil, props, "captionPosition","bottom");
        String caption = getProperty(wikiUtil, props, "caption",
                                     (String) null);
        if (caption != null) {
	    caption=entry.getTypeHandler().processDisplayTemplate(request,  entry,caption);
	}

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
        if (caption != null && captionPosition.equals("top")) {
            HU.span(sb, caption, HU.cssClass("wiki-image-caption"));
            sb.append(HU.br());
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


        if (caption != null && captionPosition.equals("bottom")) {
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
	if(entry==null) {
	    return getMessage(wikiUtil, props, msg("No image entry"));
	}
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

    public String makeErrorMessage(Request requests, WikiUtil wikiUtil,Hashtable props,
				   String tag,
				   String dfltMsg) {
	String msg = getMessage(wikiUtil, props,  null);
	if(msg!=null) return msg;
	if(dfltMsg==null) dfltMsg = "Could not process tag:" + tag;
	return HU.span(dfltMsg,HU.cssClass("ramadda-wiki-error"));
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
                                HU.span("Could not process tag: " + tag,HU.cssClass("ramadda-wiki-error")));
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
            prefix = Utils.convertPattern(prefix).replace("\\n","\n");;
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
        for (int i = 0; i < WikiTags.WIKITAGS.length; i++) {
            WikiTags.WikiTagCategory cat = WikiTags.WIKITAGS[i];
            for (int tagIdx = 0; tagIdx < cat.tags.length; tagIdx++) {
                WikiTags.WikiTag      tag = cat.tags[tagIdx];
                List<String> tmp = new ArrayList<String>();
		String label = Utils.makeLabel(tag.label) + " properties";
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
        String wiki = request.getUnsafeString("wikitext", "");
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
        Result result = new Result("", new StringBuilder(wiki));
        result.setShouldDecorate(false);

	/*
	  if(true) {
	    Result result2 = new Result("", new StringBuilder("XXXX"));
	    result2.setShouldDecorate(false);
	    return result2;
	    }*/
	




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
            File f = getStorageManager().getTmpFile("notebook.json");
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

	    String menus = getProperty(wikiUtil,props,"menus",null);
	    List<LabeledObject> extras = new ArrayList<LabeledObject>();
	    if(menus!=null) {
		extras.add(new LabeledObject(getProperty(wikiUtil,props,"menusTitle","Tools"),
					     wikifyEntry(request, entry,"<div class=ramadda-entry-tools>\n{{menu  showLabel=false title=\"\"  popup=false   menus=\""+ menus+"\"}}\n</div>"))); 
	    }
            return getHtmlOutputHandler().getInformationTabs(myRequest, entry, false,extras);
        } else if (theTag.equals(WIKI_TAG_FA)) {
	    String icon=
		getProperty(wikiUtil, props, "icon", "");
	    String style=
		getProperty(wikiUtil, props, "style", "");		
	    String tmp =  HU.tag("i",HU.attrs("class",icon,"style",style),"");
	    return tmp;
		
        } else if (theTag.equals(WIKI_TAG_CAPTION)
                   || theTag.equals(WIKI_TAG_IMAGE2)) {}
        else if (theTag.equals(WIKI_TAG_TAGCLOUD)) {
            StringBuilder tagCloud = new StringBuilder();
            int threshold = getProperty(wikiUtil, props, "threshold", 0);
            getMetadataManager().doMakeTagCloudOrList(request,
						      getProperty(wikiUtil, props, "type", ""), tagCloud, true,
						      threshold);

            return tagCloud.toString();
	} else if(theTag.equals(WIKI_TAG_SEARCHBYTYPE)) {
	    HashSet<String> supers = null;
	    HashSet<String> cats=null;
	    HashSet<String> types=null;
	    String listStyle="";
	    String width = getProperty(wikiUtil,props,"width",null);
	    if(width!=null) {
		width = HU.makeDim(width,"px");
		listStyle+=HU.css("width",width,"max-width",width);
	    }
	    String height = getProperty(wikiUtil,props,"height",null);
	    if(height!=null) {
		height = HU.makeDim(height,"px");
		listStyle+=HU.css("xheight",height,"max-height",height);
	    }	    
	    boolean showHeader = getProperty(wikiUtil,props,"showHeader",true);
	    boolean showSearchField = getProperty(wikiUtil,props,"showSearchField",true);
	    if(props.get("supers")!=null) supers =
					      (HashSet<String>) Utils.makeHashSet(Utils.split(props.get("supers"),",",true,true));
	    if(props.get("cats")!=null) cats =
					      (HashSet<String>) Utils.makeHashSet(Utils.split(props.get("cats"),",",true,true));	    
	    if(props.get("types")!=null) types =
					      (HashSet<String>) Utils.makeHashSet(Utils.split(props.get("types"),",",true,true));	    
	    getSearchManager().addSearchByTypeList(request, sb,showHeader,showSearchField,listStyle,supers,cats,types);
	    return sb.toString();
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
        } else if (theTag.equals("share")) {
	    //from: https://viima.github.io/jquery-social-share-bar/
	    StringBuilder css = new StringBuilder(".js-share > .fab, .js-share > .fas, .js-share > .fa {color:white; font-size:16px;}\n");
	    css.append(".sharing-providers > li > a, .sharing-providers > li {width:30px; height:30px; font-size:16px;}\n");
	    css.append(".sharing-providers > li > a {line-height:24px;}\n");
	    css.append(".ramadda-share {margin-left:5px;margin-right:5px;}\n");
	    if(getProperty(wikiUtil,props,"horizontal",false)) {
		css.append(".sharing-providers > li {display:inline-block;}\n");
		css.append(".share-bar {top:initial;bottom:10px; left: 50%; transform: translateX(-50%);}\n");
		css.append(".share-bar.right {right: initial;}\n");
		css.append(".share-bar.left {left:50%;}\n");
	    }
	    if(getProperty(wikiUtil,props,"here",false)) {
		css.append(".share-bar {transform:initial;left:initial;bottom:initial;top:initial;display:inline-block;position:relative;}\n");
		css.append(".share-bar.left {left:initial;}\n");
	    }
	    //	    System.err.println(css);
	    sb.append(HU.importJS(getRepository().getHtdocsUrl("/lib/share/jquery-social-share-bar.js")));
	    sb.append(HU.cssLink(getRepository().getHtdocsUrl("/lib/share/jquery-social-share-bar.css")));	    
	    sb.append(HU.importCss(css.toString()));
	    String style = getProperty(wikiUtil, props, "style", "");
	    List<String> args  = new ArrayList<String>();

	    String tmp;
	    if((tmp=getProperty(wikiUtil,props,"title",null))!=null) {
		Utils.add(args,"pageTitle",JsonUtil.quote(tmp));
	    }
	    if((tmp=getProperty(wikiUtil,props,"url",null))!=null) {
		Utils.add(args,"pageUrl",JsonUtil.quote(tmp));
	    }
	    if((tmp=getProperty(wikiUtil,props,"desc",null))!=null) {
		Utils.add(args,"pageDesc",JsonUtil.quote(tmp));
	    }
	    if((tmp=getProperty(wikiUtil,props,"animate",null))!=null) {
		Utils.add(args,"animate",tmp);
	    }	    	    
	    
	    Utils.add(args,"position",JsonUtil.quote(getProperty(wikiUtil, props, "position", "right")));
	    Utils.add(args,"theme",JsonUtil.quote(getProperty(wikiUtil, props, "theme", "square")));
	    Utils.add(args,"animate",""+getProperty(wikiUtil, props, "animate",true));
	    String channels = "facebook,twitter,reddit,linkedin,pinterest,email";

	    Utils.add(args,"channels",JsonUtil.list(JsonUtil.quote(Utils.split(
									       getProperty(wikiUtil, props, "channels", channels),",",true,true))));
	    String id = HU.getUniqueId("share_");
	    HU.div(sb,"",HU.attrs("id",id,"class","ramadda-share share-bar","style",style));
	    StringBuilder js = new StringBuilder();
	    js.append("$('#" + id +"').share(" +JsonUtil.map(args)+");\n");
	    HU.script(sb,js.toString());
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
        } else if (theTag.equals(WIKI_TAG_QRCODE)) {
	    sb.append(HU.importJS(getRepository().getHtdocsUrl("/lib/qrcode.js")));
	    String id = HU.getUniqueId("qrcode_");
            String url = getProperty(wikiUtil, props, "url",
				     request.getAbsoluteUrl(request.entryUrl(getRepository().URL_ENTRY_SHOW, entry)));
	    HU.href(sb,url,    HU.div("",HU.attr("id",id)),HU.attr("target","_link"));
	    String width = getProperty(wikiUtil,props,"width","128");
	    String height = getProperty(wikiUtil,props,"height",width);
	    String js  = "new QRCode(" + HU.squote(id)+","+
		JsonUtil.map("text",JsonUtil.quote(url),"width",width,
			     "height",height,
			     "colorDark",JsonUtil.quote(getProperty(wikiUtil,props,"colorDark","#000000")),
			     "colorLight",JsonUtil.quote(getProperty(wikiUtil,props,"colorLight","#ffffff"))) +");\n";
							       
	    HU.script(sb,js);
	    return sb.toString();
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
		String buttonClass= getProperty(wikiUtil, props, "buttonClass",""); 
                return HU.href(
			       url, title,
			       HU.cssClass("ramadda-button " + buttonClass)
			       + HU.attr("role", "button"));
            } else {
                return HU.href(url, title);
            }
        } else if (theTag.equals(WIKI_TAG_VERSION)) {
	    return RepositoryUtil.getVersion();
        } else if (theTag.equals(WIKI_TAG_MAKELABEL)) {
	    return Utils.makeLabel(remainder);
        } else if (theTag.equals(WIKI_TAG_MEDIA)) {
            if ( !entry.getResource().isDefined()) {
                return  getProperty(wikiUtil, props, ATTR_MESSAGE,"");
	    }
	    boolean full   = getProperty(wikiUtil, props, "full",false);
	    boolean popup   = getProperty(wikiUtil, props, ATTR_POPUP, true);
	    String popupCaption = getProperty(wikiUtil, props, "popupCaption","");
	    if (popup) {
		addImagePopupJS(request, wikiUtil, sb, props);
	    }
	    String width = getProperty(wikiUtil, props,"width", "100%");
	    String height = getProperty(wikiUtil, props,"height", (String) null);
	    String path   = entry.getResource().getPath();
	    String _path   = path.toLowerCase();
	    String imageUrl = null;
	    if(entry.isImage()) {
		imageUrl = entry.getTypeHandler().getEntryResourceUrl(request, entry);
	    }

	    if(_path.indexOf(".pdf")>=0) {
		imageUrl = getMetadataManager().getThumbnailUrl(request, entry);
		if(full || imageUrl==null) {
		    String pdfUrl = entry.getTypeHandler().getEntryResourceUrl(request, entry);
		    return HU.getPdfEmbed(pdfUrl,props);
		}
	    }		


	    if(imageUrl!=null) {
		String image =  HU.img(imageUrl, "", HU.attr("width", width));
		if (popup) {
		    String entryUrl = request.entryUrl(getRepository().URL_ENTRY_SHOW,
						       entry);
		    String popupExtras = HU.cssClass("popup_image")
			+ HU.attr("width", "100%");
		    String dataCaption = HU.href(entryUrl,entry.getName()).replace("\"","'");
		    String idPrefix = "gallery";
		    popupExtras += HU.attr("data-fancybox", idPrefix) +
			HU.attr("data-caption", dataCaption);			
		    String popupUrl = imageUrl;
		    image =  HU.href(popupUrl, HU.div(image,
						      HU.attr(
							      "id", idPrefix + "div5")), popupExtras);

		}
		return image;
	    }



	    if(entry.getResource().isUrl()) {
		StringBuilder buff =new StringBuilder();
		wikiUtil.embedMedia(buff,entry.getResource().getPath(), props);
		return buff.toString();
	    }
	    if(entry.getResource().isFile()) {
		String mediaUrl = entry.getTypeHandler().getEntryResourceUrl(request, entry);
		String embed = HU.getMediaEmbed(mediaUrl,width,height);
		if(embed!=null) return embed;
	    }




	    return  getProperty(wikiUtil, props, ATTR_MESSAGE,"");
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
                                 HU.attr(HU.ATTR_WIDTH, ICON_WIDTH));
                } else {
                    img = HU.img(typeHandler.getIconUrl(icon),"", HU.attr(HU.ATTR_WIDTH, ICON_WIDTH));
                }
            }

            String label = getProperty(wikiUtil, props, ATTR_TITLE,
                                       typeHandler.getLabel());

            return HU
                .href(request
		      .makeUrl(getRepository().URL_ENTRY_FORM, ARG_GROUP,
			       group.getId(), EntryManager.ARG_TYPE,
			       typeHandler.getType()), img + " " + msg(label));

        } else if (theTag.equals(WIKI_TAG_SNIPPET)) {
	    return getSnippet(request, entry, true,"");
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
	    String prefix = getProperty(wikiUtil,props,"textBefore","");
	    if(stringDefined(prefix))
		prefix=  prefix+HU.space(1);
	    String text = getProperty(wikiUtil,props,"textAfter","");
	    if(stringDefined(text))
		text= HU.space(1) + text;
	    String style=HU.css("text-align","left","display","inline-block","padding","5px");
	    style+=getProperty(wikiUtil,props,"style","");
	    if(getProperty(wikiUtil,props,"decorate",false))
		style+=HU.css("border","var(--basic-border)");

	    String l = getProperty(wikiUtil,props,"license","CC-BY").trim();
	    License license = getMetadataManager().getLicense(l);
	    if(license==null)
		license = getMetadataManager().getLicense(l.toUpperCase());
	    if(license==null) 
		//a hack  for the cc licenses
		license = getMetadataManager().getLicense(l+"-4.0");
	    if(license==null) {
		return HU.div(prefix+l+text,HU.style(style));
	    }
	    String result= "";
	    String icon = license.getIcon();
	    if(icon!=null) {
		result =   HU.image(icon,
                                    HU.attrs("title",license.getName(),"width", "60", "border", "0"));
		if(getProperty(wikiUtil,props,"includeName",false)) {
		    result+=HU.space(1)+license.getName();
		}
	    } else {
		result = license.getName();
	    }
	    String url = license.getUrl();
            if(url!=null) result =  HU.href(url, result, HU.attrs("target","_other","style","text-decoration:none;"));
	    return HU.div(prefix+result + text,HU.style(style));
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
        } else if (theTag.equals(WIKI_TAG_ICON)) {
	    String width = getProperty(wikiUtil, props, "width", ICON_WIDTH);
	    String url = entry.getTypeHandler().getEntryIconUrl(request,  entry);
	    return HU.img(url, "", HU.attr("width", width));
        } else if (theTag.equals(WIKI_TAG_NAME)) {
            String name = entry==null?"NULL ENTRY":getEntryDisplayName(entry);
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
            IO.close(fis);
	    if(as!=null) {
		boolean doFile = false;
		if(as.equals("file")) {
		    doFile = true;
		    String ext = IO.getFileExtension(entry.getResource().getPath()).toLowerCase();
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
	    String displayType = Utils.getProperty(props, "displayType",(String)null);
	    if(displayType==null) displayType="null";
	    else displayType=HU.squote(displayType);
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
						  + "," + HU.squote(label) + "," + displayType+");\n");
			
		    }
		}
		return "";
	    }

            for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
                String key   = (String) keys.nextElement();
                String value = (String) props.get(key);
		value = value.replaceAll("\n"," ");
                wikiUtil.appendJavascript(HU.call("addGlobalDisplayProperty",HU.squote(key),HU.squote(value),displayType));
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
            ImageOutputHandler ioh = getImageOutputHandler();
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
		caption=entry.getTypeHandler().processDisplayTemplate(request,  entry,caption);
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

            String tooltip = getProperty(wikiUtil, props, "tooltip",null);
	    if(tooltip!=null) {
		tooltip = tooltip.replace("${entryid}",entry.getId()).replace("${entryname}",entry.getName());
		tooltip = tooltip.replace("${mainentryid}",originalEntry.getId()).replace("${mainentryname}",originalEntry.getName());		
		props.put("tooltip",tooltip);
	    }

            List<String> displayProps = new ArrayList<String>();
	    String jsonUrl = getDataUrl(request, entry,  wikiUtil, theTag, props,displayProps);
	    if(Misc.equals("true",request.getExtraProperty(PROP_MAKESNAPSHOT))) {
		if(request.isAnonymous()) throw new RuntimeException("Anonymous users cannot make snapshots");
		List<String[]> snapshotFiles = (List<String[]>) request.getExtraProperty("snapshotfiles");
		Hashtable<String,String> snapshotMap = (Hashtable<String,String>) request.getExtraProperty("snapshotmap");
		String fileName = snapshotMap.get(jsonUrl);
		if(fileName==null) {
		    File tmpFile = getStorageManager().getTmpFile("point.json");
		    Date now = new Date();
		    fileName = jsonUrl.replaceAll("^/.*\\?","").replace("output=points.product&product=points.json&","").replaceAll("[&=\\?]+","_").replace("entryid_","");
		    fileName += "_"+  now.getTime() +".json";
		    fileName =  Utils.makeID(entry.getName()) +"_"+fileName;
		    snapshotFiles.add(new String[]{tmpFile.toString(), fileName, entry.getName()});
		    snapshotMap.put(jsonUrl, fileName);
		    URL url = new URL(request.getAbsoluteUrl(jsonUrl).replace("localhost:","127.0.0.1:"));
		    OutputStream fos = getStorageManager().getFileOutputStream(tmpFile);
		    InputStream fis = IO.getInputStream(url);
		    IOUtil.writeTo(fis, fos);
		    IO.close(fos);
		}
		jsonUrl = fileName;
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
            return messageWarning("Google earth view is no longer available");
        } else if (theTag.equals(WIKI_TAG_DISPLAY_IMPORTS)) {
	    StringBuilder tmp = new StringBuilder();
	    getPageHandler().addDisplayImports(request, tmp,true);
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

            String links = getEntryManager().getEntryActionsTable(request,entry, type,null,false,props);
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
		    //Escape the delimiter with true
                    List<String> toks = Utils.split(value, ",",false,false,true);
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
                return getPageHandler().messageError("No _tag or _template attribute specified");
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
		    header = headerTemplate.replace("${name}",theEntry.getName()).replace("${entryid}", theEntry.getId()).replace("${entryurl}",url).replace("${icon}",getPageHandler().getEntryIconImage(request, theEntry));

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
        } else if (theTag.equals(WIKI_TAG_PAGETEMPLATE)) {
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
		return  makeErrorMessage(request,wikiUtil,props,theTag, "No entries available");
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
                    urlLabel = getPageHandler().getEntryIconImage(request,child) + " " + urlLabel;
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
                String icon = getPageHandler().getEntryIconImage(request,child);
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
                        title = getPageHandler().getEntryIconImage(request, child) + " " + title;
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

	    boolean showSnippet = getProperty(wikiUtil, props, "showSnippet",
					      false);

	    boolean showDescription = getProperty(wikiUtil, props,
						  "showDescription", false);

	    boolean showTextTop = getProperty(wikiUtil, props,
					      "showTextTop", false);
	    

            if (getProperty(wikiUtil, props, ATTR_USEDESCRIPTION) != null) {
                boolean useDescription = getProperty(wikiUtil, props,
						     ATTR_USEDESCRIPTION, true);
                if (useDescription) {
                    dfltTag = WIKI_TAG_SIMPLE;
                } else {
                    dfltTag = WIKI_TAG_HTML;
                }
            } else if(doingGrid) {
		dfltTag = WIKI_TAG_CARD;
	    }

	    if(flipCards) dfltTag = WIKI_TAG_CARD;
            boolean showDate = getProperty(wikiUtil, props, "showDate",
					   false);
            String frontStyle = getProperty(wikiUtil, props, "frontStyle","");
            String backStyle = getProperty(wikiUtil, props, "backStyle","");	    
            SimpleDateFormat sdf =         new SimpleDateFormat(getProperty(wikiUtil,props,"dateFormat","MMM dd, yyyy"));
            SimpleDateFormat sdf2 =         new SimpleDateFormat(getProperty(wikiUtil,props,"dateFormat","yyyy-MM-dd HH:mm"));	    
            boolean showicon = getShowIcon(wikiUtil, props, false);

	    String titleTemplate = getProperty(wikiUtil, props, "titleTemplate",null);
            if (doingGrid) {
		//                showicon = false;
                if (props.get("showLink") == null) {
                    props.put("showLink", "false");
                }
            }



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
	    //Check for any child. prefix properties
	    for (Enumeration keys = tmpProps.keys(); keys.hasMoreElements(); ) {
		Object key = keys.nextElement();
		String skey = key.toString();
		if(skey.startsWith("tag.")) {
		    tmpProps.put(skey.substring("tag.".length()), tmpProps.get(key));
		    tmpProps.remove(key);
		}
	    }

            if (doingGrid || flipCards) {
                tmpProps.put("showHeading", "false");
                if (tmpProps.get(ATTR_SHOWICON) == null) {
                    tmpProps.put(ATTR_SHOWICON, "true");
                }
            }
            if (children.size() > 0) {
                checkHeading(request, wikiUtil, props, sb);
            }



	    boolean  linkTop = getProperty(wikiUtil, props, "linkTop",false);
            for (Entry child : children) {
		String text = "";
		if (showSnippet || showDescription) {
		    String snippet = showDescription? child.getDescription():getSnippet(request, entry, false,null);
		    if (stringDefined(snippet)) {
			text = wikifyEntry(request, child, snippet, false, 
					   wikiUtil.getNotTags());
		    }
		}


                String title = getEntryDisplayName(child);
                if (showicon) {
		    title = getPageHandler().getEntryIconImage(request,  child) + " " + title;
                }
		if(titleTemplate!=null) {
		    title = applyTemplate(request, child,titleTemplate, "${title}",title);
		}
                titles.add(title);
                //                urls.add(request.entryUrl(getRepository().URL_ENTRY_SHOW, child));
                urls.add(getEntryManager().getEntryUrl(request, child));

                tmpProps.put("defaultToCard", "true");
		String inner = my_getWikiInclude(wikiUtil, newRequest,
						 originalEntry, child, tag, tmpProps, "", true);
                StringBuilder content =   new StringBuilder();
		if(showTextTop && stringDefined(text)) content.append(text);
		content.append(HU.center(inner));
		if(!showTextTop && stringDefined(text)) content.append(text);
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
		    if(linkTop)
			content = new StringBuilder(HU.center(href)+content.toString());
		    else
			content.append(HU.center(href));
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
					      "inner-height", 200);
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
		if(getProperty(wikiUtil, props, "addPageSearch",false)) {
		    HU.addPageSearch(sb,"#" + id +" .ramadda-gridbox",null,"Find");
		}
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
                width = getProperty(wikiUtil, props, ATTR_WIDTH,"200");
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
			//			if (showicon) {
			//			    label = getPageHandler().getEntryIconImage(request,  child) + " " + label;
			//			}
                        HU.div(comp, HU.href(urls.get(i), label),  HU.title(Utils.stripTags(title)) + headingClass);
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
		sb.append(buff);
		if(addHeader) {
		    getMapManager().addMapImports(request, sb);
		    HU.script(sb, "Ramadda.Components.init(" + HU.squote(id)+");");
		}
		HU.close(sb,"div");
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
		return  makeErrorMessage(request,wikiUtil,props,theTag, "No image entries available");
	    }
            ImageOutputHandler ioh = getImageOutputHandler();
            Request imageRequest = request.cloneMe();

            String    width     = getProperty(wikiUtil, props, ATTR_WIDTH,  null);
            if (width != null) {
                imageRequest.put(ARG_WIDTH, width);
            }
            boolean loopStart = getProperty(wikiUtil, props, "loopstart",
                                            false);
            if (loopStart) {
                imageRequest.put("loopstart", "true");
            }

	    props.put("useAttachment",""+useAttachment);
            String iwidth = getProperty(wikiUtil, props, "imageWidth", (String) null);
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

            ioh.makePlayer(imageRequest, entry, children, props, sb,  false);
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
            int depth = Math.min(ENTRY_TREE_MAX_DEPTH,getProperty(wikiUtil, props, "depth", 4));
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
	    sb.append(HU.open("span","class","ramadda-menutree"));
	    int count =
		doFullTree(request, wikiUtil, originalEntry, entry, props, true, doMenu, menuId,  
			   style, labelWidth, addPrefix, "", showRoot, showIcon, depth, types, sb,0);
	    sb.append(HU.close("span"));
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
		return  makeErrorMessage(request,wikiUtil,props,theTag, "No entries available");
            }
            boolean noTemplate = getProperty(wikiUtil, props, "noTemplate",
                                             true);
            getHtmlOutputHandler().makeTreeView(request, children, sb, width,
						height, noTemplate,props);

            return sb.toString();
        } else if (theTag.equals(WIKI_TAG_ENTRIES_TEMPLATE)) {
            List<Entry> children = getEntries(request, wikiUtil,
					      originalEntry, entry, props);

	    if(children.size()==0) {
		return  makeErrorMessage(request,wikiUtil,props,theTag, "No entries available");
	    }
	    
	    String style = getProperty(wikiUtil, props, "style","");
	    String prefix = getProperty(wikiUtil, props, "before","");
	    String suffix = getProperty(wikiUtil, props, "after","");
	    String delimiter = getProperty(wikiUtil, props, "delimiter","");	    
	    sb.append(prefix);
	    String template = getProperty(wikiUtil, props,
					  "template", "${name link=true}");
	    template = template.replace("_space_","&nbsp;").replace("_nl_","\n");

	    List<Utils.Macro> macros   =  Utils.splitMacros(template);
	    for (int i=0;i<children.size();i++) {
		Entry child = children.get(i);
		if(i>0) sb.append(delimiter);
		String url = getEntryManager().getEntryUrl(request, child);
		String text =  processMacros(request, macros, child);
		sb.append(text);
	    }
	    sb.append(suffix);

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

		return  makeErrorMessage(request,wikiUtil,props,theTag, "No entries available");
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

	String template = getProperty(wikiUtil, props,
				      "template", null);


	boolean highlightThis = getProperty(wikiUtil, props,
					    "highlightThis", false);
	boolean horizontal = getProperty(wikiUtil, props, "horizontal",
					 false);
	boolean decorate = getProperty(wikiUtil, props, "decorate",
				       false);
	boolean showSnippet = getProperty(wikiUtil, props,
					     "showSnippet", getProperty(wikiUtil,props,"includeSnippet",false));
	boolean showDescription = getProperty(wikiUtil, props,
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
	String tagOpen  = getProperty(wikiUtil, props, ATTR_TAGOPEN,  "<li>");
	String tagClose =  getProperty(wikiUtil, props, ATTR_TAGCLOSE,"");

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
	if(pre!=null) {
	    for(String s: pre) links.add("<li> " + s);
	}
        List<Utils.Macro> macros   = null;
	if(template!=null) {
	    macros = Utils.splitMacros(template);
	}


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
		    url = getEntryManager().getEntryUrl(request, child,false);
		    //                        url = request.entryUrl(getRepository().URL_ENTRY_SHOW, child);
		} else {
		    url = request.entryUrl(
					   getRepository().URL_ENTRY_SHOW, child,
					   ARG_OUTPUT, output);
		}
	    }

	    String linkLabel;
	    if(macros!=null) {
		linkLabel = processMacros(request, macros, child);
	    } else {
		linkLabel = getEntryDisplayName(child);
		if (showicon) {
		    linkLabel = getPageHandler().getEntryIconImage(request,child) +HU.space(1) + linkLabel;
		}
	    }


	    String snippet =  showSnippet?getSnippet(request,  child, true,""):showDescription?child.getDescription():null;

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


    private String processMacros(Request request, List<Utils.Macro> macros, Entry child) throws Exception {
	StringBuilder label = new StringBuilder();
	for(Utils.Macro macro: macros) {
	    if(macro.isText()) {
		label.append(macro.getText());
	    } else {
		String v=null;
		if(macro.getId().equals(TypeHandler.FIELD_ICON)) {
		    v =getPageHandler().getEntryIconImage(request, child);
		} else if(macro.getId().equals(TypeHandler.FIELD_NAME)) {
		    v = getEntryDisplayName(child);
		} else if(macro.getId().equals(TypeHandler.FIELD_SNIPPET)) {
		    v = getSnippet(request, child,macro.getProperty("wikify",false),
				   macro.getProperty("default",""));
		} else if(macro.getId().equals(TypeHandler.FIELD_CREATEDATE)) {
		    v= getDateHandler().formatDateWithMacro(request, child,
							    child.getCreateDate(),macro);
		} else if(macro.getId().equals(TypeHandler.FIELD_CHANGEDATE)) {
		    v = getDateHandler().formatDateWithMacro(request, child,
							     child.getCreateDate(), macro);
		} else if(macro.getId().equals(TypeHandler.FIELD_FROMDATE)) {
		    v = getDateHandler().formatDateWithMacro(request, child,
								      child.getStartDate(),macro);
		} else if(macro.getId().equals(TypeHandler.FIELD_TODATE)) {
		    v = getDateHandler().formatDateWithMacro(request, child,
							     child.getEndDate(),macro);
		} else if(macro.getId().equals(TypeHandler.FIELD_DATE)) {
		    v = getDateHandler().formatDateWithMacro(request, child,
								      child.getStartDate(),macro);
		} else if(macro.getId().equals(TypeHandler.FIELD_DESCRIPTION)) {
		    String desc = child.getDescription();		
		    if(macro.getProperty("wikify",false)) {
			desc  = wikifyEntry(request, child, desc);
		    }
		    v = desc;
		} else {
		    Column column = child.getTypeHandler().getColumn(macro.getId());
		    if(column!=null) {
			Object[] values = child.getValues();
			v = column.getString(values);
			if (v == null) {
			    v = "";
			}
			if(macro.getProperty("justIcon",false)) {
			    v = column.getIcon(v);
			} else {
			    String icon = macro.getProperty("icons",null);
			    if(icon!=null) {
				List<String> icons = (List<String>)macro.getProperty("iconlist");
				if(icons==null) {
				    icons =Utils.split(icon,",");
				    macro.putProperty("iconlist",icons);
				}
				String url = null;
				for(int i=0;i<icons.size();i+=2) {
				    if(v.matches(icons.get(i))) {
					url = icons.get(i+1);
					break;
				    }
				}
				if(url!=null) {
				    v = HU.img(getRepository().getIconUrl(url),v,"");
				}
			    } else {
				v= child.getTypeHandler().decorateValue(request, child, column, v);
			    }
			}
		    } else {		    
			v = "unknown macro:" +macro.getId();
		    }
		}
		if(v!=null) {
		    if(macro.getProperty("includeIcon",false)) {
			v = getPageHandler().getEntryIconImage(request, child) +" " + v;
		    }
		    if(macro.getProperty("link",false)) {
			if(macro.getProperty("noline",true)) {
			    v = HU.href(getEntryManager().getEntryUrl(request, child),v,
					HU.cssClass("noline"));
			} else {
			    v = HU.href(getEntryManager().getEntryUrl(request, child),v);
			}
		    }
		    label.append(v);
		}
	    }
	}
	return  label.toString();
    }
	


    private String makeChunks(Request request, WikiUtil wikiUtil, Hashtable props, List chunks)
	throws Exception {
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
	if(columns>0)  {
	    int cnt = 0;
	    StringBuilder sb = new StringBuilder();
	    for(int i=0;i<tds.size();i++) {
		sb.append(HU.div(tds.get(i).toString(),HU.style("vertical-align:top;display:table-cell;")));
		if(++cnt>=columns)  {
		    cnt=0;
		    sb.append("<br>");
		}
	    }
	    return sb.toString();
	}
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


    public Result processGetDataUrl(Request request) throws Exception {
        Entry entry = getEntryManager().getEntry(request, request.getString(ARG_ENTRYID, ""));
	if(entry==null) {
	    throw new RuntimeException("could not find entry:" + request.getString(ARG_ENTRYID, ""));
	}
	//Dummy tag
	String tag = "display_linechart";
	Hashtable props = request.getArgs();
	props.remove(ARG_REMOTE);
	List<String> displayProps = new ArrayList<String>();
	WikiUtil  wikiUtil = new WikiUtil();
	String jsonUrl = request.getAbsoluteUrl(getDataUrl(request, entry,wikiUtil,tag,props,displayProps));
        Result result = new Result("", new StringBuilder(jsonUrl),IO.MIME_TEXT);
        result.setShouldDecorate(false);
        return result;
    }



    public String getDataUrl(Request request, Entry entry, WikiUtil wikiUtil, String theTag, Hashtable props, List<String> displayProps) throws Exception {
	String jsonUrl = null;
	String remote = getProperty(wikiUtil,props,ARG_REMOTE,null);
	ServerInfo serverInfo = getServer(request, entry, wikiUtil, props);
	boolean doEntries = getProperty(wikiUtil, props, "doEntries",false);
	boolean doEntry = getProperty(wikiUtil, props, "doEntry", false);
	String ancestor = getProperty(wikiUtil, props, ARG_ANCESTOR,
				      null);
	if (doEntries || doEntry) {
	    String extra ="";
	    String orderBy = getProperty(wikiUtil, props,"orderby",null);
	    if(orderBy!=null)
		extra+="&orderby=" + orderBy;
	    String sortBy = getProperty(wikiUtil, props,"sortby",null);
	    if(sortBy!=null)
		extra+="&orderby=" + sortBy;		    
	    String ascending = getProperty(wikiUtil, props,"ascending",null);
	    if(ascending!=null)
		extra+="&ascending=" + ascending;
	    String sortDir = getProperty(wikiUtil, props,"sortdir",null);
	    if(sortDir!=null)
		extra+="&ascending=" + sortDir.equals("up");

	    if(serverInfo!=null) {
		jsonUrl = HtmlUtils.url(serverInfo.getUrl()+  "/entry/show",
					ARG_ENTRYID,entry.getId(), ARG_OUTPUT,
					JsonOutputHandler.OUTPUT_JSON_POINT.getId(),"remoteRequest","true");
	    } else {
		String entries = getProperty(wikiUtil,props,ATTR_ENTRIES,null);
		jsonUrl = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry, ARG_OUTPUT,
					   JsonOutputHandler.OUTPUT_JSON_POINT.getId());
		if(entries!=null) jsonUrl = HU.url(jsonUrl,ATTR_ENTRIES,entries);
	    }
	    //If there is an ancestor specified then we use the /search/do url
	    boolean doSearch = getProperty(wikiUtil, props, "doSearch",false);
	    ancestor = getProperty(wikiUtil, props, "ancestor",(String)null);
		
	    if (ancestor!=null || doSearch) {
		//		    String orderBy = ORDERBY_FROMDATE;
		//		    boolean ascending =false;
		jsonUrl = HU.url(getRepository().getUrlBase()+"/search/do", ARG_OUTPUT,
				 JsonOutputHandler.OUTPUT_JSON_POINT.getId()
				 /*,ARG_ORDERBY,orderBy+(ascending?"_ascending":"_descending")*/);

	    }
	    if(extra.length()>0) jsonUrl+=extra;
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
		if(stringDefined(remote)) {
		    URL url = new URL(remote);
		    String path = url.getPath();
		    if(path.endsWith("/entry/data")) {
			jsonUrl = remote;
		    }  else {
			String entryId =  StringUtil.findPattern(remote, "entryid=([^&]+)(\\?|$)");
			//https://localhost:8430/repository/entry/show?entryid=6ad1f1fb-8b6f-4fe8-a759-3027f512977e
			if(!stringDefined(entryId)) {
			    //check for alias - https://ramadda.org/repository/a/test_alias
			    entryId =  StringUtil.findPattern(remote, "/a/([^&]+)(\\?|$)");
			}
			if(!stringDefined(entryId)) {
			    throw new IllegalArgumentException("Could not find entry id in remote URL: "+ remote);
			}
			String prefix = StringUtil.findPattern(path,"^(.+)/entry/show");
			if(prefix==null)
			    prefix = StringUtil.findPattern(path,"^(.+)/a/");
			if(prefix==null)
			    prefix = "/repository";			   
			jsonUrl = url.getProtocol()+"://" + url.getHost()+(url.getPort()>0?":" + url.getPort():"");
			jsonUrl +=prefix+"/entry/data?entryid=" + entryId;



			String getUrlUrl = HU.url(
						  url.getProtocol()+"://" + url.getHost()+(url.getPort()>0?":" + url.getPort():"")+
						  "/getdataurl",
						  ARG_ENTRYID,entryId);
			for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
			    String arg = (String) keys.nextElement();
			    String v = (String) props.get(arg);
			    getUrlUrl=HU.url(getUrlUrl,arg,v);
			    if(arg.equals("max")||arg.equals("lastRecords")) {
				jsonUrl=HU.url(jsonUrl,arg,v);
			    }
			}
			System.err.println("remote url:" +jsonUrl);
			//			System.err.println(getUrlUrl);			
		    }
		} else {
		    jsonUrl = entry.getTypeHandler().getUrlForWiki(request,
								   entry, theTag, props, displayProps);
		}
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
	return jsonUrl;
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


	String divId  = HU.getUniqueId("div_");
	HU.open(sb,"div",HU.id(divId));
	if(getProperty(wikiUtil, props, "addPageSearch",false)) {
	    HU.addPageSearch(sb,"#" + divId +" .entry-list-row-data",null,"Find");
	}
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

	for(String prop: new String[]{"maxHeight","details","simple","showHeader",
				      "showDate","showCreateDate","showSize","showChangeDate",
				      "columns",
				      "showAttachments",
				      "showType","showIcon","showThumbnails","showArrow","showForm","showCrumbs","dateWidth","sizeWidth","nameWidth","typeWidth","createDateWidth","fromDateWidth","changeDateWidth"}) {
	    String v =getProperty(wikiUtil, props, prop, (String)null);
	    if(v!=null) {
		argProps.add(prop);
		argProps.add(JsonUtil.quote(v));
	    }
	}

	boolean formOpen =getProperty(wikiUtil, props, "formOpen", defaultTableFormOpen);
	if(formOpen) {
	    argProps.add("formOpen");
	    argProps.add(""+formOpen);
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
	HU.close(sb,"div");
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
	    boolean minified = getRepository().getMinifiedOk();
	    if(minified)
		HtmlUtils.importJS(sb, getPageHandler().getCdnPath("/lib/ace/src-min/ace.min.js"));	    
	    else
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
					  getProperty(wikiUtil, props, "listentries", false));
        boolean showCheckbox = getProperty(wikiUtil, props, "showCheckbox",
                                           false);
        boolean showSearch = getProperty(wikiUtil, props, "showSearch",
                                         false);
        boolean checkboxOn = getProperty(wikiUtil, props, "checkboxOn", true);

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
		snippet = StringUtil.findPattern(text, "(?s)<snippet-hide>(.*)</snippet-hide>");
		if (snippet == null) {
		    snippet = StringUtil.findPattern(text, "(?s)\\+snippet(.*?)-snippet");
		}
		if (snippet == null) {
		    //Only get the first 400 characters so we just get the notes at the start of the text
		    if(text.length()>400) text=text.substring(0,399);
		    snippet = StringUtil.findPattern(text, "(?s)\\+note\\s*(.*?)-note");
		}		
		//Now check for embedded tags
		if(snippet!=null && snippet.indexOf("{{")>=0) snippet = null;
		if (snippet == null) {
		    snippet = StringUtil.findPattern(text, "(?s)\\+callout-info\\s*(.*?)-callout");
		}		
		//Now check for embedded tags
		if(snippet!=null && snippet.indexOf("{{")>=0) snippet = null;
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
    private int doFullTree(Request request,  WikiUtil wikiUtil,
                            Entry originalEntry, Entry entry,
                            Hashtable props,
			    boolean top, boolean asMenu, String menuId,
			    String style,
			    int labelWidth,
			    boolean addPrefix,
                            String prefix, boolean showRoot,
                            boolean showIcon, int depth, List<String> types,
                            Appendable sb,int count)
	throws Exception {
	if(top) {
	    HU.open(sb,"ul",HU.attrs("id",menuId, "style",style));
	    sb.append("\n");
	}
	
	if ((prefix.length() > 0) || showRoot) {
	    HU.open(sb, "li");
            String label = Utils.clipTo(getEntryManager().getEntryDisplayName(entry),labelWidth,"...");
	    if(showIcon)
		label = getPageHandler().getEntryIconImage(request, entry) + HU.SPACE + label;
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

		count++;
		if(count>ENTRY_TREE_MAX_COUNT) break;
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
                count = doFullTree(request,  wikiUtil, originalEntry, child, props,
				 false, asMenu, null,			   
				 style, labelWidth, addPrefix, p, showRoot, showIcon, depth, types,
				 sb,count);
	    }
	    if(addedUl) {
		HU.close(sb,"ul","\n");
	    }
        }
	if(top && showRoot) HU.close(sb,"ul","\n");
	HU.close(sb,"li","\n");	
	if(top) HU.close(sb,"ul","\n");
	return count;
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
        String wikiText = getProperty(wikiUtil, props, "wikiText", null);


        boolean showHeading = getProperty(wikiUtil, props, "showHeading",
                                          true);
        boolean includeChildren = getProperty(wikiUtil, props, "includeChildren",
					      false);
        if (showHeading) {
            HU.div(card, HU.href(entryUrl, entry.getName()),
                   HU.title(entry.getName())
                   + HU.cssClass("ramadda-subheading"));
        }
	boolean showPlaceholder = getProperty(wikiUtil, props, "showPlaceholder", false);
        boolean useThumbnail = getProperty(wikiUtil, props, "useThumbnail",
                                           true);
        boolean showSnippet = getProperty(wikiUtil, props, "showSnippet",
                                          false);

        boolean showSnippetHover = getProperty(wikiUtil, props,
					       "showSnippetHover", false);

	boolean showDescription = getProperty(wikiUtil, props,
						 "showDescription", false)	;

        if (showSnippet || showSnippetHover || showDescription) {
            String snippet = showDescription? entry.getDescription():getSnippet(request, entry, false,null);
            if (Utils.stringDefined(snippet)) {
                snippet = wikifyEntry(request, entry, snippet, false, 
                                      wikiUtil.getNotTags());
                if (showSnippet) {
                    HU.div(card, snippet,"");
                } else if (showSnippetHover) {
		    String clazz = "ramadda-snippet-hover";
                    HU.div(card, snippet, HU.cssClass(clazz));

                }
            }
        }

	boolean addTags = getProperty(wikiUtil, props, "addTags", false);
	if(addTags) {
	    String[] tagTypes;
	    String  tagType = getProperty(wikiUtil, props, "tagTypes",null);
	    if(tagType!=null) {
		tagTypes = Utils.toStringArray(Utils.split(tagType,",",true,true));
	    } else {
		tagTypes = new String[]{"enum_tag","content.keyword"};
	    }
	    List<Metadata> tagList =getMetadataManager().findMetadata(request, entry,
						  tagTypes, false);
	    if(tagList!=null && tagList.size()>0) {
		card.append("<div class=metadata-tags>");
		for(Metadata metadata: tagList) {
		    MetadataHandler mtdh = getMetadataManager().findHandler(metadata.getType());
		    String searchUrl = mtdh.getSearchUrl(request, metadata);
		    card.append(HU.href(searchUrl,mtdh.getTag(request, metadata)));
		}
		card.append("</div>");
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
	    if(imageUrl==null && showPlaceholder) {
		imageUrl = getPageHandler().makeHtdocsUrl("/images/placeholder.png");
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



	if(wikiText!=null) {
            card.append(wikifyEntry(request, entry, wikiText.replace("\\n","\n")));
	} else if (imageUrl != null) {
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
	wikiUtil.setProperty(ATTR_REQUEST,   request);
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
            entries = getEntryManager().applyFilter(request, entries, filter);
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
            entries = getEntryManager().applyFilter(request, 
						    getEntryManager().getImageEntries(request, entries,
										      useAttachment), filter);
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

    private Utils.TriFunction<SelectInfo,String,String,String>
	getIdMatcher(final Request request,
		     final Entry entry,
		     final WikiUtil wikiUtil,
		     final Hashtable props) {
	return  (entryId,baseId,thePrefix)->{
	    try {
		if(baseId!=null && entryId.equals(baseId)) entryId = thePrefix;
		if(entryId.startsWith(thePrefix)) {
		    return  getSelectFromString(request, entry, wikiUtil,
						props,Utils.clip(entryId,thePrefix));
		}
		return null;
	    } catch(Exception exc) {
		throw new RuntimeException(exc);
	    }
	};
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
                                  Hashtable theProps)
	throws Exception {

        if (theProps == null) {
            theProps = new Hashtable();
        }
	final Hashtable props  = theProps;

        Hashtable   searchProps = null;
        List<Entry> entries     = new ArrayList<Entry>();
        Request myRequest = request.cloneMe();
        request = myRequest;
	String prefix = getProperty(wikiUtil,props,"argPrefix","");
        int         max         =   getProperty(wikiUtil, props, ARG_MAX, -1);
        String      orderBy     =  getProperty(wikiUtil, props, "sort");
	if (orderBy == null) {
	    orderBy = getProperty(wikiUtil, props, "sortby");
	}
	if (orderBy == null) {
	    orderBy = getProperty(wikiUtil, props, "orderby");
	}	    

	//xxxxx
	String sortDir = getProperty(wikiUtil,props,ATTR_SORT_DIR,
				     getProperty(wikiUtil,props,ATTR_SORT_ORDER,null));

	if(sortDir==null) {
	    String v = getProperty(wikiUtil,props,"ascending",null);
	    if(v!=null) {
		if(v.equals("true")) sortDir=DIR_UP;
		else if(v.equals("false")) sortDir = DIR_DOWN;
	    }
	}

	if(sortDir==null) {
	    String v = getProperty(wikiUtil,props,"descending",null);
	    if(v!=null) {
		if(v.equals("true")) sortDir=DIR_DOWN;
		else if(v.equals("false")) sortDir = DIR_UP;
	    }
	}

	if(sortDir==null) sortDir = DIR_DOWN;
	boolean descending = sortDir.equals(DIR_DOWN);

        HashSet     nots        = new HashSet();
	SelectInfo select=null;
	Utils.TriFunction<SelectInfo,String,String,String> matches =
	    getIdMatcher(request, baseEntry,wikiUtil,props);

        for (String entryId : Utils.split(ids, ",", true, true)) {
            if (entryId.startsWith("#")) {
                continue;
            }
            if (entryId.startsWith("not:")) {
                nots.add(entryId.substring("not:".length()));
                continue;
            }

            if (entryId.startsWith("entries.max:")) {
                max = Integer.parseInt(entryId.substring("entries.max:".length()));
                continue;
            }
            if (entryId.startsWith("entries.orderby:")) {
                orderBy = entryId.substring("entries.orderby:".length());
                continue;
            }
            if (entryId.startsWith("entries.ascending:")) {
                descending=  entryId.substring("entries.ascending:".length()).equals("false");
                continue;
            }

            entryId = entryId.replace("_COMMA_", ",").replace("_comma_",",");

	    if (entryId.startsWith(ID_SEARCH + ".")) {
                List<String> tokens = Utils.splitUpTo(entryId, "=", 2);
                if (tokens.size() == 2) {
                    if (searchProps == null) {
                        searchProps = new Hashtable();
                        searchProps.putAll(props);
                    }
                    searchProps.put(tokens.get(0), tokens.get(1));
                    myRequest.put(Utils.clip(tokens.get(0), ID_SEARCH+"."),tokens.get(1));
                }
                continue;
            }




            Entry  theBaseEntry = baseEntry;
            String filter = null;

	    if((select = matches.call(entryId,ID_THIS,PREFIX_THIS))!=null) { 
		entries.addAll(getEntryManager().applyFilter(select.getRequest(), select.getEntry(), select));
                continue;
            }
	    

            if (entryId.equals(ID_ROOT)) {
                entries.addAll(getEntryManager().applyFilter(request, request.getRootEntry(), filter));
                continue;
            }

	    if(entryId.equals(ID_SEARCH) || entryId.startsWith(PREFIX_SEARCH)) {
		List<Entry> foundEntries =
		    getEntriesFromEmbeddedSearch(request, wikiUtil,  props, baseEntry,  entryId,-1);
		if(foundEntries!=null) {
		    entries.addAll(getEntryManager().applyFilter(request,  foundEntries,filter));
		}
		continue;
	    }



	    if((select = matches.call(entryId,ID_CHILDREN,PREFIX_CHILDREN))!=null) { 
                List<Entry> children = getEntryManager().getChildren(select.getRequest(),
								     select.getEntry(),select);
		entries.addAll(getEntryManager().applyFilter(select.getRequest(), children,filter));
                continue;
            }


	    if((select = matches.call(entryId,ID_GRANDPARENT,PREFIX_GRANDPARENT))!=null) { 
                Entry parent = getEntryManager().getEntry(request,
							  select.getEntry().getParentEntryId());
                if (parent != null) {
                    Entry grandparent = getEntryManager().getEntry(request,
								   parent.getParentEntryId());
                    if (grandparent != null) {
                        entries.addAll(getEntryManager().applyFilter(select.getRequest(), grandparent, select));
                    }
                }
                continue;
            }


	    if((select = matches.call(entryId,ID_ANCESTORS,PREFIX_ANCESTORS))!=null) { 
                List<Entry> tmp    = new ArrayList<Entry>();
                Entry       parent = select.getEntry().getParentEntry();
                while (parent != null) {
                    tmp.add(0, parent);
                    parent = parent.getParentEntry();
                }
                entries.addAll(getEntryManager().applyFilter(request, tmp, select));
                continue;
            }

	    if((select = matches.call(entryId,ID_SIBLINGS,PREFIX_SIBLINGS))!=null) { 
                Entry parent = getEntryManager().getEntry(request,
							  select.getEntry().getParentEntryId());
                if (parent != null) {
                    for (Entry sibling :
			     getEntryManager().getChildren(request, parent)) {
                        if ( !sibling.getId().equals(select.getEntry().getId())) {
                            entries.add(sibling);
                        }
                    }
                }
		entries = getEntryManager().applyFilter(request, entries, select);
                continue;
            }


	    if((select = matches.call(entryId,ID_LINKS,PREFIX_LINKS))!=null) { 
                List<Association> associations =
                    getRepository().getAssociationManager().getAssociations(
									    request, select.getEntry().getId());
                for (Association association : associations) {
                    String id = null;
                    if ( !association.getFromId().equals(
							 select.getEntry().getId())) {
                        id = association.getFromId();
                    } else if ( !association.getToId().equals(
							      select.getEntry().getId())) {
                        id = association.getToId();
                    } else {
                        continue;
                    }
		    Entry e = getEntryManager().getEntry(request, id);
		    if(e!=null)
			entries.add(e);
		    
                }
		entries = getEntryManager().applyFilter(request, entries, select);
                continue;
            }


	    if((select = matches.call(entryId,ID_PARENT,PREFIX_PARENT))!=null) { 		
                entries.addAll(getEntryManager().applyFilter(
							     request, 
							     getEntryManager().getEntry(
											request,
											select.getEntry().getParentEntryId()), select));

                continue;
            }

	    if((select = matches.call(entryId,ID_GRANDCHILDREN,PREFIX_GRANDCHILDREN))!=null ||
	       (select = matches.call(entryId,ID_GREATGRANDCHILDREN,PREFIX_GREATGRANDCHILDREN))!=null) {
                List<Entry> children = getEntryManager().getChildren(request,
								     select.getEntry());
                List<Entry> grandChildren = new ArrayList<Entry>();
                for (Entry child : children) {
                    //Include the children non folders
                    if (!child.isGroup()) {
                        grandChildren.add(child);
                    } else {
                        grandChildren.addAll(getEntryManager().getChildren(request, child));
                    }
                }

                if (entryId.equals(ID_GREATGRANDCHILDREN) || entryId.startsWith(PREFIX_GREATGRANDCHILDREN)) {
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

		entries.addAll(getEntryManager().applyFilter(request, grandChildren, select));
                continue;
            }

            if (entryId.startsWith(ATTR_SEARCH_URL)) {
                if (searchProps == null) {
                    searchProps = props;
                }

                myRequest.put(ARG_AREA_MODE,
                              getProperty(wikiUtil, searchProps,
                                          ARG_AREA_MODE,
                                          VALUE_AREA_CONTAINS));
                myRequest.put(ARG_MAX,
                              getProperty(wikiUtil, searchProps,
                                          "search." + ARG_MAX, "100"));
                addSearchTerms(myRequest, wikiUtil, searchProps,
                               theBaseEntry);

		List<String> tokens = (entryId.indexOf("=") >= 0)
		    ? Utils.splitUpTo(entryId,
				      "=", 2)
		    : Utils.splitUpTo(entryId,
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


            Entry entry = getEntryManager().getEntry(request, entryId);
            if (entry != null) {
		entries.addAll(getEntryManager().applyFilter(request, entry, filter));
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

    public ImageOutputHandler getImageOutputHandler() throws Exception {
	return (ImageOutputHandler) getRepository().getOutputHandler(
								     ImageOutputHandler.OUTPUT_PLAYER);
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

    private synchronized void initWikiMenuButtons() throws Exception {
	if(wikiMenuEtcButton!=null) return;
	String textAreaId = "${textareaid}";
        String        buttonClass = HU.clazz("ramadda-menubar-button");
        StringBuilder help        = new StringBuilder();
        StringBuilder etc        = new StringBuilder();	

        StringBuilder tags1  = new StringBuilder();
        StringBuilder tags2 = new StringBuilder();
        StringBuilder tags3 = new StringBuilder();
        StringBuilder tags4 = new StringBuilder();	
	Utils.TriFunction<String,String,String,String> l = (title,pre,post)->{
	    return getWikiEditLink(textAreaId,title,pre,post,"");
	};

	Utils.QuadFunction<String,String,String,String,String> l2 = (title,tt,pre,post)->{
	    return getWikiEditLink(textAreaId,title,pre,post,tt);
	};
	
	BiFunction<String,String,String> makeButton = (title,contents)->{
	    return HU.makePopup(null,HU.div(title,HU.cssClass("ramadda-menubar-button")),
				HU.div(contents, "class='wiki-editor-popup'"),
				new NamedValue("linkAttributes", buttonClass));
	};



        Utils.appendAll(tags1,
			l2.call("Section", "Section wrapper. use +section-map for a map bg\nimg:section.png","+section title={{name}}_newline__newline_", "-section"),
			l2.call( "Frame", "Full page framed section","+frame background=#fff frameSize=0 shadow title=\"\"\\n", "-frame"),
			l2.call( "Table", "HTML table","+table height=400 hover=true cellborder=false rowborder=false stripe=false ordering=false paging=false searching=false_newline_:tr &quot;heading 1&quot; &quot;heading 2&quot;_newline_+tr_newline_:td colum 1_newline_+td_newline_column 2_newline_", "-td_newline_-tr_newline_-table"),
			l2.call( "Row/Column", "Create a 12 unit wide bootstrap row\nimg:row.png", "+row_newline_+col-6_newline_", "-col_newline_+col-6_newline_-col_newline_-row"),
			l2.call( "Left-right", "2 column table aligned left and right","+leftright_nl_+left_nl_-left_nl_+right_nl_-right_nl_", "-leftright"),
			l2.call( "Left-middle-right", "3 column table aligned left,center and right","+leftmiddleright_nl_+left_nl_-left_nl_+middle_nl_-middle_nl_+right_nl_-right_nl_", "-leftmiddleright"),
			l2.call( "Tabs", "Make tabs\nimg:tabs.png","+tabs center=false minarrow=false tight=false noBorder=false #minheight=\"\" _newline_+tab tab title_newline_", "-tab_newline_-tabs_newline_"),
			l2.call( "Accordion", "Accordion layout\nimg:accordion.png","+accordion decorate=false collapsible=true activeSegment=0 _newline_+segment segment  title_newline_", "-segment_newline_-accordion_newline_"),
			l2.call( "Slides", "Slides layout\nimg:slides.png","+slides dots=true slidesToShow=1 bigArrow=true  centerMode=true variableWidth=true arrows=true  dots=true  infinite=false style=_qt__qt__nl_+slide Title_nl_", "-slide_nl_-slides_nl_"),
			//			l2.call("Grid box", "+grid #decorated=true #columns=_qt_1fr 2fr_qt_ _nl_:filler_nl_+gridbox #flex=1 #style=_qt__qt_ #width=_qt__qt_ #title=_qt_Title 1_qt__nl_-gridbox_nl_+gridbox #title=_qt_Title 2_qt__nl_-gridbox_nl_:filler_nl_", "-grid"),
			l2.call("Scroll panels","For full page story scrolling\nimg:scroll.png","+scroll_newline_+panel color=gradient1 name=home style=_quote__quote_ _newline_+center_newline_<div class=scroll-indicator>Scroll Down</div>_newline_-center_newline_-panel_newline_+panel color=gradient2 name=panel1_newline__newline_-panel_newline_+panel color=blue name=panel2_newline__newline_-panel_newline_", "-scroll") 
			
			); 

        Utils.appendAll(tags2,
			l2.call("Center", "Center text","\\n+center\\n","-center"),
			l2.call("Center div", "Center the block, not the text","\\n+centerdiv\\n","-centerdiv"),			
			l2.call("Horizontal layout", "","+hbox #space=10 #style=\"\"\\n", "-hbox"),
			l2.call("Inset", "top/left/bottom/right spacing","+inset #space=10 top=0 bottom=0 left=0 right=0 _newline_", "-inset"),
			l2.call("Popup", "Popup link\nimg:popup.png","+popup link=_qt_Link_qt_ icon=_qt_fa-solid fa-arrow-right-from-bracket_qt_ title=_qt_Title_qt_ header=true draggable=true decorate=true sticky=true my=_qt__qt_ at=_qt__qt_ animate=false_nl__nl_", "-popup_nl_"),
			l.call("Menu", "+menu_nl_    :menuheader Header_nl_    :menuitem Item 1_nl_    +menu Menu 1_nl_        :menuitem Item 2_nl_        +menuitem style=_qt_width:300px; _qt_ _nl_        Menu contents_nl_        -menuitem_nl_    -menu_nl_    +menu Menu 2_nl_        :menuitem Item 3_nl_    -menu_nl_-menu", ""),

			l2.call("Toggle","Show a closed toggle box","+toggle Toggle Label\\n","-toggle"),
			l2.call("Open Toggle","Show a open toggle box","+toggleopen Toggle Label\\n","-toggleopen"),			
			l2.call("Side Toggle",
				"Side Toggle","+sidetoggle label=\"Click me\" boxBackground=\"#fff\" boxWidth=\"200px\" fontSize=16px buttonWidth=\"24px\" buttonBackground=\"#fff\" buttonColor=\"#000\" boxTop=\"100px\" buttonTop=\"100px\"\\n",
				"-sidetoggle"),
			l2.call("Enlarge/Shrink",
				"Enlarge/Shrink Toggle\nimg:enlarge.png\nShow content in an expandable block","+enlarge height=\"200\" enlargeLabel=\"Show more\" shrinkLabel=\"Show less\"\\n",
				"-enlarge"),
			l2.call("Expandable", "Allow a section to expand to the full browser window",
				"+expandable header=_quote__quote_ expand=false_newline_", "-expandable"),
			l2.call("Full screen", "Allow a section to be expanded to full screen",  "+fullscreen_newline_", "-fullscreen"),
			l2.call("Draggable", "A draggable section\nimg:draggable.png","+draggable framed=true header=_quote__quote_ style=_quote_background:#fff;_quote_ toggle=_quote_true_quote_ toggleVisible=_quote_true_quote__newline_",
			       "-draggable"));

        Utils.appendAll(tags4,
			l.call("Navigation left", ":navleft leftStyle=_qt_width:250px;_qt_ rightStyle=_qt__qt_  maxLevel=_qt_4_qt_", ""),
			l.call("Navigation top", ":navtop style=_quote__quote_ delimiter=_quote_|_quote_  maxLevel=_qt__qt_", ""),
			l.call("Navigation popup", ":navpopup align=right|left  maxLevel=_qt__qt_", ""),	    
			l.call("Prev arrow", "{{prev position=relative|fixed decorate=false iconSize=32 sort=name,entryorder sortAscending=true style=_qt_left:250px;_qt_  showName=false}}", ""),
			l.call("Next arrow", "{{next position=relative|fixed decorate=false iconSize=32 sort=name,entryorder sortAscending=true style=_dq_  showName=false}}", ""),
			l.call("Absolute", "\\n+absolute top= bottom= left= right=\\n","-absolute"),
			l.call("Relative", "\\n+relative\\n","-relative"));


        Utils.appendAll(tags3, l2.call( "Note", "A centered text note\nimg:note.png","+note\\n\\n", "-note"));
        String[] colors = new String[] {"gray",  "yellow"};
	/*
        for (String color : colors) {
            tags3.append(getWikiEditLink(textAreaId, HU.div("Note "
							    + color, HU.attrs(
									      "style", "padding:2px; display:inline-block;", "class", "ramadda-background-"
									      + color)), 
					 "+note-" + color + "_nl__nl_", "-note",""));
					 }*/

        Utils.appendAll(tags3, l.call( "Box", "+box_nl__nl_", "-box"));
	/*
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
	*/

        Utils.appendAll(tags3,
                        l2.call( "Callout", "Callout box\nimg:callout.png", "+callout_nl__nl_", "-callout"),
                        l2.call( "Callout info", "Callout box\nimg:calloutinfo.png","+callout-info_nl__nl_", "-callout"),
                        l2.call( "Callout tip", "Callout box\nimg:callouttip.png","+callout-tip_nl__nl_", "-callout"),
                        l2.call( "Callout question", "Callout box\nimg:calloutquestion.png","+callout-question_nl__nl_", "-callout"),
                        l2.call( "Callout warning", "Callout box\nimg:calloutwarning.png","+callout-warning_nl__nl_", "-callout"),
                        l2.call( "Text Balloon", "Text balloon with avatar\nimg:balloon.png","+balloon-left avatar=true #width=400px #style=\"background:#fffeec;\"_nl__nl_", "-balloon"));



        StringBuilder misc1 = new StringBuilder();
        StringBuilder misc2 = new StringBuilder();
        StringBuilder misc3 = new StringBuilder();
	Utils.appendAll(misc3,
			l.call( "Macro", ":macro name value_nl_${name}_nl_", ""),
			l.call( "Template", "+template template_name_nl_... ${var1} ... ${var2}_nl_", "-template"),
			l.call( "Apply template", "+apply template_name_nl_:var var1 Some value_nl_+var var2_nl_Some other value_nl_..._nl_-var_nl_", "-apply"),
			l.call( "Inline apply", ":apply template_name var1=\"some value\" var2=\"Some other value\"", ""),
			l2.call( "Javascript", "Include Javascript","+js_newline_", "-js"),
			l.call( "CSS", "+css_newline_", "-css"),
			l.call( "PRE", "+pre_newline_", "-pre"),
                        l2.call( "Xml", "Include XML","+xml addCopy=true addDownload=true downloadFile=download.xml_nl__nl_", "-xml"),
			l.call( "Code", "```_newline__newline_", "```"),
			l2.call( "Property", "Name value properties",
				 "{{property name=value", "}}"));

        Utils.appendAll(misc1,
			l.call( "Title", ":title {{name link=true}}", ""),
			l2.call( "Heading", "A heading. Is added to any :navtop's",
				 ":heading your heading", ""),
			l.call( "Heading-1", ":h1 your heading", ""),
			l.call( "Heading-2", ":h2 your heading", ""),
			l.call( "Heading-3", ":h3 your heading", ""),	    
			l.call("Break", "\\n:br", ""),
			l.call("Paragraph", "\\n:p", ""),
			l2.call("Vertical space", "Add vertical space","\\n:vspace 1em", ""),
			l.call("Bold text", "\\'\\'\\'", "\\'\\'\\'"),
			l.call("Italic text", "\\'\\'", "\\'\\'"),
			l.call("Code", "```\\n", "\\n```"),
			l2.call("FA icon","Font Awesome icon","{{fa icon=\"fas fa-cog\" style=\"\"}}",""));
        Utils.appendAll(misc2,
			l2.call("Internal link", "Link to another entry","[[id|link text", "]]"),
			l2.call("External link", "Link to an external URL","[http://www.example.com link title", "]"),
			l2.call("Embed YT, etc.","Embed content from YouTube, Wikipedia, etc",
				"@(youtube, wikipedia, etc, URL)",""),
			l.call("Horizontal line", "\\n----\\n", ""),
			l2.call("Button", "Add a button with a URL",":button url label", ""),
			l2.call("Language Block", "Show/hide block based on user's language preference","+lang one of es en fr etc.\\n", "-lang"),
			l2.call("Language Switcher", "Add a widget to switch languages",":langswitcher en,es,fr, etc.\\n", ""),
			l2.call("Set Language", "Set the language of the page",":setlanguage es\\n", ""),
			l2.call("Draft", "Show a 'Draft' background","+draft\\n", "-draft"),
			l2.call("Remark", "One line comment","\\n:rem ", ""),
			l2.call( "Skip", "Skip a section of wiki text",
				 "+skip_nl__nl_", "-skip"),
			l2.call("Reload", "Reload the page after some time",
				"\\n:reload seconds=30 showCheckbox=true showLabel=true", ""),
			l2.call("After", "Fade in a block of content after some time","+after pause=0 afterFade=5000_newline__newline_", "-after"),
			l2.call("Odometer", "Show a spinning counter","{{odometer initCount=0 count=100 immediate=true pause=1000}}", ""));
	

        wikiMenuTagsButton = makeMenuButton("Tags",
					    HU.span(HU.hbox(misc1, misc2,misc3),
						    HU.attrs("data-title","Tags","class","wiki-menubar-tags")));




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

        String tidyButton =
	    HU.href("#", "Tidy",   HU.attrs("id", textAreaId+"_tidy")); 


        String findButton =
	    HU.href("#", "Find",   HU.attrs("id", textAreaId+"_find")); 


        String wcButton =
	    HU.href("#", "Word Count",
		    HU.attrs("id", textAreaId+"_wordcount", "xstyle", "padding:5px;",
			     "xclass",
			     "ramadda-menubar-button ramadda-menubar-button-last"));



	List<String> etcLinks = new ArrayList<String>();
	Utils.add(etcLinks, findButton, previewButton,tidyButton);
	if(getRepository().isLLMEnabled()) {
	    etcLinks.add(HU.href("#", "GPT",
				 HU.attrs("id", textAreaId+"_rewrite")));
	    etcLinks.add(HU.href("#", "Transcribe",
				 HU.attrs("id", textAreaId+"_transcribe")));	    
	}
	Utils.add(etcLinks,colorButton, wcButton);
	etc.append(Utils.join(etcLinks,"<br>"));
	//	help.append("<div class=ramadda-thin-hr></div><b>Help</b><br>");


	BiConsumer<String,String> makeHelp = (p,title)->{
	    help.append(HU.href(getRepository().getUrlBase()
				+ p, title,
				"target=_help") + "<br>");
	};

	/*
        for (String extraHelp :
		 Utils.split(request.getString("extrahelp", ""), ",",
			     true, true)) {
            List<String> toks = Utils.splitUpTo(extraHelp, "|", 2);
            if (toks.size() == 2) {
                help.append(HU.href(Utils.encodeUntrustedText(toks.get(0)),
                                    Utils.encodeUntrustedText(toks.get(1)),
                                    "target=_help") + "<br>");
            }
	    }*/

        makeHelp.accept("/userguide/wiki/wikitext.html", "Wiki text");
        makeHelp.accept("/userguide/wiki/wikidisplay.html", "Displays and Charts");
        makeHelp.accept("/userguide/wiki/wikitext.html#sections", "Sections");
        makeHelp.accept("/userguide/wiki/wikitext.html#gridlayout", "Grid layout");
        makeHelp.accept("/userguide/wiki/wikientries.html",
                        "Specifying the entry");
        makeHelp.accept("/userguide/wiki/wikientries.html#entries",
                        "Specifying multiple entries");
        makeHelp.accept("/search/providers", "Search Providers");
        makeHelp.accept("/search/info#entrytypes", "Entry Types");
        makeHelp.accept("/search/info#metadatatypes", "Metadata Types");
        makeHelp.accept("/colortables", "Color Tables");

        wikiMenuEtcButton = makeMenuButton("Etc", etc.toString());
        wikiMenuHelpButton = makeMenuButton("Help", help.toString());
        wikiMenuFormattingButton = makeMenuButton("Formatting",
						  HU.span(HU.hbox(tags1, tags2,tags3,tags4),
							  HU.attrs("data-title","Formatting","class","wiki-menubar-tags")));



    }


    private static final String BUTTONCLASS = HU.clazz("ramadda-menubar-button");
    public String makeMenuButton(final String title, final String contents) {
	return HU.makePopup(null,HU.div(title,HU.cssClass("ramadda-menubar-button")),
			    HU.div(contents, "class='wiki-editor-popup'"),
			    new NamedValue("linkAttributes", BUTTONCLASS));
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
            getPageHandler().addDisplayImports(request, buttons,true);
        }
	if(wikiMenuEtcButton==null) {
	    initWikiMenuButtons();
	}


        List<Link> links = getRepository().getOutputLinks(request,
							  new OutputHandler.State(entry));
        for (Link link : links) {
            if (link.getOutputType() == null) {
                continue;
            }

            String prop = link.getOutputType().getId();
            String js = "javascript:WikiUtil.insertTags(" + HU.squote(textAreaId)
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
                String js = "javascript:WikiUtil.insertTags(" + HU.squote(textAreaId)
		    + "," + HU.squote(pair[1]) + ",'');";
                fromTypeBuff.append(HU.href(js, pair[0]));
                fromTypeBuff.append("<br>");
            }
        }




        String entriesButton = makeMenuButton("Entries",
						HU.span(makeTagsMenu(entry,textAreaId),
							HU.attrs("data-title","Entries","class","wiki-menubar-tags")));
	
        String displaysButton = HU.href("javascript:noop()", "Displays",
					HU.attrs("id", "displays_button" + textAreaId,
						 "class",
						 "ramadda-menubar-button"));


        String addEntry = OutputHandler.getSelect(request, textAreaId,
						  "Entry ID", true, "entryid", entry, false, false,
						  BUTTONCLASS);



        String importEntry = OutputHandler.getSelect(request, textAreaId,
						     "Embed Entry", true, "wikilink", entry, false, false,
						     BUTTONCLASS);

        String fieldLink = OutputHandler.getSelect(request, textAreaId,
						   "Field ID", true, "fieldname", entry, false, false,
						   BUTTONCLASS);

        HU.open(buttons, "div",
                HU.cssClass("ramadda-menubar")
                + HU.attrs("id", textAreaId + "_toolbar"));
	buttons.append(HU.span(HU.img(getIconUrl("fas fa-binoculars")),HU.attrs("style","margin-left:4px;","title","Search for tags", "class","ramadda-clickable","id", textAreaId + "_toolbar_search")));
        Utils.appendAll(buttons, HU.span("", HU.id(textAreaId + "_prefix")),
                        wikiMenuFormattingButton, wikiMenuTagsButton, entriesButton);
        if (fromTypeBuff != null) {
	    String label = entry==null?"Wiki Tags":entry.getTypeHandler().getTypeProperty("wiki.edit.links.label",entry.getTypeHandler().getLabel() + " tags");
	    String popup = HU.makePopup(null,HU.div(label,BUTTONCLASS),
					HU.div(fromTypeBuff.toString(), "class='wiki-editor-popup'"),
					new NamedValue("linkAttributes", BUTTONCLASS));
	    buttons.append(popup);
        }

        Utils.appendAll(buttons, importEntry, /*addEntry,*/ displaysButton,  
                        fieldLink);

        if (entry != null) {
            entry.getTypeHandler().addToWikiToolbar(request, entry, buttons,
						    textAreaId);
        }

        buttons.append(wikiMenuEtcButton);
        buttons.append(wikiMenuHelpButton);
        HU.close(buttons, "div");
	String s = buttons.toString();
	s = s.replace("${textareaid}",textAreaId);
        return  s;
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
                String js2 = "javascript:WikiUtil.insertTags(" + HU.squote(textAreaId)
		    + "," + HU.squote("{{" + textToInsert + " ")
		    + "," + HU.squote("}}") + "," + HU.squote("")
		    + ");";
                sb.append(HU.div(HU.href(js2, tuple[0]),
				 HU.attrs("class","wiki-editor-popup-link",
					  "style","margin-left:8px;")));
	    }
            sb.append("</td>");
	}

	sb.append("<td valign=top>\n");
        for (int i = 0; i < WikiTags.WIKITAGS.length; i++) {
            WikiTags.WikiTagCategory cat = WikiTags.WIKITAGS[i];
            if (rowCnt + cat.tags.length > 10) {
                rowCnt = 0;
                if (i > 0) {
                    sb.append("</td><td>&nbsp;</td><td valign=top>\n");
                }
            }
            sb.append(HU.div(cat.category,HU.attrs("class","wiki-editor-popup-category")));
            rowCnt += cat.tags.length;
            for (int tagIdx = 0; tagIdx < cat.tags.length; tagIdx++) {
                WikiTags.WikiTag tag          = cat.tags[tagIdx];
                String  textToInsert = tag.tag;
                if (tag.attrs.length() > 0) {
                    textToInsert += " " + tag.attrs;
                }

                String js2 = "javascript:WikiUtil.insertTags(" + HU.squote(textAreaId)
		    + "," + HU.squote("{{" + textToInsert + " ")
		    + "," + HU.squote("}}") + "," + HU.squote("")
		    + ");";
		String attrs = HU.attrs("class","wiki-editor-popup-link",
					"style","margin-left:8px;");
		if(tag.tt!=null) attrs+=HU.attr("title",getMenuTooltip(tag.tt));
                sb.append(HU.div(HU.href(js2, tag.label),
				 attrs));
            }
            sb.append(HU.br());
        }
        sb.append("</td></tr></table>\n");

        return sb.toString();

    }


    public String getMenuTooltip(String tt) {
	if(tt.indexOf("img:")>=0)  {
	    String img = StringUtil.findPattern(tt, "(img:\\s*[^\\s\n]+)");
	    if(img!=null) {
		String url = img.replace("img:","");
		url = getRepository().getHtdocsUrl("/help/wiki/" + url);
		tt = tt.replace(img,HU.img(url,"",HU.attr("width","300px")));
	    }
	}
	tt = tt.replace("\n","<br>");
	return tt;
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
                                   String tt) {
        String js;
	String linkAttrs = "";
	if(stringDefined(tt)) {
	    tt = getMenuTooltip(tt);
	    linkAttrs+=HU.attr("title",tt);
	}

        if (suffix.length() == 0) {
            String prop = prefix + suffix;
            js = "javascript:WikiUtil.insertText(" + HU.squote(textAreaId) + ","
		+ HU.squote(prop) + ");";
        } else {
            js = "javascript:WikiUtil.insertTags(" + HU.squote(textAreaId) + ","
		+ HU.squote(prefix) + "," + HU.squote(suffix) + ","
		+ HU.squote("") + ");";
        }
	String attrs=  HU.attrs("class","wiki-editor-popup-link");
        return HU.div(HU.href(js, label,linkAttrs),attrs);
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

        getPageHandler().addDisplayImports(request, sb, true);


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
		props.put("isolineEnabled","true");
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
		String authToken = getAuthManager().getAuthToken(sessionId);
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
	    String style="";
	    if(getProperty(wikiUtil, props, "displayInline",false)) {
		style+="display:inline-block;";
	    }
            HU.div(sb, "", HU.id(mainDivId) +HU.clazz("display-group") +HU.style(style));
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
	if(getProperty(wikiUtil, props, "displayInline",false)) {
	    style+="display:inline-block;";
	}

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
            props.remove(ATTR_ENTRIES);
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





    private ServerInfo getServer(Request request, Entry entry,
				 WikiUtil wikiUtil, Hashtable props) throws Exception {
	ServerInfo server = entry!=null?entry.getRemoteServer():null;
	if(server!=null) return server;
	String remoteServer = getProperty(wikiUtil, props, "remoteServer", null);
	return  !Utils.stringDefined(remoteServer)?null:new ServerInfo(new URL(remoteServer),"","");

    }


}
