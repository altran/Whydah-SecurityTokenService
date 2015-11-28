SecurityTokenService
====================

![Build Status](http://jenkins.capraconsulting.no/buildStatus/icon?job=Whydah-SecurityTokenService)


The UserToken and ApplicationToken generator and security session manager for the Whydah system

If you are planning on integrating, you might want to run SecurityTokenService in DEV mode. This shortcuts the authentication.
You can manually control the UserTokens for the different test-users you want, by creating a file named t_<username>.token which
consists of the XML representation of the access roles++ you want the spesific user to expose to the integrated application. One example is provided in [t_test@hotmail.com.token](https://raw2.github.com/altran/Whydah-SSOLoginWebApp/master/t_test@hotmail.com.token) and is served if you in DEV mode try to log in with test@hotmail.com 

![Architectural Overview](https://raw2.github.com/altran/Whydah-SSOLoginWebApp/master/Whydah%20infrastructure.png)


Client code example
===================

```
//  Execute a POST to authenticate my application
String appToken = Request.Post("https://sso.whydah.net/sso/logon")
        .bodyForm(Form.form().add("applicationcredential", myAppCredential).build())
        .execute().returnContent().asBytes();

//  authenticate with username and password (user credential)
String usertoken = Request.Post("https://sso.whydah.net/sso/user/"+appTokenID+"/"+new UserTicket(UUID.randomUUID()).toString()+"/usertoken/")
        .bodyForm(Form.form().add("apptoken", appToken)
        .add("usercredential", new UserCredential(username,password).asXML()).build())
        .execute().returnContent().asBytes();

//  Execute a POST  to SecurityTokenService with userticket to get usertoken
String usertoken = Request.Post("https://sso.whydah.net/sso/user/"+appTokenID+"/get_usertoken_by_userticket/")
        .bodyForm(Form.form().add("apptoken", appToken)
        .add("userticket", userTicket).build())
        .execute().returnContent().asBytes();

// That's all you need to get a full user database, IAM/SSO, Facebook/OAUTH support ++
boolean hasEmployeeRoleInMyApp = $(usertoken).xpath("/usertoken/application[@ID="+myAppId+"]/role[@name=\"Employee\"");
```
(Example using Apache HTTP Components Fluent API and jOOX Fluent API)


Installation
============

## User, starting and logging
* create a user for the service, typically SecurityTokenService
* create update_service.sh

```
#!/bin/sh

A=SecurityTokenService
V=SNAPSHOT


if [[ $V == *SNAPSHOT* ]]; then
   echo Note: If the artifact version contains "SNAPSHOT" - the artifact latest greates snapshot is downloaded, Irrelevent of version number!!!
   path="http://mvnrepo.cantara.no/content/repositories/snapshots/net/whydah/token/$A"
   version=`curl -s "$path/maven-metadata.xml" | grep "<version>" | sed "s/.*<version>\([^<]*\)<\/version>.*/\1/" | tail -n 1`
   echo "Version $version"
   build=`curl -s "$path/$version/maven-metadata.xml" | grep '<value>' | head -1 | sed "s/.*<value>\([^<]*\)<\/value>.*/\1/"`
   JARFILE="$A-$build.jar"
   url="$path/$version/$JARFILE"
else #A specific Release version
   path="http://mvnrepo.cantara.no/content/repositories/releases/net/whydah/token/$A"
   url=$path/$V/$A-$V.jar
   JARFILE=$A-$V.jar
fi

# Download
echo Downloading $url
wget -O $JARFILE -q -N $url


#Create symlink or replace existing sym link
if [ -h $A.jar ]; then
   unlink $A.jar
fi
ln -s $JARFILE $A.jar
```


* create securitytokenservice.PROD.properties

```
DEFCON=5
# Normal operations
applicationname=SecurityTokenService
applicationid=11
applicationsecret=secretq986Ep6By7B9J46m96D


myuri=https://sso.whydah.net/tokenservice/
service.port=9998
useradminservice=https://sso.whydah.net/uib/useradminservice/
testpage=enabled

# Temporary provisioning of applications secret in wait for UAS/UIB support
11=secretq986Ep6By7B9J46m96D
12=secretA4t8dzz8mz7a5QQJ7Px
15=secret36R6Jr47D4Hj5R6p9qT
19=secretwJFKsUvJFmhypwK7j6D
99=secret36R6Jr47D4Hj5R6p9qT
100=secretnbKZ2wfC6RMmMuzXpk
```


* create start_service.sh

```
#!/bin/sh
nohup /usr/bin/java -DIAM_MODE=PROD -Dhazelcast.config=hazelcast.xml -DIAM_CONFIG=/home/SecurityTokenService/securitytokenservice.PROD.properties -jar /home/SecurityTokenService/SecurityTokenService.jar
```

Verify instance:
[http://localhost:9998/tokenservice/application.wadl]

If you have enabled test-page in the properties, you can run and verify the key services from the testpage application (testpage=enabled)
[http://localhost:9998/tokenservice/]

Configuration and Integration
============

## Properties

* From the installation script above, we see that [IAM_MODE](https://wiki.cantara.no/display/iam/IAM_MODE) is required. Set it according to your intallation.

* Create securitytokenservice.TEST.properties (Or PROD.properties) and configure as you find suitable for your needs.

|Property | Example values PROD | Comment |
|-------- | -------------- | -----------| 
|*myuri*|http://myserver.net/tokenservice/ | The URI to this instance of STS |
|*service.port*|9998| Port for this service |
|*useradminservice*| http://myservice/useradminservice/ | URL to useridentitybackend |
|*testpage*|disabled| Whether or not to enable the testpage. The url is printed when you start the service with it enabled. |
|*logourl*|http://stocklogos.com/somelogo.png | A logo to display for the kicks of it | 

## Integration
The whole point of true SSO is to have all business applications on board, not only some of them.
Therefore Whydah is designed for easy peasy integration so all developers relatively easily can do it.
As a 3rd party app developer, you can do as follows to develop and test your integration locally:
* Download and configure STS in DEV-mode
* Create usertoken files for the different roles you have in your application. Remember **t_** in front of filename and **.token**
** [Read more about user tokens here](https://wiki.cantara.no/display/whydah/UserToken)
* Run STS in DEV-mode locally, different usernames will give you different "stubbed" usertokens, password check is omitted.
* Learn the login flow using the testpage.
* Build your integration.

**Note:** If you don't want to create your own login screen, you can use the configurable login user interface provided with [SSOLoginWebApplication](http://github.com/altran/Whydah-SSOLoginWebApplication). In that case, you'll need to download and set up that application as well to talk with your STS. 

## Using the testpage
The testpage is accessible at *myuri* if it is enabled.
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

## LICENSE

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
