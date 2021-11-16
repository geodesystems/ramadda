set ::mydir [file dirname [file normalize [info script]]]
set ::pageCnt 0
set ::haveWritten 0
set ::initTest 0
set ::output stdout
set ::consoleCnt 0

proc getUrl {url} {
    catch {exec rm tmp.csv}
    catch {exec curl -k $url > tmp.csv} err
    set fp [open tmp.csv]
    set c [read $fp]
    close $fp
    set c
}


proc initTest {} {
    if {!$::initTest} {
	exec osascript $::mydir/clearcache.scpt
	set ::initTest 1
    }
}


proc finish {} {
    write  "</div></body></html>"
}

proc write {html {mode a}} {
    #    set fp [open results.html $mode]
    if {!$::haveWritten} {
	set top  "<html><title>Test Results</title><head>\n<script  type='text/JavaScript'>\n"
	append top {
	    var errorCnt = 0;
	    function doError(name,prefix) {
		name = "<a href='#" + name+"'>" + name +"</a>";
		var ele = document.getElementById('header');
		if (errorCnt>0) 
	        	ele.innerHTML=ele.innerHTML+"&nbsp;|&nbsp;";
		else
		ele.innerHTML="Had console output: "; 
		errorCnt++;
		ele.innerHTML=ele.innerHTML+name +"&nbsp;";
	    }
	} 
	append top "</script>\n";
	append top {
	    <style type='text/css'>
	    :root {
		--font-size:12pt;
		--font-family:  'Open Sans', Helvetica Neue, Arial, Helvetica, sans-serif;
	    }
	    body {
		font-family: var(--font-family);
		font-size: var(--font-size) !important;
		margin: 0px;
		margin-left:10px;
		padding: 0px;
	    }

	    html, body {
		height: 100%;
	    }

	    img {
		border:1px solid #eee;
	    }
	    .test-grid {
		vertical-align:top;
		display:flex;
		flex-wrap: wrap;
	    }
	    .test-gridbox {
		display:inline-block;
		flex-grow:1;
		vertical-align:top;
		margin:5px;
		padding:10px;
		padding-bottom:6px;
		padding-top:6px;
		text-align:left;
	    }

	    </style>
	}

	append top "\n</head><body><center><div id=header></div></center>\n<div class=test-grid>\n"
	puts $::output $top
	set ::haveWritten 1
    }
    puts $::output $html
    flush $::output
}



proc capture {_group name url {doDisplays 1} {sleep 3}} {
    initTest
    regsub -all {[/ .'\",]+} $name _ clean
    set image image_${clean}.png
    set thumb thumb_${_group}_${clean}.png
    set consoleFile console_${_group}_${clean}.txt
    if {![file exists $thumb]} {
	puts stderr "\ttesting $name\n\turl:$url"
	#Bring Firefox to the front and tell it to load the page
	if {[regexp geodesystems.com $url]} {
	    if {![regexp fortest $url]} {
		set url "$url#fortest"
	    }
	}
	set cmd "tell application \"Safari\" to set the URL of the front document to \"$url\""    
	if {[catch {
	    exec osascript -e {activate application "Safari"}
	    exec osascript -e $cmd
	    if {[catch {
		if {$doDisplays} {
		    exec osascript $::mydir/captureDisplays.scpt
		}  else {
		    if {$sleep>0} {
			exec sleep $sleep
		    }
		    exec osascript $::mydir/capture.scpt
		}
	    } err]} {
		##Do this since any call to log in the above script triggers an error
		puts stderr "$err"
		if {[regexp variable $err]} {
		    exit
		}
	    }
	    exec cp capture.png $thumb
	    set console ~/Console.txt
	    file delete -force $consoleFile
	    ##Now grab the contents of the file
	    if {[file exists $console]} {
		file delete -force $console
	    }
	    exec osascript $::mydir/grabconsole.scpt
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
    incr ::pageCnt
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
	    set ignore 0
	    break;
	}
	if {!$ignore} {
	    regsub -all {<} $c {&lt;} c
	    regsub -all {>} $c {&gt;} c	
	    incr ::consoleCnt
	    set extra "<br><div style='border:1px solid #efefef;background:#FEAFAF;max-width:100%;'>$c</div>"
	    append extra "\n<script  type='text/JavaScript'>doError('$name');</script>\n"
	}
    }
    set line  "<a name='$name'></a><div class='test-gridbox ' style='width:300px;display:inline-block;margin:6px;'><a href=\"$url\">#$::pageCnt $name\n<img width=100% border=0 src=${thumb}>\n</a>$extra</div>\n"
    write $line
}    




