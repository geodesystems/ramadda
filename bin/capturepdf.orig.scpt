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
   set nbw to count windows
   keystroke "p" using command down
   
   repeat
       if exists sheet 1 of window 1 then exit repeat
   end repeat
   
   tell sheet 1 of window 1
       set PDFButton to first menu button
       click PDFButton
       click menu item 2 of menu 1 of PDFButton
       
       repeat
           if exists sheet 1 then exit repeat
       end repeat
       
       
       tell sheet 1
           # Set the Desktop as destination folder
           
           set value of text field 1 to pdfFile
           delay 2
           keystroke "g" using {command down, shift down}
           repeat until exists sheet 1
               delay 2
           end repeat
           tell sheet 1
               # CAUTION. before 10.11 the target field was a text field. Now it's a combo box
               if class of UI elements contains combo box then
                   --> {static text, combo box, button, button}
                   set value of combo box 1 to posixFolderPath
               else
                   --> {static text, text field, button, button}
                   set value of text field 1 to posixFolderPath
               end if
               get name of buttons
               keystroke return
           end tell
           get name of buttons
           
           keystroke return
       end tell
   end tell
end tell

end run
