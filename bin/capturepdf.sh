#!/bin/sh
#run:
#capturepdf url file
#This will call capturepdf.scpt to capture the URL
#It then waits for the file ~/capture<random>.pdf to be written

#It then moves the file to this directory

mydir=$(dirname "$0")
cwd=$(pwd)
pdfs="${cwd}/pdfs"
mkdir  "${pdfs}"
url=$1
file=$2
echo  "capturing $url"
pdf="capture${RANDOM}.pdf"
osascript -e "activate application \"Safari\""
osascript -e "tell application \"Safari\" to set the URL of the front document to \"$url\""    
#echo "Saving PDF: ${pdf} to ${pdfs}"
osascript ${mydir}/capturepdf.scpt $pdf "${pdfs}"
echo "waiting on file"
#look for files  haven't been changed in the last 2 seconds
files=`find ${pdfs}/${pdf} -type f -mtime -2s 2> /dev/null`
while [ -z "$files" ]
do
    sleep 1
    files=`find ${pdfs}/${pdf} -type f -mtime -2s 2> /dev/null`
done
sleep 1
echo "moving ${pdfs}/${pdf} to ${pdfs}/$file"
mv "${pdfs}/${pdf}" "${pdfs}/$file"

