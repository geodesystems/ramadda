#geojson is from
#https://www.sd511.org/#&zoom=7.978186436385607&lon=-100.30169924551444&lat=43.26529779342613

source $env(RAMADDA_ROOT)/bin/ramadda.tcl
package require json


if {![file exists cameralist.tcl]} {
    eval exec sh $::env(SEESV) -geojson false -tcl camera  [lindex $argv 0]   > cameralist.tcl
}


set ::xml "<entries>"
set ::cameraArgs {cameras  atmos  surface  name  mrm  description  latitude  longitude}

set ::cnt 0
proc camera $::cameraArgs { 
    set route $description
    
    set name "$name - $mrm "
    set cameras [string trim $cameras]
    set cameras [string range $cameras 1 end]
    set cameras [string range $cameras 0 [expr [string length $cameras] - 2]]
    set j [json::json2dict $cameras]
    foreach obj $j {
	incr ::cnt
	if {$::cnt>40} return;

	set url  [dict get  $obj image]
	set n  [dict get  $obj name]	
	set desc  [dict get  $obj description]
	set entryName [string trim "$route - $name"]
	regsub  "${mrm}\$" $entryName "MM $mrm" entryName
	if {$mrm!=""} {
#	    append entryName " MM $mrm"
	}
	if { $n!="" && $n!="null" } {
	    append entryName " - $n"
	}
	append ::xml [openEntry type_image_webcam  {} {} "$entryName" latitude $latitude longitude $longitude url $url]
	append ::xml [col description $desc]
	append ::xml [col location $route]	
	append ::xml [closeEntry] 
    }
}

source cameralist.tcl

append ::xml "</entries>"
puts $::xml
