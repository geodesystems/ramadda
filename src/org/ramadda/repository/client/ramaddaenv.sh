
## This file gets sourced when you run search.sh
## You can define the default repository and (optionally) 
## the user id and password 
## for running the ramadda client
## Alternatlively you can set these as your shell environment variables


if [ -z "$RAMADDA_CLIENT_REPOSITORY" ]; then
    export RAMADDA_CLIENT_REPOSITORY=http://localhost/repository
fi  

if [ -z "$RAMADDA_CLIENT_USER" ]; then
    export RAMADDA_CLIENT_USER=
    export RAMADDA_CLIENT_PASSWORD=
fi  






