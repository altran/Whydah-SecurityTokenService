package net.whydah.token.data.application;

public interface ApplicationAuthenticator {
    ApplicationToken logonApplication(ApplicationCredential applicationCredential);

    boolean validateApplication(ApplicationToken applicationToken);
}
