package net.whydah.token.application;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.token.config.AppConfig;
import net.whydah.token.config.ApplicationModelHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.Map;


public class AuthenticatedApplicationRepository {
    private final static Logger log = LoggerFactory.getLogger(AuthenticatedApplicationRepository.class);

    public static int DEFAULT_SESSION_EXTENSION_TIME_IN_SECONDS = 240;

    private static final Map<String, ApplicationToken> applicationTokenMap;

    static {
        AppConfig appConfig = new AppConfig();
        String xmlFileName = System.getProperty("hazelcast.config");
        log.info("Loading hazelcast configuration from :" + xmlFileName);
        Config hazelcastConfig = new Config();
        if (xmlFileName != null && xmlFileName.length() > 10) {
            try {
                hazelcastConfig = new XmlConfigBuilder(xmlFileName).build();
                log.info("Loading hazelcast configuration from :" + xmlFileName);
            } catch (FileNotFoundException notFound) {
                log.error("Error - not able to load hazelcast.xml configuration.  Using embedded as fallback");
            }
        }
        hazelcastConfig.setProperty("hazelcast.logging.type", "slf4j");
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig);
        applicationTokenMap = hazelcastInstance.getMap(appConfig.getProperty("gridprefix") + "_authenticated_applicationtokens");
        log.info("Connecting to map {}", appConfig.getProperty("gridprefix") + "_authenticated_applicationtokens");
    }


    public static void addApplicationToken(ApplicationToken token) {
        applicationTokenMap.put(token.getApplicationTokenId(), token);
    }

    public static ApplicationToken getApplicationToken(String applicationtokenid) {
        return applicationTokenMap.get(applicationtokenid);
    }


    public static boolean verifyApplicationToken(ApplicationToken token) {
        return token.equals(applicationTokenMap.get(token.getApplicationTokenId()));
    }

    public static boolean verifyApplicationTokenId(String applicationtokenid) {
        return applicationTokenMap.get(applicationtokenid) != null;
    }

    public static ApplicationToken renewApplicationTokenId(String applicationtokenid) {
        ApplicationToken temp = applicationTokenMap.get(applicationtokenid);
        temp.setExpires(updateExpires(temp.getExpires(), temp.getApplicationID()));
        applicationTokenMap.put(temp.getApplicationTokenId(), temp);
        return temp;
    }

    public static boolean verifyApplicationTokenXml(String applicationtokenXml) {
        try {
            String applicationID = getApplocationTokenIdFromApplicationTokenXML(applicationtokenXml);
            return applicationTokenMap.get(applicationID) != null;
        } catch (StringIndexOutOfBoundsException e) {
            return false;
        }
    }

    public static String getApplicationIdFromApplicationTokenID(String applicationtokenid) {
        ApplicationToken at = applicationTokenMap.get(applicationtokenid);
        if (at!=null) {
            return at.getApplicationID();
        }
        log.error("getApplicationIdFromApplicationTokenID - Unable to find applicationID for applicationtokenId=" + applicationtokenid);
        return "";
    }

    public static String getApplocationTokenIdFromApplicationTokenXML(String applicationTokenXML) {
        String applicationTokenId = "";
        if (applicationTokenXML == null) {
            log.debug("applicationTokenXML was empty, so returning empty applicationTokenId.");
        } else {
            String expression = "/applicationtoken/params/applicationtokenID[1]";
            applicationTokenId = findValue(applicationTokenXML, expression);
        }
        return applicationTokenId;
    }


    private static String updateExpires(String oldExpiry, String applicationID) {
        String applicationMaxSessionTime = ApplicationModelHelper.getParameterForApplication(ApplicationModelHelper.maxSessionTimeoutSeconds, applicationID);
        log.info("maxSessionTimeoutSeconds: {} for applicationID: {}", applicationMaxSessionTime, applicationID);
        if (applicationMaxSessionTime != null && (Long.parseLong(applicationMaxSessionTime) > 0)) {
            // Set to application configured maxSessionTimeoutSeconds if found and shave off 10 seconds
            return String.valueOf(System.currentTimeMillis() + Long.parseLong(applicationMaxSessionTime) * 1000 - 10 * 1000);

        }
        return String.valueOf(System.currentTimeMillis() + DEFAULT_SESSION_EXTENSION_TIME_IN_SECONDS * 1000);
    }


    private static String findValue(String xmlString,  String expression) {
        String value = "";
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(xmlString)));
            XPath xPath = XPathFactory.newInstance().newXPath();


            XPathExpression xPathExpression = xPath.compile(expression);
            value = xPathExpression.evaluate(doc);
        } catch (Exception e) {
            log.warn("Failed to parse xml. Expression {}, xml {}, ", expression, xmlString, e);
        }
        return value;
    }


}
