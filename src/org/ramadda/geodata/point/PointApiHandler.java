/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.point;


import org.ramadda.data.record.*;
import org.ramadda.data.services.*;
import org.ramadda.repository.*;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.Element;

import ucar.unidata.util.StringUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

public class PointApiHandler extends RepositoryManager {


    /**
     * ctor
     *
     * @param repository the main ramadda repository
     * @param node xml node from nlasapi.xml
     * @param props extra properties
     *
     * @throws Exception On badness
     */
    public PointApiHandler(Repository repository, Element node,
                           Hashtable props)
            throws Exception {
        super(repository);
    }


    public PointOutputHandler getPointOutputHandler() {
        return (PointOutputHandler) getRepository().getOutputHandler(
            PointOutputHandler.class);
    }

    public Result processJsonRequest(Request request) throws Exception {
        PointOutputHandler poh   = getPointOutputHandler();
        Entry              entry = getEntryManager().getEntry(request);
        request.getHttpServletResponse().setContentType("test/json");
        //Set a default of 500 for num points
        request.put(RecordFormHandler.ARG_NUMPOINTS,
                    request.getString(RecordFormHandler.ARG_NUMPOINTS,
                                      "500"));

        request.put(ARG_OUTPUT, poh.OUTPUT_PRODUCT.getId());
        request.put(ARG_PRODUCT, poh.OUTPUT_JSON.toString());
        request.put(RecordConstants.ARG_ASYNCH, "false");
        request.setReturnFilename(entry.getName() + ".json");

        return poh.outputEntry(request, poh.OUTPUT_PRODUCT, entry);
    }

    public Result processDataRequest(Request request) throws Exception {
        PointOutputHandler poh   = getPointOutputHandler();
        Entry              entry = getEntryManager().getEntry(request);
        request.put(ARG_OUTPUT, poh.OUTPUT_PRODUCT.getId());
        request.put(ARG_PRODUCT, poh.OUTPUT_JSON.toString());
        request.put(RecordConstants.ARG_ASYNCH, "false");

        return poh.outputEntry(request, poh.OUTPUT_PRODUCT, entry);
    }

}
