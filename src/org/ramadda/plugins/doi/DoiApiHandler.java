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

package org.ramadda.plugins.doi;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;


import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.*;


/**
 * Provides a top-level API
 *
 */
public class DoiApiHandler extends RepositoryManager implements RequestHandler {


    /** _more_ */
    public static final String ARG_DOI = "doi";

    /**
     *     ctor
     *
     *     @param repository the repository
     *     @param node xml from api.xml
     *     @param props propertiesn
     *
     *     @throws Exception on badness
     */
    public DoiApiHandler(Repository repository) throws Exception {
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
    public Result processDoiSearch(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(HtmlUtils.p());
        if ( !request.defined(ARG_DOI)) {
            makeForm(request, sb);

            return new Result("", sb);
        }

        Entry entry = getEntryManager().getEntryFromMetadata(request,
                          DoiMetadataHandler.TYPE_DOI,
                          request.getString(ARG_DOI, ""), 2);
        if (entry == null) {
            sb.append("Could not find DOI:" + request.getString(ARG_DOI, ""));
            sb.append(HtmlUtils.p());
            makeForm(request, sb);

            return new Result("", sb);
        }
        request.put(ARG_ENTRYID, entry.getId());

        return getEntryManager().processEntryShow(request);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     */
    private void makeForm(Request request, StringBuffer sb) {
        String base = getRepository().getUrlBase();
        sb.append(HtmlUtils.formTable());
        sb.append(HtmlUtils.form(base + "/doi"));
        sb.append(HtmlUtils.formEntry("DOI",
                                      HtmlUtils.input(ARG_DOI,
                                          request.getString(ARG_DOI, ""))));

        sb.append(HtmlUtils.formEntry("", HtmlUtils.submit("Find DOI", "")));
        sb.append(HtmlUtils.formClose());
        sb.append(HtmlUtils.formTableClose());
    }

}
