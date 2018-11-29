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

package org.ramadda.repository.map;


import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.RepositoryManager;
import org.ramadda.repository.RepositoryUtil;
import org.ramadda.repository.Request;
import org.ramadda.repository.metadata.JpegMetadataHandler;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.metadata.MetadataHandler;
import org.ramadda.repository.output.KmlOutputHandler;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.WikiConstants;
import org.ramadda.repository.output.WikiManager;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;

import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.awt.geom.Rectangle2D;

import java.util.ArrayList;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;





/**
 * This class provides a variety of mapping services, e.g., map display and map form selector
 *
 * @author Jeff McWhirter
 */
public class MapManager extends RepositoryManager implements WikiConstants {

    /** _more_ */
    public static final String PROP_INITIAL_LOCATION = "initialLocation";

    /** _more_ */
    public static final String PROP_INITIAL_BOUNDS = "initialBounds";

    /** _more_ */
    public static final String PROP_MAPS_GOOGLE_JS = "ramadda.maps.google.js";


    /** default height for GE plugin */
    public static int DFLT_EARTH_HEIGHT = 500;

    /** default width for the entries list */
    public static final int EARTH_ENTRIES_WIDTH = 150;

    /** GoogleEarth keys */
    private List<List<String>> geKeys;

    /**
     * Create a new MapManager for the repository
     *
     * @param repository  the repository
     */
    public MapManager(Repository repository) {
        super(repository);
    }


    /**
     * Should maps be shown?
     *
     * @return  true if okay to show maps
     * @deprecated
     */
    public boolean shouldShowMaps() {
        return showMaps();
    }

    /**
     * Should maps be shown?
     *
     * @return the show maps property value
     */
    public boolean showMaps() {
        return getRepository().getProperty(PROP_SHOWMAP, true);
    }





    /**
     * Create a map
     *
     * @param request      the Request
     * @param forSelection true if map used for selection
     * @param props _more_
     *
     * @return a map information holder
     */
    public MapInfo createMap(Request request, boolean forSelection,
                             Hashtable<String, String> props) {
        return createMap(request, MapInfo.DFLT_WIDTH, MapInfo.DFLT_HEIGHT,
                         forSelection, props);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Hashtable<String, String> getMapProps(Request request,
            Entry entry, Hashtable<String, String> props)
            throws Exception {
        if (props == null) {
            props = new Hashtable<String, String>();
        }
        boolean didLoc = false;
        if (entry != null) {
            if (entry.hasLocationDefined()) {
                props.put(PROP_INITIAL_LOCATION,
                          Json.list(entry.getLatitude() + "",
                                    entry.getLongitude() + ""));
                didLoc = true;
            } else if (entry.hasAreaDefined()) {
                props.put(PROP_INITIAL_BOUNDS,
                          Json.list(entry.getNorth() + "",
                                    entry.getWest() + "",
                                    entry.getSouth() + "",
                                    entry.getEast() + ""));
                didLoc = true;
            }
        }

        if ( !didLoc) {
            String userLoc =
                (String) getSessionManager().getSessionProperty(request,
                    ARG_AREA);
            if (userLoc != null) {
                List<String> toks = StringUtil.split(userLoc, ";");
                props.put(PROP_INITIAL_BOUNDS, Json.list(toks));
                didLoc = true;
            }
        }


        if ( !didLoc) {
            String userLoc =
                (String) getSessionManager().getSessionProperty(request,
                    ARG_LOCATION_LATITUDE);
            if (userLoc != null) {
                List<String> toks = StringUtil.split(userLoc, ";");
                props.put(PROP_INITIAL_LOCATION, Json.list(toks));
                didLoc = true;
            }
        }


        if ( !didLoc) {
            entry = getSessionManager().getLastEntry(request);
            if (entry != null) {
                if (entry.hasLocationDefined()) {
                    props.put(PROP_INITIAL_LOCATION,
                              Json.list(entry.getLatitude() + "",
                                        entry.getLongitude() + ""));


                    didLoc = true;
                } else if (entry.hasAreaDefined()) {
                    props.put(PROP_INITIAL_BOUNDS,
                              Json.list(entry.getNorth() + "",
                                        entry.getWest() + "",
                                        entry.getSouth() + "",
                                        entry.getEast() + ""));
                    didLoc = true;
                }
            }
        }

        return props;
    }


    /**
     * Create a map
     *
     * @param request      the Request
     * @param width        map width
     * @param height       map height
     * @param forSelection true if map used for selection
     * @param props _more_
     *
     * @return a map information holder
     */
    public MapInfo createMap(Request request, int width, int height,
                             boolean forSelection,
                             Hashtable<String, String> props) {

        //        System.err.println("MapManager.createMap: " + width + " " + height);
        MapInfo mapInfo = new MapInfo(request, getRepository(), width,
                                      height, forSelection);

        String maplayers = getRepository().getProperty(PROP_MAP_LAYERS, null);
        if (maplayers != null) {
            mapInfo.addProperty("mapLayers",
                                StringUtil.split(maplayers, ";", true, true));
        }



        if (props != null) {
            for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
                String key   = (String) keys.nextElement();
                String value = props.get(key);
                mapInfo.addProperty(key, Json.quote(value));
            }
        }


        mapInfo.addProperty(
            "defaultMapLayer",
            Json.quote(
                getRepository().getProperty(
                    PROP_MAP_DEFAULTLAYER, "google.hybrid")));

        if ( !showMaps()) {
            return mapInfo;
        }

        String key = KEY2;
        if (request.getExtraProperty(key) == null) {
            mapInfo.addHtml(getHtmlImports(request));
            request.putExtraProperty(key, "");
        }

        return mapInfo;
    }


    /** _more_ */
    private static final String KEY1 = "initmap";

    /** _more_ */
    private static final String KEY2 = "initmap";

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addMapImports(Request request, Appendable sb)
            throws Exception {
        String key = KEY1;
        if (request.getExtraProperty(key) == null) {
            sb.append(HtmlUtils.comment("map imports"));
            sb.append(getHtmlImports(request));
            request.putExtraProperty(key, "added");
        }

    }


    /** the base for the openlayers URL */

    private static final int OPENLAYERS_V2 = 2;

    /** _more_ */
    private static final int OPENLAYERS_V3 = 3;

    /** _more_ */
    private static final int OPENLAYERS_VERSION = OPENLAYERS_V2;
    //    private static final int OPENLAYERS_VERSION = OPENLAYERS_V3;

    /** _more_ */
    private static final String OPENLAYERS_BASE_V2 = "/lib/openlayers/v2";

    /** _more_ */
    private static final String OPENLAYERS_BASE_V3 = "/lib/openlayers/v3";


    /**
     * Get the HtmlImports
     * @param request the Request
     *
     * @return  the imports
     */
    private String getHtmlImports(Request request) {
        StringBuilder sb = new StringBuilder();
        if (OPENLAYERS_VERSION == OPENLAYERS_V2) {
            sb.append(
                HtmlUtils.cssLink(
                    getRepository().htdocsUrl(
                        OPENLAYERS_BASE_V2 + "/theme/default/style.css")));
            sb.append(
                HtmlUtils.importJS(
                    getRepository().htdocsUrl(
                        OPENLAYERS_BASE_V2 + "/OpenLayers.js")));
        } else {
            /*
            sb.append(
                HtmlUtils.cssLink(
                    getRepository().htdocsUrl(
                        OPENLAYERS_BASE_V3 + "/ol.css")));
            sb.append(
                HtmlUtils.importJS(
                    getRepository().htdocsUrl(
                        OPENLAYERS_BASE_V3 + "/ol.js")));
*/
            sb.append(
                HtmlUtils.cssLink(
                    "https://cdnjs.cloudflare.com/ajax/libs/openlayers/4.6.4/ol-debug.css"));
            sb.append(
                HtmlUtils.importJS(
                    "https://cdnjs.cloudflare.com/ajax/libs/openlayers/4.6.4/ol-debug.js"));

        }

        //        addGoogleMapsApi(request, sb);
        if (OPENLAYERS_VERSION == OPENLAYERS_V2) {
            sb.append(
                HtmlUtils.importJS(
                    getRepository().htdocsUrl("/ramaddamap.js")));
        } else {
            sb.append(
                HtmlUtils.importJS(
                    getRepository().htdocsUrl("/ramaddamap3.js")));
        }
        sb.append(
            HtmlUtils.cssLink(getRepository().htdocsUrl("/ramaddamap.css")));

        return sb.toString();
    }


    /**
     * Add in the google maps api javascript call
     * @param request the request
     * @param sb  the html
     */
    private void addGoogleMapsApi(Request request, StringBuilder sb) {
        String mapLayers = getRepository().getProperty(PROP_MAP_LAYERS, null);
        if ((mapLayers != null)
                && (mapLayers.toLowerCase().indexOf("google") < 0)) {
            return;
        }
        StringBuilder gmaps = new StringBuilder();
        gmaps.append(getRepository().getProperty(PROP_MAPS_GOOGLE_JS, ""));
        if ( !gmaps.toString().isEmpty()) {
            String[] keyAndOther = getGoogleMapsKey(request);
            if (keyAndOther != null) {
                if ( !keyAndOther[0].isEmpty()) {
                    gmaps.append("&key=" + keyAndOther[0]);
                }
                if (keyAndOther[1] != null) {
                    gmaps.append("&");
                    gmaps.append(keyAndOther[1]);
                }
            }
            sb.append(HtmlUtils.importJS(gmaps.toString()));
        }
    }


    /**
     * Apply the admin configuration
     *
     * @param request the Request
     *
     * @throws Exception _more_
     */
    @Override
    public void applyAdminSettings(Request request) throws Exception {
        geKeys = null;
    }

    /**
     * Is GoogleEarth plugin enabled?
     *
     * @param request  the Request
     *
     * @return true if enabled
     */
    public boolean isGoogleEarthEnabled(Request request) {
        //Exclude iphone, android and linux
        if (request.isMobile()) {
            return false;
        }
        String userAgent = request.getUserAgent("").toLowerCase();
        if (userAgent.indexOf("linux") >= 0) {
            return false;
        }

        //LOOK: We don't need the maps API key anymore
        //        return getGoogleMapsKey(request) != null;
        return true;
    }

    /**
     * Get the GoogleMaps key for the Request
     *
     * @param request the Request
     *
     * @return  a list of GE keys
     */
    public String[] getGoogleMapsKey(Request request) {
        if (geKeys == null) {
            String geAPIKeys =
                getRepository().getProperty(PROP_GOOGLEAPIKEYS, null);
            if ((geAPIKeys == null) || (geAPIKeys.trim().length() == 0)) {
                return null;
            }
            List<List<String>> tmpKeys = new ArrayList<List<String>>();
            for (String line :
                    StringUtil.split(geAPIKeys, "\n", true, true)) {
                List<String> toks = StringUtil.split(line, ";", true, false);
                if (toks.size() > 1) {
                    tmpKeys.add(toks);
                }
            }
            geKeys = tmpKeys;
        }
        String hostname         = request.getServerName();
        int    port             = request.getServerPort();
        String hostnameWithPort = hostname + ":" + port;
        for (String h : new String[] { hostnameWithPort, hostname }) {
            // System.err.println("hostname:" + hostname);
            for (List<String> tuple : geKeys) {
                String server = tuple.get(0);
                // check to see if this matches me 
                //            System.err.println("    server:" + server);
                if (server.equals("*") || hostname.endsWith(h)) {
                    String mapsKey = tuple.get(1);
                    if (tuple.size() > 2) {
                        return new String[] { mapsKey, tuple.get(2) };
                    } else {
                        return new String[] { mapsKey, null };
                    }
                }
            }
        }

        return null;
    }




    /**
     * Get the GoogleEarth plugin html
     *
     * @param request   The Request
     * @param sb        StringBuilder for passing strings
     * @param width     width of GE plugin
     * @param height    height of GE plugin
     * @param url       the KML URL
     *
     * @return  the html for the plugin
     *
     * @throws Exception  problem creating the HTML
     */
    public String getGoogleEarthPlugin(Request request, Appendable sb,
                                       int width, int height, String url)
            throws Exception {

        String[] keyAndOther = getGoogleMapsKey(request);
        //        if (keyAndOther == null) {
        //            sb.append("Google Earth is not enabled");
        //            return null;
        //       }
        boolean zoomOnClick = request.get("zoom", true);
        boolean showdetails = request.get("showdetails", true);

        String  otherOpts   = "";
        String  mapsKey     = "";
        if (keyAndOther != null) {
            if ( !keyAndOther[0].isEmpty()) {
                mapsKey = "?key=" + keyAndOther[0];
            }
            if (keyAndOther[1] != null) {
                otherOpts = ", {\"other_params\":\"" + keyAndOther[1] + "\"}";
            }
        }
        Integer currentId = (Integer) request.getExtraProperty("ge.id");
        int     nextNum   = 1;
        if (currentId != null) {
            nextNum = currentId.intValue() + 1;
        }
        request.putExtraProperty("ge.id", new Integer(nextNum));
        String id = "map3d" + nextNum;
        if (request.getExtraProperty("ge.inited") == null) {
            addGoogleEarthImports(request, sb);
            sb.append(HtmlUtils.script("google.load(\"earth\", \"1\""
                                       + otherOpts + ");"));
        }


        String style = "";
        if (width > 0) {
            style += "width:" + width + "px; ";
        }
        if (height <= 0) {
            height = DFLT_EARTH_HEIGHT;
        }
        style += "height:" + height + "px; ";

        String earthHtml =
            HtmlUtils.div("",
                          HtmlUtils.id(id) + HtmlUtils.style(style)
                          + HtmlUtils.cssClass(CSS_CLASS_EARTH_CONTAINER));
        sb.append("\n");
        sb.append(earthHtml);
        sb.append(HtmlUtils.italics(msgLabel("On click")));
        sb.append(HtmlUtils.space(2));
        sb.append(
            HtmlUtils.checkbox(
                "tmp", "true", showdetails,
                HtmlUtils.id("googleearth.showdetails")));
        sb.append("\n");
        sb.append(HtmlUtils.space(1));
        sb.append(HtmlUtils.italics(msg("Show details")));
        sb.append(HtmlUtils.space(2));
        sb.append(
            HtmlUtils.checkbox(
                "tmp", "true", zoomOnClick,
                HtmlUtils.id("googleearth.zoomonclick")));
        sb.append(HtmlUtils.space(1));
        sb.append(HtmlUtils.italics(msg("Zoom")));


        sb.append(HtmlUtils.script("var  " + id + " = new RamaddaEarth("
                                   + HtmlUtils.squote(id) + ", "
                                   + ((url == null)
                                      ? "null"
                                      : HtmlUtils.squote(url)) + ");\n"));

        return id;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addGoogleEarthImports(Request request, Appendable sb)
            throws Exception {
        if (request.getExtraProperty("ge.inited") == null) {
            request.putExtraProperty("ge.inited", "true");
            getPageHandler().addGoogleJSImport(request, sb);
            sb.append(
                HtmlUtils.importJS(
                    getRepository().htdocsUrl("/google/googleearth.js")));
            sb.append(
                HtmlUtils.importJS(
                    getRepository().htdocsUrl(
                        "/google/extensions-0.2.1.pack.js")));
        }

    }


    /**
     * Get the HTML for the entries
     *
     * @param request      the Request
     * @param entries      the Entrys
     * @param sb           StringBuilder to pass info back
     * @param width        width of GE plugin
     * @param height       height of GE plugin
     * @param includeList  true to include a list of entries
     * @param justPoints   true to just make the points
     *
     * @throws Exception problem creating the plugin
     */
    public void getGoogleEarth(Request request, List<Entry> entries,
                               Appendable sb, int width, int height,
                               boolean includeList, boolean justPoints)
            throws Exception {



        StringBuilder mapSB = new StringBuilder();

        String id = getMapManager().getGoogleEarthPlugin(request, mapSB,
                        width, height, null);

        StringBuilder js         = new StringBuilder();
        List<String>  categories = new ArrayList<String>();
        Hashtable<String, StringBuilder> catMap = new Hashtable<String,
                                                      StringBuilder>();
        String categoryType = request.getString("category", "type");

        int    kmlCnt       = 0;
        int    numEntries   = 0;
        for (Entry entry : entries) {
            String kmlUrl = KmlOutputHandler.getKmlUrl(request, entry);
            if ((kmlUrl == null)
                    && !(entry.hasLocationDefined()
                         || entry.hasAreaDefined())) {
                continue;
            }

            String category;
            if (Misc.equals(categoryType, "parent")) {
                category = entry.getParentEntry().getName();
            } else {
                category = entry.getTypeHandler().getCategory(
                    entry).getLabel().toString();
            }
            if ( !Utils.stringDefined(category)) {
                category = entry.getTypeHandler().getDescription();
            }
            StringBuilder catSB = catMap.get(category);
            if (catSB == null) {
                catMap.put(category, catSB = new StringBuilder());
                categories.add(category);
            }
            String call = id + ".entryClicked("
                          + HtmlUtils.squote(entry.getId()) + ");";
            catSB.append(
                HtmlUtils.open(
                    HtmlUtils.TAG_DIV,
                    HtmlUtils.cssClass(CSS_CLASS_EARTH_NAV)));
            boolean visible = true;
            //If there are lots of kmls then don't load all of them
            if (kmlUrl != null) {
                visible = (kmlCnt++ < 3);
            }
            //            catSB.append(
            //                "<table cellspacing=0 cellpadding=0  width=100%><tr><td>");
            catSB.append(HtmlUtils.checkbox("tmp", "true", visible,
                    HtmlUtils.style("margin:0px;padding:0px;margin-right:5px;padding-bottom:10px;")
                    + HtmlUtils.id("googleearth.visibility." + entry.getId())
                    + HtmlUtils.onMouseClick(id + ".togglePlacemarkVisible("
                        + HtmlUtils.squote(entry.getId()) + ")")));

            String navUrl  = "javascript:" + call;
            String iconUrl = getPageHandler().getIconUrl(request, entry);
            catSB.append(
                HtmlUtils.href(
                    getEntryManager().getEntryURL(request, entry),
                    HtmlUtils.img(
                        iconUrl, msg("Click to view entry details"))));
            catSB.append("&nbsp;");
            catSB.append(HtmlUtils.href(navUrl, getEntryDisplayName(entry)));
            //            catSB.append("</td>");
            /*
            catSB.append(HtmlUtils.space(2));
            catSB.append(
                HtmlUtils.href(
                    navUrl,
                    HtmlUtils.img(
                        getRepository().iconUrl(ICON_MAP_NAV),
                        "View entry"), HtmlUtils.cssClass(
                            CSS_CLASS_EARTH_LINK)));
            catSB.append("</td>");
            */
            //            catSB.append("</tr></table>");
            catSB.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
            numEntries++;

            double  lat          = entry.getSouth();
            double  lon          = entry.getEast();
            String  pointsString = "null";
            boolean hasPolygon   = false;
            List<Metadata> metadataList =
                getMetadataManager().getMetadata(entry);
            for (Metadata metadata : metadataList) {
                if ( !metadata.getType().equals(
                        MetadataHandler.TYPE_SPATIAL_POLYGON)) {
                    continue;
                }
                List<double[]> points   = new ArrayList<double[]>();
                String         s        = metadata.getAttr1();
                StringBuilder  pointsSB = new StringBuilder();
                for (String pair : StringUtil.split(s, ";", true, true)) {
                    List<String> toks = StringUtil.splitUpTo(pair, ",", 2);
                    if (toks.size() != 2) {
                        continue;
                    }
                    double polyLat = Utils.decodeLatLon(toks.get(0));
                    double polyLon = Utils.decodeLatLon(toks.get(1));
                    if (pointsSB.length() == 0) {
                        pointsSB.append("new Array(");
                    } else {
                        pointsSB.append(",");
                    }
                    pointsSB.append(polyLat);
                    pointsSB.append(",");
                    pointsSB.append(polyLon);
                }
                hasPolygon = true;
                pointsSB.append(")");
                pointsString = pointsSB.toString();
            }

            //            hasPolygon = false;
            if ((kmlUrl == null) && !hasPolygon && entry.hasAreaDefined()
                    && !justPoints) {
                pointsString = "new Array(" + entry.getNorth() + ","
                               + entry.getWest() + "," + entry.getNorth()
                               + "," + entry.getEast() + ","
                               + entry.getSouth() + "," + entry.getEast()
                               + "," + entry.getSouth() + ","
                               + entry.getWest() + "," + entry.getNorth()
                               + "," + entry.getWest() + ")";
            }

            String name = getEntryDisplayName(entry);

            name = name.replace("\r", " ");
            name = name.replace("\n", " ");
            name = name.replace("\"", "\\\"");
            name = name.replace("'", "\\'");

            String desc = HtmlUtils.img(iconUrl)
                          + getEntryManager().getEntryLink(request, entry);
            desc = desc.replace("\r", " ");
            desc = desc.replace("\n", " ");
            desc = desc.replace("\"", "\\\"");
            desc = desc.replace("'", "\\'");

            if (kmlUrl == null) {
                kmlUrl = "null";
            } else {
                kmlUrl = HtmlUtils.squote(kmlUrl);
            }

            String detailsUrl =
                HtmlUtils.url(getRepository().getUrlPath(request,
                    getRepository().URL_ENTRY_SHOW), new String[] {
                        ARG_ENTRYID,
                        entry.getId(), ARG_OUTPUT, "mapinfo" });

            String fromTime = "null";
            String toTime   = "null";
            if (entry.getCreateDate() != entry.getStartDate()) {
                fromTime = HtmlUtils.squote(
                    DateUtil.getTimeAsISO8601(entry.getStartDate()));
                if (entry.getStartDate() != entry.getEndDate()) {
                    toTime = HtmlUtils.squote(
                        DateUtil.getTimeAsISO8601(entry.getEndDate()));
                }
            }


            String infoHtml =
                cleanupInfo(request,
                            getMapManager().makeInfoBubble(request, entry,
                                true));
            js.append(
                HtmlUtils.call(
                    id + ".addPlacemark", HtmlUtils.comma(
                        HtmlUtils.squote(entry.getId()), HtmlUtils.squote(
                            name), HtmlUtils.squote(infoHtml), "" + lat, ""
                                + lon) + "," + HtmlUtils.squote(detailsUrl)
                                    + "," + HtmlUtils.squote(
                                        request.getAbsoluteUrl(
                                            iconUrl)) + "," + pointsString
                                                + "," + kmlUrl + ","
                                                    + fromTime + ","
                                                        + toTime));
            js.append("\n");
        }

        if (height <= 0) {
            height = DFLT_EARTH_HEIGHT;
        }


        String listwidth = request.getString(WikiManager.ATTR_LISTWIDTH,
                                             "20%");
        layoutMap(request, sb, null, includeList, numEntries, listwidth,
                  height, categories, catMap, mapSB.toString(), "", "");

        sb.append(HtmlUtils.script(js.toString()));

    }

    /**
     * Make the table for the entry list
     *
     *
     * @param request _more_
     * @param sb          StringBuilder to append to
     * @param map _more_
     * @param includeList flag to include the list or not
     * @param numEntries  number of entries
     * @param listwidth   width of list table element
     * @param height      height
     * @param categories  list of categories
     * @param catMap      category map
     * @param mapHtml     the map html
     * @param navTop _more_
     * @param extraNav _more_
     *
     * @throws Exception _more_
     */
    private void layoutMap(Request request, Appendable sb, MapInfo map,
                           boolean includeList, int numEntries,
                           String listwidth, int height,
                           List<String> categories,
                           Hashtable<String, StringBuilder> catMap,
                           String mapHtml, String navTop, String extraNav)
            throws Exception {

        getRepository().getWikiManager().addDisplayImports(request, sb);
        HtmlUtils.open(sb, HtmlUtils.TAG_DIV, HtmlUtils.cssClass("row"));
        int weight = 12;
        if (includeList || (extraNav.length() > 0)) {
            HtmlUtils.open(sb, HtmlUtils.TAG_DIV,
                           HtmlUtils.cssClass("col-md-3"));
            weight -= 3;
            sb.append(
                HtmlUtils.open(
                    HtmlUtils.TAG_DIV,
                    HtmlUtils.cssClass(
                        CSS_CLASS_EARTH_ENTRIES + ((map == null)
                    ? ""
                    : " " + map.getVariableName())) + HtmlUtils.style(
                        "max-height:" + height + "px; overflow-y: auto;")));

            if ( !includeList) {
                sb.append(extraNav);
            } else {
                sb.append(navTop);

                boolean doToggle = (numEntries > 5)
                                   && (categories.size() > 1);
                for (int catIdx = 0; catIdx < categories.size(); catIdx++) {
                    String        category = categories.get(catIdx);
                    StringBuilder catSB    = catMap.get(category);
                    if (doToggle) {
                        sb.append(HtmlUtils.makeShowHideBlock(category,
                                catSB.toString(), catIdx == 0));
                    } else {
                        if (categories.size() > 1) {
                            sb.append(HtmlUtils.b(category));
                            sb.append(HtmlUtils.br());
                        }
                        sb.append(catSB);
                    }
                }
            }
            sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
            sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
            //            sb.append("</td>");
            //            sb.append("<td align=left>");
        }
        HtmlUtils.open(sb, HtmlUtils.TAG_DIV,
                       HtmlUtils.cssClass("col-md-" + weight));
        sb.append(mapHtml);
        sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
        sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
        if (includeList) {
            //            sb.append("</td></tr></table>");
        }

    }

    /**
     * Make the info bubble for the map popups
     *
     * @param request   the Request
     * @param entry     the Entry
     * @param encodeResults _more_
     *
     * @return  the HTML for the popup
     *
     * @throws Exception   problem getting the entry info
     */
    public String makeInfoBubble(Request request, Entry entry,
                                 boolean encodeResults)
            throws Exception {
        String bubble = makeInfoBubble(request, entry);
        if (encodeResults) {
            return "base64:" + RepositoryUtil.encodeBase64(bubble.getBytes());
        }

        return bubble;
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
    public String makeInfoBubble(Request request, Entry entry)
            throws Exception {

        String fromEntry = entry.getTypeHandler().getMapInfoBubble(request,
                               entry);
        if (fromEntry != null) {
            fromEntry = getWikiManager().wikifyEntry(request, entry,
                    fromEntry, false, null, null,
                    new String[] { WikiConstants.WIKI_TAG_MAPENTRY,
                                   WikiConstants.WIKI_TAG_MAP });

            return getRepository().translate(request, fromEntry);
        }
        StringBuilder info    = new StringBuilder();

        boolean       isImage = entry.isImage();
        if (isImage) {
            int width = Utils.getDimension(request.getString(ATTR_WIDTH,
                            (String) null), 400);
            int height = Utils.getDimension(request.getString(ATTR_HEIGHT,
                             (String) null), 270);
            String alt = request.getString(ATTR_ALT,
                                           getEntryDisplayName(entry));
            int imageWidth =
                Utils.getDimension(request.getString(ATTR_IMAGEWIDTH,
                    (String) null), width);
            String imageClass = request.getString("imageclass",
                                    (String) null);
            String extra = HtmlUtils.attr(HtmlUtils.ATTR_WIDTH,
                                          ((imageWidth < 0)
                                           ? ("" + -imageWidth + "%")
                                           : "" + imageWidth));
            if ((alt != null) && !alt.isEmpty()) {
                extra += " " + HtmlUtils.attr(ATTR_ALT, alt);
            }
            //"slides_image"
            String image =
                HtmlUtils.img(
                    getRepository().getHtmlOutputHandler().getImageUrl(
                        request, entry), "", extra);
            if (request.get(WikiManager.ATTR_LINK, true)) {
                image = HtmlUtils.href(
                    request.entryUrl(getRepository().URL_ENTRY_SHOW, entry),
                    image);
                /*  Maybe add this later
                } else if (request.get(WikiManager.ATTR_LINKRESOURCE, false)) {
                    image =  HtmlUtils.href(
                        entry.getTypeHandler().getEntryResourceUrl(request, entry),
                        image);
                */
            }
            image = HtmlUtils.center(image);
            if (imageClass != null) {
                image = HtmlUtils.div(image, HtmlUtils.cssClass(imageClass));
            }


            String position = request.getString(ATTR_TEXTPOSITION, POS_LEFT);
            String content  = entry.getDescription();
            if (position.equals(POS_NONE)) {
                content = image;
            } else if (position.equals(POS_BOTTOM)) {
                content = image + HtmlUtils.br() + content;
            } else if (position.equals(POS_TOP)) {
                content = content + HtmlUtils.br() + image;
            } else if (position.equals(POS_RIGHT)) {
                content = HtmlUtils.table(
                    HtmlUtils.row(
                        HtmlUtils.col(image)
                        + HtmlUtils.col(content), HtmlUtils.attr(
                            HtmlUtils.ATTR_VALIGN, "top")), HtmlUtils.attr(
                                HtmlUtils.ATTR_CELLPADDING, "4"));
            } else if (position.equals(POS_LEFT)) {
                content = HtmlUtils.table(
                    HtmlUtils.row(
                        HtmlUtils.col(content)
                        + HtmlUtils.col(image), HtmlUtils.attr(
                            HtmlUtils.ATTR_VALIGN, "top")), HtmlUtils.attr(
                                HtmlUtils.ATTR_CELLPADDING, "4"));
            } else {
                content = "Unknown position:" + position;
            }

            String nameString = getEntryDisplayName(entry);
            nameString = HtmlUtils.href(
                HtmlUtils.url(
                    request.makeUrl(getRepository().URL_ENTRY_SHOW),
                    ARG_ENTRYID, entry.getId()), nameString);

            info.append(nameString);
            info.append(HtmlUtils.br());
            info.append(content);

            return info.toString();
        }


        String bubble = entry.getTypeHandler().getBubbleTemplate(request,
                            entry);
        /*
        if(bubble == null) {
            bubble = ":heading {{link}}\n";
            if(entry.getTypeHandler().isGroup()) {
                bubble += "{{tree details=\"false\" message=\"\"}}\n";
            } else {
                bubble += "{{information}}\n";
            }
            }*/


        if (bubble != null) {
            bubble = getWikiManager().wikifyEntry(request, entry, bubble,
                    false, null, null,
                    new String[] { WikiConstants.WIKI_TAG_MAPENTRY,
                                   WikiConstants.WIKI_TAG_MAP });

            return getRepository().translate(request, bubble);
        }



        String wikiTemplate =
            getRepository().getHtmlOutputHandler().getWikiText(request,
                entry);
        if (wikiTemplate != null) {
            String wiki = getWikiManager().wikifyEntry(request, entry,
                              wikiTemplate, true, null, null,
                              new String[] { WikiConstants.WIKI_TAG_MAPENTRY,
                                             WikiConstants.WIKI_TAG_MAP });
            info.append(wiki);
        } else {
            HtmlUtils.sectionHeader(
                info,
                getPageHandler().getEntryHref(
                    request, entry,
                    entry.getTypeHandler().getEntryName(entry)));

            info.append("<table class=\"formtable\">");
            info.append(entry.getTypeHandler().getInnerEntryContent(entry,
                    request, null, OutputHandler.OUTPUT_HTML, true, false,
                    false));

            List<String> urls = new ArrayList<String>();
            getMetadataManager().getThumbnailUrls(request, entry, urls);

            if ( !isImage && (urls.size() > 0)) {
                info.append("<tr><td colspan=2>"
                            + HtmlUtils.img(urls.get(0), "", " width=300 ")
                            + "</td></tr>");
            }
            info.append("</table>\n");
        }
        String s = getRepository().translate(request, info.toString());

        return s;

    }


    /**
     * Get the map information
     *
     * @param request       the Request
     * @param entriesToUse  the list of Entrys
     * @param sb            StringBuilder to pass back html
     * @param width         width of the map
     * @param height        height of the map
     * @param mapProps _more_
     * @param props _more_
     *
     * @return MapInfo (not really used)
     *
     * @throws Exception  problem creating map
     */
    public MapInfo getMap(Request request, Entry mainEntry, List<Entry> entriesToUse,
                          StringBuilder sb, int width, int height,
                          Hashtable mapProps, Hashtable props)
            throws Exception {

        boolean doCategories = Utils.getProperty(props, "doCategories", true);
        boolean details = request.get("mapdetails",
                                      Utils.getProperty(props, ATTR_DETAILS,
                                          false));
        boolean listentries = Utils.getProperty(props, ATTR_LISTENTRIES,
                                  false);
        boolean cbx          = Utils.getProperty(props, "showCheckbox",
                                   false);
        boolean search       = Utils.getProperty(props, "showSearch", false);
        boolean cbxOn        = Utils.getProperty(props, "checkboxOn", true);
        String  mapVar       = Utils.getProperty(props, ATTR_MAPVAR);
        String  selectFields = Utils.getProperty(props, ATTR_SELECTFIELDS);
        String  selectBounds = Utils.getProperty(props, ATTR_SELECTBOUNDS);
        boolean forceBounds  = true;
        String viewBounds = Utils.getProperty(props, ATTR_VIEWBOUNDS,
                                              selectBounds);

        if(viewBounds!=null && viewBounds.equals("<bounds>")) {
            viewBounds = mainEntry.getBoundsString();
        }
        if(request.get("mapsubset",false)) {
            forceBounds  = false;
        }

        MapInfo map = createMap(request, width, height, false, null);
        if (map == null) {
            return null;
        }
        if (selectFields != null) {
            map.setSelectFields(selectFields);
        }
        if (selectBounds != null) {
            map.setSelectBounds(selectBounds);
        }

        if (mapVar != null) {
            map.setMapVar(mapVar);
        }
        if (mapProps != null) {
            map.getMapProps().putAll(mapProps);
        }
        addToMap(request, map, entriesToUse, details, true);

        Rectangle2D.Double bounds = null;
        if (viewBounds != null) {
            List<String> toks = StringUtil.split(viewBounds, ",");
            if (toks.size() == 4) {
                double north = Double.parseDouble(toks.get(0));
                double west  = Double.parseDouble(toks.get(1));
                double south = Double.parseDouble(toks.get(2));
                double east  = Double.parseDouble(toks.get(3));
                bounds = new Rectangle2D.Double(west, south, east - west,
                        north - south);
            }
        }
        if (bounds == null) {
            bounds = getEntryManager().getBounds(entriesToUse);
        }
        map.centerOn(bounds, forceBounds);

        List<String> categories = new ArrayList<String>();
        Hashtable<String, StringBuilder> catMap = new Hashtable<String,
                                                      StringBuilder>();
        String categoryType = request.getString("category", "type");
        int    numEntries   = 0;
        for (Entry entry : entriesToUse) {
            if ( !(entry.hasLocationDefined() || entry.hasAreaDefined())) {
                continue;
            }

            String category;
            if ( !doCategories) {
                category = "";
            } else {
                if (Misc.equals(categoryType, "parent")) {
                    category = getEntryDisplayName(entry.getParentEntry());
                } else {
                    category = entry.getTypeHandler().getCategory(
                        entry).getLabel().toString();
                }
            }

            StringBuilder catSB = catMap.get(category);
            if (catSB == null) {
                catMap.put(category, catSB = new StringBuilder());
                categories.add(category);
            }
            catSB.append(
                HtmlUtils.open(
                    HtmlUtils.TAG_DIV,
                    "data-mapid=\"" + entry.getId() + "\" "
                    + HtmlUtils.cssClass(CSS_CLASS_EARTH_NAV)));
            String iconUrl = getPageHandler().getIconUrl(request, entry);

            String navUrl = "javascript:" + map.getVariableName()
                            + ".hiliteMarker(" + sqt(entry.getId()) + ");";

            if (cbx) {
                String cbxId = "visible_" + map.getMapId() + "_"
                               + entry.getId();
                catSB.append(HtmlUtils.checkbox("tmp", "true", cbxOn,
                        HtmlUtils.id(cbxId)) + HtmlUtils.space(2));
            }
            catSB.append(
                HtmlUtils.href(
                    getEntryManager().getEntryURL(request, entry),
                    HtmlUtils.img(
                        iconUrl, msg("Click to view entry details"))));
            catSB.append("&nbsp;");
            String label = getEntryDisplayName(entry);
            catSB.append(HtmlUtils.href(navUrl, label,
                                        HtmlUtils.attr(HtmlUtils.ATTR_TITLE,
                                            label)));
            catSB.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
            numEntries++;
        }
        String listwidth = request.getString(WikiManager.ATTR_LISTWIDTH,
                                             "250");
        String navTop   = "";
        String searchId = "";
        if (search) {
            searchId = "search_" + map.getMapId();
            navTop += HtmlUtils.input(
                "tmp", "", 20,
                HtmlUtils.attr("placeholder", msg("Search text"))
                + HtmlUtils.id(searchId)) + "<br>";

        }
        if (cbx) {
            String cbxId = "visibleall_" + map.getMapId();
            navTop += HtmlUtils.checkbox("tmp", "true", true,
                                         HtmlUtils.id(cbxId)) + "<br>";

        }

        if ((map.getHtml().length() == 0) && (catMap.size() == 0)) {
            listentries = false;
        }
        String extra = map.getExtraNav();
        layoutMap(request, sb, map, listentries, numEntries, listwidth,
                  height, categories, catMap, map.getHtml(), navTop, extra);

        String js = "highlightMarkers('." + map.getVariableName()
                    + " .ramadda-earth-nav', " + map.getVariableName()
                    + ", '#ffffcc', 'white');";

        if (search) {
            js += map.getVariableName() + ".initSearch(" + sqt(searchId)
                  + ");";
        }


        sb.append(HtmlUtils.script(JQuery.ready(js)));

        return map;

    }

    /**
     * Add the entry to the map
     *
     * @param request      The request
     * @param map          the map information
     * @param entriesToUse the Entrys to use
     * @param detailed     deatiled or not
     * @param screenBigRects     handle big rectangles
     *
     * @throws Exception  problem adding entries to map
     */
    public void addToMap(Request request, MapInfo map,
                         List<Entry> entriesToUse, boolean detailed,
                         boolean screenBigRects)
            throws Exception {
        screenBigRects = false;
        int cnt = 0;
        for (Entry entry : entriesToUse) {
            if (entry.hasAreaDefined()) {
                cnt++;
            }
        }


        boolean          makeRectangles = cnt <= 100;
        MapBoxProperties mapProperties  = new MapBoxProperties("blue", false);


        if (request.get(ARG_MAP_ICONSONLY, false)) {
            makeRectangles = false;
        }


        for (Entry entry : entriesToUse) {

            boolean addMarker = true;
            String  idBase    = entry.getId();
            List<Metadata> metadataList =
                getMetadataManager().getMetadata(entry);

            boolean rectOK = true;
            if (detailed) {
                rectOK = entry.getTypeHandler().addToMap(request, entry, map);
                if ( !rectOK) {
                    addMarker = false;
                }
            }

            if (makeRectangles) {
                boolean didMetadata = map.addSpatialMetadata(entry,
                                          metadataList);
                if (rectOK && entry.hasAreaDefined() && !didMetadata) {
                    if ( !screenBigRects
                            || (Math.abs(entry.getEast() - entry.getWest())
                                < 90)) {
                        map.addBox(entry, mapProperties);
                    }
                }
            }


            if (entry.hasLocationDefined() || entry.hasAreaDefined()) {

                double[] location;
                if (makeRectangles || !entry.hasAreaDefined()) {
                    location = entry.getLocation();
                } else {
                    location = entry.getCenter();
                }

                for (Metadata metadata : metadataList) {
                    if (metadata.getType().equals(
                            JpegMetadataHandler.TYPE_CAMERA_DIRECTION)) {
                        double dir = Double.parseDouble(metadata.getAttr1());
                        LatLonPointImpl fromPt =
                            new LatLonPointImpl(location[0], location[1]);
                        LatLonPointImpl pt = Bearing.findPoint(fromPt, dir,
                                                 0.5, null);
                        map.addLine(entry, entry.getId(), fromPt, pt, null);

                        break;
                    }
                }

                if (addMarker) {
                    if (entry.getTypeHandler().getTypeProperty("map.circle",
                            false)) {
                        map.addCircle(request, entry);
                    } else {
                        map.addMarker(request, entry);
                    }
                }
            }
        }



    }



    /**
     * _more_
     *
     * @param request _more_
     * @param infoHtml _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String cleanupInfo(Request request, String infoHtml)
            throws Exception {
        infoHtml = infoHtml.replace("\r", " ");
        infoHtml = infoHtml.replace("\n", " ");
        infoHtml = infoHtml.replace("\"", "\\\"");
        infoHtml = infoHtml.replace("'", "\\'");
        infoHtml = getRepository().translate(request, infoHtml);

        return infoHtml;
    }

    /**
     * Quote a String with double quotes (&quot;)
     *
     * @param s  the String
     *
     * @return  the quoted String
     */
    private static String qt(String s) {
        return "\"" + s + "\"";
    }

    /**
     * Quote a String with single quotes (')
     *
     * @param s  the String
     *
     * @return  the quoted String
     */
    private static String sqt(String s) {
        return "'" + s + "'";
    }



    /**
     * Create a lat/lon point string for OpenLayers
     *
     * @param lat  the latitude
     * @param lon  the longitude
     *
     * @return  the OpenLayers String
     */
    public static String llp(double lat, double lon) {
        if (lat < -90) {
            lat = -90;
        }
        if (lat > 90) {
            lat = 90;
        }
        if (lon < -180) {
            lon = -180;
        }
        if (lon > 180) {
            lon = 180;
        }

        return "new OpenLayers.LonLat(" + lon + "," + lat + ")";
    }



}
