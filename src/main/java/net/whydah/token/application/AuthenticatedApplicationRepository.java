package net.whydah.token.application;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import net.whydah.token.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;


public class AuthenticatedApplicationRepository {
    private final static Logger logger = LoggerFactory.getLogger(AuthenticatedApplicationRepository.class);

    private static final Map<String, ApplicationToken> apptokens;

    static {
        AppConfig appConfig = new AppConfig();
        String xmlFileName = System.getProperty("hazelcast.config");
        logger.info("Loading hazelcast configuration from :" + xmlFileName);
        Config hazelcastConfig = new Config();
        if (xmlFileName != null && xmlFileName.length() > 10) {
            try {
                hazelcastConfig = new XmlConfigBuilder(xmlFileName).build();
                logger.info("Loading hazelcast configuration from :" + xmlFileName);
            } catch (FileNotFoundException notFound) {
                logger.error("Error - not able to load hazelcast.xml configuration.  Using embedded as fallback");
            }
        }
        hazelcastConfig.setProperty("hazelcast.logging.type", "slf4j");
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig);
        apptokens = hazelcastInstance.getMap(appConfig.getProperty("gridprefix")+"_authenticated_apptokens");
        logger.info("Connecting to map {}",appConfig.getProperty("gridprefix")+"_authenticated_apptokens");
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

    public static boolean verifyApplicationToken(String s) {
        try {
            //TODO baardl: Implement ApplicationTokenVerification
            String appid = s.substring(s.indexOf("<applicationtoken>") + "<applicationtoken>".length(), s.indexOf("</applicationtoken>"));
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
        logger.error("Unable to find applicationID for applkicationtokenid="+applicationtokenid);
        return "";
    }

}
