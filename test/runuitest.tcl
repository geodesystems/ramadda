set loc [file dirname [file normalize [info script]]]
set urls [read [open $loc/uiurls.txt r]]
set ullr {0,0,2000,1000}
set cnt 0
set html "<div style='margin:20px;'>"
foreach url $urls {
    set url [string trim $url]
    if {$url==""} continue;
    if {$url == "quit"} {
	append html "</div>"
	puts [open uiimages.html w] $html
	exit
    }
    if {[regexp {^#.*} $url]} {
	continue
    }

    if {[regexp {^label:(.*)$} $url match label]} {
	puts "label:$label"
	append html "<h2>$label</h2>\n"
	continue
    }
	
    if [regexp output= $url] continue;
    if {![regexp https $url]} continue;
    if {[info exists seen($url)]} continue;
    set sleep 15
    if {[regexp {^([0-9]+):(.*)$} $url match prefix rest]} {
	set url $rest
	set sleep $prefix
    }
    set seen($url) 1
    incr cnt
    set image image$cnt.png
    set thumb thumb${cnt}.png

    if {![file exists $thumb]} {
	puts  "capturing $url"
	#Bring Firefox to the front and tell it to reload the main page
	exec osascript -e {activate application "Safari"}
	set cmd "tell application \"Safari\" to set the URL of the front document to \"$url\""    
	exec osascript -e $cmd
	exec sleep $sleep
	exec osascript $loc/capture.scpt
	exec cp capture.png $thumb
    }
    append html "<a href=$url>$url<br><img width=1200 border=0 src=thumb${cnt}.png></a><p>\n"
}

append html "</div>"
puts [open uiimages.html w] $html
