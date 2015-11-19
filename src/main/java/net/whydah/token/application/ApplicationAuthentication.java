package net.whydah.token.application;

import com.google.inject.Inject;
import com.sun.jersey.api.view.Viewable;
import net.whydah.token.config.AppConfig;
import net.whydah.token.config.ApplicationMode;
import net.whydah.token.user.UserCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Random;

@Path("/")
public class ApplicationAuthentication {
    private final static Logger log = LoggerFactory.getLogger(ApplicationAuthentication.class);

    @Inject
    private AppConfig appConfig;


    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response info() {
        if ("enabled".equals(appConfig.getProperty("testpage"))) {
            //log.debug("Showing test page");
            HashMap<String, Object> model = new HashMap<String, Object>();
            UserCredential testUserCredential = new UserCredential("whydah_user", "whydah_password");
            model.put("applicationcredential", new ApplicationCredential().toXML());
            model.put("testUserCredential", testUserCredential.toXML());
            return Response.ok().entity(new Viewable("/testpage.html.ftl", model)).build();
        } else {
            //log.debug("Showing prod page");
            return Response.ok().entity(new Viewable("/html/prodwelcome.html")).build();
        }
    }


    @Path("/applicationtokentemplate")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response getApplicationTokenTemplate() {
        ApplicationToken template = new ApplicationToken();
        return Response.ok().entity(template.toXML()).build();
    }


    @Path("/applicationcredentialtemplate")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response getApplicationCredentialsTemplate() {
        ApplicationCredential template = new ApplicationCredential();
        return Response.ok().entity(template.toXML()).build();
    }

    @Path("/usercredentialtemplate")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response getUserCredentialsTemplate() {
        UserCredential template = new UserCredential("", "");
        return Response.ok().entity(template.toXML()).build();
    }

    @Path("/logon")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response logonApplication(@FormParam("applicationcredential") String appCredentialXml) {
        log.trace("logonApplication with appCredentialXml={}", appCredentialXml);
        if (!verifyApplicationCredentials(appCredentialXml)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        ApplicationToken token = new ApplicationToken(appCredentialXml);
        token.setBaseuri(appConfig.getProperty("myuri"));
        token.setExpires(String.valueOf((System.currentTimeMillis() + 10* new Random().nextInt(500))));
        AuthenticatedApplicationRepository.addApplicationToken(token);
        String applicationTokenXml = token.toXML();
        log.trace("logonApplication returns applicationTokenXml={}", applicationTokenXml);
        return Response.ok().entity(applicationTokenXml).build();
    }

    @Path("{applicationtokenid}/validate")
    @GET
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response validateApplicationTokenId(@PathParam("applicationtokenid") String applicationtokenid) {
        log.debug("verify apptokenid {}", applicationtokenid);
        if (AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            log.debug("Apptokenid valid");
            return Response.ok().build();
        } else {
            log.debug("Apptokenid not valid");
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    //ED: I think/hope this can be removed...
    @Path("{applicationtokenid}/validate")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Deprecated
    public Response validateApplicationtokenidPOST(@PathParam("applicationtokenid") String applicationtokenid) {
        log.debug("verify apptokenid {}", applicationtokenid);
        if (AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            log.debug("Apptokenid valid");
            return Response.ok().build();
        } else {
            log.debug("Apptokenid not valid");
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    private boolean verifyApplicationCredentials(String appCredentials) {
        if (ApplicationMode.getApplicationMode().equals(ApplicationMode.DEV)) {
            log.trace("verifyApplicationCredentials - running in DEV mode, auto accepted.");
            return true;
        }

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(appCredentials)));
            XPath xPath = XPathFactory.newInstance().newXPath();

            String secretxpath = "applicationcredential/params/applicationSecret";
            String appidxpath = "applicationcredential/params/applicationID";
            XPathExpression xPathExpression = xPath.compile(secretxpath);
            String appSecret = (xPathExpression.evaluate(doc));
            xPathExpression = xPath.compile(appidxpath);
            String appId = (xPathExpression.evaluate(doc));




            if (appId == null || appId.length() < 2) {
                log.warn("Application authentication failed. No or null applicationID");
                return false;

            }
            if (appSecret == null || appSecret.length() < 2) {
                log.warn("verifyApplicationCredentials - verify appSecret failed. No or null applicationSecret");
                log.warn("Application authentication failed. No or null applicationSecret for applicationId={}", appId);
                return false;
            }


            String expectedAppSecret = appConfig.getProperty(appId);
            log.trace("verifyApplicationCredentials: appid={}, appSecret={}, expectedAppSecret={}", appId, appSecret, expectedAppSecret);

            if (expectedAppSecret == null || expectedAppSecret.length() < 2) {
                log.warn("Application authentication failed. No application secret in property file for applicationId={}", appId);
                return false;
            }
            if (!appSecret.equalsIgnoreCase(expectedAppSecret)) {
                if (!checkAppsecretFromUAS(expectedAppSecret)) {
                    log.warn("Application authentication failed. Incoming applicationSecret does not match applicationSecret from property file.");
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Error in verifyApplicationCredentials.", e);
            return false;
        }
        return true;
    }

    private boolean checkAppsecretFromUAS(String inputSecret){
        /**
         *         WebTarget applicationList = uasClient.target(userAdminServiceUri).path(myAppTokenId + "/" + adminUserTokenId + "/applications");

         Response response = applicationList.request().get();
         if (response.getStatus() == FORBIDDEN.getStatusCode()) {
         return null;
         //throw new IllegalArgumentException("Log on failed. " + ClientResponse.Status.FORBIDDEN);
         }
         if (response.getStatus() == OK.getStatusCode()) {
         String responseJson = response.readEntity(String.class);

         return ApplicationJsonpathHelper.findApplicationSecretFromApplicationListById(responseJson, inputSecret));
         }

         */
        return false;

    }
}
