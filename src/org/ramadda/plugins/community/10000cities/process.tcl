set ::doTest 0

package require json

if  [llength $argv] {
    set ::doTest 1
}

set ::scriptDir [file dirname [info script]]
source ~/bin/lib.tcl
source ~/bin/utils.tcl
source $::scriptDir/timezones.tcl

set ::countyCnt 0
set ::stateCnt 0

set ::baseDir generated/united_states
set ::statsDir cache/stats
file mkdir $::baseDir
file mkdir $::statsDir
proc getStateDir {{abbr {}}} {
    if {$abbr == ""} {
        return [file join $::baseDir states]
    }
    set abbr [string tolower $abbr]
    set dir [file join $::baseDir states $abbr]
}

proc getCountyDir {state county} {
    return [file  join [getStateDir $state] counties $county]
}

proc getPlaceDir {state county} {
    return [file  join [getStateDir $state] communities $county]
}


set ::censusDesc "<wiki>\n+section # title=\"\{\{name\}\}\"\n\{\{treeview  width=\"-100\"  message=\"No census data available\" height=\"500\"\}\}\n-section\n+section # label=\"The Data\"\n\{\{tree message=\"No census data available\" \}\}\n-section\n"


proc makeGroup {dir name {attrs {}} {desc {}} {inner {}}  } {
    if {[lsearch $attrs "type"]<0} {
        lappend attrs type;
        lappend attrs group;
    }
    if {$desc !=""} {
        set desc [Util::cdataTag description "" $desc]
    }
    lappend attrs name
    lappend attrs $name
    file mkdir $dir
    writeFile $dir/.this.ramadda.xml [xmlTag entry "$inner $desc" $attrs]
}


proc makeEntry {dir name {attrs {}} {desc {}} {inner {}}} {
    if {[lsearch $attrs "type"]<0} {
        lappend attrs type;
        lappend attrs group;
    }
    if {$desc !=""} {
        set desc [Util::cdataTag description "" $desc]
    }
    append desc $inner
    lappend attrs name
    lappend attrs $name
    regsub -all {/} $name "_" name
    writeFile $dir/$name.entry.xml [xmlTag entry "$desc" $attrs]
}

proc state {abbr name fips lat lon pop} {
    if {$::doTest} {
	if {$abbr !="CO"} return;
        if {[expr rand()<0.90]} {
#            return
        }
        if {[incr ::stateCnt]>1} return;
    }

    set abbr [string tolower $abbr]
    set nameLower [string tolower $name]
    regsub -all { } $nameLower {_} nameLower

    set tz [getTimezone $nameLower]
    set ::states($abbr) $fips
    set ::states($abbr,name) $name
    set ::states($abbr,nameLower) $nameLower
    set stateDir [getStateDir $abbr]
#    set md [xmlTag metadata "" [list type content.alias encoded false attr1 "https://$abbr.10000cities.org"]]
#    append md [xmlTag metadata "" [list type content.alias encoded false attr1 "https://$nameLower.10000cities.org"]]
    set md [xmlTag metadata "" [list type content.alias encoded false attr1 "$abbr.10000cities.org"]]
    append md [xmlTag metadata "" [list type content.alias encoded false attr1 "$nameLower.10000cities.org"]]
    append md [xmlTag metadata "" [list type community_snapshot encoded false attr1 $fips attr2 $pop ]]
    append md [xmlTag metadata "" [list type content.timezone encoded false attr1 $tz inherited true]]




    set desc "\n:heading Welcome to the 10000 Cities Data Hub for the state of $name\n<br>Below you will find state-level data and individual data hubs for each county.\n<br>"
    makeGroup $stateDir $name [list type community_datahub latitude $lat longitude $lon] "$desc" $md

    set countyDesc "<wiki>\n+section # title=\"{{name}}\"\n:blurb-blue County data hubs for $name\n{{map  sort=\"name\"  width=\"100%\" listentries=\"true\" height=\"500\"}}\n-section\n+section # label=\"The Counties\"\n{{tree message=\"\" sort=\"name\" details=\"false\"}}\n-section\n"

#    set md [xmlTag metadata "" [list type content.alias encoded false attr1 "https://counties.$abbr.10000cities.org"]]
    set md [xmlTag metadata "" [list type content.alias encoded false attr1 "counties.$abbr.10000cities.org"]]

    makeGroup  $stateDir/counties Counties [] $countyDesc "$md $::sortMD"
    makeGroup $stateDir/communities  Communities
    makeGroup $stateDir/data  "$name Data"

    makeGroup  $stateDir/data/census "$name Census Snapshot" [list latitude $lat longitude $lon]  $::censusDesc
    addCensus $stateDir/data/census $name county {} state $fips {} {} $lat $lon



}

set ::census {
<entries>
  <entry name="{entry.name} Census Snapshot - {label}" type="{type}"  lat="{lat}" lon="{lon}" >
    <for_type encoded="false"><![CDATA[{for_type}]]></for_type>
    <for_value encoded="false"><![CDATA[{for_value}]]></for_value>
    <in_type1 encoded="false"><![CDATA[{type1}]]></in_type1>
    <in_value1 encoded="false"><![CDATA[{value1}]]></in_value1>
    <in_type2 encoded="false"><![CDATA[{type2}]]></in_type2>
    <in_value2 encoded="false"><![CDATA[{value2}]]></in_value2>
    <source_url encoded="false"><![CDATA[]]></source_url>
    <include_locales encoded="false"><![CDATA[false]]></include_locales>
  </entry>
</entries>
}


proc addCensus {dir name for_type for_value type1 value1 type2 value2 lat lon} {

#    foreach f [glob -nocomplain "$::scriptDir/templates/*.entry.xml"]  {}

    set types { 
        {"Age and Gender" type_census_age} 
        {"Commuting" type_census_commuting} 
        {"Housing" type_census_housing} 
        {"Poverty" type_census_poverty} 
        {"Children" type_census_children} 
        {"Education" type_census_education} 
        {"Nationality" type_census_nationality} 
        {"Race" type_census_race} 
    }

    foreach pair $types {
        foreach {label type} $pair break
        set c [string trim $::census]
        regsub -all "{label}" $c $label  c
        regsub -all "{type}" $c $type  c
        regsub -all "{entry.name}" $c $name  c
        regsub -all "{for_type}" $c $for_type  c
        regsub -all "{for_value}" $c $for_value  c
        regsub -all "{type1}" $c $type1  c
        regsub -all "{value1}" $c $value1  c
        regsub -all "{type2}" $c $type2  c
        regsub -all "{value2}" $c $value2  c
        regsub -all "{lat}" $c $lat  c
        regsub -all "{lon}" $c $lon  c
##        set fileName [file tail $f]
        set fileName $type.entry.xml
        writeFile $dir/$fileName $c
    }
}




#proc city_county {state geoid ansi name pop hu aland awater aland_sqmi awater_sqmi lat lon} {}


proc county {state geoid ansi name pop hu aland awater aland_sqmi awater_sqmi lat lon} {

    if {0} {
        set statsFile [file join $::statsDir/$geoid.json]
        if {![file exists $statsFile]} {
            set json [Util::fetchUrl https://www.datasciencetoolkit.org/coordinates2statistics/${lat}%2c${lon}]
            Util::writeFile $statsFile $json
        } else {
            set json [Util::readFile $statsFile]
        }
        set stats [::json::json2dict $json]
    }

    if {$::doTest} {
        if {[expr rand()<0.90]} {
#            return
        }
    }

    set state [string tolower $state]
    if {![info exists  ::states($state)]} {
#        puts "no state: $name"
        return
    }
    
    if {$::doTest} {
        if {[incr ::countyCnt]>1} return;
    }


    set stateUpper [string toupper $state]
    set label "$name $stateUpper Data Hub"

    set fips $::states($state)
    set dir [getCountyDir $state $name]
    if {![regexp {(\d\d)(\d\d\d)} $geoid match stateId countyId]} {
        puts "NO match: $state - $geoid - $name"
        exit
    }
    set countyLower [string tolower $name]
    regsub -all {\s+} $countyLower {_} countyLower

    regsub -all {[^a-zA-Z]+} $countyLower {} countyLower
    set host "$countyLower.$::states($state,nameLower).10000cities.org"


#    set desc "\n:heading Welcome to the 10000 Cities Data Hub for $name $::states($state,name)\n"
    set desc ""
    append desc "{{10000cities.welcome}}\n"

    regsub -all { } $name {_} _name
    set stateName $::states($state,name)

    set countyId "${_name}_$stateName"
    set sunriseId "sunrisesunset_$countyId"
    set daymetId "daymet_$countyId"
    set nwsId "nwsfeed_$countyId"

    set wikipediaLink "https://en.wikipedia.org/wiki/$_name,_$stateName"
    append desc ":vspace 10px\n"
    append desc "@($wikipediaLink imageWidth=100 addHeader=false ignoreError=true)\n"
    append desc ":vspace 10px\n"
    append desc ":heading Current Conditions\n"
    append desc "{{sunrisesunset entry=\"$sunriseId\"}}\n"
    append desc "{{nws.hazards entry=\"$nwsId\" addHeader=\"false\"}}\n"
    append desc ":br\n"
    append desc "{{nws.current entry=\"$nwsId\" addHeader=\"false\" orientation=\"vertical\"}}\n"



##    append desc "Below are folders for data and community resources.\n"


#    set md [xmlTag metadata "" [list type content.alias encoded false attr1 "https://$host"]]
    set md [xmlTag metadata "" [list type content.alias encoded false attr1 "$host"]]

    #state geoid ansi name pop hu aland awater


    append md [xmlTag metadata "" [list type community_snapshot encoded false attr1 $geoid attr2 $pop attr3 $aland attr4 $awater]]
    append md [xmlTag metadata "" [list type property encoded false attr1 "state" attr2 $state]]


    makeGroup  $dir $label  [list  type community_datahub latitude $lat longitude $lon] $desc $md
#    makeEntry  $dir Documents 
#   makeGroup  $dir/blogs Blogs
    makeGroup  $dir/news "$name Data News" [list type weblog] 
    makeEntry  $dir/news "First Post" [list type blogentry] "First post to the $name Data News blog"
    makeGroup  $dir/wiki "Community Data Wiki"
    makeEntry  $dir/wiki  "First Wiki Page" [list type wikipage] "==First wiki page=="

#    makeGroup  $dir/links "Links" [] "<wiki>\n+section title={{name}}\n{{links linkresource=\"true\" message=\"\" }}\n-section\n" 
#    makeEntry $dir/links "Wikipedia Page" [list type link url $wikipediaLink ] "" 

#    makeEntry $dir/links "Weather Page" [list type link ] ""  [Util::cdataTag url "" "https://forecast.weather.gov/MapClick.php?lat=$lat&lon=$lon"]

#    makeEntry  $dir "Community Resources" [] ""
    makeGroup  $dir/data Data 
    set off 2
    set bounds "[expr $lat+$off],[expr $lon-$off],[expr $lat-$off],[expr $lon+$off]"



    set desc {<wiki>
        +section title="{{name}}"
        {{map listentries="true" width="100%" height="500"}}
-section
----
+section label="The Stations"
        {{tree message="No local weather stations available"}}
-section
    }



    set inner [Util::cdataTag entry_ids "" "search.type=type_awc_metar\nsearch.bbox=$bounds\nsearch\n"]
    makeEntry  $dir/data "Local Weather Stations"  [list type type_virtual] $desc $inner
    makeEntry  $dir/data "$name Sunrise/Sunset"  [list id $sunriseId type sunrisesunset latitude $lat longitude $lon] "" ""
    makeEntry  $dir/data "$name Historic Weather"  [list id $daymetId type type_daymet latitude $lat longitude $lon] "" ""
    makeEntry  $dir/data "$name Current Weather"  [list id $nwsId type nwsfeed latitude $lat longitude $lon] "" ""
    

    if {[regexp {(ak|co|id|mt|or|ut|wa|wy)} $state]} {
    set desc {<wiki>
        +section title="{{name}}"
        {{map listentries="true" width="100%" height="500"}}
-section
----
+section label="The Stations"
        {{tree message="No local Snotel stations available"}}
-section
    }

    set inner [Util::cdataTag entry_ids "" "search.type=type_point_snotel\nsearch.bbox=$bounds\nsearch\n"]
    makeEntry  $dir/data "Local SNOTEL Stations"  [list type type_virtual] $desc $inner
    }


##    set md [xmlTag metadata "" [list type content.alias encoded false attr1 "https://census.$host"]]
    set md ""
    makeGroup  $dir/census "$name Census Snapshot" [list latitude $lat longitude $lon]  $::censusDesc $md
    addCensus $dir/census $name tract {} state $stateId county $countyId $lat $lon
}


proc city_county {city county state} {
}

proc place {state geoid ansi name lsad func pop hu aland awater aland_sqmi awater_sqmi lat lon} {
    if {$::doTest} {
        if {[expr rand()<0.95]} {
            return
        }
    }

    set state [string tolower $state]
    if {![info exists  ::states($state)]} {
##        puts "place - no state:$state: for place  $name"
        return
    }


    set fips $::states($state)
    set dir [getPlaceDir $state $name]
    if {![regexp {(\d\d)(\d\d\d)} $geoid match stateId placeId]} {
        puts "NO match: $state - $geoid - $name"
        exit
    }
    file mkdir $dir
    writeFile $dir/.this.ramadda.xml "<entry name=\"$name\"  type=\"community_datahub\" latitude=\"$lat\" longitude=\"$lon\"  />\n"

}



file mkdir [getStateDir]
set ::sortMD [xmlTag metadata "" [list type content.sort encoded false attr1 "name"]]
makeGroup  [getStateDir] "States"   [list] "<wiki>+section title={{name}}\n{{tree  message=\"\" sort=\"name\"  details=\"false\"}}\n-section" $::sortMD


set usdesc "\n:heading Welcome to the 10000 Cities Data Hub for the United States.\n<br>Below you will find data at the national level as well as data hubs for each state."
#set md [xmlTag metadata "" [list type content.alias encoded false attr1 "https://usa.10000cities.org"]]
set md [xmlTag metadata "" [list type content.alias encoded false attr1 "usa.10000cities.org"]]
makeGroup  $::baseDir "United States"   [list  type community_datahub north 68.9594424 west -165.4277301 east -56.0917926 south 10.2745632] $usdesc $md
set dir [file join $::baseDir data]
makeGroup  $dir "United States Data"  {US Data}
makeGroup  $dir/census "United States Census Snapshot"  [list] $::censusDesc
addCensus $dir/census "US"  state {} {} {} {} {}  40 -95




puts "Processing states"
source $::scriptDir/dict.states.tcl
puts "Processing counties"
source $::scriptDir/counties.tcl
source $::scriptDir/city_county.tcl

#puts "Processing places"
#source $::scriptDir/places.tcl



