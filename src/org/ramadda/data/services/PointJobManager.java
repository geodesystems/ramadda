/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.services;

import org.ramadda.data.record.*;
import org.ramadda.data.record.filter.*;
import org.ramadda.data.services.*;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.job.JobInfo;

import org.ramadda.repository.job.JobManager;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.TypeHandler;

import org.w3c.dom.*;

import java.io.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;


@SuppressWarnings("unchecked")
public class PointJobManager extends RecordJobManager {

    public PointJobManager(PointOutputHandler pointOutputHandler) {
        super(pointOutputHandler);
    }

    public PointOutputHandler getPointOutputHandler() {
        return (PointOutputHandler) getRecordOutputHandler();
    }

    public String makeJobUrl(Request dummy) {
        dummy.remove(ARG_GETDATA);
        dummy.remove(ARG_RECORDENTRY);
        dummy.remove(ARG_RECORDENTRY_CHECK);
        dummy.remove("Boxes");
        List<String> products = (List<String>) dummy.get(ARG_PRODUCT,
                                    new ArrayList<String>());

        HashSet<String> formats = new HashSet<String>();
        formats.addAll(products);

        //if no grid products then get rid of the grid parameters
        if ( !getPointOutputHandler().anyGriddedFormats(formats)) {
            for (String gridArg : new String[] {
                ARG_WIDTH, ARG_HEIGHT, ARG_COLORTABLE, ARG_GRID_RADIUS_CELLS,
                ARG_GRID_RADIUS_DEGREES, ARG_HILLSHADE_AZIMUTH,
                ARG_HILLSHADE_ANGLE, ARG_GRID_RADIUS_DEGREES_ORIG,
            }) {
                dummy.remove(gridArg);
            }
        }

        for (Enumeration keys = dummy.keys(); keys.hasMoreElements(); ) {
            String arg = (String) keys.nextElement();
            if (arg.startsWith("job.")) {
                dummy.remove(arg);
            }
            if (arg.startsWith("OpenLayers")) {
                dummy.remove(arg);
            }
        }

        return super.makeJobUrl(dummy);
    }

}
