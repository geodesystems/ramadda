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
    var ID_RUN = "maprun";
    var ID_STEP = "mapstep";
    var ID_SHOWALl = "showall";
    var ID_ANIMATION_LABEL = "animationlabel";
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

                var buttons = HtmlUtils.div(["id", this.getDomId(ID_RUN), "class", "ramadda-button", "what", "run"], "Start Animation") + "&nbsp;" +
                    HtmlUtils.div(["id", this.getDomId(ID_STEP), "class", "ramadda-button", "what", "run"], "Step") + "&nbsp;" +
                    HtmlUtils.div(["id", this.getDomId(ID_SHOWALl), "class", "ramadda-button", "what", "run"], "Show All") + "&nbsp;" +
                    HtmlUtils.span(["id", this.getDomId(ID_ANIMATION_LABEL), "class", "display-map-animation-label"]);
                buttons = HtmlUtils.div(["class", "display-map-toolbar"], buttons);
                this.jq(ID_TOP_LEFT).append(buttons);
                this.run = this.jq(ID_RUN);
                this.step = this.jq(ID_STEP);
                this.showAll = this.jq(ID_SHOWALl);
                this.animation.label = this.jq(ID_ANIMATION_LABEL);
                this.run.button().click(() => {
                    this.toggleAnimation();
                });
                this.step.button().click(() => {
                    if (!this.animation.running)
                        this.startAnimation(true);
                });
                this.showAll.button().click(() => {
                    this.animation.running = false;
                    this.animation.inAnimation = false;
                    this.animation.label.html("");
                    this.run.html("Start Animation");
                    this.showAllPoints();
                });
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
            this.map.map.events.register("zoomend", "", function() {
                theDisplay.mapBoundsChanged();
            });
            this.map.map.events.register("moveend", "", function() {
                theDisplay.mapBoundsChanged();
            });

            var overrideBounds = false;
            if (this.getProperty("bounds")) {
                overrideBounds = true;
                var toks = this.getProperty("bounds", "").split(",");
                if (toks.length == 4) {
                    if (this.getProperty("showBounds", true)) {
                        var attrs = {};
                        if (this.getProperty("boundsColor")) {
                            attrs.strokeColor = this.getProperty("boundsColor", "");
                        }
                        this.map.addRectangle("bounds", parseFloat(toks[0]), parseFloat(toks[1]), parseFloat(toks[2]), parseFloat(toks[3]), attrs, "");
                    }
                    this.setInitMapBounds(parseFloat(toks[0]), parseFloat(toks[1]), parseFloat(toks[2]), parseFloat(toks[3]));
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
            this.map.map.addLayer(layer);
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
            var bounds = this.map.map.calculateBounds();
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
                north));
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
            return source.getProperty(prop, dflt);
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
            var circles = this.points;
            for (var i = 0; i < circles.length; i++) {
                var circle = circles[i];
                if (circle.style && circle.style.display == "none") continue;
                var center = circle.center;
                var matchedFeature = null;
                var index = -1;

                for (var j = 0; j < features.length; j++) {
                    var feature = features[j];
                    var geometry = feature.geometry;
                    if (!geometry) {
                        break;
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
                                index = j;
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
                        index = j;
                        break;
                    }
                }
                if (matchedFeature) {
                    features.splice(index, 1);
                    style = matchedFeature.style;
                    if (!style) style = {
                        "stylename": "from display"
                    };
                    $.extend(style, circle.style);
                    matchedFeature.style = style;
                    matchedFeature.popupText = circle.text;
                    matchedFeature.dataIndex = i;
                }
            }
            for (var i = 0; i < features.length; i++) {
                var feature = features[i];
                style = feature.style;
                if (!style) style = {
                    "stylename": "from display"
                };
                $.extend(style, {
                    "display": "none"
                });
            }

            /*
            if (("" + this.getProperty("pruneFeatures", "")) == "true") {
                this.vectorLayer.removeFeatures(features);
                var dataBounds = this.vectorLayer.getDataExtent();
                bounds = this.map.transformProjBounds(dataBounds);
                if(!force && this.getProperty("bounds") == null)
                    this.map.centerOnMarkers(bounds, true);
            }
            */
            this.vectorLayer.redraw();
        },
        needsData: function() {
            return true;
        },
        animation: {
            running: false,
            inAnimation: false,
            begin: null,
            end: null,
            dateMin: null,
            dateMax: null,
            dateRange: 0,
            dateFormat: this.getProperty("animationDateFormat", "yyyyMMdd"),
            mode: this.getProperty("animationMode", "cumulative"),
            steps: this.getProperty("animationSteps", 60),
            windowUnit: this.getProperty("animationWindow", ""),
            window: 0,
            speed: parseInt(this.getProperty("animationSpeed", 250)),
        },
        toggleAnimation: function() {
            this.animation.running = !this.animation.running;
            this.run.html(this.animation.running ? "Stop Animation" : "Start Animation");
            if (this.animation.running)
                this.startAnimation();
        },
        startAnimation: function(justOneStep) {
            if (!this.points) {
                return;
            }
            if (!this.animation.dateMax) return;
            if (!justOneStep)
                this.animation.running = true;
            if (!this.animation.inAnimation) {
                this.animation.inAnimation = true;
                this.animation.label.html("");
                var date = this.animation.dateMin;
                this.animation.begin = date;
                var unit = this.animation.windowUnit;
                if (unit != "") {
                    var tmp = 0;
                    var size = 0;
                    //Pad the size
                    if (unit == "decade") {
                        this.animation.begin = new Date(date.getUTCFullYear(), 0);
                        size = 1000 * 60 * 60 * 24 * 365 * 10 + 1000 * 60 * 60 * 24 * 365;
                    } else if (unit == "year") {
                        this.animation.begin = new Date(date.getUTCFullYear(), 0);
                        size = 1000 * 60 * 60 * 24 * 366;
                    } else if (unit == "month") {
                        this.animation.begin = new Date(date.getUTCFullYear(), date.getMonth());
                        size = 1000 * 60 * 60 * 24 * 32;
                    } else if (unit == "day") {
                        this.animation.begin = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay());
                        size = 1000 * 60 * 60 * 25;
                    } else if (unit == "hour") {
                        this.animation.begin = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay(), date.getHours());
                        size = 1000 * 60 * 61;
                    } else if (unit == "minute") {
                        this.animation.begin = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay(), date.getHours(), date.getMinutes());
                        size = 1000 * 61;
                    } else {
                        this.animation.begin = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay(), date.getHours(), date.getSeconds());
                        size = 1001;
                    }
                    this.animation.window = size;
                } else {
                    this.animation.window = this.animation.dateRange / this.animation.steps;
                }
                this.animation.end = this.animation.begin;
                for (var i = 0; i < this.points.length; i++) {
                    var point = this.points[i];
                    point.style.display = 'none';
                }
                if (this.map.circles)
                    this.map.circles.redraw();
            }
            this.stepAnimation();
        },
        stepAnimation: function() {
            if (!this.points) return;
            if (!this.animation.dateMax) return;
            var oldEnd = this.animation.end;
            var unit = this.animation.windowUnit;
            var date = new Date(this.animation.end.getTime() + this.animation.window);
            if (unit == "decade") {
                this.animation.end = new Date(date.getUTCFullYear(), 0);
            } else if (unit == "year") {
                this.animation.end = new Date(date.getUTCFullYear(), 0);
            } else if (unit == "month") {
                this.animation.end = new Date(date.getUTCFullYear(), date.getMonth());
            } else if (unit == "day") {
                this.animation.end = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay());
            } else if (unit == "hour") {
                this.animation.end = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay(), date.getHours());
            } else if (unit == "minute") {
                this.animation.end = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay(), date.getHours(), date.getMinutes());
            } else {
                this.animation.end = new Date(this.animation.end.getTime() + this.animation.window);
            }
            if (this.animation.mode == "sliding") {
                this.animation.begin = oldEnd;
            }
            //                console.log("step:" + date  +" -  " + this.animation.end);
            var windowStart = this.animation.begin.getTime();
            var windowEnd = this.animation.end.getTime();
            var atLoc = {};
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
            if (windowEnd < this.animation.dateMax.getTime()) {
                this.animation.label.html(this.formatAnimationDate(this.animation.begin) + " - " + this.formatAnimationDate(this.animation.end));
                if (this.animation.running) {
                    setTimeout(() => this.stepAnimation(), this.animation.speed);
                }
            } else {
                this.animation.running = false;
                this.animation.label.html("");
                this.animation.inAnimation = false;
                this.animation.label.html("");
                this.run.html("Start Animation");
            }
            this.applyVectorMap(true);
        },
        formatAnimationDate: function(d) {
            if (this.animation.dateFormat == "yyyy") {
                return Utils.formatDateYYYY(d);
            } else if (this.animation.dateFormat == "yyyyMMdd") {
                return Utils.formatDateYYYYMMDD(d);
            } else {
                return Utils.formatDate(d);
            }
        },
        showAllPoints: function() {
            if (!this.points) return;
            for (var i = 0; i < this.points.length; i++) {
                var point = this.points[i];
                point.style.display = 'inline';
            }
            if (this.map.circles)
                this.map.circles.redraw();
            this.applyVectorMap(true);
        },

        updateUI: function() {
            this.haveCalledUpdateUI = true;
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
            var fields = pointData.getRecordFields();
            var bounds = {};
            var points = RecordUtil.getPoints(records, bounds);
            if (isNaN(bounds.north)) {
                console.log("no bounds:" + bounds);
                return;
            }
            //console.log("bounds:" + bounds.north +" " + bounds.west +" " + bounds.south +" " + bounds.east);
            this.initBounds = bounds;
            this.setInitMapBounds(bounds.north, bounds.west, bounds.south,
                bounds.east);
            if (this.map == null) {
                return;
            }
            if (points.length == 0) {
                console.log("points.length==0");
                return;
            }

            source = this;
            var radius = parseFloat(this.getDisplayProp(source, "radius", 8));
            var strokeWidth = parseFloat(this.getDisplayProp(source, "strokeWidth", "1"));
            var strokeColor = this.getDisplayProp(source, "strokeColor", "#000");
            var colorByAttr = this.getDisplayProp(source, "colorBy", null);
            var colors = this.getColorTable(true);
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
            var showSegments = this.getProperty("showSegments", false);
            var sizeSegments = this.getProperty("sizeSegments", false);
            var sizeEndPoints = this.getProperty("sizeEndPoints", true);
            var showEndPoints = this.getProperty("showEndPoints", false);
            var endPointSize = parseInt(this.getProperty("endPointSize", "4"));
            var dfltEndPointSize = endPointSize;
            var segmentWidth = parseInt(this.getProperty("segmentWidth", "1"));
            var dfltSegmentWidth = segmentWidth;
            var showPoints = this.getProperty("showPoints", true);
            var lineColor = this.getProperty("lineColor", "green");
            var colorBy = {
                id: colorByAttr,
                minValue: 0,
                maxValue: 0,
                field: null,
                index: -1,
                isString: false,
            };


            var sizeBy = {
                id: this.getDisplayProp(source, "sizeBy", null),
                minValue: 0,
                maxValue: 0,
                field: null,
                index: -1,
                isString: false,
                stringMap: {}
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
                    if (field.getType() == "string") colorBy.isString = true;
                }
                if (field.getId() == sizeBy.id || ("#" + (i + 1)) == sizeBy.id) {
                    sizeBy.field = field;
                    if (field.getType() == "string") sizeBy.isString = true;
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
                    this.setProperty("colorBy", value);
                    this.updateUI();
                });
            }

            sizeBy.index = sizeBy.field != null ? sizeBy.field.getIndex() : -1;
            colorBy.index = colorBy.field != null ? colorBy.field.getIndex() : -1;
            var excludeZero = this.getProperty(PROP_EXCLUDE_ZERO, false);
            this.animation.dateMin = null;
            this.animation.dateMax = null;
            var colorByMap = {};
            var colorByValues = [];

            var colorByMinPerc = this.getDisplayProp(source, "colorByMinPercentile", -1);
            var colorByMaxPerc = this.getDisplayProp(source, "colorByMaxPercentile", -1);

            var justOneMarker = this.getProperty("justOneMarker",false);
            for (var i = 0; i < points.length; i++) {
                var pointRecord = records[i];

                if (this.animation.dateMin == null) {
                    this.animation.dateMin = pointRecord.getDate();
                    this.animation.dateMax = pointRecord.getDate();
                } else {
                    var date = pointRecord.getDate();
                    if (date) {
                        if (date.getTime() < this.animation.dateMin.getTime())
                            this.animation.dateMin = date;
                        if (date.getTime() > this.animation.dateMax.getTime())
                            this.animation.dateMax = date;
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


                if (isNaN(v) || v === null)
                    continue;
                if (excludeZero && v == 0) {
                    continue;
                }
                if (!colorBy.isString) {
                    if (i == 0 || v > colorBy.maxValue) colorBy.maxValue = v;
                    if (i == 0 || v < colorBy.minValue) colorBy.minValue = v;
                }
                if (!sizeBy.isString) {
                    v = tuple[sizeBy.index];
                    if (i == 0 || v > sizeBy.maxValue) sizeBy.maxValue = v;
                    if (i == 0 || v < sizeBy.minValue) sizeBy.minValue = v;
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



            if (this.animation.dateMax) {
                this.animation.dateRange = this.animation.dateMax.getTime() - this.animation.dateMin.getTime();
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
                for (var i = 0; i < this.points.length; i++)
                    this.map.removePoint(this.points[i]);
                for (var i = 0; i < this.lines.length; i++)
                    this.map.removePolygon(this.lines[i]);
                this.points = [];
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
                };

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
                            props.pointRadius = 6 + parseInt(15 * percent);
                        }
                        if (sizeEndPoints) {
                            endPointSize = dfltEndPointSize + parseInt(10 * percent);
                        }
                        if (sizeSegments) {
                            segmentWidth = dfltSegmentWidth + parseInt(10 * percent);
                        }
                    }
                    //                            console.log("percent:" + percent +  " radius: " + props.pointRadius +" Value: " + value  + " range: " + sizeBy.minValue +" " + sizeBy.maxValue);
                }
                if (colorBy.index >= 0) {
                    var value = pointRecord.getData()[colorBy.index];
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

                    props.fillOpacity = 0.8;
                    props.fillColor = colors[index];
                    didColorBy = true;
                }

                var html = this.getRecordHtml(pointRecord, fields);
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
                            var point = this.map.addPoint("endpt-" + i, p1, pointProps, html);
                            this.points.push(point);
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
                    var mapPoint = this.map.addPoint("pt-" + i, point, props, html, dontAddPoint);
                    var date = pointRecord.getDate();
                    if (date) {
                        mapPoint.date = date.getTime();
                    }
                    this.points.push(mapPoint);
                }
            }
            if (didColorBy) {
                this.displayColorTable(colors, ID_BOTTOM, colorBy.origMinValue, colorBy.origMaxValue, {
                    stringValues: colorByValues
                });
            }

            this.applyVectorMap();
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