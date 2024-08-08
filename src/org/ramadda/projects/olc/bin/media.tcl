

proc initMediaFiles {} {
    if {[file exists media_collection.tcl]} return;
    puts 1
    catch {exec sh /Users/jeffmc/bin/seesv.sh -dots 10 -template {} {media_collection {${0}} {${1}} {${2}} {${3}} {${4}} {${5}} {${6}} {${7}} {${8}} {${9}} {${10}} {${11}} {${12}} {${13}} {${14}} {${15}} {${16}}  }  {\n} {} media_collection.xlsx > media_collection.tcl} err

    puts 2
    catch {exec sh /Users/jeffmc/bin/seesv.sh -dots 10 -template {} {media_series {${0}} {${1}} {${2}} {${3}} {${4}} {${5}} {${6}} {${7}} {${8}} {${9}} {${10}} {${11}} {${12}} {${13}} {${14}} {${15}} {${16}} {${17}} }  {\n} {} media_series.xlsx > media_series.tcl}

    puts 3
    catch {exec sh /Users/jeffmc/bin/seesv.sh -template {} {media_file {${0}} {${1}} {${2}} {${3}} {${4}} {${5}} {${6}} {${7}} {${8}} {${9}} {${10}} {${11}} {${12}} {${13}} {${14}}}  {\n} {} media_file.xlsx > media_file.tcl}

    puts 4
    catch {exec sh /Users/jeffmc/bin/seesv.sh -template {} {media_item {${0}} {${1}} {${2}} {${3}} {${4}} {${5}} {${6}} {${7}} {${8}} {${9}} {${10}} {${11}} {${12}} {${13}} {${14}} {${15}} {${16}} {${17}} {${18}} {${19}} }  {\n} {} media_item.xlsx > media_item.tcl}
}
    


initMediaFiles


set ::mcp {collection_nbr collection_title inclusive_dates bulk_dates bio_org_history creator location language organiz_arrange notes provenance scope_content summary topic_term assoc_materials genre_form pers_fam_name}
proc media_collection $::mcp  {
    incr ::ccnt
    foreach p $::mcp {
	set v [string trim [set $p]]
	set v [spell $v]
	set $p $v
	check collection $::ccnt $p $v
    }

    set cid [cid $collection_nbr]
    if {[info exists ::cids($cid)]} {
#	puts "duplicate collection: $cid $collection_title source title: $::cids($cid)"
	return
    }
#    puts "collection: $cid"
    set ::cmap($cid) $collection_title
    set ::cids($cid) 1
    append ::entries [openEntry type_archive_collection $cid "" $collection_title]
    append ::entries [col collection_number [pad $collection_nbr]]
    append ::entries [mtd2 archive_note "Scope and Content" $scope_content]
    append ::entries "</entry>\n"
}




set ::msp {collection_nbr series_nbr series_title inclusive_dates bulk_dates location bio_org_history scope_content organiz_arrange summary language assoc_materials notes creator creator_role name_type xxx_genre_form provenance}
proc media_series $::msp {
    incr ::scnt
    foreach p $::msp {
	set v [string trim [set $p]]
	set v [spell $v]
	set $p $v
	check series $::scnt $p $v
    }
    set parent [cid $collection_nbr]
    if {![info exists ::cids($parent)]} {
#	puts "no parent: $collection_nbr for series: $series_title"
	puts "**** no parent: $collection_nbr series: $series_nbr $series_title"
    }


    set id [sid $collection_nbr $series_nbr]
    if {[info exists ::sids($id)]} {
	if {$::sids($id) != $series_title} {
	    puts "no match series: id: $id  source: $::sids($id)  media:$series_title"
	}
#	puts "duplicate series: $id $series_title"
	return
    }
    puts "**** xxx series: $collection_nbr series: $series_nbr $series_title"


    set ::sids($id) 1
    append ::entries [openEntry type_archive_series $id $parent  $series_title]
    set summary [processSummary $summary]
    append ::entries [col series_number [pad $series_nbr]]
    append ::entries [col location $location]
    
    foreach lang [split $language "'"]  {
	set lang [string trim $lang]
	append ::entries [mtd1 archive_language $lang]
    }
    append ::entries [mtd2 archive_note Scope $scope_content]
    append ::entries [mtd2 archive_note Note $notes]    
    append ::entries [mtd2 archive_note History $bio_org_history]
    append ::entries [mtd2 archive_note Arrangement $organiz_arrange]
    append ::entries [mtd2 archive_note "Provenance" $provenance]
    append ::entries [mtd2 archive_note "Note" $summary]        
    append ::entries [mtd2 archive_creator $creator $creator_role]    
    handleDate $bulk_dates series $::scnt
    if {[regexp {.*\d\d\d\d-.*} $name_type]} {
	handleDate $name_type series $::scnt
    } else {
	if {$name_type!=""} {
##	    puts stderr "name_type:$name_type"
	}
    }
    append ::entries "</entry>\n"
}






set ::mfp {collection_nbr series_nbr file_unit_nbr title category dates creator genre_form language location notes_prov_notes phys_char_description summary_note assoc_materials_handling_inst  type_format_form_film_orig_format_format_media_type}
proc media_file $::mfp {
    foreach p $::mfp {
	set v [string trim [set $p]]
	set v [spell $v]
	set $p $v
	check file $::fcnt $p $v
    }
    set parent [sid $collection_nbr $series_nbr]
    if {![info exists   ::sids($parent)]} {
	puts  stderr "file: $file_unit_nbr $title no parent series: $parent "
    }
    set id [fid $collection_nbr $series_nbr $file_unit_nbr]
    if {[info exists ::fids($id)]} {
	if {$::fids($id) != $title} {
	    puts "no match file id: $id source:  $::fids($id)  media: $title"
	}
#	puts "duplicate file $::fids($id)  media: $title"
	return
    }
    incr ::fcnt
    puts "media file $id $title"

    set ::fids($id) $title
    append ::entries [openEntry type_archive_file $id $parent $title]
    set summary_note [processSummary $summary_note]

    append ::entries [col file_number [pad $file_unit_nbr]]
    append ::entries [col location $location]
    handlePhysicalDescription $phys_desc 
    handleDate  $dates file $::fcnt
    append ::entries [mtd2 archive_note "Summary" $summary_note]        
    append ::entries [mtd2 archive_note "Provenance" $notes_prov]
    append ::entries [mtd1 archive_creator $creator]
    append ::entries [mtd1 archive_language $language]    
    append ::entries "</entry>\n"
}


set ::mip {collection_nbr series_nbr file_unit_nbr item_nbr title dates summary_note phys_desc notes_prov creator creator_role assoc_materials phys_char language added_entry category
    format dimensions inscr_marks location}



proc media_item $::mip {
    foreach p $::mip {
	set v [string trim [set $p]]
	set v [spell $v]
	set $p $v
	check item $::icnt $p $v
    }
    if {$item_nbr==""} return
    set parent [fid $collection_nbr $series_nbr $file_unit_nbr]
    if {![info exists   ::fids($parent)]} {
	set ::fids($parent) 1
#	puts   "$::mitemcnt missing file:  \{$collection_nbr\} \{$series_nbr\} \{$file_unit_nbr\} $title"
	puts   "files \{$collection_nbr\} \{$series_nbr\} \{$file_unit_nbr\} \{File $file_unit_nbr\}  {}  {}  {}  {}  {}  {}  {}"

	set sparent [sid $collection_nbr $series_nbr]
	if {![info exists   ::sids($sparent)]} {
#	    puts  stderr "missing series: $sparent $title"
	}	
	return
    }


    set id [iid $collection_nbr $series_nbr $file_unit_nbr $item_nbr]
    if {[info exists ::iids($id)]} {
	if {$::iids($id) != $title} {
#	    puts "item id: $id\n\torig title: $::iids($id)\n\tmedia title: $title"
	}
#	puts "duplicate item: $id  title: $::iids($id) media: $title"
	return
    }

    incr ::m_icnt
    incr ::icnt

    append ::entries [openEntry type_archive_item $id $parent $title]
    append ::entries [col item_number [pad $item_nbr]]
    append ::entries [col location $location]
    handlePhysicalDescription $phys_char 
    append ::entries [mtd2 archive_note "Provenance" $notes_prov]



#dimensions inscr_marks
    
    foreach lang [split $language ","]  {
	set lang [string trim $lang]
	append ::entries [mtd1 archive_language $lang]
    }


    set assoc_materials  [string trim $assoc_materials]
#    if {$assoc_materials==""} return
    #puts "item title: $title assoc_materials: $assoc_materials" 
    #return

    regsub -all __ $assoc_materials {X} assoc_materials
    foreach mat [split     $assoc_materials X] {
	set mat [string trim $mat]
	if {$mat=="" || $mat=="Related"} continue
	append ::entries [mtd2 archive_note "Associated Material" $mat]
    }
    if {$added_entry!=""} {
	puts $added_entry
	append ::entries [mtd1 archive_internal $added_entry]
#	puts "item title: $title added_entry: $added_entry"
    }
#    puts $title
#    return

    append ::entries [mtd1 archive_media_type $format]
    append ::entries [mtd2 archive_creator $creator $creator_role]    
    append ::entries [mtd1 archive_category $category]    
    handleDate  $dates "item: $title" $::icnt
    set summary_note [processSummary $summary_note]
    append ::entries [mtd2 archive_note "Summary" $summary_note]        
    append ::entries "</entry>\n"
}
