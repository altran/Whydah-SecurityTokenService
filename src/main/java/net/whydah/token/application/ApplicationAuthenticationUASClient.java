package net.whydah.token.application;

import net.whydah.admin.health.HealthResource;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.util.WhydahUtil;
import net.whydah.sso.whydah.ThreatSignal;
import net.whydah.token.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;


public class ApplicationAuthenticationUASClient {

    private final static Logger log = LoggerFactory.getLogger(ApplicationAuthenticationUASClient.class);

    private static AppConfig appConfig = new AppConfig();


    public static boolean checkAppsecretFromUAS(ApplicationCredential applicationCredential) {

        String useradminservice = appConfig.getProperty("useradminservice");
        ApplicationToken stsToken = AuthenticatedApplicationTokenRepository.getSTSApplicationToken();


        /**
         * Command version of UAS auth call
         */
        try {
            boolean isOKinUAS = new CommandCheckApplicationCredentialInUAS(URI.create(useradminservice), stsToken.getApplicationTokenId(), applicationCredential).execute();
            log.debug("CommandCheckApplicationCredentialInUAS returned: {}", isOKinUAS);
            if (isOKinUAS) {
                return true;
            }
        } catch (Exception e) {
            log.info("Unable to access UAS by Command", e);
        }

        // Avoid bootstrap signalling the first 5 seconds
        if (Instant.now().getEpochSecond() - getRunningSince().getEpochSecond() > 5000) {
            HealthResource.addThreatSignal(createThreat("Illegal application tried to access whydah. ApplicationID: " + applicationCredential.getApplicationID() + ", Response from UAS: "));
        }
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
