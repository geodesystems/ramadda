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

package org.ramadda.repository.search;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;

import org.ramadda.repository.util.ServerInfo;

import java.net.URL;

import java.util.ArrayList;
import java.util.List;


/**
 * Class description
 *
 *
 * @version        $version$, Sat, Nov 8, '14
 * @author         Enter your name here...
 */
public class TestSearchProvider extends SearchProvider {


    /** _more_ */
    private String externalUrl;

    /** _more_ */
    private String name;

    /**
     * _more_
     *
     * @param repository _more_
     */
    public TestSearchProvider(Repository repository) {
        super(repository);
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param args _more_
     */
    public TestSearchProvider(Repository repository, List<String> args) {
        super(repository);
        if (args != null) {
            if (args.size() > 0) {
                externalUrl = args.get(0);
            }
            if (args.size() > 1) {
                name = args.get(1);
            } else {
                name = externalUrl;
            }
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/favicon.png";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        if (name != null) {
            return name;
        }

        return "TestSearchProvider";
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param url _more_
     *
     * @return _more_
     */
    public String applyMacros(Request request, String url) {
        url = url.replace("${text}", request.getString(ARG_TEXT, ""));
        url = url.replace("${north}",
                          request.getString(TypeHandler.REQUESTARG_NORTH,
                                            ""));
        url = url.replace("${west}",
                          request.getString(TypeHandler.REQUESTARG_WEST, ""));
        url = url.replace("${south}",
                          request.getString(TypeHandler.REQUESTARG_SOUTH,
                                            ""));
        url = url.replace("${east}",
                          request.getString(TypeHandler.REQUESTARG_EAST, ""));

        //TODO: Add time and others
        return url;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param searchInfo _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<Entry> getEntries(Request request, SearchInfo searchInfo)
            throws Exception {
        List<Entry> results = new ArrayList<Entry>();
        if (externalUrl != null) {
            request = request.cloneMe();
            String url = applyMacros(request, externalUrl);
            Runnable runnable =
                getSearchManager().makeRunnable(request,
                    new ServerInfo(new URL(url), this.name, ""),
                    getEntryManager().getDummyGroup(), results, null, null);
            runnable.run();
        } else {
            System.err.println("TestSearchProvider.getEntries");
            StringBuilder sb = new StringBuilder();
            List[] fromRepos = getEntryManager().getEntries(request, sb);
            searchInfo.addMessage(this, sb.toString());
            results.addAll(((List<Entry>) fromRepos[0]));
            results.addAll(((List<Entry>) fromRepos[1]));

        }

        return results;
    }



}
