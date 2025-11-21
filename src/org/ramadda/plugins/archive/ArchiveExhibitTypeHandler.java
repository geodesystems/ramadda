/**
Copyright (c) 2008-2025 Geode Systems LLC
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
public class ArchiveExhibitTypeHandler extends ExtensibleGroupTypeHandler {

    public ArchiveExhibitTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    private void wikify(Request request, Entry entry, StringBuilder sb, String wiki) throws Exception {
	sb.append(getWikiManager().wikifyEntry(request, entry,wiki));
    }

    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
	StringBuilder sb = new StringBuilder();
        if (tag.equals("archive_exhibit")) {

	}
	
	wikify(request, entry,sb,"----");
	return sb.toString();
    }
}
