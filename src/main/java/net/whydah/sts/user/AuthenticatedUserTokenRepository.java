package net.whydah.sts.user;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import net.whydah.sso.ddd.model.application.ApplicationTokenID;
import net.whydah.sso.ddd.model.user.LastSeen;
import net.whydah.sso.ddd.model.user.UserTokenId;
import net.whydah.sso.user.types.UserToken;
import net.whydah.sts.config.AppConfig;
import net.whydah.sts.threat.ThreatResource;
import net.whydah.sts.user.statistics.UserSessionObservedActivity;
import net.whydah.sts.util.ApplicationModelHelper;
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
    public static long DEFAULT_USER_SESSION_EXTENSION_TIME_IN_SECONDS = 14 * 24 * 60 * 60;  // 14 days
    public static long DEFAULT_USER_SESSION_TIME_IN_SECONDS = 7 * 24 * 60 * 60;  // 7 days


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
        active_username_usertokenids_map = hazelcastInstance.getMap(appConfig.getProperty("gridprefix") + "active_username_usertokenids_map");

        log.info("Connecting to map {} - size: {}", appConfig.getProperty("gridprefix") + "activeusertokensmap", getMapSize());
        log.info("Connecting to map {} - size: {}", appConfig.getProperty("gridprefix") + "active_username_usertokenids_map", getMapSize());

        lastSeenMap = hazelcastInstance.getMap(appConfig.getProperty("gridprefix") + "lastSeenMap");
        log.info("Connecting to map {} - size: {}", appConfig.getProperty("gridprefix") + "lastSeenMap", getLastSeenMapSize());
        Set clusterMembers = hazelcastInstance.getCluster().getMembers();
        noOfClusterMembers = clusterMembers.size();
        String userTokenDefaultTimeout = appConfig.getProperty("user.session.timeout");
        if (userTokenDefaultTimeout != null && (Integer.parseInt(userTokenDefaultTimeout) > 0)) {
            log.info("Updated DEFAULT_USER_SESSION_EXTENSION_TIME_IN_SECONDS to " + userTokenDefaultTimeout);
            DEFAULT_USER_SESSION_EXTENSION_TIME_IN_SECONDS = Integer.parseInt(userTokenDefaultTimeout);
            log.info("Updated DEFAULT_USER_SESSION_TIME_IN_SECONDS to " + userTokenDefaultTimeout);
            DEFAULT_USER_SESSION_TIME_IN_SECONDS = DEFAULT_USER_SESSION_EXTENSION_TIME_IN_SECONDS / 2;
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
        return LastSeen.WHITE_LIST[0];
    }

    public static String getLastSeenByEmail(String email) {
        if (email != null) {
            Date d = lastSeenMap.get(email);
            if (d != null) {
                return d.toString();
            }
        }
        return LastSeen.WHITE_LIST[0];
    }

    /**
     * Get UserToken from UserTokenRepository. If sts is not found or is not valid/timed out, null is returned.
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
        if (active_username_usertokenids_map.containsKey(userName)) {
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
     * Check if sts exists in UserTokenRepository, and is valid and not timed out.
     *
     * @param userToken UserToken
     * @return true if sts is valid.
     */
    public static boolean verifyUserToken(UserToken userToken, String applicationTokenId) {
        if (userToken.getUserTokenId() == null) {
            log.info("UserToken not valid, missing UserTokenId");
            return false;
        }
        if (userToken.getEmail() != null) {
            lastSeenMap.put(userToken.getEmail(), new Date());

        }
        if (!ApplicationTokenID.isValid(applicationTokenId)) {
            log.debug("Matching against invalid ApplicationTokenId: {} - returning false", applicationTokenId);
            return false;
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
            log.info("UserToken not valid: not the same as in repo. userToken: {}  repoToken: {}", userToken, resToken);
            return false;
        }

        ObservedActivity observedActivity = new UserSessionObservedActivity(resToken.getUid(), "userSessionVerification", applicationTokenId);
        MonitorReporter.reportActivity(observedActivity);

        return true;
    }

    public static void renewUserToken(String usertokenid, String applicationTokenId) {
        UserToken userToken = activeusertokensmap.remove(usertokenid);
        active_username_usertokenids_map.remove(userToken.getUserName());
        userToken.setDefcon(ThreatResource.getDEFCON());
        userToken.setTimestamp(String.valueOf(System.currentTimeMillis()));
        long applicationUserTokenLifespanInSeconds = ApplicationModelHelper.getUserTokenLifeSpanSecondsFromApplication(applicationTokenId);
        log.debug("addUserToken: found applicationUserTokenLifespanInSeconds {} for ApplicationID:{}", applicationUserTokenLifespanInSeconds, ApplicationModelHelper.getApplicationId(new ApplicationTokenID(applicationTokenId)));
        if (applicationUserTokenLifespanInSeconds < DEFAULT_USER_SESSION_EXTENSION_TIME_IN_SECONDS) {
            userToken.setLifespan(String.valueOf(applicationUserTokenLifespanInSeconds * 1000));
        } else {
            userToken.setLifespan(String.valueOf(DEFAULT_USER_SESSION_EXTENSION_TIME_IN_SECONDS * 1000));
        }

        addUserToken(userToken, applicationTokenId, "renew");
        ObservedActivity observedActivity = new UserSessionObservedActivity(userToken.getUid(), "userSessionRenewal", applicationTokenId);
        MonitorReporter.reportActivity(observedActivity);

    }

    public static UserToken refreshUserToken(String applicationTokenId, UserToken refreshedUserToken) {
        //UserToken oldusertoken = activeusertokensmap.remove(usertokenid);
        return addUserToken(refreshedUserToken, applicationTokenId, "refresh");

    }

    private static String generateID() {
        return UUID.randomUUID().toString();
    }

    public static UserToken addUserToken(UserToken userToken, String applicationTokenId, String authType) {
        if (!UserTokenId.isValid(userToken.getUserTokenId())) {
            log.error("Error: UserToken has no valid usertokenid, generating new userTokenId");
            userToken.setUserTokenId(generateID());
        }

        long applicationUserTokenLifespanInSeconds = ApplicationModelHelper.getUserTokenLifeSpanSecondsFromApplication(applicationTokenId);
        log.debug("addUserToken: found applicationUserTokenLifespanInSeconds {} for ApplicationID:{}", applicationUserTokenLifespanInSeconds, ApplicationModelHelper.getApplicationId(new ApplicationTokenID(applicationTokenId)));
        if (applicationUserTokenLifespanInSeconds < DEFAULT_USER_SESSION_TIME_IN_SECONDS) {
            userToken.setLifespan(String.valueOf(applicationUserTokenLifespanInSeconds * 1000));
        } else {
            userToken.setLifespan(String.valueOf(DEFAULT_USER_SESSION_TIME_IN_SECONDS * 1000));
        }
        userToken.setTimestamp(String.valueOf(System.currentTimeMillis()));


        if (userToken.getEmail() != null) {
            String lastSeenString = AuthenticatedUserTokenRepository.getLastSeen(userToken);
            userToken.setLastSeen(lastSeenString);
            lastSeenMap.put(userToken.getEmail(), new Date());

        }

        activeusertokensmap.put(userToken.getUserTokenId(), userToken);
        log.info("Added userToken with id {}", userToken.getUserTokenId(), " content:" + userToken.toString());

        if (userToken.getUserName() != null) {
            active_username_usertokenids_map.put(userToken.getUserName(), userToken.getUserTokenId());
        }
        if ("renew".equalsIgnoreCase(authType)) {
            return userToken;  // alreqdy reported
        }
        if ("refresh".equalsIgnoreCase(authType)) {
            return userToken;  // alreqdy reported
        }
        ObservedActivity observedActivity = new UserSessionObservedActivity(userToken.getUid(), "userSessionCreatedByPassword", applicationTokenId);
        if ("pin".equalsIgnoreCase(authType)) {
            observedActivity = new UserSessionObservedActivity(userToken.getUid(), "userSessionCreatedByPin", applicationTokenId);
        }
        MonitorReporter.reportActivity(observedActivity);
        return userToken;
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
//        String logString = "";
//        for (Map.Entry<String, UserToken> entry : activeusertokensmap.entrySet()) {
//            logString = logString + entry.getValue().getUserTokenId() + "(" + entry.getValue().getUserName() + "), ";
//        }
        log.debug("UserToken mapsize :{} - content:{}", activeusertokensmap.size(), activeusertokensmap);
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
                log.debug("Removed userTokenID {} - marked as icvalid", userToken.getUserTokenId());
                activeusertokensmap.remove(userToken.getUserTokenId());
            }
        }
    }

}
