set ::loc [file dirname [file normalize [info script]]]

proc runit {url file} {
    puts $url
    set pdf ~/capture.pdf
    set cmd "tell application \"Safari\" to set the URL of the front document to \"$url\""    
    if {[catch {
	exec osascript -e {activate application "Safari"}
	exec osascript -e $cmd
	exec sleep 5
	exec osascript $::loc/capturepdf.scpt
	#Wait a bit
	set existsCnt 0;
	set mtime 0
	for {set i 0} {$i<10} {incr i} {
	    if {[file exists $pdf]} {
		set t [file mtime $pdf]
		if {$mtime == $t} {
		    puts "moving ~/capture.pdf to $file"
		    file rename  -force $pdf $file
		    break;
		}
		set mtime $t
	    }
	    exec sleep 1
	}
    } err]} {
	puts stderr "Error: $err"
	exit
    }
}

runit "https://localhost:8430/repository/entry/show?entryid=2eb1ff46-9e33-4917-95ff-950f36802891&db.search=Search&dbsortdir=desc&forprint=true&entriesperpage=8" "test.pdf"
