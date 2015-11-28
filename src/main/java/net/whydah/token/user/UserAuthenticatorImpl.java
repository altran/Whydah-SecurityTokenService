package net.whydah.token.user;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache.ApacheHttpClient;
import net.whydah.token.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

public class UserAuthenticatorImpl implements UserAuthenticator {
    private static final Logger log = LoggerFactory.getLogger(UserAuthenticatorImpl.class);
    private static final String USER_AUTHENTICATION_PATH = "/auth/logon/user";
    private static final String CREATE_AND_LOGON_OPERATION = "createandlogon";


    private URI useradminservice;
    private final WebResource uasResource;
    private final UserTokenFactory userTokenFactory;



    @Inject
    public UserAuthenticatorImpl(@Named("useradminservice") URI useradminservice, UserTokenFactory userTokenFactory) {
        this.useradminservice = useradminservice;
        this.uasResource = ApacheHttpClient.create().resource(useradminservice);
        this.userTokenFactory = userTokenFactory;
    }

    @Override
    public UserToken logonUser(final String applicationTokenId, final String appTokenXml, final String userCredentialXml) {
        log.trace("logonUser - Calling UserAdminService at " + useradminservice + " appTokenXml:" + appTokenXml + " userCredentialXml:" + userCredentialXml);
        try {
            WebResource webResource = uasResource.path(applicationTokenId).path(USER_AUTHENTICATION_PATH);
            ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, userCredentialXml);

            UserToken userToken = getUserToken(appTokenXml, response);
            AppConfig.updateApplinks(useradminservice,applicationTokenId,response.toString());

            return userToken;
        } catch (Exception e) {
            log.error("Problems connecting to {}", useradminservice);
            throw e;
        }
    }

    @Override
    public UserToken createAndLogonUser(String applicationtokenid, String appTokenXml, String userCredentialXml, String fbUserXml) {
        log.trace("createAndLogonUser - Calling UserAdminService at with appTokenXml:\n" + appTokenXml + "userCredentialXml:\n" + userCredentialXml + "fbUserXml:\n" + fbUserXml);
        WebResource webResource = uasResource.path(applicationtokenid).path(USER_AUTHENTICATION_PATH).path(CREATE_AND_LOGON_OPERATION);
        log.debug("createAndLogonUser - Calling createandlogon " + webResource.toString());
        ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, fbUserXml);

        UserToken token = getUserToken(appTokenXml, response);
        token.setSecurityLevel("0");  // 3rd party token as source = securitylevel=0
        return token;
    }


    private UserToken getUserToken(String appTokenXml, ClientResponse response) {
        if (response.getStatus() == Response.Status.OK.getStatusCode() || response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()){
            String userAggregateXML = response.getEntity(String.class);
            log.debug("Response from UserAdminService: {}", userAggregateXML);
            if (userAggregateXML.contains("logonFailed")) {
                throw new AuthenticationFailedException("Authentication failed.");
            }

            UserToken userToken = userTokenFactory.fromUserAggregate(userAggregateXML);
            userToken.setSecurityLevel("1");  // UserIdentity as source = securitylevel=0
            ActiveUserTokenRepository.addUserToken(userToken);
            return userToken;

        } else  {
            log.error("Response from UAS: {}: {}", response.getStatus(), response.getEntity(String.class));
            throw new AuthenticationFailedException("Authentication failed. Status code " + response.getStatus());
        }
    }

}
