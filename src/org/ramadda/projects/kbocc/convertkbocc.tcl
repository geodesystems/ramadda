set ::sitesfp [open foosites.xml w]
puts $::sitesfp "<entries>"

if {![file exists converted]} {
    exec mkdir converted
}


proc convert {site f latitude longitude notes} {
    set file /Users/jeffmc/kbocc/rawfiles/$f
    if {![file exists $file]} {
	puts stderr "No file: $file"
	return
    }	

    if {![info exists ::loc($site,lat)]} {
	set ::loc($site,lat) "$latitude"
	set ::loc($site,lon) "$longitude"
	puts $::sitesfp "<entry name=\"$site\" type=\"type_kbocc_site\" latitude=\"$latitude\" longitude=\"$longitude\"/>"
    } else {
	if {!($::loc($site,lat) eq $latitude) || !($::loc($site,lon) eq $longitude)} {
	    puts stderr "bad loc: $site $latitude $longitude should be $::loc($site,lat) $::loc($site,lon)"
	}
    }
    set yy ""
    set inst ""
    foreach pattern {{(\d\d)-(\d\d)} {(\d\d)_(\d\d)} {(\d\d)-(\d)} {^(\d\d)} {(\d\d\d\d)}} {
	if {[regexp  $pattern $f match yy inst]} {
	    break
	}
    }
    if {$yy==""} {
	puts stderr "no year $f"
	return
    }

    if {[string length $yy]==2} {
	set yy "20${yy}"
    }
    regsub -all { +} $site _ site
    if {$inst !=""} {
	set inst "_$inst"
    }
    set newFile [file join converted "${site}_$yy$inst.csv"]
    file copy -force $file  $newFile

    #Now move it to rawfiles2 for now
    set newFile /Users/jeffmc/kbocc/rawfiles2/$f
    file rename -force $file  $newFile
}




set lines [read [open files.csv r]]
set cnt 0
foreach line [split $lines "\n"] {
    set line [string trim $line]
    if {$line==""} continue
    incr cnt
    if {$cnt==1} continue
    foreach {site file latitude longitude notes} [split $line ,] break
    convert  $site $file $latitude $longitude $notes
}


puts $::sitesfp "</entries>"
close $::sitesfp

foreach file [glob -nocomplain  /Users/jeffmc/kbocc/rawfiles2/*] {
    file rename $file [file join /Users/jeffmc/kbocc/rawfiles [file tail $file]]
}
