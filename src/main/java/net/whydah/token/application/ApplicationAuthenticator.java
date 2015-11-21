package net.whydah.token.application;

import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.application.types.ApplicationToken;

public interface ApplicationAuthenticator {
    ApplicationToken logonApplication(ApplicationCredential applicationCredential);

    boolean validateApplication(ApplicationToken applicationToken);
}
