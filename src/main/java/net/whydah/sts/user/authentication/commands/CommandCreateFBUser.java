package net.whydah.sts.user.authentication.commands;

import com.github.kevinsawicki.http.HttpRequest;
import net.whydah.sso.commands.baseclasses.BaseHttpPostHystrixCommand;
import net.whydah.sso.ddd.model.application.ApplicationTokenID;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserToken;
import net.whydah.sts.errorhandling.AuthenticationFailedException;
import net.whydah.sts.threat.ThreatResource;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.UUID;

public class CommandCreateFBUser extends BaseHttpPostHystrixCommand<UserToken> {

    private String fbUserXml;

    public CommandCreateFBUser(URI userAdminServiceUri, String appTokenXml, String myAppTokenId, String fbUserXml) {
        super(userAdminServiceUri, appTokenXml, myAppTokenId, "UASUserAdminGroup", 2000);

        this.fbUserXml = fbUserXml;
        if (userAdminServiceUri == null || !ApplicationTokenID.isValid(myAppTokenId) || fbUserXml == null) {
            log.error(TAG + " initialized with null-values - will fail - userAdminServiceUri:{}, myAppTokenId:{},  fbUserXml:{}", userAdminServiceUri, myAppTokenId, fbUserXml);
        }
    }

    @Override
    protected HttpRequest dealWithRequestBeforeSend(HttpRequest request) {
        return request.contentType(MediaType.APPLICATION_XML).send(fbUserXml);
    }

    @Override
    protected UserToken dealWithResponse(String response) {

        if (response.length() > 32) {
            log.trace("Response from UserAdminService: {}", response);
            if (response.contains("logonFailed")) {
                throw new AuthenticationFailedException("Authentication failed.");
            }

            UserToken userToken = UserTokenMapper.fromUserAggregateXml(response);
            userToken.setSecurityLevel("0");  // UserIdentity as source = securitylevel=0
            userToken.setUserTokenId(UUID.randomUUID().toString());
            userToken.setDefcon(ThreatResource.getDEFCON());
            userToken.setTimestamp(String.valueOf(System.currentTimeMillis()));
            log.debug("Returning UserToken: {}", userToken);


            return userToken;
        }
        return null;
    }

    @Override
    protected String getTargetPath() {
        return myAppTokenId + "/auth/logon/user/createandlogon";
    }

    /**
     * 	private static final String USER_AUTHENTICATION_PATH = "/auth/logon/user";
     private static final String CREATE_AND_LOGON_OPERATION = "createandlogon";

     *
     * 		log.trace("createAndLogonUser - Calling UserAdminService at with appTokenXml:\n" + appTokenXml + "userCredentialXml:\n" + userCredentialXml + "fbUserXml:\n" + fbUserXml);
     WebResource webResource = uasResource.path(applicationtokenid).path(USER_AUTHENTICATION_PATH).path(CREATE_AND_LOGON_OPERATION);
     log.debug("createAndLogonUser - Calling createandlogon " + webResource.toString());
     ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, fbUserXml);

     UserToken token = getUserToken(applicationtokenid, appTokenXml, response);
     token.setSecurityLevel("0");  // 3rd party sts as source = securitylevel=0

     */
}

