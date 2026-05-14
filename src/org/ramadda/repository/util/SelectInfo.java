/**
   Copyright (c) 2008-2026 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.util;

import org.ramadda.repository.search.SearchProvider;
import org.ramadda.repository.Constants;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Request;
import org.ramadda.repository.metadata.Metadata;

import org.ramadda.util.Utils;
import org.ramadda.util.sql.Clause;

import ucar.unidata.util.Misc;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds information for generating entries in entries.xml
 */
public class SelectInfo implements Constants {
    private boolean haveInited = false;
    private Request request;
    private Entry entry;
    private List<Clause> where;
    private int max = -1;
    String orderBy;
    String type;    
    String name;
    Boolean ascending = null;
    boolean hadOrderBy = false;
    boolean syntheticOk = true;
    String filter;
    String fromDate;
    String toDate;
    private StringBuilder msgs = new StringBuilder();

    public SelectInfo(Request request) {
        this.request = request;
    }

    public SelectInfo(Request request, Entry entry) {
        this(request);
        this.entry   = entry;
    }

    public SelectInfo(Request request, int max) {
        this(request);
	this.max=max;
    }

    public SelectInfo(Request request, Entry entry, int max, String orderBy,
                      boolean ascending) {
        this(request, entry);
	this.max = max;
        this.orderBy   = orderBy;
        this.ascending = Boolean.valueOf(ascending);
	hadOrderBy = orderBy!=null;
    }

    public SelectInfo(Request request, Entry entry, List<Clause> where) {
        this(request, entry, where, -1);
    }

    public SelectInfo(Request request, Entry entry, List<Clause> where,
                      int max) {
        this(request, entry);
	this.where = where;
	this.max   = max;
    }

    public SelectInfo(Request request, Entry entry, List<Clause> where,
                      int max, boolean syntheticOk ) {	
        this(request, entry,where,max);
        this.syntheticOk =syntheticOk; 
    }

    public SelectInfo(Request request, Entry entry,  boolean syntheticOk ) {	
        this(request, entry);
        this.syntheticOk =syntheticOk; 
    }

    public SelectInfo(Request request, String type) {
	this(request);
	this.type = type;
    }

    public void addMessage(SearchProvider provider, String msg)
            throws Exception {
        if (msg.length() > 0) {
            synchronized (msgs) {
                msgs.append(msg);
                msgs.append("<br>");
            }
        }
    }

    public void setRequest (Request value) {
	request = value;
    }

    public Request getRequest () {
	return request;
    }

    public void setType (String value) {
	type = value;
    }

    public String getType () {
	return type;
    }

    public void setEntry (Entry value) {
	entry = value;
    }

    public Entry getEntry () {
	return entry;
    }

    public void setSyntheticOk (boolean value) {
	syntheticOk = value;
    }

    public boolean getSyntheticOk () {
	return syntheticOk;
    }

    public void init() {
        if (haveInited) {
            return;
        }
        haveInited = true;
        if (max < 0) {
            if (entry != null) {
                max = entry.getTypeHandler().getDefaultQueryLimit(request,
								  entry);
            }
        }
        max = request.get(Constants.ARG_MAX, max);
        if (max <= 0) {
            max = Constants.DB_MAX_ROWS;
        }

	if(orderBy==null)
	    orderBy = request.getString(Constants.ARG_ORDERBY, null);
        //Use the metadata if there wasn't an orderby in the request arg
        if ((orderBy == null) && (entry != null)) {
            try {
                Metadata sortMetadata =
                    request.getRepository().getMetadataManager()
		    .getSortOrderMetadata(request, entry,true);
                if (sortMetadata != null) {
                    if (Misc.equals(sortMetadata.getAttr2(), "true")) {
                        ascending = Boolean.valueOf(true);
                    } else {
                        ascending = Boolean.valueOf(false);
                    }
                    orderBy = sortMetadata.getAttr1();
                    String tmp = sortMetadata.getAttr3();
                    if (Utils.stringDefined(tmp)) {
                        int tmpMax = Integer.parseInt(tmp.trim());
                        if (tmpMax > 0) {
                            max = tmpMax;
                        }
                    }
                }
            } catch (Exception ignore) {}
        }

	hadOrderBy = orderBy!=null;

        if (orderBy == null) {
            orderBy = Constants.ORDERBY_CREATEDATE;
        }
        if (request.defined(Constants.ARG_ASCENDING)) {
            ascending = Boolean.valueOf(request.getString(Constants.ARG_ASCENDING,"true"));
        }

	//	System.err.println("Entry:" + entry +" ORDER:" + orderBy +" ascending:" + ascending);

    }

    public boolean getHadOrderBy () {
	return hadOrderBy;
    }

    public List<Clause> getWhere() {
        return where;
    }

    public int getMax() {
        init();
        return max;
    }

    public void setMax(int max) {
	this.max = max;
    }

    public int getSkip() {
        init();

        return request.get(ARG_SKIP, 0);
    }

    public String getOrderBy() {
        init();
        return orderBy;
    }

    public void setOrderBy(String o) {
        this.orderBy = o;
    }    

    public boolean getAscending() {
        init();
        if (ascending != null) {
            return ascending;
        }

        return false;
    }

    public void setAscending(boolean a) {
	this.ascending = a;
    }

    public boolean hasAscending() {
        init();
        return ascending != null;
    }

    public void setFilter (String value) {
	filter = value;
    }

    public String getFilter () {
	return filter;
    }

    public void setName (String value) {
	name = value;
    }

    public String getName () {
	return name;
    }

    public void setFromDate (String value) {
	fromDate = value;
    }

    public String getFromDate () {
	return fromDate;
    }

    public void setToDate (String value) {
	toDate = value;
    }

    public String getToDate () {
	return toDate;
    }

    public String toString() {
        init();
        return "entry:" + entry.getName()+" orderBy:" + orderBy +" ascending:" + ascending +" max:" + max;
    }

}
