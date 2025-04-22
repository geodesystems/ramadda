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

    public void entriesCreated(Request request, List<Entry> entries);

    public void entriesModified(Request request, List<Entry> entries);

    public void entriesDeleted(List<String> ids);

    public void entriesMoved(List<Entry> entries);

}
