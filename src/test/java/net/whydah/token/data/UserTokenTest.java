package net.whydah.token.data;

import net.whydah.token.data.helper.ActiveUserTokenRepository;
import net.whydah.token.data.helper.FreemarkerProcessor;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class UserTokenTest {
    private FreemarkerProcessor freemarkerProcessor = new FreemarkerProcessor();

    @Test
    public void testCreateUserToken() {
        UserToken utoken = new UserToken();
        utoken.setFirstName("Ola");
        utoken.setLastName("Nordmann");
        utoken.setTimestamp("123123123");
        utoken.setPersonRef("73637276722376");
        utoken.setTokenid(UUID.randomUUID().toString());
        String xml = freemarkerProcessor.toXml(utoken);
        UserToken copyToken = UserToken.createFromUserTokenXML(xml);
        String copyxml = freemarkerProcessor.toXml(copyToken);
        assertTrue("The generated user token is wrong.", xml.equalsIgnoreCase(copyxml));
    }

    @Test
    public void testActiveUserTokenRepository(){
        UserToken utoken = new UserToken();
        utoken.setFirstName("Ola");
        utoken.setLastName("Nordmann");
        utoken.setTimestamp(String.valueOf(System.currentTimeMillis() + 1000));
        utoken.setTokenid(UUID.randomUUID().toString());
        utoken.setPersonRef("78125637812638");
        ActiveUserTokenRepository.addUserToken(utoken);
        assertTrue("Verification of valid token failed", ActiveUserTokenRepository.verifyUserToken(utoken));
        utoken.setFirstName("Pelle");
        String token = freemarkerProcessor.toXml(utoken);
        assertTrue("Token not updated", token.indexOf("Pelle") > 0);
        assertFalse("Verification of in-valid token successful", ActiveUserTokenRepository.verifyUserToken(utoken));
    }

    @Test
    public void testTimedOutActiveUserTokenRepository(){
        UserToken utoken = new UserToken();
        utoken.setTokenid(UUID.randomUUID().toString());
        utoken.setFirstName("Ola");
        utoken.setLastName("Nordmann");
        utoken.setPersonRef("78125637812638");
        utoken.setTimestamp(String.valueOf(System.currentTimeMillis() - 1000));
        utoken.setLifespan("0");
        ActiveUserTokenRepository.addUserToken(utoken);
        assertFalse("Verification of timed-out token successful", ActiveUserTokenRepository.verifyUserToken(utoken));
    }

    @Test
    public void testCreateUserTokenWithRoles() {
        UserToken utoken = new UserToken();
        utoken.setFirstName("Olav");
        utoken.setLastName("Nordmann");
        utoken.setTokenid(UUID.randomUUID().toString());
        utoken.putApplicationCompanyRoleValue("2349785543", "Whydah.net",  "Kunde 1", "styreleder", "Diktator");
        utoken.putApplicationCompanyRoleValue("2349785543", "Whydah.net",  "Kunde 2", "vaktmester", "ansatt");
        utoken.putApplicationCompanyRoleValue("2349785543", "Whydah.net",  "Kunde 3", "styremedlem", "");
        utoken.putApplicationCompanyRoleValue("appa", "whydag.org",  "Kunde 1", "styremedlem", "Valla");
        String tokenxml = freemarkerProcessor.toXml(utoken);

        UserToken copyToken = UserToken.createFromUserTokenXML(tokenxml);
        String copyxml = freemarkerProcessor.toXml(copyToken);
        System.out.println("FROM: "+tokenxml);
        System.out.println("TO: "+copyxml);
        assertEquals(tokenxml, copyxml);
    }

    @Test
    public void createFromUserIdentityXML() {
        String identityXML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<whydahuser>\n" +
                "    <identity>\n" +
                "        <username>test@hotmail.com</username>\n" +
                "        <cellPhone>90967400</cellPhone>\n" +
                "        <email>test@hotmail.com</email>\n" +
                "        <firstname>Test</firstname>\n" +
                "        <lastname>Testesen</lastname>\n" +
                "        <personRef>436276390081408</personRef>\n" +
                "        <UID>test@hotmail.com</UID>\n" +
                "    </identity>\n" +
                "        <application>\n" +
                "            <appId>Invoice</appId>\n" +
                "            <applicationName>Contempus Invoice</applicationName>\n" +
                "            <organizationName>ABC AS</organizationName>\n" +
                "            <roleName>VM</roleName>\n" +
                "            <roleValue>2010 - 2011</roleValue>\n" +
                "        </application>\n" +
                "        <application>\n" +
                "            <appId>Invoice</appId>\n" +
                "            <applicationName>ABC AS</applicationName>\n" +
                "            <organizationName>Etterstad</organizationName>\n" +
                "            <roleName>NT</roleName>\n" +
                "            <roleValue>2010 - 2011</roleValue>\n" +
                "        </application>\n" +
                "</whydahuser>";
        String appXML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n " +
                    " <token>\n" +
                    "     <params>\n" +
                    "         <applicationtoken>123123123123</applicationtoken>\n" +
                    "         <applicationid>123</applicationid>\n" +
                    "         <applicationname>faktura</applicationname>\n" +
                    "         <expires>3213213212</expires>\n" +
                    "     </params> \n" +
                    " </token>\n";
        UserToken userToken = UserToken.createUserIdentity(appXML, identityXML);

        //String xml = freemarkerProcessor.toXml(userToken);

        assertEquals("436276390081408", userToken.getPersonRef());
        assertEquals("Test", userToken.getFirstName());
        assertEquals("Testesen", userToken.getLastName());
        assertEquals("test@hotmail.com", userToken.getEmail());
    }
}
