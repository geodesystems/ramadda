#!/bin/bash

#
#This script installs some base packages, Postgres and then RAMADDA
#


INSTALLER_DIR="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
source "${INSTALLER_DIR}/lib.sh"

installPostgres
