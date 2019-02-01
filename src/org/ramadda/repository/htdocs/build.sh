#!/bin/sh

dir=`dirname $0`
yui="java -jar ${dir}/../../../../../lib/yuicompressor-2.4.8.jar"
dest="${dir}/min"

cat ${dir}/display/widgets.js ${dir}/display/display.js ${dir}/display/pointdata.js ${dir}/display/control.js ${dir}/display/displaychart.js ${dir}/display/displayd3.js ${dir}/display/displayentry.js ${dir}/display/displayext.js ${dir}/display/displaymanager.js ${dir}/display/displaymap.js ${dir}/display/displaytable.js > ${dest}/display_all.js
${yui} ${dest}/display_all.js > ${dest}/display_all.min.js
${yui} ${dest}/display_all.js > ${dest}/display_all.min.js
${yui} ${dir}/display/displayplotly.js > ${dest}/displayplotly.min.js
${yui} ${dir}/ramadda.js > ${dest}/ramadda.min.js
${yui} ${dir}/ramaddamap.js > ${dest}/ramaddamap.min.js
${yui} ${dir}/utils.js > ${dest}/utils.min.js
${yui} ${dir}/wiki.js > ${dest}/wiki.min.js
${yui} ${dir}/repositories.js > ${dest}/repositories.min.js
${yui} ${dir}/selectform.js > ${dest}/selectform.min.js
${yui} ${dir}/entry.js > ${dest}/entry.min.js
${yui} ${dir}/style.css > ${dest}/style.min.css
${yui} ${dir}/ramaddamap.css > ${dest}/ramaddamap.min.css
${yui} ${dir}/display/display.css > ${dest}/display.min.css

