/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;

import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.map.MapLayer;
import org.ramadda.util.WikiUtil;

import java.util.Hashtable;
import java.util.List;

/**
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
     *
     * @return _more_
     */
    public String getDefaultOutputType(Repository repository,
                                       Request request, Entry entry,
                                       List<Entry> children) {
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
