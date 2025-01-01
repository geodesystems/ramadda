/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;


import java.util.List;


/**
 * Interface description
 *
 *
 * @author         Enter your name here...
 */
public interface EntryChecker {

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entries _more_
     */
    public void entriesCreated(Request request, List<Entry> entries);

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entries _more_
     */
    public void entriesModified(Request request, List<Entry> entries);

    /**
     * _more_
     *
     * @param ids _more_
     */
    public void entriesDeleted(List<String> ids);


    /**
     * _more_
     *
     * @param entries _more_
     */
    public void entriesMoved(List<Entry> entries);

}
