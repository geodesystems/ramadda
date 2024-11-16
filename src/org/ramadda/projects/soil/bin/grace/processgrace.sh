#!/bin/sh
set -e
export mydir=`dirname $0`


#This calls the SeeSV command line
#The environment variable needs to be set to the seesv.sh
#e.g.
#export SEESV=/Users/jeffmc/bin/seesv.sh

#The -cleaninput says the input CSV is one line per record
#the -dots say to print a progress message

seesv() {
    ${SEESV}   -dots  "tab2000"  "$@"
}


if [ ! -d "tcl" ]; then
    mkdir  tcl
fi


for file in "$@"; do
  filename=$(basename "$file")
  echo "$filename"
  basename="${filename%.*}"
  seesv -trim 0-100 -min 1 -tcl $basename  $file > "tcl/$basename.tcl"
done

