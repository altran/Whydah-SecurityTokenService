package net.whydah.token.config;

import net.whydah.sso.application.types.Application;
import net.whydah.sso.util.ApplicationModelUtil;
import java.net.URI;
import java.util.List;

import static net.whydah.sso.util.ApplicationModelUtil.getApplicationList;

public class ApplicationModelHelper {
    private static URI userAdminServiceUri = null;
    private static AppConfig appConfig = new AppConfig();

    static {
        userAdminServiceUri = URI.create(appConfig.getProperty("myuri"));
    }

    public static Application getApplication(String applicationID) {
        ApplicationModelUtil.getApplication(applicationID);
    }

    // TODO temp override
    public static List<Application> getApplicationList() {
        return ApplicationModelUtil.getApplicationList();
    }

    // JsonPath query against Application.json to find value, empty string if not found
    public static String getParameterForApplication(String param, String applicationID) {
        return ApplicationModelUtil.getParameterForApplication(param, applicationID);
    }


    public static void updateApplicationList(String myAppTokenId, String userTokenId) {
        ApplicationModelUtil.updateApplicationList(userAdminServiceUri, myAppTokenId, userTokenId);
    }

    public static void forcedUpdateApplicationList(String myAppTokenId, String userTokenId) {
        ApplicationModelUtil.forcedUpdateApplicationList(userAdminServiceUri, myAppTokenId, userTokenId);
    }

}
