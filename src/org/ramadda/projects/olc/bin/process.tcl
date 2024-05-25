
set ::entries "<entries>\n";
proc cdata {s} {
    return "<!\[CDATA\[$s\]\]>"
}

proc col {name s} {
    return "<$name>[cdata $s]</$name>\n"
}    


proc attrs {args} {
    set e ""
    foreach {key value} $args {
	append e [col $key $value]
    }
    set e
}
proc openEntry {type id parent name} {
    set e   "<entry id=\"$id\" type=\"$type\" ";
    if {$parent !=""} {append e " parent=\"$parent\" ";}
    append e ">\n";
    append e [attrs name $name]
    set e
}

proc  cid {cid} {
    return collection_$cid
}

proc  sid {cid sid} {
    return "[cid $cid]_series_$sid"
}


proc  fid {cid sid fid} {
    return "[sid $cid $sid]_file_$fid"
}

proc  iid {cid sid fid iid} {
    return "[fid $cid $sid $fid]_item_$iid"
}

proc collection {collection_title collection_nbr location organiz_arrange scope_content bio_org_history user_1 notes provenance column1 _1 _2 _3}  {
    append ::entries [openEntry type_archive_collection [cid $collection_nbr] "" $collection_title]
    append ::entries [col location $location]
    append ::entries [col creator_sketch $column1]
    append ::entries [col scope_and_content $scope_content]        
    append ::entries "</entry>\n"
}

proc series {collection_nbr series_nbr series_title location scope_content notes user_1 bio_org_history organiz_arrange provenance bulk_dates name_type creator summary column1} {
    set parent [cid $collection_nbr]
    set id [sid $collection_nbr $series_nbr]
    append ::entries [openEntry type_archive_series $id $parent  $series_title]
    append ::entries [col location $location]
    append ::entries [col creator_sketch $column1]
    append ::entries [col scope_and_content $scope_content]        
    append ::entries "</entry>\n"
}

proc files {collection_nbr series_nbr file_unit_nbr item_nbr title location phys_desc notes_prov pers_fam_name other_terms media_type creator category dates summary_note user_1} {
    set parent [sid $collection_nbr $series_nbr]
    set id [fid $collection_nbr $series_nbr $file_unit_nbr]
    append ::entries [openEntry type_archive_file $id $parent $title]
    append ::entries [col location $location]
    append ::entries "</entry>\n"
}

proc item {collection_nbr series_nbr file_unit_nbr item_nbr title location phys_desc notes_prov pers_fam_name other_terms media_type creator category dates summary_note user_1} {
    if {$item_nbr==""} return
    set parent [fid $collection_nbr $series_nbr $file_unit_nbr]
    set id [iid $collection_nbr $series_nbr $file_unit_nbr $item_nbr]
    append ::entries [openEntry type_archive_item $id $parent $title]
    append ::entries [col location $location]
    append ::entries "</entry>\n"
}

source collections.tcl
source series.tcl
source files.tcl
source items.tcl

append ::entries "</entries>\n";

puts $::entries

