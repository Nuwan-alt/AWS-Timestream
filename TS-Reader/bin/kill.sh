#!/bin/bash

source config.sh

PID_COUNT=`ps -ef | grep -vw grep | grep $COMPONENT_TAG | wc -l`

if [ $PID_COUNT -eq 1 ]; then
    KILLER=`ps -ef | grep -vw grep | grep $COMPONENT_TAG | awk '{print $2}'`
    kill -15 $KILLER
    echo -n "Term Signal sent ."
    while ps axg | grep -vw grep | grep -w $KILLER > /dev/null; do echo -n "."; sleep 1; done
    echo " Killed!! "
elif [ $PID_COUNT -eq 0 ]; then
    echo "Nothing to kill"
else
    echo "More than one process running. Please kill manually."
fi

exit 0
