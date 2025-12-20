/*
 * Copyright (c) 2008-2026 Geode Systems LLC
 * SPDX-License-Identifier: Apache-2.0
 */


package org.ramadda.repository.output;

import org.ramadda.repository.Constants;
import org.ramadda.repository.DateHandler;
import org.ramadda.repository.search.SpecialSearch;

import org.ramadda.util.Utils;
import ucar.unidata.util.StringUtil;

import java.util.ArrayList;
import java.util.List;


public class WikiTag {
    String label;
    String tag;
    String attrs;
    String tt;
    List<String> attrsList = new ArrayList<String>();
    public WikiTag(String tag) {
	this(tag, null);
    }

    public   WikiTag(String tag, String label, String... attrs) {
	boolean debug=false;
	this.tag = tag;
	if (label == null) {
	    label = StringUtil.camelCase(tag);
	}
	this.label = label;
	for (String attr : attrs) {
	    attrsList.add(attr);
	}
	if (attrs.length == 1) {
	    this.attrs = attrs[0];
	} else {
	    StringBuilder sb  = new StringBuilder();
	    int     lineLength = 0;
	    int cnt =0;
	    boolean addSpace = true;
	    for (int i = 0; i < attrs.length; i += 2) {
		cnt++;
		if (lineLength > 80 || cnt>3 ||attrs[i].startsWith("#")) {
		    sb.append("_newline_");
		    addSpace=false;
		    lineLength = 0;
		    cnt=0;
		} 


		if(attrs[i]!=null && attrs[i].equals(WikiTags.ATTR_TT)) {
		    tt = attrs[i+1];
		    if(debug) System.err.println("TT:" + tt);
		    continue;
		}

		lineLength += attrs[i].length() + attrs[i + 1].length();
		if(debug) System.err.println("attr:" + attrs[i] + "=" + attrs[i+1]);
		attr(sb, addSpace,attrs[i], attrs[i + 1]);
		addSpace=true;
	    }
	    this.attrs = sb.toString();
	    if(debug) System.err.println("attrs:" + this.attrs);
	}
    }

    private static void attr(StringBuilder sb, boolean addSpace,String name, String value) {
        Utils.append(sb, addSpace?" ":"", name, "=", "&quote;", value, "&quote;", " ");
    }    
}
