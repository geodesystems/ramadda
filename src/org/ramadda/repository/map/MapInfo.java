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

package org.ramadda.repository.map;


import org.ramadda.repository.Entry;
import org.ramadda.repository.PageDecorator;
import org.ramadda.repository.PageHandler;
import org.ramadda.repository.Repository;
import org.ramadda.repository.RepositoryUtil;
import org.ramadda.repository.Request;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.metadata.MetadataHandler;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.MapRegion;
import org.ramadda.util.Utils;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import java.awt.geom.Rectangle2D;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;



/**
 * A MapInfo class to hold map info
 */
public class MapInfo {

    private static final HtmlUtils HU = null;

    /** default box color */
    public static final String DFLT_BOX_COLOR = "blue";

    /** default map width */
    public static final String DFLT_WIDTH = "700";

    /** default map height */
    public static final String DFLT_HEIGHT = "500";

    /** The associated repository */
    private Repository repository;

    /** the map count */
    private static int cnt = 0;

    /** the map variable name */
    private String mapVarName;

    /** _more_ */
    private String mapDivId;

    /** _more_ */
    private String mapStyle;

    /** the width */
    private String width = DFLT_WIDTH;

    /** the height */
    private String height = DFLT_HEIGHT;

    /** is the map for selection */
    private boolean forSelection = false;

    /** _more_ */
    private boolean mapHidden = false;

    /** list of map regions */
    private List<MapRegion> mapRegions = null;

    /** default map region */
    private String defaultMapRegion = null;

    /** the javascript buffer? */
    private StringBuilder jsBuffer = null;

    /** _more_ */
    private StringBuilder extraNav = new StringBuilder();

    /** right side of widget */
    private StringBuilder rightSide = new StringBuilder();

    /** _more_ */
    private String headerMessage;

    /** the html */
    private StringBuilder html = new StringBuilder();

    /** map properties */
    private Hashtable mapProps;

    /** selection label */
    private String selectionLabel;

    /** the request */
    private Request request;

    /** _more_ */
    private String selectBounds;

    /** _more_ */
    private String selectFields;


    /**
     * Create a MapInfo for the associated repository
     *
     *
     * @param request    the Request
     * @param repository the associated repository
     */
    public MapInfo(Request request, Repository repository) {
        this(request, repository, DFLT_WIDTH, DFLT_HEIGHT);
    }

    /**
     * Create a MapInfo for the associated repository
     *
     *
     * @param request    the Request
     * @param repository the associated repository
     * @param width  the width of the map
     * @param height  the height of the map
     */
    public MapInfo(Request request, Repository repository, String width,
                   String height) {
        this(request, repository, width, height, false);
    }

    /**
     * Create a MapInfo for the associated repository
     *
     *
     * @param request    the Request
     * @param repository the associated repository
     * @param width  the width of the map
     * @param height  the height of the map
     * @param forSelection  true if for selecting something
     */
    public MapInfo(Request request, Repository repository, String width,
                   String height, boolean forSelection) {
        this.request      = request;
        this.repository   = repository;

        this.mapDivId       = this.mapVarName = makeMapVar();
        this.width        = width;
        this.height       = height;
        this.forSelection = forSelection;
    }

    /**
     * _more_
     *
     * @param s _more_
     */
    public void setStyle(String s) {
        mapStyle = s;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getMapVar() {
        return this.mapVarName;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static final String makeMapVar() {
        return "ramaddaMap" + (cnt++);
    }


    /**
     * _more_
     *
     * @param hidden _more_
     */
    public void setMapHidden(boolean hidden) {
        this.mapHidden = hidden;
    }

    /**
     * _more_
     *
     * @param mapVar _more_
     */
    public void setMapVar(String mapVar) {
        this.mapDivId     = mapVar;
        this.mapVarName = mapVar.replaceAll("-", "_");
    }


    /**
     *  Set the MapVarName property.
     *
     *  @param value The new value for MapVarName
     */
    public void xsetMapVarName(String value) {
        mapVarName = value;
    }

    /**
     *  Get the MapVarName property.
     *
     *  @return The MapVarName
     */
    public String getVariableName() {
        return mapVarName;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getMapId() {
        return mapDivId;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getSelectBounds() {
        return selectBounds;
    }

    /**
     * _more_
     *
     * @param b _more_
     */
    public void setSelectBounds(String b) {
        selectBounds = b;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getSelectFields() {
        return selectFields;
    }

    /**
     * _more_
     *
     * @param b _more_
     */
    public void setSelectFields(String b) {
        selectFields = b;
    }

    /**
     * Shortcut to repository.msg
     *
     * @param s  the string
     *
     * @return  the translated message
     */
    public String msg(String s) {
        return repository.msg(s);
    }


    /**
     * Shortcut to repository.msgLabel
     *
     * @param s  the string
     *
     * @return  the label
     */
    public String msgLabel(String s) {
        return repository.msgLabel(s);
    }


    /**
     * Add the html to the output
     *
     * @param s  the string
     */
    public void addHtml(String s) {
        html.append(s);
    }

    /**
     * Add JavaSript code to the output
     *
     * @param s  the JavaSript
     */
    public void addJS(String s) {
        getJS().append(s);
    }

    /**
     * Add the right side of the widget
     *
     * @param s  the HTML
     */
    public void addRightSide(String s) {
        rightSide.append(s);
    }



    public boolean addSpatialMetadata(Entry entry,
                                      List<Metadata> metadataList) {
	return addSpatialMetadata(entry, metadataList, false);
    }



    /**
     * Add spatial metadata
     *
     * @param entry  the entry
     * @param metadataList  the list of metatdata
     *
     * @return true if we added some metadata
     */
    public boolean addSpatialMetadata(Entry entry,
                                      List<Metadata> metadataList, boolean force) {
        boolean didone = false;
        if (force || entry.getTypeHandler().shouldShowPolygonInMap()) {
            for (Metadata metadata : metadataList) {
                if (metadata.getType().equals(
                        MetadataHandler.TYPE_SPATIAL_POLYGON)) {
                    List<double[]> points = new ArrayList<double[]>();
                    Utils.parsePointString(metadata.getAttr1(), points);
                    Utils.parsePointString(metadata.getAttr2(), points);
                    Utils.parsePointString(metadata.getAttr3(), points);
                    Utils.parsePointString(metadata.getAttr4(), points);
                    Utils.parsePointString(metadata.getAttr(5), points);
                    Utils.parsePointString(metadata.getAttr(6), points);		    		    
                    this.addLines(entry, entry.getId() /* + "_polygon"*/,
                                  points, null);
                    didone = true;
                }
            }
        }

        return didone;
    }


    /**
     * Get the map div
     *
     * @param contents the contents
     *
     * @return  the div tag
     */
    private String getMapDiv(String contents) {
        StringBuilder result = new StringBuilder();
        if (headerMessage != null) {
            result.append(headerMessage);
        }
        String swidth;
	    
	if(!Utils.stringDefined(width)) {
	    swidth = "";
	} else if(width.startsWith("-")) {
	    swidth =  HU.css("width",width.substring(1) + "%");
	} else {
	    swidth =  HU.css("width",HU.makeDim(width,"px"));
	}
	
        String styles;
        if (mapHidden) {
            styles = "display:none;";
        } else {
            styles = mapStyle;
            if (styles == null) {
                styles = HU.css("height", HU.makeDim(height, "px")) + swidth;
            } else  {
		styles += HU.css("height", HU.makeDim(height,"px"))  + swidth;
	    }
        }

        String footer2 =
            HU.div("",
                          HU.cssClass("ramadda-map-footer")
                          + HU.id(mapDivId + "_footer2"));
        String popup = HU.div("", HU.cssClass("ramadda-popup")
			      + HU.id(mapDivId + "_loc_popup"));

        String readout =
            HU.div("&nbsp;",
                          HU.cssClass("ramadda-map-latlonreadout")
                          + HU.id(mapDivId + "_latlonreadout")
                          + HU.style(swidth));
	if(!Misc.equals("true", getMapProps().get("showLatLonReadout"))) readout = "";
        String footer =
            HU.div("",
                          HU.cssClass("ramadda-map-footer")
                          + HU.id(mapDivId + "_footer"));
        HU.div(result, "",
                      HU.cssClass("ramadda-map-search")
                      + HU.id(mapDivId + "_search"));
	String mapDiv =   HU.div(contents,
				 HU.cssClass("ramadda-map")
				 + HU.style(styles) + " " + HU.id(mapDivId));
	String mapHeader =   HU.div("",
				    HU.cssClass("ramadda-map-header") +HU.id(mapDivId+"_header"));
	String mapSlider =   HU.div("",
				    HU.cssClass("ramadda-map-slider") +HU.id(mapDivId+"_slider"));
	

        HU.div(result, mapHeader+mapDiv+mapSlider,
	       HU.cssClass("ramadda-map-container"));


        String url = request.getUrl();
        String label;
        if (request.get("mapdetails", false)) {
            url   = url.replace("mapdetails=true", "");
            label = "Details Off";
        } else {
            url   = url + "&mapdetails=true";
            label = "Details On";
        }

        String showDetailsLink =
            (String) getMapProps().get("showDetailsLink");

        result.append("\n");
        result.append(footer);
        result.append(HU.leftRight(readout, footer2));
        result.append(popup);
        /*
        if (Misc.equals(showDetailsLink, "true")) {
            result.append(HU.leftRight(readout,
                    HU.href(url, label)));
        } else {
            result.append(readout);
            }*/
        result.append("\n");

        return result.toString();
    }


    /**
     * Get the HTML for this map
     *
     * @return  the HTML
     */
    public String getHtml() {
        repository.getPageHandler().addToMap(request, this);
        for (PageDecorator pageDecorator :
                repository.getPluginManager().getPageDecorators()) {
            pageDecorator.addToMap(request, this);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(html);

	/*
	  For now don't decorate with the WMS legend popup
	  if (rightSide.length() > 0) {
	  sb.append("<table width=\"100%\"><tr valign=top><td>");
	  }
	*/

        sb.append(getMapDiv(""));

        /*
        if (extra.length() > 0) {
            HU.close(sb, HU.TAG_DIV);
            HU.close(sb, HU.TAG_DIV);
            }*/

	/*
        For now don't decorate with the WMS legend popup
        if (rightSide.length() > 0) {
	sb.append("</td><td width=10%>");
	sb.append(rightSide);
	sb.append("</td></tr></table>");
        }
	*/

        HU.script(sb, getFinalJS());
        sb.append("\n");
        return sb.toString();
    }


    /**
     * Get the JavaScript for this map
     *
     * @return  the JavaScript
     */
    private StringBuilder getJS() {
        if (jsBuffer == null) {
            jsBuffer = new StringBuilder();
        }

        return jsBuffer;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getExtraNav() {
        return extraNav.toString();
    }

    /**
     * _more_
     *
     * @param s _more_
     */
    public void appendExtraNav(String s) {
        extraNav.append(s);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private String getFinalJS() {
        try {
            Appendable js = Utils.makeAppendable();
            js.append("\n//map javascript\n");
            Utils.append(js, "var params = ", formatProps(), ";\n");
            Utils.append(js, "var ", mapVarName, " = new RepositoryMap(",
                         HU.squote(mapDivId), ", params);\n");
            Utils.append(js, "var theMap = ", mapVarName, ";\n");
            // TODO: why is this here?
            if ( !forSelection) {
                Utils.append(js, "theMap.initMap(", forSelection, ");\n");
            }
            js.append(getJS());
	    return js.toString();
        } catch (Exception exc) {
            throw new IllegalArgumentException(exc);
        }
    }



    /**
     * Add a property for the map
     *
     * @param name   the property name
     * @param value  the value
     */
    public void addProperty(String name, Object value) {
        getMapProps().put(name, value);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Hashtable getMapProps() {
        if (mapProps == null) {
            mapProps = new Hashtable();
        }

        return mapProps;
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Object getProperty(String key) {
        return getMapProps().get(key);
    }

    /**
     * Format the properties
     *
     * @return  the properties as a Javascript string
     */
    private String formatProps() {
        StringBuilder props = new StringBuilder("{");
        if ( !getMapProps().isEmpty()) {
            for (Enumeration<String> e = getMapProps().keys();
                    e.hasMoreElements(); ) {
                String key   = e.nextElement();
                Object value = getMapProps().get(key);
                props.append("\n");
                props.append(key);
                props.append(":");
                if (value instanceof List) {
                    props.append("[ \n");
                    List vals = (List) value;
                    for (int i = 0; i < vals.size(); i++) {
                        props.append(
                            HU.squote(vals.get(i).toString()));
                        if (i < vals.size() - 1) {
                            props.append(",");
                        }
                    }
                    props.append("\n]");
                } else {
                    props.append(value.toString());
                }
                if (e.hasMoreElements()) {
                    props.append(",");
                }
            }
            props.append("\n");
        }
        if (mapHidden) {
            props.append(",");
            props.append("mapHidden:true");
        }
        props.append("}");

        return props.toString();
    }

    /**
     * Make a selector
     *
     * @param arg  the argument
     * @param popup  true to make a popup
     * @param nwse  the north, south, east and west ids
     *
     * @return  the corresponding code
     *
     * @throws Exception _more_
     */
    public String makeSelector(String arg, boolean popup, String[] nwse)
            throws Exception {
        return makeSelector(arg, popup, nwse, "", "");
    }

    /**
     * Make a selector
     *
     * @param arg  the argument
     * @param popup  true to make a popup
     * @param nwseValues _more_
     * @param extraLeft  extra left text
     * @param extraTop  extra top text
     *
     * @return  the corresponding code
     *
     * @throws Exception _more_
     */
    public String makeSelector(String arg, boolean popup,
                               String[] nwseValues, String extraLeft,
                               String extraTop)
            throws Exception {
        return makeSelector(arg, popup, nwseValues, nwseValues, extraLeft,
                            extraTop);
    }


    /**
     * _more_
     *
     * @param arg _more_
     * @param popup _more_
     * @param nwseValues _more_
     * @param nwseView _more_
     * @param extraLeft _more_
     * @param extraTop _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String makeSelector(String arg, boolean popup,
                               String[] nwseValues, String[] nwseView,
                               String extraLeft, String extraTop)
            throws Exception {
        boolean doRegion = true;
        if (nwseValues == null) {
            nwseValues = new String[] { "", "", "", "" };
        }
        if (nwseView == null) {
            nwseView = nwseValues;
        }

        if (nwseValues.length == 2) {
            doRegion = false;
        }
        StringBuilder widget  = new StringBuilder();
        String        regions = "";
        if (doRegion) {
            regions = getRegionSelectorWidget(arg);
        }
        widget.append(getSelectorWidget(arg, nwseValues));
        StringBuilder sb        = new StringBuilder();
        String        clearLink = getSelectorClearLink(msg("Clear"));
        if (doRegion) {
            String msg1 =
                HU.italics(msg("Shift-drag to select region"));
            String msg2 =
                HU.italics(msg("Command-drag to move region"));
            sb.append(HU.leftRight(HU.SPACE+msg1+HU.SPACE+msg2, clearLink+HU.SPACE));
	    //            sb.append(HU.leftRight(msg2, clearLink));
        } else {
            sb.append(
                HU.leftRight(
                    HU.italics(msg("Click to select point")),
                    clearLink));
        }

        sb.append(getMapDiv(""));
        if ((extraLeft != null) && (extraLeft.length() > 0)) {
            widget.append(HU.br() + extraLeft);
        }

        String rightSide = null;
        String initParams = HU.squote(arg) + "," + doRegion + ","
                            + (popup
                               ? "1"
                               : "0");

        Hashtable<String, String> sessionProps =
            repository.getMapManager().getMapProps(request, null, null);


        if (Utils.stringDefined(nwseView[0])) {
            if (nwseView.length == 4) {
                addProperty(MapManager.PROP_INITIAL_BOUNDS,
                            Json.list(nwseView[0], nwseView[1], nwseView[2],
                                      nwseView[3]));
            } else {
                addProperty(MapManager.PROP_INITIAL_LOCATION,
                            Json.list(nwseView[0], nwseView[1]));
            }
        }

        getMapProps().putAll(sessionProps);

        if (popup) {
            String popupLabel = (selectionLabel != null)
		? selectionLabel
		: HU.img("fas fa-globe",
			 //			 repository.getIconUrl("/icons/map.png"),
                                    msg("Show Map"));
	    PageHandler ph = repository.getPageHandler();
	    String initCall =  getVariableName() + ".selectionPopupInit();";
	    String mapPopup =  HtmlUtils.makePopup(null, popupLabel,sb.toString(), ph.arg("my","left top"),ph.arg("at","right top-50px"),ph.arg("animate",false),ph.arg("inPlace",true),ph.arg("header",true),ph.arg("sticky",true),ph.arg("initCall",initCall));

            rightSide = HU.SPACE2 + mapPopup + HU.SPACE2 + extraTop;
        } else {
            rightSide = sb.toString();
        }

        addJS(getVariableName() + ".setSelection(" + initParams + ");\n");

        String mapStuff = HU.table(new Object[] { widget.toString(),
						  rightSide });
        StringBuilder retBuf = new StringBuilder();
        if (Utils.stringDefined(regions)) {
            retBuf.append(regions);
            HU.open(retBuf,"div",HU.attrs("id",getVariableName() + "_mapToggle"));
            retBuf.append(mapStuff);
            retBuf.append("</div>");
            // Hack to hide the maps if they haven't selected a custom region.
            addJS("if ($('#" + getVariableName()
                  + "_regions option:selected').val() != \"CUSTOM\") {"
                  + "$('#" + getVariableName()
                  + "_mapToggle').hide(); } else {" + getVariableName()
                  + ".initMap(" + forSelection + ");}\n");
            // Fire the map selection change to pick up the current map params
            addJS("$('#" + getVariableName() + "_regions').change();");
        } else {
            retBuf.append(mapStuff);
            // this wasn't done in the initial making of the JS
            if (forSelection && !popup) {
                addJS(getVariableName() + ".initMap(" + forSelection
                      + ");\n");
            }
        }
        retBuf.append(html);
        retBuf.append(HU.script(getFinalJS().toString()));
        return retBuf.toString();
    }


    /**
     * Get the selector clear link
     *
     * @param msg  name for the clear link
     *
     * @return  the link
     */
    public String getSelectorClearLink(String msg) {
        return HU.mouseClickHref(getVariableName()
				 + ".selectionClear();", msg,HU.cssClass("ramadda-highlightable"));
    }

    /**
     * _more_
     *
     *
     * @param arg _more_
     * @return _more_
     */
    public String getRegionSelectorWidget(String arg) {
        StringBuilder widget = new StringBuilder();
	//	if(mapRegions==null) mapRegions = repository.getPageHandler().getMapRegions();
        if ((mapRegions != null) && (mapRegions.size() > 0)) {
            List values = new ArrayList<String>();
            //values.add(new TwoFacedObject("Select Region", ""));
            for (MapRegion region : mapRegions) {
                String value = region.getId() + "," + region.getNorth() + ","
                               + region.getWest() + "," + region.getSouth()
                               + "," + region.getEast();
                values.add(new TwoFacedObject(region.getName(), value));
            }
            values.add(new TwoFacedObject("Custom", "CUSTOM"));
            String regionSelectId = getVariableName() + "_regions";
            widget.append(HU.hidden(arg + "_regionid", "",
                                           HU.id(getVariableName()
                                               + "_regionid")));
            widget.append(
                HU.select(
                    "mapregion", values, getDefaultMapRegion(),
                    HU.id(regionSelectId)
                    + HU.attr(
                        HU.ATTR_ONCHANGE,
                        HU.call(
                            "MapUtils.mapRegionSelected",
                            HU.squote(regionSelectId),
                            HU.squote(mapDivId)))));
        }

        return widget.toString();

    }

    /**
     * GEt the selector widget
     *
     * @param arg  the argument
     * @param nwse the N,S,E and W labels
     *
     * @return  the widget
     */
    public String getSelectorWidget(String arg, String[] nwse) {
        boolean doRegion = true;
        if (nwse == null) {
            nwse = new String[] { "", "", "", "" };
        }
        StringBuilder widget = new StringBuilder();
        if (nwse.length == 2) {
            doRegion = false;
        }

        if (doRegion) {
            widget.append(HU.makeLatLonBox(mapDivId, arg, nwse[2],
                    nwse[0], nwse[3], nwse[1]));

        } else {
            widget.append(" ");
            widget.append(msgLabel("Latitude"));
            widget.append(" ");
            widget.append(
                HU.input(
                    arg + ".latitude", nwse[0],
                    HU.SIZE_5 + " "
                    + HU.id(arg + ".latitude")) + " "
                        + msgLabel("Longitude") + " "
                        + HU.input(
                            arg + ".longitude", nwse[1],
                            HU.SIZE_5 + " "
                            + HU.id(arg + ".longitude")) + " ");

        }

        return widget.toString();
    }


    /**
     * Add a box to the map
     *
     * @param entry  the map entry
     * @param properties  the properties for the box
     *
     * @throws Exception _more_
     */
    public void addBox(Entry entry, MapBoxProperties properties)
            throws Exception {
        addBox(entry.getId(), entry.getName(),
               repository.getMapManager().makeInfoBubble(request, entry,
                   true), properties, entry.getNorth(), entry.getWest(),
                          entry.getSouth(), entry.getEast());
    }

    /**
     * Add a box to the map
     *
     * @param id  the id
     * @param boxName _more_
     * @param text _more_
     * @param llr  the bounds
     * @param properties the box properties
     */
    public void addBox(String id, String boxName, String text,
                       LatLonRect llr, MapBoxProperties properties) {
        addBox(id, boxName, text, properties, llr.getLatMax(),
               llr.getLonMin(), llr.getLatMin(), llr.getLonMax());
    }


    /**
     * Add a box to the map
     *
     * @param id  the id
     * @param boxName _more_
     * @param text _more_
     * @param properties the box properties
     * @param north  north value
     * @param west   west value
     * @param south  south value
     * @param east   east value
     */
    public void addBox(String id, String boxName, String text,
                       MapBoxProperties properties, double north,
                       double west, double south, double east) {
        String color = properties.getColor();
        String attrs = Json.map("color", (color == null)
                                         ? "null"
                                         : Json.quote(color), "selectable",
                                             "" + properties.getSelectable(),
                                             "zoomToExtent",
                                             "" + properties
                                                 .getZoomToExtent());
        getJS().append("var mapBoxAttributes = " + attrs + ";\n");
        getJS().append(
            mapVarName + ".createBox("
            + HU.comma(
                HU.squote(id),
                HU.squote(boxName.replaceAll("'", "\\\\'")),
                "" + north, "" + west, "" + south, "" + east,
                HU.squote(text), "mapBoxAttributes") + ");\n");
    }


    /**
     * Add a line to the map
     *
     *
     * @param entry _more_
     * @param id  the line id
     * @param fromPt  starting point
     * @param toPt    ending point
     * @param info _more_
     */
    public void addLine(Entry entry, String id, LatLonPointImpl fromPt,
                        LatLonPointImpl toPt, String info) {
        addLine(entry, id, fromPt.getLatitude(), fromPt.getLongitude(),
                toPt.getLatitude(), toPt.getLongitude(), info);
    }

    /**
     * Add a set of lines
     *
     *
     * @param entry _more_
     * @param id  the lines id
     * @param pts  the points
     * @param info _more_
     */
    public void addLines(Entry entry, String id, double[][] pts,
                         String info) {
        StringBuilder attrs = new StringBuilder("{");
        entry.getTypeHandler().initMapAttrs(entry, this, attrs);
        attrs.append("}");
        for (int i = 1; i < pts.length; i++) {
            addLine(entry, id, pts[i - 1][0], pts[i - 1][1], pts[i][0],
                    pts[i][1], info);
        }
    }

    /**
     * Add a set of lines
     *
     *
     * @param entry _more_
     * @param id  the lines id
     * @param pts  the points
     * @param info _more_
     */
    public void addLines(Entry entry, String id, List<double[]> pts,
                         String info) {
        if (pts.size() == 0) {
            return;
        }
        boolean anyNan = false;
        for (int i = 1; !anyNan && (i < pts.size()); i++) {
            double[] currentPoint = pts.get(i);
            anyNan = Double.isNaN(currentPoint[0])
                     || Double.isNaN(currentPoint[0]);
        }

        if (anyNan) {
            double[] lastGoodPoint = pts.get(0);
            for (int i = 1; i < pts.size(); i++) {
                double[] currentPoint = pts.get(i);
                if (Double.isNaN(currentPoint[0])
                        || Double.isNaN(currentPoint[0])) {
                    lastGoodPoint = null;

                    continue;
                }
                if (lastGoodPoint != null) {
                    addLine(entry, id, lastGoodPoint[0], lastGoodPoint[1],
                            currentPoint[0], currentPoint[1], info);
                }
                lastGoodPoint = currentPoint;
            }
        } else {
            StringBuilder attrs = new StringBuilder("{");
            entry.getTypeHandler().initMapAttrs(entry, this, attrs);
            attrs.append("}");
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < pts.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                double[] pt = pts.get(i);
                sb.append(pt[0]);
                sb.append(",");
                sb.append(pt[1]);
            }
            sb.append("]");
            String name = entry.getName().replaceAll("'", "\\\\'");
            getJS().append(mapVarName + ".addLines("
                           + HU.comma(HU.squote(id),
                                             HU.squote(name),
                                             attrs.toString(),
                                             sb.toString()) + ");\n");
        }


    }


    /**
     * Add a line
     *
     *
     * @param entry _more_
     * @param id  the line id
     * @param fromLat  starting lat
     * @param fromLon  starting lon
     * @param toLat    ending lat
     * @param toLon    ending lon
     * @param info _more_
     */
    public void addLine(Entry entry, String id, double fromLat,
                        double fromLon, double toLat, double toLon,
                        String info) {
        StringBuilder attrs = new StringBuilder("{");
        entry.getTypeHandler().initMapAttrs(entry, this, attrs);
        attrs.append("}");
        String name = entry.getName().replaceAll("'", "\\\\'");
        getJS().append(mapVarName + ".addLine("
                       + HU.comma(HU.squote(id),
                                         HU.squote(name),
                                         "" + fromLat, "" + fromLon,
                                         "" + toLat, "" + toLon,
                                         attrs.toString()) + ");\n");
    }

    /**
     * Add a marker
     *
     * @param id  the marker id
     * @param pt  the position
     * @param icon  the icon
     * @param markerName _more_
     * @param info  the associated text
     */
    public void addMarker(String id, LatLonPointImpl pt, String icon,
                          String markerName, String info) {
        addMarker(id, pt.getLatitude(), pt.getLongitude(), icon, markerName,
                  info);
    }

    /**
     * Add a marker
     *
     * @param id  the marker id
     * @param lat  the latitude
     * @param lon  the longitude
     * @param icon  the icon
     * @param markerName _more_
     * @param info  the associated text
     */
    public void addMarker(String id, double lat, double lon, String icon,
                          String markerName, String info) {
        addMarker(id, lat, lon, icon, markerName, info, null);
    }

    /**
     * _more_
     *
     * @param id _more_
     * @param lat _more_
     * @param lon _more_
     * @param icon _more_
     * @param markerName _more_
     * @param info _more_
     * @param parentId _more_
     */
    public void addMarker(String id, double lat, double lon, String icon,
                          String markerName, String info, String parentId) {
	addMarker(id,lat,lon,null,icon,markerName,info,parentId);
    }

    public void addMarker(String id, double lat, double lon, String polygon, String icon,
                          String markerName, String info, String parentId) {

        getJS().append(mapVarName + ".addMarker(" + HU.squote(id)
                       + "," + llp(lat, lon) + "," + ((icon == null)
                ? "null"
                : HU.squote(icon)) + ","
                                          + HU.squote(
                                              markerName.replaceAll(
                                                  "'", "\\\\'")) + ","
                                                      + HU.squote(
                                                          info) + ","
                                                              + ((parentId
                                                                  == null)
                ? "null"
								 : HU.squote(parentId)) + ",null,null,null,null," + (polygon!=null?HU.squote(polygon):"null") + ");\n");
    }


    public void addMarker(Request request, Entry entry) throws Exception {
	addMarker(request, entry, false);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void addMarker(Request request, Entry entry, boolean useThumbnail) throws Exception {
        String   icon = repository.getPageHandler().getIconUrl(request,
                            entry);


	if(useThumbnail) {
            List<String> urls = new ArrayList<String>();
            repository.getMetadataManager().getThumbnailUrls(request, entry, urls);
	    if(urls.size()>0) icon = urls.get(0);
	}

        double[] location = entry.getCenter();
        String   id       = entry.getId();
        String info = repository.getMapManager().makeInfoBubble(request,
                          entry, true);
        String props = "null";

        String fillColor = entry.getTypeHandler().getDisplayAttribute(entry,
								      "mapFillColor");
        if (fillColor != null) {
            props = "{fillColor:'" + fillColor + "'";
            props += "}";
        }

        getJS().append(
            mapVarName + ".addEntryMarker(" + HU.squote(id) + ","
            + llp(location[0], location[1]) + "," + HU.squote(icon)
            + ","
            + HU.squote(entry.getName().replaceAll("'", "\\\\'"))
            + "," + HU.squote(info) + ","
            + HU.squote(entry.getTypeHandler().getType()) + ","
            + props + ");\n");
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private int getValue(Entry entry, String prop, int dflt) {
        TypeHandler th = entry.getTypeHandler();

        return (int) entry.getValue(
            entry.getColumnIndex(th.getTypeProperty(prop + ".field", null)),
            th.getTypeProperty(prop, dflt));
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private String getValue(Entry entry, String prop, String dflt) {
        TypeHandler th = entry.getTypeHandler();

        return (String) entry.getValue(
            entry.getColumnIndex(th.getTypeProperty(prop + ".field", null)),
            th.getTypeProperty(prop, dflt));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void addCircle(Request request, Entry entry) throws Exception {
        double[]    location = entry.getCenter();
        TypeHandler th       = entry.getTypeHandler();

        int         radius   = getValue(entry, "map.circle.radius", 10);
        int strokeWidth      = getValue(entry, "map.circle.stroke.width", 0);
        String fillColor = getValue(entry, "map.circle.fill.color", "orange");
        String strokeColor = getValue(entry, "map.circle.stroke.color",
                                      "orange");

        addCircle(entry.getId(), Math.max(-80, Math.min(80, location[0])),
                  location[1], radius, strokeWidth, strokeColor, fillColor,
                  repository.getMapManager().makeInfoBubble(request, entry,
                      true));
    }


    public void addCircle(String id, double lat, double lon, int radius) {
	addCircle(id,lat,lon,radius,1,"#000","blue","");
    }



    /**
     * _more_
     *
     * @param id _more_
     * @param lat _more_
     * @param lon _more_
     * @param radius _more_
     * @param strokeWidth _more_
     * @param strokeColor _more_
     * @param fillColor _more_
     * @param info _more_
     */
    public void addCircle(String id, double lat, double lon, int radius,
                          int strokeWidth, String strokeColor,
                          String fillColor, String info) {
        String attrs = Json.map("pointRadius", "" + radius, "strokeWidth",
                                "" + strokeWidth, "fillColor",
                                Json.quote(fillColor), "strokeColor",
                                Json.quote(strokeColor));
        getJS().append(mapVarName + ".addPoint("
                       + HU.comma(HU.squote(id), llp(lat, lon),
                                         attrs,
                                         HU.squote(info)) + ");\n");
    }




    /**
     * Add a KML Url
     *
     *
     * @param name _more_
     * @param url  the URL
     * @param canSelect _more_
     * @param args _more_
     */
    public void addKmlUrl(String name, String url, boolean canSelect,
                          String args) {
        name = name.replaceAll("'", " ");
        getJS().append(mapVarName + ".addKMLLayer(" + HU.squote(name)
                       + "," + HU.squote(url) + "," + canSelect
                       + ",null,null," + args + ");\n");

    }

    /**
     * Add a GeoJson Url
     *
     *
     * @param name _more_
     * @param url  the URL
     * @param canSelect _more_
     * @param args _more_
     */
    public void addGeoJsonUrl(String name, String url, boolean canSelect,
                              String args) {
        getJS().append(mapVarName + ".addGeoJsonLayer("
                       + HU.squote(name) + "," + HU.squote(url)
                       + "," + canSelect + ",null,null," + args + ");\n");
    }




    /**
     * Center the map on the bounds
     *
     * @param bounds  the bounds
     * @param force _more_
     */
    public void centerOn(Rectangle2D.Double bounds, boolean force) {
        if (bounds != null) {
	    //	    System.err.println("Center on A:" +Utils.getStack(5));
            Utils.append(getJS(), "var bounds = new OpenLayers.Bounds(",
                         bounds.getX(), ",", bounds.getY(), ",",
                         (bounds.getX() + bounds.getWidth()), ",",
                         (bounds.getY() + bounds.getHeight()), ");\n");
            Utils.append(getJS(), mapVarName, ".centerOnMarkers(bounds, ",
                         force, ");\n");
        } else {
            center();
        }

    }


    /**
     * Center the map
     */
    public void center() {
        Utils.append(getJS(), mapVarName, ".centerOnMarkersInit(null);\n");
    }

    /**
     * Center on the entry bounds
     *
     * @param entry  the entry
     */
    public void centerOn(Entry entry) {
        if (entry == null) {
            center();
            return;
        }
        if (entry.hasAreaDefined()) {
            centerOn(entry.getNorth(), entry.getWest(), entry.getSouth(),
                     entry.getEast());
        } else {
            center();
        }
    }


    /**
     * Center on the box defined by the N,S,E,W coords
     *
     * @param north  north edge
     * @param west   west edge
     * @param south  south edge
     * @param east   east edge
     */
    public void centerOn(double north, double west, double south,
                         double east) {
        Utils.append(getJS(), "var bounds = new OpenLayers.Bounds(", west,
                     ",", south, ",", east, ",", north, ");\n");
	//	System.err.println("Center on B:" +Utils.getStack(5));
        Utils.append(getJS(), mapVarName, ".centerOnMarkers(bounds);\n");
    }


    /**
     * Get the highlight  href tag
     *
     * @param id  the id
     * @param label  the label
     *
     * @return  the href tag
     */
    public String getHiliteHref(String id, String label) {
        return "<a href=\"javascript:" + getVariableName() + ".hiliteMarker("
               + HU.squote(id) + ");\">" + label + "</a>";
    }

    /**
     * Create a OpenLayers.LonLat string representation from a lat/lon point
     *
     * @param lat  the latitude
     * @param lon  the longitude
     *
     * @return the OpenLayer.LonLat
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


    /**
     *  Set the Width property.
     *
     *  @param value The new value for Width
     */
    public void setWidth(String value) {
        this.width = value;
    }

    /**
     *  Get the Width property.
     *
     *  @return The Width
     */
    public String getWidth() {
        return this.width;
    }

    /**
     *  Set the Height property.
     *
     *  @param value The new value for Height
     */
    public void setHeight(String value) {
        height = value;
    }

    /**
     *  Get the Height property.
     *
     *  @return The Height
     */
    public String getHeight() {
        return height;
    }





    /**
     * Set the selection label
     *
     * @param l  the label
     */
    public void setSelectionLabel(String l) {
        selectionLabel = l;
    }

    /**
     * Is this for selection?
     *
     * @return  true if for selection
     */
    public boolean forSelection() {
        return forSelection;
    }


    /**
     *  Set the MapRegions property.
     *
     *  @param value The new value for MapRegions
     */
    public void setMapRegions(List<MapRegion> value) {
        mapRegions = value;
    }

    /**
     *  Get the MapRegions property.
     *
     *  @return The MapRegions
     */
    public List<MapRegion> getMapRegions() {
        return mapRegions;
    }

    /**
     *  Set the DefaultMapRegion property.
     *
     *
     * @param mapRegion _more_
     */
    public void setDefaultMapRegion(String mapRegion) {
        defaultMapRegion = mapRegion;
    }

    /**
     *  Get the DefaultMapRegion property.
     *
     *  @return The DefaultMapRegion
     */
    public String getDefaultMapRegion() {
        return defaultMapRegion;
    }

    /**
     * Set the HeaderMessage property.
     *
     * @param value The new value for HeaderMessage
     */
    public void setHeaderMessage(String value) {
        headerMessage = value;
    }

    /**
     * Get the HeaderMessage property.
     *
     * @return The HeaderMessage
     */
    public String getHeaderMessage() {
        return headerMessage;
    }
}
