/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.fieldproject;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;

import org.w3c.dom.*;

import java.io.File;

import java.text.SimpleDateFormat;


import java.util.Date;
import java.util.List;
import java.util.TimeZone;


/**
 *
 *
 * @author Jeff McWhirter
 */
public class VisitTypeHandler extends ExtensibleGroupTypeHandler {


    /**
     *
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception On badness
     */
    public VisitTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param parent _more_
     * @param newEntry _more_
     *
     * @throws Exception On badness
     */
    @Override
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
        super.initializeEntryFromForm(request, entry, parent, newEntry);
        Object[] values = getEntryValues(entry);
        String   status = "" + values[0];
        System.err.println("status:" + status);
        if (status.equals("private")) {}
    }


}
