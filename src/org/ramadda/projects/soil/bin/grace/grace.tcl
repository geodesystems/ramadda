package require Tcl 8.6

##source the lib
source $env(RAMADDA_ROOT)/bin/ramadda.tcl


set ::cnt 0
set ::sites {}
array set ::sitesMap {}
set ::exportFiles {}


array set ::names {
    measargenes {Measure Genes}
    measbiomasscho  {Measure Biomass Cho}
    measbiomassenergy  {Measure Biomass Energy} 
    measbiomassminan  {Measure Biomass Minan}  
    measgasnutrientloss  {Measure Gas Nutrient Loss}
    measghgflux  {Measure GHG Flux}
    measgrazingplants  {Measure Grazing Plants}
    measharvestfraction  {Measure Harvest Fraction}
    measnutreff  {Measure Nutrient Reff}
    measresiduemgnt  {Measure Residue Management}
    meassoilbiol  {Measure Soil Biology}
    meassoilchem  {Measure Soil Chemistry}
    meassoilcover  {Measure Soil Cover}
    meassoilphys  {Measure Soil Phys}
    measwaterqualityarea  {Measure Water Quality Area}
    measwaterqualityconc  {Measure Water Quality Conc}
    measwinderosionarea  {Measure Wind Erosion Area}
    measyieldnutuptake  {Measure Yield Nutrient Uptake}
    mgtamendments  {Management Amendments}
    mgtgrazing {Management Grazing}
    mgtgrowthstages {Management Growth Stages}
    mgtplanting {Management Planting}
    mgtresidue {Management Residue}
    mgttillage {Management Tillage}} 

set ::xml "<entries>\n"

oo::class create Base {
    variable props
    # Constructor to initialize the citations list as empty
    constructor {} {
	array set props {}
    }

    
    method setProp {key value} {
	set props($key) $value
    }
    method getProp {key} {
	return $props($key)
    }    
}



oo::class create Site {
    superclass Base
    variable id
    variable citations
    variable persons
    variable treatments
    variable units        
    constructor {siteId} {
	set id $siteId
        set citations {}
        set persons {}
        set treatments {}
        set units {}				
    }

    method addPerson {person} {
        lappend persons $person
    }

    method addCitation {citation} {
        lappend citations $citation
    }
    method addTreatment {treatment} {
        lappend treatments $treatment
    }    

    method addUnit {unit} {
        lappend units $unit
    }    

    method process {} {
#	if {[incr ::cnt]>5} return
	set name "$id - [my getProp site_id_descriptor]"
	set start [my getProp start_date]
	set end [my getProp end_date]	

	set entryid [string tolower $id]
	append ::xml [openEntry type_geo_site $entryid {} $name]
	if {$start!=""} {
	    append ::xml [col fromdate $start]
	}
	if {$end!=""} {
	    append ::xml [col todate $end]
	}
	set lat [my getProp latitude_decimal_deg]
	set lon [my getProp longitude_decimal_deg]    
	append ::xml [col latitude $lat]
	append ::xml [col longitude $lon]
	append ::xml [col altitude [my getProp elevation_m]]		
	append ::xml [col description [my getProp site_history]]
	set	alias "grace-[string tolower $id]"
	regsub { } $alias _ alias
	append ::xml [mtd1 content.alias $alias]
	append ::xml [mtd5 content.address "" [my getProp city] [my getProp state_province] [my getProp postal_code] [my getProp country]   ]

#country city postal_code
	foreach prop {siteid research_unit project_name experiment_name funding_source mlra native_veg duration_of_study} {
	    set label $prop
	    regsub -all _ $label { } label
	    set label [toProperCase $label]
	    set v [string trim [my getProp $prop]]
	    if {$v!=""} {
		append ::xml [mtd2 property $label $v]
	    }
	}
	foreach person $persons {
	    set name "[$person getProp first_name] [$person getProp middle_name] [$person getProp last_name] [$person getProp suffix]"
	    regsub -all {  +} $name { } name
	    set name [string trim $name]
	    append ::xml [mtdN soil_grace_person $name [$person getProp role_in_study]  [$person getProp primary_contact] [$person getProp department] [$person getProp organization] [$person getProp profession] [$person getProp email] [$person getProp telephone] [$person getProp web_site]  [$person getProp note] ]

	}

	foreach c $citations {
	    append ::xml [mtdN content.fullcitation  [$c getProp title] [$c getProp date_published] [$c getProp type] [$c getProp author] [$c getProp correspond_author] [$c getProp identifier] [$c getProp description] [$c getProp citation] [$c getProp url]] 

	}
	
	append ::xml [closeEntry] 
	foreach data [array names ::names] {
#	    puts "$data=$::names($data)"

	    set path "split/$data/${data}_${entryid}.csv"
	    if {![file exists $path]} {
		continue;
	    }
	    lappend ::exportFiles $path
	    set name "[toProperCase $id] - [toProperCase $::names($data)]"
	    append ::xml [openEntry type_soil_grace_measurement {} $entryid $name file $path latitude $lat longitude $lon]
	    append ::xml [col datatype $data]
	    append ::xml [closeEntry] 
	}


    }
}


set ::overviewArgs {siteid site_id_descriptor research_unit project_name experiment_name funding_source start_date end_date duration_of_study}
proc overview $::overviewArgs {
    set mySite [Site new $siteid]
    set ::sitesMap($siteid) $mySite
    lappend ::sites $mySite

    foreach attr $::overviewArgs {
	$mySite setProp $attr [set $attr]
    }
}


set ::fieldsitesArgs {siteid date mlra field_id country state_province county city postal_code latitude_decimal_deg longitude_decimal_deg spatial_description elevation_m map_mm mat_degc native_veg site_history}
proc fieldsites $::fieldsitesArgs {
    set site $::sitesMap($siteid) 
    foreach attr $::fieldsitesArgs {
	$site setProp $attr [set $attr]
    }
}


set ::personsArgs {siteid last_name first_name  middle_name  suffix  role_in_study  primary_contact  department  organization  date_created profession email telephone  web_site  note} 
proc persons $::personsArgs {
    set site $::sitesMap($siteid) 
    set person [Base new]
    $site addPerson $person
    foreach arg $::personsArgs {
	$person setProp $arg [set $arg]
    }
}

set ::citationsArgs {siteid date_published type title is_part_of  author  correspond_author  identifier description citation url} 
proc citations $::citationsArgs {
    set site $::sitesMap($siteid) 
    set c [Base new]
    $site addCitation $c
    foreach arg $::citationsArgs {
	$c setProp $arg [set $arg]
    }
}

set ::treatmentsArgs  {    siteid treatment_id start_date  treatment_descriptor  rotation_descriptor  tillage_descriptor n_treatment_descriptor  project_scenario fertilizer_amendment_class  cover_crop residue_removal irrigation organic_management  grazing_rate animal_species  operation ars_projects}
proc treatments $::treatmentsArgs {
    set site $::sitesMap($siteid) 
    set treatment [Base new]
    $site addTreatment $treatment
    foreach arg $::treatmentsArgs {
	$treatment setProp $arg [set $arg]
    }
}

set ::experunitsArgs {siteid exp_unit_id  treatment_id field_id  start_date  end_date  change_in_management soil_series  soil_classification  landscape_position  latitude longitude slope exp_unit_size_m2}

proc experunits  $::experunitsArgs {
    set site $::sitesMap($siteid) 
    set unit [Base new]
    $site addUnit $unit
    foreach arg  $::experunitsArgs {
	$unit setProp $arg [set $arg]
    }
}

source tcl/overview.tcl
source tcl/fieldsites.tcl
source tcl/persons.tcl
source tcl/citations.tcl
source tcl/treatments.tcl
source tcl/experunits.tcl


foreach site  $::sites {
    $site process
}





append ::xml "</entries>"





set efp [open entries.xml w]
puts $efp $::xml
close $efp



eval exec jar -cvf graceimport.zip entries.xml $::exportFiles
