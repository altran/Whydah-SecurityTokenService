package net.whydah.token.user;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache.ApacheHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

public class UserAuthenticatorImpl implements UserAuthenticator {
    private static final Logger logger = LoggerFactory.getLogger(UserAuthenticatorImpl.class);
    private static final String USER_AUTHENTICATION_PATH = "/authenticate/user";
    private static final String CREATE_AND_LOGON_PATH = "createandlogon";
    //private static final String USER_TOKEN_URL = "usertoken";
    //private static final String AUTHENTICATE = "authenticate";
    //private static final String USER_URL = "user";


    //@Named("useridentitybackend")
    private URI useridentitybackend;
    private final WebResource uibResource;

    @Inject
    public UserAuthenticatorImpl(@Named("useridentitybackend") URI useridentitybackend) {
        this.useridentitybackend = useridentitybackend;
        uibResource = ApacheHttpClient.create().resource(useridentitybackend);
    }

    @Override
    public UserToken logonUser(final String applicationTokenId, final String appTokenXml, final String userCredentialXml) {
        logger.trace("logonUser - Calling UserIdentityBackend at " + useridentitybackend + " appTokenXml:" + appTokenXml + " userCredentialXml:" + userCredentialXml);
        try {
            // /uib/{applicationTokenId}/authenticate/user
            WebResource webResource = uibResource.path(applicationTokenId).path(USER_AUTHENTICATION_PATH);
            ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, userCredentialXml);

            UserToken token = getUserToken(appTokenXml, response);
            return token;
        } catch (Exception e) {
            logger.error("Problems connecting to {}", useridentitybackend);
            throw e;
        }
    }

    @Deprecated     //TODO move this functionality to new UserAdminService
    @Override
    public UserToken createAndLogonUser(String applicationtokenid, String appTokenXml, String userCredentialXml, String fbUserXml) {
        logger.trace("createAndLogonUser - Calling UserIdentityBackend at with appTokenXml:\n" + appTokenXml + "userCredentialXml:\n" + userCredentialXml + "fbUserXml:\n" + fbUserXml);
        // TODO /uib//{applicationTokenId}/{applicationTokenId}/createandlogon/
        // TODO /authenticate/user
        WebResource webResource = uibResource.path(applicationtokenid).path(USER_AUTHENTICATION_PATH).path(CREATE_AND_LOGON_PATH);
        logger.debug("createAndLogonUser - Calling createandlogon " + webResource.toString());
        ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, fbUserXml);

        UserToken token = getUserToken(appTokenXml, response);
        token.setSecurityLevel("0");  // 3rd party token as source = securitylevel=0
        return token;
    }


    private UserToken getUserToken(String appTokenXml, ClientResponse response) {
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            logger.error("Response from UIB: {}: {}", response.getStatus(), response.getEntity(String.class));
            throw new AuthenticationFailedException("Authentication failed. Status code " + response.getStatus());
        }
        String identityXML = response.getEntity(String.class);
        logger.debug("Response from UserIdentityBackend: {}", identityXML);
        if (identityXML.contains("logonFailed")) {
            throw new AuthenticationFailedException("Authentication failed.");
        }

        UserToken token = UserToken.createUserTokenFromUserAggregate(appTokenXml, identityXML);
        token.setSecurityLevel("1");  // UserIdentity as source = securitylevel=0
        ActiveUserTokenRepository.addUserToken(token);
        return token;
    }

}
