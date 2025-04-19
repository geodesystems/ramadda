/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.database.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;

import org.ramadda.util.sql.*;

import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.net.*;

import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
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
public class GraphOutputHandler extends OutputHandler {

    /** _more_ */
    public static final OutputType OUTPUT_GRAPH = new OutputType("Graph",
                                                      "graph.graph",
                                                      OutputType.TYPE_VIEW,
                                                      "", ICON_GRAPH);

    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public GraphOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_GRAPH);
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
            links.add(makeLink(request, state.getEntry(), OUTPUT_GRAPH));
        }
    }

    /** _more_ */
    static long cnt = System.currentTimeMillis();

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_LABEL = "label";

    /** _more_ */
    public static final String ATTR_URL = "url";

    /** _more_ */
    public static final String ATTR_GRAPHURL = "graphurl";

    /** _more_ */
    public static final String ATTR_NODEID = "nodeid";

    /** _more_ */
    public static final String ATTR_ICON = "icon";

    /** _more_ */
    public static final String ATTR_SOURCE = "source";

    /** _more_ */
    public static final String ATTR_TARGET = "target";

    /** _more_ */
    public static final String ATTR_SOURCE_ID = "source_id";

    /** _more_ */
    public static final String ATTR_TARGET_ID = "target_id";

    /** _more_ */
    public static final String ATTR_TITLE = "title";

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param nodes _more_
     * @param seen _more_
     *
     * @throws Exception _more_
     */
    private void addNode(Request request, Entry entry, List<String> nodes,
                         HashSet<String> seen)
            throws Exception {
        if (entry == null) {
            return;
        }
        if (seen.contains(entry.getId())) {
            return;
        }
        seen.add(entry.getId());
        String getIconUrl = getPageHandler().getIconUrl(request, entry);
        String url = getRepository().getUrlBase() + "/graph/get?entryid="
                     + entry.getId();
        String entryUrl = request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                           entry);
        nodes.add(JsonUtil.mapAndQuote(Utils.makeListFromValues(ATTR_NAME,
                entry.getName(), ATTR_NODEID, entry.getId(), ATTR_URL,
                entryUrl, ATTR_GRAPHURL, url, ATTR_ICON, getIconUrl)));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param from _more_
     * @param to _more_
     * @param title _more_
     * @param links _more_
     *
     * @throws Exception _more_
     */
    private void addLink(Request request, Entry from, Entry to, String title,
                         List<String> links)
            throws Exception {
        if ((from == null) || (to == null)) {
            return;
        }
        links.add(JsonUtil.mapAndQuote(Utils.makeListFromValues(ATTR_SOURCE_ID,
                from.getId(), ATTR_TARGET_ID, to.getId(), ATTR_TITLE,
                title)));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {

        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);

        return outputGraphEntries(request, entry, entries);

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGraphEntries(Request request, Entry mainEntry,
                                     List<Entry> entries)
            throws Exception {
        StringBuilder sb = new StringBuilder();
        getGraph(request, mainEntry, entries, sb, 960, 500);
        Result result = new Result(msg("Graph"), sb);
        addLinks(request, result, new State(mainEntry));

        return result;

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param entries _more_
     * @param sb _more_
     * @param width _more_
     * @param height _more_
     *
     * @throws Exception _more_
     */
    public void getGraph(Request request, Entry mainEntry,
                         List<Entry> entries, Appendable sb, int width,
                         int height)
            throws Exception {
        StringBuilder   js    = new StringBuilder();
        String          id    = addPrefixHtml(sb, js, width, height);
        HashSet<String> seen  = new HashSet<String>();
        List<String>    nodes = new ArrayList<String>();
        List<String>    links = new ArrayList<String>();
        for (Entry entry : entries) {
            addNode(request, entry, nodes, seen);
            addNode(request, entry.getParentEntry(), nodes, seen);
            addLink(request, entry.getParentEntry(), entry, "", links);
            getAssociations(request, entry, nodes, links, seen);
        }

        addSuffixHtml(sb, js, id, nodes, links, width, height);
    }

    /** _more_ */
    private int graphCnt = 0;

    /**
     * _more_
     *
     * @param sb _more_
     * @param js _more_
     * @param width _more_
     * @param height _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String addPrefixHtml(Appendable sb, Appendable js, int width,
                                int height)
            throws Exception {
        String divId = "graph_" + (graphCnt++);
        js.append("function createGraph" + divId + "() {\n");
        sb.append(HU.importJS(getFileUrl("/lib/d3/d3.v3.min.js")));
        sb.append(HU.importJS(getFileUrl("/d3graph.js")));
        sb.append(HU.div("",
                                HU.style("width:" + width + ";height:"
                                    + height) + HU.id(divId)
                                        + HU.cssClass("graph-div")));

        return divId;
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param js _more_
     * @param id _more_
     * @param nodes _more_
     * @param links _more_
     * @param width _more_
     * @param height _more_
     *
     * @throws Exception _more_
     */
    public void addSuffixHtml(Appendable sb, Appendable js, String id,
                              List<String> nodes, List<String> links,
                              int width, int height)
            throws Exception {
        js.append("var nodes  = [\n");
        js.append(StringUtil.join(",\n", nodes));
        js.append("];\n");
        js.append("var links = [\n");
        js.append(StringUtil.join(",", links));
        js.append("];\n");
        js.append("return new D3Graph(\"#" + id + "\", nodes,links," + width
                  + "," + height + ");\n}\n");
        js.append("var " + id + " = createGraph" + id + "();\n");
        sb.append(HU.script(js.toString()));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     * @param children _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {
        children.add(0, group);

        return outputGraphEntries(request, group, children);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param id _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    protected void getAssociationsGraph(Request request, String id,
                                        Appendable sb)
            throws Exception {
        List<Association> associations =
            getAssociationManager().getAssociations(request, id);
        for (Association association : associations) {
            Entry   other  = null;
            boolean isTail = true;
            if (association.getFromId().equals(id)) {
                other = getEntryManager().getEntry(request,
                        association.getToId());
                isTail = true;
            } else {
                other = getEntryManager().getEntry(request,
                        association.getFromId());
                isTail = false;
            }

            if (other != null) {
                String imageAttr = XmlUtil.attrs("imagepath",
                                       getPageHandler().getIconUrl(request,
                                           other));

                sb.append(
                    XmlUtil.tag(
                        TAG_NODE,
                        imageAttr
                        + XmlUtil.attrs(
                            ATTR_TYPE, other.getTypeHandler().getNodeType(),
                            ATTR_ID, other.getId(), ATTR_TOOLTIP,
                            getTooltip(other), ATTR_TITLE,
                            getGraphNodeTitle(other.getName()))));
                String fromId = association.getFromId();
                String toId   = association.getToId();

                sb.append(XmlUtil.tag(TAG_EDGE,
                                      XmlUtil.attrs(ATTR_TITLE,
                                          association.getType(), ATTR_TYPE,
                                          "link", ATTR_FROM, fromId, ATTR_TO,
                                          toId)));

            }
        }

        //        System.err.println(sb);

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    private void addNodeTag(Request request, Appendable sb, Entry entry)
            throws Exception {
        if (entry == null) {
            return;
        }
        String imageUrl = null;
        if (Utils.isImage(entry.getResource().getPath())) {
            imageUrl =
                HU.url(
                    getRepository().URL_ENTRY_GET + entry.getId()
                    + IO.getFileExtension(
                        entry.getResource().getPath()), ARG_ENTRYID,
                            entry.getId(), ARG_IMAGEWIDTH, "75");
        } else {
            List<String> urls = new ArrayList<String>();
            getMetadataManager().getThumbnailUrls(request, entry, urls);
            if (urls.size() > 0) {
                imageUrl = urls.get(0) + "&thumbnail=true";
            }
        }

        String imageAttr = XmlUtil.attrs("imagepath",
                                         getPageHandler().getIconUrl(request,
                                             entry));

        String nodeType = entry.getTypeHandler().getNodeType();
        if (imageUrl != null) {
            nodeType = "imageentry";
        }
        String attrs = imageAttr
                       + XmlUtil.attrs(ATTR_TYPE, nodeType, ATTR_ID,
                                       entry.getId(), ATTR_TOOLTIP,
                                       getTooltip(entry), ATTR_TITLE,
                                       getGraphNodeTitle(entry.getName()));

        if (imageUrl != null) {
            attrs = attrs + " " + XmlUtil.attr("image", imageUrl);
        }
        //        System.err.println(entry.getName() + " " + attrs);
        sb.append(XmlUtil.tag(TAG_NODE, attrs));
        sb.append("\n");

    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param from _more_
     * @param to _more_
     * @param type _more_
     *
     * @throws Exception _more_
     */
    private void addEdgeTag(Appendable sb, Entry from, Entry to, String type)
            throws Exception {
        addEdgeTag(sb, from.getId(), to.getId(), type);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param from _more_
     * @param to _more_
     * @param type _more_
     *
     * @throws Exception _more_
     */
    private void addEdgeTag(Appendable sb, String from, String to,
                            String type)
            throws Exception {
        sb.append(XmlUtil.tag(TAG_EDGE,
                              XmlUtil.attrs(ATTR_TYPE, type, ATTR_FROM, from,
                                            ATTR_TO, to)));
        sb.append("\n");

    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processGraphGet(Request request) throws Exception {

        String id    = (String) request.getId((String) null);
        Entry  entry = getEntryManager().getEntry(request, id);
        if (entry == null) {
            throw new IllegalArgumentException("Could not find entry:" + id);

        }

        StringBuilder   js      = new StringBuilder();
        StringBuilder   linkJS  = new StringBuilder();

        List<String>    nodes   = new ArrayList<String>();
        List<String>    links   = new ArrayList<String>();
        HashSet<String> seen    = new HashSet<String>();

        List<Entry>     entries = new ArrayList<Entry>();
        if (entry.isGroup()) {
            entries.addAll(getEntryManager().getChildren(request, entry));
        }

        addNode(request, entry.getParentEntry(), nodes, seen);
        addLink(request, entry.getParentEntry(), entry, "", links);

        for (Entry e : entries) {
            addNode(request, e, nodes, seen);
            addLink(request, entry, e, "", links);
        }

        getAssociations(request, entry, nodes, links, seen);

        js.append("{\n");
        js.append("\"nodes\":[\n");
        js.append(StringUtil.join(",", nodes));
        js.append("]");
        js.append(",\n");
        js.append("\"links\":[\n");
        js.append(StringUtil.join(",", links));
        js.append("]\n");
        js.append("}\n");

        System.err.println(js);

        return new Result(BLANK, js,
                          getRepository().getMimeTypeFromSuffix(".json"));

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param nodes _more_
     * @param links _more_
     * @param seen _more_
     *
     * @throws Exception _more_
     */
    private void getAssociations(Request request, Entry entry,
                                 List<String> nodes, List<String> links,
                                 HashSet<String> seen)
            throws Exception {
        List<Association> associations =
            getAssociationManager().getAssociations(request, entry.getId());
        for (Association association : associations) {
            Entry from = null;
            Entry to   = null;
            if (association.getFromId().equals(entry.getId())) {
                from = getEntryManager().getEntry(request,
                        association.getToId());
                addNode(request, from, nodes, seen);
                addLink(request, from, entry, association.getType(), links);
            } else {
                to = getEntryManager().getEntry(request,
                        association.getFromId());
                addNode(request, to, nodes, seen);
                addLink(request, entry, from, association.getType(), links);
            }
        }

    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private String getGraphNodeTitle(String s) {
        if (s.length() > 40) {
            s = s.substring(0, 39) + "...";
        }

        return s;
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    private String getTooltip(Entry entry) {
        if (true) {
            return entry.getName();
        }
        String desc = entry.getDescription();
        if ((desc == null) || (desc.length() == 0)) {
            desc = entry.getName();
        }

        return desc;
    }

}
