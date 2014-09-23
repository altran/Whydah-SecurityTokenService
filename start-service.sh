#!/bin/sh
nohup /usr/bin/java -DIAM_MODE=PROD -Dhazelcast.config=hazelcast.xml -DIAM_CONFIG=/home/SecurityTokenService/securitytokenservice.PROD.properties -jar /home/SecurityTokenService/SecurityTokenService.jar
