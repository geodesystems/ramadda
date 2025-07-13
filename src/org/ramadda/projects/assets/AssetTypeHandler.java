/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.projects.assets;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.output.*;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.TTLCache;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.seesv.Seesv;

import ucar.unidata.util.StringUtil;


import org.w3c.dom.*;
import org.json.*;
import java.net.URL;
import java.io.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;


public class AssetTypeHandler extends GenericTypeHandler implements WikiTagHandler {


    private TTLCache<String,String> seenId= new TTLCache<String,String>(Utils.minutesToMillis(2));



    public AssetTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }




    @Override
    public synchronized void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
	
	String id = entry.getStringValue(request,"asset_id",null);
	if(stringDefined(id)) return;
	String type = entry.getTypeHandler().getTypeProperty("asset.type","ASSET");
	int cnt=0;
	id = type+"-"+StringUtil.padLeft(""+cnt,5,"0");
	while(seenId.get(id)!=null || getDatabaseManager().tableContains("TYPE_ASSETS_BASE", "ASSET_ID",id)) {
	    cnt++;
	    id = type+"-"+StringUtil.padLeft(""+cnt,5,"0");
	}

	seenId.put(id,"");
	entry.setValue("asset_id",id);
    }

    @Override
    public void initTags(Hashtable<String, WikiTagHandler> tagHandlers) {
	tagHandlers.put("assets_barcode",this);
    }

    @Override
    public void addTagDefinition(List<String>  tags) {
    }

    public String handleTag(WikiUtil wikiUtil, Request request,
                            Entry originalEntry, Entry entry, String theTag,
                            Hashtable props, String remainder) throws Exception {
	StringBuilder sb = new StringBuilder();
	String uid = HU.getUniqueId("assets");
	HU.div(sb,"",HU.attrs("id",uid));

	sb.append("<script src='https://unpkg.com/@zxing/library@0.18.6/umd/index.min.js'></script>\n");
	HU.importJS(sb,getRepository().getHtdocsUrl("/assets/barcode.js?time=" + (new Date().getTime())));
	sb.append(HU.cssLink(getRepository().getHtdocsUrl("/assets/assets.css?time=" + (new Date().getTime()))));
	List<String> args = new ArrayList<String>();
	String type = Utils.getProperty(props,"type",entry.getStringValue(request,"asset_type",null));
	if(Utils.stringDefined(type)) {
	    TypeHandler assetType = getRepository().getTypeHandler(type);
	    if(assetType!=null) {
		Utils.add(args,"defaultType",JU.quote(type),"defaultTypeLabel",JU.quote(assetType.getLabel()));
	    }
	}
	String entryId = Utils.getProperty(props,"parent",entry.getId());
	Utils.add(args,"entryid",JU.quote(entryId));
	StringBuilder js = new StringBuilder();
	js.append(HU.call("new AssetCreator",HU.squote(uid),JU.map(args)));
	HU.script(sb,js.toString());
	return sb.toString();

    }



}
