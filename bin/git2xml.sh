#!/bin/sh
#Author: Jeff McWhirter
#Take the output of this file and add it to RAMADDA as a CSV file.
#Once loaded go to the View>Convert Data menu and enter
#-xml commit
#-cut 1 
#-addheader "name.type enum date.format yyyy-MM-dd_space_HH:mm:ss_space_Z"
#Hit process to get a transformed file that RAMADDA can load as a Point Text file
#
echo "<commits><commit><files>" 
git log  --name-only --pretty=format:"</files></commit><commit><name>%cn</name><email>%ce</email><date>%ci</date><hash>%H</hash><comments><![CDATA[%B]]></comments><files>"  ${1}
echo "</files></commit></commits>" 


