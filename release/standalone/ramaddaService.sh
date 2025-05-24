#!/bin/bash

RAMADDA_DIR="$(cd "$(dirname "$0")" && pwd)"
BASE=$RAMADDA_DIR
PARENT=`dirname $BASE`

#This saves the log and pid in the parent directory
PID=$PARENT/ramadda.pid
LOG=$PARENT/ramadda.log

COMMAND="sh $RAMADDA_DIR/ramadda.sh"

#check if we are on centos
if [ "$#" -ge 2 ]; then
    export WAITSET="true"
fi


status() {
    echo "ramadda.service: RAMADDA Status"
    if [ -f $PID ]
    then
        echo
        echo "ramadda.service: PID file: $( cat $PID ) [$PID]"
        echo
        ps -ef | grep -v grep | grep $( cat $PID )
    else
        echo
        echo "ramadda.service: No Pid file"
    fi
}

start() {
    if [ -f $PID ]
    then
        echo "ramadda.service: RAMADDA already started. PID: [$( cat $PID )]" >>$LOG
    else
	##Overwrite the log file on service start
        echo "ramadda.service: START: Running $COMMAND" >$LOG
        touch $PID
        nohup $COMMAND >>$LOG 2>&1 &
	RAMADDA_PID=$!
	echo $RAMADDA_PID> $PID
        echo "ramadda.service: START $(date '+%Y-%m-%d %X'):   RAMADDA_PID: $RAMADDA_PID" >>$LOG
	if [ -z "${WAITSET}"]; then
	    exit
	fi
	echo "ramadda.service: WAIT" >>$LOG
	wait "$!"
	echo "ramadda.service: EXIT" >>$LOG
	exit
    fi
}

kill_cmd() {
    SIGNAL=""; MSG="Killing RAMADDA"
    while true
    do
        LIST=`ps -ef | grep -v grep | grep java | grep ramadda.jar |  awk '{print $2}'`
        if [ "$LIST" ]
        then
            echo "$MSG $LIST" ; 
            echo $LIST | xargs kill $SIGNAL
            sleep 2
            SIGNAL="-15" 
            MSG="Killing $SIGNAL"
            if [ -f $PID ]
            then
                /bin/rm $PID
            fi
        else
#           echo "All killed..." ; 
           break
        fi
    done
}

stop() {
    echo "ramadda.service: stopping RAMADDA" >> $LOG

    if [ -f $PID ]
    then
        if kill $( cat $PID )
        then echo "ramadda.service: $(date '+%Y-%m-%d %X'): STOP" >>$LOG
        fi
        /bin/rm $PID
        kill_cmd
    fi
}

case "$1" in
    'start')
            stop ;  
	    sleep 1 ;
            start
            ;;
    'stop')
            stop
            ;;
    'restart')
            stop ;  sleep 1 ;
            start
            ;;
    'status')
            status
            ;;
    *)
            echo
            echo "Usage: $0 { start | stop | restart | status }"
            echo
            exit 1
            ;;
esac

exit 0
