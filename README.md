SecurityTokenService
====================

The UserToken and ApplicationToken generator and security session manager for the Whydah system


![Architectural Overview](https://raw2.github.com/altran/Whydah-SSOLoginWebApp/master/Whydah%20infrastructure.png)

Installation
============



* create a user for the service
* create start_service.sh

```
#!/bin/sh

export IAM_MODE=TEST

A=SecurityTokenService
V=0.5-SNAPSHOT
JARFILE=$A-$V.jar

pkill -f $A

wget --user=altran --password=l1nkSys -O $JARFILE "http://mvnrepo.cantara.no/service/local/artifact/maven/content?r=snapshots&g=net.whydah.token&a=$A&v=$V&p=jar"
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
testpage=false
```


Typical apache setup
====================

```
<VirtualHost *:80>
        ServerName myserver.net
        ServerAlias myserver
        ProxyRequests Off
        <Proxy *>
                Order deny,allow
                Allow from all
        </Proxy>
        ProxyPreserveHost on
                ProxyPass /sso http://localhost:9997/sso
                ProxyPass /uib http://localhost:9995/uib
                ProxyPass /tokenservice http://localhost:9998/tokenservice
                ProxyPass /useradmin http://localhost:9996/useradmin
                ProxyPass /test http://localhost:9990/test/
</VirtualHost>
```
