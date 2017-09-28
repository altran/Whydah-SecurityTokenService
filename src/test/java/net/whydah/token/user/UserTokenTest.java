package net.whydah.token.user;

import net.whydah.sso.application.mappers.ApplicationCredentialMapper;
import net.whydah.sso.application.mappers.ApplicationTokenMapper;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.config.ApplicationMode;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserToken;
import net.whydah.token.application.ApplicationThreatResource;
import net.whydah.token.application.AuthenticatedApplicationTokenRepository;
import net.whydah.token.config.AppConfig;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final static Logger log = LoggerFactory.getLogger(UserTokenTest.class);
    private final static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();


    @BeforeClass
    public static void init() {
        System.setProperty(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);
        System.setProperty(AppConfig.IAM_CONFIG_KEY, "src/test/testconfig.properties");
    }


    @Test
    @Ignore
    public void testCreateUserToken() throws Exception {
        UserToken userToken = new UserToken();
        userToken.setFirstName("Ola");
        userToken.setEmail("test@whydah.net");
        userToken.setLastName("Nordmann");
        userToken.setTimestamp("123123123");
        userToken.setPersonRef("73637276722376");
        userToken.setDefcon(ApplicationThreatResource.getDEFCON());
        userToken.setTokenid(UUID.randomUUID().toString());
        String xml = freemarkerProcessor.toXml(userToken);

        UserToken copyToken = UserTokenMapper.fromUserTokenXml(xml);
        String copyxml = freemarkerProcessor.toXml(copyToken);
        //assertTrue("The generated user token is wrong.", xml.equalsIgnoreCase(copyxml));

        assertXMLEqual(xml, copyxml);
    }

    @Test
    public void testActiveUserTokenRepository() {
        UserToken userToken = new UserToken();
        userToken.setUserName(UUID.randomUUID().toString());
        userToken.setFirstName("Ola");
        userToken.setLastName("Nordmann");
        userToken.setEmail("test@whydah.net");
        userToken.setTimestamp(String.valueOf(System.currentTimeMillis()));
        userToken.setLifespan(String.valueOf(2 * 60 * 60 * new Random().nextInt(100)));

        userToken.setTokenid(UUID.randomUUID().toString());
        userToken.setPersonRef("78125637812638");

        AuthenticatedUserTokenRepository.addUserToken(userToken, "2012xxxx", "");
        assertTrue("Verification of valid token failed", AuthenticatedUserTokenRepository.verifyUserToken(userToken, "2012"));

        userToken.setFirstName("Pelle");
        String usertokenfromfreemarkertransformation = freemarkerProcessor.toXml(userToken);
        assertTrue("UserToken not updated", usertokenfromfreemarkertransformation.indexOf("Pelle") > 0);
        assertFalse("Verification of changed usertoken fail as it should", AuthenticatedUserTokenRepository.verifyUserToken(userToken, "2012xxxx"));
    }

    @Test
    public void testTimedOutActiveUserTokenRepository() {
        UserToken userToken = new UserToken();
        userToken.setUserName(UUID.randomUUID().toString());
        userToken.setTokenid(UUID.randomUUID().toString());
        userToken.setFirstName("Ola");
        userToken.setLastName("Nordmann");
        userToken.setEmail("test@whydah.net");
        userToken.setPersonRef("78125637812638");
        userToken.setTimestamp(String.valueOf(System.currentTimeMillis() - 1000));
        userToken.setLifespan("0");
        AuthenticatedUserTokenRepository.addUserToken(userToken, "", "");
        assertFalse("Verification of timed-out token successful", AuthenticatedUserTokenRepository.verifyUserToken(userToken, ""));
    }

    @Test
    @Ignore
    public void testCreateUserTokenWithRolesFreemarkerCopy() {
        UserToken userToken = new UserToken();
        userToken.setFirstName("Olav");
        userToken.setLastName("Nordmann");
        userToken.setEmail("test2@whydah.net");
        userToken.setTokenid(UUID.randomUUID().toString());
        userToken.addApplicationRoleEntry(new UserApplicationRoleEntry("2349785543", "Whydah.net", "Kunde 1", "Boardmember", "Diktator"));
        userToken.addApplicationRoleEntry(new UserApplicationRoleEntry("2349785543", "Whydah.net", "Kunde 2", "tester", "ansatt"));
        userToken.addApplicationRoleEntry(new UserApplicationRoleEntry("2349785543", "Whydah.net", "Kunde 3", "Boardmember", ""));
        userToken.addApplicationRoleEntry(new UserApplicationRoleEntry("appa", "whydag.org", "Kunde 1", "President", "Valla"));
        String tokenxml = freemarkerProcessor.toXml(userToken);

        UserToken copyToken = UserTokenMapper.fromUserTokenXml(tokenxml);
        String copyxml = freemarkerProcessor.toXml(copyToken);
        //System.out.println("FROM: " + tokenxml);
        //System.out.println("TO: " + copyxml);
        assertEquals(tokenxml, copyxml);
        UserToken copyToken2 = UserTokenMapper.fromUserTokenXml(tokenxml);
        String copyxml2 = freemarkerProcessor.toXml(copyToken2);
        //System.out.println("FILTERED: " + copyxml2);
        // assertFalse("Should not be equal as result is applicationfiltered ", tokenxml.equals(copyxml2));
    }

    @Test
    @Ignore
    public void testCreateUserTokenWithRolesUserTokenCopy() {
        UserToken userToken = new UserToken();
        userToken.setFirstName("Olav");
        userToken.setLastName("Nordmann");
        userToken.setEmail("test2@whydah.net");
        userToken.setTokenid(UUID.randomUUID().toString());
        userToken.addApplicationRoleEntry(new UserApplicationRoleEntry("2349785543", "Whydah.net", "Kunde 1", "Boardmember", "Diktator"));
        userToken.addApplicationRoleEntry(new UserApplicationRoleEntry("2349785543", "Whydah.net", "Kunde 2", "tester", "ansatt"));
        userToken.addApplicationRoleEntry(new UserApplicationRoleEntry("2349785543", "Whydah.net", "Kunde 3", "Boardmember", ""));
        userToken.addApplicationRoleEntry(new UserApplicationRoleEntry("appa", "whydag.org", "Kunde 1", "President", "Valla"));
        String tokenxml = freemarkerProcessor.toXml(userToken);

        UserToken copyToken = UserTokenMapper.fromUserTokenXml(tokenxml);
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
                "        <personref>0</personref>\n" +
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

        UserToken userToken = UserTokenMapper.fromUserAggregateXml(identityXML);

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
                "        <personref>0</personref>\n" +
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
        List<UserApplicationRoleEntry> roleList = new LinkedList<>();

        DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
        Document doc = documentBuilder.parse(new InputSource(new StringReader(identityXML)));
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList applicationNodes = (NodeList) xPath.evaluate("/whydahuser/applications/application/appId", doc, XPathConstants.NODESET);
        for (int i = 1; i < applicationNodes.getLength() + 1; i++) {
            UserApplicationRoleEntry role = new UserApplicationRoleEntry();
            role.setApplicationId((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/appId", doc, XPathConstants.STRING));
            role.setOrgName((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/orgName", doc, XPathConstants.STRING));
            role.setRoleName((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/roleName", doc, XPathConstants.STRING));
            role.setRoleValue((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/roleValue", doc, XPathConstants.STRING));
            //System.out.println(role);
            roleList.add(role);
        }
        //System.out.println(roleList);
        assertTrue(roleList.size() == 4);

    }

    /**
     * Need to rewrite this to test the complete flow
     *
     * @throws Exception
     */
    @Test
    @Ignore
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
                "        <personref>0</personref>\n" +
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


        UserToken userToken = UserTokenMapper.fromUserAggregateXml(identityXML);

        //System.out.printf(userToken.toString());
        //String xml = freemarkerProcessor.toXml(userToken);
        //System.out.println(freemarkerProcessor.toXml(userToken));

        assertEquals("0", userToken.getPersonRef());
        assertEquals("User", userToken.getFirstName());
        assertEquals("Admin", userToken.getLastName());
        assertEquals("useradmin@getwhydah.com", userToken.getEmail());

        assertTrue(freemarkerProcessor.toXml(userToken).indexOf("UserAdmin") > 0);
        assertTrue(freemarkerProcessor.toXml(userToken).indexOf("WhydahUserAdmin") > 0);

        ApplicationCredential cred = new ApplicationCredential("19", "myapp", "dummy");
        ApplicationToken imp = ApplicationTokenMapper.fromApplicationCredentialXML(ApplicationCredentialMapper.toXML(cred));
        AuthenticatedApplicationTokenRepository.addApplicationToken(imp);


        List<UserApplicationRoleEntry> origRoleList = userToken.getRoleList();
        List<UserApplicationRoleEntry> roleList = new LinkedList<>();
        String myappid = AuthenticatedApplicationTokenRepository.getApplicationIdFromApplicationTokenID(imp.getApplicationTokenId());
        for (int i = 0; i < origRoleList.size(); i++) {
            UserApplicationRoleEntry are = origRoleList.get(i);
            if (are.getApplicationId().equalsIgnoreCase(myappid)) {
                roleList.add(are);
            }
        }
        assertTrue(roleList.size() == 3);

    }

    @Test
    public void testActiveUserTokenExpiresRepository() {
        UserToken utoken = new UserToken();
        utoken.setUserName(UUID.randomUUID().toString());
        utoken.setFirstName("Ola");
        utoken.setLastName("Nordmann");
        utoken.setEmail("test@whydah.net");
        utoken.setTimestamp(String.valueOf(System.currentTimeMillis()));
        utoken.setLifespan(String.valueOf(2 * 60 * 60 * new Random().nextInt(100)));
        utoken.setTokenid(UUID.randomUUID().toString());
        utoken.setPersonRef("78125637812638");

        AuthenticatedUserTokenRepository.addUserToken(utoken, "", "");
        assertTrue("Verification of valid token failed", AuthenticatedUserTokenRepository.verifyUserToken(utoken, ""));

        utoken.setFirstName("Pelle");
        String token = freemarkerProcessor.toXml(utoken);
        assertTrue("Token not updated", token.indexOf("Pelle") > 0);
        assertFalse("Verification of in-valid token successful", AuthenticatedUserTokenRepository.verifyUserToken(utoken, ""));
    }

    @Test
    public void testAuthenticatedUserTokenRepositoryCleanup() throws Exception {
        UserToken utoken = new UserToken();
        utoken.setUserName(UUID.randomUUID().toString());
        utoken.setFirstName("Ola");
        utoken.setLastName("Nordmann");
        utoken.setEmail("test@whydah.net");
        utoken.setTimestamp(String.valueOf(System.currentTimeMillis()));
        utoken.setTokenid(UUID.randomUUID().toString());
        utoken.setPersonRef("78125637812638");
        utoken.setLifespan(String.valueOf(1 * 1000));

        int noOfUsers = AuthenticatedUserTokenRepository.getMapSize();
        log.debug("Users:" + noOfUsers);

        AuthenticatedUserTokenRepository.addUserToken(utoken, "", "");
        int noOfUsersAfter = AuthenticatedUserTokenRepository.getMapSize();
        log.debug("Users (after):" + noOfUsersAfter);
        assertTrue(noOfUsers < noOfUsersAfter);

        Thread.sleep(2000);
        AuthenticatedUserTokenRepository.cleanUserTokenMap();
        int noOfUsersAfter2 = AuthenticatedUserTokenRepository.getMapSize();
        log.debug("Users (after2):" + noOfUsersAfter2);
        assertTrue(noOfUsersAfter2 < noOfUsersAfter);

    }
}
