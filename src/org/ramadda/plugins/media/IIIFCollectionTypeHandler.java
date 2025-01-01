/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;


import org.ramadda.repository.*;
import org.ramadda.repository.util.SelectInfo;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.WikiUtil;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.IO;

import org.json.*;

import org.w3c.dom.*;



import java.net.URL;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;



/**
 *
 *
 */
public class IIIFCollectionTypeHandler extends TypeHandler {


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public IIIFCollectionTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository,entryNode);
    }



    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if ( !tag.equals("iiif_collection")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }

	List<Entry> children = getEntryManager().getChildren(request, entry);
	children = getEntryManager().applyFilter(request, children,new SelectInfo(request, "type_iiif_document"));
	if(children.size()==0) return "No IIIF Documents";



	return IIIFDocumentTypeHandler.getIIIFDisplay(getRepository(), request, children.get(0), props,children);
    }



}
