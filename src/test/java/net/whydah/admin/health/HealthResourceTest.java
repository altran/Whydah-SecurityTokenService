package net.whydah.admin.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.sso.commands.systemtestbase.SystemTestBaseConfig;
import net.whydah.sso.config.ApplicationMode;
import net.whydah.sso.whydah.ThreatSignal;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static junit.framework.TestCase.assertTrue;

public class HealthResourceTest {

    private static final Logger log = LoggerFactory.getLogger(HealthResourceTest.class);
    private static ObjectMapper mapper = new ObjectMapper();

    static SystemTestBaseConfig config;

    @BeforeClass
    public static void init() throws Exception {
        System.setProperty(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);
        config = new SystemTestBaseConfig();

    }


    @Test
    @Ignore
    public void testThreatListOutput() throws Exception {
        ThreatSignal t = new ThreatSignal();
        t.setText("jkljl");
        HealthResource.addThreatSignal(t);

        String threatSignalJson = HealthResource.getHealthTextJson();
        assertTrue(threatSignalJson.indexOf("jkljl") > 0);

    }
}
