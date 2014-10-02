package net.whydah.token.data;

import net.whydah.token.data.application.ApplicationData;
import net.whydah.token.data.helper.CompanyRoles;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by totto on 10/2/14.
 */
public class UserAggregateMappingTest {

    private final static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    private Map applicationCompanyRoleValueMap = new HashMap();

    public void testMultiRoleApplications() {

        String userAggregateXML = "<whydahuser>\n" +
                "    <identity>\n" +
                "        <username>Jan.Helge.Maurtvedt@altran.com</username>\n" +
                "        <cellPhone></cellPhone>\n" +
                "        <email>Jan.Helge.Maurtvedt@altran.com</email>\n" +
                "        <firstname>Jan Helge</firstname>\n" +
                "        <lastname>Maurtvedt</lastname>\n" +
                "        <personRef></personRef>\n" +
                "        <UID>19dc76a8-b122-4138-b08f-7367e9988c06</UID>\n" +
                "    </identity>\n" +
                "    <applications>\n" +
                "        <application>\n" +
                "            <appId>99</appId>\n" +
                "            <applicationName>WhydahTestWebApplication</applicationName>\n" +
                "            <orgName>Whydah</orgName>\n" +
                "            <roleName>WhydahDefaultUser</roleName>\n" +
                "            <roleValue>true</roleValue>\n" +
                "        </application>\n" +
                "        <application>\n" +
                "            <appId>100</appId>\n" +
                "            <applicationName>ACS</applicationName>\n" +
                "            <orgName>Altran</orgName>\n" +
                "            <roleName>Employee</roleName>\n" +
                "            <roleValue>Jan.Helge.Maurtvedt@altran.com</roleValue>\n" +
                "        </application>\n" +
                "        <application>\n" +
                "            <appId>100</appId>\n" +
                "            <applicationName>ACS</applicationName>\n" +
                "            <orgName>Altran</orgName>\n" +
                "            <roleName>Manager</roleName>\n" +
                "            <roleValue></roleValue>\n" +
                "        </application>\n" +
                "        <application>\n" +
                "            <appId>99</appId>\n" +
                "            <applicationName>WhydahTestWebApplication</applicationName>\n" +
                "            <orgName>Whydah</orgName>\n" +
                "            <roleName>WhydahDefaultUser</roleName>\n" +
                "            <roleValue>Jan.Helge.Maurtvedt@altran.com</roleValue>\n" +
                "        </application>\n" +
                "    </applications>\n" +
                "</whydahuser>\n";


    }


}
