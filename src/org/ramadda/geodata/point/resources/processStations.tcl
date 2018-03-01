set ::statesFile  /Users/jeffmc/source/ramadda-code/src/org/ramadda/repository/resources/geo/states.txt
foreach line  [split [read [open $::statesFile r]] "\n"] {
    set line [string trim $line]
    if {$line == ""}  continue;
    if {[regexp {^#.*} $line]} continue;
    foreach {abbr name id lat lon} [split $line "\t"] break
    
    set ::stateNames([string tolower $abbr]) $name
    set ::stateNames($abbr) $name
}


set ::stateNames(pr) "Puerto Rico"
set ::stateNames(gu) "Guam"



set ::wiki {<wiki>
+section label={{name}} #
{{map listentries="true" width="800" height="600"}}
-section
+section label="Data" #
{{table}}
-section
}

puts "<entries>"
proc station {id name latitude longitude st} {
    set parent $st
    if {![info exists ::states($st)]} {
        set ::states($st) 0
        set  name $st
        if {[info exists ::stateNames([string tolower $st])]} {
            set name $::stateNames([string tolower $st])
        }
        puts "<entry type=\"group\" name=\"$name Weather Stations\" id=\"$st\">"
        puts "<description><!\[CDATA\[$::wiki\]\]></description>"
        puts "</entry>"
    }
    incr   ::states($st)
##    if {$::states($st)>3} {return}
    puts "<entry type=\"type_awc_metar\" name=\"$id - $name\" latitude=\"$latitude\" longitude=\"$longitude\" parent=\"$parent\">"
    puts "<site_id><!\[CDATA\[$id\]\]></site_id>"
    puts "<time_offset><!\[CDATA\[24\]\]></time_offset>"
    puts "</entry>"

}

source stations.tcl


puts "</entries>"