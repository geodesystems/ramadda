//
//This JS file is a RAMADDA plugin file. Add it to your RAMADDA home/plugins directory and
//it will get included in any page with a map since it ends with "mapextra.js"
//


//This adds WMS or WMTS layers. The call format is:
//MapLayer(id,name,URL, options) 
//The id is used to reference this map layer in a display map
//The options specify:
//type: specify if this is a wms or a wmts (the default)
//layer: the WMS layer.
//isOverlay - if false then this is a base layer. Else it is an overlay
//isForMap- if false then this layer won't show up in the layer list but can still be
//referenced in a map

//
//Drought layers. These are WMS layers
//
new MapLayer('7_day_emodis_vegdri_index','7-Day eMODIS VegDRI Index',
	     'https://dmsdata.cr.usgs.gov/geoserver/quickdri_vegdri_conus_week_data/vegdri_conus_week_data/ows',
	     {type:'wms',layer:'vegdri_conus_week_data',isOverlay:true,isForMap:false});
new MapLayer('7_day_emodis_quickdri_index','7-Day eMODIS QuickDRI Index',
	     'https://dmsdata.cr.usgs.gov/geoserver/quickdri_quickdri_conus_week_data/quickdri_conus_week_data/ows',
	     {type:'wms',layer:'quickdri_conus_week_data',isOverlay:true,isForMap:false});

//
//A WMTS layer
//

new MapLayer('bocoopenspace','Boulder Co. Open Space','https://caltopo.com/tile/geoimage_571EF841944797FD7E2492988BD9901BFD781CC44B18A10E3C4158D7F9484FF5/${z}/${x}/${y}@2x.png',
	     {attribution:'Map from Caltopo',isOverlay:true,isForMap:false});

