package net.whydah.token.user;

import net.whydah.token.config.ApplicationMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * This class is responsible for handling test and development of this module as a standalone instance, shortcutting user authentication
 */

public class DummyUserAuthenticator implements UserAuthenticator {
    private static final Logger logger = LoggerFactory.getLogger(DummyUserAuthenticator.class);
    private final static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();


    public UserToken logonUser(String applicationTokenId, String appTokenXml, String userCredentialXml) {
        if (ApplicationMode.getApplicationMode().equalsIgnoreCase(ApplicationMode.DEV)) {
            UserToken ut = new UserToken();
            try {
                ut = ut.createUserTokenFromUserTokenXML(loadFromFile(parseUsernameFromUserCredential(userCredentialXml)));
            } catch (Exception ioe) {
                logger.info("Could not load dummy token for username. " + ioe);
            }

            return ut;
        }
        throw new IllegalStateException();
    }

    public UserToken createAndLogonUser(String applicationtokenid, String appTokenXml, String userCredentialXml, String fbUserXml) {
        if (ApplicationMode.getApplicationMode().equalsIgnoreCase(ApplicationMode.DEV)) {
            return new UserToken();
        }
        throw new IllegalStateException();
    }


    private static String loadFromFile(String dummytokenuser) throws IOException {
        String xmlToken;
        try (BufferedReader br = new BufferedReader(new FileReader("t_" + dummytokenuser + ".token"))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            xmlToken = sb.toString();
        }
        return xmlToken;
    }

    private String parseUsernameFromUserCredential(String userCredential) {
        return userCredential.substring(userCredential.indexOf("<username>") + "<username>".length(), userCredential.indexOf("</username>"));
    }


}