#!/bin/sh
#download the pdfbox-app-3.0.1.jar from
#https://pdfbox.apache.org/download.html
#and copy it into the same directory as this shell script

dir=`dirname $0`
java -jar ${dir}/pdfbox-app-3.0.1.jar "$@"
