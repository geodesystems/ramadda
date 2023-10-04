#!/bin/sh
#run:
#capturepdf url file
#This will call capturepdf.scpt to capture the URL
#It then waits for the file ~/capture<random>.pdf to be written

#It then moves the file to this directory

mydir=$(dirname "$0")
cwd=$(pwd)
pdfs="${cwd}/pdfs"

if [ !  -d "${pdfs}" ]
then
    mkdir  "${pdfs}"
fi

url=$1
file="${pdfs}/$2"
if [ -f "${file}" ]
then
   echo  "file exists ${file}"
   exit
fi
echo  "capturing ${2}"
pdf="capture${RANDOM}.pdf"
osascript -e "activate application \"Safari\""
osascript -e "tell application \"Safari\" to set the URL of the front document to \"$url\""    
sleep 20
#echo "Saving PDF: ${pdf} to ${pdfs}"
osascript ${mydir}/capturepdf.scpt $pdf "${pdfs}"
#look for files  haven't been changed in the last 2 seconds
files=`find ${pdfs}/${pdf} -type f -mtime -2s 2> /dev/null`
while [ -z "$files" ]
do
    sleep 1
    files=`find ${pdfs}/${pdf} -type f -mtime -2s 2> /dev/null`
done
sleep 1
echo "done  ${file}"
mv "${pdfs}/${pdf}" "${file}"

