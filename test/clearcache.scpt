tell application "Safari"
    activate
end tell
tell application "System Events" 
     click menu item "Empty Caches" of menu "Develop" of menu bar item "Develop" of menu bar 1 of application process "Safari"
end tell

