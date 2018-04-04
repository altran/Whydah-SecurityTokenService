package org.valuereporter.agent;

import net.whydah.sso.config.ApplicationMode;
import net.whydah.sts.user.EnvHelper;
import net.whydah.sts.user.statistics.UserSessionObservedActivity;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.valuereporter.agent.activity.ObservedActivity;
import org.valuereporter.agent.activity.ObservedActivityDistributer;
import org.valuereporter.agent.http.HttpObservationDistributer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class MonitorReporterTest {

    private final static Logger log = LoggerFactory.getLogger(MonitorReporterTest.class);
    private static HttpObservationDistributer observationDistributer;

    @BeforeClass
    public static void shared() {
        Map<String, String> envs = new HashMap<>();
        envs.put(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);
        EnvHelper.setEnv(envs);

        String reporterHost = "localhost";
        String reporterPort = "4901";
        String prefix = "report";
        int cacheSize = 10;
        int forwardInterval = 10;
        new Thread(new ObservedActivityDistributer(reporterHost, reporterPort, prefix, cacheSize, forwardInterval)).start();
    }


    @Test
    @Ignore
    public void testReporting() {
        for (int n = 0; n < 200; n++) {
            ObservedActivity observedActivity = new UserSessionObservedActivity(UUID.randomUUID().toString(), "userSessionRenewal", UUID.randomUUID().toString());
            MonitorReporter.reportActivity(observedActivity);
        }
        try {
            Thread.sleep(10000);
        } catch (Exception e) {
            log.error("Unable to wait", e);
        }

    }

}
