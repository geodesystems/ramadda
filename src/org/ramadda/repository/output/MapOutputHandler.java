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
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.JpegMetadataHandler;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.metadata.MetadataHandler;
import org.ramadda.repository.type.*;


import org.ramadda.sql.SqlUtil;
import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;

import java.io.*;

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
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class MapOutputHandler extends OutputHandler implements WikiConstants {


    /** Map output type */
    public static final OutputType OUTPUT_MAP =
        new OutputType("Map", "map.map",
                       OutputType.TYPE_VIEW | OutputType.TYPE_FORSEARCH, "",
                       ICON_MAP);

    /** GoogleEarth output type */
    public static final OutputType OUTPUT_GEMAP =
        new OutputType("Google Earth", "map.gemap",
                       OutputType.TYPE_VIEW | OutputType.TYPE_FORSEARCH, "",
                       ICON_GOOGLEEARTH);


    /**
     * Create a MapOutputHandler
     *
     *
     * @param repository  the repository
     * @param element     the Element
     * @throws Exception  problem generating handler
     */
    public MapOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_MAP);
        addType(OUTPUT_GEMAP);
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
        boolean     ok         = false;
        List<Entry> allEntries = state.getAllEntries();
        if (allEntries.size() == 0) {
            Entry singleEntry = state.getEntry();
            if (singleEntry != null) {
                allEntries.add(state.getEntry());
            }
        }
        for (Entry entry : allEntries) {
            if (entry.isGeoreferenced()) {
                ok = true;

                break;
            }
        }
        if (ok) {
            links.add(makeLink(request, state.getEntry(), OUTPUT_MAP));
            if (getMapManager().isGoogleEarthEnabled(request)) {
                links.add(makeLink(request, state.getEntry(), OUTPUT_GEMAP));
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
        StringBuilder sb = new StringBuilder();

        getPageHandler().entrySectionOpen(request, entry, sb, "Map", true);

        if (outputType.equals(OUTPUT_GEMAP)) {
            getMapManager().getGoogleEarth(request, entriesToUse, sb, -1, -1,
                                           true, false);

            getPageHandler().entrySectionClose(request, entry, sb);

            return makeLinksResult(request,
                                   msg("Google Earth") + " - "
                                   + entry.getName(), sb, new State(entry));
        }

        Hashtable props = new Hashtable();
        props.put(ATTR_DETAILS, "true");
        props.put(ATTR_LISTENTRIES, "false");
        MapInfo map = getMapManager().getMap(request, entry, entriesToUse,
                                             sb, 700, 500, null, props);

        getPageHandler().entrySectionClose(request, entry, sb);

        return makeLinksResult(request, msg("Map") + " - " + entry.getName(),
                               sb, new State(entry));
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
        StringBuilder sb = new StringBuilder();
        getPageHandler().entrySectionOpen(request, group, sb, "Map", true);
        if (entriesToUse.size() == 0) {
            sb.append(HtmlUtils.b(msg(LABEL_NO_ENTRIES_FOUND))
                      + HtmlUtils.p());

            getPageHandler().entrySectionClose(request, group, sb);

            return makeLinksResult(request,
                                   msg("Map") + " - " + group.getName(), sb,
                                   new State(group, subGroups, entries));
        }

        showNext(request, subGroups, entries, sb);
        if (outputType.equals(OUTPUT_GEMAP)) {
            getMapManager().getGoogleEarth(request, entriesToUse, sb, -1, -1,
                                           true, false);

            getPageHandler().entrySectionClose(request, group, sb);

            return makeLinksResult(request,
                                   msg("Google Earth") + " - "
                                   + group.getName(), sb, new State(group));
        }


        Hashtable props = new Hashtable();
        props.put(ATTR_DETAILS, "false");
        props.put(ATTR_LISTENTRIES, "true");
        MapInfo map = getMapManager().getMap(request, group, entriesToUse,
                                             sb, -100, 500, null, props);

        getPageHandler().entrySectionClose(request, group, sb);

        return makeLinksResult(request, msg("Map") + " - " + group.getName(),
                               sb, new State(group, subGroups, entries));
    }




}
