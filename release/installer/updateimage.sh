#!/bin/sh

#
#This script updates a RAMADDA AWS image
#It changes the keystore password and the install password
#


INSTALLER_DIR="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
source "${INSTALLER_DIR}/lib.sh"

host=`curl http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null`
printf "OK, the public IP of this machine is ${host}\n"


generate_keystore
generate_install_password

service ${SERVICE_NAME} restart

header "Image update completed";
printf "Finish the configuration at https://${host}/repository\n"
printf "The installation password is ${install_password}\n"



exit














