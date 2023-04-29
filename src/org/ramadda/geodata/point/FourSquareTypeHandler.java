/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;

import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.text.Seesv;


import org.ramadda.data.record.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.services.*;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;

import java.net.URL;
import java.util.Hashtable;
import java.util.List;

import java.io.*;
import org.w3c.dom.Element;
import org.json.*;


/**
 * TypeHandler for Foursquare API
 * requires the export FOURSQUARE_API_KEY environment variable to be set
 *
 */
public class FourSquareTypeHandler extends PointTypeHandler {

    public static final String PROP_KEY = "FOURSQUARE_API_KEY";

    /** _more_ */
    public static final String URL =
        "https://api.foursquare.com/v3/places/search";
    //?categories=$cat&ne=40.041,-105.209&sw=39.978,-105.301"	\

    private static int IDX =
        org.ramadda.data.services.RecordTypeHandler.IDX_LAST + 1;


    /** _more_ */
    public static final int IDX_SITE_ID = IDX++;


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     *
     * @throws Exception On badnes
     */
    public FourSquareTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    @Override
    public boolean getForUser() {
	return Utils.stringDefined(getRepository().getProperty(PROP_KEY));
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception On badnes
     */
    @Override
    public String getPathForEntry(Request request, Entry entry, boolean forRead)
            throws Exception {
        String siteId = entry.getStringValue(IDX_SITE_ID, "");
        String url = URL.replace("{station}", siteId);
	if(request.defined("bounds")) {
	    List<String> nwse = Utils.split(request.getString("bounds",""),",",true,true);
	    if(nwse.size()==4) {
		//Don't encode the args
		url=HU.url(url,new String[]{"ne",nwse.get(0)+","+ nwse.get(1),"sw",nwse.get(2)+","+nwse.get(3)},false);
	    }
	}

	System.err.println("URL:" + url);
        return url;
    }

    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        return new FourSquareRecordFile(getRepository(), entry,
					getPathForEntry(request, entry,true));
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Dec 8, '18
     * @author         Enter your name here...
     */
    public static class FourSquareRecordFile extends CsvFile {

        /** _more_ */
        Repository repository;

        /** _more_ */
        Entry entry;

        /**
         * _more_
         *
         *
         * @param repository _more_
         * @param entry _more_
         * @param filename _more_
         *
         * @throws IOException _more_
         */
        public FourSquareRecordFile(Repository repository, Entry entry,
                                   String filename)
                throws IOException {
            super(filename);
            this.repository = repository;
            this.entry      = entry;
        }

	@Override
	public InputStream doMakeInputStream(Seesv csvUtil, boolean buffered) throws Exception {
	    return null;
	    //TODO:
	    //	    return IO.getInputStreamFromGet(new URL(getFilename()), "accept",   "application/json",
	    //					    "Authorization", repository.getProperty(PROP_KEY,"").trim());
	}
    }

}
