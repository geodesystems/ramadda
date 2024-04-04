/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.metadata.MetadataType;
import org.ramadda.repository.type.Column;
import org.ramadda.repository.type.TypeHandler;

import org.ramadda.repository.util.ServerInfo;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;

import org.w3c.dom.*;
import java.io.*;
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
    public static final OutputType OUTPUT_JSON = new OutputType("JSON",
                                                     "json",
                                                     OutputType.TYPE_FEEDS|
								OutputType.TYPE_FORSEARCH,
                                                     "", ICON_JSON);

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
                    "/" + IO.stripExtension(state.getEntry().getName())
                    + ".json"));
        }
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     * @param children _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result outputGroup(final Request request, OutputType outputType,
                              final Entry group, final List<Entry> children)
            throws Exception {

        if (group.isDummy()) {
            request.setReturnFilename("Search_Results.json");
        } else {
            request.setReturnFilename(IO.stripExtension(group.getName())
                                      + ".json");
        }
	boolean doSort = !request.defined(ARG_ORDERBY);
        List<Entry> allEntries = new ArrayList<Entry>();
	String entries = request.getString("entries",null);
	if(entries!=null) {
	    doSort=false;
	    for(String id: Utils.split(entries,",",true,true)) {
		Entry entry =  getEntryManager().getEntry(request, id);
		if(entry!=null) allEntries.add(entry);
	    }
	} else   if (request.get("ancestors", false)) {
            allEntries.add(group);
            Entry parent = group.getParentEntry();
            while (parent != null) {
                allEntries.add(parent);
                parent = parent.getParentEntry();
            }
        } else if (request.get(ARG_ONLYENTRY, false)) {
            allEntries.add(group);
        } else {
            allEntries.addAll(children);
        }


        StringBuilder sb = new StringBuilder();
        if ((outputType != null) && outputType.equals(OUTPUT_JSON_POINT)) {
            makePointJson(request, group, allEntries, sb,doSort);
        } else {
	    InputStream is =IO.pipeIt(new IO.PipedThing(){
		    public void run(OutputStream os) {
			PrintStream           pw  = new PrintStream(os);
			try {
			    long t1 = System.currentTimeMillis();
			    makeJson(request, allEntries, pw);
			    long t2 = System.currentTimeMillis();
			    //			    Utils.printTimes("makeJson",t1,t2);
			} catch(Exception exc) {
			    getLogManager().logError("Making JSON:" + group,exc);
			    pw.println("Making JSON:" + exc);
			}
		    }});
	    Result r = makeStream(request,group, is);
	    return r;
        }
        request.setCORSHeaderOnResponse();
        return new Result("", sb, JsonUtil.MIMETYPE);
    }

    private Result makeStream(Request request, Entry entry, InputStream is) throws Exception {
	return request.returnStream(getStorageManager().getOriginalFilename(entry.getResource().getPath()),
				    JU.MIMETYPE,is);	    
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
        request.setReturnFilename(IO.stripExtension(entry.getName())
                                  + ".json");
        List<Entry> allEntries = new ArrayList<Entry>();
        if ( !request.get("children", false)) {
            allEntries.add(entry);
        }
        if (request.get("ancestors", false)) {
            Entry parent = entry.getParentEntry();
            while (parent != null) {
                allEntries.add(parent);
                parent = parent.getParentEntry();
            }
        }
        StringBuilder sb = new StringBuilder();
        makeJson(request, allEntries, sb);
        request.setCORSHeaderOnResponse();

        return new Result("", sb, JsonUtil.MIMETYPE);
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
        JsonUtil.quoteAttr(items, "index", "" + header.size());
        JsonUtil.quoteAttr(items, "id", name);
        JsonUtil.quoteAttr(items, "type", type);
        JsonUtil.quoteAttr(items, "label", label);
        for (int i = 0; i < attrs.length; i += 2) {
            JsonUtil.quoteAttr(items, attrs[i], attrs[i + 1]);
        }
        if (name.indexOf("date") >= 0) {
            JsonUtil.attr(items, "isDate", "true");
        }
        header.add(JsonUtil.map(items));
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
                              List<Entry> entries, Appendable sb,boolean sort)
            throws Exception {

        String entryTypes = request.getString("entryTypes", null);
        if (entryTypes != null) {
            List<String> types = Utils.split(entryTypes, ",", true, true);
            List<Entry>  tmp   = new ArrayList<Entry>();
            for (Entry entry : entries) {
                boolean ok = false;
                for (String type : types) {
                    if (entry.getTypeHandler().isType(type)) {
                        ok = true;

                        break;
                    }
                }
                if (ok) {
                    tmp.add(entry);
                }
            }
            entries = tmp;
        }

        String notentryTypes = request.getString("notEntryTypes", null);
        if (notentryTypes != null) {
            List<String> types = Utils.split(notentryTypes, ",", true, true);
            List<Entry>  tmp   = new ArrayList<Entry>();
            for (Entry entry : entries) {
                boolean ok = true;
                for (String type : types) {
                    if (entry.getTypeHandler().isType(type)) {
                        ok = false;

                        break;
                    }
                }
                if (ok) {
                    tmp.add(entry);
                }
            }
        }
	if(sort) {
	    entries = EntryUtil.sortEntriesOnDate(entries, true);
	}
	//	System.err.println("Json: sort:" + sort +" entries:" + entries);
        List<String> fields     = new ArrayList<String>();
        boolean      remote     = request.get("remoteRequest", false);
        boolean      imagesOnly = request.get("imagesOnly", false);
        boolean addSnippets = request.get("addSnippets", false);

        addPointHeader(fields, "name", "Name", "string");
        addPointHeader(fields, "description", "Description", "string");
        if (addSnippets) {
            addPointHeader(fields, "snippet", "Snippet", "string",
                           "forDisplay", "false");
        }
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

        boolean addAttributes = request.get("addAttributes", false);
        boolean addPointUrl   = request.get("addPointUrl", false);
        boolean addImages     = request.get("addImages", false);
        boolean addThumbnails = request.get("addThumbnails", addImages);
        boolean addMediaUrl   = request.get("addMediaUrl", false);
        if (addPointUrl) {
            addPointHeader(fields, "pointurl", "Point URL", "url",
                           "forDisplay", "true");
        }

        if (addThumbnails) {
            addPointHeader(fields, "thumbnail", "Thumbnail", "image",
                           "forDisplay", "false");
        }
        if (addImages) {
            addPointHeader(fields, "image", "Image", "image", "forDisplay",
                           "false");
        }
        if (addMediaUrl) {
            addPointHeader(fields, "media_url", "Media URL", "url",
                           "forDisplay", "false");
        }


        TypeHandler  typeHandler = null;
        List<Column> columns     = null;
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
            if (imagesOnly) {
                if ( !entry.isImage()) {
                    continue;
                }
            }

            List<String> entryArray = new ArrayList<String>();
            //Note: if the entry is a different type than the first one then
            //the columns will mismatch
            String array = toPointJson(request, entry, addSnippets,
                                       addAttributes, addPointUrl,
                                       addThumbnails, addImages, addMediaUrl,
                                       typeHandler, columns, showFileUrl,
                                       remote);
            entryArray.add("values");
            entryArray.add(array);
            values.add(JsonUtil.map(entryArray));
        }

        List<String> topItems = new ArrayList<String>();
        topItems.add("name");
        topItems.add(JsonUtil.quote(mainEntry.getName()));
        topItems.add("fields");
        topItems.add(JsonUtil.list(null, fields, false).toString());
        topItems.add("data");
        topItems.add(JsonUtil.list(null, values, false).toString());
        JsonUtil.map(sb, topItems);
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
	sb.append(JU.listOpen());
	int cnt=0;
        for (Entry entry : entries) {
	    if(cnt++>=1) sb.append(",");
	    toJson(request, entry,sb);
        }
	sb.append(JU.listClose());
    }



    /** _more_ */
    private static SimpleDateFormat sdf;

    /** _more_ */
    private static SimpleDateFormat ymdsdf;
    private static SimpleDateFormat hhmmsdf;    

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
	if(DateHandler.isNullDate(dttm)) {
	    return DateHandler.NULL_DATE_LABEL;
	}
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
	    Date date = new Date(dttm);
	    if(DateHandler.isNullDate(date)) {
		return DateHandler.NULL_DATE_LABEL;
	    }
            return ymdsdf.format(date);
        }
    }

    private String formatHHMM(long dttm) {
        if (hhmmsdf == null) {
            hhmmsdf = RepositoryUtil.makeDateFormat("HH:mm");
        }
        synchronized (hhmmsdf) {
	    Date date = new Date(dttm);
	    if(DateHandler.isNullDate(date)) {
		return DateHandler.NULL_DATE_LABEL;
	    }
            return hhmmsdf.format(date);
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
    private void toJson(Request request, Entry entry,Appendable sb) throws Exception {
        List<String> items = new ArrayList<String>();
        JsonUtil.quoteAttr(items, "id", entry.getId());
        String entryName = entry.getName();
        JsonUtil.quoteAttr(items, "name", entryName);
	String slug = getRepository().getRepositorySlug();	
	if(stringDefined(slug)) {
	    JsonUtil.quoteAttr(items, "repositorySlug", slug);
	}
        String displayName = getEntryDisplayName(entry);
        if ( !displayName.equals(entryName)) {
            JsonUtil.quoteAttr(items, "displayName", displayName);
        }


        String snippet = getWikiManager().getSnippet(request, entry, true,
                             null);
        if (snippet != null) {
            JsonUtil.quoteAttr(items, "snippet", snippet);
        }


	String mapGlyphs = entry.getTypeHandler().getProperty(entry,"mapglyphs",null);
	if(mapGlyphs!=null) {
            JsonUtil.quoteAttr(items, "mapglyphs", mapGlyphs);
	}


        if (request.get("includecrumbs", false)) {
            if (entry.getParentEntry() != null) {
                JsonUtil.quoteAttr(items, "breadcrumbs",
                                   getPageHandler().getBreadCrumbs(request,
                                       entry.getParentEntry(), null, null,
								   60,-1));
            }
        }

        if (request.get("includedescription", true)) {
            JsonUtil.quoteAttr(items, "description", entry.getDescription());
        }
        boolean canEdit = getAccessManager().canDoEdit(request, entry);
        JsonUtil.attr(items, "canedit", canEdit + "");

        TypeHandler type = entry.getTypeHandler();

        /**
         *  Don't get the typeJson as it takes a *long* time for DbTypes
         * String      typeJson = type.getJson(request);
         * typeJson = JsonUtil.mapAndQuote(Utils.makeList("id", type.getType(), "name", type.getLabel()));
         */
        JsonUtil.attr(items, "type", JsonUtil.quote(type.getType()));
        JsonUtil.attr(items, "typeName", JsonUtil.quote(type.getLabel()));
        //
	if(entry.getTypeHandler().isType("type_point")) {
            JsonUtil.attr(items, "isPoint", "true");
	}
        if (entry.isGroup()) {
            JsonUtil.attr(items, "isGroup", "true");
            /*
            List<Entry> children = getEntryManager().getChildren(request, entry);
            List<String> ids = new ArrayList<String>();
            for(Entry child: children) {
                ids.add(JsonUtil.quote(child.getId()));
            }
            JsonUtil.attr(items, "childEntryIds", JsonUtil.list(ids));
            */
        }

        JsonUtil.quoteAttr(
            items, "icon",
            request.getAbsoluteUrl(
                getPageHandler().getIconUrl(request, entry)));
        JsonUtil.quoteAttr(items, "iconRelative",
                           getPageHandler().getIconUrl(request, entry));

	Entry parent = entry.getParentEntry();
	if(parent!=null) {
	    JsonUtil.quoteAttr(items, "parent", parent.getId());
	    JsonUtil.quoteAttr(items, "parentName", parent.getName());
	    JsonUtil.quoteAttr(
			       items, "parentIcon",
			       request.getAbsoluteUrl(
						      getPageHandler().getIconUrl(request, parent)));
	}
        if (entry.getIsRemoteEntry()) {
            JsonUtil.attr(items, "isRemote", "true");
            ServerInfo server = entry.getRemoteServer();
            JsonUtil.attr(items, "remoteRepository",
                          JsonUtil.map(Utils.makeList("url",
                              JsonUtil.quote(server.getUrl()), "name",
                              JsonUtil.quote(server.getLabel()))));
            JsonUtil.quoteAttr(items, "remoteUrl", entry.getRemoteUrl());
            String remoteParent = entry.getRemoteParentEntryId();
            if (remoteParent != null) {
                JsonUtil.quoteAttr(items, "remoteParent", remoteParent);
            }
        }



        JsonUtil.quoteAttr(items, "startDate",
                           formatDate(entry.getStartDate()));
        JsonUtil.quoteAttr(items, "ymd", formatYMD(entry.getStartDate()));
        JsonUtil.quoteAttr(items, "hhmm", formatHHMM(entry.getStartDate()));
        JsonUtil.quoteAttr(items, "endDate", formatDate(entry.getEndDate()));
        JsonUtil.quoteAttr(items, "createDate",  formatDate(entry.getCreateDate()));
        JsonUtil.quoteAttr(items, "changeDate", formatDate(entry.getChangeDate()));	
        JsonUtil.quoteAttr(items, "startDateFormat",
                           getDateHandler().formatDateShort(request, entry,
							    entry.getStartDate()));
        JsonUtil.quoteAttr(items, "endDateFormat",
                           getDateHandler().formatDateShort(request, entry,
							    entry.getEndDate()));
        JsonUtil.quoteAttr(items, "createDateFormat",
                           getDateHandler().formatDateShort(request, entry,
							    entry.getCreateDate()));
        JsonUtil.quoteAttr(items, "changeDateFormat",
                           getDateHandler().formatDateShort(request, entry,
                               entry.getChangeDate()));
	


	String searchDisplay = entry.getTypeHandler().getSearchDisplayText(request,  entry);
	if(searchDisplay!=null) {
            JsonUtil.quoteAttr(items, "displayHtml", searchDisplay);
	}
	    


        if (entry.getUser() != null) {
            JsonUtil.quoteAttr(items, "creator", entry.getUser().getId());
            JsonUtil.quoteAttr(items, "creatorName", entry.getUser().getName());	    
        }
        if (entry.getResource().isUrl()) {
	    //Catch errors 
	    try {
		String jsonUrl = entry.getTypeHandler().getPathForEntry(request, entry,false);
		JsonUtil.quoteAttr(items, "url",jsonUrl);
	    } catch(Exception exc) {
		getLogManager().logError("Error reading path for entry:"+ entry + " " + entry.getId(),exc);
	    }
        }


        if (entry.hasAreaDefined()) {
            double[] center = entry.getCenter();
            JsonUtil.attr(items, "geometry",
                          JsonUtil.map(Utils.makeList("type",
                              JsonUtil.quote("Point"), "coordinates",
                              JsonUtil.list("" + center[1],
                                            "" + center[0]))));
            JsonUtil.attr(items, "bbox",
                          JsonUtil.list("" + entry.getWest(),
                                        "" + entry.getSouth(),
                                        "" + entry.getEast(),
                                        "" + entry.getNorth()));
        } else if (entry.hasLocationDefined()) {
            JsonUtil.attr(items, "geometry",
                          JsonUtil.map(Utils.makeList("type",
                              JsonUtil.quote("Point"), "coordinates",
                              JsonUtil.list("" + entry.getLongitude(),
                                            "" + entry.getLatitude()))));
            JsonUtil.attr(items, "bbox",
                          JsonUtil.list("" + entry.getLongitude(),
                                        "" + entry.getLatitude(),
                                        "" + entry.getLongitude(),
                                        "" + entry.getLatitude()));
        }

        if (entry.hasAltitudeTop()) {
            JsonUtil.attr(items, "altitudeTop", "" + entry.getAltitudeTop());
        }

        if (entry.hasAltitudeBottom()) {
            JsonUtil.attr(items, "altitudeBottom",
                          "" + entry.getAltitudeBottom());
        }

	JsonUtil.attr(items, "order",    ""+entry.getEntryOrder());


        if (request.get("includeservices", true)) {
            TypeHandler       typeHandler = entry.getTypeHandler();
            List<ServiceInfo> services    = new ArrayList<ServiceInfo>();
            typeHandler.getServiceInfos(request, entry, services);
            List<String> jsonServiceInfos = new ArrayList<String>();
            for (ServiceInfo service : services) {
                jsonServiceInfos.add(JsonUtil.map(Utils.makeList("url",
                        JsonUtil.quote(service.getUrl()), "relType",
                        JsonUtil.quote(service.getType()), "name",
                        JsonUtil.quote(service.getName()), "mimeType",
                        JsonUtil.quote(service.getMimeType()))));
            }

            items.add("services");
            items.add(JsonUtil.list(jsonServiceInfos));
        }
        //        System.err.println("services:" + JsonUtil.list(jsonServiceInfos));

        Resource resource = entry.getResource();
        if (resource != null) {
            if (resource.isUrl()) {
                String temp = resource.getPath();
                JsonUtil.quoteAttr(items, "isurl", "true");
                if (temp == null) {
                    JsonUtil.quoteAttr(items, "filename", "");
                } else {
                    JsonUtil.quoteAttr(items, "filename",
                                       java.net.URLEncoder.encode(temp,
                                           "UTF-8"));
                }

                JsonUtil.attr(items, "filesize", "" + resource.getFileSize());
                JsonUtil.quoteAttr(
                    items, "fileSizeLabel",
                    "" + formatFileLength(resource.getFileSize()));
            } else if (resource.isFile()) {
                JsonUtil.quoteAttr(items, "isfile", "true");
                JsonUtil.quoteAttr(items, "filename",
                                   getStorageManager().getFileTail(entry));
                JsonUtil.attr(items, "filesize", "" + resource.getFileSize());
                if (Utils.stringDefined(resource.getMd5())) {
                    JsonUtil.quoteAttr(items, "md5", resource.getMd5());
                }
            }
        } else {
            JsonUtil.quoteAttr(items, "filename", "no resource");
            JsonUtil.attr(items, "filesize", "0");
            JsonUtil.quoteAttr(items, "md5", "");
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
		if(columns!=null) {
		    for (int i = 0; i < extraParameters.length && i< columns.size(); i++) {
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
			//                    JsonUtil.attr(items, "column." + columnName, JsonUtil.quote(value));
			extraColumns.add(JsonUtil.map(Utils.makeList(columnName,
								     JsonUtil.quote(value))));
			ids.add(columnName);
			attrs.add(JsonUtil.map(Utils.makeList("id",
							      JsonUtil.quote(columnName), "type",
							      JsonUtil.quote("attribute"), "label",
							      JsonUtil.quote(column.getLabel()), "value",
							      JsonUtil.quote(value), "canshow",
							      Boolean.toString(column.getCanShow()))));
		    }
		}
            }
        }




        if (request.get(ARG_LINKS, false)) {
            List<String> links = new ArrayList<String>();
            for (Link link :
                    repository.getEntryManager().getEntryLinks(request,
                        entry)) {
                OutputType outputType = link.getOutputType();
                links.add(JsonUtil.map(Utils.makeList("label",
                        JsonUtil.quote(link.getLabel()), "type",
                        (outputType == null)
                        ? "unknown"
                        : JsonUtil.quote(outputType.toString()), "url",
                        (link.getUrl() == null)
                        ? JsonUtil.quote("")
                        : JsonUtil.quote(
                            java.net.URLEncoder.encode(
                                link.getUrl(), "UTF-8")), "icon",
                                    JsonUtil.quote(link.getIcon()))));
            }
            JsonUtil.attr(items, "links", JsonUtil.list(links));
        }


        if (request.get(ARG_METADATA, true)) {
            List<Metadata> metadataList =
                getMetadataManager().getMetadata(request,entry);
            if (metadataList != null) {
		boolean fileOk = getAccessManager().canDoFile(request, entry);
                for (Metadata metadata : metadataList) {
                    MetadataType metadataType =
                        getMetadataManager().findType(metadata.getType());
                    if (metadataType == null) {
                        continue;
                    }
		    if(!metadataType.getCanView()) {
			continue;
		    }
		    if(!fileOk && metadataType.hasFile()) {
			continue;
		    }
                    List<String> mapItems   = new ArrayList<String>();
                    List<String> valueItems = new ArrayList<String>();
                    JsonUtil.quoteAttr(mapItems, "id", metadata.getId());
                    JsonUtil.quoteAttr(mapItems, "type", metadata.getType());
                    JsonUtil.quoteAttr(mapItems, "label",
                                       metadataType.getLabel());

                    int attrIdx = 1;
                    //We always add the four attributes to have always the same structure
                    while (attrIdx <= 4) {
                        String attr = metadata.getAttr(attrIdx);
                        if (attr != null) {
                            if (attr.length() > 0) {
                                JsonUtil.quoteAttr(valueItems,
                                        "attr" + attrIdx, attr);
                            } else {
                                JsonUtil.quoteAttr(valueItems,
                                        "attr" + attrIdx, "");
                            }
                        } else {
                            JsonUtil.quoteAttr(valueItems, "attr" + attrIdx,
                                    "");
                        }
                        attrIdx++;
                    }

                    mapItems.add("value");
                    mapItems.add(JsonUtil.map(valueItems));
                    ids.add(metadata.getId());
                    attrs.add(JsonUtil.map(mapItems));
                }
            }
        }

        if (request.get("includeproperties", true)) {
            entry.getTypeHandler().addToJson(request, entry, items, attrs);
        }
        JsonUtil.attr(items, "properties", JsonUtil.list(attrs, false));
	sb.append(JU.map(items));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param addSnippets _more_
     * @param addAttributes _more_
     * @param addPointUrl _more_
     * @param addThumbnails _more_
     * @param addImages _more_
     * @param addMediaUrl _more_
     * @param mainTypeHandler _more_
     * @param columns _more_
     * @param showFileUrl _more_
     * @param remote _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String toPointJson(Request request, Entry entry,
                               boolean addSnippets, boolean addAttributes,
                               boolean addPointUrl, boolean addThumbnails,
                               boolean addImages, boolean addMediaUrl,
                               TypeHandler mainTypeHandler,
                               List<Column> columns, boolean showFileUrl,
                               boolean remote)
            throws Exception {

        List<String> items = new ArrayList<String>();
        items.add(JsonUtil.quote(entry.getName()));
        items.add(JsonUtil.quote(entry.getDescription()));
        if (addSnippets) {
            String snippet = getWikiManager().getRawSnippet(request, entry,
                                 true);
            items.add(JsonUtil.quote((snippet != null)
                                     ? snippet
                                     : ""));
        }
        items.add(JsonUtil.quote(entry.getId()));
        items.add(JsonUtil.quote(entry.getTypeHandler().getType()));
        items.add(JsonUtil.quote(entry.getTypeHandler().getLabel()));
        items.add(JsonUtil.quote(formatDate(entry.getStartDate())));
        items.add(JsonUtil.quote(formatDate(entry.getEndDate())));
        items.add(JsonUtil.quote(formatDate(entry.getCreateDate())));
        items.add(
            JsonUtil.quote(
                request.getAbsoluteUrl(
                    getPageHandler().getIconUrl(request, entry))));
        String url;
        url = getEntryManager().getEntryUrl(request, entry);
        items.add(JsonUtil.quote(remote
                                 ? request.getAbsoluteUrl(url)
                                 : url));

        if (addPointUrl) {
            url = entry.getTypeHandler().getUrlForWiki(request, entry,
                    WikiConstants.WIKI_TAG_DISPLAY, new Hashtable(),
                    new ArrayList<String>());

            items.add(JsonUtil.quote((url == null)
                                     ? ""
                                     : remote
                                       ? request.getAbsoluteUrl(url)
                                       : url));
        }

        if (addThumbnails) {
            List<String> urls = new ArrayList<String>();
            getMetadataManager().getThumbnailUrls(request, entry, urls);
            if (urls.size() > 0) {
                url = urls.get(0);
                items.add(JsonUtil.quote((url == null)
                                         ? ""
                                         : remote
                                           ? request.getAbsoluteUrl(url)
                                           : url));
            } else {
                items.add("null");
            }
        }


        if (addImages) {
            if (entry.isImage()) {
                url = entry.getTypeHandler().getEntryResourceUrl(request,
                        entry);
                items.add(JsonUtil.quote((url == null)
                                         ? ""
                                         : remote
                                           ? request.getAbsoluteUrl(url)
                                           : url));
            } else {
                items.add(JsonUtil.quote(""));
            }
        }
        if (addMediaUrl) {
            url = entry.getTypeHandler().getMediaUrl(request, entry);
            items.add(JsonUtil.quote((url == null)
                                     ? ""
                                     : remote
                                       ? request.getAbsoluteUrl(url)
                                       : url));
        }
        TypeHandler typeHandler = entry.getTypeHandler();
        if (addAttributes && (columns != null)) {
            Object[] extraParameters = entry.getValues();
            if ((extraParameters != null)
                    && typeHandler.isType(mainTypeHandler.getType())) {
                //              System.err.println("entry:" + entry);
                //              System.err.println("extra:" + extraParameters.length);
                //              System.err.println("columns:" + columns);
                for (Column column : columns) {
                    Object v = extraParameters[column.getOffset()];
                    if (v == null) {
			if (column.isNumeric()) {
			    items.add("null");
			} else {
			    items.add(JsonUtil.quote(""));
			}
                    } else {
                        if (column.isDate()) {
                            items.add(JsonUtil.quote(formatDate((Date) v)));
                        } else if (column.isNumeric()) {
                            items.add(v.toString());
                        } else {
                            items.add(JsonUtil.quote(v.toString()));
                        }
                    }
                }
            } else {
                for (Column column : columns) {
		    if (column.isNumeric()) {
			items.add("null");
		    } else {
			items.add(JsonUtil.quote(""));
		    }
                }
            }
        }
        if (showFileUrl) {
            url = entry.getTypeHandler().getEntryResourceUrl(request, entry);
            items.add(JsonUtil.quote((url == null)
                                     ? ""
                                     : remote
                                       ? request.getAbsoluteUrl(url)
                                       : url));
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

        return JsonUtil.list(items);
    }



    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {}


}
