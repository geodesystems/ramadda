#!/bin/sh
#
#This script is used to call the tabular.jar as a command line for extracting
#tabular data from PDF files. It is used by RAMADDA's CSV processing to support
#using PDFs as an input data source.
#To use set the environment variable:
#export RAMADDA_TABULA=/path/to/tabula.sh
#
# --pages all
myDir=`dirname $0`
if [ -e ${myDir}/lib/tabula.jar ]
then
    java -jar ${myDir}/lib/tabula.jar $1
elif [ -e ${myDir}/tabula.jar ]
then
    java -jar ${myDir}/tabula.jar $1
else
    echo "Cannot find tabula.jar"
fi
