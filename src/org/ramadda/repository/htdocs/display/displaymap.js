/**
   Copyright 2008-2019 Geode Systems LLC
*/

var DISPLAY_MAP = "map";
var DISPLAY_MAPGRID = "mapgrid";

var displayMapMarkers = ["marker.png", "marker-blue.png", "marker-gold.png", "marker-green.png"];
var displayMapCurrentMarker = -1;
var displayMapUrlToVectorListeners = {};
var displayMapMarkerIcons = {};

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
    var ID_LATFIELD = "latfield";
    var ID_LONFIELD = "lonfield";
    var ID_MAP = "map";
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
	    //            html += HtmlUtils.div([ATTR_CLASS, "", ATTR_ID, this.getDomId(ID_BOTTOM)]);

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
	    //	    if(!this.map.doPopup)
	    //		this.map.doSelect = false;
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
			    var debug = value.indexOf("xxxx0803")>=0;
			    if(debug)
				console.log("A:" + value);
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
	    if(this.getProperty("showMarkerToggle")) {
		html += HtmlUtils.checkbox("",["id",this.getDomId("showMarkerToggle")],true) +" " +
		    this.getProperty("showMarkerToggleLabel","Show Markers") +"&nbsp;&nbsp;";
	    }
	    return html;
	},
	xcnt:0,
	initHeader2:function() {
	    let _this = this;
	    this.jq("showMarkerToggle").change(function() {
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
	    if(this.map)
		this.map.setProgress(HtmlUtils.div([ATTR_CLASS, "display-map-message"], "No data available"));
	},
	startProgress: function() {
	    let msg = this.getProperty("loadingMessage","Loading map...");
	    if(this.map)
		this.map.setProgress(HtmlUtils.div([ATTR_CLASS, "display-map-message"], msg));
	},
	clearProgress: function() {
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


	    //Only show the indicators for lots of records
	    let msg = this.getProperty("loadingMessage","Loading map...");
	    if(msg!="")
		this.map.setProgress(HtmlUtils.div([ATTR_CLASS, "display-map-message"], msg));
	    setTimeout(()=>{
		try {
		    this.updateUIInner(pointData, records);
		    if(callback)callback();
		} catch(exc) {
		    console.log(exc)
		    this.map.setProgress(HtmlUtils.div([ATTR_CLASS, "display-map-message"], "" + exc));
		    return;
		}
		this.map.setProgress("");
	    });

	},
	
	updateUIInner: function(pointData, records) {
	    var t1= new Date();
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
            this.addPoints(records,fields,points,pointBounds);
	    var t3= new Date();
	    //	    Utils.displayTimes("time pts=" + points.length,[t1,t2,t3], true);
            this.addLabels(records,fields,points);
            this.applyVectorMap();
	    this.lastUpdateTime = new Date();
	},
	heatmapCnt:0,
	createHeatmap(records, bounds, colorBy) {
	    records = records || this.filterData();
	    bounds = bounds ||  RecordUtil.getBounds(records);
	    colorBy = colorBy || this.getColorByInfo(records);
	    if(this.heatmapLayer)
		this.map.removeLayer(this.heatmapLayer);
	    let w = Math.round(this.getProperty("gridWidth",800));

	    if(this.getProperty("heatmapSetWidthFromData")) {
		let seen = {};
		let seenCnt = 0;
		records.every(r=>{
		    if(!seen[r.getLongitude()]) {seenCnt++;seen[r.getLongitude()] = true;}
		    return true;
		});
		w = seenCnt;
	    }


	    let dfltArgs = this.getDefaultGridByArgs();
	    if(this.reloadHeatmap) {
		this.reloadHeatmap = false;
		bounds = RecordUtil.convertBounds(this.map.transformProjBounds(this.map.getMap().getExtent()));
		records = RecordUtil.subset(records, bounds);
	    }
	    bounds = RecordUtil.expandBounds(bounds,0.1);
	    if(dfltArgs.cellSize==0) {
		let size = 1;
		while(w/(bounds.east-bounds.west)/size>1000)size++;
		dfltArgs.cellSizeX = dfltArgs.cellSizeY = dfltArgs.cellSize = size;
	    } else if(String(dfltArgs.cellSize).endsWith("%")) {
		dfltArgs.cellSize =dfltArgs.cellSizeX =  dfltArgs.cellSizeY = Math.floor(parseFloat(dfltArgs.cellSize.substring(0,dfltArgs.cellSize.length-1))/100*w);
	    }

	    let ratio = (bounds.east-bounds.west)/(bounds.north-bounds.south);
	    //Skew the height so we get round circles?
	    //	    let h = 1.25*Math.round(w/ratio);
	    let h = Math.floor(w/ratio);
//	    console.log("dim:" + w +" " + h);
//	    console.log("dim:" + w +" " +h + " c:" + dfltArgs.cellSize);

	    let args =$.extend({colorBy:colorBy,w:w,h:h,bounds:bounds,forMercator:true},
			       dfltArgs);
	    let img = RecordUtil.gridData(this.getId(),records,args);
	    this.heatmapLayer = this.map.addImageLayer("heatmap"+(this.heatmapCnt++), "Heatmap", "", img, true, bounds.north, bounds.west, bounds.south, bounds.east,w,h, { 
		isBaseLayer: false
	    });
	    colorBy.displayColorTable(null,true);
	    if(this.getProperty("heatmapShowToggle",false)) {
		let cbx = this.jq("heatmaptoggle");
		let reload =  HtmlUtils.getIconImage("fa-sync",["style","cursor:pointer;","title","Reload heatmap", "id",this.getDomId("heatmapreload")])+"&nbsp;&nbsp;";
		this.jq(ID_HEADER2_PREFIX).html(reload + HtmlUtils.checkbox("",["id",this.getDomId("heatmaptoggle")],cbx.length==0 ||cbx.is(':checked')) +"&nbsp;" +
						this.getProperty("heatmapToggleLabel","Toggle Heatmap") +"&nbsp;&nbsp;");
		let _this = this;
		this.jq("heatmapreload").click(()=> {
		    this.haveCalledUpdateUI = false;
		    this.reloadHeatmap = true;
		    this.createHeatmap();
		});
		this.jq("heatmaptoggle").change(function() {
		    _this.heatmapLayer.setVisibility($(this).is(':checked'));
		});
	    }
	},

        addPoints: function(records, fields, points,bounds) {
            let colorBy = this.getColorByInfo(records);
	    if(this.getProperty("doGridPoints",false)|| this.getProperty("doHeatmap",false)) {

		this.createHeatmap(records, bounds, colorBy);
		if(!this.getProperty("heatmapIncludeData"))
		    return;
	    }
	    //if(bounds) {
	    //	    bounds = RecordUtil.convertBounds(this.map.transformProjBounds(this.map.getMap().getExtent()));
	    //	    records = RecordUtil.subset(records, bounds);
	    //}

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
		    this.getAnimation().init(dateMin, dateMax,records);
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
	    //	    this.jq(ID_BOTTOM).html(HtmlUtils.div(["id",this.getDomId(ID_COLORTABLE)])+
	    //				    HtmlUtils.div(["id",this.getDomId(ID_SHAPES)]));
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
		"showLocationSearch=\"true\"",
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
		"sizeBy=\"\"",
		"sizeByLog=\"true\"",
		"sizeByMap=\"value1:size,...,valueN:size\"",
		'sizeByRadiusMin="2"',
		'sizeByRadiusMax="20"',
		"boundsAnimation=\"true\"",
		"centerOnFilterChange=\"true\"",
		"centerOnHighlight=\"true\"",
		'doHeatmap=true',
		'heatmapOperator="average|min|max|count"',
		'recordHighlightRadius=20',
		'recordHighlightStrokeWidth=2',
		'recordHighlightStrokeColor=red',
		'recordHighlightFillColor=rgba(0,0,0,0)',
		"markerIcon=\"/icons/...\"",
		"showSegments=\"true\"",
		'showRecordSelection=false',
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
	    this.handleEventRecordHighlight(source,args);
	    return;
            if (!this.getProperty("showRecordSelection", true)) return;
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
	    var sparkLinesColorBy = this.getColorByInfo(records,"sparkLinesColorBy");
	    var strokeColorBy = this.getColorByInfo(records,"strokeColorBy","strokeColorByMap");
	    this.writeHtml(ID_DISPLAY_CONTENTS, table);


	    let sparkLineField = this.getFieldById(fields,this.getProperty("sparkLineField"));
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
		    console.log("Could not find cell:" + state);
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
	    if(this.getProperty("showTooltips",true)) {
		contents.find(".display-mapgrid-cell").tooltip({
		    content: function() {
			var record = records[$(this).attr("recordIndex")];
			if(record) {
			    return HtmlUtils.div(["style","max-height:400px;overflow-y:auto;"], _this.getRecordHtml(record));
			}
			return null;
		    }
		});
	    }

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




/*
let test = [
  {
    "name": "Afghanistan",
    "alpha-2": "AF",
    "alpha-3": "AFG",
    "country-code": "004",
    "iso_3166-2": "ISO 3166-2:AF",
    "region": "Asia",
    "sub-region": "Southern Asia",
    "region-code": "142",
    "sub-region-code": "034",
    "coordinates": [
      22,
      8
    ]
  },
  {
    "name": "\u00c5land Islands",
    "alpha-2": "AX",
    "alpha-3": "ALA",
    "country-code": "248",
    "iso_3166-2": "ISO 3166-2:AX",
    "sub-region-code": "154",
    "region-code": "150",
    "sub-region": "Northern Europe",
    "region": "Europe"
  },
  {
    "name": "Albania",
    "alpha-2": "AL",
    "alpha-3": "ALB",
    "country-code": "008",
    "iso_3166-2": "ISO 3166-2:AL",
    "region": "Europe",
    "sub-region": "Southern Europe",
    "region-code": "150",
    "sub-region-code": "039",
    "coordinates": [
      15,
      9
    ]
  },
  {
    "name": "Algeria",
    "alpha-2": "DZ",
    "alpha-3": "DZA",
    "country-code": "012",
    "iso_3166-2": "ISO 3166-2:DZ",
    "region": "Africa",
    "sub-region": "Northern Africa",
    "region-code": "002",
    "sub-region-code": "015",
    "coordinates": [
      13,
      11
    ]
  },
  {
    "name": "American Samoa",
    "alpha-2": "AS",
    "alpha-3": "ASM",
    "country-code": "016",
    "iso_3166-2": "ISO 3166-2:AS",
    "region": "Oceania",
    "sub-region": "Polynesia",
    "region-code": "009",
    "sub-region-code": "061"
  },
  {
    "name": "Andorra",
    "alpha-2": "AD",
    "alpha-3": "AND",
    "country-code": "020",
    "iso_3166-2": "ISO 3166-2:AD",
    "region": "Europe",
    "sub-region": "Southern Europe",
    "region-code": "150",
    "sub-region-code": "039"
  },
  {
    "name": "Angola",
    "alpha-2": "AO",
    "alpha-3": "AGO",
    "country-code": "024",
    "iso_3166-2": "ISO 3166-2:AO",
    "region": "Africa",
    "sub-region": "Middle Africa",
    "region-code": "002",
    "sub-region-code": "017",
    "coordinates": [
      13,
      17
    ]
  },
  {
    "name": "Anguilla",
    "alpha-2": "AI",
    "alpha-3": "AIA",
    "country-code": "660",
    "iso_3166-2": "ISO 3166-2:AI",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029"
  },
  {
    "name": "Antarctica",
    "alpha-2": "AQ",
    "alpha-3": "ATA",
    "country-code": "010",
    "iso_3166-2": "ISO 3166-2:AQ",
    "sub-region-code": null,
    "region-code": null,
    "sub-region": null,
    "region": null,
    "coordinates": [
      15,
      23
    ]
  },
  {
    "name": "Antigua & Barbuda",
    "alpha-2": "AG",
    "alpha-3": "ATG",
    "country-code": "028",
    "iso_3166-2": "ISO 3166-2:AG",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029",
    "coordinates": [
      7,
      4
    ]
  },
  {
    "name": "Argentina",
    "alpha-2": "AR",
    "alpha-3": "ARG",
    "country-code": "032",
    "iso_3166-2": "ISO 3166-2:AR",
    "region": "Americas",
    "sub-region": "South America",
    "region-code": "019",
    "sub-region-code": "005",
    "coordinates": [
      6,
      14
    ]
  },
  {
    "name": "Armenia",
    "alpha-2": "AM",
    "alpha-3": "ARM",
    "country-code": "051",
    "iso_3166-2": "ISO 3166-2:AM",
    "region": "Asia",
    "sub-region": "Western Asia",
    "region-code": "142",
    "sub-region-code": "145",
    "coordinates": [
      20,
      6
    ]
  },
  {
    "name": "Aruba",
    "alpha-2": "AW",
    "alpha-3": "ABW",
    "country-code": "533",
    "iso_3166-2": "ISO 3166-2:AW",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029"
  },
  {
    "name": "Australia",
    "alpha-2": "AU",
    "alpha-3": "AUS",
    "country-code": "036",
    "iso_3166-2": "ISO 3166-2:AU",
    "region": "Oceania",
    "sub-region": "Australia and New Zealand",
    "region-code": "009",
    "sub-region-code": "053",
    "coordinates": [
      24,
      19
    ]
  },
  {
    "name": "Austria",
    "alpha-2": "AT",
    "alpha-3": "AUT",
    "country-code": "040",
    "iso_3166-2": "ISO 3166-2:AT",
    "region": "Europe",
    "sub-region": "Western Europe",
    "region-code": "150",
    "sub-region-code": "155",
    "coordinates": [
      15,
      6
    ]
  },
  {
    "name": "Azerbaijan",
    "alpha-2": "AZ",
    "alpha-3": "AZE",
    "country-code": "031",
    "iso_3166-2": "ISO 3166-2:AZ",
    "region": "Asia",
    "sub-region": "Western Asia",
    "region-code": "142",
    "sub-region-code": "145",
    "coordinates": [
      21,
      7
    ]
  },
  {
    "name": "Bahamas",
    "alpha-2": "BS",
    "alpha-3": "BHS",
    "country-code": "044",
    "iso_3166-2": "ISO 3166-2:BS",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029",
    "coordinates": [
      4,
      2
    ]
  },
  {
    "name": "Bahrain",
    "alpha-2": "BH",
    "alpha-3": "BHR",
    "country-code": "048",
    "iso_3166-2": "ISO 3166-2:BH",
    "region": "Asia",
    "sub-region": "Western Asia",
    "region-code": "142",
    "sub-region-code": "145",
    "coordinates": [
      20,
      9
    ]
  },
  {
    "name": "Bangladesh",
    "alpha-2": "BD",
    "alpha-3": "BGD",
    "country-code": "050",
    "iso_3166-2": "ISO 3166-2:BD",
    "region": "Asia",
    "sub-region": "Southern Asia",
    "region-code": "142",
    "sub-region-code": "034",
    "coordinates": [
      23,
      8
    ]
  },
  {
    "name": "Barbados",
    "alpha-2": "BB",
    "alpha-3": "BRB",
    "country-code": "052",
    "iso_3166-2": "ISO 3166-2:BB",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029",
    "coordinates": [
      8,
      6
    ]
  },
  {
    "name": "Belarus",
    "alpha-2": "BY",
    "alpha-3": "BLR",
    "country-code": "112",
    "iso_3166-2": "ISO 3166-2:BY",
    "region": "Europe",
    "sub-region": "Eastern Europe",
    "region-code": "150",
    "sub-region-code": "151",
    "coordinates": [
      17,
      4
    ]
  },
  {
    "name": "Belgium",
    "alpha-2": "BE",
    "alpha-3": "BEL",
    "country-code": "056",
    "iso_3166-2": "ISO 3166-2:BE",
    "region": "Europe",
    "sub-region": "Western Europe",
    "region-code": "150",
    "sub-region-code": "155",
    "coordinates": [
      13,
      5
    ]
  },
  {
    "name": "Belize",
    "alpha-2": "BZ",
    "alpha-3": "BLZ",
    "country-code": "084",
    "iso_3166-2": "ISO 3166-2:BZ",
    "region": "Americas",
    "sub-region": "Central America",
    "region-code": "019",
    "sub-region-code": "013",
    "coordinates": [
      2,
      3
    ]
  },
  {
    "name": "Benin",
    "alpha-2": "BJ",
    "alpha-3": "BEN",
    "country-code": "204",
    "iso_3166-2": "ISO 3166-2:BJ",
    "region": "Africa",
    "sub-region": "Western Africa",
    "region-code": "002",
    "sub-region-code": "011",
    "coordinates": [
      15,
      14
    ]
  },
  {
    "name": "Bermuda",
    "alpha-2": "BM",
    "alpha-3": "BMU",
    "country-code": "060",
    "iso_3166-2": "ISO 3166-2:BM",
    "region": "Americas",
    "sub-region": "Northern America",
    "region-code": "019",
    "sub-region-code": "021"
  },
  {
    "name": "Bhutan",
    "alpha-2": "BT",
    "alpha-3": "BTN",
    "country-code": "064",
    "iso_3166-2": "ISO 3166-2:BT",
    "region": "Asia",
    "sub-region": "Southern Asia",
    "region-code": "142",
    "sub-region-code": "034",
    "coordinates": [
      24,
      7
    ]
  },
  {
    "name": "Bolivia",
    "alpha-2": "BO",
    "alpha-3": "BOL",
    "country-code": "068",
    "iso_3166-2": "ISO 3166-2:BO",
    "region": "Americas",
    "sub-region": "South America",
    "region-code": "019",
    "sub-region-code": "005",
    "coordinates": [
      6,
      11
    ]
  },
  {
    "name": "Bonaire, Sint Eustatius and Saba",
    "alpha-2": "BQ",
    "alpha-3": "BES",
    "country-code": "535",
    "iso_3166-2": "ISO 3166-2:BQ",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029"
  },
  {
    "name": "Bosnia & Herzegovina",
    "alpha-2": "BA",
    "alpha-3": "BIH",
    "country-code": "070",
    "iso_3166-2": "ISO 3166-2:BA",
    "region": "Europe",
    "sub-region": "Southern Europe",
    "region-code": "150",
    "sub-region-code": "039",
    "coordinates": [
      15,
      7
    ]
  },
  {
    "name": "Botswana",
    "alpha-2": "BW",
    "alpha-3": "BWA",
    "country-code": "072",
    "iso_3166-2": "ISO 3166-2:BW",
    "region": "Africa",
    "sub-region": "Southern Africa",
    "region-code": "002",
    "sub-region-code": "018",
    "coordinates": [
      15,
      18
    ]
  },
  {
    "name": "Bouvet Island",
    "alpha-2": "BV",
    "alpha-3": "BVT",
    "country-code": "074",
    "iso_3166-2": "ISO 3166-2:BV",
    "sub-region-code": null,
    "region-code": null,
    "sub-region": null,
    "region": null
  },
  {
    "name": "Brazil",
    "alpha-2": "BR",
    "alpha-3": "BRA",
    "country-code": "076",
    "iso_3166-2": "ISO 3166-2:BR",
    "region": "Americas",
    "sub-region": "South America",
    "region-code": "019",
    "sub-region-code": "005",
    "coordinates": [
      8,
      11
    ]
  },
  {
    "name": "British Indian Ocean Territory",
    "alpha-2": "IO",
    "alpha-3": "IOT",
    "country-code": "086",
    "iso_3166-2": "ISO 3166-2:IO",
    "sub-region-code": null,
    "region-code": null,
    "sub-region": null,
    "region": null
  },
  {
    "name": "Brunei Darussalam",
    "alpha-2": "BN",
    "alpha-3": "BRN",
    "country-code": "096",
    "iso_3166-2": "ISO 3166-2:BN",
    "region": "Asia",
    "sub-region": "South-Eastern Asia",
    "region-code": "142",
    "sub-region-code": "035",
    "coordinates": [
      25,
      12
    ]
  },
  {
    "name": "Bulgaria",
    "alpha-2": "BG",
    "alpha-3": "BGR",
    "country-code": "100",
    "iso_3166-2": "ISO 3166-2:BG",
    "region": "Europe",
    "sub-region": "Eastern Europe",
    "region-code": "150",
    "sub-region-code": "151",
    "coordinates": [
      17,
      7
    ]
  },
  {
    "name": "Burkina Faso",
    "alpha-2": "BF",
    "alpha-3": "BFA",
    "country-code": "854",
    "iso_3166-2": "ISO 3166-2:BF",
    "region": "Africa",
    "sub-region": "Western Africa",
    "region-code": "002",
    "sub-region-code": "011",
    "coordinates": [
      13,
      13
    ]
  },
  {
    "name": "Burundi",
    "alpha-2": "BI",
    "alpha-3": "BDI",
    "country-code": "108",
    "iso_3166-2": "ISO 3166-2:BI",
    "region": "Africa",
    "sub-region": "Eastern Africa",
    "region-code": "002",
    "sub-region-code": "014",
    "coordinates": [
      15,
      16
    ]
  },
  {
    "name": "Cambodia",
    "alpha-2": "KH",
    "alpha-3": "KHM",
    "country-code": "116",
    "iso_3166-2": "ISO 3166-2:KH",
    "region": "Asia",
    "sub-region": "South-Eastern Asia",
    "region-code": "142",
    "sub-region-code": "035",
    "coordinates": [
      25,
      10
    ]
  },
  {
    "name": "Cameroon",
    "alpha-2": "CM",
    "alpha-3": "CMR",
    "country-code": "120",
    "iso_3166-2": "ISO 3166-2:CM",
    "region": "Africa",
    "sub-region": "Middle Africa",
    "region-code": "002",
    "sub-region-code": "017",
    "coordinates": [
      14,
      15
    ]
  },
  {
    "name": "Canada",
    "alpha-2": "CA",
    "alpha-3": "CAN",
    "country-code": "124",
    "iso_3166-2": "ISO 3166-2:CA",
    "region": "Americas",
    "sub-region": "Northern America",
    "region-code": "019",
    "sub-region-code": "021",
    "coordinates": [
      1,
      1
    ]
  },
  {
    "name": "Cabo Verde",
    "alpha-2": "CV",
    "alpha-3": "CPV",
    "country-code": "132",
    "iso_3166-2": "ISO 3166-2:CV",
    "region": "Africa",
    "sub-region": "Western Africa",
    "region-code": "002",
    "sub-region-code": "011",
    "coordinates": [
      10,
      15
    ]
  },
  {
    "name": "Cayman Islands",
    "alpha-2": "KY",
    "alpha-3": "CYM",
    "country-code": "136",
    "iso_3166-2": "ISO 3166-2:KY",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029"
  },
  {
    "name": "Central African Republic",
    "alpha-2": "CF",
    "alpha-3": "CAF",
    "country-code": "140",
    "iso_3166-2": "ISO 3166-2:CF",
    "region": "Africa",
    "sub-region": "Middle Africa",
    "region-code": "002",
    "sub-region-code": "017",
    "coordinates": [
      16,
      14
    ]
  },
  {
    "name": "Chad",
    "alpha-2": "TD",
    "alpha-3": "TCD",
    "country-code": "148",
    "iso_3166-2": "ISO 3166-2:TD",
    "region": "Africa",
    "sub-region": "Middle Africa",
    "region-code": "002",
    "sub-region-code": "017",
    "coordinates": [
      14,
      13
    ]
  },
  {
    "name": "Chile",
    "alpha-2": "CL",
    "alpha-3": "CHL",
    "country-code": "152",
    "iso_3166-2": "ISO 3166-2:CL",
    "region": "Americas",
    "sub-region": "South America",
    "region-code": "019",
    "sub-region-code": "005",
    "coordinates": [
      6,
      13
    ]
  },
  {
    "name": "China",
    "alpha-2": "CN",
    "alpha-3": "CHN",
    "country-code": "156",
    "iso_3166-2": "ISO 3166-2:CN",
    "region": "Asia",
    "sub-region": "Eastern Asia",
    "region-code": "142",
    "sub-region-code": "030",
    "coordinates": [
      24,
      6
    ]
  },
  {
    "name": "Christmas Island",
    "alpha-2": "CX",
    "alpha-3": "CXR",
    "country-code": "162",
    "iso_3166-2": "ISO 3166-2:CX",
    "sub-region-code": null,
    "region-code": null,
    "sub-region": null,
    "region": null
  },
  {
    "name": "Cocos (Keeling) Islands",
    "alpha-2": "CC",
    "alpha-3": "CCK",
    "country-code": "166",
    "iso_3166-2": "ISO 3166-2:CC",
    "sub-region-code": null,
    "region-code": null,
    "sub-region": null,
    "region": null
  },
  {
    "name": "Colombia",
    "alpha-2": "CO",
    "alpha-3": "COL",
    "country-code": "170",
    "iso_3166-2": "ISO 3166-2:CO",
    "region": "Americas",
    "sub-region": "South America",
    "region-code": "019",
    "sub-region-code": "005",
    "coordinates": [
      5,
      9
    ]
  },
  {
    "name": "Comoros",
    "alpha-2": "KM",
    "alpha-3": "COM",
    "country-code": "174",
    "iso_3166-2": "ISO 3166-2:KM",
    "region": "Africa",
    "sub-region": "Eastern Africa",
    "region-code": "002",
    "sub-region-code": "014",
    "coordinates": [
      18,
      18
    ]
  },
  {
    "name": "Congo",
    "alpha-2": "CG",
    "alpha-3": "COG",
    "country-code": "178",
    "iso_3166-2": "ISO 3166-2:CG",
    "region": "Africa",
    "sub-region": "Middle Africa",
    "region-code": "002",
    "sub-region-code": "017",
    "coordinates": [
      14,
      16
    ]
  },
  {
    "name": "Congo (Democratic Republic of the)",
    "alpha-2": "CD",
    "alpha-3": "COD",
    "country-code": "180",
    "iso_3166-2": "ISO 3166-2:CD",
    "region": "Africa",
    "sub-region": "Middle Africa",
    "region-code": "002",
    "sub-region-code": "017",
    "coordinates": [
      15,
      15
    ]
  },
  {
    "name": "Cook Islands",
    "alpha-2": "CK",
    "alpha-3": "COK",
    "country-code": "184",
    "iso_3166-2": "ISO 3166-2:CK",
    "region": "Oceania",
    "sub-region": "Polynesia",
    "region-code": "009",
    "sub-region-code": "061"
  },
  {
    "name": "Costa Rica",
    "alpha-2": "CR",
    "alpha-3": "CRI",
    "country-code": "188",
    "iso_3166-2": "ISO 3166-2:CR",
    "region": "Americas",
    "sub-region": "Central America",
    "region-code": "019",
    "sub-region-code": "013",
    "coordinates": [
      3,
      7
    ]
  },
  {
    "name": "C\u00f4te d'Ivoire",
    "alpha-2": "CI",
    "alpha-3": "CIV",
    "country-code": "384",
    "iso_3166-2": "ISO 3166-2:CI",
    "region": "Africa",
    "sub-region": "Western Africa",
    "region-code": "002",
    "sub-region-code": "011",
    "coordinates": [
      12,
      15
    ]
  },
  {
    "name": "Croatia",
    "alpha-2": "HR",
    "alpha-3": "HRV",
    "country-code": "191",
    "iso_3166-2": "ISO 3166-2:HR",
    "region": "Europe",
    "sub-region": "Southern Europe",
    "region-code": "150",
    "sub-region-code": "039",
    "coordinates": [
      14,
      7
    ]
  },
  {
    "name": "Cuba",
    "alpha-2": "CU",
    "alpha-3": "CUB",
    "country-code": "192",
    "iso_3166-2": "ISO 3166-2:CU",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029",
    "coordinates": [
      4,
      3
    ]
  },
  {
    "name": "Cura\u00e7ao",
    "alpha-2": "CW",
    "alpha-3": "CUW",
    "country-code": "531",
    "iso_3166-2": "ISO 3166-2:CW",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029"
  },
  {
    "name": "Cyprus",
    "alpha-2": "CY",
    "alpha-3": "CYP",
    "country-code": "196",
    "iso_3166-2": "ISO 3166-2:CY",
    "region": "Asia",
    "sub-region": "Western Asia",
    "region-code": "142",
    "sub-region-code": "145",
    "coordinates": [
      17,
      10
    ]
  },
  {
    "name": "Czech Republic",
    "alpha-2": "CZ",
    "alpha-3": "CZE",
    "country-code": "203",
    "iso_3166-2": "ISO 3166-2:CZ",
    "region": "Europe",
    "sub-region": "Eastern Europe",
    "region-code": "150",
    "sub-region-code": "151",
    "coordinates": [
      15,
      5
    ]
  },
  {
    "name": "Denmark",
    "alpha-2": "DK",
    "alpha-3": "DNK",
    "country-code": "208",
    "iso_3166-2": "ISO 3166-2:DK",
    "region": "Europe",
    "sub-region": "Northern Europe",
    "region-code": "150",
    "sub-region-code": "154",
    "coordinates": [
      14,
      3
    ]
  },
  {
    "name": "Djibouti",
    "alpha-2": "DJ",
    "alpha-3": "DJI",
    "country-code": "262",
    "iso_3166-2": "ISO 3166-2:DJ",
    "region": "Africa",
    "sub-region": "Eastern Africa",
    "region-code": "002",
    "sub-region-code": "014",
    "coordinates": [
      17,
      13
    ]
  },
  {
    "name": "Dominica",
    "alpha-2": "DM",
    "alpha-3": "DMA",
    "country-code": "212",
    "iso_3166-2": "ISO 3166-2:DM",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029",
    "coordinates": [
      7,
      7
    ]
  },
  {
    "name": "Dominican Republic",
    "alpha-2": "DO",
    "alpha-3": "DOM",
    "country-code": "214",
    "iso_3166-2": "ISO 3166-2:DO",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029",
    "coordinates": [
      6,
      4
    ]
  },
  {
    "name": "Ecuador",
    "alpha-2": "EC",
    "alpha-3": "ECU",
    "country-code": "218",
    "iso_3166-2": "ISO 3166-2:EC",
    "region": "Americas",
    "sub-region": "South America",
    "region-code": "019",
    "sub-region-code": "005",
    "coordinates": [
      5,
      10
    ]
  },
  {
    "name": "Egypt",
    "alpha-2": "EG",
    "alpha-3": "EGY",
    "country-code": "818",
    "iso_3166-2": "ISO 3166-2:EG",
    "region": "Africa",
    "sub-region": "Northern Africa",
    "region-code": "002",
    "sub-region-code": "015",
    "coordinates": [
      16,
      11
    ]
  },
  {
    "name": "El Salvador",
    "alpha-2": "SV",
    "alpha-3": "SLV",
    "country-code": "222",
    "iso_3166-2": "ISO 3166-2:SV",
    "region": "Americas",
    "sub-region": "Central America",
    "region-code": "019",
    "sub-region-code": "013",
    "coordinates": [
      1,
      5
    ]
  },
  {
    "name": "Equatorial Guinea",
    "alpha-2": "GQ",
    "alpha-3": "GNQ",
    "country-code": "226",
    "iso_3166-2": "ISO 3166-2:GQ",
    "region": "Africa",
    "sub-region": "Middle Africa",
    "region-code": "002",
    "sub-region-code": "017",
    "coordinates": [
      13,
      16
    ]
  },
  {
    "name": "Eritrea",
    "alpha-2": "ER",
    "alpha-3": "ERI",
    "country-code": "232",
    "iso_3166-2": "ISO 3166-2:ER",
    "region": "Africa",
    "sub-region": "Eastern Africa",
    "region-code": "002",
    "sub-region-code": "014",
    "coordinates": [
      16,
      13
    ]
  },
  {
    "name": "Estonia",
    "alpha-2": "EE",
    "alpha-3": "EST",
    "country-code": "233",
    "iso_3166-2": "ISO 3166-2:EE",
    "region": "Europe",
    "sub-region": "Northern Europe",
    "region-code": "150",
    "sub-region-code": "154",
    "coordinates": [
      17,
      2
    ]
  },
  {
    "name": "Ethiopia",
    "alpha-2": "ET",
    "alpha-3": "ETH",
    "country-code": "231",
    "iso_3166-2": "ISO 3166-2:ET",
    "region": "Africa",
    "sub-region": "Eastern Africa",
    "region-code": "002",
    "sub-region-code": "014",
    "coordinates": [
      17,
      14
    ]
  },
  {
    "name": "Falkland Islands (Malvinas)",
    "alpha-2": "FK",
    "alpha-3": "FLK",
    "country-code": "238",
    "iso_3166-2": "ISO 3166-2:FK",
    "region": "Americas",
    "sub-region": "South America",
    "region-code": "019",
    "sub-region-code": "005"
  },
  {
    "name": "Faroe Islands",
    "alpha-2": "FO",
    "alpha-3": "FRO",
    "country-code": "234",
    "iso_3166-2": "ISO 3166-2:FO",
    "region": "Europe",
    "sub-region": "Northern Europe",
    "region-code": "150",
    "sub-region-code": "154"
  },
  {
    "name": "Fiji",
    "alpha-2": "FJ",
    "alpha-3": "FJI",
    "country-code": "242",
    "iso_3166-2": "ISO 3166-2:FJ",
    "region": "Oceania",
    "sub-region": "Melanesia",
    "region-code": "009",
    "sub-region-code": "054",
    "coordinates": [
      27,
      19
    ]
  },
  {
    "name": "Finland",
    "alpha-2": "FI",
    "alpha-3": "FIN",
    "country-code": "246",
    "iso_3166-2": "ISO 3166-2:FI",
    "region": "Europe",
    "sub-region": "Northern Europe",
    "region-code": "150",
    "sub-region-code": "154",
    "coordinates": [
      17,
      1
    ]
  },
  {
    "name": "France",
    "alpha-2": "FR",
    "alpha-3": "FRA",
    "country-code": "250",
    "iso_3166-2": "ISO 3166-2:FR",
    "region": "Europe",
    "sub-region": "Western Europe",
    "region-code": "150",
    "sub-region-code": "155",
    "coordinates": [
      12,
      5
    ]
  },
  {
    "name": "French Guiana",
    "alpha-2": "GF",
    "alpha-3": "GUF",
    "country-code": "254",
    "iso_3166-2": "ISO 3166-2:GF",
    "region": "Americas",
    "sub-region": "South America",
    "region-code": "019",
    "sub-region-code": "005"
  },
  {
    "name": "French Polynesia",
    "alpha-2": "PF",
    "alpha-3": "PYF",
    "country-code": "258",
    "iso_3166-2": "ISO 3166-2:PF",
    "region": "Oceania",
    "sub-region": "Polynesia",
    "region-code": "009",
    "sub-region-code": "061"
  },
  {
    "name": "French Southern Territories",
    "alpha-2": "TF",
    "alpha-3": "ATF",
    "country-code": "260",
    "iso_3166-2": "ISO 3166-2:TF",
    "sub-region-code": null,
    "region-code": null,
    "sub-region": null,
    "region": null
  },
  {
    "name": "Gabon",
    "alpha-2": "GA",
    "alpha-3": "GAB",
    "country-code": "266",
    "iso_3166-2": "ISO 3166-2:GA",
    "region": "Africa",
    "sub-region": "Middle Africa",
    "region-code": "002",
    "sub-region-code": "017",
    "coordinates": [
      14,
      17
    ]
  },
  {
    "name": "Gambia",
    "alpha-2": "GM",
    "alpha-3": "GMB",
    "country-code": "270",
    "iso_3166-2": "ISO 3166-2:GM",
    "region": "Africa",
    "sub-region": "Western Africa",
    "region-code": "002",
    "sub-region-code": "011",
    "coordinates": [
      12,
      12
    ]
  },
  {
    "name": "Georgia",
    "alpha-2": "GE",
    "alpha-3": "GEO",
    "country-code": "268",
    "iso_3166-2": "ISO 3166-2:GE",
    "region": "Asia",
    "sub-region": "Western Asia",
    "region-code": "142",
    "sub-region-code": "145",
    "coordinates": [
      21,
      6
    ]
  },
  {
    "name": "Germany",
    "alpha-2": "DE",
    "alpha-3": "DEU",
    "country-code": "276",
    "iso_3166-2": "ISO 3166-2:DE",
    "region": "Europe",
    "sub-region": "Western Europe",
    "region-code": "150",
    "sub-region-code": "155",
    "coordinates": [
      14,
      4
    ]
  },
  {
    "name": "Ghana",
    "alpha-2": "GH",
    "alpha-3": "GHA",
    "country-code": "288",
    "iso_3166-2": "ISO 3166-2:GH",
    "region": "Africa",
    "sub-region": "Western Africa",
    "region-code": "002",
    "sub-region-code": "011",
    "coordinates": [
      13,
      14
    ]
  },
  {
    "name": "Gibraltar",
    "alpha-2": "GI",
    "alpha-3": "GIB",
    "country-code": "292",
    "iso_3166-2": "ISO 3166-2:GI",
    "region": "Europe",
    "sub-region": "Southern Europe",
    "region-code": "150",
    "sub-region-code": "039"
  },
  {
    "name": "Greece",
    "alpha-2": "GR",
    "alpha-3": "GRC",
    "country-code": "300",
    "iso_3166-2": "ISO 3166-2:GR",
    "region": "Europe",
    "sub-region": "Southern Europe",
    "region-code": "150",
    "sub-region-code": "039",
    "coordinates": [
      16,
      9
    ]
  },
  {
    "name": "Greenland",
    "alpha-2": "GL",
    "alpha-3": "GRL",
    "country-code": "304",
    "iso_3166-2": "ISO 3166-2:GL",
    "region": "Americas",
    "sub-region": "Northern America",
    "region-code": "019",
    "sub-region-code": "021",
    "coordinates": [
      8,
      1
    ]
  },
  {
    "name": "Grenada",
    "alpha-2": "GD",
    "alpha-3": "GRD",
    "country-code": "308",
    "iso_3166-2": "ISO 3166-2:GD",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029",
    "coordinates": [
      7,
      8
    ]
  },
  {
    "name": "Guadeloupe",
    "alpha-2": "GP",
    "alpha-3": "GLP",
    "country-code": "312",
    "iso_3166-2": "ISO 3166-2:GP",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029"
  },
  {
    "name": "Guam",
    "alpha-2": "GU",
    "alpha-3": "GUM",
    "country-code": "316",
    "iso_3166-2": "ISO 3166-2:GU",
    "region": "Oceania",
    "sub-region": "Micronesia",
    "region-code": "009",
    "sub-region-code": "057"
  },
  {
    "name": "Guatemala",
    "alpha-2": "GT",
    "alpha-3": "GTM",
    "country-code": "320",
    "iso_3166-2": "ISO 3166-2:GT",
    "region": "Americas",
    "sub-region": "Central America",
    "region-code": "019",
    "sub-region-code": "013",
    "coordinates": [
      1,
      4
    ]
  },
  {
    "name": "Guernsey",
    "alpha-2": "GG",
    "alpha-3": "GGY",
    "country-code": "831",
    "iso_3166-2": "ISO 3166-2:GG",
    "region": "Europe",
    "sub-region": "Northern Europe",
    "region-code": "150",
    "sub-region-code": "154"
  },
  {
    "name": "Guinea",
    "alpha-2": "GN",
    "alpha-3": "GIN",
    "country-code": "324",
    "iso_3166-2": "ISO 3166-2:GN",
    "region": "Africa",
    "sub-region": "Western Africa",
    "region-code": "002",
    "sub-region-code": "011",
    "coordinates": [
      11,
      14
    ]
  },
  {
    "name": "Guinea-Bissau",
    "alpha-2": "GW",
    "alpha-3": "GNB",
    "country-code": "624",
    "iso_3166-2": "ISO 3166-2:GW",
    "region": "Africa",
    "sub-region": "Western Africa",
    "region-code": "002",
    "sub-region-code": "011",
    "coordinates": [
      11,
      13
    ]
  },
  {
    "name": "Guyana",
    "alpha-2": "GY",
    "alpha-3": "GUY",
    "country-code": "328",
    "iso_3166-2": "ISO 3166-2:GY",
    "region": "Americas",
    "sub-region": "South America",
    "region-code": "019",
    "sub-region-code": "005",
    "coordinates": [
      6,
      10
    ]
  },
  {
    "name": "Haiti",
    "alpha-2": "HT",
    "alpha-3": "HTI",
    "country-code": "332",
    "iso_3166-2": "ISO 3166-2:HT",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029",
    "coordinates": [
      5,
      4
    ]
  },
  {
    "name": "Heard Island and McDonald Islands",
    "alpha-2": "HM",
    "alpha-3": "HMD",
    "country-code": "334",
    "iso_3166-2": "ISO 3166-2:HM",
    "sub-region-code": null,
    "region-code": null,
    "sub-region": null,
    "region": null
  },
  {
    "name": "Holy See",
    "alpha-2": "VA",
    "alpha-3": "VAT",
    "country-code": "336",
    "iso_3166-2": "ISO 3166-2:VA",
    "region": "Europe",
    "sub-region": "Southern Europe",
    "region-code": "150",
    "sub-region-code": "039"
  },
  {
    "name": "Honduras",
    "alpha-2": "HN",
    "alpha-3": "HND",
    "country-code": "340",
    "iso_3166-2": "ISO 3166-2:HN",
    "region": "Americas",
    "sub-region": "Central America",
    "region-code": "019",
    "sub-region-code": "013",
    "coordinates": [
      2,
      5
    ]
  },
  {
    "name": "Hong Kong",
    "alpha-2": "HK",
    "alpha-3": "HKG",
    "country-code": "344",
    "iso_3166-2": "ISO 3166-2:HK",
    "region": "Asia",
    "sub-region": "Eastern Asia",
    "region-code": "142",
    "sub-region-code": "030"
  },
  {
    "name": "Hungary",
    "alpha-2": "HU",
    "alpha-3": "HUN",
    "country-code": "348",
    "iso_3166-2": "ISO 3166-2:HU",
    "region": "Europe",
    "sub-region": "Eastern Europe",
    "region-code": "150",
    "sub-region-code": "151",
    "coordinates": [
      16,
      6
    ]
  },
  {
    "name": "Iceland",
    "alpha-2": "IS",
    "alpha-3": "ISL",
    "country-code": "352",
    "iso_3166-2": "ISO 3166-2:IS",
    "region": "Europe",
    "sub-region": "Northern Europe",
    "region-code": "150",
    "sub-region-code": "154",
    "coordinates": [
      10,
      1
    ]
  },
  {
    "name": "India",
    "alpha-2": "IN",
    "alpha-3": "IND",
    "country-code": "356",
    "iso_3166-2": "ISO 3166-2:IN",
    "region": "Asia",
    "sub-region": "Southern Asia",
    "region-code": "142",
    "sub-region-code": "034",
    "coordinates": [
      22,
      9
    ]
  },
  {
    "name": "Indonesia",
    "alpha-2": "ID",
    "alpha-3": "IDN",
    "country-code": "360",
    "iso_3166-2": "ISO 3166-2:ID",
    "region": "Asia",
    "sub-region": "South-Eastern Asia",
    "region-code": "142",
    "sub-region-code": "035",
    "coordinates": [
      25,
      13
    ]
  },
  {
    "name": "Iran (Islamic Republic of)",
    "alpha-2": "IR",
    "alpha-3": "IRN",
    "country-code": "364",
    "iso_3166-2": "ISO 3166-2:IR",
    "region": "Asia",
    "sub-region": "Southern Asia",
    "region-code": "142",
    "sub-region-code": "034",
    "coordinates": [
      20,
      8
    ]
  },
  {
    "name": "Iraq",
    "alpha-2": "IQ",
    "alpha-3": "IRQ",
    "country-code": "368",
    "iso_3166-2": "ISO 3166-2:IQ",
    "region": "Asia",
    "sub-region": "Western Asia",
    "region-code": "142",
    "sub-region-code": "145",
    "coordinates": [
      20,
      7
    ]
  },
  {
    "name": "Ireland",
    "alpha-2": "IE",
    "alpha-3": "IRL",
    "country-code": "372",
    "iso_3166-2": "ISO 3166-2:IE",
    "region": "Europe",
    "sub-region": "Northern Europe",
    "region-code": "150",
    "sub-region-code": "154",
    "coordinates": [
      10,
      4
    ]
  },
  {
    "name": "Isle of Man",
    "alpha-2": "IM",
    "alpha-3": "IMN",
    "country-code": "833",
    "iso_3166-2": "ISO 3166-2:IM",
    "region": "Europe",
    "sub-region": "Northern Europe",
    "region-code": "150",
    "sub-region-code": "154"
  },
  {
    "name": "Israel",
    "alpha-2": "IL",
    "alpha-3": "ISR",
    "country-code": "376",
    "iso_3166-2": "ISO 3166-2:IL",
    "region": "Asia",
    "sub-region": "Western Asia",
    "region-code": "142",
    "sub-region-code": "145",
    "coordinates": [
      18,
      10
    ]
  },
  {
    "name": "Italy",
    "alpha-2": "IT",
    "alpha-3": "ITA",
    "country-code": "380",
    "iso_3166-2": "ISO 3166-2:IT",
    "region": "Europe",
    "sub-region": "Southern Europe",
    "region-code": "150",
    "sub-region-code": "039",
    "coordinates": [
      13,
      7
    ]
  },
  {
    "name": "Jamaica",
    "alpha-2": "JM",
    "alpha-3": "JAM",
    "country-code": "388",
    "iso_3166-2": "ISO 3166-2:JM",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029",
    "coordinates": [
      4,
      4
    ]
  },
  {
    "name": "Japan",
    "alpha-2": "JP",
    "alpha-3": "JPN",
    "country-code": "392",
    "iso_3166-2": "ISO 3166-2:JP",
    "region": "Asia",
    "sub-region": "Eastern Asia",
    "region-code": "142",
    "sub-region-code": "030",
    "coordinates": [
      27,
      6
    ]
  },
  {
    "name": "Jersey",
    "alpha-2": "JE",
    "alpha-3": "JEY",
    "country-code": "832",
    "iso_3166-2": "ISO 3166-2:JE",
    "region": "Europe",
    "sub-region": "Northern Europe",
    "region-code": "150",
    "sub-region-code": "154"
  },
  {
    "name": "Jordan",
    "alpha-2": "JO",
    "alpha-3": "JOR",
    "country-code": "400",
    "iso_3166-2": "ISO 3166-2:JO",
    "region": "Asia",
    "sub-region": "Western Asia",
    "region-code": "142",
    "sub-region-code": "145",
    "coordinates": [
      18,
      8
    ]
  },
  {
    "name": "Kazakhstan",
    "alpha-2": "KZ",
    "alpha-3": "KAZ",
    "country-code": "398",
    "iso_3166-2": "ISO 3166-2:KZ",
    "region": "Asia",
    "sub-region": "Central Asia",
    "region-code": "142",
    "sub-region-code": "143",
    "coordinates": [
      24,
      5
    ]
  },
  {
    "name": "Kenya",
    "alpha-2": "KE",
    "alpha-3": "KEN",
    "country-code": "404",
    "iso_3166-2": "ISO 3166-2:KE",
    "region": "Africa",
    "sub-region": "Eastern Africa",
    "region-code": "002",
    "sub-region-code": "014",
    "coordinates": [
      17,
      15
    ]
  },
  {
    "name": "Kiribati",
    "alpha-2": "KI",
    "alpha-3": "KIR",
    "country-code": "296",
    "iso_3166-2": "ISO 3166-2:KI",
    "region": "Oceania",
    "sub-region": "Micronesia",
    "region-code": "009",
    "sub-region-code": "057",
    "coordinates": [
      27,
      17
    ]
  },
  {
    "name": "North Korea",
    "alpha-2": "KP",
    "alpha-3": "PRK",
    "country-code": "408",
    "iso_3166-2": "ISO 3166-2:KP",
    "region": "Asia",
    "sub-region": "Eastern Asia",
    "region-code": "142",
    "sub-region-code": "030",
    "coordinates": [
      25,
      6
    ]
  },
  {
    "name": "South Korea",
    "alpha-2": "KR",
    "alpha-3": "KOR",
    "country-code": "410",
    "iso_3166-2": "ISO 3166-2:KR",
    "region": "Asia",
    "sub-region": "Eastern Asia",
    "region-code": "142",
    "sub-region-code": "030",
    "coordinates": [
      25,
      7
    ]
  },
  {
    "name": "Kosovo",
    "alpha-2": "XK",
    "alpha-3": "XKX",
    "country-code": "383",
    "iso_3166-2": "ISO 3166-2:XK",
    "region": "Europe",
    "sub-region": "Southern Europe",
    "region-code": "142",
    "sub-region-code": "030",
    "coordinates": [
      16,
      8
    ]
  },
  {
    "name": "Kuwait",
    "alpha-2": "KW",
    "alpha-3": "KWT",
    "country-code": "414",
    "iso_3166-2": "ISO 3166-2:KW",
    "region": "Asia",
    "sub-region": "Western Asia",
    "region-code": "142",
    "sub-region-code": "145",
    "coordinates": [
      19,
      8
    ]
  },
  {
    "name": "Kyrgyzstan",
    "alpha-2": "KG",
    "alpha-3": "KGZ",
    "country-code": "417",
    "iso_3166-2": "ISO 3166-2:KG",
    "region": "Asia",
    "sub-region": "Central Asia",
    "region-code": "142",
    "sub-region-code": "143",
    "coordinates": [
      23,
      6
    ]
  },
  {
    "name": "Lao People's Democratic Republic",
    "alpha-2": "LA",
    "alpha-3": "LAO",
    "country-code": "418",
    "iso_3166-2": "ISO 3166-2:LA",
    "region": "Asia",
    "sub-region": "South-Eastern Asia",
    "region-code": "142",
    "sub-region-code": "035",
    "coordinates": [
      25,
      9
    ]
  },
  {
    "name": "Latvia",
    "alpha-2": "LV",
    "alpha-3": "LVA",
    "country-code": "428",
    "iso_3166-2": "ISO 3166-2:LV",
    "region": "Europe",
    "sub-region": "Northern Europe",
    "region-code": "150",
    "sub-region-code": "154",
    "coordinates": [
      17,
      3
    ]
  },
  {
    "name": "Lebanon",
    "alpha-2": "LB",
    "alpha-3": "LBN",
    "country-code": "422",
    "iso_3166-2": "ISO 3166-2:LB",
    "region": "Asia",
    "sub-region": "Western Asia",
    "region-code": "142",
    "sub-region-code": "145",
    "coordinates": [
      18,
      9
    ]
  },
  {
    "name": "Lesotho",
    "alpha-2": "LS",
    "alpha-3": "LSO",
    "country-code": "426",
    "iso_3166-2": "ISO 3166-2:LS",
    "region": "Africa",
    "sub-region": "Southern Africa",
    "region-code": "002",
    "sub-region-code": "018",
    "coordinates": [
      17,
      19
    ]
  },
  {
    "name": "Liberia",
    "alpha-2": "LR",
    "alpha-3": "LBR",
    "country-code": "430",
    "iso_3166-2": "ISO 3166-2:LR",
    "region": "Africa",
    "sub-region": "Western Africa",
    "region-code": "002",
    "sub-region-code": "011",
    "coordinates": [
      12,
      14
    ]
  },
  {
    "name": "Libya",
    "alpha-2": "LY",
    "alpha-3": "LBY",
    "country-code": "434",
    "iso_3166-2": "ISO 3166-2:LY",
    "region": "Africa",
    "sub-region": "Northern Africa",
    "region-code": "002",
    "sub-region-code": "015",
    "coordinates": [
      15,
      11
    ]
  },
  {
    "name": "Liechtenstein",
    "alpha-2": "LI",
    "alpha-3": "LIE",
    "country-code": "438",
    "iso_3166-2": "ISO 3166-2:LI",
    "region": "Europe",
    "sub-region": "Western Europe",
    "region-code": "150",
    "sub-region-code": "155"
  },
  {
    "name": "Lithuania",
    "alpha-2": "LT",
    "alpha-3": "LTU",
    "country-code": "440",
    "iso_3166-2": "ISO 3166-2:LT",
    "region": "Europe",
    "sub-region": "Northern Europe",
    "region-code": "150",
    "sub-region-code": "154",
    "coordinates": [
      16,
      4
    ]
  },
  {
    "name": "Luxembourg",
    "alpha-2": "LU",
    "alpha-3": "LUX",
    "country-code": "442",
    "iso_3166-2": "ISO 3166-2:LU",
    "region": "Europe",
    "sub-region": "Western Europe",
    "region-code": "150",
    "sub-region-code": "155",
    "coordinates": [
      13,
      6
    ]
  },
  {
    "name": "Macao",
    "alpha-2": "MO",
    "alpha-3": "MAC",
    "country-code": "446",
    "iso_3166-2": "ISO 3166-2:MO",
    "region": "Asia",
    "sub-region": "Eastern Asia",
    "region-code": "142",
    "sub-region-code": "030"
  },
  {
    "name": "Macedonia",
    "alpha-2": "MK",
    "alpha-3": "MKD",
    "country-code": "807",
    "iso_3166-2": "ISO 3166-2:MK",
    "region": "Europe",
    "sub-region": "Southern Europe",
    "region-code": "150",
    "sub-region-code": "039",
    "coordinates": [
      17,
      8
    ]
  },
  {
    "name": "Madagascar",
    "alpha-2": "MG",
    "alpha-3": "MDG",
    "country-code": "450",
    "iso_3166-2": "ISO 3166-2:MG",
    "region": "Africa",
    "sub-region": "Eastern Africa",
    "region-code": "002",
    "sub-region-code": "014",
    "coordinates": [
      19,
      19
    ]
  },
  {
    "name": "Malawi",
    "alpha-2": "MW",
    "alpha-3": "MWI",
    "country-code": "454",
    "iso_3166-2": "ISO 3166-2:MW",
    "region": "Africa",
    "sub-region": "Eastern Africa",
    "region-code": "002",
    "sub-region-code": "014",
    "coordinates": [
      15,
      17
    ]
  },
  {
    "name": "Malaysia",
    "alpha-2": "MY",
    "alpha-3": "MYS",
    "country-code": "458",
    "iso_3166-2": "ISO 3166-2:MY",
    "region": "Asia",
    "sub-region": "South-Eastern Asia",
    "region-code": "142",
    "sub-region-code": "035",
    "coordinates": [
      24,
      11
    ]
  },
  {
    "name": "Maldives",
    "alpha-2": "MV",
    "alpha-3": "MDV",
    "country-code": "462",
    "iso_3166-2": "ISO 3166-2:MV",
    "region": "Asia",
    "sub-region": "Southern Asia",
    "region-code": "142",
    "sub-region-code": "034",
    "coordinates": [
      21,
      12
    ]
  },
  {
    "name": "Mali",
    "alpha-2": "ML",
    "alpha-3": "MLI",
    "country-code": "466",
    "iso_3166-2": "ISO 3166-2:ML",
    "region": "Africa",
    "sub-region": "Western Africa",
    "region-code": "002",
    "sub-region-code": "011",
    "coordinates": [
      14,
      12
    ]
  },
  {
    "name": "Malta",
    "alpha-2": "MT",
    "alpha-3": "MLT",
    "country-code": "470",
    "iso_3166-2": "ISO 3166-2:MT",
    "region": "Europe",
    "sub-region": "Southern Europe",
    "region-code": "150",
    "sub-region-code": "039",
    "coordinates": [
      11,
      8
    ]
  },
  {
    "name": "Marshall Islands",
    "alpha-2": "MH",
    "alpha-3": "MHL",
    "country-code": "584",
    "iso_3166-2": "ISO 3166-2:MH",
    "region": "Oceania",
    "sub-region": "Micronesia",
    "region-code": "009",
    "sub-region-code": "057",
    "coordinates": [
      26,
      15
    ]
  },
  {
    "name": "Martinique",
    "alpha-2": "MQ",
    "alpha-3": "MTQ",
    "country-code": "474",
    "iso_3166-2": "ISO 3166-2:MQ",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029"
  },
  {
    "name": "Mauritania",
    "alpha-2": "MR",
    "alpha-3": "MRT",
    "country-code": "478",
    "iso_3166-2": "ISO 3166-2:MR",
    "region": "Africa",
    "sub-region": "Western Africa",
    "region-code": "002",
    "sub-region-code": "011",
    "coordinates": [
      11,
      12
    ]
  },
  {
    "name": "Mauritius",
    "alpha-2": "MU",
    "alpha-3": "MUS",
    "country-code": "480",
    "iso_3166-2": "ISO 3166-2:MU",
    "region": "Africa",
    "sub-region": "Eastern Africa",
    "region-code": "002",
    "sub-region-code": "014",
    "coordinates": [
      19,
      20
    ]
  },
  {
    "name": "Mayotte",
    "alpha-2": "YT",
    "alpha-3": "MYT",
    "country-code": "175",
    "iso_3166-2": "ISO 3166-2:YT",
    "region": "Africa",
    "sub-region": "Eastern Africa",
    "region-code": "002",
    "sub-region-code": "014"
  },
  {
    "name": "Mexico",
    "alpha-2": "MX",
    "alpha-3": "MEX",
    "country-code": "484",
    "iso_3166-2": "ISO 3166-2:MX",
    "region": "Americas",
    "sub-region": "Central America",
    "region-code": "019",
    "sub-region-code": "013",
    "coordinates": [
      1,
      3
    ]
  },
  {
    "name": "Micronesia (Federated States of)",
    "alpha-2": "FM",
    "alpha-3": "FSM",
    "country-code": "583",
    "iso_3166-2": "ISO 3166-2:FM",
    "region": "Oceania",
    "sub-region": "Micronesia",
    "region-code": "009",
    "sub-region-code": "057",
    "coordinates": [
      26,
      16
    ]
  },
  {
    "name": "Moldova (Republic of)",
    "alpha-2": "MD",
    "alpha-3": "MDA",
    "country-code": "498",
    "iso_3166-2": "ISO 3166-2:MD",
    "region": "Europe",
    "sub-region": "Eastern Europe",
    "region-code": "150",
    "sub-region-code": "151",
    "coordinates": [
      18,
      5
    ]
  },
  {
    "name": "Monaco",
    "alpha-2": "MC",
    "alpha-3": "MCO",
    "country-code": "492",
    "iso_3166-2": "ISO 3166-2:MC",
    "region": "Europe",
    "sub-region": "Western Europe",
    "region-code": "150",
    "sub-region-code": "155"
  },
  {
    "name": "Mongolia",
    "alpha-2": "MN",
    "alpha-3": "MNG",
    "country-code": "496",
    "iso_3166-2": "ISO 3166-2:MN",
    "region": "Asia",
    "sub-region": "Eastern Asia",
    "region-code": "142",
    "sub-region-code": "030",
    "coordinates": [
      25,
      5
    ]
  },
  {
    "name": "Montenegro",
    "alpha-2": "ME",
    "alpha-3": "MNE",
    "country-code": "499",
    "iso_3166-2": "ISO 3166-2:ME",
    "region": "Europe",
    "sub-region": "Southern Europe",
    "region-code": "150",
    "sub-region-code": "039",
    "coordinates": [
      15,
      8
    ]
  },
  {
    "name": "Montserrat",
    "alpha-2": "MS",
    "alpha-3": "MSR",
    "country-code": "500",
    "iso_3166-2": "ISO 3166-2:MS",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029"
  },
  {
    "name": "Morocco",
    "alpha-2": "MA",
    "alpha-3": "MAR",
    "country-code": "504",
    "iso_3166-2": "ISO 3166-2:MA",
    "region": "Africa",
    "sub-region": "Northern Africa",
    "region-code": "002",
    "sub-region-code": "015",
    "coordinates": [
      12,
      11
    ]
  },
  {
    "name": "Mozambique",
    "alpha-2": "MZ",
    "alpha-3": "MOZ",
    "country-code": "508",
    "iso_3166-2": "ISO 3166-2:MZ",
    "region": "Africa",
    "sub-region": "Eastern Africa",
    "region-code": "002",
    "sub-region-code": "014",
    "coordinates": [
      16,
      17
    ]
  },
  {
    "name": "Myanmar",
    "alpha-2": "MM",
    "alpha-3": "MMR",
    "country-code": "104",
    "iso_3166-2": "ISO 3166-2:MM",
    "region": "Asia",
    "sub-region": "South-Eastern Asia",
    "region-code": "142",
    "sub-region-code": "035",
    "coordinates": [
      24,
      8
    ]
  },
  {
    "name": "Namibia",
    "alpha-2": "NA",
    "alpha-3": "NAM",
    "country-code": "516",
    "iso_3166-2": "ISO 3166-2:NA",
    "region": "Africa",
    "sub-region": "Southern Africa",
    "region-code": "002",
    "sub-region-code": "018",
    "coordinates": [
      15,
      19
    ]
  },
  {
    "name": "Nauru",
    "alpha-2": "NR",
    "alpha-3": "NRU",
    "country-code": "520",
    "iso_3166-2": "ISO 3166-2:NR",
    "region": "Oceania",
    "sub-region": "Micronesia",
    "region-code": "009",
    "sub-region-code": "057",
    "coordinates": [
      26,
      17
    ]
  },
  {
    "name": "Nepal",
    "alpha-2": "NP",
    "alpha-3": "NPL",
    "country-code": "524",
    "iso_3166-2": "ISO 3166-2:NP",
    "region": "Asia",
    "sub-region": "Southern Asia",
    "region-code": "142",
    "sub-region-code": "034",
    "coordinates": [
      23,
      9
    ]
  },
  {
    "name": "Netherlands",
    "alpha-2": "NL",
    "alpha-3": "NLD",
    "country-code": "528",
    "iso_3166-2": "ISO 3166-2:NL",
    "region": "Europe",
    "sub-region": "Western Europe",
    "region-code": "150",
    "sub-region-code": "155",
    "coordinates": [
      13,
      4
    ]
  },
  {
    "name": "New Caledonia",
    "alpha-2": "NC",
    "alpha-3": "NCL",
    "country-code": "540",
    "iso_3166-2": "ISO 3166-2:NC",
    "region": "Oceania",
    "sub-region": "Melanesia",
    "region-code": "009",
    "sub-region-code": "054"
  },
  {
    "name": "New Zealand",
    "alpha-2": "NZ",
    "alpha-3": "NZL",
    "country-code": "554",
    "iso_3166-2": "ISO 3166-2:NZ",
    "region": "Oceania",
    "sub-region": "Australia and New Zealand",
    "region-code": "009",
    "sub-region-code": "053",
    "coordinates": [
      26,
      21
    ]
  },
  {
    "name": "Nicaragua",
    "alpha-2": "NI",
    "alpha-3": "NIC",
    "country-code": "558",
    "iso_3166-2": "ISO 3166-2:NI",
    "region": "Americas",
    "sub-region": "Central America",
    "region-code": "019",
    "sub-region-code": "013",
    "coordinates": [
      2,
      6
    ]
  },
  {
    "name": "Niger",
    "alpha-2": "NE",
    "alpha-3": "NER",
    "country-code": "562",
    "iso_3166-2": "ISO 3166-2:NE",
    "region": "Africa",
    "sub-region": "Western Africa",
    "region-code": "002",
    "sub-region-code": "011",
    "coordinates": [
      15,
      12
    ]
  },
  {
    "name": "Nigeria",
    "alpha-2": "NG",
    "alpha-3": "NGA",
    "country-code": "566",
    "iso_3166-2": "ISO 3166-2:NG",
    "region": "Africa",
    "sub-region": "Western Africa",
    "region-code": "002",
    "sub-region-code": "011",
    "coordinates": [
      13,
      15
    ]
  },
  {
    "name": "Niue",
    "alpha-2": "NU",
    "alpha-3": "NIU",
    "country-code": "570",
    "iso_3166-2": "ISO 3166-2:NU",
    "region": "Oceania",
    "sub-region": "Polynesia",
    "region-code": "009",
    "sub-region-code": "061"
  },
  {
    "name": "Norfolk Island",
    "alpha-2": "NF",
    "alpha-3": "NFK",
    "country-code": "574",
    "iso_3166-2": "ISO 3166-2:NF",
    "region": "Oceania",
    "sub-region": "Australia and New Zealand",
    "region-code": "009",
    "sub-region-code": "053"
  },
  {
    "name": "Northern Mariana Islands",
    "alpha-2": "MP",
    "alpha-3": "MNP",
    "country-code": "580",
    "iso_3166-2": "ISO 3166-2:MP",
    "region": "Oceania",
    "sub-region": "Micronesia",
    "region-code": "009",
    "sub-region-code": "057"
  },
  {
    "name": "Norway",
    "alpha-2": "NO",
    "alpha-3": "NOR",
    "country-code": "578",
    "iso_3166-2": "ISO 3166-2:NO",
    "region": "Europe",
    "sub-region": "Northern Europe",
    "region-code": "150",
    "sub-region-code": "154",
    "coordinates": [
      15,
      1
    ]
  },
  {
    "name": "Oman",
    "alpha-2": "OM",
    "alpha-3": "OMN",
    "country-code": "512",
    "iso_3166-2": "ISO 3166-2:OM",
    "region": "Asia",
    "sub-region": "Western Asia",
    "region-code": "142",
    "sub-region-code": "145",
    "coordinates": [
      19,
      11
    ]
  },
  {
    "name": "Pakistan",
    "alpha-2": "PK",
    "alpha-3": "PAK",
    "country-code": "586",
    "iso_3166-2": "ISO 3166-2:PK",
    "region": "Asia",
    "sub-region": "Southern Asia",
    "region-code": "142",
    "sub-region-code": "034",
    "coordinates": [
      21,
      8
    ]
  },
  {
    "name": "Palau",
    "alpha-2": "PW",
    "alpha-3": "PLW",
    "country-code": "585",
    "iso_3166-2": "ISO 3166-2:PW",
    "region": "Oceania",
    "sub-region": "Micronesia",
    "region-code": "009",
    "sub-region-code": "057",
    "coordinates": [
      25,
      16
    ]
  },
  {
    "name": "Palestine, State of",
    "alpha-2": "PS",
    "alpha-3": "PSE",
    "country-code": "275",
    "iso_3166-2": "ISO 3166-2:PS",
    "region": "Asia",
    "sub-region": "Western Asia",
    "region-code": "142",
    "sub-region-code": "145"
  },
  {
    "name": "Panama",
    "alpha-2": "PA",
    "alpha-3": "PAN",
    "country-code": "591",
    "iso_3166-2": "ISO 3166-2:PA",
    "region": "Americas",
    "sub-region": "Central America",
    "region-code": "019",
    "sub-region-code": "013",
    "coordinates": [
      4,
      8
    ]
  },
  {
    "name": "Papua New Guinea",
    "alpha-2": "PG",
    "alpha-3": "PNG",
    "country-code": "598",
    "iso_3166-2": "ISO 3166-2:PG",
    "region": "Oceania",
    "sub-region": "Melanesia",
    "region-code": "009",
    "sub-region-code": "054",
    "coordinates": [
      25,
      17
    ]
  },
  {
    "name": "Paraguay",
    "alpha-2": "PY",
    "alpha-3": "PRY",
    "country-code": "600",
    "iso_3166-2": "ISO 3166-2:PY",
    "region": "Americas",
    "sub-region": "South America",
    "region-code": "019",
    "sub-region-code": "005",
    "coordinates": [
      6,
      12
    ]
  },
  {
    "name": "Peru",
    "alpha-2": "PE",
    "alpha-3": "PER",
    "country-code": "604",
    "iso_3166-2": "ISO 3166-2:PE",
    "region": "Americas",
    "sub-region": "South America",
    "region-code": "019",
    "sub-region-code": "005",
    "coordinates": [
      5,
      11
    ]
  },
  {
    "name": "Philippines",
    "alpha-2": "PH",
    "alpha-3": "PHL",
    "country-code": "608",
    "iso_3166-2": "ISO 3166-2:PH",
    "region": "Asia",
    "sub-region": "South-Eastern Asia",
    "region-code": "142",
    "sub-region-code": "035",
    "coordinates": [
      26,
      11
    ]
  },
  {
    "name": "Pitcairn",
    "alpha-2": "PN",
    "alpha-3": "PCN",
    "country-code": "612",
    "iso_3166-2": "ISO 3166-2:PN",
    "region": "Oceania",
    "sub-region": "Polynesia",
    "region-code": "009",
    "sub-region-code": "061"
  },
  {
    "name": "Poland",
    "alpha-2": "PL",
    "alpha-3": "POL",
    "country-code": "616",
    "iso_3166-2": "ISO 3166-2:PL",
    "region": "Europe",
    "sub-region": "Eastern Europe",
    "region-code": "150",
    "sub-region-code": "151",
    "coordinates": [
      15,
      4
    ]
  },
  {
    "name": "Portugal",
    "alpha-2": "PT",
    "alpha-3": "PRT",
    "country-code": "620",
    "iso_3166-2": "ISO 3166-2:PT",
    "region": "Europe",
    "sub-region": "Southern Europe",
    "region-code": "150",
    "sub-region-code": "039",
    "coordinates": [
      11,
      6
    ]
  },
  {
    "name": "Puerto Rico",
    "alpha-2": "PR",
    "alpha-3": "PRI",
    "country-code": "630",
    "iso_3166-2": "ISO 3166-2:PR",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029"
  },
  {
    "name": "Qatar",
    "alpha-2": "QA",
    "alpha-3": "QAT",
    "country-code": "634",
    "iso_3166-2": "ISO 3166-2:QA",
    "region": "Asia",
    "sub-region": "Western Asia",
    "region-code": "142",
    "sub-region-code": "145",
    "coordinates": [
      19,
      10
    ]
  },
  {
    "name": "R\u00e9union",
    "alpha-2": "RE",
    "alpha-3": "REU",
    "country-code": "638",
    "iso_3166-2": "ISO 3166-2:RE",
    "region": "Africa",
    "sub-region": "Eastern Africa",
    "region-code": "002",
    "sub-region-code": "014"
  },
  {
    "name": "Romania",
    "alpha-2": "RO",
    "alpha-3": "ROU",
    "country-code": "642",
    "iso_3166-2": "ISO 3166-2:RO",
    "region": "Europe",
    "sub-region": "Eastern Europe",
    "region-code": "150",
    "sub-region-code": "151",
    "coordinates": [
      17,
      6
    ]
  },
  {
    "name": "Russian Federation",
    "alpha-2": "RU",
    "alpha-3": "RUS",
    "country-code": "643",
    "iso_3166-2": "ISO 3166-2:RU",
    "region": "Europe",
    "sub-region": "Eastern Europe",
    "region-code": "150",
    "sub-region-code": "151",
    "coordinates": [
      25,
      4
    ]
  },
  {
    "name": "Rwanda",
    "alpha-2": "RW",
    "alpha-3": "RWA",
    "country-code": "646",
    "iso_3166-2": "ISO 3166-2:RW",
    "region": "Africa",
    "sub-region": "Eastern Africa",
    "region-code": "002",
    "sub-region-code": "014",
    "coordinates": [
      16,
      16
    ]
  },
  {
    "name": "Saint Barth\u00e9lemy",
    "alpha-2": "BL",
    "alpha-3": "BLM",
    "country-code": "652",
    "iso_3166-2": "ISO 3166-2:BL",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029"
  },
  {
    "name": "Saint Helena, Ascension and Tristan da Cunha",
    "alpha-2": "SH",
    "alpha-3": "SHN",
    "country-code": "654",
    "iso_3166-2": "ISO 3166-2:SH",
    "region": "Africa",
    "sub-region": "Western Africa",
    "region-code": "002",
    "sub-region-code": "011"
  },
  {
    "name": "St. Kitts & Nevis",
    "alpha-2": "KN",
    "alpha-3": "KNA",
    "country-code": "659",
    "iso_3166-2": "ISO 3166-2:KN",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029",
    "coordinates": [
      6,
      5
    ]
  },
  {
    "name": "St. Lucia",
    "alpha-2": "LC",
    "alpha-3": "LCA",
    "country-code": "662",
    "iso_3166-2": "ISO 3166-2:LC",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029",
    "coordinates": [
      7,
      5
    ]
  },
  {
    "name": "Saint Martin (French part)",
    "alpha-2": "MF",
    "alpha-3": "MAF",
    "country-code": "663",
    "iso_3166-2": "ISO 3166-2:MF",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029"
  },
  {
    "name": "Saint Pierre and Miquelon",
    "alpha-2": "PM",
    "alpha-3": "SPM",
    "country-code": "666",
    "iso_3166-2": "ISO 3166-2:PM",
    "region": "Americas",
    "sub-region": "Northern America",
    "region-code": "019",
    "sub-region-code": "021"
  },
  {
    "name": "St. Vincent & the Grenadines",
    "alpha-2": "VC",
    "alpha-3": "VCT",
    "country-code": "670",
    "iso_3166-2": "ISO 3166-2:VC",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029",
    "coordinates": [
      7,
      6
    ]
  },
  {
    "name": "Samoa",
    "alpha-2": "WS",
    "alpha-3": "WSM",
    "country-code": "882",
    "iso_3166-2": "ISO 3166-2:WS",
    "region": "Oceania",
    "sub-region": "Polynesia",
    "region-code": "009",
    "sub-region-code": "061",
    "coordinates": [
      28,
      18
    ]
  },
  {
    "name": "San Marino",
    "alpha-2": "SM",
    "alpha-3": "SMR",
    "country-code": "674",
    "iso_3166-2": "ISO 3166-2:SM",
    "region": "Europe",
    "sub-region": "Southern Europe",
    "region-code": "150",
    "sub-region-code": "039"
  },
  {
    "name": "Sao Tome and Principe",
    "alpha-2": "ST",
    "alpha-3": "STP",
    "country-code": "678",
    "iso_3166-2": "ISO 3166-2:ST",
    "region": "Africa",
    "sub-region": "Middle Africa",
    "region-code": "002",
    "sub-region-code": "017",
    "coordinates": [
      11,
      16
    ]
  },
  {
    "name": "Saudi Arabia",
    "alpha-2": "SA",
    "alpha-3": "SAU",
    "country-code": "682",
    "iso_3166-2": "ISO 3166-2:SA",
    "region": "Asia",
    "sub-region": "Western Asia",
    "region-code": "142",
    "sub-region-code": "145",
    "coordinates": [
      19,
      9
    ]
  },
  {
    "name": "Senegal",
    "alpha-2": "SN",
    "alpha-3": "SEN",
    "country-code": "686",
    "iso_3166-2": "ISO 3166-2:SN",
    "region": "Africa",
    "sub-region": "Western Africa",
    "region-code": "002",
    "sub-region-code": "011",
    "coordinates": [
      13,
      12
    ]
  },
  {
    "name": "Serbia",
    "alpha-2": "RS",
    "alpha-3": "SRB",
    "country-code": "688",
    "iso_3166-2": "ISO 3166-2:RS",
    "region": "Europe",
    "sub-region": "Southern Europe",
    "region-code": "150",
    "sub-region-code": "039",
    "coordinates": [
      16,
      7
    ]
  },
  {
    "name": "Seychelles",
    "alpha-2": "SC",
    "alpha-3": "SYC",
    "country-code": "690",
    "iso_3166-2": "ISO 3166-2:SC",
    "region": "Africa",
    "sub-region": "Eastern Africa",
    "region-code": "002",
    "sub-region-code": "014",
    "coordinates": [
      18,
      17
    ]
  },
  {
    "name": "Sierra Leone",
    "alpha-2": "SL",
    "alpha-3": "SLE",
    "country-code": "694",
    "iso_3166-2": "ISO 3166-2:SL",
    "region": "Africa",
    "sub-region": "Western Africa",
    "region-code": "002",
    "sub-region-code": "011",
    "coordinates": [
      12,
      13
    ]
  },
  {
    "name": "Singapore",
    "alpha-2": "SG",
    "alpha-3": "SGP",
    "country-code": "702",
    "iso_3166-2": "ISO 3166-2:SG",
    "region": "Asia",
    "sub-region": "South-Eastern Asia",
    "region-code": "142",
    "sub-region-code": "035",
    "coordinates": [
      24,
      13
    ]
  },
  {
    "name": "Sint Maarten (Dutch part)",
    "alpha-2": "SX",
    "alpha-3": "SXM",
    "country-code": "534",
    "iso_3166-2": "ISO 3166-2:SX",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029"
  },
  {
    "name": "Slovakia",
    "alpha-2": "SK",
    "alpha-3": "SVK",
    "country-code": "703",
    "iso_3166-2": "ISO 3166-2:SK",
    "region": "Europe",
    "sub-region": "Eastern Europe",
    "region-code": "150",
    "sub-region-code": "151",
    "coordinates": [
      16,
      5
    ]
  },
  {
    "name": "Slovenia",
    "alpha-2": "SI",
    "alpha-3": "SVN",
    "country-code": "705",
    "iso_3166-2": "ISO 3166-2:SI",
    "region": "Europe",
    "sub-region": "Southern Europe",
    "region-code": "150",
    "sub-region-code": "039",
    "coordinates": [
      14,
      6
    ]
  },
  {
    "name": "Solomon Islands",
    "alpha-2": "SB",
    "alpha-3": "SLB",
    "country-code": "090",
    "iso_3166-2": "ISO 3166-2:SB",
    "region": "Oceania",
    "sub-region": "Melanesia",
    "region-code": "009",
    "sub-region-code": "054",
    "coordinates": [
      26,
      18
    ]
  },
  {
    "name": "Somalia",
    "alpha-2": "SO",
    "alpha-3": "SOM",
    "country-code": "706",
    "iso_3166-2": "ISO 3166-2:SO",
    "region": "Africa",
    "sub-region": "Eastern Africa",
    "region-code": "002",
    "sub-region-code": "014",
    "coordinates": [
      18,
      14
    ]
  },
  {
    "name": "South Africa",
    "alpha-2": "ZA",
    "alpha-3": "ZAF",
    "country-code": "710",
    "iso_3166-2": "ISO 3166-2:ZA",
    "region": "Africa",
    "sub-region": "Southern Africa",
    "region-code": "002",
    "sub-region-code": "018",
    "coordinates": [
      16,
      20
    ]
  },
  {
    "name": "South Georgia and the South Sandwich Islands",
    "alpha-2": "GS",
    "alpha-3": "SGS",
    "country-code": "239",
    "iso_3166-2": "ISO 3166-2:GS",
    "sub-region-code": null,
    "region-code": null,
    "sub-region": null,
    "region": null
  },
  {
    "name": "South Sudan",
    "alpha-2": "SS",
    "alpha-3": "SSD",
    "country-code": "728",
    "iso_3166-2": "ISO 3166-2:SS",
    "region": "Africa",
    "sub-region": "Eastern Africa",
    "region-code": "002",
    "sub-region-code": "014",
    "coordinates": [
      15,
      13
    ]
  },
  {
    "name": "Spain",
    "alpha-2": "ES",
    "alpha-3": "ESP",
    "country-code": "724",
    "iso_3166-2": "ISO 3166-2:ES",
    "region": "Europe",
    "sub-region": "Southern Europe",
    "region-code": "150",
    "sub-region-code": "039",
    "coordinates": [
      12,
      6
    ]
  },
  {
    "name": "Sri Lanka",
    "alpha-2": "LK",
    "alpha-3": "LKA",
    "country-code": "144",
    "iso_3166-2": "ISO 3166-2:LK",
    "region": "Asia",
    "sub-region": "Southern Asia",
    "region-code": "142",
    "sub-region-code": "034",
    "coordinates": [
      22,
      11
    ]
  },
  {
    "name": "Sudan",
    "alpha-2": "SD",
    "alpha-3": "SDN",
    "country-code": "729",
    "iso_3166-2": "ISO 3166-2:SD",
    "region": "Africa",
    "sub-region": "Northern Africa",
    "region-code": "002",
    "sub-region-code": "015",
    "coordinates": [
      16,
      12
    ]
  },
  {
    "name": "Suriname",
    "alpha-2": "SR",
    "alpha-3": "SUR",
    "country-code": "740",
    "iso_3166-2": "ISO 3166-2:SR",
    "region": "Americas",
    "sub-region": "South America",
    "region-code": "019",
    "sub-region-code": "005",
    "coordinates": [
      7,
      11
    ]
  },
  {
    "name": "Svalbard and Jan Mayen",
    "alpha-2": "SJ",
    "alpha-3": "SJM",
    "country-code": "744",
    "iso_3166-2": "ISO 3166-2:SJ",
    "region": "Europe",
    "sub-region": "Northern Europe",
    "region-code": "150",
    "sub-region-code": "154"
  },
  {
    "name": "Swaziland",
    "alpha-2": "SZ",
    "alpha-3": "SWZ",
    "country-code": "748",
    "iso_3166-2": "ISO 3166-2:SZ",
    "region": "Africa",
    "sub-region": "Southern Africa",
    "region-code": "002",
    "sub-region-code": "018",
    "coordinates": [
      16,
      19
    ]
  },
  {
    "name": "Sweden",
    "alpha-2": "SE",
    "alpha-3": "SWE",
    "country-code": "752",
    "iso_3166-2": "ISO 3166-2:SE",
    "region": "Europe",
    "sub-region": "Northern Europe",
    "region-code": "150",
    "sub-region-code": "154",
    "coordinates": [
      16,
      1
    ]
  },
  {
    "name": "Switzerland",
    "alpha-2": "CH",
    "alpha-3": "CHE",
    "country-code": "756",
    "iso_3166-2": "ISO 3166-2:CH",
    "region": "Europe",
    "sub-region": "Western Europe",
    "region-code": "150",
    "sub-region-code": "155",
    "coordinates": [
      14,
      5
    ]
  },
  {
    "name": "Syria",
    "alpha-2": "SY",
    "alpha-3": "SYR",
    "country-code": "760",
    "iso_3166-2": "ISO 3166-2:SY",
    "region": "Asia",
    "sub-region": "Western Asia",
    "region-code": "142",
    "sub-region-code": "145",
    "coordinates": [
      19,
      7
    ]
  },
  {
    "name": "Taiwan, Province of China",
    "alpha-2": "TW",
    "alpha-3": "TWN",
    "country-code": "158",
    "iso_3166-2": "ISO 3166-2:TW",
    "region": "Asia",
    "sub-region": "Eastern Asia",
    "region-code": "142",
    "sub-region-code": "030"
  },
  {
    "name": "Tajikistan",
    "alpha-2": "TJ",
    "alpha-3": "TJK",
    "country-code": "762",
    "iso_3166-2": "ISO 3166-2:TJ",
    "region": "Asia",
    "sub-region": "Central Asia",
    "region-code": "142",
    "sub-region-code": "143",
    "coordinates": [
      23,
      7
    ]
  },
  {
    "name": "Tanzania",
    "alpha-2": "TZ",
    "alpha-3": "TZA",
    "country-code": "834",
    "iso_3166-2": "ISO 3166-2:TZ",
    "region": "Africa",
    "sub-region": "Eastern Africa",
    "region-code": "002",
    "sub-region-code": "014",
    "coordinates": [
      17,
      16
    ]
  },
  {
    "name": "Thailand",
    "alpha-2": "TH",
    "alpha-3": "THA",
    "country-code": "764",
    "iso_3166-2": "ISO 3166-2:TH",
    "region": "Asia",
    "sub-region": "South-Eastern Asia",
    "region-code": "142",
    "sub-region-code": "035",
    "coordinates": [
      24,
      10
    ]
  },
  {
    "name": "Timor-Leste",
    "alpha-2": "TL",
    "alpha-3": "TLS",
    "country-code": "626",
    "iso_3166-2": "ISO 3166-2:TL",
    "region": "Asia",
    "sub-region": "South-Eastern Asia",
    "region-code": "142",
    "sub-region-code": "035",
    "coordinates": [
      25,
      14
    ]
  },
  {
    "name": "Togo",
    "alpha-2": "TG",
    "alpha-3": "TGO",
    "country-code": "768",
    "iso_3166-2": "ISO 3166-2:TG",
    "region": "Africa",
    "sub-region": "Western Africa",
    "region-code": "002",
    "sub-region-code": "011",
    "coordinates": [
      14,
      14
    ]
  },
  {
    "name": "Tokelau",
    "alpha-2": "TK",
    "alpha-3": "TKL",
    "country-code": "772",
    "iso_3166-2": "ISO 3166-2:TK",
    "region": "Oceania",
    "sub-region": "Polynesia",
    "region-code": "009",
    "sub-region-code": "061"
  },
  {
    "name": "Tonga",
    "alpha-2": "TO",
    "alpha-3": "TON",
    "country-code": "776",
    "iso_3166-2": "ISO 3166-2:TO",
    "region": "Oceania",
    "sub-region": "Polynesia",
    "region-code": "009",
    "sub-region-code": "061",
    "coordinates": [
      28,
      19
    ]
  },
  {
    "name": "Trinidad & Tobago",
    "alpha-2": "TT",
    "alpha-3": "TTO",
    "country-code": "780",
    "iso_3166-2": "ISO 3166-2:TT",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029",
    "coordinates": [
      7,
      9
    ]
  },
  {
    "name": "Tunisia",
    "alpha-2": "TN",
    "alpha-3": "TUN",
    "country-code": "788",
    "iso_3166-2": "ISO 3166-2:TN",
    "region": "Africa",
    "sub-region": "Northern Africa",
    "region-code": "002",
    "sub-region-code": "015",
    "coordinates": [
      14,
      11
    ]
  },
  {
    "name": "Turkey",
    "alpha-2": "TR",
    "alpha-3": "TUR",
    "country-code": "792",
    "iso_3166-2": "ISO 3166-2:TR",
    "region": "Asia",
    "sub-region": "Western Asia",
    "region-code": "142",
    "sub-region-code": "145",
    "coordinates": [
      18,
      7
    ]
  },
  {
    "name": "Turkmenistan",
    "alpha-2": "TM",
    "alpha-3": "TKM",
    "country-code": "795",
    "iso_3166-2": "ISO 3166-2:TM",
    "region": "Asia",
    "sub-region": "Central Asia",
    "region-code": "142",
    "sub-region-code": "143",
    "coordinates": [
      22,
      7
    ]
  },
  {
    "name": "Turks and Caicos Islands",
    "alpha-2": "TC",
    "alpha-3": "TCA",
    "country-code": "796",
    "iso_3166-2": "ISO 3166-2:TC",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029"
  },
  {
    "name": "Tuvalu",
    "alpha-2": "TV",
    "alpha-3": "TUV",
    "country-code": "798",
    "iso_3166-2": "ISO 3166-2:TV",
    "region": "Oceania",
    "sub-region": "Polynesia",
    "region-code": "009",
    "sub-region-code": "061",
    "coordinates": [
      27,
      18
    ]
  },
  {
    "name": "Uganda",
    "alpha-2": "UG",
    "alpha-3": "UGA",
    "country-code": "800",
    "iso_3166-2": "ISO 3166-2:UG",
    "region": "Africa",
    "sub-region": "Eastern Africa",
    "region-code": "002",
    "sub-region-code": "014",
    "coordinates": [
      16,
      15
    ]
  },
  {
    "name": "Ukraine",
    "alpha-2": "UA",
    "alpha-3": "UKR",
    "country-code": "804",
    "iso_3166-2": "ISO 3166-2:UA",
    "region": "Europe",
    "sub-region": "Eastern Europe",
    "region-code": "150",
    "sub-region-code": "151",
    "coordinates": [
      17,
      5
    ]
  },
  {
    "name": "United Arab Emirates",
    "alpha-2": "AE",
    "alpha-3": "ARE",
    "country-code": "784",
    "iso_3166-2": "ISO 3166-2:AE",
    "region": "Asia",
    "sub-region": "Western Asia",
    "region-code": "142",
    "sub-region-code": "145",
    "coordinates": [
      20,
      10
    ]
  },
  {
    "name": "Great Britain and Northern Ireland",
    "alpha-2": "GB",
    "alpha-3": "GBR",
    "country-code": "826",
    "iso_3166-2": "ISO 3166-2:GB",
    "region": "Europe",
    "sub-region": "Northern Europe",
    "region-code": "150",
    "sub-region-code": "154",
    "coordinates": [
      11,
      4
    ]
  },
  {
    "name": "United States of America",
    "alpha-2": "US",
    "alpha-3": "USA",
    "country-code": "840",
    "iso_3166-2": "ISO 3166-2:US",
    "region": "Americas",
    "sub-region": "Northern America",
    "region-code": "019",
    "sub-region-code": "021",
    "coordinates": [
      1,
      2
    ]
  },
  {
    "name": "United States Minor Outlying Islands",
    "alpha-2": "UM",
    "alpha-3": "UMI",
    "country-code": "581",
    "iso_3166-2": "ISO 3166-2:UM",
    "sub-region-code": null,
    "region-code": null,
    "sub-region": null,
    "region": null
  },
  {
    "name": "Uruguay",
    "alpha-2": "UY",
    "alpha-3": "URY",
    "country-code": "858",
    "iso_3166-2": "ISO 3166-2:UY",
    "region": "Americas",
    "sub-region": "South America",
    "region-code": "019",
    "sub-region-code": "005",
    "coordinates": [
      7,
      12
    ]
  },
  {
    "name": "Uzbekistan",
    "alpha-2": "UZ",
    "alpha-3": "UZB",
    "country-code": "860",
    "iso_3166-2": "ISO 3166-2:UZ",
    "region": "Asia",
    "sub-region": "Central Asia",
    "region-code": "142",
    "sub-region-code": "143",
    "coordinates": [
      22,
      6
    ]
  },
  {
    "name": "Vanuatu",
    "alpha-2": "VU",
    "alpha-3": "VUT",
    "country-code": "548",
    "iso_3166-2": "ISO 3166-2:VU",
    "region": "Oceania",
    "sub-region": "Melanesia",
    "region-code": "009",
    "sub-region-code": "054",
    "coordinates": [
      26,
      19
    ]
  },
  {
    "name": "Venezuela",
    "alpha-2": "VE",
    "alpha-3": "VEN",
    "country-code": "862",
    "iso_3166-2": "ISO 3166-2:VE",
    "region": "Americas",
    "sub-region": "South America",
    "region-code": "019",
    "sub-region-code": "005",
    "coordinates": [
      6,
      9
    ]
  },
  {
    "name": "Viet Nam",
    "alpha-2": "VN",
    "alpha-3": "VNM",
    "country-code": "704",
    "iso_3166-2": "ISO 3166-2:VN",
    "region": "Asia",
    "sub-region": "South-Eastern Asia",
    "region-code": "142",
    "sub-region-code": "035",
    "coordinates": [
      26,
      9
    ]
  },
  {
    "name": "Virgin Islands (British)",
    "alpha-2": "VG",
    "alpha-3": "VGB",
    "country-code": "092",
    "iso_3166-2": "ISO 3166-2:VG",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029"
  },
  {
    "name": "Virgin Islands (U.S.)",
    "alpha-2": "VI",
    "alpha-3": "VIR",
    "country-code": "850",
    "iso_3166-2": "ISO 3166-2:VI",
    "region": "Americas",
    "sub-region": "Caribbean",
    "region-code": "019",
    "sub-region-code": "029"
  },
  {
    "name": "Wallis and Futuna",
    "alpha-2": "WF",
    "alpha-3": "WLF",
    "country-code": "876",
    "iso_3166-2": "ISO 3166-2:WF",
    "region": "Oceania",
    "sub-region": "Polynesia",
    "region-code": "009",
    "sub-region-code": "061"
  },
  {
    "name": "Western Sahara",
    "alpha-2": "EH",
    "alpha-3": "ESH",
    "country-code": "732",
    "iso_3166-2": "ISO 3166-2:EH",
    "region": "Africa",
    "sub-region": "Northern Africa",
    "region-code": "002",
    "sub-region-code": "015"
  },
  {
    "name": "Yemen",
    "alpha-2": "YE",
    "alpha-3": "YEM",
    "country-code": "887",
    "iso_3166-2": "ISO 3166-2:YE",
    "region": "Asia",
    "sub-region": "Western Asia",
    "region-code": "142",
    "sub-region-code": "145",
    "coordinates": [
      18,
      11
    ]
  },
  {
    "name": "Zambia",
    "alpha-2": "ZM",
    "alpha-3": "ZMB",
    "country-code": "894",
    "iso_3166-2": "ISO 3166-2:ZM",
    "region": "Africa",
    "sub-region": "Eastern Africa",
    "region-code": "002",
    "sub-region-code": "014",
    "coordinates": [
      14,
      18
    ]
  },
  {
    "name": "Zimbabwe",
    "alpha-2": "ZW",
    "alpha-3": "ZWE",
    "country-code": "716",
    "iso_3166-2": "ISO 3166-2:ZW",
    "region": "Africa",
    "sub-region": "Eastern Africa",
    "region-code": "002",
    "sub-region-code": "014",
    "coordinates": [
      16,
      18
    ]
  }
]



let countries= [];
test.map(c=>{
    if(!c.coordinates) return;
    countries.push({
	name: c.name,
	codes: [c["alpha-2"],c["alpha-3"],c["country-code"],c["iso_3166-2"]],
	x: c.coordinates[0],
	y: c.coordinates[1],
    });
});

 
Utils.makeDownloadFile("countries.json",JSON.stringify(countries,null,1));

*/
