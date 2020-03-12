proc getUrl {url} {
    catch {exec rm tmp.csv}
    catch {exec curl $url > tmp.csv} err
    set fp [open tmp.csv]
    set c [read $fp]
    close $fp
    set c
}


set ::html "<div style='margin:20px;'>"
set ::loc [file dirname [file normalize [info script]]]
set ::cnt 0
set ::tcnt 0



set csv [getUrl https://geodesystems.com/repository/entry/show?orderby=name&entryid=049a8297-58d7-4646-b689-b188ac274640&output=default.csv&fields=name,id&showheader=false]
foreach line [split $csv "\n"] {
    set line [string trim $line]
    if {$line==""} continue;
    incr ::tcnt
    foreach     {name id} [split $line ,] break
    if {$name=="Features" || $name=="Latest"} continue;
    puts stderr "processing $name"
    set csv [getUrl https://geodesystems.com/repository/entry/show?orderby=name&entryid=${id}&output=default.csv&fields=name,id&showheader=false&showheader=false]
    append ::html "<h2>$name</h2>"
    set  ::cnt2 0
    foreach line2 [split $csv "\n"] {
	set line2 [string trim $line2]
	if {$line2==""} continue;
	incr ::cnt
	if {$::cnt2>2} break
	incr ::cnt2

	foreach     {name id} [split $line2 ,] break
	puts stderr "\tprocessing $name"
	set image image$::cnt.png
	set thumb thumb${::cnt}.png
	set url "https://geodesystems.com/repository/entry/show?entryid=${id}"
	set sleep 5
	if {![file exists $thumb]} {
	    #Bring Firefox to the front and tell it to reload the main page
	    exec osascript -e {activate application "Safari"}
	    set cmd "tell application \"Safari\" to set the URL of the front document to \"$url\""    
	    exec osascript -e $cmd
	    exec sleep $sleep
	    exec osascript $::loc/capture.scpt
	    exec cp capture.png $thumb
	}
	append ::html "<a href=$url>$name<br><img width=1200 border=0 src=thumb${cnt}.png></a><p>\n"
    }
}
append ::html "</div>"
puts [open uiimages.html w] $::html
exit




set urls [read [open $loc/uiurls.txt r]]
foreach url $urls {
    set url [string trim $url]
    if {$url==""} continue;
    if {$url == "quit"} {
	append ::html "</div>"
	puts [open uiimages.html w] $::html
	exit
    }
    if {[regexp {^#.*} $url]} {
	continue
    }
    if {[regexp {^label:(.*)$} $url match label]} {
	puts "label:$label"
	append ::html "<h2>$label</h2>\n"
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
    incr ::cnt
    set image image$::cnt.png
    set thumb thumb${::cnt}.png

    if {![file exists $thumb]} {
	puts  "capturing $url"
	#Bring Firefox to the front and tell it to reload the main page
	exec osascript -e {activate application "Safari"}
	set cmd "tell application \"Safari\" to set the URL of the front document to \"$url\""    
	exec osascript -e $cmd
	exec sleep $sleep
	exec osascript $::loc/capture.scpt
	exec cp capture.png $thumb
    }
    append ::html "<a href=$url>$url<br><img width=1200 border=0 src=thumb${cnt}.png></a><p>\n"
}

append ::html "</div>"
puts [open uiimages.html w] $::html
