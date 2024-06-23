/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.ogc;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.GeoUtils;

import org.w3c.dom.*;


import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import java.util.List;


/**
 * Manages WMS capabilities URLs
 */
@SuppressWarnings("unchecked")
public class WmsCapabilitiesTypeHandler extends ExtensibleGroupTypeHandler {


    /**
     * ctor
     *
     * @param repository the repository
     * @param node the types.xml node
     * @throws Exception On badness
     */
    public WmsCapabilitiesTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param fromImport _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
        //Read the xml
        String url = entry.getResource().getPath();
        //        System.err.println("URL:" + url);
        InputStream fis  = getStorageManager().getInputStream(url);
        Element     root = XmlUtil.getRoot(fis);
        IOUtil.close(fis);

        String version = XmlUtil.getAttribute(root, WmsUtils.ATTR_VERSION,
                             "1.3.0");
        String  format     = "image/png";
        String  defaultSrs = "EPSG:4326";

        Element service    = XmlUtil.findChild(root, WmsUtils.TAG_SERVICE);
        if (service == null) {
            logError("WMS: No service node", null);

            return;
        }
        Element capabilityNode = XmlUtil.findChild(root,
                                     WmsUtils.TAG_CAPABILITY);
        if (capabilityNode == null) {
            logError("WMS: No capability node", null);

            return;
        }
        Element getMap = XmlUtil.findDescendantFromPath(capabilityNode,
                             xpath(WmsUtils.TAG_REQUEST,
                                   WmsUtils.TAG_GETMAP));
        if (getMap == null) {
            logError("WMS: No getMap node", null);

            return;
        }

        Element onlineResource = XmlUtil.findDescendantFromPath(getMap,
                                     xpath(WmsUtils.TAG_DCPTYPE,
                                           WmsUtils.TAG_HTTP,
                                           WmsUtils.TAG_GET,
                                           WmsUtils.TAG_ONLINERESOURCE));
        if (onlineResource == null) {
            logError("WMS: No onlineResource node", null);

            return;
        }


        String entryName = XmlUtil.getGrandChildText(service,
                               WmsUtils.TAG_TITLE, entry.getName());

        //A hack for the NRL WMS servers
        String categoryName = StringUtil.findPattern(url,
                                  ".*/category/([^\\?/]+)\\?.*");
        if (categoryName == null) {
            categoryName = StringUtil.findPattern(url,
                    ".*/family/([^\\?/]+)\\?.*");
        }

        if (categoryName == null) {
            categoryName = StringUtil.findPattern(url,
                    ".*/wms/([^\\?/]+)\\?.*");
        }

        if (categoryName != null) {
            entryName += " - " + categoryName;
        }

        entryName = entryName.replaceAll("_", " ");

        entry.setName(entryName);
        if (entry.getDescription().length() == 0) {
            entry.setDescription(XmlUtil.getGrandChildText(service,
                    WmsUtils.TAG_ABSTRACT, entry.getDescription()));
        }
        addMetadata(request,entry, service);

        String getMapUrl = XmlUtil.getAttribute(onlineResource, "xlink:href");
        if (getMapUrl.indexOf("?") < 0) {
            getMapUrl += "?";
        } else {
            getMapUrl += "&";
        }
        getMapUrl += HtmlUtils.arg(WmsUtils.ARG_VERSION, version);
        getMapUrl += "&"
                     + HtmlUtils.arg(WmsUtils.ARG_REQUEST,
                                     WmsUtils.REQUEST_GETMAP);

        List<Entry> children = new ArrayList<Entry>();
        //We'll insert these later
        entry.putProperty("entries", children);

        List layers = XmlUtil.findDescendants(root, WmsUtils.TAG_LAYER);
        TypeHandler layerTypeHandler =
            getRepository().getTypeHandler("type_wms_layer");

        double mnorth = Double.NaN,
               msouth = Double.NaN,
               mwest  = Double.NaN,
               meast  = Double.NaN;

        for (int i = 0; i < layers.size(); i++) {
            Element layer    = (Element) layers.get(i);
            Element nameNode = XmlUtil.findChild(layer, WmsUtils.TAG_NAME);
            if (nameNode == null) {
                continue;
            }

            String name = XmlUtil.getChildText(nameNode);
            String title = XmlUtil.getGrandChildText(layer,
                               WmsUtils.TAG_TITLE, name);
            String desc = XmlUtil.getGrandChildText(layer,
                              WmsUtils.TAG_ABSTRACT, "");

            String srsAttr   = defaultSrs;
            String imageUrl  = getMapUrl;
            String layersArg = name.replaceAll(" ", "%20");
            imageUrl += "&"
                        + HtmlUtils.arg(WmsUtils.ARG_LAYERS, layersArg,
                                        false);
            imageUrl += "&"
                        + HtmlUtils.arg(WmsUtils.ARG_FORMAT, format, true);

            double  north = Double.NaN,
                    south = Double.NaN,
                    west  = Double.NaN,
                    east  = Double.NaN;


            Element crs = XmlUtil.findChildRecurseUp(layer, WmsUtils.TAG_CRS);
            if (crs != null) {
                imageUrl +=
                    "&"
                    + HtmlUtils.arg(WmsUtils.ARG_CRS,
                                    srsAttr = XmlUtil.getChildText(crs),
                                    true);
            } else {
                List srss = XmlUtil.findChildrenRecurseUp(layer,
                                WmsUtils.TAG_SRS);
                Element srs = null;
                for (Element e : (List<Element>) srss) {
                    if (srs == null) {
                        srs = e;
                    }
                    if (XmlUtil.getChildText(e).indexOf("4326") >= 0) {
                        srs = e;

                        break;
                    }

                }


                if (srs != null) {
                    imageUrl += "&"
                                + HtmlUtils.arg(WmsUtils.ARG_SRS,
                                    srsAttr = XmlUtil.getChildText(srs),
                                    true);
                }
            }


            //<BoundingBox CRS="EPSG:4326" minx="-180.0" miny="-90" maxx="180.0" maxy="90"/>
            Element llbbox = XmlUtil.findChildRecurseUp(layer,
                                 WmsUtils.TAG_LATLONBOUNDINGBOX);


            Element gbbox = XmlUtil.findChildRecurseUp(layer,
                                WmsUtils.TAG_EX_GEOGRAPHICBOUNDINGBOX);

            //            System.err.println("layer:" + name + " ll:" + llbbox + " ");

            String bboxString = null;
            if (llbbox != null) {
                north = GeoUtils.decodeLatLon(XmlUtil.getAttribute(llbbox,
                        WmsUtils.ATTR_MAXY));
                south = GeoUtils.decodeLatLon(XmlUtil.getAttribute(llbbox,
                        WmsUtils.ATTR_MINY));
                west = GeoUtils.decodeLatLon(XmlUtil.getAttribute(llbbox,
                        WmsUtils.ATTR_MINX));
                east = GeoUtils.decodeLatLon(XmlUtil.getAttribute(llbbox,
                        WmsUtils.ATTR_MAXX));
                bboxString = west + "," + south + "," + east + "," + north;
            } else if (gbbox != null) {
                north = GeoUtils.decodeLatLon(XmlUtil.getGrandChildText(gbbox,
                        WmsUtils.TAG_NORTHBOUNDLATITUDE, ""));
                south = GeoUtils.decodeLatLon(XmlUtil.getGrandChildText(gbbox,
                        WmsUtils.TAG_SOUTHBOUNDLATITUDE, ""));
                east = GeoUtils.decodeLatLon(XmlUtil.getGrandChildText(gbbox,
                        WmsUtils.TAG_EASTBOUNDLONGITUDE, ""));
                west = GeoUtils.decodeLatLon(XmlUtil.getGrandChildText(gbbox,
                        WmsUtils.TAG_WESTBOUNDLONGITUDE, ""));
                bboxString = west + "," + south + "," + east + "," + north;
            }



            Element bbox = XmlUtil.findChildRecurseUp(layer,
                               WmsUtils.TAG_BOUNDINGBOX);

            if (bbox == null) {
                logError("WMS: No BBOX specified: "
                         + XmlUtil.toString(layer), null);
                System.err.println("WMS: No BBOX specified: "
                                   + XmlUtil.toString(layer));

                continue;
            }


            String minx = XmlUtil.getAttribute(bbox, WmsUtils.ATTR_MINX);
            String maxx = XmlUtil.getAttribute(bbox, WmsUtils.ATTR_MAXX);
            String miny = XmlUtil.getAttribute(bbox, WmsUtils.ATTR_MINY);
            String maxy = XmlUtil.getAttribute(bbox, WmsUtils.ATTR_MAXY);

            if (bboxString == null) {
                bboxString = minx + "," + miny + "," + maxx + "," + maxy;
            }


            //Don't encode the args
            imageUrl += "&"
                        + HtmlUtils.arg(WmsUtils.ARG_BBOX, bboxString, false);
            imageUrl += "&" + HtmlUtils.arg(WmsUtils.ARG_WIDTH, "400") + "&"
                        + HtmlUtils.arg(WmsUtils.ARG_HEIGHT, "400");

            //<BoundingBox CRS="EPSG:4326" minx="-180.0" miny="-90" maxx="180.0" maxy="90"/>
            Element style = XmlUtil.findChildRecurseUp(layer,
                                WmsUtils.TAG_STYLE);
            if (style != null) {
                imageUrl += "&"
                            + HtmlUtils.arg("styles",
                                            XmlUtil.getGrandChildText(style,
                                                WmsUtils.TAG_NAME,
                                                    "default"));
            }



            Resource resource = new Resource(imageUrl, Resource.TYPE_URL);
            Date     now      = new Date();
            Date     date     = now;



            //The  values array corresponds  to the column defs in wmstypes.xml
            Object[] values =
                layerTypeHandler.makeEntryValues(new Hashtable());
            values[0] = getMapUrl;
            values[1] = name;
            values[2] = version;
            values[3] = srsAttr;
            values[4] = format;

            Entry layerEntry =
                layerTypeHandler.createEntry(getRepository().getGUID());

            if ( !Double.isNaN(north)) {
                mnorth = Double.isNaN(mnorth)
                         ? north
                         : Math.max(mnorth, north);
                msouth = Double.isNaN(msouth)
                         ? south
                         : Math.min(msouth, south);
                meast  = Double.isNaN(meast)
                         ? east
                         : Math.max(meast, east);
                mwest  = Double.isNaN(mwest)
                         ? west
                         : Math.min(mwest, west);
                //                System.err.println("north:" + north +" south:" + south +" east:" + east +" west:" + west);
                layerEntry.setNorth(north);
                layerEntry.setSouth(south);
                layerEntry.setEast(east);
                layerEntry.setWest(west);
            }


            //A hack for the usda wms
            String year = StringUtil.findPattern(title,
                              ".*[^\\d](\\d\\d\\d\\d)[^\\d].*");
            if (year != null) {
                try {
                    date = Utils.parseDate(year);
                } catch (Exception exc) {}
            }

            String dttm = StringUtil.findPattern(title,
                              ".*[^\\d](\\d\\d\\d\\d\\.\\d\\d\\.\\d\\d).*");
            if (dttm != null) {
                try {
                    dttm = dttm.replaceAll("\\.", "-");
                    date = Utils.parseDate(dttm);
                } catch (Exception exc) {}
            }


            title = title.replaceAll("_", " ");
            layerEntry.initEntry(title, desc, entry, entry.getUser(),
                                 resource, "", Entry.DEFAULT_ORDER,
                                 now.getTime(), now.getTime(),
                                 date.getTime(), date.getTime(), values);
            children.add(layerEntry);
        }


        if ( !Double.isNaN(mnorth)) {
            //            System.err.println("final north:" + mnorth +" south:" + msouth +" east:" + meast +" west:" + mwest);
            entry.setNorth(mnorth);
            entry.setSouth(msouth);
            entry.setEast(meast);
            entry.setWest(mwest);
        }
    }


    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     * @param fromImport _more_
     */
    @Override
    public void doFinalEntryInitialization(Request request, Entry entry,
                                           boolean fromImport) {
        try {
            super.doFinalEntryInitialization(request, entry, fromImport);
            List<Entry> childrenEntries =
                (List<Entry>) entry.getProperty("entries");
            if (childrenEntries == null) {
                return;
            }
            entry.putProperty("entries", new ArrayList<Entry>());
            getEntryManager().addNewEntries(request, childrenEntries);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }



    /**
     * _more_
     *
     * @param node _more_
     * @param path _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception on badness
     */
    private String getText(Element node, String path, String dflt)
            throws Exception {
        Element child = XmlUtil.findDescendantFromPath(node, path);
        if (child != null) {
            return XmlUtil.getChildText(child);
        }

        return dflt;

    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param service _more_
     *
     * @throws Exception on badness
     */
    private void addMetadata(Request request,Entry entry, Element service) throws Exception {
        addKeywords(request,entry, service);
        String person = getText(service,
                                xpath(WmsUtils.TAG_CONTACTINFORMATION,
                                      WmsUtils.TAG_CONTACTPERSONPRIMARY,
                                      WmsUtils.TAG_CONTACTPERSON), null);
        String org = getText(service,
                             xpath(WmsUtils.TAG_CONTACTINFORMATION,
                                   WmsUtils.TAG_CONTACTORGANIZATION), "");
        String position = getText(service,
                                  xpath(WmsUtils.TAG_CONTACTINFORMATION,
                                        WmsUtils.TAG_CONTACTPOSITION), "");
        String email =
            getText(service,
                    xpath(WmsUtils.TAG_CONTACTINFORMATION,
                          WmsUtils.TAG_CONTACTELECTRONICMAILADDRESS), "");

        if (person != null) {
            getMetadataManager().addMetadata(request,
                entry,
                new Metadata(
                    getRepository().getGUID(), entry.getId(),
                    getMetadataManager().findType("project_person"), true, person, position, org, email,
                    ""));
        }
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param service _more_
     *
     * @throws Exception on badness
     */
    private void addKeywords(Request request,Entry entry, Element service) throws Exception {
        Element keyWords = XmlUtil.findChild(service,
                                             WmsUtils.TAG_KEYWORDLIST);
        if (keyWords != null) {
            List children = XmlUtil.findChildren(keyWords,
                                WmsUtils.TAG_KEYWORD);
            for (int i = 0; i < children.size(); i++) {
                String text = XmlUtil.getChildText((Element) children.get(i));
                getMetadataManager().addMetadata(request,entry,
                        new Metadata(getRepository().getGUID(),
                                     entry.getId(), getMetadataManager().findType("content.keyword"), true,
                                     text, "", "", "", ""));

            }
        }
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @return _more_
     */
    public static String xpath(String... args) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(".");
            }
            sb.append(args[i]);
        }

        return sb.toString();
    }


}
