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
