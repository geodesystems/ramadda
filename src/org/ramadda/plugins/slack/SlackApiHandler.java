/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.slack;


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
public class SlackApiHandler extends RepositoryManager implements RequestHandler {

    /**
     *     ctor
     *
     *     @param repository the repository
     *
     *     @throws Exception on badness
     */
    public SlackApiHandler(Repository repository) throws Exception {
        super(repository);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public List<SlackHarvester> getHarvesters() {
        List<SlackHarvester> harvesters = new ArrayList<SlackHarvester>();
        for (Harvester harvester : getHarvesterManager().getHarvesters()) {
            if (harvester instanceof SlackHarvester) {
                if (harvester.getActiveOnStart()) {
                    harvesters.add((SlackHarvester) harvester);
                } else {
                    System.err.println("slack harvester: not active");
                }
            }
        }

        return harvesters;
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
    public Result processSlackApi(Request request) throws Exception {
        Result result = null;
        for (SlackHarvester harvester : getHarvesters()) {
            try {
                result = harvester.handleRequest(request);
                if (result != null) {
                    break;
                }
            } catch (Exception exc) {
                exc.printStackTrace();

                return new Result(
                    "Oops, I did it again. An error has occurred:" + exc,
                    "text");
            }

        }
        if (result == null) {
            result = new Result("", new StringBuffer("nothing defined"));
        }
        result.setShouldDecorate(false);

        return result;
    }


}
