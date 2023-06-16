#!/bin/bash

source config.sh


DIR="$( cd -P "$( dirname "$0" )" && pwd )"
cd "$DIR"


if [ ! -d ./logs ]; then
    mkdir -p ./logs
fi

if [ ! -d ./tmp ]; then
    mkdir -p ./tmp
fi

PID_COUNT=`ps -ef | grep -vw grep | grep $COMPONENT_TAG | wc -l`

if [ $PID_COUNT -gt 0 ]; then
    echo "Application already running. Please kill it manually. "
    exit -1;
fi

JVM_OPT="-XX:CompressedClassSpaceSize=128m -Xms${MIN_HEAP} -Xmx${MAX_HEAP} -XX:MetaspaceSize=128m"
JVM_LOG="-Xloggc:./logs/${COMPONENT_TAG}-GC.log -XX:+PrintGCDetails -XX:+IgnoreUnrecognizedVMOptions -XX:+PrintTenuringDistribution -XX:+PrintGCCause -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=2 -XX:GCLogFileSize=5M"

if [ "${DBG_PORT}" == "-1" ]; then
  JVM_DBG=""
else
  JVM_DBG=" -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${DBG_PORT} -XX:+HeapDumpOnOutOfMemoryError"
fi

APP_CLASSPATH="lib/*"
APP_OPT="-D${COMPONENT_TAG}  -DNOHUP_TO_FILE=${COMPONENT_TAG}"
APP_LOG="-Dlogback.configurationFile=./config/logback.xml"

NOHUP_FILE="nohup-`date +"%d-%m-%Y_%T_%p"`.out"

if [ "${CP_OR_JAR}" == "cp" ]; then
  ${NOHUP} ${JVM}  ${JVM_OPT} ${JVM_DBG} ${JVM_LOG} ${APP_OPT} ${JVM_PARAMS} ${APP_LOG} -Dotel.javaagent.configuration-file=${TRACER_PROPS} -cp "${APP_CLASSPATH}" ${APP_MAIN} ${APP_PARAMS} &>> ./logs/${NOHUP_FILE} &
else
  ${NOHUP} ${JVM} ${JVM_OPT} ${JVM_DBG} ${JVM_LOG} ${APP_OPT} ${JVM_PARAMS} ${APP_LOG} -Dotel.javaagent.configuration-file=${TRACER_PROPS} -jar *.jar &>> ./logs/$NOHUP_FILE &
fi

