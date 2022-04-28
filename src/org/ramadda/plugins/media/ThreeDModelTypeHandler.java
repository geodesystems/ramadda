/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/
package org.ramadda.plugins.media;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.ProcessRunner;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;
import org.w3c.dom.*;
import ucar.unidata.util.IOUtil;

import java.io.*;
import java.util.zip.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 */
public class ThreeDModelTypeHandler extends GenericTypeHandler {

    /**  */
    private static int IDX = 0;

    private static final int IDX_MODEL_FILE = IDX++;

    /**  */
    private static final int IDX_IMAGE_WIDTH = IDX++;

    /**  */
    private static final int IDX_IMAGE_HEIGHT = IDX++;

    /**  */
    private static final int IDX_TILES_URL = IDX++;

    /**  */
    private static final int IDX_STYLE = IDX++;

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public ThreeDModelTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }



    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param fromImport _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,
                                   boolean fromImport)
            throws Exception {
        super.initializeNewEntry(request, entry, fromImport);
        if ( !entry.isFile()) {
            return;
        }
	String resource = entry.getResource().getPath();
	if(!resource.toLowerCase().endsWith(".zip")) {
	    return;
	}

	List<String> files = new ArrayList<String>();
	File entryDir = getStorageManager().getEntryDir(entry.getId(),true);
	InputStream fis =
	    getStorageManager().getFileInputStream(resource);
	unzip(entry,entryDir, "",files,fis);
	fis.close();

	String modelFile = "";
	for(String path: files) {
	    System.err.println("FILE:" + path);
	    if(path.toLowerCase().matches(".*(fbx|gltf).*")) {
		modelFile = path;
		break;
	    }
	}
	System.err.println(modelFile);
	entry.setValue(IDX_MODEL_FILE, modelFile);
    }


    private void unzip(Entry entry,File entryDir, String prefix, List<String>files,InputStream fis) throws Exception {
	ZipInputStream zin =
	    getStorageManager().makeZipInputStream(fis);
	ZipEntry ze = null;
	while ((ze = zin.getNextEntry()) != null) {
	    String path = ze.getName();
	    path = path.replaceAll("\\.\\.","_");
	    if (ze.isDirectory()) {
		File dir = new File(entryDir,path);
		System.err.println("dir:" + path);
		dir.mkdirs();
		continue;
	    }
	    String parent = new File(path).getParent();
	    System.err.println("PATH:" + path +" PARENT:"  + parent);
	    File parentDir = parent==null?entryDir:new File(entryDir,parent);
	    parentDir.mkdirs();
	    if(path.endsWith(".zip")) {
		System.err.println("Unzipping:" + path);
		unzip(entry,parentDir,"",files,zin);
		continue;
	    }
	    File destFile = new File(entryDir,path);
	    FileOutputStream toStream  = new FileOutputStream(destFile);
	    System.err.println("writing to:"+ destFile);
            IOUtil.writeTo(zin, toStream);
            IOUtil.close(toStream);
	    files.add(path);
	}
    }

    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {

        if ( !tag.equals("3dmodel")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
	String modelFile = (String)entry.getValue(IDX_MODEL_FILE,null);
	if(!Utils.stringDefined(modelFile)) {
	    return "No model file found";
	}
	String url = getRepository().getUrlBase()
	    + "/entryfile/" + entry.getId() +"/" 
	    + modelFile;
        StringBuilder sb     = new StringBuilder();
	if(request.getExtraProperty("3dmodeljs")==null) {
	    for(String js:new String[]{"//unpkg.com/fflate",
		    "//cdn.jsdelivr.net/npm/fflate/umd/index.js",
		    "//unpkg.com/three",
		    "//unpkg.com/three/examples/js/loaders/GLTFLoader.js",
		    "//unpkg.com/three/examples/js/loaders/FBXLoader.js",
		    getRepository().getHtdocsUrl("/lib/three/controls/OrbitControls.js"),
		    getRepository().getHtdocsUrl("/lib/three/model.js")}) {
		HU.importJS(sb,js);
	    }
	    request.putExtraProperty("3dmodeljs","true");
	}
	String id = HU.getUniqueId("model_");
	HU.div(sb,"",HU.attrs("style",HU.css("width","600px","height","400px"),"tabindex","1", "id",id,"class","ramadda-nooutline"));
	List<String> jsonProps = new ArrayList<String>();
	Utils.add(jsonProps,"id",JsonUtil.quote(id));
    String js = "new Model3D(" + HtmlUtils.quote(url)+"," + JsonUtil.map(jsonProps)+");";
	HU.script(sb, js);
        return sb.toString();
    }



}
