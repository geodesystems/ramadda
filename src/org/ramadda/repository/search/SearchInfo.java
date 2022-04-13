/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.search;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 * Holds information about a search
 */
public class SearchInfo {

    /** _more_ */
    private StringBuilder msgs = new StringBuilder();

    /**
     * _more_
     */
    public SearchInfo() {}

    /**
     * _more_
     *
     * @param provider _more_
     * @param msg _more_
     *
     * @throws Exception _more_
     */
    public void addMessage(SearchProvider provider, String msg)
            throws Exception {
        if (msg.length() > 0) {
            synchronized (msgs) {
                msgs.append(msg);
                msgs.append("<br>");
            }
        }
    }

}
