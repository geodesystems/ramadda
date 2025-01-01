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
public class FieldNoteTypeHandler extends ExtensibleGroupTypeHandler {

    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception On badness
     */
    public FieldNoteTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param arg _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    @Override
    public String getFormDefault(Entry entry, String arg, String dflt) {
        if (arg.equals(ARG_NAME)) {
            Date             now = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d yyyy");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            return sdf.format(now) + " - ";
        }

        return super.getFormDefault(entry, arg, dflt);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public Date[] getDefaultDateRange(Request request, Entry entry) {
        if (entry != null) {
            return super.getDefaultDateRange(request, entry);
        }

        return new Date[] { new Date(), null };
    }

}
