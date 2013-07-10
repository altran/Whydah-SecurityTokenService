package net.whydah.iam.service.resource;

import com.google.inject.Inject;
import com.sun.jersey.api.view.Viewable;
import net.whydah.iam.service.data.helper.AuthenticatedApplicationRepository;
import net.whydah.iam.service.data.UserToken;
import net.whydah.iam.service.data.helper.ActiveUserTokenRepository;
import net.whydah.iam.service.data.helper.UserAuthenticator;
import net.whydah.iam.service.exception.AuthenticationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;

@Path("/iam")
public class UserTokenResource {
    private final static Logger logger = LoggerFactory.getLogger(UserTokenResource.class);

    private static Map ticketmap = new HashMap();

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

    @Path("/{applicationtokenid}/usertoken")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response getUserToken(@PathParam("applicationtokenid") String applicationtokenid,
                                 @FormParam("apptoken") String appTokenXml,
                                 @FormParam("usercredential") String userCredentialXml) {
        if (!verifyApptoken(applicationtokenid, appTokenXml)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").build();
        }
        try {
            UserToken token = userAuthenticator.logonUser(appTokenXml, userCredentialXml);
            return Response.ok(new Viewable("/usertoken.ftl", token)).build();
        } catch (AuthenticationFailedException ae) {
                return Response.status(Response.Status.FORBIDDEN).entity("User authentication failed").build();
        }
    }

    @Path("/{applicationtokenid}/{ticketid}/usertoken")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response getUserToken(@PathParam("applicationtokenid") String applicationtokenid,
                                 @PathParam("ticketid") String ticketid,
                                 @FormParam("apptoken") String appTokenXml,
                                 @FormParam("usercredential") String userCredentialXml) {
        if (!verifyApptoken(applicationtokenid, appTokenXml)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").build();
        }
        try {
            UserToken token = userAuthenticator.logonUser(appTokenXml, userCredentialXml);
            ticketmap.put(ticketid, token.getTokenid());
            return Response.ok(new Viewable("/usertoken.ftl", token)).build();
        } catch (AuthenticationFailedException ae) {
                return Response.status(Response.Status.FORBIDDEN).entity("User authentication failed").build();
        }
    }

    @Path("/{applicationtokenid}/{ticketid}/createuser")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response createAndLogOnUser(@PathParam("applicationtokenid") String applicationtokenid,
                                 @PathParam("ticketid") String ticketid,
                                 @FormParam("apptoken") String appTokenXml,
                                 @FormParam("usercredential") String userCredentialXml,
                                 @FormParam("fbuser") String fbUserXml) {

        if (!verifyApptoken(applicationtokenid, appTokenXml)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").build();
        }

        try {
            UserToken token = userAuthenticator.createAndLogonUser(appTokenXml, userCredentialXml, fbUserXml);
            ticketmap.put(ticketid, token.getTokenid());
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
        logger.debug("usertokenid: {}", userTokenId);
        logger.debug("applicationtokenid: {}", applicationtokenid);
        logger.debug("appTokenXml: {}", appTokenXml);

        if (!verifyApptoken(applicationtokenid, appTokenXml)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Illegal application for this service").build();
        }
        final UserToken userToken = ActiveUserTokenRepository.getUserToken(userTokenId);
        if (userToken != null) {
            return Response.ok(new Viewable("/usertoken.ftl", userToken)).build();
        }
        return Response.status(Response.Status.NOT_ACCEPTABLE).build();
    }

    @Path("/{applicationtokenid}/getusertokenbyticketid")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response getUserTokenByTicketId(@PathParam("applicationtokenid") String applicationtokenid,
                                     @FormParam("apptoken") String appTokenXml,
                                     @FormParam("ticketid") String ticketId) {
        logger.debug("ticketid: {}", ticketId);
        if (!verifyApptoken(applicationtokenid, appTokenXml)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Illegal application for this service").build();
        }
        String userTokenId = (String)ticketmap.get(ticketId);
        if (userTokenId == null) {
        	return Response.status(Response.Status.GONE).build(); //410 
        }
        logger.debug("Found tokenid: "+userTokenId);
        ticketmap.remove(ticketId);
        final UserToken userToken = ActiveUserTokenRepository.getUserToken(userTokenId);

        if (userToken != null) {
            return Response.ok(new Viewable("/usertoken.ftl", userToken)).build();
        }
        return Response.status(Response.Status.NOT_ACCEPTABLE).build(); //406
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
        if(!AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Application authentication not valid.").build();
        }
        return Response.ok().language(userTokenXml).build();
    }

    private boolean verifyApptoken(String apptokenid, String appTokenXml) {
        return appTokenXml.contains(apptokenid) && AuthenticatedApplicationRepository.verifyApplicationToken(appTokenXml);
    }
}
