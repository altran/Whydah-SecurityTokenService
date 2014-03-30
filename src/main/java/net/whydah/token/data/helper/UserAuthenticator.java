package net.whydah.token.data.helper;

import net.whydah.token.data.UserToken;

public interface UserAuthenticator {
    public UserToken logonUser(String applicationTokenId, String appTokenXml, String userCredentialXml);

    UserToken createAndLogonUser(String appTokenXml, String userTokenId, String userCredentialXml, String fbUserXml);
}
