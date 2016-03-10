package net.whydah.token.user;

public interface UserAuthenticator {
    UserToken logonUser(String applicationTokenId, String appTokenXml, String userCredentialXml);

    UserToken createAndLogonUser(String applicationtokenid, String appTokenXml, String userCredentialXml, String fbUserXml);
    public UserToken createAndLogonPinUser(String applicationtokenid, String appTokenXml, String userCredentialXml, String pin, String userJson);
}
