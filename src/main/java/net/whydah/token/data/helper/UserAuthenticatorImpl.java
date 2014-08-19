package net.whydah.token.data.helper;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache.ApacheHttpClient;
import net.whydah.token.data.UserToken;
import net.whydah.token.exception.AuthenticationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

public class UserAuthenticatorImpl implements UserAuthenticator {
    private static final Logger logger = LoggerFactory.getLogger(UserAuthenticator.class);
    private static final String USER_TOKEN_URL = "usertoken";
    private static final String USER_URL = "user";
    private static final String CREATEANDLOGON_URL = "createandlogon";
    private static final String AUTHENTICATE = "authenticate";

    @Inject
    @Named("useridentitybackend")
    private URI useridentitybackend;
    private final Client restClient;

    public UserAuthenticatorImpl() {
        restClient = ApacheHttpClient.create();
    }

    public final UserToken logonUser(final String applicationTokenId,final String appTokenXml, final String userCredentialXml) {
        logger.trace("logonUser - Calling UserIdentityBackend at " + useridentitybackend);

        // /uib/{applicationTokenId}/authenticate/user
        try {
            WebResource webResource = restClient.resource(useridentitybackend).path(applicationTokenId).path(AUTHENTICATE).path(USER_URL);
            ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, userCredentialXml);

            UserToken token = getUserToken(appTokenXml, response);
            return token;
        } catch (Exception e) {
            logger.error("Problems connecting to {}", useridentitybackend);
            throw e;
        }
    }

    /**
     * @deprecated TODO move this functionality to new UserAdminService
     * @param applicationtokenid
     * @param appTokenXml
     * @param userCredentialXml
     * @param fbUserXml
     * @return
     */
    @Override
    public UserToken createAndLogonUser(String applicationtokenid, String appTokenXml, String userCredentialXml, String fbUserXml) {
        logger.trace("createAndLogonUser - Calling UserIdentityBackend at " + useridentitybackend);
        // TODO /uib//{applicationTokenId}/{applicationTokenId}/createandlogon/
        WebResource webResource = restClient.resource(useridentitybackend).path(applicationtokenid).path(applicationtokenid).path(CREATEANDLOGON_URL);
        logger.debug("createAndLogonUser - Calling createandlogon with fbUserXml= \n" + fbUserXml);
        ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, fbUserXml);

        UserToken token = getUserToken(appTokenXml, response);
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

        UserToken token = UserToken.createUserIdentity(appTokenXml, identityXML);
        ActiveUserTokenRepository.addUserToken(token);
        return token;
    }

}
