echo "purging display_all.min.js"
wget -o jsdelivr.json https://purge.jsdelivr.net/gh/geodesystems/ramadda@latest/src/org/ramadda/repository/htdocs/min/display_all.min.js
echo "purging ramadda_all.min.js"
wget -o jsdelivr.json https://purge.jsdelivr.net/gh/geodesystems/ramadda@latest/src/org/ramadda/repository/htdocs/min/ramadda_all.min.js
echo "purging style.min.css"
wget -o jsdelivr.json https://purge.jsdelivr.net/gh/geodesystems/ramadda@latest/src/org/ramadda/repository/htdocs/style.min.css
echo "purging style.css"
wget -o jsdelivr.json https://purge.jsdelivr.net/gh/geodesystems/ramadda@latest/src/org/ramadda/repository/htdocs/style.css
exit

