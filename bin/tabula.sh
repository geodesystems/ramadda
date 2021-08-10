#!/bin/sh
#
#This script is used to call the tabular.jar as a command line for extracting
#tabular data from PDF files.
#It is used by RAMADDA's CSV processing to support using PDFs as an input
#data source. To use copy this file along with the /lib/tabula.jar
#to some directory (e.g., ~/.ramadda/bin). Then set the environment variable:
#export ramadda_tabula=<ramadda home>/bin/tabula.sh
#
myDir=`dirname $0`
java -jar ${myDir}/tabula.jar $1
