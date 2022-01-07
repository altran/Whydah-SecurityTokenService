package net.whydah.sts.application;

import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.session.baseclasses.ApplicationModelUtil;
import net.whydah.sts.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ApplicationModelFacade {

    private final static Logger log = LoggerFactory.getLogger(ApplicationModelFacade.class);

    public static final URI userAdminServiceUri;
    private final static AppConfig appConfig = new AppConfig();
    final static int APPLICATION_UPDATE_CHECK_INTERVAL_IN_SECONDS = 60; // update 60 secs

    static final ScheduledExecutorService app_update_scheduler;

    static {
        userAdminServiceUri = URI.create(appConfig.getProperty("useradminservice"));
        app_update_scheduler = Executors.newScheduledThreadPool(1);
        app_update_scheduler.scheduleWithFixedDelay(ApplicationModelFacade::doUpdateApplicationListTask,
                5, APPLICATION_UPDATE_CHECK_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
    }

    private static void doUpdateApplicationListTask() {
        try {
            updateApplicationList();
        } catch (Exception ex) {
            log.warn("", ex);
        }
    }

    public static Application getApplication(String applicationID) {
        Application app = ApplicationModelUtil.getApplication(applicationID);
        if (app == null) {
            try {
                Future<?> future = app_update_scheduler.submit(ApplicationModelFacade::doUpdateApplicationListTask);
                future.get(20, TimeUnit.SECONDS);
                return ApplicationModelUtil.getApplication(applicationID);
            } catch (Exception e) {
                log.warn("", e);
            }
        }
        return app;
    }

    public static List<Application> getApplicationList() {
        return ApplicationModelUtil.getApplicationList();
    }

    private static void updateApplicationList() {
        log.debug("Update application list");

        ApplicationToken stsApplicationToken = AuthenticatedApplicationTokenRepository.getSTSApplicationToken();
        ApplicationModelUtil.updateApplicationList(userAdminServiceUri, stsApplicationToken.getApplicationTokenId());

        //update full sts from applications
        String fTokenList = "";
        for (Application application : ApplicationModelUtil.getApplicationList()) {
            if (application.isFullTokenApplication()) {
                fTokenList = fTokenList + application.getId() + ",";
            }
        }

        //add predefined list
        for (String app : AppConfig.getPredefinedFullTokenApplications()) {
            if (!fTokenList.contains(app)) {
                fTokenList = fTokenList + app + ",";
            }
        }

        //assign to the final list
        fTokenList = fTokenList.endsWith(",") ? fTokenList.substring(0, fTokenList.length() - 1) : fTokenList;
        AppConfig.setFullTokenApplications(fTokenList);
        log.debug("Full token list: " + fTokenList);
    }

}
