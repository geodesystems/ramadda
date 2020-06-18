#!/bin/sh

#
#This script installs some base packages, Postgres and then RAMADDA
#


OS_REDHAT="redhat"
OS_AMAZON="amazon_linux"
os=$OS_AMAZON

INSTALLER_DIR="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
PARENT_DIR=`dirname $INSTALLER_DIR`
USER_DIR=$PARENT_DIR
SERVICE_NAME="ramadda"
RAMADDA_INSTALL_DIR=${PARENT_DIR}/${SERVICE_NAME}
SERVER_DIR=$RAMADDA_INSTALL_DIR/ramaddaserver
SERVICE_SCRIPT=${SERVER_DIR}/ramaddaService.sh


BASEDIR=/mnt/ramadda

promptUser=1
yumArg=""

mkdir -p  ${RAMADDA_INSTALL_DIR}

RAMADDA_DOWNLOAD="https://geodesystems.com/repository/release/latest/ramaddaserver.zip"

SERVICE_DIR="/etc/rc.d/init.d"



usage() {
    echo "installer.sh -os redhat -y (assume yes installer) -help "
    exit
}

header() {
    local msg="$1"
    printf "\n*** ${msg} ***\n";
}


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
	    yumArg=--assumeyes
	    ;;
	*)
	    echo "Unknown argument $1"
	    usage
	    ;;
    esac
    shift
done

#echo "target os: $os"

if [ "$os" == "${OS_REDHAT}" ]; then
    pgsql=pgsql
    pgService=postgresql-server
    pgInstall=http://yum.postgresql.org/9.3/redhat/rhel-6-x86_64/pgdg-redhat93-9.3-1.noarch.rpm
else
    pgsql=pgsql
    pgService=postgresql
    pgInstall=postgresql-server
fi




yumInstall() {
    local target="$1"
    if [ "$yumArg" == "" ]; then
	yum install ${target}
    else 
	yum install ${yumArg} ${target}
    fi
}

askYesNo() {
    local msg="$1"
    local dflt="$2"

    if [ $promptUser == 0 ]; then
	response="$dflt";
	return;
    fi

    read -p "${msg}?  [y|A(all)|n]: " response
    if [ "$response" == "A" ]; then
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

    if [ "$response" == "" ]; then
	response="$dflt";
    fi
    if [ "$response" == "" ]; then
	response="n";
    fi

}

ask() {
    local msg="$1";
    local dflt="$2";
    local extra="$3"
    if [ $promptUser == 0 ]; then
	response="$dflt";
        return;
    fi

    if [ "$extra" != "" ]; then
        printf "\n# $extra\n"
    fi

    read -p "${msg} " response;

    if [ "$response" == "" ]; then
	response="$dflt";
    fi
}

mntDir=""

domnt() {
    header  "Volume Installation";
    echo "The database and the RAMADDA home directory will be installed on /mnt/ramadda"
    echo "We need to mount a volume as /mnt/ramadda"
    declare -a dirLocations=("/dev/xvdb" )
    for i in "${dirLocations[@]}"
    do
	if [ -b "$i" ]; then
            askYesNo  "Do you want to mount the volume: $i "  "y"
            if [ "$response" == "y" ]; then
		mntDir="$i"
		break;
            fi
	fi
    done


    ##/dev/xvdb       /mnt/ramadda   ext4    defaults,nofail        0       2
    while [ "$mntDir" == "" ]; do
	ask  "Enter the volume to mount, e.g., /dev/xvdb  [<volume>|n] "  ""
	echo "RES: $response"
	if [ "$response" == "" ] ||  [ "$response" == "n"  ]; then
            break;
	fi
	if [ -b $response ]; then
            mntDir="$response"
            break;
	fi
	echo "Volume does not exist: $response"
    done

    echo "MNT: $mntDir"

    if [ "$mntDir" != "" ]; then
	mntState=$( file -s $mntDir );
	case $mntState in
	    *files*)
		echo "$mntDir is already mounted";
		;;
	    *)
		echo "Mounting $BASEDIR on $mntDir"
		if [ ! -f /etc/fstab.bak ]; then
		    cp  /etc/fstab /etc/fstab.bak
		fi
		sed -e 's/.*$BASEDIR.*//g' /etc/fstab | sed -e 's/.*added ramadda.*//g' > dummy.fstab
		mv dummy.fstab /etc/fstab
		printf "\n#added by ramadda installer.sh\n${mntDir}   $BASEDIR ext4 defaults,nofail   0 2\n" >> /etc/fstab
		mkfs -t ext4 $mntDir
		mkdir $BASEDIR
		mount $mntDir $BASEDIR
		mount -a
		;;
	esac
    fi
}


dobasedir() {
    dfltDir="";
    if [ -d "${USER_DIR}" ]; then
	dfltDir="${USER_DIR}/ramadda";
    fi

    if [ -d "/mnt/ramadda" ]; then
	dfltDir="/mnt/ramadda";
    fi

    while [ "$BASEDIR" == "" ]; do
	ask   "Enter base directory: [$dfltDir]:" $dfltDir  "The base directory holds the repository and pgsql sub-directories"
	if [ "$response" == "" ]; then
            break;
	fi
	BASEDIR=$response;
	break
    done
}


dopostgres() {
    header   "Database Installation";
    askYesNo  "Install Postgres database"  "y"
    if [ "$response" == "y" ]; then
	yum install -y  ${pgInstall} > /dev/null
	postgresql-setup initdb

	if  [ -d ${postgresDir} ] ; then
	    if  [ ! -h ${postgresDir} ]; then
		echo "Moving ${postgresDir} to $PGDIR"
		mv  ${postgresDir} $PGDIR
		ln  -s -f  $PGDIR ${postgresDir}
		chown -R postgres ${postgresDir}
		chown -R postgres ${PGDIR}
	    fi
	else
	    echo "Warning: ${postgresDir} does not exist"	
	fi

	if [ "$os" == "${OS_REDHAT}" ]; then
	    systemctl enable postgresql
	    systemctl start postgresql.service
	else
	    chkconfig ${pgService} on
	    service ${pgService} start
	fi

	if [ ! -f ${postgresDataDir}/pg_hba.conf.bak ]; then
            cp ${postgresDataDir}/pg_hba.conf ${postgresDataDir}/pg_hba.conf.bak
	fi

	postgresPassword="password$RANDOM-$RANDOM"
	postgresUser="ramadda"
	postgresAuth="
#	
#written out by the RAMADDA installer
#
host repository ${postgresUser} 127.0.0.1/32  password
local   all             all                                     peer
host    all             all             127.0.0.1/32            ident
host    all             all             ::1/128                 ident
"


	printf "${postgresAuth}" > ${postgresDataDir}/pg_hba.conf

	service ${pgService} reload

	printf "create database repository;\ncreate user ramadda;\nalter user ramadda with password '${postgresPassword}';\ngrant all privileges on database repository to ramadda;\n" > /tmp/postgres.sql
	chmod 644 /tmp/postgres.sql
	echo "Creating repository database and Adding ramadda user"
	su -c "psql -f /tmp/postgres.sql"  - postgres > /dev/null
	rm -f ${INSTALLER_DIR}/postgres.sql
	printf "ramadda.db=postgres\nramadda.db.postgres.user=ramadda\nramadda.db.postgres.password=${postgresPassword}"  > ${RAMADDA_HOME_DIR}/db.properties
    fi
}


##dobasedir

if [ ! -d "$BASEDIR" ]; then
    domnt;
else
    echo "$BASEDIR already mounted"
fi


RAMADDA_HOME_DIR=$BASEDIR/repository
mkdir -p $RAMADDA_HOME_DIR
postgresDir=/var/lib/${pgsql}
postgresDataDir=${postgresDir}/data
PGDIR="${BASEDIR}/${pgsql}"

tmpdir=`dirname $BASEDIR`
permissions=$(stat -c %a $tmpdir)
if [ "$permissions" == "700" ]; then
    chmod 755 "$tmpdir"
fi



if [ ! -d "$PGDIR" ]; then
    dopostgres;
else
    echo "PostgreSQL already installed"
fi



echo "Installing base packages - wget, unzip & java"
yum install -y wget > /dev/null
yum install -y unzip > /dev/null
yum install -y java > /dev/null

echo "Fixing the localhost name problem"
sed -e 's/HOSTNAME=localhost.localdomain/HOSTNAME=ramadda.localdomain/g' /etc/sysconfig/network> dummy.network
mv dummy.network /etc/sysconfig/network
sed -e 's/127.0.0.1   localhost localhost.localdomain/127.0.0.1 ramadda.localdomain ramadda localhost localhost.localdomain/g' /etc/hosts> dummy.hosts
mv dummy.hosts /etc/hosts



header  "RAMADDA Installation"
askYesNo "Download and install RAMADDA from Geode Systems"  "y"
if [ "$response" == "y" ]; then
    rm -f ${INSTALLER_DIR}/ramaddaserver.zip
    echo "Downloading RAMADDA from ${RAMADDA_DOWNLOAD}"
    wget -O ${INSTALLER_DIR}/ramaddaserver.zip ${RAMADDA_DOWNLOAD}
    rm -r -f ${SERVER_DIR}
    unzip -d ${RAMADDA_INSTALL_DIR} -o ${INSTALLER_DIR}/ramaddaserver.zip
    ramaddaConfig="##
## Generated by the RAMADDA installer
##

#RAMADDA home directory
export RAMADDA_HOME=${RAMADDA_HOME_DIR}

#Port RAMADDA runs on
export RAMADDA_PORT=80
"


    printf "$ramaddaConfig" > ${SERVER_DIR}/ramaddaenv.sh

    askYesNo "Install RAMADDA as a service"  "y"
    if [ "$response" == "y" ]; then
        printf "#!/bin/sh\n# chkconfig: - 80 30\n# description: RAMADDA repository\n\nsh ${SERVICE_SCRIPT} \"\$@\"\n" > ${SERVICE_DIR}/${SERVICE_NAME}
        chmod 755 ${SERVICE_DIR}/${SERVICE_NAME}
        chkconfig ${SERVICE_NAME} on
        printf "To run the RAMADDA service do:\nsudo service ${SERVICE_NAME} start|stop|restart\n"
    fi
fi




header "SSL Configuration";
printf "We need the public IP address to configure SSL\n"
read -p "Are you running in Amazon AWS? [y|n]: " response
if [ "$response" == "y" ]; then
        host=`curl http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null`
        printf "OK, the public IP of this machine is ${host}\n"
else
    host=`ifconfig | grep inet | grep cast | awk '/inet addr/{print substr($2,6)}'`
    if [ "$host" == "" ]; then
	host=`ifconfig | grep inet | grep cast | awk '/.*/{print $2}'`
    fi
    read -p "Is this IP address correct - ${host}?  [y|n]: " response
    if [ "$response" == "n" ]; then
	read -p "Enter the IP address: " host
    fi
fi

printf "A self-signed SSL certificate can be created for the IP address ${host}\n";
printf "This will enable you to access your server securely but you will need to\n";
printf "add a real certificate or add other entries to the keystore for other domain names\n";
askYesNo "Generate keystore and enable SSL" "y"
if [ "$response" == "y" ]; then
    password="ssl_${RANDOM}_${RANDOM}_${RANDOM}"
    echo "Generating new keystore file: ${RAMADDA_HOME_DIR}/keystore  for host: $host."
    echo "The password is stored in ${RAMADDA_HOME_DIR}/ssl.properties"
    rm -f ${RAMADDA_HOME_DIR}/keystore
    printf "${password}\n${password}\n${host}\nRAMADDA\nRAMADDA\ncity\nstate\ncountry\nyes\n\n" | keytool -genkey -keyalg RSA -alias ramadda -keystore ${RAMADDA_HOME_DIR}/keystore > /dev/null 2> /dev/null
    printf "#generated password\n\nramadda.ssl.password=${password}\nramadda.ssl.keypassword=${password}\nramadda.ssl.port=443\n" > ${RAMADDA_HOME_DIR}/ssl.properties
    printf "\nIf you need to create a new key then delete ${RAMADDA_HOME_DIR}/keystore and run:\n    keytool -genkey -keyalg RSA -alias ramadda -keystore ${RAMADDA_HOME_DIR}/keystore\nIf you are installing your own certificate then generate the keystore and copy it to ${RAMADDA_HOME_DIR}\n"
fi


install_password="${RANDOM}_${RANDOM}"
printf  "ramadda.install.password=${install_password}" > ${RAMADDA_HOME_DIR}/install.properties

service ${SERVICE_NAME} restart

header "Installation complete";
printf "RAMADDA is installed. \n\tRAMADDA home directory: ${RAMADDA_HOME_DIR}\n\tPostgres directory: ${PGDIR}\n\tLog file: ${RAMADDA_INSTALL_DIR}/ramadda.log\n\tService script: ${SERVICE_SCRIPT}\n"
printf "Finish the configuration at https://${host}/repository\n"
printf "The installation password is ${install_password}\n"
printf "Note: since this is a self-signed certificate your browser will show that this is an insecure connection\n"
printf "\n"


exit














