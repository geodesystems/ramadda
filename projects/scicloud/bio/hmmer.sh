#!/bin/sh


url="ftp://selab.janelia.org/pub/software/hmmer3/3.1b1/hmmer-3.1b1-linux-intel-x86_64.tar.gz"
download=hmmer-3.1b1-linux-intel-x86_64.tar.gz
dir=hmmer-3.1b1-linux-intel-x86_64

echo "Installing ${dir}"
rm -r -f ${dir}
if [ ! -f ${download} ]; then
    wget -O ${download} ${url}
fi

tar -zxvf ${download}
cd ${dir}
./configure
make
make install








