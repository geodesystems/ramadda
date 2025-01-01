/**
Copyright (c) 2008-2025 Geode Systems LLC
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

import ucar.unidata.util.StringUtil;
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
    private static final int IDX_PROPERTIES = IDX++;        
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
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
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
            if (path.toLowerCase().matches(".*\\.(fbx|gltf|glb|dae|3ds|obj)$")) {
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

    public Result processEntryAction(Request request, Entry entry)
            throws Exception {
        String action = request.getString("action", "");
	if(action.equals("3dmodelembedtext")) {
	    StringBuilder sb = new StringBuilder();
	    getPageHandler().entrySectionOpen(request, entry, sb, "3D Model Embed");
	    sb.append(request.form(getRepository().URL_ENTRY_ACTION));
	    sb.append(HU.hidden(ARG_ENTRYID,entry.getId()));
	    sb.append(HU.hidden(ARG_ACTION,"3dmodelembedtext"));	
	    sb.append(HU.formTable());
	    sb.append(HU.formEntry("Width:",HU.input("width",request.getString("width","640"))));
	    sb.append(HU.formEntry("Height:",HU.input("height",request.getString("height","480"))));
	    sb.append(HU.formEntry("",HU.labeledCheckbox("addtitle","true",request.get("addtitle",false),"Add Title")));
	    sb.append(HU.formEntry("",HU.labeledCheckbox("decorate","true",request.get("decorate",false),"Show Annotations")));
	    sb.append(HU.formEntry("",HU.submit("Make Embed",ARG_OK)));	
	    sb.append(HU.formTableClose());
	    sb.append(HU.formClose());
	    if(request.exists(ARG_OK)) {
		sb.append("Copy the following text to embed this 3D model in a web page");
		String id = HU.getUniqueId("block_");
		int width = request.get("width",640);
		int height = request.get("height",480);		
		boolean decorate=request.get("decorate",false);
		boolean addTitle = request.get("addtitle",false);
		int hoffset=18;
		int woffset=15;
		if(decorate) woffset+=200;
		if(addTitle) hoffset+=50;
		String url = request.getAbsoluteUrl(
						    HU.url(
							   request.makeUrl(getRepository().URL_ENTRY_ACTION),
							   new String[] { ARG_ENTRYID,
									  entry.getId(),
									  "action", "3dmodelembed",
									  "addtitle",""+addTitle,
									  "width",""+(width-woffset),"height",""+(height-hoffset),
									  "decoratemodel",""+decorate,
							   }));

		sb.append(HU.open("pre",HU.attrs("id",id,"add-copy","true")));
		sb.append("&lt;iframe\n");
		sb.append(" width=" + width);
		sb.append(" height=" + height);
		sb.append(" src='"+ url+"'&gt;\n&lt;/iframe&gt;");
		sb.append("</pre>");
		HU.script(sb,"Utils.addCopyLink('" +id+"');");
	    }
	    getPageHandler().entrySectionClose(request, entry, sb);
	    return getEntryManager().addEntryHeader(request, entry,
						    new Result("3D Model Embed",sb));
	}
	if(action.equals("3dmodelembed")) {
	    request.put("template","empty");
	    String wiki = "";
	    if(request.get("addtitle",true)) wiki+=":property linktarget _3dmodel\n+section title={{name}}\n";
	    wiki+="{{3dmodel}}\n";
	    if(request.get("addtitle",true)) wiki+="-section\n";
	    String text =getWikiManager().wikifyEntry(request, entry,wiki);
	    return new Result("",new StringBuilder(text));
	}
	return super.processEntryAction(request,entry);

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
	String[] jsImports = new String[]{
	    ".gltf","//unpkg.com/three@0.126.0/examples/js/loaders/GLTFLoader.js",
	    ".glb","//unpkg.com/three@0.126.0/examples/js/loaders/GLTFLoader.js",	    
	    ".stl","//unpkg.com/three@0.126.0/examples/js/loaders/STLLoader.js",	    
	    ".fbx","//unpkg.com/three@0.126.0/examples/js/loaders/FBXLoader.js",
	    ".3ds","//unpkg.com/three@0.126.0/examples/js/loaders/TDSLoader.js",
	    ".obj","//unpkg.com/three@0.126.0/examples/js/loaders/OBJLoader.js",
	    ".dae","//unpkg.com/three@0.126.0/examples/js/loaders/ColladaLoader.js"};


        StringBuilder sb = new StringBuilder();
        if (request.getExtraProperty("3dmodeljs") == null) {
	    HU.script(sb,"//dummy var for the fflate to work\nvar exports={};\n");
            for (String js : new String[] {
		    //                "//unpkg.com/fflate",
                "//cdn.jsdelivr.net/npm/fflate/umd/index.js",
		"//unpkg.com/three@0.126.0/build/three.js",
		//                "//unpkg.com/three@0.126.0",
                getRepository().getHtdocsUrl("/lib/three/controls/OrbitControls.js")
		}) {
                HU.importJS(sb, js);
		sb.append("\n");
            }
	    HU.cssLink(sb,getRepository().getHtdocsUrl("/lib/three/model.css"));
            request.putExtraProperty("3dmodeljs", "true");
	}

	//Import the loaders
	for(Entry entry: entries) {
	    String modelFile = (String) entry.getStringValue(request,IDX_MODEL_FILE, null);
	    String  file;
	    if (!Utils.stringDefined(modelFile)) {
		file = entry.getResource().getPath();
	    } else {
		file = modelFile;
	    }
	    for(int i=0;i<jsImports.length;i+=2) {
		if(file.indexOf(jsImports[i])>=0) {
		    linkJS(request, sb, jsImports[i+1]);
		    sb.append("\n");
		    break;
		}
	    }
	}	

	//Now the model.js
	linkJS(request, sb, getRepository().getHtdocsUrl("/lib/three/model.js"));

	int cnt = 0;
	for(Entry entry: entries) {
	    String url;
	    String modelFile = (String) entry.getStringValue(request,IDX_MODEL_FILE, null);
	    String  file;

	    if (!Utils.stringDefined(modelFile)) {
		url = getEntryManager().getEntryResourceUrl(request, entry);
		file = entry.getResource().getPath();
	    } else {
		url = getRepository().getUrlBase() + "/entryfile/"
		    + entry.getId() + "/" + modelFile;
		file = modelFile;
	    }
	    List attrs = Utils.makeListFromValues("url",JsonUtil.quote(url),"id",JsonUtil.quote(entry.getId()),
					"name",JsonUtil.quote(entry.getName()));
	    String tmp;
	    
	    tmp = (String)entry.getValue(request,IDX_CAMERA_POSITION);
	    if(Utils.stringDefined(tmp)) {
		Utils.add(attrs,"cameraPosition",JsonUtil.quote(tmp));
	    }
            List<String> urls = new ArrayList<String>();
            getMetadataManager().getThumbnailUrls(request, entry, urls);
	    if(urls.size()>0) {
		Utils.add(attrs,"thumbnail",JsonUtil.quote(urls.get(0)));
	    }

	    String[] thumbnail = getMetadataManager().getThumbnailUrl(request,  entry);
	    if(thumbnail!=null)
		Utils.add(attrs,"thumbnail",JsonUtil.quote(thumbnail[0]));


	    String include = getMetadataManager().getMetadataUrl(request, entry,"3dmodel_texture");
	    if(include!=null)
		Utils.add(attrs,"texture",JsonUtil.quote(include));
	    include = getMetadataManager().getMetadataUrl(request, entry,"3dmodel_normal");
	    if(include!=null)
		Utils.add(attrs,"normal",JsonUtil.quote(include));	    


	    String background = getMetadataManager().getMetadataUrl(request, entry,"3dmodel_background");
	    if(background!=null)
		Utils.add(attrs,"backgroundImage",JsonUtil.quote(background));
	    background = getMetadataManager().getMetadataUrl(request, entry,"3dmodel_fixed_background");
	    if(background!=null)
		Utils.add(attrs,"fixedBackgroundImage",JsonUtil.quote(background));	    

	    tmp = (String)entry.getValue(request,IDX_AMBIENT_LIGHT);
	    if(Utils.stringDefined(tmp)) {
		Utils.add(attrs,"ambientLight",JsonUtil.quote(tmp));
	    }
	    tmp= (String)entry.getValue(request,IDX_LIGHTS);
	    if(Utils.stringDefined(tmp)) {
		Utils.add(attrs,"lights",JsonUtil.quote(tmp));
	    }
	    tmp= (String)entry.getValue(request,IDX_PROPERTIES);
	    if(Utils.stringDefined(tmp)) {
		for(String line: Utils.split(tmp,"\n",true,true)) {
		    List<String> toks = StringUtil.splitUpTo(line,"=",2);
		    if(toks.size()==2) {
			Utils.add(attrs,toks.get(0).trim(),JsonUtil.quoteType(toks.get(1).trim()));
		    }
		}
	    }


	    tmp= (String)entry.getValue(request,IDX_ANNOTATIONS);
	    if(Utils.stringDefined(tmp)) {
		Utils.add(attrs,"annotations",JsonUtil.quote(tmp));
	    }	    
	    
	    List<String> watermarks = getMetadataManager().getMetadataUrls(request, entry, "3dmodel_watermark");
	    if(watermarks!=null) {
		for(int i=0;i<watermarks.size();i++) {
		    Utils.add(attrs,"watermark"+(i+1),JsonUtil.quote(watermarks.get(i)));
		}
	    }

	    String snippet = getWikiManager().getSnippet(request, entry, true,null);
	    if(snippet!=null) 
		Utils.add(attrs,"description",JsonUtil.quote(snippet));

	    Utils.add(attrs,"entryid",JsonUtil.quote(entry.getId()));
	    models.add(JsonUtil.map(attrs));

	    boolean gotOne = false;
	    for(int i=0;i<jsImports.length;i+=2) {
		if(file.indexOf(jsImports[i])>=0) {
		    //		    importJS(request, sb, jsImports[i+1]);
		    gotOne = true;
		    break;
		}
	    }
	    if(!gotOne) {
		return "Unknown file type:" +file;
	    }
	}


        String id = HU.getUniqueId("model_");
	sb.append("<div class=ramadda-model>");
	sb.append("<table border=0 cellspacing=0 cellpadding=0><tr valign=top><td>");
        HU.div(sb, "",HU.attrs("id", id+"_toc"));
	sb.append("</td><td>\n");
	String width = request.getString("width",Utils.getProperty(props,"width","640"));
	String height =request.getString("height",Utils.getProperty(props,"height","480"));
        sb.append(HU.open("div",
			  HU.attrs("style", HU.css("position","relative",
						   "width", HU.makeDim(width,"px"), 
						   "height", HU.makeDim(height,"px")),
				   "tabindex", "1", "id", id, "class",
				   "ramadda-model-display ramadda-nooutline")));

	sb.append(HU.close("div"));
	sb.append("</td>");
	if(request.get("decoratemodel",true)) {
	    sb.append("<td>");
	    HU.div(sb, "",HU.attrs("id", id+"_annotations"));
	    sb.append("</td>");
	}
	sb.append("</tr></table>");
	sb.append("</div>");
        List<String> jsonProps = new ArrayList<String>();
        Utils.add(jsonProps, "id", JsonUtil.quote(id));
	String sessionId = request.getSessionId();
	if(sessionId!=null) {
	    String authToken = getAuthManager().getAuthToken(sessionId);
	    Utils.add(jsonProps, "authtoken", JsonUtil.quote(authToken));
	}

	Utils.add(jsonProps,"width",width,"height",height);
	List tmp = Utils.makeListFromDictionary(props);
	for(int i=0;i<tmp.size();i+=2) {
	    Utils.add(jsonProps,tmp.get(i),
		      JsonUtil.quoteType(tmp.get(i+1)));
	}
        String js = "new Ramadda3DDisplayManager(" +
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

    @Override
    public void initTags(Hashtable<String, WikiTagHandler> tagHandlers) {
	tagHandlers.put("3dmodel",this);
    }

    @Override
    public void addTagDefinition(List<String>  tags) {
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

