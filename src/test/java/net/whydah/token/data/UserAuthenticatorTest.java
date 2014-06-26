package net.whydah.token.data;

import net.whydah.token.config.ApplicationMode;
import net.whydah.token.data.helper.DummyUserAuthenticator;
import net.whydah.token.data.helper.EnvHelper;
import net.whydah.token.data.helper.UserAuthenticator;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by totto on 6/20/14.
 */
public class UserAuthenticatorTest {


    String applicationTokenId="81dc9bdb52d04dc20036dbd8313ed055";
    String appTokenXml="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><token>\n" +
            "     <params>\n" +
            "         <applicationtokenID>81dc9bdb52d04dc20036dbd8313ed055</applicationtokenID>\n" +
            "         <applicationid>23</applicationid>\n" +
            "         <applicationname>1234</applicationname>\n" +
            "         <expires>1403187368265</expires>\n" +
            "     </params> \n" +
            "     <Url type=\"application/xml\" method=\"POST\" template=\"http://localhost:9998/tokenservice/token/81dc9bdb52d04dc20036dbd8313ed055/getusertokenbytokenid\"/> \n" +
            " </token>";
    String userCredentialXml= "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n" +
            " <usercredential>\n" +
            "    <params>\n" +
            "        <username>test@hotmail.com</username>\n" +
            "        <password>061073</password>\n" +
            "    </params> \n" +
            "</usercredential>\n";

    @Test
    public void testDummyUserAuthenticator() {
        Map<String, String> envs = new HashMap<String, String>();
        envs.put(ApplicationMode.IAM_MODE_KEY,ApplicationMode.DEV);
        EnvHelper.setEnv(envs);
        UserAuthenticator ua = new DummyUserAuthenticator();
        UserToken ut = ua.logonUser(applicationTokenId,appTokenXml,userCredentialXml);
        System.out.println(ut.toString());
    }


}
