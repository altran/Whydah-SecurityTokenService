package net.whydah.token.application;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import net.whydah.sso.application.mappers.ApplicationCredentialMapper;
import net.whydah.sso.application.mappers.ApplicationTokenMapper;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.session.baseclasses.ApplicationModelUtil;
import net.whydah.sso.session.baseclasses.ExchangeableKey;
import net.whydah.token.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.crypto.spec.IvParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;


public class AuthenticatedApplicationTokenRepository {
    private final static Logger log = LoggerFactory.getLogger(AuthenticatedApplicationTokenRepository.class);

    public static long DEFAULT_SESSION_EXTENSION_TIME_IN_SECONDS = 120; //One minute = 60 seconds //2400;
    private static AppConfig appConfig = new AppConfig();
    private static String stsApplicationTokenID = "";
    private static ApplicationToken myToken;

    private static final Map<String, ApplicationToken> applicationTokenMap;
    private static final Map<String, String> applicationKeyMap;

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
        applicationTokenMap = hazelcastInstance.getMap(appConfig.getProperty("gridprefix") + "authenticated_applicationtokens");
        applicationKeyMap = hazelcastInstance.getMap(appConfig.getProperty("gridprefix") + "_applicationkeys");
        String applicationDefaultTimeout = System.getProperty("application.session.timeout");
        if (applicationDefaultTimeout != null && (Integer.parseInt(applicationDefaultTimeout) > 0)) {
            log.info("Updated DEFAULT_SESSION_EXTENSION_TIME_IN_SECONDS to " + applicationDefaultTimeout);
            DEFAULT_SESSION_EXTENSION_TIME_IN_SECONDS = Integer.parseInt(applicationDefaultTimeout);
        }
        log.info("Connecting to map {} - map size: {}", appConfig.getProperty("gridprefix") + "authenticated_applicationtokens", getMapSize());
    }


    public static void addApplicationToken(ApplicationToken applicationToken) {
        long remainingSecs = (Long.parseLong(applicationToken.getExpires()) - System.currentTimeMillis()) / 1000;
        log.debug("Added {} expires in {} seconds", applicationToken.getApplicationName(), remainingSecs);
        applicationTokenMap.put(applicationToken.getApplicationTokenId(), applicationToken);
        if (applicationToken.getApplicationID().equalsIgnoreCase("9999")) {
            if (applicationKeyMap.containsKey(applicationToken.getApplicationTokenId())) {
                // Maybe update key here...
            } else {
                // Bootstrap key initialization
                ExchangeableKey applicationKey = new ExchangeableKey(applicationToken.getApplicationTokenId().getBytes(), new IvParameterSpec("01234567890ABCDE".getBytes()));
                applicationKeyMap.put(applicationToken.getApplicationTokenId(), applicationKey.toJsonEncoded());
            }
        }
    }

    public static ApplicationToken getApplicationToken(String applicationtokenid) {
        ApplicationToken applicationToken = applicationTokenMap.get(applicationtokenid);
        if (applicationToken == null) {
            return null;
        }
        if (isApplicationTokenExpired(applicationToken.getApplicationTokenId())) {
            applicationTokenMap.remove(applicationToken.getApplicationTokenId());
            return null;
        }
        return applicationTokenMap.get(applicationtokenid);
    }

    public static ExchangeableKey getExchangeableKeyForApplicationToken(ApplicationToken applicationToken) {
        return new ExchangeableKey(applicationKeyMap.get(applicationToken.getApplicationTokenId()));
    }

    public static boolean verifyApplicationToken(ApplicationToken applicationToken) {
        if (applicationToken == null) {
            return false;
        }
        ApplicationToken applicationTokenFromMap = applicationTokenMap.get(applicationToken.getApplicationTokenId());
        if (applicationTokenFromMap == null) {
            return false;
        }
        if (isApplicationTokenExpired(applicationTokenFromMap.getApplicationTokenId())) {
            applicationTokenMap.remove(applicationTokenFromMap.getApplicationTokenId());
            return false;
        }
        return true;
    }

    public static boolean verifyApplicationTokenId(String applicationtokenid) {
        return applicationTokenMap.get(applicationtokenid) != null;
    }

    public static ApplicationToken renewApplicationTokenId(String applicationtokenid) {
        ApplicationToken temp = applicationTokenMap.get(applicationtokenid);  // Can't remove as verify check in map
        if (verifyApplicationToken(temp)) {
            applicationTokenMap.remove(applicationtokenid);
            String oldExpires = temp.getExpiresFormatted();
            temp.setExpires(updateExpires(temp.getExpires(), temp.getApplicationID()));
            log.info("Updated expiry for applicationID:{}  oldExpiry:{}, newExpiry: {}", applicationtokenid, oldExpires, temp.getExpiresFormatted());
            applicationTokenMap.put(temp.getApplicationTokenId(), temp);
            return temp;
        }
        return null;
    }

    public static boolean verifyApplicationTokenXml(String applicationtokenXml) {
        try {
            String applicationID = getApplocationTokenIdFromApplicationTokenXML(applicationtokenXml);
            ApplicationToken applicationToken = applicationTokenMap.get(applicationID);
            return verifyApplicationToken(applicationToken);
        } catch (StringIndexOutOfBoundsException e) {
            return false;
        }
    }

    public static String getApplicationIdFromApplicationTokenID(String applicationtokenid) {
        ApplicationToken at = applicationTokenMap.get(applicationtokenid);
        if (at != null) {
            return at.getApplicationID();
        }
        log.error("getApplicationIdFromApplicationTokenID - Unable to find applicationID for applicationtokenId=" + applicationtokenid);
        return "";
    }

    public static String getApplicationNameFromApplicationTokenID(String applicationtokenid) {
        ApplicationToken at = applicationTokenMap.get(applicationtokenid);
        if (at != null) {
            return at.getApplicationName();
        }
        log.error("getApplicationNameFromApplicationTokenID - Unable to find applicationName for applicationtokenId=" + applicationtokenid);
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


    public static boolean isApplicationTokenExpired(String applicationtokenid) {
        ApplicationToken temp = applicationTokenMap.get(applicationtokenid);

        Long expires = Long.parseLong(temp.getExpires());
        Long now = System.currentTimeMillis();
        if (expires > now) {
            return false;
        }
        return true;
    }

    private static String updateExpires(String oldExpiry, String applicationID) {
        String applicationMaxSessionTime = ApplicationModelFacade.getParameterForApplication(ApplicationModelUtil.maxSessionTimeoutSeconds, applicationID);
        if (applicationMaxSessionTime != null && (applicationMaxSessionTime.length() > 0) && (Long.parseLong(applicationMaxSessionTime) > 0)) {
            log.info("maxSessionTimeoutSeconds found: {} for applicationID: {}", Long.parseLong(applicationMaxSessionTime), applicationID);
            // Set to application configured maxSessionTimeoutSeconds if found and shave off 10 seconds
            return String.valueOf(System.currentTimeMillis() + Long.parseLong(applicationMaxSessionTime) * 1000 - 10 * 1000);

        }
        log.info("maxSessionTimeoutSeconds not found, using default: {} for applicationID: {}", DEFAULT_SESSION_EXTENSION_TIME_IN_SECONDS, applicationID);
        return String.valueOf(System.currentTimeMillis() + DEFAULT_SESSION_EXTENSION_TIME_IN_SECONDS * 1000);
    }


    private static String findValue(String xmlString, String expression) {
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

    public static int getMapSize() {
        return applicationTokenMap.size();
    }

    public static String getActiveApplications() {
        Map<String, Integer> applicationMap = new HashMap<>();
        for (Map.Entry<String, ApplicationToken> entry : applicationTokenMap.entrySet()) {
            // Let us use ApplicationID to identify applications without applicationName
            String applicationIdentifier;
            if (entry.getValue().getApplicationName() == null || entry.getValue().getApplicationName().length() < 2) {
                applicationIdentifier = entry.getValue().getApplicationID();
            } else {
                applicationIdentifier = entry.getValue().getApplicationName() + "[" + entry.getValue().getApplicationID() + "]";
            }
            if (applicationMap.get(applicationIdentifier) != null) {
                applicationMap.put(applicationIdentifier, 1 + applicationMap.get(applicationIdentifier));
            } else {
                applicationMap.put(applicationIdentifier, 1);
            }
        }
        logActiveApplicationTokenIDs();
        return applicationMap.toString();
    }


    public static void logActiveApplicationTokenIDs() {

        // Do not mess up the log completely...  :)
        if (ThreadLocalRandom.current().nextInt(1, 100 + 1) < 2) {
            String lognString = "";
            for (Map.Entry<String, ApplicationToken> entry : applicationTokenMap.entrySet()) {
                lognString = lognString + entry.getValue().getApplicationTokenId() + "(" + entry.getValue().getApplicationName() + "), ";
            }
            log.trace("Active applicationTokenIDs: " + lognString);

        }
    }

    /**
     * @return a singleton STSApplicationToken
     */
    public static ApplicationToken getSTSApplicationToken() {
        String applicationName = appConfig.getProperty("applicationname");
        String applicationId = appConfig.getProperty("applicationid");
        String applicationsecret = appConfig.getProperty("applicationsecret");
        // Do not create duplicate sts sessions
        if (stsApplicationTokenID.equals("")) {
            ApplicationCredential ac = new ApplicationCredential(applicationId, applicationName, applicationsecret);
            myToken = ApplicationTokenMapper.fromApplicationCredentialXML(ApplicationCredentialMapper.toXML(ac));
            myToken.setExpires(String.valueOf((System.currentTimeMillis() + 100000 * 5000)));  // Long time
            stsApplicationTokenID = myToken.getApplicationTokenId();
            addApplicationToken(myToken);
        }
        return myToken;

    }

    public static void cleanApplicationTokenMap() {
        // OK... let us obfucscate/filter sessionsid's in signalEmitter field
        for (Map.Entry<String, ApplicationToken> entry : applicationTokenMap.entrySet()) {
            ApplicationToken applicationToken = entry.getValue();
            if (isApplicationTokenExpired(applicationToken.getApplicationTokenId())) {
                applicationTokenMap.remove(applicationToken.getApplicationTokenId());
            }
        }
    }

}
