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

package org.ramadda.geodata.ogc;


import org.ramadda.repository.*;
import org.ramadda.repository.output.*;

import org.ramadda.util.CswUtil;
import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;

import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.xml.XmlUtil;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class CswOutputHandler extends OutputHandler {

    /** output type for viewing map */
    public static final OutputType OUTPUT_CSW = new OutputType("CSW", "csw",
                                                    OutputType.TYPE_FEEDS,
                                                    "", "/icons/globe.jpg");

    /**
     * Constructor
     *
     * @param repository The repository
     * @param element The xml element from outputhandlers.xml
     * @throws Exception On badness
     */
    public CswOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        //add in the output types
        addType(OUTPUT_CSW);
    }



    /**
     * This method gets called to add to the types list the OutputTypes that are applicable
     * to the given State.  The State can be viewing a single Entry (state.entry non-null),
     * viewing a Group (state.group non-null).
     *
     * @param request The request
     * @param state The state
     * @param links _more_
     *
     *
     * @throws Exception On badness
     */
    @Override
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if (state.getEntry() != null) {
            links.add(makeLink(request, state.getEntry(), OUTPUT_CSW));
        }
    }



    /**
     * Output the html for the given entry
     *
     * @param request the request
     * @param outputType type of output
     * @param entry the entry
     *
     * @return the result
     *
     * @throws Exception on badness
     */
    @Override
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        StringBuffer sb = new StringBuffer();

        return new Result("", sb);
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
    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> subGroups,
                              List<Entry> entries)
            throws Exception {
        StringBuffer sb = new StringBuffer();

        return new Result("", sb);

    }

}
