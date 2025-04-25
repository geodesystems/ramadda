/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.archive;

import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.SelectInfo;

import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.ArrayList;
import java.util.Date;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.List;
import java.util.Locale;

/**
 */
public class TaxonomyRecordTypeHandler extends ExtensibleGroupTypeHandler {

    public TaxonomyRecordTypeHandler(Repository repository, Element entryNode)
	throws Exception {
        super(repository, entryNode);
    }

    @Override
    public void addColumnToEntryForm(Request request, Entry parentEntry, Entry entry,
                                     Column column, Appendable formBuffer,
                                     Object[] values, Hashtable state,
                                     FormInfo formInfo,
                                     TypeHandler sourceTypeHandler)

    /*    public void addColumnToEntryForm(Request request, Column column,
                                     Appendable formBuffer, Entry parentEntry,Entry entry,
                                     Object[] values, Hashtable state,
                                     FormInfo formInfo,
                                     TypeHandler sourceTypeHandler)*/
	throws Exception {
	super.addColumnToEntryForm(request, parentEntry, entry,
				   column,formBuffer,
				   values, state, formInfo,
				   sourceTypeHandler);
	if(!column.getName().equals("common_name")) return;
	StringBuilder sb = new StringBuilder();
	HU.importJS(sb,getRepository().getHtdocsUrl("/archive/itis.js"));
	HU.script(sb,HU.call("Itis.init"));
	formBuffer.append(sb.toString());
    }

}
