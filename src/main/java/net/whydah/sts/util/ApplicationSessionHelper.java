package net.whydah.sts.util;

import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sts.application.ApplicationModelFacade;
import net.whydah.sts.application.AuthenticatedApplicationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.whydah.sts.application.AuthenticatedApplicationTokenRepository.DEFAULT_APPLICATION_SESSION_EXTENSION_TIME_IN_SECONDS;

public class ApplicationSessionHelper {

    private static final Logger log = LoggerFactory.getLogger(ApplicationSessionHelper.class);
    public static long defaultlifespan = DEFAULT_APPLICATION_SESSION_EXTENSION_TIME_IN_SECONDS * 1000;

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
                if (maxUserSessionFromApplication / 1000 < DEFAULT_APPLICATION_SESSION_EXTENSION_TIME_IN_SECONDS) {
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
