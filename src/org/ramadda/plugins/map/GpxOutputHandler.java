/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

package org.ramadda.plugins.map;


import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.gis.shapefile.EsriShapefile;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;

import ucar.unidata.xml.XmlUtil;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 *
 *
 */
public class GpxOutputHandler extends OutputHandler {


    /** Map output type */
    public static final OutputType OUTPUT_GPX =
        new OutputType("GPS GPX File", "gpx",
                       OutputType.TYPE_FEEDS | OutputType.TYPE_FORSEARCH, "",
                       ICON_MAP);





    /**
     * Create a MapOutputHandler
     *
     *
     * @param repository  the repository
     * @param element     the Element
     * @throws Exception  problem generating handler
     */
    public GpxOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_GPX);
    }



    /**
     * Get the entry links
     *
     * @param request  the Request
     * @param state    the repository State
     * @param links    the links
     *
     * @throws Exception  problem creating links
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        for (Entry entry : state.getAllEntries()) {
            if (entry.hasLocationDefined() || entry.hasAreaDefined()) {
                links.add(makeLink(request, state.getEntry(), OUTPUT_GPX));

                break;
            }
        }
    }


    /**
     * Output the entry
     *
     * @param request      the Request
     * @param outputType   the type of output
     * @param entry        the Entry to output
     *
     * @return  the Result
     *
     * @throws Exception  problem outputting entry
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        List<Entry> entriesToUse = new ArrayList<Entry>();
        entriesToUse.add(entry);

        return outputGpx(request, entry, entriesToUse);
    }


    /**
     * Output a group
     *
     * @param request      The Request
     * @param outputType   the type of output
     * @param group        the group Entry
     * @param subGroups    the subgroups
     * @param entries      The list of Entrys
     *
     * @return  the resule
     *
     * @throws Exception    problem on output
     */
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> subGroups,
                              List<Entry> entries)
            throws Exception {
        List<Entry> entriesToUse = new ArrayList<Entry>(subGroups);
        entriesToUse.addAll(entries);

        return outputGpx(request, group, entriesToUse);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGpx(Request request, Entry entry, List<Entry> entries)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(XmlUtil.openTag(GpxUtil.TAG_GPX,
                                  XmlUtil.attrs(new String[] {
                                      GpxUtil.ATTR_VERSION,
                                      "1.1", GpxUtil.ATTR_CREATOR,
                                      "RAMADDA" })));

        for (Entry child : entries) {
            if ( !(child.hasLocationDefined() || child.hasAreaDefined())) {
                continue;
            }
            if (child.hasAreaDefined()) {}
            else {
                sb.append(XmlUtil.tag(GpxUtil.TAG_WPT,
                                      XmlUtil.attrs(new String[] {
                                          GpxUtil.ATTR_LAT,
                                          "" + child.getLatitude(),
                                          GpxUtil.ATTR_LON,
                                          "" + child.getLongitude() })));
            }
        }

        Result result = new Result("", sb, "application/gpx+xml");
        result.setReturnFilename(IOUtil.stripExtension(entry.getName())
                                 + ".gpx");

        return result;
    }



}
