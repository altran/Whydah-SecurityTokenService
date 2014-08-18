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
V=LATEST
JARFILE=$A-$V.jar

pkill -f $A

wget -O $JARFILE "http://mvnrepo.cantara.no/service/local/artifact/maven/content?r=snapshots&g=net.whydah.token&a=$A&v=$V&p=jar"
nohup java -jar -DIAM_CONFIG=securitytokenservice.TEST.properties $JARFILE &


tail -f nohup.out
```

* create securitytokenservice.TEST.properties (Or PROD.properties) and configure as you find suitable for your needs

|Property | Example values PROD | Comment |
|-------- | -------------- | -----------| 
|*myuri*|http://myserver.net/tokenservice/ | The URI to this instance of STS |
|*service.port*|9998| Port for this service |
|*useridbackendUri*| http://myservice/uib/ | URL to useridentity backend |
|*testpage*|disabled| Whether or not to enable the testpage. The url is printed when you start the service with it enabled. |
|*logourl*|http://stocklogos.com/somelogo.png | A logo to display for the kicks of it | 

* Use the testpage provided
The testpage (URL is printed when service is started), as accessible at *myuri* id enabled. 

It is useful to test that application logon and user logon actually works.

You may change the input before trying to log on to see the usertoken for different users.


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

Verify instance:
*  http://server:9998/tokenservice/application.wadl

If you have enabled test-page in the properties, you can run and verify the key services from the testpage application (testpage=enabled)
* http://server:9998/tokenservice/


Developer info
==============

* https://wiki.cantara.no/display/iam/Architecture+Overview
* https://wiki.cantara.no/display/iam/Key+Whydah+Data+Structures
* https://wiki.cantara.no/display/iam/Modules

If you are planning on integrating, you might want to run SecurityTokenService in DEV mode. This shortcuts the authentication.
You can manually control the UserTokens for the different test-users you want, by creating a file named t_<username>.token which
consists of the XML representation of the access roles++ you want the spesific user to expose to the integrated application.