#!/bin/sh


url="http://downloads.sourceforge.net/project/picard/picard-tools/1.119/picard-tools-1.119.zip?r=http%3A%2F%2Fsourceforge.net%2Fprojects%2Fpicard%2Ffiles%2Fpicard-tools%2F1.119%2F&ts=1425590098&use_mirror=softlayer-dal"
download=picard-tools-1.119.zip
dir=picard-tools-1.119

echo "Installing ${dir}"
rm -r -f ${dir}
if [ ! -f ${download} ]; then
    wget -O ${download} ${url}
fi

unzip ${download}










