/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;


import org.ramadda.repository.auth.AccessException;
import org.ramadda.repository.auth.AccessManager;
import org.ramadda.repository.auth.AuthorizationMethod;
import org.ramadda.repository.auth.User;
import org.ramadda.repository.search.SearchManager;
import org.ramadda.repository.database.DatabaseManager;
import org.ramadda.repository.database.Tables;
import org.ramadda.repository.harvester.Harvester;
import org.ramadda.repository.metadata.AdminMetadataHandler;
import org.ramadda.repository.metadata.ContentMetadataHandler;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.metadata.JpegMetadataHandler;
import org.ramadda.repository.output.WikiConstants;

import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.output.XmlOutputHandler;
import org.ramadda.repository.type.Column;
import org.ramadda.repository.type.ProcessFileTypeHandler;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.repository.type.TypeInsertInfo;
import org.ramadda.repository.util.SelectInfo;
import org.ramadda.repository.util.ServerInfo;
import org.ramadda.repository.util.FileWriter;
import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.NamedList;
import org.ramadda.util.geo.GeoUtils;


import org.ramadda.util.TTLCache;
import org.ramadda.util.TTLObject;
import org.ramadda.util.ImageUtils;
import org.ramadda.util.Utils;
import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlNodeList;
import ucar.unidata.xml.XmlUtil;

import java.awt.Image;

import org.json.*;

import java.awt.geom.Rectangle2D;

import java.io.*;

import java.net.URL;
import java.net.URLConnection;

import java.nio.file.Files;
import java.nio.file.Path;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.text.SimpleDateFormat;

import java.util.function.BiConsumer;

import java.util.zip.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.time.ZonedDateTime;
import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;




/**
 * This class does most of the work of managing the entries
 */
@SuppressWarnings("unchecked")
public class EntryManager extends RepositoryManager {



    /** _more_ */
    public static final String[] PRELOAD_CATEGORIES = { "Documents",
							"General", "Information", "Collaboration", "Database" };


    public enum TEMPLATE {
	YES,
	NO
    }

    public enum INTERNAL {
	YES,
	NO
    }    


    /** _more_ */
    public static boolean debug = false;

    private static boolean didone= false;

    public static boolean debugGetEntries = false;    

    /** How many entries to we keep in the cache */
    public static final int ENTRY_CACHE_LIMIT = 2000;

    /** _more_ */
    public static final int SYNTHENTRY_CACHE_LIMIT = 10000;

    /** _more_ */
    public static final int ENTRY_CACHE_TTL_MINUTES = 10;

    /** _more_ */
    public static final int SYNTHENTRY_CACHE_TTL_MINUTES =  60;


    /** _more_ */
    public static final String SESSION_ENTRIES = "entries";

    /** _more_ */
    public static final String SESSION_TYPES = "types";

    /** _more_ */
    private Object MUTEX_ENTRY = new Object();


    /** _more_ */
    private static final String GROUP_TOP = "Top";

    /** _more_ */
    private static final String ID_ROOT = "root";


    /** _more_ */
    public static final String ID_PREFIX_REMOTE = "remote:";


    /** _more_ */
    private TTLObject<Entry> rootCache;

    /** Caches sites */
    private TTLCache<String, Entry> entryCache;

    private TTLCache<String, Entry> aliasCache =
	new TTLCache<String,Entry>(Utils.minutesToMillis(5));

    /** 5 minute cache  - 2 levels with entry being the primary level then orderby/etc being secondary */
    private TTLCache<String, Hashtable<String,List<String>>> childrenCache =
	new TTLCache<String,Hashtable<String,List<String>>>(Utils.minutesToMillis(5));    


    /** _more_ */
    private TTLCache<String, Entry> synthEntryCache;

    //Keep the history around for 10 minutes
    private TTLCache<String,List<Entry.EntryHistory>> entryHistories =
	new TTLCache<String,List<Entry.EntryHistory>>(Utils.minutesToMillis(10));


    private boolean httpCacheFile = true;
    
    /**
     * _more_
     *
     * @param repository _more_
     */
    public EntryManager(Repository repository) {
        super(repository);
    }

    /**
     * _more_
     */
    @Override
    public void initAttributes() {
        super.initAttributes();
	httpCacheFile = getRepository().getProperty("ramadda.http.cachefile", true);
    }


    /**
     * _more_
     *
     * @param msg _more_
     */
    public void debug(String msg) {
        if (debug) {
            logInfo(msg);
        }
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public Entry getRootEntry() {
        return getRootEntry(null);
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public Entry getRootEntry(Request request) {
        try {
            if (rootCache == null) {
                initTopEntry();
            }
            Entry topEntry = rootCache.get();
            if (topEntry == null) {
                topEntry = initTopEntry();
            }


            if (getRepository().getEnableHostnameMapping()
		&& (request != null)) {
		String key =  "http://"   + request.getRequestHostname();
                Entry fromHostname = getEntryFromAlias(request,key);
		if(fromHostname==null) {
		    aliasCache.put(key, topEntry);
		} else {
                    return fromHostname;
                }
            }
            return topEntry;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    protected Entry initTopEntry() throws Exception {
        String fixedTopId = getRepository().getProperty(PROP_ENTRY_TOP,
							(String) null);
        Clause clause;
        if (fixedTopId != null) {
            clause = Clause.eq(Tables.ENTRIES.COL_ID, fixedTopId);
        } else {
            clause = Clause.isNull(Tables.ENTRIES.COL_PARENT_GROUP_ID);
        }

        Statement statement =
            getDatabaseManager().select(Tables.ENTRIES.COLUMNS,
                                        Tables.ENTRIES.NAME, clause);
        List<Entry> entries  = readEntries(statement);

        Entry       topEntry = null;
        if (entries.size() > 1) {
            System.err.println(
			       "RAMADDA: there are more than one top-level entries");
            entries = getEntryUtil().sortEntriesOnCreateDate(entries, false);
            for (Entry entry : entries) {
                if (topEntry == null) {
                    if (entry.getType().equals(TypeHandler.TYPE_GROUP)) {
                        topEntry = entry;
			continue;
                    }
                }
                System.err.println("entry:" + entry.getType() + " - "
                                   + entry.getName() + " " + entry.getId());
		//		deleteEntry(null, entry);
            }
        }


        if ((topEntry == null) && (entries.size() > 0)) {
            topEntry = (Entry) entries.get(0);
        }

        //Make the top group if needed
        if (topEntry == null) {
            topEntry = makeNewGroup(getRepository().getAdminRequest(),null, GROUP_TOP,
                                    getUserManager().getDefaultUser());
            getAccessManager().initTopEntry(topEntry);
        }

        if (rootCache == null) {
            rootCache = new TTLObject<Entry>(Utils.minutesToMillis(60),"Entry Root" );
        }
        rootCache.put(topEntry);

        return topEntry;

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
    public List<Entry> getParents(Request request, Entry entry)
	throws Exception {
        Entry root = request.getRootEntry();
        Entry parent = getEntryManager().findGroup(request,
						   entry.getParentEntryId());
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

        return parents;
    }

    /**
     * _more_
     *
     * @param descendent _more_
     *
     * @return _more_
     */
    public Entry getSecondToTopEntry(Entry descendent) {
        Entry topEntry = null;
        if (descendent != null) {
            topEntry = descendent;
            while (topEntry != null) {
                Entry parent = topEntry.getParentEntry();
                if (parent == null) {
                    break;
                }
                if (parent.isTopEntry()) {
                    break;
                }
                topEntry = parent;
            }
        }

        return topEntry;
    }





    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public String getFullEntryShowUrl(Request request) {
        if ((request == null) || !request.isRealRequest()) {
            return getRepository().URL_ENTRY_SHOW.getFullUrl(null);
        }

        return request.getAbsoluteUrl(getRepository().URL_ENTRY_SHOW);
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public String getFullEntryGetUrl(Request request) {
        return request.getAbsoluteUrl(getRepository().URL_ENTRY_GET);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param alias _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getEntryFromAlias(Request request, String alias)
	throws Exception {
	//Use the admin request in case the aliases entry has access control
	Entry entry = aliasCache.get(alias);
	if(entry!=null) {
	    //Then filter the entry for this user
	    entry =  getAccessManager().filterEntry(request, entry);	    
	    if(entry==null)
		throw new AccessException("You do not have access to this entry",request);
	    return entry;
	}
	Request adminRequest = getRepository().getAdminRequest();
	List<Entry> entries =  getEntriesFromAlias(adminRequest, alias);
	if(entries.size()>0) {
	    entry =  entries.get(0);
	    aliasCache.put(alias,entry);
	    //Then filter the entry for this user
	    entry =   getAccessManager().filterEntry(request, entry);
	    if(entry==null)
		throw new AccessException("You do not have access to this entry",request);	    
	    return entry;
	}
	return null;
    }

    public List<Entry> getEntriesFromAlias(Request request, String alias)
	throws Exception {
	return   getEntriesFromMetadata(request,
					ContentMetadataHandler.TYPE_ALIAS, alias,
					1);
    }



    public Result processSiteMap(Request request)  throws Exception {
	StringBuilder xml = new StringBuilder();
	xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
	List<Entry> entries = getEntriesFromMetadata(request,
						     "isinsitemap", "true",
						     1);

	if(entries.size()==0) {
	    Entry root = request.getRootEntry();
	    if(root!=null) {
		entries = getChildren(request, root);
	    }
	}
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	for(Entry entry: entries) {
	    String url = request.getAbsoluteUrl(getRepository().URL_ENTRY_SHOW) +"?" + HU.arg(ARG_ENTRYID,entry.getId());
	    xml.append("<url>\n");
	    xml.append("<loc>" + url+"</loc>\n");
	    String date =  sdf.format(new Date(entry.getChangeDate()));
	    xml.append("<lastmod>" + date +"</lastmod>\n");
	    xml.append("</url>\n");
	}
	xml.append("</urlset>");
	return new Result(xml.toString(), MIME_XML);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param metadataType _more_
     * @param value _more_
     * @param attrIndex _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> getEntriesFromMetadata(Request request, String metadataType,
					      String value, int attrIndex)
	throws Exception {
	List<Entry> entries = new ArrayList<Entry>();
        String column = ((attrIndex == 1)
                         ? Tables.METADATA.COL_ATTR1
                         : (attrIndex == 2)
			 ? Tables.METADATA.COL_ATTR2
			 : (attrIndex == 3)
			 ? Tables.METADATA.COL_ATTR3
			 : Tables.METADATA.COL_ATTR3);

        Statement statement =
            getDatabaseManager().select(
					Tables.ENTRIES.COL_ID,
					Misc.newList(Tables.ENTRIES.NAME, Tables.METADATA.NAME),
					Clause.and(
						   new Clause[] {
						       Clause.join(
								   Tables.ENTRIES.COL_ID,
								   Tables.METADATA.COL_ENTRY_ID),
						       Clause.eq(column, value),
						       Clause.eq(Tables.METADATA.COL_TYPE,
								 metadataType) }), "", 100);

        SqlUtil.Iterator iter = getDatabaseManager().getIterator(statement);
        ResultSet        results;
        while ((results = iter.getNext()) != null) {
            String id = results.getString(1);
            Entry entry = getEntry(request, id);
	    if(entry!=null) entries.add(entry);
        }
	iter.close();

        return entries;
    }


    public void checkParents() throws Exception {
        Statement statement =
            getDatabaseManager().select(
					Tables.ENTRIES.COL_ID+"," +Tables.ENTRIES.COL_PARENT_GROUP_ID,
					Misc.newList(Tables.ENTRIES.NAME),
					null);
        SqlUtil.Iterator iter = getDatabaseManager().getIterator(statement);
        ResultSet        results;
	int cnt = 0;
	while ((results = iter.getNext()) != null) {
	    if((cnt%100)==0) System.err.print(".");
            String id = results.getString(1);
            String parentId = results.getString(1);
	    Entry parent = getEntry(null, parentId,false);
	    if(parent!=null) continue;
	    Entry entry = getEntry(null,id,false);
	    System.err.println("entry:" + (entry) +" " + id);
	    if(cnt++>10) break;
	}
    }
	


    /**
     * _more_
     *
     * @return _more_
     */
    public synchronized TTLCache<String, Entry> getEntryCache() {
        //Get a local copy because another thread could clear the cache while we're in the middle of this
        TTLCache<String, Entry> theCache = entryCache;
        if (theCache == null) {
            int cacheTimeMinutes =
                getRepository().getProperty(PROP_CACHE_TTL,
                                            ENTRY_CACHE_TTL_MINUTES);
            entryCache = theCache = new EntryCache(Utils.minutesToMillis(cacheTimeMinutes), ENTRY_CACHE_LIMIT,"Entry Cache");
        }
        return theCache;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private TTLCache<String, Entry> getSynthEntryCache() {
        TTLCache<String, Entry> theCache = synthEntryCache;
        if (theCache == null) {
            synthEntryCache = theCache = new EntryCache(Utils.minutesToMillis(SYNTHENTRY_CACHE_TTL_MINUTES),SYNTHENTRY_CACHE_LIMIT,"Synthetic Entry Cache");
        }
        return theCache;
    }


    private class EntryCache extends TTLCache<String,Entry> {
	public EntryCache(long ttl,int limit,String name) {
	    super(ttl,limit,name);
	}

	/**
	 * override this to check if there was a limit set on the entry
	 */
	@Override
	public  boolean cacheValueOk(Entry entry) {
	    if(entry.getCacheActiveLimit()>0) {
		if(new Date().getTime()>entry.getCacheActiveLimit()) {
		    return false;
		}
	    }
	    return true;
	}
    }


    /**
     * _more_
     */
    @Override
    public void clearCache() {
        super.clearCache();
	if(childrenCache !=null)
	    childrenCache.clearCache();
	if(entryCache!=null)
	    entryCache.clearCache();
	if(synthEntryCache!=null)
	    synthEntryCache.clearCache();
	if(rootCache!=null)
	    rootCache.clearCache();
        getEntryUtil().clearCache();
    }

    /**
     * _more_
     *
     * @param entry _more_
     */
    public void cacheSynthEntry(Entry entry) {
	if(!entry.getCacheOk()) return;
        synchronized (MUTEX_ENTRY) {
            getSynthEntryCache().put(entry.getId(), entry);
        }
    }


    /**
     * _more_
     *
     * @param entry _more_
     */
    public void cacheEntry(Entry entry) {
	if(!entry.getCacheOk()) return;
        synchronized (MUTEX_ENTRY) {
	    //	    System.err.println("CACHING:" + entry);
            //Check if we are caching
            if ( !getRepository().doCache()) {
                return;
            }
            if (entry.getTypeHandler().getCanCache(entry)) {
                getEntryCache().put(entry.getId(), entry);
            } 
        }
    }


    /**
     * _more_
     *
     * @param entryId _more_
     *
     * @return _more_
     */
    public Entry getEntryFromCache(String entryId) {
        return getEntryFromCache(entryId, true);
    }

    /**
     * _more_
     *
     * @param entryId _more_
     * @param isId _more_
     *
     * @return _more_
     */
    public Entry getEntryFromCache(String entryId, boolean isId) {
	//	if(true) return null;
        synchronized (MUTEX_ENTRY) {
            Entry entry = getEntryCache().get(entryId);
            if (entry == null) {
                entry = getSynthEntryCache().get(entryId);
            }
            return entry;
        }
    }


    /**
     * _more_
     *
     * @param id _more_
     */
    protected void removeFromCache(String id) {
        synchronized (MUTEX_ENTRY) {
	    //Check if its the root. if it is then clear the root cache
	    if(rootCache!=null) {
		Entry topEntry = rootCache.get();
		if(topEntry!=null && topEntry.getId().equals(id)) {
		    rootCache.clearCache();
		}
	    }
            getEntryCache().remove(id);
            getSynthEntryCache().remove(id);
        }
    }


    /**
     * _more_
     *
     * @param entry _more_
     */
    public void removeFromCache(Entry entry) {
        removeFromCache(entry.getId());
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param urlArg _more_
     * @param requestUrl _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getEntryFromRequest(Request request, String urlArg,
                                     RequestUrl requestUrl, boolean nullOk)
	throws Exception {
        Entry entry = null;
        if (request.defined(urlArg)) {
            try {
                entry = getEntryFromArg(request, urlArg);
            } catch (Exception exc) {
                logError("", exc);
                throw exc;
            }
            if (entry == null) {
                String entryId = request.getString(urlArg, BLANK);
                Entry  tmp     = getEntry(request, entryId, false);
                if (tmp != null) {
                    logInfo("Cannot access entry:" + entryId + "  IP:"
                            + request.getIp());
                    logInfo("Request:" + request);
                    throw new IllegalArgumentException(
						       "You do not have access to this entry");
                }
            }
        } else {
            String path   = request.getRequestPath();
            String prefix = requestUrl.toString();
            if (path.length() > prefix.length()) {
                String suffix = path.substring(prefix.length());
                suffix = java.net.URLDecoder.decode(suffix, "UTF-8");
                entry = findEntryFromName(request, null, suffix);
                if (entry == null) {
		    if(nullOk) return null;
                    fatalError(request, "Could not find entry:" + suffix);
                }
            }

            if (entry == null) {
                //Try the hostname to alias lookup
                entry = getEntryFromAlias(request,
                                          "http://"
                                          + request.getRequestHostname());
            }
            if (entry == null) {
                entry = request.getRootEntry();
            }
        }
	if(entry!=null) {
	    request.setCurrentEntry(entry);
	}
        return entry;
    }



    public void sanitizeEntries(List<Entry> entries) {
	for(Entry entry: entries) {
	    sanitizeEntry(entry);
	}	    
    }

    public void sanitizeEntry(Entry entry) {
	entry.sanitize();
    }

    public Result processMakeSnapshot(Request request, Entry entry) throws Exception {
        if (request.isAnonymous()) {
	    return makeSnapshotForm(request, entry,messageError("Have to be an logged in to create a file snapshot"));
	}

	if(request.defined(ARG_CANCEL)) {
	    return new Result(getEntryUrl(request, entry));
	}

	if(request.defined(ARG_OK)) {
	    getAuthManager().ensureAuthToken(request);
	    return makeSnapshot(request, entry);
	}
	return makeSnapshotForm(request, entry,null);
    }
	


    private  Result makeSnapshotForm(Request request, Entry entry, String prefix) throws Exception {
	StringBuilder sb = new StringBuilder();
	getPageHandler().entrySectionOpen(request, entry, sb,
					  "Create a snapshot");
	Entry parent = entry.getParentEntry();
	if(parent==null) {
	    sb.append(messageError("Cannot snapshot the top-level entry"));
	    return makeEntryEditResult(request,  entry,"Snapshot", sb);
	}

	request.formPostWithAuthToken(sb, getRepository().URL_ENTRY_SHOW);
	sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
	sb.append(HU.hidden(ARG_OUTPUT,getRepository().OUTPUT_MAKESNAPSHOT.getId()));
	String extraMsg = "";
	List<TwoFacedObject> types= new ArrayList<TwoFacedObject>();
	types.add(new TwoFacedObject("Snapshot Entry", SNAPSHOT_ENTRY));
	if (request.isAdmin()) {
	    types.add(new TwoFacedObject("HTML File", SNAPSHOT_FILE));
	    extraMsg  = " 'HTML File' or ";
	}
	types.add(new TwoFacedObject("HTML Export", SNAPSHOT_EXPORT));


	String msg = ":note A snapshot is a permanent HTML page of this entry. Any data that is referenced will also be saved. If you choose " + extraMsg +" 'HTML Export' as the type then the destination entry is not required.";
        sb.append(getWikiManager().wikifyEntry(request, entry,msg));
	if(prefix !=null) sb.append(prefix);

	sb.append(HU.formTable());

	HU.formEntry(sb, msgLabel("Snapshot type"), HU.select(ARG_SNAPSHOT_TYPE, types));
	getPageHandler().addEntrySelect(request, parent, ARG_DEST_ENTRY,sb,"Destination Entry", "");
	String base = request.getAbsoluteUrl("");
	HU.formEntry(sb, msgLabel("URL Base"), HU.input("snapshotbase",base));
	HU.formTableClose(sb);
	sb.append(HU.submit("Create snapshot", ARG_OK));
	sb.append(HU.SPACE);
	sb.append(HU.submit(LABEL_CANCEL, ARG_CANCEL));
	sb.append(HU.formClose());
	getPageHandler().entrySectionClose(request, entry, sb);
	return makeEntryEditResult(request,  entry,"Snapshot", sb);
    }


    private Result makeSnapshot(Request request, Entry entry) throws Exception {
	String type = request.getString(ARG_SNAPSHOT_TYPE,"");
	StringBuilder sb  = new StringBuilder();
	boolean makeFile = type.equals(SNAPSHOT_FILE);
	boolean makeExport = type.equals(SNAPSHOT_EXPORT);
	boolean makeEntry = type.equals(SNAPSHOT_ENTRY);	
	if(makeFile) {
	    if (!request.isAdmin()) {
		return makeSnapshotForm(request, entry,messageError("Have to be an admin to create a file snapshot"));
	    }
	}
	Date now = new Date();
	String cleanName = Utils.makeID(entry.getName());
	String snapshotFile =cleanName +"_"+ entry.getId() +"_"+  now.getTime() +".html";
	String snapshotFilePath  =getRepository().getUrlBase()+"/snapshots/pages/" + snapshotFile;
	if(makeExport) {
	    request.putExtraProperty(PROP_OVERRIDE_URL,"#");
	} else if(makeFile) {
	    request.putExtraProperty(PROP_OVERRIDE_URL,snapshotFilePath);
	}
	request.putExtraProperty(PROP_MAKESNAPSHOT,"true");
	List<String[]> snapshotFiles = new ArrayList<String[]>();
	request.putExtraProperty("snapshotfiles", snapshotFiles);
	Hashtable<String,String> snapshotMap = new Hashtable<String,String>();
	request.putExtraProperty("snapshotmap", snapshotMap);
	request.put(ARG_OUTPUT,OutputHandler.OUTPUT_HTML.getId());
	getRepository().getHtmlOutputHandler().handleDefaultWiki(request, entry,sb);
	Result tmpResult = new Result("",sb);
	tmpResult.setTitle(entry.getName());
	Request tmpRequest = request.cloneMe();

	if(makeEntry || makeExport) {
	    //use empty template when we generate  an entry 
	    tmpRequest.put(ARG_TEMPLATE,"empty");
	}
	if(makeExport) {
	    String base = request.getString("snapshotbase","");
	    if(!stringDefined(base)) base = request.getAbsoluteUrl("");
	    tmpRequest.appendHead0("<base href='" + base +"' target='_blank'>\n");
	}
	tmpRequest.setUser(getUserManager().getAnonymousUser());
	getPageHandler().decorateResult(tmpRequest, tmpResult);
	String html = tmpResult.getStringContent();
	if(makeExport) {
	    //String to  = request.getAbsoluteUrl("");
	    //	    html = html.replaceAll(getRepository().getUrlBase(),to+"/repository");
	    //	    html = html.replaceAll("/" + RepositoryUtil.getHtdocsVersion(),"");
	    OutputStream os = request.getHttpServletResponse().getOutputStream();
	    request.getHttpServletResponse().setContentType("multipart/x-zip");
	    request.setReturnFilename(IO.stripExtension(Utils.makeID(entry.getName()))+"_snapshot.zip");
	    FileWriter zipFileWriter= new FileWriter(new ZipOutputStream(os));
	    zipFileWriter.setCompressionOn();
	    for(String[]tuple: snapshotFiles) {
		String tmpFile =tuple[0];
		String jsonFileName= tuple[1];
		FileInputStream fis =new FileInputStream(tmpFile);
                zipFileWriter.writeFile(jsonFileName, fis);
		IO.close(fis);
	    }
	    zipFileWriter.writeFile(IO.stripExtension(Utils.makeID(entry.getName()))+".html", html.getBytes());
	    zipFileWriter.close();
	    Result result = Result.makeNoOpResult();
	    result.setShouldDecorate(false);
	    return result;
	}
	
	if(makeFile) {
	    for(String[]tuple: snapshotFiles) {
		String jsonFileName= tuple[1];
		String tmpFile =tuple[0];
		getStorageManager().moveFile(new File(tmpFile), new File(getStorageManager().getSnapshotsDataDir()+"/"+ jsonFileName));
		String newPath = getRepository().getUrlBase()+"/snapshots/data/" + jsonFileName;
		html = html.replaceAll(jsonFileName, newPath);
	    }
	    File file  = new File(getStorageManager().getHtdocsDir()+"/snapshots/pages/" + snapshotFile);
	    getStorageManager().writeFile(file, html);
	    return new Result(snapshotFilePath);
	}

	if(makeEntry) {
	    Entry parent = getEntry(request, request.getString(ARG_DEST_ENTRY+"_hidden",""));
	    if(parent==null) {
		return makeSnapshotForm(request, entry,messageError("No parent entry specified"));
	    }
	    if(!canAddTo(request, parent)) {
		return makeSnapshotForm(request, entry,messageError("You do not have permission to add to the parent entry"));
	    }
	    File zipFile = getStorageManager().getTmpFile("snapshortimport.zip");
	    FileOutputStream fos  = new FileOutputStream(zipFile);
	    FileWriter zipFileWriter= new FileWriter(new ZipOutputStream(fos));
	    StringBuilder entriesXml = new StringBuilder("<entries>\n");
	    String htmlFileName =  IO.stripExtension(entry.getName())+".html";
	    String htmlEntryId = getRepository().getGUID();
	    entriesXml.append(XmlUtil.tag("entry",XmlUtil.attr("name",entry.getName()+" snapshot") +
					  XmlUtil.attr("id",htmlEntryId) +
					  XmlUtil.attr("type","type_document_html") +
					  XmlUtil.attr("embed_type","embed") +
					  XmlUtil.attr("file",htmlFileName)));
	    html = html.replaceAll("(?s)" + PageHandler.IMPORTS_BEGIN+".*?" + PageHandler.IMPORTS_END,"");
	    for(String[]tuple: snapshotFiles) {
		String tmpFile =tuple[0];
		String jsonFileName= tuple[1];
		String jsonFileId = IO.stripExtension(jsonFileName);
		html = html.replaceAll(jsonFileName,getRepository().getUrlBase()+"/entry/get?entryid=" + jsonFileId);
		String dataEntryName= tuple[2];		
		FileInputStream fis =new FileInputStream(tmpFile);
                zipFileWriter.writeFile(jsonFileName, fis);
		IO.close(fis);
		entriesXml.append(XmlUtil.tag("entry",XmlUtil.attr("name",dataEntryName+" data") +
					      XmlUtil.attr("id",jsonFileId) +
					      XmlUtil.attr("parent",htmlEntryId) +
					      XmlUtil.attr("type","type_datafile_json") +
					      XmlUtil.attr("file",jsonFileName)));
		entriesXml.append("\n");
	    }
	    entriesXml.append("</entries>\n");
	    zipFileWriter.writeFile(htmlFileName, html.getBytes());
	    zipFileWriter.writeFile("entries.xml",entriesXml.toString().getBytes());
	    zipFileWriter.close();
	    return  processEntryImportInner(request, parent, zipFile.toString(),true);
	}

	return makeSnapshotForm(request, entry,"");
    }



	


    public Result processEntryFile(Request request) throws Exception {
	//	/repository/entry/file/id/...
	String path = request.getRequestPath();
	int idx = path.indexOf("/entryfile/");
	path = path.substring(idx+"/entryfile/".length());
	idx = path.indexOf("/");
	String id = path.substring(0,idx);
	path = path.substring(idx);
	Entry entry = getEntry(request, id);
	if(entry==null) {
	    return getRepository().make404(request);
	}
	File dir = getStorageManager().getEntryDir(entry.getId(),false);
	if(!dir.exists()) {
	    return getRepository().make404(request);
	}
	File f = new File(dir,path);
	if(!f.exists()) {
	    return getRepository().make404(request);
	}
	f = getStorageManager().checkReadFile(f);
        String mimeType = getRepository().getMimeTypeFromSuffix(
								IO.getFileExtension(f.toString()));
	return new Result(BLANK,
			  getStorageManager().getFileInputStream(f),
			  mimeType);
    }


    public Result processEntryData(Request request) throws Exception {
	request.put("output","points.product");
	if(!request.defined("product"))
	    request.put("product","points.json");
	try {
	    //	request.put("product","points.csv");
	    return   processEntryShow(request);
	} catch(Exception exc) {
	    getLogManager().logError("Error processing data",exc);
	    StringBuilder sb = new StringBuilder();
	    sb.append(JsonUtil.map(Utils.makeList("error",JsonUtil.quote("Error:" + exc))));
	    Result result  = new Result("", sb, JsonUtil.MIMETYPE);
	    result.setResponseCode(Result.RESPONSE_INTERNALERROR);
	    return result;
	}
    }


    public Result processEntryWikiText(Request request) throws Exception {
	StringBuilder sb = new StringBuilder();
	Entry entry = getEntryFromRequest(request, ARG_ENTRYID,
					  getRepository().URL_ENTRY_SHOW,false);
	String what = request.getString("what","");
	if(entry!=null) {
	    String text = "";
	    if(what.equals("description"))
		text = entry.getDescription();
	    else if(what.equals("children_ids")) {
		StringBuilder buff = new StringBuilder();
		for(Entry child: getEntryUtil().sortEntriesOn(getChildren(request, entry),
							      ORDERBY_ENTRYORDER+","+
							      ORDERBY_NAME,false)) {
							      
		    if(buff.length()>0) buff.append(",");
		    buff.append(child.getId());
		}
		text = buff.toString();
	    } else if(what.equals("children_links")) {
		StringBuilder buff = new StringBuilder();
		for(Entry child: getEntryUtil().sortEntriesOn(getChildren(request, entry),
							      ORDERBY_ENTRYORDER+","+
							      ORDERBY_NAME,false)) {
		    buff.append("[[");
		    buff.append(child.getId());
		    buff.append("|");
		    buff.append(child.getName());
		    buff.append("]]\n");
		}
		text = buff.toString();
	    }  else
		text = entry.getTypeHandler().getWikiTemplate(request, entry);
	    if(text!=null)
		sb.append(text.trim());
	}
	if(sb.length()==0) sb.append("no wiki text");
	Result result  = new Result("", sb, IO.MIME_TEXT);
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
    public Result processEntryShow(Request request) throws Exception {
        if (request.getCheckingAuthMethod()) {
            OutputHandler handler = getRepository().getOutputHandler(request);
            return new Result(handler.getAuthorizationMethod(request));
        }

	request.setIsEntryShow(true);


        Entry entry = null;
        if (request.exists("parentof")) {
            Entry sibling = getEntry(request,
                                     request.getString("parentof", ""));
            if (sibling != null) {
                entry = sibling.getParentEntry();
            }
        }

        if (entry == null) {
	    String id = request.getString(ARG_ENTRYID,null);
	    if(id!=null &&
	       isSynthEntry(id) && (request.getIsRobot() || 
				    request.getIsGoogleBot())) {
		System.err.println("skipping synth entry from bot request:" + id);
		return getRepository().getNoRobotsResult(request);
	    }	       
            entry = getEntryFromRequest(request, ARG_ENTRYID,
                                        getRepository().URL_ENTRY_SHOW,false);
        }

        if (entry == null) {
            fatalError(request, "No entry specified");
        }



	if(getRepository().getLogActivity()) {
	    if(request.getString("product","").equals("points.json")) {
		logEntryActivity(request, entry,"data");
	    } else {
		logEntryActivity(request, entry,"view");
	    }
	}

	getSessionManager().setLastEntry(request, entry);
        addSessionEntry(request, entry);
        if (entry.getIsRemoteEntry()) {
            String redirectUrl = entry.getRemoteServer().getUrl()
		+ getRepository().URL_ENTRY_SHOW.getPath();
            String[] tuple = getRemoteEntryInfo(entry.getId());
            request.put(ARG_ENTRYID, tuple[1]);
            request.put(ARG_FULLURL, "true");


            redirectUrl = redirectUrl + "?" + request.getUrlArgs();
            URL           url        = new URL(redirectUrl);
            URLConnection connection = url.openConnection();
            InputStream   is         = connection.getInputStream();

            return new Result("", is, connection.getContentType());
        }


        if (request.get(ARG_NEXT, false)
	    || request.get(ARG_PREVIOUS, false)) {
            boolean next = request.get(ARG_NEXT, false);
            List<String> ids =
                getChildIds(request,
                            findGroup(request, entry.getParentEntryId()),
                            null);
            String nextId = null;
            int    index  = ids.indexOf(entry.getId());
            if (index >= 0) {
                if (next) {
                    index++;
                } else {
                    index--;
                }
                if (index < 0) {
                    index = ids.size() - 1;
                } else if (index >= ids.size()) {
                    index = 0;
                }
                nextId = ids.get(index);
            }
            //Do a redirect
            if (nextId != null) {
                request.put(ARG_ENTRYID, nextId);
                request.remove(ARG_NEXT, ARG_PREVIOUS);

                return new Result(request.getUrl());
            }
        }

        Result result = processEntryShow(request, entry);
        return addEntryHeader(request, entry, result);
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
    public Result processEntryTypes(Request request) throws Exception {
        String            output       = request.getString(ARG_OUTPUT, "");
	boolean asHtml = request.getRequestPath().endsWith(".html");
        List<String>      types        = new ArrayList<String>();
        List<TypeHandler> typeHandlers = asHtml?
	    getRepository().getTypeHandlers():
	    getRepository().getTypeHandlersForDisplay(false);
        boolean           checkCnt     = request.get("checkcount", true);
	HashSet only = null;
	String typesList = request.getString("types",null);
	if(typesList!=null) {
	    only = new HashSet();
	    for(String type: Utils.split(typesList,",")) {
		only.add(type);
	    }
	}
	if(asHtml) {
	    StringBuilder sb = new StringBuilder();
	    getPageHandler().sectionOpen(request, sb,"Entry Type",false);
	    HU.script(sb,"HtmlUtils.initPageSearch('.ramadda-type',null,'Find Type')");
	    sb.append("<table><tr><td class=ramadda-table-heading>Type name</td><td class=ramadda-table-heading>Type ID</td></tr>");
	    for (TypeHandler typeHandler : typeHandlers) {
		String icon = HU.img(typeHandler.getTypeIconUrl(),"",HU.attr("width",ICON_WIDTH));
		sb.append(HU.tr(HU.td(icon+" "+  typeHandler.getDescription())+
				HU.td(HU.span(typeHandler.getType(),HU.attr("class","ramadda-type-id"))),
				HU.attr("class","ramadda-type")));
	    }
	    sb.append("</table>");
	    HU.script(sb,"Utils.initCopyable('.ramadda-type-id');");
	    getPageHandler().sectionClose(request, sb);
	    return new Result("Entry Types",sb);
	}


        for (TypeHandler typeHandler : typeHandlers) {
	    if (!typeHandler.getIncludeInSearch() &&  !typeHandler.getForUser()) {
                continue;
            }
	    if(only!=null &&!only.contains(typeHandler.getType())) {
		continue;
	    }
            if (checkCnt) {
                int cnt = getEntryUtil().getEntryCount(typeHandler);
                if (!typeHandler.getIncludeInSearch() && cnt == 0) {
                    continue;
                }
            }
            types.add(typeHandler.getJson(request));
        }

        StringBuilder sb = new StringBuilder(JsonUtil.list(types));
        request.setReturnFilename("types.json");
        request.setCORSHeaderOnResponse();
        return new Result("", sb, JsonUtil.MIMETYPE);
    }

    public Result processEntryNames(Request request) throws Exception {
	List<String> ids = Utils.split(request.getString("entryids",""),",",true,true);
	List<String> names = new ArrayList<String>();
	for(String id: ids) {
	    Entry entry = getEntry(request, id);
	    if(entry!=null) {
		names.add(JsonUtil.map(JsonUtil.quoteList(Utils.makeList("id",id,"name",entry.getName(),"icon",getPageHandler().getIconUrl(request, entry)))));
	    }
	}
        StringBuilder sb = new StringBuilder(JsonUtil.list(names));
        request.setReturnFilename("names.json");
        request.setCORSHeaderOnResponse();
        return new Result("", sb, JsonUtil.MIMETYPE);
    }


    private static class EntryActivity {
	String entryId;
	Hashtable<String,Integer> counts = new Hashtable<String,Integer>();
	public EntryActivity(String entryId) {
	    this.entryId = entryId;
	}
	public synchronized void addActivity(String activity) {
	    Integer count = counts.get(activity);
	    if(count==null) {
		count = Integer.valueOf(0);
		counts.put(activity,count);
	    }
	    counts.put(activity,Integer.valueOf(count.intValue()+1));
	}

    }


    private Hashtable<String,EntryActivity> entryActivities = new Hashtable<String,EntryActivity>();



    private void logEntryActivityToDatabase(Request request, Entry entry, String activity) throws Exception {
	synchronized(entryActivities) {
	    EntryActivity entryActivity = entryActivities.get(entry.getId());
	    if(entryActivity==null) {
		entryActivity = new EntryActivity(entry.getId());
		entryActivities.put(entry.getId(), entryActivity);
	    }
	    entryActivity.addActivity(activity);
	}
	if(true) return;

	Date dttm = new Date();
        GregorianCalendar cal =
            new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);
        cal.setTime(dttm);
	ZonedDateTime input = ZonedDateTime.now();
	input = input.of(input.getYear(),input.getMonthValue(),input.getDayOfMonth(),0,0,0,0,input.getZone());
	System.out.println(input);
	ZonedDateTime startOfLastWeek = input.minusWeeks(1).with(DayOfWeek.SUNDAY);
	ZonedDateTime endOfLastWeek = startOfLastWeek.plusDays(6);
	System.out.println(endOfLastWeek);

	long week =  endOfLastWeek.toInstant().toEpochMilli();

	getDatabaseManager().executeInsert(Tables.ENTRY_ACTIVITY.INSERT,
					   new Object[] {entry.getId(),dttm,new Date(week),
					       activity, request.getIp()});

    }    

    public void logEntryActivity(Request request, Entry entry, String activity) throws Exception {
	if(getRepository().getLogActivityToFile()) getLogManager().logActivity(request, entry, activity);
	if(getRepository().getLogActivityToDatabase()) logEntryActivityToDatabase(request, entry, activity);	
    }

    public Result processEntryActivity(Request request) throws Exception {
	Entry entry = getEntry(request);
	StringBuilder sb = new StringBuilder();
	sb.append(request.formPost(getRepository().URL_ENTRY_ACTIVITY));
	sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
	getPageHandler().entrySectionOpen(request, entry, sb,
					  "Entry Activity");

	if(request.exists(ARG_DELETE) && !request.exists(ARG_CANCEL)) {
	    if(request.exists(ARG_CONFIRM)) {
		getDatabaseManager().delete(Tables.ENTRY_ACTIVITY.NAME,
					    Clause.and(Clause.eq(Tables.ENTRY_ACTIVITY.COL_ENTRYID,entry.getId()),Clause.eq(Tables.ENTRY_ACTIVITY.COL_ACTIVITY, request.getString(ARG_DELETE,""))));

		sb.append("Events deleted<br>");
	    } else {
		sb.append(HU.hidden(ARG_DELETE,request.getString(ARG_DELETE,"")));
		sb.append(getPageHandler().showDialogQuestion("Are you sure you want to clear the " + request.getString(ARG_DELETE,"")+" activity?",
							      HU.submit("Yes",  ARG_CONFIRM) +" " +
							      HU.submit(LABEL_CANCEL, ARG_CANCEL)));
	    }
	}

	String ARG_BYDATE = "bydate";
	Clause clause = Clause.eq(Tables.ENTRY_ACTIVITY.COL_ENTRYID, entry.getId());


	SimpleDateFormat sdf =
	    RepositoryUtil.makeDateFormat("yyyy-MM-dd");
	String extra = SqlUtil.groupBy(SqlUtil.comma(Tables.ENTRY_ACTIVITY.COL_ENTRYID,Tables.ENTRY_ACTIVITY.COL_ACTIVITY));
	if(true || request.exists(ARG_BYDATE)) {
	    //	    extra += ", CAST(" + Tables.ENTRY_ACTIVITY.COL_DATE +" AS DATE)";	    
	    extra += ", date(" + Tables.ENTRY_ACTIVITY.COL_DATE +")";
	    extra += " order by date(" + Tables.ENTRY_ACTIVITY.COL_DATE +")";	    
	}

	System.err.println(extra);
        Statement stmt =
            getDatabaseManager().select(SqlUtil.comma(new String[] {
			SqlUtil.comma(Tables.ENTRY_ACTIVITY.COL_ACTIVITY,
				      "date(" +Tables.ENTRY_ACTIVITY.COL_DATE+")",
				      SqlUtil.count(Tables.ENTRY_ACTIVITY.COL_ENTRYID))
		    }), Tables.ENTRY_ACTIVITY.NAME, clause, extra);

	ResultSet        results;
	SqlUtil.Iterator iter = getDatabaseManager().getIterator(stmt);
	boolean didOne  = false;
	String url = getRepository().URL_ENTRY_ACTIVITY+"?" + HU.arg(ARG_ENTRYID,entry.getId());
	Hashtable<Date,Object[]> events = new Hashtable<Date,Object[]>();
	HashSet<String> seen  = new HashSet<String>();
	while ((results = iter.getNext()) != null) {
	    if(!didOne) {
		didOne = true;
		sb.append("<table class=formtable><tr class=entry-list-header><td></td><td style='padding-left:10px;padding-right:10px;' class=entry-list-header-column>Date</td><td style='padding-left:10px;padding-right:10px;' class=entry-list-header-column>Activity</td><td style='padding-left:10px;padding-right:10px;'  class=entry-list-header-column> Count </td>");
	    }
	    int col=1;
	    sb.append("<tr>");
	    String activity = results.getString(col++);
	    Date date = getDatabaseManager().getTimestamp( results,  col++);
	    Object[] tuple = events.get(date);
	    if(tuple==null) {
		tuple = new Object[]{0,0,0};
		events.put(date,tuple);
	    }
	    sb.append(HU.col(HU.href(url+"&"+HU.arg(ARG_DELETE,activity),HU.getIconImage("fas fa-trash-alt",HU.attr("title","Clear these events")))));
	    sb.append(HU.col(sdf.format(date)));
	    sb.append(HU.col(activity));
	    sb.append(HU.col(results.getString(col++),"align=right"));		      
	    sb.append("</tr>");
	}
	if(didOne) {
	    sb.append("</table>");
	} else {
	    sb.append("No activity");
	}
        sb.append(HU.formClose());
	getPageHandler().entrySectionClose(request, entry, sb);
        Result result = new Result("Entry Activity", sb);
	return addEntryHeader(request, entry, result);
    }



    public Result processEntryAddFile(Request request) throws Exception {
        getAuthManager().ensureAuthToken(request);
	StringBuilder sb = new StringBuilder();
	request.setReturnFilename("result.json");
	String fileContents = request.getString("file",(String) null);
	String fileName = request.getString("filename",(String) null);	
	fileName = fileName.replace("/","_");
	File tmpFile; 
	try {
	    tmpFile = getStorageManager().decodeFileContents(request,fileName, fileContents);
	} catch(Exception exc) {
	    exc.printStackTrace();
	    return new Result("",
			      new StringBuilder(JsonUtil.mapAndQuote(Utils.makeList("status","error","message",exc.getMessage()))), 
			      JsonUtil.MIMETYPE);
	}
	
	if(!tmpFile.exists()) {
	    sb.append(JsonUtil.mapAndQuote(Utils.makeList("status","error","message","Unable to write file:" + fileName)));
	    return new Result("", sb, JsonUtil.MIMETYPE);
	}
	return  processEntryAddFile(request, tmpFile,fileName,request.getString("description",""));
    }

    public Result processEntryAddFile(Request request, File tmpFile, String fileName, String description) throws Exception {
	StringBuilder sb = new StringBuilder();
	try {
	    Entry group = findGroup(request);
	    if ( !getAccessManager().canDoNew(request, group)) {
		sb.append(JsonUtil.mapAndQuote(Utils.makeList("status","error","message","You do not have permission to add a file")));
		return new Result("", sb, JsonUtil.MIMETYPE);
	    }

	    String fileType = request.getString("filetype","");
	    TypeHandler typeHandler = null;
	    if(fileType.length()>0) {
		typeHandler = getRepository().getTypeHandler(fileType);
	    }
	    if(typeHandler==null) {
		typeHandler = findDefaultTypeHandler(fileName);
	    }
	    //	    System.err.println("tmpFile:" + tmpFile +" " + tmpFile.exists() +  " file name:" + fileName);
	    tmpFile = getStorageManager().copyToStorage(request, tmpFile,fileName);
	    //	    System.err.println("newFile:" + tmpFile +" " + tmpFile.exists());
	    String name = request.getString(ARG_NAME,null);
	    if(name == null) {
		name = fileName;
		if(typeHandler.okToSetNewNameDefault()) {
		    name = fileName.replaceAll("_", " ");
		    name = IO.stripExtension(name);
		    name = StringUtil.camelCase(name);
		}
	    }
	    Entry newEntry = addFileEntry(request, tmpFile,
					  group, null, name, description, request.getUser(),
					  typeHandler, null);
	    String url = getEntryResourceUrl(request, newEntry,ARG_INLINE_DFLT,ARG_FULL_DFLT,ARG_ADDPATH_TRUE,false);

	    sb.append(JsonUtil.mapAndQuote(Utils.makeList("status","ok","message","File added","entryid",newEntry.getId(),"name",newEntry.getName(),"type",newEntry.getTypeHandler().getType(), "geturl",
							  url)));
	    return new Result("", sb, JsonUtil.MIMETYPE);
	} catch(Exception exc) {
	    System.err.println("Error:" + exc);
	    exc.printStackTrace();
	    sb.append(JsonUtil.mapAndQuote(Utils.makeList("status","error","message","Error:" + exc)));
	    return new Result("", sb, JsonUtil.MIMETYPE);
	}
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
    public Result processEntryDump(Request request) throws Exception {
        OutputStream os = request.getHttpServletResponse().getOutputStream();
        final PrintWriter pw = new PrintWriter(os);
        request.setReturnFilename("entries.txt");
        Statement stmt =
            getDatabaseManager().select(SqlUtil.comma(new String[] {
			Tables.ENTRIES.COL_ID,
			Tables.ENTRIES.COL_TYPE,
			Tables.ENTRIES.COL_NAME }), Tables.ENTRIES.NAME, null, null);
        getDatabaseManager().iterate(stmt, new SqlUtil.ResultsHandler() {
		public boolean handleResults(ResultSet results) throws Exception {
		    int col = 1;
		    pw.append(results.getString(col++));
		    pw.append(",");
		    pw.append(results.getString(col++));
		    pw.append(",");
		    pw.append(results.getString(col++));
		    pw.append("\n");

		    return true;
		}
	    });
        pw.close();

        return Result.makeNoOpResult();
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param result _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result addEntryHeader(Request request, Entry entry, Result result)
	throws Exception {

	boolean debug = false;
        if (entry == null) {
            return result;
        }

	if(request.getCloned()) {
	    if(debug) System.err.println("addEntryHeader cloned:" + entry);
	    return result;
	}
	if(request.isEmbedded()) {
	    if(debug) System.err.println("addEntryHeader embedded:" + entry);
	    return result;
	}
	String mime = result.getMimeType();
	if(mime!=null && !mime.equals("text/html")) {
	    if(debug) System.err.println("addEntryHeader don't need to write:" + entry +" " + request);
	    //	    if(debug)  System.err.println(Utils.getStack(10));
	    return result;
	}
	if(request.getExtraProperty("added entry header")!=null) {
	    if(debug) {
		System.err.println("addEntryHeader already added:" + entry);
		//		System.err.println(Utils.getStack(10));
	    }
	    return result;
	}
	request.putExtraProperty("added entry header","true");

	if(debug)
	    System.err.println("addEntryHeader:" + entry +" mime:" + result.getMimeType() +" " + request);
	//	if(debug)   System.err.println(Utils.getStack(10));
	
        if (Utils.stringUndefined(result.getTitle())) {
            result.setTitle(entry.getTypeHandler().getEntryName(entry));
        }

        //If entry is a dummy that means its from search results
        if (result.getShouldDecorate() && !entry.isDummy()) {
            Entry entryForHeader = entry;

            //If its a search result then use the top-level group for the header
            if (entry.isDummy()) {
                entryForHeader = request.getRootEntry();
            }
            String entryFooter = getPageHandler().entryFooter(request,
							      entryForHeader);

            StringBuilder menu = new StringBuilder();

            StringBuilder titleCrumbs = new StringBuilder();
            StringBuilder entryPopup = new StringBuilder();	        	    
            String crumbs = getPageHandler().getEntryHeader(request,
							    entryForHeader, titleCrumbs,entryPopup);
	    String url = getEntryUrl(request, entry); 
	    result.putProperty(PROP_ENTRY_NAME, entry.getName());
	    result.putProperty(PROP_ENTRY_POPUP, entryPopup.toString());
	    result.putProperty(PROP_ENTRY_URL, url);
            result.putProperty(PROP_ENTRY_HEADER, crumbs);
            result.putProperty(PROP_ENTRY_BREADCRUMBS, titleCrumbs.toString());
            result.putProperty(PROP_ENTRY_FOOTER, entryFooter);

            List<Metadata> metadataList =
                getMetadataManager().findMetadata(request, entry,
						  new String[]{ContentMetadataHandler.TYPE_LOGO}, true);
            if ((metadataList != null) && (metadataList.size() > 0)) {
                Metadata metadata = metadataList.get(0);
                String   title    = metadata.getAttr3();
                if (Utils.stringDefined(title)) {
                    result.putProperty(PROP_REPOSITORY_NAME, title);
                }
                String imageUrl = getMetadataManager().getType(
							       metadata).getImageUrl(
										     request, entry, metadata, null);
                if ((imageUrl != null) && (imageUrl.length() > 0)) {
                    result.putProperty(PROP_LOGO_IMAGE, imageUrl);
                }

                String logoUrl = metadata.getAttr2();
                if ((logoUrl != null) && (logoUrl.length() > 0)) {
                    result.putProperty(PROP_LOGO_URL, logoUrl);
                }

                String pageTitle = metadata.getAttr3();
                if ((pageTitle != null) && (pageTitle.length() > 0)) {
                    result.putProperty(PROP_REPOSITORY_NAME, pageTitle);
                }
            }

        }

        return result;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    private void printRequest(Request request, Entry entry) throws Exception {
        if (true) {
            return;
        }
        if ( !request.isEmbedded()
	     && (request.getString("output", null) == null)) {
            System.err.println("/entry " + entry.getName());
            System.err.println("https://" + request.getServerName() + ":"
                               + request.getServerPort() + request.getUrl());
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
    public Result processEntryShow(Request request, Entry entry)
	throws Exception {
	Result result = null;
        OutputHandler outputHandler =
            getRepository().getOutputHandler(request);
        if (request.getIsRobot()) {
            if ( !outputHandler.allowRobots()) {
                return getRepository().getNoRobotsResult(request);
            }
        }


        outputHandler.incrNumberOfConnections();
        OutputType outputType = request.getOutput();
        outputType.incrNumberOfCalls();
        boolean handleAsGroup = handleEntryAsGroup(entry);

	//Add the canonical link for google crawl
	String full = HU.url(getFullEntryShowUrl(request),ARG_ENTRYID,entry.getId());
	request.addHeadContent("<link rel=\"canonical\" href=\"" + full +"\" />");


        try {
            if (handleAsGroup) {
                result = processGroupShow(request, outputHandler, outputType,
                                          entry);
            } else {
                printRequest(request, entry);
                OutputType dfltOutputType = getDefaultOutputType(request,
								 entry, null);
                if (dfltOutputType != null) {
                    outputType = dfltOutputType;
                    outputHandler =
                        getRepository().getOutputHandler(outputType);
                }
                result = outputHandler.outputEntry(request, outputType,
						   entry);
            }
	} catch(Exception exc) {
	    getLogManager().logError("Error showing:" + entry.getName() +" id:" + entry.getId());
	    throw exc;
        } finally {
            outputHandler.decrNumberOfConnections();
        }

        return result;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param outputHandler _more_
     * @param outputType _more_
     * @param group _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result processGroupShow(Request request,
				    OutputHandler outputHandler,
				    OutputType outputType, Entry group)
	throws Exception {
        printRequest(request, group);
        boolean      doLatest    = request.get(ARG_LATEST, false);
        List<Entry>  children     = new ArrayList<Entry>();

	if(outputHandler.requiresChildrenEntries(request, outputType, group)) {
	    try {
		getChildrenEntries(request,outputHandler,group, children);
	    } catch (Exception exc) {
		exc.printStackTrace();
		request.put(ARG_MESSAGE, "Error finding children" + ":"
			    + exc.getMessage());
	    }
	}
        if (doLatest) {
            if (children.size() > 0) {
                children = getEntryUtil().sortEntriesOnDate(children, true);
                return outputHandler.outputEntry(request, outputType,
						 children.get(0));
            }
	}
        group.setChildren(children);

        OutputType dfltOutputType = getDefaultOutputType(request, group, children);
        if (dfltOutputType != null) {
            outputType    = dfltOutputType;
            outputHandler = getRepository().getOutputHandler(outputType);
        }
        Result result = outputHandler.outputGroup(request, outputType, group, children);

        return result;
    }


    
    public void getChildrenEntries(Request request, OutputHandler outputHandler, Entry group, List<Entry>children) throws Exception {
	TypeHandler  typeHandler = group.getTypeHandler();
	List<Clause> where       = typeHandler.assembleWhereClause(request);
	//	debugGetEntries = true;
	typeHandler.getChildrenEntries(
				       request, group, children,
				       new SelectInfo(request, group,where, outputHandler.getMaxEntryCount()));
	if(debugGetEntries) {
	    System.err.println("got:" + children.size());
	    for(int i=0;i<children.size() && i<10;i++) {
		System.err.println("E:" + children.get(i).getName());
	    }
	}
	
	if(request.getString(ARG_ORDERBY,"").equals(ORDERBY_NUMBER)) {
	    List<Entry> tmp = EntryUtil.sortEntriesOnNumber(children, !request.get(ARG_ASCENDING,true));
	    children.clear();
	    children.addAll(tmp);
	}


    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    private OutputType getDefaultOutputType(Request request, Entry entry,
                                            List<Entry> children) {
        if ( !request.defined(ARG_OUTPUT)) {
            for (PageDecorator pageDecorator :
		     repository.getPluginManager().getPageDecorators()) {
                String defaultOutput =
                    pageDecorator.getDefaultOutputType(getRepository(),
						       request, entry, children);
                if (defaultOutput != null) {
                    OutputType outputType =
                        getRepository().findOutputType(defaultOutput);
                    request.put(ARG_OUTPUT, defaultOutput);

                    return outputType;
                }
            }
        }

        return null;
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
    public Result processEntryAccess(Request request) throws Exception {
        Entry entry = getEntry(request);

        return entry.getTypeHandler().processEntryAccess(request, entry);
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
    public Result processEntryTypeAction(Request request) throws Exception {
        Entry entry = getEntry(request);
        if (entry == null) {
            throw new RepositoryUtil.MissingEntryException(
							   "Could not find entry");
        }

        return entry.getTypeHandler().processEntryAction(request, entry);
    }


    private Object VOTE_MUTEX=new Object();
    public Result processEntryVote(Request request) throws Exception {
	StringBuilder sb = new StringBuilder();
        Entry         entry = getEntry(request);
	if(entry==null) {
	    sb.append(JsonUtil.mapAndQuote(Utils.makeList("error", "Could not find entry")));
	    return new Result("", sb, JsonUtil.MIMETYPE);
	}
	
	synchronized(VOTE_MUTEX) {
	    List<Metadata> metadataList =
		getMetadataManager().findMetadata(request, entry,
						  new String[]{"content.votes"},
						  false);
	    String json;
	    File f;
	    if(metadataList!=null && metadataList.size()>0) {
		f = getMetadataManager().getFile(request, entry,
						 metadataList.get(0), 1);
		json = getStorageManager().readFile(f.toString());
	    } else {
		if ( !getAccessManager().canDoEdit(request, entry)) {
		    return new Result("", new StringBuilder(JsonUtil.map("error",JsonUtil.quote("No vote file available"))), JsonUtil.MIMETYPE);	
		}
		json = "{}";
		f = getStorageManager().getTmpFile("votes.json");
		IOUtil.writeFile(f, json);
		f = new File(getStorageManager().moveToEntryDir(entry,
								f).getName());
		getMetadataManager().addMetadata(request,entry,
						 new Metadata(getRepository().getGUID(), entry.getId(),
							      "content.votes", false, f.toString(), "", "", "",""));
		getEntryManager().updateEntry(null, entry);
	    }

	    if(request.defined("key") && request.defined("vote")) {
		String key = request.getString("key","");
		String vote = request.getString("vote","");	        	    
		JSONObject obj = new JSONObject(json);
		JSONObject keyObj = obj.optJSONObject(key);
		if(keyObj==null) {
		    keyObj = new JSONObject();
		    obj.put(key,keyObj);	    
		    keyObj.put("yes",0);
		    keyObj.put("no",0);		
		}
		keyObj.put(vote,keyObj.optInt(vote,0)+1);
		json = obj.toString();
		IOUtil.writeFile(f,json);
		sb.append(JsonUtil.map(Utils.makeList("ok", "true")));
		if(!request.get("returnvotes",false))
		    return new Result("", sb, JsonUtil.MIMETYPE);
	    }
	    return new Result("", new StringBuilder(json), JsonUtil.MIMETYPE);	
	}
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
    public Result processEntryForm(Request request) throws Exception {
        Entry entry = null;

        if (request.defined(ARG_ENTRYID)) {
            entry = getEntry(request);
        }
        StringBuilder sb    = new StringBuilder();
        Entry         group = addEntryForm(request, entry, sb);
        getPageHandler().entrySectionClose(request, entry, sb);
        if (entry == null) {
	    if(group!=null && !canAddTo(request, group)) {
		sb    = new StringBuilder();
		getPageHandler().entrySectionOpen(request, group, sb,"Entry Add");
		sb.append(getPageHandler().showDialogError("You do not have permission to add a new entry"));
		getPageHandler().entrySectionClose(request, entry, sb);
	    }
            return addEntryHeader(request, group,
                                  new Result("Add Entry", sb));
        }

	if ( !getAccessManager().canDoEdit(request, entry)) {
	    throw new AccessException("Cannot edit:" + entry.getLabel(),
				      request);
	}

        return makeEntryEditResult(request, entry, "Edit Entry", sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry addEntryForm(Request request, Entry entry, Appendable sb)
	throws Exception {


        String type  = null;
        Entry  group = null;

        if (entry != null) {
            type = entry.getTypeHandler().getType();
            if ( !entry.isTopEntry()) {
                group = findGroup(request, entry.getParentEntryId());
            }
        }



        boolean isEntryTop = ((entry != null) && entry.isTopEntry());
        if ( !isEntryTop && (group == null)) {
            group = findGroup(request);
        }


        if (type == null) {
            type = request.getString(ARG_TYPE, (String) null);
        }

        TypeHandler typeHandler = ((entry == null)
                                   ? ((type == null)
                                      ? null
                                      : getRepository().getTypeHandler(type))
                                   : entry.getTypeHandler());

	if ( !canBeCreatedBy(request, typeHandler)) {
            getPageHandler().makeEntrySection(request, group, sb,
					      "New Entry Error",
					      getPageHandler().showDialogError(
									       "User cannot create entry of type:" + typeHandler));
	    return group;
	}
        addSessionType(request, type);


        if ((entry != null) && entry.getIsLocalFile()) {
            getPageHandler().entrySectionOpen(request, entry, sb,
					      "Local File");
            sb.append(
		      getPageHandler().showDialogWarning(
							 "This is a local file and cannot be edited"));
            if (request.getUser().getAdmin() && entry.isFile()) {
                sb.append("File path: " + entry.getResource().getPath());
            }
            getPageHandler().entrySectionClose(request, entry, sb);

            return group;
        }


        if (entry != null) {
            getPageHandler().entrySectionOpen(request, entry, sb, "Edit");
        } else {
            getPageHandler().entrySectionOpen(request, group, sb,
					      ((typeHandler != null)
					       ? msg("Create new") + " " + typeHandler.getLabel()
					       : msg("Create new entry")));
        }

	if(typeHandler!=null) {
	    typeHandler.addToEntryFormHeader(request, sb,entry);
	}


        String formId = request.getUniqueId("entryform_");
        if (type == null) {
            sb.append(request.form(getRepository().URL_ENTRY_FORM,
                                   HU.attr("name", "entryform")
                                   + HU.id(formId)));
        } else {
            request.uploadFormWithAuthToken(
					    sb, getRepository().URL_ENTRY_CHANGE,
					    HU.attr("name", "entryform") + HU.id(formId));
        }


        sb.append(HU.formTable("ramadda-entry-edit",true));
        String title = BLANK;

        if (type == null) {
	    HU.formEntry(sb,
			 msgLabel("Type"),
			 getRepository().makeTypeSelect(
							request, false, "", true, null));
	    HU.formEntry(sb, BLANK, HU.submit("Select Type to Add"));
            sb.append(HU.hidden(ARG_GROUP, group.getId()));
        } else {
            title = ((entry == null)
                     ? "Add Entry"
                     : "Edit Entry");
            String submitButton = HU.submit((entry == null)
					    ? "Add " + typeHandler.getLabel()
					    : "Save", ARG_SUBMIT,
					    makeButtonSubmitDialog(sb, ((entry == null)
									? msg("Creating Entry...")
									: msg("Changing Entry..."))));

            String nextButton = ((entry == null)
                                 ? ""
                                 : HU.submit("Save & Next",
					     ARG_SAVENEXT));


            String deleteButton = (((entry != null) && isTopEntry(entry))
                                   ? ""
                                   : HU.submit("Delete",
					       ARG_DELETE,
					       makeButtonSubmitDialog(sb,
								      "Deleting Entry...")));



            String cancelButton = HU.submit(LABEL_CANCEL, ARG_CANCEL);
            String buttons      = ((entry != null)
                                   ? HU.buttons(submitButton, cancelButton)
                                   : HU.buttons(submitButton,cancelButton));

            FormInfo formInfo = new FormInfo(formId);
	    String message=null;

	    List<Entry.EntryHistory> history = getEntryHistory(entry);
	    if(request.exists(ARG_UNDO)) {
		if(history!=null && history.size()>0) {
		    Entry.EntryHistory entryHistory = history.get(history.size()-1);
		    history.remove(history.size()-1);
		    message="Showing version: " + entryHistory.date;
		    formInfo.setHistory(entryHistory);
		}
	    }

	    if(history!=null && history.size()>0) {
		buttons+=HU.space(2) +HU.submit("Undo", ARG_UNDO,
						HU.attr("title","Revert description to:" +
							history.get(history.size()-1).date));
	    }




            HU.row(sb, HU.colspan(buttons, 2));
	    if(message!=null) {
		HU.row(sb, HU.colspan(message, 2));
	    }

            if (entry != null) {
                sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
                sb.append(HU.hidden(ARG_ENTRY_TIMESTAMP,
				    getEntryTimestamp(entry)));
                if (isAnonymousUpload(entry)) {
                    List<Metadata> metadataList =
                        getMetadataManager().findMetadata(request, entry,
							  new String[]{AdminMetadataHandler.TYPE_ANONYMOUS_UPLOAD},
							  false);
                    String extra = "";

                    if (metadataList != null) {
                        Metadata metadata = metadataList.get(0);
                        String   user     = metadata.getAttr1();
                        String   email    = metadata.getAttr4();
                        if (email == null) {
                            email = "";
                        }
                        extra = "<br><b>From user:</b> "
			    + metadata.getAttr1() + " <b>Email:</b> "
			    + email + " <b>IP:</b> "
			    + metadata.getAttr2();
                    }
                    String msg = HU.space(2) + msg("Make public?")
			+ extra;
                    sb.append(HU.formEntry(msgLabel("Publish"),
					   HU.checkbox(ARG_PUBLISH, "true", false)
					   + msg));
                }
            } else {
                sb.append(HU.hidden(ARG_TYPE, type));
                sb.append(HU.hidden(ARG_GROUP, group.getId()));
            }

            typeHandler.addToEntryForm(request, sb, group, entry, formInfo);
            formInfo.addToForm(sb);
            HU.row(sb, HU.colspan(buttons, 2));

        }
        HU.formTableClose(sb);
	//Add some space here so the map popup flows ok
	sb.append("<div style='height:100px;'></div>");

        return group;

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param type _more_
     *
     * @throws Exception _more_
     */
    public void addSessionType(Request request, String type)
	throws Exception {
        if (type == null) {
            return;
        }
        if ( !type.equals(TYPE_FILE) && !type.equals(TYPE_GROUP)) {
            List<String> pastTypes =
                (List<String>) getSessionManager().getSessionProperty(
								      request, SESSION_TYPES);
            if (pastTypes == null) {
                pastTypes = new ArrayList<String>();
                getSessionManager().putSessionProperty(request,
						       SESSION_TYPES, pastTypes);
            }
            pastTypes.remove(type);
            pastTypes.add(0, type);
            //cap it at 3 types
            if (pastTypes.size() > 3) {
                pastTypes.remove(3);
            }
        }

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
    public List<Entry> getSessionEntries(Request request) throws Exception {
        List<String> list =
            (List<String>) getSessionManager().getSessionProperty(request,
								  SESSION_ENTRIES);
        if (list == null) {
            list = new ArrayList<String>();
        }
        List<Entry> entries = new ArrayList<Entry>();
        for (String id : list) {
            Entry entry = getEntry(request, id);
            if (entry != null) {
                entries.add(entry);
            }
        }

        return entries;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void addSessionEntry(Request request, Entry entry)
	throws Exception {
        if (request.isAnonymous()) {
            return;
        }
        List<String> list =
            (List<String>) getSessionManager().getSessionProperty(request,
								  SESSION_ENTRIES);
        if (list == null) {
            list = new ArrayList<String>();
            getSessionManager().putSessionProperty(request, SESSION_ENTRIES,
						   list);
        }
	synchronized(list) {
	    list.remove(entry.getId());
	    list.add(0, entry.getId());
	    //Cap the size at 8
	    if (list.size() > 8) {
		list.remove(list.size() - 1);
	    }
	}
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    private String getEntryTimestamp(Entry entry) {
        long changeDate = entry.getChangeDate();

        return "" + changeDate;
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
    public Result processEntryChange(final Request request) throws Exception {
        boolean download = request.get(ARG_RESOURCE_DOWNLOAD, false);
        getAuthManager().ensureAuthToken(request);
        if (download) {
            ActionManager.Action action = new ActionManager.Action() {
		    public void run(Object actionId) throws Exception {
			try {
			    Result result = doProcessEntryChange(request, false,actionId);
			    String url = result.getRedirectUrl();
			    if(url!=null) {
				getActionManager().setContinueHtml(actionId,
								   HU.href(url,  msg("Continue")));
			    } else {
				String content = result.getStringContent();
				getActionManager().setContinueHtml(actionId,
								   content);
			    }
			    
			} catch(Exception exc) {
			    logError("",exc);
			}
		    }
		};

            return getActionManager().doAction(request, action,
					       "Downloading file", "");

        }



        return doProcessEntryChange(request, false, null);
    }



    public Result processEntryChangeField(Request request) throws Exception {
	StringBuilder sb = new StringBuilder();
	Entry entry = null;
	try {
	    entry = getEntry(request);
	} catch(Exception exc) {
	    getLogManager().logError("Processing entryChangeField",exc);
	}
	if(entry==null) {
	    sb.append(JsonUtil.mapAndQuote(Utils.makeList("error", "Could not find entry")));
	    return new Result("", sb, JsonUtil.MIMETYPE);
	}
	if ( !getAccessManager().canDoEdit(request, entry)) {
	    sb.append(JsonUtil.mapAndQuote(Utils.makeList("error", "No permission to edit entry")));
	    return new Result("", sb, JsonUtil.MIMETYPE);
	}

	String what = request.getString("what","");
	try {
	    if(what.equals("entryorder")) {
		entry.setEntryOrder(request.get("value",999));
	    } else  if(what.equals("name")) {
		entry.setName(request.getString("value",entry.getName()));		
	    }   else {
		sb.append(JsonUtil.mapAndQuote(Utils.makeList("error", "Unknown field:" +what)));
		return new Result("", sb, JsonUtil.MIMETYPE);
	    }
	} catch(Exception exc) {
	    sb.append(JsonUtil.mapAndQuote(Utils.makeList("error", "An error has occurred:" + exc)));
	    return new Result("", sb, JsonUtil.MIMETYPE);
	}
	updateEntry(request, entry);
	sb.append(JsonUtil.mapAndQuote(Utils.makeList("message", "OK, field has changed")));
	return new Result("", sb, JsonUtil.MIMETYPE);
    }

    public Result processEntrySetFile(final Request request) throws Exception {
	getAuthManager().ensureAuthToken(request);
	StringBuilder sb = new StringBuilder();
        Entry         entry = getEntry(request);
	if(entry==null) {
	    sb.append(JsonUtil.mapAndQuote(Utils.makeList("error", "Could not find entry")));
	    return new Result("", sb, JsonUtil.MIMETYPE);
	}
	if ( !getAccessManager().canDoEdit(request, entry)) {
	    sb.append(JsonUtil.mapAndQuote(Utils.makeList("error", "No permision to edit entry")));
	    return new Result("", sb, JsonUtil.MIMETYPE);
	}
	String contents = request.getString("file",(String)null);
	if(contents==null)  {
	    contents = request.getString("base64file",(String)null);
	    if(contents!=null) {
		contents = new String(Utils.decodeBase64(contents));
	    }
	}
	
	if(contents==null) {
	    sb.append(JsonUtil.mapAndQuote(Utils.makeList("error", "No file contents given")));
	    return new Result("", sb, JsonUtil.MIMETYPE);
	}

        if (!entry.isFile()) {
	    //Probably don't need/want this check
	    //	    sb.append(JsonUtil.mapAndQuote(Utils.makeList("error", "Entry is not a file")));
	    //	    return new Result("", sb, JsonUtil.MIMETYPE);
	}

	removeFromCache(entry);
	String oldFileName = null;
	File oldFile = entry.getResource().getTheFile();
	if(oldFile!=null && oldFile.exists()) {
	    oldFileName = getStorageManager().getFileTail(oldFile.getName());
	} else {
	    oldFileName = entry.getTypeHandler().getDefaultFilename();
	}
	File tmpFile = getStorageManager().getTmpFile(oldFileName);
        OutputStream  toStream   = getStorageManager().getFileOutputStream(tmpFile);
        IOUtil.writeTo(new ByteArrayInputStream(contents.getBytes()), toStream);
        IO.close(toStream);
	File newFile = getStorageManager().moveToStorage(request,
							 tmpFile);
	entry.getResource().setFile(newFile,Resource.TYPE_STOREDFILE);

	//Check for bounds
	if(request.exists("bounds")) {
	    List<String> pts   = Utils.split(request.getString("bounds",""),",",true,true);
	    if(pts.size()==4) {
		entry.setNorth(Double.parseDouble(pts.get(0)));
		entry.setWest(Double.parseDouble(pts.get(1)));
		entry.setSouth(Double.parseDouble(pts.get(2)));
		entry.setEast(Double.parseDouble(pts.get(3)));		
	    }
	}


	updateEntry(request, entry);
	sb.append(JsonUtil.mapAndQuote(Utils.makeList("message", "OK, file has been saved")));
	return new Result("", sb, JsonUtil.MIMETYPE);
    }

    
    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public String getEntryListName(Request request, Entry entry) {
        return entry.getTypeHandler().getEntryListName(request, entry);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param parent _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canAddTo(Request request, Entry parent) throws Exception {
        return getRepository().getAccessManager().canDoNew(request,    parent);

    }



    public void initEntry(Entry entry,
			   String name, String description, Entry parentEntry,
			   User user, Resource resource, String category,
			   int entryOrder, long createDate, long changeDate,
			   long startDate, long endDate, Object[] values) {
	entry.initEntry(name, description, parentEntry,user,resource,category,entryOrder,createDate,
			changeDate, startDate, endDate, values);
	parentageChanged(parentEntry);

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param typeHandler _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canBeCreatedBy(Request request, TypeHandler typeHandler)
	throws Exception {
        if ( !typeHandler.canBeCreatedBy(request)) {
            return false;
        }

        String ips = getRepository().getProperty("ramadda.type."
						 + typeHandler.getType() + ".ips", null);
        if (ips != null) {
            String  requestIp = request.getIp();
            boolean ok        = false;
            for (String ip : Utils.split(ips, ";", true, true)) {
                if (requestIp.startsWith(ip)) {
                    ok = true;

                    break;
                }
            }
            if ( !ok) {
                return false;
            }
        }

        return true;
    }


    private Result doProcessEntryChange(Request request, boolean forUpload,
                                        Object actionId)
	throws Exception {
	Entry  parentEntry=null;
	Entry entry = null;
        if (request.defined(ARG_GROUP)) {
	    parentEntry = findGroup(request);
	}
        if (request.defined(ARG_ENTRYID)) {
            entry = getEntry(request);
	}
	try {
	    return  doProcessEntryChangeInner(request,  forUpload, actionId, parentEntry, entry);
	} catch(Exception exc) {
	    logError("", exc);
	    StringBuilder sb = new StringBuilder();
            Throwable     inner     = LogUtil.getInnerException(exc);
	    Entry theEntry= entry!=null?entry:parentEntry;
	    if(theEntry!=null)
		getPageHandler().entrySectionOpen(request, theEntry, sb, "Error");

	    sb.append(getPageHandler().showDialogError(inner.getMessage()));
            if ((request.getUser() != null) && request.getUser().getAdmin()) {
                sb.append(
			  HtmlUtils.pre(
					HtmlUtils.entityEncode(
							       LogUtil.getStackTrace(inner))));
            }
	    if(theEntry!=null)
		getPageHandler().entrySectionClose(request, theEntry, sb);
            Result result = new Result("Error", sb);
	    result.setResponseCode(Result.RESPONSE_INTERNALERROR);
	    return result;
	}
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param forUpload _more_
     * @param actionId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result doProcessEntryChangeInner(Request request, boolean forUpload,
					     Object actionId, Entry parentEntry, Entry entry)
	throws Exception {
        User user = request.getUser();
        if (forUpload) {
            logInfo("upload doProcessEntryChange user = " + user);
        }

        TypeHandler typeHandler = null;
        boolean     newEntry    = true;
        if (request.defined(ARG_ENTRYID)) {
            if (entry == null) {
                fatalError(request, "Cannot find entry");
            }
            if (forUpload) {
                fatalError(request, "Cannot edit when doing an upload");
            }
            if ( !getAccessManager().canDoEdit(request, entry)) {
                throw new AccessException("Cannot edit:" + entry.getLabel(),
                                          request);
            }
            if (entry.getIsLocalFile()) {
                return new Result(
				  request.entryUrl(
						   getRepository().URL_ENTRY_SHOW, entry, ARG_MESSAGE,
						   "Cannot edit local files"));

            }


            if (request.exists(ARG_UNDO)) {
		return processEntryForm(request);
	    }


            if (request.exists(ARG_CANCEL)) {
                return new Result(
				  request.entryUrl(getRepository().URL_ENTRY_SHOW, entry));
            }


            //Remove this entry from the memory cache 
            //so edits don't show up for others
            removeFromCache(entry);
	    addEntryHistory(entry);

            typeHandler = entry.getTypeHandler();
            newEntry    = false;


            if ((entry != null) && isAnonymousUpload(entry)) {
                if (request.get(ARG_JUSTPUBLISH, false)) {
                    publishAnonymousEntry(request, entry);
                    List<Entry> entries = new ArrayList<Entry>();
                    entries.add(entry);
                    insertEntries(request, entries, newEntry, false);

                    return new Result(
				      request.entryUrl(
						       getRepository().URL_ENTRY_FORM, entry));
                }
            }



            //If we have a timestamp then check if the user 
            //was editing an up to date entry
            if (request.defined(ARG_ENTRY_TIMESTAMP)) {
                String formTimestamp = request.getString(ARG_ENTRY_TIMESTAMP, "0");
                String currentTimestamp = getEntryTimestamp(entry);
                if ( !Misc.equals(formTimestamp, currentTimestamp)) {
                    StringBuilder sb        = new StringBuilder();
                    String        dateRange = "";
                    try {
			dateRange = getDateHandler().parseDate(formTimestamp) + ":"
			    + getDateHandler().parseDate(currentTimestamp);
                    } catch (Exception ignore) {}
                    getPageHandler().entrySectionOpen(request, entry, sb,
						      "Entry Edit");
                    sb.append(
			      getPageHandler().showDialogError(
							       msg(
								   "Error: The entry you are editing has been edited since the time you began the edit"
								   + dateRange+"<p>Below is the text you were editing")));
		    



		    sb.append(HU.textArea("",request.getString(ARG_DESCRIPTION,""),10,100));
                    getPageHandler().entrySectionClose(request, entry, sb);

                    return addEntryHeader(request, entry,
                                          new Result("Entry Edit Error",
						     sb));
                }
            }

            if (request.exists(ARG_DELETE_CONFIRM)) {
                if (entry.isTopEntry()) {
		    return new Result("", getPageHandler().makeEntryPage(request, entry, "",
									 getPageHandler().showDialogError("Cannot delete top-level folder")));
                }
                Entry group = findGroup(request, entry.getParentEntryId());
                deleteEntry(request, entry);
		return new Result("", getPageHandler().makeEntryPage(request, group, "Entry delete",
								     getPageHandler().showDialogError("Entry: " + entry.getName() +" is deleted")));

            }

            if (request.exists(ARG_DELETE)) {
                return new Result(
				  request.entryUrl(
						   getRepository().URL_ENTRY_DELETE, entry));
            }
        } else if (forUpload) {
            typeHandler =
                getRepository().getTypeHandler(TypeHandler.TYPE_CONTRIBUTION);
        } else {
            typeHandler =
                getRepository().getTypeHandler(request.getString(ARG_TYPE,
								 TypeHandler.TYPE_ANY), true);
        }

	if(!typeHandler.canCreate(request)) {
	    throw new IllegalArgumentException("User cannot create entry of type:" + typeHandler);
	}


	boolean testNew=request.get(ARG_TESTNEW, false);
	List<String> testLog = new ArrayList<String>();
        boolean     figureOutType = request.get(ARG_TYPE_GUESS, false);

	String datePattern = request.getUnsafeString(ARG_DATE_PATTERN,null);
	if(!stringDefined(datePattern)) datePattern = null;

        List<Entry> entries       = new ArrayList<Entry>();
        String      category      = "";
        if (request.defined(ARG_CATEGORY)) {
            category = request.getString(ARG_CATEGORY, "");
        } else {
            category = request.getString(ARG_CATEGORY_SELECT, "");
        }

	int entryOrder = request.get(ARG_ENTRYORDER,entry!=null?entry.getEntryOrder():Entry.DEFAULT_ORDER);
        File serverFile = null;
        if (request.defined(ARG_SERVERFILE)) {
            //IMPORTANT:
            request.ensureAdmin();
            serverFile = new File(request.getString(ARG_SERVERFILE,
						    (String) null));
            getStorageManager().checkLocalFile(serverFile);
        }

        if (entry == null) {

            if (forUpload) {
                logInfo("Upload:creating a new entry");
            }
            String groupId = request.getString(ARG_GROUP, (String) null);
            if (parentEntry == null) {
                fatalError(request, "You must specify a parent folder");
            }
            if (forUpload) {
                logInfo("Upload:checking access");
            }
            boolean okToCreateNewEntry =
		forUpload?getAccessManager().canDoUpload(request, parentEntry):
		getAccessManager().canDoNew(request, parentEntry);

            if (forUpload) {
                logInfo("Upload:is ok to create:" + okToCreateNewEntry);
            }
            if ( !okToCreateNewEntry) {
                throw new AccessException(msgLabel("You do not have permission to add entries to") +  parentEntry.getLabel(),
                                          request);
            }

	    List<NewEntryInfo> infos = new ArrayList<NewEntryInfo>();
            String       resource  = "";
            String urlArgument = request.getAnonymousEncodedString(ARG_URL,
								   BLANK);
            String  filename     = typeHandler.getUploadedFile(request);
            boolean unzipArchive = false;
            boolean stripExif = request.get(ARG_STRIPEXIF,false);

            boolean isFile       = false;
            String  resourceName = request.getString(ARG_FILE, BLANK);

            if (serverFile != null) {
                filename = serverFile.toString();
            }

            if (resourceName.length() == 0) {
                resourceName = IO.getFileTail(resource);
            }

            //The type might accept a .zip file - e.g., shapefiles

            if (typeHandler.getTypeProperty("upload.zip", false)) {
                unzipArchive = false;
            } else {
                unzipArchive = (forUpload
                                ? false
                                : request.get(ARG_FILE_UNZIP, false));
            }
		

            if (serverFile != null) {
                isFile   = true;
                resource = filename;
                if (forUpload) {
                    fatalError(request, "No filename specified");
                }
            } else if (filename != null) {
                //A File was uploaded
                isFile   = true;
                resource = filename;
            } else {
                if (forUpload) {
                    fatalError(request, "No filename specified");
                }

                //A URL was selected
                resource = urlArgument.trim();
                if ( !request.get(ARG_RESOURCE_DOWNLOAD, false)) {
                    unzipArchive = false;
                } else {
                    isFile = true;
                    File newFile = downloadUrl(request, resource, actionId,
					       parentEntry);
                    if (newFile == null) {
                        return new Result(
					  request.entryUrl(
							   getRepository().URL_ENTRY_SHOW, parentEntry,
							   ARG_MESSAGE,
							   "Error downloading URL"));
                    }
                    resourceName =
                        getStorageManager().getFileTail(newFile.toString());
                    resource = newFile.toString();
                }
            }

            boolean isGzip = resource.endsWith(".gz");
            // check if it's a zip file
            if (unzipArchive && !resource.toLowerCase().endsWith(".zip")) {
                unzipArchive = false;
            }

            boolean hasZip = false;
	    boolean hasUpload = false;
	    for(int i=0;i<100;i++) {
		if(!request.defined("upload_file_"+i)) continue;
		hasUpload  = true;
		String name = request.getString("upload_name_"+i);
		String contents = request.getString("upload_file_"+i);
		File tmpFile = getStorageManager().decodeFileContents(request, name, contents);
		if(request.get(ARG_FILE_UNZIP, false) && tmpFile.toString().endsWith(".zip")) {
		    unzipResource(request, parentEntry, user, infos,tmpFile.toString(), datePattern, testNew,  testLog);
		} else {
		    infos.add(new NewEntryInfo(name, tmpFile.toString(), parentEntry));
		}
	    }

	    if(hasUpload) {
		isFile = true;
		unzipArchive =  request.get(ARG_FILE_UNZIP, false);
	    } else if (serverFile != null) {
                if ( !serverFile.exists()) {
                    StringBuilder message =
                        new StringBuilder(
					  getPageHandler().showDialogError(
									   msg("File does not exist")));

                    return addEntryHeader(request, parentEntry,
                                          new Result("", message));
                }

                if (serverFile.isDirectory()) {
                    String pattern =
                        request.getString(ARG_SERVERFILE_PATTERN, null);
                    if (pattern!=null && pattern.length() == 0) {
                        pattern = null;
                    }
                    if (pattern != null) {
                        pattern = StringUtil.wildcardToRegexp(pattern);
                    }
                    //TODO for Don - Walk the tree
                    final String thePattern = pattern;
                    File[] files = serverFile.listFiles(new FileFilter() {
			    public boolean accept(File f) {
				if (thePattern == null) {
				    return true;
				}
				String name = f.getName();
				if (name.matches(thePattern)) {
				    return true;
				}

				return false;
			    }
			});
                    int fileCnt = 0;
                    for (File f : files) {
                        if (f.isFile()) {
			    infos.add(new NewEntryInfo(f.toString(),f.toString(), parentEntry));
                            fileCnt++;
                        }
                    }
                    if (fileCnt == 0) {
                        StringBuilder message =
                            new StringBuilder(
					      getPageHandler().showDialogError(
									       msg("No files found matching pattern")));

                        return addEntryHeader(request, parentEntry,
					      new Result("", message));
                    }
                } else {
		    infos.add(new NewEntryInfo(resourceName,serverFile.toString(), parentEntry));
                }
            } else if ( !unzipArchive) {
		infos.add(new NewEntryInfo(resourceName,resource, parentEntry));
            } else {
                hasZip = true;
		unzipResource(request, parentEntry, user, infos,resource, datePattern,testNew,  testLog);
	    }

            if (request.exists(ARG_CANCEL)) {
                return new Result(
				  request.entryUrl(
						   getRepository().URL_ENTRY_SHOW, parentEntry));
            }


            String description = getEntryDescription(request, entry);
            Date   createDate  = new Date();
            Date[] dateRange = request.getDateRange(ARG_FROMDATE, ARG_TODATE,
						    createDate);

            File originalFile = null;
	    for(NewEntryInfo info: infos) {
                String theResource = info.resource;
		if(stripExif && Utils.isImage(theResource) && new File(theResource).exists()) {
		    theResource  = ImageUtils.stripImageMetadata(theResource);
		}

                if (!testNew && isFile && (serverFile == null)) {
                    if (forUpload) {
                        theResource =
                            getStorageManager().moveToAnonymousStorage(
								       request, new File(theResource),
								       "").toString();
                    } else {
                        theResource =
                            getStorageManager().moveToStorage(request,
							      originalFile =
							      new File(theResource)).toString();
                    }
		}



                String id           = getRepository().getGUID();
                String resourceType = Resource.TYPE_UNKNOWN;
                if (serverFile != null) {
                    resourceType = Resource.TYPE_LOCAL_FILE;
                } else if (isFile) {
                    resourceType = Resource.TYPE_STOREDFILE;
                } else {
		    if(stringDefined(theResource)) {
			try {
			    new URL(theResource);
			    resourceType = Resource.TYPE_URL;
			} catch (Exception exc) {
			    System.err.println("Error: trying to determine resource type:" + exc +" for:" + theResource);
			}
		    }
                }
		if(resourceType == Resource.TYPE_URL) {
		    String _resource = theResource.toLowerCase();
		    //only allow http and https
		    if(!_resource.startsWith("http:")
		       && !_resource.startsWith("https:")) {
			throw new IllegalArgumentException("Malformed URL:" + theResource);
		    }

		}


                TypeHandler typeHandlerToUse = typeHandler;
                //See if we can figure out the type 
                if (figureOutType) {
                    TypeHandler tmp = findDefaultTypeHandler(theResource);
                    if (tmp != null) {
                        typeHandlerToUse = tmp;
                    }
                }

                if ( !canBeCreatedBy(request, typeHandlerToUse)) {
                    fatalError(request,
                               "Cannot create an entry of type "
                               + typeHandlerToUse.getDescription());
                }


                //If its an anon upload  or we're unzipping an archive then don't set the name
                String name = ((forUpload || hasZip)
                               ? ""
                               : request.getAnonymousEncodedString(ARG_NAME,
								   BLANK));


		boolean noName = false;
                if (name.indexOf("${") >= 0) {}
                if ((name.trim().length() == 0)
		    && typeHandlerToUse.okToSetNewNameDefault()) {
		    noName =true;
                    String nameTemplate =
                        typeHandlerToUse.getTypeProperty("nameTemplate",
						    (String) null);
                    if (nameTemplate == null) {
                        name = IO.getFileTail(Utils.getDefined("",info.name,info.resource));
                        if (request.get(ARG_MAKENAME, false)) {
                            name = name.replaceAll("_", " ");
                            name = IO.stripExtension(name);
                            StringBuilder tmp = new StringBuilder();
                            for (String tok :
				     Utils.split(name, " ", true, true)) {
                                tok = StringUtil.camelCase(tok);
                                tmp.append(tok);
                                tmp.append(" ");
                            }
                            name = tmp.toString().trim();
                        }
                    }
                }



                if (name.trim().length() == 0) {
                    String nameTemplate =
                        typeHandlerToUse.getTypeProperty("nameTemplate",
							 (String) null);
                    if (nameTemplate == null) {
                        name = typeHandlerToUse.getDefaultEntryName(info.name);
                    }
                }
                entry = typeHandlerToUse.createEntry(id);
                Date[] theDateRange = { dateRange[0], dateRange[1] };
                if (theDateRange[0] == null) {
		    if(datePattern!=null) {
			Date tmpDate = Utils.extractDate(datePattern, info.name);
			if(tmpDate!=null) {
			    theDateRange[0] = tmpDate;
			    if(testNew) entry.putTransientProperty("dateextract","extracted date from: " + info.name +" date: " + tmpDate);
			} else {
			    if(testNew) entry.putTransientProperty("dateextract","failed to extract date from: " + info.name);
			}
		    }
		}

		if(!entry.getTypeHandler().getTypeProperty("date.nullok",false)) {
		    if (theDateRange[0] == null) {
			theDateRange[0] = ((theDateRange[1] == null)
					   ? createDate
					   : theDateRange[1]);
		    }
		    if (theDateRange[1] == null) {
			theDateRange[1] = theDateRange[0];
		    }
		}
		if(noName)
		    entry.putTransientProperty("noname","true");
		    
                initEntry(entry, name, description, info.parent, request.getUser(),
			  new Resource(theResource, resourceType),
			  category, entryOrder,
			  createDate.getTime(),
			  createDate.getTime(),
			  DateHandler.getTime(theDateRange[0]),
			  DateHandler.getTime(theDateRange[1]), null);
                if (forUpload) {
                    initUploadedEntry(request, entry, info.parent);
                }

                setEntryState(request, entry, info.parent, newEntry);
                entries.add(entry);
            }
	} else {
            boolean fileUpload      = false;
            String  newResourceName = request.getUploadedFile(ARG_FILE);
            String  newResourceType = null;

            //Did they upload a new file???
            if (newResourceName != null) {
		if(!testNew) {
		    newResourceName = getStorageManager().moveToStorage(request,
									new File(newResourceName)).toString();
		}
                newResourceType = Resource.TYPE_STOREDFILE;
            } else if (serverFile != null) {
                newResourceName = serverFile.toString();
                newResourceType = Resource.TYPE_LOCAL_FILE;
            } else if (request.defined(ARG_URL)) {
                String url = request.getAnonymousEncodedString(ARG_URL, null);
                if (request.get(ARG_RESOURCE_DOWNLOAD, false)) {
                    File newFile = downloadUrl(request, url, actionId, entry);
                    if (newFile == null) {
                        return new Result(
					  request.entryUrl(
							   getRepository().URL_ENTRY_SHOW, entry,
							   ARG_MESSAGE,
							   "Error downloading URL"));

                    }
		    if(!testNew) {
			newResourceName =
			    getStorageManager().moveToStorage(request,
							      newFile).toString();
		    }
                    newResourceType = Resource.TYPE_LOCAL_FILE;
                } else {
                    newResourceName = url;
                    newResourceType = Resource.TYPE_URL;
                }
	    }


            if ((newResourceName != null)
		|| request.get(ARG_DELETEFILE, false)) {
                //If it was a stored file then remove the old one
                if (!testNew && entry.getResource().isStoredFile()) {
                    getStorageManager().removeFile(entry.getResource());
                }
                if (newResourceName != null) {
                    entry.setResource(new Resource(newResourceName,
						   newResourceType));
                } else {
                    entry.setResource(new Resource());
                }
            }


            Date[] dateRange = request.getDateRange(ARG_FROMDATE, ARG_TODATE,
						    new Date());
            String newName = request.getString(ARG_NAME, entry.getLabel());

            entry.setName(newName);
            entry.setDescription(getEntryDescription(request, entry));

            if (isAnonymousUpload(entry)) {
                if (request.get(ARG_PUBLISH, false)) {
                    publishAnonymousEntry(request, entry);
                }
            } else {
                entry.setCategory(category);
		entry.setEntryOrder(entryOrder);
            }


	    entry.setStartDate(dateRange[0]);
            if (dateRange[1] == null) {
                dateRange[1] = dateRange[0];
            }
	    entry.setEndDate(dateRange[1]);
            setEntryState(request, entry, entry.getParentEntry(), newEntry);
            entries.add(entry);
        }


        if (request.getUser().getAdmin() && request.defined(ARG_USER_ID)) {
            User newUser =
                getUserManager().findUser(request.getString(ARG_USER_ID,
							    "").trim());
            if (newUser == null) {
                fatalError(request,
                           "Could not find user: "
                           + request.getString(ARG_USER_ID, ""));
            }
            for (Entry theEntry : entries) {
                theEntry.setUser(newUser);
            }
        }



        try {
            if (!testNew && newEntry) {
                if (request.get(ARG_METADATA_ADD, false)) {
                    addInitialMetadata(request, entries, newEntry, false);
                } else if (request.get(ARG_METADATA_ADDSHORT, false)) {
                    addInitialMetadata(request, entries, newEntry, true);
                }
            }

	    List<String> tags = request.get(ARG_TAGS,new ArrayList<String>());
	    //Check for deleted metadata. This input gets set in ramadda.js
	    for(Entry e: entries) {
		List<Metadata> existingMetadata = getMetadataManager().getMetadata(request,e);
		for(Metadata mtd: existingMetadata) {
		    if(request.getString("metadata_state_" + mtd.getId(),"").equals("delete")) {
			mtd.setMarkedForDelete(true);
		    }
		}
	    }

	    for(String tag: tags) {
		tag = tag.trim();
		if(tag.length()==0) continue;
		for(Entry e: entries) {
		    getMetadataManager().addMetadata(request,
						     e,
						     new Metadata(getRepository().getGUID(), e.getId(),
								  "enum_tag", false, tag, "", "", "", ""),true);
		}
	    }


	    if(!testNew) {
		insertEntries(request, entries, newEntry, false);
		if (newEntry) {
		    for (Entry theNewEntry : entries) {
			theNewEntry.getTypeHandler().doFinalEntryInitialization(
										request, theNewEntry, false);
		    }
		}
	    } else {
		for(Entry theEntry: entries) {
		    String        entryIcon = HU.getIconImage(getPageHandler().getIconUrl(request, theEntry));
		    String extract = (String) theEntry.getTransientProperty("dateextract");
		    String message = entryIcon+" new entry: " + theEntry.getName() +"<br>" + HU.space(12) +"type: " + theEntry.getTypeHandler().getLabel();
		    if(extract!=null)
			message+="<br>" + HU.space(12)+extract;
		    testLog.add(message);
		}
	    }
        } catch (Exception exc) {
	    logError("", exc);
            Throwable inner = LogUtil.getInnerException(exc);
            if (parentEntry != null) {
                String msg =
                    getPageHandler().showDialogError(inner.getMessage());
                if ((request.getUser() != null)
		    && request.getUser().getAdmin()) {
                    msg += HU.pre(
				  HU.entityEncode(LogUtil.getStackTrace(inner)));
                }

                return getPageHandler().makeEntryHeaderResult(request,
							      parentEntry, "Entry Create Error", msg);
            }

            throw exc;
	}


	if(testNew) {
	    StringBuilder sb = new StringBuilder();
	    getPageHandler().entrySectionOpen(request, parentEntry, sb,
					      "Entry test create log");
	    sb.append(getPageHandler().showDialogNote("No entries were created"));
	    sb.append(Utils.join(testLog,"<br>"));
	    getPageHandler().entrySectionClose(request, parentEntry, sb);
	    return new Result("",sb);
	}

        if (forUpload|| request.exists(ARG_BULKUPLOAD)) {
            entry = (Entry) entries.get(0);
            return new Result(
			      request.entryUrl(
					       getRepository().URL_ENTRY_SHOW, entry.getParentEntry(),
					       ARG_MESSAGE,
					       "Entry has been uploaded"));
        }

	if(request.responseAsJson()) {
	    List<String> ids = new ArrayList<String>();
	    for(Entry e: entries) {
		ids.add(JsonUtil.quote(e.getId()));
	    }
	    StringBuilder sb = new StringBuilder();
	    sb.append(JsonUtil.map(Utils.makeList("entries",JsonUtil.list(ids))));
	    return new Result("", sb, JsonUtil.MIMETYPE);
	}

        if (entries.size() == 1) {
            entry = (Entry) entries.get(0);
	    if(!newEntry) {
		entry.getTypeHandler().entryChanged(entry);
	    }

            if (entry.getTypeHandler().returnToEditForm()) {
                return new Result(
				  request.entryUrl(getRepository().URL_ENTRY_FORM, entry));
            } else {
                return new Result(
				  request.entryUrl(getRepository().URL_ENTRY_SHOW, entry));
            }
        } else if (entries.size() > 1) {
            entry = (Entry) entries.get(0);

            return new Result(
			      request.entryUrl(
					       getRepository().URL_ENTRY_SHOW, entry.getParentEntry(),
					       ARG_MESSAGE,
					       entries.size()
					       + HU.pad("files uploaded")));
        } else {
	    StringBuilder sb = new StringBuilder();
	    if(parentEntry!=null)
		getPageHandler().entrySectionOpen(request, parentEntry, sb,
						  "Entry Create");
	    sb.append(getPageHandler().showDialogWarning("No entries created"));
	    if(parentEntry!=null)
		getPageHandler().entrySectionClose(request, parentEntry, sb);
            return new Result(BLANK,sb);

        }

    }


    private synchronized Entry.EntryHistory addEntryHistory(Entry entry) {
	Entry.EntryHistory history = entry.getTypeHandler().createHistory(entry);
	List<Entry.EntryHistory> list = entryHistories.get(entry.getId());
	if(list==null) {
	    list = new ArrayList<Entry.EntryHistory>();
	    entryHistories.put(entry.getId(),list);
	}
	//keep around the last 5
	while(list.size()>5) list.remove(0);
	list.add(history);
	return history;
    }
    
    private List<Entry.EntryHistory> getEntryHistory(Entry entry) {
	if(entry==null) return null;
	return entryHistories.get(entry.getId());
    }


    private void unzipResource(Request request, Entry parentEntry, User user,
			       List<NewEntryInfo> infos,
			       String resource,
			       String datePattern,
			       boolean testNew,List<String>testLog ) throws Exception {
	Hashtable<String, Entry> nameToGroup = new Hashtable<String,  Entry>();
	InputStream fis =
	    getStorageManager().getFileInputStream(resource);
	OutputStream fos = null;
	ZipInputStream zin =
	    getStorageManager().makeZipInputStream(fis);
	ZipEntry ze = null;
	try {
	    while ((ze = zin.getNextEntry()) != null) {
		if (ze.isDirectory()) {
		    continue;
		}
		String path = ze.getName();
		String name = IO.getFileTail(path);
		if (name.equals("MANIFEST.MF")) {
		    continue;
		}
		//Skip dot files as well
		if (name.startsWith(".")) {
		    continue;
		}
		Entry parent = parentEntry;
		if (request.get(ARG_FILE_PRESERVEDIRECTORY, false)) {
		    List<String> toks = Utils.split(path, "/",
						    true, true);
		    String ancestors = "";
		    //Remove the file name from the list of tokens
		    if (toks.size() > 0) {
			toks.remove(toks.size() - 1);
		    }
		    for (String parentName : toks) {
			parentName = parentName.replaceAll("_", " ");
			ancestors  = ancestors + "/" + parentName;
			Entry group = nameToGroup.get(ancestors);
			if (group == null) {
			    Request tmpRequest = request.cloneMe();
			    tmpRequest.setUser(user);
			    group = findGroupUnder(tmpRequest,
						   parent, parentName, user,testNew);
			    if(group!=null) {
				if(datePattern!=null) {
				    Date tmpDate = Utils.extractDate(datePattern, parentName);
				    if(testNew) {
					if(tmpDate!=null) 
					    testLog.add("Group: " + parentName +" extracted date: " + tmpDate);
					else
					    testLog.add("Group: " + parentName +" failed to extract date");
				    }
				    if(tmpDate!=null) {
					group.setStartDate(tmpDate.getTime());
					group.setEndDate(tmpDate.getTime());				    
				    }
				} else {
				    testLog.add("Group: " + parentName);
				}
				nameToGroup.put(ancestors, group);
			    } else {
				if(testNew) {
				    testLog.add("Failed to create group:" + parentName);
				}				
			    }
			}
			parent = group;
		    }
		}
		File f = getStorageManager().getTmpFile(name);
		fos = getStorageManager().getFileOutputStream(f);
		try {
		    IOUtil.writeTo(zin, fos);
		} finally {
		    IO.close(fos);
		}
		infos.add(new NewEntryInfo(name,f.toString(), parent));
	    }
	} finally {
	    IO.close(fis);
	    IO.close(zin);
	}
	parentageChanged(parentEntry);
    }	

    



    private static class NewEntryInfo {
	String name;
	Entry parent;
	String resource;
	public NewEntryInfo(String _name, String _resource, Entry _parent) {
	    name = _name;
	    resource = _resource;
	    parent = _parent;
	}

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
    private String getEntryDescription(Request request, Entry entry) throws Exception {
        boolean isWiki      = request.get(ARG_ISWIKI, false);
	boolean isDescriptionWiki= entry==null?false: entry.getTypeHandler().isDescriptionWiki(entry);
	String dflt = entry!=null?entry.getDescription():BLANK;
	if(dflt==null) dflt = BLANK;
        String  description = request.getAnonymousEncodedString(isWiki
								? ARG_WIKITEXT
								: ARG_DESCRIPTION, dflt).trim();

        if (request.get(ARG_ISWIKI, false)) {
            if ( !description.startsWith(WIKI_PREFIX)) {
                description = WIKI_PREFIX+"\n" + description;
            }
        } else {
            if (description.startsWith(WIKI_PREFIX) && !isDescriptionWiki) {
                description = description.substring(WIKI_PREFIX.length()).trim();
            }
        }

        return description;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param url _more_
     * @param actionId _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private File downloadUrl(Request request, String url, Object actionId,
                             Entry entry)
	throws Exception {
	getStorageManager().checkUrl(request,url);

        URL           fromUrl    = new URL(url);
        URLConnection connection = fromUrl.openConnection();
	String dispo = connection.getHeaderField("Content-disposition");
        String        tail       = IO.getFileTail(url);
	if(dispo!=null) {
	    String filename = StringUtil.findPattern(dispo,"filename=(.*)");
	    if(stringDefined(filename))
		tail = IO.cleanFileName(filename);
	}
        File          newFile = getStorageManager().getTmpFile(tail);

        InputStream   fromStream = connection.getInputStream();
        if (actionId != null) {
            ucar.unidata.util.JobManager.getManager().startLoad("File copy",
								actionId);
        }
        int length = connection.getContentLength();
        if (length > 0 & actionId != null) {
            getActionManager().setActionMessage(actionId,
						msg("Downloading") + " " + length + " " + msg("bytes"));
        }
        OutputStream toStream =
            getStorageManager().getFileOutputStream(newFile);
        try {
            long bytes = IOUtil.writeTo(fromStream, toStream, actionId,
                                        length);
            if (bytes < 0) {
                return null;
            }
        } finally {
            IO.close(toStream);
            IO.close(fromStream);
        }

        return newFile;
    }

    List<TypeHandler> sortedTypeHandlers;


    /**
     * _more_
     *
     * @param theResource _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public TypeHandler findDefaultTypeHandler(String theResource)
	throws Exception {
	return findDefaultTypeHandler(null,  theResource, false);
    }

    public TypeHandler findDefaultTypeHandler(Entry locale, String theResource, boolean isFile)
	throws Exception {	
        File   newFile   = new File(theResource);
        String shortName = newFile.getName();
	String _theResource= theResource.toLowerCase();
	//Sort them so we get the longest pattern first
	if(sortedTypeHandlers==null) {
	    List<TypeHandler> tmp = new ArrayList<TypeHandler>();
	    tmp.addAll(getRepository().getTypeHandlers());
	    Comparator comp = new Comparator() {
		    public int compare(Object o1, Object o2) {
			TypeHandler t1 = (TypeHandler)o1;
			TypeHandler t2 = (TypeHandler)o2;			
			String p1 = t1.getFilePattern();
			String p2 = t2.getFilePattern();			
			if(p1==null) p1="";
			if(p2==null) p2="";
			return p2.length()-p1.length();
		    }
		};
	    Object[] array = tmp.toArray();
	    Arrays.sort(array, comp);
	    sortedTypeHandlers = (List<TypeHandler>) Misc.toList(array);
	}


	if(locale!=null) {
            List<Metadata> metadataList =
                getMetadataManager().findMetadata(getRepository().getAdminRequest(), locale,
						  new String[]{"entry_type_patterns"}, true);
	    if(metadataList!=null) {
		
		for(Metadata metadata: metadataList) {
		    String type = metadata.getAttr1();
		    for(String pattern: Utils.split(metadata.getAttr2(),"\n",true, true)) {
			if(pattern.startsWith("file:")) {
			    if(!isFile)continue;
			    pattern = pattern.substring("file:".length()).trim();
			    if(pattern.length()==0) {
				TypeHandler typeHandler =
				    getRepository().getTypeHandler(type);
				if(typeHandler!=null) return typeHandler;
			    }
			}
			if(pattern.startsWith("folder:")) {
			    if(isFile)continue;
			    pattern = pattern.substring("folder:".length()).trim();
			    if(pattern.length()==0) {
				TypeHandler typeHandler =
				    getRepository().getTypeHandler(type);
				if(typeHandler!=null) return typeHandler;
			    }
			}


			if(theResource.matches(pattern)) {
			    TypeHandler typeHandler =
				getRepository().getTypeHandler(type);
			    if(typeHandler!=null) return typeHandler;
			}

		    }
		}
	    }

	}

        //Handle case sensitive first
        for (TypeHandler otherTypeHandler :sortedTypeHandlers) {
            if (otherTypeHandler.canHandleResource(theResource, shortName)) {
                return otherTypeHandler;
            }
        }

        //now try any case
        for (TypeHandler otherTypeHandler :sortedTypeHandlers) {
            if (otherTypeHandler.canHandleResource(_theResource,
						   shortName.toLowerCase())) {
                return otherTypeHandler;
            }
        }



        return null;
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param template _more_
     *
     * @return _more_
     */
    public String replaceMacros(Entry entry, String template) {
        Date createDate = new Date(entry.getCreateDate());
        Date fromDate   = new Date(entry.getStartDate());
        Date toDate     = new Date(entry.getEndDate());

        String url = HU.url(getFullEntryShowUrl(null), ARG_ENTRYID,
			    entry.getId());
        //j-
        String[] macros = {
            "entryid", entry.getId(), "parentid", entry.getParentEntryId(),
            "resourcepath", entry.getResource().getPath(), "resourcename",
            getStorageManager().getFileTail(entry.getResource().getPath()),
            "filename",
            getStorageManager().getFileTail(entry.getResource().getPath()),
            "fileextension",
            IO.getFileExtension(entry.getResource().getPath()), "name",
            getEntryDisplayName(entry), "fullname", entry.getFullName(),
            "user", entry.getUser().getLabel(), "url", url
        };

        //j+
        String result = template;

        for (int i = 0; i < macros.length; i += 2) {
            String macro = "${" + macros[i] + "}";
            String value = macros[i + 1];
            result = result.replace(macro, value);
        }

        return replaceMacros(result, createDate, fromDate, toDate);
    }



    /**
     * _more_
     *
     * @param template _more_
     * @param createDate _more_
     * @param fromDate _more_
     * @param toDate _more_
     *
     * @return _more_
     */
    public String replaceMacros(String template, Date createDate,
                                Date fromDate, Date toDate) {
        GregorianCalendar fromCal =
            new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);
        fromCal.setTime(fromDate);

        GregorianCalendar createCal =
            new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);
        createCal.setTime(createDate);

        GregorianCalendar toCal =
            new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);
        toCal.setTime(toDate);


        int createDay        = createCal.get(GregorianCalendar.DAY_OF_MONTH);
        int fromDay          = fromCal.get(GregorianCalendar.DAY_OF_MONTH);
        int toDay            = toCal.get(GregorianCalendar.DAY_OF_MONTH);

        int createWeek       = createCal.get(GregorianCalendar.WEEK_OF_MONTH);
        int fromWeek         = fromCal.get(GregorianCalendar.WEEK_OF_MONTH);
        int toWeek           = toCal.get(GregorianCalendar.WEEK_OF_MONTH);

        int createWeekOfYear = createCal.get(GregorianCalendar.WEEK_OF_YEAR);
        int fromWeekOfYear   = fromCal.get(GregorianCalendar.WEEK_OF_YEAR);
        int toWeekOfYear     = toCal.get(GregorianCalendar.WEEK_OF_YEAR);


        int createMonth      = createCal.get(GregorianCalendar.MONTH) + 1;
        int fromMonth        = fromCal.get(GregorianCalendar.MONTH) + 1;
        int toMonth          = toCal.get(GregorianCalendar.MONTH) + 1;

        int createYear       = createCal.get(GregorianCalendar.YEAR);
        int fromYear         = fromCal.get(GregorianCalendar.YEAR);
        int toYear           = toCal.get(GregorianCalendar.YEAR);



        //j-
        String[] macros = {
            "day", padZero(fromDay), "week", fromWeek + "", "month",
            padZero(fromMonth), "year", fromYear + "", "date",
            getDateHandler().formatDate(fromDate), "fromdate",
            getDateHandler().formatDate(fromDate), "monthname",
            DateUtil.MONTH_NAMES[fromMonth - 1], "create_day",
            padZero(createDay), "from_day", padZero(fromDay), "to_day",
            padZero(toDay), "create_week", "" + createWeek, "from_week",
            "" + fromWeek, "to_week", "" + toWeek, "create_weekofyear",
            "" + createWeekOfYear, "from_weekofyear", "" + fromWeekOfYear,
            "to_weekofyear", "" + toWeekOfYear, "create_date",
            getDateHandler().formatDate(createDate), "from_date",
            getDateHandler().formatDate(fromDate), "to_date",
            getDateHandler().formatDate(toDate), "create_month",
            padZero(createMonth), "from_month", padZero(fromMonth),
            "to_month", padZero(toMonth), "create_year", createYear + "",
            "from_year", fromYear + "", "to_year", toYear + "",
            "create_monthname", DateUtil.MONTH_NAMES[createMonth - 1],
            "from_monthname", DateUtil.MONTH_NAMES[fromMonth - 1],
            "to_monthname", DateUtil.MONTH_NAMES[toMonth - 1]
        };

        //j+
        String result = template;

        for (int i = 0; i < macros.length; i += 2) {
            String macro = "${" + macros[i] + "}";
            String value = macros[i + 1];
            result = result.replace(macro, value);
        }

        return result;
    }

    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    private String padZero(int v) {
        return ((v < 10)
                ? "0"
                : "") + v;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param parent _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    private void setEntryState(Request request, Entry entry, Entry parent,
                               boolean newEntry)
	throws Exception {
        if (request.exists(ARG_LOCATION_LATITUDE)
	    && request.exists(ARG_LOCATION_LONGITUDE)) {
	    if (request.defined(ARG_LOCATION_LATITUDE)
		&& request.defined(ARG_LOCATION_LONGITUDE)) {
		entry.setLatitude(GeoUtils.decodeLatLon(request.getString(ARG_LOCATION_LATITUDE,"")));
		entry.setLongitude(GeoUtils.decodeLatLon(request.getString(ARG_LOCATION_LONGITUDE,"")));
		if(entry.hasLocationDefined()) {
		    getSessionManager().putSessionProperty(request,
							   ARG_LOCATION_LATITUDE,
							   entry.getLatitude() + ";" + entry.getLongitude());
		}
	    } else {
		entry.setLatitude(Double.NaN);
		entry.setLongitude(Double.NaN);
	    }
        } else if (request.exists(ARG_AREA + "_south")) {
            boolean hasSouth = request.defined(ARG_AREA + "_south");
            boolean hasNorth = request.defined(ARG_AREA + "_north");
            boolean hasWest  = request.defined(ARG_AREA + "_west");
            boolean hasEast  = request.defined(ARG_AREA + "_east");

            if (hasNorth && hasWest && !hasSouth && !hasEast) {
                entry.setLatitude(request.getLatOrLonValue(ARG_AREA
							   + "_north", Entry.NONGEO));
                entry.setLongitude(request.getLatOrLonValue(ARG_AREA
							    + "_west", Entry.NONGEO));
            } else if (hasSouth && hasEast && !hasNorth && !hasWest) {
                entry.setLatitude(request.getLatOrLonValue(ARG_AREA
							   + "_south", Entry.NONGEO));
                entry.setLongitude(request.getLatOrLonValue(ARG_AREA
							    + "_east", Entry.NONGEO));
            } else {
                entry.setSouth(request.getLatOrLonValue(ARG_AREA + "_south",
							Entry.NONGEO));
                entry.setNorth(request.getLatOrLonValue(ARG_AREA + "_north",
							Entry.NONGEO));
                entry.setWest(request.getLatOrLonValue(ARG_AREA + "_west",
						       Entry.NONGEO));
                entry.setEast(request.getLatOrLonValue(ARG_AREA + "_east",
						       Entry.NONGEO));
            }
	    if(entry.hasAreaDefined()) {
		getRepository().getSessionManager().setArea(request,
							    entry.getNorth(), entry.getWest(), entry.getSouth(),
							    entry.getEast());
	    }

        }

        List<Entry> children       = null;


        double      altitudeTop    = entry.getAltitudeTop();
        double      altitudeBottom =  entry.getAltitudeBottom();
        if (request.defined(ARG_ALTITUDE)) {
            altitudeTop = altitudeBottom = request.get(ARG_ALTITUDE,
						       Entry.NONGEO);
        } else {
            if (request.defined(ARG_ALTITUDE_TOP)) {
                altitudeTop = request.get(ARG_ALTITUDE_TOP, Entry.NONGEO);
            }
            if (request.defined(ARG_ALTITUDE_BOTTOM)) {
                altitudeBottom = request.get(ARG_ALTITUDE_BOTTOM,
                                             Entry.NONGEO);
            }
        }
        entry.setAltitudeTop(altitudeTop);
        entry.setAltitudeBottom(altitudeBottom);
        entry.getTypeHandler().initializeEntryFromForm(request, entry,
						       parent, newEntry);
    }



    public boolean isTopEntry(Entry entry) {
	return getRootEntry().getId().equals(entry.getId());
    }



    public boolean okToDelete(Request request, Entry entry) throws Exception {
	if(isTopEntry(entry)) return false;
	//        if (entry.isTopEntry()) {return false;}
	if(!getAccessManager().canDoDelete(request, entry)) {
	    return false;
	}

        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry,
					      new String[]{AdminMetadataHandler.TYPE_PREVENTDELETION}, true);
        //Reset the category
        if (metadataList != null) {
	    for(Metadata mtd: metadataList) {
		if(Utils.equals("true",mtd.getAttr1())) {
		    return false;
		}
	    }
	}
	return true;
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
    public Result processEntryDelete(Request request) throws Exception {
        Entry         entry = getEntry(request);
        if (!okToDelete(request, entry)) {
	    return new Result("Entry Delete", getPageHandler().makeEntryPage(request, entry, "Entry delete",
									     getPageHandler().showDialogError("Cannot delete this entry")));
        }

        if (request.exists(ARG_CANCEL)) {
            return new Result(request.entryUrl(getRepository().URL_ENTRY_SHOW, entry));
        }


        StringBuilder sb    = new StringBuilder();
        if (request.exists(ARG_DELETE_CONFIRM)) {
            getAuthManager().ensureAuthToken(request);
            List<Entry> entries = new ArrayList<Entry>();
            entries.add(entry);
            Entry group = findGroup(request, entry.getParentEntryId());
            if (entry.isGroup()) {
                return asynchDeleteEntries(request, entries);
            } else {
                deleteEntries(request, entries, null);
                if (group == null) {
                    group = request.getRootEntry();
                }

                return new Result(
				  request.entryUrl(getRepository().URL_ENTRY_SHOW, group));
            }
        }

        String breadcrumbs = getPageHandler().getConfirmBreadCrumbs(request,
								    entry);
        StringBuilder inner = new StringBuilder();
        if (entry.isGroup()) {
            inner.append(
			 msg("Are you sure you want to delete the following folder?"));
	    inner.append("<br>");
            inner.append(
			 HU.b(
			      msg(
				  "Note: This will also delete everything contained by this folder")));
            inner.append(
			 HU.div(
				breadcrumbs, HU.cssClass("ramadda-confirm")));
        } else {
            inner.append(
			 msg("Are you sure you want to delete the following entry?"));
            inner.append(
			 HU.div(
				breadcrumbs, HU.cssClass("ramadda-confirm")));
        }



        StringBuilder fb = new StringBuilder();
        fb.append(request.form(getRepository().URL_ENTRY_DELETE, BLANK));

        getAuthManager().addAuthToken(request, fb);
        fb.append(HU.buttons(HU.submit("OK",
				       ARG_DELETE_CONFIRM), HU.submit(LABEL_CANCEL,
								      ARG_CANCEL)));
        fb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
        fb.append(HU.formClose());
        getPageHandler().entrySectionOpen(request, entry, sb, "Delete Entry");
        sb.append(getPageHandler().showDialogQuestion(inner.toString(),
						      fb.toString()));

        getPageHandler().entrySectionClose(request, entry, sb);

        return makeEntryEditResult(request, entry,
                                   "Entry delete confirm", sb);
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
    public Result processEntryListDelete(Request request) throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        for (String id :
		 Utils.split(request.getString(ARG_ENTRYIDS, ""), ",",
			     true, true)) {
            Entry entry = getEntry(request, id, false);
            if (entry == null) {
                throw new RepositoryUtil.MissingEntryException(
							       "Could not find entry:" + id);
            }
            if (isTopEntry(entry)) {
		return new Result("Entry Delete", getPageHandler().makeEntryPage(request, entry, "Entry delete",
										 getPageHandler().showDialogError("Cannot delete top-level folder")));
            }
            entries.add(entry);
        }


        return processEntryListDelete(request, entries);

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntryListDelete(Request request, List<Entry> entries)
	throws Exception {
        StringBuilder sb = new StringBuilder();
        Entry parent = getEntryFromRequest(request, ARG_ENTRYID,
					   getRepository().URL_ENTRY_GET,true);

        if (request.exists(ARG_CANCEL)) {
            if (entries.size() == 0) {
                return new Result(
				  request.makeUrl(getRepository().URL_ENTRY_SHOW));
            }
            String id = parent!=null?parent.getId():entries.get(0).getParentEntryId();
            return new Result(request.makeUrl(getRepository().URL_ENTRY_SHOW,
					      ARG_ENTRYID, id));
        }




        if (entries.size() == 0) {
            return new Result(
			      "",
			      getPageHandler().makeEntryPage(request, parent,"Entry Delete",
							     getPageHandler().showDialogWarning(
												msg("No entries selected"))));
        }


        StringBuilder entryListSB = new StringBuilder();
	boolean allOkToDelete = true;
	List<Entry> entriesCantDelete = new ArrayList<Entry>();

	for(Entry entryToDelete: entries) {
	    if(!okToDelete(request, entryToDelete)) {
		allOkToDelete=false;
		entriesCantDelete.add(entryToDelete);
	    }
	}

	if(!allOkToDelete) {
	    for (Entry entryCantDelete: entriesCantDelete) {
		entryListSB.append(getPageHandler().getConfirmBreadCrumbs(
									  request, entryCantDelete));
		entryListSB.append(HU.br());
	    }
            return new Result(
			      "",
			      getPageHandler().makeEntryPage(request, parent,"Entry Delete",
							     getPageHandler().showDialogWarning(
												msg("<b>Entry delete cancelled.</b><br>The following entries can't be deleted:<br>" + entryListSB))));

	}

        if (request.exists(ARG_DELETE_CONFIRM)) {
            getAuthManager().ensureAuthToken(request);
            return asynchDeleteEntries(request, entries);
        }



        StringBuilder msgSB    = new StringBuilder();
        StringBuilder idBuffer = new StringBuilder();
        for (Entry entry : entries) {
            idBuffer.append(",");
            idBuffer.append(entry.getId());
        }
        boolean       anyFolders  = false;
        for (Entry toBeDeletedEntry : entries) {
            entryListSB.append(getPageHandler().getConfirmBreadCrumbs(
								      request, toBeDeletedEntry));
            entryListSB.append(HU.br());
            if (toBeDeletedEntry.isGroup()) {
                anyFolders = true;
            }
        }

        if (entries.size() == 1) {
            msgSB.append(
			 msg("Are you sure you want to delete the following entry?"));
        } else {
            msgSB.append(
			 msg(
			     "Are you sure you want to delete all of the following entries?"));
        }
        if (anyFolders) {
            msgSB.append(HU.div(HU.b(
				     msg(
					 "Note: This will also delete everything contained by the below "
					 + ((entries.size() == 1)
					    ? "folder"
					    : "folders")))));
        }


        msgSB.append(HU.div(entryListSB.toString(),
			    HU.cssClass("ramadda-confirm")));

        request.formPostWithAuthToken(sb,
                                      getRepository().URL_ENTRY_DELETELIST);
        StringBuilder hidden =
            new StringBuilder(HU.hidden(ARG_ENTRYIDS,
					idBuffer.toString()));
        String form = PageHandler.makeOkCancelForm(request,
						   getRepository().URL_ENTRY_DELETELIST,
						   ARG_DELETE_CONFIRM, hidden.toString());
	sb.append(getPageHandler().makeEntryPage(request, parent,"Entry Delete",
						 getPageHandler().showDialogQuestion(msgSB.toString(),  form)));


        return new Result("Delete Confirm", sb);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     */
    public Result asynchDeleteEntries(Request request,
                                      final List<Entry> entries) {
        final Request        theRequest = request;
        Entry                entry      = entries.get(0);
        Entry                group      = entries.get(0).getParentEntry();
        final String         groupId    = entries.get(0).getParentEntryId();

        ActionManager.Action action     = new ActionManager.Action() {
		public void run(Object actionId) throws Exception {
		    deleteEntries(theRequest, entries, actionId);
		}
	    };
        String href = (group == null)
	    ? ""
	    : HtmlUtils.button(HU.href(
				       request.entryUrl(
							getRepository().URL_ENTRY_SHOW,
							group), "Continue"));

        return getActionManager().doAction(request, action, "Deleting entry",
                                           href, group);
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param title _more_
     * @param sb _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result makeEntryEditResult(Request request, Entry entry,
                                      String title, Appendable sb)
	throws Exception {
        Result result = new Result(title, sb);

        return addEntryHeader(request, entry, result);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void deleteEntry(Request request, Entry entry) throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);
        deleteEntries(request, entries, null);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param asynchId _more_
     *
     * @throws Exception _more_
     */
    public void deleteEntries(Request request, List<Entry> entries,
                              Object asynchId)
	throws Exception {

        if (entries.size() == 0) {
            return;
        }
        delCnt = 0;
        Connection connection = getDatabaseManager().getConnection();
        try {
            deleteEntriesInner(request, entries, connection, asynchId);
        } finally {
            getDatabaseManager().closeConnection(connection);
        }
    }


    /** _more_ */
    int delCnt = 0;

    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param connection _more_
     * @param actionId _more_
     *
     * @throws Exception _more_
     */
    private void deleteEntriesInner(final Request request, List<Entry> entries,
                                    Connection connection, Object actionId)
	throws Exception {

        //Exclude the synthetic entries
        List<Entry> okEntries = new ArrayList<Entry>();
        for (Entry entry : entries) {
            //we don't ask the type if its a synth type 
            if (
		/** entry.getTypeHandler().isSynthType()|| */
		isSynthEntry(entry.getId())) {
                if (getStorageManager().isProcessFile(entry.getFile())) {
                    IOUtil.deleteDirectory(entry.getFile());
                }

                continue;
            }
            okEntries.add(entry);
        }
        entries = okEntries;
	parentageChanged(entries);

        List<Descendent> found = getDescendents(request, entries, connection,
						null, true, true, actionId);

	boolean anyCantBeDeleted = false;
	StringBuilder errMsg = new StringBuilder();
	for (int i = found.size() - 1; i >= 0; i--) {
	    Descendent descendent = found.get(i);
	    if(!okToDelete(request, descendent.entry)) {
		anyCantBeDeleted = true;
		errMsg.append(getPageHandler().getConfirmBreadCrumbs(request, descendent.entry));
		errMsg.append(HU.br());
		break;
	    }		
	}

	if(anyCantBeDeleted) {
            String msg = "<b>Entry delete cancelled.</b><br>The following entry can't be deleted:<br>" + errMsg;
	    getActionManager().setContinueHtml(actionId, msg);
	    return;
	}	    

        String query;

        query =
            SqlUtil.makeDelete(Tables.PERMISSIONS.NAME,
                               SqlUtil.eq(Tables.PERMISSIONS.COL_ENTRY_ID,
                                          "?"));
        PreparedStatement permissionsStmt =
            connection.prepareStatement(query);

        query = SqlUtil.makeDelete(
				   Tables.ASSOCIATIONS.NAME,
				   SqlUtil.makeOr(
						  Misc.newList(
							       SqlUtil.eq(Tables.ASSOCIATIONS.COL_FROM_ENTRY_ID, "?"),
							       SqlUtil.eq(Tables.ASSOCIATIONS.COL_TO_ENTRY_ID, "?"))));

        PreparedStatement assocStmt = connection.prepareStatement(query);
        query = SqlUtil.makeDelete(Tables.COMMENTS.NAME,
                                   SqlUtil.eq(Tables.COMMENTS.COL_ENTRY_ID,
					      "?"));
        PreparedStatement commentsStmt = connection.prepareStatement(query);


        query = SqlUtil.makeDelete(Tables.METADATA.NAME,
                                   SqlUtil.eq(Tables.METADATA.COL_ENTRY_ID,
					      "?"));
        PreparedStatement metadataStmt = connection.prepareStatement(query);

        PreparedStatement entriesStmt = connection.prepareStatement(
								    SqlUtil.makeDelete(
										       Tables.ENTRIES.NAME,
										       Tables.ENTRIES.COL_ID, "?"));

        PreparedStatement[] statements = { permissionsStmt, metadataStmt,
                                           commentsStmt, assocStmt,
                                           entriesStmt };

        connection.setAutoCommit(false);
        Statement extraStmt = connection.createStatement();
        try {
            int batchCnt       = 0;
            int totalDeleteCnt = 0;
            //Go backwards so we go up the tree and hit the children first
            final List<String>   allIds            = new ArrayList<String>();
            List<Resource> resourcesToDelete = new ArrayList<Resource>();
            for (int i = found.size() - 1; i >= 0; i--) {
		Descendent descendent = found.get(i);
		/*                Object[] tuple  = found.get(i);
				  String   id     = (String) tuple[0];
				  Object[] values = (Object[]) tuple[4];
				  Entry    parent = (Entry) tuple[5];
		*/
                String   id     = descendent.id;
                Object[] values = descendent.values;
                Entry    parent = descendent.parent;
                removeFromCache(id);
                allIds.add(id);
                totalDeleteCnt++;
                if ((actionId != null)
		    && !getActionManager().getActionOk(actionId)) {
                    getActionManager().setActionMessage(actionId,
							"Delete canceled");
                    connection.rollback();

                    return;
                }
                getActionManager().setActionMessage(actionId,
						    "Deleted:" + totalDeleteCnt + "/" + found.size()
						    + " entries");

                resourcesToDelete.add(new Resource(new File(descendent.path), descendent.resourceType));
                batchCnt++;
                assocStmt.setString(2, id);
                for (PreparedStatement stmt : statements) {
                    stmt.setString(1, id);
                    stmt.addBatch();
                }

                TypeHandler typeHandler =
                    getRepository().getTypeHandler(descendent.type, true);
                typeHandler.deleteEntry(request, extraStmt, id, parent,
                                        values);
                if (batchCnt > 100) {
                    for (PreparedStatement stmt : statements) {
                        stmt.executeBatch();
                    }
                    batchCnt = 0;
                }
            }
            for (PreparedStatement stmt : statements) {
                stmt.executeBatch();
            }
            connection.commit();
            connection.setAutoCommit(true);
            for (Resource resource : resourcesToDelete) {
                getStorageManager().removeFile(resource);
            }
            for (String id : allIds) {
                getStorageManager().deleteEntryDir(id);
            }
	    getRepository().checkDeletedEntries(request,  allIds);

	    for(Descendent descendent: found) {
                String   id     = descendent.id;
                TypeHandler typeHandler =
                    getRepository().getTypeHandler(descendent.type, true);
		if(typeHandler!=null)
		    typeHandler.entryDeleted(id);
	    }

        } finally {
            getDatabaseManager().closeStatement(extraStmt);
            for (PreparedStatement stmt : statements) {
                getDatabaseManager().closeStatement(stmt);
            }
        }


	//clear out the type count
	getEntryUtil().clearCache();


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
    public Result processEntryUploadOk(Request request) throws Exception {
        StringBuilder sb    = new StringBuilder();
        Entry         entry = getEntry(request);
        //We use the category on the entry to flag the uploaded entries
        entry.setCategory("");
        addNewEntry(request, entry);

        return new Result(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                           entry));
    }

    /** _more_ */
    public final static String CATEGORY_UPLOAD = "upload";

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    private void publishAnonymousEntry(Request request, Entry entry)
	throws Exception {
        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry,
					      new String[]{AdminMetadataHandler.TYPE_ANONYMOUS_UPLOAD}, false);
        //Reset the category
        if (metadataList != null) {
            Metadata metadata = metadataList.get(0);
            User     newUser  =
                getUserManager().findUser(metadata.getAttr1());
            if (newUser != null) {
                entry.setUser(newUser);
            } else {
                entry.setUser(entry.getParentEntry().getUser());
            }
            entry.setCategory(metadata.getAttr3());
        } else {
            entry.setCategory("");
        }
        entry.setTypeHandler(
			     getRepository().getTypeHandler(TypeHandler.TYPE_FILE));

        if (entry.isFile()) {
            File newFile = getStorageManager().moveToStorage(request,
							     entry.getResource().getTheFile());
            entry.getResource().setPath(newFile.toString());
        }

    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public boolean isAnonymousUpload(Entry entry) {
        return Misc.equals(entry.getCategory(), CATEGORY_UPLOAD);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param parentEntry _more_
     *
     * @throws Exception _more_
     */
    private void initUploadedEntry(Request request, Entry entry,
                                   Entry parentEntry)
	throws Exception {


        String oldType = entry.getCategory();
        entry.setCategory(CATEGORY_UPLOAD);
        //Note: the name and description have already been encoded to prevent xss attacks
        //        entry.setName(Utils.encodeUntrustedText(getEntryDisplayName(entry)));
        //        entry.setDescription(
        //            Utils.encodeUntrustedText(entry.getDescription()));

        String fromName = Utils.encodeUntrustedText(
						    request.getString(
								      ARG_CONTRIBUTION_FROMNAME, ""));
        String fromEmail = Utils.encodeUntrustedText(
						     request.getString(
								       ARG_CONTRIBUTION_FROMEMAIL, ""));
        String user = fromName;
        getMetadataManager().addMetadata(request,
					 entry,
					 new Metadata(
						      getRepository().getGUID(), entry.getId(),
						      AdminMetadataHandler.TYPE_ANONYMOUS_UPLOAD, false, user,
						      request.getIp(), ((oldType != null)
									? oldType
									: ""), fromEmail, ""));
        User parentUser = parentEntry.getUser();
        logInfo("upload: setting user to: " + parentUser.getName()
                + " from parent folder:" + parentEntry);
        entry.setUser(parentUser);

        if (getMailManager().isEmailEnabled()) {
            StringBuilder contents =
                new StringBuilder(
				  "A new entry has been uploaded to the RAMADDA server under the folder: ");
            String url1 = HU.url(getFullEntryShowUrl(request),
				 ARG_ENTRYID, parentEntry.getId());

            contents.append(HU.href(url1, parentEntry.getFullName()));
            contents.append("<p>\n\n");
            String url = HU.url(getFullEntryShowUrl(request),
				ARG_ENTRYID, entry.getId());
            contents.append("Edit to confirm: ");
            contents.append(HU.href(url, entry.getLabel()));
            boolean sentNotification = false;
            List<Metadata> metadataList =
                getMetadataManager().findMetadata(request, parentEntry,
						  new String[]{ContentMetadataHandler.TYPE_CONTACT}, true);
            if (metadataList != null) {
                for (Metadata metadata : metadataList) {
                    sentNotification = true;
		    try {
			getRepository().getMailManager().sendEmail(
								   metadata.getAttr2(), "Uploaded Entry",
								   contents.toString(), true);
		    } catch(Exception exc) {
			getLogManager().logError("Sending upload email", exc);
		    }

                }

            }
            if ( !sentNotification) {
		try {
		    getRepository().getMailManager().sendEmail(
							       parentUser.getEmail(), "Uploaded Entry",
							       contents.toString(), true);
		} catch(Exception exc) {
		    getLogManager().logError("Sending upload email", exc);
		}
            }
        }
    }


    private Hashtable<String,Integer> uploadCounts = new Hashtable<String,Integer>();

    public Result processEntryUpload(Request request) throws Exception {
        Entry         group = findGroup(request);
        StringBuilder sb    = new StringBuilder();
        if ( !request.exists(ARG_CONTRIBUTION_FROMNAME)) {
	    getPageHandler().entrySectionOpen(request, group, sb,"Upload a File");
	    addAnonymousUploadForm(request, group,sb);
	    getPageHandler().entrySectionClose(request, group, sb);
        } else {
	    getPageHandler().entrySectionOpen(request, group, sb,"Upload a File");
	    String ip = request.getIp();
	    Integer cnt = uploadCounts.get(ip);
	    if(cnt==null) uploadCounts.put(ip,cnt=new Integer(0));
	    if(cnt>8) {
		sb.append(messageError("Too many requests"));
	    } else {
		uploadCounts.put(ip,new Integer(cnt+1));
		doProcessEntryChange(request, true, null);
		sb.append(messageNote("Thanks"));
	    }
	    getPageHandler().entrySectionClose(request, group, sb);
        }

        return makeEntryEditResult(request, group, "Upload", sb);
    }



    public void addAnonymousUploadForm(Request request, Entry group,Appendable sb) throws Exception {
	sb.append(request.uploadForm(getRepository().URL_ENTRY_UPLOAD,
				     HU.attr("name",
					     "entryform")));
	sb.append(HU.formTable());
	sb.append(HU.hidden(ARG_GROUP, group.getId()));
        TypeHandler typeHandler =
            getRepository().getTypeHandler(TypeHandler.TYPE_CONTRIBUTION);

	typeHandler.addToEntryForm(request, sb, group, null, new FormInfo(""));
	HU.formTableClose(sb);
	sb.append(HU.insetDiv(HU.submit("Upload"),10,45,0,0));
	sb.append(HU.formClose());
    }

    public static class Types extends NamedList<TypeHandler> {
	public  Types(String name) {
	    super(name);
	}
    }

    public static class SuperType extends NamedList<Types> {
	public  SuperType(String name) {
	    super(name);
	}
    }


    public List<SuperType> getCats(boolean anyOk) throws Exception {
	List<SuperType> superTypes = new ArrayList<SuperType>();
	Hashtable<String,SuperType> superMap = new Hashtable<String,SuperType>();
	Hashtable<String,Types> typesMap = new Hashtable<String,Types>();

        for (String superCat : PRELOAD_CATEGORIES) {
	    SuperType superType  = new SuperType(superCat);
	    superTypes.add(superType);
	    superMap.put(superCat, superType);
	}

        List<TypeHandler> typeHandlers = getRepository().getTypeHandlersForDisplay(anyOk);

        for (TypeHandler typeHandler : typeHandlers) {
            String superCat = typeHandler.getSuperCategory();
            if (superCat.equals("Basic") || superCat.equals("")) {
                superCat = "General";
            }
	    SuperType superType  = superMap.get(superCat);
	    if(superType == null) {
		superType  = new SuperType(superCat);
		superTypes.add(superType);
		superMap.put(superCat, superType);
		//		System.err.println("new super:" + superType);
	    }
	    String key = superCat+"-"+typeHandler.getCategory();
	    Types types = typesMap.get(key);
	    if(types == null) {
		types = new Types(typeHandler.getCategory());
		superType.add(types);
		typesMap.put(key,types);
		//		System.err.println("\tnew sub:" + types);
	    }
	    types.add(typeHandler);
	}
	return superTypes;
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
    public Result processEntryNew(Request request) throws Exception {

        Entry         group = findGroup(request);
        StringBuilder sb    = new StringBuilder();
        getPageHandler().entrySectionOpen(request, group, sb,
                                          "Choose entry type");

	sb.append("<center>");
	HU.script(sb,"HtmlUtils.initPageSearch('.type-list-item','.type-list-container','Find Type')");
	sb.append("</center>");
	for(EntryManager.SuperType superType:getEntryManager().getCats(false)) {
	    boolean didSuper = false;
	    for(EntryManager.Types types: superType.getList()) {
		boolean didSub = false;
		for(TypeHandler typeHandler: types.getList()) {
		    if(!typeHandler.canCreate(request))
			continue;
		    if(!didSuper) {
			didSuper = true;
			sb.append("<div class=type-group-container><div class='type-group-header'>" + superType.getName()+"</div><div class=type-group>");
		    }
		    if(!didSub) {
			didSub=true;
			sb.append("<div class=type-list-container><div class='type-list-header'>" + types.getName()+"</div><div class=type-list>");
		    }
		    String icon = typeHandler.getIconProperty(null);
		    String img;
		    if (icon == null) {
			icon = ICON_BLANK;
		    }
		    img = HU.img(typeHandler.getIconUrl(icon),"",  HU.attr(HU.ATTR_WIDTH,ICON_WIDTH));
		    String href = HU
			.href(request
			      .makeUrl(getRepository().URL_ENTRY_FORM, ARG_GROUP, group
				       .getId(), ARG_TYPE, typeHandler.getType()), img + HU.SPACE
			      + msg(typeHandler.getLabel()));
		    
		    String help = typeHandler.getHelp();
		    String title = typeHandler.getLabel();
		    String ttimg = HU.img(typeHandler.getIconUrl(icon),"",  HU.attr(HU.ATTR_WIDTH,"32px")).replace("\"","'");
		    if(stringDefined(help)) title+= " - " + help;
		    title = ttimg + HU.space(1) +title;
		    HU.div(sb,href,HU.attrs("class","type-list-item","title",title,
					    "data-category",typeHandler.getCategory()));
		}
		if(didSub) {
		    sb.append("</div></div>");
		}
	    }
	    if(didSuper) {
		sb.append("</div></div>");
	    }
	}
	HU.script(sb,"HtmlUtils.initTooltip('.type-list-item')");

        getPageHandler().entrySectionClose(request, group, sb);
        return makeEntryEditResult(request, group, "Create Entry", sb);

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
    public Result processEntryGetByFilename(Request request)
	throws Exception {
        if (request.getCheckingAuthMethod()) {
            return new Result(AuthorizationMethod.AUTH_HTTP);
        }

        String file = request.getString(ARG_FILESUFFIX, null);
        if (file == null) {
            file = IO.getFileTail(request.getRequestPath());
            request.put(ARG_FILESUFFIX, file);
        }

        List<Entry> entries =getEntriesFromDb(request,null,null);
        if (entries.size() == 0) {
            throw new IllegalArgumentException(
					       "Could not find entry with file");
        }

        return processEntryGet(request, entries.get(0));
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
    public Result processEntryShowPath(Request request) throws Exception {
        List<String> toks = Utils.split(request.getRequestPath(), "/",
					true, true);
        String id    = toks.get(toks.size() - 1);
        Entry  entry = getEntry(request, id);
        if (entry == null) {
            throw new RepositoryUtil.MissingEntryException(
							   "Could not find entry from id:" + id);
        }

        Result r = processEntryShow(request, entry);

        return addEntryHeader(request, entry, r);
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
    public Result processEntryLinks(Request request) throws Exception {
        Entry entry = getEntry(request);
        if (entry == null) {
            throw new IllegalArgumentException("Unable to find entry:"
					       + request);
        }
        StringBuilder sb = new StringBuilder();
        getPageHandler().entrySectionOpen(request, entry, sb,
                                          "Entry Actions");

	HU.open(sb,"div",HU.clazz("ramadda-entry-actions"));
        sb.append(getEntryActionsTable(request, entry, OutputType.TYPE_ALL));
        getPageHandler().entrySectionClose(request, entry, sb);
	HU.close(sb,"div");

        return addEntryHeader(request, entry,
                              new Result("Entry Actions", sb));
    }

    public Result processEntryMenu(Request request) throws Exception {
        Entry entry = getEntry(request);
        if (entry == null) {
            throw new IllegalArgumentException("Unable to find entry:"
					       + request);
        }
        StringBuilder sb = new StringBuilder();
        String links = getEntryActionsTable(request, entry,
					    OutputType.TYPE_MENU, null, false, null);

	Result result = new Result("",new StringBuilder(links));
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
    public Result processEntryGet(Request request) throws Exception {

        if (request.getCheckingAuthMethod()) {
            return new Result(AuthorizationMethod.AUTH_HTTP);
        }

        Entry entry = getEntryFromRequest(request, ARG_ENTRYID,
                                          getRepository().URL_ENTRY_GET,false);

        return processEntryGet(request, entry);
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
    public Result processEntryGet(Request request, Entry entry)
	throws Exception {
        if (entry == null) {
            throw new RepositoryUtil.MissingEntryException(
							   "Could not find entry");
        }

        if ( !getAccessManager().canAccessFile(request, entry)) {
            throw new AccessException("No access to file", request);
        }


        request.setCORSHeaderOnResponse();
        addSessionEntry(request, entry);

        if ( !entry.getResource().isUrl()) {
            if ( !getAccessManager().canDownload(request, entry)) {
                fatalError(request, "Cannot download file");
            }
        }

        String path = entry.getResource().getPath();
        String mimeType = getRepository().getMimeTypeFromSuffix(
								IO.getFileExtension(path));

	if(!stringDefined(mimeType) || mimeType.equals("unknown")) {
	    //If we can't determine  the mime type from the path then sometimes the typehandler
	    //will have one from the types.xml file
	    mimeType = entry.getTypeHandler().getMimeType();
	}


	logEntryActivity(request, entry,"download");
	
        boolean isImage = Utils.isImage(path);
	String imageWidth = request.getString(ARG_SERVERIMAGEWIDTH, request.getString(ARG_IMAGEWIDTH,null));
        if (isImage && imageWidth!=null) {
            int width = Integer.parseInt(imageWidth);
            File thumb = getStorageManager().getCacheFile("thumb_entry"
							  + IOUtil.cleanFileName(entry.getId()) + "_"
							  + width + IO.getFileExtension(path));
	    //	    System.err.println(thumb.exists() +" " +thumb);
            if ( !thumb.exists()) {
		long t1 = System.currentTimeMillis();
		//Use the new resizeImage method
		Image image = ImageUtils.resizeImage(new File(entry.getResource().getPath()),width,-1);
		//		Image image = ImageUtils.readImage(entry.getResource().getPath());
		//		image = ImageUtils.resize(image, width, -1);
		//		ImageUtils.waitOnImage(image);
		long t2 = System.currentTimeMillis();
                ImageUtils.writeImageToFile(image, thumb);
		long t3 = System.currentTimeMillis();
		//		Utils.printTimes("resize image",t1,t2,t3);
            }
            return new Result(BLANK,
                              getStorageManager().getFileInputStream(thumb),
                              mimeType);
        } else {
            File file   = getStorageManager().getEntryFile(entry);
            long length = file.length();
	    long totalLength = length;
            if (request.isHeadRequest()) {
                Result result = new Result("", new StringBuilder());
                result.addHttpHeader(HU.HTTP_CONTENT_LENGTH,
                                     "" + length);
                result.addHttpHeader("Connection", "close");
                result.setLastModified(new Date(file.lastModified()));

                return result;
            }

	    boolean inline = request.get("fileinline", ARG_INLINE_DFLT);
	    //	    System.err.println("setReturnFilename:" + inline +" " + request);
            //Get the original filename and set that on the result so the browser sees the file - not just "get"
            String fileName = getStorageManager().getFileTail(entry);
            request.setReturnFilename(fileName, inline);

            int response = Result.RESPONSE_OK;
            InputStream inputStream;

	    if (entry.getResource().isUrl()) {
		String url = entry.getResource().getPath();
		if ( !url.startsWith("http:") && !url.startsWith("https:")) {
		    throw new IllegalArgumentException("Can't get a non-http url:" +url);
		}
		URLConnection connection = new URL(url).openConnection();
		//		inputStream         = connection.getInputStream();
		//		System.err.println("C:" + IOUtil.readContents(inputStream));
		//		connection = new URL(url).openConnection();
		inputStream         = connection.getInputStream();
		Result result = new Result(BLANK, inputStream, mimeType);
		result.setResponseCode(response);
		return result;
	    } 
	    inputStream = getStorageManager().getFileInputStream(file);


            String range = (String) request.getHttpHeaderArgs().get("Range");
            long   byteStart = -1;
            long   byteEnd   = -1;
            if (Utils.stringDefined(range)) {
                //assume: bytes=start-end
                List<String> toks1 = Utils.splitUpTo(range, "=", 2);
                List<String> toks  = Utils.split(toks1.get(1), "-");
                byteStart = Long.decode(toks.get(0)).longValue();
                if ((toks.size() > 1) && Utils.stringDefined(toks.get(1))) {
                    byteEnd = Long.decode(toks.get(1)).longValue();
                }
            }

            if (byteStart > 0) {
                inputStream.skip(byteStart);
                response = Result.RESPONSE_PARTIAL;
                if (byteEnd > 0) {
                    //TODO: how to limit the length
                }
                length -= byteStart;
            }

	    if(byteEnd<0)  byteEnd = totalLength;
            Result result = new Result(BLANK, inputStream, mimeType);
            result.setResponseCode(response);
            result.addHttpHeader("Accept-Ranges", "bytes");
            result.addHttpHeader("Content-Range", "bytes " + byteStart +"-"+byteEnd+"/"+totalLength);	               result.addHttpHeader(HU.HTTP_CONTENT_LENGTH, "" + length);
            result.setLastModified(new Date(file.lastModified()));
            result.setCacheOk(httpCacheFile);
	    return result;
        }

    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param filename _more_
     *
     * @return _more_
     */
    public File getCacheFile(Entry entry, String filename) {
        File f = getStorageManager().getCacheFile(entry.getId() + "_"
						  + entry.getChangeDate() + "_" + filename, false);
        if (f.exists()) {
	    int  seconds = entry.getTypeHandler().getCacheTime();
            try {
                List<Metadata> mtdl = getMetadataManager().findMetadata(
									getRepository().getTmpRequest(),
									entry, new String[]{"cachetime"}, true);
                if ((mtdl != null) && (mtdl.size() > 0)) {
                    seconds = Integer.parseInt(mtdl.get(0).getAttr1());
		}
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }

	    if(seconds>=0) {
		Date now     = new Date();
		long ftime   = f.lastModified();
		long diff    = (now.getTime() - ftime) / 1000;
		if (diff > seconds) {
		    f.delete();
                }
	    }
	}

        return f;
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public File getFileForEntry(Entry entry) throws Exception {
        return entry.getTypeHandler().getFileForEntry(entry);
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
    public Result processGetEntries(Request request) throws Exception {
        List<Entry> entries    = new ArrayList();
        boolean     doAll      = request.defined("getall");
        boolean     doSelected = request.defined("getselected");
        String      arg        = (doAll
                                  ? ARG_ALLENTRY
                                  : ARG_SELENTRY);

        for (Object s : request.get(arg, new ArrayList<String>())) {
            String id    = s.toString();
            Entry  entry = getEntry(request, id);
            if (entry != null) {
                entries.add(entry);
            }
        }
        String ids = request.getIds((String) null);
        if (ids != null) {
            List<String> idList = Utils.split(ids, ",", true, true);
            for (String id : idList) {
                Entry entry = getEntry(request, id);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }

        entries = getAccessManager().filterEntries(request, entries);
        Entry group = null;
        for (Entry entry : entries) {
	    Entry parent = entry.getParentEntry();
	    if(parent==null) continue;
            if (group == null) {
                group = parent;
	    } else if ( !group.equals(parent)) {
                group = null;
                break;
            }
        }

        if (group != null) {
            request.put(ARG_ENTRYID, group.getId());
        }



	if(!request.defined(ARG_OUTPUT)) {
	    Entry parent = getEntryFromRequest(request, ARG_ENTRYID,
					       getRepository().URL_ENTRY_GET,true);	
	    return new Result("",getPageHandler().makeEntryPage(request, parent,"",
								getPageHandler().showDialogError("No action specified",false,HtmlUtils.backButton("Back"))));
	}

        OutputHandler outputHandler =
            getRepository().getOutputHandler(request);
        String dummyGroupName = request.getString(ARG_RETURNFILENAME,
						  "Results");
        Result result = outputHandler.outputGroup(request,
						  request.getOutput(),
						  getDummyGroup(dummyGroupName),
						  entries);

        return addEntryHeader(request, (group != null)
			      ? group
			      : request.getRootEntry(), result);
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
    public Result processEntryCopy(Request request) throws Exception {

        Entry parent = getEntryFromRequest(request, ARG_ENTRYID,
					   getRepository().URL_ENTRY_GET,true);

        String      fromIds = request.getString(ARG_FROM, "");
        List<Entry> entries = new ArrayList<Entry>();
        for (String id : Utils.split(fromIds, ",", true, true)) {
            Entry entry = getEntry(request, id, false);
            if (entry == null) {
                throw new RepositoryUtil.MissingEntryException(
							       "Could not find entry:" + id);
            }
            if (isTopEntry(entry)) {
                StringBuilder sb = new StringBuilder();
                sb.append(messageNote(msg("Cannot copy top-level folder")));
                return new Result("Entry Delete", sb);
            }
            entries.add(entry);
        }

        if (entries.size() == 0) {
            return new Result("", getPageHandler().makeEntryPage(request, parent,"Move/Copy/Link",
								 getPageHandler().showDialogError("No entries specified",false, HtmlUtils.backButton("Back"))));
        }

        if (request.exists(ARG_CANCEL)) {
            return new Result(
			      request.entryUrl(
					       getRepository().URL_ENTRY_SHOW, parent!=null?parent:entries.get(0)));
        }


        StringBuilder fromList = new StringBuilder();
        for (Entry fromEntry : entries) {
            fromList.append(getPageHandler().getBreadCrumbs(request, fromEntry));
            fromList.append(HU.br());
        }
        String fromDiv =
            HU.div(fromList.toString(),
		   HU.cssClass("entry-confirm-list"));

        String force  = request.getString(ARG_ACTION_FORCE, (String) null);
        String action = null;
        if (force != null) {
            action = force;
        } else {
            action = request.getString(ARG_ACTION, "move");
        }
        boolean isMove = Misc.equals(action, "move");
        boolean isCopy = Misc.equals(action, "copy");
        boolean isLink = Misc.equals(action, "link");

        String  label  = "Move/Copy/Link";
        if (force != null) {
            label = isCopy
		? "Copy"
		: isMove
		? "Move"
		: "Link";
        }

        if ( !(isCopy || isMove || isLink)) {
            isMove = true;
        }
        Entry  toEntry = null;

        String toId    = request.getString(ARG_TO + "_hidden", (String) null);
        if (toId == null) {
            toId = request.getString(ARG_TO, (String) null);
        }

        String toName = request.getString(ARG_TONAME, (String) null);
        if (toId != null) {
            toEntry = getEntry(request, toId);
        } else if (toName != null) {
            toEntry = findEntryFromName(request, null,toName);
        }
        if ( !request.exists(ARG_CONFIRM) || (toEntry == null)) {
            StringBuilder sb = new StringBuilder();
            if (entries.size() == 1) {
                getPageHandler().entrySectionOpen(request, parent!=null?parent:entries.get(0),
						  sb, null);
            }
            if (request.exists(ARG_CONFIRM) && (toEntry == null)) {
                sb.append(
			  getPageHandler().showDialogWarning(
							     "Please select a destination"));
            }
            request.formPostWithAuthToken(sb, getRepository().URL_ENTRY_COPY);
            if (force != null) {
                sb.append(HU.hidden(ARG_ACTION_FORCE, force));
            }
	    if(parent!=null)
		sb.append(HU.hidden(ARG_ENTRYID,parent.getId()));

	    if(toEntry!=null) {
		getPageHandler().entrySectionOpen(request, toEntry,sb,msg(label),false);
	    } else {
		getPageHandler().sectionOpen(request, sb,msg(label),false);
	    }
	    HU.div(sb, msg("The entries"), HU.cssClass("entry-confirm-header"));
            sb.append(fromDiv);

	    StringBuilder left  = new StringBuilder();
	    StringBuilder right  = new StringBuilder();
	    StringBuilder middle = new StringBuilder();
	    StringBuilder bottom  = new StringBuilder();
            if (force == null) {
                left.append(
			    HU.div(
				   msg("What do you want to do?"),
				   HU.cssClass("entry-confirm-header")));
		String radiosId = HU.getUniqueId("buttons_");
		String extraId = HU.getUniqueId("buttons_");		
                left.append(
			    HU.open(
				    HU.TAG_DIV,
				    HU.id(radiosId) + HU.cssClass("entry-confirm-list")));
                left.append(HU.labeledRadio(ARG_ACTION, "move", isMove,
					    msg("Move")));
                left.append("&nbsp;&nbsp;");
                left.append(HU.labeledRadio(ARG_ACTION, "copy", isCopy,
					    msg("Copy")));
                left.append("&nbsp;&nbsp;");
                left.append(HU.labeledRadio(ARG_ACTION, "link", isLink,
					    msg("Link")));
                left.append(HU.close(HU.TAG_DIV));
		bottom.append("<div style='margin-left:20px;margin-bottom:20px;display:" + (isCopy?"block":"none")+";' id='" + extraId+"'>");
		bottom.append(HU.formTable());
		bottom.append(HU.formEntry(msgLabel("Size Limit"),
					   HU.input(ARG_COPY_SIZE_LIMIT,
						    request.getString(ARG_COPY_SIZE_LIMIT, ""), HU.SIZE_5)+" (MB)"));
		bottom.append(HU.formEntry("",	
					   HU.labeledCheckbox(ARG_COPY_DEEP,"true",
							      request.get(ARG_COPY_DEEP, false), "Make deep copy (for synthetic entries)")));
		bottom.append(HU.formEntryTop(msgLabel("Excludes"), 
					      HU.textArea(ARG_EXCLUDES,request.getString(ARG_EXCLUDES),3,20) +" Patterns, one per line"));

		bottom.append(HU.formEntry("",	
					   HU.labeledCheckbox(ARG_COPY_DO_METADATA,
							      "true",
							      request.get(ARG_COPY_DO_METADATA, false), "Extract metadata")));



		bottom.append(HU.formTableClose());
		bottom.append("</div>");
		bottom.append(HU.script("HtmlUtils.initRadioToggle(" +HU.comma(HU.squote("#"+radiosId),"{'copy':" +HU.squote("#"+extraId))+"});\n"));
            }


	    HU.div(middle, HU.div("To",HU.cssClass("entry-confirm-header")) +HU.faIcon(ICON_RIGHTARROW, HU.style("font-size:24pt;")),HU.style("margin-left:32px;margin-right:32px;text-align:center;"));

	    HU.div(right,
		   toEntry==null?"Select a destination":"Target entry:",
		   HU.cssClass("entry-confirm-header"));
            right.append(
			 HU.open(
				 HU.TAG_DIV,
				 HU.cssClass("entry-confirm-list")));
            right.append(HU.hidden(ARG_FROM, fromIds));

            if (toEntry != null) {
                right.append(toEntry.getTypeHandler().getEntryName(toEntry));
                right.append(HU.hidden(ARG_TO, toEntry.getId()));
            } else {
                Entry theParent = entries.get(0).getParentEntry();
		right.append(OutputHandler.makeEntrySelect(request, ARG_TO, true,"",theParent));
            }
            right.append(HU.close(HU.TAG_DIV));

	    HU.hrow(sb, left.toString(), middle.toString(), right.toString());
	    sb.append(bottom);
            sb.append(
		      HU.div(
			     msg("Are you sure?"),
			     HU.cssClass("entry-confirm-header")));
            sb.append(
		      HU.open(
			      HU.TAG_DIV,
			      HU.cssClass("entry-confirm-list")));
            sb.append(HU.buttons(HU.submit("Yes, do it",
					   ARG_CONFIRM), HU.submit(LABEL_CANCEL, ARG_CANCEL)));

            sb.append(HU.close(HU.TAG_DIV));

            sb.append(HU.formClose());
            sb.append(HU.sectionClose());

            if (entries.size() == 1) {
                getPageHandler().entrySectionClose(request, parent!=null?parent:entries.get(0),
						   sb);
            }
            return addEntryHeader(request, parent!=null?parent:entries.get(0),
                                  new Result("Entry Move/Copy", sb));
        }




        if (toEntry == null) {
            throw new RepositoryUtil.MissingEntryException(
							   "Could not find entry: " + ((toId == null)
										       ? toName
										       : toId));
        }
        boolean isGroup = toEntry.isGroup();


        if (isLink) {
            if (entries.size() == 1) {
                return new Result(
				  request.makeUrl(
						  getRepository().URL_ASSOCIATION_ADD, ARG_FROM,
						  entries.get(0).getId(), ARG_TO, toEntry.getId()));
            }
        }


        if (isMove) {
            if ( !isGroup) {
                throw new IllegalArgumentException(
						   "Can only copy/move to a folder");
            }

            for (Entry fromEntry : entries) {
                if ( !getAccessManager().canDoEdit(request, fromEntry)) {
                    throw new AccessException("Cannot move:"
					      + fromEntry.getLabel(), request);
                }
            }
        } else if (isCopy) {
            if ( !isGroup) {
                throw new IllegalArgumentException(
						   "Can only copy/move to a folder");
            }
        }


        if ( !getAccessManager().canDoNew(request, toEntry)) {
            throw new AccessException("Cannot copy/move to:"
                                      + toEntry.getLabel(), request);
        }


        for (Entry fromEntry : entries) {
            if ( !okToMove(fromEntry, toEntry)) {
                StringBuilder sb = new StringBuilder();
                sb.append(
			  getPageHandler().showDialogError(
							   msg("Cannot move a folder to its descendent")));

                return addEntryHeader(request, fromEntry, new Result("", sb));
            }
        }


        getAuthManager().ensureAuthToken(request);
	addSessionEntry(request, toEntry);
        if (isMove) {
            return processEntryMove(request, toEntry, entries);
        } else if (isCopy) {
            return processEntryCopy(request, toEntry, null, entries);
        } else if (isLink) {
            if (entries.size() == 1) {
                return new Result(
				  request.makeUrl(
						  getRepository().URL_ASSOCIATION_ADD, ARG_FROM,
						  entries.get(0).getId(), ARG_TO, toEntry.getId()));
            }
        }


        Result result = new Result(label, new StringBuilder());

        return addEntryHeader(request, toEntry, result);

    }




    /**
     * _more_
     *
     * @param request _more_
     * @param toGroup _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntryCopy(final Request request,
                                   final Entry toGroup,
				   final String pathTemplate,
                                   final List<Entry> entries)
	throws Exception {

        final String link =
            HU.href(request.entryUrl(getRepository().URL_ENTRY_SHOW,
				     toGroup), "Continue");
        ActionManager.Action action = new ActionManager.Action() {
		public void run(Object actionId) throws Exception {
		    processEntryCopyAsynch(request, toGroup, pathTemplate, entries, actionId,
					   link);
		}
	    };


        return getActionManager().doAction(request, action,
                                           "Copying entries", link, toGroup);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param toGroup _more_
     * @param entries _more_
     * @param actionId _more_
     * @param link _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public List<Entry> processEntryCopyAsynch(final Request request,
					      final Entry toGroup, final String pathTemplate,
					      final List<Entry> entries, Object actionId,
					      String link)
	throws Exception {
	boolean deepCopy  = request.get(ARG_COPY_DEEP,false);
	boolean doMetadata= request.get(ARG_COPY_DO_METADATA,false);
	EntryUtil.Excluder excluder = new EntryUtil.Excluder(Utils.split(request.getString(ARG_EXCLUDES,""),"\n",true,true),
							     (int)(request.get(ARG_COPY_SIZE_LIMIT,(double)-1.0)*1000*1000));
        StringBuilder sb         = new StringBuilder();
        List<Entry>   newEntries = new ArrayList<Entry>();
        try {
            Connection connection = getDatabaseManager().getConnection();
            connection.setAutoCommit(false);
            List<Descendent> ids = getDescendents(request, entries, connection,
						  excluder,
						  true, !deepCopy, actionId);
            getDatabaseManager().closeConnection(connection);
            Hashtable<String, Entry> oldIdToNewEntry = new Hashtable<String,
		Entry>();
            for (int i = 0; i < ids.size(); i++) {
                if ( !getActionManager().getActionOk(actionId)) {
                    return null;
                }
                Descendent descendent       = ids.get(i);
                String      id             = descendent.id;
                Entry       oldEntry       = getEntry(request, id);
		if(!excluder.isEntryOk(oldEntry)) {
		    System.err.println("Is excluded:" + oldEntry);
		    continue;
		}
		Entry newEntry =
		    copyEntry(request, toGroup, pathTemplate,
			      oldIdToNewEntry, newEntries, oldEntry,doMetadata);
            }

            int           count = 0;
            StringBuilder links = new StringBuilder();
            for (Entry newEntry : newEntries) {
                if ( !getActionManager().getActionOk(actionId)) {
                    return null;
                }
                //Do this instead of addNewEntry so the doFinalEntryInit does *not* get called
                List<Entry> tmp = new ArrayList<Entry>();
                tmp.add(newEntry);
                insertEntries(request, tmp, true, false);
                count++;
                getActionManager().setActionMessage(actionId,
						    "Copied " + count + "/" + newEntries.size()
						    + " entries");
                links.append(
			     HU.href(
				     request.entryUrl(
						      getRepository().URL_ENTRY_SHOW,
						      newEntry),
				     getPageHandler().getEntryIconImage(request, newEntry) +" "+
				     newEntry.getName()));
                links.append("<br>");
            }
            if (newEntries.size() > 0) {
                link = links.toString();
            }
	    parentageChanged(toGroup);
            getActionManager().setContinueHtml(actionId,
					       count + " entries copied" + HU.br() + link);
        } catch (Exception exc) {
            if (actionId == null) {
                throw exc;
            }
            getActionManager().setContinueHtml(actionId,
					       "Error copying entries:" + exc);
            getLogManager().logError("Copying entries", exc);
            return null;
        }
        return newEntries;
    }

    private Entry copyEntry(Request request,
			    Entry parent, String pathTemplate,
			    Hashtable<String, Entry> oldIdToNewEntry,
			    List<Entry> newEntries,
			    Entry oldEntry,boolean doMetadata) throws Exception {
	String      newId          = getRepository().getGUID();
	TypeHandler oldTypeHandler =oldEntry.getMasterTypeHandler();
	TypeHandler newTypeHandler =   oldTypeHandler.getTypeHandlerForCopy(oldEntry);
	Entry newEntry = newTypeHandler.createEntry(newId);
	oldIdToNewEntry.put(oldEntry.getId(), newEntry);
	//See if this new entry is somewhere down in the tree
	Entry newParent =
	    oldIdToNewEntry.get(oldEntry.getParentEntryId());
	if (newParent == null) {
	    newParent = parent;
	}
	Resource newResource =
	    oldTypeHandler.getResourceForCopy(request, oldEntry,
					      newEntry);
	initEntry(newEntry, oldEntry.getName(),
		  oldEntry.getDescription(),
		  newParent, request.getUser(),
		  newResource, oldEntry.getCategory(),
		  oldEntry.getEntryOrder(),
		  oldEntry.getCreateDate(),
		  new Date().getTime(),
		  oldEntry.getStartDate(),
		  oldEntry.getEndDate(),
		  oldEntry.getValues());

	newEntry.setParentEntry(getPathEntry(request,newParent,newEntry,pathTemplate));
	parentageChanged(newEntry);
	newEntry.setLocation(oldEntry);
	newTypeHandler.initializeCopiedEntry(newEntry, oldEntry);

	List<Metadata> newMetadata = new ArrayList<Metadata>();
	for (Metadata oldMetadata :
		 getMetadataManager().getMetadata(request,oldEntry)) {
	    newMetadata.add(
			    getMetadataManager().copyMetadata(
							      oldEntry, newEntry, oldMetadata));
	}
	newEntry.setMetadata(newMetadata);
	if(doMetadata) {
	    List<Entry> entries = new ArrayList<Entry>();
	    entries.add(newEntry);
	    addInitialMetadata(request, entries, false,false);
	}


	newEntries.add(newEntry);
	return newEntry;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param toGroup _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result processEntryMove(Request request, Entry toGroup,
                                    List<Entry> entries)
	throws Exception {
	parentageChanged(entries);
        Connection connection = getDatabaseManager().getConnection();
        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();
        try {
            for (Entry fromEntry : entries) {
                fromEntry.setParentEntry(toGroup);
                String oldId = fromEntry.getId();
                String newId = oldId;
                String sql =
                    "UPDATE  " + Tables.ENTRIES.NAME + " SET "
                    + SqlUtil.unDot(Tables.ENTRIES.COL_PARENT_GROUP_ID)
                    + " = " + SqlUtil.quote(fromEntry.getParentEntryId())
                    + " WHERE "
                    + SqlUtil.eq(Tables.ENTRIES.COL_ID,
                                 SqlUtil.quote(fromEntry.getId()));
                statement.execute(sql);
                connection.commit();
            }
            getDatabaseManager().closeStatement(statement);
            connection.setAutoCommit(true);

	    getRepository().checkMovedEntries(entries);
	    parentageChanged(entries);

            return new Result(request.makeUrl(getRepository().URL_ENTRY_SHOW,
					      ARG_ENTRYID, entries.get(0).getId()));
        } finally {
            getDatabaseManager().closeConnection(connection);
        }

    }



    /**
     * _more_
     *
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void setEntryBounds(Entry entry) throws Exception {
        Connection connection = getDatabaseManager().getConnection();
        try {
            Statement statement = connection.createStatement();
            String sql =
                "UPDATE  " + Tables.ENTRIES.NAME + " SET "
                + columnSet(Tables.ENTRIES.COL_NORTH, entry.getNorth()) + ","
                + columnSet(Tables.ENTRIES.COL_SOUTH, entry.getSouth()) + ","
                + columnSet(Tables.ENTRIES.COL_EAST, entry.getEast()) + ","
                + columnSet(Tables.ENTRIES.COL_WEST, entry.getWest()) + ","
                + columnSet(
			    Tables.ENTRIES.COL_ALTITUDEBOTTOM,
			    entry.getAltitudeBottom()) + ","
		+ columnSet(
                            Tables.ENTRIES.COL_ALTITUDETOP,
                            entry.getAltitudeTop()) + " WHERE "
		+ SqlUtil.eq(
			     Tables.ENTRIES.COL_ID,
			     SqlUtil.quote(entry.getId()));
            statement.execute(sql);
            getDatabaseManager().closeStatement(statement);
        } finally {
            getDatabaseManager().closeConnection(connection);
        }
    }


    /**
     * _more_
     *
     * @param col _more_
     * @param value _more_
     *
     * @return _more_
     */
    private String columnSet(String col, double value) {
        return SqlUtil.unDot(col) + " = " + value;
    }


    /**
     * _more_
     *
     * @param parent _more_
     * @param children _more_
     */
    public void setBoundsOnEntry(final Entry parent, List<Entry> children) {
        try {
            Rectangle2D.Double rect = getEntryUtil().getBounds(children);
            if ((rect != null) && !rect.equals(parent.getBounds())) {
                parent.setBounds(rect);
                setEntryBounds(parent);
            }
        } catch (Exception exc) {
            logError("Updating parent's bounds", exc);
        }
    }

    /**
     * _more_
     *
     * @param fromEntry _more_
     * @param toEntry _more_
     *
     * @return _more_
     */
    protected boolean okToMove(Entry fromEntry, Entry toEntry) {
        if ( !toEntry.isGroup()) {
            return false;
        }

        if (toEntry.getId().equals(fromEntry.getId())) {
            return false;
        }
        if (toEntry.getParentEntry() == null) {
            return true;
        }

        return okToMove(fromEntry, toEntry.getParentEntry());
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
    public Result processEntryXmlCreate(Request request) throws Exception {
        try {
	    //true implies just check the session id
            getAuthManager().ensureAuthToken(request,true);
            return processEntryXmlCreateInner(request);
        } catch (Exception exc) {
            if (request.getString(ARG_RESPONSE, "").equals(RESPONSE_XML)) {
                exc.printStackTrace();

                return new Result(XmlUtil.tag(TAG_RESPONSE,
					      XmlUtil.attr(ATTR_CODE, CODE_ERROR),
					      "" + exc.getMessage()), MIME_XML);
            }

            throw exc;
        }
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
    public Result processEntryPublish(Request request) throws Exception {
        try {
            //TODO: check for auth token
            //request.ensureAuthToken();
            return processEntryPublishInner(request);
        } catch (Exception exc) {
            if (request.getString(ARG_RESPONSE, "").equals(RESPONSE_XML)) {
                exc.printStackTrace();

                return new Result(XmlUtil.tag(TAG_RESPONSE,
					      XmlUtil.attr(ATTR_CODE, CODE_ERROR),
					      "" + exc.getMessage()), MIME_XML);
            }

            throw exc;
        }
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
    private Result processEntryPublishInner(Request request)
	throws Exception {

        StringBuilder sb      = new StringBuilder();
        String        fromIds = request.getString(ARG_FROM, (String) null);
        if (fromIds == null) {
            fromIds = request.getString("entries", "");
        }
        List<Entry> entries = new ArrayList<Entry>();
        for (String id : Utils.split(fromIds, ",", true, true)) {
            Entry entry = getEntry(request, id, false);
            if (entry == null) {
                throw new RepositoryUtil.MissingEntryException(
							       "Could not find entry:" + id);
            }
            if ( !isSynthEntry(entry.getId())) {
                continue;
            }
            entries.add(entry);
        }

        String encoded = request.getString(ARG_DESCRIPTION + "_encoded",
                                           null);
        if (encoded != null) {
            encoded = new String(Utils.decodeBase64(encoded));
        }

        String extraDesc = request.getString(ARG_DESCRIPTION + "_extra",
                                             (String) null);

        String textFromUser = request.getString(ARG_DESCRIPTION,
						(encoded == null)
						? ""
						: encoded);

        String desc = textFromUser;

        if (extraDesc != null) {
            desc = extraDesc;
            if (textFromUser.indexOf("${extra}") >= 0) {
                textFromUser = textFromUser.replace("${extra}",
						    (extraDesc != null)
						    ? extraDesc
						    : "");
            } else {
                if (textFromUser.startsWith(WIKI_PREFIX)) {
                    textFromUser = Utils.concatString(WIKI_PREFIX, extraDesc,
						      textFromUser.substring(WIKI_PREFIX.length()));
                } else {
                    textFromUser = Utils.concatString(extraDesc,
						      textFromUser);
                }
            }
        } else {
            textFromUser = textFromUser.replace("${extra}", "");
        }

        if (request.exists(ARG_CANCEL)) {
            return new Result("", new StringBuilder("OK"));
        }

        Entry   parent  = getParentEntry(request);

        String  type    = request.getString(ARG_TYPE, "wikipage");
        boolean isWiki  = type.equals("wikipage");
        boolean isAlbum = type.equals("media_photoalbum");
        boolean isBlog  = type.equals("blogentry");


        String  name    = isWiki
	    ? "Wiki Page"
	    : isAlbum
	    ? "Photo Album"
	    : "Blog Post";
        name = request.getString(ARG_NAME, name);



        if (request.exists(ARG_CONFIRM)) {
            getAuthManager().ensureAuthToken(request);
            if (parent == null) {
                sb.append(
			  getPageHandler().showDialogNote(
							  msg("Please select a destination entry")));
            } else {
                TypeHandler typeHandler =
                    getRepository().getTypeHandler(type);
                Entry entry =
                    typeHandler.createEntry(getRepository().getGUID());
                Date     date   = new Date();
                Object[] values =
                    typeHandler.makeEntryValues(new Hashtable());
                if (isWiki) {
                    values[0] = textFromUser;
                    desc      = "";
                } else if (isAlbum) {
                    //ignore textFromUser
                    if (extraDesc != null) {
                        desc = extraDesc;
                    }
                } else {
                    //blog
                    desc = textFromUser;
                }

                initEntry(entry, name, desc, parent, request.getUser(),
			  new Resource(), "", 999,date.getTime(),
			  date.getTime(), date.getTime(),
			  date.getTime(), values);

                addNewEntry(request, entry);
                //TODO - check for extra desc here
                if (entries.size() > 0) {
                    List<Entry> newEntries = processEntryCopyAsynch(request,
								    entry, null, entries, null, null);
                    for (int i = 0; i < newEntries.size(); i++) {
                        String oldId = entries.get(i).getId();
                        String newId = newEntries.get(i).getId();
                        textFromUser = textFromUser.replace(oldId, newId);
                    }
                    if (isWiki) {
                        values[0] = textFromUser;
                    } else {
                        entry.setDescription(textFromUser);
                    }
                }
                updateEntry(request, entry);

                return new Result(
				  request.entryUrl(getRepository().URL_ENTRY_SHOW, entry));
	    }
	}



        Entry dummy = getDummyGroup(name);

        request.uploadFormWithAuthToken(sb,
                                        getRepository().URL_ENTRY_PUBLISH,
                                        "");

        sb.append(HU.hidden(ARG_FROM, fromIds));
        sb.append(HU.hidden(ARG_TYPE, type));

        String label = isWiki
	    ? "Publish Wiki Page"
	    : "Publish Blog Post";
        sb.append(HU.sectionOpen(msg(label)));

        sb.append(HU.formTable());
        sb.append(
		  HU.formEntry(
			       "",
			       HU.buttons(
					  HU.submit("Publish", ARG_CONFIRM),
					  HU.submit(LABEL_CANCEL, ARG_CANCEL))));

        sb.append(HU.formEntry(msgLabel("Name"),
			       HU.input(ARG_NAME,
					request.getString(ARG_NAME, ""),
					HU.SIZE_70)));

        String textWidget = HU.textArea(ARG_DESCRIPTION + "_extra",
					"", 5, 120, "");
        sb.append(HU.formEntryTop(msgLabel("Description"),
				  textWidget));

        sb.append(HU.comment("The description"));
        String encodedDesc = Utils.encodeBase64(desc);
        sb.append(HU.hidden(ARG_DESCRIPTION + "_encoded",
			    encodedDesc));

        String buttons =
            getRepository().getWikiManager().makeWikiEditBar(request, dummy,
							     ARG_DESCRIPTION) + HU.br();





        String select = getRepository().getHtmlOutputHandler().getSelect(
									 request, ARG_TO,
									 HU.highlightable(
											  HU.image("fas fa-bars")
											  + HU.SPACE
											  + msg("Select")
											  + HU.SPACE), true, "", null, false, false);




        sb.append(HU.hidden(ARG_TO + "_hidden", "",
			    HU.id(ARG_TO + "_hidden")));


        sb.append(HU.formEntry(msgLabel("Destination"),
			       select + HU.space(1)
			       + HU.disabledInput(ARG_TO, "",
						  HU.SIZE_60
						  + HU.id(ARG_TO))));


        sb.append(HU.formTableClose());
        sb.append(HU.formClose());

        sb.append(HU.sectionClose());
        sb.append(HU.hr());
        sb.append(HU.p());

        String wiki = getWikiManager().wikifyEntry(request, dummy, desc);

        sb.append(wiki);




        return new Result("", sb);

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
    public Result processEntryExport(Request request) throws Exception {
        Entry entry = getEntry(request);
        if (entry == null) {
            throw new IllegalArgumentException("Unable to find entry:"
					       + request);
        }

        if ( !getAccessManager().canDoExport(request, entry)) {
            throw new IllegalArgumentException("Cannot export entry");
        }

        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);

        return getRepository().getZipOutputHandler().toZip(request, entry.getName(),
							   entries, true, true,false);
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
    public Result processEntryImport(Request request) throws Exception {
        Entry group = findGroup(request);
        if ( !getAccessManager().canDoNew(request, group)) {
            throw new AccessException("You cannot import", request);
        }

        StringBuilder        sb          = new StringBuilder();
        StringBuilder        extraForm   = new StringBuilder();
        List<TwoFacedObject> importTypes = new ArrayList<TwoFacedObject>();
        for (ImportHandler importHandler :
		 getRepository().getImportHandlers()) {
            importHandler.addImportTypes(importTypes, extraForm);
        }
        getPageHandler().entrySectionOpen(request, group, sb,
                                          msg("Import Entries"), true);

        request.uploadFormWithAuthToken(sb,
                                        getRepository().URL_ENTRY_XMLCREATE,
                                        makeFormSubmitDialog(sb,
							     msg("Importing "
								 + LABEL_ENTRIES)));
	String inputId = getRepository().getGUID();

        sb.append(HU.hidden(ARG_GROUP, group.getId()));
        sb.append(HU.formTable());
        sb.append(HU.formEntry(msgLabel("File"),
			       HU.fileInput(ARG_FILE,
					    HU.id(inputId) + HU.SIZE_70)));
	
	HtmlUtils.script(sb, "Ramadda.initFormUpload(" + HU.comma(HU.squote(inputId),HU.squote(""))+");");
        sb.append(HU.formEntry(msgLabel("Or URL"),
			       HU.input(ARG_URL, "",
					HU.SIZE_70)));
	

	if (request.isAdmin()) {
	    sb.append(HU.formEntry(msgLabel("Or File on Server"),
				   HU.input(ARG_SERVERFILE, "",
					    HU.SIZE_70)));
	}
	

        if (importTypes.size() > 0) {
            importTypes.add(
			    0, new TwoFacedObject("RAMADDA will figure it out", ""));
            sb.append(HU.formEntry(msgLabel("Type"),
				   HU.select(ARG_IMPORT_TYPE,
					     importTypes)));
        }

        sb.append(HU.formEntry(msgLabel("Extra"),
			       HU.input("extra",
					request.getString("extra", ""),
					HU.SIZE_70)));

        sb.append(HU.formEntry("", HU.submit("Submit")));


        sb.append(extraForm);

        HU.formTableClose(sb);
        sb.append(HU.formClose());

        getPageHandler().entrySectionClose(request, group, sb);

        return makeEntryEditResult(request, group, "Entry Import", sb);
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
    private Result processEntryXmlCreateInner(Request request)
	throws Exception {

        Entry parent = null;
        if (request.exists(ARG_GROUP)) {
            parent = getEntryFromArg(request, ARG_GROUP);
            if (parent == null) {
                parent = findEntryFromName(request,null, request.getString(ARG_GROUP, ""));
            }

            if (parent == null) {
                throw new IllegalArgumentException(
						   "Could not find parent entry:"
						   + request.getString(ARG_GROUP));
            } else if ( !parent.isGroup()) {
                throw new IllegalArgumentException("Entry is not a group:"
						   + parent);
            }
        }


        //Fetch the URL
        String url = request.getString(ARG_URL, null);
        //Check the import handlers
        for (ImportHandler importHandler :
		 getRepository().getImportHandlers()) {

            Result result = importHandler.handleUrlRequest(request,
							   getRepository(), url, parent);
            if (result != null) {
                return result;
            }
        }



	boolean deleteFile = true;

        String file = null;
        if (Utils.stringDefined(url)) {
            file = getStorageManager().fetchUrl(url).toString();
	    deleteFile = false;
        } else 	if (request.isAdmin() && request.defined(ARG_SERVERFILE)) {
            request.ensureAdmin();
	    file = request.getString(ARG_SERVERFILE,"");
	    File _file = new File(file);
	    deleteFile = false;
	    if(_file.exists() && _file.isDirectory()) {
		Result lastResult = null;
		for(File _child:_file.listFiles()) {
		    String child = _child.toString();
		    if(child.toLowerCase().endsWith(".zip") || child.toLowerCase().endsWith(".xml")) {
			System.err.println("Processing directory import:" + child);
			lastResult =  processEntryImportInner(request, parent, child,deleteFile);
		    }
		}
		if(lastResult==null) {
		    return new Result("", new StringBuilder(
							    getPageHandler().showDialogError("No .zip or .xml files found in directory:" + file)));
		}
		return lastResult;
	    }
	}

        if (file == null) {
            file = request.getUploadedFile(ARG_FILE);
        }

        if (file == null) {
            throw new IllegalArgumentException("No file argument given");
        }


	Result result = handleImport(request, file, parent);
	if(result!=null) return result;
	return processEntryImportInner(request, parent, file,deleteFile);
    }

    private Result handleImport(Request request, String file, Entry parent) throws Exception{
        //Check the import handlers
        for (ImportHandler importHandler :
		 getRepository().getImportHandlers()) {
            Result result = importHandler.handleRequest(request,
							getRepository(), file, parent);
            if (result != null) {
                return result;
            }
        }
	return  null;
    }


    private  Result processEntryImportInner(Request request, Entry parent, String file, boolean deleteFile) throws Exception {	
        String entriesXml = null;
        Hashtable<String, File> origFileToStorage = new Hashtable<String,
	    File>();

        InputStream fis = getStorageManager().getFileInputStream(file);
        try {
            if (file.endsWith(".zip")) {
                ZipInputStream zin =
                    getStorageManager().makeZipInputStream(fis);
                ZipEntry ze;
                while ((ze = zin.getNextEntry()) != null) {
                    String entryName = ze.getName();
                    if (entryName.endsWith("entries.xml")) {
                        InputStream entriesStream = zin;
                        //Check the import handlers
                        for (ImportHandler importHandler :
				 getRepository().getImportHandlers()) {
                            InputStream newStream =
                                importHandler.getStream(request, parent,
							entryName, entriesStream);
                            if (newStream != null) {
                                entriesStream = newStream;

                                break;
                            }
                        }

                        entriesXml =
                            new String(IOUtil.readBytes(entriesStream, null,
							false));
                    } else {
                        String name = IO.getFileTail(ze.getName());
                        File f = getStorageManager().getTmpFile(name);
                        OutputStream fos =
                            getStorageManager().getFileOutputStream(f);
                        IOUtil.writeTo(zin, fos);
                        fos.close();
                        //Add both the zip path and the filename in case we have dir/filename.txt in the zip
                        origFileToStorage.put(ze.getName(), f);
                        origFileToStorage.put(name, f);
                    }
                }
                if (entriesXml == null) {
                    throw new IllegalArgumentException(
						       "No entries.xml file provided");
                }
            }
            if (entriesXml == null) {
                InputStream entriesStream = fis;
                //Check the import handlers
                for (ImportHandler importHandler :
			 getRepository().getImportHandlers()) {
                    InputStream newStream = importHandler.getStream(request,
								    parent, file, entriesStream);
                    if ((newStream != null) && (newStream != entriesStream)) {
                        entriesStream = newStream;

                        break;
                    }
                }
                entriesXml = IOUtil.readInputStream(entriesStream);
            }
        } finally {
            IO.close(fis);
	    if(deleteFile) {
		getStorageManager().deleteFile(new File(file));
	    }
        }


        Element root = XmlUtil.getRoot(entriesXml);
        for (ImportHandler importHandler :
		 getRepository().getImportHandlers()) {
            Element newRoot = importHandler.getDOM(request, root);
            if ((newRoot != null) && (newRoot != root)) {
                root = newRoot;

                break;
            }
        }


	StringBuilder msg = new StringBuilder();
        List<Entry> newEntries = processEntryXml(request, root, parent,
						 origFileToStorage,msg);


        for (Entry entry : newEntries) {
            entry.getTypeHandler().doFinalEntryInitialization(request, entry,
							      true);
        }
        if (request.getString(ARG_RESPONSE, "").equals(RESPONSE_XML)) {
            //TODO: Return a list of the newly created entries
            Element resultRoot = XmlUtil.create(XmlUtil.makeDocument(),
						TAG_RESPONSE, null,
						new String[] { ATTR_CODE,
							       CODE_OK });

            for (Entry entry : newEntries) {
                XmlUtil.create(resultRoot.getOwnerDocument(), TAG_ENTRY,
                               resultRoot, new String[] { ATTR_ID,
							  entry.getId() });


            }
            String xml = XmlUtil.toString(resultRoot);
            return new Result(xml, MIME_XML);
        }

        StringBuilder sb = new StringBuilder();
        if (parent != null) {
            getPageHandler().entrySectionOpen(request, parent, sb,
					      "Imported Entries", true);
        }


        sb.append("<ul>");
	sb.append(msg);

        for (Entry entry : newEntries) {
            sb.append("<li> ");
            sb.append(getPageHandler().getBreadCrumbs(request, entry,
						      parent));
        }
        sb.append("</ul>");
        if (parent != null) {
            getPageHandler().entrySectionClose(request, parent, sb);
        }


        if (parent != null) {
            return makeEntryEditResult(request, parent,
                                       "Imported " + LABEL_ENTRIES, sb);
        }

        return new Result("", sb);

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param root _more_
     * @param parent _more_
     * @param origFileToStorage _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> processEntryXml(Request request, Element root,
                                       Entry parent,
                                       Hashtable<String,File> origFileToStorage,
				       StringBuilder msg)
	throws Exception {
        Hashtable<String, Entry> entries = new Hashtable<String, Entry>();
        if (parent != null) {
            entries.put("", parent);
        }


        List<Entry>   newEntries       = new ArrayList<Entry>();
        List<Element> entryNodes       = new ArrayList<Element>();
        List<Element> associationNodes = new ArrayList<Element>();

        NodeList      children;
        if (root.getTagName().equals(TAG_ENTRY)) {
            children = new XmlNodeList();
            ((XmlNodeList) children).add(root);
        } else {
            children = XmlUtil.getElements(root);
        }



        for (int i = 0; i < children.getLength(); i++) {
            Element node = (Element) children.item(i);
            if (node.getTagName().equals(TAG_ENTRY)) {
                entryNodes.add(node);
            } else if (node.getTagName().equals(TAG_ASSOCIATION)) {
                associationNodes.add(node);
            } else {
                throw new IllegalArgumentException("Unknown tag:"
						   + node.getTagName());
            }
        }

        List<String[]> idList = new ArrayList<String[]>();
        for (Element node : entryNodes) {
            List<Entry> entryList = createEntryFromXml(request, node,
						       entries, origFileToStorage, true,
						       TEMPLATE.NO,
						       INTERNAL.YES,msg);

            newEntries.addAll(entryList);
            if (XmlUtil.hasAttribute(node, ATTR_ID)) {
                for (Entry entry : entryList) {
                    idList.add(new String[] {
			    XmlUtil.getAttribute(node, ATTR_ID, ""),
			    entry.getId() });
                }
            }

            if (XmlUtil.getAttribute(node, ATTR_ADDMETADATA, false)) {
                addInitialMetadata(request, entryList, true, false);
            } else if (XmlUtil.getAttribute(node, ATTR_ADDSHORTMETADATA,
                                            false)) {
                addInitialMetadata(request, entryList, true, true);
            }
        }


        for (Element node : associationNodes) {
            String id =
                getAssociationManager().processAssociationXml(request, node,
							      entries, origFileToStorage);
        }

	for (Enumeration keys = entries.keys();
	     keys.hasMoreElements(); ) {
	    Object key = keys.nextElement();
	    Entry value = entries.get(key);
	    idList.add(new String[] {
		    key.toString(),
		    value.getId() });

	}

	//Sort the idList so the longest strings are first
        Comparator comp = new Comparator() {
		public int compare(Object o1, Object o2) {
		    String[] s1     = (String[]) o1;
		    String[] s2     = (String[]) o2;		
		    return s2[0].length()-s1[0].length();
		}
	    };
        Object[] array = idList.toArray();
        Arrays.sort(array, comp);
	idList = (List<String[]>) Misc.toList(array);
	if(!didone) {
	    didone = true;
	    for(String [] tuple: idList) {
		//		System.out.println(tuple[0]);
	    }
	}
        //Replace any references to old entries
        for (Entry newEntry : newEntries) {
            newEntry.getTypeHandler().convertIdsFromImport(newEntry, idList);
        }

        addNewEntries(request, newEntries, true);

	for(Entry newEntry: newEntries) {
	    Element permissions = (Element)
		newEntry.getProperty(AccessManager.TAG_PERMISSIONS);
	    if(permissions!=null) {
		getAccessManager().applyEntryXml(newEntry, permissions);
	    }
	}




        return newEntries;
    }

    public List<Entry> createRemoteEntries(Request request, ServerInfo serverInfo, String entriesXml)
	throws Exception {
	String serverUrl = serverInfo.getUrl();
	final Entry parentEntry =
	    new Entry(getRepository().getGroupTypeHandler(), true);
	parentEntry.setId(getEntryManager().getRemoteEntryId(serverUrl,
							     ""));
	getEntryManager().cacheEntry(parentEntry);
	parentEntry.setRemoteServer(serverInfo);
	parentEntry.setUser(getUserManager().getAnonymousUser());
	//            parentEntry.setParentEntry(tmpEntry);
	parentEntry.setName(serverUrl);


	StringBuilder msg = new StringBuilder();
	List<Entry> entries = new ArrayList<Entry>();
	//            System.err.println("Remote URL:" + remoteSearchUrl);
	//	System.out.println(entriesXml);
	try {
	    Element  root     = XmlUtil.getRoot(entriesXml);
	    List<Element> elements = new ArrayList<Element>();
	    if(root==null || root.getTagName()==null) {
		getLogManager().logSpecial("Empty XML creating remote entries:" + serverInfo);
		return entries;
	    }

	    //Is this a search result
	    if(XmlUtil.getAttribute(root,ATTR_TYPE,"").equals("type_dummy") ||
	       (XmlUtil.getAttribute(root,"name","").equals("Results") &&
		XmlUtil.getAttribute(root,"parent","").equals(""))) {
		NodeList children = XmlUtil.getElements(root,TAG_ENTRY);
		for (int i = 0; i < children.getLength(); i++) {
		    elements.add((Element) children.item(i));
		}
	    } else if(root.getTagName().equals(TAG_ENTRY)) {	
		elements.add(root);
	    } else {
		System.err.println("Unknown remote entry xml:"+entriesXml);
	    }

	    for (Element node:elements) {
		List<Entry> entryList =
		    createEntryFromXml(request, node,
				       parentEntry, new Hashtable(),
				       new Hashtable<String,Entry>(),
				       false, TEMPLATE.YES,INTERNAL.NO,msg,serverInfo);

		if(entryList.size()==0) continue;
		Entry entry = entryList.get(0);
		//Add the little arrow and the server's slug as a name prefix
		String prefix="&#10548;";
		String slug = serverInfo.getSlug();
		if(stringDefined(slug)) prefix+=slug+" - ";
		entry.setName(prefix + entry.getName());		
		entry.setRemoteServer(serverInfo);
		Resource resource =  new Resource(
						  "remote:"
						  + XmlUtil.getAttribute(
									 node, ATTR_RESOURCE,
									 ""), Resource.TYPE_REMOTE_FILE);
		String fileSize = XmlUtil.getAttribute(node,ATTR_FILESIZE,(String)null);
		if(stringDefined(fileSize)) resource.setFileSize(Long.parseLong(fileSize));
		entry.setResource(resource);
		Date createDate = (XmlUtil.hasAttribute(node, ATTR_CREATEDATE)
				   ? getDateHandler().parseDate(
								XmlUtil.getAttribute(
										     node, ATTR_CREATEDATE))
				   : null);
		if(createDate!=null) entry.setCreateDate(createDate.getTime());
		String id = XmlUtil.getAttribute(node, ATTR_ID);
		entry.setId(getEntryManager().getRemoteEntryId(serverUrl, id));
		entry.setRemoteServer(serverInfo);
		entry.setRemoteUrl(serverUrl + "/entry/show?entryid=" + id);
		entry.setRemoteId(id);
		//		System.err.println("Remote entry:" + entry.getRemoteUrl());
		getEntryManager().cacheEntry(entry);
		entries.add((Entry) entry);
	    }
	} finally {}

	return entries;
    }



    public List<Entry> createEntryFromXml(Request request, Element node,
                                          Hashtable<String, Entry> entryMap,
                                          Hashtable<String, File> filesMap,
                                          boolean checkAccess,TEMPLATE isTemplate,
					  INTERNAL  internal,StringBuilder msg)
	throws Exception {
        String parentId    = XmlUtil.getAttribute(node, ATTR_PARENT, "");
        Entry  parentEntry =  entryMap.get(parentId);
        if (parentEntry == null) {
            parentEntry = (Entry) getEntry(request, parentId);
        }
        if (parentEntry == null) {
            parentEntry = (Entry) findEntryFromName(request, null, parentId);
        }
        if (parentEntry == null) {
	    //Check for the wild card
	    parentEntry =  entryMap.get("*");
        }	

        if (parentEntry == null) {
            // Lets not check for now. Some entry xml doesn't have a parent
	    throw new RepositoryUtil.MissingEntryException("Could not find parent:" + parentId);
	    // +" xml:" + XmlUtil.toString(node));
        }

        List<Entry> entryList = createEntryFromXml(request, node,
						   parentEntry, filesMap, entryMap, checkAccess,
						   isTemplate,internal,msg);
	addImportedEntries(node,entryMap,entryList);
	return entryList;
    }

    public List<Entry> createEntryFromXml(Request request, Element node, Entry parentEntry,
                                          Hashtable<String, File> filesMap,
					  Hashtable<String, Entry> entryMap,
                                          boolean checkAccess, TEMPLATE isTemplate,
                                          INTERNAL isInternal,StringBuilder msg,ServerInfo...remoteServers)
	throws Exception {

        boolean doAnonymousUpload = false;
        String name = cleanupEntryName(Utils.getAttributeOrTag(node,
							       ATTR_NAME, ""));

        String originalId = XmlUtil.getAttribute(node, ATTR_ID,
						 (String) null);

        String category = Utils.getAttributeOrTag(node, ATTR_CATEGORY, "");
        int entryOrder = Utils.getAttributeOrTag(node, ATTR_ENTRYORDER, Entry.DEFAULT_ORDER);	
        String description = Utils.getAttributeOrTag(node, ATTR_DESCRIPTION,
						     (String) null);

        if (description == null) {
            Element descriptionNode = XmlUtil.findChild(node,
							TAG_DESCRIPTION);
            if (descriptionNode != null) {
                description = XmlUtil.getChildText(descriptionNode);
                if ((description != null)
		    && XmlUtil.getAttribute(descriptionNode, "encoded",
					    false)) {
                    description = new String(Utils.decodeBase64(description));
                }
            }
        }
        if (description == null) {
            description = "";
        }

	description = description.replaceAll("_rightbracket_","]");
	if(description.length()>10000) {
	    //	    System.err.println("l:" + name + " " + description.length());
	}


        if (checkAccess && (parentEntry != null)) {
            if ( !getAccessManager().canDoNew(request, parentEntry)) {
                if (getAccessManager().canDoUpload(request, parentEntry)) {
                    doAnonymousUpload = true;
                } else {
                    throw new IllegalArgumentException(
						       "Cannot add to parent folder");
                }
            }
        }


        String file = Utils.getAttributeOrTag(node, ATTR_FILE, (String) null);
        String fileName = Utils.getAttributeOrTag(node, ATTR_FILENAME,
						  (String) null);


        if (file != null) {
            File tmp = ((filesMap == null)
                        ? null
                        : filesMap.get(file));
            if (tmp == null) {
                throw new IllegalStateException("No file in import: " + file);
            }
            if (doAnonymousUpload) {
                File newFile =
                    getStorageManager().moveToAnonymousStorage(request, tmp,
							       "");

                file = newFile.toString();
            } else {
                String targetName = fileName;
                if (targetName == null) {
                    targetName = new File(file).getName();
                }
                if (targetName != null) {
                    targetName =
                        getStorageManager().getStorageFileName(targetName);
                }
                File newFile = getStorageManager().moveToStorage(request,
								 tmp, targetName);
                file = newFile.toString();
            }
        }



        String url = Utils.getAttributeOrTag(node, ATTR_URL, (String) null);
        if (url == null) {
            url = XmlUtil.getGrandChildText(node, ATTR_URL, (String) null);
        }
        if (url == null) {
            if (Misc.equals(XmlUtil.getAttribute(node, ATTR_RESOURCE_TYPE,
						 (String) null), Resource.TYPE_URL)) {
                url = Utils.getAttributeOrTag(node, ATTR_RESOURCE,
					      (String) null);
            }
        }

        if ((url != null)
	    && XmlUtil.getAttribute(node, "download",
				    "").equals("true")) {
            URL u = new URL(url);
            File f = getStorageManager().getTmpFile(IO.getFileTail(u.getFile()));
            Utils.writeTo(u, f);
            if ( !f.exists()) {
                throw new IllegalArgumentException("Failed to download URL:"
						   + u);
            }
            file = getStorageManager().moveToStorage(request, f,
						     f.getName()).toString();
        }


        String localFile = XmlUtil.getAttribute(node, ATTR_LOCALFILE,
						(String) null);
        String localFileToMove = XmlUtil.getAttribute(node,
						      ATTR_LOCALFILETOMOVE, (String) null);

        String directory = XmlUtil.getAttribute(node, ATTR_DIRECTORY,
						(String) null);
	boolean unique = XmlUtil.getAttributeFromTree(node, ATTR_UNIQUE,false);
	boolean nameUnique = XmlUtil.getAttributeFromTree(node, "nameUnique",false);	
        List<Resource> resources = new ArrayList<Resource>();

        if (file != null) {
            resources.add(new Resource(file, Resource.TYPE_STOREDFILE));
        } else if (localFile != null) {
            if ( !request.getUser().getAdmin()) {
                throw new IllegalArgumentException(
						   "Only administrators can upload a local file");
            }
            resources.add(new Resource(localFile, Resource.TYPE_LOCAL_FILE));
        } else if (directory != null) {
            if ( !request.getUser().getAdmin()) {
                throw new IllegalArgumentException(
						   "Only administrators can upload a local directory");
            }
            File dir = getStorageManager().checkReadFile(new File(directory));
            File[] children = dir.listFiles();
            String pattern = XmlUtil.getAttribute(node, ATTR_FILE_PATTERN,
						  (String) null);

            for (File childFile : children) {
                if ( !childFile.isFile()) {
                    continue;
                }
                if ((pattern != null)
		    && !childFile.getName().matches(pattern)) {
                    continue;
                }
                resources.add(new Resource(childFile,
                                           Resource.TYPE_LOCAL_FILE));
            }
            if (resources.size() == 0) {
                throw new IllegalArgumentException("No files found under:"
						   + directory + " " + ((pattern != null)
									? "matching pattern:" + pattern
									: ""));
            }
        } else if (localFileToMove != null) {
            if ( !request.getUser().getAdmin()) {
                throw new IllegalArgumentException(
						   "Only administrators can upload a local file");
            }
            localFileToMove = getStorageManager().moveToStorage(request,
								new File(localFileToMove)).toString();

            resources.add(new Resource(localFileToMove,
                                       Resource.TYPE_STOREDFILE));
        } else if (url != null) {
            Resource resource = new Resource(url, Resource.TYPE_URL);
            resources.add(resource);
            int size = XmlUtil.getAttribute(node, ATTR_SIZE, 0);
            resource.setFileSize(size);
        } else {
            resources.add(new Resource("", Resource.TYPE_UNKNOWN));
        }

        String type = Utils.getAttributeOrTag(node, ATTR_TYPE,
					      TypeHandler.TYPE_FILE);

        //System.err.println("TYPE:" + type + ":");
        List<Entry> entries = new ArrayList<Entry>();
        Date        now     = new Date();
	//Check for an entry with no file that already exists
	if(unique && resources.size()==1 && resources.get(0).isUnknown()) {
	    Entry  childEntry = findEntryWithName(request, parentEntry,
						  name);
	    if(childEntry!=null) {
		entries.add(childEntry);
		msg.append("<li> Non-unique entry: " + childEntry.getName());
		addImportedEntries(node,entryMap,entries);
		//Return empty list
		entries = new ArrayList<Entry>();
		return entries;
	    }
	}
        for (Resource resource : resources) {
	    if(unique) {
		List<Entry> tmp = getEntriesWithResource(getRepository().getAdminRequest(), resource);
		if(tmp.size()>0) {
		    msg.append("<li> Non-unique file: " + resource);
		    continue;
		}
	    }


            TypeHandler typeHandler = null;
            if (type.equals(TypeHandler.TYPE_GUESS)) {
                typeHandler = findDefaultTypeHandler(resource.getPath());
            }

            if (typeHandler == null) {
                //Pass in false so we error if the repository does not find the type
                typeHandler = getRepository().getTypeHandler(type);
            }


            if (typeHandler == null) {
                throw new RepositoryUtil.MissingEntryException(
							       "Could not find type:" + type);
            }
            if ( !canBeCreatedBy(request, typeHandler)) {
                throw new IllegalArgumentException(
						   "Cannot create an entry of type "
						   + typeHandler.getDescription());
            }


            Date createDate = (XmlUtil.hasAttribute(node, ATTR_CREATEDATE)
                               ? getDateHandler().parseDate(
							    XmlUtil.getAttribute(
										 node, ATTR_CREATEDATE))
                               : now);
            Date changeDate = (XmlUtil.hasAttribute(node, ATTR_CHANGEDATE)
                               ? getDateHandler().parseDate(
							    XmlUtil.getAttribute(
										 node, ATTR_CHANGEDATE))
                               : createDate);
            //don't use the create and change date from the xml
            createDate = changeDate = now;
	    Date fromDate = (XmlUtil.hasAttribute(node, ATTR_FROMDATE)
                             ? getDateHandler().parseDate(
							  XmlUtil.getAttribute(node, ATTR_FROMDATE))
                             : createDate);
            Date toDate = (XmlUtil.hasAttribute(node, ATTR_TODATE)
                           ? getDateHandler().parseDate(
							XmlUtil.getAttribute(node, ATTR_TODATE))
                           : fromDate);

            String id    = getRepository().getGUID();
            Entry  entry = typeHandler.createEntry(id);
	    if(remoteServers.length>0) entry.setRemoteServer(remoteServers[0]);
            if (originalId != null) {
                entry.putProperty(ATTR_ORIGINALID, originalId);
            }
            String entryName = name;
            if (entryName.length() == 0) {
                entryName = resource.getTheFile().getName();
            }

	    
	    if(nameUnique) {
		Entry  childEntry = findEntryWithName(request, parentEntry,   entryName);
		if(childEntry!=null) {
		    msg.append("<li> Non-unique entry: " + childEntry.getName());
		}
		continue;
	    }

            initEntry(entry, entryName, description, parentEntry,
		      request.getUser(), resource, category,
		      entryOrder,
		      createDate.getTime(), changeDate.getTime(),
		      fromDate.getTime(), toDate.getTime(), null);

	    Element permissions  =XmlUtil.findChild(node,AccessManager.TAG_PERMISSIONS);
	    if(permissions!=null) {
		entry.putProperty(AccessManager.TAG_PERMISSIONS, permissions);
	    }


	    String pathTemplate = request.getString(ARG_PATHTEMPLATE,"");
	    if(Utils.stringDefined(pathTemplate)) {
		entry.setParentEntry(getPathEntry(request,parentEntry,entry,pathTemplate));
	    }

	    if(isInternal==INTERNAL.NO) {
		entry.setRemoteParentEntryId(XmlUtil.getAttribute(node,"parent",(String)null));
	    }


            if (doAnonymousUpload) {
                initUploadedEntry(request, entry, parentEntry);
            }
            String lat = Utils.getAttributeOrTag(node, ATTR_LATITUDE, null);
            String lon = Utils.getAttributeOrTag(node, ATTR_LONGITUDE, null);

            if ((lat != null) && (lon != null)) {
                entry.setNorth(GeoUtils.decodeLatLon(lat));
                entry.setSouth(entry.getNorth());
                entry.setWest(GeoUtils.decodeLatLon(lon));
                entry.setEast(entry.getWest());
            } else {
                entry.setNorth(GeoUtils.decodeLatLon(XmlUtil.getAttribute(node,
								       ATTR_NORTH, entry.getNorth() + "")));
                entry.setSouth(GeoUtils.decodeLatLon(XmlUtil.getAttribute(node,
								       ATTR_SOUTH, entry.getSouth() + "")));
                entry.setEast(GeoUtils.decodeLatLon(XmlUtil.getAttribute(node,
								      ATTR_EAST, entry.getEast() + "")));
                entry.setWest(GeoUtils.decodeLatLon(XmlUtil.getAttribute(node,
								      ATTR_WEST, entry.getWest() + "")));
            }

            entry.setAltitudeTop(Utils.getAttributeOrTag(node,
							 ATTR_ALTITUDE_TOP, entry.getAltitudeTop()));
            entry.setAltitudeBottom(Utils.getAttributeOrTag(node,
							    ATTR_ALTITUDE_BOTTOM, entry.getAltitudeBottom()));
            entry.setAltitudeTop(Utils.getAttributeOrTag(node, ATTR_ALTITUDE,
							 entry.getAltitudeTop()));
            entry.setAltitudeBottom(Utils.getAttributeOrTag(node,
							    ATTR_ALTITUDE, entry.getAltitudeBottom()));

            NodeList entryChildren = XmlUtil.getElements(node);
            for (Element entryChild : (List<Element>) entryChildren) {
                String tag = entryChild.getTagName();
                if (tag.equals("tag")) {
                    getMetadataManager().addMetadata(request,entry,
						     new Metadata(getRepository().getGUID(),
								  entry.getId(), "enum_tag", true,
								  XmlUtil.getChildText(entryChild),
								  "", "", "", ""));

                } else if (tag.equals(TAG_METADATA)) {
                    getMetadataManager().processMetadataXml(request,entry,
							    entryChild, filesMap, isInternal);
                } else if (tag.equals(TAG_DESCRIPTION)) {}
                else {
                    //                throw new IllegalArgumentException("Unknown tag:"
                    //                        + node.getTagName());
                }
            }

            entry.setXmlNode(node);
            entry.getTypeHandler().initializeEntryFromXml(request, entry,
							  node, filesMap);
            entries.add(entry);
        }

        return entries;

    }



    private void addImportedEntries(Element node, Hashtable<String,Entry> entries, List<Entry> entryList) {
	String tmpid = XmlUtil.getAttribute(node, ATTR_ID, (String) null);
        if (tmpid != null) {
            for (Entry entry : entryList) {
                entries.put(tmpid, entry);
            }
        }
        String aliases = XmlUtil.getAttribute(node, "aliases", (String) null);
        if (aliases != null) {
	    for(String alias: Utils.split(aliases,",",true,true)) {
		for (Entry entry : entryList) {
		    entries.put(alias, entry);
		}
            }
        }	
    }

    /**
     *  trim and remove the delimiter character
     *
     * @param name _more_
     *
     * @return _more_
     */
    public String cleanupEntryName(String name) {
        if (name.length() > 200) {
            name = name.substring(0, 195) + "...";
        }
        name = name.replaceAll(Entry.PATHDELIMITER, "-");

        return name;
    }


    public Entry getPathEntry(Request request, Entry parentEntry, Entry entry, String path) throws Exception {
	if(!Utils.stringDefined(path)) return parentEntry;
	List<String> toks = Utils.split(path,"/",true,true);
	Entry pathEntry = parentEntry;
	for(String tok: toks) {
	    String parentType = "group";
	    List<String> subToks = Utils.splitUpTo(tok,":",2);
	    if(subToks.size()>1) {
		tok = subToks.get(0).trim();
		Hashtable props = Utils.getProperties(subToks.get(1));
		parentType = Utils.getProperty(props,"type",parentType);
	    }
	    String parentName = replaceMacros(entry, tok);
	    Entry newEntry = findEntryFromPath(request, pathEntry,parentName);
	    if(newEntry==null) {
		if(!getAccessManager().canDoNew(request, pathEntry)) {
		    throw new AccessException("Can't create new entry:" + pathEntry,request);
		}

		newEntry = makeNewGroup(request,pathEntry, parentName,request.getUser(),
					null,parentType);
	    }
	    pathEntry  = newEntry;
	}
	return pathEntry;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param args _more_
     *
     * @return _more_
     */
    public String getEntryLink(Request request, Entry entry, String hrefAttrs, String... args) {
        return getEntryLink(request, entry, false, hrefAttrs, args);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param addIcon _more_
     * @param args _more_
     *
     * @return _more_
     */
    public String getEntryLink(Request request, Entry entry, boolean addIcon, String hrefAttrs,
                               String... args) {
        try {
            String label = (addIcon
                            ?   getPageHandler().getEntryIconImage(request, entry) + " "
                            : "") + getEntryDisplayName(entry);

            return HU.href(getEntryURL(request, entry, args), label, hrefAttrs);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param args _more_
     *
     * @return _more_
     */
    public String getEntryURL(Request request, Entry entry, String... args) {
	try {
	    if(request==null) request = getRepository().getTmpRequest();
	    return request.entryUrl(getRepository().URL_ENTRY_SHOW, entry, args);
	} catch(Exception exc) {
	    throw new RuntimeException(exc);
	}
    }


    public String getPopupLink(Request request, Entry entry,
			       String linkText) throws Exception {
	
        String        entryIcon = getPageHandler().getEntryIconImage(request, entry);
	String link = entryIcon+HU.SPACE+linkText;
	StringBuilder inner = new StringBuilder();
	String snippet = getWikiManager().getSnippet(request, entry, true,null);
	if(Utils.stringDefined(snippet)) {
	    HU.div(inner,snippet,HU.cssClass("ramadda-snippet"));
	}
	if (entry.isImage()) {
	    String img  = getEntryResourceUrl(request, entry);
	    inner.append(HU.center(HU.img(img,"",HU.attr("width","90%") + HU.style("max-width","400px"))));
	}

	List<Entry> children = getChildren(request, entry);
	for (Entry child : getEntryUtil().sortEntriesOnName(children,false)) {
	    String url       = getEntryUrl(request, child);
	    String linkLabel = child.getName();
	    linkLabel =getPageHandler().getEntryIconImage(request,child) + HU.space(1) + linkLabel;
	    String href = HU.href(url, linkLabel,HU.attrs("title",getPageHandler().getEntryTooltip(child)));
	    inner.append(HU.div(href,HU.attrs("class","ramadda-menu-item")));
	}
	String html =HU.div(inner.toString(),HU.style("min-width","300px","max-height","200px","overflow-y","auto"));
	String title = HU.div(entryIcon+HU.SPACE+HU.href(getEntryUrl(request, entry),entry.getName()),HU.style("margin-left","20px","margin-right","20px"));
        String menuLink = HU.makePopup(null, link, html,arg("title", title), arg("draggable",true),arg("header",true),arg("decorate",true));
	return menuLink;

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param linkText _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public EntryLink getAjaxLink(Request request, Entry entry, String linkText)
	throws Exception {
        return getAjaxLink(request, entry, linkText, null);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param linkText _more_
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public EntryLink getAjaxLink(Request request, Entry entry, String linkText, String url)
	throws Exception {
        return getAjaxLink(request, entry, linkText, url, request.get(ARG_DECORATE, true), request.get("showIcon", true));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param linkText _more_
     * @param url _more_
     * @param textBeforeEntryLink _more_
     * @param decorateMetadata _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public EntryLink getAjaxLink(Request request, Entry entry, String linkText, String url,  boolean decorateMetadata, boolean showIcon)
	throws Exception {

        String  entryShowUrl =  request.makeUrl(getRepository().URL_ENTRY_SHOW);
	boolean forTree = true;
        boolean forTreeView  = request.get(ARG_TREEVIEW, false);
        if (url == null) {
            //For now don't use the full entry path
            url = HU.url(entryShowUrl, ARG_ENTRYID, entry.getId());
            //            url = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry);
        } else if (forTreeView) {
            url = url.replace("%27", "'");
            url = url.replace("'", "");
        }


        if (forTreeView) {
            String label = getEntryListName(request, entry);
            label = label.replace("'", "\\'");
            url = Utils.concatString("javascript:",
                                     HU.call("treeViewClick",
					     HU.jsMakeArgs(true,
							   entry.getId(), url, label)));
            forTree = false;
        }


        boolean showUrl     = request.get(ARG_DISPLAYLINK, true);
        boolean showDetails = request.get(ARG_DETAILS, true);
        showIcon    = request.get("showIcon", showIcon);
        String nameTemplate   = request.getString("nameTemplate",null);
        String  entryId     = entry.getId();
        String  uid         = HU.getUniqueId("link_");
        String  output      = "inline";
        String  targetId    = HU.getUniqueId("targetspan_");
        String  qtargetId   = HU.squote(targetId);
        boolean okToMove    = !request.getUser().getAnonymous();
        String  prefix      = "";

	String folderClickUrl =null;
        if (forTree) {
            folderClickUrl = HU.url(entryShowUrl, ARG_ENTRYID,
				    entry.getId(), ARG_OUTPUT, output,
				    ARG_DETAILS,
				    Boolean.toString(showDetails),
				    "showIcon",
				    Boolean.toString(showIcon),						  
				    ARG_DISPLAYLINK,
				    Boolean.toString(showUrl), forTreeView
				    ? ARG_TREEVIEW
				    : "nop", "true");
	    if(nameTemplate!=null)
		folderClickUrl+="&"+HU.arg("nameTemplate",nameTemplate);
            String message = entry.isGroup()
		? "Click to open folder"
		: "Click to view contents";
            String imgClick =
                HU.onMouseClick(HU.call("Ramadda.folderClick",
					HU.comma(HU.squote(uid),
						 HU.squote(folderClickUrl),
						 HU.squote(getDownArrowIcon()))));


            prefix = HU.jsLink("",HU.span(getIconImage("fas fa-caret-right"),
					  HU.attrs("style","margin-right:4px;","class", "entry-arrow","title",message,"id","img_"+uid) +
					  imgClick));
        }

        StringBuilder sourceEvent = new StringBuilder();
        StringBuilder targetEvent = new StringBuilder();
        String        entryIcon = getPageHandler().getIconUrl(request, entry);
        String        iconId      = "img_" + uid;


        StringBuilder imgText = new StringBuilder();
        if (okToMove) {
            imgText.append(msg("Drag to move"));
            imgText.append("; ");
        }
        String imgUrl = null;
        if (entry.getResource().isUrl()) {
            try {
                imgUrl = entry.getTypeHandler().getPathForEntry(request,
								entry,false);
                imgText.append(msg("Click to view URL"));
            } catch (Exception exc) {
                imgUrl = "bad image";
                imgText.append("Error:" + exc);
            }

        } else if (entry.getResource().isFile()) {
            if (getAccessManager().canDownload(request, entry)) {
                imgUrl = entry.getTypeHandler().getEntryResourceUrl(request, entry);
                imgText.append(msg("Click to download file"));
            }
        }

        String img = HU.img(entryIcon, imgText.toString(),
			    HU.attr("width",ICON_WIDTH) +
			    Utils.concatString(HU.id(iconId),
					       sourceEvent.toString()));

        StringBuilder sb = new StringBuilder();
        HU.open(sb, HU.TAG_SPAN,
		HU.attr("title", linkText)
		+ HU.cssClass("entry-name")
		+ HU.id(targetId) + targetEvent.toString());

        sb.append(prefix);
        if (imgUrl != null) {
            img = HU.href(imgUrl, img);
        }
	if(showIcon)
	    sb.append(img);
        sb.append(HU.space(1));
        if (decorateMetadata) {
            getMetadataManager().decorateEntry(request, entry, sb, true);
        }
        if (showUrl) {
            HU.span(sb, getTooltipLink(request, entry, linkText, url),
		    HU.cssClass("entry-link"));
        } else {
            HU.span(sb, linkText,
		    Utils.concatString(targetEvent.toString(),
				       HU.cssClass("entry-link")));
        }

        HU.close(sb, HU.TAG_SPAN);
        String folderBlock = ( !forTree
                               ? ""
                               : HU.div("",
					HU.attrs(HU.ATTR_STYLE,
						 "display:none;", HU.ATTR_CLASS,
						 CSS_CLASS_FOLDER_BLOCK,
						 HU.ATTR_ID, uid)));

        return new EntryLink(sb.toString(), folderBlock, uid, folderClickUrl);

    }





    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param linkText _more_
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getTooltipLink(Request request, Entry entry,
                                 String linkText, String url)
	throws Exception {
        if (url == null) {
            url = getEntryUrl(request, entry);
            //            url = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry);
        }

        String elementId  = entry.getId();
        String qid        = HU.squote(elementId);
        String linkId     = HU.getUniqueId("link_");
        String qlinkId    = HU.squote(linkId);

        String target     = (request.defined(ARG_TARGET)
                             ? request.getString(ARG_TARGET, "")
                             : null);
        String targetAttr = ((target != null)
                             ? HU.attr(HU.ATTR_TARGET, target)
                             : "");

        return HU.href(url, linkText,
		       HU.id(linkId) + targetAttr);
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
    public List<Link> getEntryLinks(Request request, Entry entry)
	throws Exception {
        List<Link>          links = new ArrayList<Link>();
        OutputHandler.State state;
	if (handleEntryAsGroup(entry)) {
	    List<Entry>  children     = new ArrayList<Entry>();
	    try {
		getChildrenEntries(request, getRepository().getHtmlOutputHandler(),entry, children);
	    } catch(Exception exc) {
		//Don't throw the exception since this would block the user from showing the entry menu
                logError("Calling getChildrenEntries:"+entry.getName() , exc);
	    }
	    state = new OutputHandler.State(entry,children);
	} else  {
	    state = new OutputHandler.State(entry);
	}
        entry.getTypeHandler().getEntryLinks(request, entry,state, links);
        links.addAll(getRepository().getOutputLinks(request, state));
        return links;
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param file _more_
     * @param andInsert _more_
     *
     * @throws Exception _more_
     */
    public void addAttachment(Request request,Entry entry, File file, boolean andInsert)
	throws Exception {
        String theFile = getStorageManager().moveToEntryDir(entry,
							    file).getName();
        getMetadataManager().addMetadata(request,
					 entry,
					 new Metadata(
						      getRepository().getGUID(), entry.getId(),
						      ContentMetadataHandler.TYPE_ATTACHMENT, false, theFile, "",
						      "", "", ""));
        if (andInsert) {
            updateEntry(null, entry);
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param typeMask _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getEntryActionsTable(Request request, Entry entry, int typeMask)
	throws Exception {
        List<Link> links = getEntryLinks(request, entry);
        return getEntryActionsTable(request, entry, typeMask, links);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param typeMask _more_
     * @param links _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getEntryActionsTable(Request request, Entry entry,
                                       int typeMask, List<Link> links)
	throws Exception {
        return getEntryActionsTable(request, entry, typeMask, links, false,
                                    null);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param typeMask _more_
     * @param links _more_
     * @param returnNullIfNoneMatch _more_
     * @param header _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getEntryActionsTable(Request request, Entry entry,
                                       int typeMask, List<Link> links,
                                       boolean returnNullIfNoneMatch,
                                       Hashtable props)
	throws Exception {
	if(links==null)
	    links = getEntryLinks(request, entry);	    
	final boolean showLabel = props==null?true:Utils.getProperty(props,"showLabel",true);
	String header = props==null?null:Utils.getProperty(props,"header",null);
	Hashtable<Object,Integer> count  =new Hashtable<Object,Integer>();
        StringBuilder
            viewSB          = null,
            exportSB        = null,
            nonHtmlSB       = null,
            actionSB        = null,
            otherSB      = null,
            fileSB          = null;
        int     cnt         = 0;
        boolean needToAddHr = false;
        for (Link link : links) {

            if ( !link.isType(typeMask)) {
                continue;
            }
            StringBuilder sb;
            if (link.isType(OutputType.TYPE_VIEW)) {
                if (viewSB == null) {
                    viewSB =new  StringBuilder();
                    cnt++;
                }
                sb = viewSB;
            } else if (link.isType(OutputType.TYPE_FEEDS)) {
                if (exportSB == null) {
                    cnt++;
                    exportSB =  new StringBuilder();
                }
                sb = exportSB;
            } else if (link.isType(OutputType.TYPE_FILE)) {
                if (fileSB == null) {
                    cnt++;
                    fileSB = new StringBuilder();
                }
                sb = fileSB;
            } else if (link.isType(OutputType.TYPE_OTHER)) {
                if (otherSB == null) {
                    cnt++;
                    otherSB = new StringBuilder();
                }
                sb = otherSB;
            } else {
                if (actionSB == null) {
                    cnt++;
                    actionSB = new StringBuilder();
                }
                sb = actionSB;
            }
            //Only add the hr if we have more things in the list
            if (needToAddHr && (sb.length() > 0)) {
                sb.append("</div>");
		HU.div(sb,
		       "",
		       HU.cssClass(CSS_CLASS_MENUITEM_SEPARATOR));
		HU.open(sb,
			HU.TAG_DIV,
			HU.cssClass(CSS_CLASS_MENU_GROUP));
            }
            needToAddHr = link.getHr();
            if (needToAddHr) {
                continue;
            }
	    Integer c = count.get(sb);
	    if(c == null) c = Integer.valueOf(0);
	    c  = Integer.valueOf(c.intValue()+1);
	    count.put(sb,c);

	    if(sb.length()==0) {
		HU.open(sb,
			HU.TAG_DIV,
			HU.cssClass(CSS_CLASS_MENU_GROUP));
	    }
            HU.open(sb, HU.TAG_DIV, "class",
		    CSS_CLASS_MENUITEM);
            if (link.getIcon() == null) {
                sb.append(HU.SPACE);
            } else {
                HU.href(sb, link.getUrl(),
			getIconImage(link.getIcon(),"width",ICON_WIDTH));
            }
	    sb.append(HU.SPACE);
	    String tooltip = link.getTooltip();
	    if(!Utils.stringDefined(tooltip)) tooltip = link.getLabel();
	    HU.href(sb,
		    link.getUrl(), msg(link.getLabel()),
		    HU.attrs("title",tooltip,"class",
			     CSS_CLASS_MENUITEM_LINK));
            sb.append(HU.close(HU.TAG_DIV));
        }

        if (returnNullIfNoneMatch && (cnt == 0)) {
            return null;
        }


        StringBuilder menu = new StringBuilder();
        if (header != null) {
            menu.append(
			HU.div(
			       header, HU.cssClass("ramadda-entry-menu-title")));
        }
        menu.append("<table class=\"ramadda-menu\">");
        HU.open(menu, HU.TAG_TR, HU.ATTR_VALIGN, "top");

	BiConsumer<StringBuilder,String> finisher = (sb,label) -> {
	    if(sb==null) return;
            sb.append("</div>");
	    Integer c = count.get(sb);
	    String s = sb.toString();
	    if(c.intValue()<12) {
		s = s.replaceAll("ramadda-menugroup","ramadda-menugroup ramadda-menugroup-ext");
	    }
	    if(!showLabel) label="";
	    else label =  HU.b(msg(label)) + "<br>";
			       
            menu.append(HU.tag(HU.TAG_TD, "", label + s));
	};

	finisher.accept(fileSB,"File");
	finisher.accept(actionSB,"Edit");
	finisher.accept(viewSB,"View");
	finisher.accept(exportSB,"Links");
	finisher.accept(otherSB,"Etc");	
	
        if (typeMask!=OutputType.TYPE_ALL && (typeMask & OutputType.TYPE_CHILDREN) != 0) {
            List<Entry> children = getChildrenSafe(request, entry);
            if (children.size() > 0) {
                StringBuilder childrenSB = new StringBuilder();
		for (Entry child : getEntryUtil().sortEntriesOnName(children,false)) {
                    String url       = getEntryUrl(request, child);
                    String linkLabel = noMsg(child.getName());
                    linkLabel =
                        getPageHandler().getEntryIconImage(request, child) + HU.space(1) + linkLabel;
                    String href = HU.href(url, linkLabel,HU.attrs("title",getPageHandler().getEntryTooltip(child)));
		    HU.div(childrenSB,href,HU.attrs("class","ramadda-menu-item"));
                }
		HU.tag(menu,
		       HU.TAG_TD, "",
		       HU.b(msg("Children"))  
		       + HU.div(
				childrenSB.toString(),
				HU.clazz("ramadda-menugroup ramadda-menugroup-ext")));
            }
        }

        menu.append(HU.close(HU.TAG_TR));
        menu.append(HU.close(HU.TAG_TABLE));

        return menu.toString();


    }







    /**
     *     _more_
     *
     *     @param request _more_
     *     @param entry _more_
     *
     *     @return _more_
     *
     *     @throws Exception _more_
     */
    public Entry getParent(Request request, Entry entry) throws Exception {
        return getParent(request, entry, true);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param checkAccess _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getParent(Request request, Entry entry, boolean checkAccess)
	throws Exception {

        Entry parent = getEntry(request, entry.getParentEntryId(),
                                checkAccess);
        if ((parent != null) && parent.equals(entry)) {
            //Whoa, got a loop
            System.err.println("EntryManager: got a loop:" + entry.getName()
                               + " " + parent.getName());

            return null;
        }

        return parent;
    }



    /**
     * _more_
     *
     *
     * @param entryId _more_
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getEntry(Request request, String entryId) throws Exception {
        return getEntry(request, entryId, true);
    }


    /**
     * _more_
     *
     * @param entryId _more_
     * @param request _more_
     * @param andFilter _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getEntry(Request request, String entryId, boolean andFilter)
	throws Exception {
        return getEntry(request, entryId, andFilter, false);
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
    public Entry getEntry(Request request) throws Exception {
        return getEntryFromArg(request, ARG_ENTRYID);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param urlArg _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getEntryFromArg(Request request, String urlArg)
	throws Exception {
        String entryId = request.getString(urlArg, (String) null);
        if ( !Utils.stringDefined(entryId)) {
            return null;
        }
        Entry entry = getEntry(request, entryId);
        if (entry == null) {
            entry = findEntryFromName(request, null, entryId);
        }

        if (entry == null) {
            Entry tmp = getEntry(request, request.getString(urlArg, BLANK),
                                 false);
            if (tmp != null) {
                logInfo("Cannot access entry:" + entryId + "  IP:"
                        + request.getIp());

                logInfo("Request:" + request);

                throw new AccessException(
					  "You do not have access to this entry", request);
            }

            throw new RepositoryUtil.MissingEntryException(
							   "Could not find entry:" + request.getString(urlArg, BLANK));
        }
        return entry;
    }




    /**
     * _more_
     *
     * @param server _more_
     * @param id _more_
     *
     * @return _more_
     */
    public String getRemoteEntryId(String server, String id) {
        return ID_PREFIX_REMOTE + Utils.encodeBase64(server)
	    + TypeHandler.ID_DELIMITER + id;
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public String[] getRemoteEntryInfo(String id) {
        if (id.length() == 0) {
            return new String[] { "", "" };
        }
        id = id.substring(ID_PREFIX_REMOTE.length());
        String[] pair = Utils.split(id, TypeHandler.ID_DELIMITER, 2);
        if (pair == null) {
            return new String[] { "", "" };
        }
        pair[0] = new String(Utils.decodeBase64(pair[0]));

        return pair;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param server _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getRemoteEntry(Request request, String server, String id)
	throws Exception {
        String remoteUrl = server + getRepository().URL_ENTRY_SHOW.getPath();
        remoteUrl =
            HU.url(remoteUrl, ARG_ENTRYID, id, ARG_OUTPUT,
		   XmlOutputHandler.OUTPUT_XMLENTRY.toString());
        String entriesXml = getStorageManager().readSystemResource(remoteUrl);

        return null;
    }


    /**
     * If this entry is a harvested or local file (i.e., it is not a stored file
     * in the repository's own storage area) then check its file date. If its greater than the entry's date then change the entry.
     *
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    private void checkEntryFileTime(Entry entry) throws Exception {

        if (true) {
            return;
        }


        /**
         *     Don't do this because many harvested files get a date range set for them
         *
         * if ((entry == null) || !entry.isFile()) {
         *   return;
         * }
         *
         * if (entry.getResource().isStoredFile()) {
         *   return;
         * }
         * File f = entry.getResource().getTheFile();
         * if ((f == null) || !f.exists()) {
         *   return;
         * }
         * if (entry.getStartDate() != entry.getEndDate()) {
         *   return;
         * }
         *
         * long fileTime = f.lastModified();
         * if (fileTime == entry.getStartDate()) {
         *   return;
         * }
         * entry.setStartDate(fileTime);
         * entry.setEndDate(fileTime);
         * updateEntry(null, entry);
         */
    }


    /**
     * _more_
     *
     * @param entryId _more_
     * @param request _more_
     * @param andFilter _more_
     * @param abbreviated _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getEntry(Request request, String entryId, boolean andFilter,
                          boolean abbreviated)
	throws Exception {

        if (entryId == null) {
            debug("getEntry: id is null ");
            return null;
        }
        if (entryId.equals(ID_ROOT)) {
            return request.getRootEntry();
        }

        //        synchronized (MUTEX_ENTRY) {
        Entry entry = getEntryFromCache(entryId);
        if (entry != null) {
            debug("getEntry: from cache:" + entry);
            checkEntryFileTime(entry);
            if ( !andFilter) {
                return entry;
            }
            entry = getAccessManager().filterEntry(request, entry);
            debug("getEntry: after filter:" + entry);
            return entry;
        }

        try {
            if (entryId.startsWith(ID_PREFIX_REMOTE)) {
                String[] tuple = getRemoteEntryInfo(entryId);
                return getRemoteEntry(request, tuple[0], tuple[1]);
            } else if (isSynthEntry(entryId)) {
                String[]    pair          = getSynthId(entryId);
                String      parentEntryId = pair[0];
                String      syntheticPart = pair[1];
                Entry       parentEntry   = null;

                TypeHandler typeHandler   =
                    getSynthTypeHandler(parentEntryId);
                if (typeHandler != null) {
                    parentEntry = typeHandler.getSynthTopLevelEntry();
                }

                if (syntheticPart == null) {
		    return parentEntry;
                }

                if (parentEntry == null) {
                    parentEntry = getEntry(request, parentEntryId, andFilter,
                                           abbreviated);
                }


                if (parentEntry == null) {
                    return null;
                }
                if (typeHandler == null) {
                    typeHandler = parentEntry.getTypeHandler();
                }

		entry = makeSynthEntry(request, typeHandler, parentEntry, entryId, syntheticPart);
                if (entry == null) {
                    return null;
                }
            } else {
                entry = createEntryFromDatabase(entryId, abbreviated);
            }
        } catch (Exception exc) {
            logError("creating entry:" + entryId, exc);
            return null;
        }

	//Try it as an alias
	if(entry==null) {
	    entry =  getEntryFromAlias(request, entryId);
	}

        if ( !abbreviated && (entry != null)) {
            cacheEntry(entry);
        }

        if (andFilter && (entry != null)) {
            entry = getAccessManager().filterEntry(request, entry);
            debug("getEntry: after filter 2:" + entry);
        }

        return entry;
        //    }

    }

    private Entry makeSynthEntry(Request request, TypeHandler typeHandler,
				 Entry parentEntry, String id, String syntheticPart)
	throws Exception {
	Entry entry;

	entry = getSynthEntryCache().get(id);
	if(entry==null) {
	    entry = typeHandler.makeSynthEntry(request, parentEntry, syntheticPart);
	}
	return entry;
    }



    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public TypeHandler getSynthTypeHandler(String id) throws Exception {
        //For backwards compatability
        if (id.equals("process")) {
            id = ProcessFileTypeHandler.TYPE_PROCESS;
        }


        TypeHandler typeHandler = getRepository().getTypeHandler(id);
        if (typeHandler == null) {
            typeHandler = getRepository().getTypeHandler("type_" + id);
        }
        if (typeHandler == null) {
            typeHandler = getSearchManager().getSearchProvider(id);
        }

        return typeHandler;
    }



    public Result processEntryTest(Request request) throws Exception{
	if(true)   return new Result("", MIME_TEXT);
	List<Entry> entries = new ArrayList<Entry>();
	for(String id: Utils.split(request.getString("ids",""), ",",true,true)) {
	    Entry entry = createEntryFromDatabase(id,false);
	    if(entry==null) {
		//	    System.err.println("no entry: testCnt:" + testCnt);
		throw new IllegalArgumentException("No entry:" + id);
	    } 
	    entries.add(entry);
	    if(request.get("addchildren",false)) 
		entries.addAll(getChildren(request, entry));
	}
	getSearchManager().entriesModified(request, entries);
	return new Result("reindexed #" + entries.size(), MIME_TEXT);
    }



    /**
     * _more_
     *
     * @param entryId _more_
     * @param abbreviated _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry createEntryFromDatabase(String entryId, boolean abbreviated)
	throws Exception {
        Entry entry = null;
	Statement entryStmt =null;
        try {
	    entryStmt =	getDatabaseManager().select(Tables.ENTRIES.COLUMNS,
						    Tables.ENTRIES.NAME,
						    Clause.eq(Tables.ENTRIES.COL_ID,
							      entryId));
	    ResultSet results = entryStmt.getResultSet();
            if ( !results.next()) {
		getDatabaseManager().closeAndReleaseConnection(entryStmt);
                return null;
            }
            String entryType = results.getString(Tables.ENTRIES.COL_NODOT_TYPE);
            TypeHandler typeHandler =
                getRepository().getTypeHandler(entryType, true);
            entry = typeHandler.createEntryFromDatabase(entryStmt.getConnection(), results, abbreviated);
            checkEntryFileTime(entry);
        } finally {
	    //getDatabaseManager().closeAndReleaseConnection(entryStmt);
	    getDatabaseManager().closeStatement(entryStmt);
        }

        return entry;
    }



    private static final boolean LUCENE_OK = true;
    private static final boolean LUCENE_NOTOK = false;

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> searchEntries(Request request)
	throws Exception {
        return searchEntries(request, (List<Clause>)null);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param extraClauses _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> searchEntries(Request request, List<Clause> extraClauses)
	throws Exception {
        List<Clause> clauses = getClauses(request);
        if (extraClauses != null) {
            clauses.addAll(extraClauses);
        }
        TypeHandler typeHandler = getRepository().getTypeHandler(request,false);
	//This might be a remote search where we don't have this type
	if(typeHandler==null) {
	    return new ArrayList<Entry>();
	}
	return getEntries(request, clauses, typeHandler, LUCENE_OK);
    }

    /**
       This gets the search clauses in the request
    */
    private List<Clause> getClauses(Request request) throws Exception {
        TypeHandler typeHandler = getRepository().getTypeHandler(request);
        List<Clause> clauses = typeHandler.assembleWhereClause(request);
	return clauses;
    }


    public List<Entry> getEntriesWithType(Request request, String type) 
	throws Exception {
        TypeHandler typeHandler = getRepository().getTypeHandler(request);
	List<Clause> clauses = new ArrayList<Clause>();
	List<Clause> ors = new ArrayList<Clause>();
	getDatabaseManager().addTypeClause(getRepository(),request, Utils.split(type,",",true,true),ors);
	clauses.add(Clause.or(ors));
	return  getEntriesFromDb(request,  clauses, typeHandler);
    }

    public List<Entry> getEntriesWithResource(Request request, Resource resource) 
	throws Exception {
        TypeHandler typeHandler = getRepository().getTypeHandler(request);
	List<Clause> clauses = new ArrayList<Clause>();
        clauses.add(Clause.eq(Tables.ENTRIES.COL_RESOURCE, resource.getPath()));
	return  getEntriesFromDb(request,  clauses, typeHandler);
    }


    public List<Entry> getEntryRootTree(Request request) throws Exception {
	if(request.defined(ARG_ANCESTOR)) {
	    String entryRoot = request.getString(ARG_ANCESTOR,"");
	    Entry root = getEntry(request,entryRoot);
	    if(root!=null) {
		List<Entry> all = new ArrayList<Entry>();
		all.add(root);
		for(Entry child: getChildrenAll(request,root,null)) {
		    all.add(child);
		    for(Entry grandchild:getChildrenAll(request, child,null)) {
			all.add(grandchild);
			all.addAll(getChildrenAll(request, grandchild,null));
		    }
		}
		return all;
	    }
	}
	return null;
    }


    /**
       This bypasses the lucene search and does a search directly in the database
    */
    public List<Entry> getEntriesFromDb(Request request) 
	throws Exception {
	return getEntriesFromDb(request, getClauses(request), (TypeHandler)null);
    }


    public List<Entry> getEntriesFromDb(Request request, List<Clause> clauses,
					TypeHandler typeHandler)
	throws Exception {
	return getEntries(request, clauses, typeHandler, LUCENE_NOTOK);
    }    


    private List<Entry> getEntries(Request request, List<Clause> clauses,
				   TypeHandler typeHandler,boolean luceneOk)
	throws Exception {	
        List<Entry> allEntries    = new ArrayList<Entry>();
	boolean didSearch = false;

	//Check if we should let lucene do the searching
	if(luceneOk) {
	    //	    System.err.println("LUCENE");
	    getSearchManager().processLuceneSearch(request, allEntries);
	    didSearch = true;
	}


	if(!didSearch) {
	    List<Entry> entryTree = getEntryRootTree(request);
	    if(entryTree!=null) {
		List<Clause> ors = new ArrayList<Clause>();
		for(Entry e:entryTree) {
		    ors.add(Clause.eq(Tables.ENTRIES.COL_PARENT_GROUP_ID,e.getId()));
		}
		Clause tree  =Clause.or(ors);
		if(clauses.size()==1) {
		    Clause c = clauses.get(0);
		    clauses = new ArrayList<Clause>();
		    clauses.add(Clause.and(c,tree));
		} else {	
		    clauses.add(tree);
		}
	    }


	    //	    System.err.println("NOT LUCENE");
	    int skipCnt = request.get(ARG_SKIP, 0);
	    SqlUtil.debug = false;
	    boolean canDoSelectOffset = getDatabaseManager().canDoSelectOffset();
	    Hashtable   seen          = new Hashtable();


	    SelectInfo select = new SelectInfo(request);
	    String order = getQueryOrderAndLimit(request, false, null,  select);
	    if(typeHandler==null)
		typeHandler =  getRepository().getTypeHandler(request);
	    if(clauses==null) clauses = new ArrayList<Clause>();

	    Statement statement = typeHandler.select(request,
						     Tables.ENTRIES.COLUMNS, clauses,
						     order);

	    ResultSet        results;
	    SqlUtil.Iterator iter = getDatabaseManager().getIterator(statement);

	    long             t1   = System.currentTimeMillis();
	    try {
		while ((results = iter.getNext()) != null) {
		    if ( !canDoSelectOffset && (skipCnt-- > 0)) {
			continue;
		    }
		    String id    = results.getString(1);
		    Entry  entry = getEntryFromCache(id);
		    if (entry == null) {
			//id,type,name,desc,group,user,file,createdata,fromdate,todate
			TypeHandler localTypeHandler =
			    getRepository().getTypeHandler(results.getString(2),
							   true);
			entry = localTypeHandler.createEntryFromDatabase(null,results);
			cacheEntry(entry);
		    }
		    if (seen.get(entry.getId()) != null) {
			continue;
		    }
		    seen.put(entry.getId(), BLANK);
		    allEntries.add(entry);
		}
		if(SearchManager.debugSearch)
		    getSearchManager().debug("DB Search:" + clauses+" result:" + allEntries.size());
	    } finally {
		long t2 = System.currentTimeMillis();
		if ((t2 - t1) > 60 * 1000) {
		    getLogManager().logError("Select took a long time:"
					     + (t2 - t1));
		}
		getDatabaseManager().closeAndReleaseConnection(statement);
	    }

	    SqlUtil.debug = false;
	    if(select.getOrderBy().equals("number")) {
		//TODO: sort
		//	    String pattern = "number:" + mtd[0].getAttr4();
	    }
	}





        //Only split them into groups and non-groups if we aren't doing an orderby
        if (!request.exists(ARG_ORDERBY)) {
	    String text = request.getString(ARG_TEXT);
	    //if there was a text search then put the entries whose names
	    //match the text up first
	    if(text!=null) {
		List<Entry> first = new ArrayList<Entry>();
		List<Entry> last = new ArrayList<Entry>();
		for(Entry entry: allEntries) {
		    String name = entry.getName();
		    if(name.regionMatches(true,0,text,0,text.length())) {
			first.add(entry);
		    } else {
			last.add(entry);
		    }
		}
		allEntries = new ArrayList<Entry>();
		allEntries.addAll(first);
		allEntries.addAll(last);
	    }

	    List<Entry> entries       = new ArrayList<Entry>();
	    entries.addAll(getEntryUtil().getGroups(allEntries));
	    entries.addAll(getEntryUtil().getNonGroups(allEntries));			   
	    allEntries = entries;
        }

	return getAccessManager().filterEntries(request, allEntries);
    }



    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public boolean isSynthEntry(String id) {
        return id.startsWith(ID_PREFIX_SYNTH);
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public String[] getSynthId(String id) {
        id = id.substring(ID_PREFIX_SYNTH.length());
        String[] pair = Utils.split(id, TypeHandler.ID_DELIMITER, 2);
        if (pair == null) {
            return new String[] { id, null };
        }

        return pair;
    }



    /**
     * _more_
     *
     * @param parentEntry _more_
     * @param subId _more_
     *
     * @return _more_
     */
    public String createSynthId(Entry parentEntry, String subId) {
        return Repository.ID_PREFIX_SYNTH + parentEntry.getId()
	    + TypeHandler.ID_DELIMITER + subId;
    }




    /**
     * _more_
     */
    public void clearSeenResources() {
        seenResources = new HashSet();
    }




    /** _more_ */
    private HashSet seenResources = new HashSet();



    /**
     * _more_
     *
     * @param harvester _more_
     * @param typeHandler _more_
     * @param entries _more_
     * @param makeThemUnique _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean processEntries(Harvester harvester,
                                  TypeHandler typeHandler,
                                  List<Entry> entries, boolean makeThemUnique)
	throws Exception {
        if (makeThemUnique) {
            entries = getUniqueEntries(entries);
        }
        addNewEntries(null, entries);

        return true;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param newFile _more_
     * @param group _more_
     * @param name _more_
     * @param user _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry addFileEntry(Request request, File newFile, Entry group,String pathTemplate,
                              String name, String desc, User user)
	throws Exception {
        return addFileEntry(request, newFile, group, pathTemplate, name, desc, user, null, null);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param newFile _more_
     * @param group _more_
     * @param name _more_
     * @param user _more_
     * @param typeHandler _more_
     * @param initializer _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry addFileEntry(Request request, File newFile, Entry parentEntry, String pathTemplate,
                              String name, String desc, User user,
                              TypeHandler typeHandler,
                              EntryInitializer initializer)
	throws Exception {

        String resourceType;

        //Is it a ramadda managed file?
        if (IO.isADescendent(getStorageManager().getRepositoryDir(),
			     newFile)) {
            resourceType = Resource.TYPE_STOREDFILE;
        } else {
            resourceType = Resource.TYPE_LOCAL_FILE;
        }


        Entry entry = makeEntry(request, new Resource(newFile, resourceType),
                                parentEntry, name, desc, user, typeHandler,
                                initializer);

	entry.setParentEntry(getPathEntry(request,parentEntry,entry,pathTemplate));
	List<Entry> entries = new ArrayList<Entry>();
	entries.add(entry);
	addInitialMetadata(request, entries, true, false);
        addNewEntry(request, entry);

        return entry;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param resource _more_
     * @param group _more_
     * @param name _more_
     * @param description _more_
     * @param user _more_
     * @param typeHandler _more_
     * @param initializer _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry makeEntry(Request request, Resource resource, Entry group,
                           String name, String description, User user,
                           TypeHandler typeHandler,
                           EntryInitializer initializer)
	throws Exception {


        if ( !getRepository().getAccessManager().canDoNew(request, group)) {
            throw new AccessException("Cannot add to folder", request);
        }

        if (typeHandler == null) {
            typeHandler =
                getRepository().getTypeHandler(TypeHandler.TYPE_FILE);
        }

        Entry entry = typeHandler.createEntry(getRepository().getGUID());


        Date  dttm  = new Date();
        initEntry(entry, name, description, group, request.getUser(), resource,
		  "", Entry.DEFAULT_ORDER, dttm.getTime(), dttm.getTime(),
		  dttm.getTime(), dttm.getTime(), null);
        if (initializer != null) {
            initializer.initEntry(entry);
        }

        return entry;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param file _more_
     * @param entry _more_
     * @param associatedEntry _more_
     * @param associationType _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntryPublish(Request request, File file,
                                      Entry entry, Entry associatedEntry,
                                      String associationType)
	throws Exception {
        Entry parent = findGroup(request,
				 request.getString(ARG_PUBLISH_ENTRY + "_hidden",
						   ""));
	Date now = new Date();


	entry.setCreateDate(now.getTime());
	entry.setChangeDate(now.getTime());	
        if (parent == null) {
            return new Result(
			      "",
			      new StringBuilder(
						getPageHandler().showDialogError(
										 msg("Could not find folder"))));
        }

        if ( !canAddTo(request, parent)) {
            throw new IllegalArgumentException(
					       "No permissions to add new entry");
        }

        File newFile =
            getRepository().getStorageManager().moveToStorage(request, file);

        TypeHandler typeHandler = findDefaultTypeHandler(newFile.toString());
        if (typeHandler == null) {
            typeHandler =
                getRepository().getTypeHandler(TypeHandler.TYPE_FILE);
        }

        Entry newEntry = ((entry != null)
                          ? entry
                          : new Entry(typeHandler, false));


        newEntry.setParentEntry(parent);
        newEntry.setResource(new Resource(newFile, Resource.TYPE_STOREDFILE));
        newEntry.setId(getRepository().getGUID());
        newEntry.setName(request.getString(ARG_PUBLISH_NAME,""));

        if ( !Utils.stringDefined(newEntry.getName())) {
	    newEntry.setName(StorageManager.getOriginalFilename(newFile.getName()));
        }
        newEntry.clearMetadata();
        newEntry.setUser(request.getUser());
        if (request.get(ARG_METADATA_ADD, false)) {
            newEntry.clearArea();
            List<Entry> entries = (List<Entry>) Misc.newList(newEntry);
            getEntryManager().addInitialMetadata(request, entries, false,
						 request.get(ARG_SHORT, false));
        }
        addNewEntry(request, newEntry);
        if ((associatedEntry != null)
	    && !isSynthEntry(associatedEntry.getId())) {
            getAuthManager().addAuthToken(request);
            getAssociationManager().addAssociation(request, associatedEntry,
						   newEntry, "", associationType);
        }

	parentageChanged(parent);
        return new Result(request.entryUrl(getRepository().URL_ENTRY_FORM,
                                           newEntry));

    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param columnSize _more_
     *
     * @throws IllegalArgumentException _more_
     */
    public void checkColumnSize(String name, String value, int columnSize)
	throws IllegalArgumentException {
        if (value.length() > columnSize) {
            throw new IllegalArgumentException(name + " size:"
					       + value.length() + " is greater than column size:"
					       + columnSize);
        }
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param statement _more_
     * @param isNew _more_
     * @param typeHandler _more_
     *
     * @throws Exception _more_
     */
    private void setStatement(Entry entry, PreparedStatement statement,
                              boolean isNew, TypeHandler typeHandler)
	throws Exception {
        String description = entry.getDescription();
        checkColumnSize("name", entry.getName(), Entry.MAX_NAME_LENGTH);
        checkColumnSize("description", description, Entry.MAX_DESCRIPTION_LENGTH);


        int col = 1;
	//Note: these have to be in the same order as Tables.ENTRIES
        //id,type,name,desc,group,user,file,category,order,createdata,fromdate,todate
        statement.setString(col++, entry.getId());
        statement.setString(col++, typeHandler.getType());

        statement.setString(col++, entry.getName());
        statement.setString(col++, description);
        statement.setString(col++, entry.getParentEntryId());
        //        statement.setString(col++, entry.getCollectionGroupId());
        statement.setString(col++, entry.getUserId());
        if (entry.getResource() == null) {
            entry.setResource(new Resource());
        }
        statement.setString(
			    col++,
			    getStorageManager().resourceToDB(entry.getResource().getPath()));
        statement.setString(col++, entry.getResource().getType());
        statement.setString(col++, entry.getResource().getMd5());
        statement.setLong(col++, entry.getResource().getFileSize());
        statement.setString(col++, entry.getCategory());
        statement.setInt(col++, entry.getEntryOrder());

        getDatabaseManager().setDate(statement, col++, entry.getCreateDate());
        long updateTime = entry.getChangeDate();
        getDatabaseManager().setDate(statement, col++, updateTime);
        try {
            getDatabaseManager().setDate(statement, col, entry.getStartDate());
            getDatabaseManager().setDate(statement, col + 1,  entry.getEndDate());
        } catch (Exception exc) {
            getLogManager().logError("Error: Bad date " + entry.getResource()
                                     + " " + new Date(entry.getStartDate()));
            getDatabaseManager().setDate(statement, col, new Date());
            getDatabaseManager().setDate(statement, col + 1, new Date());
        }
        col += 2;


        //This cleans  up bad double values
        getDatabaseManager().setDouble(statement, col++, entry.getSouth(),
                                       Entry.NONGEO);
        getDatabaseManager().setDouble(statement, col++, entry.getNorth(),
                                       Entry.NONGEO);
        getDatabaseManager().setDouble(statement, col++, entry.getEast(),
                                       Entry.NONGEO);
        getDatabaseManager().setDouble(statement, col++, entry.getWest(),
                                       Entry.NONGEO);
        getDatabaseManager().setDouble(statement, col++,
                                       entry.getAltitudeTop(), Entry.NONGEO);
        getDatabaseManager().setDouble(statement, col++,
                                       entry.getAltitudeBottom(),
                                       Entry.NONGEO);

        if ( !isNew) {
            statement.setString(col++, entry.getId());
        }
    }

    public void entryFileChanged(Request request, Entry entry) throws Exception {
	entry.setChangeDate(System.currentTimeMillis());
	File f = new File(entry.getResource().getPath());
	entry.getResource().setFileSize(f.length());
	updateEntry(request, entry);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void updateEntry(Request request, Entry entry) throws Exception {
        List<Entry> tmp = new ArrayList<Entry>();
        tmp.add(entry);
        updateEntries(request, tmp);
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_b
     */
    public void addNewEntry(Request request, Entry entry) throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);
        addNewEntries(request, entries);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @throws Exception _more_
     */
    public void addNewEntries(Request request, List<Entry> entries)
	throws Exception {
        addNewEntries(request, entries, false);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param fromImport _more_
     *
     * @throws Exception _more_
     */
    public void addNewEntries(Request request, List<Entry> entries,
                              boolean fromImport)
	throws Exception {
        insertEntries(request, entries, true, fromImport);
        for (Entry theNewEntry : entries) {
            theNewEntry.getTypeHandler().doFinalEntryInitialization(request,
								    theNewEntry, fromImport);
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @throws Exception _more_
     */
    public void updateEntries(Request request, List<Entry> entries)
	throws Exception {
	updateEntries(request, entries, true);
    }

    public void updateEntries(Request request, List<Entry> entries, boolean callCheckModified)
	throws Exception {
        insertEntries(request, entries, false, false, callCheckModified);
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entries _more_
     * @param isNew _more_
     * @param fromImport _more_
     *
     * @throws Exception _more_
     */
    public void insertEntries(Request request, final List<Entry> entries,
                               boolean isNew, boolean fromImport)
	throws Exception {
	insertEntries(request, entries, isNew, fromImport, true);
    }

    private void insertEntries(Request request, final List<Entry> entries,
                               boolean isNew, boolean fromImport,
			       boolean callCheckModified)
	throws Exception {

	if(request==null) request=getRepository().getAdminRequest();
        if (entries.size() == 0) {
            return;
        }

	for (Entry entry : entries) {
	    if(!entry.getTypeHandler().canCreate(request)) {
		throw new IllegalArgumentException("User cannot create entry of type:" + entry.getTypeHandler());
	    }
	    entry.getTypeHandler().initEntry(entry);
	}
	    
	boolean okToInsert = true;

        if (isNew) {
	    okToInsert= !request.exists(ARG_BULKUPLOAD);
            for (Entry theNewEntry : entries) {
                theNewEntry.getTypeHandler().initializeNewEntry(request,theNewEntry, fromImport);
                String name = theNewEntry.getName();
                if (name.trim().length() == 0) {
                    String nameTemplate =
                        theNewEntry.getTypeHandler().getTypeProperty(
								     "nameTemplate", (String) null);
                    if (nameTemplate != null) {
                        Object[] values =
                            theNewEntry.getTypeHandler().getEntryValues(
									theNewEntry);
                        SimpleDateFormat sdf =
                            RepositoryUtil.makeDateFormat("yyyy-MM-dd HH:mm");
                        nameTemplate = nameTemplate.replace(
							    "${date}",
							    sdf.format(new Date(theNewEntry.getStartDate())));
                        if (values != null) {
                            List<Column> columns =
                                theNewEntry.getTypeHandler().getColumns();
                            if (columns != null) {
                                for (Column c : columns) {
                                    Object obj = c.getObject(values);
                                    nameTemplate = nameTemplate.replace("${"
									+ c.getName()
									+ "}", obj.toString());
                                }
                            }
                        }
                        theNewEntry.setName(nameTemplate);
                    }
                }

            }
	}

	if(okToInsert)
	    insertEntriesIntoDatabase(request,  entries,isNew,callCheckModified);

    }

    public void insertEntriesIntoDatabase(Request request, final List<Entry> entries,
					  boolean isNew, boolean callCheckModified) throws Exception {
        //We have our own connection
        Connection connection = getDatabaseManager().getConnection();
        try {
            insertEntriesInner(request, entries, connection, isNew, callCheckModified);
        } finally {
            getDatabaseManager().closeConnection(connection);
        }

	//clear out the type count
	if(isNew)  {
	    getEntryUtil().clearCache();
	}
    }



    /**
     * _more_
     *
     * @param entries _more_
     * @param connection _more_
     * @param isNew _more_
     *
     * @throws Exception _more_
     */
    private void insertEntriesInner(final Request request, final List<Entry> entries,
                                    Connection connection, boolean isNew, boolean callCheckModified)
	throws Exception {

        if (entries.size() == 0) {
            return;
        }

        long              t1          = System.currentTimeMillis();
        int               cnt         = 0;
        int               metadataCnt = 0;
	PreparedStatement entryStmt   = connection.prepareStatement(isNew
								    ? Tables.ENTRIES.INSERT
								    : SqlUtil.makeUpdate(Tables.ENTRIES.NAME,
											 Tables.ENTRIES.COL_ID,
											 Tables.ENTRIES.ARRAY));
        PreparedStatement metadataStmt =
            connection.prepareStatement(Tables.METADATA.INSERT);


        Hashtable typeStatements = new Hashtable();
        int       batchCnt       = 0;
        connection.setAutoCommit(false);
        long updateTime = getRepository().currentTime();
        for (Entry entry : entries) {
            entry.clearTransientProperties();
            entry.getTypeHandler().clearCache();

            //Do we want to clear it from the cache???
            removeFromCache(entry);
            if ( !isNew) {
                entry.setChangeDate(updateTime);
            }

            TypeHandler          typeHandler = entry.getTypeHandler();
            List<TypeInsertInfo> typeInserts =
                new ArrayList<TypeInsertInfo>();
            typeHandler.getInsertSql(isNew, typeInserts);
            for (TypeInsertInfo tif : typeInserts) {
                PreparedStatement typeStatement =
                    (PreparedStatement) typeStatements.get(tif.getSql());
                if (typeStatement == null) {
                    typeStatement = connection.prepareStatement(tif.getSql());
                    typeStatements.put(tif.getSql(), typeStatement);
                }
                tif.setStatement(typeStatement);
            } 
	    setStatement(entry, entryStmt, isNew, typeHandler);


            batchCnt++;
            entryStmt.addBatch();

            for (TypeInsertInfo tif : typeInserts) {
                PreparedStatement typeStatement = tif.getStatement();
                batchCnt++;
                tif.getTypeHandler().setStatement(entry, typeStatement,
						  isNew);
                typeStatement.addBatch();
            }

            DatabaseManager dbm          = getDatabaseManager();

            List<Metadata>  metadataList = entry.getMetadata();
            if (metadataList != null) {
                if ( !isNew) {
                    dbm.delete(Tables.METADATA.NAME,
                               Clause.eq(Tables.METADATA.COL_ENTRY_ID,
                                         entry.getId()));
                }
                for (Metadata metadata : metadataList) {
		    if(metadata.getMarkedForDelete()) continue;
                    int col = 1;
                    metadataCnt++;
                    metadataStmt.setString(col++, metadata.getId());
                    metadataStmt.setString(col++, entry.getId());
                    metadataStmt.setString(col++, metadata.getType());
                    metadataStmt.setInt(col++, metadata.getInherited()
					? 1
					: 0);
                    String name = metadata.getType() + " " + metadata.getId();
                    dbm.setString(metadataStmt, col++, name,
                                  metadata.getAttr1(), Metadata.MAX_LENGTH);
                    dbm.setString(metadataStmt, col++, name,
                                  metadata.getAttr2(), Metadata.MAX_LENGTH);
                    dbm.setString(metadataStmt, col++, name,
                                  metadata.getAttr3(), Metadata.MAX_LENGTH);
                    dbm.setString(metadataStmt, col++, name,
                                  metadata.getAttr4(), Metadata.MAX_LENGTH);
                    dbm.setString(metadataStmt, col++, name,
                                  metadata.getExtra(),
                                  Metadata.MAX_LENGTH_EXTRA);
                    metadataStmt.addBatch();
                    batchCnt++;
                }
            }

            if (batchCnt > 1000) {
                //                    if(isNew)
                entryStmt.executeBatch();
                //                    else                        entryStmt.executeUpdate();
                if (metadataCnt > 0) {
                    metadataStmt.executeBatch();
                }
                for (Enumeration keys = typeStatements.keys();
		     keys.hasMoreElements(); ) {
                    PreparedStatement typeStatement =
                        (PreparedStatement) typeStatements.get(
							       keys.nextElement());
                    //                        if(isNew)
                    typeStatement.executeBatch();
                    //                        else                            typeStatement.executeUpdate();
                }
                batchCnt    = 0;
                metadataCnt = 0;
            }
        }
        if (batchCnt > 0) {
            entryStmt.executeBatch();
            metadataStmt.executeBatch();
            for (Enumeration keys = typeStatements.keys();
		 keys.hasMoreElements(); ) {
                Object key = keys.nextElement();
                PreparedStatement typeStatement =
                    (PreparedStatement) typeStatements.get(key);
                try {
                    typeStatement.executeBatch();
                } catch (Exception exc) {
                    System.err.println("ERROR: " + key);

                    throw exc;
                }
            }
        }
        connection.commit();
        connection.setAutoCommit(true);


        long t2 = System.currentTimeMillis();
        totalTime    += (t2 - t1);
        totalEntries += entries.size();
        for (Entry entry : entries) {
            Entry parentEntry = entry.getParentEntry();
            if (parentEntry != null) {
                parentEntry.getTypeHandler().childEntryChanged(entry, isNew);
            }
        }

        if (t2 > t1) {
            double seconds = totalTime / 1000.0;
            //            if ((totalEntries % 100 == 0) && (seconds > 0)) {
            if (seconds > 0) {
                //                System.err.println(totalEntries + " average rate:"
                //                 + (int) (totalEntries / seconds)
                //                 + "/second");
            }
        }

        getDatabaseManager().closeStatement(entryStmt);
        getDatabaseManager().closeStatement(metadataStmt);
        for (Enumeration keys =
		 typeStatements.keys(); keys.hasMoreElements(); ) {
            PreparedStatement typeStatement =
                (PreparedStatement) typeStatements.get(keys.nextElement());
            getDatabaseManager().closeStatement(typeStatement);
        }


	if(callCheckModified) {
	    if(isNew) getRepository().checkNewEntries(request,entries);
	    else getRepository().checkModifiedEntries(request,entries);
	}

    }





    /** _more_ */
    long totalTime = 0;

    /** _more_ */
    int totalEntries = 0;



    /**
     * _more_
     *
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> getUniqueEntries(List<Entry> entries)
	throws Exception {
        return getUniqueEntries(entries, new ArrayList<Entry>());
    }

    /**
     * _more_
     *
     * @param entries _more_
     * @param nonUniqueOnes _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */

    public List<Entry> getUniqueEntries(List<Entry> entries,
                                        List<Entry> nonUniqueOnes)
	throws Exception {
        List<Entry> needToAdd = new ArrayList();
        String      query     = BLANK;
        try {
            if (entries.size() == 0) {
                return needToAdd;
            }
            if (seenResources.size() > 10000) {
                seenResources = new HashSet();
            }
            Connection connection = getDatabaseManager().getConnection();
            //FOR NOW:  Try using the name for uniqueness instead of the resource
            PreparedStatement selectbak =
                SqlUtil.getSelectStatement(
					   connection, "count(" + Tables.ENTRIES.COL_ID + ")",
					   Misc.newList(Tables.ENTRIES.NAME), Clause.and(
											 //                        Clause.eq(Tables.ENTRIES.COL_RESOURCE, ""),
											 Clause.eq(Tables.ENTRIES.COL_NAME,
												   ""), Clause.eq(Tables.ENTRIES.COL_PARENT_GROUP_ID,
														  "?")), "");

            PreparedStatement select = SqlUtil.getSelectStatement(connection,
								  Tables.ENTRIES.COL_ID,
								  Misc.newList(Tables.ENTRIES.NAME),
								  Clause.and(
									     //                        Clause.eq(Tables.ENTRIES.COL_RESOURCE, ""),
									     Clause.eq(Tables.ENTRIES.COL_NAME,
										       ""), Clause.eq(Tables.ENTRIES.COL_PARENT_GROUP_ID,
												      "?")), "");
            long t1 = System.currentTimeMillis();
            for (Entry entry : entries) {
                //                String path = getStorageManager().resourceToDB(entry.getResource().getPath());
                String path        = entry.getName();
                Entry  parentEntry = entry.getParentEntry();
                if (parentEntry == null) {
                    needToAdd.add(entry);

                    continue;
                }
                String key = parentEntry.getId() + "_" + path;
                if (seenResources.contains(key)) {
                    nonUniqueOnes.add(entry);

                    continue;
                }
                seenResources.add(key);

                select.setString(1, path);
                select.setString(2, entry.getParentEntry().getId());
                ResultSet results = select.executeQuery();
                if (results.next()) {
                    String id = results.getString(1);
                    if (id == null) {
                        needToAdd.add(entry);
                    } else {
                        entry.putTransientProperty("existingEntryId", id);
                        nonUniqueOnes.add(entry);
                    }
                    /*
		      int found = results.getInt(1);
		      if (found == 0) {
		      needToAdd.add(entry);
		      } else {
		      nonUniqueOnes.add(entry);
		      }*/
                } else {
                    needToAdd.add(entry);
                }

            }
            getDatabaseManager().closeStatement(select);
            getDatabaseManager().closeConnection(connection);
            long t2 = System.currentTimeMillis();
        } catch (Exception exc) {
            logError("Processing:" + query, exc);

            throw exc;
        }

        return needToAdd;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public String getEntryResourceUrl(Request request, Entry entry) {
        return getEntryResourceUrl(request, entry, ARG_INLINE_DFLT,ARG_FULL_DFLT,ARG_ADDPATH_DFLT,false);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param full _more_
     * @param addPath _more_
     *
     * @return _more_
     */
    public String getEntryResourceUrl(Request request, Entry entry,boolean inline,
				      boolean full, boolean addPath) {

	return getEntryResourceUrl(request, entry, inline,full, addPath, false);
    }

    public String getEntryResourceUrl(Request request, Entry entry,boolean inline,
				      boolean full, boolean addPath, boolean proxyIfNeeded) { 	


        if (!proxyIfNeeded && entry.getResource().isUrl()) {
            try {
                return entry.getTypeHandler().getPathForEntry(request, entry,false);
            } catch (Exception exc) {
                return "Error:" + exc;
            }
        }

        String fileTail = getStorageManager().getFileTail(entry);
        fileTail = HU.urlEncodeExceptSpace(fileTail);
        //For now use the full entry path ???? why though ???
        if (addPath && fileTail.equals(entry.getName())) {
            fileTail = entry.getFullName(true);
        }
	String base;
        if (request.getMakeAbsoluteUrls() || full) {
            base = getFullEntryGetUrl(request) + "/" + fileTail;
        } else {
	    base = request.makeUrl(getRepository().URL_ENTRY_GET) + "/" + fileTail;
        }
	if(inline !=ARG_INLINE_DFLT)
	    base =  HU.url(base, ARG_ENTRYID, entry.getId(),"fileinline",""+inline);
	else
	    base =  HU.url(base, ARG_ENTRYID, entry.getId());
	return base;
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getDummyGroup() throws Exception {
        return getDummyGroup("Results");
    }

    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getDummyGroup(String name) throws Exception {
        Entry dummyGroup = new Entry(getRepository().getTypeHandler("type_dummy"),
                                     true, name);
        dummyGroup.setId(getRepository().getGUID());
        dummyGroup.setUser(getUserManager().getAnonymousUser());

        return dummyGroup;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> getChildrenGroups(Request request, Entry group)
	throws Exception {
        List<Entry> result = new ArrayList<Entry>();
        if ( !group.isGroup()) {
            return result;
        }
        for (Entry entry : getChildren(request, group)) {
            if (entry.isGroup()) {
                result.add(entry);
            }
        }

        return result;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> getChildrenEntries(Request request, Entry group)
	throws Exception {
        List<Entry> result = new ArrayList<Entry>();
        if ( !group.isGroup()) {
            return result;
        }
        for (Entry entry : getChildren(request, group)) {
            if ( !entry.isGroup()) {
                result.add(entry);
            }
        }

        return result;
    }



    /**
     * _more_
     *
     * @param entries _more_
     * @param entry _more_
     * @param flag _more_
     * @param orNot _more_
     */
    public void orNot(List<Entry> entries, Entry entry, boolean flag,
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
    public List<Entry> applyFilter(Request request, Entry entry, SelectInfo select)
	throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);
	if(select==null) return entries;
        return applyFilter(request, entries, select);
    }

    public List<Entry> applyFilter(Request request, Entry entry, String filter) 
	throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);
        return applyFilter(request, entries, filter);
    }    


    public List<Entry> applyFilter(Request request, List<Entry> entries, SelectInfo select)
	throws Exception {
	if(select==null) return entries;
	if(select.getFilter()!=null) {
	    entries =  applyFilter(request, entries, select.getFilter());
	}
	if(stringDefined(select.getType())) {
	    List<Entry> tmp  = new ArrayList<Entry>();
	    List<String> types = Utils.split(select.getType(),",",true,true);
	    for(Entry entry: entries) {
		boolean ok  = false;
		for(String type: types) {
		    if(entry.getTypeHandler().isType(type)) {
			ok = true;
			break;
		    }
		}
		if(ok)
		    tmp.add(entry);
	    }
	    entries= tmp;
	}


	//	System.err.println("date:" + fromDate +" " + toDate);
	if(Utils.stringDefined(select.getFromDate()) || Utils.stringDefined(select.getToDate())) {
	    Date[] range = Utils.getDateRange(select.getFromDate(), select.getToDate(), new Date());
	    List<Entry> tmp = new ArrayList<Entry>();
	    for(Entry e: entries) {
		if(range[0]!=null&& e.getStartDate()<range[0].getTime()) {
		    continue;
		}
		if(range[1]!=null && e.getEndDate()>range[1].getTime()) {
		    continue;
		}
		tmp.add(e);
	    }
	    entries=tmp;
	}



	if(select.getOrderBy()!=null) {
	    entries= EntryUtil.sortEntriesOn(entries,select.getOrderBy(),!select.getAscending());
	}

	if(select.getMax()>=0 && entries.size()>select.getMax()) {
            List<Entry> tmp = new ArrayList<Entry>();
	    for(int i=0;i<entries.size() && i<select.getMax();i++)
		tmp.add(entries.get(i));
	    entries=tmp;
	}



	return entries;
    }

    /**
     * _more_
     *
     * @param request the request
     * @param entries _more_
     * @param filter _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> applyFilter(Request request, List<Entry> entries, String filter)
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
            entries = getImageEntriesOrNot(request, entries, doNot, false);
	} else    if (filter.equals(FILTER_IMAGE_OR_ATTACHMENT)) {
            entries = getImageEntriesOrNot(request, entries, doNot, true);	    
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
                Utils.split(filter.substring(FILTER_TYPE.length()), ",",
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
     * _more_
     *
     * @param request _more_
     * @param parentEntry _more_
     * @param select _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> getChildrenAll(Request request, Entry parentEntry,
                                      SelectInfo select)
	throws Exception {
        List<Entry> children = new ArrayList<Entry>();
        if ( !parentEntry.isGroup()) {
            return children;
        }
        List<Entry>  entries      = new ArrayList<Entry>();
        List<String> ids          = getChildIds(request, parentEntry, select);
        boolean      doingOrderBy = request.exists(ARG_ORDERBY);
        for (String id : ids) {
            Entry entry = getEntry(request, id);
            if (entry == null) {
                continue;
            }
            if (doingOrderBy) {
                children.add(entry);
            } else {
                if (entry.isGroup()) {
                    children.add(entry);
                } else {
                    entries.add(entry);
                }
            }
        }
        children.addAll(entries);
	children =applyFilter(request,children,select);


        return parentEntry.getTypeHandler().postProcessEntries(request,
							       children);
    }


    /**
       Get the children entries. If there is an error then log it and return an empty list
       We have this because the getChildren call can throw an error (e.g., Bad
       AWS S3 credentials) and this can block critical user things (menus, etc);
    */
    public List<Entry> getChildrenSafe(Request request, Entry parentEntry) {
	try {
	    return getChildren(request, parentEntry);
	} catch(Exception exc) {
	    logError("Error getting chilldren:" + parentEntry,exc);
	    return new ArrayList<Entry>();
	}
    }


    public List<Entry> getChildren(Request request, SelectInfo select) throws Exception {
	List<Entry> children =  getChildren(select.getRequest(), select.getEntry());
	return applyFilter(request,children,select);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param parentEntry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> getChildren(Request request, Entry parentEntry, SelectInfo...selects)
	throws Exception {
        List<Entry> children = new ArrayList<Entry>();
	if(parentEntry==null) {
	    throw new IllegalArgumentException("No parent entry given");
	}
        if ( !parentEntry.isGroup()) {
            return children;
        }
	SelectInfo select = selects.length>0?selects[0]:new SelectInfo(request, parentEntry);
        List<Entry>  entries      = new ArrayList<Entry>();
        List<String> ids          = getChildIds(request, parentEntry, select);
        boolean      doingOrderBy = request.exists(ARG_ORDERBY);
        for (String id : ids) {
            Entry entry = getEntry(request, id);
            if (entry == null) {
                continue;
            }
            if ( !doingOrderBy) {
                if (entry.isGroup()) {
                    children.add(entry);
                } else {
                    entries.add(entry);
                }
            } else {
                children.add(entry);
            }
        }
        children.addAll(entries);


	//If it is a synth entry then it hasn't been sorted so check if there was a sort
	boolean isSynthEntry = isSynthEntry(parentEntry.getId());
	if (parentEntry.getTypeHandler().isSynthType() || isSynthEntry) {
	    //init in case it hasn't
	    select.init();
	    if(select.getHadOrderBy()) {
		children = EntryUtil.sortEntriesOn(children, select.getOrderBy(),!select.getAscending());
	    }
	}


	if(request.getString(ARG_ORDERBY,"").equals(ORDERBY_NUMBER)) {
	    children = EntryUtil.sortEntriesOnNumber(children, !request.get(ARG_ASCENDING,true));
	}

	children =applyFilter(request,children,select);
        return parentEntry.getTypeHandler().postProcessEntries(request,
							       children);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param select _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<String> getChildIds(Request request, Entry group,
                                    SelectInfo select)
	throws Exception {

	if(select==null) select = new SelectInfo(request, group);

        boolean isSynthEntry = isSynthEntry(group.getId());
        if (group.getTypeHandler().isSynthType() || isSynthEntry) {
            List<String> ids       = new ArrayList<String>();
	    if(!select.getSyntheticOk()) return ids;

            Entry        mainEntry = group;
            String       synthId   = null;
            if (isSynthEntry) {
                String[] pair    = getSynthId(mainEntry.getId());
                String   entryId = pair[0];
                synthId = pair[1];
                TypeHandler synthTypeHandler = getSynthTypeHandler(entryId);
                Entry       tmpMainEntry     = null;
                if (synthTypeHandler != null) {
                    tmpMainEntry = synthTypeHandler.getSynthTopLevelEntry();
                } else {
                    tmpMainEntry = (Entry) getEntry(request, entryId, false,
						    false);
                }
                if (tmpMainEntry != null) {
                    mainEntry = tmpMainEntry;
                }
                if (mainEntry == null) {
                    return ids;
                }
	    }

            //            System.err.println("****  Get synthids:" + mainEntry.getTypeHandler().getSynthIds(request, mainEntry,
            //                                                                                              group, synthId));
            try {
                return mainEntry.getMasterTypeHandler().getSynthIds(request,select,
								    mainEntry, group, synthId);
            } catch (Exception exc) {
                getLogManager().logError("Error getting synthIds from:"
                                         + mainEntry, exc);
		throw exc;
            }
        }

        return getChildIdsFromDatabase(request, group, select);
    }



    public void parentageChanged(List<Entry> entries) {
	for(Entry entry: entries) parentageChanged(entry.getParentEntry());
    }

    public void parentageChanged(Entry parent) {
	if(parent!=null) {
	    childrenCache.remove(parent.getId());
	    parent.getTypeHandler().childrenChanged(parent);
	}
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param select _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<String> getChildIdsFromDatabase(Request request, Entry group,
						SelectInfo select)
	throws Exception {
        List<Clause> where = ((select == null)
                              ? null
                              : select.getWhere());

	boolean debug = false;
	String debugName = request.getString("debug",null);
	if(debugName!=null && group.getName().indexOf(debugName)>=0) debug = true;
	boolean canCache = where==null || where.size()==0;
	boolean addToCache = false;
	String orderBy = getQueryOrderAndLimit(request, true, group, select);
	boolean canDoSelectOffset = getDatabaseManager().canDoSelectOffset();
	int         skipCnt     = request.get(ARG_SKIP, 0);
	List<String> allIds=null;
	String cacheKey=null;
	Hashtable<String,List<String>> cache = null;
	if(canCache) {
	    //2 level cache
	    cache = childrenCache.get(group.getId());
	    if(cache==null) {
		cache= new Hashtable<String,List<String>>();
		childrenCache.put(group.getId(), cache);
	    }
	    cacheKey = Utils.makeCacheKey(group.getChangeDate(),orderBy);
	    //	    if(debug)getLogManager().logSpecial("getChildIds: cacheKey:" + cacheKey);
	    //Check the children cache when there is no clause
	    allIds   = cache.get(cacheKey);
	    if(allIds!=null && debug)
		getLogManager().logSpecial("getChildIds:" + group.getName()+" from cache:" + Utils.clip(allIds,10,5,"..."));
	}
	if(allIds==null)  {
	    where = where!=null?where = new ArrayList<Clause>(where):new ArrayList<Clause>();
	    where.add(Clause.eq(Tables.ENTRIES.COL_PARENT_GROUP_ID,group.getId()));
	    TypeHandler typeHandler = getRepository().getTypeHandler(request);
	    Statement statement = typeHandler.select(request,
						     Tables.ENTRIES.COL_ID, where, orderBy);
	    SqlUtil.Iterator iter = getDatabaseManager().getIterator(statement);
	    ResultSet        results;
	    allIds   = new ArrayList<String>();
	    while ((results = iter.getNext()) != null) {
		allIds.add(results.getString(1));
	    }
	    if(cacheKey!=null && cache!=null) {
		if(debug)
		    getLogManager().logSpecial("getChildIds:" + group.getName() +" caching ids:" + Utils.clip(allIds,10,5,"..."));
		cache.put(cacheKey, allIds);
	    } else {
		if(debug) getLogManager().logSpecial("getChildIds:" + group.getName() +" not caching:" + Utils.clip(allIds,10,5,"..."));
	    }
	}
	List<String>  ids   = new ArrayList<String>();
	for(String id: allIds) {
            if ( !canDoSelectOffset && (skipCnt-- > 0)) {
                continue;
            }
            ids.add(id);
        }
        return ids;
    }










    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result publishEntries(Request request, List<Entry> entries)
	throws Exception {
        StringBuilder sb               = new StringBuilder();
        List<Entry>   publishedEntries = new ArrayList<Entry>();
        boolean       didone           = false;
        for (Entry entry : entries) {
            if ( !getAccessManager().canDoEdit(request, entry)) {
                continue;
            }

            if ( !isAnonymousUpload(entry)) {
                continue;
            }
            publishAnonymousEntry(request, entry);
            publishedEntries.add(entry);
            if ( !didone) {
                sb.append(msgHeader("Published Entries"));
                didone = true;
            }
            sb.append(
		      HU.href(
			      request.entryUrl(getRepository().URL_ENTRY_SHOW, entry),
			      getEntryDisplayName(entry)));
            sb.append(HU.br());
        }
        if ( !didone) {
            sb.append(
		      getPageHandler().showDialogNote(
						      msg("No entries to publish")));
        }
        updateEntries(request, publishedEntries);

        return new Result("Publish Entries", sb);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param shortForm _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result addInitialMetadataToEntries(Request request,
					      List<Entry> entries, boolean shortForm)
	throws Exception {
        StringBuilder sb = new StringBuilder();
        List<Entry> changedEntries = addInitialMetadata(request, entries,
							false, shortForm);
        if (changedEntries.size() == 0) {
            sb.append("No metadata added");
        } else {
            sb.append(changedEntries.size() + " "+ "entries changed");
            updateEntries(request, changedEntries);
        }
        if (entries.size() > 0) {
            return new Result(
			      request.entryUrl(
					       getRepository().URL_ENTRY_SHOW,
					       entries.get(0).getParentEntry(), ARG_MESSAGE,
					       sb.toString()));
        }

        return new Result("Metadata", sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param newEntries _more_
     * @param shortForm _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public List<Entry> addInitialMetadata(Request request,
                                          List<Entry> entries,
                                          boolean newEntries,
                                          boolean shortForm)
	throws Exception {
        List<Entry> changedEntries = new ArrayList<Entry>();
        for (Entry theEntry : entries) {
            if ( !newEntries
		 && !getAccessManager().canDoEdit(request, theEntry)) {
                continue;
            }

            Hashtable extra = new Hashtable();
            getMetadataManager().getMetadata(request,theEntry);
	    long t1= System.currentTimeMillis();
            boolean changed =
                getMetadataManager().addInitialMetadata(request, theEntry,
							extra, shortForm);
	    long t2= System.currentTimeMillis();
	    //	    System.err.println("addMetadata:" + theEntry+" time:" + (t2-t1));
            if ( !theEntry.hasAreaDefined()
		 && (extra.get(ARG_MINLAT) != null)) {
                theEntry.setSouth(Misc.getProperty(extra, ARG_MINLAT, 0.0));
                theEntry.setNorth(Misc.getProperty(extra, ARG_MAXLAT, 0.0));
                theEntry.setWest(Misc.getProperty(extra, ARG_MINLON, 0.0));
                theEntry.setEast(Misc.getProperty(extra, ARG_MAXLON, 0.0));
                theEntry.normalizeLongitude();
                theEntry.trimAreaResolution();

                changed = true;
            }
            Date startDate = (Date) extra.get(ARG_FROMDATE);
            Date endDate   = (Date) extra.get(ARG_TODATE);
            if (startDate != null) {
                theEntry.setStartDate(startDate.getTime());
                if (endDate == null) {
                    endDate = startDate;
                }
                theEntry.setEndDate(endDate.getTime());
                changed = true;
            }
            if (changed) {
                changedEntries.add(theEntry);
            }
        }

        return changedEntries;
    }


    /**
     * _more_
     *
     * @param xmlFile _more_
     * @param internal _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> parseEntryXml(File xmlFile, INTERNAL isInternal, TEMPLATE isTemplate,
				     Hashtable<String,Entry> entriesMap,
				     Hashtable<String,File> filesMap)
	throws Exception {

	StringBuilder msg = new StringBuilder();
	if(entriesMap == null) entriesMap = new Hashtable<String,Entry> ();
        Element root =
            XmlUtil.getRoot(getStorageManager().readSystemResource(xmlFile));


        List<Element> associationNodes = new ArrayList<Element>();
        associationNodes.addAll((List<Element>) XmlUtil.findDescendants(root,
									TAG_ASSOCIATION));

        if (root.getTagName().equals(TAG_ENTRIES)) {
            //Look for the child entry
            Element child = XmlUtil.findChild(root, TAG_ENTRY);
            if (child == null) {
                throw new IllegalArgumentException(
						   "Could not find entry xml definition in:" + xmlFile);
            }
            root = child;

        }


	if(filesMap==null)filesMap = new Hashtable<String, File>();
        filesMap.put("root", xmlFile.getParentFile());
        List<Entry> entryList =
            createEntryFromXml(
			       new Request(
					   getRepository(),
					   getUserManager().getDefaultUser()), root,
			       entriesMap, filesMap, false, isTemplate,isInternal,msg);

        if (isInternal == INTERNAL.YES) {
            for (Element assNode : associationNodes) {
                String fromId = XmlUtil.getAttribute(assNode, ATTR_FROM);
                String toId   = XmlUtil.getAttribute(assNode, ATTR_TO);
                //                if(fromId.equals("this")) fromId  = entry.getId();
                //                if(toId.equals("this")) toId  = entry.getId();
                for (Entry entry : entryList) {
                    entry.addAssociation(
					 new Association(
							 getRepository().getGUID(),
							 XmlUtil.getAttribute(assNode, ATTR_NAME, ""),
							 XmlUtil.getAttribute(assNode, ATTR_TYPE, ""),
							 fromId, toId));
                }
            }
        }


        return entryList;
    }




    /**
     *  This writes
     *
     * @param request _more_
     * @param entry _more_
     * @param file _more_
     *
     * @throws Exception _more_
     */
    public void writeEntryXmlFile(Request request, Entry entry, File file)
	throws Exception {
        IOUtil.writeFile(
			 file,
			 getRepository().getXmlOutputHandler().getEntryXml(
									   request, entry));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void writeEntryXmlFile(Request request, Entry entry)
	throws Exception {
        writeEntryXmlFile(request, entry, getEntryXmlFile(entry));
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public File getEntryXmlFile(Entry entry) throws Exception {
        return getEntryXmlFile(entry.getFile());
    }


    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public File getEntryXmlFile(File file) throws Exception {
        File   parent  = file.getParentFile();
        String name    = file.getName();
        String newName = "." + name + ".ramadda.xml";

        return new File(IOUtil.joinDir(parent, newName));
    }

    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getTemplateEntry(File file,Hashtable<String,Entry> entriesMap,
				  Hashtable<String,File> filesMap) throws Exception {
        try {
	    if(file==null) return null;
            Entry entry = getTemplateEntryInner(file, entriesMap,filesMap);

            return entry;
        } catch (Exception exc) {
            getLogManager().logError(
				     "Error creating template entry from file:" + file);

            throw exc;
        }
    }

    private Entry getTemplateEntryInner(File file,Hashtable<String,Entry> entriesMap,Hashtable<String,File> filesMap)
	throws Exception {
        File    parent      = file.getParentFile();
        boolean isDirectory = file.isDirectory();
        String  type        = (isDirectory
                               ? "dir"
                               : "file");
        String  filename    = file.getName();
        String[] names = { "." + filename + ".ramadda.xml",
                           "." + type + ".ramadda.xml", ".ramadda.xml", };


        for (String name : names) {
            File f = new File(IOUtil.joinDir(parent, name));
            if (f.exists()) {
                return parseEntryXml(f, INTERNAL.NO, TEMPLATE.YES,entriesMap,filesMap).get(0);
            }
        }

        if (isDirectory) {
            File f = new File(IOUtil.joinDir(file, ".this.ramadda.xml"));
            if (f.exists()) {
                Entry entry = parseEntryXml(f, INTERNAL.NO,TEMPLATE.YES,entriesMap,filesMap).get(0);
                return entry;
            }

        }

        return null;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param s _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getEntryText(Request request, Entry entry, String s)
	throws Exception {
        //<attachment name>
        if (s.indexOf("<attachment") >= 0) {
            List<Association> associations =
                getAssociationManager().getAssociations(request, entry);
            for (Association association : associations) {
                if ( !association.getFromId().equals(entry.getId())) {
                    continue;
                }
            }
        }

        return s;
    }




    /**
     * _more_
     *
     *
     * @param request _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry findGroup(Request request, String id) throws Exception {
        if ((id == null) || (id.length() == 0)) {
            return null;
        }
        Entry group = getEntryFromCache(id);
        if (group != null) {
            return group;
        }

        if (isSynthEntry(id) || id.startsWith("catalog:")) {
            return (Entry) getEntry(request, id);
        }

	if(true)
	    return getEntry(request, id);


        Statement statement =
            getDatabaseManager().select(Tables.ENTRIES.COLUMNS,
                                        Tables.ENTRIES.NAME,
                                        Clause.eq(Tables.ENTRIES.COL_ID, id));

        List<Entry> groups = readEntries(statement);
        for (Entry entry : groups) {
            if (entry.isGroup()) {
                return entry;
            }
        }

        return null;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param name _more_
     * @param user _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry findGroupUnder(Request request, Entry group, String name,
                                User user)
	throws Exception {
	return findGroupUnder(request, group, name, user, false);
    }	

    public Entry findGroupUnder(Request request, Entry group, String name,
                                User user, boolean testNew)
	throws Exception {	
        List<String> toks = (List<String>) Utils.split(name,
						       Entry.PATHDELIMITER, true, true);

        for (String tok : toks) {
            Entry theChild = null;
            for (Entry child : getChildrenGroups(request, group)) {
                if (child.isGroup() && child.getName().equals(tok)) {
                    theChild = (Entry) child;

                    break;
                }
            }
            if (theChild == null) {
		if(!testNew) {
		    theChild = makeNewGroup(request,group, tok, user);
		} else {
		    theChild =  new Entry(getRepository().getGroupTypeHandler(),true, tok);
		}		    
            }
            group = theChild;
        }
        return group;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param parent _more_
     * @param path _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry findEntryFromPath(Request request, Entry parent, String path)
	throws Exception {
        Entry currentEntry = parent;
        List<String> toks = Utils.split(path, Entry.PATHDELIMITER, true,
					true);
        for (int i = 0; i < toks.size(); i++) {
            if (currentEntry.getTypeHandler().isSynthType()) {
                List<String> subset = toks.subList(i, toks.size());
                if (subset.size() == 0) {
                    break;
                }
                return currentEntry.getTypeHandler().makeSynthEntry(request,
								    currentEntry, subset);
            }

            String name       = toks.get(i);
            Entry  childEntry = findEntryWithName(request, currentEntry,
						  name);
            //Try to decode any slashes
            if (childEntry == null) {
                name       = name.replaceAll("%2F", "/");
                childEntry = findEntryWithName(request, currentEntry, name);
            }
            if (childEntry == null) {
                return null;
            }

            currentEntry = childEntry;
        }

        return currentEntry;
    }





    /**
     * _more_
     *
     *
     * @param request _more_
     * @param parent _more_
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry findEntryWithName(Request request, Entry parent, String name)
	throws Exception {

        boolean isSynthEntry = isSynthEntry(parent.getId());
        if (parent.getTypeHandler().isSynthType() || isSynthEntry) {
            List<Entry> entries = getChildrenAll(request, parent, null);
            for (Entry entry : entries) {
                if (entry.getName().equals(name)) {
                    return entry;
                }
            }
        }


	//Check if the name is an ID
	Entry entry =   getEntry(request, name);
	if(entry!=null) return entry;

        String fullPath = ((parent == null)
                           ? ""
                           : parent.getFullName()) + Entry.IDDELIMITER + name;

        Entry  group    = getEntryFromCache(fullPath, false);
        if (group != null) {
            return group;
        }
        String[] ids = findEntryIdsWithName(request, parent, name);
        if (ids.length == 0) {
            return null;
        }

        return getEntry(request, ids[0], false);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param parent _more_
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> findEntriesWithName(Request request, Entry parent,
                                           String name)
	throws Exception {
        List<Entry> entries  = new ArrayList<Entry>();
        String      fullPath = ((parent == null)
                                ? ""
                                : parent.getFullName()) + Entry.IDDELIMITER
	    + name;



        Entry       group    = getEntryFromCache(fullPath, false);
        if (group != null) {
            entries.add(group);

            return entries;
        }
        String[] ids = findEntryIdsWithName(request, parent, name);
        if (ids.length == 0) {
            return entries;
        }
        for (String id : ids) {
            entries.add(getEntry(request, id, false));
        }

        return entries;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param parent _more_
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] findEntryIdsWithName(Request request, Entry parent,
                                         String name)
	throws Exception {
        Clause clause = null;
        Clause nameClause;
        if (name.indexOf("%") >= 0) {
            nameClause = Clause.like(Tables.ENTRIES.COL_NAME, name);
        } else {
            nameClause = Clause.eq(Tables.ENTRIES.COL_NAME, name);
        }


        String[] ids =
            SqlUtil.readString(
			       getDatabaseManager().getIterator(
								getDatabaseManager().select(
											    Tables.ENTRIES.COL_ID, Tables.ENTRIES.NAME,
											    clause =
											    Clause.and(
												       Clause.eq(
														 Tables.ENTRIES.COL_PARENT_GROUP_ID,
														 parent.getId()), nameClause))));


        return ids;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param parent _more_
     * @param resource _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] findEntryIdsWithResource(Request request, Entry parent,
                                             String resource)
	throws Exception {
        String[] ids =
            SqlUtil.readString(
			       getDatabaseManager().getIterator(
								getDatabaseManager().select(
											    Tables.ENTRIES.COL_ID, Tables.ENTRIES.NAME,
											    Clause.and(
												       Clause.eq(
														 Tables.ENTRIES.COL_PARENT_GROUP_ID,
														 parent.getId()), Clause.eq(
																	    Tables.ENTRIES.COL_RESOURCE,
																	    resource)))));

        return ids;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param parent _more_
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> findDescendants(Request request, Entry parent,
                                       String name)
	throws Exception {
        List<String> toks = (List<String>) Utils.split(name,
						       Entry.PATHDELIMITER, true, true);

        List<Entry> parents = new ArrayList<Entry>();
        parents.add(parent);
        for (int i = 0; i < toks.size(); i++) {
            String      tok     = toks.get(i);
            List<Entry> matched = new ArrayList<Entry>();
            for (Entry p : parents) {
                if ( !p.isGroup()) {
                    continue;
                }
                for (Entry child : getChildren(request, p)) {
                    String childName = child.getName();

                    if (childName.equals(tok)) {
                        matched.add(child);
                    } else if (StringUtil.stringMatch(childName, tok, false,
						      true)) {
                        matched.add(child);
                    } else {
                        if (child.isFile()) {
                            childName =
                                getStorageManager().getFileTail(child);
                            if (childName.equals(tok)) {
                                matched.add(child);
                            } else if (StringUtil.stringMatch(childName, tok,
							      false, true)) {
                                matched.add(child);
                            }
                        }
                    }
                }
            }
            if (i == toks.size() - 1) {
                return matched;
            }
            parents = matched;
        }

        return null;
    }


    /**
     *
     * @param request _more_
     * @param name _more_
     * @param user _more_
     * @param createIfNeeded _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry findEntryFromName(Request request, Entry baseEntry, String name)
	throws Exception {
        return findEntryFromName(request, baseEntry, name, false, null,null,null);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param name _more_
     * @param createIfNeeded _more_
     * @param lastGroupType _more_
     * @param templateEntry _more_
     * @param initializer _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry findEntryFromName(Request request, Entry baseEntry, String name, 
                                   boolean createIfNeeded,
                                   String lastGroupType, Entry templateEntry,
                                   EntryInitializer initializer)
	throws Exception {
	boolean debug = false;
	if(debug)
	    System.err.println("Find entry: " + "base entry:" + baseEntry +" name:" + name);	

        if (name == null) {
            return null;
        }
        name = name.trim();
        if (name.startsWith(Entry.PATHDELIMITER)) {
            name = name.substring(1);
        }
	boolean haveBase = baseEntry!=null;
	if(!haveBase) {
	    baseEntry    = request.getRootEntry();
	}

        String topEntryName = baseEntry.getName();
        if (name.equals("") || name.equals(topEntryName)) {
            return baseEntry;
        }
        //Tack on the top group name if its not there
	//        if ( !name.startsWith(topEntryName + Entry.PATHDELIMITER)) {
	//            name = topEntryName + Entry.PATHDELIMITER + name;
	//        }

	//It might just be an ID?
	if(!haveBase && name.indexOf(Entry.PATHDELIMITER)<0) {
	    return null;
	}
        //split the list
        List<String> toks = (List<String>) Utils.split(name,
						       Entry.PATHDELIMITER, true, true);

	//	debug = true;
	if(debug)
	    System.err.println("\tname:" + name);
	if(debug)
	    System.err.println("\ttoks:" + toks);
        //Now remove the top group if we didn't have a root entry passed in
	if(!haveBase) {
	    toks.remove(0);
	    if(debug)
		System.err.println("\tremoving first tok:" + toks);
	}

        Entry currentEntry = baseEntry;

        if (lastGroupType == null) {
            lastGroupType = TypeHandler.TYPE_GROUP;
        }
        String groupType = TypeHandler.TYPE_GROUP;

        for (int i = 0; i < toks.size(); i++) {
            boolean      lastOne   = (i == toks.size() - 1);
            String       childName = Entry.decodeName(toks.get(i));

            List<Clause> clauses   = new ArrayList<Clause>();
            clauses.add(Clause.eq(Tables.ENTRIES.COL_PARENT_GROUP_ID,
                                  currentEntry.getId()));
            clauses.add(Clause.eq(Tables.ENTRIES.COL_NAME, childName));
            List<Entry> entries = readEntries(
					      getDatabaseManager().select(
									  Tables.ENTRIES.COLUMNS,
									  Tables.ENTRIES.NAME, clauses));
            if (entries.size() > 0) {
                currentEntry = entries.get(0);
            } else {
                if ( !createIfNeeded) {
                    return null;
                }
                currentEntry = makeNewGroup(request,currentEntry, childName, request.getUser(),
                                            null, (lastOne
						   ? lastGroupType
						   : groupType), initializer);
            }

            if (currentEntry.getTypeHandler().isSynthType()) {
                List<String> subset = toks.subList(i + 1, toks.size());
                if (subset.size() == 0) {
                    break;
                }

                return currentEntry.getTypeHandler().makeSynthEntry(request,
								    currentEntry, subset);
            }
        }
        if (currentEntry == null) {
            return null;
        }

        Entry filteredEntry = getAccessManager().filterEntry(request,
							     currentEntry);
        if (filteredEntry == null) {
            System.err.println("EntryManger.findEntryFromName:"
                               + " cannot view entry:" + currentEntry
                               + " user:" + request.getUser());
        }

        return filteredEntry;
    }


    /**
     * _more_
     *
     * @param parent _more_
     * @param name _more_
     * @param user _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry makeNewGroup(Request request,Entry parent, String name, User user)
	throws Exception {
        return makeNewGroup(request,parent, name, user, null);
    }



    /**
     * _more_
     *
     * @param parent _more_
     * @param name _more_
     * @param user _more_
     * @param template _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry makeNewGroup(Request request,Entry parent, String name, User user,
                              Entry template)
	throws Exception {
        String groupType = TypeHandler.TYPE_GROUP;
        if (template != null) {
            groupType = template.getTypeHandler().getType();
        }
        return makeNewGroup(request,parent, name, user, template, groupType);
    }



    /**
     * _more_
     *
     * @param parent _more_
     * @param name _more_
     * @param user _more_
     * @param template _more_
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry makeNewGroup(Request request,Entry parent, String name, User user,
                              Entry template, String type)
	throws Exception {
        return makeNewGroup(request,parent, name, user, template, type, null);
    }

    /**
     * _more_
     *
     * @param parent _more_
     * @param name _more_
     * @param user _more_
     * @param template _more_
     * @param type _more_
     * @param initializer _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry makeNewGroup(Request request,Entry parent, String name, User user,
                              Entry template, String type,
                              EntryInitializer initializer)
	throws Exception {
        //        synchronized (MUTEX_ENTRY) {
	Date now = new Date();
	String datePattern = request.getUnsafeString(ARG_DATE_PATTERN,null);
        Date date = Utils.extractDate(datePattern, name);
        if (date == null) {
            date = now;
        }

	if(type==null) type=TypeHandler.TYPE_GROUP;
        TypeHandler typeHandler = getRepository().getTypeHandler(type);
        Entry       group       = new Entry(getGroupId(parent), typeHandler);
        group.setName(name);
        group.setDate(now.getTime());

        group.setStartDate(date.getTime());
        group.setEndDate(date.getTime());	

        if (template != null) {
            group.initWith(template, true);
            getRepository().getMetadataManager().initNewEntry(request,group, initializer);
        }
        group.setParentEntry(parent);
        group.setUser(user);
        if (initializer != null) {
            initializer.initEntry(group);
        }
        addNewEntry(null, group);
        cacheEntry(group);
        return group;
        //        }
    }


    /**
     * _more_
     *
     * @param parent _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getGroupId(Entry parent) throws Exception {
        //FOr now just use regular ids for groups
        if (true) {
            return getRepository().getGUID();
        }

        int    baseId = 0;
        Clause idClause;
        String idWhere;
        if (parent == null) {
            idClause = Clause.isNull(Tables.ENTRIES.COL_PARENT_GROUP_ID);
        } else {
            idClause = Clause.eq(Tables.ENTRIES.COL_PARENT_GROUP_ID,
                                 parent.getId());
        }
        String newId = null;
        while (true) {
            if (parent == null) {
                newId = BLANK + baseId;
            } else {
                newId = parent.getId() + Entry.IDDELIMITER + baseId;
            }

            Statement stmt =
                getDatabaseManager().select(Tables.ENTRIES.COL_ID,
                                            Tables.ENTRIES.NAME,
                                            new Clause[] { idClause,
							   Clause.eq(Tables.ENTRIES.COL_ID, newId) });
            ResultSet idResults = stmt.getResultSet();

            if ( !idResults.next()) {
                break;
            }
            baseId++;
        }

        return newId;

    }


    /**
     * _more_
     *
     *
     * @param request _more_
     *
     * @param clause _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> getGroups(Request request, Clause clause)
	throws Exception {

        List<Clause> clauses = new ArrayList<Clause>();
        if (clause != null) {
            clauses.add(clause);
        }
        clauses.add(Clause.eq(Tables.ENTRIES.COL_TYPE,
                              TypeHandler.TYPE_GROUP));
        Statement statement =
            getDatabaseManager().select(Tables.ENTRIES.COL_ID,
                                        Tables.ENTRIES.NAME, clauses);

        return getGroups(
			 request,
			 SqlUtil.readString(
					    getDatabaseManager().getIterator(statement), 1));
    }


    /**
     * _more_
     *
     * @param entries _more_
     *
     * @return _more_
     */
    public List<Entry> getGroups(List<Entry> entries) {
        List<Entry> groupList = new ArrayList<Entry>();
        for (Entry entry : entries) {
            if (entry.isGroup()) {
                groupList.add((Entry) entry);
            }
        }

        return groupList;
    }

    /**
     * _more_
     *
     *
     *
     * @param request _more_
     * @param groupIds _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> getGroups(Request request, String[] groupIds)
	throws Exception {
        List<Entry> groupList = new ArrayList<Entry>();
        for (int i = 0; i < groupIds.length; i++) {
            Entry group = findGroup(request, groupIds[i]);
            if (group != null) {
                groupList.add(group);
            }
        }

        return groupList;
    }









    /**
     * _more_
     *
     * @param statement _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<Entry> readEntries(Statement statement) throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        try {
            SqlUtil.Iterator iter =
                getDatabaseManager().getIterator(statement);
            ResultSet results;
            while ((results = iter.getNext()) != null) {
                String entryType = results.getString(2);
                TypeHandler typeHandler =
                    getRepository().getTypeHandler(entryType, true);
                Entry entry =
                    (Entry) typeHandler.createEntryFromDatabase(null, results);
                entries.add(entry);
                cacheEntry(entry);
            }
        } finally {
            getDatabaseManager().closeAndReleaseConnection(statement);
        }

        for (Entry entry : entries) {
            if (entry.getParentEntryId() != null) {
                Entry parentEntry = (Entry) findGroup(null,
						      entry.getParentEntryId());
                entry.setParentEntry(parentEntry);
            }
        }

        return entries;
    }


    /**
     * _more_
     *
     * @param sb _more_
     */
    public void addStatusInfo(StringBuffer sb) {
        HU.formEntry(sb,msgLabel("Entry Cache"),
		     getEntryCache().size() / 2 + "");
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
    public Entry findGroup(Request request) throws Exception {
        String groupNameOrId = (String) request.getString(ARG_GROUP,
							  (String) null);
        if (groupNameOrId == null) {
            groupNameOrId = (String) request.getString(ARG_ENTRYID,
						       (String) null);
        }
        if ((groupNameOrId == null) && request.exists("parentof")) {
            Entry sibling = getEntry(request,
                                     request.getString("parentof", ""));
            if (sibling != null) {
                return sibling.getParentEntry();
            }

        }



        if (groupNameOrId == null) {
            throw new IllegalArgumentException("No group entry specified");
        }
        Entry entry = getEntry(request, groupNameOrId, false);
        if (entry != null) {
            if ( !entry.isGroup()) {
                throw new IllegalArgumentException("Not a group:"
						   + groupNameOrId);
            }

            return (Entry) entry;
        }

        throw new RepositoryUtil.MissingEntryException(
						       "Could not find folder:" + groupNameOrId);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entries _more_
     * @param connection _more_
     * @param firstCall _more_
     * @param ignoreSynth _more_
     * @param actionId _more_
     *
     * @return _more_
     * @throws Exception _more_
     */
    protected List<Descendent> getDescendents(Request request,
					      List<Entry> entries,
					      Connection connection,
					      EntryUtil.Excluder excluder,
					      boolean firstCall,
					      boolean ignoreSynth,
					      Object actionId)
	throws Exception {

        boolean        ok       = true;
        List<Descendent> children = new ArrayList();
        for (Entry entry : entries) {
            if ((actionId != null)
		&& !getActionManager().getActionOk(actionId)) {
                ok = false;
            }
            if ( !ok) {
                break;
            }

            if (firstCall) {
                children.add(new Descendent(entry));
		/*
		  children.add(new Object[] {
		  entry.getId(), entry.getTypeHandler().getType(),
		  entry.getResource().getPath(),
		  entry.getResource().getType(),
		  entry.getTypeHandler().getEntryValues(entry),
		  entry.getParentEntry()
		  });
		*/
            }
            if ( !entry.isGroup()) {
                continue;
            }

            if (entry.getMasterTypeHandler().isSynthType()
		|| isSynthEntry(entry.getId())) {
                if (ignoreSynth) {
                    continue;
                }
                for (String childId :
			 getChildIds(request, (Entry) entry, null)) {
                    if ((actionId != null)
			&& !getActionManager().getActionOk(actionId)) {
                        ok = false;

                        break;
                    }
                    Entry childEntry = getEntry(request, childId);
                    if (childEntry == null) {
                        continue;
                    }
		    if(excluder!=null && !excluder.isEntryOk(childEntry))
			continue;
                    children.add(new Descendent(childEntry));
		    /*
		      children.add(new Object[] {
		      childId, childEntry.getType(),
		      childEntry.getResource().getPath(),
		      childEntry.getResource().getType(),
		      entry.getTypeHandler().getEntryValues(entry),
		      entry.getParentEntry()
		      });
		    */
                    if (childEntry.isGroup()) {
                        children.addAll(getDescendents(request,
						       (List<Entry>) Misc.newList(childEntry),
						       connection,excluder, false, ignoreSynth, actionId));
                    }
                }

                return children;
	    }


            Statement stmt = SqlUtil.select(connection,
                                            SqlUtil.comma(new String[] {
						    Tables.ENTRIES.COL_ID,
						    Tables.ENTRIES.COL_TYPE, Tables.ENTRIES.COL_RESOURCE,
						    Tables.ENTRIES.COL_RESOURCE_TYPE }), Misc.newList(
												      Tables.ENTRIES.NAME), Clause.eq(
																      Tables.ENTRIES.COL_PARENT_GROUP_ID, entry.getId()));

            SqlUtil.Iterator iter = getDatabaseManager().getIterator(stmt);
            //Don't close the statement because that ends up closing the connection
            iter.setShouldCloseStatement(false);
            ResultSet results;
            while ((results = iter.getNext()) != null) {
                if ((actionId != null)
		    && !getActionManager().getActionOk(actionId)) {
                    ok = false;

                    break;
                }
                int    col       = 1;
                String childId   = results.getString(col++);
                String childType = results.getString(col++);
                String resource = getStorageManager().resourceFromDB(
								     results.getString(col++));
                String resourceType = results.getString(col++);
                Entry  childEntry   = getEntry(request, childId);
                if (childEntry == null) {
                    //This happened when a previous delete tree went bad and a parent has a record of
                    //a child that does not exist, or if ramadda.delete_entry_when_file_is_missing=true
                    continue;
                }

		if(excluder!=null && !excluder.isEntryOk(childEntry)) {
		    continue;
		}

                children.add(new Descendent(childEntry));
		/*

		  children.add(new Object[] {
		  childId, childType, resource, resourceType,
		  childEntry.getTypeHandler().getEntryValues(childEntry),
		  childEntry.getParentEntry()
		  });
		*/
                children.addAll(getDescendents(request,
					       (List<Entry>) Misc.newList(childEntry), connection,
					       excluder, false, ignoreSynth, actionId));
            }
            getDatabaseManager().closeStatement(stmt);
        }

        return children;

    }

    /** _more_ */
    private ProcessFileTypeHandler processFileTypeHandler;

    /** _more_ */
    private Entry processEntry;


    /** _more_ */
    private Hashtable<String, TypeHandler> synthEntryHandlers =
        new Hashtable<String, TypeHandler>();


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public ProcessFileTypeHandler getProcessFileTypeHandler()
	throws Exception {
        if (processFileTypeHandler == null) {
            ProcessFileTypeHandler tmp =
                new ProcessFileTypeHandler(getRepository(), null);
            tmp.setType("type_process");
            processFileTypeHandler = tmp;
        }

        return processFileTypeHandler;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getProcessEntry() throws Exception {
        return getRepository().getProcessFileTypeHandler().getSynthTopLevelEntry();
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public boolean handleEntryAsGroup(Entry entry) {
        if ( !entry.isGroup()) {
            return false;
        }
        String type = entry.getType();
        if (type.equals(TYPE_GROUP)) {
            return true;
        }

        //TODO: look at type handler properties
        return true;
    }


    /** _more_ */
    private HashSet missingResources = new HashSet();

    /** _more_ */
    public static final String PROP_DELETE_ENTRY_FILE_IS_MISSING =
        "ramadda.delete_entry_when_file_is_missing";

    /**
     * This handles entries when their file is missing
     * If the property ramadda.delete_entry_when_file_is_missing is set to true
     * then the entry is deleted.
     * Else, if the user is not logged then the the entry isn't shown
     *
     *
     * @param request the request
     * @param entry the entry
     *
     * @return The entry or null if missing
     *
     * @throws Exception on badness
     */
    public Entry handleMissingFileEntry(Request request, Entry entry)
	throws Exception {

        //If its not a FILE then don't do anything
        if ( !entry.getResource().isFileType()) {
            return entry;
        }

        File f = entry.getResource().getTheFile();
        //        if (f.exists()) {
        // Maybe handle linked files better
        Path p = f.toPath();
        if (Files.exists(p)) {
            return entry;
        }

        if (Files.notExists(p)) {

            if (getRepository().getProperty(
					    PROP_DELETE_ENTRY_FILE_IS_MISSING, false)) {
                deleteEntry(request, entry);
                System.err.println(
				   "RAMADDA: Deleted entry with missing file: "
				   + entry.getName() + " File:" + f);
                logInfo("RAMADDA: Deleted entry with missing file: "
                        + entry.getName() + " File:" + f);

                return null;
            } else {
                //                System.err.println("RAMADDA: Not configured to delete files: "+ entry.getName() + " File:" + f);

            }

            //Don't show the bad files for regular folk
            if (request!=null && request.isAnonymous()) {
                return null;
            }
        }

        String path = entry.getResource().getPath();
        if ( !missingResources.contains(path)) {
            missingResources.add(path);
            //            logError("File for entry: " + entry.getId() + " does not exist:" + path, null);
        }

        return entry;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param pattern _more_
     * @param to _more_
     * @param html _more_
     * @param doit _more_
     *
     * @throws Exception _more_
     */
    public void changeResourcePaths(Request request, String pattern,
                                    String to, Appendable html, boolean doit)
	throws Exception {
        Clause clause = Clause.like(Tables.ENTRIES.COL_RESOURCE,
                                    "%" + pattern + "%");
        Statement stmt =
            getDatabaseManager().select(
					Tables.ENTRIES.COL_ID + "," + Tables.ENTRIES.COL_RESOURCE,
					Tables.ENTRIES.NAME, clause, null);
        SqlUtil.Iterator iter    = getDatabaseManager().getIterator(stmt);
        ResultSet        results = null;
        String[] colNames = new String[] { Tables.ENTRIES.COL_RESOURCE };
        html.append("<ul>");
        int cnt = 0;
        while ((results = iter.getNext()) != null) {
            String id       = results.getString(1);
            String resource = results.getString(2);
            String newValue = resource.replace(pattern, to);
            cnt++;
            if (cnt <= 100) {
                html.append("<li> "
                            + HU.href(getRepository().URL_ENTRY_SHOW
				      + "?" + ARG_ENTRYID + "="
				      + id, resource) + "   to: "
			    + newValue);
                if (cnt == 100) {
                    html.append("<li> ...");
                }
            }
            if (doit) {
                getDatabaseManager().update(Tables.ENTRIES.NAME,
                                            Tables.ENTRIES.COL_ID, id,
                                            colNames,
                                            new String[] { newValue });
            }
        }
        html.append("</ul>");
        if (cnt == 0) {
            html.append(msg("Nothing found"));
        }
        getDatabaseManager().closeStatement(stmt);

    }


    public void  metadataHasChanged(Entry entry) throws Exception {
	Date date = new Date();
	getDatabaseManager().update(Tables.ENTRIES.NAME,
				    Tables.ENTRIES.COL_ID, 
				    entry.getId(),
				    new String[] {Tables.ENTRIES.COL_CHANGEDATE},
				    new Object[]{date});
	entry.setChangeDate(date.getTime());
	entry.setMetadata(null);
    }


    /**
     * _more_
     *
     * @param request The request
     * @param addOrderBy _more_
     * @param forEntry _more_
     * @param select _more_
     *
     * @return _more_
     */
    private String getQueryOrderAndLimit(Request request, boolean addOrderBy,
					 Entry forEntry, SelectInfo select) {


	if(select==null) select = new SelectInfo(request, forEntry);
        String  selectOrderBy        =  select.getOrderBy();
	boolean desc = !select.getAscending();
        boolean haveOrder = request.exists(ARG_ASCENDING) || select.hasAscending();
        String limitString =
	    getDatabaseManager().getLimitString(select.getSkip(),select.getMax());

        String orderBy = BLANK;
        if (addOrderBy) {
            orderBy = SqlUtil.orderBy(Tables.ENTRIES.COL_FROMDATE,desc);
        }	
        if (selectOrderBy != null) {
	    StringBuilder sb = new StringBuilder();
	    for(String by:Utils.split(selectOrderBy,",",true,true)) {
		if(sb.length()>0) sb.append(",");
		if (by.equals(ORDERBY_FROMDATE)) {
		    sb.append(SqlUtil.orderBy(Tables.ENTRIES.COL_FROMDATE,desc,false));
		} else if (by.equals(ORDERBY_TODATE)) {
		    sb.append(SqlUtil.orderBy(Tables.ENTRIES.COL_TODATE, desc,false));
		} else if (by.equals(ORDERBY_ENTRYORDER)) {
		    sb.append(SqlUtil.orderBy(Tables.ENTRIES.COL_ENTRYORDER, desc,false));		
		} else if (by.equals(ORDERBY_TYPE)) {
		    sb.append(SqlUtil.orderBy(Tables.ENTRIES.COL_TYPE,desc,false));
		} else if (by.equals(ORDERBY_SIZE)) {
		    //TODO: add a NULLS LAST for derby/postgres and something else for mysql and others?
		    sb.append(SqlUtil.orderBy(Tables.ENTRIES.COL_FILESIZE,desc,false));
		} else if (by.equals(ORDERBY_CREATEDATE) || by.equals(ORDERBY_RELEVANT)) {
		    sb.append(SqlUtil.orderBy(Tables.ENTRIES.COL_CREATEDATE,desc,false));
		} else if (by.equals(ORDERBY_NAME)) {
		    if ( !haveOrder) {
			desc = false;
		    }
		    sb.append(SqlUtil.orderBy("LOWER("+Tables.ENTRIES.COL_NAME+")",desc,false));
		}
	    }
	    if(sb.length()>0)
		orderBy="ORDER BY " + sb.toString();
	    else orderBy="";
        }
	if(debugGetEntries)
	    System.err.println("order:" + orderBy + " limit:" + limitString);
	return orderBy + limitString;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param base _more_
     * @param current _more_
     * @param dir _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getRelativeEntry(Request request, Entry base, Entry current,
                                  String dir)
	throws Exception {
        dir = dir.trim();
        //        System.err.println("getRelativeEntry: base:" + base.getName()  + " cwd:" + current.getName() + " text:" + dir);
        List<String> toks = Utils.split(dir, "/", true, true);
        if (toks.size() == 0) {
            //Maybe return base?
            return current;
        }
        Entry cwd = current;
        if (dir.startsWith("/")) {
            cwd = base;
        }


        for (String tok : toks) {
            //            System.err.println("   tok:" + tok + " cwd:" + cwd.getName());
            if (tok.equals("..")) {
                cwd = cwd.getParentEntry();
                if ((cwd == null) || cwd.equals(base)) {
                    break;
                }

                continue;
            }
            if (tok.matches("\\d+")) {
                int index =  Integer.parseInt(tok);
                index--;
                List<Entry> children = getChildren(request,   cwd);
                if ((index < 0) || (index >= children.size())) {
                    System.err.println("Did not get #child:" + dir
                                       + " index:" + index + " cwd:"
                                       + cwd.getName());

                    return null;
                }
                cwd = children.get(index);

                continue;
            }

            Entry child = findEntryWithName(request, cwd, tok);
            if (child == null) {
                System.err.println("Did not find child:" + tok + " cwd:"
                                   + cwd.getName());

                return null;
            }
            cwd = child;
        }

        return cwd;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param text _more_
     *
     * @throws Exception _more_
     */
    public void appendText(Request request, Entry entry, String text)
	throws Exception {
        if (entry.getTypeHandler().getType().equals("wikipage")) {
            text = text.replaceAll("<br>", "\n");
            Object[] values = entry.getTypeHandler().getEntryValues(entry);
            if (values[0] == null) {
                values[0] = text;
            } else {
                values[0] = values[0] + "\n" + text;
            }
        } else {
            entry.setDescription(entry.getDescription() + "<br>" + text);
        }
        updateEntry(request, entry);
    }


    /** _more_ */
    private String rightArrowIcon;

    /** _more_ */
    private String downArrowIcon;


    /**
     * _more_
     *
     * @return _more_
     */
    public String getRightArrowIcon() {
        if (rightArrowIcon == null) {
            rightArrowIcon =
                getRepository().getIconUrl(ICON_TOGGLEARROWRIGHT);
        }

        return rightArrowIcon;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getDownArrowIcon() {
        if (downArrowIcon == null) {
            downArrowIcon = getRepository().getIconUrl(ICON_TOGGLEARROWDOWN);
        }

        return downArrowIcon;
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
    private Entry getParentEntry(Request request) throws Exception {
        Entry parent = getEntryFromArg(request, ARG_TO + "_hidden");
        if (request.defined(ARG_GROUP)) {
            if (parent == null) {
                parent = getEntryFromArg(request, ARG_GROUP);
            }
            if (parent == null) {
                parent = findEntryFromName(request,null, request.getString(ARG_GROUP, ""));
            }
        }

        if (parent == null) {
            return null;
        }
        if ( !parent.isGroup()) {
            throw new IllegalArgumentException("Entry is not a group:"
					       + parent);
        }
        if ( !canAddTo(request, parent)) {
            throw new AccessException("Cannot add to entry", request);
        }


        return parent;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param baseArg _more_
     * @param value _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getEntryFormSelect(Request request, Entry entry,
                                     String baseArg, String value,String...type)
	throws Exception {
        Entry theEntry = null;
        if (value.length() > 0) {
            theEntry = getRepository().getEntryManager().getEntry(request,
								  value);
        }
        StringBuffer sb = new StringBuffer();
	String entryType = type.length>0?type[0]:null;
        String select =
            getRepository().getHtmlOutputHandler().getSelect(request,
							     baseArg,
							     HU.span(HU.image("fas fa-hand-pointer"),
								     HU.attrs("title","Select")),
							     true, null, entry,entryType);
	select  = HU.span(select,HU.attrs("class","ramadda-entry-select-links"));
        String event = getRepository().getHtmlOutputHandler().getSelectEvent(request, baseArg, true,null,  entry,entryType);

        sb.append("\n");
        sb.append(HU.hidden(baseArg + "_hidden", value,
			    HU.id(baseArg + "_hidden")));
        sb.append("\n");
	sb.append(select);
        sb.append(HU.disabledInput(baseArg, ((theEntry != null)
					     ? theEntry.getFullName()
					     : ""), HU.onMouseClick(event) +
				   HU.cssClass(HU.CLASS_DISABLEDINPUT+" ramadda-clickable") + HU.id(baseArg) + HU.SIZE_40));
        sb.append("\n");
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
    public String getEntryUrl(Request request, Entry entry) {
	return getEntryUrl(request, entry, true);
    }

    public String getEntryUrl(Request request, Entry entry, boolean aliasOk) {
        try {
	    if(aliasOk) {
		//		System.err.println("getEntryUrl:" + entry.getName());
		List<Metadata> metadataList =  getMetadataManager().findMetadata(request, entry,
										 new String[]{ContentMetadataHandler.TYPE_ALIAS}, false);
		if ((metadataList != null) && (metadataList.size() > 0)) {
		    for (Metadata metadata : metadataList) {
			if(!metadata.isType(ContentMetadataHandler.TYPE_ALIAS)) continue;
			String alias = metadata.getAttr1();
			if ( !alias.startsWith("http:")) {
			    return getRepository().getUrlBase() + "/a/" + alias;
			}
		    }
		}
	    }

            if (request == null) {
                request = getRepository().getTmpRequest();
            }

            return request.entryUrl(getRepository().URL_ENTRY_SHOW, entry);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    private static class Descendent {
	Entry entry;
	String id;
	String type;
	String path;
	String resourceType;
	Object[]values;
	Entry parent;
	Descendent(Entry entry) {
	    this.entry = entry;
	    id = entry.getId();
	    type = entry.getTypeHandler().getType();
	    path = entry.getResource().getPath();
	    resourceType = entry.getResource().getType();
	    values = entry.getTypeHandler().getEntryValues(entry);
	    parent = entry.getParentEntry();
	}

    }



}
