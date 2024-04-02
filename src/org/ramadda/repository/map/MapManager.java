/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.map;


import org.json.*;


import org.ramadda.plugins.map.ShapefileOutputHandler;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.RepositoryManager;
import org.ramadda.repository.RepositoryUtil;
import org.ramadda.repository.Request;
import org.ramadda.repository.Result;
import org.ramadda.repository.metadata.JpegMetadataHandler;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.metadata.MetadataHandler;
import org.ramadda.repository.output.KmlOutputHandler;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.WikiConstants;
import org.ramadda.repository.output.WikiManager;


import org.ramadda.repository.type.TypeHandler;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JQuery;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.MapProvider;
import org.ramadda.util.MapRegion;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.ramadda.util.geo.Address;
import org.ramadda.util.geo.Bounds;
import org.ramadda.util.geo.GeoUtils;
import org.ramadda.util.geo.Place;


import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.awt.geom.Rectangle2D;

import java.io.InputStream;
import java.net.URL;

import java.util.ArrayList;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;



/**
 * This class provides a variety of mapping services, e.g., map display and map form selector
 *
 * @author Jeff McWhirter
 */
@SuppressWarnings("unchecked")
public class MapManager extends RepositoryManager implements WikiConstants,
        MapProvider {

    /** _more_ */
    public static final String ADDED_IMPORTS = "initmap";

    /** _more_ */
    public static final String PROP_SCREENBIGRECTS = "screenBigRects";

    /** _more_ */
    public static final String PROP_DETAILED = "detailed";

    /** _more_ */
    public static final String PROP_INITIAL_LOCATION = "initialLocation";

    /** _more_ */
    public static final String PROP_INITIAL_BOUNDS = "initialBounds";

    /** _more_ */
    public static final String PROP_MAPS_GOOGLE_JS = "ramadda.maps.google.js";


    /** default height for GE plugin */
    public static String DFLT_EARTH_HEIGHT = "500";

    /** default width for the entries list */
    public static final int EARTH_ENTRIES_WIDTH = 150;

    /** GoogleEarth keys */
    private List<List<String>> geKeys;

    /**  */
    private StringBuilder extraJS = new StringBuilder();

    private boolean addedExtra = false;
    
    /**
     * Create a new MapManager for the repository
     *
     * @param repository  the repository
     */
    public MapManager(Repository repository) {
        super(repository);
    }




    /**
     *
     * @param s _more_
     */
    public void addExtraMapJS(String s) {
        extraJS.append(s);
        extraJS.append("\n");
    }


    /**
     *
     * @param sb _more_
     * @param width _more_
     * @param height _more_
     * @param pts _more_
     * @param props _more_
     */
    public void makeMap(StringBuilder sb, String width, String height,
                        List<double[]> pts, Hashtable<String, String> props) {
        try {
            Request tmp = getRepository().getTmpRequest();
            MapInfo mapInfo = createMap(tmp, null, width, height, false,
                                        props);
	    mapInfo.setShowFooter(false);
            int radius = (props == null)
                         ? 4
                         : Utils.getProperty(props, "radius", 4);
            for (double[] pt : pts) {
                mapInfo.addCircle("", pt[0], pt[1], radius);
            }
            mapInfo.center();
            sb.append(mapInfo.getHtml());
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * Create a map
     *
     * @param request      the Request
     * @param entry _more_
     * @param forSelection true if map used for selection
     * @param props _more_
     *
     * @return a map information holder
     *
     * @throws Exception _more_
     */
    public MapInfo createMap(Request request, Entry entry,
                             boolean forSelection,
                             Hashtable<String, String> props)
            throws Exception {
        return createMap(request, entry, MapInfo.DFLT_WIDTH,
                         MapInfo.DFLT_HEIGHT, forSelection, props);
    }


    public void addMapMarkerMetadata(Request request, Entry entry,List propList) throws Exception {
	List<Metadata> markers =
	    getMetadataManager().findMetadata(request, entry,
					      new String[]{"map_marker"}, true);
	if ((markers != null) && (markers.size() > 0)) {
	    int cnt = 1;
	    for (Metadata mtd : markers) {
		int idx = 1;
		//The order is defined in resources/mapmetadata.xml Map Marker metadata
		List<String> attrs      = new ArrayList<String>();
		String       markerDesc = mtd.getAttr(idx++);
		List<String> toks =
		    Utils.splitUpTo(mtd.getAttr(idx++), ",", 2);
		String lat        = (toks.size() > 0)
		    ? toks.get(0)
		    : "";
		String lon        = (toks.size() > 1)
		    ? toks.get(1)
		    : "";
		String markerType = mtd.getAttr(idx++);
		String markerIcon = mtd.getAttr(idx++);
		Utils.add(attrs, "metadataId", mtd.getId(),
			  "description", markerDesc, "lat", lat, "lon",
			  lon, "type", markerType, "icon", markerIcon);
		for (String attr :
			 Utils.split(mtd.getAttr(idx++), "\n", true,
				     true)) {
		    if (attr.startsWith("#")) {
			continue;
		    }
		    List<String> pair = Utils.splitUpTo(attr, "=",
							2);
		    attrs.addAll(pair);
		    if (pair.size() == 1) {
			attrs.add("");
		    }
		}
		String json = JsonUtil.mapAndQuote(attrs);
		Utils.add(propList, "marker" + cnt,
			  JsonUtil.quote("base64:"
					 + Utils.encodeBase64(json)));
		cnt++;
	    }
	}
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
                          JsonUtil.list(entry.getLatitude() + "",
                                        entry.getLongitude() + ""));
                didLoc = true;
            } else if (entry.hasAreaDefined()) {
                props.put(PROP_INITIAL_BOUNDS,
                          JsonUtil.list(entry.getNorth() + "",
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
                List<String> toks = Utils.split(userLoc, ";");
                props.put(PROP_INITIAL_BOUNDS, JsonUtil.list(toks));
                didLoc = true;
            }
        }


        if ( !didLoc) {
            String userLoc =
                (String) getSessionManager().getSessionProperty(request,
								ARG_LOCATION_LATITUDE);
            if (userLoc != null) {
                List<String> toks = Utils.split(userLoc, ";");
                props.put(PROP_INITIAL_LOCATION, JsonUtil.list(toks));
                didLoc = true;
            }
        }


        if ( !didLoc) {
            entry = getSessionManager().getLastEntry(request);
            if (entry != null) {
                if (entry.hasLocationDefined()) {
                    props.put(PROP_INITIAL_LOCATION,
                              JsonUtil.list(entry.getLatitude() + "",
                                            entry.getLongitude() + ""));


                    didLoc = true;
                } else if (entry.hasAreaDefined()) {
                    props.put(PROP_INITIAL_BOUNDS,
                              JsonUtil.list(entry.getNorth() + "",
                                            entry.getWest() + "",
                                            entry.getSouth() + "",
                                            entry.getEast() + ""));
                    didLoc = true;
                }
            }
        }

        return props;
    }

    /** _more_ */
    private String defaultMapLayer;

    /** _more_ */
    private String mapLayers;

    /**
     * _more_
     */
    @Override
    public void initAttributes() {
        super.initAttributes();
        defaultMapLayer = getRepository().getProperty(PROP_MAP_DEFAULTLAYER,
						      "osm");
        mapLayers = getRepository().getProperty(PROP_MAP_LAYERS, null);

	if(!addedExtra) {
	    addedExtra = true;
	    String esdl = getRepository().getProperty("ramadda.datacube.servers",null);
	    if(esdl!=null) {
		addExtraMapJS("MapUtils.addMapProperty('datacubeservers','" + esdl+"');\n");
	    }

	}

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getDefaultMapLayer() {
        return defaultMapLayer;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getMapLayers() {
        return mapLayers;
    }

    /**
     * Create a map
     *
     * @param request      the Request
     * @param entry _more_
     * @param width        map width
     * @param height       map height
     * @param forSelection true if map used for selection
     * @param props _more_
     *
     * @return a map information holder
     *
     * @throws Exception _more_
     */
    public MapInfo createMap(Request request, Entry entry, String width,
                             String height, boolean forSelection,
                             Hashtable<String, String> props)
	throws Exception {
        return createMap(request, entry, width, height, forSelection, false,
                         props);
    }

    /**
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processExtraJS(Request request) throws Exception {
        String extra = extraJS.toString();
        if (extra.length() > 0) {
            extra = "function initExtraMap(map) {\n" + extra + "\n}\n";
        }

        Result result = new Result(extra, Result.TYPE_JS);
        result.setCacheOk(true);

        return result;
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
    public Result processWms(Request request) throws Exception {
        String layer = request.getString("layers", "white");
        InputStream inputStream =
            IOUtil.getInputStream(
				  "/org/ramadda/repository/htdocs/images/maps/" + layer
				  + ".png", getClass());

        return getRepository().makeResult(request, "/wms/white", inputStream,
                                          "image", true);
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
    public Result processRegions(Request request) throws Exception {
        List<String> regions = new ArrayList<String>();
        for (MapRegion region : getPageHandler().getMapRegions()) {
            List<String> values = new ArrayList<String>();
            regions.add(JsonUtil.map(Utils.makeList("name",
						    JsonUtil.quote(region.getName()), "group",
						    JsonUtil.quote(region.getGroup()), "north",
						    region.getNorth() + "", "west", region.getWest() + "",
						    "south", region.getSouth() + "", "east",
						    region.getEast() + "")));
        }

        return new Result(JsonUtil.list(regions), Result.TYPE_JSON);
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
    public Result processGeocode(Request request) throws Exception {

        StringBuilder sb       = new StringBuilder();
        String        q        = request.getString("query", "").trim();
        int           max      = 50;
        String        smax     = StringUtil.findPattern(q, "max=([\\d]+)");

        boolean       doGoogle = true;
        boolean       doLocal  = true;
        boolean       doPlaces = true;
        if (q.indexOf("nogoogle") >= 0) {
            doGoogle = false;
            q        = q.replace("nogoogle", "").trim();
        }

        if (q.indexOf("nolocal") >= 0) {
            doLocal = false;
            q       = q.replace("nolocal", "").trim();
        }

        if (q.indexOf("noplaces") >= 0) {
            doPlaces = false;
            q        = q.replace("noplaces", "").trim();
        }

        if (smax != null) {
            max = Integer.parseInt(smax);
            q   = q.replace("max=" + smax, "").trim();
        }

        boolean startsWith = q.startsWith("^");
        if (startsWith) {
            q = q.substring(1);
        }
        List<String> objs   = new ArrayList<String>();
        Bounds       bounds = null;
        if (request.defined("bounds")) {
            List<String> toks = Utils.split(request.getString("bounds", ""),
                                            ",");
            bounds = new Bounds(Double.parseDouble(toks.get(0)),
                                Double.parseDouble(toks.get(1)),
                                Double.parseDouble(toks.get(2)),
                                Double.parseDouble(toks.get(3)));
        }
        HashSet<String> seen = new HashSet<String>();
        if (doGoogle) {
            Place place1 = GeoUtils.getLocationFromAddress(q, bounds);
            if (place1 != null) {
                seen.add(place1.getName());
                objs.add(JsonUtil.map(Utils.makeList("name",
						     JsonUtil.quote(place1.getName()), "latitude",
						     "" + place1.getLatitude(), "longitude",
						     "" + place1.getLongitude())));
            }
        }
        if (doLocal) {
            List<Place> places = Place.search(q, max, bounds, startsWith);
            for (Place place : places) {
                if (seen.contains(place.getName())) {
                    continue;
                }
                seen.add(place.getName());
                objs.add(JsonUtil.map(Utils.makeList("name",
						     JsonUtil.quote(place.getName()), "latitude",
						     "" + place.getLatitude(), "longitude",
						     "" + place.getLongitude())));
            }
        }

        if (doPlaces) {
            if (startsWith) {
                q = q + "%";
            }
            String encodedq = q.replaceAll(" ", "%20").replaceAll("%", "%25");
            String dbUrl =
                "https://geodesystems.com/repository/entry/show?entryid=e71b0cc7-6740-4cf5-8e4b-61bd45bf883e&db.search=Search&text="
                + encodedq + "&db.view=json&max=" + max;

            if (bounds != null) {
                dbUrl +=
                    "&"
                    + HU.arg("search.db_us_places.location_south",
                                    "" + bounds.getSouth());
                dbUrl += "&"
		    + HU.arg("search.db_us_places.location_east",
				    "" + bounds.getEast());
                dbUrl += "&"
		    + HU.arg("search.db_us_places.location_west",
				    "" + bounds.getWest());
                dbUrl +=
                    "&"
                    + HU.arg("search.db_us_places.location_north",
                                    "" + bounds.getNorth());
            }

            try {
                JSONObject json    = JsonUtil.readUrl(dbUrl);
                JSONArray  results = JsonUtil.readArray(json, "results");
                if (results != null) {
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject result = results.getJSONObject(i);
                        String name = result.get("feature_name").toString();
                        String fclass =
                            result.get("feature_class").toString();
                        String icon =
                            getRepository().getProperty("icon."
							+ fclass.toLowerCase(), (String) null);
                        String state = result.get("state_alpha").toString();
                        String county = result.get("county_name").toString();
                        List<String> toks =
                            Utils.splitUpTo(
					    result.get("location").toString(), "|", 2);
                        if (icon != null) {
                            icon = getRepository().getUrlBase() + icon;
                        }
                        objs.add(JsonUtil.map(Utils.makeList("name",
							     JsonUtil.quote(name + " (" + fclass + ") "
									    + county + ", " + state), "icon",
							     ((icon != null)
							      ? JsonUtil.quote(icon)
							      : null), "latitude", toks.get(0),
							     "longitude", toks.get(1))));
                    }
                }
            } catch (Exception exc) {
                exc.printStackTrace();
                System.err.println("Error:" + exc);

            }
        }

        sb.append(JsonUtil.map(Utils.makeList("result",
					      JsonUtil.list(objs))));

        return new Result("", sb, JsonUtil.MIMETYPE);

    }

    /**
     *
     * @param request _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public Result processGetAddress(Request request) throws Exception {

        List<String> results = new ArrayList<String>();
        if ( !request.isAnonymous()) {
            Address address =
                GeoUtils.getAddress(request.get("latitude", 0.0),
					      request.get("longitude", 0.0));
            if (address != null) {
                int idx = 0;
                results.add(JsonUtil.mapAndQuote(Utils.makeList("address",
								address.getAddress(), "city", address.getCity(),
								"county", address.getCounty(), "state",
								address.getState(), "zip", address.getPostalCode(),
								"country", address.getCountry())));
            }
        }

        return new Result(JsonUtil.list(results), Result.TYPE_JSON);
    }

    public Result processGetIsoline(Request request) throws Exception {
	boolean ok  = !request.isAnonymous();
	if(!ok) {
	    return new Result("{error:'Isoline routing not available to non logged in users'}", Result.TYPE_JSON);
	}

	String hereKey = GeoUtils.getHereKey();
	if(hereKey==null) {
	    return new Result("{error:'No routing API defined'}", Result.TYPE_JSON);
	}


	request.setReturnFilename("isoline.json");
	
	//https://isoline.router.hereapi.com/v8/isolines?transportMode=car&origin=52.51578,13.37749&range[type]=time&range[values]=300'
	List<String> points = Utils.split(request.getString("points",""),",",true,true);
	String url = HU.url("https://isoline.router.hereapi.com/v8/isolines",
			    "transportMode",request.getString("mode","car"),
			    "range[type]",request.getString("rangetype","time"),
			    "range[values]",request.getString("rangevalue","300"),
			    "apikey",  hereKey);

	//Do this here so the comma doesn't get encoded
	url+="&origin="+request.getString("latitude","") +","+   request.getString("longitude","");
	IO.Result r = IO.doGetResult(new URL(url));
	if(r.getError()) {
	    //		System.err.println(r.getResult());
	}
	return new Result("", new StringBuilder(r.getResult()), JsonUtil.MIMETYPE);
    }


    public Result processGetRoute(Request request) throws Exception {
	boolean ok = false;
	if(request.exists(ARG_ENTRYID)) {
	    Entry entry =
		(Entry) getEntryManager().getEntry(request,
						   request.getString(ARG_ENTRYID));
	    if(entry!=null) {
		ok = getAccessManager().canDoEdit(request, entry);
	    }
	}

	if(!ok) {
	    ok = !request.isAnonymous();

	}
	if(!ok) {
	    return new Result("{error:'Routing not available to non logged in users'}", Result.TYPE_JSON);
	}
	String hereKey = GeoUtils.getHereKey();
	String googleKey = GeoUtils.getGoogleKey();
	if(hereKey==null && googleKey==null) {
	    return new Result("{error:'No routing API defined'}", Result.TYPE_JSON);
	}

	String mode = request.getString("mode","car");
	List<String> points = Utils.split(request.getString("points",""),",",true,true);
	if(points.size()<4) {
	    return new Result("{error:'Incorrect number of points in routing request'}", Result.TYPE_JSON);
	}

	String provider = request.getString("provider",googleKey!=null?"google":"here");
	boolean doingGoogle = googleKey!=null && provider.equals("google");
	boolean doingHere = hereKey!=null && !doingGoogle;

	request.setReturnFilename("route.json");
	if(request.get("dosequence",false)) {
	    String args = HU.args(
			 "start",
			 points.get(0) +","+   points.get(1),
			 "end",points.get(points.size()-2) + "," +  points.get(points.size()-1),
			 "improveFor","time",
			 "departure","2014-12-09T09:30:00+01:00",
			 "mode","fastest;car;traffic:enabled",
			 "apikey",  hereKey);
	    if(points.size()>2) {
		int cnt = 1;
		for(int i=2;i<points.size()-3;i+=2) {
		    args +="&destination" + (cnt++)+"=" + points.get(i) +"," + points.get(i+1);
		}
	    }
	    String url = HU.url("https://wps.hereapi.com/v8/findsequence2")+"?"+args;
	    IO.Result r = IO.doGetResult(new URL(url));
	    if(r.getError()) {
		//		System.err.println(r.getResult());
	    }
	    return new Result("", new StringBuilder(r.getResult()), JsonUtil.MIMETYPE);
	}

	if(doingHere) {
	    String url = HU.url("https://router.hereapi.com/v8/routes",
				"transportMode",mode,
				"origin",points.get(0) +","+   points.get(1),
				"destination",
				points.get(points.size()-2) + "," +  points.get(points.size()-1),
				//"return","polyline,actions,instructions,summary,travelSummary","apikey",  hereKey);
				"return","polyline,actions,instructions","apikey",  hereKey);
	    
	    if(points.size()>2) {
		for(int i=2;i<points.size()-3;i+=2) {
		    url +="&via=" + points.get(i) +"," + points.get(i+1);
		}
	    }

	    IO.Result r = IO.doGetResult(new URL(url));
	    return new Result("", new StringBuilder(r.getResult()), JsonUtil.MIMETYPE);
	}


	if(doingGoogle) {
	    if(mode.equals("car")) mode=  "driving";
	    else if(mode.equals("bicycle")) mode=  "bicycling";
	    else if(mode.equals("pedestrian")) mode=  "walking";	    	    
	    else mode = "transit";

	    String url = HU.url("https://maps.googleapis.com/maps/api/directions/json",
				"mode",mode,
				"origin",points.get(0) +","+   points.get(1),
				"destination",
				points.get(points.size()-2) + "," +  points.get(points.size()-1),
				"key",  googleKey);

	    if(points.size()>2) {
		String waypoints="";
		for(int i=2;i<points.size()-3;i+=2) {
		    if(waypoints.length()>0) waypoints+="%7C";
		    waypoints += points.get(i) +"," + points.get(i+1);
		}
		url+="&waypoints=" + waypoints;
	    }

	    //	    System.err.println(url);
	    return request.returnStream("route.json",JsonUtil.MIMETYPE, IO.getInputStream(url));
	    //	    String json = IO.readUrl(url);
	    //	    System.err.println(json);
	    //	    return new Result(json, Result.TYPE_JSON);
	}

	return new Result("{error:'No routing service available'}", Result.TYPE_JSON);


    }
    



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param width _more_
     * @param height _more_
     * @param forSelection _more_
     * @param hidden _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public MapInfo createMap(Request request, Entry entry, String width,
                             String height, boolean forSelection,
                             boolean hidden, Hashtable<String, String> props)
	throws Exception {
        //        System.err.println("MapManager.createMap: " + width + " " + height);
        if (props == null) {
            props = new Hashtable<String, String>();
        }
        String style = props.get("style");
        props.remove("style");
        MapInfo mapInfo = new MapInfo(request, getRepository(), props, width,
                                      height, forSelection);

        if (style != null) {
            mapInfo.setStyle(style);
        }
        mapInfo.setMapHidden(hidden);


        String imageOpacity = (String) props.get("imageOpacity");
        if (imageOpacity != null) {
            mapInfo.addProperty("imageOpacity", imageOpacity);
        }
        String showOpacitySlider = (String) props.get("showOpacitySlider");
        if (showOpacitySlider != null) {
            mapInfo.addProperty("showOpacitySlider", showOpacitySlider);
        }
        String showSearch = (String) props.get("showSearch");
        if (showSearch != null) {
            mapInfo.addProperty("showSearch", "" + showSearch.equals("true"));
        }
        String displayDiv = (String) props.get("displayDiv");
        if (displayDiv != null) {
            mapInfo.addProperty("displayDiv", JsonUtil.quote(displayDiv));
        }


        String simple = (String) props.get("simple");
        if (simple != null) {
            mapInfo.addProperty("simple", "" + simple.equals("true"));
        }
        if (mapLayers != null) {
            mapInfo.addProperty("mapLayers",
                                Utils.split(mapLayers, ";", true, true));
        }

	/** for now don't do this since it adds all of the props to the map
        for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
            String key   = (String) keys.nextElement();
            String value = props.get(key);
	    if(key.indexOf(".")>=0) continue;
	    mapInfo.addProperty(key, JsonUtil.quote(value));
        }
	*/

        String mapLayer = null;
        if (entry != null) {
            List<Metadata> layers =
                getMetadataManager().findMetadata(request, entry,
						  "map_layer", true);
            if ((layers != null) && (layers.size() > 0)) {
                mapLayer = layers.get(0).getAttr1();
            }
        }


        if (mapLayer == null) {
            mapLayer = (String) props.get("defaultMapLayer");
        }
        if (mapLayer == null) {
            mapLayer = getDefaultMapLayer();
        }
        mapInfo.addProperty("defaultMapLayer", JsonUtil.quote(mapLayer));

        String key = ADDED_IMPORTS;
        if (request.getExtraProperty(key) == null) {
            mapInfo.addHtml(getHtmlImports(request));
            request.putExtraProperty(key, "");
        }

        return mapInfo;
    }


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
        String key = ADDED_IMPORTS;
        if (request.getExtraProperty(key) == null) {
            request.appendHead(getHtmlImports(request));
            //            sb.append(getHtmlImports(request));
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
     *
     * @throws Exception _more_
     */
    public String getHtmlImports(Request request) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        boolean minified = getRepository().getMinifiedOk();
	minified= false;
        if (minified) {
            HU.cssLink(sb,
                              getPageHandler().getCdnPath(OPENLAYERS_BASE_V2
							  + "/theme/default/style.mini.css"));
            sb.append("\n");
            HU.importJS(sb,
                               getPageHandler().getCdnPath(OPENLAYERS_BASE_V2
							   + "/OpenLayers.mini.js"));
            sb.append("\n");
        } else {
            HU.cssLink(sb,
                              getPageHandler().getCdnPath(OPENLAYERS_BASE_V2
							  + "/theme/default/style.css"));
            sb.append("\n");
            HU.importJS(sb,
                               getPageHandler().getCdnPath(OPENLAYERS_BASE_V2
							   + "/OpenLayers.debug.js"));
            sb.append("\n");
        }

        if (minified) {
            HU.importJS(sb, getPageHandler().getCdnPath("/min/maputils.min.js"));
            sb.append("\n");
            HU.importJS(sb, getPageHandler().getCdnPath("/min/ramaddamap.min.js"));
            sb.append("\n");
        } else {
            HU.importJS(sb,getPageHandler().getCdnPath("/maputils.js"));
            sb.append("\n");
            HU.importJS(sb,getPageHandler().getCdnPath("/ramaddamap.js"));
            sb.append("\n");
        }


        String extra = getRepository().getUrlBase() + "/map/extra/"
	    + RepositoryUtil.getHtdocsVersion() + "/extra.js";
        HU.importJS(sb, extra);

        if (minified) {
            HU.cssLink(
			      sb, getPageHandler().getCdnPath("/min/ramaddamap.min.css"));
            sb.append("\n");
        } else {
            HU.cssLink(sb,
                              getPageHandler().getCdnPath("/ramaddamap.css"));
            sb.append("\n");
        }

        return sb.toString();
    }


    /**
     * Add in the google maps api javascript call
     * @param request the request
     * @param sb  the html
     */
    private void addGoogleMapsApi(Request request, StringBuilder sb) {
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
            sb.append(HU.importJS(gmaps.toString()));
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
            for (String line : Utils.split(geAPIKeys, "\n", true, true)) {
                List<String> toks = Utils.split(line, ";", true, false);
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
    private void layoutMap(Request request, Appendable sb, MapInfo map,Hashtable props,
                           boolean showList,   int numEntries,
                           String height,
                           List<String> categories,
                           Hashtable<String, StringBuilder> catMap,
                           String mapHtml, String navTop, String extraNav)
	throws Exception {
	//        String listwidth = request.getString(WikiManager.ATTR_LISTWIDTH, "250");
	HU.open(sb,  HU.TAG_DIV,
		HU.id("mapdiv_" + map.getVariableName()));
        boolean entriesListInMap = Utils.getProperty(props, "listInMap",false);
	height = HU.makeDim(height, "px");
	StringBuilder toc = new StringBuilder();
	if(showList || entriesListInMap) {
	    toc.append(navTop);
	    boolean doToggle = /*(numEntries > 5) &&*/ (categories.size() > 1);
	    for (int catIdx = 0; catIdx < categories.size(); catIdx++) {
		String        category = categories.get(catIdx);
		StringBuilder catSB    = catMap.get(category);
		String content = HU.div(catSB.toString(),HU.style("margin-left:10px;"));
		if (doToggle) {
		    toc.append(HU.makeShowHideBlock(category, content, catIdx == 0 || numEntries<30));
		} else {
		    toc.append(HU.b(category));
		    toc.append(content);
		}
	    }
	}
	if(entriesListInMap) showList = false;
        getPageHandler().addDisplayImports(request, sb);
        int weight = 12;
        if (showList ||stringDefined(extraNav)) {
            weight -= 3;
            HU.open(sb, HU.TAG_DIV, HU.cssClass("row row-tight"));
            HU.open(sb, HU.TAG_DIV,
                           HU.cssClass("col-md-3"));
            HU.open(sb, HU.TAG_DIV, HU.cssClass("ramadda-links"));
	    HU.open(sb,  HU.TAG_DIV,
			   HU.cssClass(CSS_CLASS_EARTH_ENTRIES + ((map == null)
								  ? ""
								  : " " + map.getVariableName())) +
		    HU.style(HU.css("max-height", height,"overflow-y","auto")));
            if (!showList) {
                sb.append(extraNav);
            } else {
		sb.append(toc);
            }
            sb.append(HU.close(HU.TAG_DIV));
            sb.append(HU.close(HU.TAG_DIV));
            sb.append(HU.close(HU.TAG_DIV));
            //            sb.append("</td>");
            //            sb.append("<td align=left>");
            HU.open(sb, HU.TAG_DIV,
                           HU.cssClass("col-md-" + weight));
        }


	HU.open(sb,"div",HU.style("position:relative;"));
	sb.append(mapHtml);
	if(entriesListInMap) {	
	    String tocStyle="";
	    String w;
	    if((w=Utils.getProperty(props,"listWidth",null))!=null) {
		tocStyle+=HU.css("width",HU.makeDim(w, "px"),
				 "max-width",HU.makeDim(w, "px"));				 
	    }
	    StringBuilder tocOuter = new StringBuilder();
	    String uid =HU.getUniqueId("toc_");
	    StringBuilder contents = new StringBuilder();
	    String listHeader = Utils.getProperty(props, "listHeader","Entries");
	    String link = HU.makeShowHideBlock(contents,listHeader,toc.toString(), true,"","",null,null);
	    HU.open(tocOuter,"div",HU.attrs("class", "ramadda-earth-nav ramadda-map-toc-outer","id",uid, "style",HU.css("max-height",height)));
	    HU.div(tocOuter,link,HU.clazz("ramadda-clickable ramadda-map-toc-header"));
	    HU.open(tocOuter,"div",HU.attrs("class","ramadda-map-toc-inner","style",tocStyle));
	    tocOuter.append(contents);
	    HU.close(tocOuter,HU.TAG_DIV,HU.TAG_DIV);
	    sb.append(tocOuter);
	    HU.script(sb,"$(\"#" + uid+"\").draggable();");
	}
	HU.close(sb,HU.TAG_DIV);
        if (weight != 12) {
            HU.close(sb,HU.TAG_DIV,HU.TAG_DIV);
        }

	HU.close(sb,HU.TAG_DIV);	


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
                                 MapInfo mapInfo,boolean encodeResults)
	throws Exception {
        String bubble = makeInfoBubble(request, entry);
	//Check for the JSON based chart specification and add the display imports if needed
	if(mapInfo!=null && bubble!=null && bubble.indexOf("chartType")>=0)  {
	    repository.getPageHandler().addDisplayImports(request, mapInfo.getBuffer(),false);
	}


        if (encodeResults) {
            return encodeText(bubble);
        }

        return bubble;
    }

    public String encodeText(String s) {
	return "base64:" + Utils.encodeBase64(s);
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
    public String makeInfoBubble(Request request, Entry entry,String...extraHeader)
	throws Exception {
        String fromEntry = entry.getTypeHandler().getMapInfoBubble(request,  entry);
        if (fromEntry != null) {
            //If its not json then wikify it
            if ( !fromEntry.startsWith("{") || fromEntry.startsWith("{{")) {
                fromEntry = getWikiManager().wikifyEntry(getRepository().getAdminRequest(), entry,
							 fromEntry, false,  
							 Utils.makeHashSet(WikiConstants.WIKI_TAG_MAPENTRY,
									   WikiConstants.WIKI_TAG_MAP));
            }
	    if(extraHeader.length>0) {
		fromEntry=HU.div(HU.b(Utils.join("<br>",extraHeader)),"") +fromEntry;
	    }
            return fromEntry;
        }
        StringBuilder info    = new StringBuilder();
	HU.open(info,"div",HU.cssClass("ramadda-map-bubble"));
	if(extraHeader.length>0) {
	    info.append(HU.div(HU.b(Utils.join("<br>",extraHeader)),""));
	}
        boolean       isImage = entry.isImage();
        if (isImage) {
            int width = Utils.getDimension(request.getString(ATTR_WIDTH,
							     (String) null), 400);
            String alt = request.getString(ATTR_ALT,
                                           getEntryDisplayName(entry));
            int imageWidth =
                Utils.getDimension(request.getString(ATTR_IMAGEWIDTH,
						     (String) null), width);
            String imageClass = request.getString("imageclass",
						  (String) null);
            String extra = HU.attr(HU.ATTR_WIDTH,
                                          ((imageWidth < 0)
                                           ? ("" + -imageWidth + "%")
                                           : "" + imageWidth));
            if ((alt != null) && !alt.isEmpty()) {
                extra += " " + HU.attr(ATTR_ALT, alt);
            }
            //"slides_image"
            String image = HU.img(getRepository().getHtmlOutputHandler().getImageUrl(
										     request, entry), "", extra);
            if (request.get(WikiManager.ATTR_LINK, true)) {
                image = HU.href(
				       request.entryUrl(getRepository().URL_ENTRY_SHOW, entry),
				       image);
                /*  Maybe add this later
		    } else if (request.get(WikiManager.ATTR_LINKRESOURCE, false)) {
                    image =  HU.href(
		    entry.getTypeHandler().getEntryResourceUrl(request, entry),
		    image);
                */
            }
            image = HU.center(image);
            if (imageClass != null) {
                image = HU.div(image, HU.cssClass(imageClass));
            }


            String position = request.getString(ATTR_TEXTPOSITION, POS_BOTTOM);
            String content =  getWikiManager().getSnippet(request, entry, true,"");
            if (position.equals(POS_NONE)) {
                content = image;
            } else if (position.equals(POS_BOTTOM)) {
                content = HU.div(image,"") + content;
            } else if (position.equals(POS_TOP)) {
                content = content +  HU.div(image,"");
            } else if (position.equals(POS_RIGHT)) {
                content = HU.table(
					  HU.row(
							HU.col(image)
							+ HU.col(content), HU.attr(
												 HU.ATTR_VALIGN, "top")), HU.attr(
																		HU.ATTR_CELLPADDING, "4"));
            } else if (position.equals(POS_LEFT)) {
                content = HU.table(
					  HU.row(
							HU.col(content)
							+ HU.col(image), HU.attr(
											       HU.ATTR_VALIGN, "top")), HU.attr(
																	      HU.ATTR_CELLPADDING, "4"));
            } else {
                content = "Unknown position:" + position;
            }

            String nameString = getEntryDisplayName(entry);
            nameString = HU.href(
					HU.url(
						      request.makeUrl(getRepository().URL_ENTRY_SHOW),
						      ARG_ENTRYID, entry.getId()), nameString);

	    HU.div(info,nameString,HU.cssClass("ramadda-map-header"));
            info.append(content);
	    HU.close(info,"div");
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
            bubble = getWikiManager().wikifyEntry(getRepository().getAdminRequest(), entry, bubble,
						  false, 
						  Utils.makeHashSet(WikiConstants.WIKI_TAG_MAPENTRY,
								    WikiConstants.WIKI_TAG_MAP));


            return  bubble;
        }

        String wikiTemplate = null;
        //            getRepository().getHtmlOutputHandler().getWikiText(request,   entry);


        String description = entry.getDescription();
        if (TypeHandler.isWikiText(description)) {
            wikiTemplate = description;
        }


        if (false && wikiTemplate != null) {
            String wiki = getWikiManager().wikifyEntry(
						       request, entry, wikiTemplate, true, 
						       Utils.makeHashSet(
									 WikiConstants.WIKI_TAG_MAPENTRY,
									 WikiConstants.WIKI_TAG_MAP,
									 WikiConstants.WIKI_TAG_EARTH));
            info.append(wiki);
        } else {
            HU.sectionHeader(
				    info,
				    getPageHandler().getEntryHref(
								  request, entry,
								  HU.getIconImage("fas fa-link","style","font-size:10pt;")+" "+
								  entry.getTypeHandler().getEntryName(entry)));

	    String snippet =  getWikiManager().getSnippet(request, entry, true,null);
	    if(stringDefined(snippet)) {
		info.append(getWikiManager().wikifyEntry(request,entry,
							 WikiUtil.note(snippet)));
	    } else {
		info.append("<table class=\"formtable\">");
		StringBuilder tb = new StringBuilder();
		entry.getTypeHandler().getInnerEntryContent(entry,
							    request, null, OutputHandler.OUTPUT_HTML, true, false,false, null,null,tb);
		info.append(tb.toString());

	    }
	    List<String> urls = new ArrayList<String>();
	    getMetadataManager().getThumbnailUrls(request, entry, urls);
            if ( !isImage && (urls.size() > 0)) {
                info.append("<tr><td colspan=2>"
                            + HU.img(urls.get(0), "", " width=300 ")
                            + "</td></tr>");
            }
            info.append("</table>\n");
        }
        return info.toString();
    }


    /**
     * Get the map information
     *
     * @param request       the Request
     * @param mainEntry _more_
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
    public MapInfo getMap(Request request, Entry mainEntry,
                          List<Entry> entriesToUse, Appendable sb,
                          String width, String height, Hashtable mapProps,
                          Hashtable props)
	throws Exception {

        boolean doCategories = Utils.getProperty(props, "doCategories",
						 false);
        boolean details = request.get("mapdetails",
                                      Utils.getProperty(props, ATTR_DETAILS,
							Utils.getProperty(props,
									  ATTR_MAPDETAILS, false)));
        boolean listEntries = Utils.getProperty(props, ATTR_LISTENTRIES,
						Utils.getProperty(props,"listentries",
								  false));


        boolean linkEntries = Utils.getProperty(props, "linkEntries",false);
        boolean cbx = Utils.getProperty(props, "showCheckbox", false);
        boolean searchMarkers = Utils.getProperty(props, "showMarkersSearch",
						  false);

        boolean cbxOn        = Utils.getProperty(props, "checkboxOn", true);
        String  mapVar       = Utils.getProperty(props, ATTR_MAPVAR);
        String  selectFields = Utils.getProperty(props, ATTR_SELECTFIELDS);
        String  selectBounds = Utils.getProperty(props, ATTR_SELECTBOUNDS);
        boolean forceBounds  = true;
        String viewBounds = Utils.getProperty(props, ATTR_VIEWBOUNDS,
					      selectBounds);




        if ((viewBounds != null) && viewBounds.equals("<bounds>")) {
            viewBounds = mainEntry.getBoundsString();
        }
        if (request.get("mapsubset", false)) {
            forceBounds = false;
        }
        if (selectFields != null) {
            forceBounds = false;
        }

        boolean showExtra = Utils.getProperty(props,"showExtra",true);
        boolean hidden = Misc.equals(props.get("mapHidden"), "true");
        MapInfo map = createMap(request, mainEntry, width, height, false,
                                hidden, props);


        if (map == null) {
            return null;
        }



	if(props!=null) {
	    String geojson = (String) props.get("geojson");
	    if(stringDefined(geojson)) {
		geojson = mainEntry.getTypeHandler().applyTemplate(mainEntry, geojson, true);
		//Check for any macros not added
		if(geojson.indexOf("${")<0)  {
		    map.addGeoJsonUrl(IO.getFileTail(geojson), geojson, true, "");
		}
	    }
	    for(int i=1;true;i++) {
		String marker = Utils.getProperty(props,"marker" + i,null);
		if(marker==null) break;
		//marker1="latitude:40,longitude:-105"
		Hashtable<String,String> markerProps = new Hashtable<String,String>();
		for(String tok:Utils.split(marker,",",true,true)) {
		    List<String> pair = Utils.splitUpTo(tok,":",2);
		    if(pair.size()!=2) continue;
		    markerProps.put(pair.get(0),pair.get(1));
		}
		double  lat = Utils.getProperty(markerProps,"latitude",Double.NaN);
		double  lon = Utils.getProperty(markerProps,"longitude",Double.NaN);		
		if(Double.isNaN(lat) || Double.isNaN(lon)) continue;
		int  radius= Utils.getProperty(markerProps,"radius",10);
		String color= Utils.getProperty(markerProps,"color","blue");
		String text= Utils.getProperty(markerProps,"text","");
		map.addCircle("marker"+i,lat,lon,radius,0,"",color,text);
	    }


	}

        if (selectFields != null) {
            map.setSelectFields(selectFields);
        }
        if (selectBounds != null) {
            map.setSelectBounds(selectBounds);
        }

        if (mapVar != null) {
            mapVar = mapVar.replace("${entryid}", mainEntry.getId());
            map.setMapVar(mapVar);
        }
        if (mapProps != null) {
            map.getMapProps().putAll(mapProps);
        }

	


        for (Object key : Utils.getKeys(props)) {
            String skey      = key.toString();
            String converted = skey.replaceAll("[^a-zA-Z0-9_]", "X");
            if ( !skey.equals(converted)) {
                //              System.err.println("skipping:" + skey +" converted:" + converted);
                continue;
            }
            String v = (String) props.get(skey);
            if (v.equals("true") || v.equals("false")) {}
            else {
                try {
                    Double.parseDouble(v);
                } catch (Exception exc) {
                    v = JsonUtil.quote(v);
                }
            }
            //      System.err.println("key:" + skey+"=" + v);
            map.getMapProps().put(skey, v);
        }

        if ((entriesToUse.size() == 1)
	    && !entriesToUse.get(0).hasAreaDefined()) {
	    String zoomLevel=  mapProps==null?"12":
		Utils.getProperty(mapProps, "zoomLevel", "12");		
	    if(zoomLevel!=null && !zoomLevel.equals("default")) 
		map.getMapProps().put("zoomLevel",zoomLevel);

        }

        Hashtable theProps = Utils.makeMap(PROP_DETAILED, "" + details,
                                           PROP_SCREENBIGRECTS, "true");


        if (mapProps != null) {
            theProps.putAll(mapProps);
        }
        if (props != null) {
            theProps.putAll(props);
        }

        //xx
        addToMap(request, map, entriesToUse, theProps);




        Rectangle2D.Double bounds = null;
        if (viewBounds != null) {
            List<String> toks = Utils.split(viewBounds, ",");
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
            bounds = getEntryUtil().getBounds(entriesToUse);
        }


        boolean haveLocation = false;
        if (request.defined("map_bounds")) {
	    List<String> toks = Utils.split(request.getString("map_bounds",
							      ""), ",", true, true);
            if (toks.size() == 4) {
		haveLocation = true;
                map.addProperty(MapManager.PROP_INITIAL_BOUNDS,
                                JsonUtil.list(toks.get(0), toks.get(1),
					      toks.get(2), toks.get(3)));
            }
        } else if (request.defined("map_location")) {
            List<String> toks = Utils.split(request.getString("map_location",
							      ""), ",", true, true);
            if (toks.size() == 2) {
		haveLocation = true;
                map.addProperty(MapManager.PROP_INITIAL_LOCATION,
                                JsonUtil.list(toks.get(0), toks.get(1)));
            }
        }


        if ( !haveLocation && (props != null)
	     && ((stringDefined(props.get("mapCenter")))
		 || (stringDefined(props.get("mapBounds"))))) {
            haveLocation = true;
        }




        if ( !haveLocation) {
            map.centerOn(bounds, forceBounds);
        }

        List<String> categories = new ArrayList<String>();
        Hashtable<String, StringBuilder> catMap = new Hashtable<String,
	    StringBuilder>();
        String categoryType = request.getString("category", "type");
        int    numEntries   = 0;
	List<String> markers = new ArrayList<String>();
	String iconWidth = Utils.getProperty(props,"listIconSize",ICON_WIDTH);
        String entryIcon = (String) props.get("entryIcon");
	if(stringDefined(entryIcon)) {
	    entryIcon = getPageHandler().getIconUrl(entryIcon);
	}

        for (Entry entry : entriesToUse) {
	    addMapMarkerMetadata(request, entry, markers);
            if ( !(entry.hasLocationDefined() || entry.hasAreaDefined())) {
                continue;
            }

            String category;
            if ( !doCategories) {
                category = "";
            } else {
                if (Misc.equals(categoryType, "parent")) {
                    category = getEntryDisplayName(entry.getParentEntry());
		}  else {
                    category = entry.getTypeHandler().getCategory(entry,categoryType).getLabel().toString();
                }
            }

            StringBuilder catSB = catMap.get(category);
            if (catSB == null) {
                catMap.put(category, catSB = new StringBuilder());
                categories.add(category);
            }
	    StringBuilder line = new StringBuilder();
            String suffix = map.getMapId() + "_" + mapEntryId(entry);
            line.append(HU.open(HU.TAG_DIV,
				HU.id("block_" + suffix) + "data-mapid=\""
				+ mapEntryId(entry) + "\" "
				+ HU.cssClass(CSS_CLASS_EARTH_NAV)));
	    String iconUrl;
	    if(stringDefined(entryIcon)) {
		iconUrl = entryIcon;
	    } else {
		iconUrl = getPageHandler().getIconUrl(request,entry);
	    }
	    String entryIconImage = HU.img(iconUrl,"Click to view entry details",HU.attr("width",iconWidth));

            String navUrl = "javascript:" + map.getVariableName()
		+ ".hiliteMarker(" + sqt(Utils.makeID(mapEntryId(entry))) + ",event);";

            if (cbx) {
                String cbxId = "visible_" + suffix;
                line.append(HU.checkbox("tmp", "true", cbxOn,
						HU.id(cbxId)) + HU.space(2));
            }
	    String entryUrl = getEntryManager().getEntryURL(request, entry);
	    String link = HU.href(entryUrl,HU.getIconImage("fas fa-link",
							   "title","View entry",
							   "style","font-size:8pt;margin-right:2px;"));

	    line.append(link);
	    String name = getEntryDisplayName(entry);
	    String label =   entryIconImage+"&nbsp;"+name;
	    
	    if(linkEntries)
		line.append(HU.href(entryUrl,label));
	    else
		line.append(HU.span(label,HU.attrs("onclick",navUrl,"class","ramadda-clickable",
						    HU.ATTR_TITLE,  name+HU.NL+"Shift-click to zoom")));
            line.append(HU.close(HU.TAG_DIV));
	    catSB.append(line);
            numEntries++;
        }

        String navTop   = "";
        String searchId = "";
        if (searchMarkers) {
            searchId = "search_" + map.getMapId();
            navTop += HU.input(
				      "tmp", "", 20,
				      HU.attr("placeholder", msg(" Search text"))
				      + HU.id(searchId)) + "<br>";

        }
        if (cbx) {
            String cbxId = "visibleall_" + map.getMapId();
            navTop += HU.checkbox("tmp", "true", true,
                                         HU.id(cbxId)) + "<br>";

        }

        if (request.defined("map_layer")) {
            map.addProperty("defaultMapLayer",
                            JsonUtil.quote(request.getString("map_layer",
							     "")));
        }

	for(int i=0;i<markers.size();i+=2) {
	    map.addProperty("marker" + (i+1),markers.get(i+1));
	}

        String mapHtml = map.getHtml(false);
        if ((mapHtml.length() == 0) && (catMap.size() == 0)) {
            listEntries = false;
        }
        String extra = showExtra?map.getExtraNav():"";
        layoutMap(request, sb, map, props,listEntries, numEntries, 
                  height, categories, catMap, mapHtml, navTop, extra);

	sb.append(HU.script(map.getFinalJS().toString()));


	//        String js = map.getVariableName() + ".highlightMarkers('."
	//	    + map.getVariableName() + " .ramadda-earth-nav');";
        String js = HU.call(map.getVariableName() + ".highlightMarkers",
			    HU.squote("#mapdiv_" + map.getVariableName()));
        if (searchMarkers) {
            js += map.getVariableName() + ".initSearch(" + sqt(searchId)+ ");";
        }
        sb.append(HU.script(JQuery.ready(js)));

        return map;

    }

    public static String mapEntryId(Entry entry) {
	return mapEntryId(entry.getId());
    }


    public static String mapEntryId(String id) {
	return id.replace("-","_");
    }
    

    public String getMapResourceUrl(Request request, Entry entry) {
	return getMapResourceUrl(request, getEntryManager().getEntryResourceUrl(request, entry));
    }


    /**
       sometime we need to check if the url is a kmz and do some sort of conversion
    */
    public String getMapResourceUrl(Request request, String url) {
	return url;
    }
    


    /**
     * Add the entry to the map
     *
     * @param request      The request
     * @param map          the map information
     * @param entriesToUse the Entrys to use
     * @param props _more_
     *
     * @throws Exception  problem adding entries to map
     */
    public void addToMap(Request request, MapInfo map,
                         List<Entry> entriesToUse, Hashtable props)
	throws Exception {
        boolean detailed    = Misc.getProperty(props, PROP_DETAILED, false);
        boolean showBounds  = Utils.getProperty(props, "showBounds", true);
        boolean showMarkers = Utils.getProperty(props, "showMarkers", true);
        boolean screenBigRects = Misc.getProperty(props, PROP_SCREENBIGRECTS,
						  false);

        String entryIcon = (String) props.get("entryIcon");
	if(stringDefined(entryIcon)) {
	    entryIcon = getPageHandler().getIconUrl(entryIcon);
	}


        boolean showCameraDirection = Misc.getProperty(props,
						       "showCameraDirection", true);
        boolean useThumbnail = Misc.getProperty(props, "useThumbnail", false);


        boolean showLines    = Utils.getProperty(props, "showLines", false);

        double azimuthLength    = Utils.getProperty(props, "azimuthLength", 1.0);	

	String azimuthColor = Utils.getProperty(props, "azimuthColor", "red");
	String azimuthWidth = Utils.getProperty(props, "azimuthWidth", "2");	
        //            map.addLines(entry, "", polyLine, null);

        if ((entriesToUse.size() == 1) && detailed) {
            List<Metadata> metadataList =
                getMetadataManager().findMetadata(request,
						  entriesToUse.get(0), "map_displaymap", true);
            if ((metadataList != null) && (metadataList.size() > 0)) {
                for (Metadata metadata : metadataList) {
                    if (Utils.stringDefined(metadata.getAttr1())) {
                        Entry mapEntry =
                            (Entry) getEntryManager().getEntry(request,
							       metadata.getAttr1());
                        if (mapEntry != null) {
			    if(mapEntry.getTypeHandler().isType("geo_shapefile")) {
				String url =
				    request.entryUrl(getRepository()
						     .URL_ENTRY_SHOW, mapEntry, ARG_OUTPUT,
						     ShapefileOutputHandler.OUTPUT_GEOJSON
						     .toString(), "formap", "true");
				map.addGeoJsonUrl(mapEntry.getName(), url, true,
						  ShapefileOutputHandler.makeMapStyle(request, mapEntry));


			    } else if(mapEntry.getTypeHandler().isType("geo_geojson")) {
				String url =
				    request.entryUrl(getRepository().URL_ENTRY_GET, mapEntry).toString();
				map.addGeoJsonUrl(
						  mapEntry.getName(), url, true,"");
				
			    }
                        }
                    }
                }
            }
        }
        screenBigRects = false;
        int cnt = 0;
        if (showLines) {
            for (Entry entry : entriesToUse) {
                List<Metadata> metadataList =
                    getMetadataManager().findMetadata(request, entry,
						      MetadataHandler.TYPE_SPATIAL_POLYGON, true);

                if ((metadataList == null) || (metadataList.size() == 0)) {
                    continue;
                }
                map.addSpatialMetadata(entry, metadataList, true);

            }
        }

        for (Entry entry : entriesToUse) {
            if (entry.hasAreaDefined()) {
                cnt++;
            }
        }


        boolean          makeRectangles = cnt <= 100;
        MapProperties mapProperties  = new MapProperties(null, false);

        if (request.get(ARG_MAP_ICONSONLY, false)) {
            makeRectangles = false;
        }

        if ( !showBounds) {
            makeRectangles = false;
        }

        for (Entry entry : entriesToUse) {
            boolean addMarker = true;
            List<Metadata> metadataList =
                getMetadataManager().getMetadata(request,entry);

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

                if (showCameraDirection) {
                    for (Metadata metadata : metadataList) {

                        if (metadata.getType().equals(
						      JpegMetadataHandler.TYPE_CAMERA_DIRECTION)) {
                            double dir =
                                Double.parseDouble(metadata.getAttr1());
                            double km  = azimuthLength;
                            String kms = metadata.getAttr2();
                            if (Utils.stringDefined(kms)) {
                                km = Double.parseDouble(kms);
                            }
                            LatLonPointImpl fromPt =
                                new LatLonPointImpl(location[0], location[1]); 
                           LatLonPointImpl pt = Bearing.findPoint(fromPt,
								   dir, km, null);
                            map.addLine(entry, mapEntryId(entry), fromPt, pt,
                                        null,"strokeColor",azimuthColor,"strokeWidth",azimuthWidth);

                            break;
                        }
                    }
                }

                if (addMarker && showMarkers) {
                    if (entry.getTypeHandler().getTypeProperty("map.circle",
							       false)) {
                        map.addCircle(request, entry);
                    } else {
                        map.addMarker(request, entry, entryIcon,useThumbnail);
                    }
                }
            }
        }

    }



    public void initMapSelector(Request request, TypeHandler typeHandler,
				Entry parentEntry,Entry entry, MapInfo map)
	throws Exception {
	if(entry!=null) {
	    typeHandler.addToMapSelector(request, entry, entry, map);
	}
	List<Metadata> metadataList =
	    getMetadataManager().findMetadata(request,
					      entry!=null?entry:parentEntry, "map_selector_layer", true);
	if ((metadataList != null) && (metadataList.size() > 0)) {
	    for (Metadata metadata : metadataList) {
		if (!Utils.stringDefined(metadata.getAttr1())) continue;
		Entry mapEntry =
		    (Entry) getEntryManager().getEntry(request,
						       metadata.getAttr1());
		if (mapEntry == null) continue;
		mapEntry.getTypeHandler().addToMapSelector(request, mapEntry, entry, map);
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

