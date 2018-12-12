set ::scriptDir [file dirname [info script]]
source ~/bin/lib.tcl
source ~/bin/utils.tcl


puts "<entries>"
puts "<entry id=\"US\" type=\"group\" name=\"US Air Quality Stations\"/>"
proc process {country state city location lat lon} {
#    if {$state!="Nebraska"} return;
    if {![info exists ::states($state)]} {
        puts "<entry id=\"$state\" parent=\"US\" type=\"group\" name=\"$state\"/>"
        set ::states($state) 1
    }
    set inner  "\n"
    append inner [xmlTag name [xmlCdata "$city - $location"] {}] 
    append inner [xmlTag country [xmlCdata $country] {}] 
    append inner [xmlTag city [xmlCdata $city] {}] 
    append inner [xmlTag location [xmlCdata $location] {}] 
    append inner "\n"
    set attrs [list parent $state type type_point_openaq  latitude $lat longitude $lon]
    puts [xmlTag entry $inner  $attrs]
}
source $::scriptDir/aqstations.tcl
puts "</entries>"


