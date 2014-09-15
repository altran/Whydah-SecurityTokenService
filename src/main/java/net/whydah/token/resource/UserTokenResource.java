package net.whydah.token.resource;

import com.google.inject.Inject;
import com.sun.jersey.api.view.Viewable;
import net.whydah.token.config.ApplicationMode;
import net.whydah.token.data.UserToken;
import net.whydah.token.data.application.AuthenticatedApplicationRepository;
import net.whydah.token.data.helper.ActiveUserTokenRepository;
import net.whydah.token.data.helper.DevModeHelper;
import net.whydah.token.data.helper.UserAuthenticator;
import net.whydah.token.exception.AuthenticationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;

@Path("/token")
public class UserTokenResource {
    private final static Logger logger = LoggerFactory.getLogger(UserTokenResource.class);

    private static Map ticketmap = new HashMap();
    private static Map  applicationtokenidmap = new HashMap();


    @Inject
    private UserAuthenticator userAuthenticator;

    @Context
    UriInfo uriInfo;

    @Path("/usertokentemplate")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response getUserTokenTemplate() {
        return Response.ok(new Viewable("/usertoken.ftl", new UserToken())).build();
    }

    /**
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
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").build();
        }
        try {
            UserToken token = userAuthenticator.logonUser(applicationtokenid, appTokenXml, userCredentialXml);
            return Response.ok(new Viewable("/usertoken.ftl", token)).build();
        } catch (AuthenticationFailedException ae) {
                return Response.status(Response.Status.FORBIDDEN).entity("User authentication failed").build();
        }
    }

    @Path("/{applicationtokenid}/{userticket}/usertoken")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response getUserToken(@PathParam("applicationtokenid") String applicationtokenid,
                                 @PathParam("userticket") String userticket,
                                 @FormParam("apptoken") String appTokenXml,
                                 @FormParam("usercredential") String userCredentialXml) {
        if (ApplicationMode.getApplicationMode().equals(ApplicationMode.DEV)) {
            return DevModeHelper.return_DEV_MODE_ExampleUserToken(1);
        }

        if (!verifyApplicationToken(applicationtokenid, appTokenXml)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").build();
        }
        try {
            UserToken token = userAuthenticator.logonUser(applicationtokenid,appTokenXml, userCredentialXml);
            ticketmap.put(userticket, token.getTokenid());
            return Response.ok(new Viewable("/usertoken.ftl", token)).build();
        } catch (AuthenticationFailedException ae) {
                return Response.status(Response.Status.FORBIDDEN).entity("User authentication failed").build();
        }
    }

    @Path("/{applicationtokenid}/{ticket}/createuser")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response createAndLogOnUser(@PathParam("applicationtokenid") String applicationtokenid,
                                 @PathParam("ticket") String ticket,
                                 @FormParam("apptoken") String appTokenXml,
                                 @FormParam("usercredential") String userCredentialXml,
                                 @FormParam("fbuser") String fbUserXml) {
        logger.trace("Response createAndLogOnUser: usercredential:"+userCredentialXml+"fbuser:"+fbUserXml);

        if (ApplicationMode.getApplicationMode()==ApplicationMode.DEV ) {
            return DevModeHelper.return_DEV_MODE_ExampleUserToken(1);
        }

        if (!verifyApplicationToken(applicationtokenid, appTokenXml)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").build();
        }

        try {
            applicationtokenidmap.put(applicationtokenid,applicationtokenid);
            UserToken token = userAuthenticator.createAndLogonUser(applicationtokenid,appTokenXml, userCredentialXml, fbUserXml);
            ticketmap.put(ticket, token.getTokenid());
            return Response.ok(new Viewable("/usertoken.ftl", token)).build();
        } catch (AuthenticationFailedException ae) {
            return Response.status(Response.Status.FORBIDDEN).entity("Error creating or authenticating user.").build();
        }
    }




    @Path("/{applicationtokenid}/validateusertoken")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response validateUserTokenXML(@PathParam("applicationtokenid") String applicationtokenid, @FormParam("usertoken") String userTokenXml) {
        if(!AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").build();
        }
        if (ActiveUserTokenRepository.verifyUserToken(UserToken.createFromUserTokenXML(userTokenXml))) {
            return Response.ok().build();
        }
        return Response.status(Response.Status.CONFLICT).build();
    }

    @Path("/{applicationtokenid}/validateusertokenid/{usertokenid}")
    @GET
    public Response validateUserTokenID(@PathParam("applicationtokenid") String applicationtokenid, @PathParam("usertokenid") String usertokenid) {
        if(!AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").build();
        }
        if (ActiveUserTokenRepository.getUserToken(usertokenid) != null) {
            logger.debug("Verified {}", usertokenid);
            return Response.ok().build();
        }
        logger.debug("Usertoken not ok: {}", usertokenid);
        return Response.status(Response.Status.CONFLICT).build();
    }

    @Path("/{applicationtokenid}/getusertokenbytokenid")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response getUserTokenById(@PathParam("applicationtokenid") String applicationtokenid,
                                     @FormParam("apptoken") String appTokenXml,
                                     @FormParam("usertokenid") String userTokenId) {
        logger.trace("getUserTokenById: applicationtokenid={}, usertokenid={}, appTokenXml={}", applicationtokenid, userTokenId, appTokenXml);

        if (ApplicationMode.getApplicationMode().equals(ApplicationMode.DEV)) {
            return DevModeHelper.return_DEV_MODE_ExampleUserToken(1);
        }

        if (!verifyApplicationToken(applicationtokenid, appTokenXml)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Illegal application for this service").build();
        }
        final UserToken userToken = ActiveUserTokenRepository.getUserToken(userTokenId);
        if (userToken != null) {
            logger.trace("getUserTokenByTokenID OK. Response={}", userToken.toString());
            return Response.ok(new Viewable("/usertoken.ftl", userToken)).build();
        }
        if (applicationtokenidmap.get(userTokenId)!=null){
            UserToken netIQToken = new UserToken();
            netIQToken.putApplicationCompanyRoleValue("11","SecurityTokenService","Whydah","WhydahUserAdmin","1");
            logger.trace("getUserTokenByTokenID OK. Response={}", netIQToken.toString());
            return Response.ok(new Viewable("/usertoken.ftl", netIQToken)).build();
        }
        return Response.status(Response.Status.NOT_ACCEPTABLE).build();
    }

    @Path("/{applicationtokenid}/getusertokenbyuserticket")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response getUserTokenByTicket(@PathParam("applicationtokenid") String applicationtokenid,
                                     @FormParam("apptoken") String appTokenXml,
                                     @FormParam("userticket") String userticket) {

        logger.trace("getUserTokenByTicket: applicationtokenid={}, ticket={}, appTokenXml={}", applicationtokenid, userticket, appTokenXml);

        if (ApplicationMode.getApplicationMode().equals(ApplicationMode.DEV)) {
            return DevModeHelper.return_DEV_MODE_ExampleUserToken(1);
        }

        if (!verifyApplicationToken(applicationtokenid, appTokenXml)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Illegal application for this service").build();
        }
        String userTokenId = (String) ticketmap.get(userticket);
        if (userTokenId == null) {
            logger.warn("Attempt to resolve non-existant ticket {}",userticket);
        	return Response.status(Response.Status.GONE).build(); //410 
        }
        logger.debug("Found tokenid: " + userTokenId);
        ticketmap.remove(userticket);
        final UserToken userToken = ActiveUserTokenRepository.getUserToken(userTokenId);

        if (userToken == null) {
            return Response.status(Response.Status.NOT_ACCEPTABLE).build(); //406
        }
        logger.trace("getUserTokenByTicket OK. Response={}", userToken.toString());
        return Response.ok(new Viewable("/usertoken.ftl", userToken)).build();
    }

    @Path("/{applicationtokenid}/releaseusertoken")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response releaseUserToken(@PathParam("applicationtokenid") String applicationtokenid,
                                     @FormParam("usertokenid") String userTokenID) {
        if(!AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").build();
        }
        if(userTokenID == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing usertokenid.").build();
        }
        ActiveUserTokenRepository.removeUserToken(userTokenID);
        return Response.ok().build();
    }

    @Path("/{applicationtokenid}/transformusertoken")
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
        }
        return true; //FIXME bli validAppToken;
    }
}
