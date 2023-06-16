#!/bin/bash

# Application info
COMPONENT_TAG=@component.tag@
DBG_PORT=@dbg.port@
MIN_HEAP=@min.heap@
MAX_HEAP=@max.heap@
APP_MAIN=@main.class@
APP_PARAMS=@additional.app.params@
JVM_PARAMS=@additional.jvm.params@
CP_OR_JAR=@cp.or.jar@
TRACER_PATH=@tracer.path@
TRACER_PROPS=@tracer.props@


# Environment info
JVM=/home/directfn/app/jdk-17.0.6/bin/java
NOHUP=nohup