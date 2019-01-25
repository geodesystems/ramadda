#!/bin/sh

dir=`dirname $0`
yui="java -jar ${dir}/../../../../../lib/yuicompressor-2.4.8.jar"

cat ${dir}/display/utils.js ${dir}/display/display.js ${dir}/display/pointdata.js ${dir}/display/control.js ${dir}/display/displaychart.js ${dir}/display/displayd3.js ${dir}/display/displayentry.js ${dir}/display/displayext.js ${dir}/display/displaymanager.js ${dir}/display/displaymap.js ${dir}/display/displaytable.js ${dir}/display/displayplotly.js > ${dir}/display/display_all.js
${yui} ${dir}/display/display_all.js > ${dir}/display/display_all_mini.js
${yui} ${dir}/ramadda.js > ${dir}/ramadda.mini.js
${yui} ${dir}/ramaddamap.js > ${dir}/ramaddamap.mini.js
${yui} ${dir}/utils.js > ${dir}/utils.mini.js
${yui} ${dir}/wiki.js > ${dir}/wiki.mini.js
${yui} ${dir}/repositories.js > ${dir}/repositories.mini.js
${yui} ${dir}/selectform.js > ${dir}/selectform.mini.js
${yui} ${dir}/entry.js > ${dir}/entry.mini.js
${yui} ${dir}/style.css > ${dir}/style.mini.css
${yui} ${dir}/display.css > ${dir}/display.mini.css

