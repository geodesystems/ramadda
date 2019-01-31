/**
Copyright 2008-2018 Geode Systems LLC
*/

var DISPLAY_MAP = "map";

var displayMapMarkers = ["marker.png", "marker-blue.png","marker-gold.png","marker-green.png"];

var displayMapCurrentMarker =-1;
var displayMapUrlToVectorListeners = {};
var displayMapMarkerIcons = {};

function displayMapGetMarkerIcon() {
    displayMapCurrentMarker++;
    if(displayMapCurrentMarker>= displayMapMarkers.length)  displayMapCurrentMarker = 0;
    return  ramaddaBaseUrl + "/lib/openlayers/v2/img/" + displayMapMarkers[displayMapCurrentMarker];
}

addGlobalDisplayType({type : DISPLAY_MAP,label : "Map"});

function MapFeature(source, points) {
	RamaddaUtil.defineMembers(this, {
		source : source,
		points : points
	});
}




function RamaddaMapDisplay(displayManager, id, properties) {
	var ID_LATFIELD = "latfield";
	var ID_LONFIELD = "lonfield";
	var ID_MAP = "map";
	var ID_BOTTOM = "bottom";
	var SUPER;
	RamaddaUtil.defineMembers(this, {
                showLocationReadout: false,
                showBoxes:true,
                    showPercent: false,
                    percentFields: null,
                    kmlLayer:null,
                    kmlLayerName:"",
                    geojsonLayer:null,
                    geojsonLayerName:"",
                    theMap: null
            });

	RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id,
			DISPLAY_MAP, properties));
	addRamaddaDisplay(this);
	RamaddaUtil.defineMembers(this, {
		mapBoundsSet : false,
		features : [],
		myMarkers : {},
		mapEntryInfos : {},
		sourceToLine : {},
		sourceToPoints : {},
		snarf : true,
		initDisplay : function() {
                    this.createUI();
                    var _this = this;
                    var html = "";
                    var extraStyle = "min-height:200px;";
                    var width = this.getWidth();
                    if(Utils.isDefined(width)) {
                        if (width > 0) {
                            extraStyle += "width:" + width + "px; ";
                        } else if(width<0) {
                            extraStyle += "width:" + (-width)+"%;";
                        } else if(width !="") {
                            extraStyle += "width:" + width+";";
                        }
                    }

                    var height = this.getProperty("height", 300);
                    // var height = this.getProperty("height",-1);
                    if (height > 0) {
                        extraStyle += " height:" + height + "px; ";
                    } else if(height<0) {
                        extraStyle += " height:" + (-height) + "%; ";
                    }
                    

                    html += HtmlUtil.div([ ATTR_CLASS, "display-map-map", "style",
                                           extraStyle, ATTR_ID, this.getDomId(ID_MAP) ]);
                    html += HtmlUtil.div([ ATTR_CLASS, "",  ATTR_ID, this.getDomId(ID_BOTTOM) ]);

                    if(this.showLocationReadout) {
                        html += HtmlUtil.openTag(TAG_DIV, [ ATTR_CLASS,
                                                            "display-map-latlon" ]);
                        html += HtmlUtil.openTag("form");
                        html += "Latitude: "
                            + HtmlUtil.input(this.getDomId(ID_LATFIELD), "", [ "size",
                                                                               "7", ATTR_ID, this.getDomId(ID_LATFIELD) ]);
                        html += "  ";
                        html += "Longitude: "
                            + HtmlUtil.input(this.getDomId(ID_LONFIELD), "", [ "size",
                                                                               "7", ATTR_ID, this.getDomId(ID_LONFIELD) ]);
                        html += HtmlUtil.closeTag("form");
                        html += HtmlUtil.closeTag(TAG_DIV);
                    }
                    this.setContents(html);

                    if(!this.map) {
                        this.createMap();
                    } else  {
                        this.map.setMapDiv(this.getDomId(ID_MAP));
                    }

                    if(!this.haveCalledUpdateUI) {
                        var callback  = function() {
                            _this.updateUI();
                        }
                        setTimeout(callback,1);
                    }
		},
               checkLayout: function() {
                    var d = this.jq(ID_MAP);
                    if(d.width()>0 && this.lastWidth!=d.width() && this.map) {
                        this.lastWidth = d.width();
                        this.map.getMap().updateSize();
                    }
                },

                createMap: function() {
                    var theDisplay  =this;

                    var params = {
                        "defaultMapLayer" : this.getProperty("defaultMapLayer",
                                                             map_default_layer),
                                
                    };
                    var displayDiv = this.getProperty("displayDiv", null);
                    if(displayDiv) {
                        params.displayDiv = displayDiv;
                    }
                    var mapLayers = this.getProperty("mapLayers", null);
                    if(mapLayers) {
                        params.mapLayers =  [mapLayers];
                    }

                    this.map =  this.getProperty("theMap",null);
                    if(this.map) {
                        this.map.setMapDiv(this.getDomId(ID_MAP));
                    } else {
                        this.map = new RepositoryMap(this.getDomId(ID_MAP), params);
                        this.lastWidth = this.jq(ID_MAP).width();
                    }
                    if(this.doDisplayMap()) {
                        this.map.setDefaultCanSelect(false);
                    }
                    this.map.initMap(false);

                    var point = new OpenLayers.LonLat(-107,40);
                    //                    this.map.addPoint("x", point,{});
                    //                    this.map.addMarker("x", point, null, "","");
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

                    if (this.getProperty("bounds")) {
                        var toks = this.getProperty("bounds","").split(",");
                        if(toks.length==4) {
                            if(this.getProperty("showBounds"),true) {
                                var attrs = {};
                                if(this.getProperty("boundsColor")) {
                                    attrs.strokeColor = this.getProperty("boundsColor","");
                                }
                                this.map.addRectangle("bounds", parseFloat(toks[0]),parseFloat(toks[1]),parseFloat(toks[2]),parseFloat(toks[3]),attrs,"");
                            }
                            this.setInitMapBounds(parseFloat(toks[0]),parseFloat(toks[1]),parseFloat(toks[2]),parseFloat(toks[3]));
                        }
                    }

                    var currentFeatures = this.features;
                    this.features = [];
                    for ( var i = 0; i < currentFeatures.length; i++) {
                        this.addFeature(currentFeatures[i]);
                    }
                    var entries = this.getDisplayManager().collectEntries();
                    for ( var i = 0; i < entries.length; i++) {
                        var pair = entries[i];
                        this.handleEventEntriesChanged(pair.source, pair.entries);
                    }

                    if(this.layerEntries) {
                        var selectCallback = function(layer) {
                            _this.handleLayerSelect(layer);
                        }
                        var unselectCallback = function(layer) {
                            _this.handleLayerUnselect(layer);
                        }
                        var toks = this.layerEntries.split(",");
                        for(var i=0;i<toks.length;i++) {
                            var tok = toks[i];
                            var url = ramaddaBaseUrl + "/entry/show?output=shapefile.kml&entryid=" + tok;
                            this.map.addKMLLayer("layer", url, true, selectCallback, unselectCallback);
                            //TODO: Center on the kml
                        }
                    }

                    if(theDisplay.kmlLayer!=null) {
                        var url = ramaddaBaseUrl + "/entry/show?output=shapefile.kml&entryid=" + theDisplay.kmlLayer;
                        theDisplay.addBaseMapLayer(url, true);
                    }
                    if(theDisplay.geojsonLayer!=null) {
                        url = theDisplay.getRamadda().getEntryDownloadUrl(theDisplay.geojsonLayer);
                        theDisplay.addBaseMapLayer(url, false);
                    }
                },
                addBaseMapLayer: function(url, isKml) {
                    var theDisplay = this;
                    mapLoadInfo = displayMapUrlToVectorListeners[url];
                    if(mapLoadInfo == null) {
                        mapLoadInfo = {otherMaps:[], layer:null};
                        selectFunc  = function(layer) {
                            theDisplay.mapFeatureSelected(layer);
                        }
                        if(isKml)
                            this.map.addKMLLayer(this.kmlLayerName,url,this.doDisplayMap(),selectFunc,null,null,
                                                 function(map,layer) {theDisplay.baseMapLoaded(layer,url);});
                        else
                           this.map.addGeoJsonLayer(this.geojsonLayerName,url,this.doDisplayMap(),selectFunc,null,null,
                                                     function(map,layer) {
                                                         theDisplay.baseMapLoaded(layer,url);
                                                     });
                    } else if(mapLoadInfo.layer) {
                        this.cloneLayer(mapLoadInfo.layer);
                    } else {
                        this.map.showLoadingImage();
                        mapLoadInfo.otherMaps.push(this);
                    }
                },
                mapFeatureSelected: function(layer) {
                    if(!this.getPointData()) {
                        return;
                    }
                    this.map.onFeatureSelect(layer);
                    if(!Utils.isDefined(layer.feature.dataIndex)) {
                        return;
                    }
                    this.getDisplayManager().propagateEventRecordSelection(this,this.getPointData(),{index:layer.feature.dataIndex});
                },
               doDisplayMap:  function() {
                    var v = (this.kmlLayer!=null || this.geojsonLayer!=null) && ((""+this.getProperty("displayAsMap","")) == "true");
                    return  v;
                },
                cloneLayer: function(layer) {
                    var theDisplay  = this;
                    this.map.hideLoadingImage();
                    layer = layer.clone();
                    var features = layer.features;
                    var clonedFeatures = [];
                    for (var j = 0; j < features.length; j++) {
                        feature = features[j];
                        feature = feature.clone();
                        if(feature.style) {
                            oldStyle = feature.style;
                            feature.style={};
                            for(var a in oldStyle) {
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
                    this.map.addSelectCallback(layer,this.doDisplayMap(),function(layer) {
                            theDisplay.mapFeatureSelected(layer);});
                },
                baseMapLoaded: function(layer, url) {
                    this.vectorLayer = layer;
                    this.applyVectorMap();
                    mapLoadInfo = displayMapUrlToVectorListeners[url];
                    if(mapLoadInfo) {
                        mapLoadInfo.layer = layer;
                        for(var i=0;i<mapLoadInfo.otherMaps.length;i++) {
                            mapLoadInfo.otherMaps[i].cloneLayer(layer);
                        }
                        mapLoadInfo.otherMaps =[];
                    }
                },
                handleLayerSelect: function(layer) {
                    var args = this.layerSelectArgs;
                    if(!this.layerSelectPath) {
                        if(!args) {
                            this.map.onFeatureSelect(layer);
                            return;
                        }
                        //If args was defined then default to search
                        this.layerSelectPath = "/search/do";
                    }
                    var url = ramaddaBaseUrl + this.layerSelectPath;
                    if(args) {
                        var toks = args.split(",");
                        for(var i=0;i<toks.length;i++) {
                            var tok = toks[i];
                            var toktoks = tok.split(":");                            
                            var urlArg = toktoks[0];
                            var layerField = toktoks[1];
                            var attrs = layer.feature.attributes;
                            var fieldValue = null;
                            for (var attr in attrs) {
                                var attrName =""+ attr;
                                if(attrName == layerField) {
                                    var attrValue = null;
                                    if (typeof attrs[attr] == 'object' || typeof attrs[attr] == 'Object') {
                                        var o = attrs[attr];
                                        attrValue=  o["value"];
                                    } else {
                                        attrValue = attrs[attr];
                                    }
                                    url = HtmlUtil.appendArg(url, urlArg, attrValue);
                                    url = url.replace("${" + urlArg +"}", attrValue);
                                }
                            }
                        }
                    }
                    url = HtmlUtil.appendArg(url, "output", "json");
                    var entryList = new EntryList(this.getRamadda(), url, null, false);
                    entryList.doSearch(this);
                    this.getEntryList().showMessage("Searching", HtmlUtil.div([ATTR_STYLE,"margin:20px;"], this.getWaitImage()));
                },
                getEntryList: function() {
                    if(!this.entryListDisplay) {
                        var props = {
                            showMenu: true,
                            showTitle: true,
                            showDetails:true,
                            layoutHere: false,
                            showForm: false,
                            doSearch: false,
                        };
                        var id = this.getUniqueId("display");
                        this.entryListDisplay = new RamaddaEntrylistDisplay(this.getDisplayManager(),id,props);
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
		addMapLayer : function(source, props) {
                    var _this = this;
                    var entry = props.entry;
                    if(!this.addedLayers) this.addedLayers = {}; 
                    if(this.addedLayers[entry.getId()]) {
                        var layer = this.addedLayers[entry.getId()];
                        if(layer) {
                            this.map.removeKMLLayer(layer);
                            this.addedLayers[entry.getId()] = null;
                        } 
                        return;
                    }
                    
                    if(entry.getType().getId() == "geo_shapefile") {
                        var bounds = createBounds(entry.getWest(),entry.getSouth(), entry.getEast(),entry.getNorth());
                        if(bounds.left<-180||bounds.right>180 || bounds.bottom<-90 || bounds.top>90) {
                            console.log("entry has bad bounds:" + entry.getName() +" " + bounds);
                            return;
                        }

                        var selectCallback = function(layer) {_this.handleLayerSelect(layer);}
                        var unselectCallback = function(layer) {_this.handleLayerUnselect(layer);}
                        var url = ramaddaBaseUrl + "/entry/show?output=shapefile.kml&entryid=" + entry.getId();
                        var layer = this.map.addKMLLayer(entry.getName(), url, true, selectCallback, unselectCallback);
                        this.addedLayers[entry.getId()] = layer;
                        bounds = this.map.transformLLBounds(bounds);
                        this.map.map.zoomToExtent(bounds, true);
                        return;
                    }

                    var baseUrl = entry.getAttributeValue("base_url");
                    if (!Utils.stringDefined(baseUrl)) {
                        console.log("No base url:" + entry.getId());
                        return;
                    }
                    var layer = entry.getAttributeValue("layer_name");
                    if(layer == null) {
                        layer = entry.getName();
                    }
                    this.map.addWMSLayer(entry.getName(), baseUrl,  layer, false);
		},
		mapBoundsChanged : function() {
                    var bounds = this.map.map.calculateBounds();
			bounds = bounds.transform(this.map.sourceProjection,
					this.map.displayProjection);
			this.getDisplayManager().handleEventMapBoundsChanged(this, bounds);
		},
		addFeature : function(feature) {
                    this.features.push(feature);
                    feature.line = this.map.addPolygon("lines_"
                                                       + feature.source.getId(), RecordUtil
                                                       .clonePoints(feature.points), null);
		},
		xloadInitialData : function() {
                    if (this.getDisplayManager().getData().length > 0) {
                        this.handleEventPointDataLoaded(this, this.getDisplayManager()
                                                        .getData()[0]);
                    }
		},

		getContentsDiv : function() {
                    return HtmlUtil.div([ ATTR_CLASS, "display-contents", ATTR_ID,
                                          this.getDomId(ID_DISPLAY_CONTENTS) ], "");
		},
                handleEventEntryMouseover: function(source, args) {
                    id = args.entry.getId() +"_mouseover";
                    attrs = {
                        lineColor:"red",
                        fillColor:"red",
                        fillOpacity:0.5,
                        lineOpacity:0.5,
                        doCircle:true,
                        lineWidth:1,
                        fill: true,
                        circle: {
                            lineColor: "black"
                        },
                        polygon: {
                            lineWidth:4,
                        }
                    }
                    this.addOrRemoveEntryMarker(id, args.entry, true, attrs);
                },
                handleEventEntryMouseout: function(source, args) {
                    id = args.entry.getId() +"_mouseover";
                    this.addOrRemoveEntryMarker(id, args.entry, false);
                },
                handleEventAreaClear:  function() {
                    this.map.clearRegionSelector();
                },
		handleClick : function(theMap, lon, lat) {
                    if(this.doDisplayMap()) {
                        return;
                    }
                    this.getDisplayManager().handleEventMapClick(this, lon, lat);
		},

		getPosition : function() {
			var lat = $("#" + this.getDomId(ID_LATFIELD)).val();
			var lon = $("#" + this.getDomId(ID_LONFIELD)).val();
			if (lat == null)
				return null;
			return [ lat, lon ];
		},

		setInitMapBounds : function(north, west, south, east) {
                    if (!this.map) return;
                    this.map.centerOnMarkers(new OpenLayers.Bounds(west, south, east,
                                                                   north));
		},

		sourceToEntries : {},
		handleEventEntriesChanged : function(source, entries) {
                    //debug
                    if (source == this.lastSource) {
                        this.map.clearSelectionMarker();
                    }
                    if((typeof source.forMap)!="undefined" && !source.forMap) {
                        return;
                    }
                    var oldEntries = this.sourceToEntries[source.getId()];
                    if (oldEntries != null) {
                        for ( var i = 0; i < oldEntries.length; i++) {
                            var id = source.getId() + "_" + oldEntries[i].getId();
                            this.addOrRemoveEntryMarker(id, oldEntries[i], false);
                        }
                    }

                    this.sourceToEntries[source.getId()] = entries;

                    var markers = new OpenLayers.Layer.Markers("Markers");
                    var lines = new OpenLayers.Layer.Vector("Lines", {});
                    var north = -90, west=180,south=90,east = -180;
                    var didOne = false;
                    for ( var i = 0; i < entries.length; i++) {
                        var  entry = entries[i];
                        var id = source.getId() + "_" + entry.getId();
                        var mapEntryInfo  = this.addOrRemoveEntryMarker(id, entries[i], true);
                        if (entry.hasBounds()) {
                            if(entry.getNorth()>90 ||
                               entry.getSouth()<-90 ||
                               entry.getEast()>180 ||
                               entry.getWest()<-180) {
                                console.log("bad bounds on entry:" + entry.getName() +" " +
                                            entry.getNorth() + " " +
                                            entry.getSouth()+ " " +
                                            entry.getEast()+ " " +
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
                    var bounds = (didOne? createBounds(west, south, east, north):null);
                    //debug                    this.map.centerOnMarkers(bounds, true);
		},
		handleEventEntrySelection : function(source, args) {
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
                addOrRemoveEntryMarker : function(id, entry, add, args) {
                    if(!args) {
                        args = {};
                    }
                    var dflt = {
                        lineColor: entry.lineColor,
                        fillColor: entry.lineColor,
                        lineWidth: entry.lineWidth,
                        doCircle:false,
                        doRectangle: this.showBoxes,
                        fill:false,
                        fillOpacity: 0.75,
                        pointRadius : 12,
                        polygon: {},
                        circle:{}
                    }
                    dfltPolygon = {
                    }
                    dfltCircle = {
                    }
                    $.extend(dflt, args);
                    if(!dflt.lineColor) dflt.lineColor = "blue";

                    $.extend(dfltPolygon, dflt);
                    if(args.polygon)
                        $.extend(dfltPolygon, args.polygon);
                    $.extend(dfltCircle, dflt);
                    if(args.circle)
                        $.extend(dfltCircle, args.circle);

                    var mapEntryInfo = this.mapEntryInfos[id];
                    if (!add) {
                        if (mapEntryInfo != null) {
                            mapEntryInfo.removeFromMap(this.map);
                            this.mapEntryInfos[id] = null;
                        }
                    } else  {
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
                            if(latitude<-90 || latitude>90 || longitude<-180 || longitude>180) {
                                return;
                            }
                            var point = new OpenLayers.LonLat(longitude, latitude);
                            if(dflt.doCircle) {
                                attrs = {pointRadius : dfltCircle.pointRadius, 
                                         stroke: true,
                                         strokeColor : dfltCircle.lineColor,
                                         strokeWidth : dfltCircle.lineWidth,
                                         fillColor: dfltCircle.fillColor,
                                         fillOpacity: dfltCircle.fillOpacity,
                                         fill: dfltCircle.fill,};
                                mapEntryInfo.circle = this.map.addPoint(id, point,attrs);
                            } else {
                                mapEntryInfo.marker = this.map.addMarker(id, point, entry.getIconUrl(), "", this.getEntryHtml(entry));
                            }
                            if(entry.polygon) {
                                var points = []
                                    for(var i=0;i<entry.polygon.length;i+=2) { 
                                        points.push(new OpenLayers.Geometry.Point(entry.polygon[i+1],entry.polygon[i]));
                                    }
                                var attrs = {
                                    strokeColor:dfltPolygon.lineColor,
                                    strokeWidth:Utils.isDefined(dfltPolygon.lineWidth)?dfltPolygon.lineWidth:2
                                };
                                mapEntryInfo.polygon =  this.map.addPolygon(id, entry.getName(), points, attrs, mapEntryInfo.marker);
                            }
                            var theDisplay = this;
                            if(mapEntryInfo.marker) {
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
		handleMapClick : function(marker) {
			if (this.selectedMarker != null) {
				this.getDisplayManager().handleEventEntrySelection(this, {
					entry : this.selectedMarker.entry,
					selected : false
				});
			}
			this.getDisplayManager().handleEventEntrySelection(this, {
				entry : marker.entry,
				selected : true
			});
			this.selectedMarker = marker;
		},
                getDisplayProp: function(source, prop, dflt) {
                    if(Utils.isDefined(this[prop])) {
                        return this[prop];
                    }
                    prop = "map-" + prop;
                    if(Utils.isDefined(source[prop])) {
                        return source[prop];
                    }
                    return source.getProperty(prop, dflt);
                },
                applyVectorMap: function() {
                    if(this.vectorMapApplied) {
                        return;
                    }
                    if(!this.doDisplayMap()) {
                        return;
                    }
                    if(!this.vectorLayer) {
                        return;
                    }
                    if(!this.points) {
                        return;
                    }
                    this.vectorMapApplied = true;
                    var features = this.vectorLayer.features.slice();
                    var circles = this.points;
                    for (var i = 0; i < circles.length; i++) {
                        var circle = circles[i];
                        var center = circle.center;
                        var matchedFeature = null;
                        var index = -1;

                        for (var j = 0; j < features.length; j++) {
                            var feature = features[j];
                            var geometry = feature.geometry;
                            if(!geometry) {
                                break;
                            }
                            bounds = geometry.getBounds();
                            if(!bounds.contains(center.x,center.y)) {
                                continue;
                            }
                            if(geometry.components) {
                                for(var sub=0;sub<geometry.components.length;sub++) {
                                    comp = geometry.components[sub];
                                    bounds = comp.getBounds();
                                    if(!bounds.contains(center.x,center.y)) {
                                        continue;
                                    }
                                    if(comp.containsPoint && comp.containsPoint(center)) {
                                        matchedFeature = feature;
                                        index = j;
                                        break;
                                    }
                                }
                                if(matchedFeature)
                                    break;
                                continue;
                            }
                            if(!geometry.containsPoint) {
                                console.log("unknown geometry:" + geometry.CLASS_NAME);
                                continue;
                            }
                            if(geometry.containsPoint(center)) {
                                matchedFeature = feature;
                                index = j;
                                break;
                            }
                       }
                       if(matchedFeature) {
                           features.splice(index,1);
                           style = matchedFeature.style;
                           if(!style) style = {
                                   "stylename": "from display"
                               };
                           $.extend(style, circle.style);
                           matchedFeature.style = style;
                           matchedFeature.popupText =  circle.text;
                           matchedFeature.dataIndex = i;
                       } 
                    }
                    if((""+this.getProperty("pruneFeatures","")) == "true") {
                        this.vectorLayer.removeFeatures(features);
                        var dataBounds = this.vectorLayer.getDataExtent();
                        bounds = this.map.transformProjBounds(dataBounds);
                        this.map.centerOnMarkers(bounds,true);
                    } 
                    this.vectorLayer.redraw();
                },
                needsData:function() {
                    return true;
                },
               updateUI: function() {
                    this.haveCalledUpdateUI = true;
                    SUPER.updateUI.call(this);
                    if(!this.getDisplayReady()) {
                        return;
                    }
                    if(!this.hasData()) {
                        return;
                    }
                    if(!this.getProperty("showData",true)) 
                        return;
                    var pointData = this.getPointData();
                    var records = pointData.getRecords();
                    if(records == null) {
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
                    if(points.length ==0) {
                        console.log("points.legnth==0");
                        return;
                    }

                    source = this;
                    var radius = parseFloat(this.getDisplayProp(source,"radius",8));
                    var strokeWidth = parseFloat(this.getDisplayProp(source,"strokeWidth","1"));
                    var strokeColor = this.getDisplayProp(source, "strokeColor", "#000");
                    var colorByAttr = this.getDisplayProp(source, "colorBy", null);
                    var colors = this.getColorTable(true);
                    var sizeByAttr = this.getDisplayProp(source, "sizeBy",null);
                    var isTrajectory =  this.getDisplayProp(source,"isTrajectory",false);
                    if(isTrajectory) {
                        this.map.addPolygon("id", points, null,null);
                        return;
                    }
                     if(!colors && source.colors && source.colors.length>0) {
                        colors = source.colors;
                        if(colors.length==1 &&  Utils.ColorTables[colors[0]]) {
                            colors = Utils.ColorTables[colors[0]].colors;
                        }
                    }

                    if(colors == null) {
                        colors = Utils.ColorTables.grayscale.colors;
                    }
                    var records = pointData.getRecords();

                    var colorBy = {
                        id:colorByAttr,
                        minValue:0,
                        maxValue:0,
                        field: null,
                        index:-1,
                    };

                    
                    var sizeBy = {
                        id:this.getDisplayProp(source,"sizeBy",null),
                        minValue:0,
                        maxValue:0,
                        field: null,
                        index:-1,
                    };

                    for(var i=0;i<fields.length;i++) {
                        var field = fields[i];
                        if(field.getId() == colorBy.id || ("#"+(i+1)) ==colorBy.id) {
                            colorBy.field = field;
                        }
                        if(field.getId() == sizeBy.id || ("#"+(i+1)) == sizeBy.id) {
                            sizeBy.field = field;
                        }
                    }


                    sizeBy.index = sizeBy.field!=null?sizeBy.field.getIndex():-1;
                    colorBy.index = colorBy.field!=null?colorBy.field.getIndex():-1;
                    var excludeZero = this.getProperty(PROP_EXCLUDE_ZERO,false);
                    for(var i=0;i<points.length;i++) {
                        var pointRecord  = records[i];
                        var tuple = pointRecord.getData();
                        var v = tuple[colorBy.index];
                        if(excludeZero && v == 0) {
                            continue;
                        }
                        if(i == 0 || v>colorBy.maxValue) colorBy.maxValue = v;
                        if(i == 0 || v<colorBy.minValue) colorBy.minValue = v;

                        v = tuple[sizeBy.index];
                        if(i == 0 || v>sizeBy.maxValue) sizeBy.maxValue = v;
                        if(i == 0 || v<sizeBy.minValue) sizeBy.minValue = v;
                    }


                    if(this.showPercent) {
                        colorBy.minValue = 0;
                        colorBy.maxValue = 100;
                    } 
                    colorBy.minValue = this.getDisplayProp(source, "colorByMin", colorBy.minValue);
                    colorBy.maxValue = this.getDisplayProp(source, "colorByMax", colorBy.maxValue);

                    //                    console.log("Color by:" + " Min: " + colorBy.minValue +" Max: " + colorBy.maxValue);

                    var dontAddPoint = this.doDisplayMap();
                    var didColorBy = false;
                    var seen = {};
                    for(var i=0;i<points.length;i++) {
                        var pointRecord  = records[i];
                        var point = points[i];
                        if(Utils.isDefined(seen[point])) continue;
                        seen[point] = true;
                        var props = {
                            pointRadius:radius,
                            strokeWidth: strokeWidth,
                            strokeColor: strokeColor,
                        };

                        if(sizeBy.index>=0) {
                            var value = pointRecord.getData()[sizeBy.index];
                            var denom = (sizeBy.maxValue-sizeBy.minValue);
                            var percent = (denom == 0?NaN:(value-sizeBy.minValue)/denom);
                            props.pointRadius = 6 + parseInt(15*percent);
                            //                            console.log("percent:" + percent +  " radius: " + props.pointRadius +" Value: " + value  + " range: " + sizeBy.minValue +" " + sizeBy.maxValue);
                        }
                        if(colorBy.index>=0) {
                            var value = pointRecord.getData()[colorBy.index];
                            //                            console.log("value:" + value +" index:" + colorBy.index+" " + pointRecord.getData());
                            var percent = 0;
                            var msg = "";
                            var pctFields = null;
                            if(this.percentFields!=null) {
                                pctFields = this.percentFields.split(",");
                            }
                            if(this.showPercent) {
                                var total = 0;
                                var data= pointRecord.getData();
                                var msg ="";
                                for(var j=0;j<data.length;j++) {
                                    var ok = fields[j].isNumeric && !fields[j].isFieldGeo();
                                    if(ok && pctFields!=null) {
                                        ok =  pctFields.indexOf(fields[j].getId())>=0 ||
                                            pctFields.indexOf("#"+(j+1))>=0;
                                    }
                                    if(ok) {
                                        total+=data[j];
                                        msg += " " + data[j];
                                    }
                                }
                                if(total!=0) {
                                    percent0 = percent  = value/total*100;
                                    percent = (percent-colorBy.minValue)/(colorBy.maxValue-colorBy.minValue);
                                    //                                    console.log("%:" + percent0 +" range:" + percent +" value"+ value +" " + total+"data: " + msg);
                                }

                            } else {
                                percent = (value-colorBy.minValue)/(colorBy.maxValue-colorBy.minValue);
                            }

                            var index = parseInt(percent*colors.length);
                            //                            console.log(colorBy.index +" value:" + value+ " " + percent + " " +index + " " + msg);
                            if(index>=colors.length) index = colors.length-1;
                            else if(index<0) index = 0;
                            //                            console.log("value:" + value+ " %:" + percent +" index:" + index +" c:" + colors[index]);

                            props.fillOpacity=0.8;
                            props.fillColor = colors[index];
                            didColorBy = true;
                        }
                        var html = this.getRecordHtml(pointRecord,fields);
                        point = this.map.addPoint("pt-"  + i, point, props, html,dontAddPoint);
                        if(!this.points) 
                            this.points=[];
                        this.points.push(point);
                    }
                    if(didColorBy)
                        this.displayColorTable(colors, ID_BOTTOM, colorBy.minValue, colorBy.maxValue);
                    this.applyVectorMap();
                },
		handleEventRemoveDisplay : function(source, display) {
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
		findFeature : function(source, andDelete) {
			for ( var i in this.features) {
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

		handleEventRecordSelection : function(source, args) {
                    var record = args.record;
                    if (record.hasLocation()) {
                        var latitude = record.getLatitude();
                        var longitude = record.getLongitude();
                        if(latitude<-90 || latitude>90 || longitude<-180 || longitude>180) return;
                        var point = new OpenLayers.LonLat(longitude, latitude);
                        var marker = this.myMarkers[source];
                        if (marker != null) {
                            this.map.removeMarker(marker);
                        }
                        var icon = displayMapMarkerIcons[source];
                        if(icon == null) {
                            icon =  displayMapGetMarkerIcon();
                            displayMapMarkerIcons[source] = icon;
                        }
                        this.myMarkers[source] = this.map.addMarker(source.getId(), point, icon, "", args.html,null,24);
                    }
		}
	});
}

function MapEntryInfo(entry) {
	RamaddaUtil.defineMembers(this, {
		entry : entry,
		marker : null,
		rectangle : null,
		removeFromMap : function(map) {
			if (this.marker != null) {
				map.removeMarker(this.marker);
			}
			if (this.rectangle != null) {
				map.removePolygon(this.rectangle);
			}
                        if(this.polygon!=null) {
				map.removePolygon(this.polygon);
                        }
                        if(this.circle!=null) {
                            map.removePoint(this.circle);
                        }
		}

	});
}
