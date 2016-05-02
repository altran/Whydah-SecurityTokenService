package net.whydah.token.user;

import net.whydah.sso.application.mappers.ApplicationCredentialMapper;
import net.whydah.sso.application.mappers.ApplicationTokenMapper;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.token.application.ApplicationThreatResource;
import net.whydah.token.application.AuthenticatedApplicationRepository;
import net.whydah.token.config.AppConfig;
import net.whydah.token.config.ApplicationMode;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.*;

public class UserTokenTest {
    private FreemarkerProcessor freemarkerProcessor = new FreemarkerProcessor();
    private final static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();


    @BeforeClass
    public static void init() {
        System.setProperty(ApplicationMode.IAM_MODE_KEY, ApplicationMode.TEST);
        System.setProperty(AppConfig.IAM_CONFIG_KEY, "src/test/testconfig.properties");
    }


    @Test
    @Ignore
    public void testCreateUserToken() throws Exception {
        UserToken utoken = new UserToken();
        utoken.setFirstName("Ola");
        utoken.setEmail("test@whydah.net");
        utoken.setLastName("Nordmann");
        utoken.setTimestamp("123123123");
        utoken.setPersonRef("73637276722376");
        utoken.setDefcon(ApplicationThreatResource.getDEFCON());
        utoken.setTokenid(UUID.randomUUID().toString());
        String xml = freemarkerProcessor.toXml(utoken);

        UserToken copyToken = UserTokenFactory.fromXml(xml);
        String copyxml = freemarkerProcessor.toXml(copyToken);
        //assertTrue("The generated user token is wrong.", xml.equalsIgnoreCase(copyxml));

        assertXMLEqual(xml, copyxml);
    }

    @Test
    public void testActiveUserTokenRepository() {
        UserToken utoken = new UserToken();
        utoken.setFirstName("Ola");
        utoken.setLastName("Nordmann");
        utoken.setEmail("test@whydah.net");
        utoken.setTimestamp(String.valueOf(System.currentTimeMillis() + 1000));
        utoken.setTokenid(UUID.randomUUID().toString());
        utoken.setPersonRef("78125637812638");
        utoken.setLifespan(String.valueOf(2*60 * 60 * new Random().nextInt(100)));

        ActiveUserTokenRepository.addUserToken(utoken, "", "");
        assertTrue("Verification of valid token failed", ActiveUserTokenRepository.verifyUserToken(utoken,""));

        utoken.setFirstName("Pelle");
        String token = freemarkerProcessor.toXml(utoken);
        assertTrue("Token not updated", token.indexOf("Pelle") > 0);
        assertFalse("Verification of in-valid token successful", ActiveUserTokenRepository.verifyUserToken(utoken,""));
    }

    @Test
    public void testTimedOutActiveUserTokenRepository() {
        UserToken utoken = new UserToken();
        utoken.setTokenid(UUID.randomUUID().toString());
        utoken.setFirstName("Ola");
        utoken.setLastName("Nordmann");
        utoken.setEmail("test@whydah.net");
        utoken.setPersonRef("78125637812638");
        utoken.setTimestamp(String.valueOf(System.currentTimeMillis() - 1000));
        utoken.setLifespan("0");
        ActiveUserTokenRepository.addUserToken(utoken, "", "");
        assertFalse("Verification of timed-out token successful", ActiveUserTokenRepository.verifyUserToken(utoken,""));
    }

    @Test
    @Ignore
    public void testCreateUserTokenWithRolesFreemarkerCopy() {
        UserToken utoken = new UserToken();
        utoken.setFirstName("Olav");
        utoken.setLastName("Nordmann");
        utoken.setEmail("test2@whydah.net");
        utoken.setTokenid(UUID.randomUUID().toString());
        utoken.addApplicationRoleEntry(new ApplicationRoleEntry("2349785543", "Whydah.net", "Kunde 1", "Boardmember", "Diktator"));
        utoken.addApplicationRoleEntry(new ApplicationRoleEntry("2349785543", "Whydah.net", "Kunde 2", "tester", "ansatt"));
        utoken.addApplicationRoleEntry(new ApplicationRoleEntry("2349785543", "Whydah.net", "Kunde 3", "Boardmember", ""));
        utoken.addApplicationRoleEntry(new ApplicationRoleEntry("appa", "whydag.org", "Kunde 1", "President", "Valla"));
        String tokenxml = freemarkerProcessor.toXml(utoken);

        UserToken copyToken = UserTokenFactory.fromXml(tokenxml);
        String copyxml = freemarkerProcessor.toXml(copyToken);
        //System.out.println("FROM: " + tokenxml);
        //System.out.println("TO: " + copyxml);
        assertEquals(tokenxml, copyxml);
        UserToken copyToken2 = UserTokenFactory.fromXml(tokenxml);
        copyToken2.setApplicationID("2349785543");
        String copyxml2 = freemarkerProcessor.toXml(copyToken2);
        //System.out.println("FILTERED: " + copyxml2);
        // assertFalse("Should not be equal as result is applicationfiltered ", tokenxml.equals(copyxml2));
    }

    @Test
    @Ignore
    public void testCreateUserTokenWithRolesUserTokenCopy() {
        UserToken utoken = new UserToken();
        utoken.setFirstName("Olav");
        utoken.setLastName("Nordmann");
        utoken.setEmail("test2@whydah.net");
        utoken.setTokenid(UUID.randomUUID().toString());
        utoken.addApplicationRoleEntry(new ApplicationRoleEntry("2349785543", "Whydah.net", "Kunde 1", "Boardmember", "Diktator"));
        utoken.addApplicationRoleEntry(new ApplicationRoleEntry("2349785543", "Whydah.net", "Kunde 2", "tester", "ansatt"));
        utoken.addApplicationRoleEntry(new ApplicationRoleEntry("2349785543", "Whydah.net", "Kunde 3", "Boardmember", ""));
        utoken.addApplicationRoleEntry(new ApplicationRoleEntry("appa", "whydag.org", "Kunde 1", "President", "Valla"));
        String tokenxml = freemarkerProcessor.toXml(utoken);

        UserToken copyToken = UserTokenFactory.fromXml(tokenxml);
        UserToken copy2Token = copyToken.copy();
        assertTrue(copy2Token.toString().equalsIgnoreCase(copyToken.toString()));
    }

    @Test
    public void createFromUserIdentityXML() {
        String identityXML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<whydahuser>\n" +
                "    <identity>\n" +
                "        <username>admin</username>\n" +
                "        <cellPhone>+1555406789</cellPhone>\n" +
                "        <email>useradmin@getwhydah.com</email>\n" +
                "        <firstname>User</firstname>\n" +
                "        <lastname>Admin</lastname>\n" +
                "        <personRef>0</personRef>\n" +
                "        <UID>useradmin</UID>\n" +
                "    </identity>\n" +
                "    <applications>\n" +
                "        <application>\n" +
                "            <appId>19</appId>\n" +
                "            <applicationName>UserAdminWebApplication</applicationName>\n" +
                "            <orgName>Support</orgName>\n" +
                "            <roleName>WhydahUserAdmin</roleName>\n" +
                "            <roleValue>1</roleValue>\n" +
                "        </application>\n" +
                "        <application>\n" +
                "            <appId>19</appId>\n" +
                "            <applicationName>UserAdminWebApplication</applicationName>\n" +
                "            <orgName>Support</orgName>\n" +
                "            <roleName>TEST</roleName>\n" +
                "            <roleValue>13</roleValue>\n" +
                "        </application>\n" +
                "        <application>\n" +
                "            <appId>19</appId>\n" +
                "            <applicationName>UserAdminWebApplication</applicationName>\n" +
                "            <orgName>ACS</orgName>\n" +
                "            <roleName>TULL</roleName>\n" +
                "            <roleValue>1</roleValue>\n" +
                "        </application>\n" +
                "        <application>\n" +
                "            <appId>199</appId>\n" +
                "            <applicationName>UserAdminWebApplication</applicationName>\n" +
                "            <orgName>Support</orgName>\n" +
                "            <roleName>WhydahUserAdmin</roleName>\n" +
                "            <roleValue>1</roleValue>\n" +
                "        </application>\n" +
                "        <application>\n" +
                "            <appId>19</appId>\n" +
                "            <applicationName>UserAdminWebApplication</applicationName>\n" +
                "            <orgName>Support</orgName>\n" +
                "            <roleName>UserAdmin</roleName>\n" +
                "            <roleValue>100</roleValue>\n" +
                "        </application>\n" +
                "    </applications>\n" +
                "</whydahuser>\n";


        String appXML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n " +
                " <token>\n" +
                "     <params>\n" +
                "         <applicationtoken>123123123123</applicationtoken>\n" +
                "         <applicationid>123</applicationid>\n" +
                "         <applicationname>ACS</applicationname>\n" +
                "         <expires>3213213212</expires>\n" +
                "     </params> \n" +
                " </token>\n";
        //UserToken2 userToken = UserToken2.createUserTokenFromUserAggregate(appXML, identityXML);

        UserToken userToken = new UserTokenFactory("0").fromUserAggregate(identityXML);

                //System.out.printf(userToken.toString());
        //String xml = freemarkerProcessor.toXml(userToken);
        //System.out.println(freemarkerProcessor.toXml(userToken));

        assertEquals("0", userToken.getPersonRef());
        assertEquals("User", userToken.getFirstName());
        assertEquals("Admin", userToken.getLastName());
        assertEquals("useradmin@getwhydah.com", userToken.getEmail());

        assertTrue(freemarkerProcessor.toXml(userToken).indexOf("UserAdmin") > 0);
        assertTrue(freemarkerProcessor.toXml(userToken).indexOf("WhydahUserAdmin") > 0);
    }

    @Test
    public void testUserAggregateParsing() throws Exception {
        String identityXML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<whydahuser>\n" +
                "    <identity>\n" +
                "        <username>admin</username>\n" +
                "        <cellPhone>+1555406789</cellPhone>\n" +
                "        <email>useradmin@getwhydah.com</email>\n" +
                "        <firstname>User</firstname>\n" +
                "        <lastname>Admin</lastname>\n" +
                "        <personRef>0</personRef>\n" +
                "        <UID>useradmin</UID>\n" +
                "    </identity>\n" +
                "    <applications>\n" +
                "        <application>\n" +
                "            <appId>19</appId>\n" +
                "            <applicationName>UserAdminWebApplication</applicationName>\n" +
                "            <orgName>Support</orgName>\n" +
                "            <roleName>WhydahUserAdmin</roleName>\n" +
                "            <roleValue>1</roleValue>\n" +
                "        </application>\n" +
                "        <application>\n" +
                "            <appId>19</appId>\n" +
                "            <applicationName>UserAdminWebApplication</applicationName>\n" +
                "            <orgName>Support</orgName>\n" +
                "            <roleName>UserAdmin</roleName>\n" +
                "            <roleValue>100</roleValue>\n" +
                "        </application>\n" +
                "        <application>\n" +
                "            <appId>121</appId>\n" +
                "            <applicationName>UserAdminWebApplication</applicationName>\n" +
                "            <orgName>Support</orgName>\n" +
                "            <roleName>UserAdmin</roleName>\n" +
                "            <roleValue>100</roleValue>\n" +
                "        </application>\n" +
                "        <application>\n" +
                "            <appId>19</appId>\n" +
                "            <applicationName>UserAdminWebApplication</applicationName>\n" +
                "            <orgName>Support</orgName>\n" +
                "            <roleName>TEST</roleName>\n" +
                "            <roleValue>3</roleValue>\n" +
                "        </application>\n" +
                "    </applications>\n" +
                "</whydahuser>\n";
        List<ApplicationRoleEntry> roleList = new LinkedList<>();

        DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
        Document doc = documentBuilder.parse(new InputSource(new StringReader(identityXML)));
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList applicationNodes = (NodeList) xPath.evaluate("/whydahuser/applications/application/appId", doc, XPathConstants.NODESET);
        for (int i = 1; i < applicationNodes.getLength() + 1; i++) {
            ApplicationRoleEntry role = new ApplicationRoleEntry();
            role.setApplicationId((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/appId", doc, XPathConstants.STRING));
            role.setOrganizationName((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/orgName", doc, XPathConstants.STRING));
            role.setRoleName((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/roleName", doc, XPathConstants.STRING));
            role.setRoleValue((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/roleValue", doc, XPathConstants.STRING));
            //System.out.println(role);
            roleList.add(role);
        }
        //System.out.println(roleList);
        assertTrue(roleList.size() == 4);

    }
    @Test
    public void testUserTokenFullUserToken() throws Exception {
        assertTrue(UserTokenFactory.shouldReturnFullUserToken("2211"));
        assertFalse(UserTokenFactory.shouldReturnFullUserToken("22121"));

    }

    @Test
    public void testUserTokenFiltering() throws Exception {
        String identityXML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<whydahuser>\n" +
                "    <identity>\n" +
                "        <username>admin</username>\n" +
                "        <cellPhone>+1555406789</cellPhone>\n" +
                "        <email>useradmin@getwhydah.com</email>\n" +
                "        <firstname>User</firstname>\n" +
                "        <lastname>Admin</lastname>\n" +
                "        <personRef>0</personRef>\n" +
                "        <UID>useradmin</UID>\n" +
                "    </identity>\n" +
                "    <applications>\n" +
                "        <application>\n" +
                "            <appId>19</appId>\n" +
                "            <applicationName>UserAdminWebApplication</applicationName>\n" +
                "            <orgName>Support</orgName>\n" +
                "            <roleName>WhydahUserAdmin</roleName>\n" +
                "            <roleValue>1</roleValue>\n" +
                "        </application>\n" +
                "        <application>\n" +
                "            <appId>19</appId>\n" +
                "            <applicationName>UserAdminWebApplication</applicationName>\n" +
                "            <orgName>Support</orgName>\n" +
                "            <roleName>UserAdmin</roleName>\n" +
                "            <roleValue>100</roleValue>\n" +
                "        </application>\n" +
                "        <application>\n" +
                "            <appId>121</appId>\n" +
                "            <applicationName>UserAdminWebApplication</applicationName>\n" +
                "            <orgName>Support</orgName>\n" +
                "            <roleName>UserAdmin</roleName>\n" +
                "            <roleValue>100</roleValue>\n" +
                "        </application>\n" +
                "        <application>\n" +
                "            <appId>19</appId>\n" +
                "            <applicationName>UserAdminWebApplication</applicationName>\n" +
                "            <orgName>Support</orgName>\n" +
                "            <roleName>TEST</roleName>\n" +
                "            <roleValue>3</roleValue>\n" +
                "        </application>\n" +
                "    </applications>\n" +
                "</whydahuser>\n";


        UserToken userToken = new UserTokenFactory("0").fromUserAggregate(identityXML);

        //System.out.printf(userToken.toString());
        //String xml = freemarkerProcessor.toXml(userToken);
        //System.out.println(freemarkerProcessor.toXml(userToken));

        assertEquals("0", userToken.getPersonRef());
        assertEquals("User", userToken.getFirstName());
        assertEquals("Admin", userToken.getLastName());
        assertEquals("useradmin@getwhydah.com", userToken.getEmail());

        assertTrue(freemarkerProcessor.toXml(userToken).indexOf("UserAdmin") > 0);
        assertTrue(freemarkerProcessor.toXml(userToken).indexOf("WhydahUserAdmin") > 0);

        ApplicationCredential cred = new ApplicationCredential("19","myapp","dummy");
        ApplicationToken imp = ApplicationTokenMapper.fromApplicationCredentialXML(ApplicationCredentialMapper.toXML(cred));
        AuthenticatedApplicationRepository.addApplicationToken(imp);



        List<ApplicationRoleEntry> origRoleList = userToken.getRoleList();
        List<ApplicationRoleEntry> roleList = new LinkedList<>();
        String myappid = AuthenticatedApplicationRepository.getApplicationIdFromApplicationTokenID(imp.getApplicationTokenId());
        for (int i=0;i<origRoleList.size();i++){
            ApplicationRoleEntry are = origRoleList.get(i);
            if (are.getApplicationId().equalsIgnoreCase(myappid)){
                roleList.add(are);
            }
        }
        assertTrue(roleList.size() == 3);

    }

    }
