package net.whydah.token.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 03.11.14
 */
public class UserToken2Factory {
    private static final Logger logger = LoggerFactory.getLogger(UserToken2Factory.class);
    private static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    private final Random rand = new Random();

    private static String defcon = "0";
    private String issuer = "/token/issuer/tokenverifier";
    private String lifespan = String.valueOf(60 * 60 * rand.nextInt(1000));


    public UserToken2Factory(String defcon) {
        UserToken2Factory.defcon = defcon;
    }

    public static UserToken2 fromUserTokenXml(String userTokenXml) {
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(userTokenXml)));
            XPath xPath = XPathFactory.newInstance().newXPath();

            String uid = (String) xPath.evaluate("/usertoken/uid", doc, XPathConstants.STRING);
            String personRef = (String) xPath.evaluate("/usertoken/personRef", doc, XPathConstants.STRING);
            String userName = (String) xPath.evaluate("/usertoken/username", doc, XPathConstants.STRING);
            String firstName = (String) xPath.evaluate("/usertoken/firstname", doc, XPathConstants.STRING);
            String lastName = (String) xPath.evaluate("/usertoken/lastname", doc, XPathConstants.STRING);
            String email = (String) xPath.evaluate("/usertoken/email", doc, XPathConstants.STRING);
            String securityLevel = (String) xPath.evaluate("/usertoken/securitylevel", doc, XPathConstants.STRING);

            String tokenid = (String) xPath.evaluate("/usertoken/@id", doc, XPathConstants.STRING);
            String timestamp = (String) xPath.evaluate("/usertoken/timestamp", doc, XPathConstants.STRING);
            String defcon = (String) xPath.evaluate("/usertoken/DEFCON", doc, XPathConstants.STRING);
            String lifespan = (String) xPath.evaluate("/usertoken/lifespan", doc, XPathConstants.STRING);
            String issuer = (String) xPath.evaluate("/usertoken/issuer", doc, XPathConstants.STRING);
            //applicationCompanyRoleValueMap = new HashMap<>();
            //parseAndUpdateRolemapFromUserToken(doc);

            List<ApplicationRoleEntry> roleList = new ArrayList<>();
            //TODO verify XPATH for application
            /*
            //NodeList applicationNodes = (NodeList) xPath.evaluate("/whydahuser/applications/application/appId", doc, XPathConstants.NODESET);
            NodeList applicationNodes = (NodeList) xPath.evaluate("//application", doc, XPathConstants.NODESET);
            for (int i = 1; i < applicationNodes.getLength() + 1; i++) {
                ApplicationRoleEntry role = new ApplicationRoleEntry();
                role.setApplicationid((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/appId", doc, XPathConstants.STRING));
                role.setApplicationname((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/applicationName", doc, XPathConstants.STRING));
                role.setOrganizationname((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/orgName", doc, XPathConstants.STRING));
                role.setRolename((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/roleName", doc, XPathConstants.STRING));
                role.setRolevalue((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/roleValue", doc, XPathConstants.STRING));
                roleList.add(role);
            }
            */

            UserToken2 userToken = new UserToken2();
            userToken.setUid(uid);
            userToken.setUserName(userName);
            userToken.setFirstName(firstName);
            userToken.setLastName(lastName);
            userToken.setEmail(email);
            userToken.setPersonRef(personRef);
            userToken.setSecurityLevel(securityLevel);
            userToken.setRoleList(roleList);

            userToken.setTokenid(tokenid);
            userToken.setTimestamp(timestamp);
            UserToken2.setDefcon(defcon);
            userToken.setLifespan(lifespan);
            userToken.setIssuer(issuer);

            return userToken;
        } catch (Exception e) {
            logger.error("Error parsing userTokenXml " + userTokenXml, e);
            return null;
        }
    }

    public UserToken2 createUserTokenFromUserAggregate(String appTokenXml, String userAggregateXML) {
        UserToken2 userToken = fromUserAggregate(userAggregateXML);
        String issuer = extractIssuer(appTokenXml);
        userToken.setIssuer(issuer);
        userToken.setTokenid(generateID());
        userToken.setTimestamp(String.valueOf(System.currentTimeMillis()));
        return userToken;
    }


    private UserToken2 fromUserAggregate(String userAggregateXML) {
        try {
            DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
            Document doc = documentBuilder.parse(new InputSource(new StringReader(userAggregateXML)));
            XPath xPath = XPathFactory.newInstance().newXPath();
            String uid = (String) xPath.evaluate("//UID", doc, XPathConstants.STRING);
            String userName = (String) xPath.evaluate("//identity/username", doc, XPathConstants.STRING);
            String firstName = (String) xPath.evaluate("//identity/firstname", doc, XPathConstants.STRING);
            String lastName = (String) xPath.evaluate("//identity/lastname", doc, XPathConstants.STRING);
            String email = (String) xPath.evaluate("//identity/email", doc, XPathConstants.STRING);
            String personRef = (String) xPath.evaluate("//identity/personRef", doc, XPathConstants.STRING);


            List<ApplicationRoleEntry> roleList = new ArrayList<>();
            NodeList applicationNodes = (NodeList) xPath.evaluate("/whydahuser/applications/application/appId", doc, XPathConstants.NODESET);
            for (int i = 1; i < applicationNodes.getLength() + 1; i++) {
                ApplicationRoleEntry role = new ApplicationRoleEntry();
                role.setApplicationid((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/appId", doc, XPathConstants.STRING));
                role.setApplicationname((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/applicationName", doc, XPathConstants.STRING));
                role.setOrganizationname((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/orgName", doc, XPathConstants.STRING));
                role.setRolename((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/roleName", doc, XPathConstants.STRING));
                role.setRolevalue((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/roleValue", doc, XPathConstants.STRING));
                roleList.add(role);
            }
            UserToken2 userToken = new UserToken2();
            userToken.setUid(uid);
            userToken.setUserName(userName);
            userToken.setFirstName(firstName);
            userToken.setLastName(lastName);
            userToken.setEmail(email);
            userToken.setPersonRef(personRef);
            userToken.setRoleList(roleList);

            String securityLevel = "1";
            userToken.setSecurityLevel(securityLevel);
            return userToken;
        } catch (Exception e) {
            logger.error("Error parsing userAggregateXML " + userAggregateXML, e);
            return null;
        }
    }

    private String extractIssuer(String appTokenXml) {
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(appTokenXml)));
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "/token/Url[1]/@template";
            XPathExpression xPathExpression = xPath.compile(expression);
            return xPathExpression.evaluate(doc);
        } catch (Exception e) {
            logger.error("Error when parsing appToken " + appTokenXml, e);
            return null;
        }
    }

    private String generateID() {
        return UUID.randomUUID().toString();
    }
}
