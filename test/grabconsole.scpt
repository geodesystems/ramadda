tell application "Safari"
    activate
end tell
tell application "System Events" 
     click menu item "Show JavaScript Console" of menu "Develop" of menu bar item "Develop" of menu bar 1 of application process "Safari"
end tell
tell application "System Events" to tell process "Safari"
     keystroke "s" using command down
     delay 1
     keystroke return
end tell

