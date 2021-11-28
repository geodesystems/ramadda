#!/bin/sh

#
#This uses jsmin for minification
#to install do:
#pip install jsmin
#

dir=`dirname $0`
minify="python -m jsmin "
#yui="java -jar ${dir}/../../../../../lib/yuicompressor-2.4.8.jar"
dest="${dir}/min"

cat ${dir}/display/widgets.js ${dir}/display/display.js ${dir}/display/displaymanager.js ${dir}/display/pointdata.js   ${dir}/display/displaychart.js ${dir}/display/displayimages.js ${dir}/display/control.js ${dir}/display/notebook.js ${dir}/display/displayd3.js ${dir}/display/displaytext.js  ${dir}/display/displayentry.js ${dir}/display/displayext.js  ${dir}/display/displaymap.js ${dir}/display/editablemap.js ${dir}/display/displaymisc.js ${dir}/display/displaytable.js ${dir}/display/displayplotly.js ${dir}/display/displaythree.js > ${dest}/display_all.js
${minify} ${dest}/display_all.js > ${dest}/display_all.min.js
${minify} ${dest}/display_all.js > ${dest}/display_all.min.js
#${minify} ${dir}/display/displayplotly.js > ${dest}/displayplotly.min.js
${minify} ${dir}/ramadda.js > ${dest}/ramadda.min.js
${minify} ${dir}/ramaddamap.js > ${dest}/ramaddamap.min.js
${minify} ${dir}/utils.js > ${dest}/utils.min.js
${minify} ${dir}/wiki.js > ${dest}/wiki.min.js
${minify} ${dir}/repositories.js > ${dest}/repositories.min.js
${minify} ${dir}/selectform.js > ${dest}/selectform.min.js
${minify} ${dir}/entry.js > ${dest}/entry.min.js
${minify} ${dir}/lib/openlayers/v2/OpenLayers.debug.js > ${dir}/lib/openlayers/v2/OpenLayers.mini.js
cp  ${dir}/style.css  ${dir}/style.min.css
${minify} ${dir}/ramaddamap.css > ${dest}/ramaddamap.min.css
##don't minify the display.css as it screws up how jquery styles are overridden
#${minify} ${dir}/display/display.css > ${dest}/display.min.css
cp ${dir}/display/display.css  ${dest}/display.min.css


cat ${dir}/utils.js ${dir}/ramadda.js ${dir}/entry.js ${dir}/wiki.js > ${dir}/tmp.js
${minify} ${dir}/tmp.js > ${dest}/ramadda_all.min.js
rm ${dir}/tmp.js


cat \
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

