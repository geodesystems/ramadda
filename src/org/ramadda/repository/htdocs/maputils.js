/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/



var debugBounds = false;
var getMapDebug = false;
var debugPopup = false;
var debugSelect= false;

var RamaddaToFloat = v=>{
    if(v!=null) v=parseFloat(v);
    return v;
};



var MapUtils =  {
    me:'MapUtils',
    regions:null,
    POSITIONMARKERID: 'location',
    LABEL_MAP:{
	'aiannhr':'Federal Recognition',
	'mtfcc':'Feature Class',
	'geoid':  'Geographic ID',
	'aland':  'Land Area',
	'awater':  'Water Area',	    
	'countyfp':  'County FIPS',
	'statefp':  'State FIPS',
	'namelsad':  'Name-LSAD',
	'lsad':'Legal Area Descriptor',
	'aiannhce':'American Indian Census Code',
	'aiannhns':'Anerican Indian Standard Code',
	'classfp':'Class Code',
	'comptyp':'Component Type',
	'intptlat':'Latitude',
	'intptlon':'Longitude',
	'usps':'USPS State Abbrev.',
	'ansicode':'ANSI Code',
	'funcstat':'Functional Status',
	'drawseq':'Draw Sequence',
	'cdsessn':'CD Session',
	'cd115fp':'CD FIPS',
	'affgeoid':'Amer. Fact Finder ID',

    },
    properties:{},
    testSize:{
	active:false,
	out:null,
	fontUnit:'px',
	fontSize:3,
	line:"ABCDEFGHIJKLMNOPQRSTUVWXYZABCD\nABCDEFGHIJKLMNOPQRSTUVWXYZABCD",
	rows:2,
	seen:{}
    },
    MAP_RESOURCES:null,
    MAP_RESOURCES_MAP:null,
    initMapResources:function() {
	if(!this.MAP_RESOURCES) {
            $.getJSON(Ramadda.getUrl('/mapresources.json'), data=>{
		this.MAP_RESOURCES_MAP={};
		this.MAP_RESOURCES = data;
		this.MAP_RESOURCES.forEach((r,idx)=>{this.MAP_RESOURCES_MAP[idx] = r;});
	    }).fail(err=>{
		console.error('Failed loading mapresources.json:' + err);
	    });
	}
    },



    handleSize:function(bbox) {
	if(!this.testSize.active) return;
	if(!this.testSize.out) {
	    this.testSize.out='let ' +this.testSize.fontUnit+'Map={';
	}
	if(this.testSize.seen[this.testSize.fontSize]) return;
	this.testSize.seen[this.testSize.fontSize]=true;
	let line = this.testSize.fontSize+':{x:'+Utils.trimDecimals(bbox.width/this.testSize.line.length,2)+',y:'+Utils.trimDecimals(bbox.height/this.testSize.rows,2)+'},';
	console.log(line);
	this.testSize.out+=line+'\n';
	if(this.testSize.fontSize==24)
	    Utils.makeDownloadFile(this.testSize.fontUnit+'map.txt',this.testSize.out+'\n};\n');
    },


    loadTurf: function(callback) {
	if(!window.turf) {
	    let url = ramaddaCdn+"/lib/turf.min.js";
	    Utils.loadScript(url,callback);
	    return false;
	}
	return true;
    },

    extendBounds:function(b1,b2) {
	if(b1) {
	    if(b2) b1.extend(b2);
	    return b1;
	}
	return b2;
    },
    intercept:function(lat1,lon1,lat2,lon2) {
	// define the two points
	let point1 = { lat: lat1, lng: lon1 }; 
	let point2 = { lat: lat2, lng: lon2}; 

	// calculate the difference in longitude
	let lngDiff = Math.abs(point1.lng - point2.lng);

	// if the difference is greater than 180 degrees, adjust it
	if (lngDiff > 180) {
	    lngDiff = 360 - lngDiff;
	}
	
	// calculate the slope of the line connecting the two points
	let latDiff = point2.lat - point1.lat;
	let slope = latDiff / lngDiff;
	
	// calculate the latitude of the intercept point on the date line
	return point1.lat + ((180 - point1.lng) * slope);
    },

    crossIDL:function(lon1,  lon2) {
	// Determine if the two points are on opposite sides of the IDL
	if ((lon1 > 0 && lon2 < 0) || (lon1 < 0 && lon2 > 0)) {
	    // Calculate the longitude difference
	    let lonDiff = Math.abs(lon1 - lon2);
	    // If the longitude difference is greater than 180 degrees, the line segment crosses the IDL
	    if (lonDiff > 180) {
		return true;
	    }
	}
	return false;
    },

    addMapProperty:function(key,prop) {
	this.properties[key] = prop;
    },
    getMapProperty:function(key,dflt) {
	return this.properties[key]??dflt;
    },
    makeLabel:function(l) {
	if(!l) return l;
	let _l=l.toLowerCase();
	return  this.LABEL_MAP[_l] ?? Utils.makeLabel(l);
    },
    stopEvent:function(e) {
	OpenLayers.Event.stop(e);
    },
    
    createGraticule:function(attrs) {
	return  new OpenLayers.Control.Graticule(attrs);
    },
    createLinearRing:function(pts) {
	return new OpenLayers.Geometry.LinearRing(pts);
    },
    createLineString:function(pts) {
	return  new OpenLayers.Geometry.LineString(pts);
    },
    createPolygon:function(pts){
	return new OpenLayers.Geometry.Polygon(pts);
    },
    setFeatureStyle:function(feature,style,orig) { 
	if(!feature) return;
	feature.style = style;
	if(typeof orig !='undefined')
	    feature.originalStyle=orig;
	return style;
    },

    createVector:function(geom,attrs,style) {
        return  new OpenLayers.Feature.Vector(geom,attrs,style);
    },
    formatLocationValue:function(value) {
        value = parseFloat(value);
        let scale = Math.pow(10,7);
        value = (Math.floor(value * scale) /scale);
	return value;
    },
    getVectorStyle:function(style) {
	return OpenLayers.Feature.Vector.style[style];
    },
    createStyleMap:function(attrs) {
	return new OpenLayers.StyleMap(attrs);
    },
    createLonLat: function(lon, lat) {
	return new OpenLayers.LonLat(RamaddaToFloat(lon), RamaddaToFloat(lat));
    },
    createPoint: function(lon, lat) {
	return new OpenLayers.Geometry.Point(RamaddaToFloat(lon), RamaddaToFloat(lat));
    },    
    getCenter: function(bounds) {
	return MapUtils.createPoint(bounds.left+(bounds.right-bounds.left)/2,
				    bounds.bottom+(bounds.top-bounds.bottom)/2);

    },
    boundsDefined:function(bounds) {
	return bounds!=null && bounds.left!=null;
    },
    createBoundsFromPoints:function (points) {
	let left=0,right=0,bottom=0,top=0;
	points.forEach((p,idx)=>{
	    left = idx==0?p.x:Math.min(left,p.x);
	    right = idx==0?p.x:Math.max(right,p.x);
	    top = idx==0?p.y:Math.max(top,p.y);
	    bottom = idx==0?p.y:Math.min(bottom,p.y);	    	    	    
	})
	return this.createBounds(left,bottom,right,top);
    },
    //w,s,n,e
    createBounds:function (v1, v2, v3, v4,recurse) {
	//Check for being passed in a bounds object
	if(!Utils.isNumber(v1) && v1 && v1.left&& !recurse) {
	    return this.createBounds(v1.left,v1.bottom,v1.right,v1.top,true);
	}
	let bounds =  new OpenLayers.Bounds(RamaddaToFloat(v1), RamaddaToFloat(v2), RamaddaToFloat(v3), RamaddaToFloat(v4));
	
	return bounds;
    },
    createPixel:function(x,y) {
	return new OpenLayers.Pixel(x,y);
    },
    createSize:function(w,h) {
	return new OpenLayers.Size(w,h);
    },
    createMarker:function(point,icon) {
	return new OpenLayers.Marker(point, icon);
    },
    createBox:function(bounds) {
	return new OpenLayers.Marker.Box(bounds);
    },
    createIcon:function(url, size, offset, calculateOffset) {
	return new OpenLayers.Icon(url,size,offset, calculateOffset);
    },
    createImage:function(name,pos,size,url) {
        return  OpenLayers.Util.createImage(name,pos,size,url);
    },
    createLayerGeoJson:function(map,name,url) {
	return  MapUtils.createLayerVector(name, {
	    projection: map.displayProjection,
	    strategies: [new OpenLayers.Strategy.Fixed()],
	    protocol: new OpenLayers.Protocol.HTTP({
                url: url,
                format: new OpenLayers.Format.GeoJSON({ignoreExtraDims:true})
	    }),
        });
    },	
    createLayerKML:function(map,name,url) {
	return MapUtils.createLayerVector(name, {
            projection: map.displayProjection,
            strategies: [new OpenLayers.Strategy.Fixed()],
            protocol: new OpenLayers.Protocol.HTTP({
                url: url,
                format: new OpenLayers.Format.KML({
                    extractStyles: true,
                    extractAttributes: true,
                    maxDepth: 2
                })
            }),
        });
    },
    createLayerGPX:function(map,name,url) {
	return  MapUtils.createLayerVector(name, {
            strategies: [new OpenLayers.Strategy.Fixed()],
            projection: map.displayProjection,
            protocol: new OpenLayers.Protocol.HTTP({
                url:url,
                format: new OpenLayers.Format.GPX({
                    extractStyles: true,
                    extractAttributes: true,
                    maxDepth: 2
                })
	    })});
    },

    createLayerImage:function(name, url,
			      imageBounds,
			      size,attrs) {
        return  new OpenLayers.Layer.Image(
            name, url,
            imageBounds,
	    size,attrs);
    },
    createLayerWMS:function(name,url,attrs) {
        return new OpenLayers.Layer.WMS(name, url, attrs);
    },
    createLayerXYZ:function(name,url,attrs) {
        return new OpenLayers.Layer.XYZ(name, url, attrs);
    },    
    createLayerMarkers:function(name,attrs) {
	return new OpenLayers.Layer.Markers(name,attrs);
    },
    createLayerBoxes:function(name,attrs) {
	return new OpenLayers.Layer.Boxes(name,attrs);
    },    
    createLayerVector:function(name,attrs,style) {
	return new OpenLayers.Layer.Vector(name,attrs,style);
    },    
    createProjection: function(name) {
	return new OpenLayers.Projection(name);
    },
    distance: function(lat1,lon1,lat2,lon2) {
	//From: https://www.movable-type.co.uk/scripts/latlong.html
	const R = 6371e3; // metres
	const o1 = lat1 * Math.PI/180; // o, l in radians
	const o2 = lat2 * Math.PI/180;
	const deo = (lat2-lat1) * Math.PI/180;
	const dl = (lon2-lon1) * Math.PI/180;
	const a = Math.sin(deo/2) * Math.sin(deo/2) +
              Math.cos(o1) * Math.cos(o2) *
              Math.sin(dl/2) * Math.sin(dl/2);
	const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	const d = R * c; // in metres
	return MapUtils.metersToFeet(d);
    },
    metersToFeet:function(m) {
	return 3.28084*m;
    },
    squareMetersToSquareFeet:function(m) {
	return 10.7639*m;
    } ,
    calculateArea: function(pts) {
	let area = 0;
	let ConvertToRadian = (input)=>{
	    return input * Math.PI / 180;
	};
	if (pts.length > 2)   {
	    for (let i = 0; i < pts.length; i++)    {
		let p1 = pts[i];
		let p2 = i<pts.length-1?pts[i + 1]:pts[0];
		area += ConvertToRadian(p2.x - p1.x) * (2 + Math.sin(ConvertToRadian(p1.y)) + Math.sin(ConvertToRadian(p2.y)));
	    }
	    area = area * 6378137 * 6378137 / 2;
	    if(area<0) area = -area;
	    area = MapUtils.squareMetersToSquareFeet(area);
	    return area;
	}
	return -1;
    },
    squareFeetInASquareMile: 27878400,
    symbols:{
	lightning : [0, 0, 4, 2, 6, 0, 10, 5, 6, 3, 4, 5, 0, 0],
	rectangle : [0, 0, 4, 0, 4, 10, 0, 10, 0, 0],
	diamond : [5, 0, 10, 5, 5, 10, 0, 5, 5, 0],	
	church: [4, 0, 6, 0, 6, 4, 10, 4, 10, 6, 6, 6, 6, 14, 4, 14, 4, 6, 0, 6, 0, 4, 4, 4, 4, 0],
	_x: [0, 0, 6,6,3,3,6,0,0,6,3,3],
	thinx: [0, 0, 6,6,3,3,6,0,0,6,3,3],	
//	arrow: [0,0,5,10,10,0],
	arrow: [0,10, 5,7,10,10, 5,0, 0,10],
	downtriangle: [0,0,5,10,10,0],	
	plane: [5,0,5,0,4,1,4,3,0,5,0,6,4,5,4,8,2,10,3,10,5,9,5,9,8,10,8,10,6,8,6,5,10,6,10,5,6,3,6,1,5,0,5,0],
    },
    /*
      Define new symbols
      To make a new shape see errata.js 
    */
    initSymbols:function() {
	for(a in this.symbols)  {
	    OpenLayers.Renderer.symbol[a] = this.symbols[a];
	}
    },
    isFeatureVisible:function(feature) {
	if(!feature.style) return true;
	return feature.style.display!='none';
    },
    setFeatureVisible:function(feature, vis) {
	if(!feature.style) {
	    //No need to add a style if it is visible
	    if(vis) return;
	    feature.style = feature.layer?$.extend({},feature.layer.style):{};
	}
	if(vis) {
	    feature.style.display = 'inline';
	}  else {
	    feature.style.display = 'none';
	}
	if(feature.originalStyle) feature.originalStyle.display = feature.style.display;
//	$.extend(feature.style,{display:feature.style.display});
    },

    declutter:function(map,features,args) {
	let opts = {
	    fontSize:'12px',
	    padding:1,
	    granularity:1
	}
	let orig = features;
	if(args) $.extend(opts,args);
	opts.padding = Math.min(10,opts.padding);
	let ptMap={4:{x:1.75,y:5.99},
		   5:{x:2.19,y:7.5},
		   6:{x:2.63,y:8.99},
		   7:{x:3.07,y:10.16},
		   8:{x:3.51,y:11.83},
		   9:{x:3.94,y:13.25},
		   10:{x:4.38,y:14.66},
		   11:{x:4.82,y:16.08},
		   12:{x:5.26,y:17.5},
		   13:{x:5.69,y:19.16},
		   14:{x:6.13,y:20.33},
		   15:{x:6.56,y:22},
		   16:{x:7,y:23.41},
		   17:{x:7.44,y:24.83},
		   18:{x:7.88,y:26.25},
		   19:{x:8.31,y:27.66},
		   20:{x:8.75,y:29.08},
		   21:{x:9.19,y:30.5},
		   22:{x:9.62,y:31.91},
		   23:{x:10.06,y:33.58},
		   24:{x:10.5,y:34.75}};

	let pxMap={4:{x:1.31,y:4.5},
		   5:{x:1.64,y:5.62},
		   6:{x:1.97,y:6.75},
		   7:{x:2.3,y:7.87},
		   8:{x:2.63,y:9},
		   9:{x:2.96,y:10},
		   10:{x:3.29,y:11.25},
		   11:{x:3.62,y:12},
		   12:{x:3.94,y:13.25},
		   13:{x:4.27,y:14.25},
		   14:{x:4.6,y:15.24},
		   15:{x:4.93,y:16.5},
		   16:{x:5.26,y:17.5},
		   17:{x:5.58,y:18.75},
		   18:{x:5.91,y:19.75},
		   19:{x:6.24,y:21},
		   20:{x:6.56,y:22},
		   21:{x:6.89,y:22.75},
		   22:{x:7.22,y:24},
		   23:{x:7.55,y:25},
		   24:{x:7.88,y:26.25}};

	let fontSize = opts.fontSize??'12px';
	let match =  fontSize.match(/^([0-9]+)($|[^\d]+)/);
	let fontSizeNum = 12;
	let fontSizeUnit='px';
	if(match) {
	    fontSizeNum=parseInt(match[1]);
	    fontSizeUnit = match[2];
	}
	let sizeMap = pxMap;
	if(fontSizeUnit=='pt') {
	    sizeMap = ptMap;
	}
	let getDim=()=>{
	    if(sizeMap[fontSizeNum]) return sizeMap[fontSizeNum];
	    if(fontSizeNum<4) return sizeMap[4];
	    return sizeMap[25];
	}
	if(!opts.pixelsPerCharacter) {
	    opts.pixelsPerCharacter =getDim().x*opts.padding;
	}
	if(!opts.pixelsPerLine) {
	    opts.pixelsPerLine =getDim().y*opts.padding;
	}	
	//	console.log(opts);

	if(this.testSize.active) {
	    let fs = ++this.testSize.fontSize;
	    features.forEach(feature=>{
		feature.style.fontSize=fs+this.testSize.fontUnit;
		feature.style.label=this.testSize.line;
	    });
	}


	let mapBounds = map.transformLLBounds(map.getBounds());
	let dim;
//	console.log('map',mapBounds,'#features',features.length);
	let goodFeatures=[];
	features.forEach((feature,idx)=>{
	    feature.gridSpacingInfo=null;
	    if(!feature.geometry) return;
	    if(!feature.style) return;
	    let bounds = feature.geometry.getBounds();
	    if(!mapBounds.contains(bounds) && !mapBounds.intersectsBounds(bounds))  {
//		console.log('not in bounds',bounds);
		MapUtils.setFeatureVisible(feature,false);
		return;
	    }
	    MapUtils.setFeatureVisible(feature,true);
	    goodFeatures.push(feature);
	    let center = bounds.getCenterLonLat();
	    let screen = map.getMap().getViewPortPxFromLonLat(center);
	    let cx = screen.x;
	    let cy = screen.y;
	    let left = cx;
	    let right = cx;
	    let top=cy;
	    let bottom = cy;
	    let height=1;
	    let width=1;
	    if(!feature.style.label) {
		if(feature.style.pointRadius) {
		    let r = (+feature.style.pointRadius);
		    width=r*2;
		    height=r*2;
		    if(left==right) {
			left = cx-r;
			right = cx+r;
		    }
		    if(top==bottom) {
			top = cy-r;
			bottom = cy+r;
		    }		    
		} else {
		    height=opts.padding*(top-bottom);
		    width=opts.padding*(right-left);
		}
	    } else {
		let lines = feature.style.label.split('\n');
		let maxWidth=0;
		lines.forEach(line=>{
		    maxWidth=Math.max(maxWidth,line.length);
		});
		let charWidth = width = opts.pixelsPerCharacter*maxWidth;
		let charHeight= height = opts.pixelsPerLine*lines.length;
		left = screen.x-charWidth/2;
		right = screen.x+charWidth/2;		
		top=screen.y;
		bottom = screen.y+charHeight;
	    }

	    if(!dim) {
		dim={
		    minx:left,
		    maxx:right,
		    miny:top,
		    maxy:bottom						
		}
	    } else {
		dim.minx = Math.min(dim.minx,left);
		dim.maxx = Math.max(dim.maxx,right);
		dim.miny = Math.min(dim.miny,top);
		dim.maxy = Math.max(dim.maxy,bottom);		    		    
	    }

	    feature.gridSpacingInfo={
		height:Math.ceil(height),
		width:Math.ceil(width),
		left:Math.floor(left),
		right:Math.ceil(right),
		top:Math.floor(top),
		bottom:Math.ceil(bottom)
	    }
	});
	if(!dim) return;
	dim= {
	    minx:Math.floor(dim.minx),
	    maxx:Math.ceil(dim.maxx),
	    miny:Math.floor(dim.miny),
	    maxy:Math.ceil(dim.maxy),
	};
	features = goodFeatures;
	let offsetX = -dim.minx;
	let offsetY = -dim.miny;	
	let gridW = parseInt(dim.maxx+offsetX);
	let gridH = parseInt(dim.maxy+offsetY);	
	let grid={};
	let hideCnt =0, showCnt=0;
	features.forEach((feature,idx)=>{
	    let debug = false;
	    if(!this.isFeatureVisible(feature)) {
		console.log('not viz');
		return
	    }
	    let info = feature.gridSpacingInfo;
	    let indexX = parseInt(offsetX+info.left);
	    let indexY = parseInt(offsetY+info.top);	    
	    let clear = true;
	    if(indexX+feature.gridSpacingInfo.width<0
	       || indexY+feature.gridSpacingInfo.height<0) {
		//This should never happen
		clear=false;
		console.log('skipping:',indexX,indexY);
	    }  else {
		let coords =[];
		indexX = Math.max(0,indexX);
		indexY = Math.max(0,indexY);		
		for(let x=indexX;clear && x<indexX+feature.gridSpacingInfo.width;x+=opts.granularity) {
		    if(x>=gridW) break;
		    for(let y=indexY;clear && y<indexY+feature.gridSpacingInfo.height;y+=opts.granularity) {
			if(y>=gridH) break;
			let key  = x+'_'+y;
			if(grid[key]) {
			    clear = false;
			} else {
			    //Dont' set them yet because this feature might not be clear from other coords
			    coords.push([x,y]);
			}
		    }
		}
		//If clear then set the flags
		if(clear) {
		    coords.forEach(coord=>{
			let key  = coord[0]+'_'+coord[1];
			grid[key] = true;		    
		    });
		}
	    }
	    if(this.testSize.active) clear =debug;
	    if(!clear) {
		hideCnt++;
		MapUtils.setFeatureVisible(feature,false);
	    } else {
		showCnt++;
		MapUtils.setFeatureVisible(feature,true);		    
	    }
	});
//	console.log("#orig:" + orig.length,"#features:",features.length,"hide:" + hideCnt +" show:" + showCnt);
    },
    makeDefaultFeatureText:function (attrs,columns,valueFormatter,labelGetter) {
	if(!columns) columns  = Object.keys(attrs);
        let html = '<table>';
	let first = [];
	let middle = [];
	let last = [];
	columns.forEach(attr=>{
	    let _attr = attr.toLowerCase();
	    if(_attr=='latitude' || _attr=='lat'|| _attr=='longitude' || _attr=='lon' || _attr=='long')
		last.push(attr);
	    else if(_attr=='name') first.push(attr);
	    else middle.push(attr);
	});

	columns = Utils.mergeLists(first,middle,last);
	columns.forEach(attr=>{
            let lclabel = attr.toLowerCase();
            if (lclabel == 'objectid' ||
                lclabel == 'feature_type' ||
                lclabel == 'shapearea' ||
                lclabel == 'styleurl' ||
                lclabel == 'shapelen') return;

            let label;
	    if(labelGetter)
		label = labelGetter(attr);
	    if(!Utils.isDefined(label))
		label = MapUtils.makeLabel(attr);
            if (lclabel == 'startdate') label = 'Start Date';
            else if (lclabel == 'enddate') label = 'End Date';
            else if (lclabel == 'aland') label = 'Land Area';
            else if (lclabel == 'awater') label = 'Water Area';
            html += '<tr valign=top><td align=right><div style=\'margin-right:5px;margin-bottom:3px;\'><b>' + HU.span(['title',attr],label) + ':</b></div></td><td><div style=\'margin-right:5px;margin-bottom:3px;\'>';
            let value;
            if (attrs[attr] != null && (typeof attrs[attr] == 'object' || typeof attrs[attr] == 'Object')) {
                let o = attrs[attr];
                value = '' + o['value'];
            } else {
                value = '' + attrs[attr];
            }
            if (value.startsWith('http:') || value.startsWith('https:')) {
                value = '<a target=_link href=\'' + value + '\'>' + value + '</a>';
            }
            if (value == 'null') return;
	    if(valueFormatter) value = valueFormatter(attr,value);
	    html += value;
            html += '</div></td></tr>';
        });
        html += '</table>';
	return html;
    }	
    
    
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




var RAMADDA_MAP_LAYERS= [];
var RAMADDA_MAP_LAYERS_MAP= {};
function MapLayer(id,name,url,opts) {
    if(url.replace) {
	url  =url.replace(/([^\$]){/g,'$1${');
    }
    this.opts = opts??{};
    if(!Utils.stringDefined(id)) id  =Utils.makeID(name);
    this.id =id;
    if(!Utils.stringDefined(name)) name = Utils.makeLabel(id);
    this.name = name;
    this.url = url;
    RAMADDA_MAP_LAYERS.push(this);
    RAMADDA_MAP_LAYERS_MAP[id] = this;    
}


MapLayer.prototype = {
    isForMap:function() {
	if(Utils.isDefined(this.opts.isForMap))  return this.opts.isForMap;
	return true;
    },
    createMapLayer:function(map) {
	this.layer = this.createMapLayerInner(map);
	return this.layer;
    },
    createMapLayerInner:function(map) {	
	if(this.opts.creator) {
	    return  this.opts.creator(this,map);
	}

	if(this.opts.type=='simple') {
	    return map.makeSimpleWms(this.id);
	}
	if(this.opts.type=='wms') {
            return map.addWMSLayer(this.name,this.url,this.opts.layer, Utils.isDefined(this.opts.isOverlay)?!this.opts.isOverlay:true);
	}
	return map.createXYZLayer(this.name,this.url,this.opts.attribution,this.opts.isOverlay);
    }
}


var map_default_layer = 'osm';



new MapLayer('osm','OSM',['//a.tile.openstreetmap.org/${z}/${x}/${y}.png',
			  '//b.tile.openstreetmap.org/${z}/${x}/${y}.png',
			  '//c.tile.openstreetmap.org/${z}/${x}/${y}.png'],
	     {attribution:'Map courtesy of OSM'});


new MapLayer('esri.topo','ESRI Topo','https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/${z}/${y}/${x}',
	     {isForMap:true,attribution:'Map courtesy of ESRI'});


new MapLayer('google.roads','Google Maps - Roads','https://mt0.google.com/vt/lyrs=m&hl=en&x=${x}&y=${y}&z=${z}',{attribution:'Map courtesy of Google'});
new MapLayer('google.hybrid','Google Maps - Hybrid','https://mt0.google.com/vt/lyrs=y&hl=en&x=${x}&y=${y}&z=${z}',{attribution:'Map courtesy of Google'});
new MapLayer('esri.street','ESRI Streets','https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/${z}/${y}/${x}',
	     {attribution:'Map courtesy of ESRI'});
new MapLayer('esri.natgeoworldmap','ESRI NatGeo World Map','https://server.arcgisonline.com/ArcGIS/rest/services/NatGeo_World_Map/MapServer/tile/{z}/{y}/{x}',{attribution:'Tiles (C) Esri -- National Geographic, Esri, DeLor'});


new MapLayer('opentopo','OpenTopo','//a.tile.opentopomap.org/${z}/${x}/${y}.png}',
	    {attribution:'Map courtesy of OpenTopo'});

new MapLayer('usfs','Forest Service','https://caltopo.com/tile/f16a/${z}/${x}/${y}.png',{attribution:'Map courtesy of Caltopo'});

new MapLayer('caltopo.mapbuilder','MapBuilder Topo','https://img.caltopo.com/tile/mbt/${z}/${x}/${y}.png',{attribution:'Map courtesy of Caltopo'});

new MapLayer('usgs.topo','USGS Topo','https://basemap.nationalmap.gov/ArcGIS/rest/services/USGSTopo/MapServer/tile/${z}/${y}/${x}',{attribution:'USGS - The National Map'});

new MapLayer('esri.worldtopomap','ESRI World Topo','https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}',{attribution:'Tiles (C) Esri -- Esri, DeLorme, NAVTEQ, TomTom, I'});


new MapLayer('google.terrain','Google Maps - Terrain','https://mt0.google.com/vt/lyrs=p&hl=en&x=${x}&y=${y}&z=${z}',
	     {attribution:'Map courtesy of Google'});
new MapLayer('google.satellite','Google Maps - Satellite','https://mt0.google.com/vt/lyrs=s&hl=en&x=${x}&y=${y}&z=${z}',{attribution:'Map courtesy of Google'});

new MapLayer('naip','NAIP - USDA',
	     'https://gis.apfo.usda.gov/arcgis/rest/services/NAIP/USDA_CONUS_PRIME/ImageServer/tile/${z}/${y}/${x}?blankTile=false',{attribution:'Map courtesy of USDA'});

new MapLayer('naip.esri','NAIP - ESRI',
	     'https://naip.maptiles.arcgis.com/arcgis/rest/services/NAIP/MapServer/tile/${z}/${y}/${x}',{attribution:'Map courtesy of ESRI'});

new MapLayer('naip.caltopo','NAIP - CalTopo',
	     'https://caltopo.com/tile/n/${z}/${x}/${y}.png',{attribution:'Map courtesy of Caltopo'});

new MapLayer('esri.worldimagery','ESRI World Imagery','https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',{attribution:'Tiles (C) Esri -- Source: Esri, i-cubed, USDA, USG'});

new MapLayer('usgs.imagery','USGS Imagery','https://basemap.nationalmap.gov/ArcGIS/rest/services/USGSImageryOnly/MapServer/tile/${z}/${y}/${x}', {attribution:'USGS - The National Map'});

new MapLayer('usgs.imagery.topo','USGS Imagery-Topo',
	     'https://basemap.nationalmap.gov/arcgis/rest/services/USGSImageryTopo/MapServer/tile/${z}/${y}/${x}', {attribution:'USGS - The National Map'});


new MapLayer('usgs.hydro','USGS Hydro',
	     'https://basemap.nationalmap.gov/arcgis/rest/services/USGSHydroCached/MapServer/tile/${z}/${y}/${x}', {attribution:'USGS - The National Map'});



new MapLayer('esri.shaded','ESRI Shaded Relief','https://server.arcgisonline.com/ArcGIS/rest/services/World_Shaded_Relief/MapServer/tile/${z}/${y}/${x}',{attribution:'Map courtesy of ESRI'});
new MapLayer('esri.lightgray','ESRI Light Gray','https://services.arcgisonline.com/arcgis/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/${z}/${y}/${x}',{attribution:'Map courtesy of ESRI'});
new MapLayer('esri.darkgray','ESRI Dark Gray','https://services.arcgisonline.com/arcgis/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/${z}/${y}/${x}',{attribution:'Map courtesy of ESRI'});
new MapLayer('esri.terrain','ESRI Terrain','https://server.arcgisonline.com/ArcGIS/rest/services/World_Terrain_Base/MapServer/tile/${z}/${y}/${x}',{attribution:'Map courtesy of ESRI'});
new MapLayer('shadedrelief','Shaded Relief','https://caltopo.com/tile/hs_m315z45s3/${z}/${x}/${y}.png',{attribution:'Map courtesy of Caltopo'});

new MapLayer('world.hillshade','World Hillshade','https://services.arcgisonline.com/arcgis/rest/services/Elevation/World_Hillshade/MapServer/tile/${z}/${y}/${x}', {attribution:'USGS - The National Map'});

new MapLayer('cartolight','Carto-Light','https://cartodb-basemaps-a.global.ssl.fastly.net/light_all/${z}/${x}/${y}.png');
new MapLayer('esri.worldgraycanvas','ESRI World Gray','https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/{z}/{y}/{x}',{attribution:'Tiles (C) Esri -- Esri, DeLorme, NAVTEQ'});
new MapLayer('cartodb.positron','CartoDB Positron','https://a.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png',{attribution:'(C) OpenStreetMap contributors (C) CARTO'});

new MapLayer('cartodb.voyager','CartoDB Voyager','https://a.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png',{attribution:'(C) OpenStreetMap contributors (C) CARTO'});


new MapLayer('esri.worldphysical','ESRI World Physical','https://server.arcgisonline.com/ArcGIS/rest/services/World_Physical_Map/MapServer/tile/{z}/{y}/{x}',{attribution:'Tiles (C) Esri -- Source: US National Park Service'});


new MapLayer('publiclands','Public Lands','https://caltopo.com/tile/sma/${z}/${x}/${y}.png',
	     {attribution:'Map courtesy of Caltopo',isOverlay:true,legend:ramaddaBaseUrl+'/images/publiclands.png'});

new MapLayer('federallands','Federal Lands',['//gis.blm.gov/arcgis/rest/services/lands/BLM_Natl_SMA_Cached_without_PriUnk/MapServer/tile/${z}/${y}/${x}'],
	     {legend:ramaddaBaseUrl+'/images/federallands.png',
	      attribution:'Map courtesy of BLM'});

new MapLayer('seafloor','Seafloor',['//tiles.arcgis.com/tiles/C8EMgrsFcRFL6LrL/arcgis/rest/services/GEBCO_basemap_NCEI/MapServer/tile/${z}/${y}/${x}']);

new MapLayer('esri.oceanbasemap','ESRI Ocean Basemap','https://server.arcgisonline.com/ArcGIS/rest/services/Ocean/World_Ocean_Base/MapServer/tile/{z}/{y}/{x}',{attribution:'Tiles (C) Esri -- Sources: GEBCO, NOAA, CHS, OSU,'});


new MapLayer('historic','Caltopo Historic','https://caltopo.com/tile/1900/${z}/${x}/${y}.png',{attribution:'Map courtesy of Caltopo',isOverlay:true});

new MapLayer('strava.all','Strava - All','https://heatmap-external-a.strava.com/tiles/all/hot/{z}/{x}/{y}.png',{attribution:'Map tiles by <a href="https://labs.strava.com/heat'});


new MapLayer('nasa.earthatnight','Earth at Night','https://map1.vis.earthdata.nasa.gov/wmts-webmerc/VIIRS_CityLights_2012/default//GoogleMapsCompatible_Level8/{z}/{y}/{x}.jpg',{attribution:'Imagery provided by services from the Global Image'});

new MapLayer('moon','Moon','https://cartocdn-gusc.global.ssl.fastly.net/opmbuilder/api/v1/map/named/opm-moon-basemap-v0-1/all/${z}/${x}/${y}.png');
new MapLayer('mars','Mars','https://cartocdn-gusc.global.ssl.fastly.net/opmbuilder/api/v1/map/named/opm-mars-basemap-v0-1/all/${z}/${x}/${y}.png');

new MapLayer('lightblue','','',{type:'simple'});
new MapLayer('blue','','',{type:'simple'});
new MapLayer('white','','',{type:'simple'});
new MapLayer('black','','',{type:'simple'});
new MapLayer('gray','','',{type:'simple'});









MapUtils.initSymbols();

var ARG_ZOOMLEVEL = 'zoomLevel';
var ARG_MAPCENTER = 'mapCenter';

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
    let linkGroup = source.params.linkGroup;
    if(!source.params.linked && !source.params.linkMouse && !linkGroup) return;
    var bounds = source.getBounds();
    var baseLayer = source.map.baseLayer;
    var zoom = source.map.getZoom();
    for(var i=0;i<window.globalMapList.length;i++) {
	var map = window.globalMapList[i];
	if(map.stateIsBeingSet || map.mapId==source.mapId) continue;
	if(linkGroup != map.params.linkGroup) continue;
	map.stateIsBeingSet = true;
	map.receiveShareState(source, state);
	map.stateIsBeingSet = false;
    }
}



function CollisionHandler(map,opts) {
    opts = opts??{};
    $.extend(this, {
	map:map,
	countAtPoint:{},
	offset: 0,
	state:{},
	collisionArgs:opts.collisionArgs??{},
	collisionInfos:[]
    })
    this.opts = opts;
    let mapBounds= this.map.getBounds();
    let mapW= mapBounds.right-mapBounds.left;
    let divW =  $("#" + this.map.mapDivId).width();
    let baseOffset= mapW*0.025;
    let pointSize= opts.pointSize??16;
    let pixelsPer= divW/mapW;
    let scaleFactor= 360/pixelsPer;
    let cnt= 0;
    //figure out the offset but use cnt so we don't go crazy
    while(pixelsPer*this.offset<pointSize && cnt<100) {
	this.offset+=baseOffset;
	cnt++;
    }

    this.getCollisionPoint = p=>{
        let pt = MapUtils.createLonLat(p.x,p.y);
        pt = this.map.transformLLPoint(pt);
	pt = this.map.getMap().getViewPortPxFromLonLat(pt);
	const x = Math.round(pt.x / pointSize) * pointSize;
	const y = Math.round(pt.y / pointSize) * pointSize;
	let location = this.map.transformProjPoint(this.map.getMap().getLonLatFromPixel({x:x,y:y}));
	let point= MapUtils.createPoint(location.lon,location.lat);
	if(this.countAtPoint[point]) {
	    this.countAtPoint[point]++;
	} else {
	    this.countAtPoint[point]=1;
	}
	return point;
    };
}


CollisionHandler.prototype = {
    initPoints:function(points) {
	return points.map(point=>{
	    if(point.x===null || point.y===null) return;
	    point.collisionPoint = this.getCollisionPoint(point);
	});
    },
    getCollisionInfo:function(collisionPoint,hook) {
	let info = this.state[collisionPoint];
	if(!info) {
	    info = this.state[collisionPoint]=
		new CollisionInfo(this, this.countAtPoint[collisionPoint], collisionPoint,this.collisionArgs);
	    this.collisionInfos.push(info);
	}
	return info;
    },
    getCollisionInfos:function() {
	return this.collisionInfos;
    },
    addCollisionLines:function(info,lines) {
	if(this.opts.addCollisionLines)
	    this.opts.addCollisionLines(info,lines);
	else
	    this.map.getLinesLayer().addFeatures(lines);
    },
    setCollisionVisible:function(info,visible) {
	if(this.opts.setCollisionVisible)
	    this.opts.setCollisionVisible(info,visible);
    }

}


function CollisionInfo(handler,numRecords, collisionPoint,args) {
    $.extend(this,{
	handler:handler,
	fixed:false,
	visible: false,
	icon:null,
	iconSize:16,
	dotOpacity:1.0,
	dotColor:'blue',
	dotColorOn:null,
	dotColorOff:null,
	dotRadius:12,
	ringColor:'red',
	ringWidth:3,
	scaleDots:false,
	labelTemplate:'${count}',
	labelColor:"#fff",
	labelFontSize:10,
	dots: null,
	collisionPoint:collisionPoint,
	dot:null,
	numRecords:numRecords,
	records:[],
	addLines:false,
	lines:[],
	features:[],
    });


    if(args) {
	$.extend(this,args);
	if(this.textGetter) {
	    this.myTextGetter = (feature)=>{
		return  this.textGetter(this.records);
	    }
	}
    }

    if(!Utils.stringDefined(this.labelTemplate)) this.labelTemplate = null;
    if(this.visible) {
	this.checkLines();
    }    
}


CollisionInfo.prototype = {
    addRecord: function(record) {
	this.records.push(record);
    },
    addLine:function(line) {
	this.lines.push(line);
    },
    checkLines: function() {
	if(!this.addedLines) {
	    this.addedLines = true;
	    this.handler.addCollisionLines(this,this.lines);
	}
    },
    createDots: function(idx) {
	this.dots = [];
	if(this.icon) {
	    this.dots.push(this.handler.map.createMarker("dot-" + idx, [this.collisionPoint.x,this.collisionPoint.y], this.icon, "", "",null,this.iconSize,null,null,null,null,false));

	} else {
	    let style = this.getCollisionDotStyle(this);
	    let dot = this.handler.map.createPoint("dot-" + idx, this.collisionPoint, style,null,this.myTextGetter);
	    this.dots.push(dot);
	    style = $.extend({},style);
	    style.label=null;
	    style.fillColor='transparent';
	    style.strokeColor=this.ringColor;
	    style.strokeWidth=this.ringWidth;

	    dot = this.handler.map.createPoint("dot2-" + idx, this.collisionPoint, style,null,this.myTextGetter);
	    this.dots.push(dot);		
	}
	this.dots.forEach(dot=>{
	    dot.collisionInfo  = this;
	});
	return this.dots;
    },
    dotSelected:function(dot) {
	if(this.fixed) return false;
	this.setVisible(!this.visible);
	return true;
    },
    styleCollisionDot:function(dot) {
	$.extend(dot.style, this.getCollisionDotStyle(dot.collisionInfo));
    },
    addFeatures:function(features) {
	this.features.push(...features);
//	points.forEach(p=>this.points.push(p));
    },
    getCollisionDotStyle:function(collisionInfo) {
	let dotColor= this.dotColor;
	let dotRadius = this.dotRadius;
	if(!this.fixed) {
	    if(this.scaleDots) {
		let extra = collisionInfo.numRecords;
		if(extra>1)
		    dotRadius = Math.min(Math.ceil(dotRadius+extra/2),28);
//		console.log("scaling dots " + dotRadius +" " +collisionInfo.numRecords);
	    } 

	    if(collisionInfo.visible)  {
		dotColor = this.dotColorOn??dotColor;
	    } else {
		dotColor = this.dotColorOff??dotColor;
	    }
	}
	let style = {
	    fillColor:dotColor,
	    fillOpacity:this.dotOpacity,
	    pointRadius:dotRadius,
	    strokeWidth:0
	}
	if(this.labelTemplate) {
	    style =
		$.extend(style,{
		    label:this.labelTemplate.replace('${count}',this.records.length),
		    fontColor:this.labelColor,
		    fontSize:this.labelFontSize,
		    labelOutlineColor:'transparent',

		});
	}		    

	return style;

    },


    setVisible:function(visible) {
	this.visible = visible;
	this.dots.forEach(dot=>{
	    this.styleCollisionDot(dot);
	    dot.layer.drawFeature(dot, dot.style);
	});

	this.checkLines();
	//These are the spokes
	this.lines.forEach(f=>{
	    f.featureVisible = this.visible;
	    this.handler.map.checkFeatureVisible(f,true);
	});
	this.handler.setCollisionVisible(this,visible);
    }
}


function RamaddaBounds(north,west,south,east) {
    if(north && Utils.isDefined(north.north)) {
	let b = north;
	this.north = b.north;
	this.west  = b.west;
	this.south  =b.south;
	this.east = b.east;
    } else if(north && Utils.isDefined(north.top)) {
	let b = north;
	this.north = b.top;
	this.west  = b.left;
	this.south  =b.bottom;
	this.east = b.right
    }  else { 
	this.north = north;
	this.west  = west;
	this.south  =south;
	this.east = east;
    }
    $.extend(this,{
	toString: function() {
	    return "N:" + this.north +" W:" + this.west +" S:" + this.south +" E:" + this.east;
	}
    });
	      
}
RamaddaBounds.prototype= {
    expand:function(lat,  lon) {
        this.north = isNaN(this.north)
                     ? lat
                     : Math.max(this.north, lat);
        this.south = isNaN(this.south)
                     ? lat
                     : Math.min(this.south, lat);
        this.west  = isNaN(this.west)
                     ? lon
                     : Math.min(this.west, lon);
        this.east  = isNaN(this.east)
                     ? lon
                     : Math.max(this.east, lon);
    }


    
};
