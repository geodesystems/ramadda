/**
   Copyright (c) 2008-2026 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.projects.assets;

import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.output.*;
import org.ramadda.util.FormInfo;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;
import ucar.unidata.util.StringUtil;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class AssetTypeHandler extends GenericTypeHandler implements WikiTagHandler {
    private TTLCache<String,String> seenId= new TTLCache<String,String>(Utils.minutesToMillis(2));
    public AssetTypeHandler(Repository repository, org.w3c.dom.Element node)
	throws Exception {
        super(repository, node);
    }

    @Override
    public synchronized void initializeNewEntry(Request request, Entry entry,NewType newType)
	throws Exception {
	if(stringDefined(entry.getStringValue(request,"asset_id",null))) return;
	String type = entry.getTypeHandler().getTypeProperty("asset.type",null);
	if(type==null) {
	    type = entry.getTypeHandler().getType().replace("type_assets_","").toUpperCase();
	}
	int cnt=0;
	String id = type+"-"+StringUtil.padLeft(""+cnt,5,"0");
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
    public void addTagDefinition(List<String>  tags) {}

    private void initJS(Request request, StringBuilder sb) throws Exception {
	HU.div(sb,"",HU.attrs("id","barcodedebug", "style","margin:5px;padding:5px;text-align:left;width:100%;max-height:200px;overflow-y:auto;"));
	HU.importJS(sb,getRepository().getHtdocsUrl("/lib/zxing/zxing.min.js"),
		    getRepository().getHtdocsUrl("/assets/barcode.js",true));
	sb.append(HU.cssLink(getRepository().getHtdocsUrl("/assets/assets.css",true)));
    }

    @Override
    public String handleTag(WikiUtil wikiUtil, Request request,
                            Entry originalEntry, Entry entry, String theTag,
                            Hashtable props, String remainder) throws Exception {
	StringBuilder sb = new StringBuilder();
	String uid = HU.getUniqueId("assets");
	HU.div(sb,"",HU.attrs("id",uid));
	initJS(request, sb);
	List<String> args = new ArrayList<String>();
	String type = Utils.getProperty(props,"type",entry.getStringValue(request,"asset_type",null));
	if(Utils.stringDefined(type)) {
	    TypeHandler assetType = getRepository().getTypeHandler(type);
	    if(assetType!=null) {
		Utils.add(args,"defaultType",JU.quote(type),"defaultTypeLabel",JU.quote(assetType.getLabel()));
	    }
	}
	if(Utils.getProperty(props,"doScan",false))  Utils.add(args,"scanMode","true");
	String entryId = Utils.getProperty(props,"parent",entry.getId());
	Utils.add(args,"entryid",JU.quote(entryId));
	HU.script(sb,HU.call("new AssetHandler",HU.squote(uid),JU.map(args)));
	return sb.toString();
    }

    @Override
    public void addColumnToEntryForm(Request request, Entry parentEntry, Entry entry,
                                     Column column, Appendable formBuffer,
                                     Object[] values, Hashtable state,
                                     FormInfo formInfo, TypeHandler sourceTypeHandler)
	throws Exception {
	super.addColumnToEntryForm(request, parentEntry, entry, column,
				   formBuffer, values, state, formInfo, sourceTypeHandler);
	if(!column.getName().equals("asset_id")) return;
	StringBuilder sb = new StringBuilder();
	initJS(request, sb);
	List<String> args = new ArrayList<String>();
	Utils.add(args,"editMode","true");
	HU.script(sb,HU.call("new AssetHandler","null",JU.map(args)));
	formBuffer.append(sb.toString());
    }
}
