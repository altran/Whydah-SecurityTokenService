package net.whydah.token.application;

import com.google.inject.Inject;
import com.sun.jersey.api.view.Viewable;

import net.whydah.errorhandling.AppException;
import net.whydah.errorhandling.AppExceptionCode;
import net.whydah.sso.application.helpers.ApplicationCredentialHelper;
import net.whydah.sso.application.mappers.ApplicationCredentialMapper;
import net.whydah.sso.application.mappers.ApplicationTokenMapper;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.config.ApplicationMode;
import net.whydah.token.config.AppConfig;
import net.whydah.token.user.UserCredential;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.HashMap;

@Path("/")
public class ApplicationAuthenticationResource {
    private final static Logger log = LoggerFactory.getLogger(ApplicationAuthenticationResource.class);

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
            return Response.ok().entity(new Viewable("/testpage.html.ftl", model)).header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
        } else {
            //log.debug("Showing prod page");
            return Response.ok().entity(new Viewable("/html/prodwelcome.html")).header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
        }
    }


    @Path("/applicationtokentemplate")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response getApplicationTokenTemplate() {
        ApplicationToken template = new ApplicationToken();
        return Response.ok().entity(ApplicationTokenMapper.toXML(template)).header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
    }


    @Path("/applicationcredentialtemplate")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response getApplicationCredentialsTemplate() {
        ApplicationCredential template = ApplicationCredentialMapper.fromXml(ApplicationCredentialHelper.getDummyApplicationCredential());
        return Response.ok().entity(ApplicationCredentialMapper.toXML(template)).header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
    }

    @Path("/usercredentialtemplate")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response getUserCredentialsTemplate() {
        UserCredential template = new UserCredential("", "");
        return Response.ok().entity(template.toXML()).header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
    }

    /**
	 * @throws AppException 
	 * @api {post} logon logonApplication
	 * @apiName logon
	 * @apiGroup Security Token Service (STS)
	 * @apiDescription Log on my application and get the application specification 
	 * 
	 * @apiParam {String} applicationcredential A label for this address.
	 * @apiParamExample {xml} Request-Example:
	 * &lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt; 
 	 * &lt;applicationcredential&gt;
     * &lt;params&gt;
     * &lt;applicationID&gt;101&lt;/applicationID&gt;
     *  &lt;applicationName&gt;Whydah-SystemTests&lt;/applicationName&gt;
     *   &lt;applicationSecret&gt;55fhRM6nbKZ2wfC6RMmMuzXpk&lt;/applicationSecret&gt;
     * &lt;/params&gt;
	 * &lt;/applicationcredential&gt;
	 *
	 * @apiSuccessExample Success-Response:
	 *	HTTP/1.1 200 OK
	 *	&lt;applicationtoken&gt;
     * 		&lt;params&gt;
     *		    &lt;applicationtokenID&gt;1d58b70dc0fdc98b5cdce4745fb086c4&lt;/applicationtokenID&gt;
     *		    &lt;applicationid&gt;101&lt;/applicationid&gt;
     *			&lt;applicationname&gt;Whydah-SystemTests&lt;/applicationname&gt;
     *			&lt;expires&gt;1480931112185&lt;/expires&gt;
     *		&lt;/params&gt; 
     * 		&lt;Url type="application/xml" method="POST" template="https://whydahdev.cantara.no/tokenservice/user/1d58b70dc0fdc98b5cdce4745fb086c4/get_usertoken_by_usertokenid"/&gt; 
 	 *	&lt;/applicationtoken&gt;
	 *
	 *
	 * @apiError 403/7000 Application is invalid.
	 * @apiError 500/9999 A generic exception or an unexpected error 
	 *
	 * @apiErrorExample Error-Response:
	 *     HTTP/1.1 403 Forbidden
	 *     {
	 *  		"status": 403,
	 *  		"code": 7000,
	 *  		"message": "Illegal Application.",
	 *  		"link": "",
	 *  		"developerMessage": "Application is invalid."
	 *		}
	 */
    @Path("/logon")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response logonApplication(@FormParam("applicationcredential") String appCredentialXml) throws AppException {
        log.trace("logonApplication with applicationcredential={}", appCredentialXml);
        if (!verifyApplicationCredentials(appCredentialXml)) {
            log.warn("logonApplication - illegal applicationcredential applicationID:{} , returning FORBIDDEN", ApplicationCredentialMapper.fromXml(appCredentialXml).getApplicationID());
            //return Response.status(Response.Status.FORBIDDEN).header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
            throw AppExceptionCode.APP_ILLEGAL_7000;
        }
        ApplicationToken token = ApplicationTokenMapper.fromApplicationCredentialXML(appCredentialXml);
        if (token.getApplicationName() == null || token.getApplicationName().length() < 1) {
            log.warn("Old Whydah ApplicationCredential received, please inform application owner to update the ApplicationCredential. ApplicationCredential:" + appCredentialXml);
        }
        token.setBaseuri(appConfig.getProperty("myuri"));
        token.setExpires(String.valueOf((System.currentTimeMillis() + AuthenticatedApplicationRepository.DEFAULT_SESSION_EXTENSION_TIME_IN_SECONDS * 1000)));
        AuthenticatedApplicationRepository.addApplicationToken(token);
        String applicationTokenXml = ApplicationTokenMapper.toXML(token);
        log.trace("logonApplication returns applicationTokenXml={}", applicationTokenXml);
        return Response.ok().entity(applicationTokenXml).header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
    }

    /**
   	 * @throws AppException 
   	 * @api {get} :applicationtokenid/validate validateApplicationTokenId
   	 * @apiName validateApplicationTokenId
   	 * @apiGroup Security Token Service (STS)
   	 * @apiDescription Validate application by an application token id 
   	 * 
   	 * @apiSuccessExample Success-Response:
   	 *	HTTP/1.1 200 OK
	 *	{
	 *  	"result": "true"
	 *	}
   	 *
   	 *
   	 * @apiError 403/7000 Application is invalid.
   	 * @apiError 500/9999 A generic exception or an unexpected error 
   	 *
   	 * @apiErrorExample Error-Response:
   	 *     HTTP/1.1 403 Forbidden
   	 *     {
   	 *  		"status": 403,
   	 *  		"code": 7000,
   	 *  		"message": "Illegal Application.",
   	 *  		"link": "",
   	 *  		"developerMessage": "Application is invalid."
   	 *		}
   	 */
    @Path("{applicationtokenid}/validate")
    @GET
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response validateApplicationTokenId(@PathParam("applicationtokenid") String applicationtokenid) throws AppException {
        log.trace("validateApplicationTokenId - validate applicationtokenid:{}", applicationtokenid);
        if (AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            log.debug("validateApplicationTokenId - applicationtokenid:{} for applicationname:{} is valid", applicationtokenid, AuthenticatedApplicationRepository.getApplicationToken(applicationtokenid).getApplicationName());
            return Response.ok("{\"result\": \"true\"}").header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
        } else {
            log.warn("validateApplicationTokenId - applicationtokenid:{}  is not valid", applicationtokenid);
            throw AppExceptionCode.APP_ILLEGAL_7000;
            //return Response.status(Response.Status.FORBIDDEN).header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
        }
    }

    /**
   	 * @throws AppException 
   	 * @api {post} :applicationtokenid/renew_applicationtoken extendApplicationSession
   	 * @apiName extendApplicationSession
   	 * @apiGroup Security Token Service (STS)
   	 * @apiDescription Extend my application session
   	 * 
   	 * @apiSuccessExample Success-Response:
	 *	HTTP/1.1 200 OK
	 *	&lt;applicationtoken&gt;
     * 		&lt;params&gt;
     *		    &lt;applicationtokenID&gt;1d58b70dc0fdc98b5cdce4745fb086c4&lt;/applicationtokenID&gt;
     *		    &lt;applicationid&gt;101&lt;/applicationid&gt;
     *			&lt;applicationname&gt;Whydah-SystemTests&lt;/applicationname&gt;
     *			&lt;expires&gt;1480931112185&lt;/expires&gt;
     *		&lt;/params&gt; 
     * 		&lt;Url type="application/xml" method="POST" template="https://whydahdev.cantara.no/tokenservice/user/1d58b70dc0fdc98b5cdce4745fb086c4/get_usertoken_by_usertokenid"/&gt; 
 	 *	&lt;/applicationtoken&gt;
   	 *
   	 * @apiError 403/7000 Application is invalid.
   	 * @apiError 500/9999 A generic exception or an unexpected error 
   	 *
   	 * @apiErrorExample Error-Response:
   	 *     HTTP/1.1 403 Forbidden
   	 *     {
   	 *  		"status": 403,
   	 *  		"code": 7000,
   	 *  		"message": "Illegal Application.",
   	 *  		"link": "",
   	 *  		"developerMessage": "Application is invalid."
   	 *		}
   	 */
    @Path("{applicationtokenid}/renew_applicationtoken")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response extendApplicationSession(@PathParam("applicationtokenid") String applicationtokenid) throws AppException {
        log.debug("renew session for applicationtokenid: {}", applicationtokenid);
        if (AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            ApplicationToken token = AuthenticatedApplicationRepository.renewApplicationTokenId(applicationtokenid);
            log.info("ApplicationToken for {} extended, expires: {}", token.getApplicationName(), token.getExpiresFormatted());
            String applicationTokenXml = ApplicationTokenMapper.toXML(token);
            log.trace("extendApplicationSession returns applicationTokenXml={}", applicationTokenXml);
            return Response.ok().entity(applicationTokenXml).header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
        } else {
            log.warn("applicationtokenid={} not valid", applicationtokenid);
            throw AppExceptionCode.APP_ILLEGAL_7000;
            //return Response.status(Response.Status.FORBIDDEN).header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
        }
    }

    /**
   	 * @throws AppException 
   	 * @api {get} :applicationtokenid/get_application_id getApplicationId
   	 * @apiName getApplicationIdFromApplicationTokenId
   	 * @apiGroup Security Token Service (STS)
   	 * @apiDescription Get my application id from an application token id
   	 * 
   	 * @apiSuccessExample Success-Response:
	 *	HTTP/1.1 200 OK plain/text
	 *	101
   	 *
   	 * @apiError 403/7000 Application is invalid.
   	 * @apiError 500/9999 A generic exception or an unexpected error 
   	 *
   	 * @apiErrorExample Error-Response:
   	 *     HTTP/1.1 403 Forbidden
   	 *     {
   	 *  		"status": 403,
   	 *  		"code": 7000,
   	 *  		"message": "Illegal Application.",
   	 *  		"link": "",
   	 *  		"developerMessage": "Application is invalid."
   	 *		}
   	 */
    @Path("{applicationtokenid}/get_application_id")
    @GET
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getApplicationIdFromApplicationTokenId(@PathParam("applicationtokenid") String applicationtokenid) throws AppException {
        log.debug("verify apptokenid {}", applicationtokenid);
        ApplicationToken myApp = AuthenticatedApplicationRepository.getApplicationToken(applicationtokenid);
        if (myApp != null || myApp.toString().length() > 10) {
            log.debug("Apptokenid valid");
            return Response.ok(myApp.getApplicationID()).header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
        } else {
            log.debug("Apptokenid not valid");
            //return Response.status(Response.Status.FORBIDDEN).header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
            throw AppExceptionCode.APP_ILLEGAL_7000;
        }
    }

    /**
   	 * @throws AppException 
   	 * @api {get} :applicationtokenid/get_application_name getApplicationName
   	 * @apiName getApplicationNameFromApplicationTokenId
   	 * @apiGroup Security Token Service (STS)
   	 * @apiDescription Get my application name from an application token id
   	 * 
   	 * @apiSuccessExample Success-Response:
	 *	HTTP/1.1 200 OK plain/text
	 *	Whydah-SystemTests
   	 *
   	 * @apiError 403/7000 Application is invalid.
   	 * @apiError 500/9999 A generic exception or an unexpected error 
   	 *
   	 * @apiErrorExample Error-Response:
   	 *     HTTP/1.1 403 Forbidden
   	 *     {
   	 *  		"status": 403,
   	 *  		"code": 7000,
   	 *  		"message": "Illegal Application.",
   	 *  		"link": "",
   	 *  		"developerMessage": "Application is invalid."
   	 *		}
   	 */
    @Path("{applicationtokenid}/get_application_name")
    @GET
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getApplicationNameFromApplicationTokenId(@PathParam("applicationtokenid") String applicationtokenid) throws AppException {
        log.debug("verify apptokenid {}", applicationtokenid);
        ApplicationToken myApp = AuthenticatedApplicationRepository.getApplicationToken(applicationtokenid);
        if (myApp != null || myApp.toString().length() > 10) {
            log.debug("Apptokenid valid");
            return Response.ok(myApp.getApplicationName()).header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
        } else {
            log.debug("Apptokenid not valid");
            //return Response.status(Response.Status.FORBIDDEN).header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
            throw AppExceptionCode.APP_ILLEGAL_7000;
        }
    }

    private boolean verifyApplicationCredentials(String appCredentials) {
        if (ApplicationMode.getApplicationMode().equals(ApplicationMode.DEV)) {
            log.trace("verifyApplicationCredentials - running in DEV mode, auto accepted.");
            return true;
        }

        try {
            ApplicationCredential applicationCredential = ApplicationCredentialMapper.fromXml(appCredentials);
            if (applicationCredential == null || applicationCredential.getApplicationID() == null || applicationCredential.getApplicationID().length() < 2) {
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
                log.debug("No application secret in property file for applicationId={} - applicationName: {} - Trying UAS/UIB", applicationCredential.getApplicationID(), applicationCredential.getApplicationName());
                if (!ApplicationAuthenticationUASClient.checkAppsecretFromUAS(applicationCredential)) {
                    log.warn("Application authentication failed. Incoming applicationSecret does not match applicationSecret in UIB");
                    return false;
                } else {
                    log.info("Application authentication OK for appId:{}, applicationName: {} from UAS", applicationCredential.getApplicationID(), applicationCredential.getApplicationName());
                    return true;
                }
            }
            if (!applicationCredential.getApplicationSecret().equalsIgnoreCase(expectedAppSecret)) {
                log.info("Incoming applicationSecret does not match applicationSecret from property file. - Trying UAS/UIB");
                if (!ApplicationAuthenticationUASClient.checkAppsecretFromUAS(applicationCredential)) {
                    log.warn("Application authentication failed. Incoming applicationSecret does not match applicationSecret in UIB");
                    return false;
                } else {
                    log.info("Application authentication OK for appId:{}, applicationName: {} from UAS", applicationCredential.getApplicationID(), applicationCredential.getApplicationName());
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Error in verifyApplicationCredentials.", e);
            return false;
        }
        return true;
    }

//    /**
//   	 * @throws AppException 
//   	 * @api {get} :applicationtokenid/hasUASAccess hasUASAccess
//   	 * @apiName hasUASAccess
//   	 * @apiGroup Security Token Service (STS)
//   	 * @apiDescription Check if an application has UAS access 
//   	 * 
//   	 * @apiSuccessExample Success-Response:
//   	 *	HTTP/1.1 200 OK
//	 *	{
//	 *  	"result": "true"
//	 *	}
//   	 *
//   	 *
//   	 * @apiError 403/7000 Application is invalid.
//   	 * @apiError 500/9999 A generic exception or an unexpected error 
//   	 *
//   	 * @apiErrorExample Error-Response:
//   	 *     HTTP/1.1 403 Forbidden
//   	 *     {
//   	 *  		"status": 403,
//   	 *  		"code": 7000,
//   	 *  		"message": "Illegal Application.",
//   	 *  		"link": "",
//   	 *  		"developerMessage": "Application is invalid."
//   	 *		}
//   	 */
//    @Path("{applicationtokenid}/hasUASAccess")
//    @GET
//    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//    public Response hasUASAccess(@PathParam("applicationtokenid") String applicationtokenid) throws AppException {
//        log.trace("validateApplicationTokenId - validate applicationtokenid:{}", applicationtokenid);
//        if (AuthenticatedApplicationRepository.verifyUASAccess(applicationtokenid)) {
//            log.debug("validateApplicationTokenId - applicationtokenid:{} for applicationname:{} is valid", applicationtokenid, AuthenticatedApplicationRepository.getApplicationToken(applicationtokenid).getApplicationName());
//            return Response.ok("{\"result\": \"true\"}").header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
//        } else {
//            log.warn("validateApplicationTokenId - applicationtokenid:{}  does not have UAS access", applicationtokenid);
//            throw AppExceptionCode.APP_ILLEGAL_7000;
//        }
//    }


}
