#!/bin/sh

#
#This uses jsmin and terser for minification
#to install do:
#pip install jsmin
#
#terser is a Node/NPM based package - https://github.com/terser/terser
#

dir=`dirname $0`
cssminify="python3 -m jsmin "
jsminify="terser --compress --mangle -- "
#If you don't want to install terser then set jsminify to
#jsminify="${cssminify}"


dest="${dir}/min"

#echo "minifying bootstrap.css"
#${cssminify} ${dir}/lib/bootstrap-reduced/css/bootstrap.css > ${dir}/lib/bootstrap-reduced/css/bootstrap.min.css

#exit

now=`date`
echo "minifying display_all.js"
echo "var build_date=\"RAMADDA build date: $now\";\n" > ${dir}/now.txt
cat ${dir}/now.txt ${dir}/colortables.js ${dir}/display/widgets.js ${dir}/display/display.js ${dir}/display/displaymanager.js ${dir}/display/pointdata.js   ${dir}/display/displaychart.js ${dir}/display/displayimages.js ${dir}/display/control.js ${dir}/display/notebook.js ${dir}/display/displayd3.js ${dir}/display/displaytext.js  ${dir}/display/displayentry.js ${dir}/display/displayext.js  ${dir}/display/displaymap.js ${dir}/display/imdv.js ${dir}/display/displaymisc.js  ${dir}/display/displayplotly.js ${dir}/display/displaythree.js ${dir}/display/displaytable.js > ${dest}/display_all.js
${jsminify} ${dest}/display_all.js > ${dest}/display_all.min.js


echo "minifying the other files"
#${jsminify} ${dir}/display/displayplotly.js > ${dest}/displayplotly.min.js
${jsminify} ${dir}/ramadda.js > ${dest}/ramadda.min.js
${jsminify} ${dir}/ramaddamap.js > ${dest}/ramaddamap.min.js
${jsminify} ${dir}/utils.js > ${dest}/utils.min.js
${jsminify} ${dir}/wiki.js > ${dest}/wiki.min.js
${jsminify} ${dir}/repositories.js > ${dest}/repositories.min.js
${jsminify} ${dir}/selectform.js > ${dest}/selectform.min.js
${jsminify} ${dir}/entry.js > ${dest}/entry.min.js

echo "minifying openlayers"
${jsminify} ${dir}/lib/openlayers/v2/OpenLayers.debug.js > ${dir}/lib/openlayers/v2/OpenLayers.mini.js


cp  ${dir}/style.css  ${dir}/style.min.css

#Don't minify ramaddamap.css as it breaks some things
#${cssminify} ${dir}/ramaddamap.css > ${dest}/ramaddamap.min.css
cp  ${dir}/ramaddamap.css  ${dest}/ramaddamap.min.css

##don't minify the display.css as it screws up how jquery styles are overridden
#${cssminify} ${dir}/display/display.css > ${dest}/display.min.css
cp ${dir}/display/display.css  ${dest}/display.min.css


echo "minifying ramadda_all"
# ${dir}/wiki.js
cat ${dir}/now.txt ${dir}/utils.js ${dir}/ramadda.js ${dir}/entry.js > ${dir}/tmp.js
${jsminify} ${dir}/tmp.js > ${dest}/ramadda_all.min.js
rm ${dir}/tmp.js

##Not needed for now
#${dir}/lib/jquery.bt.min.js \
#${dir}/lib/fancybox-3/jquery.fancybox.min.js \
##${dir}/lib/datatables/src/jquery.dataTables.min.js \
##${dir}/lib/selectboxit/javascripts/jquery.selectBoxIt.min.js \
##${jsminify} ${dir}/lib/superfish-1.7.10/js/superfish.js > ${dir}/lib/superfish-1.7.10/js/superfish.min.js


echo "making jquery_lib_all.min.js"
cat \
${dir}/now.txt \
${dir}/lib/jquery.cookie.js \
${dir}/lib/jquery.easing.1.4.1.min.js \
${dir}/lib/jquery.ui.touch-punch.min.js \
${dir}/lib/superfish-1.7.10/js/superfish.min.js \
${dir}/lib/jbreadcrumb/js/jquery.jBreadCrumb.1.1.min.js \
${dir}/lib/jquery.scrollintoview.min.js \
${dir}/lib/dom-drag.min.js \
> ${dir}/min/jquery_lib_all.min.js


#    ${dir}/lib/fancybox-3/jquery.fancybox.min.css \
cat \
    ${dir}/lib/superfish-1.7.10/css/superfish.css \
    ${dir}/lib/selectboxit/stylesheets/jquery.selectBoxIt.css \
    > ${dir}/min/lib_all.css
