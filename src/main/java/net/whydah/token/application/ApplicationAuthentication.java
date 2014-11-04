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

@Path("/")
public class ApplicationAuthentication {
    private final static Logger logger = LoggerFactory.getLogger(ApplicationAuthentication.class);

    @Inject
    private AppConfig appConfig;


    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response info() {
        if ("enabled".equals(appConfig.getProperty("testpage"))) {
            //logger.debug("Showing test page");
            HashMap<String, Object> model = new HashMap<String, Object>();
            UserCredential testUserCredential = new UserCredential("whydah_user", "whydah_password");
            model.put("applicationcredential", new ApplicationCredential().toXML());
            model.put("testUserCredential", testUserCredential.toXML());
            return Response.ok().entity(new Viewable("/testpage.html.ftl", model)).build();
        } else {
            //logger.debug("Showing prod page");
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
        logger.trace("logonApplication with appCredentialXml={}", appCredentialXml);
        if (!verifyApplicationCredentials(appCredentialXml)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        ApplicationToken token = new ApplicationToken(appCredentialXml);
        token.setBaseuri(appConfig.getProperty("myuri"));
        AuthenticatedApplicationRepository.addApplicationToken(token);
        String applicationTokenXml = token.toXML();
        logger.trace("logonApplication returns applicationTokenXml={}", applicationTokenXml);
        return Response.ok().entity(applicationTokenXml).build();
    }

    @Path("{applicationtokenid}/validate")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response validateApplicationtokenid(@PathParam("applicationtokenid") String applicationtokenid) {
        logger.debug("verify apptokenid {}", applicationtokenid);
        if (AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            logger.debug("Apptokenid valid");
            return Response.ok().build();
        } else {
            logger.debug("Apptokenid not valid");
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    private boolean verifyApplicationCredentials(String appCredentials) {
        if (ApplicationMode.getApplicationMode().equals(ApplicationMode.DEV)) {
            logger.trace("verifyApplicationCredentials - running in DEV mode, auto accepted.");
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
                logger.warn("Application authentication failed. No or null applicationID");
                return false;

            }
            if (appSecret == null || appSecret.length() < 2) {
                logger.warn("verifyApplicationCredentials - verify appSecret failed. No or null applicationSecret");
                logger.warn("Application authentication failed. No or null applicationSecret for applicationId={}", appId);
                return false;
            }


            String expectedAppSecret = appConfig.getProperty(appId);
            logger.trace("verifyApplicationCredentials: appid={}, appSecret={}, expectedAppSecret={}", appId, appSecret, expectedAppSecret);

            if (expectedAppSecret == null || expectedAppSecret.length() < 2) {
                logger.warn("Application authentication failed. No application secret in property file for applicationId={}", appId);
                return false;
            }
            if (!appSecret.equalsIgnoreCase(expectedAppSecret)) {
                logger.warn("Application authentication failed. Incoming applicationSecret does not match applicationSecret from property file.");
                return false;
            }
        } catch (Exception e) {
            logger.error("Error in verifyApplicationCredentials.", e);
            return false;
        }
        return true;
    }
}
