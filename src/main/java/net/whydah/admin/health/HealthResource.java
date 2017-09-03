package net.whydah.admin.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import net.whydah.sso.util.WhydahUtil;
import net.whydah.sso.whydah.ThreatSignal;
import net.whydah.token.application.ApplicationModelFacade;
import net.whydah.token.application.ApplicationThreatResource;
import net.whydah.token.application.AuthenticatedApplicationTokenRepository;
import net.whydah.token.config.AppConfig;
import net.whydah.token.user.ActivePinRepository;
import net.whydah.token.user.ActiveUserTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Endpoint for health check
 */
@Path("/health")
public class HealthResource {
    private static final Logger log = LoggerFactory.getLogger(HealthResource.class);

    private static Map<String, ThreatSignal> threatSignalMap = new TreeMap<>();
    private static ObjectMapper mapper = new ObjectMapper();

    static {
        AppConfig appConfig = new AppConfig();
        String xmlFileName = System.getProperty("hazelcast.config");
        log.info("Loading hazelcast configuration from :" + xmlFileName);
        Config hazelcastConfig = new Config();
        if (xmlFileName != null && xmlFileName.length() > 10) {
            try {
                hazelcastConfig = new XmlConfigBuilder(xmlFileName).build();
                log.info("Loading hazelcast configuration from :" + xmlFileName);
            } catch (FileNotFoundException notFound) {
                log.error("Error - not able to load hazelcast.xml configuration.  Using embedded as fallback");
            }
        }
        hazelcastConfig.setProperty("hazelcast.logging.type", "slf4j");
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig);
        threatSignalMap = hazelcastInstance.getMap(appConfig.getProperty("gridprefix") + "threatSignalMap");
        log.info("Connecting to threatSignalMap {}", appConfig.getProperty("gridprefix") + "threatSignalMap");
        log.info("Loaded threatSignalMap size=" + threatSignalMap.size());

    }

    public HealthResource() {
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response isHealthy() throws Exception {

        log.trace("isHealthy={}", getHealthText());
        return Response.ok(getHealthTextJson()).build();

    }

    public static String getHealthText() {
        return "Status: OK" +
                "\nVersion:" + getVersion() +
                "\nDEFCON: " + ApplicationThreatResource.getDEFCON() +
                "\nClusterSize: " + ActiveUserTokenRepository.getNoOfClusterMembers() +
                "\nActiveUserTokenMapSize: " + ActiveUserTokenRepository.getMapSize() +
                "\nLastSeenMapSize: " + ActiveUserTokenRepository.getLastSeenMapSize() +
                "\nPinMapSize: " + ActivePinRepository.getPinMap().size() +
                "\nAuthenticatedApplicationRepositoryMapSize: " + AuthenticatedApplicationTokenRepository.getMapSize();
    }

    public static String getHealthTextJson() {
        int applicationMapSize = 0;
        try {
            applicationMapSize = ApplicationModelFacade.getApplicationList().size();
            return "{\n" +
                    "  \"Status\": \"OK\",\n" +
                    "  \"Version\": \"" + getVersion() + "\",\n" +
                    "  \"DEFCON\": \"" + ApplicationThreatResource.getDEFCON() + "\",\n" +
                    "  \"ClusterSize\": " + ActiveUserTokenRepository.getNoOfClusterMembers() + ",\n" +
                    "  \"ApplicationMapSize\": " + applicationMapSize + ",\n" +
                    "  \"ActiveUserTokenMapSize\": " + ActiveUserTokenRepository.getMapSize() + ",\n" +
                    "  \"LastSeenMapSize\": " + ActiveUserTokenRepository.getLastSeenMapSize() + ",\n" +
                    "  \"PinMapSize\": " + ActivePinRepository.getPinMap().size() + ",\n" +
                    "  \"ThreatSignalMapSize\": " + threatSignalMap.size() + ",\n" +
                    "  \"AuthenticatedApplicationRepositoryMapSize\": " + AuthenticatedApplicationTokenRepository.getMapSize() + ",\n" +
                    "  \"Active Applications\": \"" + AuthenticatedApplicationTokenRepository.getActiveApplications().replace(",", ",\n                          ") + "\",\n" +
                    "  \"now\": \"" + Instant.now() + "\",\n" +
                    "  \"running since\": \"" + WhydahUtil.getRunningSince() + "\"," +
                    "  \n\n" +
                    getThreatMapDetails() +
                    "}\n\n";
        } catch (Exception e) {
            return "{\n" +
                    "  \"Status\": \"UNCONNECTED\",\n" +
                    "  \"Version\": \"" + getVersion() + "\",\n" +
                    "  \"DEFCON\": \"" + ApplicationThreatResource.getDEFCON() + "\",\n" +
                    "}\n\n";

        }
    }


    private static String getThreatMapDetails() {
        String threatSignalJson = " ";
//        if (valid user with right role)  // todo:  Implement this limitation
        try {
            // add minor json prettifying intendation
            threatSignalJson = "  " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(threatSignalMap).replace("\n", "\n  ");
            return "  \"Threat Signals\": \n" + threatSignalJson + "\n";
        } catch (Exception e) {
            return "";
        }


    }

    private static String getVersion() {
        Properties mavenProperties = new Properties();
        String resourcePath = "/META-INF/maven/net.whydah.token/SecurityTokenService/pom.properties";
        URL mavenVersionResource = HealthResource.class.getResource(resourcePath);
        if (mavenVersionResource != null) {
            try {
                mavenProperties.load(mavenVersionResource.openStream());
                return mavenProperties.getProperty("version", "missing version info in " + resourcePath);
            } catch (IOException e) {
                log.warn("Problem reading version resource from classpath: ", e);
            }
        }
        return "(DEV VERSION)";
    }

    public static void addThreatSignal(ThreatSignal signal) {
        threatSignalMap.put(Instant.now().toString(), signal);
    }
}
