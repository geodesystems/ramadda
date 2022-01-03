#!/bin/sh

url="http://downloads.sourceforge.net/project/dcm4che/dcm4che2/2.0.28/dcm4che-2.0.28-bin.zip?r=http%3A%2F%2Fsourceforge.net%2Fprojects%2Fdcm4che%2Ffiles%2Fdcm4che2%2F2.0.28%2F&ts=1425588476&use_mirror=hivelocity"
target=dcm4che-2.0.28-bin
dir=dcm4che-2.0.28

echo "Installing ${target}"
rm -r -f ${dir}
if [ ! -f ${target}.zip ]; then
    wget -O ${target}.zip ${url}
fi
unzip ${target}.zip
cd ${dir}






