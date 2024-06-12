 package require csv


##@(https://en.wikipedia.org/wiki/Benton_County,_Arkansas imageWidth=100 addHeader=false ignoreError=true)

proc biaIndex {key} {
    for {set i 0} {$i<[llength $::key]} {incr i} {
	if {[lindex $::key $i] == $key} {
	    return $i
	}
    }
    return -1

}
proc bia {line key} {
    set idx [biaIndex $key]
    lindex $line $idx
}

proc biaFind {name} {
    foreach line $::bia {
	set tribe [bia $line tribe]
	if {[regexp -nocase $name $tribe]} {
#	    puts stderr "GOT: $name $tribe"
	    return $line
	}
    }
#    puts stderr "NO MATCH: $name"
    return {}
}

set fp [open template.xml r]
set ::template [read $fp]
close $fp

proc parseCsvFile {filename} {
    set fp [open $filename r]
    set csvData [read $fp]
    close $fp

    set ::bia {}
    set cnt 0
    foreach row [split [string trim $csvData] \n] {
	set line  [csv::split $row]
	if {$cnt==0} {
	    set idx 0
	    foreach key $line {
		set key [string tolower $key]
		incr idx
	    }
	    set ::key $line
	} else {
	    lappend ::bia $line
	}
	incr cnt
    }
    set ::bia
}

# Main script execution
set parsedData [parseCsvFile BIA_Tribal_Leadership_Directory.csv]
set ::list {kootenai colville jamestown elwha samish ronde siletz yakama gamble warm northwestern kasaan summit {duck valley} siuslaw umatilla}


proc cleanTribe {tribe} {
    set text [string tolower $tribe]
    foreach v $::list {
	if {[regexp $v  $text]} {
	    set text $v
#	    puts "XXX $v $tribe"
	    break
	}
    }

    regsub -all -nocase tribes $text { } text
    regsub -all -nocase tribe $text { } text
    regsub -all -nocase nation $text { } text    
    set text [string trim [string tolower $text]]
}     

proc geojson {tribe file} {
    if {[file exists $file] && [file size $file]>100} {
#	puts "ok: $tribe"
	return 1
    }

    set text [cleanTribe $tribe]
    regsub -all { } $text {+} text
    set url "https://ramadda.org/repository/entry/show?entryid=c6969703-c500-49ad-9fe5-ee14c19a5b2d&output=geojsonfilter&geojson_property=name&geojson_filter=Subset&geojson_value=(?i).*$text.*"
    catch {exec wget -O $file $url}
    if {[file size $file]<100} {
	puts "not ok: $tribe text:$text"
	file delete $file
	return 0
    }
#    puts "ok: $tribe"
    return 1
}

puts "<entries>"
proc cdata {s} {
    return "<!\[CDATA\[$s\]\]>"
}


proc attr {n v} {
    return " $n=\"$v\" "
}

proc tribe {state tribe wiki lat lon} {
    set key [cleanTribe $tribe]
    set biaInfo [biaFind $key]

#    puts "$tribe $lat $lon"
    set file [string tolower $tribe]
    regsub -all { } $file _ file
    regsub -all {[\(\)]+} $file {} file    
    set id $file
    regsub -all {'} $id {} id
    set file "$file.geojson"
    puts "<entry [attr type tribal_datahub] [attr name $tribe] [attr id $id]";
    set desc "+callout-info\nThis is an example data hub for the $tribe\n-callout\n"
    append desc "@($wiki imageWidth=100 addHeader=false ignoreError=true)\n"

    set metadata ""
    set attrs ""    
    if {[llength $biaInfo]>0} {
	set lat [bia $biaInfo Y]
	set lon [bia $biaInfo X]	
	set url [string trim [bia $biaInfo website]]
	if {$url!=""} {
	    append metadata "<metadata [attr type content.url]>"
	    append metadata "<attr [attr fileid $file] [attr index 1] [attr encoded false]>[cdata $url]</attr>"
	    append metadata "<attr [attr fileid $file] [attr index 2] [attr encoded false]>$tribe Website</attr>"	    
	    append metadata "</metadata>"
	}
    }


    set bounds ""
    set north ""
    set west ""
    set south ""
    set east ""
    if {[file exists $file]} {
	set bounds [exec java org.ramadda.util.geo.GeoJson -bounds $file]
	regexp {north:(.*) +west:(.*) +south:(.*) +east:(.*)} $bounds match north west south east
#	set lat [expr $south+($north-$south)/2]
#	set lon [expr $west+($east-$west)/2]
	append metadata "<metadata [attr type map_displaymap_file] [attr inherited true]>\n"
	append metadata "<attr [attr fileid $file] [attr index 1] [attr encoded false]>[cdata $file]</attr>\n"
	append metadata "</metadata>\n"
	append attrs "[attr north $north] [attr west $west] [attr south  $south] [attr east $east]"
    } else {
	append attrs " [attr latitude $lat] [attr longitude $lon] "
    }

    puts $attrs
    puts ">"

    puts $metadata
    puts "<state>[cdata $state]</state>\n"
    puts "<metadata [attr type content.alias]><attr [attr index 1] [attr encoded false]><!\[CDATA\[tribe_$id\]\]></attr></metadata>"
    set children ""
    set template $::template
    set dataid ${id}_data
    append children "<entry [attr type group] [attr name Data] [attr parent $id] [attr id ${dataid}]/>\n"
    regsub -all {%tribe%} $template $tribe template
    regsub -all {%latitude%} $template $lat template
    regsub -all {%longitude%} $template $lon template
    regsub -all {%parent%} $template $dataid template            
    append children $template

    append children "<entry [attr type type_document_collection] [attr name Documents] [attr parent $id]/>\n"    
    if {$lat!="N/A"} {
#	set cy [expr $south+($north-$south)/2]
#	set cx [expr $west+($east-$west)/2]
	set cy $lat
	set cx $lon
	set d 1
	set bbox "[expr $cy+$d],[expr $cx-$d],[expr $cy-$d],[expr $cx+$d]"
	append children "<entry [attr type type_virtual] [attr name {Local Weather Stations}] [attr parent ${dataid}]>\n"
	append children "<description>[cdata {{{map}}}]</description>"
	append children "<entry_ids [attr encoded false]><!\[CDATA\[search.type=type_awc_metar\nsearch.bbox=$bbox\nsearch\]\]></entry_ids>\n"
	append children "</entry>\n"

	append children "<entry [attr type type_virtual] [attr name {USGS Stream Gauges}] [attr parent ${dataid}]>\n"
	append children "<description>[cdata {{{map}}}]</description>"
	append children "<entry_ids [attr encoded false]><!\[CDATA\[search.type=type_usgs_gauge\nsearch.bbox=$bbox\nsearch\]\]></entry_ids>\n"
	append children "</entry>\n"


	set d 2
	set bbox "[expr $cy+$d],[expr $cx-$d],[expr $cy-$d],[expr $cx+$d]"
	append children "<entry [attr type type_virtual] [attr name {NOAA Sea-Level Trends}] [attr parent ${dataid}]>\n"
	append children "<description>[cdata {{{map}}}]</description>"
	append children "<entry_ids [attr encoded false]><!\[CDATA\[search.type=type_noaa_tides_trend\nsearch.bbox=$bbox\nsearch\]\]></entry_ids>\n"
	append children "</entry>\n"

    }

    if {$lat!="N/A"} {
	set name "$tribe Current Conditions"
	append children "<entry [attr type nwsfeed] [attr name $name] [attr id ${id}_nws] [attr parent ${dataid}] [attr latitude $lat] [attr longitude $lon]/>\n"
	set name "$tribe Historic Weather"
	append children "<entry [attr type type_daymet] [attr name $name] [attr parent ${dataid}] [attr latitude $lat] [attr longitude $lon]/>\n"	
	append desc ":heading Current Conditions\n"
	append desc "{{nws.hazards entry=\"${id}_nws\" addHeader=\"false\"}}\n"
	append desc ":br\n"
	append desc "{{nws.current [attr entry ${id}_nws] [attr addHeader false] [attr orientation vertical]}}\n";
	append desc ":br\n"
	append desc "{{nws.forecast [attr entry ${id}_nws] showHeader=false showDetails=false count=1000}}\n";

	
    }
    puts  "<description><!\[CDATA\[$desc\]\]></description>\n"
    puts "</entry>"    
    puts $children
#    geojson $tribe $file
}

source tribes.tcl
puts "</entries>"
