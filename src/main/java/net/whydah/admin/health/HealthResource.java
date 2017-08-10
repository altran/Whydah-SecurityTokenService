package net.whydah.admin.health;

import net.whydah.token.application.ApplicationModelFacade;
import net.whydah.token.application.ApplicationThreatResource;
import net.whydah.token.application.AuthenticatedApplicationRepository;
import net.whydah.token.user.ActivePinRepository;
import net.whydah.token.user.ActiveUserTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Endpoint for health check
 */
@Path("/health")
public class HealthResource {
    private static final Logger log = LoggerFactory.getLogger(HealthResource.class);

    private static List<String> threatSignalList = new LinkedList<String>();

    public HealthResource() {
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response isHealthy() throws Exception {
        
        log.trace("isHealthy={}", getHealthText());
        return Response.ok(getHealthTextJson()).build();
        
    }

    public static String  getHealthText(){
        return "Status: OK"+
                "\nVersion:" + getVersion() +
                "\nDEFCON: "+ ApplicationThreatResource.getDEFCON()+
                "\nClusterSize: " + ActiveUserTokenRepository.getNoOfClusterMembers() +
                "\nActiveUserTokenMapSize: " + ActiveUserTokenRepository.getMapSize() +
                "\nLastSeenMapSize: " + ActiveUserTokenRepository.getLastSeenMapSize() +
                "\nPinMapSize: " + ActivePinRepository.getPinMap().size() +
                "\nAuthenticatedApplicationRepositoryMapSize: " + AuthenticatedApplicationRepository.getMapSize() +
                "\nActive Applications: " + AuthenticatedApplicationRepository.getActiveApplications();
    }

    public static String getHealthTextJson() {
        int applicationMapSize = 0;
        try {
            applicationMapSize = ApplicationModelFacade.getApplicationList().size();
        } catch (Exception e) {

        }
        return "{\n" +
                "  \"Status\": \"OK\",\n" +
                "  \"Version\": \"" + getVersion() + "\",\n" +
                "  \"DEFCON\": \"" + ApplicationThreatResource.getDEFCON() + "\",\n" +
                "  \"ClusterSize\": " + ActiveUserTokenRepository.getNoOfClusterMembers() + ",\n" +
                "  \"ApplicationMapSize\": " + applicationMapSize + ",\n" +
                "  \"ActiveUserTokenMapSize\": " + ActiveUserTokenRepository.getMapSize() + ",\n" +
                "  \"LastSeenMapSize\": " + ActiveUserTokenRepository.getLastSeenMapSize() + ",\n" +
                "  \"PinMapSize\": " + ActivePinRepository.getPinMap().size() + ",\n" +
                "  \"AuthenticatedApplicationRepositoryMapSize\": " + AuthenticatedApplicationRepository.getMapSize() + ",\n" +
                "  \"Active Applications\": \"" + AuthenticatedApplicationRepository.getActiveApplications() + "\"\n" +
                "}\n\n\n" +
                threatSignalList;
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

    public static void addThreatSignal(String signal) {
        threatSignalList.add(signal);
    }
}
