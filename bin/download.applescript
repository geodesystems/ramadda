set filePath to (POSIX file "/Users/jeffmc/urls.txt") as alias
set urlList to paragraphs of (read filePath)

tell application "Safari"
	activate
	repeat with theURL in urlList
	        set URL of document 1 to theURL
--		open location theURL
		delay 2
		do JavaScript "
		   var form =  document.querySelector('form');
                   if(form) {
         		   console.log('download');
		       form.submit();
                    }  else {
         		   console.log('no form');
		    }
		" in document 1
		delay 2 
	end repeat
end tell
