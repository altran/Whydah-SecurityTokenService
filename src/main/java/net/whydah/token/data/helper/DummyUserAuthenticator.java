package net.whydah.token.data.helper;

import net.whydah.token.config.ApplicationMode;
import net.whydah.token.data.UserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.*;

/**
 *
 * This class is responsible for handling test and development of this module as a standalone instance, shortcutting user authentication
 *
 */

public class DummyUserAuthenticator implements UserAuthenticator {
    private static final Logger logger = LoggerFactory.getLogger(DummyUserAuthenticator.class);
    private final static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();


    public UserToken logonUser(String applicationTokenId, String appTokenXml, String userCredentialXml){
        if (ApplicationMode.getApplicationMode().equalsIgnoreCase(ApplicationMode.DEV)) {
            UserToken ut = new UserToken();
            try {
                ut = ut.createFromUserTokenXML(loadFromFile(parseUsernameFromUserCredential(userCredentialXml)));
            } catch (Exception ioe) {
                logger.info("Could not load dummy token for username. "+ioe);
            }

            return ut ;
        }
        throw new IllegalStateException();
    }

    public UserToken createAndLogonUser(String applicationtokenid, String appTokenXml, String userCredentialXml, String fbUserXml){
        if (ApplicationMode.getApplicationMode().equalsIgnoreCase(ApplicationMode.DEV)) {
            return new UserToken();
        }
        throw new IllegalStateException();
    }



    private static String loadFromFile(String dummytokenuser) throws IOException {
        String xmlToken;
        try(BufferedReader br = new BufferedReader(new FileReader("t_"+dummytokenuser+".token"))) {
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

    private String parseUsernameFromUserCredential(String userCredential){
         return userCredential.substring(userCredential.indexOf("<username>") + "<username>".length(), userCredential.indexOf("</username>"));
    }


}