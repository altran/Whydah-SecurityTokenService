package net.whydah.token.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Totto
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a>
 */
public class UserToken implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(UserToken.class);

    // nextInt is normally exclusive of the top value,
    // so add 1 to make it inclusive

    private String tokenid;

    //From UIB
    private String uid;
    private String personRef;
    private String userName;
    private String firstName;
    private String lastName;
    private String email;
    private String timestamp;

    private static String defcon;
    private String securityLevel;
    private String lifespan;
    private String issuer;


    private String ns2link;
    private List<ApplicationRoleEntry> roleList;
    //Ignored properties: cellPhone,


    public UserToken() {
        this.timestamp = Long.toString(System.currentTimeMillis());
        this.roleList = new LinkedList<>();
    }

    /*
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
                role.setApplicationId((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/appId", doc, XPathConstants.STRING));
                role.setApplicationName((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/applicationName", doc, XPathConstants.STRING));
                role.setOrganizationName((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/orgName", doc, XPathConstants.STRING));
                role.setRoleName((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/roleName", doc, XPathConstants.STRING));
                role.setRoleValue((String) xPath.evaluate("/whydahuser/applications/application[" + i + "]/roleValue", doc, XPathConstants.STRING));
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
    */

    public boolean isValid() {
        logger.trace("usertoken - isValid  timestamp={}  lifespan={}", timestamp, lifespan);
        if (timestamp == null || lifespan == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        long timeout = Long.parseLong(timestamp) + Long.parseLong(lifespan);
        logger.debug("usertoken - isValid timeout={} > now={}", timeout, now);
        boolean stillValid = (timeout > now);
        if (!stillValid) {
            logger.info ("SecurityToken timed out.");
        }
        return stillValid;
    }

    public UserToken copy() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            byte[] obj = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(obj);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (UserToken) ois.readObject();
        } catch (Exception e) {
            logger.error("Error copying UserToken", e);
        }
        return null;
    }

    //Used by usertoken.ftl
    public String getMD5() {
        String md5base = null2empty(uid) + null2empty(personRef) + null2empty(tokenid) + null2empty(timestamp)
                + null2empty(firstName) + null2empty(lastName) + null2empty(email) + null2empty(securityLevel) + null2empty(issuer);
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


    public void addApplicationRoleEntry(ApplicationRoleEntry role) {
        roleList.add(role);
    }

    public void setTokenid(String tokenid) {
        this.tokenid = tokenid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }
    public void setPersonRef(String personRef) {
        this.personRef = personRef;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    public void setSecurityLevel(String securityLevel) {
        this.securityLevel = securityLevel;
    }
    public void setLifespan(String lifespan) {
        this.lifespan = lifespan;
    }
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    public void setRoleList(List<ApplicationRoleEntry> roleList) {
        this.roleList = roleList;
    }
    public static void setDefcon(String defcon) {
        UserToken.defcon = defcon;
    }

    public void setNs2link(String ns2link) {
        this.ns2link = ns2link;
    }


    public String getTokenid() {
        return tokenid;
    }
    public String getUid() {
        return uid;
    }
    public String getPersonRef() {
        return personRef;
    }
    public String getUserName() {
        return userName;
    }
    public String getFirstName() {
        return firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public String getEmail() {
        return email;
    }
    public String getTimestamp() {
        return timestamp;
    }
    public String getSecurityLevel() {
        return securityLevel;
    }
    public String getLifespan() {
        return lifespan;
    }
    public String getIssuer() {
        return issuer;
    }
    public List<ApplicationRoleEntry> getRoleList() {
        return roleList;
    }
    public static String getDefcon() {
        return defcon;
    }
    public String getNs2link() {
        return ns2link;
    }

    @Override
    public String toString() {
        return "UserToken{" +
                "tokenid='" + tokenid + '\'' +
                ", uid='" + uid + '\'' +
                ", personRef='" + personRef + '\'' +
                ", userName='" + userName + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", securityLevel='" + securityLevel + '\'' +
                ", lifespan='" + lifespan + '\'' +
                ", issuer='" + issuer + '\'' +
                ", roleList.size=" + roleList.size() +
                '}';
    }
}