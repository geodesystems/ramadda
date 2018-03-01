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

import org.ramadda.util.KmlUtil;

import org.w3c.dom.*;

import ucar.unidata.gis.*;
import ucar.unidata.gis.shapefile.*;
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
public class KmlEntryOutputHandler extends OutputHandler {


    /** Map output type */
    public static final OutputType OUTPUT_KML_HTML =
        new OutputType("Display as HTML", "kml.html", OutputType.TYPE_VIEW,
                       "", ICON_KML);

    /** _more_ */
    public static final OutputType OUTPUT_KMZ_IMAGE =
        new OutputType("Display as HTML", "kml.image", OutputType.TYPE_VIEW,
                       "", ICON_KML);


    /**
     * Create a MapOutputHandler
     *
     *
     * @param repository  the repository
     * @param element     the Element
     * @throws Exception  problem generating handler
     */
    public KmlEntryOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_KML_HTML);
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
        if (state.entry == null) {
            return;
        }
        if (state.entry.getTypeHandler().isType("geo_kml")) {
            links.add(makeLink(request, state.entry, OUTPUT_KML_HTML));
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
        if (outputType.equals(OUTPUT_KML_HTML)) {
            return outputKmlHtml(request, entry);
        }

        return null;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result outputKmlHtml(Request request, Entry entry)
            throws Exception {

        StringBuffer sb   = new StringBuffer();
        Element      root = KmlTypeHandler.readKml(getRepository(), entry);
        if (root == null) {
            sb.append(
                getPageHandler().showDialogError(
                    "Could not read KML/KMZ file"));

            return new Result("KML/KMZ Error", sb);
        }
        walkTree(request, entry, sb, root);

        Result result = new Result("", sb);

        return result;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param node _more_
     */
    private void walkTree(Request request, Entry entry, StringBuffer sb,
                          Element node) {
        String tagName = node.getTagName();
        if (tagName.equals(KmlUtil.TAG_KML)) {
            walkChildren(request, entry, sb, node);

            return;
        }

        if (tagName.equals(KmlUtil.TAG_FOLDER)
                || tagName.equals(KmlUtil.TAG_DOCUMENT)
                || tagName.equals(KmlUtil.TAG_TOUR)) {
            //TODO: encode the text
            sb.append("<li> ");
            appendName(node, sb, tagName);
            sb.append("<ul>");
            walkChildren(request, entry, sb, node);
            sb.append("</ul>");
        } else if (tagName.equals(KmlUtil.TAG_PLACEMARK)) {
            sb.append("<li> ");
            appendName(node, sb, tagName);
        } else if (tagName.equals(KmlUtil.TAG_GROUNDOVERLAY)) {
            sb.append("<li> ");
            appendName(node, sb, tagName);
        } else {
            //            sb.append("<li> ");
            //            sb.append(tagName);
        }
    }

    /**
     * _more_
     *
     * @param node _more_
     * @param sb _more_
     * @param tagName _more_
     */
    private void appendName(Node node, StringBuffer sb, String tagName) {
        sb.append(tagName + ": ");
        sb.append(XmlUtil.getGrandChildText(node, KmlUtil.TAG_NAME, tagName));
        String desc = XmlUtil.getGrandChildText(node,
                          KmlUtil.TAG_DESCRIPTION, null);
        if (desc != null) {
            sb.append(HtmlUtils.div(desc,
                                    HtmlUtils.cssClass("kml-description")));
        }

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param node _more_
     */
    private void walkChildren(Request request, Entry entry, StringBuffer sb,
                              Element node) {
        NodeList children = XmlUtil.getElements(node);
        for (int i = 0; i < children.getLength(); i++) {
            Element child = (Element) children.item(i);
            walkTree(request, entry, sb, child);
        }
    }





}
