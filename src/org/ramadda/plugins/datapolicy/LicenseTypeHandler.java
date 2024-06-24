/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.datapolicy;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.database.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.SelectInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 */
@SuppressWarnings("unchecked")
public class LicenseTypeHandler extends GenericTypeHandler {


    public LicenseTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {

        if ( !tag.equals("type_usage_descriptor") && !tag.equals("type_license")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
	String id = entry.getStringValue(request,"usage_id",
					 entry.getStringValue(request,"license_id",""));
	String wiki = "{{usage  descriptor=\"" + id+"\"  includeName=\"true\" showDescription=\"true\" decorate=\"true\" }}";

	return getWikiManager().wikifyEntry(request, entry, wiki);

    }




}
