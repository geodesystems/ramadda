on run arguments
   if (count of (arguments)) ≥ 1 then
       set csvFile to item 1 of arguments
    else   
        set csvFile to "file.csv"
    end if

   if (count of (arguments)) ≥ 2 then
--       set destDir to POSIX path of item 2 of arguments
       set destDir to item 2 of arguments       
    else   
        set destDir to "/Users/jeffmc/Desktop"
    end if

   set fullPath to (destDir & "/" & csvFile)
--   display dialog fullPath

tell application "Microsoft Excel"
    activate
    set theWorkbook to active workbook
--    set csvFilePath to POSIX path of (choose file name default name csvFile default location destDir)
    set csvFilePath to POSIX path of fullPath
    save theWorkbook in csvFilePath as CSV file format
end tell
end run