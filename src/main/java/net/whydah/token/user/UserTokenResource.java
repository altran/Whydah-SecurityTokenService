package net.whydah.token.user;

import com.google.inject.Inject;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.sun.jersey.api.view.Viewable;
import net.whydah.token.application.AuthenticatedApplicationRepository;
import net.whydah.token.config.AppConfig;
import net.whydah.token.config.ApplicationMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

@Path("/user")
public class UserTokenResource {
    private final static Logger logger = LoggerFactory.getLogger(UserTokenResource.class);

    private static Map userticketmap = new HashMap();
    private static Map  applicationtokenidmap = new HashMap();

    static {
        AppConfig appConfig = new AppConfig();
        String xmlFileName = System.getProperty("hazelcast.config");
        logger.info("Loading hazelcast configuration from :" + xmlFileName);
        Config hazelcastConfig = new Config();
        if (xmlFileName != null && xmlFileName.length() > 10) {
            try {
                hazelcastConfig = new XmlConfigBuilder(xmlFileName).build();
                logger.info("Loading hazelcast configuration from :" + xmlFileName);
            } catch (FileNotFoundException notFound) {
                logger.error("Error - not able to load hazelcast.xml configuration.  Using embedded as fallback");
            }
        }
        hazelcastConfig.setProperty("hazelcast.logging.type", "slf4j");
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig);
        userticketmap = hazelcastInstance.getMap(appConfig.getProperty("gridprefix")+"userticketmap");
        applicationtokenidmap = hazelcastInstance.getMap(appConfig.getProperty("gridprefix")+"applicationtokenidmap");
        logger.info("Connectiong to map {}",appConfig.getProperty("gridprefix")+"userticketmap");
        logger.info("Connectiong to map {}",appConfig.getProperty("gridprefix")+"applicationtokenidmap");


    }

    public static void initializeDistributedMap() {
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
     *
     *
     *
     * TODO baardl: rename the param apptoken
     * @param applicationtokenid the current application wanting to authenticate the user.
     * @param appTokenXml the token representing the application the user want to access.
     * @param userCredentialXml
     * @return
     */
    @Path("/{applicationtokenid}/usertoken")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response getUserToken(@PathParam("applicationtokenid") String applicationtokenid,
                                 @FormParam("apptoken") String appTokenXml,
                                 @FormParam("usercredential") String userCredentialXml) {
        if (!verifyApplicationToken(applicationtokenid, appTokenXml)) {
            logger.warn("getUserToken - attempt to access from invalid application. ID: {}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").build();
        }
        try {
            UserToken userToken = userAuthenticator.logonUser(applicationtokenid, appTokenXml, userCredentialXml);
            userToken.setNs2link(appConfig.getProperty("myuri")+"user/"+applicationtokenid+"/validate_usertokenid/"+userToken.getTokenid());
            return Response.ok(new Viewable("/usertoken.ftl", userToken)).build();
        } catch (AuthenticationFailedException ae) {
            logger.warn("getUserToken - User authentication failed");
                return Response.status(Response.Status.FORBIDDEN).entity("User authentication failed").build();
        }
    }

    /**
     * Login in user by his/her usercredentials and register its ticket in the ticket-map for session handover
     *
     * @param applicationtokenid
     * @param userticket
     * @param appTokenXml
     * @param userCredentialXml
     * @return
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

        if (!verifyApplicationToken(applicationtokenid, appTokenXml)) {
            logger.warn("getUserTokenAndStoreUserTicket - attempt to access from invalid application. ID: {}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").build();
        }
        try {
            UserToken usertoken = userAuthenticator.logonUser(applicationtokenid, appTokenXml, userCredentialXml);

            // Add the user to the ticket-map with the ticket given from the caller
            userticketmap.put(userticket, usertoken.getTokenid());
            usertoken.setNs2link(appConfig.getProperty("myuri")+"user/"+applicationtokenid+"/validate_usertokenid/"+usertoken.getTokenid());
            return Response.ok(new Viewable("/usertoken.ftl", usertoken)).build();
        } catch (AuthenticationFailedException ae) {
            logger.warn("getUserTokenAndStoreUserTicket - User authentication failed");
                return Response.status(Response.Status.FORBIDDEN).entity("User authentication failed").build();
        }
    }



    /**
     * Verify that a usertoken and a user session is still valid. Usually used for application re-entries and before allowing
     * a user important and critical processes like monetary transactions
     *
     *
     * @param applicationtokenid
     * @param userTokenXml
     * @return
     */
    @Path("/{applicationtokenid}/validate_usertoken")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response validateUserTokenXML(@PathParam("applicationtokenid") String applicationtokenid, @FormParam("usertoken") String userTokenXml) {
        if(!AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            logger.warn("validateUserTokenXML - attempt to access from invalid application. ID: {}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").build();
        }
        //UserToken userToken = UserToken.createFromUserTokenXML(userTokenXml);
        UserToken userToken = UserTokenFactory.fromXml(userTokenXml);
        if (ActiveUserTokenRepository.verifyUserToken(userToken)) {
            return Response.ok().build();
        }
        logger.warn("validateUserTokenXML failed for usertoken {}", userTokenXml);
        return Response.status(Response.Status.CONFLICT).build();
    }

    @Path("/{applicationtokenid}/validate_usertokenid/{usertokenid}")
    @GET
    public Response validateUserTokenID(@PathParam("applicationtokenid") String applicationtokenid, @PathParam("usertokenid") String usertokenid) {
        if(!AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            logger.warn("validateUserTokenXML - attempt to access from invalid application. ID: {}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").build();
        }
        if (ActiveUserTokenRepository.getUserToken(usertokenid) != null) {
            logger.trace("Verified {}", usertokenid);
            return Response.ok().build();
        }
        logger.warn("Usertoken not ok: {}", usertokenid);
        return Response.status(Response.Status.CONFLICT).build();
    }

    /**
     * Used to create a userticket for a user to transfer a session between whydah SSO apps
     *
     * @param applicationtokenid
     * @param appTokenXml
     * @param userticket
     * @param userTokenId
     * @return
     */
    @Path("/{applicationtokenid}/create_userticket_by_usertokenid")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createUserTicketByUserTokenId(@PathParam("applicationtokenid") String applicationtokenid,
                                                  @FormParam("apptoken") String appTokenXml,
                                                  @FormParam("userticket") String userticket,
                                                  @FormParam("usertokenid") String userTokenId) {
        logger.trace("createUserTicketByUserTokenId: applicationtokenid={}, userticket={}, usertokenid={}, appTokenXml={}", applicationtokenid, userticket, userTokenId, appTokenXml);


        if (!verifyApplicationToken(applicationtokenid, appTokenXml)) {
            logger.warn("createUserTicketByUserTokenId - attempt to access from invalid application. ID: {}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity("Illegal application for this service").build();
        }
        final UserToken userToken = ActiveUserTokenRepository.getUserToken(userTokenId);
        if (userToken != null) {
            userticketmap.put(userticket, userToken.getTokenid());
            logger.trace("createUserTicketByUserTokenId OK. Response={}", userToken.toString());
            userToken.setNs2link(appConfig.getProperty("myuri") +"user/"+ applicationtokenid + "/validate_usertokenid/" + userToken.getTokenid());
            return Response.ok(new Viewable("/usertoken.ftl", userToken)).build();
        }
        logger.warn("createUserTicketByUserTokenId - attempt to access with non acceptable usertokenid {}", userTokenId);
        return Response.status(Response.Status.NOT_ACCEPTABLE).build();
    }


    /**
     * Used to get the usertoken from a usertokenid, which the application usually stores in its secure cookie
     *
     * @param applicationtokenid
     * @param appTokenXml
     * @param userTokenId
     * @return
     */
    @Path("/{applicationtokenid}/get_usertoken_by_usertokenid")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response getUserTokenByUserTokenId(@PathParam("applicationtokenid") String applicationtokenid,
                                     @FormParam("apptoken") String appTokenXml,
                                     @FormParam("usertokenid") String userTokenId) {
        logger.trace("getUserTokenByUserTokenId: applicationtokenid={}, usertokenid={}, appTokenXml={}", applicationtokenid, userTokenId, appTokenXml);

        if (ApplicationMode.getApplicationMode().equals(ApplicationMode.DEV)) {
            return DevModeHelper.return_DEV_MODE_ExampleUserToken(1);
        }

        if (!verifyApplicationToken(applicationtokenid, appTokenXml)) {
            logger.warn("getUserTokenByUserTokenId - attempt to access from invalid application. ID: {}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity("Illegal application for this service").build();
        }
        final UserToken userToken = ActiveUserTokenRepository.getUserToken(userTokenId);
        if (userToken != null) {
            logger.trace("getUserTokenByUserTokenId OK. Response={}", userToken.toString());
            userToken.setNs2link(appConfig.getProperty("myuri")+"user/" + applicationtokenid + "/validate_usertokenid/" + userToken.getTokenid());
            return Response.ok(new Viewable("/usertoken.ftl", userToken)).build();
        }
        if (applicationtokenidmap.get(userTokenId) != null){
            //UserToken netIQToken = new UserToken();
            //netIQToken.putApplicationCompanyRoleValue("11","SecurityTokenService","Whydah","WhydahUserAdmin","1");
            throw new RuntimeException("Branch in getUserTokenByUserTokenId which should never be reached! Talk to Totto or Erik!");
            /*
            UserToken netIQToken = UserTokenFactory.createNetIQToken();
            logger.trace("getUserTokenByUserTokenId OK. Response={}", netIQToken.toString());
            // /{applicationtokenid}/validate_usertokenid/{usertokenid}
            userToken.setNs2link(appConfig.getProperty("myuri") + applicationtokenid + "/validate_usertokenid/" + userToken.getTokenid());
            return Response.ok(new Viewable("/usertoken.ftl", netIQToken)).build();
            */
        }
        logger.warn("getUserTokenByUserTokenId - attempt to access with non acceptable usertokenid {}", userTokenId);
        return Response.status(Response.Status.NOT_ACCEPTABLE).build();
    }

    /**
     * Lookup a user by a one-time userticket, usually the first thing we do after receiving a SSO redirect back to
     * an application from SSOLoginWebApplication
     *
     *
     * @param applicationtokenid
     * @param appTokenXml
     * @param userticket
     * @return
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

        logger.trace("getUserTokenByUserTicket: applicationtokenid={}, userticket={}, appTokenXml={}", applicationtokenid, userticket, appTokenXml);

        if (ApplicationMode.getApplicationMode().equals(ApplicationMode.DEV)) {
            return DevModeHelper.return_DEV_MODE_ExampleUserToken(1);
        }

        if (!verifyApplicationToken(applicationtokenid, appTokenXml)) {
            logger.warn("getUserTokenByUserTicket - attempt to access from invalid application. ID: {}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity("Illegal application for this service").build();
        }
        String userTokenId = (String) userticketmap.get(userticket);
        if (userTokenId == null) {
            logger.warn("getUserTokenByUserTicket - Attempt to resolve non-existing ticket {}", userticket);
            return Response.status(Response.Status.GONE).build(); //410
        }
        logger.trace("getUserTokenByUserTicket - Found usertokenid: " + userTokenId);
        userticketmap.remove(userticket);
        final UserToken userToken = ActiveUserTokenRepository.getUserToken(userTokenId);

        if (userToken == null) {
            logger.warn("getUserTokenByUserTicket - illegal/Null userticket received ");
            return Response.status(Response.Status.NOT_ACCEPTABLE).build(); //406
        }
        logger.trace("getUserTokenByUserTicket OK. Response={}", userToken.toString());
        userToken.setNs2link(appConfig.getProperty("myuri")+"user/" + applicationtokenid + "/validate_usertokenid/" + userToken.getTokenid());
        return Response.ok(new Viewable("/usertoken.ftl", userToken)).build();
    }


    /**
     * Force cross-applications/SSO session logout. Use with extreme care as the user's hate the resulting user experience..
     *
     * @param applicationtokenid
     * @param usertokenid
     * @return
     */
    @Path("/{applicationtokenid}/release_usertoken")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response releaseUserToken(@PathParam("applicationtokenid") String applicationtokenid,
                                     @FormParam("usertokenid") String usertokenid) {
        logger.trace("releaseUserToken - entry.  usertokenid={}", usertokenid);
        if(!AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            logger.warn("releaseUserToken - attempt to access from invalid application. ID: {}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").build();
        }
        if (usertokenid == null) {
            logger.warn("releaseUserToken - attempt with no usertokenid: Null");
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing usertokenid.").build();
        }
        logger.trace("releaseUserToken - removed session, usertokenid={}", usertokenid);
        ActiveUserTokenRepository.removeUserToken(usertokenid);
        return Response.ok().build();
    }

    /**
     * This method is for elevating user access to a higher level for the receiving end of a session handover between SSO applications
     *
     * @param applicationtokenid
     * @param userTokenXml
     * @param newAppToken
     * @return
     */
    @Path("/{applicationtokenid}/transform_usertoken")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response transformUserToken(@PathParam("applicationtokenid") String applicationtokenid,
                                       @FormParam("usertoken") String userTokenXml,
                                       @FormParam("tp_applicationtoken") String newAppToken) {
        if (ApplicationMode.getApplicationMode().equals(ApplicationMode.DEV)) {
            return DevModeHelper.return_DEV_MODE_ExampleUserToken(1);
        }

        if(!AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            logger.warn("transformUserToken - attempt to access from invalid application. ID: {}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").build();
        }
        return Response.ok().language(userTokenXml).build();
    }


    private boolean verifyApplicationToken(String applicationtokenid, String applicationtokenXml) {
        boolean validAppToken = false;
        if (applicationtokenXml != null && applicationtokenid != null) {
            validAppToken = applicationtokenXml.contains(applicationtokenid) && AuthenticatedApplicationRepository.verifyApplicationToken(applicationtokenXml);
        } else {
            logger.debug("not expecting null values appTokenId {}, appTokenXml {}", applicationtokenid, applicationtokenXml);
            return false;
        }
        return true; //FIXME bli validAppToken;
    }


    /**
     * The SSOLoginWebApplication backend for 3rd party UserTokens. Receive a new user, create a Whydah UserIdentity with
     * the corresponding defaultroles (UAS|UIB) and create a new session with a one-time userticket for handover to receiving
     * SSO applications
     *
     * @param applicationtokenid
     * @param userticket
     * @param appTokenXml
     * @param userCredentialXml
     * @param thirdPartyUserTokenXml typically facebook user-token or other oauth2 usertoken
     * @return
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
        logger.trace("Response createAndLogOnUser: usercredential:" + userCredentialXml + "fbuser:" + thirdPartyUserTokenXml);

        if (ApplicationMode.getApplicationMode()==ApplicationMode.DEV ) {
            return DevModeHelper.return_DEV_MODE_ExampleUserToken(1);
        }

        if (!verifyApplicationToken(applicationtokenid, appTokenXml)) {
            // TODO:  Limit this operation to SSOLoginWebApplication ONLY
            logger.warn("createAndLogOnUser - attempt to access from invalid application. ID: {}", applicationtokenid);
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").build();
        }

        try {
            applicationtokenidmap.put(applicationtokenid,applicationtokenid);
            UserToken userToken = userAuthenticator.createAndLogonUser(applicationtokenid, appTokenXml, userCredentialXml, thirdPartyUserTokenXml);
            userticketmap.put(userticket, userToken.getTokenid());
            userToken.setNs2link(appConfig.getProperty("myuri")+"user/"+applicationtokenid+"/validate_usertokenid/"+userToken.getTokenid());
            return Response.ok(new Viewable("/usertoken.ftl", userToken)).build();
        } catch (AuthenticationFailedException ae) {
            logger.warn("createAndLogOnUser - Error creating or authenticating user. Token: {}", thirdPartyUserTokenXml);
            return Response.status(Response.Status.FORBIDDEN).entity("Error creating or authenticating user.").build();
        }
    }

    boolean isEmpty(String userticket) {
        boolean isEmpty = false;
        if (userticket == null || userticket.isEmpty()){
            isEmpty = true;
        }
        return isEmpty;
    }

}
