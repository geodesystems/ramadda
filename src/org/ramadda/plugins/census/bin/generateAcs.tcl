
set ::statesFile  /Users/jeffmc/source/ramadda-code/src/org/ramadda/repository/resources/geo/states.txt

set fields "NAME\nB01003_001E\nB01001_002E\nB01001_026E\nB01001A_001E\nB01001B_001E\nB01001I_001E\nB01001D_001E\nB25001_001E\nB07013_002E\nB07013_003E\n"

set ::for_type county
set ::in_type state


set ::states []
set ::baseDir "[file dirname [file dirname $argv0]]"
set ::resourceDir "$::baseDir/resources"
foreach line  [split [read [open $::statesFile r]] "\n"] {
    set line [string trim $line]
    if {$line == ""}  continue;
    if {[regexp {^#.*} $line]} continue;
    foreach {abbr name id lat lon} [split $line "\t"] break
    lappend ::states [list $abbr $name $id]
}




set ::xml "<entries>\n";

set n "US Population"
append ::xml "<entry type=\"group\" name=\"$n\" id=\"topgroup\" >\n";
append ::xml "<fields>$fields</fields>\n";
append ::xml "<for_type>state</for_type>\n";
append ::xml "<in_type>none</in_type>\n";
append ::xml "</entry>\n"

foreach s $::states {
    foreach {abbr name id} $s break
    set n "$name Counties"
    append ::xml "<entry type=\"type_census_acs\" name=\"$n\" parent=\"topgroup\">\n";
    append ::xml "<fields>$fields</fields>\n";
    set ::in_value $id
    append ::xml "<for_type>$::for_type</for_type>\n";
    append ::xml "<in_type1>$::in_type</in_type1>\n";
    append ::xml "<in_value1>$id</in_value1>\n";
    append ::xml "</entry>\n"
    
}
append ::xml "</entries>\n";

puts "$::xml"