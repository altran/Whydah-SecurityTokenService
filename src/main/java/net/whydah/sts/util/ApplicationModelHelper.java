package net.whydah.sts.util;

import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.ddd.model.application.ApplicationId;
import net.whydah.sso.ddd.model.application.ApplicationTokenID;
import net.whydah.sts.application.ApplicationModelFacade;
import net.whydah.sts.application.AuthenticatedApplicationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.whydah.sts.user.AuthenticatedUserTokenRepository.DEFAULT_USER_SESSION_EXTENSION_TIME_IN_SECONDS;

public class ApplicationModelHelper {

    private static final Logger log = LoggerFactory.getLogger(ApplicationModelHelper.class);

    public static long getUserTokenLifeSpanSecondsFromApplication(String applicationtokenid) {
        ApplicationToken appToken = AuthenticatedApplicationTokenRepository.getApplicationToken(applicationtokenid);
        if(appToken!=null){
			Application app = ApplicationModelFacade.getApplication(appToken.getApplicationID());
			//set the correct timeout depends on the application's security
			if(app!=null){
                return getUserTokenLifeSpanSeconds(app);
            }
		}
        log.debug("Returning ApplicationToken defaultlifespanseconds:{} for applicationtokenid:{}", DEFAULT_USER_SESSION_EXTENSION_TIME_IN_SECONDS, applicationtokenid);
        return DEFAULT_USER_SESSION_EXTENSION_TIME_IN_SECONDS;
    }

    public static long getUserTokenLifeSpanSeconds(Application app) {
        //TODO: a correlation between securityLevel and lifespan?
        if (app.getSecurity() != null) {
            long maxUserSessionFromApplication = Long.valueOf(app.getSecurity().getMaxSessionTimeoutSeconds());
            if (maxUserSessionFromApplication > 10) {  // Avoid setting timeout to 0 is missing getMaxSessionTimeoutSeconds.
                if (maxUserSessionFromApplication < DEFAULT_USER_SESSION_EXTENSION_TIME_IN_SECONDS) {
                    log.debug("Returning ApplicationToken MaxSessionTimeoutSeconds:{} for Application:{}", maxUserSessionFromApplication, app.getName());
                    return maxUserSessionFromApplication;
                }

            }
        }
        //return
        log.debug("Returning ApplicationToken defaultUserTokenlifespan seconds:{} for Application:{}", DEFAULT_USER_SESSION_EXTENSION_TIME_IN_SECONDS, app.getName());

        return DEFAULT_USER_SESSION_EXTENSION_TIME_IN_SECONDS;
    }

    public static ApplicationId getApplicationId(ApplicationTokenID applicationtokenid) {
        ApplicationToken appToken = AuthenticatedApplicationTokenRepository.getApplicationToken(applicationtokenid.getId());
        if (appToken != null) {
            Application app = ApplicationModelFacade.getApplication(appToken.getApplicationID());

            //set the correct timeout depends on the application's security
            if (app != null) {
                return new ApplicationId(app.getId());
            }
        }
        log.warn("ApplicationId not for for ApplicationTokenId:{}", applicationtokenid);
        return null;
    }

}
