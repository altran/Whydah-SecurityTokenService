package net.whydah.sts.health;

import net.whydah.sso.commands.systemtestbase.SystemTestBaseConfig;
import net.whydah.sso.config.ApplicationMode;
import net.whydah.sso.whydah.ThreatSignal;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static junit.framework.TestCase.assertTrue;

public class HealthResourceTest {

    private static final Logger log = LoggerFactory.getLogger(HealthResourceTest.class);

    static SystemTestBaseConfig config;

    @BeforeClass
    public static void init() throws Exception {
        System.setProperty(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);
        config = new SystemTestBaseConfig();

    }


    @Test
    // @Ignore
    public void testThreatListOutput() throws Exception {
        String threatSignalText = "A very important ThreatSignal";
        ThreatSignal threatSignal = new ThreatSignal();
        threatSignal.setText(threatSignalText);
        HealthResource.addThreatSignal(threatSignal);

        String healthResponseJson = HealthResource.getHealthTextJson();
        log.debug("Response from /health: {}", healthResponseJson);
        assertTrue(healthResponseJson.indexOf("Status") > 0);
        assertTrue(healthResponseJson.indexOf("DEFCON") > 0);
        assertTrue(healthResponseJson.indexOf(threatSignalText) > 0);

    }
}
