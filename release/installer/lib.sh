#!/bin/sh




#
#basic shared initialization
#

#set -e

export INSTALLER_DIR="$(cd "$(dirname "$0")" && pwd)"
export MYDIR="$(cd "$(dirname "$0")" && pwd)"
export SERVICE_NAME="ramadda"
export SERVICE_DIR="/etc/rc.d/init.d"
export RAMADDA_HTTP_PORT=80
export RAMADDA_HTTPS_PORT=443

export AWS_BASE_DIR=/mnt/ramadda
export RAMADDA_BASE_DIR="$AWS_BASE_DIR"
export OS_REDHAT="redhat"
export OS_AMAZON="amazon_linux"

##For now only amazon linux is supported 
export os=$OS_AMAZON


export RAMADDA_DOWNLOAD="https://ramadda.org/repository/release/latest/ramaddaserver.zip"
export PARENT_DIR=`dirname $INSTALLER_DIR`
export YUM_ARG=""
export promptUser=1



fix_localhost_name() {
    echo "Fixing the localhost name problem"
    sed -e 's/HOSTNAME=localhost.localdomain/HOSTNAME=ramadda.localdomain/g' /etc/sysconfig/network> dummy.network
    mv dummy.network /etc/sysconfig/network
    sed -e 's/127.0.0.1   localhost localhost.localdomain/127.0.0.1 ramadda.localdomain ramadda localhost localhost.localdomain/g' /etc/hosts> dummy.hosts
    mv dummy.hosts /etc/hosts
}



check_firewall() {
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




random_number() {
    number=$(od -An -N2 -i /dev/urandom | awk '{print $1 % 10000 + 1}')
    echo $number
}






ask_base_dir() {
    header  "RAMADDA Base Directory"
    printf "RAMADDA needs two directories- ramaddainstall and ramaddahome\n"
    printf  "Where should these directories be created under?\n"
    ask  "Enter \".\" for current directory. Default: [${RAMADDA_BASE_DIR}]"  ${RAMADDA_BASE_DIR}
    if [ "$response" = "." ]; then
	response=`pwd`
    fi

    export RAMADDA_BASE_DIR="$response"
    ##fix the problem with /home/ec2-user not being readable to the postgres user
    if [ -d "${RAMADDA_BASE_DIR}" ]; then
	chmod +rx "${RAMADDA_BASE_DIR}"
    fi
}


init_env() {
    ask_base_dir
    export USER_DIR=$PARENT_DIR
    export RAMADDA_HOME_DIR=${RAMADDA_BASE_DIR}/ramaddahome
    export RAMADDA_INSTALL_DIR=${RAMADDA_BASE_DIR}/ramaddainstall
    export RAMADDA_SERVER_DIR=${RAMADDA_INSTALL_DIR}/ramaddaserver
    export MOUNT_DIR=""
    echo "Ok, RAMADDA will be installed under: $RAMADDA_BASE_DIR with the following directories:"
    echo "${RAMADDA_INSTALL_DIR} - where the RAMADDA server will be installed"
    echo "${RAMADDA_HOME_DIR} - where RAMADDA stores its configuration, files,  etc"

}






do_basedir() {
    dfltDir="";
    if [ -d "${USER_DIR}" ]; then
	dfltDir="${USER_DIR}/ramadda";
    fi

    if [ -d "${AWS_BASE_DIR}" ]; then
	dfltDir="${AWS_BASE_DIR}";
    fi

    while [ "$RAMADDA_BASE_DIR" = "" ]; do
	ask   "Enter base directory: [$dfltDir]:" $dfltDir  "The base directory holds the repository and pgsql sub-directories"
	if [ "$response" = "" ]; then
            break;
	fi
	RAMADDA_BASE_DIR=$response;
	break
    done
}



usage() {
    echo "installer.sh -os redhat -y (assume yes installer) -help "
    exit
}

header() {
    local msg="$1"
    printf "\n*** ${msg} ***\n";
}


yumInstall() {
    local target="$1"
    if [ "$YUM_ARG" = "" ]; then
	yum install ${target}
    else 
	yum install ${YUM_ARG} ${target}
    fi
}

pause() {
    local msg="$1"
    if [ "$msg" = "" ]; then
	msg="pause "
    fi
    read -p "${msg}"
}

askYesNo() {
    local msg="$1"
    local dflt="$2"

    if [ $promptUser = 0 ]; then
	response="$dflt";
	return;
    fi

    read -p "${msg}?  [y|A(all)|n]: " response
    if [ "$response" = "A" ]; then
	promptUser=0;
	response="$dflt";
	return;
    fi

    case $response in y|Y) 
            response='y'
            ;;  
	"")
	    response="$dflt";
	    ;;

        *) response='n'
            ;;
    esac

    if [ "$response" = "" ]; then
	response="$dflt";
    fi
    if [ "$response" = "" ]; then
	response="n";
    fi

}

ask() {
    local msg="$1";
    local dflt="$2";
    local extra="$3"
    if [ $promptUser -eq 0 ]; then
	response="$dflt";
        return;
    fi

    if [ "$extra" != "" ]; then
        printf "\n# $extra\n"
    fi

    read -p "${msg} " response;

    if [ "$response" = "" ]; then
	response="$dflt";
    fi
}


#
# parse the args
#
while [ $# != 0 ]
do
    case $1 in 
	-os)
	    shift
	    os=$1;
	    ;;
	-help)
	    usage
	    ;;
	-y)
	    promptUser=0
	    YUM_ARG=--assumeyes
	    ;;
	*)
	    echo "Unknown argument $1"
	    usage
	    ;;
    esac
    shift
done


if [ "$os" = "${OS_REDHAT}" ]; then
    export PG_SERVICE=postgresql-server
    export PG_INSTALL=http://yum.postgresql.org/9.3/redhat/rhel-6-x86_64/pgdg-redhat93-9.3-1.noarch.rpm
else
    export PG_SERVICE=postgresql.service
    export PG_INSTALL=postgresql-server
fi


do_main_install() {
    init_env


    #mount the volume if needed
    if [ ! -d "$RAMADDA_BASE_DIR" ]; then
	aws_do_mount;
    fi

    echo "RAMADDA Home dir:$RAMADDA_HOME_DIR"
    mkdir -p $RAMADDA_HOME_DIR


    tmpdir=`dirname $RAMADDA_BASE_DIR`
    permissions=$(stat -c %a $tmpdir)
    if [ "$permissions" = "700" ]; then
	chmod 755 "$tmpdir"
    fi

    ask_install_java
    ask_postgres
    fix_localhost_name
    ask_install_ramadda
    check_firewall
    ask_keystore
    generate_install_password
    start_service
    do_finish_message
}


install_java() {
    if command -v "yum" &> /dev/null ; then
	yum install -y java
	sudo /usr/sbin/alternatives --config java
    else
	apt update
	apt install openjdk-11-jdk
    fi
}


ask_install_java() {
    echo "Installing Java"
    askYesNo "Do you want to install Java?"  "y"
    if [ "$response" = "y" ]; then
	install_java
    fi
}


ask_postgres()  {
    header "Postgres install"
    echo "RAMADDA can run with Postgres or it's own built in Derby database"
    askYesNo "Do you want to install and use postgres?"  "y"
    if [ "$response" = "y" ]; then
	install_postgres
    fi
}


install_postgres() {
    export PG_DIR=/var/lib/pgsql
    export PG_HBA=${PG_DIR}/data/pg_hba.conf
    export PG_REAL_DIR="${RAMADDA_BASE_DIR}/pgsql"

    echo "Installing PostgreSQL 15 with:"
    printf "\tsudo dnf install postgresql15.x86_64 postgresql15-server -y"
    sudo dnf install postgresql15.x86_64 postgresql15-server -y
    #sudo apt install postgresql
    if  [ ! -f ${PG_HBA} ] ; then
	echo "setting up postgres"
	postgresql-setup --initdb
    fi

    ##if /var/lib/pgsql exists and it isn't a link
    if  [ -d ${PG_DIR} ]; then 
	if [ ! -h ${PG_DIR} ]; then 
	    if  [  -d ${PG_REAL_DIR} ] ; then
		echo "Looks like ${PG_REAL_DIR} already exists. Moving ${PG_DIR} to ${PG_DIR}.bak"
		mv ${PG_DIR} ${PG_DIR}.bak
	    else
		echo "Moving ${PG_DIR} to $PG_REAL_DIR"
		mv  ${PG_DIR} $PG_REAL_DIR
	    fi
	    echo "linking $PG_DIR to $PG_REAL_DIR"
	    ln  -s -f  $PG_REAL_DIR ${PG_DIR}
	else
	    echo "Looks like ${PG_DIR} is already linked to ${PG_REAL_DIR}"
	fi
    else
	echo "Warning: ${PG_DIR} does not exist"	
	exit
    fi

    chown -R postgres ${PG_DIR}
    chown -R postgres ${PG_REAL_DIR}



    PG_PASSWORD="password$(random_number)-$(random_number)"
    PG_USER="ramadda"

    ls ${PG_HBA}


    if [ ! -f ${PG_HBA}.bak ]; then
	#If we haven't updated pg_hba then 
	echo "adding $PG_USER to  ${PG_HBA}"
        cp ${PG_HBA} ${PG_HBA}.bak
	postgresAuth="
#	
#written out by the RAMADDA installer
#
host repository ${PG_USER} 127.0.0.1/32  password
#For now add this as I can't get the password to work
local   repository      ${PG_USER}                                  trust
local   all             all                                     peer
host    all             all             127.0.0.1/32            ident
host    all             all             ::1/128                 ident
"


	printf "${postgresAuth}" > ${PG_HBA}
    fi

    echo "adding postgres service"
    systemctl enable ${PG_SERVICE}
    systemctl start ${PG_SERVICE}

    
    printf "create database repository;\ncreate user ramadda;\nalter user ramadda with password '${PG_PASSWORD}';\ngrant all privileges on database repository to ramadda;  ALTER DATABASE repository OWNER TO ramadda;ALTER SCHEMA public OWNER TO ramadda;\n" > /tmp/postgres.sql
    chmod 644 /tmp/postgres.sql
    echo "Creating repository database, adding ramadda user and setting privileges and password"
    su -c "psql -f /tmp/postgres.sql"  - postgres > /dev/null
    rm -f ${INSTALLER_DIR}/postgres.sql
    echo "writing properties to ${RAMADDA_HOME_DIR}/db.properties"
    printf "ramadda.db=postgres\nramadda.db.postgres.user=ramadda\nramadda.db.postgres.password=${PG_PASSWORD}"  > ${RAMADDA_HOME_DIR}/db.properties

    
}



download_installer() {
    rm -f ${INSTALLER_DIR}/ramaddaserver.zip
    echo "Downloading RAMADDA from ${RAMADDA_DOWNLOAD}"
    wget -O ${INSTALLER_DIR}/ramaddaserver.zip ${RAMADDA_DOWNLOAD}
}

install_ramadda() {
    rm -r -f ${RAMADDA_SERVER_DIR}
    unzip -d ${RAMADDA_INSTALL_DIR} -o ${INSTALLER_DIR}/ramaddaserver.zip
    export SERVICE_SCRIPT=${RAMADDA_SERVER_DIR}/ramaddaService.sh    

    ask "What HTTP port should RAMADDA run on? [${RAMADDA_HTTP_PORT}]:" ${RAMADDA_HTTP_PORT}
    export  RAMADDA_HTTP_PORT=$response

    ask "What HTTPS port should RAMADDA run on? [${RAMADDA_HTTPS_PORT}]:" ${RAMADDA_HTTPS_PORT}
    export  RAMADDA_HTTPS_PORT=$response	


    ramaddaConfig="##
## Generated by the RAMADDA installer
## This gets sourced by the ramaddaserver/ramadda.sh script
##

##RAMADDA home directory
export RAMADDA_HOME=${RAMADDA_HOME_DIR}

##Port RAMADDA runs on
export RAMADDA_PORT=${RAMADDA_HTTP_PORT}

##Uncomment this to change which Java is used:
#export JAVA=java                                                                                  

##Uncomment this to set the memory to be used
#JAVA_MEMORY=2056m

"
    printf "$ramaddaConfig" > ${RAMADDA_INSTALL_DIR}/ramaddaenv.sh
}

ask_ip(){
    printf "We need the public IP address to configure SSL\n"
    read -p "Are you running in Amazon AWS? [y|n]: " response
    if [ "$response" = "y" ]; then
	export MYIP=`curl http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null`
	printf "OK, the public IP of this machine is ${MYIP}\n"
    else
	export MYIP=`ifconfig | grep inet | grep cast | awk '/inet addr/{print substr($2,6)}'`
	if [ "$MYIP" = "" ]; then
	    export MYIP=`ifconfig | grep inet | grep cast | awk '/.*/{print $2}'`
	fi
	read -p "Is this IP address correct - ${MYIP}?  [y|n]: " response
	if [ "$response" = "n" ]; then
	    read -p "Enter the IP address: " tmpip
	    export MYIP="${tmpip}"
	fi
    fi
}    



ask_install_ramadda() {
    header  "RAMADDA Installation"
    askYesNo "Download and install RAMADDA from Geode Systems"  "y"
    if [ "$response" = "y" ]; then
	download_installer

	ask  "Where should RAMADDA be installed? [${RAMADDA_INSTALL_DIR}]:"  "${RAMADDA_INSTALL_DIR}"
	export RAMADDA_INSTALL_DIR=$response
	install_ramadda
	askYesNo "Install RAMADDA as a service"  "y"
	if [ "$response" = "y" ]; then
	    install_service
	fi
    fi
}

install_service() {
    if command -v "service" &> /dev/null ; then
	aws_install_service
    else
	linux_install_service
    fi
}

start_service() {
    printf "Starting RAMADDA"
    if command -v "service" &> /dev/null ; then
	service ${SERVICE_NAME} restart
    else
	systemctl start ${SERVICE_NAME}
    fi
}




aws_install_service() {
    printf "#!/bin/sh\n# chkconfig: - 80 30\n# description: RAMADDA repository\n\nsh ${SERVICE_SCRIPT} \"\$@\"\n" > ${SERVICE_DIR}/${SERVICE_NAME}
    chmod 755 ${SERVICE_DIR}/${SERVICE_NAME}
    chkconfig ${SERVICE_NAME} on
    printf "To run the RAMADDA service do:\nsudo service ${SERVICE_NAME} start|stop|restart\n"
    printf "Service script is: ${SERVICE_SCRIPT}\n"
}

linux_install_service() {
service="[Unit]
Description=RAMADDA

[Service]
ExecStart=/usr/bin/bash ${SERVICE_SCRIPT} start wait
ExecStop=/user/bin/bash ${SERVICE_SCRIPT} stop
Restart=no
StandardOutput=syslog
StandardError=syslog
SyslogIdentifier=ramadda
User=root

[Install]
WantedBy=multi-user.target
"
printf "$service" > /etc/systemd/system/ramadda.service
systemctl daemon-reload
systemctl enable ramadda
printf "To run the RAMADDA service do:\nsudo systemctl start ramadda\nsudo systemctl stop ramadda\n"
printf "Service script is: ${SERVICE_SCRIPT}\n"
}


ask_keystore() {
    header "SSL Configuration";
    printf "A self-signed SSL certificate can be created\n";
    printf "This will enable you to access your server securely but you will need to\n";
    printf "add a real certificate or add other entries to the keystore for other domain names\n";
    askYesNo "Generate keystore and enable SSL" "y"
    if [ "$response" = "y" ]; then
	generate_keystore
    fi
}




generate_keystore() {
    password="ssl_$(random_number)_$(random_number)_$(random_number)"
    echo "Generating new keystore file: ${RAMADDA_HOME_DIR}/keystore"
    echo "The password is stored in ${RAMADDA_HOME_DIR}/ssl.properties"
    rm -f ${RAMADDA_HOME_DIR}/keystore
    printf "${password}\n${password}\nRAMADDA\nRAMADDA\nRAMADDA\ncity\nstate\ncountry\nyes\n\n" | keytool -genkey -keyalg RSA -alias ramadda -keystore ${RAMADDA_HOME_DIR}/keystore > /dev/null 2> /dev/null
    printf "#generated password\n\nramadda.ssl.password=${password}\nramadda.ssl.keypassword=${password}\nramadda.ssl.port=${RAMADDA_HTTPS_PORT}\n" > ${RAMADDA_HOME_DIR}/ssl.properties
    printf "\nIf you need to create a new key then delete ${RAMADDA_HOME_DIR}/keystore and run:\n    keytool -genkey -keyalg RSA -alias ramadda -keystore ${RAMADDA_HOME_DIR}/keystore\nIf you are installing your own certificate then generate the keystore and copy it to ${RAMADDA_HOME_DIR}\n"
    printf "****\nNote: since this is a self-signed certificate your browser will show that this is an insecure connection\n*****\n"
    printf "\n"
}


generate_install_password() {
    export install_password="$(random_number)_$(random_number)"
    printf  "ramadda.install.password=${install_password}" > ${RAMADDA_HOME_DIR}/install.properties
}






aws_do_mount() {
    header  "Volume Installation";
    echo "The database and the RAMADDA home directory will be installed on ${AWS_BASE_DIR}"
    echo "We need to mount a volume as ${AWS_BASE_DIR}"
    dirs="/dev/foo /dev/sdb /dev/bar"
    for dir in $dirs; do
	if [ -b "$dir" ]; then
            askYesNo  "Do you want to mount the volume: $dir "  "y"
            if [ "$response" = "y" ]; then
		MOUNT_DIR="$dir"
		break;
            fi
	fi
    done

    ##/dev/xvdb       ${AWS_BASE_DIR}   ext4    defaults,nofail        0       2
    while [ "$MOUNT_DIR" = "" ]; do
	ask  "Enter the volume to mount, e.g., /dev/xvdb  [<volume>|n] "  ""
	if [ "$response" = "" ] ||  [ "$response" = "n"  ]; then
            break;
	fi
	if [ -b $response ]; then
            MOUNT_DIR="$response"
            break;
	fi
	echo "Volume does not exist: $response"
    done

    echo "Mounting: $MOUNT_DIR"

    if [ "$MOUNT_DIR" != "" ]; then
	mntState=$( file -s $MOUNT_DIR );
	case $mntState in
	    *files*)
		echo "$MOUNT_DIR is already mounted";
		;;
	    *)
		echo "Mounting $RAMADDA_BASE_DIR on $MOUNT_DIR"
		if [ ! -f /etc/fstab.bak ]; then
		    cp  /etc/fstab /etc/fstab.bak
		fi
		sed -e 's/.*$RAMADDA_BASE_DIR.*//g' /etc/fstab | sed -e 's/.*added ramadda.*//g' > dummy.fstab
		mv dummy.fstab /etc/fstab
		printf "\n#added by ramadda installer.sh\n${MOUNT_DIR}   $RAMADDA_BASE_DIR ext4 defaults,nofail   0 2\n" >> /etc/fstab
		askYesNo "Do you want to make the file system on ${MOUNT_DIR}?"  "y"
		if [ "$response" = "y" ]; then
		    mkfs -t ext4 $MOUNT_DIR
		fi
		mkdir $RAMADDA_BASE_DIR
		mount $MOUNT_DIR $RAMADDA_BASE_DIR
		mount -a
		;;
	esac
    fi
}



do_finish_message() {
    header "Installation complete";
    printf "RAMADDA is installed. \n\tRAMADDA home directory: ${RAMADDA_HOME_DIR}\n\tLog file: ${RAMADDA_INSTALL_DIR}/ramadda.log\n"
    printf "Finish the configuration at https://<your IP>:${RAMADDA_HTTPS_PORT}/repository\n"
    printf "The installation password is ${install_password}\n"
}

