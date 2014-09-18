package net.whydah.token.data.user;

import com.google.inject.Inject;
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
import javax.xml.xpath.*;
import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class UserToken implements Serializable{
    private static final Logger logger = LoggerFactory.getLogger(UserToken.class);
    private final static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    private String tokenid;
    private String uid;
    private String personRef;
    private String userName;
    private String firstName;
    private String lastName;
    private String email;
    private String timestamp;
    private String securityLevel = "0";
    private String lifespan = String.valueOf(60 * 60 * 1000); // 1 time
    private String issuer = "/token/issuer/tokenverifier";
    private Map<String, ApplicationData> applicationCompanyRoleValueMap = new HashMap<>();

    private static String defcon = "0";
    private static AppConfig appConfig = new AppConfig();


    public UserToken() {
        defcon = appConfig.getProperty("DEFCON");
    }

    public static UserToken createUserTokenFromUserTokenXML(String userTokenXml) {
        defcon = appConfig.getProperty("DEFCON");
        UserToken userToken = new UserToken();
        userToken.parseAndUpdatefromUserToken(userTokenXml);
        return userToken;
    }

    public static UserToken createUserTokenFromUserAggregate(String appTokenXml, String userAggregateXML) {
        defcon = appConfig.getProperty("DEFCON");
        UserToken userToken = new UserToken();
        userToken.parseAndUpdatefromAppToken(appTokenXml);
        userToken.parseAndUpdatefromUserAggregate(userAggregateXML);
        userToken.tokenid = userToken.generateID();
        userToken.timestamp = String.valueOf(System.currentTimeMillis());
        return userToken;
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

    private void parseAndUpdatefromUserAggregate(String userAggregateXML) {
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
            NodeList applicationNodes = (NodeList) xPath.evaluate("//application", doc, XPathConstants.NODESET);
            for(int i=0; i<applicationNodes.getLength(); i++) {
                Node appNode = applicationNodes.item(i);
                NodeList children = appNode.getChildNodes();
                HashMap<String, String> values = getAppValues(children);
                putApplicationCompanyRoleValue(values.get("appId"), values.get("applicationName"), values.get("orgName"), values.get("roleName"), values.get("roleValue"));
            }
        } catch (Exception e) {
            logger.error("Error parsing userAggregateXML " + userAggregateXML, e);
        }
    }

    private HashMap<String, String> getAppValues(NodeList children) {
        HashMap<String, String> values = new HashMap<String, String>();
        for(int j=0; j<children.getLength(); j++) {
            Node node = children.item(j);
            if(node.getNodeType() == Node.ELEMENT_NODE) {
                values.put(node.getNodeName(), node.getTextContent());
            }
        }
        return values;
    }

    private void parseAndUpdatefromUserToken(String userTokenXml) {
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(userTokenXml)));
            XPath xPath = XPathFactory.newInstance().newXPath();
            tokenid = (String) xPath.evaluate("/token/@id", doc, XPathConstants.STRING);
            uid = (String) xPath.evaluate("/token/uid", doc, XPathConstants.STRING);
            personRef = (String) xPath.evaluate("/token/personRef", doc, XPathConstants.STRING);
            userName = (String) xPath.evaluate("/token/username", doc, XPathConstants.STRING);
            firstName = (String) xPath.evaluate("/token/firstname", doc, XPathConstants.STRING);
            lastName = (String) xPath.evaluate("/token/lastname", doc, XPathConstants.STRING);
            email = (String) xPath.evaluate("//token/email", doc, XPathConstants.STRING);
            timestamp = (String) xPath.evaluate("/token/timestamp", doc, XPathConstants.STRING);
            defcon = (String) xPath.evaluate("/token/DEFCON", doc, XPathConstants.STRING);
            securityLevel = (String) xPath.evaluate("/token/securitylevel", doc, XPathConstants.STRING);
            lifespan = (String) xPath.evaluate("/token/lifespan", doc, XPathConstants.STRING);
            issuer = (String) xPath.evaluate("/token/issuer", doc, XPathConstants.STRING);
            applicationCompanyRoleValueMap = new HashMap<>();
            parseAndUpdateRolemapFromUserToken(doc);
        } catch (Exception e) {
            logger.error("Error parsing userTokenXml " + userTokenXml, e);
        }
    }

    private void parseAndUpdateRolemapFromUserToken(Document doc) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            NodeList applicationNodes = (NodeList) xPath.evaluate("//application", doc, XPathConstants.NODESET);
            for(int i=0; i<applicationNodes.getLength(); i++) {
                addApplicationroles(applicationNodes.item(i));
            }
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    private void addApplicationroles(Node appNode) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        String appId = (String) xPath.evaluate("@ID", appNode, XPathConstants.STRING);
        String appName = (String) xPath.evaluate("./applicationName", appNode, XPathConstants.STRING);
        String organizationName = (String) xPath.evaluate("./organizationName", appNode, XPathConstants.STRING);
            NodeList roles = (NodeList) xPath.evaluate("./role", appNode, XPathConstants.NODESET);
            for(int k=0; k<roles.getLength(); k ++) {
                Node roleNode = roles.item(k);
                String roleName = (String) xPath.evaluate("@name", roleNode, XPathConstants.STRING);
                String roleValue = (String) xPath.evaluate("@value", roleNode, XPathConstants.STRING);
                putApplicationCompanyRoleValue(appId, appName, organizationName, roleName, roleValue);
            }

    }


    public String getMD5() {
        String md5base = null2empty(uid) + null2empty(personRef) + null2empty(tokenid) + null2empty(timestamp)
                + null2empty(firstName) + null2empty(lastName) + null2empty(email) + securityLevel + issuer;
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(md5base.getBytes("UTF-8"));
            for (ApplicationData app : applicationCompanyRoleValueMap.values()) {
                m.update(app.getApplicationID().getBytes("UTF-8"));
                for (CompanyRoles companyRoles : app.getCompaniesAndRolesMap().values()) {
                    // TODO: get Company Name instead
                    // m.update(companyRoles.getCompanyNumber().getBytes("UTF-8"));
                    Map<String,String> roles = companyRoles.getRoleMap();
                    for (Map.Entry<String, String> roleEntry : roles.entrySet()) {
                        m.update(roleEntry.getKey().getBytes("UTF-8"));
                        m.update(roleEntry.getValue().getBytes("UTF-8"));
                    }
                }
            }
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1,digest);
            return bigInt.toString(16);
        } catch (Exception e) {
            logger.error("Error creating MD5 hash, returning empty string. userToken: " + toString(), e);
            return "";
        }
    }


    private String null2empty(String value) {
        return value != null ? value : "";
    }

    public void putApplicationCompanyRoleValue(String p_application_ID, String p_application_Name,  String p_company_name, String p_role, String p_value) {
        if (applicationCompanyRoleValueMap.containsKey(p_application_ID)) {
            ApplicationData application = applicationCompanyRoleValueMap.get(p_application_ID);
            CompanyRoles company = application.getCompaniesAndRolesMap().get(p_company_name);
            if (company != null) {  // Application and company exists, just update the rolemap
                company.getRoleMap().put(p_role, p_value);
            } else {
                company = new CompanyRoles();
                company.setCompanyName(p_company_name);
                Map<String, String> rolemap = new HashMap<String, String>();
                rolemap.put(p_role, p_value);
                company.setRoleMap(rolemap);
                application.addCompanyWithRoles(company.getCompanyNumber(), company);
                applicationCompanyRoleValueMap.put(application.getApplicationID(), application);
            }
            // Add or update existing application
        } else {
            ApplicationData application = new ApplicationData();
            application.setApplicationID(p_application_ID);
            application.setApplicationName(p_application_Name);
            CompanyRoles company = new CompanyRoles();
            company.setCompanyName(p_company_name);
            Map<String,String> rolemap = new HashMap<String, String>();
            rolemap.put(p_role, p_value);
            company.setRoleMap(rolemap);
            application.addCompanyWithRoles(company.getCompanyNumber(), company);
            applicationCompanyRoleValueMap.put(application.getApplicationID(), application);
        }
    }

    public boolean isValid() {
        if (timestamp == null || lifespan == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        long timeout = Long.parseLong(timestamp) + Long.parseLong(lifespan);
        boolean stillValid = (timeout > now);
        if (!stillValid) {
            logger.info ("SecurityToken timed out.");
        }
        return stillValid;
    }

    private String generateID() {
        return UUID.randomUUID().toString();
    }


    public String toXML(String applicationID) {
        return "UserToken{" +
                "tokenid='" + tokenid + '\'' +
                ", uid='" + uid + '\'' +
                ", personRef='" + personRef + '\'' +
                ", userName='" + userName + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", DEFCON='" + defcon + '\'' +
                ", securityLevel='" + securityLevel + '\'' +
                ", lifespan='" + lifespan + '\'' +
                ", issuer='" + issuer + '\'' +
                ", applicationCompanyRoleValueMap=" + getApplicationCompanyRoleMap(applicationID) +
                '}';

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
                ", DEFCON =" + defcon + '\'' +
                ", securityLevel='" + securityLevel + '\'' +
                ", lifespan='" + lifespan + '\'' +
                ", issuer='" + issuer + '\'' +
                ", applicationCompanyRoleValueMap=" + getApplicationCompanyRoleMap(null) +
                '}';
    }

    private Map getApplicationCompanyRoleMap(String applicationID) {
        if (applicationID == null) {
            return applicationCompanyRoleValueMap;
        }
        if (applicationCompanyRoleValueMap.containsKey(applicationID)) {
            Map<String, ApplicationData> returMap = new HashMap<>();
            returMap.put(applicationID, applicationCompanyRoleValueMap.get(applicationID));
            return returMap;
        }
        return applicationCompanyRoleValueMap;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserToken userToken = (UserToken) o;

        if (applicationCompanyRoleValueMap != null ? !applicationCompanyRoleValueMap.equals(userToken.applicationCompanyRoleValueMap) : userToken.applicationCompanyRoleValueMap != null)
            return false;
        if (lastName != null ? !lastName.equals(userToken.lastName) : userToken.lastName != null) return false;
        if (firstName != null ? !firstName.equals(userToken.firstName) : userToken.firstName != null) return false;
        if (userName != null ? !userName.equals(userToken.userName) : userToken.userName != null) return false;
        if (email != null ? !email.equals(userToken.email) : userToken.email != null) return false;
        if (uid != null ? !uid.equals(userToken.uid) : userToken.uid != null) return false;
        if (issuer != null ? !issuer.equals(userToken.issuer) : userToken.issuer != null) return false;
        if (lifespan != null ? !lifespan.equals(userToken.lifespan) : userToken.lifespan != null) return false;
        if (personRef != null ? !personRef.equals(userToken.personRef) : userToken.personRef != null) return false;
        if (securityLevel != null ? !securityLevel.equals(userToken.securityLevel) : userToken.securityLevel != null)
            return false;
        if (timestamp != null ? !timestamp.equals(userToken.timestamp) : userToken.timestamp != null) return false;
        if (tokenid != null ? !tokenid.equals(userToken.tokenid) : userToken.tokenid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = tokenid != null ? tokenid.hashCode() : 0;
        int salt = 31;
        result = salt * result + (uid != null ? uid.hashCode() : 0);
        result = salt * result + (personRef != null ? personRef.hashCode() : 0);
        result = salt * result + (userName != null ? userName.hashCode() : 0);
        result = salt * result + (firstName != null ? firstName.hashCode() : 0);
        result = salt * result + (lastName != null ? lastName.hashCode() : 0);
        result = salt * result + (email != null ? email.hashCode() : 0);
        result = salt * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = salt * result + (securityLevel != null ? securityLevel.hashCode() : 0);
        result = salt * result + (lifespan != null ? lifespan.hashCode() : 0);
        result = salt * result + (issuer != null ? issuer.hashCode() : 0);
        result = salt * result + (applicationCompanyRoleValueMap != null ? applicationCompanyRoleValueMap.hashCode() : 0);
        return result;
    }

    public UserToken copy() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            byte[] obj = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(obj);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (UserToken)ois.readObject();
        } catch (Exception e) {
            logger.error("Error copying UserToken", e);
        }
        return null;
    }


    public String getUid() {
        return uid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }
    public void setTokenid(String tokenid) {
        this.tokenid = tokenid;
    }
    public String getTokenid() {
        return this.tokenid;
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

    public void setDefcon(String defcon) {
        this.defcon = defcon;
    }


    public String getSecurityLevel() {
        return securityLevel;
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
    public String getLifespan() {
        return lifespan;
    }
    public String getIssuer() {
        return issuer;
    }

    public String getDefcon() {
        return defcon;
    }
    public Collection<ApplicationData> getApplications() {
        return applicationCompanyRoleValueMap.values();
    }

}
