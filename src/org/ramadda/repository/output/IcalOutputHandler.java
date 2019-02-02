/*
* Copyright (c) 2008-2019 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.repository.output;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringBufferCollection;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


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



    /** _more_ */
    public static final OutputType OUTPUT_ICAL = new OutputType("ICAL",
                                                     "ical",
                                                     OutputType.TYPE_FEEDS,
                                                     "", ICON_CALENDAR);


    /** _more_ */
    private SimpleDateFormat sdf;

    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public IcalOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_ICAL);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {

        if (state.getEntry() != null) {
            links.add(
                makeLink(
                    request, state.entry, OUTPUT_ICAL,
                    "/" + IOUtil.stripExtension(state.getEntry().getName())
                    + ".ics"));
        }
    }





    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(OutputType output) {
        if (output.equals(OUTPUT_ICAL)) {
            return repository.getMimeTypeFromSuffix(".ics");
        } else {
            return super.getMimeType(output);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> subGroups,
                              List<Entry> entries)
            throws Exception {
        entries.addAll(subGroups);

        return outputEntries(request, entries);
    }

    /**
     * _more_
     *
     * @param t _more_
     *
     * @return _more_
     */
    private String format(long t) {
        if (sdf == null) {
            sdf = RepositoryUtil.makeDateFormat("yyyyMMdd'T'HHmmss");
        }

        return sdf.format(new Date(t)) + "Z";
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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
            if (entry.hasAreaDefined()) {
                loc = entry.getLocation();
            } else if (entry.hasLocationDefined()) {
                loc = entry.getCenter();
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
