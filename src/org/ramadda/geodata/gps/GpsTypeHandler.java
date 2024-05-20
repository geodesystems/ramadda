/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.gps;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;

import org.w3c.dom.*;

import ucar.unidata.util.Misc;

import java.io.File;

import java.util.List;


/**
 *
 *
 * @author Jeff McWhirter
 */
public class GpsTypeHandler extends GenericTypeHandler {

    /** _more_ */
    public static final String TYPE_GPS = "project_gps";

    /** _more_ */
    public static final String TYPE_RINEX = "project_gps_rinex";

    /** _more_ */
    public static final String TYPE_RAW = "project_gps_raw";

    /** _more_ */
    public static final String TYPE_CONTROLPOINTS =
        "project_gps_controlpoints";

    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception On badness
     */
    public GpsTypeHandler(Repository repository, Element node)
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
        if (newEntry) {
            initializeGpsEntry(entry);
        }
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param fromImport _more_
     *
     * @throws Exception On badness
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
        //        Misc.printStack("GpsTypeHandler.initializeNewEntry",10,null);
        initializeGpsEntry(entry);
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    private void initializeGpsEntry(Entry entry) throws Exception {
        //Get the output handler
        GpsOutputHandler gpsOutputHandler =
            (GpsOutputHandler) getRepository().getOutputHandler(
                GpsOutputHandler.OUTPUT_GPS_TORINEX);
        gpsOutputHandler.initializeGpsEntry(entry, this);
    }



}
