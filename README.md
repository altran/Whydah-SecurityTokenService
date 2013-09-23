SecurityTokenService
====================

The UserToken and ApplicationToken generator and security session manager for the Whydah system


Installation
============



* create a user for the service
* create start_service.sh

```
#!/bin/sh

export IAM_MODE=TEST

A=SecurityTokenService
V=1.0-SNAPSHOT
JARFILE=$A-$V.jar

pkill -f $A

wget --user=altran --password=l1nkSys -O $JARFILE "http://mvnrepo.cantara.no/service/local/artifact/maven/content?r=altran-snapshots&g=net.whydah.sso.service&a=$A&v=$V&p=jar"
nohup java -jar -DIAM_CONFIG=securitytokenservice.TEST.properties $JARFILE &


tail -f nohup.out
```

* create securitytokenservice.TEST.properties

```
#myuri=http://myserver.net/tokenservice/
myuri=http://localhost:9998/tokenservice/
service.port=9998
#useridbackendUri=http://nkk-test-02.cloudapp.net/uib/
useridbackendUri=http://localhost:9995/uib/
testpage=false```
