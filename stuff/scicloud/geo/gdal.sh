#!/bin/sh


url=http://download.osgeo.org/gdal/1.11.2/gdal1112.zip
download=gdal1112.zip
dir=gdal-1.11.2

echo "Installing ${dir}"
rm -r -f ${dir}
if [ ! -f ${download} ]; then
    wget -O ${download} ${url}
fi
unzip ${download}
cd ${dir}
./configure  --without-libtool
make
make install



