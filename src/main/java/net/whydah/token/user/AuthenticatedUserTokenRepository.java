package net.whydah.token.user;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import net.whydah.sso.user.types.UserToken;
import net.whydah.token.application.ApplicationThreatResource;
import net.whydah.token.application.SessionHelper;
import net.whydah.token.config.AppConfig;
import net.whydah.token.user.statistics.UserSessionObservedActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.valuereporter.agent.MonitorReporter;
import org.valuereporter.agent.activity.ObservedActivity;

import java.io.FileNotFoundException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AuthenticatedUserTokenRepository {
    private final static Logger log = LoggerFactory.getLogger(AuthenticatedUserTokenRepository.class);
    private static Map<String, UserToken> activeusertokensmap;
    private static Map<String, String> active_username_usertokenids_map;
    private static Map<String, Date> lastSeenMap;
    private static int noOfClusterMembers = 0;
    private static HazelcastInstance hazelcastInstance;
    public static long DEFAULT_SESSION_EXTENSION_TIME_IN_SECONDS = 14 * 24 * 60 * 60;  // 14 days


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
        hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig);
        activeusertokensmap = hazelcastInstance.getMap(appConfig.getProperty("gridprefix") + "activeusertokensmap");
        active_username_usertokenids_map =  hazelcastInstance.getMap(appConfig.getProperty("gridprefix") + "active_username_usertokenids_map");
        
        log.info("Connecting to map {} - size: {}", appConfig.getProperty("gridprefix") + "activeusertokensmap", getMapSize());
        log.info("Connecting to map {} - size: {}", appConfig.getProperty("gridprefix") + "active_username_usertokenids_map", getMapSize());
        
        lastSeenMap = hazelcastInstance.getMap(appConfig.getProperty("gridprefix") + "lastSeenMap");
        log.info("Connecting to map {} - size: {}", appConfig.getProperty("gridprefix") + "lastSeenMap", getLastSeenMapSize());
        Set clusterMembers = hazelcastInstance.getCluster().getMembers();
        noOfClusterMembers = clusterMembers.size();
        String applicationDefaultTimeout = System.getProperty("user.session.timeout");
        if (applicationDefaultTimeout != null && (Integer.parseInt(applicationDefaultTimeout) > 0)) {
            log.info("Updated DEFAULT_SESSION_EXTENSION_TIME_IN_SECONDS to " + applicationDefaultTimeout);
            DEFAULT_SESSION_EXTENSION_TIME_IN_SECONDS = Integer.parseInt(applicationDefaultTimeout);
        }

    }

    public static void setLastSeen(UserToken userToken) {
        if (userToken != null && userToken.getEmail() != null) {
            lastSeenMap.put(userToken.getEmail(), new Date());
        }
    }


    public static String getLastSeen(UserToken userToken) {
        if (userToken != null) {
            Date d = lastSeenMap.get(userToken.getEmail());
            if (d != null) {
                return d.toString();
            }
        }
        return "Not seen";
    }

    public static String getLastSeenByEmail(String email) {
        if (email != null) {
            Date d = lastSeenMap.get(email);
            if (d != null) {
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
    public static UserToken getUserToken(String usertokenId, String applicationTokenId) {
        log.debug("getUserToken with userTokenid=" + usertokenId);
        if (usertokenId == null) {
            return null;
        }
        UserToken resToken = activeusertokensmap.get(usertokenId);
        if (resToken != null && verifyUserToken(resToken, applicationTokenId)) {
            resToken.setLastSeen(AuthenticatedUserTokenRepository.getLastSeen(resToken));
            lastSeenMap.put(resToken.getEmail(), new Date());
            log.info("Valid userToken found: " + resToken);
            log.debug("userToken=" + resToken);

            ObservedActivity observedActivity = new UserSessionObservedActivity(resToken.getUid(), "userSessionAccess", applicationTokenId);
            MonitorReporter.reportActivity(observedActivity);
            log.trace("Adding activity to statistics cache {}", observedActivity);

            return resToken;
        }
        log.debug("No usertoken found for usertokenId=" + usertokenId);
        return null;
    }
    
    public static UserToken getUserTokenByUserName(String userName, String applicationTokenId) {
        log.debug("getUserToken with userName=" + userName);
        if (userName == null) {
            return null;
        }
        if(active_username_usertokenids_map.containsKey(userName)){
        	String usertokenid = active_username_usertokenids_map.get(userName);
        	 UserToken resToken = activeusertokensmap.get(usertokenid);
             if (resToken != null && verifyUserToken(resToken, applicationTokenId)) {
                 resToken.setLastSeen(AuthenticatedUserTokenRepository.getLastSeen(resToken));
                 lastSeenMap.put(resToken.getEmail(), new Date());
                 log.info("Valid userToken found: " + resToken);
                 log.debug("userToken=" + resToken);

                 ObservedActivity observedActivity = new UserSessionObservedActivity(resToken.getUid(), "userSessionAccess", applicationTokenId);
                 MonitorReporter.reportActivity(observedActivity);
                 log.trace("Adding activity to statistics cache {}", observedActivity);

                 return resToken;
             }
        }
       
        log.debug("No usertoken found for username=" + userName);
        return null;
    }

    /**
     * Check if token exists in UserTokenRepository, and is valid and not timed out.
     *
     * @param userToken UserToken
     * @return true if token is valid.
     */
    public static boolean verifyUserToken(UserToken userToken, String applicationTokenId) {
        if (userToken.getUserTokenId() == null) {
            log.info("UserToken not valid, missing UserTokenId");
            return false;
        }
        if (userToken.getEmail() != null) {
            lastSeenMap.put(userToken.getEmail(), new Date());

        }
        UserToken resToken = activeusertokensmap.get(userToken.getUserTokenId());
        if (resToken == null) {
            log.info("UserToken not found in repo.");
            return false;
        }
        log.debug("UserToken from repo: {}", resToken);
        if (!resToken.isValid()) {
            log.debug("resToken is not valid");
            activeusertokensmap.remove(userToken.getUserTokenId());
            active_username_usertokenids_map.remove(userToken.getUserName());
            
            return false;
        }
        if (!userToken.toString().equals(resToken.toString())) {
            log.info("UserToken not valid: not the same as in repo. token: {}  repotoken: {}", userToken, resToken);
            return false;
        }
        ObservedActivity observedActivity = new UserSessionObservedActivity(resToken.getUid(), "userSessionVerification", applicationTokenId);
        MonitorReporter.reportActivity(observedActivity);

        return true;
    }

    public static void renewUserToken(String usertokenid, String applicationTokenId) {
        UserToken utoken = activeusertokensmap.remove(usertokenid);
        active_username_usertokenids_map.remove(utoken.getUserName());
        utoken.setDefcon(ApplicationThreatResource.getDEFCON());
        utoken.setTimestamp(String.valueOf(System.currentTimeMillis()));
        utoken.setLifespan(String.valueOf(1000 * SessionHelper.getApplicationLifeSpanSeconds(applicationTokenId)));
        addUserToken(utoken, applicationTokenId, "renew");
        ObservedActivity observedActivity = new UserSessionObservedActivity(utoken.getUid(), "userSessionRenewal", applicationTokenId);
        MonitorReporter.reportActivity(observedActivity);

    }

    public static void refreshUserToken(String usertokenid, String applicationTokenId, UserToken refreshedUserToken) {
        UserToken oldusertoken = activeusertokensmap.remove(usertokenid);
        refreshedUserToken.setUserTokenId(usertokenid);
        addUserToken(refreshedUserToken, applicationTokenId, "refresh");

    }

    private static String generateID() {
        return UUID.randomUUID().toString();
    }

    public static void addUserToken(UserToken userToken, String applicationTokenId, String authType) {
        if (userToken.getUserTokenId() == null) {
            log.error("Error: UserToken has no usertokenid");
            userToken.setUserTokenId(generateID());
        }

        if (userToken.getLifespan() == null) {
            log.debug("addUserToken: UserToken has no lifespan");
            userToken.setLifespan(String.valueOf(1000 * SessionHelper.getApplicationLifeSpanSeconds(applicationTokenId)));
            userToken.setTimestamp(String.valueOf(System.currentTimeMillis()));

        }

        if (userToken.getEmail() != null) {
            userToken.setLastSeen(AuthenticatedUserTokenRepository.getLastSeen(userToken));
            lastSeenMap.put(userToken.getEmail(), new Date());

        }
        if (activeusertokensmap.containsKey(userToken.getUserTokenId())) {
            log.error("Error: trying to update an already existing UserToken in repo..");
            return;
        }
        //UserToken copy = userToken.copy();
        activeusertokensmap.put(userToken.getUserTokenId(), userToken);
      
        if(userToken.getUserName()!=null){
            active_username_usertokenids_map.put(userToken.getUserName(), userToken.getUserTokenId());
        }
        if ("renew".equalsIgnoreCase(authType)) {
            return;  // alreqdy reported
        }
        if ("refresh".equalsIgnoreCase(authType)) {
            return;  // alreqdy reported
        }
        ObservedActivity observedActivity = new UserSessionObservedActivity(userToken.getUid(), "userSessionCreatedByPassword", applicationTokenId);
        if ("pin".equalsIgnoreCase(authType)) {
            observedActivity = new UserSessionObservedActivity(userToken.getUid(), "userSessionCreatedByPin", applicationTokenId);
        }
        MonitorReporter.reportActivity(observedActivity);
        log.info("Added token with id {}", userToken.getUserTokenId(), " content:" + userToken.toString());
    }

    public static void removeUserToken(String userTokenId, String applicationTokenId) {
        UserToken removedToken = activeusertokensmap.remove(userTokenId);
        active_username_usertokenids_map.remove(removedToken.getUserName());
        
        ObservedActivity observedActivity = new UserSessionObservedActivity(removedToken.getUid(), "userSessionRemoved", applicationTokenId);
        MonitorReporter.reportActivity(observedActivity);

    }

    public static void initializeDistributedMap() {
    }

    public static int getMapSize() {
        logUserTokenMap();
        return activeusertokensmap.size();
    }

    public static void logUserTokenMap() {
        String logString = "";
        for (Map.Entry<String, UserToken> entry : activeusertokensmap.entrySet()) {
            logString = logString + entry.getValue().getUserTokenId() + "(" + entry.getValue().getUserName() + "), ";
        }
        log.debug("UserToken map:{}", logString);
    }

    public static int getLastSeenMapSize() {
        return lastSeenMap.size();
    }

    public static int getNoOfClusterMembers() {
        Set clusterMembers = hazelcastInstance.getCluster().getMembers();
        noOfClusterMembers = clusterMembers.size();
        return noOfClusterMembers;
    }


    public static void cleanUserTokenMap() {
        // OK... let us obfucscate/filter sessionsid's in signalEmitter field
        for (Map.Entry<String, UserToken> entry : activeusertokensmap.entrySet()) {
            UserToken userToken = entry.getValue();
            if (!userToken.isValid()) {
                activeusertokensmap.remove(userToken.getUserTokenId());
            }
        }
    }

}
