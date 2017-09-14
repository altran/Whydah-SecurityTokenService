<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<usertoken xmlns:ns2="http://www.w3.org/1999/xhtml" id="${it.tokenid!}">
    <uid>${it.uid!?xml}</uid>
    <timestamp>${it.timestamp!}</timestamp>
    <lifespan>${it.lifespan!}</lifespan>
    <issuer>${it.issuer!?xml}</issuer>
    <securitylevel>${it.securityLevel!}</securitylevel>
    <DEFCON>${DEFCON!?xml}</DEFCON>
    <username>${it.userName!?xml}</username>
    <firstname>${it.firstName!?xml}</firstname>
    <lastname>${it.lastName!?xml}</lastname>
    <cellphone>${it.cellPhone!?xml}</cellphone>
    <email>${it.email!?xml}</email>
    <personref>${it.personRef!?xml}</personref>
    <#list it.roleList as role>
    <application ID="${role.applicationId!?xml}">
        <applicationName>${role.applicationName!?xml}</applicationName>
        <organizationName>${role.orgName!?xml}</organizationName>
        <role name="${role.roleName!?xml}" value="${role.roleValue!?xml}"/>
    </application>
    </#list>

    <ns2:link type="application/xml" href="${it.ns2link!}" rel="self"/>
    <hash type="MD5">${it.MD5}</hash>
</usertoken>

