source [file join [file dirname [info script]] lib.tcl]

set ::gpsxml [file join $::resourcedir gpsramadda.xml]

#Recurse down the tree
proc addGps {file underGps} { 
    if {![file isdirectory $file]} {
	return
    }
    set tail [file tail $file]

    if {$underGps} {
	copyFile $::gpsxml [file  join [file dirname $file] .$tail.ramadda.xml]
    }
    ##Have this here because we only put the .xml into sub-dirs of the GPS dir
    if {$tail== "GPS"} {
	set underGps 1
    }


    foreach child [glob -nocomplain $file/*] {
	addGps $child $underGps
    }
}


foreach dir $argv {
    addGps $dir 0
}

