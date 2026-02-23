/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.geo;

import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.service.Service;

import org.ramadda.util.WikiUtil;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

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


public class CoreBaseTypeHandler extends GenericTypeHandler {
    private Service tiffService;
    


    public CoreBaseTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
	String service = "<service link=\"gdal_tiff2coreimage\"  target=\"attachment\"/>";
	Element root = XmlUtil.getRoot(service);
	tiffService = new Service(repository, root);
    }

    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
	super.initializeNewEntry(request, entry,newType);
	if(!isNew(newType)) return;
	CoreUtil.initializeNewEntry(request, entry);
	if(entry.getTypeHandler().isType(CoreUtil.TYPE_CORE_IMAGE)) {
	    //	    applyService(request,entry,tiffService);
	}


    }
}
