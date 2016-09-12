package net.whydah.token.user;

import com.google.inject.Inject;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.sun.jersey.api.view.Viewable;
import net.whydah.sso.commands.adminapi.user.CommandSendSMSToUser;
import net.whydah.sso.config.ApplicationMode;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserToken;
import net.whydah.sso.util.DelayedSendSMSTask;
import net.whydah.token.application.ApplicationThreatResource;
import net.whydah.token.application.AuthenticatedApplicationRepository;
import net.whydah.token.config.AppConfig;
import net.whydah.token.config.ApplicationModelHelper;
import net.whydah.token.config.SSLTool;
import net.whydah.token.user.statistics.UserSessionObservedActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.valuereporter.agent.MonitorReporter;
import org.valuereporter.agent.activity.ObservedActivity;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.FileNotFoundException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@Path("/user")
public class UserTokenResource {
    private final static Logger log = LoggerFactory.getLogger(UserTokenResource.class);
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    public static final String GET_POST_DELETE_PUT = "GET, POST, DELETE, PUT";
    public static final String USER_AUTHENTICATION_FAILED = "User authentication failed";
    public static final String APPLICATION_AUTHENTICATION_NOT_VALID = "Application authentication not valid.";
    public static final String ILLEGAL_APPLICATION_FOR_THIS_SERVICE = "Illegal application for this service";

    private static Map userticketmap = new HashMap();
    private static Map applicationtokenidmap = new HashMap();
    private static java.util.Random generator = new SecureRandom();

    private static final String SMS_GW_SERVICE_URL;
    private static final String SMS_GW_SERVICE_ACCOUNT;
    private static final String SMS_GW_USERNAME;
    private static final String SMS_GW_PASSWORD;
    private static final String SMS_GW_QUERY_PARAM;

    public static final String GRIDPREFIX = "gridprefix";

    static {

        AppConfig appConfig = new AppConfig();

        // Property-overwrite of SSL verification to support weak ssl certificates
        if ("disabled".equalsIgnoreCase(appConfig.getProperty("sslverification"))) {
            SSLTool.disableCertificateValidation();

        }
        String xmlFileName = System.getProperty("hazelcast.config");
        log.info("Loading hazelcast configuration from :" + xmlFileName);
        Config hazelcastConfig = new Config();
        if (xmlFileName != null && xmlFileName.length() > 10) {
            try {
                hazelcastConfig = new XmlConfigBuilder(xmlFileName).build();
                log.info("Loading hazelcast configuration from :" + xmlFileName);
            } catch (FileNotFoundException notFound) {
                log.error("Error - not able to load hazelcast.xml configuration.  Using embedded as fallback");
            }
        }
        hazelcastConfig.setProperty("hazelcast.logging.type", "slf4j");
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig);
        userticketmap = hazelcastInstance.getMap(appConfig.getProperty(GRIDPREFIX) + "userticket_map");
        log.info("Connectiong to map {}", appConfig.getProperty(GRIDPREFIX) + "userticket_map");
        applicationtokenidmap = hazelcastInstance.getMap(appConfig.getProperty(GRIDPREFIX) + "applicationtokenid_map");
        log.info("Connectiong to map {}", appConfig.getProperty(GRIDPREFIX) + "applicationtokenid_map");

        SMS_GW_SERVICE_URL = appConfig.getProperty("smsgw.serviceurl");  //URL https://smsgw.somewhere/../sendMessages/
        SMS_GW_SERVICE_ACCOUNT = appConfig.getProperty("smsgw.serviceaccount");  //serviceAccount
        SMS_GW_USERNAME = appConfig.getProperty("smsgw.username");  //smsserviceusername
        SMS_GW_PASSWORD = appConfig.getProperty("smsgw.password");  //msservicepassword
        SMS_GW_QUERY_PARAM = appConfig.getProperty("smsgw.queryparams");   //serviceId=serviceAccount&me...ssword=smsservicepassword
    }


    private UserAuthenticator userAuthenticator;

    @Context
    UriInfo uriInfo;


    @Inject
    public UserTokenResource(UserAuthenticator userAuthenticator) {
        this.userAuthenticator = userAuthenticator;
    }

    @Inject
    private AppConfig appConfig;

    @Path("/usertoken_template")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response getUserTokenTemplate() {
        return Response.ok(new Viewable("/usertoken.ftl", new UserToken())).build();
    }

    /**
     * TODO baardl: rename the param apptoken
     *
     * @param applicationtokenid application session
     * @param appTokenXml        application session data
     * @param userCredentialXml  user credentials i.e. (username / password)
     * @return userToken - user session data
     */
    @Path("/{applicationtokenid}/usertoken")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response getUserToken(@PathParam("applicationtokenid") String applicationtokenid,
                                 @FormParam("apptoken") String appTokenXml,
                                 @FormParam("usercredential") String userCredentialXml) {
        if (ApplicationMode.getApplicationMode().equals(ApplicationMode.DEV)) {
            return DevModeHelper.return_DEV_MODE_ExampleUserToken(1);
        }
        if (!UserTokenFactory.verifyApplicationToken(applicationtokenid, appTokenXml)) {
            log.warn("getUserToken - attempt to access from invalid application. applicationtokenid={}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity(APPLICATION_AUTHENTICATION_NOT_VALID).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        try {
            UserToken userToken = userAuthenticator.logonUser(applicationtokenid, appTokenXml, userCredentialXml);
            return createUserTokenResponse(applicationtokenid, userToken);

        } catch (AuthenticationFailedException ae) {
            log.warn("getUserToken - User authentication failed");
            return Response.status(Response.Status.FORBIDDEN).entity(USER_AUTHENTICATION_FAILED).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
    }

    /**
     * Login in user by his/her usercredentials and register its ticket in the ticket-map for session handover
     *
     * @param applicationtokenid application session
     * @param userticket         user session handover ticket
     * @param appTokenXml        application session data
     * @param userCredentialXml  user credentials i.e. (username / password)
     * @return user session data
     */
    @Path("/{applicationtokenid}/{userticket}/usertoken")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response getUserTokenAndStoreUserTicket(@PathParam("applicationtokenid") String applicationtokenid,
                                                   @PathParam("userticket") String userticket,
                                                   @FormParam("apptoken") String appTokenXml,
                                                   @FormParam("usercredential") String userCredentialXml) {
        if (ApplicationMode.getApplicationMode().equals(ApplicationMode.DEV)) {
            return DevModeHelper.return_DEV_MODE_ExampleUserToken(1);
        }

        if (!UserTokenFactory.verifyApplicationToken(applicationtokenid, appTokenXml)) {
            log.warn("getUserTokenAndStoreUserTicket - attempt to access from invalid application. applicationtokenid={}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        try {
            UserToken userToken = userAuthenticator.logonUser(applicationtokenid, appTokenXml, userCredentialXml);

            // Add the user to the ticket-map with the ticket given from the caller
            userticketmap.put(userticket, userToken.getTokenid());
            return createUserTokenResponse(applicationtokenid, userToken);
        } catch (AuthenticationFailedException ae) {
            log.warn("getUserTokenAndStoreUserTicket - User authentication failed");
            return Response.status(Response.Status.FORBIDDEN).entity(USER_AUTHENTICATION_FAILED).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
    }


    /**
     * Verify that a usertoken and a user session is still valid. Usually used for application re-entries and before allowing
     * a user important and critical processes like monetary transactions
     *
     * @param applicationtokenid - application session id
     * @param userTokenXml       - user session data
     * @return - OK if valid user session exists based upon user session data
     */
    @Path("/{applicationtokenid}/validate_usertoken")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response validateUserTokenXML(@PathParam("applicationtokenid") String applicationtokenid, @FormParam("usertoken") String userTokenXml) {
        if (!AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            log.warn("validateUserTokenXML - attempt to access from invalid application. applicationtokenid={}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        UserToken userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
        if (ActiveUserTokenRepository.verifyUserToken(userToken, applicationtokenid)) {
            return Response.ok().header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        log.warn("validateUserTokenXML failed for usertoken {}", userTokenXml);
        return Response.status(Response.Status.UNAUTHORIZED).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
    }

    @Path("/{applicationtokenid}/validate_usertokenid/{usertokenid}")
    @GET
    public Response validateUserTokenID(@PathParam("applicationtokenid") String applicationtokenid, @PathParam("usertokenid") String usertokenid) {
        if (!AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            log.warn("validateUserTokenXML - attempt to access from invalid application. applicationtokenid={}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        if (ActiveUserTokenRepository.getUserToken(usertokenid, applicationtokenid) != null) {
            log.trace("Verified {}", usertokenid);
            return Response.ok().header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        log.warn("Usertoken not ok: {}", usertokenid);
        return Response.status(Response.Status.UNAUTHORIZED).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
    }

    /**
     * Used to create a userticket for a user to transfer a session between whydah SSO apps
     *
     * @param applicationtokenid application session
     * @param appTokenXml        application session data
     * @param userticket         user session handover ticket
     * @param userTokenId        user session id
     * @return usertoken - user session h
     */
    @Path("/{applicationtokenid}/create_userticket_by_usertokenid")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createUserTicketByUserTokenId(@PathParam("applicationtokenid") String applicationtokenid,
                                                  @FormParam("apptoken") String appTokenXml,
                                                  @FormParam("userticket") String userticket,
                                                  @FormParam("usertokenid") String userTokenId) {
        log.trace("createUserTicketByUserTokenId: applicationtokenid={}, userticket={}, usertokenid={}, appTokenXml={}", applicationtokenid, userticket, userTokenId, appTokenXml);


        if (!UserTokenFactory.verifyApplicationToken(applicationtokenid, appTokenXml)) {
            log.warn("createUserTicketByUserTokenId - attempt to access from invalid application. applicationtokenid={}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity(ILLEGAL_APPLICATION_FOR_THIS_SERVICE).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        final UserToken userToken = ActiveUserTokenRepository.getUserToken(userTokenId, applicationtokenid);
        if (userToken != null) {
            userticketmap.put(userticket, userToken.getTokenid());
            log.trace("createUserTicketByUserTokenId OK. Response={}", userToken.toString());
            return createUserTokenResponse(applicationtokenid, userToken);
        }
        log.warn("createUserTicketByUserTokenId - attempt to access with non acceptable usertokenid {}", userTokenId);
        return Response.status(Response.Status.NOT_ACCEPTABLE).build();
    }


    /**
     * Used to get the usertoken from a usertokenid, which the application usually stores in its secure cookie
     *
     * @param applicationtokenid application session
     * @param appTokenXml        application session data
     * @param userTokenId        user session id
     * @return usertoken
     */
    @Path("/{applicationtokenid}/get_usertoken_by_usertokenid")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response getUserTokenByUserTokenId(@PathParam("applicationtokenid") String applicationtokenid,
                                              @FormParam("apptoken") String appTokenXml,
                                              @FormParam("usertokenid") String userTokenId) {
        log.trace("getUserTokenByUserTokenId: applicationtokenid={}, usertokenid={}, appTokenXml={}", applicationtokenid, userTokenId, appTokenXml);

        if (ApplicationMode.getApplicationMode().equals(ApplicationMode.DEV)) {
            return DevModeHelper.return_DEV_MODE_ExampleUserToken(1);
        }

        if (!UserTokenFactory.verifyApplicationToken(applicationtokenid, appTokenXml)) {
            log.warn("getUserTokenByUserTokenId - attempt to access from invalid application. applicationtokenid={}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity(ILLEGAL_APPLICATION_FOR_THIS_SERVICE).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        final UserToken userToken = ActiveUserTokenRepository.getUserToken(userTokenId, applicationtokenid);
        if (userToken == null) {
            log.warn("getUserTokenByUserTokenId - attempt to access with non acceptable usertokenid={}", userTokenId);
            return Response.status(Response.Status.NOT_ACCEPTABLE).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        log.info("getUserTokenByUserTokenId - valid session found for {} ", userTokenId);
        return createUserTokenResponse(applicationtokenid, userToken);
    }

    /**
     * Used to get the lase seend time, which the application usually stores in its secure cookie
     *
     * @param applicationtokenid application session
     * @param userEmail          email of user we try to locate
     * @return last seen as String
     */
    @Path("/{applicationtokenid}/{email}/last_seen")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getLastSeenByUserTokenId(@PathParam("applicationtokenid") String applicationtokenid,
                                             @PathParam("email") String userEmail) {
        log.trace("getLastSeenByUserTokenId: applicationtokenid={}, email={}", applicationtokenid, userEmail);

        if (ApplicationMode.getApplicationMode().equals(ApplicationMode.DEV)) {
            return DevModeHelper.return_DEV_MODE_ExampleUserToken(1);
        }

        if (!UserTokenFactory.verifyApplicationToken(applicationtokenid, "")) {
            log.warn("getLastSeenByUserTokenId - attempt to access from invalid application. applicationtokenid={}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity(ILLEGAL_APPLICATION_FOR_THIS_SERVICE).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        String lastSeen = ActiveUserTokenRepository.getLastSeenByEmail(userEmail);
        if (lastSeen == null) {
            log.warn("getLastSeenByUserTokenId - attempt to access with non acceptable userEmail={}", userEmail);
            return Response.status(Response.Status.NOT_ACCEPTABLE).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        log.info("getLastSeenByUserTokenId - valid session found for {} ", userEmail);
        return Response.status(Response.Status.OK).entity(lastSeen).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
    }


    /**
     * Lookup a user by a one-time userticket, usually the first thing we do after receiving a SSO redirect back to
     * an application from SSOLoginWebApplication
     *
     * @param applicationtokenid application session
     * @param appTokenXml        application session data
     * @param userticket         user session handover ticket
     * @return usertoken
     */
    @Path("/{applicationtokenid}/get_usertoken_by_userticket")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response getUserTokenByUserTicket(@PathParam("applicationtokenid") String applicationtokenid,
                                             @FormParam("apptoken") String appTokenXml,
                                             @FormParam("userticket") String userticket) {
        if (isEmpty(appTokenXml) || isEmpty(userticket)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing required parameters").build();
        }

        log.trace("getUserTokenByUserTicket: applicationtokenid={}, userticket={}, appTokenXml={}", applicationtokenid, userticket, appTokenXml);

        if (ApplicationMode.getApplicationMode().equals(ApplicationMode.DEV)) {
            return DevModeHelper.return_DEV_MODE_ExampleUserToken(1);
        }

        if (!UserTokenFactory.verifyApplicationToken(applicationtokenid, appTokenXml)) {
            log.warn("getUserTokenByUserTicket - attempt to access from invalid application. applicationtokenid={}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity(ILLEGAL_APPLICATION_FOR_THIS_SERVICE).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        String userTokenId = (String) userticketmap.get(userticket);
        if (userTokenId == null) {
            log.warn("getUserTokenByUserTicket - Attempt to resolve non-existing userticket={}", userticket);
            return Response.status(Response.Status.GONE).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build(); //410
        }
        log.trace("getUserTokenByUserTicket - Found usertokenid: " + userTokenId);
        userticketmap.remove(userticket);
        final UserToken userToken = ActiveUserTokenRepository.getUserToken(userTokenId, applicationtokenid);

        if (userToken == null) {
            log.warn("getUserTokenByUserTicket - illegal/Null userticket received ");
            return Response.status(Response.Status.NOT_ACCEPTABLE).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build(); //406
        }
        log.trace("getUserTokenByUserTicket OK. Response={}", userToken.toString());
        return createUserTokenResponse(applicationtokenid, userToken);
    }

    /**
     * Lookup a user by a one-time pin-code distributed to the users registered cellPhone number, usually the first thing we do after receiving a SSO redirect back to
     * an application from SSOLoginWebApplication
     *
     * @param applicationtokenid application session
     * @param userticket         one time ticket for the roundtrip of the operation
     * @param appTokenXml        application session data
     * @param phoneno            user phonenumber
     * @param pin                user pin
     * @return usertoken
     */
    @Path("/{applicationtokenid}/{userticket}/get_usertoken_by_pin_and_logon_user")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response getUserTokenByPinAndLogonUser(@PathParam("applicationtokenid") String applicationtokenid,
                                                  @PathParam("userticket") String userticket,
                                                  @FormParam("adminUserTokenId") String adminUserTokenId,
                                                  @FormParam("apptoken") String appTokenXml,
                                                  @FormParam("phoneno") String phoneno,
                                                  @FormParam("pin") String pin) {

        log.trace("getUserTokenByDistributedPinAndLogonUser() called with " + "applicationtokenid = [" + applicationtokenid + "], userticket = [" + userticket + "], appTokenXml = [" + appTokenXml + "], phoneno = [" + phoneno + "], pin = [" + pin + "]");

        if (isEmpty(appTokenXml) || isEmpty(pin) || isEmpty(phoneno)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing required parameters").build();
        }

        log.trace("getUserTokenByDistributedPinAndLogonUser: applicationtokenid={}, pin={}, appTokenXml={}", applicationtokenid, pin, appTokenXml);

        if (ApplicationMode.getApplicationMode().equals(ApplicationMode.DEV)) {
            return DevModeHelper.return_DEV_MODE_ExampleUserToken(1);
        }

        // Verify calling application
        if (!UserTokenFactory.verifyApplicationToken(applicationtokenid, appTokenXml)) {
            log.warn("getUserTokenByUserTicket - attempt to access from invalid application. applicationtokenid={}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity(ILLEGAL_APPLICATION_FOR_THIS_SERVICE).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }

        final UserToken userToken = userAuthenticator.logonPinUser(applicationtokenid, appTokenXml, adminUserTokenId, phoneno, pin);
        if (userToken == null) {
            log.warn("getUserTokenByDistributedPinAndLogonUser - attempt to access with non acceptable username, phoneno={}", phoneno);
            return Response.status(Response.Status.NOT_ACCEPTABLE).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        log.info("getUserTokenByDistributedPinAndLogonUser - valid session created for {} ", phoneno);
        ApplicationModelHelper.updateApplicationList(applicationtokenid, adminUserTokenId);
        return createUserTokenResponse(applicationtokenid, userToken);

    }


    /**
     * Force cross-applications/SSO session logout. Use with extreme care as the user's hate the resulting user experience..
     *
     * @param applicationtokenid application session
     * @param usertokenid        user session id
     * @return Response.OK
     */
    @Path("/{applicationtokenid}/release_usertoken")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response releaseUserToken(@PathParam("applicationtokenid") String applicationtokenid,
                                     @FormParam("usertokenid") String usertokenid) {
        log.trace("releaseUserToken - entry.  usertokenid={}", usertokenid);
        if (!AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            log.warn("releaseUserToken - attempt to access from invalid application. applicationtokenid={}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        if (usertokenid == null) {
            log.warn("releaseUserToken - attempt with no usertokenid: Null");
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing usertokenid.").header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        log.trace("releaseUserToken - removed session, usertokenid={}", usertokenid);
        ActiveUserTokenRepository.removeUserToken(usertokenid, applicationtokenid);
        return Response.ok().header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
    }

    /**
     * Request SSO user session renewal.
     *
     * @param applicationtokenid application session
     * @param usertokenid        user session id
     * @return userTokenXml - usertoken with extended lease
     */
    @Path("/{applicationtokenid}/renew_usertoken")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response renewUserToken(@PathParam("applicationtokenid") String applicationtokenid,
                                   @FormParam("usertokenid") String usertokenid) {
        log.trace("renewUserToken - entry.  usertokenid={}", usertokenid);
        if (!AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            log.warn("renewUserToken - attempt to access from invalid application. applicationtokenid={}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        if (usertokenid == null) {
            log.warn("renewUserToken - attempt with no usertokenid: Null");
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing usertokenid.").header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }

        ActiveUserTokenRepository.renewUserToken(usertokenid, applicationtokenid);

        log.trace("renewUserToken - session renewed, usertokenid={}", usertokenid);
        return Response.ok().header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
    }

    /**
     * Request SSO user session renewal.
     *
     * @param applicationtokenid application session
     * @param usertokenid        user session id
     * @return
     */
    @Path("/{applicationtokenid}/refresh_usertoken")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response refreshUserToken(@PathParam("applicationtokenid") String applicationtokenid,
                                     @FormParam("usertokenid") String usertokenid) {
        log.debug("refresh_usertoken - entry.  usertokenid={}", usertokenid);
        if (!AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            log.warn("refresh_usertoken - attempt to access from invalid application. applicationtokenid={}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        if (usertokenid == null) {
            log.warn("refresh_usertoken - attempt with no usertokenid: Null");
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing usertokenid.").header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        UserToken refreshedUserToken = userAuthenticator.getRefreshedUserToken(usertokenid);
        ActiveUserTokenRepository.refreshUserToken(usertokenid, applicationtokenid, refreshedUserToken);
        final UserToken userToken = ActiveUserTokenRepository.getUserToken(usertokenid, applicationtokenid);

        log.debug("refresh_usertoken - usertoken refreshed, usertokenid={}", usertokenid);
        return createUserTokenResponse(applicationtokenid, userToken);
    }


    /**
     * This method is for elevating user access to a higher level for the receiving end of a session handover between SSO applications
     *
     * @param applicationtokenid calling application session
     * @param appTokenXml        application session data
     * @param userTokenXml       user session data
     * @param newAppTokenId      application session id of receiving application session
     * @return UserTokenXml as seen for the given application session
     */
    @Path("/{applicationtokenid}/transform_usertoken")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response transformUserToken(@PathParam("applicationtokenid") String applicationtokenid,
                                       @FormParam("apptoken") String appTokenXml,
                                       @FormParam("usertoken") String userTokenXml,
                                       @FormParam("to_apptokenid") String newAppTokenId) {
        if (ApplicationMode.getApplicationMode().equals(ApplicationMode.DEV)) {
            return DevModeHelper.return_DEV_MODE_ExampleUserToken(1);
        }

        if (ApplicationMode.getApplicationMode().equals(ApplicationMode.DEV)) {
            return DevModeHelper.return_DEV_MODE_ExampleUserToken(1);
        }

        if (!UserTokenFactory.verifyApplicationToken(applicationtokenid, appTokenXml)) {
            log.warn("getUserTokenByUserTokenId - attempt to access from invalid application. applicationtokenid={}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity(ILLEGAL_APPLICATION_FOR_THIS_SERVICE).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        String userTokenId = UserTokenMapper.fromUserTokenXml(userTokenXml).getTokenid();
        final UserToken userToken = ActiveUserTokenRepository.getUserToken(userTokenId, applicationtokenid);
        userToken.setDefcon(ApplicationThreatResource.getDEFCON());
        if (userToken == null) {
            log.warn("getUserTokenByUserTokenId - attempt to access with non acceptable userTokenId={}", userTokenId);
            return Response.status(Response.Status.NOT_ACCEPTABLE).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        return createUserTokenResponse(newAppTokenId, userToken);
    }

    /**
     * The backend for PIN signup processes
     *
     * @param applicationtokenid   the ID of the application session
     * @param phoneNo              the callPhone to get the message
     * @param smsPin               the pin-code massage to send to the user
     * @return
     */
    @Path("/{applicationtokenid}/send_sms_pin")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response sendSMSPin(@PathParam("applicationtokenid") String applicationtokenid,
                               @FormParam("phoneNo") String phoneNo,
                               @FormParam("smsPin") String smsPin) {
        log.info("sendSMSPin: phoneNo:" + phoneNo + ", smsPin:" + smsPin);

        if (phoneNo == null || smsPin == null) {
            log.warn("sendSMSPin: attempt to use service with emty parameters");
            return Response.status(Response.Status.NOT_ACCEPTABLE).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }

        if (!AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            log.warn("sendSMSPin - attempt to access from invalid application. applicationtokenid={}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity(ILLEGAL_APPLICATION_FOR_THIS_SERVICE).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }

        log.trace("CommandSendSMSToUser - ({}, {}, {}, {}, {}, {}, {})", SMS_GW_SERVICE_URL, SMS_GW_SERVICE_ACCOUNT, SMS_GW_USERNAME, SMS_GW_PASSWORD, SMS_GW_QUERY_PARAM, phoneNo, smsPin);
        String response = new CommandSendSMSToUser(SMS_GW_SERVICE_URL, SMS_GW_SERVICE_ACCOUNT, SMS_GW_USERNAME, SMS_GW_PASSWORD, SMS_GW_QUERY_PARAM, phoneNo, smsPin).execute();
        log.debug("Answer from smsgw: " + response);
        ActivePinRepository.setPin(phoneNo, smsPin);
        return Response.ok().header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();

    }

    /**
     * The backend for PIN signup processes
     *
     * @param applicationtokenid the ID of the application session
     * @param phoneNo            the callPhone to get the message
     * @return
     */
    @Path("/{applicationtokenid}/generate_pin_and_send_sms_pin")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response sendgenerateAndSendSMSPin(@PathParam("applicationtokenid") String applicationtokenid,
                                              @FormParam("phoneNo") String phoneNo) {
        log.info("sendgenerateAndSendSMSPin: phoneNo:" + phoneNo);

        String smsPin = generatePin();
        if (phoneNo == null || smsPin == null) {
            log.warn("sendSMSPin: attempt to use service with emty parameters");
            return Response.status(Response.Status.NOT_ACCEPTABLE).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }

        if (!AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            log.warn("sendSMSPin - attempt to access from invalid application. applicationtokenid={}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity(ILLEGAL_APPLICATION_FOR_THIS_SERVICE).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }

        log.trace("CommandSendSMSToUser - ({}, {}, {}, {}, {}, {}, {})", SMS_GW_SERVICE_URL, SMS_GW_SERVICE_ACCOUNT, SMS_GW_USERNAME, SMS_GW_PASSWORD, SMS_GW_QUERY_PARAM, phoneNo, smsPin);
        String response = new CommandSendSMSToUser(SMS_GW_SERVICE_URL, SMS_GW_SERVICE_ACCOUNT, SMS_GW_USERNAME, SMS_GW_PASSWORD, SMS_GW_QUERY_PARAM, phoneNo, smsPin).execute();
        log.trace("Answer from smsgw: " + response);
        ActivePinRepository.setPin(phoneNo, smsPin);
        return Response.ok().header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();

    }

    /**
     * @param applicationtokenid application session
     * @param appTokenXml        application session data
     * @param phoneno            user phonenumber
     * @param pin                user entered pin
     * @return usertoken
     */
    @Path("/{applicationtokenid}/verify_phone_by_pin")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response verifyPhoneByPin(@PathParam("applicationtokenid") String applicationtokenid,
                                     @FormParam("appTokenXml") String appTokenXml,
                                     @FormParam("phoneno") String phoneno,
                                     @FormParam("pin") String pin) {

        log.trace("verifyPhoneByPin() called with " + "applicationtokenid = [" + applicationtokenid + "], appTokenXml = [" + appTokenXml + "], phoneno = [" + phoneno + "], pin = [" + pin + "]");

        if (isEmpty(appTokenXml) || isEmpty(pin) || isEmpty(phoneno)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing required parameters").build();
        }

        log.trace("verifyPhoneByPin: applicationtokenid={}, pin={}, appTokenXml={}", applicationtokenid, pin, appTokenXml);

        if (ApplicationMode.getApplicationMode().equals(ApplicationMode.DEV)) {
            return DevModeHelper.return_DEV_MODE_ExampleUserToken(1);
        }

        // Verify calling application
        if (!UserTokenFactory.verifyApplicationToken(applicationtokenid, appTokenXml)) {
            log.warn("verifyPhoneByPin - attempt to access from invalid application. applicationtokenid={}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity(ILLEGAL_APPLICATION_FOR_THIS_SERVICE).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }

        if (ActivePinRepository.usePin(phoneno, pin)) {
            return Response.status(Response.Status.OK).build();
        } else {
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
    }

    /**
     * The backend for sms messages to user
     *
     * @param applicationtokenid    The application session ID
     * @param phoneNo               The cellPhone to send message to
     * @param smsMessage                 The message to send to the suer
     * @return
     */
    @Path("/{applicationtokenid}/send_sms")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response sendSMSMessage(@PathParam("applicationtokenid") String applicationtokenid,
                                   @FormParam("phoneNo") String phoneNo,
                                   @FormParam("smsMessage") String smsMessage) {
        log.info("Response sendSMSMessage: phoneNo:{}, smsMessage:{}", phoneNo, smsMessage);

        if (!AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            log.warn("sendSMSMessage - attempt to access from invalid application. applicationtokenid={}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity(ILLEGAL_APPLICATION_FOR_THIS_SERVICE).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        String cellNo = phoneNo;
        new CommandSendSMSToUser(SMS_GW_SERVICE_URL, SMS_GW_SERVICE_ACCOUNT, SMS_GW_USERNAME, SMS_GW_PASSWORD, SMS_GW_QUERY_PARAM, cellNo, smsMessage).execute();
        return Response.ok().header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();

    }

    /**
     * The backend for sms messages to user
     *
     * @param applicationtokenid    The application session ID
     * @param timestamp               The timestamp of when to send the message
     * @param phoneNo               The cellPhone to send message to
     * @param smsMessage                 The message to send to the suer
     * @return
     */
    @Path("/{applicationtokenid}/send_scheduled_sms")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response sendScheduledSMSMessage(@PathParam("applicationtokenid") String applicationtokenid,
                                            @FormParam("timestamp") String timestamp,
                                            @FormParam("phoneNo") String phoneNo,
                                            @FormParam("smsMessage") String smsMessage) {
        log.info("Response sendScheduledSMSMessage: timestamp:{}, phoneNo:{}, smsMessage:{}", timestamp, phoneNo, smsMessage);

        if (!AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            log.warn("sendScheduledSMSMessage - attempt to access from invalid application. applicationtokenid={}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity(ILLEGAL_APPLICATION_FOR_THIS_SERVICE).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        String cellNo = phoneNo;
        new DelayedSendSMSTask(Long.parseLong(timestamp), SMS_GW_SERVICE_URL, SMS_GW_SERVICE_ACCOUNT, SMS_GW_USERNAME, SMS_GW_PASSWORD, SMS_GW_QUERY_PARAM, cellNo, smsMessage);
        return Response.ok().header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();

    }

    /**
     * The SSOLoginWebApplication backend for 3rd party UserTokens. Receive a new user, create a Whydah UserIdentity with
     * the corresponding defaultroles (UAS|UIB) and create a new session with a one-time userticket for handover to receiving
     * SSO applications
     *
     * @param applicationtokenid     calling application session
     * @param userticket             user session id
     * @param appTokenXml            application session data
     * @param userCredentialXml      user credential  i.e. (username and password)
     * @param thirdPartyUserTokenXml typically facebook user-token or other oauth2 usertoken
     * @return user session data
     */
    @Path("/{applicationtokenid}/{userticket}/create_user")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response createAndLogOnUser(@PathParam("applicationtokenid") String applicationtokenid,
                                       @PathParam("userticket") String userticket,
                                       @FormParam("apptoken") String appTokenXml,
                                       @FormParam("usercredential") String userCredentialXml,
                                       @FormParam("fbuser") String thirdPartyUserTokenXml) {
        log.trace("Response createAndLogOnUser: usercredential:" + userCredentialXml + "fbuser:" + thirdPartyUserTokenXml);

        if (ApplicationMode.getApplicationMode() == ApplicationMode.DEV) {
            return DevModeHelper.return_DEV_MODE_ExampleUserToken(1);
        }

        if (!UserTokenFactory.verifyApplicationToken(applicationtokenid, appTokenXml)) {
            // TODO:  Limit this operation to SSOLoginWebApplication ONLY
            log.warn("createAndLogOnUser - attempt to access from invalid application. applicationtokenid={}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }

        try {
            applicationtokenidmap.put(applicationtokenid, applicationtokenid);
            UserToken userToken = userAuthenticator.createAndLogonUser(applicationtokenid, appTokenXml, userCredentialXml, thirdPartyUserTokenXml);
            userticketmap.put(userticket, userToken.getTokenid());
            userToken.setDefcon(ApplicationThreatResource.getDEFCON());
            userToken.setNs2link(appConfig.getProperty("myuri") + "user/" + applicationtokenid + "/validate_usertokenid/" + userToken.getTokenid());
            // Report to statistics
            ObservedActivity observedActivity = new UserSessionObservedActivity(userToken.getUserName(), "userCreated", applicationtokenid);
            MonitorReporter.reportActivity(observedActivity);
            return createUserTokenResponse(applicationtokenid, userToken);
        } catch (AuthenticationFailedException ae) {
            log.warn("createAndLogOnUser - Error creating or authenticating user. thirdPartyUserTokenXml={}", thirdPartyUserTokenXml);
            return Response.status(Response.Status.FORBIDDEN).entity("Error creating or authenticating user.").header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
    }


    /**
     * The SSOLoginWebApplication backend for 3rd party UserTokens. Receive a new user, create a Whydah UserIdentity with
     * the corresponding defaultroles (UAS|UIB) and create a new session with a one-time userticket for handover to receiving
     * SSO applications
     *
     * @param applicationtokenid calling application session
     * @param pin                user session pin
     * @param appTokenXml        application session data
     * @param newUserjson        a simple userjson for new user
     * @return user session data
     */
    @Path("/{applicationtokenid}/{userticket}/{pin}/create_pinverified_user")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response createAndLogOnPinUser(@PathParam("applicationtokenid") String applicationtokenid,
                                          @PathParam("userticket") String userticket,
                                          @PathParam("pin") String pin,
                                          @FormParam("apptoken") String appTokenXml,
                                          @FormParam("adminUserTokenId") String adminUserTokenId,
                                          @FormParam("cellPhone") String cellPhone,
                                          @FormParam("jsonuser") String newUserjson) {
        log.info("Request createAndLogOnPinUser:  jsonuser:" + newUserjson);

        if (ApplicationMode.getApplicationMode() == ApplicationMode.DEV) {
            return DevModeHelper.return_DEV_MODE_ExampleUserToken(1);
        }

        if (pin == null || pin.length() < 4) {
            pin = generatePin();
            log.info("createAndLogOnPinUser - empty pin in request, gererating internal pin and use it");
        }
        if (!UserTokenFactory.verifyApplicationToken(applicationtokenid, appTokenXml)) {
            // TODO:  Limit this operation to SSOLoginWebApplication ONLY
            log.warn("createAndLogOnPinUser - attempt to access from invalid application. applicationtokenid={}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
        try {
            UserToken userToken = userAuthenticator.createAndLogonPinUser(applicationtokenid, appTokenXml, adminUserTokenId, cellPhone, pin, newUserjson);
            userticketmap.put(userticket, userToken.getTokenid());
            log.debug("createAndLogOnPinUser Added ticket:{} for usertoken:{} username: {}", userticket, userToken.getTokenid(), userToken.getUserName());
            userToken.setDefcon(ApplicationThreatResource.getDEFCON());
            userToken.setNs2link(appConfig.getProperty("myuri") + "user/" + applicationtokenid + "/validate_usertokenid/" + userToken.getTokenid());
            // Report to statistics
            ObservedActivity observedActivity = new UserSessionObservedActivity(userToken.getUserName(), "userCreated", applicationtokenid);
            MonitorReporter.reportActivity(observedActivity);
            return createUserTokenResponse(applicationtokenid, userToken);
        } catch (AuthenticationFailedException ae) {
            log.warn("createAndLogOnPinUser - Error creating or authenticating user. jsonuser={}", newUserjson);
            return Response.status(Response.Status.FORBIDDEN).entity("Error creating or authenticating user.").header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        } catch (Exception e) {
            log.error("Unhandled exception: ", e);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Error creating or authenticating user.").header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
        }
    }

    private Response createUserTokenResponse(@PathParam("applicationtokenid") String applicationtokenid, UserToken userToken) {
        log.trace("getUserTokenByUserTokenId OK. Response={}", userToken.toString());
        userToken.setNs2link(appConfig.getProperty("myuri") + "user/" + applicationtokenid + "/validate_usertokenid/" + userToken.getTokenid());
        userToken.setLastSeen(ActiveUserTokenRepository.getLastSeen(userToken));
        userToken.setDefcon(ApplicationThreatResource.getDEFCON());
        UserTokenFactory.getFilteredUserToken(applicationtokenid, userToken);
        ActiveUserTokenRepository.setLastSeen(userToken);
        return Response.ok(new Viewable("/usertoken.ftl", UserTokenFactory.getFilteredUserToken(applicationtokenid, userToken))).header(ACCESS_CONTROL_ALLOW_ORIGIN, "*").header(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_DELETE_PUT).build();
    }


    boolean isEmpty(String userticket) {
        boolean isEmpty = false;
        if (userticket == null || userticket.isEmpty()) {
            isEmpty = true;
        }
        return isEmpty;
    }

    public static String generatePin() {
        int i = generator.nextInt(10000) % 10000;
        java.text.DecimalFormat f = new java.text.DecimalFormat("0000");
        return f.format(i);

    }

}
