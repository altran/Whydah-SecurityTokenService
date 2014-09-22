package net.whydah.token.data.user;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.Map;

public class ActiveUserTokenRepository {
    private final static Logger logger = LoggerFactory.getLogger(ActiveUserTokenRepository.class);
    private static Map<String, UserToken> activeusertokensmap;

    static {
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
        activeusertokensmap = hazelcastInstance.getMap("activeusertokensmap");

    }


    /**
     * Get UserToken from UserTokenRepository. If token is not found or is not valid/timed out, null is returned.
     *
     * @param tokenId userTokenId
     * @return UserToken if found and valid, null if not.
     */
    public static UserToken getUserToken(String tokenId) {
        logger.debug("getUserToken with userTokenid=" + tokenId);
        if (tokenId == null) {
            return null;
        }
        UserToken resToken = activeusertokensmap.get(tokenId);
        if (resToken != null && verifyUserToken(resToken)) {
            logger.info("Valid userToken found: " + resToken);
            logger.debug("userToken=" + resToken);
            return resToken;
        }
        logger.debug("No usertoken found for tokenId=" + tokenId);
        return null;
    }

    /**
     * Check if token exists in UserTokenRepository, and is valid and not timed out.
     *
     * @param token UserToken
     * @return true if token is valid.
     */
    public static boolean verifyUserToken(UserToken token) {
        if (token.getTokenid() == null) {
            logger.info("UserToken not valid, missing tokenId");
            return false;
        }
        UserToken resToken = activeusertokensmap.get(token.getTokenid());
        if (resToken == null) {
            logger.info("UserToken not found in repo.");
            return false;
        }
        logger.debug("UserToken from repo: {}", resToken);
        if (!resToken.isValid()) {
            logger.debug("resToken is not valid");
            activeusertokensmap.remove(token.getTokenid());
            return false;
        }
        if (token.toString().equals(resToken.toString())) {
            return true;
        }
        logger.info("UserToken not valid: not the same as in repo. token: {}  repotoken: {}", token, resToken);
        return false;
    }

    public static void addUserToken(UserToken token) {
        if (token.getTokenid() == null) {
            logger.error("Error: token has net tokenid");
            return;
        }
        if (activeusertokensmap.containsKey(token.getTokenid())) {
            logger.error("Error: trying to update an already existing UserToken in repo..");
            return;
        }
        UserToken copy = token.copy();
        activeusertokensmap.put(copy.getTokenid(), copy);
        logger.info("Added token with id {}", copy.getTokenid(), " content:" + copy);
    }

    public static void removeUserToken(String userTokenId) {
        activeusertokensmap.remove(userTokenId);
    }

    public static void initializeDistributedMap() {
    }

    ;
}
