SecurityTokenService
====================

The UserToken and ApplicationToken generator and security session manager for the Whydah system

In DEV mode, STS allow you to create sample tokens in files for easy integration.
One example is provided in [t_test@hotmail.com.token] and is served if you in DV mode try to log in with test@hotmail.com 

![Architectural Overview](https://raw2.github.com/altran/Whydah-SSOLoginWebApp/master/Whydah%20infrastructure.png)

Installation
============

## User, starting and logging
* create a user for the service, typically SecurityTokenService
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

# The log is found in the logs directory, there is also a more detailed nohup:
tail -f nohup.out
```

Verify instance:
*  http://server:9998/tokenservice/application.wadl

If you have enabled test-page in the properties, you can run and verify the key services from the testpage application (testpage=enabled)
* http://server:9998/tokenservice/

Configuration and Integration
============

## Properties

* From the installation script above, we see that [IAM_MODE|https://wiki.cantara.no/display/iam/IAM_MODE] is required. Set it according to your intallation.

* Create securitytokenservice.TEST.properties (Or PROD.properties) and configure as you find suitable for your needs.

|Property | Example values PROD | Comment |
|-------- | -------------- | -----------| 
|*mybaseuri*|http://myserver.net/tokenservice/ | The URI to this instance of STS |
|*service.port*|9998| Port for this service |
|*useridentitybackend*| http://myservice/uib/ | URL to useridentitybackend |
|*testpage*|disabled| Whether or not to enable the testpage. The url is printed when you start the service with it enabled. |
|*logourl*|http://stocklogos.com/somelogo.png | A logo to display for the kicks of it | 

## Integration
The whole point of true SSO is to have all business applications on board, not only some of them.
Therefore Whydah is designed for easy peasy integration so all developers relatively easily can do it.
As a 3rd party app developer, you can do as follows to develop and test your integration locally:
* Download and configure STS in DEV-mode
* Create usertoken files for the different roles you have in your application.
* Run STS in DEV-mode locally, different usernames will give you different "stubbed" usertokens, password check is omitted.
* Learn the login flow using the testpage.
* Build your integration.

**Note:** If you don't want to create your own login screen, you can use the configurable login user interface provided with [SSOLoginWebApplication|http://github.com/altran/Whydah-SSOLoginWebApplication]. In that case, you'll need to download and set up that application as well to talk with your STS. 

## Using the testpage
The testpage is accessible at *mybaseuri* if it is enabled.
You will normally find at at [http://localhost:9998/tokenservice/] if you run locally.

It is useful to test that application logon and user logon actually works.
In TEST mode, it helps you run integration tests to verify that UIB works as expected.
In DEV mode, it allow you to test your created *.token files, used to stub away the rest of the stack.

You may change the input before trying to log on to see the usertoken for different users.

Typical apache setup for the whole stack
====================
Using Apache in front is only necessary if you set up the Whole stack - typically PROD or TEST
Also note that you will prbably not want to run the entire stack on one server. 
UIB should typically be behind a tight firewall.

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

Developer info
==============

* https://wiki.cantara.no/display/iam/Architecture+Overview
* https://wiki.cantara.no/display/iam/Key+Whydah+Data+Structures
* https://wiki.cantara.no/display/iam/Modules

If you are planning on integrating, you might want to run SecurityTokenService in DEV mode. This shortcuts the authentication.
You can manually control the UserTokens for the different test-users you want, by creating a file named t_<username>.token which
consists of the XML representation of the access roles++ you want the spesific user to expose to the integrated application.