/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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
							       getRepository().getEntryManager().getDummyGroup(), entries);

        sb.append(new String(tmpResult.getContent()));
    }





}
