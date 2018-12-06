dir=`dirname $0`
yui="java -jar ${dir}/../../../../../lib/yuicompressor-2.4.8.jar"

cat display/utils.js display/display.js display/pointdata.js display/control.js display/displaychart.js display/displayd3.js display/displayentry.js display/displayext.js display/displaymanager.js display/displaymap.js display/displaytable.js > display/display_all.js
${yui} display/display_all.js > display/display_all_mini.js
${yui} ramadda.js > ramadda.mini.js
${yui} ramaddamap.js > ramaddamap.mini.js
${yui} utils.js > utils.mini.js
${yui} wiki.js > wiki.mini.js
${yui} repositories.js > repositories.mini.js
${yui} selectform.js > selectform.mini.js
${yui} entry.js > entry.mini.js
${yui} style.css > style.mini.css
${yui} display.css > display.mini.css

