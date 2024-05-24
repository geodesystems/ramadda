
set ::entries "<entries>\n";
proc cdata {s} {
    return "<!\[CDATA\[$s\]\]>"
}

proc col {name s} {
    return "<$name>[cdata $s]</$name>\n"
}    


set ::cnt 0
proc collection {collection_title collection_nbr location organiz_arrange scope_content bio_org_history user_1 notes provenance column1 _1 _2 _3}  {
    append ::entries "<entry id=\"collection_$collection_nbr\" type=\"type_archive_collection\" >"
    append ::entries [col name $collection_title]
    append ::entries [col location $location]
    append ::entries [col creator_sketch $column1]
    append ::entries [col scope_and_content $scope_content]        
    append ::entries "</entry>\n"
}

proc series {collection_nbr series_nbr series_title location scope_content notes user_1 bio_org_history organiz_arrange provenance bulk_dates name_type creator summary column1} {
    set parent "collection_${collection_nbr}"
    set id "series_${collection_nbr}_${series_nbr}"
    append ::entries "<entry id=\"$id\" parent=\"$parent\" type=\"type_archive_series\">"
    append ::entries [col name $series_title]
    append ::entries [col location $location]
    append ::entries [col creator_sketch $column1]
    append ::entries [col scope_and_content $scope_content]        
    append ::entries "</entry>\n"

}

proc files {collection_nbr series_nbr file_unit_nbr item_nbr title location phys_desc notes_prov pers_fam_name other_terms media_type creator category dates summary_note user_1} {
    set id "file_${collection_nbr}_${series_nbr}_${file_unit_nbr}"
    set parent "series_${collection_nbr}_${series_nbr}"



}

proc item {collection_nbr series_nbr file_unit_nbr item_nbr title location phys_desc notes_prov pers_fam_name other_terms media_type creator category dates summary_note user_1} {
}

source collections.tcl
source series.tcl
source files.tcl
source items.tcl

append ::entries "</entries>\n";

puts $::entries

