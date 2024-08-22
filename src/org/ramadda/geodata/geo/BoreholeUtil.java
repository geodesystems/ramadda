/**
Copyright (c) 2008-2024 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.geo;

import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import ucar.unidata.util.StringUtil;

import org.w3c.dom.*;
import org.json.*;
import java.net.URL;
import java.io.*;
import org.json.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;


public class BoreholeUtil {
    private static JSONArray holes;
    public static void initializeNewEntry(Request request, Entry entry) 
            throws Exception {
	TypeHandler typeHandler = entry.getTypeHandler();
	if(typeHandler.isType("type_borehole_xrf")) {
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
