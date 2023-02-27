/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.util;


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

    /**  */
    Boolean ascending = null;

    boolean hadOrderBy = false;

    boolean syntheticOk = true;


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     */
    public SelectInfo(Request request, Entry entry) {
        this.request = request;
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

        orderBy = request.getString(Constants.ARG_ORDERBY, orderBy);
        //Use the metadata if there wasn't an orderby in the request arg
        if ((orderBy == null) && (entry != null)) {
            try {
                Metadata sortMetadata =
                    request.getRepository().getMetadataManager()
                        .getSortOrderMetadata(request, entry);
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

    /**
      * @return _more_
     */
    public boolean hasAscending() {
        init();

        return ascending != null;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        init();

        return " max:" + max;
    }


}
