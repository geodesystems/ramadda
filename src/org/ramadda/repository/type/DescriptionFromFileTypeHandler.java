/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;

import org.ramadda.repository.*;

import org.w3c.dom.*;

import ucar.unidata.xml.XmlUtil;

import java.util.Hashtable;

/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class DescriptionFromFileTypeHandler extends GenericTypeHandler {

    public DescriptionFromFileTypeHandler(Repository repository,
                                          Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    public DescriptionFromFileTypeHandler(Repository repository, String type,
                                          String description) {
        super(repository, type, description);
    }

    /**
     *
     * @param entry _more_
     */
    public void initEntryHasBeenCalled(Entry entry) {
        super.initEntryHasBeenCalled(entry);
        System.err.println("initEntry");
        if (entry.getResource().isFile()) {
            try {
                entry.setDescription(
                    getStorageManager().readFile(
                        entry.getResource().getPath()));
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

    }

}
