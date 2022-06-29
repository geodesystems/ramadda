/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;


import org.ramadda.repository.*;
import org.ramadda.repository.map.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.*;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class MapOutputHandler extends OutputHandler implements WikiConstants {


    /** Map output type */
    public static final OutputType OUTPUT_MAP =
        new OutputType("Map", "map.map",
                       OutputType.TYPE_VIEW | OutputType.TYPE_FORSEARCH, "",
                       ICON_MAP);

    /** GoogleEarth output type */
    public static final OutputType OUTPUT_GEMAP =
        new OutputType("Google Earth", "map.gemap", OutputType.TYPE_VIEW, "",
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
            /*
            if (getMapManager().isGoogleEarthEnabled(request)) {
                links.add(makeLink(request, state.getEntry(), OUTPUT_GEMAP));
            }
            */

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

        String prefix = request.getPrefixHtml();
        if (prefix != null) {
            sb.append(prefix);
        }

        if (outputType.equals(OUTPUT_GEMAP)) {
            getMapManager().getGoogleEarth(request, entriesToUse, sb, "", "",
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
                                             sb, "700", "500", null, props);

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
     *
     * @return  the resule
     *
     * @throws Exception    problem on output
     */
    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {
        StringBuilder sb     = new StringBuilder();
        String        prefix = request.getPrefixHtml();
        if (Utils.stringDefined(prefix)) {
            sb.append(prefix);
        } else {
            getPageHandler().entrySectionOpen(request, group, sb, "Map",
                    true);
        }

        if (children.size() == 0) {
            sb.append(HtmlUtils.b(msg(LABEL_NO_ENTRIES_FOUND))
                      + HtmlUtils.p());

            if (prefix == null) {
                getPageHandler().entrySectionClose(request, group, sb);
            }

            return makeLinksResult(request,
                                   msg("Map") + " - " + group.getName(), sb,
                                   new State(group, children));
        }

        showNext(request, children, sb);
        if (outputType.equals(OUTPUT_GEMAP)) {
            getMapManager().getGoogleEarth(request, children, sb, "", "",
                                           true, false);

            if (prefix == null) {
                getPageHandler().entrySectionClose(request, group, sb);
            }

            return makeLinksResult(request,
                                   msg("Google Earth") + " - "
                                   + group.getName(), sb, new State(group));
        }


        Hashtable props = new Hashtable();
        props.put(ATTR_DETAILS, "false");
        props.put(ATTR_LISTENTRIES, "true");
        MapInfo map = getMapManager().getMap(request, group, children,
                                             sb, "100%", "500", null, props);

        if (prefix == null) {
            getPageHandler().entrySectionClose(request, group, sb);
        }

        return makeLinksResult(request, msg("Map") + " - " + group.getName(),
                               sb, new State(group, children));
    }




}
