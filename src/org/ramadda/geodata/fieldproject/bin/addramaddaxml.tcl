source [file join [file dirname [info script]] lib.tcl]


set ::sitexml [file join $::resourcedir siteramadda.xml]
set ::visitxml [file join $::resourcedir visitramadda.xml]

foreach projectdir $argv {
    copyFile  $::sitexml [file  join $projectdir .ramadda.xml]
    foreach sitedir [glob -nocomplain [file join $projectdir *]] {
	copyFile  $::visitxml [file join $sitedir .ramadda.xml]
    }
}

