#!/bin/sh

target=hdf5-1.8.14
url=ftp://ftp.hdfgroup.org/HDF5/current/src/${target}.tar.gz
echo "Installing ${target}"
rm -r -f ${target}
if [ ! -f ${target}.tar ]; then
    wget ${url}
fi
gunzip ${target}.tar.gz
tar -xvf ${target}.tar
cd ${target}
./configure 
make
make install



