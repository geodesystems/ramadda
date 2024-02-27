#!/bin/sh

export MYDIR="$(cd "$(dirname "$0")" && pwd)"

#This is intended to be run as a cron job to renew the  certificate using letsencrypt
#It assumes that the letsencrypt.sh script is in the same directory
#Change the domain, ramadda home and letsencrypt  path
export DOMAIN=
export RAMADDAHOME=/mnt/ramadda/ramaddahome
export LETSENCRYPT=$MYDIR/letsencrypt.sh

#Lets encrypt requires a renewal every 3 months but renewals can't occur outside of
#2 weeks from the renewal date so this should be set to run once a week
#sudo crontab -e
#0 3 * * 0 /path/to/renewcertificate.sh

#Check with:
#sudo crontab -l
#
#note: new versions of Amazon Linux don't have crontab installed
#you can  install it with:
#sudo yum install cronie
#sudo service crond start

if [ "$DOMAIN" = "" ]; then
   echo "No -domain specified"
   exit
fi
   
if [ ! -d "$RAMADDAHOME" ]; then
   echo "RAMADDA home does not exist: $RAMADDAHOME"
   exit
fi

if [ ! -e "$LETSENCRYPT" ]; then
   echo "letsencrypt does not exist: $LETSENCRYPT"
   exit
fi

echo "Renewing certificate" >  ${MYDIR}/certificate.log
date >>  ${MYDIR}/certificate.log
printf "y\nn\n" | \
    sh ${LETSENCRYPT} \
       -renew \
       -home ${RAMADDAHOME} \
       -domain ${DOMAIN} >>  ${MYDIR}/certificate.log

