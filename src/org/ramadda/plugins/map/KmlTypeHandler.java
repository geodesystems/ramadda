/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.map;

import org.ramadda.repository.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.type.GenericTypeHandler;

import org.ramadda.util.geo.KmlUtil;

import org.w3c.dom.*;
import org.w3c.dom.Element;

import ucar.unidata.util.IOUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;
import java.util.List;
import java.util.zip.*;

/**
 */
@SuppressWarnings("unchecked")
public class KmlTypeHandler extends GenericTypeHandler {

    public KmlTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
        if ( !entry.isFile()) {
            return;
        }
        initializeKmlEntry(request, entry, newEntry);
    }

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

    public static InputStream readDoc(Repository repository, Entry entry)
            throws Exception {
        String  path    = entry.getFile().toString();
        if (path.toLowerCase().endsWith(".kmz")) {
            ZipInputStream zin =
                new ZipInputStream(
                    repository.getStorageManager().getFileInputStream(path));
            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                String name = ze.getName().toLowerCase();
                if (name.toLowerCase().endsWith(".kml")) {
		    return zin;
                }
            }
            IOUtil.close(zin);
	    return null;
        } else {
            return repository.getStorageManager().getInputStream(
								 entry.getFile().toString());
        }

    }

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

    private static double convert(String value, double dflt) {
        if (value == null) {
            return dflt;
        }

        return Double.parseDouble(value);
    }

    private static void setBounds(double[] nwse, double lat, double lon) {
        setLat(nwse, lat);
        setLon(nwse, lon);
    }

    private static void setLon(double[] nwse, double lon) {
        nwse[1] = (nwse[1] == Entry.NONGEO)
                  ? lon
                  : Math.min(nwse[1], lon);
        nwse[3] = (nwse[3] == Entry.NONGEO)
                  ? lon
                  : Math.max(nwse[3], lon);
    }

    private static void setLat(double[] nwse, double lat) {
        nwse[0] = (nwse[0] == Entry.NONGEO)
                  ? lat
                  : Math.max(nwse[0], lat);
        nwse[2] = (nwse[2] == Entry.NONGEO)
                  ? lat
                  : Math.min(nwse[2], lat);
    }

    private static void setNorth(double[] nwse, double lat) {
        nwse[0] = (nwse[0] == Entry.NONGEO)
                  ? lat
                  : Math.max(nwse[0], lat);
    }

    private static void setSouth(double[] nwse, double lat) {
        nwse[2] = (nwse[2] == Entry.NONGEO)
                  ? lat
                  : Math.min(nwse[2], lat);
    }

    private static void setWest(double[] nwse, double lon) {
        nwse[1] = (nwse[1] == Entry.NONGEO)
                  ? lon
                  : Math.min(nwse[1], lon);
    }

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
        String file = entry.getResource().getPath();
	String url;
	if(file.toLowerCase().indexOf(".kmz")>=0) {
	    url =  getRepository().getUrlBase()+"/entry/show?entryid=" + entry.getId() +"&output=kml.doc&converthref=true";
	} else {
	    url = getMapManager().getMapResourceUrl(request, entry);
	}

        map.addKmlUrl(entry.getName(),
		      url,
                      true, null);

        return false;
    }

}
