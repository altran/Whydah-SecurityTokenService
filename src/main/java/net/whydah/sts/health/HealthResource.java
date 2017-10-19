package net.whydah.sts.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import net.whydah.sso.util.WhydahUtil;
import net.whydah.sso.whydah.ThreatSignal;
import net.whydah.sts.application.ApplicationModelFacade;
import net.whydah.sts.application.AuthenticatedApplicationTokenRepository;
import net.whydah.sts.config.AppConfig;
import net.whydah.sts.threat.ThreatResource;
import net.whydah.sts.user.AuthenticatedUserTokenRepository;
import net.whydah.sts.user.authentication.ActivePinRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Endpoint for health check
 */
@Path("/health")
public class HealthResource {
    private static final Logger log = LoggerFactory.getLogger(HealthResource.class);

    private static Map<String, ThreatSignal> threatSignalMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private static ObjectMapper mapper = new ObjectMapper();
    private static boolean isExtendedInfoEnabled = false;

    static {
        AppConfig appConfig = new AppConfig();
        try {
            isExtendedInfoEnabled = (appConfig.getProperty("testpage").equalsIgnoreCase("enabled"));
        } catch (Exception e) {
            log.info("Ubanle to find testpage property or property file, returning isExtendedInfoEnabled=false");
        }
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
                "\nDEFCON: " + ThreatResource.getDEFCON() +
                "\nmax application session time: " + AuthenticatedApplicationTokenRepository.DEFAULT_SESSION_EXTENSION_TIME_IN_SECONDS +
                "\nClusterSize: " + AuthenticatedUserTokenRepository.getNoOfClusterMembers() +
                "\nUserLastSeenMapSize: " + AuthenticatedUserTokenRepository.getLastSeenMapSize() +
                "\nUserPinMapSize: " + ActivePinRepository.getPinMap().size() +
                "\nAuthenticatedTokenMapSize: " + AuthenticatedUserTokenRepository.getMapSize() +
                "\nAuthenticatedApplicationKeyMapSize: " + AuthenticatedApplicationTokenRepository.getKeyMapSize() +
                "\nAuthenticatedApplicationRepositoryMapSize: " + AuthenticatedApplicationTokenRepository.getMapSize();
    }

    public static String getHealthTextJson() {
        int applicationMapSize = 0;
        try {
            applicationMapSize = ApplicationModelFacade.getApplicationList().size();
            if (isExtendedInfoEnabled) {
                return "{\n" +
                        "  \"Status\": \"OK\",\n" +
                        "  \"Version\": \"" + getVersion() + "\",\n" +
                        "  \"DEFCON\": \"" + ThreatResource.getDEFCON() + "\",\n" +
                        "  \"max application session time (s)\": \"" + AuthenticatedApplicationTokenRepository.DEFAULT_SESSION_EXTENSION_TIME_IN_SECONDS + "\",\n" +
                        "  \"ClusterSize\": \"" + AuthenticatedUserTokenRepository.getNoOfClusterMembers() + "\",\n" +
                        "  \"UserLastSeenMapSize\": \"" + AuthenticatedUserTokenRepository.getLastSeenMapSize() + "\",\n" +
                        "  \"UserPinMapSize\": \"" + ActivePinRepository.getPinMap().size() + "\",\n" +
                        "  \"AuthenticatedUserTokenMapSize\": \"" + AuthenticatedUserTokenRepository.getMapSize() + "\",\n" +
                        "  \"AuthenticatedApplicationRepositoryMapSize\": \"" + AuthenticatedApplicationTokenRepository.getMapSize() + "\",\n" +
                        "  \"AuthenticatedApplicationKeyMapSize\": \"" + AuthenticatedApplicationTokenRepository.getKeyMapSize() + "\",\n" +
                        "  \"ConfiguredApplications\":  \"" + applicationMapSize + "\",\n" +
                        "  \"ActiveApplications\": \"" + AuthenticatedApplicationTokenRepository.getActiveApplications().replace(",", ",\n                          ") + "\",\n" +
                        "  \"ThreatSignalMapSize\": \"" + threatSignalMap.size() + "\",\n" +
                        "  \"now\": \"" + Instant.now() + "\",\n" +
                        "  \"running since\": \"" + WhydahUtil.getRunningSince() + "\"," +
                        "  \n\n" +
                        getThreatMapDetails() +
                        "}\n\n";

            }
            return "{\n" +
                    "  \"Status\": \"OK\",\n" +
                    "  \"Version\": \"" + getVersion() + "\",\n" +
                    "  \"DEFCON\": \"" + ThreatResource.getDEFCON() + "\",\n" +
                    "  \"max application session time (s)\": \"" + AuthenticatedApplicationTokenRepository.DEFAULT_SESSION_EXTENSION_TIME_IN_SECONDS + "\",\n" +
                    "  \"ClusterSize\": \"" + AuthenticatedUserTokenRepository.getNoOfClusterMembers() + "\",\n" +
                    "  \"UserLastSeenMapSize\": \"" + AuthenticatedUserTokenRepository.getLastSeenMapSize() + "\",\n" +
                    "  \"UserPinMapSize\": \"" + ActivePinRepository.getPinMap().size() + "\",\n" +
                    "  \"AuthenticatedUserTokenMapSize\": \"" + AuthenticatedUserTokenRepository.getMapSize() + "\",\n" +
                    "  \"AuthenticatedApplicationRepositoryMapSize\": \"" + AuthenticatedApplicationTokenRepository.getMapSize() + "\",\n" +
                    "  \"ConfiguredApplications\":  \"" + applicationMapSize + "\",\n" +
                    "  \"ThreatSignalMapSize\": \"" + threatSignalMap.size() + "\",\n" +
                    "  \"now\": \"" + Instant.now() + "\",\n" +
                    "  \"running since\": \"" + WhydahUtil.getRunningSince() + "\"" +
                    "}\n\n";

        } catch (Exception e) {
            return "{\n" +
                    "  \"Status\": \"UNCONNECTED\",\n" +
                    "  \"Version\": \"" + getVersion() + "\",\n" +
                    "  \"DEFCON\": \"" + ThreatResource.getDEFCON() + "\",\n" +
                    "  \"now\": \"" + Instant.now() + "\",\n" +
                    "  \"running since\": \"" + WhydahUtil.getRunningSince() + "\"" +
                    "}\n\n";

        }
    }

    private static String getThreatMapDetails() {
        String threatSignalJson = " ";
        //        if (valid user with right role)  // todo:  Implement this limitation
        // using isExtendedInfoEnabled for now

        // Lets trigger map-cleanup first
        AuthenticatedApplicationTokenRepository.cleanApplicationTokenMap();
        AuthenticatedUserTokenRepository.cleanUserTokenMap();


        // OK... let us obfucscate/filter sessionsid's in signalEmitter field
        for (Map.Entry<String, ThreatSignal> entry : threatSignalMap.entrySet()) {
            ThreatSignal threatSignal = entry.getValue();
            threatSignal.setSignalEmitter(threatSignal.getSignalEmitter().replace("a", "*").replace("b", "*").replace("c", "*").replace("d", "*").replace("e", "*"));
            Map<String, Object> additionalProperties = threatSignal.getAdditionalProperties();
        	
            List<String> obfuscateList = Arrays.asList("usertokenid", "apptokenid","appName");
            for(String pro : additionalProperties.keySet()){
            	if(obfuscateList.contains(pro)){
            		threatSignal.getAdditionalProperties().put(pro, threatSignal.getAdditionalProperties().get(pro).toString().replace("a", "*").replace("b", "*").replace("c", "*").replace("d", "*").replace("e", "*"));
            	}
            }
            
            threatSignalMap.put(entry.getKey(), threatSignal);
        }
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
        if (threatSignalMap.size() > 5000) {
            try {
                log.warn("ThreatSignalMap overrun, dumping and clearing");
                log.warn(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(threatSignalMap));
            } catch (Exception e) {
                // Do nothing
            }
            threatSignalMap.clear();
        }
        threatSignalMap.put(Instant.now().toString(), signal);
    }

    public static Instant getRunningSince() {
        long uptimeInMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        return Instant.now().minus(uptimeInMillis, ChronoUnit.MILLIS);
    }

}
