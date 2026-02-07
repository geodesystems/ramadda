set ::mydir [file dirname [file normalize [info script]]]
set ::pageCnt 0
set ::haveWritten 0
set ::haveWrittenError 0
set ::initTest 0
set ::output ""
set ::consoleCnt 0

proc getUrl {url} {
    catch {exec rm tmp.txt}
    set rc [catch {
	exec curl -k -s -o tmp.txt -w "%{http_code}" $url
    } code]

    if {$code!=200} {
	error "Error: url received code $code: $url"
    }

#    catch {exec curl -k $url > tmp.txt} err
#    puts $err
    set fp [open tmp.txt]
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

proc getTop { {title {Test Results}} {inError 0}} {
    set top  "<html><title>$title</title><head>\n"
    append top [read [open "$::mydir/prefix.html" r]]
    append top "\n</head><body>";
    if {$inError} {
	append top "<h2>Errors</h2>"
    }
    append top "<center><div id=header></div></center>\n<div id=header2></div><div xclass=test-grid>\n"
    set top
}

proc write {html} {
    if {!$::haveWritten} {
	puts $::output [getTop]
	set ::haveWritten 1
    }
    puts $::output $html
    flush $::output
}

proc writeError {html} {
    if {!$::haveWrittenError} {
	set fp [open errors.html w]
	puts $fp [getTop  "Test Errors" 1]
	close $fp
	set ::haveWrittenError 1
    }
    set fp [open errors.html a]
    set error $html
    puts $fp $error
    close $fp
}



proc convertUrl {url} {
    if {[info exists ::env(RAMADDA_USER)]} {
	regsub -all "{ARG_USER}" $url  $::env(RAMADDA_USER) url
    }
    if {[info exists ::env(RAMADDA_PASSWORD)]} {
	regsub -all "{ARG_PASSWORD}" $url  $::env(RAMADDA_PASSWORD) url
    }
    set url
}


proc logout {} {
    capture "" "" https://ramadda.org/repository/user/logout 0 0 1
}

proc capture {_group name url {doDisplays 1} {sleep 3} {justCall 0} } {
    set url [convertUrl $url]
    initTest
    regsub -all {[/ .'\",]+} $name _ clean
    regsub -all {\?} $clean _ clean    
    set image image_${clean}.png
    set thumb thumb_${_group}_${clean}.png
    set consoleFile console_${_group}_${clean}.txt
    if {$justCall || ![file exists $thumb]} {
	puts stderr "\t$name url:$url"
	#Bring Firefox to the front and tell it to load the page
	if {[regexp ramadda.org $url]} {
	    if {![regexp fortest $url]} {
		set url "$url#fortest"
	    }
	}
	if {0} {
	    close front document
	    delay 0.2
	    make new document with properties {URL:theURL}
	}


	set cmd "tell application \"Safari\" to set the URL of the front document to \"$url\""
##set the url to about:blank to clear the console
	set cmd "tell application \"Safari\"\n    tell front document\n        set its URL to \"about:blank\"\n        delay 0.2\n        set its URL to \"$url\"\n    end tell\nend tell\n"
	if {[catch {
	    exec osascript -e {activate application "Safari"}
	    exec osascript -e $cmd
	    if {$justCall} {
		return "OK";
	    }

	    if {[catch {
		if {$sleep>0} {
		    exec sleep $sleep
		}
		if {$doDisplays} {
		    exec osascript $::mydir/captureDisplays.scpt
		}  else {
		    exec osascript $::mydir/capture.scpt
		}
	    } err]} {
		##Do this since any call to log in the above script triggers an error
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
	    if {$err!="OK"} {
		puts stderr "Error running script: $err"
		write "Error: $err<hr>"
		write "</div>"
		exit
	    }
	}
    }
    if {$justCall} {
	return
    }
    incr ::pageCnt
    set extra ""
    set extraError ""    
    set inError 0
    if {[file exists $consoleFile]} {
	set fp [open $consoleFile r]
	set c [read $fp]
	close $fp
	set lines ""
	set debug [regexp {.*Vega.*} $consoleFile]
	set debug 0
	set seenFailed 0
	foreach line [split $c "\n"] {
	    set line [string trim $line]
	    if {$line==""} continue;
	    if {$debug} {puts stderr "Line: $line"}

	    if {[regexp {Cannot load.*.map} $line]} {
		continue;
	    }

	    if {[regexp {Source Map loading errors} $line]} {
		continue;
	    }


	    if {[regexp {The input spec uses.*} $line]} {
		continue;
	    }		

	    if {[regexp {Failed to load resource: the server responded with a status of 404.*} $line]} {
		continue;
	    }

	    if {[regexp {Failed to load resource.*.map} $line]} {
		continue;
	    }
	    
	    set skip 0
	    foreach pattern { {GeolocationPositionError}  {A server with the specified hostname could not be found} {Unrecognized Content-Security-Policy directive} {googleads\.g\.doubleclick\.net} {Version} {\[Warning\]} {\[Log\]} {The input spec uses Vega} } {
		if {[regexp ".*$pattern.*" $line]} {
		    set skip 1
		}
	    }
	    if $skip continue;

	    if {[regexp {\[Warning\] *THREE.*} $line]} {
		continue;
	    }
	    if {[regexp {.*\.map due to access control checks.*} $line]} {
		continue;
	    }

	    if {[regexp {.*Not allowed to request resource.*} $line]} {
		continue;
	    }
	    if {[regexp {\.js\.map.*} $line]} {
		continue;
	    }	    

	    if {[regexp {Failed to load resource} $line]} {
		if {![regexp {Insurrection} $line]} {
		    if {$seenFailed} continue;
		    set seenFailed 1
		}
	    }
	    if {[regexp {\[Log\]} $line]} {
		continue;
	    }
	    if {[regexp {\[Debug\]} $line]} {
		continue;
	    }
	    if {[regexp {\[Info\]} $line]} {
		continue;
	    }	    
	    if {[regexp {not represented on screen} $line]} {
		continue;
	    }
	    if {[regexp {allowed to display insecure content from} $line]} {
		continue;
	    }
	    if {[regexp -nocase {Texture has been resized from} $line]} {
		continue;
	    }
	    if {[regexp -nocase {\.min\.js\.map} $line]} {
		continue;
	    }	    	    	    

	    if {[regexp -nocase {multiple instances of three} $line]} {
		if {$debug} {puts stderr "SKIPPING : $line"}
		continue;
	    } 
	    if {[regexp {The input spec uses Vega} $line]} {
		continue;
	    }	    	    
	    if {[regexp -nocase {Feature policy.*failed for iframe with origin} $line]} {
		continue;
	    }
	    if {[regexp -nocase {invalid sandbox flag} $line]} {
		continue;
	    }	    
	    if {[regexp -nocase {A-Frame:warn} $line]} {
		continue;
	    }	    
	    if {[regexp -nocase {\[Log\].*load point data} $line]} {
		continue;
	    }	    
	    regsub -all {<} $line {\&lt;} line
	    regsub -all {>} $line {\&gt;} line	
	    append lines "$line\n"
	}
	if {$lines!=""} {
	    set inError 1
	    if {$debug} {puts stderr "Lines:$lines"}
	    incr ::consoleCnt
	    set extra "<br><pre style='font-size:10pt;padding:2px;margin:0px;max-height:200px;overflow-y:auto;border:1px solid #efefef;background:#FEAFAF;max-width:100%;'>$lines</pre>"
	    append extra "\n<script  type='text/JavaScript'>doError('$name');</script>\n"
	    set extraError "<pre style='font-size:10pt;padding:4px;margin:10px;margin-top:2px;margin-bottom:10px;max-height:200px;overflow-y:auto;border:1px solid #ccc;'>$lines</pre>"
	}
    }
    set line  "<a name='$name'></a><div class='test-gridbox ' style='width:400px;display:inline-block;margin:6px;'><a href=\"$url\">#$::pageCnt $name\n<img width=100% border=0 src=${thumb}>\n</a>$extra</div>\n"
    write $line
    if {$inError} {
	set line  "<a href=\"$url\">#$::pageCnt $name </a>$extraError";
	writeError $line
    }
}    




