#!/bin/sh

export IAM_MODE=TEST

A=SecurityTokenService
V=0.4-SNAPSHOT
JARFILE=$A-$V.jar

pkill -f $A

wget --user=altran --password=l1nkSys -O $JARFILE "http://mvnrepo.cantara.no/service/local/artifact/maven/content?r=altran-snapshots&g=net.whydah.token&a=$A&v=$V&p=jar"
nohup java -jar -DIAM_CONFIG=securitytokenservice.TEST.properties $JARFILE &


tail -f nohup.out
