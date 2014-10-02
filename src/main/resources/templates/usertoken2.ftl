<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<usertoken xmlns:ns2="http://www.w3.org/1999/xhtml" id="${it.tokenid!}">
    <uid>${it.uid!?xml}</uid>
    <timestamp>${it.timestamp!}</timestamp>
    <lifespan>${it.lifespan!}</lifespan>
    <issuer>${it.issuer!?xml}</issuer>
    <securitylevel>${it.securityLevel!}</securitylevel>
    <DEFCON>${it.defcon!}</DEFCON>
    <username>${it.userName!?xml}</username>
    <firstname>${it.firstName!?xml}</firstname>
    <lastname>${it.lastName!?xml}</lastname>
    <email>${it.email!?xml}</email>
    <personRef>${it.personRef!?xml}</personRef>
    <#list it.roleList as app>
    <application ID="${app.applicationid!?xml}">
        <applicationName>${app.applicationname!?xml}</applicationName>
        <organizationName>${app.organizationname!?xml}</organizationName>
        <role name="${app.rolename?xml}" value="${app.rolevalue!?xml}"/>
    </application>
</#list>

    <ns2:link type="application/xml" href="/${it.tokenid!?xml}" rel="self"/>
    <hash type="MD5">${it.MD5}</hash>
</usertoken>

