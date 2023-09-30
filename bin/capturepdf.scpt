on run arg
   set pdfFile to arg
   set thePDFPath to (path to home folder as string)
   set posixFolderPath to POSIX path of thePDFPath


tell application "System Events"
   set frontmostProcess to first process where it is frontmost -- this will be the script process
   if name of frontmostProcess is in {"Script Editor", "AppleScript Editor"} then # CAUTION the name changed
       set visible of frontmostProcess to false -- hide the script process
       repeat while (frontmostProcess is frontmost) -- wait until the script is hidden
           delay 0.2
       end repeat
       tell (first process where it is frontmost)
           set theProcess to its name
           set theApp to its file
       end tell
       set frontmost of frontmostProcess to true -- unhide the script process
   else
       tell frontmostProcess
           set theProcess to its name
           set theApp to its file
       end tell
   end if
   set theApp to name of theApp
end tell

activate application theApp
tell application "System Events" to tell process theProcess
   click menu item "Export as PDF…" of menu "File" of menu bar 1
   repeat
       if exists sheet 1 of window 1 then exit repeat
   end repeat
   tell sheet 1 of window 1
       set value of text field 1 to pdfFile
       delay 1
       keystroke return
   end tell
end tell
end run
