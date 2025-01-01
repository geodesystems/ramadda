/**
   Copyright (c) 2008-2025 Geode Systems LLC
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

    /**  */
    private boolean haveInited = false;

    /**  */
    private Request request;

    /**  */
    private Entry entry;

    /** _more_ */
    private List<Clause> where;

    /** _more_ */
    private int max = -1;

    /**  */
    String orderBy;
    String type;    

    String name;
    
    /**  */
    Boolean ascending = null;

    boolean hadOrderBy = false;

    boolean syntheticOk = true;

    String filter;

    String fromDate;
    String toDate;
    
    /** _more_ */
    private StringBuilder msgs = new StringBuilder();


    public SelectInfo(Request request) {
        this.request = request;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     */
    public SelectInfo(Request request, Entry entry) {
        this(request);
        this.entry   = entry;
    }

    /**
     *
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param max _more_
     * @param orderBy _more_
     * @param ascending _more_
     */
    public SelectInfo(Request request, Entry entry, int max, String orderBy,
                      boolean ascending) {
        this(request, entry);
        this.max       = max;
        this.orderBy   = orderBy;
        this.ascending = Boolean.valueOf(ascending);
	hadOrderBy = orderBy!=null;
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param where _more_
     */
    public SelectInfo(Request request, Entry entry, List<Clause> where) {
        this(request, entry, where, -1);
    }

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param where _more_
     * @param max _more_
     */
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

    /**
     * _more_
     *
     * @param provider _more_
     * @param msg _more_
     *
     * @throws Exception _more_
     */
    public void addMessage(SearchProvider provider, String msg)
            throws Exception {
        if (msg.length() > 0) {
            synchronized (msgs) {
                msgs.append(msg);
                msgs.append("<br>");
            }
        }
    }



    /**
       Set the Request property.

       @param value The new value for Request
    **/
    public void setRequest (Request value) {
	request = value;
    }

    /**
       Get the Request property.

       @return The Request
    **/
    public Request getRequest () {
	return request;
    }

    /**
       Set the Type property.

       @param value The new value for Type
    **/
    public void setType (String value) {
	type = value;
    }

    /**
       Get the Type property.

       @return The Type
    **/
    public String getType () {
	return type;
    }





    /**
       Set the Entry property.

       @param value The new value for Entry
    **/
    public void setEntry (Entry value) {
	entry = value;
    }

    /**
       Get the Entry property.

       @return The Entry
    **/
    public Entry getEntry () {
	return entry;
    }



    /**
     */
    /**
       Set the SyntheticOk property.

       @param value The new value for SyntheticOk
    **/
    public void setSyntheticOk (boolean value) {
	syntheticOk = value;
    }

    /**
       Get the SyntheticOk property.

       @return The SyntheticOk
    **/
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

    /**
     * _more_
     *
     * @return _more_
     */
    public List<Clause> getWhere() {
        return where;
    }



    /**
     *  Get the Max property.
     *
     *  @return The Max
     */
    public int getMax() {
        init();
        return max;
    }

    public void setMax(int max) {
	this.max = max;
    }

    /**
     * @return _more_
     */
    public int getSkip() {
        init();

        return request.get(ARG_SKIP, 0);
    }

    /**
     *  Get the OrderBy property.
     *
     *  @return The OrderBy
     */
    public String getOrderBy() {
        init();
        return orderBy;
    }

    public void setOrderBy(String o) {
        this.orderBy = o;
    }    


    /**
     *  Get the Ascending property.
     *
     *  @return The Ascending
     */
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

    /**
     * @return _more_
     */
    public boolean hasAscending() {
        init();
        return ascending != null;
    }

    /**
       Set the Filter property.

       @param value The new value for Filter
    **/
    public void setFilter (String value) {
	filter = value;
    }

    /**
       Get the Filter property.

       @return The Filter
    **/
    public String getFilter () {
	return filter;
    }


    /**
       Set the Name property.

       @param value The new value for Name
    **/
    public void setName (String value) {
	name = value;
    }

    /**
       Get the Name property.

       @return The Name
    **/
    public String getName () {
	return name;
    }


    /**
       Set the FromDate property.

       @param value The new value for FromDate
    **/
    public void setFromDate (String value) {
	fromDate = value;
    }

    /**
       Get the FromDate property.

       @return The FromDate
    **/
    public String getFromDate () {
	return fromDate;
    }

    /**
       Set the ToDate property.

       @param value The new value for ToDate
    **/
    public void setToDate (String value) {
	toDate = value;
    }

    /**
       Get the ToDate property.

       @return The ToDate
    **/
    public String getToDate () {
	return toDate;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        init();
        return "entry:" + entry.getName()+" orderBy:" + orderBy +" ascending:" + ascending +" max:" + max;
    }


}
