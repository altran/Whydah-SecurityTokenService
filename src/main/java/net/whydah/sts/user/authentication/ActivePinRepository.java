package net.whydah.sts.user.authentication;


import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import net.whydah.sts.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class ActivePinRepository {
    private final static Logger log = LoggerFactory.getLogger(ActivePinRepository.class);
    private static Map<String, String> pinMap;
    private static Map<String, String> smsResponseLogMap;
    //private static Map<String, String> pinMap = ExpiringMap.builder()
    //        .expiration(30, TimeUnit.SECONDS)
    //        .build();


    static {
        AppConfig appConfig = new AppConfig();
        String xmlFileName = System.getProperty("hazelcast.config");
        if (xmlFileName == null || xmlFileName.trim().isEmpty()) {
            xmlFileName = appConfig.getProperty("hazelcast.config");
        }
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
        //hazelcastConfig.getGroupConfig().setName("STS_HAZELCAST");
        HazelcastInstance hazelcastInstance;
        try {
        	hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig);
        } catch(Exception ex) {
        	hazelcastInstance = Hazelcast.newHazelcastInstance();
        }
        pinMap = hazelcastInstance.getMap(appConfig.getProperty("gridprefix")+"pinMap");
        smsResponseLogMap = hazelcastInstance.getMap(appConfig.getProperty("gridprefix")+"smsResponseLogMap");
        log.info("Connecting to map {}",appConfig.getProperty("gridprefix")+"pinMap");
        log.info("Loaded pin-Map size=" + pinMap.size());
        
        pinMap.keySet().forEach(keySet -> {
        	if(!pinMap.get(keySet).contains(":")) {
        		pinMap.remove(keySet);
        	}
        });
    
    }

    public static void setPin(String phoneNr, String pin, String smsResponse) {
        pin = paddPin(pin);
        log.debug("Adding pin:{}  to phone:{} ", pin, phoneNr);
        log.debug("SMS log for " + phoneNr + ": "+ smsResponse);
        pinMap.put(phoneNr, pin + ":" + Instant.now().toEpochMilli());
        if(smsResponse!=null){
        	smsResponseLogMap.put(phoneNr, smsResponse);
        }
    }
    
    public static String getPinSentIfAny(String phoneNr) {
    	
    	String storedPin = pinMap.get(phoneNr);
    	if(storedPin !=null && storedPin.contains(":")) {
    		String[] parts = storedPin.split(":");
    		String pin = parts[0];
    		String datetime = parts[1];
    		Instant inst = Instant.ofEpochMilli(Long.valueOf(datetime));
    		if(Instant.now().isAfter(inst.plus(5, ChronoUnit.MINUTES))) {
    			pinMap.remove(phoneNr);
    			return null;
    		} else {
    			return pin;
    		}
    	} else {
    		//return storedPin;
    		return null;
    	}
      
    }

    public static boolean usePin(String phoneNr, String pin) {
        pin = paddPin(pin);
        log.debug("usePin - Trying pin {} for phone {}: ", pin, phoneNr);
        if (isValidPin(phoneNr, pin)) {
            log.info("usePin - Used pin:{} for phone: {}", pin, phoneNr);
            //remove after used
            pinMap.remove(phoneNr);
            smsResponseLogMap.remove(phoneNr);
            return true;
        }
        log.debug("usePin - Failed to use pin {} for phone {}  ", pin, phoneNr);
        return false;
    }
    public static Map<String, String> getPinMap(){
        return  pinMap;
    }
    
    public static Map<String, String> getSMSResponseLogMap(){
        return smsResponseLogMap;
    }

    private static boolean isValidPin(String phoneNr, String pin) {
        pin = paddPin(pin);
        String storedPin = pinMap.get(phoneNr);
        if (storedPin != null && storedPin.contains(":")) {
        	String[] parts = storedPin.split(":");
        	storedPin = parts[0];
			String datetime = parts[1];
			Instant inst = Instant.ofEpochMilli(Long.valueOf(datetime));
			if(Instant.now().isAfter(inst.plus(5, ChronoUnit.MINUTES))) {
				pinMap.remove(phoneNr);
				log.warn("Pin expired : {} - {}", phoneNr, pin);
				return false;
			}
        }

        log.debug("isValidPin on pin:{},  storedpin:{}, phone:{}", pin, storedPin, phoneNr);
        if (storedPin != null && storedPin.equals(pin)) {
        	return true;
        }

        log.warn("Illegal pin logon attempted. phone: {} invalid pin attempted:{}", phoneNr, pin);
        return false;
    }

    public static String paddPin(String pin) {

        if (pin.length() == 3) {
            pin = "0" + pin;
        } else if (pin.length() == 2) {
            pin = "00" + pin;
        } else if (pin.length() == 1) {
            pin = "000" + pin;
        }
        return pin;
    }

}
