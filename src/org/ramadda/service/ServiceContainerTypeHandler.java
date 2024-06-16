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

package org.ramadda.service;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.HtmlOutputHandler;
import org.ramadda.repository.output.ServiceOutputHandler;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlUtil;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 */
public class ServiceContainerTypeHandler extends ServiceTypeHandler {

    /** _more_ */
    private  static int IDX = ServiceTypeHandler.IDX_LAST + 1;

    public static final int IDX_SERIAL = IDX++;

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public ServiceContainerTypeHandler(Repository repository,
                                       Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    @Override
    public Service getService(Request request, Entry entry) throws Exception {
        Service service = new Service(getRepository(), entry);
        service.setDescription(entry.getDescription());
        service.setSerial(entry.getStringValue(request,IDX_SERIAL, "true").equals("true"));

        List<Entry> children = getEntryManager().getChildren(request, entry);
        for (Entry child : children) {
            if ( !child.getTypeHandler().isType(TYPE_SERVICE)) {
                continue;
            }
            ServiceTypeHandler childTypeHandler =
                (ServiceTypeHandler) child.getTypeHandler();
            Service childService = childTypeHandler.getService(request,
                                       child);
            if (childService != null) {
                service.addChild(childService);
            } else {
                System.err.println("No child service:" + child);
            }
        }

        return service;
    }



}
