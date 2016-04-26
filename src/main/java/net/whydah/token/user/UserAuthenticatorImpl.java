package net.whydah.token.user;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache.ApacheHttpClient;
import net.whydah.sso.commands.adminapi.user.CommandGetUserAggregate;
import net.whydah.sso.commands.adminapi.user.CommandListUsers;
import net.whydah.token.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

public class UserAuthenticatorImpl implements UserAuthenticator {
    private static final Logger log = LoggerFactory.getLogger(UserAuthenticatorImpl.class);
    private static final String USER_AUTHENTICATION_PATH = "/auth/logon/user";
    private static final String CREATE_AND_LOGON_OPERATION = "createandlogon";
    private static final String defaultlifespan = "245000";


    private URI useradminservice;
    private final AppConfig appConfig;
    private final WebResource uasResource;
    private final UserTokenFactory userTokenFactory;


    @Inject
    public UserAuthenticatorImpl(@Named("useradminservice") URI useradminservice, UserTokenFactory userTokenFactory, AppConfig appConfig) {
        this.useradminservice = useradminservice;
        this.appConfig = appConfig;
        this.uasResource = ApacheHttpClient.create().resource(useradminservice);
        this.userTokenFactory = userTokenFactory;
    }

    @Override
    public UserToken logonUser(final String applicationTokenId, final String appTokenXml, final String userCredentialXml) {
        log.trace("logonUser - Calling UserAdminService at " + useradminservice + " appTokenXml:" + appTokenXml + " userCredentialXml:" + userCredentialXml);
        try {
            WebResource webResource = uasResource.path(applicationTokenId).path(USER_AUTHENTICATION_PATH);
            ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, userCredentialXml);

            UserToken userToken = getUserToken(applicationTokenId, appTokenXml, response);
            AppConfig.updateApplinks(useradminservice, applicationTokenId, response.toString());

            return userToken;
        } catch (Exception e) {
            log.error("Problems connecting to {}", useradminservice);
            log.info("Rethrowing exception ", e);
            throw e;
        }
    }

    @Override
    public UserToken createAndLogonUser(String applicationtokenid, String appTokenXml, String userCredentialXml, String fbUserXml) {
        log.trace("createAndLogonUser - Calling UserAdminService at with appTokenXml:\n" + appTokenXml + "userCredentialXml:\n" + userCredentialXml + "fbUserXml:\n" + fbUserXml);
        WebResource webResource = uasResource.path(applicationtokenid).path(USER_AUTHENTICATION_PATH).path(CREATE_AND_LOGON_OPERATION);
        log.debug("createAndLogonUser - Calling createandlogon " + webResource.toString());
        ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, fbUserXml);

        UserToken token = getUserToken(applicationtokenid, appTokenXml, response);
        token.setSecurityLevel("0");  // 3rd party token as source = securitylevel=0
        return token;
    }


    @Override
    public UserToken createAndLogonPinUser(String applicationtokenid, String appTokenXml, String adminUserTokenId, String cellPhone, String pin, String userJson) {
        if (ActivePinRepository.usePin(cellPhone, pin)) {
            try {
                WebResource uasWR = uasResource.path(applicationtokenid).path(adminUserTokenId).path("user");
                ClientResponse uasResponse = uasWR.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, userJson);
                if (uasResponse.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
                    String error = uasResponse.getEntity(String.class);
                    log.error(error);
                } else {
                    String userIdentityJson = uasResponse.getEntity(String.class);
                    UserToken userToken = UserTokenFactory.fromUserIdentityJson(userIdentityJson);
                    userToken.setSecurityLevel("0");  // 3rd party token as source = securitylevel=0
                    userToken.setLifespan(defaultlifespan);

                    ActiveUserTokenRepository.addUserToken(userToken, applicationtokenid);
                    return userToken;
                    // return Response.ok(new Viewable("/usertoken.ftl", myToken)).build();
                }
            } catch (Exception e) {
                log.error("Problems connecting to {}", useradminservice);
                throw e;
            }
        }
        throw new AuthenticationFailedException("APin uthentication failed. Status code ");
    }

    @Override
    public UserToken logonPinUser(String applicationtokenid, String appTokenXml, String adminUserTokenIdparam, String cellPhone, String pin) {
        log.trace("logonPinUser() called with " + "applicationtokenid = [" + applicationtokenid + "], appTokenXml = [" + appTokenXml + "], cellPhone = [" + cellPhone + "], pin = [" + pin + "]");
        if (ActivePinRepository.usePin(cellPhone, pin)) {
            try {
                String adminUserTokenId = getWhyDahAdminUserTokenId(applicationtokenid, appTokenXml);
                String usersQuery = cellPhone;

                // produserer userJson. denne kan inneholde fler users dette er json av
                String usersJson = new CommandListUsers(useradminservice, applicationtokenid, adminUserTokenId, usersQuery).execute();
                log.trace("CommandListUsers for query {} found users {}", usersQuery, usersJson);

                UserToken userTokenIdentity = getFirstMatch(usersJson, cellPhone);
                log.trace("Found matching UserIdentity {}", userTokenIdentity);

                String userAggregateJson = new CommandGetUserAggregate(useradminservice, applicationtokenid, adminUserTokenId, userTokenIdentity.getUid()).execute();

                UserToken userToken = UserTokenFactory.fromUserAggregateJson(userAggregateJson);
                userToken.setSecurityLevel("0");  // UserIdentity as source = securitylevel=0
                userToken.setLifespan(defaultlifespan);

                ActiveUserTokenRepository.addUserToken(userToken, applicationtokenid);

                //TODO Make flow for error handling in case of no userId above

                return userToken;
            } catch (Exception e) {
                log.error("Problems connecting to {}", useradminservice);
                throw e;
            }
        } else {
            log.warn("logonPinUser, illegal pin attempted");
        }
        throw new AuthenticationFailedException("Pin authentication failed. Status code ");
    }

    /**
     * This is a temporary solution. It should be replaced by a better serarch method in User Identity Backend's Lucene-serarch.
     * <p>
     * UIB's Lucene doesn't care about the "mobile="- or "username"-part of the query, resulting in a wildcard-search with the phoneno!
     *
     * @param usersJson
     * @param cellPhone
     * @return
     */
    private UserToken getFirstMatch(String usersJson, String cellPhone) {
        List<UserToken> userTokens = UserTokenFactory.fromUsersIdentityJson(usersJson);
        for (UserToken userIdentity : userTokens) {
            if (cellPhone.equals(userIdentity.getCellPhone()) && cellPhone.equals(userIdentity.getUserName())) {
                return userIdentity;
            }
        }
        return null;
    }


    private UserToken getUserToken(String applicationtokenid, String appTokenXml, ClientResponse response) {
        if (response.getStatus() == Response.Status.OK.getStatusCode() || response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            String userAggregateXML = response.getEntity(String.class);
            log.debug("Response from UserAdminService: {}", userAggregateXML);
            if (userAggregateXML.contains("logonFailed")) {
                throw new AuthenticationFailedException("Authentication failed.");
            }

            UserToken userToken = userTokenFactory.fromUserAggregate(userAggregateXML);
            userToken.setSecurityLevel("1");  // UserIdentity as source = securitylevel=0
            userToken.setLifespan(defaultlifespan);
            ActiveUserTokenRepository.addUserToken(userToken, applicationtokenid);
            return userToken;

        } else {
            log.error("getUserToken - Response from UAS: {}: {}", response.getStatus(), response.getEntity(String.class));
            throw new AuthenticationFailedException("Authentication failed. Status code " + response.getStatus());
        }
    }

    private String getWhyDahAdminUserTokenId(String applicationtokenid, String appTokenXml) {

        String user = appConfig.getProperty("whydah.adminuser.username");
        String password = appConfig.getProperty("whydah.adminuser.password");
        UserCredential userCredential = new UserCredential(user, password);
        UserToken whyDahUserAdminUserToken = logonUser(applicationtokenid, appTokenXml, userCredential.toXML());


        return whyDahUserAdminUserToken.getTokenid();
    }
}
