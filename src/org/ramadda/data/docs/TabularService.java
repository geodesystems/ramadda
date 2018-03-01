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
     *
     * @return _more_
     * @throws Exception _more_
     */
    public boolean extractSheet(Request request, Service service,
                                ServiceInput input, List args)
            throws Exception {
        return getTabularOutputHandler().extractSheet(request, service,
                input, args);
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
