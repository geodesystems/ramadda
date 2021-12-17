sh ${RAMADDA_GIT_ROOT}/bin/doall.sh
tclsh ${RAMADDA_GIT_ROOT}/bin/changeminor.tcl
cd ${RAMADDA_GIT_ROOT} 
sh ${ANT_HOME} -q tag  "$1"
sh ${RAMADDA_BIN}/makerelease.sh ${GEODESYSTEMS_IP}
