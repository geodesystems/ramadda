
package require csv

set ::sitefp [open sites.csv w]
puts $::sitefp "site,latitude,longitude"
set ::entriesfp [open entries.xml w]
puts $::entriesfp "<entries>"
set ::shfp [open create.sh w]
puts -nonewline $::shfp "zip  /Users/jeffmc/plant.zip entries.xml "

set ::cnt 0

proc wentry {v} {
    puts $::entriesfp $v
}
proc col {list index} {
    set v [string trim [lindex $list $index]]
    regsub -all --  _comma_ $v , v
    set v
}
proc readFile {file} {
    set fp [open $file r]
    set contents [string trim [read $fp]]
    close $fp
    set contents
}



proc readMD {file} {
    set contents [readFile $file]
    set lines [split $contents "\n"]
    set keys [csv::split [lindex $lines 0]]
    set values [csv::split [lindex $lines 1]]    
    set list {}
    # Loop through the keys and values lists
    for {set i 0} {$i < [llength $keys]} {incr i} {
	set key [lindex $keys $i]
	set value [lindex $values $i]
	lappend list $key $value

    }
    set list
}

proc cdata {key value} {
    return  "<$key><!\[CDATA\[$value\]\]></$key>"
}

proc num {value} {
    if {$value=="NA"} {return "NaN"}
    return $value
}

proc attr {key value} {
    regsub -all {<} $value {\&lt;} value
    regsub -all {>} $value {\&gt;} value    
    return " $key=\"$value\" "
}

proc process {site type file measurement datatype} {
    #stand:st_age,st_aspect,st_basal_area,st_clay_perc,st_density,st_growth_condition,st_height,st_lai,st_name,st_remarks,st_sand_perc,st_silt_perc,st_soil_depth,st_soil_texture,st_terrain,st_treatment,si_code,st_USDA_soil_texture

    #env:env_leafarea_seasonal,env_netrad,env_plant_watpot,env_ppfd_in,env_precip,env_remarks,env_rh,env_swc_deep_depth,env_swc_shallow_depth,env_sw_in,env_ta,env_time_daylight,env_timestep,env_time_zone,env_vpd,env_ws,si_code

    #site:si_addcontr_email,si_addcontr_firstname,si_addcontr_institution,si_addcontr_lastname,si_code,si_contact_email,si_contact_firstname,si_contact_institution,si_contact_lastname,si_country,si_dendro_network,si_dist_mgmt,si_elev,si_flux_network,si_igbp,si_lat,si_long,si_name,si_paper,si_remarks,is_inside_country,si_mat,si_map,si_biome
    array set env [readMD ${site}_env_md.csv]
    array set stand [readMD ${site}_stand_md.csv]
    array set md [readMD ${site}_site_md.csv]
    set datafile "${site}_${file}.csv"
    set file_size [file size $datafile]
    if {$file_size>4000000} {
#	puts "big $datafile $file_size"
	return;
    }
    incr ::cnt
    if {$::cnt >10} return
    puts stderr "processing site:$site #$::cnt"
    regsub -all {_} $site  {-} _site
    set name $md(si_name)
    regsub -all {_} $name { }  name
    set name "$name - $_site - $datatype"
    set country $md(si_country)
    if {[info exists ::countries($country)]} {
	set country $::countries($country)
    } else {
	puts "no country: $country"
    }
    set biome $md(si_biome)
    set institution $md(si_contact_institution)
    regsub -all {\n} $institution { } institution 
    set remarks $md(si_remarks)
    if {$remarks=="NA"} {set remarks ""}
    set lat $md(si_lat)
    set lon $md(si_long)
    puts $::sitefp "$site,$lat,$lon"
    flush $::sitefp
    puts -nonewline $::shfp " $datafile "
    wentry "<entry  [attr name $name] [attr site_id $site] [attr country $country] [attr biome $biome]  [attr type $type] [attr altitude [num $md(si_elev)]] [attr latitude $lat] [attr longitude $lon] [attr file $datafile] "
    wentry [attr measurement $measurement]
    wentry [attr datatype $datatype]
    wentry [attr stand_terrain $stand(st_terrain)]
    wentry [attr stand_density [num $stand(st_density)]]    
    wentry [attr stand_age [num $stand(st_age)]]        
    wentry ">"
    wentry [cdata institution $institution]
    wentry [cdata description $remarks]
    wentry "</entry>"
    flush $::entriesfp
}



set script_dir [file dirname [info script]]
set contents [readFile $script_dir/countries.csv]
set tmp {}
foreach line  [split $contents "\n"] {
    foreach {key value} [split $line ,] break
    lappend tmp $key $value
}
array set ::countries $tmp

set sites {}
foreach file [glob -nocomplain "*_site_md.csv"] {
    regexp {^(.*)_site_md.*} $file match site
    lappend sites $site
}
set entrytype type_sapflux
set ::typefp [open /Users/jeffmc/planttypes.xml w]
puts $::typefp "<types supercategory=\"Geoscience\" category=\"Plant Data\">"
puts $::typefp "<type name=\"$entrytype\" super=\"point_text_csv\" description=\"Plantflux Test\" handler=\"org.ramadda.data.services.PointTypeHandler\">"
puts $::typefp "<property name=\"icon\" value=\"/plant/tree.png\"/>"
puts $::typefp "<property name=\"form.area.show\" value=\"false\"/>"
puts $::typefp "<property name=\"record.file.class\" value=\"org.ramadda.data.point.text.CsvFile\"/>"
puts $::typefp {<column  name="measurement"  type="enumeration" label="Measurement" group="Data" cansearch="true" values="plant:Per Plant,leaf:Per Leaf Area,sapwood:Per Sapwood Area"/>}
puts $::typefp {<column  name="datatype"  type="enumeration" label="Data Type" cansearch="true"/>}
puts $::typefp {<column  name="site_id"  type="string" label="Site ID" cansearch="true" group="Site Information"/>}
puts $::typefp {<column  name="country"  type="enumeration" label="Country" cansearch="true" />}
puts $::typefp {<column  name="institution"  type="enumeration" label="Institution" cansearch="true"/>}
puts $::typefp {<column  name="biome"  type="enumeration" label="Biome" cansearch="true"/>}
puts $::typefp {<column  name="stand_age"  type="double" label="Stand Age" cansearch="true" group="Stand Information"/>}
puts $::typefp {<column  name="stand_density"  type="double" label="Stand Density" cansearch="true"/>}
puts $::typefp {<column  name="stand_terrain"  type="enumeration" label="Stand Terrain" cansearch="true"/>}
puts $::typefp {
<property name="record.properties"> 
csvcommands1=-addheader, timestamp.type date   timestamp.format {yyyy-MM-dd'T'HH:mm:ss'Z'}
</property>
}

puts $::typefp {
<property name="bubble">
<![CDATA[ 
+section title={{name}}
{{display_linechart fields=#3}}
{{display_linechart fields=#4}}
-section
]]></property>
}
puts $::typefp {
<embedwiki>
<![CDATA[ 
+section title={{name}}
{{display_linechart fields=#3}}
{{display_linechart fields=#4}}
-section
]]></embedwiki>
}

puts $::typefp {
<wiki><![CDATA[
+section title={{name}}
+row
+col-8
{{display_linechart fields=#3}} 
{{display_linechart fields=#4}} 
{{display_linechart fields=#5}} 
-col
+col-4
{{mapentry}}
-col
-row
{{display_download}}
{{display_table}}
:heading Information
{{information details=true}}
-section
]]></wiki>
}


puts $::typefp "</type>"
puts $::typefp "</types>"
close $::typefp

regexp {.*/([^/]+)$} [exec pwd] match measurement


foreach site $sites {process $site $entrytype sapf_data $measurement "Sap Flow"}
set ::cnt 0
foreach site $sites {process $site $entrytype env_data $measurement "Environmental"} 

close $::sitefp
puts $::entriesfp "</entries>"
flush $::entriesfp
close $::entriesfp
puts  $::shfp ""
close $::shfp
