#!/bin/sh

url=http://download.osgeo.org/proj/proj-4.8.0.zip
download=proj-4.8.0.zip
dir=proj-4.8.0

echo "Installing ${dir}"
rm -r -f ${dir}
if [ ! -f ${download} ]; then
    wget -O ${download} ${url}
fi
unzip ${download}
cd ${dir}
./configure
make
make install



