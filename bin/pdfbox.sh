#!/bin/sh

#Download pdfbox-app-3.0.4.jar from https://pdfbox.apache.org/download.html
#and copy it to the same dir as this script


dir=`dirname $0`
java -jar ${dir}/pdfbox-app-3.0.4.jar "$@"
