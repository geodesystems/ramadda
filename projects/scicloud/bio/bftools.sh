#!/bin/sh

url="http://downloads.openmicroscopy.org/latest/bio-formats5.0/artifacts/bftools.zip"
target=bftools.zip
dir=bftools

echo "Installing ${target}"
rm -r -f ${dir}
if [ ! -f ${target} ]; then
    wget -O ${target} ${url}
fi
unzip ${target}








