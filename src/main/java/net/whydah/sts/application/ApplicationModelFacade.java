package net.whydah.sts.application;

import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.session.baseclasses.ApplicationModelUtil;
import net.whydah.sso.whydah.TimeLimitedCodeBlock;
import net.whydah.sts.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;


public class ApplicationModelFacade {
    public static URI userAdminServiceUri = null;
    private static AppConfig appConfig = new AppConfig();
    private final static Logger log = LoggerFactory.getLogger(ApplicationModelFacade.class);


    public static String xfTokenList = "";

    static {
        userAdminServiceUri = URI.create(appConfig.getProperty("useradminservice"));
    }

    public static Application getApplication(String applicationID) {
        long startTime = System.currentTimeMillis();
        try {
            TimeLimitedCodeBlock.runWithTimeout(new Callable<String>() {
                @Override
                public String call() {
                    logTimedCode(startTime, "starting processing!");
                    updateApplicationList(99);
                    logTimedCode(startTime, "processing completed!");
                    return "";
                }

            }, 20, TimeUnit.SECONDS);
        } catch (Exception e) {
            logTimedCode(startTime, "was interrupted!");
        }
        return ApplicationModelUtil.getApplication(applicationID);
    }

    public static List<Application> getApplicationList() {
        long startTime = System.currentTimeMillis();
        try {
            TimeLimitedCodeBlock.runWithTimeout(new Callable<String>() {
                @Override
                public String call() {
                    logTimedCode(startTime, "starting processing!");
                    updateApplicationList(99);
                    logTimedCode(startTime, "processing completed!");
                    return "";
                }

            }, 20, TimeUnit.SECONDS);
        } catch (Exception e) {
            logTimedCode(startTime, "was interrupted!");
        }
        return ApplicationModelUtil.getApplicationList();
    }

    public static String getParameterForApplication(String param, String applicationID) {
        return ApplicationModelUtil.getParameterForApplication(param, applicationID);
    }

    public static void updateApplicationList(String myAppTokenId, String userTokenId) {

        ApplicationModelUtil.updateApplicationList(userAdminServiceUri, myAppTokenId, userTokenId);
    }

    public static void updateApplicationList(int percentage) {


        if (ApplicationModelUtil.shouldUpdate(percentage) || ApplicationModelUtil.getApplicationList() == null || ApplicationModelUtil.getApplicationList().size() == 0) {
//        if (ApplicationModelUtil.shouldUpdate(percentage) || fTokenList == null || fTokenList.length() < 2) {
            //get all applications from UAS
            ApplicationToken stsApplicationToken = AuthenticatedApplicationTokenRepository.getSTSApplicationToken();
            ApplicationModelUtil.updateApplicationList(userAdminServiceUri, stsApplicationToken.getApplicationTokenId());
        }

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


    }

    private static void logTimedCode(long startTime, String msg) {
        long elapsedSeconds = (System.currentTimeMillis() - startTime);
        log.info("{}ms [{}] {}\n", elapsedSeconds, Thread.currentThread().getName(), msg);
    }
}
