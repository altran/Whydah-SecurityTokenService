package net.whydah.token.application;

import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.session.baseclasses.ApplicationModelUtil;
import net.whydah.token.config.AppConfig;

import java.net.URI;
import java.util.List;


public class ApplicationModelFacade {
    public static URI userAdminServiceUri = null;
    private static AppConfig appConfig = new AppConfig();

    public static String fTokenList = "";

    static {
        userAdminServiceUri = URI.create(appConfig.getProperty("useradminservice"));
    }

    public static Application getApplication(String applicationID) {
    	updateApplicationList();
        return ApplicationModelUtil.getApplication(applicationID);
    }

    public static List<Application> getApplicationList() {
    	updateApplicationList();
        return ApplicationModelUtil.getApplicationList();
    }

    public static String getParameterForApplication(String param, String applicationID) {
        return ApplicationModelUtil.getParameterForApplication(param, applicationID);
    }

    public static void updateApplicationList(String myAppTokenId, String userTokenId) {
        
    	ApplicationModelUtil.updateApplicationList(userAdminServiceUri, myAppTokenId, userTokenId);
    }
    
    public static void updateApplicationList() {

        if (ApplicationModelUtil.shouldUpdate() || fTokenList == null || fTokenList.length() < 2) {
            //get all applications from UAS
            ApplicationToken token = AuthenticatedApplicationTokenRepository.getSTSApplicationToken();
            ApplicationModelUtil.updateApplicationList(userAdminServiceUri, token.getApplicationTokenId());
    		
    		//update full token from applications
            fTokenList = "";
            for (Application application : ApplicationModelUtil.getApplicationList()) {
    			if (application.isFullTokenApplication()) {
    				fTokenList = fTokenList + application.getId() + ",";
    			}
    		}
    		
    		//add predefined list
    		for(String app : AppConfig.getPredefinedFullTokenApplications()){
    			if(!fTokenList.contains(app)){
    				fTokenList = fTokenList + app + ",";
    			}
    		}
    		
    		//assign to the final list
    		fTokenList = fTokenList.endsWith(",")?fTokenList.substring(0, fTokenList.length() - 1):fTokenList;
    		AppConfig.setFullTokenApplications(fTokenList);
    		
    	}
    }
   
}
