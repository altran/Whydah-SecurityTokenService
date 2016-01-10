package net.whydah.admin.health;

import net.whydah.token.application.ApplicationThreatResource;
import net.whydah.token.user.ActiveUserTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
    @Produces(MediaType.TEXT_PLAIN)
    public Response isHealthy() {
        boolean ok = true;
        log.trace("isHealthy={}", ok);
        if (ok) {
            return Response.ok("Status: OK"+"\nDEFCON: "+ ApplicationThreatResource.getDEFCON()+"\nActiveUserTokenMapSize: "+ ActiveUserTokenRepository.getMapSize()).build();
        } else {
            //Intentionally not returning anything the client can use to determine what's the error for security reasons.
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
