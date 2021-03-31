proc getUrl {url} {
    catch {exec rm tmp.csv}
    catch {exec curl $url > tmp.csv} err
    set fp [open tmp.csv]
    set c [read $fp]
    close $fp
    set c
}


set ::html "<title>UI Tests</title><div style='margin:20px;'>\n"
set ::loc [file dirname [file normalize [info script]]]
set ::cnt 0
set ::tcnt 0
set ::limit 500

proc finish {} {
    set final "$::html\n</div>"
    set fp [open uiimages.html w]
    puts $fp $final
    close $fp
    
}


proc runit {name id} {
    append ::html "<h2>$name</h2>"
    set url "https://geodesystems.com/repository/entry/show?ascending=true&orderby=name&entryid=${id}&output=default.csv&fields=name,id&showheader=false&showheader=false"
    puts $url
    set csv [getUrl $url]
    set  ::cnt2 0
#    set ::limit  100
    foreach line2 [split $csv "\n"] {
	set line2 [string trim $line2]
	if {$line2==""} continue;
	if {$::cnt2>$::limit} break
	incr ::cnt2
	incr ::cnt
	foreach     {name id} [split $line2 ,] break
	regsub -all {[/ .'\",]+} $name _ clean
	puts stderr "\tprocessing $name"
	set image image_${clean}.png
	set thumb thumb_${clean}.png
	set url "https://geodesystems.com/repository/entry/show?entryid=${id}"
	set sleep 10
	if {![file exists $thumb]} {
	    #Bring Firefox to the front and tell it to reload the main page
	    set cmd "tell application \"Safari\" to set the URL of the front document to \"$url\""    
	    if {[catch {
		exec osascript -e {activate application "Safari"}
		exec osascript -e $cmd
		exec sleep $sleep
		exec osascript $::loc/capture.scpt
		exec cp capture.png $thumb

	    } err]} {
		puts stderr "Error: $err"
		set ::html "Error: $err<hr>$::html"
		append ::html "</div>"
		puts [open uiimages.html w] $::html
		exit
	    }
	}
	set line  "<a href=\"$url\">$name<br>\n<img width=1200 border=0 src=thumb_${clean}.png>\n</a><p>\n"
	append ::html $line
	finish
    }
}




proc processGroup {root} {
    set csv [getUrl "https://geodesystems.com/repository/entry/show?entryid=${root}&ascending=true&orderby=name&output=default.csv&fields=name,id&showheader=false"]
    foreach line [split $csv "\n"] {
	set line [string trim $line]
	if {$line==""} continue;
	incr ::tcnt
	foreach     {name id} [split $line ,] break
	puts stderr "processing $name"
	runit $name $id
	if {$name=="Features" || $name=="Latest"} continue;
    }
    finish
}
    

set chartId 3ebcb4f4-fa4d-4fb3-9ede-d42ec7e0aa9d
set mapId 1d0fa3f5-407e-4a39-a3da-9a5ed7e1e687
set textId 23847d93-4bca-4d54-a6db-f96a19be250b
set mediaId bca6228e-3f8e-49d4-a20e-b5a0ea8a6441

#runit "Charts" $chartId
#runit "Maps" $mapId
#runit "Text" $textId
runit "Media" $mediaId

finish
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
    append ::html "<a href=$url>$url<br><img width=1200 border=0 src=thumb${::cnt}.png></a><p>\n"
}

append ::html "</div>"
puts [open uiimages.html w] $::html
