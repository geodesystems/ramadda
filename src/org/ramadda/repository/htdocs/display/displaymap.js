/**
   Copyright 2008-2023 Geode Systems LLC
*/


const DISPLAY_MAP = "map";
const DISPLAY_MAPGRID = "mapgrid";
const DISPLAY_MAPCHART = "mapchart";
const DISPLAY_MAPARRAY = "maparray";
const DISPLAY_MAPSHRINK = "mapshrink";
const DISPLAY_MAPIMAGES = "mapimages";

let displayMapMarkers = ["marker.png", "marker-blue.png", "marker-gold.png", "marker-green.png"];
let displayMapCurrentMarker = -1;
let displayMapUrlToVectorListeners = {};
let displayMapMarkerIcons = {};

var debugit = false;
var debugFeatureLinking = false;
var debugMapTime = false;

addGlobalDisplayType({
    type: DISPLAY_MAP,
    label: "Map",
    category:CATEGORY_MAPS,
    tooltip: makeDisplayTooltip("Maps of many colors",["map1.png","map2.png"],"Lots of ways to show georeferenced data - dots, heatmaps, plots, etc"),        
});

addGlobalDisplayType({
    type: DISPLAY_MAPGRID,
    label: "Map Grid",
    category:CATEGORY_MAPS,
    tooltip: makeDisplayTooltip("Schematic map grid","mapgrid.png","Can display US States or World countries"),    
});

addGlobalDisplayType({
    type: DISPLAY_MAPCHART,
    label: "Map Chart",
    requiresData: true,
    category:CATEGORY_MAPS,
    tooltip: makeDisplayTooltip("2.5D display in a map","mapchart.png","Plot numeric data as heights. Can display US States, European countries or world countries"),        
});


addGlobalDisplayType({
    type: DISPLAY_MAPSHRINK,
    label: "Map Shrink",
    requiresData: true,
    category:CATEGORY_MAPS,
    tooltip: makeDisplayTooltip("Show values as relative size of map regions","mapshrink.png","Can display US States, European countries or world countries"),            
});


addGlobalDisplayType({
    type: DISPLAY_MAPARRAY,
    label: "Map Array",
    requiresData: true,
    category:CATEGORY_MAPS,
    tooltip: makeDisplayTooltip("Colored map regions displayed separately","maparray.png","Can display US States, European countries or world countries"),                
});
addGlobalDisplayType({
    type: DISPLAY_MAPIMAGES,
    label: "Map Images",
    requiresData: true,
    category:CATEGORY_MAPS,
    tooltip: makeDisplayTooltip("Display images in map regions","mapimage.png","Can display US States, European countries or world countries"),                    
});




function MapFeature(source, points) {
    RamaddaUtil.defineMembers(this, {
        source: source,
        points: points
    });
}

var ID_MAP = "map";
var ID_MAP_CONTAINER = "mapcontainer";
var xcnt = 1;
var ycnt = 1;

function RamaddaBaseMapDisplay(displayManager, id, type,  properties) {
    $.extend(this, {
        theMap: null
    });

    this.myycnt = ++ycnt;
    this.myName = "map " + (this.myycnt);
    const SUPER = new RamaddaDisplay(displayManager, id, type,   properties);
    RamaddaUtil.inherit(this,SUPER);
    this.defineSizeByProperties();
    let myProps = [
	{label:'Base Map Properties'},
	{p:'bounds',ex:'north,west,south,east',tt:'initial bounds'},
	{p:'gridBounds',ex:'north,west,south,east'},	
	{p:'mapCenter',ex:'lat,lon',tt:"initial position"},
	{p:'zoomLevel',ex:4,tt:"initial zoom"},
	{p:'zoomTimeout',ex:500,tt:"initial zoom timeout delay. set this if the map is in tabs, etc, and not going to the initial zoom"},
	{p:'linked',ex:true,tt:"Link location with other maps"},
	{p:'linkGroup',ex:'some_name',tt:"Map groups to link with"},
	{p:'initialLocation', ex:'lat,lon',tt:"initial location"},
	{p:'defaultMapLayer',ex:'osm|google.roads|esri.street|opentopo|esri.topo|usfs|usgs.topo|google.terrain|google.satellite|naip|usgs.imagery|esri.shaded|esri.lightgray|esri.darkgray|esri.terrain|shadedrelief|esri.aeronautical|historic|osm.toner|osm.toner.lite|watercolor'},
//	{p:'mapLayers',ex:'ol.openstreetmap,esri.topo,esri.street,esri.worldimagery,esri.lightgray,esri.physical,opentopo,usgs.topo,usgs.imagery,usgs.relief,osm.toner,osm.toner.lite,watercolor'},
	{p:'extraLayers',tt:'comma separated list of layers to display'},
	{p:'linkField',tt:'The field in the data to match with the map field, e.g., geoid'},
	{p:'linkFeature',tt:'The field in the map to match with the data field, e.g., geoid'},	

	{p:'annotationLayerTop',ex:'true',tt:'If showing the extra annotation layer put it on top'},

	{p:'showBounds',ex:'true',d:false},
	{p:'boundsStrokeColor',d:'blue'},
	{p:'boundsFillColor',d:'transparent'},
	{p:'boundsFillOpacity',d:'0.0'},			

	{p:'showOpacitySlider',ex:'false',d:false},
	{p:'showLocationSearch',ex:'true'},
	{p:'showLatLonPosition',ex:'false',d:true},
	{p:'showLayerSwitcher',d:true,ex:'false'},
	{p:'showScaleLine',ex:'true',d:false},
	{p:'showZoomPanControl',ex:'true',d:true},
	{p:'showZoomOnlyControl',ex:'false',d:false},
	{p:'enableDragPan',ex:'false',d:true},
	{p:'showLayers',d:true,ex:'false',tt:'Connect points with map vectors'},
	{p:'showBaseLayersSelect',ex:true,d:false},
	{p:'locations',ex:'usairports.json,usstates.json'},
	{p:'highlightColor',d:'blue',ex:'#ccc',tt:''},
	{p:'highlightFillColor',ex:'#ccc',
	 tt:'Use "match" to match the features opacity'},		
	{p:'highlightFillOpacity',ex:'0.5',
	 tt:'Use "match" to match the features opacity'},		
	{p:'highlightStrokeWidth',ex:'2',
	 tt:'Use "match" to match the features opacity'},			 
	{p:"highlightStrokeColor",
	 tt:'Use "match" to match the features opacity'},

	{p:'selectFillColor',ex:'#ccc',
	 tt:'Use "match" to match the features opacity'},		
	{p:'selectFillOpacity',ex:'0.5',
	 tt:'Use "match" to match the features opacity'},		
	{p:'selectStrokeWidth',ex:'2',
	 tt:'Use "match" to match the features opacity'},			 
	{p:"selectStrokeColor",
	 tt:'Use "match" to match the features opacity'},
	{p:"selectStrokeOpacity",
	 tt:'Use "match" to match the features opacity'},			 		
	
        {p:"vectorLayerStrokeColor"},
	{p:"vectorLayerFillColor"},
	{p:"vectorLayerFillOpacity",ex:0.25},
        {p:"vectorLayerStrokeWidth",ex:2},
    ];

    this.debugZoom = properties['debugZoom'];
    
    displayDefineMembers(this, myProps, {
        mapBoundsSet: false,
	extraLayers:[],
        initDisplay: function() {
//	    if(displayDebug.initMap) console.log(this.getLogLabel()+".initDisplay");
            SUPER.initDisplay.call(this);
	    if(!HU.documentReady) {
		$( document ).ready(()=> {
		    if(this.map) {
			setTimeout(()=>{
			    this.callingUpdateSize = true;
			    this.map.getMap().updateSize();
			    this.callingUpdateSize = false;
			},50);
		    }
		});
	    }
            let _this = this;
            let html = "";
            let extraStyle="";
            let height = this.getProperty("height", this.getProperty("mapHeight", 300));
            if (height < 0) {
		height = (-height)+"%";
	    }
	    height = HU.getDimension(height);
            extraStyle += HU.css(HEIGHT, height);

	    let map =HU.div(["tabindex","1",ATTR_CLASS, "display-map-map ramadda-expandable-target", STYLE,
			     extraStyle, ATTR_ID, this.domId(ID_MAP)]);

	    let mapContainer = HU.div([CLASS,"ramadda-map-container",ID,this.domId(ID_MAP_CONTAINER)],
				      map+
				      HU.div([CLASS,"ramadda-map-slider",STYLE,this.getProperty("popupSliderStyle", "max-height:400px;overflow-y:auto;xxxmax-width:300px;overflow-x:auto;"),ID,this.domId(ID_MAP)+"_slider"]));

            this.setContents(mapContainer);
    
            if (!this.map) {
                this.createMap();
            } else {
                this.map.setMapDiv(this.domId(ID_MAP));
            }
            if (!this.haveCalledUpdateUI) {
//		if(displayDebug.initMap)  console.log(this.getLogLabel()+" have not calledUpdateUI. setting up callback");
                let callback = function() {
		    _this.updateUICallback = null;
                    _this.updateUI();
                }
                this.updateUICallback = setTimeout(callback, 1);
            }
        },
	getTurfPoints:function(f) {
	    let p=[];
	    let coords = f.geometry.coordinates;
	    coords.forEach(pair=>{
		p.push(pair[1],pair[0]);
	    });
	    return p;
	},
	makeTurfBounds:function(bounds,padding) {
	    if(Utils.isDefined(padding)) {
		let w = bounds.east-bounds.west;
		let h = bounds.north - bounds.south;
		return [bounds.west-padding*w, bounds.south-padding*h, bounds.east+padding*w, bounds.north+padding*h];
	    }
	    return [bounds.west, bounds.south, bounds.east, bounds.north];
	},
	makeFeature:function(map,geometryType, style, points) {
	    if(points.length>2) {
		let latLons = [];
		for(let i=0;i<points.length;i+=2) {
		    latLons.push(MapUtils.createPoint(points[i+1],points[i]));
		}
		if(geometryType=="OpenLayers.Geometry.Polygon") {
		    map.transformPoints(latLons);
		    let linearRing = MapUtils.createLinearRing(latLons);
		    let geom = MapUtils.createPolygon(linearRing);
		    return MapUtils.createVector(geom,null,style);
		} else {
		    return  map.createPolygon("","",latLons,style,null,geometryType=="OpenLayers.Geometry.LineString");
		}
	    } 
	    let point =  MapUtils.createLonLat(points[1], points[0]);
	    return  map.createPoint("",point,style);
	},

	createGeoJsonLayer:function(name,geojson,layer) {
	    if(layer) {
		layer.removeFeatures(layer.features);
	    } else {
		layer = MapUtils.createLayerVector(name,
						   {projection: this.getMap().getMap().displayProjection});
		this.getMap().addLayer(layer);
		layer.ramaddaLayerIndex = 100;
		if(Utils.isDefined(this.layerVisible) && !this.layerVisible)
		    layer.setVisibility(false);
	    }
            let format= new OpenLayers.Format.GeoJSON({});
	    let features = format.read(geojson);
	    let strategy = new OpenLayers.Strategy.Fixed();
	    strategy.layer= layer;
	    strategy.merge({features:features});
	    this.extraLayers.push(layer);
	    return layer;
	},

	getBaseLayersSelect:function() {
	    if(this.map.baseLayers) {
		let items = [];
		let on = false;
		for(a in this.map.baseLayers) {
		    let layer = this.map.baseLayers[a];
		    if(!layer.isBaseLayer) continue;
		    if(layer.getVisibility()) on = a;
		    items.push([a,layer.name]);
		}
		return  HU.span([TITLE,"Choose base layer", CLASS,"display-filter"],  HU.select("",[ID,this.domId("baselayers")],items,on));
	    }
	    return '';
	},
	initBaseLayersSelect:function() {
	    let _this = this;
	    this.jq("baselayers").change(function() {
		let on = $(this).val();
		for(let id in _this.map.baseLayers) {
		    if(id==on) {
			_this.map.getMap().setBaseLayer(_this.map.baseLayers[id]);
			break;
		    }
		}
	    });
	},
	checkLevelRange:function(layers, redraw) {
	    if(!layers) layers=[this.getMap().getMarkersLayer()];
	    if(this.debugZoom) console.log("features:");
	    let level = this.getMap().getMap().getZoom();
	    layers.forEach(layer=>{
		if(!layer) return;
		layer.features.forEach(feature=>{
		    if(!feature.levelRange) {
			if(this.debugZoom) console.log("\tno level range");
			return;
		    }
		    if(!feature.style) feature.style = {};
		    let visible = level>=feature.levelRange.min &&
			level<=feature.levelRange.max;
		    let changed= false;
		    //		console.log("\tlevel:",level,feature.levelRange,visible); 
		    if(visible) {
			changed = feature.style.display != 'inline';
			feature.style.display = 'inline';
		    }  else {
			changed = feature.style.display != 'none';
			feature.style.display = 'none';
		    }			
		    if(redraw &&changed) {
			layer.drawFeature(feature);
		    }
		});
	    });
	},

	getMap:function() {
	    return this.map;
	},
	setErrorMessage: function(msg) {
	    if(this.errorMessageHandler) {
		this.errorMessageHandler(this,msg);
		return
	    }
	    if(this.map)
		this.map.setProgress(HU.div([ATTR_CLASS, "display-map-message"], msg));
	    else
		SUPER.setErrorMessage.call(this,msg);
	},
	setMessage: function(msg) {
	    if(this.map) {
		if(msg!="")
		    msg = HU.div([ATTR_CLASS, "display-map-message"], msg);
		this.map.setProgress(msg);
	    }
	},
	setMapLabel: function(msg) {
	    if(this.map)
		this.map.setLabel(HU.div([ATTR_CLASS, "display-map-message"], msg));
	},	
	startProgress: function() {
	    this.setMessage(this.getProperty("loadingMessage","Creating map..."));
	},
	clearProgress: function() {
	    if(this.errorMessage) {
		this.errorMessage = null;
		return;
	    }
	    if(this.map)
		this.map.setProgress("");
	},

        checkLayout: function() {
            if (!this.map) {
                return;
            }
            let d = this.jq(ID_MAP);
            if (d.width() > 0 && this.lastWidth != d.width() && this.map) {
                this.lastWidth = d.width();
                this.map.getMap().updateSize();
            }
	    if(!this.setMapLocationAndZoom && this.mapParams) {
		this.setMapLocationAndZoom = true;
		if(this.mapParams.initialZoom>=0) {
		    this.map.getMap().zoomTo(this.mapParams.initialZoom);
		}
		if(this.mapParams.initialLocation) {
		    let loc = MapUtils.createLonLat(this.mapParams.initialLocation.lon, this.mapParams.initialLocation.lat);
		    this.map.setCenter(loc);
		}

	    }
        },

        initMapParams: function(params) {
	    if(this.getShowOpacitySlider()) {
		params.showOpacitySlider=true;
	    }
	    ['highlightStrokeColor','highlightFillColor',"highlightStrokeWidth","highlightFillOpacity","changeSizeOnSelect"].forEach(p=>{
		let v = this.getProperty(p);
		if(Utils.isDefined(v)) {
		    params[p] = v;
		}
	    });
	},
	initMap:function(map) {
	},
        doDisplayMap: function() {
	    return true;
	},
        createMap: function() {
            this.map = this.getProperty("externalMap", null);
	    if(this.map) {
		return this.map;
	    }

            let _this = this;
            let params = {
                defaultMapLayer: this.getDefaultMapLayer(map_default_layer),
		showLayerSwitcher: this.getShowLayerSwitcher(),
		showScaleLine: this.getShowScaleLine(),
		showLatLonPosition: this.getShowLatLonPosition(),
		showZoomPanControl: this.getShowZoomPanControl(),
		showZoomOnlyControl: this.getShowZoomOnlyControl(),
		enableDragPan: this.getEnableDragPan(),
		highlightColor: this.getHighlightColor(),
		highlightFillColor: this.getHighlightFillColor("match"),
		highlightFillOpacity: this.getHighlightFillOpacity(),				
		highlightStrokeWidth: this.getHighlightStrokeWidth(1),

		selectFillColor: this.getSelectFillColor("match"),
		selectFillOpacity: this.getSelectFillOpacity(),				
		selectStrokeWidth: this.getSelectStrokeWidth(),
		selectStrokeColor: this.getSelectStrokeColor(),		
		selectStrokeOpacity: this.getSelectStrokeOpacity(),				

		showLatLonLines:this.getProperty("showLatLonLines"),
		popupWidth: this.getProperty("popupWidth",400),
		popupHeight: this.getProperty("popupHeight",200),		

            };
	    this.mapParams = params;
            let displayDiv = this.getProperty("displayDiv", null);
            if (displayDiv) {
                params.displayDiv = displayDiv;
		params.displayDivSticky = this.getProperty("displayDivSticky", false);
            }
            if (!this.getShowLocationSearch(true)) {
                params.showLocationSearch = false;
            }
//            let mapLayers = this.getMapLayers(null);
//            if (mapLayers) {
//                params.mapLayers = [mapLayers];
//            }

	    params.linked = this.getLinked(false);
	    params.linkGroup = this.getLinkGroup(null);

	    this.hadInitialPosition = false;
            if (this.getProperty("latitude")) {
		this.hadInitialPosition = true;
                params.initialLocation = {lon:+this.getProperty("longitude", -105),
					  lat:+this.getProperty("latitude", 40)};
	    }
	    if(this.getMapCenter()) {
		this.hadInitialPosition = true;
		[lat,lon] =  this.getMapCenter().replace("%2C",",").split(",");
                params.initialLocation = {lon:lon,lat:lat};
	    }

	    if(this.getZoomLevel()) {
		this.hadInitialPosition = true;
                params.initialZoom = +this.getZoomLevel();
		params.initialZoomTimeout = this.getZoomTimeout();
	    }

            this.map = this.getProperty("theMap", null);
            if (this.map) {
                this.map.setMapDiv(this.domId(ID_MAP));
            } else {
		if(this.getInitialLocation()) {
		    let toks = this.getInitialLocation().split(",");
		    params.initialLocation = {
			lat:+toks[0],
			lon:+toks[1]
		    }
		}
		this.initMapParams(params);
                this.map = new RepositoryMap(this.domId(ID_MAP), params);
		this.map.myid = this.getLogLabel();
		//Set this so there is no popup on the off feature
		this.map.textGetter = (layer,feature) =>{
		    return null;
		};
                this.lastWidth = this.jq(ID_MAP).width();
            }
	    this.initMap(this.map);

            if (this.doDisplayMap()) {
                this.map.setDefaultCanSelect(false);
            }
            this.map.initMap(false);

	    let hasLoc = Utils.isDefined(this.getZoomLevel())   ||
		Utils.isDefined(this.getMapCenter()) ||
		this.hadInitialPosition;
	    
            if (this.getPropertyBounds() ||this.getPropertyGridBounds() ) {
		this.hadInitialPosition = true;
                let toks = this.getPropertyBounds(this.getGridBounds("")).split(",");
                if (toks.length == 4) {
                    if (this.getShowBounds()) {
                        let attrs = {};
                        if (this.getBoundsStrokeColor(this.getProperty("boundsColor"))) {
                            attrs.strokeColor = this.getBoundsStrokeColor(this.getProperty("boundsColor"));
                        }
                        attrs.fillColor = this.getBoundsFillColor();
                        attrs.fillOpacity = this.getBoundsFillOpacity();			
                        this.map.addRectangle("bounds", parseFloat(toks[0]), parseFloat(toks[1]), parseFloat(toks[2]), parseFloat(toks[3]), attrs, "");
                    }
		    if(!hasLoc)
			this.setInitMapBounds(parseFloat(toks[0]), parseFloat(toks[1]), parseFloat(toks[2]), parseFloat(toks[3]));
                }
            }

	    
	    if(this.getProperty("annotationLayer")) {
		let opts = {theMap:this.map,
			    embedded:true,
			    displayOnly:true,
			   };
		if(this.getPropertyAnnotationLayerTop()) {
		    opts.layerIndex = 100;
		}

		this.editableMap = new  RamaddaEditablemapDisplay(this.getDisplayManager(),HU.getUniqueId(""),opts);
		this.editableMap.initDisplay(true);
		this.editableMap.loadMap(this.getProperty("annotationLayer"));
	    }
	    
//extraLayers="baselayer:nexrad,geojson:US States:resources/usmap.json:fillColor:transparent"
	    this.getProperty("extraLayers","").split(",").forEach(tuple=>{
		if(tuple.trim().length==0) return;
		let toks = tuple.split(":");
		toks = toks.map(tok=>{return tok.replace(/_semicolon_/g,":")});
		toks = toks.map(tok=>{return tok.replace(/_comma_/g,",")});		
		let getUrl = url =>{
		    if(url.startsWith("resources")) {
			url = ramaddaBaseUrl +"/" + url;
		    } else if(url.startsWith("/resources")) {
			url = ramaddaBaseUrl + url;			
		    } else    if(!url.startsWith("/") && !url.startsWith("http")) {
			url = ramaddaBaseUrl +"/entry/get?entryid=" + url;
		    }
		    return url;
		};

		let type = toks[0];
		if(type=="baselayer") {
		    let layer = this.map.getBaseLayer(toks[1]);
		    if(!layer) {
			this.logMsg("Could not find base layer:" + toks[1]);
		    } else {
			layer.setVisibility(true);
		    }
		} else 	if(type=="geojson" || type=="kml") {
		    let name = toks[1];		
		    let url = getUrl(toks[2]);
//		    console.log("Adding geojson:" + url);
		    let args = {
			fillColor:'transparent',
		    }
		    for(let i=3;i<toks.length;i+=2) {
			args[toks[i]] = toks[i+1];
		    }
		    //(name, url, canSelect, selectCallback, unselectCallback, args, loadCallback, zoomToExtent)
		    if(type=="kml") {
			this.map.addKmlLayer(name, url, false, null, null, args, null);
		    } else {
			this.map.addGeoJsonLayer(name, url, false, null, null, args, null);
		    }
		} else if(type=="wms") {
		    let name = toks[1];
		    let url = toks[2];
		    let layer=toks[3];
		    let opacity = toks[4];
                    this.map.addWMSLayer(name,url,layer, false,true,{visible:true,opacity:opacity});
		  //  "wms:ESRI Aeronautical,https://wms.chartbundle.com/mp/service,sec",
		} else {
		    console.log("Unknown map type:" + type)
		}
	    });



            if (this.getShowLayers()) {
		//do this later so the map displays its initial location OK
		setTimeout(()=>{
                    if (_this.getProperty("kmlLayer")) {
			let url = ramaddaBaseUrl + "/entry/show?output=shapefile.kml&entryid=" + _this.getProperty("kmlLayer");
			_this.addBaseMapLayer(url, true);
                    }
                    if (_this.getProperty("geojsonLayer")) {
			url = _this.getRamadda().getEntryDownloadUrl(_this.getProperty("geojsonLayer"));
			_this.addBaseMapLayer(url, false);
                    }
		},500);
            }
        },
        addBaseMapLayer: function(url, isKml) {
            let _this = this;
            mapLoadInfo = displayMapUrlToVectorListeners[url];
            if (mapLoadInfo == null) {
                mapLoadInfo = {
                    otherMaps: [],
                    layer: null
                };
                let selectFunc = function(layer) {
                    _this.mapFeatureSelected(layer);
                }
		//Don't do this for now as its handled elsewhere?
		selectFunc = null;
                let hasBounds = this.getProperty("bounds") != null ||
		    Utils.isDefined(this.getProperty("zoomLevel"))   ||
		    Utils.isDefined(this.getProperty("mapCenter"));
		let attrs =   {
                    strokeColor: this.getProperty("vectorLayerStrokeColor","#000"),
		    fillColor:this.getProperty("vectorLayerFillColor","#ccc"),
		    fillOpacity:this.getProperty("vectorLayerFillOpacity",0.25),
                    strokeWidth: this.getProperty("vectorLayerStrokeWidth",1),
		}
                if (isKml)
                    this.map.addKMLLayer(this.getProperty("kmlLayerName"), url, this.doDisplayMap(), selectFunc, null, attrs,
					 function(map, layer) {
					     _this.baseMapLoaded(layer, url);
					 }, !hasBounds);
                else
                    this.map.addGeoJsonLayer(this.getProperty("geojsonLayerName"), url, this.doDisplayMap(), selectFunc, null, attrs,
					     function(map, layer) {
						 _this.baseMapLoaded(layer, url);
					     }, !hasBounds);
            } else if (mapLoadInfo.layer) {
                this.cloneLayer(mapLoadInfo.layer);
            } else {
                this.map.showLoadingImage();
                mapLoadInfo.otherMaps.push(this);
            }
        },
        baseMapLoaded: function(layer, url) {
            this.vectorLayer = layer;
            this.applyVectorMap();
            mapLoadInfo = displayMapUrlToVectorListeners[url];
            if (mapLoadInfo) {
                mapLoadInfo.layer = layer;
                for (let i = 0; i < mapLoadInfo.otherMaps.length; i++) {
                    mapLoadInfo.otherMaps[i].cloneLayer(layer);
                }
                mapLoadInfo.otherMaps = [];
            }
        },
        applyVectorMap: function(force, textGetter, args) {
	},
        getBounds: function() {
	    if(this.map)
		return this.map.getBounds();
	    return null;
	},
    });
}
	


function RamaddaMapDisplay(displayManager, id, properties) {
    const ID_MAP_SLIDER = "map_slider";    
    const ID_LATFIELD = "latfield";
    const ID_LONFIELD = "lonfield";
    const ID_SIZEBY_LEGEND = "sizebylegend";
    const ID_COLORTABLE_SIDE = "colortableside";
    const ID_SHAPES = "shapes";
    const ID_HEATMAP_ANIM_LIST = "heatmapanimlist";
    const ID_HEATMAP_ANIM_PLAY = "heatmapanimplay";
    const ID_HEATMAP_ANIM_STEP_FORWARD = "heatmapanimstepforward";
    const ID_HEATMAP_ANIM_STEP_BACK = "heatmapanimstepback";    
    const ID_HEATMAP_TOGGLE = "heatmaptoggle";    
    const ID_REGION_SELECTOR = "regionselector";
    const ID_HTMLLAYER = "htmllayer";
    const ID_TRACK_VIEW = "trackview";

    $.extend(this, {
        showBoxes: true,
        showPercent: false,
        percentFields: null,
        kmlLayer: null,
        kmlLayerName: "",
        geojsonLayer: null,
        geojsonLayerName: "",
        theMap: null,
	layerVisible:true
    });


    const SUPER = new RamaddaBaseMapDisplay(displayManager, id, DISPLAY_MAP, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    this.defineSizeByProperties();
    let myProps = [
	{label:'Map Properties'},
	{p:'strokeWidth',d:1},
	{p:'strokeColor',d:'#000'},
	{p:"strokeOpacity",d:1},
	{p:"fillColor",d:"blue"},
	{p:"fillOpacity",d:0.5},
	{p:'radius',d:5,tt:"Size of the map points"},
	{p:'scaleRadius',ex:"true",tt:'Scale the radius based on # points shown'},
	{p:'radiusScale',ex:"value,size,value,size e.g.: 10000,1,8000,2,5000,3,2000,3,1000,5,500,6,250,8,100,10,50,12",tt:'Radius scale'},
	{p:'maxRadius',ex:"16",d:1000},
	{p:'shape',d:'circle',ex:'plane|star|cross|x|square|triangle|circle|lightning|church',tt:'Use shape'},
	{p:'markerIcon',ex:"/icons/..."},
	{p:'iconSize',ex:16},
	{p:'justOneMarker',ex:"true",tt:'This is for data that is all at one point and you want to support selecting points for other displays'},	
	{p:'showPoints',ex:'true',tt:'Also show the map points when showing heatmap or glyphs or vectors'},
	{p:'applyPointsToVectors',d:true,tt:'If false then just show any attached map vectors without coloring them from the points'},
	{p:'bounds',ex:'north,west,south,east',tt:'initial bounds'},
	{p:'gridBounds',ex:'north,west,south,east'},	
	{p:'mapCenter',ex:'lat,lon',tt:"initial position"},
	{p:'zoomLevel',ex:4,tt:"initial zoom"},
	{p:'zoomTimeout',ex:500,tt:"initial zoom timeout delay. set this if the map is in tabs, etc, and not going to the initial zoom"},
	{p:'fixedPosition',ex:true,tt:'Keep the initial position'},
	{p:'linked',ex:true,tt:"Link location with other maps"},
	{p:'linkGroup',ex:'some_name',tt:"Map groups to link with"},
	{p:'initialLocation', ex:'lat,lon',tt:"initial location"},
	{p:'defaultMapLayer',ex:'ol.openstreetmap|esri.topo|esri.street|esri.worldimagery|esri.lightgray|esri.physical|opentopo|usgs.topo|usgs.imagery|usgs.relief|osm.toner|osm.toner.lite|watercolor'},
//	{p:'mapLayers',ex:'ol.openstreetmap,esri.topo,esri.street,esri.worldimagery,esri.lightgray,esri.physical,opentopo,usgs.topo,usgs.imagery,usgs.relief,osm.toner,osm.toner.lite,watercolor'},
	{p:'extraLayers',tt:'comma separated list of layers to display',
	 ex:'baselayer:goes-visible,baselayer:nexrad,geojson:US States:/resources/usmap.json:fillColor:transparent'},

	{p:'doPopup', ex:'false',tt:"Don't show popups"},
	{p:'doPopupSlider', ex:'true',tt:"Do the inline popup that slides down"},
	{p:'popupSliderRight', ex:'true',tt:"Position the inline slider to the right"},	
	{p:'popupSliderStyle', ex:'max-width:300px;overflow-x:auto;',tt:""},	
	{p:'labelField',ex:'',tt:'field to show in TOC'},
	{p:'showRegionSelector',ex:true},
	{p:'regionSelectorLabel'},	
	{p:'centerOnFilterChange',ex:true,tt:'Center map when the data filters change'},
	{p:'centerOnHighlight',ex:true,tt:'Center map when a record is highlighted'},
	{p:'centerOnMarkersAfterUpdate',ex:true,tt:'Always center on the markers'},	
	{p:'doInitCenter',tt:'Center the maps on initialization'},
	{p:'boundsAnimation',ex:true,tt:'Animate when map is centered'},
	{p:'iconField',ex:'""',tt:'Field id for the image icon url'},
	{p:'rotateField',ex:'""',tt:'Field id for degrees rotation'},	

	{label:"Map GUI"},
	{p:'showTableOfContents',ex:'true',tt:'Show left table of contents'},
	{p:'tableOfContentsTitle'},
	{p:'showMarkersToggle',ex:'true',tt:'Show the toggle checkbox for the marker layer'},
	{p:'showMarkersToggleLabel',ex:'label',tt:'Label to use for checkbox'},
	{p:'showClipToBounds',ex:'true',tt:'Show the clip bounds checkbox'},
	{p:'clipToBounds',ex:'true',tt:'Clip to bounds'},	
	{p:'showMarkers',ex:'false',tt: 'Hide the markers'},
	{p:'acceptEntryMarkers',ex:'true',d:false,tt:'If other maps can add entry markers and boxes'},

	{label:'Map Highlight'},
	{p:'showRecordSelection',ex:'false',d:'true'},
	{p:'highlight',ex:'true',tt:"Show mouse over highlights"},
	{p:'displayDiv',tt:'Div id to show highlights in'},
	{p:'showRecordHighlight',d:true},
	{p:'recordHighlightFeature',ex:'true',tt:'If there is a vector map that is being shown then highlight the map feature instead of drawing a point'},
	{p:'recordHighlightShape',ex:'circle|star|cross|x|square|triangle|circle|lightning|rectangle'},
	{p:'recordHighlightRadius',ex:'20',tt:'Radius to use to show other displays highlighted record'},
	{p:'recordHighlightStrokeWidth',ex:'2',tt:'Stroke to use to show other displays highlighted record'},
	{p:'recordHighlightStrokeColor',ex:'red',tt:'Color to use to show other displays highlighted record'},
	{p:'recordHighlightFillColor',ex:'rgba(0,0,0,0)',tt:'Fill color to use to show other displays highlighted record'},
	{p:'recordHighlightFillOpacity',ex:'0.5',tt:'Fill opacity to use to show other displays highlighted record'},
	{p:'recordHighlightVerticalLine',tt:'Draw a vertical line at the location of the selected record'},
	{p:'highlightColor',ex:'#ccc',tt:''},
	{p:'highlightFillColor',ex:'#ccc',tt:''},	
	{p:'highlightStrokeWidth',ex:'2',tt:''},	
	{p:'unhighlightColor',ex:'#ccc',tt:'Fill color when records are unhighlighted with the filters'},
	{p:'unhighlightStrokeWidth',ex:'1',tt:'Stroke width for when records are unhighlighted with the filters'},
	{p:'unhighlightStrokeColor',ex:'#aaa',tt:'Stroke color for when records are unhighlighted with the filters'},
	{p:'unhighlightRadius',d:-1,ex:'1',tt:'Radius for when records are highlighted with the filters'},

	{label:"Map Collisions"},
	{p:'handleCollisions',ex:'true',tt:"Handle point collisions"},
	{p:'collisionFixed',canCache:true,d:false,ex:'false',tt:"Always show markers",canCache:true},
	{p:'collisionMinPixels',d:16,ex:'16',tt:"How spread out",canCache:true},
	{p:'collisionDotColor',d:'blue',tt:"Color of dot drawn at center",canCache:true},
	{p:'collisionDotColorOn',canCache:true},
	{p:'collisionDotColorOff',canCache:true},		
	{p:'collisionDotRadius',d:6,tt:"Radius of dot drawn at center",canCache:true},
	{p:'collisionScaleDots',ex:'false',d:true,tt:"Scale the group dots",canCache:true},
				
	{p:'collisionLineColor',ex:'red',tt:"Color of line drawn at center",canCache:true},
	{p:'collisionIcon',ex:'/icons/...',tt:"Use an icon for collisions",canCache:true},
	{p:'collisionIconSize',d:16,ex:'16',canCache:true},
	{p:'collisionTooltip',ex:'${default}',tt:"Tooltip to use for collision dot",canCache:true},


	{label:"Map Lines"},
	{p:'showSegments',ex:'true',tt:'If data has 2 lat/lon locations draw a line'},
	{p:'segmentWidth',d:'1',tt:'Segment line width'},	
	{p:'useGreatCircle',d:false,ex:'true',tt:'use great circle routes for segments'},
	{p:'sizeSegments',d:false,ex:'true',tt:'Size the segments based on record value'},	
	{p:'isPath',ex:'true',tt:'Make a path from the points'},	
	{p:'pathWidth',ex:'2'},
	{p:'pathColor',ex:'red'},	
	{p:'isTrajectory',ex:'true',tt:'Make a path from the points'},	
	{p:'showPathEndPoint',ex:true},
	{p:'pathEndPointShape',ex:'arrow'},
	{p:'latField1',tt:'Field id for segments'},
	{p:'lonField1',tt:'Field id for segments'},
	{p:'latField2',tt:'Field id for segments'},
	{p:'lonField2',tt:'Field id for segments'},
	{p:'trackUrlField',ex:'field id',tt:'The data can contain a URL that points to data'},

	{label:"Map Labels"},
	{p:"labelTemplate",ex:"${field}",tt:"Display labels in the map"},
	{p:"labelKeyField",ex:"field",tt:"Make a key, e.g., A, B, C, ... based on the value of the key field"},	
	{p:"labelLimit",ex:"1000",tt:"Max number of records to display labels"},
  	{p:"doLabelGrid",ex:"true",tt:"Use a grid to determine if a label should be shown"},		
	{p:"labelFontColor",ex:"#000"},
	{p:"labelFontSize",ex:"12px"},
	{p:"labelFontFamily",ex:"'Open Sans', Helvetica Neue, Arial, Helvetica, sans-serif"},
	{p:"labelFontWeight",ex:"plain"},
	{p:"labelAlign",ex:"l|c|r t|m|b"},
	{p:"labelXOffset",ex:"0"},
	{p:"labelYOffset",ex:"0"},
	{p:"labelOutlineColor",ex:"#fff"},
	{p:"labelOutlineWidth",ex:"0"},


	{label:'Map Glyphs'},
	{p:'doGridPoints',ex:'true',tt:'Display a image showing shapes or bars',canCache:true},
	{p:'gridWidth',ex:'800',tt:'Width of the canvas'},
	{label:'label glyph',p:"glyph1",ex:"type:label,pos:sw,dx:10,dy:-10,label:field_colon_ ${field}_nl_field2_colon_ ${field2}"},
	{label:'rect glyph', p:"glyph1",ex:"type:rect,pos:sw,dx:10,dy:0,colorBy:field,width:150,height:100"},
	{label:'circle glyph',p:"glyph1",ex:"type:circle,pos:n,dx:10,dy:-10,fill:true,colorBy:field,width:20,baseWidth:5,sizeBy:field"},
	{label:'3dbar glyph', p:"glyph1",ex:"type:3dbar,pos:sw,dx:0,dy:0,height:30,width:6,baseHeight:5,sizeBy:field"},
	{label:'gauge glyph',p:"glyph1",ex:"type:gauge,color:#000,pos:sw,width:50,height:50,dx:10,dy:-10,sizeBy:field,sizeByMin:0"},

	{label:'Hex map, Voronoi, etc'},
	{p:'mapType',ex:'hex or triangle or square or voronoi',tt:'Create a hex or triangle or square or voronoi map'},
	{p:'doHexmap',ex:'true',tt:'Create a hexmap'},
	{p:'doTrianglemap',ex:'true',tt:'Create a triangle map'},		
	{p:'doSquaremap',ex:'true',tt:'Create a square map'},		
	{p:'hexmapShowCount',ex:'true',tt:'Display the count'},
	{p:'hexmapUseFullBounds',d:true,tt:'When filtering is the original fill bounds used'},
	{p:'hexmapPadding',tt:'Percent to bad out the bounding box',d:0.05},
	{p:'hexmapCellSide',tt:'How many on a side',d:25},
	{p:'hexmapUnits',tt:'Side units',d:'miles'},
	{p:'hexmapStrokeColor',d:'blue'},
	{p:'hexmapStrokeWidth',d:1},
	{p:'hexmapStrokeOpacity',d:1.0},
	{p:'hexmapStrokeDashstyle',d:'solid'},
	{p:'hexmapFillColor',d:'transparent'},
	{p:'hexmapFillOpacity',d:1.0},	
	{p:'hexmapColorBy',tt:'Field id to color the hexmap by'},
	{p:'hexmapEmptyStrokeColor'},
	{p:'hexmapEmptyStrokeWidth'},
	{p:'hexmapEmptyFillColor'},
	{p:'doVoronoi',ex:'true',tt:'Create  voronoi polygons'},
	{p:'voronoiColorBy',tt:'Field id to color the hexmap by'},
	{p:'voronoiStrokeColor',d:'blue'},
	{p:'voronoiStrokeWidth',d:1},
	{p:'voronoiStrokeOpacity',d:1},	
	{p:'voronoiStrokeDashstyle',d:'solid'},
	{p:'voronoiFillColor',d:'transparent'},
	{p:'voronoiFillOpacity',d:1.0},	
	{label:'Heatmap'},
	{p:'doHeatmap',ex:'true',tt:'Grid the data into an image',canCache:true},
	{p:'hmShowPoints',ex:'true',tt:'Also show the map points'},
	{p:'hmShowReload',ex:'true',tt:''},
	{p:'hmShowGroups',ex:'true',tt:''},
	{p:'hmBounds',ex:'north,west,south,east',tt:''},
	{p:'htmlLayerField'},
	{p:'htmlLayerShape',ex:'barchart|piechart'},	
	{p:'htmlLayerWidth',ex:'30'},
	{p:'htmlLayerHeight',ex:'15'},
	{p:'htmlLayerStyle',ex:'css style'},
	{p:'htmlLayerScale',ex:'2:0.75,3:1,4:2,5:3,6:4,7:6',tt:'zoomlevel:scale,...'},
	{p:'cellShape',ex:'rect|3dbar|circle|vector'},
	{p:'cellColor',ex:'color'},
	{p:'cellFilled',ex:true},
	{p:'cellSize',ex:'8'},
	{p:'cellSizeH',ex:'20',tt:'Base value to scale by to get height'},
	{p:'cellSizeHBase',ex:'0',tt:'Extra height value'},
	{p:'arrowLength',d:-1,canCache:true},
	{p:'lineWidth',d:1,canCache:true},	
	{p:'angleBy',ex:'field',tt:'field for angle of vectors'},
	{p:'hmOperator',ex:'count|average|min|max'},
	{p:'hmAnimationSleep',ex:'1000'},
	{p:'hmReloadOnZoom',ex:'true'},
	{p:'reloadOnZoom',ex:'true'},	
	{p:'hmGroupByDate',ex:'true|day|month|year|decade',tt:'Group heatmap images by date'}, 
	{p:'hmGroupBy',ex:'field id',tt:'Field to group heatmap images'}, 
	{p:'hmLabelPrefix'},
	{p:'hmShowToggle',ex:true,tt:'Show the toggle checkbox to turn off/on the heatmap'},
	{p:'hmToggleLabel'},
	{p:'boundsScale',ex:'0.1',tt:'Scale up the map bounds'},
	{p:'hmFilter',ex:'average5|average9|average25|gauss9|gauss25',tt:'Apply filter to image'},
	{p:'hmFilterPasses',ex:'1'},
	{p:'hmFilterThreshold',ex:'1'},
	{p:'hmCountThreshold',ex:'1'},
    ];
    
    myProps.push({label:'Canvas'});
    myProps.push(...RamaddaDisplayUtils.getCanvasProps());
    myProps.push(...RamaddaDisplayUtils.sparklineProps);


    displayDefineMembers(this, myProps, {
        mapBoundsSet: false,
        features: [],
        myMarkers: {},
        mapEntryInfos: {},
	tracks:{},
	checkFinished: function() {
	    return true;
	},
        initDisplay: function() {
            SUPER.initDisplay.call(this);
            let _this = this;
	    let legendSide = this.getProperty("sizeByLegendSide");
	    if(legendSide) {
		let legend = HU.div([ID,this.domId(ID_SIZEBY_LEGEND)]);
		if(legendSide=="top") {
		    this.jq(ID_TOP).append(legend);
		} else if(legendSide=="left") {
		    this.jq(ID_LEFT).append(legend);
		} else if(legendSide=="right") {
		    this.jq(ID_RIGHT).append(legend);
		} else if(legendSide=="bottom") {
		    this.jq(ID_BOTTOM).append(legend);
		} else {
		    console.log("Unknown legend side:" + legendSide);
		}
	    }
	    this.startProgress();
        },
        checkLayout: function() {
            if (!this.map) {
                return;
            }
            let d = this.jq(ID_MAP);
            if (d.width() > 0 && this.lastWidth != d.width() && this.map) {
                this.lastWidth = d.width();
                this.map.getMap().updateSize();
            }
	    if(!this.setMapLocationAndZoom && this.mapParams) {
		this.setMapLocationAndZoom = true;
		if(this.mapParams.initialZoom>=0) {
		    this.map.getMap().zoomTo(this.mapParams.initialZoom);
		}
		if(this.mapParams.initialLocation) {
		    let loc = MapUtils.createLonLat(this.mapParams.initialLocation.lon, this.mapParams.initialLocation.lat);
		    this.map.setCenter(loc);
		}

	    }
        },

	handlePopup: function(feature, popup) {
	    if(!this.trackUrlField) return;
	    let func = ()=>{
		if(feature.record) {
		    if(this.tracks[feature.record.getId()]) {
			this.removeTrack(feature.record);
		    } else {
			let url = feature.record.getValue(this.trackUrlField.getIndex());
			$.getJSON(url, data=>{this.loadTrack(feature.record, data)}).fail(err=>{console.log("url failed:" + url +"\n" + err)});
		    }
		}
	    };
	    this.jq(ID_TRACK_VIEW).click(func);
	    this.jq(ID_TRACK_VIEW+"_1").click(func);	    
	},
	macroHook: function(record, token,value) {
	    if(!this.trackUrlField) {
		return null;
	    }
	    if(token.tag!=this.trackUrlField.getId()) {
		return null;
	    }
	    if(String(value).trim().length==0) return "";
	    this.currentPopupRecord = record;
	    let haveTrack = this.tracks[record.getId()]!=null;
	    let label =haveTrack?"Remove track":(token.attrs["label"] ||  "View track");
	    return SPACE + HU.span([CLASS,"ramadda-clickable",ID,this.domId(ID_TRACK_VIEW)],label);
	}, 
	getRecordUrlHtml: function(attrs, field, record) {
	    this.currentPopupRecord = record;
	    if(!this.trackUrlField || this.trackUrlField.getId()!=field.getId()) {
		return SUPER.getRecordUrlHtml.call(this, attrs, field, record);
	    }
	    let value = record.getValue(field.getIndex());
	    let haveTrack = this.tracks[record.getId()]!=null;
	    let label = haveTrack?"Remove track":(attrs[field.getId()+".label"] || "View track");
	    return  HU.span([CLASS,"ramadda-clickable",ID,this.domId(ID_TRACK_VIEW+"_1")],label);
	},
	removeTrack:function(record) {
	    if(this.tracks[record.getId()]) {
		this.map.removePolygon(this.tracks[record.getId()]);
		this.tracks[record.getId()] = null;
	    }
	    this.jq(ID_TRACK_VIEW).html("View track");
	    this.jq(ID_TRACK_VIEW+"_1").html("View track");
	    let item = this.jq(ID_LEFT).find(HU.attrSelect(RECORD_ID,record.getId()));
	    item.removeClass("display-map-toc-item-on");
	},
	loadTrack: function(record, data) {
            let newData = makePointData(data, null,this,"");
	    let points = RecordUtil.getPoints(newData.getRecords(),{});
	    let feature = this.markers?this.markers[record.getId()]:null;
	    let item = this.jq(ID_LEFT).find(HU.attrSelect(RECORD_ID,record.getId()));
	    record.trackData = newData;
	    item.addClass("display-map-toc-item-on");
	    try {
		record.setLocation(points[0].y, points[0].x);
		let loc =  MapUtils.createLonLat(points[0].x, points[0].y);
		loc = this.map.transformLLPoint(loc);
		if(feature)
		    feature.move(loc);
	    } catch(err) {
		console.log(err);
	    }

	    let bounds = {};
	    let attrs = {
		strokeColor:this.getStrokeColor("blue"),
		strokeWidth:this.getStrokeWidth(1),
		fillColor:this.getProperty("fillColor") ||'transparent',
	    };
            let polygon = this.map.addPolygon("", "", points, attrs);
	    polygon.record = record;
	    this.tracks[record.getId()]=polygon;
	    if(polygon.geometry) {
		this.map.zoomToExtent(polygon.geometry.getBounds());
	    }
	    this.map.closePopup();
	    setTimeout(()=>{
		this.getDisplayManager().notifyEvent(DisplayEvent.dataSelection, this, {data:newData});
	    },100);
	},
	applyToFeatureLayers:function(func) {
	    if(this.myFeatureLayer) func(this.myFeatureLayer);
	    if(this.myFeatureLayerNoSelect) func(this.myFeatureLayerNoSelect);
	},
	addFeatures:function(features,noSelect) {
	    let madeNewOne = false;
	    let layer = noSelect?this.myFeatureLayerNoSelect:this.myFeatureLayer;
	    if(!layer) {
		if(debugit && ycnt!=this.myycnt) {
		    console.trace("addFeatures",this.myName);
		}
		if(noSelect) 
		    layer = this.myFeatureLayerNoSelect = this.map.createFeatureLayer("Map Features NS",false);		
		else
		    layer = this.myFeatureLayer = this.map.createFeatureLayer("Map Features",true);
		if(Utils.isDefined(this.layerVisible)) {
		    layer.setVisibility(this.layerVisible);
		}
		if(this.getProperty("showMarkersToggle") && !this.getProperty("markersVisibility", true)) {
		    this.applyToFeatureLayers(layer=>{layer.setVisibility(false);});
		}
		this.myFeatures= [];
	    }
	    layer.addFeatures(features);
	    features.forEach(feature=>{
		feature.layer = layer;
		this.myFeatures.push(feature);
	    });
	},

	removeFeature: function(feature) {
	    if(feature) {
		this.applyToFeatureLayers(layer=>{
		    layer.removeFeatures([feature]);
		});
	    }
	},

	removeExtraLayers: function() {
	    try {
		this.extraLayers.every(layer=>{
		    layer.destroy();
		    this.map.removeLayer(layer,true);
		    return true;
		});
	    } catch(exc) {
		console.log(exc);
	    }
	    this.extraLayers = [];
	    this.heatmapLayers = [];
	    this.voronoiLayer=null;
	    this.hexmapLayer=null;
	},

	toString: function() {
	    return "displaymap";
	},
	deleteDisplay: function() {
	    if(debugit)
	       console.log("delete:" + this.myName);
	    this.getDisplayManager().removeDisplay(this);
	    this.removeFeatures();
	    this.displayDeleted = true;
            this.map.getMap().events.unregister("updatesize", this,this.updatesizeFunc);
            this.map.getMap().events.unregister("moveend", this,this.moveendFunc);
            this.map.getMap().events.unregister("zoomend", this,this.zoomendFunc);	    	    
            let pointData = this.getPointData();
	    if(pointData) {
		pointData.removeFromCache(this);
	    }
	},

	removeFeatures: function() {
	    this.removeFeatureLayer();
	    this.removeExtraLayers();
	},


	setVisible: function(visible) {
	    this.layerVisible = visible;
	    let show = true;
	    if(this.jq("showMarkersToggle").length>0) {
		show = this.jq("showMarkersToggle").is(":checked");
	    }
	    if(show) {
		if(this.myFeatureLayer) {
		    this.myFeatureLayer.setVisibility(visible);
//		    console.log("setvisible:" + visible+" "+ this.myFeatureLayer.getVisibility());
		}
		if(this.myFeatureLayerNoSelect) {
		    this.myFeatureLayerNoSelect.setVisibility(visible);
		}
	    }
	    //TODO: have my own labelLayer
	    if(this.map.labelLayer)
		this.map.labelLayer.setVisibility(visible);
	    this.extraLayers.forEach(layer=>{
		layer.setVisibility(visible);
	    });

	},

	removeFeatureLayer: function() {
	    if(debugMapTime)
		console.time('removeFeatureLayer');
	    if(this.myFeatureLayer) {
		this.myFeatureLayer.destroy();
		this.map.removeLayer(this.myFeatureLayer,true);
		this.myFeatureLayer = null;
	    }
	    if(this.myFeatureLayerNoSelect) {
		this.myFeatureLayerNoSelect.destroy();
		this.map.removeLayer(this.myFeatureLayerNoSelect,true);
		this.myFeatureLayerNoSelect = null;
	    }	    
	    if(this.labelFeatures) {
		this.map.labelLayer.removeFeatures(this.labelFeatures,{silent:true});
		this.labelFeatures = null;
		this.jq("legendid").html("");
	    }
	    if(debugMapTime)
		console.timeEnd('removeFeatureLayer');
	    this.myFeatures= null;
	},

        initMapParams: function(params) {
	    SUPER.initMapParams.call(this,params);
	    if(this.getDoPopupSlider() || this.getPopupSliderRight()) {
		params.doPopupSlider = true;
		if(this.getPopupSliderRight()) {
		    params.popupSliderRight = true;
		}
	    }
	},
	initMap: function(map) {
	    if(!this.getShowMarkers(this.getProperty("markersVisibility", true))) {
		map.getMarkersLayer().setVisibility(false);
	    }
	    let boundsAnimation = this.getProperty("boundsAnimation");
	    if(boundsAnimation) {
		this.didAnimationBounds = false;
                let animationBounds = boundsAnimation.split(",");
                if (animationBounds.length == 4) {
		    let pause = parseFloat(this.getProperty("animationPause","1000"));
		    HU.callWhenScrolled(this.domId(ID_MAP),()=>{
			if(_this.didAnimationBounds) {
			    return;
			}
			_this.didAnimationBounds = true;
			let a = animationBounds;
			let b = MapUtils.createBounds(parseFloat(a[1]),parseFloat(a[2]),parseFloat(a[3]),parseFloat(a[0]));
			_this.map.animateViewToBounds(b);
		    },pause);
		}
            }
	},
        createMap: function() {
	    SUPER.createMap.call(this);
            let _this = this;
	    if(!this.getShowMarkers(this.getProperty("markersVisibility", true))) {
		this.map.getMarkersLayer().setVisibility(false);
	    }

            if (this.doDisplayMap()) {
                this.map.setDefaultCanSelect(false);
            }
            this.map.initMap(false);
            this.map.addRegionSelectorControl(function(bounds) {
		_this.propagateEvent(DisplayEvent.mapBoundsChanged, {"bounds": bounds,    "force": true});
            },true);
	    this.map.popupHandler = (feature,popup) =>{
		this.handlePopup(feature, popup);
	    };
	    this.map.addFeatureSelectHandler(feature=>{
		if(debugPopup) console.log("\tdisplaymap: featureSelectHandler");
		let didSomething= false;
		let record = feature.record;
		if(feature.collisionInfo)  {
		    if(debugPopup) console.log("has collisioninfo");
		    feature.collisionInfo.dotSelected(feature);
		    return true;
		}
		if(record) {
		    this.propagateEventRecordSelection({record:record});
		    this.propagateFilterFields(record);
//		    didSomething= true;
		}

		if(record && !this.getMap().getDoPopup() && this.getShowRecordSelection()) {
		    if(debugPopup) console.log("highlighting point");
		    this.highlightPoint(record.getLatitude(),record.getLongitude(),true,false);
//		    didSomething= true;
		}

		if(record && this.getProperty("shareSelected")) {
		    let idField = this.getFieldById(null,"id");
		    if(idField) {
			ramaddaDisplaySetSelectedEntry(record.getValue(idField.getIndex()),this.getDisplayManager().getDisplays(),this);
		    }
		    if(debugPopup) console.log("\tdisplaymap: share selected");
//		    didSomething= true;
		}
		if(didSomething)
		    this.lastFeatureSelectTime = new Date();
		return false;
	    });

            this.map.addFeatureHighlightHandler((feature, highlight)=>{
		let record = feature.record;
		if(record) {
		    if(this.lastHighlightedRecord) {
			let args = {highlight:false,record: this.lastHighlightedRecord};
			this.getDisplayManager().notifyEvent(DisplayEvent.recordHighlight, this, args);
			if (this.getAnimationEnabled()) {
			    this.getAnimation().handleEventRecordHighlight(this, args);
			}
			this.lastHighlightedRecord = null;
		    }
		    if(highlight) {
			this.lastHighlightedRecord = record;
		    }
		    let args = {highlight:highlight,record: record};
		    this.getDisplayManager().notifyEvent(DisplayEvent.recordHighlight, this, args);
		    if (this.getAnimationEnabled()) {
			this.getAnimation().handleEventRecordHighlight(this, args);
		    }
		}

	    });

	    this.map.highlightBackgroundColor=this.getProperty("highlighBackgroundColor","#fff");
	    if(this.getProperty("addEntryMarkers")) {
		this.map.setDoPopup(true);
	    } else {
		this.map.setDoPopup(this.getProperty("doPopup",true));
	    }
	    this.createTime = new Date();

            this.map.addClickHandler(this.domId(ID_LONFIELD), this
				     .domId(ID_LATFIELD), null, this);

            this.map.getMap().events.register("updatesize", this, this.updatesizeFunc=()=>{
		if(!this.callingUpdateSize) {
		    _this.updateHtmlLayers();
		}
            });

	    //register the events in a bit
	    setTimeout(()=>{
		this.registerEvents();
	    },1000);



	    
	    let boundsAnimation = this.getProperty("boundsAnimation");
	    if(boundsAnimation) {
		this.didAnimationBounds = false;
                let animationBounds = boundsAnimation.split(",");
                if (animationBounds.length == 4) {
		    let pause = parseFloat(this.getProperty("animationPause","1000"));
		    HU.callWhenScrolled(this.domId(ID_MAP),()=>{
			if(_this.didAnimationBounds) {
			    return;
			}
			_this.didAnimationBounds = true;
			let a = animationBounds;
			let b = MapUtils.createBounds(parseFloat(a[1]),parseFloat(a[2]),parseFloat(a[3]),parseFloat(a[0]));
			_this.map.animateViewToBounds(b);
		    },pause);
		}
            }


            let currentFeatures = this.features;
            this.features = [];
            for (let i = 0; i < currentFeatures.length; i++) {
                this.addFeature(currentFeatures[i]);
            }
	    if(this.getProperty("displayEntries",true)) {
		let entries = this.getDisplayManager().collectEntries();
		for (let i = 0; i < entries.length; i++) {
                    let pair = entries[i];
                    this.handleEventEntriesChanged(pair.source, pair.entries);
		}
	    }


	    if(this.getProperty("addEntryMarkers")) {
		this.getDisplayEntry(entry=>{
		    if(!entry) {
			console.log("failed to get entry");
			return;
		    }
		    entry.getChildrenEntries(entries=>{
			entries.forEach(entry=>{
			    if(!entry.hasLocation()) return;
			    let lonlat = MapUtils.createLonLat(entry.getWest(), entry.getNorth());
			    let html = "<b>" + entry.getName()+"</b>";
			    if(entry.isImage()) {
				html+="<br>" + HU.image(entry.getImageUrl(),["width","200px"]);
			    }
			    this.map.addMarker(entry.getId(), lonlat, entry.getIconUrl(),  "", html, null, 16);
			});
		    });
		});

	    }

            if (this.layerEntries) {
                let selectCallback = function(feature,layer,event) {
                    _this.handleLayerSelect(layer);
                }
                let unselectCallback = function(feature,layer,event) {
                    _this.handleLayerUnselect(layer);
                }
                let toks = this.layerEntries.split(",");
                for (let i = 0; i < toks.length; i++) {
                    let tok = toks[i];
                    let url = ramaddaBaseUrl + "/entry/show?output=shapefile.kml&entryid=" + tok;
                    this.map.addKMLLayer("layer", url, true, selectCallback, unselectCallback);
                    //TODO: Center on the kml
                }
            }

	    for(let markerIdx=1;true;markerIdx++) {
		let marker = this.getProperty("marker" + markerIdx);
		if(!marker) break;
		this.map.addMarkerEmbed(marker);
	    }
        },
	registerEvents:function() {
            this.getMap().getMap().events.register("zoomend", this, this.zoomendFunc = ()=>{
		if(this.callingUpdateUI) return;
		if(this.lastUpdateTime) {
		    let diff = (new Date().getTime())-this.lastUpdateTime.getTime()
		    if(diff<2000) {
			return
		    }
		}
		if(this.debugZoom) {
		    console.log("level:" + this.getMap().getMap().getZoom());
		}
		//		console.log(this.getLogLabel()+" zoomend:"+this.callingUpdateUI+" " + this.lastUpdateTime);
		if(this.pointLevelRange || this.glyphLevelRange) {
		    this.checkLevelRange([this.myFeatureLayer],true);
		}		

                this.mapBoundsChanged();
		this.checkHeatmapReload();
		this.updateHtmlLayers();
		if(!this.haveAddPoints) {
		    return;
		}

		if(this.getHandleCollisions()) {
		    if(this.lastZoom == this.map.getZoom()) {
			return;
		    }
		    //Wait a bit
		    if(this.lastCollisionTimeout) {
			clearTimeout(this.lastCollisionTimeout);
		    }

		    //		    console.log(this.getLogLabel()+" setting up collision timeout");
		    this.lastTimeout = setTimeout(()=>{
			this.haveCalledUpdateUI = false;
			//			console.log(this.getLogLabel()+" calling updateUI from handleCollisions");
			this.updateUI();
			this.lastCollisionTimeout = null;
		    },1000);
		}
		//		console.log(this.getLogLabel()+" finished zoomend");
            });
            this.map.getMap().events.register("moveend", this, this.moveendFunc = ()=> {
		if(this.map.doingPopup) return;
                this.mapBoundsChanged();
		this.checkHeatmapReload();
            });
	},
        getBounds: function() {
	    if(this.map)
		return this.map.getBounds();
	    return null;
	},
        mapFeatureSelected: function(layer) {
            if (!this.getPointData()) {
                return;
            }
            this.map.onFeatureSelect(layer);
            if (!Utils.isDefined(layer.feature.record)) {
                return;
            }
            this.propagateEventRecordSelection({
                record: layer.feature.record
            });
        },
	showVectorLayer:true,
	toggleVectorLayer: function() {
	    this.showVectorLayer = !this.showVectorLayer;
            if(this.vectorLayer != null) {
		this.vectorLayer.setVisibility(this.showVectorLayer);
	    }
	    this.haveCalledUpdateUI = false;
	    this.updateUI();
	},
        doDisplayMap: function() {
            if (!this.getShowLayers()) return false;
            if (!this.getProperty("displayAsMap", true)) return false;
            if(this.getProperty("kmlLayer") || this.getProperty("geojsonLayer")) {
		if(this.getShowLayers()) {
		    return this.showVectorLayer;
		}
	    }
        },
        cloneLayer: function(layer) {
            let _this = this;
            this.map.hideLoadingImage();
            layer = layer.clone();
            let features = layer.features;
            let clonedFeatures = [];
            for (let j = 0; j < features.length; j++) {
                feature = features[j];
                feature = feature.clone();
                if (feature.style) {
                    oldStyle = feature.style;
                    feature.style = {};
                    for (let a in oldStyle) {
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
            this.map.addSelectCallback(layer, this.doDisplayMap(), function(feature,layer,event) {
                _this.mapFeatureSelected(layer);
            });
        },
        handleEventPointDataLoaded: function(source, pointData) {
        },
        handleEventFieldsSelected: function(source, fields) {
	    if(this.getProperty("selectedFieldIsColorBy") && fields.length>0) {
		this.colorByFieldChanged(fields[0]);
	    }
        },
        handleLayerSelect: function(layer) {
	    if(debugPopup) this.logMsg("handleLayerSelect");
            let args = this.layerSelectArgs;
            if (!this.layerSelectPath) {
                if (!args) {
                    this.map.onFeatureSelect(layer);
                    return;
                }
                //If args was defined then default to search
                this.layerSelectPath = "/search/do";
            }
            let url = ramaddaBaseUrl + this.layerSelectPath;
            if (args) {
                let toks = args.split(",");
                for (let i = 0; i < toks.length; i++) {
                    let tok = toks[i];
                    let toktoks = tok.split(":");
                    let urlArg = toktoks[0];
                    let layerField = toktoks[1];
                    let attrs = layer.feature.attributes;
                    let fieldValue = null;
                    for (let attr in attrs) {
                        let attrName = "" + attr;
                        if (attrName == layerField) {
                            let attrValue = null;
                            if (typeof attrs[attr] == 'object' || typeof attrs[attr] == 'Object') {
                                let o = attrs[attr];
                                attrValue = o["value"];
                            } else {
                                attrValue = attrs[attr];
                            }
                            url = HU.appendArg(url, urlArg, attrValue);
                            url = url.replace("${" + urlArg + "}", attrValue);
                        }
                    }
                }
            }
            url = HU.appendArg(url, "output", "json");
            let entryList = new EntryList(this.getRamadda(), url, null, false);
            entryList.doSearch(this);
            this.getEntryList().showMessage("Searching", HU.div([ATTR_STYLE, HU.css("margin","20px")], this.getWaitImage()));
        },
        getEntryList: function() {
            if (!this.entryListDisplay) {
                let props = {
                    showMenu: true,
                    showTitle: true,
                    showDetails: true,
                    layoutHere: false,
                    showForm: false,
                    doSearch: false,
                };
                let id = this.getUniqueId("display");
                this.entryListDisplay = new RamaddaEntrylistDisplay(this.getDisplayManager(), id, props);
                this.getDisplayManager().addDisplay(this.entryListDisplay);
            }
            return this.entryListDisplay;
        },
        entryListChanged: function(entryList) {
            let entries = entryList.getEntries();
            this.getEntryList().entryListChanged(entryList);
        },
        handleLayerUnselect: function(layer) {
            this.map.onFeatureUnselect(layer);
        },
        addMapLayer: function(source, props) {
            let _this = this;
            let entry = props.entry;
            if (!this.addedLayers) this.addedLayers = {};
            if (this.addedLayers[entry.getId()]) {
                let layer = this.addedLayers[entry.getId()];
                if (layer) {
                    this.map.removeKMLLayer(layer);
                    this.addedLayers[entry.getId()] = null;
                }
                return;
            }

            let type = entry.getType().getId();
            if (type == "geo_shapefile" || type == "geo_geojson") {
                let bounds = MapUtils.createBounds(entry.getWest(), entry.getSouth(), entry.getEast(), entry.getNorth());
                if (bounds.left < -180 || bounds.right > 180 || bounds.bottom < -90 || bounds.top > 90) {
                    bounds = null;
                }

                let selectCallback = function(feature,layer,event) {
		    if(debugPopup) this.logMsg("selectCallback");
                    _this.handleLayerSelect(layer);
                }
                let unselectCallback = function(feature,layer,event) {
                    _this.handleLayerUnselect(layer);
                }
                let layer;
                if (type == "geo_geojson") {
                    let url = entry.getRamadda().getEntryDownloadUrl(entry);
                    layer = this.map.addGeoJsonLayer(this.getProperty("geojsonLayerName"), url, this.doDisplayMap(), selectCallback, unselectCallback, null, null, true);
                } else {
                    let url = ramaddaBaseUrl + "/entry/show?output=shapefile.kml&entryid=" + entry.getId();
                    layer = this.map.addKMLLayer(entry.getName(), url, true, selectCallback, unselectCallback, null, null, true);
                }
                this.addedLayers[entry.getId()] = layer;
                return;
            }

            let baseUrl = entry.getAttributeValue("base_url");
            if (!Utils.stringDefined(baseUrl)) {
                console.log("No base url:" + entry.getId());
                return;
            }
            let layer = entry.getAttributeValue("layer_name");
            if (layer == null) {
                layer = entry.getName();
            }
            this.map.addWMSLayer(entry.getName(), baseUrl, layer, false);
        },
        mapBoundsChanged: function() {
            let bounds = this.map.getMap().calculateBounds().transform(this.map.sourceProjection,
								       this.map.displayProjection);
	    this.propagateEvent(DisplayEvent.mapBoundsChanged, {"bounds": bounds,    "force": false});
	    if(this.clipToView || this.getClipToBounds()) {
		if(this.lastUpdateTime) {
		    let now = new Date();
		    if(now.getTime()-this.lastUpdateTime.getTime()>1000) {
			this.haveCalledUpdateUI = false;
			this.clipBounds = true;
			this.updatingFromClip = true;
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
	    let style="";
	    if(!this.getProperty("showInnerContents",true)) {
		style+="display:none;";
	    }		

            let html =  HU.div([STYLE,style,ATTR_CLASS, "display-inner-contents", ID,
			   this.domId(ID_DISPLAY_CONTENTS)], "");
	    return html;
        },
	removeHighlight: function() {
	    if(this.highlightMarker)
		this.removeFeature(this.highlightMarker);
	},
	highlightPoint: function(lat,lon,highlight,andCenter) {
	    if(!this.getMap()) return;
	    this.removeHighlight();
	    if(!this.getShowRecordHighlight()) return;
	    if(highlight) {
		let point = MapUtils.createLonLat(lon,lat);
                let attrs = {
                    pointRadius: parseFloat(this.getProperty("recordHighlightRadius", +this.getPropertyRadius(6)+8)),
                    stroke: true,
                    strokeColor: this.getProperty("recordHighlightStrokeColor", "#000"),
                    strokeWidth: parseFloat(this.getProperty("recordHighlightStrokeWidth", 2)),
		    fillColor: this.getProperty("recordHighlightFillColor", "#ccc"),
		    fillOpacity: parseFloat(this.getProperty("recordHighlightFillOpacity", 0.5)),
                };
		if(this.getProperty("recordHighlightUseMarker",false)) {
		    let size = +this.getProperty("recordHighlightRadius", +this.getRadius(24));
		    this.highlightMarker = this.getMap().createMarker("pt-" + i, point, null, "pt-" + i,null,null,size);
		} else 	if(this.getProperty("recordHighlightVerticalLine",false)) {
		    let points = [];
                    points.push(MapUtils.createPoint(lon,0));
		    points.push(MapUtils.createPoint(lon,80));
                    this.highlightMarker = this.getMap().createPolygon(id, "highlight", points, attrs, null);
		} else {
		    attrs.graphicName = this.getProperty("recordHighlightShape");
		    this.highlightMarker =  this.getMap().createPoint("highlight", point, attrs);
		}
		if(this.highlightMarker) this.addFeatures([this.highlightMarker]);
		if(andCenter && this.getProperty("centerOnHighlight",false)) {
		    this.getMap().setCenter(point);
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
	propagateFilterFields: function(record) {
	    let fields = this.getFieldsByIds(null, this.getProperty("filterFieldsToPropagate"));
	    fields.map(field=>{
		let args = {
		    fieldId:field.getId(),
		    value:record.getValue(field.getIndex())
		};
		this.propagateEvent(DisplayEvent.filterChanged, args);
	    });
	},	    
	doneLocation:false,
        handleClick: function(theMap, event, lon, lat) {
	    if(event.shiftKey && event.metaKey) {
		if(!this.doneLocation) {
		    if(Utils.isAnonymous()) {
			console.log("latitude,longitude");
		    } else {
			console.log("latitude,longitude,address,city,state,zip,country");
		    }
		    this.doneLocation = true;
		}
		let loc = lat+"," + lon;
		if(Utils.isAnonymous()) {
                    let point= this.map.addPoint("", MapUtils.createLonLat(lon, lat));
		    Utils.copyToClipboard(loc+"\n");
		    console.log(loc);
		} else  {
		    let url = ramaddaBaseUrl +"/map/getaddress?latitude=" + lat +"&longitude=" + lon;
		    $.getJSON(url, data=>{
			if(data.length==0) {
			    console.log(loc+",,,,,");
			} else {
			    let comp = data[0];
			    let point= this.map.addPoint("", MapUtils.createLonLat(lon, lat),null,comp.address);
			    console.log(loc+"," + comp.address+"," + comp.city +"," + comp.state +"," + comp.zip +"," + comp.country); 
			}
		    }).fail(err=>{
			console.log(loc+",,,,,");
		    });
		}
		return;
	    }

	    let debug = false;
	    if(debug)   console.log("click");
	    if(this.lastFeatureSelectTime) {
		let diff = new Date().getTime()-this.lastFeatureSelectTime.getTime();
		this.lastFeatureSelectTime = null;
		if(diff<1000) {
		    if(debug)   console.log("\tclick: lastFeatureSelectTime:" + diff)
		    return;
		}
	    }

	    if(event.shiftKey) {
		if(Utils.isAnonymous()) return;
		let text = prompt("Marker text", "");
		if(!text) return;
		let url = ramaddaBaseUrl +"/metadata/addform?entryid=" + this.getProperty("entryId")+"&metadata_type=map_marker&metadata_attr1=" +
		    encodeURIComponent(text) +"&metadata_attr2=" + lat +"," + lon; 
		window.location = url;
		if(debug) console.log("\tclick:shift");
		return
	    }

            if (!this.map) {
		if(debug)    console.log("\tclick:no map")
                return;
            }
            if (this.doDisplayMap()) {
		if(debug)    console.log("\tclick: no display map")
                return;
            }

            let justOneMarker = this.getJustOneMarker(false);
            if(justOneMarker) {
		if(debug)    console.log("\tclick: just one")
                let pointData = this.getPointData();
                if(pointData) {
                    pointData.handleEventMapClick(this, this, lon, lat);
		    this.getDisplayManager().notifyEvent("mapClick", this, {lat:lat,lon:lon});
                }
            }


	    if(!this.records) {
		if(debug)    console.log("\tclick: no records")
		return;
	    }
	    let indexObj = [];
            let closest = RecordUtil.findClosest(this.records, lon, lat, indexObj);
            if (!closest) {
		if(debug)    console.log("\tclick: no closest")
		return;
	    }
	    if(debug)    console.log("\tclick: handling")
	    this.propagateEventRecordSelection({record: closest});

	    //If we are highlighting a record then change the marker
	    if(this.highlightMarker) {
		this.highlightPoint(closest.getLatitude(),closest.getLongitude(),true,false);
	    }
	    
	    this.propagateFilterFields(closest);
        },

        getPosition: function() {
            let lat = $("#" + this.domId(ID_LATFIELD)).val();
            let lon = $("#" + this.domId(ID_LONFIELD)).val();
            if (lat == null)
                return null;
            return [lat, lon];
        },

        haveInitBounds: false,
        setInitMapBounds: function(north, west, south, east) {
            if (!this.map) return;
            if (this.haveInitBounds) return;
	    this.lastUpdateTime = new Date();
            this.haveInitBounds = true;
	    if(this.getProperty("doInitCenter",true)) {
		this.map.centerOnMarkers(MapUtils.createBounds(west, south, east,
							       north),true);
	    }
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
            let oldEntries = this.sourceToEntries[source.getId()];
            if (oldEntries != null) {
                for (let i = 0; i < oldEntries.length; i++) {
                    let id = source.getId() + "_" + oldEntries[i].getId();
                    this.addOrRemoveEntryMarker(id, oldEntries[i], false);
                }
            }

            this.sourceToEntries[source.getId()] = entries;
            let markers = MapUtils.createLayerMarkers("Markers",{});
            let lines =  MapUtils.createLayerVector("Lines", {});
            let north = -90,
                west = 180,
                south = 90,
                east = -180;
            let didOne = false;
            for (let i = 0; i < entries.length; i++) {
                let entry = entries[i];
                let id = source.getId() + "_" + entry.getId();
                let mapEntryInfo = this.addOrRemoveEntryMarker(id, entries[i], true);
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
            let bounds = (didOne ? MapUtils.createBounds(west, south, east, north) : null);
            //debug                    this.map.centerOnMarkers(bounds, true);
        },
        handleEventEntrySelection: function(source, args) {
            if (!this.map) {
                return;
            }
            let _this = this;
            let entry = args.entry;
            if (entry == null) {
                this.map.clearSelectionMarker();
                return;
            }
            let selected = args.selected;

            if (!entry.hasLocation()) {
                return;
            }
        },
        addOrRemoveEntryMarker: function(id, entry, add, args) {
	    if(!this.getAcceptEntryMarkers()) {
		return
	    }
            if (!args) {
                args = {};
            }
            let dflt = {
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

            let mapEntryInfo = this.mapEntryInfos[id];
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
                        let attrs = {};
                        mapEntryInfo.rectangle = this.map.addRectangle(id,
								       entry.getNorth(), entry.getWest(), entry
								       .getSouth(), entry.getEast(), attrs);
                    }
                    let latitude = entry.getLatitude();
                    let longitude = entry.getLongitude();
                    if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                        return;
                    }
                    let point = MapUtils.createLonLat(longitude, latitude);

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
                        let points = []
                        for (let i = 0; i < entry.polygon.length; i += 2) {
                            points.push(MapUtils.createPoint(entry.polygon[i + 1], entry.polygon[i]));
                        }
                        let attrs = {
                            strokeColor: dfltPolygon.lineColor,
                            strokeWidth: Utils.isDefined(dfltPolygon.lineWidth) ? dfltPolygon.lineWidth : 2
                        };
                        mapEntryInfo.polygon = this.map.addPolygon(id, entry.getName(), points, attrs, mapEntryInfo.marker);
                    }
                    let _this = this;
                    if (mapEntryInfo.marker) {
                        mapEntryInfo.marker.entry = entry;
                        mapEntryInfo.marker.ramaddaClickHandler = function(marker) {
                            _this.handleMapClick(marker);
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
        applyVectorMap: function(force, textGetter, args) {


	    

	    let debug = false;
            if (!force && this.vectorMapApplied) {
                return;
            }
	    let points = this.myPoints || this.myFeatures;
            if (!this.doDisplayMap() || !this.vectorLayer || !points) {
                return;
            }

	    if(!this.getApplyPointsToVectors()) {
		return;
	    }
    
	    if(!args) args = {};
	    if(debug) this.logMsg("applyVectorMap");
	    if(!textGetter) textGetter  = this.textGetter;

	    let linkField=this.getFieldById(null,this.getProperty("linkField"));
	    let linkFeature=this.getLinkFeature();
            let features = this.vectorLayer.features.slice();


            let allFeatures = features.slice();
	    this.recordToFeature = {};
	    if(debug) console.log("\t#features:" + features.length);
	    points.forEach(point=>{
		let record = point.record;
		if(!record) return;
		let feature = record.getDisplayProperty(this.getId(),"feature");
		if(feature)  this.recordToFeature[record.getId()] = feature;
	    });

	    if(linkFeature && linkField) {
		linkFeature = linkFeature.toLowerCase();
		let recordMap = {};

		points.forEach(p=>{
		    let record = p.record;
		    if(record) {
			let tuple = record.getData();
			let value = tuple[linkField.getIndex()];
			value  = value.toString().trim();
			record.linkValue = value;
			recordMap[value] = record;
		    }
		});
		if(debugFeatureLinking)
		    console.log("Map data/feature linking: data field:" + linkField.getId() +" looking for map feature:" + linkFeature);

		let errorCnt = 0;
		features.forEach((feature,idx)=>{
		    let attrs = feature.attributes;
		    let ok = false;
		    let debug = false;
		    for (let attr in attrs) {
			let _attr = String(attr).toLowerCase();
			if(linkFeature==_attr) {
			    if(debugFeatureLinking && idx<3)
				console.log("\tFound map attribute");
			    ok  = true;
			    let value = this.map.getAttrValue(attrs, attr);
			    if(value) {
				value = value.toString().trim();
				feature.linkValue = value;
				record = recordMap[value];
				if(record) {
				    if(debugFeatureLinking&& idx<10)
					console.log("\tfound record:" + value+": " + record.getId());
				    this.recordToFeature[record.getId()] = feature;
				} else {
				    if(debugFeatureLinking) {
					if(errorCnt++<20) {
					    console.log("\tCould not find record with map value:" + value.replace(/ /g,"X") +":");
					    console.dir(attrs);
					}
				    }
				}
			    } else {
				console.log("\tno map feature attribute value");
			    }
			    
			}
		    }
		    if(!ok) console.log("No map feature found:" + linkFeature);
		});
	    }


	    let j=0;
	    features.forEach((feature,idx)=>{
		feature.wasPruned = feature.pruned;
		feature.pruned = false;
		feature.newStyle=null;
		if(feature.style) {
		    feature.style.display ="inline-block";
		}
		feature.featureIndex = j++;
		feature.featureMatched = false;
		feature.pointCount = 0;
		feature.circles = [];
		if(feature.style)feature.style.balloonStyle = null;
	    });

            this.vectorMapApplied = true;

	    let maxExtent = null;
	    let doCount = this.getProperty("colorByCount",false);
	    let matchedFeatures = [];
	    let seen = {};
	    let maxCnt = -1;
	    let minCnt = -1;



	    points.forEach((point,idx)=>{
                if (point.style && point.style.display == "none") {
		    return;
		}
		let record = point.record;
                let center = point.center;
		let tmp = {index:-1,maxExtent: maxExtent};
		let matchedFeature = this.recordToFeature[record.getId()];
		if(matchedFeature) {
		    if (matchedFeature.geometry) {
			if (maxExtent === null) {
			    maxExtent = MapUtils.createBounds();
			}
			maxExtent.extend(matchedFeature.geometry.getBounds());
		    } else {
			//console.log("no geometry:" + matchedFeature.CLASS_NAME);
		    }
		}  else {
                    matchedFeature = this.findContainingFeature(features, center,tmp,false);
		}
		if(!matchedFeature) {
		    return;
		}
		matchedFeature.featureMatched = true;
		record.setDisplayProperty(this.getId(),"feature",matchedFeature);
		if(!point.colorByColor && point.hasColorByValue && isNaN(point.colorByValue)) {
		    return;
		}
		maxExtent = tmp.maxExtent;
		if(!seen[matchedFeature.featureIndex]) {
		    seen[matchedFeature.featureIndex] = true;
		    matchedFeatures.push(matchedFeature); 
		}
		matchedFeature.circles.push(point);
		matchedFeature.record = record;
		matchedFeature.textGetter=textGetter;
		if(doCount) {
		    matchedFeature.pointCount++;
		    maxCnt = maxCnt==-1?matchedFeature.pointCount:Math.max(maxCnt, matchedFeature.pointCount);
		    minCnt = minCnt==-1?matchedFeature.pointCount:Math.min(minCnt, matchedFeature.pointCount);
		} else {
		    if(tmp.index>=0)
			features.splice(tmp.index, 1);
		}
	    });


	    if(!doCount) {
		for(let i=0;i<matchedFeatures.length;i++) {
		    let matchedFeature = matchedFeatures[i];
		    style = matchedFeature.style;
		    if (!style) style = {
			"stylename": "from display",
		    };
		    delete style.display;
		    let newStyle = {};
		    $.extend(newStyle,style);
		    let circle = matchedFeature.circles[0];
		    $.extend(newStyle, circle.style);
		    matchedFeature.newStyle=newStyle;
		    matchedFeature.popupText = circle.text;
		    matchedFeature.dataIndex = i;
		}
	    }



	    let prune = this.getProperty("pruneFeatures", false);
	    if(doCount) {
		let colors = this.getColorTable(true);
		if (colors == null) {
		    colors = Utils.ColorTables.grayscale.colors;
		}
		let range = maxCnt-minCnt;
		let labelSuffix = this.getProperty("doCountLabel","points");
		for (let j = 0; j < features.length; j++) {
		    let feature = features[j];
		    let percent = range==0?0:(feature.pointCount - minCnt) /range;
                    let index = parseInt(percent * colors.length);
                    if (index >= colors.length) index = colors.length - 1;
                    else if (index < 0) index = 0;
		    let color= colors[index];
		    let style = feature.style;
		    if (!style) style = {
			"stylename": "from display",
		    };
		    let newStyle = {};
		    $.extend(newStyle,style);
		    $.extend(newStyle,{
			fillColor: color,
			"fillOpacity": 0.75,
			"strokeWidth": 1,
		    });

		    if(feature.pointCount==0) {
			//TODO: what to do with no count features?
			if(prune === true) {
			    newStyle.display = "none";
			}
		    }
		    feature.newStyle = newStyle;
		    feature.dataIndex = j;
		    feature.popupText = HU.div([],feature.pointCount +SPACE + labelSuffix);
		}
		this.displayColorTable(colors, ID_COLORTABLE, minCnt,maxCnt,{});
	    } else {
		if(prune) {
		    for (let i = 0; i < features.length; i++) {
			let feature = features[i];
			if(feature.featureMatched) {
			    continue;
			}
			if(feature.featureIndex==440)
			    this.logMsg("PRUNING:" + feature.featureIndex);
			let style = feature.style;
			if (!style) style = {
			    "stylename": "from display"
			};
			let newStyle = {};
			$.extend(newStyle,style);
			newStyle.display= "none";
			feature.pruned = true;
			feature.newStyle = newStyle;
		    }
		}
	    }



	    let redrawCnt = 0;
	    allFeatures.forEach((feature,idx)=>{
		if(!feature.newStyle) feature.newStyle={};
		if(feature.wasPruned && !feature.pruned ||
		   !feature.style ||
		   (feature.newStyle.display && 
		    feature.newStyle.display=="none" &&
		    feature.newStyle.display!=feature.style.display) ||
		   feature.style.fillColor!=feature.newStyle.fillColor) {
		    feature.style = feature.newStyle;
		    if(!feature.style.fillColor) {
			feature.style.fillColor = "rgba(230,230,230,0.5)";
			feature.style.strokeColor = "rgba(200,200,200,0.5)";
			feature.style.strokeWidth=1;
		    }
		    redrawCnt++;
		    this.vectorLayer.drawFeature(feature);
		}
		feature.newStyle = null;
	    });

	    return
	    


//	    console.log("redraw:" + redrawCnt);

            if (!args.dontSetBounds && maxExtent && !this.hadInitialPosition && this.getCenterOnFilterChange(true)) {
//		console.log("max:" + this.map.transformProjBounds(maxExtent));
		this.map.zoomToExtent(maxExtent, true);
	    }
	    if(!this.getProperty("fixedPosition",false))  {
		this.hadInitialPosition    = false;
	    }


        },
	findContainingFeature: function(features, center, info,debug) {
//	    debug=true;
	    let matchedFeature = null;
            for (let j = 0; j < features.length; j++) {
                let feature = features[j];
                let geometry = feature.geometry;
                if (!geometry) {
		    if(debug) console.log("\tno geometry")
                    continue;
                }
                bounds = geometry.getBounds();
                if (!bounds.contains(center.x, center.y)) {
		    //		    if(debug) console.log("\tnot in bounds:" + bounds)
                    continue;
                }
		if(debug) console.log("\tfindContainingFeature:" + center.x+" " + center.y);
                if (geometry.components) {
		    if(debug) console.log("\thas components:" +geometry.components.length);
                    geometry.components.every(comp=> {
                        bounds = comp.getBounds();
                        if (!bounds.contains(center.x, center.y)) {
			    if(debug) console.log("\t\tnot contain:" + bounds + " " + comp.CLASS_NAME);
			    return true;
                        }
			if(!comp.containsPoint) {
			    if(debug) console.log("\t\tunknown geometry:" + comp.CLASS_NAME);
			    return true;
			}
			if(debug) console.log("\t\tcontains:" + comp.containsPoint(center));
                        if (comp.containsPoint(center)) {
                            matchedFeature = feature;
			    if (feature.geometry) {
				if (info.maxExtent === null) {
				    info.maxExtent = MapUtils.createBounds();
				}
				info.maxExtent.extend(feature.geometry.getBounds());
			    }
                            info.index = j;
			    return false;
                        }
			return true;
                    });
		}
		if(matchedFeature) return matchedFeature;
                if (!geometry.containsPoint) {
                    if(debug && !geometry.components) 
			console.log("unknown geometry:" + geometry.CLASS_NAME);
                    continue;
                }
                if (geometry.containsPoint(center)) {
		    if (info.maxExtent === null) {
			info.maxExtent = MapUtils.createBounds();
		    }
		    info.maxExtent.extend(geometry.getBounds());
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
	    if(this.myFeatures) {
                for (let i = 0; i < this.myFeatures.length; i++) {
                    let point = this.myFeatures[i];
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
	    let animation = this.getAnimation();
            let windowStart = animation.begin.getTime();
            let windowEnd = animation.end.getTime();
            let atLoc = {};
	    if(this.myFeatures) {
		for (let i = 0; i < this.myFeatures.length; i++) {
                    let point = this.myFeatures[i];
                    if (point.date < windowStart || point.date > windowEnd) {
			point.style.display = 'none';
			continue;
                    }
                    if (atLoc[point.location]) {
			let other = atLoc[point.location];
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
	    }

	    this.applyToFeatureLayers(layer=>{layer.redraw();});
            this.applyVectorMap(true, this.textGetter);
	},
        showAllPoints: function() {
	    if(this.myFeatures) {
		for (let i = 0; i < this.myFeatures.length; i++) {
		    let line = this.myFeatures[i];
		    line.style.display = 'inline';
		}
		if (this.map.lines)
		    this.map.lines.redraw();
	    }
	    this.applyToFeatureLayers(layer=>{layer.redraw();});
            this.applyVectorMap(true);
        },

	colorByFieldChanged:function(field) {
	    this.haveCalledUpdateUI = false;
	    this.setProperty("colorBy", field);
	    this.vectorMapApplied  = false;
	    this.updateUI({fieldChanged:true});
	},
	sizeByFieldChanged:function(field) {
	    this.haveCalledUpdateUI = false;
	    this.setProperty("sizeBy", field);
	    this.vectorMapApplied  = false;
	    this.updateUI({fieldChanged:true});
	},
	dataFilterChanged: function(args) {
	    if(!args) args = {};
	    this.vectorMapApplied  = false;
	    this.updateUI({source:args.source, dataFilterChanged:true, dontSetBounds:true,  reload:true,callback: (records)=>{
		if(args.source=="animation") return;
		if(this.getCenterOnFilterChange(false)) {
		    if (this.vectorLayer && this.showVectorLayer) {
			if(this.getShowPoints()) {
			    if(records && records.length)
				this.map.centerOnMarkers(null, false, true);
			} else {
			    this.map.zoomToLayer(this.vectorLayer,1.2);
			}
		    } else if(this.lastImageLayer) {
			this.map.zoomToLayer(this.lastImageLayer);
		    } else {
			//true -> Just markers
			if(records && records.length)
			    this.map.centerOnMarkers(null, false, true);
		    }
		}
	    }});
	},
	requiresGeoLocation: function() {
	    if(this.shapesField && this.shapesTypeField) return false;
	    if(this.getLinkField() && this.getLinkFeature()) return false;
	    return true;
	},
	addFilters: function(filters) {
	    SUPER.addFilters.call(this, filters);
	    if(this.getProperty("showBoundsFilter")) {
		filters.push(new BoundsFilter(this));
	    }
	},
	getHeader2:function() {
	    let html = SUPER.getHeader2.call(this);
	    if(this.getProperty("showClipToBounds")) {
		this.clipToView=false;
		html =  HU.div([STYLE,HU.css("display","inline-block","cursor","pointer","padding","1px","border","1px solid rgba(0,0,0,0)"), TITLE,"Clip to view", ID,this.domId("clip")],HU.getIconImage("fa-map"))+SPACE2+ html;
	    }


	    if(this.getProperty("showMarkersToggle")) {
		let dflt = this.getProperty("markersVisibility", true);
		html += HU.checkbox(this.domId("showMarkersToggle"),[ID,this.domId("showMarkersToggle")],dflt,
				    this.getProperty("showMarkersToggleLabel","Show Markers")) +SPACE2;
	    }

	    if(this.getShowBaseLayersSelect()) {
		html+=this.getBaseLayersSelect();
	    }

	    if(this.getProperty("showVectorLayerToggle",false)) {
		html += HU.checkbox("",[ID,this.domId("showVectorLayerToggle")],!this.showVectorLayer) +" " +
		    this.getProperty("showVectorLayerToggleLabel","Show Points") +SPACE4;
	    }
	    html += HU.span([ID,this.domId("locations")]);

	    return html;
	},
	locationMenuCnt:0,
	addLocationMenu:function(url,data) {
	    let html = "";
	    let idx = this.locationMenuCnt++;
	    let label = data.label || data.name;
	    if(!label) {
		label = Utils.makeLabel(url.replace(/^.*[\\\/]/, '').replace(/\.[^\.]+$/,"").replace("_"," "));
	    }
	    html += HU.div([CLASS,"ramadda-menu-button ramadda-clickable ramadda-map-button bold",ID,this.domId("location_" + idx)],"View " + (label)) +SPACE;
	    this.map.appendToolbar(html);
//	    this.jq("locations").append(html);
	    let _this = this;
	    this.jq("location_" + idx).click(function() {
		let inner = "";
		let locations = [];
		if(data.features) {
		    data.features.forEach(feature=>{
			let name = feature.properties.NAME || feature.properties.name;
			locations.push({name:name,geometry:feature.geometry});
		    });
		} else {
		    locations = data.locations;
		}
		locations.sort((a,b)=>{
		    return a.name.localeCompare(b.name);
		});
		locations.forEach((loc,idx)=>{
		    if(Utils.isDefined(loc.latitude)) {
			inner+=HU.div([CLASS,"ramadda-clickable ramadda-hoverable", "latitude",loc.latitude,"longitude",loc.longitude,CLASS,"display-map-location"], loc.name);
		    } else if(Utils.isDefined(loc.north)) {
			inner+=HU.div([CLASS,"ramadda-clickable ramadda-hoverable", "north",loc.north,"west",loc.west,"south",loc.south,"east",loc.east, CLASS,"display-map-location"], loc.name);

		    } else if(Utils.isDefined(loc.geometry)) {
			inner+=HU.div([CLASS,"ramadda-clickable ramadda-hoverable", "index",idx, CLASS,"display-map-location"], loc.name);
		    }
		});
		inner = HU.div([ID,_this.domId("locationmenu"),STYLE,HU.css("max-height","200px","overflow-y","auto","padding","5px")],inner);
		let dialog = HU.makeDialog({content:inner,my:"left top",at:"left bottom",anchor:$(this),draggable:false,header:false});
		_this.jq("locationmenu").find(".ramadda-clickable").click(function() {
		    if(_this.locationFeatures) {
			_this.locationFeatures.forEach(feature=>{
			    _this.map.getHighlightLinesLayer().removeFeatures([feature]);
			});
		    }
		    _this.locationFeatures = [];
		    if($(this).attr("longitude")) {
			let point = MapUtils.createLonLat(+$(this).attr("longitude"),+$(this).attr("latitude"));
			_this.map.getMap().zoomTo(9);
			_this.map.setCenter(point);
		    } else if($(this).attr("north")) {
			_this.map.setViewToBounds(new RamaddaBounds(+$(this).attr("north"),+$(this).attr("west"),+$(this).attr("south"),+$(this).attr("east")));
		    } else {
			let geometry = locations[$(this).attr("index")].geometry;
			let type = geometry.type;
			let shapes = geometry.coordinates;
			let style = {
			    strokeColor: "blue",
			    fillColor:'rgba(0,0,255,0.10)',
			    strokeWidth: 2};
			if(type=="MultiPolygon") {
			    for(let i=0;i<shapes.length;i++) {
				let c2 = shapes[i];
				for(let j=0;j<c2.length;j++) {
				    _this.locationFeatures.push(_this.createFeature(c2[j],null,null,style));
				}
			    }
			} else if(type=="Polygon") {
			    for(let i=0;i<shapes.length;i++) {
				_this.locationFeatures.push(_this.createFeature(shapes[i],null,null,style));
			    }
			} else {
			    console.log("Unknown geometry:" + type);
			}
			_this.map.centerOnFeatures(_this.locationFeatures);
		    }
		    dialog.remove();
		});
	    });
	},
	initHeader2:function() {
	    let _this = this;
	    this.initBaseLayersSelect();


	    this.getProperty("locations","").split(",").forEach(url=>{
		url  =url.trim();
		if(url.length==0) return;
		if(!url.startsWith("/") && !url.startsWith("http")) {
		    url = ramaddaCdn + "/resources/" +url;			
		}
		let success = (data) =>{
		    data=JSON.parse(data);
		    this.addLocationMenu(url, data);
		};
		let fail = err=>{console.log("Error loading location json:" + url+"\n" + err);}
		Utils.doFetch(url, success,fail,null);	    
	    });


	    this.jq("showMarkersToggle").change(function() {
		let visible = $(this).is(':checked');
		_this.applyToFeatureLayers(layer=>{layer.setVisibility(visible);})
	    });
	    this.jq("showVectorLayerToggle").change(function() {
		_this.toggleVectorLayer();
	    });
	    
	    this.jq("clip").click(function(e){
		_this.clipToView = !_this.clipToView;
		if(!_this.clipToView) {
		    $(this).css("border","1px solid rgba(0,0,0,0)");
		} else {
		    $(this).css("border","1px solid #aaa");
		}
		_this.haveCalledUpdateUI = false;
		_this.updateUI();
	    });
	},

	handleNoData: function(pointData,reload) {
	    this.jq(ID_PAGE_COUNT).html("");
            this.addPoints([],[],[]);
	    this.setMessage(this.getNoDataMessage());

	},
	createFeature:function(polygon,record, textGetter, style){
	    if(!style) {
		if(this.baseStyle) {
		    this.baseStyle = $.extend({}, MapUtils.getVectorStyle('default'));
		    $.extend(this.baseStyle,{
			strokeColor: this.getProperty("vectorLayerStrokeColor","#000"),
			fillColor:this.getProperty("vectorLayerFillColor","#ccc"),
			fillOpacity:this.getProperty("vectorLayerFillOpacity",0.10),
			strokeWidth: this.getProperty("vectorLayerStrokeWidth",1),
			cursor:'pointer'
		    });
		}
		style = this.baseStyle;
	    }


	    let sitePoints = [];
	    polygon.forEach(pair=>{
		let point = MapUtils.createPoint(pair[0],pair[1]);
		let projPoint = this.map.transformLLPoint(point);
		sitePoints.push(projPoint);
	    });
	    let linearRing = MapUtils.createLinearRing(sitePoints);
	    let geometry = MapUtils.createPolygon([linearRing]);
	    let polygonFeature = MapUtils.createVector(geometry, null, style);
	    this.map.getHighlightLinesLayer().addFeatures([polygonFeature]);
	    polygonFeature.record = record;
	    polygonFeature.textGetter = textGetter;
	    return polygonFeature;
	},
	loadShapes: function(records) {
	    if(this.coordinateFeatures) {
		this.map.getHighlightLinesLayer().removeFeatures(this.coordinateFeatures);
	    }
	    let textGetter = (f)=>{
		if(f.record) {
                    return  this.getRecordHtml(f.record, null, this.getProperty("tooltip"));
		}
		return "NONE";
	    };
	    this.coordinateFeatures = [];
	    records.forEach((r,idx)=>{
		let type = r.getValue(this.shapesTypeField.getIndex());		
		let shapesString= r.getValue(this.shapesField.getIndex());
		let shapes = JSON.parse(shapesString);
		if(type=="MultiPolygon") {
		    for(let i=0;i<shapes.length;i++) {
			let c2 = shapes[i];
			for(let j=0;j<c2.length;j++) {
			    this.coordinateFeatures.push(this.createFeature(c2[j],r,textGetter));
			}
		    }
		} else if(type=="Polygon") {
		    for(let i=0;i<shapes.length;i++) {
			this.coordinateFeatures.push(this.createFeature(shapes[i],r,textGetter));
		    }
		} else {
		    console.log("Unknown geometry:" + type);
		}
	    });

	},	    
	toggleTrack:function(record,item) {
	    let marker = this.markers?this.markers[record.getId()]:null;
	    if(this.tracks[record.getId()]) {
		if(item)item.removeClass("display-map-toc-item-on");
		this.map.removePolygon(this.tracks[record.getId()]);
		this.tracks[record.getId()] = null;
		if(item) {
		    item.attr(TITLE,"Click to view; Double-click to view track");
		}
	    } else {
		if(item) {
		    item.addClass("display-map-toc-item-on");
		    item.attr(TITLE,"Click to view; Double-click to remove track");
		}
		let url = record.getValue(this.trackUrlField.getIndex());
		if(url!="")
		    $.getJSON(url, data=>{this.loadTrack(record, data)}).fail(err=>{console.log("url failed:" + url +"\n" + err)});
	    }
	    this.map.setCenter(MapUtils.createLonLat(record.getLongitude(),record.getLatitude()));
	},
	makeToc:function(records) {
	    let labelField = this.getFieldById(null,this.getProperty("labelField","name"));
	    if(!labelField) labelField = this.getFieldByType(null,"string");
	    if(labelField) {
		let html = "";
		let iconField = this.getFieldById(null, this.getProperty("iconField"));
		records.forEach((record,idx)=>{
		    let title = "View record";
		    if(this.trackUrlField) title = "Click to view; Double-click to view track";
		    let clazz = "ramadda-clickable  display-map-toc-item ramadda-noselect";
		    let value = labelField.getValue(record);
		    if(!iconField) {
			clazz+=" ramadda-nav-list-link ";
		    } else {
			value = HU.getIconImage(iconField.getValue(record,icon_blank16),["width",16]) + SPACE + value;
		    }
		    html+= HU.span([TITLE, title, CLASS,clazz,RECORD_ID,record.getId(),RECORD_INDEX,idx], value);
		});

		let height = this.getProperty("height", this.getProperty("mapHeight", 300));

		html = HU.div([CLASS, "display-map-toc",STYLE,HU.css("max-height","calc(" +HU.getDimension(height)+" - 1em)"),ID, this.domId("toc")],html);
		let title = this.getProperty("tableOfContentsTitle","");
		if(title) html = HU.center(HU.b(title)) + html;
		this.jq(ID_LEFT).html(html);
		let _this = this;
		let items = this.jq(ID_LEFT).find(".display-map-toc-item");
		this.makeTooltips(items,records);
		items.click(function() {
		    let idx = $(this).attr(RECORD_INDEX);
		    let record = records[idx];
		    if(!record) return;
		    _this.highlightPoint(record.getLatitude(), record.getLongitude(),true, false);
		    _this.map.setCenter(MapUtils.createLonLat(record.getLongitude(),record.getLatitude()));
		    _this.map.setZoom(10);
		    if(record.trackData) {
			setTimeout(()=>{
			    _this.getDisplayManager().notifyEvent("dataSelection", _this, {data:record.trackData});
			},100);
		    }
		});

		items.dblclick(function() {
		    _this.removeHighlight();
		    let idx = $(this).attr(RECORD_INDEX);
		    let record = records[idx];
		    if(!record) return;
		    if(_this.trackUrlField) {
			let url = record.getValue(_this.trackUrlField.getIndex());
			if(url && url.length>0) {
			    _this.toggleTrack(record,$(this));
			    return;
			}
		    } 
		    _this.map.setCenter(MapUtils.createLonLat(record.getLongitude(),record.getLatitude()));
		});
	    }
	},	    

        updateUI: function(args) {
	    if(!args) args={};
	    let debug = false;
	    this.lastUpdateTime = null;
            SUPER.updateUI.call(this,args);
//	    console.log("map.updateUI: " + !this.getDisplayReady() +" " + !this.hasData() +" " +!this.getProperty("showData", true));
            if (this.haveCalledUpdateUI || !this.getDisplayReady() ||!this.hasData() || !this.getProperty("showData", true)) {
		if(debug) console.log("map.updateUI have called:" + this.haveCalledUI +" ready:" + this.getDisplayReady() +" has data:" + this.hasData() +" showData:" +this.getProperty("showData", true));
                return;
            }




	    if(this.updateUICallback) {
		clearTimeout(this.updateUICallback);
		this.updateUICallback = null;
	    }
            let pointData = this.getPointData();
	    this.lastZoom = this.map?this.map.getZoom():null;

	    //Set the shapes Fields here before filter data so we can accept non georeferenced data
	    this.shapesField = this.getFieldById(null,this.getProperty("shapesField"));
	    this.shapesTypeField = this.getFieldById(null,this.getProperty("shapesTypeField"));
	    this.trackUrlField  =  this.getFieldById(null,this.getProperty("trackUrlField"));
            let records = this.records =  this.filterData();

	    if(this.shapesTypeField && this.shapesField) {
		this.setProperty("tooltipNotFields",this.shapesTypeField.getId()+"," + this.shapesField);
		this.loadShapes(records);
	    }

	    if(debug) console.log("displaymap.updateUI reload=" +args.reload);
            if (records == null) {
		if(debug) console.log("\tno data");
                return;
            }



	    if(this.getShowTableOfContents(false)) {
		this.makeToc(records);
	    }
	    if(!this.updatingFromClip) {
		//stop the flash
		if(args.source!="animation") {
		    this.setMessage(args.dataFilterChanged|| args.fieldChanged|| args.reload?"Reloading map...":"Creating map...");
		}
	    }
	    this.updatingFromClip = false;

//	    setTimeout(()=>{
		try {
		    if(displayDebug.initMap) this.logMsg("calling updateUIInner",true);
		    this.callingUpdateUI = true;
		    this.updateUIInner(args, pointData, records,debug);
		    this.callingUpdateUI = false;
		    if(this.getCenterOnMarkersAfterUpdate()) {
			this.map.centerOnMarkers();
		    }
		    if(displayDebug.initMap) this.logMsg("done calling updateUIInner",true);
		    if(args.callback)args.callback(records);
		    this.clearProgress();
		    if(!this.layerVisible) {
			setTimeout(()=>{this.setVisible(false);},50);
		    }
		} catch(exc) {
		    this.callingUpdateUI = false;
		    console.log(exc)
		    console.log(exc.stack);
		    this.setMessage("Error:" + exc);
		}
		this.setIsFinished();
//	    });
	},
	filterDataPhase2:function(records) {
	    records = SUPER.filterDataPhase2.call(this,records);
	    if(this.clipBounds || this.clipToView) {
		let bounds = RecordUtil.getBounds(records);
		this.clipBounds = false;
		let clipRecords = false;
		if(!this.lastPointBounds || (this.lastPointBounds && this.lastPointBounds!=bounds)) {
		    clipRecords = true;
		}
		this.lastPointBounds = bounds;
		if(this.clipToView || clipRecords) {
		    let viewbounds = this.map.getMap().calculateBounds().transform(this.map.sourceProjection, this.map.displayProjection);
		    let tmpRecords =records.filter(r=>{
			return viewbounds.containsLonLat(MapUtils.createLonLat(r.getLongitude(),r.getLatitude()));
		    });
//		    console.log("clipped records:" + tmpRecords.length);
		    records = tmpRecords;
		}
	    }
	    return records;
	},


	updateUIInner: function(args, pointData, records, debug) {
	    let _this = this;
	    let t1= new Date();
	    debug = debug || displayDebug.displayMapUpdateUI;
	    if(debug) console.log("displaymap.updateUIInner:" + records.length);
	    this.haveCalledUpdateUI = true;
	    if(this.getProperty("showRegionSelector")) {
		//Fetch the regions
		if(!ramaddaMapRegions) {
		    let jqxhr = $.getJSON(ramaddaBaseUrl +"/regions.json", data=> {
			if (GuiUtils.isJsonError(data)) {
			    console.log("Error fetching regions");
			    ramaddaMapRegions=[];
			    return;
			}
			ramaddaMapRegions=data;
		    });
		}		    
		let label = this.getProperty("regionSelectorLabel") || HU.getIconImage("fa-globe-americas");
		let button = HU.div([CLASS,"ramadda-menu-button ramadda-clickable",  TITLE,"Select region", ID,this.domId("selectregion")],label)+SPACE2;
		this.writeHeader(ID_HEADER2_PREPREFIX, button);
		this.jq("selectregion").click(function() {
		    let id = _this.domId(ID_REGION_SELECTOR);
		    let groups = {};
		    ramaddaMapRegions.forEach((r,idx)=>{
			//skip world as its a dup
			if(r.name == "World") return
			let group = r.group;
			if(group.toLowerCase()=="model regions") group="Regions";
			let name = r.name.replace(/ /g,"&nbsp;");
			let item = HU.div([CLASS,"ramadda-menu-item", "idx",idx],name);
			if(!groups[group]) groups[group] = "";
			groups[group] +=item;});
		    let html = "<table width=100%><tr valign=top>";
		    Object.keys(groups).forEach(group=>{
			html+= HU.td([STYLE,HU.css()], HU.div([STYLE,HU.css("font-weight","bold","border-bottom","1px solid #ccc","margin-right","5px")], Utils.camelCase(group))+HU.div([STYLE,HU.css("max-height","200px","overflow-y","auto", "margin-right","10px")], groups[group]));
		    });
		    html+="</tr></table>"
		    //set the global 
		    let popup = HtmlUtils.setPopupObject(HtmlUtils.getTooltip());
		    html = HU.div([ID,id],html);
		    popup.html(HU.div([CLASS, "ramadda-popup-inner"], html));
		    popup.show();
		    popup.position({
			of: $(this),
			my: "left top",
			at: "left bottom",
		    });
		    _this.jq(ID_REGION_SELECTOR).find(".ramadda-menu-item").click(function() {
			let region = ramaddaMapRegions[+$(this).attr("idx")];
			HtmlUtils.hidePopupObject();
			_this.map.setViewToBounds(new RamaddaBounds(region.north, region.west, region.south, region.east));
		    });
		});
	    }

	    if(!this.getProperty("makeDisplay",true)) {
		return;
	    }
	    if(!this.fullBounds) {
		this.fullBounds = {};
		RecordUtil.getPoints(this.getRecords(), this.fullBounds);
	    }
            let pointBounds = {};
            let points = RecordUtil.getPoints(records, pointBounds);
            let fields = pointData.getRecordFields();
            let showSegments = this.getShowSegments(false);
	    if(records.length!=0) {
		if (!isNaN(pointBounds.north)) {
		    this.pointBounds = pointBounds;
		    this.initBounds = pointBounds;
		    if(!showSegments && !this.hadInitialPosition && !args.dontSetBounds) {
			if(!args.dataFilterChanged || this.getCenterOnFilterChange(true)) {
			    this.setInitMapBounds(pointBounds.north, pointBounds.west, pointBounds.south, pointBounds.east);
			}
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
	    let t2= new Date();
//	    debug = true;
	    if(debug) console.log("displaymap calling addPoints");


            this.addPoints(records,fields,points,pointBounds,debug);
	    let t3= new Date();
            this.addLabels(records,fields);
            this.applyVectorMap(true, this.textGetter,args);
	    let t4= new Date();
	    if(debug) Utils.displayTimes("time pts=" + points.length,[t1,t2,t3,t4], true);
	    this.lastUpdateTime = new Date();
	},
	xcnt:0,
	heatmapCnt:0,
	animationApply: function(animation, skipUpdateUI) {
//	    console.log("map.applyAnimation:" +this.heatmapVisible);
 	    if(!this.heatmapLayers || !this.heatmapVisible) {
//		console.log("map.applyAnimation-1");
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
	    offLayers.forEach(layer=>{
		layer.setVisibility(false);
	    });
	    console.log("map.applyAnimation-2:" + onDate);
 	    if(!onDate) {
		SUPER.animationApply.call(this, animation, skipUpdateUI);
	    }
	    if(onLayer!=null)
		this.setMapLabel(onLayer.heatmapLabel);
	},
        setDateRange: function(min, max) {
	    //Not sure why we do this
	    if(this.getDoGridPoints(false)|| this.getDoHeatmap(false)) {
		SUPER.setDateRange.call(this, min,max);
	    } else {
		SUPER.setDateRange.call(this, min,max);
	    }
	},
	showColorTable: function(colorBy) {
	    colorBy.displayColorTable(null,true);
	    this.callingUpdateSize = true;
	    //for now don't do this as it takes a long time
	    //	    this.map.getMap().updateSize();
	    this.callingUpdateSize = false;
	},

	applyHeatmapAnimation: function(index) {
	    this.jq(ID_HEATMAP_ANIM_LIST)[0].selectedIndex = index;
	    let offLayers = [];
	    this.heatmapLayers.forEach((layer,idx)=>{
		if(index==idx)
		    layer.setVisibility(true);
		else
		    offLayers.push(layer);
	    });
	    offLayers.forEach(layer=>{
		layer.setVisibility(false);
	    });
	    this.setMapLabel(this.heatmapLayers[index].heatmapLabel);

	},
	stepHeatmapAnimation: function(delta){
	    let index = this.jq(ID_HEATMAP_ANIM_LIST)[0].selectedIndex;
	    index+=delta;
	    if(index<0) {
		index = 0;
	    }
	    if(index>=this.heatmapLayers.length) {
		index =0;
	    }
	    this.applyHeatmapAnimation(index);
	    if(this.heatmapPlayingAnimation) {
		setTimeout(()=>{
		    this.stepHeatmapAnimation();
		},this.getHmAnimationSleep(1000));
	    }
	},
	checkHeatmapReload:function() {
//	    return
	    if(!this.getHmReloadOnZoom(this.getReloadOnZoom(false))) return;
	    let now = new Date ();
	    //Don't do this the first couple of seconds after we've been created
	    if(now.getTime()-this.createTime.getTime()<3000) return;
	    let diff = 0;
	    if(this.checkHeatmapReloadTime) {
		diff = now.getTime()-this.checkHeatmapReloadTime.getTime();
	    }
	    this.checkHeatmapReloadTime = now;
	    if(diff<1000) {
		if(!this.checkHeatmapReloadPending) {
		    this.checkHeatmapReloadPending = true;
		    setTimeout(()=>{
			this.checkHeatmapReloadPending = false;
			this.checkHeatmapReload();
		    },1100)
		}
		return;
	    }
	    this.checkHeatmapReloadTime = null;
	    this.reloadHeatmap = true;
	    this.haveCalledUpdateUI = false;
	    this.updateUI();
	},
	createHeatmap(records, fields, bounds) {
	    let debug = displayDebug.displayMapCreateMap;
//	    debug = true;
	    if(debug) console.log("createHeatmap");
	    let colorBy = this.getColorByInfo(records, null,null,null,["hmColorBy","colorBy",""]);
	    let angleBy = this.getColorByInfo(records, "angleBy",null,null,["hmAngleBy","angleBy",""]);
	    let lengthBy = this.getColorByInfo(records, "lengthBy",null,null,["hmLengthBy","lengthBy",""]);
	    if(!angleBy.isEnabled()) angleBy = colorBy;
	    if(!lengthBy.isEnabled()) lengthBy=null;
	    records = records || this.filterData();
	    if(this.getHmBounds()) {
		let toks = this.getHmBounds().split(",");
		bounds = new RamaddaBounds(+toks[0],+toks[1], +toks[2],+toks[3]);
	    }
	    let mapBounds = this.map.getBounds();
	    bounds = bounds ||  RecordUtil.getBounds(records);
	    bounds = RecordUtil.convertBounds(mapBounds);
	    if(this.pointBounds) bounds = this.pointBounds;
	    if(debug) {
		console.dir(bounds.north,bounds.west,bounds.south,bounds.east);
	    }


	    this.removeExtraLayers();
	    this.heatmapLayers = [];
	    this.extraLayers = [];	    
	    if(records.length==0) {
		this.errorMessage = this.getNoDataMessage();
		this.setMessage(this.errorMessage);
		return
	    }
	    if(this.reloadHeatmap) {
		this.reloadHeatmap = false;
		bounds = new RamaddaBounds(this.map.getBounds());
		records = RecordUtil.subset(records, bounds);
//		bounds =  RecordUtil.getBounds(records);
	    }
	    bounds = RecordUtil.expandBounds(bounds,this.getProperty("boundsScale",0.05));

	    let dfltArgs = this.getDefaultGridByArgs();
	    let ratio = (bounds.east-bounds.west)/(bounds.north-bounds.south);
	    let w = Math.round(this.getProperty("gridWidth",800));
	    let h = Math.round(w/ratio);
	    let groupByField = this.getFieldById(null,this.getHmGroupBy());
	    let groupByDate = this.getHmGroupByDate();
	    if(debug) console.log("\tcalling groupBy");
	    let t1 = new Date();
	    let groups = (groupByField || groupByDate)?RecordUtil.groupBy(records, this, groupByDate, groupByField):null;
	    let t2 = new Date();
	    //	    Utils.displayTimes("make groups",[t1,t2],true);
	    if(debug) console.log("\tdone calling groupBy");
	    if(groups == null || groups.max == 0) {
		doTimes = false;
		groups= {
		    max:records.length,
		    values:["none"],
		    map:{none:records}
		}
	    }
	    
	    //	    if(debug) console.log("\tdone calling groupBy count="+ groups.values.length);
	    let recordCnt = groups.max;
 	    if(dfltArgs.cellSize==0) {
		let sqrt = Math.sqrt(recordCnt);
		let size = Math.round(w/sqrt);
		dfltArgs.cellSizeX = dfltArgs.cellSizeY = dfltArgs.cellSize = size;
	    } else if(String(dfltArgs.cellSize).endsWith("%")) {
		dfltArgs.cellSize =dfltArgs.cellSizeX =  dfltArgs.cellSizeY = Math.floor(parseFloat(dfltArgs.cellSize.substring(0,dfltArgs.cellSize.length-1))/100*w);
	    }
	    let args =$.extend({colorBy:colorBy,angleBy:angleBy,lengthBy:lengthBy,w:w,h:h,bounds:bounds,forMercator:true},
			       dfltArgs);
	    if(debug) {
		console.log("#records:" + records.length+" dim:" + w +" " +h + " #records:" + records.length +" cell:" + dfltArgs.cellSizeX + " #records:" + records.length +" bounds:" + bounds);
	    }
	    let labels = [];
	    let labelPrefix = this.getHmLabelPrefix("${field}-");
	    groups.values.forEach((value,idx)=>{
		let recordsAtTime = groups.map[value];
		if(debug)
		    console.log("group:" + value +" #:" + groups.map[value].length);

		let img = Gfx.gridData(this.getId(),fields, recordsAtTime,args);
//		$("#testimg").html(HU.image(img,[WIDTH,"100%", STYLE,"border:1px solid blue;"]));
		let label = value=="none"?"Heatmap": labelPrefix +" " +groups.labels[idx];
		label = label.replace("${field}",colorBy.field?colorBy.field.getLabel():"");
		labels.push(label);
//		console.log("B:" + bounds);
		let layer = this.map.addImageLayer("heatmap"+(this.heatmapCnt++), label, "", img, idx==0, bounds.north, bounds.west, bounds.south, bounds.east,w,h, { 
		    isBaseLayer: false,
		});
		//For now don't set the layer index since it places this layer too high
		//		this.map.getMap().setLayerIndex(layer, 1000);
		layer.heatmapLabel = label;
		if(groupByDate) {
		    if(value.getTime)
			layer.date = value;
		}
		this.extraLayers.push(layer);
		this.heatmapLayers.push(layer);
	    });
	    if(this.getHmShowGroups(true) && this.heatmapLayers.length>1 && !this.getAnimationEnabled()) {
		this.heatmapPlayingAnimation = false;
		let controls =  [];
		controls.push(HU.div([ID,this.domId(ID_HEATMAP_ANIM_STEP_BACK),STYLE,HU.css("display","inline-block"),TITLE,"Step back"],
 				     HU.getIconImage("fa-step-backward",[CLASS,"display-anim-button"])));

		if(!groupByField) 
		    controls.push(HU.div([ID,this.domId(ID_HEATMAP_ANIM_PLAY),STYLE,HU.css("display","inline-block"),TITLE,"Play/Stop Animation"],
					 HU.getIconImage("fa-play",[CLASS,"display-anim-button"])));
		controls.push(HU.div([ID,this.domId(ID_HEATMAP_ANIM_STEP_FORWARD),STYLE,HU.css("display","inline-block"),TITLE,"Step forward"],
 				     HU.getIconImage("fa-step-forward",[CLASS,"display-anim-button"])));
		

		controls.push(HU.div([STYLE,HU.css("display","inline-block","margin-left","5px","margin-right","5px")], HU.select("",[ID,this.domId(ID_HEATMAP_ANIM_LIST)],labels)));
		this.writeHeader(ID_HEADER2_PREPREFIX, Utils.join(controls,"&nbsp;&nbsp;"));
		let _this = this;
		this.jq(ID_HEATMAP_ANIM_LIST).change(function() {
		    let index = $(this)[0].selectedIndex;
		    _this.applyHeatmapAnimation(index);
		});
		this.jq(ID_HEATMAP_ANIM_PLAY).click(function() {
		    _this.heatmapPlayingAnimation = !_this.heatmapPlayingAnimation;
		    let icon = _this.heatmapPlayingAnimation?"fa-stop":"fa-play";
		    $(this).html(HU.getIconImage(icon,[CLASS, "display-anim-button"]));
		    if(_this.heatmapPlayingAnimation) {
			_this.stepHeatmapAnimation(1);
		    }
		});
		this.jq(ID_HEATMAP_ANIM_STEP_FORWARD).click(function() {
		    _this.heatmapPlayingAnimation = false;
		    let icon = _this.heatmapPlayingAnimation?"fa-stop":"fa-play";
		    _this.jq(ID_HEATMAP_ANIM_PLAY).html(HU.getIconImage(icon,[CLASS,"display-anim-button"]));
		    _this.stepHeatmapAnimation(1);
		});
		this.jq(ID_HEATMAP_ANIM_STEP_BACK).click(function() {
		    _this.heatmapPlayingAnimation = false;
		    let icon = _this.heatmapPlayingAnimation?"fa-stop":"fa-play";
		    _this.jq(ID_HEATMAP_ANIM_PLAY).html(HU.getIconImage(icon,[CLASS,"display-anim-button"]));
		    _this.stepHeatmapAnimation(-1);
		});		

	    }
	    if(groups.values[0]!="none") {
		this.setMapLabel(labels[0]);
	    }
	    this.showColorTable(colorBy);
	    if(this.getHmShowToggle(this.getProperty("hm.showToggle")) || this.getHmShowReload()) {
		let cbx = this.jq(ID_HEATMAP_TOGGLE);
		let reload =  HU.getIconImage("fa-sync",[CLASS,"display-anim-button",TITLE,"Reload heatmap", ID,this.domId("heatmapreload")])+SPACE2;
		this.heatmapVisible= cbx.length==0 ||cbx.is(':checked');

		this.writeHeader(ID_HEADER2_PREFIX,
				 reload + HU.checkbox("",[ID,this.domId(ID_HEATMAP_TOGGLE)],this.heatmapVisible) +SPACE +
				 this.getHmToggleLabel(this.getProperty("hm.toggleLabel","Toggle Heatmap")) +SPACE2);
		let _this = this;
		this.jq("heatmapreload").click(()=> {
		    this.reloadHeatmap = true;
		    this.removeExtraLayers();
		    this.haveCalledUpdateUI = false;
		    this.updateUI();
		});
		this.jq(ID_HEATMAP_TOGGLE).change(function() {
		    if(_this.heatmapLayers)  {
			let visible = $(this).is(':checked');
			_this.heatmapVisible  = visible;
			_this.heatmapLayers.forEach(layer=>layer.setVisibility(visible));
			_this.map.setPointsVisibility(!visible);
		    }
		});
	    }
	},

	updateHtmlLayers: function() {
	    if(this.htmlLayerInfo) {
		this.createHtmlLayer(this.htmlLayerInfo.records, this.htmlLayerInfo.fields);
	    }
	},
	updateHtmlLayer:function() {
	    if(!this.htmlLayer) return;
	    if(!this.htmlLayerId) {
		this.htmlLayerId =this.getUniqueId(ID_HTMLLAYER);
		let vp  = this.map.getMap().getViewport();
		vp = $(vp).children()[0];
		$(vp).css("display","relative");
		$(vp).append(HU.div([CLASS,"display-map-htmllayer", ID,this.htmlLayerId]));
	    }
	    $("#"+ this.htmlLayerId).html(this.htmlLayer);
	},
        createHtmlLayer: function(records, fields) {
	    let htmlLayerField = this.getFieldById(fields,this.getHtmlLayerField());
	    this.htmlLayerInfo = {
		records:records,
		fields:fields,
	    };
	    this.htmlLayer = "";
	    let fillColor = this.getFillColor("#619FCA");
	    let strokeColor = this.getStrokeColor("#888");
	    let w = this.getHtmlLayerWidth(30);
	    let h = this.getHtmlLayerHeight(15);
	    let shape = this.getHtmlLayerShape("barchart");
	    if(shape=="barchart")
		this.setProperty("colorBy",htmlLayerField.getId());
	    if(this.getHtmlLayerScale()) {
		let zooms = [];		
		this.getHtmlLayerScale().split(",").forEach(t=>{
		    zooms.push(t.split(":"));
		});
		//3:0.5,4:1,5:2
		let zoom = this.map.map.getZoom();
		let scale = 1.0;
		if(zooms.length==1 && zooms[0].length==1) {
		    scale=zooms[0][0];
		} else {
		    zooms.every(t=>{
			scale=t[1];
			if(t[0] >= zoom) {
			    return false;
			}
			return true;
		    });
		}
		w*=scale;h*=scale;
	    }
	    let style = this.getHtmlLayerStyle("");
	    let infos = [];
	    let allData = this.getColumnValues(records, htmlLayerField);
	    let groups = RecordUtil.groupBy(records, this, false,"latlon");
	    let container = $($(this.map.getMap().getViewport()).children()[0]);
	    let cleft = +container.css("left").replace("px","");
	    let ctop = +container.css("top").replace("px","");
	    let hoverW = w*3;
	    let hoverH = h*3;
	    let layerRecords = [];

	    groups.values.forEach((value,idx)=>{
		let recordsAtTime = groups.map[value];
		let data = [];
		layerRecords.push(recordsAtTime[0]);
		recordsAtTime.forEach((r,idx)=>{
		    data.push(r.getValue(htmlLayerField.getIndex()));
		});
		let record = recordsAtTime[0];
		let px = this.map.getMap().getPixelFromLonLat(this.map.transformLLPoint(MapUtils.createLonLat(record.getLongitude(),record.getLatitude())));
		let id = this.getId() +"_sl"+ idx;
		let hid = id +"_hover";
		let html = 
		    HU.div([ID,id,  CLASS,'display-map-html-item',STYLE,style +HU.css('line-height','0px','z-index','1000','position','absolute','left', (px.x-w/2-cleft) +'px','top', (px.y-h/2-ctop)+'px')]) +
		    HU.div([ID,hid, RECORD_INDEX, idx,TITLE,"", CLASS,'display-map-html-hitem', STYLE,style +HU.css('display','none','line-heigh','0px','z-index','1001','position','absolute','left', (px.x-hoverW/2-cleft) +'px','top', (px.y-hoverH/2-ctop)+'px')]);
		this.htmlLayer += html;
		infos.push({
		    id:id,
		    hoverId: hid,
		    data:data,
		    records: recordsAtTime
		});
	    });
	    this.updateHtmlLayer();
            let colorBy = this.getColorByInfo(records);
	    infos.forEach((info,idx)=>{
		if(shape == "pie" || shape == "piechart") {
		    [0,1].forEach((cid,idx)=>{
			let id = HU.getUniqueId("pie");
			let cw = idx==0?w:hoverW;
			let ch = idx==0?h:hoverH;
			let pie = HU.tag('canvas',[STYLE,HU.css('cursor','pointer'),ID,id ,WIDTH,cw,HEIGHT, ch]);
			if(idx==0)
			    $("#" + info.id).html(pie);
			else
			    $("#" + info.hoverId).html(pie);
			let canvas = document.getElementById(id);
			let color = colorBy&& colorBy.isEnabled()?colorBy.getColor(info.data[0]):fillColor;
			let ctx = canvas.getContext("2d");
			if(idx==1) {
			    ctx.fillStyle= '#fff';
			    ctx.beginPath();
			    ctx.moveTo(cw/2,ch/2);
			    ctx.arc(cw/2,ch/2, cw/2-2, 0-Math.PI/2, 2*Math.PI);
			    ctx.closePath();
			    ctx.fill();
			}
			ctx.beginPath();
			ctx.moveTo(cw/2,ch/2);
			ctx.arc(cw/2,ch/2, cw/2-2, 0-Math.PI/2, info.data[0]*2 * Math.PI-Math.PI/2);
			ctx.lineTo(cw/2,ch/2);
			ctx.closePath();
			ctx.strokeStyle= strokeColor;
			ctx.fillStyle= color;
			ctx.fill();
			ctx.stroke();
			ctx.beginPath();
			ctx.arc(cw/2,ch/2, cw/2-2, 0, 2 * Math.PI);
			ctx.closePath();
			ctx.stroke();
		    });
		} else {
		    drawSparkLine(this,"#"+ info.id,w,h,info.data,info.records,allData.min,allData.max,colorBy);
		    $('#' + info.hoverId).css('background','#fff').css('border','1px solid #ccc');
		    drawSparkLine(this,"#"+ info.hoverId,hoverW,hoverH,info.data,info.records,allData.min,allData.max,colorBy);		    
		}
	    });
	    let items = this.find(".display-map-html-item");
	    let hitems = this.find(".display-map-html-hitem");
	    this.makeTooltips(hitems, layerRecords);

	    items.mouseenter(function() {
		$(this).css('display','none');
		$("#"+$(this).attr(ID)+"_hover").fadeIn(1000);
		
	    });
	    hitems.mouseleave(function() {
		$("#"+ $(this).attr(ID).replace('_hover','')).css('display','block');
		$(this).css('display','none');
	    });
	    if(colorBy.hasField()) {
		this.showColorTable(colorBy);
	    }
	},


        makeVoronoi: function(records, fields, points,bounds) {
	    if(!MapUtils.loadTurf(()=>{this.makeVoronoi(records, fields, points,bounds);})) {
		return;
	    }

	    let options = {
		bbox: this.makeTurfBounds(bounds)
	    };
	    let geojsonPoints = [];
	    let dups = {};
	    let vpoints = [];
	    points.forEach((p,idx)=>{
		let record = records[idx];
		let key =p.x+'_'+p.y;
		if(dups[key]) return;
		dups[key] = true;
		geojsonPoints.push(turf.point([p.x,p.y]));
		vpoints.push({point:p,record:record});
	    });
	    let collection = turf.featureCollection(geojsonPoints);
//	    let points = turf.randomPoint(1000, options);
	    let  voronoiPolygons= turf.voronoi(collection, options);
	    this.voronoiLayer =this.createGeoJsonLayer('Voronoi',voronoiPolygons,this.voronoiLayer);
            let colorBy = this.getColorByInfo(records,'voronoiColorBy',null,null,null,this.lastColorBy);
	    this.lastColorBy = colorBy;
	    let textGetter = this.getTextGetter(fields);
	    let style = {
		strokeColor:this.getVoronoiStrokeColor(),
		strokeWidth:this.getVoronoiStrokeWidth(),
		strokeDashstyle:this.getVoronoiStrokeDashstyle(),
		fillColor:this.getVoronoiFillColor(),
		fillOpacity:this.getVoronoiFillOpacity()		
	    }
	    this.voronoiLayer.features.forEach((f,idx)=>{
		let record = vpoints[idx].record;
		let s =Utils.clone({},style);
		if(colorBy.isEnabled()) {
		    s.fillColor= colorBy.getColorFromRecord(record);
		}
		f.style=s;
		f.record  =record;
		f.textGetter  = textGetter;
	    });
	    this.voronoiLayer.redraw();
            if (colorBy.isEnabled()) {
		colorBy.displayColorTable();
	    }
	},

        makeHexmap: function(records, fields, points,bounds) {
	    if(!MapUtils.loadTurf(()=>{this.makeHexmap(records, fields, points,bounds);})) {
		return;
	    }

	    let bbox =  this.makeTurfBounds(this.getHexmapUseFullBounds()?this.fullBounds:bounds,this.getHexmapPadding());
	    let cellSide = this.getHexmapCellSide();
	    let options = {units: this.getHexmapUnits()};
	    let hexgrid;
	    let mapType = this.getProperty('mapType');
	    if(this.getDoTrianglemap() || mapType=='triangle') {
		hexgrid = turf.triangleGrid(bbox, cellSide, options);
	    } else if(this.getDoSquaremap() || mapType=='square') {
		hexgrid = turf.squareGrid(bbox, cellSide, options);
	    } else {
		hexgrid = turf.hexGrid(bbox, cellSide, options);
	    }

	    let vpoints = [];
//	    console.time('x');
	    let yes= 0, no=0;

	    points.forEach((p,idx)=>{
		let record = records[idx];
		let pts = turf.points([[p.x,p.y]]);
		let isIn = false;
		hexgrid.features.every((feature,fidx)=>{
		    if(!feature.searchWithin) {
			let coords = feature.geometry.coordinates[0];
			feature.searchWithin = turf.polygon([coords]);
			feature.bbox = turf.bbox(feature.searchWithin);
			feature.records=[];
		    }
		    if(p.x>=feature.bbox[0] && p.x<= feature.bbox[2] &&
		       p.y>=feature.bbox[1] && p.y<=feature.bbox[3]) {
			let ptsWithin = turf.pointsWithinPolygon(pts,feature.searchWithin);
			if(ptsWithin.features.length>0) {
			    isIn = true;
			    feature.records.push(record);
			    return false;
			}
		    }
		    return true;
		});
		if(isIn) yes++;
		else no++;
	    });
	    let maxCount = -1,minCount=-1;
	    hexgrid.features.forEach((feature,fidx)=>{
		if(feature.records && feature.records.length>0) {
		    maxCount=maxCount==-1?feature.records.length:Math.max(maxCount, feature.records.length);
		    minCount=minCount==-1?feature.records.length:Math.min(minCount, feature.records.length);		    
		}
	    });
//	    console.timeEnd('x');
//	    console.log("yes:" + yes +" no:" + no);
	    this.hexmapLayer =this.createGeoJsonLayer('Hexmap',hexgrid,this.hexmapLayer);

            let colorBy = this.getColorByInfo(records,'hexmapColorBy',null,null,null,
					      this.lastColorBy,
					      {colorTableProperty:'hexmapColorTable'});
	    if(this.getHexmapShowCount()) {
		colorBy.setDoCount(minCount,maxCount);
	    }
	    this.lastColorBy = colorBy;
	    let textGetter = this.getTextGetter(fields);
	    let style = {
		strokeColor:this.getHexmapStrokeColor(),
		strokeDashstyle:this.getHexmapStrokeDashstyle(),
		strokeWidth:this.getHexmapStrokeWidth(),
		fillColor:this.getHexmapFillColor('transparent'),
		fillOpacity:this.getHexmapFillOpacity()				
	    }
	    let emptyStyle = {
		strokeColor:this.getHexmapEmptyStrokeColor(style.strokeColor),
		strokeWidth:this.getHexmapEmptyStrokeWidth(style.strokeWidth),
		fillColor:this.getHexmapEmptyFillColor(style.fillColor),
		fillOpacity:this.getHexmapFillOpacity()						
	    }
	    this.hexmapLayer.features.forEach((f,idx)=>{
		let records=hexgrid.features[idx].records;
		let s = (records && records.length>0)?style:emptyStyle;
		s = Utils.clone({},s);
		if(records && records.length>0 && colorBy.isEnabled()) {
		    s.fillColor= colorBy.getColorFromRecord(records);
		} 
		f.records  =records;
		f.style=s;
		f.textGetter  = textGetter;
	    });
	    this.hexmapLayer.redraw();
            if (colorBy.isEnabled()) {
		colorBy.displayColorTable();
	    }

	},
	


        addPoints: function(records, fields, points,bounds,debug) {
	    let mapType = this.getProperty('mapType');
	    if(mapType=='voronoi' || this.getProperty('doVoronoi',false)) {
		this.makeVoronoi(records,fields,points,bounds);
		if(!this.getShowPoints())
		    return;
	    }
	    if(mapType=='hex' ||
	       mapType=='triangle' ||
	       mapType=='square' ||
	       this.getDoHexmap() ||
	       this.getDoTrianglemap() ||
	       this.getDoSquaremap()) {
		this.makeHexmap(records,fields,points,bounds);
		if(!this.getShowPoints())
		    return;
	    }	    



	    if(this.getDoGridPoints()|| this.getDoHeatmap(false)) {
		let showMarkers = this.showMarkers;
		if(this.getHmShowPoints() || this.getShowPoints()) {
		    let show = true;
		    if(this.jq("showMarkersToggle").length>0) {
			show = this.jq("showMarkersToggle").is(":checked");
		    }
		    if(show) {
			this.createPoints(records, fields, points, bounds,debug);
			if(this.getHmShowToggle(false) && this.map.circles) {
			    this.map.setPointsVisibility(false);
			}
		    }
		}

		this.createHeatmap(records, fields, bounds);
		return;
	    }
	    if(this.getHtmlLayerField()) {
		this.createHtmlLayer(records, fields);
		return;
	    }
	    if(debugMapTime)
		console.time('createPoints');
	    this.createPoints(records, fields, points, bounds,debug);
	    if(debugMapTime) {
		console.timeEnd('createPoints');
		console.log('#points:' + records.length);
	    }
	},
        createPoints: function(records, fields, points,bounds, debug) {
	    debug = debug ||displayDebug.displayMapAddPoints;
	    let debugTimes  = false;
	    let features = [];
	    let featuresToAdd = [];
	    let pointsToAdd = [];	    
	    //getColorByInfo: function(records, prop,colorByMapProp, defaultColorTable,propPrefix) {
            let colorBy = this.getColorByInfo(records,null,null,null,null,this.lastColorBy);
	    this.lastColorBy = colorBy;
	    let cidx=0
	    let polygonField = this.getFieldById(fields, this.getProperty("polygonField"));
	    let polygonColorTable = this.getColorTable(true, "polygonColorTable",null);
	    let latlon = this.getProperty("latlon",true);
            let source = this;
            let radius = +this.getPropertyRadius(8);
	    let highlightRecords = this.getFilterHighlight();
	    let unhighlightFillColor = this.getUnhighlightColor();
	    let unhighlightStrokeWidth = this.getProperty("unhighlightStrokeWidth",0);
	    let unhighlightStrokeColor = this.getProperty("unhighlightStrokeColor","#aaa");
	    let unhighlightRadius = this.getUnhighlightRadius();
	    let strokeOpacity = this.getStrokeOpacity();
	    this.markers = {};

	    //change the order of the records if we are highlighting
	    if(highlightRecords) {
		let tmpRecords = [];
		let tmpPoints = [];		
		records.forEach((record,idx)=>{
		    if(!record.isHighlight(this)) {
			tmpRecords.push(record);
			tmpPoints.push(points[idx]);
		    }
		});
		records.forEach((record,idx)=>{
		    if(record.isHighlight(this)) {
			tmpRecords.push(record);
			tmpPoints.push(points[idx]);
		    }
		});

		records = tmpRecords;
		points = tmpPoints;
	    }

	    if(this.getPropertyScaleRadius()) {
		let seen ={};
		let numLocs = 0;
		points.every(p=>{
		    let key = p.x+"_"+p.y;
		    if(!seen[key]) {
			numLocs++;
			seen[key] = true;
		    }
		    return true;
		});
		let radiusScale = this.getPropertyRadiusScale();
		if(Utils.stringDefined(radiusScale)) {
		    radiusScale = radiusScale.split(",").map(t=>{return +t;});
		} else  {
		    //Just make up some numbers
		    radiusScale =[10000,2,6000,3,4500,4,3500,5,2600,6,1300,7,800,8,300,9,275,10,250,11,225,12,175,13,125,14,100,15,50,16];
		}
//		radius=radiusScale[1];
		for(let i=0;i<radiusScale.length;i+=2) {
		    if(numLocs<radiusScale[i]) {
			radius = radiusScale[i+1];
		    }
		}
//		console.log("#locs:" + numLocs +" #records:" +records.length + " radius:" + radius);
	    }
	    radius = Math.min(radius, this.getMaxRadius());



            let strokeWidth = +this.getPropertyStrokeWidth();
            let strokeColor = this.getPropertyStrokeColor();
            let sizeByAttr = this.getDisplayProp(source, "sizeBy", null);
            let isTrajectory = this.getDisplayProp(source, "isTrajectory", false);
            if (isTrajectory) {
		let tpoints = points.map(p=>{
		    return $.extend({},p);
		});
                let attrs = {
                    strokeWidth: this.getProperty("strokeWidth",2),
                    strokeColor: this.getPathColor(this.getStrokeColor("blue")),
		    fillColor:this.getProperty("fillColor","transparent")
                }
		if(tpoints.length==1) {
		    featuresToAdd.push(this.map.createPoint(ID,  tpoints[0], attrs, null));
		} else {
		    if(this.getShowPathEndPoint()) {
			featuresToAdd.push(this.map.createMarker("startpoint", tpoints[0],ramaddaCdn+"/icons/map/marker-green.png"));
			featuresToAdd.push(this.map.createMarker("endpoint", tpoints[tpoints.length-1],ramaddaCdn+"/icons/map/marker-blue.png"));
		    }
		    let poly = this.map.createPolygon(ID, "", tpoints, attrs, null);
		    poly.noSelect = true;
		    featuresToAdd.push(poly);
		}
		if(!this.getProperty("showPoints")) {
		    this.addFeatures(featuresToAdd);
                    return;
		}
            }

            let latField1 = this.getFieldById(fields, this.getProperty("latField1"));
            let latField2 = this.getFieldById(fields, this.getProperty("latField2"));
            let lonField1 = this.getFieldById(fields, this.getProperty("lonField1"));
            let lonField2 = this.getFieldById(fields, this.getProperty("lonField2"));
	    let showSegments = this.getShowSegments(false);
	    let greatCircle = this.getUseGreatCircle();
            if (greatCircle && (
		showSegments && latField1 && latField2 && lonField1 && lonField2) ||
		polygonField) {
		if(!MapUtils.loadTurf(()=>{
		    this.createPoints(records, fields, points,bounds, debug);		
		})) {
		    return;
		}

	    }


            let sizeSegments = this.getSizeSegments();
            let sizeEndPoints = this.getProperty("sizeEndPoints", true);
            let showEndPoints = this.getProperty("showEndPoints", false);
            let endPointSize = parseInt(this.getProperty("endPointSize", "4"));
            let dfltEndPointSize = endPointSize;
            let segmentWidth = parseInt(this.getSegmentWidth(1));
            let dfltSegmentWidth = segmentWidth;
	    let haveLayer = this.getShowLayers() && (this.getProperty("geojsonLayer") || this.getProperty("kmlLayer"));
            let showPoints = this.getProperty("showPoints", !haveLayer);
            let lineColor = this.getProperty("lineColor", "green");
	    let lineCap = this.getProperty('lineCap', 'round');
            let iconField = this.getFieldById(fields, this.getProperty("iconField"));

	    let makeLevelRange = v=>{
		if(!Utils.isDefined(v)) return null;
		let toks  = v.split(":");
		let r = {
		    min:Utils.stringDefined(toks[0])?+toks[0]:-1,
		    max:Utils.stringDefined(toks[1])?+toks[1]:100
		}
		if(isNaN(r.min)) r.min = -1;
		if(isNaN(r.max)) r.max = 100;
		return r;
	    };

	    this.glyphLevelRange =makeLevelRange(this.getProperty("glyphLevelRange"));
	    this.pointLevelRange =makeLevelRange(this.getProperty("pointLevelRange"));

	    let glyphs=RamaddaDisplayUtils.getGlyphs(this,fields,records);
	    let canvasBackground = this.getProperty('canvasBackground');
	    let canvasBorder = this.getProperty('canvasBorder');
	    let canvasWidth =this.getCanvasWidth();
	    let canvasHeight =this.getCanvasHeight();
	    let glyphSize =this.getProperty("glyphSize","32");


            let rotateField = this.getFieldById(fields, this.getProperty("rotateField"));	    
	    let markerIcon = this.getProperty("markerIcon",this.getProperty("pointIcon"));
	    if(markerIcon && markerIcon.startsWith("/")) {
                markerIcon =  ramaddaBaseUrl + markerIcon;
	    }
	    let usingIcon = markerIcon || iconField;
	    let showPoint = !usingIcon;
	    if(glyphs.length>0) showPoint=this.getProperty("showPoint",true);

            let iconSize = parseFloat(this.getProperty("iconSize",this.getProperty("radius",32)));
	    let iconMap = this.getIconMap();
	    let dfltShape = this.getProperty("defaultShape",null);
	    let dfltShapes = ["circle","triangle","star",  "square", "cross","x", "lightning","rectangle","church"];
	    let dfltShapeIdx=0;
	    let shapeBy = {
		id: this.getDisplayProp(source, "shapeBy", null),
		field:null,
		map: {}
	    }


	    if(this.getDisplayProp(source, "shapeByMap", null)) {
		this.getDisplayProp(source, "shapeByMap", null).split(",").forEach((pair)=>{
		    let tuple = pair.split(":");
		    shapeBy.map[tuple[0]] = tuple[1];
		})
	    }

	    let sizeBy = new SizeBy(this, this.getProperty("sizeByAllRecords",true)?this.getData().getRecords():records);
            for (let i = 0; i < fields.length; i++) {
                let field = fields[i];
                if (field.getId() == shapeBy.id || ("#" + (i + 1)) == shapeBy.id) {
                    shapeBy.field = field;
		    if (field.isString()) shapeBy.isString = true;
                }
            }
            shapeBy.index = shapeBy.field != null ? shapeBy.field.getIndex() : -1;


            if (this.getProperty("showColorByMenu", false) && colorBy.field && !this.madeColorByMenu) {
                this.madeColorByMenu = true;
                let menu = HU.open(SELECT,[CLASS,'ramadda-pulldown',ID,this.domId("colorByMenu")]);
                for (let i = 0; i < fields.length; i++) {
                    let field = fields[i];
                    if (!field.isNumeric() || field.isFieldGeo()) continue;
                    let extra = "";
                    if (colorBy.field.getId() == field.getId()) extra = "selected ";
                    menu += "<option value='" + field.getId() + "' " + extra + ">" + field.getLabel() + "</option>\n";
                }
                menu += HU.close(SELECT);
                this.writeHtml(ID_TOP_RIGHT, "Color by: " + menu);
                this.jq("colorByMenu").change(() => {
                    let value = this.jq("colorByMenu").val();
                    this.vectorMapApplied = false;
		    this.haveCalledUpdateUI = false;
                    this.setProperty("colorBy", value);
                    this.updateUI();
                });
            }



	    let dateMin = null;
	    let dateMax = null;

	    let dates = [];
            let justOneMarker = this.getPropertyJustOneMarker();

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
	    }


            if (dateMax) {
		if (this.getAnimationEnabled()) {
		    //TODO: figure out when to call this. We want to update the animation if it was from a filter change
		    //but not from an animation change. Hummmm.
		    //this.getAnimation().init(dateMin, dateMax,records);
		}
            }

	    this.removeFeatureLayer();
            let didColorBy = false;
            let seen = {};
	    let xnct =0;
	    let lastPoint;
	    let pathAttrs ={
		strokeColor: this.getProperty("pathColor",lineColor),
		strokeWidth: this.getProperty("pathWidth",1)
	    };


	    let fillColor = this.getFillColor();
	    let fillOpacity =  this.getFillOpacity();
	    let isPath = this.getProperty("isPath", false);
	    let groupByField = this.getFieldById(null,this.getProperty("groupByField"));
	    let groups;
	    if(groupByField)
		groups =  RecordUtil.groupBy(records, this, false, groupByField);
	    


	    let tooltip = this.getProperty("tooltip");
	    let highlight = this.getProperty("highlight");
	    let highlightTemplate = this.getProperty("highlightTemplate");
	    if(highlightTemplate)
		highlight=true;
	    
	    let highlightWidth = this.getProperty("highlightWidth",200);
	    let highlightHeight = this.getProperty("highlightHeight",-1);
	    let highlightSize = null;
	    if(highlightHeight>0) {
	    	highlightSize = MapUtils.createSize(highlightWidth,highlightHeight);
	    }

	    let addedPoints = [];
	    let textGetter = this.getTextGetter(fields);

	    let highlightGetter = f=>{
		if(f.record) {
                    return   HU.div([STYLE,HU.css('background','#fff')],this.getRecordHtml(f.record, fields, highlightTemplate|| tooltip));
		}
		return null;
	    };	    
	    this.haveAddPoints = true;
	    let displayInfo = this.displayInfo = {};
	    records.forEach(record=>{
		let recordLayout = displayInfo[record.getId()] = {
		    features:[],
		    visible:true
		}
		if(!record.point) {
		    recordLayout.x = record.getLongitude();
		    recordLayout.y = record.getLatitude();
		} else {
		    recordLayout.x = record.point.x;
		    recordLayout.y = record.point.y;
		}
	    });


	    if(this.getHandleCollisions()) {
		//TODO: labels
		let doLabels = this.getProperty("collisionLabels",false);
		if(doLabels &!this.map.collisionLabelsLayer) {
		    this.map.collisionLabelsLayer = MapUtils.createLayerVector("Collision Labels", {
			styleMap: MapUtils.createStyleMap({'default':{
                            label : "${label}"
			}}),
                    });
		    this.map.addVectorLayer(this.map.collisionLabelsLayer, true);
                    this.map.collisionLabelsLayer.setZIndex(100);
		}

		let mapBounds = this.map.getBounds();
		let mapW = mapBounds.right-mapBounds.left;
		let divW  = $("#" + this.getProperty(PROP_DIVID)).width();
		let pixelsPer = divW/mapW;
		let scaleFactor = 360/pixelsPer;
		let baseOffset = mapW*0.025;
		let offset = 0;
		let cnt = 0;
		let minPixels = this.getProperty("collisionMinPixels",16);
		//figure out the offset but use cnt so we don't go crazy
		while(pixelsPer*offset<minPixels && cnt<100) {
		    offset+=baseOffset;
		    cnt++;
		}
		let lineWidth = this.getProperty("collisionLineWidth","2");				
		let lineColor = this.getProperty("collisionLineColor","#000");
		//		console.log("checking collisions:" + mapBounds +" offset:" + offset);
		let seen1={};
//		let decimals =  parseFloat(this.getProperty("collisionRound",1));
		let decimals = -1;
		let pixels = [6,12,24,48,96,192];
		for(let i=0;i<pixels.length;i++) {
		    if(pixelsPer<pixels[i]) break;
		    decimals++;
		}
		let rnd = (v)=>{
		    if(decimals>0) {
			let v0 = Utils.roundDecimals(v,decimals);
			return v0;
		    }
		    v= Math.round(v);
		    if(decimals<0)
			if (v%2 != 0)
			    v--;
		    return v;
		};
		let getPoint = (p=>{
		    let lat = rnd(p.y);
		    let lon = rnd(p.x);
		    return MapUtils.createPoint(lon,lat);
		});
		let recordInfo = {};
		records.forEach(record=>{
		    let recordLayout = displayInfo[record.getId()];
		    if(recordLayout.x===null || recordLayout.y===null) return;
		    recordLayout.rpoint = getPoint(recordLayout);
		    if(seen1[recordLayout.rpoint]) {
			seen1[recordLayout.rpoint]++;
		    } else {
			seen1[recordLayout.rpoint]=1;
		    }
		});
		let collisionState= this.collisionState = {};
		records.forEach((record,idx)=>{
		    let recordLayout = displayInfo[record.getId()];
		    let point = recordLayout;
		    let rpoint = recordLayout.rpoint;
		    if(rpoint ==null) return;
		    if(seen1[rpoint]==1) {
			return;
		    } 
		    let cntAtPoint = seen1[rpoint];
		    let anglePer = 360/cntAtPoint;
		    let lineOffset = offset;
		    let delta = cntAtPoint/8;
		    if(delta>1)
			lineOffset*=delta;
		    let info = collisionState[rpoint];
		    if(!info) {
			info = collisionState[rpoint]= new CollisionInfo(this, seen1[rpoint], rpoint);
                        featuresToAdd.push(info.createDot(idx));
		    }
		    recordLayout.collisionInfo = info;
		    recordLayout.visible = info.visible;
		    info.addRecord(record);
		    let cnt = info.records.length;
		    let ep = Utils.rotate(rpoint.x,rpoint.y,rpoint.x,rpoint.y-lineOffset,cnt*anglePer-180,true);
		    let line = this.map.createLine("line-" + idx, "", rpoint.y,rpoint.x, ep.y,ep.x, {strokeColor:lineColor,strokeWidth:lineWidth});
		    if(!info.visible) {
			line.featureVisible = false;
			this.map.checkFeatureVisible(line,true);
		    }
		    info.addLine(line);
		    featuresToAdd.push(line);
		    point.x=ep.x;
		    point.y=ep.y;
		});
	    }

	    let i=0;
	    let sizeByFunc = function(percent, size) {
                if (sizeEndPoints &&!isNaN(percent)) {
		    endPointSize = dfltEndPointSize + parseInt(10 * percent);
                }
                if (sizeSegments) {
		    if(isNaN(percent)) {
			segmentWidth = dfltSegmentWidth + size;
		    } else {
			segmentWidth = dfltSegmentWidth + parseInt(10 * percent);
			segmentWidth=size;
			if(segmentWidth==0 || isNaN(segmentWidth)) segmentWidth=1;
		    }
                }
	    };


	    if(isPath && groups) {
		groups.values.forEach(value=>{
		    let firstRecord = null;
		    let lastRecord = null;
		    let secondRecord = null;		    
		    groups.map[value].forEach(record=>{
			if(!firstRecord) firstRecord=record;
			i++;
			if(lastRecord) {
			    pathAttrs.strokeColor = colorBy.getColorFromRecord(record, pathAttrs.strokeColor,true);
			    let line = this.map.createLine("line-" + i, "", lastRecord.getLatitude(), lastRecord.getLongitude(), record.getLatitude(),record.getLongitude(),pathAttrs);
			    featuresToAdd.push(line);
			    line.record=record;
			    line.textGetter=textGetter;
			}
			secondRecord = lastRecord;
			lastRecord = record;
			if(secondRecord) {
/*
			    let angleDeg = Utils.getBearing({x:lastRecord.getLongitude(),
							   
							   y:lastRecord.getLatitude()},
							  {x:secondRecord.getLongitude(),
							   y:secondRecord.getLatitude()});							  
//			    let endPoint = this.map.createPoint("endpoint", {x:lastRecord.getLongitude(),y:lastRecord.getLatitude()}, {fillColor:"red",strokeColor:"#000",pointRadius:6,graphicName:"arrow",rotation:angleDeg}, null);
//featuresToAdd.push(endPoint);
//                            featuresToAdd.push(endPoint);
*/
			}

		    });
		    if(lastRecord) {
			let color=  colorBy.getColorFromRecord(lastRecord, pathAttrs.strokeColor);
			if(secondRecord && this.getShowPathEndPoint(false)) {
			    let shape = this.getProperty("pathEndPointShape",null);
			    let angleDeg = Utils.getBearing({lon:secondRecord.getLongitude(),
							     lat:secondRecord.getLatitude()},
							    {lon:lastRecord.getLongitude(),
							     lat:lastRecord.getLatitude()});							  
			    let endPoint = this.map.createPoint("endpoint", {x:lastRecord.getLongitude(),y:lastRecord.getLatitude()}, {fillColor:color,strokeColor:"#000",pointRadius:6,graphicName:shape,rotation:angleDeg}, null);
			    featuresToAdd.push(endPoint);
			}
			if(this.getProperty("showPathStartPoint",false)) {
			    let endPoint = this.map.createPoint("startpoint", {x:firstRecord.getLongitude(),y:firstRecord.getLatitude()}, {fillColor:color,pointRadius:2}, null);
			    featuresToAdd.push(endPoint);
			}			
		    }
		});

	    }


	    let colorByEnabled = colorBy.isEnabled();
	    let graphicName = this.getPropertyShape();
	    let didMarker = false;
	    let times=[new Date()];
	    records.forEach((record,idx)=>{
		i++;
		let recordLayout = displayInfo[record.getId()];
		if(!recordLayout) return;
		let point  = recordLayout;
		if(!point) {
                    point = MapUtils.createPoint(record.getLongitude(), record.getLatitude());
		} else {
		    if(!Utils.isDefined(point.x) || !Utils.isDefined(point.y)) return;
		}

		if(justOneMarker) {
		    debug = false;
		    if(didMarker) {
			if(debug)
			    console.log("didMarker");
			return;
		    }
                    this.map.removeMarker(this.justOneMarker);
                    if(!isNaN(point.x) && !isNaN(point.y)) {
			didMarker = true;
                        this.justOneMarker= this.map.createMarker(id, [point.x,point.y], null, "", "");
			featuresToAdd.push(this.justOneMarker);
			if(debug) console.log("\tadding justOneMarker had initial position:" + this.hadInitialPosition);
			if(!this.hadInitialPosition) {
			    let loc = MapUtils.createLonLat(point.x,point.y);
			    if(debug) console.log("\tsetting center:" + loc);
			    this.map.setCenter(loc);
			}
                        return;
                    } else {
			return;
                    }
                }

                let values = record.getData();
                let props = {
		    strokeOpacity:strokeOpacity,
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


                segmentWidth = dfltSegmentWidth;
                props.pointRadius = sizeBy.getSize(values, props.pointRadius,sizeByFunc);
		if(props.pointRadius<0) return;
		if(isNaN(props.pointRadius) || props.pointRadius == 0) props.pointRadius= radius;
		let hasColorByValue = false;
		let colorByValue;
		let colorByColor;
		let theColor =  null;

		if(colorBy.compareFields.length>0) {
		    let maxColor = null;
		    let maxValue = 0;
		    colorBy.compareFields.forEach((f,idx)=>{
			let value = record.getData()[f.getIndex()];
			if(idx==0 || value>maxValue) {
			    maxColor = colorBy.colors[idx];
			    maxValue = value;
			}
		    });
		    colorByValue = maxValue;
		    theColor = maxColor;
		} else {
		    if(colorByEnabled) {
			let value = record.getData()[colorBy.index];
			colorByValue = value;
			theColor =  colorBy.getColorFromRecord(record, theColor,false);
//			if(idx<5) console.log("%cpt:" + value + " " + theColor,"background:" + theColor);
		    }
                }

		if(theColor) {
                    didColorBy = true;
		    hasColorByValue  = true;
		    colorByColor = props.fillColor = colorBy.convertColor(theColor, colorByValue);
		}

		if(highlightRecords && !record.isHighlight(this)) {
		    props.fillColor =  unhighlightFillColor;
		    props.strokeColor =  unhighlightStrokeColor;
		    props.strokeWidth=unhighlightStrokeWidth;
		    if(unhighlightRadius>0)
			props.pointRadius = unhighlightRadius;
		}

		if(polygonField) {
		    let s = values[polygonField.getIndex()];
		    let polygonProps ={};
		    $.extend(polygonProps,props);
		    polygonProps.fillColor = "transparent";
		    if(polygonProps.strokeWidth==0)
			polygonProps.strokeWidth=1;
		    if(polygonColorTable) {
			if(cidx>=polygonColorTable.length) cidx=0;
			polygonProps.strokeColor=polygonColorTable[cidx++];
		    }
		    let polys = this.map.createPolygonFromString(s, polygonProps,latlon,null);
		    polys.forEach(poly=>{
			poly.textGetter = textGetter;
			poly.record = record;
			let recordDate = record.getDate();
			if (recordDate) {
			    poly.date = recordDate.getTime();
			}
			featuresToAdd.push(poly);
		    });
		    polys.forEach(poly=>{
			featuresToAdd.push(poly);
		    });
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
		    attrs.strokeLinecap = lineCap;
		    attrs.strokeColor =   colorBy.getColorFromRecord(record, attrs.strokeColor);
                    attrs.strokeWidth = segmentWidth;
		    let line;
		    if(greatCircle) {
			let start = turf.point([lon1,lat1]);
			let end = turf.point([lon2,lat2]);
			let points = this.getTurfPoints(turf.greatCircle(start, end));
			let type = 'OpenLayers.Geometry.LineString';
			line =this.makeFeature(this.getMap(),type,attrs,points);		    
		    } else {
			line= this.map.createLine("line-" + i, "", lat1, lon1, lat2, lon2, attrs);
		    }
		    featuresToAdd.push(line);
		    line.record = record;
		    line.textGetter = textGetter;
		    if(highlight) {
			line.highlightTextGetter = highlightGetter;
			line.highlightSize = highlightSize;
		    }	
		    line.record = record;
                    featuresToAdd.push(line);
                    if (showEndPoints) {
                        let pointProps = {};
                        $.extend(pointProps, props);
                        pointProps.fillColor = attrs.strokeColor;
                        pointProps.strokeColor = attrs.strokeColor;
                        pointProps.pointRadius = dfltEndPointSize;
                        pointProps.pointRadius = endPointSize;
                        let p1 = MapUtils.createLonLat(lon1, lat1);
                        let p2 = MapUtils.createLonLat(lon2, lat2);
                        if (!Utils.isDefined(seen[p1])) {
                            seen[p1] = true;
			    let pt1 =this.map.createPoint("endpt-" + i, p1, pointProps);
			    featuresToAdd.push(pt1);
			    pt1.record = record;
			    pt1.textGetter = textGetter;
                            featuresToAdd.push(pt1);
                        }
                        if (!Utils.isDefined(seen[p2])) {
                            seen[p2] = true;
                            let pt2 = this.map.createPoint("endpt2-" + i, p2, pointProps);
			    featuresToAdd.push(pt2);
			    pt2.record = record;
			    pt2.textGetter = textGetter;
                            featuresToAdd.push(pt2);
                        }

                    }
		}
                //We do this because openlayers gets really slow when there are lots of features at one point
		let key = point.x*10000 + point.y;
		if (!seen[key]) {
		    seen[key] = 1;
		}  else {
		    //			console.log(this.formatDate(record.getDate()) +" " + record.getLatitude() + " " + seen[key]);
		    if (seen[key] > 500) {
			return;
		    }
		    seen[key]++;
		}


		let mapPoint=null;
		let mapPoints =recordLayout.features;

		//marker

		if(usingIcon) {
		    if(iconField) {
			let tuple = record.getData();
			let icon = tuple[iconField.getIndex()];
			if(iconMap) {
			    icon = iconMap[icon];
			    if(!icon) icon = this.getMarkerIcon();
			}
			let size = iconSize;
			if(sizeBy.index>=0) {
			    size = props.pointRadius;
			}
			mapPoint = this.map.createMarker("pt-" + i, point, icon, "pt-" + i,null,null,size);
			mapPoint.isMarker = true;
			mapPoints.push(mapPoint);
			this.markers[record.getId()] = mapPoint;
			pointsToAdd.push(mapPoint);
		    } else  {
			let attrs = {
			}
			if(rotateField) attrs.rotation = record.getValue(rotateField.getIndex());
			mapPoint = this.map.createMarker("pt-" + i, point, markerIcon, "pt-" + i,null,null,iconSize,null,null,attrs);
			mapPoint.levelRange = this.pointLevelRange;
			mapPoint.textGetter= textGetter;
			pointsToAdd.push(mapPoint);
			mapPoint.isMarker = true;
			mapPoints.push(mapPoint);
			this.markers[record.getId()] = mapPoint;
		    }
		}


		if(glyphs.length>0) {
		    let cid = HU.getUniqueId("canvas_");
		    let c = HU.tag("canvas",[CLASS,"", WIDTH,canvasWidth,HEIGHT,canvasHeight,ID,cid]);
		    $(document.body).append(c);
		    let canvas = document.getElementById(cid);
		    let ctx = canvas.getContext("2d");

		    if(canvasBackground) {
			ctx.fillStyle=canvasBackground;
			ctx.fillRect(0,0,canvasWidth,canvasHeight);
		    }
		    if(canvasBorder) {
			ctx.strokeStyle = canvasBorder;
			ctx.strokeRect(0,0,canvasWidth,canvasHeight);
		    }
		    ctx.strokeStyle ="#000";
		    ctx.fillStyle="#000";
		    let pending = [];
		    glyphs.forEach(glyph=>{
			let isReady =  glyph.draw({}, canvas, ctx, 0,canvasHeight,{record:record});
			if(isReady) pending.push(isReady);
		    });
		    let img = canvas.toDataURL();
		    let size = glyphSize;
		    if(sizeBy.index>=0) {
			size = props.pointRadius;
		    }
		    mapPoint = this.map.createMarker("pt-" + i, point, img, "pt-" + i,null,null,size);
		    mapPoint.levelRange=this.glyphLevelRange;
		    mapPoint.isMarker = true;
		    mapPoints.push(mapPoint);
		    this.markers[record.getId()] = mapPoint;
		    pointsToAdd.push(mapPoint);
		}


		if(showPoint || colorByEnabled)  {
		    if(!props.graphicName)
			props.graphicName = graphicName;
		    if(rotateField) props.rotation = record.getValue(rotateField.getIndex());
		    props.fillColor =   colorBy.getColorFromRecord(record, props.fillColor);

		    if(radius>0) {
			mapPoint = this.map.createPoint("pt-" + i, point, props, null);
			mapPoint.levelRange = this.pointLevelRange;
			pointsToAdd.push(mapPoint);
			this.markers[record.getId()] = mapPoint;
			mapPoints.push(mapPoint);
		    }
		}


		if(isPath && !groups && lastPoint) {
		    pathAttrs.strokeColor = colorBy.getColorFromRecord(record, pathAttrs.strokeColor);
		    let line = this.map.createLine("line-" + i, "", lastPoint.y, lastPoint.x, point.y,point.x,pathAttrs);
		    pointsToAdd.push(line);
		}
		lastPoint = point;
		if(features) {
		    mapPoints.forEach(f=>{features.push(f);});
		}
                let date = record.getDate();

		mapPoints.forEach(mapPoint=>{
		    if(highlight) {
			mapPoint.highlightTextGetter = highlightGetter;
			mapPoint.highlightSize = highlightSize;
		    }
		    mapPoint.record = record;
		    mapPoint.textGetter = textGetter;
		    mapPoint.hasColorByValue = hasColorByValue;
		    mapPoint.colorByValue= colorByValue;
		    mapPoint.colorByColor = colorByColor;
		    if (date) {
			mapPoint.date = date.getTime();
		    }
		    if(!recordLayout.visible) {
			mapPoint.featureVisible = false;
			this.map.checkFeatureVisible(mapPoint,true,records.length<20);
		    }
		});

		if(recordLayout.collisionInfo) {
		    recordLayout.collisionInfo.addPoints(mapPoints);
		}
	    });
	    
	    times.push(new Date());
	    
	    if(showPoints) {
		this.addFeatures(pointsToAdd);
	    }
	    this.myPoints = pointsToAdd;
	    this.addFeatures(featuresToAdd);
	    times.push(new Date());
	    if(debugTimes)
		Utils.displayTimes("map: #records:" + records.length+" times:",times, true,["create markers","add features"]);


	    if(records.length>0 && this.getProperty("selectFirstRecord")&& !this.haveSelectedFirstRecord) {
		this.haveSelectedFirstRecord = true;
		let record = records[0];
		this.propagateEventRecordSelection({record:record});
		let displayDiv = this.getProperty("displayDiv", null);
		if(displayDiv && this.textGetter) {
		    $("#" + displayDiv).html(this.textGetter({record:record}));
		}
		let marker =  this.markers[record.getId()];
		if(marker) {
		    this.map.handleFeatureclick(null,marker);
		}
	    }

	    if (showSegments) {
		this.map.centerOnMarkers();
	    }


//Don't think we have to do this here. Saves lots of draw time
//	    if(this.map.circles)
//		this.map.circles.redraw();


	    let legendSide = this.getProperty("sizeByLegendSide");
	    if(legendSide) {
		let legend = sizeBy.getLegend(5,fillColor,legendSide=="left" || legendSide=="right");
		if(legend !="") {
		    let style = this.getProperty("sizeByLegendStyle");
		    if(style) legend = HU.div([STYLE,style],legend);
		    this.jq(ID_SIZEBY_LEGEND).html(legend);
		    this.callingUpdateSize = true;
		    this.map.getMap().updateSize();
		    this.callingUpdateSize = false;
		}
	    }
	    times = [new Date()];
	    this.jq(ID_BOTTOM).append(HU.div([ID,this.domId(ID_SHAPES)]));
	    if (didColorBy) {
		this.showColorTable(colorBy);
	    }
	    times.push(new Date());
//	    Utils.displayTimes("final map points:",times, true);

	    if(iconField&& iconMap) {
		let html = "";
		for(a in iconMap) {
		    html+=HU.image(iconMap[a],["width","32"]) +" " + a+" ";
		}
		this.jq(ID_SHAPES).html(HU.center(html));
	    }

	    if(shapeBy.field) {
		let shapes = shapeBy.field.getLabel()+": ";
		for(v in shapeBy.map) {
		    let shape = shapeBy.map[v];
		    if(shape=="circle") shape=HU.getIconImage("fa-circle");
		    else if(shape=="square") shape=HU.getIconImage("fa-square");		    
		    else if(shape=="rectangle") shape=HU.getIconImage("fa-square");		    
		    else if(shape=="star") shape=HU.getIconImage("fa-star");		    
		    else if(shape=="triangle") shape=HU.getIconImage("/icons/triangle.png",["width","16px"]);		    
		    else if(shape=="lightning") shape=HU.getIconImage("/icons/lightning.png",["width","16px"]);		    
		    else if(shape=="cross") shape=HU.getIconImage("/icons/cross.png",["width","16px"]);		    
		    else if(shape=="church") shape=HU.getIconImage("fa-cross");
		    shapes+=shape+" " + v +SPACE2;
		}
		this.jq(ID_SHAPES).html(HU.center(shapes));
	    }

	    if (this.getProperty("animationTakeStep", false)) {
		this.getAnimation().doNext();
	    }

	    if(this.pointLevelRange || this.glyphLevelRange) {
		this.checkLevelRange([this.myFeatureLayer],true);
	    }		


        },

	getTextGetter:function(fields) {
	    let tooltip = this.getProperty("tooltip");
	    return  this.textGetter = f=>{
		if(!Utils.stringDefined(tooltip)) {
		    if(debugPopup) console.log("No tooltip");
		    return null;
		}

		let records;
		if(f.record) records = [f.record];
		else records = f.records;
		if(!records || records.length==0) return null;
		let text ='';
		records.forEach((record,idx)=>{
		    if(idx>0) text+='<thin_hr/>';
		    text +=   this.getRecordHtml(record, fields, tooltip);
		});
		if(debugPopup) console.log("textGetter: getRecordHtml:"  + text);
		if(text=="") return null;
		return text;
	    };
	},


        addLabels:function(records, fields) {
	    let limit = this.getLabelLimit(1000);
	    if(records.length>limit) return;
	    

	    let pixelsPerCell = 10;
            let width = this.map.getMap().viewPortDiv.offsetWidth;
            let height = this.map.getMap().viewPortDiv.offsetHeight;
	    let bounds = this.map.getBounds();
	    let numCellsX = Math.round(width/pixelsPerCell);
	    let numCellsY = Math.round(height/pixelsPerCell);	    
	    let cellWidth = (bounds.right-bounds.left)/numCellsX;
	    let cellHeight = (bounds.top-bounds.bottom)/numCellsY;	    
	    let grid = {};
	    let doLabelGrid = this.getDoLabelGrid();
	    //
            let labelTemplate = this.getLabelTemplate();
	    let labelKeyField;
	    if(this.getLabelKeyField()) {
		labelKeyField = this.getFieldById(fields,this.getLabelKeyField());
	    }
            if(!labelTemplate && !labelKeyField) return;
	    if(labelKeyField) labelTemplate= "${_key}";
	    labelTemplate = labelTemplate.replace(/_nl_/g,"\n");
	    if(!this.map.labelLayer) {
		this.map.labelLayer = MapUtils.createLayerVector("Labels", {
		    styleMap: MapUtils.createStyleMap({'default':{
                        label : labelTemplate,
                        fontColor: this.getProperty("labelFontColor","#000"),
                        fontSize: this.getProperty("labelFontSize","10pt"),
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


	    let keyMap={};
	    let keyLegend="";
	    let features =  [];
            let seen = {};
	    let colorBy = this.getProperty("colorBy");
	    let sizeBy = this.getProperty("sizeBy");
	    let keyIndex = 0;
	    let alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("");
	    let  keys = [];
	    for(let j=0;j<3;j++) {
		alpha.forEach(c=>{
		    let cc = "";
		    for(let i=0;i<=j;i++) {
			cc+=c;
		    }
		    keys.push(cc);
		});
	    }
            for (let i = 0; i < records.length; i++) {
		let record = records[i];
                let point = record.point;
		//For now just set the lat/lon
		let indexX = Math.floor((record.getLongitude()-bounds.left)/cellWidth);
		let indexY = Math.floor((record.getLatitude()-bounds.bottom)/cellHeight);		
//		if(i<10)   console.log(record.getLongitude() +" " + record.getLatitude() +" " + indexX +" " + indexY);
		if(doLabelGrid) {
		    let key = indexX +"_"+ indexY;
		    if(grid[key]) continue;
		    grid[key] = true;
		}
		point = {x:record.getLongitude(),y:record.getLatitude()};
                let center = MapUtils.createPoint(point.x, point.y);
                center.transform(this.map.displayProjection, this.map.sourceProjection);
                let pointFeature = MapUtils.createVector(center);
                pointFeature.noSelect = true;
                pointFeature.attributes = {
                };
                pointFeature.attributes[RECORD_INDEX] = (i+1);
                pointFeature.attributes["recordIndex"] = (i+1)+"";
		if(labelKeyField) {
		    let v = labelKeyField.getValue(record);
		    if(!keyMap[v]) {
			if(keyIndex>=keys.length) keyIndex=0;
			keyMap[v] = keys[keyIndex++];
			keyLegend+=keyMap[v]+": " + v+"<br>";
		    }
		    pointFeature.attributes["_key"] = keyMap[v];
                }
		for (let fieldIdx = 0;fieldIdx < fields.length; fieldIdx++) {
		    let field = fields[fieldIdx];
		    pointFeature.attributes[field.getId()] = field.getValue(record);
		    if(colorBy && field.getId() == colorBy) {
			pointFeature.attributes["colorBy"] = field.getValue(record);
		    }
		    if(sizeBy && field.getId() == sizeBy) {
			pointFeature.attributes["sizeBy"] = field.getValue(record);
                    }
		}
                features.push(pointFeature);
	    }
	    if(keyLegend.length>0) {
		if(!this.legendId) {
		    this.legendId = this.domId("legendid");
		    this.jq(ID_RIGHT).append(HU.div([ID,this.legendId, CLASS,"display-map-legend",STYLE, HU.css("max-height",this.getHeight("400px"))]));
		}
		this.jq("legendid").html(keyLegend);
	    }
	    if(this.labelFeatures)
		this.map.labelLayer.removeFeatures(this.labelFeatures);
            this.map.labelLayer.addFeatures(features);
	    this.labelFeatures = features;
	    $("#" + this.map.labelLayer.id).css("z-index",900);
        },


        handleEventRemoveDisplay: function(source, display) {
            if (!this.map) {
                return;
            }
            let mapEntryInfo = this.mapEntryInfos[display];
            if (mapEntryInfo != null) {
                mapEntryInfo.removeFromMap(this.map);
            }
            let feature = this.findFeature(display, true);
            if (feature != null) {
                if (feature.line != null) {
                    this.map.removePolygon(feature.line);
                }
            }
        },
        findFeature: function(source, andDelete) {
            for (let i in this.features) {
                let feature = this.features[i];
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
                let icon = this.getProperty("markerIcon");
                if (icon.startsWith("/"))
                    return ramaddaBaseUrl + icon;
                else
                    return icon;
            }
            displayMapCurrentMarker++;
            if (displayMapCurrentMarker >= displayMapMarkers.length) displayMapCurrentMarker = 0;
            return ramaddaCdn + "/lib/openlayers/v2/img/" + displayMapMarkers[displayMapCurrentMarker];
        },
	highlightMarker:null,
        handleEventRecordHighlight: function(source, args) {
	    SUPER.handleEventRecordHighlight.call(this,source,args);
	    if(displayDebug.handleEventRecordSelect)
		this.logMsg("handleEvent");
	    
	    if(this.getRecordHighlightFeature() ||
	       isNaN(args.record.getLatitude()) ||
	       isNaN(args.record.getLongitude())) {
		if(this.recordToFeature) {
		    let feature = this.recordToFeature[args.record.getId()];
		    if(this.highlightedFeature) {
			this.getMap().unhighlightFeature(this.highlightedFeature);
		    }
		    this.highlightedFeature = feature;
		    if(feature) {
			let style = {};
			if(feature.style) style = $.extend(style,feature.style);
			style = $.extend(style,{
			    strokeColor: this.getRecordHighlightStrokeColor('red'),
			    strokeWidth: this.getRecordHighlightStrokeWidth(3)}
					);
			this.getMap().highlightFeature(feature,style);
						       
		    }
		}
	    } else {
		this.highlightPoint(args.record.getLatitude(),args.record.getLongitude(),args.highlight,true);
	    }
	},
        handleEventRecordSelection: function(source, args) {
	    SUPER.handleEventRecordSelection.call(this, source, args);
            if (!this.map) {
                return;
            }
	    args.highlight = true;
            if (!this.getShowRecordSelection(true)) {
		return;
	    }
	    this.handleEventRecordHighlight(source,args);
	    //For now return
	    if(true) return;

            let record = args.record;
            if (record.hasLocation()) {
                let latitude = record.getLatitude();
                let longitude = record.getLongitude();
                if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) return;
                let point = MapUtils.createLonLat(longitude, latitude);
                let marker = this.myMarkers[source];
                if (marker != null) {
                    this.map.removeMarker(marker);
                }
                let icon = displayMapMarkerIcons[source];
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


let ramaddaGridState = {
    grids:{
    },
    loading:{
    }

};

function RamaddaMapgridDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_MAPGRID, properties);
    let myProps = [
	{label:'Grid Map Attributes'},
	{p:'localeField',ex:''},
	{p:'grid',ex:'countries|us'},
	{p:'cellSize',ex:'30',tt:'use 0 for flexible width'},
	{p:'cellHeight',ex:'30'},
	{p:'showCellLabel',ex:'false'},
    ];
    myProps.push(...RamaddaDisplayUtils.sparklineProps);

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
        displayData: function(reload) {
	    this.updateUI();
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
	    this.updateUI({colorByFieldChanged:true});
	},
	updateUI: function() {
	    let what = this.getProperty("grid","us");
	    let grid = ramaddaGridState.grids[what];
	    if(!grid) {
		if(ramaddaGridState.loading[what]) return;
		ramaddaGridState.loading[what] = true;
		let url = ramaddaCdn +"/resources/grid_"+ what+".json";
		$.getJSON(url, ( data ) =>{
		    ramaddaGridState.grids[what] = data;
		    this.updateUI();
		}).fail(function(err) {
		    console.log( "error loading " + url);
		    console.dir(err)
		})
		return;
	    }


	    let records = this.filterData();
	    if(!records) return;
            let fields = this.getData().getNonGeoFields();
	    let localeField = this.getFieldById(fields,this.getProperty("localeField","state"));
	    if(localeField==null) {
		localeField = this.getFieldById(fields,"state");
	    }
	    let minx = Number.MAX_VALUE;
	    let miny = Number.MAX_VALUE;
	    let maxx = Number.MIN_VALUE;
	    let maxy = Number.MIN_VALUE;
	    let map = {};


	    grid.forEach(o=>{
		minx = Math.min(minx,o.x);
		maxx = Math.max(maxx,o.x);
		miny = Math.min(miny,o.y);
		maxy = Math.max(maxy,o.y);
		map[this.domId("cell_" +o.x+ "_"+o.y)] = o;
	    });

            let colorBy = this.getColorByInfo(records);
	    let sparkLinesColorBy = this.getColorByInfo(records,"sparklineColorBy");
	    let strokeColorBy = this.getColorByInfo(records,"strokeColorBy","strokeColorByMap");
	    let sparkLineField = this.getFieldById(fields,this.getProperty("sparklineField"));
	    let table =HU.open(TABLE,[WIDTH,"100%"]);
	    let width = this.getProperty("cellWidth", this.getProperty("cellSize",0));
	    let height = this.getProperty("cellHeight",width);
	    if(height==0) height=30;
	    let showLabel  = this.getProperty("showCellLabel",true);
	    let cellStyle  = this.getProperty("cellStyle","");
	    let cellMap = {};
	    for(let y=1;y<=maxy;y++) {
		table+=HU.open(TR);
		for(let x=1;x<=maxx;x++) {
		    let id = this.domId("cell_" +x+ "_"+y);
		    let o = map[id];
		    let extra = " id='" + id +"' ";
		    let style = HU.css('position','relative','margin','1px','vertical-align','center','text-align','center',HEIGHT, height+"px");
		    if(width>0) style+=HU.css(WIDTH,width+'px');
		    let c = "";
		    if(o) {
			style+="background:#ccc;" + cellStyle;
			if(!sparkLineField) {
			    extra += " title='" + o.name +"' ";
			}
			extra += HU.attr(CLASS,'display-mapgrid-cell');
			c = HU.div([STYLE,HU.css('padding-left','3px')], (showLabel?o.codes[0]:""));
			o.codes.forEach(c=>cellMap[c] = id);
			cellMap[o.name] = id;
		    }
		    let td = HU.td([],"<div " + extra +" style='" + style +"'>" + c+"</div>");
		    table+=td;
		}
		table+=HU.close(TR);
	    }
	    table +=HU.tr([],HU.td(["colspan", maxx],"<br>" +   HU.div([ID,this.domId(ID_COLORTABLE)])));
	    table+=HU.close(TABLE);
	    this.setContents(HU.center(table));

	    let states = [];
	    let stateData = this.stateData = {
	    }
	    let minData = 0;
	    let maxData = 0;
	    let seen = {};
	    let contents = this.getContents();
	    for(let i=0;i<records.length;i++) {
		let record = records[i]; 
		let tuple = record.getData();
		let state = tuple[localeField.getIndex()];
		let cellId = cellMap[state];
		if(!cellId) {
		    cellId = cellMap[state.toUpperCase()];
		}
		if(!cellId) {
		    //		    console.log("Could not find cell:" + state);
		    continue;
		}
		$("#"+cellId).attr(RECORD_INDEX,i);

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

		let colorByEnabled = colorBy.isEnabled();

		//TODO: sort the state data on time
                if (colorByEnabled) {
                    let value = record.getData()[colorBy.index];
		    let color = colorBy.getColorFromRecord(record);
		    let cell = contents.find("#" + cellId);
		    cell.css("background",color);
		    let foreground = Utils.getForegroundColor(color);
		    if(foreground) {
			cell.css('color', foreground);
		    }
		    cell.attr(RECORD_INDEX,i);
                }
		if (strokeColorBy.isEnabled()) {
                    let value = record.getData()[strokeColorBy.index];
		    let color = strokeColorBy.getColor(value, record);
		    let cell = contents.find("#" + cellId);
		    cell.css("border-color",color);
		    cell.css("border-width","2px");
                }
	    }

	    if(sparkLineField) {
		let vOffset = 0;
		states.forEach((state,idx)=>{
		    let s = stateData[state];
		    let innerId = s.cellId+"_inner";
		    let cellWidth = width;
		    if(cellWidth==0) {
			cellWidth = $("#" + s.cellId).width();
		    }
		    let style = HU.css(WIDTH,cellWidth+'px',HEIGHT, (height-vOffset) +'px','position','absolute','left','0px','top', vOffset+'px');
		    let innerDiv = HU.div([ID, innerId, STYLE,style]);
		    $("#" + s.cellId).append(innerDiv);
		    drawSparkLine(this, "#"+innerId,cellWidth,height-vOffset,s.data,s.records,minData,maxData,sparkLinesColorBy);
		});
	    }

	    this.makePopups(contents.find(".display-mapgrid-cell"), records);
	    let _this = this;
	    contents.find(".display-mapgrid-cell").click(function() {
		let record = records[$(this).attr(RECORD_INDEX)];
		if(record) {
		    _this.propagateEventRecordSelection({record: record});
		}
	    });	
	    if(!sparkLineField) {
		this.makeTooltips(contents.find(".display-mapgrid-cell"), records, null, "${default}");
	    }
            if (colorBy.index >= 0) {
		colorBy.displayColorTable();
	    }
	    if (sparkLinesColorBy.index >= 0) {
		sparkLinesColorBy.displayColorTable();
	    }
	},

        handleEventRecordSelection: function(source, args) {
	    let contents = this.getContents();
	    if(this.selectedCell) {
		this.selectedCell.css("border",this.selectedBorder);
	    }
	    let index = this.recordToIndex[args.record.getId()];
	    if(!Utils.isDefined(index)) return;
	    this.selectedCell = contents.find(HU.attrSelect(RECORD_INDEX, index));
	    this.selectedBorder = this.selectedCell.css("border");
	    this.selectedCell.css("border","1px solid red");
	},
    })}




const ID_BASEMAP = "basemap";
function RamaddaOtherMapDisplay(displayManager, id, type, properties) {
    const SUPER = new RamaddaFieldsDisplay(displayManager, id, type, properties);
    let myProps = [
	{label:'Base map properties'},
	{p:'regionField',ex:''},
	{p:'mapFile',ex:'usmap.json|countries.json',d:"usmap.json"},
	{p:'mapEntry',tt:'entry id of geojson map file'},
	{p:'mapFeature',tt:'feature name to match data with',canCache:true},		
	{p:'valueField',tt:'Field to get the height of each polygon from',ex:''},
	{p:'skipRegions',ex:'Alaska,Hawaii'},
	{p:'pruneMissing',ex:'true'},				
	{p:'mapBackground',ex:'transparent'},
	{p:'transforms',ex:"Alaska,0.4,30,-40;Hawaii,2,50,5;Region,scale,offsetX,offsetY"},
	{p:'prunes',ex:'Alaska,100;Region,maxCount'},
	{p:'mapWidth',ex:'600'},
	{p:'mapHeight',ex:'400'},
	{p:'maxLon'},
	{p:'minLon'},
	{p:'maxLat'},
	{p:'minLat'},			
	{p:"strokeColor"},
	{p:"strokeWidth"},
	{p:"missingFill"},			
    ];

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        checkLayout: function() {
            this.updateUI();
        },
        makeMap: function() {
	},
	makePoly:function(polygon) {
	    let poly = [];
	    polygon.forEach(point=>{
		let lon = point[0];
		let lat = point[1];
		if(isNaN(lon) || isNaN(lat)) return;
		poly.push({x:lon,y:lat});
	    });
	    return poly;
	},
	findValues:function(region, valueMap) {
	    if(valueMap[region]) return valueMap[region];
	    let values = null;
	    if(!this.aliasMap[region]) {
		return null;
	    }
	    this.aliasMap[region].forEach(alias=>{
		if(valueMap[alias]) values = valueMap[alias];
	    });
	    return values;
	},
	makeValueMap: function(records,needsValue) {
	    let regionField=this.getFieldById(null,this.getPropertyRegionField());
	    let valueField=this.getFieldById(null,this.getPropertyValueField());	    
	    if(!regionField) {
                this.displayError("No region field specified");
		return null
	    }
	    if(!valueField && needsValue) {
                this.displayError("No value field specified");
		return null
	    }	    
	    //If the colorBy wasn't set then use the value field
	    if(valueField) {
		if(this.getProperty("colorBy")==null) this.setProperty("colorBy",valueField.getId());
		if(this.getProperty("sizeBy")==null) this.setProperty("sizeBy",valueField.getId());	    
	    }
	    let valueMap = {};
	    this.valueRange = {
		min: null,
		max:null
	    };
	    this.idToRecord = {};
	    records.forEach(record=>{
		let region = record.getValue(regionField.getIndex());
		this.idToRecord[record.getId()] = record;
		let values  = valueMap[region] = {
		    record:record
		}
		if(valueField) { 
		    let value = record.getValue(valueField.getIndex());
		    values.value = value;
		    this.valueRange.min = this.valueRange.min===null?value:Math.min(value,this.valueRange.min);
		    this.valueRange.max = this.valueRange.max===null?value:Math.max(value,this.valueRange.max);
		}
	    });
	    if(valueField) {
		records.forEach(record=>{
		    let region = record.getValue(regionField.getIndex());
		    let values  = valueMap[region];
		    let value = values.value
		    let percent = (value-this.valueRange.min)/(this.valueRange.max-this.valueRange.min);
		    values.percent = percent;
		});
	    }
	    return valueMap;
	},
	writeMap:function(skipHeight)  {
	    let width = this.getMapWidth(this.getProperty("width",800));
	    let css = HU.css(BACKGROUND,this.getMapBackground("transparent"),WIDTH,HU.getDimension(width));
	    let height;
	    if(!skipHeight) {
		height = this.getMapHeight(this.getProperty("height"));
		let mw = this.mapRange.maxLon-this.mapRange.minLon;
		let mh = this.mapRange.maxLat-this.mapRange.minLat;
		if(!height)
		    height = mh/mw*width;
		if(isNaN(height)) height=400; 
		css+=HU.css(HEIGHT,HU.getDimension(height));
	    }
	    
	    this.mapRange.maxLon= this.getPropertyMaxLon(this.mapRange.maxLon);
	    this.mapRange.minLon= this.getPropertyMinLon(this.mapRange.minLon);
	    this.mapRange.maxLat= this.getPropertyMaxLat(this.mapRange.maxLat);
	    this.mapRange.minLat= this.getPropertyMinLat(this.mapRange.minLat);	    	    
	    this.setContents(HU.div([ID,this.domId(ID_BASEMAP),STYLE,css]));
	    if(isNaN(width)) {
		width = this.getContents().width();
	    }
	    return [width,height];

	},
	makeSvg: function(width,height) {
	    const svg = d3.select("#" + this.domId(ID_BASEMAP)).append('svg')
		  .attr('width', width)
		  .attr('height', height)
		  .append('g')
	    let padx = 0;
	    let pady = padx;
	    let scaleX  = d3.scaleLinear().domain([this.mapRange.minLon, this.mapRange.maxLon]).range([padx, width-padx]);
	    let scaleY  = d3.scaleLinear().domain([this.mapRange.maxLat, this.mapRange.minLat]).range([pady, height-pady]);
	    return [svg,scaleX,scaleY];
	},

	clearTooltip: function() {
	    if(this.tooltipDiv)
		this.tooltipDiv.style("opacity", 0);
	},
	makeTooltipDiv: function() {
	    if(!this.tooltipDiv) {
		this.tooltipDiv = d3.select("body").append("div")
		    .attr("class", "ramadda-shadow-box  display-tooltip")
		    .style("opacity", 0)
		    .style("position", "absolute")
		    .style("background", "#fff")
	    }
	    this.clearTooltip();
	    return this.tooltipDiv;
	},
	addEvents:function(polys, idToRecord, tooltipDiv) {
	    let _this = this;
	    idToRecord  = idToRecord|| this.idToRecord;
	    tooltipDiv = tooltipDiv || this.makeTooltipDiv();
	    polys.on('click', function (d, i) {
		let poly = d3.select(this);
		let record = idToRecord[poly.attr(RECORD_ID)];
		if(record)
		    _this.propagateEventRecordSelection({record: record});
	    });
	    polys.on('mouseover', function (d, i) {
		let poly = d3.select(this);
		let record = idToRecord[poly.attr(RECORD_ID)];
		poly.attr("lastStroke",poly.attr("stroke"))
		    .attr("lastFill",poly.attr("fill"));
		poly.attr("stroke",_this.getProperty('highlightStrokeColor','blue')).attr("stroke-width",_this.getProperty('highlightStrokeWidth',1))
		    .attr("fill",_this.getProperty('highlightFillColor','blue'));
		let tooltip = _this.getProperty("tooltip");
		if(!tooltip) return;
		let regionName = poly.attr("regionName");
		let tt = null;
		if(!record) {
		    tt = regionName;
		    console.log("no record found for region:" +regionName);
		} else {
		    _this.propagateEventRecordSelection({highlight:true,record: record});
		    tt =  _this.getRecordHtml(record,null,tooltip);
		}
		if(tt) {
		    _this.tooltipDiv.html(tt)
			.style("left", (d3.event.pageX + 10) + "px")
			.style("top", (d3.event.pageY + 20) + "px");
		    _this.tooltipDiv.style("opacity", 1);
		    //For now don't transition as it seems to screw up
		    //subsequent mouse overs
		    /*
		    _this.tooltipDiv.transition()
			.delay(500)
			.duration(500)
			.style("opacity", 1);
		    */
		}
	    });
	    polys.on('mouseout', function (d, i) {
//		_this.tooltipDiv.transition();
		let poly = d3.select(this);
		poly.attr("stroke",poly.attr("lastStroke"))
		    .attr("fill",poly.attr("lastFill"))
		    .attr("stroke-width",1);
		_this.tooltipDiv.style("opacity", 0);
	    });
	},
        updateUI: function() {
	    this.clearTooltip();
	    if(!this.mapJson) {
		if(!this.gettingFile) {
		    this.gettingFile = true;
		    let mapFile = this.getPropertyMapFile();
		    let mapEntry = this.getMapEntry();
		    if(mapEntry!=null) {
			mapFile = ramaddaBaseUrl + "/entry/get?entryid=" + mapEntry;
		    } else {
			if(!mapFile.startsWith("/") && !mapFile.startsWith("http")) {
			    mapFile =ramaddaCdn +"/resources/" + mapFile;
			}
		    }
		    $.getJSON(mapFile, (data) =>{
			this.mapJson = data;
			this.regionNames=[];
			this.makeRegions();
			this.updateUI();
		    });
		}
		return;
	    }
	    if(!this.regions) {
		if(!this.makeRegions()) return;
	    }
	    this.makeMap();
	},
	makeRegions:function() {
	    if(!this.mapJson) return false;
	    let debug = this.getProperty("debug");
	    let pruneMissing = this.getPropertyPruneMissing(false);
	    let allRegions = {};
	    if(this.getData()==null) {
		return false;
	    }
	    let allRecords = this.getData().getRecords()
	    if(!allRecords) return;
	    let regionField=this.getFieldById(null,this.getPropertyRegionField());
	    if(regionField==null) {
		this.displayError("No region field");
		return false;
	    }
	    allRecords.forEach(record=>{
		let v = record.getValue(regionField.getIndex());
//		console.log("data region:" + v);
		allRegions[v] = true;
	    });
	    this.regions = {};
	    this.mapRange  = {
		minLon:null,
		maxLon:null,
		minLat:null,
		maxLat:null
	    };
	    let transforms = {}
	    let prunes = {}	    
	    this.getPropertyTransforms("").split(";").map(t=>t.split(",")).forEach(tuple=>{
		let region = tuple[0];
		transforms[region] = {
		    scale:tuple[1]!=null?+tuple[1]:1,
		    dx:tuple[2]!=null?+tuple[2]:0,
		    dy:tuple[3]!=null?+tuple[3]:0}
	    });

	    this.getPropertyPrunes("").split(";").map(t=>t.split(",")).forEach(tuple=>{
		let region = tuple[0];
		prunes[region] =  +tuple[1];
	    });

	    let tfunc=(region,polygon)=>{
		let prune = prunes[region];
		if(prune>0) {
		    if(polygon.length<prune) return null;
		}

		let transform = transforms[region];
		if(!transform) 
		    return polygon;
		let bounds = Utils.getBounds(polygon);
		let centerx = bounds.minx + (bounds.maxx-bounds.minx)/2;
		let centery = bounds.miny + (bounds.maxy-bounds.miny)/2;		
		polygon.map(pair=>{
		    pair[0]= (pair[0]-centerx)*transform.scale+centerx;
		    pair[1]= (pair[1]-centery)*transform.scale+centery;		    		    
		    pair[0] += transform.dx;
		    pair[1] += transform.dy;
		    return pair;
		});
		return polygon;		
	    };
	    
	    this.skipRegions = this.getPropertySkipRegions("").split(",").map(r=>r.replace(/_comma_/g,","));
	    let features = this.mapJson.geojson;
	    if(!features)
		features = this.mapJson.features;

	    this.aliasMap = {};
	    let mapFeature = this.getMapFeature();
	    features.forEach(blob=>{
		let region;
		if(mapFeature) {
		    region = blob.properties[mapFeature];
		} else  {
		    region = blob.properties.name || blob.properties.name_long || blob.properties.NAME || blob.properties.ADMIN;
		}
		let aliases = [region];
		//Some hacks
		if(region=="United States of America") aliases.push("United States");
		if(region=="United Republic of Tanzania") aliases.push("Tanzania");
		if(region=="Democratic Republic of the Congo") aliases.push("Democratic Republic of Congo");
		if(region=="Czech Rep.") aliases.push("Czech Republic");
		if(region=="Bosnia and Herz.") aliases.push("Bosnia and Herzegovina");
		this.aliasMap[region] = aliases;
		if(blob.properties.ISO_A3)
		    aliases.push(blob.properties.ISO_A3);
		if(blob.properties.STUSPS)
		    aliases.push(blob.properties.STUSPS);
		if(blob.properties.STATEFP)
		    aliases.push(blob.properties.STATEFP);		
		if(!blob.geometry) {
		    if(debug)
			console.log(region +" no geometry");
		    return;
		}
		if(debug)
		    console.log("region:" + region);
		let ok = true;
		aliases.forEach(alias=>{
		    if(this.skipRegions.includes(alias)) ok = false;});
		if(!ok) {
		    return;
		}
		ok = false;
		aliases.forEach(alias=>{
		    if(allRegions[alias]) {
			ok =true;
		    }});
		if(!ok) this.handleWarning("Missing data for map region:" + region);
		if(pruneMissing && !ok) {
		    if(debug)console.log("missing data:" + region);
//		    return;
		}
		this.regionNames.push(region);
		let coords = blob.geometry.coordinates;
		let info = {
		    name:region,
		    aliases: aliases,
		    polygons:[],
		    bounds:null
		};
		aliases.forEach(alias=>{
		    this.regions[alias] = info;
		});
		if(blob.geometry.type  == "MultiPolygon") {
		    coords.forEach(group=>{
			group.forEach(polygon=>{
			    polygon  = tfunc(region,polygon);
			    if(polygon)info.polygons.push(polygon);
			});
		    });
		} else {
		    coords.forEach(polygon=>{
			info.polygons.push(tfunc(region,polygon));
		    });
		}
		info.polygons.forEach(polygon=>{
		    polygon.forEach(point=>{
			let lon = point[0];
			let lat = point[1];
			if(isNaN(lon) || isNaN(lat)) return;
			this.mapRange.minLon= this.mapRange.minLon===null?lon:Math.min(this.mapRange.minLon,lon);
			this.mapRange.maxLon= this.mapRange.maxLon===null?lon:Math.max(this.mapRange.maxLon,lon);
			this.mapRange.minLat= this.mapRange.minLat===null?lat:Math.min(this.mapRange.minLat,lat);
			this.mapRange.maxLat= this.mapRange.maxLat===null?lat:Math.max(this.mapRange.maxLat,lat);						
		    });
		});
		let bounds = null;
		info.polygons.forEach(polygon=>{
		    bounds = Utils.mergeBounds(bounds, Utils.getBounds(polygon));
		});
		info.bounds = bounds;
	    });
	    return true;
	},
	
    });
}


function RamaddaMapchartDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaOtherMapDisplay(displayManager, id, DISPLAY_MAPCHART, properties);
    let myProps = [
	{label:'Map chart Properties'},
	{p:'fixedFillColor',tt:'Use this color for all polygons',ex:'red'},
	{p:'fixedStrokeColor',tt:'Use this color for all polygons',ex:'red'},	
	{p:'missingLineColor'},
	{p:'missingFillColor'},		
	{p:'maxLayers',ex:'10'},
	{p:'translateX',ex:'0'},
	{p:'translateY',ex:'0'},	
	{p:'skewX',ex:'-10'},
	{p:'skewY',ex:'0'},	
	{p:'rotate',ex:'10'},
	{p:'scale',ex:'0'},
	{p:'fillColor',ex:'red'},
	{p:'blur',ex:'4'},			
    ];

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        makeMap: function() {
            let records = this.filterData();
            if (!records) {
                return;
            }
	    let valueMap = this.makeValueMap(records,true);
	    if(!valueMap) return;
	    let allRecords = this.getData().getRecords()
	    let pruneMissing = this.getPropertyPruneMissing(false);
	    let maxLayers = +this.getPropertyMaxLayers(20);
	    Object.keys(valueMap).forEach(region=>{
		let values = valueMap[region];
		values.layers = Math.round(values.percent*(maxLayers-1))+1;
	    });
	    this.colorBy = this.getColorByInfo(allRecords);

	    let [width,height] = this.writeMap();
	    let [svg, scaleX, scaleY] = this.makeSvg(width,height);
	    SU.transform(svg,SU.translate(width/2, height/2), SU.scale(0.9), SU.rotate(this.getPropertyRotate(0)), SU.translate(-width/2,-height/2), SU.translate(this.getPropertyTranslateX(30),this.getPropertyTranslateY(0)), SU.skewX(this.getPropertySkewX(-10)), SU.scale(this.getPropertyScale(1)));
	    let defs = svg.append("defs");
	    SU.makeBlur(svg,"blur", this.getPropertyBlur(3));
	    pruneMissing=false;
	    let fixedFillColor = this.getFixedFillColor();
	    let fixedStrokeColor = this.getFixedStrokeColor();	    	    
//	    console.dir(this.colorBy);
	    for(let layer=0;layer<maxLayers;layer++) {
		this.regionNames.forEach(region=>{
		    let values= this.findValues(region, valueMap);
		    let maxLayer = 1;
		    let value = NaN;
		    let missing = values==null;
		    let record = null;
		    if(!missing) {
			maxLayer = values.layers;
			value = values.value;
			record = values.record;
		    } else {
			if(pruneMissing) return;
			maxLayer = 1;
			if(layer>0) return;
		    }
		    let recordId = record?record.getId():"";
		    if(!Utils.isDefined(maxLayer)) maxLayer = 1;
		    if(layer>maxLayer) return;
		    this.regions[region].polygons.forEach(polygon=>{
			let uid = HtmlUtils.getUniqueId('selector_');
			let poly = this.makePoly(polygon);
			let fillColor = "transparent";
			if(missing) {
			    fillColor = this.getMissingFillColor("#ccc");
			    lineColor=this.getMissingLineColor("#000"); 
			} else {
			    if(layer==maxLayer-1) {
				fillColor = this.colorBy.getColorFromRecord(record);
				lineColor  = Utils.pSBC(0.1,fillColor);
			    } else {
				lineColor  = Utils.pSBC(-0.3,this.colorBy.getColor(value));
			    }
			    if(fixedFillColor)
				fillColor=fixedFillColor;
			    if(fixedStrokeColor)
				lineColor=fixedStrokeColor;			    
			}
			if(missing) {
			    svg.selectAll(uid)
				.data([poly])
				.enter().append("polygon")
				.attr("points",function(d) { 
				    return d.map(d=>{return [-layer+scaleX(d.x),-layer+scaleY(d.y)].join(",");}).join(" ");
				})
				.attr("regionName",region)
				.attr("fill",fillColor)
		    		.attr("stroke-width",1)
			    	.attr("stroke",lineColor);
			    return;
			}

			

			if(layer==0) {
			    svg.selectAll(uid)
				.data([poly])
				.enter().append("polygon")
				.attr("points",function(d) { 
				    return d.map(d=>{return [-layer+scaleX(d.x),-layer+scaleY(d.y)].join(",");}).join(" ");
				})
		    		.attr("stroke-width",3)
				.attr("stroke","black")
				.style("filter","url(#blur)");
			}
			let polys = 
			    svg.selectAll(uid)
			    .data([poly])
			    .enter().append("polygon")
			    .attr("points",function(d) { 
				return d.map(d=>{return [-layer+scaleX(d.x),-layer+scaleY(d.y)].join(",");}).join(" ");
			    })
			    .attr("fill",fillColor)
			    .attr("opacity",1)
			    .attr("stroke",lineColor)
			    .attr("stroke-width",1)
			    .style("cursor", "pointer")
			    .attr(RECORD_ID,recordId);
			this.addEvents(polys);
		    });
		});
	    }
	    if(!fixedFillColor)
		this.colorBy.displayColorTable();
	}
    });
}



function RamaddaMaparrayDisplay(displayManager, id, properties) {
    const ID_MAPBLOCK = "mapblock";
    const ID_MAPLABEL = "maplabel";        
    const SUPER = new RamaddaOtherMapDisplay(displayManager, id, DISPLAY_MAPARRAY, properties);
    let myProps = [
	{label:'Map array properties'},
	{p:'blockWidth',ex:''},
	{p:'sortByValue',ex:'true'},
	{p:'fillColor',ex:'red'},
	{p:'showValue',ex:'true'},	
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        makeMap: function() {
            let records = this.filterData();
            if (!records) {
                return;
            }
	    let valueMap = this.makeValueMap(records,true);
	    if(!valueMap) return;
	    let allRecords = this.getData().getRecords()
	    this.colorBy = this.getColorByInfo(allRecords);
	    let [width,height] = this.writeMap(true);
	    let blockWidth= this.getPropertyBlockWidth(75);
	    let blockHeight= blockWidth;
	    let pruneMissing = this.getPropertyPruneMissing(true);
	    let sortedRegions = this.regionNames;
	    if(this.getSortByValue(true)) {
		sortedRegions.sort((a,b)=>{
		    if(!valueMap[a]) return 1;
		    if(!valueMap[b]) return -1;		    
		    return valueMap[a].value-valueMap[b].value;
		});
	    } else {
		sortedRegions.sort();
	    }

	    let html = "";
	    sortedRegions.forEach((region,idx)=>{
		html+= HU.div([CLASS,"display-maparray-block"],
			      HU.div([CLASS,"display-maparray-header"],region) +
			      HU.div([ID,this.domId(ID_MAPBLOCK+"_"+idx),CLASS,"display-maparray-map",STYLE,HU.css(WIDTH,blockWidth+"px",HEIGHT,blockHeight+"px")]) +
			      HU.div([ID,this.domId(ID_MAPLABEL+"_"+idx),"display-maparray-label"]));			      


		    
	    });
	    this.jq(ID_BASEMAP).html(html+"<p>");

	    let showValue = this.getPropertyShowValue(true);

	    sortedRegions.forEach((region,idx)=>{
		let info = this.regions[region];
		let svg = d3.select("#" + this.domId(ID_MAPBLOCK+"_"+idx)).append('svg')
		    .attr('width', blockWidth)
		    .attr('height', blockHeight)
		    .append('g')
		let padx=5;
		let pady=5;
		let mapWidth = info.bounds.getWidth();
		let mapHeight = info.bounds.getHeight();
		let scaleX;
		let scaleY;
		if(mapWidth>mapHeight) {
		    scaleX= d3.scaleLinear().domain([info.bounds.minx, info.bounds.maxx]).range([0, blockWidth-padx]);
		    scaleY= d3.scaleLinear().domain([info.bounds.maxy, info.bounds.miny]).range([0, (mapHeight/mapWidth)*blockHeight-pady]);
		} else {
		    scaleX= d3.scaleLinear().domain([info.bounds.minx, info.bounds.maxx]).range([0, (mapWidth/mapHeight)*blockWidth-padx]);
		    scaleY= d3.scaleLinear().domain([info.bounds.maxy, info.bounds.miny]).range([0, blockHeight-pady]);
		}
		let values = valueMap[region];
		let value = NaN;
		let missing = values==null;
		let record = null;
		if(!missing) {
		    value = values.value;
		    record = values.record;
		    if(showValue) {
			this.jq(ID_MAPLABEL+"_"+idx).html(value);
		    }
		} else {
		    if(pruneMissing) return;
		}

		let recordId = record?record.getId():"";
		info.polygons.forEach(polygon=>{
		    let uid = HtmlUtils.getUniqueId();
		    let poly = this.makePoly(polygon);
		    let fillColor = "transparent";
		    if(missing) {
			fillColor = "#ccc";
			lineColor="#000" 
		    } else {
			fillColor = this.colorBy.getColor(value);
			lineColor = "#ccc";
		    }
		    if(missing) {
			svg.selectAll(uid)
			    .data([poly])
			    .enter().append("polygon")
			    .attr("points",function(d) { 
				return d.map(d=>{return [-layer+scaleX(d.x),-layer+scaleY(d.y)].join(",");}).join(" ");
			    })
			    .attr("fill","#ccc")
		    	    .attr("stroke-width",1)
			    .attr("stroke","black");
			return;
		    }
		    let polys = 
			svg.selectAll(uid)
			.data([poly])
			.enter().append("polygon")
			.attr("points",function(d) { 
			    return d.map(d=>{return [+scaleX(d.x),+scaleY(d.y)].join(",");}).join(" ");
			})
			.attr("fill",fillColor)
			.attr("opacity",1)
			.attr("stroke",lineColor)
			.attr("stroke-width",1)
			.style("cursor", "pointer")
			.attr(RECORD_ID,recordId);
		    this.addEvents(polys);
		});
	    });
	    this.colorBy.displayColorTable();
	}
    });
}




function RamaddaMapshrinkDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaOtherMapDisplay(displayManager, id, DISPLAY_MAPSHRINK, properties);
    let myProps = [
	{label:'Map shrink Properties'},
   ];

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        makeMap: function() {
            let records = this.filterData();
            if (!records) {
                return;
            }
	    let allRecords = this.getData().getRecords()
	    let pruneMissing = this.getPropertyPruneMissing(false);
	    let valueMap = this.makeValueMap(records,true);
	    if(!valueMap) return;
	    let sizeBy = new SizeBy(this, allRecords);
	    this.colorBy = this.getColorByInfo(allRecords);
	    let [width,height] = this.writeMap();
	    let [svg, scaleX, scaleY] = this.makeSvg(width,height);

	    for(let layer=0;layer<2;layer++) {
		this.regionNames.forEach(region=>{
		    let values= this.findValues(region, valueMap);
		    let value = NaN;
		    let missing = values==null;
		    let record = null;
		    if(!missing) {
			value = values.value;
			record = values.record;
		    } else {
			if(pruneMissing) return;
			if(layer>0) return;
		    }
		    let recordId = record?record.getId():"";
		    this.regions[region].polygons.forEach(polygon=>{
			let uid = HtmlUtils.getUniqueId();
			let poly = this.makePoly(polygon);
			let fillColor = "red";
			let transform  = "";
			lineColor="#000" 
			if(layer==0) {
			    fillColor = "#fff";
			} else {
			    lineColor="transparent" 
			    fillColor = this.colorBy.getColor(value);
			    let bounds = Utils.getBounds(polygon);
			    let center = bounds.getCenter();
			    let p=0;
			    let sizeByFunc = function(p, size) {
				percent = p;
				return percent;
			    }
			    sizeBy.getSizeFromValue(value,sizeByFunc);
			    transform = SU.translate(scaleX(center.x),scaleY(center.y)) + SU.scale(percent) + SU.translate(-scaleX(center.x),-scaleY(center.y))
			}
			if(missing) {
			    svg.selectAll("base"+uid)
				.data([poly])
				.enter().append("polygon")
				.attr("points",function(d) { 
				    return  d.map(d=>{return [-layer+scaleX(d.x),-layer+scaleY(d.y)].join(",");}).join(" ");
				})
				.attr("fill","#ccc")
		    		.attr("stroke-width",1)
			    	.attr("stroke","black");
			    return;
			}
			if(layer==0) {
			    svg.selectAll("base"+uid)
				.data([poly])
				.enter().append("polygon")
				.attr("points",function(d) { 
				    return  d.map(d=>{return [-layer+scaleX(d.x),-layer+scaleY(d.y)].join(","); }).join(" ");
				})
		    		.attr("stroke-width",1)
				.attr("stroke","black")
				.attr('transform',transform);
			}
			let polys = 
			    svg.selectAll(uid)
			    .data([poly])
			    .enter().append("polygon")
			    .attr("points",function(d) { 
				return  d.map(d=>{return [-layer+scaleX(d.x),-layer+scaleY(d.y)].join(",");}).join(" ");
			    })
			    .attr("fill",fillColor)
			    .attr("opacity",1)
			    .attr("stroke",lineColor)
			    .attr("stroke-width",1)
			    .attr('transform',transform)
			    .style("cursor", "pointer")
			    .attr(RECORD_ID,recordId);
			if(layer==1)
			    this.addEvents(polys);
		    });
		});
	    }
	    this.colorBy.displayColorTable();
	}
    });
}


function RamaddaMapimagesDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaOtherMapDisplay(displayManager, id, DISPLAY_MAPIMAGES, properties);
    let myProps = [
	{label:'Map Images Properties'},
	{p:'imageField',ex:''},
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        getHeightForStyle: function(dflt) {
	    return null;
	},
	addMacroAttributes:function(macros,row,attrs) {
	    SUPER.addMacroAttributes.call(this,macros,row,attrs);
	    if(!this.imageField) return;
	    let f = this.imageField;
	    let value = row[f.getIndex()];
	    let imageAttrs = [];
	    let tokenAttrs  = macros.getAttributes("imageField_image");
	    let width = tokenAttrs?tokenAttrs["width"]:null;
	    if(width) {
		imageAttrs.push("width");
		imageAttrs.push(width);
	    } else if(this.getProperty("imageWidth")) {
		imageAttrs.push("width");
		imageAttrs.push(this.getProperty("imageWidth")); 
	    } else  {
		imageAttrs.push("width");
		imageAttrs.push("100%");
	    }
	    imageAttrs.push("style");
	    imageAttrs.push("vertical-align:top");
	    let img =  HU.image(value, imageAttrs);
	    attrs["imageField" +"_image"] =  img;
	    attrs["imageField" +"_url"] =  value;
	},
        makeMap: function() {
            let records = this.filterData();
            if (!records) {
                return;
            }
	    this.imageField = this.getFieldById(null,this.getPropertyImageField());	    
	    if(this.imageField == null) {
		this.imageField =  this.getFieldByType(null, "image");
	    }
	    let strokeWidth = this.getStrokeWidth(1);
	    let strokeColor = this.getStrokeColor("#000");
	    if(this.imageField==null) {
                this.displayError("No image fields");
		return
	    }
	    let valueMap = this.makeValueMap(records);
	    if(!valueMap) return;
	    Object.keys(valueMap).forEach(region=>{
		let values = valueMap[region];
		values.image = values.record.getValue(this.imageField.getIndex());
	    });
	    let [width, height] = this.writeMap();
	    let [svg, scaleX, scaleY] = this.makeSvg(width,height);
	    let defs = svg.append("defs");
	    this.regionNames.forEach((region,idx)=>{
		let values= this.findValues(region, valueMap);
		let recordId = values!=null?values.record.getId():"";
		let regionClean = Utils.cleanId(region);
		this.regions[region].polygons.forEach(polygon=>{
		    let uid = HtmlUtils.getUniqueId();
		    if(values!=null) {
			defs.append("svg:pattern")
			    .attr("id", "bgimage"+ uid)
			    .attr("x", "1")
			    .attr("y", "1")
			    .attr("width", "100%")
		            .attr("height", "100%")
			    .attr("patternContentUnits","objectBoundingBox")
			    .append("svg:image")
			    .attr("xlink:href", values.image)
			    .attr("preserveAspectRatio","none")
			    .attr("width", 1)
			    .attr("height", 1)
			    .attr("x", "0")
			    .attr("y", "0");
		    }
		    let polys = svg.selectAll(regionClean+"base"+uid)
			.data([this.makePoly(polygon)])
			.enter().append("polygon")
			.attr("regionName",region)
			.attr("points",function(d) { 
			    return d.map(d=>{return [scaleX(d.x),scaleY(d.y)].join(",");}).join(" ");
			})
			.attr(RECORD_ID,recordId)
		    	.attr("stroke-width",strokeWidth)
			.attr("stroke",strokeColor);
		    if(values!=null)
			polys.style("fill", "url(#bgimage"+ uid+")")
		    else
			polys.style("fill",this.getPropertyMissingFill("#fff"));
		    this.addEvents(polys);
		});
	    });
	}
    });
}



		
function CollisionInfo(display,numRecords, roundPoint) {
    $.extend(this,{
	dot: null,
	display:display,
	roundPoint:roundPoint,
	visible: display.getCollisionFixed(),
	dot:null,
	numRecords:numRecords,
	records:[],
	addLines:false,
	lines:[],
	points:[],
	addRecord: function(record) {
	    this.records.push(record);
	},
	addLine:function(line) {
	    this.lines.push(line);
	},
	checkLines: function() {
	    if(!this.addedLines) {
		this.addedLines = true;
		this.display.addFeatures(this.lines,true);
		this.display.addFeatures(this.points,false);
	    }
	},
	createDot: function(idx) {
	    let tooltip = this.display.getCollisionTooltip();
	    let textGetter = tooltip==null?null:dot=>{
		let html = "";
		this.records.forEach(record=>{
		    html+=HU.div([STYLE,HU.css("border-bottom","1px solid #ccc")], this.display.getRecordHtml(record, null,tooltip));
		});
		return html;
	    };
	    let collisionIcon=this.display.getCollisionIcon();
	    let collisionIconSize=this.display.getCollisionIconSize();		
	    if(collisionIcon)
		this.dot = this.display.map.createMarker("dot-" + idx, [this.roundPoint.x,this.roundPoint.y], collisionIcon, "", "",null,collisionIconSize,null,null,null,null,false);
	    else {
		let style = this.getCollisionDotStyle(this);
		this.dot = this.display.map.createPoint("dot-" + idx, this.roundPoint, style,null,textGetter);
	    }
	    this.dot.collisionInfo  = this;
	    return this.dot;
	},
	dotSelected:function(dot) {
	    if(this.display.getCollisionFixed()) return;
	    this.setVisible(!this.visible);
	},
	styleCollisionDot:function(dot) {
	    $.extend(dot.style, this.getCollisionDotStyle(dot.collisionInfo));
	},
	addPoints:function(points) {
	    points.forEach(p=>this.points.push(p));
	},
	getCollisionDotStyle:function(collisionInfo) {
	    let collisionFixed = this.display.getCollisionFixed();
	    let dotColor = this.display.getCollisionDotColor();
	    let dotRadius = this.display.getCollisionDotRadius();
	    if(!collisionFixed) {
		if(this.display.getPropertyCollisionScaleDots(false)) {
		    let scale = collisionInfo.numRecords/16;
		    if(scale>1)
			dotRadius = Math.min(dotRadius*scale,32);
//		    console.log("scaling dots " + dotRadius +" " +collisionInfo.numRecords);
		} 

		if(collisionInfo.visible)  {
		    dotColor = this.display.getCollisionDotColorOn()??dotColor;
		} else {
		    dotColor = this.display.getCollisionDotColorOff()??dotColor;
		}
	    }
	    return {
		fillColor:dotColor,
		pointRadius:dotRadius
	    }
	},


	setVisible:function(visible) {
	    this.visible = visible;
	    this.styleCollisionDot(this.dot);
	    this.dot.layer.drawFeature(this.dot, this.dot.style);
	    this.checkLines();
	    //These are the spokes
	    this.lines.forEach(f=>{
		f.featureVisible = this.visible;
		this.display.map.checkFeatureVisible(f,true);
	    });

	    this.records.forEach(record=>{
		let layoutThis = this.display.displayInfo[record.getId()];
		if(!layoutThis) {
		    return;
		}
		layoutThis.features.forEach(f=>{
		    f.featureVisible = this.visible;
		    this.display.map.checkFeatureVisible(f,true);
		});
	    });
	},
    });
    if(this.visible) {
	this.checkLines();
    }
}





