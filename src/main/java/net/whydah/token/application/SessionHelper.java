package net.whydah.token.application;

import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.token.user.AuthenticatedUserTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionHelper {

    private static final Logger log = LoggerFactory.getLogger(SessionHelper.class);
    public static long defaultlifespan = AuthenticatedUserTokenRepository.DEFAULT_SESSION_EXTENSION_TIME_IN_SECONDS * 1000;

    public static long getApplicationLifeSpanSeconds(String applicationtokenid) {
        ApplicationToken appToken = AuthenticatedApplicationTokenRepository.getApplicationToken(applicationtokenid);
        if(appToken!=null){
			Application app = ApplicationModelFacade.getApplication(appToken.getApplicationID());
			//set the correct timeout depends on the application's security
			if(app!=null){
                return getApplicationLifeSpanSeconds(app);
            }
		}
        log.debug("Returning ApplicationToken defaultlifespanseconds:{} for applicationtokenid:{}", defaultlifespan / 1000, applicationtokenid);
        return defaultlifespan;
	}

    public static long getApplicationLifeSpanSeconds(Application app) {
        //TODO: a correlation between securityLevel and lifespan?
        if (app.getSecurity() != null) {
            long maxUserSessionFromApplication = Long.valueOf(app.getSecurity().getMaxSessionTimeoutSeconds());
            if (maxUserSessionFromApplication / 1000 > 10) {  // Avoid setting timeout to 0 is missing getMaxSessionTimeoutSeconds.
                if (maxUserSessionFromApplication / 1000 < AuthenticatedUserTokenRepository.DEFAULT_SESSION_EXTENSION_TIME_IN_SECONDS) {
                    log.debug("Returning ApplicationToken MaxSessionTimeoutSeconds:{} for Application:{}", maxUserSessionFromApplication / 1000, app.getName());
                    return maxUserSessionFromApplication;
                }

            }
        }
        //return
        log.debug("Returning ApplicationToken defaultlifespanseconds:{} for Application:{}", defaultlifespan / 1000, app.getName());

        return defaultlifespan;
	}

}
