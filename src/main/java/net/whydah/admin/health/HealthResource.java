package net.whydah.admin.health;

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
import java.util.Properties;

/**
 * Endpoint for health check
 */
@Path("/health")
public class HealthResource {
    private static final Logger log = LoggerFactory.getLogger(HealthResource.class);

    public HealthResource() {
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response isHealthy() {
        boolean ok = true;
        log.trace("isHealthy={}", getHealthText());
        if (ok) {
            return Response.ok(getHealthTextJson()).build();
        } else {
            //Intentionally not returning anything the client can use to determine what's the error for security reasons.
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    public static String  getHealthText(){
        return "Status: OK"+
                "\nDEFCON: "+ ApplicationThreatResource.getDEFCON()+
                "\nClusterSize: " + ActiveUserTokenRepository.getNoOfClusterMembers() +
                "\nActiveUserTokenMapSize: " + ActiveUserTokenRepository.getMapSize() +
                "\nLastSeenMapSize: " + ActiveUserTokenRepository.getLastSeenMapSize() +
                "\nPinMapSize: " + ActivePinRepository.getPinMap().size() +
                "\nAuthenticatedApplicationRepositoryMapSize: " + AuthenticatedApplicationRepository.getMapSize() +
                "\nActive Applications: " + AuthenticatedApplicationRepository.getActiveApplications();
    }

    public static String getHealthTextJson() {
        return "{\n" +
                "  \"Status\": \"OK\",\n" +
                "  \"Version\": \"" + getVersion() + "\",\n" +
                "  \"DEFCON\": \"" + ApplicationThreatResource.getDEFCON() + "\",\n" +
                "  \"ClusterSize\": " + ActiveUserTokenRepository.getNoOfClusterMembers() + ",\n" +
                "  \"ActiveUserTokenMapSize\": " + ActiveUserTokenRepository.getMapSize() + ",\n" +
                "  \"LastSeenMapSize\": " + ActiveUserTokenRepository.getLastSeenMapSize() + ",\n" +
                "  \"PinMapSize\": " + ActivePinRepository.getPinMap().size() + ",\n" +
                "  \"AuthenticatedApplicationRepositoryMapSize\": " + AuthenticatedApplicationRepository.getMapSize() + ",\n" +
                "  \"Active Applications\": \"" + AuthenticatedApplicationRepository.getActiveApplications() + "\"\n" +
                "}\n";
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
}
