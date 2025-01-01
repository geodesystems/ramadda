/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.map;


import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;


import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;


import org.ramadda.repository.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JQuery;


import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.ramadda.util.geo.KmlUtil;


import org.w3c.dom.*;

import ucar.unidata.geoloc.Bearing;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.*;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 */
@SuppressWarnings("unchecked")
public class GpxTypeHandler extends PointTypeHandler {

    /** _more_ */
    private static int IDX = RecordTypeHandler.IDX_LAST + 1;

    /** _more_ */
    private static int IDX_DISTANCE = IDX++;

    /** _more_ */
    private static int IDX_TOTAL_TIME = IDX++;

    /** _more_ */
    private static int IDX_MOVING_TIME = IDX++;

    /** _more_ */
    private static int IDX_SPEED = IDX++;

    /** _more_ */
    private static int IDX_GAIN = IDX++;

    /** _more_ */
    private static int IDX_LOSS = IDX++;

    /**  */
    private static TTLCache<String, List<String>> extraTagsCache =
        new TTLCache<String, List<String>>(60 * 60 * 1000, "Gpx extra tags");


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public GpxTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isGroup() {
        return true;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    @Override
    public String getMapInfoBubble(Request request, Entry entry) {
        return null;
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Element readXml(Entry entry) throws Exception {
        return XmlUtil.getRoot(
            getStorageManager().readSystemResource(entry.getFile()));
    }

    /**
     *
     * @param t _more_
     *  @return _more_
     */
    private String digit(int t) {
        if (t < 10) {
            return "0" + t;
        }

        return "" + t;
    }

    /**
     *
     * @param t _more_
     *  @return _more_
     */
    private String formatTime(double t) {
        String fmt = "";
        fmt += digit((int) t);
        double minutes = 60 * (t - (int) t);
        fmt += ":" + digit((int) (minutes));
        double seconds = 60 * (minutes - (int) minutes);
        fmt += ":" + digit((int) (seconds));

        return fmt;
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

        if (tag.equals("gpx.stats")) {
            double distance = (Double) entry.getDoubleValue(request,IDX_DISTANCE,
                                  Double.valueOf(0));
            double totalTime = (Double) entry.getDoubleValue(request,IDX_TOTAL_TIME,
                                   Double.valueOf(0));
            double movingTime = (Double) entry.getDoubleValue(request,IDX_MOVING_TIME,
                                    Double.valueOf(0));
            double speed = (Double) entry.getDoubleValue(request,IDX_SPEED, Double.valueOf(0));
            double gain  = (Double) entry.getDoubleValue(request,IDX_GAIN, Double.valueOf(0));
            double loss  = (Double) entry.getDoubleValue(request,IDX_LOSS, Double.valueOf(0));
            //            System.err.println("distance:" + distance +" totalTime:" + totalTime );
            if (Double.isNaN(distance) || (distance == -1)
                    || (totalTime == -1)) {
                initializeNewEntry(request, entry, NewType.NEW);
                distance = (Double) entry.getDoubleValue(request,IDX_DISTANCE,
                        Double.valueOf(0));
                totalTime = (Double) entry.getDoubleValue(request,IDX_TOTAL_TIME,
                        Double.valueOf(0));
                movingTime = (Double) entry.getDoubleValue(request,IDX_MOVING_TIME,
                        Double.valueOf(0));
                speed = (Double) entry.getDoubleValue(request,IDX_SPEED, Double.valueOf(0));
                gain  = (Double) entry.getDoubleValue(request,IDX_GAIN, Double.valueOf(0));
                loss  = (Double) entry.getDoubleValue(request,IDX_LOSS, Double.valueOf(0));
                if (distance != -1) {
                    try {
                        getEntryManager().updateEntry(request, entry);
                    } catch (Exception exc) {}
                }

            }
            if (distance == 0) {
                return "";
            }


            String        totalFmt  = formatTime(totalTime);
            String        movingFmt = formatTime(movingTime);
            StringBuilder sb        = new StringBuilder();
            sb.append(
                HtmlUtils.importCss(
                    ".gpx-stats td {padding-left:10px; padding-right:10px;}\n.gpx-stats .gpx-stats-data {font-size:150%;    font-weight: bold;}\n.gpx-stats .gpx-stats-labels td {color: gray;}"));
            sb.append(
                "<table cellpadding=0 cellspacing=0 class=\"gpx-stats\">");
            sb.append(HtmlUtils.row(HtmlUtils.cols(distance + " miles",
                    gain + " ft", speed + " mph", totalFmt,
                    movingFmt), "class=gpx-stats-data"));
            sb.append(HtmlUtils.row(HtmlUtils.cols("Distance", "Elevation Gain",
                    "Avg. Speed", "Total Time",
                    "Moving Time"), "class=gpx-stats-labels"));
            sb.append("</table>");



            return sb.toString();
        }

        return super.getWikiInclude(wikiUtil, request, originalEntry, entry,
                                    tag, props);
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
	if(!isNew(newType)) return;
	extractInfo(request, entry, true);
    }

    private static double readElevation(Element trackPoint) {
	String v = XmlUtil.getGrandChildText(trackPoint, "ele",null);
	if(v==null)
	    v =  XmlUtil.getAttribute(trackPoint,"ele","0");
	return Double.parseDouble(v);
    }

	


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param isNew _more_
     *
     * @throws Exception _more_
     */
    public void extractInfo(Request request, Entry entry, boolean isNew)
            throws Exception {

        //Don't call super
        //super.initializeNewEntry(request, entry);

        Element root     = readXml(entry);
        Element metadata = XmlUtil.findChild(root, GpxUtil.TAG_METADATA);
        Element bounds   = null;

        if (metadata != null) {
            bounds = XmlUtil.findChild(metadata, GpxUtil.TAG_BOUNDS);
        }

        if (bounds == null) {
            bounds = XmlUtil.findChild(root, GpxUtil.TAG_BOUNDS);
        }
        boolean hasBounds = false;
        if (bounds != null) {
            hasBounds = true;
            entry.setNorth(XmlUtil.getAttribute(bounds, GpxUtil.ATTR_MAXLAT,
                    Entry.NONGEO));
            entry.setSouth(XmlUtil.getAttribute(bounds, GpxUtil.ATTR_MINLAT,
                    Entry.NONGEO));
            entry.setWest(XmlUtil.getAttribute(bounds, GpxUtil.ATTR_MINLON,
                    Entry.NONGEO));
            entry.setEast(XmlUtil.getAttribute(bounds, GpxUtil.ATTR_MAXLON,
                    Entry.NONGEO));
        }

        if (isNew) {
            String name = XmlUtil.getGrandChildText(root, GpxUtil.TAG_NAME,
                              (String) null);
            if ((name == null) && (metadata != null)) {
                name = XmlUtil.getGrandChildText(metadata, GpxUtil.TAG_NAME,
                        entry.getName());

            }
            if (name != null) {
                entry.setName(name);
            }
            if (entry.getDescription().length() == 0) {
                String desc = XmlUtil.getGrandChildText(root,
                                  GpxUtil.TAG_DESC, (String) null);
                if ((desc == null) && (metadata != null)) {
                    desc = XmlUtil.getGrandChildText(metadata,
                            GpxUtil.TAG_DESC, "");

                }
                if (desc != null) {
                    entry.setDescription(desc);
                }
            }

            String keywords = XmlUtil.getGrandChildText(root,
                                  GpxUtil.TAG_KEYWORDS, null);
            if (keywords != null) {
                for (String word :
                        StringUtil.split(keywords, ",", true, true)) {
                    getMetadataManager().addMetadata(request,entry,
                            new Metadata(getRepository().getGUID(),
                                         entry.getId(),
                                         getMetadataManager().findType(ContentMetadataHandler.TYPE_KEYWORD),
                                         false, word, "", "", "", ""));
                }
            }

            String url = XmlUtil.getGrandChildText(root, GpxUtil.TAG_URL,
                             null);
            String urlName = XmlUtil.getGrandChildText(root,
                                 GpxUtil.TAG_URLNAME, "");
            if (url != null) {
                getMetadataManager().addMetadata(request,entry,
                        new Metadata(getRepository().getGUID(),
                                     entry.getId(),
                                     getMetadataManager().findType(ContentMetadataHandler.TYPE_URL), false,
                                     url, urlName, "", "", ""));

            }

            String author = XmlUtil.getGrandChildText(root,
                                GpxUtil.TAG_AUTHOR, null);
            if (author != null) {
                getMetadataManager().addMetadata(request,entry,
                        new Metadata(getRepository().getGUID(),
                                     entry.getId(),
                                     getMetadataManager().findType(ContentMetadataHandler.TYPE_AUTHOR),
                                     false, author, "", "", "", ""));

            }


            String email = XmlUtil.getGrandChildText(root, GpxUtil.TAG_EMAIL,
                               null);
            if (email != null) {
                getMetadataManager().addMetadata(request,entry,
                        new Metadata(getRepository().getGUID(),
                                     entry.getId(),
                                     getMetadataManager().findType(ContentMetadataHandler.TYPE_EMAIL),
                                     false, email, "", "", "", ""));

            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        long             minTime = -1;
        long             maxTime = -1;
        double           maxLat  = Double.NEGATIVE_INFINITY;
        double           minLat  = Double.POSITIVE_INFINITY;
        double           maxLon  = Double.NEGATIVE_INFINITY;
        double           minLon  = Double.POSITIVE_INFINITY;

        //        <time>2012-11-24T14:47:34</time>
        //This grabs all of the time nodes in the entire document
        for (Element child :
                ((List<Element>) XmlUtil.findDescendants(root,
                    GpxUtil.TAG_TIME))) {
            String time = XmlUtil.getChildText(child);
            if (time != null) {
                Date dttm = sdf.parse(time);
                minTime = (minTime == -1)
                          ? dttm.getTime()
                          : Math.min(minTime, dttm.getTime());
                maxTime = (maxTime == -1)
                          ? dttm.getTime()
                          : Math.max(maxTime, dttm.getTime());
            }
        }


        /*
  <wpt lat="39.930073" lon="-105.271828">
    <name>Video 1</name>
    <link href="http://www.trimbleoutdoors.com/DrawMediaObjectData.aspx?mediaObjectId=306850">
      <text>Raw Video</text>
      <type>video/x-m4v</type>
    </link>
    <extensions>
      <TO:mediaObjectID>306850</TO:mediaObjectID>
      <TO:ID>4334210</TO:ID>
    </extensions>
  </wpt>
*/
        for (Element child :
                ((List<Element>) XmlUtil.findChildren(root,
                    GpxUtil.TAG_WPT))) {
            Element linkNode = XmlUtil.findChild(child, GpxUtil.TAG_LINK);
            if (linkNode != null) {
                String href = XmlUtil.getAttribute(linkNode,
                                  GpxUtil.ATTR_HREF);
                Element textNode = XmlUtil.findChild(linkNode,
                                       GpxUtil.TAG_TEXT);
                Element typeNode = XmlUtil.findChild(linkNode,
                                       GpxUtil.TAG_TYPE);
            }
            maxLat = Math.max(maxLat,
                              XmlUtil.getAttribute(child, GpxUtil.ATTR_LAT,
                                  maxLat));
            minLat = Math.min(minLat,
                              XmlUtil.getAttribute(child, GpxUtil.ATTR_LAT,
                                  minLat));
            maxLon = Math.max(maxLon,
                              XmlUtil.getAttribute(child, GpxUtil.ATTR_LON,
                                  maxLon));
            minLon = Math.min(minLon,
                              XmlUtil.getAttribute(child, GpxUtil.ATTR_LON,
                                  minLon));
        }



        for (Element child :
                ((List<Element>) XmlUtil.findChildren(root,
                    GpxUtil.TAG_RTEPT))) {
            maxLat = Math.max(maxLat,
                              XmlUtil.getAttribute(child, GpxUtil.ATTR_LAT,
                                  maxLat));
            minLat = Math.min(minLat,
                              XmlUtil.getAttribute(child, GpxUtil.ATTR_LAT,
                                  minLat));
            maxLon = Math.max(maxLon,
                              XmlUtil.getAttribute(child, GpxUtil.ATTR_LON,
                                  maxLon));
            minLon = Math.min(minLon,
                              XmlUtil.getAttribute(child, GpxUtil.ATTR_LON,
                                  minLon));
        }

        for (Element child :
                ((List<Element>) XmlUtil.findDescendants(root,
                    GpxUtil.TAG_TRKPT))) {
            maxLat = Math.max(maxLat,
                              XmlUtil.getAttribute(child, GpxUtil.ATTR_LAT,
                                  maxLat));
            minLat = Math.min(minLat,
                              XmlUtil.getAttribute(child, GpxUtil.ATTR_LAT,
                                  minLat));
            maxLon = Math.max(maxLon,
                              XmlUtil.getAttribute(child, GpxUtil.ATTR_LON,
                                  maxLon));
            minLon = Math.min(minLon,
                              XmlUtil.getAttribute(child, GpxUtil.ATTR_LON,
                                  minLon));
        }

        if (minTime > 0) {
            entry.setStartDate(minTime);
            entry.setEndDate(maxTime);
        }

        if ( !hasBounds) {
            if (maxLat != Double.NEGATIVE_INFINITY) {
                entry.setNorth(maxLat);
            }
            if (minLat != Double.POSITIVE_INFINITY) {
                entry.setSouth(minLat);
            }
            if (maxLon != Double.NEGATIVE_INFINITY) {
                entry.setEast(maxLon);
            }
            if (minLon != Double.POSITIVE_INFINITY) {
                entry.setWest(minLon);
            }
        }


        Bearing bearing       = new Bearing();
        double  totalDistance = 0;
        double  elevationGain = 0;
        double  elevationLoss = 0;
        double  movingTime    = 0;
        for (Element track :
                ((List<Element>) XmlUtil.findChildren(root,
                    GpxUtil.TAG_TRK))) {
            double lastLat       = 0;
            double lastLon       = 0;
            long   lastTime      = 0;
            double lastElevation = -9999;
            for (Element trackSeg :
                    ((List<Element>) XmlUtil.findChildren(track,
                        GpxUtil.TAG_TRKSEG))) {
                for (Element trackPoint :
                        ((List<Element>) XmlUtil.findChildren(trackSeg,
                            GpxUtil.TAG_TRKPT))) {
                    double lat = XmlUtil.getAttribute(trackPoint,
                                     GpxUtil.ATTR_LAT, 0.0);
                    double lon = XmlUtil.getAttribute(trackPoint,
                                     GpxUtil.ATTR_LON, 0.0);
                    double elevation = readElevation(trackPoint);
                    if (lastElevation == -9999) {
                        lastElevation = elevation;
                    } else {
                        double delta = Math.abs(lastElevation - elevation);
                        if (delta > 5) {
                            if (elevation > lastElevation) {
                                elevationGain += delta;
                            } else {
                                elevationLoss += delta;
                            }
			    //			    System.out.println(elevation+" " + lastElevation +" " + delta+" " + elevationGain);
                            lastElevation = elevation;
                        }
                    }

                    double speed = 0;
		    //                    if (ele != null) {
		    //                        ele = "" + (Double.parseDouble(ele) * 3.28084);
		    //                    }
                    String time = XmlUtil.getGrandChildText(trackPoint,
                                      "time", (String) null);
                    long thisTime = 0;
                    if (time != null) {
                        Date dttm = sdf.parse(time);
                        thisTime = dttm.getTime();
                    }
                    if (lastLat != 0) {
                        bearing = Bearing.calculateBearing(lastLat, lastLon,
                                lat, lon, bearing);
                        double distance = 0.621371 * bearing.getDistance();
                        totalDistance += distance;
                        if (time != null) {
                            double hours = (thisTime - lastTime) / 1000.0
                                           / 60 / 60;
                            if (hours != 0) {
                                speed = distance / hours;
                            }
                            if (speed > 0.01) {
                                movingTime += hours;
                            }
                        }
                    }
                    lastLat  = lat;
                    lastLon  = lon;
                    lastTime = thisTime;
                }
            }
        }

        double hours        = 0;
        double averageSpeed = 0;
        if (movingTime > 0) {
            averageSpeed = totalDistance / movingTime;
        }

        entry.setValue(IDX_DISTANCE,
                       Double.valueOf(Math.round(100.0 * totalDistance) / 100.0));
        entry.setValue(IDX_TOTAL_TIME,
                       Double.valueOf((maxTime - minTime) / 1000.0 / 60 / 60));
        entry.setValue(IDX_MOVING_TIME, Double.valueOf(movingTime));
        entry.setValue(IDX_SPEED,
                       Double.valueOf(Math.round(100 * averageSpeed) / 100.0));
        entry.setValue(IDX_GAIN, Double.valueOf((int) (3.28084 * elevationGain)));
        entry.setValue(IDX_LOSS, Double.valueOf((int) (3.28084 * elevationLoss)));

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

        super.initializeEntryFromForm(request, entry, parent, newEntry);
        if (newEntry) {
            return;
        }
        extractInfo(request, entry, false);
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        SimpleDateFormat sdf  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String           dttm = "2010-01-27T00:39:16Z";
        sdf.parse("2010-01-27T00:39:00Z");
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param links _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void getEntryLinks(Request request, Entry entry, OutputHandler.State state, List<Link> links)
            throws Exception {
        super.getEntryLinks(request, entry, state, links);
        links.add(new Link(request.entryUrl(getRepository().URL_ENTRY_ACCESS,
                                            entry, "type", "kml"), ICON_KML,
                                                "Convert GPX to KML",
                                                    OutputType.TYPE_FILE));
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
    public Result processEntryAccess(Request request, Entry entry)
            throws Exception {
        Element gpxRoot = readXml(entry);

        Element root    = KmlUtil.kml(entry.getName());
        Element folder  = KmlUtil.folder(root, entry.getName(), true);

        for (Element child :
                ((List<Element>) XmlUtil.findChildren(gpxRoot,
                    GpxUtil.TAG_WPT))) {
            String name = XmlUtil.getGrandChildText(child, GpxUtil.TAG_NAME,
                              "");
            String desc = XmlUtil.getGrandChildText(child, GpxUtil.TAG_DESC,
                              "");
            String sym = XmlUtil.getGrandChildText(child, GpxUtil.TAG_SYM,
                             "");
            double lat = XmlUtil.getAttribute(child, GpxUtil.ATTR_LAT, 0.0);
            double lon = XmlUtil.getAttribute(child, GpxUtil.ATTR_LON, 0.0);
            KmlUtil.placemark(folder, name, desc, lat, lon, 0, null);
        }

        for (Element track :
                ((List<Element>) XmlUtil.findChildren(gpxRoot,
                    GpxUtil.TAG_TRK))) {
            for (Element trackSeg :
                    ((List<Element>) XmlUtil.findChildren(track,
                        GpxUtil.TAG_TRKSEG))) {
                List<double[]> points = new ArrayList<double[]>();
                for (Element trackPoint :
                        ((List<Element>) XmlUtil.findChildren(trackSeg,
                            GpxUtil.TAG_TRKPT))) {
                    double lat = XmlUtil.getAttribute(trackPoint,
                                     GpxUtil.ATTR_LAT, 0.0);
                    double lon = XmlUtil.getAttribute(trackPoint,
                                     GpxUtil.ATTR_LON, 0.0);
                    points.add(new double[] { lat, lon });
                }
                float[][] coords = new float[2][points.size()];
                int       cnt    = 0;
                for (double[] point : points) {
                    coords[0][cnt] = (float) point[0];
                    coords[1][cnt] = (float) point[1];
                    cnt++;
                }
                KmlUtil.placemark(folder, "track", "", coords,
                                  java.awt.Color.RED, 2);
            }
        }


        for (Element track :
                ((List<Element>) XmlUtil.findChildren(gpxRoot,
                    GpxUtil.TAG_RTE))) {
            List<double[]> points = new ArrayList<double[]>();
            for (Element trackPoint :
                    ((List<Element>) XmlUtil.findChildren(track,
                        GpxUtil.TAG_RTEPT))) {
                double lat = XmlUtil.getAttribute(trackPoint,
                                 GpxUtil.ATTR_LAT, 0.0);
                double lon = XmlUtil.getAttribute(trackPoint,
                                 GpxUtil.ATTR_LON, 0.0);
                points.add(new double[] { lat, lon });
            }
            float[][] coords = new float[2][points.size()];
            int       cnt    = 0;
            for (double[] point : points) {
                coords[0][cnt] = (float) point[0];
                coords[1][cnt] = (float) point[1];
                cnt++;
            }
            KmlUtil.placemark(folder, "track", "", coords,
                              java.awt.Color.RED, 2);
        }



        StringBuffer sb = new StringBuffer(XmlUtil.XML_HEADER);
        sb.append(XmlUtil.toString(root));

        Result result =
            new Result("GPX Entry", sb.toString().getBytes(),
                       getRepository().getMimeTypeFromSuffix(".kml"), false);
        result.setReturnFilename(IOUtil.stripExtension(entry.getName())
                                 + ".kml");

        return result;
    }


    /**
     * _more_
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

        if ( !entry.isFile()) {
            return true;
        }
        try {
            String header =
                getRepository().getPageHandler().getEntryHref(request, entry,
                    this.getEntryName(entry));
            Element              root  = readXml(entry);
            int                  cnt   = 0;
            List<TwoFacedObject> tfos  = new ArrayList<TwoFacedObject>();
            StringBuilder        extra = new StringBuilder();
            String icon =
                HtmlUtils.img(repository.getIconUrl("/icons/marker.png"));


            int markerCnt = 0;
            for (Element child :
                    ((List<Element>) XmlUtil.findChildren(root,
                        GpxUtil.TAG_WPT))) {
                if (cnt++ > 500) {
                    break;
                }
                Element linkNode = XmlUtil.findChild(child, GpxUtil.TAG_LINK);
                if (linkNode != null) {
                    String href = XmlUtil.getAttribute(linkNode,
                                      GpxUtil.ATTR_HREF);
                    Element textNode = XmlUtil.findChild(linkNode,
                                           GpxUtil.TAG_TEXT);
                    Element typeNode = XmlUtil.findChild(linkNode,
                                           GpxUtil.TAG_TYPE);

                }

                String name = XmlUtil.getGrandChildText(child,
                                  GpxUtil.TAG_NAME, "");
                String desc = XmlUtil.getGrandChildText(child,
                                  GpxUtil.TAG_DESC, "");
                String sym = XmlUtil.getGrandChildText(child,
                                 GpxUtil.TAG_SYM, "");
                double lat = XmlUtil.getAttribute(child, GpxUtil.ATTR_LAT,
                                 0.0);
                double lon = XmlUtil.getAttribute(child, GpxUtil.ATTR_LON,
                                 0.0);

                double elev = readElevation(child);

                StringBuilder info = new StringBuilder();
                info.append(header);
                info.append("<br>");
                info.append(name);
                if ( !Utils.stringDefined(desc)) {
                    info.append("<br>");
                    info.append(desc);
                }
                if (lat != 0) {
                    info.append("<br>");
                    info.append("Latitude: ");
                    info.append("" + lat);
                    info.append("<br>");
                    info.append("Longitude: ");
                    info.append("" + lon);
                }
		//                if (elev != null) {
                    info.append("<br>");
                    info.append("Elevation: ");
                    info.append(elev);
		    //                }
                String sinfo = info.toString();
                sinfo = sinfo.replaceAll("\n", "<br>");
                sinfo = sinfo.replaceAll("'", "\\'");

                sinfo = "base64:" + Utils.encodeBase64(sinfo);
                String id = entry.getId() + "-" + markerCnt;
                markerCnt++;
                map.addMarker(id, lat, lon, null, name, sinfo, entry.getId());
                String navUrl = "javascript:" + map.getVariableName()
                                + ".hiliteMarker(" + sqt(id) + ");";
                tfos.add(new TwoFacedObject(name, new String[] { id,
                        HtmlUtils.href(navUrl, icon + " " + name) }));
            }

            //            TwoFacedObject.sort(tfos);
            extra.append("<div class=\"gpx-map-links\">");
            for (TwoFacedObject tfo : tfos) {
                String[] tuple = (String[]) tfo.getId();
                extra.append(
                    HtmlUtils.div(
                        tuple[1],
                        "data-mapid=\"" + tuple[0] + "\" "
                        + HtmlUtils.cssClass("gpx-map-link")));
            }
            extra.append("</div>");
            if (tfos.size() > 0) {
                String js =
                    map.getVariableName()
                    + ".highlightMarkers('.gpx-map-links .gpx-map-link','#ffffcc');";
                extra.append(HtmlUtils.script(JQuery.ready(js)));
                map.appendExtraNav(extra.toString());
            }

            for (Element track :
                    ((List<Element>) XmlUtil.findChildren(root,
                        GpxUtil.TAG_TRK))) {
                for (Element trackSeg :
                        ((List<Element>) XmlUtil.findChildren(track,
                            GpxUtil.TAG_TRKSEG))) {
                    List<double[]> points = new ArrayList<double[]>();
                    for (Element trackPoint :
                            ((List<Element>) XmlUtil.findChildren(trackSeg,
                                GpxUtil.TAG_TRKPT))) {
                        double lat = XmlUtil.getAttribute(trackPoint,
                                         GpxUtil.ATTR_LAT, 0.0);
                        double lon = XmlUtil.getAttribute(trackPoint,
                                         GpxUtil.ATTR_LON, 0.0);
                        points.add(new double[] { lat, lon });
                    }
                    if (points.size() > 1) {
                        map.addLines(entry, entry.getId(), points, header);
                    }
                }
            }

            for (Element track :
                    ((List<Element>) XmlUtil.findChildren(root,
                        GpxUtil.TAG_RTE))) {
                List<double[]> points = new ArrayList<double[]>();
                for (Element trackPoint :
                        ((List<Element>) XmlUtil.findChildren(track,
                            GpxUtil.TAG_RTEPT))) {
                    double lat = XmlUtil.getAttribute(trackPoint,
                                     GpxUtil.ATTR_LAT, 0.0);
                    double lon = XmlUtil.getAttribute(trackPoint,
                                     GpxUtil.ATTR_LON, 0.0);
                    points.add(new double[] { lat, lon });
                }
                map.addLines(entry, entry.getId(), points, header);
            }

        } catch (Exception exc) {
            getLogManager().logError("GpxTypeHandler.addToMap:"
                                     + entry.getName(), exc);
        }

        return false;

    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param mapInfo _more_
     * @param sb _more_
     */
    @Override
    public void initMapAttrs(Entry entry, MapInfo mapInfo, StringBuilder sb) {
        super.initMapAttrs(entry, mapInfo, sb);
        String color = "blue";
        sb.append("'strokeColor':'" + color + "','strokeWidth':2");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param properties _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        return new GpxRecordFile(this, entry, new IO.Path(entry.getResource().getPath()));
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Dec 8, '18
     * @author         Enter your name here...
     */
    public static class GpxRecordFile extends CsvFile {


        /**  */
        GpxTypeHandler typeHandler;

        /**  */
        Entry entry;

        /**
         * _more_
         *
         *
         * @param typeHandler _more_
         * @param entry _more_
         * @param filename _more_
         *
         * @throws IOException _more_
         */
        public GpxRecordFile(GpxTypeHandler typeHandler, Entry entry,
                             IO.Path path)
	    throws IOException {
            super(path);
            this.typeHandler = typeHandler;
            this.entry       = entry;
        }

        /**
         * _more_
         *
         * @param buffered _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        @Override
        public InputStream doMakeInputStream(boolean buffered)
                throws Exception {

            InputStream   source   = super.doMakeInputStream(buffered);
            Element       root     = XmlUtil.getRoot(source);
            StringBuilder s        = new StringBuilder("#converted stream\n");
            boolean       didTrack = false;
            SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            List<String> extraTags = getExtraTags(this.entry);
            List<String> extra     = new ArrayList<String>();

            /*
        <extensions>
          <ns3:TrackPointExtension>
            <ns3:atemp>14.0</ns3:atemp>
            <ns3:hr>127</ns3:hr>
            <ns3:cad>0</ns3:cad>
          </ns3:TrackPointExtension>
        </extensions>
            */

            for (Element track :
                    ((List<Element>) XmlUtil.findChildren(root,
                        GpxUtil.TAG_TRK))) {
                TrackInfo trackInfo = new TrackInfo();
                for (Element trackSeg :
                        ((List<Element>) XmlUtil.findChildren(track,
                            GpxUtil.TAG_TRKSEG))) {
                    didTrack = true;
                    trackInfo.reset();
                    for (Element pt :
                            ((List<Element>) XmlUtil.findChildren(trackSeg,
                                GpxUtil.TAG_TRKPT))) {
			if (extraTags.size() > 0) {
                            extra.clear();
                            Element extensions = XmlUtil.findChild(pt,
                                                     "extensions");
                            if (extensions != null) {
                                NodeList grandchildren =
                                    XmlUtil.getGrandChildren(extensions);
                                for (String tag : extraTags) {
                                    boolean gotIt = false;
                                    for (int i = 0;
                                            i < grandchildren.getLength();
                                            i++) {
                                        Element ele =
                                            (Element) grandchildren.item(i);
                                        if (tag.equals(ele.getTagName())) {
                                            extra.add(XmlUtil.getChildText(
                                                    ele));
                                            gotIt = true;

                                            break;
                                        }
                                    }
                                    if ( !gotIt) {
                                        extra.add("NaN");
                                    }
                                }
                            }
                            if (extra.size() == 0) {
                                for (String dummy : extraTags) {
                                    extra.add("NaN");
                                }
                            }
                            //                      System.err.println("extra:" + extra);
                        }
                        double lat = XmlUtil.getAttribute(pt,
                                         GpxUtil.ATTR_LAT, 0.0);
                        double lon = XmlUtil.getAttribute(pt,
                                         GpxUtil.ATTR_LON, 0.0);
                        double elevation =readElevation(pt);
                        String time = XmlUtil.getGrandChildText(pt, "time",
                                          (String) null);
                        Date dttm = ((time == null)
                                     ? null
                                     : sdf.parse(time));

                        trackInfo.setPoint(lat, lon, elevation, dttm, time,
                                           extra, s);
                    }
                }
            }

            if ( !didTrack) {
                Element rte = XmlUtil.findChild(root, GpxUtil.TAG_RTE);
                if (rte != null) {
                    TrackInfo trackInfo = new TrackInfo();
                    for (Element pt :
                            ((List<Element>) XmlUtil.findChildren(rte,
                                GpxUtil.TAG_RTEPT))) {
                        double lat = XmlUtil.getAttribute(pt,
                                         GpxUtil.ATTR_LAT, 0.0);
                        double lon = XmlUtil.getAttribute(pt,
                                         GpxUtil.ATTR_LON, 0.0);
                        double elevation =readElevation(pt);
                        String time = XmlUtil.getGrandChildText(pt, "time",
                                          (String) null);
                        Date dttm = ((time == null)
                                     ? null
                                     : sdf.parse(time));
                        trackInfo.setPoint(lat, lon, elevation, dttm, time,
                                           null, s);
                    }
                } else {
                    TrackInfo trackInfo = new TrackInfo();
                    for (Element child :
                            ((List<Element>) XmlUtil.findChildren(root,
                                GpxUtil.TAG_WPT))) {
                        String name = XmlUtil.getGrandChildText(child,
                                          GpxUtil.TAG_NAME, "");
                        String desc = XmlUtil.getGrandChildText(child,
                                          GpxUtil.TAG_DESC, "");
                        String time = XmlUtil.getGrandChildText(child,
                                          GpxUtil.TAG_TIME, "");
                        double lat = XmlUtil.getAttribute(child,
                                         GpxUtil.ATTR_LAT, 0.0);
                        double lon = XmlUtil.getAttribute(child,
                                         GpxUtil.ATTR_LON, 0.0);

                        double elevation = readElevation(child);
                        Date dttm = ((time == null)
                                     ? null
                                     : sdf.parse(time));
                        trackInfo.setPoint(lat, lon, elevation, dttm, time,
                                           null, s);
                    }
                }
            }

            //                System.err.println(s);
            ByteArrayInputStream bais =
                new ByteArrayInputStream(s.toString().getBytes());

            return bais;

        }

        /**
         *
         * @param entry _more_
         *  @return _more_
         *
         * @throws Exception _more_
         */
        private List<String> getExtraTags(Entry entry) throws Exception {
            List<String> tags = extraTagsCache.get(entry.getId());
            if (tags == null) {
                tags = new ArrayList<String>();
                extraTagsCache.put(entry.getId(), tags);
                Element root = typeHandler.readXml(entry);
                for (Element track :
                        ((List<Element>) XmlUtil.findChildren(root,
                            GpxUtil.TAG_TRK))) {
                    for (Element trackSeg :
                            ((List<Element>) XmlUtil.findChildren(track,
                                GpxUtil.TAG_TRKSEG))) {
                        for (Element pt :
                                ((List<Element>) XmlUtil.findChildren(
                                    trackSeg, GpxUtil.TAG_TRKPT))) {
                            Element extensions = XmlUtil.findChild(pt,
                                                     "extensions");
                            if (extensions != null) {
                                NodeList grandchildren =
                                    XmlUtil.getGrandChildren(extensions);
                                for (int i = 0; i < grandchildren.getLength();
                                        i++) {
                                    Element ele =
                                        (Element) grandchildren.item(i);
                                    String tag  = ele.getTagName();
                                    String name = tag;
                                    tags.add(tag);
                                }
                            }

                            break;
                        }

                        break;
                    }

                    break;
                }
            }

            return tags;
        }


        /**
         * _more_
         *
         * @param visitInfo _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        public VisitInfo prepareToVisit(VisitInfo visitInfo)
                throws Exception {
            super.prepareToVisit(visitInfo);
            List<String> tags   = getExtraTags(this.entry);
            List<String> fields = new ArrayList<String>();

            for (String s : new String[] {
                makeField(FIELD_DATE, attrType("date"),
                          attrFormat("yyyy-MM-dd'T'HH:mm:ss")),
                makeField("point_altitude", attrType("double"),
                          attrChartable(), attrUnit("feet"),
                          attrLabel("Elevation")),
                makeField("grade", attrType("double"), attrChartable(),
                          attrUnit("%"), attrLabel("Grade")),
                makeField("elevation_gain", attrType("double"),
                          attrChartable(), attrUnit("feet"),
                          attrLabel("Elevation Gain")),
                makeField("elevation_loss", attrType("double"),
                          attrChartable(), attrUnit("feet"),
                          attrLabel("Elevation Loss")),
                makeField("speed", attrType("double"), attrChartable(),
                          attrUnit("m/h"), attrLabel("Speed")),
                makeField("total_distance", attrType("double"),
                          attrChartable(), attrUnit("miles"),
                          attrLabel("Total Distance")),
                makeField("latitude", attrType("double"),
                          attrLabel("Latitude")),
                makeField("longitude", attrType("double"),
                          attrLabel("Longitude"))
            }) {
                fields.add(s);
            }
            for (String tag : tags) {
                int index = tag.indexOf(":");
                if (index >= 0) {
                    tag = tag.substring(index + 1);
                }
                String label = Utils.makeLabel(tag);
                //A hack for garmin
                if (tag.equals("atemp")) {
                    label = "Temperature";
                } else if (tag.equals("wtemp")) {
                    label = "Temperature";
                } else if (tag.equals("hr")) {
                    label = "Heart Rate";
                } else if (tag.equals("cad")) {
                    label = "Cadence";
                }
                fields.add(makeField(tag, attrType("double"),
                                     attrLabel(label)));
            }
            putFields(fields);

            return visitInfo;
        }
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private static String sqt(String s) {
        return "'" + s + "'";
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Feb 22, '19
     * @author         Enter your name here...
     */
    public static class TrackInfo {

        /** _more_ */
        double lastLat = 0;

        /** _more_ */
        double lastLon = 0;

        /** _more_ */
        long lastTime = 0;

        /** _more_ */
        double totalDistance = 0;

        /** _more_ */
        double elevationGain = 0;

        /** _more_ */
        double elevationLoss = 0;

        /** _more_ */
        double lastElevation = 0;

        /** _more_ */
        double grade;

        /** _more_ */
        double speed;

        /** _more_ */
        double avgSpeed;

        /** _more_ */
        Bearing bearing = new Bearing();

        /** _more_ */
        List<Double> speedWindow = new ArrayList<Double>();

        /** _more_ */
        List<Double> gradeWindow = new ArrayList<Double>();

        /**
         * _more_
         */
        void reset() {
            speedWindow = new ArrayList<Double>();
            gradeWindow = new ArrayList<Double>();
        }

        /**
         * _more_
         *
         * @param lat _more_
         * @param lon _more_
         * @param elevation _more_
         * @param dttm _more_
         * @param time _more_
         * @param extra _more_
         * @param s _more_
         *
         * @throws Exception _more_
         */
        void setPoint(double lat, double lon, double elevation, Date dttm,
                      String time, List<String> extra, StringBuilder s)
                throws Exception {
            if (lastElevation != 0) {
                if (elevation > lastElevation) {
                    elevationGain += (elevation - lastElevation);
                } else {
                    elevationLoss += (lastElevation - elevation);
                }
            }


            if (lastLat != 0) {
                double distance = calculateDistance(lat, lon, elevation);
                if (gradeWindow.size() > 6) {
                    gradeWindow.remove(0);
                }
                gradeWindow.add(this.grade);
                if (dttm != null) {
                    double hours = (dttm.getTime() - lastTime) / 1000.0 / 60
                                   / 60;
                    if (hours != 0) {
                        speed = distance / hours;
                    }
                    if (speedWindow.size() > 6) {
                        speedWindow.remove(0);
                    }
                    speedWindow.add(speed);
                }
            }

            avgSpeed = Utils.getAverage(speedWindow);
            double grade = Utils.getAverage(gradeWindow);
            if (speed < 0.05) {
                avgSpeed = 0;
            }
            lastLat       = lat;
            lastLon       = lon;
            lastElevation = elevation;
            lastTime      = ((dttm == null)
                             ? 0
                             : dttm.getTime());


            s.append((time != null)
                     ? time
                     : "");
            s.append(",");
            s.append(elevation * 3.28084);
            s.append(",");
            s.append(grade);
            s.append(",");
            s.append(elevationGain);
            s.append(",");
            s.append(elevationLoss);
            s.append(",");
            s.append(Math.round(100 * avgSpeed) / 100.0);
            s.append(",");
            s.append(Math.round(100 * totalDistance) / 100.0);
            s.append(",");
            s.append(lat);
            s.append(",");
            s.append(lon);
            if (extra != null) {
                for (String v : extra) {
                    s.append(",");
                    s.append(v);
                }
            }
            s.append("\n");
        }



        /**
         * _more_
         *
         * @param lat _more_
         * @param lon _more_
         * @param elevation _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        Double calculateDistance(double lat, double lon, double elevation)
                throws Exception {
            bearing = Bearing.calculateBearing(lastLat, lastLon, lat, lon,
                    bearing);
            double distance = 0.621371 * bearing.getDistance();
            totalDistance += distance;
            if (distance > 0) {
                grade = (elevation - lastElevation) / (distance * 5280);
            } else {
                grade = 0;
            }

            return distance;
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Feb 22, '19
     * @author         Enter your name here...
     */
    public static class TrackPoint {

        /** _more_ */
        double lat;

        /** _more_ */
        double lon;

        /** _more_ */
        double elevation;

        /** _more_ */
        Date dttm;

        /**
         * _more_
         *
         * @param lat _more_
         * @param lon _more_
         * @param elevation _more_
         * @param dttm _more_
         */
        public TrackPoint(double lat, double lon, double elevation,
                          Date dttm) {
            this.lat       = lat;
            this.lon       = lon;
            this.elevation = elevation;
            this.dttm      = dttm;
        }

    }




}
