package net.whydah.sts.user.authentication;

import net.whydah.sso.user.types.UserToken;

public interface UserAuthenticator {
    UserToken logonUser(String applicationTokenId, String appTokenXml, String userCredentialXml);

    UserToken createAndLogonUser(String applicationtokenid, String appTokenXml, String userCredentialXml, String fbUserXml);
    UserToken createAndLogonPinUser(String applicationtokenid, String appTokenXml, String userCredentialXml, String cellPhone, String pin, String userJson);
    UserToken logonPinUser(String applicationtokenid, String appTokenXml, String adminUserTokenIdparam, String cellPhone, String pin);

    UserToken getRefreshedUserToken(String uid);
}
