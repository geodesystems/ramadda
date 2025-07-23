/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.ogc;


import org.ramadda.repository.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.GeoUtils;

import org.w3c.dom.*;


import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import java.util.List;


/**
 * Manages WMS capabilities URLs
 */
@SuppressWarnings("unchecked")
public class WmsLayerTypeHandler extends ExtensibleGroupTypeHandler {


    /**
     * ctor
     *
     * @param repository the repository
     * @param node the types.xml node
     * @throws Exception On badness
     */
    public WmsLayerTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }



    @Override
    public boolean addToMap(Request request, Entry entry, MapInfo map)
            throws Exception {



	if(isType("type_wms_layer")) {
	    String baseUrl = entry.getStringValue(request,"base_url",null);
	    String layer = entry.getStringValue(request,"layer_name",null);	    
	    if(stringDefined(baseUrl) && stringDefined(layer)) {
		map.addJS(HtmlUtils.call("theMap.addWMSLayer",
					 HtmlUtils.squote(entry.getName()),
					 HtmlUtils.squote(baseUrl),
					 HtmlUtils.squote(layer),
					 "false","false","{visible:true}"));

		map.addJS("\n");
		if (entry.hasAreaDefined(request)) {
		    map.centerOn(request, entry);
		}
	    }
	    return false;
	}
	    

	if(isType("type_wmts_layer")) {
	    String url = entry.getResource().getPath();
	    if(!stringDefined(url)) return false;
	    map.addJS(HtmlUtils.call("theMap.createXYZLayer",
				     HtmlUtils.squote(entry.getName()),
				     HtmlUtils.squote(url),
				     "null","true","true","true"));
	    map.addJS("\n");
	    if (entry.hasAreaDefined(request)) {
		map.centerOn(request, entry);
	    }
	}
	return false;
    }




    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
	if(!isNew(newType)) return;
	if(isType("type_wms_layer")) return;

        String url = entry.getResource().getPath();
	if(stringDefined(url)) {
	    if(!Utils.stringDefined(request.getString(ARG_NAME,""))) {
		//https://storage.googleapis.com/noaa-nidis-drought-gov-data/current-conditions/tile/v1/precip-prcntnorm-60d/4/2/5.png?time=1753230600000
		//https://storage.googleapis.com/noaa-nidis-drought-gov-data/current-conditions/tile/v1/precip-prcntnorm-30d/${z}/${x}/${y}.png?time=1753221600000
		String name = StringUtil.findPattern(url,"/([^/]+)/\\$\\{z");
		if(name==null)
		    name = StringUtil.findPattern(url,"/([^/]+)/[0-9]+/[0-9]+/[0-9]+");		    
		if(name!=null) entry.setName(Utils.makeLabel(name));
	    }

	    url = url.replaceAll("/[0-9]+/[0-9]+/[0-9]+","/\\${z}/\\${x}/\\${y}");
	    url = url.replaceAll("time=[0-9]+","time=\\${timestamp\\}");
	    entry.getResource().setPath(url);
	}
    }



}
