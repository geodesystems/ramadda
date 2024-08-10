puts "<entries>"
set ::cnt 0
proc well {well url lat lon} {
    incr ::cnt
#    if {$::cnt>2} return
    regexp {([^/]+)$} $url match file
    if {![file exists $file]} {
	catch {exec wget -O $file $url} err
    }
    puts "<entry isnew=\"true\" type=\"type_document_pdf\" name=\"$file\" latitude=\"$lat\" longitude=\"$lon\" file=\"$file\" />"
}

source sites.tcl
puts "</entries>"
