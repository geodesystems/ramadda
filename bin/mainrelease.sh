#!/bin/sh

#this script copies over the RAMADDA build  products to the release directory.
#This directory is then served up by RAMADDA at:
#https://geodesystems.com/repository/alias/release/


#This is where the released files are stored
#dest=/mnt/ramadda/release/ramadda_6.0
dest=/mnt/ramadda/release/latest

#Where the build products are put
dist=/mnt/ramadda/source/ramadda/dist


mkdir -p $dest
mkdir -p $dest/plugins
echo "copying plugins"
scp ${dist}/plugins/coreplugins/* ${dist}/plugins/bioplugins/* ${dist}/plugins/geoplugins/* ${dist}/plugins/miscplugins/*   ${dist}/plugins/coreplugins.jar ${dist}/plugins/miscplugins.jar ${dist}/plugins/bioplugins.jar ${dist}/plugins/geoplugins.jar ${dist}/plugins/projectplugins/* ${dest}/plugins

#cp the top level build products
echo "copying core"
scp ${dist}/ramaddainstaller.zip  ${dist}/ramaddaserver.zip   ${dist}/repository.war ${dist}/seesv.zip ${dest}





