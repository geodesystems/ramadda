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
            this.map.addFeatureHighlightHandler((feature, highlight)=>{
//		console.log(this.type+ ".featureHighlight");
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
	    this.map.doPopup = this.getProperty("doPopup",true);
	    if(!this.map.doPopup)
		this.map.doSelect = false;
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
	    if(!this.map) return;
	    if(this.highlightMarker) {
		this.map.removePoint(this.highlightMarker);
		this.map.removeMarker(this.highlightMarker);
		this.highlightMarker = null;
	    }
	    if(args.highlight) {
		var point = new OpenLayers.LonLat(args.record.getLongitude(), args.record.getLatitude());
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
		    this.highlightMarker =  this.map.addPoint(args.record.getId(), point, attrs);
		}
		if(this.getProperty("centerOnHighlight",false)) {
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
	    this.updateUI(()=>{
		if(this.getProperty("centerOnFilterChange",false)) {
		    if (this.vectorLayer && this.points) {
			//If we have  a map then don't do anything?
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
		return HtmlUtils.div(["style","display:inline-block;cursor:pointer;padding:1px;border:1px solid rgba(0,0,0,0);", "title","Clip to view", "id",this.getDomId("clip")],HtmlUtils.getIconImage("fa-globe-americas"))+"&nbsp;&nbsp;"+ html;
	    }
	    return html;
	},
	xcnt:0,
	initHeader2:function() {
	    let _this = this;
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

        updateUI: function(callback) {
	    this.lastUpdateTime = null;
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
		}catch(exc) {
		    this.map.setProgress(HtmlUtils.div([ATTR_CLASS, "display-map-message"], "" + exc));
		    return;
		}
		this.map.setProgress("");
		if(callback) callback();
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
	    Utils.displayTimes("time pts=" + points.length,[t1,t2,t3], true);
            this.addLabels(records,fields,points);
            this.applyVectorMap();
	    this.lastUpdateTime = new Date();
	},
        addPoints: function(records, fields, points,pointBounds) {
	    let cidx=0
	    let polygonField = this.getFieldById(fields, this.getProperty("polygonField"));
	    let polygonColorTable = this.getColorTable(true, "polygonColorTable",null);
	    let latlon = this.getProperty("latlon",true);
            let source = this;
            let radius = parseFloat(this.getDisplayProp(source, "radius", 8));
	    if(this.getProperty("scaleRadius")) {
		let radiusScale  = [10000,1,8000,2,5000,3,2000,4,1000,6,500,8,250,10,100,16,50,18];
		radius=radiusScale[1];
		for(let i=0;i<radiusScale.length;i+=2) {
		    if(points.length<+radiusScale[i]) {
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

            let colorBy = this.getColorByInfo(records);
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
	    let addedPoints = [];

	    let textGetter = f=>{
		if(f.record) {
                    return  this.getRecordHtml(f.record, fields, tooltip);
		}
		return null;
	    };

            for (let i = 0; i < records.length; i++) {
                let record = records[i];
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
			if (date) {
			    poly.date = date.getTime();
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
			    mapPoint.highlightTextGetter = textGetter;
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
		"showClipToBounds=true",
		"sizeBy=\"\"",
		"sizeByLog=\"true\"",
		"sizeByMap=\"value1:size,...,valueN:size\"",
		'sizeByRadiusMin="2"',
		'sizeByRadiusMax="20"',
		"boundsAnimation=\"true\"",
		"centerOnFilterChange=\"true\"",
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
            var labelTemplate = this.getProperty("labelTemplate");
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
					'stateField=""',
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
	    var stateField = this.getFieldById(fields,this.getProperty("stateField"));
	    if(stateField==null) {
		stateField = this.getFieldById(fields,"state");
	    }
	    var minx = Number.MAX_VALUE;
	    var miny = Number.MAX_VALUE;
	    var maxx = Number.MIN_VALUE;
	    var maxy = Number.MIN_VALUE;
	    var map = {};
	    this.grid.map(o=>{
		minx = Math.min(minx,o.x);
		maxx = Math.max(maxx,o.x);
		miny = Math.min(miny,o.y);
		maxy = Math.max(maxy,o.y);
		map[o.x+"_"+o.y] = o;
	    });

	    var table ="<table border=0 cellspacing=0 cellpadding=0>";
	    var w = this.getProperty("cellSize","40");
	    var showLabel  = this.getProperty("showCellLabel",true);
	    var cellStyle  = this.getProperty("cellStyle","");
	    var cellMap = {};
	    for(var y=1;y<=maxy;y++) {
		table+="<tr>";
		for(var x=1;x<=maxx;x++) {
		    var id = x+ "_"+y;
		    var o = map[id];
		    var extra = " id='" + id +"' ";
		    var style = "margin:1px;vertical-align:center;text-align:center;width:" + w+"px;" +"height:" + w+"px;";
		    var c = "";
		    if(o) {
			style+="background:#ccc;" + cellStyle;
			extra += " title='" + o.state +"' ";
			extra += " class='display-mapgrid-cell' ";
			c = "<div>" + (showLabel?o.code:"")+"</div>";
			cellMap[o.code] = id;
			cellMap[o.state] = id;
		    }
		    var td = "<td>" + "<div " + extra +" style='" + style +"'>" + c+"</div></td>";
		    table+=td;
		}
		table+="</tr>";
	    }
	    table +="<tr><td colspan=" + maxx+"><br>" +   HtmlUtils.div(["id",this.getDomId(ID_COLORTABLE)]) +"</td></tr>";
	    table+="</table>";
            var colorBy = this.getColorByInfo(records);
	    var strokeColorBy = this.getColorByInfo(records,"strokeColorBy","strokeColorByMap");
	    this.writeHtml(ID_DISPLAY_CONTENTS, table);
	    var contents = this.jq(ID_DISPLAY_CONTENTS);
	    for(var i=0;i<records.length;i++) {
		var record = records[i]; 
		var tuple = record.getData();
		var state = tuple[stateField.getIndex()];
		var cellId = cellMap[state];
		if(!cellId) {
		    cellId = cellMap[state.toUpperCase()];
		}

		if(!cellId) {
		    console.log("Could not find cell:" + state);
		    continue;
		}
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
	    this.makePopups(contents.find(".display-mapgrid-cell"), records);
	    let _this = this;
	    contents.find(".display-mapgrid-cell").click(function() {
		var record = records[$(this).attr("recordIndex")];
		if(record) {
		    _this.getDisplayManager().notifyEvent("handleEventRecordSelection", _this, {record: record});
		}
	    });
            if (colorBy.index >= 0) {
		colorBy.displayColorTable();
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

	grid:  [
{state:"Alaska",code:"AK",x:2,y:1},
{state:"Hawaii",code:"HI",x:2,y:8},
{state:"Washington",code:"WA",x:3,y:3},
{state:"Oregon",code:"OR",x:3,y:4},
{state:"California",code:"CA",x:3,y:5},
{state:"Idaho",code:"ID",x:4,y:3},
{state:"Nevada",code:"NV",x:4,y:4},
{state:"Utah",code:"UT",x:4,y:5},
{state:"Arizona",code:"AZ",x:4,y:6},
{state:"Montana",code:"MT",x:5,y:3},
{state:"Wyoming",code:"WY",x:5,y:4},
{state:"Colorado",code:"CO",x:5,y:5},
{state:"New Mexico",code:"NM",x:5,y:6},
{state:"North Dakota",code:"ND",x:6,y:3},
{state:"South Dakota",code:"SD",x:6,y:4},
{state:"Nebraska",code:"NE",x:6,y:5},
{state:"Kansas",code:"KS",x:6,y:6},
{state:"Oklahoma",code:"OK",x:6,y:7},
{state:"Texas",code:"TX",x:6,y:8},
{state:"Minnesota",code:"MN",x:7,y:3},
{state:"Iowa",code:"IA",x:7,y:4},
{state:"Missouri",code:"MO",x:7,y:5},
{state:"Arkansas",code:"AR",x:7,y:6},
{state:"Louisiana",code:"LA",x:7,y:7},
{state:"Illinois",code:"IL",x:8,y:3},
{state:"Indiana",code:"IN",x:8,y:4},
{state:"Kentucky",code:"KY",x:8,y:5},
{state:"Tennessee",code:"TN",x:8,y:6},
{state:"Mississippi",code:"MS",x:8,y:7},
{state:"Wisconsin",code:"WI",x:9,y:2},
{state:"Ohio",code:"OH",x:9,y:4},
{state:"West Virginia",code:"WV",x:9,y:5},
{state:"North Carolina",code:"NC",x:9,y:6},
{state:"Alabama",code:"AL",x:9,y:7},
{state:"Michigan",code:"MI",x:9,y:3},
{state:"Pennsylvania",code:"PA",x:10,y:4},
{state:"Virginia",code:"VA",x:10,y:5},
{state:"South Carolina",code:"SC",x:10,y:6},
{state:"Georgia",code:"GA",x:10,y:7},
{state:"New York",code:"NY",x:11,y:3},
{state:"New Jersey",code:"NJ",x:11,y:4},
{state:"Maryland",code:"MD",x:11,y:5},
{state:"DC",code:"DC",x:11,y:6},
{state:"Florida",code:"FL",x:11,y:8},
{state:"Vermont",code:"VT",x:12,y:2},
{state:"Rhode Island",code:"RI",x:12,y:3},
{state:"Connecticut",code:"CT",x:12,y:4},
{state:"Delaware",code:"DE",x:12,y:5},
{state:"Maine",code:"ME",x:13,y:1},
{state:"New Hampshire",code:"NH",x:13,y:2},
{state:"Massachusetts",code:"MA",x:13,y:3},
]




    })}
