#!/bin/sh

#
#This script installs RAMADDA on a Linux machine
#

export MYDIR="$(cd "$(dirname "$0")" && pwd)"


. "${MYDIR}/lib.sh"

install_service() {
    linux_install_service
}


export BASE_DIR=/mnt/ramadda
#This comes after setting BASE_DIR
init_env

#mount the volume if neede
if [ ! -d "$BASE_DIR" ]; then
    aws_do_mount;
else
    echo "$BASE_DIR already mounted"
fi

echo "RAMADDA Home dir:$RAMADDA_HOME_DIR"
mkdir -p $RAMADDA_HOME_DIR


tmpdir=`dirname $BASE_DIR`
permissions=$(stat -c %a $tmpdir)
if [ "$permissions" == "700" ]; then
    chmod 755 "$tmpdir"
fi




echo "Installing Java"
askYesNo "Do you want to install Java?"  "y"
if [ "$response" == "y" ]; then
    apt install openjdk-11-jdk
fi




askYesNo "Install postgres"  "y"
if [ "$response" == "y" ]; then
    install_postgres
fi

ask_install_ramadda
ask_keystore
generate_install_password

read -p "Should we open ports ${RAMADDA_HTTP_PORT} and ${RAMADDA_HTTPS_PORT} in the firewall? [y|n]: " response
if [ "$response" == "y" ]; then
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
	    echo "Could not find firewall-cmd or ufw

	fi
    fi
fi


printf "Starting RAMADDA"
systemctl start ${SERVICE_NAME}
do_finish_message

exit














