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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.zip.*;


/**
 *
 *
 */
public class ThreeDModelTypeHandler  extends GenericTypeHandler implements WikiTagHandler {

    /**  */
    private static int IDX = 0;

    /**  */
    private static final int IDX_MODEL_FILE = IDX++;

    /**  */
    private static final int IDX_CAMERA_POSITION = IDX++;

    /**  */
    private static final int IDX_AMBIENT_LIGHT = IDX++;

    private static final int IDX_LIGHTS = IDX++;
    private static final int IDX_ANNOTATIONS = IDX++;        

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
        if ( !resource.toLowerCase().endsWith(".zip")) {
	    entry.setValue(IDX_MODEL_FILE, "");
            return;
        }

        List<String> files = new ArrayList<String>();
        File entryDir = getStorageManager().getEntryDir(entry.getId(), true);
        InputStream  fis   = getStorageManager().getFileInputStream(resource);
        unzip(entry, entryDir, null, files, fis);
        fis.close();

        String modelFile = "";
        for (String path : files) {
            if (path.toLowerCase().matches(".*(fbx|gltf|dae).*")) {
                modelFile = path;
                break;
            }
        }
        System.err.println(modelFile);
        entry.setValue(IDX_MODEL_FILE, modelFile);
    }


    /**
     *
     * @param entry _more_
     * @param entryDir _more_
     * @param prefix _more_
     * @param files _more_
     * @param fis _more_
     *
     * @throws Exception _more_
     */
    private void unzip(Entry entry, File entryDir, String prefix,
                       List<String> files, InputStream fis)
            throws Exception {
        ZipInputStream zin = getStorageManager().makeZipInputStream(fis);
        ZipEntry       ze  = null;
        while ((ze = zin.getNextEntry()) != null) {
            String path = ze.getName();
            path = path.replaceAll("\\.\\.", "_");
            if (ze.isDirectory()) {
                File dir = new File(entryDir, path);
                System.err.println("dir:" + path);
                dir.mkdirs();
                continue;
            }
            String parent = new File(path).getParent();
            File parentDir = (parent == null)
                             ? entryDir
                             : new File(entryDir, parent);
            parentDir.mkdirs();
            if (path.endsWith(".zip")) {
                System.err.println("Unzipping:" + path);
                unzip(entry, parentDir, parent, files, zin);
                continue;
            }
            File             destFile = new File(entryDir, path);
            FileOutputStream toStream = new FileOutputStream(destFile);
            System.err.println("writing to:" + destFile);
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
	List<Entry> entries = new ArrayList<Entry>();
	entries.add(entry);
	return get3DModelWiki(wikiUtil, request,originalEntry, entries,props);
    }

    public String get3DModelWiki(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, List<Entry> entries,
                                 Hashtable props)
            throws Exception {


	List<String> models = new ArrayList<String>();
	for(Entry entry: entries) {
	    String url;
	    String modelFile = (String) entry.getValue(IDX_MODEL_FILE, null);
	    if (!Utils.stringDefined(modelFile)) {
		url = getEntryManager().getEntryResourceUrl(request, entry);
	    } else {
		url = getRepository().getUrlBase() + "/entryfile/"
		    + entry.getId() + "/" + modelFile;
	    }
	    List attrs = Utils.makeList("url",JsonUtil.quote(url),"id",JsonUtil.quote(entry.getId()),
					"name",JsonUtil.quote(entry.getName()));
	    String tmp;
	    
	    tmp = (String)entry.getValue(IDX_CAMERA_POSITION);
	    if(Utils.stringDefined(tmp)) {
		Utils.add(attrs,"cameraPosition",JsonUtil.quote(tmp));
	    }

	    String thumbnail = getMetadataManager().getThumbnailUrl(request,  entry);
	    if(thumbnail!=null)
		Utils.add(attrs,"thumbnail",JsonUtil.quote(thumbnail));


	    String background = getMetadataManager().getMetadataUrl(request, entry,"3dmodel_background");
	    if(background!=null)
		Utils.add(attrs,"backgroundImage",JsonUtil.quote(background));
	    background = getMetadataManager().getMetadataUrl(request, entry,"3dmodel_fixed_background");
	    if(background!=null)
		Utils.add(attrs,"fixedBackgroundImage",JsonUtil.quote(background));	    

	    tmp = (String)entry.getValue(IDX_AMBIENT_LIGHT);
	    if(Utils.stringDefined(tmp)) {
		Utils.add(attrs,"ambientLight",JsonUtil.quote(tmp));
	    }
	    tmp= (String)entry.getValue(IDX_LIGHTS);
	    if(Utils.stringDefined(tmp)) {
		Utils.add(attrs,"lights",JsonUtil.quote(tmp));
	    }
	    tmp= (String)entry.getValue(IDX_ANNOTATIONS);
	    if(Utils.stringDefined(tmp)) {
		Utils.add(attrs,"annotations",JsonUtil.quote(tmp));
	    }	    
	    
	    Utils.add(attrs,"entryid",JsonUtil.quote(entry.getId()));
	    models.add(JsonUtil.map(attrs));
	}
        StringBuilder sb = new StringBuilder();
        if (request.getExtraProperty("3dmodeljs") == null) {
            for (String js : new String[] {
                "//unpkg.com/fflate",
                "//cdn.jsdelivr.net/npm/fflate/umd/index.js",
                "//unpkg.com/three",
                "//unpkg.com/three/examples/js/loaders/GLTFLoader.js",
                "//unpkg.com/three/examples/js/loaders/FBXLoader.js",
		"//unpkg.com/three/examples/js/loaders/ColladaLoader.js",
                getRepository().getHtdocsUrl(
					     "/lib/three/controls/OrbitControls.js"),
                getRepository().getHtdocsUrl("/lib/three/model.js")
            }) {
                HU.importJS(sb, js);
            }
	    HU.cssLink(sb,getRepository().getHtdocsUrl("/lib/three/model.css"));
            request.putExtraProperty("3dmodeljs", "true");
        }
        String id = HU.getUniqueId("model_");
	sb.append("<table border=0 cellspacing=0 cellpadding=0><tr valign=top><td>");

        HU.div(sb, "",HU.attrs("id", id+"_toc"));
	sb.append("</td><td>");
        HU.div(sb, "",
               HU.attrs("style", HU.css("width", HU.makeDim(Utils.getProperty(props,"width","640"),"px"), 
					"height", HU.makeDim(Utils.getProperty(props,"height","480"),"px")),
                        "tabindex", "1", "id", id, "class",
                        "ramadda-model-display ramadda-nooutline"));
	sb.append("</td><td>");
        HU.div(sb, "",HU.attrs("id", id+"_annotations"));
	sb.append("</td></tr></table>");
        List<String> jsonProps = new ArrayList<String>();
        Utils.add(jsonProps, "id", JsonUtil.quote(id));
	String sessionId = request.getSessionId();
	if(sessionId!=null) {
	    String authToken = RepositoryUtil.hashString(sessionId);
	    Utils.add(jsonProps, "authtoken", JsonUtil.quote(authToken));
	}


	List tmp = Utils.makeList(props);
	for(int i=0;i<tmp.size();i+=2) {
	    Utils.add(jsonProps,tmp.get(i),
		      JsonUtil.quoteType(tmp.get(i+1)));
	}
        String js = "new Ramadda3DDisplay(" +
	    JsonUtil.list(models) + "," +
	    JsonUtil.map(jsonProps) + ");";
        HU.script(sb, js);
        return sb.toString();
    }



    public void getWikiTags(List<String[]> tags, Entry entry) {
	tags.add(new String[]{"3dmodel","3dmodel \n#width=600 #height=400 #background=f4f4f4 \n" +
			      "#showAxes=true #axesColor=red \n" +
			      "#showBox=true #bboxColor=#ff0000 \n" +
			      "#cameraPosition=\"posx,posy,posz;rotx;roty;rotz\" \n" +
			      "#ambientLight=\"#404040,30\" #lights=\"#ff0000,x,y,z,intensity;...\" \n"
	    });
    }

    public void initTags(Hashtable<String, WikiTagHandler> tagHandlers) {
	tagHandlers.put("3dmodel",this);
    }

    /**
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param theTag _more_
     * @param props _more_
     * @param remainder _more_
      * @return _more_
     */
    public String handleTag(WikiUtil wikiUtil, Request request,
                            Entry originalEntry, Entry entry, String theTag,
                            Hashtable props, String remainder) throws Exception {
	if(entry.getTypeHandler().isType(this.getType())) {
	    return getWikiInclude(wikiUtil, request, originalEntry,  entry,
				  theTag, props);
	}
	
	List<Entry> children = getEntryUtil().getEntriesOfType(getWikiManager().getEntries(request, wikiUtil,
											   originalEntry, entry, props),
							       getType());

	if(children.size()==0) {
	    return "No 3d Model entries";
	}
	return get3DModelWiki(wikiUtil, request,originalEntry, children,props);
    }


}

