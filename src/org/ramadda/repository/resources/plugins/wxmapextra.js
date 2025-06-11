//
//This JS file is a RAMADDA plugin file. Add it to your RAMADDA home/plugins directory and
//it will get included in any page with a map since it ends with "mapextra.js"
//



//
//This adds a number of WMTS layers of weather data
//from  https://mesonet.agron.iastate.edu/ogc/
//
function makeWxLayers() {
    let maxZoom =  8;
    let wlayers = [ 
	{name:'NEXRAD Reflectivity', maxZoom:11,id:'nexrad-n0q-900913',alias:'nexrad'},
	{name:'NEXRAD - 5 minutes old', maxZoom:11,id:'nexrad-n0q-900913-m5m',alias:'nexrad-5'},
	{name:'NEXRAD - 10 minutes old', maxZoom:11,id:'nexrad-n0q-900913-m10m',alias:'nexrad-10'},
	{name:'NEXRAD - 15 minutes old', maxZoom:11,id:'nexrad-n0q-900913-m15m',alias:'nexrad-15'},	
	{name:'NEXRAD - 20 minutes old', maxZoom:11,id:'nexrad-n0q-900913-m20m',alias:'nexrad-20'},
	{name:'NEXRAD - 30 minutes old', maxZoom:11,id:'nexrad-n0q-900913-m30m',alias:'nexrad-30'},	
	{name:'NEXRAD - 40 minutes old', maxZoom:11,id:'nexrad-n0q-900913-m40m',alias:'nexrad-40'},	
	{name:'NEXRAD - 50 minutes old', maxZoom:11,id:'nexrad-n0q-900913-m50m',alias:'nexrad-50'},		

	{name:'NEXRAD Echo Tops', maxZoom:11,id:'nexrad-eet-900913',alias:'echotops'},	
	{name:'1 Hour Precip', maxZoom:11,id:'q2-n1p-900913',alias:'1hourprecip'},
	{name:'24 hr precip', maxZoom:maxZoom,id:'q2-p24h-900913',alias:'precipitation'},
	{name:'GOES Infrared',maxZoom:maxZoom,   id:'goes-ir-4km-900913', alias:'goes-ir'},
	{name:'GOES Water Vapor', maxZoom:maxZoom,id:'goes-wv-4km-900913', alias:'goes-wv'},
	{name:'GOES Visible', maxZoom:maxZoom,id:'goes-vis-1km-900913', alias:'goes-visible'}
    ];


    //This is a  bit more complex than most layer specifications as it calculates the resolution
    //(and some other things that slips my mind now)



    let ctor = (mapLayer,map)=>{
	let _this = map;
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


	return  new OpenLayers.Layer.TMS(
            mapLayer.name,
            'https://mesonet.agron.iastate.edu/cache/tile.py/', {
		layername: mapLayer.id,
		service: '1.0.0',
		type: 'png',
		visibility: false,
		getURL: get_my_url,
		isBaseLayer: false,
		maxZoomLevel: mapLayer.opts.maxZoom,
		minZoomLevel:mapLayer.minZoom,			    
            });
    };

    wlayers.forEach(l=>{
	new MapLayer(l.id,l.name,'',
		     {isOverlay:true,
		      refresh:300,
		      minZoom:l.minZoom,
		      maxZoom:l.maxZoom,
		      creator:ctor,
		      alias:l.alias
		     });
    });
}

makeWxLayers();
