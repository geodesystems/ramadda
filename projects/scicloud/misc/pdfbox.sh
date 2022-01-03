#!/bin/sh

echo "Installing pdfbox"
bindir=/home/ec2-user/bin
pdfbox=pdfbox-app-1.8.8.jar
if [ ! -f ${bindir}/${pdfbox} ]; then
    wget ftp://apache.cs.utah.edu/apache.org//pdfbox/1.8.8/${pdfbox}
    mv ${pdfbox} ${bindir}
fi
echo "#!/bin/sh
java -jar ${bindir}/${pdfbox} \"\$@\"
" > ${bindir}/pdfbox.sh
chmod 755 ${bindir}/pdfbox.sh




