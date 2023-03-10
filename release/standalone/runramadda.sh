#!/bin/sh
#This script is intended to be copied from the RAMADDA install directory
#so that the install directory can be re-installed without losing settings

#Set the RAMADDA_HOME and RAMADDDA_INSTALL environment variables

export RAMADDA_HOME=/Users/jeffmc/foo
export RAMADDA_INSTALL=/Users/jeffmc/source/ramadda/dist/ramaddaserver/

#You can also specify the port, path to java
#export RAMADDA_PORT=8080
#export JAVA=/path/to/java
#export JAVA_MEMORY=2056m


sh ${RAMADDA_INSTALL}/ramadda.sh $*








