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


import org.ramadda.repository.auth.AccessException;
import org.ramadda.repository.auth.AuthorizationMethod;
import org.ramadda.repository.auth.Permission;
import org.ramadda.repository.auth.User;
import org.ramadda.repository.database.DatabaseManager;
import org.ramadda.repository.database.Tables;
import org.ramadda.repository.harvester.Harvester;
import org.ramadda.repository.metadata.AdminMetadataHandler;
import org.ramadda.repository.metadata.ContentMetadataHandler;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.output.XmlOutputHandler;
import org.ramadda.repository.type.Column;
import org.ramadda.repository.type.ProcessFileTypeHandler;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.repository.type.TypeInsertInfo;
import org.ramadda.repository.util.SelectInfo;
import org.ramadda.util.CategoryBuffer;
import org.ramadda.util.CategoryList;
import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.TTLCache;
import org.ramadda.util.TTLObject;
import org.ramadda.util.Utils;
import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlNodeList;
import ucar.unidata.xml.XmlUtil;


import java.awt.Image;
import java.awt.geom.Rectangle2D;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.net.URL;
import java.net.URLConnection;

import java.nio.file.Files;
import java.nio.file.Path;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;




/**
 * This class does most of the work of managing repository content
 */
public class EntryManager extends RepositoryManager {

    /** _more_ */
    public static final String[] PRELOAD_CATEGORIES = { "Documents",
            "General", "Information", "Collaboration", "Database" };


    /** _more_ */
    private EntryUtil entryUtil;


    /** _more_ */
    public static boolean debug = false;

    //In sql

    /** _more_ */
    public static final int MAX_NAME_LENGTH = 200;

    /** _more_ */
    public static final int MAX_DESCRIPTION_LENGTH = 15000;


    /** How many entries to we keep in the cache */
    public static final int ENTRY_CACHE_LIMIT = 2000;

    /** _more_ */
    public static final int SYNTHENTRY_CACHE_LIMIT = 4000;

    /** _more_ */
    public static final int ENTRY_CACHE_TTL_MINUTES = 60;

    /** _more_ */
    public static final int SYNTHENTRY_CACHE_TTL_MINUTES = 24 * 60;


    /** _more_ */
    public static final String SESSION_FOLDERS = "folders";

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

    /** _more_ */
    private TTLCache<String, Entry> synthEntryCache;




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
    public EntryUtil getEntryUtil() {
        if (entryUtil == null) {
            entryUtil = new EntryUtil(getRepository());
        }

        return entryUtil;
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
                Entry fromHostname = getEntryFromAlias(request,
                                         "http://"
                                         + request.getRequestHostname());


                if (fromHostname != null) {
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
                    }
                }
                System.err.println("entry:" + entry.getType() + " - "
                                   + entry.getName() + " " + entry.getId()
                                   + " - " + new Date(entry.getCreateDate()));
            }
        }


        if ((topEntry == null) && (entries.size() > 0)) {
            topEntry = (Entry) entries.get(0);
        }

        //Make the top group if needed
        if (topEntry == null) {
            topEntry = makeNewGroup(null, GROUP_TOP,
                                    getUserManager().getDefaultUser());
            getAccessManager().initTopEntry(topEntry);
        }

        if (rootCache == null) {
            rootCache = new TTLObject<Entry>(60 * 60 * 1000);
        }
        rootCache.put(topEntry);

        return topEntry;

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
        return getEntryFromMetadata(request,
                                    ContentMetadataHandler.TYPE_ALIAS, alias,
                                    1);
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
    public Entry getEntryFromMetadata(Request request, String metadataType,
                                      String value, int attrIndex)
            throws Exception {
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
                                  metadataType) }), "", 1);

        SqlUtil.Iterator iter = getDatabaseManager().getIterator(statement);
        ResultSet        results;
        while ((results = iter.getNext()) != null) {
            String id = results.getString(1);
            iter.close();

            return getEntry(request, id);
        }

        return null;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private TTLCache<String, Entry> getEntryCache() {
        //Get a local copy because another thread could clear the cache while we're in the middle of this
        TTLCache<String, Entry> theCache = entryCache;
        if (theCache == null) {
            int cacheTimeMinutes =
                getRepository().getProperty(PROP_CACHE_TTL,
                                            ENTRY_CACHE_TTL_MINUTES);
            //Convert to milliseconds
            entryCache = theCache = new TTLCache<String,
                    Entry>(cacheTimeMinutes * 60 * 1000);
        } else if (theCache.size() > ENTRY_CACHE_LIMIT) {
            entryCache = null;
        }

        return theCache;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private TTLCache<String, Entry> getSynthEntryCache() {
        //Get a local copy because another thread could clear the cache while we're in the middle of this
        TTLCache<String, Entry> theCache = synthEntryCache;
        if (theCache == null) {
            synthEntryCache = theCache = new TTLCache<String,
                    Entry>(SYNTHENTRY_CACHE_TTL_MINUTES * 60 * 1000);
        } else if (theCache.size() > SYNTHENTRY_CACHE_LIMIT) {
            synthEntryCache = null;
        }

        return theCache;
    }



    /**
     * _more_
     */
    @Override
    public void clearCache() {
        super.clearCache();
        entryCache      = null;
        synthEntryCache = null;
        rootCache       = null;
        getEntryUtil().clearCache();
    }


    /**
     * _more_
     *
     * @param entry _more_
     */
    public void cacheSynthEntry(Entry entry) {
        synchronized (MUTEX_ENTRY) {
            //Check if we are caching
            if ( !getRepository().doCache()) {
                return;
            }
            getSynthEntryCache().put(entry.getId(), entry);
        }
    }


    /**
     * _more_
     *
     * @param entry _more_
     */
    public void cacheEntry(Entry entry) {
        synchronized (MUTEX_ENTRY) {
            //Check if we are caching
            if ( !getRepository().doCache()) {
                return;
            }
            getEntryCache().put(entry.getId(), entry);
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
    protected Entry getEntryFromCache(String entryId, boolean isId) {
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
     * @param groupId _more_
     *
     * @return _more_
     */
    protected Entry getGroupFromCache(String groupId) {
        return getGroupFromCache(groupId, true);
    }

    /**
     * _more_
     *
     * @param groupId _more_
     * @param isId _more_
     *
     * @return _more_
     */
    protected Entry getGroupFromCache(String groupId, boolean isId) {
        Entry entry = getEntryFromCache(groupId, isId);
        if (entry == null) {
            return null;
        }

        return entry;
    }


    /**
     * _more_
     *
     * @param id _more_
     */
    protected void removeFromCache(String id) {
        synchronized (MUTEX_ENTRY) {
            getEntryCache().remove(id);
            getSynthEntryCache().remove(id);
        }
    }


    /**
     * _more_
     *
     * @param entry _more_
     */
    protected void removeFromCache(Entry entry) {
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
                                     RequestUrl requestUrl)
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
                entry = findEntryFromName(request, suffix, request.getUser(),
                                          false);
                if (entry == null) {
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

        getSessionManager().setLastEntry(request, entry);

        return entry;
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

        Entry entry = null;
        if (request.exists("parentof")) {
            Entry sibling = getEntry(request,
                                     request.getString("parentof", ""));
            if (sibling != null) {
                entry = sibling.getParentEntry();
            }
        }

        if (entry == null) {
            entry = getEntryFromRequest(request, ARG_ENTRYID,
                                        getRepository().URL_ENTRY_SHOW);
        }


        if (entry == null) {
            fatalError(request, "No entry specified");
        }


        addSessionFolder(request, entry);
        if (entry.getIsRemoteEntry()) {
            String redirectUrl = entry.getRemoteServer()
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
                            new SelectInfo(new ArrayList<Clause>()));
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
                request.remove(ARG_NEXT);
                request.remove(ARG_PREVIOUS);

                return new Result(request.getUrl());
            }
        }

        Result result = processEntryShow(request, entry);
        Result r      = addEntryHeader(request, entry, result);

        return r;
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
        List<String>      types        = new ArrayList<String>();
        List<TypeHandler> typeHandlers = getRepository().getTypeHandlers();
        boolean           checkCnt     = request.get("checkcount", true);
        for (TypeHandler typeHandler : typeHandlers) {
            if ( !typeHandler.getForUser()) {
                continue;
            }
            if (checkCnt) {
                int cnt = getEntryUtil().getEntryCount(typeHandler);
                if (cnt == 0) {
                    continue;
                }
            }
            types.add(typeHandler.getJson(request));
        }

        StringBuilder sb = new StringBuilder(Json.list(types));
        request.setReturnFilename("types.json");
        request.setCORSHeaderOnResponse();

        return new Result("", sb, Json.MIMETYPE);
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
        //For now don't add the entry header for the top-level entry
        //for pages like search, etc.
        if (entry == null) {
            return result;
        }
        if (Utils.stringUndefined(result.getTitle())) {
            result.setTitle(entry.getTypeHandler().getEntryName(entry));
        }

        if (entry == null) {
            entry = request.getRootEntry();
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

            StringBuilder titleCrumbs = new StringBuilder();
            String crumbs = getPageHandler().getEntryHeader(request,
                                entryForHeader, titleCrumbs);
            result.putProperty(PROP_ENTRY_HEADER, crumbs);
            result.putProperty(PROP_ENTRY_BREADCRUMBS,
                               titleCrumbs.toString());

            //FOR NOW - 
            //            result.putProperty(PROP_ENTRY_HEADER, "");
            //            result.putProperty(PROP_ENTRY_BREADCRUMBS, crumbs);

            result.putProperty(PROP_ENTRY_FOOTER, entryFooter);

            List<Metadata> metadataList =
                getMetadataManager().findMetadata(request, entry,
                    ContentMetadataHandler.TYPE_LOGO, true);
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

        try {
            if (handleAsGroup) {
                result = processGroupShow(request, outputHandler, outputType,
                                          entry);
            } else {
                OutputType dfltOutputType = getDefaultOutputType(request,
                                                entry, null, null);
                if (dfltOutputType != null) {
                    outputType = dfltOutputType;
                    outputHandler =
                        getRepository().getOutputHandler(outputType);
                }
                result = outputHandler.outputEntry(request, outputType,
                        entry);
            }
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
    public Result processGroupShow(Request request,
                                   OutputHandler outputHandler,
                                   OutputType outputType, Entry group)
            throws Exception {
        boolean      doLatest    = request.get(ARG_LATEST, false);
        TypeHandler  typeHandler = group.getTypeHandler();
        List<Clause> where       = typeHandler.assembleWhereClause(request);
        List<Entry>  entries     = new ArrayList<Entry>();
        List<Entry>  subGroups   = new ArrayList<Entry>();
        try {
            typeHandler.getChildrenEntries(
                request, group, entries, subGroups,
                new SelectInfo(where, outputHandler.getMaxEntryCount()));
        } catch (Exception exc) {
            exc.printStackTrace();
            request.put(ARG_MESSAGE,
                        getRepository().translate(request,
                            "Error finding children") + ":"
                                + exc.getMessage());
        }

        if (doLatest) {
            if (entries.size() > 0) {
                entries = getEntryUtil().sortEntriesOnDate(entries, true);

                return outputHandler.outputEntry(request, outputType,
                        entries.get(0));
            }
        }

        group.setSubEntries(entries);
        group.setSubGroups(subGroups);

        OutputType dfltOutputType = getDefaultOutputType(request, group,
                                        subGroups, entries);
        if (dfltOutputType != null) {
            outputType    = dfltOutputType;
            outputHandler = getRepository().getOutputHandler(outputType);
        }
        Result result = outputHandler.outputGroup(request, outputType, group,
                            subGroups, entries);

        return result;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param subFolders _more_
     * @param subEntries _more_
     *
     * @return _more_
     */
    private OutputType getDefaultOutputType(Request request, Entry entry,
                                            List<Entry> subFolders,
                                            List<Entry> subEntries) {
        if ( !request.defined(ARG_OUTPUT)) {
            for (PageDecorator pageDecorator :
                    repository.getPluginManager().getPageDecorators()) {
                String defaultOutput =
                    pageDecorator.getDefaultOutputType(getRepository(),
                        request, entry, subFolders, subEntries);
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


    /** _more_ */
    public static final String ARG_EXTEDIT_EDIT = "extedit.edit";

    /** _more_ */
    public static final String ARG_EXTEDIT_SPATIAL = "extedit.spatial";

    /** _more_ */
    public static final String ARG_EXTEDIT_TEMPORAL = "extedit.temporal";

    /** _more_ */
    public static final String ARG_EXTEDIT_MD5 = "extedit.md5";

    /** _more_ */
    public static final String ARG_EXTEDIT_REPORT = "extedit.report";

    /** _more_ */
    public static final String ARG_EXTEDIT_REPORT_MISSING =
        "extedit.report.missing";

    /** _more_ */
    public static final String ARG_EXTEDIT_REPORT_FILES =
        "extedit.report.files";

    /** _more_ */
    public static final String ARG_EXTEDIT_REPORT_EXTERNAL =
        "extedit.report.external";

    /** _more_ */
    public static final String ARG_EXTEDIT_REPORT_INTERNAL =
        "extedit.report.internal";

    /** _more_ */
    public static final String ARG_EXTEDIT_SETPARENTID =
        "extedit.setparentid";

    /** _more_ */
    public static final String ARG_EXTEDIT_NEWTYPE = "extedit.newtype";

    /** _more_ */
    public static final String ARG_EXTEDIT_NEWTYPE_PATTERN =
        "extedit.newtype.pattern";

    /** _more_ */
    public static final String ARG_EXTEDIT_OLDTYPE = "extedit.oldtype";

    /** _more_ */
    public static final String ARG_EXTEDIT_RECURSE = "extedit.recurse";

    /** _more_ */
    public static final String ARG_EXTEDIT_CHANGETYPE = "extedit.changetype";

    /** _more_ */
    public static final String ARG_EXTEDIT_CHANGETYPE_RECURSE =
        "extedit.changetype.recurse";

    /** _more_ */
    public static final String ARG_EXTEDIT_CHANGETYPE_RECURSE_CONFIRM =
        "extedit.changetype.recurse.confirm";


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntryExtEdit(final Request request)
            throws Exception {

        Entry              entry        = getEntry(request);
        final Entry        finalEntry   = entry;
        final boolean      recurse = request.get(ARG_EXTEDIT_RECURSE, false);
        final EntryManager entryManager = this;

        if (request.exists(ARG_EXTEDIT_EDIT)) {
            final boolean doMd5     = request.get(ARG_EXTEDIT_MD5, false);
            final boolean doSpatial = request.get(ARG_EXTEDIT_SPATIAL, false);
            final boolean doTemporal = request.get(ARG_EXTEDIT_TEMPORAL,
                                           false);
            ActionManager.Action action = new ActionManager.Action() {
                public void run(Object actionId) throws Exception {
                    EntryVisitor walker = new EntryVisitor(request,
                                              getRepository(), actionId,
                                              recurse) {
                        public boolean processEntry(Entry entry,
                                List<Entry> children)
                                throws Exception {
                            boolean changed = false;
                            if (doSpatial) {
                                Rectangle2D.Double rect = getBounds(children);
                                if (rect != null) {
                                    if ( !Misc.equals(rect,
                                            entry.getBounds())) {
                                        entry.setBounds(rect);
                                        changed = true;
                                    }
                                }
                            }
                            if (doTemporal) {
                                if (setTimeFromChildren(getRequest(), entry,
                                        children)) {
                                    changed = true;
                                }
                            }
                            if (changed) {
                                incrementProcessedCnt(1);
                                append(getPageHandler().getConfirmBreadCrumbs(
                                    getRequest(), entry));
                                append(HtmlUtils.br());
                                updateEntry(getRequest(), entry);
                            }

                            return true;
                        }
                    };
                    walker.walk(finalEntry);
                    getActionManager().setContinueHtml(actionId,
                            walker.getMessageBuffer().toString());
                }
            };

            return getActionManager().doAction(request, action,
                    "Walking the tree", "", entry);

        }


        /*
        if(request.exists(ARG_EXTEDIT_MD5)) {
            ActionManager.Action action = new ActionManager.Action() {
                public void run(Object actionId) throws Exception {
                    StringBuilder sb = new StringBuilder();
                    setMD5(request, actionId, sb, recurse, entry.getId(), new int[]{0}, new int[]{0});
                    if(sb.length()==0) sb.append("No checksums set");
                    getActionManager().setContinueHtml(actionId,
                                                       sb.toString());
                }
            };
            return getActionManager().doAction(request, action,
                                               "Setting MD5 Checksum", "", entry);

        }

        */

        final StringBuilder sb = new StringBuilder();



        if (request.exists(ARG_EXTEDIT_CHANGETYPE)) {
            TypeHandler newTypeHandler = getRepository().getTypeHandler(
                                             request.getString(
                                                 ARG_EXTEDIT_NEWTYPE, ""));

            entry = changeType(request, entry, newTypeHandler);
            sb.append(
                getPageHandler().showDialogNote(
                    msg("Entry type has been changed")));
        } else if (request.exists(ARG_EXTEDIT_CHANGETYPE_RECURSE)) {
            final boolean forReal =
                request.get(ARG_EXTEDIT_CHANGETYPE_RECURSE_CONFIRM, false);
            final TypeHandler newTypeHandler = getRepository().getTypeHandler(
                                                   request.getString(
                                                       ARG_EXTEDIT_NEWTYPE,
                                                       ""));
            final String oldType = request.getString(ARG_EXTEDIT_OLDTYPE, "");
            final String pattern =
                request.getString(ARG_EXTEDIT_NEWTYPE_PATTERN, (String) null);
            ActionManager.Action action = new ActionManager.Action() {
                public void run(final Object actionId) throws Exception {
                    EntryVisitor walker = new EntryVisitor(request,
                                              getRepository(), actionId,
                                              true) {
                        public boolean processEntry(Entry entry,
                                List<Entry> children)
                                throws Exception {
                            if ( !oldType.equals(TypeHandler.TYPE_ANY)
                                    && !entry.getTypeHandler().isType(
                                        oldType)) {
                                System.err.println("\tdoesn't match type:"
                                        + oldType);

                                return true;
                            }
                            if ((pattern != null) && (pattern.length() > 0)) {
                                boolean matches =
                                    entry.getName().matches(pattern);
                                if ( !matches
                                        && (entry.getResource().getPath()
                                            != null)) {
                                    matches =
                                        entry.getResource().getPath().matches(
                                            pattern);
                                }

                                if ( !matches) {
                                    System.err.println(
                                        "\tdoesn't match pattern:" + pattern);

                                    return true;
                                }
                            }
                            if (forReal) {
                                System.err.println("\tchanging type:"
                                        + entry.getName());
                                append("Changing type:" + entry.getName()
                                       + "<br>");
                                entry = changeType(request, entry,
                                        newTypeHandler);
                            } else {
                                System.err.println(
                                    "\twould be changing type:"
                                    + entry.getName());
                                append("We would be changing type:"
                                       + entry.getName() + "<br>");
                            }

                            return true;
                        }
                    };
                    walker.walk(finalEntry);
                    getActionManager().setContinueHtml(actionId,
                            walker.getMessageBuffer().toString());
                }
            };

            return getActionManager().doAction(request, action,
                    "Walking the tree, changing entries", "", entry);
        }


        /*
        if(request.exists(ARG_EXTEDIT_SETPARENTID)) {
            ActionManager.Action action = new ActionManager.Action() {
                public void run(Object actionId) throws Exception {
                    StringBuilder sb = new StringBuilder();
                    setParentId(request, actionId, sb, recurse, entry.getId(), new int[]{0}, new int[]{0});
                    getActionManager().setContinueHtml(actionId,
                                                       sb.toString());
                }
            };
            return getActionManager().doAction(request, action,
                                               "Setting parent ids", "", entry);

        }
        */

        if (request.exists(ARG_EXTEDIT_REPORT)) {
            final long[] size     = { 0 };
            final int[]  numFiles = { 0 };
            final boolean showMissing =
                request.get(ARG_EXTEDIT_REPORT_MISSING, false);
            final boolean showFiles = request.get(ARG_EXTEDIT_REPORT_FILES,
                                          false);
            EntryVisitor walker = new EntryVisitor(request, getRepository(),
                                      null, true) {
                @Override
                public boolean processEntry(Entry entry, List<Entry> children)
                        throws Exception {
                    for (Entry child : children) {
                        String url =
                            request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                             child);
                        if (child.isFileType()) {
                            boolean exists = child.getResource().fileExists();
                            if ( !exists && !showMissing) {
                                continue;
                            }
                            if (exists && !showFiles) {
                                continue;
                            }
                            append("<tr><td>");
                            append(getPageHandler().getBreadCrumbs(request,
                                    child, entry));
                            append("</td><td align=right>");
                            if (exists) {
                                File file = child.getFile();
                                size[0] += file.length();
                                numFiles[0]++;
                                append("" + file.length());
                            } else {
                                append("Missing:" + child.getResource());
                            }
                            append("</td>");
                            append("<td>");
                            if (child.getResource().isStoredFile()) {
                                append("***");
                            }
                            append("</td>");
                            append("</tr>");
                        } else if (child.isGroup()) {}
                        else {}
                    }

                    return true;
                }
            };
            walker.walk(entry);
            sb.append("<table><tr><td><b>" + msg("File") + "</b></td><td><b>"
                      + msg("Size") + "</td><td></td></tr>");
            sb.append(walker.getMessageBuffer());
            sb.append("<tr><td><b>" + msgLabel("Total")
                      + "</td><td align=right>"
                      + HtmlUtils.b(formatFileLength(size[0]))
                      + "</td></tr>");
            sb.append("</table>");
            sb.append("**** - File managed by RAMADDA");

            return makeEntryEditResult(request, entry, "Entry Report", sb);
        }


        sb.append(request.form(getRepository().URL_ENTRY_EXTEDIT,
                               HtmlUtils.attr("name", "entryform")));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));


        getPageHandler().entrySectionOpen(request, entry, sb,
                                          "Extended Edit", true);


        sb.append(formHeader("Spatial and Temporal Metadata"));
        sb.append(HtmlUtils.openInset(5, 30, 20, 0));

        sb.append(HtmlUtils.labeledCheckbox(ARG_EXTEDIT_SPATIAL, "true",
                                            false, "Set spatial metadata"));
        sb.append(HtmlUtils.p());
        sb.append(HtmlUtils.labeledCheckbox(ARG_EXTEDIT_TEMPORAL, "true",
                                            false, "Set temporal metadata"));
        sb.append(HtmlUtils.p());
        sb.append(HtmlUtils.labeledCheckbox(ARG_EXTEDIT_RECURSE, "true",
                                            true, "Recurse"));
        sb.append(HtmlUtils.p());
        //        sb.append(HtmlUtils.labeledCheckbox(ARG_EXTEDIT_MD5,"true", false, "Set MD5 checksums"));
        //        sb.append(HtmlUtils.p());
        sb.append(HtmlUtils.submit(msg("Set spatial and temporal metadata"),
                                   ARG_EXTEDIT_EDIT));



        sb.append(HtmlUtils.closeInset());

        sb.append(formHeader("File Listing"));
        sb.append(HtmlUtils.openInset(5, 30, 20, 0));
        sb.append(HtmlUtils.checkbox(ARG_EXTEDIT_REPORT_MISSING, "true",
                                     true) + " " + msg("Show missing files")
                                           + "<p>");
        sb.append(HtmlUtils.checkbox(ARG_EXTEDIT_REPORT_FILES, "true", true)
                  + " " + msg("Show OK files") + "<p>");
        sb.append(HtmlUtils.submit(msg("Generate File Listing"),
                                   ARG_EXTEDIT_REPORT));



        List<HtmlUtils.Selector> tfos = getTypeHandlerSelectors(request,
                                            true, true, entry);

        sb.append(HtmlUtils.closeInset());
        sb.append(formHeader("Change Entry Type"));
        sb.append(HtmlUtils.openInset(5, 30, 20, 0));
        sb.append(msgLabel("New type"));
        sb.append(HtmlUtils.space(1));

        //        this.jq(ID_TYPE_FIELD).selectBoxIt({});
        //        "data-iconurl",icon];

        sb.append(HtmlUtils.select(ARG_EXTEDIT_NEWTYPE, tfos));
        sb.append(HtmlUtils.p());
        List<Column> columns = entry.getTypeHandler().getColumns();
        if ((columns != null) && (columns.size() > 0)) {
            StringBuilder note = new StringBuilder();
            for (Column col : columns) {
                if (note.length() > 0) {
                    note.append(", ");
                }
                note.append(col.getLabel());
            }
            sb.append(msgLabel("Note: this metadata would be lost") + note);
        }

        sb.append(HtmlUtils.p());
        sb.append(HtmlUtils.submit(msg("Change type of this entry"),
                                   ARG_EXTEDIT_CHANGETYPE));
        sb.append(HtmlUtils.formClose());
        sb.append(HtmlUtils.closeInset());

        sb.append(formHeader("Change Descendents Entry Type"));
        sb.append(request.form(getRepository().URL_ENTRY_EXTEDIT,
                               HtmlUtils.attr("name", "entryform")));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));

        sb.append(HtmlUtils.openInset(5, 30, 20, 0));
        sb.append(HtmlUtils.formTable());
        sb.append(HtmlUtils.formEntry(msgLabel("Old type"),
                                      HtmlUtils.select(ARG_EXTEDIT_OLDTYPE,
                                          tfos)));

        sb.append(
            HtmlUtils.formEntry(
                msgLabel("Regexp Pattern"),
                HtmlUtils.input(ARG_EXTEDIT_NEWTYPE_PATTERN, "") + " "
                + msg("Only change type for entries that match this pattern")));
        sb.append(HtmlUtils.formEntry(msgLabel("New type"),
                                      HtmlUtils.select(ARG_EXTEDIT_NEWTYPE,
                                          tfos)));
        sb.append(
            HtmlUtils.formEntry(
                "",
                HtmlUtils.checkbox(
                    ARG_EXTEDIT_CHANGETYPE_RECURSE_CONFIRM, "true",
                    false) + " " + msg("Yes, change them")));


        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.p());
        sb.append(
            HtmlUtils.submit(
                msg("Change the type of all descendent entries"),
                ARG_EXTEDIT_CHANGETYPE_RECURSE));

        sb.append(HtmlUtils.formClose());
        sb.append(HtmlUtils.closeInset());


        return makeEntryEditResult(request, entry, "Extended Edit", sb);

    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param fileType _more_
     * @param nonFileType _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<HtmlUtils.Selector> getTypeHandlerSelectors(Request request,
            boolean fileType, boolean nonFileType, Entry entry)
            throws Exception {
        List<String> sessionTypes =
            (List<String>) getSessionManager().getSessionProperty(request,
                SESSION_TYPES);

        List<HtmlUtils.Selector> tfos  = new ArrayList<HtmlUtils.Selector>();

        HashSet<String>          first = new HashSet<String>();
        if (sessionTypes != null) {
            first.addAll(sessionTypes);
        }

        CategoryList<HtmlUtils.Selector> cats =
            new CategoryList<HtmlUtils.Selector>();
        for (String preload : PRELOAD_CATEGORIES) {
            cats.get(preload);
        }



        for (TypeHandler typeHandler : getRepository().getTypeHandlers()) {
            if ( !typeHandler.getForUser()) {
                continue;
            }
            if ( !fileType && !typeHandler.isGroup()) {
                continue;
            }
            if ( !nonFileType && typeHandler.isGroup()) {
                continue;
            }

            if ((entry != null)
                    && !entry.getTypeHandler().canChangeTo(typeHandler)) {
                continue;
            }

            HtmlUtils.Selector tfo =
                new HtmlUtils.Selector(
                    HtmlUtils.space(2) + typeHandler.getLabel(),
                    typeHandler.getType(), typeHandler.getTypeIconUrl());
            //Add the seen ones first
            if (first.contains(typeHandler.getType())) {
                if (tfos.size() == 0) {
                    tfos.add(new HtmlUtils.Selector("Recent", "", null, 0,
                            true));
                }
                tfos.add(tfo);
            }

            cats.add(typeHandler.getCategory(), tfo);
        }
        for (String cat : cats.getCategories()) {
            List<HtmlUtils.Selector> selectors = cats.get(cat);
            if (selectors.size() > 0) {
                tfos.add(new HtmlUtils.Selector(cat, "", null, 0, true));
                tfos.addAll(selectors);
            }
        }

        return tfos;
    }

    /*
        private void setMD5(Request request, StringBuilder sb, boolean recurse, String entryId, int []totalCnt, int[] setCnt) throws Exception {
            if(!getRepository().getActionManager().getActionOk(actionId)) {
                return;
            }
            Statement stmt = getDatabaseManager().select(SqlUtil.comma(new String[]{Tables.ENTRIES.COL_ID,
                                                                                    Tables.ENTRIES.COL_TYPE,
                                                                                    Tables.ENTRIES.COL_MD5,
                                                                                    Tables.ENTRIES.COL_RESOURCE}),
                Tables.ENTRIES.NAME,
                Clause.eq(Tables.ENTRIES.COL_PARENT_GROUP_ID, entryId));
            SqlUtil.Iterator iter = getDatabaseManager().getIterator(stmt);
            ResultSet        results;

            while ((results = iter.getNext()) != null) {
                totalCnt[0]++;
                int col = 1;
                String id = results.getString(col++);
                String type= results.getString(col++);
                String md5 = results.getString(col++);
                String resource = results.getString(col++);
                if(new File(resource).exists() && !Utils.stringDefined(md5)) {
                    setCnt[0]++;
                    Entry entry = getEntry(request, id);
                    if(!getAccessManager().canDoAction(request, entry,
                                                       Permission.ACTION_EDIT)) {
                        continue;
                    }
                    md5 = ucar.unidata.util.IOUtil.getMd5(resource);
                    getDatabaseManager().update(Tables.ENTRIES.NAME,
                                                Tables.ENTRIES.COL_ID,
                                                id, new String[]{Tables.ENTRIES.COL_MD5},
                                                new String[]{md5});
                    sb.append(getPageHandler().getConfirmBreadCrumbs(request, entry));
                    sb.append(HtmlUtils.br());
                }
                getActionManager().setActionMessage(actionId,
                                                    "Checked " + totalCnt[0] +" entries<br>Changed " + setCnt[0] +" entries");

                if(recurse) {
                    TypeHandler typeHandler = getRepository().getTypeHandler(type);
                    if(typeHandler.isGroup()) {
                        setMD5(request, actionId,  sb, recurse, id, totalCnt, setCnt);
                    }
                }
            }
            getDatabaseManager().closeStatement(stmt);
        }
        */



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param newTypeHandler _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry changeType(Request request, Entry entry,
                             TypeHandler newTypeHandler)
            throws Exception {
        addSessionType(request, newTypeHandler.getType());

        if ( !getAccessManager().canDoAction(request, entry,
                                             Permission.ACTION_EDIT)) {
            throw new AccessException("Cannot edit:" + entry.getLabel(),
                                      request);
        }

        Connection connection = getDatabaseManager().getConnection();
        try {
            Statement extraStmt = connection.createStatement();
            entry.getTypeHandler().deleteEntry(request, extraStmt,
                    entry.getId(), entry.getParentEntry(),
                    entry.getTypeHandler().getEntryValues(entry));
        } finally {
            getDatabaseManager().closeConnection(connection);
        }

        getDatabaseManager().update(Tables.ENTRIES.NAME,
                                    Tables.ENTRIES.COL_ID, entry.getId(),
                                    new String[] { Tables.ENTRIES.COL_TYPE },
                                    new String[] {
                                        newTypeHandler.getType() });

        removeFromCache(entry);
        entry = newTypeHandler.changeType(request, entry);

        return entry;
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
            return addEntryHeader(request, group,
                                  new Result(msg("Add Entry"), sb));
        }

        return makeEntryEditResult(request, entry, msg("Edit Entry"), sb);
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



        addSessionType(request, type);


        if ((entry != null) && entry.getIsLocalFile()) {
            sb.append(msg("This is a local file and cannot be edited"));

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



        String formId = HtmlUtils.getUniqueId("entryform_");
        if (type == null) {
            sb.append(request.form(getRepository().URL_ENTRY_FORM,
                                   HtmlUtils.attr("name", "entryform")
                                   + HtmlUtils.id(formId)));
        } else {
            request.uploadFormWithAuthToken(
                sb, getRepository().URL_ENTRY_CHANGE,
                HtmlUtils.attr("name", "entryform") + HtmlUtils.id(formId));
        }


        sb.append(HtmlUtils.formTable());
        String title = BLANK;

        if (type == null) {
            sb.append(
                HtmlUtils.formEntry(
                    msgLabel("Type"),
                    getRepository().makeTypeSelect(
                        request, false, "", true, null)));

            sb.append(
                HtmlUtils.formEntry(
                    BLANK, HtmlUtils.submit(msg("Select Type to Add"))));
            sb.append(HtmlUtils.hidden(ARG_GROUP, group.getId()));
        } else {
            title = ((entry == null)
                     ? msg("Add Entry")
                     : msg("Edit Entry"));
            String submitButton = HtmlUtils.submit((entry == null)
                    ? "Add " + typeHandler.getLabel()
                    : msg("Save"), ARG_SUBMIT,
                                   makeButtonSubmitDialog(sb, ((entry == null)
                    ? msg("Creating Entry...")
                    : msg("Changing Entry..."))));

            String nextButton = ((entry == null)
                                 ? ""
                                 : HtmlUtils.submit("Save & Next",
                                     ARG_SAVENEXT));


            String deleteButton = (((entry != null) && entry.isTopEntry())
                                   ? ""
                                   : HtmlUtils.submit(msg("Delete"),
                                       ARG_DELETE,
                                       makeButtonSubmitDialog(sb,
                                           "Deleting Entry...")));



            String cancelButton = HtmlUtils.submit(msg("Cancel"), ARG_CANCEL);
            String buttons      = ((entry != null)
                                   ? HtmlUtils.buttons(submitButton,
                                       deleteButton, cancelButton)
                                   : HtmlUtils.buttons(submitButton,
                                       cancelButton));

            HtmlUtils.row(sb, HtmlUtils.colspan(buttons, 2));
            if (entry != null) {
                sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
                sb.append(HtmlUtils.hidden(ARG_ENTRY_TIMESTAMP,
                                           getEntryTimestamp(entry)));
                if (isAnonymousUpload(entry)) {
                    List<Metadata> metadataList =
                        getMetadataManager().findMetadata(request, entry,
                            AdminMetadataHandler.TYPE_ANONYMOUS_UPLOAD,
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
                    String msg = HtmlUtils.space(2) + msg("Make public?")
                                 + extra;
                    sb.append(HtmlUtils.formEntry(msgLabel("Publish"),
                            HtmlUtils.checkbox(ARG_PUBLISH, "true", false)
                            + msg));
                }
            } else {
                sb.append(HtmlUtils.hidden(ARG_TYPE, type));
                sb.append(HtmlUtils.hidden(ARG_GROUP, group.getId()));
            }

            FormInfo formInfo = new FormInfo(formId);
            typeHandler.addToEntryForm(request, sb, group, entry, formInfo);

            formInfo.addToForm(sb);
            HtmlUtils.row(sb, HtmlUtils.colspan(buttons, 2));

        }
        HtmlUtils.formTableClose(sb);

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
    private void addSessionType(Request request, String type)
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
    public List<Entry> getSessionFolders(Request request) throws Exception {
        List<String> list =
            (List<String>) getSessionManager().getSessionProperty(request,
                SESSION_FOLDERS);
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
    public void addSessionFolder(Request request, Entry entry)
            throws Exception {
        if (request.isAnonymous()) {
            return;
        }
        if (entry.isGroup()) {
            List<String> list =
                (List<String>) getSessionManager().getSessionProperty(
                    request, SESSION_FOLDERS);
            if (list == null) {
                list = new ArrayList<String>();
            }
            list.remove(entry.getId());
            list.add(0, entry.getId());
            //Cap the size at 5
            if (list.size() > 5) {
                list.remove(list.size() - 1);
            }
            getSessionManager().putSessionProperty(request, SESSION_FOLDERS,
                    list);
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
        request.ensureAuthToken();
        if (download) {
            ActionManager.Action action = new ActionManager.Action() {
                public void run(Object actionId) throws Exception {
                    Result result = doProcessEntryChange(request, false,
                                        actionId);
                    getActionManager().setContinueHtml(actionId,
                            HtmlUtils.href(result.getRedirectUrl(),
                                           msg("Continue")));
                }
            };

            return getActionManager().doAction(request, action,
                    "Downloading file", "");

        }

        return doProcessEntryChange(request, false, null);
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
        return getRepository().getAccessManager().canDoAction(request,
                parent, Permission.ACTION_NEW);

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
            for (String ip : StringUtil.split(ips, ";", true, true)) {
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
    private Result doProcessEntryChange(Request request, boolean forUpload,
                                        Object actionId)
            throws Exception {


        User user = request.getUser();
        if (forUpload) {
            logInfo("upload doProcessEntryChange user = " + user);
        }

        Entry       entry       = null;
        TypeHandler typeHandler = null;
        boolean     newEntry    = true;
        if (request.defined(ARG_ENTRYID)) {
            entry = getEntry(request);
            if (entry == null) {
                fatalError(request, "Cannot find entry");
            }
            if (forUpload) {
                fatalError(request, "Cannot edit when doing an upload");
            }
            if ( !getAccessManager().canDoAction(request, entry,
                    Permission.ACTION_EDIT)) {
                throw new AccessException("Cannot edit:" + entry.getLabel(),
                                          request);
            }
            if (entry.getIsLocalFile()) {
                return new Result(
                    request.entryUrl(
                        getRepository().URL_ENTRY_SHOW, entry, ARG_MESSAGE,
                        getRepository().translate(
                            request, "Cannot edit local files")));

            }


            //Remove this entry from the memory cache 
            //so edits don't show up for others
            removeFromCache(entry);

            typeHandler = entry.getTypeHandler();
            newEntry    = false;



            if (request.exists(ARG_CANCEL)) {
                return new Result(
                    request.entryUrl(getRepository().URL_ENTRY_SHOW, entry));
            }

            if ((entry != null) && isAnonymousUpload(entry)) {
                if (request.get(ARG_JUSTPUBLISH, false)) {
                    publishAnonymousEntry(request, entry);
                    List<Entry> entries = new ArrayList<Entry>();
                    entries.add(entry);
                    insertEntries(request, entries, newEntry);

                    return new Result(
                        request.entryUrl(
                            getRepository().URL_ENTRY_FORM, entry));
                }
            }



            //If we have a timestamp then check if the user 
            //was editing an up to date entry
            if (request.defined(ARG_ENTRY_TIMESTAMP)) {
                String formTimestamp = request.getString(ARG_ENTRY_TIMESTAMP,
                                           "0");
                String currentTimestamp = getEntryTimestamp(entry);
                if ( !Misc.equals(formTimestamp, currentTimestamp)) {
                    StringBuilder sb        = new StringBuilder();
                    String        dateRange = "";
                    try {
                        dateRange = new Date(formTimestamp) + ":"
                                    + new Date(currentTimestamp);
                    } catch (Exception ignore) {}
                    getPageHandler().entrySectionOpen(request, entry, sb,
                            "Entry Edit");
                    sb.append(
                        getPageHandler().showDialogError(
                            msg(
                            "Error: The entry you are editing has been edited since the time you began the edit:"
                            + dateRange)));
                    getPageHandler().entrySectionClose(request, entry, sb);

                    return addEntryHeader(request, entry,
                                          new Result(msg("Entry Edit Error"),
                                              sb));
                }
            }

            if (request.exists(ARG_DELETE_CONFIRM)) {
                if (entry.isTopEntry()) {
                    return new Result(
                        request.entryUrl(
                            getRepository().URL_ENTRY_SHOW, entry,
                            ARG_MESSAGE,
                            getRepository().translate(
                                request, "Cannot delete top-level folder")));
                }


                deleteEntry(request, entry);
                Entry group = findGroup(request, entry.getParentEntryId());

                return new Result(
                    request.entryUrl(
                        getRepository().URL_ENTRY_SHOW, group, ARG_MESSAGE,
                        getRepository().translate(
                            request, "Entry is deleted")));
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

        boolean     figureOutType = request.get(ARG_TYPE_GUESS, false);

        List<Entry> entries       = new ArrayList<Entry>();
        String      category      = "";
        if (request.defined(ARG_CATEGORY)) {
            category = request.getString(ARG_CATEGORY, "");
        } else {
            category = request.getString(ARG_CATEGORY_SELECT, "");
        }


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
            if (groupId == null) {
                fatalError(request, "You must specify a parent folder");
            }
            Entry parentEntry = findGroup(request);
            if (forUpload) {
                logInfo("Upload:checking access");
            }
            boolean okToCreateNewEntry =
                getAccessManager().canDoAction(request, parentEntry,
                    (forUpload
                     ? Permission.ACTION_UPLOAD
                     : Permission.ACTION_NEW), forUpload);

            if (forUpload) {
                logInfo("Upload:is ok to create:" + okToCreateNewEntry);
            }
            if ( !okToCreateNewEntry) {
                throw new AccessException("Cannot add:" + entry.getLabel(),
                                          request);
            }


            List<String> resources = new ArrayList<String>();
            List<Entry>  parents   = new ArrayList<Entry>();
            List<String> origNames = new ArrayList<String>();


            String       resource  = "";
            String urlArgument = request.getAnonymousEncodedString(ARG_URL,
                                     BLANK);
            String  filename     = typeHandler.getUploadedFile(request);
            boolean unzipArchive = false;

            boolean isFile       = false;
            String  resourceName = request.getString(ARG_FILE, BLANK);

            if (serverFile != null) {
                filename = serverFile.toString();
            }

            if (resourceName.length() == 0) {
                resourceName = IOUtil.getFileTail(resource);
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
                                getRepository().translate(
                                    request, "Error downloading URL")));
                    }
                    resourceName =
                        getStorageManager().getFileTail(newFile.toString());
                    resource = newFile.toString();
                }
            }

            boolean isGzip = resource.endsWith(".gz");

            // check if it's a zp file
            if (unzipArchive && !resource.toLowerCase().endsWith(".zip")) {
                unzipArchive = false;
            }

            /*
            if (unzipArchive && !resource.toLowerCase().endsWith(".zip")) {
                if (!isGzip) {
                    unzipArchive = false;
                }
            }
            if (isGzip && unzipArchive) {
                //TODO: use GZIPInputStream to unzip the file
            }
            */

            boolean hasZip = false;

            if (serverFile != null) {
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
                    if (pattern.length() == 0) {
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
                            resources.add(f.toString());
                            origNames.add(f.toString());
                            parents.add(parentEntry);
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
                    resources.add(serverFile.toString());
                    origNames.add(resourceName);
                    parents.add(parentEntry);
                }
            } else if ( !unzipArchive) {
                resources.add(resource);
                origNames.add(resourceName);
                parents.add(parentEntry);
            } else {
                hasZip = true;
                Hashtable<String, Entry> nameToGroup = new Hashtable<String,
                                                           Entry>();
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
                        String name = IOUtil.getFileTail(path);
                        if (name.equals("MANIFEST.MF")) {
                            continue;
                        }
                        //Skip dot files as well
                        if (name.startsWith(".")) {
                            continue;
                        }
                        Entry parent = parentEntry;
                        if (request.get(ARG_FILE_PRESERVEDIRECTORY, false)) {
                            List<String> toks = StringUtil.split(path, "/",
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
                                    Request tmpRequest =
                                        getRepository().getTmpRequest();
                                    tmpRequest.setUser(user);
                                    group = findGroupUnder(tmpRequest,
                                            parent, parentName, user);
                                    nameToGroup.put(ancestors, group);
                                }
                                parent = group;
                            }
                        }
                        File f = getStorageManager().getTmpFile(request,
                                     name);
                        fos = getStorageManager().getFileOutputStream(f);
                        try {
                            IOUtil.writeTo(zin, fos);
                        } finally {
                            IOUtil.close(fos);
                        }
                        parents.add(parent);
                        resources.add(f.toString());
                        origNames.add(name);
                    }
                } finally {
                    IOUtil.close(fis);
                    IOUtil.close(zin);
                }
            }

            if (request.exists(ARG_CANCEL)) {
                return new Result(
                    request.entryUrl(
                        getRepository().URL_ENTRY_SHOW, parentEntry));
            }

            String description =
                request.getAnonymousEncodedString(ARG_DESCRIPTION, BLANK);

            Date createDate = new Date();
            Date[] dateRange = request.getDateRange(ARG_FROMDATE, ARG_TODATE,
                                   createDate);


            File originalFile = null;

            for (int resourceIdx = 0; resourceIdx < resources.size();
                    resourceIdx++) {
                Entry parent = parents.get(resourceIdx);
                resourceName = (String) resources.get(resourceIdx);
                String theResource = (String) resources.get(resourceIdx);
                String origName    = (String) origNames.get(resourceIdx);
                if (isFile && (serverFile == null)) {
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

                //If its an anon upload  or we're unzipping an archive then don't set the name
                String name = ((forUpload || hasZip)
                               ? ""
                               : request.getAnonymousEncodedString(ARG_NAME,
                                   BLANK));



                if (name.indexOf("${") >= 0) {}
                if (name.trim().length() == 0) {
                    name = IOUtil.getFileTail(origName);
                    if (request.get(ARG_MAKENAME, false)) {
                        name = name.replaceAll("_", " ");
                        name = IOUtil.stripExtension(name);
                        StringBuilder tmp = new StringBuilder();
                        for (String tok :
                                StringUtil.split(name, " ", true, true)) {
                            tok = StringUtil.camelCase(tok);
                            tmp.append(tok);
                            tmp.append(" ");
                        }
                        name = tmp.toString().trim();
                    }
                }


                Date[] theDateRange = { dateRange[0], dateRange[1] };

                if (request.defined(ARG_DATE_PATTERN)) {
                    String format = request.getUnsafeString(ARG_DATE_PATTERN,
                                        BLANK);
                    String pattern = format;
                    //swap out any of the date tokens with a decimal regexp
                    for (String s : new String[] {
                        "y", "m", "M", "d", "H", "m"
                    }) {
                        pattern = pattern.replaceAll(s, "_DIGIT_");
                    }
                    pattern = pattern.replaceAll("_DIGIT_", "\\\\d");
                    pattern = ".*(" + pattern + ").*";
                    Matcher matcher =
                        Pattern.compile(pattern).matcher(origName);
                    if (matcher.find()) {
                        String dateString = matcher.group(1);
                        Date dttm = RepositoryUtil.makeDateFormat(
                                        format).parse(dateString);
                        theDateRange[0] = dttm;
                        theDateRange[1] = dttm;
                    } else {}
                }

                String id           = getRepository().getGUID();
                String resourceType = Resource.TYPE_UNKNOWN;
                if (serverFile != null) {
                    resourceType = Resource.TYPE_LOCAL_FILE;
                } else if (isFile) {
                    resourceType = Resource.TYPE_STOREDFILE;
                } else {
                    try {
                        new URL(theResource);
                        resourceType = Resource.TYPE_URL;
                    } catch (Exception exc) {}
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

                if (name.trim().length() == 0) {
                    name = typeHandlerToUse.getDefaultEntryName(resourceName);
                }
                entry = typeHandlerToUse.createEntry(id);


                if (theDateRange[0] == null) {
                    //Don't try to extract the date from the name of the file
                    //Its more trouble than worth due to bad matches
                    //theDateRange[0] = theDateRange[1] =   Utils.extractDate(theResource);
                }


                if (theDateRange[0] == null) {
                    theDateRange[0] = ((theDateRange[1] == null)
                                       ? createDate
                                       : theDateRange[1]);
                }
                if (theDateRange[1] == null) {
                    theDateRange[1] = theDateRange[0];
                }



                entry.initEntry(name, description, parent, request.getUser(),
                                new Resource(theResource, resourceType),
                                category, createDate.getTime(),
                                createDate.getTime(),
                                theDateRange[0].getTime(),
                                theDateRange[1].getTime(), null);
                if (forUpload) {
                    initUploadedEntry(request, entry, parent);
                }

                setEntryState(request, entry, parent, newEntry);
                entries.add(entry);
            }
        } else {
            boolean fileUpload      = false;
            String  newResourceName = request.getUploadedFile(ARG_FILE);
            String  newResourceType = null;

            //Did they upload a new file???
            if (newResourceName != null) {
                newResourceName = getStorageManager().moveToStorage(request,
                        new File(newResourceName)).toString();
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
                                getRepository().translate(
                                    request, "Error downloading URL")));

                    }
                    newResourceName =
                        getStorageManager().moveToStorage(request,
                            newFile).toString();
                    newResourceType = Resource.TYPE_LOCAL_FILE;
                } else {
                    newResourceName = url;
                    newResourceType = Resource.TYPE_URL;
                }
            }




            if ((newResourceName != null)
                    || request.get(ARG_DELETEFILE, false)) {
                //If it was a stored file then remove the old one
                if (entry.getResource().isStoredFile()) {
                    getStorageManager().removeFile(entry.getResource());
                }
                if (newResourceName != null) {
                    entry.setResource(new Resource(newResourceName,
                            newResourceType));
                } else {
                    entry.setResource(new Resource());
                }
            }

            if (entry.isTopEntry()) {
                //fatalError(request,"Cannot edit top-level folder");
            }
            Date[] dateRange = request.getDateRange(ARG_FROMDATE, ARG_TODATE,
                                   new Date());
            String newName = request.getString(ARG_NAME, entry.getLabel());

            entry.setName(newName);
            String tmp = request.getString(ARG_DESCRIPTION,
                                           entry.getDescription());

            entry.setDescription(request.getString(ARG_DESCRIPTION,
                    entry.getDescription()));

            if (isAnonymousUpload(entry)) {
                if (request.get(ARG_PUBLISH, false)) {
                    publishAnonymousEntry(request, entry);
                }
            } else {
                entry.setCategory(category);
            }


            if (dateRange[0] != null) {
                entry.setStartDate(dateRange[0].getTime());
            }
            if (dateRange[1] == null) {
                dateRange[1] = dateRange[0];
            }

            if (dateRange[1] != null) {
                entry.setEndDate(dateRange[1].getTime());
            }
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



        if (newEntry) {
            if (request.get(ARG_METADATA_ADD, false)) {
                addInitialMetadata(request, entries, newEntry, false);
            } else if (request.get(ARG_METADATA_ADDSHORT, false)) {
                addInitialMetadata(request, entries, newEntry, true);
            }
        }


        insertEntries(request, entries, newEntry);
        if (newEntry) {
            for (Entry theNewEntry : entries) {
                theNewEntry.getTypeHandler().doFinalEntryInitialization(
                    request, theNewEntry, false);
            }
        }



        if (forUpload) {
            entry = (Entry) entries.get(0);

            return new Result(
                request.entryUrl(
                    getRepository().URL_ENTRY_SHOW, entry.getParentEntry(),
                    ARG_MESSAGE,
                    getRepository().translate(
                        request, "File has been uploaded")));
        }

        if (entries.size() == 1) {
            entry = (Entry) entries.get(0);
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
                    + HtmlUtils.pad(
                        getRepository().translate(
                            request, "files uploaded"))));
        } else {
            return new Result(BLANK,
                              new StringBuilder(msg("No entries created")));
        }

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
        if ( !url.toLowerCase().startsWith("http:")
                && !url.toLowerCase().startsWith("https:")
                && !url.toLowerCase().startsWith("ftp:")) {
            fatalError(request, "Cannot download url:" + url);
        }
        getStorageManager().checkPath(url);
        String        tail       = IOUtil.getFileTail(url);
        File          newFile = getStorageManager().getTmpFile(request, tail);

        URL           fromUrl    = new URL(url);
        URLConnection connection = fromUrl.openConnection();
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
            IOUtil.close(toStream);
            IOUtil.close(fromStream);
        }

        return newFile;
    }


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
        File   newFile   = new File(theResource);
        String shortName = newFile.getName();

        //Handle case sensitive first
        for (TypeHandler otherTypeHandler :
                getRepository().getTypeHandlers()) {
            if (otherTypeHandler.canHandleResource(theResource, shortName)) {
                return otherTypeHandler;
            }
        }

        //now try any case
        for (TypeHandler otherTypeHandler :
                getRepository().getTypeHandlers()) {
            if (otherTypeHandler.canHandleResource(theResource.toLowerCase(),
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

        String url = HtmlUtils.url(getFullEntryShowUrl(null), ARG_ENTRYID,
                                   entry.getId());
        //j-
        String[] macros = {
            "entryid", entry.getId(), "parentid", entry.getParentEntryId(),
            "resourcepath", entry.getResource().getPath(), "resourcename",
            getStorageManager().getFileTail(entry.getResource().getPath()),
            "filename",
            getStorageManager().getFileTail(entry.getResource().getPath()),
            "fileextension",
            IOUtil.getFileExtension(entry.getResource().getPath()), "name",
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
        if (request.defined(ARG_LOCATION_LATITUDE)
                && request.defined(ARG_LOCATION_LONGITUDE)) {
            entry.setLatitude(request.get(ARG_LOCATION_LATITUDE, 0.0));
            entry.setLongitude(request.get(ARG_LOCATION_LONGITUDE, 0.0));

            getSessionManager().putSessionProperty(request,
                    ARG_LOCATION_LATITUDE,
                    entry.getLatitude() + ";" + entry.getLongitude());
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
            getRepository().getSessionManager().setArea(request,
                    entry.getNorth(), entry.getWest(), entry.getSouth(),
                    entry.getEast());

        }

        List<Entry> children       = null;


        double      altitudeTop    = Entry.NONGEO;
        double      altitudeBottom = Entry.NONGEO;
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





    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param children _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public boolean setTimeFromChildren(Request request, Entry entry,
                                       List<Entry> children)
            throws Exception {
        if (children == null) {
            children = getChildren(request, entry);
        }
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        for (Entry child : children) {
            minTime = Math.min(minTime, child.getStartDate());
            maxTime = Math.max(maxTime, child.getEndDate());
        }
        boolean changed = false;

        if (minTime != Long.MAX_VALUE) {
            long diffStart = minTime - entry.getStartDate();
            long diffEnd   = maxTime - entry.getEndDate();
            //We seem to lose some time resolution when we store so only assume a change
            //when the time differs by more than 5 seconds
            changed = (diffStart < -10000) || (diffStart > 10000)
                      || (diffEnd < -10000) || (diffEnd > 10000);
            entry.setStartDate(minTime);
            entry.setEndDate(maxTime);

        }

        return changed;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void setBoundsFromChildren(Request request, Entry entry)
            throws Exception {
        if (entry == null) {
            return;
        }
        Rectangle2D.Double rect = getBounds(getChildren(request, entry));
        if (rect != null) {
            entry.setBounds(rect);
            updateEntry(request, entry);
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
    public Result processEntryDelete(Request request) throws Exception {
        Entry         entry = getEntry(request);
        StringBuilder sb    = new StringBuilder();
        if (entry.isTopEntry()) {
            sb.append(
                getPageHandler().showDialogNote(
                    "Cannot delete top-level folder"));

            return makeEntryEditResult(request, entry, "Delete Entry", sb);
        }

        if (request.exists(ARG_CANCEL)) {
            return new Result(
                request.entryUrl(getRepository().URL_ENTRY_FORM, entry));
        }

        if (request.exists(ARG_DELETE_CONFIRM)) {
            request.ensureAuthToken();
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
            inner.append(
                HtmlUtils.div(
                    breadcrumbs, HtmlUtils.cssClass("ramadda-confirm")));
            inner.append(
                HtmlUtils.b(
                    msg(
                    "Note: This will also delete everything contained by this folder")));
        } else {
            inner.append(
                msg("Are you sure you want to delete the following entry?"));
            inner.append(
                HtmlUtils.div(
                    breadcrumbs, HtmlUtils.cssClass("ramadda-confirm")));
        }



        StringBuilder fb = new StringBuilder();
        fb.append(request.form(getRepository().URL_ENTRY_DELETE, BLANK));

        getRepository().addAuthToken(request, fb);
        fb.append(HtmlUtils.buttons(HtmlUtils.submit(msg("OK"),
                ARG_DELETE_CONFIRM), HtmlUtils.submit(msg("Cancel"),
                    ARG_CANCEL)));
        fb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        fb.append(HtmlUtils.formClose());
        getPageHandler().entrySectionOpen(request, entry, sb, "Delete Entry");
        sb.append(getPageHandler().showDialogQuestion(inner.toString(),
                fb.toString()));

        getPageHandler().entrySectionClose(request, entry, sb);

        return makeEntryEditResult(request, entry,
                                   msg("Entry delete confirm"), sb);
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
                StringUtil.split(request.getString(ARG_ENTRYIDS, ""), ",",
                                 true, true)) {
            Entry entry = getEntry(request, id, false);
            if (entry == null) {
                throw new RepositoryUtil.MissingEntryException(
                    "Could not find entry:" + id);
            }
            if (entry.isTopEntry()) {
                StringBuilder sb = new StringBuilder();
                sb.append(
                    getPageHandler().showDialogNote(
                        msg("Cannot delete top-level folder")));

                return new Result(msg("Entry Delete"), sb);
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
        if (request.exists(ARG_CANCEL)) {
            if (entries.size() == 0) {
                return new Result(
                    request.makeUrl(getRepository().URL_ENTRY_SHOW));
            }
            String id = entries.get(0).getParentEntryId();

            return new Result(request.makeUrl(getRepository().URL_ENTRY_SHOW,
                    ARG_ENTRYID, id));
        }

        if (request.exists(ARG_DELETE_CONFIRM)) {
            request.ensureAuthToken();

            return asynchDeleteEntries(request, entries);
        }


        if (entries.size() == 0) {
            return new Result(
                "",
                new StringBuilder(
                    getPageHandler().showDialogWarning(
                        msg("No entries selected"))));
        }

        StringBuilder msgSB    = new StringBuilder();
        StringBuilder idBuffer = new StringBuilder();
        for (Entry entry : entries) {
            idBuffer.append(",");
            idBuffer.append(entry.getId());
        }
        boolean       anyFolders  = false;
        StringBuilder entryListSB = new StringBuilder();
        for (Entry toBeDeletedEntry : entries) {
            entryListSB.append(
                getPageHandler().getConfirmBreadCrumbs(
                    request, toBeDeletedEntry));
            entryListSB.append(HtmlUtils.br());
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
        msgSB.append(HtmlUtils.div(entryListSB.toString(),
                                   HtmlUtils.cssClass("ramadda-confirm")));

        if (anyFolders) {
            msgSB.append(
                HtmlUtils.div(
                    HtmlUtils.b(
                        msg(
                        "Note: This will also delete everything contained by the above "
                        + ((entries.size() == 1)
                           ? "folder"
                           : "folders")))));
        }
        request.formPostWithAuthToken(sb,
                                      getRepository().URL_ENTRY_DELETELIST);
        StringBuilder hidden =
            new StringBuilder(HtmlUtils.hidden(ARG_ENTRYIDS,
                idBuffer.toString()));
        String form = PageHandler.makeOkCancelForm(request,
                          getRepository().URL_ENTRY_DELETELIST,
                          ARG_DELETE_CONFIRM, hidden.toString());
        sb.append(getPageHandler().showDialogQuestion(msgSB.toString(),
                form));


        return new Result(msg("Delete Confirm"), sb);
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
                      : HtmlUtils.href(
                          request.entryUrl(
                              getRepository().URL_ENTRY_SHOW,
                              group), "Continue");

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
    private void deleteEntry(Request request, Entry entry) throws Exception {
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
    private void deleteEntriesInner(Request request, List<Entry> entries,
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


        List<Object[]> found = getDescendents(request, entries, connection,
                                   true, true, actionId);
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
            List<String>   allIds            = new ArrayList<String>();
            List<Resource> resourcesToDelete = new ArrayList<Resource>();
            for (int i = found.size() - 1; i >= 0; i--) {
                Object[] tuple  = found.get(i);
                String   id     = (String) tuple[0];
                Object[] values = (Object[]) tuple[4];
                Entry    parent = (Entry) tuple[5];
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

                resourcesToDelete.add(
                    new Resource(
                        new File((String) tuple[2]), (String) tuple[3]));

                batchCnt++;
                assocStmt.setString(2, id);
                for (PreparedStatement stmt : statements) {
                    stmt.setString(1, id);
                    stmt.addBatch();
                }

                TypeHandler typeHandler =
                    getRepository().getTypeHandler((String) tuple[1], true);
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
            Misc.run(getRepository(), "checkDeletedEntries", allIds);
        } finally {
            getDatabaseManager().closeStatement(extraStmt);
            for (PreparedStatement stmt : statements) {
                getDatabaseManager().closeStatement(stmt);
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
                AdminMetadataHandler.TYPE_ANONYMOUS_UPLOAD, false);
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
        getMetadataManager().addMetadata(
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

        if (getAdmin().isEmailCapable()) {
            StringBuilder contents =
                new StringBuilder(
                    "A new entry has been uploaded to the RAMADDA server under the folder: ");
            String url1 = HtmlUtils.url(getFullEntryShowUrl(request),
                                        ARG_ENTRYID, parentEntry.getId());

            contents.append(HtmlUtils.href(url1, parentEntry.getFullName()));
            contents.append("<p>\n\n");
            String url = HtmlUtils.url(getFullEntryShowUrl(request),
                                       ARG_ENTRYID, entry.getId());
            contents.append("Edit to confirm: ");
            contents.append(HtmlUtils.href(url, entry.getLabel()));
            boolean sentNotification = false;
            List<Metadata> metadataList =
                getMetadataManager().findMetadata(request, parentEntry,
                    ContentMetadataHandler.TYPE_CONTACT, true);
            if (metadataList != null) {
                for (Metadata metadata : metadataList) {
                    sentNotification = true;
                    getRepository().getMailManager().sendEmail(
                        metadata.getAttr2(), "Uploaded Entry",
                        contents.toString(), true);

                }

            }
            if ( !sentNotification) {
                getRepository().getMailManager().sendEmail(
                    parentUser.getEmail(), "Uploaded Entry",
                    contents.toString(), true);
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
    public Result processEntryUpload(Request request) throws Exception {
        TypeHandler typeHandler =
            getRepository().getTypeHandler(TypeHandler.TYPE_CONTRIBUTION);

        Entry         group = findGroup(request);
        StringBuilder sb    = new StringBuilder();
        if ( !request.exists(ARG_CONTRIBUTION_FROMNAME)) {
            getPageHandler().entrySectionOpen(request, group, sb,
                    "Upload a File");
            sb.append(request.uploadForm(getRepository().URL_ENTRY_UPLOAD,
                                         HtmlUtils.attr("name",
                                             "entryform")));
            sb.append(HtmlUtils.submit(msg("Upload")));
            sb.append(HtmlUtils.formTable());
            sb.append(HtmlUtils.hidden(ARG_GROUP, group.getId()));
            typeHandler.addToEntryForm(request, sb, group, null,
                                       new FormInfo(""));
            HtmlUtils.formTableClose(sb);
            sb.append(HtmlUtils.submit(msg("Upload")));
            sb.append(HtmlUtils.formClose());
            getPageHandler().entrySectionClose(request, group, sb);
        } else {
            return doProcessEntryChange(request, true, null);
        }

        return makeEntryEditResult(request, group, msg("Upload"), sb);
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


        Hashtable<String, CategoryBuffer> superCatMap = new Hashtable<String,
                                                            CategoryBuffer>();
        List<String>   superCats = new ArrayList<String>();

        CategoryBuffer cats      = new CategoryBuffer();
        //Preload the super categories
        superCats.add("");
        superCatMap.put("", cats);
        superCats.add("Science and Education");
        superCatMap.put("Science and Education", new CategoryBuffer());
        superCats.add("Miscellaneous");
        superCatMap.put("Miscellaneous", new CategoryBuffer());


        for (String preload : PRELOAD_CATEGORIES) {
            cats.get(preload);
        }

        HashSet<String> exclude = new HashSet<String>();
        //        exclude.add(TYPE_FILE);
        //        exclude.add(TYPE_GROUP);


        List<String> sessionTypes =
            (List<String>) getSessionManager().getSessionProperty(request,
                SESSION_TYPES);

        List<TypeHandler> typeHandlers = getRepository().getTypeHandlers();
        for (TypeHandler typeHandler : typeHandlers) {
            if ( !typeHandler.getForUser()) {
                continue;
            }
            if (typeHandler.isAnyHandler()) {
                continue;
            }
            if (exclude.contains(typeHandler.getType())) {
                continue;
            }
            if ( !typeHandler.canBeCreatedBy(request)) {
                continue;
            }
            String icon = typeHandler.getIconProperty(null);
            String img;
            if (icon == null) {
                icon = ICON_BLANK;
                img = HtmlUtils.img(typeHandler.getIconUrl(icon), "",
                                    HtmlUtils.attr(HtmlUtils.ATTR_WIDTH,
                                        "16"));
            } else {
                img = HtmlUtils.img(typeHandler.getIconUrl(icon));
            }




            boolean hasUsedType =
                ((sessionTypes != null)
                 && sessionTypes.contains(typeHandler.getType()));
            String category      = typeHandler.getCategory();
            String superCategory = typeHandler.getSuperCategory();
            if(superCategory.equals("Basic")) superCategory ="";
            cats = superCatMap.get(superCategory);
            if (cats == null) {
                cats = new CategoryBuffer();
                superCats.add(superCategory);
                superCatMap.put(superCategory, cats);
            }

            cats.get(category);
            if (hasUsedType) {
                cats.moveToFront(category);
            }
            cats.append(category, HtmlUtils
                .href(request
                    .makeUrl(getRepository().URL_ENTRY_FORM, ARG_GROUP, group
                        .getId(), ARG_TYPE, typeHandler.getType()), img + " "
                            + msg(typeHandler.getLabel())));

            cats.append(category, HtmlUtils.br());
        }

        StringBuilder inner = new StringBuilder();

        for (String superCategory : superCats) {
            cats = superCatMap.get(superCategory);
            inner.append("<div class=ramadda-section>");
            inner.append("<h2>" + superCategory + "</h2>");
            inner.append("<table cellpadding=10><tr valign=top>");
            int colCnt = 0;
            for (String cat : cats.getCategories()) {
                StringBuilder catBuff = cats.get(cat);
                if (catBuff.length() == 0) {
                    continue;
                }
                HtmlUtils.col(inner,
                              HtmlUtils.b(msg(cat))
                              + HtmlUtils.div(catBuff.toString(),
                                  HtmlUtils.cssClass("entry-type-list")));
                colCnt++;
                if (colCnt > 4) {
                    inner.append(
                        "</tr><tr><td>&nbsp;</td></tr><tr valign=top>");
                    colCnt = 0;
                }
            }
            inner.append("</tr></table>");
            inner.append("</div>");
        }

        getPageHandler().entrySectionOpen(request, group, sb,
                                          "Choose entry type");
        sb.append(HtmlUtils.open("div"," class='ramadda-links' "));
        sb.append(HtmlUtils.insetDiv(inner.toString(), 10, 20, 0, 0));
        sb.append(HtmlUtils.close("div"));
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
            file = IOUtil.getFileTail(request.getRequestPath());
            request.put(ARG_FILESUFFIX, file);
        }


        List[] groupAndEntries =
            getRepository().getEntryManager().getEntries(request);
        List<Entry> entries = (List<Entry>) groupAndEntries[1];
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
        List<String> toks = StringUtil.split(request.getRequestPath(), "/",
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

        sb.append(getEntryActionsTable(request, entry, OutputType.TYPE_ALL));
        getPageHandler().entrySectionClose(request, entry, sb);

        return addEntryHeader(request, entry,
                              new Result(msg("Entry Actions"), sb));
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
                                          getRepository().URL_ENTRY_GET);

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


        if ( !entry.getResource().isUrl()) {
            if ( !getAccessManager().canDownload(request, entry)) {
                fatalError(request, "Cannot download file");
            }
        }


        String path = entry.getResource().getPath();
        String mimeType = getRepository().getMimeTypeFromSuffix(
                              IOUtil.getFileExtension(path));

        boolean isImage = Utils.isImage(path);
        if (request.defined(ARG_IMAGEWIDTH) && isImage) {
            int width = request.get(ARG_IMAGEWIDTH, 75);
            File thumb = getStorageManager().getThumbFile("entry"
                             + IOUtil.cleanFileName(entry.getId()) + "_"
                             + width + IOUtil.getFileExtension(path));
            if ( !thumb.exists()) {
                Image image = Utils.readImage(entry.getResource().getPath());
                image = ImageUtils.resize(image, width, -1);
                ImageUtils.waitOnImage(image);
                ImageUtils.writeImageToFile(image, thumb);
            }

            return new Result(BLANK,
                              getStorageManager().getFileInputStream(thumb),
                              mimeType);
        } else {
            File file   = entry.getFile();
            long length = file.length();
            if (request.isHeadRequest()) {
                Result result = new Result("", new StringBuilder());
                result.addHttpHeader(HtmlUtils.HTTP_CONTENT_LENGTH,
                                     "" + length);
                result.addHttpHeader("Connection", "close");
                result.setLastModified(new Date(file.lastModified()));

                return result;
            }

            //Get the original filename and set that on the result so the browser sees the file - not just "get"
            String fileName = getStorageManager().getFileTail(entry);
            request.setReturnFilename(fileName);

            int response = Result.RESPONSE_OK;
            InputStream inputStream =
                getStorageManager().getFileInputStream(file);

            String range = (String) request.getHttpHeaderArgs().get("Range");

            long   byteStart = -1;
            long   byteEnd   = -1;
            if (Utils.stringDefined(range)) {
                //assume: bytes=start-end
                List<String> toks1 = StringUtil.splitUpTo(range, "=", 2);
                List<String> toks  = StringUtil.split(toks1.get(1), "-");
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

            Result result = new Result(BLANK, inputStream, mimeType);
            result.setResponseCode(response);
            result.addHttpHeader("Accept-Ranges", "bytes");
            result.addHttpHeader(HtmlUtils.HTTP_CONTENT_LENGTH, "" + length);
            result.setLastModified(new Date(file.lastModified()));
            result.setCacheOk(
                getRepository().getProperty("ramadda.http.cachefile", false));

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
        return getStorageManager().getCacheFile(entry.getId() + "_"
                + entry.getChangeDate() + "_" + filename);
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
            List<String> idList = StringUtil.split(ids, ",", true, true);
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
            if (group == null) {
                group = entry.getParentEntry();
            } else if ( !group.equals(entry.getParentEntry())) {
                group = null;

                break;
            }
        }

        if (group != null) {
            request.put(ARG_ENTRYID, group.getId());
        }

        OutputHandler outputHandler =
            getRepository().getOutputHandler(request);
        String dummyGroupName = request.getString(ARG_RETURNFILENAME,
                                    "Search Results");
        Result result = outputHandler.outputGroup(request,
                            request.getOutput(),
                            getDummyGroup(dummyGroupName),
                            new ArrayList<Entry>(), entries);

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

        String      fromIds = request.getString(ARG_FROM, "");
        List<Entry> entries = new ArrayList<Entry>();
        for (String id : StringUtil.split(fromIds, ",", true, true)) {
            Entry entry = getEntry(request, id, false);
            if (entry == null) {
                throw new RepositoryUtil.MissingEntryException(
                    "Could not find entry:" + id);
            }
            if (entry.isTopEntry()) {
                StringBuilder sb = new StringBuilder();
                sb.append(
                    getPageHandler().showDialogNote(
                        msg("Cannot copy top-level folder")));

                return new Result(msg("Entry Delete"), sb);
            }
            entries.add(entry);
        }

        if (entries.size() == 0) {
            throw new IllegalArgumentException("No entries specified");
        }

        if (request.exists(ARG_CANCEL)) {
            return new Result(
                request.entryUrl(
                    getRepository().URL_ENTRY_SHOW, entries.get(0)));
        }


        StringBuilder fromList = new StringBuilder();
        for (Entry fromEntry : entries) {
            fromList.append(getPageHandler().getBreadCrumbs(request,
                    fromEntry));
            fromList.append(HtmlUtils.br());
        }
        String fromDiv =
            HtmlUtils.div(fromList.toString(),
                          HtmlUtils.cssClass("entry-confirm-list"));

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
            toEntry = findGroupFromName(request, toName, request.getUser(),
                                        false);
        }



        if ( !request.exists(ARG_CONFIRM) || (toEntry == null)) {


            StringBuilder sb = new StringBuilder();
            if (entries.size() == 1) {
                getPageHandler().entrySectionOpen(request, entries.get(0),
                        sb, null);
            }
            if (request.exists(ARG_CONFIRM) && (toEntry == null)) {
                sb.append(
                    getPageHandler().showDialogWarning(
                        "Please select a destination"));
            }
            request.formPostWithAuthToken(sb, getRepository().URL_ENTRY_COPY);
            if (force != null) {
                sb.append(HtmlUtils.hidden(ARG_ACTION_FORCE, force));
            }

            sb.append(HtmlUtils.sectionOpen(msg(label)));

            sb.append(
                HtmlUtils.div(
                    msg("The Entries"),
                    HtmlUtils.cssClass("entry-confirm-header")));
            sb.append(fromDiv);


            if (force == null) {
                sb.append(
                    HtmlUtils.div(
                        msg("What do you want to do?"),
                        HtmlUtils.cssClass("entry-confirm-header")));
                sb.append(
                    HtmlUtils.open(
                        HtmlUtils.TAG_DIV,
                        HtmlUtils.cssClass("entry-confirm-list")));
                sb.append(HtmlUtils.labeledRadio(ARG_ACTION, "move", isMove,
                        msg("Move")));
                sb.append("&nbsp;&nbsp;");
                sb.append(HtmlUtils.labeledRadio(ARG_ACTION, "copy", isCopy,
                        msg("Copy")));
                sb.append("&nbsp;&nbsp;");
                sb.append(HtmlUtils.labeledRadio(ARG_ACTION, "link", isLink,
                        msg("Link")));
                sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
            }



            sb.append(
                HtmlUtils.div(
                    msg("Select a destination"),
                    HtmlUtils.cssClass("entry-confirm-header")));
            sb.append(
                HtmlUtils.open(
                    HtmlUtils.TAG_DIV,
                    HtmlUtils.cssClass("entry-confirm-list")));
            sb.append(HtmlUtils.hidden(ARG_FROM, fromIds));


            if (toEntry != null) {
                sb.append(msgLabel("Target Entry"));
                sb.append(HtmlUtils.space(1));
                sb.append(toEntry.getTypeHandler().getEntryName(toEntry));
                sb.append(HtmlUtils.hidden(ARG_TO, toEntry.getId()));
            } else {
                Entry parent = entries.get(0).getParentEntry();
                String select =
                    getRepository().getHtmlOutputHandler().getSelect(
                        request, ARG_TO, HtmlUtils.img(
                            getRepository().getIconUrl(
                                ICON_FOLDER_OPEN)) + HtmlUtils.space(1)
                                    + msg("Select")
                                    + HtmlUtils.space(
                                        1), true, "", parent, false);

                sb.append(HtmlUtils.hidden(ARG_TO + "_hidden",
                                           parent.getId(),
                                           HtmlUtils.id(ARG_TO + "_hidden")));

                sb.append(select);
                sb.append(HtmlUtils.space(1));
                sb.append(HtmlUtils.disabledInput(ARG_TO, parent.getName(),
                        HtmlUtils.SIZE_60 + HtmlUtils.id(ARG_TO)));

            }
            sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));

            sb.append(
                HtmlUtils.div(
                    msg("You sure?"),
                    HtmlUtils.cssClass("entry-confirm-header")));
            sb.append(
                HtmlUtils.open(
                    HtmlUtils.TAG_DIV,
                    HtmlUtils.cssClass("entry-confirm-list")));
            sb.append(HtmlUtils.buttons(HtmlUtils.submit(msg("Yes, do it"),
                    ARG_CONFIRM), HtmlUtils.submit(msg("Cancel"),
                        ARG_CANCEL)));

            sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));

            sb.append(HtmlUtils.formClose());
            sb.append(HtmlUtils.sectionClose());

            if (entries.size() == 1) {
                getPageHandler().entrySectionClose(request, entries.get(0),
                        sb);
            }

            return addEntryHeader(request, entries.get(0),
                                  new Result(msg("Entry Move/Copy"), sb));
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
                if ( !getAccessManager().canDoAction(request, fromEntry,
                        Permission.ACTION_EDIT)) {
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

        if (isGroup) {
            addSessionFolder(request, toEntry);
        }

        if ( !getAccessManager().canDoAction(request, toEntry,
                                             Permission.ACTION_NEW)) {
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


        request.ensureAuthToken();
        if (isMove) {
            Entry toGroup = (Entry) toEntry;

            return processEntryMove(request, toGroup, entries);
        } else if (isCopy) {
            Entry toGroup = (Entry) toEntry;

            return processEntryCopy(request, toGroup, entries);
        } else if (isLink) {
            if (entries.size() == 1) {
                return new Result(
                    request.makeUrl(
                        getRepository().URL_ASSOCIATION_ADD, ARG_FROM,
                        entries.get(0).getId(), ARG_TO, toEntry.getId()));
            }
        }


        Result result = new Result(msg(label), new StringBuilder());

        return addEntryHeader(request, toEntry, result);

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
    public Result processEntryTypeChange(Request request) throws Exception {

        String      fromIds = request.getString(ARG_FROM, "");
        List<Entry> entries = new ArrayList<Entry>();
        for (String id : StringUtil.split(fromIds, ",", true, true)) {
            Entry entry = getEntry(request, id, false);
            if (entry == null) {
                throw new RepositoryUtil.MissingEntryException(
                    "Could not find entry:" + id);
            }
            if (entry.isTopEntry()) {
                StringBuilder sb = new StringBuilder();
                sb.append(
                    getPageHandler().showDialogNote(
                        msg("Cannot copy top-level folder")));

                return new Result(msg("Entry Delete"), sb);
            }
            entries.add(entry);
        }


        if (entries.size() == 0) {
            throw new IllegalArgumentException("No entries specified");
        }

        if (request.exists(ARG_CANCEL)) {
            return new Result(
                request.entryUrl(
                    getRepository().URL_ENTRY_SHOW, entries.get(0)));
        }


        StringBuffer sb = new StringBuffer();
        if (request.exists(ARG_CONFIRM)) {
            TypeHandler newTypeHandler = getRepository().getTypeHandler(
                                             request.getString(
                                                 ARG_EXTEDIT_NEWTYPE, ""));



            request.ensureAuthToken();

            sb.append(msgLabel("The following entries have been changed"));
            sb.append("<ul>");
            for (Entry entry : entries) {
                if ( !getAccessManager().canDoAction(request, entry,
                        Permission.ACTION_EDIT)) {
                    throw new IllegalArgumentException(
                        "Whoa dude, you can't edit this entry:"
                        + entry.getName());
                }
                entry = changeType(request, entry, newTypeHandler);
                String icon = newTypeHandler.getIconProperty(null);
                if (icon != null) {
                    icon = newTypeHandler.getIconUrl(icon);
                }
                sb.append(HtmlUtils.href(getEntryURL(request, entry),
                                         HtmlUtils.img(icon) + " "
                                         + entry.getName()));
                sb.append("<br>");
            }
            sb.append("</ul>");





            return new Result(msg(""), sb);
        }



        List<HtmlUtils.Selector> tfos = getTypeHandlerSelectors(request,
                                            true, true, null);

        request.formPostWithAuthToken(sb,
                                      getRepository().URL_ENTRY_TYPECHANGE);
        sb.append(HtmlUtils.hidden(ARG_FROM, fromIds));

        sb.append(HtmlUtils.p());

        StringBuffer inner = new StringBuffer();
        inner.append(msg("Are you sure you want to change the entry types?"));
        inner.append(HtmlUtils.p());
        inner.append(HtmlUtils.formTable());
        inner.append(
            HtmlUtils.formEntry(
                msgLabel("New type"),
                HtmlUtils.select(ARG_EXTEDIT_NEWTYPE, tfos)));

        HtmlUtils.formTableClose(inner);

        sb.append(
            getPageHandler().showDialogQuestion(
                inner.toString(),
                HtmlUtils.buttons(
                    HtmlUtils.submit(
                        msg("Yes, change the entry types"),
                        ARG_CONFIRM), HtmlUtils.submit(
                            msg("Cancel"), ARG_CANCEL))));
        sb.append(HtmlUtils.formClose());
        sb.append("<table>");
        sb.append("<tr><td><b>Entry</b></td><td><b>Type</b></td></tr>");
        for (Entry entry : entries) {
            sb.append("<tr><td>");
            sb.append(HtmlUtils.img(entry.getTypeHandler().getTypeIconUrl()));
            sb.append(" ");
            sb.append(entry.getName());
            sb.append("</td><td>");
            sb.append(entry.getTypeHandler().getLabel());
            sb.append("</td></tr>");
        }
        sb.append("</table>");


        return new Result(msg(""), sb);

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
                                   final List<Entry> entries)
            throws Exception {


        final String link =
            HtmlUtils.href(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                            toGroup), "Continue");
        ActionManager.Action action = new ActionManager.Action() {
            public void run(Object actionId) throws Exception {
                processEntryCopyAsynch(request, toGroup, entries, actionId,
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
    private List<Entry> processEntryCopyAsynch(final Request request,
            final Entry toGroup, final List<Entry> entries, Object actionId,
            String link)
            throws Exception {

        StringBuilder sb         = new StringBuilder();
        List<Entry>   newEntries = new ArrayList<Entry>();
        try {
            Connection connection = getDatabaseManager().getConnection();
            connection.setAutoCommit(false);
            List<Object[]> ids = getDescendents(request, entries, connection,
                                     true, true, actionId);
            getDatabaseManager().closeConnection(connection);
            Hashtable<String, Entry> oldIdToNewEntry = new Hashtable<String,
                                                           Entry>();

            for (int i = 0; i < ids.size(); i++) {
                if ( !getActionManager().getActionOk(actionId)) {
                    return null;
                }


                Object[]    tuple          = ids.get(i);
                String      id             = (String) tuple[0];
                Entry       oldEntry       = getEntry(request, id);
                String      newId          = getRepository().getGUID();
                TypeHandler oldTypeHandler = oldEntry.getTypeHandler();
                TypeHandler newTypeHandler =
                    oldTypeHandler.getTypeHandlerForCopy(oldEntry);
                Entry newEntry = newTypeHandler.createEntry(newId);
                oldIdToNewEntry.put(oldEntry.getId(), newEntry);
                //See if this new entry is somewhere down in the tree
                Entry newParent =
                    oldIdToNewEntry.get(oldEntry.getParentEntryId());
                if (newParent == null) {
                    newParent = toGroup;
                }
                Resource newResource =
                    oldTypeHandler.getResourceForCopy(request, oldEntry,
                        newEntry);
                newEntry.initEntry(oldEntry.getName(),
                                   oldEntry.getDescription(),
                                   (Entry) newParent, request.getUser(),
                                   newResource, oldEntry.getCategory(),
                                   oldEntry.getCreateDate(),
                                   new Date().getTime(),
                                   oldEntry.getStartDate(),
                                   oldEntry.getEndDate(),
                                   oldEntry.getValues());

                newEntry.setLocation(oldEntry);
                newTypeHandler.initializeCopiedEntry(newEntry, oldEntry);

                List<Metadata> newMetadata = new ArrayList<Metadata>();
                for (Metadata oldMetadata :
                        getMetadataManager().getMetadata(oldEntry)) {
                    newMetadata.add(
                        getMetadataManager().copyMetadata(
                            oldEntry, newEntry, oldMetadata));
                }
                newEntry.setMetadata(newMetadata);
                newEntries.add(newEntry);
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
                insertEntries(request, tmp, true);
                count++;
                getActionManager().setActionMessage(actionId,
                        "Copied " + count + "/" + newEntries.size()
                        + " entries");
                links.append(
                    HtmlUtils.href(
                        request.entryUrl(
                            getRepository().URL_ENTRY_SHOW,
                            newEntry), newEntry.getName()));
                links.append("<br>");
            }
            if (newEntries.size() > 0) {
                link = links.toString();
            }

            getActionManager().setContinueHtml(actionId,
                    count + " entries copied" + HtmlUtils.br() + link);
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
        addSessionFolder(request, toGroup);
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
            Rectangle2D.Double rect = getBounds(children);
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
     * @param children _more_
     *
     * @return _more_
     */
    public Rectangle2D.Double getBounds(List<Entry> children) {
        Rectangle2D.Double rect = null;

        for (Entry child : children) {
            if ( !child.hasAreaDefined() && !child.hasLocationDefined()) {
                continue;
            }


            if (rect == null) {
                rect = child.getBounds();
            } else {
                rect.add(child.getBounds());
            }
        }

        return rect;
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
            request.ensureAuthToken();

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
        for (String id : StringUtil.split(fromIds, ",", true, true)) {
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
                if (textFromUser.startsWith("<wiki>")) {
                    textFromUser = Utils.concatString("<wiki>", extraDesc,
                            textFromUser.substring("<wiki>".length()));
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
            request.ensureAuthToken();
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

                entry.initEntry(name, desc, parent, request.getUser(),
                                new Resource(), "", date.getTime(),
                                date.getTime(), date.getTime(),
                                date.getTime(), values);

                addNewEntry(request, entry);
                //TODO - check for extra desc here
                if (entries.size() > 0) {
                    List<Entry> newEntries = processEntryCopyAsynch(request,
                                                 entry, entries, null, null);
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

        sb.append(HtmlUtils.hidden(ARG_FROM, fromIds));
        sb.append(HtmlUtils.hidden(ARG_TYPE, type));

        String label = isWiki
                       ? "Publish Wiki Page"
                       : "Publish Blog Post";
        sb.append(HtmlUtils.sectionOpen(msg(label)));

        sb.append(HtmlUtils.formTable());
        sb.append(
            HtmlUtils.formEntry(
                "",
                HtmlUtils.buttons(
                    HtmlUtils.submit(msg("Publish"), ARG_CONFIRM),
                    HtmlUtils.submit(msg("Cancel"), ARG_CANCEL))));

        sb.append(HtmlUtils.formEntry(msgLabel("Name"),
                                      HtmlUtils.input(ARG_NAME,
                                          request.getString(ARG_NAME, ""),
                                          HtmlUtils.SIZE_70)));

        String textWidget = HtmlUtils.textArea(ARG_DESCRIPTION + "_extra",
                                "", 5, 120, "");
        sb.append(HtmlUtils.formEntryTop(msgLabel("Description"),
                                         textWidget));

        sb.append(HtmlUtils.comment("The description"));
        String encodedDesc = Utils.encodeBase64(desc.getBytes());
        sb.append(HtmlUtils.hidden(ARG_DESCRIPTION + "_encoded",
                                   encodedDesc));

        String buttons =
            getRepository().getWikiManager().makeWikiEditBar(request, dummy,
                ARG_DESCRIPTION) + HtmlUtils.br();





        String select = getRepository().getHtmlOutputHandler().getSelect(
                            request, ARG_TO, HtmlUtils.img(
                                getRepository().getIconUrl(
                                    ICON_FOLDER_OPEN)) + HtmlUtils.space(1)
                                        + msg("Select")
                                        + HtmlUtils.space(
                                            1), true, "", null, false);




        sb.append(HtmlUtils.hidden(ARG_TO + "_hidden", "",
                                   HtmlUtils.id(ARG_TO + "_hidden")));


        sb.append(HtmlUtils.formEntry(msgLabel("Destination"),
                                      select + HtmlUtils.space(1)
                                      + HtmlUtils.disabledInput(ARG_TO, "",
                                          HtmlUtils.SIZE_60
                                          + HtmlUtils.id(ARG_TO))));


        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.formClose());

        sb.append(HtmlUtils.sectionClose());
        sb.append(HtmlUtils.hr());
        sb.append(HtmlUtils.p());

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

        if ( !getAccessManager().canExportEntry(request, entry)) {
            throw new IllegalArgumentException("Cannot export entry");
        }

        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);

        return getRepository().getZipOutputHandler().toZip(request, "",
                entries, true, true);
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

        if ( !getAccessManager().canDoAction(request, group,
                                             Permission.ACTION_NEW)) {
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
        sb.append(HtmlUtils.hidden(ARG_GROUP, group.getId()));
        sb.append(HtmlUtils.formTable());
        sb.append(HtmlUtils.formEntry(msgLabel("File"),
                                      HtmlUtils.fileInput(ARG_FILE,
                                          HtmlUtils.SIZE_70)));

        sb.append(HtmlUtils.formEntry(msgLabel("Or URL"),
                                      HtmlUtils.input(ARG_URL, "",
                                          HtmlUtils.SIZE_70)));
        if (importTypes.size() > 0) {
            importTypes.add(
                0, new TwoFacedObject("RAMADDA will figure it out", ""));
            sb.append(HtmlUtils.formEntry(msgLabel("Type"),
                                          HtmlUtils.select(ARG_IMPORT_TYPE,
                                              importTypes)));
        }

        sb.append(HtmlUtils.formEntry(msgLabel("Extra"),
                                      HtmlUtils.input("extra",
                                          request.getString("extra", ""),
                                          HtmlUtils.SIZE_70)));

        sb.append(HtmlUtils.formEntry("", HtmlUtils.submit("Submit")));


        sb.append(extraForm);

        HtmlUtils.formTableClose(sb);
        sb.append(HtmlUtils.formClose());

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
                parent = findEntryFromName(request,
                                           request.getString(ARG_GROUP, ""),
                                           request.getUser(), false);
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
        String file = null;

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



        if (Utils.stringDefined(url)) {
            file = getStorageManager().fetchUrl(url).toString();
        }

        if (file == null) {
            file = request.getUploadedFile(ARG_FILE);
        }

        if (file == null) {
            throw new IllegalArgumentException("No file argument given");
        }

        //Check the import handlers
        for (ImportHandler importHandler :
                getRepository().getImportHandlers()) {
            Result result = importHandler.handleRequest(request,
                                getRepository(), file, parent);
            if (result != null) {
                return result;
            }
        }

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
                        String name = IOUtil.getFileTail(ze.getName());
                        File f = getStorageManager().getTmpFile(request,
                                     name);
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
            IOUtil.close(fis);
            getStorageManager().deleteFile(new File(file));
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


        List<Entry> newEntries = processEntryXml(request, root, parent,
                                     origFileToStorage);


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

        sb.append(msgHeader("Imported entries"));
        sb.append("<ul>");

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
                                       Hashtable<String,
                                           File> origFileToStorage)
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

            Entry entry = createEntryFromXml(request, node, entries,
                                             origFileToStorage, true, false);

            newEntries.add(entry);
            if (XmlUtil.hasAttribute(node, ATTR_ID)) {
                idList.add(new String[] {
                    XmlUtil.getAttribute(node, ATTR_ID, ""),
                    entry.getId() });
            }

            if (XmlUtil.getAttribute(node, ATTR_ADDMETADATA, false)) {
                addInitialMetadata(request,
                                   (List<Entry>) Misc.newList(entry), true,
                                   false);
            } else if (XmlUtil.getAttribute(node, ATTR_ADDSHORTMETADATA,
                                            false)) {
                addInitialMetadata(request,
                                   (List<Entry>) Misc.newList(entry), true,
                                   true);
            }
        }


        for (Element node : associationNodes) {
            String id =
                getAssociationManager().processAssociationXml(request, node,
                    entries, origFileToStorage);
        }

        //Replace any entry re
        for (Entry newEntry : newEntries) {
            newEntry.getTypeHandler().convertIdsFromImport(newEntry, idList);
        }

        addNewEntries(request, newEntries, true);

        return newEntries;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param node _more_
     * @param entries _more_
     * @param files _more_
     * @param checkAccess _more_
     * @param internal _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry createEntryFromXml(Request request, Element node,
                                    Hashtable<String, Entry> entries,
                                    Hashtable<String, File> files,
                                    boolean checkAccess, boolean internal)
            throws Exception {
        String parentId    = XmlUtil.getAttribute(node, ATTR_PARENT, "");
        Entry  parentEntry = (Entry) entries.get(parentId);
        if (parentEntry == null) {
            parentEntry = (Entry) getEntry(request, parentId);
        }
        if (parentEntry == null) {
            parentEntry = (Entry) findEntryFromName(request, parentId,
                    request.getUser(), false);
        }
        if (parentEntry == null) {
            // Lets not check for now. Some entry xml doesn't have a parent
            //            throw new RepositoryUtil.MissingEntryException("Could not find parent:" + parentId);
        }

        Entry entry = createEntryFromXml(request, node, parentEntry, files,
                                         checkAccess, internal);
        String tmpid = XmlUtil.getAttribute(node, ATTR_ID, (String) null);
        if (tmpid != null) {
            entries.put(tmpid, entry);
        }

        return entry;
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

    /**
     * _more_
     *
     * @param request _more_
     * @param node _more_
     * @param parentEntry _more_
     * @param files _more_
     * @param checkAccess _more_
     * @param internal _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry createEntryFromXml(Request request, Element node,
                                    Entry parentEntry,
                                    Hashtable<String, File> files,
                                    boolean checkAccess, boolean internal)
            throws Exception {


        boolean doAnonymousUpload = false;

        String name = cleanupEntryName(Utils.getAttributeOrTag(node,
                          ATTR_NAME, ""));

        String originalId = XmlUtil.getAttribute(node, ATTR_ID,
                                (String) null);


        String category = XmlUtil.getAttribute(node, ATTR_CATEGORY, "");
        String description = XmlUtil.getAttribute(node, ATTR_DESCRIPTION,
                                 (String) null);
        if (description == null) {
            Element descriptionNode = XmlUtil.findChild(node,
                                          TAG_DESCRIPTION);
            if (descriptionNode != null) {
                description = XmlUtil.getChildText(descriptionNode);
                if ((description != null)
                        && XmlUtil.getAttribute(descriptionNode, "encoded",
                            false)) {
                    description =
                        new String(RepositoryUtil.decodeBase64(description));
                }
            }
        }
        if (description == null) {
            description = "";
        }


        if (checkAccess && (parentEntry != null)) {
            if ( !getAccessManager().canDoAction(request, parentEntry,
                    Permission.ACTION_NEW)) {
                if (getAccessManager().canDoAction(request, parentEntry,
                        Permission.ACTION_UPLOAD)) {
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
            File tmp = ((files == null)
                        ? null
                        : files.get(file));
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
            File f = getStorageManager().getTmpFile(request,
                         IOUtil.getFileTail(u.getFile()));
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




        String   id = getRepository().getGUID();

        Resource resource;
        if (file != null) {
            resource = new Resource(file, Resource.TYPE_STOREDFILE);
        } else if (localFile != null) {
            if ( !request.getUser().getAdmin()) {
                throw new IllegalArgumentException(
                    "Only administrators can upload a local file");
            }
            resource = new Resource(localFile, Resource.TYPE_LOCAL_FILE);
        } else if (localFileToMove != null) {
            if ( !request.getUser().getAdmin()) {
                throw new IllegalArgumentException(
                    "Only administrators can upload a local file");
            }
            localFileToMove = getStorageManager().moveToStorage(request,
                    new File(localFileToMove)).toString();

            resource = new Resource(localFileToMove,
                                    Resource.TYPE_STOREDFILE);
        } else if (url != null) {
            resource = new Resource(url, Resource.TYPE_URL);
            int size = XmlUtil.getAttribute(node, ATTR_SIZE, 0);
            resource.setFileSize(size);
        } else {
            resource = new Resource("", Resource.TYPE_UNKNOWN);
        }



        String type = XmlUtil.getAttribute(node, ATTR_TYPE,
                                           TypeHandler.TYPE_FILE);

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


        Date now        = new Date();
        Date createDate = (XmlUtil.hasAttribute(node, ATTR_CREATEDATE)
                           ? getDateHandler().parseDate(
                               XmlUtil.getAttribute(node, ATTR_CREATEDATE))
                           : now);
        Date changeDate = (XmlUtil.hasAttribute(node, ATTR_CHANGEDATE)
                           ? getDateHandler().parseDate(
                               XmlUtil.getAttribute(node, ATTR_CHANGEDATE))
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

        Entry entry = typeHandler.createEntry(id);
        if (originalId != null) {
            entry.putProperty(ATTR_ORIGINALID, originalId);
        }
        entry.initEntry(name, description, parentEntry, request.getUser(),
                        resource, category, createDate.getTime(),
                        changeDate.getTime(), fromDate.getTime(),
                        toDate.getTime(), null);

        if (doAnonymousUpload) {
            initUploadedEntry(request, entry, parentEntry);
        }
        if (XmlUtil.hasAttribute(node, ATTR_LATITUDE)
                && XmlUtil.hasAttribute(node, ATTR_LONGITUDE)) {
            entry.setNorth(Utils.decodeLatLon(XmlUtil.getAttribute(node,
                    ATTR_LATITUDE, "")));
            entry.setSouth(entry.getNorth());
            entry.setWest(Utils.decodeLatLon(XmlUtil.getAttribute(node,
                    ATTR_LONGITUDE, "")));
            entry.setEast(entry.getWest());
        } else {
            entry.setNorth(Utils.decodeLatLon(XmlUtil.getAttribute(node,
                    ATTR_NORTH, entry.getNorth() + "")));
            entry.setSouth(Utils.decodeLatLon(XmlUtil.getAttribute(node,
                    ATTR_SOUTH, entry.getSouth() + "")));
            entry.setEast(Utils.decodeLatLon(XmlUtil.getAttribute(node,
                    ATTR_EAST, entry.getEast() + "")));
            entry.setWest(Utils.decodeLatLon(XmlUtil.getAttribute(node,
                    ATTR_WEST, entry.getWest() + "")));
        }

        entry.setAltitudeTop(XmlUtil.getAttribute(node, ATTR_ALTITUDE_TOP,
                entry.getAltitudeTop()));
        entry.setAltitudeBottom(XmlUtil.getAttribute(node,
                ATTR_ALTITUDE_BOTTOM, entry.getAltitudeBottom()));
        entry.setAltitudeTop(XmlUtil.getAttribute(node, ATTR_ALTITUDE,
                entry.getAltitudeTop()));
        entry.setAltitudeBottom(XmlUtil.getAttribute(node, ATTR_ALTITUDE,
                entry.getAltitudeBottom()));

        NodeList entryChildren = XmlUtil.getElements(node);
        for (Element entryChild : (List<Element>) entryChildren) {
            String tag = entryChild.getTagName();
            if (tag.equals("tag")) {
                getMetadataManager().addMetadata(entry,
                        new Metadata(getRepository().getGUID(),
                                     entry.getId(), "enum_tag", true,
                                     XmlUtil.getChildText(entryChild), "",
                                     "", "", ""));

            } else if (tag.equals(TAG_METADATA)) {
                getMetadataManager().processMetadataXml(entry, entryChild,
                        files, internal);
            } else if (tag.equals(TAG_DESCRIPTION)) {}
            else {
                //                throw new IllegalArgumentException("Unknown tag:"
                //                        + node.getTagName());
            }
        }

        entry.setXmlNode(node);
        entry.getTypeHandler().initializeEntryFromXml(request, entry, node);

        return entry;

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
    public String getEntryLink(Request request, Entry entry, String... args) {
        return getEntryLink(request, entry, false, args);
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
    public String getEntryLink(Request request, Entry entry, boolean addIcon,
                               String... args) {
        try {
            String label = (addIcon
                            ? HtmlUtils.img(
                                getPageHandler().getIconUrl(
                                    request, entry)) + " "
                            : "") + getEntryDisplayName(entry);

            return HtmlUtils.href(getEntryURL(request, entry, args), label);
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
        return request.entryUrl(getRepository().URL_ENTRY_SHOW, entry, args);
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
    public EntryLink getAjaxLink(Request request, Entry entry,
                                 String linkText)
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
    public EntryLink getAjaxLink(Request request, Entry entry,
                                 String linkText, String url)
            throws Exception {
        return getAjaxLink(request, entry, linkText, url, true);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param linkText _more_
     * @param url _more_
     * @param forTreeNavigation _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public EntryLink getAjaxLink(Request request, Entry entry,
                                 String linkText, String url,
                                 boolean forTreeNavigation)
            throws Exception {
        return getAjaxLink(request, entry, linkText, url, forTreeNavigation,
                           null);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param linkText _more_
     * @param url _more_
     * @param forTreeNavigation _more_
     * @param textBeforeEntryLink _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public EntryLink getAjaxLink(Request request, Entry entry,
                                 String linkText, String url,
                                 boolean forTreeNavigation,
                                 String textBeforeEntryLink)
            throws Exception {
        return getAjaxLink(request, entry, linkText, url, forTreeNavigation,
                           textBeforeEntryLink,
                           request.get(ARG_DECORATE, true));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param linkText _more_
     * @param url _more_
     * @param forTreeNavigation _more_
     * @param textBeforeEntryLink _more_
     * @param decorateMetadata _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public EntryLink getAjaxLink(Request request, Entry entry,
                                 String linkText, String url,
                                 boolean forTreeNavigation,
                                 String textBeforeEntryLink,
                                 boolean decorateMetadata)
            throws Exception {

        String  entryShowUrl =
            request.makeUrl(getRepository().URL_ENTRY_SHOW);

        boolean forTreeView  = request.get(ARG_TREEVIEW, false);
        if (url == null) {
            //For now don't use the full entry path
            url = HtmlUtils.url(entryShowUrl, ARG_ENTRYID, entry.getId());
            //            url = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry);
        } else if (forTreeView) {
            url = url.replace("%27", "'");
            url = url.replace("'", "");
        }

        if (forTreeView) {
            String label = getEntryListName(request, entry);
            label = label.replace("'", "\\'");
            url = Utils.concatString("javascript:",
                                     HtmlUtils.call("treeViewClick",
                                         HtmlUtils.jsMakeArgs(true,
                                             entry.getId(), url, label)));
            forTreeNavigation = false;
        }


        //        if(true)
        //            return new EntryLink("","","");


        boolean showUrl     = request.get(ARG_DISPLAYLINK, true);
        boolean showDetails = request.get(ARG_DETAILS, true);
        String  entryId     = entry.getId();
        String  uid         = HtmlUtils.getUniqueId("link_");
        String  output      = "inline";
        String  targetId    = HtmlUtils.getUniqueId("targetspan_");
        String  qtargetId   = HtmlUtils.squote(targetId);
        boolean okToMove    = !request.getUser().getAnonymous();
        String  prefix      = "";

        if (forTreeNavigation) {
            String folderClickUrl = HtmlUtils.url(entryShowUrl, ARG_ENTRYID,
                                        entry.getId(), ARG_OUTPUT, output,
                                        ARG_DETAILS,
                                        Boolean.toString(showDetails),
                                        ARG_DISPLAYLINK,
                                        Boolean.toString(showUrl), forTreeView
                    ? ARG_TREEVIEW
                    : "nop", "true");
            String message = entry.isGroup()
                             ? "Click to open folder"
                             : "Click to view contents";
            String imgClick =
                HtmlUtils.onMouseClick(HtmlUtils.call("folderClick",
                    HtmlUtils.comma(HtmlUtils.squote(uid),
                                    HtmlUtils.squote(folderClickUrl),
                                    HtmlUtils.squote(getDownArrowIcon()))));


            prefix = Utils.concatString(
                HtmlUtils.open(HtmlUtils.TAG_SPAN, "class", "entry-arrow"),
                HtmlUtils.img(
                    getRightArrowIcon(), msg(message),
                    Utils.concatString(
                        HtmlUtils.id("img_" + uid),
                        imgClick)), HtmlUtils.close("span"));
        }

        //        if(true)
        //            return new EntryLink("","","");


        StringBuilder sourceEvent = new StringBuilder();
        StringBuilder targetEvent = new StringBuilder();
        String        entryIcon = getPageHandler().getIconUrl(request, entry);
        String        iconId      = "img_" + uid;
        if (okToMove) {
            if (forTreeNavigation) {
                HtmlUtils.onMouseOver(
                    targetEvent,
                    HtmlUtils.call(
                        "mouseOverOnEntry",
                        HtmlUtils.comma(
                            "event", HtmlUtils.squote(entry.getId()),
                            qtargetId)));


                HtmlUtils.onMouseUp(targetEvent,
                                    HtmlUtils.call("mouseUpOnEntry",
                                        HtmlUtils.comma("event",
                                            HtmlUtils.squote(entry.getId()),
                                            qtargetId)));
            }
            HtmlUtils.onMouseOut(targetEvent,
                                 HtmlUtils.call("mouseOutOnEntry",
                                     HtmlUtils.comma("event",
                                         HtmlUtils.squote(entry.getId()),
                                         qtargetId)));

            HtmlUtils.onMouseDown(
                sourceEvent,
                HtmlUtils.call(
                    "mouseDownOnEntry",
                    HtmlUtils.comma(
                        "event", HtmlUtils.squote(entry.getId()),
                        HtmlUtils.squote(entry.getLabel().replace("'", "")),
                        HtmlUtils.squote(iconId),
                        HtmlUtils.squote(entryIcon))));
        }

        //        if(true)
        //            return new EntryLink("","","");

        StringBuilder imgText = new StringBuilder();
        if (okToMove) {
            imgText.append(msg("Drag to move"));
            imgText.append("; ");
        }
        String imgUrl = null;
        if (entry.getResource().isUrl()) {
            try {
                imgUrl = entry.getTypeHandler().getPathForEntry(request,
                        entry);
                imgText.append(msg("Click to view URL"));
            } catch (Exception exc) {
                imgUrl = "bad image";
                imgText.append("Error:" + exc);
            }

        } else if (entry.getResource().isFile()) {
            if (getAccessManager().canDownload(request, entry)) {
                imgUrl = entry.getTypeHandler().getEntryResourceUrl(request,
                        entry);
                imgText.append(msg("Click to download file"));
            }
        }


        String img = HtmlUtils.img(entryIcon, imgText.toString(),
                                   Utils.concatString(HtmlUtils.id(iconId),
                                       sourceEvent.toString()));

        StringBuilder sb = new StringBuilder();
        HtmlUtils.open(sb, HtmlUtils.TAG_SPAN,
                       HtmlUtils.id(targetId) + targetEvent.toString());

        sb.append(prefix);
        if (imgUrl != null) {
            img = HtmlUtils.href(imgUrl, img);
        }
        sb.append(img);
        sb.append(HtmlUtils.space(1));
        if (textBeforeEntryLink != null) {
            sb.append(textBeforeEntryLink);
        }

        if (decorateMetadata) {
            getMetadataManager().decorateEntry(request, entry, sb, true);
        }
        if (showUrl) {
            HtmlUtils.span(sb, getTooltipLink(request, entry, linkText, url),
                           HtmlUtils.cssClass("entry-link"));
        } else {
            HtmlUtils.span(sb, linkText,
                           Utils.concatString(targetEvent.toString(),
                               HtmlUtils.cssClass("entry-link")));
        }

        HtmlUtils.close(sb, HtmlUtils.TAG_SPAN);
        String folderBlock = ( !forTreeNavigation
                               ? ""
                               : HtmlUtils.div("",
                                   HtmlUtils.attrs(HtmlUtils.ATTR_STYLE,
                                       "display:none;", HtmlUtils.ATTR_CLASS,
                                       CSS_CLASS_FOLDER_BLOCK,
                                       HtmlUtils.ATTR_ID, uid)));

        return new EntryLink(sb.toString(), folderBlock, uid);

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
            url = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry);
        }

        String elementId  = entry.getId();
        String qid        = HtmlUtils.squote(elementId);
        String linkId     = HtmlUtils.getUniqueId("link_");
        String qlinkId    = HtmlUtils.squote(linkId);

        String target     = (request.defined(ARG_TARGET)
                             ? request.getString(ARG_TARGET, "")
                             : null);
        String targetAttr = ((target != null)
                             ? HtmlUtils.attr(HtmlUtils.ATTR_TARGET, target)
                             : "");

        return HtmlUtils.href(url, linkText,
                              HtmlUtils.id(linkId) + targetAttr);
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
        OutputHandler.State state = new OutputHandler.State(entry);
        entry.getTypeHandler().getEntryLinks(request, entry, links);
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
    public void addAttachment(Entry entry, File file, boolean andInsert)
            throws Exception {
        String theFile = getStorageManager().moveToEntryDir(entry,
                             file).getName();
        getMetadataManager().addMetadata(
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
    public String getEntryActionsTable(Request request, Entry entry,
                                       int typeMask)
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
                                       String header)
            throws Exception {




        StringBuilder
            htmlSB          = null,
            exportSB        = null,
            nonHtmlSB       = null,
            actionSB        = null,
            categorySB      = null,
            fileSB          = null;
        int     cnt         = 0;
        boolean needToAddHr = false;
        for (Link link : links) {
            if ( !link.isType(typeMask)) {
                continue;
            }
            StringBuilder sb;
            if (link.isType(OutputType.TYPE_VIEW)) {
                if (htmlSB == null) {
                    htmlSB =
                        new StringBuilder(HtmlUtils.open(HtmlUtils.TAG_DIV,
                            HtmlUtils.cssClass(CSS_CLASS_MENU_GROUP)));
                    cnt++;
                }
                sb = htmlSB;
                //} else if (link.isType(OutputType.TYPE_FEEDS)) {
                //if (nonHtmlSB == null) {
            } else if (link.isType(OutputType.TYPE_FEEDS)) {
                if (exportSB == null) {
                    cnt++;
                    exportSB =
                        new StringBuilder(HtmlUtils.open(HtmlUtils.TAG_DIV,
                            HtmlUtils.cssClass(CSS_CLASS_MENU_GROUP)));
                }
                sb = exportSB;
            } else if (link.isType(OutputType.TYPE_FILE)) {
                if (fileSB == null) {
                    cnt++;
                    fileSB =
                        new StringBuilder(HtmlUtils.open(HtmlUtils.TAG_DIV,
                            HtmlUtils.cssClass(CSS_CLASS_MENU_GROUP)));
                }
                sb = fileSB;
            } else if (link.isType(OutputType.TYPE_OTHER)) {
                if (categorySB == null) {
                    cnt++;
                    categorySB =
                        new StringBuilder(HtmlUtils.open(HtmlUtils.TAG_DIV,
                            HtmlUtils.cssClass(CSS_CLASS_MENU_GROUP)));
                }
                sb = categorySB;
            } else {
                if (actionSB == null) {
                    cnt++;
                    actionSB =
                        new StringBuilder(HtmlUtils.open(HtmlUtils.TAG_DIV,
                            HtmlUtils.cssClass(CSS_CLASS_MENU_GROUP)));
                    //                    actionSB.append("<tr><td class=entrymenulink>" + msg("Edit") +"</td></tr>");
                }
                sb = actionSB;
            }
            //Only add the hr if we have more things in the list
            if (needToAddHr && (sb.length() > 0)) {
                sb.append("</div>");

                sb.append(
                    HtmlUtils.div(
                        "",
                        HtmlUtils.cssClass(CSS_CLASS_MENUITEM_SEPARATOR)));
                sb.append(
                    HtmlUtils.open(
                        HtmlUtils.TAG_DIV,
                        HtmlUtils.cssClass(CSS_CLASS_MENU_GROUP)));
            }
            needToAddHr = link.getHr();
            if (needToAddHr) {
                continue;
            }

            sb.append(HtmlUtils.open(HtmlUtils.TAG_DIV, "class",
                                     CSS_CLASS_MENUITEM));
            if (link.getIcon() == null) {
                sb.append(HtmlUtils.space(1));
            } else {
                sb.append(HtmlUtils.href(link.getUrl(),
                                         HtmlUtils.img(link.getIcon())));
            }
            sb.append(HtmlUtils.space(1));
            sb.append(
                HtmlUtils.href(
                    link.getUrl(), msg(link.getLabel()),
                    HtmlUtils.cssClass(CSS_CLASS_MENUITEM_LINK)));
            sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
        }

        if (returnNullIfNoneMatch && (cnt == 0)) {
            return null;
        }


        StringBuilder menu = new StringBuilder();
        if (header != null) {
            menu.append(
                HtmlUtils.div(
                    header, HtmlUtils.cssClass("ramadda-entry-menu-title")));
        }
        menu.append("<table class=\"ramadda-menu\">");
        HtmlUtils.open(menu, HtmlUtils.TAG_TR, HtmlUtils.ATTR_VALIGN, "top");
        if (fileSB != null) {
            fileSB.append("</div>");
            menu.append(HtmlUtils.tag(HtmlUtils.TAG_TD, "",
                                      HtmlUtils.b(msg("File")) + "<br>"
                                      + fileSB.toString()));
        }
        if (actionSB != null) {
            actionSB.append("</div>");
            menu.append(HtmlUtils.tag(HtmlUtils.TAG_TD, "",
                                      HtmlUtils.b(msg("Edit")) + "<br>"
                                      + actionSB.toString()));
        }

        if (htmlSB != null) {
            htmlSB.append("</div>");
            menu.append(HtmlUtils.tag(HtmlUtils.TAG_TD, "",
                                      HtmlUtils.b(msg("View")) + "<br>"
                                      + htmlSB.toString()));
        }


        if (exportSB != null) {
            exportSB.append("</div>");
            menu.append(HtmlUtils.tag(HtmlUtils.TAG_TD, "",
                                      HtmlUtils.b(msg("Links")) + "<br>"
                                      + exportSB.toString()));
        }

        if (categorySB != null) {
            categorySB.append("</div>");
            menu.append(HtmlUtils.tag(HtmlUtils.TAG_TD, "",
                                      HtmlUtils.b(msg("Data")) + "<br>"
                                      + categorySB.toString()));
        }

        menu.append(HtmlUtils.close(HtmlUtils.TAG_TR));
        menu.append(HtmlUtils.close(HtmlUtils.TAG_TABLE));

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
            entry = findEntryFromName(request, entryId, request.getUser(),
                                      false);
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

        if (entry != null) {
            getSessionManager().setLastEntry(request, entry);
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
        return ID_PREFIX_REMOTE
               + RepositoryUtil.encodeBase64(server.getBytes())
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
        String[] pair = StringUtil.split(id, TypeHandler.ID_DELIMITER, 2);
        if (pair == null) {
            return new String[] { "", "" };
        }
        pair[0] = new String(RepositoryUtil.decodeBase64(pair[0]));

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
            HtmlUtils.url(remoteUrl, ARG_ENTRYID, id, ARG_OUTPUT,
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


                entry = typeHandler.makeSynthEntry(request, parentEntry,
                        syntheticPart);
                if (entry == null) {
                    return null;
                }
            } else {
                entry = createEntryFromDatabase(entryId, abbreviated);
                debug("getEntry: from database:" + entry);
            }
        } catch (Exception exc) {
            logError("creating entry:" + entryId, exc);

            return null;
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
        Statement entryStmt =
            getDatabaseManager().select(Tables.ENTRIES.COLUMNS,
                                        Tables.ENTRIES.NAME,
                                        Clause.eq(Tables.ENTRIES.COL_ID,
                                            entryId));
        try {
            ResultSet results = entryStmt.getResultSet();
            if ( !results.next()) {
                return null;
            }
            String entryType = results.getString(2);
            TypeHandler typeHandler =
                getRepository().getTypeHandler(entryType, true);
            entry = typeHandler.createEntryFromDatabase(results, abbreviated);
            checkEntryFileTime(entry);
        } finally {
            getDatabaseManager().closeAndReleaseConnection(entryStmt);
        }

        return entry;
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
    public List[] getEntries(Request request) throws Exception {
        return getEntries(request, new StringBuilder());
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param searchCriteriaSB _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List[] getEntries(Request request, Appendable searchCriteriaSB)
            throws Exception {
        return getEntries(request, searchCriteriaSB, null);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param searchCriteriaSB _more_
     * @param extraClauses _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List[] getEntries(Request request, Appendable searchCriteriaSB,
                             List<Clause> extraClauses)
            throws Exception {
        TypeHandler typeHandler = getRepository().getTypeHandler(request);
        List<Clause> where = typeHandler.assembleWhereClause(request,
                                 searchCriteriaSB);
        if (extraClauses != null) {
            where.addAll(extraClauses);
        }

        return getEntries(request, where, typeHandler);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param clauses _more_
     * @param typeHandler _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry>[] getEntries(Request request, List<Clause> clauses,
                                    TypeHandler typeHandler)
            throws Exception {
        int skipCnt = request.get(ARG_SKIP, 0);
        SqlUtil.debug = false;
        List<Entry> entries       = new ArrayList<Entry>();
        List<Entry> groups        = new ArrayList<Entry>();
        boolean canDoSelectOffset = getDatabaseManager().canDoSelectOffset();
        Hashtable   seen          = new Hashtable();
        List<Entry> allEntries    = new ArrayList<Entry>();


        Statement statement = typeHandler.select(request,
                                  Tables.ENTRIES.COLUMNS, clauses,
                                  getQueryOrderAndLimit(request, false, null,
                                      new SelectInfo()));

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
                    entry = localTypeHandler.createEntryFromDatabase(results);
                    cacheEntry(entry);
                }
                if (seen.get(entry.getId()) != null) {
                    continue;
                }
                seen.put(entry.getId(), BLANK);
                allEntries.add(entry);
            }
        } finally {
            long t2 = System.currentTimeMillis();
            if ((t2 - t1) > 60 * 1000) {
                getLogManager().logError("Select took a long time:"
                                         + (t2 - t1));
            }
            getDatabaseManager().closeAndReleaseConnection(statement);
        }


        //Only split them into groups and non-groups if we aren't doing an orderby
        if ( !request.exists(ARG_ORDERBY)) {
            for (Entry entry : allEntries) {
                if (entry.isGroup()) {
                    groups.add(entry);
                } else {
                    entries.add(entry);
                }
            }
        } else {
            entries = allEntries;
        }

        entries = getAccessManager().filterEntries(request, entries);
        groups  = getAccessManager().filterEntries(request, groups);


        return (List<Entry>[]) new List[] { groups, entries };
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
        String[] pair = StringUtil.split(id, TypeHandler.ID_DELIMITER, 2);
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
    public Entry addFileEntry(Request request, File newFile, Entry group,
                              String name, User user)
            throws Exception {
        return addFileEntry(request, newFile, group, name, user, null, null);
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
    public Entry addFileEntry(Request request, File newFile, Entry group,
                              String name, User user,
                              TypeHandler typeHandler,
                              EntryInitializer initializer)
            throws Exception {

        String resourceType;

        //Is it a ramadda managed file?
        if (IOUtil.isADescendent(getStorageManager().getRepositoryDir(),
                                 newFile)) {
            resourceType = Resource.TYPE_STOREDFILE;
        } else {
            resourceType = Resource.TYPE_LOCAL_FILE;
        }


        Entry entry = makeEntry(request, new Resource(newFile, resourceType),
                                group, name, "", user, typeHandler,
                                initializer);

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


        if ( !getRepository().getAccessManager().canDoAction(request, group,
                Permission.ACTION_NEW)) {
            throw new AccessException("Cannot add to folder", request);
        }

        if (typeHandler == null) {
            typeHandler =
                getRepository().getTypeHandler(TypeHandler.TYPE_FILE);
        }

        Entry entry = typeHandler.createEntry(getRepository().getGUID());


        Date  dttm  = new Date();
        entry.initEntry(name, "", group, request.getUser(), resource,
                        description, dttm.getTime(), dttm.getTime(),
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
        Entry parent = getEntryManager().findGroup(request,
                           request.getString(ARG_PUBLISH_ENTRY + "_hidden",
                                             ""));
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
        newEntry.setName(request.getString(ARG_PUBLISH_NAME,
                                           newFile.getName()));
        if ( !Utils.stringDefined(newEntry.getName())) {
            newEntry.setName(newFile.getName());
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
            getRepository().addAuthToken(request);
            getAssociationManager().addAssociation(request, associatedEntry,
                    newEntry, "", associationType);
        }

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
        checkColumnSize("name", entry.getName(), MAX_NAME_LENGTH);
        checkColumnSize("description", description, MAX_DESCRIPTION_LENGTH);


        int col = 1;
        //id,type,name,desc,group,user,file,createdata,fromdate,todate
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
        //create date
        getDatabaseManager().setDate(statement, col++, entry.getCreateDate());

        long updateTime = entry.getChangeDate();
        getDatabaseManager().setDate(statement, col++, updateTime);
        try {
            getDatabaseManager().setDate(statement, col,
                                         entry.getStartDate());
            getDatabaseManager().setDate(statement, col + 1,
                                         entry.getEndDate());
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
        insertEntries(request, entries, true);
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
        insertEntries(request, entries, false);
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entries _more_
     * @param isNew _more_
     *
     * @throws Exception _more_
     */
    private void insertEntries(Request request, List<Entry> entries,
                               boolean isNew)
            throws Exception {
        if (entries.size() == 0) {
            return;
        }

        if (isNew) {
            for (Entry theNewEntry : entries) {
                theNewEntry.getTypeHandler().initializeNewEntry(request,
                        theNewEntry);
            }
        }


        //We have our own connection
        Connection connection = getDatabaseManager().getConnection();
        try {
            insertEntriesInner(entries, connection, isNew);
            if ( !isNew) {
                Misc.run(getRepository(), "checkModifiedEntries", entries);
            } else {}
        } finally {
            getDatabaseManager().closeConnection(connection);
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
    private void insertEntriesInner(List<Entry> entries,
                                    Connection connection, boolean isNew)
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


        Misc.run(getRepository(), isNew
                                  ? "checkNewEntries"
                                  : "checkModifiedEntries", entries);

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
            PreparedStatement select =
                SqlUtil.getSelectStatement(
                    connection, "count(" + Tables.ENTRIES.COL_ID + ")",
                    Misc.newList(Tables.ENTRIES.NAME), Clause.and(
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
                    int found = results.getInt(1);
                    if (found == 0) {
                        needToAdd.add(entry);
                    } else {
                        nonUniqueOnes.add(entry);
                    }
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
        return getEntryResourceUrl(request, entry, false);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param full _more_
     *
     * @return _more_
     */
    public String getEntryResourceUrl(Request request, Entry entry,
                                      boolean full) {
        //false - don't show the entry path
        return getEntryResourceUrl(request, entry, full, false);
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
    public String getEntryResourceUrl(Request request, Entry entry,
                                      boolean full, boolean addPath) {
        if (entry.getResource().isUrl()) {
            try {
                return entry.getTypeHandler().getPathForEntry(request, entry);
            } catch (Exception exc) {
                return "Error:" + exc;
            }
        }

        String fileTail = getStorageManager().getFileTail(entry);
        fileTail = HtmlUtils.urlEncodeExceptSpace(fileTail);
        //For now use the full entry path ???? why though ???
        if (addPath && fileTail.equals(entry.getName())) {
            fileTail = entry.getFullName(true);
        }
        if (request.getMakeAbsoluteUrls() || full) {
            return HtmlUtils.url(getFullEntryGetUrl(request) + "/"
                                 + fileTail, ARG_ENTRYID, entry.getId());
        } else {
            return HtmlUtils.url(
                request.makeUrl(getRepository().URL_ENTRY_GET) + "/"
                + fileTail, ARG_ENTRYID, entry.getId());
        }
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getDummyGroup() throws Exception {
        return getDummyGroup("Search Results");
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
        Entry dummyGroup = new Entry(getRepository().getGroupTypeHandler(),
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

        return parentEntry.getTypeHandler().postProcessEntries(request,
                children);
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
    public List<Entry> getChildren(Request request, Entry parentEntry)
            throws Exception {
        List<Entry> children = new ArrayList<Entry>();
        if ( !parentEntry.isGroup()) {
            return children;
        }
        List<Entry>  entries      = new ArrayList<Entry>();
        List<String> ids          = getChildIds(request, parentEntry, null);
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

        boolean isSynthEntry = isSynthEntry(group.getId());
        if (group.getTypeHandler().isSynthType() || isSynthEntry) {
            List<String> ids       = new ArrayList<String>();

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
                return mainEntry.getMasterTypeHandler().getSynthIds(request,
                        mainEntry, group, synthId);
            } catch (Exception exc) {
                getLogManager().logError("Error getting synthIds from:"
                                         + mainEntry, exc);

                return new ArrayList<String>();
            }
        }

        return getChildIdsFromDatabase(request, group, select);
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
        List<String> ids   = new ArrayList<String>();


        if (where != null) {
            where = new ArrayList<Clause>(where);
        } else {
            where = new ArrayList<Clause>();
        }
        where.add(Clause.eq(Tables.ENTRIES.COL_PARENT_GROUP_ID,
                            group.getId()));


        String orderBy = getQueryOrderAndLimit(request, true, group, select);

        TypeHandler typeHandler = getRepository().getTypeHandler(request);
        int         skipCnt     = request.get(ARG_SKIP, 0);
        Statement statement = typeHandler.select(request,
                                  Tables.ENTRIES.COL_ID, where, orderBy);
        SqlUtil.Iterator iter = getDatabaseManager().getIterator(statement);
        ResultSet        results;
        boolean canDoSelectOffset = getDatabaseManager().canDoSelectOffset();

        while ((results = iter.getNext()) != null) {
            String id = results.getString(1);
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
     * @param groups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result changeType(Request request, List<Entry> groups,
                             List<Entry> entries)
            throws Exception {
        /*
        if ( !request.getUser().getAdmin()) {
            return null;
        }
        TypeHandler typeHandler =
            getRepository().getTypeHandler(TypeHandler.TYPE_HOMEPAGE);


        List<Entry> changedEntries = new ArrayList<Entry>();

        entries.addAll(groups);

        for(Entry entry: entries) {
            if(entry.isGroup()) {
                entry.setTypeHandler(typeHandler);
                changedEntries.add(entry);
            }
        }
        insertEntries(request, changedEntries, false);*/
        return new Result("Metadata", new StringBuilder("OK"));
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
            if ( !getAccessManager().canDoAction(request, entry,
                    Permission.ACTION_EDIT)) {
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
                HtmlUtils.href(
                    request.entryUrl(getRepository().URL_ENTRY_SHOW, entry),
                    getEntryDisplayName(entry)));
            sb.append(HtmlUtils.br());
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
            sb.append(getRepository().translate(request,
                    "No metadata added"));
        } else {
            sb.append(changedEntries.size() + " "
                      + getRepository().translate(request,
                          "entries changed"));
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
                    && !getAccessManager().canDoAction(request, theEntry,
                        Permission.ACTION_EDIT)) {
                continue;
            }

            Hashtable extra = new Hashtable();
            getMetadataManager().getMetadata(theEntry);
            boolean changed =
                getMetadataManager().addInitialMetadata(request, theEntry,
                    extra, shortForm);
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
    public Entry parseEntryXml(File xmlFile, boolean internal)
            throws Exception {

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


        Hashtable files = new Hashtable<String, File>();
        files.put("root", xmlFile.getParentFile());
        Entry entry = createEntryFromXml(new Request(getRepository(),
                          getUserManager().getDefaultUser()), root,
                              new Hashtable(), files, false, internal);

        if (internal) {
            for (Element assNode : associationNodes) {
                String fromId = XmlUtil.getAttribute(assNode, ATTR_FROM);
                String toId   = XmlUtil.getAttribute(assNode, ATTR_TO);
                //                if(fromId.equals("this")) fromId  = entry.getId();
                //                if(toId.equals("this")) toId  = entry.getId();
                entry.addAssociation(
                    new Association(
                        getRepository().getGUID(),
                        XmlUtil.getAttribute(assNode, ATTR_NAME, ""),
                        XmlUtil.getAttribute(assNode, ATTR_TYPE, ""), fromId,
                        toId));
            }
        }


        return entry;
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
    public Entry getTemplateEntry(File file) throws Exception {
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
                return parseEntryXml(f, true);
            }
        }

        if (isDirectory) {
            File f = new File(IOUtil.joinDir(file, ".this.ramadda.xml"));
            if (f.exists()) {
                Entry entry = parseEntryXml(f, true);

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
        Entry group = getGroupFromCache(id);
        if (group != null) {
            return group;
        }

        if (isSynthEntry(id) || id.startsWith("catalog:")) {
            return (Entry) getEntry(request, id);
        }


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
        //        synchronized (MUTEX_ENTRY) {
        List<String> toks = (List<String>) StringUtil.split(name,
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
                theChild = makeNewGroup(group, tok, user);
            }
            group = theChild;
        }

        return group;
        //        }
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
        List<String> toks = StringUtil.split(path, Entry.PATHDELIMITER, true,
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


        String fullPath = ((parent == null)
                           ? ""
                           : parent.getFullName()) + Entry.IDDELIMITER + name;
        Entry  group    = getGroupFromCache(fullPath, false);
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



        Entry       group    = getGroupFromCache(fullPath, false);
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
    public Entry findGroupFromName(Request request, String name, User user,
                                   boolean createIfNeeded)
            throws Exception {
        return findGroupFromName(request, name, user, createIfNeeded,
                                 TypeHandler.TYPE_GROUP);
    }

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param name _more_
     * @param user _more_
     * @param createIfNeeded _more_
     * @param lastGroupType _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry findGroupFromName(Request request, String name, User user,
                                   boolean createIfNeeded,
                                   String lastGroupType)
            throws Exception {
        Entry entry = findEntryFromName(request, name, user, createIfNeeded,
                                        lastGroupType);
        if ((entry != null) && (entry.isGroup())) {
            return (Entry) entry;
        }

        return null;
    }


    /**
     * _more_
     *
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
    public Entry findEntryFromName(Request request, String name, User user,
                                   boolean createIfNeeded)
            throws Exception {
        return findEntryFromName(request, name, user, createIfNeeded, null);
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
        List<String> toks = (List<String>) StringUtil.split(name,
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
     * _more_
     *
     *
     * @param request _more_
     * @param name _more_
     * @param user _more_
     * @param createIfNeeded _more_
     * @param lastGroupType _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry findEntryFromName(Request request, String name, User user,
                                   boolean createIfNeeded,
                                   String lastGroupType)
            throws Exception {
        return findEntryFromName(request, name, user, createIfNeeded,
                                 lastGroupType, null);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param name _more_
     * @param user _more_
     * @param createIfNeeded _more_
     * @param lastGroupType _more_
     * @param initializer _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry findEntryFromName(Request request, String name, User user,
                                   boolean createIfNeeded,
                                   String lastGroupType,
                                   EntryInitializer initializer)
            throws Exception {
        return findEntryFromName(request, name, user, createIfNeeded,
                                 lastGroupType, null, initializer);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param name _more_
     * @param user _more_
     * @param createIfNeeded _more_
     * @param lastGroupType _more_
     * @param templateEntry _more_
     * @param initializer _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry findEntryFromName(Request request, String name, User user,
                                   boolean createIfNeeded,
                                   String lastGroupType, Entry templateEntry,
                                   EntryInitializer initializer)
            throws Exception {
        if (name == null) {
            return null;
        }
        name = name.trim();
        if (name.startsWith(Entry.PATHDELIMITER)) {
            name = name.substring(1);
        }
        Entry  rootEntry    = request.getRootEntry();
        String topEntryName = rootEntry.getName();
        if (name.equals(topEntryName)) {
            return rootEntry;
        }
        //Tack on the top group name if its not there
        if ( !name.startsWith(topEntryName + Entry.PATHDELIMITER)) {
            name = topEntryName + Entry.PATHDELIMITER + name;
        }
        //split the list
        List<String> toks = (List<String>) StringUtil.split(name,
                                Entry.PATHDELIMITER, true, true);
        //Now remove the top group

        toks.remove(0);

        Entry currentEntry = rootEntry;

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

                currentEntry = makeNewGroup(currentEntry, childName, user,
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
    public Entry makeNewGroup(Entry parent, String name, User user)
            throws Exception {
        return makeNewGroup(parent, name, user, null);
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
    public Entry makeNewGroup(Entry parent, String name, User user,
                              Entry template)
            throws Exception {
        String groupType = TypeHandler.TYPE_GROUP;
        if (template != null) {
            groupType = template.getTypeHandler().getType();
        }

        return makeNewGroup(parent, name, user, template, groupType);
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
    public Entry makeNewGroup(Entry parent, String name, User user,
                              Entry template, String type)
            throws Exception {
        return makeNewGroup(parent, name, user, template, type, null);
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
    public Entry makeNewGroup(Entry parent, String name, User user,
                              Entry template, String type,
                              EntryInitializer initializer)
            throws Exception {
        //        synchronized (MUTEX_ENTRY) {
        Date date = Utils.extractDate(name);
        if (date == null) {
            date = new Date();
        }

        TypeHandler typeHandler = getRepository().getTypeHandler(type);
        Entry       group       = new Entry(getGroupId(parent), typeHandler);
        group.setName(name);
        group.setDate(date.getTime());
        if (template != null) {
            group.initWith(template, true);
            getRepository().getMetadataManager().initNewEntry(group,
                    initializer);
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
                    (Entry) typeHandler.createEntryFromDatabase(results);
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
        sb.append(HtmlUtils.formEntry(msgLabel("Entry Cache"),
                                      getEntryCache().size() / 2 + ""));
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
            throw new IllegalArgumentException("No folder specified");
        }
        Entry entry = getEntry(request, groupNameOrId, false);
        if (entry != null) {
            if ( !entry.isGroup()) {
                throw new IllegalArgumentException("Not a folder:"
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
    protected List<Object[]> getDescendents(Request request,
                                            List<Entry> entries,
                                            Connection connection,
                                            boolean firstCall,
                                            boolean ignoreSynth,
                                            Object actionId)
            throws Exception {

        boolean        ok       = true;
        List<Object[]> children = new ArrayList();
        for (Entry entry : entries) {
            if ((actionId != null)
                    && !getActionManager().getActionOk(actionId)) {
                ok = false;
            }
            if ( !ok) {
                break;
            }

            if (firstCall) {
                children.add(new Object[] {
                    entry.getId(), entry.getTypeHandler().getType(),
                    entry.getResource().getPath(),
                    entry.getResource().getType(),
                    entry.getTypeHandler().getEntryValues(entry),
                    entry.getParentEntry()
                });
            }
            if ( !entry.isGroup()) {
                continue;
            }

            if (entry.getTypeHandler().isSynthType()
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
                    children.add(new Object[] {
                        childId, childEntry.getType(),
                        childEntry.getResource().getPath(),
                        childEntry.getResource().getType(),
                        entry.getTypeHandler().getEntryValues(entry),
                        entry.getParentEntry()
                    });
                    if (childEntry.isGroup()) {
                        children.addAll(getDescendents(request,
                                (List<Entry>) Misc.newList(childEntry),
                                connection, false, ignoreSynth, actionId));
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

                children.add(new Object[] {
                    childId, childType, resource, resourceType,
                    childEntry.getTypeHandler().getEntryValues(childEntry),
                    childEntry.getParentEntry()
                });

                children.addAll(getDescendents(request,
                        (List<Entry>) Misc.newList(childEntry), connection,
                        false, ignoreSynth, actionId));
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


    /*
        public TypeHandler getSynthEntryTypeHandler(String id)
            throws Exception {
            TypeHandler typeHandler = synthEntryHandlers.get(id);
            if (processFileTypeHandler == null) {
                ProcessFileTypeHandler tmp =
                    new ProcessFileTypeHandler(getRepository(), null);
                tmp.setType("type_process");
                processFileTypeHandler = tmp;
            }

            return processFileTypeHandler;
        }
    */


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getProcessEntry() throws Exception {
        return getRepository().getTypeHandler(
            ProcessFileTypeHandler.TYPE_PROCESS).getSynthTopLevelEntry();
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
            if (request.isAnonymous()) {
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
                            + HtmlUtils.href(getRepository().URL_ENTRY_SHOW
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
    public String getQueryOrderAndLimit(Request request, boolean addOrderBy,
                                        Entry forEntry, SelectInfo select) {

        List<Metadata> metadataList = null;

        Metadata       sortMetadata = null;
        if ((forEntry != null) && !request.exists(ARG_ORDERBY)) {
            try {
                sortMetadata =
                    getMetadataManager().getSortOrderMetadata(request,
                        forEntry);
            } catch (Exception ignore) {}
        }

        String  order     = " DESC ";
        boolean haveOrder = request.exists(ARG_ASCENDING);
        String  by        = null;
        int     max       = ((select == null)
                             ? -1
                             : select.getMaxCount());

        if (max <= 0) {
            max = DB_MAX_ROWS;
        }

        if (forEntry != null) {
            max = forEntry.getTypeHandler().getDefaultQueryLimit(request,
                    forEntry);
        }

        if (sortMetadata != null) {
            haveOrder = true;
            if (Misc.equals(sortMetadata.getAttr2(), "true")) {
                order = " ASC ";
            } else {
                order = " DESC ";
            }
            by = sortMetadata.getAttr1();
            String tmp = sortMetadata.getAttr3();
            if ((tmp != null) && (tmp.length() > 0)) {
                int tmpMax = Integer.parseInt(tmp.trim());
                if (tmpMax > 0) {
                    max = tmpMax;
                    if ( !request.defined(ARG_MAX)) {
                        request.put(ARG_MAX, "" + max);
                    }
                }
            }
        } else {
            by = request.getString(ARG_ORDERBY, (String) null);
            if (request.get(ARG_ASCENDING, false)) {
                order = " ASC ";
            }
        }


        max = request.get(ARG_MAX, max);
        //        System.err.println ("Max:" + max);

        String limitString = BLANK;
        limitString =
            getDatabaseManager().getLimitString(request.get(ARG_SKIP, 0),
                max);


        String orderBy = BLANK;
        if (addOrderBy) {
            orderBy = " ORDER BY " + Tables.ENTRIES.COL_FROMDATE + order;
        }
        //!!CAREFUL HERE!! - sql injection with the ARG_ORDERBY
        //Don't just use the by.
        if (by != null) {
            if (by.equals(SORTBY_FROMDATE)) {
                orderBy = " ORDER BY " + Tables.ENTRIES.COL_FROMDATE + order;
            } else if (by.equals(SORTBY_TODATE)) {
                orderBy = " ORDER BY " + Tables.ENTRIES.COL_TODATE + order;
            } else if (by.equals(SORTBY_TYPE)) {
                orderBy = " ORDER BY " + Tables.ENTRIES.COL_TYPE + order;
            } else if (by.equals(SORTBY_SIZE)) {
                orderBy = " ORDER BY " + Tables.ENTRIES.COL_FILESIZE + order;
            } else if (by.equals(SORTBY_CREATEDATE)) {
                orderBy = " ORDER BY " + Tables.ENTRIES.COL_CREATEDATE
                          + order;
            } else if (by.equals(SORTBY_NAME)) {
                if ( !haveOrder) {
                    order = " ASC ";
                }
                orderBy = " ORDER BY " + Tables.ENTRIES.COL_NAME + order;
            }
        }

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
        List<String> toks = StringUtil.split(dir, "/", true, true);
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
                int index = new Integer(tok).intValue();
                index--;
                List<Entry> children = getEntryManager().getChildren(request,
                                           cwd);
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
                parent = findEntryFromName(request,
                                           request.getString(ARG_GROUP, ""),
                                           request.getUser(), false);
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

        addSessionFolder(request, parent);

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
                                     String baseArg, String value)
            throws Exception {
        Entry theEntry = null;
        if (value.length() > 0) {
            theEntry = getRepository().getEntryManager().getEntry(request,
                    value);
        }
        StringBuffer sb = new StringBuffer();
        String select =
            getRepository().getHtmlOutputHandler().getSelect(request,
                baseArg, "Select", true, null, entry);
        sb.append("\n");
        sb.append(HtmlUtils.hidden(baseArg + "_hidden", value,
                                   HtmlUtils.id(baseArg + "_hidden")));
        sb.append("\n");
        sb.append(HtmlUtils.disabledInput(baseArg, ((theEntry != null)
                ? theEntry.getFullName()
                : ""), HtmlUtils.id(baseArg) + HtmlUtils.SIZE_60) + select);
        sb.append("\n");

        return sb.toString();
    }



}
