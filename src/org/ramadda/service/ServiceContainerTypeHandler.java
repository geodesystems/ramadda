/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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
