set url  "https://arcgis.sd.gov/arcgis/rest/services/DENR/NR42_SpillReports_Public/MapServer/0/query?resultOffset={offset}&resultRecordCount=1000&f=geojson&returnGeometry=true&spatialRel=esriSpatialRelIntersects&geometry=%7B%22xmin%22%3A-100958012.374962984%2C%22ymin%22%3A-5322463.1535537895%2C%22xmax%22%3A10644926.307106916%2C%22ymax%22%3A15635549.221409855%2C%22spatialReference%22%3A%7B%22wkid%22%3A3857%7D%7D&geometryType=esriGeometryEnvelope&inSR=3857&outFields=*&outSR=4326"

for {set off 0} {$off<20000} {incr off 1000} {
    regsub "{offset}" $url $off _url
    set file spill_${off}.geojson
    if {![file exists $file]} {
	catch {exec wget -O spill_${off}.geojson $_url}
    }
    if {![file exists $file]} {
	continue;
    }	
    if {[file size $file]<500} {
	continue;
    }
    set csv "spill_${off}.csv"
    if {[file exists $csv]} {
	continue;
    }
    puts "file: $file"
    exec sh /Users/jeffmc/bin/seesv.sh -geojson false -p $file  > $csv
}


eval exec sh /Users/jeffmc/bin/seesv.sh  -append 1 [glob spill_*.csv] > allspills.csv
exec sh /Users/jeffmc/bin/seesv.sh  -change material "\"" "" -change material petroleum Petroleum -trim "0-10" -addheader "site_type.type enumeration material.type multienumeration city.type enumeration county.type enumeration status.type enumeration spill_cat.type enumeration sor_type.label {Source} sor_type.type enumeration zip_code.type string" -p allspills.csv > sd_spills.csv
