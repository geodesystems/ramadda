/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.map;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.AccessException;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.type.*;



import org.ramadda.service.Service;
import org.ramadda.service.ServiceOutput;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.geo.Bounds;


import org.w3c.dom.*;

import ucar.nc2.units.DateUnit;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;
import java.awt.image.*;
import java.awt.image.BufferedImage;
import javax.imageio.*;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;



@SuppressWarnings("unchecked")
public class GeoDatabaseTypeHandler extends GenericTypeHandler {

    public GeoDatabaseTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    @Override
    public void handleServiceResults(Request request, Entry entry,
                                     Service service, ServiceOutput output)
            throws Exception {
        super.handleServiceResults(request, entry, service, output);
        List<Entry> entries = output.getEntries();
        if (entries.size() != 0) {
            return;
        }
	Bounds bounds = null;
        String results = output.getResults();
	//	System.err.println("results:" + results);
	String layer = null;
	String geometry = null;
	for(String line: Utils.split(results,"\n",true,true)) {
	    if(line.startsWith("Layer name:")) {
		layer = null;
		List<String> toks = Utils.splitUpTo(line,":",2);
		if(toks.size()==2) {
		    layer = toks.get(1).trim();
		}
		continue;
	    }
	    if(line.startsWith("Geometry:")) {
		geometry=null;
		List<String> toks = Utils.splitUpTo(line,":",2);
		if(toks.size()==2) {
		    geometry = toks.get(1).trim();
		}
		continue;
	    }

	    if(line.startsWith("Feature Count:")) {
		List<String> toks = Utils.splitUpTo(line,":",2);
		if(toks.size()!=2) continue;
		String fc = toks.get(1).trim();
		if(layer!=null && geometry!=null)
		    getMetadataManager().addMetadata(request,entry, "geodatabase_layer", false, layer,geometry,fc);
		layer = null;geometry=null;
		continue;
	    }






	    if(line.startsWith("Extent:")) {
		//Extent: (-104.055600, 42.490360) - (-96.437901, 45.774391)
		List<String> toks = Utils.splitUpTo(line,":",2);
		if(toks.size()!=2) continue;
		String b = toks.get(1).trim();
		//private static double[] getLatLon(String line) {
		int index = b.indexOf(")");
		if(index<0) continue;
		String s1 = b.substring(0,index);
		String s2 = b.substring(index+1);			
		double[]p1 =getLatLon(s1);
		double[]p2 =getLatLon(s2);		
		if(p1!=null) {
		    if(bounds==null) bounds=new Bounds(p1[1],p1[0]);
		    else bounds.expand(p1[1],p1[0]);
		}
		if(p2!=null) {
		    if(bounds==null) bounds=new Bounds(p2[1],p2[0]);
		    else bounds.expand(p2[1],p2[0]);
		}
		continue;
	    }
	}

	if(bounds!=null) {
	    entry.setBounds(bounds);
	}
    }


    private static double[] getLatLon(String line) {
        line = line.trim();
        line = StringUtil.findPattern(line, ".*\\(([^\\)]+)\\.*");
        if (line == null) {
            return null;
        }

        List<String> toks = StringUtil.split(line, ",", true, true);
        if (toks.size() != 2) {
            return null;
        }

        return new double[] { decodeLatLon(toks.get(0)),
                              decodeLatLon(toks.get(1)) };
    }

    private static double decodeLatLon(String s) {
        s = s.replace("d", ":");
        s = s.replace("'", ":");
        s = s.replace("\"", "");

        return Misc.decodeLatLon(s);
    }

}
