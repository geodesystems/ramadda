#!/bin/sh

###############################################################
#This script renews the letsencrypt certificate  used by RAMADDA
#It can be run as a cron job  (see below)
###############################################################

#This uses the RAMADDA_DOMAIN environment variable. Either set it in your .bashrc with:
#export RAMADDA_DOMAIN=yourdomain.org

#Or set it in this script
#export RAMADDA_DOMAIN=yourdomain.org

#If you have alternate domains set:
#export RAMADDA_OTHER_DOMAINS=domain1.org,domain2.org

export MYDIR="$(cd "$(dirname "$0")" && pwd)"

#Change the  ramadda home and letsencrypt  path if they are different than the default
export RAMADDA_HOME=/mnt/ramadda/ramaddahome

#letsencrypt.sh should be in the same directory
export LETSENCRYPT=${MYDIR}/letsencrypt.sh


###############################################################
##cron jobs
###############################################################
#note: new versions of Amazon Linux don't have crontab installed. install with:
#sudo yum install cronie
#sudo systemctl start crond
#sudo service crond start

#Lets encrypt requires a renewal every 3 months but renewals can't occur outside of
#2 weeks from the renewal date so this should be set to run once a week
#The source .bashrc is there to pick up the RAMADDA_DOMAIN
#sudo crontab -e 
#0 0 */7 * * TZ=America/Denver bash -c "source /home/ec2-user/.bashrc && /home/ec2-user/ramaddainstaller/renewcertificate.sh" >> /home/ec2-user/certificate_cron.log 2>&1


#Check with:
#sudo crontab -l


if [ "$RAMADDA_DOMAIN" = "" ]; then
   echo "No -domain specified"
   exit
fi
   
if [ ! -d "$RAMADDA_HOME" ]; then
   echo "RAMADDA home does not exist: $RAMADDA_HOME"
   exit
fi

if [ ! -e "$LETSENCRYPT" ]; then
   echo "letsencrypt does not exist: $LETSENCRYPT"
   exit
fi

echo "Renewing certificate" >  ${MYDIR}/certificate.log
date >>  ${MYDIR}/certificate.log


printf "y\nn\n" | \
    sh "${LETSENCRYPT}" \
       -renew \
       -home "${RAMADDA_HOME}" \
       -other "${RAMADDA_OTHER_DOMAINS}" \
       -domain "${RAMADDA_DOMAIN}" >>  "${MYDIR}/certificate.log"

service ramadda start
