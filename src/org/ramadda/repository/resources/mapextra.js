//this gets wrapped in
//This gets called when a new map is created
map.addBaseLayer(map.createXYZLayer("Boulder Co. Open Space", "https://caltopo.com/tile/geoimage_571EF841944797FD7E2492988BD9901BFD781CC44B18A10E3C4158D7F9484FF5/${z}/${x}/${y}@2x.png","Map from Caltopo",true));

let maxZoom =  8;
let wlayers = [ 
    {name:'GOES Infrared',maxZoom:maxZoom,		    id:'goes-ir-4km-900913', alias:'goes-ir'},
    {name:'GOES Water Vapor', maxZoom:maxZoom,id:'goes-wv-4km-900913', alias:'goes-wv'},
    {name:'GOES Visible', maxZoom:maxZoom,id:'goes-vis-1km-900913', alias:'goes-visible'},
    {name:'NWS Radar', maxZoom:maxZoom,id:'nexrad-n0q-900913',alias:'nexrad'},
    {name:'24 hr precip', maxZoom:maxZoom,id:'q2-p24h-900913',alias:'precipition'}];

//wlayers = [ {name:'NWS Radar', maxZoom:maxZoom,id:'nexrad-n0q-900913',alias:'nexrad'}]


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


wlayers.forEach(l=>{
    let layer = new OpenLayers.Layer.TMS(
        l.name,
        'https://mesonet.agron.iastate.edu/cache/tile.py/', {
            layername: l.id,
            service: '1.0.0',
            type: 'png',
            visibility: false,
            getURL: get_my_url,
            isBaseLayer: false,
	    maxZoomLevel: l.maxZoom,
	    minZoomLevel: l.minZoom,			    
        }, {}
    );
    let redrawFunc = () =>{
	if(layer.getVisibility()) {
	    layer.redraw(true);
	}
	setTimeout(redrawFunc,1000*60*5);
    };
    setTimeout(redrawFunc,1000*60*5);

    map.baseLayers[l.id] = layer;
    if(l.alias) map.baseLayers[l.alias] = layer;		    
    layer.ramaddaId = l.id;
    map.addLayer(layer,true);
});
