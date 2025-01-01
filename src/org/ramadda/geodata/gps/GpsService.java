/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.gps;


import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;



import org.ramadda.service.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import java.util.List;


/**
 */
public class GpsService extends Service {

    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public GpsService(Repository repository, Element element)
            throws Exception {
        super(repository, element);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param input _more_
     * @param args _more_
     * @param start _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addExtraArgs(Request request, ServiceInput input,
                             List<String> args, boolean start)
            throws Exception {
        if ( !start) {
            return;
        }
        List<Entry> entries = input.getEntries();
        if (entries.size() == 0) {
            return;
        }
        Entry entry = entries.get(0);
        String antenna = entry.getStringValue(request,GpsOutputHandler.IDX_ANTENNA_TYPE,
                                        (String) null);
        double height = entry.getDoubleValue(request,GpsOutputHandler.IDX_ANTENNA_HEIGHT,
                                       0.0);

        System.err.println("ant:" + antenna + " h:" + height);
        if (height != 0) {
            args.add("-O.pe");
            args.add("" + height);
            args.add("0");
            args.add("0");
        }
        if ((antenna != null) && (antenna.length() > 0)
                && !antenna.equalsIgnoreCase(Antenna.NONE)) {
            args.add("-O.at");
            args.add(antenna);
        }
    }



}
