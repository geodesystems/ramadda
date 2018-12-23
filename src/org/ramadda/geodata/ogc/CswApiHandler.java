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

package org.ramadda.geodata.ogc;


import org.ramadda.repository.*;
import org.ramadda.util.CswUtil;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.WfsUtil;

import org.w3c.dom.Element;

import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 *
 */

public class CswApiHandler extends RepositoryManager {

    /**
     * ctor
     *
     * @param repository the main ramadda repository
     * @param node xml node from nlasapi.xml
     * @param props extra properties
     *
     * @throws Exception On badness
     */
    public CswApiHandler(Repository repository, Element node, Hashtable props)
            throws Exception {
        super(repository);
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processCswRequest(Request request) throws Exception {
        //        /ogc/wfs/<type>
        String wfsRequest = request.getString(WfsUtil.ARG_REQUEST,
                                WfsUtil.REQUEST_GETCAPABILITIES);
        if (wfsRequest.equals(WfsUtil.REQUEST_GETCAPABILITIES)) {
            return processGetCapabilitiesRequest(request);
        } else if (wfsRequest.equals(WfsUtil.REQUEST_DESCRIBEFEATURETYPE)) {
            return processDescribeFeatureTypeRequest(request);
        } else {
            return handleError(request, "Unknown request:" + wfsRequest);
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param message _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleError(Request request, String message)
            throws Exception {
        request.setReturnFilename("error.xml");
        StringBuffer xml = new StringBuffer();

        return new Result("", xml, "application/xml");
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processGetCapabilitiesRequest(Request request)
            throws Exception {
        request.setReturnFilename("csw.xml");
        String xml = getRepository().getResource(
                         "/org/ramadda/geodata/ogc/capabilities.xml");
        //        xml = xml.replaceAll("${wfs.onlineresource}",
        //                             getRepository().getUrlBase() + "/ogc/wfs");

        return new Result("", new StringBuffer(xml), "application/xml");
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processDescribeFeatureTypeRequest(Request request)
            throws Exception {
        request.setReturnFilename("wfs.xml");
        StringBuffer xml = new StringBuffer();

        return new Result("", xml, "application/xml");
    }

}
