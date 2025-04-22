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

    public PageDecorator() {
        super(null);
    }

    public PageDecorator(Repository repository) {
        super(repository);

    }

    public String decoratePage(Repository repository, Request request,
                               String html, Entry entry) {
        return html;
    }

    public String getDefaultOutputType(Repository repository,
                                       Request request, Entry entry,
                                       List<Entry> children) {
        return null;
    }

    public void addToMap(Request request, MapInfo mapInfo) {}

    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props) {
        return null;
    }

}
