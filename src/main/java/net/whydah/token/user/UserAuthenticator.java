package net.whydah.token.user;

public interface UserAuthenticator {
    UserToken logonUser(String applicationTokenId, String appTokenXml, String userCredentialXml);

    UserToken createAndLogonUser(String applicationtokenid, String appTokenXml, String userCredentialXml, String fbUserXml);
    UserToken createAndLogonPinUser(String applicationtokenid, String appTokenXml, String userCredentialXml, String cellPhone, String pin, String userJson);
    UserToken logonPinUser(String applicationtokenid, String appTokenXml, String adminUserTokenId,String cellPhone, String pin);
}
