package net.whydah.token.user;

import net.whydah.token.config.ApplicationMode;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2014-11-04
 */
public class UserToken2FactoryTest {
    private final String userTokenXml1 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"+
            "<usertoken xmlns:ns2=\"http://www.w3.org/1999/xhtml\" id=\"6f04e78e-bb75-4214-a786-2be7657bb38a\">\n"+
            "    <uid>uid1</uid>\n"+
            "    <timestamp>1415091487335</timestamp>\n"+
            "    <lifespan>82800</lifespan>\n"+
            "    <issuer>UserToken2FactoryTestTokenIssuer</issuer>\n"+
            "    <securitylevel>0</securitylevel>\n"+
            "    <DEFCON>5</DEFCON>\n"+
            "    <username>username1</username>\n"+
            "    <firstname>Olav</firstname>\n"+
            "    <lastname>Nordmann</lastname>\n"+
            "    <email>notworking@email.com</email>\n"+
            "    <personRef></personRef>\n"+
            "    <application ID=\"2349785543\">\n"+
            "        <applicationName>Whydah.net</applicationName>\n"+
            "        <organizationName>Kunde 3</organizationName>\n"+
            "        <role name=\"Boardmember\" value=\"\"/>\n"+
            "    </application>\n"+
            "    <application ID=\"appa\">\n"+
            "        <applicationName>whydag.org</applicationName>\n"+
            "        <organizationName>Kunde 1</organizationName>\n"+
            "        <role name=\"President\" value=\"Valla\"/>\n"+
            "    </application>\n"+
            "\n"+
            "    <ns2:link type=\"application/xml\" href=\"/6f04e78e-bb75-4214-a786-2be7657bb38a\" rel=\"self\"/>\n"+
            "    <hash type=\"MD5\">509eec7aef07357e4660f99f74255390</hash>\n"+
            "</usertoken>\n"+
            "\n";


    @BeforeClass
    public static void setEnv() {
        Map<String, String> envs = new HashMap<String, String>();
        envs.put(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);
        EnvHelper.setEnv(envs);
    }


    @Test
    public void testFromXml() {
        UserToken2Factory factory = new UserToken2Factory("1");
        UserToken2 userToken = factory.fromXml(userTokenXml1);
        assertEquals(userToken.getUid(), "uid1");
        assertEquals(userToken.getUserName(), "username1");
        assertEquals(userToken.getIssuer(), "UserToken2FactoryTestTokenIssuer");
        assertEquals(userToken.getDefcon(), "5");
        assertEquals(userToken.getTimestamp(), "1415091487335");
        assertEquals(userToken.getLifespan(), "82800");
        //assertEquals(userToken.getRoleList().size(), 2);
    }
}
