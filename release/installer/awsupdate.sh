#!/bin/sh

#
#This reinstalls RAMADDA on AWS Linux. It assumes ramadda has been installed as a service
#Run this from the ramaddainstall directory, i.e., the directory that holds the
#ramaddaserver directory
#

#get the latest release
wget  -O ramaddaserver.zip https://ramadda.org/repository/entry/get/ramaddaserver.zip?entryid=synth%3A498644e1-20e4-426a-838b-65cffe8bd66f%3AL3JhbWFkZGFzZXJ2ZXIuemlw

#stop ramadda
service ramadda stop 

#install the new ramadda
rm -r -f ramaddaserver
unzip ramaddaserver.zip

#start ramadda
service ramadda start 
