/**
   Copyright 2008-2019 Geode Systems LLC
*/

let DISPLAY_MAP = "map";
let DISPLAY_MAPGRID = "mapgrid";

let displayMapMarkers = ["marker.png", "marker-blue.png", "marker-gold.png", "marker-green.png"];
let displayMapCurrentMarker = -1;
let displayMapUrlToVectorListeners = {};
let displayMapMarkerIcons = {};

addGlobalDisplayType({
    type: DISPLAY_MAP,
    label: "Map"
});
 
addGlobalDisplayType({
    type: DISPLAY_MAPGRID,
    label: "Map Grid"
});

function MapFeature(source, points) {
    RamaddaUtil.defineMembers(this, {
        source: source,
        points: points
    });
}


function RamaddaMapDisplay(displayManager, id, properties) {
    let ID_LATFIELD = "latfield";
    let ID_LONFIELD = "lonfield";
    let ID_MAP = "map";
    let ID_SHAPES = "shapes";
    let ID_HEATMAP_ANIM_LIST = "heatmapanimlist";
    let ID_HEATMAP_ANIM_PLAY = "heatmapanimplay";
    let SUPER;
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
            html += HtmlUtils.div([ATTR_CLASS, "display-map-map ramadda-expandable-target", "style",
				   extraStyle, ATTR_ID, this.getDomId(ID_MAP)
				  ]);
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
	    this.startProgress();

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
		params.displayDivSticky = this.getProperty("displayDivSticky", false);
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
		params.showScaleLine = this.getProperty("showScaleLine",false);
		params.showLatLonPosition = this.getProperty("showLatLonPosition",true);
		params.showZoomPanControl = this.getProperty("showZoomPanControl",false);
		params.showZoomOnlyControl = this.getProperty("showZoomOnlyControl",true);
                this.map = new RepositoryMap(this.getDomId(ID_MAP), params);
                this.lastWidth = this.jq(ID_MAP).width();

            }

	    if(!this.getProperty("markersVisibility", true)) {
		this.map.getMarkersLayer().setVisibility(false);
	    }

            if (this.doDisplayMap()) {
                this.map.setDefaultCanSelect(false);
            }
            this.map.initMap(false);

            this.map.addRegionSelectorControl(function(bounds) {
                theDisplay.getDisplayManager().handleEventMapBoundsChanged(this, bounds, true);
            });
	    this.map.addFeatureSelectHandler(feature=>{
		if(feature.record && !this.map.doPopup) {
		    this.highlightPoint(feature.record.getLatitude(),feature.record.getLongitude(),true,false);
		}
	    });

            this.map.addFeatureHighlightHandler((feature, highlight)=>{
		if(feature.record) {
		    if(this.lastHighlightedRecord) {
			var args = {highlight:false,record: this.lastHighlightedRecord};
			this.getDisplayManager().notifyEvent("handleEventRecordHighlight", this, args);
			if (this.getProperty("doAnimation", false)) {
			    this.getAnimation().handleEventRecordHighlight(this, args);
			}
			this.lastHighlightedRecord = null;
		    }
		    if(highlight) {
			this.lastHighlightedRecord = feature.record;
		    }
		    var args = {highlight:highlight,record: feature.record};
		    this.getDisplayManager().notifyEvent("handleEventRecordHighlight", this, args);
		    if (this.getProperty("doAnimation", false)) {
			this.getAnimation().handleEventRecordHighlight(this, args);
		    }
		}

	    });

	    this.map.highlightBackgroundColor=this.getProperty("highlighBackgroundColor","rgba(0,0,0,0)");
	    this.map.doPopup = this.getProperty("doPopup",true);
            this.map.addClickHandler(this.getDomId(ID_LONFIELD), this
				     .getDomId(ID_LATFIELD), null, this);
            this.map.getMap().events.register("zoomend", "", function() {
                theDisplay.mapBoundsChanged();
            });
            this.map.getMap().events.register("moveend", "", function() {
                theDisplay.mapBoundsChanged();
            });

            var overrideBounds = false;
            if (this.getProperty("bounds") ||this.getProperty("gridBounds") ) {
                overrideBounds = true;
                var toks = this.getProperty("bounds", this.getProperty("gridBounds","")).split(",");
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
        getBounds: function() {
	    return this.map.getBounds();
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
        handleEventFieldsSelected: function(source, fields) {
	    if(this.getProperty("selectedFieldIsColorBy") && fields.length>0) {
		this.colorByFieldChanged(fields[0]);
	    }
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
            let bounds = this.map.getMap().calculateBounds().transform(this.map.sourceProjection,
								       this.map.displayProjection);
            this.getDisplayManager().handleEventMapBoundsChanged(this, bounds);

	    if(this.clipToView) {
		if(this.lastUpdateTime) {
		    let now = new Date();
		    if(now.getTime()-this.lastUpdateTime.getTime()>1000) {
			this.haveCalledUpdateUI = false;
			this.clipBounds = true;
			this.updateUI();
		    }
		}
	    }

        },
        addFeature: function(feature) {
            this.features.push(feature);
            feature.line = this.map.addPolygon("lines_" +
					       feature.source.getId(), RecordUtil
					       .clonePoints(feature.points), null);
        },
        getContentsDiv: function() {
            return HtmlUtils.div([ATTR_CLASS, "display-contents", ATTR_ID,
				  this.getDomId(ID_DISPLAY_CONTENTS)
				 ], "");
        },
	highlightMarker:null,
        handleEventRecordHighlight: function(source, args) {
	    SUPER.handleEventRecordHighlight.call(this,source,args);
	    this.highlightPoint(args.record.getLatitude(),args.record.getLongitude(),args.highlight,true);
	},
	highlightPoint: function(lat,lon,highlight,andCenter) {
	    if(!this.map) return;
	    if(this.highlightMarker) {
		this.map.removePoint(this.highlightMarker);
		this.map.removeMarker(this.highlightMarker);
		this.highlightMarker = null;
	    }
	    if(highlight) {
		var point = new OpenLayers.LonLat(lon,lat);
                var attrs = {
                    pointRadius: parseFloat(this.getProperty("recordHighlightRadius", +this.getProperty("radius",6)+8)),
                    stroke: true,
                    strokeColor: this.getProperty("recordHighlightStrokeColor", "#000"),
                    strokeWidth: parseFloat(this.getProperty("recordHighlightStrokeWidth", 2)),
		    fillColor: this.getProperty("recordHighlightFillColor", "#ccc"),
		    fillOpacity: parseFloat(this.getProperty("recordHighlightFillOpacity", 0.5)),
                };
		if(this.getProperty("recordHighlightUseMarker",false)) {
		    var size = parseFloat(this.getProperty("recordHighlightRadius", +this.getProperty("radius",24)));
		    this.highlightMarker = this.map.addMarker("pt-" + i, point, null, "pt-" + i,null,null,size);
		} else {
		    this.highlightMarker =  this.map.addPoint("highlight", point, attrs);
		}
		if(andCenter && this.getProperty("centerOnHighlight",false)) {
		    this.map.setCenter(point);
		}
	    }
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
	    var linkField=this.getFieldById(null,this.getProperty("linkField"));
	    var linkFeature=this.getProperty("linkFeature");
            var features = this.vectorLayer.features.slice();
	    var recordToFeature = {};

	    if(linkFeature && linkField) {
		var recordMap = {};
		this.points.map(p=>{
		    var record = p.record;
		    if(record) {
			var tuple = record.getData();
			var value = tuple[linkField.getIndex()];
			value  = value.toString().trim();
			record.linkValue = value;
			if(value.indexOf("xxxx0803")>=0)
			    console.log("record:" + value.replace(/ /g,"X") +":");
			recordMap[value] = record;
		    }
		});



		features.map(feature=>{
		    var attrs = feature.attributes;
		    var ok = false;
		    for (var attr in attrs) {
			if(linkFeature==attr) {
			    ok  = true;
			    var value = this.map.getAttrValue(attrs, attr);
			    let debug = false;
			    if(value) {
				if(debug)
				    console.log("\tbefore");
				value = value.toString().trim();
				feature.linkValue = value;
				record = recordMap[value];
				if(record) {
				    if(debug)
					console.log("\tAdding:" + value+": " + record.getId());
				    recordToFeature[record.getId()] = feature;
				} else {
				    if(debug)
					console.log("\tCould not find record:" + value.replace(/ /g,"X") +":");
				}
				if(debug)
				    console.log("\tAFter");
			    } else {
				console.log("no attr value");
			    }
			    
			}
		    }
		    if(!ok) console.log("No ATTR found");
		});
	    }

	    var j=0;
	    features.map(feature=>{
		feature.featureIndex = j++;
		feature.featureMatched = false;
		feature.pointCount = 0;
		feature.circles = [];
	    });


            if (!this.points) {
                return;
            }
            this.vectorMapApplied = true;

	    var maxExtent = null;
            var circles = this.points;
	    var doCount = this.getProperty("colorByCount",false);
	    for (var j = 0; j < features.length; j++) {
		var feature = features[j];
	    }
	    var matchedFeatures = [];
	    var seen = {};
	    var maxCnt = -1;
	    var minCnt = -1;

            for (var i = 0; i < circles.length; i++) {
                var circle = circles[i];
                if (circle.style && circle.style.display == "none") continue;
		var record = circle.record;
                var center = circle.center;
		var tmp = {index:-1,maxExtent: maxExtent};
		var matchedFeature = recordToFeature[record.getId()];
		if(matchedFeature) {
		    matchedFeature.featureMatched = true;
		} 
		if(!matchedFeature) 
                    matchedFeature = this.findContainingFeature(features, center,tmp);
		if(!matchedFeature) continue;
		if(!circle.colorByColor && circle.hasColorByValue && isNaN(circle.colorByValue)) {
		    continue;
		}
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
		    if(tmp.index>=0)
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

	    var prune = this.getProperty("pruneFeatures", true);
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
		if(prune) {
		    for (var i = 0; i < features.length; i++) {
			var feature = features[i];
			if(feature.featureMatched) {
			    continue;
			}
			var style = feature.style;
			if (!style) style = {
			    "stylename": "from display"
			};
			$.extend(style, {
			    "display": "none",
			});
			feature.style = style;
		    }
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
	    if(this.lines==null) return
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

	    if (this.map.lines)
                this.map.lines.redraw();
	    if (this.map.markers)
                this.map.markers.redraw();
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
	    this.updateUI(true, ()=>{
		if(this.getProperty("centerOnFilterChange",false)) {
		    if (this.vectorLayer && this.points) {
			//If we have  a map then don't do anything?
		    } else if(this.lastImageLayer) {
			this.map.zoomToLayer(this.lastImageLayer);
		    } else {
			this.map.centerOnMarkers(null, false, false);
		    }
		}
	    });
	},
	requiresGeoLocation: function() {
	    return true;
	},
	getHeader2:function() {
	    let html = SUPER.getHeader2.call(this);
	    if(this.getProperty("showClipToBounds")) {
		this.clipToView=false;
		html =  HtmlUtils.div(["style","display:inline-block;cursor:pointer;padding:1px;border:1px solid rgba(0,0,0,0);", "title","Clip to view", "id",this.getDomId("clip")],HtmlUtils.getIconImage("fa-globe-americas"))+"&nbsp;&nbsp;"+ html;
	    }
	    if(this.getProperty("showMarkersToggle")) {
		let dflt = this.getProperty("markersVisibility", true);
		html += HtmlUtils.checkbox("",["id",this.getDomId("showMarkersToggle")],dflt) +" " +
		    this.getProperty("showMarkersToggleLabel","Show Markers") +"&nbsp;&nbsp;";
	    }
	    return html;
	},
	xcnt:0,
	initHeader2:function() {
	    let _this = this;
	    this.jq("showMarkersToggle").change(function() {
		_this.map.circles.setVisibility($(this).is(':checked'));
	    });
	    this.jq("clip").click(function(e){
		console.log("clip:" + _this.clipToView);
		_this.clipToView = !_this.clipToView;
		if(!_this.clipToView) {
		    $(this).css("border","1px solid rgba(0,0,0,0)");
		    _this.clipToView = false;
		    return;
		}
		$(this).css("border","1px solid #aaa");
		_this.haveCalledUpdateUI = false;
		_this.clipBounds = true;
		_this.updateUI();
	    });
	},

	handleNoData: function(pointData,reload) {
	    this.jq(ID_PAGE_COUNT).html("");
            this.addPoints([],[],[]);
	    this.setMessage("No data available");
	},
	setErrorMessage: function(msg) {
	    if(this.map)
		this.map.setProgress(HtmlUtils.div([ATTR_CLASS, "display-map-message"], msg));
	    else
		SUPER.setErrorMessage.call(this,msg);
	},
	setMessage: function(msg) {
	    if(this.map) {
		this.map.setProgress(HtmlUtils.div([ATTR_CLASS, "display-map-message"], msg));
	    }
	},
	setMapLabel: function(msg) {
	    if(this.map)
		this.map.setLabel(HtmlUtils.div([ATTR_CLASS, "display-map-message"], msg));
	},	
	startProgress: function() {
	    this.setMessage(this.getProperty("loadingMessage","Loading map..."));
	},
	clearProgress: function() {
	    if(this.errorMessage) {
		this.errorMessage = null;
		return;
	    }
	    if(this.map)
		this.map.setProgress("");
	},
        updateUI: function(reload, callback) {
	    this.lastUpdateTime = null;
            SUPER.updateUI.call(this,reload);
            if (!this.getDisplayReady()) {
                return;
            }
            if (!this.hasData()) {
                return;
            }
            if (!this.getProperty("showData", true)) {
                return;
            }
	    if(this.haveCalledUpdateUI) {
		return;
	    }
            let pointData = this.getPointData();
            let records = this.records =  this.filterData();
            if (records == null) {
                err = new Error();
                console.log("null records:" + err.stack);
                return;
            }

	    this.setMessage("Creating display...");
	    setTimeout(()=>{
		try {
		    this.updateUIInner(pointData, records);
		    if(callback)callback();
		} catch(exc) {
		    console.log(exc)
		    console.log(exc.stack);
		    this.setMessage("" + exc);
		    return;
		}
		this.clearProgress();
	    });

	},
	
	animationApply: function(animation, skipUpdateUI) {
 	    if(!this.heatmapLayers) {
		SUPER.animationApply.call(this, animation, skipUpdateUI);
		return;
	    }
	    let onDate=null;
//	    console.log("displaymap.animationApply:" + animation.begin + " " +animation.end);
	    let onLayer = null;
	    let offLayers = [];
	    this.heatmapLayers.every(layer=>{
		if(!layer.date) return true;
		if(layer.date.getTime()>= animation.begin.getTime() && layer.date.getTime()<= animation.end.getTime()) {
		    onDate = layer.date;
		    onLayer = layer;
		    layer.setVisibility(true);
		} else {
		    if(layer.getVisibility()) 
			offLayers.push(layer);
		}
		return true;
	    })
	    offLayers.map(layer=>{
		layer.setVisibility(false);
	    });
 	    if(!onDate) {
		SUPER.animationApply.call(this, animation, skipUpdateUI);
	    }
	    if(onLayer!=null)
		this.setMapLabel(onLayer.heatmapLabel);
	},
        setDateRange: function(min, max) {
	    if(this.getProperty("doGridPoints",false)|| this.getProperty("doHeatmap",false)) {
	    } else {
		SUPER.setDateRange.call(this, min,max);
	    }
	},
	updateUIInner: function(pointData, records) {
	    var t1= new Date();
	    let debug = displayDebug.displayMapUpdateUI;
	    if(debug) console.log("displaymap.updateUIInner");
	    this.haveCalledUpdateUI = true;
            let pointBounds = {};
            let points = RecordUtil.getPoints(records, pointBounds);

	    if(this.clipBounds) {
		this.clipBounds = false;
		let clipRecords = false;
		if(this.lastPointBounds && this.lastPointBounds!=pointBounds) {
		    clipRecords = true;
		}
		this.lastPointBounds = pointBounds;
		if(clipRecords) {
		    let viewbounds = this.map.getMap().calculateBounds().transform(this.map.sourceProjection, this.map.displayProjection);
		    let tmpRecords =records.filter(r=>{
			return viewbounds.containsLonLat(new OpenLayers.LonLat(r.getLongitude(),r.getLatitude()));
		    });
		    //		console.log("clipped records:" + tmpRecords.length);
		    this.records = records = tmpRecords;
		}
	    }

            let fields = pointData.getRecordFields();
            let showSegments = this.getProperty("showSegments", false);

	    if(records.length!=0) {
		if (!isNaN(pointBounds.north)) {
		    this.initBounds = pointBounds;
		    if(!showSegments) {
			this.setInitMapBounds(pointBounds.north, pointBounds.west, pointBounds.south,
					      pointBounds.east);
		    }
		}
	    }
	    if (this.map == null) {
		return;
	    }
	    if(this.highlightMarker) {
		this.map.removePoint(this.highlightMarker);
		this.map.removeMarker(this.highlightMarker);
		this.highlightMarker = null;
	    }
	    this.map.clearSeenMarkers();
	    var t2= new Date();
	    if(debug) console.log("displaymap calling addPoints");
            this.addPoints(records,fields,points,pointBounds);
	    var t3= new Date();
	    if(debug) Utils.displayTimes("time pts=" + points.length,[t1,t2,t3], true);
            this.addLabels(records,fields,points);
            this.applyVectorMap();
	    this.lastUpdateTime = new Date();
	},
	heatmapCnt:0,
	applyHeatmapAnimation: function(index) {
	    this.jq(ID_HEATMAP_ANIM_LIST)[0].selectedIndex = index;
	    let offLayers = [];
	    this.heatmapLayers.map((layer,idx)=>{
		if(index==idx)
		    layer.setVisibility(true);
		else
		    offLayers.push(layer);
	    });
	    offLayers.map(layer=>{
		layer.setVisibility(false);
	    });
	    this.setMapLabel(this.heatmapLayers[index].heatmapLabel);

	},
	stepHeatmapAnimation: function(){
	    let index = this.jq(ID_HEATMAP_ANIM_LIST)[0].selectedIndex;
	    index++;
	    if(index>=this.heatmapLayers.length) {
		index =0;
	    }
	    this.applyHeatmapAnimation(index);
	    if(this.heatmapPlayingAnimation) {
		setTimeout(()=>{
		    this.stepHeatmapAnimation();
		},this.getProperty("hm.AnimationSleep",1000));
	    }
	},
	createHeatmap(records, bounds) {
	    let debug = displayDebug.displayMapCreateMap;
	    if(debug) console.log("createHeatmap");
	    let colorBy = this.getColorByInfo(records, null,null,null,["hm.",""]);
	    records = records || this.filterData();
	    bounds = bounds ||  RecordUtil.getBounds(records);
 	    if(this.heatmapLayers) {
		try {
		    this.heatmapLayers.every(layer=>{
			this.map.removeLayer(layer);
			return true;
		    });
		} catch(exc) {
		    console.log(exc);
		}
	    }
	    this.heatmapLayers = [];
	    if(records.length==0) {
		this.errorMessage = "No data available";
		this.setMessage(this.errorMessage);
		return
	    }
	    if(this.reloadHeatmap) {
		this.reloadHeatmap = false;
		bounds = RecordUtil.convertBounds(this.map.transformProjBounds(this.map.getMap().getExtent()));
		records = RecordUtil.subset(records, bounds);
	    }
	    bounds = RecordUtil.expandBounds(bounds,0.05);

	    let dfltArgs = this.getDefaultGridByArgs();
	    let w = Math.round(this.getProperty("gridWidth",800));
	    let ratio = (bounds.east-bounds.west)/(bounds.north-bounds.south);
	    let h = Math.round(w/ratio);
	    let groupByField = this.getFieldById(null,this.getProperty("hm.groupBy"));
	    let doTimes = this.getProperty("hm.doTimes",false);
	    if(debug) console.log("\tcalling groupBy");
	    let groups = (groupByField || doTimes)?RecordUtil.groupBy(records, this, this.getProperty("hm.dateBin"), groupByField):null;
	    if(groups == null || groups.max == 0) {
		doTimes = false;
		groups= {
		    max:records.length,
		    values:["none"],
		    map:{none:records}
		}
	    }

	    if(debug) console.log("\tdone calling groupBy");
	    let recordCnt = groups.max;
 	    if(dfltArgs.cellSize==0) {
		let sqrt = Math.sqrt(recordCnt);
		let size = Math.round(w/sqrt);
		dfltArgs.cellSizeX = dfltArgs.cellSizeY = dfltArgs.cellSize = size;
	    } else if(String(dfltArgs.cellSize).endsWith("%")) {
		dfltArgs.cellSize =dfltArgs.cellSizeX =  dfltArgs.cellSizeY = Math.floor(parseFloat(dfltArgs.cellSize.substring(0,dfltArgs.cellSize.length-1))/100*w);
	    }
	    let args =$.extend({colorBy:colorBy,w:w,h:h,bounds:bounds,forMercator:true},
			       dfltArgs);
	    if(debug)
		console.log("dim:" + w +" " +h + " #records:" + records.length +" cell:" + dfltArgs.cellSizeX + " #records:" + records.length);
	    let labels = [];
	    let labelPrefix = this.getProperty("hm.labelPrefix","${field}-");
	    groups.values.every((value,idx)=>{
//		console.log("group:" + value +" #:" + groups.map[value].length);
		let recordsAtTime = groups.map[value];
		let img = RecordUtil.gridData(this.getId(),recordsAtTime,args);
		let label = value=="none"?"Heatmap": labelPrefix +" " +groups.labels[idx];
		label = label.replace("${field}",colorBy.field?colorBy.field.getLabel():"");
		labels.push(label);
		let layer = this.map.addImageLayer("heatmap"+(this.heatmapCnt++), label, "", img, idx==0, bounds.north, bounds.west, bounds.south, bounds.east,w,h, { 
		    isBaseLayer: false,
		});
		layer.heatmapLabel = label;
		if(doTimes) {
		    if(value.getTime)
			layer.date = value;
		}
		this.heatmapLayers.push(layer);
		return true;
	    });
	    let _this = this;
	    if(this.heatmapLayers.length>1 && !this.getProperty("doAnimation")) {
		this.heatmapPlayingAnimation = false;
		let controls = HtmlUtils.div(["id",this.getDomId(ID_HEATMAP_ANIM_PLAY),"style","display:inline-block;","title","Play/Stop Animation"],
					     HtmlUtils.getIconImage("fa-play",["style","    cursor:pointer;"]));
		controls += HtmlUtils.div(["style","display:inline-block;margin-left:5px;margin-right:5px;"], HtmlUtils.select("",["id",this.getDomId(ID_HEATMAP_ANIM_LIST)],labels));
		this.writeHeader(ID_HEADER2_PREPREFIX, controls);
		this.jq(ID_HEATMAP_ANIM_LIST).change(function() {
		    let index = $(this)[0].selectedIndex;
		    _this.applyHeatmapAnimation(index);
		});
		this.jq(ID_HEATMAP_ANIM_PLAY).click(function() {
		    _this.heatmapPlayingAnimation = !_this.heatmapPlayingAnimation;
		    let icon = _this.heatmapPlayingAnimation?"fa-stop":"fa-play";
		    $(this).html(HtmlUtils.getIconImage(icon,["style","    cursor:pointer;"]));
		    if(_this.heatmapPlayingAnimation) {
			_this.stepHeatmapAnimation();
		    }
		});
	    }
	    if(groups.values[0]!="none") {
		this.setMapLabel(labels[0]);
	    }
	    colorBy.displayColorTable(null,true);
	    if(this.getProperty("hm.showToggle",false)) {
		let cbx = this.jq("heatmaptoggle");
		let reload =  HtmlUtils.getIconImage("fa-sync",["style","cursor:pointer;","title","Reload heatmap", "id",this.getDomId("heatmapreload")])+"&nbsp;&nbsp;";
		this.writeHeader(ID_HEADER2_PREFIX, reload + HtmlUtils.checkbox("",["id",this.getDomId("heatmaptoggle")],cbx.length==0 ||cbx.is(':checked')) +"&nbsp;" +
				 this.getProperty("hm.toggleLabel","Toggle Heatmap") +"&nbsp;&nbsp;");
		let _this = this;
		this.jq("heatmapreload").click(()=> {
		    this.haveCalledUpdateUI = false;
		    this.reloadHeatmap = true;
		    this.createHeatmap();
		});
		this.jq("heatmaptoggle").change(function() {
		    if(_this.heatmapLayers)  {
			let visible = $(this).is(':checked');
			_this.heatmapLayers.map(layer=>layer.setVisibility(visible));
		    }
		});
	    }
	},

        addPoints: function(records, fields, points,bounds) {
	    let debug = displayDebug.displayMapAddPoints;
	    if(this.getProperty("doGridPoints",false)|| this.getProperty("doHeatmap",false)) {
		if(debug) console.log("displaymap creating heatmap");
		this.createHeatmap(records, bounds);
		if(debug) console.log("displaymap done creating heatmap");
		if(!this.getProperty("hm.showPoints"))
		    return;
	    }
            let colorBy = this.getColorByInfo(records);
	    let cidx=0
	    let polygonField = this.getFieldById(fields, this.getProperty("polygonField"));
	    let polygonColorTable = this.getColorTable(true, "polygonColorTable",null);
	    let latlon = this.getProperty("latlon",true);
            let source = this;
            let radius = parseFloat(this.getDisplayProp(source, "radius", 8));
	    if(this.getProperty("scaleRadius")) {
		let seen ={};
		let numLocs = 0;
		points.every(p=>{
		    if(!seen[p]) {
			numLocs++;
			seen[p] = true;
		    }
		    return true;
		});

		let radiusScale  = [10000,1,8000,2,5000,3,2000,3,1000,5,500,6,250,8,100,10,50,12];
		radius=radiusScale[1];
		for(let i=0;i<radiusScale.length;i+=2) {
		    if(numLocs<+radiusScale[i]) {
			radius = +radiusScale[i+1];
		    }
		}
	    }
            let strokeWidth = parseFloat(this.getDisplayProp(source, "strokeWidth", "1"));
            let strokeColor = this.getDisplayProp(source, "strokeColor", "#000");
            let sizeByAttr = this.getDisplayProp(source, "sizeBy", null);
            let isTrajectory = this.getDisplayProp(source, "isTrajectory", false);
            if (isTrajectory) {
                let attrs = {
                    strokeWidth: 2,
                    strokeColor: "blue"
                }
                this.map.addPolygon("id", "", points, attrs, null);
                return;
            }

            let latField1 = this.getFieldById(fields, this.getProperty("latField1"));
            let latField2 = this.getFieldById(fields, this.getProperty("latField2"));
            let lonField1 = this.getFieldById(fields, this.getProperty("lonField1"));
            let lonField2 = this.getFieldById(fields, this.getProperty("lonField2"));
            let sizeSegments = this.getProperty("sizeSegments", false);
            let sizeEndPoints = this.getProperty("sizeEndPoints", true);
            let showEndPoints = this.getProperty("showEndPoints", false);
            let endPointSize = parseInt(this.getProperty("endPointSize", "4"));
            let dfltEndPointSize = endPointSize;
            let segmentWidth = parseInt(this.getProperty("segmentWidth", "1"));
            let dfltSegmentWidth = segmentWidth;
            let showPoints = this.getProperty("showPoints", true);
            let lineColor = this.getProperty("lineColor", "green");
	    let pointIcon = this.getProperty("pointIcon");
	    if(pointIcon) this.pointsAreMarkers = true;
            let iconField = this.getFieldById(fields, this.getProperty("iconField"));
            let iconSize = parseFloat(this.getProperty("iconSize",32));
	    let iconMap = this.getIconMap();
	    if(iconField || iconMap)
		this.pointsAreMarkers = true;
	    let dfltShape = this.getProperty("defaultShape",null);
	    let dfltShapes = ["circle","triangle","star",  "square", "cross","x", "lightning","rectangle","church"];
	    let dfltShapeIdx=0;
	    let shapeBy = {
		id: this.getDisplayProp(source, "shapeBy", null),
		field:null,
		map: {}
	    }

	    if(this.getDisplayProp(source, "shapeByMap", null)) {
		this.getDisplayProp(source, "shapeByMap", null).split(",").map((pair)=>{
		    let tuple = pair.split(":");
		    shapeBy.map[tuple[0]] = tuple[1];
		})
	    }


	    //	    console.log("records:" + records.length +" color by range:" + colorBy.minValue + " " + colorBy.maxValue);
            let sizeBy = {
                id: this.getDisplayProp(source, "sizeBy", null),
                minValue: 0,
                maxValue: 0,
                field: null,
                index: -1,
                isString: false,
                stringMap: {},
            };

            let sizeByMap = this.getProperty("sizeByMap");
            if (sizeByMap) {
                let toks = sizeByMap.split(",");
                for (let i = 0; i < toks.length; i++) {
                    let toks2 = toks[i].split(":");
                    if (toks2.length > 1) {
                        sizeBy.stringMap[toks2[0]] = toks2[1];
                    }
                }
            }




            for (let i = 0; i < fields.length; i++) {
                let field = fields[i];
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
                let menu = "<select class='ramadda-pulldown' id='" + this.getDomId("colorByMenu") + "'>";
                for (let i = 0; i < fields.length; i++) {
                    let field = fields[i];
                    if (!field.isNumeric() || field.isFieldGeo()) continue;
                    let extra = "";
                    if (colorBy.field.getId() == field.getId()) extra = "selected ";
                    menu += "<option value='" + field.getId() + "' " + extra + ">" + field.getLabel() + "</option>\n";
                }
                menu += "</select>";
                this.writeHtml(ID_TOP_RIGHT, "Color by: " + menu);
                this.jq("colorByMenu").change(() => {
                    let value = this.jq("colorByMenu").val();
                    this.vectorMapApplied = false;
		    this.haveCalledUpdateUI = false;
                    this.setProperty("colorBy", value);
                    this.updateUI();
                });
            }

            sizeBy.index = sizeBy.field != null ? sizeBy.field.getIndex() : -1;
            shapeBy.index = shapeBy.field != null ? shapeBy.field.getIndex() : -1;

	    let dateMin = null;
	    let dateMax = null;

	    let dates = [];
            let justOneMarker = this.getProperty("justOneMarker",false);


            for (let i = 0; i < records.length; i++) {
                let pointRecord = records[i];
		dates.push(pointRecord.getDate());
                if (dateMin == null) {
                    dateMin = pointRecord.getDate();
                    dateMax = pointRecord.getDate();
                } else {
                    let date = pointRecord.getDate();
                    if (date) {
                        if (date.getTime() < dateMin.getTime())
                            dateMin = date;
                        if (date.getTime() > dateMax.getTime())
                            dateMax = date;
                    }
                }
                let tuple = pointRecord.getData();
                if (!sizeBy.isString) {
                    let v = tuple[sizeBy.index];
		    if (!isNaN(v) && !(v === null)) {
			if (i == 0 || v > sizeBy.maxValue) sizeBy.maxValue = v;
			if (i == 0 || v < sizeBy.minValue) sizeBy.minValue = v;
		    }
		    
                }
	    }


            sizeBy.radiusMin = parseFloat(this.getProperty("sizeByRadiusMin", -1));
            sizeBy.radiusMax = parseFloat(this.getProperty("sizeByRadiusMax", -1));
            let sizeByOffset = 0;
            let sizeByLog = this.getProperty("sizeByLog", false);
            let sizeByFunc = Math.log;
            if (sizeByLog) {
                if (sizeBy.minValue < 1) {
                    sizeByOffset = 1 - sizeBy.minValue;
                }
                sizeBy.minValue = sizeByFunc(sizeBy.minValue + sizeByOffset);
                sizeBy.maxValue = sizeByFunc(sizeBy.maxValue + sizeByOffset);
            }
            sizeBy.range = sizeBy.maxValue - sizeBy.minValue;

	    //	    console.log(JSON.stringify(sizeBy,null,2));
            if (dateMax) {
		if (this.getProperty("doAnimation", false)) {
		    //TODO: figure out when to call this. We want to update the animation if it was from a filter change
		    //but not from an animation change. Hummmm.
		    //this.getAnimation().init(dateMin, dateMax,records);
		}
            }

            if (this.points) {
                for (let i = 0; i < this.points.length; i++) {
		    if(this.pointsAreMarkers)
			this.map.removeMarker(this.points[i]);
		    else
			this.map.removePoint(this.points[i]);
		}
                this.points = [];
            }



            if (this.lines) {
                for (let i = 0; i < this.lines.length; i++)
                    this.map.removePolygon(this.lines[i]);
                this.lines = [];
            }


            if (!this.points) {
                this.points = [];
                this.lines = [];
            }

            let dontAddPoint = this.doDisplayMap();
            let didColorBy = false;
            let seen = {};
	    let lastPoint;
	    let pathAttrs ={
		strokeColor: this.getProperty("pathColor",lineColor),
		strokeWidth: this.getProperty("pathWidth",1)
	    };

	    let fillColor = this.getProperty("fillColor");
	    let fillOpacity =  this.getProperty("fillOpacity",0.8);
	    let isPath = this.getProperty("isPath", false);
	    let showSegments = this.getProperty("showSegments", false);
	    let tooltip = this.getProperty("tooltip");
	    let highlight = this.getProperty("highlight");
	    let highlightTemplate = this.getProperty("highlightTemplate",tooltip);
	    let addedPoints = [];

	    let textGetter = f=>{
		if(f.record) {
                    return  this.getRecordHtml(f.record, fields, tooltip);
		}
		return null;
	    };
	    let highlightGetter = f=>{
		if(f.record) {
                    return  this.getRecordHtml(f.record, fields, highlightTemplate);
		}
		return null;
	    };	    

            for (let i = 0; i < records.length; i++) {
                let record = records[i];
		let recordDate = record.getDate();
                let tuple = record.getData();
		if(!record.point)
                    record.point = new OpenLayers.Geometry.Point(record.getLongitude(), record.getLatitude());
		let point = record.point;

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


                let values = record.getData();
                let props = {
                    pointRadius: radius,
                    strokeWidth: strokeWidth,
                    strokeColor: strokeColor,
		    fillColor: fillColor,
		    fillOpacity: fillOpacity
                };



		if(shapeBy.field) {
		    let gv = values[shapeBy.index];
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
                    let value = values[sizeBy.index];
                    if (sizeBy.isString) {
                        if (Utils.isDefined(sizeBy.stringMap[value])) {
                            let v = parseInt(sizeBy.stringMap[value]);
                            segmentWidth = dfltSegmentWidth + v;
                            props.pointRadius = v;
                        } else if (Utils.isDefined(sizeBy.stringMap["*"])) {
                            let v = parseInt(sizeBy.stringMap["*"]);
                            segmentWidth = dfltSegmentWidth + v;
                            props.pointRadius = v;
                        } else {
                            segmentWidth = dfltSegmentWidth;
                        }
                    } else {
                        let denom = sizeBy.range;
                        let v = value + sizeByOffset;
                        if (sizeByLog) v = sizeByFunc(v);
                        let percent = (denom == 0 ? NaN : (v - sizeBy.minValue) / denom);
                        if (sizeBy.radiusMax >= 0 && sizeBy.radiusMin >= 0) {
                            props.pointRadius = Math.round(sizeBy.radiusMin + percent * (sizeBy.radiusMax - sizeBy.radiusMin));
                        } else {
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
                }


		if(isNaN(props.pointRadius) || props.pointRadius == 0) props.pointRadius= radius;
		let hasColorByValue = false;
		let colorByValue;
		let colorByColor;
		let theColor =  null;
		if(colorBy.compareFields.length>0) {
		    let maxColor = null;
		    let maxValue = 0;
		    colorBy.compareFields.map((f,idx)=>{
			let value = record.getData()[f.getIndex()];
			if(idx==0 || value>maxValue) {
			    maxColor = colorBy.colors[idx];
			    maxValue = value;
			}
		    });
		    colorByValue = maxValue;
		    theColor = maxColor;
		} else if (colorBy.index >= 0) {
                    let value = record.getData()[colorBy.index];
		    colorByValue = value;
		    theColor =  colorBy.getColor(value, record);
                }

		if(theColor) {
                    didColorBy = true;
		    hasColorByValue  = true;
		    colorByColor = props.fillColor = colorBy.convertColor(theColor, colorByValue);
		}


		if(polygonField) {
		    let s = values[polygonField.getIndex()];
		    let delimiter;
		    [";",","].map(d=>{
			if(s.indexOf(d)>=0) delimiter = d;
		    });
		    let toks  = s.split(delimiter);
		    

		    let polygonProps ={};
		    $.extend(polygonProps,props);
		    if(polygonProps.strokeWidth==0)
			polygonProps.strokeWidth=1;
		    if(polygonColorTable) {
			if(cidx>=polygonColorTable.length) cidx=0;
			polygonProps.strokeColor=polygonColorTable[cidx++];
		    }
		    for(let pIdx=2;pIdx<toks.length;pIdx+=2) {
			let p = [];
			let lat1 = parseFloat(toks[pIdx-2]);
			let lon1 = parseFloat(toks[pIdx-1]);
			let lat2 = parseFloat(toks[pIdx]);
			let lon2 = parseFloat(toks[pIdx+1]);
			if(!latlon) {
			    let tmp =lat1;
			    lat1=lon1;
			    lon1=tmp;
			    tmp =lat2;
			    lat2=lon2;
			    lon2=tmp;
			}
			p.push(new OpenLayers.Geometry.Point(lon1,lat1));
			p.push(new OpenLayers.Geometry.Point(lon2,lat2));
			let poly = this.map.addPolygon("polygon" + pIdx, "",p,polygonProps);
			poly.textGetter = textGetter;
			poly.record = record;
			if (recordDate) {
			    poly.date = recordDate.getTime();
			}
			this.lines.push(poly);
		    }
		}


                if (showSegments && latField1 && latField2 && lonField1 && lonField2) {
                    let lat1 = values[latField1.getIndex()];
                    let lat2 = values[latField2.getIndex()];
                    let lon1 = values[lonField1.getIndex()];
                    let lon2 = values[lonField2.getIndex()];
                    let attrs = {};
                    if (props.fillColor)
                        attrs.strokeColor = props.fillColor;
                    else
                        attrs.strokeColor = lineColor;
                    attrs.strokeWidth = segmentWidth;
		    let line = this.map.addLine("line-" + i, "", lat1, lon1, lat2, lon2, attrs);
		    line.record = record;
		    line.textGetter = textGetter;
                    this.lines.push(line);
                    if (showEndPoints) {
                        let pointProps = {};
                        $.extend(pointProps, props);
                        pointProps.fillColor = attrs.strokeColor;
                        pointProps.strokeColor = attrs.strokeColor;
                        pointProps.pointRadius = dfltEndPointSize;
                        pointProps.pointRadius = endPointSize;
                        let p1 = new OpenLayers.LonLat(lon1, lat1);
                        let p2 = new OpenLayers.LonLat(lon2, lat2);
                        if (!Utils.isDefined(seen[p1])) {
                            seen[p1] = true;
			    let pt1 =this.map.addPoint("endpt-" + i, p1, pointProps);
			    pt1.record = record;
			    pt1.textGetter = textGetter;
                            this.points.push(pt1);
                        }
                        if (!Utils.isDefined(seen[p2])) {
                            seen[p2] = true;
                            let pt2 = this.map.addPoint("endpt2-" + i, p2, pointProps);
			    pt2.record = record;
			    pt2.textGetter = textGetter;
                            this.points.push(pt2);
                        }

                    }
		}


                if (showPoints) {
                    //We do this because openlayers gets really slow when there are lots of features at one point
		    let key = String(point);
		    if (!seen[key]) seen[key] = 1;
                    if (seen[key] > 500) {
			continue;
		    }
                    seen[key]++;
		    //		    continue;

		    let mapPoint=null;
		    if(iconField) {
			let icon = tuple[iconField.getIndex()];
			if(iconMap) {
			    icon = iconMap[icon];
			    if(!icon) icon = this.getMarkerIcon();
			}
			let size = iconSize;
			if(sizeBy.index >= 0) {
			    size = props.pointRadius;
			}
			mapPoint = this.map.addMarker("pt-" + i, point, icon, "pt-" + i,null,null,size);
		    } else  if(pointIcon) {
			mapPoint = this.map.addMarker("pt-" + i, point, pointIcon, "pt-" + i,null,null,props.pointRadius);
		    } else {
			if(!props.graphicName)
			    props.graphicName = this.getProperty("shape","circle");
			if(radius>0) {
			    mapPoint = this.map.addPoint("pt-" + i, point, props, null, dontAddPoint);
			}
		    }
		    if(isPath && lastPoint) {
			this.lines.push(this.map.addLine("line-" + i, "", lastPoint.y, lastPoint.x, point.y,point.x,pathAttrs));
		    }
		    lastPoint = point;

                    let date = record.getDate();
		    if(mapPoint) {
			if(highlight)
			    mapPoint.highlightTextGetter = highlightGetter;
			mapPoint.record = record;
			mapPoint.textGetter = textGetter;
			mapPoint.hasColorByValue = hasColorByValue;
		 	mapPoint.colorByValue= colorByValue;
			mapPoint.colorByColor = colorByColor;
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
	    this.jq(ID_BOTTOM).append(HtmlUtils.div(["id",this.getDomId(ID_SHAPES)]));
            if (didColorBy) {
		colorBy.displayColorTable();
            }

	    if(iconField&& iconMap) {
		let html = "";
		for(a in iconMap) {
		    html+=HtmlUtils.image(iconMap[a],["width","32"]) +" " + a+" ";
		}
		this.jq(ID_SHAPES).html("<center>" +html+ "</center>");
	    }

	    if(shapeBy.field) {
		let shapes = shapeBy.field.getLabel()+": ";
		for(v in shapeBy.map) {
		    let shape = shapeBy.map[v];
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
		'defaultMapLayer ="ol.openstreetmap|esri.topo|esri.street|esri.worldimagery|opentopo|usgs.topo|usgs.imagery|usgs.relief|osm.toner|osm.toner.lite|watercolor"',
		"strokeWidth=1",
		"strokeColor=\"#000\"",
		"fillColor=\"\"",
		"radius=\"5\"",
		'scaleRadius=true',
		"shape=\"star|cross|x|square|triangle|circle|lightning|church\"",
		"colorBy=\"\"",
		"colorByLog=\"true\"",
		"colorByMap=\"value1:color1,...,valueN:colorN\"",
		'doGridPoints=true',
		"showClipToBounds=true",
		"showLocationSearch=\"true\"",
		'showLocationReadout=false',
		'showLatLonPosition=false',
		'doPopup=false',
		'linked=true',
		"sizeBy=\"\"",
		"sizeByLog=\"true\"",
		"sizeByMap=\"value1:size,...,valueN:size\"",
		'sizeByRadiusMin="2"',
		'sizeByRadiusMax="20"',
		"boundsAnimation=\"true\"",
		"centerOnFilterChange=\"true\"",
		"centerOnHighlight=\"true\"",
		'recordHighlightRadius=20',
		'recordHighlightStrokeWidth=2',
		'recordHighlightStrokeColor=red',
		'recordHighlightFillColor=rgba(0,0,0,0)',
		"markerIcon=\"/icons/...\"",
		"showSegments=\"true\"",
		'showRecordSelection=false',
		'showMarkersToggle=true',
		'showMarkersToggleLabel="label"',
		'markersVisibility=false',
		'convertColorIntensity=true',
		'intensitySourceMin=0',
		'intensitySourceMax=100',
		'intensityTargetMin=1',
		'intensityTargetMax=0',
		'convertColorAlpha=true',
		'alphaSourceMin=0',
		'alphaSourceMax=100',
		'alphaTargetMin=0',
		'alphaTargetMax=1',
		'inlinelabel:Heatmap Attributes',
		'doHeatmap=true',
		'hm.operator="count|average|min|max"',
		'hm.filter="average5|average9|average25|gauss9|gauss25"',
		'hm.AnimationSleep="1000"',
		'hm.groupBy="field id"',
		'hm.doTimes="true"',
		'hm.dateBin="day|month|year|decade"',
		'hm.labelPrefix=""',
		'hm.showToggle=""',
		'hm.toggleLabel=""',
		'hm.showPoints="true"',
		'hm.filterPasses="1"',
		'hm.filterThreshold="1"',
		'hm.countThreshold="1"',
	    ]);
	},

        addLabels:function(records, fields, points) {
            let labelTemplate = this.getProperty("labelTemplate");
            if(!labelTemplate) return;
	    if(labelTemplate) {
		labelTemplate = labelTemplate.replace(/_nl_/g,"\n");
		if(!this.map.labelLayer) {
		    this.map.labelLayer = new OpenLayers.Layer.Vector("Labels", {
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
			    labelSelect:true,
			}}),
                    });
		    this.map.addVectorLayer(this.map.labelLayer, true);
                    this.map.labelLayer.setZIndex(100);
		}
	    }



	    var features =  [];
            var seen = {};
	    var colorBy = this.getProperty("colorBy");
	    var sizeBy = this.getProperty("sizeBy");
            for (var i = 0; i < records.length; i++) {
		var record = records[i];
                var point = record.point;
                if(seen[point]) continue;
                seen[point] = true;
                var center = new OpenLayers.Geometry.Point(point.x, point.y);
                center.transform(this.map.displayProjection, this.map.sourceProjection);
                var tuple = record.getData();
                var pointFeature = new OpenLayers.Feature.Vector(center);
                pointFeature.noSelect = true;
                pointFeature.attributes = {
                };
                pointFeature.attributes["recordIndex"] = (i+1);
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
		this.map.labelLayer.removeFeatures(this.labelFeatures);
            this.map.labelLayer.addFeatures(features);
	    this.labelFeatures = features;
	    $("#" + this.map.labelLayer.id).css("z-index",1000);
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
            if (!this.map) {
                return;
            }
	    args.highlight = true;
            if (!this.getProperty("showRecordSelection", true)) {
		return;
	    }


	    this.handleEventRecordHighlight(source,args);
	    return;
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




function RamaddaMapgridDisplay(displayManager, id, properties) {
    let SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_MAPGRID, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        needsData: function() {
            return true;
        },
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Grid Map Attributes",
					'localeField=""',
					'grid=countries|us',
					'cellSize="20"',
					'showCellLabel=false',
				    ]);
	},
        handleEventFieldsSelected: function(source, fields) {
	    if(this.getProperty("selectedFieldIsColorBy") && fields.length>0) {
		this.colorByFieldChanged(fields[0]);
	    }
        },
	colorByFieldChanged:function(field) {
	    this.haveCalledUpdateUI = false;
	    this.setProperty("colorBy", field);
	    this.vectorMapApplied  = false;
	    this.updateUI();
	},
	updateUI: function() {
	    var pointData = this.getData();
	    if (pointData == null) return;
	    var records = this.filterData();
	    if(!records) return;

            var fields = this.getData().getNonGeoFields();
	    var localeField = this.getFieldById(fields,this.getProperty("localeField","state"));
	    if(localeField==null) {
		localeField = this.getFieldById(fields,"state");
	    }
	    var minx = Number.MAX_VALUE;
	    var miny = Number.MAX_VALUE;
	    var maxx = Number.MIN_VALUE;
	    var maxy = Number.MIN_VALUE;
	    var map = {};
	    let grid = this.getProperty("grid","us")=="countries"?this.countries:this.states;


	    grid.map(o=>{
		minx = Math.min(minx,o.x);
		maxx = Math.max(maxx,o.x);
		miny = Math.min(miny,o.y);
		maxy = Math.max(maxy,o.y);
		map[this.getDomId("cell_" +o.x+ "_"+o.y)] = o;
	    });

	    var table ="<table border=0 cellspacing=0 cellpadding=0>";
	    var w = this.getProperty("cellSize","40");
	    var showLabel  = this.getProperty("showCellLabel",true);
	    var cellStyle  = this.getProperty("cellStyle","");
	    var cellMap = {};
	    for(var y=1;y<=maxy;y++) {
		table+="<tr>";
		for(var x=1;x<=maxx;x++) {
		    var id = this.getDomId("cell_" +x+ "_"+y);
		    var o = map[id];
		    var extra = " id='" + id +"' ";
		    var style = "position:relative;margin:1px;vertical-align:center;xtext-align:center;width:" + w+"px;" +"height:" + w+"px;";
		    var c = "";
		    if(o) {
			style+="background:#ccc;" + cellStyle;
			extra += " title='" + o.name +"' ";
			extra += " class='display-mapgrid-cell' ";
			c = HtmlUtils.div(["style","padding-left:3px;"], (showLabel?o.codes[0]:""));
			o.codes.map(c=>cellMap[c] = id);
			cellMap[o.name] = id;
		    }
		    var td = "<td>" + "<div " + extra +" style='" + style +"'>" + c+"</div></td>";
		    table+=td;
		}
		table+="</tr>";
	    }
	    table +="<tr><td colspan=" + maxx+"><br>" +   HtmlUtils.div(["id",this.getDomId(ID_COLORTABLE)]) +"</td></tr>";
	    table+="</table>";
            var colorBy = this.getColorByInfo(records);
	    var sparkLinesColorBy = this.getColorByInfo(records,"sparklineColorBy");
	    var strokeColorBy = this.getColorByInfo(records,"strokeColorBy","strokeColorByMap");
	    this.writeHtml(ID_DISPLAY_CONTENTS, table);


	    let sparkLineField = this.getFieldById(fields,this.getProperty("sparklineField"));
	    let states = [];
	    let stateData = {
	    }
	    let minData = 0;
	    let maxData = 0;
	    let seen = {};
	    var contents = this.jq(ID_DISPLAY_CONTENTS);
	    for(var i=0;i<records.length;i++) {
		var record = records[i]; 
		var tuple = record.getData();
		var state = tuple[localeField.getIndex()];
		var cellId = cellMap[state];
		if(!cellId) {
		    cellId = cellMap[state.toUpperCase()];
		}
		if(!cellId) {
		    //		    console.log("Could not find cell:" + state);
		    continue;
		}
		$("#"+cellId).attr("recordIndex",i);

		if(!stateData[state]) {
		    states.push(state);
		    stateData[state] = {
			cellId: cellId,
			data:[],
			records:[]
		    }
		}
		if(sparkLineField) {
		    let value = record.getValue(sparkLineField.getIndex());
		    if(!isNaN(value)) {
			minData = i==0?value:Math.min(minData, value);
			maxData = i==0?value:Math.max(maxData, value);
			stateData[state].data.push(value);
			stateData[state].records.push(record);
		    }
		}
		//TODO: sort the state data on time
                if (colorBy.index >= 0) {
                    var value = record.getData()[colorBy.index];
		    var color = colorBy.getColor(value, record);
		    var cell = contents.find("#" + cellId);
		    cell.css("background",color);
		    cell.attr("recordIndex",i);
                }
		if (strokeColorBy.index >= 0) {
                    var value = record.getData()[strokeColorBy.index];
		    var color = strokeColorBy.getColor(value, record);
		    var cell = contents.find("#" + cellId);
		    cell.css("border-color",color);
		    cell.css("border-width","2px");
                }
	    }

	    if(sparkLineField) {
		states.map((state,idx)=>{
		    let vOffset = 15;
		    let s = stateData[state];
		    let innerId = s.cellId+"_inner";
		    let innerDiv = HtmlUtils.div(["id", innerId, "style","width:" + w +"px;height:" + (w-vOffset) +"px;position:absolute;left:0px;top:" + vOffset+"px;"],"");
		    $("#" + s.cellId).append(innerDiv);
		    this.drawSparkLine("#"+innerId,w,w-vOffset,s.data,s.records,minData,maxData,sparkLinesColorBy);
		});
	    }


	    this.makePopups(contents.find(".display-mapgrid-cell"), records);
	    let _this = this;
	    contents.find(".display-mapgrid-cell").click(function() {
		var record = records[$(this).attr("recordIndex")];
		if(record) {
		    _this.getDisplayManager().notifyEvent("handleEventRecordSelection", _this, {record: record});
		}
	    });
	    this.makeTooltips(contents.find(".display-mapgrid-cell"), records,null,"${default}");
            if (colorBy.index >= 0) {
		colorBy.displayColorTable();
	    }
	    if (sparkLinesColorBy.index >= 0) {
		sparkLinesColorBy.displayColorTable();
	    }



	},

        handleEventRecordSelection: function(source, args) {
	    var contents = this.jq(ID_DISPLAY_CONTENTS);
	    if(this.selectedCell) {
		this.selectedCell.css("border",this.selectedBorder);
	    }
	    var index = this.recordToIndex[args.record.getId()];
	    if(!Utils.isDefined(index)) return;
	    this.selectedCell = contents.find("[recordIndex='" + index+"']");
	    this.selectedBorder = this.selectedCell.css("border");
	    this.selectedCell.css("border","1px solid red");
	},

	states:  [
	    {name:"Alaska",codes:["AK"],x:2,y:1},
	    {name:"Hawaii",codes:["HI"],x:2,y:8},
	    {name:"Washington",codes:["WA"],x:3,y:3},
	    {name:"Oregon",codes:["OR"],x:3,y:4},
	    {name:"California",codes:["CA"],x:3,y:5},
	    {name:"Idaho",codes:["ID"],x:4,y:3},
	    {name:"Nevada",codes:["NV"],x:4,y:4},
	    {name:"Utah",codes:["UT"],x:4,y:5},
	    {name:"Arizona",codes:["AZ"],x:4,y:6},
	    {name:"Montana",codes:["MT"],x:5,y:3},
	    {name:"Wyoming",codes:["WY"],x:5,y:4},
	    {name:"Colorado",codes:["CO"],x:5,y:5},
	    {name:"New Mexico",codes:["NM"],x:5,y:6},
	    {name:"North Dakota",codes:["ND"],x:6,y:3},
	    {name:"South Dakota",codes:["SD"],x:6,y:4},
	    {name:"Nebraska",codes:["NE"],x:6,y:5},
	    {name:"Kansas",codes:["KS"],x:6,y:6},
	    {name:"Oklahoma",codes:["OK"],x:6,y:7},
	    {name:"Texas",codes:["TX"],x:6,y:8},
	    {name:"Minnesota",codes:["MN"],x:7,y:3},
	    {name:"Iowa",codes:["IA"],x:7,y:4},
	    {name:"Missouri",codes:["MO"],x:7,y:5},
	    {name:"Arkansas",codes:["AR"],x:7,y:6},
	    {name:"Louisiana",codes:["LA"],x:7,y:7},
	    {name:"Illinois",codes:["IL"],x:8,y:3},
	    {name:"Indiana",codes:["IN"],x:8,y:4},
	    {name:"Kentucky",codes:["KY"],x:8,y:5},
	    {name:"Tennessee",codes:["TN"],x:8,y:6},
	    {name:"Mississippi",codes:["MS"],x:8,y:7},
	    {name:"Wisconsin",codes:["WI"],x:9,y:2},
	    {name:"Ohio",codes:["OH"],x:9,y:4},
	    {name:"West Virginia",codes:["WV"],x:9,y:5},
	    {name:"North Carolina",codes:["NC"],x:9,y:6},
	    {name:"Alabama",codes:["AL"],x:9,y:7},
	    {name:"Michigan",codes:["MI"],x:9,y:3},
	    {name:"Pennsylvania",codes:["PA"],x:10,y:4},
	    {name:"Virginia",codes:["VA"],x:10,y:5},
	    {name:"South Carolina",codes:["SC"],x:10,y:6},
	    {name:"Georgia",codes:["GA"],x:10,y:7},
	    {name:"New York",codes:["NY"],x:11,y:3},
	    {name:"New Jersey",codes:["NJ"],x:11,y:4},
	    {name:"Maryland",codes:["MD"],x:11,y:5},
	    {name:"DC",codes:["DC"],x:11,y:6},
	    {name:"Florida",codes:["FL"],x:11,y:8},
	    {name:"Vermont",codes:["VT"],x:12,y:2},
	    {name:"Rhode Island",codes:["RI"],x:12,y:3},
	    {name:"Connecticut",codes:["CT"],x:12,y:4},
	    {name:"Delaware",codes:["DE"],x:12,y:5},
	    {name:"Maine",codes:["ME"],x:13,y:1},
	    {name:"New Hampshire",codes:["NH"],x:13,y:2},
	    {name:"Massachusetts",codes:["MA"],x:13,y:3},
	],
	countries:
	[
	    {
		"name": "Afghanistan",
		"codes": [
		    "AF",
		    "AFG",
		    "004",
		    "ISO 3166-2:AF"
		],
		"x": 22,
		"y": 8
	    },
	    {
		"name": "Albania",
		"codes": [
		    "AL",
		    "ALB",
		    "008",
		    "ISO 3166-2:AL"
		],
		"x": 15,
		"y": 9
	    },
	    {
		"name": "Algeria",
		"codes": [
		    "DZ",
		    "DZA",
		    "012",
		    "ISO 3166-2:DZ"
		],
		"x": 13,
		"y": 11
	    },
	    {
		"name": "Angola",
		"codes": [
		    "AO",
		    "AGO",
		    "024",
		    "ISO 3166-2:AO"
		],
		"x": 13,
		"y": 17
	    },
	    {
		"name": "Antarctica",
		"codes": [
		    "AQ",
		    "ATA",
		    "010",
		    "ISO 3166-2:AQ"
		],
		"x": 15,
		"y": 23
	    },
	    {
		"name": "Antigua & Barbuda",
		"codes": [
		    "AG",
		    "ATG",
		    "028",
		    "ISO 3166-2:AG"
		],
		"x": 7,
		"y": 4
	    },
	    {
		"name": "Argentina",
		"codes": [
		    "AR",
		    "ARG",
		    "032",
		    "ISO 3166-2:AR"
		],
		"x": 6,
		"y": 14
	    },
	    {
		"name": "Armenia",
		"codes": [
		    "AM",
		    "ARM",
		    "051",
		    "ISO 3166-2:AM"
		],
		"x": 20,
		"y": 6
	    },
	    {
		"name": "Australia",
		"codes": [
		    "AU",
		    "AUS",
		    "036",
		    "ISO 3166-2:AU"
		],
		"x": 24,
		"y": 19
	    },
	    {
		"name": "Austria",
		"codes": [
		    "AT",
		    "AUT",
		    "040",
		    "ISO 3166-2:AT"
		],
		"x": 15,
		"y": 6
	    },
	    {
		"name": "Azerbaijan",
		"codes": [
		    "AZ",
		    "AZE",
		    "031",
		    "ISO 3166-2:AZ"
		],
		"x": 21,
		"y": 7
	    },
	    {
		"name": "Bahamas",
		"codes": [
		    "BS",
		    "BHS",
		    "044",
		    "ISO 3166-2:BS"
		],
		"x": 4,
		"y": 2
	    },
	    {
		"name": "Bahrain",
		"codes": [
		    "BH",
		    "BHR",
		    "048",
		    "ISO 3166-2:BH"
		],
		"x": 20,
		"y": 9
	    },
	    {
		"name": "Bangladesh",
		"codes": [
		    "BD",
		    "BGD",
		    "050",
		    "ISO 3166-2:BD"
		],
		"x": 23,
		"y": 8
	    },
	    {
		"name": "Barbados",
		"codes": [
		    "BB",
		    "BRB",
		    "052",
		    "ISO 3166-2:BB"
		],
		"x": 8,
		"y": 6
	    },
	    {
		"name": "Belarus",
		"codes": [
		    "BY",
		    "BLR",
		    "112",
		    "ISO 3166-2:BY"
		],
		"x": 17,
		"y": 4
	    },
	    {
		"name": "Belgium",
		"codes": [
		    "BE",
		    "BEL",
		    "056",
		    "ISO 3166-2:BE"
		],
		"x": 13,
		"y": 5
	    },
	    {
		"name": "Belize",
		"codes": [
		    "BZ",
		    "BLZ",
		    "084",
		    "ISO 3166-2:BZ"
		],
		"x": 2,
		"y": 3
	    },
	    {
		"name": "Benin",
		"codes": [
		    "BJ",
		    "BEN",
		    "204",
		    "ISO 3166-2:BJ"
		],
		"x": 15,
		"y": 14
	    },
	    {
		"name": "Bhutan",
		"codes": [
		    "BT",
		    "BTN",
		    "064",
		    "ISO 3166-2:BT"
		],
		"x": 24,
		"y": 7
	    },
	    {
		"name": "Bolivia",
		"codes": [
		    "BO",
		    "BOL",
		    "068",
		    "ISO 3166-2:BO"
		],
		"x": 6,
		"y": 11
	    },
	    {
		"name": "Bosnia & Herzegovina",
		"codes": [
		    "BA",
		    "BIH",
		    "070",
		    "ISO 3166-2:BA"
		],
		"x": 15,
		"y": 7
	    },
	    {
		"name": "Botswana",
		"codes": [
		    "BW",
		    "BWA",
		    "072",
		    "ISO 3166-2:BW"
		],
		"x": 15,
		"y": 18
	    },
	    {
		"name": "Brazil",
		"codes": [
		    "BR",
		    "BRA",
		    "076",
		    "ISO 3166-2:BR"
		],
		"x": 8,
		"y": 11
	    },
	    {
		"name": "Brunei Darussalam",
		"codes": [
		    "BN",
		    "BRN",
		    "096",
		    "ISO 3166-2:BN"
		],
		"x": 25,
		"y": 12
	    },
	    {
		"name": "Bulgaria",
		"codes": [
		    "BG",
		    "BGR",
		    "100",
		    "ISO 3166-2:BG"
		],
		"x": 17,
		"y": 7
	    },
	    {
		"name": "Burkina Faso",
		"codes": [
		    "BF",
		    "BFA",
		    "854",
		    "ISO 3166-2:BF"
		],
		"x": 13,
		"y": 13
	    },
	    {
		"name": "Burundi",
		"codes": [
		    "BI",
		    "BDI",
		    "108",
		    "ISO 3166-2:BI"
		],
		"x": 15,
		"y": 16
	    },
	    {
		"name": "Cambodia",
		"codes": [
		    "KH",
		    "KHM",
		    "116",
		    "ISO 3166-2:KH"
		],
		"x": 25,
		"y": 10
	    },
	    {
		"name": "Cameroon",
		"codes": [
		    "CM",
		    "CMR",
		    "120",
		    "ISO 3166-2:CM"
		],
		"x": 14,
		"y": 15
	    },
	    {
		"name": "Canada",
		"codes": [
		    "CA",
		    "CAN",
		    "124",
		    "ISO 3166-2:CA"
		],
		"x": 1,
		"y": 1
	    },
	    {
		"name": "Cabo Verde",
		"codes": [
		    "CV",
		    "CPV",
		    "132",
		    "ISO 3166-2:CV"
		],
		"x": 10,
		"y": 15
	    },
	    {
		"name": "Central African Republic",
		"codes": [
		    "CF",
		    "CAF",
		    "140",
		    "ISO 3166-2:CF"
		],
		"x": 16,
		"y": 14
	    },
	    {
		"name": "Chad",
		"codes": [
		    "TD",
		    "TCD",
		    "148",
		    "ISO 3166-2:TD"
		],
		"x": 14,
		"y": 13
	    },
	    {
		"name": "Chile",
		"codes": [
		    "CL",
		    "CHL",
		    "152",
		    "ISO 3166-2:CL"
		],
		"x": 6,
		"y": 13
	    },
	    {
		"name": "China",
		"codes": [
		    "CN",
		    "CHN",
		    "156",
		    "ISO 3166-2:CN"
		],
		"x": 24,
		"y": 6
	    },
	    {
		"name": "Colombia",
		"codes": [
		    "CO",
		    "COL",
		    "170",
		    "ISO 3166-2:CO"
		],
		"x": 5,
		"y": 9
	    },
	    {
		"name": "Comoros",
		"codes": [
		    "KM",
		    "COM",
		    "174",
		    "ISO 3166-2:KM"
		],
		"x": 18,
		"y": 18
	    },
	    {
		"name": "Congo",
		"codes": [
		    "CG",
		    "COG",
		    "178",
		    "ISO 3166-2:CG"
		],
		"x": 14,
		"y": 16
	    },
	    {
		"name": "Congo (Democratic Republic of the)",
		"codes": [
		    "CD",
		    "COD",
		    "180",
		    "ISO 3166-2:CD"
		],
		"x": 15,
		"y": 15
	    },
	    {
		"name": "Costa Rica",
		"codes": [
		    "CR",
		    "CRI",
		    "188",
		    "ISO 3166-2:CR"
		],
		"x": 3,
		"y": 7
	    },
	    {
		"name": "Côte d'Ivoire",
		"codes": [
		    "CI",
		    "CIV",
		    "384",
		    "ISO 3166-2:CI"
		],
		"x": 12,
		"y": 15
	    },
	    {
		"name": "Croatia",
		"codes": [
		    "HR",
		    "HRV",
		    "191",
		    "ISO 3166-2:HR"
		],
		"x": 14,
		"y": 7
	    },
	    {
		"name": "Cuba",
		"codes": [
		    "CU",
		    "CUB",
		    "192",
		    "ISO 3166-2:CU"
		],
		"x": 4,
		"y": 3
	    },
	    {
		"name": "Cyprus",
		"codes": [
		    "CY",
		    "CYP",
		    "196",
		    "ISO 3166-2:CY"
		],
		"x": 17,
		"y": 10
	    },
	    {
		"name": "Czech Republic",
		"codes": [
		    "CZ",
		    "CZE",
		    "203",
		    "ISO 3166-2:CZ"
		],
		"x": 15,
		"y": 5
	    },
	    {
		"name": "Denmark",
		"codes": [
		    "DK",
		    "DNK",
		    "208",
		    "ISO 3166-2:DK"
		],
		"x": 14,
		"y": 3
	    },
	    {
		"name": "Djibouti",
		"codes": [
		    "DJ",
		    "DJI",
		    "262",
		    "ISO 3166-2:DJ"
		],
		"x": 17,
		"y": 13
	    },
	    {
		"name": "Dominica",
		"codes": [
		    "DM",
		    "DMA",
		    "212",
		    "ISO 3166-2:DM"
		],
		"x": 7,
		"y": 7
	    },
	    {
		"name": "Dominican Republic",
		"codes": [
		    "DO",
		    "DOM",
		    "214",
		    "ISO 3166-2:DO"
		],
		"x": 6,
		"y": 4
	    },
	    {
		"name": "Ecuador",
		"codes": [
		    "EC",
		    "ECU",
		    "218",
		    "ISO 3166-2:EC"
		],
		"x": 5,
		"y": 10
	    },
	    {
		"name": "Egypt",
		"codes": [
		    "EG",
		    "EGY",
		    "818",
		    "ISO 3166-2:EG"
		],
		"x": 16,
		"y": 11
	    },
	    {
		"name": "El Salvador",
		"codes": [
		    "SV",
		    "SLV",
		    "222",
		    "ISO 3166-2:SV"
		],
		"x": 1,
		"y": 5
	    },
	    {
		"name": "Equatorial Guinea",
		"codes": [
		    "GQ",
		    "GNQ",
		    "226",
		    "ISO 3166-2:GQ"
		],
		"x": 13,
		"y": 16
	    },
	    {
		"name": "Eritrea",
		"codes": [
		    "ER",
		    "ERI",
		    "232",
		    "ISO 3166-2:ER"
		],
		"x": 16,
		"y": 13
	    },
	    {
		"name": "Estonia",
		"codes": [
		    "EE",
		    "EST",
		    "233",
		    "ISO 3166-2:EE"
		],
		"x": 17,
		"y": 2
	    },
	    {
		"name": "Ethiopia",
		"codes": [
		    "ET",
		    "ETH",
		    "231",
		    "ISO 3166-2:ET"
		],
		"x": 17,
		"y": 14
	    },
	    {
		"name": "Fiji",
		"codes": [
		    "FJ",
		    "FJI",
		    "242",
		    "ISO 3166-2:FJ"
		],
		"x": 27,
		"y": 19
	    },
	    {
		"name": "Finland",
		"codes": [
		    "FI",
		    "FIN",
		    "246",
		    "ISO 3166-2:FI"
		],
		"x": 17,
		"y": 1
	    },
	    {
		"name": "France",
		"codes": [
		    "FR",
		    "FRA",
		    "250",
		    "ISO 3166-2:FR"
		],
		"x": 12,
		"y": 5
	    },
	    {
		"name": "Gabon",
		"codes": [
		    "GA",
		    "GAB",
		    "266",
		    "ISO 3166-2:GA"
		],
		"x": 14,
		"y": 17
	    },
	    {
		"name": "Gambia",
		"codes": [
		    "GM",
		    "GMB",
		    "270",
		    "ISO 3166-2:GM"
		],
		"x": 12,
		"y": 12
	    },
	    {
		"name": "Georgia",
		"codes": [
		    "GE",
		    "GEO",
		    "268",
		    "ISO 3166-2:GE"
		],
		"x": 21,
		"y": 6
	    },
	    {
		"name": "Germany",
		"codes": [
		    "DE",
		    "DEU",
		    "276",
		    "ISO 3166-2:DE"
		],
		"x": 14,
		"y": 4
	    },
	    {
		"name": "Ghana",
		"codes": [
		    "GH",
		    "GHA",
		    "288",
		    "ISO 3166-2:GH"
		],
		"x": 13,
		"y": 14
	    },
	    {
		"name": "Greece",
		"codes": [
		    "GR",
		    "GRC",
		    "300",
		    "ISO 3166-2:GR"
		],
		"x": 16,
		"y": 9
	    },
	    {
		"name": "Greenland",
		"codes": [
		    "GL",
		    "GRL",
		    "304",
		    "ISO 3166-2:GL"
		],
		"x": 8,
		"y": 1
	    },
	    {
		"name": "Grenada",
		"codes": [
		    "GD",
		    "GRD",
		    "308",
		    "ISO 3166-2:GD"
		],
		"x": 7,
		"y": 8
	    },
	    {
		"name": "Guatemala",
		"codes": [
		    "GT",
		    "GTM",
		    "320",
		    "ISO 3166-2:GT"
		],
		"x": 1,
		"y": 4
	    },
	    {
		"name": "Guinea",
		"codes": [
		    "GN",
		    "GIN",
		    "324",
		    "ISO 3166-2:GN"
		],
		"x": 11,
		"y": 14
	    },
	    {
		"name": "Guinea-Bissau",
		"codes": [
		    "GW",
		    "GNB",
		    "624",
		    "ISO 3166-2:GW"
		],
		"x": 11,
		"y": 13
	    },
	    {
		"name": "Guyana",
		"codes": [
		    "GY",
		    "GUY",
		    "328",
		    "ISO 3166-2:GY"
		],
		"x": 6,
		"y": 10
	    },
	    {
		"name": "Haiti",
		"codes": [
		    "HT",
		    "HTI",
		    "332",
		    "ISO 3166-2:HT"
		],
		"x": 5,
		"y": 4
	    },
	    {
		"name": "Honduras",
		"codes": [
		    "HN",
		    "HND",
		    "340",
		    "ISO 3166-2:HN"
		],
		"x": 2,
		"y": 5
	    },
	    {
		"name": "Hungary",
		"codes": [
		    "HU",
		    "HUN",
		    "348",
		    "ISO 3166-2:HU"
		],
		"x": 16,
		"y": 6
	    },
	    {
		"name": "Iceland",
		"codes": [
		    "IS",
		    "ISL",
		    "352",
		    "ISO 3166-2:IS"
		],
		"x": 10,
		"y": 1
	    },
	    {
		"name": "India",
		"codes": [
		    "IN",
		    "IND",
		    "356",
		    "ISO 3166-2:IN"
		],
		"x": 22,
		"y": 9
	    },
	    {
		"name": "Indonesia",
		"codes": [
		    "ID",
		    "IDN",
		    "360",
		    "ISO 3166-2:ID"
		],
		"x": 25,
		"y": 13
	    },
	    {
		"name": "Iran (Islamic Republic of)",
		"codes": [
		    "IR",
		    "IRN",
		    "364",
		    "ISO 3166-2:IR"
		],
		"x": 20,
		"y": 8
	    },
	    {
		"name": "Iraq",
		"codes": [
		    "IQ",
		    "IRQ",
		    "368",
		    "ISO 3166-2:IQ"
		],
		"x": 20,
		"y": 7
	    },
	    {
		"name": "Ireland",
		"codes": [
		    "IE",
		    "IRL",
		    "372",
		    "ISO 3166-2:IE"
		],
		"x": 10,
		"y": 4
	    },
	    {
		"name": "Israel",
		"codes": [
		    "IL",
		    "ISR",
		    "376",
		    "ISO 3166-2:IL"
		],
		"x": 18,
		"y": 10
	    },
	    {
		"name": "Italy",
		"codes": [
		    "IT",
		    "ITA",
		    "380",
		    "ISO 3166-2:IT"
		],
		"x": 13,
		"y": 7
	    },
	    {
		"name": "Jamaica",
		"codes": [
		    "JM",
		    "JAM",
		    "388",
		    "ISO 3166-2:JM"
		],
		"x": 4,
		"y": 4
	    },
	    {
		"name": "Japan",
		"codes": [
		    "JP",
		    "JPN",
		    "392",
		    "ISO 3166-2:JP"
		],
		"x": 27,
		"y": 6
	    },
	    {
		"name": "Jordan",
		"codes": [
		    "JO",
		    "JOR",
		    "400",
		    "ISO 3166-2:JO"
		],
		"x": 18,
		"y": 8
	    },
	    {
		"name": "Kazakhstan",
		"codes": [
		    "KZ",
		    "KAZ",
		    "398",
		    "ISO 3166-2:KZ"
		],
		"x": 24,
		"y": 5
	    },
	    {
		"name": "Kenya",
		"codes": [
		    "KE",
		    "KEN",
		    "404",
		    "ISO 3166-2:KE"
		],
		"x": 17,
		"y": 15
	    },
	    {
		"name": "Kiribati",
		"codes": [
		    "KI",
		    "KIR",
		    "296",
		    "ISO 3166-2:KI"
		],
		"x": 27,
		"y": 17
	    },
	    {
		"name": "North Korea",
		"codes": [
		    "KP",
		    "PRK",
		    "408",
		    "ISO 3166-2:KP"
		],
		"x": 25,
		"y": 6
	    },
	    {
		"name": "South Korea",
		"codes": [
		    "KR",
		    "KOR",
		    "410",
		    "ISO 3166-2:KR"
		],
		"x": 25,
		"y": 7
	    },
	    {
		"name": "Kosovo",
		"codes": [
		    "XK",
		    "XKX",
		    "383",
		    "ISO 3166-2:XK"
		],
		"x": 16,
		"y": 8
	    },
	    {
		"name": "Kuwait",
		"codes": [
		    "KW",
		    "KWT",
		    "414",
		    "ISO 3166-2:KW"
		],
		"x": 19,
		"y": 8
	    },
	    {
		"name": "Kyrgyzstan",
		"codes": [
		    "KG",
		    "KGZ",
		    "417",
		    "ISO 3166-2:KG"
		],
		"x": 23,
		"y": 6
	    },
	    {
		"name": "Lao People's Democratic Republic",
		"codes": [
		    "LA",
		    "LAO",
		    "418",
		    "ISO 3166-2:LA"
		],
		"x": 25,
		"y": 9
	    },
	    {
		"name": "Latvia",
		"codes": [
		    "LV",
		    "LVA",
		    "428",
		    "ISO 3166-2:LV"
		],
		"x": 17,
		"y": 3
	    },
	    {
		"name": "Lebanon",
		"codes": [
		    "LB",
		    "LBN",
		    "422",
		    "ISO 3166-2:LB"
		],
		"x": 18,
		"y": 9
	    },
	    {
		"name": "Lesotho",
		"codes": [
		    "LS",
		    "LSO",
		    "426",
		    "ISO 3166-2:LS"
		],
		"x": 17,
		"y": 19
	    },
	    {
		"name": "Liberia",
		"codes": [
		    "LR",
		    "LBR",
		    "430",
		    "ISO 3166-2:LR"
		],
		"x": 12,
		"y": 14
	    },
	    {
		"name": "Libya",
		"codes": [
		    "LY",
		    "LBY",
		    "434",
		    "ISO 3166-2:LY"
		],
		"x": 15,
		"y": 11
	    },
	    {
		"name": "Lithuania",
		"codes": [
		    "LT",
		    "LTU",
		    "440",
		    "ISO 3166-2:LT"
		],
		"x": 16,
		"y": 4
	    },
	    {
		"name": "Luxembourg",
		"codes": [
		    "LU",
		    "LUX",
		    "442",
		    "ISO 3166-2:LU"
		],
		"x": 13,
		"y": 6
	    },
	    {
		"name": "Macedonia",
		"codes": [
		    "MK",
		    "MKD",
		    "807",
		    "ISO 3166-2:MK"
		],
		"x": 17,
		"y": 8
	    },
	    {
		"name": "Madagascar",
		"codes": [
		    "MG",
		    "MDG",
		    "450",
		    "ISO 3166-2:MG"
		],
		"x": 19,
		"y": 19
	    },
	    {
		"name": "Malawi",
		"codes": [
		    "MW",
		    "MWI",
		    "454",
		    "ISO 3166-2:MW"
		],
		"x": 15,
		"y": 17
	    },
	    {
		"name": "Malaysia",
		"codes": [
		    "MY",
		    "MYS",
		    "458",
		    "ISO 3166-2:MY"
		],
		"x": 24,
		"y": 11
	    },
	    {
		"name": "Maldives",
		"codes": [
		    "MV",
		    "MDV",
		    "462",
		    "ISO 3166-2:MV"
		],
		"x": 21,
		"y": 12
	    },
	    {
		"name": "Mali",
		"codes": [
		    "ML",
		    "MLI",
		    "466",
		    "ISO 3166-2:ML"
		],
		"x": 14,
		"y": 12
	    },
	    {
		"name": "Malta",
		"codes": [
		    "MT",
		    "MLT",
		    "470",
		    "ISO 3166-2:MT"
		],
		"x": 11,
		"y": 8
	    },
	    {
		"name": "Marshall Islands",
		"codes": [
		    "MH",
		    "MHL",
		    "584",
		    "ISO 3166-2:MH"
		],
		"x": 26,
		"y": 15
	    },
	    {
		"name": "Mauritania",
		"codes": [
		    "MR",
		    "MRT",
		    "478",
		    "ISO 3166-2:MR"
		],
		"x": 11,
		"y": 12
	    },
	    {
		"name": "Mauritius",
		"codes": [
		    "MU",
		    "MUS",
		    "480",
		    "ISO 3166-2:MU"
		],
		"x": 19,
		"y": 20
	    },
	    {
		"name": "Mexico",
		"codes": [
		    "MX",
		    "MEX",
		    "484",
		    "ISO 3166-2:MX"
		],
		"x": 1,
		"y": 3
	    },
	    {
		"name": "Micronesia (Federated States of)",
		"codes": [
		    "FM",
		    "FSM",
		    "583",
		    "ISO 3166-2:FM"
		],
		"x": 26,
		"y": 16
	    },
	    {
		"name": "Moldova (Republic of)",
		"codes": [
		    "MD",
		    "MDA",
		    "498",
		    "ISO 3166-2:MD"
		],
		"x": 18,
		"y": 5
	    },
	    {
		"name": "Mongolia",
		"codes": [
		    "MN",
		    "MNG",
		    "496",
		    "ISO 3166-2:MN"
		],
		"x": 25,
		"y": 5
	    },
	    {
		"name": "Montenegro",
		"codes": [
		    "ME",
		    "MNE",
		    "499",
		    "ISO 3166-2:ME"
		],
		"x": 15,
		"y": 8
	    },
	    {
		"name": "Morocco",
		"codes": [
		    "MA",
		    "MAR",
		    "504",
		    "ISO 3166-2:MA"
		],
		"x": 12,
		"y": 11
	    },
	    {
		"name": "Mozambique",
		"codes": [
		    "MZ",
		    "MOZ",
		    "508",
		    "ISO 3166-2:MZ"
		],
		"x": 16,
		"y": 17
	    },
	    {
		"name": "Myanmar",
		"codes": [
		    "MM",
		    "MMR",
		    "104",
		    "ISO 3166-2:MM"
		],
		"x": 24,
		"y": 8
	    },
	    {
		"name": "Namibia",
		"codes": [
		    "NA",
		    "NAM",
		    "516",
		    "ISO 3166-2:NA"
		],
		"x": 15,
		"y": 19
	    },
	    {
		"name": "Nauru",
		"codes": [
		    "NR",
		    "NRU",
		    "520",
		    "ISO 3166-2:NR"
		],
		"x": 26,
		"y": 17
	    },
	    {
		"name": "Nepal",
		"codes": [
		    "NP",
		    "NPL",
		    "524",
		    "ISO 3166-2:NP"
		],
		"x": 23,
		"y": 9
	    },
	    {
		"name": "Netherlands",
		"codes": [
		    "NL",
		    "NLD",
		    "528",
		    "ISO 3166-2:NL"
		],
		"x": 13,
		"y": 4
	    },
	    {
		"name": "New Zealand",
		"codes": [
		    "NZ",
		    "NZL",
		    "554",
		    "ISO 3166-2:NZ"
		],
		"x": 26,
		"y": 21
	    },
	    {
		"name": "Nicaragua",
		"codes": [
		    "NI",
		    "NIC",
		    "558",
		    "ISO 3166-2:NI"
		],
		"x": 2,
		"y": 6
	    },
	    {
		"name": "Niger",
		"codes": [
		    "NE",
		    "NER",
		    "562",
		    "ISO 3166-2:NE"
		],
		"x": 15,
		"y": 12
	    },
	    {
		"name": "Nigeria",
		"codes": [
		    "NG",
		    "NGA",
		    "566",
		    "ISO 3166-2:NG"
		],
		"x": 13,
		"y": 15
	    },
	    {
		"name": "Norway",
		"codes": [
		    "NO",
		    "NOR",
		    "578",
		    "ISO 3166-2:NO"
		],
		"x": 15,
		"y": 1
	    },
	    {
		"name": "Oman",
		"codes": [
		    "OM",
		    "OMN",
		    "512",
		    "ISO 3166-2:OM"
		],
		"x": 19,
		"y": 11
	    },
	    {
		"name": "Pakistan",
		"codes": [
		    "PK",
		    "PAK",
		    "586",
		    "ISO 3166-2:PK"
		],
		"x": 21,
		"y": 8
	    },
	    {
		"name": "Palau",
		"codes": [
		    "PW",
		    "PLW",
		    "585",
		    "ISO 3166-2:PW"
		],
		"x": 25,
		"y": 16
	    },
	    {
		"name": "Panama",
		"codes": [
		    "PA",
		    "PAN",
		    "591",
		    "ISO 3166-2:PA"
		],
		"x": 4,
		"y": 8
	    },
	    {
		"name": "Papua New Guinea",
		"codes": [
		    "PG",
		    "PNG",
		    "598",
		    "ISO 3166-2:PG"
		],
		"x": 25,
		"y": 17
	    },
	    {
		"name": "Paraguay",
		"codes": [
		    "PY",
		    "PRY",
		    "600",
		    "ISO 3166-2:PY"
		],
		"x": 6,
		"y": 12
	    },
	    {
		"name": "Peru",
		"codes": [
		    "PE",
		    "PER",
		    "604",
		    "ISO 3166-2:PE"
		],
		"x": 5,
		"y": 11
	    },
	    {
		"name": "Philippines",
		"codes": [
		    "PH",
		    "PHL",
		    "608",
		    "ISO 3166-2:PH"
		],
		"x": 26,
		"y": 11
	    },
	    {
		"name": "Poland",
		"codes": [
		    "PL",
		    "POL",
		    "616",
		    "ISO 3166-2:PL"
		],
		"x": 15,
		"y": 4
	    },
	    {
		"name": "Portugal",
		"codes": [
		    "PT",
		    "PRT",
		    "620",
		    "ISO 3166-2:PT"
		],
		"x": 11,
		"y": 6
	    },
	    {
		"name": "Qatar",
		"codes": [
		    "QA",
		    "QAT",
		    "634",
		    "ISO 3166-2:QA"
		],
		"x": 19,
		"y": 10
	    },
	    {
		"name": "Romania",
		"codes": [
		    "RO",
		    "ROU",
		    "642",
		    "ISO 3166-2:RO"
		],
		"x": 17,
		"y": 6
	    },
	    {
		"name": "Russian Federation",
		"codes": [
		    "RU",
		    "RUS",
		    "643",
		    "ISO 3166-2:RU"
		],
		"x": 25,
		"y": 4
	    },
	    {
		"name": "Rwanda",
		"codes": [
		    "RW",
		    "RWA",
		    "646",
		    "ISO 3166-2:RW"
		],
		"x": 16,
		"y": 16
	    },
	    {
		"name": "St. Kitts & Nevis",
		"codes": [
		    "KN",
		    "KNA",
		    "659",
		    "ISO 3166-2:KN"
		],
		"x": 6,
		"y": 5
	    },
	    {
		"name": "St. Lucia",
		"codes": [
		    "LC",
		    "LCA",
		    "662",
		    "ISO 3166-2:LC"
		],
		"x": 7,
		"y": 5
	    },
	    {
		"name": "St. Vincent & the Grenadines",
		"codes": [
		    "VC",
		    "VCT",
		    "670",
		    "ISO 3166-2:VC"
		],
		"x": 7,
		"y": 6
	    },
	    {
		"name": "Samoa",
		"codes": [
		    "WS",
		    "WSM",
		    "882",
		    "ISO 3166-2:WS"
		],
		"x": 28,
		"y": 18
	    },
	    {
		"name": "Sao Tome and Principe",
		"codes": [
		    "ST",
		    "STP",
		    "678",
		    "ISO 3166-2:ST"
		],
		"x": 11,
		"y": 16
	    },
	    {
		"name": "Saudi Arabia",
		"codes": [
		    "SA",
		    "SAU",
		    "682",
		    "ISO 3166-2:SA"
		],
		"x": 19,
		"y": 9
	    },
	    {
		"name": "Senegal",
		"codes": [
		    "SN",
		    "SEN",
		    "686",
		    "ISO 3166-2:SN"
		],
		"x": 13,
		"y": 12
	    },
	    {
		"name": "Serbia",
		"codes": [
		    "RS",
		    "SRB",
		    "688",
		    "ISO 3166-2:RS"
		],
		"x": 16,
		"y": 7
	    },
	    {
		"name": "Seychelles",
		"codes": [
		    "SC",
		    "SYC",
		    "690",
		    "ISO 3166-2:SC"
		],
		"x": 18,
		"y": 17
	    },
	    {
		"name": "Sierra Leone",
		"codes": [
		    "SL",
		    "SLE",
		    "694",
		    "ISO 3166-2:SL"
		],
		"x": 12,
		"y": 13
	    },
	    {
		"name": "Singapore",
		"codes": [
		    "SG",
		    "SGP",
		    "702",
		    "ISO 3166-2:SG"
		],
		"x": 24,
		"y": 13
	    },
	    {
		"name": "Slovakia",
		"codes": [
		    "SK",
		    "SVK",
		    "703",
		    "ISO 3166-2:SK"
		],
		"x": 16,
		"y": 5
	    },
	    {
		"name": "Slovenia",
		"codes": [
		    "SI",
		    "SVN",
		    "705",
		    "ISO 3166-2:SI"
		],
		"x": 14,
		"y": 6
	    },
	    {
		"name": "Solomon Islands",
		"codes": [
		    "SB",
		    "SLB",
		    "090",
		    "ISO 3166-2:SB"
		],
		"x": 26,
		"y": 18
	    },
	    {
		"name": "Somalia",
		"codes": [
		    "SO",
		    "SOM",
		    "706",
		    "ISO 3166-2:SO"
		],
		"x": 18,
		"y": 14
	    },
	    {
		"name": "South Africa",
		"codes": [
		    "ZA",
		    "ZAF",
		    "710",
		    "ISO 3166-2:ZA"
		],
		"x": 16,
		"y": 20
	    },
	    {
		"name": "South Sudan",
		"codes": [
		    "SS",
		    "SSD",
		    "728",
		    "ISO 3166-2:SS"
		],
		"x": 15,
		"y": 13
	    },
	    {
		"name": "Spain",
		"codes": [
		    "ES",
		    "ESP",
		    "724",
		    "ISO 3166-2:ES"
		],
		"x": 12,
		"y": 6
	    },
	    {
		"name": "Sri Lanka",
		"codes": [
		    "LK",
		    "LKA",
		    "144",
		    "ISO 3166-2:LK"
		],
		"x": 22,
		"y": 11
	    },
	    {
		"name": "Sudan",
		"codes": [
		    "SD",
		    "SDN",
		    "729",
		    "ISO 3166-2:SD"
		],
		"x": 16,
		"y": 12
	    },
	    {
		"name": "Suriname",
		"codes": [
		    "SR",
		    "SUR",
		    "740",
		    "ISO 3166-2:SR"
		],
		"x": 7,
		"y": 11
	    },
	    {
		"name": "Swaziland",
		"codes": [
		    "SZ",
		    "SWZ",
		    "748",
		    "ISO 3166-2:SZ"
		],
		"x": 16,
		"y": 19
	    },
	    {
		"name": "Sweden",
		"codes": [
		    "SE",
		    "SWE",
		    "752",
		    "ISO 3166-2:SE"
		],
		"x": 16,
		"y": 1
	    },
	    {
		"name": "Switzerland",
		"codes": [
		    "CH",
		    "CHE",
		    "756",
		    "ISO 3166-2:CH"
		],
		"x": 14,
		"y": 5
	    },
	    {
		"name": "Syria",
		"codes": [
		    "SY",
		    "SYR",
		    "760",
		    "ISO 3166-2:SY"
		],
		"x": 19,
		"y": 7
	    },
	    {
		"name": "Tajikistan",
		"codes": [
		    "TJ",
		    "TJK",
		    "762",
		    "ISO 3166-2:TJ"
		],
		"x": 23,
		"y": 7
	    },
	    {
		"name": "Tanzania",
		"codes": [
		    "TZ",
		    "TZA",
		    "834",
		    "ISO 3166-2:TZ"
		],
		"x": 17,
		"y": 16
	    },
	    {
		"name": "Thailand",
		"codes": [
		    "TH",
		    "THA",
		    "764",
		    "ISO 3166-2:TH"
		],
		"x": 24,
		"y": 10
	    },
	    {
		"name": "Timor-Leste",
		"codes": [
		    "TL",
		    "TLS",
		    "626",
		    "ISO 3166-2:TL"
		],
		"x": 25,
		"y": 14
	    },
	    {
		"name": "Togo",
		"codes": [
		    "TG",
		    "TGO",
		    "768",
		    "ISO 3166-2:TG"
		],
		"x": 14,
		"y": 14
	    },
	    {
		"name": "Tonga",
		"codes": [
		    "TO",
		    "TON",
		    "776",
		    "ISO 3166-2:TO"
		],
		"x": 28,
		"y": 19
	    },
	    {
		"name": "Trinidad & Tobago",
		"codes": [
		    "TT",
		    "TTO",
		    "780",
		    "ISO 3166-2:TT"
		],
		"x": 7,
		"y": 9
	    },
	    {
		"name": "Tunisia",
		"codes": [
		    "TN",
		    "TUN",
		    "788",
		    "ISO 3166-2:TN"
		],
		"x": 14,
		"y": 11
	    },
	    {
		"name": "Turkey",
		"codes": [
		    "TR",
		    "TUR",
		    "792",
		    "ISO 3166-2:TR"
		],
		"x": 18,
		"y": 7
	    },
	    {
		"name": "Turkmenistan",
		"codes": [
		    "TM",
		    "TKM",
		    "795",
		    "ISO 3166-2:TM"
		],
		"x": 22,
		"y": 7
	    },
	    {
		"name": "Tuvalu",
		"codes": [
		    "TV",
		    "TUV",
		    "798",
		    "ISO 3166-2:TV"
		],
		"x": 27,
		"y": 18
	    },
	    {
		"name": "Uganda",
		"codes": [
		    "UG",
		    "UGA",
		    "800",
		    "ISO 3166-2:UG"
		],
		"x": 16,
		"y": 15
	    },
	    {
		"name": "Ukraine",
		"codes": [
		    "UA",
		    "UKR",
		    "804",
		    "ISO 3166-2:UA"
		],
		"x": 17,
		"y": 5
	    },
	    {
		"name": "United Arab Emirates",
		"codes": [
		    "AE",
		    "ARE",
		    "784",
		    "ISO 3166-2:AE"
		],
		"x": 20,
		"y": 10
	    },
	    {
		"name": "Great Britain and Northern Ireland",
		"codes": [
		    "GB",
		    "GBR",
		    "826",
		    "ISO 3166-2:GB"
		],
		"x": 11,
		"y": 4
	    },
	    {
		"name": "United States of America",
		"codes": [
		    "US",
		    "USA",
		    "840",
		    "ISO 3166-2:US"
		],
		"x": 1,
		"y": 2
	    },
	    {
		"name": "Uruguay",
		"codes": [
		    "UY",
		    "URY",
		    "858",
		    "ISO 3166-2:UY"
		],
		"x": 7,
		"y": 12
	    },
	    {
		"name": "Uzbekistan",
		"codes": [
		    "UZ",
		    "UZB",
		    "860",
		    "ISO 3166-2:UZ"
		],
		"x": 22,
		"y": 6
	    },
	    {
		"name": "Vanuatu",
		"codes": [
		    "VU",
		    "VUT",
		    "548",
		    "ISO 3166-2:VU"
		],
		"x": 26,
		"y": 19
	    },
	    {
		"name": "Venezuela",
		"codes": [
		    "VE",
		    "VEN",
		    "862",
		    "ISO 3166-2:VE"
		],
		"x": 6,
		"y": 9
	    },
	    {
		"name": "Viet Nam",
		"codes": [
		    "VN",
		    "VNM",
		    "704",
		    "ISO 3166-2:VN"
		],
		"x": 26,
		"y": 9
	    },
	    {
		"name": "Yemen",
		"codes": [
		    "YE",
		    "YEM",
		    "887",
		    "ISO 3166-2:YE"
		],
		"x": 18,
		"y": 11
	    },
	    {
		"name": "Zambia",
		"codes": [
		    "ZM",
		    "ZMB",
		    "894",
		    "ISO 3166-2:ZM"
		],
		"x": 14,
		"y": 18
	    },
	    {
		"name": "Zimbabwe",
		"codes": [
		    "ZW",
		    "ZWE",
		    "716",
		    "ISO 3166-2:ZW"
		],
		"x": 16,
		"y": 18
	    }
	]
    })}


