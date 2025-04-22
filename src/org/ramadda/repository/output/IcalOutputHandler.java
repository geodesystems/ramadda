/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.IO;

import org.w3c.dom.*;
import java.io.*;
import java.io.File;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.regex.*;
import java.util.zip.*;

/**
 *
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class IcalOutputHandler extends OutputHandler {

    public static final OutputType OUTPUT_ICAL = new OutputType("ICAL",
                                                     "ical",
                                                     OutputType.TYPE_FEEDS,
                                                     "", ICON_CALENDAR);

    private SimpleDateFormat sdf;

    public IcalOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_ICAL);
    }

    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {

        if (state.getEntry() != null) {
            links.add(
                makeLink(
                    request, state.getEntry(), OUTPUT_ICAL,
                    "/" + IO.stripExtension(state.getEntry().getName())
                    + ".ics"));
        }
    }

    public String getMimeType(OutputType output) {
        if (output.equals(OUTPUT_ICAL)) {
            return repository.getMimeTypeFromSuffix(".ics");
        } else {
            return super.getMimeType(output);
        }
    }

    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {
        return outputEntries(request, children);
    }

    private String format(long t) {
        if (sdf == null) {
            sdf = RepositoryUtil.makeDateFormat("yyyyMMdd'T'HHmmss");
        }

        return sdf.format(new Date(t)) + "Z";
    }

    public Result outputEntries(Request request, List<Entry> entries)
            throws Exception {

        StringBuffer sb = new StringBuffer();
        sb.append("BEGIN:VCALENDAR\n");
        sb.append("PRODID:-//Unidata/UCAR//RAMADDA Calendar//EN\n");
        sb.append("VERSION:2.0\n");
        sb.append("CALSCALE:GREGORIAN\n");
        sb.append("METHOD:PUBLISH\n");
        OutputType output = request.getOutput();
        request.put(ARG_OUTPUT, OutputHandler.OUTPUT_HTML);

        for (Entry entry : entries) {
            sb.append("BEGIN:VEVENT\n");
            sb.append("UID:" + entry.getId() + "\n");
            sb.append("CREATED:" + format(entry.getCreateDate()) + "\n");
            sb.append("DTSTAMP:" + format(entry.getCreateDate()) + "\n");
            sb.append("DTSTART:" + format(entry.getStartDate()) + "\n");
            sb.append("DTEND:" + format(entry.getEndDate()) + "\n");

            sb.append("SUMMARY:" + entry.getName() + "\n");
            String desc = entry.getDescription();
            desc = desc.replace("\n", "\\n");

            sb.append("DESCRIPTION:" + desc);
            sb.append("\n");
            double[] loc = null;
            if (entry.hasAreaDefined(request)) {
                loc = entry.getLocation(request);
            } else if (entry.hasLocationDefined(request)) {
                loc = entry.getCenter(request);
            }
            if (loc != null) {
                sb.append("GEO:" + loc[0] + ";" + loc[1] + "\n");
            }
            String url = request.getAbsoluteUrl(
                             request.makeUrl(
                                 repository.URL_ENTRY_SHOW, ARG_ENTRYID,
                                 entry.getId()));
            sb.append("ATTACH:" + url + "\n");
            sb.append("END:VEVENT\n");
        }

        request.put(ARG_OUTPUT, output);
        sb.append("END:VCALENDAR\n");

        Result result = new Result("Query Results", sb,
                                   getMimeType(OUTPUT_ICAL));

        return result;

    }

}
