package net.whydah.token.config;

import net.whydah.sso.application.helpers.ApplicationHelper;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.user.mappers.UserTokenMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

/**
 * Helper methods for reading configuration.
 */
public class AppConfig {
    public final static String IAM_CONFIG_KEY = "IAM_CONFIG";
    private final static Logger log = LoggerFactory.getLogger(AppConfig.class);

    private static Properties properties=null;
    private static String fullTokenApplications = "2210,2211,2212,2215,2219";
    Random rand = new Random();

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public AppConfig() {
        if (rand.nextInt(100)>95 || properties==null) {  // reload properties on 5% of the calls or if not loaded
            try {
                properties = readProperties(ApplicationMode.getApplicationMode());
                logProperties(properties);
                fullTokenApplications = getProperty("fulltokenapplications");
            } catch (IOException e) {
                throw new RuntimeException(e.getLocalizedMessage(), e);
            }
        }
    }

    private void logProperties(Properties properties) {
        Set keys = properties.keySet();
        for (Object key : keys) {
            log.info("Property: {}, value {}", key, properties.getProperty((String) key));
        }
    }

    private static Properties readProperties(String appMode) throws IOException {
        Properties properties = loadFromClasspath(appMode);

        String configfilename = System.getProperty(IAM_CONFIG_KEY);
        if(configfilename != null) {
            loadFromFile(properties, configfilename);
        }
        return properties;
    }

    private static Properties loadFromClasspath(String appMode) throws IOException {
        Properties properties = new Properties();
        String propertyfile = String.format("securitytokenservice.%s.properties", appMode);
        log.info("Loading default properties from classpath: {}", propertyfile);
        InputStream is = AppConfig.class.getClassLoader().getResourceAsStream(propertyfile);
        if(is == null) {
            log.error("Error reading {} from classpath.", propertyfile);
            System.exit(3);
        }
        properties.load(is);
        return properties;
    }

    private static void loadFromFile(Properties properties, String configfilename) throws IOException {
        File file = new File(configfilename);
        //System.out.println(file.getAbsolutePath());
        log.info("Overriding default properties with file {}", file.getAbsolutePath());
        if(file.exists()) {
            properties.load(new FileInputStream(file));
        } else {
            log.error("Config file {} specified by System property {} not found.", configfilename, IAM_CONFIG_KEY);
            System.exit(3);
        }
    }


    public static String getFullTokenApplications() {
        return fullTokenApplications;
    }

    public static void setFullTokenApplications(String appLinks) {
        fullTokenApplications = appLinks;
        log.debug("updated fulltoken list: "+appLinks);
    }


    public static void updateApplinks(URI userAdminServiceUri, String myAppTokenId, String responseXML) {
        log.trace("updateApplinks(URI userAdminServiceUri:{}, String myAppTokenId:{}, String responseXML:{}",userAdminServiceUri,  myAppTokenId,  responseXML);
        if (responseXML == null || responseXML.length() < 2) {
            // Ignore empty responses
            return;
        }
        if (shouldUpdate() || getFullTokenApplications() == null || fullTokenApplications.length() < 6) {
            String userTokenId = UserTokenMapper.fromUserTokenXml(responseXML).getTokenid();
            String applicationsJson = new CommandListApplications(userAdminServiceUri, myAppTokenId, userTokenId, "").execute();
            log.debug("AppLications returned:" + applicationsJson);
            List<Application> applications = ApplicationMapper.fromJsonList(applicationsJson);
            String fTokenList="";
            for (Application application : applications) {
                if (application.isFullTokenApplication()){
                    fTokenList=fTokenList+application.getId()+",";
                    log.debug("is fulltoken {} appid:{}", application.isFullTokenApplication(), application.getId());
                }

            }
            if (fTokenList==null || fTokenList.length()<3){
                // Some error occured, ignore data
                return;
            }

            setFullTokenApplications(fTokenList.substring(0,fTokenList.length()-1));
        }
    }


    public static boolean shouldUpdate() {
        int max = 100;
        return (5 >= ((int) (Math.random() * max)));  // update on 5 percent of requests
    }

}
