/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.gps;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import java.io.*;

import java.util.Hashtable;
import java.util.regex.*;


/**
 * Provides a top-level API
 *
 */
public class GpsApiHandler extends RepositoryManager implements RequestHandler {



    /**
     * ctor
     *
     * @param repository the repository
     *
     * @throws Exception on badness
     */
    public GpsApiHandler(Repository repository) throws Exception {
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
    public Result processAddOpus(Request request) throws Exception {
        GpsOutputHandler gpsOutputHandler =
            (GpsOutputHandler) getRepository().getOutputHandler(
                GpsOutputHandler.OUTPUT_GPS_TORINEX);

        return gpsOutputHandler.processAddOpus(request);
    }

}
