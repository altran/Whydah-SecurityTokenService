package net.whydah.token.user;

import com.google.inject.Singleton;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import net.whydah.token.application.ApplicationThreatResource;
import net.whydah.token.application.AuthenticatedApplicationRepository;
import net.whydah.token.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 03.11.14
 */
@Singleton
public class UserTokenFactory {
    static final String TOKEN_ISSUER = "/token/TOKEN_ISSUER/tokenverifier";

    private static final Logger log = LoggerFactory.getLogger(UserTokenFactory.class);
    private static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    //private static Random rand = new Random();
    private static String defcon = UserToken.DEFCON.DEFCON5.toString();
    private static String lifespanMs;
    //private String lifespanMs = String.valueOf(60 * 60 * rand.nextInt(1000));

    //ED: I do not like this dependency...
    private static AppConfig appConfig = new AppConfig();

    @Deprecated
    public UserTokenFactory() {
        this(appConfig.getProperty("DEFCON"));
    }

    public UserTokenFactory(String defcon) {
        this.defcon = defcon;
        //lifespanMs = String.valueOf(60 * 60 * rand.nextInt(100));
        lifespanMs = String.valueOf(14 * 24 * 60 * 60 * 1000); //14 days, reduce when refresh is implemented.
    }

    public static UserToken fromXml(String userTokenXml) {
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(userTokenXml)));
            XPath xPath = XPathFactory.newInstance().newXPath();

            String uid = (String) xPath.evaluate("/usertoken/uid", doc, XPathConstants.STRING);
            String personRef = (String) xPath.evaluate("/usertoken/personRef", doc, XPathConstants.STRING);
            String userName = (String) xPath.evaluate("/usertoken/username", doc, XPathConstants.STRING);
            String firstName = (String) xPath.evaluate("/usertoken/firstname", doc, XPathConstants.STRING);
            String lastName = (String) xPath.evaluate("/usertoken/lastname", doc, XPathConstants.STRING);
            String email = (String) xPath.evaluate("/usertoken/cellphone", doc, XPathConstants.STRING);
            String cellPhone = (String) xPath.evaluate("/usertoken/email", doc, XPathConstants.STRING);
            String securityLevel = (String) xPath.evaluate("/usertoken/securitylevel", doc, XPathConstants.STRING);

            String tokenId = (String) xPath.evaluate("/usertoken/@id", doc, XPathConstants.STRING);
            String timestamp = (String) xPath.evaluate("/usertoken/timestamp", doc, XPathConstants.STRING);
            String lastSeen = (String) xPath.evaluate("/usertoken/lastseen", doc, XPathConstants.STRING);

            String defcon = (String) xPath.evaluate("/usertoken/DEFCON", doc, XPathConstants.STRING);
            String lifespan = (String) xPath.evaluate("/usertoken/lifespan", doc, XPathConstants.STRING);
            String issuer = (String) xPath.evaluate("/usertoken/issuer", doc, XPathConstants.STRING);


            List<ApplicationRoleEntry> roleList = new ArrayList<>();
            NodeList applicationNodes = (NodeList) xPath.evaluate("//application", doc, XPathConstants.NODESET);
            for (int i = 0; i < applicationNodes.getLength(); i++) {
                Node appNode = applicationNodes.item(i);
                String appId = (String) xPath.evaluate("@ID", appNode, XPathConstants.STRING);
                String appName = (String) xPath.evaluate("./applicationName", appNode, XPathConstants.STRING);
                String organizationName = (String) xPath.evaluate("./organizationName", appNode, XPathConstants.STRING);
                NodeList roles = (NodeList) xPath.evaluate("./role", appNode, XPathConstants.NODESET);

                for (int k = 0; k < roles.getLength(); k ++) {
                    Node roleNode = roles.item(k);
                    String roleName = (String) xPath.evaluate("@name", roleNode, XPathConstants.STRING);
                    String roleValue = (String) xPath.evaluate("@value", roleNode, XPathConstants.STRING);

                    ApplicationRoleEntry role = new ApplicationRoleEntry();
                    role.setApplicationId(appId);
                    role.setApplicationName(appName);
                    role.setOrganizationName(organizationName);
                    role.setRoleName(roleName);
                    role.setRoleValue(roleValue);
                    roleList.add(role);
                }
            }


            UserToken userToken = new UserToken();
            userToken.setUid(uid);
            userToken.setUserName(userName);
            userToken.setFirstName(firstName);
            userToken.setLastName(lastName);
            userToken.setEmail(email);
            userToken.setCellPhone(cellPhone);
            userToken.setPersonRef(personRef);
            userToken.setSecurityLevel(securityLevel);
            userToken.setRoleList(roleList);

            userToken.setTokenid(tokenId);
            userToken.setTimestamp(timestamp);
            userToken.setLastSeen(lastSeen);
            userToken.setDefcon(defcon);
            userToken.setLifespan(lifespan);
            userToken.setIssuer(issuer);
            return userToken;
        } catch (Exception e) {
            log.error("Error parsing userTokenXml " + userTokenXml, e);
            return null;
        }
    }

    //String appTokenXml
    public  UserToken fromUserAggregate(String userAggregateXML) {
        UserToken userToken = parseUserAggregateXml(userAggregateXML);
        userToken.setTokenid(generateID());
        userToken.setTimestamp(String.valueOf(System.currentTimeMillis()));
        String securityLevel = "1"; //UserIdentity as source = securitylevel=0
        userToken.setSecurityLevel(securityLevel);

        userToken.setDefcon(defcon);
        //String issuer = extractIssuer(appTokenXml);
        userToken.setIssuer(TOKEN_ISSUER);
        userToken.setLifespan(lifespanMs);
        return userToken;
    }

    private UserToken parseUserAggregateXml(String userAggregateXML) {
        try {
            DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
            Document doc = documentBuilder.parse(new InputSource(new StringReader(userAggregateXML)));
            XPath xPath = XPathFactory.newInstance().newXPath();
            String uid = (String) xPath.evaluate("//identity/UID", doc, XPathConstants.STRING);
            String userName = (String) xPath.evaluate("//identity/username", doc, XPathConstants.STRING);
            String firstName = (String) xPath.evaluate("//identity/firstname", doc, XPathConstants.STRING);
            String lastName = (String) xPath.evaluate("//identity/lastname", doc, XPathConstants.STRING);
            String cellPhone = (String) xPath.evaluate("//identity/cellPhone", doc, XPathConstants.STRING);
            String email = (String) xPath.evaluate("//identity/email", doc, XPathConstants.STRING);
            String personRef = (String) xPath.evaluate("//identity/personRef", doc, XPathConstants.STRING);


            List<ApplicationRoleEntry> roleList = new ArrayList<>();
            NodeList applicationNodes = (NodeList) xPath.evaluate("/whydahuser/applications/application/appId", doc, XPathConstants.NODESET);
            for (int i = 1; i < applicationNodes.getLength() + 1; i++) {
                ApplicationRoleEntry role = new ApplicationRoleEntry();
                role.setApplicationId((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/appId", doc, XPathConstants.STRING));
                role.setApplicationName((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/applicationName", doc, XPathConstants.STRING));
                role.setOrganizationName((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/orgName", doc, XPathConstants.STRING));
                role.setRoleName((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/roleName", doc, XPathConstants.STRING));
                role.setRoleValue((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/roleValue", doc, XPathConstants.STRING));
                roleList.add(role);
            }
            UserToken userToken = new UserToken();
            userToken.setUid(uid);
            userToken.setUserName(userName);
            userToken.setFirstName(firstName);
            userToken.setLastName(lastName);
            userToken.setEmail(email);
            userToken.setCellPhone(cellPhone);
            userToken.setPersonRef(personRef);
            userToken.setRoleList(roleList);
            userToken.setDefcon(ApplicationThreatResource.getDEFCON());
            return userToken;
        } catch (Exception e) {
            log.error("Error parsing userAggregateXML " + userAggregateXML, e);
            return null;
        }
    }


    private static String generateID() {
        return UUID.randomUUID().toString();
    }

    public static void setDefcon(String defcon) {
        UserTokenFactory.defcon = defcon;
    }

    public static boolean shouldReturnFullUserToken(String applicationID) {
        String[] applicationIDs = AppConfig.getFullTokenApplications().split(",");
        for (int i = 0; i < applicationIDs.length; i++){
            if (applicationIDs[i].equalsIgnoreCase(applicationID)) {
                log.info("shouldReturnFullUserToken=true");
                return true;
            }
        }
        log.trace("shouldReturnFullUserToken=false");
        return false;
    }


    public static UserToken fromUserIdentityJson(String userIdentityJSON) {
        UserToken userToken = parseUserIdentityJson(userIdentityJSON);
        userToken.setTokenid(generateID());
        userToken.setTimestamp(String.valueOf(System.currentTimeMillis()));
        String securityLevel = "1";
        userToken.setSecurityLevel(securityLevel);
        return userToken;
    }

    private static UserToken parseUserIdentityJson(String userIdentityJSON) {
        try {
            DocumentBuilder e = dbf.newDocumentBuilder();
            String uid = getFieldFromUserAggregateJson("$.identity.uid", userIdentityJSON);
            String userName = getFieldFromUserAggregateJson("$.identity.username", userIdentityJSON);
            String firstName = getFieldFromUserAggregateJson("$.identity.firstName", userIdentityJSON);
            String lastName = getFieldFromUserAggregateJson("$.identity.lastName", userIdentityJSON);
            String email = getFieldFromUserAggregateJson("$.identity.email", userIdentityJSON);
            String cellPhone = getFieldFromUserAggregateJson("$.identity.cellPhone", userIdentityJSON);
            String personRef = getFieldFromUserAggregateJson("$.identity.personRef", userIdentityJSON);
            UserToken userToken = new UserToken();
            userToken.setUid(uid);
            userToken.setUserName(userName);
            userToken.setFirstName(firstName);
            userToken.setLastName(lastName);
            userToken.setEmail(email);
            userToken.setPersonRef(personRef);
            userToken.setCellPhone(cellPhone);
            return userToken;
        } catch (Exception var10) {
            log.error("Error parsing userAggregateJSON " + userIdentityJSON, var10);
        }

        /**
         * Fallbacl parsing to handle old, non conformant useridentyty json format
         */
        try {
            DocumentBuilder e = dbf.newDocumentBuilder();
            String uid = getFieldFromUserAggregateJson("$..uid", userIdentityJSON);
            String userName = getFieldFromUserAggregateJson("$..username", userIdentityJSON);
            String firstName = getFieldFromUserAggregateJson("$..firstName", userIdentityJSON);
            String lastName = getFieldFromUserAggregateJson("$..lastName", userIdentityJSON);
            String email = getFieldFromUserAggregateJson("$..email", userIdentityJSON);
            String cellPhone = getFieldFromUserAggregateJson("$..cellPhone", userIdentityJSON);
            String personRef = getFieldFromUserAggregateJson("$..personRef", userIdentityJSON);
            UserToken userToken = new UserToken();
            userToken.setUid(uid);
            userToken.setUserName(userName);
            userToken.setFirstName(firstName);
            userToken.setLastName(lastName);
            userToken.setEmail(email);
            userToken.setPersonRef(personRef);
            userToken.setCellPhone(cellPhone);
            return userToken;
        } catch (Exception var10) {
            log.error("Error parsing userAggregateJSON " + userIdentityJSON, var10);
            return null;
        }
    }

    //String appTokenXml
    public static UserToken fromUserAggregateJson(String userAggregateXML) {
        UserToken userToken = parseUserAggregateJson(userAggregateXML);
        userToken.setTokenid(generateID());
        userToken.setTimestamp(String.valueOf(System.currentTimeMillis()));
        String securityLevel = "1"; //UserIdentity as source = securitylevel=0
        userToken.setSecurityLevel(securityLevel);

        userToken.setDefcon(defcon);
        //String issuer = extractIssuer(appTokenXml);
        userToken.setIssuer(TOKEN_ISSUER);
        userToken.setLifespan(lifespanMs);
        return userToken;
    }

    private static UserToken parseUserAggregateJson(String userAggregateJSON) {
        try {
            DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
            String uid = getFieldFromUserAggregateJson("$..uid", userAggregateJSON);
            String userName = getFieldFromUserAggregateJson("$..username",userAggregateJSON);
            String firstName = getFieldFromUserAggregateJson("$..firstName",userAggregateJSON);
            String lastName = getFieldFromUserAggregateJson("$..lastName",userAggregateJSON);
            String email = getFieldFromUserAggregateJson("$..email",userAggregateJSON);
            String cellPhone = getFieldFromUserAggregateJson("$..cellPhone",userAggregateJSON);
            String personRef = getFieldFromUserAggregateJson("$..personRef",userAggregateJSON);

            UserToken userToken = new UserToken();
            userToken.setUid(uid);
            userToken.setUserName(userName);
            userToken.setFirstName(firstName);
            userToken.setLastName(lastName);
            userToken.setEmail(email);
            userToken.setPersonRef(personRef);
            userToken.setCellPhone(cellPhone);
//            userToken.setRoleList(roleList);
            return userToken;
        } catch (Exception e) {
            log.error("Error parsing userAggregateJSON " + userAggregateJSON, e);
            return null;
        }
    }

    public static String getFieldFromUserAggregateJson(String expression, String jsonString ) throws PathNotFoundException {
        //String expression = "$.identity.uid";
        String value = "";
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(jsonString);
        String result= JsonPath.read(document, expression);
        value=result.toString();

        return value;
    }


    public static UserToken getFilteredUserToken(String applicationTokenID,UserToken userToken) {

        String myappid = AuthenticatedApplicationRepository.getApplicationIdFromApplicationTokenID(applicationTokenID);
        log.info("getFilteredUserToken - found appid={}",myappid);
        if (shouldReturnFullUserToken(myappid)){
            log.info("getFilteredUserToken - no filtering");
            return userToken;
        } else {
            List<ApplicationRoleEntry> origRoleList = userToken.getRoleList();
            List<ApplicationRoleEntry> roleList = new LinkedList<>();
            log.info("getFilteredUserToken - filtering active");

            for (int i=0;i<origRoleList.size();i++){
                ApplicationRoleEntry are = origRoleList.get(i);
                if (are.getApplicationId().equalsIgnoreCase(myappid)){
                    roleList.add(are);
                }
            }
            userToken.setRoleList(roleList);
            return userToken;
        }
    }

    public static boolean verifyApplicationToken(String applicationtokenid, String applicationtokenXml) {
        // TODO - possibly implement check if apptokenXml is identical from source to repo
        boolean validAppToken = false;
        if (applicationtokenid != null) {
            return AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid);
        } else {
            log.warn("verifyApplicationToken - not expecting null values applicationtokenid {}, applicationtokenXml {}", applicationtokenid, applicationtokenXml);
            return false;
        }
    }

}
