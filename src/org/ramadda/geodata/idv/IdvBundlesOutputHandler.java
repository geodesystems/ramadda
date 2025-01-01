/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.idv;


import org.ramadda.repository.Entry;
import org.ramadda.repository.EntryManager;
import org.ramadda.repository.Link;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.Result;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.xml.XmlUtil;


import java.util.List;


/**
 * A class to output the IDV bundles XML
 */
public class IdvBundlesOutputHandler extends OutputHandler {

    /** bundles XML tag */
    private static final String TAG_BUNDLES = "bundles";

    /** bundle XML tag */
    private static final String TAG_BUNDLE = "bundle";

    /** Default top level category */
    private final static String DEFAULT_TOPCATEGORY = "Toolbar";

    /** Default top level category */
    private final static String ARG_TOP = "top";

    /** Map output type */
    public static final OutputType OUTPUT_BUNDLES =
        new OutputType("IDV Bundles XML", "idv.bundles",
                       OutputType.TYPE_FEEDS, "", "/idv/idv.gif");



    /**
     * Create a IdvBundlesOutputHandler
     *
     *
     * @param repository  the repository
     * @param element     the Element
     * @throws Exception  problem generating handler
     */
    public IdvBundlesOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_BUNDLES);
    }



    /**
     * Get the entry links
     *
     * @param request  the Request
     * @param state    the repository State
     * @param links    the links to add to
     *
     * @throws Exception  problem creating links
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if (state.getEntry() != null) {
            for (Entry entry : state.getAllEntries()) {
                if (isBundle(entry)) {
                    links.add(makeLink(request, state.getEntry(),
                                       OUTPUT_BUNDLES));

                    break;
                }
            }
        }
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

        boolean justOneEntry = group.isDummy() && (children.size()==1);

        // can't use category since that is the data type
        String topCategory = request.getString(ARG_TOP, DEFAULT_TOPCATEGORY);

        String title       = (justOneEntry
                              ? children.get(0).getName()
                              : group.getName());
        title = request.getString(ARG_TITLE, title);
        Document doc = XmlUtil.makeDocument();
        Element root = XmlUtil.create(doc, TAG_BUNDLES, null,
                                      new String[] { ATTR_NAME,
                title });

        if (justOneEntry) {
            addEntryToXml(request, children.get(0), root, topCategory, 1);
        } else {
            topCategory += ">" + title;
            for (Entry sg : children) {
                addEntryToXml(request, sg, root, topCategory, 1);
            }
        }

        StringBuffer sb = new StringBuffer(XmlUtil.XML_HEADER);
        sb.append(XmlUtil.toString(root));
        Result result = new Result(title, sb, "text/xml");
        result.setReturnFilename(title + ".bundles.xml");

        return result;


    }

    /**
     * Add the entry to the root XML
     *
     * @param request   the Request
     * @param entry     the Entry
     * @param root      the XML root node
     * @param category  the bundle category
     * @param level     the bundle level in the heirarchy (depth) from top
     *
     * @throws Exception  problem adding
     */
    public void addEntryToXml(Request request, Entry entry, Element root,
                              String category, int level)
            throws Exception {

        int depth = request.get(ARG_DEPTH, 2);  //get grandChildren
        if (level > depth) {
            return;
        }
        if (entry.isGroup()) {
            String subCat = category + ">" + entry.getName();
            List<Entry> children = getEntryManager().getChildren(request,
                                       entry);
            for (Entry child : children) {
                addEntryToXml(request, child, root, subCat, level + 1);
            }
        } else {
            if ( !isBundle(entry)) {
                return;
            }
            String name = entry.getName();
            String url = request.getAbsoluteUrl(
                             getEntryManager().getEntryResourceUrl(
								   request, entry, EntryManager.ARG_INLINE_DFLT, false, false));
            XmlUtil.create(TAG_BUNDLE, root, new String[] {
                ATTR_CATEGORY, category, ATTR_NAME, name, ATTR_URL, url
            });
        }

    }

    /**
     * Is this entry a bundle?
     *
     * @param entry  the Entry
     *
     * @return  true if a bundle
     */
    public static boolean isBundle(Entry entry) {
        return (entry.getResource().getPath().endsWith(".xidv")
                || entry.getResource().getPath().endsWith(".zidv"));
    }
}
