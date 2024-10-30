/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.geo;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.TypeHandler;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.JsonUtil;

import org.json.*;

import org.w3c.dom.*;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 */
public class CoreBoxImageImportHandler extends ImportHandler {


    /**
     * _more_
     */
    public CoreBoxImageImportHandler() {
        super(null);
    }

    /**
     * _more_
     *
     * @param repository _more_
     */
    public CoreBoxImageImportHandler(Repository repository) {
        super(repository);
    }

    /**
     * _more_
     *
     * @param importTypes _more_
     * @param formBuffer _more_
     */
    public void xaddImportTypes(List<TwoFacedObject> importTypes,
                               Appendable formBuffer) {
    }


    @Override
    public Result handleRequest(Request request, Repository repository,
                                String uploadedFile, Entry parentEntry)
            throws Exception {
	if(!uploadedFile.endsWith("coreimages.zip")) return null;
	return null;
	/**
        if ( !request.getString(ARG_IMPORT_TYPE, "").equals(TYPE_IIIF)) {
            return null;
        }
        List<Entry> entries = new ArrayList<Entry>();

	String json = new String(IOUtil.readBytes(getStorageManager().getFileInputStream(uploadedFile)));

	JSONObject root = new JSONObject(json);
	JSONArray sequences = root.getJSONArray("sequences");
	JSONArray topMetadata   = root.optJSONArray("metadata");
	String topThumbnail   = JsonUtil.readValue(root,"thumbnail.@id",null);
	JSONArray topDescription   = root.optJSONArray("description");
	StringBuilder desc = new StringBuilder();
	if(topDescription!=null) {
	    for (int i = 0; i < topDescription.length(); i++) {
		if(i>0) desc.append("<br>");
		String s = topDescription.optString(i);
		if(s!=null) {
		    desc.append(s);
		} else {
		    JSONObject jo = topDescription.optJSONObject(i);
		    if(jo!=null) {
			s = jo.optString("@value");
			if(s!=null) 
			    desc.append(s);

		    }
		}
	    }
	}
	String topDescriptionS = root.optString("description",null);
	if(topDescriptionS!=null) {
	    desc.append(topDescriptionS);
	}


	String topLabel = root.optString("label");
        for (int i = 0; i < sequences.length(); i++) {
            JSONObject sequence   = sequences.getJSONObject(i);
	    JSONArray canvases = sequence.getJSONArray("canvases");
	    for (int j = 0; j < canvases.length(); j++) {
		JSONObject canvas   = canvases.getJSONObject(j);
		Entry album = getRepository().getTypeHandler("media_photoalbum").createEntry();
		album.setCreateDate(new Date().getTime());
		String thumbnail   = JsonUtil.readValue(canvas,"thumbnail.@id",topThumbnail);
		if(thumbnail!=null) {
		    getRepository().getMetadataManager().addThumbnailUrl(request, album,thumbnail,Utils.getFileTail(thumbnail));
		}

		album.setParentEntry(parentEntry);
		String label = canvas.getString("label");
		//Some heuristics to get the best label
		if(topLabel!=null && topLabel.length()-label.length()>5)
		    label = topLabel;
		album.setName(label);
		album.setDescription(desc.toString());
		entries.add(album);
		List<JSONArray> metadataList = new ArrayList<JSONArray>();
		if(topMetadata!=null) metadataList.add(topMetadata);
		JSONArray canvasMetadata = canvas.optJSONArray("metadata");
		if(canvasMetadata!=null) metadataList.add(canvasMetadata);
		addMetadata(getRepository(), request,album,metadataList);
		JSONArray images = canvas.getJSONArray("images");
		int imageCnt = 0;
		for (int k = 0; k < images.length(); k++) {
		    JSONObject image   = images.getJSONObject(k);
		    JSONObject resource   = image.getJSONObject("resource");
		    String url = resource.getString("@id");
		    Entry imageEntry = getRepository().getTypeHandler("type_image").createEntry();
		    imageEntry.setCreateDate(new Date().getTime());
		    imageEntry.setName(Utils.getFileTail(url));
		    imageEntry.setParentEntry(album);
		    imageEntry.setResource(new Resource(url,Resource.TYPE_URL));
		    imageEntry.setEntryOrder(imageCnt++);
		    if(thumbnail!=null) {
			getRepository().getMetadataManager().addThumbnailUrl(request, imageEntry,thumbnail,Utils.getFileTail(thumbnail));
		    }
		    entries.add(imageEntry);
		}
	    }
	}

        StringBuffer sb = new StringBuffer();
        for (Entry newEntry : entries) {
            newEntry.setUser(request.getUser());
        }
        getEntryManager().addNewEntries(request, entries);
        getPageHandler().entrySectionOpen(request, parentEntry, sb,
                                          "Imported Entries");
        sb.append("<ul>");
        for (Entry newEntry : entries) {
            sb.append("<li> ");
            sb.append(getPageHandler().getBreadCrumbs(request, newEntry,
                    parentEntry));
        }

        getPageHandler().entrySectionClose(request, parentEntry, sb);
        return getEntryManager().addEntryHeader(request, parentEntry,
                new Result("", sb));
	*/
    }



    /*
    public static void addMetadata(Repository repository,Request request,Entry entry,List<JSONArray>metadataList) throws Exception {
	for(JSONArray metadatas: metadataList) {
	    for (int k = 0; k < metadatas.length(); k++) {
		JSONObject metadata   = metadatas.getJSONObject(k);
		String mlabel =  metadata.getString("label");
		List<String> values = new ArrayList<String>();
		JSONArray tmp = metadata.optJSONArray("value");
		if(tmp!=null) {
		    for(int l=0;l<tmp.length();l++)
			values.add(tmp.getString(l));
		} else {
		    String tmp2 = metadata.optString("value");
		    if(tmp2!=null)
			values.add(tmp2);
		}
		for(String mvalue: values) {
		    String mtype = ContentMetadataHandler.TYPE_PROPERTY;
		    String v1=mlabel;
		    String v2= mvalue;
		    if(mlabel.equals("Author")) {
			v1 = v2;v2=null; mtype="metadata_author";
		    } else if(mlabel.equals("Publisher")) {
			v1 = v2;v2=null; mtype="metadata_publisher";
		    } else if(mlabel.equals("Title")) {
			if(!Utils.stringDefined(entry.getName())) 
			entry.setName(v2); v1=null;
		    } else if(mlabel.equals("Location")) {
			v1 = v2;v2=null; mtype="content.location";
		    } else if(mlabel.equals("Subject")) {
			v1 = v2;v2=null; mtype="content.subject";
		    } else if(mlabel.equals("Subjects")) {
			v1 = v2;v2=null; mtype="content.subject";
		    } else if(mlabel.equals("Short Title")) {
			v1=null;
		    } else if(mlabel.equals("Note")) {
			v1=null;
			if(!Utils.stringDefined(entry.getDescription())) 
			    entry.setDescription(v2);
		    } else  if(mlabel.startsWith("Download")) {
			v1=null;
		    } else  if(mlabel.equals("Date")) {
			Date dttm = Utils.parseDate(v2);
			if(dttm!=null)
			    entry.setStartAndEndDate(dttm.getTime());
			v1=null;
		    }
		    if(v1!=null) {
			repository.getMetadataManager().addMetadata(request,entry,
								    mtype,true,
								    v1,v2);
		    }
		}
	    }
	}
    }

    */
}
