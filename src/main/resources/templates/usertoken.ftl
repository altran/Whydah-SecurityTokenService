<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<token xmlns:ns2="http://www.w3.org/1999/xhtml" id="${it.tokenid!}">
    <uid>${it.uid!?xml}</uid>
    <securitylevel>${it.securityLevel!}</securitylevel>
    <personRef>${it.personRef!?xml}</personRef>
    <username>${it.userName!?xml}</username>
    <firstname>${it.firstName!?xml}</firstname>
    <lastname>${it.lastName!?xml}</lastname>
    <email>${it.email!?xml}</email>
    <timestamp>${it.timestamp!}</timestamp>
    <lifespan>${it.lifespan!}</lifespan>
    <issuer>${it.issuer!?xml}</issuer>
<#list it.applications as app>
    <application ID="${app.applicationID!?xml}">
        <applicationName>${app.applicationName!?xml}</applicationName>
        <#list app.companyRoles as role>
        <organization ID="${role.companyNumber}">
            <organizationName>${role.companyName!?xml}</organizationName>
            <#list role.roleMap?keys as key>
            <role name="${key?xml}" value="${role.roleMap[key]!?xml}"/>
            </#list>
        </organization>
        </#list>
    </application>
</#list>

    <ns2:link type="application/xml" href="/${it.id!?xml}" rel="self"/>
    <hash type="MD5">${it.MD5}</hash>
</token>
