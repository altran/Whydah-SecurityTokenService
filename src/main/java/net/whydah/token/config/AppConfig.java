package net.whydah.token.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * Helper methods for reading configuration.
 */
public class AppConfig {
    public final static String IAM_CONFIG_KEY = "IAM_CONFIG";
    private final static Logger logger = LoggerFactory.getLogger(AppConfig.class);

    private final Properties properties;

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public AppConfig() {
        try {
            properties = readProperties(ApplicationMode.getApplicationMode());
            logProperties(properties);
        } catch (IOException e) {
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    private void logProperties(Properties properties) {
        Set keys = properties.keySet();
        for (Object key : keys) {
            logger.info("Property: {}, value {}", key, properties.getProperty((String) key));
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
        logger.info("Loading default properties from classpath: {}", propertyfile);
        InputStream is = AppConfig.class.getClassLoader().getResourceAsStream(propertyfile);
        if(is == null) {
            logger.error("Error reading {} from classpath.", propertyfile);
            System.exit(3);
        }
        properties.load(is);
        return properties;
    }

    private static void loadFromFile(Properties properties, String configfilename) throws IOException {
        File file = new File(configfilename);
        //System.out.println(file.getAbsolutePath());
        logger.info("Overriding default properties with file {}", file.getAbsolutePath());
        if(file.exists()) {
            properties.load(new FileInputStream(file));
        } else {
            logger.error("Config file {} specified by System property {} not found.", configfilename, IAM_CONFIG_KEY);
            System.exit(3);
        }
    }
}
