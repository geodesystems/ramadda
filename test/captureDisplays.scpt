set ok to 0
set tries to 0

repeat while ok = 0
       tell application "Safari"
           activate
	   set winID to id of window 1
	   set tries to tries + 1
	   if tries > 10
	        log "Too many tries: " & tries
		exit repeat
	   end if
           set ok to   do JavaScript "if(!window['Utils']) {captureValue = 0;} else {captureValue = Utils.areDisplaysReady();}" in document 1 
	   if ok = 0
	        delay 1
	   end if
       end tell
end repeat
delay 1
do shell script "screencapture   -a -x -l " & winID & " capture.png" 
