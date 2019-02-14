/*
 * Copyright (c) 2008-2019 Geode Systems LLC
 */


// Map ids
var map_esri_topo = "esri.topo";
var map_esri_street = "esri.street";
var map_esri_worldimagery = "esri.worldimagery";
var map_opentopo = "opentopo";
var map_usgs_topo = "usgs.topo";
var map_usgs_imagery = "usgs.imagery";
var map_usgs_relief = "usgs.relief";
var map_watercolor = "watercolor";
var map_white = "white";
var map_blue = "blue";
var map_black = "black";
var map_gray = "gray";

var map_usfs_ownership = "usfs.ownership";
var map_osm = "osm";
var map_osm_toner = "osm.toner";
var map_ol_openstreetmap = "ol.openstreetmap";

// Microsoft maps - only work for -180 to 180
var map_ms_shaded = "ms.shaded";
var map_ms_hybrid = "ms.hybrid";
var map_ms_aerial = "ms.aerial";

var map_default_layer = map_osm;

var ramaddaCircleHiliteAttrs = {
    strokeColor: 'black',
    strokeWidth: 1,
    fill: true,
    fillOpacity: 0.5,
    fillColor: 'red'
};

function createLonLat(lon, lat) {
    lon = parseFloat(lon);
    lat = parseFloat(lat);
    return new OpenLayers.LonLat(lon, lat);
}

function createBounds(v1, v2, v3, v4) {
    v1 = parseFloat(v1);
    v2 = parseFloat(v2);
    v3 = parseFloat(v3);
    v4 = parseFloat(v4);
    return new OpenLayers.Bounds(v1, v2, v3, v4);
}

function createProjection(name) {
    return new OpenLayers.Projection(name);
}

var positionMarkerID = "location";
var latlonReadoutID = "ramadda-map-latlonreadout";

var mapDefaults = {
    maxLatValue: 85,
    zoomLevels: 40,
    defaultZoomLevel: 11,
    maxExtent: createBounds(-20037508, -20037508, 20037508, 20037508),
    sourceProjection: createProjection("EPSG:900913"),
    displayProjection: createProjection("EPSG:4326"),
    units: "m",
    doSphericalMercator: true,
    wrapDateline: true,
    location: createLonLat(-100, 40)
}


//Global list of all maps on this page
var ramaddaMaps = new Array();
var ramaddaMapMap = {};

function ramaddaAddMap(map) {
    ramaddaMaps.push(map);
    ramaddaMapMap[map.mapId] = map;
}


function RepositoryMap(mapId, params) {
    if (!params) params = {};
    this.mapId = mapId || "map";
    ramaddaAddMap(this);
    let theMap = this;
    $.extend(this, {
        name: "map",
        sourceProjection: mapDefaults.sourceProjection,
        displayProjection: mapDefaults.displayProjection,
        projectionUnits: mapDefaults.units,
        mapDivId: this.mapId,
        showScaleLine: true,
        showLayerSwitcher: true,
        showZoomPanControl: true,
        showZoomOnlyControl: false,
        showLatLonPosition: true,
        enableDragPan: true,
        defaultLocation: mapDefaults.location,
        initialZoom: mapDefaults.defaultZoomLevel,
        latlonReadout: latlonReadoutID,
        map: null,
        defaultMapLayer: map_default_layer,
        defaultCanSelect: true,
        haveAddedDefaultLayer: false,
        layer: null,
        markers: null,
        vectors: null,
        loadedLayers: [],
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

    initMapFunctions(this);

    var dflt = {
        pointRadius: 3,
        fillOpacity: 0.8,
        fillColor: "#e6e6e6",
        fill: true,
        strokeColor: "#999",
        strokeWidth: 1,
        scrollToZoom: false,
        selectOnHover: false,
        highlightOnHover: true
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

    this.defaults = {};

    if (Utils.isDefined(params.onSelect)) {
        this.onSelect = params.onSelect;
    } else {
        this.onSelect = null;
    }
    if (params.initialLocation) {
        this.defaultLocation = createLonLat(params.initialLocation[1], params.initialLocation[0]);
    } else if (Utils.isDefined(params.initialBounds) && params.initialBounds[0] <= 90 && params.initialBounds[0] >= -90) {
        this.defaultBounds = createBounds(params.initialBounds[1], params.initialBounds[2], params.initialBounds[3], params.initialBounds[0]);
    }


    var options = {
        projection: this.sourceProjection,
        displayProjection: this.displayProjection,
        units: this.projectionUnits,
        controls: [],
        maxResolution: 156543.0339,
        maxExtent: mapDefaults.maxExtent,
        div: this.mapDivId,
        eventListeners: {
            featureover: function(e) {
                theMap.handleFeatureover(e.feature);
            },
            featureout: function(e) {
                theMap.handleFeatureout(e.feature);
            },
            nofeatureclick: function(e) {
                theMap.handleNofeatureclick(e.layer);
            },
            featureclick: function(e) {
                theMap.handleFeatureclick(e.layer, e.feature);
            }
        }
    };

    this.mapOptions = options;
    theMap.finishMapInit();
    jQuery(document).ready(function($) {
        if (theMap.getMap()) {
            theMap.getMap().updateSize();
        }
    });

}

function initMapFunctions(theMap) {
    RamaddaUtil.defineMembers(theMap, {
     
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

            this.map = new OpenLayers.Map(this.mapDivId, this.mapOptions);
            //register the location listeners later since the map triggers a number of
            //events at the start
            var callback = function() {
                _this.map.events.register("changebaselayer", "", function() {
                        _this.baseLayerChanged();
                    });
                _this.map.events.register("zoomend", "", function() {
                        _this.locationChanged();
                    });
                _this.map.events.register("moveend", "", function() {
                        _this.locationChanged();
                    });
            };
            setTimeout(callback,2000);

            if (this.mapHidden) {
                //A hack when we are hidden
                this.map.size = new OpenLayers.Size(1, 1);
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
        },
        locationChanged:function() {
                var latlon = this.transformProjBounds(this.map.getExtent());
                var bounds = "map_bounds=" +latlon.top+","+latlon.left +"," + latlon.bottom +"," + latlon.right;
                var url = ""+window.location;
                url = url.replace(/\&?map_bounds=[-\d\.]+,[-\d\.]+,[-\d\.]+,[-\d\.]+/g,"");
                if(!url.includes("?")) url+="?";
                url +="&" + bounds;
                try {
                    if(window.history.replaceState)
                        window.history.replaceState("", "", url);
                } catch(e) {
                    console.log("err:" + e);
              }
        },
        baseLayerChanged:function() {
                var baseLayer = this.map.baseLayer;
                if(!baseLayer) return;
                baseLayer = baseLayer.ramaddaId;
                var latlon = this.transformProjBounds(this.map.getExtent());
                var arg = "map_layer=" +baseLayer;
                var url = ""+window.location;
                url = url.replace(/\&?map_layer=[a-z\.]+/g,"");
                if(!url.includes("?")) url+="?";
                url +="&" + arg;
                try {
                    if(window.history.replaceState)
                        window.history.replaceState("", "", url);
                } catch(e) {
                    console.log("err:" + e);
                }
        },
        setMapDiv: function(divid) {
            this.mapHidden = false;
            this.mapDivId = divid;
            theMap.getMap().render(theMap.mapDivId);
            theMap.getMap().updateSize();
            theMap.centerOnMarkers(theMap.dfltBounds);
        },
        handleFeatureover: function(feature, skipText) {
            var layer = feature.layer;
            if (!(layer.isMapLayer === true)) {
                /*
                if(feature.ramaddaId) {
                    marker = this.findMarker(feature.ramaddaId);
                    if(marker) {
                        this.circleMarker(feature.ramaddaId,ramaddaCircleHiliteAttrs);
                        return;
                    }
                    }*/
                if (!skipText && feature.text) {
                    this.showFeatureText(feature);
                }
                return;
            }

            if (layer.canSelect === false || !(layer.isMapLayer === true)) return;
            var _this = this;
            if (!feature.isSelected) {
                feature.originalStyle = feature.style;
                feature.style = null;
                layer.drawFeature(feature, "temporary");
                if (this.displayDiv) {
                    this.displayedFeature = feature;
                    var callback = function() {
                        if (_this.displayedFeature == feature) {
                            _this.showText(_this.getFeatureText(layer, feature));
                            _this.dateFeatureOver(feature);
                        }
                    }
                    if (!skipText) {
                        setTimeout(callback, 500);
                    }
                }

            }
        },
        handleFeatureout: function(feature, skipText) {
            layer = feature.layer;
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
            if (layer.canSelect === false) return;
            if (layer.selectedFeature) {
                this.unselectFeature(layer.selectedFeature);
            }
            this.selectedFeature = feature;
            layer.selectedFeature = feature;
            layer.selectedFeature.isSelected = true;
            layer.drawFeature(layer.selectedFeature, "select");
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
                        this.getMap().zoomToExtent(bounds);
                        this.getMap().setCenter(bounds.getCenterLonLat());
                    }
                }
            }
        },


        unselectFeature: function(feature) {
            if (!feature) return;
            feature.renderIntent = null;
            feature.isSelected = false;
            layer = feature.layer;
            if(!layer) return;
            layer.drawFeature(layer.selectedFeature, layer.selectedFeature.style || "default");
            layer.selectedFeature.isSelected = false;
            layer.selectedFeature = null;
            this.selectedFeature = null;
            this.onPopupClose();
            return;

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
        addLayer: function(layer) {
            if (this.map != null) {
                this.map.addLayer(layer);
                this.checkLayerOrder();
            } else {
                this.initialLayers.push(layer);
            }
        },
        checkLayerOrder: function() {
            if (this.circles) {
                this.map.setLayerIndex(this.circles, this.map.layers.length - 1);
                this.map.raiseLayer(this.circles, this.map.layers.length - 1);
                this.circles.redraw();
            }
            if (this.markers) {
                this.map.setLayerIndex(this.markers, this.map.layers.length - 1);
                this.map.raiseLayer(this.markers, this.map.layers.length - 1);
            }
            this.map.resetLayersZIndex();
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
            var imageBounds = createBounds(west, south, east, north);
            imageBounds = this.transformLLBounds(imageBounds);
            var image = new OpenLayers.Layer.Image(
                name, url,
                imageBounds,
                new OpenLayers.Size(width, height), {
                    numZoomLevels: 3,
                    isBaseLayer: theArgs.isBaseLayer,
                        resolutions: this.map.layers[0]?this.map.layers[0].resolutions:null,
                        maxResolution: this.map.layers[0]?this.map.layers[0].resolutions[0]:null
                }
            );

            //        image.setOpacity(0.5);
            if (theArgs.forSelect) {
                theMap.selectImage = image;
            }
            var lonlat = new createLonLat(west, north);
            image.lonlat = this.transformLLPoint(lonlat);

            image.setVisibility(visible);
            image.id = layerId;
            image.text = this.getPopupText(desc);
            image.ramaddaId = layerId;
            image.ramaddaName = name;
            this.addLayer(image);
            image.north = north;
            image.west = west;
            image.south = south;
            image.east = east;
            if (!this.imageLayers) this.imageLayers = {}
            if (!theArgs.isBaseLayer) {
                this.imageLayers[layerId] = image;
            }
            return image;
        },
        addWMSLayer: function(name, url, layer, isBaseLayer) {
            var layer = new OpenLayers.Layer.WMS(name, url, {
                layers: layer,
                format: "image/png",
                isBaseLayer: false,
                srs: "epse:4326",
                transparent: true

            }, {
                wrapDateLine: mapDefaults.wrapDateline
            });
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
            this.addLayer(layer);
        },
        addMapLayer: function(name, url, layer, isBaseLayer, isDefault) {
            var layer;
            if (/\/tile\//.exec(url)) {
                layer = new OpenLayers.Layer.XYZ(
                    name, url, {
                        sphericalMercator: mapDefaults.doSphericalMercator,
                        numZoomLevels: mapDefaults.zoomLevels,
                        wrapDateLine: mapDefaults.wrapDateline
                    });
            } else {
                layer = new OpenLayers.Layer.WMS(name, url, {
                    layers: layer,
                    format: "image/png"
                }, {
                    wrapDateLine: mapDefaults.wrapDateline
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
                this.map.setLayerIndex(layer, 0);
                this.map.setBaseLayer(layer);
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
            map = new OpenLayers.StyleMap({
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
            value = "";
            if ((typeof p[attr] == 'object') || (typeof p[attr] == 'Object')) {
                var o = p[attr];
                if (o) {
                    value = "" + o["value"];
                }
            } else {
                value = "" + p[attr];
            }
            return value;
        },

        searchFor: function(searchFor) {
            var _this = this;
            if (searchFor) searchFor = searchFor.trim();
            if (searchFor == "") searchFor = null;
            this.searchText = searchFor;
            var bounds = null;
            var toks = null;
            var doHelp = false;
            var download = $("#" + this.searchDiv + "_download").is(':checked');

            var attrs = [];
            if (searchFor) {
                searchFor = searchFor.trim();
                if (searchFor == "?") {
                    doHelp = true;
                }
                searchFor = searchFor.toLowerCase();
                toks = [];
                var doOr = true;
                tmp = searchFor.split("|");
                if (tmp.length == 1) {
                    tmp = searchFor.split("&");
                    doOr = tmp.length == 1;
                }
                for (var i = 0; i < tmp.length; i++) {
                    var not = false;
                    var equals = false;
                    var tok = tmp[i];
                    tmp2 = tok.split(":");
                    var field = "";
                    var value = tok;
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

            for (a in this.loadedLayers) {
                var layer = this.loadedLayers[a];
                if (layer.selectedFeature) {
                    this.handleNofeatureclick(layer);
                }
                for (f in layer.features) {
                    var feature = layer.features[f];
                    var p = feature.attributes;
                    if (!searchFor) {
                        feature.featureVisibleSearch = true;
                        attrs.push(p);
                        continue;
                    }
                    if (doHelp) {
                        var space = "&nbsp;&nbsp;&nbsp;"
                        var help = "Enter, e.g.:<br>";
                        help += space + "<i>&lt;term&gt;</i> (any match)<br>";
                        help += space + "<i>=&lt;term&gt;</i> (exact match)<br>";
                        help += space + "<i>!&lt;term&gt;</i> (not match)<br>";
                        help += space + "<i>&lt;term 1&gt;|&lt;term 2&gt;</i> (match any)<br>";
                        help += space + "<i>&lt;term 1&gt;&amp;&lt;term 2&gt;</i> (match all)<br>";
                        help += "Or by field:<br>";
                        for (var attr in p) {
                            help += space + attr.toLowerCase() + ":" + "<i>&lt;term&gt;</i><br>";
                        }
                        this.showText(help);
                        return
                    }
                    var matches = false;
                    var checkAll = false;
                    var allMatched = true;
                    var someMatched = false;
                    if (tok.not) checkAll = true;
                    for (v in toks) {
                        var tok = toks[v];
                        for (var attr in p) {
                            if (tok.field != "" && tok.field != attr.toLowerCase()) {
                                continue;
                            }
                            var value = this.getAttrValue(p, attr);
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

                if (download) {
                    var csv = "";
                    for (var i in attrs) {
                        var p = attrs[i];
                        for (var attr in p) {
                            if (csv != "") csv += ",";
                            csv += attr.toLowerCase();
                        }
                        csv += "\n";
                        break;
                    }
                    for (var i in attrs) {
                        var p = attrs[i];
                        var line = "";
                        for (var attr in p) {
                            var value = this.getAttrValue(p, attr);
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
            var layer = feature.layer;
            var visible = this.getFeatureVisible(feature);
            if (feature.originalStyle) {
                feature.style = feature.originalStyle;
            }
            var style = feature.style;
            if (!style) {
                style = {};
                var defaultStyle = layer.styleMap.styles["default"].defaultStyle;
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
            var visible = true;
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
            var _this = this;
            var didSearch = this.searchText || (this.startDate && this.endDate);
            var bounds = null;
            var html = "";
            var didOn = false;
            var didOff = false;
            var cnt = 0;
            var onFeature = null;
            for (var i = 0; i < layer.features.length; i++) {
                var feature = layer.features[i];
                var visible = this.checkFeatureVisible(feature, false);
                if (!visible) {
                    this.clearDateFeature(feature);
                    didOff = true;
                } else {
                    this.dateFeatureSelect(feature);
                    didOn = true;
                    cnt++;
                    if (!onFeature) onFeature = feature;
                    html += HtmlUtils.div(["class", "ramadda-map-feature", "feature-index", "" + i], this.getFeatureName(feature));
                    var geometry = feature.geometry;
                    if (geometry) {
                        var fbounds = geometry.getBounds();
                        if (bounds) bounds.extend(fbounds);
                        else bounds = fbounds;
                    }
                }
            }
            if (cnt == 1) {
                this.showText(this.getFeatureText(layer, onFeature));
            } else {
                if (didSearch || (didOn && didOff)) {
                    var id = this.mapDivId + "_features";
                    this.showText(HtmlUtils.div(["id", id, "class", "ramadda-map-features"], html));
                    $("#" + id + " .ramadda-map-feature").click(function() {
                        var index = parseInt($(this).attr("feature-index"));
                        _this.handleFeatureclick(layer, layer.features[index], true);
                    });
                    $("#" + id + " .ramadda-map-feature").mouseover(function() {
                        var index = parseInt($(this).attr("feature-index"));
                        _this.handleFeatureover(layer.features[index], true);
                    });
                    $("#" + id + " .ramadda-map-feature").mouseout(function() {
                        var index = parseInt($(this).attr("feature-index"));
                        _this.handleFeatureout(layer.features[index], true);
                    });

                } else {
                    this.clearDateFeature();
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
                this.getMap().zoomToExtent(bounds);
                this.getMap().setCenter(bounds.getCenterLonLat());
            } else {
                this.centerOnMarkers(theMap.dfltBounds, this.centerOnMarkersForce);
            }
        },
        getFeatureText: function(layer, feature) {
            var style = feature.style || feature.originalStyle || layer.style;
            var p = feature.attributes;
            var out = feature.popupText;
            if (!out) {
                if (style && style["balloonStyle"]) {
                    out = style["balloonStyle"];
                    for (var attr in p) {
                        //$[styleid/attr]
                        var label = attr.replace("_", " ");
                        var value = "";
                        if (typeof p[attr] == 'object' || typeof p[attr] == 'Object') {
                            var o = p[attr];
                            value = "" + o["value"];
                        } else {
                            value = "" + p[attr];
                        }
                        out = out.replace("${" + style.id + "/" + attr + "}", value);
                    }
                } else {
                    out = "<table>";
                    for (var attr in p) {
                        var label = attr;
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
                        var value;
                        if (p[attr] != null && (typeof p[attr] == 'object' || typeof p[attr] == 'Object')) {
                            var o = p[attr];
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
            if (this.onSelect) {
                func = window[this.onSelect];
                func(this, layer);
                return;
            }
            feature = layer.feature;
            var out = this.getFeatureText(layer, feature);
            if (this.currentPopup) {
                this.map.removePopup(this.currentPopup);
                this.currentPopup.destroy();
            }
            var popup = new OpenLayers.Popup.FramedCloud("popup", feature.geometry.getBounds().getCenterLonLat(),
                null, out, null, true,
                function() {
                    theMap.onPopupClose()
                });
            feature.popup = popup;
            popup.feature = feature;

            //Use theMap instead of this because when the function is called the this object isn't the ramaddamap
            theMap.map.addPopup(popup);
            theMap.currentPopup = popup;
        }
    });

    theMap.onFeatureUnselect = function(layer) {
        this.onPopupClose();
    }

    theMap.removeKMLLayer = function(layer) {
        this.map.removeLayer(layer);
    }

    theMap.setDefaultCanSelect = function(canSelect) {
        this.defaultCanSelect = canSelect;
    }

    theMap.getCanSelect = function(canSelect) {
        if (((typeof canSelect) == "undefined") || (canSelect == null)) {
            return this.defaultCanSelect;
        }
        return canSelect;
    }

    theMap.addSelectCallback = function(layer, canSelect, selectCallback, unselectCallback) {
        var _this = this;
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
               this.map.addControl(highlight);
               highlight.activate();   
               }
               this.map.addControl(select);
               select.activate();   
            */
        }
    };

    theMap.startDate = null;
    theMap.endDate = null;
    theMap.startFeature = null;
    theMap.endFeature = null;
    theMap.initDates = function(layer) {
        var _this = this;
        var features = layer.features;
        var didDate = false;
        var didYear = false;
        this.hasDates = false;
        this.dates = [];
        this.dateFeatures = [];
        this.minDate = null;
        this.maxDate = null;
        for (var i = 0; i < features.length; i++) {
            var feature = features[i];
            var p = feature.attributes;
            for (var attr in p) {
                var name = ("" + attr).toLowerCase();
                var isYear = name.includes("year") || name.includes("_yr");
                var isDate = name.includes("date");
                if (!(isDate || isYear)) continue;
                var value = this.getAttrValue(p, attr);
                if (!value) continue;
                try {
                    var date = Utils.parseDate(value);
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
            var options = null;
            if (didYear)
                options = {
                    year: 'numeric'
                };
            $("#" + this.mapDivId + "_footer").html(HtmlUtils.div(["class", "ramadda-map-animation", "id", this.mapDivId + "_animation"], ""));
            this.animation = $("#" + this.mapDivId + "_animation");
            var ticksDiv = HtmlUtils.div(["class", "ramadda-map-animation-ticks", "id", this.mapDivId + "_animation_ticks"], "");
            var infoDiv = HtmlUtils.div(["class", "ramadda-map-animation-info", "id", this.mapDivId + "_animation_info"], "");
            this.animation.html(ticksDiv + infoDiv);
            var startLabel = Utils.formatDate(this.minDate, options);
            var endLabel = Utils.formatDate(this.maxDate, options);
            this.animationTicks = $("#" + this.mapDivId + "_animation_ticks");
            this.animationInfo = $("#" + this.mapDivId + "_animation_info");
            var center = "";
            if (this.startDate && this.endDate) {
                center = HtmlUtils.div(["id", this.mapDivId + "_ticks_reset", "class", "ramadda-map-animation-tick-reset"], "Reset");
            }
            var info = "<table width=100%><tr valign=top><td width=40%>" + startLabel + "</td><td align=center width=20%>" + center + "</td><td align=right width=40%>" + endLabel + "</td></tr></table>";
            this.animationInfo.html(info);
            if (this.startDate && this.endDate) {
                var reset = $("#" + this.mapDivId + "_ticks_reset");
                reset.click(function() {
                    _this.startDate = null;
                    _this.endDate = null;
                    _this.startFeature = null;
                    _this.endFeature = null;
                    _this.setFeatureDateRange(layer);
                });
            }
            var width = this.animationTicks.width();
            var percentPad = width > 0 ? 5 / width : 0;
            //            console.log("w:" + width + " " + percentPad);
            var html = "";
            var start = this.minDate.getTime();
            var end = this.maxDate.getTime();
            if (this.startDate != null && this.endDate != null) {
                start = this.startDate.getTime();
                end = this.endDate.getTime();
            }
            var range = end - start;
            if (range > 0) {
                for (var i = 0; i < this.dates.length; i++) {
                    var date = this.dates[i];
                    var time = date.getTime();
                    if (time < start || time > end) continue;
                    var feature = this.dateFeatures[i];
                    feature.dateIndex = i;
                    var percent = 100 * (time - start) / range;
                    percent = percent - percent * percentPad;
                    var fdate = date.toLocaleDateString("en-US", options);
                    var name = Utils.camelCase(this.getFeatureName(feature));
                    var tooltip = "";
                    tooltip += name != null ? name + "<br>" : "";
                    tooltip += fdate;
                    tooltip += "<br>shift-click: set visible range<br>cmd/ctrl-click:zoom";
                    tooltip += "";
                    html += HtmlUtils.div(["id", this.mapDivId + "_tick" + i, "feature-index", "" + i, "style", "left:" + percent + "%", "class", "ramadda-map-animation-tick", "title", tooltip], "");
                }
            }
            this.animationTicks.html(html);
            var tick = $("#" + this.mapDivId + "_animation .ramadda-map-animation-tick");
            tick.tooltip({
                content: function() {
                    return $(this).prop('title');
                },
                position: {
                    my: "left top",
                    at: "left bottom+2"
                },
                classes: {
                    "ui-tooltip": "ramadda-tooltip"
                }
            });
            tick.mouseover(function() {
                var index = parseInt($(this).attr("feature-index"));
                var feature = _this.dateFeatures[index];
                _this.handleFeatureover(feature, true);
            });
            tick.mouseout(function() {
                var index = parseInt($(this).attr("feature-index"));
                var feature = _this.dateFeatures[index];
                _this.handleFeatureout(feature, true);
            });
            tick.click(function(evt) {
                var index = parseInt($(this).attr("feature-index"));
                var feature = _this.dateFeatures[index];
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
                    console.log("date:" + _this.startDate + " " + _this.endDate);
                    if (_this.startDate != null && _this.endDate != null) {
                        if (_this.startDate.getTime() == _this.endDate.getTime()) {
                            _this.startDate = null;
                            _this.startFeature = null;
                            _this.endDate = null;
                            _this.endFeature = null;
                            _this.dateFeatureSelect(null);
                            return;
                        } else if (_this.startDate.getTime() > _this.endDate.getTime()) {
                            var tmp = _this.startDate;
                            _this.startDate = _this.endDate;
                            _this.endDate = tmp;
                            tmp = _this.startFeature;
                            _this.startFeature = _this.endFeature;
                            _this.endFeature = tmp;
                        }
                        _this.setFeatureDateRange(feature.layer);
                    }
                } else {
                    var center = evt.metaKey || evt.ctrlKey;
                    if (_this.startDate != null || _this.endDate != null) {
                        _this.startDate = null;
                        _this.endDate = null;
                        _this.setFeatureDateRange(feature.layer, feature.featureDate);
                        //                            center = true;
                    }
                    _this.clearDateFeature();
                    _this.handleFeatureclick(feature.layer, feature, center);
                }
            });
        }
    }

    theMap.setFeatureDateRange = function(layer) {
        this.dateFeatureSelect(null);
        console.log("set range:" + this.startDate + " " + this.endDate);
        var features = layer.features;
        if (layer.selectedFeature) {
            this.unselectFeature(layer.selectedFeature);
        }
        for (var i = 0; i < features.length; i++) {
            var feature = features[i];
            if (this.startDate && this.endDate && feature.featureDate) {
                feature.featureVisibleDate = this.startDate.getTime() <= feature.featureDate.getTime() &&
                    feature.featureDate.getTime() <= this.endDate.getTime();
            } else {
                feature.featureVisibleDate = true;
            }
        }
        this.setFeatureVisibility(layer);
        this.initDates(layer);
    }
    theMap.dateFeatureSelect = function(feature) {
        var tick = this.getFeatureTick(feature);
        tick.css("background-color", this.tickSelectColor);
        tick.css("zIndex", "100");
    }
    theMap.dateFeatureOver = function(feature) {
        var tick = this.getFeatureTick(feature);
        tick.css("background-color", this.tickHoverColor);
        tick.css("zIndex", "100");
        //In case some aren't closed
        this.getFeatureTick(null).tooltip("close");
        tick.tooltip("open");
    }
    theMap.dateFeatureOut = function(feature) {
        var tick = this.getFeatureTick(feature);
        if (feature && (feature == this.startFeature || feature == this.endFeature)) {
            tick.css("background-color", this.tickSelectColor);
            tick.css("zIndex", "0");
        } else {
            tick.css("background-color", "");
            tick.css("zIndex", "0");
        }
        tick.tooltip("close");
    }
    theMap.getFeatureTick = function(feature) {
        if (!feature)
            return $("#" + this.mapDivId + "_animation_ticks" + " .ramadda-map-animation-tick");
        return $("#" + this.mapDivId + "_tick" + feature.dateIndex);
    }
    theMap.clearDateFeature = function(feature) {
        var element = feature != null ? $("#" + this.mapDivId + "_tick" + feature.dateIndex) : $("#" + this.mapDivId + "_animation_ticks .ramadda-map-animation-tick");
        //        console.log("clear date:" +(feature!=null?feature.dateIndex:"NA")+ " " + element.size());
        element.css("background-color", "");
        element.css("zIndex", "0");
    }


    theMap.initMapVectorLayer = function(layer, canSelect, selectCallback, unselectCallback, loadCallback) {
        var _this = this;
        this.showLoadingImage();
        layer.isMapLayer = true;
        layer.canSelect = canSelect;
        this.loadedLayers.push(layer);
        layer.events.on({
            "loadend": function(e) {
                _this.hideLoadingImage();
                if (e.response && Utils.isDefined(e.response.code) && e.response.code == OpenLayers.Protocol.Response.FAILURE) {
                    console.log("An error occurred loading the map");
                    return;
                }
                if (_this.centerOnMarkersCalled) {
                    _this.centerOnMarkers(_this.dfltBounds, _this.centerOnMarkersForce);
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
    }

    theMap.addGeoJsonLayer = function(name, url, canSelect, selectCallback, unselectCallback, args, loadCallback) {
        var layer = new OpenLayers.Layer.Vector(name, {
            projection: this.displayProjection,
            strategies: [new OpenLayers.Strategy.Fixed()],
            protocol: new OpenLayers.Protocol.HTTP({
                url: url,
                format: new OpenLayers.Format.GeoJSON({})
            }),
            //xxstyleMap: this.getVectorLayerStyleMap(args)
        });
        layer.styleMap = this.getVectorLayerStyleMap(layer, args);
        this.initMapVectorLayer(layer, canSelect, selectCallback, unselectCallback, loadCallback);
        return layer;
    }

    theMap.addKMLLayer = function(name, url, canSelect, selectCallback, unselectCallback, args, loadCallback) {
        var layer = new OpenLayers.Layer.Vector(name, {
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
        layer.styleMap = this.getVectorLayerStyleMap(layer, args);
        this.initMapVectorLayer(layer, canSelect, selectCallback, unselectCallback, loadCallback);
        return layer;
    }

    theMap.createXYZLayer = function(name, url, attribution) {
        var options = {
            sphericalMercator: mapDefaults.doSphericalMercator,
            numZoomLevels: mapDefaults.zoomLevels,
            wrapDateLine: mapDefaults.wrapDateline
        };
        if (attribution)
            options.attribution = attribution;
        return new OpenLayers.Layer.XYZ(name, url, options);
    }

    theMap.addBaseLayers = function() {
        if (!this.mapLayers) {
            this.mapLayers = [
                map_osm,
                map_esri_topo,
                map_esri_street,
                map_opentopo,
                map_usfs_ownership,
                map_usgs_topo,
                map_usgs_imagery,
                map_usgs_relief,
                map_osm_toner,
                map_watercolor,
                map_white,
                map_gray,
                map_blue,
                map_black

            ];
        }
        var dflt = this.defaultMapLayer || map_osm;
        if (!this.haveAddedDefaultLayer && dflt) {
            var index = this.mapLayers.indexOf(dflt);
            if (index >= 0) {
                this.mapLayers.splice(index, 1);
                this.mapLayers.splice(0, 0, dflt);
            }
        }

        this.firstLayer = null;
        this.hybridLayer = null;
        this.defaultOLMapLayer = null;


        for (i = 0; i < this.mapLayers.length; i++) {
            mapLayer = this.mapLayers[i];
            if (mapLayer == null) {
                continue;
            }
            var newLayer = null;
            if (mapLayer == map_osm) {
                urls = [
                    '//a.tile.openstreetmap.org/${z}/${x}/${y}.png',
                    '//b.tile.openstreetmap.org/${z}/${x}/${y}.png',
                    '//c.tile.openstreetmap.org/${z}/${x}/${y}.png'
                ];
                newLayer = new OpenLayers.Layer.OSM("Open Street Map", urls);
            } else if (mapLayer == map_osm_toner) {
                urls = ["http://a.tile.stamen.com/toner/${z}/${x}/${y}.png"];
                newLayer = new OpenLayers.Layer.OSM("OSM-Toner", urls);
            } else if (mapLayer == map_watercolor) {
                urls = ["http://c.tile.stamen.com/watercolor/${z}/${x}/${y}.jpg"];
                newLayer = new OpenLayers.Layer.OSM("Watercolor", urls);

            } else if (mapLayer == map_opentopo) {
                newLayer = this.createXYZLayer("OpenTopo", "//a.tile.opentopomap.org/${z}/${x}/${y}.png}");
            } else if (mapLayer == map_esri_worldimagery) {
                //Not working
                newLayer = this.createXYZLayer("ESRI World Imagery",
                    "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}");
            } else if (mapLayer == map_white) {
                this.addImageLayer(map_white, "White Background", "", ramaddaBaseUrl + "/images/white.png", false, 90, -180, -90, 180, 50, 50, {
                    isBaseLayer: true
                });
                continue;
            } else if (mapLayer == map_gray) {
                this.addImageLayer(map_gray, "Gray Background", "", ramaddaBaseUrl + "/images/gray.png", false, 90, -180, -90, 180, 50, 50, {
                    isBaseLayer: true
                });
                continue;
            } else if (mapLayer == map_blue) {
                this.addImageLayer(map_blue, "Blue Background", "", ramaddaBaseUrl + "/images/blue.png", false, 90, -180, -90, 180, 50, 50, {
                    isBaseLayer: true
                });
                continue;
            } else if (mapLayer == map_black) {
                this.addImageLayer(map_black, "Black Background", "", ramaddaBaseUrl + "/images/black.png", false, 90, -180, -90, 180, 50, 50, {
                    isBaseLayer: true
                });
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
            } else if (mapLayer == map_esri_topo) {
                newLayer = this.createXYZLayer("ESRI Topo",
                    "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/${z}/${y}/${x}");
            } else if (mapLayer == map_usfs_ownership) {
                newLayer = this.createXYZLayer("USFS Ownership",
                    "https://apps.fs.usda.gov/arcx/rest/services/wo_nfs_gstc/GSTC_TravelAccessBasemap_01/MapServer/tile/${z}/${y}/${x}");
            } else if (mapLayer == map_esri_street) {
                newLayer = this.createXYZLayer("ESRI Streets",
                    "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/${z}/${y}/${x}");
            } else if (/\/tile\//.exec(mapLayer)) {
                var layerURL = mapLayer;
                newLayer = new OpenLayers.Layer.XYZ(
                    "ESRI China Map", layerURL, {
                        sphericalMercator: mapDefaults.doSphericalMercator,
                        numZoomLevels: mapDefaults.zoomLevels,
                        wrapDateLine: mapDefaults.wrapDateline
                    });
            } else if (mapLayer == map_ms_shaded) {
                newLayer = new OpenLayers.Layer.VirtualEarth(
                    "Virtual Earth - Shaded", {
                        'type': VEMapStyle.Shaded,
                        sphericalMercator: mapDefaults.doSphericalMercator,
                        wrapDateLine: mapDefaults.wrapDateline,
                        numZoomLevels: mapDefaults.zoomLevels
                    });
            } else if (mapLayer == map_ms_hybrid) {
                newLayer = new OpenLayers.Layer.VirtualEarth(
                    "Virtual Earth - Hybrid", {
                        'type': VEMapStyle.Hybrid,
                        sphericalMercator: mapDefaults.doSphericalMercator,
                        numZoomLevels: mapDefaults.zoomLevels,
                        wrapDateLine: mapDefaults.wrapDateline
                    });
            } else if (mapLayer == map_ms_aerial) {
                newLayer = new OpenLayers.Layer.VirtualEarth(
                    "Virtual Earth - Aerial", {
                        'type': VEMapStyle.Aerial,
                        sphericalMercator: mapDefaults.doSphericalMercator,
                        numZoomLevels: mapDefaults.zoomLevels,
                        wrapDateLine: mapDefaults.wrapDateline
                    });
            } else {
                var match = /wms:(.*),(.*),(.*)/.exec(mapLayer);
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
                newLayer.ramaddaId = mapLayer;
                if (mapLayer == this.defaultMapLayer) {
                    this.defaultOLMapLayer = newLayer;
                }
                this.addLayer(newLayer);
            }

        }

        this.graticule = new OpenLayers.Control.Graticule({
            layerName: "Lat/Lon Lines",
            numPoints: 2,
            labelled: true,
            visible: false
        });
        this.map.addControl(this.graticule);

        return;
    }



    theMap.initSearch = function(inputId) {
        $("#" + inputId).keyup(function(e) {
            if (e.keyCode == 13) {
                theMap.searchMarkers($("#" + inputId).val());
            }
        });
    }

    theMap.searchMarkers = function(text) {
        text = text.trim();
        text = text.toLowerCase();
        var all = text == "";
        var cbxall = $(':input[id*=\"' + "visibleall_" + this.mapId + '\"]');
        cbxall.prop('checked', all);
        if (this.markers) {
            var list = this.getMarkers();
            for (var idx = 0; idx < list.length; idx++) {
                marker = list[idx];
                var visible = true;
                var cbx = $('#' + "visible_" + this.mapId + "_" + marker.ramaddaId);
                var name = marker.name;
                if (all) visible = true;
                else if (!Utils.isDefined(name)) {
                    visible = false;
                } else {
                    visible = name.toLowerCase().includes(text);
                }
                if (visible) {
                    marker.style.display = 'inline';
                } else {
                    marker.style.display = 'none';
                }
                cbx.prop('checked', visible);
                //            marker.display(visible);
            }
            this.markers.redraw();
        }
        if (this.boxes && this.boxes.markers) {
            for (var marker in this.boxes.markers) {
                marker = this.boxes.markers[marker];
                name = marker.name;
                var visible = true;
                if (all) visible = true;
                else if (!Utils.isDefined(name)) {
                    visible = false;
                } else {
                    visible = name.toLowerCase().includes(text);
                }
                marker.display(visible);
            }
            this.boxes.redraw();
        }

        if (this.lines) {
            var features = this.lines.features;
            for (var i = 0; i < features.length; i++) {
                var line = features[i];
                name = line.name;
                var visible = true;
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
    }

    theMap.setLatLonReadout = function(llr) {
        this.latlonReadout = llr;
    }

    theMap.getMap = function() {
        return this.map;
    }
    theMap.initMap = function(doRegion) {
        this.startTime = Date.now();
        if (this.inited)
            return;
        this.inited = true;
        let theMap = this;

        if (this.enableDragPan) {
            this.map.addControl(new OpenLayers.Control.Navigation({
                zoomWheelEnabled: this.scrollToZoom,
                dragPanOptions: {
                    enableKinetic: true
                }
            }));
        }

        /*this.map.addControl(new OpenLayers.Control.TouchNavigation({
            dragPanOptions: {
                enableKinetic: true
            }
        }));*/


        if (this.showZoomPanControl && !this.showZoomOnlyControl) {
            this.map.addControl(new OpenLayers.Control.PanZoom());
        }
        if (this.showZoomOnlyControl && !this.showZoomPanControl) {
            this.map.addControl(new OpenLayers.Control.Zoom());
        }

        if (this.showScaleLine) {
            this.map.addControl(new OpenLayers.Control.ScaleLine());
        }
        //        this.map.addControl(new OpenLayers.Control.OverviewMap());

        var keyboardControl = new OpenLayers.Control();
        var control = new OpenLayers.Control();
        var callbacks = {
            keydown: function(evt) {
                if (evt.keyCode == 79) {
                    if (!theMap.imageLayers) return;
                    for (id in theMap.imageLayers) {
                        image = theMap.imageLayers[id];
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
                    if (theMap.selectImage) {
                        theMap.selectImage.setVisibility(!theMap.selectImage.getVisibility());
                    }
                }
            }

        };
        var handler = new OpenLayers.Handler.Keyboard(control, callbacks, {});
        handler.activate();
        this.map.addControl(keyboardControl);
        this.map.addControl(new OpenLayers.Control.KeyboardDefaults());

        if (this.showLayerSwitcher) {
            this.map.addControl(new OpenLayers.Control.LayerSwitcher());
        }

        if (this.showLatLonPosition) {
            var latLonReadout = GuiUtils.getDomObject(this.latlonReadout);
            if (latLonReadout) {
                this.map.addControl(new OpenLayers.Control.MousePosition({
                    numDigits: 3,
                    element: latLonReadout.obj,
                    prefix: "Position: "
                }));
            } else {
                this.map.addControl(new OpenLayers.Control.MousePosition({
                    numDigits: 3,
                    prefix: "Position: "
                }));
            }
        }


        if (this.initialBoxes) {
            this.initBoxes(this.initialBoxes);
            this.initialBoxes = null;
        }

        //        this.defaultLocation =  createLonLat(-105,40);
        if (this.defaultLocation && !this.defaultBounds) {
            var center = this.defaultLocation;
            var offset = 10.0;
            this.defaultBounds = createBounds(center.lon - offset, center.lat - offset, center.lon + offset, center.lat + offset);
            this.defaultLocation = null;
        }

        if (this.defaultBounds) {
            var llPoint = this.defaultBounds.getCenterLonLat();
            var projPoint = this.transformLLPoint(llPoint);
            this.map.setCenter(projPoint);
            this.map.zoomToExtent(this.transformLLBounds(this.defaultBounds));
            this.defaultBounds = null;
        } else {
            this.map.zoomToMaxExtent();
        }

        for (var i = 0; i < this.initialLayers.length; i++) {
            this.addLayer(this.initialLayers[i]);
        }
        this.initialLayers = [];

        if (doRegion) {
            this.addRegionSelectorControl();
        }

        var cbx = $(':input[id*=\"' + "visible_" + this.mapId + '\"]');
        var _this = this;
        cbx.change(function(event) {
            _this.checkImageLayerVisibility();
            _this.checkMarkerVisibility();
            _this.checkLinesVisibility();
            _this.checkBoxesVisibility();
        });

        var cbxall = $(':input[id*=\"' + "visibleall_" + this.mapId + '\"]');
        cbxall.change(function(event) {
            cbx.prop("checked", cbxall.is(':checked'));
            _this.checkImageLayerVisibility();
            _this.checkMarkerVisibility();
            _this.checkLinesVisibility();
            _this.checkBoxesVisibility();

            //                cbx.prop("checked", cbxall.is(':checked')).trigger("change");
        });
    }


    theMap.addVectorLayer = function(layer, canSelect) {
        this.addLayer(layer);
        if (this.getCanSelect(canSelect)) {
            this.vectorLayers.push(layer);
            var _this = this;
            if (!this.map.featureSelect) {
                this.map.featureSelect = new OpenLayers.Control.SelectFeature(layer, {
                    multiple: false,
                    hover: this.selectOnHover,
                    onSelect: function(feature) {
                        _this.showMarkerPopup(feature, true);
                    }
                });
                /*
                if (this.highlightOnHover) {
                    this.map.highlightSelect = new OpenLayers.Control.SelectFeature(layer, {
                            multiple: false, 
                            hover: true,
                            highlightOnly: true,
                            renderIntent: "temporary"
                        });
                    this.map.addControl(this.map.highlightSelect);
                    this.map.highlightSelect.activate();   
                    }*/

                //for now
                //this.map.addControl(this.map.featureSelect);
                //                this.map.featureSelect.activate();
            } else {
                this.map.featureSelect.setLayer(this.vectorLayers);
                /*
                  if(this.map.highlightSelect) {
                    this.map.highlightSelect.setLayer(this.vectorLayers);
                }
                */
            }
        }
    }

    theMap.isLayerVisible = function(id, parentId) {
        //        var cbx =   $(':input[id*=\"' + "visible_" + this.mapId +"_" + id+'\"]');
        var cbx = $('#' + "visible_" + this.mapId + "_" + id);
        if (cbx.size() == 0 && parentId != null) cbx = $('#' + "visible_" + this.mapId + "_" + parentId);
        if (cbx.size() == 0) return true;
        return cbx.is(':checked');
    }

    theMap.initForDrawing = function() {
        var theMap = this;
        if (!theMap.drawingLayer) {
            this.drawingLayer = new OpenLayers.Layer.Vector("Drawing");
            this.addLayer(theMap.drawingLayer);
        }
        this.drawControl = new OpenLayers.Control.DrawFeature(
            theMap.drawingLayer, OpenLayers.Handler.Point);
        // theMap.drawControl.activate();
        this.map.addControl(theMap.drawControl);
    }

    theMap.drawingFeatureAdded = function(feature) {
        // alert(feature);
    }

    theMap.addClickHandler = function(lonfld, latfld, zoomfld, object) {
        this.lonFldId = lonfld;
        this.latFldId = latfld;
        if (this.clickHandler)
            return;
        if (!this.map)
            return;
        this.clickHandler = new OpenLayers.Control.Click();
        this.clickHandler.setLatLonZoomFld(lonfld, latfld, zoomfld, object);
        this.clickHandler.setTheMap(this);
        this.map.addControl(this.clickHandler);
        this.clickHandler.activate();
    }

    theMap.setSelection = function(argBase, doRegion, absolute) {
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
    }




    theMap.selectionPopupInit = function() {
        if (!this.inited) {
            this.initMap(this.selectRegion);
            if (this.argBase && !this.fldNorth) {
                this.setSelection(this.argBase);
            }

            if (this.fldNorth) {
                // alert("north = " + this.fldNorth.obj.value);
                this.setSelectionBox(this.fldNorth.obj.value,
                    this.fldWest.obj.value, this.fldSouth.obj.value,
                    this.fldEast.obj.value, true);
            }

            if (this.fldLon) {
                this.addClickHandler(this.fldLon.id, this.fldLat.id);
                this.setSelectionMarker(this.fldLon.obj.value, this.fldLat.obj.value);
            }
        }
    }

    theMap.setSelectionBoxFromFields = function(zoom) {
        if (this.fldNorth) {
            // alert("north = " + this.fldNorth.obj.value);
            this.setSelectionBox(this.fldNorth.obj.value,
                this.fldWest.obj.value, this.fldSouth.obj.value,
                this.fldEast.obj.value, true);
            if (this.selectorBox) {
                var boxBounds = this.selectorBox.bounds
                this.map.setCenter(boxBounds.getCenterLonLat());
                if (zoom) {
                    this.map.zoomToExtent(boxBounds);
                }
            }
        }
    }

    theMap.toggleSelectorBox = function(toggle) {
        if (this.selectorControl) {
            if (toggle) {
                this.selectorControl.activate();
                this.selectorControl.box.activate();
            } else {
                this.selectorControl.deactivate();
                this.selectorControl.box.deactivate();
            }
        }
    }

    theMap.resetExtent = function() {
        this.map.zoomToMaxExtent();
    }

    // Assume that north, south, east, and west are in degrees or
    // some variant thereof
    theMap.setSelectionBox = function(north, west, south, east, centerView) {
        if (north == "" || west == "" || south == "" || east == "")
            return;
        var bounds = createBounds(west, Math.max(south,
            -mapDefaults.maxLatValue), east, Math.min(north, mapDefaults.maxLatValue));
        if (!this.selectorBox) {
            var args = {
                "color": "red",
                "selectable": false
            };
            this.selectorBox = this.createBox("", "Selector box", north, west, south, east, "", args);
        } else {
            this.selectorBox.bounds = this.transformLLBounds(bounds);
            // this.selectorBox.bounds = bounds;
        }
        if (centerView) {
            this.setViewToBounds(bounds)
        }

        if (this.boxes) {
            this.boxes.redraw();
        }

        if (theMap.selectImage) {
            var imageBounds = createBounds(west, south, east, north);
            imageBounds = theMap.transformLLBounds(imageBounds);
            theMap.selectImage.extent = imageBounds;
            theMap.selectImage.redraw();
        }

    }

    theMap.clearSelectionMarker = function() {
        if (this.selectorMarker != null) {
            this.removeMarker(this.selectorMarker);
            this.selectorMarker = null;
        }
    }

    theMap.clearSelectedFeatures = function() {
        if (this.map.controls != null) {
            var myControls = this.map.controls;
            for (i = 0; i < myControls.length; i++) {
                if (myControls[i].displayClass == "olControlSelectFeature") {
                    myControls[i].unselectAll();
                }
            }
        }
    }

    theMap.setSelectionMarker = function(lon, lat, andCenter, zoom) {
        if (!lon || !lat || lon == "" || lat == "")
            return;
        if (this.lonFldId != null) {
            $("#" + this.lonFldId).val(formatLocationValue(lon));
            $("#" + this.latFldId).val(formatLocationValue(lat));
        }


        var lonlat = new createLonLat(lon, lat);
        if (this.selectorMarker == null) {
            this.selectorMarker = this.addMarker(positionMarkerID, lonlat, "", "", "", 20, 10);
        } else {
            this.selectorMarker.lonlat = this.transformLLPoint(lonlat);
        }
        if (this.markers) {
            this.markers.redraw();
        }
        if (andCenter) {
            this.map.setCenter(this.selectorMarker.lonlat);
        }
        if (zoom) {
            if (zoom.zoomOut) {
                var level = this.map.getZoom();
                level--;
                if (this.map.isValidZoomLevel(level)) {
                    this.map.zoomTo(level);
                }
                return;
            }
            if (zoom.zoomIn) {
                var level = this.map.getZoom();
                level++;
                if (this.map.isValidZoomLevel(level)) {
                    this.map.zoomTo(level);
                }
                return;
            }

            var offset = zoom.offset;
            if (offset) {
                var bounds = this.transformLLBounds(createBounds(lon - offset, lat - offset, lon + offset, lat + offset));
                this.map.zoomToExtent(bounds);
            }
        }
    }

    theMap.transformLLBounds = function(bounds) {
        if (!bounds)
            return;
        var llbounds = bounds.clone();
        return llbounds.transform(this.displayProjection, this.sourceProjection);
    }

    theMap.transformLLPoint = function(point) {
        if (!point)
            return null;
        var llpoint = point.clone();
        return llpoint.transform(this.displayProjection, this.sourceProjection);
    }

    theMap.transformProjBounds = function(bounds) {
        if (!bounds)
            return;
        var projbounds = bounds.clone();
        return projbounds.transform(this.sourceProjection, this.displayProjection);
    }

    theMap.transformProjPoint = function(point) {
        if (!point)
            return;
        var projpoint = point.clone();
        return projpoint.transform(this.sourceProjection, this.displayProjection);
    }

    theMap.normalizeBounds = function(bounds) {
        if (!this.map) {
            return bounds;
        }
        var newBounds = bounds;
        var newLeft = bounds.left;
        var newRight = bounds.right;
        var extentBounds = this.map.restrictedExtent;
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
        newBounds = createBounds(newLeft, bounds.bottom, newRight,
            bounds.top);
        return newBounds;
    }

    theMap.findSelectionFields = function() {
        if (this.argBase && !(this.fldNorth || this.fldLon)) {
            this.setSelection(this.argBase);
        }
    }

    theMap.selectionClear = function() {
        this.findSelectionFields();
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
    }

    theMap.getMarkers = function() {
            if (this.markers == null) return [];
            return this.markers.features;
        },
        theMap.clearRegionSelector = function(listener) {
            if (!this.selectorControl)
                return;
            if (this.selectorControl.box) {
                //Nothing here really works to hide the box
                this.selectorControl.box.removeBox();
            }
        },
        theMap.addRegionSelectorControl = function(listener) {
            var theMap = this;
            if (theMap.selectorControl)
                return;
            theMap.selectorListener = listener;

            theMap.selectorControl = new OpenLayers.Control();
            OpenLayers.Util.extend(theMap.selectorControl, {
                draw: function() {
                    this.box = new OpenLayers.Handler.Box(theMap.selectorControl, {
                        "done": this.notice
                    }, {
                        keyMask: OpenLayers.Handler.MOD_SHIFT,
                        xxxboxDivClassName: "map-drag-box"
                    });
                    this.box.activate();
                },

                notice: function(bounds) {
                    var ll = this.map.getLonLatFromPixel(new OpenLayers.Pixel(
                        bounds.left, bounds.bottom));
                    var ur = this.map.getLonLatFromPixel(new OpenLayers.Pixel(
                        bounds.right, bounds.top));
                    ll = theMap.transformProjPoint(ll);
                    ur = theMap.transformProjPoint(ur);
                    var bounds = createBounds(ll.lon, ll.lat, ur.lon,
                        ur.lat);
                    bounds = theMap.normalizeBounds(bounds);
                    theMap.setSelectionBox(bounds.top, bounds.left, bounds.bottom, bounds.right, false);
                    theMap.findSelectionFields();
                    if (listener) {
                        listener(bounds);
                    }
                    if (theMap.fldNorth) {
                        theMap.fldNorth.obj.value = formatLocationValue(bounds.top);
                        theMap.fldSouth.obj.value = formatLocationValue(bounds.bottom);
                        theMap.fldWest.obj.value = formatLocationValue(bounds.left);
                        theMap.fldEast.obj.value = formatLocationValue(bounds.right);
                    }
                }
            });
            theMap.map.addControl(theMap.selectorControl);


            theMap.panControl = new OpenLayers.Control();
            OpenLayers.Util.extend(theMap.panControl, {
                draw: function() {
                    this.box = new OpenLayers.Handler.Drag(theMap.panControl, {
                        "down": this.down,
                        "move": this.move
                    }, {
                        keyMask: OpenLayers.Handler.MOD_META,
                        boxDivClassName: "map-drag-box"
                    });
                    this.box.activate();
                },

                down: function(pt) {
                    this.firstPoint = this.map.getLonLatFromPixel(new OpenLayers.Pixel(
                        pt.x, pt.y));
                    this.firstPoint = theMap.transformProjPoint(this.firstPoint);
                    theMap.findSelectionFields();
                    if (theMap.fldNorth) {
                        n = this.origNorth = parseFloat(theMap.fldNorth.obj.value);
                        s = this.origSouth = parseFloat(theMap.fldSouth.obj.value);
                        w = this.origWest = parseFloat(theMap.fldWest.obj.value);
                        e = this.origEast = parseFloat(theMap.fldEast.obj.value);
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
                    var ll = this.map.getLonLatFromPixel(new OpenLayers.Pixel(pt.x, pt.y));
                    ll = theMap.transformProjPoint(ll);
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
                    var bounds = createBounds(newWest, newSouth, newEast, newNorth);
                    bounds = theMap.normalizeBounds(bounds);
                    theMap.setSelectionBox(bounds.top, bounds.left, bounds.bottom, bounds.right, false);
                    theMap.findSelectionFields();
                    if (!theMap.fldNorth) return;
                    theMap.fldNorth.obj.value = bounds.top;
                    theMap.fldSouth.obj.value = bounds.bottom;
                    theMap.fldWest.obj.value = bounds.left;
                    theMap.fldEast.obj.value = bounds.right;

                }
            });
            theMap.map.addControl(theMap.panControl);
        }

    theMap.onPopupClose = function(evt) {
        if (this.currentPopup) {
            this.map.removePopup(this.currentPopup);
            this.currentPopup.destroy();
            this.currentPopup = null;
            this.hiliteBox('');
            if (this.selectedFeature) {
                this.unselectFeature(this.selectedFeature);
            }
        }
    }

    theMap.findObject = function(id, array) {
        for (i = 0; i < array.length; i++) {
            var aid = array[i].ramaddaId;
            if (!aid)
                array[i].id;
            if (aid == id) {
                return array[i];
            }
        }
        return null;
    }

    theMap.findMarker = function(id) {
        if (!this.markers) {
            return null;
        }
        return this.findObject(id, this.getMarkers());
    }

    theMap.findFeature = function(id) {
        return this.features[id];
    }

    theMap.findBox = function(id) {
        if (!this.boxes) {
            return null;
        }
        return this.findObject(id, this.boxes.markers);
    }

    theMap.hiliteBox = function(id) {
        if (this.currentBox) {
            this.currentBox.setBorder("blue");
        }
        this.currentBox = this.findBox(id);
        if (this.currentBox) {
            this.currentBox.setBorder("red");
        }
    }
    theMap.checkMarkerVisibility = function() {
        if (!this.markers) return;
        var list = this.getMarkers();
        for (var idx = 0; idx < list.length; idx++) {
            marker = list[idx];
            var visible = this.isLayerVisible(marker.ramaddaId, marker.parentId);
            //            console.log("   visible:" + visible +" " + marker.ramaddaId + " " + marker.parentId);
            if (visible) {
                marker.style.display = 'inline';
            } else {
                marker.style.display = 'none';
            }
            //            marker.display(visible);
        }
        this.markers.redraw();
    }

    theMap.checkLinesVisibility = function() {
        if (!this.lines) return;
        var features = this.lines.features;
        for (var i = 0; i < features.length; i++) {
            var line = features[i];
            var visible = this.isLayerVisible(line.ramaddaId);
            if (visible) {
                line.style.display = 'inline';
            } else {
                line.style.display = 'none';
            }

        }
        this.lines.redraw();

    }


    theMap.checkBoxesVisibility = function() {
        if (!this.boxes) return;
        for (var marker in this.boxes.markers) {
            marker = this.boxes.markers[marker];
            var visible = this.isLayerVisible(marker.ramaddaId);
            marker.display(visible);
        }
        this.boxes.redraw();
    }



    theMap.checkImageLayerVisibility = function() {
        if (!this.imageLayers) return;
        for (var i in this.imageLayers) {
            var visible = this.isLayerVisible(i);
            var image = this.imageLayers[i];
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
    }

    theMap.hiliteMarker = function(id) {
        var mymarker = this.findMarker(id);
        if (!mymarker) {
            mymarker = this.findFeature(id);
        }

        if (!mymarker && this.imageLayers) {
            mymarker = this.imageLayers[id];
        }

        if (!mymarker) {
            return;
        }
        this.map.setCenter(mymarker.lonlat);
        this.showMarkerPopup(mymarker);
    }


    theMap.hideLoadingImage = function() {
        if (this.loadingImage) {
            this.loadingImage.style.visibility = "hidden";
        }
    }

    theMap.showLoadingImage = function() {
        if (this.loadingImage) {
            this.loadingImage.style.visibility = "inline";
            return;
        }
        sz = new OpenLayers.Size();
        sz.h = 120;
        sz.w = 120;
        width = this.map.viewPortDiv.offsetWidth;
        height = this.map.viewPortDiv.offsetHeight;
        position = new OpenLayers.Pixel(width / 2 - sz.w / 2, height / 2 - sz.h / 2);
        this.loadingImage = OpenLayers.Util.createImage("loadingimage",
            position,
            sz,
            ramaddaBaseUrl + '/icons/mapprogress.gif');
        this.loadingImage.style.zIndex = 1010;
        this.map.viewPortDiv.appendChild(this.loadingImage);
    }


    // bounds are in lat/lon
    theMap.centerOnMarkers = function(dfltBounds, force) {
        this.centerOnMarkersCalled = true;
        this.centerOnMarkersForce = force;
        now = Date.now();
        //        console.log("center on markers:" + ((now-this.startTime)/1000));
        var bounds = null;
        // bounds = this.boxes.getDataExtent();
        if (dfltBounds) {
            if (dfltBounds.left < -180 || dfltBounds.left > 180 ||
                dfltBounds.right < -180 || dfltBounds.right > 180 ||
                dfltBounds.bottom < -90 || dfltBounds.bottom > 90 ||
                dfltBounds.top < -90 || dfltBounds.top > 90) {
                dfltBounds = createBounds(-180, -90, 180, 90);
            }
        }
        this.dfltBounds = dfltBounds;
        //        if (!bounds) {
        if (!force) {
            if (this.markers) {
                // markers are in projection coordinates
                var dataBounds = this.markers.getDataExtent();
                bounds = this.transformProjBounds(dataBounds);
            }
            if (this.lines) {
                var dataBounds = this.lines.getDataExtent();
                var fromLine = this.transformProjBounds(dataBounds);
                if (bounds)
                    bounds.extend(fromLine);
                else
                    bounds = fromLine;
            }
            for (var layer in this.getMap().layers) {
                layer = this.getMap().layers[layer];
                if (!layer.getDataExtent) continue;
                if (layer.isBaseLayer || !layer.getVisibility()) continue;
                var dataBounds = layer.getDataExtent();
                if (dataBounds) {
                    var latlon = this.transformProjBounds(dataBounds);
                    if (bounds)
                        bounds.extend(latlon);
                    else
                        bounds = latlon;
                }
            }
        }

        //        }
        //        console.log("bounds:" +bounds);

        if (!bounds) {
            bounds = dfltBounds;
        }
        if (!bounds) {
            return;
        }

        if (!this.getMap()) {
            this.defaultBounds = bounds;
            return;
        }
        if (bounds.getHeight() > 160) {
            bounds.top = 80;
            bounds.bottom = -80;
        }
        this.setViewToBounds(bounds);
    }

    theMap.setViewToBounds = function(bounds) {
        projBounds = this.transformLLBounds(bounds);
        if (projBounds.getWidth() == 0) {
            this.getMap().zoomTo(this.initialZoom);
        } else {
            this.getMap().zoomToExtent(projBounds);
        }
        this.getMap().setCenter(projBounds.getCenterLonLat());

    }

    theMap.setCenter = function(latLonPoint) {
        var projPoint = this.transformLLPoint(latLonPoint);
        this.getMap().setCenter(projPoint);
    }


    theMap.zoomToMarkers = function() {
        if (!this.markers)
            return;
        bounds = this.markers.getDataExtent();
        if (bounds == null) return;
        this.getMap().setCenter(bounds.getCenterLonLat());
        this.getMap().zoomToExtent(bounds);
    }

    theMap.centerToMarkers = function() {
        if (!this.markers)
            return;
        bounds = this.markers.getDataExtent();
        this.getMap().setCenter(bounds.getCenterLonLat());
    }

    theMap.setInitialCenterAndZoom = function(lon, lat, zoomLevel) {
        this.defaultLocation = createLonLat(lon, lat);
        this.initialZoom = zoomLevel;
    }



    theMap.getPopupText = function(text, marker) {
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
    }

    theMap.seenMarkers = {};

    theMap.addMarker = function(id, location, iconUrl, markerName, text, parentId, size, voffset, canSelect) {
        if (size == null) size = 16;
        if (voffset == null) voffset = 0;

        if (!this.markers) {
            this.markers = new OpenLayers.Layer.Vector("Markers");
            this.addVectorLayer(this.markers, canSelect);
        }
        if (!iconUrl) {
            iconUrl = ramaddaBaseUrl + '/icons/marker.png';
        }
        var sz = new OpenLayers.Size(size, size);
        var calculateOffset = function(size) {
            return new OpenLayers.Pixel(-(size.w / 2), -(size.h / 2) - voffset);
        };

        var icon = new OpenLayers.Icon(iconUrl, sz, null, calculateOffset);
        var projPoint = this.transformLLPoint(location);
        var marker = new OpenLayers.Marker(projPoint, icon);
        var feature = new OpenLayers.Feature.Vector(
            new OpenLayers.Geometry.Point(location.lon, location.lat).transform(this.displayProjection, this.sourceProjection), {
                description: ''
            }, {
                externalGraphic: iconUrl,
                graphicHeight: size,
                graphicWidth: size,
                graphicXOffset: -size / 2,
                graphicYOffset: -size / 2
            });

        feature.ramaddaId = id;
        feature.parentId = parentId;
        feature.text = this.getPopupText(text, feature);
        feature.name = markerName;
        feature.location = location;

        marker.ramaddaId = id;
        marker.text = this.getPopupText(text, marker);
        marker.name = markerName;
        marker.location = location;
        var locationKey = location + "";
        feature.locationKey = locationKey;
        var seenMarkers = this.seenMarkers[locationKey];
        if (seenMarkers == null) {
            seenMarkers = [];
            this.seenMarkers[locationKey] = seenMarkers;
        } else {}
        seenMarkers.push(feature);
        //        console.log("loc:" + locationKey +" " + seenMarkers);


        var _this = this;
        var clickFunc = function(evt) {
            _this.showMarkerPopup(marker, true);
            if (marker.ramaddaClickHandler != null) {
                marker.ramaddaClickHandler.call(null, marker);
            }
            OpenLayers.Event.stop(evt);
        };

        marker.events.register('click', marker, clickFunc);
        marker.events.register('touchstart', marker, clickFunc);


        var visible = this.isLayerVisible(marker.ramaddaId, marker.parentId);
        if (!visible) marker.display(false);

        feature.what = "marker";
        this.markers.addFeatures([feature]);
        return feature;
    }



    theMap.initBoxes = function(theBoxes) {
        if (!this.getMap()) {
            // alert('whoa, no map');
        }
        this.addLayer(theBoxes);
        // Added this because I was getting an unknown method error
        theBoxes.getFeatureFromEvent = function(evt) {
            return null;
        };
        var sf = new OpenLayers.Control.SelectFeature(theBoxes);
        this.getMap().addControl(sf);
        sf.activate();
    }

    theMap.removeBox = function(box) {
        if (this.boxes && box) {
            this.boxes.removeMarker(box);
            this.boxes.redraw();
        }
    }

    theMap.addBox = function(box) {
        if (!this.boxes) {
            this.boxes = new OpenLayers.Layer.Boxes("Boxes", {
                wrapDateLine: mapDefaults.wrapDateline,
            });
            if (!this.getMap()) {
                this.initialBoxes = this.boxes;
            } else {
                this.initBoxes(this.boxes);
            }
        }
        this.boxes.addMarker(box);
        this.boxes.redraw();
    }


    theMap.createBox = function(id, name, north, west, south, east, text, params) {

        if (text.indexOf("base64:") == 0) {
            text = window.atob(text.substring(7));
        }

        var args = {
            "color": "blue",
            "selectable": true,
            "zoomToExtent": false,
            "sticky": false
        };

        for (var i in params) {
            args[i] = params[i];
        }

        var bounds = createBounds(west, Math.max(south, -mapDefaults.maxLatValue),
            east, Math.min(north, mapDefaults.maxLatValue));
        var projBounds = this.transformLLBounds(bounds);
        box = new OpenLayers.Marker.Box(projBounds);
        box.sticky = args.sticky;
        var theMap = this;

        if (args["selectable"]) {
            box.events.register("click", box, function(e) {
                theMap.showMarkerPopup(box, true);
                OpenLayers.Event.stop(e);
            });
        }

        var lonlat = new createLonLat(west, north);
        box.lonlat = this.transformLLPoint(lonlat);
        box.text = this.getPopupText(text);
        box.name = name;
        box.setBorder(args["color"], 1);
        box.ramaddaId = id;
        var attrs = {
            fillColor: "red",
            fillOpacity: 1.0,
            pointRadius: 5,
        };

        if (args["zoomToExtent"]) {
            this.centerOnMarkers(bounds);
        }
        this.addBox(box);
        return box;
    }


    theMap.circleMarker = function(id, attrs) {
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
        var _this = this;
        this.showFeatureText(marker);

        return this.addPoint(id + "_circle", marker.location, myattrs);
    }


    theMap.uncircleMarker = function(id) {
        feature = this.features[id + "_circle"];
        if (feature) {
            this.circles.removeFeatures([feature]);
        }
        this.hideFeatureText(feature);
    }

    theMap.showFeatureText = function(feature) {
            var _this = this;
            if (feature.text && this.displayDiv) {
                this.textFeature = feature;
                var callback = function() {
                    if (_this.textFeature == feature) {
                        _this.showText(_this.textFeature.text);
                    }
                }
                setTimeout(callback, 500);
            }
        },

        theMap.showText = function(text) {
            $("#" + this.displayDiv).html(text);
        }

    theMap.hideFeatureText = function(feature) {
            if (!feature || this.textFeature == feature) {
                this.showText("");
            }
        },


        theMap.addPoint = function(id, point, attrs, text, notReally) {
            //Check if we have a LonLat instead of a Point
            var location = point;
            if (typeof point.x === 'undefined') {
                point = new OpenLayers.Geometry.Point(point.lon, point.lat);
            } else {
                location = createLonLat(location.x, location.y);
            }

            var _this = this;

            var cstyle = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style['default']);
            $.extend(cstyle, {
                pointRadius: 5,
                stroke: true,
                strokeColor: "red",
                strokeWidth: 0,
                strokeOpacity: 0.75,
                fill: true,
                fillColor: "blue",
                fillOpacity: 0.75,
            });

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
            var center = new OpenLayers.Geometry.Point(point.x, point.y);
            center.transform(this.displayProjection, this.sourceProjection);
            var feature = new OpenLayers.Feature.Vector(center, null, cstyle);

            feature.center = center;
            feature.ramaddaId = id;
            feature.text = this.getPopupText(text, feature);
            feature.location = location;
            this.features[id] = feature;
            if (!notReally) {
                if (this.circles == null) {
                    this.circles = new OpenLayers.Layer.Vector("Circles Layer");
                    /*
                      this.circles.events.on({
                      'featureselected': function(feature) {
                      feature  = feature.feature;
                      _this.showMarkerPopup(feature);
                      },
                      'featureunselected': function(feature) {
                      }
                      });*/
                    this.addVectorLayer(this.circles);
                    /*
                      this.addLayer(this.circles);
                      var sf = new OpenLayers.Control.SelectFeature(this.circles,{
                      clickout: false, 
                      toggle: false,
                      multiple: false, 
                      hover: false,
                      toggleKey: "ctrlKey", 
                      multipleKey: "shiftKey", 
                      box: false
                      });

                      this.getMap().addControl(sf);
                      sf.activate();
                    */
                }

                this.circles.addFeatures([feature]);
            }
            return feature;
        }

    theMap.removePoint = function(point) {
        if (this.circles)
            this.circles.removeFeatures([point]);
    }

    theMap.addRectangle = function(id, north, west, south, east, attrs, info) {
        var points = [new OpenLayers.Geometry.Point(west, north),
            new OpenLayers.Geometry.Point(west, south),
            new OpenLayers.Geometry.Point(east, south),
            new OpenLayers.Geometry.Point(east, north),
            new OpenLayers.Geometry.Point(west, north)
        ];
        return this.addPolygon(id, "", points, attrs, info);
    }


    theMap.addLine = function(id, name, lat1, lon1, lat2, lon2, attrs, info) {
        var points = [new OpenLayers.Geometry.Point(lon1, lat1),
            new OpenLayers.Geometry.Point(lon2, lat2)
        ];
        return this.addPolygon(id, name, points, attrs, info);
    }

    theMap.addLines = function(id, name, attrs, values, info) {
        var points = [];
        for (var i = 0; i < values.length; i += 2) {
            points.push(new OpenLayers.Geometry.Point(values[i + 1], values[i]));
        }
        return this.addPolygon(id, name, points, attrs, info);
    }

    theMap.removePolygon = function(line) {
        if (this.lines) {
            //            this.lines.removeAllFeatures();
            this.lines.removeFeatures([line]);
        }
    }

    var cnt = 0;

    theMap.addPolygon = function(id, name, points, attrs, marker) {
        var _this = this;
        var location;
        if(points.length>1) {
            location = new OpenLayers.LonLat(points[0].x+(points[1].x-points[0].x)/2,
                                             points[0].y+(points[1].y-points[0].y)/2);
        } else  {
            location = new OpenLayers.LonLat(points[0].x, points[0].y);
        }
        for (var i = 0; i < points.length; i++) {
            points[i].transform(this.displayProjection, this.sourceProjection);
        }

        var base_style = OpenLayers.Util.extend({},
            OpenLayers.Feature.Vector.style['default']);
        var style = OpenLayers.Util.extend({}, base_style);
        style.strokeColor = "blue";
        style.strokeWidth = 1;
        if (attrs) {
            for (key in attrs) {
                style[key] = attrs[key];
            }
        }

        if (!this.lines) {
   
            this.lines = new OpenLayers.Layer.Vector("Lines", {
                style: base_style
            });
            this.addVectorLayer(this.lines);
        }

        var lineString = new OpenLayers.Geometry.LineString(points);
        var line = new OpenLayers.Feature.Vector(lineString, null, style);
        /*
         * line.events.register("click", line, function (e) { alert("box
         * click"); _this.showMarkerPopup(box); OpenLayers.Event.stop(evt); });
         */
        line.text =marker;
        line.ramaddaId = id;
        line.location  = location;
        line.name = name;
        var visible = this.isLayerVisible(line.ramaddaId);
        if (visible) {
            line.style.display = 'inline';
        } else {
            line.style.display = 'none';
        }

        this.lines.addFeatures([line]);
        return line;
    }

    theMap.showMarkerPopup = function(marker, fromClick) {
        if (this.entryClickHandler && window[this.entryClickHandler]) {
            if (!window[this.entryClickHandler](this, marker)) {
                return;
            }
        }

        if (this.currentPopup) {
            this.getMap().removePopup(this.currentPopup);
            this.currentPopup.destroy();
        }


        var id = marker.ramaddaId;
        if (!id)
            id = marker.id;
        this.hiliteBox(id);
        var theMap = this;
        if (marker.inputProps) {
            marker.text = this.getPopupText(marker.inputProps.text);
        }

        var markertext = marker.text;
        if (fromClick && marker.locationKey != null) {
            markers = this.seenMarkers[marker.locationKey];
            if (markers.length > 1) {
                markertext = "";
                for (var i = 0; i < markers.length; i++) {
                    otherMarker = markers[i];
                    if (i > 0)
                        markertext += '<hr>';
                    if (otherMarker.inputProps) {
                        otherMarker.text = this.getPopupText(otherMarker.inputProps.text);
                    }
                    markertext += otherMarker.text;
                    if (i > 10) break;
                }
            }
        }
        // set marker text as the location
        var location = marker.location;
        if (!location) return;
        if (typeof location.lon === 'undefined') {
            location = createLonLat(location.x, location.y);
        }
        if (!markertext || markertext == "") {
            //            marker.location = this.transformProjPoint(marker.lonlat);
            if (location.lat == location.lat) {
                markertext = "Lon: " + location.lat + "<br>" + "Lat: " + location.lon;
            }
        }


        var projPoint = this.transformLLPoint(location);
        popup = new OpenLayers.Popup.FramedCloud("popup", projPoint,
            null, markertext, null, true,
            function() {
                theMap.onPopupClose()
            });

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

    }

    theMap.popupChart = function(props) {
            var displayManager = getOrCreateDisplayManager(props.divId, {}, true);
            var pointDataProps = {
                entryId: props.entryId
            };
            var title = props.title;
            if (!title) title = "Chart";
            var chartProps = {
                "title": title,
                "layoutHere": true,
                "divid": props.divId,
                "entryId": props.entryId,
                "fields": props.fields,
                "vAxisMinValue": props.vAxisMinValue,
                "vAxisMaxValue": props.vAxisMaxValue,
                "data": new PointData(title, null, null, getRamadda().getRoot() + "/entry/show?entryid=" + props.entryId + "&output=points.product&product=points.json&numpoints=1000", pointDataProps)
            };
            displayManager.createDisplay(props.chartType, chartProps);
        },
        theMap.removeMarker = function(marker) {
            if (this.markers) {
                //            this.markers.removeMarker(marker);            
                this.markers.removeFeatures([marker]);
            }
        }

}

function formatLocationValue(value) {
    return number_format(value, 3, ".", "");
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
        lonFld = GuiUtils.getDomObject(this.lonFldId);
        latFld = GuiUtils.getDomObject(this.latFldId);
        zoomFld = GuiUtils.getDomObject(this.zoomFldId);
        if (latFld && lonFld) {
            latFld.obj.value = formatLocationValue(lonlat.lat);
            lonFld.obj.value = formatLocationValue(lonlat.lon);
        }
        if (zoomFld) {
            zoomFld.obj.value = this.theMap.getMap().getZoom();
        }
        //                this.theMap.setSelectionMarker(lonlat.lon, lonlat.lat);

        if (this.clickListener != null) {
            this.clickListener.handleClick(this, lonlat.lon, lonlat.lat);
        }


    }

});





var CUSTOM_MAP = "CUSTOM";
var firstCustom = true;

var MapUtils = {
    mapRegionSelected: function(selectId, baseId) {
        var value = $("#" + selectId).val();
        if (value == null) {
            console.log("Error: No map region value");
            return;
        }
        if (value == "") {
            this.toggleMapWidget(baseId, false);
            return;
        }
        var toks = value.split(",");

        if (toks.length == 1) {
            if (toks[0] != CUSTOM_MAP) {
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
            var mapVar = window[baseId];
            if (mapVar && !mapVar.inited) {
                mapVar.initMap(true);
            }
            $("#" + baseId + "_mapToggle").show();
        } else {
            $("#" + baseId + "_mapToggle").hide();
        }
    }

}


var markerMap = {};

function highlightMarkers(selector, mapVar, background1, background2, id) {
    $(selector).mouseenter(
        function() {
            if (background1)
                $(this).css('background', background1);
            if (!$(this).data('mapid'))
                return;
            if (mapVar.circleMarker($(this).data('mapid'), ramaddaCircleHiliteAttrs)) {
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