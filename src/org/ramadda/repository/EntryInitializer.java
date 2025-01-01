/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;


import java.io.File;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public abstract class EntryInitializer {

    /**
     * _more_
     *
     * @param entry _more_
     */
    public void initEntry(Entry entry) {}

    /**
     * _more_
     *
     * @param entry _more_
     * @param fileArg _more_
     *
     * @return _more_
     */
    public File getMetadataFile(Entry entry, String fileArg) {
        return null;
    }


}
