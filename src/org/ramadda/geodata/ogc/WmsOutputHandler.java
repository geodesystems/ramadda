/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.ogc;


import org.ramadda.repository.*;
import org.ramadda.repository.output.*;

import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;

import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.gis.WmsSelection;
import ucar.unidata.util.WmsUtil;
import ucar.unidata.xml.XmlUtil;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;



/**
 * This class handles WMS Capabilities URLs. It loads the XML and generates a web page listing
 * each of the layers
 */
@SuppressWarnings("unchecked")
public class WmsOutputHandler extends OutputHandler {

    /** output type for viewing map */
    public static final OutputType OUTPUT_WMS_VIEWER =
        new OutputType("WMS Map View", "wms.viewer", OutputType.TYPE_VIEW,
                       "", "/icons/globe.jpg");


    /**
     *   Caches the DOMS from the url
     *   TODO: Expire the cache after some time so we would pick up any changes to the wms xml
     */
    private Hashtable<String, Element> wmsCache = new Hashtable<String,
                                                      Element>();



    /**
     * Constructor
     *
     * @param repository The repository
     * @param element The xml element from outputhandlers.xml
     * @throws Exception On badness
     */
    public WmsOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        //add in the output types
        addType(OUTPUT_WMS_VIEWER);
    }



    /**
     * This method gets called to add to the types list the OutputTypes that are applicable
     * to the given State.  The State can be viewing a single Entry (state.entry non-null),
     * viewing a Group (state.group non-null).
     *
     * @param request The request
     * @param state The state
     * @param links _more_
     *
     *
     * @throws Exception On badness
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        //If it is a single entry whose type is wms.capabilities then add the viewer link 
        if (state.entry != null) {
            if (state.entry.getType().equals("wms.capabilities")) {
                links.add(makeLink(request, state.entry, OUTPUT_WMS_VIEWER));
            }
        }
    }


    /**
     * This reads the WMS capabilities for the given entry. It caches the DOM
     *
     * @param entry the entry
     *
     * @return The WMS DOM
     *
     * @throws Exception On badness
     */
    private Element getWmsRoot(Entry entry) throws Exception {
        String  wmsUrl = entry.getResource().getPath();
        Element root   = wmsCache.get(wmsUrl);
        if (root == null) {
            root = XmlUtil.getRoot(wmsUrl, getClass());
            if (wmsCache.size() > 10) {
                wmsCache = new Hashtable<String, Element>();
            }
            //TODO: Expire the cache after some time so we would pick up any changes to the wms xml
            wmsCache.put(wmsUrl, root);
        }

        return root;
    }



    /**
     * Output the html for the given entry
     *
     * @param request the request
     * @param outputType type of output
     * @param entry the entry
     *
     * @return the result
     *
     * @throws Exception on badness
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        //For now we just have one output - the wms viewer

        StringBuffer sb = new StringBuffer();

        //Include the javascript library
        sb.append(HtmlUtils.importJS(getFileUrl("/wms/wms.js")));


        //Get the DOM
        Element root = getWmsRoot(entry);

        //Find the capability node
        Element capabilityNode = XmlUtil.findDescendant(root,
                                     WmsUtil.TAG_CAPABILITY);

        //Get the top level layer node
        Element topLevelLayer = XmlUtil.findDescendant(capabilityNode,
                                    WmsUtil.TAG_LAYER);
        if (topLevelLayer == null) {
            sb.append("No top level layer found");

            return new Result("", sb);
        }
        String title = XmlUtil.getGrandChildText(topLevelLayer,
                           WmsUtil.TAG_TITLE);
        sb.append(header(title));


        //Find the sub layers
        List<Element> layerNodes =
            (List<Element>) XmlUtil.findChildren(topLevelLayer,
                WmsUtil.TAG_LAYER);

        //Go through each layer, find its styles and then use the WmsUtil method to extract out the
        //urls and other information from the DOM
        for (Element layerNode : layerNodes) {
            String[] message = new String[] { null };
            List<Element> styles = XmlUtil.findChildren(layerNode,
                                       WmsUtil.TAG_STYLE);

            //Throw in the parent layer node so we get its title, etc.
            styles.add(0, layerNode);
            List<WmsSelection> layers = WmsUtil.processNode(root, styles,
                                            message, false);
            //Check if there was a problem
            if (message[0] != null) {
                sb.append(message[0]);
                sb.append("<br>");

                continue;
            }
            StringBuffer layerSB = new StringBuffer("<ul>");

            //Now just go through the children styles
            for (int i = 1; i < layers.size(); i++) {
                WmsSelection wms = layers.get(i);
                layerSB.append("<li>");
                layerSB.append(getHref(wms));
            }
            layerSB.append("</ul>");
            sb.append(HtmlUtils.makeShowHideBlock(layers.get(0).getTitle(),
                    layerSB.toString(), false));
        }

        return new Result("", sb);
    }


    /**
     * Get the href link for the given wms layer
     *
     * @param wms wms info
     *
     * @return href
     */
    public String getHref(WmsSelection wms) {
        String href = HtmlUtils.href(getUrl(wms), wms.getTitle());

        return href;
    }


    /**
     * Get the url for the given wms layer
     *
     * @param wms wms info
     *
     * @return url to image
     */
    public String getUrl(WmsSelection wms) {
        double width  = wms.getBounds().getDegreesX();
        double height = wms.getBounds().getDegreesY();
        String url = wms.assembleRequest(wms.getBounds(), 600,
                                         (int) (600 * height / width));

        return url;
    }



}
