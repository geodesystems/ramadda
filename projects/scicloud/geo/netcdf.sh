
echo "Installing netcdf"
netcdfDest=/usr/local
netcdf=netcdf-4.3.3
rm -r -f ${netcdf}
if [ ! -f netcdf-4.3.3.zip ]; then
    wget ftp://ftp.unidata.ucar.edu/pub/netcdf/${netcdf}.zip
fi
unzip ${netcdf}.zip
cd ${netcdf}
./configure --prefix=${netcdfDest} --disable-netcdf-4
make check install
