package net.whydah.sts.user;

import net.whydah.sso.config.ApplicationMode;
import net.whydah.sso.user.types.UserToken;
import net.whydah.sts.user.authentication.DummyUserAuthenticator;
import net.whydah.sts.user.authentication.UserAuthenticator;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;


public class UserAuthenticatorTest {


    String applicationTokenId = "81dc9bdb52d04dc20036dbd8313ed055";
    String appTokenXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><token>\n" +
            "     <params>\n" +
            "         <applicationtokenID>81dc9bdb52d04dc20036dbd8313ed055</applicationtokenID>\n" +
            "         <applicationid>2312</applicationid>\n" +
            "         <applicationname>TestApplication</applicationname>\n" +
            "         <expires>" + System.currentTimeMillis() + 2000 + "</expires>\n" +
            "     </params> \n" +
            "     <Url type=\"application/xml\" method=\"POST\" template=\"http://localhost:9998/tokenservice/user/81dc9bdb52d04dc20036dbd8313ed055/get_usertoken_by_usertokenid\"/> \n" +
            " </token>";
    String userCredentialXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n" +
            " <usercredential>\n" +
            "    <params>\n" +
            "        <username>test@hotmail.com</username>\n" +
            "        <password>061073</password>\n" +
            "    </params> \n" +
            "</usercredential>\n";

    @Test
    public void testDummyUserAuthenticator() {
        Map<String, String> envs = new HashMap<String, String>();
        envs.put(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);
        EnvHelper.setEnv(envs);
        UserAuthenticator ua = new DummyUserAuthenticator();
        UserToken ut = ua.logonUser(applicationTokenId, appTokenXml, userCredentialXml);
        // System.out.println(ut.toString());
        assertNotNull(ut);
    }


}
