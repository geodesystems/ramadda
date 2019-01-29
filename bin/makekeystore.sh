#!/bin/sh

#This script uses https://letsencrypt.org and https://certbot.eff.org/ to create and install a SSL certificate for RAMADDA

#usage:
#makekeystore.sh new -> make a new certificate
#makekeystore.sh renew -> renew the certificates (default)

#This assumes the directory structure:
#the github source tree
#/some/dir/source/ramadda  

#the  ramadda home dir
#/some/dir/repository 

MYDIR=`dirname $0`
PARENT=`dirname $MYDIR`
GRANDPARENT=`dirname $PARENT`
GREATPARENT=`dirname $GRANDPARENT`
BASEDIR=${GREATPARENT}

#For now hard code this
BASEDIR=/mnt/ramadda
#Change this to point to your RAMADDA home dir
RAMADDA_HOME=${BASEDIR}/repository
echo "RAMADDA_HOME=${RAMADDA_HOME}"

#This file should exist in your RAMADDA home dir and should contain the 2 passwords (without the '#' comment delimiter):
#ramadda.ssl.password=some password
#ramadda.ssl.keypassword=some password
PROPERTY_FILE=${RAMADDA_HOME}/repository.properties

##Set your first domain and any other domains
FIRST_DOMAIN=geodesystems.com
OTHER_DOMAINS=www.geodesystems.com,dmt.geodesystems.com,boulderdata.org,www.boulderdata.org,communidata.org,www.communidata.org,hoganpancost.org,www.hoganpancost.org,10000cities.org,www.10000cities.org,ramadda.org,www.ramadda.org


BIN_DIR=`dirname $0`
PASSWORD=`cat $PROPERTY_FILE | egrep "^ramadda.ssl.password" | cut -d'=' -f2`
KEYSTORE=keystore.jks
SRCKEYSTORE=keystore.pkcs12
CERTBOT=${BIN_DIR}/certbot-auto


case $1 in
    ""|"renew")
	${CERTBOT} --debug renew
	;;
    "new")
	${CERTBOT}  --debug certonly --webroot -w ${RAMADDA_HOME}/htdocs -d ${FIRST_DOMAIN} -d ${OTHER_DOMAINS}
	;;
esac



#Check for the -0001 dir. Note sure why certbot makes this
LETSENCRYPT_DIR="/etc/letsencrypt/live/${FIRST_DOMAIN}-0001"
if [! -d "$LETSENCRYPT_DIR" ]; then
    LETSENCRYPT_DIR="/etc/letsencrypt/live/${FIRST_DOMAIN}"
fi

echo "letsencrypt dir: ${LETSENCRYPT_DIR}"

openssl pkcs12 -export -out ${SRCKEYSTORE} -in /etc/letsencrypt/live/${LETSENCRYPT_DIR}/fullchain.pem  -inkey /etc/letsencrypt/live/${LETSENCRYPT_DIR}/privkey.pem  -password pass:${PASSWORD}
rm -f ${KEYSTORE}
echo "$PASSWORD
$PASSWORD
$PASSWORD
" | keytool -importkeystore -srckeystore ${SRCKEYSTORE} -destkeystore ${KEYSTORE} -deststoretype pkcs12
rm -f ${SRCKEYSTORE}

cp ${KEYSTORE} ${RAMADDA_HOME}

#We have RAMADDA running as service. This restarts it
service ramadda start
