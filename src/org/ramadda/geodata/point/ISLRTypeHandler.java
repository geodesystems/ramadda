/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;

import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.GeoUtils;

import ucar.unidata.util.StringUtil;
import org.w3c.dom.*;

import java.net.URL;
import java.io.*;

import java.util.ArrayList;
import java.util.List;

public class ISLRTypeHandler extends PointTypeHandler {
    private static int IDX = PointTypeHandler.IDX_LAST + 1;
    private static int IDX_STATION_ID = IDX++;

    public ISLRTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
	throws Exception {
	if(!isNew(newType)) return;
	//sl_taskforce_scenarios_psmsl_id_759.csv
	String id = StringUtil.findPattern(entry.getResource().getPath(),".*_(\\d+)\\..*");
	if(!stringDefined(id)) return;
	entry.setValue(IDX_STATION_ID,id);
	String url = "https://www.sonel.org/?page=altimetrie&psmslId=" + id;
	IO.Result result = IO.doGetResult(new URL(url));
	if(result.getError()) {
	    getLogManager().logSpecial("Failed to read ISLR station:" + url);
	    return;
	}	    
	String html = result.getResult();
	String name = StringUtil.findPattern(html,"(?s)>Tide gauge name:\\s*</td>\\s*<td>([^<]+)<.*");
	if(name!=null) entry.setName(name);
	String lat = StringUtil.findPattern(html,"(?s)<td>Latitude\\s*:\\s*</td>\\s*<td>\\s*([-\\d\\.]+)</td>");
	String lon = StringUtil.findPattern(html,"(?s)<td>Longitude\\s*:\\s*</td>\\s*<td>\\s*([-\\d\\.]+)</td>");	
	if(lat!=null && lon!=null) {
	    entry.setLatitude(GeoUtils.decodeLatLon(lat));
	    entry.setLongitude(GeoUtils.decodeLatLon(lon));
	}
    }

}
