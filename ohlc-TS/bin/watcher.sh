#!/bin/bash

source config.sh

SCRIPT=$(readlink -f -- "$0")
SCRIPTPATH=$(dirname "$SCRIPT")

COUNT="$(ps aux|grep $COMPONENT_TAG|grep -v 'grep'|grep $USER|wc -l)"
TIME="$(date)"
FILE="$SCRIPTPATH/logs/watcher.log"
PAUSE="$SCRIPTPATH/WATCHER.PAUSE"

test -f "$FILE" || echo "$TIME : New File" > $FILE

while getopts PR ver
do
        case "$ver" in
        P ) touch "$PAUSE" && echo "$TIME : Watcher Paused " >> $FILE && echo "$TIME : Watcher Paused " && exit 1;;
        R ) rm -f "$PAUSE" && echo "$TIME : Watcher Resumed " >> $FILE && echo "$TIME : Watcher Resumed " && exit 1;;

        esac
done;

echo "$TIME : Number of running instances is $COUNT" >> $FILE

if [ $COUNT -eq 0 ]
then
    if [ -f "$PAUSE" ]
    then
        echo "$TIME : No Instance Running but Watcher Pause " >> $FILE
    else
	    echo "$TIME : No Instance Running. " >> $FILE
	    cd $SCRIPTPATH
	    ./run.sh -d
	fi
else
	echo "$TIME : No start required. " >> $FILE
fi