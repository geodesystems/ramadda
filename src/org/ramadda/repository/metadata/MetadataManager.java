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

package org.ramadda.repository.metadata;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.database.*;
import org.ramadda.repository.util.FileWriter;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;




import org.ramadda.util.sql.Clause;

import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.lang.reflect.*;

import java.net.*;

import java.sql.ResultSet;
import java.sql.Statement;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import java.util.regex.*;
import java.util.zip.*;




/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class MetadataManager extends RepositoryManager {

    /** _more_ */
    private static final String SUFFIX_SELECT = ".select.";


    /** _more_ */
    private Object MUTEX_METADATA = new Object();


    /** _more_ */
    public RequestUrl URL_METADATA_FORM = new RequestUrl(getRepository(),
                                              "/metadata/form",
                                              "Edit Properties");

    /** _more_ */
    public RequestUrl URL_METADATA_LIST = new RequestUrl(getRepository(),
                                              "/metadata/list",
                                              "Property Listing");

    /** _more_ */
    public RequestUrl URL_METADATA_VIEW = new RequestUrl(getRepository(),
                                              "/metadata/view",
                                              "Property View");

    /** _more_ */
    public RequestUrl URL_METADATA_ADDFORM = new RequestUrl(getRepository(),
                                                 "/metadata/addform",
                                                 "Add Property");

    /** _more_ */
    public RequestUrl URL_METADATA_ADD = new RequestUrl(getRepository(),
                                             "/metadata/add");

    /** _more_ */
    public RequestUrl URL_METADATA_CHANGE = new RequestUrl(getRepository(),
                                                "/metadata/change");




    /** _more_ */
    private Hashtable<String, Hashtable<String, String>> metadataTypeToTemplate =
        new Hashtable<String, Hashtable<String, String>>();

    /** _more_ */
    protected Hashtable distinctMap = new Hashtable();

    /** _more_ */
    private List<MetadataHandler> metadataHandlers =
        new ArrayList<MetadataHandler>();

    /** _more_ */
    private Hashtable<Class, MetadataHandler> metadataHandlerMap =
        new Hashtable<Class, MetadataHandler>();


    /** _more_ */
    protected Hashtable<String, MetadataType> typeMap = new Hashtable<String,
                                                            MetadataType>();

    /** _more_ */
    protected Hashtable<String, MetadataHandler> handlerMap =
        new Hashtable<String, MetadataHandler>();


    /** _more_ */
    private List<MetadataType> metadataTypes = new ArrayList<MetadataType>();


    /** _more_ */
    private List<String> tableNames = new ArrayList<String>();


    /**
     * _more_
     *
     *
     * @param repository _more_
     *
     */
    public MetadataManager(Repository repository) {
        super(repository);
    }


    /** _more_ */
    MetadataHandler dfltMetadataHandler;


    /**
     * _more_
     *
     * @param stringType _more_
     *
     * @return _more_
     */
    public MetadataType findType(String stringType) {
        return typeMap.get(stringType);
    }

    /**
     * _more_
     *
     * @param metadata _more_
     *
     * @return _more_
     */
    public MetadataType getType(Metadata metadata) {
        return typeMap.get(metadata.getType());
    }


    /**
     * _more_
     *
     * @param stringType _more_
     *
     * @return _more_
     */
    public MetadataHandler findHandler(String stringType) {
        MetadataType type = findType(stringType);
        if (type == null) {
            return null;
        }

        return type.getHandler();
    }

    /**
     * _more_
     *
     * @param type _more_
     */
    public void addMetadataType(MetadataType type) {
        metadataTypes.add(type);
        typeMap.put(type.getId(), type);
        handlerMap.put(type.getId(), type.getHandler());
        if (type.getHasDatabaseTable()) {
            tableNames.add(type.getTableName());
        }
    }


    /**
     * _more_
     *
     * @param metadataType _more_
     * @param templateType _more_
     * @param templateContents _more_
     */
    public void addTemplate(String metadataType, String templateType,
                            String templateContents) {
        Hashtable<String, String> templatesForType =
            metadataTypeToTemplate.get(metadataType);
        if (templatesForType == null) {
            templatesForType = new Hashtable<String, String>();
            metadataTypeToTemplate.put(metadataType, templatesForType);

        }
        templatesForType.put(templateType, templateContents);
    }

    /**
     * _more_
     *
     * @param metadataType _more_
     * @param templateType _more_
     *
     * @return _more_
     */
    public String getTemplate(String metadataType, String templateType) {
        Hashtable<String, String> templatesForType =
            metadataTypeToTemplate.get(metadataType);
        if (templatesForType == null) {
            return null;
        }

        return templatesForType.get(templateType);
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
    public String getJsonLD(Request request, Entry entry) throws Exception {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        StringBuilder sb =
            new StringBuilder("\n<script type=\"application/ld+json\">\n");
        List<Metadata> metadataList = getMetadata(entry);
        List<String>   top          = new ArrayList<String>();
        top.add("@context");
        top.add(Json.quote("https://schema.org/"));
        top.add("@type");
        top.add(Json.quote("Dataset"));
        top.add("name");
        top.add(Json.quote(Json.cleanString(entry.getName())));
        top.add("url");
        top.add(Json.quote(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                            entry)));
        String snippet = getWikiManager().getRawSnippet(request, entry,false);
        if ((snippet != null) && (snippet.length() > 0)) {
            top.add("description");
            top.add(Json.quote(Json.cleanString(snippet)));
        }
        if (entry.hasDate()) {
            top.add("temporalCoverage");
            if (entry.getStartDate() == entry.getEndDate()) {
                top.add(
                    Json.quote(sdf.format(new Date(entry.getStartDate()))));
            } else {
                top.add(
                    Json.quote(
                        sdf.format(new Date(entry.getStartDate())) + "/"
                        + sdf.format(new Date(entry.getEndDate()))));
            }

        }
        if (entry.isGeoreferenced()) {
            List<String> geo = new ArrayList<String>();
            geo.add("@type");
            geo.add(Json.quote("Place"));
            geo.add("geo");
            if (entry.hasAreaDefined()) {
                String box = entry.getSouth() + " " + entry.getWest() + " "
                             + entry.getNorth() + " " + entry.getEast();
                geo.add(Json.map("@type", Json.quote("GeoShape"), "box",
                                 Json.quote(box)));
            } else {
                geo.add(Json.map("@type", Json.quote("GeoCoordinates"),
                                 "latitude",
                                 Json.quote("" + entry.getLatitude()),
                                 "longitude",
                                 Json.quote("" + entry.getLongitude())));
            }
            top.add("spatialCoverage");
            top.add(Json.map(geo));
        }

        if (entry.isFile()) {
            top.add("distribution");
            top.add(
                Json.mapAndQuote(
                    "@type", "DataDownload", "contentUrl",
                    getEntryManager().getEntryResourceUrl(
                        request, entry, true, false)));
        }


        List<String> keywords = null;
        List<String> ids      = null;
        for (Metadata md : metadataList) {
            String type = md.getType();
            if (type.equals("content.license")) {
                top.add("license");
                top.add(Json.quote(md.getAttr1()));
            } else if (type.equals("doi_identifier")) {
                if (ids == null) {
                    ids = new ArrayList<String>();
                }
                ids.add(md.getAttr2());
            } else if (type.equals("thredds.creator")) {
                List<String> ctor = new ArrayList<String>();
                ctor.add("@type");
                ctor.add(Json.quote("Organization"));
                ctor.add("name");
                ctor.add(Json.quote(md.getAttr1()));
                ctor.add("url");
                ctor.add(Json.quote(md.getAttr4()));
                ctor.add("contactPoint");
                ctor.add(Json.mapAndQuote("@type", "ContactPoint", "email",
                                          md.getAttr3()));
                top.add("creator");
                top.add(Json.map(ctor));
            } else if (type.equals("enum_gcmdkeyword")
                       || type.equals("content.keyword")
                       || type.equals("enum_tag")
                       || type.equals("thredds.keyword")) {
                if (keywords == null) {
                    keywords = new ArrayList<String>();
                }
                keywords.add(md.getAttr1());
            }
        }
        if (ids != null) {
            top.add("identifier");
            top.add(Json.list(ids, true));
        }
        if (keywords != null) {
            top.add("keywords");
            top.add(Json.list(keywords, true));
        }
        Json.map(sb, top, false);
        sb.append("\n</script>\n");

        return sb.toString();

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param forLink _more_
     *
     * @throws Exception On badness
     */
    public void decorateEntry(Request request, Entry entry, Appendable sb,
                              boolean forLink)
            throws Exception {
        StringBuilder mine = new StringBuilder();
        for (Metadata metadata : getMetadata(entry)) {
            MetadataHandler handler = findMetadataHandler(metadata.getType());
            handler.decorateEntry(request, entry, mine, metadata, forLink);
            if (forLink) {
                //Only do the first one so we don't get multiple thumbnails
                if (mine.length() > 0) {
                    break;
                }
            } else {
                if (mine.length() > 0) {
                    mine.append(HtmlUtils.br());
                }
            }

        }
        sb.append(mine);
    }





    /**
     * _more_
     *
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception On badness
     */
    public void getTextCorpus(Entry entry, Appendable sb) throws Exception {
        for (Metadata metadata : getMetadata(entry)) {
            MetadataHandler handler = findMetadataHandler(metadata.getType());
            handler.getTextCorpus(entry, sb, metadata);
        }
    }


    /**
     * _more_
     *
     * @param oldEntry _more_
     * @param newEntry _more_
     * @param oldMetadata _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Metadata copyMetadata(Entry oldEntry, Entry newEntry,
                                 Metadata oldMetadata)
            throws Exception {
        MetadataHandler handler = findMetadataHandler(oldMetadata.getType());

        return handler.copyMetadata(oldEntry, newEntry, oldMetadata);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param urls _more_
     *
     * @throws Exception On badness
     */
    public void getThumbnailUrls(Request request, Entry entry,
                                 List<String> urls)
            throws Exception {
        for (Metadata metadata : getMetadata(entry)) {
            MetadataHandler handler = findMetadataHandler(metadata.getType());
            handler.getThumbnailUrls(request, entry, urls, metadata);
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return list of name/url pairs
     *
     * @throws Exception _more_
     */
    public List<String[]> getFilelUrls(Request request, Entry entry)
            throws Exception {
        List<String[]> nameUrlPairs = new ArrayList<String[]>();
        for (Metadata metadata : getMetadata(entry)) {
            MetadataHandler handler = findMetadataHandler(metadata.getType());
            handler.getFileUrls(request, entry, nameUrlPairs, metadata);
        }

        return nameUrlPairs;
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
    public List<String> getImageUrls(Request request, Entry entry)
            throws Exception {
        List<String> urls = new ArrayList<String>();
        List<String[]> metadataUrls =
            getMetadataManager().getFilelUrls(request, entry);
        for (String[] pair : metadataUrls) {
            //[0] is the filename,[1] is the url
            if (Utils.isImage(pair[0])) {
                urls.add(pair[1]);
            }
        }

        return urls;
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param type _more_
     * @param checkInherited _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public List<Metadata> findMetadata(Request request, Entry entry,
                                       String type, boolean checkInherited)
            throws Exception {
        return findMetadata(request, entry, type, checkInherited, true);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param type _more_
     * @param checkInherited _more_
     * @param firstOk _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public List<Metadata> findMetadata(Request request, Entry entry,
                                       String type, boolean checkInherited,
                                       boolean firstOk)
            throws Exception {
        List<Metadata> result = new ArrayList<Metadata>();
        findMetadata(request, entry, type, result, checkInherited, firstOk);
        if (result.size() == 0) {
            return null;
        }

        return result;
    }

    /** _more_ */
    public static boolean debug = false;

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
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public List<Metadata> getInheritedMetadata(Request request, Entry entry)
            throws Exception {
        List<Metadata> result = new ArrayList<Metadata>();
        //        this.debug  = true;
        //        EntryManager.debug  =true;
        Entry parent = getEntryManager().getParent(request, entry);
        //        EntryManager.debug  =false;
        if (parent == null) {
            //            debug("METADATA: getInheritedMetadata entry=" + entry.getName() + " parent is NULL");
        } else {
            //            debug("METADATA: getInheritedMetadata entry=" + entry.getName() + " parent:" + parent.getName());
            findInheritedMetadata(request, parent, result);
        }
        this.debug = false;

        return result;
    }

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param result _more_
     *
     * @throws Exception On badness
     */
    private void findInheritedMetadata(Request request, Entry entry,
                                       List<Metadata> result)
            throws Exception {
        debug("METADATA: findInherited: entry=" + entry);
        if (entry == null) {
            return;
        }
        List<Metadata> metadataList = getMetadata(entry);
        debug("METADATA: findInheritedMetadata:" + metadataList);
        for (Metadata metadata : metadataList) {
            if ( !metadata.getInherited()) {
                continue;
            }
            result.add(metadata);
        }
        findInheritedMetadata(request,
                              getEntryManager().getParent(request, entry),
                              result);
    }




    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param type _more_
     * @param result _more_
     * @param checkInherited _more_
     * @param firstTime _more_
     *
     * @throws Exception On badness
     */
    private void findMetadata(Request request, Entry entry, String type,
                              List<Metadata> result, boolean checkInherited,
                              boolean firstTime)
            throws Exception {

        if (entry == null) {
            return;
        }
        if (debug) {
            System.out.println("metadata:" + type + " entry:" + entry);
        }
        for (Metadata metadata : getMetadata(entry)) {
            if (debug) {
                System.out.println("\ttype:" + metadata.getType() + " "
                                   + metadata.getInherited());
            }
            if ( !firstTime && !metadata.getInherited()) {
                if (debug) {
                    System.out.println("\tskip1");
                }

                continue;
            }
            if (type != null) {
                if (metadata.getType().equals(type)) {
                    result.add(metadata);
                }
            } else {
                result.add(metadata);
            }
        }
        if (checkInherited) {
            findMetadata(request,
                         getEntryManager().getParent(request, entry), type,
                         result, checkInherited, false);
        }
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Metadata findMetadata(Request request, Entry entry, String id)
            throws Exception {
        if (entry == null) {
            return null;
        }
        for (Metadata metadata : getMetadata(entry)) {
            if (metadata.getId().equals(id)) {
                return metadata;
            }
        }

        return null;
    }


    /**
     * _more_
     *
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public List<Metadata> getMetadata(Entry entry) throws Exception {
        return getMetadata(entry, null);
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Metadata> getMetadata(Entry entry, String type)
            throws Exception {
        if (entry.isDummy()) {
            return (entry.getMetadata() == null)
                   ? new ArrayList<Metadata>()
                   : getMetadata(entry.getMetadata(), type);
        }
        List<Metadata> metadataList = entry.getMetadata();
        if (metadataList != null) {
            return getMetadata(metadataList, type);
        }

        final List<Metadata> finalMetadataList = new ArrayList();
        Statement stmt =
            getDatabaseManager().select(
                Tables.METADATA.COLUMNS, Tables.METADATA.NAME,
                Clause.eq(Tables.METADATA.COL_ENTRY_ID, entry.getId()),
                getDatabaseManager().makeOrderBy(Tables.METADATA.COL_TYPE));

        getDatabaseManager().iterate(stmt, new SqlUtil.ResultsHandler() {
            public boolean handleResults(ResultSet results) throws Exception {
                int             col     = 1;
                String          type    = results.getString(3);
                MetadataHandler handler = findMetadataHandler(type);
                DatabaseManager dbm     = getDatabaseManager();
                finalMetadataList.add(
                    handler.makeMetadata(
                        dbm.getString(results, col++),
                        dbm.getString(results, col++),
                        dbm.getString(results, col++),
                        results.getInt(col++) == 1,
                        getAttrString(results, col++),
                        getAttrString(results, col++),
                        getAttrString(results, col++),
                        getAttrString(results, col++),
                        dbm.getString(results, col++)));

                return true;
            }
        });

        metadataList = Metadata.sort(finalMetadataList);
        entry.setMetadata(metadataList);

        return getMetadata(metadataList, type);
    }


    /**
     * _more_
     *
     * @param metadata _more_
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Metadata> getMetadata(List<Metadata> metadata, String type)
            throws Exception {
        if (type == null) {
            return metadata;
        }
        List<Metadata> tmp = new ArrayList<Metadata>();
        for (Metadata m : metadata) {
            if (m.getType().equals(type)) {
                tmp.add(m);
            }
        }

        return tmp;
    }

    /**
     * _more_
     *
     * @param results _more_
     * @param col _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getAttrString(ResultSet results, int col)
            throws Exception {
        String s = getDatabaseManager().getString(results, col);
        if ((s != null) && (s.length() > Metadata.MAX_LENGTH)) {
            s = s.substring(0, Metadata.MAX_LENGTH - 1);
        }

        return s;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param extra _more_
     * @param shortForm _more_
     *
     * @return _more_
     */
    public List<Metadata> getInitialMetadata(Request request, Entry entry,
                                             Hashtable extra,
                                             boolean shortForm) {
        List<Metadata> metadataList = new ArrayList<Metadata>();
        for (MetadataHandler handler : getMetadataHandlers()) {
            try {
                handler.getInitialMetadata(request, entry, metadataList,
                                           extra, shortForm);
            } catch (Exception exc) {
                System.err.println(
                    "MetadataManager.getInitialMetadata error: " + exc);
            }
        }

        return metadataList;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param extra _more_
     * @param shortForm _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean addInitialMetadata(Request request, Entry entry,
                                      Hashtable extra, boolean shortForm)
            throws Exception {
        boolean changed = false;
        for (Metadata metadata :
                getInitialMetadata(request, entry, extra, shortForm)) {
            if (addMetadata(entry, metadata, true)) {
                changed = true;
            }
        }
        if (extra.size() > 0) {
            changed = true;
        }

        return changed;
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param value _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean addMetadata(Entry entry, Metadata value) throws Exception {
        return addMetadata(entry, value, false);
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param value _more_
     * @param checkUnique _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean addMetadata(Entry entry, Metadata value,
                               boolean checkUnique)
            throws Exception {
        List<Metadata> metadata = getMetadata(entry);
        if (checkUnique && metadata.contains(value)) {
            return false;
        }
        metadata.add(value);

        return true;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param fileWriter _more_
     * @param doc _more_
     * @param parent _more_
     *
     * @throws Exception On badness
     */
    public void addMetadata(Request request, Entry entry,
                            FileWriter fileWriter, Document doc,
                            Element parent)
            throws Exception {
        List<Metadata> metadataList = getMetadata(entry);
        for (Metadata metadata : metadataList) {
            MetadataHandler metadataHandler = findMetadataHandler(metadata);
            if (metadataHandler == null) {
                continue;
            }
            metadataHandler.addMetadata(request, entry, fileWriter, metadata,
                                        parent);

        }
    }









    /**
     * _more_
     *
     * @return _more_
     */
    public List<MetadataHandler> getMetadataHandlers() {
        return metadataHandlers;
    }




    /**
     * _more_
     *
     * @param metadata _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public MetadataHandler findMetadataHandler(Metadata metadata)
            throws Exception {
        MetadataHandler handler = handlerMap.get(metadata.getType());
        if (handler != null) {
            return handler;
        }
        if (dfltMetadataHandler == null) {
            dfltMetadataHandler = new MetadataHandler(getRepository(), null);
        }

        return dfltMetadataHandler;
    }



    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public MetadataHandler findMetadataHandler(String type) throws Exception {
        MetadataHandler handler = handlerMap.get(type);
        if (handler != null) {
            return handler;
        }
        if (dfltMetadataHandler == null) {
            dfltMetadataHandler = new MetadataHandler(getRepository(), null);
        }

        return dfltMetadataHandler;
    }


    /**
     * _more_
     *
     * @param c _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public MetadataHandler getHandler(Class c) throws Exception {
        MetadataHandler handler = metadataHandlerMap.get(c);
        if (handler == null) {
            Constructor ctor = Misc.findConstructor(c,
                                   new Class[] { Repository.class });
            if (ctor == null) {
                throw new IllegalStateException(
                    "Could not find constructor for MetadataHandler:"
                    + c.getName());
            }

            handler = (MetadataHandler) ctor.newInstance(new Object[] {
                getRepository() });

            metadataHandlers.add(handler);
            metadataHandlerMap.put(c, handler);
        }

        return handler;
    }


    /**
     * _more_
     *
     * @param pluginManager _more_
     * @throws Exception On badness
     */
    public void loadMetadataHandlers(PluginManager pluginManager)
            throws Exception {
        List<String> metadataDefFiles =
            getRepository().getPluginManager().getMetadataDefFiles();
        for (String file : metadataDefFiles) {
            try {
                file = getStorageManager().localizePath(file);
                if (pluginManager.haveSeen(file)) {
                    continue;
                }
                Element root = XmlUtil.getRoot(file, getClass());
                if (root == null) {
                    continue;
                }
                MetadataType.parse(root, this);
            } catch (Exception exc) {
                logError("Error loading metadata handler file:" + file, exc);

                throw exc;
            }

        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     *
     * @return _more_
     * @throws Exception On badness
     */
    public Appendable addToSearchForm(Request request, Appendable sb)
            throws Exception {
        for (MetadataType type : metadataTypes) {
            if ( !type.getSearchable()) {
                continue;
            }
            type.getHandler().addToSearchForm(request, sb, type);
        }

        return sb;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<MetadataType> getMetadataTypes() {
        return metadataTypes;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] getFileUrl(Request request, Entry entry,
                               Metadata metadata)
            throws Exception {
        MetadataType type = getType(metadata);

        return type.getFileUrl(request, entry, metadata);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     * @param attr _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public File getFile(Request request, Entry entry, Metadata metadata,
                        int attr)
            throws Exception {
        MetadataType type = getType(metadata);

        return type.getFile(entry, metadata, attr);
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public List<Metadata> getMetadataFromClipboard(Request request)
            throws Exception {
        List<Metadata> metadata =
            (List<Metadata>) getSessionManager().getSessionProperty(request,
                PROP_METADATA);

        return metadata;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param metadataList _more_
     *
     * @throws Exception On badness
     */
    public void copyMetadataToClipboard(Request request,
                                        List<Metadata> metadataList)
            throws Exception {
        List<Metadata> copies = new ArrayList<Metadata>();
        for (Metadata metadata : metadataList) {
            copies.add(new Metadata(metadata));
        }
        getSessionManager().putSessionProperty(request, PROP_METADATA,
                copies);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Appendable addToBrowseSearchForm(Request request, Appendable sb)
            throws Exception {
        StringBuilder tmp      = new StringBuilder();
        List<String>  titles   = new ArrayList<String>();
        List<String>  contents = new ArrayList<String>();
        sb.append("<ul>");
        for (MetadataType type : metadataTypes) {
            if ( !type.getBrowsable()) {
                continue;
            }
            String link =
                HtmlUtils
                    .href(request
                        .makeUrl(getRepository().getMetadataManager()
                            .URL_METADATA_LIST, ARG_METADATA_TYPE,
                                type.toString()), type.getLabel());

            sb.append("<li>");
            sb.append(link);
            //            type.getHandler().addToBrowseSearchForm(request, tmp, type, titles, contents);
        }
        sb.append("</ul>");

        //        HtmlUtils.makeAccordion(sb, titles, contents);

        return sb;
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param entryChild _more_
     * @param fileMap _more_
     * @param internal _more_
     *
     * @throws Exception On badness
     */
    public void processMetadataXml(Entry entry, Element entryChild,
                                   Hashtable fileMap, boolean internal)
            throws Exception {
        String          type    = XmlUtil.getAttribute(entryChild, ATTR_TYPE);
        MetadataHandler handler = findMetadataHandler(type);
        handler.processMetadataXml(entry, entryChild, fileMap, internal);
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param initializer _more_
     *
     * @throws Exception On badness
     */
    public void initNewEntry(Entry entry, EntryInitializer initializer)
            throws Exception {
        for (Metadata metadata : getMetadata(entry)) {
            MetadataHandler handler = findMetadataHandler(metadata.getType());
            handler.initNewEntry(metadata, entry, initializer);
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Result processMetadataChange(Request request) throws Exception {
        synchronized (MUTEX_METADATA) {
            Entry entry = getEntryManager().getEntry(request);
            Entry parent = getEntryManager().getParent(request, entry);
            boolean canEditParent =
                (parent != null)
                && getAccessManager().canDoAction(request, parent,
                    Permission.ACTION_EDIT);


            if (request.exists(ARG_METADATA_DELETE)) {
                Hashtable args = request.getArgs();
                for (Enumeration keys =
                        args.keys(); keys.hasMoreElements(); ) {
                    String arg = (String) keys.nextElement();
                    if ( !arg.startsWith(ARG_METADATA_ID + SUFFIX_SELECT)) {
                        continue;
                    }
                    getDatabaseManager().delete(Tables.METADATA.NAME,
                            Clause.eq(Tables.METADATA.COL_ID,
                                      request.getString(arg, BLANK)));
                }
            } else {
                List<Metadata> newMetadataList  = new ArrayList<Metadata>();
                List<Metadata> existingMetadata = getMetadata(entry);
                Hashtable<String, Metadata> map = new Hashtable<String,
                                                      Metadata>();
                for (Metadata metadata : existingMetadata) {
                    map.put(metadata.getId(), metadata);
                }

                for (MetadataHandler handler : metadataHandlers) {
                    handler.handleFormSubmit(request, entry, map,
                                             newMetadataList);
                }

                if ( !request.isAnonymous()
                        && request.exists(ARG_METADATA_CLIPBOARD_COPY)) {
                    List<Metadata> toCopy = new ArrayList<Metadata>();
                    for (Metadata metadata : newMetadataList) {
                        if (request.defined(ARG_METADATA_ID + SUFFIX_SELECT
                                            + metadata.getId())) {
                            toCopy.add(metadata);
                        }
                    }
                    copyMetadataToClipboard(request, toCopy);
                }


                if (canEditParent
                        && request.exists(ARG_METADATA_ADDTOPARENT)) {
                    List<Metadata> parentMetadataList = getMetadata(parent);
                    int            cnt                = 0;

                    for (Metadata metadata : newMetadataList) {
                        if (request.defined(ARG_METADATA_ID + SUFFIX_SELECT
                                            + metadata.getId())) {
                            Metadata newMetadata =
                                new Metadata(getRepository().getGUID(),
                                             parent.getId(), metadata);

                            if ( !parentMetadataList.contains(newMetadata)) {
                                insertMetadata(newMetadata);
                                cnt++;
                            }
                        }
                    }
                    parent.setMetadata(null);
                    parent.getTypeHandler().metadataChanged(request, parent);

                    return new Result(request.makeUrl(URL_METADATA_FORM,
                            ARG_ENTRYID, parent.getId(), ARG_MESSAGE,
                            cnt + " "
                            + getRepository().translate(request,
                                "metadata items added")));

                }


                for (Metadata metadata : newMetadataList) {
                    getDatabaseManager().delete(Tables.METADATA.NAME,
                            Clause.eq(Tables.METADATA.COL_ID,
                                      metadata.getId()));
                    insertMetadata(metadata);
                }
            }
            entry.setMetadata(null);
            entry.getTypeHandler().metadataChanged(request, entry);
            Misc.run(getRepository(), "checkModifiedEntries",
                     Misc.newList(entry));

            return new Result(request.makeUrl(URL_METADATA_FORM, ARG_ENTRYID,
                    entry.getId()));
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Result processMetadataList(Request request) throws Exception {
        boolean doCloud = request.getString(ARG_TYPE, "list").equals("cloud");
        String  header;
        if (doCloud) {
            request.put(ARG_TYPE, "list");
            header = HtmlUtils.href(request.getUrl(), msg("List"))
                     + HtmlUtils.span(
                         "&nbsp;|&nbsp;",
                         HtmlUtils.cssClass(
                             CSS_CLASS_SEPARATOR)) + HtmlUtils.b(
                                 msg("Cloud"));
        } else {
            request.put(ARG_TYPE, "cloud");
            header = HtmlUtils.b(msg("List"))
                     + HtmlUtils.span(
                         "&nbsp;|&nbsp;",
                         HtmlUtils.cssClass(
                             CSS_CLASS_SEPARATOR)) + HtmlUtils.href(
                                 request.getUrl(), msg("Cloud"));
        }
        String metadataType     = request.getString(ARG_METADATA_TYPE, "");
        MetadataHandler handler = findMetadataHandler(metadataType);
        MetadataType    type    = handler.findType(metadataType);

        StringBuilder   sb      = new StringBuilder();
        if ( !request.responseAsJson()) {
            sb.append(HtmlUtils.sectionOpen(msg("Browse Metadata")));
            sb.append(HtmlUtils.center(header));
            sb.append(HtmlUtils.hr());
        }
        doMakeTagCloudOrList(request, metadataType, sb, doCloud, 0);
        if (request.responseAsJson()) {
            request.setCORSHeaderOnResponse();

            return new Result("", sb, Json.MIMETYPE);
        }

        sb.append(HtmlUtils.sectionClose());

        return getSearchManager().makeResult(request,
                                             msg(type.getLabel() + " Cloud"),
                                             sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param metadataType _more_
     * @param sb _more_
     * @param doCloud _more_
     * @param threshold _more_
     *
     * @throws Exception On badness
     */
    public void doMakeTagCloudOrList(Request request, String metadataType,
                                     Appendable sb, boolean doCloud,
                                     int threshold)
            throws Exception {

        boolean         doJson  = request.responseAsJson();
        MetadataHandler handler = findMetadataHandler(metadataType);
        MetadataType    type    = handler.findType(metadataType);
        if ((type == null) || (type.getChildren() == null)) {
            if (doJson) {
                sb.append(Json.list(new ArrayList<String>()));
            }

            return;
        }
        MetadataElement           element    = type.getChildren().get(0);
        List<TwoFacedObject>      enumValues = element.getValues();
        Hashtable<String, String> labels     = new Hashtable<String,
                                                   String>();
        if (enumValues != null) {
            for (TwoFacedObject tfo : enumValues) {
                labels.put((String) tfo.getId(), (String) tfo.getLabel());
            }
        }


        String[] values = getDistinctValues(request, handler, type);
        int[]    cnt    = new int[values.length];
        int      max    = -1;
        int      min    = 10000;
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            cnt[i] = 0;
            Statement stmt = getDatabaseManager().select(
                                 SqlUtil.count("*"), Tables.METADATA.NAME,
                                 Clause.and(
                                     Clause.eq(
                                         Tables.METADATA.COL_TYPE,
                                         type.getId()), Clause.eq(
                                             Tables.METADATA.COL_ATTR1,
                                             value)));
            ResultSet results = stmt.getResultSet();
            if ( !results.next()) {
                continue;
            }
            cnt[i] = results.getInt(1);
            max    = Math.max(cnt[i], max);
            min    = Math.min(cnt[i], min);
            getDatabaseManager().closeAndReleaseConnection(stmt);
        }
        int    diff         = max - min;
        double distribution = diff / 5.0;
        if ( !doCloud) {
            List tuples = new ArrayList();
            for (int i = 0; i < values.length; i++) {
                if (cnt[i] > threshold) {
                    tuples.add(new Object[] { new Integer(cnt[i]),
                            values[i] });
                }
            }
            tuples = Misc.sortTuples(tuples, false);

            if (doJson) {
                List<String> maps = new ArrayList<String>();
                for (int i = 0; i < tuples.size(); i++) {
                    Object[] tuple = (Object[]) tuples.get(i);
                    String   value = (String) tuple[1];

                    String   label = labels.get(value);
                    if (label == null) {
                        label = value;
                    }
                    maps.add(Json.map("count", tuple[0].toString(), "value",
                                      Json.quote(value), "label",
                                      Json.quote(label)));
                }
                sb.append(Json.list(maps));
            } else {
                sb.append(HtmlUtils.formTable());
                sb.append(HtmlUtils.row(HtmlUtils.cols(HtmlUtils.b("Count"),
                        HtmlUtils.b(type.getLabel()))));
                for (int i = 0; i < tuples.size(); i++) {
                    Object[] tuple = (Object[]) tuples.get(i);
                    sb.append("<tr><td width=\"1%\" align=right>");
                    sb.append(tuple[0].toString());
                    sb.append("</td><td>");
                    String value = (String) tuple[1];
                    sb.append(HtmlUtils.href(handler.getSearchUrl(request,
                            type, value), value));
                    sb.append("</td></tr>");
                }
                sb.append(HtmlUtils.formTableClose());
            }

        } else {
            for (int i = 0; i < values.length; i++) {
                if (cnt[i] <= threshold) {
                    continue;
                }
                double percent = cnt[i] / distribution;
                int    bin     = (int) (percent * 5);
                String css     = "font-size:" + (12 + bin * 2);
                String value   = values[i];
                String ttValue = value.replace("\"", "'");
                if (value.length() > 30) {
                    value = value.substring(0, 29) + "...";
                }
                sb.append("<span style=\"" + css + "\">");
                String extra = XmlUtil.attrs("alt",
                                             "Count:" + cnt[i] + " "
                                             + ttValue, "title",
                                                 "Count:" + cnt[i] + " "
                                                 + ttValue);
                sb.append(HtmlUtils.href(handler.getSearchUrl(request, type,
                        values[i]), value, extra));
                sb.append("</span>");
                sb.append(" &nbsp; ");
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
     * @throws Exception On badness
     */
    public Result processMetadataView(Request request) throws Exception {
        long           t1           = System.currentTimeMillis();
        Entry          entry        = getEntryManager().getEntry(request);
        List<Metadata> metadataList = getMetadata(entry);
        Metadata metadata = findMetadata(request, entry,
                                         request.getString(ARG_METADATA_ID,
                                             ""));
        if (metadata == null) {
            String attachment = IOUtil.getFileTail(request.getRequestPath());
            for (Metadata md : metadataList) {
                metadata = md;
                MetadataType metadataType =
                    getMetadataManager().findType(metadata.getType());
                if (metadataType == null) {
                    continue;
                }
                MetadataElement element =
                    metadataType.getDisplayImageElement(request, entry,
                        metadata, attachment);

                if (element != null) {
                    return metadataType.processView(request, entry, metadata,
                            element);
                }
            }

            return new Result("", "Could not find metadata");
        }
        long            t2      = System.currentTimeMillis();
        MetadataHandler handler = findMetadataHandler(metadata.getType());
        Result          result = handler.processView(request, entry,
                                     metadata);

        long            t3      = System.currentTimeMillis();
        result = getEntryManager().addEntryHeader(request,
                request.getRootEntry(), result);
        long t4 = System.currentTimeMillis();

        //        Utils.printTimes("metadata", t1,t2,t3,t4);
        return result;
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Result processMetadataForm(Request request) throws Exception {
        Entry         entry = getEntryManager().getEntry(request);
        StringBuilder sb    = new StringBuilder();
        request.appendMessage(sb);

        return processMetadataForm(request, entry, sb);
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
     * @throws Exception On badness
     */
    public Result processMetadataForm(Request request, Entry entry,
                                      Appendable sb)
            throws Exception {
        boolean canEditParent = getAccessManager().canDoAction(request,
                                    getEntryManager().getParent(request,
                                        entry), Permission.ACTION_EDIT);


        List<Metadata> metadataList = getMetadata(entry);
        getPageHandler().entrySectionOpen(request, entry, sb,
                                          "Edit Properties");
        if (metadataList.size() == 0) {
            sb.append(
                getPageHandler().showDialogNote(
                    msg("No properties defined for entry")));
            sb.append(msgLabel("Add new property"));
            makeAddList(request, entry, sb);
        } else {
            sb.append("\n");
            request.uploadFormWithAuthToken(sb, URL_METADATA_CHANGE);
            sb.append("\n");
            sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
            sb.append("\n");
            String buttons = HtmlUtils.buttons(
                                 HtmlUtils.submit(msg("Change")),
                                 HtmlUtils.submit(
                                     msg("Delete selected"),
                                     ARG_METADATA_DELETE), HtmlUtils.submit(
                                         msg("Copy selected to clipboard"),
                                         ARG_METADATA_CLIPBOARD_COPY));
            sb.append(buttons);
            sb.append("\n");
            List<String> titles   = new ArrayList<String>();
            List<String> contents = new ArrayList<String>();
            for (Metadata metadata : metadataList) {
                metadata.setEntry(entry);
                MetadataHandler metadataHandler =
                    findMetadataHandler(metadata);
                if (metadataHandler == null) {
                    continue;
                }
                String[] html = metadataHandler.getForm(request, entry,
                                    metadata, true);
                if (html == null) {
                    continue;
                }

                String cbxId = "cbx_" + metadata.getId();
                String cbx =
                    HtmlUtils.checkbox(
                        ARG_METADATA_ID + SUFFIX_SELECT + metadata.getId(),
                        metadata.getId(), false,
                        HtmlUtils.id(cbxId) + " "
                        + HtmlUtils.attr(
                            HtmlUtils.ATTR_TITLE,
                            msg(
                            "Shift-click: select range; Control-click: toggle all")) + HtmlUtils.attr(
                                HtmlUtils.ATTR_ONCLICK,
                                HtmlUtils.call(
                                    "checkboxClicked",
                                    HtmlUtils.comma(
                                        "event", HtmlUtils.squote("cbx_"),
                                        HtmlUtils.squote(cbxId)))));

                StringBuilder metadataEntry = new StringBuilder();
                HtmlUtils.comment(metadataEntry, "Metadata part begin");
                metadataEntry.append(HtmlUtils.formTable());
                metadataEntry.append(HtmlUtils.formEntry("",
                        cbx + HtmlUtils.space(2) + msg("Select")));
                metadataEntry.append("\n");
                metadataEntry.append(html[1]);
                HtmlUtils.formTableClose(metadataEntry);
                HtmlUtils.comment(metadataEntry, "Metadata part end");
                titles.add(html[0]);
                String content = HtmlUtils.div(
                                     metadataEntry.toString(),
                                     HtmlUtils.cssClass(
                                         "ramadda-metadata-form"));
                contents.add(content);
            }
            sb.append(HtmlUtils.beginInset(10, 10, 10, 10));
            HtmlUtils.makeAccordion(sb, titles, contents);
            sb.append(HtmlUtils.endInset());
            sb.append(buttons);
            HtmlUtils.comment(sb, "Metadata form end");
            sb.append(HtmlUtils.formClose());
            sb.append("\n");
        }

        getPageHandler().entrySectionClose(request, entry, sb);

        return getEntryManager().makeEntryEditResult(request, entry,
                msg("Edit Properties"), sb);

    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Result processMetadataAddForm(Request request) throws Exception {
        StringBuilder sb    = new StringBuilder();
        Entry         entry = getEntryManager().getEntry(request);

        if (request.get(ARG_METADATA_CLIPBOARD_PASTE, false)) {
            getPageHandler().entrySectionOpen(request, entry, sb,
                    "Add Property");
            List<Metadata> clipboard = getMetadataFromClipboard(request);
            if ((clipboard == null) || (clipboard.size() == 0)) {
                sb.append(
                    getPageHandler().showDialogError("Clipboard empty"));
            } else {
                //TODO: file attachments
                for (Metadata copiedMetadata : clipboard) {
                    Metadata newMetadata =
                        new Metadata(getRepository().getGUID(),
                                     entry.getId(), copiedMetadata);
                    insertMetadata(newMetadata);
                }
                entry.setMetadata(null);
                sb.append(
                    getPageHandler().showDialogNote(
                        "Metadata pasted from clipboard"));
            }

            sb.append(HtmlUtils.sectionClose());

            return processMetadataForm(request, entry, sb);
        }

        if ( !request.exists(ARG_METADATA_TYPE)) {
            getPageHandler().entrySectionOpen(request, entry, sb,
                    "Add Property");
            makeAddList(request, entry, sb);
        } else {
            String          type = request.getString(ARG_METADATA_TYPE,
                                       BLANK);
            MetadataHandler handler = findMetadataHandler(type);
            if (handler != null) {
                MetadataType metadataType = handler.findType(type);
                getPageHandler().entrySectionOpen(request, entry, sb,
                        msgLabel("Add Property") + metadataType.getLabel());
                handler.makeAddForm(request, entry, metadataType, sb);

            }
        }

        sb.append(HtmlUtils.sectionClose());

        return getEntryManager().makeEntryEditResult(request, entry,
                msg("Add Property"), sb);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception On badness
     */
    private void makeAddList(Request request, Entry entry, Appendable sb)
            throws Exception {
        List<String>   groups    = new ArrayList<String>();
        Hashtable      groupMap  = new Hashtable();

        List<Metadata> clipboard = getMetadataFromClipboard(request);
        if ((clipboard != null) && (clipboard.size() > 0)) {
            StringBuilder clipboardSB = new StringBuilder();
            Entry         dummyEntry  = new Entry();
            int           cnt         = 0;
            for (Metadata copied : clipboard) {
                MetadataHandler handler =
                    findMetadataHandler(copied.getType());
                MetadataType type  = handler.getType(copied.getType());
                String       label = type.getTypeLabel(copied);
                String       row   = label;
                clipboardSB.append(row);
                clipboardSB.append("<br>");
                cnt++;
            }

            request.uploadFormWithAuthToken(sb, URL_METADATA_ADDFORM);
            sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
            sb.append(HtmlUtils.hidden(ARG_METADATA_CLIPBOARD_PASTE, "true"));
            sb.append(HtmlUtils.submit(msg("Copy from Clipboard")));
            sb.append(HtmlUtils.formClose());
            sb.append(HtmlUtils.makeShowHideBlock("Clipboard",
                    clipboardSB.toString(), false));
            sb.append(HtmlUtils.p());
        }

        for (MetadataType type : metadataTypes) {
            if (type.getAdminOnly() && !request.getUser().getAdmin()) {
                continue;
            }
            if ( !type.getForUser()) {
                continue;
            }
            if ( !type.isForEntry(entry)) {
                continue;
            }
            String        name    = type.getCategory();
            StringBuilder groupSB = (StringBuilder) groupMap.get(name);
            if (groupSB == null) {
                groupMap.put(name, groupSB = new StringBuilder());
                groups.add(name);
            }
            //            request.uploadFormWithAuthToken(groupSB, URL_METADATA_ADDFORM);
            groupSB.append(request.form(URL_METADATA_ADDFORM));
            groupSB.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
            groupSB.append(HtmlUtils.hidden(ARG_METADATA_TYPE, type.getId()));
            groupSB.append(HtmlUtils.submit(msg("Add") + HtmlUtils.space(1)
                                            + type.getLabel()));
            if (Utils.stringDefined(type.getHelp())) {
                groupSB.append(HtmlUtils.space(2));
                groupSB.append(type.getHelp());
            }
            groupSB.append(HtmlUtils.formClose());
            groupSB.append(HtmlUtils.p());
            groupSB.append(NEWLINE);
        }

        List<String> titles = new ArrayList<String>();
        List<String> tabs   = new ArrayList<String>();

        for (String name : groups) {
            titles.add(name);
            tabs.add(HtmlUtils.insetDiv(groupMap.get(name).toString(), 5, 10,
                                        5, 10));
        }
        HtmlUtils.makeAccordion(sb, titles, tabs);
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Result processMetadataAdd(Request request) throws Exception {
        synchronized (MUTEX_METADATA) {
            Entry entry = getEntryManager().getEntry(request);
            if (request.exists(ARG_CANCEL)) {
                return new Result(request.makeUrl(URL_METADATA_ADDFORM,
                        ARG_ENTRYID, entry.getId()));
            }
            List<Metadata> newMetadata = new ArrayList<Metadata>();
            for (MetadataHandler handler : metadataHandlers) {
                handler.handleAddSubmit(request, entry, newMetadata);
            }

            for (Metadata metadata : newMetadata) {
                insertMetadata(metadata);
            }
            entry.setMetadata(null);
            Misc.run(getRepository(), "checkModifiedEntries",
                     Misc.newList(entry));

            return new Result(request.makeUrl(URL_METADATA_FORM, ARG_ENTRYID,
                    entry.getId()));

        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param handler _more_
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public String[] getDistinctValues(Request request,
                                      MetadataHandler handler,
                                      MetadataType type)
            throws Exception {
        Hashtable myDistinctMap = distinctMap;
        String[]  values        = (String[]) ((myDistinctMap == null)
                ? null
                : myDistinctMap.get(type.getId()));

        if (values == null) {
            Statement stmt = getDatabaseManager().select(
                                 SqlUtil.distinct(Tables.METADATA.COL_ATTR1),
                                 Tables.METADATA.NAME,
                                 Clause.eq(
                                     Tables.METADATA.COL_TYPE, type.getId()));
            values =
                SqlUtil.readString(getDatabaseManager().getIterator(stmt), 1);

            if (myDistinctMap != null) {
                myDistinctMap.put(type.getId(), values);
            }
        }

        return values;
    }


    /**
     * _more_
     *
     * @param metadata _more_
     *
     * @throws Exception On badness
     */
    public void insertMetadata(Metadata metadata) throws Exception {
        distinctMap = null;

        DatabaseManager dbm = getDatabaseManager();
        String          lbl = metadata.getType();
        dbm.executeInsert(Tables.METADATA.INSERT, new Object[] {
            metadata.getId(), metadata.getEntryId(), metadata.getType(),
            new Integer(metadata.getInherited()
                        ? 1
                        : 0),
            dbm.checkString(lbl, metadata.getAttr1(), Metadata.MAX_LENGTH),
            dbm.checkString(lbl, metadata.getAttr2(), Metadata.MAX_LENGTH),
            dbm.checkString(lbl, metadata.getAttr3(), Metadata.MAX_LENGTH),
            dbm.checkString(lbl, metadata.getAttr4(), Metadata.MAX_LENGTH),
            dbm.checkString(lbl, metadata.getExtra(),
                            Metadata.MAX_LENGTH_EXTRA),
        });
    }

    /**
     * _more_
     *
     * @param metadata _more_
     *
     * @throws Exception On badness
     */
    public void deleteMetadata(Metadata metadata) throws Exception {
        getDatabaseManager().delete(Tables.METADATA.NAME,
                                    Clause.eq(Tables.METADATA.COL_ID,
                                        metadata.getId()));
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
    public Metadata getSortOrderMetadata(Request request, Entry entry)
            throws Exception {
        if (entry == null) {
            return null;
        }
        List<Metadata> metadataList = findMetadata(request, entry,
                                          ContentMetadataHandler.TYPE_SORT,
                                          true);
        if ((metadataList != null) && (metadataList.size() > 0)) {
            return metadataList.get(0);
        }

        return null;
    }



}
