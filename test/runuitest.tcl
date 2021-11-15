set ::loc [file dirname [file normalize [info script]]]
set ::cnt 0
set  ::cnt2 0
set ::tcnt 0
set ::limit 500
set ::root https://geodesystems.com/repository


proc getUrl {url} {
    catch {exec rm tmp.csv}
    catch {exec curl -k $url > tmp.csv} err
    set fp [open tmp.csv]
    set c [read $fp]
    close $fp
    set c
}


proc finish {} {
    write  "</div></body></html>"
}

proc write {html {mode a}} {
    set fp [open results.html $mode]
    puts $fp $html
    flush $fp
    close $fp
}




proc capture {_group name id} {
    regsub -all {[/ .'\",]+} $name _ clean
    set image image_${clean}.png
    set thumb thumb_${_group}_${clean}.png
    set consoleFile console_${_group}_${clean}.txt
    set url $id
    if {![regexp {^http} $url]} {
	set url "$::root/entry/show?entryid=${id}#fortest"
    }
    set sleep 1
    if {![file exists $thumb]} {
	if {$::cnt2>$::limit} break
	incr ::cnt2
	puts stderr "\tprocessing $name"
	#Bring Firefox to the front and tell it to reload the main page
	set cmd "tell application \"Safari\" to set the URL of the front document to \"$url\""    
	if {[catch {
	    exec osascript -e {activate application "Safari"}
	    exec osascript -e $cmd
	    if {[catch {
		exec osascript $::loc/captureDisplays.scpt
	    } err]} {
		##Do this since any call to log in the above script triggers an error
		puts stderr "$err"
		if {[regexp variable $err]} {
		    exit
		}
	    }
	    set console ~/Console.txt
	    file delete -force $consoleFile
	    exec cp capture.png $thumb
	    ##Now grab the contents of the file
	    if {[file exists $console]} {
		file delete -force $console
	    }
	    exec osascript $::loc/grabconsole.scpt
	    ##Wait for it
	    exec sleep 1
	    if {[file exists $console]} {
		set c [read [open $console r]]
		set fp [open $consoleFile w] 
		puts $fp $c
		close $fp
		file delete $console
	    } 
	} err]} {
	    puts stderr "Error running script: $err"
	    write "Error: $err<hr>"
	    write "</div>"
	    exit
	}
    }
    incr ::cnt
    set extra ""
    if {[file exists $consoleFile]} {
	set fp [open $consoleFile r]
	set c [read $fp]
	close $fp
	set ignore 1
	foreach line [split $c "\n"] {
	    set line [string trim $line]
	    if {$line==""} continue;
	    if {[regexp {allowed to display insecure content from} $line]} {
		continue;
	    }
	    puts stderr "console: $line"
	    set ignore 0
	    break;
	}
	if {!$ignore} {
	    puts stderr "got console from $name"
	    regsub -all {<} $c {&lt;} c
	    regsub -all {>} $c {&gt;} c	
	    set extra "<br><div style='border:1px solid #efefef;background:#FEAFAF;max-width:100%;'>$c</div>"
	    append extra "\n<script  type='text/JavaScript'>doError('$name');</script>\n"
	}
    }
    set line  "<a name='$name'></a><div class='ramadda-gridbox ' style='width:300px;display:inline-block;margin:6px;'><a href=\"$url\">#$::cnt $name\n<img width=100% border=0 src=${thumb}>\n</a>$extra</div>\n"
    write $line
#    finish
}    


proc runit {group id {groupLimit 10000}} {
    write "</div>\n"
    write "<h2>$group</h2><div class=ramadda-grid>"
    set url "$::root/entry/show?ascending=true&orderby=name&entryid=${id}&output=default.csv&fields=name,id&showheader=false&showheader=false"
    puts "group: $group"
    set csv [getUrl $url]
    set  ::cnt2 0
#    set ::limit  100
    regsub -all { } $group _ _group
    foreach line2 [split $csv "\n"] {
	set line2 [string trim $line2]
	if {$line2==""} continue;
	foreach     {name id} [split $line2 ,] break
	capture $_group $name $id
    }
}



set top  "<html><title>RAMADDA Test Results</title><head>\n<script  type='text/JavaScript'>\n"
append top {
    var errorCnt = 0;
    function doError(name) {
	name = "<a href='#" + name+"'>" + name +"</a>";
	var ele = document.getElementById('header');
	if (errorCnt>0) 
	    ele.innerHTML=ele.innerHTML+"&nbsp;|&nbsp;";
	errorCnt++;
	ele.innerHTML=ele.innerHTML+name +"&nbsp;";
    }
} 
append top "</script>\n";


append top "<link href='https://geodesystems.com/repository/htdocs_v5_0_69/style.css'  rel='stylesheet'  type='text/css' />\n</head><body><center><div id=header></div></center>\n<div class=ramadda-grid>\n"
write $top w

exec osascript $::loc/clearcache.scpt

if {[llength $argv] != 0} {
    foreach id $argv {
	capture "Local"  "Page" $id
    }
} else {
    runit "Charts" 3ebcb4f4-fa4d-4fb3-9ede-d42ec7e0aa9d
    runit "Maps" 1d0fa3f5-407e-4a39-a3da-9a5ed7e1e687
    runit "Data tables" b36bb6fc-b2c4-4d12-8c31-1f4dcff6881e
    runit "Science Data" 1012d4bb-5e57-460a-95f5-07c997bd04e8
    #runit Covid 52644ac1-f6d6-45ea-88af-b5d2ec75742e
    runit "Text" 23847d93-4bca-4d54-a6db-f96a19be250b
    runit "Boulder and Colorado" 4624f63d-cd71-43e8-a558-83835c6b5541
    runit Dashboards eb4102f8-720f-4ef3-9211-0ce5940da04d

    runit "Media" bca6228e-3f8e-49d4-a20e-b5a0ea8a6441
    runit Cards e4b6667d-d640-4048-a756-b06e4c352a62 3
    #runit Features 26fff0d9-3de7-4bbd-8a6f-a26d8a287f4a
    runit Misc a7fe0b5c-5c1b-4005-afce-e92b082fa335
    runit Notebooks 38bb6bb4-3f51-4625-9acd-c79c4f445c78
}
finish
exit


