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




proc processReport {survey} {

    if {$survey == "crop"} {
        set surveyName Crop
        puts stderr "Generating crop reports"
        set ::entryType usda_arms_crop
        set metadataUrl "http://arms-api.azurewebsites.net/api/Parameters?survey=Crop&report={report}&subject={subject}&distinctParams=fipsStateCode"
    } else {
        set surveyName Finance
        puts stderr "Generating finance reports"
        set ::entryType usda_arms_finance
        set metadataUrl "http://arms-api.azurewebsites.net/api/Parameters?survey=FINANCE&report={report}&subject={subject}&distinctParams=fipsStateCode"
    }




    set reports [readAndSplit  $::resourceDir/${::entryType}_reports.txt 1]
    set subjects [readAndSplit  $::resourceDir/${::entryType}_subjects.txt 1]
    set states [list]
    foreach tuple [readAndSplit  $::resourceDir/${::entryType}_states.txt 1]  {
        foreach {id name} $tuple break
        lappend states $id
    }
    set xml "<entries>\n"


    set statesToUse $states


    if {[llength $::stateArgs]>0} {
        set statesToUse $::stateArgs
    }
    foreach report $reports {
        foreach {rid rname} $report break;
        foreach subject $subjects {
            foreach {sid sname} $subject break;
            set url [string trim $metadataUrl]
            set filename $::baseDir/arms_metadata/${rid}_${sid}_states.json
            if {![file exists $filename]} {
                regsub -all  "\{report\}" $url $rid url
                regsub -all  "\{subject\}" $url $sid url
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

            set ::validStates($rid,$sid) [list]
            if {[regexp {"Keys"\s*:\s*\[([^\]]+)\].*} $c match v]} {
                regsub  -all {\"} $v {} v
                set newStates [list]
                foreach state [split $v ,] {
                    set name $::stateNames($state)
                    lappend newStates  $state 
                }
                set ::validStates($rid,$sid) $newStates
            }
        }
    }


    foreach state $statesToUse {
        set name $::stateNames($state)

        set location ""
        set lat ""
        set lon ""
        if [info exists ::stateLocations($name)] {
            foreach {lat lon}  $::stateLocations($name) break
        } else {
            puts stderr "No location for: $name"
        }

        set attrs ""
        if {$lat!=""} {
            append attrs " latitude=\"$lat\"  longitude=\"$lon\"  "
        }
        append xml "<entry name=\"USDA ARMS $surveyName Reports - $name\" type=\"group\" id=\"$state\" $attrs />\n"

        foreach report $reports {
            foreach {rid rname} $report break
            regsub -all {\s+} $rname _ rname
            regsub -all {,} $rname _ rname
            set rname [string tolower $rname]
            regsub -all {__+} $rname _ rname
            foreach subject $subjects {
                foreach {sid sname} $subject break
                if {$sid!="00"} {
                    set valid [lsearch $::validStates($rid,$sid) $state]
                    if {$valid<0} {
                        #                puts stderr "none: $rname - $sname"
                        continue
                    }
                    #                puts stderr "good: $rname - $sname"
                }

                regsub -all {\s+} $sname _ sname
                regsub -all {,} $sname _ sname
                regsub -all {__+} $sname _ sname
                set sname [string tolower $sname]
                set tmp [string tolower ${name}]
                regsub -all { } $tmp _ tmp
                set filename "arms_${rname}_${sname}_${tmp}.csv"
                
                set attrs ""
                if {$lat!=""} {
                    append attrs " latitude=\"$lat\"  longitude=\"$lon\"  "
                }

                append xml "<entry parent=\"$state\" name=\"\" type=\"type_${::entryType}\" $attrs >\n\t<subject>$sid</subject>\n\t<report>$rid</report>\n\t<state>$state</state>\n</entry>\n"

            }
        }
    }

    append xml "</entries>\n"
    set xml
}



foreach s {crop finance} {
    set ::survey $s
    set xml [processReport $s]
    set filename arms_${s}_entries.xml
    puts stderr "writing to $filename"
    set ofp [open "$filename" w]
    puts $ofp $xml
    close $ofp
}


