/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.geo;

import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.Utils;
import org.json.*;

public class CoreUtil {
    public static final String TYPE_CORE_BASE = "type_core_base";
    public static final String TYPE_CORE_IMAGE = "type_core_coreimage";
    public static final String TYPE_COREBOX_XML = "type_core_corebox_xml";
    public static final String TYPE_CORE_XRF = "type_core_xrf";


    private static JSONArray holes;
    public static void initializeNewEntry(Request request, Entry entry) 
            throws Exception {
	TypeHandler typeHandler = entry.getTypeHandler();
	if(typeHandler.isType(TYPE_CORE_XRF)) {
	    Double d=(Double)entry.getValue(request, "xrf_voltage");
	    if(d!=null) {	    
		double voltage = d;
		if(voltage==15) {
		    entry.setValue("fields_to_show","k_wt,fe_wt,ca_wt,si_wt");
		} else if (voltage==40) {
		    entry.setValue("fields_to_show","fe_wt,cu_wt,sr_wt,ba_wt");
		}
	    }
	}


	String holeId = (String) entry.getValue(request,"hole");
	if(!typeHandler.stringDefined(holeId)) return;
	if(holes==null) {
	    holes = new JSONArray(typeHandler.getStorageManager().readUncheckedSystemResource("/org/ramadda/geodata/geo/resources/holes.json"));
	}
	holeId=holeId.trim();
	for(int i=0;i<holes.length();i++) {
	    JSONObject hole=holes.getJSONObject(i);
	    String id = hole.getString("id").trim();
	    if(id.equals(holeId)) {
		entry.setLatitude(hole.getDouble("latitude"));
		entry.setLongitude(hole.getDouble("longitude"));		
	    }
	}

    }

}
