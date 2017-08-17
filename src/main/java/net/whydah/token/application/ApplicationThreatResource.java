package net.whydah.token.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.admin.health.HealthResource;
import net.whydah.sso.whydah.DEFCON;
import net.whydah.sso.whydah.ThreatSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/threat")
public class ApplicationThreatResource {
    private final static Logger log = LoggerFactory.getLogger(ApplicationThreatResource.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static String defconvalue= DEFCON.DEFCON5.toString();


    @Path("/{applicationtokenid}/signal")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response logSignal(@PathParam("applicationtokenid") String applicationtokenid,
                              @FormParam("signal") String jsonSignal) {
        log.warn("logSignal with applicationtokenid: {} - signal={}", applicationtokenid, jsonSignal);

        ThreatSignal receivedSignal;
        String formattedSignal;
        try {

            receivedSignal = mapper.readValue(jsonSignal, ThreatSignal.class);
            if (receivedSignal.getSignalEmitter() != null || receivedSignal.getSignalEmitter().length() < 5) {
                if (applicationtokenid != null && applicationtokenid.length() > 6) {
                    String applicationID = AuthenticatedApplicationRepository.getApplicationIdFromApplicationTokenID(applicationtokenid);
                    String applicationName = AuthenticatedApplicationRepository.getApplicationNameFromApplicationTokenID(applicationtokenid);
                    receivedSignal.setSignalEmitter(applicationID + ":" + applicationName);
                }
            }
        } catch (Exception e) {
            receivedSignal = new ThreatSignal();
            receivedSignal.setText(jsonSignal);
            if (applicationtokenid != null && applicationtokenid.length() > 6) {
                String applicationID = AuthenticatedApplicationRepository.getApplicationIdFromApplicationTokenID(applicationtokenid);
                String applicationName = AuthenticatedApplicationRepository.getApplicationNameFromApplicationTokenID(applicationtokenid);
                receivedSignal.setSignalEmitter(applicationID + ":" + applicationName);
            }
        }
        try {
            formattedSignal = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(receivedSignal);
        } catch (Exception e) {
            formattedSignal = jsonSignal;
        }
        HealthResource.addThreatSignal(receivedSignal);
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
