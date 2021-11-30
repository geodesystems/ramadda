#!/bin/sh

#
#This uses jsmin and terser for minification
#to install do:
#pip install jsmin
#
#terser is a Node/NPM based package - https://github.com/terser/terser
#

dir=`dirname $0`
cssminify="python -m jsmin "

jsminify="terser --compress --mangle -- "
#If you don't want to install terser then set jsminify to
#jsminify="${cssminify}"

dest="${dir}/min"

now=`date`
echo "var build_date=\"RAMADDA build date: $now\";\n" > ${dir}/now.txt
cat ${dir}/now.txt ${dir}/display/widgets.js ${dir}/display/display.js ${dir}/display/displaymanager.js ${dir}/display/pointdata.js   ${dir}/display/displaychart.js ${dir}/display/displayimages.js ${dir}/display/control.js ${dir}/display/notebook.js ${dir}/display/displayd3.js ${dir}/display/displaytext.js  ${dir}/display/displayentry.js ${dir}/display/displayext.js  ${dir}/display/displaymap.js ${dir}/display/editablemap.js ${dir}/display/displaymisc.js  ${dir}/display/displayplotly.js ${dir}/display/displaythree.js ${dir}/display/displaytable.js > ${dest}/display_all.js
${jsminify} ${dest}/display_all.js > ${dest}/display_all.min.js
${jsminify} ${dest}/display_all.js > ${dest}/display_all.min.js
#${jsminify} ${dir}/display/displayplotly.js > ${dest}/displayplotly.min.js
${jsminify} ${dir}/ramadda.js > ${dest}/ramadda.min.js
${jsminify} ${dir}/ramaddamap.js > ${dest}/ramaddamap.min.js
${jsminify} ${dir}/utils.js > ${dest}/utils.min.js
${jsminify} ${dir}/wiki.js > ${dest}/wiki.min.js
${jsminify} ${dir}/repositories.js > ${dest}/repositories.min.js
${jsminify} ${dir}/selectform.js > ${dest}/selectform.min.js
${jsminify} ${dir}/entry.js > ${dest}/entry.min.js
${jsminify} ${dir}/lib/openlayers/v2/OpenLayers.debug.js > ${dir}/lib/openlayers/v2/OpenLayers.mini.js
cp  ${dir}/style.css  ${dir}/style.min.css
${cssminify} ${dir}/ramaddamap.css > ${dest}/ramaddamap.min.css
##don't minify the display.css as it screws up how jquery styles are overridden
#${cssminify} ${dir}/display/display.css > ${dest}/display.min.css
cp ${dir}/display/display.css  ${dest}/display.min.css


cat ${dir}/now.txt ${dir}/utils.js ${dir}/ramadda.js ${dir}/entry.js ${dir}/wiki.js > ${dir}/tmp.js
${jsminify} ${dir}/tmp.js > ${dest}/ramadda_all.min.js
rm ${dir}/tmp.js


cat \
${dir}/now.txt \
${dir}/lib/datatables/src/jquery.dataTables.min.js \
${dir}/lib/jquery/js/jquery.cookie.js \
${dir}/lib/jquery.easing.1.3.min.js \
${dir}/lib/jquery.bt.min.js \
${dir}/lib/jquery.ui.touch-punch.min.js \
${dir}/lib/superfish/js/superfish.min.js \
${dir}/lib/jbreadcrumb/js/jquery.jBreadCrumb.1.1.min.js \
${dir}/lib/selectboxit/javascripts/jquery.selectBoxIt.min.js \
${dir}/lib/fancybox-3/jquery.fancybox.min.js \
> ${dir}/min/jquery_lib_all.min.js


cat \
    ${dir}/lib/superfish/css/superfish.css \
    ${dir}/lib/selectboxit/stylesheets/jquery.selectBoxIt.css \
    ${dir}/lib/fancybox-3/jquery.fancybox.min.css \
    > ${dir}/min/lib_all.css
