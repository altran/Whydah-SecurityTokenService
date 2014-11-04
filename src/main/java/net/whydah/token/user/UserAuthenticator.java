package net.whydah.token.user;

public interface UserAuthenticator {
    UserToken2 logonUser(String applicationTokenId, String appTokenXml, String userCredentialXml);

    UserToken2 createAndLogonUser(String applicationtokenid, String appTokenXml, String userCredentialXml, String fbUserXml);
}
