/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

package org.ramadda.geodata.echo;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;






/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class EchoOutputHandler extends OutputHandler {



    /** _more_ */
    public static final OutputType OUTPUT_ECHO_XML =
        new OutputType("Echo-XML", "echo.xml",
                       OutputType.TYPE_FEEDS | OutputType.TYPE_FORSEARCH, "",
                       "/echo/nasa.png");


    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public EchoOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_ECHO_XML);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if (state.isDummyGroup()) {
            return;
        }
        if (state.group != null) {
            links.add(makeLink(request, state.group, OUTPUT_ECHO_XML,
                               "/"
                               + IOUtil.stripExtension(state.group.getName())
                               + ".echo.zip"));
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> subGroups,
                              List<Entry> entries)
            throws Exception {
        OutputStream os = request.getHttpServletResponse().getOutputStream();
        request.getHttpServletResponse().setContentType("application/zip");
        List<Entry> collections = new ArrayList<Entry>();
        collections.add(group);
        EchoPublisher publisher = new EchoPublisher(getRepository(), "dummy");
        publisher.writeCollections(collections, os, true);
        Result result = new Result();
        result.setNeedToWrite(false);

        return result;
    }


}
