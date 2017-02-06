package net.whydah.token.application;

import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.user.types.UserToken;
import net.whydah.token.config.ApplicationModelHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionHelper {

    private static final Logger log = LoggerFactory.getLogger(UserToken.class);

    public static int defaultlifespan = 14 * 24 * 60 * 60 * 1000;  // 14 days  245000 = 4 seconds;

    // String.valueOf(14 * 24 * 60 * 60 * 1000);
    public static long getApplicationLifeSpan(String applicationtokenid){

		ApplicationToken appToken = AuthenticatedApplicationRepository.getApplicationToken(applicationtokenid);
		
		if(appToken!=null){
			Application app = ApplicationModelHelper.getApplication(appToken.getApplicationID());

			//set the correct timeout depends on the application's security
			if(app!=null){
			
				return getApplicationLifeSpan(app);
			}
		}
		
		return defaultlifespan;
	}
	
	public static long getApplicationLifeSpan(Application app) {
		//TODO: a correlation between securityLevel and lifespan?
        if (app.getSecurity() != null) {
            long maxUserSessionFromApplication = Long.valueOf(app.getSecurity().getMaxSessionTimeoutSeconds());
            if (maxUserSessionFromApplication > 22450000) {
                log.debug("Returning MaxSessionTimeoutSeconds:{} for Application:{}", maxUserSessionFromApplication, app.getName());
                return maxUserSessionFromApplication;
            }
        }
        //return

        return defaultlifespan;
	}

}
