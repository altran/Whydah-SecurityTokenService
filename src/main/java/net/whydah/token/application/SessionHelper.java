package net.whydah.token.application;

import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.user.types.UserToken;
import net.whydah.token.user.AuthenticatedUserTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionHelper {

    private static final Logger log = LoggerFactory.getLogger(UserToken.class);
    public static int defaultlifespan = AuthenticatedUserTokenRepository.DEFAULT_SESSION_EXTENSION_TIME_IN_SECONDS * 1000;

    public static long getApplicationLifeSpan(String applicationtokenid){
        ApplicationToken appToken = AuthenticatedApplicationTokenRepository.getApplicationToken(applicationtokenid);
        if(appToken!=null){
			Application app = ApplicationModelFacade.getApplication(appToken.getApplicationID());
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
            if (maxUserSessionFromApplication > 1) {  // Avoid setting timeout to 0 is missing getMaxSessionTimeoutSeconds.
                if (maxUserSessionFromApplication < AuthenticatedUserTokenRepository.DEFAULT_SESSION_EXTENSION_TIME_IN_SECONDS * 1000) {
                    log.debug("Returning MaxSessionTimeoutSeconds:{} for Application:{}", maxUserSessionFromApplication, app.getName());
                    return maxUserSessionFromApplication;
                }

            }
        }
        //return
        log.debug("Returning defaultlifespan:{} for Application:{}", defaultlifespan, app.getName());

        return defaultlifespan;
	}

}
