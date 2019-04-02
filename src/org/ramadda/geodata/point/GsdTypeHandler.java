/*
* Copyright (c) 2008-2019 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.geodata.point;


import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.output.WikiConstants;
import org.ramadda.repository.type.*;
import org.ramadda.util.Utils;
import org.ramadda.util.text.CsvUtil;

import org.w3c.dom.*;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Hashtable;
import java.util.GregorianCalendar;


/**
 */
public class GsdTypeHandler extends PointTypeHandler {


    /** _more_ */
    private SimpleDateFormat dateSDF;

    /** _more_ */
    private static int IDX = PointTypeHandler.IDX_LAST + 1;

    /** _more_ */
    private static int IDX_MODEL = IDX++;


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public GsdTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    /** _more_ */
    private static final String URL_TEMPLATE =
        "https://rucsoundings.noaa.gov/get_soundings.cgi?data_source={model}&latest=latest&n_hrs=1.0&fcst_len=shortest&airport={lat}%2C{lon}&text=Ascii%20text%20%28GSD%20format%29&hydrometeors=false&start=latest";



    public String getUrlForWiki(Request request, Entry entry, String tag,
                                Hashtable props) {
        if (tag.equals(WikiConstants.WIKI_TAG_CHART)
                || tag.equals(WikiConstants.WIKI_TAG_DISPLAY)) {
            String url = super.getUrlForWiki(request, entry, tag,props);
            return url+"&latitude=${latitude}&longitude=${longitude}&model=${model}";
        }
        return super.getUrlForWiki(request, entry, tag, props);
    }


    @Override
    public String getPathForRecordEntry(Entry entry,
                                        Hashtable requestProperties)
            throws Exception {
        String url = URL_TEMPLATE;
        String lat = (String) requestProperties.get("latitude");
        String lon = (String) requestProperties.get("longitude");
        String model = (String)requestProperties.get("model");
        if(model == null || model.equals("{model}"))
            model = (String) entry.getValue(IDX_MODEL,"GFS");
        if(model.length()==0) model  = "GFS";

        url =  url.replace("{model}",model);
        url =  url.replace("{lat}",lat!=null?lat:"40");
        url =  url.replace("{lon}",lon!=null?lon:"-105");
        url = super.getPathForRecordEntry(entry, url, requestProperties);
        System.err.println("url:" + url);
        return url;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getPathForEntry(Request request, Entry entry)
            throws Exception {
        return getPathForRecordEntry(entry, request.getDefinedProperties());
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry)
            throws Exception {}

}
