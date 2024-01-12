package net.whydah.sts.user.authentication;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.whydah.sso.application.mappers.ApplicationTokenMapper;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.commands.adminapi.user.CommandGetUserAggregate;
import net.whydah.sso.commands.adminapi.user.CommandListUsers;
import net.whydah.sso.user.mappers.UserCredentialMapper;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserCredential;
import net.whydah.sso.user.types.UserToken;
import net.whydah.sts.application.AuthenticatedApplicationTokenRepository;
import net.whydah.sts.config.AppConfig;
import net.whydah.sts.errorhandling.AuthenticationFailedException;
import net.whydah.sts.user.AuthenticatedUserTokenRepository;
import net.whydah.sts.user.UserTokenFactory;
import net.whydah.sts.user.authentication.commands.CommandCreateFBUser;
import net.whydah.sts.user.authentication.commands.CommandCreatePinUser;
import net.whydah.sts.user.authentication.commands.CommandVerifyUserCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public class UserAuthenticatorImpl implements UserAuthenticator {
    private static final Logger log = LoggerFactory.getLogger(UserAuthenticatorImpl.class);


    private URI useradminservice;
    private final AppConfig appConfig;


    @Inject
    public UserAuthenticatorImpl(@Named("useradminservice") URI useradminservice, UserTokenFactory userTokenFactory, AppConfig appConfig) {
        this.useradminservice = useradminservice;
        this.appConfig = appConfig;
    }

    @Override
    public UserToken logonUser(String applicationTokenId, String appTokenXml, final String userCredentialXml) throws AuthenticationFailedException {
        UserCredential userCredential = UserCredentialMapper.fromXml(userCredentialXml);
        if (userCredential != null) {
            log.trace("logonUser - Calling UserAdminService at " + useradminservice + " appTokenXml:" + appTokenXml + " userCredentialSafeXml:" + userCredential.toSafeXML());
        } else {
            log.trace("logonUser - Unable to map userCredentialXML - Calling UserAdminService at " + useradminservice + " appTokenXml:" + appTokenXml + " userCredentialXml:" + userCredentialXml);

        }

        UserToken uToken = new CommandVerifyUserCredential(useradminservice, appTokenXml, applicationTokenId, userCredentialXml).execute();
        if (uToken == null) {
            throw new AuthenticationFailedException(String.format("Authentication failed for user: %s, appTokenId: %s", userCredentialXml, applicationTokenId));
        }
        // credential check success
        UserToken existingUserToken = AuthenticatedUserTokenRepository.getUserTokenByUserName(userCredential.getUserName(), applicationTokenId);
        if (existingUserToken != null && existingUserToken.isValid()) {
            // re-use id from existing user-token in order to avoid invaliding existing sessions
            uToken.setUserTokenId(existingUserToken.getUserTokenId());
            // TODO figure out whether more fields should be copied from existing user-token, or whether we should just
            //  re-use and refresh existing token and throw away the one created from credential check.
        }
        return AuthenticatedUserTokenRepository.addUserToken(uToken, applicationTokenId, "usertokenid");
    }

    @Override
    public UserToken createAndLogonUser(String applicationTokenId, String appTokenXml, String userCredentialXml, String thirdpartyUserXML) throws AuthenticationFailedException {
        log.trace("createAndLogonUser - Calling UserAdminService at with appTokenXml:\n" + appTokenXml + "userCredentialXml:\n" + userCredentialXml + "thirdpartyUserXML:\n" + thirdpartyUserXML);
        UserToken userToken = new CommandCreateFBUser(useradminservice, appTokenXml, applicationTokenId, thirdpartyUserXML).execute();
        return AuthenticatedUserTokenRepository.addUserToken(userToken, applicationTokenId, "usertokenid");
    }


    @Override
    public UserToken createAndLogonPinUser(String applicationTokenId, String appTokenXml, String adminUserTokenId, String cellPhone, String pin, String userJson) {
        if (ActivePinRepository.usePin(cellPhone, pin)) {
            try {
                UserToken userToken = new CommandCreatePinUser(useradminservice, appTokenXml, applicationTokenId, adminUserTokenId, userJson).execute();
                if (userToken == null) {
                    throw new AuthenticationFailedException("Pin authentication failed. Status code ");

                } else {
                    return AuthenticatedUserTokenRepository.addUserToken(userToken, applicationTokenId, "pin");
                }
            } catch (Exception e) {
                log.error(String.format("createAndLogonPinUser - Problems connecting to %s", useradminservice), e);
                throw e;
            }
        }
        throw new AuthenticationFailedException("Pin authentication failed. Status code ");
    }

    public UserToken getRefreshedUserToken(String usertokenid) {
        try {
            ApplicationToken stsApplicationToken = AuthenticatedApplicationTokenRepository.getSTSApplicationToken();
            String user = appConfig.getProperty("whydah.adminuser.username");
            String password = appConfig.getProperty("whydah.adminuser.password");
            UserCredential userCredential = new UserCredential(user, password);
            UserToken whydahUserAdminUserToken = logonUser(stsApplicationToken.getApplicationTokenId(), ApplicationTokenMapper.toXML(stsApplicationToken), userCredential.toXML());

            UserToken oldUserToken = AuthenticatedUserTokenRepository.getUserToken(usertokenid, stsApplicationToken.getApplicationTokenId());

            String userAggregateJson = new CommandGetUserAggregate(useradminservice, stsApplicationToken.getApplicationTokenId(), whydahUserAdminUserToken.getUserTokenId(), oldUserToken.getUid()).execute();

            UserToken refreshedUserToken = UserTokenMapper.fromUserAggregateJson(userAggregateJson);

            refreshedUserToken.setTimestamp(oldUserToken.getTimestamp());
            refreshedUserToken.setLifespan(oldUserToken.getLifespan());
            refreshedUserToken.setUserTokenId(usertokenid);

            return refreshedUserToken;
        } catch (Exception e) {
            log.warn("Unable to use STScredentials to refresh usertoken", e);
        }
        return null;

    }

    @Override
    public UserToken logonPinUser(String applicationtokenid, String appTokenXml, String adminUserTokenId, String cellPhone, String pin) {
        log.info("logonPinUser() called with " + "applicationtokenid = [" + applicationtokenid + "], appTokenXml = [" + appTokenXml + "], cellPhone = [" + cellPhone + "], pin = [" + pin + "]");
        if (ActivePinRepository.usePin(cellPhone, pin)) {
            String usersQuery = cellPhone;
            // produserer userJson. denne kan inneholde fler users dette er json av
            String usersJson = new CommandListUsers(useradminservice, applicationtokenid, adminUserTokenId, usersQuery).execute();
            log.info("CommandListUsers for query {} found users {}", usersQuery, usersJson);
            UserToken userTokenIdentity = getFirstMatch(usersJson, usersQuery);
            if (userTokenIdentity != null) {
                log.info("Found matching UserIdentity {}", userTokenIdentity);

                String userAggregateJson = new CommandGetUserAggregate(useradminservice, applicationtokenid, adminUserTokenId, userTokenIdentity.getUid()).execute();

                UserToken userToken = UserTokenMapper.fromUserAggregateJson(userAggregateJson);
                userToken.setSecurityLevel("0");  // UserIdentity as source = securitylevel=0
                userToken.setTimestamp(String.valueOf(System.currentTimeMillis()));

                return AuthenticatedUserTokenRepository.addUserToken(userToken, applicationtokenid, "pin");

            } else {
                log.error("Unable to find a user matching the given phonenumber.");
                throw new AuthenticationFailedException("Unable to find a user matching the given phonenumber.");
            }
        } else {
            log.warn("logonPinUser, illegal pin attempted - pin not registered");
            throw new AuthenticationFailedException("Pin authentication failed. Status code ");
        }

    }


    /**
     * Method to enable pin-logon for whydah users
     * Implements the following prioritizing
     * a)  userName+cellPhone = number
     * b)  userName = number
     * c)  cellPhone=number
     *
     * @param usersJson
     * @param cellPhone
     * @return
     */
    private UserToken getFirstMatch(String usersJson, String cellPhone) {
        log.info("Searching for: ", cellPhone);
        log.info("Searching in: ", usersJson);
        List<UserToken> userTokens = UserTokenFactory.fromUsersIdentityJson(usersJson);
        // First lets find complete matches
        for (UserToken userIdentity : userTokens) {
            if (cellPhone.equals(userIdentity.getCellPhone()) && cellPhone.equals(userIdentity.getUserName())) {
                return userIdentity;
            }
        }
        // The prioritize userName
        for (UserToken userIdentity : userTokens) {
            log.info("getFirstMatch: getUserName: " + userIdentity.getUserName());
            if (cellPhone.equals(userIdentity.getUserName())) {
                return userIdentity;
            }
        }
        // The and finally cellPhone users
        for (UserToken userIdentity : userTokens) {
            log.info("getFirstMatch: cellPhone: " + userIdentity.getCellPhone());
            if (cellPhone.equals(userIdentity.getCellPhone())) {
                return userIdentity;
            }
        }
        return null;
    }


    private static String generateID() {
        return UUID.randomUUID().toString();
    }


}
