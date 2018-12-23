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

package org.ramadda.geodata.thredds;



import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.TypeHandler;

import org.w3c.dom.*;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;

import java.util.Hashtable;



/**
 *
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.3 $
 */

public class ThreddsApiHandler extends RepositoryManager implements RequestHandler {

    /**
     * ctor
     *
     * @param repository the repository
     * @param node xml from api.xml
     * @param props properties from xml
     *
     * @throws Exception On badness
     */
    public ThreddsApiHandler(Repository repository, Element node,
                             Hashtable props)
            throws Exception {
        super(repository);
    }


    /**
     * api hook
     *
     * @param request the request
     *
     * @return the thredd catalog response
     *
     * @throws Exception On badness
     */
    public Result processThreddsRequest(Request request) throws Exception {
        CatalogOutputHandler catalogOutputHandler =
            (CatalogOutputHandler) getRepository().getOutputHandler(
                CatalogOutputHandler.OUTPUT_CATALOG.getId());

        String path   = request.getRequestPath();
        String prefix = getRepository().getUrlBase() + "/thredds";
        Entry  entry  = null;
        if (path.length() > prefix.length()) {
            String suffix = path.substring(prefix.length());
            suffix = java.net.URLDecoder.decode(suffix, "UTF-8");
            suffix = IOUtil.stripExtension(suffix);
            entry = getEntryManager().findEntryFromName(request, suffix,
                    request.getUser(), false);
            if (entry == null) {
                getEntryManager().fatalError(request,
                                             "Could not find entry:"
                                             + suffix);
            }
        }
        if (entry == null) {
            entry = getEntryManager().getRootEntry();
        }

        request.put(ARG_OUTPUT, CatalogOutputHandler.OUTPUT_CATALOG.getId());

        return getEntryManager().processEntryShow(request, entry);
    }
}
