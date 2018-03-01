package require http


set ::states_csv ../../repository/resources/geo/states.csv

set ::baseDir "[file dirname [file dirname $argv0]]"
set ::resourceDir "$::baseDir/resources"
set ::stateArgs $argv

set zz "00"
set ::stateNames($zz) "All Survey States"
foreach line  [split [read [open $::states_csv r]] "\n"] {
    set line [string trim $line]
    if {$line == ""}  continue;
    foreach {abbr name id lat lon} [split $line ,] break
    set loc [list [string trim $lat] [string trim $lon]]
    foreach key [list $name $id $abbr] {
        set ::stateLocations($key) $loc
        set ::stateLocations([string toupper $key]) $loc
        set ::stateLocations([string tolower $key]) $loc
    }
    set ::stateNames($id) $name
}




proc readAndSplit {file tuple} {
    set list [list]
    foreach line  [split [read [open $file r]] "\n"] {
        set line [string trim $line]
        if {$line == ""} continue;
        if {[regexp {.*#.*} $line]} continue;
        if {$tuple} {
            lappend list [split $line =]
        } else {
            lappend list [lindex [split $line =] 0]
        }
    }
    set list
} 


proc processReport {source} {

    set metadataUrl "http://nass-api.azurewebsites.net/api/get_dependent_param_values?distinctParams=state_name&freq_desc=ANNUAL&source_desc=${source}&agg_level_desc=STATE&commodity_desc="

    if {$source == "SURVEY"} {
        set sourceName Survey
    } else {
        set sourceName Census
    }




    set commoditys [readAndSplit  $::resourceDir/nass_commodities.txt 1]
    set states [list]
    foreach tuple [readAndSplit  $::resourceDir/nass_states.txt 1]  {
        foreach {id name} $tuple break
        lappend states $id
        set ::stateNames($id) $name
    }
    set xml "<entries>\n"


    set statesToUse $states


    if {[llength $::stateArgs]>0} {
        set statesToUse  $::stateArgs
    }






    foreach commodity $commoditys {
        foreach {cid cname} $commodity break;
        set url [string trim $metadataUrl]
        set metadata_dir $::baseDir/nass_metadata_survey
        if {$source=="CENSUS"} {
            set metadata_dir $::baseDir/nass_metadata_census
        }
        set filename $metadata_dir/${cid}_states.json
        if {![file exists $filename]} {
            set tmp $cid
            regsub -all {\&} $tmp {&amp;} tmp
            regsub -all { } $tmp {%20} tmp
            append url $tmp
            puts stderr "fetching metadata for $cname"
            set tok [::http::geturl $url -timeout 30000]
            set status [::http::status $tok]
            if {$status!="ok"} {
                puts  stderr  "bad url"
                exit
            }
            set c [::http::data $tok]
            set fp [open $filename w]
            puts $fp $c
            close $fp
        } else {
            set fp [open $filename r]
            set c [read $fp]
            close $fp
        }

        set ::validStates($cid) [list]
        #    puts "ID: $cid"
        if {[regexp {"Values"\s*:\s*\[([^\]]+)\].*} $c match v]} {
            regsub  -all {\"} $v {} v
            set newStates [list]
            foreach state [split $v ,] {
                lappend newStates  $state 
            }
            #        puts "\t $newStates"
            set ::validStates($cid) $newStates
        }
    }


    foreach state $statesToUse {
        set sname $::stateNames($state)

        set location ""
        set lat ""
        set lon ""
        foreach testName [list $sname [string tolower $sname]] {
            if [info exists ::stateLocations($testName)] {
                foreach {lat lon}  $::stateLocations($testName) break
            } 
        }

        if {$lat == ""} {
            puts stderr "No location: $sname"
        }
        set attrs ""
        if {$lat!=""} {
            append attrs " latitude=\"$lat\"  longitude=\"$lon\"  "
        }
        append xml "<entry name=\"USDA NASS $sourceName Reports - $sname\" type=\"group\" id=\"$state\" $attrs />\n"

        set cnt 0
        foreach commodity $commoditys {
            incr cnt
            ##        if {$cnt>100} break    
            foreach {cid cname} $commodity break
            if {$cid!="US TOTAL"} {
                set valid [lsearch $::validStates($cid) $state]
                if {$valid<0} {
                    #                puts stderr "not valid: $state $cname "
                    continue
                }
                #            puts stderr "$state $cname "
            }

            regsub -all {\s+} $sname _ sname
            regsub -all {,} $sname _ sname
            regsub -all {__+} $sname _ sname
            set sname [string tolower $sname]
            set tmp [string tolower ${name}]
            regsub -all { } $tmp _ tmp
            append xml "<entry parent=\"$state\" name=\"\" type=\"type_usda_nass\" $attrs >\n\t<source>$source</source>\n\t<commodity>$cid</commodity>\n\t<state>$state</state>\n</entry>\n"

        }
    }

    append xml "</entries>\n"
    set xml
}



foreach S {SURVEY  CENSUS} {
    set xml [processReport $S]
    set s [string tolower $S]
    set filename nass_${s}_entries.xml
    puts stderr "writing to $filename"
    set ofp [open "$filename" w]
    puts $ofp $xml
    close $ofp
}