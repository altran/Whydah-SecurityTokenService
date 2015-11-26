package net.whydah.token.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/threat")
public class ApplicationThreatResource {
    private final static Logger log = LoggerFactory.getLogger(ApplicationThreatResource.class);

    //  WebTarget userTokenResource = tokenServiceClient.target(tokenServiceUri).path("threat").path(myAppTokenId).path("signal");
    @Path("/{applicationtokenid}/signal")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response logSignal(@PathParam("applicationtokenid") String applicationtokenid, @FormParam("signal") String jsonSignal) {
        log.warn("logSignal with applicationtokenid: {} - signal={}", applicationtokenid, jsonSignal);
        return Response.ok().build();
    }

}
