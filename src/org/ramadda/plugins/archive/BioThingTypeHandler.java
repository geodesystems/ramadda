/**
   Copyright (c) 2008-2023 Geode Systems LLC
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
public class BioThingTypeHandler extends ExtensibleGroupTypeHandler {

    public BioThingTypeHandler(Repository repository, Element entryNode)
	throws Exception {
        super(repository, entryNode);
    }

    @Override
    public void addColumnToEntryForm(Request request, Column column,
                                     Appendable formBuffer, Entry parentEntry,Entry entry,
                                     Object[] values, Hashtable state,
                                     FormInfo formInfo,
                                     TypeHandler sourceTypeHandler)
	throws Exception {
	super.addColumnToEntryForm(request, column, formBuffer, parentEntry, entry,
				   values, state, formInfo,
				   sourceTypeHandler);
	if(!column.getName().equals("common_name")) return;
	StringBuilder sb = new StringBuilder();
	//	sb.append(HU.input("","",HU.attrs("placeholder","Enter name and press return to search",
	//					  "size","40","id",uid)));
	HU.importJS(sb,getRepository().getUrlBase()+"/archive/itis.js");
	HU.script(sb,HU.call("Itis.init"));
	formBuffer.append(sb.toString());
    }




}