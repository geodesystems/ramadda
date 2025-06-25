/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.services;

import org.ramadda.util.IO;
import org.ramadda.data.point.text.MultiMonthFile;
import org.ramadda.data.record.RecordFile;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;

import org.w3c.dom.Element;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

public class NoaaPsdMonthlyClimateIndexTypeHandler extends PointTypeHandler {

    public NoaaPsdMonthlyClimateIndexTypeHandler(Repository repository,
            Element node)
            throws Exception {
        super(repository, node);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
        // override to set the missing value from the file/url
        String   loc    = fixUrl(entry.getResource().getPath());

        Object[] values = entry.getValues();
        /* Values are:
         *    0 - number of points
         *    1 - properties string
         *    2 - missing value
         *    3 - units
         */
        double missingValue = readMissingValue(loc);
        if ( !Double.isNaN(missingValue)) {
            values[2] = Double.valueOf(missingValue);
            entry.setValues(values);
        }
    }

    private String fixUrl(String loc) {
        if (loc.startsWith("http:")) {
            loc = loc.replace("http:", "https:");
        }

        return loc;
    }

    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        String name        = entry.getName();
        String loc         = fixUrl(entry.getResource().getPath());
        String description = entry.getDescription();
        if ((description == null) || description.isEmpty()) {
            description = name;
        }
        String units   = "";
        Double missing = -99.9;
        /* Values are:
         *    0 - number of points
         *    1 - properties string
         *    2 - missing value
         *    3 - units
         */
        Object[] values = entry.getValues();
        if (values != null) {
            if (values[3] != null) {
                units = (String) values[3];
            }
            if (values[2] != null) {
                missing = (Double) values[2];
            }
        }
        RecordFile myRF = new MultiMonthFile(new IO.Path(loc), name, description, units,
                                             missing);

        return myRF;
    }

    private double readMissingValue(String loc) throws Exception {
        double           missing  = Double.NaN;
        String           contents = IOUtil.readContents(loc);
        List<String>     lines = StringUtil.split(contents, "\n", true, true);
        Iterator<String> iter     = lines.iterator();
        String           header   = iter.next();
        List<String>     years    = StringUtil.split(header, " ", true, true);
        if (years.size() < 2) {
            throw new Exception("can't find time range in: " + header);
        }
        int startYear = Integer.parseInt(years.get(0));
        int endYear   = Integer.parseInt(years.get(1));
        int numYears  = endYear - startYear + 1;
        for (int i = 0; i < numYears; i++) {
            iter.next();
        }
        // Read in the missing value
        String missingStr = iter.next().trim();
        missing = Misc.parseNumber(missingStr);

        return missing;
    }

}
