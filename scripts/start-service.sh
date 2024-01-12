#!/bin/bash

#  If IAM_MODE not set, use PROD
if [ -z "$IAM_MODE" ]; then 
  IAM_MODE=PROD
fi


# If Version is from source, find the artifact
if [ "$Version" = "FROM_SOURCE" ]; then 
    # Find the built artifact
    Version=$(find target/* -name '*.jar' | grep SNAPSHOT | grep -v javadoc | grep -v original | grep -v lib)
else
    Version=SecurityTokenService.jar
fi


# If IAM_CONFIG not set, use embedded
if [ -z "$IAM_CONFIG" ]; then
  nohup /usr/bin/java --illegal-access=warn --add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED -DIAM_MODE=PROD -DIAM_MODE=$IAM_MODE -Dhazelcast.config=hazelcast.xml -jar  $Version &
else  
  nohup /usr/bin/java --illegal-access=warn --add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED -DIAM_MODE=PROD -DIAM_MODE=$IAM_MODE -Dhazelcast.config=hazelcast.xml -DIAM_CONFIG=$IAM_CONFIG -jar  $Version &
fi


