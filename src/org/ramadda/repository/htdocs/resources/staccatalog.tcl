puts "{"
puts {
    "title": "RAMADDA STAC Catalog",
    "type": "Catalog",
    "id": "ramadda-catalog",
    "stac_version": "1.0.0",
    "description": "Top-level catalog of STAC catalogs.",
    "links": [
}
    
set cnt 0
foreach item {
    {"Google Earth Engine"  "https://earthengine-stac.storage.googleapis.com/catalog/catalog.json"}
    {"Google Earth Engine for Open EO" "https://earthengine-stac.storage.googleapis.com/catalog/catalog.json"}
    {"NASA ISERV Science Operation Center"  "https://nasa-iserv.s3-us-west-2.amazonaws.com/catalog/catalog.json"} 
    {"Earth Search" https://earth-search.aws.element84.com/v0}
    {"NASA CMR" https://cmr.earthdata.nasa.gov/stac}
    {"ESA Open Science Catalog"  "https://eoepca.github.io/open-science-catalog-metadata/catalog.json"} 
    {"Digital Earth Africa"  "https://explorer.digitalearth.africa/stac/"}
    {"Digital Earth Australia"  https://explorer.sandbox.dea.ga.gov.au/stac/}     
    {"Microsoft Planetary Computer" https://planetarycomputer.microsoft.com/api/stac/v1}
    {"Monthly Mosaic of Sentinel 2 Images for Catalonia" https://datacloud.icgc.cat/stac-catalog/catalog.json}
    {"OpenTopography Data Catalog" https://portal.opentopography.org/stac/catalog.json}
    {"SPOT Orthoimages of Canada" https://spot-canada-ortho.s3.amazonaws.com/catalog.json }
    {"World Bank - Light Every Night" https://globalnightlight.s3.amazonaws.com/VIIRS_npp_catalog.json}
    {"California Forest Observatory"     "https://storage.googleapis.com/cfo-public/catalog.json"} 
    {"Astrea Earth OnDemand geospatial imagery query and analysis tool"     "https://eod-catalog-svc-prod.astraea.earth/"} 
} {


foreach {title url} $item break
    if {$cnt!=0} {
	puts ","
    }
    incr cnt
    puts -nonewline "{"
    puts "\"title\":\"$title\","
    puts "\"href\": \"$url\","
    puts {"rel": "child", "type": "application/json"}
    puts "}"
}

puts "\],\n\"stac_extensions\": \[\]\n\}"
