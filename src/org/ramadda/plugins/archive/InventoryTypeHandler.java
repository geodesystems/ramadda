/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.archive;



import org.ramadda.repository.*;
import org.ramadda.repository.database.DatabaseManager;
import org.ramadda.repository.database.Tables;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.ramadda.util.WikiUtil;
import ucar.unidata.util.StringUtil;
import org.ramadda.util.sql.Clause;
import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;



import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;


@SuppressWarnings("unchecked")
public class InventoryTypeHandler extends ExtensibleGroupTypeHandler {

    public InventoryTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
	return super.getWikiInclude(wikiUtil,request, originalEntry, entry, tag, props);
    }

}
