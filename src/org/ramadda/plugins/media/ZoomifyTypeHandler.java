/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import java.io.*;

import java.util.Hashtable;


/**
 *
 *
 */
public class ZoomifyTypeHandler extends GenericTypeHandler {


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public ZoomifyTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {

        if ( !tag.equals("zoomify")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
        StringBuilder sb = new StringBuilder();
        String style =
            "width:800px;height:600px;border:1px solid black;color:#333;background-color: black;";
        String s = (String) entry.getValue(3);
        if (Utils.stringDefined(s)) {
            style += s;
        }
        style = style.replaceAll("\n", " ");
        //      sb.append(HU.importCss( ".openseadragon {" + style +"}"));
        HU.importJS(
            sb,
            getPageHandler().makeHtdocsUrl(
                "/lib/openseadragon/openseadragon.min.js"));
        String id = Utils.getGuid();
        sb.append("<center>\n");
        HU.div(sb, "", " id=" + id + " style='" + style + "'");
        sb.append("\n</center>\n");
        String template =
            "OpenSeadragon({id:\"" + id
            + "\",prefixUrl: \"/repository/lib/openseadragon/images/\",\nshowNavigator:  true,\n        tileSources:    [{\ntype:\"zoomifytileservice\",\nwidth:      ${image_width},\nheight:     ${image_height},\n tilesUrl:   \"${tiles_url}\"}]});";

        template = template.replace("${image_width}", "" + entry.getValue(0));
        template = template.replace("${image_height}",
                                    "" + entry.getValue(1));
        template = template.replace("${tiles_url}", "" + entry.getValue(2));
        HU.script(sb, template);

        return sb.toString();
    }



}
