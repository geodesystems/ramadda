/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/
// Copyright (c) 2008-2021 Geode Systems LLC
// SPDX-License-Identifier: Apache-2.0

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
     * @param entries _more_
     */
    public void entriesCreated(List<Entry> entries);

    /**
     * _more_
     *
     * @param entries _more_
     */
    public void entriesModified(List<Entry> entries);

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
