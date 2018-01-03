package net.whydah.sts;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import net.whydah.sso.application.helpers.ApplicationTokenXpathHelper;
import net.whydah.sso.application.mappers.ApplicationCredentialMapper;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.commands.systemtestbase.SystemTestBaseConfig;
import net.whydah.sso.config.ApplicationMode;
import net.whydah.sso.user.types.UserCredential;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static org.junit.Assert.assertTrue;

public class PostTest {
    private static URI baseUri;
    Client restClient;
    private static ServiceStarter serviceStarter;
    static SystemTestBaseConfig config;

    @BeforeClass
    public static void init() throws Exception {
        config = new SystemTestBaseConfig();
        System.setProperty(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);
        serviceStarter = new ServiceStarter();
        serviceStarter.startServer();
        baseUri = UriBuilder.fromUri("http://localhost/tokenservice/").port(serviceStarter.getPort()).build();
    }

    @Before
    public void initRun() throws Exception {
        restClient = ApacheHttpClient.create();
    }

    @AfterClass
    public static void teardown() throws Exception {
        serviceStarter.stop();
    }

    @Test
    public void testLogonApplication() {
        if (config.systemTest) {
            String appCredential = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><applicationcredential><appid>app123</appid><appsecret>123123password</appsecret></applicationcredential>";
//        String  appCredential = ApplicationCredentialMapper.toXML(ApplicationCredentialMapper.fromXml("<?xml version='1.0' encoding='UTF-8' standalone='yes'?><applicationcredential><appid>app123</appid><appsecret>123123</appsecret></applicationcredential>"));
            String responseXML = logonApplication(appCredential);
            assertTrue(responseXML.contains("applicationtoken"));
            assertTrue(responseXML.contains("applicationid"));
            assertTrue(responseXML.contains("expires"));
            assertTrue(responseXML.contains("Url"));
        }
    }

    @Test
    public void testLogonUIBApplication() {
        if (config.systemTest) {
            String appCredential =
                    " <applicationcredential>\n" +
                            "    <params>\n" +
                            "        <applicationID>2210</applicationID>\n" +
                            "        <applicationName>Whydah-UserIdentityBackend</applicationName>\n" +
                            "        <applicationSecret>6r46g3q986Ep6By7B9J46m96D</applicationSecret>\n" +
                            "        <applicationurl></applicationurl>\n" +
                            "        <minimumsecuritylevel>0</minimumsecuritylevel>" +
                            "    </params> \n" +
                            "</applicationcredential>\n";

            String responseXML = logonApplication(appCredential);
            assertTrue(responseXML.contains("applicationtoken"));
            assertTrue(responseXML.contains("applicationid"));
            assertTrue(responseXML.contains("expires"));
            assertTrue(responseXML.contains("Url"));
        }
    }

    @Test
    public void testLogonUASApplication() {
        if (config.systemTest) {

            String appCredential =
                    "<applicationcredential>\n" +
                            "    <params>\n" +
                            "        <applicationID>2212</applicationID>\n" +
                            "        <applicationName>INN UserAdminService-3</applicationName>\n" +
                            "        <applicationSecret>9ju592A4t8dzz8mz7a5QQJ7Px</applicationSecret>\n" +
                            "        <applicationurl></applicationurl>\n" +
                            "        <minimumsecuritylevel>0</minimumsecuritylevel>    </params> \n" +
                            "</applicationcredential>\n";

            String responseXML = logonApplication(appCredential);
            assertTrue(responseXML.contains("applicationtoken"));
            assertTrue(responseXML.contains("applicationid"));
            assertTrue(responseXML.contains("expires"));
            assertTrue(responseXML.contains("Url"));
        }

    }

    @Test
    public void testPostToGetUserToken() {
        if (config.systemTest) {
            String apptokenxml = getAppToken();
            String applicationtokenid = getApplicationTokenIdFromAppToken(apptokenxml);
            UserCredential user = new UserCredential("nalle", "puhpassword");


            WebResource userTokenResource = restClient.resource(baseUri).path("user").path(applicationtokenid).path("/usertoken");
            MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
            formData.add("apptoken", apptokenxml);
            formData.add("usercredential", user.toXML());
            ClientResponse response = userTokenResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
            System.out.println("Calling:" + userTokenResource.getURI());
            String responseXML = response.getEntity(String.class);
            System.out.println("responseXML:\n" + responseXML);
            assertTrue(responseXML.contains("usertoken"));
            assertTrue(responseXML.contains("DEFCON"));
            assertTrue(responseXML.contains("applicationName"));
            assertTrue(responseXML.contains("hash"));
        }
    }

    private String getAppToken() {
        ApplicationCredential acred = new ApplicationCredential("21356253", "ine app", "dummysecret");
        return logonApplication(ApplicationCredentialMapper.toXML(acred));
    }

    private String logonApplication(String appCredential) {
        WebResource logonResource = restClient.resource(baseUri).path("logon");
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("applicationcredential", appCredential);
        ClientResponse response = logonResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
        return response.getEntity(String.class);
    }

    private String getApplicationTokenIdFromAppToken(String appTokenXML) {
        return ApplicationTokenXpathHelper.getApplicationTokenIDFromApplicationToken(appTokenXML);
    }
}
