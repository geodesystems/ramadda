
source $env(RAMADDA_ROOT)/bin/ramadda.tcl

array set ::titles {}

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
	append ::entries [openEntry type_document_collection $id {} $id]
	append ::entries [col show_tag_search false]
	append ::entries [col show_information false]
	append ::entries [col show_new false]
	append ::entries [col add_tags false]	
	append ::entries "</entry>\n"
    }
    return $id
}    

set ::bookArgs {
    accompanying_material  author_analytics  author_dates  authors_name  barcode_name  call_number  condition  copyright  cost  donor  edition  extended_title  f_p_printed  f_p_reading_level  first_upc  funding_source  hidden_from_opac  holdings_barcode  holdings_note  isbn  last_checked_out_to  lc_control_number  material_type  notes  number_of_part  number_of_visible_copies  physical_details  place_of_publication  price_availability  date  publisher  purchase_date  series_title  series_volume  statement_of_responsibility  subject_headings  summary  technical_description  title  title_analytics  uniform_title
}

proc book $::bookArgs  {

#    if {$::cnt>100} return
    set uid ""
    foreach p $::bookArgs {
	set v [string trim [set $p]]
	set v [spell $v]
	set $p $v
	append uid " $v"
	if {0} {
	    if {$v==""} {
		if {![regexp date $p]} {set $p "UNKNOWN: $p"}
	    } else {
		set $p $v
	    }
	}
	check books $::cnt $p $v
    }

    if {[info exists ::titles($title)]} {
    }
    set ::titles($title) $uid
    set entryExtra ""
    set description ""
    set googleDate ""
    set thumbnail1File ""
    set thumbnail2File ""    

#    catch {set isbn [format %.0f $isbn]}	
    set isbnClean {}
    foreach _isbn [split $isbn " "] {
	set _isbn [string trim $_isbn]
	if {$_isbn == ""} continue
	if {![regexp {^\d+$} $_isbn]} {
	    continue;
	}
	if {[string length $_isbn]<5} continue;
		
	lappend isbnClean $_isbn

	set isbnFile "isbn/${_isbn}.json"
	if {![file exists $isbnFile]} {
	    set url  "https://www.googleapis.com/books/v1/volumes?q=isbn:$_isbn"
	    catch {exec curl -o $isbnFile $url}
	    after 2000
	}


	set isbnFile2 "openlib/${_isbn}.json"
	if {![file exists $isbnFile2]} {
	    set url  "https://openlibrary.org/api/books?bibkeys=ISBN:${_isbn}&format=json&jscmd=data"
	    puts "openlib: $_isbn"
	    catch {exec curl -o $isbnFile2 $url}
	    after 2000
	}	

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
		    if {[dict exists $volume publishedDate]} {
			set googleDate [dict get $volume publishedDate]
		    }
		    if {[dict exists $volume imageLinks]} {
			set images  [dict get $volume imageLinks]
			set thumbnail [dict get $images thumbnail]
			set thumbnail1File "thumbnails1/${_isbn}.jpg"
			if {![file exists $thumbnail1File]} {
			    catch {exec curl -o $thumbnail1File $thumbnail}
			}
		    }
		    if {[dict exists $volume categories]} {
			set cats [dict get  $volume categories]
			foreach cat $cats {
#			    puts $cat
			}
		    }
		    if {[dict exists $volume description]} { 
			set description [dict get  $volume description]	    
			puts $description
		    }
		}
	    }
	}

	if {[file exists $isbnFile2]} {
	    array set seen {}
	    set fp [open $isbnFile2 r]
	    set json [read $fp]
	    close $fp
	    set j [json::json2dict $json]
	    set keys [dict keys $j]
	    if {[llength $keys]>=1} {
#		puts "<p><b>$title</b>"
		set book   [dict get  $j [lindex $keys 0]]	    
		if {[dict exists $book ebooks]} {
		    foreach ebook [dict get $book ebooks] { 
			set preview [dict get $ebook preview_url]
			if {![info exists seen($preview)]} {
			    set seen($preview) 1
#			    puts " <a href=$preview>Preview link</a>"
			    append entryExtra [mtd2 archive_link $preview "Open Library Preview Link"]
			}
		    }
		}
		if {[dict exists $book cover]} {
		    set cover [dict get $book cover]
		    if {[dict exists $cover large]} {
			set large [dict get $cover large]
			set thumbnail2File "thumbnails2/${_isbn}.jpg"
			if {[file size $thumbnail2File]< 100} {
			    file delete $thumbnail2File
			}

			if {![file exists $thumbnail2File]} {
			    puts "fetching $thumbnail2File $large"
			    catch {exec curl -L -o $thumbnail2File $large}
			}
		    } else {
			puts "no large:$title"
		    }
		}
		
		array set seenSubjects {}
		foreach key {subjects subject_people} {
		    if {[dict exists $book $key]} {
			set scnt 0
			foreach subject [dict get  $book $key] {
			    if {[info exists seenSubjects($subject)]} continue;
			    set seenSubjects($subject) 1
			    set name [dict get $subject name]
			    incr scnt
#			    puts "<br>&nbsp;&nbsp;&nbsp;subject:$name"
			    append entryExtra [mtd1 archive_subject $name]
			}
		    }
		}
	    }
	}
    }


    set category [getCategory $call_number $title]
    set parent [getParent $title]


    regsub -all {[\[\]]} $date {} date
    regsub -all unk $date {} date
    regsub -all n/a $date {} date        

    set endDate ""
    regexp {(.*)-(.*)} $date match date endDate

    if {$date!=""} {
#	puts $date
    }
    if {$date==""} {
	set date $googleDate
    }

    if {$date!=""} {
#	puts "pub: $date google: $googleDate"
    }


    set thumbnailFile ""
    set credit ""
    if {[file exists $thumbnail2File]} {
	set thumbnailFile  $thumbnail2File 
	set credit "Credit: openlibrary.org"
    } elseif {[file exists $thumbnail1File]} {
	set thumbnailFile  $thumbnail1File 
	set credit "Credit: Google Books"
    }

    if {![file exists $thumbnailFile]} {
	return
    }



    append ::entries [openEntry type_archive_book {} $parent  $title]    
    if {[file exists $thumbnailFile]} { 
	incr ::cnt
	lappend ::files $thumbnailFile
	append ::entries [makeThumbnail $thumbnailFile $credit] 
    }

    append ::entries $entryExtra

    if {$date !=""} {
	append ::entries [col fromdate $date]    
    }
    if {$endDate !=""} {
	append ::entries [col todate $date]    
    }    


    set place_of_publication [string trim $place_of_publication]
    regsub {:$} $place_of_publication {} place_of_publication
    regsub {^\[} $place_of_publication {} place_of_publication
    regsub {\]$} $place_of_publication {} place_of_publication        

    if {$summary!=""} {
	set description $summary
    }

    if {$description!=""} {
	append ::entries [col description $description]
    }


    regsub -all {\s+;} $series_title {;} series_title
    append ::entries [col authors $authors_name]
    append ::entries [col series_title $series_title]
    append ::entries [col series_volume $series_volume]

    set isbn [join $isbnClean "\n"]
    append ::entries [col isbn $isbn]
    append ::entries [col lccn $lc_control_number]

    append ::entries [col publisher $publisher]
    append ::entries [col publication_place $place_of_publication]    
    append ::entries [col subject_headings $subject_headings]	
    append ::entries [col item_type Book]
    
#    append ::entries [col physical_details $physical_details]
    append ::entries [col barcode $holdings_barcode]    

    if {[regexp {(\d\d\d\d)} $copyright match cp]} {
	append ::entries [col copyright_date $cp]
    }


    append ::entries [col condition $condition]
    append ::entries [col cost $cost]
    append ::entries [col donor $donor]        
    append ::entries [col edition $edition]        
    append ::entries [col call_number $call_number]
    append ::entries [col accompanying_material $accompanying_material]
    append ::entries [col holdings_note $holdings_note]
    append ::entries [col notes $notes]        



    if {[regexp {(.*)/(.*)/(.*)} $purchase_date match mm dd yyyy]} {
	if {[string length $dd]==1} {
	    set dd "0$dd"
	}
	if {[string length $mm]==1} {
	    set mm "0$mm"
	}
	append ::entries [col purchase_date "${yyyy}-${mm}-${dd}"]
    }


    if {false} {
	append ::entries [col item_count $item_count]    
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



eval exec zip books.zip bookentries.xml $::files
