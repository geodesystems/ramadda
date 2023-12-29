#!/bin/sh

#Download pdfbox-app-2.0.27.jar from https://pdfbox.apache.org/download.html
#and copy it to the same dir as this script


dir=`dirname $0`
java -jar ${dir}/pdfbox-app-2.0.27.jar "$@"
