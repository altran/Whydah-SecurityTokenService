package net.whydah.token;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import net.whydah.token.application.ApplicationCredential;
import net.whydah.token.config.ApplicationMode;
import net.whydah.token.user.UserCredential;
import org.junit.*;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static org.junit.Assert.assertTrue;

public class PostTest {
    private static URI baseUri;
    Client restClient;
    private static ServiceStarter serviceStarter;

    @BeforeClass
    public static void init() throws Exception {
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
        String appCredential = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><applicationcredential><appid>app123</appid><appsecret>123123</appsecret></applicationcredential>";
        String responseXML = logonApplication(appCredential);
        assertTrue(responseXML.contains("applicationtoken"));
        assertTrue(responseXML.contains("applicationid"));
        assertTrue(responseXML.contains("expires"));
        assertTrue(responseXML.contains("Url"));
    }

    @Test
    @Ignore
    public void testUserToken() {
        String apptokenxml = getAppToken();
        String applicationtokenid = getTokenIdFromAppToken(apptokenxml);
        UserCredential user = new UserCredential("nalle", "puh");

        WebResource userTokenResource = restClient.resource(baseUri).path("user/" + applicationtokenid + "/usertoken");
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("apptoken", apptokenxml);
        formData.add("usercredential", user.toXML());
        ClientResponse response = userTokenResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
        String responseXML = response.getEntity(String.class);
        //System.out.println(responseXML);
        assertTrue(responseXML.contains("securitylevel"));
        assertTrue(responseXML.contains("id=\""));
        assertTrue(responseXML.contains("personid"));
        assertTrue(responseXML.contains("hash"));
    }

    private String getAppToken() {
        ApplicationCredential acred = new ApplicationCredential();
        acred.setApplicationID("Whydah-TestWebApp");
        acred.setApplicationSecret("dummy");
        return logonApplication(acred.toXML());
    }

    private String logonApplication(String appCredential) {
        WebResource logonResource = restClient.resource(baseUri).path("logon");
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("applicationcredential", appCredential);
        ClientResponse response = logonResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
        return response.getEntity(String.class);
    }

    private String getTokenIdFromAppToken(String appTokenXML) {
        return appTokenXML.substring(appTokenXML.indexOf("<applicationtoken>") + "<applicationtoken>".length(), appTokenXML.indexOf("</applicationtoken>"));
    }
}
