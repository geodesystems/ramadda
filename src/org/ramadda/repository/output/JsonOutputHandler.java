/**
Copyright (c) 2008-2024 Geode Systems LLC
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



public class JsonOutputHandler extends OutputHandler {
    public static final String ARG_EXTRACOLUMNS = "extracolumns";
    public static final String ARG_METADATA = "metadata";
    public static final String ARG_LINKS = "links";
    public static final String ARG_ONLYENTRY = "onlyentry";
    public static final OutputType OUTPUT_JSON = new OutputType("JSON",
                                                     "json",
                                                     OutputType.TYPE_FEEDS|
								OutputType.TYPE_FORSEARCH,
                                                     "", ICON_JSON);

    public static final OutputType OUTPUT_JSON_POINT =
        new OutputType("JSON", "json.point", OutputType.TYPE_FEEDS, "",
                       ICON_JSON);


    public JsonOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_JSON);
        addType(OUTPUT_JSON_POINT);
    }


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


    @Override
    public Result outputGroup(final Request request, OutputType outputType,
                              final Entry group, final List<Entry> children)
            throws Exception {

        request.setCORSHeaderOnResponse();
        if (group.isDummy()) {
            request.setReturnFilename("Search_Results.json");
        } else {
            request.setReturnFilename(IO.stripExtension(group.getName())  + ".json");
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
        return new Result("", sb, JU.MIMETYPE);
    }

    private Result makeStream(Request request, Entry entry, InputStream is) throws Exception {
	return request.returnStream(getStorageManager().getOriginalFilename(entry.getResource().getPath()),
				    JU.MIMETYPE,is);	    
    }


    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        request.setCORSHeaderOnResponse();
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
        return new Result("", sb, JU.MIMETYPE);
    }


    private void addPointHeader(List<String> header, String name,
                                String label, String type, String... attrs)
            throws Exception {
        List<String> items = new ArrayList<String>();
        JU.quoteAttr(items, "index", "" + header.size());
        JU.quoteAttr(items, "id", name);
        JU.quoteAttr(items, "type", type);
        JU.quoteAttr(items, "label", label);
        for (int i = 0; i < attrs.length; i += 2) {
            JU.quoteAttr(items, attrs[i], attrs[i + 1]);
        }
        if (name.indexOf("date") >= 0) {
            JU.attr(items, "isDate", "true");
        }
        header.add(JU.map(items));
    }

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
	    entries = getEntryUtil().sortEntriesOnDate(entries, true);
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
        boolean addImages     = request.get("addImages", true);
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
	    TypeHandler otherTypeHandler = entry.getTypeHandler();
	    columns     = otherTypeHandler.getColumnsForPointJson();
	    if(columns!=null) {
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
            values.add(JU.map(entryArray));
        }

        List<String> topItems = new ArrayList<String>();
        topItems.add("name");
        topItems.add(JU.quote(mainEntry.getName()));
        topItems.add("fields");
        topItems.add(JU.list(null, fields, false).toString());
        topItems.add("data");
        topItems.add(JU.list(null, values, false).toString());
        JU.map(sb, topItems);
    }


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



    private static SimpleDateFormat sdf;
    private static SimpleDateFormat ymdsdf;
    private static SimpleDateFormat hhmmsdf;    

    private String formatDate(long dttm) {
        return formatDate(new Date(dttm));
    }

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

    private void toJson(Request request, Entry entry,Appendable sb) throws Exception {
        List<String> items = new ArrayList<String>();
        JU.quoteAttr(items, "id", entry.getId());
        String entryName = entry.getName();
        JU.quoteAttr(items, "name", entryName);
	ServerInfo server = entry.getRemoteServer();
	String slug = server!=null?null:getRepository().getRepositorySlug();	
	if(stringDefined(slug)) {
	    JU.quoteAttr(items, "repositorySlug", slug);
	}
        String displayName = getEntryDisplayName(entry);
        if ( !displayName.equals(entryName)) {
            JU.quoteAttr(items, "displayName", displayName);
        }
	String embedWiki = entry.getTypeHandler().getEmbedWiki(request, entry);
	if(embedWiki!=null)
            JU.quoteAttr(items, "embedWikiText", embedWiki);


        String snippet = getWikiManager().getSnippet(request, entry, true,
                             null);
        if (snippet != null) {
            JU.quoteAttr(items, "snippet", snippet);
        }


	String mapGlyphs = entry.getTypeHandler().getProperty(entry,"mapglyphs",null);
	if(mapGlyphs!=null) {
            JU.quoteAttr(items, "mapglyphs", mapGlyphs);
	}


        if (request.get("includecrumbs", false)) {
            if (entry.getParentEntry() != null) {
                JU.quoteAttr(items, "breadcrumbs",
                                   getPageHandler().getBreadCrumbs(request,
                                       entry.getParentEntry(), null, null,
								   60,-1));
            }
        }

        if (request.get("includedescription", true)) {
            JU.quoteAttr(items, "description", entry.getDescription());
        }
        boolean canEdit = getAccessManager().canDoEdit(request, entry);
        JU.attr(items, "canedit", canEdit + "");

        TypeHandler type = entry.getTypeHandler();

        /**
         *  Don't get the typeJson as it takes a *long* time for DbTypes
         * String      typeJson = type.getJson(request);
         * typeJson = JU.mapAndQuote(Utils.makeListFromValues("id", type.getType(), "name", type.getLabel()));
         */
        JU.attr(items, "type", JU.quote(type.getType()));
        JU.attr(items, "typeName", JU.quote(type.getLabel()));
        //
	if(entry.getTypeHandler().isType("type_point")) {
            JU.attr(items, "isPoint", "true");
	}
        if (entry.isGroup()) {
            JU.attr(items, "isGroup", "true");
            /*
            List<Entry> children = getEntryManager().getChildren(request, entry);
            List<String> ids = new ArrayList<String>();
            for(Entry child: children) {
                ids.add(JU.quote(child.getId()));
            }
            JU.attr(items, "childEntryIds", JU.list(ids));
            */
        }

        JU.quoteAttr(
            items, "icon",
            request.getAbsoluteUrl(
                getPageHandler().getIconUrl(request, entry)));
        JU.quoteAttr(items, "iconRelative",
                           getPageHandler().getIconUrl(request, entry));

	Entry parent = entry.getParentEntry();
	if(parent!=null) {
	    JU.quoteAttr(items, "parent", parent.getId());
	    JU.quoteAttr(items, "parentName", parent.getName());
	    JU.quoteAttr(
			       items, "parentIcon",
			       request.getAbsoluteUrl(
						      getPageHandler().getIconUrl(request, parent)));
	}
        if (entry.getIsRemoteEntry()) {
            JU.attr(items, "isRemote", "true");
            JU.attr(items, "remoteRepository",
                          JU.map(Utils.makeListFromValues("url",
                              JU.quote(server.getUrl()), "name",
                              JU.quote(server.getLabel()))));
            JU.quoteAttr(items, "remoteUrl", entry.getRemoteUrl());
            String remoteParent = entry.getRemoteParentEntryId();
            if (remoteParent != null) {
                JU.quoteAttr(items, "remoteParent", remoteParent);
            }
        }



        JU.quoteAttr(items, "startDate",
                           formatDate(entry.getStartDate()));
        JU.quoteAttr(items, "ymd", formatYMD(entry.getStartDate()));
        JU.quoteAttr(items, "hhmm", formatHHMM(entry.getStartDate()));
        JU.quoteAttr(items, "endDate", formatDate(entry.getEndDate()));
        JU.quoteAttr(items, "createDate",  formatDate(entry.getCreateDate()));
        JU.quoteAttr(items, "changeDate", formatDate(entry.getChangeDate()));	
        JU.quoteAttr(items, "startDateFormat",
                           getDateHandler().formatDateShort(request, entry,
							    entry.getStartDate()));
        JU.quoteAttr(items, "endDateFormat",
                           getDateHandler().formatDateShort(request, entry,
							    entry.getEndDate()));
        JU.quoteAttr(items, "createDateFormat",
                           getDateHandler().formatDateShort(request, entry,
							    entry.getCreateDate()));
        JU.quoteAttr(items, "changeDateFormat",
                           getDateHandler().formatDateShort(request, entry,
                               entry.getChangeDate()));
	


	String searchDisplay = entry.getTypeHandler().getSearchDisplayText(request,  entry);
	if(searchDisplay!=null) {
            JU.quoteAttr(items, "displayHtml", searchDisplay);
	}
	    


        if (entry.getUser() != null) {
            JU.quoteAttr(items, "creator", entry.getUser().getId());
            JU.quoteAttr(items, "creatorName", entry.getUser().getName());	    
        }
        if (entry.getResource().isUrl()) {
	    //Catch errors 
	    try {
		String jsonUrl = entry.getTypeHandler().getPathForEntry(request, entry,false);
		JU.quoteAttr(items, "url",jsonUrl);
	    } catch(Exception exc) {
		getLogManager().logError("Error reading path for entry:"+ entry + " " + entry.getId(),exc);
	    }
        }


        if (entry.hasAreaDefined(request)) {
            double[] center = entry.getCenter(request);
            JU.attr(items, "geometry",
                          JU.map(Utils.makeListFromValues("type",
                              JU.quote("Point"), "coordinates",
                              JU.list("" + center[1],
                                            "" + center[0]))));
            JU.attr(items, "bbox",
                          JU.list("" + entry.getWest(request),
                                        "" + entry.getSouth(request),
                                        "" + entry.getEast(request),
                                        "" + entry.getNorth(request)));
        } else if (entry.hasLocationDefined(request)) {
            JU.attr(items, "geometry",
                          JU.map(Utils.makeListFromValues("type",
                              JU.quote("Point"), "coordinates",
                              JU.list("" + entry.getLongitude(request),
                                            "" + entry.getLatitude(request)))));
            JU.attr(items, "bbox",
                          JU.list("" + entry.getLongitude(request),
                                        "" + entry.getLatitude(request),
                                        "" + entry.getLongitude(request),
                                        "" + entry.getLatitude(request)));
        }

        if (entry.hasAltitudeTop()) {
            JU.attr(items, "altitudeTop", "" + entry.getAltitudeTop());
        }

        if (entry.hasAltitudeBottom()) {
            JU.attr(items, "altitudeBottom",
                          "" + entry.getAltitudeBottom());
        }

	JU.attr(items, "order",    ""+entry.getEntryOrder());


        if (request.get("includeservices", true)) {
            TypeHandler       typeHandler = entry.getTypeHandler();
            List<ServiceInfo> services    = new ArrayList<ServiceInfo>();
            typeHandler.getServiceInfos(request, entry, services);
            List<String> jsonServiceInfos = new ArrayList<String>();
            for (ServiceInfo service : services) {
                jsonServiceInfos.add(JU.map(Utils.makeListFromValues("url",
                        JU.quote(service.getUrl()), "relType",
                        JU.quote(service.getType()), "name",
                        JU.quote(service.getName()), "mimeType",
                        JU.quote(service.getMimeType()))));
            }

            items.add("services");
            items.add(JU.list(jsonServiceInfos));
        }
        //        System.err.println("services:" + JU.list(jsonServiceInfos));

        Resource resource = entry.getResource();
        if (resource != null) {
            if (resource.isUrl()) {
                String temp = resource.getPath();
                JU.quoteAttr(items, "isurl", "true");
                if (temp == null) {
                    JU.quoteAttr(items, "filename", "");
                } else {
                    JU.quoteAttr(items, "filename",
                                       java.net.URLEncoder.encode(temp,
                                           "UTF-8"));
                }

                JU.attr(items, "filesize", "" + resource.getFileSize());
                JU.quoteAttr(
                    items, "fileSizeLabel",
                    "" + formatFileLength(resource.getFileSize()));
            } else if (resource.isFile()) {
                JU.quoteAttr(items, "isfile", "true");
                JU.quoteAttr(items, "filename",
                                   getStorageManager().getFileTail(entry));
                JU.attr(items, "filesize", "" + resource.getFileSize());
                if (Utils.stringDefined(resource.getMd5())) {
                    JU.quoteAttr(items, "md5", resource.getMd5());
                }
            }
        } else {
            JU.quoteAttr(items, "filename", "no resource");
            JU.attr(items, "filesize", "0");
            JU.quoteAttr(items, "md5", "");
        }

        List<String> attrs = new ArrayList<String>();
        List<String> ids   = new ArrayList<String>();


        // Add special columns to the entries depending on the type
        if (request.get(ARG_EXTRACOLUMNS, true)) {
            List<String> extraColumns    = new ArrayList<String>();
            List<String> columnNames     = new ArrayList<String>();
            List<String> columnLabels    = new ArrayList<String>();
	    List<Column> columns = entry.getTypeHandler().getColumns();
	    if(columns!=null) {
		for(Column column:columns) {
		    String columnName = column.getName();
		    Object v          = entry.getValue(request, column);
		    if (v == null) {
			v = "";
		    }
		    if (v instanceof Date) {
			v = formatDate((Date) v);
		    }
		    String value = v.toString();
		    columnNames.add(columnName);
		    columnLabels.add(column.getLabel());
		    //                    JU.attr(items, "column." + columnName, JU.quote(value));
		    extraColumns.add(JU.map(Utils.makeListFromValues(columnName,
									   JU.quote(value))));
		    ids.add(columnName);
		    attrs.add(JU.map(Utils.makeListFromValues("id",
								    JU.quote(columnName), "type",
								    JU.quote("attribute"), "label",
								    JU.quote(column.getLabel()), "value",
								    JU.quote(value), "canshow",
								    Boolean.toString(column.getCanShow()))));
		}
            }
        }




        if (request.get(ARG_LINKS, false)) {
            List<String> links = new ArrayList<String>();
            for (Link link :
                    repository.getEntryManager().getEntryLinks(request,
                        entry)) {
                OutputType outputType = link.getOutputType();
                links.add(JU.map(Utils.makeListFromValues("label",
                        JU.quote(link.getLabel()), "type",
                        (outputType == null)
                        ? "unknown"
                        : JU.quote(outputType.toString()), "url",
                        (link.getUrl() == null)
                        ? JU.quote("")
                        : JU.quote(
                            java.net.URLEncoder.encode(
                                link.getUrl(), "UTF-8")), "icon",
                                    JU.quote(link.getIcon()))));
            }
            JU.attr(items, "links", JU.list(links));
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
		    if(metadataType.isPrivate(request, entry,metadata)) {
			continue;
		    }
                    List<String> mapItems   = new ArrayList<String>();
                    List<String> valueItems = new ArrayList<String>();
                    JU.quoteAttr(mapItems, "id", metadata.getId());
                    JU.quoteAttr(mapItems, "type", metadata.getType());
                    JU.quoteAttr(mapItems, "label",
                                       metadataType.getLabel());

                    int attrIdx = 1;
                    //We always add the four attributes to have always the same structure
                    while (attrIdx <= 4) {
                        String attr = metadata.getAttr(attrIdx);
                        if (attr != null) {
                            if (attr.length() > 0) {
                                JU.quoteAttr(valueItems,
                                        "attr" + attrIdx, attr);
                            } else {
                                JU.quoteAttr(valueItems,
                                        "attr" + attrIdx, "");
                            }
                        } else {
                            JU.quoteAttr(valueItems, "attr" + attrIdx,
                                    "");
                        }
                        attrIdx++;
                    }

                    mapItems.add("value");
                    mapItems.add(JU.map(valueItems));
                    ids.add(metadata.getId());
                    attrs.add(JU.map(mapItems));
                }
            }
        }

        if (request.get("includeproperties", true)) {
            entry.getTypeHandler().addToJson(request, entry, items, attrs);
        }
        JU.attr(items, "properties", JU.list(attrs, false));
	sb.append(JU.map(items));
    }


    private String toPointJson(Request request, Entry entry,
                               boolean addSnippets, boolean addAttributes,
                               boolean addPointUrl, boolean addThumbnails,
                               boolean addImages, boolean addMediaUrl,
                               TypeHandler mainTypeHandler,
                               List<Column> columns, boolean showFileUrl,
                               boolean remote)
            throws Exception {

        List<String> items = new ArrayList<String>();
        items.add(JU.quote(entry.getName()));
        items.add(JU.quote(entry.getDescription()));
        if (addSnippets) {
            String snippet = getWikiManager().getRawSnippet(request, entry,
                                 true);
            items.add(JU.quote((snippet != null)
                                     ? snippet
                                     : ""));
        }
        items.add(JU.quote(entry.getId()));
        items.add(JU.quote(entry.getTypeHandler().getType()));
        items.add(JU.quote(entry.getTypeHandler().getLabel()));
        items.add(JU.quote(formatDate(entry.getStartDate())));
        items.add(JU.quote(formatDate(entry.getEndDate())));
        items.add(JU.quote(formatDate(entry.getCreateDate())));
        items.add(
            JU.quote(
                request.getAbsoluteUrl(
                    getPageHandler().getIconUrl(request, entry))));
        String url;
        url = getEntryManager().getEntryUrl(request, entry);
        items.add(JU.quote(remote
                                 ? request.getAbsoluteUrl(url)
                                 : url));

        if (addPointUrl) {
            url = entry.getTypeHandler().getUrlForWiki(request, entry,
                    WikiConstants.WIKI_TAG_DISPLAY, new Hashtable(),
                    new ArrayList<String>());

            items.add(JU.quote((url == null)
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
                items.add(JU.quote((url == null)
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
                items.add(JU.quote((url == null)
                                         ? ""
                                         : remote
                                           ? request.getAbsoluteUrl(url)
                                           : url));
            } else {
                items.add(JU.quote(""));
            }
        }
        if (addMediaUrl) {
            url = entry.getTypeHandler().getMediaUrl(request, entry);
            items.add(JU.quote((url == null)
                                     ? ""
                                     : remote
                                       ? request.getAbsoluteUrl(url)
                                       : url));
        }
        TypeHandler typeHandler = entry.getTypeHandler();
        if (addAttributes && (columns != null)) {
            if (typeHandler.isType(mainTypeHandler.getType())) {
                for (Column column : columns) {
                    Object v = entry.getValue(request, column);
		    //extraParameters[column.getOffset()];
                    if (v == null) {
			if (column.isNumeric()) {
			    items.add("null");
			} else {
			    items.add(JU.quote(""));
			}
                    } else {
                        if (column.isDate()) {
                            items.add(JU.quote(formatDate((Date) v)));
                        } else if (column.isNumeric()) {
                            items.add(v.toString());
                        } else {
                            items.add(JU.quote(v.toString()));
                        }
                    }
                }
            } else {
                for (Column column : columns) {
		    if (column.isNumeric()) {
			items.add("null");
		    } else {
			items.add(JU.quote(""));
		    }
                }
            }
        }
        if (showFileUrl) {
            url = entry.getTypeHandler().getEntryResourceUrl(request, entry);
            items.add(JU.quote((url == null)
                                     ? ""
                                     : remote
                                       ? request.getAbsoluteUrl(url)
                                       : url));
        }
        items.add("" + ((entry.getLatitude(request) == Entry.NONGEO)
                        ? "null"
                        : entry.getLatitude(request)));
        items.add("" + ((entry.getLongitude(request) == Entry.NONGEO)
                        ? "null"
                        : entry.getLongitude(request)));
        items.add("" + ((entry.getAltitude() == Entry.NONGEO)
                        ? "null"
                        : entry.getAltitude()));

        return JU.list(items);
    }


}
