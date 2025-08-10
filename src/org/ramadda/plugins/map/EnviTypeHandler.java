/**
Copyright (c) 2008-2025 Geode Systems LLC
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

import org.w3c.dom.*;

import ucar.nc2.units.DateUnit;

import ucar.unidata.util.IOUtil;
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
public class EnviTypeHandler extends LatLonImageTypeHandler  {

    public EnviTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    public String getServiceFilePath(Service service, Entry dataEntry) throws Exception {
	Entry parent = dataEntry.getParentEntry();
	String dataPath = getStorageManager().getEntryResourcePath(dataEntry);	
	File dataFile = new File(dataPath);
	File dataDir = dataFile.getParentFile();
	File headerFile = new File(IOUtil.joinDir(dataDir,IOUtil.stripExtension(dataFile.getName())+".hdr"));
	if(headerFile.exists()) {
	    return dataPath;
	}
	
	Entry hdrEntry=null;
	for(Entry child:  getEntryManager().getChildren(getRepository().getAdminRequest(), dataEntry)) {
	    if(!child.isFile()) continue;
	    File file = child.getFile();
	    if(file.getName().equals(headerFile.getName())) {
		hdrEntry = child;
		break;
	    }
	}
	if(hdrEntry==null) return null;
	String hdrPath = getStorageManager().getEntryResourcePath(hdrEntry);
	if(new File(hdrPath).getParentFile().equals(new File(dataPath).getParentFile())) {
	    System.err.println("same dir");
	    return dataPath;
	}

	File dir = getStorageManager().getProcessDir(getRepository().getGUID());
	

	return null;

    }

}
