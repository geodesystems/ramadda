echo "Installing MB system"
rm -r -f mbsystem
if [ ! -f MB-System.tar ]; then
    echo "Fetching MB system"
    wget ftp://ftp.ldeo.columbia.edu/pub/MB-System/MB-System.tar.gz
    gunzip MB-System.tar.gz
fi
tar -xvf MB-System.tar
mv mbsystem-* mbsystem
cd mbsystem
./configure
make
make install
