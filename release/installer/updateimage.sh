#!/bin/sh

#
#This script updates a RAMADDA AWS image
#It changes the keystore password and the install password
#


MYDIR=`dirname $0`
source "${MYDIR}/lib.sh"

export MYIP=`curl http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null`

service ${SERVICE_NAME} stop

##update with latest ramadda
download_installer
install_ramadda

generate_keystore
generate_install_password

service ${SERVICE_NAME} start

header "Image update completed";
printf "Finish the configuration at https://${MYIP}/repository\n"
printf "The installation password is ${install_password}\n"



exit














