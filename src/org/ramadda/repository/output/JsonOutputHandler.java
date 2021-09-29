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

package org.ramadda.repository.output;


//import com.google.gson.*;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.metadata.MetadataType;
import org.ramadda.repository.type.Column;
import org.ramadda.repository.type.TypeHandler;

import org.ramadda.repository.util.ServerInfo;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;


import org.w3c.dom.*;


import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;

import java.text.DateFormat;

import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class JsonOutputHandler extends OutputHandler {

    // Parameters for the output

    /** _more_ */
    public static final String ARG_EXTRACOLUMNS = "extracolumns";

    /** _more_ */
    public static final String ARG_METADATA = "metadata";

    /** _more_ */
    public static final String ARG_LINKS = "links";

    /** _more_ */
    public static final String ARG_ONLYENTRY = "onlyentry";


    /** _more_ */
    public static final OutputType OUTPUT_JSON =
        new OutputType("JSON", "json",
                       OutputType.TYPE_FEEDS | OutputType.TYPE_FORSEARCH, "",
                       ICON_JSON);

    /** _more_ */
    public static final OutputType OUTPUT_JSON_POINT =
        new OutputType("JSON", "json.point", OutputType.TYPE_FEEDS, "",
                       ICON_JSON);



    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public JsonOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_JSON);
        addType(OUTPUT_JSON_POINT);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if ((state.getEntry() != null)
                && (state.getEntry().getName() != null)) {
            links.add(
                makeLink(
                    request, state.getEntry(), OUTPUT_JSON,
                    "/" + IOUtil.stripExtension(state.getEntry().getName())
                    + ".json"));
        }
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> subGroups,
                              List<Entry> entries)
            throws Exception {

        if (group.isDummy()) {
            request.setReturnFilename("Search_Results.json");
        } else {
            request.setReturnFilename(IOUtil.stripExtension(group.getName())
                                      + ".json");
        }
        List<Entry> allEntries = new ArrayList<Entry>();
        if (request.get("ancestors", false)) {
            allEntries.add(group);
	    Entry parent  =group.getParentEntry();
	    while(parent!=null) {
		allEntries.add(parent);
		parent  =parent.getParentEntry();
	    }
	} else if (request.get(ARG_ONLYENTRY, false)) {
            allEntries.add(group);
        } else {
            allEntries.addAll(subGroups);
            allEntries.addAll(entries);
        }
        StringBuilder sb = new StringBuilder();
        if (outputType!=null && outputType.equals(OUTPUT_JSON_POINT)) {
            makePointJson(request, group, allEntries, sb);
        } else {
            makeJson(request, allEntries, sb);
        }
        request.setCORSHeaderOnResponse();

        return new Result("", sb, Json.MIMETYPE);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        request.setReturnFilename(IOUtil.stripExtension(entry.getName())
                                  + ".json");
        List<Entry> allEntries = new ArrayList<Entry>();
        allEntries.add(entry);
        if (request.get("ancestors", false)) {
	    Entry parent  =entry.getParentEntry();
	    while(parent!=null) {
		allEntries.add(parent);
		parent  =parent.getParentEntry();
	    }
	}
        StringBuilder sb = new StringBuilder();
	makeJson(request, allEntries, sb);
        request.setCORSHeaderOnResponse();
        return new Result("", sb, Json.MIMETYPE);
    }


    /**
     * _more_
     *
     * @param header _more_
     * @param name _more_
     * @param label _more_
     * @param type _more_
     * @param attrs _more_
     *
     * @throws Exception _more_
     */
    private void addPointHeader(List<String> header, String name,
                                String label, String type, String... attrs)
            throws Exception {
        List<String> items = new ArrayList<String>();
        Json.quoteAttr(items, "index", "" + header.size());
        Json.quoteAttr(items, "id", name);
        Json.quoteAttr(items, "type", type);
        Json.quoteAttr(items, "label", label);
        for (int i = 0; i < attrs.length; i += 2) {
            Json.quoteAttr(items, attrs[i], attrs[i + 1]);
        }
        if (name.indexOf("date") >= 0) {
            Json.attr(items, "isDate", "true");
        }
        header.add(Json.map(null, items, false).toString());
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param entries _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void makePointJson(Request request, Entry mainEntry,
                              List<Entry> entries, Appendable sb)
            throws Exception {
	entries= EntryUtil.sortEntriesOnDate(entries,false);
        List<String> fields = new ArrayList<String>();
	boolean remote= request.get("remoteRequest",false);


        /*      items.add(Json.quote(entry.getName()));
        items.add(Json.quote(entry.getDescription()));
        items.add(Json.quote(entry.getId()));
        items.add(Json.quote(entry.getTypeHandler().getType()));
        items.add(Json.quote(entry.getTypeHandler().getLabel()));
        items.add(Json.quote(formatDate(entry.getStartDate())));
        items.add(Json.quote(formatDate(entry.getEndDate())));
        items.add(Json.quote(formatDate(entry.getCreateDate())));
        items.add(Json.quote(request.getAbsoluteUrl(getPageHandler().getIconUrl(request, entry))));
        items.add(Json.quote(getEntryManager().getEntryUrl(request, entry)));
        */


        boolean      addSnippets = request.get("addSnippets", false);		

        addPointHeader(fields, "name", "Name", "string");
        addPointHeader(fields, "description", "Description", "string");
	if(addSnippets)
	    addPointHeader(fields, "snippet", "Snippet", "string", "forDisplay",
			   "false");
        addPointHeader(fields, "id", "Id", "string", "forDisplay", "false");
        addPointHeader(fields, "typeid", "Type ID", "enumeration");
        addPointHeader(fields, "type", "Type", "enumeration");
        addPointHeader(fields, "start_date", "Start Date", "date");
        addPointHeader(fields, "end_date", "End Date", "date");
        addPointHeader(fields, "create_date", "Create Date", "date",
                       "forDisplay", "false");
        addPointHeader(fields, "icon", "Icon", "image", "forDisplay",
                       "false");
        addPointHeader(fields, "entry_url", "Entry Url", "url", "forDisplay",
                       "false");

        boolean      addAttributes = request.get("addAttributes", false);
        boolean      addPointUrl = request.get("addPointUrl", false);	
        boolean      addThumbnails = request.get("addThumbnails", false);
        boolean      addImages = request.get("addImages", false);	
	if(addPointUrl)
	    addPointHeader(fields, "pointurl", "Point URL", "url", "forDisplay",
			   "true");

	if(addThumbnails)
	    addPointHeader(fields, "thumbnail", "Thumbnail", "image", "forDisplay",
			   "false");
	if(addImages)
	    addPointHeader(fields, "image", "Image", "image", "forDisplay",
			   "false");	

        TypeHandler  typeHandler   = null;
        List<Column> columns       = null;
        if (addAttributes && (entries.size() > 0)) {
            Entry    entry           = entries.get(0);
            Object[] extraParameters = entry.getValues();
            if (extraParameters != null) {
                typeHandler = entry.getTypeHandler();
                columns     = typeHandler.getColumnsForPointJson();
                for (Column column : columns) {
                    String columnName = column.getName();
                    String type       = column.isDate()
                                        ? "date"
                                        : column.isNumeric()
                                          ? "double"
                                          : column.isBoolean()
                                            ? "enumeration"
                                            : column.isEnumeration()
                            ? "enumeration"
                            : "string";
                    addPointHeader(fields, columnName, column.getLabel(),
                                   type);
                }
            }
        }


        boolean showFileUrl = (entries.size() == 0)
                              ? false
                              : entries.get(0).getResource().hasResource();
        if (showFileUrl) {
            addPointHeader(fields, "file_url", "File Url", "url");
        }
        addPointHeader(fields, "latitude", "Latitude", "double");
        addPointHeader(fields, "longitude", "Longitude", "double");
        addPointHeader(fields, "elevation", "Elevation", "double");


        List<String> values = new ArrayList<String>();
        for (Entry entry : entries) {
            List<String> entryArray = new ArrayList<String>();
            //Note: if the entry is a different type than the first one then
            //the columns will mismatch
            String array = toPointJson(request, entry, addSnippets, addAttributes,addPointUrl, addThumbnails, addImages,
                                       columns, showFileUrl,remote);
            entryArray.add("values");
            entryArray.add(array);
            values.add(Json.map(entryArray, false));
        }

        List<String> topItems = new ArrayList<String>();
        topItems.add("name");
        topItems.add(Json.quote(mainEntry.getName()));
        topItems.add("fields");
        topItems.add(Json.list(null, fields, false).toString());
        topItems.add("data");
        topItems.add(Json.list(null, values, false).toString());
        Json.map(sb, topItems, false);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void makeJson(Request request, List<Entry> entries, Appendable sb)
            throws Exception {
        List<String> items = new ArrayList<String>();
        for (Entry entry : entries) {
            items.add(toJson(request, entry));
        }
        Json.list(sb, items, false);
        //        System.out.println ("JSON:" + Json.list(items));
    }



    /** _more_ */
    private static SimpleDateFormat sdf;

    /** _more_ */
    private static SimpleDateFormat ymdsdf;

    /**
     * _more_
     *
     * @param dttm _more_
     *
     * @return _more_
     */
    private String formatDate(long dttm) {
        return formatDate(new Date(dttm));
    }

    /**
     * _more_
     *
     * @param dttm _more_
     *
     * @return _more_
     */
    private String formatDate(Date dttm) {
        if (sdf == null) {
            sdf = RepositoryUtil.makeDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        }
        synchronized (sdf) {
            return sdf.format(dttm);
        }
    }


    /**
     * _more_
     *
     * @param dttm _more_
     *
     * @return _more_
     */
    private String formatYMD(long dttm) {
        if (ymdsdf == null) {
            ymdsdf = RepositoryUtil.makeDateFormat("yyyy-MM-dd");
        }
        synchronized (ymdsdf) {
            return ymdsdf.format(new Date(dttm));
        }
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
    private String toJson(Request request, Entry entry) throws Exception {

        List<String> items = new ArrayList<String>();
        Json.quoteAttr(items, "id", entry.getId());
        String entryName = entry.getName();
        Json.quoteAttr(items, "name", entryName);
        String displayName = getEntryDisplayName(entry);
        if ( !displayName.equals(entryName)) {
            Json.quoteAttr(items, "displayName", displayName);
        }

	String snippet = getWikiManager().getSnippet(request, entry, true,null);
	if(snippet!=null) {
	    Json.quoteAttr(items, "snippet", snippet);
	}

        Json.quoteAttr(items, "description", entry.getDescription());
        TypeHandler type     = entry.getTypeHandler();
        String      typeJson = type.getJson(request);
        typeJson = Json.mapAndQuote("id", type.getType(), "name",
                                    type.getLabel());
        Json.attr(items, "type", Json.quote(type.getType()));
        Json.attr(items, "typeName", Json.quote(type.getLabel()));
        //
        if (entry.isGroup()) {
            Json.attr(items, "isGroup", "true");
            /*
            List<Entry> children = getEntryManager().getChildren(request, entry);
            List<String> ids = new ArrayList<String>();
            for(Entry child: children) {
                ids.add(Json.quote(child.getId()));
            }
            Json.attr(items, "childEntryIds", Json.list(ids));
            */
        }

        Json.quoteAttr(
            items, "icon",
            request.getAbsoluteUrl(
                getPageHandler().getIconUrl(request, entry)));

        Json.quoteAttr(items, "parent", entry.getParentEntryId());
        if (entry.getIsRemoteEntry()) {
            Json.attr(items, "isRemote", "true");
            ServerInfo server = entry.getRemoteServer();
            Json.attr(items, "remoteRepository",
                      Json.map("url", Json.quote(server.getUrl()), "name",
                               Json.quote(server.getLabel())));
            Json.quoteAttr(items, "remoteUrl", entry.getRemoteUrl());
	    String remoteParent = entry.getRemoteParentEntryId();
	    if(remoteParent!=null) {
		Json.quoteAttr(items, "remoteParent", remoteParent);
	    }
        }

        Json.quoteAttr(items, "startDate", formatDate(entry.getStartDate()));
        Json.quoteAttr(items, "ymd", formatYMD(entry.getStartDate()));
        Json.quoteAttr(items, "endDate", formatDate(entry.getEndDate()));
        Json.quoteAttr(items, "createDate",
                       formatDate(entry.getCreateDate()));

        if (entry.getUser() != null) {
            Json.quoteAttr(items, "creator", entry.getUser().getId());
        }
        if (entry.getResource().isUrl()) {
            Json.quoteAttr(items, "url",
                           entry.getTypeHandler().getPathForEntry(request,
                               entry));
        }

        if (entry.hasAreaDefined()) {
            double[] center = entry.getCenter();
            Json.attr(items, "geometry",
                      Json.map("type", Json.quote("Point"), "coordinates",
                               Json.list("" + center[1], "" + center[0])));
            Json.attr(items, "bbox",
                      Json.list("" + entry.getWest(), "" + entry.getSouth(),
                                "" + entry.getEast(), "" + entry.getNorth()));
        } else if (entry.hasLocationDefined()) {
            Json.attr(items, "geometry",
                      Json.map("type", Json.quote("Point"), "coordinates",
                               Json.list("" + entry.getLongitude(),
                                         "" + entry.getLatitude())));
            Json.attr(items, "bbox",
                      Json.list("" + entry.getLongitude(),
                                "" + entry.getLatitude(),
                                "" + entry.getLongitude(),
                                "" + entry.getLatitude()));
        }

        if (entry.hasAltitudeTop()) {
            Json.attr(items, "altitudeTop", "" + entry.getAltitudeTop());
        } else {
            Json.attr(items, "altitudeTop", "-9999");
        }

        if (entry.hasAltitudeBottom()) {
            Json.attr(items, "altitudeBottom",
                      "" + entry.getAltitudeBottom());
        } else {
            Json.attr(items, "altitudeBottom", "-9999");
        }


        TypeHandler       typeHandler = entry.getTypeHandler();
        List<ServiceInfo> services    = new ArrayList<ServiceInfo>();
        typeHandler.getServiceInfos(request, entry, services);
        List<String> jsonServiceInfos = new ArrayList<String>();
        for (ServiceInfo service : services) {
            jsonServiceInfos.add(Json.map("url",
                                          Json.quote(service.getUrl()),
                                          "relType",
                                          Json.quote(service.getType()),
                                          "name",
                                          Json.quote(service.getName()),
                                          "mimeType",
                                          Json.quote(service.getMimeType())));
        }

        items.add("services");
        items.add(Json.list(jsonServiceInfos));
        //        System.err.println("services:" + Json.list(jsonServiceInfos));

        Resource resource = entry.getResource();
        if (resource != null) {
            if (resource.isUrl()) {
                String temp = resource.getPath();
                if (temp == null) {
                    Json.quoteAttr(items, "filename", "");
                } else {
                    Json.quoteAttr(items, "filename",
                                   java.net.URLEncoder.encode(temp));
                }

                Json.attr(items, "filesize", "" + resource.getFileSize());
                Json.quoteAttr(items, "md5", "");
                //TODO MATIAS            } else if(resource.isFileNoCheck()) {
            } else if (resource.isFile()) {
                Json.quoteAttr(items, "filename",
                               getStorageManager().getFileTail(entry));
                Json.attr(items, "filesize", "" + resource.getFileSize());
                if (resource.getMd5() != null) {
                    Json.quoteAttr(items, "md5", resource.getMd5());
                } else {
                    Json.quoteAttr(items, "md5", "");
                }
            }
        } else {
            Json.quoteAttr(items, "filename", "no resource");
            Json.attr(items, "filesize", "0");
            Json.quoteAttr(items, "md5", "");
        }

        List<String> attrs = new ArrayList<String>();
        List<String> ids   = new ArrayList<String>();

        // Add special columns to the entries depending on the type
        if (request.get(ARG_EXTRACOLUMNS, true)) {
            List<String> extraColumns    = new ArrayList<String>();
            List<String> columnNames     = new ArrayList<String>();
            List<String> columnLabels    = new ArrayList<String>();
            Object[]     extraParameters = entry.getValues();
            if (extraParameters != null) {
                List<Column> columns = entry.getTypeHandler().getColumns();
                for (int i = 0; i < extraParameters.length; i++) {
                    Column column     = columns.get(i);
                    String columnName = column.getName();
                    Object v          = entry.getValue(i);
                    if (v == null) {
                        v = "";
                    }
                    if (v instanceof Date) {
                        v = formatDate((Date) v);
                    }
                    String value = v.toString();
                    columnNames.add(columnName);
                    columnLabels.add(column.getLabel());
                    //                    Json.attr(items, "column." + columnName, Json.quote(value));
                    extraColumns.add(Json.map(new String[] { columnName,
                            Json.quote(value) }));
                    ids.add(columnName);
                    attrs.add(
                        Json.map(
                            "id", Json.quote(columnName), "type",
                            Json.quote("attribute"), "label",
                            Json.quote(column.getLabel()), "value",
                            Json.quote(value), "canshow",
                            Boolean.toString(column.getCanShow())));
                }
            }
        }



        if (request.get(ARG_LINKS, false)) {
            List<String> links = new ArrayList<String>();
            for (Link link :
                    repository.getEntryManager().getEntryLinks(request,
                        entry)) {
                OutputType outputType = link.getOutputType();
                links.add(Json.map(new String[] {
                    "label", Json.quote(link.getLabel()), "type",
                    (outputType == null)
                    ? "unknown"
                    : Json.quote(outputType.toString()), "url",
                    (link.getUrl() == null)
                    ? Json.quote("")
                    : Json.quote(java.net.URLEncoder.encode(link.getUrl())),
                    "icon", Json.quote(link.getIcon())
                }));
            }
            Json.attr(items, "links", Json.list(links));
        }

        if (request.get(ARG_METADATA, true)) {
            List<Metadata> metadataList =
                getMetadataManager().getMetadata(entry);
            if (metadataList != null) {
                for (Metadata metadata : metadataList) {
                    MetadataType metadataType =
                        getMetadataManager().findType(metadata.getType());
                    if (metadataType == null) {
                        continue;
                    }

                    List<String> mapItems   = new ArrayList<String>();
                    List<String> valueItems = new ArrayList<String>();
                    Json.quoteAttr(mapItems, "id", metadata.getId());
                    Json.quoteAttr(mapItems, "type", metadata.getType());
                    Json.quoteAttr(mapItems, "label",
                                   metadataType.getLabel());

                    int attrIdx = 1;
                    //We always add the four attributes to have always the same structure
                    while (attrIdx <= 4) {
                        String attr = metadata.getAttr(attrIdx);
                        if (attr != null) {
                            if (attr.length() > 0) {
                                Json.quoteAttr(valueItems, "attr" + attrIdx,
                                        attr);
                            } else {
                                Json.quoteAttr(valueItems, "attr" + attrIdx,
                                        "");
                            }
                        } else {
                            Json.quoteAttr(valueItems, "attr" + attrIdx, "");
                        }
                        attrIdx++;
                    }

                    mapItems.add("value");
                    mapItems.add(Json.map(valueItems));
                    ids.add(metadata.getId());
                    attrs.add(Json.map(mapItems));
                }
            }
        }

        entry.getTypeHandler().addToJson(request, entry, items, attrs);
        Json.attr(items, "properties", Json.list(attrs, false));

        return Json.map(items);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param addAttributes _more_
     * @param columns _more_
     * @param showFileUrl _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String toPointJson(Request request, Entry entry,boolean addSnippets,
                               boolean addAttributes, boolean addPointUrl, boolean addThumbnails, boolean addImages, List<Column> columns,
                               boolean showFileUrl,boolean remote)
            throws Exception {

        List<String> items = new ArrayList<String>();
        items.add(Json.quote(entry.getName()));
        items.add(Json.quote(entry.getDescription()));
	if(addSnippets) {
	    String snippet= getWikiManager().getRawSnippet(request, entry,true);
	    items.add(Json.quote(snippet!=null?snippet:""));
	}
        items.add(Json.quote(entry.getId()));
        items.add(Json.quote(entry.getTypeHandler().getType()));
        items.add(Json.quote(entry.getTypeHandler().getLabel()));
        items.add(Json.quote(formatDate(entry.getStartDate())));
        items.add(Json.quote(formatDate(entry.getEndDate())));
        items.add(Json.quote(formatDate(entry.getCreateDate())));
        items.add(
            Json.quote(
                request.getAbsoluteUrl(
                    getPageHandler().getIconUrl(request, entry))));
	String url;
	url = getEntryManager().getEntryUrl(request, entry);
        items.add(Json.quote(remote?request.getAbsoluteUrl(url):url));

	if(addPointUrl) {
	    url = entry.getTypeHandler().getUrlForWiki(request,
								  entry, WikiConstants.WIKI_TAG_DISPLAY, new Hashtable(), new ArrayList<String>());
	    
	    items.add(Json.quote(url==null?"":remote?request.getAbsoluteUrl(url):url));
	}


	if(addThumbnails) {
            List<String> urls = new ArrayList<String>();
            getMetadataManager().getThumbnailUrls(request, entry, urls);
	    if(urls.size()>0) {
		url = urls.get(0);
		items.add(Json.quote(url==null?"":remote?request.getAbsoluteUrl(url):url));
	    }    else {
		items.add("null");
	    }
	}


	if(addImages) {
	    if(entry.isImage()) {
		url = entry.getTypeHandler().getEntryResourceUrl(request, entry);
		items.add(Json.quote(url==null?"":remote?request.getAbsoluteUrl(url):url));
	    } else {
		items.add(Json.quote(""));
	    }
	}
        TypeHandler typeHandler = entry.getTypeHandler();
        if (addAttributes) {
            Object[] extraParameters = entry.getValues();
            if ((extraParameters != null) && (columns != null)) {
		System.err.println("entry:" + entry);
		System.err.println("extra:" + extraParameters.length);
		System.err.println("columns:" + columns);
                for (Column column : columns) {
                    Object v = extraParameters[column.getOffset()];
                    if (v == null) {
                        items.add("null");
                    } else {
                        if (column.isDate()) {
                            items.add(Json.quote(formatDate((Date) v)));
                        } else if (column.isNumeric()) {
                            items.add(v.toString());
                        } else {
                            items.add(Json.quote(v.toString()));
                        }
                    }
                }
            }
        }
        if (showFileUrl) {
	    url = entry.getTypeHandler().getEntryResourceUrl(request, entry);
	    items.add(Json.quote(url==null?"":remote?request.getAbsoluteUrl(url):url));
        }
        items.add("" + ((entry.getLatitude() == Entry.NONGEO)
                        ? "null"
                        : entry.getLatitude()));
        items.add("" + ((entry.getLongitude() == Entry.NONGEO)
                        ? "null"
                        : entry.getLongitude()));
        items.add("" + ((entry.getAltitude() == Entry.NONGEO)
                        ? "null"
                        : entry.getAltitude()));

        return Json.list(items);
    }



    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
    }


}
