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

package org.ramadda.plugins.box;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import ucar.unidata.util.IOUtil;

import java.io.*;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Proxy that searches google
 *
 */
public class BoxSearchProvider extends SearchProvider {


    /** _more_ */
    public static final String URL =
        "https://api.duckduckgo.com/?format=json";

    /** _more_ */
    public static final String SEARCH_ID = "duckduckgo";

    /**
     * _more_
     *
     * @param repository _more_
     */
    public BoxSearchProvider(Repository repository) {
        super(repository, SEARCH_ID, "Box Search Provider");
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param args _more_
     */
    public BoxSearchProvider(Repository repository, List<String> args) {
        super(repository, SEARCH_ID, "Box");
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param searchCriteriaSB _more_
     * @param searchInfo _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<Entry> getEntries(Request request, SearchInfo searchInfo)
            throws Exception {

        List<Entry> entries = new ArrayList<Entry>();

        return entries;
    }

}
