/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.atlassian;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.ui.HttpFormEntry;

import ucar.unidata.util.IOUtil;


import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;


import java.net.*;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.*;


/**
 * Provides a top-level API
 *
 */
@SuppressWarnings("unchecked")
public class HipchatApiHandler extends RepositoryManager implements RequestHandler {

    /**
     *     ctor
     *
     *     @param repository the repository
     *
     *     @throws Exception on badness
     */
    public HipchatApiHandler(Repository repository) throws Exception {
        super(repository);
    }




    /**
     * handle the request
     *
     * @param request request
     *
     * @return result
     *
     * @throws Exception on badness
     */
    public Result processHipchatApi(Request request) throws Exception {
        HipchatHarvester.CommandRequest cmdRequest = null;
        Result                          result     = null;
        for (HipchatHarvester harvester : getHarvesters()) {
            try {
                if (cmdRequest == null) {
                    cmdRequest = harvester.doMakeCommandRequest(request);
                }
                result = harvester.handleRequest(cmdRequest);
                if (result != null) {
                    result.setShouldDecorate(false);

                    return result;
                }
            } catch (Exception exc) {
                exc.printStackTrace();

                return harvester.message("Oops, an error has occurred:"
                                         + exc);
            }
        }

        return new Result(Hipchat.encodeMessage("Nothing enabled"),
                          Constants.MIME_TEXT);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<HipchatHarvester> getHarvesters() {
        return (List<HipchatHarvester>) getHarvesterManager().getHarvesters(
            HipchatHarvester.class);
    }


}
