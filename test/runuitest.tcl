proc getUrl {url} {
    catch {exec rm tmp.csv}
    catch {exec curl -k $url > tmp.csv} err
    set fp [open tmp.csv]
    set c [read $fp]
    close $fp
    set c
}



set ::loc [file dirname [file normalize [info script]]]
set ::cnt 0
set ::tcnt 0
set ::limit 500

proc finish {} {
    write  "</div>"
}

proc write {html {mode a}} {
    set fp [open uiimages.html $mode]
    puts $fp $html
    flush $fp
    close $fp
}


proc runit {group id} {
    write "<h2>$group</h2>"
    set url "https://geodesystems.com/repository/entry/show?ascending=true&orderby=name&entryid=${id}&output=default.csv&fields=name,id&showheader=false&showheader=false#fortest"
    puts $url
    set csv [getUrl $url]
    set  ::cnt2 0
#    set ::limit  100
    regsub -all { } $group _ _group
    foreach line2 [split $csv "\n"] {
	set line2 [string trim $line2]
	if {$line2==""} continue;
	foreach     {name id} [split $line2 ,] break
	regsub -all {[/ .'\",]+} $name _ clean

	set image image_${clean}.png
	set thumb thumb_${_group}_${clean}.png
	set url "https://geodesystems.com/repository/entry/show?entryid=${id}"
	set sleep 10
	if {![file exists $thumb]} {
	    if {$::cnt2>$::limit} break
	    incr ::cnt2
	    incr ::cnt
	    puts stderr "\tprocessing $name"
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
		write "Error: $err<hr>"
		write "</div>"
		exit
	    }
	}
	set line  "<a href=\"$url\"><div>$name</div>\n<img width=50% border=0 src=${thumb}>\n</a><p>\n"
	write $line
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
    

write "" w
runit "Charts" 3ebcb4f4-fa4d-4fb3-9ede-d42ec7e0aa9d
runit "Maps" 1d0fa3f5-407e-4a39-a3da-9a5ed7e1e687
runit "Text" 23847d93-4bca-4d54-a6db-f96a19be250b
runit "Media" bca6228e-3f8e-49d4-a20e-b5a0ea8a6441
runit "Boulder and Colorado" 4624f63d-cd71-43e8-a558-83835c6b5541
finish
exit


