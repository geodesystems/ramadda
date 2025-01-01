/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.test;


import org.ramadda.repository.*;
import org.ramadda.repository.output.*;

import java.util.List;


/**
 * An example of a page decorator. Change the above package to your package structure.
 * Compile this class and make a jar file, e.g.,:
 * jar -cvf testdecorator.jar TestPageDecorator.class
 *
 * Put the jar file in the ramadda plugins directory, e.g.:
 * ~/.ramadda/plugins
 *
 * Now when ramadda runs decorate page will be called with the page html from the templates.
 * Update the html as needed and return it.
 *
 * @author Jeff McWhirter
 */
public class TestPageDecorator extends PageDecorator {

    /**
     * ctor
     *
     * @param repository _more_
     */
    public TestPageDecorator(Repository repository) {
        super(repository);
    }

    /**
     * Decorate the html
     *
     * @param repository the repository
     * @param request the request
     * @param html The html page template
     * @param entry This is the last entry the user has seen. Note: this may be null.
     *
     * @return The html
     */
    public String decoratePage(Repository repository, Request request,
                               String html, Entry entry) {
        Entry secondToTopMostEntry = null;
        if (entry != null) {
            secondToTopMostEntry =
                repository.getEntryManager().getSecondToTopEntry(entry);
        }
        if (secondToTopMostEntry != null) {
            //Use this to change the template
        }

        //Just add on XXXXXX so we cna see this working
        return html + "XXXXXX";
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
    @Override
    public String getDefaultOutputType(Repository repository,
                                       Request request, Entry entry,
                                       List<Entry> children) {
        if (entry.isGroup()) {
            for (Entry child : children) {
                if (child.getResource().isImage()) {
                    return ImageOutputHandler.OUTPUT_PLAYER.getId();
                }
            }
        }

        return null;
    }


}
