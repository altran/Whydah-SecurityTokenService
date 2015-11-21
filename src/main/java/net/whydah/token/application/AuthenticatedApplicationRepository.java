package net.whydah.token.application;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.token.config.AppConfig;
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
import java.util.HashMap;
import java.util.Map;


public class AuthenticatedApplicationRepository {
    private final static Logger log = LoggerFactory.getLogger(AuthenticatedApplicationRepository.class);

    private static final Map<String, ApplicationToken> apptokens;

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
        apptokens = hazelcastInstance.getMap(appConfig.getProperty("gridprefix")+"_authenticated_apptokens");
        log.info("Connecting to map {}",appConfig.getProperty("gridprefix")+"_authenticated_apptokens");
    }


    public static void addApplicationToken(ApplicationToken token) {
        apptokens.put(token.getApplicationTokenId(), token);
    }

    public static ApplicationToken getApplicationToken(String applicationtokenid) {
        return apptokens.get(applicationtokenid);
    }


    public static boolean verifyApplicationToken(ApplicationToken token) {
        return token.equals(apptokens.get(token.getApplicationTokenId()));
    }

    public static boolean verifyApplicationTokenId(String applicationtokenid) {
        return apptokens.get(applicationtokenid) != null;
    }

    public static boolean verifyApplicationTokenXml(String applicationtokenXml) {
        try {
            String appid = getAppTokenIdFromAppTokenXML(applicationtokenXml);
            return apptokens.get(appid) != null;
        } catch (StringIndexOutOfBoundsException e) {
            return false;
        }
    }

    public static String getApplicationIdFromApplicationTokenID(String applicationtokenid) {
        ApplicationToken at = apptokens.get(applicationtokenid);
        if (at!=null) {
            return at.getApplicationID();
        }
        log.error("getApplicationIdFromApplicationTokenID - Unable to find applicationID for applkicationtokenid="+applicationtokenid);
        return "";
    }

    public static  String getAppTokenIdFromAppTokenXML(String appTokenXML) {
        String appTokenId = "";
        if (appTokenXML == null) {
            log.debug("roleXml was empty, so returning empty orgName.");
        } else {
            String expression = "/applicationtoken/params/applicationtokenID[1]";
            appTokenId = findValue(appTokenXML, expression);
        }
        return appTokenId;
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
