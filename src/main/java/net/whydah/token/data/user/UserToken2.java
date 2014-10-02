package net.whydah.token.data.user;

import net.whydah.token.config.AppConfig;
import net.whydah.token.data.application.ApplicationData;
import net.whydah.token.data.helper.CompanyRoles;
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
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.Serializable;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;

public class UserToken2 implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(UserToken2.class);
    private final static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    Random rand = new Random();

    // nextInt is normally exclusive of the top value,
    // so add 1 to make it inclusive

    private String tokenid;

    public String getPersonRef() {
        return personRef;
    }

    public void setPersonRef(String personRef) {
        this.personRef = personRef;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(String securityLevel) {
        this.securityLevel = securityLevel;
    }

    public String getLifespan() {
        return lifespan;
    }

    public void setLifespan(String lifespan) {
        this.lifespan = lifespan;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public static String getDefcon() {
        return defcon;
    }

    public static void setDefcon(String defcon) {
        UserToken2.defcon = defcon;
    }

    public List<ApplicationRoleEntry> getRoleList() {
        return roleList;
    }

    public void setRoleList(List<ApplicationRoleEntry> roleList) {
        this.roleList = roleList;
    }

    public String getUid() {
        return uid;
    }


    public String getTokenid() {
        return tokenid;
    }

    public void setTokenid(String tokenid) {
        this.tokenid = tokenid;
    }

    private String uid;
    private String personRef;
    private String userName;
    private String firstName;
    private String lastName;
    private String email;
    private String timestamp = Long.toString(System.currentTimeMillis());
    private String securityLevel = "0";
    private String lifespan = String.valueOf(60 * 60 * rand.nextInt(1000));
    private String issuer = "/token/issuer/tokenverifier";
    private List<ApplicationRoleEntry> roleList;

    private static String defcon = "0";
    private static AppConfig appConfig = new AppConfig();


    public UserToken2() {
        defcon = appConfig.getProperty("DEFCON");
        lifespan = String.valueOf(60 * 60 * rand.nextInt(100));
        roleList = new LinkedList();
    }

    public static UserToken2 createUserTokenFromUserTokenXML(String userTokenXml) {
        defcon = appConfig.getProperty("DEFCON");
        UserToken2 userToken = new UserToken2();
        return userToken;
    }

    private void parseAndUpdatefromUserToken(String userTokenXml) {
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(userTokenXml)));
            XPath xPath = XPathFactory.newInstance().newXPath();
            tokenid = (String) xPath.evaluate("/usertoken/@id", doc, XPathConstants.STRING);
            uid = (String) xPath.evaluate("/usertoken/uid", doc, XPathConstants.STRING);
            personRef = (String) xPath.evaluate("/usertoken/personRef", doc, XPathConstants.STRING);
            userName = (String) xPath.evaluate("/usertoken/username", doc, XPathConstants.STRING);
            firstName = (String) xPath.evaluate("/usertoken/firstname", doc, XPathConstants.STRING);
            lastName = (String) xPath.evaluate("/usertoken/lastname", doc, XPathConstants.STRING);
            email = (String) xPath.evaluate("/usertoken/email", doc, XPathConstants.STRING);
            timestamp = (String) xPath.evaluate("/usertoken/timestamp", doc, XPathConstants.STRING);
            defcon = (String) xPath.evaluate("/usertoken/DEFCON", doc, XPathConstants.STRING);
            securityLevel = (String) xPath.evaluate("/usertoken/securitylevel", doc, XPathConstants.STRING);
            lifespan = (String) xPath.evaluate("/usertoken/lifespan", doc, XPathConstants.STRING);
            issuer = (String) xPath.evaluate("/usertoken/issuer", doc, XPathConstants.STRING);
        } catch (Exception e) {
            logger.error("Error parsing userTokenXml " + userTokenXml, e);
        }
    }

    private void parseAndUpdatefromAppToken(String appTokenXml) {
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(appTokenXml)));
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "/token/Url[1]/@template";
            XPathExpression xPathExpression = xPath.compile(expression);
            issuer = xPathExpression.evaluate(doc);
        } catch (Exception e) {
            logger.error("Error when parsing appToken " + appTokenXml, e);
        }
    }

    private void parseAndUpdateFromUserAggregate(String userAggregateXML) {
        try {
            DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
            Document doc = documentBuilder.parse(new InputSource(new StringReader(userAggregateXML)));
            XPath xPath = XPathFactory.newInstance().newXPath();
            uid = (String) xPath.evaluate("//UID", doc, XPathConstants.STRING);
            userName = (String) xPath.evaluate("//identity/username", doc, XPathConstants.STRING);
            firstName = (String) xPath.evaluate("//identity/firstname", doc, XPathConstants.STRING);
            lastName = (String) xPath.evaluate("//identity/lastname", doc, XPathConstants.STRING);
            email = (String) xPath.evaluate("//identity/email", doc, XPathConstants.STRING);
            personRef = (String) xPath.evaluate("//identity/personRef", doc, XPathConstants.STRING);
            securityLevel = "1";
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
        } catch (Exception e) {
            logger.error("Error parsing userAggregateXML " + userAggregateXML, e);
        }
    }

    public static UserToken2 createUserTokenFromUserAggregate(String appTokenXml, String userAggregateXML) {
        defcon = appConfig.getProperty("DEFCON");
        UserToken2 userToken = new UserToken2();
        userToken.parseAndUpdatefromAppToken(appTokenXml);
        userToken.parseAndUpdateFromUserAggregate(userAggregateXML);
        userToken.tokenid = userToken.generateID();
        userToken.timestamp = String.valueOf(System.currentTimeMillis());
        return userToken;
    }

    private String generateID() {
        return UUID.randomUUID().toString();
    }

    public String getMD5() {
        String md5base = null2empty(uid) + null2empty(personRef) + null2empty(tokenid) + null2empty(timestamp)
                + null2empty(firstName) + null2empty(lastName) + null2empty(email) + securityLevel + issuer;
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(md5base.getBytes("UTF-8"));
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            return bigInt.toString(16);
        } catch (Exception e) {
            logger.error("Error creating MD5 hash, returning empty string. userToken: " + toString(), e);
            return "";
        }
    }

    private String null2empty(String value) {
        return value != null ? value : "";
    }


}