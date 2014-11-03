package net.whydah.token.application;

public interface ApplicationAuthenticator {
    ApplicationToken logonApplication(ApplicationCredential applicationCredential);

    boolean validateApplication(ApplicationToken applicationToken);
}
