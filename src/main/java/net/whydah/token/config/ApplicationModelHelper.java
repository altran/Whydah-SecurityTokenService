package net.whydah.token.config;

import net.whydah.sso.user.mappers.UserTokenMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class ApplicationModelHelper {
    private static final Logger log = LoggerFactory.getLogger(ApplicationModelHelper.class);
    private static String myApplicationList = "[{}]";

    public static String getApplicationList() {
        return myApplicationList;
    }

    public static void setApplicationList(String appLinks) {
        myApplicationList = appLinks;
    }

    public static void updateApplicationList(URI userAdminServiceUri, String myAppTokenId, String userTokenXML) {
        if (shouldUpdate() || getApplicationList() == null || myApplicationList.length() < 6) {
            String userTokenId = UserTokenMapper.fromUserTokenXml(userTokenXML).getTokenid();
            String applicationsJson = new net.whydah.sso.commands.adminapi.application.CommandListApplications(userAdminServiceUri, myAppTokenId, userTokenId, "").execute();
            log.debug("AppLications returned:" + applicationsJson);
            if (applicationsJson != null) {
                if (applicationsJson.length() > 20) {
                    setApplicationList(applicationsJson);
                }
            }
        }
    }

    public static void forcedUpdateApplicationList(URI userAdminServiceUri, String myAppTokenId, String userTokenXML) {
        String userTokenId = UserTokenMapper.fromUserTokenXml(userTokenXML).getTokenid();
        String applicationsJson = new net.whydah.sso.commands.adminapi.application.CommandListApplications(userAdminServiceUri, myAppTokenId, userTokenId, "").execute();
        log.debug("AppLications returned:" + applicationsJson);
        if (applicationsJson != null) {
            if (applicationsJson.length() > 20) {
                setApplicationList(applicationsJson);
            }
        }
    }

    public static boolean shouldUpdate() {
        int max = 1000;
        return (5 >= ((int) (Math.random() * max)));  // update on 0.5 percent of requests
    }

}
