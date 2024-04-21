/**
Copyright (c) 2008-2024 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.projects.kbocc;

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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;


public class KboccHydroTypeHandler extends PointTypeHandler {

    private JSONArray loggers;
    public KboccHydroTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    @Override
    public void initializeNewEntry(Request request, Entry entry,
                                   boolean fromImport)
            throws Exception {

	if(fromImport) return;
	if(entry.hasLocationDefined()) return;
	File file = entry.getFile();
	if(!file.exists()) return;
	if(loggers==null) {
	    loggers = new JSONArray(getStorageManager().readUncheckedSystemResource("/org/ramadda/projects/kbocc/loggers.json"));
	}
	String fileName = getStorageManager().getOriginalFilename(file.getName());
	for(int i=0;i<loggers.length();i++) {
	    JSONObject logger=loggers.getJSONObject(i);
	    String id = logger.getString("id").trim();
	    if(fileName.startsWith(id)) {
		entry.setLatitude(logger.getDouble("latitude"));
		entry.setLongitude(logger.getDouble("longitude"));
		entry.setValue("location",logger.getString("location"));
		entry.setValue("notes",logger.getString("notes"));		
		break;
	    }
	}

	
    }

}
