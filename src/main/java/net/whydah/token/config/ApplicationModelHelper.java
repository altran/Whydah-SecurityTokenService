package net.whydah.token.config;

import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.basehelpers.JsonPathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

public class ApplicationModelHelper {
    private static final Logger log = LoggerFactory.getLogger(ApplicationModelHelper.class);
    private static List<Application> applications;
    private static URI userAdminServiceUri = null;
    private static AppConfig appConfig = new AppConfig();

    public static final String maxSessionTimeoutSeconds = "$.security.maxSessionTimoutSeconds";
    public static final String minDEFCON = "$.security.minDEFCON";
    public static final String minSecurityLevel = "$.security.minSecurityLevel";


    static {
        userAdminServiceUri = URI.create(appConfig.getProperty("myuri"));

    }


    public static List<Application> getApplicationList() {
        return applications;
    }

    // JsonPath query against Application.json to find value, empty string if not found
    public static String getParameterForApplication(String param, String applicationID) {
        if (applications == null) {
            return "";
        }
        try {
            for (Application application : applications) {
                if (applicationID.equalsIgnoreCase(application.getId())) {
                    log.info("Found application, looking for ", param);
                    return JsonPathHelper.findJsonPathValue(ApplicationMapper.toJson(application), param);
                }
            }
        } catch (Exception e) {
            log.warn("Attempt to find {} from applicationID: {} failed. returning empty string.", param, applicationID);
        }
        return "";

    }


    public static void updateApplicationList(String myAppTokenId, String userTokenId) {
        if (shouldUpdate() || getApplicationList() == null || applications.size() < 6) {
            String applicationsJson = new net.whydah.sso.commands.adminapi.application.CommandListApplications(userAdminServiceUri, myAppTokenId, userTokenId, "").execute();
            log.debug("AppLications returned:" + applicationsJson);
            if (applicationsJson != null) {
                if (applicationsJson.length() > 20) {
                    applications = ApplicationMapper.fromJsonList(applicationsJson);
                }
            }
        }
    }

    public static void forcedUpdateApplicationList(String myAppTokenId, String userTokenId) {
        String applicationsJson = new net.whydah.sso.commands.adminapi.application.CommandListApplications(userAdminServiceUri, myAppTokenId, userTokenId, "").execute();
        log.debug("AppLications returned:" + applicationsJson);
        if (applicationsJson != null) {
            if (applicationsJson.length() > 20) {
                applications = ApplicationMapper.fromJsonList(applicationsJson);
            }
        }
    }

    public static boolean shouldUpdate() {
        int max = 1000;
        return (5 >= ((int) (Math.random() * max)));  // update on 0.5 percent of requests
    }

}
