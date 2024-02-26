#!/bin/sh

#
#This script installs RAMADDA on a Linux machine
#

export MYDIR="$(cd "$(dirname "$0")" && pwd)"


. "${MYDIR}/lib.sh"

install_service() {
    linux_install_service
}

install_postgres() {
    echo "Not implemented yet"
}

install_java() {
    apt update
    apt install openjdk-11-jdk
}

start_service() {
    printf "Starting RAMADDA"
    systemctl start ${SERVICE_NAME}
}

install_step2() {
    read -p "Should we open ports ${RAMADDA_HTTP_PORT} and ${RAMADDA_HTTPS_PORT} in the firewall? [y|n]: " response
    if [ "$response" = "y" ]; then
	if command -v "ufw" &> /dev/null ; then
	    ufw allow ${RAMADDA_HTTP_PORT}/tcp
	    ufw allow ${RAMADDA_HTTPS_PORT}/tcp	
	    ufw reload
	else
	    if command -v "firewall-cmd" &> /dev/null ; then
		firewall-cmd --add-port=${RAMADDA_HTTP_PORT}/tcp --permanent
		firewall-cmd --add-port=${RAMADDA_HTTPS_PORT}/tcp --permanent
		firewall-cmd --reload
	    else
		echo "Could not find firewall-cmd or ufw"

	    fi
	fi
    fi
}

do_main_install

exit
