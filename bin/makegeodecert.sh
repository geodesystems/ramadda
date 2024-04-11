#!/bin/sh


BIN_DIR="$(cd "$(dirname "$0")" && pwd)"
OTHER_DOMAINS=www.geodesystems.com,boulderdata.org,www.boulderdata.org,communidata.org,www.communidata.org,10000cities.org,www.10000cities.org,ramadda.org,www.ramadda.org,asdi.ramadda.org,sdn.ramadda.org,landknowledge.ramadda.org,ditchproject.org,www.ditchproject.org,landknowledge.org,olc.landknowledge.org,www.landknowledge.org

sh "${BIN_DIR}/letsencrypt.sh" -renew -home /mnt/ramadda/repository  -domain geodesystems.com -other "$OTHER_DOMAINS" "$@"
