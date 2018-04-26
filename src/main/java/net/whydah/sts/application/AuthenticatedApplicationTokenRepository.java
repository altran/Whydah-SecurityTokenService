package net.whydah.sts.application;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import net.whydah.sso.application.mappers.ApplicationCredentialMapper;
import net.whydah.sso.application.mappers.ApplicationTokenMapper;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.ddd.model.application.ApplicationTokenExpires;
import net.whydah.sso.session.WhydahApplicationSession;
import net.whydah.sso.session.baseclasses.ExchangeableKey;
import net.whydah.sts.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.IvParameterSpec;
import java.io.FileNotFoundException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;


public class AuthenticatedApplicationTokenRepository {
    private final static Logger log = LoggerFactory.getLogger(AuthenticatedApplicationTokenRepository.class);

    public static final long DEFAULT_APPLICATION_SESSION_EXTENSION_TIME_IN_SECONDS;
    public static final int STS_TOKEN_MULTIPLIER = 50;
    private static AppConfig appConfig = new AppConfig();
    private static String mySTSApplicationTokenId = "";
    private static ApplicationToken mySTSApplicationToken;

    private static final Map<String, ApplicationToken> applicationTokenMap;
    private static final Map<String, String> applicationCryptoKeyMap;
    private static Base64.Decoder decoder = Base64.getDecoder();


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
                log.error("Error - not able to load hazelcast.xml configuration.  Using embedded configuration as fallback");
            }
        }

        hazelcastConfig.setProperty("hazelcast.logging.type", "slf4j");
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig);
        applicationTokenMap = hazelcastInstance.getMap(appConfig.getProperty("gridprefix") + "_authenticated_applicationtokens");
        log.info("Connecting to map {} - map size: {}", appConfig.getProperty("gridprefix") + "_authenticated_applicationtokens", getMapSize());

        applicationCryptoKeyMap = hazelcastInstance.getMap(appConfig.getProperty("gridprefix") + "_applicationkeys");
        log.info("Connecting to map {} - map size: {}", appConfig.getProperty("gridprefix") + "_applicationkeys", getKeyMapSize());


        String applicationDefaultTimeout = appConfig.getProperty("application.session.timeout");
        log.info("Read DEFAULT_APPLICATION_SESSION_EXTENSION_TIME_IN_SECONDS from properties " + applicationDefaultTimeout);
        if (applicationDefaultTimeout != null && (Integer.parseInt(applicationDefaultTimeout) > 0)) {
            if (Integer.parseInt(applicationDefaultTimeout) < WhydahApplicationSession.APPLICATION_SESSION_CHECK_INTERVAL_IN_SECONDS * 10) {
                log.warn("Attempt to set application.session.timeout to low, overriding with WhydahApplicationSession.APPLICATION_SESSION_CHECK_INTERVAL_IN_SECONDS*10: {} ", WhydahApplicationSession.APPLICATION_SESSION_CHECK_INTERVAL_IN_SECONDS * 10);
                DEFAULT_APPLICATION_SESSION_EXTENSION_TIME_IN_SECONDS = WhydahApplicationSession.APPLICATION_SESSION_CHECK_INTERVAL_IN_SECONDS * 10;
            } else {
                DEFAULT_APPLICATION_SESSION_EXTENSION_TIME_IN_SECONDS = Integer.parseInt(applicationDefaultTimeout);
            }
        } else {
            DEFAULT_APPLICATION_SESSION_EXTENSION_TIME_IN_SECONDS = WhydahApplicationSession.APPLICATION_SESSION_CHECK_INTERVAL_IN_SECONDS * 10; //One minute = 60 seconds //2400;
        }
        log.info("Set DEFAULT_APPLICATION_SESSION_EXTENSION_TIME_IN_SECONDS to " + DEFAULT_APPLICATION_SESSION_EXTENSION_TIME_IN_SECONDS);

    }

    public static void updateApplicationToken(ApplicationToken applicationToken) {
    	 applicationTokenMap.put(applicationToken.getApplicationTokenId(), applicationToken);
    }

    public static void addApplicationToken(ApplicationToken applicationToken) {
       
        long remainingSecs = (Long.parseLong(applicationToken.getExpires()) - System.currentTimeMillis()) / 1000;
        log.info("Added {} applicationID:{} applicationTokenId:{} - expires in {} seconds", applicationToken.getApplicationName(), applicationToken.getApplicationID(), applicationToken.getApplicationTokenId(), remainingSecs);
        applicationTokenMap.put(applicationToken.getApplicationTokenId(), applicationToken);
        
        if (applicationCryptoKeyMap.containsKey(applicationToken.getApplicationTokenId())) { 
                // Maybe update key here...
                log.debug("updating cryptokey for applicationId: {} with applicationTokenId:{}", applicationToken.getApplicationID(), applicationToken.getApplicationTokenId());
                try {
                    String iv = "MDEyMzQ1Njc4OTBBQkNERQ==";
                    ExchangeableKey applicationKey = new ExchangeableKey();
                    applicationKey.setEncryptionSecret(System.currentTimeMillis() + applicationToken.getApplicationTokenId());
                    applicationKey.setIv(new IvParameterSpec(decoder.decode(iv)));
                    applicationCryptoKeyMap.put(applicationToken.getApplicationTokenId(), applicationKey.toJsonEncoded());
                } catch (Exception e) {
                    log.warn("Unable to create cryotokey for applicationIs: {}", applicationToken.getApplicationID());
                }
            } else {
                // Bootstrap key initialization
                log.debug("adding new cryptokey for applicationId: {} with applicationTokenId:{}", applicationToken.getApplicationID(), applicationToken.getApplicationTokenId());
                try {
                    String iv = "MDEyMzQ1Njc4OTBBQkNERQ==";
                    ExchangeableKey applicationKey = new ExchangeableKey();
                    applicationKey.setEncryptionSecret(applicationToken.getApplicationTokenId());
                    applicationKey.setIv(new IvParameterSpec(decoder.decode(iv)));
                    applicationCryptoKeyMap.put(applicationToken.getApplicationTokenId(), applicationKey.toJsonEncoded());
                } catch (Exception e) {
                    log.warn("Unable to create cryotokey for applicationIs: {}", applicationToken.getApplicationID());
                }
            }
    }

    public static ApplicationToken getApplicationToken(String applicationtokenid) {
        ApplicationToken applicationToken = applicationTokenMap.get(applicationtokenid);
        if (applicationToken == null) {
            return null;
        }
        if (isApplicationTokenExpired(applicationToken)) {
            log.warn("Attempting to get an expired applicationtoken.  ApplicationId:{} ApplicationTokenId:{}", applicationToken.getApplicationID(), applicationtokenid);
            applicationTokenMap.remove(applicationToken.getApplicationTokenId());
            applicationCryptoKeyMap.remove(applicationToken.getApplicationTokenId());
            return null;
        }
        return applicationTokenMap.get(applicationtokenid);
    }

    public static ExchangeableKey getExchangeableKeyForApplicationToken(ApplicationToken applicationToken) {
        ExchangeableKey exchangeableKey = new ExchangeableKey(applicationCryptoKeyMap.get(applicationToken.getApplicationTokenId()));
        if (exchangeableKey.getIv() == null) {
            log.warn("Attempt fo find key for applicationID:{} with applicationTokenId:[} failed", applicationToken.getApplicationID(), applicationToken.getApplicationTokenId());
            return null;
        }
        return exchangeableKey;
    }

    public static boolean verifyApplicationToken(ApplicationToken applicationToken) {
        if (applicationToken == null) {
            return false;
        }
        ApplicationToken applicationTokenFromMap = applicationTokenMap.get(applicationToken.getApplicationTokenId());
        if (applicationTokenFromMap == null) {
            return false;
        }
        if (isApplicationTokenExpired(applicationTokenFromMap)) {
            applicationTokenMap.remove(applicationTokenFromMap.getApplicationTokenId());
            applicationCryptoKeyMap.remove(applicationTokenFromMap.getApplicationTokenId());

            return false;
        }
        return true;
    }

    public static boolean verifyApplicationTokenId(String applicationtokenid) {
        return applicationTokenMap.get(applicationtokenid) != null;
    }

    public static ApplicationToken renewApplicationTokenId(String applicationtokenid) {
        ApplicationToken renewApplicationToken = applicationTokenMap.get(applicationtokenid);  // Can't remove as verify check in map
        String exchangeableKey = applicationCryptoKeyMap.get(applicationtokenid);  // Can't remove as verify check in map
        if (verifyApplicationToken(renewApplicationToken)) {
            //applicationTokenMap.remove(applicationtokenid);
            //applicationCryptoKeyMap.remove(applicationtokenid); //HUYDO: BUG HERE, don't remove. It should be there as we are extending the expiry date
            String oldExpires = renewApplicationToken.getExpiresFormatted();
            renewApplicationToken.setExpires(String.valueOf(new ApplicationTokenExpires(DEFAULT_APPLICATION_SESSION_EXTENSION_TIME_IN_SECONDS * 1000).getValueAsAbsoluteTimeInMilliseconds()));
            log.info("Updated expiry for applicationId:{} applicationtokenid:{} oldExpiry:{}, newExpiry: {}", renewApplicationToken.getApplicationID(), applicationtokenid, oldExpires, renewApplicationToken.getExpiresFormatted());
            applicationTokenMap.put(renewApplicationToken.getApplicationTokenId(), renewApplicationToken);
            log.debug("updating cryptokey for applicationId: {} with applicationTokenId:{}", renewApplicationToken.getApplicationID(), renewApplicationToken.getApplicationTokenId());
            try {
                String iv = "MDEyMzQ1Njc4OTBBQkNERQ==";
                ExchangeableKey applicationKey = new ExchangeableKey();
                applicationKey.setEncryptionSecret(System.currentTimeMillis() + renewApplicationToken.getApplicationTokenId());
                applicationKey.setIv(new IvParameterSpec(decoder.decode(iv)));
                applicationCryptoKeyMap.put(renewApplicationToken.getApplicationTokenId(), applicationKey.toJsonEncoded());
            } catch (Exception e) {
                log.warn("Unable to create cryotokey for applicationId: {}, re-using old", renewApplicationToken.getApplicationID());
                applicationCryptoKeyMap.put(renewApplicationToken.getApplicationTokenId(), exchangeableKey);
            }
            return renewApplicationToken;
        }
        return null;
    }


    public static String getApplicationIdFromApplicationTokenID(String applicationtokenid) {
        ApplicationToken applicationToken = applicationTokenMap.get(applicationtokenid);
        if (applicationToken != null) {
            return applicationToken.getApplicationID();
        }
        log.error("getApplicationIdFromApplicationTokenId - Unable to find applicationId for applicationTokenId=" + applicationtokenid);
        return "";
    }

    public static String getApplicationNameFromApplicationTokenID(String applicationtokenid) {
        ApplicationToken applicationToken = applicationTokenMap.get(applicationtokenid);
        if (applicationToken != null) {
            return applicationToken.getApplicationName();
        }
        log.error("getApplicationNameFromApplicationTokenId - Unable to find applicationName for applicationTokenId=" + applicationtokenid);
        return "";
    }

    public static String getApplicationCryptoKeyFromApplicationTokenID(String applicationtokenid) {
        String applicationKey = applicationCryptoKeyMap.get(applicationtokenid);
        return applicationKey;
    }


    public static boolean isApplicationTokenExpired(ApplicationToken applicationtoken) {

        Long expires = Long.parseLong(applicationtoken.getExpires());
        //log.info("Checking {} for timeout, ",applicationtoken.getApplicationID());
        Long now = System.currentTimeMillis();
        if (expires > now) {
            log.trace("Checking applicationId {} for timeout, result false",applicationtoken.getApplicationID());
            return false;
        }
        log.trace("Checking applicationId {} for timeout, result: false",applicationtoken.getApplicationID());
        return true;
    }



    public static int getMapSize() {
        return applicationTokenMap.size();
    }

    public static int getKeyMapSize() {
        return applicationCryptoKeyMap.size();
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
            log.trace("Active applicationTokenIds: " + lognString);

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
        if (mySTSApplicationTokenId.equals("") || !applicationTokenMap.containsKey(mySTSApplicationTokenId)) {  // First time
            ApplicationCredential ac = new ApplicationCredential(applicationId, applicationName, applicationsecret);
            mySTSApplicationToken = ApplicationTokenMapper.fromApplicationCredentialXML(ApplicationCredentialMapper.toXML(ac));
            mySTSApplicationToken.setExpires(String.valueOf(new ApplicationTokenExpires(DEFAULT_APPLICATION_SESSION_EXTENSION_TIME_IN_SECONDS * 1000L * STS_TOKEN_MULTIPLIER).getValue()));  // 100 times the default
            mySTSApplicationTokenId = mySTSApplicationToken.getApplicationTokenId();
            addApplicationToken(mySTSApplicationToken);
        } else {  // update expires
            mySTSApplicationToken = applicationTokenMap.get(mySTSApplicationTokenId);
            mySTSApplicationToken.setExpires(String.valueOf(new ApplicationTokenExpires(DEFAULT_APPLICATION_SESSION_EXTENSION_TIME_IN_SECONDS * 1000L * STS_TOKEN_MULTIPLIER).getValue()));  // 100 times the default
            //very costly to generate key every time, just update
            //addApplicationToken(mySTSApplicationToken);
            updateApplicationToken(mySTSApplicationToken);
        }
        return mySTSApplicationToken;

    }

    public static void cleanApplicationTokenMap() {
        // OK... let us obfucscate/filter sessionsid's in signalEmitter field
        for (Map.Entry<String, ApplicationToken> entry : applicationTokenMap.entrySet()) {
            ApplicationToken applicationToken = entry.getValue();
            if (isApplicationTokenExpired(applicationToken)) {
                applicationTokenMap.remove(applicationToken.getApplicationTokenId());
                applicationCryptoKeyMap.remove(applicationToken.getApplicationTokenId());
            }
        }
    }

}
