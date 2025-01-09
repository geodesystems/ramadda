
source $env(RAMADDA_ROOT)/bin/ramadda.tcl


package require json

array set ::dewey {
    500 {medicinal plants}
    800 storytelling
    400 language
    200 spirituality 
    340 conflicts
    350  conflicts
    700 art 
    920 genealogy
    921 genealogy    
    922 genealogy
    923 genealogy
    924 genealogy
    925 genealogy
    926 genealogy
    927 genealogy
    928 genealogy
    370 {education/learning}
} 


set ::termsHtml [open terms.html w]
set ::messy [open messyterms.html w]
puts $::termsHtml  "<body><div style='font-size:70%;font-family:helvetica;line-height:1.2em;'>"
puts $::messy "<body><div style='font-size:70%;font-family:helvetica;line-height:1.2em;'>"

set ::files {}

source ../lib.tcl
set ::entries "<entries>\n";
set ::cnt 0 

proc getCategory {d t} {
    if {![regexp {^[^\d]*(\d+).*} $d match n]} {
	puts stderr "no match: $d"
	return ""
    }
    if {[info exists ::dewey($n)]} {
#	puts "$n - $::dewey($n)"
	return $::dewey($n)
    }
#    puts "$n"
    return ""
}





array set ::parent {}

proc getParent {title} {
    regsub -all fixme $title {} title
    set title [string trim $title]
    regexp {^(.)} $title p
    set p [string toupper $p]
    set id "Books - etc"
    foreach key {A-D E-H I-L M-P Q-T U-Z} {
	if {[regexp "\[$key\]" $p]} {
	    set id "Books $key"
	}
    }
    if {![info exists ::parent($id)]} {
	set ::parent($id)  1
	append ::entries [openEntry group $id {} $id]
	append ::entries "</entry>\n"
    }
    return $id
}    

set ::bookArgs {
    accompanying_material  author_analytics  author_dates  authors_name  barcode_name  call_number  condition  copyright  cost  donor  edition  extended_title  f_p_printed  f_p_reading_level  first_upc  funding_source  hidden_from_opac  holdings_barcode  holdings_note  isbn  last_checked_out_to  lc_control_number  material_type  notes  number_of_part  number_of_visible_copies  physical_details  place_of_publication  price_availability  publication_date  publisher  purchase_date  series_title  series_volume  statement_of_responsibility  subject_headings  summary  technical_description  title  title_analytics  uniform_title
}

proc book $::bookArgs  {
    incr ::cnt
#    if {$::cnt>100} return
    foreach p $::bookArgs {
	set v [string trim [set $p]]
	set v [spell $v]
	set $p $v
	check books $::cnt $p $v
    }
#    catch {set isbn [format %.0f $isbn]}	

    foreach _isbn [split $isbn " "] {
	set _isbn [string trim $_isbn]
	if {$_isbn == ""} continue
	if {![regexp {^\d+$} $_isbn]} {
	    continue;
	}
	if {[string length $_isbn]<5} continue;
	set isbnFile "isbn/${_isbn}.json"
	if {![file exists $isbnFile]} {
	    puts "isbn: $_isbn"
	    set url  "https://www.googleapis.com/books/v1/volumes?q=isbn:$_isbn"
	    puts $url
	    catch {exec curl -o $isbnFile $url}
	    after 2000
	}
	set thumbnailFile ""

	if {[file exists $isbnFile]} {
	    set fp [open $isbnFile r]
	    set json [read $fp]
	    close $fp
	    set j [json::json2dict $json]
	    set cnt   [dict get  $j totalItems]
	    if {$cnt>=1} {
		set items   [dict get  $j items]	    
		foreach item $items {
		    set volume [dict get  $item volumeInfo]
#		    puts  [dict get  $item description]	    
		    if {[dict exists $volume imageLinks]} {
			set images  [dict get $volume imageLinks]
			set thumbnail [dict get $images thumbnail]
			set thumbnailFile "thumbnails/${_isbn}.jpg"
			if {![file exists $thumbnailFile]} {
			    puts $thumbnailFile
			    catch {exec curl -o $thumbnailFile $thumbnail}
			}
		    }
		    if {[dict exists $volume categories]} {
			set cats [dict get  $volume categories]
			foreach cat $cats {
#			    puts $cat
			}
		    }

		    if {[dict exists $volume description]} { 
			set desc [dict get  $volume description]	    
#			puts "title: $title description: $desc"
		    }
		}
	    }
	}

    }




    set category [getCategory $call_number $title]
    set parent [getParent $title]
    append ::entries [openEntry type_archive_book {} $parent  $title]    


    puts $publication_date
    return
    set year $publication_date
    set year1 ""
    regsub -all "l" $year 1 year
    regsub -all {^[^\d]*(\d\d\d\d).*} $year {\1} year1
    
    set year [string trim $year1]
    if {![regexp {^\d\d\d\d$} $year]} {
	set year ""
    }
    if {[string length $lccn]>20} {
	set other_terms "$other_terms .  $lccn"
	set lccn ""
    }

    if {[file exists $thumbnailFile]} {
	append ::entries [makeThumbnail $thumbnailFile "Image courtesy of Google Books"]
    }


    if {$year !=""} {
	append ::entries [col fromdate $year]    
    }
    if {[regexp Kurz $item_type]} {
	set item_type ""
    }

    append ::entries [col description $notes]    
    append ::entries [col item_type $item_type]
    append ::entries [col item_count $item_count]    
    append ::entries [col call_number $call_nbr]
    append ::entries [col physical_characteristic $phys_char]
    append ::entries [col authors $author]
    append ::entries [col publisher $publisher]
    append ::entries [col isbn $isbn]
    append ::entries [col lccn $lccn]
    append ::entries [col barcode $barcode]    
    append ::entries [col volume $volume]
    append ::entries [col series $series]        
    if {$other_terms!=""} {
	set fp $::termsHtml
	set messy 0
	if {[string length $other_terms]>1000} {
	    set messy 1
	    set fp $::messy
	}

	puts $fp "<div style='font-weight:bold;'>#$::cnt: $title<div style='margin-left:30px;'>Terms: $other_terms</div></div>"
	regsub -all {\t} $other_terms { } other_terms

	regsub -all {(\d+)\.(\d+)} $other_terms {\1_dot\2} other_terms
	foreach dot {{G.P.O.} {St.} {S.D.}  {U.S.} {D. F.}  {Mont.} { ca.} {Hist.} {etc.} {Wyo.} {D. C.} {1st.} { L.} {C. d. } {A. R.} {Neb.} {n B.} {M. R} {d. } {H. } { Co.} { E. C. } { Fla.} { J.,} {Wis.} {regt.} { S.} {-ca. } {N.D. } {J. A. } { C.} { G.} { F.} {N.D.} { fl. } {D.C.} {Minn.} {Feb.} {Jan.} { 1. } { 2. } { Pa.} {7.100} { J. H.} {0.00} {Ft. } {S.D} {U.S } {970.1} {N.Y.}  { A.} {H.H.} {....} { 2. } { 3. } { O.--b. } {N.J.}  {W. } {Vol.} {T.C.} {Dak. } { E. } {T. } { p. } {Soc. } {United States. Army. } {United States. Army.} {J. } {Dept.} {Calif. } {s.n.} {Supt.} {Docs.}} {
	    regsub -all {\.} $dot {\\.} p
	    regsub -all {\.} $dot {_dot} w	
	    regsub -all -- $p $other_terms $w other_terms
	}
	array set seen {}

	foreach term [split $other_terms .] {
	    set term [string trim $term]
	    if {$term==""} continue
	    regsub -all _dot $term . term
	    regsub -all etc $term etc. term
	    regsub -all {etc\.\.} $term etc. term	    
	    regsub {^--} $term {} term
	    set term [fixme $term]
	    if {[info exists seen($term)]} continue
	    set seen($term) 1
	    puts $fp "<div style='padding:2px;border-bottom:1px solid #ccc;white-space: nowrap;margin-left:60px;'> $term</div>\n"
	    if {!$messy} {
		append ::entries [mtd1 archive_term $term]
	    }


	}
	puts $fp "<div style='margin-top:10px;'></div>"
    }
    append ::entries "</entry>\n"
}


catch {exec sh /Users/jeffmc/bin/seesv.sh -template {} {book {${0}} {${1}} {${2}} {${3}} {${4}} {${5}} {${6}} {${7}} {${8}} {${9}} {${10}} {${11}} {${12}} {${13}} {${14}} {${15}} {${16}} {${17}} }  {\n} {} books.csv > books.tcl}


source newbooks.tcl
append ::entries "</entries>\n";

set fp [open bookentries.xml w]
puts $fp $::entries
close $fp


puts $::termsHtml "</div></body>"
puts $::messy "</div></body>"
close $::termsHtml
close $::messy





