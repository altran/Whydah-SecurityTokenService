package net.whydah.token.user;


import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import net.whydah.token.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.Map;

public class ActivePinRepository {
    private final static Logger log = LoggerFactory.getLogger(ActiveUserTokenRepository.class);
    private static Map<String, String> pinMap;
    //private static Map<String, String> pinMap = ExpiringMap.builder()
    //        .expiration(30, TimeUnit.SECONDS)
    //        .build();


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
        pinMap = hazelcastInstance.getMap(appConfig.getProperty("gridprefix")+"pinMap");
        log.info("Connecting to map {}",appConfig.getProperty("gridprefix")+"pinMap");

    }

    public static void setPin(String phoneNr, String pin) {
        log.info("Adding pin: " + pin + " to phone: "+ phoneNr);
        pinMap.put(phoneNr, pin);
    }

    public static boolean usePin(String phoneNr, String pin) {
        if (isValidPin(phoneNr, pin)) {
            log.info("Used pin for phone: "+ phoneNr);
            pinMap.remove(phoneNr);
            return true;
        }
        return false;
    }
    public static Map<String, String> getPinMap(){
        return  pinMap;
    }

    private static boolean isValidPin(String phoneNr, String pin) {
        String storedPin = pinMap.get(phoneNr);
        log.info("isValidPin on pin: " + pin + " storedpin: " + storedPin + " phonenumber: " + phoneNr);
        if (storedPin != null && storedPin.equals(pin)) {
            return true;
        }
        return false;
    }
}
