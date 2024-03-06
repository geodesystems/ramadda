/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.docs;


import org.ramadda.repository.*;


import org.ramadda.service.*;

import org.w3c.dom.*;

import java.util.List;



/**
 *
 * @author Jeff McWhirter/geodesystems.com
 */
public class TabularService extends Service {


    /**
     * ctor
     *
     * @param repository _more_
     * @throws Exception _more_
     */
    public TabularService(Repository repository) throws Exception {
        super(repository, (Element) null);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public TabularOutputHandler getTabularOutputHandler() throws Exception {
        return (TabularOutputHandler) getRepository().getOutputHandler(
            TabularOutputHandler.class);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param service _more_
     * @param input _more_
     * @param args _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean csv(Request request, Service service, ServiceInput input,
                       List args)
            throws Exception {
        return getTabularOutputHandler().csv(request, service, input, args);
    }


}
