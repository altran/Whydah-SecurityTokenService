package net.whydah.token.data.helper;

import net.whydah.token.config.ApplicationMode;
import net.whydah.token.data.UserToken;


public class DummyUserAuthenticator implements UserAuthenticator {

    public UserToken logonUser(String applicationTokenId, String appTokenXml, String userCredentialXml){
        if (ApplicationMode.getApplicationMode().equalsIgnoreCase(ApplicationMode.DEV)) {
            return new UserToken();
        }
        throw new IllegalStateException();
    }

    public UserToken createAndLogonUser(String applicationtokenid, String appTokenXml, String userCredentialXml, String fbUserXml){
        if (ApplicationMode.getApplicationMode().equalsIgnoreCase(ApplicationMode.DEV)) {
            return new UserToken();
        }
        throw new IllegalStateException();
    }
}
