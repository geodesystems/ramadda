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

package org.ramadda.repository;


import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.map.MapLayer;



import org.ramadda.util.WikiUtil;

import java.util.Hashtable;

import java.util.List;



/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class PageDecorator extends RepositoryManager {

    /**
     * _more_
     */
    public PageDecorator() {
        super(null);
    }


    /**
     * _more_
     *
     * @param repository _more_
     */
    public PageDecorator(Repository repository) {
        super(repository);

    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param request _more_
     * @param html _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public String decoratePage(Repository repository, Request request,
                               String html, Entry entry) {
        return html;
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param request _more_
     * @param entry _more_
     * @param subFolders _more_
     * @param subEntries _more_
     *
     * @return _more_
     */
    public String getDefaultOutputType(Repository repository,
                                       Request request, Entry entry,
                                       List<Entry> subFolders,
                                       List<Entry> subEntries) {
        return null;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param mapInfo _more_
     */
    public void addToMap(Request request, MapInfo mapInfo) {}


    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     */
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props) {
        return null;
    }


}
