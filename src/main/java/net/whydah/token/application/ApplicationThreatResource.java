package net.whydah.token.application;

import net.whydah.admin.health.HealthResource;
import net.whydah.sso.whydah.DEFCON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;

@Path("/threat")
public class ApplicationThreatResource {
    private final static Logger log = LoggerFactory.getLogger(ApplicationThreatResource.class);

    private static String defconvalue= DEFCON.DEFCON5.toString();


    @Path("/{applicationtokenid}/signal")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response logSignal(@PathParam("applicationtokenid") String applicationtokenid,
                              @FormParam("signal") String jsonSignal) {
        log.warn("logSignal with applicationtokenid: {} - signal={}", applicationtokenid, jsonSignal);
        if (applicationtokenid != null && applicationtokenid.length() > 6) {
            String applicationID = AuthenticatedApplicationRepository.getApplicationIdFromApplicationTokenID(applicationtokenid);
            String applicationName = AuthenticatedApplicationRepository.getApplicationNameFromApplicationTokenID(applicationtokenid);
            HealthResource.addThreatSignal(applicationtokenid + " (" + applicationID + "[" + applicationName + "]):" + jsonSignal + " - Received: " + Instant.now());
        } else {  // Allow and handle threat signals from non-whydah components
            HealthResource.addThreatSignal(applicationtokenid + " ([]):" + jsonSignal + " - Received: " + Instant.now());
        }
        return Response.ok().build();
    }


    public static String getDEFCON(){
        return defconvalue;
    }

    public static void setDEFCON(String s){
        if (isInEnum(s, DEFCON.class)){
            defconvalue=s;
        }
    }


    public static <E extends Enum<E>> boolean isInEnum(String value, Class<E> enumClass) {
        for (E e : enumClass.getEnumConstants()) {
            if(e.name().equals(value)) {
                return true; }
        }
        return false;
    }
}
