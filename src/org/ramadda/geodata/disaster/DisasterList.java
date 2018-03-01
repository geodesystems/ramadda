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

package org.ramadda.geodata.disaster;




import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.TypeHandler;

import org.w3c.dom.*;;

import java.util.ArrayList;
import java.util.Hashtable;


import java.util.List;


/**
 *
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.3 $
 */

public class DisasterList extends SpecialSearch implements RequestHandler {

    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @param props _more_
     *
     * @throws Exception _more_
     */
    public DisasterList(Repository repository, Element node, Hashtable props)
            throws Exception {
        super(repository, node, props);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param entries _more_
     *
     * @throws Exception _more_
     */
    public void makeEntryList(Request request, StringBuffer sb,
                              List<Entry> entries)
            throws Exception {
        Result tmpResult =
            getRepository().getHtmlOutputHandler().outputGroup(request,
                HtmlOutputHandler.OUTPUT_HTML,
                getRepository().getEntryManager().getDummyGroup(), entries,
                new ArrayList<Entry>());

        sb.append(new String(tmpResult.getContent()));
    }





}
