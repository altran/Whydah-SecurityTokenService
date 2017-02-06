package net.whydah.token.application;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.whydah.sso.whydah.DEFCON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/threat")
public class ApplicationThreatResource {
    private final static Logger log = LoggerFactory.getLogger(ApplicationThreatResource.class);

    private static String defconvalue= DEFCON.DEFCON5.toString();

    //  WebTarget userTokenResource = tokenServiceClient.target(tokenServiceUri).path("threat").path(myAppTokenId).path("signal");
    @Path("/{applicationtokenid}/signal")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response logSignal(@PathParam("applicationtokenid") String applicationtokenid, @FormParam("signal") String jsonSignal) {
        log.warn("logSignal with applicationtokenid: {} - signal={}", applicationtokenid, jsonSignal);
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
