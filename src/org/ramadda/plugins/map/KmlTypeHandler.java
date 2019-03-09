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

package org.ramadda.plugins.map;


import org.ramadda.repository.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.KmlUtil;

import org.w3c.dom.*;
import org.w3c.dom.Element;


import ucar.unidata.gis.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import java.awt.geom.Rectangle2D;


import java.io.File;



import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.*;


/**
 */
public class KmlTypeHandler extends GenericTypeHandler {



    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public KmlTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param parent _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
        if ( !entry.isFile()) {
            return;
        }
        initializeKmlEntry(request, entry, newEntry);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    public static void initializeKmlEntry(Request request, Entry entry,
                                          boolean newEntry)
            throws Exception {

        Element kmlRoot = readKml(request.getRepository(), entry);
        if (kmlRoot == null) {
            return;
        }
        double[] nwse = new double[] { Entry.NONGEO, Entry.NONGEO,
                                       Entry.NONGEO, Entry.NONGEO, };

        List<Element> lats = (List<Element>) XmlUtil.findDescendants(kmlRoot,
                                 KmlUtil.TAG_LATITUDE);
        for (Element latNode : lats) {
            setLat(nwse, Double.parseDouble(XmlUtil.getChildText(latNode)));
        }
        List<Element> lons = (List<Element>) XmlUtil.findDescendants(kmlRoot,
                                 KmlUtil.TAG_LONGITUDE);
        for (Element lonNode : lons) {
            setLon(nwse, Double.parseDouble(XmlUtil.getChildText(lonNode)));
        }



        initializeEntry(entry, kmlRoot, nwse);


        if (nwse[0] != Entry.NONGEO) {
            entry.setNorth(nwse[0]);
        }
        if (nwse[1] != Entry.NONGEO) {
            entry.setWest(nwse[1]);
        }
        if (nwse[2] != Entry.NONGEO) {
            entry.setSouth(nwse[2]);
        }
        if (nwse[3] != Entry.NONGEO) {
            entry.setEast(nwse[3]);
        }
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
    @Override
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        if ( !Misc.equals(entry.getValue(0, "false"), "true")) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        String wiki =
            "+section title={{name}}\n{{description wikify=\"true\"}}\n+row\n+col-md-8\n{{map width=\"100%\" height=\"450\" viewBounds=\"<bounds>\" details=\"true\" displayDiv=\"mapDisplay\" showSearch=\"true\" }}\n-col\n+col-md-4\n<div style=\"padding-top:20px;\"></div>\n<div id=\"mapDisplay\" style=\"max-height:450px; overflow-y:auto;\"></div>\n-col\n-row\n-section\n";
        sb.append(getRepository().getWikiManager().wikifyEntry(request,
                entry, wiki));

        return new Result("", sb);
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Element readKml(Repository repository, Entry entry)
            throws Exception {
        Element kmlRoot = null;
        String  path    = entry.getFile().toString();
        if (path.toLowerCase().endsWith(".kmz")) {
            ZipInputStream zin =
                new ZipInputStream(
                    repository.getStorageManager().getFileInputStream(path));
            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                String name = ze.getName().toLowerCase();
                if (name.toLowerCase().endsWith(".kml")) {
                    kmlRoot =
                        XmlUtil.getRoot(new String(IOUtil.readBytes(zin)));

                    break;
                }
            }
            IOUtil.close(zin);
        } else {
            kmlRoot = XmlUtil.getRoot(
                repository.getStorageManager().readSystemResource(
                    entry.getFile()));
        }

        return kmlRoot;
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param node _more_
     * @param nwse _more_
     */
    private static void initializeEntry(Entry entry, Element node,
                                        double[] nwse) {
        String tagName = node.getTagName();
        if (tagName.equals(KmlUtil.TAG_FOLDER)
                || tagName.equals(KmlUtil.TAG_KML)
                || tagName.equals(KmlUtil.TAG_DOCUMENT)) {
            NodeList children = XmlUtil.getElements(node);
            for (int i = 0; i < children.getLength(); i++) {
                Element child = (Element) children.item(i);
                initializeEntry(entry, child, nwse);
            }

            return;
        }


        if (tagName.equals(KmlUtil.TAG_GROUNDOVERLAY)) {
            Element llbox = XmlUtil.findChild(node, KmlUtil.TAG_LATLONBOX);
            if (llbox != null) {
                setNorth(nwse,
                         convert(XmlUtil.getGrandChildText(llbox,
                             KmlUtil.TAG_NORTH, null), Entry.NONGEO));
                setWest(nwse,
                        convert(XmlUtil.getGrandChildText(llbox,
                            KmlUtil.TAG_WEST, null), Entry.NONGEO));
                setSouth(nwse,
                         convert(XmlUtil.getGrandChildText(llbox,
                             KmlUtil.TAG_SOUTH, null), Entry.NONGEO));
                setEast(nwse,
                        convert(XmlUtil.getGrandChildText(llbox,
                            KmlUtil.TAG_EAST, null), Entry.NONGEO));
            } else {
                System.err.println("no  latlonbox:" + XmlUtil.toString(node));
            }

            return;
        }

        if (tagName.equals(KmlUtil.TAG_PLACEMARK)) {
            List<Element> coords =
                (List<Element>) XmlUtil.findDescendants(node,
                    KmlUtil.TAG_COORDINATES);
            for (Element coordNode : coords) {
                setBounds(nwse, XmlUtil.getChildText(coordNode));
            }
            if (coords.size() > 0) {
                return;
            }
            System.err.println("no  coords:" + XmlUtil.toString(node));

            return;
        }

        //        System.err.println("Unknown:" + tagName);
    }

    /**
     * _more_
     *
     * @param nwse _more_
     * @param coordString _more_
     */
    private static void setBounds(double[] nwse, String coordString) {
        if (coordString != null) {
            double[][] coords = KmlUtil.parseCoordinates(coordString);
            for (int i = 0; i < coords[0].length; i++) {
                double lat = coords[1][i];
                double lon = coords[0][i];
                setBounds(nwse, lat, lon);
            }
        }
    }



    /**
     * _more_
     *
     * @param value _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private static double convert(String value, double dflt) {
        if (value == null) {
            return dflt;
        }

        return Double.parseDouble(value);
    }

    /**
     * _more_
     *
     * @param nwse _more_
     * @param lat _more_
     * @param lon _more_
     */
    private static void setBounds(double[] nwse, double lat, double lon) {
        setLat(nwse, lat);
        setLon(nwse, lon);
    }

    /**
     * _more_
     *
     * @param nwse _more_
     * @param lon _more_
     */
    private static void setLon(double[] nwse, double lon) {
        nwse[1] = (nwse[1] == Entry.NONGEO)
                  ? lon
                  : Math.min(nwse[1], lon);
        nwse[3] = (nwse[3] == Entry.NONGEO)
                  ? lon
                  : Math.max(nwse[3], lon);
    }


    /**
     * _more_
     *
     * @param nwse _more_
     * @param lat _more_
     */
    private static void setLat(double[] nwse, double lat) {
        nwse[0] = (nwse[0] == Entry.NONGEO)
                  ? lat
                  : Math.max(nwse[0], lat);
        nwse[2] = (nwse[2] == Entry.NONGEO)
                  ? lat
                  : Math.min(nwse[2], lat);
    }


    /**
     * _more_
     *
     * @param nwse _more_
     * @param lat _more_
     */
    private static void setNorth(double[] nwse, double lat) {
        nwse[0] = (nwse[0] == Entry.NONGEO)
                  ? lat
                  : Math.max(nwse[0], lat);
    }

    /**
     * _more_
     *
     * @param nwse _more_
     * @param lat _more_
     */
    private static void setSouth(double[] nwse, double lat) {
        nwse[2] = (nwse[2] == Entry.NONGEO)
                  ? lat
                  : Math.min(nwse[2], lat);
    }


    /**
     * _more_
     *
     * @param nwse _more_
     * @param lon _more_
     */
    private static void setWest(double[] nwse, double lon) {
        nwse[1] = (nwse[1] == Entry.NONGEO)
                  ? lon
                  : Math.min(nwse[1], lon);
    }

    /**
     * _more_
     *
     * @param nwse _more_
     * @param lon _more_
     */
    private static void setEast(double[] nwse, double lon) {
        nwse[3] = (nwse[3] == Entry.NONGEO)
                  ? lon
                  : Math.max(nwse[3], lon);
    }

    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param map _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public boolean addToMap(Request request, Entry entry, MapInfo map)
            throws Exception {
        map.addKmlUrl(entry.getName(),
                      getEntryManager().getEntryResourceUrl(request, entry,
                          false), true, null);

        return false;
    }




}
