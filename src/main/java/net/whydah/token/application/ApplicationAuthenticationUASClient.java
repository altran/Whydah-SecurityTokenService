package net.whydah.token.application;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import net.whydah.admin.health.HealthResource;
import net.whydah.sso.application.mappers.ApplicationCredentialMapper;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.util.WhydahUtil;
import net.whydah.sso.whydah.ThreatSignal;
import net.whydah.token.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;


public class ApplicationAuthenticationUASClient {

    private final static Logger log = LoggerFactory.getLogger(ApplicationAuthenticationUASClient.class);


    private static final String APPLICATION_AUTH_PATH = "application/auth";
    public static final String APP_CREDENTIAL_XML = "appCredentialXml";
    private static AppConfig appConfig = new AppConfig();
    private static java.util.Random generator = new SecureRandom();


    public static boolean checkAppsecretFromUAS(ApplicationCredential applicationCredential) {

        String useradminservice = appConfig.getProperty("useradminservice");
        ApplicationToken stsToken = AuthenticatedApplicationTokenRepository.getSTSApplicationToken();

        /**
         * Command version of UAS auth call
         */
        try {
            boolean isOKinUAS = new CommandCheckApplicationCredentialInUAS(URI.create(useradminservice), stsToken, applicationCredential).execute();
            if (isOKinUAS) {
                return true;
            }
        } catch (Exception e) {
            log.info("Unable to access UAS by Command", e);
        }


        /**
         * Legacy version of UAS auth call
         */
        WebResource uasResource = ApacheHttpClient.create().resource(useradminservice);
        int uasResponseCode = 0;
        WebResource webResource = uasResource.path(stsToken.getApplicationTokenId()).path(APPLICATION_AUTH_PATH);
        log.info("checkAppsecretFromUAS - Calling application auth " + webResource.toString());
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add(APP_CREDENTIAL_XML, ApplicationCredentialMapper.toXML(applicationCredential));
        try {
            ClientResponse response = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
            uasResponseCode = response.getStatus();
            log.info("checkAppsecretFromUAS - Response from UAS:" + uasResponseCode);
            if (uasResponseCode == 204) {
                return true;
            }
        } catch (Exception e) {
            log.error("checkAppsecretFromUAS - Problems connecting to {}", useradminservice);
            throw e;
        }
        log.warn("Illegal application tried to access whydah. ApplicationID: {}, Response from UAS: {}", applicationCredential.getApplicationID(), uasResponseCode);

        // Avoid bootstrap signalling the first 5 seconds
        if (Instant.now().getEpochSecond() - getRunningSince().getEpochSecond() > 5000) {
            if (uasResponseCode != 503) {
                HealthResource.addThreatSignal(createThreat("Illegal application tried to access whydah. ApplicationID: " + applicationCredential.getApplicationID() + ", Response from UAS: " + uasResponseCode));
            }
        }
        log.warn("checkAppsecretFromUAS: Response from UAS:" + uasResponseCode);
        return false;
    }

    public static Instant getRunningSince() {
        long uptimeInMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        return Instant.now().minus(uptimeInMillis, ChronoUnit.MILLIS);
    }

    public static ThreatSignal createThreat(String text) {
        ThreatSignal threatSignal = new ThreatSignal();
        threatSignal.setSignalEmitter("SecurityTokenService");
        threatSignal.setAdditionalProperty("EMITTER IP", WhydahUtil.getMyIPAddresssesString());
        threatSignal.setInstant(Instant.now().toString());
        threatSignal.setText(text);
        return threatSignal;
    }
}
