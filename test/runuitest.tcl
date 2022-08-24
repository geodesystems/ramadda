#!/usr/bin/tclsh

set ::mydir [file dirname [file normalize [info script]]]
set ::root https://ramadda.org/repository
source $::mydir/uitest.tcl


proc runGroup {group id {groupLimit 10000}} {
    write "</div>\n"
    write "<h2>$group</h2><div class=ramadda-grid>"
    set url "$::root/entry/show?ascending=true&orderby=name&entryid=${id}&output=default.csv&fields=name,id&showheader=false&showheader=false"
    puts stderr "group: $group"
    set csv [getUrl $url]
    regsub -all { } $group _ _group
    foreach line2 [split $csv "\n"] {
	set line2 [string trim $line2]
	if {$line2==""} continue;
	foreach     {name id} [split $line2 ,] break
	set url "$::root/entry/show?entryid=$id#fortest"
	capture $_group $name $url
    }
}


set urls [list]
set sleep 5
for {set i 0} {$i <[llength $argv]} {incr i} {
    set  arg [lindex $argv $i]
    if {$arg == "-help"} {
	puts stderr "usage tclsh runuitest.tcl <-clean (remove and thumb_ and console_ files> <-o output.html> <-sleep sleep_seconds> <-urls file (file contains urls to test_> <url>"
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
    

    if {$arg == "-sleep"} {
	incr i
	set sleep [lindex $argv $i]
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
    lappend urls "$arg"
}
if {[llength $urls]} {
    set cnt 0
    foreach url $urls {
	incr cnt

        if {[regexp {sleep(.*)} $url match pause]} {
	    set pause [string trim $pause]
	    puts stderr "sleep:$pause"
	    exec sleep $pause
	    continue
	}
	capture ""  "Page $cnt" $url 0 $sleep
    }
} else {
#Run with the default ramadda.org entries
    runGroup "Charts" 3ebcb4f4-fa4d-4fb3-9ede-d42ec7e0aa9d
    runGroup "Maps" 1d0fa3f5-407e-4a39-a3da-9a5ed7e1e687
    runGroup "Data tables" b36bb6fc-b2c4-4d12-8c31-1f4dcff6881e
    runGroup "Science Data" 1012d4bb-5e57-460a-95f5-07c997bd04e8
    #runGroup Covid 52644ac1-f6d6-45ea-88af-b5d2ec75742e
    runGroup "Text" 23847d93-4bca-4d54-a6db-f96a19be250b
    runGroup "Boulder and Colorado" 4624f63d-cd71-43e8-a558-83835c6b5541
    runGroup Dashboards eb4102f8-720f-4ef3-9211-0ce5940da04d
    runGroup "Media" bca6228e-3f8e-49d4-a20e-b5a0ea8a6441
    runGroup Cards e4b6667d-d640-4048-a756-b06e4c352a62 3
    #runGroup Features 26fff0d9-3de7-4bbd-8a6f-a26d8a287f4a
    runGroup Misc a7fe0b5c-5c1b-4005-afce-e92b082fa335
    runGroup Notebooks 38bb6bb4-3f51-4625-9acd-c79c4f445c78
}
finish
exit


