#!/usr/bin/tclsh

set ::mydir [file dirname [file normalize [info script]]]
set ::root https://ramadda.org/repository
set ::maxCount 10000
set ::total 0
set groupID ""
set urls [list]
set ::sleep 5

source $::mydir/uitest.tcl

set ::urlsfp [open testurls.txt w]

proc writeUrl {url {desc ""}} {
    if {$desc!=""} {
	puts $::urlsfp "#$desc"
    }
    puts $::urlsfp $url
    flush $::urlsfp
}

proc runGroup {group id {groupLimit 10000}} {
    if {$::total>=$::maxCount} {
	return;
    }

    write "</div>\n"
    write "<h2>$group</h2><div class=ramadda-grid>"
    if {[regexp http $group]} {
	set url $group
    } else {
	set url "$::root/entry/show?ascending=true&orderby=name&entryid=${id}&output=default.csv&escape=true&fields=name,id&showheader=false&showheader=false"
    }
    puts stderr "group: $group $url"
    writeUrl $url CSV
    set csv [getUrl $url]
    regsub -all { } $group _ _group
    foreach line2 [split $csv "\n"] {
	set line2 [string trim $line2]
	if {$line2==""} continue;
	foreach     {name id} [split $line2 ,] break
	set url "$::root/entry/show?entryid=$id#fortest"
	writeUrl $url $name
	capture $_group $name $url 0 $::sleep
	incr ::total 1
	if {$::total>=$::maxCount} {
	    break;
	}
    }
}


proc usage {} {
    puts stderr "usage tclsh runuitest.tcl <-clean (remove and thumb_ and console_ files> <-o output.html> <-sleep sleep_seconds> <-urls file (file contains urls to test_> <url>"
}



for {set i 0} {$i <[llength $argv]} {incr i} {
    set  arg [lindex $argv $i]
    if {$arg == "-help"} {
	usage
	exit
    }
    if {$arg == "-o"} {
	incr i
	set ::output [open [lindex $argv $i] w]
	continue;
    }

    if {$arg == "-clean"} {
	foreach file [glob -nocomplain thumb_*.png] {
	    file delete -force $file
	}
	foreach file [glob -nocomplain console_*.txt] {
	    file delete -force $file
	}	    
	continue;
    }
    
    if {$arg == "-max"} {
	incr i
	set ::maxCount  [lindex $argv $i]
	continue;
    }

    if {$arg == "-sleep"} {
	incr i
	set ::sleep [lindex $argv $i]
	continue;
    }
    if {$arg == "-group"} {
	incr i
	set groupID [lindex $argv $i]
	continue;
    }

    if {$arg == "-urls"} {
	incr i
	set file [lindex $argv $i]
	foreach url [split [read [open $file r]] "\n"]  {
	    set url [string trim $url]
	    if {$url == ""} {continue;}
	    if {[regexp "^#.*" $url]} continue;
	    lappend  urls  $url
	}
	continue
    }
    if {![regexp {http} $arg]} {
	puts stderr "Unknown argument:$arg"
	usage
	exit
    }

    lappend urls "$arg"
}


if {$::output==""} {
    set ::output [open results.html w]
}

if {$groupID!=""} {
    runGroup "Group" $groupID
} elseif {[llength $urls]} {
    set cnt 0
    foreach url $urls {
	incr cnt

        if {[regexp {sleep(.*)} $url match pause]} {
	    set pause [string trim $pause]
	    puts stderr "sleep:$pause"
	    exec sleep $pause
	    continue
	}
	capture ""  "Page $cnt" $url 0 $::sleep
	incr ::total 1
	if {$::total>=$::maxCount} {
	    break;
	}
    }
} else {
#Run with the default ramadda.org entries
    runGroup "Test Suite" ce064b0c-ad96-49ac-b7b2-6bc8ce86aac4
    runGroup "Natural Science" 3e14c357-9989-453e-ba3a-1837e79e9712
    runGroup "Asset Reports" 78ac9b04-b151-49d1-b682-e99d155ef70a
    runGroup "Asset Examples" 34be31f1-357c-46bf-b8a2-7c43dbfac1cc
    runGroup "Archive" cc4fbc93-b3af-406c-9ebf-1c80f241d84a
    runGroup "Data List" 5ec45056-fe82-4d98-a9c4-4f1da94be8b0
    runGroup "RAMADDA Tour" ramadda_tour
    runGroup "RAMADDA Tour - Part 2" ramadda_tour_2    
    runGroup "Tracks" 30361e7a-8d2a-4dde-b3b9-cb0fe556c8be
    runGroup "Earth Science" 624d4236-ac54-4566-ad5c-f46acdb26ee1
    runGroup "Misc" 81e5ae0a-be26-4133-89c3-1d3868d60e57
    runGroup "IMDV" aa5c104c-0c85-4937-86b1-7daf5e7dda28
    runGroup "SeeSV" a424786b-ebf2-43c3-add7-21c3bb2f526f
    runGroup "Charts" 3ebcb4f4-fa4d-4fb3-9ede-d42ec7e0aa9d
    runGroup "Map Examples" geospatial_data
    runGroup "All Maps" 1d0fa3f5-407e-4a39-a3da-9a5ed7e1e687
    runGroup "Misc Displays" 3270b9eb-b63b-4b41-8cd8-5a5e09ea0dae
    runGroup "Other Data" b36bb6fc-b2c4-4d12-8c31-1f4dcff6881e
    runGroup "Science Data" 1012d4bb-5e57-460a-95f5-07c997bd04e8
    runGroup "Soundings" 53f411f7-9390-4afc-9cb9-e7313030498b
    #runGroup Covid 52644ac1-f6d6-45ea-88af-b5d2ec75742e
    runGroup "Text" 23847d93-4bca-4d54-a6db-f96a19be250b
    runGroup "BOCO Elections" boco_election_displays
    runGroup "Boulder and Colorado" 4624f63d-cd71-43e8-a558-83835c6b5541
    runGroup "Dashboards" eb4102f8-720f-4ef3-9211-0ce5940da04d
    runGroup "Media" bca6228e-3f8e-49d4-a20e-b5a0ea8a6441
    runGroup "Cards" e4b6667d-d640-4048-a756-b06e4c352a62 3
    #runGroup Features 26fff0d9-3de7-4bbd-8a6f-a26d8a287f4a
    runGroup "Miscellany" a7fe0b5c-5c1b-4005-afce-e92b082fa335
    runGroup "Notebooks" 38bb6bb4-3f51-4625-9acd-c79c4f445c78
}
finish
exit


