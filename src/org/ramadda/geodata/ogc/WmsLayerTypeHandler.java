/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.ogc;


import org.ramadda.repository.*;
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
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
        //Read the xml
        String url = entry.getResource().getPath();
	if(stringDefined(url)) {
	    url = url.replace("/[0-9]+/[0-9]+/[0-9]+","${z}/${x}/${y}");
	    entry.getResource().setPath(url);
	}
    }



}
