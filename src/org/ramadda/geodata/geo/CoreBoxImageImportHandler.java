/**
Copyright (c) 2008-2025 Geode Systems LLC
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

import java.util.zip.*;
import org.json.*;

import org.w3c.dom.*;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import java.io.*;

import java.net.URL;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CoreBoxImageImportHandler extends ImportHandler {

    public CoreBoxImageImportHandler() {
        super(null);
    }

    public CoreBoxImageImportHandler(Repository repository) {
        super(repository);
    }

    public void addImportTypes(List<TwoFacedObject> importTypes,
                               Appendable formBuffer) {
        super.addImportTypes(importTypes, formBuffer);
        importTypes.add(new TwoFacedObject("Borehole Core Images","coreimages"));
    }


    @Override
    public Result handleRequest(Request request, Repository repository,
                                String uploadedFile, Entry parentEntry)
            throws Exception {
	if(!uploadedFile.endsWith("coreimages.zip")) {
	    if ( !request.getString(ARG_IMPORT_TYPE, "").equals("coreimages")) {
		return null;
	    }
	}
	
		
        StringBuffer sb = new StringBuffer();
        getPageHandler().entrySectionOpen(request, parentEntry, sb,
                                          "Imported Entries");

        List<Entry> entries = new ArrayList<Entry>();
	List<FileHolder> files  = new ArrayList<FileHolder>();
	InputStream fis =   getStorageManager().getFileInputStream(uploadedFile);
	ZipInputStream zin = getStorageManager().makeZipInputStream(fis);
	ZipEntry ze = null;
	while ((ze = zin.getNextEntry()) != null) {
	    if (ze.isDirectory()) {
		continue;
	    }
	    String path = ze.getName();
	    String name = IO.getFileTail(path);
	    if(name.indexOf("MANIFEST")>=0) continue;

	    File f = getStorageManager().getTmpFile(name);
	    OutputStream  fos = getStorageManager().getFileOutputStream(f);
	    try {
		IOUtil.writeTo(zin, fos);
	    } finally {
		IO.close(fos);
	    }
	    files.add(new FileHolder(f,path));
	}

	Hashtable<String,FileHolder> map = new Hashtable<String,FileHolder>();

	for(FileHolder fileHolder: files) {
	    String name = fileHolder.file.getName();
	    if(!Utils.isImage(name) || name.indexOf("_ML")>=0) continue;
	    fileHolder.isCoreImage = true;
	    String path = fileHolder.getPathToUse();
	    map.put(path,fileHolder);
	}

	for(FileHolder fileHolder: files) {
	    if(fileHolder.isCoreImage) continue;
	    String path = fileHolder.getPathToUse();
	    FileHolder parent = map.get(path);
	    if(parent==null) {
		sb.append("Could not find parent core image for:" + path+"<br>");
		continue;
	    }
	    //	    System.err.println("Parent:" + parent.path +" child:"+ fileHolder.path);
	    parent.files.add(fileHolder);
	}

	for (String key : map.keySet()) {
	    FileHolder parent = map.get(key);
	    Entry coreEntry = makeEntry(request, "type_borehole_coreimage",
					parent, parentEntry);

	    coreEntry.setValue("top_depth",new Double(Double.NaN));
	    coreEntry.setValue("bottom_depth",new Double(Double.NaN));	    
	    entries.add(coreEntry);

	    for(FileHolder child: parent.files) {
		Entry childEntry = makeEntry(request, null, child,coreEntry);
		if(childEntry==null) continue;

		entries.add(childEntry);
		
	    }
	}

        getEntryManager().addNewEntries(request, entries);
        getEntryManager().parentageChanged(entries,true);

        sb.append("<ul>");
        for (Entry newEntry : entries) {
            sb.append("<li> ");
            sb.append(getPageHandler().getBreadCrumbs(request, newEntry,
                    parentEntry));
        }

        getPageHandler().entrySectionClose(request, parentEntry, sb);
        return getEntryManager().addEntryHeader(request, parentEntry,
                new Result("", sb));

    }

    private Entry  makeEntry(Request request,
			     String type,
			     FileHolder file, Entry parentEntry) throws Exception     {
	File f =  getStorageManager().moveToStorage(request, file.file);
	Resource resource = new Resource(f, Resource.TYPE_STOREDFILE);
	TypeHandler typeHandler;
	if(stringDefined(type)) {
	    typeHandler = getRepository().getTypeHandler(type);
	} else {
	    typeHandler = getEntryManager().findDefaultTypeHandler(request,file.file.toString());
	}
	if(typeHandler==null) {
            typeHandler =
                getRepository().getTypeHandler(TypeHandler.TYPE_FILE);
	}
	
	
	Entry entry = typeHandler.createEntry();
	Date now = new Date();
	entry.setStartDate(DateHandler.NULL_DATE);
	entry.setEndDate(DateHandler.NULL_DATE);	
	
	entry.setResource(resource);
	entry.setCreateDate(now.getTime());
	entry.setParentEntry(parentEntry);
	entry.setName(makeName(file.file.getName()));
	entry.getTypeHandler().initializeNewEntry(request, entry, TypeHandler.NewType.NEW);
	return entry;

    }
    


    private String makeName(String path)    {
	String name = IO.getFileTail(path);
	name = name.replaceAll("_", " ");
	name = IO.stripExtension(name);
	return name;
    }
			
			       
    private static class FileHolder {
	boolean isCoreImage = false;
	File file;
	String path;
	List<FileHolder> files = new ArrayList<FileHolder>();
	FileHolder(File file, String path) {
	    this.file = file;
	    this.path= path;
	}
	public String getPathToUse() {
	    int idx = path.indexOf("/");
	    if(idx<0) return "";
	    return path.substring(0,idx);
	}

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
