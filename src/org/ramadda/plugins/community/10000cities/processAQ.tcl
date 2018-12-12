set ::scriptDir [file dirname [info script]]
source ~/bin/lib.tcl
source ~/bin/utils.tcl


set ::xml ""
append ::xml "<entries>"
append ::xml "\n"
append ::xml "<entry id=\"US\" type=\"group\" name=\"US Air Quality Stations\"/>"
append ::xml "\n"
set ::stateList [list]
proc process {country state city location lat lon} {
#    if {$state!="Nebraska"} return;
    if {![info exists ::states($state)]} {
        lappend ::stateList $state
        set inner [xmlTag description [xmlCdata "${state}_description"] {}] 
        set attrs [list id $state parent US type group name $state]
        append ::xml [xmlTag entry $inner  $attrs]
        append ::xml "\n"
        set ::states($state) 0
    }
    incr ::states($state) 
    set inner  "\n"
    append inner [xmlTag name [xmlCdata "$city - $location"] {}] 
    append inner [xmlTag country [xmlCdata $country] {}] 
    append inner [xmlTag city [xmlCdata $city] {}] 
    append inner [xmlTag location [xmlCdata $location] {}] 
    append inner "\n"
    set attrs [list parent $state type type_point_openaq  latitude $lat longitude $lon]
    append ::xml [xmlTag entry $inner  $attrs]
    append ::xml "\n"
}
source $::scriptDir/aqstations.tcl
append ::xml  "</entries>"
append ::xml "\n"

foreach state $::stateList {
    regsub -all "${state}_description" $::xml ":note $::states($state) stations" ::xml
}
puts $::xml
