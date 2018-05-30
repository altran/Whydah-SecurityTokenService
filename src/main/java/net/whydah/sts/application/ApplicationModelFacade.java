package net.whydah.sts.application;

import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.session.baseclasses.ApplicationModelUtil;
import net.whydah.sso.util.Lock;
import net.whydah.sso.whydah.TimeLimitedCodeBlock;
import net.whydah.sts.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class ApplicationModelFacade {
    public static final URI userAdminServiceUri;
    private static AppConfig appConfig = new AppConfig();
    private final static Logger log = LoggerFactory.getLogger(ApplicationModelFacade.class);
    static int APPLICATION_UPDATE_CHECK_INTERVAL_IN_SECONDS = 60; // update 60 secs 

    public static String xfTokenList = "";
    static ScheduledExecutorService app_update_scheduler; 
    static {
        userAdminServiceUri = URI.create(appConfig.getProperty("useradminservice"));
        app_update_scheduler.scheduleAtFixedRate(
				new Runnable() {
					public void run() {
						try {
							updateApplicationList();
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				},
				5, APPLICATION_UPDATE_CHECK_INTERVAL_IN_SECONDS, TimeUnit.SECONDS); 
    }

    public static Application getApplication(String applicationID) {
        Application app =  ApplicationModelUtil.getApplication(applicationID);
        if(app==null) {       	
        	try {
				TimeLimitedCodeBlock.runWithTimeout(new Runnable() {
					@Override
					public void run() {
						updateApplicationList();
						while(fetchingAppListLock.isLocked()) {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								break;
							}
						}
					}
				}, 20, TimeUnit.SECONDS);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
        	return ApplicationModelUtil.getApplication(applicationID);
        } else {
        	return app;
        }	
    }

    public static List<Application> getApplicationList() {
    	
    	try {
			TimeLimitedCodeBlock.runWithTimeout(new Runnable() {
				@Override
				public void run() {
					updateApplicationList();
					while(fetchingAppListLock.isLocked()) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							break;
						}
					}
				}
			}, 20, TimeUnit.SECONDS);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
        return ApplicationModelUtil.getApplicationList();
    }

    public static String getParameterForApplication(String param, String applicationID) {
        return ApplicationModelUtil.getParameterForApplication(param, applicationID);
    }

//    public static void updateApplicationList(String myAppTokenId, String userTokenId) {
//
//        ApplicationModelUtil.updateApplicationList(userAdminServiceUri, myAppTokenId, userTokenId);
//    }
    
    static Lock fetchingAppListLock = new Lock();

    public static void updateApplicationList() {

    	if(!fetchingAppListLock.isLocked()) {
    		try {
				fetchingAppListLock.lock();
				
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
	    		log.debug("Full token list: "+ fTokenList);
							
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				fetchingAppListLock.unlock();
			}
    		
    	}

    }

//    private static void logTimedCode(long startTime, String msg) {
//        long elapsedSeconds = (System.currentTimeMillis() - startTime);
//        log.info("{}ms [{}] {}\n", elapsedSeconds, Thread.currentThread().getName(), msg);
//    }
}
