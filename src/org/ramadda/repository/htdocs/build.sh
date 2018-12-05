yui="java -jar /Users/jeffmc/bin/yuicompressor-2.4.8.jar"
cd display
cat utils.js display.js pointdata.js control.js displaychart.js displayd3.js displayentry.js displayext.js displaymanager.js displaymap.js displaytable.js > display_all.js
${yui} display_all.js > display_all_mini.js
cd ../
${yui} ramadda.js > ramadda.mini.js
${yui} ramaddamap.js > ramaddamap.mini.js
${yui} utils.js > utils.mini.js
${yui} wiki.js > wiki.mini.js
${yui} repositories.js > repositories.mini.js
${yui} selectform.js > selectform.mini.js
${yui} entry.js > entry.mini.js

