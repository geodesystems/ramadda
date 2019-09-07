/**
   Copyright 2008-2019 Geode Systems LLC
*/

var DISPLAY_MAP = "map";

var displayMapMarkers = ["marker.png", "marker-blue.png", "marker-gold.png", "marker-green.png"];
var displayMapCurrentMarker = -1;
var displayMapUrlToVectorListeners = {};
var displayMapMarkerIcons = {};

addGlobalDisplayType({
    type: DISPLAY_MAP,
    label: "Map"
});

function MapFeature(source, points) {
    RamaddaUtil.defineMembers(this, {
        source: source,
        points: points
    });
}




function RamaddaMapDisplay(displayManager, id, properties) {
    var ID_LATFIELD = "latfield";
    var ID_LONFIELD = "lonfield";
    var ID_MAP = "map";
    var ID_BOTTOM = "bottom";
    var ID_COLORTABLE = "colortable";
    var ID_SHAPES = "shapes";
    var SUPER;
    RamaddaUtil.defineMembers(this, {
        showLocationReadout: false,
        showBoxes: true,
        showPercent: false,
        percentFields: null,
        kmlLayer: null,
        kmlLayerName: "",
        geojsonLayer: null,
        geojsonLayerName: "",
        theMap: null
    });

    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id,
							 DISPLAY_MAP, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        mapBoundsSet: false,
        features: [],
        myMarkers: {},
        mapEntryInfos: {},
        sourceToLine: {},
        sourceToPoints: {},
        snarf: true,
        initDisplay: function() {
            SUPER.initDisplay.call(this);
            var _this = this;
            var html = "";
            var extraStyle = "min-height:200px;";
            var width = this.getWidth();
            if (Utils.isDefined(width)) {
                if (width > 0) {
                    extraStyle += "width:" + width + "px; ";
                } else if (width < 0) {
                    extraStyle += "width:" + (-width) + "%;";
                } else if (width != "") {
                    extraStyle += "width:" + width + ";";
                }
            }

            var height = this.getProperty("height", 300);
            // var height = this.getProperty("height",-1);
            if (height > 0) {
                extraStyle += " height:" + height + "px; ";
            } else if (height < 0) {
                extraStyle += " height:" + (-height) + "%; ";
            } else if (height != "") {
                extraStyle += " height:" + (height) + ";";
            }

            if (this.getProperty("doAnimation", false)) {
		//		this.getAnimation().makeControls();
            }
            html += HtmlUtils.div([ATTR_CLASS, "display-map-map", "style",
				   extraStyle, ATTR_ID, this.getDomId(ID_MAP)
				  ]);
            html += HtmlUtils.div([ATTR_CLASS, "", ATTR_ID, this.getDomId(ID_BOTTOM)]);

            if (this.showLocationReadout) {
                html += HtmlUtils.openTag(TAG_DIV, [ATTR_CLASS,
						    "display-map-latlon"
						   ]);
                html += HtmlUtils.openTag("form");
                html += "Latitude: " +
                    HtmlUtils.input(this.getDomId(ID_LATFIELD), "", ["size",
								     "7", ATTR_ID, this.getDomId(ID_LATFIELD)
								    ]);
                html += "  ";
                html += "Longitude: " +
                    HtmlUtils.input(this.getDomId(ID_LONFIELD), "", ["size",
								     "7", ATTR_ID, this.getDomId(ID_LONFIELD)
								    ]);
                html += HtmlUtils.closeTag("form");
                html += HtmlUtils.closeTag(TAG_DIV);
            }
            this.setContents(html);

            if (!this.map) {
                this.createMap();
            } else {
                this.map.setMapDiv(this.getDomId(ID_MAP));
            }

            if (!this.haveCalledUpdateUI) {
                var callback = function() {
                    _this.updateUI();
                }
                setTimeout(callback, 1);
            }
        },
        checkLayout: function() {
            if (!this.map) {
                return;
            }
            var d = this.jq(ID_MAP);
            if (d.width() > 0 && this.lastWidth != d.width() && this.map) {
                this.lastWidth = d.width();
                this.map.getMap().updateSize();
            }
        },

        createMap: function() {
            var theDisplay = this;
            var params = {
                "defaultMapLayer": this.getProperty("defaultMapLayer",
						    map_default_layer),

            };
            var displayDiv = this.getProperty("displayDiv", null);
            if (displayDiv) {
                params.displayDiv = displayDiv;
            }
            if (!this.getProperty("showLocationSearch", true)) {
                params.showLocationSearch = false;
            }
            var mapLayers = this.getProperty("mapLayers", null);
            if (mapLayers) {
                params.mapLayers = [mapLayers];
            }

	    params.linked = this.getProperty("linked", false);
	    params.linkGroup = this.getProperty("linkGroup", null);
            this.map = this.getProperty("theMap", null);
            if (this.map) {
                this.map.setMapDiv(this.getDomId(ID_MAP));
            } else {
                this.map = new RepositoryMap(this.getDomId(ID_MAP), params);
                this.lastWidth = this.jq(ID_MAP).width();
            }
            if (this.doDisplayMap()) {
                this.map.setDefaultCanSelect(false);
            }
            this.map.initMap(false);

            this.map.addRegionSelectorControl(function(bounds) {
                theDisplay.getDisplayManager().handleEventMapBoundsChanged(this, bounds, true);
            });
            this.map.addClickHandler(this.getDomId(ID_LONFIELD), this
				     .getDomId(ID_LATFIELD), null, this);
            this.map.getMap().events.register("zoomend", "", function() {
                theDisplay.mapBoundsChanged();
            });
            this.map.getMap().events.register("moveend", "", function() {
                theDisplay.mapBoundsChanged();
            });

            var overrideBounds = false;
            if (this.getProperty("bounds")) {
                overrideBounds = true;
                var toks = this.getProperty("bounds", "").split(",");
                if (toks.length == 4) {
                    if (this.getProperty("showBounds", false)) {
                        var attrs = {};
                        if (this.getProperty("boundsColor")) {
                            attrs.strokeColor = this.getProperty("boundsColor", "");
                        }
                        this.map.addRectangle("bounds", parseFloat(toks[0]), parseFloat(toks[1]), parseFloat(toks[2]), parseFloat(toks[3]), attrs, "");
                    }
                    this.setInitMapBounds(parseFloat(toks[0]), parseFloat(toks[1]), parseFloat(toks[2]), parseFloat(toks[3]));
                }
            }
	    
	    var boundsAnimation = this.getProperty("boundsAnimation");
	    let _this = this;
	    if(boundsAnimation) {
		this.didAnimationBounds = false;
                let animationBounds = boundsAnimation.split(",");
                if (animationBounds.length == 4) {
		    var pause = parseFloat(this.getProperty("animationPause","1000"));
		    HtmlUtils.callWhenScrolled(this.getDomId(ID_MAP),()=>{
			if(_this.didAnimationBounds) {
			    return;
			}
			_this.didAnimationBounds = true;
			var a = animationBounds;
			var b = createBounds(parseFloat(a[1]),parseFloat(a[2]),parseFloat(a[3]),parseFloat(a[0]));
			_this.map.animateViewToBounds(b);
		    },pause);
		}
            }

            var currentFeatures = this.features;
            this.features = [];
            for (var i = 0; i < currentFeatures.length; i++) {
                this.addFeature(currentFeatures[i]);
            }
            var entries = this.getDisplayManager().collectEntries();
            for (var i = 0; i < entries.length; i++) {
                var pair = entries[i];
                this.handleEventEntriesChanged(pair.source, pair.entries);
            }

            if (this.layerEntries) {
                var selectCallback = function(layer) {
                    _this.handleLayerSelect(layer);
                }
                var unselectCallback = function(layer) {
                    _this.handleLayerUnselect(layer);
                }
                var toks = this.layerEntries.split(",");
                for (var i = 0; i < toks.length; i++) {
                    var tok = toks[i];
                    var url = ramaddaBaseUrl + "/entry/show?output=shapefile.kml&entryid=" + tok;
                    this.map.addKMLLayer("layer", url, true, selectCallback, unselectCallback);
                    //TODO: Center on the kml
                }
            }
            if (this.showDataLayers()) {
                if (theDisplay.kmlLayer != null) {
                    var url = ramaddaBaseUrl + "/entry/show?output=shapefile.kml&entryid=" + theDisplay.kmlLayer;
                    theDisplay.addBaseMapLayer(url, true);
                }
                if (theDisplay.geojsonLayer != null) {
                    url = theDisplay.getRamadda().getEntryDownloadUrl(theDisplay.geojsonLayer);
                    theDisplay.addBaseMapLayer(url, false);
                }
            }
            if (this.getProperty("latitude")) {
                this.map.setCenter(createLonLat(parseFloat(this.getProperty("longitude", -105)),
						parseFloat(this.getProperty("latitude", 40))));
            }


        },
        addBaseMapLayer: function(url, isKml) {
            var theDisplay = this;
            mapLoadInfo = displayMapUrlToVectorListeners[url];
            if (mapLoadInfo == null) {
                mapLoadInfo = {
                    otherMaps: [],
                    layer: null
                };
                selectFunc = function(layer) {
                    theDisplay.mapFeatureSelected(layer);
                }
                var hasBounds = this.getProperty("bounds") != null;
                if (isKml)
                    this.map.addKMLLayer(this.kmlLayerName, url, this.doDisplayMap(), selectFunc, null, null,
					 function(map, layer) {
					     theDisplay.baseMapLoaded(layer, url);
					 }, !hasBounds);
                else
                    this.map.addGeoJsonLayer(this.geojsonLayerName, url, this.doDisplayMap(), selectFunc, null, null,
					     function(map, layer) {
						 theDisplay.baseMapLoaded(layer, url);
					     }, !hasBounds);
            } else if (mapLoadInfo.layer) {
                this.cloneLayer(mapLoadInfo.layer);
            } else {
                this.map.showLoadingImage();
                mapLoadInfo.otherMaps.push(this);
            }
        },
        mapFeatureSelected: function(layer) {
            if (!this.getPointData()) {
                return;
            }
            this.map.onFeatureSelect(layer);
            if (!Utils.isDefined(layer.feature.dataIndex)) {
                return;
            }
            this.getDisplayManager().propagateEventRecordSelection(this, this.getPointData(), {
                index: layer.feature.dataIndex
            });
        },
        showDataLayers: function() {
            return this.getProperty("showLayers", true);
        },
        doDisplayMap: function() {
            if (!this.showDataLayers()) return false;
            if (!this.getProperty("displayAsMap", true)) return false;
            return this.kmlLayer != null || this.geojsonLayer != null;
        },
        cloneLayer: function(layer) {
            var theDisplay = this;
            this.map.hideLoadingImage();
            layer = layer.clone();
            var features = layer.features;
            var clonedFeatures = [];
            for (var j = 0; j < features.length; j++) {
                feature = features[j];
                feature = feature.clone();
                if (feature.style) {
                    oldStyle = feature.style;
                    feature.style = {};
                    for (var a in oldStyle) {
                        feature.style[a] = oldStyle[a];
                    }
                }
                feature.layer = layer;
                clonedFeatures.push(feature);
            }
            layer.removeAllFeatures();
            this.map.getMap().addLayer(layer);
            layer.addFeatures(clonedFeatures);
            this.vectorLayer = layer;
            this.applyVectorMap();
            this.map.addSelectCallback(layer, this.doDisplayMap(), function(layer) {
                theDisplay.mapFeatureSelected(layer);
            });
        },
        handleEventPointDataLoaded: function(source, pointData) {
        },
        baseMapLoaded: function(layer, url) {
            this.vectorLayer = layer;
            this.applyVectorMap();
            mapLoadInfo = displayMapUrlToVectorListeners[url];
            if (mapLoadInfo) {
                mapLoadInfo.layer = layer;
                for (var i = 0; i < mapLoadInfo.otherMaps.length; i++) {
                    mapLoadInfo.otherMaps[i].cloneLayer(layer);
                }
                mapLoadInfo.otherMaps = [];
            }
        },
        handleLayerSelect: function(layer) {
            var args = this.layerSelectArgs;
            if (!this.layerSelectPath) {
                if (!args) {
                    this.map.onFeatureSelect(layer);
                    return;
                }
                //If args was defined then default to search
                this.layerSelectPath = "/search/do";
            }
            var url = ramaddaBaseUrl + this.layerSelectPath;
            if (args) {
                var toks = args.split(",");
                for (var i = 0; i < toks.length; i++) {
                    var tok = toks[i];
                    var toktoks = tok.split(":");
                    var urlArg = toktoks[0];
                    var layerField = toktoks[1];
                    var attrs = layer.feature.attributes;
                    var fieldValue = null;
                    for (var attr in attrs) {
                        var attrName = "" + attr;
                        if (attrName == layerField) {
                            var attrValue = null;
                            if (typeof attrs[attr] == 'object' || typeof attrs[attr] == 'Object') {
                                var o = attrs[attr];
                                attrValue = o["value"];
                            } else {
                                attrValue = attrs[attr];
                            }
                            url = HtmlUtils.appendArg(url, urlArg, attrValue);
                            url = url.replace("${" + urlArg + "}", attrValue);
                        }
                    }
                }
            }
            url = HtmlUtils.appendArg(url, "output", "json");
            var entryList = new EntryList(this.getRamadda(), url, null, false);
            entryList.doSearch(this);
            this.getEntryList().showMessage("Searching", HtmlUtils.div([ATTR_STYLE, "margin:20px;"], this.getWaitImage()));
        },
        getEntryList: function() {
            if (!this.entryListDisplay) {
                var props = {
                    showMenu: true,
                    showTitle: true,
                    showDetails: true,
                    layoutHere: false,
                    showForm: false,
                    doSearch: false,
                };
                var id = this.getUniqueId("display");
                this.entryListDisplay = new RamaddaEntrylistDisplay(this.getDisplayManager(), id, props);
                this.getDisplayManager().addDisplay(this.entryListDisplay);
            }
            return this.entryListDisplay;
        },
        entryListChanged: function(entryList) {
            var entries = entryList.getEntries();
            this.getEntryList().entryListChanged(entryList);
        },
        handleLayerUnselect: function(layer) {
            this.map.onFeatureUnselect(layer);
        },
        addMapLayer: function(source, props) {
            var _this = this;
            var entry = props.entry;
            if (!this.addedLayers) this.addedLayers = {};
            if (this.addedLayers[entry.getId()]) {
                var layer = this.addedLayers[entry.getId()];
                if (layer) {
                    this.map.removeKMLLayer(layer);
                    this.addedLayers[entry.getId()] = null;
                }
                return;
            }

            var type = entry.getType().getId();
            if (type == "geo_shapefile" || type == "geo_geojson") {
                var bounds = createBounds(entry.getWest(), entry.getSouth(), entry.getEast(), entry.getNorth());
                if (bounds.left < -180 || bounds.right > 180 || bounds.bottom < -90 || bounds.top > 90) {
                    bounds = null;
                }

                var selectCallback = function(layer) {
                    _this.handleLayerSelect(layer);
                }
                var unselectCallback = function(layer) {
                    _this.handleLayerUnselect(layer);
                }
                var layer;
                if (type == "geo_geojson") {
                    var url = entry.getRamadda().getEntryDownloadUrl(entry);
                    layer = this.map.addGeoJsonLayer(this.geojsonLayerName, url, this.doDisplayMap(), selectCallback, unselectCallback, null, null, true);
                } else {
                    var url = ramaddaBaseUrl + "/entry/show?output=shapefile.kml&entryid=" + entry.getId();
                    layer = this.map.addKMLLayer(entry.getName(), url, true, selectCallback, unselectCallback, null, null, true);
                }
                this.addedLayers[entry.getId()] = layer;
                return;
            }

            var baseUrl = entry.getAttributeValue("base_url");
            if (!Utils.stringDefined(baseUrl)) {
                console.log("No base url:" + entry.getId());
                return;
            }
            var layer = entry.getAttributeValue("layer_name");
            if (layer == null) {
                layer = entry.getName();
            }
            this.map.addWMSLayer(entry.getName(), baseUrl, layer, false);
        },
        mapBoundsChanged: function() {
            var bounds = this.map.getMap().calculateBounds();
            bounds = bounds.transform(this.map.sourceProjection,
				      this.map.displayProjection);
            this.getDisplayManager().handleEventMapBoundsChanged(this, bounds);
        },
        addFeature: function(feature) {
            this.features.push(feature);
            feature.line = this.map.addPolygon("lines_" +
					       feature.source.getId(), RecordUtil
					       .clonePoints(feature.points), null);
        },
        xloadInitialData: function() {
            if (this.getDisplayManager().getData().length > 0) {
                this.handleEventPointDataLoaded(this, this.getDisplayManager()
						.getData()[0]);
            }
        },

        getContentsDiv: function() {
            return HtmlUtils.div([ATTR_CLASS, "display-contents", ATTR_ID,
				  this.getDomId(ID_DISPLAY_CONTENTS)
				 ], "");
        },
        handleEventEntryMouseover: function(source, args) {
            if (!this.map) {
                return;
            }
            id = args.entry.getId() + "_mouseover";
            attrs = {
                lineColor: "red",
                fillColor: "red",
                fillOpacity: 0.5,
                lineOpacity: 0.5,
                doCircle: true,
                lineWidth: 1,
                fill: true,
                circle: {
                    lineColor: "black"
                },
                polygon: {
                    lineWidth: 4,
                }
            }
            this.addOrRemoveEntryMarker(id, args.entry, true, attrs);
        },
        handleEventEntryMouseout: function(source, args) {
            if (!this.map) {
                return;
            }
            id = args.entry.getId() + "_mouseover";
            this.addOrRemoveEntryMarker(id, args.entry, false);
        },
        handleEventAreaClear: function() {
            if (!this.map) {
                return;
            }
            this.map.clearRegionSelector();
        },
        handleClick: function(theMap, lon, lat) {
            if (!this.map) {
                return;
            }
            if (this.doDisplayMap()) {
                return;
            }
            var justOneMarker = this.getProperty("justOneMarker",false);
            if(justOneMarker) {
                var pointData = this.getPointData();
                if(pointData) {
                    pointData.handleEventMapClick(this, source, lon, lat);
                }
            }
            this.getDisplayManager().handleEventMapClick(this, lon, lat);
        },

        getPosition: function() {
            var lat = $("#" + this.getDomId(ID_LATFIELD)).val();
            var lon = $("#" + this.getDomId(ID_LONFIELD)).val();
            if (lat == null)
                return null;
            return [lat, lon];
        },

        haveInitBounds: false,
        setInitMapBounds: function(north, west, south, east) {
            if (!this.map) return;
            if (this.haveInitBounds) return;
            this.haveInitBounds = true;
            this.map.centerOnMarkers(new OpenLayers.Bounds(west, south, east,
							   north),true);
        },

        sourceToEntries: {},
        handleEventEntriesChanged: function(source, entries) {
            if (!this.map) {
                return;
            }
            //debug
            if (source == this.lastSource) {
                this.map.clearSelectionMarker();
            }
            if ((typeof source.forMap) != "undefined" && !source.forMap) {
                return;
            }
            var oldEntries = this.sourceToEntries[source.getId()];
            if (oldEntries != null) {
                for (var i = 0; i < oldEntries.length; i++) {
                    var id = source.getId() + "_" + oldEntries[i].getId();
                    this.addOrRemoveEntryMarker(id, oldEntries[i], false);
                }
            }

            this.sourceToEntries[source.getId()] = entries;

            var markers = new OpenLayers.Layer.Markers("Markers");
            var lines = new OpenLayers.Layer.Vector("Lines", {});
            var north = -90,
                west = 180,
                south = 90,
                east = -180;
            var didOne = false;
            for (var i = 0; i < entries.length; i++) {
                var entry = entries[i];
                var id = source.getId() + "_" + entry.getId();
                var mapEntryInfo = this.addOrRemoveEntryMarker(id, entries[i], true);
                if (entry.hasBounds()) {
                    if (entry.getNorth() > 90 ||
                        entry.getSouth() < -90 ||
                        entry.getEast() > 180 ||
                        entry.getWest() < -180) {
                        console.log("bad bounds on entry:" + entry.getName() + " " +
				    entry.getNorth() + " " +
				    entry.getSouth() + " " +
				    entry.getEast() + " " +
				    entry.getWest());
                        continue;
                    }

                    north = Math.max(north, entry.getNorth());
                    south = Math.min(south, entry.getSouth());
                    east = Math.max(east, entry.getEast());
                    west = Math.min(west, entry.getWest());
                    didOne = true;
                }
            }
            var bounds = (didOne ? createBounds(west, south, east, north) : null);
            //debug                    this.map.centerOnMarkers(bounds, true);
        },
        handleEventEntrySelection: function(source, args) {
            if (!this.map) {
                return;
            }
            var _this = this;
            var entry = args.entry;
            if (entry == null) {
                this.map.clearSelectionMarker();
                return;
            }
            var selected = args.selected;

            if (!entry.hasLocation()) {
                return;
            }

            /*
              if (selected) {
              this.lastSource = source;
              this.map.setSelectionMarker(entry.getLongitude(), entry.getLatitude(), true, args.zoom);
              } else if (source == this.lastSource) {
              this.map.clearSelectionMarker();
              }
            */
        },
        addOrRemoveEntryMarker: function(id, entry, add, args) {
            if (!args) {
                args = {};
            }
            var dflt = {
                lineColor: entry.lineColor,
                fillColor: entry.lineColor,
                lineWidth: entry.lineWidth,
                doCircle: false,
                doRectangle: this.showBoxes,
                fill: false,
                fillOpacity: 0.75,
                pointRadius: 12,
                polygon: {},
                circle: {}
            }
            dfltPolygon = {}
            dfltCircle = {}
            $.extend(dflt, args);
            if (!dflt.lineColor) dflt.lineColor = "blue";

            $.extend(dfltPolygon, dflt);
            if (args.polygon)
                $.extend(dfltPolygon, args.polygon);
            $.extend(dfltCircle, dflt);
            if (args.circle)
                $.extend(dfltCircle, args.circle);

            var mapEntryInfo = this.mapEntryInfos[id];
            if (!add) {
                if (mapEntryInfo != null) {
                    mapEntryInfo.removeFromMap(this.map);
                    this.mapEntryInfos[id] = null;
                }
            } else {
                if (mapEntryInfo == null) {
                    mapEntryInfo = new MapEntryInfo(entry);
                    this.mapEntryInfos[id] = mapEntryInfo;
                    if (entry.hasBounds() && dflt.doRectangle) {
                        var attrs = {};
                        mapEntryInfo.rectangle = this.map.addRectangle(id,
								       entry.getNorth(), entry.getWest(), entry
								       .getSouth(), entry.getEast(), attrs);
                    }
                    var latitude = entry.getLatitude();
                    var longitude = entry.getLongitude();
                    if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                        return;
                    }
                    var point = new OpenLayers.LonLat(longitude, latitude);
                    if (dflt.doCircle) {
                        attrs = {
                            pointRadius: dfltCircle.pointRadius,
                            stroke: true,
                            strokeColor: dfltCircle.lineColor,
                            strokeWidth: dfltCircle.lineWidth,
                            fillColor: dfltCircle.fillColor,
                            fillOpacity: dfltCircle.fillOpacity,
                            fill: dfltCircle.fill,
                        };
                        mapEntryInfo.circle = this.map.addPoint(id, point, attrs);
                    } else {
                        mapEntryInfo.marker = this.map.addMarker(id, point, entry.getIconUrl(), "", this.getEntryHtml(entry));
                    }
                    if (entry.polygon) {
                        var points = []
                        for (var i = 0; i < entry.polygon.length; i += 2) {
                            points.push(new OpenLayers.Geometry.Point(entry.polygon[i + 1], entry.polygon[i]));
                        }
                        var attrs = {
                            strokeColor: dfltPolygon.lineColor,
                            strokeWidth: Utils.isDefined(dfltPolygon.lineWidth) ? dfltPolygon.lineWidth : 2
                        };
                        mapEntryInfo.polygon = this.map.addPolygon(id, entry.getName(), points, attrs, mapEntryInfo.marker);
                    }
                    var theDisplay = this;
                    if (mapEntryInfo.marker) {
                        mapEntryInfo.marker.entry = entry;
                        mapEntryInfo.marker.ramaddaClickHandler = function(marker) {
                            theDisplay.handleMapClick(marker);
                        };
                        if (this.handledMarkers == null) {
                            this.map.centerToMarkers();
                            this.handledMarkers = true;
                        }
                    }
                }
                return mapEntryInfo;
            }
        },
        handleMapClick: function(marker) {
            if (this.selectedMarker != null) {
                this.getDisplayManager().handleEventEntrySelection(this, {
                    entry: this.selectedMarker.entry,
                    selected: false
                });
            }
            this.getDisplayManager().handleEventEntrySelection(this, {
                entry: marker.entry,
                selected: true
            });
            this.selectedMarker = marker;
        },
        getDisplayProp: function(source, prop, dflt) {
            if (Utils.isDefined(this[prop])) {
                return this[prop];
            }
            prop = "map-" + prop;
            if (Utils.isDefined(source[prop])) {
                return source[prop];
            }
	    if(source.getProperty)
		return source.getProperty(prop, dflt);
	    return null;
        },
        applyVectorMap: function(force) {
            if (!force && this.vectorMapApplied) {
                return;
            }
            if (!this.doDisplayMap()) {
                return;
            }
            if (!this.vectorLayer) {
                return;
            }
            if (!this.points) {
                return;
            }
            this.vectorMapApplied = true;
            var features = this.vectorLayer.features.slice();
	    var maxExtent = null;
            var circles = this.points;
	    var doCount = this.getProperty("colorByCount",false);
	    for (var j = 0; j < features.length; j++) {
		var feature = features[j];
		feature.pointCount = 0;
		feature.circles = [];
		feature.featureIndex = j;
	    }
	    var matchedFeatures = [];
	    var seen = {};
	    var maxCnt = -1;
	    var minCnt = -1;
            for (var i = 0; i < circles.length; i++) {
                var circle = circles[i];
                if (circle.style && circle.style.display == "none") continue;
                var center = circle.center;
		var tmp = {index:-1,maxExtent: maxExtent};
                var matchedFeature = this.findContainingFeature(features, center,tmp);
		if(circle.hasColorByValue && isNaN(circle.colorByValue)) continue;
		if(!matchedFeature) continue;
		maxExtent = tmp.maxExtent;
		if(!seen[matchedFeature.featureIndex]) {
		    seen[matchedFeature.featureIndex] = true;
		    matchedFeatures.push(matchedFeature); 
		}
		matchedFeature.circles.push(circle);
		if(doCount) {
		    matchedFeature.pointCount++;
		    maxCnt = maxCnt==-1?matchedFeature.pointCount:Math.max(maxCnt, matchedFeature.pointCount);
		    minCnt = minCnt==-1?matchedFeature.pointCount:Math.min(minCnt, matchedFeature.pointCount);
		} else {
		    features.splice(tmp.index, 1);
		}
            }
	    if(!doCount) {
		for(var i=0;i<matchedFeatures.length;i++) {
		    var matchedFeature = matchedFeatures[i];
		    style = matchedFeature.style;
		    if (!style) style = {
			"stylename": "from display",
		    };
		    style.display = null;
		    var circle = matchedFeature.circles[0];
		    $.extend(style, circle.style);
		    matchedFeature.style = style;
		    matchedFeature.popupText = circle.text;
		    matchedFeature.dataIndex = i;
		}
	    }

	    var prune = this.getProperty("pruneFeatures", "false",true);
	    if(doCount) {
		//xxxxx
		var colors = this.getColorTable(true);
		if (colors == null) {
		    colors = Utils.ColorTables.grayscale.colors;
		}
		var range = maxCnt-minCnt;
		var labelSuffix = this.getProperty("doCountLabel","points");
		for (var j = 0; j < features.length; j++) {
		    var feature = features[j];
		    var percent = range==0?0:(feature.pointCount - minCnt) /range;
                    var index = parseInt(percent * colors.length);
                    if (index >= colors.length) index = colors.length - 1;
                    else if (index < 0) index = 0;
		    var color= colors[index];
		    var style = feature.style;
		    if (!style) style = {
			"stylename": "from display",
		    };
		    $.extend(style,{
			fillColor: color,
			"fillOpacity": 0.75,
			"strokeWidth": 1,
		    });

		    if(feature.pointCount==0) {
			//TODO: what to do with no count features?
			if(prune === true) {
			    style.display = "none";
			}
		    }
		    feature.style = style;
		    feature.dataIndex = j;
		    feature.popupText = HtmlUtils.div([],feature.pointCount +"&nbsp;" + labelSuffix);
		}
		this.displayColorTable(colors, ID_BOTTOM, minCnt,maxCnt,{});
	    } else {
		for (var i = 0; i < features.length; i++) {
		    var feature = features[i];
		    var style = feature.style;
		    if (!style) style = {
			"stylename": "from display"
		    };
		    $.extend(style, {
			"display": "none",
		    });
		    feature.style = style;
		}	
		/*
		  if(prune) {
		  this.vectorLayer.removeFeatures(features);
		  var dataBounds = this.vectorLayer.getDataExtent();
		  bounds = this.map.transformProjBounds(dataBounds);
		  if(!force && this.getProperty("bounds") == null)
		  this.map.centerOnMarkers(bounds, true);
		  }
		*/
	    }
            this.vectorLayer.redraw();
            if (maxExtent && !this.getProperty("bounds")) {
		this.map.getMap().zoomToExtent(maxExtent, true);
	    }

        },
	findContainingFeature: function(features, center, info) {
	    var matchedFeature = null;
            for (var j = 0; j < features.length; j++) {
                var feature = features[j];
                var geometry = feature.geometry;
                if (!geometry) {
                    continue;
                }
                bounds = geometry.getBounds();
                if (!bounds.contains(center.x, center.y)) {
                    continue;
                }
                if (geometry.components) {
                    for (var sub = 0; sub < geometry.components.length; sub++) {
                        comp = geometry.components[sub];
                        bounds = comp.getBounds();
                        if (!bounds.contains(center.x, center.y)) {
                            continue;
                        }
                        if (comp.containsPoint && comp.containsPoint(center)) {
                            matchedFeature = feature;
			    geometry = feature.geometry;
			    if (geometry) {
				if (info.maxExtent === null) {
				    info.maxExtent = new OpenLayers.Bounds();
				}
				info.maxExtent.extend(geometry.getBounds());
			    }
                            info.index = j;
                            break;
                        }
                    }
                    if (matchedFeature)
                        break;
                    continue;
                }
                if (!geometry.containsPoint) {
                    console.log("unknown geometry:" + geometry.CLASS_NAME);
                    continue;
                }
                if (geometry.containsPoint(center)) {
                    matchedFeature = feature;
                    info.index = j;
                    break;
                }
	    }
	    return matchedFeature;
	},
        needsData: function() {
            return true;
        },
	animationStart:function(animation) {
	    if(this.points) {
                for (var i = 0; i < this.points.length; i++) {
                    var point = this.points[i];
                    point.style.display = 'none';
                }
	    }
            if (this.map.circles)
                this.map.circles.redraw();

	},
        handleDateRangeChanged: function(source, prop) {
	    this.getAnimation().setDateRange(prop.minDate, prop.maxDate);
	    this.applyDateRange();
	},
       	animationApply: function(animation) {
	    SUPER.animationApply.call(this,animation,true);
	    this.applyDateRange();
	},
       	applyDateRange: function() {
	    var animation = this.getAnimation();
            var windowStart = animation.begin.getTime();
            var windowEnd = animation.end.getTime();
            var atLoc = {};
            for (var i = 0; i < this.lines.length; i++) {
                var line = this.lines[i];
                if (line.date < windowStart || line.date > windowEnd) {
                    line.style.display = 'none';
                    continue;
                }
                line.style.display = 'inline';
	    }

            for (var i = 0; i < this.points.length; i++) {
                var point = this.points[i];
                if (point.date < windowStart || point.date > windowEnd) {
                    point.style.display = 'none';
                    continue;
                }
                if (atLoc[point.location]) {
                    var other = atLoc[point.location];
                    if (other.date < point.date) {
                        atLoc[point.location] = point;
                        other.style.display = 'none';
                        point.style.display = 'inline';
                    } else {
                        point.style.display = 'none';
                    }
                    continue;
                }
                atLoc[point.location] = point;
                point.style.display = 'inline';
            }

            if (this.map.circles)
                this.map.circles.redraw();
            if (this.map.lines)
                this.map.lines.redraw();
            this.applyVectorMap(true);
	},
        showAllPoints: function() {
	    if(this.lines) {
		for (var i = 0; i < this.lines.length; i++) {
		    var line = this.lines[i];
		    line.style.display = 'inline';
		}
		if (this.map.lines)
		    this.map.lines.redraw();
	    }
            if (!this.points) return;
            for (var i = 0; i < this.points.length; i++) {
                var point = this.points[i];
                point.style.display = 'inline';
            }
            if (this.map.circles)
                this.map.circles.redraw();
            this.applyVectorMap(true);
        },

	colorByFieldChanged:function(field) {
	    this.haveCalledUpdateUI = false;
	    this.setProperty("colorBy", field);
	    this.vectorMapApplied  = false;
	    this.updateUI();
	},
	sizeByFieldChanged:function(field) {
	    this.haveCalledUpdateUI = false;
	    this.setProperty("sizeBy", field);
	    this.vectorMapApplied  = false;
	    this.updateUI();
	},
	dataFilterChanged: function() {
	    this.vectorMapApplied  = false;
	    this.updateUI();
	    if(this.getProperty("centerOnFilterChange",false)) {
		if (this.vectorLayer && this.points) {
		    //If we have  a map then don't do anything?
		} else {
		    this.map.centerOnMarkers(null, false, false);
		}
	    }
	},
        updateUI: function() {
            SUPER.updateUI.call(this);
            if (!this.getDisplayReady()) {
                return;
            }
            if (!this.hasData()) {
                return;
            }
            if (!this.getProperty("showData", true)) {
                return;
            }
            var pointData = this.getPointData();
            var records = this.filterData();
            if (records == null) {
                err = new Error();
                console.log("null records:" + err.stack);
                return;
            }

	    
	    if(this.haveCalledUpdateUI) {
		return;
	    }
	    this.haveCalledUpdateUI = true;
            var fields = pointData.getRecordFields();
            var bounds = {};
            var points = RecordUtil.getPoints(records, bounds);
            var showSegments = this.getProperty("showSegments", false);



	    if(records.length!=0) {
		if (isNaN(bounds.north)) {
		    console.log("no bounds:" + bounds);
		    return;
		}
		//		console.log("bounds:" + bounds.north +" " + bounds.west +" " + bounds.south +" " + bounds.east);
		this.initBounds = bounds;
		if(!showSegments) {
		    this.setInitMapBounds(bounds.north, bounds.west, bounds.south,
					  bounds.east);
		}
	    }
	    if (this.map == null) {
		return;
	    }
            if (points.length == 0) {
                //console.log("points.length==0");
		//                return;
            }

            this.addPoints(records,fields,points);
            this.addLabels(records,fields,points);
            this.applyVectorMap();
	},

        addPoints: function(records, fields, points) {
	    var polygonField = this.getFieldById(fields, this.getProperty("polygonField"));
            var source = this;
            var radius = parseFloat(this.getDisplayProp(source, "radius", 8));
            var strokeWidth = parseFloat(this.getDisplayProp(source, "strokeWidth", "1"));
            var strokeColor = this.getDisplayProp(source, "strokeColor", "#000");
            var colorByAttr = this.getDisplayProp(source, "colorBy", null);
	    var colors;
	    if(colorByAttr) {
		//First get the ct from the field name
		colors = this.getColorTable(true,colorByAttr +".colorTable");
		if(!colors) {
		    var c = this.getProperty(colorByAttr +".colors");
		    if(c)
			colors = c.split(",");
		}
		if(!colors)
		    colors = this.getColorTable(true);
	    }

            var sizeByAttr = this.getDisplayProp(source, "sizeBy", null);
            var isTrajectory = this.getDisplayProp(source, "isTrajectory", false);
            if (isTrajectory) {
                var attrs = {
                    strokeWidth: 2,
                    strokeColor: "blue"
                }

                this.map.addPolygon("id", "", points, attrs, null);
                return;
            }
            if (!colors && source.colors && source.colors.length > 0) {
                colors = source.colors;
                if (colors.length == 1 && Utils.ColorTables[colors[0]]) {
                    colors = Utils.ColorTables[colors[0]].colors;
                }
            }

            if (colors == null) {
                colors = Utils.ColorTables.grayscale.colors;
            }

            var latField1 = this.getFieldById(fields, this.getProperty("latField1"));
            var latField2 = this.getFieldById(fields, this.getProperty("latField2"));
            var lonField1 = this.getFieldById(fields, this.getProperty("lonField1"));
            var lonField2 = this.getFieldById(fields, this.getProperty("lonField2"));
            var sizeSegments = this.getProperty("sizeSegments", false);
            var sizeEndPoints = this.getProperty("sizeEndPoints", true);
            var showEndPoints = this.getProperty("showEndPoints", false);
            var endPointSize = parseInt(this.getProperty("endPointSize", "4"));
            var dfltEndPointSize = endPointSize;
            var segmentWidth = parseInt(this.getProperty("segmentWidth", "1"));
            var dfltSegmentWidth = segmentWidth;
            var showPoints = this.getProperty("showPoints", true);
            var lineColor = this.getProperty("lineColor", "green");
	    var pointIcon = this.getProperty("pointIcon");
	    if(pointIcon) this.pointsAreMarkers = true;
            var iconField = this.getFieldById(fields, this.getProperty("iconField"));
            var iconSize = parseFloat(this.getProperty("iconSize",32));
	    if(iconField)
		this.pointsAreMarkers = true;
	    var dfltShape = this.getProperty("defaultShape",null);
	    var dfltShapes = ["circle","triangle","star",  "square", "cross","x", "lightning","rectangle","church"];
	    var dfltShapeIdx=0;
	    var shapeBy = {
		id: this.getDisplayProp(source, "shapeBy", null),
		field:null,
		map: {}
	    }

	    if(this.getDisplayProp(source, "shapeByMap", null)) {
		this.getDisplayProp(source, "shapeByMap", null).split(",").map((pair)=>{
		    var tuple = pair.split(":");
		    shapeBy.map[tuple[0]] = tuple[1];
		})
	    }

            var colorBy = {
                id: colorByAttr,
                minValue: 0,
                maxValue: 0,
                field: null,
                index: -1,
                isString: false,
                stringMap: null
            };


            var sizeBy = {
                id: this.getDisplayProp(source, "sizeBy", null),
                minValue: 0,
                maxValue: 0,
                field: null,
                index: -1,
                isString: false,
                stringMap: {},
            };

            var sizeByMap = this.getProperty("sizeByMap");
            if (sizeByMap) {
                var toks = sizeByMap.split(",");
                for (var i = 0; i < toks.length; i++) {
                    var toks2 = toks[i].split(":");
                    if (toks2.length > 1) {
                        sizeBy.stringMap[toks2[0]] = toks2[1];
                    }
                }
            }

            for (var i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (field.getId() == colorBy.id || ("#" + (i + 1)) == colorBy.id) {
                    colorBy.field = field;
		    if (field.isString()) colorBy.isString = true;
                }
                if (field.getId() == shapeBy.id || ("#" + (i + 1)) == shapeBy.id) {
                    shapeBy.field = field;
		    if (field.isString()) shapeBy.isString = true;
                }
                if (field.getId() == sizeBy.id || ("#" + (i + 1)) == sizeBy.id) {
                    sizeBy.field = field;
		    if (field.isString()) sizeBy.isString = true;
                }
            }


            if (this.getProperty("showColorByMenu", false) && colorBy.field && !this.madeColorByMenu) {
                this.madeColorByMenu = true;
                var menu = "<select class='ramadda-pulldown' id='" + this.getDomId("colorByMenu") + "'>";
                for (var i = 0; i < fields.length; i++) {
                    var field = fields[i];
                    if (!field.isNumeric || field.isFieldGeo()) continue;
                    var extra = "";
                    if (colorBy.field.getId() == field.getId()) extra = "selected ";
                    menu += "<option value='" + field.getId() + "' " + extra + ">" + field.getLabel() + "</option>\n";
                }
                menu += "</select>";
                this.writeHtml(ID_TOP_RIGHT, "Color by: " + menu);
                /*
                  this.jq("colorByMenu").superfish({
                  //Don't set animation - it is broke on safari
                  //                    animation: {height:'show'},
                  speed: 'fast',
                  delay: 300
                  });
                */
                this.jq("colorByMenu").change(() => {
                    var value = this.jq("colorByMenu").val();
                    this.vectorMapApplied = false;
		    this.haveCalledUpdateUI = false;
                    this.setProperty("colorBy", value);
                    this.updateUI();
                });
            }

            sizeBy.index = sizeBy.field != null ? sizeBy.field.getIndex() : -1;
            colorBy.index = colorBy.field != null ? colorBy.field.getIndex() : -1;
            shapeBy.index = shapeBy.field != null ? shapeBy.field.getIndex() : -1;
            var excludeZero = this.getProperty(PROP_EXCLUDE_ZERO, false);
	    var dateMin = null;
	    var dateMax = null;

	    var colorByMapProp = this.getProperty("colorByMap");
            if (colorByMapProp) {
                var toks = colorByMapProp.split(",");
		colorBy.stringMap = {};
                for (var i = 0; i < toks.length; i++) {
                    var toks2 = toks[i].split(":");
                    if (toks2.length > 1) {
                        colorBy.stringMap[toks2[0]] = toks2[1];
                    }
                }
            }

            var colorByMap = {};
            var colorByValues = [];

            var colorByMinPerc = this.getDisplayProp(source, "colorByMinPercentile", -1);
            var colorByMaxPerc = this.getDisplayProp(source, "colorByMaxPercentile", -1);

	    var dates = [];
            var justOneMarker = this.getProperty("justOneMarker",false);
            for (var i = 0; i < points.length; i++) {
                var pointRecord = records[i];
		dates.push(pointRecord.getDate());
                if (dateMin == null) {
                    dateMin = pointRecord.getDate();
                    dateMax = pointRecord.getDate();
                } else {
                    var date = pointRecord.getDate();
                    if (date) {
                        if (date.getTime() < dateMin.getTime())
                            dateMin = date;
                        if (date.getTime() > dateMax.getTime())
                            dateMax = date;
                    }
                }
                var tuple = pointRecord.getData();
                var v = tuple[colorBy.index];
                if (colorBy.isString) {
                    if (!Utils.isDefined(colorByMap[v])) {
                        colorByValues.push(v);
                        colorByMap[v] = colorByValues.length;
                        colorBy.minValue = 1;
                        colorBy.maxValue = colorByValues.length;
                        //                        console.log("cb:" +colorBy.minValue +" -  " +colorBy.maxValue);
                    }
                }


                if (excludeZero && v === 0) {
                    continue;
                }
                if (!colorBy.isString) {
		    if (!isNaN(v) && !(v === null)) {
			if (i == 0 || v > colorBy.maxValue) colorBy.maxValue = v;
			if (i == 0 || v < colorBy.minValue) colorBy.minValue = v;
		    }
                }
                if (!sizeBy.isString) {
                    v = tuple[sizeBy.index];
		    if (!isNaN(v) && !(v === null)) {
			if (i == 0 || v > sizeBy.maxValue) sizeBy.maxValue = v;
			if (i == 0 || v < sizeBy.minValue) sizeBy.minValue = v;
		    }
		    
                }
            }


            sizeBy.radiusMin = parseFloat(this.getProperty("sizeByRadiusMin", -1));
            sizeBy.radiusMax = parseFloat(this.getProperty("sizeByRadiusMax", -1));
            var sizeByOffset = 0;
            var sizeByLog = this.getProperty("sizeByLog", false);
            var sizeByFunc = Math.log;
            if (sizeByLog) {
                if (sizeBy.minValue < 1) {
                    sizeByOffset = 1 - sizeBy.minValue;
                }
                sizeBy.minValue = sizeByFunc(sizeBy.minValue + sizeByOffset);
                sizeBy.maxValue = sizeByFunc(sizeBy.maxValue + sizeByOffset);
            }
            sizeBy.range = sizeBy.maxValue - sizeBy.minValue;

            if (dateMax) {
		this.getAnimation().init(dateMin, dateMax,dates);
            }


            if (this.showPercent) {
                colorBy.minValue = 0;
                colorBy.maxValue = 100;
            }
            colorBy.minValue = this.getDisplayProp(source, "colorByMin", colorBy.minValue);
            colorBy.maxValue = this.getDisplayProp(source, "colorByMax", colorBy.maxValue);
            colorBy.origMinValue = colorBy.minValue;
            colorBy.origMaxValue = colorBy.maxValue;

            var colorByOffset = 0;
            var colorByLog = this.getProperty("colorByLog", false);
            var colorByFunc = Math.log;
            if (colorByLog) {
                if (colorBy.minValue < 1) {
                    colorByOffset = 1 - colorBy.minValue;
                }
                colorBy.minValue = colorByFunc(colorBy.minValue + colorByOffset);
                colorBy.maxValue = colorByFunc(colorBy.maxValue + colorByOffset);
            }
            colorBy.range = colorBy.maxValue - colorBy.minValue;
            //            console.log("cb:" + colorBy.minValue +" " + colorBy.maxValue+ " off:" + colorByOffset);


            if (this.points) {
                for (var i = 0; i < this.points.length; i++) {
		    if(this.pointsAreMarkers)
			this.map.removeMarker(this.points[i]);
		    else
			this.map.removePoint(this.points[i]);
		}
                this.points = [];
            }


            if (this.lines) {
                for (var i = 0; i < this.lines.length; i++)
                    this.map.removePolygon(this.lines[i]);
                this.lines = [];
            }

            if (!this.points) {
                this.points = [];
                this.lines = [];
            }

            var dontAddPoint = this.doDisplayMap();
            var didColorBy = false;
            var seen = {};
            for (var i = 0; i < points.length; i++) {
                var pointRecord = records[i];
                var tuple = pointRecord.getData();
                var point = points[i];
                if(justOneMarker) {
                    if(this.justOneMarker)
                        this.map.removeMarker(this.justOneMarker);
                    if(!isNaN(point.x) && !isNaN(point.y)) {
                        this.justOneMarker= this.map.addMarker(id, [point.x,point.y], null, "", "");
                        return;
                    } else {
                        continue;
                    }
                }



                var values = pointRecord.getData();
                var props = {
                    pointRadius: radius,
                    strokeWidth: strokeWidth,
                    strokeColor: strokeColor,
		    fillColor: this.getProperty("fillColor")
                };

		if(shapeBy.field) {
		    var gv = values[shapeBy.index];
		    if(gv)  {
			if(!Utils.isDefined(shapeBy.map[gv])) {
			    if(dfltShape) {
				shapeBy.map[gv] = dfltShape;
			    } else {
				if(dfltShapeIdx>=dfltShapes.length)
				    dfltShapeIdx = 0;
				shapeBy.map[gv] = dfltShapes[dfltShapeIdx++];
			    }
			}
			if(Utils.isDefined(shapeBy.map[gv])) {
			    props.graphicName = shapeBy.map[gv];
			}
		    }
		}




                if (sizeBy.index >= 0) {
                    var value = values[sizeBy.index];
                    if (sizeBy.isString) {
                        if (Utils.isDefined(sizeBy.stringMap[value])) {
                            var v = parseInt(sizeBy.stringMap[value]);
                            segmentWidth = dfltSegmentWidth + v;
                            props.pointRadius = v;
                        } else if (Utils.isDefined(sizeBy.stringMap["*"])) {
                            var v = parseInt(sizeBy.stringMap["*"]);
                            segmentWidth = dfltSegmentWidth + v;
                            props.pointRadius = v;
                        } else {
                            segmentWidth = dfltSegmentWidth;
                        }
                    } else {
                        var denom = sizeBy.range;
                        var v = value + sizeByOffset;
                        if (sizeByLog) v = sizeByFunc(v);
                        var percent = (denom == 0 ? NaN : (v - sizeBy.minValue) / denom);
                        if (sizeBy.radiusMax >= 0 && sizeBy.radiusMin >= 0) {
                            props.pointRadius = Math.round(sizeBy.radiusMin + percent * (sizeBy.radiusMax - sizeBy.radiusMin));
                        } else {
			    //xxxxx
                            props.pointRadius = 6 + parseInt(15 * percent);
                        }
                        if (sizeEndPoints) {
                            endPointSize = dfltEndPointSize + parseInt(10 * percent);
                        }
                        if (sizeSegments) {
                            segmentWidth = dfltSegmentWidth + parseInt(10 * percent);
			    if(segmentWidth==0 || isNaN(segmentWidth)) segmentWidth=1;
                        }
                    }
                    //                            console.log("percent:" + percent +  " radius: " + props.pointRadius +" Value: " + value  + " range: " + sizeBy.minValue +" " + sizeBy.maxValue);
                }
		if(isNaN(props.pointRadius) || props.pointRadius == 0) props.pointRadius= radius;
		var hasColorByValue = false;
		var colorByValue;

                if (colorBy.index >= 0) {
                    var value = pointRecord.getData()[colorBy.index];
		    hasColorByValue  = true;
		    colorByValue = value;
                    //                            console.log("value:" + value +" index:" + colorBy.index+" " + pointRecord.getData());
                    var percent = 0;
                    var msg = "";
                    var pctFields = null;
                    if (this.percentFields != null) {
                        pctFields = this.percentFields.split(",");
                    }
                    if (this.showPercent) {
                        var total = 0;
                        var data = pointRecord.getData();
                        var msg = "";
                        for (var j = 0; j < data.length; j++) {
                            var ok = fields[j].isNumeric && !fields[j].isFieldGeo();
                            if (ok && pctFields != null) {
                                ok = pctFields.indexOf(fields[j].getId()) >= 0 ||
                                    pctFields.indexOf("#" + (j + 1)) >= 0;
                            }
                            if (ok) {
                                total += data[j];
                                msg += " " + data[j];
                            }
                        }
                        if (total != 0) {
                            percent0 = percent = value / total * 100;
                            percent = (percent - colorBy.minValue) / (colorBy.maxValue - colorBy.minValue);
                            //                                    console.log("%:" + percent0 +" range:" + percent +" value"+ value +" " + total+"data: " + msg);
                        }

                    } else {
                        var v = value;
                        if (colorBy.isString) {
                            v = colorByMap[v];
                        }
                        v += colorByOffset;
                        if (colorByLog) {
                            v = colorByFunc(v);
                        }
                        percent = (v - colorBy.minValue) / colorBy.range;
                        //console.log("cbv:" +value +" %:" + percent);
                    }

                    var index = parseInt(percent * colors.length);
                    //                            console.log(colorBy.index +" value:" + value+ " " + percent + " " +index + " " + msg);
                    if (index >= colors.length) index = colors.length - 1;
                    else if (index < 0) index = 0;
                    //                            console.log("value:" + value+ " %:" + percent +" index:" + index +" c:" + colors[index]);

		    if(colorBy.stringMap) {
			props.fillColor = colorBy.stringMap[value];
			if(!Utils.isDefined(props.fillColor)) {
			    props.fillColor = colorBy.stringMap["default"];
			}
			if(!Utils.isDefined(props.fillColor)) {
			    props.fillColor = "blue";
			}
		    } else {
			props.fillColor = colors[index];
		    }
                    props.fillOpacity = 0.8;
                    didColorBy = true;
                }


                var html = this.getRecordHtml(pointRecord, fields);

		if(polygonField) {
		    var s = values[polygonField.getIndex()];
		    var delimiter;
		    [";",","].map(d=>{
			if(s.indexOf(d)>=0) delimiter = d;
		    });
		    var toks  = s.split(delimiter);
		    var p = [];
		    for(var pIdx=0;pIdx<toks.length;pIdx+=2) {
			var lat = parseFloat(toks[pIdx]);
			var lon = parseFloat(toks[pIdx+1]);
			p.push(new OpenLayers.Geometry.Point(lon,lat));
		    }
		    var poly = this.map.addPolygon("polygon" + pIdx, "",p,props,html);
		    if (date) {
			poly.date = date.getTime();
		    }
		    this.lines.push(poly);
		}




		var showSegments = this.getProperty("showSegments", false);
                if (showSegments && latField1 && latField2 && lonField1 && lonField2) {
                    var lat1 = values[latField1.getIndex()];
                    var lat2 = values[latField2.getIndex()];
                    var lon1 = values[lonField1.getIndex()];
                    var lon2 = values[lonField2.getIndex()];
                    var attrs = {};
                    if (props.fillColor)
                        attrs.strokeColor = props.fillColor;
                    else
                        attrs.strokeColor = lineColor;
                    attrs.strokeWidth = segmentWidth;
                    this.lines.push(this.map.addLine("line-" + i, "", lat1, lon1, lat2, lon2, attrs, html));
                    if (showEndPoints) {
                        var pointProps = {};
                        $.extend(pointProps, props);
                        pointProps.fillColor = attrs.strokeColor;
                        pointProps.strokeColor = attrs.strokeColor;
                        pointProps.pointRadius = dfltEndPointSize;
                        pointProps.pointRadius = endPointSize;
                        var p1 = new OpenLayers.LonLat(lon1, lat1);
                        var p2 = new OpenLayers.LonLat(lon2, lat2);
                        if (!Utils.isDefined(seen[p1])) {
                            seen[p1] = true;
                            this.points.push(this.map.addPoint("endpt-" + i, p1, pointProps, html));
                        }
                        if (!Utils.isDefined(seen[p2])) {
                            seen[p2] = true;
                            this.points.push(this.map.addPoint("endpt2-" + i, p2, pointProps, html));
                        }

                    }
		}

                if (showPoints) {
                    //We do this because openlayers gets really slow when there are lots of features at one point
                    if (!Utils.isDefined(seen[point])) seen[point] = 0;
                    if (seen[point] > 500) continue;
                    seen[point]++;
		    var mapPoint=null;
		    if(iconField) {
			var icon = tuple[iconField.getIndex()];
			var size = iconSize;
			if(sizeBy.index >= 0) {
			    size = props.pointRadius * size;
			}
			mapPoint = this.map.addMarker("pt-" + i, point, icon, "pt-" + i,html,null,size);
		    } else  if(pointIcon) {
			mapPoint = this.map.addMarker("pt-" + i, point, pointIcon, "pt-" + i,html);
		    } else {
			if(!props.graphicName)
			    props.graphicName = this.getProperty("shape","circle");
			if(radius>0)
			    mapPoint = this.map.addPoint("pt-" + i, point, props, html, dontAddPoint);
		    }

                    var date = pointRecord.getDate();
		    if(mapPoint) {
			mapPoint.hasColorByValue = hasColorByValue;
		 	mapPoint.colorByValue= colorByValue;
			if (date) {
			    mapPoint.date = date.getTime();
			}
			this.points.push(mapPoint);
		    }
		}
	    }

	    if (showSegments) {
		this.map.centerOnMarkers(null, true, null);
	    }


	    if(this.map.circles)
		this.map.circles.redraw();
	    this.jq(ID_BOTTOM).html(HtmlUtils.div(["id",this.getDomId(ID_COLORTABLE)])+
				    HtmlUtils.div(["id",this.getDomId(ID_SHAPES)]));
            if (didColorBy) {
		if(colorBy.stringMap) {
		    var colors = [];
		    colorByValues= [];
		    for (var i in colorBy.stringMap) {
			colorByValues.push(i);
			colors.push(colorBy.stringMap[i]);
		    }
		    this.displayColorTable(colors, ID_COLORTABLE, colorBy.origMinValue, colorBy.origMaxValue, {
			stringValues: colorByValues});
		} else {
		    this.displayColorTable(colors, ID_COLORTABLE, colorBy.origMinValue, colorBy.origMaxValue, {
			stringValues: colorByValues
		    });
		}
            }

	    if(shapeBy.field) {
		var shapes = shapeBy.field.getLabel()+": ";
		for(v in shapeBy.map) {
		    var shape = shapeBy.map[v];
		    if(shape=="circle") shape=HtmlUtils.getIconImage("fa-circle");
		    else if(shape=="square") shape=HtmlUtils.getIconImage("fa-square");		    
		    else if(shape=="rectangle") shape=HtmlUtils.getIconImage("fa-square");		    
		    else if(shape=="star") shape=HtmlUtils.getIconImage("fa-star");		    
		    else if(shape=="triangle") shape=HtmlUtils.getIconImage("/icons/triangle.png",["width","16px"]);		    
		    else if(shape=="lightning") shape=HtmlUtils.getIconImage("/icons/lightning.png",["width","16px"]);		    
		    else if(shape=="cross") shape=HtmlUtils.getIconImage("/icons/cross.png",["width","16px"]);		    
		    else if(shape=="church") shape=HtmlUtils.getIconImage("fa-cross");
		    shapes+=shape+" " + v +"&nbsp;&nbsp;"
		}
		this.jq(ID_SHAPES).html("<center>" +shapes+"</center>");
	    }

	    if (this.getProperty("animationTakeStep", false)) {
		this.getAnimation().doNext();
	    }


        },
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(), [
		"label:Map Attributes",
		"defaultMapLayer =\"\"",
		"showLocationSearch=\"true\"",
		"strokeWidth=1",
		"strokeColor=\"#000\"",
		"fillColor=\"\"",
		"radius=\"5\"",
		"shape=\"triangle\"",
		"colorBy=\"\"",
		"colorByLog=\"true\"",
		"colorByMap=\"value1:color1,...,valueN:colorN\"",
		"sizeBy=\"\"",
		"sizeByLog=\"true\"",
		"sizeByMap=\"value1:color1,...,valueN:colorN\"",
		'sizeByRadiusMin="2"',
		'sizeByRadiusMin="20"',
		"boundsAnimation=\"true\"",
		"centerOnFilterChange=\"true\"",
		"markerIcon=\"/icons/...\"",
		"showSegments=\"true\"",
		'showRecordSelection=false',
	    ]);
	},

        addLabels:function(records, fields, points) {
            var labelTemplate = this.getProperty("labelTemplate");
            if(!labelTemplate) return;
            labelTemplate = labelTemplate.replace(/_nl_/g,"\n");
	    if(!this.labelLayer) {
		this.labelLayer = new OpenLayers.Layer.Vector("Simple Geometry", {
                    styleMap: new OpenLayers.StyleMap({'default':{
                        label : labelTemplate,
                        fontColor: this.getProperty("labelFontColor","#000"),
                        fontSize: this.getProperty("labelFontSize","12px"),
                        fontFamily: this.getProperty("labelFontFamily","'Open Sans', Helvetica Neue, Arial, Helvetica, sans-serif"),
                        fontWeight: this.getProperty("labelFontWeight","plain"),
                        labelAlign: this.getProperty("labelAlign","lb"),
                        labelXOffset: this.getProperty("labelXOffset","0"),
                        labelYOffset: this.getProperty("labelYOffset","0"),
                        labelOutlineColor:this.getProperty("labelOutlineColor","#fff"),
                        labelOutlineWidth: this.getProperty("labelOutlineWidth","0"),
                    }}),
                });
		this.map.addVectorLayer(this.labelLayer, true);
	    }
	    var features =  [];
            var seen = {};
	    var colorBy = this.getProperty("colorBy");
	    var sizeBy = this.getProperty("sizeBy");
            for (var i = 0; i < records.length; i++) {
                var point = points[i];
                if(seen[point]) continue;
                seen[point] = true;
                var center = new OpenLayers.Geometry.Point(point.x, point.y);
                center.transform(this.map.displayProjection, this.map.sourceProjection);
                var pointRecord = records[i];
                var tuple = pointRecord.getData();
                var pointFeature = new OpenLayers.Feature.Vector(center);
                pointFeature.noSelect = true;
                pointFeature.attributes = {
                };
                for (var fieldIdx = 0;fieldIdx < fields.length; fieldIdx++) {
                    var field = fields[fieldIdx];
                    pointFeature.attributes[field.getId()] = field.getValue(tuple);
		    if(colorBy && field.getId() == colorBy) {
			pointFeature.attributes["colorBy"] = field.getValue(tuple);
		    }
		    if(sizeBy && field.getId() == sizeBy) {
			pointFeature.attributes["sizeBy"] = field.getValue(tuple);
		    }
                }
                features.push(pointFeature);
	    }
	    if(this.labelFeatures)
		this.labelLayer.removeFeatures(this.labelFeatures);
            this.labelLayer.addFeatures(features);
	    this.labelFeatures = features;
            $("#" + this.labelLayer.id).css("z-index",1000);
        },


        handleEventRemoveDisplay: function(source, display) {
            if (!this.map) {
                return;
            }
            var mapEntryInfo = this.mapEntryInfos[display];
            if (mapEntryInfo != null) {
                mapEntryInfo.removeFromMap(this.map);
            }
            var feature = this.findFeature(display, true);
            if (feature != null) {
                if (feature.line != null) {
                    this.map.removePolygon(feature.line);
                }
            }
        },
        findFeature: function(source, andDelete) {
            for (var i in this.features) {
                var feature = this.features[i];
                if (feature.source == source) {
                    if (andDelete) {
                        this.features.splice(i, 1);
                    }
                    return feature;
                }
            }
            return null;
        },

        getMarkerIcon: function() {
            if (this.getProperty("markerIcon")) {
                var icon = this.getProperty("markerIcon");
                if (icon.startsWith("/"))
                    return ramaddaBaseUrl + icon;
                else
                    return icon;
            }
            displayMapCurrentMarker++;
            if (displayMapCurrentMarker >= displayMapMarkers.length) displayMapCurrentMarker = 0;
            return ramaddaBaseUrl + "/lib/openlayers/v2/img/" + displayMapMarkers[displayMapCurrentMarker];
        },
        handleEventRecordSelection: function(source, args) {
            if (!this.getProperty("showRecordSelection", true)) return;
            if (!this.map) {
                return;
            }
            var record = args.record;
            if (record.hasLocation()) {
                var latitude = record.getLatitude();
                var longitude = record.getLongitude();
                if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) return;
                var point = new OpenLayers.LonLat(longitude, latitude);
                var marker = this.myMarkers[source];
                if (marker != null) {
                    this.map.removeMarker(marker);
                }
                var icon = displayMapMarkerIcons[source];
                if (icon == null) {
                    icon = this.getMarkerIcon();
                    displayMapMarkerIcons[source] = icon;
                }
                this.myMarkers[source] = this.map.addMarker(source.getId(), point, icon, "", args.html, null, 24);
            }
        }
    });
}

function MapEntryInfo(entry) {
    RamaddaUtil.defineMembers(this, {
        entry: entry,
        marker: null,
        rectangle: null,
        removeFromMap: function(map) {
            if (this.marker != null) {
                map.removeMarker(this.marker);
            }
            if (this.rectangle != null) {
                map.removePolygon(this.rectangle);
            }
            if (this.polygon != null) {
                map.removePolygon(this.polygon);
            }
            if (this.circle != null) {
                map.removePoint(this.circle);
            }
        }

    });
}


