package net.whydah.iam.service.data.helper;

import net.whydah.iam.service.data.UserToken;

public interface UserAuthenticator {
    public UserToken logonUser(String appTokenXml, String userCredentialXml);

    UserToken createAndLogonUser(String appTokenXml, String userCredentialXml, String fbUserXml);
}
