tell application "Safari"
    activate
    set winID to id of window 1
end tell
do shell script "screencapture   -x -l " & winID & " capture.png" 
