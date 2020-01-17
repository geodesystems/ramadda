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

cat ${dir}/display/widgets.js ${dir}/display/display.js ${dir}/display/pointdata.js ${dir}/display/control.js ${dir}/display/notebook.js ${dir}/display/displaychart.js ${dir}/display/displayd3.js ${dir}/display/displaytext.js ${dir}/display/displayentry.js ${dir}/display/displayext.js ${dir}/display/displaymanager.js ${dir}/display/displaymap.js ${dir}/display/displaymap.js ${dir}/display/displaytable.js ${dir}/display/displayplotly.js > ${dest}/display_all.js
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
cp  ${dir}/style.css  ${dest}/style.min.css
${minify} ${dir}/ramaddamap.css > ${dest}/ramaddamap.min.css
##don't minify the display.css as it screws up how jquery styles are overridden
#${minify} ${dir}/display/display.css > ${dest}/display.min.css
cp ${dir}/display/display.css  ${dest}/display.min.css

