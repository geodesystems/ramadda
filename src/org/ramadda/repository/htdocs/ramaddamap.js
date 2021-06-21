/*
 * Copyright (c) 2008-2019 Geode Systems LLC
 */

var debugBounds = false;
var getMapDebug = false;

//This gets set by Java
var ramaddaMapRegions = null;


/* base maps to add */
var map_esri_topo = "esri.topo";
var map_esri_street = "esri.street";
var map_esri_worldimagery = "esri.worldimagery";
var map_esri_terrain = "esri.terrain";
var map_esri_shaded = "esri.shaded";
var map_esri_lightgray = "esri.lightgray";
var map_esri_darkgray = "esri.darkgray";
var map_esri_physical = "esri.physical";
var map_esri_aeronautical = "esri.aeronautical";
var map_opentopo = "opentopo";
var map_usgs_topo = "usgs.topo";
var map_usgs_imagery = "usgs.imagery";
var map_usgs_relief = "usgs.relief";
var map_watercolor = "watercolor";
var map_weather = "weather";
var map_lightblue = "lightblue";
var map_white = "white";
var map_blue = "blue";
var map_black = "black";
var map_gray = "gray";
var map_usfs_ownership = "usfs.ownership";
var map_osm = "osm";
var map_osm_toner = "osm.toner";
var map_osm_toner_lite = "osm.toner.lite";
var map_ol_openstreetmap = "ol.openstreetmap";

// Microsoft maps - only work for -180 to 180
var map_ms_shaded = "ms.shaded";
var map_ms_hybrid = "ms.hybrid";
var map_ms_aerial = "ms.aerial";

var map_default_layer = map_osm;


/*
Define new symbols
To make a new shape see errata.js 
*/

OpenLayers.Renderer.symbol.lightning = [0, 0, 4, 2, 6, 0, 10, 5, 6, 3, 4, 5, 0, 0];
OpenLayers.Renderer.symbol.rectangle = [0, 0, 4, 0, 4, 10, 0, 10, 0, 0];
OpenLayers.Renderer.symbol.church = [4, 0, 6, 0, 6, 4, 10, 4, 10, 6, 6, 6, 6, 14, 4, 14, 4, 6, 0, 6, 0, 4, 4, 4, 4, 0];
OpenLayers.Renderer.symbol._x = [0, 0, 6,6,3,3,6,0,0,6,3,3];
OpenLayers.Renderer.symbol.arrow = [0,0,5,10,10,0];
OpenLayers.Renderer.symbol.plane = [5,0,5,0,4,1,4,3,0,5,0,6,4,5,4,8,2,10,3,10,5,9,5,9,8,10,8,10,6,8,6,5,10,6,10,5,6,3,6,1,5,0,5,0];
OpenLayers.Renderer.symbol.arrow = [4,0,-2,10,12,10,6,0,4,0,-2,10,12,10,6,0];


var MapUtils =  {
    me:"MapUtils",
    CUSTOM_MAP : "CUSTOM",
    POSITIONMARKERID: "location",
    formatLocationValue:function(value) {
	return number_format(value, 4, ".", "");
    },
    createLonLat: function(lon, lat) {
	lon = parseFloat(lon);
	lat = parseFloat(lat);
	return new OpenLayers.LonLat(lon, lat);
    },
    createBounds:function (v1, v2, v3, v4) {
	v1 = parseFloat(v1);
	v2 = parseFloat(v2);
	v3 = parseFloat(v3);
	v4 = parseFloat(v4);
	return new OpenLayers.Bounds(v1, v2, v3, v4);
    },
    createProjection: function(name) {
	return new OpenLayers.Projection(name);
    },
    mapRegionSelected: function(selectId, baseId) {
        let value = $("#" + selectId).val();
        if (value == null) {
            console.log("Error: No map region value");
            return;
        }
        if (value == "") {
            this.toggleMapWidget(baseId, false);
            return;
        }
        let toks = value.split(",");

        if (toks.length == 1) {
            if (toks[0] != this.CUSTOM_MAP) {
                return;
            } else {
                if (!firstCustom) {
                    this.setMapRegion(baseId, "", "", "", "", "");
                }
                firstCustom = false;
                this.toggleMapWidget(baseId, true);
                return;
            }
        }
        if (toks.length != 5) {
            return;
        }
        this.toggleMapWidget(baseId, false);
        this.setMapRegion(baseId, toks[0], toks[1], toks[2], toks[3], toks[4]);
    },


    setMapRegion: function(baseId, regionid, north, west, south, east) {
        $("#" + baseId + "_regionid").val(regionid);
        $("#" + baseId + "_north").val(north);
        $("#" + baseId + "_west").val(west);
        $("#" + baseId + "_south").val(south);
        $("#" + baseId + "_east").val(east);

    },

    toggleMapWidget: function(baseId, onOrOff) {
        if (onOrOff) {
            // check if the map has been initialized
            let mapVar = window[baseId];
            if (mapVar && !mapVar.inited) {
                mapVar.initMap(true);
            }
            $("#" + baseId + "_mapToggle").show();
        } else {
            $("#" + baseId + "_mapToggle").hide();
        }
    },
}

MapUtils.defaults = {
    maxLatValue: 85,
    zoomLevels: 40,
    defaultZoomLevel: -1,
    maxExtent: MapUtils.createBounds(-20037508, -20037508, 20037508, 20037508),
    sourceProjection: MapUtils.createProjection("EPSG:900913"),
    displayProjection: MapUtils.createProjection("EPSG:4326"),
    units: "m",
    doSphericalMercator: true,
    wrapDateline: true,
    location: MapUtils.createLonLat(0, 0)
}

MapUtils.circleHiliteAttrs =  {
    strokeColor: 'black',
    strokeWidth: 1,
    fill: true,
    fillOpacity: 0.5,
    fillColor: 'red'
}



var ramaddaMapMap = {};

function ramaddaMapAdd(map) {
    if (window.globalMapList == null) {
        window.globalMapList = [];
    }
    window.globalMapList.push(map);
    ramaddaMapMap[map.mapId] = map;
}

function ramaddaMapRemove(map) {
    if (window.globalMapList == null) {
        window.globalMapList = [];
    }
    window.globalMapList.push(map);
    delete ramaddaMapMap[map.mapId];
}


function ramaddaMapCheckLayout() {
    setTimeout(()=>{
	if (window.globalMapList != null) {
            window.globalMapList.map(map => {
		map.map.updateSize();
	    });
	}
    },1000);
}



var ramaddaMapLastShareTime=0;
var ramaddaMapLastShareMap = "";
function ramaddaMapShareState(source, state) {
    //We check the time since the last state change because the zoomTo is done in the event loop
    //which can result in an infinite loop of state change calls
    let time = new Date().getTime();
    if(source.mapId!=ramaddaMapLastShareMap) {
	if(time-ramaddaMapLastShareTime<2000) {
	    return;
	}
    }
    ramaddaMapLastShareTime = time;
    ramaddaMapLastShareMap = source.mapId;
    if(source.stateIsBeingSet) return;
    let linkGroup = source.linkGroup;
    if(!source.linked && !linkGroup) return;
    var bounds = source.getBounds();
    var baseLayer = source.map.baseLayer;
    var zoom = source.map.getZoom();
    for(var i=0;i<window.globalMapList.length;i++) {
	var map = window.globalMapList[i];
	if(map.stateIsBeingSet || (!map.linked && !map.linkGroup)) continue;
	if(map.mapId==source.mapId) continue;
	if(linkGroup && linkGroup != map.linkGroup) continue;
	map.stateIsBeingSet = true;
	map.map.setCenter(source.map.getCenter(),
			  source.map.getZoom());
	map.stateIsBeingSet = false;
    }
}



function RepositoryMap(mapId, params) {
    let _this = this;
    if (!params) params = {};
    this.params = params;
    this.mapId = mapId || "map";
    ramaddaMapAdd(this);
    if(params.mapCenter) {
	[lat,lon] =  params.mapCenter.split(",");
	params.initialLocation = {lon:lon,lat:lat};
    }

    let showDflt = true;
    if(params.simple)
	showDflt = false;

    $.extend(this, {
        name: "map",
        sourceProjection: MapUtils.defaults.sourceProjection,
        displayProjection: MapUtils.defaults.displayProjection,
        projectionUnits: MapUtils.defaults.units,
        mapDivId: this.mapId,
        showScaleLine: showDflt,
        showLayerSwitcher: showDflt,
        showLatLonPosition: showDflt,
        showZoomPanControl: false,
        showZoomOnlyControl: true,
        enableDragPan: true,
        defaultLocation: MapUtils.defaults.location,
        initialZoom: Utils.isDefined(params.zoomLevel)?params.zoomLevel:MapUtils.defaults.defaultZoomLevel,
        latlonReadout: null,
        map: null,
        showBounds: true,
        defaultMapLayer: map_default_layer,
        haveAddedDefaultLayer: false,
        defaultCanSelect: true,
	highlightColor:"blue",
	highlightStrokeWidth:2,
	highlightOpacity:1,
        layer: null,
        markers: null,
        vectors: null,
	allLayers: [],
        loadedLayers: [],
	nonSelectLayers: [],
	doPopup:true,
	shareSelected:false,
	doSelect:true,
        boxes: null,
        kmlLayer: null,
        kmlLayerName: null,
        showSearch: false,
        geojsonlLayer: null,
        geojsonLayerName: null,
        vectorLayers: [],
        features: {},
        lines: null,
        selectorBox: null,
        selectorMarker: null,
        listeners: [],
        fixedText: false,
        initialLayers: [],
        imageLayers: {},
        tickSelectColor: "red",
        tickHoverColor: "blue",
        tickColor: "#888"
    });

    this.seenMarkers = {};    
    this.startDate = null;
    this.endDate = null;
    this.startFeature = null;
    this.endFeature = null;
    this.basePointStyle = null;


    var dflt = {
        pointRadius: 3,
        fillOpacity: 0.8,
        fillColor: "#e6e6e6",
        fill: true,
        strokeColor: "blue",
        strokeWidth: 1,
        scrollToZoom: false,
        selectOnHover: false,
        highlightOnHover: true,
        showLocationSearch: false,
    };

    $.extend(this, dflt);
    $.extend(this, params);

    this.defaultStyle = {
        pointRadius: this.pointRadius,
        fillOpacity: this.fillOpacity,
        fillColor: this.fillColor,
        fill: this.fill,
        strokeColor: this.strokeColor,
        strokeWidth: this.strokeWidth,
    };


    this.highlightStyle = {
	strokeColor:this.highlightColor,
	strokeWidth:this.highlightStrokeWidth,
	fillColor:this.highlightFillColor,
	fillOpacity:this.highlightOpacity
    }


    this.defaults = {};

    if (Utils.isDefined(params.onSelect)) {
        this.onSelect = params.onSelect;
    } else {
        this.onSelect = null;
    }
    if (params.initialLocation) {
	if(!Array.isArray(params.initialLocation)) {
            this.defaultLocation = MapUtils.createLonLat(params.initialLocation.lon, params.initialLocation.lat);
	} else {
            this.defaultLocation = MapUtils.createLonLat(params.initialLocation[1], params.initialLocation[0]);
	}
	
	if(debugBounds)
	    console.log("setting default location:" + this.defaultLocation);
    } else if (Utils.isDefined(params.initialBounds)) {
        if ((typeof params.initialBounds) == "string") {
            params.initialBounds = params.initialBounds.split(",");
        }
	//xxxxx
        this.defaultBounds = MapUtils.createBounds(params.initialBounds[1], params.initialBounds[2], params.initialBounds[3], params.initialBounds[0]);
	this.defaultLocation = null;
	if(debugBounds)
	    console.log("setting default bounds-1:" + this.defaultBounds);
    }
    

    var options = {
        projection: this.sourceProjection,
        displayProjection: this.displayProjection,
        units: this.projectionUnits,
        controls: [],
        maxResolution: 156543.0339,
        maxExtent: MapUtils.defaults.maxExtent,
        div: this.mapDivId,
        eventListeners: {
            featureover: function(e) {
                _this.handleFeatureover(e.feature);
            },
            featureout: function(e) {
                _this.handleFeatureout(e.feature);
            },
            nofeatureclick: function(e) {
                _this.handleNofeatureclick(e.layer);
            },
            featureclick: function(e) {
                if(e.feature && e.feature.noSelect) {
                    return;
                }
		let time =  new Date().getTime();
		//We get multiple click events if we have multiple features on the same point
		if(Utils.isDefined(this.lastClickTime)) {
		    if(time-this.lastClickTime <500) {
			return;
		    }
		}
		this.lastClickTime  = time;
		_this.handleFeatureclick(e.layer, e.feature);
            }
        }
    };


    this.mapOptions = options;
    this.finishMapInit();
    jQuery(document).ready(function($) {
        if (_this.getMap()) {
            _this.getMap().updateSize();
        }
    });
}



OpenLayers.Control.Click = OpenLayers.Class(OpenLayers.Control, {
    listeners: null,
    addListener: function(listener) {
        if (this.listeners == null) {
            this.listeners = [];
        }
        this.listeners.push(listener);
    },
    defaultHandlerOptions: {
        'single': true,
        'double': false,
        'pixelTolerance': 0,
        'stopSingle': false,
        'stopDouble': false
    },

    initialize: function(options) {
        this.handlerOptions = OpenLayers.Util.extend({},
						     this.defaultHandlerOptions);
        OpenLayers.Control.prototype.initialize.apply(this, arguments);
        this.handler = new OpenLayers.Handler.Click(this, {
            'click': this.trigger
        }, this.handlerOptions);
    },
    setLatLonZoomFld: function(lonFld, latFld, zoomFld, listener) {
        this.lonFldId = lonFld;
        this.latFldId = latFld;
        this.zoomFldId = zoomFld;
        this.clickListener = listener;
    },

    setTheMap: function(map) {
        this.theMap = map;
    },

    trigger: function(e) {
        var xy = this.theMap.getMap().getLonLatFromViewPortPx(e.xy);
        var lonlat = this.theMap.transformProjPoint(xy)
        if (this.listeners != null) {
            for (var i = 0; i < this.listeners.length; i++) {
                this.listeners[i](lonlat);
            }
        }
        if (!this.lonFldId) {
            this.lonFldId = "lonfld";
            this.latFldId = "latfld";
            this.zoomFldId = "zoomfld";
        }
	let lonFld = GuiUtils.getDomObject(this.lonFldId);
        let latFld = GuiUtils.getDomObject(this.latFldId);
        let zoomFld = GuiUtils.getDomObject(this.zoomFldId);
        if (latFld && lonFld) {
            latFld.obj.value = MapUtils.formatLocationValue(lonlat.lat);
            lonFld.obj.value = MapUtils.formatLocationValue(lonlat.lon);
        }
        if (zoomFld) {
            zoomFld.obj.value = this.theMap.getMap().getZoom();
        }
        //                this.setSelectionMarker(lonlat.lon, lonlat.lat);

        if (this.clickListener != null) {
            this.clickListener.handleClick(this, e, lonlat.lon, lonlat.lat);
        }


    }

});




var firstCustom = true;


var markerMap = {};

function highlightMarkers(selector, mapVar, background1, background2, id) {
    $(selector).mouseenter(
        function() {
            if (background1)
                $(this).css('background', background1);
            if (!$(this).data('mapid'))
                return;
            if (mapVar.circleMarker($(this).data('mapid'), MapUtils.circleHiliteAttrs)) {
                return;
            }
            if (id == null)
                return;
            if (!Utils.isDefined($(this).data('latitude'))) {
                console.log("no lat");
                return;
            }
            attrs = {
                pointRadius: 12,
                stroke: true,
                strokeColor: "blue",
                strokeWidth: 2,
                fill: false,
            };
            point = mapVar.addPoint(id, new OpenLayers.LonLat($(this).data('longitude'), $(this).data('latitude')),
				    attrs);
            markerMap[id] = point;
        });
    $(selector).mouseleave(
        function() {
            if (background2)
                $(this).css('background', background2);
            if (!$(this).data('mapid'))
                return;
            if (id && markerMap[id]) {
                mapVar.removePoint(markerMap[id]);
                markerMap[id] = null;
            }
            mapVar.uncircleMarker($(this).data('mapid'));
        });
}


function ramaddaFindFeature(layer, point) {
    for (var j = 0; j < layer.features.length; j++) {
        var feature = layer.features[j];
        var geometry = feature.geometry;
        if (!geometry) {
            continue;
        }
        bounds = geometry.getBounds();
        if (!bounds.contains(point.x, point.y)) {
            continue;
        }
        if (geometry.components) {
            for (var sub = 0; sub < geometry.components.length; sub++) {
                comp = geometry.components[sub];
                bounds = comp.getBounds();
                if (!bounds.contains(point.x, point.y)) {
                    continue;
                }
                if (comp.containsPoint && comp.containsPoint(point)) {
                    return {
                        feature: feature,
                        index: j
                    };
                }
            }
        } else {
            if (!geometry.containsPoint) {
                console.log("unknown geometry:" + geometry.CLASS_NAME);
                continue;
            }
            if (geometry.containsPoint(point)) {
                return {
                    feature: feature,
                    index: j
                };
            }
        }
    }
    return null;
}


RepositoryMap.prototype = {
    centerOnMarkers: function(dfltBounds, force, justMarkerLayer) {
	if(debugBounds) {
	    console.log("centerOnMarkers: force=" + force +" dflt:" + dfltBounds +" justMarkers:" + justMarkerLayer);
	}
        this.centerOnMarkersCalled = true;
        this.centerOnMarkersForce = force;
        let now = Date.now();
        let bounds = null;
        if (dfltBounds) {
            if (dfltBounds.left < -180 || dfltBounds.left > 180 ||
                dfltBounds.right < -180 || dfltBounds.right > 180 ||
                dfltBounds.bottom < -90 || dfltBounds.bottom > 90 ||
                dfltBounds.top < -90 || dfltBounds.top > 90) {
                dfltBounds = MapUtils.createBounds(-180, -90, 180, 90);
            }
        }
        this.dfltBounds = dfltBounds;
        if (!force) {
	    let didMarkers = false;
            if (this.markers) {
                // markers are in projection coordinates
                var dataBounds = this.markers.getDataExtent();
		if(debugBounds)
		    console.log("centerOnMarkers using markers.getDataExtent");
                bounds = this.transformProjBounds(dataBounds);
		didMarkers = true;
            }
	    if(bounds && isNaN(bounds.left)) bounds = null;
            if (this.circles) {
                let dataBounds = this.circles.getDataExtent();
                let b = this.transformProjBounds(dataBounds);
                if (bounds)
                    bounds.extend(b);
                else
                    bounds = b;
		didMarkers = true;
		if(debugBounds)
		    console.log("centerOnMarkers using circles.getDataExtent:" + b);
	    }

	    if(bounds && isNaN(bounds.left)) bounds = null;

            if ( !bounds || !didMarkers || !justMarkerLayer) {
                if (this.lines) {
                    var dataBounds = this.lines.getDataExtent();
                    var fromLine = this.transformProjBounds(dataBounds);
                    if (bounds)
                        bounds.extend(fromLine);
                    else
                        bounds = fromLine;
		    if(debugBounds)
			console.log("centerOnMarkers using lines.getDataExtent");
                }
                for (var layer in this.getMap().layers) {
                    layer = this.getMap().layers[layer];
                    if (!layer.getDataExtent) {
			continue;
		    }
                    if (layer.isBaseLayer || !layer.getVisibility()) {
			continue;
		    }
                    var dataBounds = layer.getDataExtent();
                    if (dataBounds) {
                        var latlon = this.transformProjBounds(dataBounds);
                        if (bounds)
                            bounds.extend(latlon);
                        else
                            bounds = latlon;
			if(debugBounds)
			    console.log("centerOnMarkers using layer.getDataExtent: " + latlon +" layer=" + layer.name +" " + layer.ramaddaId);
                    }
                }
            }
        }


        if (!bounds) {
	    if(debugBounds)
		console.log("centerOnMarkers using dfltBounds: " + dfltBounds);
            bounds = dfltBounds;
        }
        if (!bounds) {
	    if(debugBounds)
		console.log("centerOnMarkers: no bounds");
            return;
        }

        if (!this.getMap()) {
            this.defaultBounds = bounds;
	    if(debugBounds)
		console.log("centerOnMarkers: no map");
            return;
        }
        if (bounds.getHeight() > 160) {
            bounds.top = 80;
            bounds.bottom = -80;
	    if(debugBounds)
		console.log("centerOnMarkers resetting height");
        }
	if(debugBounds)
	    console.log("calling setViewToBounds: " + bounds);
        this.setViewToBounds(bounds);
    },
    setViewToBounds: function(bounds) {
        let projBounds = this.transformLLBounds(bounds);
        if (projBounds.getWidth() == 0) {
	    if(debugBounds) console.log("setViewToBounds center");
	    this.getMap().setCenter(projBounds.getCenterLonLat());
        } else {
	    //	    console.log(bounds.getCenterLonLat());
	    this.getMap().setCenter(projBounds.getCenterLonLat());
            this.zoomToExtent(projBounds);
        }
    },
    setCenter:function(to) {
	if(debugBounds)
	    console.log("setCenter");
	this.getMap().panTo(this.transformLLPoint(to));
//        this.getMap().setCenter(this.transformLLPoint(to));
    },
    setZoom: function(zoom) {
	if(debugBounds)
	    console.log("setZoom");
        this.getMap().zoomTo(zoom);
    },
    zoomToMarkers: function() {
        if (!this.markers)
            return;
        bounds = this.markers.getDataExtent();
        if (bounds == null) return;
        this.getMap().setCenter(bounds.getCenterLonLat());
        this.zoomToExtent(bounds);
    },
    zoomToExtent: function(bounds,flag) {
	if(debugBounds) {
	    console.log("zoomToExtent:" );
	}
	this.getMap().zoomToExtent(bounds,flag);
    },
    centerToMarkers: function() {
        if (!this.markers)
            return;
        bounds = this.markers.getDataExtent();
	if(debugBounds)
	    console.log("centerToMarkers");
        this.getMap().setCenter(bounds.getCenterLonLat());
    },
    setInitialCenterAndZoom: function(lon, lat, zoomLevel) {
        this.defaultLocation = MapUtils.createLonLat(lon, lat);
        this.initialZoom = zoomLevel;
    },
    setInitialZoom: function(zoomLevel) {
        this.initialZoom = zoomLevel;
    },
    setInitialCenter: function(lon,lat) {
        this.defaultLocation = MapUtils.createLonLat(lon, lat);
    },
    finishMapInit: function() {
        let _this = this;
        if (this.showSearch) {
            this.searchDiv = this.mapDivId + "_search";
            var cbx = HtmlUtils.checkbox(this.searchDiv + "_download", [], false);
            var input = "<input placeholder=\"Search - ? for help\" id=\"" + this.searchDiv + "_input" + "\" size=40>";
            var search = "<table width=100%><tr><td>" + input + " <span  id=\"" + this.searchDiv + "_message\"></span></td><td align=right>" + cbx + " Download</td></tr></table>"
            $("#" + this.searchDiv).html(search);
            this.searchMsg = $("#" + this.searchDiv + "_message");
            var searchInput = $("#" + this.searchDiv + "_input");
            searchInput.keypress(function(event) {
                if (event.which == 13) {
                    _this.searchFor(searchInput.val());
                }
            });
        }


	$("#" + this.mapDivId).html(HtmlUtils.div(["style","width:100%;height:100%;position:relative;","id",this.mapDivId+"_themap"]));
	$("#" + this.mapDivId+"_themap").append(HtmlUtils.div(["id",this.mapDivId+"_progress", CLASS,"ramadda-map-progesss", "style","z-index:3000;position:absolute;top:10px;left:50px;"],""));
	$("#" + this.mapDivId+"_themap").append(HtmlUtils.div(["id",this.mapDivId+"_label", "style","z-index:1000;position:absolute;bottom:10px;left:10px;"],""));
	$("#" + this.mapDivId+"_themap").append(HtmlUtils.div(["id",this.mapDivId+"_toolbar", "style","z-index:1000;position:absolute;top:10px;left:50%;    transform: translateX(-50%);"],""));
	if(this.showBookmarks || true) {
	    //		$("#" + this.mapDivId+"_themap").append(HtmlUtils.div(["id",this.mapDivId+"_bookmarks", "style","z-index:2000;position:absolute;top:140px;left:20px;"],HtmlUtils.getIconImage("fa-bookmark")));
	}
        this.map = new OpenLayers.Map(this.mapDivId+"_themap", this.mapOptions);
        //register the location listeners later since the map triggers a number of




        //events at the start
        var callback = function() {
            _this.getMap().events.register("changebaselayer", "", function() {
                _this.baseLayerChanged();
            });
            _this.getMap().events.register("zoomend", "", function() {
		_this.zoomChanged();
                _this.locationChanged();
		_this.setNoPointerEvents();
            });
            _this.getMap().events.register("moveend", "", function() {
                _this.locationChanged();
            });
	    _this.getMap().events.register("move", "", function() {
                _this.locationChanged();
            });
        };
	//Do this later
        setTimeout(callback, 2000);

        if (this.mapHidden) {
            //A hack when we are hidden
            this.getMap().size = new OpenLayers.Size(1, 1);
        }

        this.addBaseLayers();
        if (this.kmlLayer) {
            var url = ramaddaBaseUrl + "/entry/show?output=shapefile.kml&entryid=" + this.kmlLayer;
            this.addKMLLayer(this.kmlLayerName, url, false, null, null, null, null);
        }
        if (this.geojsonLayer) {
            var url = getRamadda().getEntryDownloadUrl(this.geojsonLayer);
            this.addGeoJsonLayer(this.geojsonLayerName, url, false, null, null, null, null);
        }
	Utils.addDisplay(this);
    },
    setProgress: function(msg) {
	$("#" + this.mapDivId+"_progress").html(msg);
    },
    setLabel: function(msg) {
	$("#" + this.mapDivId+"_label").html(msg);
    },	
    getBounds: function() {
	return  this.transformProjBounds(this.getMap().getExtent());
    },
    zoomChanged: function() {
    },
    locationChanged: function() {
	ramaddaMapShareState(this,"bounds");
	let latlon = this.getBounds();
	let bits = 100000;
	let r = (v=>{
	    return Math.round(v*bits)/bits;
	});
	latlon.top = r(latlon.top);
	latlon.left = r(latlon.left);
	latlon.bottom = r(latlon.bottom);
	latlon.right = r(latlon.right);

	HU.addToDocumentUrl("map_bounds",latlon.top + "," + latlon.left + "," + latlon.bottom + "," + latlon.right);
	HU.addToDocumentUrl("zoomLevel" , this.getMap().getZoom());

	let center =   this.transformProjPoint(this.getMap().getCenter())
        HU.addToDocumentUrl("mapCenter", r(center.lat)+","+ r(center.lon));
    },
    baseLayerChanged: function() {
        let baseLayer = this.getMap().baseLayer;
        if (!baseLayer) return;
        baseLayer = baseLayer.ramaddaId;
	HU.addToDocumentUrl("defaultMapLayer",baseLayer);
	ramaddaMapShareState(this,"baseLayer");
    },
    appendToolbar: function(html) {
	$("#" + this.mapDivId+"_toolbar").append(html);
    },

    setMapDiv: function(divid) {
        this.mapHidden = false;
        this.mapDivId = divid;
        this.getMap().render(this.mapDivId);
        this.getMap().updateSize();
        this.centerOnMarkers(this.dfltBounds);
    },
    makePopup: function(projPoint, text) {

	return  new OpenLayers.Popup("popup",
				     projPoint,
				     null,
				     text,
				     true,
				     ()=>{this.onPopupClose()});
	/** If we uses a FramedCloud then the olPopup class css in style.css needs to be changed to not have a border 
        return  new OpenLayers.Popup.FramedCloud("popup", projPoint,
						     null, text, null, true,
						     ()=>{this.onPopupClose()});
	**/

    },

    handleFeatureover: function(feature, skipText) {
	if(this.doMouseOver || feature.highlightText || feature.highlightTextGetter) {
	    var location = feature.location;
	    if (location) {
		if(this.highlightFeature != feature) {
		    this.closeHighlightPopup();
		    var projPoint = this.transformLLPoint(location);
		    let text = feature.highlightTextGetter?feature.highlightTextGetter(feature):feature.highlightText;
		    if (!Utils.stringDefined(text))  {text = feature.text;}
		    if (Utils.stringDefined(text)) {
			text =HtmlUtils.div(["style","padding:2px;"],text);
			this.highlightPopup = new OpenLayers.Popup("popup",
								   projPoint,
								   feature.highlightSize,
								   text,
								   false);
			this.highlightPopup.backgroundColor=this.highlightBackgroundColor||"#ffffcc";
			this.highlightPopup.autoSize=true;
			this.highlightPopup.keepInMap=true;
			this.highlightPopup.padding=0;
			this.getMap().addPopup(this.highlightPopup);
		    }
		}
	    }
	}



        var layer = feature.layer;
        if (!(layer.isMapLayer === true)) {
            if (!skipText && feature.text) {
                this.showFeatureText(feature);
            }
            return;
        }
	//            if (layer.canSelect === false || !(layer.isMapLayer === true)) return;
	if (layer.canSelect === false) return;
        var _this = this;

	//xxxx
        if (!feature.isSelected) {
            feature.originalStyle = feature.style;
            feature.style = null;
	    //"temporary"
	    let highlightStyle = $.extend({},this.highlightStyle);
	    if(feature.originalStyle) {
		highlightStyle.fillColor  = Utils.brighterColor(feature.originalStyle.fillColor||highlightStyle.fillColor,0.4);
	    }
            layer.drawFeature(feature, highlightStyle);
            if (this.displayDiv) {
                this.displayedFeature = feature;
                var callback = function() {
                    if (_this.displayedFeature == feature) {
			let text = _this.getFeatureText(layer, feature);
                        _this.showText(text);
                        _this.dateFeatureOver(feature);
                    }
                }
                if (!skipText) {
                    setTimeout(callback, 500);
                }
            }

        }
    },
    closeHighlightPopup: function() {
        if(this.highlightPopup) {
            this.getMap().removePopup(this.highlightPopup);
            this.highlightPopup.destroy();
	    this.highlightPopup  = null;
	    this.highlightFeature = null;
	}
    },
    handleFeatureout: function(feature, skipText) {
	this.closeHighlightPopup();
        let layer = feature.layer;
        if (layer && !(layer.isMapLayer === true)) {
            if (!skipText) {
                if (feature.text && !this.fixedText) {
                    this.hideFeatureText(feature);
                }
            }
            return;
        }
        if (layer == null || layer.canSelect === false) return;
        feature.style = feature.originalStyle;
        if (!feature.isSelected) {
            layer.drawFeature(feature, feature.style || "default");
        }
        this.dateFeatureOut(feature);
        if (!skipText && this.displayedFeature == feature && !this.fixedText) {
	    if(!this.displayDivSticky) 
                this.showText("");
        }
    },
    getLayerCanSelect: function(layer) {
        if (!layer) return false;
        return layer.canSelect;
    },
    handleNofeatureclick: function(layer) {
        if (layer.canSelect === false) return;
        if (layer && layer.selectedFeature) {
            this.unselectFeature(layer.selectedFeature);
            this.clearDateFeature();
        }
    },
    handleFeatureclick: function(layer, feature, center) {
        if (!layer)
            layer = feature.layer;

        this.dateFeatureSelect(feature);
        if (layer.canSelect === false) {
	    return;
	}
        if (layer.selectedFeature) {
            this.unselectFeature(layer.selectedFeature);
        }
	if(!this.doSelect) return;
        this.selectedFeature = feature;
        layer.selectedFeature = feature;
        layer.selectedFeature.isSelected = true;
	let style = $.extend({},feature.style);
	$.extend(style, {
	    strokeColor:this.highlightColor,
	    strokeWidth: this.highlightStrokeWidth,
	    strokeOpacity: 0.75,
	    fillOpacity: 0.75,
	    fill: true,
	});

	if(style.fillColor!="transparent") {
	    style.fillColor  = this.highlightColor;
	} 

	if(Utils.isDefined(style.pointRadius)) {
	    style.pointRadius = Math.round(style.pointRadius*1.5);
	}

	if(feature.style) {
	    if(feature.style.externalGraphic) {
		style = $.extend({},feature.style);
		style.graphicHeight*=1.5;
		style.graphicWidth*=1.5;
		style.graphicXOffset = -style.graphicWidth/ 2;
		style.graphicYOffset = -style.graphicHeight/ 2;
	    } else {
		//		    if(Utils.isDefined(feature.style.pointRadius)) {
		//			style.pointRadius = feature.style.pointRadius;
		//		    }
	    }
	    
	}
	//layer.drawFeature(layer.selectedFeature, "select");
	layer.drawFeature(layer.selectedFeature, style);
        if (layer.selectCallback) {
            layer.feature = layer.selectedFeature;
            if (feature.originalStyle) {
                feature.style = feature.originalStyle;
            }
            layer.selectCallback(layer);
        } else {
            this.showMarkerPopup(feature, true);
        }

        if (center) {
            var geometry = feature.geometry;
            if (geometry) {
                var bounds = geometry.getBounds();
                if (bounds) {
                    this.zoomToExtent(bounds);
                    this.getMap().setCenter(bounds.getCenterLonLat());
                }
            }
        }
    },


    unselectFeature: function(feature) {
        if (!feature) return;
        feature.renderIntent = null;
        feature.isSelected = false;
        let layer = feature.layer;
        if (!layer) return;
        layer.drawFeature(layer.selectedFeature, layer.selectedFeature.style || "default");
        layer.selectedFeature.isSelected = false;
        layer.selectedFeature = null;
        this.selectedFeature = null;
        this.onPopupClose();
        if (true) return;

        if (!feature) return;
        this.clearDateFeature(feature);
        layer = feature.layer;
        layer.selectedFeature = null;
        this.onPopupClose();
        feature.isSelected = false;
        layer.selectedFeature = null;
        this.selectedFeature = null;
        this.checkFeatureVisible(feature, true);
    },
    handleEntrySelect: function(id, name, type) {
        if (type != "geo_kml" && type != "geo_json" && type != "geo_shapefile") return null;
        var layer;
        if (type == "geo_kml") {
            var url = ramaddaBaseUrl + "/entry/get?entryid=" + id;
            layer = this.addKMLLayer(name, url, true, null, null, null, null, true);
        } else if (type == "geo_json") {
            var url = ramaddaBaseUrl + "/entry/get?entryid=" + id;
            layer = this.addGeoJsonLayer(name, url, true, null, null, null, null, true);
        } else {
            var url = ramaddaBaseUrl + "/entry/show?output=shapefile.kml&entryid=" + id;
            layer = this.addKMLLayer(name, url, true, null, null, null, null, true);
        }
        return layer;
    },
    findLayer: function(id) {
        for(var i=0;i<this.getMap().layers.length;i++) {
	    if(this.getMap().layers[i].ramaddaId == id) return this.getMap().layers[i];
	}
	return null;
    },
    addLayer: function(layer,nonSelectable) {
	this.allLayers.push(layer);
	layer.initialVisibility  = layer.getVisibility();
	if (this.map != null) {
            this.getMap().addLayer(layer);
	    if(nonSelectable)
		this.nonSelectLayers.push(layer);
            this.checkLayerOrder();
        } else {
            this.initialLayers.push(layer);
        }
    },
    checkLayerOrder: function() {
	let base = this.numberOfBaseLayers;
	this.nonSelectLayers.every(layer=>{
	    this.getMap().setLayerIndex(layer, base++);		
	    return true;
	});
	this.loadedLayers.every(layer=>{
	    this.getMap().setLayerIndex(layer, base++);		
	    return true;
	});
	if (this.boxes) {
            this.getMap().setLayerIndex(this.boxes, base++);
	}
	if (this.lines) {
            this.getMap().setLayerIndex(this.lines, base++);
	}
        if (this.circles) {
            this.getMap().setLayerIndex(this.circles, base++);
	}
	if (this.markers) {
            this.getMap().setLayerIndex(this.markers, base++);
	}
	if (this.labelLayer) {
	    this.getMap().setLayerIndex(this.labelLayer, base++);
	}
	//            this.getMap().resetLayersZIndex();
    },
    addImageLayer: function(layerId, name, desc, url, visible, north, west, south, east, width, height, args) {
        var _this = this;
        var theArgs = {
            forSelect: false,
            addBox: true,
            isBaseLayer: false,
        };
        if (args)
            OpenLayers.Util.extend(theArgs, args);

        //Things go blooeey with lat up to 90
        if (north > 88) north = 88;
        if (south < -88) south = -88;
        var latLonBounds = MapUtils.createBounds(west, south, east, north);
        var imageBounds = this.transformLLBounds(latLonBounds);
        var image = new OpenLayers.Layer.Image(
            name, url,
            imageBounds,
            new OpenLayers.Size(width, height), {
                numZoomLevels: 3,
                isBaseLayer: theArgs.isBaseLayer,
                resolutions: this.getMap().layers[0] ? this.getMap().layers[0].resolutions : null,
                maxResolution: this.getMap().layers[0] ? this.getMap().layers[0].resolutions[0] : null
            }
        );

        image.latLonBounds = latLonBounds;
        //        image.setOpacity(0.5);
        if (theArgs.forSelect) {
            this.selectImage = image;
        }
        var lonlat = new MapUtils.createLonLat(west, north);
        image.lonlat = this.transformLLPoint(lonlat);
        image.setVisibility(visible);
        image.id = layerId;
        image.text = this.getPopupText(desc);
        image.ramaddaId = layerId;
        image.ramaddaName = name;
        this.addLayer(image,true);
        image.north = north;
        image.west = west;
        image.south = south;
        image.east = east;
        if (!this.imageLayers) this.imageLayers = {}
        if (!theArgs.isBaseLayer) {
            this.imageLayers[layerId] = image;
        }

	if(!this.noPointerEventsLayers) this.noPointerEventsLayers = [];
	this.noPointerEventsLayers.push(image);
	this.setNoPointerEvents();
        return image;
    },
    setNoPointerEvents: function() {
	if(!this.noPointerEventsLayers) return;
	//Do this later
	setTimeout(()=>{
	    this.noPointerEventsLayers.forEach(image=>{
		if(image.div) {
		    var childNodes = image.div.childNodes;
		    for(var i = 0, len = childNodes.length; i < len; ++i) {
			var element = childNodes[i].firstChild || childNodes[i];
			var lastChild = childNodes[i].lastChild;
			if (lastChild && lastChild.nodeName.toLowerCase() === "iframe") {
			    element = lastChild.parentNode;
			}
			element.style["pointer-events"]="none";
		    }
		}
	    });
	},1000);
    },
    addWMSLayer: function(name, url, layer, isBaseLayer, nonSelectable,args) {
	if(!args) args = {};
	let attrs = {
            wrapDateLine: MapUtils.defaults.wrapDateline,
        };
	if(args.opacity) attrs.opacity=args.opacity;
        var layer = new OpenLayers.Layer.WMS(name, url, {
            layers: layer,
            format: "image/png",
            isBaseLayer: false,
            srs: "epse:4326",
            transparent: true
        }, attrs);
        if (isBaseLayer) {
            layer.isBaseLayer = true;
            layer.visibility = false;
        } else {
            layer.isBaseLayer = false;
            layer.visibility = true;
        }

        //If we have this here we get this error: 
        //http://lists.osgeo.org/pipermail/openlayers-users//2012-August/026025.html
        //        layer.reproject = true;
        this.addLayer(layer,nonSelectable);
	return layer;
    },
    addMapLayer: function(name, url, layer, isBaseLayer, isDefault) {
        var layer;
        if (/\/tile\//.exec(url)) {
            layer = new OpenLayers.Layer.XYZ(
                name, url, {
                    sphericalMercator: MapUtils.defaults.doSphericalMercator,
                    numZoomLevels: MapUtils.defaults.zoomLevels,
                    wrapDateLine: MapUtils.defaults.wrapDateline
                });
        } else {
            layer = new OpenLayers.Layer.WMS(name, url, {
                layers: layer,
                format: "image/png"
            }, {
                wrapDateLine: MapUtils.defaults.wrapDateline
            });
        }
        if (isBaseLayer)
            layer.isBaseLayer = true;
        else
            layer.isBaseLayer = false;
        layer.visibility = false;
        layer.reproject = true;
        this.addLayer(layer);
        if (isDefault) {
            this.haveAddedDefaultLayer = true;
            this.getMap().setLayerIndex(layer, 0);
            this.getMap().setBaseLayer(layer);
        }
    },

    getVectorLayerStyleMap: function(layer, args) {
        var props = {
            pointRadius: this.pointRadius,
            fillOpacity: this.fillOpacity,
            fillColor: this.fillColor,
            fill: this.fill,
            strokeColor: this.strokeColor,
            strokeWidth: this.strokeWidth,
            select_fillOpacity: this.fillOpacity,
            select_fillColor: "#666",
            select_strokeColor: "#666",
            select_strokeWidth: 1
        };
        if (args) RamaddaUtil.inherit(props, args);
        var temporaryStyle = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style["temporary"]);
        $.extend(temporaryStyle, {
            pointRadius: props.pointRadius,
            fillOpacity: props.fillOpacity,
            strokeWidth: props.strokeWidth,
            strokeColor: props.strokeColor,
        });
        var selectStyle = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style["select"]);
        $.extend(selectStyle, {
            pointRadius: 3,
            fillOpacity: props.select_fillOpacity,
            fillColor: props.select_fillColor,
            strokeColor: props.select_strokeColor,
            strokeWidth: props.select_strokeWidth
        });

        var defaultStyle = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style["default"]);
        $.extend(defaultStyle, {
            pointRadius: props.pointRadius,
            fillOpacity: props.fillOpacity,
            fillColor: props.fillColor,
            fill: props.fill,
            strokeColor: props.strokeColor,
            strokeWidth: props.strokeWidth
        });
        let map = new OpenLayers.StyleMap({
            "temporary": temporaryStyle,
            "default": defaultStyle,
            "select": selectStyle
        });
        return map;
    },

    getFeatureName: function(feature) {
        var p = feature.attributes;
        for (var attr in p) {
            var name = ("" + attr).toLowerCase();
            if (!(name.includes("label"))) continue;
            var value = this.getAttrValue(p, attr);
            if (value) return value;
        }
        for (var attr in p) {
            var name = ("" + attr).toLowerCase();
            if (!(name.includes("name"))) continue;
            var value = this.getAttrValue(p, attr);
            if (value) return value;
        }
    },
    getAttrValue: function(p, attr) {
        let value = "";
        if ((typeof p[attr] == 'object') || (typeof p[attr] == 'Object')) {
            let o = p[attr];
            if (o) {
                value = "" + o["value"];
            }
        } else {
            value = "" + p[attr];
        }
        return value;
    },

    searchFor: function(searchFor) {
        let _this = this;
        if (searchFor) searchFor = searchFor.trim();
        if (searchFor == "") searchFor = null;
        this.searchText = searchFor;
        let bounds = null;
        let toks = null;
        let doHelp = false;
        let download = $("#" + this.searchDiv + "_download").is(':checked');
        let attrs = [];
        let doOr = true;
        if (searchFor) {
            searchFor = searchFor.trim();
            if (searchFor == "?") {
                doHelp = true;
            }
            searchFor = searchFor.toLowerCase();
            toks = [];
            let tmp = searchFor.split("|");
            if (tmp.length == 1) {
                tmp = searchFor.split("&");
                doOr = tmp.length == 1;
            }
            for (let i = 0; i < tmp.length; i++) {
                let not = false;
                let equals = false;
                let tok = tmp[i];
                let tmp2 = tok.split(":");
                let field = "";
                let value = tok;
                if (tmp2.length > 1) {
                    field = tmp2[0];
                    value = tmp2[1];
                }
                if (value.startsWith("!")) {
                    not = true;
                    value = value.substring(1);
                }
                if (value.startsWith("=")) {
                    value = value.substring(1);
                    equals = true;
                }
                toks.push({
                    field: field,
                    value: value,
                    not: not,
                    equals: equals
                });
            }
        } else {
            this.searchMsg.html("");
	}

        for (let a in this.loadedLayers) {
            let layer = this.loadedLayers[a];
            if (layer.selectedFeature) {
                this.handleNofeatureclick(layer);
            }
            for (let f in layer.features) {
                let feature = layer.features[f];
                let p = feature.attributes;
                if (!searchFor) {
                    feature.featureVisibleSearch = true;
                    attrs.push(p);
                    continue;
                }
                if (doHelp) {
                    let space = "&nbsp;&nbsp;&nbsp;"
                    let help = "Enter, e.g.:<br>";
                    help += space + "<i>&lt;term&gt;</i> (any match)<br>";
                    help += space + "<i>=&lt;term&gt;</i> (exact match)<br>";
                    help += space + "<i>!&lt;term&gt;</i> (not match)<br>";
                    help += space + "<i>&lt;term 1&gt;|&lt;term 2&gt;</i> (match any)<br>";
                    help += space + "<i>&lt;term 1&gt;&amp;&lt;term 2&gt;</i> (match all)<br>";
                    help += "Or by field:<br>";
                    for (let attr in p) {
                        help += space + attr.toLowerCase() + ":" + "<i>&lt;term&gt;</i><br>";
                    }
                    this.showText(help);
                    return
                }
                let matches = false;
                let checkAll = false;
                let allMatched = true;
                let someMatched = false;
//                if (tok.not) checkAll = true;
                for (let v in toks) {
                    let tok = toks[v];
                    for (let attr in p) {
                        if (tok.field != "" && tok.field != attr.toLowerCase()) {
                            continue;
                        }
                        let value = this.getAttrValue(p, attr);
                        value = value.toLowerCase().trim();
                        if (tok.equals) {
                            matches = (value == tok.value);
                        } else {
                            matches = value.includes(tok.value);
                        }
                        if (tok.not) {
                            matches = !matches;
                            if (!matches) {
                                break;
                            }
                        } else {
                            if (matches) break;
                        }
                    }
                    if (matches) someMatched = true;
                    if (!matches) allMatched = false;
                }

                if ((doOr && someMatched) || (!doOr && allMatched)) {
                    feature.featureVisibleSearch = true;
                    if (this.getFeatureVisible(feature)) {
                        attrs.push(p);
                    }
                } else {
                    feature.featureVisibleSearch = false;
                }
            }

            this.centerOnMarkers();
            if (download) {
                let csv = "";
                for (let i in attrs) {
                    let p = attrs[i];
                    for (let attr in p) {
                        if (csv != "") csv += ",";
                        csv += attr.toLowerCase();
                    }
                    csv += "\n";
                    break;
                }
                for (let i in attrs) {
                    let p = attrs[i];
                    let line = "";
                    for (let attr in p) {
                        let value = this.getAttrValue(p, attr);
                        if (value.includes(",")) {
                            value = "\"" + value + "\"";
                        }
                        if (line != "") line += ",";
                        line += value;
                    }
                    line += "\n";
                    csv += line;
                }
                Utils.makeDownloadFile("download.csv", csv);
            }
            this.setFeatureVisibility(layer);
        }
    },
    checkFeatureVisible: function(feature, redraw) {
        let layer = feature.layer;
        let visible = this.getFeatureVisible(feature);
        if (feature.originalStyle) {
            feature.style = feature.originalStyle;
        }
        let style = feature.style;
        if (!style) {
            style = {};
            let defaultStyle = layer.styleMap.styles["default"].defaultStyle;
            $.extend(style, defaultStyle);
            feature.style = style;
        } else {}
        if (!visible) {
            style.display = 'none';
        } else {
            style.display = 'inline';
        }
        if (redraw) {
            if (!feature.isSelected)
                feature.renderIntent = null;
            layer.drawFeature(feature, feature.style || "default");
        }
        return visible;
    },

    getFeatureVisible: function(feature) {
        let visible = true;
        if (Utils.isDefined(feature.featureVisibleSearch) && !feature.featureVisibleSearch) {
            visible = false;
        }
        if (Utils.isDefined(feature.featureVisibleDate) && !feature.featureVisibleDate) {
            visible = false;
        }
        if (Utils.isDefined(feature.featureVisible) && !feature.featureVisible) {
            visible = false;
        }
        return visible;
    },
    setFeatureVisibility: function(layer) {
        let _this = this;
        let didSearch = this.searchText || (this.startDate && this.endDate);
        let bounds = null;
        let html = "";
        let didOn = false;
        let didOff = false;
        let cnt = 0;
        let onFeature = null;
        for (let i = 0; i < layer.features.length; i++) {
            let feature = layer.features[i];
            let visible = this.checkFeatureVisible(feature, false);
            if (!visible) {
                this.clearDateFeature(feature);
                didOff = true;
            } else {
                this.dateFeatureSelect(feature);
                didOn = true;
                cnt++;
                if (!onFeature) onFeature = feature;
                html += HtmlUtils.div(["class", "ramadda-map-feature", "feature-index", "" + i], this.getFeatureName(feature));
                let geometry = feature.geometry;
                if (geometry) {
                    let fbounds = geometry.getBounds();
                    if (bounds) bounds.extend(fbounds);
                    else bounds = fbounds;
                }
            }
        }
        if (cnt == 1) {
            this.showText(this.getFeatureText(layer, onFeature));
        } else {
            if (didSearch || (didOn && didOff)) {
                let id = this.mapDivId + "_features";
                this.showText(HtmlUtils.div(["id", id, "class", "ramadda-map-features"], html));
                $("#" + id + " .ramadda-map-feature").click(function() {
                    let index = parseInt($(this).attr("feature-index"));
                    _this.handleFeatureclick(layer, layer.features[index], true);
                });
                $("#" + id + " .ramadda-map-feature").mouseover(function() {
                    let index = parseInt($(this).attr("feature-index"));
                    _this.handleFeatureover(layer.features[index], true);
                });
                $("#" + id + " .ramadda-map-feature").mouseout(function() {
                    let index = parseInt($(this).attr("feature-index"));
                    _this.handleFeatureout(layer.features[index], true);
                });

            } else {
                this.clearDateFeature();
		if(!this.displayDivSticky) 
		    this.showText("");
            }
        }
        if (this.searchMsg) {
            if (didSearch)
                this.searchMsg.html(cnt + " matched");
            else
                this.searchMsg.html("");
        }
        layer.redraw();
        if (bounds) {
            this.zoomToExtent(bounds);
            this.getMap().setCenter(bounds.getCenterLonLat());
        } else {
            this.centerOnMarkers(this.dfltBounds, this.centerOnMarkersForce);
        }
    },
    getFeatureText: function(layer, feature) {
	if(feature.textGetter) {
	    return  feature.textGetter(feature);
	}
	if(this.textGetter) {
	    return this.textGetter(layer,feature);
	}

        let style = feature.style || feature.originalStyle || layer.style;
        let p = feature.attributes;
        let out = feature.popupText;
        if (!out) {
            if (style && style["balloonStyle"]) {
                out = style["balloonStyle"];
                for (let attr in p) {
                    //$[styleid/attr]
                    let label = attr.replace("_", " ");
                    let value = "";
                    if (typeof p[attr] == 'object' || typeof p[attr] == 'Object') {
                        let o = p[attr];
                        value = "" + o["value"];
                    } else {
                        value = "" + p[attr];
                    }
                    out = out.replace("${" + style.id + "/" + attr + "}", value);
                }
            } else {
                out = "<table>";
                for (let attr in p) {
                    let label = attr;
                    lclabel = label.toLowerCase();
                    if (lclabel == "objectid" ||
                        lclabel == "feature_type" ||
                        lclabel == "shapearea" ||
                        lclabel == "styleurl" ||
                        lclabel == "shapelen") continue;
                    if (lclabel == "startdate") label = "Start Date";
                    else if (lclabel == "enddate") label = "End Date";
                    else if (lclabel == "aland") label = "Land Area";
                    else if (lclabel == "awater") label = "Water Area";
                    label = label.replace(/_/g, " ");
                    label = Utils.camelCase(label);
                    out += "<tr valign=top><td align=right><div style=\"margin-right:5px;margin-bottom:3px;\"><b>" + label + ":</b></div></td><td><div style=\"margin-right:5px;margin-bottom:3px;\">";
                    let value;
                    if (p[attr] != null && (typeof p[attr] == 'object' || typeof p[attr] == 'Object')) {
                        let o = p[attr];
                        value = "" + o["value"];
                    } else {
                        value = "" + p[attr];
                    }
                    if (value.startsWith("http:") || value.startsWith("https:")) {
                        value = "<a href='" + value + "'>" + value + "</a>";
                    }
                    if (value == "null") continue;
                    out += value;
                    out += "</div></td></tr>";
                }
                out += "</table>";
            }
        }
        return out;
    },
    onFeatureSelect: function(layer) {
	let _this = this;
        if (this.onSelect) {
            func = window[this.onSelect];
            func(this, layer);
            return;
        }
        let feature = layer.feature;
	if(this.featureSelectHandler) {
	    if(this.featureSelectHandler(feature)) {
		return;
	    }
	}

	if(!this.doPopup) return;
        let out = this.getFeatureText(layer, feature);
	if(!out) return;
        if (this.currentPopup) {
            this.getMap().removePopup(this.currentPopup);
            this.currentPopup.destroy();
        }

        let popup = this.makePopup(feature.geometry.getBounds().getCenterLonLat(),out);
        feature.popup = popup;
        popup.feature = feature;
        this.getMap().addPopup(popup);
        this.currentPopup = popup;
    },

    onFeatureUnselect:  function(layer) {
        this.onPopupClose();
    },

    removeKMLLayer:  function(layer) {
	this.removeLayer(layer);
    },
    removeLayer:  function(layer) {
	this.allLayers = OpenLayers.Util.removeItem(this.allLayers, layer);
	if(this.nonSelectLayers)
	    this.nonSelectLayers = OpenLayers.Util.removeItem(this.nonSelectLayers, layer);
	if(this.loadedLayers)
	    this.loadedLayers = OpenLayers.Util.removeItem(this.loadedLayers, layer);
        this.getMap().removeLayer(layer);
    },

    setDefaultCanSelect:  function(canSelect) {
        this.defaultCanSelect = canSelect;
    },

    getCanSelect:  function(canSelect) {
        if (((typeof canSelect) == "undefined") || (canSelect == null)) {
            return this.defaultCanSelect;
        }
        return canSelect;
    },

    addSelectCallback:  function(layer, canSelect, selectCallback, unselectCallback) {
        let _this = this;
        if (this.getCanSelect(canSelect)) {
            /** don't add listeners here. We do it up at the main map level
                select = new OpenLayers.Control.SelectFeature(layer, {
                multiple: false, 
                hover: this.selectOnHover,
                //highlightOnly: true,
                renderIntent: "select",
                });
                if (this.highlightOnHover) {
                highlight = new OpenLayers.Control.SelectFeature(layer, {
                multiple: false, 
                hover: true,
                highlightOnly: true,
                renderIntent: "temporary"
                });
                }
                //                highlight.selectStyle = OpenLayers.Feature.Vector.style['temporary'];
                */
            if (selectCallback == null || !Utils.isDefined(selectCallback))
                selectCallback = function(layer) {
                    _this.onFeatureSelect(layer)
                };
            if (unselectCallback == null || !Utils.isDefined(unselectCallback))
                unselectCallback = function(layer) {
                    _this.onFeatureUnselect()
                };
            layer.selectCallback = selectCallback;
            layer.unselectCallback = selectCallback;
            /**
               layer.events.on({ 
               "featureselected": selectCallback,
               "featureunselected": unselectCallback,
               });

               if (this.highlightOnHover) {
               this.getMap().addControl(highlight);
               highlight.activate();   
               }
               this.getMap().addControl(select);
               select.activate();   
            */
	}     
    },

    initDates:  function(layer) {
        let _this = this;
        let features = layer.features;
        let didDate = false;
        let didYear = false;
        this.hasDates = false;
        this.dates = [];
        this.dateFeatures = [];
        this.minDate = null;
        this.maxDate = null;
        for (let i = 0; i < features.length; i++) {
            let feature = features[i];
            let p = feature.attributes;
            for (let attr in p) {
                let name = ("" + attr).toLowerCase();
                let isYear = name.includes("year") || name.includes("_yr");
                let isDate = name.includes("date");
                if (!(isDate || isYear)) continue;
                let value = this.getAttrValue(p, attr);
                if (!value) continue;
                if (value == "0") continue;
                try {
                    let date = Utils.parseDate(value);
                    if (isYear) didYear = true;
                    else didDate = true;
                    if (this.startDate != null && this.endDate != null) {
                        if (date.getTime() < this.startDate.getTime() || date.getTime() > this.endDate.getTime()) {
                            continue;
                        }
                    }
                    this.dates.push(date);
                    this.dateFeatures.push(feature);
                    this.minDate = this.minDate == null ? date : this.minDate.getTime() > date.getTime() ? date : this.minDate;
                    this.maxDate = this.maxDate == null ? date : this.maxDate.getTime() < date.getTime() ? date : this.maxDate;
                    feature.featureDate = date;
                    this.hasDates = true;
                    break;
                } catch (e) {
                    console.log("error:" + e);
                }
            }
        }
        if (this.hasDates) {
            let options = null;
            if (didYear)
                options = {
                    year: 'numeric'
                };
            $("#" + this.mapDivId + "_footer").html(HtmlUtils.div(["class", "ramadda-map-animation", "id", this.mapDivId + "_animation"], ""));
            this.animation = $("#" + this.mapDivId + "_animation");
            let ticksDiv = HtmlUtils.div(["class", "ramadda-map-animation-ticks", "id", this.mapDivId + "_animation_ticks"], "");
            let infoDiv = HtmlUtils.div(["class", "ramadda-map-animation-info", "id", this.mapDivId + "_animation_info"], "");
            this.animation.html(ticksDiv + infoDiv);
            let startLabel = Utils.formatDate(this.minDate, options);
            let endLabel = Utils.formatDate(this.maxDate, options);
            this.animationTicks = $("#" + this.mapDivId + "_animation_ticks");
            this.animationInfo = $("#" + this.mapDivId + "_animation_info");
            let center = "";
            if (this.startDate && this.endDate) {
                center = HtmlUtils.div(["id", this.mapDivId + "_ticks_reset", "class", "ramadda-map-animation-tick-reset"], "Reset");
            }
            let info = "<table width=100%><tr valign=top><td width=40%>" + startLabel + "</td><td align=center width=20%>" + center + "</td><td align=right width=40%>" + endLabel + "</td></tr></table>";
            this.animationInfo.html(info);
            if (this.startDate && this.endDate) {
                let reset = $("#" + this.mapDivId + "_ticks_reset");
                reset.click(function() {
                    _this.startDate = null;
                    _this.endDate = null;
                    _this.startFeature = null;
                    _this.endFeature = null;
                    _this.setFeatureDateRange(layer, "Resetting range...");
                });
            }
            let width = this.animationTicks.width();
            let percentPad = width > 0 ? 5 / width : 0;
            //            console.log("w:" + width + " " + percentPad);
            let html = "";
            let start = this.minDate.getTime();
            let end = this.maxDate.getTime();
            if (this.startDate != null && this.endDate != null) {
                start = this.startDate.getTime();
                end = this.endDate.getTime();
            }
            let range = end - start;
            if (range > 0) {
                for (let i = 0; i < this.dates.length; i++) {
                    let date = this.dates[i];
                    let time = date.getTime();
                    if (time < start || time > end) continue;
                    let feature = this.dateFeatures[i];
                    feature.dateIndex = i;
                    let percent = 100 * (time - start) / range;
                    percent = percent - percent * percentPad;
                    if(!options) options = {};
                    let fdate = date.toLocaleDateString("en-US", options);
                    let name = Utils.camelCase(this.getFeatureName(feature));
                    let tooltip = "";
                    tooltip += name != null ? name + "<br>" : "";
                    tooltip += fdate;
                    tooltip += "<br>shift-click: set visible range<br>cmd/ctrl-click:zoom";
                    tooltip += "";
                    html += HtmlUtils.div(["id", this.mapDivId + "_tick" + i, "feature-index", "" + i, "style", "left:" + percent + "%", "class", "ramadda-map-animation-tick", "title", tooltip], "");
                }
            }
            this.animationTicks.html(html);
            let tick = $("#" + this.mapDivId + "_animation .ramadda-map-animation-tick");
            tick.tooltip({
                content: function() {
                    return $(this).prop('title');
                },
                position: {
                    my: "left top",
                    at: "left bottom+2"
                },
                classes: {
                    "ui-tooltip": "ramadda-popup"
                }
            });
            tick.mouseover(function() {
                let index = parseInt($(this).attr("feature-index"));
                let feature = _this.dateFeatures[index];
                _this.handleFeatureover(feature, true);
            });
            tick.mouseout(function() {
                let index = parseInt($(this).attr("feature-index"));
                let feature = _this.dateFeatures[index];
                _this.handleFeatureout(feature, true);
            });
            tick.click(function(evt) {
                let index = parseInt($(this).attr("feature-index"));
                let feature = _this.dateFeatures[index];
                if (evt.shiftKey) {
                    if (_this.startDate == null) {
                        _this.startDate = feature.featureDate;
                        _this.startFeature = feature;
                        _this.dateFeatureSelect(feature);
                    } else if (_this.endDate == null) {
                        _this.endDate = feature.featureDate;
                        _this.endFeature = feature;
                        _this.dateFeatureSelect(feature);
                    } else {
                        _this.dateFeatureSelect(null);
                        _this.startDate = feature.featureDate;
                        _this.startFeature = feature;
                        _this.endDate = null;
                        _this.endFeature = null;
                        _this.dateFeatureSelect(feature);
                    }
                    if (_this.startDate != null && _this.endDate != null) {
                        if (_this.startDate.getTime() == _this.endDate.getTime()) {
                            _this.startDate = null;
                            _this.startFeature = null;
                            _this.endDate = null;
                            _this.endFeature = null;
                            _this.dateFeatureSelect(null);
                            return;
                        } else if (_this.startDate.getTime() > _this.endDate.getTime()) {
                            let tmp = _this.startDate;
                            _this.startDate = _this.endDate;
                            _this.endDate = tmp;
                            tmp = _this.startFeature;
                            _this.startFeature = _this.endFeature;
                            _this.endFeature = tmp;
                        }
                        _this.setFeatureDateRange(feature.layer);
                    }
                } else {
                    let center = evt.metaKey || evt.ctrlKey;
                    if (_this.startDate != null || _this.endDate != null) {
                        _this.startDate = null;
                        _this.endDate = null;
                        //                        _this.setFeatureDateRange(feature.layer, feature.featureDate,"Resetting range...");
                        //                            center = true;
                    }
                    _this.clearDateFeature();
                    _this.handleFeatureclick(feature.layer, feature, center);
                }
            });
        }
    },

    setFeatureDateRange:  function(layer, msg) {
        this.dateFeatureSelect(null);
        if (!msg) msg = "Setting range...";
        this.animationTicks.html(msg);
        setTimeout(() => this.setFeatureDateRangeInner(layer), 100);
    },

    setFeatureDateRangeInner:  function(layer) {
        let features = layer.features;
        if (layer.selectedFeature) {
            this.unselectFeature(layer.selectedFeature);
        }
        for (let i = 0; i < features.length; i++) {
            let feature = features[i];
            if (this.startDate && this.endDate && feature.featureDate) {
                feature.featureVisibleDate = this.startDate.getTime() <= feature.featureDate.getTime() &&
                    feature.featureDate.getTime() <= this.endDate.getTime();
            } else {
                feature.featureVisibleDate = true;
            }
        }
        this.setFeatureVisibility(layer);
        this.initDates(layer);
    },
    dateFeatureSelect:  function(feature) {
        let tick = this.getFeatureTick(feature);
        tick.css("background-color", this.tickSelectColor);
        tick.css("zIndex", "100");
    },
    dateFeatureOver:  function(feature) {
        let tick = this.getFeatureTick(feature);
        tick.css("background-color", this.tickHoverColor);
        tick.css("zIndex", "100");
        //In case some aren't closed
        //        this.getFeatureTick(null).tooltip("close");
        if (this.tickOpen) this.tickOpen.tooltip("close");
        this.tickOpen = tick;
        tick.tooltip("open");
    },
    dateFeatureOut:  function(feature) {
        let tick = this.getFeatureTick(feature);
        if (feature && (feature == this.startFeature || feature == this.endFeature)) {
            tick.css("background-color", this.tickSelectColor);
            tick.css("zIndex", "0");
        } else {
            tick.css("background-color", "");
            tick.css("zIndex", "0");
        }
        this.tickOpen = null;
        tick.tooltip("close");
    },
    getFeatureTick:  function(feature) {
        if (!feature)
            return $("#" + this.mapDivId + "_animation_ticks" + " .ramadda-map-animation-tick");
        return $("#" + this.mapDivId + "_tick" + feature.dateIndex);
    },
    clearDateFeature:  function(feature) {
        let element = feature != null ? $("#" + this.mapDivId + "_tick" + feature.dateIndex) : $("#" + this.mapDivId + "_animation_ticks .ramadda-map-animation-tick");
        element.css("background-color", "");
        element.css("zIndex", "0");
    },


    initMapVectorLayer:  function(layer, canSelect, selectCallback, unselectCallback, loadCallback, zoomToExtent) {
        let _this = this;
        this.showLoadingImage();
        layer.isMapLayer = true;
        layer.canSelect = canSelect;
        this.loadedLayers.push(layer);
        layer.events.on({
            "loadend": function(e) {
                _this.hideLoadingImage();
                if (e.response && Utils.isDefined(e.response.code) && e.response.code == OpenLayers.Protocol.Response.FAILURE) {
                    console.log("An error occurred loading the map:" + JSON.stringify(e.response, null, 2));
                    return;
                }
                if (zoomToExtent) {
                    let dataBounds = layer.getDataExtent();
                    if (dataBounds) {
                        _this.zoomToExtent(dataBounds, true);
                    }
                } else {
                    if (_this.centerOnMarkersCalled) {
                        if (!_this.dfltBounds)
                            _this.centerOnMarkers(_this.dfltBounds, _this.centerOnMarkersForce);
                    }
                }
                if (loadCallback) {
                    loadCallback(_this, layer);
                }
                if (layer.features.length == 1 && _this.displayDiv) {
                    _this.fixedText = true;
                    _this.showText(_this.getFeatureText(layer, layer.features[0]));
                }
                _this.initDates(layer);
            }
        });

        this.addLayer(layer);
        this.addSelectCallback(layer, canSelect, selectCallback, unselectCallback);
    },

    addGeoJsonLayer:  function(name, url, canSelect, selectCallback, unselectCallback, args, loadCallback, zoomToExtent) {
        let layer = new OpenLayers.Layer.Vector(name, {
            projection: this.displayProjection,
            strategies: [new OpenLayers.Strategy.Fixed()],
            protocol: new OpenLayers.Protocol.HTTP({
                url: url,
                format: new OpenLayers.Format.GeoJSON({})
            }),
            //xxstyleMap: this.getVectorLayerStyleMap(args)
        });
	let opts =  {
            strokeColor: 'blue',
            strokeWidth: 1,
        }
	if(args) $.extend(opts, args);
        layer.styleMap = this.getVectorLayerStyleMap(layer, opts);
        this.initMapVectorLayer(layer, canSelect, selectCallback, unselectCallback, loadCallback, zoomToExtent);
        return layer;
    },

    addKMLLayer:  function(name, url, canSelect, selectCallback, unselectCallback, args, loadCallback, zoomToExtent) {
        let layer = new OpenLayers.Layer.Vector(name, {
            projection: this.displayProjection,
            strategies: [new OpenLayers.Strategy.Fixed()],
            protocol: new OpenLayers.Protocol.HTTP({
                url: url,
                format: new OpenLayers.Format.KML({
                    extractStyles: true,
                    extractAttributes: true,
                    maxDepth: 2
                })
            }),
            //styleMap: this.getVectorLayerStyleMap(args)
        });
        if (!args) {
            args = {
                strokeColor: 'blue',
                strokeWidth: 2
            }
        }
        layer.styleMap = this.getVectorLayerStyleMap(layer, args);
        this.initMapVectorLayer(layer, canSelect, selectCallback, unselectCallback, loadCallback, zoomToExtent);
        return layer;
    },

    createXYZLayer:  function(name, url, attribution) {
        let options = {
            sphericalMercator: MapUtils.defaults.doSphericalMercator,
            numZoomLevels: MapUtils.defaults.zoomLevels,
            wrapDateLine: MapUtils.defaults.wrapDateline
        };
        if (attribution)
            options.attribution = attribution;
        return new OpenLayers.Layer.XYZ(name, url, options);
    },

    addBaseLayers:  function() {
        if (!this.mapLayers) {
            this.mapLayers = [
                map_osm,
                map_esri_topo,
                map_esri_street,
		map_esri_shaded,
		map_esri_lightgray,
		map_esri_darkgray,
		map_esri_physical,
		map_esri_terrain,
		map_esri_aeronautical,
                map_opentopo,
                map_usfs_ownership,
                map_usgs_topo,
                map_usgs_imagery,
                map_usgs_relief,
                map_osm_toner,
                map_osm_toner_lite,
                map_watercolor,
                map_weather,
                map_white,
		map_lightblue,
                map_gray,
                map_blue,
                map_black,
            ];
        }
        let dflt = this.defaultMapLayer || map_osm;
        if (!this.haveAddedDefaultLayer && dflt) {
            let index = this.mapLayers.indexOf(dflt);
            if (index >= 0) {
                this.mapLayers.splice(index, 1);
                this.mapLayers.splice(0, 0, dflt);
            }
        }

        this.firstLayer = null;
        this.hybridLayer = null;
        this.defaultOLMapLayer = null;

	this.baseLayers = {};
	this.numberOfBaseLayers = 0;

        for (let i = 0; i < this.mapLayers.length; i++) {
            let mapLayer = this.mapLayers[i];
            if (mapLayer == null) {
                continue;
            }
            let newLayer = null;
            if (mapLayer == map_osm) {
                let urls = [
                    '//a.tile.openstreetmap.org/${z}/${x}/${y}.png',
                    '//b.tile.openstreetmap.org/${z}/${x}/${y}.png',
                    '//c.tile.openstreetmap.org/${z}/${x}/${y}.png'
                ];
                newLayer = new OpenLayers.Layer.OSM("Open Street Map", urls);
            } else if (mapLayer == map_osm_toner) {
                let urls = ["http://a.tile.stamen.com/toner/${z}/${x}/${y}.png"];
                newLayer = new OpenLayers.Layer.OSM("OSM-Toner", urls);
            } else if (mapLayer == map_osm_toner_lite) {
                let urls = ["http://a.tile.stamen.com/toner-lite/${z}/${x}/${y}.png"];
                newLayer = new OpenLayers.Layer.OSM("OSM-Toner Lite", urls);
            } else if (mapLayer == map_watercolor) {
                let urls = ["http://c.tile.stamen.com/watercolor/${z}/${x}/${y}.jpg"];
                newLayer = new OpenLayers.Layer.OSM("Watercolor", urls);
            } else if (mapLayer == map_opentopo) {
                newLayer = this.createXYZLayer("OpenTopo", "//a.tile.opentopomap.org/${z}/${x}/${y}.png}");
            } else if (mapLayer == map_weather) {
		let maxZoom =  8;
                let wlayers = [ 
		    {name:'GOES Infrared',maxZoom:maxZoom,		    id:'goes-ir-4km-900913', alias:'goes-ir'},
		    {name:'GOES Water Vapor', maxZoom:maxZoom,id:'goes-wv-4km-900913', alias:'goes-wv'},
		    {name:'GOES Visible', maxZoom:maxZoom,id:'goes-vis-1km-900913', alias:'goes-visible'},
		    {name:'NWS Radar', maxZoom:maxZoom,id:'nexrad-n0q-900913',alias:'nexrad'},
		    {name:'24 hr precip', maxZoom:maxZoom,id:'q2-p24h-900913',alias:'precipition'}];

                wlayers = [ 
		    {name:'NWS Radar', maxZoom:maxZoom,id:'nexrad-n0q-900913',alias:'nexrad'}]


		let _this = this;
		let get_my_url = function(bounds) {
		    let res = _this.getMap().getResolution();
		    let z = _this.getMap().getZoom();
		    let x = Math.round((bounds.left - this.maxExtent.left) / (res * this.tileSize.w));
		    let y = Math.round((this.maxExtent.top - bounds.top) / (res * this.tileSize.h));
		    let path = z + "/" + x + "/" + y + "." + this.type + "?" + parseInt(Math.random() * 9999);
		    let url = this.url;
		    if (url instanceof Array) {
			url = this.selectUrl(path, url);
		    }
		    return url + this.service + "/" + this.layername + "/" + path;
		};


		wlayers.forEach(l=>{
                    let layer = new OpenLayers.Layer.TMS(
                        l.name,
                        'https://mesonet.agron.iastate.edu/cache/tile.py/', {
                            layername: l.id,
                            service: '1.0.0',
                            type: 'png',
                            visibility: false,
                            getURL: get_my_url,
                            isBaseLayer: false,
			    maxZoomLevel: l.maxZoom,
			    minZoomLevel: l.minZoom,			    
                        }, {}
                    );
		    let redrawFunc = () =>{
			if(layer.getVisibility()) {
			    layer.redraw(true);
			}
			setTimeout(redrawFunc,1000*60*5);
		    };
		    setTimeout(redrawFunc,1000*60*5);

		    this.baseLayers[l.id] = layer;
		    if(l.alias) this.baseLayers[l.alias] = layer;		    
                    layer.ramaddaId = l.id;
                    this.addLayer(layer,true);
                });

            } else if (mapLayer == map_white || mapLayer== map_lightblue || mapLayer == map_gray || mapLayer == map_blue || mapLayer == map_black || mapLayer == map_gray) {
		this.makeSimpleWms(mapLayer);
		continue;
            } else if (mapLayer == map_usgs_topo) {
                newLayer = this.createXYZLayer("USGS Topo",
					       "https://basemap.nationalmap.gov/ArcGIS/rest/services/USGSTopo/MapServer/tile/${z}/${y}/${x}",
					       'USGS - The National Map');
            } else if (mapLayer == map_usgs_imagery) {
                newLayer = this.createXYZLayer("USGS Imagery",
					       "https://basemap.nationalmap.gov/ArcGIS/rest/services/USGSImageryOnly/MapServer/tile/${z}/${y}/${x}",
					       "USGS - The National Map");
            } else if (mapLayer == map_usgs_relief) {
                newLayer = this.createXYZLayer("USGS Shaded Relief",
					       "https://basemap.nationalmap.gov/ArcGIS/rest/services/USGSShadedReliefOnly/MapServer/tile/${z}/${y}/${x}",
					       'USGS - The National Map');
            } else if (mapLayer == map_usfs_ownership) {
                newLayer = this.createXYZLayer("USFS Ownership",
					       "https://apps.fs.usda.gov/arcx/rest/services/wo_nfs_gstc/GSTC_TravelAccessBasemap_01/MapServer/tile/${z}/${y}/${x}");
            } else if (mapLayer == map_esri_worldimagery) {
                //Not working
                newLayer = this.createXYZLayer("ESRI World Imagery",
					       "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}");

            } else if (mapLayer == map_esri_topo) {
                newLayer = this.createXYZLayer("ESRI Topo",
					       "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/${z}/${y}/${x}");
	    } else if(mapLayer==map_esri_aeronautical) {
                newLayer = this.addWMSLayer("ESRI Aeronautical","https://wms.chartbundle.com/mp/service","sec", true);
            } else if (mapLayer == map_esri_darkgray) {
		newLayer = this.createXYZLayer("ESRI Dark Gray",
					       "https://services.arcgisonline.com/arcgis/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/${z}/${y}/${x}");
	    } else if (mapLayer == map_esri_lightgray) {
		newLayer = this.createXYZLayer("ESRI Light Gray",
					       "https://services.arcgisonline.com/arcgis/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/${z}/${y}/${x}");
            } else if (mapLayer == map_esri_terrain) {
		newLayer = this.createXYZLayer("ESRI Terrain",
					       "https://server.arcgisonline.com/ArcGIS/rest/services/World_Terrain_Base/MapServer/tile/${z}/${y}/${x}");
            } else if (mapLayer == map_esri_shaded) {
		newLayer = this.createXYZLayer("ESRI Shaded Relief",
					       "https://server.arcgisonline.com/ArcGIS/rest/services/World_Shaded_Relief/MapServer/tile/${z}/${y}/${x}");
            } else if (mapLayer == map_esri_physical) {
		newLayer = this.createXYZLayer("ESRI Physical",
					       "https://server.arcgisonline.com/ArcGIS/rest/services/World_Physical_Map/MapServer/tile/${z}/${y}/${x}");
            } else if (mapLayer == map_esri_street) {
                newLayer = this.createXYZLayer("ESRI Streets",
					       "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/${z}/${y}/${x}");
            } else if (/\/tile\//.exec(mapLayer)) {
                let layerURL = mapLayer;
                newLayer = new OpenLayers.Layer.XYZ(
                    "ESRI China Map", layerURL, {
                        sphericalMercator: MapUtils.defaults.doSphericalMercator,
                        numZoomLevels: MapUtils.defaults.zoomLevels,
                        wrapDateLine: MapUtils.defaults.wrapDateline
                    });
            } else if (mapLayer == map_ms_shaded) {
                newLayer = new OpenLayers.Layer.VirtualEarth(
                    "Virtual Earth - Shaded", {
                        'type': VEMapStyle.Shaded,
                        sphericalMercator: MapUtils.defaults.doSphericalMercator,
                        wrapDateLine: MapUtils.defaults.wrapDateline,
                        numZoomLevels: MapUtils.defaults.zoomLevels
                    });
            } else if (mapLayer == map_ms_hybrid) {
                newLayer = new OpenLayers.Layer.VirtualEarth(
                    "Virtual Earth - Hybrid", {
                        'type': VEMapStyle.Hybrid,
                        sphericalMercator: MapUtils.defaults.doSphericalMercator,
                        numZoomLevels: MapUtils.defaults.zoomLevels,
                        wrapDateLine: MapUtils.defaults.wrapDateline
                    });
            } else if (mapLayer == map_ms_aerial) {
                newLayer = new OpenLayers.Layer.VirtualEarth(
                    "Virtual Earth - Aerial", {
                        'type': VEMapStyle.Aerial,
                        sphericalMercator: MapUtils.defaults.doSphericalMercator,
                        numZoomLevels: MapUtils.defaults.zoomLevels,
                        wrapDateLine: MapUtils.defaults.wrapDateline
                    });
            } else {
                let match = /wms:(.*),(.*),(.*)/.exec(mapLayer);
                if (!match) {
                    alert("no match for map layer:" + mapLayer);
                    continue;
                }
                this.addWMSLayer(match[1], match[2], match[3], true);
            }

            if (this.firstLayer == null) {
                this.firstLayer = newLayer;
            }
            if (newLayer != null) {
		this.baseLayers[mapLayer] = newLayer;
                newLayer.ramaddaId = mapLayer;
                if (mapLayer == this.defaultMapLayer) {
                    this.defaultOLMapLayer = newLayer;
                }
                this.addLayer(newLayer);
		this.numberOfBaseLayers++;
            }

        }


        this.graticule = new OpenLayers.Control.Graticule({
            layerName: "Lat/Lon Lines",
            numPoints: 2,
            labelled: true,
            visible: false
        });
        this.getMap().addControl(this.graticule);
    },

    getBaseLayer: function(id) {
	if(this.baseLayers) return this.baseLayers[id];
    },
    makeSimpleWms:  function(mapLayer) {
	let url = "/repository/wms?version=1.1.1&request=GetMap&layers=" + mapLayer +"&FORMAT=image%2Fpng&SRS=EPSG%3A4326&BBOX=-180.0,-80.0,180.0,80.0&width=400&height=400"
	this.addWMSLayer(Utils.makeLabel(mapLayer) +" background", url, mapLayer, true);
    },




    initSearch:  function(inputId) {
	let _this = this;
        $("#" + inputId).keyup(function(e) {
            if (e.keyCode == 13) {
                _this.searchMarkers($("#" + inputId).val());
            }
        });
    },



    searchMarkers:  function(text) {
        text = text.trim();
        text = text.toLowerCase();
        let all = text == "";
        let cbxall = $(':input[id*=\"' + "visibleall_" + this.mapId + '\"]');
        cbxall.prop('checked', all);
        let bounds = new OpenLayers.Bounds();
        let cnt = 0;
        if (this.markers) {
            let list = this.getMarkers();
            for (let idx = 0; idx < list.length; idx++) {
                marker = list[idx];
                let visible = true;
                let cbx = $('#' + "visible_" + this.mapId + "_" + marker.ramaddaId);
                let block = $('#' + "block_" + this.mapId + "_" + marker.ramaddaId);
                let name = marker.name;
                if (all) visible = true;
                else if (!Utils.isDefined(name)) {
                    visible = false;
                } else {
                    visible = name.toLowerCase().includes(text);
                }
                if (visible) {
                    marker.style.display = 'inline';
                    if (marker.location)
                        bounds.extend(marker.location);
                    cnt++;
                } else {
                    marker.style.display = 'none';
                }
                if (visible)
                    block.show();
                else
                    block.hide();
                cbx.prop('checked', visible);
            }
            this.markers.redraw();
        }
        if (this.boxes && this.boxes.markers) {
            for (let marker in this.boxes.markers) {
                marker = this.boxes.markers[marker];
                name = marker.name;
                let visible = true;
                if (all) visible = true;
                else if (!Utils.isDefined(name)) {
                    visible = false;
                } else {
                    visible = name.toLowerCase().includes(text);
                }
                marker.display(visible);
                if (visible) {
                    let b = this.transformProjBounds(marker.bounds);
                    bounds.extend(b);
                    cnt++;
                }
            }
            this.boxes.redraw();
        }

        if (this.lines) {
            let features = this.lines.features;
            for (let i = 0; i < features.length; i++) {
                let line = features[i];
                name = line.name;
                let visible = true;
                if (all) visible = true;
                else if (!Utils.isDefined(name)) {
                    visible = false;
                } else {
                    visible = name.toLowerCase().includes(text);
                }
                if (visible) {
                    line.style.display = 'inline';
                } else {
                    line.style.display = 'none';
                }
            }
            this.lines.redraw();
        }
        if (cnt > 0) {
            this.centerOnMarkers(bounds, true);
        }
    },

    setLatLonReadout:  function(llr) {
        this.latlonReadout = llr;
    },

    getMap:  function() {
	if(getMapDebug)
	    console.trace();
        return this.map;
    },
    initMap:  function(doRegion) {
	let _this = this;
        this.startTime = Date.now();
        if (this.inited)
            return;
        this.inited = true;
        if (this.enableDragPan) {
            this.getMap().addControl(new OpenLayers.Control.Navigation({
                zoomWheelEnabled: this.scrollToZoom,
                dragPanOptions: {
                    enableKinetic: true
                }
            }));
        }

        /*this.getMap().addControl(new OpenLayers.Control.TouchNavigation({
          dragPanOptions: {
          enableKinetic: true
          }
          }));*/


        if (this.showZoomPanControl && !this.showZoomOnlyControl) {
            this.getMap().addControl(new OpenLayers.Control.PanZoom());
        }
        if (this.showZoomOnlyControl && !this.showZoomPanControl) {

            this.getMap().addControl(new OpenLayers.Control.Zoom());
        }
        if (this.showScaleLine) {
            this.getMap().addControl(new OpenLayers.Control.ScaleLine());
        }
//	this.getMap().addControl(new OpenLayers.Control.OverviewMap());

        let keyboardControl = new OpenLayers.Control();
        let control = new OpenLayers.Control();
        let callbacks = {
            keydown: function(evt) {
                if (evt.keyCode == 79) {
                    if (!_this.imageLayers) return;
                    for (id in _this.imageLayers) {
                        image = _this.imageLayers[id];
                        if (!Utils.isDefined(image.opacity)) {
                            image.opacity = 1.0;
                        }
                        opacity = image.opacity;
                        if (evt.shiftKey) {
                            opacity += .1;
                        } else {
                            opacity -= 0.1;
                        }
                        if (opacity < 0) opacity = 0;
                        else if (opacity > 1) opacity = 1;
                        image.setOpacity(opacity);
                    }
                }
                if (evt.keyCode == 84) {
                    if (_this.selectImage) {
                        _this.selectImage.setVisibility(!_this.selectImage.getVisibility());
                    }
                }
            }

        };
        let handler = new OpenLayers.Handler.Keyboard(control, callbacks, {});
        handler.activate();
        this.getMap().addControl(keyboardControl);
        this.getMap().addControl(new OpenLayers.Control.KeyboardDefaults());

        if (this.showLayerSwitcher) {
            this.getMap().addControl(new OpenLayers.Control.LayerSwitcher());
        }


        if (this.showLatLonPosition) {
            if (!this.latlonReadout)
                this.latlonReadout = this.mapId + "_latlonreadout";
            let latLonReadout = GuiUtils.getDomObject(this.latlonReadout);
            if (latLonReadout) {
                this.getMap().addControl(new OpenLayers.Control.MousePosition({
                    numDigits: 5,
                    element: latLonReadout.obj,
                    prefix: "Position: "
                }));
            } else {
                this.getMap().addControl(new OpenLayers.Control.MousePosition({
                    numDigits: 5,
                    prefix: "Position: "
                }));
            }
        }
        if (this.showLocationSearch) {
            this.initLocationSearch();
        }

        if (this.defaultBounds) {
            this.hadDefaultBounds = true;
	}

        if (false && this.defaultLocation && !this.defaultBounds) {
            let center = this.defaultLocation;
            let offset = 10.0;
            this.defaultBounds = MapUtils.createBounds(center.lon - offset, center.lat - offset, center.lon + offset, center.lat + offset);
	    if(debugBounds)
		console.log("setting default bounds-2:" + center.lon +" " + center.lat +" bounds:" + this.defaultBounds);
            this.defaultLocation = null;
        }

	this.applyDefaultLocation();

        for (let i = 0; i < this.initialLayers.length; i++) {
            this.addLayer(this.initialLayers[i]);
        }
        this.initialLayers = [];

        if (doRegion) {
            this.addRegionSelectorControl();
        }

        let cbx = $(':input[id*=\"' + "visible_" + this.mapId + '\"]');
        cbx.change(function(event) {
            _this.checkImageLayerVisibility();
            _this.checkMarkerVisibility();
            _this.checkLinesVisibility();
            _this.checkBoxesVisibility();
        });

        let cbxall = $(':input[id*=\"' + "visibleall_" + this.mapId + '\"]');
        cbxall.change(function(event) {
            cbx.prop("checked", cbxall.is(':checked'));
            _this.checkImageLayerVisibility();
            _this.checkMarkerVisibility();
            _this.checkLinesVisibility();
            _this.checkBoxesVisibility();

            //                cbx.prop("checked", cbxall.is(':checked')).trigger("change");
        });
    },

    applyDefaultLocation:  function() {	
	if(debugBounds)
	    console.log("apply default location:" + this.defaultLocation);
	if(this.defaultLocation) {
            let projPoint = this.transformLLPoint(this.defaultLocation);
            this.getMap().setCenter(projPoint);
	    if(!(this.initialZoom>=0)) {
		this.getMap().zoomTo(4);
	    }
	    this.defaultLocation = null;
	} else  if (this.defaultBounds) {
            let llPoint = this.defaultBounds.getCenterLonLat();
            let projPoint = this.transformLLPoint(llPoint);
            this.getMap().setCenter(projPoint);
            this.zoomToExtent(this.transformLLBounds(this.defaultBounds));
            this.defaultBounds = null;
        } else {
            this.getMap().zoomToMaxExtent();
        }

	if(this.initialZoom>=0) {
	    if(debugBounds)
		console.log("initial zoom:" + this.initialZoom);
	    let zoom = this.initialZoom;
	    this.initialZoom=-1;
	    this.getMap().zoomTo(zoom);
	    //In case we are in tabs then set the zoom level later
	    if(true || this.initialZoomTimeout) {
		setTimeout(()=>{
		    this.getMap().zoomTo(zoom);
		},this.initialZoomTimeout);
	    }
	}
    },


    removeSearchMarkers:  function() {
        if (!this.searchMarkerList) return;
        for (let i = 0; i < this.searchMarkerList.length; i++) {
            this.removeMarker(this.searchMarkerList[i]);
        }
    },


    addAllLocationResults:  function() {
        this.removeSearchMarkers();
        this.searchMarkerList = [];
        if (!this.locationSearchResults) return;
        let east, west, north, south;
        for (let i = 0; i < this.locationSearchResults.length; i++) {
            let result = this.locationSearchResults[i];
            let lonlat = new MapUtils.createLonLat(result.longitude, result.latitude);
            let icon = result.icon;
            if (!icon)
                icon = ramaddaBaseUrl + "/icons/green-dot.png";
            this.searchMarkerList.push(this.addMarker("search", lonlat, icon, "", result.name, 20, 20));
            east = i == 0 ? result.longitude : Math.max(east, result.longitude);
            west = i == 0 ? result.longitude : Math.min(west, result.longitude);
            north = i == 0 ? result.latitude : Math.max(north, result.latitude);
            south = i == 0 ? result.latitude : Math.min(south, result.latitude);
        }
        let bounds = this.transformLLBounds(MapUtils.createBounds(west, south, east, north));
        this.zoomToExtent(bounds);
    },

    initLocationSearch:  function() {
        if (this.selectRegion) return;
        let _this = this;
        let input = HtmlUtils.span(["style", "padding-right:4px;", "id", this.mapDivId + "_loc_search_wait"], "") +
            HtmlUtils.checkbox(this.mapDivId + "_loc_bounds", ["title", "Search in map bounds"], false) + HtmlUtils.span(["title", "Search in map bounds"], " In view ") +
            HtmlUtils.input("", "", ["class", "ramadda-map-loc-input", "title", "^string - matches beginning", "size", "30", "placeholder", "Search location", "id", this.mapDivId + "_loc_search"])
        $("#" + this.mapDivId + "_footer2").html(input);
        let searchInput = $("#" + this.mapDivId + "_loc_search");
        let bounds = $("#" + this.mapDivId + "_loc_bounds");
        let searchPopup = $("#" + this.mapDivId + "_loc_popup");
        let wait = $("#" + this.mapDivId + "_loc_search_wait");
        searchInput.blur(function(e) {
            setTimeout(function() {
                wait.html("");
                searchPopup.hide();
            }, 250);
        });
        searchInput.on('input', function(e) {
            searchPopup.hide();
            if (searchInput.val() == "") {
                _this.removeSearchMarkers();
            }
        });

        searchInput.keypress(function(e) {
            let keyCode = e.keyCode || e.which;
            if (keyCode == 27) {
                searchPopup.hide();
                return;
            }
            if (keyCode != 13) {
                return;
            }
            wait.html(HtmlUtils.image(icon_wait));
            let url = ramaddaBaseUrl + "/geocode?query=" + encodeURIComponent(searchInput.val());
            if (bounds.is(':checked')) {
                let b = _this.transformProjBounds(_this.getMap().getExtent());
                url += "&bounds=" + b.top + "," + b.left + "," + b.bottom + "," + b.right;
            }
            let jqxhr = $.getJSON(url, function(data) {
                wait.html("");
                let result = HtmlUtils.openTag("div", ["style", "max-height:400px;overflow-y:auto;"]);
                if (data.result.length == 0) {
                    wait.html("Nothing found");
                    return;
                } else {
                    _this.locationSearchResults = data.result;
                    for (let i = 0; i < data.result.length; i++) {
                        let n = data.result[i].name.replace("\"", "'");
                        let icon = data.result[i].icon;
                        if (!icon)
                            icon = ramaddaBaseUrl + "/icons/green-dot.png";
                        result += HtmlUtils.div(["class", "ramadda-map-loc", "name", n, "icon", icon, "latitude", data.result[i].latitude, "longitude", data.result[i].longitude], "<img width='16' src=" + icon + "> " + data.result[i].name);
                    }
                    result += HtmlUtils.div(["class", "ramadda-map-loc", "name", "all"], "Show all");
                }
                let my = "left bottom";
                let at = "left top";
                result += HtmlUtils.closeTag("div");
                searchPopup.html(result);
                HtmlUtils.setPopupObject(searchPopup);
                searchPopup.show();
                searchPopup.position({
                    of: searchInput,
                    my: my,
                    at: at,
                    collision: "fit fit"
                });
                searchPopup.find(".ramadda-map-loc").click(function() {
                    searchPopup.hide();
                    let name = $(this).attr("name");
                    if (name == "all") {
                        _this.addAllLocationResults();
                        return;
                    }
                    let lat = parseFloat($(this).attr("latitude"));
                    let lon = parseFloat($(this).attr("longitude"));
                    let icon = $(this).attr("icon");
                    let offset = 0.05;
                    let bounds = _this.transformLLBounds(MapUtils.createBounds(lon - offset, lat - offset, lon + offset, lat + offset));
                    let lonlat = new MapUtils.createLonLat(lon, lat);
                    _this.removeSearchMarkers();
                    _this.searchMarkerList = [];
                    _this.searchMarkerList.push(_this.addMarker("search", lonlat, icon, "", name, 20, 20));
                    //Only zoom  if its a zoom in
                    let b = _this.transformProjBounds(_this.getMap().getExtent());
                    if (Math.abs(b.top - b.bottom) > offset) {
                        _this.zoomToExtent(bounds);
                    } else {
                        _this.setCenter(MapUtils.createLonLat(lon, lat));
                    }
                });
            }).fail(function(jqxhr, textStatus, error) {
                wait.html("Error:" + error);
            });
        });
    },

    addVectorLayer:  function(layer, canSelect) {
        this.addLayer(layer);
        this.vectorLayers.push(layer);
        if (this.getCanSelect(canSelect)) {
            let _this = this;
            if (!this.getMap().featureSelect) {
                this.getMap().featureSelect = new OpenLayers.Control.SelectFeature([layer], {
                    multiple: false,
                    hover: this.selectOnHover,
                    onSelect: function(feature) {
                        _this.showMarkerPopup(feature, true);
                    }
                });
                /*
                  if (this.highlightOnHover) {
                  this.getMap().highlightSelect = new OpenLayers.Control.SelectFeature(layer, {
                  multiple: false, 
                  hover: true,
                  highlightOnly: true,
                  renderIntent: "temporary"
                  });
                  this.getMap().addControl(this.getMap().highlightSelect);
                  this.getMap().highlightSelect.activate();   
                  }*/

                //for now
                //this.getMap().addControl(this.getMap().featureSelect);
                //                this.getMap().featureSelect.activate();
            } else {
		this.getMap().featureSelect.layers.push(layer);
		//		this.getMap().featureSelect.layers = Utils.mergeLists(tmp,this.getMap().featureSelect.layers);
                /*
                  if(this.getMap().highlightSelect) {
                  this.getMap().highlightSelect.setLayer(this.vectorLayers);
                  }
                */
            }
        }
        this.checkLayerOrder();
    },

    isLayerVisible:  function(id, parentId) {
        //        let cbx =   $(':input[id*=\"' + "visible_" + this.mapId +"_" + id+'\"]');
        let cbx = $('#' + "visible_" + this.mapId + "_" + id);
        if (cbx.length == 0 && parentId != null) cbx = $('#' + "visible_" + this.mapId + "_" + parentId);
        if (cbx.length == 0) return true;
        return cbx.is(':checked');
    },

    initForDrawing:  function() {
	let _this = this;
        if (!_this.drawingLayer) {
            this.drawingLayer = new OpenLayers.Layer.Vector("Drawing");
            this.addLayer(_this.drawingLayer);
        }
        this.drawControl = new OpenLayers.Control.DrawFeature(
            _this.drawingLayer, OpenLayers.Handler.Point);
        // _this.drawControl.activate();
        this.getMap().addControl(_this.drawControl);
    },

    drawingFeatureAdded:  function(feature) {
        // alert(feature);
    },

    addClickHandler:  function(lonfld, latfld, zoomfld, object) {
        this.lonFldId = lonfld;
        this.latFldId = latfld;

        if (this.clickHandler)
            return;
        if (!this.map) {
            return;
	}
        this.clickHandler = new OpenLayers.Control.Click();
        this.clickHandler.setLatLonZoomFld(lonfld, latfld, zoomfld, object);
        this.clickHandler.setTheMap(this);
        this.getMap().addControl(this.clickHandler);
        this.clickHandler.activate();
	return this.clickHandler;
    },

    setSelection:  function(argBase, doRegion, absolute) {
        this.selectRegion = doRegion;
        this.argBase = argBase;
        if (!GuiUtils) {
            return;
        }
        this.fldNorth = GuiUtils.getDomObject(this.argBase + "_north");
        if (this.fldNorth == null)
            this.fldNorth = GuiUtils.getDomObject(this.argBase + ".north");
        if (this.fldNorth == null)
            this.fldNorth = GuiUtils.getDomObject(this.mapId + "_north");


        this.fldSouth = GuiUtils.getDomObject(this.argBase + "_south");
        if (!this.fldSouth)
            this.fldSouth = GuiUtils.getDomObject(this.argBase + ".south");
        if (this.fldSouth == null)
            this.fldSouth = GuiUtils.getDomObject(this.mapId + "_south");

        this.fldEast = GuiUtils.getDomObject(this.argBase + "_east");
        if (!this.fldEast)
            this.fldEast = GuiUtils.getDomObject(this.argBase + ".east");
        if (this.fldEast == null)
            this.fldEast = GuiUtils.getDomObject(this.mapId + "_east");

        this.fldWest = GuiUtils.getDomObject(this.argBase + "_west");
        if (!this.fldWest)
            this.fldWest = GuiUtils.getDomObject(this.argBase + ".west");

        if (this.fldWest == null)
            this.fldWest = GuiUtils.getDomObject(this.mapId + "_west");

        this.fldLat = GuiUtils.getDomObject(this.argBase + "_latitude");
        if (!this.fldLat)
            this.fldLat = GuiUtils.getDomObject(this.argBase + ".latitude");

        if (this.fldLat == null)
            this.fldLat = GuiUtils.getDomObject(this.mapId + "_latitude");


        this.fldLon = GuiUtils.getDomObject(this.argBase + "_longitude");
        if (!this.fldLon)
            this.fldLon = GuiUtils.getDomObject(this.argBase + ".longitude");
        if (this.fldLon == null)
            this.fldLon = GuiUtils.getDomObject(this.mapId + "_longitude");


        if (this.fldLon) {
            this.addClickHandler(this.fldLon.id, this.fldLat.id);
            this.setSelectionMarker(this.fldLon.obj.value, this.fldLat.obj.value);
        }
    },

    selectionPopupInit:  function() {
        if (!this.inited) {
            this.initMap(this.selectRegion);
            if (this.argBase && !this.fldNorth) {
                this.setSelection(this.argBase);
            }

            if (this.fldNorth) {
                this.setSelectionBox(this.fldNorth.obj.value,
				     this.fldWest.obj.value, this.fldSouth.obj.value,
				     this.fldEast.obj.value, true);
            }

            if (this.fldLon) {
                this.addClickHandler(this.fldLon.id, this.fldLat.id);
                this.setSelectionMarker(this.fldLon.obj.value, this.fldLat.obj.value);
            }
        }
	this.map.updateSize();
    },

    setSelectionBoxFromFields:  function(zoom) {
        if (this.fldNorth) {
            // alert("north = " + this.fldNorth.obj.value);
            this.setSelectionBox(this.fldNorth.obj.value,
				 this.fldWest.obj.value, this.fldSouth.obj.value,
				 this.fldEast.obj.value, true);
            if (this.selectorBox) {
                let boxBounds = this.selectorBox.bounds
                this.getMap().setCenter(boxBounds.getCenterLonLat());
                if (zoom) {
                    this.zoomToExtent(boxBounds);
                }
            }
        }
    },

    toggleSelectorBox:  function(toggle) {
        if (this.selectorControl) {
            if (toggle) {
                this.selectorControl.activate();
                this.selectorControl.box.activate();
            } else {
                this.selectorControl.deactivate();
                this.selectorControl.box.deactivate();
            }
        }
    },

    resetExtent:  function() {
	if(debugBounds)
	    console.log("resetExtent");
        this.getMap().zoomToMaxExtent();
    },

    // Assume that north, south, east, and west are in degrees or
    // some variant thereof
    setSelectionBox:  function(north, west, south, east, centerView) {
        if (north == "" || west == "" || south == "" || east == "")
            return;
        let bounds = MapUtils.createBounds(west, Math.max(south,
						 -MapUtils.defaults.maxLatValue), east, Math.min(north, MapUtils.defaults.maxLatValue));
        if (!this.selectorBox) {
            let args = {
                "color": "red",
                "selectable": false
            };
            this.selectorBox = this.createBox("", "Selector box", north, west, south, east, "", args);
        } else {
            this.selectorBox.bounds = this.transformLLBounds(bounds);
            // this.selectorBox.bounds = bounds;
        }
        if (centerView) {
	    if(debugBounds)
		console.log("calling setViewToBounds-1");
            this.setViewToBounds(bounds)
        }

        if (this.boxes) {
            this.boxes.redraw();
        }

        if (this.selectImage) {
            let imageBounds = MapUtils.createBounds(west, south, east, north);
            imageBounds = this.transformLLBounds(imageBounds);
            this.selectImage.extent = imageBounds;
            this.selectImage.redraw();
        }

    },

    clearSelectionMarker:  function() {
        if (this.selectorMarker != null) {
            this.removeMarker(this.selectorMarker);
            this.selectorMarker = null;
        }
    },

    clearSelectedFeatures:  function() {
        if (this.getMap().controls != null) {
            let myControls = this.getMap().controls;
            for (i = 0; i < myControls.length; i++) {
                if (myControls[i].displayClass == "olControlSelectFeature") {
                    myControls[i].unselectAll();
                }
            }
        }
    },

    setSelectionMarker:  function(lon, lat, andCenter, zoom) {
        if (!lon || !lat || lon == "" || lat == "")
            return;
        if (this.lonFldId != null) {
            $("#" + this.lonFldId).val(MapUtils.formatLocationValue(lon));
            $("#" + this.latFldId).val(MapUtils.formatLocationValue(lat));
        }


        let lonlat = new MapUtils.createLonLat(lon, lat);
        if (this.selectorMarker == null) {
            this.selectorMarker = this.addMarker(MapUtils.POSITIONMARKERID, lonlat, "", "", "", 20, 10);
        } else {
            this.selectorMarker.lonlat = this.transformLLPoint(lonlat);
        }
        if (this.markers) {
            this.markers.redraw();
        }
        if (andCenter) {
	    if(debugBounds)
		console.log("setSelectionMarker-1");
            this.getMap().setCenter(this.selectorMarker.lonlat);
        }
        if (zoom) {
	    if(debugBounds)
		console.log("setSelectionMarker-2");

            if (zoom.zoomOut) {
                let level = this.getMap().getZoom();
                level--;
                if (this.getMap().isValidZoomLevel(level)) {
                    this.getMap().zoomTo(level);
                }
                return;
            }
            if (zoom.zoomIn) {
                let level = this.getMap().getZoom();
                level++;
                if (this.getMap().isValidZoomLevel(level)) {
                    this.getMap().zoomTo(level);
                }
                return;
            }

            let offset = zoom.offset;
            if (offset) {
                let bounds = this.transformLLBounds(MapUtils.createBounds(lon - offset, lat - offset, lon + offset, lat + offset));
                this.zoomToExtent(bounds);
            }
        }
    },

    transformLLBounds:  function(bounds) {
        if (!bounds)
            return;
	if(Utils.isDefined(bounds.north))
            bounds= MapUtils.createBounds(bounds.west,bounds.south, bounds.east,bounds.north);
        let llbounds = bounds.clone();
        return llbounds.transform(this.displayProjection, this.sourceProjection);
    },

    transformLLPoint:  function(point) {
        if (!point)
            return null;
        let llpoint = point.clone();
        return llpoint.transform(this.displayProjection, this.sourceProjection);
    },

    transformProjBounds:  function(bounds) {
        if (!bounds)
            return;
        let projbounds = bounds.clone();
        return projbounds.transform(this.sourceProjection, this.displayProjection);
    },

    transformProjPoint:  function(point) {
        if (!point)
            return;
        let projpoint = point.clone();
        return projpoint.transform(this.sourceProjection, this.displayProjection);
    },

    normalizeBounds:  function(bounds) {
        if (!this.map) {
            return bounds;
        }
        let newBounds = bounds;
        let newLeft = bounds.left;
        let newRight = bounds.right;
        let extentBounds = this.getMap().restrictedExtent;
        if (!extentBounds) {
            extentBounds = this.maxExtent;
        }
        if (!extentBounds) {
            return bounds;
        }
        /*
         * if (extentBounds.left < 0) { // map is -180 to 180 if (bounds.right >
         * 180) { //bounds is 0 to 360 newLeft = bounds.left-360; newRight =
         * bounds.right-360; } } else { // map is 0 to 360
         */
        if (extentBounds.left >= 0) { // map is 0 to 360+
            if (bounds.left < 0) { // left edge is -180 to 180
                newLeft = bounds.left + 360;
            }
            if (bounds.right < 0) { // right edge is -180 to 180
                newRight = bounds.right + 360;
            }
        }
        // just account for crossing the dateline
        if (newLeft > newRight) {
            newRight = bounds.right + 360;
        }
        newLeft = Math.max(newLeft, extentBounds.left);
        newRight = Math.min(newRight, extentBounds.right);
        newBounds = MapUtils.createBounds(newLeft, bounds.bottom, newRight,
				 bounds.top);
        return newBounds;
    },

    findSelectionFields:  function() {
        if (this.argBase && !(this.fldNorth || this.fldLon)) {
            this.setSelection(this.argBase);
        }
    },

    selectionClear:  function() {
        this.findSelectionFields();
	HU.addToDocumentUrl("map_bounds","");

        if (this.fldNorth) {
            this.fldNorth.obj.value = "";
            this.fldSouth.obj.value = "";
            this.fldWest.obj.value = "";
            this.fldEast.obj.value = "";
        } else if (this.fldLat) {
            this.fldLon.obj.value = "";
            this.fldLat.obj.value = "";
        }
        if (this.selectorBox && this.boxes) {
            this.boxes.removeMarker(this.selectorBox);
            this.selectorBox = null;
        }
        if (this.selectorMarker && this.markers) {
            this.removeMarker(this.selectorMarker);
            this.selectorMarker = null;
        }
    },

    clearMarkers:  function() {
        if (!this.markers) return;
        let markers = this.getMarkers();
        this.markers.removeFeatures(markers);
        this.markers.redraw();
    },
    getMarkers:  function() {
        if (this.markers == null) return [];
        return this.markers.features;
    },
    clearRegionSelector:  function(listener) {
        if (!this.selectorControl)
            return;
        if (this.selectorControl.box) {
            //Nothing here really works to hide the box
            this.selectorControl.box.removeBox();
        }
    },
    addRegionSelectorControl:  function(listener) {
        let _this = this;
        if (_this.selectorControl)
            return;
        _this.selectorListener = listener;

        _this.selectorControl = new OpenLayers.Control();
        OpenLayers.Util.extend(_this.selectorControl, {
            draw: function() {
                this.box = new OpenLayers.Handler.Box(_this.selectorControl, {
                    "done": this.notice
                }, {
                    keyMask: OpenLayers.Handler.MOD_SHIFT,
                    xxxboxDivClassName: "map-drag-box"
                });
                this.box.activate();
            },

            notice: function(bounds) {
                let ll = _this.getMap().getLonLatFromPixel(new OpenLayers.Pixel(
                    bounds.left, bounds.bottom));
                let ur = _this.getMap().getLonLatFromPixel(new OpenLayers.Pixel(
                    bounds.right, bounds.top));
                ll = _this.transformProjPoint(ll);
                ur = _this.transformProjPoint(ur);
                bounds = MapUtils.createBounds(ll.lon, ll.lat, ur.lon,
					  ur.lat);
                bounds = _this.normalizeBounds(bounds);
                _this.setSelectionBox(bounds.top, bounds.left, bounds.bottom, bounds.right, false);
                _this.findSelectionFields();
                if (listener) {
                    listener(bounds);
                }
                if (_this.fldNorth) {
                    _this.fldNorth.obj.value = MapUtils.formatLocationValue(bounds.top);
                    _this.fldSouth.obj.value = MapUtils.formatLocationValue(bounds.bottom);
                    _this.fldWest.obj.value = MapUtils.formatLocationValue(bounds.left);
                    _this.fldEast.obj.value = MapUtils.formatLocationValue(bounds.right);
		    HU.addToDocumentUrl("map_bounds",bounds.top+"," + bounds.left+"," + bounds.bottom +","+bounds.right);
                }
            }
        });
        _this.map.addControl(_this.selectorControl);
        _this.panControl = new OpenLayers.Control();
        OpenLayers.Util.extend(_this.panControl, {
            draw: function() {
                this.box = new OpenLayers.Handler.Drag(_this.panControl, {
                    "down": this.down,
                    "move": this.move
                }, {
                    keyMask: OpenLayers.Handler.MOD_META,
                    boxDivClassName: "map-drag-box"
                });
                this.box.activate();
            },

            down: function(pt) {
                this.firstPoint = _this.getMap().getLonLatFromPixel(new OpenLayers.Pixel(
                    pt.x, pt.y));
                this.firstPoint = _this.transformProjPoint(this.firstPoint);
                _this.findSelectionFields();
                if (_this.fldNorth) {
                    n = this.origNorth = parseFloat(_this.fldNorth.obj.value);
                    s = this.origSouth = parseFloat(_this.fldSouth.obj.value);
                    w = this.origWest = parseFloat(_this.fldWest.obj.value);
                    e = this.origEast = parseFloat(_this.fldEast.obj.value);
                    pt = this.firstPoint;
                    this.doWest = false;
                    this.doSouth = false;
                    this.doEast = false;
                    this.doNorth = false;
                    if (pt.lon <= e && pt.lon >= w && pt.lat <= n && pt.lat >= s) {
                        this.doSouth = this.doWest = this.doEast = this.doNorth = true;
                        this.type = "center";
                    } else if (pt.lon > e) {
                        if (pt.lat > n) {
                            this.type = "ne";
                            this.doEast = this.doNorth = true;
                        } else if (pt.lat < s) {
                            this.type = "se";
                            this.doSouth = this.doEast = true;
                        } else {
                            this.type = "e";
                            this.doEast = true;
                        }
                    } else if (pt.lon < w) {
                        if (pt.lat > n) {
                            this.type = "nw";
                            this.doWest = this.doNorth = true;
                        } else if (pt.lat < s) {
                            this.type = "sw";
                            this.doSouth = this.doWest = true;
                        } else {
                            this.type = "w";
                            this.doWest = true;
                        }
                    } else if (pt.lat > n) {
                        this.type = "n";
                        this.doNorth = true;
                    } else {
                        this.type = "s";
                        this.doSouth = true;
                    }
                }
            },
            move: function(pt) {
                let ll = _this.getMap().getLonLatFromPixel(new OpenLayers.Pixel(pt.x, pt.y));
                ll = _this.transformProjPoint(ll);
                dx = ll.lon - this.firstPoint.lon;
                dy = ll.lat - this.firstPoint.lat;
                newWest = this.origWest;
                newEast = this.origEast;
                newSouth = this.origSouth;
                newNorth = this.origNorth;
                if (this.doWest)
                    newWest += dx;
                if (this.doSouth)
                    newSouth += dy;
                if (this.doEast)
                    newEast += dx;
                if (this.doNorth)
                    newNorth += dy;
                let bounds = MapUtils.createBounds(newWest, newSouth, newEast, newNorth);
                bounds = _this.normalizeBounds(bounds);
                _this.setSelectionBox(bounds.top, bounds.left, bounds.bottom, bounds.right, false);
                _this.findSelectionFields();
                if (!_this.fldNorth) return;
                _this.fldNorth.obj.value = MapUtils.formatLocationValue(bounds.top);
                _this.fldSouth.obj.value = MapUtils.formatLocationValue(bounds.bottom);
                _this.fldWest.obj.value = MapUtils.formatLocationValue(bounds.left);
                _this.fldEast.obj.value = MapUtils.formatLocationValue(bounds.right);

            }
        });
        _this.map.addControl(_this.panControl);
    },

    closePopup:  function(evt) {
        if (this.currentPopup) {
	    this.getMap().removePopup(this.currentPopup);
            this.currentPopup.destroy();
            this.currentPopup = null;
            this.hiliteBox('');
            if (this.selectedFeature) {
                this.unselectFeature(this.selectedFeature);
            }
        }
    },


    onPopupClose:  function(evt) {
	if(this.displayDiv) {
	    $("#" + this.displayDiv).html("");
	}
        if (this.currentPopup) {
            this.getMap().removePopup(this.currentPopup);
            this.currentPopup.destroy();
            this.currentPopup = null;
            this.hiliteBox('');
            if (this.selectedFeature) {
                this.unselectFeature(this.selectedFeature);
            }
        }
    },

    findObject:  function(id, array) {
        for (i = 0; i < array.length; i++) {
            let aid = array[i].ramaddaId;
            if (!aid)
                array[i].id;
            if (aid == id) {
                return array[i];
            }
        }
        return null;
    },

    findMarker:  function(id) {
        if (!this.markers) {
            return null;
        }
        return this.findObject(id, this.getMarkers());
    },

    findFeature:  function(id) {
        return this.features[id];
    },

    findBox:  function(id) {
        if (!this.boxes) {
            return null;
        }
        return this.findObject(id, this.boxes.markers);
    },

    hiliteBox:  function(id) {
        if (this.currentBox) {
            this.currentBox.setBorder(this.currentBox.defaultBorderColor||"blue",1);
        }
        this.currentBox = this.findBox(id);
        if (this.currentBox) {
            this.currentBox.setBorder("red");
        }
    },
    checkMarkerVisibility:  function() {
        if (!this.markers) return;
        let list = this.getMarkers();
        for (let idx = 0; idx < list.length; idx++) {
            marker = list[idx];
            let visible = this.isLayerVisible(marker.ramaddaId, marker.parentId);
            //            console.log("   visible:" + visible +" " + marker.ramaddaId + " " + marker.parentId);
            if (visible) {
                marker.style.display = 'inline';
            } else {
                marker.style.display = 'none';
            }
            //            marker.display(visible);
        }
        this.markers.redraw();
    },

    checkLinesVisibility:  function() {
        if (!this.lines) return;
        let features = this.lines.features;
        for (let i = 0; i < features.length; i++) {
            let line = features[i];
            let visible = this.isLayerVisible(line.ramaddaId);
            if (visible) {
                line.style.display = 'inline';
            } else {
                line.style.display = 'none';
            }

        }
        this.lines.redraw();

    },


    checkBoxesVisibility:  function() {
        if (!this.boxes) return;
        for (let marker in this.boxes.markers) {
            marker = this.boxes.markers[marker];
            let visible = this.isLayerVisible(marker.ramaddaId);
            marker.display(visible);
        }
        this.boxes.redraw();
    },



    checkImageLayerVisibility:  function() {
        if (!this.imageLayers) return;
        for (let i in this.imageLayers) {
            let visible = this.isLayerVisible(i);
            let image = this.imageLayers[i];
            image.setVisibility(visible);
            if (!visible) {
                if (image.box) {
                    this.removeBox(image.box);
                    image.box = null;
                }
            } else {
                if (image.box) {
                    this.removeBox(image.box);
                    image.box = this.createBox(i, "", image.north, image.west, image.south, image.east, image.text, {});
                }
            }
        }
    },

    hiliteMarker:  function(id) {
        let mymarker = this.findMarker(id);
        if (!mymarker) {
            mymarker = this.findFeature(id);
        }

        if (!mymarker && this.imageLayers) {
            mymarker = this.imageLayers[id];
        }


        if (!mymarker) {
            return;
        }
        let latLonBounds = mymarker.latLonBounds;
        if (latLonBounds) {
            let projBounds = this.transformLLBounds(latLonBounds);
            this.zoomToExtent(projBounds);
        } else {
	    if(debugBounds) console.log("hiliteMarker");
            this.getMap().setCenter(mymarker.lonlat);
        }

        this.showMarkerPopup(mymarker);
    },


    hideLoadingImage:  function() {
        if (this.loadingImage) {
            this.loadingImage.style.visibility = "hidden";
        }
    },

    showLoadingImage:  function() {
        if (this.loadingImage) {
            this.loadingImage.style.visibility = "inline";
            return;
        }
        let sz = new OpenLayers.Size();
        sz.h = 120;
        sz.w = 120;
        let width = this.getMap().viewPortDiv.offsetWidth;
        let height = this.getMap().viewPortDiv.offsetHeight;
        let position = new OpenLayers.Pixel(width / 2 - sz.w / 2, height / 2 - sz.h / 2);
        this.loadingImage = OpenLayers.Util.createImage("loadingimage",
							position,
							sz,
							ramaddaBaseUrl + '/icons/mapprogress.gif');
        this.loadingImage.style.zIndex = 1010;
        this.getMap().viewPortDiv.appendChild(this.loadingImage);
    },


    centerOnMarkerLayer:  function() {
        this.centerOnMarkers(null, false, true);
    },

    centerOnMarkersInit:  function() {
	if(!this.hadDefaultBounds) {
            this.centerOnMarkers(null);
	}
    },

    getLayerVisbileExtent: function(layer) {
        let maxExtent = null;
        let features = layer.features;
	if(!features) return layer.getDataExtent();
        if(features.length > 0) {
            let geometry = null;
            for(let i=0, len=features.length; i<len; i++) {
		if(!features[i].getVisibility()) continue;
                geometry = features[i].geometry;
                if (geometry) {
                    if (maxExtent === null) {
                        maxExtent = new OpenLayers.Bounds();
                    }
                    maxExtent.extend(geometry.getBounds());
                }
            }
        }
        return maxExtent;
    },

    zoomToLayer:  function(layer,scale)  {
        let dataBounds = this.getLayerVisbileExtent(layer);
	if(!dataBounds)
	    dataBounds = layer.extent;
        if (dataBounds) {
	    if(scale)
		dataBounds = dataBounds.scale(parseFloat(scale),dataBounds.getCenterPixel());
            this.zoomToExtent(dataBounds, true);
	}
    },

    animateViewToBounds:  function(bounds, ob,steps) {
	if(!Utils.isDefined(steps)) steps = 1;
	if(!ob)
	    ob = this.transformProjBounds(this.getMap().getExtent());
	let numSteps = 10;
	if(steps>numSteps) {
	    this.setViewToBounds(bounds);
	    return;
	}
	let p = steps/numSteps; 
	steps++;
	newBounds = MapUtils.createBounds(
	    ob.left+(bounds.left- ob.left)*p,
	    ob.bottom+(bounds.bottom- ob.bottom)*p,
	    ob.right+(bounds.right- ob.right)*p,
	    ob.top+(bounds.top-ob.top)*p);
	this.setViewToBounds(newBounds);
	setTimeout(()=>this.animateViewToBounds(bounds, ob, steps),125);
    },


    getPopupText:  function(text, marker) {
        if (text == null) return null;
        if (text.indexOf("base64:") == 0) {
            text = window.atob(text.substring(7));
            if (text.indexOf("{") == 0) {
                props = JSON.parse(text);
                text = props.text;
                if (!text) text = "";
                if (marker) {
                    marker.inputProps = props;
                }
            }
        }
        return text;
    },

    clearSeenMarkers:  function() {
	this.seenMarkers = {};
    },

    addEntryMarker:  function(id, location, iconUrl, markerName, text, type, props) {
        marker = this.addMarker(id, location, iconUrl, markerName, text);
        marker.entryType = type;
        marker.entryId = id;
	if(props && props.fillColor) {
	    let pointStyle = {
		pointRadius:props.radius||12,
		fillColor:props.fillColor,
		strokeWidth:props.strokeWidth,
		strokeColor:props.strokeColor
	    };
	    this.addPoint(id, location, pointStyle,"");
	}
    },


    createMarker:  function(id, location, iconUrl, markerName, text, parentId, size, xoffset, yoffset, canSelect,attrs) {
	if(!attrs) attrs  = {};
        if (Array.isArray(location)) {
            location = MapUtils.createLonLat(location[0], location[1]);
        }
        if (size == null) size = 16;
        if (xoffset == null) xoffset = 0;
        if (yoffset == null) yoffset = 0;
        if (!this.markers) {
            this.markers = new OpenLayers.Layer.Vector("Markers");
            this.addVectorLayer(this.markers, canSelect);
        }
        if (!iconUrl) {
            iconUrl = ramaddaBaseUrl + '/icons/marker.png';
        }
        let sz = new OpenLayers.Size(size, size);
        let calculateOffset = function(size) {
            let offset = new OpenLayers.Pixel(-(size.w / 2)-xoffset, -(size.h / 2) - yoffset);
	    return offset;
        };

        let icon = new OpenLayers.Icon(iconUrl, sz, null, calculateOffset);
        let projPoint = this.transformLLPoint(location);
        let marker = new OpenLayers.Marker(projPoint, icon);
	let pt = new OpenLayers.Geometry.Point(location.lon, location.lat).transform(this.displayProjection, this.sourceProjection);
	let style = {
                externalGraphic: iconUrl,
                graphicHeight: size,
                graphicWidth: size,
                graphicXOffset: xoffset + (-size / 2),
                graphicYOffset: -size / 2,
		labelYOffset:-size,
		label:attrs.label
        };
	if(Utils.isDefined(attrs.rotation)) {
	    style.rotation = attrs.rotation;
	}
        let feature = new OpenLayers.Feature.Vector(
            pt,
	    {
                description: ''
            }, style);

        feature.ramaddaId = id;
        feature.parentId = parentId;
        feature.text = this.getPopupText(text, feature);
        feature.name = markerName;
        feature.location = location;

        marker.ramaddaId = id;
        marker.text = this.getPopupText(text, marker);
        marker.name = markerName;
        marker.location = location;
        let locationKey = location + "";
        feature.locationKey = locationKey;
        let seenMarkers = this.seenMarkers[locationKey];
        if (seenMarkers == null) {
            seenMarkers = [];
            this.seenMarkers[locationKey] = seenMarkers;
        } else {}
        seenMarkers.push(feature);
        //        console.log("loc:" + locationKey +" " + seenMarkers);


        let _this = this;
        let clickFunc = function(evt) {
            _this.showMarkerPopup(marker, true);
            if (marker.ramaddaClickHandler != null) {
                marker.ramaddaClickHandler.call(null, marker);
            }
            OpenLayers.Event.stop(evt);
        };

        marker.events.register('click', marker, clickFunc);
        marker.events.register('touchstart', marker, clickFunc);


        let visible = this.isLayerVisible(marker.ramaddaId, marker.parentId);
        if (!visible) marker.display(false);
        feature.what = "marker";
        return feature;
    },

    addMarker:  function(id, location, iconUrl, markerName, text, parentId, size, yoffset, canSelect, attrs) {
	if(Utils.isDefined(location.x)) {
	    location = MapUtils.createLonLat(location.x,location.y);
	}
        let marker = this.createMarker(id, location, iconUrl, markerName, text, parentId, size, 0, yoffset, canSelect,attrs);
        this.addMarkers([marker]);
        return marker;
    },

    addMarkers:  function(markers) {
        if (!this.markers) {
            this.markers = new OpenLayers.Layer.Vector("Markers");
            this.addVectorLayer(this.markers, true);
        }
        this.markers.addFeatures(markers);
    },



    initBoxes:  function(theBoxes) {
        if (!this.getMap()) {
            // alert('whoa, no map');
        }
        this.addLayer(theBoxes);
        // Added this because I was getting an unknown method error
        theBoxes.getFeatureFromEvent = function(evt) {
            return null;
        };
	//Don't do this as the box select hides the marker select
	//        let sf = new OpenLayers.Control.SelectFeature(theBoxes);
	//        this.getMap().addControl(sf);
	//        sf.activate();
    },

    removeBox:  function(box) {
        if (this.boxes && box) {
            this.boxes.removeMarker(box);
            this.boxes.redraw();
        }
    },

    addBox:  function(box) {
        if (!this.boxes) {
            this.boxes = new OpenLayers.Layer.Boxes("Boxes", {
                wrapDateLine: MapUtils.defaults.wrapDateline,
            });
            if (!this.getMap()) {
                this.initialBoxes = this.boxes;
            } else {
                this.initBoxes(this.boxes);
            }
        }
        this.boxes.addMarker(box);
        this.boxes.redraw();
    },


    createBox:  function(id, name, north, west, south, east, text, params) {
        if (text.indexOf("base64:") == 0) {
            text = window.atob(text.substring(7));
        }

        let args = {
            "color": "blue",
            "selectable": true,
            "zoomToExtent": false,
            "sticky": false
        };
        for (let i in params) {
            args[i] = params[i];
        }
	if(!args.color) args.color = this.params.boxColor || "blue";
        let bounds = MapUtils.createBounds(west, Math.max(south, -MapUtils.defaults.maxLatValue),
				  east, Math.min(north, MapUtils.defaults.maxLatValue));
        let projBounds = this.transformLLBounds(bounds);
        box = new OpenLayers.Marker.Box(projBounds);
        box.sticky = args.sticky;
        let _this = this;

        if (args["selectable"]) {
            box.events.register("click", box, function(e) {
                _this.showMarkerPopup(box, true);
                OpenLayers.Event.stop(e);
            });
        }

        let lonlat = new MapUtils.createLonLat(west, north);
        box.lonlat = this.transformLLPoint(lonlat);
        box.text = this.getPopupText(text);
        box.name = name;
        box.setBorder(args["color"], 1);
	box.defaultBorderColor = args["color"];
        box.ramaddaId = id;
        if (args["zoomToExtent"]) {
            this.centerOnMarkers(bounds);
        }
        this.addBox(box);
        return box;
    },


    circleMarker:  function(id, attrs) {
        marker = this.findMarker(id);
        if (!marker) {
            return null;
        }
        myattrs = {
            pointRadius: 12,
            stroke: true,
            strokeColor: "red",
            strokeWidth: 2,
            fill: false,
        };

        if (attrs)
            $.extend(myattrs, attrs);
        let _this = this;
        this.showFeatureText(marker);
        return this.addPoint(id + "_circle", marker.location, myattrs);
    },


    uncircleMarker:  function(id) {
        feature = this.features[id + "_circle"];
        if (feature && this.circles) {
            this.circles.removeFeatures([feature]);
        }
        this.hideFeatureText(feature);
    },

    addFeatureHighlightHandler:  function( callback) {
	this.featureHighlightHandler = callback;
    },

    addFeatureSelectHandler:  function( callback) {
	this.featureSelectHandler= callback;
    },

    showFeatureText:  function(feature) {
	if(this.featureHighlightHandler) {
	    this.featureHighlightHandler(feature,true);
	}
        let _this = this;
        if (feature.text && this.displayDiv) {
            this.textFeature = feature;
            let callback = function() {
                if (_this.textFeature == feature) {
                    _this.showText(_this.textFeature.text);
                }
            }
            setTimeout(callback, 500);
        }
    },

    showText:  function(text) {
        $("#" + this.displayDiv).html(text);
    },

    hideFeatureText:  function(feature) {
	if(this.featureHighlightHandler)
	    this.featureHighlightHandler(feature,false);
        if (!feature || this.textFeature == feature) {
	    if(!this.displayDivSticky) 
                this.showText("");
        }
    },
    
    
    addPoint:  function(id, point, attrs, text, notReally, textGetter) {
        //Check if we have a LonLat instead of a Point
        let location = point;
        if (typeof point.x === 'undefined') {
            point = new OpenLayers.Geometry.Point(point.lon, point.lat);
        } else {
            location = MapUtils.createLonLat(point.x, point.y);
        }

        let _this = this;
	if(!this.basePointStyle) {
	    this.basePointStyle = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style['default']);
            $.extend(this.basePointStyle, {
		pointRadius: 5,
		stroke: true,
		strokeColor: "red",
		strokeWidth: 0,
		strokeOpacity: 0.75,
		fill: true,
		fillColor: "blue",
		fillOpacity: 0.75,
            });
	}

        let cstyle = $.extend({},this.basePointStyle);

        if (attrs) {
            $.extend(cstyle, attrs);
            if (cstyle.pointRadius <= 0) cstyle.pointRadius = 1;
        }
        if (cstyle.fillColor == "" || cstyle.fillColor == "none") {
            cstyle.fillOpacity = 0.0;
        }
        if (cstyle.strokeColor == "" || cstyle.strokeColor == "none") {
            cstyle.strokeOpacity = 0.0;
        }

	//["star", "cross", "x", "square", "triangle", "circle", "lightning", "rectangle", "church"];
        let center = new OpenLayers.Geometry.Point(point.x, point.y);
        center.transform(this.displayProjection, this.sourceProjection);
        let feature = new OpenLayers.Feature.Vector(center, null, cstyle);
        feature.center = center;
        feature.ramaddaId = id;
	if(text)
            feature.text = this.getPopupText(text, feature);
        feature.location = location;
        this.features[id] = feature;
        if (!notReally) {
	    this.getMarkersLayer().addFeatures([feature],{silent:true});
	}
        return feature;
    },

    getMarkersLayer:  function() {
        if (this.circles == null) {
            this.circles = new OpenLayers.Layer.Vector("Shapes");
	    this.circles.layerName = "circles";
            this.circles.setZIndex(1);
            this.addVectorLayer(this.circles);
	    this.circles.canSelect = true;
        }
	return this.circles;
    },

    setPointsVisibility: function(visible) {
	if(this.circles) 
	    this.circles.setVisibility(visible);
    },

    removePoint:  function(point) {
        if (this.circles)
            this.circles.removeFeatures([point]);
    },

    addRectangle:  function(id, north, west, south, east, attrs, info) {
        let points = [new OpenLayers.Geometry.Point(west, north),
		      new OpenLayers.Geometry.Point(west, south),
		      new OpenLayers.Geometry.Point(east, south),
		      new OpenLayers.Geometry.Point(east, north),
		      new OpenLayers.Geometry.Point(west, north)
		     ];
        return this.addPolygon(id, "", points, attrs, info);
    },


    addLine:  function(id, name, lat1, lon1, lat2, lon2, attrs, info) {
        let points = [new OpenLayers.Geometry.Point(lon1, lat1),
		      new OpenLayers.Geometry.Point(lon2, lat2)
		     ];
        return this.addPolygon(id, name, points, attrs, info);
    },

    addLines:  function(id, name, attrs, values, info) {
	attrs = attrs || {};
	if (!attrs.strokeWidth)
	    attrs.strokeWidth = this.strokeWidth;

	if (!attrs.strokeColor)
	    attrs.strokeColor = this.strokeColor;
	    

        let points = [];
        for (let i = 0; i < values.length; i += 2) {
//	    console.log("pt:" + values[i+1] + " " + values[i]);
            points.push(new OpenLayers.Geometry.Point(values[i + 1], values[i]));
        }
        return this.addPolygon(id, name, points, attrs, info);
    },

    removePolygon:  function(line) {
        if (this.lines) {
            //            this.lines.removeAllFeatures();
            this.lines.removeFeatures([line]);
        }
    },

    addPolygon:  function(id, name, points, attrs, marker) {
        let _this = this;
        let location;
        if (points.length > 1) {
            location = new OpenLayers.LonLat(points[0].x + (points[1].x - points[0].x) / 2,
					     points[0].y + (points[1].y - points[0].y) / 2);
        } else {
            location = new OpenLayers.LonLat(points[0].x, points[0].y);
        }
        for (let i = 0; i < points.length; i++) {
            points[i].transform(this.displayProjection, this.sourceProjection);
        }


        let base_style = OpenLayers.Util.extend({},
						OpenLayers.Feature.Vector.style['default']);
        let style = OpenLayers.Util.extend({}, base_style);
        style.strokeColor = "blue";
        style.strokeWidth = 1;
        if (attrs) {
            for (let key in attrs) {
                style[key] = attrs[key];
            }
        }

        if (!this.lines) {
            this.lines = new OpenLayers.Layer.Vector("Lines", {
                style: base_style
            });
            this.addVectorLayer(this.lines);
        }

        let lineString = new OpenLayers.Geometry.LineString(points);
        let line = new OpenLayers.Feature.Vector(lineString, null, style);
        /*
         * line.events.register("click", line, function (e) { alert("box
         * click"); _this.showMarkerPopup(box); OpenLayers.Event.stop(evt); });
         */
	if(!marker) marker = name;

        line.text = marker;
        line.ramaddaId = id;
        line.location = location;
        line.name = name;
        let visible = this.isLayerVisible(line.ramaddaId);
        if (visible) {
            line.style.display = 'inline';
        } else {
            line.style.display = 'none';
        }

        this.lines.addFeatures([line]);
        return line;
    },

    getLinesLayer: function() {
        if (!this.lines) {
            let base_style = OpenLayers.Util.extend({},
						    OpenLayers.Feature.Vector.style['default']);
            this.lines = new OpenLayers.Layer.Vector("Lines", {
                style: base_style
            });
            this.addVectorLayer(this.lines);
        }	
	return this.lines;
    },
    centerOnFeatures: function(features) {
	let bounds = new OpenLayers.Bounds();
	features.forEach(feature=>{
	    bounds.extend(feature.geometry.getBounds());
	});
	//var center = bounds.getCenterLonLat(); <-- you don't really need this if you want to zoom. But it will give you the center lat long coords.
	this.zoomToExtent(bounds);
    },
    getHighlightLinesLayer: function() {
        if (!this.highlightlines) {
            let base_style = OpenLayers.Util.extend({},
						    OpenLayers.Feature.Vector.style['default']);
            this.highlightlines = new OpenLayers.Layer.Vector("Highlight Lines", {
                style: base_style
            });
            this.addVectorLayer(this.highlightlines);
        }	
	return this.highlightlines;
    },

    handleMarkerLayer:  function() {
        if (this.currentPopup) {
            this.getMap().removePopup(this.currentPopup);
            this.currentPopup.destroy();
            this.currentPopup = null;
        }
        let marker = this.currentEntryMarker;
        if (marker.entryLayer) {
            let idx = this.loadedLayers.indexOf(marker.entryLayer);
            if (idx >= 0)
                this.loadedLayers = this.loadedLayers.slice(idx);
            this.getMap().removeLayer(marker.entryLayer);
            marker.entryLayer = null;
            return;
        }
        let layer = this.handleEntrySelect(marker.entryId, marker.name, marker.entryType);
        if (layer) {
            marker.entryLayer = layer;
        }
    },

    showMarkerPopup:  function(marker, fromClick, simplePopup) {

        if (this.entryClickHandler && window[this.entryClickHandler]) {
            if (!window[this.entryClickHandler](this, marker)) {
                return;
            }
        }

        if (this.currentPopup) {
            this.getMap().removePopup(this.currentPopup);
            this.currentPopup.destroy();
        }

	if(this.featureSelectHandler) {
	    if(this.featureSelectHandler(marker)) {
		return;
	    }
	}


        let id = marker.ramaddaId;
        if (!id)
            id = marker.id;
	if(this.shareSelected &&  window["ramaddaDisplaySetSelectedEntry"]) {
	    ramaddaDisplaySetSelectedEntry(id);
	}

	if(!this.doPopup) {
	    return;
	}

        this.hiliteBox(id);
        let _this = this;
        if (marker.inputProps) {
            marker.text = this.getPopupText(marker.inputProps.text);
        }
        let markerText;



	if(marker.textGetter) {
	    markerText =marker.textGetter(marker);
	} else if(this.textGetter) {
	    markerText = this.textGetter(marker.layer, marker);
	}
	if(!markerText) {
	    markerText =marker.text;
	}
	if(!markerText) return;
	if(this.displayDiv) {
	    $("#" + this.displayDiv).html(markerText);
	    return;
	}

        if (fromClick && marker.locationKey != null) {
            markers = this.seenMarkers[marker.locationKey];
            if (markers && markers.length > 1) {
                markerText = "";
                for (let i = 0; i < markers.length; i++) {
                    otherMarker = markers[i];
                    if (i > 0)
                        markerText += '<hr>';
                    if (otherMarker.inputProps) {
			otherMarker.text = otherMarker.textGetter?otherMarker.textGetter(marker):this.getPopupText(otherMarker.inputProps.text);
                    }
		    let text = otherMarker.text?otherMarker.text:otherMarker.textGetter?otherMarker.textGetter(marker):"NA";
                    markerText += text;
                    if (i > 10) break;
                }
            }
        }

	
        // set marker text as the location
        let location = marker.location;

        let projPoint = null;
	if(!location) {
	    if(marker.geometry) {
		let b = marker.geometry.getBounds();
		if(b) {
		    projPoint = b.getCenterLonLat();
		}
	    }
	} else {
            if (typeof location.lon === 'undefined') {
		location = MapUtils.createLonLat(location.x, location.y);
            }
            if (!markerText || markerText == "") {
		//            marker.location = this.transformProjPoint(marker.lonlat);
		if (location.lat == location.lat) {
                    markerText = "Lon: " + location.lat + "<br>" + "Lat: " + location.lon;
		}
            }
	    
            if (marker.entryType) {
		let type = marker.entryType;
		if (type == "geo_kml" || type == "geo_json" || type == "geo_shapefile") {
                    this.currentEntryMarker = marker;
                    let call = "ramaddaMapMap['" + this.mapId + "'].handleMarkerLayer();";
                    let label = marker.entryLayer ? "Remove Layer" : "Load Layer";
                    markerText = "<center>" + HtmlUtils.onClick(call, label) + "</center>" + markerText;
		}
            }
	    projPoint = this.transformLLPoint(location);
	}	

	if(projPoint==null) {
	    console.log("No location for feature popup");
	    return;
	}


	if(simplePopup || this.simplePopup) {
	    /* makePopup always makes a simpler popup just keep this here as fallback
	      popup = new OpenLayers.Popup("popup",
	      projPoint, null, HtmlUtils.div(["style",""],markerText), true);*/
            popup = this.makePopup( projPoint,markerText);
	} else {
            popup = this.makePopup( projPoint,markerText);
	} 

        if (marker.inputProps && marker.inputProps.minSizeX) {
            popup.minSize = new OpenLayers.Size(marker.inputProps.minSizeX, marker.inputProps.minSizeY);
        }

        marker.popupText = popup;
        popup.marker = marker;
        this.getMap().addPopup(popup);
        this.currentPopup = popup;

        if (marker.inputProps && marker.inputProps.chartType) {
            this.popupChart(marker.inputProps);
        }

	if(this.popupHandler) {
	    this.popupHandler(marker,popup);
	}
	


    },

    popupChart:  function(props) {
        let displayManager = getOrCreateDisplayManager(props.divId, {}, true);
        let pointDataProps = {
            entryId: props.entryId
        };
        let title = props.title;
        if (!title) title = "Chart";
        let fields = (props.fields == null ? null : props.fields.split(","));
        let chartProps = {
            "title": title,
            "layoutHere": true,
            "divid": props.divId,
            "entryId": props.entryId,
            "fields": fields,
            "vAxisMinValue": props.vAxisMinValue,
            "vAxisMaxValue": props.vAxisMaxValue,
            "data": new PointData(title, null, null, getRamadda().getRoot() + "/entry/show?entryid=" + props.entryId + "&output=points.product&product=points.json&numpoints=10000", pointDataProps)
        };
	if(props.chartArgs) {
	    let toks = props.chartArgs.split(",");
	    for(let i=0;i<toks.length;i+=2) {
		chartProps[toks[i]] = toks[i+1];
	    }
	}
        displayManager.createDisplay(props.chartType, chartProps);
    },

    removeMarker:  function(marker) {
        if (this.markers) {
            //            this.markers.removeMarker(marker);            
            this.markers.removeFeatures([marker]);
        }
    },
}

