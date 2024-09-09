

array set ::types {hyd Hydropgraphy bound {County Boundary} tnrg {Township Lines} quads {Quadrangle Boundaries} bnd {Political Boundaries} sec {Section Lines} rds Roads hypsography Hypsography}
set ::shapeFiles {}

set ::entries "<entries>\n"

proc fetchMaps {id name} {
    set dir "county_${id}"
    if {[file exists $dir]} {
	exec rm -r -f $dir
    }
    exec mkdir $dir
    set file ${id}_100kdlg.zip
    set url "http://mri.usd.edu/Basedata/dlg/100K/${file}"
    puts stderr "fetching $url"
    catch {exec wget -O $dir/$file $url}
    exec unzip -d $dir $dir/$file
    makeShapes $id $dir $name
}

proc makeShapes {mapId dir countyName} {
    append ::entries "<entry type=\"type_map_folder\" name=\"$countyName Maps\" id=\"$mapId\"/>\n"
    foreach shp [glob $dir/*.shp] {
	set id [file rootname [file tail $shp]]
	set files [glob $dir/$id.*]
	eval exec jar -cvf $id.zip $files
	lappend ::shapeFiles $id.zip
	regsub {^.*-} $id {} _id
	set mapType  $::types($_id)
	set desc "+callout-info\nMap is from \[https://www.sdgs.usd.edu/digitaldata/dlg100k.aspx South Dakota Geological Society\]\n-callout\n"
	append ::entries "<entry parent=\"$mapId\" isnew=\"true\" name=\"$countyName $mapType\" type=\"geo_shapefile\" file=\"$id.zip\">\n"
	append ::entries "<description><!\[CDATA\[$desc\]\]></description>"
	append ::entries "</entry>"
    }
}



fetchMaps td {Todd County}
append ::entries "</entries>\n"
set fp [open entries.xml w]
puts $fp $::entries
close $fp

eval exec jar -cvf import.zip entries.xml $shapeFiles
