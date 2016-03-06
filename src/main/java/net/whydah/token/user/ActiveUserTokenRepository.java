package net.whydah.token.user;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import net.whydah.token.application.ApplicationThreatResource;
import net.whydah.token.config.AppConfig;
import net.whydah.token.user.statistics.UserSessionObservedActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.valuereporter.agent.MonitorReporter;
import org.valuereporter.agent.activity.ObservedActivity;

import java.io.FileNotFoundException;
import java.sql.Time;
import java.util.Date;
import java.util.Map;
import java.util.Random;

public class ActiveUserTokenRepository {
    private final static Logger log = LoggerFactory.getLogger(ActiveUserTokenRepository.class);
    private static Map<String, UserToken> activeusertokensmap;
    private static Map<String, Date> lastSeenMap;

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
        activeusertokensmap = hazelcastInstance.getMap(appConfig.getProperty("gridprefix")+"activeusertokensmap");
        log.info("Connecting to map {}",appConfig.getProperty("gridprefix")+"activeusertokensmap");
        lastSeenMap= hazelcastInstance.getMap(appConfig.getProperty("gridprefix")+"lastSeenMap");
        log.info("Connecting to map {}",appConfig.getProperty("gridprefix")+"lastSeenMap");
    }


    public static String getLastSeen(UserToken userToken){
        if (userToken!=null) {
            Date d = lastSeenMap.get(userToken.getEmail());
            if (d!=null){
                return d.toString();
            }
        }
        return "Not seen";
    }

    public static String getLastSeenByEmail(String email){
        if (email!=null) {
            Date d = lastSeenMap.get(email);
            if (d!=null){
                return d.toString();
            }
        }
        return "Not seen";
    }
    /**
     * Get UserToken from UserTokenRepository. If token is not found or is not valid/timed out, null is returned.
     *
     * @param usertokenId userTokenId
     * @return UserToken if found and valid, null if not.
     */
    public static UserToken getUserToken(String usertokenId,String applicationTokenId) {
        log.debug("getUserToken with userTokenid=" + usertokenId);
        if (usertokenId == null) {
            return null;
        }
        UserToken resToken = activeusertokensmap.get(usertokenId);
        if (resToken != null && verifyUserToken(resToken,applicationTokenId)) {
            resToken.setLastSeen(ActiveUserTokenRepository.getLastSeen(resToken));
            lastSeenMap.put(resToken.getEmail(),new Date());
            log.info("Valid userToken found: " + resToken);
            log.debug("userToken=" + resToken);

            ObservedActivity observedActivity = new UserSessionObservedActivity(resToken.getUid(),"userSessionAccess",applicationTokenId);
            MonitorReporter.reportActivity(observedActivity);
            log.trace("Adding activity to statistics cache {}", observedActivity);

            return resToken;
        }
        log.debug("No usertoken found for usertokenId=" + usertokenId);
        return null;
    }

    /**
     * Check if token exists in UserTokenRepository, and is valid and not timed out.
     *
     * @param userToken UserToken
     * @return true if token is valid.
     */
    public static boolean verifyUserToken(UserToken userToken,String applicationTokenId) {
        if (userToken.getTokenid() == null) {
            log.info("UserToken not valid, missing tokenId");
            return false;
        }
        if (userToken.getEmail()!=null){
            lastSeenMap.put(userToken.getEmail(),new Date());

        }
        UserToken resToken = activeusertokensmap.get(userToken.getTokenid());
        if (resToken == null) {
            log.info("UserToken not found in repo.");
            return false;
        }
        log.debug("UserToken from repo: {}", resToken);
        if (!resToken.isValid()) {
            log.debug("resToken is not valid");
            activeusertokensmap.remove(userToken.getTokenid());
            return false;
        }
        if (!userToken.toString().equals(resToken.toString())) {
            log.info("UserToken not valid: not the same as in repo. token: {}  repotoken: {}", userToken, resToken);
            return false;
        }
        ObservedActivity observedActivity = new UserSessionObservedActivity(resToken.getUid(),"userSessionVerification",applicationTokenId);
        MonitorReporter.reportActivity(observedActivity);

        return true;
    }

    public static void renewUserToken(String usertokenid,String applicationTokenId){
        UserToken utoken = ActiveUserTokenRepository.getUserToken(usertokenid,applicationTokenId);
        utoken.setDefcon(ApplicationThreatResource.getDEFCON());
        utoken.setTimestamp(String.valueOf(System.currentTimeMillis() + 1000));
        utoken.setLifespan(String.valueOf(60 * new Random().nextInt(100)));
        addUserToken(utoken,applicationTokenId);
        ObservedActivity observedActivity = new UserSessionObservedActivity(utoken.getUid(),"userSessionRenewal",applicationTokenId);
        MonitorReporter.reportActivity(observedActivity);

    }

    public static void addUserToken(UserToken userToken,String applicationTokenId) {
        if (userToken.getTokenid() == null) {
            log.error("Error: token has net tokenid");
            return;
        }

        if (userToken.getEmail()!=null){
            userToken.setLastSeen(ActiveUserTokenRepository.getLastSeen(userToken));
            lastSeenMap.put(userToken.getEmail(),new Date());

        }
        if (activeusertokensmap.containsKey(userToken.getTokenid())) {
            log.error("Error: trying to update an already existing UserToken in repo..");
            return;
        }
        UserToken copy = userToken.copy();
        activeusertokensmap.put(copy.getTokenid(), copy);
        ObservedActivity observedActivity = new UserSessionObservedActivity(userToken.getUid(),"userSessionCreated",applicationTokenId);
        MonitorReporter.reportActivity(observedActivity);
        log.info("Added token with id {}", copy.getTokenid(), " content:" + copy);
    }

    public static void removeUserToken(String userTokenId,String applicationTokenId) {
        UserToken removedToken = activeusertokensmap.remove(userTokenId);
        ObservedActivity observedActivity = new UserSessionObservedActivity(removedToken.getUid(),"userSessionRemoved",applicationTokenId);
        MonitorReporter.reportActivity(observedActivity);

    }

    public static void initializeDistributedMap() {
    }

    public static int getMapSize(){
        return activeusertokensmap.size();
    }
}
