package net.whydah.token.user;

import com.google.inject.Singleton;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;

import net.minidev.json.JSONArray;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserToken;
import net.whydah.sso.whydah.DEFCON;
import net.whydah.token.application.ApplicationModelFacade;
import net.whydah.token.application.AuthenticatedApplicationRepository;
import net.whydah.token.config.AppConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilderFactory;

import java.util.*;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 03.11.14
 */
@Singleton
public class UserTokenFactory {
    static final String TOKEN_ISSUER = "/token/TOKEN_ISSUER/tokenverifier";

    private static final Logger log = LoggerFactory.getLogger(UserTokenFactory.class);
    private static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    private static String defcon = DEFCON.DEFCON5.toString();
    private static String lifespanMs;

    private static AppConfig appConfig = new AppConfig();

    @Deprecated
    public UserTokenFactory() {
        this(appConfig.getProperty("DEFCON"));
    }

    public UserTokenFactory(String defcon) {
        this.defcon = defcon;
        lifespanMs = String.valueOf(14 * 24 * 60 * 60 * 1000); //14 days, reduce when refresh is implemented.
    }

    private static String generateID() {
        return UUID.randomUUID().toString();
    }

    public static void setDefcon(String defcon) {
        UserTokenFactory.defcon = defcon;
    }

    public static boolean shouldReturnFullUserToken(String applicationID) {
    	if(applicationID==null || applicationID.length()==0){
    		return false;
    	}
    	//check from the property files first
        String[] applicationIDs = AppConfig.getFullTokenApplications().split(",");
        for (int i = 0; i < applicationIDs.length; i++){
            if (applicationIDs[i].equalsIgnoreCase(applicationID)) {
                log.info("shouldReturnFullUserToken from properties=true");
                return true;
            }
        }
        //check if the application has been configured without filtering
        Application app = ApplicationModelFacade.getApplication(applicationID);
        if(app!=null){
        	return app.isFullTokenApplication();
        }
        return false;
    }

    public static boolean shouldReturnAnonymousUserToken(String applicationID, UserToken userToken) {
        if ("true".equalsIgnoreCase(appConfig.getProperty("ANONYMOUSTOKEN"))) {
            List<UserApplicationRoleEntry> origRoleList = userToken.getRoleList();
            log.info("shouldReturnAnonymousUserToken - ANONYMOUSTOKEN active");

            for (int i = 0; i < origRoleList.size(); i++) {
                UserApplicationRoleEntry are = origRoleList.get(i);
                if (are.getApplicationId().equalsIgnoreCase(applicationID)) {
                    // Role found, no ANONYMOUSTOKEN filtering
                    log.info("shouldReturnAnonymousUserToken - found expected role, returning false");
                    return false;
                }
            }
            // Role not found,ANONYMOUSTOKEN flag set - we should ANONYMOUSTOKEN
            return true;

        } else {
            // No ANONYMOUSTOKEN configured
            return false;
        }

    }

    public static UserToken fromUserIdentityJson(String userIdentityJSON) {
        UserToken userToken = UserTokenMapper.fromUserIdentityJson(userIdentityJSON);
        userToken.setTokenid(generateID());
        userToken.setTimestamp(String.valueOf(System.currentTimeMillis()));
        String securityLevel = "1";
        userToken.setSecurityLevel(securityLevel);
        return userToken;
    }

    public static List<UserToken> fromUsersIdentityJson(String usersIdentityJSON) {
        List<UserToken> userTokens = new ArrayList<>();
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(usersIdentityJSON);
        JSONArray users  = JsonPath.read(document, "$.result");

        for (int i = 0; i < users.size(); i++) {
            LinkedHashMap user = (LinkedHashMap) users.get(i);
            UserToken userToken = new UserToken();
            userToken.setUid((String) user.get("uid"));
            userToken.setTokenid(generateID());
            userToken.setUserName((String) user.get("username"));
            userToken.setFirstName((String) user.get("firstName"));
            userToken.setLastName((String) user.get("lastName"));
            userToken.setEmail((String) user.get("email"));
            userToken.setPersonRef((String) user.get("personRef"));
            userToken.setCellPhone((String) user.get("cellPhone"));
            userTokens.add(userToken);
        }

        return userTokens;
    }




    public static UserToken getFilteredUserToken(String applicationTokenID,UserToken userToken) {

        String myappid = AuthenticatedApplicationRepository.getApplicationIdFromApplicationTokenID(applicationTokenID);
        log.info("getFilteredUserToken - found appid={}",myappid);
        if (shouldReturnFullUserToken(myappid)){
            log.info("getFilteredUserToken - no filtering");
            return userToken;
        }
        if (shouldReturnAnonymousUserToken(myappid, userToken)) {
            log.warn("shouldReturnAnonymousUserToken = TRUE");
            userToken.setUserName("");
            userToken.setEmail("");
            userToken.setFirstName("");
            userToken.setLastName("Demographics: Oslo");
            log.info("getFilteredUserToken - returning anonymous token");
            List<UserApplicationRoleEntry> roleList = new ArrayList<>();
            userToken.setRoleList(roleList);
            return userToken;
        } else {
            List<UserApplicationRoleEntry> origRoleList = userToken.getRoleList();
            List<UserApplicationRoleEntry> roleList = new LinkedList<>();
            log.info("getFilteredUserToken - filtering active");

            for (int i=0;i<origRoleList.size();i++){
                UserApplicationRoleEntry are = origRoleList.get(i);
                if (are.getApplicationId().equalsIgnoreCase(myappid)){
                    roleList.add(are);
                }
            }
            userToken.setRoleList(roleList);
            return userToken;
        }
    }

    public static boolean verifyApplicationToken(String applicationtokenid, String applicationtokenXml) {
        // TODO - possibly implement check if apptokenXml is identical from source to repo
        boolean validAppToken = false;
        if (applicationtokenid != null) {
            return AuthenticatedApplicationRepository.verifyApplicationTokenId(applicationtokenid);
        } else {
            log.warn("verifyApplicationToken - not expecting null values applicationtokenid {}, applicationtokenXml {}", applicationtokenid, applicationtokenXml);
            return validAppToken;
        }
    }

}
