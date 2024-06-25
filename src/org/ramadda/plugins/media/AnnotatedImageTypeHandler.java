/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;


import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;


import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class AnnotatedImageTypeHandler extends ImageTypeHandler  {

    private static final String ANN_PATH = "/lib/annotorius";

    /**  */
    private static int IDX = ImageTypeHandler.IDX_LAST+1;

    private static final int IDX_ANNOTATIONS = IDX++;    

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public AnnotatedImageTypeHandler(Repository repository, Element entryNode)
	throws Exception {
        super(repository, entryNode);
    }



    private void initImports(Request request, StringBuilder sb) throws Exception {
        if (request.getExtraProperty("annotation_added") == null) {
	    HU.cssLink(sb, getHtdocsPath(ANN_PATH+"/annotorious.min.css"));
	    HU.importJS(sb,getHtdocsPath(ANN_PATH+"/annotorious.min.js"));
	    HU.importJS(sb,getHtdocsPath(ANN_PATH+"/annotorious-toolbar.min.js"));
	    HU.cssLink(sb,getHtdocsPath("/media/annotation.css","/media/annotation.css"));
            HU.importJS(sb,getHtdocsPath("/media/annotation.js","/media/annotation.js"));
            request.putExtraProperty("annotation_added", "true");
        }
    }	


    private List<String> getProperties(Request request, Entry entry,Hashtable props) throws Exception {
	List<String> jsonProps = new ArrayList<String>();
        String annotations = (String) entry.getValue(request,IDX_ANNOTATIONS);
	if(!Utils.stringDefined(annotations)) {
	    annotations = "[]";
	}
	Utils.add(jsonProps, "annotations", annotations);
	Utils.add(jsonProps,"canEdit",""+ getAccessManager().canDoEdit(request, entry));
	String authToken = request.getAuthToken();	
	Utils.add(jsonProps,"authToken",HU.quote(authToken));
	Utils.add(jsonProps,"entryId",HU.quote(entry.getId()));
	Utils.add(jsonProps,"name",HU.quote(entry.getName()));	
	Utils.add(jsonProps,"showAnnotationBar",Utils.getProperty(props, "showAnnotationBar", "true"));
	Utils.add(jsonProps,"showToolbar",Utils.getProperty(props, "showToolbar", "true"));	
        return  jsonProps;
    }

    private String makeLayout(Request request, Entry entry,StringBuilder sb,Hashtable props) throws Exception {
	initImports(request,sb);
        String        width  = Utils.getProperty(props, "width", "100%");
        String mainStyle = HU.css("width", HU.makeDim(width, null));
        String style = HU.css("width", HU.makeDim(width, null),
			      "color", "#333",
                              "background-color", "#fff");

	//	HU.open(sb,"center");
	//	HU.open(sb,"div",HU.attrs("style",HU.css("text-align","left","display","inline-block","width",width)));
        String id = HU.getUniqueId("annotated_image");
	String imgUrl = entry.getTypeHandler().getEntryResourceUrl(request, entry);
	String image    = HtmlUtils.img(imgUrl, "", HU.attrs("width","100%","id",id));
	String main = HU.div(image,HU.attrs("class","ramadda-annotated-image","style",mainStyle));
	String top = HU.div("", HU.attrs("id", id+"_top"));
	String bar = HU.div("", HU.attrs("id", id+"_annotations"));
	if(!Utils.getProperty(props, "showAnnotationBar", true)) {
	    bar = "";
	}

        sb.append(HU.div(top +
			 HU.div(bar+main,HU.attrs("class","ramadda-annotation-wrapper","style", style)),""));
	//        sb.append("\n</div>\n");
	//	HU.close(sb,"center");
	return id;
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

        if ( !tag.equals("annotated_image")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
        StringBuilder sb     = new StringBuilder();
	String id = makeLayout(request, entry,sb,props);
	List<String> jsonProps =  getProperties(request, entry,props);	
        Utils.add(jsonProps, "id", JsonUtil.quote(id));
	HU.script(sb, "new RamaddaAnnotatedImage(" + HU.comma(JsonUtil.map(jsonProps),HU.quote(id))+");\n");
        return sb.toString();
    }


}
