package net.whydah.token.application;

import com.google.inject.Inject;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import net.whydah.sso.application.helpers.ApplicationCredentialHelper;
import net.whydah.sso.application.mappers.ApplicationCredentialMapper;
import net.whydah.sso.application.mappers.ApplicationTokenMapper;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.token.config.AppConfig;
import net.whydah.token.config.ApplicationMode;
import net.whydah.token.user.UserCredential;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Random;

@Path("/")
public class ApplicationAuthenticationResource {
    private final static Logger log = LoggerFactory.getLogger(ApplicationAuthenticationResource.class);
    private static final String APPLICATION_AUTH_PATH = "application/auth";
    public static final String APP_CREDENTIAL_XML = "appCredentialXml";

    @Inject
    private AppConfig appConfig;


    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response info() {
        if ("enabled".equals(appConfig.getProperty("testpage"))) {
            //log.debug("Showing test page");
            HashMap<String, Object> model = new HashMap<String, Object>();
            UserCredential testUserCredential = new UserCredential("whydah_user", "whydah_password");
            model.put("applicationcredential", ApplicationCredentialHelper.getDummyApplicationCredential());
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
        return Response.ok().entity(ApplicationTokenMapper.toXML(template)).build();
    }


    @Path("/applicationcredentialtemplate")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response getApplicationCredentialsTemplate() {
        ApplicationCredential template = ApplicationCredentialMapper.fromXml(ApplicationCredentialHelper.getDummyApplicationCredential());
        return Response.ok().entity(ApplicationCredentialMapper.toXML(template)).build();
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
        ApplicationToken token = ApplicationTokenMapper.fromApplicationCredentialXML(appCredentialXml);
        token.setBaseuri(appConfig.getProperty("myuri"));
        token.setExpires(String.valueOf((System.currentTimeMillis() + 10 * new Random().nextInt(500))));
        AuthenticatedApplicationRepository.addApplicationToken(token);
        String applicationTokenXml = ApplicationTokenMapper.toXML(token);
        log.trace("logonApplication returns applicationTokenXml={}", applicationTokenXml);
        return Response.ok().entity(applicationTokenXml).build();
    }

    @Path("{applicationtokenid}/renew_applicationtoken")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response validateApplicationTokenId(@PathParam("applicationtokenid") String applicationtokenid) {
        log.debug("verify applicationtokenid {}", applicationtokenid);
        if (AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            log.debug("applicationtokenid valid");
            return Response.ok().build();
        } else {
            log.debug("applicationtokenid not valid");
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @Path("{applicationtokenid}/validate")
    @GET
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response extendApplicationSession(@PathParam("applicationtokenid") String applicationtokenid) {
        log.debug("verify applicationtokenid {}", applicationtokenid);
        if (AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            ApplicationToken token = AuthenticatedApplicationRepository.renewApplicationTokenId(applicationtokenid);
            log.debug("Apptoken extended");
            String applicationTokenXml = ApplicationTokenMapper.toXML(token);
            log.trace("extendApplicationSession returns applicationTokenXml={}", applicationTokenXml);
            return Response.ok().entity(applicationTokenXml).build();
        } else {
            log.debug("applicationtokenid not valid");
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @Path("{applicationtokenid}/get_application_id")
    @GET
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getApplicationIdFromApplicationTokenId(@PathParam("applicationtokenid") String applicationtokenid) {
        log.debug("verify apptokenid {}", applicationtokenid);
        ApplicationToken myApp = AuthenticatedApplicationRepository.getApplicationToken(applicationtokenid);
        if (myApp != null || myApp.toString().length() > 10) {
            log.debug("Apptokenid valid");
            return Response.ok(myApp.getApplicationID()).build();
        } else {
            log.debug("Apptokenid not valid");
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @Path("{applicationtokenid}/get_application_name")
    @GET
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getApplicationNameFromApplicationTokenId(@PathParam("applicationtokenid") String applicationtokenid) {
        log.debug("verify apptokenid {}", applicationtokenid);
        ApplicationToken myApp = AuthenticatedApplicationRepository.getApplicationToken(applicationtokenid);
        if (myApp != null || myApp.toString().length() > 10) {
            log.debug("Apptokenid valid");
            return Response.ok(myApp.getApplicationName()).build();
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
            ApplicationCredential applicationCredential = ApplicationCredentialMapper.fromXml(appCredentials);
            if (applicationCredential.getApplicationID() == null || applicationCredential.getApplicationID().length() < 2) {
                log.warn("Application authentication failed. No or null applicationID");
                return false;

            }
            if (applicationCredential.getApplicationSecret() == null || applicationCredential.getApplicationSecret().length() < 2) {
                log.warn("verifyApplicationCredentials - verify appSecret failed. No or null applicationSecret");
                log.warn("Application authentication failed. No or null applicationSecret for applicationId={}", applicationCredential.getApplicationID());
                return false;
            }


            String expectedAppSecret = appConfig.getProperty(applicationCredential.getApplicationID());
            log.trace("verifyApplicationCredentials: appid={}, appSecret={}, expectedAppSecret={}", applicationCredential.getApplicationID(), applicationCredential.getApplicationSecret(), expectedAppSecret);

            if (expectedAppSecret == null || expectedAppSecret.length() < 2) {
                log.warn("No application secret in property file for applicationId={} - applicationName: {} - Trying UAS/UIB", applicationCredential.getApplicationID(), applicationCredential.getApplicationName());
                if (!checkAppsecretFromUAS(applicationCredential)) {
                    log.warn("Application authentication failed. Incoming applicationSecret does not match applicationSecret in UIB");
                    return false;
                } else {
                    log.warn("Application authentication OK for appId:{}, applicationName: {} from UAS", applicationCredential.getApplicationID(), applicationCredential.getApplicationName());
                    return true;
                }
            }
            if (!applicationCredential.getApplicationSecret().equalsIgnoreCase(expectedAppSecret)) {
                log.info("Incoming applicationSecret does not match applicationSecret from property file. - Trying UAS/UIB");
                if (!checkAppsecretFromUAS(applicationCredential)) {
                    log.warn("Application authentication failed. Incoming applicationSecret does not match applicationSecret in UIB");
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Error in verifyApplicationCredentials.", e);
            return false;
        }
        return true;
    }

    private boolean checkAppsecretFromUAS(ApplicationCredential applicationCredential) {
        ApplicationToken token = ApplicationTokenMapper.fromApplicationCredentialXML(ApplicationCredentialMapper.toXML(applicationCredential));
        token.setBaseuri(appConfig.getProperty("myuri"));
        token.setExpires(String.valueOf((System.currentTimeMillis() + 10 * new Random().nextInt(500))));

        String useradminservice = appConfig.getProperty("useradminservice");
        ApplicationToken stsToken = getSTSApplicationToken();
        AuthenticatedApplicationRepository.addApplicationToken(stsToken);

        WebResource uasResource = ApacheHttpClient.create().resource(useradminservice);
        int uasResponseCode = 0;
        WebResource webResource = uasResource.path(stsToken.getApplicationTokenId()).path(APPLICATION_AUTH_PATH);
        log.info("checkAppsecretFromUAS - Calling application auth " + webResource.toString());
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add(APP_CREDENTIAL_XML, ApplicationCredentialMapper.toXML(applicationCredential));
        try {
            ClientResponse response = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
            uasResponseCode = response.getStatus();
            log.info("Response from UAS:" + uasResponseCode);
            if (uasResponseCode == 204) {
                return true;
            }
        } catch (Exception e) {
            log.error("checkAppsecretFromUAS - Problems connecting to {}", useradminservice);
            throw e;
        }
        log.warn("Illegal application tried to access whydah. ApplicationID: {}, Response from UAS: {}", applicationCredential.getApplicationID(), uasResponseCode);
        return false;
    }

    private ApplicationToken getSTSApplicationToken() {
        AppConfig appConfig = new AppConfig();
        String applicationName = appConfig.getProperty("applicationname");
        String applicationId = appConfig.getProperty("applicationid");
        String applicationsecret = appConfig.getProperty("applicationsecret");
        ApplicationCredential ac = new ApplicationCredential(applicationId, applicationName, applicationsecret);
        ApplicationToken myToken = ApplicationTokenMapper.fromApplicationCredentialXML(ApplicationCredentialMapper.toXML(ac));

        return myToken;

    }
}
